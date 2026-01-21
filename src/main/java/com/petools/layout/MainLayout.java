package com.petools.layout;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

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
}