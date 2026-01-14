package com.petools.layout;

import com.petools.features.autocad.AutoCADView;
import com.petools.features.projects.ProjectView;
import com.petools.features.settings.SettingsView;
import com.petools.features.sitelocator.SiteLocatorView;
import com.petools.features.todo.TodoView;
import com.petools.features.website.WebsiteView;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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
            mainLayout.setView(new ProjectView());
        });

        todoBtn.setOnAction(e -> {
            updateSelection(todoBtn);
            mainLayout.setView(cachedTodoView);
            cachedTodoView.save();
        });

        cadToolsBtn.setOnAction(e -> {
            updateSelection(cadToolsBtn);
            mainLayout.setView(new AutoCADView());
        });

        siteLocatorBtn.setOnAction(e -> {
            updateSelection(siteLocatorBtn);
            mainLayout.setView(new SiteLocatorView());
        });

        webMngmntBtn.setOnAction(e -> {
            updateSelection(webMngmntBtn);
            mainLayout.setView(new WebsiteView());
        });

        settingsBtn.setOnAction(e -> {
            updateSelection(settingsBtn);
            mainLayout.setView(new SettingsView());
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // 4. Add to Layout
        this.getChildren().addAll(
            homeBtn, todoBtn, projectDataBtn,
            cadToolsBtn, siteLocatorBtn, webMngmntBtn,
            spacer, settingsBtn
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