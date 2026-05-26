package com.sab.winery.controller;

import com.sab.winery.persistence.entity.TourProgram;
import com.sab.winery.persistence.entity.TourSession;
import com.sab.winery.persistence.entity.Winery;
import com.sab.winery.persistence.implementation.TourProgramRepositoryImpl;
import com.sab.winery.persistence.implementation.TourSessionRepositoryImpl;
import com.sab.winery.persistence.implementation.WineryRepositoryImpl;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

/** Керування програмами турів і сесіями (службовий режим). */
public class TourManagementController implements Initializable {

    @FXML
    private ComboBox<Winery> wineryCombo;

    @FXML
    private TableView<TourProgram> programsTable;

    @FXML
    private TableColumn<TourProgram, Integer> progIdCol;

    @FXML
    private TableColumn<TourProgram, String> progTitleCol;

    @FXML
    private TableColumn<TourProgram, Integer> progDurationCol;

    @FXML
    private TextField progTitleField;

    @FXML
    private TextArea progDescField;

    @FXML
    private Spinner<Integer> durationSpinner;

    @FXML
    private Spinner<Integer> defaultGuestsSpinner;

    @FXML
    private TextField progImageField;

    @FXML
    private ImageView progImagePreview;

    @FXML
    private TableView<TourSession> sessionsTable;

    @FXML
    private TableColumn<TourSession, Integer> sessIdCol;

    @FXML
    private TableColumn<TourSession, String> sessStartCol;

    @FXML
    private TableColumn<TourSession, Integer> sessCapCol;

    @FXML
    private TableColumn<TourSession, String> sessStatusCol;

    @FXML
    private TextField sessionStartField;

    @FXML
    private Spinner<Integer> sessionCapSpinner;

    private final WineryRepositoryImpl wineries = new WineryRepositoryImpl();
    private final TourProgramRepositoryImpl programsRepo = new TourProgramRepositoryImpl();
    private final TourSessionRepositoryImpl sessionsRepo = new TourSessionRepositoryImpl();

    private final ObservableList<TourProgram> programsData = FXCollections.observableArrayList();
    private final ObservableList<TourSession> sessionsData = FXCollections.observableArrayList();

    private TourProgram selectedProgram;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        progIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        progTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        progDurationCol.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        programsTable.setItems(programsData);

        sessIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        sessStartCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        sessCapCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        sessStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        sessionsTable.setItems(sessionsData);

        durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 300, 90, 15));
        defaultGuestsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 16));
        sessionCapSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 16));

        wineryCombo.setItems(FXCollections.observableArrayList(wineries.findAll()));

        programsTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, o, n) -> {
                    selectedProgram = n;
                    if (n != null) {
                        fillProgramForm(n);
                        reloadSessions();
                    } else {
                        clearProgramForm();
                        sessionsData.clear();
                    }
                });

        sessionsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                sessionStartField.setText(n.getStartTime());
                sessionCapSpinner.getValueFactory().setValue(n.getCapacity());
            }
        });

        refreshData();
    }

    public void refreshData() {
        wineryCombo.setItems(FXCollections.observableArrayList(wineries.findAll()));
        if (!wineryCombo.getItems().isEmpty()) {
            wineryCombo.getSelectionModel().selectFirst();
        }
        programsData.setAll(programsRepo.findAll());
        programsTable.getSelectionModel().clearSelection();
        sessionsData.clear();
    }

    private void reloadSessions() {
        sessionsData.clear();
        if (selectedProgram == null) {
            return;
        }
        sessionsData.setAll(sessionsRepo.findByProgramId(selectedProgram.getId()));
    }

    private void fillProgramForm(TourProgram p) {
        progTitleField.setText(p.getTitle());
        progDescField.setText(p.getDescription());
        durationSpinner.getValueFactory().setValue(p.getDurationMinutes());
        defaultGuestsSpinner.getValueFactory().setValue(p.getDefaultMaxGuests());
        progImageField.setText(p.getImagePath() != null ? p.getImagePath() : "");
        loadPreview(progImageField.getText());
        wineryCombo.getItems().stream()
                .filter(w -> w.getId() == p.getWineryId())
                .findFirst()
                .ifPresent(w -> wineryCombo.getSelectionModel().select(w));
    }

    private void clearProgramForm() {
        progTitleField.clear();
        progDescField.clear();
        progImageField.clear();
        progImagePreview.setImage(null);
    }

    @FXML
    private void browseProgramImage() {
        FileChooser ch = new FileChooser();
        ch.setTitle("Оберіть зображення туру");
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("Зображення", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File f = ch.showOpenDialog(progTitleField.getScene().getWindow());
        if (f != null) {
            progImageField.setText(f.getAbsolutePath());
            loadPreview(f.getAbsolutePath());
        }
    }

    private void loadPreview(String path) {
        if (path == null || path.isBlank()) {
            progImagePreview.setImage(null);
            return;
        }
        try {
            File f = new File(path);
            if (f.exists()) {
                progImagePreview.setImage(new Image(f.toURI().toString(), 120, 120, true, true));
            } else {
                progImagePreview.setImage(null);
            }
        } catch (Exception e) {
            progImagePreview.setImage(null);
        }
    }

    @FXML
    private void saveProgram() {
        Winery w = wineryCombo.getSelectionModel().getSelectedItem();
        if (w == null) {
            alert(Alert.AlertType.WARNING, "Оберіть виноробню.");
            return;
        }
        if (progTitleField.getText().trim().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Назва програми обов'язкова.");
            return;
        }
        TourProgram p =
                TourProgram.builder()
                        .id(selectedProgram != null ? selectedProgram.getId() : 0)
                        .wineryId(w.getId())
                        .title(progTitleField.getText().trim())
                        .description(progDescField.getText())
                        .durationMinutes(durationSpinner.getValue())
                        .defaultMaxGuests(defaultGuestsSpinner.getValue())
                        .imagePath(blankToNull(progImageField.getText()))
                        .active(true)
                        .build();
        programsRepo.save(p);
        alert(Alert.AlertType.INFORMATION, "Програму збережено.");
        refreshData();
    }

    @FXML
    private void archiveProgram() {
        if (selectedProgram == null) {
            return;
        }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setContentText("Архівувати програму (деактивувати)?");
        Optional<javafx.scene.control.ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == javafx.scene.control.ButtonType.OK) {
            programsRepo.setActive(selectedProgram.getId(), false);
            refreshData();
        }
    }

    @FXML
    private void clearProgramFormAction() {
        programsTable.getSelectionModel().clearSelection();
        clearProgramForm();
        selectedProgram = null;
    }

    @FXML
    private void saveSession() {
        if (selectedProgram == null) {
            alert(Alert.AlertType.WARNING, "Спочатку оберіть програму туру.");
            return;
        }
        String start = sessionStartField.getText().trim();
        if (start.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Вкажіть дату й час у форматі ISO, наприклад 2026-07-01T18:00:00");
            return;
        }
        TourSession sel = sessionsTable.getSelectionModel().getSelectedItem();
        TourSession s =
                TourSession.builder()
                        .id(sel != null ? sel.getId() : 0)
                        .programId(selectedProgram.getId())
                        .startTime(start)
                        .capacity(sessionCapSpinner.getValue())
                        .status("SCHEDULED")
                        .build();
        sessionsRepo.save(s);
        alert(Alert.AlertType.INFORMATION, "Сесію збережено.");
        reloadSessions();
    }

    @FXML
    private void deleteSession() {
        TourSession sel = sessionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        alert(Alert.AlertType.INFORMATION, "Для спрощення видалення сесій вимкнено; змініть статус у БД або скасуйте бронювання.");
    }

    private static String blankToNull(String s) {
        String t = s == null ? "" : s.trim();
        return t.isEmpty() ? null : t;
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
