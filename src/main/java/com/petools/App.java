package com.petools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class App extends Application {

    private Button selectedButton;
    private final String defaultButtonStyle = "-fx-background-color: #555555; -fx-text-fill: white;";
    private final String selectedButtonStyle = "-fx-background-color: #555555; -fx-text-fill: red;";

    private ObservableList<TodoItem> todoItems;
    private ObservableList<Project> projectList; // List of Project Objects
    private ObservableList<String> projectNames; // Helper list for dropdowns

    private final Path todoFilePath = getTodoFilePath();
    private final Path projectFilePath = getProjectFilePath();

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }

    @Override
    public void start(Stage stage) {
        // 1. Load Data
        projectList = loadProjects();
        todoItems = loadTodoItems();

        // 2. Create a synchronized list of Strings for the Table ComboBoxes
        projectNames = FXCollections.observableArrayList();
        for (Project p : projectList) projectNames.add(p.name);

        projectList.addListener((ListChangeListener<Project>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Project p : c.getAddedSubList()) projectNames.add(p.name);
                }
                if (c.wasRemoved()) {
                    for (Project p : c.getRemoved()) projectNames.remove(p.name);
                }
            }
        });

        // --- Home View ---
        Label label = new Label("Place Engineering Tools");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        StackPane homeView = new StackPane(label);
        homeView.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 20;");
        StackPane.setAlignment(label, Pos.TOP_CENTER);

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

        selectedButton = homeBtn;
        selectedButton.setStyle(selectedButtonStyle);

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
                else setText(item.getName()); // This ensures the list view shows the name
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

        newProjectNameField.setOnAction(addProjectBtn.getOnAction()); // Trigger add on Enter key

        deleteProjectBtn.setOnAction(e -> {
            Project selected = projectListView.getSelectionModel().getSelectedItem();
            if (selected != null) projectList.remove(selected);
        });

        // Delete on key press
        projectListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                Project selectedItem = projectListView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) projectList.remove(selectedItem);
            }
        });

        projectInputBox.getChildren().addAll(newProjectNameField, addProjectBtn, deleteProjectBtn);
        projectView.getChildren().addAll(projectHeader, projectListView, projectInputBox);
        VBox.setVgrow(projectListView, Priority.ALWAYS);


        /* ======================= */
        /* --- To-Do List View --- */
        /* ======================= */
        VBox todoView = new VBox(10);
        todoView.setPadding(new Insets(20));
        todoView.setStyle("-fx-background-color: #ffffff;");

        Label todoHeader = new Label("Project Task Tracker");
        todoHeader.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        TableView<TodoItem> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Delete on key press
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                TodoItem selectedItem = table.getSelectionModel().getSelectedItem();
                if (selectedItem != null) todoItems.remove(selectedItem);
            }
        });

        // Row Factory (Gray out done items)
        table.setRowFactory(tv -> {
            TableRow<TodoItem> row = new TableRow<>();
            ChangeListener<Boolean> completedListener = (obs, oldVal, newVal) -> {
                if (newVal) row.setStyle("-fx-background-color: #f0f0f0; -fx-opacity: 0.5; -fx-font-style: italic;");
                else row.setStyle("");
            };
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (oldItem != null) oldItem.completedProperty().removeListener(completedListener);
                if (newItem != null) {
                    newItem.completedProperty().addListener(completedListener);
                    if (newItem.getCompleted()) row.setStyle("-fx-background-color: #f0f0f0; -fx-opacity: 0.5; -fx-font-style: italic;");
                    else row.setStyle("");
                } else row.setStyle("");
            });
            return row;
        });

        /* --- COLUMNS --- */

        // 1. Done
        TableColumn<TodoItem, Boolean> checkCol = new TableColumn<>("Done");
        checkCol.setCellValueFactory(new PropertyValueFactory<>("completed"));
        checkCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkCol));
        checkCol.setMaxWidth(50);
        checkCol.setSortType(TableColumn.SortType.ASCENDING); // Default sort

        // Bind the SortedList comparator to the TableView comparator.
        SortedList<TodoItem> sortedData = new SortedList<>(todoItems);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
        table.getSortOrder().add(checkCol); // Apply default sort

        // 2. Project Column
        // NOTE: We name this "Project". If it showed "Class Name" before, this fixes it.
        TableColumn<TodoItem, String> projectCol = new TableColumn<>("Project"); 
        projectCol.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        // This uses the list of Strings (projectNames), so it will definitely display text.
        projectCol.setCellFactory(ComboBoxTableCell.forTableColumn(projectNames));
        projectCol.setOnEditCommit(e -> e.getRowValue().setProjectName(e.getNewValue()));

        // 3. Description
        TableColumn<TodoItem, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(e -> e.getRowValue().setDescription(e.getNewValue()));

        // 4. Priority
        TableColumn<TodoItem, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityCol.setCellFactory(tc -> new ComboBoxTableCell<TodoItem, String>("High", "Medium", "Low") {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); setText(null); } 
                else {
                    if (item.equalsIgnoreCase("High")) setStyle("-fx-background-color: #ffcccc; -fx-text-fill: #cc0000; -fx-alignment: CENTER;");
                    else if (item.equalsIgnoreCase("Medium")) setStyle("-fx-background-color: #fff4cc; -fx-text-fill: #b38600; -fx-alignment: CENTER;");
                    else if (item.equalsIgnoreCase("Low")) setStyle("-fx-background-color: #ccffcc; -fx-text-fill: #006600; -fx-alignment: CENTER;");
                    else setStyle("-fx-alignment: CENTER;");
                }
            }
        });
        priorityCol.setOnEditCommit(e -> e.getRowValue().setPriority(e.getNewValue()));

        // 5. Status
        TableColumn<TodoItem, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(tc -> new ComboBoxTableCell<TodoItem, String>("Not Started", "In Progress", "On Hold", "Passed", "Done") {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); setText(null); } 
                else {
                    if (item.equalsIgnoreCase("In Progress")) setStyle("-fx-background-color: #e6f7ff; -fx-text-fill: #0066cc; -fx-alignment: CENTER;");
                    else if (item.equalsIgnoreCase("Passed") || item.equalsIgnoreCase("Done")) setStyle("-fx-background-color: #ccffcc; -fx-text-fill: #006600; -fx-alignment: CENTER;");
                    else if (item.equalsIgnoreCase("Not Started")) setStyle("-fx-background-color: #f2f2f2; -fx-text-fill: #666666; -fx-alignment: CENTER;");
                    else if (item.equalsIgnoreCase("On Hold")) setStyle("-fx-background-color: #e6f2ff; -fx-text-fill: #0059b3; -fx-alignment: CENTER;");
                    else setStyle("-fx-alignment: CENTER;");
                }
            }
        });
        statusCol.setOnEditCommit(e -> e.getRowValue().setStatus(e.getNewValue()));

        // 6. Date
        TableColumn<TodoItem, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setCellFactory(col -> new DateEditingCell());
        dateCol.setOnEditCommit(e -> e.getRowValue().setDate(e.getNewValue()));

        table.getColumns().add(checkCol);
        table.getColumns().add(projectCol);
        table.getColumns().add(descCol);
        table.getColumns().add(priorityCol);
        table.getColumns().add(statusCol);
        table.getColumns().add(dateCol);

        /* --- Input Area (Using ComboBox for Project) --- */
        HBox inputBox = new HBox(10);

        // Input: Project Selection Dropdown
        ComboBox<Project> projectInput = new ComboBox<>(projectList);
        projectInput.setPromptText("Select Project");
        projectInput.setPrefWidth(150);

        // Force the ComboBox to display the NAME, not the object code
        projectInput.setConverter(new StringConverter<Project>() {
            @Override
            public String toString(Project project) {
                if (project == null) return null;
                return project.getName();
            }
            @Override
            public Project fromString(String string) {
                return null; // No need to convert back from string for a dropdown selection
            }
        });

        TextField descInput = new TextField(); descInput.setPromptText("Description");
        ComboBox<String> priInput = new ComboBox<>(); priInput.getItems().addAll("High", "Medium", "Low"); priInput.setPromptText("Priority");
        ComboBox<String> statInput = new ComboBox<>(); statInput.getItems().addAll("Not Started", "In Progress", "On Hold", "Passed"); statInput.setPromptText("Status");
        DatePicker dateInput = new DatePicker(); dateInput.setPromptText("Date");

        Button addBtn = new Button("Add Task");
        Button deleteBtn = new Button("Delete Selected");

        inputBox.getChildren().addAll(projectInput, descInput, priInput, statInput, dateInput, addBtn, deleteBtn);

        addBtn.setOnAction(e -> {
            String projName = (projectInput.getValue() != null) ? projectInput.getValue().name : "General";
            String dateStr = (dateInput.getValue() != null) ? dateInput.getValue().format(DateTimeFormatter.ofPattern("dd-MMM-yy")) : "TBD";

            todoItems.add(new TodoItem(
                false,
                projName,
                descInput.getText().isEmpty() ? "Task" : descInput.getText(),
                priInput.getValue() == null ? "Low" : priInput.getValue(),
                statInput.getValue() == null ? "Not Started" : statInput.getValue(),
                dateStr
            ));
            descInput.clear(); dateInput.setValue(null);
        });

        deleteBtn.setOnAction(e -> {
            TodoItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) todoItems.remove(selected);
        });

        todoView.getChildren().addAll(todoHeader, inputBox, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Navigation
        homeBtn.setOnAction(e -> { label.setText("Place Engineering Tools"); updateSelection(homeBtn); root.setCenter(homeView); });
        dashboardBtn.setOnAction(e -> { label.setText("Dashboard View"); updateSelection(dashboardBtn); root.setCenter(homeView); });
        todoBtn.setOnAction(e -> { updateSelection(todoBtn); root.setCenter(todoView); });

        // Point to Project View
        projectDataBtn.setOnAction(e -> { updateSelection(projectDataBtn); root.setCenter(projectView); });

        cadToolsBtn.setOnAction(e -> { label.setText("AutoCAD Automation Dashboard"); updateSelection(cadToolsBtn); root.setCenter(homeView); });
        siteLocatorBtn.setOnAction(e -> { label.setText("Engineering Site Locator"); updateSelection(siteLocatorBtn); root.setCenter(homeView); });
        webMngmntBtn.setOnAction(e -> { label.setText("Website - Project Map Management"); updateSelection(webMngmntBtn); root.setCenter(homeView); });
        settingsBtn.setOnAction(e -> { label.setText("Settings"); updateSelection(settingsBtn); root.setCenter(homeView); });

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("PE Tools");
        try { stage.getIcons().add(new Image(App.class.getResourceAsStream("/com/petools/images/pe_logo.png"))); } catch (Exception e) { }
        stage.setScene(scene);
        stage.show();
    }

    private void updateSelection(Button newSelected) {
        if (selectedButton != null) selectedButton.setStyle(defaultButtonStyle);
        selectedButton = newSelected;
        selectedButton.setStyle(selectedButtonStyle);
    }

    private Path getTodoFilePath() {
        String userHome = System.getProperty("user.home");
        Path appDataDir = Paths.get(userHome, ".petools");
        return appDataDir.resolve("todos_v2.csv");
    }

    private Path getProjectFilePath() {
        String userHome = System.getProperty("user.home");
        Path appDataDir = Paths.get(userHome, ".petools");
        return appDataDir.resolve("projects.txt");
    }

    @Override
    public void stop() throws Exception {
        saveTodoItems();
        saveProjects();
        super.stop();
    }

    /* --- Data Loading --- */
    private ObservableList<Project> loadProjects() {
        ObservableList<Project> list = FXCollections.observableArrayList();
        if (Files.exists(projectFilePath)) {
            try {
                List<String> lines = Files.readAllLines(projectFilePath);
                for(String line : lines) {
                    if(!line.trim().isEmpty()) list.add(new Project(line.trim()));
                }
            } catch (IOException e) {
                System.err.println("Error loading projects: " + e.getMessage());
            }
        }
        if(list.isEmpty()) {
            list.add(new Project("Site A - Grading"));
            list.add(new Project("Bridge Analysis"));
            list.add(new Project("Hwy 101 Expansion"));
        }
        return list;
    }

    private void saveProjects() {
        try {
            Files.createDirectories(projectFilePath.getParent());
            List<String> lines = new ArrayList<>();
            for(Project p : projectList) lines.add(p.name);
            Files.write(projectFilePath, lines);
        } catch(IOException e) { System.err.println("Error saving projects: " + e.getMessage()); }
    }

    private ObservableList<TodoItem> loadTodoItems() {
        ObservableList<TodoItem> items = FXCollections.observableArrayList(item ->
            new Observable[]{item.completedProperty()});

        if (Files.exists(todoFilePath)) {
            try {
                List<String> lines = Files.readAllLines(todoFilePath);
                for (String line : lines) {
                    String[] parts = line.split(";", -1);
                    if (parts.length >= 6) {
                        items.add(new TodoItem(
                            Boolean.parseBoolean(parts[0]),
                            parts[1], parts[2], parts[3], parts[4], parts[5]
                        ));
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading: " + e.getMessage());
            }
        } else {
            items.add(new TodoItem(false, "Site A - Grading", "Review Topo Survey", "High", "In Progress", "01-Nov-25"));
            items.add(new TodoItem(false, "Bridge Analysis", "Check Load Factors", "Medium", "Not Started", "05-Nov-25"));
        }
        return items;
    }

    private void saveTodoItems() {
        try {
            Files.createDirectories(todoFilePath.getParent());
            List<String> lines = new ArrayList<>();
            for (TodoItem item : todoItems) {
                String line = String.join(";",
                    String.valueOf(item.getCompleted()),
                    item.getProjectName(),
                    item.getDescription(),
                    item.getPriority(),
                    item.getStatus(),
                    item.getDate()
                );
                lines.add(line);
            }
            Files.write(todoFilePath, lines);
        } catch (IOException e) {
            System.err.println("Error saving: " + e.getMessage());
        }
    }
}