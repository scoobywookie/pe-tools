package com.petools.features.sitelocator;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class SiteLocatorView extends BorderPane {

    private final WebView browser;
    private WebEngine engine;
    private TextField urlField;
    private final ComboBox<String> toolSelector;

    // Common Engineering Tools
    private static final String GOOGLE_MAPS = "https://www.google.com/maps";
    private static final String USDA_SOILS = "https://websoilsurvey.nrcs.usda.gov/app/";
    private static final String FEMA_FLOOD = "https://msc.fema.gov/portal/search";
    private static final String WETLANDS = "https://www.fws.gov/wetlands/data/mapper.html";
    private static final String USGS_TOPO = "https://ngmdb.usgs.gov/topoview/viewer/";

    public SiteLocatorView() {
        // --- Top Toolbar ---
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        // Tool Selector
        Label toolLabel = new Label("Engineering Tool:");
        toolLabel.setStyle("-fx-font-weight: bold;");

        toolSelector = new ComboBox<>(FXCollections.observableArrayList(
            "Google Maps (General)",
            "USDA Soil Survey (Soils)",
            "FEMA Flood Map (Flood Zones)",
            "US FWS Wetlands Mapper",
            "USGS TopoView (Elevation)"
        ));
        toolSelector.getSelectionModel().selectFirst();
        toolSelector.setPrefWidth(220);
        toolSelector.setOnAction(e -> loadSelectedTool());

        // Address/URL Bar
        urlField = new TextField();
        HBox.setHgrow(urlField, Priority.ALWAYS);
        urlField.setPromptText("Search or enter URL...");
        urlField.setOnAction(e -> loadUrl(urlField.getText()));

        // Browser Controls
        Button backBtn = new Button("←");
        backBtn.setOnAction(e -> engine.getHistory().go(-1));
        
        Button refreshBtn = new Button("⟳");
        refreshBtn.setOnAction(e -> engine.reload());

        Button openExternalBtn = new Button("Open in Browser");
        openExternalBtn.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white;");
        openExternalBtn.setOnAction(e -> openInSystemBrowser(engine.getLocation()));

        toolbar.getChildren().addAll(toolLabel, toolSelector, backBtn, refreshBtn, urlField, openExternalBtn);
        this.setTop(toolbar);

        // --- Browser ---
        browser = new WebView();
        engine = browser.getEngine();
        
        // Listen to URL changes to update the text bar
        engine.locationProperty().addListener((obs, oldVal, newVal) -> urlField.setText(newVal));

        // Load default
        loadSelectedTool();
        
        this.setCenter(browser);
    }

    private void loadSelectedTool() {
        String selected = toolSelector.getValue();
        switch (selected) {
            case "Google Maps (General)" -> engine.load(GOOGLE_MAPS);
            case "USDA Soil Survey (Soils)" -> engine.load(USDA_SOILS);
            case "FEMA Flood Map (Flood Zones)" -> engine.load(FEMA_FLOOD);
            case "US FWS Wetlands Mapper" -> engine.load(WETLANDS);
            case "USGS TopoView (Elevation)" -> engine.load(USGS_TOPO);
        }
    }

    private void loadUrl(String url) {
        if (!url.startsWith("http")) {
            // If it's not a URL, treat it as a Google Search
            url = "https://www.google.com/maps/search/" + url.replace(" ", "+");
        }
        engine.load(url);
    }

    private void openInSystemBrowser(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {}
        }
    }
}