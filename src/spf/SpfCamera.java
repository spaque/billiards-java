package spf;

import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.ViewingPlatform;
import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.PhysicalEnvironment;
import javax.media.j3d.QuadArray;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import static spf.SpfConstants.*;

/**
 * Clase para el manejo de la camara.
 * @author Sergio Paque Martin
 */
public class SpfCamera {
	private TransformGroup m_Transform;
	private ViewPlatform m_Platform;
	private ViewingPlatform m_ViewingPlatform;
	private View m_View;
	private PhysicalBody m_Body;
	private PhysicalEnvironment m_Env;
	private boolean m_Antialiasing = false;
	private Canvas3D m_Canvas3D;
	
	private Texture2D[] m_HudTextures;
	private Appearance m_HudAppearance;
	
	// radius, azimuth, elevation
	// r, theta, phi
	private double[] m_Position = new double[] {5, -PiOver4, PiOver4};
	// dr, dtheta, dphi
	private double[] m_Velocity = new double[] {0, 0, 0};
	private double[] m_Force = new double[] {0, 0, 0};
	private double[] m_MaxCoord = new double[] {8, Double.MAX_VALUE, PiOver2 + 0.15d};
	private double[] m_MinCoord = new double[] {0.5d, -Double.MAX_VALUE, 0};
	private double m_Mass = 5;
	
	/**
	 * Constructor de SpfCamera.
	 * @param canvas Canvas3D de la aplicacion.
	 */
	public SpfCamera (Canvas3D canvas) {
		m_Canvas3D = canvas;
		
		m_ViewingPlatform = new ViewingPlatform();
		m_Platform = new ViewPlatform();
		m_Transform = new TransformGroup();
		m_Body = new PhysicalBody();
		m_Env = new PhysicalEnvironment();
		m_View = new View();
		
		m_Transform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		m_Transform.addChild(m_Platform);
		m_ViewingPlatform.addChild(m_Transform);
		
		m_View.setBackClipDistance(BackClipDistance);
		m_View.setPhysicalBody(m_Body);
		m_View.setPhysicalEnvironment(m_Env);
		m_View.attachViewPlatform(m_Platform);
		m_View.addCanvas3D(canvas);
		
		m_HudTextures = new Texture2D[8];
		// Inicializar posicion de la camara.
		updateTransform();
		// Añadir marcador
		addHUD();
	}
	
	/**
	 * Cambia el estado de anialiasing de la escena.
	 */
	public void toggleAntialiasing() {
		m_Antialiasing = !m_Antialiasing;
		m_View.setSceneAntialiasingEnable(m_Antialiasing);
	}

	/**
	 * @return Devuelve la "vista" del grafo de la escena.
	 */
	public BranchGroup getViewingPlatform () {
		return m_ViewingPlatform;
	}
	
	/**
	 * @return Devuelve la transformacion de la camara.
	 */
	public TransformGroup getTransform () {
		return m_Transform;
	}

	/**
	 * Aplica fuerza en el eje del radio.
	 * @param wPressed Tecla W presionada.
	 * @param sPressed Tecla S presionada.
	 */
	public void moveRadius(boolean wPressed, boolean sPressed) {
		m_Force[0] = 0;
		if (wPressed)
			m_Force[0] -= KeystrokeForce * 1.5d;
		if (sPressed)
			m_Force[0] += KeystrokeForce * 1.5d;
		m_Force[0] += -Drag * m_Velocity[0];
	}
	
	/**
	 * Aplica fuerza en el eje del azimuth o theta.
	 * @param rightPressed Tecla Derecha presionada.
	 * @param leftPressed Tecla izquierda presionada.
	 */
	public void moveAzimuth (boolean rightPressed, boolean leftPressed) {
		m_Force[1] = 0;
		if (rightPressed)
			m_Force[1] += KeystrokeForce;
		if (leftPressed)
			m_Force[1] -= KeystrokeForce;
		m_Force[1] += -Drag * m_Velocity[1];
	}
	
	/**
	 * Aplica fuerza en el eje de la elevacion o phi.
	 * @param upPressed Tecla Arriba presionada.
	 * @param downPressed Tecla Abajo presionada.
	 */
	public void moveElevation(boolean upPressed, boolean downPressed) {
		m_Force[2] = 0;
		if (upPressed)
			m_Force[2] -= KeystrokeForce;
		if (downPressed)
			m_Force[2] += KeystrokeForce;
		m_Force[2] += -Drag * m_Velocity[2];
	}

