package utils;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.interpolation.VertexValuatorDefault;
import org.tinfour.utils.Polyside;
import tools.GaussianSmooth;

import java.awt.geom.Rectangle2D;
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

    Layer tinBeforeFiltering = null;
    Layer tinAfterFiltering = null;

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

        if(perimeter.size() > 3) {
            for (int j = 0; j < numberOfPixelsX; j++) {
                for (int k = 0; k < numberOfPixelsY; k++) {

                    boolean pointInTin = isPointInTin(minx + j * resolution + resolution / 2.0, maxy - k * resolution - resolution / 2.0);

                    float interpolatedValue = 0;

                    if (pointInTin)
                        interpolatedValue = (float) polator.interpolate(minx + j * resolution + resolution / 2.0, maxy - k * resolution - resolution / 2.0, valuator);
                    else
                        interpolatedValue = Float.NaN;

                    float[] outValue = new float[]{interpolatedValue};

                    smoothed[j][k] = interpolatedValue;

                    //band.WriteRaster(j, k, 1, 1, outValue);
                }
            }
        }else{
            for (int j = 0; j < numberOfPixelsX; j++) {
                for (int k = 0; k < numberOfPixelsY; k++) {

                    smoothed[j][k] = 0.0f;

                }
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
        startTime = System.currentTimeMillis();

        //System.out.println(mirrored_tmp.length + " " + mirrored_tmp[0].length);
        smoothed = GaussianSmooth.smooth(smoothed, numberOfPixelsX, numberOfPixelsY, aR.kernel, aR.theta);  // THIS IS GOOD!!!! :)
        //System.out.println(mirrored_tmp.length + " " + mirrored_tmp[0].length);
        //smoothed = unmirrorAll(mirrored_tmp, aR.kernel);

        //System.out.println(smoothed.length + " " + smoothed[0].length);
        //System.out.println(aR.kernel);

        //System.exit(1);

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

        this.declareShapefiles();

        ArrayList<Double> zValues = new ArrayList<>();


        ArrayList<double[]> perimeterEdges = new ArrayList<>();

        perimeterEdges.add(new double[] {this.minx, this.maxy, this.maxx, this.maxy});
        perimeterEdges.add(new double[] {this.maxx, this.maxy, this.maxx, this.miny});
        perimeterEdges.add(new double[] {this.maxx, this.miny, this.minx, this.miny});
        perimeterEdges.add(new double[] {this.minx, this.miny, this.minx, this.maxy});


        List<IQuadEdge> perim = tin.getPerimeter();

        ArrayList<double[]> originalTinEdges = new ArrayList<>();

        double maxDistanceToPerimeter = Double.NEGATIVE_INFINITY;

        Geometry outShpGeom2 = new Geometry(ogr.wkbLinearRing);
        Geometry outShpGeom = new Geometry(ogr.wkbPolygon);

        double meanx = 0;
        double meany = 0;


        for(IQuadEdge e : perim){

            double[] edge = new double[]{e.getA().getX(), e.getA().getY(), e.getB().getX(), e.getB().getY()};
            outShpGeom2.AddPoint_2D(e.getA().getX(), e.getA().getY());
            outShpGeom2.AddPoint_2D(e.getB().getX(), e.getB().getY());

            meanx += e.getA().getX();
            meanx += e.getB().getX();

            meany += e.getA().getY();
            meany += e.getB().getY();

            originalTinEdges.add(edge);

            double dista = 0;// distanceToExtent__(perimeterEdges, edge[0], edge[1], edge[2], edge[3]);

            if(dista > maxDistanceToPerimeter){
                maxDistanceToPerimeter = dista;
            }

        }
        outShpGeom.AddGeometry(outShpGeom2);
        FeatureDefn outShpFeatDefn = tinBeforeFiltering.GetLayerDefn();
        Feature feature = new Feature(outShpFeatDefn);
        feature.SetGeometryDirectly(outShpGeom);
        feature.SetField("id", 1);

        tinBeforeFiltering.CreateFeature(feature);

        tinBeforeFiltering.SyncToDisk();
        System.out.println("Max distance to perimeter (prior): " + maxDistanceToPerimeter + " tin perimeter size: " + perim.size());
        System.out.println("Mean x: " + meanx / (perim.size() * 2));
        System.out.println("Mean y: " + meany / (perim.size() * 2));


        for(Vertex v : tin.getVertices()){

            zValues.add(v.getZ());

        }

        double[] limits = new double[]{0.0, 0.0};
        ArrayList<Integer> outlierIndexes = getOutlierIndexes(zValues, 1.0, limits);

        double min = limits[0];
        double max = limits[1];

        if(outlierIndexes.size() == 0){
            min = Double.NEGATIVE_INFINITY;
            max = Double.POSITIVE_INFINITY;
        }


        HashSet<Integer> removeThese = new HashSet<>(outlierIndexes);

        org.tinfour.standard.IncrementalTin tin2 = new org.tinfour.standard.IncrementalTin();

        int counter = 0;

        String logFileString = "";

        logFileString += "Original nodes: " + "\n";

        double meanAllNodes = 0.0;
        double sdAllNodes = 0.0;
        double sumAllNodes = 0.0;

        double minAllNodes = Double.POSITIVE_INFINITY;
        double maxAllNodes = Double.NEGATIVE_INFINITY;
        double areaOriginal = 0.0;
        double minDistanceFromBoundingBox = Double.POSITIVE_INFINITY;

        for(SimpleTriangle t : tin.triangles()){

            areaOriginal += (t.getArea() / 10000);

        }


        //if(false)
        for(Vertex v : tin.getVertices()){

            if(v.getZ() > min && v.getZ() < max){
                //removeThese.add(counter);
                tin2.add(new Vertex(v.getX(), v.getY(), v.getZ()));
            }
/*
            double distanceFromBoundingbox = this.calculateDistance(v.getX(), v.getY());

            //System.out.println("Distance from bounding box: " + distanceFromBoundingbox);
            if(distanceFromBoundingbox < minDistanceFromBoundingBox){
                minDistanceFromBoundingBox = distanceFromBoundingbox;
            }
*/
            meanAllNodes += v.getZ();
            sumAllNodes += (v.getZ() * v.getZ());

            if(v.getZ() < minAllNodes){
                minAllNodes = v.getZ();
            }

            if(v.getZ() > maxAllNodes){
                maxAllNodes = v.getZ();
            }


            //if(!removeThese.contains(counter++)){
            //    tin2.add(new Vertex(v.getX(), v.getY(), v.getZ()));
            //}
        }



        logFileString += "\tn: " + tin.getVertices().size() + "\n";


        meanAllNodes /= tin.getVertices().size();
        sdAllNodes = Math.sqrt((sumAllNodes / tin.getVertices().size()) - Math.pow(meanAllNodes, 2));


        logFileString += "\tMean: " + meanAllNodes + "\n";

        logFileString += "\tMin: " + minAllNodes + "\n";
        logFileString += "\tMax: " + maxAllNodes + "\n";

        logFileString += "\tSD: " + sdAllNodes + "\n";

        logFileString += "\tArea: " + areaOriginal + " ha " + "\n";
        logFileString += "\tMax distance from bounding box: " + maxDistanceToPerimeter + " m " + "\n";



        tin.clear();

        logFileString += "Nodes after outlier removal: " + "\n";

        double mean = 0.0;
        double sd = 0.0;
        double sum = 0.0;

        double area = 0.0;

        double minDistanceFromBoundingBox2 = Double.POSITIVE_INFINITY;


        for(SimpleTriangle t : tin2.triangles()){

                area += (t.getArea() / 10000);

        }

        double minNodes = Double.POSITIVE_INFINITY;
        double maxNodes = Double.NEGATIVE_INFINITY;

        //if(false)
        for(Vertex v : tin2.getVertices()){

            tin.add(new Vertex(v.getX(), v.getY(), v.getZ()));

            mean += v.getZ();
            sum += (v.getZ() * v.getZ());

            if(v.getZ() < minNodes){
                minNodes = v.getZ();
            }

            if(v.getZ() > maxNodes){
                maxNodes = v.getZ();
            }


        }

        double maxDistanceToPerimeter2 = Double.NEGATIVE_INFINITY;


        Geometry outShpGeom3 = new Geometry(ogr.wkbLinearRing);
        Geometry outShpGeom4 = new Geometry(ogr.wkbPolygon);

        meanx = 0.0;
        meany = 0.0;


        for(IQuadEdge e : tin.getPerimeter()){

            double[] edge = new double[]{e.getA().getX(), e.getA().getY(), e.getB().getX(), e.getB().getY()};

            meanx += e.getA().getX();
            meanx += e.getB().getX();

            meany += e.getA().getY();
            meany += e.getB().getY();

            outShpGeom3.AddPoint_2D(e.getA().getX(), e.getA().getY());
            outShpGeom3.AddPoint_2D(e.getB().getX(), e.getB().getY());

            double dista = 0; // distanceToExtent__(perimeterEdges, edge[0], edge[1], edge[2], edge[3]);

            if(dista > maxDistanceToPerimeter2){
                maxDistanceToPerimeter2 = dista;
            }

        }

        outShpGeom4.AddGeometryDirectly(outShpGeom3);
        FeatureDefn outShpFeatDefn1 = tinAfterFiltering.GetLayerDefn();
        Feature feature1 = new Feature(outShpFeatDefn1);
        feature1.SetGeometryDirectly(outShpGeom4);
        feature1.SetField("id", 1);

        tinAfterFiltering.CreateFeature(feature1);

        tinAfterFiltering.SyncToDisk();


        System.out.println("Max distance to perimeter (after): " + maxDistanceToPerimeter2 + " tin perim size: " + tin.getPerimeter().size());
        System.out.println("Mean x: " + meanx / (tin.getPerimeter().size() * 2));
        System.out.println("Mean y: " + meany / (tin.getPerimeter().size() * 2));

        mean /= tin.getVertices().size();
        sd = Math.sqrt((sum / tin.getVertices().size()) - Math.pow(mean, 2));

        logFileString += "\tn: " + tin.getVertices().size() + "\n";
        logFileString += "\tMean: " + mean + "\n";

        logFileString += "\tMin: " + minNodes + "\n";
        logFileString += "\tMax: " + maxNodes + "\n";

        logFileString += "\tSD: " + sd + "\n";
        logFileString += "\tArea: " + area + " ha " + "\n";
        logFileString += "\tMax distance from bounding box: " + maxDistanceToPerimeter2 + " m " + "\n";


        aR.writeLineToLogFile(logFileString);

        tin2.dispose();

    }

    public double distanceToExtent(ArrayList<double[]> extent, double x1, double y1, double x2, double y2){

        double minDistance = Double.POSITIVE_INFINITY;

        for(int i = 0; i < extent.size(); i++){

            double distance = calculateDistance(x1, y1, x2, y2, extent.get(i)[0], extent.get(i)[1], extent.get(i)[2], extent.get(i)[3]);

            double debugDistance = calculateDistance(1,1,1,5,5,1,15,5);
            System.out.println("DebugDistance " + debugDistance);
            System.exit(1);

            //System.out.println("disatnce: " + distance);
            if(distance < minDistance){
                minDistance = distance;
            }
        }

        //System.out.println("minDistance: " + minDistance);
        //System.out.println("-------------------");

        return minDistance;
    }

    public double distanceToExtent_(ArrayList<double[]> extent, double x1, double y1, double x2, double y2){

        double minDistance = Double.POSITIVE_INFINITY;

        for(int i = 0; i < extent.size(); i++){

            //double distance = calculateDistance(x1, y1, x2, y2, extent.get(i)[0], extent.get(i)[1], extent.get(i)[2], extent.get(i)[3]);
            double distance = calculateLargestDistance(x1, y1, x2, y2, extent.get(i)[0], extent.get(i)[1], extent.get(i)[2], extent.get(i)[3]);
            //System.out.println("disatnce: " + distance);
            if(distance < minDistance){
                minDistance = distance;
            }
        }

        //System.out.println("minDistance: " + minDistance);
        //System.out.println("-------------------");

        return minDistance;
    }

    public double distanceToExtent__(ArrayList<double[]> extent, double x1, double y1, double x2, double y2){

        double minDistance = Double.POSITIVE_INFINITY;
        double[] lineBound = pointsToLineEquation(x1, y1, x2, y2);

        boolean xDirection = false;
        boolean increment = false;


        if(x1 != x2){
            xDirection = true;
        }

        if(xDirection) {
            if (x1 < x2) {
                increment = true;
            }
        }else{
            if (y1 < y2) {
                increment = true;
            }
        }

        double slope = 0.0;

        if(xDirection){
            slope = (y2 - y1) / (x2 - x1);
        }else{
            slope = (x2 - x1) / (y2 - y1);
        }


        for(int i = 0; i < extent.size(); i++){

            //double distance = calculateDistance(x1, y1, x2, y2, extent.get(i)[0], extent.get(i)[1], extent.get(i)[2], extent.get(i)[3]);
            //double distance = calculateLargestDistance(x1, y1, x2, y2, extent.get(i)[0], extent.get(i)[1], extent.get(i)[2], extent.get(i)[3]);
            //double distance = calculateLargestDistance(x1, y1, x2, y2, extent.get(i)[0], extent.get(i)[1], extent.get(i)[2], extent.get(i)[3]);

            double[] lineExtent = pointsToLineEquation(extent.get(i)[0], extent.get(i)[1], extent.get(i)[2], extent.get(i)[3]);

            double distance = distanceBetweenLines(lineBound[0], lineBound[1], lineBound[2], lineExtent[0], lineExtent[1], lineExtent[2]);
            double distance_ = calculatePerpendicularDistance(lineExtent[0], lineExtent[1], lineExtent[2], x1, y1);

            double maxDistance_ = Double.NEGATIVE_INFINITY;

            if(xDirection){

                double xRange = Math.abs(x2 - x1);
                double xIncrement = xRange / 100;


                if(increment){

                    for(double x = x1; x < x2; x += xIncrement){

                        double y = slope * (x - x1) + y1;


                        double distance__ = calculatePerpendicularDistance(lineExtent[0], lineExtent[1], lineExtent[2], x, y);
                        //System.out.println("x " + x + " y " + y + " distance " + distance__);

                        if(distance__ > maxDistance_){
                            maxDistance_ = distance__;
                        }

                    }

                    //System.out.println("--------------------------");

                }else{

                        for(double x = x1; x > x2; x -= xIncrement){

                            double y = slope * (x - x1) + y1;

                            double distance__ = calculatePerpendicularDistance(lineExtent[0], lineExtent[1], lineExtent[2], x, y);
                            //System.out.println("x " + x + " y " + y + " distance " + distance__);
                            if(distance__ > maxDistance_){
                                maxDistance_ = distance__;
                            }
                            //System.out.println(increment + " x1 " + x1 + " x2 " + x2 + " " + x);
                        }

                    //System.out.println("--------------------------");

                }
            }else{

                double yRange = y2 - y1;
                double yIncrement = yRange / 100;

                if(increment){

                    for(double y = y1; y < y2; y += yIncrement){

                        double x = slope * (y - y1) + x1;

                        double distance__ = calculatePerpendicularDistance(lineExtent[0], lineExtent[1], lineExtent[2], x, y);

                        if(distance__ > maxDistance_){
                            maxDistance_ = distance__;
                        }

                    }

                }else{

                            for(double y = y1; y > y2; y -= yIncrement){

                                double x = slope * (y - y1) + x1;

                                double distance__ = calculatePerpendicularDistance(lineExtent[0], lineExtent[1], lineExtent[2], x, y);

                                if(distance__ > maxDistance_){
                                    maxDistance_ = distance__;
                                }

                            }
                }

            }

            //System.out.println("disatnce: " + distance);

            //System.out.println("maxDistance_: " + maxDistance_);
            //System.out.println("distance: " + distance);
            //System.out.println("minDistance: " + minDistance);
            if(maxDistance_ < minDistance){
                minDistance = maxDistance_;
            }
        }

        //System.out.println("minDistance: " + minDistance);
        //System.out.println("-------------------");
        //System.out.println(minDistance);

        //System.exit(1);
        return minDistance;
    }

    public double[] pointsToLineEquation(double x1, double y1, double x2, double y2){

        double[] output = new double[]{0,0,0};

        double a = y2 - y1;
        double b = x1 - x2;
        double c = (x2 * y1) - (x1 * y2);

        output[0] = a;
        output[1] = b;
        output[2] = c;

        return output;

    }

    public static double distanceBetweenLines(double A1, double B1, double C1, double A2, double B2, double C2) {
        // Iteratively calculate distance at various points
        double maxDistance = 0.0;

        for (int i = 0; i < 1000; i++) {
            double x = Math.random() * 100;
            double y = Math.random() * 100;

            double numerator = Math.abs(A1 * x + B1 * y + C1);
            double denominator = Math.sqrt(A1 * A1 + B1 * B1);
            double distance = numerator / denominator;

            if (distance > maxDistance) {
                maxDistance = distance;
            }
        }

        return maxDistance;
    }

    public static double calculatePerpendicularDistance(double A, double B, double C, double px, double py) {
        // Calculate the numerator and denominator of the perpendicular distance formula
        double numerator = Math.abs(A * px + B * py + C);
        double denominator = Math.sqrt(A * A + B * B);

        // Calculate the perpendicular distance
        double distance = numerator / denominator;

        return distance;
    }

    public static double calculateDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double calculateLargestDistance(double x11, double y11, double x12, double y12, double x21, double y21, double x22, double y22) {
        double distance1 = calculateDistance(x11, y11, x21, y21);
        double distance2 = calculateDistance(x11, y11, x22, y22);
        double distance3 = calculateDistance(x12, y12, x21, y21);
        double distance4 = calculateDistance(x12, y12, x22, y22);

        double largestDistance = Math.max(Math.min(distance1, distance2), Math.min(distance3, distance4));
        return largestDistance;
    }

    public double calculateDistance(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double distance;

        // Calculate the equation of the first line (line1)
        double a1 = y2 - y1;
        double b1 = x1 - x2;
        double c1 = a1 * x1 + b1 * y1;

        // Calculate the equation of the second line (line2)
        double a2 = y4 - y3;
        double b2 = x3 - x4;
        double c2 = a2 * x3 + b2 * y3;

        // Calculate the distance
        double denominator = a1 * b2 - a2 * b1;

        if (denominator == 0) {
            // Lines are parallel
            distance = euclideanDistance(x1, y1, x3, y3);
        } else {
            double x = (b2 * c1 - b1 * c2) / denominator;
            double y = (a1 * c2 - a2 * c1) / denominator;
            double minX1 = Math.min(x1, x2);
            double maxX1 = Math.max(x1, x2);
            double minY1 = Math.min(y1, y2);
            double maxY1 = Math.max(y1, y2);
            double minX3 = Math.min(x3, x4);
            double maxX3 = Math.max(x3, x4);
            double minY3 = Math.min(y3, y4);
            double maxY3 = Math.max(y3, y4);

            if (x >= minX1 && x <= maxX1 && y >= minY1 && y <= maxY1 && x >= minX3 && x <= maxX3 && y >= minY3 && y <= maxY3) {
                // The intersection point is within both line segments
                distance = 0;
            } else {
                // The intersection point is outside of at least one line segment
                double distance1 = euclideanDistance(x, y, x1, y1);
                double distance2 = euclideanDistance(x, y, x2, y2);
                double distance3 = euclideanDistance(x, y, x3, y3);
                double distance4 = euclideanDistance(x, y, x4, y4);
                distance = Math.min(Math.min(distance1, distance2), Math.min(distance3, distance4));
            }
        }

        return distance;
    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){
        return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
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


    public double calculateDistance(double x, double y) {
        // Calculate the distances to each side of the bounding box

        double distx, disty;

        double distanceXmax = Math.abs(maxx - x);
        double distanceXmin = Math.abs(x - minx);

        double distanceYmax = Math.abs(maxy - y);
        double distanceYmin = Math.abs(y - miny);

        // Calculate the distances from the point to the sides of the box
        distx = Math.max(distanceXmax, distanceXmin);
        disty = Math.max(distanceYmax, distanceYmin);

        // Calculate the minimum distance
        double distance = Math.max(distx, disty);

        return distance;
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

    public void declareShapefiles(){

        gdal.AllRegister();

        String driverName = "ESRI Shapefile";

        org.gdal.ogr.Driver shpDriver = ogr.GetDriverByName(driverName);

        String pathSeparator = System.getProperty("file.separator");

        File output_directory = new File(aR.odir + pathSeparator);

        File outFile = new File(output_directory.getAbsolutePath() + pathSeparator + "tin_border_prior.shp");
        File outFile2 = new File(output_directory.getAbsolutePath() + pathSeparator + "tin_border_after.shp");
        DataSource outShp = shpDriver.CreateDataSource(outFile.getAbsolutePath());
        DataSource outShp2 = shpDriver.CreateDataSource(outFile2.getAbsolutePath());

        tinAfterFiltering = outShp2.CreateLayer("out_name", null, 0);
        tinBeforeFiltering = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id",0);
        FieldDefn layerFieldDef2 = new FieldDefn("id",0);
        tinAfterFiltering.CreateField(layerFieldDef);
        tinBeforeFiltering.CreateField(layerFieldDef2);



    }

}
