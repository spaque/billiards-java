package spf;

import com.sun.j3d.utils.universe.SimpleUniverse;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Vector;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Locale;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.VirtualUniverse;
import javax.swing.JOptionPane;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import spf.objects.SpfObject;
import spf.objects.SpfSphere;
import spf.objects.SpfWavefrontObj;

import static spf.SpfConstants.*;

/**
 * Clase principal del juego que representa el universo.
 * @author Sergio Paque Martin
 */
public class SpfUniverse extends VirtualUniverse implements Runnable {
	private Locale m_Locale;
	private BranchGroup m_ViewGroup;
	private BranchGroup m_WorldGroup;
	private Canvas3D m_Canvas3D;
	private SpfCamera m_Camera;
	private SpfFpsBehavior m_Fps;
	private BranchGroup m_Stick;
	private Object m_Sync;
	private Thread m_GameThread;
	private HashMap<String, SpfObject> m_ObjectMap;
	private BranchGroup m_BallsBranch;
	private Vector3f m_ShotDir;
	private Vector<SpfSphere> m_ActiveBalls;
	
	private int m_NPoolBalls;
	private int m_ShotForce;
	private boolean m_FirstShot;
	private float[][] m_BallDistances;
	
	/**
	 * Constructor de SpfUniverse.
	 */
	public SpfUniverse () {
		m_Locale = new Locale(this);
		
		m_ViewGroup = new BranchGroup();
		m_ViewGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		
		m_Canvas3D = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
		m_Camera = new SpfCamera(m_Canvas3D);
		
		m_Sync = new Object();
		m_GameThread = new Thread(this);
		
		m_BallDistances = new float[GameBalls+1][GameBalls+1];
	}
	
	/**
	 * Cambia el estado del antialiasing de la escena.
	 */
	public void toggleAntialiasing() {
		m_Camera.toggleAntialiasing();
	}
	
	/**
	 * @return Devuelve el Canvas3D utilizado en la aplicacion.
	 */
	public Canvas3D getCanvas3D () {
		return m_Canvas3D;
	}
	
	/**
	 * @return Devuelve el numero de frames por segundo.
	 */
	public long getFps () {
		if (m_Fps == null) return 0;
		return m_Fps.getFps();
	}
	
	/**
	 * Carga la escena del juego.
	 * @return Devuelve el Canvas3D utilizado.
	 */
	public Canvas3D loadScene () {
		SpfScene scn = new SpfScene(m_Canvas3D, false);
		try {
			m_WorldGroup = scn.loadSceneFromFile("scene.xml");
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(m_Canvas3D, e.getMessage(),"Error", JOptionPane.ERROR_MESSAGE);
			m_WorldGroup = new BranchGroup();
		}
		m_WorldGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		m_WorldGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		m_Fps = new SpfFpsBehavior();
		m_Fps.setSchedulingBounds(new BoundingSphere());
		m_WorldGroup.addChild(m_Fps);
		m_WorldGroup.compile();
		m_Locale.addBranchGraph(m_WorldGroup);
		
		m_ViewGroup.addChild(m_Camera.getViewingPlatform());
		m_ViewGroup.compile();
		m_Locale.addBranchGraph(m_ViewGroup);
		
		// Añadimos el KeyListener al componente e
		// iniciamos la hebra de la camara
		SpfKeyListener listener = new SpfKeyListener(m_Camera);
		m_Canvas3D.addKeyListener(listener);
		new Thread(listener).start();
		
		return m_Canvas3D;
	}
	
	/**
	 * Carga en la escena las bolas del billar.
	 * @throws java.lang.IllegalArgumentException
	 */
	private void loadBilliardBalls() throws IllegalArgumentException {
		SpfScene scn = new SpfScene(m_Canvas3D, true);
		m_BallsBranch = scn.loadSceneFromFile("balls.xml");
		m_BallsBranch.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		m_BallsBranch.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		m_BallsBranch.setCapability(BranchGroup.ALLOW_DETACH);
		m_ObjectMap = scn.getSceneMap();
		initializeActiveBalls();
		m_BallsBranch.compile();
		m_WorldGroup.addChild(m_BallsBranch);
	}
	
	/**
	 * Inicializa las bolas de billar activas a
	 * partir del mapa de objetos de la escena.
	 */
	private void initializeActiveBalls () {
		m_ActiveBalls = new Vector<SpfSphere>(m_NPoolBalls);
		String key;
		m_ActiveBalls.add((SpfSphere)m_ObjectMap.get("BolaBlanca"));
		for (int i = 1; i < m_NPoolBalls; i++) {
			key = "Bola" + i;
			m_ActiveBalls.add((SpfSphere)m_ObjectMap.get(key));
		}
	}
	
	/**
	 * Inicializa la matriz de distancias de las bolas.
	 */
	private void initializeBallDistances () {
		for (int i = 0; i <= GameBalls; i++) {
			for (int j = 0; j <= GameBalls; j++) {
				m_BallDistances[i][j] = 1000;
			}
		}
	}
	
