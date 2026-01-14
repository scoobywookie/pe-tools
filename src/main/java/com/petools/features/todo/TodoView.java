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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;

public class TodoView extends BorderPane {

    private TabPane tabPane;
    private final VBox tagSidebar;

    // File Paths
    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), ".petools", "notes");
    private static final Path PROJECTS_FILE = Paths.get(System.getProperty("user.home"), ".petools", "projects.csv");

    public TodoView() {
        // Ensure notes directory exists
        if (!Files.exists(DATA_DIR)) {
            try { Files.createDirectories(DATA_DIR); } catch (IOException e) {}
        }

        // --- 1. Left Sidebar (Project Navigation) ---
        VBox headerBox = new VBox(5);
        headerBox.setPadding(new Insets(10));

        Label tagHeader = new Label("Projects");
        tagHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 14px;");

        Button refreshBtn = new Button("↻ Refresh List");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setStyle("-fx-font-size: 10px; -fx-background-color: transparent; -fx-text-fill: #0078d7; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> refreshSidebar());

        headerBox.getChildren().addAll(tagHeader, refreshBtn);

        // Content Container
        VBox tagContent = new VBox(8);
        tagContent.setPadding(new Insets(10));
        tagContent.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollWrapper = new ScrollPane(tagContent);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setStyle("-fx-background-color: transparent; -fx-background: #f4f4f4;");
        scrollWrapper.setPrefViewportWidth(150);

        this.tagSidebar = tagContent;

        BorderPane sidebarRoot = new BorderPane();
        sidebarRoot.setTop(headerBox);
        sidebarRoot.setCenter(scrollWrapper);
        sidebarRoot.setPrefWidth(170);
        sidebarRoot.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");

        this.setLeft(sidebarRoot);

        // --- 2. Center (Tabs) ---
        tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: white;");

        // Load "Week X" tab by default if no tabs exist
        // Note: We don't load ALL files on startup anymore to keep it clean.
        // We only open the current week, and let the sidebar open project tabs.
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

        // 1. Weekly / General Navigation
        tagSidebar.getChildren().add(new Label("General"));
        tagSidebar.getChildren().add(createNavButton("📅 This Week", getCurrentWeekName(), "#e3f2fd", "#0d47a1"));

        tagSidebar.getChildren().add(new Separator());
        tagSidebar.getChildren().add(new Label("Active Projects"));

        // 2. Project List
        List<String> projects = loadActiveProjects();
        if (projects.isEmpty()) {
            Label empty = new Label("(No active projects)");
            empty.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
            tagSidebar.getChildren().add(empty);
        } else {
            for (String p : projects) {
                String color = generateColor(p);
                // Action: Open a tab with the Project Name
                tagSidebar.getChildren().add(createNavButton(p, p, color, "#333333"));
            }
        }
    }

    private Button createNavButton(String label, String targetTabName, String bgHex, String textHex) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.BASELINE_LEFT);
        btn.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 10;", 
            bgHex, textHex
        ));

        // ACTION: Switch to Tab
        btn.setOnAction(e -> openTab(targetTabName));

        return btn;
    }

    private String generateColor(String seed) {
        int hash = seed.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        r = (r + 255) / 2; g = (g + 255) / 2; b = (b + 255) / 2;
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private List<String> loadActiveProjects() {
        List<String> list = new ArrayList<>();
        if (!Files.exists(PROJECTS_FILE)) return list;

        try (BufferedReader reader = Files.newBufferedReader(PROJECTS_FILE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                // CSV: Name,Client,Status...
                if (parts.length >= 3) {
                    if ("Active".equalsIgnoreCase(parts[2])) {
                        String name = parts[0].replace(";", ",");
                        if (!name.isEmpty()) list.add(name);
                    }
                }
            }
        } catch (Exception e) {}
        return list;
    }

    // --- Tab Management ---

    private void openTab(String title) {
        // 1. Check if tab is already open
        for (Tab t : tabPane.getTabs()) {
            if (t.getText().equals(title)) {
                tabPane.getSelectionModel().select(t);
                return;
            }
        }

        // 2. If not, create it and try to load content
        createNewTab(title);
    }

    private void createNewTab(String title) {
        Tab tab = new Tab(title);

        HTMLEditor editor = new HTMLEditor();
        editor.setPrefHeight(2000);

        customizeEditor(editor);

        // Try to load existing content for this title
        String filename = title.replaceAll("[^a-zA-Z0-9.-]", "_") + ".html";
        Path path = DATA_DIR.resolve(filename);
        if (Files.exists(path)) {
            try {
                String content = new String(Files.readAllBytes(path));
                editor.setHtmlText(content);
            } catch (IOException e) {}
        }

        // Autosave logic
        editor.setOnKeyReleased(e -> saveTab(tab, editor.getHtmlText()));
        editor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) saveTab(tab, editor.getHtmlText());
        });

        tab.setContent(editor);

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void customizeEditor(HTMLEditor editor) {
        Platform.runLater(() -> {
            Set<Node> toolbars = editor.lookupAll(".tool-bar");
            if (toolbars.isEmpty()) return;

            // Inject Print Button next to Scissors
            ToolBar topToolbar = (ToolBar) toolbars.iterator().next();
            Button printBtn = new Button("🖨");
            printBtn.setTooltip(new Tooltip("Print Page"));
            printBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
            printBtn.setOnAction(e -> printEditor(editor));
            topToolbar.getItems().add(new Separator());
            topToolbar.getItems().add(printBtn);
        });
    }

    private void printCurrentTab() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        if (current != null && current.getContent() instanceof HTMLEditor) {
            printEditor((HTMLEditor) current.getContent());
        }
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
        } catch (IOException e) {
        }
    }

    private String getCurrentWeekName() {
        LocalDate date = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
        return "Week " + weekNumber + " (" + date.getYear() + ")";
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}