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

    // --- 1. DEFINE CACHED VIEWS HERE ---
    private TodoView cachedTodoView;
    private AutoCADView cachedAutoCADView; // Cache the AutoCAD view

    // Styles for tab buttons
    private final String defaultButtonStyle = "-fx-background-color: #555555; -fx-text-fill: white;";
    private final String selectedButtonStyle = "-fx-background-color: #555555; -fx-text-fill: red;";

    private Button selectedButton;

    public Sidebar(MainLayout mainLayout) {
        this.mainLayout = mainLayout;

        // Container Styling
        this.setSpacing(10);
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #333333;");

        // --- 2. INITIALIZE CACHED VIEWS ---
        this.cachedTodoView = new TodoView();
        this.cachedAutoCADView = new AutoCADView();

        // Create Buttons
        Button homeBtn = createNavButton("Home");
        Button todoBtn = createNavButton("To-Do List");
        Button projectDataBtn = createNavButton("Projects");
        Button cadToolsBtn = createNavButton("AutoCAD");
        Button siteLocatorBtn = createNavButton("Site Locator");
        Button webMngmntBtn = createNavButton("Website");
        Button settingsBtn = createNavButton("Settings");

        // Define Actions
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
            // --- 3. USE THE CACHED VIEW ---
            mainLayout.setView(cachedAutoCADView);
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

        // Add to Layout
        this.getChildren().addAll(
            homeBtn, todoBtn, projectDataBtn,
            cadToolsBtn, siteLocatorBtn, webMngmntBtn,
            spacer, settingsBtn
        );

        // Set Initial Selection
        updateSelection(homeBtn);
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
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