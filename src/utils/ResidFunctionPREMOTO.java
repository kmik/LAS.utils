package utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.ejml.data.DMatrixRMaj;
import org.jblas.DoubleMatrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.index.strtree.GeometryItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;

public class ResidFunctionPREMOTO implements LevenbergMarquardt.ResidualFunction  {
    short[][] numberOfTrees = null;

    int onlyModifyThis = -1;
    double resolution = 0.5;
    double penaltyP0 = 500;
    double penaltyP1 = 100;
    double penaltyP2 = 10;
    double[] boomDistanceProbabilities = new double[]{0.05, 0.15, 0.4, 0.3};
    double[] boomDistanceRanges = new double[]{3.0, 6.0, 8.0, 10.0};

    HashMap<Integer, double[][]> standBoundaries_ = new HashMap<>();

    double[] previousCosts = null;
    double maxBoomAngle = 125;

    public boolean rejectSolution = false;
    List<KdTree.XYZPoint> points = null;
    List<Coordinate> points_ = new ArrayList<>();

    KdTree kdTree = null;
    //STRtree rTree = new STRtree();

    double[] geotransform = null;
    double maxDistance = 10.0;
    double minDistance = 1.0;

    double[] residuals = new double[0];

    float[][] chm = null;
    List<Tree> trees = new ArrayList<>();
    int numFunctions = 1;

    int numParamPerTree = 2;



    public ResidFunctionPREMOTO(){

    }

    public void setNumFunctions(int in){

        this.numFunctions = in;

    }

    public double getSomething(){

            return 1.0;
    }

    public double[] translatePoint(double initialX, double initialY, double distance, double angleInDegrees) {
        // Convert the angle from degrees to radians
        double angleInRadians = Math.toRadians(angleInDegrees);

        // Calculate the new coordinates
        double translatedX = initialX + distance * Math.cos(angleInRadians);
        double translatedY = initialY + distance * Math.sin(angleInRadians);

        // Create and return the new point
        return new double[] { translatedX, translatedY };
    }

