package com.petools.layout;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HomeView extends VBox {

    public HomeView() {
        // Container Styling
        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: #ffffff;");

        // The Label
        Label titleLabel = new Label("Place Engineering Tools");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        this.getChildren().add(titleLabel);
    }
}