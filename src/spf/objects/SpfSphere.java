package spf.objects;

import com.sun.j3d.utils.geometry.Sphere;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import spf.SpfScene;
import spf.xml.SpfXmlNode;

import static spf.SpfConstants.*;

/**
 * Clase que representa una primitiva "esfera".
 * Se utiliza para representar las bolas de billar.
 * @author Sergio Paque Martin
 */
public class SpfSphere extends SpfObject {
	private float m_Radius;
	private float[] m_AngularVelocity;
	private float[] m_Angle;
	private float[] m_Torque;
	private float InertiaTensor;

	/**
	 * Constructor de SpfSphere.
	 * @param name Nombre del objeto.
	 * @param tg Transformacion inicial del objeto.
	 * @param canvas Canvas3D que utiliza la aplicacion principal.
	 */
	public SpfSphere (String name, TransformGroup tg, Canvas3D canvas) {
		m_Name = name;
		m_Mass = 1;
		m_Elasticity = 1;
		m_Force = new float[3];
		m_Velocity = new float[3];
		m_Position = new float[3];
		m_Model = tg;
		m_Canvas3D = canvas;
		m_AngularVelocity = new float[3];
		m_Angle = new float[3];
		m_Torque = new float[3];
	}
	
	/**
	 * @return Devuelve el radio de la esfera.
	 */
	public float getRadius () {
		return m_Radius;
	}
	
	/**
	 * @return Devuelve la velocidad angular de la esfera.
	 */
	public float[] getAngularVelocity() {
		return m_AngularVelocity;
	}
	
	/**
	 * Carga una esfera a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve un nodo con la esfera leida.
	 */
	public Node load (SpfXmlNode node) {
		float radius = 1;
		String attribute;

		loadAppearance(node);
		
		attribute = node.getAttribute("radius");
		if (attribute.length() > 0)
			radius = Float.parseFloat(attribute);
		m_Radius = radius;
		
		if (m_Appearance != null) {
			int primflags = Sphere.GENERATE_NORMALS + Sphere.GENERATE_TEXTURE_COORDS;
			m_Primitive = new Sphere(radius, primflags, 63, m_Appearance);
		} else {
			m_Primitive = new Sphere(radius, 0, 63);
		}
		
		Vector3f position = new Vector3f();
		Transform3D t3d = new Transform3D();
		m_Model.getTransform(t3d);
		t3d.get(position);
		position.get(m_Position);
		
		// Calcular el momento de inercia de la esfera
		InertiaTensor = 0.4f * m_Mass * m_Radius * m_Radius;
		
		return m_Primitive;
	}
	
	@Override
	/**
	 * Actualiza el estado de la esfera, 
	 * tanto del movimiento lineal como
	 * del movimiento angular.
	 */
	public void updateState () {
		// Actualizar movimiento lineal
		super.updateState();

		Vector3f force = new Vector3f();
		// Vector desde el punto de contacto con el
		// suelo hasta el centro de la esfera.
		Vector3f r = new Vector3f(0, -m_Radius, 0);
		Vector3f perimeterVelocity = new Vector3f();
		Vector3f torque = new Vector3f();
		
		// Calcular velocidad en el perimetro de la esfera.
		perimeterVelocity.cross(new Vector3f(m_AngularVelocity), r);
		perimeterVelocity.x += m_Velocity[0];
		perimeterVelocity.y += m_Velocity[1];
		perimeterVelocity.z += m_Velocity[2];
		if (perimeterVelocity.lengthSquared() != 0) {
			// Beware of Nan!!!
			perimeterVelocity.normalize();
		}
		
		// Calcular fuerza de rozamiento de la esfera con el tapete
		force.scale(-Friction * m_Mass * m_Radius * G, perimeterVelocity);
		// Calcular torsion de la esfera
		torque.cross(r, force);
		torque.scale(100);
		torque.get(m_Torque);
		updateRotationState();
		
		// Resetear valores a 0 para evitar acumulacion de errores
		if (!isMoving()) {
			m_Velocity[0] = 0;
			m_Velocity[1] = 0;
			m_Velocity[2] = 0;
			m_AngularVelocity[0] = 0;
			m_AngularVelocity[1] = 0;
			m_AngularVelocity[2] = 0;
		}
	}
	
	/**
	 * Actualiza el estado de la esfera (movimiento angular).
	 */
	private void updateRotationState () {
		float dt = 0.009f;
		// K2 = K3
		float K1, K23, K4;
		//double L1, L2, L3, L4;
		// L1 = L2 = L3 = L4
		float L;

		for (int i = 0; i < 3; i++) {
			L = dt * f(i);
			K1 = dt * m_AngularVelocity[i];
			K23 = dt * (m_AngularVelocity[i] + L/2);
			K4 = dt * (m_AngularVelocity[i] + L);
			m_Angle[i] += (K1 + 4*K23 + K4) /6;
			m_AngularVelocity[i] += L;
		}
		m_AngularVelocity[0] *= 0.98f;
		m_AngularVelocity[1] *= 0.98f;
		m_AngularVelocity[2] *= 0.98f;
	}
	
	/**
	 * Funcion f para Runge-Kutta.
	 * @param i Indice de coordenada.
	 * @return Devuelve la aceleración angular en el eje i.
	 */
	private float f (int i) {
		return m_Torque[i] / InertiaTensor;
	}
	