    public void applyToTrees(DMatrixRMaj param){

        numberOfTrees = new short[chm.length][chm[0].length];

        for(int i = 0; i < param.data.length; i++){

            double angle = trees.get(i).getMachineBearing() + param.data[i];

            //translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);
            double[] translatedCoordinates = translatePointToClosestCHMCell_(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), 0.0, angle, trees.get(i).getHeight(), trees.get(i));


        }

    }

    public double[] translatePointToClosestCHMCell(double initialX, double initialY, double distance, double angleInDegrees, double treeHeight, int standId) {
        // Convert the angle from degrees to radians

        double minDifferenceToChm = Double.POSITIVE_INFINITY;
        double maxDistanceToOtherTree = 0.0;

        double[] output = new double[2];
        boolean solutionFound = false;
        //int distanceInPixels = (int) Math.round(2.0 / resolution);
        int distanceInPixels = (int) 1;
        int[] coordinates = new int[2];

        while(!solutionFound) {

            for (double distance_ = 1.0; distance_ < 10.0; distance_ += 0.5) {

                double angleInRadians = Math.toRadians(angleInDegrees);

                // Calculate the new coordinates
                double translatedX = initialX + distance_ * Math.cos(angleInRadians);
                double translatedY = initialY + distance_ * Math.sin(angleInRadians);

                int x = (int) Math.round((translatedX - geotransform[0]) / geotransform[1]);
                int y = (int) Math.round((translatedY - geotransform[3]) / geotransform[5]);

                double chmValue = chm[x][y];

                if(chmValue <= 5){
                    continue;
                }

                double differenceToChm = Math.abs(chmValue - treeHeight);

                boolean insideStand = pointInPolygon(standBoundaries_.get(standId), translatedX, translatedY);

                if(!insideStand){
                    continue;
                }

                boolean reject = rejectBasedOnProximityOfNeighborTree(x, y, distanceInPixels);

                if (reject)
                    continue;


                if (differenceToChm < minDifferenceToChm) {
                    //System.out.println("GOT HERE");
                    minDifferenceToChm = differenceToChm;
                    output[0] = translatedX;
                    output[1] = translatedY;

                    coordinates[0] = x;
                    coordinates[1] = y;

                    solutionFound = true;
                }

            }

            if(!solutionFound) {
                //System.out.println("no solution");
                //distanceInPixels--;
                break;
            }

            if(distanceInPixels <= 0){
                break;
            }
        }


        if(!solutionFound){
            //System.out.println("no solution");
            output[0] = initialX;
            output[1] = initialY;

            int x = (int) Math.round((initialX - geotransform[0]) / geotransform[1]);
            int y = (int) Math.round((initialY - geotransform[3]) / geotransform[5]);

            coordinates[0] = x;
            coordinates[1] = y;

        }else{
            maskTree(coordinates[0], coordinates[1], distanceInPixels);
        }

        ///System.out.println("#########################");

        // Create and return the new point
        return output;
    }

    public double[] translatePointToClosestCHMCell_(double initialX, double initialY, double distance, double angleInDegrees, double treeHeight, Tree tree) {
        // Convert the angle from degrees to radians

        double minDifferenceToChm = Double.POSITIVE_INFINITY;
        double maxDistanceToOtherTree = 0.0;

        double[] output = new double[2];
        boolean solutionFound = false;
        //int distanceInPixels = (int) Math.round(2.0 / resolution);
        int distanceInPixels = (int) 1;
        double solutionDistance = 0.0;

        int[] coordinates = new int[2];

        while(!solutionFound) {

            for (double distance_ = 2.0; distance_ < 10.0; distance_ += 0.5) {

                double angleInRadians = Math.toRadians(angleInDegrees);

                // Calculate the new coordinates
                double translatedX = initialX + distance_ * Math.cos(angleInRadians);
                double translatedY = initialY + distance_ * Math.sin(angleInRadians);

                int x = (int) Math.round((translatedX - geotransform[0]) / geotransform[1]);
                int y = (int) Math.round((translatedY - geotransform[3]) / geotransform[5]);

                double chmValue = chm[x][y];

                if(chmValue <= 3){
                    continue;
                }

                boolean insideStand = pointInPolygon(standBoundaries_.get(tree.standId), translatedX, translatedY);

                if(!insideStand){
                    continue;
                }

                double differenceToChm = Math.abs(chmValue - treeHeight);

                boolean reject = rejectBasedOnProximityOfNeighborTree(x, y, distanceInPixels);

                if (reject)
                    continue;

                if (differenceToChm < minDifferenceToChm) {
                    minDifferenceToChm = differenceToChm;
                    output[0] = translatedX;
                    output[1] = translatedY;

                    coordinates[0] = x;
                    coordinates[1] = y;

                    solutionDistance = distance_;
                    solutionFound = true;
                }

            }

            if(!solutionFound)
                distanceInPixels--;

            if(distanceInPixels <= 0){
                break;
            }
        }

        if(!solutionFound){
            //System.out.println("no solution");
            output[0] = initialX;
            output[1] = initialY;

            tree.setX_coordinate_estimated(output[0]);
            tree.setY_coordinate_estimated(output[1]);
            tree.setBoomExtension(0.0);

            int x = (int) Math.round((initialX - geotransform[0]) / geotransform[1]);
            int y = (int) Math.round((initialY - geotransform[3]) / geotransform[5]);

            coordinates[0] = x;
            coordinates[1] = y;


        }else{
            maskTree(coordinates[0], coordinates[1], distanceInPixels);
            tree.setX_coordinate_estimated(output[0]);
            tree.setY_coordinate_estimated(output[1]);
            tree.setBoomExtension(solutionDistance);
        }

        // Create and return the new point
        return output;
    }
    public boolean rejectBasedOnProximityOfNeighborTree(int x, int y, int distanceInPixels){

        distanceInPixels = 1;
        boolean reject = false;

        //System.out.println(distanceInPixels);

        int xMin = x - distanceInPixels;
        int xMax = x + distanceInPixels;
        int yMin = y - distanceInPixels;
        int yMax = y + distanceInPixels;

        if(xMin < 0){
            xMin = 0;
        }

        if(xMax > chm.length){
            xMax = chm.length;
        }

        if(yMin < 0){
            yMin = 0;
        }

        if(yMax > chm[0].length){
            yMax = chm[0].length;
        }

        for(int x_ = xMin; x_ < xMax; x_++){
            for(int y_ = yMin; y_ < yMax; y_++){

                if(numberOfTrees[x_][y_] > 0){
                    return true;
                }

            }
        }

        return false;

    }

    public void maskTree(int x, int y, int distanceInPixels){

        //int distanceInPixels = (int) Math.round(2.0 / resolution);

        int xMin = x - distanceInPixels;
        int xMax = x + distanceInPixels;
        int yMin = y - distanceInPixels;
        int yMax = y + distanceInPixels;

        if(xMin < 0){
            xMin = 0;
        }

        if(xMax > chm.length){
            xMax = chm.length;
        }

        if(yMin < 0){
            yMin = 0;
        }

        if(yMax > chm[0].length){
            yMax = chm[0].length;
        }

        for(int x_ = xMin; x_ < xMax; x_++){
            for(int y_ = yMin; y_ < yMax; y_++){

                numberOfTrees[x_][y_]++;

            }
        }

    }

    public void compute(DMatrixRMaj param, DMatrixRMaj residual){

        if(residuals.length == 0){
            this.residuals = new double[residual.data.length];
        }

        rejectSolution = false;

        double[] translatedCoordinates = new double[2];
        STRtree rTree = new STRtree();
        //ArrayList<double[]> translatedCoordinatesList = new ArrayList<>();
        double meanDistance = 0.0;
        double meanAngle = 0.0;
        List<Coordinate> nearestNeighbors = null;
        GeometryItemDistance distance_ = new GeometryItemDistance();

        int distanceInPixels = (int) Math.round(2.0 / resolution);


        if(onlyModifyThis != -1){


            for(int i_ = 0; i_ < this.residuals.length; i_++){
                residual.data[i_] = this.residuals[i_];
            }

            int i = (int)(onlyModifyThis / 2.0);

            int standId = trees.get(i).standId;

            double cost = 0.0;

            double distance = param.data[i * 2 + 0];
            double angle = trees.get(i).getMachineBearing() + param.data[i * 2 + 1];

            meanAngle += angle;
            meanDistance += distance;

            translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);
            double distFromOriginal = distance2d(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), translatedCoordinates[0], translatedCoordinates[1]);

            if (distFromOriginal > 10.0 || distFromOriginal < 1.0) {
                cost += penaltyP1;
            }

            boolean insideStand = pointInPolygon(standBoundaries_.get(standId), translatedCoordinates);

            if (!insideStand) {
                cost += penaltyP0;
            }

            //System.out.println(distance + " " + angle + " " + cost);
            int x = (int) Math.round((translatedCoordinates[0] - geotransform[0]) / geotransform[1]);
            int y = (int) Math.round((translatedCoordinates[1] - geotransform[3]) / geotransform[5]);

            int n_trees = 0;

            for (int x_ = x - distanceInPixels; x_ < x + distanceInPixels; x_++) {
                for (int y_ = y - distanceInPixels; y_ < y + distanceInPixels; y_++) {

                    if (x_ < 0 || x_ >= chm[0].length || y_ < 0 || y_ >= chm.length) {
                        continue;
                    }

                    if (numberOfTrees[y_][x_] > 0) {
                        n_trees += 1;
                    }

                    numberOfTrees[y_][x_] += 1;

                }
            }

            if (n_trees > 0) {
                cost += penaltyP2;
            }

            float chmHeight = chm[x][y];
            float treeHeight = trees.get(i).getHeight();
            float diff = chmHeight - treeHeight;

            residual.data[i] = diff + cost;

        }else {


            numberOfTrees = new short[chm.length][chm[0].length];

            for (int i = 0; i < this.trees.size(); i++) {

                int standId = trees.get(i).standId;

                double cost = 0.0;

                double distance = param.data[i * 2 + 0];
                double angle = trees.get(i).getMachineBearing() + param.data[i * 2 + 1];

                //  System.out.println("Distance: " + distance + " Angle: " + angle);

                meanAngle += angle;
                meanDistance += distance;

                translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);


                //Coordinate c = new Coordinate(translatedCoordinates[0], translatedCoordinates[1]);


                //Envelope env = new Envelope(c);

                //nearestNeighbors = (List<Coordinate>) rTree.nearestNeighbour(env, c, distance_);

                //rTree.insert(env, c);

                double distFromOriginal = distance2d(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), translatedCoordinates[0], translatedCoordinates[1]);

                if (distFromOriginal > 10.0 || distFromOriginal < 1.0) {
                    cost += penaltyP1;
                }

                boolean insideStand = pointInPolygon(standBoundaries_.get(standId), translatedCoordinates);

                if (!insideStand) {
                    cost += penaltyP0;
                }

                //System.out.println(distance + " " + angle + " " + cost);
                int x = (int) Math.round((translatedCoordinates[0] - geotransform[0]) / geotransform[1]);
                int y = (int) Math.round((translatedCoordinates[1] - geotransform[3]) / geotransform[5]);

                int n_trees = 0;

                for (int x_ = x - distanceInPixels; x_ < x + distanceInPixels; x_++) {
                    for (int y_ = y - distanceInPixels; y_ < y + distanceInPixels; y_++) {

                        if (x_ < 0 || x_ >= chm[0].length || y_ < 0 || y_ >= chm.length) {
                            continue;
                        }

                        if (numberOfTrees[y_][x_] > 0) {
                            n_trees += 1;
                        }

                        numberOfTrees[y_][x_] += 1;

                    }
                }

                if (n_trees > 0) {
                    cost += penaltyP2;
                }

                float chmHeight = chm[x][y];
                float treeHeight = trees.get(i).getHeight();
                float diff = Math.abs(chmHeight - treeHeight);

                residual.data[i] = diff + cost;

                //points.get(i).x = translatedCoordinates[0];
                //points.get(i).y = translatedCoordinates[1];
                this.residuals[i] = residual.data[i];

            }
        }
