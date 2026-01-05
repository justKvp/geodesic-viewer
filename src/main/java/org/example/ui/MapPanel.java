package org.example.ui;

import org.example.model.MapLine;
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

    private double scale = 1.0; // пиксели на метр
    private double offsetX = 0;
    private double offsetY = 0;

    private double lastMouseX;
    private double lastMouseY;

    private final List<MapPoint> points = new ArrayList<>();
    private final List<MapLine> lines = new ArrayList<>();

    private MapLine currentLine = null;
    private boolean lineDrawingMode = false;

    private volatile boolean loading = false;

    private JTextField scaleField;

    private final JLabel tooltipLabel = new JLabel();
    private final JWindow tooltipWindow;

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
                updateTooltip(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();

                if (lineDrawingMode && currentLine != null) {
                    MapPoint p = findPointNear(e);
                    if (p != null) {
                        currentLine.addPoint(p);
                        repaint();
                    }
                }
            }
        });
    }

    public void setScaleField(JTextField field) {
        this.scaleField = field;
    }

    public void setScaleManual(double pxPerMeter) {
        double centerX = offsetX + getWidth() / 2.0 / scale;
        double centerY = offsetY + getHeight() / 2.0 / scale;

        this.scale = pxPerMeter;

        // смещаем так, чтобы центр остался на месте
        offsetX = centerX - getWidth() / 2.0 / scale;
        offsetY = centerY - getHeight() / 2.0 / scale;

        if (scaleField != null) {
            scaleField.setText(String.format("%.0f", 1.0 / scale * getWidth()));
        }
        repaint();
    }


    public void setLoading(boolean loading) {
        this.loading = loading;
        repaint();
    }

    public void startLineDrawing(Color color, MapLine.LineStyle style) {
        currentLine = new MapLine(color, style);
        lines.add(currentLine);
        lineDrawingMode = true;
    }

    public void finishLineDrawing() {
        currentLine = null;
        lineDrawingMode = false;
        repaint();
    }

    public void clearLines() {
        lines.clear();
        repaint();
    }

    private MapPoint findPointNear(MouseEvent e) {
        for (MapPoint p : points) {
            Point pt = worldToScreen(p.getX(), p.getY());
            if (pt.distance(e.getX(), e.getY()) <= 10) return p;
        }
        return null;
    }

    public void loadTestPoints() {
        points.clear();
        points.addAll(List.of(
                new MapPoint(0, 0, 50, 1, "Начало"),
                new MapPoint(1000, 500, 30, 2, "Точка 2"),
                new MapPoint(2000, 1500, 70, 3, "Точка 3"),
                new MapPoint(5000, 2000, 90, 4, "Точка 4")
        ));
        fitPointsToScreen();
        repaint();
    }

    public void loadPointsFromFile(File file) {
        List<MapPoint> loaded = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p;
                if (line.contains("\t")) p = line.split("\t");
                else if (line.contains(";")) p = line.split(";");
                else p = line.split("\\s+");

                if (p.length >= 4) {
                    int id = Integer.parseInt(p[0].trim());
                    double x = Double.parseDouble(p[1].trim());
                    double y = Double.parseDouble(p[2].trim());
                    double z = Double.parseDouble(p[3].trim());
                    String comment = p.length >= 5 ? p[4].trim() : "";
                    loaded.add(new MapPoint(x, y, z, id, comment));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        points.clear();
        points.addAll(loaded);
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

            if (scaleField != null) scaleField.setText("");

            repaint();
        });
    }

    private void updateTooltip(MouseEvent e) {
        if (points.isEmpty()) {
            tooltipWindow.setVisible(false);
            return;
        }

        boolean onPoint = false;

        for (MapPoint p : points) {
            Point screen = worldToScreen(p.getX(), p.getY());

            // Игнорируем точки вне панели (или если ещё не рассчитаны)
            if (screen.x < 0 || screen.y < 0 || screen.x > getWidth() || screen.y > getHeight())
                continue;

            // Проверяем, близко ли к курсору
            if (screen.distance(e.getX(), e.getY()) <= 10) {
                // Собираем текст тултипа
                StringBuilder sb = new StringBuilder("<html>");
                sb.append("ID: ").append(p.getId()).append("<br>");
                sb.append("X: ").append(p.getX()).append("<br>");
                sb.append("Y: ").append(p.getY()).append("<br>");
                sb.append("Z: ").append(p.getZ());
                if (p.getComment() != null && !p.getComment().isBlank()) {
                    sb.append("<br>Комментарий: ").append(p.getComment());
                }
                sb.append("</html>");

                String text = sb.toString();

                // Показываем только если текст реально содержит данные
                if (!text.equals("<html></html>")) {
                    tooltipLabel.setText(text);
                    tooltipWindow.setLocation(e.getXOnScreen() + 10, e.getYOnScreen() + 10);
                    tooltipWindow.pack();
                    tooltipWindow.setVisible(true);
                    onPoint = true;
                }

                break;
            }
        }

        if (!onPoint) {
            tooltipWindow.setVisible(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        drawGrid(g2);
        drawGuides(g2);
        drawPoints(g2);
        drawLines(g2);

        if (loading) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            String text = "Загрузка...";
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() + fm.getAscent()) / 2;
            g2.drawString(text, x, y);
        }
    }

    private void drawPoints(Graphics2D g) {
        int baseSize = 8;
        int size = Math.max(baseSize, (int)(baseSize * scale));
        g.setFont(new Font("Arial", Font.PLAIN, Math.max(10, (int)(10 * scale))));

        double avgZ = points.stream().mapToDouble(MapPoint::getZ).average().orElse(0);
        double maxZ = points.stream().mapToDouble(MapPoint::getZ).max().orElse(avgZ);
        double minZ = points.stream().mapToDouble(MapPoint::getZ).min().orElse(avgZ);

        for (MapPoint p : points) {
            Point pt = worldToScreen(p.getX(), p.getY());
            if (pt.x < -size || pt.y < -size || pt.x > getWidth() + size || pt.y > getHeight() + size)
                continue;

            Color pointColor;
            if (p.getZ() > avgZ) {
                float ratio = (float) ((p.getZ() - avgZ) / (maxZ - avgZ + 0.0001));
                pointColor = new Color(0f, 0.5f + 0.5f * ratio, 0f);
            } else if (p.getZ() < avgZ) {
                float ratio = (float) ((avgZ - p.getZ()) / (avgZ - minZ + 0.0001));
                pointColor = new Color(0f, 0f, 0.5f + 0.5f * (1 - ratio));
            } else {
                pointColor = Color.GRAY;
            }

            g.setColor(pointColor);
            g.fillOval(pt.x - size / 2, pt.y - size / 2, size, size);

            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(p.getId()), pt.x + size / 2 + 2, pt.y - size / 2 - 2);
        }
    }

    private void drawLines(Graphics2D g) {
        for (MapLine line : lines) {
            g.setColor(line.getColor());
            if (line.getStyle() == MapLine.LineStyle.DASHED)
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0));
            else
                g.setStroke(new BasicStroke(2f));

            List<MapPoint> pts = line.getPoints();
            for (int i = 0; i < pts.size() - 1; i++) {
                Point a = worldToScreen(pts.get(i).getX(), pts.get(i).getY());
                Point b = worldToScreen(pts.get(i + 1).getX(), pts.get(i + 1).getY());
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(Color.LIGHT_GRAY);
        int step = 100;
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
            paint(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); } catch (PrinterException ex) { ex.printStackTrace(); }
        }
    }
}
