package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.interpolation.VertexValuatorDefault;
import org.tinfour.utils.Polyside;
import utils.argumentReader;
import utils.pointWriterMultiThread;

import utils.tinManupulator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import static org.tinfour.utils.Polyside.isPointInPolygon;

public class lasAligner {

    tinManupulator tinM;
    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
    argumentReader aR;

    public ArrayList<LASReader> refs = new ArrayList<LASReader>();
    public ArrayList<LASReader> targets = new ArrayList<LASReader>();

    double resolution = 2.0;

    int min_points_in_cell = 5;

    public double min_x = Double.POSITIVE_INFINITY, max_x = Double.NEGATIVE_INFINITY;
    public double min_y = Double.POSITIVE_INFINITY, max_y = Double.NEGATIVE_INFINITY;

    ArrayList<double[]> tinNodes = new ArrayList<>();

    ArrayList<ArrayList<Integer>> targetPairsInRef = new ArrayList<>();
    public lasAligner(){

    }

    public lasAligner(argumentReader aR){
       this.aR = aR;
    }

    public void setResolution(double res){
        this.resolution = res;
    }

    public void setTinManupulator(tinManupulator in){

        this.tinM = in;

    }

    public void setMin_points_in_cell(int min_points_in_cell) {
        this.min_points_in_cell = min_points_in_cell;
    }
    public void readRefs(){

        for(int i = 0; i < aR.ref.size(); i++){

            try {
                refs.add(new LASReader(aR.ref.get(i)));
            }
            catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public void readTargets(){

            for(int i = 0; i < aR.tar.size(); i++){

                try {

                    LASReader tmpReader = new LASReader(aR.tar.get(i));

                    targets.add(tmpReader);

                    if(tmpReader.getMinX() < min_x)
                        min_x = tmpReader.getMinX();
                    if(tmpReader.getMaxX() > max_x)
                        max_x = tmpReader.getMaxX();
                    if(tmpReader.getMinY() < min_y)
                        min_y = tmpReader.getMinY();
                    if(tmpReader.getMaxY() > max_y)
                        max_y = tmpReader.getMaxY();

                }
                catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
            }
    }

    public void processTargets() throws Exception{

        ForkJoinPool customThreadPool = new ForkJoinPool(4);

        try {

            customThreadPool.submit(() ->

                    IntStream.range(0, targets.size()).parallel().forEach(i__ -> {


        for(int i_ = 0; i_ < targets.size(); i_++) {

            double min_x = targets.get(i_).getMinX();
            double max_y = targets.get(i_).getMaxY();
            double max_x = targets.get(i_).getMaxX();
            double min_y = targets.get(i_).getMinY();

            min_x = this.min_x;
            max_y = this.max_y;
            max_x = this.max_x;
            min_y = this.min_y;


            for (int j = 0; j < targetPairsInRef.get(i_).size(); j++) {

                if (refs.get(targetPairsInRef.get(i_).get(j)).getMinX() < min_x) {
                //    min_x = refs.get(targetPairsInRef.get(i_).get(j)).getMinX();
                }
                if (refs.get(targetPairsInRef.get(i_).get(j)).getMaxY() > max_y) {
                //    max_y = refs.get(targetPairsInRef.get(i_).get(j)).getMaxY();
                }

                if (refs.get(targetPairsInRef.get(i_).get(j)).getMaxX() > max_x) {
                //    max_x = refs.get(targetPairsInRef.get(i_).get(j)).getMaxX();
                }
                if (refs.get(targetPairsInRef.get(i_).get(j)).getMinY() < min_y) {
                //    min_y = refs.get(targetPairsInRef.get(i_).get(j)).getMinY();
                }

            }

            int numberOfPixelsX = (int) Math.ceil((max_x - min_x) / resolution);
            int numberOfPixelsY = (int) Math.ceil((max_y - min_y) / resolution);

            double origo_x = min_x;
            double origo_y = max_y;

            dataPointTiny[][] firstCheck = new dataPointTiny[numberOfPixelsX][numberOfPixelsY];

            for (int j = 0; j < numberOfPixelsX; j++) {
                for (int k = 0; k < numberOfPixelsY; k++) {
                    firstCheck[j][k] = new dataPointTiny();
                }
            }

            LasPoint tempPoint = new LasPoint();

            for (int j = 0; j < targetPairsInRef.get(i_).size(); j++) {

                int thread_n = aR.pfac.addReadThread(refs.get(targetPairsInRef.get(i_).get(j)));

                LASReader in = refs.get(targetPairsInRef.get(i_).get(j));

                for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                    int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

                    try{
                        aR.pfac.prepareBuffer(thread_n, i, 20000);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        System.exit(1);
                    }
                    for (int j_ = 0; j_ < maxi; j_++) {

                        in.readFromBuffer(tempPoint);

                        int x = (int) Math.floor((tempPoint.x - origo_x) / resolution);
                        int y = (int) Math.floor((origo_y - tempPoint.y) / resolution);

                        if(x < 0 || x >= numberOfPixelsX || y < 0 || y >= numberOfPixelsY)
                            continue;

                        if (tempPoint.classification == 2) {
                            firstCheck[x][y].addGround((float) tempPoint.z);
                        } else
                            firstCheck[x][y].addNonGround((float) tempPoint.z);

                    }

                }

            }

            HashSet<Integer> properCells = new HashSet<>();

            for (int j = 0; j < numberOfPixelsX; j++) {
                for (int k = 0; k < numberOfPixelsY; k++) {

                    if (firstCheck[j][k].getGroundCount() > min_points_in_cell) {
                        //System.out.println("Ground mean: " + firstCheck[j][k].getGroundMean() + " non-ground mean: " + firstCheck[j][k].getNonGroundMean());
                        float meanGround = firstCheck[j][k].getGroundMean();
                        float meanNonGround = firstCheck[j][k].getNonGroundMean();

                        if (Float.isNaN(meanNonGround) || Math.abs(meanGround - meanNonGround) < 0.25) {

                            if( firstCheck[j][k].countGround / (firstCheck[j][k].countGround + firstCheck[j][k].countNonGround) > 0.95 )
                                properCells.add(j + k * numberOfPixelsX);
                        }
                    }
                }
            }

            System.out.println("Proper cells: " + properCells.size());


            LASReader in = targets.get(i_);

            int thread_n = aR.pfac.addReadThread(in);

            for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

                try{
                    aR.pfac.prepareBuffer(thread_n, i, 20000);
                }
                catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
                for (int j_ = 0; j_ < maxi; j_++) {

                    in.readFromBuffer(tempPoint);

                    int x = (int) Math.floor((tempPoint.x - origo_x) / resolution);
                    int y = (int) Math.floor((origo_y - tempPoint.y) / resolution);

                    if (properCells.contains(x + y * numberOfPixelsX)) {

                        firstCheck[x][y].addTarget((float) tempPoint.z);

                    }

                }
            }



            //ogr.RegisterAll(); //Registering all the formats..

            VertexValuatorDefault valuator = new VertexValuatorDefault();

            TriangularFacetInterpolator polator = new TriangularFacetInterpolator(tin);

            ArrayList<Double> valuesToCheck = new ArrayList<>();
            for(int i : properCells){
                int x = i % numberOfPixelsX;
                int y = i / numberOfPixelsX;

                if(firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target < 0.5 && firstCheck[x][y].countTarget > 10) {

                    //System.out.println("DIFFERENCE: " + (firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()));

                    float[] outValue = new float[]{firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()};
                    valuesToCheck.add((double) outValue[0]);

                }
            }

            ArrayList<Integer> outliers = getOutlierIndexes(valuesToCheck, 3);

            float maxOutlier = Float.NEGATIVE_INFINITY;
            float minOutlier = Float.POSITIVE_INFINITY;

            HashSet<Integer> outlierCells = new HashSet<>(outliers);

            for(int i = 0; i < outliers.size(); i++){

                if(valuesToCheck.get(outliers.get(i)) > maxOutlier)
                    maxOutlier = valuesToCheck.get(outliers.get(i)).floatValue();
                if(valuesToCheck.get(outliers.get(i)) < minOutlier)
                    minOutlier = valuesToCheck.get(outliers.get(i)).floatValue();

            }

            System.out.println("outliers: " + outliers.size() + " " + maxOutlier + " " + minOutlier + " " + valuesToCheck.size());
            //System.exit(1);
            int counter = 0;

            for(int i : properCells){


                int x = i % numberOfPixelsX;
                int y = i / numberOfPixelsX;

                if(firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target < 0.5 && firstCheck[x][y].countTarget > 10){

                    //System.out.println("DIFFERENCE: " + (firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()));

                    float[] outValue = new float[]{firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()};


                    if(outlierCells.contains(counter++)) {
                        System.out.println("OUTLIER: " + i + " " + outValue[0] + " ");
                        continue;
                    }

                    if(outValue[0] > 10.0) {
                        System.out.println("OUTVALUE: " + outValue[0] + " " + (firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target));
                    }

                    tin.add(new org.tinfour.common.Vertex(x + resolution / 2.0, y - resolution / 2.0, outValue[0]));

                    tinM.addPointToTin(this.min_x + x * resolution + resolution / 2.0, this.max_y - y * resolution - resolution / 2.0, outValue[0] );
                    //tinNodes.add(new double[]{ this.min_x + x * resolution + resolution / 2.0, this.max_y - y * resolution - resolution / 2.0, outValue[0] });
                    //band.WriteRaster(x, y, 1, 1, outValue);

                }

            }

            if(true)
                return;

            gdal.AllRegister();

            Driver driver = null;
            driver = gdal.GetDriverByName("GTiff");
            driver.Register();
            String outputFileName = null;
            try {
                outputFileName = aR.createOutputFileWithExtension(targets.get(i_).getFile(), "_correctionRaster.tif").getAbsolutePath();
            }catch (Exception e){
                System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
                return;
            }

            //Dataset dataset = driver.Create("tmp.tif", grid_x_size, grid_x_size, 1, gdalconst.GDT_Float32);
            Dataset dataset_output = null;
            Band band = null;

            try {
                dataset_output = driver.Create(outputFileName, numberOfPixelsX, numberOfPixelsY, 1, gdalconst.GDT_Float32);
                band =  dataset_output.GetRasterBand(1);
            }catch (Exception e){
                System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
                return;
            }


            band.SetNoDataValue(Float.NaN);

            double[] geoTransform = new double[]{min_x, resolution, 0.0, max_y, 0.0, -resolution};

            SpatialReference sr = new SpatialReference();

            sr.ImportFromEPSG(aR.EPSG);

            dataset_output.SetProjection(sr.ExportToWkt());
            dataset_output.SetGeoTransform(geoTransform);


            polator.resetForChangeToTin();

            double[][] smoothed = new double[numberOfPixelsX][numberOfPixelsY];


            for (int j = 0; j < numberOfPixelsX; j++) {
                for (int k = 0; k < numberOfPixelsY; k++) {

                    float interpolatedValue = (float)polator.interpolate(j + resolution / 2.0, k - resolution / 2.0, valuator);

                    float[] outValue = new float[]{interpolatedValue};

                    smoothed[j][k] = interpolatedValue;

                    //band.WriteRaster(j, k, 1, 1, outValue);
                }
            }

            if(aR.theta != 0.0)
                aR.kernel = (int)( 2.0 * Math.ceil( 3.0 * aR.theta) + 1.0);

            System.out.println("Kernel: " + aR.kernel + " theta: " + aR.theta);

            smoothed = GaussianSmooth.smooth(smoothed, numberOfPixelsX, numberOfPixelsY, aR.kernel, aR.theta);  // THIS IS GOOD!!!! :)

            double sum = 0.0;
            double count = 0;

            for (int j = 0; j < numberOfPixelsX; j++) {
                for (int k = 0; k < numberOfPixelsY; k++) {

                    float[] outValue = new float[]{(float)smoothed[j][k]};

                    band.WriteRaster(j, k, 1, 1, outValue);

                    if(!Float.isNaN(outValue[0])){
                        sum += outValue[0];
                        count++;
                    }

                }
            }

            double averageCorrection = sum / count;

            //System.out.println("AVERAGE: " + sum / count);
            band.FlushCache();
            //band2.FlushCache();

            dataset_output.delete();
            File outFile = null;
            pointWriterMultiThread pw = null;

            try {
                outFile = aR.createOutputFile(in);

                pw = new pointWriterMultiThread(outFile, in, "las2las", aR);
            }catch (Exception e){
                System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
                return;
            }
            LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
            aR.pfac.addWriteThread(thread_n, pw, buf);


            for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

                try {
                    aR.pfac.prepareBuffer(thread_n, i, 20000);
                }catch (Exception e){
                    System.out.println("Failed to prepare buffer");
                    return;
                }
                for (int j_ = 0; j_ < maxi; j_++) {

                    in.readFromBuffer(tempPoint);

                    double x = (tempPoint.x - min_x) / resolution;
                    double y = (max_y - tempPoint.y) / resolution;

                    double interpolatedValue = polator.interpolate(x, y, valuator);

                    if(isPointInPolygon(tin.getPerimeter(), x, y) == Polyside.Result.Inside){
                        if(!Double.isNaN(interpolatedValue))
                            tempPoint.z -= interpolatedValue;
                        else
                            tempPoint.z -= averageCorrection;
                    }else
                        tempPoint.z -= averageCorrection;


                    try {

                        aR.pfac.writePoint(tempPoint, i+j_, thread_n);

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }

                }
            }

            aR.pfac.closeThread(thread_n);
        }
                    })).get();
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public void applyCorrection(){

        LasPoint tempPoint = new LasPoint();

        for(int i_ = 0; i_ < targets.size(); i_++){

            LASReader in = targets.get(i_);


            int thread_n = aR.pfac.addReadThread(in);

            File outFile = null;
            pointWriterMultiThread pw = null;

            try {
                outFile = aR.createOutputFile(in);

                pw = new pointWriterMultiThread(outFile, in, "las2las", aR);
            }catch (Exception e){
                System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
                return;
            }
            LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
            aR.pfac.addWriteThread(thread_n, pw, buf);


            for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

                try {
                    aR.pfac.prepareBuffer(thread_n, i, 20000);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                for (int j_ = 0; j_ < maxi; j_++) {

                    in.readFromBuffer(tempPoint);

                    double interpolatedValue = tinM.interpolate(tempPoint.x, tempPoint.y);

                    boolean pointInTin = tinM.isPointInTin(tempPoint.x, tempPoint.y);

                    if(pointInTin) {
                        if (!Double.isNaN(interpolatedValue)) {
                            tempPoint.z -= interpolatedValue;
                            try {

                                aR.pfac.writePoint(tempPoint, i+j_, thread_n);

                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                        }
                        //else
                        //    tempPoint.z -= averageCorrection;
                        //}else
                        //    tempPoint.z -= averageCorrection;
                    }else{

                    }

                }
            }
            aR.pfac.closeThread(thread_n);
        }

    }


    public static ArrayList<Integer> getOutlierIndexes(ArrayList<Double> values, double zScoreThreshold) {
        ArrayList<Integer> outlierIndexes = new ArrayList<>();

        // Calculate the mean and standard deviation
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        double mean = sum / values.size();

        double sumSquaredDeviations = 0;
        for (double value : values) {
            double deviation = value - mean;
            sumSquaredDeviations += deviation * deviation;
        }
        double stdDev = Math.sqrt(sumSquaredDeviations / values.size());

        // Check each value to see if it's an outlier
        for (int i = 0; i < values.size(); i++) {
            double zScore = Math.abs((values.get(i) - mean) / stdDev);
            if (zScore > zScoreThreshold) {
                outlierIndexes.add(i);
            }
        }

        return outlierIndexes;
    }

    public void createCorrectionRaster(){



    }

    public void prepareData(){



        for(int i = 0; i < targets.size(); i++){

            targetPairsInRef.add(new ArrayList<>());

            double minx = targets.get(i).getMinX();
            double miny = targets.get(i).getMinY();
            double minz = targets.get(i).getMinZ();
            double maxx = targets.get(i).getMaxX();
            double maxy = targets.get(i).getMaxY();
            double maxz = targets.get(i).getMaxZ();


            for(int j = 0; j < refs.size(); j++){


                double minx2 = refs.get(j).getMinX();
                double miny2 = refs.get(j).getMinY();
                double minz2 = refs.get(j).getMinZ();
                double maxx2 = refs.get(j).getMaxX();
                double maxy2 = refs.get(j).getMaxY();
                double maxz2 = refs.get(j).getMaxZ();

                if(doBoundingBoxesOverlap(minx, maxx, miny, maxy, minx2, maxx2, miny2, maxy2)){
                    System.out.println("Overlap found! " + refs.get(j).getFile().getName());
                    targetPairsInRef.get(targetPairsInRef.size()-1).add(j);
                }else{
                    System.out.println("No overlap found!");
                }
            }
        }

    }

    public static boolean doBoundingBoxesOverlap(double minx1, double maxx1, double miny1, double maxy1,
                                                 double minx2, double maxx2, double miny2, double maxy2) {
        // Check for horizontal overlap
        if (maxx1 < minx2 || minx1 > maxx2) {
            return false;
        }

        // Check for vertical overlap
        if (maxy1 < miny2 || miny1 > maxy2) {
            return false;
        }

        // If both horizontal and vertical overlap exist, the boxes overlap
        return true;
    }
}

class dataPoint{

    double min_x, max_x, min_y, max_y, min_z, max_z;
    double sum_x, sum_y, sum_z;

    dataPoint(){

    }
}

class dataPointTiny{

    public short countGround = 0, countNonGround = 0, countTarget = 0;
    public float sum_z_ground = 0, sum_z_nonGround = 0, sum_z_target = 0, max_z_target = Float.NEGATIVE_INFINITY, min_z_target = Float.POSITIVE_INFINITY;


    dataPointTiny(){

    }

    public void addTarget(float z){
        countTarget++;
        sum_z_target += z;

        if(z > max_z_target)
            max_z_target = z;

        if(z < min_z_target)
            min_z_target = z;

    }

    public float getTargetMean(){

        return sum_z_target / countTarget;

    }
    public void addGround(float z){
        countGround++;
        sum_z_ground += z;
    }



    public void addNonGround(float z){
        countNonGround++;
        sum_z_nonGround += z;
    }

    public int getGroundCount(){
        return countGround;
    }

    public int getNonGroundCount(){
        return countNonGround;
    }

    public float getGroundMean(){
        return sum_z_ground / countGround;

    }

    public float getNonGroundMean(){
        return sum_z_nonGround / countNonGround;
    }

}