/*
        List<KdTree.XYZPoint> nearest = new ArrayList<>();

        if(false)
        for(KdTree.XYZPoint p : points){

            nearest =  (List<KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(2, p);
            double distance = distance(p.x, p.y, p.z, nearest.get(1).x, nearest.get(1).y, nearest.get(1).z);

            double deltah = Math.abs(trees.get(p.getIndex()).getHeight() - trees.get(nearest.get(0).getIndex()).getHeight());
            double highest = Math.max(trees.get(p.getIndex()).getHeight(), trees.get(nearest.get(0).getIndex()).getHeight());

            double val = Math.max(1, 2.5 - (highest * 0.5) - (deltah * 0.1));

            if(distance < val){
                residual.data[p.getIndex()] += penaltyP2 * (val - distance);
                //rejectSolution = true;
            }
        }

        for(int i = 0; i < this.points.size(); i++){

            this.points.get(i).setX(this.trees.get(i).x_coordinate_machine);
            this.points.get(i).setY(this.trees.get(i).y_coordinate_machine);
            this.points.get(i).setZ(0);

        }
*/
        double meanCost = 0;

        for(int i = 0; i < residual.data.length; i++){

                meanCost += residual.data[i];

        }

        meanCost = meanCost / residual.data.length;
        onlyModifyThis = -1;
        //System.out.println("DONE COMPUTE!");
        //System.out.println("done " + meanCost + " meanDistance: " + meanDistance / residual.data.length + " meanAngle: " + meanAngle / residual.data.length);

    }

    public void compute2(DMatrixRMaj param, DMatrixRMaj residual){

        if(residuals.length == 0){
            this.residuals = new double[residual.data.length];
        }

        rejectSolution = false;

        double[] translatedCoordinates = new double[2];
        STRtree rTree = new STRtree();
        //ArrayList<double[]> translatedCoordinatesList = new ArrayList<>();
        double meanDistance = 0.0;
        double meanAngle = 0.0;
        List<Coordinate> nearestNeighbors = null;
        GeometryItemDistance distance_ = new GeometryItemDistance();

        int distanceInPixels = (int) Math.round(2.0 / resolution);

            numberOfTrees = new short[chm.length][chm[0].length];


            for (int i = 0; i < this.trees.size(); i++) {

                int standId = trees.get(i).standId;

                double cost = 0.0;

                //double distance = param.data[i * 2 + 0];
                double angle = trees.get(i).getMachineBearing() + param.data[i];

                //  System.out.println("Distance: " + distance + " Angle: " + angle);

                meanAngle += angle;
                //meanDistance += distance;


                //translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);
                translatedCoordinates = translatePointToClosestCHMCell(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), 0.0, angle, trees.get(i).getHeight(),
                        standId);

                //System.out.println("coordinates before: " + trees.get(i).getX_coordinate_machine() + " " + trees.get(i).getY_coordinate_machine());
                //System.out.println("coordinates after: " + translatedCoordinates[0] + " " + translatedCoordinates[1]);
                //System.out.println("-----------------");

                //Coordinate c = new Coordinate(translatedCoordinates[0], translatedCoordinates[1]);


                //Envelope env = new Envelope(c);

                //nearestNeighbors = (List<Coordinate>) rTree.nearestNeighbour(env, c, distance_);

                //rTree.insert(env, c);

                double distFromOriginal = distance2d(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), translatedCoordinates[0], translatedCoordinates[1]);

                if(false)
                if (distFromOriginal > 10.0 || distFromOriginal < 1.0) {
                    cost += penaltyP1;
                }

                boolean insideStand = pointInPolygon(standBoundaries_.get(standId), translatedCoordinates);

                if (!insideStand) {
                    cost += penaltyP0;
                }

                //System.out.println(distance + " " + angle + " " + cost);
                int x = (int) Math.round((translatedCoordinates[0] - geotransform[0]) / geotransform[1]);
                int y = (int) Math.round((translatedCoordinates[1] - geotransform[3]) / geotransform[5]);

                int n_trees = 0;

                for (int x_ = x - distanceInPixels; x_ < x + distanceInPixels; x_++) {
                    for (int y_ = y - distanceInPixels; y_ < y + distanceInPixels; y_++) {

                        if (x_ < 0 || x_ >= chm[0].length || y_ < 0 || y_ >= chm.length) {
                            continue;
                        }

                        if (numberOfTrees[y_][x_] > 0) {
                            n_trees += 1;
                        }

                        numberOfTrees[y_][x_] += 1;

                    }
                }

                if(false)
                if (n_trees > 0) {
                    cost += penaltyP2;
                }

                float chmHeight = chm[x][y];
                float treeHeight = trees.get(i).getHeight();
                float diff = Math.abs(chmHeight - treeHeight);

                residual.data[i] = diff + cost;

                //points.get(i).x = translatedCoordinates[0];
                //points.get(i).y = translatedCoordinates[1];
                this.residuals[i] = residual.data[i];

            }

