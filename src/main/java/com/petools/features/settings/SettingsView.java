package com.petools.features.settings;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsView extends VBox {

    public SettingsView() {
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: #F9FBFD;");

        // --- Header ---
        Label header = new Label("Settings & About");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // --- Section 1: Application Info ---
        VBox infoSection = new VBox(10);
        infoSection.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 0, 0, 0, 1); -fx-background-radius: 5;");

        Label infoLabel = new Label("Application Information");
        infoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        infoSection.getChildren().addAll(
            infoLabel,
            new Separator(),
            createInfoRow("App Name:", "PE Tools (Place Engineering)"),
            createInfoRow("Version:", "1.0.0 (Release)"),
            createInfoRow("Developer:", "Daniel & Joe Puckett"),
            createInfoRow("Java Version:", System.getProperty("java.version"))
        );

        // --- Section 2: Diagnostics (Crucial for your Scripts) ---
        VBox diagSection = new VBox(10);
        diagSection.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 0, 0, 0, 1); -fx-background-radius: 5;");

        Label diagLabel = new Label("System Paths & Diagnostics");
        diagLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Calculate paths to show the user
        String userHome = System.getProperty("user.home");
        String currentDir = Paths.get("").toAbsolutePath().toString();
        String scriptPath = Paths.get("scripts", "address_to_scr.exe").toAbsolutePath().toString();

        Button openScriptFolder = new Button("Open Scripts Folder");
        openScriptFolder.setOnAction(e -> openFolder(Paths.get("scripts").toFile()));

        diagSection.getChildren().addAll(
            diagLabel,
            new Separator(),
            createInfoRow("User Home:", userHome),
            createInfoRow("Working Directory:", currentDir),
            createInfoRow("Expected Script Path:", scriptPath),
            new Label(""), // spacer
            openScriptFolder
        );

        this.getChildren().addAll(header, infoSection, diagSection);
    }

    private HBox createInfoRow(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-min-width: 150;");

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #555;");

        return new HBox(10, l, v);
    }

    private void openFolder(File file) {
        try {
            if (Desktop.isDesktopSupported() && file.exists()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {}
    }
}