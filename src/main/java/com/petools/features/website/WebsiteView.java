package com.petools.features.website;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebsiteView extends BorderPane {

    private static final String MAP_URL = "https://www.google.com/maps/d/u/0/viewer?mid=12MlD1Vic0XE2ST1EzhAce3MfHzGWMKs&femb=1&ll=35.78037777269373%2C-78.64725654537686&z=14"; 

    public WebsiteView() {
        // --- Top Toolbar ---
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("Project Map Viewer");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openExternalBtn = new Button("Open in Chrome / Edge");
        openExternalBtn.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand;");
        openExternalBtn.setOnAction(e -> openInSystemBrowser(MAP_URL));

        // CHANGED: "Home/Reset" logic instead of just "Reload"
        Button resetBtn = new Button("⟳ Reset Map");
        resetBtn.setOnAction(e -> {
            WebView view = (WebView) this.getCenter();
            // Forces the browser back to the starting URL
            view.getEngine().load(MAP_URL);
        });

        toolbar.getChildren().addAll(titleLabel, spacer, resetBtn, openExternalBtn);
        this.setTop(toolbar);

        // --- The Map (Center) ---
        WebView mapView = new WebView();
        WebEngine engine = mapView.getEngine();

        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        engine.load(MAP_URL);
        this.setCenter(mapView);
    }

    private void openInSystemBrowser(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {}
        }
    }
}