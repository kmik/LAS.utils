package utils;

import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import tools.ConcaveHull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static utils.Stanford2010.clone2DArray;

public class ShapefileUtils {

    Layer layer;
    KdTree kdTree = null;
    public String shapetype;

    boolean createKdTreeOfPoints = false;
    HashMap<Integer, Feature> pointFeatures = new HashMap<>();
    HashMap<Integer, ArrayList<Integer>> polyAffiliation = new HashMap<>();
    HashMap<Integer, String> polyIdsAsString = new HashMap<>();
    HashMap<Integer, double[][]> bounds = new HashMap<>();

    HashMap<Integer, double[][]> holes = new HashMap<>();
    HashMap<Integer, double[]> centroids = new HashMap<>();

    HashMap<Integer, double[]> boundingBoxes = new HashMap<>();

    HashMap<Integer, Double> areas = new HashMap<>();

    HashMap<Integer, Polygon> polygons = new HashMap<>();
    KdTree centroids_ = new KdTree();




    argumentReader aR;
    public ShapefileUtils(){

        ogr.RegisterAll();
        gdal.AllRegister();

    }

    public ShapefileUtils(argumentReader aR){
        ogr.RegisterAll();
        gdal.AllRegister();
        this.aR = aR;
    }

    public void createKdTree(){
        this.createKdTreeOfPoints = true;
    }

    public void calculateBoundingBoxes(){

        for(int i : bounds.keySet()){

            double[] bbox = getBoundingBox(i);
            boundingBoxes.put(i, bbox);


        }

    }

    public void addAffiliation(int polyId, int affiliation){

        if(polyAffiliation.containsKey(polyId)){
            polyAffiliation.get(polyId).add(affiliation);
        }else{
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(affiliation);
            polyAffiliation.put(polyId, temp);
        }

    }

    public int getAffiliation(int polyId){

        if(polyAffiliation.containsKey(polyId)){
            return polyAffiliation.get(polyId).get(0);
        }else{
            return -1;
        }

    }

    public  HashMap<Integer, double[][]> getBounds() {
        return bounds;
    }

    public double[] getBoundingBox(){

        double[] bbox = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};

        for(int i : bounds.keySet()){

            double[] tempBbox = getBoundingBox(i);

            if(tempBbox[0] < bbox[0]){
                bbox[0] = tempBbox[0];
            }

            if(tempBbox[1] < bbox[1]){
                bbox[1] = tempBbox[1];
            }

            if(tempBbox[2] > bbox[2]){
                bbox[2] = tempBbox[2];
            }

            if(tempBbox[3] > bbox[3]){
                bbox[3] = tempBbox[3];
            }

        }

