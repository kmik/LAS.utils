package tools;

import org.ejml.data.DMatrixRMaj;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import utils.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.index.strtree.GeometryItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;

public class treeLocationEstimator {

    float[][] auxData = null;
    Dataset tifDataset = null;
    Band tifBand = null;

    File auxiliaryDataFile = null;

    double[] geoTransform = null;
    argumentReader aR;

    ArrayList<Tree> trees = new ArrayList<Tree>();

    HashMap<Integer, ArrayList<ConcaveHull.Point>> standBoundaries = new HashMap<>();
    HashMap<Integer, double[][]> standBoundaries_ = new HashMap<>();


    public treeLocationEstimator(){

    }

    public treeLocationEstimator(argumentReader aR){

        this.aR = aR;

    }

    public void setTrees(ArrayList<Tree> trees){
        this.trees = trees;
    }

    public void setStandBoundaries(HashMap<Integer, ArrayList<ConcaveHull.Point>> standBoundaries){

        for(int i : standBoundaries.keySet()){
            ArrayList<ConcaveHull.Point> temp = standBoundaries.get(i);
            double[][] temp_ = new double[temp.size()][2];
            for(int j = 0; j < temp.size(); j++){
                temp_[j][0] = temp.get(j).x;
                temp_[j][1] = temp.get(j).y;
            }
            standBoundaries_.put(i, temp_);
        }

        this.standBoundaries = standBoundaries;
    }


    public void noEstimation(){
        for(int i = 0; i < trees.size(); i++){
            trees.get(i).setX_coordinate_estimated(trees.get(i).getX_coordinate_machine());
            trees.get(i).setY_coordinate_estimated(trees.get(i).getY_coordinate_machine());
            trees.get(i).setZ_coordinate_estimated(trees.get(i).getZ_coordinate_machine());
        }
    }
    public void simpleEstimation(double maxBoomDistance, double maxBoomAngle, double minDistanceBetweenTrees){

        KdTree kdTree = new KdTree();

        KdTree.XYZPoint point = new KdTree.XYZPoint(0,0,0);
        List<KdTree.XYZPoint> nearest = new ArrayList<KdTree.XYZPoint>();

        for(int i = 0; i < trees.size(); i++){

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

            if(i > 0){

                while(distanceToNearestTree <= minDistanceBetweenTrees || !insideStand){

                    distance = randomDouble(3, maxBoomDistance);
                    angle = trees.get(i).getMachineBearing() + randomDouble(-maxBoomAngle/2.0, maxBoomAngle/2.0);

                    translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);

                    point.setX(translatedCoordinates[0]);
                    point.setY(translatedCoordinates[1]);

                    insideStand = pointInPolygon(standBoundaries_.get(standId), translatedCoordinates);

                    nearest = (List< KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(1, point);

                    distanceToNearestTree = euclideanDistance2d(translatedCoordinates[0], translatedCoordinates[1], nearest.get(0).getX(), nearest.get(0).getY());

                }

                System.out.println(distanceToNearestTree + " " + insideStand);

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

        KdTree.XYZPoint point = new KdTree.XYZPoint(0,0,0);
        List<KdTree.XYZPoint> nearest = new ArrayList<KdTree.XYZPoint>();
        int counter = 0;

        for(int i = 0; i < trees.size(); i++){

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

            if(counter > 0){

                if(trees.get(i).id == 811){
                    //System.out.println("debug " + trees.get(i).boomAngle);
                }
                while(distanceToNearestTree <= minDistanceBetweenTrees || !insideStand){

                    distance = generateRandomDistance(boomDistanceProbabilities, boomDistanceRanges);
                    angle = trees.get(i).getMachineBearing() + randomDouble(-maxBoomAngle/2.0, maxBoomAngle/2.0);

                    translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);

                    point.setX(translatedCoordinates[0]);
                    point.setY(translatedCoordinates[1]);

                    insideStand = pointInPolygon(standBoundaries_.get(standId), translatedCoordinates);

                    nearest = (List< KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(1, point);

                    distanceToNearestTree = euclideanDistance2d(translatedCoordinates[0], translatedCoordinates[1], nearest.get(0).getX(), nearest.get(0).getY());

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
            randomDistance = Math.random() * selectedRange;
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

        for(int i = 0; i < trees.size(); i++){

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

            double deltaToAuxData = Double.POSITIVE_INFINITY;
            double treeHeight = trees.get(i).getHeight();

            double auxDataValue = -1;

            boolean distanceCondition = false;

            if(counter > 0){

                if(trees.get(i).id == 811){
                    //System.out.println("debug " + trees.get(i).boomAngle);
                }
                while(!distanceCondition || !insideStand || floatArray[0] < 3){

                    distanceCondition = false;

                    distance = generateRandomDistance(boomDistanceProbabilities, boomDistanceRanges);
                    angle = trees.get(i).getMachineBearing() + randomDouble(-maxBoomAngle/2.0, maxBoomAngle/2.0);

                    translatedCoordinates = translatePoint(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), distance, angle);

                    int x = (int) Math.round((translatedCoordinates[0] - geoTransform[0]) / geoTransform[1]);
                    int y = (int) Math.round((translatedCoordinates[1] - geoTransform[3]) / geoTransform[5]);

                    tifBand.ReadRaster(x, y, 1, 1, floatArray);
                    deltaToAuxData = Math.abs(floatArray[0] - treeHeight);

                    point.setX(translatedCoordinates[0]);
                    point.setY(translatedCoordinates[1]);

                    insideStand = pointInPolygon(standBoundaries_.get(standId), translatedCoordinates);

                    nearest = (List< KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(1, point);

                    distanceToNearestTree = euclideanDistance2d(translatedCoordinates[0], translatedCoordinates[1], nearest.get(0).getX(), nearest.get(0).getY());

                    double deltah = Math.abs(trees.get(i).getHeight() - trees.get(nearest.get(0).getIndex()).getHeight());
                    double highest = Math.max(trees.get(i).getHeight(), trees.get(nearest.get(0).getIndex()).getHeight());
                    //max(1, 2.5 - (highest * 0.5) - (deltaH * 0.1))
                    if(distanceToNearestTree > Math.max(1, 2.5 - (highest * 0.5) - (deltah * 0.1))){
                        distanceCondition = true;
                    }

                    System.out.println("raster value: " + floatArray[0] + " tree height: " + treeHeight + " distanceToNearest " + distanceToNearestTree);


                }

                System.out.println(distance + " " + insideStand);

                trees.get(i).setX_coordinate_estimated(translatedCoordinates[0]);
                trees.get(i).setY_coordinate_estimated(translatedCoordinates[1]);
                trees.get(i).setZ_coordinate_estimated(trees.get(i).getZ_coordinate_machine());
                trees.get(i).setBoomAngle(angle);
                trees.get(i).setBoomExtension(distance);

                kdTree.add(trees.get(i).toXYZPoint_estimatedCoords(i));

            }else{

                trees.get(i).setX_coordinate_estimated(translatedCoordinates[0]);
                trees.get(i).setY_coordinate_estimated(translatedCoordinates[1]);
                trees.get(i).setZ_coordinate_estimated(trees.get(i).getZ_coordinate_machine());
                trees.get(i).setBoomAngle(angle);
                trees.get(i).setBoomExtension(distance);

                kdTree.add(trees.get(i).toXYZPoint_estimatedCoords(i));
            }

            counter++;
        }

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
        return min + (max - min) * Math.random();
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
}
