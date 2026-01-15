package com.petools.features.todo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class TodoView extends BorderPane {

    private TabPane tabPane;
    private final VBox tagSidebar;

    // File Paths
    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), ".petools", "notes");
    private static final Path PROJECTS_FILE = Paths.get(System.getProperty("user.home"), ".petools", "projects.csv");

    public TodoView() {
        if (!Files.exists(DATA_DIR)) {
            try { Files.createDirectories(DATA_DIR); } catch (IOException e) {}
        }

        // --- 1. Left Sidebar ---
        VBox headerBox = new VBox(5);
        headerBox.setPadding(new Insets(10));
        Label tagHeader = new Label("Projects");
        tagHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 14px;");

        Button refreshBtn = new Button("↻ Refresh List");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setStyle("-fx-font-size: 10px; -fx-background-color: transparent; -fx-text-fill: #0078d7; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> refreshSidebar());
        headerBox.getChildren().addAll(tagHeader, refreshBtn);

        VBox tagContent = new VBox(8);
        tagContent.setPadding(new Insets(10));
        tagContent.setStyle("-fx-background-color: transparent;");
        this.tagSidebar = tagContent;

        ScrollPane scrollWrapper = new ScrollPane(tagContent);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setStyle("-fx-background-color: transparent; -fx-background: #f4f4f4;");

        BorderPane sidebarRoot = new BorderPane();
        sidebarRoot.setTop(headerBox);
        sidebarRoot.setCenter(scrollWrapper);
        sidebarRoot.setPrefWidth(170);
        sidebarRoot.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");
        this.setLeft(sidebarRoot);

        // --- 2. Center Tabs ---
        tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: white;");
        openTab(getCurrentWeekName());

        // Top Bar
        Button printBtn = new Button("🖨 Print");
        printBtn.setOnAction(e -> printCurrentTab());
        Button addTabBtn = new Button("+ New Page");
        addTabBtn.setOnAction(e -> openTab("New Page " + (tabPane.getTabs().size() + 1)));

        HBox topControls = new HBox(10, spacer(), printBtn, addTabBtn);
        topControls.setPadding(new Insets(5, 10, 5, 10));
        topControls.setAlignment(Pos.CENTER_RIGHT);
        topControls.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        VBox centerLayout = new VBox(topControls, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        this.setCenter(centerLayout);

        refreshSidebar();
    }

    public void save() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null && currentTab.getContent() instanceof HTMLEditor) {
            HTMLEditor editor = (HTMLEditor) currentTab.getContent();
            saveTab(currentTab, editor.getHtmlText());
        }
    }

    // --- Sidebar Logic ---
    private void refreshSidebar() {
        tagSidebar.getChildren().clear();
        tagSidebar.getChildren().add(new Label("General"));
        tagSidebar.getChildren().add(createNavButton("📅 This Week", getCurrentWeekName(), "#e3f2fd", "#0d47a1"));
        tagSidebar.getChildren().add(new Separator());
        tagSidebar.getChildren().add(new Label("Active Projects"));

        List<String> projects = loadActiveProjects();
        if (projects.isEmpty()) {
            Label empty = new Label("(No active projects)");
            empty.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
            tagSidebar.getChildren().add(empty);
        } else {
            for (String p : projects) {
                tagSidebar.getChildren().add(createNavButton(p, p, generateColor(p), "#333333"));
            }
        }
    }

    private Button createNavButton(String label, String targetTabName, String bgHex, String textHex) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.BASELINE_LEFT);
        btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 10;", bgHex, textHex));
        btn.setOnAction(e -> openTab(targetTabName));
        return btn;
    }

    private String generateColor(String seed) {
        int hash = seed.hashCode();
        return String.format("#%02x%02x%02x", (hash & 0xFF0000) >> 16, (hash & 0x00FF00) >> 8, hash & 0x0000FF);
    }

    private List<String> loadActiveProjects() {
        List<String> list = new ArrayList<>();
        if (!Files.exists(PROJECTS_FILE)) return list;
        try (BufferedReader reader = Files.newBufferedReader(PROJECTS_FILE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 3 && "Active".equalsIgnoreCase(parts[2])) {
                    list.add(parts[0].replace(";", ","));
                }
            }
        } catch (Exception e) {}
        return list;
    }

    // --- Tab Management ---
    private void openTab(String title) {
        for (Tab t : tabPane.getTabs()) {
            if (t.getText().equals(title)) {
                tabPane.getSelectionModel().select(t);
                return;
            }
        }
        createNewTab(title);
    }

    private void createNewTab(String title) {
        Tab tab = new Tab(title);
        HTMLEditor editor = new HTMLEditor();
        editor.setPrefHeight(2000);

        // Customize the editor
        customizeEditor(editor);

        String filename = title.replaceAll("[^a-zA-Z0-9.-]", "_") + ".html";
        Path path = DATA_DIR.resolve(filename);
        if (Files.exists(path)) {
            try { editor.setHtmlText(new String(Files.readAllBytes(path))); } catch (IOException e) {}
        }

        editor.setOnKeyReleased(e -> saveTab(tab, editor.getHtmlText()));
        editor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) saveTab(tab, editor.getHtmlText());
        });

        tab.setContent(editor);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    // --- CUSTOMIZATION LOGIC ---

    private void customizeEditor(HTMLEditor editor) {
        // Wait for Skin to load
        if (editor.getSkin() != null) {
            Platform.runLater(() -> applyCustomizations(editor));
        } else {
            editor.skinProperty().addListener((obs, old, skin) -> {
                if (skin != null) Platform.runLater(() -> applyCustomizations(editor));
            });
        }
        enableKeyboardShortcuts(editor);
    }

    private void applyCustomizations(HTMLEditor editor) {
        Set<Node> toolbars = editor.lookupAll(".tool-bar");
        if (toolbars.size() < 2) return;

        // Toolbars are usually: [0] Top (Cut/Paste/Indent), [1] Bottom (Fonts/Color)
        ToolBar topToolbar = (ToolBar) toolbars.toArray()[0];
        ToolBar bottomToolbar = (ToolBar) toolbars.toArray()[1];

        // 1. Add "Insert Link" to Bottom Toolbar (Fits better with text tools)
        Button linkBtn = new Button("🔗");
        linkBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        linkBtn.setOnAction(e -> promptForLink(editor));
        bottomToolbar.getItems().addAll(new Separator(), linkBtn);

        // 2. Add "Print" to Top Toolbar (Far right)
        Button printBtn = new Button("🖨");
        printBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        printBtn.setOnAction(e -> printEditor(editor));
        topToolbar.getItems().addAll(new Separator(), printBtn);

        // 3. Rename "Paragraph" to "Normal Text"
        Node formatNode = editor.lookup(".format-menu-button");
        if (formatNode instanceof ComboBox) {
            ComboBox<String> formatCombo = (ComboBox<String>) formatNode;
            ObservableList<String> items = formatCombo.getItems();
            int pIndex = items.indexOf("Paragraph");
            if (pIndex >= 0) {
                items.set(pIndex, "Normal Text");
            }
            // Logic Fix: Ensure "Normal Text" actually sends the 'p' command
            formatCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if ("Normal Text".equals(newVal)) {
                    WebView view = (WebView) editor.lookup(".web-view");
                    if (view != null) view.getEngine().executeScript("document.execCommand('formatBlock', false, 'p')");
                }
            });
        }

        // 4. Sort Fonts (Put Popular ones at the top)
        Node fontNode = editor.lookup(".font-family-menu-button");
        if (fontNode instanceof ComboBox) {
            ComboBox<String> fontCombo = (ComboBox<String>) fontNode;
            ObservableList<String> fonts = fontCombo.getItems();

            // Define Popular Fonts
            List<String> popular = List.of("Arial", "Calibri", "Cambria", "Helvetica", "Times New Roman", "Verdana", "System");

            // 1. Separate existing fonts into Popular vs Others
            List<String> topList = new ArrayList<>();
            List<String> bottomList = new ArrayList<>();

            for (String f : fonts) {
                if (popular.contains(f)) topList.add(f);
                else bottomList.add(f);
            }

            // 2. Sort popular list to match our preferred order (optional) or just alpha
            Collections.sort(topList);
            Collections.sort(bottomList);

            // 3. Rebuild list
            fonts.clear();
            fonts.addAll(topList);
            fonts.addAll(bottomList);

            // select the first one (usually System or Arial) so it isn't blank
            if (!fonts.isEmpty()) fontCombo.getSelectionModel().select(0);
        }
    }

    private void enableKeyboardShortcuts(HTMLEditor editor) {
        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            boolean handled = true;
            KeyCode k = event.getCode();
            WebView view = (WebView) editor.lookup(".web-view");
            if (view == null) return;
            WebEngine engine = view.getEngine();

            if (event.isControlDown()) {
                if (k == KeyCode.B) engine.executeScript("document.execCommand('bold', false, null)");
                else if (k == KeyCode.I) engine.executeScript("document.execCommand('italic', false, null)");
                else if (k == KeyCode.U) engine.executeScript("document.execCommand('underline', false, null)");
                else if (k == KeyCode.K) promptForLink(editor); 
                else if (k == KeyCode.P) printEditor(editor);
                else if (k == KeyCode.BACK_SLASH) engine.executeScript("document.execCommand('removeFormat', false, null)");
                else if (event.isShiftDown()) {
                    if (null == k) handled = false;
                    else switch (k) {
                        case DIGIT7 -> engine.executeScript("document.execCommand('insertOrderedList', false, null)");
                        case DIGIT8 -> engine.executeScript("document.execCommand('insertUnorderedList', false, null)");
                        case DIGIT9 -> engine.executeScript("document.execCommand('insertHtml', false, '<input type=\"checkbox\"> ')");
                        case L -> engine.executeScript("document.execCommand('justifyLeft', false, null)");
                        case E -> engine.executeScript("document.execCommand('justifyCenter', false, null)");
                        case R -> engine.executeScript("document.execCommand('justifyRight', false, null)");
                        case J -> engine.executeScript("document.execCommand('justifyFull', false, null)");
                        default -> handled = false;
                    }
                }
                else if (k == KeyCode.OPEN_BRACKET) engine.executeScript("document.execCommand('outdent', false, null)");
                else if (k == KeyCode.CLOSE_BRACKET) engine.executeScript("document.execCommand('indent', false, null)");
                else handled = false;

            } else if (event.isAltDown() && event.isShiftDown() && k == KeyCode.DIGIT5) {
                engine.executeScript("document.execCommand('strikeThrough', false, null)");
            } else {
                handled = false;
            }
            if (handled) event.consume();
        });
    }

    private void promptForLink(HTMLEditor editor) {
        TextInputDialog dialog = new TextInputDialog("https://");
        dialog.setTitle("Insert Link");
        dialog.setHeaderText(null);
        dialog.setContentText("URL:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(url -> {
            WebView view = (WebView) editor.lookup(".web-view");
            if (view != null) view.getEngine().executeScript("document.execCommand('createLink', false, '" + url + "')");
        });
    }

    private void printCurrentTab() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        if (current != null && current.getContent() instanceof HTMLEditor) printEditor((HTMLEditor) current.getContent());
    }

    private void printEditor(HTMLEditor editor) {
        WebView view = (WebView) editor.lookup(".web-view");
        if (view != null) {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(getScene().getWindow())) {
                view.getEngine().print(job);
                job.endJob();
            }
        }
    }

    private void saveTab(Tab tab, String content) {
        String filename = tab.getText().replaceAll("[^a-zA-Z0-9.-]", "_") + ".html";
        try (BufferedWriter writer = Files.newBufferedWriter(DATA_DIR.resolve(filename))) {
            writer.write(content);
        } catch (IOException e) {}
    }

    private String getCurrentWeekName() {
        LocalDate date = LocalDate.now();
        return "Week " + date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()) + " (" + date.getYear() + ")";
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}