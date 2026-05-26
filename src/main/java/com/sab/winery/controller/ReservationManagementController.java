package com.sab.winery.controller;

import com.sab.winery.persistence.dto.ReservationListRow;
import com.sab.winery.persistence.dto.SessionPickOption;
import com.sab.winery.persistence.entity.GuestProfile;
import com.sab.winery.persistence.entity.TourProgram;
import com.sab.winery.persistence.entity.TourSession;
import com.sab.winery.persistence.implementation.GuestProfileRepositoryImpl;
import com.sab.winery.persistence.implementation.ReservationRepositoryImpl;
import com.sab.winery.persistence.implementation.TourProgramRepositoryImpl;
import com.sab.winery.persistence.implementation.TourSessionRepositoryImpl;
import com.sab.winery.service.BookingService;
import com.sab.winery.service.VisitService;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

/**
 * Бронювання гостей на сесії туру — аналог «Договори оренди» у практиці AdAgency (зв'язок гість ↔ сесія).
 */
public class ReservationManagementController implements Initializable {

    @FXML
    private ComboBox<GuestProfile> guestCombo;

    @FXML
    private ComboBox<SessionPickOption> sessionCombo;

    @FXML
    private Spinner<Integer> partySpinner;

    @FXML
    private Label hintLabel;

    @FXML
    private TableView<ReservationListRow> table;

    @FXML
    private TableColumn<ReservationListRow, Integer> colId;

    @FXML
    private TableColumn<ReservationListRow, String> colGuest;

    @FXML
    private TableColumn<ReservationListRow, String> colEmail;

    @FXML
    private TableColumn<ReservationListRow, String> colTour;

    @FXML
    private TableColumn<ReservationListRow, String> colStart;

    @FXML
    private TableColumn<ReservationListRow, Integer> colParty;

    @FXML
    private TableColumn<ReservationListRow, String> colStatus;

    @FXML
    private TableColumn<ReservationListRow, String> colVisit;

    private final GuestProfileRepositoryImpl guests = new GuestProfileRepositoryImpl();
    private final TourProgramRepositoryImpl programs = new TourProgramRepositoryImpl();
    private final TourSessionRepositoryImpl sessionsRepo = new TourSessionRepositoryImpl();
    private final ReservationRepositoryImpl reservations = new ReservationRepositoryImpl();
    private final BookingService bookingService = new BookingService();
    private final VisitService visitService = new VisitService();

