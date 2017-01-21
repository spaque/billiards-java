package spf.xml;

import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Clase para realizar el analisis de ficheros xml.
 * @author Sergio Paque Martin
 */
public class SpfXmlNode {
    private Node m_Node;
    private List<Node> m_ChildNodes;

	/**
	 * Constructor de SpfXmlNode.
	 * @param node Nodo estandar xml.
	 */
    public SpfXmlNode(Node node) {
        m_Node = node;
        m_ChildNodes = new LinkedList<Node>();

        NodeList list = m_Node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() != Node.TEXT_NODE)
                m_ChildNodes.add(list.item(i));
        }
    }
    
	/**
	 * @return Devuelve el primer nodo hijo del nodo actual.
	 */
    public SpfXmlNode getFirstChild () {
        if (m_ChildNodes.isEmpty())
            return null;
        else
            return new SpfXmlNode(m_ChildNodes.get(0));
    }
    
	/**
	 * @return Devuelve los nodos hijo del nodo actual.
	 */
    public List<SpfXmlNode> getChildNodes () {
        List<SpfXmlNode> list = new LinkedList<SpfXmlNode>();
        for(Node node : m_ChildNodes) {
            list.add(new SpfXmlNode(node));
        }
        return list;
    }
    
	/**
	 * @return Devuelve el nombre del nodo actual.
	 */
    public String getNodeName () {
        return m_Node.getNodeName();
    }
    
	/**
	 * Obtiene el valor de un atributo o etiqueta
	 * de un nodo xml.
	 * @param name Nombre del atributo.
	 * @return Devuelve el valor asociado al atributo.
	 */
    public String getAttribute (String name) {
        NamedNodeMap attr = m_Node.getAttributes();
        Node attrNode = attr.getNamedItem(name);
        if (attrNode != null) {
            return attrNode.getNodeValue();
        } else {
            return "";
        }
    }
}
