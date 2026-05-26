package com.sab.winery.controller;

import com.sab.winery.persistence.contract.AddonServiceRepository;
import com.sab.winery.persistence.contract.ClubMembershipRepository;
import com.sab.winery.persistence.contract.GuestProfileRepository;
import com.sab.winery.persistence.contract.ReservationRepository;
import com.sab.winery.persistence.contract.VisitChargeRepository;
import com.sab.winery.persistence.dto.ReservationListRow;
import com.sab.winery.persistence.entity.AddonService;
import com.sab.winery.persistence.entity.ClubMembership;
import com.sab.winery.persistence.entity.GuestProfile;
import com.sab.winery.persistence.entity.VisitCharge;
import com.sab.winery.persistence.implementation.AddonServiceRepositoryImpl;
import com.sab.winery.persistence.implementation.ClubMembershipRepositoryImpl;
import com.sab.winery.persistence.implementation.GuestProfileRepositoryImpl;
import com.sab.winery.persistence.implementation.ReservationRepositoryImpl;
import com.sab.winery.persistence.implementation.VisitChargeRepositoryImpl;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Контролер модуля звітності з фільтром за періодом та експортом у CSV. */
public class ReportController implements Initializable {

    private static final String REPORT_TOUR_LOAD = "Завантаженість сесій турів";
    private static final String REPORT_ADDONS_FINANCE = "Дохід і борг по послугах";
    private static final String REPORT_CLUB_RISK = "Стан карток клубу і ризики";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    private ComboBox<String> reportTypeCombo;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Button exportButton;

    @FXML
    private TableView<ObservableList<String>> reportTable;

    @FXML
    private Label summaryLabel;

    private final ReservationRepository reservations = new ReservationRepositoryImpl();
    private final VisitChargeRepository charges = new VisitChargeRepositoryImpl();
    private final AddonServiceRepository services = new AddonServiceRepositoryImpl();
    private final ClubMembershipRepository memberships = new ClubMembershipRepositoryImpl();
    private final GuestProfileRepository guests = new GuestProfileRepositoryImpl();

    private final List<String> currentHeaders = new ArrayList<>();
    private final ObservableList<ObservableList<String>> currentRows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reportTypeCombo.getItems().setAll(REPORT_TOUR_LOAD, REPORT_ADDONS_FINANCE, REPORT_CLUB_RISK);
        reportTypeCombo.setValue(REPORT_TOUR_LOAD);

        LocalDate today = LocalDate.now();
        endDatePicker.setValue(today.plusMonths(1));
        startDatePicker.setValue(today.minusMonths(1));

