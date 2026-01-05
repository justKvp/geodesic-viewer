package org.example.model;

public class MapPoint {
    private final double x;
    private final double y;
    private final int id;
    private final String comment;

    public MapPoint(double x, double y, int id, String comment) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.comment = comment;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getId() { return id; }
    public String getComment() { return comment; }
}