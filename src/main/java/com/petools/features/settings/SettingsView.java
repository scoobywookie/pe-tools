package com.petools.features.settings;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        // --- Section 2: Diagnostics (Fixed for Installer) ---
        VBox diagSection = new VBox(10);
        diagSection.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 0, 0, 0, 1); -fx-background-radius: 5;");

        Label diagLabel = new Label("System Paths & Diagnostics");
        diagLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Use the Safe User Home Folder for Scripts
        // (C:\Users\Name\.petools\scripts)
        String userHome = System.getProperty("user.home");
        Path scriptDir = Paths.get(userHome, ".petools", "scripts");
        String safeScriptPath = scriptDir.toAbsolutePath().toString();

        Button openScriptFolder = new Button("Open Scripts Folder");
        openScriptFolder.setOnAction(e -> openFolder(scriptDir));

        diagSection.getChildren().addAll(
            diagLabel,
            new Separator(),
            createInfoRow("User Home:", userHome),
            createInfoRow("Safe Scripts Path:", safeScriptPath),
            new Label("(Drop your .exe / .py scripts here so they persist after updates)"),
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

    private void openFolder(Path path) {
        try {
            // 1. Create directory if it doesn't exist
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            // 2. Open in Windows Explorer
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (IOException e) {}
    }
}