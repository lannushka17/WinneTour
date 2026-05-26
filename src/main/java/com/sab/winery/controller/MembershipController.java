package com.sab.winery.controller;

import com.sab.winery.persistence.contract.ClubMembershipRepository;
import com.sab.winery.persistence.contract.GuestProfileRepository;
import com.sab.winery.persistence.entity.ClubMembership;
import com.sab.winery.persistence.entity.ClubTier;
import com.sab.winery.persistence.entity.GuestProfile;
import com.sab.winery.persistence.implementation.ClubMembershipRepositoryImpl;
import com.sab.winery.persistence.implementation.GuestProfileRepositoryImpl;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/** Управління картками клубу виноробні (лояльність гостей). */
public class MembershipController implements Initializable {

    @FXML
    private TableView<ClubMembership> membershipTable;

    @FXML
    private TableColumn<ClubMembership, Integer> idColumn;

    @FXML
    private TableColumn<ClubMembership, String> guestColumn;

    @FXML
    private TableColumn<ClubMembership, String> tierColumn;

    @FXML
    private TableColumn<ClubMembership, String> validColumn;

    @FXML
    private TableColumn<ClubMembership, Integer> discountColumn;

    @FXML
    private TableColumn<ClubMembership, String> activeColumn;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private ComboBox<GuestProfile> guestCombo;

    @FXML
    private ComboBox<ClubTier> tierCombo;

    @FXML
    private DatePicker validUntilPicker;

    @FXML
    private Spinner<Integer> discountSpinner;

    @FXML
    private CheckBox activeCheck;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    private final ClubMembershipRepository memberships = new ClubMembershipRepositoryImpl();
    private final GuestProfileRepository guests = new GuestProfileRepositoryImpl();

    private final ObservableList<ClubMembership> data = FXCollections.observableArrayList();
    private final Map<Integer, String> guestNames = new HashMap<>();

    private ClubMembership selected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        guestColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                guestNames.getOrDefault(cell.getValue().getGuestId(), "—")));
        tierColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getTier().getDisplayName()));
        validColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getValidUntil().toString()));
        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discountPercent"));
        activeColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isActive() ? "Активна" : "Неактивна"));

        membershipTable.setItems(data);

        statusFilter.getItems().setAll("Всі", "Активні", "Неактивні");
        statusFilter.setValue("Всі");
        statusFilter.valueProperty().addListener((obs, o, n) -> reload());

        tierCombo.getItems().setAll(ClubTier.values());
        tierCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null && discountSpinner.getValueFactory() != null) {
                discountSpinner.getValueFactory().setValue(n.getDefaultDiscountPercent());
            }
        });

        discountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 5, 1));
        validUntilPicker.setValue(LocalDate.now().plusYears(1));
        activeCheck.setSelected(true);

        guestCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GuestProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " · " + item.getEmail());
            }
        });
        guestCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GuestProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " · " + item.getEmail());
            }
        });

        membershipTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selected = n;
            if (n != null) {
                guestCombo.getItems().stream()
                        .filter(g -> g.getId() == n.getGuestId())
                        .findFirst()
                        .ifPresent(g -> guestCombo.getSelectionModel().select(g));
                tierCombo.setValue(n.getTier());
                validUntilPicker.setValue(n.getValidUntil());
                discountSpinner.getValueFactory().setValue(n.getDiscountPercent());
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
        List<GuestProfile> all = guests.findAllOrderByName();
        guestNames.clear();
        for (GuestProfile g : all) {
            guestNames.put(g.getId(), g.getName());
        }
        guestCombo.setItems(FXCollections.observableArrayList(all));
        reload();
    }

    private void reload() {
        List<ClubMembership> list = memberships.findAll();
        String status = statusFilter.getValue() == null ? "Всі" : statusFilter.getValue();
        if ("Активні".equals(status)) {
            list = list.stream().filter(ClubMembership::isActive).toList();
        } else if ("Неактивні".equals(status)) {
            list = list.stream().filter(m -> !m.isActive()).toList();
        }
        data.setAll(list);
    }

    @FXML
    private void handleSave() {
        if (!validate()) {
            return;
        }
        ClubMembership m = ClubMembership.builder()
                .guestId(guestCombo.getValue().getId())
                .tier(tierCombo.getValue())
                .validUntil(validUntilPicker.getValue())
                .discountPercent(discountSpinner.getValue())
                .active(activeCheck.isSelected())
                .build();
        memberships.save(m);
        clearFields();
        reload();
        alert(Alert.AlertType.INFORMATION, "Картку клубу створено.");
    }

    @FXML
    private void handleUpdate() {
        if (selected == null || !validate()) {
            return;
        }
        ClubMembership updated = ClubMembership.builder()
                .id(selected.getId())
                .guestId(guestCombo.getValue().getId())
                .tier(tierCombo.getValue())
                .validUntil(validUntilPicker.getValue())
                .discountPercent(discountSpinner.getValue())
                .active(activeCheck.isSelected())
                .build();
        memberships.save(updated);
        membershipTable.getSelectionModel().clearSelection();
        clearFields();
        reload();
        alert(Alert.AlertType.INFORMATION, "Картку оновлено.");
    }

    @FXML
    private void handleDelete() {
        if (selected == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Видалити картку клубу");
        confirm.setContentText("Видалити картку #" + selected.getId() + "?");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            memberships.deleteById(selected.getId());
            clearFields();
            reload();
            alert(Alert.AlertType.INFORMATION, "Картку видалено.");
        }
    }

    @FXML
    private void handleClear() {
        membershipTable.getSelectionModel().clearSelection();
        clearFields();
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    private boolean validate() {
        if (guestCombo.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Оберіть гостя.");
            return false;
        }
        if (tierCombo.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Оберіть рівень картки.");
            return false;
        }
        if (validUntilPicker.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Вкажіть дату закінчення.");
            return false;
        }
        return true;
    }

    private void clearFields() {
        guestCombo.setValue(null);
        tierCombo.setValue(null);
        validUntilPicker.setValue(LocalDate.now().plusYears(1));
        if (discountSpinner.getValueFactory() != null) {
            discountSpinner.getValueFactory().setValue(5);
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
