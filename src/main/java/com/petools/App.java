package com.petools;

import java.io.InputStream;

import com.petools.layout.MainLayout;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        // The MainLayout now handles Sidebar and View switching
        MainLayout root = new MainLayout();

        Scene scene = new Scene(root, 1100, 675);
        stage.setTitle("PE Tools");
        InputStream iconStream = App.class.getResourceAsStream("/com/petools/images/pe_logo.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}