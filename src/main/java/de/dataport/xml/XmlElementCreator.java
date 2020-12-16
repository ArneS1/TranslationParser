package de.dataport.xml;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.util.List;

public class XmlElementCreator {

    public Element createEmptyXML() {
        //TODO: create empty line
        return null;
    }

    public Comment createCommentXML(Document doc, String line) {
        String content = line.substring(4, line.length() - 3);
        return doc.createComment(content);
    }

    public Element createContentXML(Document doc, String line) {
        String name = line.substring(0, line.indexOf(';'));
        String content = line.substring(line.indexOf(';') + 1);

        Element newElement = doc.createElement("string");
        newElement.setAttribute("name", name);
        newElement.setTextContent(content);

        return newElement;
    }

    public Element createPluralXML(Document doc, List<String> lines) {
        Element plurals = doc.createElement("plurals");
        plurals.setAttribute("name", lines.get(0).substring(0, lines.get(0).indexOf('#')));
        lines.forEach(line ->{
            String quantity =  line.substring(line.indexOf('#')+1, line.indexOf(';'));
            String content = line.substring(line.indexOf(';')+1, line.length());
            Element item = doc.createElement("item");
            item.setAttribute("quantity", quantity);
            item.setTextContent(content);
            plurals.appendChild(item);
        });
        return plurals;
    }

}