	/**
	 * Simular movimiento de la camara y actualizar estado.
	 */
	public void updateState() {
		double dt = 0.01667f;
		double K1, K23, K4;
		//double L1, L2, L3, L4;
		// L1 = L2 = L3 = L4
		double L;
		double deltap;
		
		for (int i = 0; i < 3; i++) {
			L = dt * f(i);
			K1 = dt * m_Velocity[i];
			K23 = dt * (m_Velocity[i] + L/2);
			K4 = dt * (m_Velocity[i] + L);
			deltap = (K1 + 4*K23 + K4) /6;
			if (m_Position[i] + deltap > m_MinCoord[i] && m_Position[i] + deltap < m_MaxCoord[i]) {
				m_Position[i] += deltap;
				m_Velocity[i] += L;
			} else {
				m_Velocity[i] = 0;
			}
		}
		updateTransform();
	}
	
	/**
	 * Funcion f para Runge-Kutta.
	 * @param i Indice de coordenada.
	 * @return Devuelve la aceleración en el eje i.
	 */
	private double f (int i) {
		return m_Force[i]/m_Mass;
	}
	
	/**
	 * Actualiza la transformacion de la camara.
	 */
	private void updateTransform () {
		Point3d eye = new Point3d();
		eye.x = m_Position[0] * Math.sin(m_Position[1]) * Math.sin(m_Position[2]);
		eye.y = m_Position[0] * Math.cos(m_Position[2]);
		eye.z = m_Position[0] * Math.cos(m_Position[1]) * Math.sin(m_Position[2]);
		Transform3D t3d = new Transform3D();
		t3d.lookAt(eye, Origin, new Vector3d(0, 1, 0));
		t3d.invert();
		m_Transform.setTransform(t3d);
	}
	
	/**
	 * Añade el marcador a la camara.
	 */
	private void addHUD () {
		Transform3D t3d = new Transform3D();
		t3d.setTranslation(new Vector3f(0, -0.92f, -4.0f));
		TransformGroup tg = new TransformGroup(t3d);
		
		m_HudAppearance = new Appearance();
		TransparencyAttributes ta = new TransparencyAttributes();
		ta.setTransparencyMode(TransparencyAttributes.BLENDED);
		m_HudAppearance.setTransparencyAttributes(ta);
		RenderingAttributes ra = new RenderingAttributes();
		ra.setDepthTestFunction(RenderingAttributes.ALWAYS);
		m_HudAppearance.setRenderingAttributes(ra);
		
		QuadArray quad = new QuadArray(4, QuadArray.COORDINATES | QuadArray.TEXTURE_COORDINATE_2);
		//quad.setCoordinate(0, new float[] {-1, -1, 0});
		//quad.setCoordinate(1, new float[] {1, -1, 0});
		//quad.setCoordinate(2, new float[] {1, 1, 0});
		//quad.setCoordinate(3, new float[] {-1, 1, 0});
		quad.setCoordinate(0, new float[] {-0.5f, -0.125f, 0});
		quad.setCoordinate(1, new float[] {0.5f, -0.125f, 0});
		quad.setCoordinate(2, new float[] {0.5f, 0.125f, 0});
		quad.setCoordinate(3, new float[] {-0.5f, 0.125f, 0});
		
		quad.setTextureCoordinate(0, 0, new float[] {0, 0});
		quad.setTextureCoordinate(0, 1, new float[] {1, 0});
		quad.setTextureCoordinate(0, 2, new float[] {1, 1});
		quad.setTextureCoordinate(0, 3, new float[] {0, 1});
		
		loadHudTextures();
		m_HudAppearance.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
		m_HudAppearance.setTexture(m_HudTextures[4]);
		tg.addChild(new Shape3D(quad, m_HudAppearance));
		m_Transform.addChild(tg);
	}
	
	/**
	 * Establece la fuerza de tiro con proposito
	 * de actualizar el marcador.
	 * @param index
	 */
	public void setHudForce (int index) {
		m_HudAppearance.setTexture(m_HudTextures[index]);
	}
	
	/**
	 * Carga las texturas del marcador.
	 */
	private void loadHudTextures () {
		String base = SpfApplication.getResourcesPath();
		String path;
		
		for (int i = 0; i < m_HudTextures.length; i++) {
			path = base + "power" + (i + 1) + ".png";
			TextureLoader loader = 
					new TextureLoader(
						path, 
						0, 
						m_Canvas3D);
			ImageComponent2D image = loader.getImage();
			int imageWidth = image.getWidth();
			int imageHeight = image.getHeight();

			Texture2D texture = 
						new Texture2D(
							Texture.BASE_LEVEL,
							Texture.RGBA,
							imageWidth, imageHeight);
			texture.setImage(0, image);
			m_HudTextures[i] = texture;
		}
	}
}
