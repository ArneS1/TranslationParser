import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.w3c.dom.*;

import java.io.*;

import javax.print.Doc;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;


public class CSVParserApplication extends Application {

    private File csvFile;
    private boolean parseToXML;
    private boolean parseToStrings;
    private String savePath;

    @Override
    public void init() throws Exception {
        //Before Launch
        parseToXML = true;
        parseToStrings = true;
    }


    @Override
    public void start(Stage stage) throws Exception {

        stage.setTitle("CSV Parser");
        stage.setWidth(400);
        stage.setHeight(400);

        VBox parent = new VBox();

        Label label_dragNdrop = new Label("Drag and Drop your CSV File here:");
        ImageView imageView_dragHere = createDragDropImageView();
        Label label_outoutOptions = new Label("Output Options:");
        RadioButton radioButton_XML = new RadioButton("Parse to XML (Android)");
        radioButton_XML.setSelected(parseToXML);
        RadioButton radioButton_Swift = new RadioButton("Parse to Strings (iOS)");
        radioButton_Swift.setSelected(parseToStrings);
        //TODO: add Listeners to RadioButtons

        Button button_startParsing = new Button("Start");
        button_startParsing.setOnAction(e -> {
            savePath = getSavePath();
            startParsing();
        });

        parent.getChildren().addAll(label_dragNdrop,
                imageView_dragHere,
                label_outoutOptions,
                radioButton_XML,
                radioButton_Swift,
                button_startParsing);


        Scene scene = new Scene(parent);
        stage.setScene(scene);

        stage.show();
    }

