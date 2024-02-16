package utils;

import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogr;
import tools.ConcaveHull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LineModification {

    public LineModification(){

    }


    public static List<ConcaveHull.Point> smoothPolygon(List<ConcaveHull.Point> points) {
        List<ConcaveHull.Point> smoothedPoints = new ArrayList<>();

        // Ensure at least 3 points for interpolation
        if (points.size() < 3) {
            return new ArrayList<>(points);
        }

        // Perform linear interpolation between consecutive points
        for (int i = 0; i < points.size() - 1; i++) {
            ConcaveHull.Point currentPoint = points.get(i);
            ConcaveHull.Point nextPoint = points.get(i + 1);

            // Interpolate between current and next points
            double interpolatedX = (currentPoint.getX() + nextPoint.getX()) / 2.0;
            double interpolatedY = (currentPoint.getY() + nextPoint.getY()) / 2.0;

            smoothedPoints.add(new ConcaveHull.Point(interpolatedX, interpolatedY));
        }

        // Add the last point (ensuring it's the same as the first point for a closed polygon)
        //ConcaveHull.Point lastPoint = points.get(points.size() - 1);
        //smoothedPoints.add(new ConcaveHull.Point(lastPoint.getX(), lastPoint.getY()));
        smoothedPoints.add( new ConcaveHull.Point(smoothedPoints.get(0).getX(), smoothedPoints.get(0).getY()));
        // print smoothed points
        //for (int i = 0; i < smoothedPoints.size(); i++) {
        //    System.out.println("Smoothed point " + i + ": " + smoothedPoints.get(i).getX() + ", " + smoothedPoints.get(i).getY());
        //}

        //System.exit(1);
        return smoothedPoints;
    }

    public static boolean checkPolygonValidity(List<ConcaveHull.Point> points){

        Geometry polyGeom = new Geometry(ogr.wkbPolygon);

        Geometry pointGeom = new Geometry(ogr.wkbLinearRing);

        for(int i = 0; i < points.size(); i++){
            pointGeom.AddPoint_2D(points.get(i).getX(), points.get(i).getY());
        }

        polyGeom.AddGeometry(pointGeom);

        System.out.println("--------------------");

        return polyGeom.IsValid() && polyGeom.IsSimple();

    }

}
