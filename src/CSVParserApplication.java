import com.sun.imageio.spi.InputStreamImageInputStreamSpi;
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
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;

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
            String content = line.substring((';')+1, line.length());
            Element item = doc.createElement("item");
            item.setAttribute("quantity", quantity);
            item.setTextContent(content);
            plurals.appendChild(item);
        });
        return plurals;
    }

    private void parseToStringsFile(List<String> lines) {
        List<String> outLines = new ArrayList<>();
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
                        //TODO:
                }
            }
            else {
                System.out.println("STRINGS: no line Type on line: " + line);
            }
        });
        System.out.println("STRINGS: all lines handled");
        createStringsFile(outLines);
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

    private void createStringsFile(List<String> lines){
        try{
            String newPath = savePath.replace("\\",System.getProperty("file.separator"));
            File file = new File( newPath,"Localizable.strings");
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
