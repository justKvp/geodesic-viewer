package org.example;

import org.example.ui.MapPanel;

import javax.swing.*;
import java.io.File;

public class MainApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Map Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            MapPanel map = new MapPanel();
            frame.add(map);

            JMenuBar bar = new JMenuBar();
            JMenu file = new JMenu("Файл");

            JMenuItem open = new JMenuItem("Открыть CSV");
            open.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    map.loadPointsFromCSV(fc.getSelectedFile());
                }
            });

            JMenuItem png = new JMenuItem("Сохранить PNG");
            png.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    map.saveToPNG(fc.getSelectedFile());
                }
            });

            file.add(open);
            file.add(png);
            bar.add(file);

            frame.setJMenuBar(bar);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            map.loadTestPoints();
        });
    }
}
