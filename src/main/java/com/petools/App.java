package com.petools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip; // Import needed for Tooltip timing
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class App extends Application {

    private Button selectedButton;
    private final String defaultButtonStyle = "-fx-background-color: #555555; -fx-text-fill: white;";
    private final String selectedButtonStyle = "-fx-background-color: #555555; -fx-text-fill: red;";

    private WebView todoEditor;
    private WebEngine webEngine;
    private ObservableList<Project> projectList;

    private final Path todoFilePath = getTodoFilePath();
    private final Path projectFilePath = getProjectFilePath();
    private boolean isEditorLoaded = false;

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }

    @Override
    public void start(Stage stage) {
        projectList = loadProjects();

        // --- Home View ---
        Label label = new Label("Place Engineering Tools");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        VBox homeContent = new VBox(20, label);
        homeContent.setAlignment(Pos.CENTER);
        StackPane homeView = new StackPane(homeContent);
        homeView.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 20;");

        // --- Sidebar ---
        VBox sidebar = new VBox(10);
        sidebar.setStyle("-fx-background-color: #333333; -fx-padding: 10;");
        ArrayList<Button> buttons = new ArrayList<>();

        Button homeBtn = new Button("Home"); buttons.add(homeBtn);
        Button dashboardBtn = new Button("Dashboard"); buttons.add(dashboardBtn);
        Button todoBtn = new Button("To-Do List"); buttons.add(todoBtn);
        Button projectDataBtn = new Button("Projects"); buttons.add(projectDataBtn);
        Button cadToolsBtn = new Button("AutoCAD"); buttons.add(cadToolsBtn);
        Button siteLocatorBtn = new Button("Site Locator"); buttons.add(siteLocatorBtn);
        Button webMngmntBtn = new Button("Website"); buttons.add(webMngmntBtn);
        Button settingsBtn = new Button("Settings"); buttons.add(settingsBtn);

        for (Button btn : buttons) {
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle(defaultButtonStyle);
        }
        sidebar.getChildren().addAll(buttons);
        updateSelection(homeBtn); 

        BorderPane root = new BorderPane();
        root.setCenter(homeView);
        root.setLeft(sidebar);

        /* ===================== */
        /* --- Projects View --- */
        /* ===================== */
        VBox projectView = new VBox(10);
        projectView.setPadding(new Insets(20));
        projectView.setStyle("-fx-background-color: #ffffff;");
        Label projectHeader = new Label("Engineering Projects Management");
        projectHeader.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        ListView<Project> projectListView = new ListView<>(projectList);
        projectListView.setCellFactory(param -> new ListCell<Project>() {
            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName());
            }
        });

        HBox projectInputBox = new HBox(10);
        TextField newProjectNameField = new TextField();
        newProjectNameField.setPromptText("Enter Project Name");
        HBox.setHgrow(newProjectNameField, Priority.ALWAYS);
        Button addProjectBtn = new Button("Add Project");
        Button deleteProjectBtn = new Button("Delete Selected");

        addProjectBtn.setOnAction(e -> {
            String name = newProjectNameField.getText().trim();
            if (!name.isEmpty()) {
                projectList.add(new Project(name));
                newProjectNameField.clear();
            }
        });
        newProjectNameField.setOnAction(addProjectBtn.getOnAction());
        deleteProjectBtn.setOnAction(e -> {
            Project selected = projectListView.getSelectionModel().getSelectedItem();
            if (selected != null) projectList.remove(selected);
        });
        projectListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                Project selectedItem = projectListView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) projectList.remove(selectedItem);
            }
        });

        projectInputBox.getChildren().addAll(newProjectNameField, addProjectBtn, deleteProjectBtn);
        projectView.getChildren().addAll(projectHeader, projectListView, projectInputBox);
        VBox.setVgrow(projectListView, Priority.ALWAYS);


        /* =========================================== */
        /* --- To-Do List View (Rich Text Editor) --- */
        /* =========================================== */
        VBox todoView = new VBox();
        todoView.setStyle("-fx-background-color: #F9FBFD;");

        // --- 1. The Ribbon ---
        HBox ribbon = new HBox(5);
        ribbon.setPadding(new Insets(8, 15, 8, 15));
        ribbon.setAlignment(Pos.CENTER_LEFT);
        ribbon.setStyle("-fx-background-color: #EDF2FA; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 0, 0, 0, 1);");

        // Action: UNDO / REDO / PRINT
        Button undoBtn = createRibbonButton("↶", () -> executeCommand("undo", null));
        addCustomTooltip(undoBtn, "Undo (Ctrl+Z)");

        Button redoBtn = createRibbonButton("↷", () -> executeCommand("redo", null));
        addCustomTooltip(redoBtn, "Redo (Ctrl+Y)");

        Button printBtn = createRibbonButton("🖨", () -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(stage)) {
                webEngine.print(job);
                job.endJob();
            }
        });
        addCustomTooltip(printBtn, "Print");

        Separator sep1 = new Separator(Orientation.VERTICAL);

        // Action: TEXT STYLES
        ComboBox<String> styleCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Normal text", "Title", "Subtitle", "Heading 1", "Heading 2", "Heading 3"
        ));
        styleCombo.getSelectionModel().selectFirst();
        styleCombo.setPrefWidth(120);
        styleCombo.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-family: 'Arial';");
        styleCombo.setOnAction(e -> {
            String selected = styleCombo.getValue();
            switch (selected) {
                case "Normal text" -> executeCommand("formatBlock", "<p>");
                case "Title" -> executeCommand("formatBlock", "<h1>");
                case "Subtitle" -> executeCommand("formatBlock", "<h2>");
                case "Heading 1" -> executeCommand("formatBlock", "<h3>");
                case "Heading 2" -> executeCommand("formatBlock", "<h4>");
                case "Heading 3" -> executeCommand("formatBlock", "<h5>");
            }
            todoEditor.requestFocus();
        });
        Tooltip styleTip = new Tooltip("Styles");
        styleTip.setShowDelay(Duration.millis(100));
        styleTip.setStyle("-fx-background-color: #202124; -fx-text-fill: white; -fx-font-size: 12px;");
        styleCombo.setTooltip(styleTip);

        Separator sepStyle = new Separator(Orientation.VERTICAL);

        // Action: FONT FAMILY
        ComboBox<String> fontCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Arial", "Courier New", "Georgia", "Times New Roman", "Verdana", "Comic Sans MS"
        ));
        fontCombo.getSelectionModel().select("Arial");
        fontCombo.setStyle("-fx-background-color: transparent;");
        fontCombo.setPrefWidth(110);
        fontCombo.setOnAction(e -> executeCommand("fontName", fontCombo.getValue()));

        Tooltip fontTip = new Tooltip("Font");
        fontTip.setShowDelay(Duration.millis(100));
        fontTip.setStyle("-fx-background-color: #202124; -fx-text-fill: white; -fx-font-size: 12px;");
        fontCombo.setTooltip(fontTip);

        Separator sep2 = new Separator(Orientation.VERTICAL);

        // Action: FONT SIZE
        Button minusBtn = createRibbonButton("-", () -> executeCommand("decreaseFontSize", null));
        addCustomTooltip(minusBtn, "Decrease font size");

        TextField fontSizeDisplay = new TextField("11");
        fontSizeDisplay.setPrefWidth(40);
        fontSizeDisplay.setAlignment(Pos.CENTER);
        fontSizeDisplay.setEditable(false);
        fontSizeDisplay.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 3;");

        Button plusBtn = createRibbonButton("+", () -> executeCommand("increaseFontSize", null));
        addCustomTooltip(plusBtn, "Increase font size");

        Separator sep3 = new Separator(Orientation.VERTICAL);

        // Action: FORMATTING
        Button boldBtn = createRibbonButton("B", () -> executeCommand("bold", null));
        boldBtn.setStyle("-fx-font-weight: bold; -fx-background-color: transparent;");
        addCustomTooltip(boldBtn, "Bold (Ctrl+B)");

        Button italicBtn = createRibbonButton("I", () -> executeCommand("italic", null));
        italicBtn.setStyle("-fx-font-style: italic; -fx-font-family: serif; -fx-background-color: transparent;");
        addCustomTooltip(italicBtn, "Italic (Ctrl+I)");

        Button underlineBtn = createRibbonButton("U", () -> executeCommand("underline", null));
        underlineBtn.setStyle("-fx-underline: true; -fx-background-color: transparent;");
        addCustomTooltip(underlineBtn, "Underline (Ctrl+U)");

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setStyle("-fx-color-label-visible: false; -fx-background-color: transparent; -fx-pref-width: 40px;");
        colorPicker.setOnAction(e -> {
            String hex = toHexString(colorPicker.getValue());
            executeCommand("foreColor", hex);
        });
        Tooltip colorTip = new Tooltip("Text color");
        colorTip.setShowDelay(Duration.millis(100));
        colorTip.setStyle("-fx-background-color: #202124; -fx-text-fill: white; -fx-font-size: 12px;");
        colorPicker.setTooltip(colorTip);

        Separator sep4 = new Separator(Orientation.VERTICAL);

        // Alignment
        Button alignLeft = createRibbonButton("≡", () -> executeCommand("justifyLeft", null));
        addCustomTooltip(alignLeft, "Left align (Ctrl+Shift+L)");

        Button alignCenter = createRibbonButton("≚", () -> executeCommand("justifyCenter", null));
        addCustomTooltip(alignCenter, "Center align (Ctrl+Shift+E)");

        Button alignRight = createRibbonButton("≣", () -> executeCommand("justifyRight", null));
        addCustomTooltip(alignRight, "Right align (Ctrl+Shift+R)");

        Separator sep5 = new Separator(Orientation.VERTICAL);

        // Lists
        Button bulletListBtn = createRibbonButton("•≣", () -> executeCommand("insertUnorderedList", null));
        addCustomTooltip(bulletListBtn, "Bulleted list (Ctrl+Shift+8)");

        Button numListBtn = createRibbonButton("1.≣", () -> executeCommand("insertOrderedList", null));
        addCustomTooltip(numListBtn, "Numbered list (Ctrl+Shift+7)");

        Separator sep6 = new Separator(Orientation.VERTICAL);

        // Indentation
        Button indentLessBtn = createRibbonButton("⇠", () -> executeCommand("outdent", null));
        addCustomTooltip(indentLessBtn, "Decrease indent (Shift+Tab)");

        Button indentMoreBtn = createRibbonButton("⇢", () -> executeCommand("indent", null));
        addCustomTooltip(indentMoreBtn, "Increase indent (Tab)");

        Separator sep7 = new Separator(Orientation.VERTICAL);

        // Clear Formatting
        Button clearFormatBtn = createRibbonButton("Tₓ", () -> executeCommand("removeFormat", null));
        addCustomTooltip(clearFormatBtn, "Clear formatting (Ctrl+\\)");


        ribbon.getChildren().addAll(
            undoBtn, redoBtn, printBtn, sep1,
            styleCombo, sepStyle,
            fontCombo, sep2,
            minusBtn, fontSizeDisplay, plusBtn, sep3,
            boldBtn, italicBtn, underlineBtn, colorPicker, sep4,
            alignLeft, alignCenter, alignRight, sep5,
            bulletListBtn, numListBtn, sep6,
            indentLessBtn, indentMoreBtn, sep7,
            clearFormatBtn
        );

        // --- 2. The Editor Page (WebView) ---
        todoEditor = new WebView();
        webEngine = todoEditor.getEngine();

        // ** KEYBOARD HANDLER **
        todoEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                if (event.isShiftDown()) {
                    executeCommand("outdent", null);
                } else {
                    executeCommand("indent", null);
                }
                event.consume();
                return;
            }

            if (event.isShortcutDown()) {
                KeyCode code = event.getCode();
                if (code == KeyCode.B) { executeCommand("bold", null); event.consume(); }
                else if (code == KeyCode.I) { executeCommand("italic", null); event.consume(); }
                else if (code == KeyCode.U) { executeCommand("underline", null); event.consume(); }
                else if (code == KeyCode.BACK_SLASH) { executeCommand("removeFormat", null); event.consume(); }
                else if (code == KeyCode.Z) {
                    if (event.isShiftDown()) executeCommand("redo", null);
                    else executeCommand("undo", null);
                    event.consume();
                }
                else if (code == KeyCode.Y) { executeCommand("redo", null); event.consume(); }
                else if (code == KeyCode.DIGIT8 && event.isShiftDown()) { executeCommand("insertUnorderedList", null); event.consume(); }
                else if (code == KeyCode.DIGIT7 && event.isShiftDown()) { executeCommand("insertOrderedList", null); event.consume(); }
                else if (code == KeyCode.L && event.isShiftDown()) { executeCommand("justifyLeft", null); event.consume(); }
                else if (code == KeyCode.E && event.isShiftDown()) { executeCommand("justifyCenter", null); event.consume(); }
                else if (code == KeyCode.R && event.isShiftDown()) { executeCommand("justifyRight", null); event.consume(); }
            }
        });

        String initialContent = loadTodoHtml();
        webEngine.loadContent(initialContent);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                isEditorLoaded = true;
                webEngine.executeScript("document.body.contentEditable = true;");
                webEngine.executeScript("document.body.style.fontFamily = 'Arial';");
                webEngine.executeScript("document.body.style.fontSize = '14px';");
                webEngine.executeScript("document.body.style.margin = '0px';");
            }
        });

        VBox pageSheet = new VBox(todoEditor);
        pageSheet.setMaxWidth(850);
        pageSheet.setPadding(new Insets(0));
        pageSheet.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        VBox.setVgrow(todoEditor, Priority.ALWAYS);

        StackPane editorWorkspace = new StackPane(pageSheet);
        editorWorkspace.setStyle("-fx-background-color: #F0F0F0;");
        editorWorkspace.setPadding(new Insets(30));

        todoView.getChildren().addAll(ribbon, editorWorkspace);
        VBox.setVgrow(editorWorkspace, Priority.ALWAYS);


        // --- Navigation Logic ---
        homeBtn.setOnAction(e -> { label.setText("Place Engineering Tools"); updateSelection(homeBtn); root.setCenter(homeView); });
        dashboardBtn.setOnAction(e -> { label.setText("Dashboard View"); updateSelection(dashboardBtn); root.setCenter(homeView); });
        todoBtn.setOnAction(e -> { updateSelection(todoBtn); root.setCenter(todoView); });
        projectDataBtn.setOnAction(e -> { updateSelection(projectDataBtn); root.setCenter(projectView); });
        cadToolsBtn.setOnAction(e -> { label.setText("AutoCAD Automation Dashboard"); updateSelection(cadToolsBtn); root.setCenter(homeView); });
        siteLocatorBtn.setOnAction(e -> { label.setText("Engineering Site Locator"); updateSelection(siteLocatorBtn); root.setCenter(homeView); });
        webMngmntBtn.setOnAction(e -> { label.setText("Website - Project Map Management"); updateSelection(webMngmntBtn); root.setCenter(homeView); });
        settingsBtn.setOnAction(e -> { label.setText("Settings"); updateSelection(settingsBtn); root.setCenter(homeView); });

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("PE Tools");
        stage.getIcons().add(new Image(App.class.getResourceAsStream("/com/petools/images/pe_logo.png")));
        stage.setScene(scene);
        stage.show();
    }

    // Helper: Add styled tooltip with fast delay
    private void addCustomTooltip(Control control, String text) {
        Tooltip t = new Tooltip(text);
        // Make it appear fast (100ms) instead of the default 1 second
        t.setShowDelay(Duration.millis(100));
        // Style it to look like Google Docs (Dark background, white text)
        t.setStyle("-fx-background-color: #202124; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6px;");
        control.setTooltip(t);
    }

    private void executeCommand(String command, String value) {
        if (webEngine != null) {
            if (value == null) {
                webEngine.executeScript("document.execCommand('" + command + "', false, null)");
            } else {
                webEngine.executeScript("document.execCommand('" + command + "', false, '" + value + "')");
            }
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    private Button createRibbonButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-font-size: 14px; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #E1E5F2; -fx-text-fill: #000; -fx-background-radius: 4px; -fx-font-size: 14px;"));
        btn.setOnMouseExited(e -> {
            switch (text) {
                case "B" -> btn.setStyle("-fx-font-weight: bold; -fx-background-color: transparent;");
                case "I" -> btn.setStyle("-fx-font-style: italic; -fx-font-family: serif; -fx-background-color: transparent;");
                case "U" -> btn.setStyle("-fx-underline: true; -fx-background-color: transparent;");
                default -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-font-size: 14px;");
            }
        });
        return btn;
    }

    private void updateSelection(Button newSelected) {
        if (selectedButton != null) selectedButton.setStyle(defaultButtonStyle);
        selectedButton = newSelected;
        selectedButton.setStyle(selectedButtonStyle);
    }

    private Path getTodoFilePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".petools", "todos.html"); 
    }

    private Path getProjectFilePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".petools", "projects.txt");
    }

    @Override
    public void stop() throws Exception {
        saveTodoHtml();
        saveProjects();
        super.stop();
    }

    private ObservableList<Project> loadProjects() {
        ObservableList<Project> list = FXCollections.observableArrayList();
        if (Files.exists(projectFilePath)) {
            try {
                List<String> lines = Files.readAllLines(projectFilePath);
                for(String line : lines) {
                    if(!line.trim().isEmpty()) list.add(new Project(line.trim()));
                }
            } catch (IOException e) { System.err.println("Error loading projects"); }
        }
        if(list.isEmpty()) {
            list.add(new Project("Site A - Grading"));
            list.add(new Project("Bridge Analysis"));
        }
        return list;
    }

    private void saveProjects() {
        try {
            Files.createDirectories(projectFilePath.getParent());
            List<String> lines = new ArrayList<>();
            for(Project p : projectList) lines.add(p.getName());
            Files.write(projectFilePath, lines);
        } catch(IOException e) { System.err.println("Error saving projects"); }
    }

    private String loadTodoHtml() {
        if (Files.exists(todoFilePath)) {
            try {
                return new String(Files.readAllBytes(todoFilePath), StandardCharsets.UTF_8);
            } catch (IOException e) { System.err.println("Error loading html"); }
        }
        return "<html><body contenteditable='true' style='font-family: Arial; font-size: 14px;'>Type your notes here...</body></html>";
    }

    private void saveTodoHtml() {
        if (isEditorLoaded && webEngine != null) {
            String htmlContent = (String) webEngine.executeScript("document.documentElement.outerHTML");
            try {
                Files.createDirectories(todoFilePath.getParent());
                Files.write(todoFilePath, htmlContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.err.println("Error saving html notes: " + e.getMessage());
            }
        }
    }
}