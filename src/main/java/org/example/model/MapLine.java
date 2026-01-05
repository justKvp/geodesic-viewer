package org.example.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MapLine {

    public enum LineStyle {
        SOLID, DASHED
    }

    private final List<MapPoint> points = new ArrayList<>();
    private final Color color;
    private final LineStyle style;

    public MapLine(Color color, LineStyle style) {
        this.color = color;
        this.style = style;
    }

    public void addPoint(MapPoint p) {
        points.add(p);
    }

    public List<MapPoint> getPoints() {
        return points;
    }

    public Color getColor() {
        return color;
    }

    public LineStyle getStyle() {
        return style;
    }
}
