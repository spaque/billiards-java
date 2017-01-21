package spf.objects;

import com.sun.j3d.utils.geometry.Box;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;
import spf.xml.SpfXmlNode;

/**
 * Clase que representa una primitiva "caja".
 * @author Sergio Paque Martin
 */
public class SpfBox extends SpfObject {

	/**
	 * Constructor de SpfBox.
	 * @param name Nombre del objeto.
	 * @param tg Transformacion inicial del objeto.
	 * @param canvas Canvas3D que utiliza la aplicacion principal.
	 */
	public SpfBox (String name, TransformGroup tg, Canvas3D canvas) {
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
	 * Carga una caja a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve un nodo con la caja.
	 */
	public Node load (SpfXmlNode node) {
		float xdim = 1, ydim = 1, zdim = 1;
		String attribute;
		
		loadAppearance(node);
		
		attribute = node.getAttribute("width");
		if (attribute.length() > 0)
			xdim = Float.parseFloat(attribute);
		attribute = node.getAttribute("height");
		if (attribute.length() > 0)
			ydim = Float.parseFloat(attribute);
		attribute = node.getAttribute("depth");
		if (attribute.length() > 0)
			zdim = Float.parseFloat(attribute);
		
		if (m_Appearance != null) {
			int primflags = Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS;
			m_Primitive = new Box(xdim, ydim, zdim, primflags, m_Appearance);
		} else {
			m_Primitive = new Box();
		}
		
		Vector3f position = new Vector3f();
		Transform3D t3d = new Transform3D();
		m_Model.getTransform(t3d);
		t3d.get(position);
		position.get(m_Position);
		
		return m_Primitive;
	}
}
