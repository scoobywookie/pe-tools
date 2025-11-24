package com.petools;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public class TodoItem {
    private final SimpleBooleanProperty completed;
    private final SimpleStringProperty projectName;
    private final SimpleStringProperty description;
    private final SimpleStringProperty priority;
    private final SimpleStringProperty status;
    private final SimpleStringProperty date;

    public TodoItem(boolean completed, String projectName, String description, String priority, String status, String date) {
        this.completed = new SimpleBooleanProperty(completed);
        this.projectName = new SimpleStringProperty(projectName);
        this.description = new SimpleStringProperty(description);
        this.priority = new SimpleStringProperty(priority);
        this.status = new SimpleStringProperty(status);
        this.date = new SimpleStringProperty(date);
    }
    public SimpleBooleanProperty completedProperty() { return completed; }
    public boolean getCompleted() { return completed.get(); }
    public void setCompleted(boolean c) { this.completed.set(c); }
    public SimpleStringProperty projectNameProperty() { return projectName; }
    public String getProjectName() { return projectName.get(); }
    public void setProjectName(String s) { this.projectName.set(s); }
    public SimpleStringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String d) { this.description.set(d); }
    public SimpleStringProperty priorityProperty() { return priority; }
    public String getPriority() { return priority.get(); }
    public void setPriority(String p) { this.priority.set(p); }
    public SimpleStringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String s) { this.status.set(s); }
    public SimpleStringProperty dateProperty() { return date; }
    public String getDate() { return date.get(); }
    public void setDate(String d) { this.date.set(d); }
}