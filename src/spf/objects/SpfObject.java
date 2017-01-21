package spf.objects;

import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.image.TextureLoader;
import java.util.List;
import javax.media.j3d.Appearance;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;
import spf.SpfApplication;
import spf.xml.SpfXmlNode;

import static spf.SpfConstants.*;

/**
 * Clase base para representar las posibles primitivas.
 * @author Sergio Paque Martin
 */
public abstract class SpfObject {
	protected String m_Name;
	protected float m_Mass, m_Elasticity;
	protected float[] m_Force, m_Velocity, m_Position;
	protected TransformGroup m_Model;
	protected Primitive m_Primitive;
	protected Appearance m_Appearance;
	protected Canvas3D m_Canvas3D;
	
	protected float[] m_MinCoord = new float[] {-2.2f, -0.82f, -1.15f};
	protected float[] m_MaxCoord = new float[] {2.2f, -0.82f, 1.15f};
	
	/**
	 * Carga un objeto dado un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve el nodo con el objeto leido.
	 */
	public abstract Node load (SpfXmlNode node);
	
	/**
	 * @return Devuelve el nombre del objeto.
	 */
	public String getName() {
		return m_Name;
	}
	
	/**
	 * @return Devuelve la masa del objeto.
	 */
	public float getMass () {
		return m_Mass;
	}
	
	/**
	 * @return Devuelve la transformacion del objeto.
	 */
	public TransformGroup getTransformGroup () {
		return m_Model;
	}
	
	/**
	 * Establece la transformacion del objeto.
	 * @param t3d Nueva transformacion.
	 */
	public void setTransform (Transform3D t3d) {
		m_Model.setTransform(t3d);
	}
	
	/**
	 * @return Devuelve la posicion del objeto.
	 */
	public float[] getPosition () {
		return m_Position;
	}
	
	/**
	 * @return Devuelve la velocidad del objeto.
	 */
	public float[] getVelocity () {
		return m_Velocity;
	}
	
	/**
	 * Aplica una fuerza al objeto.
	 * @param force Vector fuerza.
	 */
	public void applyForce (Vector3f force) {
		force.get(m_Force);
	}
	
	/**
	 * @return Devuelve si el objeto esta en movimiento.
	 */
	public boolean isMoving () {
		Vector3f velocity = new Vector3f(m_Velocity);
		return velocity.lengthSquared() >= 0.0001f;
	}
	
	/**
	 * Actualiza el estado del objeto (movimiento lineal).
	 */
	public void updateState () {
		float dt = 0.009f;
		float K1, K23, K4;
		//double L1, L2, L3, L4;
		// L1 = L2 = L3 = L4
		float L;
		float deltap;
		
		for (int i = 0; i < 3; i++) {
			L = dt * f(i);
			K1 = dt * m_Velocity[i];
			K23 = dt * (m_Velocity[i] + L/2);
			K4 = dt * (m_Velocity[i] + L);
			deltap = (K1 + 4*K23 + K4) / 6;
				m_Velocity[i] += L;
			if (m_Position[i] + deltap > m_MinCoord[i] &&
					m_Position[i] + deltap < m_MaxCoord[i]) {
				m_Position[i] += deltap;
			} else {
				m_Velocity[i] *= -1;
			}
		}
		//m_Velocity[0] *= 0.98f;
		//m_Velocity[2] *= 0.98f;
	}
	
	/**
	 * Funcion f para Runge-Kutta.
	 * @param i Indice de coordenada.
	 * @return Devuelve la aceleración en el eje i.
	 */
	private float f (int i) {
		// Calcular fuerza de friccion
		float friction = -0.05f * m_Mass * G * m_Velocity[i];
		return (m_Force[i] + friction)/m_Mass;
	}
	
	/**
	 * Carga la apariencia del objeto a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 */
	protected void loadAppearance (SpfXmlNode node) {
		List<SpfXmlNode> childNodes = node.getChildNodes();
		m_Appearance = new Appearance();
		
		for (SpfXmlNode current : childNodes) {
			String type = current.getNodeName();
			if (type.compareTo("Color") == 0) {
				loadColor(current);
			} else if (type.compareTo("Material") == 0) {
				loadMaterial(current);
			} else if (type.compareTo("Texture") == 0) {
				loadTexture(current);
			} else if (type.compareTo("Transparency") == 0) {
				loadTransparency(current);
			}
		}
	}
	
