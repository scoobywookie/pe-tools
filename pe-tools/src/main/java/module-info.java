module com.petools {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    opens com.petools to javafx.fxml;
    exports com.petools;
}
