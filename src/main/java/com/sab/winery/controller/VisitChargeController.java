package com.sab.winery.controller;

import com.sab.winery.persistence.contract.AddonServiceRepository;
import com.sab.winery.persistence.contract.ReservationRepository;
import com.sab.winery.persistence.contract.VisitChargeRepository;
import com.sab.winery.persistence.dto.ReservationListRow;
import com.sab.winery.persistence.entity.AddonService;
import com.sab.winery.persistence.entity.VisitCharge;
import com.sab.winery.persistence.implementation.AddonServiceRepositoryImpl;
import com.sab.winery.persistence.implementation.ReservationRepositoryImpl;
import com.sab.winery.persistence.implementation.VisitChargeRepositoryImpl;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/** Управління нарахуваннями за додаткові послуги в межах бронювань. */
public class VisitChargeController implements Initializable {

    private static final DateTimeFormatter UI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    private TableView<VisitCharge> chargeTable;

    @FXML
    private TableColumn<VisitCharge, Integer> idColumn;

    @FXML
    private TableColumn<VisitCharge, String> reservationColumn;

    @FXML
    private TableColumn<VisitCharge, String> serviceColumn;

    @FXML
    private TableColumn<VisitCharge, Integer> qtyColumn;

    @FXML
    private TableColumn<VisitCharge, String> priceColumn;

    @FXML
    private TableColumn<VisitCharge, String> totalColumn;

    @FXML
    private TableColumn<VisitCharge, String> dateColumn;

    @FXML
    private TableColumn<VisitCharge, String> paidColumn;

    @FXML
    private ComboBox<ReservationOption> reservationFilter;

    @FXML
    private ComboBox<String> paidFilter;

    @FXML
    private ComboBox<ReservationOption> reservationCombo;

    @FXML
    private ComboBox<AddonService> serviceCombo;

    @FXML
    private Spinner<Integer> quantitySpinner;

    @FXML
    private CheckBox paidCheck;

    @FXML
    private Label totalLabel;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    private final VisitChargeRepository charges = new VisitChargeRepositoryImpl();
    private final AddonServiceRepository services = new AddonServiceRepositoryImpl();
    private final ReservationRepository reservations = new ReservationRepositoryImpl();

    private final ObservableList<VisitCharge> data = FXCollections.observableArrayList();
    private final Map<Integer, AddonService> servicesById = new HashMap<>();
    private final Map<Integer, String> reservationsLabelById = new HashMap<>();

