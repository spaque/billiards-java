package spf.objects;

import com.sun.j3d.utils.geometry.Cone;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;
import spf.xml.SpfXmlNode;

/**
 * Clase que representa una primitiva "cono".
 * @author Sergio Paque Martin
 */
public class SpfCone extends SpfObject {

	/**
	 * Constructor de SpfCone.
	 * @param name Nombre del objeto.
	 * @param tg Transformacion inicial del objeto.
	 * @param canvas Canvas3D que utiliza la aplicacion principal.
	 */
	public SpfCone (String name, TransformGroup tg, Canvas3D canvas) {
		m_Name = name;
		m_Mass = 1;
		m_Elasticity = 1;
		m_Force = new float[3];
		m_Velocity = new float[3];
		m_Position = new float[3];
		m_Model = tg;
		m_Canvas3D = canvas;
	}
	
	/**
	 * Carga un cono a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve un nodo con el cono leido.
	 */
	public Node load (SpfXmlNode node) {
		float radius = 1, height = 2;
		String attribute;

		loadAppearance(node);
		
		attribute = node.getAttribute("radius");
		if (attribute.length() > 0)
			radius = Float.parseFloat(attribute);
		attribute = node.getAttribute("height");
		if (attribute.length() > 0)
			height = Float.parseFloat(attribute);
		
		if (m_Appearance != null) {
			int primflags = Cone.GENERATE_NORMALS | Cone.GENERATE_TEXTURE_COORDS;
			m_Primitive = new Cone(radius, height, primflags, m_Appearance);
		} else {
			m_Primitive = new Cone(radius, height);
		}
		
		Vector3f position = new Vector3f();
		Transform3D t3d = new Transform3D();
		m_Model.getTransform(t3d);
		t3d.get(position);
		position.get(m_Position);
		
		return m_Primitive;
	}
}
