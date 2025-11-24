package com.petools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;

public class DateEditingCell extends TableCell<TodoItem, String> {
    private DatePicker datePicker;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yy");

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            createDatePicker();
            setText(null);
            setGraphic(datePicker);
            datePicker.requestFocus();
        }
    }
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }
    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) { setText(null); setGraphic(null); }
        else {
            if (isEditing()) { if (datePicker != null) datePicker.setValue(getDate()); setText(null); setGraphic(datePicker); } 
            else { setText(getItem()); setGraphic(null); }
        }
    }
    private void createDatePicker() {
        datePicker = new DatePicker(getDate());
        datePicker.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
        datePicker.setOnAction((e) -> {
            if(datePicker.getValue() != null) commitEdit(datePicker.getValue().format(formatter));
            else commitEdit(null);
        });
    }
    private LocalDate getDate() {
        try { return getItem() == null ? LocalDate.now() : LocalDate.parse(getItem(), formatter); }
        catch (DateTimeParseException e) { return LocalDate.now(); }
    }
}
