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
            controls.add(new JLabel("Масштаб (1:X):"));

            JTextField scaleField = new JTextField(8);
            controls.add(scaleField);
            map.setScaleField(scaleField);

            JButton applyScale = new JButton("Применить");
            applyScale.addActionListener(e -> {
                try {
                    double ratio = Double.parseDouble(scaleField.getText().trim());
                    // вычисляем scale в пикселях на метр (1 метр = ? пикселей)
                    double pxPerMeter = map.getWidth() / ratio;
                    map.setScaleManual(pxPerMeter);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Неверный масштаб");
                }
            });
            controls.add(applyScale);

            controls.add(new JSeparator(SwingConstants.VERTICAL));

            // --- Инструменты линий ---
            JButton lineBlue = new JButton("Линия (синяя)");
            JButton lineRedDashed = new JButton("Пунктир (красный)");
            JButton lineGreen = new JButton("Линия (зелёная)");
            JButton lineBlack = new JButton("Линия (чёрная)");
            JButton finishLine = new JButton("Завершить линию");
            JButton clearLines = new JButton("Очистить линии");

            lineBlue.addActionListener(e -> map.startLineDrawing(Color.BLUE, MapLine.LineStyle.SOLID));
            lineRedDashed.addActionListener(e -> map.startLineDrawing(Color.RED, MapLine.LineStyle.DASHED));
            lineGreen.addActionListener(e -> map.startLineDrawing(Color.GREEN, MapLine.LineStyle.SOLID));
            lineBlack.addActionListener(e -> map.startLineDrawing(Color.BLACK, MapLine.LineStyle.SOLID));
            finishLine.addActionListener(e -> map.finishLineDrawing());
            clearLines.addActionListener(e -> map.clearLines());

            controls.add(lineBlue);
            controls.add(lineRedDashed);
            controls.add(lineGreen);
            controls.add(lineBlack);
            controls.add(finishLine);
            controls.add(clearLines);

            // ===== Меню =====
            JMenuBar bar = new JMenuBar();
            JMenu file = new JMenu("Файл");

            JMenuItem open = new JMenuItem("Открыть CSV/TXT");
            open.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File fileToLoad = fc.getSelectedFile();
                    SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() {
                            map.setLoading(true);
                            map.loadPointsFromFile(fileToLoad); // метод loadPointsFromFile реализован в MapPanel
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

            JMenuItem savePNG = new JMenuItem("Сохранить PNG");
            savePNG.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    map.saveToPNG(fc.getSelectedFile());
                }
            });

            JMenuItem printItem = new JMenuItem("Печать на принтер");
            printItem.addActionListener(e -> map.showPrintPreview());

            file.add(open);
            file.add(savePNG);
            file.add(printItem);
            bar.add(file);
            frame.setJMenuBar(bar);

            // ===== Компоновка =====
            frame.add(controls, BorderLayout.NORTH);
            frame.add(map, BorderLayout.CENTER);

            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // ===== Тестовые точки =====
            map.loadTestPoints();
        });
    }
}
