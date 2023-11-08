package tools;

import org.ejml.data.DMatrixRMaj;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import utils.*;

import java.io.File;
import java.util.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.index.strtree.GeometryItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;

public class treeLocationEstimator {

    HashMap<Integer, Polygon> standPolygons = null;
    Random random = new Random();
    rasterCollection rasters = new rasterCollection(1);
    float[][] auxData = null;

    double[] extent = null;

    double extentBuffer = 15;

    Dataset tifDataset = null;
    Band tifBand = null;

    File auxiliaryDataFile = null;

    double[] geoTransform = null;
    argumentReader aR;

    ArrayList<Tree> trees = new ArrayList<Tree>();
    ArrayList<Tree> trees_sorted = new ArrayList<Tree>();

    HashMap<Integer, ArrayList<ConcaveHull.Point>> standBoundaries = new HashMap<>();
    HashMap<Integer, double[][]> standBoundaries_ = new HashMap<>();

    public boolean noAuxDataAvailable = false;

    public treeLocationEstimator(){

    }

    public treeLocationEstimator(argumentReader aR){

        this.aR = aR;

    }

    public void setSeed(int seed){

        random.setSeed(seed);

    }

    public void setTrees(ArrayList<Tree> trees){
        this.trees = trees;
    }

    public void sortTreesByHeight(){
        Tree[] trees_ = new Tree[trees.size()];
        for(int i = 0; i < trees.size(); i++){
            trees_[i] = trees.get(i);
        }
        Arrays.sort(trees_, new treeHeightComparator());
        //trees.clear();
        for(int i = 0; i < trees_.length; i++){
            trees_sorted.add(trees_[i]);
        }
    }

    public void setStandPolygons(HashMap<Integer, Polygon> standPolygons){

        this.standPolygons = standPolygons;

    }



    public void setStandBoundaries(HashMap<Integer, ArrayList<ConcaveHull.Point>> standBoundaries){

        double minx = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;

        for(int i : standBoundaries.keySet()){
            ArrayList<ConcaveHull.Point> temp = standBoundaries.get(i);
            double[][] temp_ = new double[temp.size()][2];



            for(int j = 0; j < temp.size(); j++){
                temp_[j][0] = temp.get(j).x;
                temp_[j][1] = temp.get(j).y;

                if(temp.get(j).x < minx)
                    minx = temp.get(j).x;
                if(temp.get(j).x > maxx)
                    maxx = temp.get(j).x;
                if(temp.get(j).y < miny)
                    miny = temp.get(j).y;
                if(temp.get(j).y > maxy)
                    maxy = temp.get(j).y;

            }
            standBoundaries_.put(i, temp_);
        }

        this.standBoundaries = standBoundaries;

        this.extent = new double[]{minx - extentBuffer, maxx + extentBuffer, miny - extentBuffer, maxy + extentBuffer};

    }


    public void noEstimation(){
        for(int i = 0; i < trees.size(); i++){
            trees.get(i).setX_coordinate_estimated(trees.get(i).getX_coordinate_machine());
            trees.get(i).setY_coordinate_estimated(trees.get(i).getY_coordinate_machine());
            trees.get(i).setZ_coordinate_estimated(trees.get(i).getZ_coordinate_machine());
        }
    }

    public void setRasters(rasterCollection rasters){
        this.rasters = rasters;
    }