    private VisitCharge selected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        reservationColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                reservationsLabelById.getOrDefault(cell.getValue().getReservationId(),
                        "#" + cell.getValue().getReservationId())));
        serviceColumn.setCellValueFactory(cell -> {
            AddonService s = servicesById.get(cell.getValue().getAddonServiceId());
            return new SimpleStringProperty(s == null ? "—" : s.getName());
        });
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceColumn.setCellValueFactory(cell -> {
            AddonService s = servicesById.get(cell.getValue().getAddonServiceId());
            return new SimpleStringProperty(s == null ? "—" : String.format("%.2f грн", s.getPricePerUnit()));
        });
        totalColumn.setCellValueFactory(cell -> {
            AddonService s = servicesById.get(cell.getValue().getAddonServiceId());
            double total = s == null ? 0 : cell.getValue().getTotalAmount(s.getPricePerUnit());
            return new SimpleStringProperty(String.format("%.2f грн", total));
        });
        dateColumn.setCellValueFactory(cell -> {
            LocalDateTime dt = cell.getValue().getUsageDateTime();
            return new SimpleStringProperty(dt == null ? "—" : dt.format(UI_DATE_FORMAT));
        });
        paidColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isPaid() ? "Так" : "Ні"));
        chargeTable.setItems(data);

        paidFilter.getItems().setAll("Всі", "Оплачені", "Неоплачені");
        paidFilter.setValue("Всі");
        paidFilter.valueProperty().addListener((obs, o, n) -> reload());

        reservationFilter.valueProperty().addListener((obs, o, n) -> reload());

        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        paidCheck.setSelected(false);

        ListCell<AddonService> svcCell = new ListCell<>() {
            @Override
            protected void updateItem(AddonService item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : String.format("%s — %.2f грн", item.getName(), item.getPricePerUnit()));
            }
        };
        serviceCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AddonService item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : String.format("%s — %.2f грн", item.getName(), item.getPricePerUnit()));
            }
        });
        serviceCombo.setButtonCell(svcCell);

        serviceCombo.valueProperty().addListener((obs, o, n) -> recalcTotal());
        quantitySpinner.valueProperty().addListener((obs, o, n) -> recalcTotal());

        chargeTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selected = n;
            if (n != null) {
                reservationCombo.getItems().stream()
                        .filter(r -> r.id() == n.getReservationId())
                        .findFirst()
                        .ifPresent(r -> reservationCombo.getSelectionModel().select(r));
                AddonService svc = servicesById.get(n.getAddonServiceId());
                if (svc != null) {
                    serviceCombo.setValue(svc);
                }
                quantitySpinner.getValueFactory().setValue(n.getQuantity());
                paidCheck.setSelected(n.isPaid());
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
        List<AddonService> all = services.findAll();
        servicesById.clear();
        for (AddonService s : all) {
            servicesById.put(s.getId(), s);
        }
        serviceCombo.setItems(FXCollections.observableArrayList(services.findAllActive()));

        List<ReservationListRow> rows = reservations.findPage("", "Всі", "time", false, 0, 500);
        reservationsLabelById.clear();
        ObservableList<ReservationOption> options = FXCollections.observableArrayList();
        for (ReservationListRow r : rows) {
            String label = String.format("#%d · %s · %s · %s", r.getReservationId(), r.getGuestName(),
                    r.getProgramTitle(), r.getSessionStart());
            reservationsLabelById.put(r.getReservationId(), label);
            options.add(new ReservationOption(r.getReservationId(), label));
        }
        reservationCombo.setItems(options);

        ObservableList<ReservationOption> filterOptions = FXCollections.observableArrayList();
        filterOptions.add(new ReservationOption(0, "Усі бронювання"));
        filterOptions.addAll(options);
        reservationFilter.setItems(filterOptions);
        if (reservationFilter.getValue() == null) {
            reservationFilter.getSelectionModel().selectFirst();
        }

        reload();
    }

    private void reload() {
        List<VisitCharge> list = charges.findAll();
        ReservationOption resFilter = reservationFilter.getValue();
        if (resFilter != null && resFilter.id() > 0) {
            list = list.stream().filter(c -> c.getReservationId() == resFilter.id()).toList();
        }
        String paidMode = paidFilter.getValue() == null ? "Всі" : paidFilter.getValue();
        if ("Оплачені".equals(paidMode)) {
            list = list.stream().filter(VisitCharge::isPaid).toList();
        } else if ("Неоплачені".equals(paidMode)) {
            list = list.stream().filter(c -> !c.isPaid()).toList();
        }
        data.setAll(list);
    }

    private void recalcTotal() {
        AddonService s = serviceCombo.getValue();
        int qty = quantitySpinner.getValue() == null ? 0 : quantitySpinner.getValue();
        double total = (s == null ? 0 : s.getPricePerUnit()) * qty;
        totalLabel.setText(String.format("%.2f грн", total));
    }

    @FXML
    private void handleSave() {
        if (!validate()) {
            return;
        }
        VisitCharge c = VisitCharge.builder()
                .reservationId(reservationCombo.getValue().id())
                .addonServiceId(serviceCombo.getValue().getId())
                .quantity(quantitySpinner.getValue())
                .usageDateTime(LocalDateTime.now())
                .paid(paidCheck.isSelected())
                .build();
        charges.save(c);
        clearFields();
        reload();
        alert(Alert.AlertType.INFORMATION, "Нарахування додано.");
    }

    @FXML
    private void handleUpdate() {
        if (selected == null || !validate()) {
            return;
        }
        VisitCharge updated = VisitCharge.builder()
                .id(selected.getId())
                .reservationId(reservationCombo.getValue().id())
                .addonServiceId(serviceCombo.getValue().getId())
                .quantity(quantitySpinner.getValue())
                .usageDateTime(selected.getUsageDateTime() == null ? LocalDateTime.now() : selected.getUsageDateTime())
                .paid(paidCheck.isSelected())
                .build();
        charges.save(updated);
        chargeTable.getSelectionModel().clearSelection();
        clearFields();
        reload();
        alert(Alert.AlertType.INFORMATION, "Нарахування оновлено.");
    }

    @FXML
    private void handleDelete() {
        if (selected == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Видалити нарахування");
        confirm.setContentText("Видалити нарахування #" + selected.getId() + "?");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            charges.deleteById(selected.getId());
            clearFields();
            reload();
            alert(Alert.AlertType.INFORMATION, "Видалено.");
        }
    }

    @FXML
    private void handleClear() {
        chargeTable.getSelectionModel().clearSelection();
        clearFields();
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    private boolean validate() {
        if (reservationCombo.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Оберіть бронювання.");
            return false;
        }
        if (serviceCombo.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Оберіть послугу.");
            return false;
        }
        if (quantitySpinner.getValue() == null || quantitySpinner.getValue() <= 0) {
            alert(Alert.AlertType.WARNING, "Кількість повинна бути більше нуля.");
            return false;
        }
        return true;
    }

    private void clearFields() {
        reservationCombo.setValue(null);
        serviceCombo.setValue(null);
        if (quantitySpinner.getValueFactory() != null) {
            quantitySpinner.getValueFactory().setValue(1);
        }
        paidCheck.setSelected(false);
        totalLabel.setText("0.00 грн");
        selected = null;
    }

    private void alert(Alert.AlertType type, String message) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    /** Опція бронювання для ComboBox. */
    public record ReservationOption(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
