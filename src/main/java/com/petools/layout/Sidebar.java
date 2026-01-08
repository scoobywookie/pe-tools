package com.petools.layout;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import com.petools.features.todo.TodoView;

public class Sidebar extends VBox {

    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private MainLayout mainLayout;
    private TodoView cachedTodoView;

    // Exact styles from your old App.java
    private final String defaultButtonStyle = "-fx-background-color: #555555; -fx-text-fill: white;";
    private final String selectedButtonStyle = "-fx-background-color: #555555; -fx-text-fill: red;";

    private Button selectedButton;

    public Sidebar(MainLayout mainLayout) {
        this.mainLayout = mainLayout;

        // 1. Container Styling
        // MATCHES ORIGINAL: VBox(10) and padding 10
        this.setSpacing(10);
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #333333;");

        this.cachedTodoView = new TodoView();

        // 2. Create Buttons
        Button homeBtn = createNavButton("Home");
        Button dashboardBtn = createNavButton("Dashboard");
        Button todoBtn = createNavButton("To-Do List");
        Button projectDataBtn = createNavButton("Projects");
        Button cadToolsBtn = createNavButton("AutoCAD");
        Button siteLocatorBtn = createNavButton("Site Locator");
        Button webMngmntBtn = createNavButton("Website");
        Button settingsBtn = createNavButton("Settings");

        // 3. Define Actions
        homeBtn.setOnAction(e -> {
            updateSelection(homeBtn);
            mainLayout.setView(new HomeView());
        });

        projectDataBtn.setOnAction(e -> {
            updateSelection(projectDataBtn);
            mainLayout.setView(new com.petools.features.projects.ProjectView());
        });

        todoBtn.setOnAction(e -> {
            updateSelection(todoBtn);
            mainLayout.setView(cachedTodoView);
            cachedTodoView.save();
        });

        // Placeholders
        dashboardBtn.setOnAction(e -> { updateSelection(dashboardBtn); mainLayout.showPlaceholder("Dashboard"); });
        cadToolsBtn.setOnAction(e -> { updateSelection(cadToolsBtn); mainLayout.showPlaceholder("AutoCAD Automation Dashboard"); });
        siteLocatorBtn.setOnAction(e -> { updateSelection(siteLocatorBtn); mainLayout.showPlaceholder("Engineering Site Locator"); });
        webMngmntBtn.setOnAction(e -> { updateSelection(webMngmntBtn); mainLayout.showPlaceholder("Website - Project Map Management"); });
        settingsBtn.setOnAction(e -> { updateSelection(settingsBtn); mainLayout.showPlaceholder("Settings"); });

        // 4. Add to Layout
        this.getChildren().addAll(
            homeBtn, dashboardBtn, todoBtn, projectDataBtn,
            cadToolsBtn, siteLocatorBtn, webMngmntBtn,
            settingsBtn
        );

        // 5. Set Initial Selection
        updateSelection(homeBtn);
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        // MATCHES ORIGINAL: expanding to fill the VBox width
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(defaultButtonStyle);
        return btn;
    }

    private void updateSelection(Button newSelected) {
        if (selectedButton != null) {
            selectedButton.setStyle(defaultButtonStyle);
        }
        selectedButton = newSelected;
        selectedButton.setStyle(selectedButtonStyle);
    }
}