/*
        List<KdTree.XYZPoint> nearest = new ArrayList<>();

        if(false)
        for(KdTree.XYZPoint p : points){

            nearest =  (List<KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(2, p);
            double distance = distance(p.x, p.y, p.z, nearest.get(1).x, nearest.get(1).y, nearest.get(1).z);

            double deltah = Math.abs(trees.get(p.getIndex()).getHeight() - trees.get(nearest.get(0).getIndex()).getHeight());
            double highest = Math.max(trees.get(p.getIndex()).getHeight(), trees.get(nearest.get(0).getIndex()).getHeight());

            double val = Math.max(1, 2.5 - (highest * 0.5) - (deltah * 0.1));

            if(distance < val){
                residual.data[p.getIndex()] += penaltyP2 * (val - distance);
                //rejectSolution = true;
            }
        }

        for(int i = 0; i < this.points.size(); i++){

            this.points.get(i).setX(this.trees.get(i).x_coordinate_machine);
            this.points.get(i).setY(this.trees.get(i).y_coordinate_machine);
            this.points.get(i).setZ(0);

        }
*/
        double meanCost = 0;

        for(int i = 0; i < residual.data.length; i++){

            meanCost += residual.data[i];

        }

        meanCost = meanCost / residual.data.length;
        onlyModifyThis = -1;
        //System.out.println("DONE COMPUTE!");
        //System.out.println("done " + meanCost + " meanDistance: " + meanDistance / residual.data.length + " meanAngle: " + meanAngle / residual.data.length);

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


    double distance(double x1, double y1, double z1, double x2, double y2, double z2){

        return Math.sqrt( (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2) );

    }

    double distance2d(double x1, double y1, double x2, double y2){

        return Math.sqrt( (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));

    }


    public void setup() throws IOException{



    }

    public void setPoints(ArrayList<double[]> points){

    }

    public void setChm(float[][] chm){

        this.chm = chm;

    }

    public void setTrees(List<Tree> trees){

        this.trees = trees;
        this.numFunctions = trees.size();
        this.previousCosts = new double[numFunctions];
        this.points = new ArrayList<>(trees.size());
        //rTree = new STRtree();

        for(int i = 0; i < trees.size(); i++){

                double x = trees.get(i).getX_coordinate_machine();
                double y = trees.get(i).getY_coordinate_machine();
                double z = 0;

                points.add(new KdTree.XYZPoint(x, y, z, i));
                points_.add(new Coordinate(x, y));

               // rTree.insert(points_.get(points_.size()-1).getEnvelopeInternal(), point);
        }

        kdTree = new KdTree(points);

    }

    public double generateRandomDistance(double[] DISTANCE_WEIGHTS, double[] DISTANCE_RANGES) {
        //Random random = new Random();

        double randomWeight = Math.random();
        double cumulativeWeight = 0.0;
        int selectedRangeIndex = 0;

        for (int i = 0; i < DISTANCE_WEIGHTS.length; i++) {
            cumulativeWeight += DISTANCE_WEIGHTS[i];
            if (randomWeight <= cumulativeWeight) {
                selectedRangeIndex = i;
                break;
            }
        }

        double selectedRange = DISTANCE_RANGES[selectedRangeIndex];

        double randomDistance = 0;

        if(selectedRangeIndex == 0){
            randomDistance = Math.random() * selectedRange;
        }else{
            randomDistance = randomDouble(DISTANCE_RANGES[selectedRangeIndex - 1], selectedRange);
        }

        return randomDistance;
    }

    public double randomDouble(double min, double max){
        return min + (max - min) * Math.random();
    }

    public void setGeotransform(double[] geotransform){

        this.geotransform = geotransform;
        this.setResolution(geotransform[1]);
    }

    public void setResolution(double resolution){

        this.resolution = resolution;

    }

    public void setStandBoundaries(HashMap<Integer, double[][]> standBoundaries_){

        this.standBoundaries_ = standBoundaries_;

    }

    /**
     * Number of functions in output
     * @return function count
     */
    public int numFunctions(){

        return this.numFunctions;
    }

    public boolean rejectSolution(){

        return this.rejectSolution;
    }

    public void onlyModifyThis(int i){

        this.onlyModifyThis = i;
    }
}
