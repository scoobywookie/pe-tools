package com.petools.layout;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class MainLayout extends BorderPane {

    public MainLayout() {
        // Initialize Sidebar and pass 'this' layout to it
        Sidebar sidebar = new Sidebar(this);

        setLeft(sidebar);

        // Set default view
        setCenter(new HomeView());
    }

    // Method to switch the center screen
    public void setView(Node view) {
        setCenter(view);
    }

    // Temporary helper until we move your Views (Projects, Todo, etc.)
    public void showPlaceholder(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 24px; -fx-text-fill: grey;");
        setCenter(new StackPane(label));
    }
}