package utils;

import org.gdal.ogr.Geometry;
import tools.ConcaveHull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class Polygon {

    int id;
    Geometry geom;

    public ArrayList<double[][]> outerRings = new ArrayList<>();
    public ArrayList<double[][]> holes = new ArrayList<>();

    double[] centroid;
    double[][] points;



    public Polygon() {

    }



    public Polygon(double[][] points) {

        // read polygon rings from shapefile


    }

    public void addOuterRing(double[][] points) {

        outerRings.add(points);

    }

    public void addHole(double[][] points) {

        // add hole to polygon
        holes.add(points);

    }

    public void setCentroid(double[] centroid){

        this.centroid[0] = centroid[0];
        this.centroid[1] = centroid[1];

    }

    public void setId(int id){

        this.id = id;

    }

    public int getId(){

        return id;

    }

    public boolean pointInPolygon(double[] point) {

        if(this.outerRings.size() == 0) {
            int numPolyCorners = this.outerRings.get(0).length;
            double[][] poly = this.outerRings.get(0);
            int j = numPolyCorners - 1;
            boolean isInside = false;

            for (int i = 0; i < numPolyCorners; i++) {
                if (poly[i][1] < point[1] && poly[j][1] >= point[1] || poly[j][1] < point[1] && poly[i][1] >= point[1]) {
                    if (poly[i][0] + (point[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) * (poly[j][0] - poly[i][0]) < point[0]) {
                        isInside = !isInside;
                    }
                }
                j = i;
            }

            if (holes.size() > 0) {


                ArrayList<double[][]> holet = this.holes;
                double[][] polyHole = null;

                for (int i_ = 0; i_ < holet.size(); i_++) {

                    numPolyCorners = holet.get(i_).length;

                    //System.out.println(holet.get(i_)[0][0] + " == " + holet.get(i_)[holet.get(i_).length-1][0]);
                    //System.out.println(holet.get(i_)[0][1] + " == " + holet.get(i_)[holet.get(i_).length-1][1]);
                    j = numPolyCorners - 1;
                    boolean is_inside_hole = false;
                    polyHole = holet.get(i_);

                    for (int i = 0; i < numPolyCorners; i++) {

                        //System.out.println(polyHole[i][0] + " " + polyHole[i][1]);

                        if (polyHole[i][1] < point[1] && polyHole[j][1] >= point[1] || polyHole[j][1] < point[1] && polyHole[i][1] >= point[1]) {
                            if (polyHole[i][0] + (point[1] - polyHole[i][1]) / (polyHole[j][1] - polyHole[i][1]) * (polyHole[j][0] - polyHole[i][0]) < point[0]) {
                                is_inside_hole = !is_inside_hole;
                            }
                        }
                        j = i;
                    }

                    //System.out.println("-------------------------");
                    if (is_inside_hole) {

                        //System.out.println("YES YES YES!");
                        return false;
                    }
                }
            }

            return isInside;
        }else{
            return false;
        }

    }

    public boolean pointInPolygonHole(int holeIndex, double[] point) {




                ArrayList<double[][]> holet = this.holes;
                double[][] polyHole = holes.get(holeIndex);
                int numPolyCorners = polyHole.length;

                    //System.out.println(holet.get(i_)[0][0] + " == " + holet.get(i_)[holet.get(i_).length-1][0]);
                    //System.out.println(holet.get(i_)[0][1] + " == " + holet.get(i_)[holet.get(i_).length-1][1]);
                    int j = numPolyCorners - 1;
                    boolean is_inside_hole = false;

                    for (int i = 0; i < numPolyCorners; i++) {

                        //System.out.println(polyHole[i][0] + " " + polyHole[i][1]);

                        if (polyHole[i][1] < point[1] && polyHole[j][1] >= point[1] || polyHole[j][1] < point[1] && polyHole[i][1] >= point[1]) {
                            if (polyHole[i][0] + (point[1] - polyHole[i][1]) / (polyHole[j][1] - polyHole[i][1]) * (polyHole[j][0] - polyHole[i][0]) < point[0]) {
                                is_inside_hole = !is_inside_hole;
                            }
                        }
                        j = i;
                    }

                    //System.out.println("-------------------------");
                    if (is_inside_hole) {

                        //System.out.println("YES YES YES!");
                        return false;
                    }

                return true;

    }
    public boolean hasHole(){
        return this.holes.size() > 0 ? true : false;
    }

    public double[][] getHole(int index){
        return this.holes.get(index);
    }

    public double pointDistanceToHole(int holeIndex, double x, double y){

        double minDistance = Double.MAX_VALUE;
        int numPoints = holes.get(holeIndex).length;

        for (int i = 0; i < numPoints; i++) {
            double[] currentPoint = holes.get(holeIndex)[i];
            double[] nextPoint = holes.get(holeIndex)[((i + 1) % numPoints)];

            double distance = distanceToSegment(x, y, currentPoint[0], currentPoint[1], nextPoint[0], nextPoint[1]);
            minDistance = Math.min(minDistance, distance);
        }

        return minDistance;

    }

    public boolean pointInPolygon(double[][] polygon, double[] point) {

        int i;
        int j;
        boolean result = false;
        for (i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            if ((polygon[i][1] > point[1]) != (polygon[j][1] > point[1]) &&
                    (point[0] < (polygon[j][0] - polygon[i][0]) * (point[1] - polygon[i][1]) / (polygon[j][1] - polygon[i][1]) + polygon[i][0])) {
                result = !result;
            }
        }

        return result;
    }

    public boolean pointInPolygon(double[][] polygon, double x, double y) {

        int i;
        int j;
        boolean result = false;
        for (i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            if ((polygon[i][1] > y) != (polygon[j][1] > y) &&
                    (x < (polygon[j][0] - polygon[i][0]) * (y - polygon[i][1]) / (polygon[j][1] - polygon[i][1]) + polygon[i][0])) {
                result = !result;
            }
        }

        return result;
    }

    private static double distanceToSegment(double pointX, double pointY, double segmentStartX, double segmentStartY,
                                            double segmentEndX, double segmentEndY) {
        double segmentLength = distance(segmentStartX, segmentStartY, segmentEndX, segmentEndY);

        if (segmentLength == 0.0) {
            // The segment is actually a point
            return distance(pointX, pointY, segmentStartX, segmentStartY);
        }

        double u = ((pointX - segmentStartX) * (segmentEndX - segmentStartX) +
                (pointY - segmentStartY) * (segmentEndY - segmentStartY)) / (segmentLength * segmentLength);

        if (u < 0.0 || u > 1.0) {
            // The closest point is outside the segment, return the distance to the nearest endpoint
            double distanceToStart = distance(pointX, pointY, segmentStartX, segmentStartY);
            double distanceToEnd = distance(pointX, pointY, segmentEndX, segmentEndY);
            return Math.min(distanceToStart, distanceToEnd);
        }

        double intersectionX = segmentStartX + u * (segmentEndX - segmentStartX);
        double intersectionY = segmentStartY + u * (segmentEndY - segmentStartY);
        return distance(pointX, pointY, intersectionX, intersectionY);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public ArrayList<tools.ConcaveHull.Point> toPoints(){

        ArrayList<ConcaveHull.Point> output = new ArrayList<>();

        for (int i = 0; i < this.outerRings.size(); i++) {

            for(int j = 0; j < this.outerRings.get(i).length; j++){
                output.add(new ConcaveHull.Point(this.outerRings.get(i)[j][0], this.outerRings.get(i)[j][1]));
            }

        }

        return output;

    }
}