    private final ObservableList<ReservationListRow> rows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("guestEmail"));
        colTour.setCellValueFactory(new PropertyValueFactory<>("programTitle"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("sessionStart"));
        colParty.setCellValueFactory(new PropertyValueFactory<>("partySize"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("reservationStatus"));
        colVisit.setCellValueFactory(new PropertyValueFactory<>("visitInfo"));
        table.setItems(rows);

        partySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 2));

        sessionCombo.valueProperty().addListener((obs, o, n) -> updateSpinnerMax(n));

        guestCombo.setCellFactory(
                lv ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(GuestProfile item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                    setText(null);
                                } else {
                                    setText(item.getName() + "  ·  " + item.getEmail());
                                }
                            }
                        });
        guestCombo.setButtonCell(
                new ListCell<>() {
                    @Override
                    protected void updateItem(GuestProfile item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName() + "  ·  " + item.getEmail());
                        }
                    }
                });

        refreshReferenceData();
        reloadTable();
    }

    public void refreshData() {
        refreshReferenceData();
        reloadTable();
    }

    private void refreshReferenceData() {
        guestCombo.setItems(FXCollections.observableArrayList(guests.findAllOrderByName()));
        reloadSessionOptions();
        if (!guestCombo.getItems().isEmpty()) {
            guestCombo.getSelectionModel().selectFirst();
        }
    }

    private void reloadSessionOptions() {
        SessionPickOption sel = sessionCombo.getSelectionModel().getSelectedItem();
        sessionCombo.getItems().setAll(buildSessionOptions());
        if (!sessionCombo.getItems().isEmpty()) {
            if (sel != null) {
                sessionCombo
                        .getItems()
                        .stream()
                        .filter(o -> o.getSessionId() == sel.getSessionId())
                        .findFirst()
                        .ifPresentOrElse(sessionCombo.getSelectionModel()::select, () -> sessionCombo.getSelectionModel().selectFirst());
            } else {
                sessionCombo.getSelectionModel().selectFirst();
            }
        }
        updateSpinnerMax(sessionCombo.getSelectionModel().getSelectedItem());
    }

    private ObservableList<SessionPickOption> buildSessionOptions() {
        ObservableList<SessionPickOption> list = FXCollections.observableArrayList();
        for (TourProgram p : programs.findAllActive()) {
            for (TourSession ts : sessionsRepo.findByProgramId(p.getId())) {
                if (!"SCHEDULED".equalsIgnoreCase(ts.getStatus())) {
                    continue;
                }
                int free = bookingService.freePlaces(ts.getId());
                list.add(
                        SessionPickOption.builder()
                                .sessionId(ts.getId())
                                .programTitle(p.getTitle())
                                .startTime(ts.getStartTime())
                                .capacity(ts.getCapacity())
                                .freePlaces(free)
                                .build());
            }
        }
        return list;
    }

    private void updateSpinnerMax(SessionPickOption opt) {
        if (opt == null) {
            hintLabel.setText("Немає доступних запланованих сесій — додайте їх у розділі «Тури й сесії».");
            partySpinner.setDisable(true);
            return;
        }
        partySpinner.setDisable(false);
        int maxParty = Math.max(1, opt.getFreePlaces());
        int cur = partySpinner.getValue();
        partySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxParty, Math.min(cur, maxParty)));
        hintLabel.setText("Максимум місць для нового бронювання: " + maxParty + " (місткість " + opt.getCapacity() + ").");
    }

    private void reloadTable() {
        rows.setAll(reservations.findPage("", "Всі", "time", true, 0, 400));
    }

    @FXML
    private void handleBook() {
        GuestProfile g = guestCombo.getSelectionModel().getSelectedItem();
        SessionPickOption opt = sessionCombo.getSelectionModel().getSelectedItem();
        if (g == null) {
            alert(Alert.AlertType.WARNING, "Додайте хоча б одного гостя у відповідному розділі.");
            return;
        }
        if (opt == null) {
            alert(Alert.AlertType.WARNING, "Немає сесії для бронювання.");
            return;
        }
        int party = partySpinner.getValue();
        try {
            bookingService.bookForGuest(opt.getSessionId(), g.getId(), party);
            alert(Alert.AlertType.INFORMATION, "Бронювання створено.");
            reloadSessionOptions();
            reloadTable();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        ReservationListRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            alert(Alert.AlertType.WARNING, "Оберіть бронювання в таблиці.");
            return;
        }
        if ("CANCELLED".equalsIgnoreCase(row.getReservationStatus())) {
            return;
        }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setContentText("Скасувати бронювання #" + row.getReservationId() + "?");
        Optional<ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                visitService.cancelReservation(row.getReservationId());
                reloadSessionOptions();
                reloadTable();
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, ex.getMessage());
            }
        }
    }

    @FXML
    private void handleVisit() {
        ReservationListRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            alert(Alert.AlertType.WARNING, "Оберіть бронювання.");
            return;
        }
        int maxParty = row.getPartySize();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Фіксація візиту");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        CheckBox noShow = new CheckBox("No-show (ніхто не прийшов)");
        Spinner<Integer> attended =
                new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxParty, maxParty));
        TextArea notes = new TextArea();
        notes.setPromptText("Нотатки (смаки, побажання)…");
        notes.setPrefRowCount(3);

        grid.addRow(0, noShow);
        grid.addRow(1, new Label("Присутніх:"), attended);
        grid.addRow(2, new Label("Нотатки:"), notes);

        noShow.selectedProperty().addListener((obs, o, n) -> attended.setDisable(Boolean.TRUE.equals(n)));

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                visitService.recordAttendance(
                        row.getReservationId(), attended.getValue(), notes.getText(), noShow.isSelected());
                reloadTable();
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, ex.getMessage());
            }
        }
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
