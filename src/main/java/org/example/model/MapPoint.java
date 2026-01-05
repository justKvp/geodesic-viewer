package org.example.model;

public class MapPoint {
    private final double x;
    private final double y;
    private final double z;
    private final int id;
    private final String comment;

    public MapPoint(double x, double y, double z, int id, String comment) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = id;
        this.comment = comment;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public int getId() { return id; }
    public String getComment() { return comment; }
}