package org.example.ui;

import org.example.model.MapPoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MapPanel extends JPanel {

    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    private double lastMouseX;
    private double lastMouseY;

    private final int gridStep = 100;
    private final List<MapPoint> points = new ArrayList<>();

    private final JLabel tooltipLabel = new JLabel();
    private final JWindow tooltipWindow;

    public void showPrintPreview() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Печать карты");

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            double scaleX = pageFormat.getImageableWidth() / getWidth();
            double scaleY = pageFormat.getImageableHeight() / getHeight();
            double printScale = Math.min(scaleX, scaleY);
            g2.scale(printScale, printScale);

            // рисуем всю панель на странице
            paint(g2);

            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) { // Показываем диалог выбора принтера
            try {
                job.print();
            } catch (PrinterException ex) {
                ex.printStackTrace();
            }
        }
    }

    public MapPanel() {
        setBackground(Color.WHITE);
        enableDrag();
        enableZoom();

        tooltipWindow = new JWindow();
        tooltipWindow.add(tooltipLabel);
        tooltipLabel.setOpaque(true);
        tooltipLabel.setBackground(new Color(255, 255, 200));
        tooltipLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean onPoint = false;
                for (MapPoint p : points) {
                    Point screen = worldToScreen(p.getX(), p.getY());
                    double dx = e.getX() - screen.x;
                    double dy = e.getY() - screen.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= 10) { // увеличил область срабатывания
                        tooltipLabel.setText("<html>ID: " + p.getId() +
                                "<br>X: " + p.getX() +
                                "<br>Y: " + p.getY() +
                                "<br>Комментарий: " + p.getComment() + "</html>");
                        tooltipWindow.setLocation(e.getXOnScreen() + 10, e.getYOnScreen() + 10);
                        tooltipWindow.pack();
                        tooltipWindow.setVisible(true);
                        onPoint = true;
                        break;
                    }
                }
                if (!onPoint) tooltipWindow.setVisible(false);
            }
        });
    }

    public void loadTestPoints() {
        points.clear();
        points.addAll(List.of(
                new MapPoint(10000, 100, 1, "Начало"),
                new MapPoint(4000, 300, 2, "Точка интереса"),
                new MapPoint(8000, 600, 3, "Метка 3"),
                new MapPoint(12000, 900, 4, "Последняя")
        ));
        fitPointsToScreen();
        repaint();
    }

    public void loadPointsFromCSV(File csvFile) {
        List<MapPoint> loadedPoints = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
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
        fitPointsToScreen();
        repaint();
    }

    public void fitPointsToScreen() {
        if (points.isEmpty()) return;

        double minX = points.stream().mapToDouble(MapPoint::getX).min().orElse(0);
        double maxX = points.stream().mapToDouble(MapPoint::getX).max().orElse(0);
        double minY = points.stream().mapToDouble(MapPoint::getY).min().orElse(0);
        double maxY = points.stream().mapToDouble(MapPoint::getY).max().orElse(0);

        double panelWidth = getWidth() > 0 ? getWidth() : 1000;
        double panelHeight = getHeight() > 0 ? getHeight() : 700;

        double worldWidth = maxX - minX;
        double worldHeight = maxY - minY;

        scale = Math.min(panelWidth / (worldWidth * 1.1), panelHeight / (worldHeight * 1.1));

        offsetX = minX - (panelWidth / scale - worldWidth) / 2;
        offsetY = minY - (panelHeight / scale - worldHeight) / 2;
    }

    public void saveToPNG(File file) {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        paint(image.getGraphics());
        try {
            javax.imageio.ImageIO.write(image, "png", file);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private Point worldToScreen(double x, double y) {
        int sx = (int) ((x - offsetX) * scale);
        int sy = (int) ((y - offsetY) * scale);
        return new Point(sx, sy);
    }

    private void enableDrag() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                double dx = (e.getX() - lastMouseX) / scale;
                double dy = (e.getY() - lastMouseY) / scale;
                offsetX -= dx;
                offsetY -= dy;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                repaint();
            }
        });
    }

    private void enableZoom() {
        addMouseWheelListener(e -> {
            double zoomFactor = Math.pow(1.1, -e.getWheelRotation());
            double mouseX = e.getX() / scale + offsetX;
            double mouseY = e.getY() / scale + offsetY;

            scale *= zoomFactor;
            offsetX = mouseX - e.getX() / scale;
            offsetY = mouseY - e.getY() / scale;

            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // фон
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        // сетка
        drawGrid(g2);

        // красные оси
        drawGuides(g2);

        // точки
        drawPoints(g2);
    }

    private void drawPoints(Graphics2D g) {
        g.setColor(Color.RED);
        int size = Math.max(8, (int)(12 * scale)); // масштабируем с zoom
        for (MapPoint p : points) {
            Point pt = worldToScreen(p.getX(), p.getY());
            if(pt.x < -size || pt.y < -size || pt.x > getWidth()+size || pt.y > getHeight()+size) continue;
            g.fillOval(pt.x - size/2, pt.y - size/2, size, size);
        }
    }

    private void drawGuides(Graphics2D g) {
        int width = getWidth();
        int height = getHeight();

        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(Math.max(1f, (float)scale)));

        Point origin = worldToScreen(0,0);

        g.drawLine(origin.x, 0, origin.x, height);
        g.drawString("+X →", origin.x + 5, 15);
        g.drawString("← -X", origin.x - 35, 15);

        g.drawLine(0, origin.y, width, origin.y);
        g.drawString("+Y ↓", 5, origin.y + 15);
        g.drawString("↑ -Y", 5, origin.y - 5);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(Color.LIGHT_GRAY);
        int step = gridStep;

        double minX = offsetX;
        double minY = offsetY;
        double maxX = offsetX + getWidth()/scale;
        double maxY = offsetY + getHeight()/scale;

        while (step*scale < 50) step *= 2;

        g.setFont(new Font("Arial", Font.PLAIN, 10));
        for(double x = Math.floor(minX/step)*step; x <= Math.ceil(maxX/step)*step; x+=step){
            int sx = (int)((x - offsetX)*scale);
            g.drawLine(sx,0,sx,getHeight());
            g.drawString(String.valueOf((int)x), sx+2, 12);
        }
        for(double y = Math.floor(minY/step)*step; y <= Math.ceil(maxY/step)*step; y+=step){
            int sy = (int)((y - offsetY)*scale);
            g.drawLine(0,sy,getWidth(),sy);
            g.drawString(String.valueOf((int)y), 2, sy-2);
        }
    }
}
