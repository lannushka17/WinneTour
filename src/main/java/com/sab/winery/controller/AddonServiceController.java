package com.sab.winery.controller;

import com.sab.winery.persistence.contract.AddonServiceRepository;
import com.sab.winery.persistence.entity.AddonService;
import com.sab.winery.persistence.entity.AddonServiceKind;
import com.sab.winery.persistence.implementation.AddonServiceRepositoryImpl;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

/** Каталог додаткових послуг — CRUD довідника. */
public class AddonServiceController implements Initializable {

    @FXML
    private TableView<AddonService> serviceTable;

    @FXML
    private TableColumn<AddonService, Integer> idColumn;

    @FXML
    private TableColumn<AddonService, String> nameColumn;

    @FXML
    private TableColumn<AddonService, String> kindColumn;

    @FXML
    private TableColumn<AddonService, Double> priceColumn;

    @FXML
    private TableColumn<AddonService, String> activeColumn;

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<AddonServiceKind> kindCombo;

    @FXML
    private Spinner<Double> priceSpinner;

    @FXML
    private CheckBox activeCheck;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    private final AddonServiceRepository repo = new AddonServiceRepositoryImpl();
    private final ObservableList<AddonService> data = FXCollections.observableArrayList();

    private AddonService selected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        kindColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getKind().getDisplayName()));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("pricePerUnit"));
        activeColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isActive() ? "Активна" : "Неактивна"));

        serviceTable.setItems(data);

        kindCombo.getItems().setAll(AddonServiceKind.values());
        priceSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100_000, 250, 10));
        activeCheck.setSelected(true);

        serviceTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selected = n;
            if (n != null) {
                nameField.setText(n.getName());
                kindCombo.setValue(n.getKind());
                priceSpinner.getValueFactory().setValue(n.getPricePerUnit());
                activeCheck.setSelected(n.isActive());
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
            } else {
                clearFields();
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
            }
        });

        updateButton.setDisable(true);
        deleteButton.setDisable(true);

        refreshData();
    }

    public void refreshData() {
        data.setAll(repo.findAll());
    }

    @FXML
    private void handleSave() {
        if (!validate()) {
            return;
        }
        AddonService s = AddonService.builder()
                .name(nameField.getText().trim())
                .kind(kindCombo.getValue())
                .pricePerUnit(priceSpinner.getValue())
                .active(activeCheck.isSelected())
                .build();
        repo.save(s);
        clearFields();
        refreshData();
        alert(Alert.AlertType.INFORMATION, "Послугу збережено.");
    }

    @FXML
    private void handleUpdate() {
        if (selected == null || !validate()) {
            return;
        }
        AddonService updated = AddonService.builder()
                .id(selected.getId())
                .name(nameField.getText().trim())
                .kind(kindCombo.getValue())
                .pricePerUnit(priceSpinner.getValue())
                .active(activeCheck.isSelected())
                .build();
        repo.save(updated);
        serviceTable.getSelectionModel().clearSelection();
        clearFields();
        refreshData();
        alert(Alert.AlertType.INFORMATION, "Послугу оновлено.");
    }

    @FXML
    private void handleDelete() {
        if (selected == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Видалити послугу");
        confirm.setContentText("Видалити '" + selected.getName() + "'?");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                repo.deleteById(selected.getId());
                clearFields();
                refreshData();
                alert(Alert.AlertType.INFORMATION, "Видалено.");
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR,
                        "Не вдалось видалити (можливо є нарахування за цією послугою): " + ex.getMessage());
            }
        }
    }

    @FXML
    private void handleClear() {
        serviceTable.getSelectionModel().clearSelection();
        clearFields();
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    private boolean validate() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Назва послуги обов'язкова.");
            return false;
        }
        if (kindCombo.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Оберіть категорію.");
            return false;
        }
        return true;
    }

    private void clearFields() {
        nameField.clear();
        kindCombo.setValue(null);
        if (priceSpinner.getValueFactory() != null) {
            priceSpinner.getValueFactory().setValue(250.0);
        }
        activeCheck.setSelected(true);
        selected = null;
    }

    private void alert(Alert.AlertType type, String message) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