	/**
	 * Actualiza la transformacion de la esfera.
	 */
	public void updateTransform() {
		Vector3f trans = new Vector3f(m_Position);
		Matrix3f rot = SpfScene.getRotationMatrixRad(m_Angle[0], m_Angle[1], m_Angle[2]);
		Transform3D t3d = new Transform3D(rot, trans, 1);
		m_Model.setTransform(t3d);
	}

	/**
	 * Actualiza el estado de la esfera en respuesta
	 * a una colision (version vista en clase).
	 * @param sphere Esfera con la que se colisiona.
	 * @param contactNormal Normal de contacto.
	 * @deprecated
	 */
	public void updateOnCollision2(SpfSphere sphere, Vector3f contactNormal) {
		contactNormal.normalize();
		
		Vector3f v1 = new Vector3f(m_Velocity);
		Vector3f v2 = new Vector3f(sphere.getVelocity());
		
		float v1Proj = v1.dot(contactNormal);
		float v2Proj = v2.dot(contactNormal);
		
		float den = (m_Mass*((1/m_Mass) + (3/sphere.getMass())));
		float vf = v1Proj + ((v2Proj - v1Proj)*(m_Elasticity + 1)) / den;
		
		Vector3f relativeInitialVelocity = new Vector3f(contactNormal);
		relativeInitialVelocity.scale(v1Proj);
		Vector3f relativeFinalVelocity = new Vector3f(contactNormal);
		relativeFinalVelocity.scale(vf);
		
		Vector3f tangentVelocity = new Vector3f();
		tangentVelocity.sub(v1, relativeInitialVelocity);
		
		Vector3f xAxis = new Vector3f(1, 0, 0);
		Vector3f zAxis = new Vector3f(0, 0, 1);
		m_Velocity[0] = relativeFinalVelocity.dot(xAxis) + tangentVelocity.dot(xAxis);
		m_Velocity[2] = relativeFinalVelocity.dot(zAxis) + tangentVelocity.dot(zAxis);
		
		Vector3f force = new Vector3f();
		Vector3f r1 = new Vector3f(contactNormal);
		Vector3f r2 = new Vector3f(contactNormal);
		Vector3f perimeterVelocity1 = new Vector3f();
		Vector3f perimeterVelocity2 = new Vector3f();
		Vector3f relativeVp = new Vector3f();
		Vector3f torque = new Vector3f();
		
		r1.scale(0.5f);
		r2.scale(-0.5f);
		perimeterVelocity1.cross(r1, new Vector3f(m_AngularVelocity));
		perimeterVelocity2.cross(r2, new Vector3f(sphere.getAngularVelocity()));
		relativeVp.sub(perimeterVelocity1, perimeterVelocity2);
		
		force.add(relativeVp, tangentVelocity);
		if (force.lengthSquared() != 0)
			force.normalize();
		force.scale(0.1f*v2Proj);
		torque.cross(r1, force);
		torque.scale(InertiaTensor*0.1f*25);
		torque.get(m_Torque);
		updateRotationState();
	}
	
	/**
	 * Actualiza el estado de la esfera en respuesta a una colision
	 * (version del libro "Physics for Game Developers"). Tiene
	 * en cuenta la respuesta lineal y angular.
	 * @param sphere Esfera con la que se colisiona.
	 * @param contactNormal Normal de contacto.
	 */
	public void updateOnCollision (SpfSphere sphere, Vector3f contactNormal) {
		contactNormal.normalize();
		
		Vector3f v1 = new Vector3f(m_Velocity);
		Vector3f v2 = new Vector3f(sphere.getVelocity());
		Vector3f w1 = new Vector3f(m_AngularVelocity);
		float v1Proj = v1.dot(contactNormal);
		float v2Proj = v2.dot(contactNormal);
		
		// Vector desde el centro de la esfera1
		// hasta la superficie de contacto.
		Vector3f r1 = new Vector3f(contactNormal);
		Vector3f r2 = new Vector3f(contactNormal);
		r1.scale(0.5f);
		r2.scale(-0.5f);
		
		// Realizar calculos intermedios para el denominador
		// de la formula del impulso
		Vector3f den1 = new Vector3f();
		den1.cross(r1, contactNormal);
		den1.scale(InertiaTensor);
		den1.cross(den1, r1);
		
		Vector3f den2 = new Vector3f();
		den2.cross(r2, contactNormal);
		den2.scale(InertiaTensor);
		den2.cross(den2, r2);
		
		float den = 1/m_Mass + 1/sphere.getMass();
		den += den1.dot(contactNormal) + den2.dot(contactNormal);
		// Calcular impulso lineal y angular
		float impulse = (v2Proj - v1Proj)/den;
		
		// Calcular cambio en la velocidad lineal
		Vector3f vChange = new Vector3f(contactNormal);
		vChange.scale(impulse/m_Mass);
		Vector3f v1f = new Vector3f();
		v1f.add(v1, vChange);
		v1f.get(m_Velocity);
		
		// Calcular cambio en la velocidad angular
		Vector3f wChange = new Vector3f(contactNormal);
		wChange.scale(impulse);
		wChange.cross(r1, wChange);
		wChange.scale(1/InertiaTensor);
		Vector3f w1f = new Vector3f();
		w1f.add(w1, wChange);
		w1f.get(m_AngularVelocity);
	}
}
