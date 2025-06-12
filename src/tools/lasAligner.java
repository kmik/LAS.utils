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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.tinfour.utils.Polyside.isPointInPolygon;

public class lasAligner {

    tinManupulator tinM;
    //org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
    argumentReader aR;

    public ArrayList<LASReader> refs = new ArrayList<LASReader>();
    public ArrayList<LASReader> targets = new ArrayList<LASReader>();

    double resolution = 4.0;

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

                    if(tmpReader.getMinX() < min_x && tmpReader.getMinX() != 0.0)
                        min_x = tmpReader.getMinX();
                    if(tmpReader.getMaxX() > max_x && tmpReader.getMaxX() != 0.0)
                        max_x = tmpReader.getMaxX();
                    if(tmpReader.getMinY() < min_y && tmpReader.getMinY() != 0.0)
                        min_y = tmpReader.getMinY();
                    if(tmpReader.getMaxY() > max_y && tmpReader.getMaxY() != 0.0)
                        max_y = tmpReader.getMaxY();

                }
                catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
            }

    }

    public void processTargetsPhotogrammetry() throws Exception{

        ForkJoinPool customThreadPool = new ForkJoinPool(aR.cores);

        try {

            customThreadPool.submit(() ->

                    IntStream.range(0, targets.size()).parallel().forEach(i_ -> {


        //for(int i_ = 0; i_ < targets.size(); i_++) {

            double min_x = targets.get(i_).getMinX();
            double max_y = targets.get(i_).getMaxY();
            double max_x = targets.get(i_).getMaxX();
            double min_y = targets.get(i_).getMinY();

            //min_x = this.min_x;
            //max_y = this.max_y;
            //max_x = this.max_x;
            //min_y = this.min_y;


            for (int j = 0; j < targetPairsInRef.get(i_).size(); j++) {

                if (refs.get(targetPairsInRef.get(i_).get(j)).getMinX() < min_x && refs.get(targetPairsInRef.get(i_).get(j)).getMinX() != 0.0) {
                    min_x = refs.get(targetPairsInRef.get(i_).get(j)).getMinX();
                }
                if (refs.get(targetPairsInRef.get(i_).get(j)).getMaxY() > max_y && refs.get(targetPairsInRef.get(i_).get(j)).getMaxY() != 0.0) {
                    max_y = refs.get(targetPairsInRef.get(i_).get(j)).getMaxY();
                }

                if (refs.get(targetPairsInRef.get(i_).get(j)).getMaxX() > max_x && refs.get(targetPairsInRef.get(i_).get(j)).getMaxX() != 0.0) {
                    max_x = refs.get(targetPairsInRef.get(i_).get(j)).getMaxX();
                }
                if (refs.get(targetPairsInRef.get(i_).get(j)).getMinY() < min_y && refs.get(targetPairsInRef.get(i_).get(j)).getMinY() != 0.0) {
                    min_y = refs.get(targetPairsInRef.get(i_).get(j)).getMinY();
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

            System.out.println("target pairs: " + targetPairsInRef.get(i_).size());
            System.out.println("origo_x: " + origo_x + " origo_y: " + origo_y);
            System.out.println("numberOfPixelsX: " + numberOfPixelsX + " numberOfPixelsY: " + numberOfPixelsY + " " + resolution);

            int thread_n = -1;

            for (int j = 0; j < targetPairsInRef.get(i_).size(); j++) {


                LASReader in = null;

                try {
                    in = new LASReader(refs.get(targetPairsInRef.get(i_).get(j)).getFile());
                }catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
                thread_n = aR.pfac.addReadThread(in);


                for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                    int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

                    try{
                        aR.pfac.prepareBuffer(thread_n, i, maxi);
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
                            firstCheck[x][y].addGround((double) tempPoint.z, (double)tempPoint.x, (double)tempPoint.y);
                        } else
                            firstCheck[x][y].addNonGround(tempPoint.z);

                    }

                }

                try {
                    in.close();
                }catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            TreeSet<Integer> properCells = new TreeSet<>();

            //boolean[][] propCells = new boolean[numberOfPixelsX][numberOfPixelsY];
            int n_propers = 0;
            int min_x_ = Integer.MAX_VALUE;
            int min_y_ = Integer.MAX_VALUE;
            int max_x_ = Integer.MIN_VALUE;
            int max_y_ = Integer.MIN_VALUE;

            // declare a file to write stuff in;
                        /*
            File file = new File("/home/koomikko/Documents/processing_directory/debug/properCells.txt");
            FileWriter fw = null;

            try {
                fw = new FileWriter(file.getAbsoluteFile());
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedWriter bw = new BufferedWriter(fw);


*/
            for (int j = 0; j < numberOfPixelsX; j++) {
                for (int k = 0; k < numberOfPixelsY; k++) {

                    if (firstCheck[j][k].getGroundCount() > min_points_in_cell) {
                        //System.out.println("Ground mean: " + firstCheck[j][k].getGroundMean() + " non-ground mean: " + firstCheck[j][k].getNonGroundMean());
                        double meanGround = firstCheck[j][k].getGroundMean();
                        double meanNonGround = firstCheck[j][k].getNonGroundMean();

                        if (Double.isNaN(meanNonGround) || Math.abs(meanGround - meanNonGround) < 0.5) {

                            if(firstCheck[j][k].max_ground - firstCheck[j][k].min_ground < 5.0)
                                if( firstCheck[j][k].countGround / (firstCheck[j][k].countGround + firstCheck[j][k].countNonGround) > 0.90 ||
                                        Math.abs(meanGround - meanNonGround) < 0.2) {


                                    if(j < min_x_)
                                        min_x_ = j;
                                    if(j > max_x_)
                                        max_x_ = j;
                                    if(k < min_y_)
                                        min_y_ = k;
                                    if(k > max_y_)
                                        max_y_ = k;
/*
                                    try {
                                        bw.write((origo_x+j*resolution) + " " + (origo_y-k*resolution) + "\n");
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
*/
                                    properCells.add(j + k * numberOfPixelsX);
                                    //propCells[j][k] = true;
                                    n_propers++;
                                    //System.out.println("proper coords: " + (origo_x+j*resolution) + " " + (origo_y-k*resolution));
                                }
                        }
                    }
                }
            }
/*
            try {
                bw.close();
                fw.close();
            }catch (Exception e){
                e.printStackTrace();
            }

 */
            System.out.println("min_x: " + min_x_ + " max_x: " + max_x_ + " min_y: " + min_y_ + " max_y: " + max_y_);
            System.out.println("Proper cells: " + properCells.size() + " " + n_propers);
            System.out.println("origo_x: " + origo_x + " origo_y: " + origo_y + " " + resolution);
            HashSet<Integer> matchedCells = new HashSet<>();
            LASReader in = null;

            try {
                in = new LASReader(targets.get(i_).getFile());
            }catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }

            tempPoint = new LasPoint();
            int counterPoints = 0;
            int readPoints = 0;

            min_x_ = Integer.MAX_VALUE;
            min_y_ = Integer.MAX_VALUE;
            max_x_ = Integer.MIN_VALUE;
            max_y_ = Integer.MIN_VALUE;

            System.out.println("Processing: " + in.getFile().getName());

            thread_n = aR.pfac.addReadThread(in);

            for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

                try {
                    aR.pfac.prepareBuffer(thread_n, i, maxi);
                }catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
/*
                try{
                    aR.pfac.prepareBuffer(thread_n, i, maxi);
                }
                catch (Exception e){

                    e.printStackTrace();
                    System.exit(1);

                }

 */

                for (int j_ = 0; j_ < maxi; j_++) {

                    // i : 14520000 j_: 11319 maxi: 12491 numpoints: 14532491 currentProgress 14531319
                    //System.out.println("i : " + i + " j_: " + j_ + " maxi: " + maxi + " numpoints: " + in.getNumberOfPointRecords() + " currentProgress " + (i+j_));
                    try {
                        in.readFromBuffer(tempPoint);
                    }catch (Exception e){
                        e.printStackTrace();

                        System.exit(1);
                    }

                    int x = (int) Math.floor((tempPoint.x - origo_x) / resolution);
                    int y = (int) Math.floor((origo_y - tempPoint.y) / resolution);

                    if(x < 0 || x >= numberOfPixelsX || y < 0 || y >= numberOfPixelsY) {

                        continue;

                    }

                    if(x < min_x_)
                        min_x_ = x;
                    if(x > max_x_)
                        max_x_ = x;
                    if(y < min_y_)
                        min_y_ = y;
                    if(y > max_y_)
                        max_y_ = y;

                    //415960.0 6945859.99

                    //System.out.println("x: " + x + " y: " + y);

                    int searchThis = x + y * numberOfPixelsX;

                    if (properCells.contains(searchThis)) {
                    //if(propCells[x][y]){

                        readPoints++;
                        matchedCells.add(x + y * numberOfPixelsX);
                        firstCheck[x][y].addTarget((double) tempPoint.z, (double)tempPoint.x, (double)tempPoint.y);

                    }

                }
            }

            try {
                in.close();
            }catch (Exception e){
                e.printStackTrace();
            }

            System.out.println("min_x_: " + min_x_ + " min_y_: " + min_y_ + " max_x_: " + max_x_ + " max_y_: " + max_y_);
            System.out.println("read points: " + readPoints + " " + in.getNumberOfPointRecords());

            System.out.println("MATCHED CELLS:" + matchedCells.size());
            System.out.println("MATCHED CELLS:" + matchedCells.size());
            System.out.println("MATCHED CELLS:" + matchedCells.size());
            System.out.println("MATCHED CELLS:" + matchedCells.size());
            System.out.println("MATCHED CELLS:" + matchedCells.size());
            System.out.println("MATCHED CELLS:" + matchedCells.size());


            //ogr.RegisterAll(); //Registering all the formats..

            VertexValuatorDefault valuator = new VertexValuatorDefault();

            //TriangularFacetInterpolator polator = new TriangularFacetInterpolator(tin);

            ArrayList<Double> valuesToCheck = new ArrayList<>();
            for(int i : properCells){

                int y = (int)Math.floor(i / numberOfPixelsX);
                int x = i - y * numberOfPixelsX;

                //int x = i % numberOfPixelsX;
                //int y = i / numberOfPixelsX;

                //System.out.println((firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target) + " " + firstCheck[x][y].countTarget + " " + checkSurroundings(firstCheck, x, y) + " " + i);

                if((firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target) < 0.5 && firstCheck[x][y].countTarget > 10) { // && checkSurroundings(firstCheck, x, y)) {

                    //System.out.println("DIFFERENCE: " + (firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()));

                    double[] outValue = new double[]{firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()};

                    valuesToCheck.add((double) outValue[0]);

                }
            }

            double[] limits = new double[]{0.0, 0.0};

            ArrayList<Integer> outliers = getOutlierIndexes(valuesToCheck, 1.0, limits);
            //ArrayList<Integer> outliers = detectOutliersPercentile(valuesToCheck, 0.9);
            //System.exit(1);
            float maxOutlier = Float.NEGATIVE_INFINITY;
            float minOutlier = Float.POSITIVE_INFINITY;

            HashSet<Integer> outlierCells = new HashSet<>(outliers);

            for(int i = 0; i < outliers.size(); i++){


                if(valuesToCheck.get(outliers.get(i)) > maxOutlier)
                    maxOutlier = valuesToCheck.get(outliers.get(i)).floatValue();
                if(valuesToCheck.get(outliers.get(i)) < minOutlier)
                    minOutlier = valuesToCheck.get(outliers.get(i)).floatValue();

            }

            minOutlier = (float)limits[0];
            maxOutlier = (float)limits[1];

            System.out.println("outliers: " + outliers.size() + " " + maxOutlier + " " + minOutlier + " " + valuesToCheck.size());
            //System.exit(1);
            int counter = 0;

            for(int i : properCells){

                int y = (int)Math.floor(i / numberOfPixelsX);
                int x = i - y * numberOfPixelsX;

                //int x = i % numberOfPixelsX;
                //int y = i / numberOfPixelsX;

                if((firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target) < 0.5 && firstCheck[x][y].countTarget > 5) { //  && checkSurroundings(firstCheck, x, y)){

                    double[] outValue = new double[]{firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()};


                    //if(outValue[0] > maxOutlier || outValue[0] < minOutlier){
                    //    continue;
                    //}

                    //if(outlierCells.contains(counter++)) {
                    //    System.out.println("OUTLIER: " + i + " " + outValue[0] + " ");
                    //    continue;
                    //}



                    double centricity = firstCheck[x][y].groundCentricity();

                    //if(Math.abs(centricity - 0.5) > 0.2)
                    //    continue;
                    //System.ou t.println("DIFFERENCE: " + (firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()));



                    if(outValue[0] > 10.0) {
                        System.out.println("OUTVALUE: " + outValue[0] + " " + (firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target) + " " + firstCheck[x][y].countGround);
                    }

                    //tin.add(new org.tinfour.common.Vertex(x + resolution / 2.0, y - resolution / 2.0, outValue[0]));

                    tinM.addPointToTin(min_x + x * resolution + resolution / 2.0, max_y - y * resolution - resolution / 2.0, outValue[0] );
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


            //polator.resetForChangeToTin();

            double[][] smoothed = new double[numberOfPixelsX][numberOfPixelsY];


            for (int j = 0; j < numberOfPixelsX; j++) {
                for (int k = 0; k < numberOfPixelsY; k++) {

                    //float interpolatedValue = (float)polator.interpolate(j + resolution / 2.0, k - resolution / 2.0, valuator);

                    //float[] outValue = new float[]{interpolatedValue};

                    //smoothed[j][k] = interpolatedValue;

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
                    aR.pfac.prepareBuffer(thread_n, i, maxi);
                }catch (Exception e){
                    System.out.println("Failed to prepare buffer");
                    return;
                }
                for (int j_ = 0; j_ < maxi; j_++) {

                    in.readFromBuffer(tempPoint);

                    double x = (tempPoint.x - min_x) / resolution;
                    double y = (max_y - tempPoint.y) / resolution;

                    //double interpolatedValue = polator.interpolate(x, y, valuator);
/*
                    if(isPointInPolygon(tin.getPerimeter(), x, y) == Polyside.Result.Inside){
                        if(!Double.isNaN(interpolatedValue))
                            tempPoint.z -= interpolatedValue;
                        else
                            tempPoint.z -= averageCorrection;
                    }else
                        tempPoint.z -= averageCorrection;
*/

                    try {

                        aR.pfac.writePoint(tempPoint, i+j_, thread_n);

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }

                }
            }

            aR.pfac.closeThread(thread_n);
        //}
                    })).get();
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public void processTargetsLidar() throws Exception{

        ForkJoinPool customThreadPool = new ForkJoinPool(aR.cores);

        try {

            customThreadPool.submit(() ->

                    IntStream.range(0, targets.size()).parallel().forEach(i_ -> {


                        //for(int i_ = 0; i_ < targets.size(); i_++) {

                        double min_x = targets.get(i_).getMinX();
                        double max_y = targets.get(i_).getMaxY();
                        double max_x = targets.get(i_).getMaxX();
                        double min_y = targets.get(i_).getMinY();

                        //min_x = this.min_x;
                        //max_y = this.max_y;
                        //max_x = this.max_x;
                        //min_y = this.min_y;


                        for (int j = 0; j < targetPairsInRef.get(i_).size(); j++) {

                            if (refs.get(targetPairsInRef.get(i_).get(j)).getMinX() < min_x && refs.get(targetPairsInRef.get(i_).get(j)).getMinX() != 0.0) {
                                min_x = refs.get(targetPairsInRef.get(i_).get(j)).getMinX();
                            }
                            if (refs.get(targetPairsInRef.get(i_).get(j)).getMaxY() > max_y && refs.get(targetPairsInRef.get(i_).get(j)).getMaxY() != 0.0) {
                                max_y = refs.get(targetPairsInRef.get(i_).get(j)).getMaxY();
                            }

                            if (refs.get(targetPairsInRef.get(i_).get(j)).getMaxX() > max_x && refs.get(targetPairsInRef.get(i_).get(j)).getMaxX() != 0.0) {
                                max_x = refs.get(targetPairsInRef.get(i_).get(j)).getMaxX();
                            }
                            if (refs.get(targetPairsInRef.get(i_).get(j)).getMinY() < min_y && refs.get(targetPairsInRef.get(i_).get(j)).getMinY() != 0.0) {
                                min_y = refs.get(targetPairsInRef.get(i_).get(j)).getMinY();
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

                        System.out.println("target pairs: " + targetPairsInRef.get(i_).size());
                        System.out.println("origo_x: " + origo_x + " origo_y: " + origo_y);
                        System.out.println("numberOfPixelsX: " + numberOfPixelsX + " numberOfPixelsY: " + numberOfPixelsY + " " + resolution);

                        int thread_n = -1;

                        for (int j = 0; j < targetPairsInRef.get(i_).size(); j++) {


                            LASReader in = null;

                            try {
                                in = new LASReader(refs.get(targetPairsInRef.get(i_).get(j)).getFile());
                            }catch (Exception e){
                                e.printStackTrace();
                                System.exit(1);
                            }
                            thread_n = aR.pfac.addReadThread(in);


                            for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                                int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

                                try{
                                    aR.pfac.prepareBuffer(thread_n, i, maxi);
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
                                        firstCheck[x][y].addGround((double) tempPoint.z, (double)tempPoint.x, (double)tempPoint.y);
                                    } else
                                        firstCheck[x][y].addNonGround(tempPoint.z);

                                }

                            }

                            try {
                                in.close();
                            }catch (Exception e){
                                e.printStackTrace();
                                System.exit(1);
                            }
                        }

                        TreeSet<Integer> properCells = new TreeSet<>();

                        //boolean[][] propCells = new boolean[numberOfPixelsX][numberOfPixelsY];
                        int n_propers = 0;
                        int min_x_ = Integer.MAX_VALUE;
                        int min_y_ = Integer.MAX_VALUE;
                        int max_x_ = Integer.MIN_VALUE;
                        int max_y_ = Integer.MIN_VALUE;

                        // declare a file to write stuff in;
                        /*
            File file = new File("/home/koomikko/Documents/processing_directory/debug/properCells.txt");
            FileWriter fw = null;

            try {
                fw = new FileWriter(file.getAbsoluteFile());
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedWriter bw = new BufferedWriter(fw);


*/
                        for (int j = 0; j < numberOfPixelsX; j++) {
                            for (int k = 0; k < numberOfPixelsY; k++) {

                                if (firstCheck[j][k].getGroundCount() > min_points_in_cell) {
                                    //System.out.println("Ground mean: " + firstCheck[j][k].getGroundMean() + " non-ground mean: " + firstCheck[j][k].getNonGroundMean());
                                    double meanGround = firstCheck[j][k].getGroundMean();
                                    double meanNonGround = firstCheck[j][k].getNonGroundMean();

                                    //if (Double.isNaN(meanNonGround) || Math.abs(meanGround - meanNonGround) < 0.5) {

                                        //if(firstCheck[j][k].max_ground - firstCheck[j][k].min_ground < 5.0)
                                       //     if( firstCheck[j][k].countGround / (firstCheck[j][k].countGround + firstCheck[j][k].countNonGround) > 0.90 ||
                                       //             Math.abs(meanGround - meanNonGround) < 0.2) {


                                                if(j < min_x_)
                                                    min_x_ = j;
                                                if(j > max_x_)
                                                    max_x_ = j;
                                                if(k < min_y_)
                                                    min_y_ = k;
                                                if(k > max_y_)
                                                    max_y_ = k;
/*
                                    try {
                                        bw.write((origo_x+j*resolution) + " " + (origo_y-k*resolution) + "\n");
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
*/
                                                properCells.add(j + k * numberOfPixelsX);
                                                //propCells[j][k] = true;
                                                n_propers++;
                                                //System.out.println("proper coords: " + (origo_x+j*resolution) + " " + (origo_y-k*resolution));
                                           // }
                                   // }
                                }
                            }
                        }
/*
            try {
                bw.close();
                fw.close();
            }catch (Exception e){
                e.printStackTrace();
            }

 */
                        System.out.println("min_x: " + min_x_ + " max_x: " + max_x_ + " min_y: " + min_y_ + " max_y: " + max_y_);
                        System.out.println("Proper cells: " + properCells.size() + " " + n_propers);
                        System.out.println("origo_x: " + origo_x + " origo_y: " + origo_y + " " + resolution);
                        HashSet<Integer> matchedCells = new HashSet<>();
                        LASReader in = null;

                        try {
                            in = new LASReader(targets.get(i_).getFile());
                        }catch (Exception e){
                            e.printStackTrace();
                            System.exit(1);
                        }

                        tempPoint = new LasPoint();
                        int counterPoints = 0;
                        int readPoints = 0;

                        min_x_ = Integer.MAX_VALUE;
                        min_y_ = Integer.MAX_VALUE;
                        max_x_ = Integer.MIN_VALUE;
                        max_y_ = Integer.MIN_VALUE;

                        System.out.println("Processing: " + in.getFile().getName());

                        thread_n = aR.pfac.addReadThread(in);

                        for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                            int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

                            try {
                                aR.pfac.prepareBuffer(thread_n, i, maxi);
                            }catch (Exception e){
                                e.printStackTrace();
                                System.exit(1);
                            }
/*
                try{
                    aR.pfac.prepareBuffer(thread_n, i, maxi);
                }
                catch (Exception e){

                    e.printStackTrace();
                    System.exit(1);

                }

 */

                            for (int j_ = 0; j_ < maxi; j_++) {

                                // i : 14520000 j_: 11319 maxi: 12491 numpoints: 14532491 currentProgress 14531319
                                //System.out.println("i : " + i + " j_: " + j_ + " maxi: " + maxi + " numpoints: " + in.getNumberOfPointRecords() + " currentProgress " + (i+j_));
                                try {
                                    in.readFromBuffer(tempPoint);
                                }catch (Exception e){
                                    e.printStackTrace();

                                    System.exit(1);
                                }

                                if(tempPoint.classification != 2)
                                    continue;

                                int x = (int) Math.floor((tempPoint.x - origo_x) / resolution);
                                int y = (int) Math.floor((origo_y - tempPoint.y) / resolution);

                                if(x < 0 || x >= numberOfPixelsX || y < 0 || y >= numberOfPixelsY) {

                                    continue;

                                }

                                if(x < min_x_)
                                    min_x_ = x;
                                if(x > max_x_)
                                    max_x_ = x;
                                if(y < min_y_)
                                    min_y_ = y;
                                if(y > max_y_)
                                    max_y_ = y;

                                //415960.0 6945859.99

                                //System.out.println("x: " + x + " y: " + y);

                                int searchThis = x + y * numberOfPixelsX;

                                if (properCells.contains(searchThis)) {
                                    //if(propCells[x][y]){

                                    readPoints++;
                                    matchedCells.add(x + y * numberOfPixelsX);
                                    firstCheck[x][y].addTarget((double) tempPoint.z, (double)tempPoint.x, (double)tempPoint.y);

                                }

                            }
                        }

                        try {
                            in.close();
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                        System.out.println("min_x_: " + min_x_ + " min_y_: " + min_y_ + " max_x_: " + max_x_ + " max_y_: " + max_y_);
                        System.out.println("read points: " + readPoints + " " + in.getNumberOfPointRecords());

                        System.out.println("MATCHED CELLS:" + matchedCells.size());
                        System.out.println("MATCHED CELLS:" + matchedCells.size());
                        System.out.println("MATCHED CELLS:" + matchedCells.size());
                        System.out.println("MATCHED CELLS:" + matchedCells.size());
                        System.out.println("MATCHED CELLS:" + matchedCells.size());
                        System.out.println("MATCHED CELLS:" + matchedCells.size());


                        //ogr.RegisterAll(); //Registering all the formats..

                        VertexValuatorDefault valuator = new VertexValuatorDefault();

                        //TriangularFacetInterpolator polator = new TriangularFacetInterpolator(tin);

                        ArrayList<Double> valuesToCheck = new ArrayList<>();
                        for(int i : properCells){

                            int y = (int)Math.floor(i / numberOfPixelsX);
                            int x = i - y * numberOfPixelsX;

                            //int x = i % numberOfPixelsX;
                            //int y = i / numberOfPixelsX;

                            //System.out.println((firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target) + " " + firstCheck[x][y].countTarget + " " + checkSurroundings(firstCheck, x, y) + " " + i);

                            if((firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target) < 5.0 && firstCheck[x][y].countTarget > min_points_in_cell) { // && checkSurroundings(firstCheck, x, y)) {

                                //System.out.println("DIFFERENCE: " + (firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()));

                                double[] outValue = new double[]{firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()};

                                valuesToCheck.add((double) outValue[0]);

                            }
                        }

                        double[] limits = new double[]{0.0, 0.0};

                        ArrayList<Integer> outliers = getOutlierIndexes(valuesToCheck, 1.0, limits);
                        //ArrayList<Integer> outliers = detectOutliersPercentile(valuesToCheck, 0.9);
                        //System.exit(1);
                        float maxOutlier = Float.NEGATIVE_INFINITY;
                        float minOutlier = Float.POSITIVE_INFINITY;

                        HashSet<Integer> outlierCells = new HashSet<>(outliers);

                        for(int i = 0; i < outliers.size(); i++){


                            if(valuesToCheck.get(outliers.get(i)) > maxOutlier)
                                maxOutlier = valuesToCheck.get(outliers.get(i)).floatValue();
                            if(valuesToCheck.get(outliers.get(i)) < minOutlier)
                                minOutlier = valuesToCheck.get(outliers.get(i)).floatValue();

                        }

                        minOutlier = (float)limits[0];
                        maxOutlier = (float)limits[1];

                        System.out.println("outliers: " + outliers.size() + " " + maxOutlier + " " + minOutlier + " " + valuesToCheck.size());
                        //System.exit(1);
                        int counter = 0;

                        for(int i : properCells){

                            int y = (int)Math.floor(i / numberOfPixelsX);
                            int x = i - y * numberOfPixelsX;

                            //int x = i % numberOfPixelsX;
                            //int y = i / numberOfPixelsX;

                            if((firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target) < 5.0 && firstCheck[x][y].countTarget > min_points_in_cell) { //  && checkSurroundings(firstCheck, x, y)){

                                double[] outValue = new double[]{firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()};


                                //if(outValue[0] > maxOutlier || outValue[0] < minOutlier){
                                //    continue;
                                //}

                                //if(outlierCells.contains(counter++)) {
                                //    System.out.println("OUTLIER: " + i + " " + outValue[0] + " ");
                                //    continue;
                                //}



                                double centricity = firstCheck[x][y].groundCentricity();

                                //if(Math.abs(centricity - 0.5) > 0.2)
                                //    continue;
                                System.out.println("DIFFERENCE: " + (firstCheck[x][y].getTargetMean() - firstCheck[x][y].getGroundMean()));



                                if(outValue[0] > 10.0) {
                                    System.out.println("OUTVALUE: " + outValue[0] + " " + (firstCheck[x][y].max_z_target - firstCheck[x][y].min_z_target) + " " + firstCheck[x][y].countGround);
                                }

                                //tin.add(new org.tinfour.common.Vertex(x + resolution / 2.0, y - resolution / 2.0, outValue[0]));

                                tinM.addPointToTin(min_x + x * resolution + resolution / 2.0, max_y - y * resolution - resolution / 2.0, outValue[0] );
                                counter++;
                                //tinNodes.add(new double[]{ this.min_x + x * resolution + resolution / 2.0, this.max_y - y * resolution - resolution / 2.0, outValue[0] });
                                //band.WriteRaster(x, y, 1, 1, outValue);

                            }

                        }

                        System.out.println("counter: " + counter);


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


                        //polator.resetForChangeToTin();

                        double[][] smoothed = new double[numberOfPixelsX][numberOfPixelsY];


                        for (int j = 0; j < numberOfPixelsX; j++) {
                            for (int k = 0; k < numberOfPixelsY; k++) {

                                //float interpolatedValue = (float)polator.interpolate(j + resolution / 2.0, k - resolution / 2.0, valuator);

                                //float[] outValue = new float[]{interpolatedValue};

                                //smoothed[j][k] = interpolatedValue;

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
                                aR.pfac.prepareBuffer(thread_n, i, maxi);
                            }catch (Exception e){
                                System.out.println("Failed to prepare buffer");
                                return;
                            }
                            for (int j_ = 0; j_ < maxi; j_++) {

                                in.readFromBuffer(tempPoint);

                                double x = (tempPoint.x - min_x) / resolution;
                                double y = (max_y - tempPoint.y) / resolution;

                                //double interpolatedValue = polator.interpolate(x, y, valuator);
/*
                    if(isPointInPolygon(tin.getPerimeter(), x, y) == Polyside.Result.Inside){
                        if(!Double.isNaN(interpolatedValue))
                            tempPoint.z -= interpolatedValue;
                        else
                            tempPoint.z -= averageCorrection;
                    }else
                        tempPoint.z -= averageCorrection;
*/

                                try {

                                    aR.pfac.writePoint(tempPoint, i+j_, thread_n);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }

                            }
                        }

                        aR.pfac.closeThread(thread_n);
                        //}
                    })).get();
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public void removeOutliersFromTin(){



    }

    public boolean checkSurroundings(dataPointTiny[][] input, int x, int y){

        int x_ = 0, y_ = 0;

        for(int x__ = x - 1; x__ <= x + 1; x__++){

            if(x__ < 0 || x__ >= input.length)
                continue;


            for(int y__ = y - 1; y__ <= y + 1; y__++){

                if(y__ < 0 || y__ >= input[0].length)
                    continue;

                if(x__ == x && y__ == y)
                    continue;

                if(input[x__][y__].countTarget == 0)
                    return false;
            }
        }

        return true;

    }

    public static ArrayList<Integer> detectOutliersPercentile(ArrayList<Double> data, double threshold) {
        ArrayList<Integer> outliers = new ArrayList<>();
        double cutoff_top = calculateCutoff(data, threshold);
        double cutoff_bottom = calculateCutoff(data, 1.0 - threshold);
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) > cutoff_top || data.get(i) < cutoff_bottom){
                outliers.add(i);
            }
        }
        return outliers;
    }

    private static double calculateCutoff(ArrayList<Double> data, double threshold) {
        ArrayList<Double> sortedData = new ArrayList<>(data);
        sortedData.sort(null);
        int cutoffIndex = (int) Math.ceil(threshold * sortedData.size());
        return sortedData.get(cutoffIndex);
    }

    public float[][] readRasterTo2DArray(Dataset tifDataset, int xDim, int yDim){

        float[][] output = new float[xDim][yDim];

        Band band = tifDataset.GetRasterBand(1);

        double[] geoTransform = tifDataset.GetGeoTransform();
        float[] floatArray = new float[xDim];

        for(int y = 0; y < yDim; y++) {

            band.ReadRaster(0, y, xDim, 1, floatArray);

            for (int x = 0; x < xDim; x++) {

                float value = floatArray[x];

                output[x][y] = value;
            }
        }

        return output;
    }


    public void applyCorrection(){

        LasPoint tempPoint = new LasPoint();

        Dataset tifDataset = gdal.Open(tinM.tiff_file_name, gdalconst.GA_ReadOnly);


        int xDim = tifDataset.getRasterXSize();
        int yDim = tifDataset.getRasterYSize();

        System.out.println("reading raster to file!");
        float[][] raster = readRasterTo2DArray(tifDataset, tifDataset.getRasterXSize(), tifDataset.getRasterYSize());

        System.out.println("Done reading raster");
        Band band = tifDataset.GetRasterBand(1);
        Double[] nodata = new Double[1];
        band.GetNoDataValue(nodata);

        System.out.println(nodata[0].floatValue());
        
        double[] geoTransform = tifDataset.GetGeoTransform();

        band.delete();
        tifDataset.delete();

        System.setProperty("GDAL_CACHEMAX", "500000000");
        
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

            float[] data = new float[1];

            for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

                try {
                    aR.pfac.prepareBuffer(thread_n, i, maxi);
                }catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
                for (int j_ = 0; j_ < maxi; j_++) {

                    in.readFromBuffer(tempPoint);

                    int x = (int) Math.round((tempPoint.x - geoTransform[0]) / geoTransform[1]);
                    int y = (int) Math.round((tempPoint.y - geoTransform[3]) / geoTransform[5]);

                    //if(x <= aR.kernel || x >= xDim-aR.kernel || y <= aR.kernel || y >= yDim-aR.kernel)
                    if(x < 0 || x >= xDim || y < 0 || y >= yDim)
                        continue;

                    //band.ReadRaster(x, y, 1, 1, data);

                    if(!Float.isNaN(raster[x][y])){

                            tempPoint.z -= raster[x][y];

                            try {

                                aR.pfac.writePoint(tempPoint, i+j_, thread_n);

                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(1);
                            }

                    }else{

                    }

                }
            }
            aR.pfac.closeThread(thread_n);

            try{
                in.close();
                in = null;
                System.gc();
            }catch (Exception e) {
                e.printStackTrace();
            }

        }

    }


    public static ArrayList<Integer> getOutlierIndexes(ArrayList<Double> values, double zScoreThreshold, double[] limits) {
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

        limits[0] = mean - stdDev;
        limits[1] = mean + stdDev;

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
    public double sum_z_ground = 0, sum_z_nonGround = 0, sum_z_target = 0, max_z_target = Float.NEGATIVE_INFINITY, min_z_target = Float.POSITIVE_INFINITY;

    public double max_ground = Double.NEGATIVE_INFINITY, min_ground = Double.POSITIVE_INFINITY;
    public double sum_x = 0, sum_y = 0;
    public double sum_x_ground = 0, sum_y_ground = 0;

    dataPointTiny(){

    }

    public void addTarget(double z, double x, double y){
        countTarget++;
        sum_z_target += z;

        if(z > max_z_target)
            max_z_target = z;

        if(z < min_z_target)
            min_z_target = z;

        sum_x += x;
        sum_y += y;
    }

    public double getMeanTargetX(){
        return sum_x / countTarget;
    }

    public double getMeanTargetY(){
        return sum_y / countTarget;
    }

    public double getTargetMean(){

        return sum_z_target / countTarget;

    }
    public void addGround(double z, double x, double y){
        countGround++;
        sum_z_ground += z;

        sum_x_ground += x;
        sum_y_ground += y;

        if(z > this.max_ground)
            this.max_ground = z;

        if(z < this.min_ground)
            this.min_ground = z;

    }

    public double getMeanGroundX(){
        return sum_x_ground / countGround;
    }

    public double getMeanGroundY(){
        return sum_y_ground / countGround;
    }

    public double groundCentricity(){

        double x = getMeanGroundX() % 1;
        double y = getMeanGroundY() % 1;

        double x_ = getMeanTargetX() % 1;
        double y_ = getMeanTargetY() % 1;

        double dist1 = euclideanDistance2d(x, y, 0.5, 0.5);
        double dist2 = euclideanDistance2d(x_, y_, 0.5, 0.5);

        return (dist1 + dist2) / 2.0;

    }

    public void addNonGround(double z){
        countNonGround++;
        sum_z_nonGround += z;
    }

    public int getGroundCount(){
        return countGround;
    }

    public int getNonGroundCount(){
        return countNonGround;
    }

    public double getGroundMean(){
        return sum_z_ground / countGround;

    }

    public double getNonGroundMean(){
        return sum_z_nonGround / countNonGround;
    }

    public float euclideanDistance2d(double x1, double y1, double x2, double y2){
        return (float) Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    }
}
