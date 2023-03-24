/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/**
 *
 * @author Trainee
 */
public final class XMLReadWrite {

    private Document dom = null;
    private String fileToBeParsed = null;
    private Element root;
    private final File f;

    public XMLReadWrite(String fileName) {
        fileToBeParsed = fileName;
        f = new File(fileToBeParsed);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            if (f.exists()) {
                dom = db.parse(f);
                root = dom.getDocumentElement();
            } else if (dom == null) {
                dom = db.newDocument();
                root = dom.createElement("root");
                dom.appendChild(root);
            }

        } catch (Exception e) {
            System.out.println("Exception in parseXmlFile : " + e);
        }

    }

    public String getTextByTag(String tag) {
        String text = null;
        NodeList nodes = root.getElementsByTagName(tag);
        Element ele = (Element) nodes.item(0);
        if (ele != null) {
            text = ele.getTextContent();
        }
        return text;
    }

    public String getTextByTag(String tag1, String tag2) {
        String text = null;
        NodeList nodes = root.getElementsByTagName(tag1);
        Element ele = (Element) nodes.item(0);

        if (ele != null) {
            nodes = ele.getElementsByTagName(tag2);
            ele = (Element) nodes.item(0);
            if (ele != null) {
                text = ele.getTextContent();
            }

        }
        return text;
    }

    public void printToFile() {
        FileOutputStream fileOutputStream = null;
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(dom);
            fileOutputStream = new FileOutputStream(f);
            StreamResult streamResult = new StreamResult(fileOutputStream);
            transformer.transform(source, streamResult);
            fileOutputStream.close();
        } catch (IOException ex) {
        } catch (TransformerException ex) {
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException ex) {
            }
        }
    }

    public void defineRoot(String tag) {
        String nodeName = root.getNodeName();
        if (!nodeName.equals(tag)) {
            dom.removeChild(root);
            root = dom.createElement(tag);
            dom.appendChild(root);
        }
    }

    public void Write(String tag, String text) {
        if (getTextByTag(tag) == null) {
            Element ele = dom.createElement(tag);
            ele.appendChild(dom.createTextNode(text));
            root.appendChild(ele);
        } else {
            Node node = root.getElementsByTagName(tag).item(0);
            if (node != null) {
                node.setTextContent(text);
            }
        }
    }

    public void Write(String tag1, String tag2, String text) {
        Node node1 = root.getElementsByTagName(tag1).item(0);
        Node node2 = ((Element) node1).getElementsByTagName(tag2).item(0);
        ((Element) node2).setTextContent(text);
    }
    
    public void Write(String tag1, String tag2, String[] text) {
        NodeList node1 = root.getElementsByTagName(tag1);
        for(int i=0;i<node1.getLength();i++){
            Node node2 = ((Element) node1.item(i)).getElementsByTagName(tag2).item(0);
            ((Element) node2).setTextContent(text[i]);
        }
        
    }

    public void removeAll(String tag) {
        NodeList nodelist = dom.getElementsByTagName(tag);
        int count = nodelist.getLength();
        for (int i = count - 1; i >= 0; i--) {
            root.removeChild(nodelist.item(i));
        }
    }

    public void add(String tag, String text) {
        Element ele = dom.createElement(tag);
        ele.appendChild(dom.createTextNode(text));
        root.appendChild(ele);
    }

    public String[] getAllTextValuesByTag(String tag) {
        NodeList nodes = dom.getElementsByTagName(tag);
        int size = nodes.getLength();
        String[] strArr = new String[size];
        for (int i = 0; i < size; i++) {
            Element ele = (Element) nodes.item(i);
            if (ele.getFirstChild() != null) {
                strArr[i] = ele.getFirstChild().getNodeValue();
            } else {
                strArr[i] = "";
            }
        }
        return strArr;
    }
}
