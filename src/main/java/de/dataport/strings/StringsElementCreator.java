package de.dataport.strings;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

public class StringsElementCreator {

    public String createContentString(String line) {
        String name = line.substring(0, line.indexOf(';'));
        String content = line.substring(line.indexOf(';') + 1);

        String newElement = "\"" + name + "\"";
        newElement += " = ";
        newElement += "\"" + content + "\";";

        return newElement;
    }

    public String createCommentString(String line) {
        String content = line.substring(4, line.length() - 3);
        return "/* " + content + " */";
    }

    public Document createPluralsFile(List<List<String>> plurals){
        Document file = createDoc();
        assert file != null;

        Element rootPlist = createXmlElement(file, "plist","version","1.0", null);
        file.appendChild(rootPlist);

        Element dict = file.createElement("dict");
        rootPlist.appendChild(dict);

        plurals.forEach(plural -> {
            String name = plural.get(0).substring(0,plural.get(0).indexOf('#'));
            final String[] zero = new String[1];
            zero[0] = "";
            final String[] one = new String[1];
            one[0] = "";
            final String[] two = new String[1];
            two[0] = "";
            final String[] few = new String[1];
            few[0] = "";
            final String[] many = new String[1];
            many[0] = "";
            final String[] other = new String[1];
            other[0] = "";
            plural.forEach(line -> {
                String substring = line.substring(line.indexOf(';') + 1, line.length());
                if(line.contains("#zero")) zero[0] = substring;
                if(line.contains("#one")) one[0] = substring;
                if(line.contains("#two")) two[0] = substring;
                if(line.contains("#few")) few[0] = substring;
                if(line.contains("#many")) many[0] = substring;
                if(line.contains("#other")) other[0] = substring;
            });

            dict.appendChild(createXmlElement(file, "key",null,null, name));

            Element rootDict = file.createElement("dict");
            dict.appendChild(rootDict);

            rootDict.appendChild(createXmlElement(file, "key",null,null, "NSStringLocalizedFormatKey"));
            rootDict.appendChild(createXmlElement(file, "string",null,null, "%#@" + name + "@"));
            rootDict.appendChild(createXmlElement(file, "key",null,null, name));

            Element pluralDict = createXmlElement(file, "dict",null,null, null);
            rootDict.appendChild(pluralDict);

            pluralDict.appendChild(createXmlElement(file,"key",null,null,"NSStringFormatSpecTypeKey"));
            pluralDict.appendChild(createXmlElement(file,"string",null,null,"NSStringPluralRuleType"));
            pluralDict.appendChild(createXmlElement(file,"key",null,null,"NSStringFormatValueTypeKey"));
            pluralDict.appendChild(createXmlElement(file,"string",null,null,"d"));

            pluralDict.appendChild(createXmlElement(file, "key", null, null, "zero"));
            pluralDict.appendChild(createXmlElement(file, "string", null, null, zero[0]));
            pluralDict.appendChild(createXmlElement(file, "key", null, null, "one"));
            pluralDict.appendChild(createXmlElement(file, "string", null, null, one[0]));
            pluralDict.appendChild(createXmlElement(file, "key", null, null, "two"));
            pluralDict.appendChild(createXmlElement(file, "string", null, null, two[0]));
            pluralDict.appendChild(createXmlElement(file, "key", null, null, "few"));
            pluralDict.appendChild(createXmlElement(file, "string", null, null, few[0]));
            pluralDict.appendChild(createXmlElement(file, "key", null, null, "many"));
            pluralDict.appendChild(createXmlElement(file, "string", null, null, many[0]));
            pluralDict.appendChild(createXmlElement(file, "key", null, null, "other"));
            pluralDict.appendChild(createXmlElement(file, "string", null, null, other[0]));

        });

        return file;
    }

    private Element createXmlElement(Document document,
                                     String tagName,
                                     String attribute,
                                     String attributeValue,
                                     String textContent){
        Element element = document.createElement(tagName);
        if(attribute != null) element.setAttribute(attribute, attributeValue);
        if(textContent != null) element.setTextContent(textContent);
        return element;
    }

    private Document createDoc() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.newDocument();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
