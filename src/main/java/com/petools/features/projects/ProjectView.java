package com.petools.features.projects;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

public class ProjectView extends VBox {

    public ProjectView() {
        // 1. Setup VBox Layout (matches your old code)
        this.setSpacing(10);
        this.setPadding(new Insets(20)); // topRightBottomLeft: 20
        this.setStyle("-fx-background-color: #ffffff;");

        // 2. Create Header
        Label projectHeader = new Label("Engineering Projects Management");
        projectHeader.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        // 3. Create Data (We simulate the list here now)
        ObservableList<Project> projectList = FXCollections.observableArrayList();
        // You can add dummy data here to test:
        // projectList.add(new Project("Sample Project A"));

        // 4. Create ListView
        ListView<Project> projectListView = new ListView<>(projectList);

        // 5. The Cell Factory (The logic from your screenshot)
        projectListView.setCellFactory(param -> new ListCell<Project>() {
            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Assuming getName() is the method in your Project class
                    setText(item.getName());
                }
            }
        });

        // 6. Add to View
        this.getChildren().addAll(projectHeader, projectListView);
    }
}