package com.petools.layout;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HomeView extends VBox {

    public HomeView() {
        // 1. Container Styling
        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: #ffffff;");

        // 2. The Label
        Label titleLabel = new Label("Place Engineering Tools");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");
        // this.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 20;");

        this.getChildren().add(titleLabel);
    }
}