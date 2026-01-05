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

    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    private double lastMouseX;
    private double lastMouseY;

    private final int gridStep = 100;
    private final List<MapPoint> points = new ArrayList<>();

    private final JLabel tooltipLabel = new JLabel();
    private final JWindow tooltipWindow;

    private volatile boolean loading = false;

    // ===== ЛИНИИ =====
    private final List<MapLine> lines = new ArrayList<>();
    private MapLine currentLine = null;
    private boolean lineDrawingMode = false;

    public void setScaleByRatio(double ratio) {
        if (ratio <= 0) return;

        // центр экрана в мировых координатах
        double centerWorldX = offsetX + getWidth() / (2 * scale);
        double centerWorldY = offsetY + getHeight() / (2 * scale);

        // масштаб 1 : N
        scale = 1.0 / ratio;

        // пересчитываем смещение, чтобы центр остался на месте
        offsetX = centerWorldX - getWidth() / (2 * scale);
        offsetY = centerWorldY - getHeight() / (2 * scale);

        repaint();
    }

    public MapPanel() {
        setBackground(Color.WHITE);
        enableDrag();
        enableZoom();

        // tooltip
        tooltipWindow = new JWindow();
        tooltipWindow.add(tooltipLabel);
        tooltipLabel.setOpaque(true);
        tooltipLabel.setBackground(new Color(255, 255, 200));
        tooltipLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // hover tooltip
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean onPoint = false;
                for (MapPoint p : points) {
                    Point screen = worldToScreen(p.getX(), p.getY());
                    if (screen.distance(e.getPoint()) <= 10) {
                        tooltipLabel.setText(
                                "<html>ID: " + p.getId() +
                                        "<br>X: " + p.getX() +
                                        "<br>Y: " + p.getY() +
                                        "<br>Z: " + p.getZ() +
                                        "<br>" + p.getComment() + "</html>"
                        );
                        tooltipWindow.setLocation(
                                e.getXOnScreen() + 10,
                                e.getYOnScreen() + 10
                        );
                        tooltipWindow.pack();
                        tooltipWindow.setVisible(true);
                        onPoint = true;
                        break;
                    }
                }
                if (!onPoint) tooltipWindow.setVisible(false);
            }
        });

        // клики для линий
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!lineDrawingMode || currentLine == null) return;

                MapPoint p = findPointNear(e);
                if (p != null) {
                    currentLine.addPoint(p);
                    repaint();
                }
            }
        });
    }

    // ================= API =================

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

    public void setLoading(boolean loading) {
        this.loading = loading;
        repaint();
    }

    // ================= POINTS =================

    public void loadTestPoints() {
        points.clear();
        points.addAll(List.of(
                new MapPoint(10000, 100, 50, 1, "Начало"),
                new MapPoint(4000, 300, 20, 2, "Точка интереса"),
                new MapPoint(8000, 600, 70, 3, "Метка 3"),
                new MapPoint(12000, 900, 90, 4, "Последняя"),
                new MapPoint(-500, 200, 10, 5, "Отрицательная X"),
                new MapPoint(600, -300, 30, 6, "Отрицательная Y"),
                new MapPoint(-200, -150, 60, 7, "Отрицательные X и Y")
        ));
        fitPointsToScreen();
        repaint();
    }

    public void loadPointsFromCSV(File csvFile) {
        List<MapPoint> loaded = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] p = line.split(",");
                if (p.length >= 5) {
                    loaded.add(new MapPoint(
                            Double.parseDouble(p[1].trim()),
                            Double.parseDouble(p[2].trim()),
                            Double.parseDouble(p[3].trim()),
                            Integer.parseInt(p[0].trim()),
                            p[4].trim()
                    ));
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

    // ================= DRAW =================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2);
        drawGuides(g2);
        drawLines(g2);
        drawPoints(g2);

        if (loading) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.drawString("Загрузка...", getWidth() / 2 - 70, getHeight() / 2);
        }
    }

    private void drawLines(Graphics2D g) {
        for (MapLine line : lines) {
            if (line.getPoints().size() < 2) continue;

            Stroke stroke = line.getStyle() == MapLine.LineStyle.DASHED
                    ? new BasicStroke(Math.max(1f, (float) (2 * scale)),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    0,
                    new float[]{10f, 10f},
                    0)
                    : new BasicStroke(Math.max(1f, (float) (2 * scale)));

            g.setStroke(stroke);
            g.setColor(line.getColor());

            List<MapPoint> pts = line.getPoints();
            Point prev = worldToScreen(pts.get(0).getX(), pts.get(0).getY());

            for (int i = 1; i < pts.size(); i++) {
                Point curr = worldToScreen(pts.get(i).getX(), pts.get(i).getY());
                g.drawLine(prev.x, prev.y, curr.x, curr.y);
                prev = curr;
            }
        }
    }

    private void drawPoints(Graphics2D g) {
        int size = Math.max(8, (int) (8 * scale));

        double avgZ = points.stream().mapToDouble(MapPoint::getZ).average().orElse(0);
        double maxZ = points.stream().mapToDouble(MapPoint::getZ).max().orElse(avgZ);
        double minZ = points.stream().mapToDouble(MapPoint::getZ).min().orElse(avgZ);

        for (MapPoint p : points) {
            Point pt = worldToScreen(p.getX(), p.getY());

            Color c;
            if (p.getZ() > avgZ) {
                float r = (float) ((p.getZ() - avgZ) / (maxZ - avgZ + 0.0001));
                c = new Color(0f, 0.5f + 0.5f * r, 0f);
            } else if (p.getZ() < avgZ) {
                float r = (float) ((avgZ - p.getZ()) / (avgZ - minZ + 0.0001));
                c = new Color(0f, 0f, 1f - 0.5f * r);
            } else {
                c = Color.GRAY;
            }

            g.setColor(c);
            g.fillOval(pt.x - size / 2, pt.y - size / 2, size, size);

            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(p.getId()), pt.x + size / 2 + 2, pt.y - size / 2 - 2);
        }
    }

    // ================= HELPERS =================

    private MapPoint findPointNear(MouseEvent e) {
        for (MapPoint p : points) {
            if (worldToScreen(p.getX(), p.getY()).distance(e.getPoint()) <= 10) {
                return p;
            }
        }
        return null;
    }

    private Point worldToScreen(double x, double y) {
        return new Point(
                (int) ((x - offsetX) * scale),
                (int) ((y - offsetY) * scale)
        );
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
                offsetX -= (e.getX() - lastMouseX) / scale;
                offsetY -= (e.getY() - lastMouseY) / scale;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                repaint();
            }
        });
    }

    private void enableZoom() {
        addMouseWheelListener(e -> {
            double factor = Math.pow(1.1, -e.getWheelRotation());
            double wx = e.getX() / scale + offsetX;
            double wy = e.getY() / scale + offsetY;

            scale *= factor;
            offsetX = wx - e.getX() / scale;
            offsetY = wy - e.getY() / scale;
            repaint();
        });
    }

    private void drawGuides(Graphics2D g) {
        Point o = worldToScreen(0, 0);
        g.setColor(Color.RED);
        g.drawLine(o.x, 0, o.x, getHeight());
        g.drawLine(0, o.y, getWidth(), o.y);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(Color.LIGHT_GRAY);
        int step = gridStep;

        while (step * scale < 50) step *= 2;

        double minX = offsetX;
        double maxX = offsetX + getWidth() / scale;
        double minY = offsetY;
        double maxY = offsetY + getHeight() / scale;

        for (double x = Math.floor(minX / step) * step; x <= maxX; x += step) {
            int sx = (int) ((x - offsetX) * scale);
            g.drawLine(sx, 0, sx, getHeight());
            g.drawString(String.valueOf((int) x), sx + 2, 12);
        }

        for (double y = Math.floor(minY / step) * step; y <= maxY; y += step) {
            int sy = (int) ((y - offsetY) * scale);
            g.drawLine(0, sy, getWidth(), sy);
            g.drawString(String.valueOf((int) y), 2, sy - 2);
        }
    }

    public void showPrintPreview() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable((g, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double s = Math.min(
                    pf.getImageableWidth() / getWidth(),
                    pf.getImageableHeight() / getHeight()
            );
            g2.scale(s, s);
            paint(g2);
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try { job.print(); } catch (PrinterException e) { e.printStackTrace(); }
        }
    }

    private void fitPointsToScreen() {
        if (points.isEmpty()) return;

        double minX = points.stream().mapToDouble(MapPoint::getX).min().orElse(0);
        double maxX = points.stream().mapToDouble(MapPoint::getX).max().orElse(0);
        double minY = points.stream().mapToDouble(MapPoint::getY).min().orElse(0);
        double maxY = points.stream().mapToDouble(MapPoint::getY).max().orElse(0);

        double w = getWidth() > 0 ? getWidth() : 1000;
        double h = getHeight() > 0 ? getHeight() : 700;

        scale = Math.min(
                w / ((maxX - minX) * 1.1),
                h / ((maxY - minY) * 1.1)
        );

        offsetX = minX - (w / scale - (maxX - minX)) / 2;
        offsetY = minY - (h / scale - (maxY - minY)) / 2;
    }

    public void saveToPNG(File file) {
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        paint(img.getGraphics());
        try {
            javax.imageio.ImageIO.write(img, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
