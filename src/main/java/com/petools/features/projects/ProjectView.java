package com.petools.features.projects;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public class ProjectView extends BorderPane {

    private final TableView<Project> table;
    private final ObservableList<Project> masterData;
    private final FilteredList<Project> filteredData;

    // Input Fields
    private final TextField pNameField, clientField, folderField;
    private final ComboBox<String> statusBox;

    // Save File: .petools/projects.csv
    private static final Path DATA_FILE = Paths.get(System.getProperty("user.home"), ".petools", "projects.csv");

    public ProjectView() {
        this.setStyle("-fx-background-color: #F9FBFD;");
        this.setPadding(new Insets(30));

        // --- 1. Header & Search ---
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("Project Database");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search Projects...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterList(newVal));

        topBar.getChildren().addAll(title, spacer, searchField);
        this.setTop(topBar);

        // --- 2. The Table ---
        table = new TableView<>();
        table.setEditable(true);

        masterData = FXCollections.observableArrayList();
        loadData();

        // Wrap the ObservableList in a FilteredList (initially displaying all data).
        filteredData = new FilteredList<>(masterData, p -> true);

        // Wrap the FilteredList in a SortedList.
        SortedList<Project> sortedData = new SortedList<>(filteredData);

        // Bind the SortedList comparator to the TableView comparator.
        // Otherwise, clicking the column header won't sort the list.
        sortedData.comparatorProperty().bind(table.comparatorProperty());

        // Add sorted (and filtered) data to the table.
        table.setItems(sortedData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // --- COL 1: Project Name (Editable) ---
        TableColumn<Project, String> colName = new TableColumn<>("Project Name");
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(e -> {
            e.getRowValue().nameProperty().set(e.getNewValue());
            saveData();
        });

        // --- COL 2: Client (Editable) ---
        TableColumn<Project, String> colClient = new TableColumn<>("Client");
        colClient.setCellValueFactory(data -> data.getValue().clientProperty());
        colClient.setCellFactory(TextFieldTableCell.forTableColumn());
        colClient.setOnEditCommit(e -> {
            e.getRowValue().clientProperty().set(e.getNewValue());
            saveData();
        });

        // --- COL 3: Status (Dropdown Edit + Colors) ---
        TableColumn<Project, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        // Define the options for the dropdown
        ObservableList<String> statusOptions = FXCollections.observableArrayList("Active", "Inactive", "On Hold", "Closed");

        // Custom Cell Factory: Combines Dropdown (ComboBoxTableCell) with your Custom Colors
        colStatus.setCellFactory(col -> new ComboBoxTableCell<>(statusOptions) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    switch (item) {
                        case "Active" -> setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                        case "Inactive" -> setStyle("-fx-text-fill: #d81414; -fx-font-weight: bold;");
                        case "On Hold" -> setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: #6c757d; -fx-font-weight: bold");
                    }
                }
            }
        });

        // Save when the user picks a new option
        colStatus.setOnEditCommit(e -> {
            e.getRowValue().statusProperty().set(e.getNewValue());
            saveData();
        });

        // --- COL 4: Folder Link (Double-Click to Edit) ---
        TableColumn<Project, String> colFolder = new TableColumn<>("Folder");
        colFolder.setSortable(false); // Folder paths usually don't need sorting, but you can set to true if desired
        colFolder.setPrefWidth(60);
        colFolder.setCellValueFactory(data -> data.getValue().folderPathProperty()); // Bind property

        colFolder.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("📂");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14px;");
                btn.setFocusTraversable(false);
                btn.setOnAction(e -> {
                    Project p = getTableView().getItems().get(getIndex());
                    openProjectFolder(p.getFolderPath());
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                // 1. Handle Double Click to Browse
                this.setOnMouseClicked((MouseEvent event) -> {
                    if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2 && !empty) {
                        chooseFolderForProject(getTableView().getItems().get(getIndex()));
                    }
                });

                if (empty) {
                    setGraphic(null);
                } else {
                    // Show button if path exists
                    if (item != null && !item.isEmpty()) {
                        setGraphic(btn);
                        Tooltip.install(btn, new Tooltip(item + "\n(Double-click cell to change)"));
                    } else {
                        // Show "Add" hint if missing
                        setGraphic(null);
                        setText("Add +");
                        setStyle("-fx-text-fill: #ccc; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        table.getColumns().addAll(colName, colClient, colStatus, colFolder);

        // Delete Key Handler
        table.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                deleteSelected();
            }
        });

        // Context Menu (Right Click)
        ContextMenu cm = new ContextMenu();

        // 1. Copy Tag Option
        MenuItem copyTagItem = new MenuItem("Copy Project Tag (for To-Do)");
        copyTagItem.setOnAction(e -> copyTagToClipboard());

        // 2. Chage Folder Path Option
        MenuItem changeFolderItem = new MenuItem("Change Folder Path...");
        changeFolderItem.setOnAction(e -> {
            Project p = table.getSelectionModel().getSelectedItem();
            if(p != null) chooseFolderForProject(p); // Opens the Browse Dialog
        });

        // 3. Delete Option
        MenuItem deleteItem = new MenuItem("Delete Project");
        deleteItem.setStyle("-fx-text-fill: red;");
        deleteItem.setOnAction(e -> deleteSelected());

        cm.getItems().addAll(copyTagItem, new SeparatorMenuItem(), deleteItem);
        table.setContextMenu(cm);

        this.setCenter(table);

        // --- 3. Add Project Form ---
        VBox inputSection = new VBox(10);
        inputSection.setPadding(new Insets(20, 0, 0, 0));

        Label addLabel = new Label("Add New Project");
        addLabel.setStyle("-fx-font-weight: bold;");

        HBox inputBar = new HBox(10);

        pNameField = new TextField();
        pNameField.setPromptText("Project Name");
        HBox.setHgrow(pNameField, Priority.ALWAYS);

        clientField = new TextField();
        clientField.setPromptText("Client");
        clientField.setPrefWidth(120);

        statusBox = new ComboBox<>(FXCollections.observableArrayList(statusOptions));
        statusBox.getSelectionModel().selectFirst();
        statusBox.setPrefWidth(100);

        // Folder Selection
        folderField = new TextField();
        folderField.setPromptText("Network Folder Path...");
        folderField.setPrefWidth(150);

        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Project Folder");
            File f = dc.showDialog(getScene().getWindow());
            if (f != null) folderField.setText(f.getAbsolutePath());
        });

        Button addBtn = new Button("Add");
        addBtn.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> addProject());

        inputBar.getChildren().addAll(pNameField, clientField, statusBox, folderField, browseBtn, addBtn);
        inputSection.getChildren().addAll(addLabel, inputBar);

        this.setBottom(inputSection);
    }

    // --- Actions ---

    private void chooseFolderForProject(Project p) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Update Folder for: " + p.getName());
        File f = dc.showDialog(getScene().getWindow());
        if (f != null) {
            p.folderPathProperty().set(f.getAbsolutePath());
            saveData();
            table.refresh(); // Force UI update
        }
    }

    private void addProject() {
        if (pNameField.getText().isEmpty()) return;

        Project newP = new Project(
            pNameField.getText(),
            clientField.getText(),
            statusBox.getValue(),
            folderField.getText()
        );
        masterData.add(newP);
        saveData();

        pNameField.clear();
        clientField.clear();
        folderField.clear();
    }

    private void deleteSelected() {
        Project selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            masterData.remove(selected);
            saveData();
        }
    }

    private void copyTagToClipboard() {
        Project selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Creates a tag like "#Barret Road"
            String tag = "#" + selected.getName();
            ClipboardContent content = new ClipboardContent();
            content.putString(tag);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private void openProjectFolder(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            File dir = new File(path);
            if (Desktop.isDesktopSupported() && dir.exists()) {
                Desktop.getDesktop().open(dir);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Folder not found: " + path);
                alert.showAndWait();
            }
        } catch (IOException e) {}
    }

    private void filterList(String text) {
        if (text == null || text.isEmpty()) {
            filteredData.setPredicate(p -> true);
        } else {
            String lower = text.toLowerCase();
            filteredData.setPredicate(p ->
                p.getName().toLowerCase().contains(lower) ||
                p.getClient().toLowerCase().contains(lower)
            );
        }
    }

    // --- Persistence (CSV) ---

    private void saveData() {
        try (BufferedWriter writer = Files.newBufferedWriter(DATA_FILE)) {
            for (Project p : masterData) {
                // CSV: Name,Client,Status,Folder
                String line = String.format("%s,%s,%s,%s",
                    escape(p.getName()),
                    escape(p.getClient()),
                    p.getStatus(),
                    escape(p.getFolderPath()));
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {}
    }

    private void loadData() {
        if (!Files.exists(DATA_FILE)) return;
        try (BufferedReader reader = Files.newBufferedReader(DATA_FILE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 4) {
                    String folder = (parts.length > 3) ? unescape(parts[3]) : "";
                    masterData.add(new Project(
                        unescape(parts[0]),
                        unescape(parts[1]),
                        parts[2],
                        folder
                    ));
                }
            }
        } catch (IOException e) {}
    }

    private String escape(String s) { return s == null ? "" : s.replace(",", ";"); }
    private String unescape(String s) { return s == null ? "" : s.replace(";", ","); }

    // --- Internal Model Class (Using Properties for TableView compatibility) --- //
    public static class Project {
        private final SimpleStringProperty name;
        private final SimpleStringProperty client;
        private final SimpleStringProperty status;
        private final SimpleStringProperty folderPath;

        public Project(String name, String client, String status, String folderPath) {
            this.name = new SimpleStringProperty(name);
            this.client = new SimpleStringProperty(client);
            this.status = new SimpleStringProperty(status);
            this.folderPath = new SimpleStringProperty(folderPath);
        }

        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }

        public String getClient() { return client.get(); }
        public SimpleStringProperty clientProperty() { return client; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }

        public String getFolderPath() { return folderPath.get(); }
        public SimpleStringProperty folderPathProperty() { return folderPath; }
    }
}