	/**
	 * Carga la transparencia del objeto a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 */
	private void loadTransparency (SpfXmlNode node) {
		TransparencyAttributes ta = new TransparencyAttributes();
		float value = 0;
		ta.setTransparencyMode(TransparencyAttributes.FASTEST);
		
		String attribute = node.getAttribute("value");
		if (attribute.length() > 0) {
			value = Float.parseFloat(attribute);
		}
		ta.setTransparency(value);
		m_Appearance.setTransparencyAttributes(ta);
	}
	
	/**
	 * Carga el color del objeto a partir un nodo xml.
	 * @param node Nodo xml actual.
	 */
	private void loadColor (SpfXmlNode node) {
		ColoringAttributes color;
		
		color = new ColoringAttributes(readColor(node), ColoringAttributes.NICEST);
		m_Appearance.setColoringAttributes(color);
	}
	
	/**
	 * Carga el material del objeto a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 */
	private void loadMaterial (SpfXmlNode node) {
		List<SpfXmlNode> childNodes = node.getChildNodes();
		Material mat = new Material();
		String type;
		
		for (SpfXmlNode current : childNodes) {
			type = current.getNodeName();
			if (type.compareTo("Ambient") == 0) {
				mat.setAmbientColor(readColor(current));
			} else if (type.compareTo("Diffuse") == 0) {
				mat.setDiffuseColor(readColor(current));
			} else if (type.compareTo("Emissive") == 0) {
				mat.setEmissiveColor(readColor(current));
			} else if (type.compareTo("Shininess") == 0) {
				String attribute = current.getAttribute("s");
				if (attribute.length() > 0)
					mat.setShininess(Float.parseFloat(attribute));
				else
					mat.setShininess(64);
			} else if (type.compareTo("Specular") == 0) {
				mat.setSpecularColor(readColor(current));
			}
		}
		m_Appearance.setMaterial(mat);
	}
	
	/**
	 * Carga la textura del objeto a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 */
	private void loadTexture (SpfXmlNode node) {
		String filename = node.getAttribute("filename");
		if (filename.length() > 0) {
			String path = SpfApplication.getResourcesPath() + filename;
			String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
			int flags;
			String format;
			if (extension.endsWith(".png") || extension.endsWith(".gif")) {
				flags = Texture.RGBA;
				format = "RGBA";
			} else {
				flags = Texture.RGB;
				format = "RGB";
			}
			
			TextureLoader loader = 
					new TextureLoader(
						path, 
						format, 
						TextureLoader.GENERATE_MIPMAP, 
						m_Canvas3D);
			ImageComponent2D image = loader.getImage();
			int imageWidth = image.getWidth();
			int imageHeight = image.getHeight();
			int imageLevel = 0;
			
			Texture2D texture = 
					new Texture2D(
						Texture.MULTI_LEVEL_MIPMAP,
						flags,
						imageWidth, imageHeight);
			texture.setImage(imageLevel, image);
			// ejecutar hasta tener tamaño 1x1
			while (imageWidth > 1 || imageHeight > 1) {
				imageLevel++;

				if (imageWidth > 1) imageWidth = imageWidth >> 1;
				if (imageHeight > 1) imageHeight = imageHeight >> 1;

				image = loader.getScaledImage(imageWidth, imageHeight);
				texture.setImage(imageLevel, image);
			}
			texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
			texture.setMinFilter(Texture.MULTI_LEVEL_LINEAR);
			
			// Modular color de la textura con el del material.
			TextureAttributes texAtt = new TextureAttributes();
			texAtt.setTextureMode(TextureAttributes.MODULATE);
			
			m_Appearance.setTexture(texture);
			m_Appearance.setTextureAttributes(texAtt);
		}
	}
	
	/**
	 * Lee un color a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve el color leido.
	 */
	private Color3f readColor (SpfXmlNode node) {
		float r = 1, g = 1, b = 1;
		String attribute;
		
		attribute = node.getAttribute("r");
		if (attribute.length() > 0)
			r = Float.parseFloat(attribute);
		attribute = node.getAttribute("g");
		if (attribute.length() > 0)
			g = Float.parseFloat(attribute);
		attribute = node.getAttribute("b");
		if (attribute.length() > 0)
			b = Float.parseFloat(attribute);
		
		return new Color3f(r, g, b);
	}
}
