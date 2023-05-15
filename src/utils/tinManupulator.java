package utils;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.Vertex;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.interpolation.VertexValuatorDefault;
import org.tinfour.utils.Polyside;
import tools.GaussianSmooth;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


import static org.tinfour.utils.Polyside.isPointInPolygon;

public class tinManupulator {

    List<IQuadEdge> perimeter = new ArrayList<>();
    public String tiff_file_name = null;

    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
    VertexValuatorDefault valuator = new VertexValuatorDefault();

    argumentReader aR;

    TriangularFacetInterpolator polator = new TriangularFacetInterpolator(tin);

    public double minx = 0, miny = 0, minz = 0, maxx = 0, maxy = 0, maxz = 0;
    public tinManupulator(argumentReader aR){

        this.aR = aR;
    }

    public synchronized void addPointToTin(double x, double y, double z){

        tin.add(new org.tinfour.common.Vertex(x, y, z));

        //System.out.println(tin.getVertices().size());
    }

    public void prepareTin(){
        polator.resetForChangeToTin();
    }

    public void printTimeInMinutesSeconds(long timeInMilliseconds, String message){
        System.out.println(message + " -- Time taken: " + (timeInMilliseconds / 1000) / 60 + " minutes and " + (timeInMilliseconds / 1000) % 60 + " seconds.");
    }
    public void writeTinToFile(File outputFile, double resolution){

        System.out.println("Writing TIN to file... ");
        System.out.println("tin size: " + tin.getVertices().size());

        // Start timer
        long startTime = System.currentTimeMillis();

        polator.resetForChangeToTin();

        int numberOfPixelsX = (int) Math.ceil((maxx - minx) / resolution);
        int numberOfPixelsY = (int) Math.ceil((maxy - miny) / resolution);

        System.out.println(numberOfPixelsX + " " + numberOfPixelsY);

        System.out.println(minx + " " + miny + " " + maxx + " " + maxy);
        double[] geoTransform = new double[]{minx, resolution, 0.0, maxy, 0.0, -resolution};

        SpatialReference sr = new SpatialReference();

        sr.ImportFromEPSG(aR.EPSG);

        gdal.AllRegister();

        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();
        String outputFileName = null;
        try {
            outputFileName = aR.createOutputFileWithExtension(outputFile, "_correctionRaster.tif").getAbsolutePath();
        }catch (Exception e){
            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
            return;
        }


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

        this.tiff_file_name = outputFileName;

        dataset_output.SetProjection(sr.ExportToWkt());
        dataset_output.SetGeoTransform(geoTransform);

        double[][] smoothed = new double[numberOfPixelsX][numberOfPixelsY];

        for (int j = 0; j < numberOfPixelsX; j++) {
            for (int k = 0; k < numberOfPixelsY; k++) {

                smoothed[j][k] = Float.NaN;

            }
        }

        this.perimeter = tin.getPerimeter();

        for (int j = 0; j < numberOfPixelsX; j++) {
            for (int k = 0; k < numberOfPixelsY; k++) {

                boolean pointInTin = isPointInTin(minx + j * resolution + resolution / 2.0, maxy - k * resolution - resolution / 2.0);

                float interpolatedValue = 0;

                if(pointInTin)
                    interpolatedValue = (float)polator.interpolate(minx + j * resolution + resolution / 2.0, maxy - k * resolution - resolution / 2.0, valuator);
                else
                    interpolatedValue = Float.NaN;

                float[] outValue = new float[]{interpolatedValue};

                smoothed[j][k] = interpolatedValue;

                //band.WriteRaster(j, k, 1, 1, outValue);
            }
        }

        // Start timer
        long endTime = System.currentTimeMillis();

        printTimeInMinutesSeconds(endTime - startTime, "Interpolation");

        startTime = System.currentTimeMillis();

        if(aR.theta != 0.0)
            aR.kernel = (int)( 2.0 * Math.ceil( 3.0 * aR.theta) + 1.0);

        //System.out.println("Kernel: " + aR.kernel + " theta: " + aR.theta);
        //System.out.println(smoothed.length + " " + smoothed[0].length);
        //double[][] mirrored_tmp = mirrorAll(smoothed, aR.kernel);

        //System.out.println(mirrored_tmp.length + " " + mirrored_tmp[0].length);
        smoothed = GaussianSmooth.smooth(smoothed, numberOfPixelsX, numberOfPixelsY, aR.kernel, aR.theta);  // THIS IS GOOD!!!! :)
        //System.out.println(mirrored_tmp.length + " " + mirrored_tmp[0].length);
        //smoothed = unmirrorAll(mirrored_tmp, aR.kernel);

        //System.out.println(smoothed.length + " " + smoothed[0].length);
        //System.out.println(aR.kernel);

        //System.exit(1);
        startTime = System.currentTimeMillis();

        // End timer
        endTime = System.currentTimeMillis();

        printTimeInMinutesSeconds(endTime - startTime, "Smoothing");

        startTime = System.currentTimeMillis();

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

        endTime = System.currentTimeMillis();

        printTimeInMinutesSeconds(endTime - startTime, "Writing to file");

        double averageCorrection = sum / count;

        //System.out.println("AVERAGE: " + sum / count);

        band.FlushCache();
        dataset_output.delete();

        ProcessBuilder pb = new ProcessBuilder("gdal_fillnodata.py", outputFileName, "-md", "10000", outputFileName);
        pb.redirectErrorStream(true);
        Process process = null;
        List<String> command = pb.command();
        System.out.println("Command: " + String.join(" ", command));

        try {
           process = pb.start();
        }catch (Exception e){
            e.printStackTrace();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
            process.destroy();
        }catch (Exception e){
            e.printStackTrace();
        }
        //System.exit(1);
    }