        reportTable.setItems(currentRows);
        exportButton.setDisable(true);
    }

    public void refreshData() {
        handleGenerate();
    }

    @FXML
    private void handleGenerate() {
        if (!validateRange()) {
            return;
        }
        LocalDate from = startDatePicker.getValue();
        LocalDate to = endDatePicker.getValue();
        String type = reportTypeCombo.getValue();
        switch (type) {
            case REPORT_TOUR_LOAD -> tourLoadReport(from, to);
            case REPORT_ADDONS_FINANCE -> addonsFinanceReport(from, to);
            case REPORT_CLUB_RISK -> clubRiskReport(from, to);
            default -> {
            }
        }
        exportButton.setDisable(currentRows.isEmpty());
    }

    @FXML
    private void handleExportCsv() {
        if (currentRows.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Спочатку сформуйте звіт.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Зберегти звіт у CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName(defaultFileName());
        Window owner = reportTable.getScene() != null ? reportTable.getScene().getWindow() : null;
        File target = chooser.showSaveDialog(owner);
        if (target == null) {
            return;
        }
        try (PrintWriter pw = new PrintWriter(target, StandardCharsets.UTF_8)) {
            pw.write('\ufeff');
            pw.println(joinCsv(currentHeaders));
            for (ObservableList<String> row : currentRows) {
                pw.println(joinCsv(row));
            }
            alert(Alert.AlertType.INFORMATION, "CSV збережено: " + target.getAbsolutePath());
        } catch (IOException ex) {
            alert(Alert.AlertType.ERROR, "Не вдалося зберегти файл: " + ex.getMessage());
        }
    }

    private void tourLoadReport(LocalDate from, LocalDate to) {
        List<ReservationListRow> rows = reservations.findPage("", "Всі", "time", true, 0, 5000);
        Map<String, Integer> reservationsByTour = new HashMap<>();
        Map<String, Integer> guestsByTour = new HashMap<>();
        Map<String, Integer> cancellationsByTour = new HashMap<>();

        int totalRes = 0;
        int totalGuests = 0;
        for (ReservationListRow r : rows) {
            LocalDate sessionDate = parseDateSafe(r.getSessionStart());
            if (sessionDate == null || sessionDate.isBefore(from) || sessionDate.isAfter(to)) {
                continue;
            }
            String tour = r.getProgramTitle() == null ? "—" : r.getProgramTitle();
            reservationsByTour.merge(tour, 1, Integer::sum);
            if ("CANCELLED".equalsIgnoreCase(r.getReservationStatus())) {
                cancellationsByTour.merge(tour, 1, Integer::sum);
            } else {
                guestsByTour.merge(tour, r.getPartySize(), Integer::sum);
                totalGuests += r.getPartySize();
            }
            totalRes++;
        }

        List<List<String>> resultRows = new ArrayList<>();
        List<String> tourKeys = new ArrayList<>(reservationsByTour.keySet());
        tourKeys.sort(Comparator.naturalOrder());
        for (String tour : tourKeys) {
            int res = reservationsByTour.getOrDefault(tour, 0);
            int g = guestsByTour.getOrDefault(tour, 0);
            int cancels = cancellationsByTour.getOrDefault(tour, 0);
            double cancelRate = res == 0 ? 0 : cancels * 100.0 / res;
            double avg = (res - cancels) == 0 ? 0 : (double) g / (res - cancels);
            resultRows.add(List.of(
                    tour,
                    String.valueOf(res),
                    String.valueOf(g),
                    String.valueOf(cancels),
                    String.format("%.2f", avg),
                    String.format("%.1f%%", cancelRate)));
        }

        setReportData(
                List.of("Програма туру", "Бронювань", "Гостей (без скасованих)",
                        "Скасувань", "Сер. розмір групи", "Відсоток скасувань"),
                resultRows);
        summaryLabel.setText(String.format("Усього бронювань: %d · Гостей: %d", totalRes, totalGuests));
    }

    private void addonsFinanceReport(LocalDate from, LocalDate to) {
        Map<Integer, AddonService> svcMap = new HashMap<>();
        for (AddonService s : services.findAll()) {
            svcMap.put(s.getId(), s);
        }

        Map<String, Integer> qtyByService = new HashMap<>();
        Map<String, Double> totalByService = new HashMap<>();
        Map<String, Double> paidByService = new HashMap<>();

        double grandTotal = 0;
        double grandPaid = 0;
        for (VisitCharge c : charges.findAll()) {
            LocalDateTime when = c.getUsageDateTime();
            if (when == null) {
                continue;
            }
            LocalDate d = when.toLocalDate();
            if (d.isBefore(from) || d.isAfter(to)) {
                continue;
            }
            AddonService s = svcMap.get(c.getAddonServiceId());
            String name = s == null ? "Невідома послуга" : s.getName();
            double price = s == null ? 0 : s.getPricePerUnit();
            double amount = c.getTotalAmount(price);
            qtyByService.merge(name, c.getQuantity(), Integer::sum);
            totalByService.merge(name, amount, Double::sum);
            if (c.isPaid()) {
                paidByService.merge(name, amount, Double::sum);
                grandPaid += amount;
            }
            grandTotal += amount;
        }

        List<List<String>> result = new ArrayList<>();
        List<String> keys = new ArrayList<>(totalByService.keySet());
        keys.sort(Comparator.naturalOrder());
        for (String name : keys) {
            int qty = qtyByService.getOrDefault(name, 0);
            double total = totalByService.getOrDefault(name, 0.0);
            double paid = paidByService.getOrDefault(name, 0.0);
            double debt = total - paid;
            double rate = total == 0 ? 0 : paid * 100.0 / total;
            result.add(List.of(
                    name,
                    String.valueOf(qty),
                    String.format("%.2f грн", total),
                    String.format("%.2f грн", paid),
                    String.format("%.2f грн", debt),
                    String.format("%.1f%%", rate)));
        }
        setReportData(
                List.of("Послуга", "Кількість", "Виручка", "Оплачено", "Борг", "Рівень оплат"),
                result);
        summaryLabel.setText(String.format("Усього: %.2f грн · Оплачено: %.2f грн · Борг: %.2f грн",
                grandTotal, grandPaid, grandTotal - grandPaid));
    }

    private void clubRiskReport(LocalDate from, LocalDate to) {
        Map<Integer, String> guestNames = new HashMap<>();
        for (GuestProfile g : guests.findAllOrderByName()) {
            guestNames.put(g.getId(), g.getName());
        }

        List<List<String>> result = new ArrayList<>();
        int active = 0;
        int risky = 0;
        for (ClubMembership m : memberships.findAll()) {
            LocalDate v = m.getValidUntil();
            if (v == null || v.isBefore(from) || v.isAfter(to)) {
                continue;
            }
            String risk = detectRisk(m);
            if (m.isActive()) {
                active++;
            }
            if (risk.startsWith("Високий")) {
                risky++;
            }
            result.add(List.of(
                    guestNames.getOrDefault(m.getGuestId(), "—"),
                    m.getTier().getDisplayName(),
                    v.format(DATE_FMT),
                    m.getDiscountPercent() + "%",
                    m.isActive() ? "Активна" : "Неактивна",
                    risk));
        }
        result.sort(Comparator.comparing(r -> r.get(0)));
        setReportData(
                List.of("Гість", "Рівень", "Діє до", "Знижка", "Статус", "Ризик"),
                result);
        summaryLabel.setText(String.format("У вибірці: %d карток · Активних: %d · Високого ризику: %d",
                result.size(), active, risky));
    }

    private String detectRisk(ClubMembership m) {
        LocalDate today = LocalDate.now();
        LocalDate validUntil = m.getValidUntil();
        if (!m.isActive()) {
            return "Високий (неактивна)";
        }
        if (validUntil == null || !validUntil.isAfter(today.plusDays(7))) {
            return "Високий";
        }
        if (!validUntil.isAfter(today.plusDays(30))) {
            return "Середній";
        }
        return "Низький";
    }

    private LocalDate parseDateSafe(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            if (iso.length() >= 10) {
                return LocalDate.parse(iso.substring(0, 10));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void setReportData(List<String> headers, List<List<String>> rows) {
        currentHeaders.clear();
        currentHeaders.addAll(headers);
        reportTable.getColumns().clear();
        for (int i = 0; i < headers.size(); i++) {
            final int columnIndex = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(headers.get(i));
            col.setCellValueFactory(cell -> {
                ObservableList<String> values = cell.getValue();
                return new ReadOnlyStringWrapper(columnIndex < values.size() ? values.get(columnIndex) : "");
            });
            col.setSortable(false);
            reportTable.getColumns().add(col);
        }
        currentRows.clear();
        for (List<String> row : rows) {
            currentRows.add(FXCollections.observableArrayList(row));
        }
    }

    private boolean validateRange() {
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Оберіть період.");
            return false;
        }
        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            alert(Alert.AlertType.WARNING, "Дата початку має бути не пізніше дати кінця.");
            return false;
        }
        return true;
    }

    private String defaultFileName() {
        String type = reportTypeCombo.getValue() == null ? "report" : reportTypeCombo.getValue();
        String sanitized = type.toLowerCase().replace(' ', '-').replaceAll("[^a-z0-9а-яіїєґ\\-]", "");
        return String.format("%s_%s_%s.csv", sanitized, startDatePicker.getValue(), endDatePicker.getValue());
    }

    private String joinCsv(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsv(values.get(i)));
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private void alert(Alert.AlertType type, String message) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