        return bbox;

    }
    public double[] getBoundingBox(int id){

        //System.out.println(id);
        double[][] points = bounds.get(id);

        double[] bbox = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};

        for(int i = 0; i < points.length; i++){

            if(points[i][0] < bbox[0]){
                bbox[0] = points[i][0];
            }

            if(points[i][1] < bbox[1]){
                bbox[1] = points[i][1];
            }

            if(points[i][0] > bbox[2]){
                bbox[2] = points[i][0];
            }

            if(points[i][1] > bbox[3]){
                bbox[3] = points[i][1];
            }

        }

        return bbox;

    }

    public double calculateAreas(){

        for(int i : bounds.keySet()){

            double[] bbox = getBoundingBox(i);
            double[][] points = bounds.get(i);
            double area = 0.0;

            for (int j = 0; j < points.length; j++) {
                double[] p1 = points[j];
                double[] p2 = points[(j + 1) % points.length];

                area += p1[0] * p2[1] - p2[0] * p1[1];
            }

            area = Math.abs(area) / 2.0;
            areas.put(i, area);
            System.out.println(area);
        }

        return 0;
    }
    public ArrayList<double[]> randomPointsInAllPolygons(double minDistance){

            ArrayList<double[]> points = new ArrayList<>();

            KdTree kdTree = new KdTree();
            KdTree.XYZPoint point = new KdTree.XYZPoint(0, 0, 0, 0);

            for(int i : bounds.keySet()){


                double[] bbox = getBoundingBox(i);

                double x = bbox[0] + Math.random() * (bbox[2] - bbox[0]);
                double y = bbox[1] + Math.random() * (bbox[3] - bbox[1]);

                int nTries = 100;


                boolean breakLoop = false;
                // Iterate this until no more point can be added

                while(!breakLoop){

                    int counter = 0;
                    boolean distanceOk = false;
                    boolean pointInPolygon = false;

                    while (!pointInPolygon || !distanceOk) {

                        x = bbox[0] + Math.random() * (bbox[2] - bbox[0]);
                        y = bbox[1] + Math.random() * (bbox[3] - bbox[1]);

                        pointInPolygon = pointInPolygon(i, x, y);

                        if (points.size() == 0)
                            distanceOk = true;
                        else {
                            distanceOk = distanceToNearest(kdTree, new KdTree.XYZPoint(x, y, 0, 0)) > minDistance;
                        }

                        if(counter++ > nTries){
                            breakLoop = true;
                            break;
                        }
                    }

                    if(!breakLoop) {
                        //System.out.println("Point added: " + x + " " + y + " " + i);
                        points.add(new double[]{x, y});
                        kdTree.add(new KdTree.XYZPoint(x, y, 0, 0));
                    }

                    //System.out.println("Points: " + points.size() + " " + i);
                }
                //System.out.println("Points: " + points.size() + " " + i);
            }

            return points;
    }

    /*
    public ArrayList<double[]> randomPointsWithinBoundingBoxes(double minDistance) {

        ArrayList<double[]> points = new ArrayList<>();

        KdTree kdTree = new KdTree();
        KdTree.XYZPoint point = new KdTree.XYZPoint(0, 0, 0, 0);

        double[] bbox = getBoundingBox();

        for (int i : bounds.keySet()) {

            double[] bbox = getBoundingBox(i);

            double x = bbox[0] + Math.random() * (bbox[2] - bbox[0]);
            double y = bbox[1] + Math.random() * (bbox[3] - bbox[1]);

            int nTries = 100;
        }
    }

     */

    public HashMap<Integer, ArrayList<double[]>> randomCirclesWithinPolygons(double circleRadius, double minDistanceBetweenTwoCircles, double areaFraction){

        double[] bbox = getBoundingBox();
        HashMap<Integer, ArrayList<double[]>> circles = new HashMap<>();


        for(int i : this.bounds.keySet()){

            System.out.println("Polygon: " + i);
            KdTree tmpTree = new KdTree();

            int currentPolyId = i;

            double[] currentBbox = getBoundingBox(i);

            double currentArea = areas.get(i);

            double targetArea = currentArea * areaFraction;
            int maxNumTries = 200;

            ArrayList<double[]> circlesInPoly = new ArrayList<>();

            boolean breakLoop = false;

            int numTries = 0;

            while(!breakLoop){

                double x = currentBbox[0] + Math.random() * (currentBbox[2] - currentBbox[0]);
                double y = currentBbox[1] + Math.random() * (currentBbox[3] - currentBbox[1]);

                int counter = 0;
                boolean distanceOk = false;
                boolean pointInPolygon = false;
                boolean distanceToBoundaryOk = false;

                while (!pointInPolygon || !distanceOk || !distanceToBoundaryOk) {

                    x = currentBbox[0] + Math.random() * (currentBbox[2] - currentBbox[0]);
                    y = currentBbox[1] + Math.random() * (currentBbox[3] - currentBbox[1]);

                    pointInPolygon = pointInPolygon(i, x, y);

                    distanceToBoundaryOk = distanceToPolygonBorder(x, y, currentPolyId) > circleRadius;

                    if (circlesInPoly.size() == 0)
                        distanceOk = true;
                    else {
                        distanceOk = distanceToNearest(tmpTree, new KdTree.XYZPoint(x, y, 0, 0)) > minDistanceBetweenTwoCircles;
                    }

                    //System.out.println(distanceToBoundaryOk + " " + pointInPolygon + " " + distanceOk + " " + x + " " + y + " " + i + " " + counter + " " + numTries);

                    if(counter++ > maxNumTries){
                        //System.out.println("Max number of tries reached: " + maxNumTries);
                        breakLoop = true;
                        break;
                    }
                }

                if(!breakLoop) {
                    //System.out.println("Point added: " + x + " " + y + " " + i);
                    circlesInPoly.add(new double[]{x, y});
                    tmpTree.add(new KdTree.XYZPoint(x, y, 0, 0));
                }
            }

            circles.put(i, circlesInPoly);
        }

        return circles;
    }

    public ArrayList<double[]> randomPointsInAllPolygonsNearRegular(double minDistance){

        ArrayList<double[]> points = new ArrayList<>();

        KdTree kdTree = new KdTree();
        KdTree.XYZPoint point = new KdTree.XYZPoint(0, 0, 0, 0);

        for(int i : bounds.keySet()){

            double[] bbox = getBoundingBox(i);

            double x = bbox[0] + Math.random() * (bbox[2] - bbox[0]);
            double y = bbox[1] + Math.random() * (bbox[3] - bbox[1]);

            int nTries = 100;


            boolean breakLoop = false;
            // Iterate this until no more point can be added

            while(!breakLoop){

                int counter = 0;
                boolean distanceOk = false;
                boolean pointInPolygon = false;

                while (!pointInPolygon || !distanceOk) {

                    x = bbox[0] + Math.random() * (bbox[2] - bbox[0]);
                    y = bbox[1] + Math.random() * (bbox[3] - bbox[1]);

                    pointInPolygon = pointInPolygon(i, x, y);
                    if (points.size() == 0)
                        distanceOk = true;
                    else {
                        distanceOk = distanceToNearest(kdTree, new KdTree.XYZPoint(x, y, 0, 0)) > minDistance;
                    }

                    if(counter++ > nTries){
                        breakLoop = true;
                        break;
                    }
                }



                if(!breakLoop) {
                    //System.out.println("Point added: " + x + " " + y + " " + i);
                    points.add(new double[]{x, y});
                    kdTree.add(new KdTree.XYZPoint(x, y, 0, 0));
                }

                //System.out.println("Points: " + points.size() + " " + i);
            }
            //System.out.println("Points: " + points.size() + " " + i);
        }

        return points;
    }
    public double distanceToNearest(KdTree tree, KdTree.XYZPoint point){


        List< KdTree.XYZPoint> nearest = (List< KdTree.XYZPoint>) tree.nearestNeighbourSearch(1, point);

        return Math.sqrt(Math.pow(nearest.get(0).getX() - point.getX(), 2) + Math.pow(nearest.get(0).getY() - point.getY(), 2));

    }

    public String getPolyIdAsString(int id){
        return polyIdsAsString.get(id);
    }

    public double[][] getBounds(int id){
        return bounds.get(id);
    }

    public boolean canCastStringToInteger(String str) {
    try {
        Integer.parseInt(str);
        return true;
    } catch (NumberFormatException e) {
        return false;
    }
}
    public void readShapeFiles(String shapeFile) throws IOException {


        System.out.println("Reading shapefile: " + shapeFile);
        DataSource ds = ogr.Open( shapeFile );
        //DataSource ds2 = ogr.Open( shapeFile2 );

        if( ds == null ) {
            System.out.println( "Opening stand shapefile failed." );
            System.exit( 1 );
        }

        Layer shapeFileLayer = ds.GetLayer(0);

        int shapeIdRolling = 0;
        int shapeId = 0;
        int nExcluded = 0;

        HashSet<Integer> usedIds = new HashSet<>();
        HashMap<Integer, Integer> usedStandids = new HashMap<>();

        boolean debugPrint = false;

        int pointId = 0;

        for(long i = 0; i < shapeFileLayer.GetFeatureCount(); i++ ) {

            if(true) {

                Feature tempF = shapeFileLayer.GetFeature(i);

                String id;

                if(!aR.field_string.equals(""))
                    id = tempF.GetFieldAsString(aR.field_string);
                else
                    id = Integer.toString(tempF.GetFieldAsInteger(aR.field));


                //if(id == null){

                //    System.out.println("null");
                //    System.exit(1);
                //}

                //tempF.GetFiel
                //System.out.println("id: " + id + " " + tempF.GetFieldCount());
                Geometry tempG = tempF.GetGeometryRef();

                boolean canCast = canCastStringToInteger(id);

                int castShapeId = -1;

// check if geometry is a MultiPolygon
                if (tempG.GetGeometryName().equals("MULTIPOLYGON")) {

                    this.shapetype = "polygon";
                    //System.out.println("here1 " + tempF.GetFieldAsInteger(0));

                   // System.out.println("MULTIPOLYGON");

                    int numGeom = tempG.GetGeometryCount();

                    for (int j = 0; j < numGeom; j++) {

                        int numGeom2 = tempG.GetGeometryRef(j).GetGeometryCount();



                        for(int j_ = 0; j_ < numGeom2; j_++){

                            Geometry tempG2 = tempG.GetGeometryRef(j).GetGeometryRef(j_);

                            //System
                            //System.out.println(tempG2.GetGeometryName());

                            if (tempG2.GetGeometryName().equals("LINEARRING")) {

                                if (tempG2 == null || tempG2.GetPoints() == null) {
                                    continue;
                                }

                                if(j_ == 0) {

                                    Polygon tempP = new Polygon();

                                    if(canCast)
                                        castShapeId = Integer.parseInt(id);
                                    else
                                        castShapeId = shapeId++;


                                    double[] centroid = tempG.GetGeometryRef(j).Centroid().GetPoint(0);
                                    //System.out.println("here3 " + tempF.GetFieldAsInteger(0));
                                    bounds.put(castShapeId, clone2DArray(tempG2.GetPoints()));
                                    //System.out.println("here4 " + tempF.GetFieldAsInteger(0));
                                    centroids.put(castShapeId, new double[]{centroid[0], centroid[1]});
                                    polyIdsAsString.put(castShapeId, id);
                                    //System.out.println("here5 " + tempF.GetFieldAsInteger(0));
                                    centroids_.add(new KdTree.XYZPoint(centroid[0], centroid[1], 0, castShapeId));

                                    //System.out.println("Centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);

                                    tempP.addOuterRing(clone2DArray(tempG2.GetPoints()));
                                    tempP.setId(castShapeId);
                                    polygons.put(castShapeId, tempP);


                                    polygons.put(castShapeId, new Polygon());

                                    if (usedIds.contains(castShapeId)) {
                                        System.out.println("duplicate id: " + castShapeId);
                                        System.exit(1);
                                    }
                                    usedIds.add(castShapeId);

                                }else{
                                    holes.put(castShapeId, clone2DArray(tempG2.GetPoints()));

                                    polygons.get(castShapeId).addHole(clone2DArray(tempG2.GetPoints()));

                                }


                                //System.out.println("centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);
                                //System.out.println("here6 " + tempF.GetFieldAsInteger(0));
                            }
                        }
                    }
                }
                else if (tempG.GetGeometryName().equals("POLYGON")) {

                    Geometry tempG2 = tempG.GetGeometryRef(0);


                    //System.out.println(tempG2.GetPointCount() + " " + tempG2.GetGeometryName().equals("LINEARRING"));
                    //shapeId++;

                    if(canCast)
                        castShapeId = Integer.parseInt(id);
                    else
                        castShapeId = shapeId++;

                    int numGeom = tempG.GetGeometryCount();

                    //System.out.println("numGeom: " + numGeom);

                    if (tempG2.GetGeometryName().equals("LINEARRING")) {


                        for(int j = 0; j < numGeom; j++) {

                            Geometry tempG3 = tempG.GetGeometryRef(j);


                            //System.out.println("POINTS: " + tempG3.GetPointCount());

                            if (tempG3 == null || tempG3.GetPoints() == null) {
                                continue;
                            }


                            if (j == 0){

                                Polygon tempP = new Polygon();

                                double[] centroid = tempG.Centroid().GetPoint(0);
                                //System.out.println("here3 " + tempF.GetFieldAsInteger(0));
                                bounds.put(castShapeId, clone2DArray(tempG3.GetPoints()));
                                //System.out.println("here4 " + tempF.GetFieldAsInteger(0));
                                centroids.put(castShapeId, new double[]{centroid[0], centroid[1]});
                                polyIdsAsString.put(castShapeId, id);
                                //System.out.println("here5 " + tempF.GetFieldAsInteger(0));

                                //System.out.println("centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);

                                centroids_.add(new KdTree.XYZPoint(centroid[0], centroid[1], 0, castShapeId));

                                tempP.addOuterRing(clone2DArray(tempG3.GetPoints()));
                                tempP.setId(castShapeId);
                                polygons.put(castShapeId, tempP);


                                polygons.put(castShapeId, new Polygon());

                                if (usedIds.contains(castShapeId)) {
                                    System.out.println("duplicate id: " + castShapeId);
                                    System.exit(1);
                                }
                                usedIds.add(castShapeId);

                            }else{

                                holes.put(castShapeId, clone2DArray(tempG3.GetPoints()));
                                polygons.get(castShapeId).addHole(clone2DArray(tempG3.GetPoints()));

                            }
                        }
                        //System.out.println("centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);
                    }
                } else if(tempG.GetGeometryName().equals("POINT")){
                    // handle other types of geometries if needed
                    shapeId = pointId++;

                    if(createKdTreeOfPoints) {

                        if(this.kdTree == null){
                            this.kdTree = new KdTree();
                        }

                        double[] centroid = tempG.GetPoint(0);

                        KdTree.XYZPoint p = new KdTree.XYZPoint(centroid[0], centroid[1], 0, shapeId);
                        kdTree.add(p);
                    }
                    this.shapetype = "point";

                    System.out.println("Point shapeId " + shapeId);
                    if(!pointFeatures.containsKey(shapeId)){
                        pointFeatures.put(shapeId, tempF);
                    }else{

                    }


                }

                //if(tempF.GetFieldAsInteger(0) == 199){
                //    System.exit(1);
                //}

            }

            if(debugPrint) {
                //System.out.println("shapeId: " + shapeId);
                //System.exit(1);
            }
            debugPrint = false;

        }

        //System.exit(1 );

        System.out.println(bounds.size() + " stand bounds read.");
        System.out.println(centroids.size() + " stand centroids read.");
        System.out.println(nExcluded + " stands rejected");
        System.out.println("holes: " + holes.size());

        System.out.println("Point features: " + pointFeatures.size());


    }

    public boolean pointInPolygon(int polyId, double x, double y){

            return pointInPolygon(bounds.get(polyId), new double[]{x, y});

    }

    public boolean pointInPolyon(int polyId, double[] point){

        return pointInPolygon(bounds.get(polyId), point);

    }

    public boolean pointInAnyPolygon(double x, double y){

        for(int i : bounds.keySet()){

            if(pointInPolygon(i, x, y)){
                return true;
            }

        }

        return false;

    }

    public int pointInWhichPolygon(double x, double y){

        for(int i : bounds.keySet()){


            if(pointInPolygon(i, x, y)){

                return i;
            }
        }
        return -1;

    }

    public List<Feature> kNearestPoints(double x, double y, int k, double radius){

        ArrayList<KdTree.XYZPoint> nearest = (ArrayList<KdTree.XYZPoint>)kdTree.nearestNeighbourSearch(k, new KdTree.XYZPoint(x, y, 0, 0));

        ArrayList<Feature> output = new ArrayList<>();

        for(KdTree.XYZPoint p : nearest){

            if(euclideanDistance(p.getX(), p.getY(), x, y) > radius){
                continue;
            }

            output.add(pointFeatures.get(p.getIndex()));
        }

        return output;

    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){

        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));

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

    public short[][] shapeFileToGrid(double resolutionX, double resolutionY){

        double[] bbox_ = getBoundingBox();

        double gridX = Math.ceil((bbox_[2] - bbox_[0]) / resolutionX);
        double gridY = Math.ceil((bbox_[3] - bbox_[1]) / resolutionY);

        //System.out.println("Number of grid cells: " + gridX * gridY);
        // In megabytes
        //System.out.println("Memory usage: " + (gridX * gridY * 2 * 2) / 1024 / 1024 + " MB");

        return new short[(int)gridX][(int)gridY];


    }

    public int[][] gridToAffiliationGrid(short[][] grid ) {

        int[][] output = new int[grid.length][grid[0].length];

        for(int x = 0; x < grid.length; x++) {
            for(int y = 0; y < grid[0].length; y++) {

                double[] realCoordinates = gridCoordinateToRealCoordinate(x, y);
                int affiliation = pointInWhichPolygon(realCoordinates[0], realCoordinates[1]);
                output[x][y] = affiliation;
            }
        }

        return output;
    }

    public int numPolygons(){

        return bounds.size();

    }

    public double[] gridCoordinateToRealCoordinate(int x, int y){

        double[] bbox = getBoundingBox();

        double realX = bbox[0] + x * aR.step + aR.step / 2;
        double realY = bbox[3] - y * aR.step - aR.step / 2;

        return new double[]{realX, realY};

    }

    public boolean boundsOverlap(double[][] bounds1, double[][] bounds2) {
        if (polygonSeparatingAxis(bounds1, bounds2) || polygonSeparatingAxis(bounds2, bounds1)) {
            return false; // Polygons are separated, so no overlap
        }
        return true; // Polygons overlap
    }

    private boolean polygonSeparatingAxis(double[][] poly1, double[][] poly2) {
        for (int i = 0; i < poly1.length; i++) {
            double[] p1 = poly1[i];
            double[] p2 = poly1[(i + 1) % poly1.length];
            double[] edge = new double[]{p2[1] - p1[1], p1[0] - p2[0]}; // Get edge vector

            if (separatingAxis(edge, poly1, poly2))
                return true; // Found a separating axis
        }
        return false; // No separating axis found
    }

    private boolean separatingAxis(double[] axis, double[][] poly1, double[][] poly2) {
        double min1 = Double.POSITIVE_INFINITY;
        double max1 = Double.NEGATIVE_INFINITY;
        double min2 = Double.POSITIVE_INFINITY;
        double max2 = Double.NEGATIVE_INFINITY;

        // Project poly1 onto axis
        for (double[] point : poly1) {
            double projection = point[0] * axis[0] + point[1] * axis[1];
            min1 = Math.min(min1, projection);
            max1 = Math.max(max1, projection);
        }

        // Project poly2 onto axis
        for (double[] point : poly2) {
            double projection = point[0] * axis[0] + point[1] * axis[1];
            min2 = Math.min(min2, projection);
            max2 = Math.max(max2, projection);
        }

        // Check for overlap
        return !(max1 >= min2 && max2 >= min1);
    }

    public double overlapPercentage(double[][] bounds1, double[][] bounds2) {
        if (!boundsOverlap(bounds1, bounds2)) {
            //System.out.println("No overlap");
            return 0; // No overlap, so percentage is 0
        }

        // Calculate the area of intersection
        double intersectionArea = calculateIntersectionArea(bounds1, bounds2);

        // Calculate the area of the smaller polygon
        double area1 = calculatePolygonArea(bounds1);
        double area2 = calculatePolygonArea(bounds2);
        double smallerArea = Math.min(area1, area2);

        //System.out.println(area1 + " " + area2 + " " + smallerArea + " " + intersectionArea + " " + (intersectionArea / smallerArea) * 100);

        // Calculate and return the overlap percentage
        return (intersectionArea / smallerArea) * 100;
    }

    public double calculateIntersectionArea(double[][] bounds1, double[][] bounds2) {
        // Check if one polygon is completely within the other
        if (isPolygonContained(bounds1, bounds2)) {
            return calculatePolygonArea(bounds1); // Return the area of the smaller polygon
        } else if (isPolygonContained(bounds2, bounds1)) {
            return calculatePolygonArea(bounds2); // Return the area of the smaller polygon
        }

        // Otherwise, find intersection points and calculate the area as before
        List<double[]> intersectionPoints = new ArrayList<>();
        for (int i = 0; i < bounds1.length; i++) {
            for (int j = 0; j < bounds2.length; j++) {
                double[] intersection = lineIntersect(bounds1[i], bounds1[(i + 1) % bounds1.length],
                        bounds2[j], bounds2[(j + 1) % bounds2.length]);
                if (intersection != null) {
                    intersectionPoints.add(intersection);
                }
            }
        }

        if (intersectionPoints.isEmpty()) {
            return 0; // No intersection points found, so no intersection area
        }

        // Sort the intersection points by polar angle from a reference point (e.g., the centroid)
        double[] centroid = calculateCentroid(intersectionPoints);
        Collections.sort(intersectionPoints, (p1, p2) -> {
            double angle1 = Math.atan2(p1[1] - centroid[1], p1[0] - centroid[0]);
            double angle2 = Math.atan2(p2[1] - centroid[1], p2[0] - centroid[0]);
            return Double.compare(angle1, angle2);
        });

        // Calculate the area of the resulting polygon formed by the intersection points
        double area = 0;
        for (int i = 0; i < intersectionPoints.size(); i++) {
            double[] p1 = intersectionPoints.get(i);
            double[] p2 = intersectionPoints.get((i + 1) % intersectionPoints.size());
            area += p1[0] * p2[1] - p2[0] * p1[1];
        }
        return Math.abs(area) / 2;
    }

    private double[] lineIntersect(double[] p1, double[] p2, double[] p3, double[] p4) {
        double x1 = p1[0], y1 = p1[1];
        double x2 = p2[0], y2 = p2[1];
        double x3 = p3[0], y3 = p3[1];
        double x4 = p4[0], y4 = p4[1];

        double denominator = ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));

        if (denominator == 0) // Parallel lines
            return null;

        double xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denominator;
        double yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denominator;

        if (xi < Math.min(x1, x2) || xi > Math.max(x1, x2) || xi < Math.min(x3, x4) || xi > Math.max(x3, x4))
            return null;
        if (yi < Math.min(y1, y2) || yi > Math.max(y1, y2) || yi < Math.min(y3, y4) || yi > Math.max(y3, y4))
            return null;

        return new double[]{xi, yi};
    }

    private double[] calculateCentroid(List<double[]> points) {
        double sumX = 0;
        double sumY = 0;
        for (double[] point : points) {
            sumX += point[0];
            sumY += point[1];
        }
        return new double[]{sumX / points.size(), sumY / points.size()};
    }

    private double calculatePolygonArea(double[][] polygon) {
        // Calculate the area of a polygon using the shoelace formula
        double area = 0;
        int n = polygon.length;
        for (int i = 0; i < n; i++) {
            double[] p1 = polygon[i];
            double[] p2 = polygon[(i + 1) % n];
            area += p1[0] * p2[1] - p2[0] * p1[1];
        }
        return Math.abs(area) / 2;
    }

    public boolean isPolygonContained(double[][] poly1, double[][] poly2) {
        for (double[] point : poly1) {
            if (!pointInPolygon(point, poly2)) {
                return false; // At least one point of poly1 is outside poly2
            }
        }
        return true; // All points of poly1 are inside poly2
    }

    private boolean pointInPolygon(double[] point, double[][] polygon) {
        int intersectCount = 0;
        double[] p1, p2;
        p1 = polygon[0];
        for (int i = 1; i <= polygon.length; i++) {
            p2 = polygon[i % polygon.length];
            if (point[1] > Math.min(p1[1], p2[1])) {
                if (point[1] <= Math.max(p1[1], p2[1])) {
                    if (point[0] <= Math.max(p1[0], p2[0])) {
                        if (p1[1] != p2[1]) {
                            double xinters = (point[1] - p1[1]) * (p2[0] - p1[0]) / (p2[1] - p1[1]) + p1[0];
                            if (p1[0] == p2[0] || point[0] <= xinters) {
                                intersectCount++;
                            }
                        }
                    }
                }
            }
            p1 = p2;
        }
        return intersectCount % 2 != 0;
    }

    public double distanceToPolygonBorder(double pointX, double pointY, int polyId) {
        double minDistance = Double.MAX_VALUE;
        int numPoints = bounds.get(polyId).length;

        for (int i = 0; i < numPoints; i++) {
            double[] currentPoint = bounds.get(polyId)[i];
            double[] nextPoint = bounds.get(polyId)[(i + 1) % numPoints];

            double distance = distanceToSegment(pointX, pointY, currentPoint[0], currentPoint[1], nextPoint[0], nextPoint[1]);
            minDistance = Math.min(minDistance, distance);
        }

        return minDistance;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
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

    public void declarePlotShapefile(String outputName){

        gdal.AllRegister();

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        String pathSeparator = System.getProperty("file.separator");

        //File output_directory = new File(aR.odir + pathSeparator);

        File outFile = aR._createOutputFile_(outputName);
        //File outFile = new File(output_directory.getAbsolutePath() + pathSeparator + outputName);
        DataSource outShp = shpDriver.CreateDataSource(outFile.getAbsolutePath());

        layer = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id", 4);

        layer.CreateField(layerFieldDef);

        FieldDefn layerFieldDef1 = new FieldDefn("center_x", 0);
        layer.CreateField(layerFieldDef1);

        FieldDefn layerFieldDef2 = new FieldDefn("center_y", 0);
        layer.CreateField(layerFieldDef2);

    }

    public List<double[]> calculateCircleBoundary(double centerX, double centerY, double radius) {
        List<double[]> boundaryPoints = new ArrayList<>();
        int numPoints = 20; // Number of points to approximate the circle

        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            boundaryPoints.add(new double[]{x, y});
        }

        return boundaryPoints;
    }

    public void addCircleToShapefile(double x, double y, double r,  String id){

        try {
            gdal.AllRegister();
            List<double[]> circlePoints = calculateCircleBoundary(x, y, r);

            Geometry outShpGeom2 = new Geometry(ogr.wkbLinearRing);
            Geometry outShpGeom = new Geometry(ogr.wkbPolygon);

            for (int i = 0; i < circlePoints.size(); i++) {

                //System.out.println("Wrote " + i);
                outShpGeom2.AddPoint_2D(circlePoints.get(i)[0], circlePoints.get(i)[1]);

            }

            if(circlePoints.size() == 0){
                System.out.println("Circle points size is zero");
                System.exit(1);
            }

            outShpGeom2.AddPoint_2D(circlePoints.get(0)[0], circlePoints.get(0)[1]);

            outShpGeom.AddGeometry(outShpGeom2);

            FeatureDefn outShpFeatDefn = layer.GetLayerDefn();
            Feature feature = new Feature(outShpFeatDefn);
            feature.SetGeometryDirectly(outShpGeom);

            //Feature feature = new Feature(allPlotsShapeFileLayer.GetLayerDefn());
            //feature.SetGeometry(outShpGeom);

            // Set feature attributes

            feature.SetField("id", id);
            feature.SetField("center_x", x);
            feature.SetField("center_y", y);


            SpatialReference spatialRef = new SpatialReference();
            spatialRef.ImportFromEPSG(3067);
            feature.GetGeometryRef().AssignSpatialReference(spatialRef);

            layer.CreateFeature(feature);

        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void finalizeOutputLayer(){
        layer.SyncToDisk();
    }

}
