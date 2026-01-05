package org.example.ui;

import javafx.geometry.Point2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import org.example.model.MapPoint;

import javafx.embed.swing.SwingFXUtils;
import javafx.print.PrinterJob;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapCanvas extends Canvas {

    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    private double lastMouseX;
    private double lastMouseY;

    private final int gridStep = 100;

    private final List<MapPoint> points = new ArrayList<>();
    private final Tooltip tooltip = new Tooltip();

    public MapCanvas(double width, double height) {
        super(width, height);
        enableZoom();
        enableDrag();
        setupTooltip();
    }

    /** Загрузка тестовых точек */
    public void loadTestPoints() {
        points.clear();
        points.addAll(List.of(
                new MapPoint(10000, 100, 1, "Начало"),
                new MapPoint(4000, 300, 2, "Точка интереса"),
                new MapPoint(8000, 600, 3, "Метка 3"),
                new MapPoint(12000, 900, 4, "Последняя")
        ));
    }

    // Метод для сохранения в PNG
    public void saveAsImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить карту как PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG файлы", "*.png")
        );
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            WritableImage image = new WritableImage((int)getWidth(), (int)getHeight());
            SnapshotParameters params = new SnapshotParameters();
            this.snapshot(params, image);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Метод для печати
    public void printCanvas() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(getScene().getWindow())) {
            this.snapshot(null, null); // обновляем изображение
            job.printPage(this);
            job.endJob();
        }
    }

    /** Загрузка точек из CSV */
    public void loadPointsFromCSV(File csvFile) {
        List<MapPoint> loadedPoints = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // пропускаем заголовок
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    int id = Integer.parseInt(parts[0].trim());
                    double x = Double.parseDouble(parts[1].trim());
                    double y = Double.parseDouble(parts[2].trim());
                    String comment = parts[3].trim();
                    loadedPoints.add(new MapPoint(x, y, id, comment));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        points.clear();
        points.addAll(loadedPoints);
    }

    /** Автоцентрирование и масштабирование */
    public void fitPointsToScreen() {
        if (points.isEmpty()) return;

        double minX = points.stream().mapToDouble(MapPoint::getX).min().getAsDouble();
        double maxX = points.stream().mapToDouble(MapPoint::getX).max().getAsDouble();
        double minY = points.stream().mapToDouble(MapPoint::getY).min().getAsDouble();
        double maxY = points.stream().mapToDouble(MapPoint::getY).max().getAsDouble();

        double worldWidth = maxX - minX;
        double worldHeight = maxY - minY;

        double scaleX = getWidth() / (worldWidth * 1.1);
        double scaleY = getHeight() / (worldHeight * 1.1);

        scale = Math.min(scaleX, scaleY);

        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;

        offsetX = centerX - getWidth() / (2 * scale);
        offsetY = centerY - getHeight() / (2 * scale);
    }

    /** Рисование всей карты */
    public void draw() {
        GraphicsContext g = getGraphicsContext2D();
        g.setFill(Color.WHITESMOKE);
        g.fillRect(0, 0, getWidth(), getHeight());

        drawGrid(g);
        drawGuides(g);
        drawPoints(g);
    }

    private void drawPoints(GraphicsContext g) {
        g.setFill(Color.DARKRED);
        for (MapPoint p : points) {
            Point2D screen = worldToScreen(p.getX(), p.getY());
            g.fillOval(screen.getX() - 4, screen.getY() - 4, 8, 8);
        }
    }

    private void drawGrid(GraphicsContext g) {
        g.setStroke(Color.LIGHTGRAY);
        g.setFont(Font.font(10));
        g.setFill(Color.BLACK);
        g.setLineWidth(Math.max(0.3, scale));

        double minPixelStep = 50;
        double step = gridStep;

        while (step * scale < minPixelStep) {
            step *= 2;
        }

        double minX = screenToWorldX(0);
        double maxX = screenToWorldX(getWidth());
        double minY = screenToWorldY(0);
        double maxY = screenToWorldY(getHeight());

        for (double x = Math.floor(minX / step) * step; x <= Math.ceil(maxX / step) * step; x += step) {
            Point2D screen = worldToScreen(x, 0);
            g.strokeLine(screen.getX(), 0, screen.getX(), getHeight());
            g.fillText(String.valueOf((int)x), screen.getX() + 2, 12);
        }

        for (double y = Math.floor(minY / step) * step; y <= Math.ceil(maxY / step) * step; y += step) {
            Point2D screen = worldToScreen(0, y);
            g.strokeLine(0, screen.getY(), getWidth(), screen.getY());
            g.fillText(String.valueOf((int)y), 2, screen.getY() - 2);
        }
    }

    private void drawGuides(GraphicsContext g) {
        double width = getWidth();
        double height = getHeight();

        g.setStroke(Color.RED);
        g.setLineWidth(Math.max(0.3, scale));
        g.setFill(Color.RED);
        g.setFont(Font.font(12));

        Point2D zero = worldToScreen(0, 0);

        // ось X
        g.strokeLine(zero.getX(), 0, zero.getX(), height);
        g.fillText("+X →", zero.getX() + 5, 15);
        g.fillText("← -X", zero.getX() - 35, 15);

        // ось Y
        g.strokeLine(0, zero.getY(), width, zero.getY());
        g.fillText("+Y ↓", 5, zero.getY() + 15);
        g.fillText("↑ -Y", 5, zero.getY() - 5);
    }

    private Point2D worldToScreen(double x, double y) {
        return new Point2D((x - offsetX) * scale, (y - offsetY) * scale);
    }

    private double screenToWorldX(double px) { return px / scale + offsetX; }
    private double screenToWorldY(double py) { return py / scale + offsetY; }

    private void enableZoom() {
        setOnScroll((ScrollEvent e) -> {
            double zoomDelta = Math.signum(e.getDeltaY());
            double zoomFactor = Math.pow(1.1, zoomDelta);

            double mouseX = e.getX() / scale + offsetX;
            double mouseY = e.getY() / scale + offsetY;

            scale *= zoomFactor;

            offsetX = mouseX - e.getX() / scale;
            offsetY = mouseY - e.getY() / scale;

            draw();
            e.consume();
        });
    }

    private void enableDrag() {
        setOnMousePressed((MouseEvent e) -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });

        setOnMouseDragged((MouseEvent e) -> {
            double dx = (e.getX() - lastMouseX) / scale;
            double dy = (e.getY() - lastMouseY) / scale;

            offsetX -= dx;
            offsetY -= dy;

            lastMouseX = e.getX();
            lastMouseY = e.getY();

            draw();
        });
    }

    private void setupTooltip() {
        Tooltip.install(this, tooltip);

        setOnMouseMoved((MouseEvent e) -> {
            boolean onPoint = false;
            for (MapPoint p : points) {
                Point2D screen = worldToScreen(p.getX(), p.getY());
                double dx = e.getX() - screen.getX();
                double dy = e.getY() - screen.getY();
                double distance = Math.sqrt(dx*dx + dy*dy);

                if (distance <= 6) {
                    tooltip.setText(
                            "ID: " + p.getId() +
                                    "\nX: " + p.getX() +
                                    "\nY: " + p.getY() +
                                    "\nКомментарий: " + p.getComment()
                    );
                    tooltip.show(this, e.getScreenX() + 10, e.getScreenY() + 10);
                    onPoint = true;
                    break;
                }
            }
            if (!onPoint) tooltip.hide();
        });
    }

    // --- Сохранение в PNG ---
    public void saveToPNG(File file) {
        try {
            var image = new javafx.scene.image.WritableImage((int)getWidth(), (int)getHeight());
            snapshot(null, image);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- Печать ---
    public void print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(getScene().getWindow())) {
            if (job.printPage(this)) job.endJob();
        }
    }
}
