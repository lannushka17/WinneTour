package com.sab.winery.controller;

import com.sab.winery.persistence.entity.WineProduct;
import com.sab.winery.persistence.entity.Winery;
import com.sab.winery.persistence.implementation.WineProductRepositoryImpl;
import com.sab.winery.persistence.implementation.WineryRepositoryImpl;
import java.io.File;
import java.net.URL;
import java.util.List;
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
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

/** Довідник продукції та залишки на складі — як у курсовому проєкті. */
public class WineProductController implements Initializable {

    @FXML
    private ComboBox<Winery> wineryCombo;

    @FXML
    private TableView<WineProduct> table;

    @FXML
    private TableColumn<WineProduct, Integer> colId;

    @FXML
    private TableColumn<WineProduct, String> colName;

    @FXML
    private TableColumn<WineProduct, String> colCat;

    @FXML
    private TableColumn<WineProduct, Double> colPrice;

    @FXML
    private TableColumn<WineProduct, Integer> colStock;

    @FXML
    private TextField nameField;

    @FXML
    private TextField categoryField;

    @FXML
    private Spinner<Double> priceSpinner;

    @FXML
    private Spinner<Integer> stockSpinner;

    @FXML
    private TextField imageField;

    @FXML
    private ImageView preview;

    private final WineryRepositoryImpl wineries = new WineryRepositoryImpl();
    private final WineProductRepositoryImpl products = new WineProductRepositoryImpl();

    private final ObservableList<WineProduct> data = FXCollections.observableArrayList();

    private WineProduct selected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockUnits"));
        table.setItems(data);

        priceSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100000, 300, 10));
        priceSpinner.setEditable(false);

        stockSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1_000_000, 0, 1));

        table.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, o, n) -> {
                            selected = n;
                            if (n != null) {
                                nameField.setText(n.getName());
                                categoryField.setText(n.getCategory());
                                priceSpinner.getValueFactory().setValue(n.getPrice());
                                stockSpinner.getValueFactory().setValue(n.getStockUnits());
                                imageField.setText(n.getImagePath() != null ? n.getImagePath() : "");
                                loadPreview(imageField.getText());
                                wineries.findAll().stream()
                                        .filter(w -> w.getId() == n.getWineryId())
                                        .findFirst()
                                        .ifPresent(w -> wineryCombo.getSelectionModel().select(w));
                            }
                        });

        refreshData();
    }

    public void refreshData() {
        List<Winery> w = wineries.findAll();
        wineryCombo.setItems(FXCollections.observableArrayList(w));
        if (!w.isEmpty()) {
            wineryCombo.getSelectionModel().selectFirst();
        }
        data.setAll(products.findAll());
        table.getSelectionModel().clearSelection();
    }

    @FXML
    private void browseImage() {
        FileChooser ch = new FileChooser();
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("Зображення", "*.png", "*.jpg", "*.jpeg"));
        File f = ch.showOpenDialog(nameField.getScene().getWindow());
        if (f != null) {
            imageField.setText(f.getAbsolutePath());
            loadPreview(f.getAbsolutePath());
        }
    }

    private void loadPreview(String path) {
        if (path == null || path.isBlank()) {
            preview.setImage(null);
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            preview.setImage(new Image(file.toURI().toString(), 100, 100, true, true));
        } else {
            preview.setImage(null);
        }
    }

    @FXML
    private void saveProduct() {
        Winery w = wineryCombo.getSelectionModel().getSelectedItem();
        if (w == null) {
            alert(Alert.AlertType.WARNING, "Оберіть виноробню.");
            return;
        }
        if (nameField.getText().trim().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Назва продукту обов'язкова.");
            return;
        }
        double price = priceSpinner.getValue();
        WineProduct p =
                WineProduct.builder()
                        .id(selected != null ? selected.getId() : 0)
                        .wineryId(w.getId())
                        .name(nameField.getText().trim())
                        .category(categoryField.getText())
                        .price(price)
                        .stockUnits(stockSpinner.getValue())
                        .imagePath(blankToNull(imageField.getText()))
                        .active(true)
                        .build();
        products.save(p);
        alert(Alert.AlertType.INFORMATION, "Продукт збережено.");
        refreshData();
    }

    @FXML
    private void clearForm() {
        selected = null;
        table.getSelectionModel().clearSelection();
        nameField.clear();
        categoryField.clear();
        imageField.clear();
        preview.setImage(null);
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
