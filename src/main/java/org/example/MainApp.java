package org.example;

import org.example.model.MapLine;
import org.example.ui.MapPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Map Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            // ===== Карта =====
            MapPanel map = new MapPanel();

            // ===== Верхняя панель =====
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

            // --- Масштаб ---
            controls.add(new JLabel("Масштаб 1 :"));

            JTextField scaleField = new JTextField(6);
            controls.add(scaleField);

            // обработчик Enter для установки масштаба
            scaleField.addActionListener(e -> {
                try {
                    double ratio = Double.parseDouble(scaleField.getText().trim());
                    if (ratio > 0) {
                        map.setScaleByRatio(ratio);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Неверный масштаб!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            });

            // если zoom мышью, очищаем поле
            map.addMouseWheelListener(e -> scaleField.setText(""));

            controls.add(new JSeparator(SwingConstants.VERTICAL));

            // ===== Инструменты линий =====
            JButton lineSolidBlue = new JButton("Линия (синяя)");
            JButton lineSolidBlack = new JButton("Линия (черная)");
            JButton lineSolidGreen = new JButton("Линия (зеленая)");
            JButton lineDashedRed = new JButton("Пунктир (красная)");

            JButton finishLine = new JButton("Завершить линию");
            JButton clearLines = new JButton("Очистить линии");

            lineSolidBlue.addActionListener(e ->
                    map.startLineDrawing(Color.BLUE, MapLine.LineStyle.SOLID)
            );
            lineSolidBlack.addActionListener(e ->
                    map.startLineDrawing(Color.BLACK, MapLine.LineStyle.SOLID)
            );
            lineSolidGreen.addActionListener(e ->
                    map.startLineDrawing(Color.GREEN, MapLine.LineStyle.SOLID)
            );
            lineDashedRed.addActionListener(e ->
                    map.startLineDrawing(Color.RED, MapLine.LineStyle.DASHED)
            );

            finishLine.addActionListener(e -> map.finishLineDrawing());
            clearLines.addActionListener(e -> map.clearLines());

            controls.add(lineSolidBlue);
            controls.add(lineSolidBlack);
            controls.add(lineSolidGreen);
            controls.add(lineDashedRed);
            controls.add(finishLine);
            controls.add(clearLines);

            // ===== Меню =====
            JMenuBar bar = new JMenuBar();
            JMenu file = new JMenu("Файл");

            JMenuItem open = new JMenuItem("Открыть CSV");
            open.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File fileToLoad = fc.getSelectedFile();

                    SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() {
                            map.setLoading(true);
                            map.loadPointsFromCSV(fileToLoad);
                            return null;
                        }

                        @Override
                        protected void done() {
                            map.setLoading(false);
                        }
                    };
                    worker.execute();
                }
            });

            JMenuItem png = new JMenuItem("Сохранить PNG");
            png.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    map.saveToPNG(fc.getSelectedFile());
                }
            });

            JMenuItem printItem = new JMenuItem("Печать на принтер");
            printItem.addActionListener(e -> map.showPrintPreview());

            file.add(open);
            file.add(png);
            file.add(printItem);
            bar.add(file);

            frame.setJMenuBar(bar);

            // ===== Компоновка =====
            frame.add(controls, BorderLayout.NORTH);
            frame.add(map, BorderLayout.CENTER);

            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // ===== Тестовые данные =====
            map.loadTestPoints();
        });
    }
}