    private ImageView createDragDropImageView() {
        ImageView img = new ImageView();
        img.setFitHeight(50);
        img.setFitWidth(100);
        img.setImage(new Image("img/drag_drop_bg.png"));

        img.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                if (dragEvent.getDragboard().hasFiles()) {
                    dragEvent.acceptTransferModes(TransferMode.ANY);
                }
                dragEvent.consume();
            }
        });

        img.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                List<File> files = dragEvent.getDragboard().getFiles();
                if (files.size() > 1) {
                    img.setImage(new Image("img/drag_drop_fail_bg.png"));
                } else {
                    if (files.get(0).getName().contains(".csv")) {
                        csvFile = files.get(0);
                        img.setImage(new Image("img/drag_drop_done_bg.png"));
                    } else img.setImage(new Image("img/drag_drop_fail_bg.png"));
                }
            }
        });
        return img;
    }

    private void startParsing() {
        if (csvFile != null) {
            List<String> lines = new ArrayList<>();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(csvFile));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                System.out.println(lines.size() + " lines read");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            if (parseToXML) parseToXmlFile(lines);
            if (parseToStrings) parseToStringsFile(lines);
        } else {
            //TODO: Tell user to select output option
        }
    }

    private void parseToXmlFile(List<String> lines) {
        Document xml = createDoc();
        assert xml != null;
        Element rootElement = xml.createElement("resources");
        xml.appendChild(rootElement);
        lines.forEach(line -> {
            Element nextElement = null;
            if (getLineType(line) != null) {
                switch (Objects.requireNonNull(getLineType(line))) {
                    case empty:
                        nextElement = createEmptyXML();
                        break;
                    case comment:
                        rootElement.appendChild(createCommentXML(xml, line));
                        break;
                    case content:
                        nextElement = createContentXML(xml, line);
                        break;
                    case plural:
                        if (!getLineType(lines.get(lines.indexOf(line) - 1)).equals(LineType.plural)) { //check if it is the first item of the plurals

                            List<String> pluralLines = new ArrayList<>();          //create list with all plural items
                            LineType lineType = LineType.plural;
                            int i = lines.indexOf(line);

                            while (lineType == LineType.plural) {
                                pluralLines.add(lines.get(i));
                                i++;
                                lineType = getLineType(lines.get(i));
                            }
                            nextElement = createPluralXML(xml, pluralLines);
                        }
                        break;
                    default:
                        System.out.println("XML: no LineType");
                        break;
                }
                if (nextElement != null) {
                    rootElement.appendChild(nextElement);
                    nextElement = null;
                }
            }
        });
        System.out.println("XML: all lines handled.");
        createXmlFile(xml);
    }

    private void createXmlFile(Document xml) {
        try {
            DOMSource source = new DOMSource(xml);
            StreamResult result = new StreamResult(new File(savePath + "/strings.xml"));
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(source, result);

        } catch (Exception e) {
            e.printStackTrace();
            //TODO: Notify User
        }

    }

    private String getSavePath(){
        JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        jfc.setDialogTitle("Choose a directory to save your file: ");
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = jfc.showSaveDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            if (jfc.getSelectedFile().isDirectory()) {
                return jfc.getSelectedFile().getAbsolutePath();
            }
        }
        return null;
    }

    private Element createEmptyXML() {
        //TODO: create empty line
        return null;
    }

    private Comment createCommentXML(Document doc, String line) {
        String content = line.substring(2, line.length() - 2);
        return doc.createComment(content);
    }

    private Element createContentXML(Document doc, String line) {
        String name = line.substring(0, line.indexOf(';'));
        String content = line.substring(line.indexOf(';') + 1);

        Element newElement = doc.createElement("string");
        newElement.setAttribute("name", name);
        newElement.setTextContent(content);

        return newElement;
    }

    private Element createPluralXML(Document doc, List<String> lines) {
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

    private void parseToStringsFile(List<String> lines) {
        List<String> outLines = new ArrayList<>();
        List<List<String>> plurals = new ArrayList<>();
        lines.forEach(line -> {
            if(getLineType(line) != null) {
                switch (Objects.requireNonNull(getLineType(line))) {
                    case empty:
                        outLines.add("");
                        break;
                    case comment:
                        outLines.add(createCommentString(line));
                        break;
                    case content:
                        outLines.add(createContentString(line));
                        break;
                    case plural:
                        if (!getLineType(lines.get(lines.indexOf(line) - 1)).equals(LineType.plural)) { //check if it is the first item of the plurals

                            List<String> pluralLines = new ArrayList<>();          //create list with all plural items
                            LineType lineType = LineType.plural;
                            int i = lines.indexOf(line);

                            while (lineType == LineType.plural) {
                                pluralLines.add(lines.get(i));
                                i++;
                                lineType = getLineType(lines.get(i));
                            }
                            plurals.add(pluralLines);
                        }
                        break;
                }
                createPluralsFile(plurals);
            }
            else {
                System.out.println("STRINGS: no line Type on line: " + line);
            }
        });
        System.out.println("STRINGS: all lines handled");
        createStringsFile(outLines, "Localizable.strings");
    }

    private String createContentString(String line) {
        String name = line.substring(0, line.indexOf(';'));
        String content = line.substring(line.indexOf(';') + 1);

        String newElement = "\"" + name + "\"";
        newElement += " = ";
        newElement += "\"" + content + "\";";

        return newElement;
    }

    private String createCommentString(String line) {
        String content = line.substring(2, line.length() - 2);
        return "/* " + content + " */";
    }

    private void createPluralsFile(List<List<String>> plurals){
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

        parseToStringsdict(file);
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

    private void parseToStringsdict(Document document){
        try {
            StreamResult result = new StreamResult(new File(savePath + "/Localizable.stringsdict"));
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMImplementation domImpl = document.getImplementation();
            DocumentType doctype = domImpl.createDocumentType("doctype",
                    "-//Apple//DTD PLIST 1.0//EN",
                    "http://www.apple.com/DTDs/PropertyList-1.0.dtd");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
            DOMSource source = new DOMSource(document);
            transformer.transform(source, result);
        } catch (Exception e){
            e.printStackTrace();
        }
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

    private void createStringsFile(List<String> lines, String fileName){
        try{
            String newPath = savePath.replace("\\",System.getProperty("file.separator"));
            File file = new File( newPath,fileName);
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            Writer writer = new BufferedWriter(fileWriter);
            System.out.println("STRINGS: File created in " + newPath);
            lines.forEach( line -> {
                try {
                    writer.write(line);
                    writer.write("\n");
                    writer.flush();
                } catch (IOException e) {
                    System.out.println("An error occurred: " + e);
                }
            });
            System.out.println("STRINGS: all lines written");
            fileWriter.close();
        } catch (IOException e){
            System.out.println("An error occurred: " + e);
        }
    }

    private LineType getLineType(String line) {
        if (line.equals("")) {
            return LineType.empty;
        }

        if (line.endsWith("==")) {
            return LineType.comment;
        }

        if (line.contains("#")) {
            return LineType.plural;
        }

        if (line.contains(";")) {     //handling normal line
            return LineType.content;
        }

        return null;
    }

    @Override
    public void stop() throws Exception {
        //After Stopping
    }
}
