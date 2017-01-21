package spf.objects;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.Node;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import spf.xml.SpfXmlNode;

/**
 * Clase que representa un quad.
 * @author Sergio Paque Martin
 */
public class SpfQuad extends SpfObject {
	
	/**
	 * Constructor de SpfQuad.
	 * @param canvas Canvas3D que utiliza la aplicacion principal.
	 */
	public SpfQuad (Canvas3D canvas) {
		m_Canvas3D = canvas;
	}

	/**
	 * Carga un quad a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve un nodo con el quad leido.
	 */
	public Node load(SpfXmlNode node) {
		float xdim = 1, ydim = 1;
		float s = 1, t = 1;
		String attribute;
		
		loadAppearance(node);
		
		attribute = node.getAttribute("width");
		if (attribute.length() > 0)
			xdim = Float.parseFloat(attribute);
		attribute = node.getAttribute("height");
		if (attribute.length() > 0)
			ydim = Float.parseFloat(attribute);
		attribute = node.getAttribute("s");
		if (attribute.length() > 0)
			s = Float.parseFloat(attribute);
		attribute = node.getAttribute("t");
		if (attribute.length() > 0)
			t = Float.parseFloat(attribute);
		
		xdim /= 2;
		ydim /= 2;
		
		// Formato de la geometria.
		QuadArray quad = new QuadArray(4, 
				QuadArray.COORDINATES | 
				QuadArray.NORMALS |
				QuadArray.TEXTURE_COORDINATE_2);
		// Generar coordenadas.
		quad.setCoordinate(0, new float[] {-xdim, -ydim, 0});
		quad.setCoordinate(1, new float[] {xdim, -ydim, 0});
		quad.setCoordinate(2, new float[] {xdim, ydim, 0});
		quad.setCoordinate(3, new float[] {-xdim, ydim, 0});
		// Generar normales.
		quad.setNormal(0, new float[] {0, 0, 1});
		quad.setNormal(1, new float[] {0, 0, 1});
		quad.setNormal(2, new float[] {0, 0, 1});
		quad.setNormal(3, new float[] {0, 0, 1});
		// Generar coordenadas de texturas.
		quad.setTextureCoordinate(0, 0, new float[] {0, 0});
		quad.setTextureCoordinate(0, 1, new float[] {s, 0});
		quad.setTextureCoordinate(0, 2, new float[] {s, t});
		quad.setTextureCoordinate(0, 3, new float[] {0, t});
		
		if (m_Appearance != null)
			return new Shape3D(quad, m_Appearance);
		return new Shape3D(quad);
	}

}
