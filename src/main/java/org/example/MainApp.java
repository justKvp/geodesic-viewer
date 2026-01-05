package org.example;

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

            // --- Карта ---
            MapPanel map = new MapPanel();

            // --- Верхняя панель управления ---
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

            controls.add(new JLabel("Масштаб:"));

            JButton scale100 = new JButton("1 : 100");
            JButton scale1000 = new JButton("1 : 1000");
            JButton scale5000 = new JButton("1 : 5000");
            JButton scale10000 = new JButton("1 : 10000");
            JButton scale15000 = new JButton("1 : 15000");

            scale100.addActionListener(e -> map.setScaleByRatio(100));
            scale1000.addActionListener(e -> map.setScaleByRatio(1000));
            scale5000.addActionListener(e -> map.setScaleByRatio(5000));
            scale10000.addActionListener(e -> map.setScaleByRatio(10000));
            scale15000.addActionListener(e -> map.setScaleByRatio(15000));

            controls.add(scale100);
            controls.add(scale1000);
            controls.add(scale5000);
            controls.add(scale10000);
            controls.add(scale15000);

            // --- Меню ---
            JMenuBar bar = new JMenuBar();
            JMenu file = new JMenu("Файл");

            JMenuItem open = new JMenuItem("Открыть CSV");
            open.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File fileToLoad = fc.getSelectedFile();

                    // ⚠ Загрузка в фоне + loading overlay
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

            // --- Компоновка ---
            frame.add(controls, BorderLayout.NORTH);
            frame.add(map, BorderLayout.CENTER);

            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // --- Тестовые точки ---
            map.loadTestPoints();
        });
    }
}