    public void simpleEstimation(double maxBoomDistance, double maxBoomAngle, double minDistanceBetweenTrees){

        KdTree kdTree = new KdTree();

        KdTree.XYZPoint point = new KdTree.XYZPoint(0,0,0);
        List<KdTree.XYZPoint> nearest = new ArrayList<KdTree.XYZPoint>();

        double maxBoomAngleOriginal = maxBoomAngle;
        double minDistanceBetweenTreesOriginal = minDistanceBetweenTrees;

        boolean notFirst = false;

        int nTriesBeforeLowerStandards = 100;
        boolean imposibleHole = false;


        double lowerDistanceBy = 0.25;
        double lowerDistanceBy_ = 0.0;

        boolean distanceCondition = false;
        int counter = 0;
        double deltah = 0.5;
        double highest = 0;

        for(int i = 0; i < trees.size(); i++){



            maxBoomAngle = maxBoomAngleOriginal;
            minDistanceBetweenTrees = minDistanceBetweenTreesOriginal;
            lowerDistanceBy_ = 0.0;

            if(!trees.get(i).trulyInStand)
                continue;


            int standId = trees.get(i).standId;
            double distance = randomDouble(0, maxBoomDistance);
            double angle = randomDouble(-maxBoomAngle/2.0, maxBoomAngle/2.0);

            //double[] debug = translatePoint(0,0, 1.414214, 45);

            //System.out.println(Arrays.toString(debug));
            //System.exit(1);
            double[] translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);

            double distanceToNearestTree = Double.NEGATIVE_INFINITY;
            boolean insideStand = false;

            if(notFirst){

                distanceCondition = false;
                int nTries = 0;
                int nFailedBecauseImpossibleHole = 0;

                //while(distanceToNearestTree <= minDistanceBetweenTrees || !insideStand){
                while(!distanceCondition || !insideStand){

                    distance = randomDouble(3, maxBoomDistance);
                    angle = trees.get(i).getMachineBearing() + randomDouble(-maxBoomAngle/2.0, maxBoomAngle/2.0);

                    translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);

                    point.setX(translatedCoordinates[0]);
                    point.setY(translatedCoordinates[1]);

                    if(!standBoundaries_.containsKey(standId)) {
                        System.out.println("trees: " + this.trees.size());
                        System.out.println("QUERY: " + standId);
                    }

                    insideStand = pointInPolygon(standBoundaries_.get(standId), translatedCoordinates);

                    boolean insideHole = false;

                    if(!imposibleHole)
                        if(standPolygons.get(standId).hasHole()){
                            for(int i_ = 0; i_ < standPolygons.get(standId).holes.size(); i_++){

                                if(standPolygons.get(standId).pointInPolygon(standPolygons.get(standId).holes.get(i_), translatedCoordinates)){
                                    insideStand = false;
                                    insideHole = true;
                                    nFailedBecauseImpossibleHole++;
                                    break;
                                }
                            }
                        }

                    //nearest = (List< KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(1, point);

                    nearest = (List< KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(1, point);

                    if(counter > 0) {
                        nearest = (List<KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(1, point);

                        distanceToNearestTree = euclideanDistance2d(translatedCoordinates[0], translatedCoordinates[1], nearest.get(0).getX(), nearest.get(0).getY());

                        deltah = Math.abs(trees.get(i).getHeight() - trees.get(nearest.get(0).getIndex()).getHeight());
                        highest = Math.max(trees.get(i).getHeight(), trees.get(nearest.get(0).getIndex()).getHeight());


                    }else{
                        distanceCondition = true;
                    }

                    //distanceToNearestTree = euclideanDistance2d(translatedCoordinates[0], translatedCoordinates[1], nearest.get(0).getX(), nearest.get(0).getY());
                    if(aR.PREMOTO_ADAPTIVEDISTANCE) {
                        if ((distanceToNearestTree > ((Math.max(0.75, highest * 0.1 - deltah * 0.1)) - lowerDistanceBy_))) {
                            //if(distanceToNearestTree > (minDistanceBetweenTrees - lowerDistanceBy_)){
                            distanceCondition = true;
                        }
                    }else{
                        //if ((distanceToNearestTree > ((Math.max(0.75, highest * 0.1 - deltah * 0.1)) - lowerDistanceBy_))) {
                        if(distanceToNearestTree > (minDistanceBetweenTrees - lowerDistanceBy_)){
                            distanceCondition = true;
                        }
                    }

                    nTries++;

                    if(nTries % nTriesBeforeLowerStandards == 0){

                        if((double)nFailedBecauseImpossibleHole / (double)nTriesBeforeLowerStandards > 0.75) {
                            imposibleHole = true;
                        }
                        nFailedBecauseImpossibleHole = 0;
                        maxBoomAngle += 50;
                        lowerDistanceBy_ += lowerDistanceBy;
                        //maxBoomDistance += 0.5;
                        minDistanceBetweenTrees -= 0.25;
                    }
                }

                //System.out.println(distanceToNearestTree + " " + insideStand);

                trees.get(i).setX_coordinate_estimated(translatedCoordinates[0]);
                trees.get(i).setY_coordinate_estimated(translatedCoordinates[1]);
                trees.get(i).setZ_coordinate_estimated(trees.get(i).getZ_coordinate_machine());
                trees.get(i).setBoomAngle(angle);
                trees.get(i).setBoomExtension(distance);
                kdTree.add(trees.get(i).toXYZPoint_estimatedCoords());

            }else{

                    trees.get(i).setX_coordinate_estimated(translatedCoordinates[0]);
                    trees.get(i).setY_coordinate_estimated(translatedCoordinates[1]);
                    trees.get(i).setZ_coordinate_estimated(trees.get(i).getZ_coordinate_machine());
                    trees.get(i).setBoomAngle(angle);
                    trees.get(i).setBoomExtension(distance);

                    kdTree.add(trees.get(i).toXYZPoint_estimatedCoords());
            }

            //System.out.println(i);
            notFirst = true;
            counter++;
        }
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


    public double euclideanDistance2d(double x1, double y1, double x2, double y2){
        return Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

    public void simpleEstimationWithProbabilities(double maxBoomDistance, double maxBoomAngle, double minDistanceBetweenTrees,
                                                  double[] boomDistanceProbabilities, double[] boomDistanceRanges){

        KdTree kdTree = new KdTree();

        double[] boomDistanceProbabilities_ = boomDistanceProbabilities.clone();
        double[] boomDistanceRanges_ = boomDistanceRanges.clone();

        KdTree.XYZPoint point = new KdTree.XYZPoint(0,0,0);
        List<KdTree.XYZPoint> nearest = new ArrayList<KdTree.XYZPoint>();
        int counter = 0;
        int nTriesBeforeLowerStandards = 100;

        double maxBoomAngleOriginal = maxBoomAngle;
        double minDistanceBetweenTreesOriginal = minDistanceBetweenTrees;
        boolean imposibleHole = false;

        double lowerDistanceBy = 0.25;
        double lowerDistanceBy_ = 0.0;

        boolean distanceCondition = false;

        double deltah = 0.5;
        double highest = 0;

        for(int i = 0; i < trees.size(); i++){

            maxBoomAngle = maxBoomAngleOriginal;
            minDistanceBetweenTrees = minDistanceBetweenTreesOriginal;
            lowerDistanceBy_ = 0.0;

            if(!trees.get(i).trulyInStand)
                continue;

            int standId = trees.get(i).standId;
            double distance = randomDouble(0, maxBoomDistance);
            double angle = randomDouble(-maxBoomAngle/2.0, maxBoomAngle/2.0);

            //double[] debug = translatePoint(0,0, 1.414214, 45);

            //System.out.println(Arrays.toString(debug));
            //System.exit(1);
            double[] translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);

            double distanceToNearestTree = Double.NEGATIVE_INFINITY;
            boolean insideStand = false;
            boolean startPrinting = false;

            if(counter > 0){

                int nTries = 0;

                int nFailedBecauseImpossibleHole = 0;

                distanceCondition = false;

                if(trees.get(i).id == 811){
                    //System.out.println("debug " + trees.get(i).boomAngle);
                }
                while(!distanceCondition || !insideStand){

                    distance = generateRandomDistance(boomDistanceProbabilities, boomDistanceRanges);
                    angle = trees.get(i).getMachineBearing() + randomDouble(-maxBoomAngle/2.0, maxBoomAngle/2.0);

                    translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);

                    point.setX(translatedCoordinates[0]);
                    point.setY(translatedCoordinates[1]);

                    insideStand = pointInPolygon(standBoundaries_.get(standId), translatedCoordinates);

                    if(!imposibleHole)
                    if(standPolygons.get(standId).hasHole()){
                        for(int i_ = 0; i_ < standPolygons.get(standId).holes.size(); i_++){

                            if(standPolygons.get(standId).pointInPolygon(standPolygons.get(standId).holes.get(i_), translatedCoordinates)){
                                insideStand = false;
                                nFailedBecauseImpossibleHole++;
                                break;
                            }
                        }
                    }

                    nearest = (List< KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(1, point);

                    if(counter > 0) {
                        nearest = (List<KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(1, point);

                        distanceToNearestTree = euclideanDistance2d(translatedCoordinates[0], translatedCoordinates[1], nearest.get(0).getX(), nearest.get(0).getY());

                        deltah = Math.abs(trees.get(i).getHeight() - trees.get(nearest.get(0).getIndex()).getHeight());
                        highest = Math.max(trees.get(i).getHeight(), trees.get(nearest.get(0).getIndex()).getHeight());


                    }else{
                        distanceCondition = true;
                    }

                    //distanceToNearestTree = euclideanDistance2d(translatedCoordinates[0], translatedCoordinates[1], nearest.get(0).getX(), nearest.get(0).getY());


                    if(aR.PREMOTO_ADAPTIVEDISTANCE) {
                        if ((distanceToNearestTree > ((Math.max(0.75, highest * 0.1 - deltah * 0.1)) - lowerDistanceBy_))) {
                            //if(distanceToNearestTree > (minDistanceBetweenTrees - lowerDistanceBy_)){
                            distanceCondition = true;
                        }
                    }else{
                        //if ((distanceToNearestTree > ((Math.max(0.75, highest * 0.1 - deltah * 0.1)) - lowerDistanceBy_))) {
                        if(distanceToNearestTree > (minDistanceBetweenTrees - lowerDistanceBy_)){
                            distanceCondition = true;
                        }
                    }

                    nTries++;

                    if(nTries % nTriesBeforeLowerStandards == 0){

                        if((double)nFailedBecauseImpossibleHole / (double)nTriesBeforeLowerStandards > 0.75) {
                            imposibleHole = true;
                        }

                        nFailedBecauseImpossibleHole = 0;

                        maxBoomAngle += 50;
                        minDistanceBetweenTrees -= 0.25;
                        lowerDistanceBy_ += lowerDistanceBy;
                        //maxBoomDistance += 0.5;
                        startPrinting = true;
                    }

                    if(startPrinting){
                        //System.out.println("nTries: " + nTries + " isinside: " + insideStand + " distanceToNearest " + distanceToNearestTree);
                    }

                }

                System.out.println(distance + " " + insideStand);

                trees.get(i).setX_coordinate_estimated(translatedCoordinates[0]);
                trees.get(i).setY_coordinate_estimated(translatedCoordinates[1]);
                trees.get(i).setZ_coordinate_estimated(trees.get(i).getZ_coordinate_machine());
                trees.get(i).setBoomAngle(angle);
                trees.get(i).setBoomExtension(distance);

                kdTree.add(trees.get(i).toXYZPoint_estimatedCoords());

            }else{

                trees.get(i).setX_coordinate_estimated(translatedCoordinates[0]);
                trees.get(i).setY_coordinate_estimated(translatedCoordinates[1]);
                trees.get(i).setZ_coordinate_estimated(trees.get(i).getZ_coordinate_machine());
                trees.get(i).setBoomAngle(angle);
                trees.get(i).setBoomExtension(distance);

                kdTree.add(trees.get(i).toXYZPoint_estimatedCoords());
            }

            counter++;
        }

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
            randomDistance = randomDouble(0, selectedRange);
        }else{
            randomDistance = randomDouble(DISTANCE_RANGES[selectedRangeIndex - 1], selectedRange);
        }

        return randomDistance;
    }


    public void estimationWithAuxiliaryData(double maxBoomDistance, double maxBoomAngle, double minDistanceBetweenTrees,
                                            double[] boomDistanceProbabilities, double[] boomDistanceRanges,
                                            double deltaAuxData){

        KdTree kdTree = new KdTree();

        float[] floatArray = new float[]{99};

        KdTree.XYZPoint point = new KdTree.XYZPoint(0,0,0);
        List<KdTree.XYZPoint> nearest = new ArrayList<KdTree.XYZPoint>();
        int counter = 0;
        int nTriesBeforeLowerStandards = 500;

        double lowerDistanceBy = 0.25;
        double lowerDistanceBy_ = 0.0;
        double lowerDistanceToCHMBy = 2.0;


        double maxBoomAngleOriginal = maxBoomAngle;
        double minDistanceBetweenTreesOriginal = minDistanceBetweenTrees;

        this.sortTreesByHeight();

        boolean imposibleHole = false;

        int treeSize = 0;

        for(int i = 0; i < trees_sorted.size(); i++){

            if(!trees_sorted.get(i).trulyInStand)
                continue;

            maxBoomAngle = maxBoomAngleOriginal;
            minDistanceBetweenTrees = minDistanceBetweenTreesOriginal;
            lowerDistanceBy_ = 0.0;
            double maxDistanceToCHM = 2.0;
            int standId = trees_sorted.get(i).standId;
            double distance = randomDouble(0, maxBoomDistance);
            double angle = randomDouble(-maxBoomAngle/2.0, maxBoomAngle/2.0);

            if(trees_sorted.get(i).getX_coordinate_machine() <= 10 || trees_sorted.get(i).getY_coordinate_machine() <= 10)
                continue;
            //double[] debug = translatePoint(0,0, 1.414214, 45);

            //System.out.println(Arrays.toString(debug));
            //System.exit(1);
            double[] translatedCoordinates = translatePoint(trees_sorted.get(i).getX_coordinate_machine(), trees_sorted.get(i).getY_coordinate_machine(), distance, angle);

            double distanceToNearestTree = Double.NEGATIVE_INFINITY;
            boolean insideStand = false;

            double deltaToAuxData = Double.POSITIVE_INFINITY;
            double treeHeight = trees_sorted.get(i).getHeight();

            //System.out.println("TREE HEIGHT: " + treeHeight);

            double auxDataValue = -1;

            boolean distanceCondition = false;

            int numberOfTries_CHM = 0;
            int numberOfTries_other = 0;

            boolean switchToAbsoluteValueInChm = false;


            int numOutsideCHM = 0;
            boolean treeOutsideCHM = false;

            if(counter > -1){

                if(trees_sorted.get(i).id == 811){
                    //System.out.println("debug " + trees.get(i).boomAngle);
                }

                int nTries = 0;

                floatArray[0] = 0;

                int nFailedBecauseImpossibleHole = 0;

                while(!distanceCondition || !insideStand || floatArray[0] > maxDistanceToCHM){

                    distanceCondition = false;

                    distance = generateRandomDistance(boomDistanceProbabilities, boomDistanceRanges);
                    angle = trees_sorted.get(i).getMachineBearing() + randomDouble(-maxBoomAngle/2.0, maxBoomAngle/2.0);

                    translatedCoordinates = translatePoint(trees_sorted.get(i).getX_coordinate_machine(), trees_sorted.get(i).getY_coordinate_machine(), distance, angle);

                    int x = (int) Math.round((translatedCoordinates[0] - rasters.getCurrentSelectionMinX()) / rasters.getResolution());
                    //int x = (int) Math.round((translatedCoordinates[0] - geoTransform[0]) / geoTransform[1]);
                    int y = (int) Math.round((translatedCoordinates[1] - rasters.getCurrentSelectionMaxY()) / -rasters.getResolution());
                    //int y = (int) Math.round((translatedCoordinates[1] - geoTransform[3]) / geoTransform[5]);

                    //tifBand.ReadRaster(x, y, 1, 1, floatArray);
                    //System.out.println("x: " + x + " y: " + y);
                    //System.out.println(auxData.length + " " + auxData[0].length);

                    if(x < 0 || x >= auxData.length || y < 0 || y >= auxData[0].length){
                        numOutsideCHM++;
                        treeOutsideCHM = true;
                        break;
                    }

                    floatArray[0] = auxData[x][y];
                    deltaToAuxData = Math.abs(floatArray[0] - treeHeight);


                    floatArray[0] = (float)deltaToAuxData;




                    point.setX(translatedCoordinates[0]);
                    point.setY(translatedCoordinates[1]);

                    insideStand = pointInPolygon(standBoundaries_.get(standId), translatedCoordinates);
                    boolean insideHole = false;

                    if(!imposibleHole)
                    if(standPolygons.get(standId).hasHole()){
                        for(int i_ = 0; i_ < standPolygons.get(standId).holes.size(); i_++){

                            if(standPolygons.get(standId).pointInPolygon(standPolygons.get(standId).holes.get(i_), translatedCoordinates)){
                                insideStand = false;
                                insideHole = true;
                                nFailedBecauseImpossibleHole++;
                                break;
                            }
                        }
                    }

                    //if(counter == 52)
                    //    System.out.println("prog: " + nFailedBecauseImpossibleHole);

                    double deltah = 0;
                    double highest = 0;

                    if(counter > 0) {
                        nearest = (List<KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(1, point);

                        distanceToNearestTree = euclideanDistance2d(translatedCoordinates[0], translatedCoordinates[1], nearest.get(0).getX(), nearest.get(0).getY());

                        deltah = Math.abs(trees_sorted.get(i).getHeight() - trees_sorted.get(nearest.get(0).getIndex()).getHeight());
                        highest = Math.max(trees_sorted.get(i).getHeight(), trees_sorted.get(nearest.get(0).getIndex()).getHeight());


                    }else{
                         distanceCondition = true;
                    }

                    //max(1, 2.5 - (highest * 0.5) - (deltaH * 0.1))
                    //if((distanceToNearestTree > ( Math.max(1, 2.5 - (highest * 0.5) - (deltah * 0.1)) - lowerDistanceBy_))){
                    //System.out.println(deltah + " " + highest + " " + distanceToNearestTree + " " + ( Math.max(0.75, highest * 0.1 - deltah * 0.1)) + " " + i);

                    if(aR.PREMOTO_ADAPTIVEDISTANCE) {
                        if ((distanceToNearestTree > ((Math.max(0.75, highest * 0.1 - deltah * 0.1)) - lowerDistanceBy_))) {
                            //if(distanceToNearestTree > (minDistanceBetweenTrees - lowerDistanceBy_)){
                            distanceCondition = true;
                        }
                    }else{
                        //if ((distanceToNearestTree > ((Math.max(0.75, highest * 0.1 - deltah * 0.1)) - lowerDistanceBy_))) {
                        if(distanceToNearestTree > (minDistanceBetweenTrees - lowerDistanceBy_)){
                            distanceCondition = true;
                        }
                    }

                    if(floatArray[0] > maxDistanceToCHM){
                        numberOfTries_CHM++;

                    }

                    if(!distanceCondition || !insideStand)
                        numberOfTries_other++;

                    nTries++;
                    //System.out.println("raster value: " + floatArray[0] + " tree height: " + treeHeight + " distanceToNearest " + distanceToNearestTree);
                    if(nTries % nTriesBeforeLowerStandards == 0){

                       // System.out.println((double)nFailedBecauseImpossibleHole / (double)nTriesBeforeLowerStandards);

                        if((double)nFailedBecauseImpossibleHole / (double)nTriesBeforeLowerStandards > 0.75) {
                            //System.out.println("IMPOSSIBLE HOLE");
                            //System.exit(1);
                            imposibleHole = true;
                        }


                        //System.out.println("HERE! " + nFailedBecauseImpossibleHole + " " + nTriesBeforeLowerStandards + " " + imposibleHole + " " + counter);
                        if(insideHole){
                            System.out.println("HERE! " + nFailedBecauseImpossibleHole + " " + nTriesBeforeLowerStandards + " " + imposibleHole + " " + counter);
                            //System.out.println(trees_sorted.get(i).getX_coordinate_machine() + " " + trees_sorted.get(i).getY_coordinate_machine());
                            maxDistanceToCHM += lowerDistanceToCHMBy;
                            maxBoomAngle += 25;
                            minDistanceBetweenTrees -= lowerDistanceBy;
                            lowerDistanceBy_ += lowerDistanceBy;
                            //System.exit(1);
                        }
                        else if(numberOfTries_CHM > numberOfTries_other){
                            maxDistanceToCHM += lowerDistanceToCHMBy;
                        }
                        else if(maxBoomAngle >= 360 && lowerDistanceBy_ < 0.5){
                            maxDistanceToCHM += lowerDistanceToCHMBy;
                        }
                        else {
                            maxBoomAngle += 25;
                            minDistanceBetweenTrees -= lowerDistanceBy;
                            //lowerDistanceBy_ += lowerDistanceBy;
                        }

                        if(nTries > (3 * nTriesBeforeLowerStandards))
                            lowerDistanceBy_ += lowerDistanceBy;

                        nFailedBecauseImpossibleHole = 0;
                        //maxBoomDistance += 0.5;

                        //System.out.println(maxBoomAngle + " " + maxDistanceToCHM + " " + minDistanceBetweenTrees + " " + lowerDistanceBy_ + " " + lowerDistanceToCHMBy + " " + i);

                        if( Math.abs(treeHeight - maxDistanceToCHM) < 5)
                            switchToAbsoluteValueInChm = true;

                    }



                }

                //System.out.println(distance + " " + insideStand);

                trees_sorted.get(i).setX_coordinate_estimated(translatedCoordinates[0]);
                trees_sorted.get(i).setY_coordinate_estimated(translatedCoordinates[1]);
                trees_sorted.get(i).setZ_coordinate_estimated(trees_sorted.get(i).getZ_coordinate_machine());
                trees_sorted.get(i).setBoomAngle(angle);
                trees_sorted.get(i).setBoomExtension(distance);

                kdTree.add(trees_sorted.get(i).toXYZPoint_estimatedCoords(i));
                //System.out.println("tree size: " + treeSize++);
                if(treeOutsideCHM){
                    System.out.println("tree outside CHM");
                    for(int i_ = 0; i_ < trees_sorted.size(); i_++){
                        trees_sorted.get(i).trulyInStand = false;
                    }
                    break;
                }

            }else{

                trees_sorted.get(i).setX_coordinate_estimated(translatedCoordinates[0]);
                trees_sorted.get(i).setY_coordinate_estimated(translatedCoordinates[1]);
                trees_sorted.get(i).setZ_coordinate_estimated(trees_sorted.get(i).getZ_coordinate_machine());
                trees_sorted.get(i).setBoomAngle(angle);
                trees_sorted.get(i).setBoomExtension(distance);

                kdTree.add(trees_sorted.get(i).toXYZPoint_estimatedCoords(i));
            }

            counter++;
        }

        kdTree = null;
        System.gc();
        System.gc();

    }

    public void estimationWithAuxiliaryDataOptimized(){

        LevenbergMarquardt lm = new LevenbergMarquardt(10);

        ResidFunctionPREMOTO residFunctionPREMOTOR = new ResidFunctionPREMOTO();

        DMatrixRMaj param = new DMatrixRMaj(this.trees.size() * 2, 1);

        for(int g = 0 ; g < param.numRows; g++)
            param.set(g,0,0);

        simulatedAnnealingPREMOTO annealing = new simulatedAnnealingPREMOTO();


        residFunctionPREMOTOR.setChm(this.auxData);
        residFunctionPREMOTOR.setTrees(this.trees);
        residFunctionPREMOTOR.setGeotransform(this.geoTransform);
        residFunctionPREMOTOR.setStandBoundaries(this.standBoundaries_);

        annealing.optimize(residFunctionPREMOTOR, param);
        applyAnnealingToTrees(param);

        /*
        lm.setDelta(200);
        lm.optimize(residFunctionPREMOTOR, param);

         */



    }

    public void releaseMemory(){



    }

    public void estimationWithAuxiliaryDataOptimized2(){

        LevenbergMarquardt lm = new LevenbergMarquardt(10);

        ResidFunctionPREMOTO residFunctionPREMOTOR = new ResidFunctionPREMOTO();

        DMatrixRMaj param = new DMatrixRMaj(this.trees.size(), 1);

        for(int g = 0 ; g < param.numRows; g++)
            param.set(g,0,0);

        simulatedAnnealingPREMOTO annealing = new simulatedAnnealingPREMOTO();


        residFunctionPREMOTOR.setChm(this.auxData);
        residFunctionPREMOTOR.setTrees(this.trees);
        residFunctionPREMOTOR.setGeotransform(this.geoTransform);
        residFunctionPREMOTOR.setStandBoundaries(this.standBoundaries_);

        annealing.optimize2(residFunctionPREMOTOR, param);
        annealing.apply(param);

        /*
        lm.setDelta(200);
        lm.optimize(residFunctionPREMOTOR, param);

         */



    }
    public void applyAnnealingToTrees(DMatrixRMaj param){

        for(int i = 0; i < trees.size(); i++){

            double distance = param.data[i * 2 + 0];
            double angle = trees.get(i).getMachineBearing() + param.data[i * 2 + 1];


            double[] translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);

            trees.get(i).setX_coordinate_estimated(translatedCoordinates[0]);
            trees.get(i).setY_coordinate_estimated(translatedCoordinates[1]);

        }


    }



    public double randomDouble(double min, double max){
        return min + (max - min) * random.nextDouble();
        //return min + (max - min) * Math.random();
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


    public void setAuxiliaryDataFile(File auxiliaryDataFile) {
        this.auxiliaryDataFile = auxiliaryDataFile;
    }

    public void readRaster(){

        gdal.AllRegister();

        this.tifDataset = gdal.Open(this.auxiliaryDataFile.getAbsolutePath(), gdalconst.GA_ReadOnly);
        this.tifBand = tifDataset.GetRasterBand(1);
        int number_of_pix_x = tifDataset.getRasterXSize();
        int number_of_pix_y = tifDataset.getRasterYSize();

        this.geoTransform = tifDataset.GetGeoTransform();
        this.auxData = new float[tifDataset.GetRasterXSize()][tifDataset.GetRasterYSize()];

        float[] floatArray = new float[number_of_pix_x];

        System.out.println("Reading raster line by line");

        long startTime = System.currentTimeMillis();

        for(int y = 0; y < number_of_pix_y; y++) {

            tifBand.ReadRaster(0, y, number_of_pix_x, 1, floatArray);

            for (int x = 0; x < number_of_pix_x; x++) {

                //System.out.println("line " + y);
                float value = floatArray[x];

                auxData[x][y] = value;

            }
        }

    }

    public void readRasters(){

        gdal.AllRegister();

        ArrayList<Integer> overlappin = this.rasters.findOverlappingRasters(this.extent[0], this.extent[1], this.extent[2], this.extent[3]);

        System.out.println("Number of overlapping rasters: " + overlappin.size());


        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double resolution = this.rasters.getRaster(0).getResolution();

        for(int i = 0 ; i < overlappin.size(); i++){

                int rasterIndex = overlappin.get(i);

                if(this.rasters.getRaster(rasterIndex).getMinX() < minX)
                    minX = this.rasters.getRaster(rasterIndex).getMinX();

                if(this.rasters.getRaster(rasterIndex).getMinY() < minY)
                    minY = this.rasters.getRaster(rasterIndex).getMinY();

                if(this.rasters.getRaster(rasterIndex).getMaxX() > maxX)
                    maxX = this.rasters.getRaster(rasterIndex).getMaxX();

                if(this.rasters.getRaster(rasterIndex).getMaxY() > maxY)
                    maxY = this.rasters.getRaster(rasterIndex).getMaxY();

        }

        double numPixelsX = (maxX - minX) / resolution;
        double numPixelsY = (maxY - minY) / resolution;


        System.out.println("Number of pixels: " + numPixelsX + " " + numPixelsY);

        if(overlappin.size() == 0){
            for(int i = 0; i < trees.size(); i++){
                trees.get(i).trulyInStand = false;
            }

            noAuxDataAvailable = true;
            return;
        }

        this.auxData = rasters.currenSelectionToArray();

        //System.exit(1);

    }

    public void readMultipleRastersTo2dArray(float[][] array, ArrayList<Integer> overlappin, rasterCollection rasters){

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double resolution = this.rasters.getRaster(0).getResolution();

        for(int i = 0 ; i < overlappin.size(); i++){

            int rasterIndex = overlappin.get(i);

            if(this.rasters.getRaster(rasterIndex).getMinX() < minX)
                minX = this.rasters.getRaster(rasterIndex).getMinX();

            if(this.rasters.getRaster(rasterIndex).getMinY() < minY)
                minY = this.rasters.getRaster(rasterIndex).getMinY();

            if(this.rasters.getRaster(rasterIndex).getMaxX() > maxX)
                maxX = this.rasters.getRaster(rasterIndex).getMaxX();

            if(this.rasters.getRaster(rasterIndex).getMaxY() > maxY)
                maxY = this.rasters.getRaster(rasterIndex).getMaxY();

        }

        double numPixelsX = (maxX - minX) / resolution;
        double numPixelsY = (maxY - minY) / resolution;

        array = new float[(int)numPixelsX][(int)numPixelsY];



    }
}

// Custom comparator for comparing Person objects based on age
class treeHeightComparator implements Comparator<Tree> {
    @Override
    public int compare(Tree tree1, Tree tree2) {
        // Compare persons based on their ages
        return Float.compare(tree2.getHeight()   , tree1.getHeight());
    }
}
