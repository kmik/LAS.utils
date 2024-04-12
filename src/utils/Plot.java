package utils;

public class Plot {

    double centerX, centerY, radius;

    public Plot() {

    }

    public Plot(double centerX, double centerY, double radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public double getRadius() {
        return radius;
    }

    public boolean pointInPlot(double x, double y) {
        return Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2) <= Math.pow(radius, 2);
    }



}
