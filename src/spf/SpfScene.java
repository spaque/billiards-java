package spf;

import spf.xml.SpfXmlNode;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.universe.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import javax.media.j3d.*;
import javax.swing.tree.*;
import javax.vecmath.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import spf.objects.*;

/**
 * Implementa la carga de escenas a partir
 * ficheros de escena xml.
 * @author Sergio Paque Martin
 */
public class SpfScene {
	private HashMap<String, SpfObject> m_SceneMap;
	private Canvas3D m_Canvas3D;
	private boolean m_DynamicScene;
	
	// <editor-fold defaultstate="collapsed" desc="Constructor">
	
	/**
	 * Constructor de SpfScene.
	 * @param canvas Canvas3D que utiliza la aplicacion.
	 * @param dynamic Escena sujeta a dinamicas del juego.
	 */
	public SpfScene (Canvas3D canvas, boolean dynamic) {
		m_SceneMap = new HashMap<String, SpfObject>(32);
		m_Canvas3D = canvas;
		m_DynamicScene = dynamic;
	}
	
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Public Methods">
    
	/**
	 * @return Devuelve el mapa de objetos de la escena.
	 */
	public HashMap<String, SpfObject> getSceneMap () {
		return m_SceneMap;
	}
	
	/**
	 * Carga una escena a partir de un fichero xml.
	 * @param filename Nombre del fichero de la escena.
	 * @return Devuelve un BranchGroup con los elementos cargados.
	 */
	public BranchGroup loadSceneFromFile (String filename) {
		File file = new File(SpfApplication.getResourcesPath() + filename);
		return loadBranchGroup(checkSceneFile(file));
	}

	// </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Private Methods">
    
