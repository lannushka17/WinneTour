package com.sab.winery.controller;

import com.sab.winery.persistence.entity.GuestProfile;
import com.sab.winery.persistence.implementation.GuestProfileRepositoryImpl;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

/** Облік відвідувачів (аналог модуля «Клієнти» у практиці AdAgency). */
public class GuestManagementController implements Initializable {

    @FXML
    private TableView<GuestProfile> table;

    @FXML
    private TableColumn<GuestProfile, Integer> idCol;

    @FXML
    private TableColumn<GuestProfile, String> nameCol;

    @FXML
    private TableColumn<GuestProfile, String> phoneCol;

    @FXML
    private TableColumn<GuestProfile, String> emailCol;

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;

    private final GuestProfileRepositoryImpl guests = new GuestProfileRepositoryImpl();
    private final ObservableList<GuestProfile> rows = FXCollections.observableArrayList();
    private GuestProfile selected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        table.setItems(rows);

        table.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, o, n) -> {
                            selected = n;
                            if (n != null) {
                                nameField.setText(n.getName());
                                phoneField.setText(n.getPhone());
                                emailField.setText(n.getEmail());
                            } else {
                                clearFields();
                            }
                        });

        refreshData();
    }

    public void refreshData() {
        rows.setAll(guests.findAllOrderByName());
    }

    @FXML
    private void handleSave() {
        if (!validate()) {
            return;
        }
        GuestProfile g =
                GuestProfile.builder()
                        .id(0)
                        .name(nameField.getText().trim())
                        .phone(phoneField.getText().trim())
                        .email(emailField.getText().trim())
                        .build();
        guests.save(g);
        alert(Alert.AlertType.INFORMATION, "Гостя збережено.");
        refreshData();
        clearFields();
        table.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleUpdate() {
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Оберіть гостя в таблиці.");
            return;
        }
        if (!validate()) {
            return;
        }
        GuestProfile g =
                GuestProfile.builder()
                        .id(selected.getId())
                        .name(nameField.getText().trim())
                        .phone(phoneField.getText().trim())
                        .email(emailField.getText().trim())
                        .build();
        guests.save(g);
        alert(Alert.AlertType.INFORMATION, "Дані оновлено.");
        refreshData();
        table.getSelectionModel().clearSelection();
        selected = null;
        clearFields();
    }

    @FXML
    private void handleDelete() {
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Оберіть гостя для видалення.");
            return;
        }
        int n = guests.countActiveReservationsForGuest(selected.getId());
        if (n > 0) {
            alert(
                    Alert.AlertType.WARNING,
                    "Не можна видалити гостя з активними бронюваннями (" + n + "). Спочатку скасуйте їх.");
            return;
        }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setContentText("Видалити гостя #" + selected.getId() + "?");
        Optional<ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            guests.deleteById(selected.getId());
            alert(Alert.AlertType.INFORMATION, "Видалено.");
            refreshData();
            table.getSelectionModel().clearSelection();
            selected = null;
            clearFields();
        }
    }

    @FXML
    private void handleClear() {
        table.getSelectionModel().clearSelection();
        selected = null;
        clearFields();
    }

    private boolean validate() {
        if (nameField.getText().trim().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Вкажіть ім'я або назву групи.");
            return false;
        }
        String em = emailField.getText().trim();
        if (em.isEmpty() || !em.contains("@")) {
            alert(Alert.AlertType.WARNING, "Вкажіть коректний e-mail.");
            return false;
        }
        if (phoneField.getText().trim().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Вкажіть телефон.");
            return false;
        }
        return true;
    }

    private void clearFields() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
