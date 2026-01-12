package com.petools.features.todo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

public class TodoView extends VBox {

    private final TabPane tabPane;
    private WebEngine activeEngine;
    private final Path todoDir = Paths.get(System.getProperty("user.home"), ".petools", "todos");

    // Constant for the base A4-ish width
    private static final double BASE_PAGE_WIDTH = 850.0;

    public TodoView() {
        this.setStyle("-fx-background-color: #F9FBFD;");

        HBox ribbon = createRibbon();

        tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: #F0F0F0; -fx-tab-min-width: 120px;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                WebView view = getWebViewFromTab(newTab);
                if (view != null) {
                    activeEngine = view.getEngine();
                    view.requestFocus();
                }
            }
        });

        initializeTabs();

        this.getChildren().addAll(ribbon, tabPane);
    }

    private void initializeTabs() {
        try {
            if (!Files.exists(todoDir)) Files.createDirectories(todoDir);

            LocalDate today = LocalDate.now();
            LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            String mondayName = "Week_" + thisMonday.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".html";
            Path currentWeekFile = todoDir.resolve(mondayName);

            if (!Files.exists(currentWeekFile)) {
                createEmptyFile(currentWeekFile, "Week of " + thisMonday.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }

            List<Path> allFiles = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(todoDir, "*.html")) {
                for (Path entry : stream) allFiles.add(entry);
            }
            allFiles.sort(Collections.reverseOrder());

            for (Path file : allFiles) {
                addTab(file);
            }

            if (!tabPane.getTabs().isEmpty()) {
                tabPane.getSelectionModel().select(0);
            }

        } catch (IOException e) {}
    }

    private void addTab(Path filePath) {
        String filename = filePath.getFileName().toString().replace(".html", "");
        String displayName = filename.startsWith("Week_") ? "Week of " + filename.replace("Week_", "") : filename;

        Tab tab = new Tab(displayName);
        tab.setUserData(filePath);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem renameItem = new MenuItem("Rename Tab");
        renameItem.setOnAction(e -> handleRename(tab));
        contextMenu.getItems().add(renameItem);
        tab.setContextMenu(contextMenu);

        WebView editor = new WebView();
        WebEngine engine = editor.getEngine();
        engine.loadContent(loadFileContent(filePath));

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                engine.executeScript("document.body.contentEditable = true;");
                engine.executeScript("document.body.style.fontFamily = 'Arial';");
                engine.executeScript("document.body.style.fontSize = '14px';");
                engine.executeScript("document.body.style.margin = '0px';");
            }
        });

        setupKeyboardShortcuts(editor, engine);

        VBox pageSheet = new VBox(editor);
        pageSheet.setMaxWidth(BASE_PAGE_WIDTH);
        pageSheet.setPadding(new Insets(0));
        pageSheet.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        VBox.setVgrow(editor, Priority.ALWAYS);

        StackPane workspace = new StackPane(pageSheet);
        workspace.setStyle("-fx-background-color: #F0F0F0;");
        workspace.setPadding(new Insets(30));

        tab.setContent(workspace);
        tabPane.getTabs().add(tab);

        tabPane.getSelectionModel().select(tab);
    }

    private void handleNewTab() {
        TextInputDialog dialog = new TextInputDialog("New Notes");
        dialog.setTitle("New Tab");
        dialog.setHeaderText("Create a new document");
        dialog.setContentText("Enter name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String safeName = name.trim().replaceAll("[^a-zA-Z0-9 _-]", ""); 
            if (safeName.isEmpty()) safeName = "Untitled";

            Path newPath = todoDir.resolve(safeName + ".html");

            if (Files.exists(newPath)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "A file with this name already exists.");
                alert.showAndWait();
                return;
            }

            try {
                createEmptyFile(newPath, safeName);
                addTab(newPath);
            } catch (IOException e) {
            }
        });
    }

    private void handleRename(Tab tab) {
        TextInputDialog dialog = new TextInputDialog(tab.getText());
        dialog.setTitle("Rename Tab");
        dialog.setHeaderText("Rename '" + tab.getText() + "'");
        dialog.setContentText("New name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String safeName = name.trim().replaceAll("[^a-zA-Z0-9 _-]", "");
            if (safeName.isEmpty()) return;

            Path oldPath = (Path) tab.getUserData();
            Path newPath = todoDir.resolve(safeName + ".html");

            if (Files.exists(newPath)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "A file with this name already exists.");
                alert.showAndWait();
                return;
            }

            try {
                saveTab(tab);
                Files.move(oldPath, newPath);
                tab.setText(safeName);
                tab.setUserData(newPath);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Could not rename file.");
                alert.showAndWait();
            }
        });
    }

    public void save() {
        for (Tab tab : tabPane.getTabs()) {
            saveTab(tab);
        }
    }

    private void saveTab(Tab tab) {
        WebView view = getWebViewFromTab(tab);
        Path path = (Path) tab.getUserData();
        if (view != null && path != null) {
            try {
                String html = (String) view.getEngine().executeScript("document.documentElement.outerHTML");
                Files.write(path, html.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) { System.err.println("Error saving tab: " + path); }
        }
    }

    private HBox createRibbon() {
        HBox ribbon = new HBox(5);
        ribbon.setPadding(new Insets(8, 15, 8, 15));
        ribbon.setAlignment(Pos.CENTER_LEFT);
        ribbon.setStyle("-fx-background-color: #EDF2FA; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 0, 0, 0, 1);");

        Button newTabBtn = createRibbonButton("+ New Tab", this::handleNewTab);
        newTabBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;");
        addCustomTooltip(newTabBtn, "Create a new document");

        Separator sep0 = new Separator(Orientation.VERTICAL);

        Button undoBtn = createRibbonButton("↶", () -> executeCommand(activeEngine, "undo", null));
        addCustomTooltip(undoBtn, "Undo (Ctrl+Z)");

        Button redoBtn = createRibbonButton("↷", () -> executeCommand(activeEngine, "redo", null));
        addCustomTooltip(redoBtn, "Redo (Ctrl+Y)");

        Button printBtn = createRibbonButton("🖨", () -> {
            if (activeEngine != null) {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(getScene().getWindow())) {
                    activeEngine.print(job);
                    job.endJob();
                }
            }
        });
        addCustomTooltip(printBtn, "Print");

        Separator sep1 = new Separator(Orientation.VERTICAL);

        ComboBox<String> styleCombo = new ComboBox<>(FXCollections.observableArrayList("Normal text", "Title", "Subtitle", "Heading 1", "Heading 2", "Heading 3"));
        styleCombo.getSelectionModel().selectFirst();
        styleCombo.setPrefWidth(120);
        styleCombo.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-family: 'Arial';");
        styleCombo.setOnAction(e -> {
            String val = styleCombo.getValue();
            String tag = val.equals("Normal text") ? "<p>" : val.equals("Title") ? "<h1>" : val.equals("Subtitle") ? "<h2>" : val.equals("Heading 1") ? "<h3>" : val.equals("Heading 2") ? "<h4>" : "<h5>";
            executeCommand(activeEngine, "formatBlock", tag);
        });

        Separator sepStyle = new Separator(Orientation.VERTICAL);

        ComboBox<String> fontCombo = new ComboBox<>(FXCollections.observableArrayList("Arial", "Courier New", "Georgia", "Times New Roman", "Verdana", "Comic Sans MS"));
        fontCombo.getSelectionModel().select("Arial");
        fontCombo.setStyle("-fx-background-color: transparent;");
        fontCombo.setPrefWidth(110);
        fontCombo.setOnAction(e -> executeCommand(activeEngine, "fontName", fontCombo.getValue()));

        Separator sep2 = new Separator(Orientation.VERTICAL);

        Button minusBtn = createRibbonButton("-", () -> executeCommand(activeEngine, "decreaseFontSize", null));
        TextField fontSizeDisplay = new TextField("11");
        fontSizeDisplay.setPrefWidth(40);
        fontSizeDisplay.setAlignment(Pos.CENTER);
        fontSizeDisplay.setEditable(false);
        fontSizeDisplay.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 3;");
        Button plusBtn = createRibbonButton("+", () -> executeCommand(activeEngine, "increaseFontSize", null));

        Separator sep3 = new Separator(Orientation.VERTICAL);

        Button boldBtn = createRibbonButton("B", () -> executeCommand(activeEngine, "bold", null));
        boldBtn.setStyle("-fx-font-weight: bold;");
        Button italicBtn = createRibbonButton("I", () -> executeCommand(activeEngine, "italic", null));
        italicBtn.setStyle("-fx-font-style: italic; -fx-font-family: serif;");
        Button underlineBtn = createRibbonButton("U", () -> executeCommand(activeEngine, "underline", null));
        underlineBtn.setStyle("-fx-underline: true;");

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setStyle("-fx-color-label-visible: false; -fx-background-color: transparent; -fx-pref-width: 40px;");
        colorPicker.setOnAction(e -> executeCommand(activeEngine, "foreColor", toHexString(colorPicker.getValue())));

        Separator sep4 = new Separator(Orientation.VERTICAL);

        Button alignLeft = createRibbonButton("≡", () -> executeCommand(activeEngine, "justifyLeft", null));
        Button alignCenter = createRibbonButton("≚", () -> executeCommand(activeEngine, "justifyCenter", null));
        Button alignRight = createRibbonButton("≣", () -> executeCommand(activeEngine, "justifyRight", null));

        Separator sep5 = new Separator(Orientation.VERTICAL);

        Button bulletListBtn = createRibbonButton("•≣", () -> executeCommand(activeEngine, "insertUnorderedList", null));
        Button numListBtn = createRibbonButton("1.≣", () -> executeCommand(activeEngine, "insertOrderedList", null));

        Separator sep6 = new Separator(Orientation.VERTICAL);

        Button indentLessBtn = createRibbonButton("⇠", () -> executeCommand(activeEngine, "outdent", null));
        Button indentMoreBtn = createRibbonButton("⇢", () -> executeCommand(activeEngine, "indent", null));

        Separator sep7 = new Separator(Orientation.VERTICAL);
        Button clearFormatBtn = createRibbonButton("Tₓ", () -> executeCommand(activeEngine, "removeFormat", null));

        ribbon.getChildren().addAll(
            newTabBtn, sep0,
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

        return ribbon;
    }

    private WebView getWebViewFromTab(Tab tab) {
        if (tab.getContent() instanceof StackPane stack) {
            if (!stack.getChildren().isEmpty() && stack.getChildren().get(0) instanceof VBox) {
                VBox box = (VBox) stack.getChildren().get(0);
                if (!box.getChildren().isEmpty() && box.getChildren().get(0) instanceof WebView) {
                    return (WebView) box.getChildren().get(0);
                }
            }
        }
        return null;
    }

    private void createEmptyFile(Path path, String title) throws IOException {
        String defaultContent = "<html><body contenteditable='true' style='font-family: Arial; font-size: 14px;'><h2>" + title + "</h2><p>Type here...</p></body></html>";
        Files.write(path, defaultContent.getBytes(StandardCharsets.UTF_8));
    }

    private String loadFileContent(Path path) {
        try { return new String(Files.readAllBytes(path), StandardCharsets.UTF_8); }
        catch (IOException e) { return "<html><body>Error loading file</body></html>"; }
    }

    private void executeCommand(WebEngine engine, String cmd, String val) {
        if (engine != null) {
            engine.executeScript("document.execCommand('" + cmd + "', false, " + (val == null ? "null" : "'" + val + "'") + ")");
        }
    }

    private void setupKeyboardShortcuts(WebView view, WebEngine engine) {
        view.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            // --- TRUE PAGE ZOOM (Fixed) ---
            if (event.isShortcutDown()) {

                double currentZoom = view.getZoom();
                double newZoom = currentZoom;

                if (null != event.getCode()) // Calculate New Zoom
                    switch (event.getCode()) {
                        case EQUALS, PLUS, ADD -> newZoom = currentZoom + 0.1;
                        case MINUS, SUBTRACT -> newZoom = Math.max(0.1, currentZoom - 0.1);
                        case DIGIT0, NUMPAD0 -> newZoom = 1.0;
                        default -> {
                        }
                    }

                // If zoom changed, apply it to BOTH View AND Container
                if (newZoom != currentZoom) {
                    view.setZoom(newZoom);

                    // Also scale the "Paper" (VBox parent) so it doesn't just re-wrap text
                    if (view.getParent() instanceof VBox pageSheet) {
                        // Scale the width of the container to match the zoom
                        pageSheet.setMaxWidth(BASE_PAGE_WIDTH * newZoom);
                        pageSheet.setMinWidth(BASE_PAGE_WIDTH * newZoom);
                    }

                    event.consume();
                    return;
                }
            }

            // --- EXISTING SHORTCUTS ---
            if (event.getCode() == KeyCode.TAB) {
                if (event.isShiftDown()) executeCommand(engine, "outdent", null);
                else executeCommand(engine, "indent", null);
                event.consume();
                return;
            }
            if (event.isShortcutDown()) {
                KeyCode code = event.getCode();
                if (code == KeyCode.B) executeCommand(engine, "bold", null);
                else if (code == KeyCode.I) executeCommand(engine, "italic", null);
                else if (code == KeyCode.U) executeCommand(engine, "underline", null);
                else if (code == KeyCode.BACK_SLASH) executeCommand(engine, "removeFormat", null);
                else if (code == KeyCode.Z) {
                    if (event.isShiftDown()) executeCommand(engine, "redo", null);
                    else executeCommand(engine, "undo", null);
                }
                else if (code == KeyCode.Y) executeCommand(engine, "redo", null);
                else if (code == KeyCode.DIGIT8 && event.isShiftDown()) executeCommand(engine, "insertUnorderedList", null);
                else if (code == KeyCode.DIGIT7 && event.isShiftDown()) executeCommand(engine, "insertOrderedList", null);
                else if (code == KeyCode.L && event.isShiftDown()) executeCommand(engine, "justifyLeft", null);
                else if (code == KeyCode.E && event.isShiftDown()) executeCommand(engine, "justifyCenter", null);
                else if (code == KeyCode.R && event.isShiftDown()) executeCommand(engine, "justifyRight", null);
            }
        });
    }

    private Button createRibbonButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-font-size: 14px; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        btn.setOnMouseEntered(e -> {
            if (!text.equals("+ New Tab")) btn.setStyle("-fx-background-color: #E1E5F2; -fx-text-fill: #000; -fx-background-radius: 4px; -fx-font-size: 14px;");
        });
        btn.setOnMouseExited(e -> {
            if (!text.equals("+ New Tab")) btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-font-size: 14px;");
        });
        return btn;
    }

    private void addCustomTooltip(Control control, String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(100));
        t.setStyle("-fx-background-color: #202124; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6px;");
        control.setTooltip(t);
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
    }
}