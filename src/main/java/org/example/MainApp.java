package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.ui.MapCanvas;

import java.io.File;
import java.util.List;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        MapCanvas canvas = new MapCanvas(800, 600);
        root.getChildren().add(canvas);

        // Привязка размеров Canvas к окну
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> canvas.draw());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> canvas.draw());

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Map Demo");
        stage.show();

        File csvFile = null;

        // --- 1️⃣ Проверка аргументов командной строки ---
        List<String> args = getParameters().getRaw();
        if (!args.isEmpty()) {
            File fileArg = new File(args.get(0));
            if (fileArg.exists()) {
                csvFile = fileArg;
            } else {
                System.err.println("CSV файл не найден: " + fileArg.getAbsolutePath());
            }
        }

        // --- 2️⃣ Если аргументов нет или файл не найден — FileChooser ---
        if (csvFile == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите CSV карту");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV файлы", "*.csv")
            );
            csvFile = fileChooser.showOpenDialog(stage);
        }

        // --- Загружаем точки ---
        if (csvFile != null) {
            canvas.loadPointsFromCSV(csvFile);
        } else {
            canvas.loadTestPoints(); // если пользователь отменил выбор
        }

        // автоцентр и авто-отдаление
        canvas.fitPointsToScreen();
        canvas.draw();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