	/**
	 * Comprueba que un fichero de escena es correcto.
	 * @param file Fichero de escena.
	 * @return Devuelve el nodo xml para tratar el fichero.
	 */
    private SpfXmlNode checkSceneFile(File file) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder;
		Document doc;
		
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(file);
		} catch (Exception e) {
			throw new IllegalArgumentException("El formato del fichero no es correcto");
		}
        
        SpfXmlNode current = new SpfXmlNode(doc.getDocumentElement());
        if (current.getNodeName().compareTo("SPF") == 0) {
            current = current.getFirstChild();
            if (current.getNodeName().compareTo("Scene") == 0) {
                current = current.getFirstChild();
                if (current.getNodeName().compareTo("BranchGroup") == 0) {
                    return current;
                } else {
                    throw new IllegalArgumentException(
                        "El formato del fichero no es correcto");
                }
            } else {
                throw new IllegalArgumentException(
                        "El formato del fichero no es correcto");
            }
        } else {
            throw new IllegalArgumentException(
                    "El formato del fichero no es correcto");
        }
    }
    
	/**
	 * Carga un BranchGroup a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuele el BranchGroup leido del nodo xml.
	 */
	private BranchGroup loadBranchGroup (SpfXmlNode node) {
		BranchGroup bg = new BranchGroup() ;
		String type;

		if (node.getNodeName().compareTo("BranchGroup") == 0) {
			List<SpfXmlNode> childNodes = node.getChildNodes();
			for (SpfXmlNode current : childNodes) {
				type = current.getNodeName();
				if (type.compareTo("TransformGroup") == 0) {
					TransformGroup tg = loadTransformGroup(current);
					if (tg != null) {
						// Si la escena es dinamica, permitir que se
						// eliminen elementos de la misma.
						if (m_DynamicScene) {
							BranchGroup middle = new BranchGroup();
							middle.setCapability(BranchGroup.ALLOW_DETACH);
							tg.setCapability(TransformGroup.ALLOW_PARENT_READ);
							middle.addChild(tg);
							bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
							bg.addChild(middle);
						} else {
							bg.addChild(tg);
						}
					}
				} else if (type.compareTo("BranchGroup") == 0) {
					BranchGroup child_bg = loadBranchGroup(current);
					bg.addChild(child_bg);
				} else if (type.compareTo("AmbientLight") == 0) {
					bg.addChild(loadAmbLight(current));
				} else if (type.compareTo("DirectionalLight") == 0) {
					bg.addChild(loadDirLight(current));
				} else {
					throw new IllegalArgumentException(
						"El formato del fichero no es correcto");
				}
			}
		} else {
			throw new IllegalArgumentException(
					"El formato del fichero no es correcto");
		}
		return bg;
    }
	
	/**
	 * Carga un TransformGroup a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve el TransformGroup leido.
	 */
	private TransformGroup loadTransformGroup (SpfXmlNode node) {
		TransformGroup tg = new TransformGroup();
		// Si la escena es dinamica, permitir
		// cambios en las transformaciones.
		if (m_DynamicScene) {
			tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			tg.setCapabilityIsFrequent(TransformGroup.ALLOW_TRANSFORM_WRITE);
		}
		float rotX = 0, rotY = 0, rotZ = 0;
		Vector3f trans = new Vector3f(0, 0, 0);
		float scale = 1;

		String value = node.getAttribute("rotX");
		if (value.length() > 0) {
			rotX = Float.parseFloat(value);
		}
		value = node.getAttribute("rotY");
		if (value.length() > 0) {
			rotY = Float.parseFloat(value);
		}
		value = node.getAttribute("rotZ");
		if (value.length() > 0) {
			rotZ = Float.parseFloat(value);
		}
		value = node.getAttribute("transX");
		if (value.length() > 0) {
			trans.x = Float.parseFloat(value);
		}
		value = node.getAttribute("transY");
		if (value.length() > 0) {
			trans.y = Float.parseFloat(value);
		}
		value = node.getAttribute("transZ");
		if (value.length() > 0) {
			trans.z = Float.parseFloat(value);
		}
		value = node.getAttribute("scale");
		if (value.length() > 0) {
			scale = Float.parseFloat(value);
		}
		Matrix3f rotation = getRotationMatrix(rotX, rotY, rotZ);
		Transform3D t3d = new Transform3D(rotation, trans, scale);
		tg.setTransform(t3d);

		// Solo permitir un hijo en un TransformGroup.
		SpfXmlNode child = node.getFirstChild();
		if (child.getNodeName().compareTo("Object") == 0) {
			Node pr = loadPrimitive(child, tg);
			if (pr != null)
				tg.addChild(pr);
		}

		// Devolver solo en caso de que tenga hijos.
		if (tg.numChildren() > 0)
			return tg;
		return null;
	}
    
	/**
	 * Carga un objeto o primitiva a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @param tg Transformacion del objeto.
	 * @return Devuelve un nodo con el objeto leido.
	 */
    private Node loadPrimitive (SpfXmlNode node, TransformGroup tg) {
        String type = node.getAttribute("type");
		String name = node.getAttribute("name");
		
		if (type.compareToIgnoreCase("Box") == 0) {
			SpfBox box = new SpfBox(name, tg, m_Canvas3D);
			m_SceneMap.put(name, box);
			return box.load(node);
		} else if (type.compareToIgnoreCase("Cone") == 0) {
			SpfCone cone = new SpfCone(name, tg, m_Canvas3D);
			m_SceneMap.put(name, cone);
			return cone.load(node);
		} else if (type.compareToIgnoreCase("Cylinder") == 0) {
			SpfCylinder cylinder = new SpfCylinder(name, tg, m_Canvas3D);
			m_SceneMap.put(name, cylinder);
			return cylinder.load(node);
		} else if (type.compareToIgnoreCase("Sphere") == 0) {
			SpfSphere sphere = new SpfSphere(name, tg, m_Canvas3D);
			m_SceneMap.put(name, sphere);
			return sphere.load(node);
		} else if (type.compareToIgnoreCase("OBJ") == 0) {
			SpfWavefrontObj obj = new SpfWavefrontObj(tg, m_Canvas3D);
			return obj.load(node);
		} else if (type.compareTo("Quad") == 0) {
			SpfQuad quad = new SpfQuad(m_Canvas3D);
			return quad.load(node);
		}
        return null;
    }
	
	/**
	 * Calcula una matriz de rotacion a partir de la
	 * rotacion en sobre cada uno de los ejes cartesianos.
	 * @param rotX Rotacion sobre el eje X en grados.
	 * @param rotY Rotacion sobre el eje Y en grados.
	 * @param rotZ Rotacion sobre el eje Z en grados.
	 * @return Devuelve la matriz de rotacion.
	 */
    public static Matrix3f getRotationMatrix (float rotX, float rotY, float rotZ) {
        Matrix3f rotationX = new Matrix3f();
        Matrix3f rotationY = new Matrix3f();
        Matrix3f rotationZ = new Matrix3f();
        
        rotationX.setIdentity();
        rotationY.setIdentity();
        rotationZ.setIdentity();
        if (rotX != 0)
            rotationX.rotX((float)Math.toRadians(rotX));
        if (rotY != 0)
            rotationY.rotY((float)Math.toRadians(rotY));
        if (rotZ != 0)
            rotationZ.rotZ((float)Math.toRadians(rotZ));
        rotationX.mul(rotationY);
        rotationX.mul(rotationZ);
        return rotationX;
    }
	
	/**
	 * Calcula una matriz de rotacion a partir de la
	 * rotacion en sobre cada uno de los ejes cartesianos.
	 * @param rotX Rotacion sobre el eje X en radianes.
	 * @param rotY Rotacion sobre el eje Y en radianes.
	 * @param rotZ Rotacion sobre el eje Z en radianes.
	 * @return Devuelve la matriz de rotacion.
	 */
    public static Matrix3f getRotationMatrixRad (float rotX, float rotY, float rotZ) {
        Matrix3f rotationX = new Matrix3f();
        Matrix3f rotationY = new Matrix3f();
        Matrix3f rotationZ = new Matrix3f();
        
        rotationX.setIdentity();
        rotationY.setIdentity();
        rotationZ.setIdentity();
        if (rotX != 0)
            rotationX.rotX(rotX);
        if (rotY != 0)
            rotationY.rotY(rotY);
        if (rotZ != 0)
            rotationZ.rotZ(rotZ);
        rotationX.mul(rotationY);
        rotationX.mul(rotationZ);
        return rotationX;
    }
	
	/**
	 * Carga una luz ambiental a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve la luz ambiental leida.
	 */
	private AmbientLight loadAmbLight (SpfXmlNode node) {
		float r = 1, g = 1, b = 1;
		String attribute;
		AmbientLight amb;
		
		attribute = node.getAttribute("r");
		if (attribute.length() > 0)
			r = Float.parseFloat(attribute);
		attribute = node.getAttribute("g");
		if (attribute.length() > 0)
			g = Float.parseFloat(attribute);
		attribute = node.getAttribute("b");
		if (attribute.length() > 0)
			b = Float.parseFloat(attribute);
		
		amb = new AmbientLight(new Color3f(r, g, b));
		BoundingSphere bounds = new BoundingSphere(new Point3d(), 100.0d);
		amb.setInfluencingBounds(bounds);
		return amb;
	}
	
	/**
	 * Carga una luz direccional a partir de un nodo xml.
	 * @param node Nodo xml actual.
	 * @return Devuelve la luz direccional leida.
	 */
	private DirectionalLight loadDirLight (SpfXmlNode node) {
		float r = 1, g = 1, b = 1;
		float dirX = 0, dirY = 0, dirZ = -1;
		String attribute;
		DirectionalLight dir;
		
		attribute = node.getAttribute("r");
		if (attribute.length() > 0)
			r = Float.parseFloat(attribute);
		attribute = node.getAttribute("g");
		if (attribute.length() > 0)
			g = Float.parseFloat(attribute);
		attribute = node.getAttribute("b");
		if (attribute.length() > 0)
			b = Float.parseFloat(attribute);
		
		attribute = node.getAttribute("dirX");
		if (attribute.length() > 0)
			dirX = Float.parseFloat(attribute);
		attribute = node.getAttribute("dirY");
		if (attribute.length() > 0)
			dirY = Float.parseFloat(attribute);
		attribute = node.getAttribute("dirZ");
		if (attribute.length() > 0)
			dirZ = Float.parseFloat(attribute);
		
		Vector3f direction = new Vector3f(dirX, dirY, dirZ);
		direction.normalize();
		dir = new DirectionalLight(new Color3f(r, g, b), direction);
		BoundingSphere bounds = new BoundingSphere(new Point3d(), 100.0d);
		dir.setInfluencingBounds(bounds);
		return dir;
	}

    // </editor-fold>
}
