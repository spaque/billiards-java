package spf.objects;

import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.objectfile.ObjectFile;
import com.sun.j3d.utils.image.TextureLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.media.j3d.Appearance;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Group;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
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

/**
 * Clase que representa un objeto cualquiera en formato
 * Wavefront, cargado a partir de un fichero OBJ y MTL.
 * @author Sergio Paque Martin
 */
public class SpfWavefrontObj extends SpfObject {
	private String m_BasePath;
	/// Mapa de materiales: nombre_grupo --> nombre_material
	private HashMap<String, String> m_ObjMat;
	/// Mapa de apariencias: nombre_material --> apariencia_material
	private HashMap<String, Appearance> m_ObjApp;
	private Node m_Node;
	
	/**
	 * Contructor de SpfWavefrontObj.
	 * @param tg Transformacion inicial del objeto.
	 * @param canvas Canvas3D que utiliza la aplicacion principal.
	 */
	public SpfWavefrontObj (TransformGroup tg, Canvas3D canvas) {
		m_Mass = 1;
		m_Elasticity = 1;
		m_Force = new float[3];
		m_Velocity = new float[3];
		m_Position = new float[3];
		m_Model = tg;
		m_Canvas3D = canvas;
		m_BasePath = SpfApplication.getResourcesPath();
		m_Node = new Group();
	}
	
	/**
	 * @return Devuelve un nodo con el objeto.
	 */
	public Node getNode () {
		return m_Node;
	}
	
	/**
	 * Carga un modelo Wavefront a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve un nodo con el modelo leido.
	 */
	public Node load(SpfXmlNode node) {
		ObjectFile obj = new ObjectFile(ObjectFile.RESIZE | ObjectFile.STRIPIFY);
		String filename = node.getAttribute("filename");
		Scene scn = null;
		try {
			scn = obj.load(m_BasePath + filename);
			loadMaterials(filename);
			// Si esta especificado el fichero de
			// materiales, cargarlo
			String matlib = node.getAttribute("matlib");
			if (matlib.length() > 0) {
				loadAppearances(matlib);
				setSceneAppearance(scn);
			}
		} catch (Exception e) {
			return null;
		}
		m_ObjApp = null;
		m_ObjMat = null;
		m_Node = scn.getSceneGroup();
		
		Vector3f position = new Vector3f();
		Transform3D t3d = new Transform3D();
		m_Model.getTransform(t3d);
		t3d.get(position);
		position.get(m_Position);
		
		return m_Node;
	}
	
	/**
	 * Carga un modelo Wavefront a partir del nombre 
	 * del fichero OBJ y el fichero MTL.
	 * @param filename Nombre del fichero OBJ.
	 * @param matlib Nombre del fichero MTL.
	 * @return
	 */
	public Node load (String filename, String matlib) {
		ObjectFile obj = new ObjectFile(ObjectFile.RESIZE | ObjectFile.STRIPIFY);
		Scene scn = null;
		try {
			scn = obj.load(m_BasePath + filename);
			loadMaterials(filename);
			if (matlib.length() > 0) {
				loadAppearances(matlib);
				setSceneAppearance(scn);
			}
		} catch (Exception e) {
			return null;
		}
		m_ObjApp = null;
		m_ObjMat = null;
		m_Node = scn.getSceneGroup();
		
		if (m_Model != null) {
			Vector3f position = new Vector3f();
			Transform3D t3d = new Transform3D();
			m_Model.getTransform(t3d);
			t3d.get(position);
			position.get(m_Position);
		}
		
		return m_Node;
	}

	/**
	 * Carga los materiales utilizados por el modelo.
	 * @param filename Nombre del fichero OBJ.
	 * @throws java.io.IOException
	 */
	private void loadMaterials (String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(m_BasePath + filename));
		String shapeName, shapeMaterial;
		String line;
		StringTokenizer st;
		m_ObjMat = new HashMap<String, String>(128);
		
