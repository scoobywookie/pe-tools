package com.petools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    private Button selectedButton;
    private final String defaultButtonStyle = "-fx-background-color: #555555; -fx-text-fill: white;";
    private final String selectedButtonStyle = "-fx-background-color: #555555; -fx-text-fill: red;";
    private TodoController todoController;

    private ObservableList<String> todoItems;
    private final Path todoFilePath = getTodoFilePath();
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
    @Override
    public void start(Stage stage) {
        // home content area
        Label label = new Label("Place Engineering Tools");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        StackPane homeView = new StackPane(label);
        homeView.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 20;");
        StackPane.setAlignment(label, Pos.TOP_CENTER);

        // Sidebar
        VBox sidebar = new VBox(10);
        sidebar.setStyle("-fx-background-color: #333333; -fx-padding: 10;");
        ArrayList<Button> buttons = new ArrayList<>();
        Button homeBtn = new Button("Home");
        buttons.add(homeBtn);

        Button dashboardBtn = new Button("Dashboard");
        buttons.add(dashboardBtn);

        Button todoBtn = new Button("To-Do List");
        buttons.add(todoBtn);

        Button projectDataBtn = new Button("Project");
        buttons.add(projectDataBtn);

        Button cadToolsBtn = new Button("AutoCAD");
        buttons.add(cadToolsBtn);

        Button siteLocatorBtn = new Button("Site Locator");
        buttons.add(siteLocatorBtn);

        Button webMngmntBtn = new Button("Website");
        buttons.add(webMngmntBtn);

        Button settingsBtn = new Button("Settings");
        buttons.add(settingsBtn);

        for (Button btn : buttons) {
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle(defaultButtonStyle);
        }

        sidebar.getChildren().addAll(buttons);

        // Set initial selected button
        selectedButton = homeBtn;
        selectedButton.setStyle(selectedButtonStyle);

        // Layout
        BorderPane root = new BorderPane(homeView);
        root.setCenter(homeView);
        root.setLeft(sidebar);

        //To-Do List View
        VBox todoView = new VBox(10);
        todoView.setPadding(new Insets(20));
        todoView.setStyle("-fx-background-color: #f4f4f4;");
        Label todoHeader = new Label("To-Do List Management");
        todoHeader.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        todoItems = loadTodoItems(); // Load tasks from file
        ListView<String> todoListView = new ListView<>(todoItems);
        todoListView.setEditable(true);
        todoListView.setCellFactory(TextFieldListCell.forListView());

        todoListView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE){
                String selectedItem = todoListView.getSelectionModel().getSelectedItem();
                    if(selectedItem != null){
                        todoItems.remove(selectedItem);
                    }
            }
        });

        HBox todoInputBox = new HBox(10);
        TextField newTodoField = new TextField();
        newTodoField.setPromptText("Enter a new to-do item and press Enter");
        HBox.setHgrow(newTodoField, Priority.ALWAYS);
        Button addTodoBtn = new Button("Add");
        Button deleteTodoBtn = new Button("Delete Selected");
        todoInputBox.getChildren().addAll(newTodoField, addTodoBtn, deleteTodoBtn);
        todoView.getChildren().addAll(todoHeader, todoListView, todoInputBox);
        VBox.setVgrow(todoListView, Priority.ALWAYS);

        // To-Do list logic
        addTodoBtn.setOnAction(e -> {
            String text = newTodoField.getText();
            if(text != null && !text.trim().isEmpty()){
                todoItems.add(text.trim());
                newTodoField.clear();
            }
        });
        newTodoField.setOnAction(addTodoBtn.getOnAction()); // Trigger add on Enter key

        deleteTodoBtn.setOnAction(e -> {
            String selectedItem = todoListView.getSelectionModel().getSelectedItem();
            if(selectedItem != null){
                todoItems.remove(selectedItem);
            }
        });

        // Navigation logic
        homeBtn.setOnAction(e -> {
            label.setText("Place Engineering Tools");
            updateSelection(homeBtn);
            root.setCenter(homeView);
        });
        dashboardBtn.setOnAction(e -> {
            label.setText("Dashboard View");
            updateSelection(dashboardBtn);
            root.setCenter(homeView);
        });
        todoBtn.setOnAction(e -> {
            updateSelection(todoBtn);
            root.setCenter(todoView);
            root.setCenter(loadView("todoview.fxml"));
        });
        projectDataBtn.setOnAction(e -> {
            label.setText("Project Data Management");
            updateSelection(projectDataBtn);
            root.setCenter(homeView);
        });
        cadToolsBtn.setOnAction(e -> {
            label.setText("AutoCAD Automation Dashboard");
            updateSelection(cadToolsBtn);
            root.setCenter(homeView);
        });
        siteLocatorBtn.setOnAction(e -> {
            label.setText("Engineering Site Locator");
            updateSelection(siteLocatorBtn);
            root.setCenter(homeView);
        });
        webMngmntBtn.setOnAction(e -> {
            label.setText("Website - Project Map Management");
            updateSelection(webMngmntBtn);
            root.setCenter(homeView);
        });
        settingsBtn.setOnAction(e -> {
            label.setText("Settings");
            updateSelection(settingsBtn);
            root.setCenter(homeView);
        });

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("PE Tools");
        stage.getIcons().add(new Image(App.class.getResourceAsStream("/com/petools/images/pe_logo.png")));
        stage.setScene(scene);
        stage.show();
    }

    private void updateSelection(Button newSelected) {
        if (selectedButton != null) {
            selectedButton.setStyle(defaultButtonStyle);
        }
        selectedButton = newSelected;
        selectedButton.setStyle(selectedButtonStyle);
    }

    private Path getTodoFilePath() {
        String userHome = System.getProperty("user.home");
        // Store data in a hidden directory in the user's home folder
        Path appDataDir = Paths.get(userHome, ".petools");
        return appDataDir.resolve("todos.txt");
    private Node loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlFile));
            Parent view = loader.load();
            // If we are loading the to-do view, get its controller to save data later
            if ("todoview.fxml".equals(fxmlFile)) {
                todoController = loader.getController();
            }
            return view;
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading view: " + fxmlFile);
        }
    }

    @Override
    public void stop() throws Exception {
        saveTodoItems();
        if (todoController != null) {
            todoController.saveTodoItems();
        }
        super.stop();
    }

    private ObservableList<String> loadTodoItems() {
        if (Files.exists(todoFilePath)) {
            try {
                return FXCollections.observableArrayList(Files.readAllLines(todoFilePath));
            } catch (IOException e) {
                System.err.println("Error loading to-do items: " + e.getMessage());
            }
        }
        return FXCollections.observableArrayList("Task 1");
    }

    private void saveTodoItems() {
        try {
            Files.createDirectories(todoFilePath.getParent()); // Ensure the directory exists
            Files.write(todoFilePath, todoItems);
        } catch (IOException e) {
            System.err.println("Error saving to-do items: " + e.getMessage());
        }
    }
}