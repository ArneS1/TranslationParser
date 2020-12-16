package de.dataport;

import de.dataport.strings.StringsElementCreator;
import de.dataport.xml.XmlElementCreator;
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
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class CSVParserApplication extends Application {

    private File csvFile;
    private boolean parseToXML;
    private boolean parseToStrings;
    private String savePath;

    private XmlElementCreator xmlElementCreator;
    private StringsElementCreator stringsElementCreator;

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
        RadioButton radioButton_iOS = new RadioButton("Parse to Strings (iOS)");
        radioButton_iOS.setSelected(parseToStrings);

        Button button_startParsing = new Button("Start");
        button_startParsing.setOnAction(e -> {
            savePath = getSavePath();
            readCSVFile();
        });

        parent.getChildren().addAll(label_dragNdrop,
                imageView_dragHere,
                label_outoutOptions,
                radioButton_XML,
                radioButton_iOS,
                button_startParsing);


        Scene scene = new Scene(parent);
        stage.setScene(scene);

        stage.show();
    }

    private ImageView createDragDropImageView() {
        ImageView img = new ImageView();
        img.setFitHeight(50);
        img.setFitWidth(100);
        setImageFromUrl(img, "src\\img\\drag_drop_bg.png");


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
                    setImageFromUrl(img,"src\\img\\drag_drop_fail_bg.png");
                } else {
                    if (files.get(0).getName().contains(".csv")) {
                        csvFile = files.get(0);
                        setImageFromUrl(img, "src\\img\\drag_drop_done_bg.png");
                    } else {
                        setImageFromUrl(img, "img\\drag_drop_fail_bg.png");
                    }
                }
            }
        });
        return img;
    }

    private void setImageFromUrl(ImageView img, String url){
        try{
            img.setImage(new Image(new FileInputStream(url)));
        } catch (Exception e){
            System.out.println("Error Loading Image");
        }
    }

    private void readCSVFile() {
        if (csvFile != null) {
            List<String> lines = new ArrayList<>();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(csvFile));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    //TODO: escape Character
                    //TODO: Remove first line
                    String newLine = removeGerman(line);
                    lines.add(newLine);
                }
                System.out.println(lines.size() + " lines read");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            startParsing(lines);
        } else {
            //TODO: Tell user to select output option
        }
    }

    private void startParsing(List<String> lines){
        if (parseToXML){
            xmlElementCreator = new XmlElementCreator();
            parseToXmlFile(lines);
        }

        if (parseToStrings) {
            stringsElementCreator = new StringsElementCreator();
            parseToStringsFile(lines);
        }
    }

    private String escapeCharacters(String line){
        String replacedLine = line.replaceAll("\"", "&quot;");
        replacedLine = line.replaceAll("'","&apos;");
        replacedLine = line.replaceAll("<","&lt;");
        replacedLine = line.replaceAll(">","&gt;");
        replacedLine = line.replaceAll("&","&amp;");
        return replacedLine;
    }

    private String removeGerman(String line){
        String returnLine = "";
        String[] parts = line.split(";");

        if (parts.length > 0) {
            returnLine += parts[0];
            System.out.println("parts: " + parts.length);

            if(parts.length > 2){
                returnLine += ";";
                returnLine += parts[2];
            } else if(parts.length > 1) {
                returnLine += ";" + parts[1];
            }
            System.out.println("LINE: " + returnLine);
        }
        return returnLine;
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
                        nextElement = xmlElementCreator.createEmptyXML();
                        break;
                    case comment:
                        rootElement.appendChild(xmlElementCreator.createCommentXML(xml, line));
                        break;
                    case content:
                        nextElement = xmlElementCreator.createContentXML(xml, line);
                        break;
                    case plural:
                        System.out.println("PLURAL:" + line);
                        if (!getLineType(lines.get(lines.indexOf(line) - 1)).equals(LineType.plural)) { //check if it is the first item of the plurals

                            List<String> pluralLines = new ArrayList<>();          //create list with all plural items
                            LineType lineType = LineType.plural;
                            int i = lines.indexOf(line);

                            while (lineType == LineType.plural) {
                                pluralLines.add(lines.get(i));
                                i++;
                                lineType = getLineType(lines.get(i));
                            }
                            nextElement = xmlElementCreator.createPluralXML(xml, pluralLines);
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
                        outLines.add(stringsElementCreator.createCommentString(line));
                        break;
                    case content:
                        outLines.add(stringsElementCreator.createContentString(line));
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
                        parseToStringsdict(stringsElementCreator.createPluralsFile(plurals));
                        break;
                }
            }
            else {
                System.out.println("STRINGS: no line Type on line: " + line);
            }
        });
        System.out.println("STRINGS: all lines handled");
        createStringsFile(outLines, "Localizable.strings");
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

        if (line.startsWith("<!--")) {
            return LineType.comment;
        }

        if (line.contains("#zero") || line.contains("#one") || line.contains("#two") || line.contains("#few") || line.contains("#many") || line.contains("#other")) {
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