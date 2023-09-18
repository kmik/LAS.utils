package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import err.toolException;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import utils.argumentReader;
import utils.pointCloudMetrics;
import utils.pointWriterMultiThread;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import static tools.createCHM.fo;


public class lasRasterTools {

    boolean[][] mask = null;
    double[] geoTransform = null;

    argumentReader aR;
    public lasRasterTools(){

    }

    public lasRasterTools(argumentReader aR){
        this.aR = aR;
    }

    public void printTimeInMinutesSeconds(long timeInMilliseconds, String message){
        System.out.println(message + " -- Time taken: " + (timeInMilliseconds / 1000) / 60 + " minutes and " + (timeInMilliseconds / 1000) % 60 + " seconds.");
    }

    public ArrayList<Dataset> readMultipleRasters(argumentReader aR){

        ArrayList<Dataset> rasters = new ArrayList<Dataset>();

        for(File raster : aR.inputFiles){
            rasters.add(gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly));
        }

        return rasters;
    }
    public void readRasters(){

        gdal.AllRegister();

        if(aR.ref.size() > 1){
            throw new toolException("Only one reference raster can be used at a time!");
        }
        if(aR.ref.size() == 0){
            throw new toolException("No reference raster provided!");
        }

        Dataset tifDataset = gdal.Open(aR.ref.get(0).getAbsolutePath(), gdalconst.GA_ReadOnly);
        Band tifBand = tifDataset.GetRasterBand(1);
        int number_of_pix_x = tifDataset.getRasterXSize();
        int number_of_pix_y = tifDataset.getRasterYSize();

        this.geoTransform = tifDataset.GetGeoTransform();

        this.mask = new boolean[tifDataset.GetRasterXSize()][tifDataset.GetRasterYSize()];

        float[] floatArray = new float[number_of_pix_x];

        System.out.println("Reading raster line by line");

        long startTime = System.currentTimeMillis();

        for(int y = 0; y < number_of_pix_y; y++) {

            tifBand.ReadRaster(0, y, number_of_pix_x, 1, floatArray);

            for (int x = 0; x < number_of_pix_x; x++) {

                //System.out.println("line " + y);
                float value = floatArray[x];

                if (value == 1.0f) {
                    mask[x][y] = true;

                }else{

                }
            }
        }

        long endTime = System.currentTimeMillis();

        printTimeInMinutesSeconds(endTime - startTime, "Raster to 2d array");

    }

    public void readRasters2(){

        gdal.AllRegister();

        if(aR.ref.size() > 1){
            throw new toolException("Only one reference raster can be used at a time!");
        }
        if(aR.ref.size() == 0){
            throw new toolException("No reference raster provided!");
        }

        String fileName = aR.ref.get(0).getAbsolutePath();
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        this.mask = new boolean[width][height];
        long startTime = System.currentTimeMillis();


        System.out.println("Reading raster line by line");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                if (rgb == 1) {
                    mask[x][y] = true;

                }else{

                }
            }
        }
        long endTime = System.currentTimeMillis();

        printTimeInMinutesSeconds(endTime - startTime, "Raster to 2d array");

    }

    public void clip(){

        ForkJoinPool customThreadPool = new ForkJoinPool(aR.cores);

        try {

            customThreadPool.submit(() ->

                    IntStream.range(0, aR.inputFiles.size()).parallel().forEach(i_ -> {
                        LASReader temp = null;

                        LasPoint tempPoint = new LasPoint();
                        try {
                            temp = new LASReader(aR.inputFiles.get(i_));
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                        int thread_n = aR.pfac.addReadThread(temp);

                        File outFile = null;
                        pointWriterMultiThread pw = null;

                        try {
                            outFile = aR.createOutputFile(temp);

                            pw = new pointWriterMultiThread(outFile, temp, "las2las", aR);
                        }catch (Exception e){
                            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
                            return;
                        }
                        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
                        aR.pfac.addWriteThread(thread_n, pw, buf);

                        float[] data = new float[1];

                        for (long i = 0; i < temp.getNumberOfPointRecords(); i += 20000) {

                            int maxi = (int) Math.min(20000, Math.abs(temp.getNumberOfPointRecords() - i));

                            try {
                                aR.pfac.prepareBuffer(thread_n, i, maxi);
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                            for (int j_ = 0; j_ < maxi; j_++) {

                                temp.readFromBuffer(tempPoint);

                                int x = (int) Math.round((tempPoint.x - geoTransform[0]) / geoTransform[1]);
                                int y = (int) Math.round((tempPoint.y - geoTransform[3]) / geoTransform[5]);

                                if(x < 0 || x >= mask.length || y < 0 || y >= mask[0].length){
                                    continue;
                                }
                                if(this.mask[x][y] == false){

                                    try {

                                        aR.pfac.writePoint(tempPoint, i+j_, thread_n);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        System.exit(1);
                                    }

                                }
                            }
                        }

                        aR.pfac.closeThread(thread_n);

                    })).get();
            }catch (Exception e){
                e.printStackTrace();
        }


    }

    public void clip2(){

        //ForkJoinPool customThreadPool = new ForkJoinPool(aR.cores);

        try {

            //customThreadPool.submit(() ->
            for(int i_ = 0; i_ < aR.inputFiles.size(); i_++) {
                //IntStream.range(0, aR.inputFiles.size()).parallel().forEach(i_ -> {
                LASReader temp = null;

                LasPoint tempPoint = new LasPoint();
                try {
                    temp = new LASReader(aR.inputFiles.get(i_));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int thread_n = aR.pfac.addReadThread(temp);

                File outFile = null;
                pointWriterMultiThread pw = null;

                try {
                    outFile = aR.createOutputFile(temp);

                    pw = new pointWriterMultiThread(outFile, temp, "las2las", aR);
                } catch (Exception e) {
                    System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
                    return;
                }
                LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
                aR.pfac.addWriteThread(thread_n, pw, buf);

                float[] data = new float[1];

                for (long i = 0; i < temp.getNumberOfPointRecords(); i += 20000) {

                    int maxi = (int) Math.min(20000, Math.abs(temp.getNumberOfPointRecords() - i));

                    try {
                        aR.pfac.prepareBuffer(thread_n, i, maxi);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    for (int j_ = 0; j_ < maxi; j_++) {

                        temp.readFromBuffer(tempPoint);

                        int x = (int) Math.round((tempPoint.x - geoTransform[0]) / geoTransform[1]);
                        int y = (int) Math.round((tempPoint.y - geoTransform[3]) / geoTransform[5]);

                        if (this.mask[x][y] == false) {

                            try {

                                aR.pfac.writePoint(tempPoint, i + j_, thread_n);

                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(1);
                            }

                        }
                    }
                }

                aR.pfac.closeThread(thread_n);
            }
                    //})).get();
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public void rasterize(LASReader pointCloud, double resolution){

        String outputFileName = fo.createNewFileWithNewExtension(pointCloud.getFile(), "_raster.tif").getAbsolutePath();

        double pointCloudMinX = pointCloud.getMinX() - aR.res/2.0;
        double pointCloudMinY = pointCloud.getMinY() - aR.res/2.0;
        double pointCloudMaxX = pointCloud.getMaxX() + aR.res/2.0;
        double pointCloudMaxY = pointCloud.getMaxY() + aR.res/2.0;

/*
        double pointCloudMinX = pointCloud.getMinX();
        double pointCloudMinY = pointCloud.getMinY();
        double pointCloudMaxX = pointCloud.getMaxX();
        double pointCloudMaxY = pointCloud.getMaxY();
*/


        int rasterWidth = (int) Math.ceil((pointCloudMaxX - pointCloudMinX) / resolution);
        int rasterHeight = (int) Math.ceil((pointCloudMaxY - pointCloudMinY) / resolution);

        double[] geoTransform = new double[6];
        geoTransform[0] = pointCloudMinX;
        geoTransform[1] = resolution;
        geoTransform[2] = 0;
        geoTransform[3] = pointCloudMaxY;
        geoTransform[4] = 0;
        geoTransform[5] = -resolution;

        LasPoint tempPoint = new LasPoint();

        org.gdal.gdal.Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();

        Dataset cehoam;
        String compressionOptions = "COMPRESS=LZW";

        try {
            cehoam = driver.Create(outputFileName, rasterWidth, rasterHeight, 1, gdalconst.GDT_Float32);
            cehoam.SetMetadataItem("COMPRESSION", compressionOptions);


        }catch (Exception e){
            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
            return;
        }
        cehoam.SetMetadataItem("test", "leaf-off");
        Band band = cehoam.GetRasterBand(1);

        band.SetNoDataValue(Float.NaN);

        SpatialReference sr = new SpatialReference();

        sr.ImportFromEPSG(3067);

        cehoam.SetProjection(sr.ExportToWkt());
        cehoam.SetGeoTransform(geoTransform);

        double[][] chm_array = new double[rasterWidth][rasterHeight];

        double minx = pointCloud.getMinX();
        double miny = pointCloud.getMinY();
        double maxx = pointCloud.getMaxX();
        double maxy = pointCloud.getMaxY();

        long n = pointCloud.getNumberOfPointRecords();

        int thread_n = aR.pfac.addReadThread(pointCloud);

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000) {

            int maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                aR.pfac.prepareBuffer(thread_n, i, maxi);
            }catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);


                if(tempPoint.x < minx)
                    minx = tempPoint.x;
                if(tempPoint.y < miny)
                    miny = tempPoint.y;
                if(tempPoint.x > maxx)
                    maxx = tempPoint.x;

                int x = (int) Math.floor((tempPoint.x - geoTransform[0]) / geoTransform[1]);
                int y = (int) Math.floor((tempPoint.y - geoTransform[3]) / geoTransform[5]);

                if (x >= 0 && x < rasterWidth && y >= 0 && y < rasterHeight) {

                    if(Double.isNaN(chm_array[x][y]))
                        chm_array[x][y] = (double)tempPoint.z;
                    else if ( (float)tempPoint.z > chm_array[x][y])
                        chm_array[x][y] = (double)tempPoint.z;
                    else{

                    }
                }

            }
        }
        
        System.out.println("minx: " + minx + " pointCloudMinX: " + pointCloudMinX);
        System.out.println("miny: " + miny + " pointCloudMinY: " + pointCloudMinY);
        System.out.println("maxx: " + maxx + " pointCloudMaxX: " + pointCloudMaxX);
        System.out.println("maxy: " + maxy + " pointCloudMaxY: " + pointCloudMaxY);

        copyRasterContents(chm_array, band);

        String[] options = new String[]{"COMPRESS=LZW"};
        cehoam.FlushCache();
        band.FlushCache();

        Dataset outputDataset = gdal.GetDriverByName("GTiff").CreateCopy(outputFileName, cehoam, 0, options);
        outputDataset.FlushCache();

    }

    public static void copyRasterContents(double[][] from, Band to){


        int x = to.getXSize();
        int y = to.getYSize();

        float[] read = new float[x*y];

        int counter = 0;

        for(int y_ = 0; y_ < from[0].length; y_++){
            for(int x_ = 0; x_ < from.length; x_++){


                read[counter++] = (float)from[x_][y_];

            }
        }

        to.WriteRaster(0, 0, x, y, read);

    }

    public void zonalStatistics(argumentReader aR) throws Exception{

        this.aR = aR;
        String polyWKT = readShapefile(aR.poly);
        ArrayList<Integer> polyIds = new ArrayList<>();


        ArrayList<double[][]> polygons = readPolygonsFromWKT(polyWKT, polyIds);
        HashMap<Integer, ArrayList<double[][]>> polygonHoles = readPolygonHolesFromWKT(polyWKT, polyIds);

        ArrayList<Dataset> rasters = new ArrayList<Dataset>();

        for(File raster : aR.inputFiles){
            rasters.add(gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly));
        }

        ArrayList<double[]> rasterExtents = new ArrayList<double[]>();
        ArrayList<double[]> rasterWidthHeight = new ArrayList<double[]>();
        ArrayList<double[]> rasterBoundingBoxes = new ArrayList<double[]>();
        ArrayList<Band> rasterBands = new ArrayList<Band>();

        aR.lCMO.prepZonal(aR.inputFiles.get(0));

        for(Dataset raster : rasters){

            double[] rasterExtent = new double[6];
            raster.GetGeoTransform(rasterExtent);
            //System.out.println(Arrays.toString(rasterExtent));
            rasterExtents.add(rasterExtent);

            double[] rasterWidthHeight_ = new double[2];
            rasterWidthHeight_[0] = raster.GetRasterXSize();
            rasterWidthHeight_[1] = raster.GetRasterYSize();

            rasterBoundingBoxes.add(new double[]{rasterExtent[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0],  rasterExtent[3]});

            rasterBands.add(raster.GetRasterBand(1));
        }

        pointCloudMetrics pCM = new pointCloudMetrics(aR);


        for(int i = 0; i < polygons.size(); i++){

            float[] readValue = new float[1];

            ArrayList<Double> gridPoints_z_a = new ArrayList<>();
            double sum_z_a = 0.0;
            ArrayList<String> colnames_a = new ArrayList<>();
            double[] polygonExtent = getPolygonExtent(polygons.get(i));

            for(int j = 0; j < rasterExtents.size(); j++) {

                HashSet<Integer> usedCells = new HashSet<Integer>();

                //System.out.println(Arrays.toString(polygonExtent));
                //System.out.println(Arrays.toString(rasterExtents.get(j)));
                //System.out.println(Arrays.toString(rasterBoundingBoxes.get(j)));
                //Check if polygon overlaps with raster

                if (polygonExtent[0] > rasterBoundingBoxes.get(j)[2] || polygonExtent[2] < rasterBoundingBoxes.get(j)[0] || polygonExtent[1] > rasterBoundingBoxes.get(j)[3] || polygonExtent[3] < rasterBoundingBoxes.get(j)[1]) {
                    //System.out.println("Polygon does not overlap with raster");
                    continue;
                }

                Band band = rasterBands.get(j);

                double[][] polygon = polygons.get(i);

                ArrayList<double[][]> polygonHoles_ = polygonHoles.get(polyIds.get(i));

                int[] extentInPixelCoordinates = new int[4];

                extentInPixelCoordinates[0] = (int)Math.floor((polygonExtent[0] - rasterBoundingBoxes.get(j)[0]) / rasterExtents.get(j)[1]);
                extentInPixelCoordinates[3] = (int)Math.floor((rasterBoundingBoxes.get(j)[3] - polygonExtent[1] ) / rasterExtents.get(j)[1]);
                extentInPixelCoordinates[2] = (int)Math.floor((polygonExtent[2] - rasterBoundingBoxes.get(j)[0]) / rasterExtents.get(j)[1]);
                extentInPixelCoordinates[1] = (int)Math.floor((rasterBoundingBoxes.get(j)[3] - polygonExtent[3] ) / rasterExtents.get(j)[1]);

                //System.out.println("extentinpixelcoordinates: " + Arrays.toString(extentInPixelCoordinates));

                for(int x = extentInPixelCoordinates[0]; x <= extentInPixelCoordinates[2]; x++){
                    for(int y = extentInPixelCoordinates[1]; y <= extentInPixelCoordinates[3]; y++){

                        double[] realCoordinates = new double[2];
                        realCoordinates[0] = rasterExtents.get(j)[0] + x * rasterExtents.get(j)[1] + rasterExtents.get(j)[1] / 2;
                        realCoordinates[1] = rasterExtents.get(j)[3] + y * rasterExtents.get(j)[5] + rasterExtents.get(j)[5] / 2;

                        boolean pointInPolygon = pointInPolygon(realCoordinates, polygon, polygonHoles, polyIds.get(i));

                        if(pointInPolygon) {
                            band.ReadRaster(x, y, 1, 1, readValue);
                            gridPoints_z_a.add((double)readValue[0]);
                            sum_z_a += readValue[0];
                        }
                    }

                }
            }


            ArrayList<Double> metrics_a = pCM.calcZonal(gridPoints_z_a, sum_z_a, "_a", colnames_a);
            //System.out.println(Arrays.toString(gridPoints_z_a.toArray()));
            //System.out.println("HERE");
            aR.lCMO.writeLineZonal(metrics_a, colnames_a, polyIds.get(i));
        }

        aR.lCMO.closeFilesZonal();
        new File(polyWKT).delete();
    }

    public static boolean pointInPolygon(double[] point, double[][] poly, HashMap<Integer, ArrayList<double[][]>> holes, int poly_id) {

        int numPolyCorners = poly.length;
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

        if(holes.containsKey(poly_id)){

            ArrayList<double[][]> holet = holes.get(poly_id);
            double[][] polyHole = null;

            for(int i_ = 0; i_ < holet.size(); i_++){

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
                if(is_inside_hole) {

                    //System.out.println("YES YES YES!");
                    return false;
                }
            }
        }

        return isInside;
    }

    public double[] getPolygonExtent(double[][] polygon){

        double[] extent = new double[4];

        extent[0] = Double.MAX_VALUE;
        extent[1] = Double.MAX_VALUE;
        extent[2] = Double.MIN_VALUE;
        extent[3] = Double.MIN_VALUE;

        for(int i = 0; i < polygon.length; i++){

            if(polygon[i][0] < extent[0])
                extent[0] = polygon[i][0];
            if(polygon[i][1] < extent[1])
                extent[1] = polygon[i][1];
            if(polygon[i][0] > extent[2])
                extent[2] = polygon[i][0];
            if(polygon[i][1] > extent[3])
                extent[3] = polygon[i][1];

        }

        return extent;

    }

    public static ArrayList<double[][]> readPolygonsFromWKT(String fileName, ArrayList<Integer> plotID1) throws Exception{

        ArrayList<double[][]> output = new ArrayList<>();
        String line1;
        int id_number = 1;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(new File(fileName)));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                String[] tokens =  line1.split(",");
                String[] tokens2 =  line1.split("\\),\\(");
                //System.out.println(Arrays.toString(tokens));
                //if(tokens[tokens.length - 1] != -999){
                boolean holes = false;

                if(tokens2.length > 1){
                    holes = true;
                }

                if(holes) {
                    tokens = line1.split(",\\(");
                    tokens[0] += ")";
                    tokens = tokens[0].split(",");
                    //System.out.println(tokens[0]);
                    //System.exit(1);
                }

                String[] iidee_ = line1.split("\",");
                String id = iidee_[iidee_.length-1];

                //System.out.println(Integer.parseInt(tokens[tokens.length - 1]) + " " + tokens2.length);

                try {
                    //plotID1.add(Integer.parseInt(tokens[tokens.length - 1]));
                    plotID1.add(Integer.parseInt(id));

                }catch (NumberFormatException e){

                    //Not directly comvertable to a number

                    String str = id.replaceAll("\\D+","");

                    try {
                        plotID1.add(Integer.parseInt(str));
                    }catch (Exception ee){

                        //Still no numbers, let's make our own then I guess

                        plotID1.add(id_number++);

                    }

                }

                //System.out.println(plotID1.get(plotID1.size()-1));


                double[][] tempPoly = new double[tokens.length - 1][2];
                int n = (tokens.length) - 2;
                int counteri = 0;

                tempPoly[counteri][0] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[1]);

                counteri++;

                boolean breikki = false;

                for(int i = 1; i < n; i++){

                    if(tokens[i].split(" ")[0].contains(")") || tokens[i].split(" ")[1].contains(")")){
                        plotID1.remove(plotID1.size() - 1);
                        breikki = true;
                        break;
                    }

                    tempPoly[counteri][0] = Double.parseDouble(tokens[i].split(" ")[0]);
                    tempPoly[counteri][1] = Double.parseDouble(tokens[i].split(" ")[1]);
                    counteri++;
                }

                //System.out.println(Arrays.toString(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")));
                tempPoly[counteri][0] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[1]);

                if(!breikki)
                    output.add(tempPoly);
                //}

            }


        }catch( IOException ioException ) {
            //ioException.printStackTrace();
        }

        return output;
    }

    public static HashMap<Integer, ArrayList<double[][]>> readPolygonHolesFromWKT(String fileName, ArrayList<Integer> plotID1) throws Exception{

        HashMap<Integer, ArrayList<double[][]>> output = new HashMap<Integer, ArrayList<double[][]>>();

        String line1;
        int id_number = 1;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(new File(fileName)));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                String[] tokens =  line1.split(",");
                String[] tokens2 =  line1.split("\\),\\(");

                if(tokens2.length <= 1){
                    continue;
                }

                int plot_id = -1;

                try {
                    plot_id = Integer.parseInt(tokens[tokens.length - 1]);
                }catch (NumberFormatException e){

                    //Not directly comvertable to a number

                    String str = tokens[tokens.length - 1].replaceAll("\\D+","");

                    try {
                        plot_id = Integer.parseInt(str);
                    }catch (Exception ee){

                        //Still no numbers, let's make our own then I guess
                        plot_id = id_number++;

                    }

                }

                //System.out.println(tokens2[0]);

                String string_here = null;

                output.put(plot_id, new ArrayList<>());


                for(int i = 1; i < tokens2.length; i++){

                    string_here = tokens2[i];

                    if(i == tokens2.length-1){

                        String[] tokens_here = string_here.split("\\)\\)");
                        //String[] coords = tokens_here[0].split()
                        //System.out.println(tokens_here[0]);
                        string_here = tokens_here[0];

                    }else{
                        string_here = tokens2[i];
                    }

                    String[] toks = string_here.split(",");

                    double[][] tempPoly = new double[toks.length][2];
                    int n = toks.length;
                    int counteri = 0;

                    for(int i_ = 0; i_ < n; i_++){

                        tempPoly[counteri][0] = Double.parseDouble(toks[i_].split(" ")[0]);
                        tempPoly[counteri][1] = Double.parseDouble(toks[i_].split(" ")[1]);
                        counteri++;
                    }

                    output.get(plot_id).add(tempPoly);
                }

            }


        }catch( IOException ioException ) {
            //ioException.printStackTrace();
        }

        return output;
    }


    public String readShapefile(String polyPath){

        File shapeFile = new File(polyPath);
        File fout = null;
        String koealat = null;

        if(shapeFile.getName().contains(".shp")){

            int checkShp = 1;
            DataSource ds = ogr.Open( polyPath);

            Layer layeri = ds.GetLayer(0);
            File checkFile = new File("tempWKT_" + System.currentTimeMillis() + ".csv");
            try {

                if (!checkFile.exists()) {


                    fout = checkFile;

                    if (fout.exists())
                        fout.delete();

                    fout.createNewFile();

                    FileOutputStream fos = new FileOutputStream(fout);
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                    bw.write("WKT,plot_id");
                    bw.newLine();

                    for (long i = 0; i < layeri.GetFeatureCount(); i++) {

                        Feature tempF = layeri.GetFeature(i);
                        Geometry tempG = tempF.GetGeometryRef();

                        if (tempG == null)
                            continue;

                        String id = "";

                        if (tempF.GetFieldCount() > 0)
                            id = tempF.GetFieldAsString(aR.field);
                        else
                            id = String.valueOf(i);

                        String out = "\"" + tempG.ExportToWkt() + "\"," + id;

                        bw.write(out);
                        bw.newLine();


                    }
                    bw.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }


            koealat = checkFile.getAbsolutePath();

        }
        return koealat;
    }
}
