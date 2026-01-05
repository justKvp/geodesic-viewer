package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.ui.MapCanvas;

import java.io.File;
import java.util.List;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        // --- Canvas ---
        MapCanvas canvas = new MapCanvas(800, 600);
        root.setCenter(canvas);

        // --- Панель под кнопки ---
        HBox bottomPanel = new HBox(10);
        bottomPanel.setStyle("-fx-padding: 5; -fx-background-color: #ddd;");
        root.setBottom(bottomPanel);

        // --- Тестовая кнопка: Сохранить скриншот ---
        Button saveButton = new Button("Сохранить скриншот");
        saveButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить карту как PNG");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PNG файлы", "*.png")
            );
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                canvas.saveToPNG(file);
            }
        });
        bottomPanel.getChildren().add(saveButton);

        // --- Привязка размеров Canvas к центру окна, учитывая панель снизу ---
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty().subtract(bottomPanel.heightProperty()));

        // --- Redraw при изменении размеров ---
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> canvas.draw());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> canvas.draw());

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Map Demo");
        stage.show();

        // --- Загрузка CSV через аргументы или FileChooser ---
        File csvFile = null;

        List<String> args = getParameters().getRaw();
        if (!args.isEmpty()) {
            File fileArg = new File(args.get(0));
            if (fileArg.exists()) {
                csvFile = fileArg;
            } else {
                System.err.println("CSV файл не найден: " + fileArg.getAbsolutePath());
            }
        }

        if (csvFile == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите CSV карту");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV файлы", "*.csv")
            );
            csvFile = fileChooser.showOpenDialog(stage);
        }

        if (csvFile != null) {
            canvas.loadPointsFromCSV(csvFile);
        } else {
            canvas.loadTestPoints();
        }

        // --- Центрирование и авто-отдаление ---
        canvas.fitPointsToScreen();
        canvas.draw();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