    public static double[][] mirrorAll(double[][] array, int bufferPixels) {
        int width = array[0].length;
        int height = array.length;
        int newWidth = width + bufferPixels * 2;
        int newHeight = height + bufferPixels * 2;
        double[][] result = new double[newHeight][newWidth];

        // Copy the original array to the center of the new array
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y + bufferPixels][x + bufferPixels] = array[y][x];
            }
        }

        // Mirror the top and bottom sides of the array
        for (int y = 0; y < bufferPixels; y++) {
            int topIndex = bufferPixels - y;
            int bottomIndex = newHeight - bufferPixels + y - 1;
            for (int x = 0; x < newWidth; x++) {
                result[topIndex][x] = result[bufferPixels + y][x];
                result[bottomIndex][x] = result[newHeight - bufferPixels - y - 1][x];
            }
        }

        // Mirror the left and right sides of the array
        for (int x = 0; x < bufferPixels; x++) {
            int leftIndex = bufferPixels - x;
            int rightIndex = newWidth - bufferPixels + x - 1;
            for (int y = 0; y < newHeight; y++) {
                result[y][leftIndex] = result[y][bufferPixels + x];
                result[y][rightIndex] = result[y][newWidth - bufferPixels - x - 1];
            }
        }

        return result;
    }

    public static double[][] unmirrorAll(double[][] arr, int buffer) {
        int height = arr.length - 2*buffer;
        int width = arr[0].length - 2*buffer;
        double[][] result = new double[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                result[i][j] = arr[i+buffer][j+buffer];
            }
        }
        return result;
    }

    public synchronized double interpolate(double x, double y){

        return polator.interpolate(x, y, valuator);

    }

    public synchronized boolean isPointInTin(double x, double y){

        return isPointInPolygon(this.perimeter, x, y) == Polyside.Result.Inside;

    }

    public void removeOutliers(){

        ArrayList<Double> zValues = new ArrayList<>();

        for(Vertex v : tin.getVertices()){

            zValues.add(v.getZ());

        }

        ArrayList<Integer> outlierIndexes = getOutlierIndexes(zValues, 2.5);

        HashSet<Integer> removeThese = new HashSet<>(outlierIndexes);

        org.tinfour.standard.IncrementalTin tin2 = new org.tinfour.standard.IncrementalTin();

        int counter = 0;

        for(Vertex v : tin.getVertices()){

            if(!removeThese.contains(counter++)){
                tin2.add(new Vertex(v.getX(), v.getY(), v.getZ()));
            }
        }

        tin = tin2;

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

}
