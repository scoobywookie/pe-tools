package com.petools.features.autocad;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AutoCADView extends VBox {

    private final TextField addressField;
    private final CheckBox downloadLayersCheck;
    private final TextArea consoleLog;

    // PORTABLE PATH: Looks for "scripts/address_to_scr.exe" inside the app folder
    private static final Path SCRIPT_DIR = Paths.get(System.getProperty("user.home"), ".petools", "scripts");
    private static final Path SCRIPT_PATH = SCRIPT_DIR.resolve("address_to_scr.exe");

    public AutoCADView() {
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: #F9FBFD;");

        // --- Header ---
        Label header = new Label("AutoCAD Automation Dashboard");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // --- 1. SELF-HEAL CHECK (Extract .exe if missing) ---
        ensureScriptExists();

        // --- Section 1: Site Setup ---
        VBox siteSection = new VBox(10);
        siteSection.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 0, 0, 0, 1); -fx-background-radius: 5;");

        Label section1Label = new Label("1. Site Import Automation");
        section1Label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label addrLabel = new Label("Project Address:");
        addrLabel.setStyle("-fx-font-weight: bold; text-fill: #555;");

        addressField = new TextField();
        addressField.setPromptText("e.g. 123 Main St, Raleigh");

        downloadLayersCheck = new CheckBox("Download GIS Layers? (Parcels, Topo, Streams)");
        downloadLayersCheck.setSelected(true);

        Button runScriptBtn = createActionBtn("Run Site Import Script", "#0078D7");
        runScriptBtn.setOnAction(e -> runExeScript());

        // Status Check
        File scriptFile = SCRIPT_PATH.toAbsolutePath().toFile();
        Label statusLabel = new Label();
        if (!scriptFile.exists()) {
            statusLabel.setText("⚠️ Exe missing at: " + scriptFile.getAbsolutePath());
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        } else {
            statusLabel.setText("✅ Automation Engine Linked");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }

        siteSection.getChildren().addAll(section1Label, new Separator(), addrLabel, addressField, downloadLayersCheck, runScriptBtn, statusLabel);

        // --- Section 2: Engineering Utilities ---
        VBox utilsSection = new VBox(15);
        utilsSection.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 0, 0, 0, 1); -fx-background-radius: 5;");

        Label section2Label = new Label("2. Drawing Utilities (Coming Soon)");
        section2Label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox buttonBox = new VBox(10); // Changed to VBox for cleaner stacking in the narrower column

        Button epaNetBtn = createActionBtn("Convert EPANET to DWG", "#28a745");
        epaNetBtn.setMaxWidth(Double.MAX_VALUE); // Make button fill width
        epaNetBtn.setOnAction(e -> runPlaceholderTask("Reading .inp file... mapping nodes to blocks... export complete."));

        Button cleanupBtn = createActionBtn("Standard Layer Cleanup", "#6c757d");
        cleanupBtn.setMaxWidth(Double.MAX_VALUE); // Make button fill width
        cleanupBtn.setOnAction(e -> runPlaceholderTask("Purging unused layers... fixing linetypes... auditing drawing... Cleaned."));

        buttonBox.getChildren().addAll(epaNetBtn, cleanupBtn);
        utilsSection.getChildren().addAll(section2Label, new Separator(), buttonBox);

        // --- NEW: Layout Container (Side-by-Side) ---
        HBox topRow = new HBox(20); // 20px gap between the two cards

        // Make them share horizontal space equally
        HBox.setHgrow(siteSection, Priority.ALWAYS);
        HBox.setHgrow(utilsSection, Priority.ALWAYS);

        topRow.getChildren().addAll(siteSection, utilsSection);

        // --- Section 3: Log ---
        VBox logSection = new VBox(10);
        VBox.setVgrow(logSection, Priority.ALWAYS);

        Label logLabel = new Label("Automation Log");
        logLabel.setStyle("-fx-font-weight: bold;");

        consoleLog = new TextArea();
        consoleLog.setEditable(false);
        consoleLog.setStyle("-fx-font-family: 'Consolas', monospace; -fx-control-inner-background: #2b2b2b; -fx-text-fill: #00ff00;");
        consoleLog.setText("System Ready.\n");

        logSection.getChildren().addAll(logLabel, consoleLog);

        // Add the topRow (side-by-side) instead of individual sections
        this.getChildren().addAll(header, topRow, logSection);
    }

    private void ensureScriptExists() {
        try {
            // 1. Create folder if missing
            if (!Files.exists(SCRIPT_DIR)) {
                Files.createDirectories(SCRIPT_DIR);
            }

            // 2. If .exe is missing, extract it from the JAR
            if (!Files.exists(SCRIPT_PATH)) {
                try (InputStream in = getClass().getResourceAsStream("/scripts/address_to_scr.exe")) {
                    if (in != null) {
                        Files.copy(in, SCRIPT_PATH, StandardCopyOption.REPLACE_EXISTING);
                        log("Restored missing automation script.");
                    } else {
                        log("ERROR: Could not find script inside app resources!");
                    }
                }
            }
        } catch (IOException e) {
            log("Error checking script: " + e.getMessage());
        }
    }

    private void runExeScript() {
        String address = addressField.getText().trim();
        if (address.isEmpty()) {
            log("❌ Error: Please enter an address.");
            return;
        }

        String downloadArg = downloadLayersCheck.isSelected() ? "y" : "n";

        log("🚀 Launching Automation Engine...");
        log("   Address: " + address);

        new Thread(() -> {
            try {
                String commandPath = SCRIPT_PATH.toAbsolutePath().toString();

                ProcessBuilder pb = new ProcessBuilder(commandPath, address, downloadArg);
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.redirectErrorStream(true);

                Process process = pb.start();

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line;
                    Platform.runLater(() -> log(outputLine));
                }

                int exitCode = process.waitFor();

                Platform.runLater(() -> {
                    if (exitCode == 0) log("\n✅ Process Complete.");
                    else log("\n❌ Process Failed (Exit Code: " + exitCode + ")");
                });

            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> log("❌ Error: " + e.getMessage()));
            }
        }).start();
    }

    private void runPlaceholderTask(String successMessage) {
        log("⏳ Starting process...");
        new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException e) {}
            Platform.runLater(() -> {
                log("✅ " + successMessage);
                log("--------------------------------------------------");
            });
        }).start();
    }

    private Button createActionBtn(String text, String colorHex) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        return btn;
    }

    private void log(String message) {
        consoleLog.appendText(timestamp() + " " + message + "\n");
    }

    private String timestamp() {
        return "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]";
    }
}