		line = br.readLine();
		while ( line != null ) {
			// Buscar lineas que definan grupos con nombre
			if (line.startsWith("g")) {
				st = new StringTokenizer(line, " ");
				if (st.countTokens() > 1) {
					st.nextToken(); // g
					shapeName = st.nextToken();
					line = br.readLine();
					st = new StringTokenizer(line, " ");
					st.nextToken(); // usemtl
					shapeMaterial = st.nextToken();
					m_ObjMat.put(shapeName.toLowerCase(), shapeMaterial);
				}
			}
			line = br.readLine();
		}
		br.close();
	}
	
	/**
	 * Carga para cada material la apariencia asociada.
	 * @param matlib Nombre del fichero MTL.
	 * @throws java.io.IOException
	 */
	private void loadAppearances (String matlib) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(m_BasePath + matlib));
		String material;
		String line;
		StringTokenizer st;
		m_ObjApp = new HashMap<String, Appearance>(32);

		line = br.readLine();
		while (line != null) {
			// Buscar lineas que definan materiales
			if (line.startsWith("newmtl")) {
				st = new StringTokenizer(line, " ");
				st.nextToken(); // newmtl
				material = st.nextToken();
				Appearance ap = readMaterial(br);
				m_ObjApp.put(material, ap);
			}
			line = br.readLine();
		}
		br.close();
	}

	/**
	 * Lee un material a partir de un Reader.
	 * @param br Reader para leer el fichero.
	 * @return Devuelve la apariencia del material.
	 * @throws java.io.IOException
	 */
	private Appearance readMaterial(BufferedReader br) throws IOException {
		Appearance ap = new Appearance();
		Color3f Ka = new Color3f(0.2f, 0.2f, 0.2f);
		Color3f Kd = new Color3f(0.8f, 0.8f, 0.8f);
		Color3f Ks = new Color3f(0, 0, 0);
		float Ns = 1;
		int ilum = 0;
		float d = 1;
		String texture = null;
		String line;
		
		line = br.readLine();
		while (line != null && !line.startsWith("#") && !line.isEmpty()) {
			// Componente ambiental
			if (line.startsWith("Ka")) {
				Ka = readColor(line);
			// Componente difusa
			} else if (line.startsWith("Kd")) {
				Kd = readColor(line);
			// Componente especular
			} else if (line.startsWith("Ks")) {
				Ks = readColor(line);
			// Brillo
			} else if (line.startsWith("Ns")) {
				StringTokenizer st = new StringTokenizer(line, " ");
				st.nextToken(); // Ns
				Ns = Float.parseFloat(st.nextToken());
				if (Ns == 0) Ns = 32;
			// Tipo de iluminacion
			} else if (line.startsWith("illum")) {
				StringTokenizer st = new StringTokenizer(line, " ");
				st.nextToken(); // illum
				ilum = Integer.parseInt(st.nextToken());
			// Mapa difuso, textura
			} else if (line.startsWith("map_Kd")) {
				StringTokenizer st = new StringTokenizer(line, " ");
				st.nextToken(); // map_Kd
				texture = st.nextToken();
			// Transparencia
			} else if (line.startsWith("d")) {
				StringTokenizer st = new StringTokenizer(line, " ");
				st.nextToken(); // d
				d = Float.parseFloat(st.nextToken());
			}
			line = br.readLine();
		}
		
		Material mat = new Material();
		if (ilum == 2) {
			mat.setSpecularColor(Ks);
			mat.setShininess(Ns);
			ilum--;
		} else {
			mat.setSpecularColor(0, 0, 0);
			mat.setShininess(1);
		}
		if (ilum == 1) {
			mat.setAmbientColor(Ka);
			mat.setDiffuseColor(Kd);
		} else {
			mat.setAmbientColor(0, 0, 0);
			mat.setDiffuseColor(0, 0, 0);
		}
		ap.setMaterial(mat);
		if (d < 1.0f) {
			TransparencyAttributes ta = 
					new TransparencyAttributes(TransparencyAttributes.BLENDED, d);
			ap.setTransparencyAttributes(ta);
		}
		if (texture != null && texture.compareTo("Undefined") != 0)
			setTexture(ap, texture);
		
		return ap;
	}
	
	/**
	 * Lee un color a patir de un string.
	 * @param line Cadena con las componentes rgb
	 * @return Devuelve el color leido.
	 */
	private Color3f readColor(String line) {
		float r, g, b;
		StringTokenizer st = new StringTokenizer(line, " ");
		st.nextToken();
		r = Float.parseFloat(st.nextToken());
		g = Float.parseFloat(st.nextToken());
		b = Float.parseFloat(st.nextToken());
		return new Color3f(r, g, b);
	}

	/**
	 * Carga una textura.
	 * @param app Apariencia para establecer la textura.
	 * @param filename Nombre del fichero de la textura.
	 */
	private void setTexture(Appearance app, String filename) {
		String path = m_BasePath + filename;
		
		File file = new File(path);
		if (!file.exists()) return;
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
		
		// Generar mipmaps
		Texture2D texture = 
					new Texture2D(
						Texture.MULTI_LEVEL_MIPMAP,
						flags,
						imageWidth, imageHeight);
		texture.setImage(imageLevel, image);
		// Ejecutar hasta tamaño 1x1
		while (imageWidth > 1 || imageHeight > 1) {
			imageLevel++;

			if (imageWidth > 1) imageWidth = imageWidth >> 1;
			if (imageHeight > 1) imageHeight = imageHeight >> 1;

			image = loader.getScaledImage(imageWidth, imageHeight);
			texture.setImage(imageLevel, image);
		}
		texture.setMagFilter(Texture.FASTEST);
		texture.setMinFilter(Texture.FASTEST);
		
		TextureAttributes texAtt = new TextureAttributes();
		texAtt.setTextureMode(TextureAttributes.MODULATE);
		
		app.setTexture(texture);
		app.setTextureAttributes(texAtt);
	}

	/**
	 * Establece la apariencia del objeto a
	 * partir de los mapas creados.
	 * @param scn Escena que contiene la geometria del objeto.
	 */
	private void setSceneAppearance(Scene scn) {
		Map<String, Shape3D> nameMap = scn.getNamedObjects();
		Set<String> keys = nameMap.keySet();
		String material;
		Shape3D shape;
		for (String name : keys) {
			material = m_ObjMat.get(name);
			if (m_ObjApp.containsKey(material)) {
				shape = nameMap.get(name);
				shape.setAppearance(m_ObjApp.get(material));
			}
		}
	}
}