	/**
	 * Dibuja el taco de billar y le asocia su comportamiento.
	 */
	private void drawCueStick () {
		SpfObject cueBall = m_ObjectMap.get("BolaBlanca");
		// Rotacion inicial del taco
		Matrix3f rot = SpfScene.getRotationMatrix(180, 0, 7);
		Vector3f trans =  new Vector3f(cueBall.getPosition());
		trans.x -= 1.0f;
		trans.y += 0.1f;
		Transform3D t3d = new Transform3D(rot, trans, 1);
		TransformGroup tg = new TransformGroup(t3d);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		
		if (m_Stick == null) {
			SpfWavefrontObj obj = new SpfWavefrontObj(null, m_Canvas3D);
			Node stick = obj.load("taco.obj", "taco.mtl");
			m_Stick = new BranchGroup();
			m_Stick.setCapability(BranchGroup.ALLOW_DETACH);
			m_Stick.setCapabilityIsFrequent(BranchGroup.ALLOW_DETACH);
			m_Stick.addChild(stick);
		}
		
		BranchGroup stickBranch = new BranchGroup();
		stickBranch.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		stickBranch.addChild(m_Stick);
		tg.addChild(stickBranch);
		BranchGroup bg = new BranchGroup();
		bg.addChild(tg);
		
		trans.y -= 0.1f;
		// Añadir el comportamiento al taco
		SpfCueStickBehavior behavior = new SpfCueStickBehavior(m_Canvas3D, tg, trans, m_FirstShot);
		m_FirstShot = false;
		behavior.setSyncObject(m_Sync);
		behavior.setUniverse(this);
		bg.addChild(behavior);
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.compile();
		
		m_WorldGroup.addChild(bg);
	}
	
	/**
	 * Elimina el taco de billar de la escena.
	 */
	private void removeCueStick () {
		int index = m_WorldGroup.numChildren() - 1;
		m_WorldGroup.removeChild(index);
		m_Stick.detach();
	}
	
	/**
	 * Realiza la simulacion fisica del tiro.
	 * @throws java.lang.InterruptedException
	 */
	private void simulateShot () throws InterruptedException {
		int it = 0;
		BitSet moving = new BitSet(m_NPoolBalls);
		
		SpfSphere bolaBlanca = m_ActiveBalls.firstElement();
		Vector3f force = new Vector3f(m_ShotDir);
		force.scale((m_ShotForce+1)*150);
		// Aplicar fuerza instantanea del tiro
		bolaBlanca.applyForce(force);
		
		moving.set(0);
		while (!moving.isEmpty()) {
			// Detectar colisiones entre bolas
			for (int i = 0; i < m_ActiveBalls.size(); i++) {
				simulateStep(m_ActiveBalls.elementAt(i), i);
			}
			// Actualizar estado de las bolas
			for (int i = 0; i < m_ActiveBalls.size(); i++) {
				SpfSphere ball = m_ActiveBalls.elementAt(i);
				// Actualizamos solo si la bola esta en movimiento
				// o cuando realizamos el tiro inicial
				if (it == 0 || ball.isMoving())
					ball.updateState();
			}
			// Actualizar mapa de bits para detectar el fin
			// de la simulacion
			for (int i = 0; i < m_ActiveBalls.size(); i++) {
				SpfSphere ball = m_ActiveBalls.elementAt(i);
				if (ball.isMoving()) {
					moving.set(i);
					ball.updateTransform();
					updateBallShadow(ball);
				} else {
					moving.clear(i);
				}
			}
			// Retiramos la fuerza del tiro tras
			// la primera iteracion
			if (it == 0) bolaBlanca.applyForce(new Vector3f());
			it++;
			Thread.sleep(9);
		}
	}
	
	/**
	 * Detecta colisiones entre una bola dada y las demas.
	 * @param ball Bola sobre la que hay que detectar colisiones.
	 * @param idx Indice que ocupa la bola en el vector de bolas.
	 */
	private void simulateStep (SpfSphere ball, int idx) {
		for (int i = 0; i < m_ActiveBalls.size(); i++) {
			SpfSphere sphere = m_ActiveBalls.elementAt(i);
			if (i != idx) {
				Vector3f v = new Vector3f(sphere.getPosition());
				v.sub(new Vector3f(ball.getPosition()));
				float currentDistance = v.length();
				float minDistance = ball.getRadius() + sphere.getRadius();
				float lastDistance = m_BallDistances[idx][i];

				// Si entramos en colision actualizamos la bola
				if (currentDistance < minDistance && currentDistance < lastDistance) {
					ball.updateOnCollision(sphere, v);
				}
				m_BallDistances[idx][i] = currentDistance;
			}
		}
	}
	
	/**
	 * Actualiza la posicion de la sombra de la bola.
	 * @param ball Bola a la que actualizar la sombra.
	 */
	public void updateBallShadow (SpfSphere ball) {
		String name = "Sombra" + ball.getName();
		SpfObject shadow = m_ObjectMap.get(name);
		if (shadow != null) {
			Transform3D t3d = new Transform3D();
			TransformGroup tg = ball.getTransformGroup();
			tg.getTransform(t3d);
			Vector3f translation = new Vector3f();
			t3d.get(translation);
			translation.y = -0.87f;
			Transform3D transform = new Transform3D();
			transform.set(translation);
			shadow.setTransform(transform);
		}
	}
	
	/**
	 * Establece la fuerza del tiro.
	 * @param force Fuerza del tiro.
	 */
	public void setShotForce (int force) {
		m_Camera.setHudForce(force);
		m_ShotForce = force;
	}
	
	/**
	 * Establece la direccion del tiro.
	 * @param dir Direccion del tiro.
	 */
	public void setShotDir (Vector3f dir) {
		m_ShotDir = dir;
	}
	
	/**
	 * Inicia el juego.
	 */
	public void startGame () {
		m_NPoolBalls = GameBalls + 1;
		m_FirstShot = true;
		initializeBallDistances();
		loadBilliardBalls();
		m_GameThread.start();
	}

	/**
	 * Bucle principal del juego.
	 */
	public void run() {
		while (true) {
			try {
				// Dibujamos el taco y esperamos
				// a que se produzca el tiro.
				synchronized(m_Sync) {
					drawCueStick();
					m_Sync.wait();
				}
				// Esperar a que se realize la animacion.
				Thread.sleep(1100);
				removeCueStick();
				simulateShot();
				//Thread.sleep(2000);
				Thread.yield();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
				System.exit(-1);
			}
		}
	}
}
