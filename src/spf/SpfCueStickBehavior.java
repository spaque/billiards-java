package spf;

import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Enumeration;
import javax.media.j3d.Alpha;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.RotPosPathInterpolator;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.media.j3d.WakeupOr;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import static spf.SpfConstants.*;

/**
 * Comportamiento personalizado para el raton.
 * @author Sergio
 */
public class SpfCueStickBehavior extends MouseBehavior {
	protected WakeupCondition m_WakeupCondition;
	private Object m_Sync;
	private SpfUniverse m_Universe;
	private float rotY = 0;
	private Component m_Component;
	private Vector3f m_CueBallPosition;
	int m_Force = 5;
	boolean m_MouseDragged = false;
	boolean m_FirstShot;
	
	/**
	 * Constructor de SpfCueStickBehavior.
	 * @param comp Componente sobre el que se escuchan los eventos.
	 * @param tg TransformGroup sobre el que se opera.
	 * @param cueBallPos Posicion de la bola blanca.
	 * @param firstShot Indica si es el primer tiro del juego.
	 */
	public SpfCueStickBehavior (Component comp, 
								TransformGroup tg, 
								Vector3f cueBallPos, 
								boolean firstShot) {
		super(comp, tg);
		m_Component = comp;
		m_CueBallPosition = cueBallPos;
		m_FirstShot = firstShot;
		WakeupCriterion[] mouseCritArray = new WakeupCriterion[2];
		mouseCritArray[0] = new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED);
		mouseCritArray[1] = new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED);
		// Despertar si se producen cualquiera de los dos eventos.
		m_WakeupCondition = new WakeupOr(mouseCritArray);
		
		setSchedulingBounds(new BoundingSphere(new Point3d(), 100));
	}
	
	/**
	 * Establece el objeto con el que se sincroniza
	 * el tiro y la simulacion del juego.
	 * @param o Objeto para la sincronizacion.
	 */
	public void setSyncObject (Object o) {
		m_Sync = o;
	}
	
	/**
	 * Establece el universo del juego.
	 * @param universe Universo del juego.
	 */
	public void setUniverse (SpfUniverse universe) {
		m_Universe = universe;
	}
	
	@Override
	/**
	 * Redefinicion para evitar los println por defecto.
	 */
	public void mouseWheelMoved (MouseWheelEvent e) {}
	
	@Override
	/**
	 * Inicializa el comportamiento.
	 */
	public void initialize() {
		super.initialize();
		wakeupOn(m_WakeupCondition);
	}

	/**
	 * Procesa los eventos recibidos.
	 * @param criteria Eventos recibidos.
	 */
	public void processStimulus(Enumeration criteria) {
		WakeupCriterion wakeup;
		AWTEvent[] events;
		MouseEvent event;
		int id;
        while (criteria.hasMoreElements()) {
			wakeup = (WakeupCriterion)criteria.nextElement();
			if (wakeup instanceof WakeupOnAWTEvent) {
				events = ((WakeupOnAWTEvent)wakeup).getAWTEvent();
				for (int i = 0; i < events.length; i++) {
					event = (MouseEvent) events[i];
					processMouseEvent(event);
					id = event.getID();
					if (id == MouseEvent.MOUSE_DRAGGED) {
						x = event.getX();
						y = event.getY();
						int dx = x - x_last;

						// Movimiento horizontal cambia la rotacion.
						float deltaRot = dx * 0.5f;
						if (!m_FirstShot || 
								(rotY + deltaRot < 80 &&
								rotY + deltaRot > -80)) {
							rotY += deltaRot;
							transformGroup.setTransform(getStickRotation(rotY));
						}
						
						// Movimiento vertical cambia la fuerza de tiro.
						int height = m_Component.getHeight();
						// Evitar movimiento fuera del componente.
						if (m_Universe != null && y > 0 && y < height) {
							int forceInv = 8 * y / height;
							m_Force = 8 - forceInv;
							m_Universe.setShotForce(m_Force - 1);
						}
						
						x_last = x;
						y_last = y;
						m_MouseDragged = true;
					} else if (id == MouseEvent.MOUSE_RELEASED) {
						if (!m_MouseDragged) continue;
						
						Matrix3f rot = SpfScene.getRotationMatrix(0, -rotY, 0);
						Vector3f dir = new Vector3f(1, 0, 0);
						rot.transform(dir);
						m_Universe.setShotDir(dir);
						
						Transform3D t3d = new Transform3D();
						Vector3f trans = new Vector3f();
						Quat4f quat = new Quat4f();
						transformGroup.getTransform(t3d);
						t3d.get(quat, trans);
						
						// Nudos sobre los que pasa el camino.
						float[] knots = new float[] { 0.0f, 0.5f, 1.0f };
						float scale = ((float)m_Force / 4) + 0.75f;
						Point3f[] points = new Point3f[3];
						// Posicion en los nudos.
						points[0] = new Point3f(trans.x, trans.y, trans.z);
						points[1] = new Point3f(trans.x - (0.3f * scale), trans.y, trans.z);
						points[2] = new Point3f(trans.x + 0.05f, trans.y, trans.z);
						Quat4f[] quats = new Quat4f[3];
						// Rotacion en los nudos.
						quats[0] = new Quat4f(quat);
						quats[1] = new Quat4f(quat);
						quats[2] = new Quat4f(quat);
						transformGroup.getTransform(t3d);
						// Temporizacion del interpolador.
						Alpha alpha = new Alpha(1, 1000);
						alpha.setStartTime(System.currentTimeMillis());
						RotPosPathInterpolator path = new RotPosPathInterpolator(alpha, transformGroup, t3d, knots, quats, points);
						path.setSchedulingBounds(new BoundingSphere(new Point3d(), 100));
						
						BranchGroup bg = new BranchGroup();
						bg.addChild(path);
						bg.compile();
						transformGroup.addChild(bg);
						
						// Notificar del tiro.
						if (m_Sync != null) {
							synchronized(m_Sync) {
								m_Sync.notify();
							}
						}
						m_MouseDragged = false;
						m_Component.removeMouseListener(this);
						m_Component.removeMouseMotionListener(this);
						m_Component.removeMouseWheelListener(this);
					}
				}
			}
		}
		wakeupOn(m_WakeupCondition);
	}
	
	/**
	 * Calcula la transformacion a utilizar por el taco.
	 * @param rotY Rotacion sobre el eje Y.
	 * @return Devuelve la rotacion calculada.
	 */
	private Transform3D getStickRotation(float rotY) {
		Transform3D t3d = new Transform3D();
		t3d.setTranslation(new Vector3f(1, 0, 0));
		Transform3D t2 = new Transform3D();
		t2.setRotation(SpfScene.getRotationMatrix(180, rotY, 7));
		t2.setTranslation(m_CueBallPosition);
		Transform3D t3 = new Transform3D();
		t3.setTranslation(new Vector3f(-1, 0, 0));
		t3d.mul(t2);
		t3d.mul(t3);
		return t3d;
	}

}
