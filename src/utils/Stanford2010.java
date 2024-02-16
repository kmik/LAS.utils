package utils;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import err.toolException;
import org.apache.commons.math3.linear.*;
import org.cts.CRSFactory;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.crs.GeodeticCRS;
import org.cts.op.CoordinateOperation;
import org.cts.op.CoordinateOperationFactory;
import org.cts.registry.EPSGRegistry;
import org.cts.registry.RegistryManager;
import org.ejml.simple.SimpleMatrix;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.locationtech.jts.geom.GeometryCollection;
import tools.*;

import org.gdal.osr.SpatialReference;
import tools.ConcaveHull;
import tools.QuickHull;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.io.File.pathSeparator;
import static tools.Boundary.angleBetween;
import static tools.ConcaveHull.calculateConcaveHull;

public class Stanford2010 {

    double cellSizeVMI = 16.0;
    double minX_finnishMap6k = 20000.0;
    double maxY_finnishMap6k = 7818000.0;

    double grid_x_size_MML = 45033;
    double orig_y, orig_x;

    int VMI_minIndexX = 0, VMI_maxIndexY = 0;
    String LIDAR_ACQUISITION_DATE = "2020-06-22T20:57:36+02:00";
    String LIDAR_ACQUISITION_DEBUG = "2021-03-26T20:57:36+02:00";

    rasterCollection rasters = null;

    HashSet<Integer> processedStands = new HashSet<>();
    HashSet<Integer> excludedStands = new HashSet<>();

    int runningPlotId = 0;
    double plotRadius = 9;
    int runningId = 0;

    int concave_hull_k = 60;
    ArrayList<String> failedFiles = new ArrayList<>();
    ArrayList<int[]> failedFilesProps = new ArrayList<>();
    File xml_file;
    argumentReader aR;

    public double maxDistanceFromStandBorder = 15;
    HashMap<Integer, double[][]> standBounds = new HashMap<>();

    HashMap<Integer, double[][]> standHoles = new HashMap<>();
    HashMap<Integer, double[]> standCentroids = new HashMap<>();

    HashMap<Integer, Polygon> standPolygons = new HashMap<>();


    KdTree centroids = new KdTree();

    Layer allStandsShapeFileLayer;
    Layer allPlotsShapeFileLayer;

    Layer getAllPlotsShapeFileLayer_square;

    Layer gridShapeFileLayer_original;
    Layer gridShapeFileLayerBuffered;

    File logFile = null;
    BufferedWriter logWriter = null;

    //This is a scaler. 1.0 = the area of the plots equals the area of the stand. 1.5 = the area of the plots is 1.5 times the area of the stand.
    double numberOfPlotsPerStand = 1.0;

    double minX, minY, maxX, maxY;

    HashMap<String, FileBatch> processingBatches = new HashMap<>();

    public Stanford2010(argumentReader aR) throws toolException {
        this.aR = aR;
        this.declareMergedShapefile();
        this.declarePlotShapefile();
        this.declarePlotShapefile_square();
        this.declareGridShapeFileOriginal();
        this.declareGridShapeFiluffered();
    }



    public void setXMLfile(File file){

        this.xml_file = file;

    }

    public double diameterToHeight(double diameter, int species){

        double m = species == 2 ? 3.0 : 2.0;

        if(species == 1){
            return (Math.pow(diameter, m)) / Math.pow((1.181 + 0.247 * diameter), m) + 1.3;

        }else if(species == 2){
            return (Math.pow(diameter, m)) / Math.pow((1.760 + 0.327 * diameter), m) + 1.3;

        }else if(species == 3) {
            return (Math.pow(diameter, m)) / Math.pow((1.014 + 0.238 * diameter), m) + 1.3;
        }

        return -1;
    }

    public void declareMergedShapefile(){

        gdal.AllRegister();

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        String pathSeparator = System.getProperty("file.separator");


        File output_directory = new File(aR.odir + pathSeparator);

        File outFile = new File(output_directory.getAbsolutePath() + pathSeparator + "allStands.shp");

        DataSource outShp = shpDriver.CreateDataSource(outFile.getAbsolutePath());
        allStandsShapeFileLayer = outShp.CreateLayer("out_name", null, 0);
        FieldDefn layerFieldDef = new FieldDefn("id",0);
        allStandsShapeFileLayer.CreateField(layerFieldDef);
    }

    public void declareLogFile(){

        String pathSeparator = System.getProperty("file.separator");
        File output_directory = new File(aR.odir + pathSeparator);
        logFile = new File(output_directory.getAbsolutePath() + pathSeparator + "log.txt");
        if(logFile.exists()){
            logFile.delete();
        }

        try{
            logFile.createNewFile();
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            logWriter = new BufferedWriter(new FileWriter(logFile));
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }


    }

    public void closeLogFile(){

        try {
            logWriter.close();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void setRasters(rasterCollection rasters){
        this.rasters = rasters;
    }

    public void writeLineToLogfile(String line){

        try {
            logWriter.write(line);
            logWriter.newLine();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

    }
    public void declarePlotShapefile(){

        gdal.AllRegister();

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        String pathSeparator = System.getProperty("file.separator");

        File output_directory = new File(aR.odir + pathSeparator);

        File outFile = new File(output_directory.getAbsolutePath() + pathSeparator + "plots.shp");
        DataSource outShp = shpDriver.CreateDataSource(outFile.getAbsolutePath());


        allPlotsShapeFileLayer = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id",0);
        allPlotsShapeFileLayer.CreateField(layerFieldDef);

        FieldDefn layerFieldDef2 = new FieldDefn("stand_id",0);
        allPlotsShapeFileLayer.CreateField(layerFieldDef2);

        FieldDefn layerFieldDef3 = new FieldDefn("v_log_p",2);
        allPlotsShapeFileLayer.CreateField(layerFieldDef3);

        FieldDefn layerFieldDef4 = new FieldDefn("v_log_s",2);
        allPlotsShapeFileLayer.CreateField(layerFieldDef4);

        FieldDefn layerFieldDef5 = new FieldDefn("v_log_b",2);
        allPlotsShapeFileLayer.CreateField(layerFieldDef5);

        FieldDefn layerFieldDef9 = new FieldDefn("v_pulp_p",2);
        allPlotsShapeFileLayer.CreateField(layerFieldDef9);

        FieldDefn layerFieldDef10 = new FieldDefn("v_pulp_s",2);
        allPlotsShapeFileLayer.CreateField(layerFieldDef10);

        FieldDefn layerFieldDef11 = new FieldDefn("v_pulp_b",2);
        allPlotsShapeFileLayer.CreateField(layerFieldDef11);

        FieldDefn layerFieldDef12 = new FieldDefn("v_energy_p",2);
        allPlotsShapeFileLayer.CreateField(layerFieldDef12);

        FieldDefn layerFieldDef13 = new FieldDefn("v_energy_s",2);
        allPlotsShapeFileLayer.CreateField(layerFieldDef13);

        FieldDefn layerFieldDef14 = new FieldDefn("v_energy_b",2);
        allPlotsShapeFileLayer.CreateField(layerFieldDef14);



    }

    public void declarePlotShapefile_square(){

        gdal.AllRegister();

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        String pathSeparator = System.getProperty("file.separator");

        File output_directory = new File(aR.odir + pathSeparator);

        File outFile = new File(output_directory.getAbsolutePath() + pathSeparator + "plots_square.shp");
        DataSource outShp = shpDriver.CreateDataSource(outFile.getAbsolutePath());


        getAllPlotsShapeFileLayer_square = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id",0);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef);

        FieldDefn layerFieldDef2 = new FieldDefn("stand_id",0);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef2);

        FieldDefn layerFieldDef3 = new FieldDefn("v_log_p",2);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef3);

        FieldDefn layerFieldDef4 = new FieldDefn("v_log_s",2);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef4);

        FieldDefn layerFieldDef5 = new FieldDefn("v_log_b",2);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef5);

        FieldDefn layerFieldDef9 = new FieldDefn("v_pulp_p",2);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef9);

        FieldDefn layerFieldDef10 = new FieldDefn("v_pulp_s",2);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef10);

        FieldDefn layerFieldDef11 = new FieldDefn("v_pulp_b",2);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef11);

        FieldDefn layerFieldDef12 = new FieldDefn("v_energy_p",2);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef12);

        FieldDefn layerFieldDef13 = new FieldDefn("v_energy_s",2);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef13);

        FieldDefn layerFieldDef14 = new FieldDefn("v_energy_b",2);
        getAllPlotsShapeFileLayer_square.CreateField(layerFieldDef14);



    }

    public void declareGridShapeFileOriginal(){

        gdal.AllRegister();

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        String pathSeparator = System.getProperty("file.separator");

        File output_directory = new File(aR.odir + pathSeparator);

        File outFile = new File(output_directory.getAbsolutePath() + pathSeparator + "grid_original_stand.shp");
        DataSource outShp = shpDriver.CreateDataSource(outFile.getAbsolutePath());


        gridShapeFileLayer_original = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id",0);
        gridShapeFileLayer_original.CreateField(layerFieldDef);

        FieldDefn layerFieldDef99 = new FieldDefn("overlap",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef99);

        FieldDefn layerFieldDef2 = new FieldDefn("stand_id",0);
        gridShapeFileLayer_original.CreateField(layerFieldDef2);

        FieldDefn layerFieldDef3 = new FieldDefn("v_log_p",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef3);

        FieldDefn layerFieldDef4 = new FieldDefn("v_log_s",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef4);

        FieldDefn layerFieldDef5 = new FieldDefn("v_log_b",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef5);

        FieldDefn layerFieldDef9 = new FieldDefn("v_pulp_p",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef9);

        FieldDefn layerFieldDef10 = new FieldDefn("v_pulp_s",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef10);

        FieldDefn layerFieldDef11 = new FieldDefn("v_pulp_b",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef11);

        FieldDefn layerFieldDef12 = new FieldDefn("v_energy_p",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef12);

        FieldDefn layerFieldDef13 = new FieldDefn("v_energy_s",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef13);

        FieldDefn layerFieldDef14 = new FieldDefn("v_energy_b",2);
        gridShapeFileLayer_original.CreateField(layerFieldDef14);
    }

    public void declareGridShapeFiluffered() {

        gdal.AllRegister();

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        String pathSeparator = System.getProperty("file.separator");

        File output_directory = new File(aR.odir + pathSeparator);

        File outFile = new File(output_directory.getAbsolutePath() + pathSeparator + "grid_buffered_stand.shp");
        DataSource outShp = shpDriver.CreateDataSource(outFile.getAbsolutePath());


        gridShapeFileLayerBuffered = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id", 0);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef);

        FieldDefn layerFieldDef99 = new FieldDefn("overlap",2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef99);

        FieldDefn layerFieldDef2 = new FieldDefn("stand_id", 0);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef2);

        FieldDefn layerFieldDef3 = new FieldDefn("v_log_p", 2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef3);

        FieldDefn layerFieldDef4 = new FieldDefn("v_log_s", 2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef4);

        FieldDefn layerFieldDef5 = new FieldDefn("v_log_b", 2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef5);

        FieldDefn layerFieldDef9 = new FieldDefn("v_pulp_p", 2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef9);

        FieldDefn layerFieldDef10 = new FieldDefn("v_pulp_s", 2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef10);

        FieldDefn layerFieldDef11 = new FieldDefn("v_pulp_b", 2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef11);

        FieldDefn layerFieldDef12 = new FieldDefn("v_energy_p", 2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef12);

        FieldDefn layerFieldDef13 = new FieldDefn("v_energy_s", 2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef13);

        FieldDefn layerFieldDef14 = new FieldDefn("v_energy_b", 2);
        gridShapeFileLayerBuffered.CreateField(layerFieldDef14);
    }

    public void finalizeMergedShapefile(){
/*
        SpatialReference spatialRef = new SpatialReference();
        spatialRef.ImportFromEPSG(3067);

        // Create and add features...

        // Iterate over features and assign spatial reference
        allPlotsShapeFileLayer.ResetReading();
        Feature feature;

        while ((feature = allPlotsShapeFileLayer.GetNextFeature()) != null) {
            feature.GetGeometryRef().AssignSpatialReference(spatialRef);
            allPlotsShapeFileLayer.SetFeature(feature);
            feature.delete();
        }

        // Iterate over features and assign spatial reference
        allStandsShapeFileLayer.ResetReading();
        while ((feature = allStandsShapeFileLayer.GetNextFeature()) != null) {
            feature.GetGeometryRef().AssignSpatialReference(spatialRef);
            allStandsShapeFileLayer.SetFeature(feature);
            feature.delete();
        }

*/

        allStandsShapeFileLayer.SyncToDisk();
        allPlotsShapeFileLayer.SyncToDisk();
        getAllPlotsShapeFileLayer_square.SyncToDisk();
        gridShapeFileLayerBuffered.SyncToDisk();
        gridShapeFileLayer_original.SyncToDisk();



    }

    public void readExcludedStandsFromFile(String fileName){

        try{
            Scanner scanner = new Scanner(new File(fileName));

            while(scanner.hasNextLine()){

                String line = scanner.nextLine();

                if(line.length() > 0)
                    excludedStands.add(Integer.parseInt(line));

            }

        }catch(Exception e){

            e.printStackTrace();
            System.exit(1);

        }

    }

    public void addPlotToShapefile(double x, double y, double r,  int id, int stand_id,
                                   double v_log_pine, double v_log_spruce, double v_log_birch,
                                   double v_pulp_pine, double v_pulp_spruce, double v_pulp_birch,
                                   double v_energy_pine, double v_energy_spruce, double v_energy_birch){

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

            FeatureDefn outShpFeatDefn = allPlotsShapeFileLayer.GetLayerDefn();
            Feature feature = new Feature(outShpFeatDefn);
            feature.SetGeometryDirectly(outShpGeom);

            //Feature feature = new Feature(allPlotsShapeFileLayer.GetLayerDefn());
            //feature.SetGeometry(outShpGeom);

            // Set feature attributes

            feature.SetField("id", id);
            feature.SetField("stand_id", stand_id);
            feature.SetField("v_log_p", v_log_pine);
            feature.SetField("v_log_s", v_log_spruce);
            feature.SetField("v_log_b", v_log_birch);
            feature.SetField("v_pulp_p", v_pulp_pine);
            feature.SetField("v_pulp_s", v_pulp_spruce);
            feature.SetField("v_pulp_b", v_pulp_birch);
            feature.SetField("v_energy_p", v_energy_pine);
            feature.SetField("v_energy_s", v_energy_spruce);
            feature.SetField("v_energy_b", v_energy_birch);

            SpatialReference spatialRef = new SpatialReference();
            spatialRef.ImportFromEPSG(3067);
            feature.GetGeometryRef().AssignSpatialReference(spatialRef);

            allPlotsShapeFileLayer.CreateFeature(feature);

        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void addPlotToShapefile_square(double x, double y, double r,  int id, int stand_id,
                                   double v_log_pine, double v_log_spruce, double v_log_birch,
                                   double v_pulp_pine, double v_pulp_spruce, double v_pulp_birch,
                                   double v_energy_pine, double v_energy_spruce, double v_energy_birch){

        try {
            gdal.AllRegister();
            List<double[]> circlePoints = calculateSquareBoundary(x, y, r);



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

            FeatureDefn outShpFeatDefn = getAllPlotsShapeFileLayer_square.GetLayerDefn();
            Feature feature = new Feature(outShpFeatDefn);
            feature.SetGeometryDirectly(outShpGeom);

            //Feature feature = new Feature(allPlotsShapeFileLayer.GetLayerDefn());
            //feature.SetGeometry(outShpGeom);

            // Set feature attributes

            feature.SetField("id", id);
            feature.SetField("stand_id", stand_id);
            feature.SetField("v_log_p", v_log_pine);
            feature.SetField("v_log_s", v_log_spruce);
            feature.SetField("v_log_b", v_log_birch);
            feature.SetField("v_pulp_p", v_pulp_pine);
            feature.SetField("v_pulp_s", v_pulp_spruce);
            feature.SetField("v_pulp_b", v_pulp_birch);
            feature.SetField("v_energy_p", v_energy_pine);
            feature.SetField("v_energy_s", v_energy_spruce);
            feature.SetField("v_energy_b", v_energy_birch);

            SpatialReference spatialRef = new SpatialReference();
            spatialRef.ImportFromEPSG(3067);
            feature.GetGeometryRef().AssignSpatialReference(spatialRef);

            getAllPlotsShapeFileLayer_square.CreateFeature(feature);

        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void addPlotToShapefile_grid_original(double x, double y, double r,  int id, int stand_id,
                                          double v_log_pine, double v_log_spruce, double v_log_birch,
                                          double v_pulp_pine, double v_pulp_spruce, double v_pulp_birch,
                                          double v_energy_pine, double v_energy_spruce, double v_energy_birch,
                                                 double overlap){

        try {
            gdal.AllRegister();
            List<double[]> circlePoints = calculateSquareBoundary(x, y, r);



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

            FeatureDefn outShpFeatDefn = gridShapeFileLayer_original.GetLayerDefn();
            Feature feature = new Feature(outShpFeatDefn);
            feature.SetGeometryDirectly(outShpGeom);

            //Feature feature = new Feature(allPlotsShapeFileLayer.GetLayerDefn());
            //feature.SetGeometry(outShpGeom);

            // Set feature attributes

            feature.SetField("id", id);
            feature.SetField("overlap", overlap);
            feature.SetField("stand_id", stand_id);
            feature.SetField("v_log_p", v_log_pine);
            feature.SetField("v_log_s", v_log_spruce);
            feature.SetField("v_log_b", v_log_birch);
            feature.SetField("v_pulp_p", v_pulp_pine);
            feature.SetField("v_pulp_s", v_pulp_spruce);
            feature.SetField("v_pulp_b", v_pulp_birch);
            feature.SetField("v_energy_p", v_energy_pine);
            feature.SetField("v_energy_s", v_energy_spruce);
            feature.SetField("v_energy_b", v_energy_birch);

            SpatialReference spatialRef = new SpatialReference();
            spatialRef.ImportFromEPSG(3067);
            feature.GetGeometryRef().AssignSpatialReference(spatialRef);

            gridShapeFileLayer_original.CreateFeature(feature);

        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void addPlotToShapefile_grid_buffered(double x, double y, double r,  int id, int stand_id,
                                                 double v_log_pine, double v_log_spruce, double v_log_birch,
                                                 double v_pulp_pine, double v_pulp_spruce, double v_pulp_birch,
                                                 double v_energy_pine, double v_energy_spruce, double v_energy_birch,
                                                 double overlap){

        try {
            gdal.AllRegister();
            List<double[]> circlePoints = calculateSquareBoundary(x, y, r);



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

            FeatureDefn outShpFeatDefn = gridShapeFileLayerBuffered.GetLayerDefn();
            Feature feature = new Feature(outShpFeatDefn);
            feature.SetGeometryDirectly(outShpGeom);

            //Feature feature = new Feature(allPlotsShapeFileLayer.GetLayerDefn());
            //feature.SetGeometry(outShpGeom);

            // Set feature attributes

            feature.SetField("id", id);
            feature.SetField("overlap", overlap);
            feature.SetField("stand_id", stand_id);
            feature.SetField("v_log_p", v_log_pine);
            feature.SetField("v_log_s", v_log_spruce);
            feature.SetField("v_log_b", v_log_birch);
            feature.SetField("v_pulp_p", v_pulp_pine);
            feature.SetField("v_pulp_s", v_pulp_spruce);
            feature.SetField("v_pulp_b", v_pulp_birch);
            feature.SetField("v_energy_p", v_energy_pine);
            feature.SetField("v_energy_s", v_energy_spruce);
            feature.SetField("v_energy_b", v_energy_birch);

            SpatialReference spatialRef = new SpatialReference();
            spatialRef.ImportFromEPSG(3067);
            feature.GetGeometryRef().AssignSpatialReference(spatialRef);

            gridShapeFileLayerBuffered.CreateFeature(feature);

        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

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

    public List<double[]> calculateSquareBoundary(double centerX, double centerY, double radius) {
        List<double[]> boundaryPoints = new ArrayList<>();

        boundaryPoints.add(new double[]{centerX - radius, centerY - radius});
        boundaryPoints.add(new double[]{centerX + radius, centerY - radius});
        boundaryPoints.add(new double[]{centerX + radius, centerY + radius});
        boundaryPoints.add(new double[]{centerX - radius, centerY + radius});

        return boundaryPoints;

    }

    public void exportShapefile(File outputFileName, ArrayList<tools.ConcaveHull.Point> border, int id, Geometry geom){

        //ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        Geometry outShpGeom2 = new Geometry(ogr.wkbLinearRing);
        Geometry outShpGeom = new Geometry(ogr.wkbPolygon);

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        DataSource outShp = shpDriver.CreateDataSource(outputFileName.getAbsolutePath());

        Layer outShpLayer = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id",0);

        outShpLayer.CreateField(layerFieldDef);
        FeatureDefn outShpFeatDefn = outShpLayer.GetLayerDefn();
        Feature outShpFeat = new Feature(outShpFeatDefn);

        SpatialReference srs = new SpatialReference();
        srs.ImportFromEPSG(3067); // WGS84

        for(int i = 0; i < border.size(); i++) {

            //System.out.println("Wrote " + i);
            outShpGeom2.AddPoint_2D(border.get(i).getX(), border.get(i).getY());
            System.out.println(border.get(i).getX() + " " + border.get(i).getY());

        }
        CoordinateTransformation transform = new CoordinateTransformation(srs, srs);

        //outShpGeom2.Transform(transform);
        //bufferedShapefile.Transform(transform);

        outShpGeom.AddGeometry(outShpGeom2);
        Geometry bufferedShapefile = outShpGeom.Buffer(7.5);

        outShpFeat.SetField("id",id);

        outShpFeat.SetGeometryDirectly(bufferedShapefile);


        outShpLayer.CreateFeature(outShpFeat);
        outShpLayer.SyncToDisk();
        System.out.println("features: " + outShpLayer.GetFeatureCount());

        allStandsShapeFileLayer.CreateFeature(outShpFeat);

    }

    public Geometry exportShapefile_(File outputFileName, ArrayList<tools.ConcaveHull.Point> border, int id, HashMap<Integer, HashMap<Integer, List<ConcaveHull.Point>>> holeConcaveHulls,
                                                              ArrayList<tools.ConcaveHull.Point> bufferedBorder){

        //bufferedBorder = new ArrayList<>();
        gdal.AllRegister();

        Geometry outShpGeom2 = new Geometry(ogr.wkbLinearRing);
        ArrayList<Geometry> holes = new ArrayList<>();

        ArrayList<Integer> holeids = new ArrayList<>();

        if(holeConcaveHulls.containsKey(id)){


            for(Integer holeid : holeConcaveHulls.get(id).keySet()) {
                holeids.add(holeid);
                List<ConcaveHull.Point> hole = holeConcaveHulls.get(id).get(holeid);
                Geometry holeGeom_ = new Geometry(ogr.wkbLinearRing);
                for (ConcaveHull.Point p : hole) {
                    holeGeom_.AddPoint_2D(p.getX(), p.getY());

                }
                    //Geometry bufferedHole = holeGeom.Buffer(7.5);
                holes.add(holeGeom_);

            }
        }

        Geometry outShpGeom = new Geometry(ogr.wkbPolygon);
        Geometry holeGeom = new Geometry(ogr.wkbMultiPolygon);

        System.out.println(holes.size());
        for(int i = 0; i < holes.size(); i++){
            Geometry tmpGeometry = new Geometry(ogr.wkbPolygon);
            tmpGeometry.AddGeometry(holes.get(i));

            Geometry tmpGeometry2 = tmpGeometry.Buffer(-7.5);

            if(tmpGeometry2.GetGeometryName().equals("MULTIPOLYGON"))
                holeGeom.AddGeometry(tmpGeometry);
            else
                holeGeom.AddGeometry(tmpGeometry2);

        }

        Geometry bufferedHole = holeGeom.Buffer(0);

        System.out.println(holes.size());
        System.out.println(bufferedHole.GetGeometryName());
        System.out.println(holeGeom.GetGeometryCount());
        System.out.println(bufferedHole.GetGeometryCount());

        for(int i = 0; i < holes.size(); i++){
            Geometry bufHole = holeGeom.GetGeometryRef(i);
            holeConcaveHulls.get(id).get(holeids.get(i)).clear();
            // iterate the points in bufHole
            for(int j = 0; j < bufHole.GetPointCount(); j++){
                double[] point = bufHole.GetPoint(j);

                holeConcaveHulls.get(id).get(holeids.get(i)).add(new tools.ConcaveHull.Point(point[0], point[1]));

            }
        }

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        DataSource outShp = shpDriver.CreateDataSource(outputFileName.getAbsolutePath());

        Layer outShpLayer = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id",0);

        outShpLayer.CreateField(layerFieldDef);
        FeatureDefn outShpFeatDefn = outShpLayer.GetLayerDefn();
        Feature outShpFeat = new Feature(outShpFeatDefn);

        SpatialReference srs = new SpatialReference();
        srs.ImportFromEPSG(3067); // WGS84

        for(int i = 0; i < border.size(); i++) {

            //System.out.println("Wrote " + i);
            outShpGeom2.AddPoint_2D(border.get(i).getX(), border.get(i).getY());

        }
        CoordinateTransformation transform = new CoordinateTransformation(srs, srs);

        //outShpGeom2.Transform(transform);
        //bufferedShapefile.Transform(transform);

        outShpGeom.AddGeometry(outShpGeom2);

        Geometry bufferedShapefile = outShpGeom.Buffer(7.5);

        System.out.println(bufferedShapefile.GetGeometryName());
        System.out.println(bufferedShapefile.GetGeometryCount());
        System.out.println(bufferedShapefile.IsValid());

        if(bufferedShapefile.GetGeometryRef(0).GetPoints().length <= 3) {
            bufferedBorder = null;
            return null;
        }


        double[][] bufferedPoints = bufferedShapefile.GetGeometryRef(0).GetPoints();

        for(int i = 0; i < bufferedPoints.length; i++){
            bufferedBorder.add(new tools.ConcaveHull.Point(bufferedPoints[i][0], bufferedPoints[i][1]));
            //System.out.println("adding: " + bufferedPoints[i][0] + " " + bufferedPoints[i][1]);
        }

        if(false)
        if(holeConcaveHulls.containsKey(id)){
            for(Geometry hole : holes){
                bufferedShapefile.AddGeometry(hole);

            }
        }



        if(true)
        if(holeConcaveHulls.containsKey(id)){
/*
            System.out.println(bufferedHole.GetGeometryCount());
            System.out.println(bufferedHole.GetGeometryName());
            System.out.println(bufferedHole.GetGeometryRef(0).GetGeometryName());
            System.out.println(bufferedShapefile.GetGeometryName());
            System.out.println(holes.size());
            System.out.println("--------------------");

 */
            SpatialReference sr = new SpatialReference();

            sr.ImportFromEPSG(aR.EPSG);

            if(bufferedHole.GetGeometryName().equals("MULTIPOLYGON")) {
                for (int i = 0; i < bufferedHole.GetGeometryCount(); i++) {
/*
                    System.out.println("multipoly");
                    System.out.println(bufferedHole.GetGeometryRef(i).GetGeometryName());
                    System.out.println(bufferedShapefile.GetGeometryName());
                    System.out.println(border.size());
                    System.out.println("ORIGINAL GEOMETRY NAME: " + bufferedShapefile.GetGeometryName());

 */
                    Geometry bufHole = bufferedHole.GetGeometryRef(i).GetGeometryRef(0);



                    //bufferedShapefile.AddGeometry(bufferedHole.GetGeometryRef(i).GetGeometryRef(0));
                    bufferedShapefile.AssignSpatialReference(sr);
                    bufferedHole.GetGeometryRef(i).AssignSpatialReference(sr);


                    boolean overlap = bufferedShapefile.Overlaps(bufferedHole.GetGeometryRef(i));
                    boolean within = bufferedHole.GetGeometryRef(i).Within(bufferedShapefile);
                    boolean contains = bufferedShapefile.Contains(bufferedHole.GetGeometryRef(i));

                    //print the points in bufferedShapefile
                    //System.out.println("BUFFERED SHAPEFILE POINTS");
                    for(int j = 0; j < bufferedShapefile.GetGeometryRef(0).GetPointCount(); j++){
                        double[] point = bufferedShapefile.GetGeometryRef(0).GetPoint(j);
                        //System.out.println(point[0] + " " + point[1]);
                    }

                    //print the points in bufferedHole
                    //System.out.println("BUFFERED HOLE POINTS");
                    for(int j = 0; j < bufferedHole.GetGeometryRef(i).GetGeometryRef(0).GetPointCount(); j++){
                        double[] point = bufferedHole.GetGeometryRef(i).GetGeometryRef(0).GetPoint(j);
                        //System.out.println(point[0] + " " + point[1]);
                    }

/*
                    System.out.println();
                    System.out.println("OVERLAP: " + overlap);
                    System.out.println("WITHIN: " + within);
                    System.out.println("CONTAINS: " + contains);
*/
                    bufferedShapefile = bufferedShapefile.Difference(bufferedHole.GetGeometryRef(i));

                    //bufferedShapefile = PolygonOperations.clipGeometry(bufferedShapefile, bufferedHole.GetGeometryRef(i).GetGeometryRef(0));

                    //System.out.println("GEOMETRY NAME AFTER: " + bufferedShapefile.GetGeometryName());
                    //if(tmp != null)
                    //    bufferedShapefile = tmp;
                    //bufferedShapefile = bufferedShapefile.Difference(bufferedHole.GetGeometryRef(i).GetGeometryRef(0));

                    holeConcaveHulls.get(id).get(holeids.get(i)).clear();
                    // iterate the points in bufHole
                    System.out.println(bufHole.GetPointCount());
                    for (int j = 0; j < bufHole.GetPointCount(); j++) {
                        double[] point = bufHole.GetPoint(j);

                        holeConcaveHulls.get(id).get(holeids.get(i)).add(new tools.ConcaveHull.Point(point[0], point[1]));

                    }
                }
            }else if(bufferedHole.GetGeometryName().equals("POLYGON")){
                Geometry bufHole = bufferedHole.GetGeometryRef(0);
                /*
                System.out.println("poly");
                System.out.println(bufferedHole.GetGeometryName());
                System.out.println("ORIGINAL GEOMETRY NAME: " + bufferedShapefile.GetGeometryName());

                 */
                bufferedShapefile.AssignSpatialReference(sr);
                bufferedHole.AssignSpatialReference(sr);
                //bufferedShapefile.AddGeometry(bufferedHole.GetGeometryRef(0));
                bufferedShapefile = bufferedShapefile.Difference(bufferedHole);

                //bufferedShapefile = PolygonOperations.clipGeometry(bufferedShapefile, bufferedHole.GetGeometryRef(0));
                //System.out.println("GEOMETRY NAME AFTER: " + bufferedShapefile.GetGeometryName());
                //

                boolean overlap = bufferedShapefile.Overlaps(bufferedHole);
                boolean within = bufferedHole.Within(bufferedShapefile);
                boolean contains = bufferedShapefile.Contains(bufferedHole);

                //System.out.println("BUFFERED SHAPEFILE POINTS");
                for(int j = 0; j < bufferedShapefile.GetGeometryRef(0).GetPointCount(); j++){
                    double[] point = bufferedShapefile.GetGeometryRef(0).GetPoint(j);
                    //System.out.println(point[0] + " " + point[1]);
                }

                //print the points in bufferedHole
                //System.out.println("BUFFERED HOLE POINTS");
                for(int j = 0; j < bufferedHole.GetGeometryRef(0).GetPointCount(); j++){
                    double[] point = bufferedHole.GetGeometryRef(0).GetPoint(j);
                    //System.out.println(point[0] + " " + point[1]);
                }
/*
                System.out.println();
                System.out.println("OVERLAP: " + overlap);
                System.out.println("WITHIN: " + within);
                System.out.println("CONTAINS: " + contains);

 */
                //if(tmp != null)
                //    bufferedShapefile = tmp;
                //bufferedShapefile = bufferedShapefile.Difference(bufferedHole.GetGeometryRef(0));
                //bufferedShapefile.RemoveGeometry(1);
                holeConcaveHulls.get(id).get(holeids.get(0)).clear();
                // iterate the points in bufHole
                System.out.println(bufHole.GetPointCount());
                for (int j = 0; j < bufHole.GetPointCount(); j++) {
                    double[] point = bufHole.GetPoint(j);

                    holeConcaveHulls.get(id).get(holeids.get(0)).add(new tools.ConcaveHull.Point(point[0], point[1]));

                }
            }else{
                System.out.println("Unknown geometry type: " + bufferedHole.GetGeometryName());
                System.exit(1);
            }



        }

        outShpFeat.SetField("id",id);

        outShpFeat.SetGeometryDirectly(bufferedShapefile);

        outShpLayer.CreateFeature(outShpFeat);
        outShpLayer.SyncToDisk();
        System.out.println("features: " + outShpLayer.GetFeatureCount());

        allStandsShapeFileLayer.CreateFeature(outShpFeat);
        Geometry geom = (Geometry)bufferedShapefile.clone();

        return geom;
    }
    public void readShapeFiles(String shapeFile) throws IOException {

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



        for(long i = 0; i < shapeFileLayer.GetFeatureCount(); i++ ) {

            if(true) {

                Feature tempF = shapeFileLayer.GetFeature(i);

                int id = (int)tempF.GetFieldAsDouble("MT_KORJUUT");

                if(id == 1000230805)
                    debugPrint = true;

                if(usedStandids.containsKey(id)){
                    usedStandids.put(id, usedStandids.get(id) + 1);
                }else{
                    usedStandids.put(id, 1);
                }
                if(excludedStands.contains(id)) {
                    nExcluded++;
                    continue;
                }

                //if(id == null){

                //    System.out.println("null");
                //    System.exit(1);
                //}

                //tempF.GetFiel
                System.out.println("id: " + id + " " + tempF.GetFieldCount());
                Geometry tempG = tempF.GetGeometryRef();


                System.out.println(tempG.GetGeometryName());
// check if geometry is a MultiPolygon
                if (tempG.GetGeometryName().equals("MULTIPOLYGON")) {
                    //System.out.println("here1 " + tempF.GetFieldAsInteger(0));

                    System.out.println("MULTIPOLYGON");

                    int numGeom = tempG.GetGeometryCount();

                    for (int j = 0; j < numGeom; j++) {

                        int numGeom2 = tempG.GetGeometryRef(j).GetGeometryCount();

                        shapeId++;

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

                                    double[] centroid = tempG.GetGeometryRef(j).Centroid().GetPoint(0);
                                    //System.out.println("here3 " + tempF.GetFieldAsInteger(0));
                                    standBounds.put(shapeId, clone2DArray(tempG2.GetPoints()));
                                    //System.out.println("here4 " + tempF.GetFieldAsInteger(0));
                                    standCentroids.put(shapeId, new double[]{centroid[0], centroid[1]});

                                    //System.out.println("here5 " + tempF.GetFieldAsInteger(0));
                                    centroids.add(new KdTree.XYZPoint(centroid[0], centroid[1], 0, shapeId));

                                    System.out.println("Centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);

                                    tempP.addOuterRing(clone2DArray(tempG2.GetPoints()));
                                    tempP.setId(shapeId);
                                    standPolygons.put(shapeId, tempP);


                                    standPolygons.put(shapeId, new Polygon());

                                    if (usedIds.contains(shapeId)) {
                                        System.out.println("duplicate id: " + shapeId);
                                        System.exit(1);
                                    }
                                    usedIds.add(shapeId);

                                }else{
                                    standHoles.put(shapeId, clone2DArray(tempG2.GetPoints()));

                                    standPolygons.get(shapeId).addHole(clone2DArray(tempG2.GetPoints()));

                                }


                                //System.out.println("centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);
                                //System.out.println("here6 " + tempF.GetFieldAsInteger(0));
                            }
                        }
                    }
                } else if (tempG.GetGeometryName().equals("POLYGON")) {
                    
                    Geometry tempG2 = tempG.GetGeometryRef(0);


                    //System.out.println(tempG2.GetPointCount() + " " + tempG2.GetGeometryName().equals("LINEARRING"));
                    shapeId++;

                    int numGeom = tempG.GetGeometryCount();

                    System.out.println("numGeom: " + numGeom);

                    if (tempG2.GetGeometryName().equals("LINEARRING")) {


                        for(int j = 0; j < numGeom; j++) {

                            Geometry tempG3 = tempG.GetGeometryRef(j);


                            System.out.println("POINTS: " + tempG3.GetPointCount());

                            if (tempG3 == null || tempG3.GetPoints() == null) {
                                continue;
                            }


                            if (j == 0){

                                Polygon tempP = new Polygon();

                                double[] centroid = tempG.Centroid().GetPoint(0);
                                //System.out.println("here3 " + tempF.GetFieldAsInteger(0));
                                standBounds.put(shapeId, clone2DArray(tempG3.GetPoints()));
                                //System.out.println("here4 " + tempF.GetFieldAsInteger(0));
                                standCentroids.put(shapeId, new double[]{centroid[0], centroid[1]});

                                //System.out.println("here5 " + tempF.GetFieldAsInteger(0));

                                System.out.println("centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);

                                centroids.add(new KdTree.XYZPoint(centroid[0], centroid[1], 0, shapeId));

                                tempP.addOuterRing(clone2DArray(tempG3.GetPoints()));
                                tempP.setId(shapeId);
                                standPolygons.put(shapeId, tempP);


                                standPolygons.put(shapeId, new Polygon());

                                if (usedIds.contains(shapeId)) {
                                    System.out.println("duplicate id: " + shapeId);
                                    System.exit(1);
                                }
                                usedIds.add(shapeId);

                            }else{

                                standHoles.put(shapeId, clone2DArray(tempG3.GetPoints()));
                                standPolygons.get(shapeId).addHole(clone2DArray(tempG3.GetPoints()));

                            }
                        }
                        //System.out.println("centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);
                    }
                } else {
                    // handle other types of geometries if needed
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

        System.out.println(standBounds.size() + " stand bounds read.");
        System.out.println(standCentroids.size() + " stand centroids read.");
        System.out.println(nExcluded + " stands rejected");
        System.out.println("holes: " + standHoles.size());

        if(false)
        for(int i : standHoles.keySet()){
            System.out.println("stand: " + i + " holes: " + standHoles.get(i).length);
        }
        //System.exit(1);
    }

    public static double[][] clone2DArray(double[][] original) {
        int rows = original.length;
        double[][] clone = new double[rows][];

        for (int i = 0; i < rows; i++) {
            int columns = original[i].length;
            clone[i] = new double[columns];
            System.arraycopy(original[i], 0, clone[i], 0, columns);
        }

        return clone;
    }

    public boolean pointInPolygon(ArrayList<ConcaveHull.Point> vertices, double[] point) {


        int i;
        int j;
        boolean result = false;
        for (i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            if ((vertices.get(i).getY() > point[1]) != (vertices.get(j).getY() > point[1]) &&
                    (point[0] < (vertices.get(j).getX() - vertices.get(i).getX()) * (point[1] - vertices.get(i).getY()) / (vertices.get(j).getY() - vertices.get(i).getY()) + vertices.get(i).getX())) {
                result = !result;
            }
        }

        return result;
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

    public boolean pointInPolygon(double x, double y, ArrayList<ConcaveHull.Point> polygonPoints){

        int i;
        int j;
        boolean result = false;
        for (i = 0, j = polygonPoints.size() - 1; i < polygonPoints.size(); j = i++) {
            if ((polygonPoints.get(i).getY() > y) != (polygonPoints.get(j).getY() > y) &&
                    (x < (polygonPoints.get(j).getX() - polygonPoints.get(i).getX()) * (y - polygonPoints.get(i).getY()) / (polygonPoints.get(j).getY() - polygonPoints.get(i).getY()) + polygonPoints.get(i).getX())) {
                result = !result;
            }
        }

        return result;
    }

    public boolean pointInPolygon(List<ConcaveHull.Point> polygon, double[] point) {

        int i;
        int j;
        boolean result = false;
        for (i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            if ((polygon.get(i).getY() > point[1]) != (polygon.get(j).getY() > point[1]) &&
                    (point[0] < (polygon.get(j).getX() - polygon.get(i).getX()) * (point[1] - polygon.get(i).getY()) / (polygon.get(j).getY() - polygon.get(i).getY()) + polygon.get(i).getX())) {
                result = !result;
            }
        }

        return result;
    }

    public static void print2DArray(double[][] arr) {

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                System.out.print(arr[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static double distanceToPolygonBorder(double pointX, double pointY, ArrayList<ConcaveHull.Point> hull) {
        double minDistance = Double.MAX_VALUE;
        int numPoints = hull.size();

        for (int i = 0; i < numPoints; i++) {
            ConcaveHull.Point currentPoint = hull.get(i);
            ConcaveHull.Point nextPoint = hull.get((i + 1) % numPoints);

            double distance = distanceToSegment(pointX, pointY, currentPoint.getX(), currentPoint.getY(), nextPoint.getX(), nextPoint.getY());
            minDistance = Math.min(minDistance, distance);
        }

        return minDistance;
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

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }


    public static double calculatePolygonArea(List<ConcaveHull.Point> vertices) {
        int numVertices = vertices.size();
        double area = 0.0;

        for (int i = 0; i < numVertices; i++) {
            ConcaveHull.Point currentVertex = vertices.get(i);
            ConcaveHull.Point nextVertex = vertices.get((i + 1) % numVertices); // Wrap around for the last vertex

            double x1 = currentVertex.getX();
            double y1 = currentVertex.getY();
            double x2 = nextVertex.getX();
            double y2 = nextVertex.getY();

            area += (x1 * y2 - x2 * y1);
        }

        // Take the absolute value and divide by 2 to get the polygon's area
        area = Math.abs(area) / 2.0;

        return area;
    }

    public double[] randomPointInPolygon(ArrayList<ConcaveHull.Point> vertices, double minX, double maxX, double minY, double maxY,
                                         double distanceToBorder, double distanceToAnotherPlot, KdTree tmpTreePlots, boolean treeEmpty, Random rand,
                                         int standId, HashMap<Integer, HashMap<Integer, List<ConcaveHull.Point>>> holeConcaveHulls ){

        boolean pointInPolygon = false;
        boolean tooCloseToBorder = true;
        boolean tooCloseToAnotherPlot = true;
        double[] xy = new double[2];
        int numberOfAttempts = 0;
        KdTree.XYZPoint point = new KdTree.XYZPoint(0,0,0);

        while(!pointInPolygon || tooCloseToBorder){

            //if it takes too long to find a point, return 0,0
            if(numberOfAttempts++ > 1000)
                return new double[]{0,0};

            xy[0] = rand.nextDouble() * (maxX - minX) + minX;
            xy[1] = rand.nextDouble() * (maxY - minY) + minY;
            point.x = xy[0];
            point.y = xy[1];

            pointInPolygon = pointInPolygon(vertices, xy);
            tooCloseToBorder = distanceToPolygonBorder(xy[0], xy[1], vertices) < distanceToBorder;

            boolean tooCloseToHole = false;

            double distanceToHole = 0.0;

            //if(this.standPolygons.get(standId).hasHole()){
            if(holeConcaveHulls.containsKey(standId)){

                //for(int i = 0; i < this.standPolygons.get(standId).holes.size(); i++){
                for(int i : holeConcaveHulls.get(standId).keySet()){

                    distanceToHole = pointDistanceToHole(holeConcaveHulls.get(standId).get(i), xy[0], xy[1]) ;
                    tooCloseToHole = distanceToHole < distanceToBorder;

                    if(pointInPolygon(holeConcaveHulls.get(standId).get(i), xy)){
                        tooCloseToHole = true;
                    }

                    if(tooCloseToHole)
                        break;
                }

            }

            if(tooCloseToHole)
                tooCloseToBorder = true;

            if(!treeEmpty) {
                List<KdTree.XYZPoint> nearestNeighbour = (List<KdTree.XYZPoint>) tmpTreePlots.nearestNeighbourSearch(1, point);
                tooCloseToAnotherPlot = nearestNeighbour.get(0).euclideanDistance(point) < distanceToAnotherPlot;
            }else{
                tooCloseToAnotherPlot = false;
            }
            if(pointInPolygon && !tooCloseToBorder && !tooCloseToAnotherPlot) {

                if(true)
                if(standId == 115){
                    System.out.println("DiSANCE TO HOLE " + distanceToHole + " " + this.runningPlotId);

                }
                return xy;
            }

        }

        return new double[]{0,0};
    }

    public double[] getPolygonBoundingBox(List<ConcaveHull.Point> polygon){

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for(ConcaveHull.Point p : polygon){

            if(p.getX() < minX)
                minX = p.getX();

            if(p.getX() > maxX)
                maxX = p.getX();

            if(p.getY() < minY)
                minY = p.getY();

            if(p.getY() > maxY)
                maxY = p.getY();

        }

        return new double[]{minX, maxX, minY, maxY};

    }

    public double pointDistanceToHole(List<ConcaveHull.Point> hole, double x, double y){

        double minDistance = Double.MAX_VALUE;
        int numPoints = hole.size();

        for (int i = 0; i < numPoints; i++) {
            ConcaveHull.Point currentPoint = hole.get(i);
            ConcaveHull.Point nextPoint = hole.get((i + 1) % numPoints);

            double distance = distanceToSegment(x, y, currentPoint.getX(), currentPoint.getY(), nextPoint.getX(), nextPoint.getY());
            minDistance = Math.min(minDistance, distance);
        }

        return minDistance;

    }


    public void createPlots(HashMap<Integer, Tree> trees, HashMap<Integer, ArrayList<ConcaveHull.Point>> hulls, Random rand,
                            HashMap<Integer, HashMap<Integer, List<ConcaveHull.Point>>> holeConcaveHulls){

        double plotAreaSquaremeters = Math.PI * Math.pow(plotRadius, 2);

        double plotAreaHectares = plotAreaSquaremeters / 10000;
        double plotAreaHectaresSquare = (16.0 * 16.0) / 10000;

        HashMap<Integer, ArrayList<Tree>> treesPerStand = new HashMap<Integer, ArrayList<Tree>>();


        for(Map.Entry<Integer, Tree> entry : trees.entrySet()){

            if(!entry.getValue().trulyInStand)
                continue;

            int standId = entry.getValue().standId;

            if(!treesPerStand.containsKey(standId)){
                treesPerStand.put(standId, new ArrayList<Tree>());
            }

            treesPerStand.get(standId).add(entry.getValue());

        }



        KdTree.XYZPoint tmpPoint = new KdTree.XYZPoint(0,0,0,0);

        for(int standid : treesPerStand.keySet()){

            double polygonArea = calculatePolygonArea(hulls.get(standid));

            int numberOfPlots = (int)((polygonArea / plotAreaSquaremeters) * numberOfPlotsPerStand);

            ArrayList<Tree> treesInStand = treesPerStand.get(standid);

            double currentX = treesInStand.get(0).getX_coordinate_machine();
            double currentY = treesInStand.get(0).getY_coordinate_machine();

            KdTree tmpTree = new KdTree();
            KdTree tmpTreePlots = new KdTree();


            for(Tree tree : treesInStand){

                KdTree.XYZPoint point = new KdTree.XYZPoint(tree.getX_coordinate_estimated(), tree.getY_coordinate_estimated(), 0, tree.id);

                tmpTree.add(point);

            }



            int simulatedPlots = 0;

            double[] polygonBoundingBox = getPolygonBoundingBox(hulls.get(standid));
            boolean treeEmpty = true;
            if(!aR.noEstimation){
                while(simulatedPlots < numberOfPlots){

                    //System.out.println("1");
                    double[] randomPoint = randomPointInPolygon(hulls.get(standid), polygonBoundingBox[0], polygonBoundingBox[1],
                            polygonBoundingBox[2], polygonBoundingBox[3], 11.33, this.plotRadius * 1.25, tmpTreePlots, treeEmpty, rand, standid,
                            holeConcaveHulls);
                    //System.out.println("2");
                    if(randomPoint[0] == 0 && randomPoint[1] == 0) {
                        simulatedPlots++;
                        continue;
                    }

                    treeEmpty = false;
                    tmpTreePlots.add(new KdTree.XYZPoint(randomPoint[0], randomPoint[1], 0, 0));
                    currentX = randomPoint[0];
                    currentY = randomPoint[1];

                    //List<double[]> circle = calculateCircleBoundary(currentX, currentY, 9);

                    tmpPoint.x = currentX;
                    tmpPoint.y = currentY;
                    tmpPoint.z = 0;

                    List<KdTree.XYZPoint> nearestNeighbour = (List<KdTree.XYZPoint>) tmpTree.nearestNeighbourSearch(100, tmpPoint);

                    //System.out.println("3");
                    double sum_v_log_pine = 0;
                    double sum_v_log_pine_squarePlot = 0;
                    double sum_v_log_spruce = 0;
                    double sum_v_log_spruce_squarePlot = 0;

                    double sum_v_log_birch = 0;
                    double sum_v_log_birch_squarePlot = 0;
                    double sum_v_pulp_pine = 0;
                    double sum_v_pulp_pine_squarePlot = 0;
                    double sum_v_pulp_spruce = 0;
                    double sum_v_pulp_spruce_squarePlot = 0;
                    double sum_v_pulp_birch = 0;
                    double sum_v_pulp_birch_squarePlot = 0;

                    double sum_v_energy_pine = 0;
                    double sum_v_energy_pine_squarePlot = 0;
                    double sum_v_energy_spruce = 0;
                    double sum_v_energy_spruce_squarePlot = 0;
                    double sum_v_energy_birch = 0;
                    double sum_v_energy_birch_squarePlot = 0;


                    for(Tree tree : treesInStand){
                        //Tree currentTree = trees.get(point.getIndex());
                        Tree currentTree = tree;
                    //for(KdTree.XYZPoint point : nearestNeighbour){

                        //Tree currentTree = trees.get(point.getIndex());

                        double distanceToPlotCenter = euclideanDistance(currentTree.getX_coordinate_estimated(), currentTree.getY_coordinate_estimated(),
                                currentX, currentY);

                        if(distanceToPlotCenter < plotRadius ){

                            if(currentTree.species == 1) {
                                sum_v_energy_pine += currentTree.volume_energia;
                                sum_v_log_pine += currentTree.volume_tukki;
                                sum_v_pulp_pine += currentTree.volume_pikkutukki;
                                sum_v_pulp_pine += currentTree.volume_kuitu;
                            }
                            if(currentTree.species == 2) {
                                sum_v_energy_spruce += currentTree.volume_energia;
                                sum_v_log_spruce += currentTree.volume_tukki;
                                sum_v_pulp_spruce += currentTree.volume_pikkutukki;
                                sum_v_pulp_spruce += currentTree.volume_kuitu;
                            }
                            if(currentTree.species == 3) {
                                sum_v_energy_birch += currentTree.volume_energia;
                                sum_v_log_birch += currentTree.volume_tukki;
                                sum_v_pulp_birch += currentTree.volume_pikkutukki;
                                sum_v_pulp_birch += currentTree.volume_kuitu;
                            }

                        }

                        //check if the tree is in the square plot (16x16m)
                        if(currentTree.getX_coordinate_estimated() > currentX - 8 && currentTree.getX_coordinate_estimated() < currentX + 8 &&
                                currentTree.getY_coordinate_estimated() > currentY - 8 && currentTree.getY_coordinate_estimated() < currentY + 8){
                            if(currentTree.species == 1) {
                                sum_v_energy_pine_squarePlot += currentTree.volume_energia;
                                sum_v_log_pine_squarePlot += currentTree.volume_tukki;
                                sum_v_pulp_pine_squarePlot += currentTree.volume_pikkutukki;
                                sum_v_pulp_pine_squarePlot += currentTree.volume_kuitu;
                            }
                            if(currentTree.species == 2) {
                                sum_v_energy_spruce_squarePlot += currentTree.volume_energia;
                                sum_v_log_spruce_squarePlot += currentTree.volume_tukki;
                                sum_v_pulp_spruce_squarePlot += currentTree.volume_pikkutukki;
                                sum_v_pulp_spruce_squarePlot += currentTree.volume_kuitu;
                            }
                            if(currentTree.species == 3) {
                                sum_v_energy_birch_squarePlot += currentTree.volume_energia;
                                sum_v_log_birch_squarePlot += currentTree.volume_tukki;
                                sum_v_pulp_birch_squarePlot += currentTree.volume_pikkutukki;
                                sum_v_pulp_birch_squarePlot += currentTree.volume_kuitu;
                            }
                        }

                    }

                    addPlotToShapefile(currentX, currentY, this.plotRadius, this.runningPlotId, standid,
                            sum_v_log_pine/plotAreaHectares, sum_v_log_spruce/plotAreaHectares, sum_v_log_birch/plotAreaHectares,
                            sum_v_pulp_pine/plotAreaHectares, sum_v_pulp_spruce/plotAreaHectares, sum_v_pulp_birch/plotAreaHectares,
                            sum_v_energy_pine/plotAreaHectares, sum_v_energy_spruce/plotAreaHectares, sum_v_energy_birch/plotAreaHectares);

                    addPlotToShapefile_square(currentX, currentY, 8.0, this.runningPlotId++, standid,
                            sum_v_log_pine_squarePlot/plotAreaHectaresSquare, sum_v_log_spruce_squarePlot/plotAreaHectaresSquare, sum_v_log_birch_squarePlot/plotAreaHectaresSquare,
                            sum_v_pulp_pine_squarePlot/plotAreaHectaresSquare, sum_v_pulp_spruce_squarePlot/plotAreaHectaresSquare, sum_v_pulp_birch_squarePlot/plotAreaHectaresSquare,
                            sum_v_energy_pine_squarePlot/plotAreaHectaresSquare, sum_v_energy_spruce_squarePlot/plotAreaHectaresSquare, sum_v_energy_birch_squarePlot/plotAreaHectaresSquare);
                    simulatedPlots++;


                }
            }



            // This is the ajoura way of doing it
            if(aR.noEstimation)
            for(Tree tree : treesInStand){

                double distanceToPreviousPlot = euclideanDistance(tree.getX_coordinate_machine(), tree.getY_coordinate_machine(), currentX, currentY);
                double distanceToBoundary = distanceToPolygonBorder(tree.getX_coordinate_machine(), tree.getY_coordinate_machine(), hulls.get(standid));

                boolean reject = false;

                if(standPolygons.get(standid).hasHole()){

                    for(int i = 0; i < standPolygons.get(standid).holes.size(); i++){

                        double distance = standPolygons.get(standid).pointDistanceToHole(i, tree.getX_coordinate_machine(), tree.getY_coordinate_machine());
                        boolean inside = standPolygons.get(standid).pointInPolygon(standPolygons.get(standid).getHole(i), tree.getX_coordinate_machine(), tree.getY_coordinate_machine());

                        if(distance < 11.33 || inside)
                            reject = true;

                    }

                }

                if(!reject)
                if(distanceToPreviousPlot > (this.plotRadius * 1.25) && distanceToBoundary > 11.33){


                    currentX = tree.getX_coordinate_machine();
                    currentY = tree.getY_coordinate_machine();

                    //List<double[]> circle = calculateCircleBoundary(currentX, currentY, 9);


                    tmpPoint.x = tree.getX_coordinate_machine();
                    tmpPoint.y = tree.getY_coordinate_machine();
                    tmpPoint.z = 0;

                    List<KdTree.XYZPoint> nearestNeighbour = (List<KdTree.XYZPoint>) tmpTree.nearestNeighbourSearch(100, tmpPoint);

                    double sum_v_log_pine = 0;
                    double sum_v_log_pine_squarePlot = 0;
                    double sum_v_log_spruce = 0;
                    double sum_v_log_spruce_squarePlot = 0;

                    double sum_v_log_birch = 0;
                    double sum_v_log_birch_squarePlot = 0;
                    double sum_v_pulp_pine = 0;
                    double sum_v_pulp_pine_squarePlot = 0;
                    double sum_v_pulp_spruce = 0;
                    double sum_v_pulp_spruce_squarePlot = 0;
                    double sum_v_pulp_birch = 0;
                    double sum_v_pulp_birch_squarePlot = 0;

                    double sum_v_energy_pine = 0;
                    double sum_v_energy_pine_squarePlot = 0;
                    double sum_v_energy_spruce = 0;
                    double sum_v_energy_spruce_squarePlot = 0;
                    double sum_v_energy_birch = 0;
                    double sum_v_energy_birch_squarePlot = 0;


                    for(Tree tree_ : treesInStand){
                        //Tree currentTree = trees.get(point.getIndex());
                        Tree currentTree = tree_;
                    //for(KdTree.XYZPoint point : nearestNeighbour){

                        //Tree currentTree = trees.get(point.getIndex());

                        double distanceToPlotCenter = euclideanDistance(currentTree.getX_coordinate_machine(), currentTree.getY_coordinate_machine(),
                                currentX, currentY);

                        if(distanceToPlotCenter < 9 ){

                            if(currentTree.species == 1) {
                                sum_v_energy_pine += currentTree.volume_energia;
                                sum_v_log_pine += currentTree.volume_tukki;
                                sum_v_pulp_pine += currentTree.volume_pikkutukki;
                                sum_v_pulp_pine += currentTree.volume_kuitu;
                            }
                            if(currentTree.species == 2) {
                                sum_v_energy_spruce += currentTree.volume_energia;
                                sum_v_log_spruce += currentTree.volume_tukki;
                                sum_v_pulp_spruce += currentTree.volume_pikkutukki;
                                sum_v_pulp_spruce += currentTree.volume_kuitu;
                            }
                            if(currentTree.species == 3) {
                                sum_v_energy_birch += currentTree.volume_energia;
                                sum_v_log_birch += currentTree.volume_tukki;
                                sum_v_pulp_birch += currentTree.volume_pikkutukki;
                                sum_v_pulp_birch += currentTree.volume_kuitu;
                            }

                        }

                        if(currentTree.getX_coordinate_estimated() > currentX - 8 && currentTree.getX_coordinate_estimated() < currentX + 8 &&
                                currentTree.getY_coordinate_estimated() > currentY - 8 && currentTree.getY_coordinate_estimated() < currentY + 8){
                            if(currentTree.species == 1) {
                                sum_v_energy_pine_squarePlot += currentTree.volume_energia;
                                sum_v_log_pine_squarePlot += currentTree.volume_tukki;
                                sum_v_pulp_pine_squarePlot += currentTree.volume_pikkutukki;
                                sum_v_pulp_pine_squarePlot += currentTree.volume_kuitu;
                            }
                            if(currentTree.species == 2) {
                                sum_v_energy_spruce_squarePlot += currentTree.volume_energia;
                                sum_v_log_spruce_squarePlot += currentTree.volume_tukki;
                                sum_v_pulp_spruce_squarePlot += currentTree.volume_pikkutukki;
                                sum_v_pulp_spruce_squarePlot += currentTree.volume_kuitu;
                            }
                            if(currentTree.species == 3) {
                                sum_v_energy_birch_squarePlot += currentTree.volume_energia;
                                sum_v_log_birch_squarePlot += currentTree.volume_tukki;
                                sum_v_pulp_birch_squarePlot += currentTree.volume_pikkutukki;
                                sum_v_pulp_birch_squarePlot += currentTree.volume_kuitu;
                            }
                        }

                    }

                    addPlotToShapefile(currentX, currentY, this.plotRadius, this.runningPlotId, standid,
                            sum_v_log_pine/plotAreaHectares, sum_v_log_spruce/plotAreaHectares, sum_v_log_birch/plotAreaHectares,
                            sum_v_pulp_pine/plotAreaHectares, sum_v_pulp_spruce/plotAreaHectares, sum_v_pulp_birch/plotAreaHectares,
                            sum_v_energy_pine/plotAreaHectares, sum_v_energy_spruce/plotAreaHectares, sum_v_energy_birch/plotAreaHectares);


                    addPlotToShapefile_square(currentX, currentY, 8.0, this.runningPlotId++, standid,
                            sum_v_log_pine_squarePlot/plotAreaHectaresSquare, sum_v_log_spruce_squarePlot/plotAreaHectaresSquare, sum_v_log_birch_squarePlot/plotAreaHectaresSquare,
                            sum_v_pulp_pine_squarePlot/plotAreaHectaresSquare, sum_v_pulp_spruce_squarePlot/plotAreaHectaresSquare, sum_v_pulp_birch_squarePlot/plotAreaHectaresSquare,
                            sum_v_energy_pine_squarePlot/plotAreaHectaresSquare, sum_v_energy_spruce_squarePlot/plotAreaHectaresSquare, sum_v_energy_birch_squarePlot/plotAreaHectaresSquare);
                    simulatedPlots++;
                }

            }
        }
    }


    public void prepareMML(){

        double anchor_x = minX_finnishMap6k;
        double anchor_y = maxY_finnishMap6k;

        double x_diff = minX - anchor_x;
        double y_diff = anchor_y - maxY;

        int n_x = (int)Math.floor(x_diff / cellSizeVMI);
        int n_y = (int)Math.floor(y_diff / cellSizeVMI);

        this.VMI_minIndexX = n_x;
        this.VMI_maxIndexY = n_y;


        this.minX = n_x * cellSizeVMI + anchor_x;
        this.maxY = anchor_y - n_y * cellSizeVMI;

        this.orig_x = this.minX;
        this.orig_y = this.maxY;

    }

    public double rectangleOverlapsPolygon(double minx, double miny, double maxx, double maxy, ArrayList<ConcaveHull.Point> polygonPoints){

        int totalPoints = 4; // Four corners of the rectangle
        int insidePoints = 0;

        // Check each corner of the rectangle
        if (pointInPolygon(minx, miny, polygonPoints)) insidePoints++;
        if (pointInPolygon(minx, maxy, polygonPoints)) insidePoints++;
        if (pointInPolygon(maxx, miny, polygonPoints)) insidePoints++;
        if (pointInPolygon(maxx, maxy, polygonPoints)) insidePoints++;


        if (insidePoints == totalPoints) {
            // Rectangle is completely inside the polygon
            return 100.0;
        } else {
            // Calculate overlap percentage based on the area of overlap
            double rectangleArea = (maxx - minx) * (maxy - miny);


            double overlapArea = calculateOverlapArea(minx, miny, maxx, maxy, polygonPoints);

            System.out.println("overlapArea: " + overlapArea);
            return (overlapArea / rectangleArea) * 100.0;
        }

    }

    // Helper method to calculate the area of a triangle given its three vertices
    private static double triangleArea(double x1, double y1, double x2, double y2, double x3, double y3) {
        return Math.abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2.0);
    }

    private static double calculateOverlapArea(double minx, double miny, double maxx, double maxy, ArrayList<ConcaveHull.Point> polygonPoints) {
        double overlapArea = 0.0;

        // Iterate over each triangle formed by a polygon edge and two rectangle corners
        for (int i = 0; i < polygonPoints.size(); i++) {
            ConcaveHull.Point p1 = polygonPoints.get(i);
            ConcaveHull.Point p2 = polygonPoints.get((i + 1) % polygonPoints.size());

            // Calculate the area of the triangle formed by p1, p2, and the lower-left corner of the rectangle
            double triangleArea1 = triangleArea(p1.getX(), p1.getY(), p2.getX(), p2.getY(), minx, miny);

            // Calculate the area of the triangle formed by p1, p2, and the upper-right corner of the rectangle
            double triangleArea2 = triangleArea(p1.getX(), p1.getY(), p2.getX(), p2.getY(), maxx, maxy);

            // Add the absolute difference between the two triangle areas to the overlapArea (for clockwise orientation)
            overlapArea += Math.abs(triangleArea1 - triangleArea2);
        }

        // Ensure the result is non-negative and does not exceed the area of the rectangle
        return Math.min(overlapArea, rectangleArea(minx, miny, maxx, maxy));
    }

    // Helper method to calculate the area of a rectangle given its coordinates
    private static double rectangleArea(double minx, double miny, double maxx, double maxy) {
        return Math.abs((maxx - minx) * (maxy - miny));
    }



    public void createGrid(HashMap<Integer, Tree> trees, HashMap<Integer, List<ConcaveHull.Point>> hulls, Random rand, int whichOne,
                           HashMap<Integer, HashMap<Integer, List<ConcaveHull.Point>>> holePoints, HashMap<Integer, Geometry> standGeometries ){

        double plotAreaSquaremeters = Math.PI * Math.pow(plotRadius, 2);

        double gridAreaSquaremeters = 16.0 * 16.0;
        double gridAreaHectares = gridAreaSquaremeters / 10000;

        double plotAreaHectares = plotAreaSquaremeters / 10000;
        double plotAreaHectaresSquare = (16.0 * 16.0) / 10000;

        HashMap<Integer, ArrayList<Tree>> treesPerStand = new HashMap<Integer, ArrayList<Tree>>();
        int counter_ = 0;
        for(Map.Entry<Integer, Tree> entry : trees.entrySet()){

            if(!entry.getValue().trulyInStand)
                continue;

            counter_++;
            int standId = entry.getValue().standId;

            if(!treesPerStand.containsKey(standId)){
                treesPerStand.put(standId, new ArrayList<Tree>());
            }

            treesPerStand.get(standId).add(entry.getValue());

            //System.out.println(entry.getValue().id + " == " + entry.getKey());
            System.out.println(trees.get(entry.getValue().id).id + " == " + entry.getValue().id);
        }



        KdTree.XYZPoint tmpPoint = new KdTree.XYZPoint(0,0,0,0);

        for(int standid : treesPerStand.keySet()){

            double polygonArea = calculatePolygonArea(hulls.get(standid));

            int numberOfPlots = (int)((polygonArea / plotAreaSquaremeters) * numberOfPlotsPerStand);

            ArrayList<Tree> treesInStand = treesPerStand.get(standid);

            double currentX = treesInStand.get(0).getX_coordinate_machine();
            double currentY = treesInStand.get(0).getY_coordinate_machine();

            KdTree tmpTree = new KdTree();
            KdTree tmpTreePlots = new KdTree();

            for(Tree tree : treesInStand){

                double treex = tree.getX_coordinate_estimated();
                double treey = tree.getY_coordinate_estimated();

                if(aR.noEstimation) {
                    treey = tree.getY_coordinate_machine();
                    treex = tree.getX_coordinate_machine();
                }

                KdTree.XYZPoint point = new KdTree.XYZPoint(treex, treey, 0);
                point.setIndex(tree.id);
                tmpTree.add(point);

            }

            // iterate tmpTree

            Iterator<KdTree.XYZPoint> iterator = tmpTree.iterator();
            int countTrees = 0;
            while(iterator.hasNext()){
                KdTree.XYZPoint point = iterator.next();
                countTrees++;
            }

            //System.out.println("Count trees2: " + countTrees);
            //System.out.println("Count trees2: " + treesInStand.size());

            //System.exit(1);
            int simulatedPlots = 0;

            double[] polygonBoundingBox = getPolygonBoundingBox(hulls.get(standid));

            KarttaLehtiJako karttaLehtiJako = new KarttaLehtiJako();

            try {
                karttaLehtiJako.readFromFile(new File(""));
            }catch (Exception e){
                e.printStackTrace();
            }

            this.minX = polygonBoundingBox[0];
            this.maxX = polygonBoundingBox[1];
            this.minY = polygonBoundingBox[2];
            this.maxY = polygonBoundingBox[3];

            this.prepareMML();

            System.out.println(this.orig_x + " " + this.orig_y);
            System.out.println(Arrays.toString(polygonBoundingBox));
            System.out.println(standid);

            int nCellsX = (int)Math.ceil((this.maxX  - this.minX) / this.cellSizeVMI);
            int nCellsY = (int)Math.ceil((this.maxY  - this.minY) / this.cellSizeVMI);

            zonalExtractor extractor = new zonalExtractor();
            extractor.setResolution(this.cellSizeVMI);
            extractor.setOriginX(this.orig_x);
            extractor.setOriginY(this.orig_y);
            extractor.setPolygon(hulls.get(standid));
            //extractor.setStandGeometry(standGeometries.get(standid));
            extractor.setXdim(nCellsX);
            extractor.setYdim(nCellsY);
            extractor.normalizePolygon();
            extractor.traversePolygon();

          // set the holes to 0
            if(true)
            if(holePoints.containsKey(standid)){

                System.out.println("HOLEPOINTS SIZE: " + holePoints.get(standid).size());
                System.out.println("HOLEPOINTS SIZE: " + holePoints.get(standid).size());
                System.out.println("HOLEPOINTS SIZE: " + holePoints.get(standid).size());
                System.out.println("HOLEPOINTS SIZE: " + holePoints.get(standid).size());
                System.out.println("HOLEPOINTS SIZE: " + holePoints.get(standid).size());
                extractor.setStandGeometry(standGeometries.get(standid));
                extractor.normalizeStandGeometry(null, standid);
                System.out.println("SUCCESS");
                //System.exit(1);
                for(int i : holePoints.get(standid).keySet()){

                    extractor.setPolygon(holePoints.get(standid).get(i));
                    extractor.setPolygon2(hulls.get(standid));

                    extractor.normalizePolygonHole();
                    extractor.normalizePolygon2();
                    extractor.traverseHole();

                }

                System.out.println("SUFFEC");


            }

            //if(holePoints.containsKey(standid))
            //System.exit(1);


            for(int x = 0; x < nCellsX; x++){
                for(int y = 0; y < nCellsY; y++){

                    zonalCell cell = extractor.getCell(x, y);

                    if(cell != null){

                        double[] cellExtent = new double[]{this.orig_x + x * this.cellSizeVMI, orig_x + (x + 1) * this.cellSizeVMI
                                , this.orig_y - (y + 1) * this.cellSizeVMI, this.orig_y - y * this.cellSizeVMI};

                        //System.out.println(Arrays.toString(cellExtent));
                        double cellOverlapPercentage = cell.intersectArea;

                        if(cell.intersectAreas.size() > 0){

                            cellOverlapPercentage= cell.intersectAreas.get(0);

                            if(false)
                            for(int i = 0; i < cell.intersectAreas.size(); i++){

                                cellOverlapPercentage -= cell.intersectAreas.get(i);
                                System.out.println(cell.intersectAreas.get(i));
                                System.out.println(cellOverlapPercentage);

                            }
                        }

                        if(cellOverlapPercentage < 0)
                            cellOverlapPercentage = 0;

                        //System.out.println(cell.intersectArea);
                        //System.out.println("overlap: " + cellOverlapPercentage);

                        //tmpPoint.setX((cellExtent[0] + cellExtent[1]) / 2.0);
                        //tmpPoint.setY((cellExtent[2] + cellExtent[3]) / 2.0);

                        tmpPoint = new KdTree.XYZPoint((cellExtent[0] + cellExtent[1]) / 2.0,(cellExtent[2] + cellExtent[3]) / 2.0,0,0);

                        //tmpPoint.z = 0;

                        int k = 100;

                        if(k >= countTrees)
                            k = countTrees;

                        List<KdTree.XYZPoint> nearestNeighbour = (List<KdTree.XYZPoint>) tmpTree.nearestNeighbourSearch2d(k, tmpPoint);
                        //List<KdTree.XYZPoint> nearestNeighbour = (List<KdTree.XYZPoint>) tmpTree.rangeSearch(100, tmpPoint, 20);
                        //System.out.println("3");
                        double sum_v_log_pine = 0;
                        double sum_v_log_pine_squarePlot = 0;
                        double sum_v_log_spruce = 0;
                        double sum_v_log_spruce_squarePlot = 0;

                        double sum_v_log_birch = 0;
                        double sum_v_log_birch_squarePlot = 0;
                        double sum_v_pulp_pine = 0;
                        double sum_v_pulp_pine_squarePlot = 0;
                        double sum_v_pulp_spruce = 0;
                        double sum_v_pulp_spruce_squarePlot = 0;
                        double sum_v_pulp_birch = 0;
                        double sum_v_pulp_birch_squarePlot = 0;

                        double sum_v_energy_pine = 0;
                        double sum_v_energy_pine_squarePlot = 0;
                        double sum_v_energy_spruce = 0;
                        double sum_v_energy_spruce_squarePlot = 0;
                        double sum_v_energy_birch = 0;
                        double sum_v_energy_birch_squarePlot = 0;

                        currentX = tmpPoint.x;
                        currentY = tmpPoint.y;

                        int counter = 0;

                        int numTrees = 0;

                        //for(KdTree.XYZPoint point : nearestNeighbour) {
                        for(Tree tree : treesInStand){
                            //Tree currentTree = trees.get(point.getIndex());
                            Tree currentTree = tree;

                            //System.out.println("distance: " + point.euclideanDistance(tmpPoint));

                            double treeX = currentTree.getX_coordinate_estimated();
                            double treeY = currentTree.getY_coordinate_estimated();

                            if(aR.noEstimation){
                                treeX = currentTree.getX_coordinate_machine();
                                treeY = currentTree.getY_coordinate_machine();

                            }

                            if(treeX > currentX - 8 && treeX < currentX + 8 &&
                                    treeY > currentY - 8 && treeY < currentY + 8){

                                numTrees++;

                                if(currentTree.species == 1) {
                                    sum_v_energy_pine_squarePlot += currentTree.volume_energia;
                                    sum_v_log_pine_squarePlot += currentTree.volume_tukki;
                                    sum_v_pulp_pine_squarePlot += currentTree.volume_pikkutukki;
                                    sum_v_pulp_pine_squarePlot += currentTree.volume_kuitu;
                                }
                                if(currentTree.species == 2) {
                                    sum_v_energy_spruce_squarePlot += currentTree.volume_energia;
                                    sum_v_log_spruce_squarePlot += currentTree.volume_tukki;
                                    sum_v_pulp_spruce_squarePlot += currentTree.volume_pikkutukki;
                                    sum_v_pulp_spruce_squarePlot += currentTree.volume_kuitu;
                                }
                                if(currentTree.species == 3) {
                                    sum_v_energy_birch_squarePlot += currentTree.volume_energia;
                                    sum_v_log_birch_squarePlot += currentTree.volume_tukki;
                                    sum_v_pulp_birch_squarePlot += currentTree.volume_pikkutukki;
                                    sum_v_pulp_birch_squarePlot += currentTree.volume_kuitu;
                                }
                            }

                        }

                        //System.exit(1);
/*
                        if(holeGrid.containsKey(standid)){

                            cellOverlapPercentage = 1.0;
                            if (x >= 0 && x < holeGrid.get(standid).length && y - 1 >= 0 && y - 1 < holeGrid.get(standid)[0].length)
                                if (holeGrid.get(standid)[x][y - 1] < 0) {

                                    cellOverlapPercentage = -1.0;

                                }
                        }
*/
                        //this.writeLineToLogfile(sum_v_energy_pine_squarePlot + "\t" + sum_v_log_pine_squarePlot + "\t" +  sum_v_pulp_pine_squarePlot + "\n");
                        this.writeLineToLogfile(numTrees + "\t" + treesInStand.size() + "\n");

                        if(whichOne == 1)
                        addPlotToShapefile_grid_buffered(currentX, currentY, 8.0, this.runningPlotId++, standid,
                                sum_v_log_pine_squarePlot/plotAreaHectaresSquare, sum_v_log_spruce_squarePlot/plotAreaHectaresSquare, sum_v_log_birch_squarePlot/plotAreaHectaresSquare,
                                sum_v_pulp_pine_squarePlot/plotAreaHectaresSquare, sum_v_pulp_spruce_squarePlot/plotAreaHectaresSquare, sum_v_pulp_birch_squarePlot/plotAreaHectaresSquare,
                                sum_v_energy_pine_squarePlot/plotAreaHectaresSquare, sum_v_energy_spruce_squarePlot/plotAreaHectaresSquare, sum_v_energy_birch_squarePlot/plotAreaHectaresSquare,
                                cellOverlapPercentage);
                        if(whichOne == 2)
                            addPlotToShapefile_grid_original(currentX, currentY, 8.0, this.runningPlotId++, standid,
                                    sum_v_log_pine_squarePlot/plotAreaHectaresSquare, sum_v_log_spruce_squarePlot/plotAreaHectaresSquare, sum_v_log_birch_squarePlot/plotAreaHectaresSquare,
                                    sum_v_pulp_pine_squarePlot/plotAreaHectaresSquare, sum_v_pulp_spruce_squarePlot/plotAreaHectaresSquare, sum_v_pulp_birch_squarePlot/plotAreaHectaresSquare,
                                    sum_v_energy_pine_squarePlot/plotAreaHectaresSquare, sum_v_energy_spruce_squarePlot/plotAreaHectaresSquare, sum_v_energy_birch_squarePlot/plotAreaHectaresSquare,
                                    cellOverlapPercentage);
                    }

                }
            }

            //System.exit(1);

            System.out.println("STANDID: " + standid + " " + numberOfPlots);



        }
    }

    public HashMap<Integer, HashMap<Integer, List<ConcaveHull.Point>>> createHoleDetectionGrid(HashMap<Integer, Tree> trees, HashMap<Integer, ArrayList<ConcaveHull.Point>> hulls, Random rand, int whichOne){

        double plotAreaSquaremeters = Math.PI * Math.pow(plotRadius, 2);

        double gridAreaSquaremeters = 16.0 * 16.0;
        double gridAreaHectares = gridAreaSquaremeters / 10000;

        double plotAreaHectares = plotAreaSquaremeters / 10000;
        double plotAreaHectaresSquare = (16.0 * 16.0) / 10000;

        HashMap<Integer, ArrayList<ArrayList<ConcaveHull.Point>>> output = new HashMap<>();
        HashMap<Integer, HashMap<Integer, List<ConcaveHull.Point>>> output_ = new HashMap<>();
        HashMap<Integer, ArrayList<Tree>> treesPerStand = new HashMap<Integer, ArrayList<Tree>>();

        for(Map.Entry<Integer, Tree> entry : trees.entrySet()){

            if(!entry.getValue().trulyInStand)
                continue;

            int standId = entry.getValue().standId;

            if(!treesPerStand.containsKey(standId)){
                treesPerStand.put(standId, new ArrayList<Tree>());
            }

            treesPerStand.get(standId).add(entry.getValue());

        }

        KdTree.XYZPoint tmpPoint = new KdTree.XYZPoint(0,0,0,0);

        int holeId = 0;


        for(int standid : treesPerStand.keySet()){

            double polygonArea = calculatePolygonArea(hulls.get(standid));

            int numberOfPlots = (int)((polygonArea / plotAreaSquaremeters) * numberOfPlotsPerStand);

            ArrayList<Tree> treesInStand = treesPerStand.get(standid);

            double currentX = treesInStand.get(0).getX_coordinate_machine();
            double currentY = treesInStand.get(0).getY_coordinate_machine();

            KdTree tmpTree = new KdTree();
            KdTree tmpTreePlots = new KdTree();
            int counter = 0;
            for(Tree tree : treesInStand){

                KdTree.XYZPoint point = new KdTree.XYZPoint(tree.getX_coordinate_machine(), tree.getY_coordinate_machine(), 0, tree.id);

                tmpTree.add(point);

            }

            if(standid == 487){

            }

            int simulatedPlots = 0;

            double[] polygonBoundingBox = getPolygonBoundingBox(hulls.get(standid));

            KarttaLehtiJako karttaLehtiJako = new KarttaLehtiJako();

            try {
                karttaLehtiJako.readFromFile(new File(""));
            }catch (Exception e){
                e.printStackTrace();
            }

            this.minX = polygonBoundingBox[0];
            this.maxX = polygonBoundingBox[1];
            this.minY = polygonBoundingBox[2];
            this.maxY = polygonBoundingBox[3];

            this.prepareMML();

            System.out.println(this.orig_x + " " + this.orig_y);
            System.out.println(Arrays.toString(polygonBoundingBox));
            System.out.println(standid);
            int nCellsX = (int)Math.ceil((this.maxX  - this.minX) / this.cellSizeVMI);
            int nCellsY = (int)Math.ceil((this.maxY  - this.minY) / this.cellSizeVMI);

            zonalExtractor extractor = new zonalExtractor();
            extractor.setResolution(this.cellSizeVMI);
            extractor.setOriginX(this.orig_x);
            extractor.setOriginY(this.orig_y);
            extractor.setPolygon(hulls.get(standid));
            extractor.setXdim(nCellsX);
            extractor.setYdim(nCellsY);
            extractor.normalizePolygon();
            extractor.traversePolygon();
/*
            // set the holes to 0
            if(this.standPolygons.get(standid).hasHole()){


                for(int i = 0; i < this.standPolygons.get(standid).holes.size(); i++){
                    extractor.setPolygon(this.standPolygons.get(standid).holes.get(i));
                    extractor.normalizePolygon();
                    extractor.traverseHole();
                }


            }

 */
            System.out.println("SUFFEC");
            //System.exit(1);

            double minDistance = 15.0;

            int[][] potentialHoles = new int[nCellsX][nCellsY];

            File debugOut = null;
            File debugOut2 = null;
            // writer debug
            BufferedWriter writer = null;
            BufferedWriter writer2 = null;


            if(standid == 487){
                debugOut = new File("/home/koomikko/Documents/customer_work/metsahallitus/HPR_data_final/debug.txt");
                debugOut2 = new File("/home/koomikko/Documents/customer_work/metsahallitus/HPR_data_final/debug2.txt");
                if(debugOut.exists())
                    debugOut.delete();

                if(debugOut2.exists())
                    debugOut2.delete();

                try {
                    debugOut.createNewFile();
                    debugOut2.createNewFile();
                }
                catch (Exception e){
                    e.printStackTrace();
                }

                try {
                    writer = new BufferedWriter(new FileWriter(debugOut));
                    writer2 = new BufferedWriter(new FileWriter(debugOut2));
                }
                catch (Exception e){
                    e.printStackTrace();
                }

            }

            if(false)
                continue;

            for(int x = 0; x < nCellsX; x++){
                for(int y = 0; y < nCellsY; y++){

                    zonalCell cell = extractor.getCell(x, y);

                    if(cell != null){

                        double[] cellExtent = new double[]{this.orig_x + x * this.cellSizeVMI, orig_x + (x + 1) * this.cellSizeVMI
                                , this.orig_y - (y + 1) * this.cellSizeVMI, this.orig_y - y * this.cellSizeVMI};

                        double[] cellCenter = new double[]{cellExtent[0] + this.cellSizeVMI / 2.0, cellExtent[3] - this.cellSizeVMI / 2.0};
                        tmpPoint.x = cellCenter[0];
                        tmpPoint.y = cellCenter[1];

                        if(standid == 487){
                            try {
                                writer2.write(cellCenter[0] + " " + cellCenter[1] + " " + 0 + "\n");
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        List<KdTree.XYZPoint> nearestNeighbour = (List<KdTree.XYZPoint>) tmpTree.nearestNeighbourSearch(1, tmpPoint);

                        for(KdTree.XYZPoint point : nearestNeighbour) {

                            Tree currentTree = trees.get(point.getIndex());

                            double distance = euclideanDistance(currentTree.getX_coordinate_machine(), currentTree.getY_coordinate_machine(), cellCenter[0], cellCenter[1]);


                            if(distance > minDistance){
                                potentialHoles[x][y] = point.getIndex();

                            }
                        }
                    }
                }
            }

            int threshold_n_connected = 1;

            HashSet<Integer> holeIds = new HashSet<>();
            HashSet<Integer> usedTreeIds = new HashSet<>();






            for(int x = 0; x < nCellsX; x++){
                for(int y = 0; y < nCellsY; y++) {

                    zonalCell cell = extractor.getCell(x, y);

                    if(cell != null){

                        //if(cell.border)
                        //    continue;

                        double[] cellExtent = new double[]{this.orig_x + x * this.cellSizeVMI, orig_x + (x + 1) * this.cellSizeVMI
                                , this.orig_y - (y + 1) * this.cellSizeVMI, this.orig_y - y * this.cellSizeVMI};

                        int id = x + y * nCellsX;
                        int n_connected = extractor.flood(potentialHoles, x, y, id);

                        if (n_connected > 0)
                            System.out.println("ID: " + id + " " + n_connected);

                        if(n_connected > threshold_n_connected) {

                            holeId++;

                            for(int x_ = 0; x_ < nCellsX; x_++) {
                                for (int y_ = 1; y_ < nCellsY; y_++) {

                                    cellExtent = new double[]{this.orig_x + x_ * this.cellSizeVMI, orig_x + (x_ + 1) * this.cellSizeVMI
                                            , this.orig_y - (y_ + 1) * this.cellSizeVMI, this.orig_y - y_ * this.cellSizeVMI};

                                    //System.out.println(Arrays.toString(cellExtent));
                                     //       System.exit(1);
                                    if(potentialHoles[x_][y_] == -id) {

                                        if(standid == 487){
                                            try {
                                                // write the center of the cell to the file
                                                writer.write(cellExtent[0] + (this.cellSizeVMI / 2.0) + " " + (cellExtent[2] + (this.cellSizeVMI / 2.0)) + " " + potentialHoles[x_][y_] + "\n");
                                            }
                                            catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }

                                        if (!output_.containsKey(standid)) {
                                            output_.put(standid, new HashMap<>());
                                            output_.get(standid).put(holeId, new ArrayList<>());


                                        } else {
                                            //if (output.get(standid).size() == 0) {
                                            if (!output_.get(standid).containsKey(holeId)) {
                                                //output.get(standid).add(new ArrayList<>());
                                                output_.get(standid).put(holeId, new ArrayList<>());
                                            }


                                        }

                                        //if(cell.border){

                                            // add the corners of the cell to outpout_
                                            output_.get(standid).get(holeId).add(new ConcaveHull.Point(cellExtent[0], cellExtent[2]));
                                            output_.get(standid).get(holeId).add(new ConcaveHull.Point(cellExtent[1], cellExtent[2]));
                                            output_.get(standid).get(holeId).add(new ConcaveHull.Point(cellExtent[1], cellExtent[3]));
                                            output_.get(standid).get(holeId).add(new ConcaveHull.Point(cellExtent[0], cellExtent[3]));

                                        //}

                                        tmpPoint.setX(cellExtent[0] + (this.cellSizeVMI));
                                        tmpPoint.setY(cellExtent[2] + (this.cellSizeVMI));
                                        List<KdTree.XYZPoint> nearestNeighbour = (List<KdTree.XYZPoint>) tmpTree.nearestNeighbourSearch(1, tmpPoint);

                                        if(!usedTreeIds.contains(nearestNeighbour.get(0).getIndex())) {
                                            output_.get(standid).get(holeId).add(new ConcaveHull.Point(nearestNeighbour.get(0).getX(), nearestNeighbour.get(0).getY()));
                                            usedTreeIds.add(nearestNeighbour.get(0).getIndex());
                                        }

                                        //output_.get(standid).get(holeId).add(new ConcaveHull.Point(nearestNeighbour.get(0).getX(), nearestNeighbour.get(0).getY()));

                                        tmpPoint.setX(cellExtent[1] - (this.cellSizeVMI));
                                        tmpPoint.setY(cellExtent[2] + (this.cellSizeVMI));
                                        nearestNeighbour = (List<KdTree.XYZPoint>) tmpTree.nearestNeighbourSearch(1, tmpPoint);
                                        if(!usedTreeIds.contains(nearestNeighbour.get(0).getIndex())) {
                                            output_.get(standid).get(holeId).add(new ConcaveHull.Point(nearestNeighbour.get(0).getX(), nearestNeighbour.get(0).getY()));
                                            usedTreeIds.add(nearestNeighbour.get(0).getIndex());
                                        }
                                        //output_.get(standid).get(holeId).add(new ConcaveHull.Point(nearestNeighbour.get(0).getX(), nearestNeighbour.get(0).getY()));

                                        tmpPoint.setX(cellExtent[1] - (this.cellSizeVMI));
                                        tmpPoint.setY(cellExtent[3] - (this.cellSizeVMI));
                                        nearestNeighbour = (List<KdTree.XYZPoint>) tmpTree.nearestNeighbourSearch(1, tmpPoint);
                                        if(!usedTreeIds.contains(nearestNeighbour.get(0).getIndex())) {
                                            output_.get(standid).get(holeId).add(new ConcaveHull.Point(nearestNeighbour.get(0).getX(), nearestNeighbour.get(0).getY()));
                                            usedTreeIds.add(nearestNeighbour.get(0).getIndex());
                                        }
                                        //output_.get(standid).get(holeId).add(new ConcaveHull.Point(nearestNeighbour.get(0).getX(), nearestNeighbour.get(0).getY()));

                                        tmpPoint.setX(cellExtent[0] + (this.cellSizeVMI));
                                        tmpPoint.setY(cellExtent[3] - (this.cellSizeVMI));
                                        nearestNeighbour = (List<KdTree.XYZPoint>) tmpTree.nearestNeighbourSearch(1, tmpPoint);
                                        if(!usedTreeIds.contains(nearestNeighbour.get(0).getIndex())) {
                                            output_.get(standid).get(holeId).add(new ConcaveHull.Point(nearestNeighbour.get(0).getX(), nearestNeighbour.get(0).getY()));
                                            usedTreeIds.add(nearestNeighbour.get(0).getIndex());
                                        }
                                        //output_.get(standid).get(holeId).add(new ConcaveHull.Point(nearestNeighbour.get(0).getX(), nearestNeighbour.get(0).getY()));

                                    }

                                }
                            }

                            holeIds.add(-id);
                        }
                    }
                }
            }


            if(standid == 487){
                System.out.println("HoleIds: " + holeIds.size());
                System.out.println("HoleIds: " + holeIds);
                System.out.println("HoleIds: " + output_.get(standid).size());
                System.out.println("HoleIds: " + output_.get(standid));

                try {
                    writer.close();
                    writer2.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                }

            }
        }

        System.out.println("Number of holes: " + output_.size());
        //System.exit(1);
        return output_;
    }

    public int treeInPolygons(double[] tree, HashMap<Integer, double[][]> polygons) {

        for (Map.Entry<Integer, double[][]> entry : polygons.entrySet()) {

            if(pointInPolygon(entry.getValue(), tree)) {
                return entry.getKey();
            }

        }

        return -1;
    }

    public OffsetDateTime parseDate(String dateString) throws DateTimeParseException {
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSXXX"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return OffsetDateTime.parse(dateString, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        writeLineToLogfile("Unable to parse the given date: " + dateString);
        throw new DateTimeParseException("Unable to parse the given date", dateString, 0);

    }

    class FileBatch{
        File origin;
        String originName;
        HashSet<File> files = new HashSet<>();

        public FileBatch(){

        }

        public void setOrigin(File origin){
            this.origin = origin;
            this.originName = origin.getName();
        }

        public void addFile(File file){
            this.files.add(file);
        }

        public void addAll(ArrayList<File>files){
            this.files.addAll(files);
        }
    }

    public ArrayList<FileBatch> createProcessingBatches(HashMap<Integer, ArrayList<File>> files){

        HashMap<String, FileBatch> batches = new HashMap<>();

        for(int i : files.keySet()){
            for(File file : files.get(i)){

                if(batches.containsKey(file.getName())){
                    //batches.get(originName).addFile(file);
                    batches.get(file.getName()).addAll(files.get(i));
                }else{
                    FileBatch batch = new FileBatch();
                    batch.setOrigin(file);
                    batches.put(file.getName(), batch);
                    batches.get(file.getName()).addAll(files.get(i));
                }
            }
        }

        // print the batches
        for(String originName : batches.keySet()){
            System.out.println("Batch: " + originName);
            for(File file : batches.get(originName).files){
                System.out.println("\t" + file.getName());
            }
        }

        this.processingBatches = batches;
        return new ArrayList<>(batches.values());

    }

    public HashMap<Integer, ArrayList<File>> getOverlappingHPRFiles(ArrayList<File> hprFiles){

        HashMap<Integer, ArrayList<File>> overlappingFilesMap = new HashMap<>();

        for(int i_ = 0; i_ < hprFiles.size(); i_++){

            this.xml_file = hprFiles.get(i_);

            SAXBuilder builder = new SAXBuilder();

            System.out.println("Parsing XML file: " + this.xml_file.getAbsolutePath());
            Document xml = null;
            try {
                xml = builder.build(this.xml_file);
            } catch (JDOMException e) {
                //e.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }


            Element root = null;

            try {
                root = xml.getRootElement();
            }catch (Exception e){
                System.out.println("Error parsing XML file: " + this.xml_file.getAbsolutePath());
                continue;
            }

            File ofile =new File("/home/koomikko/Documents/customer_work/metsahallitus/parsed.hpr");

            //System.exit(1);
            Namespace ns = root.getNamespace();

            System.out.println("Root element of XML document is : " + root.getName());
            System.out.println("Number of books in this XML : " + root.getChildren().size());


            List<Element> lista =  root.getChildren();

            List<Element> machines = root.getChildren("Machine", ns);
            Element dates = root.getChild("HarvestedProductionHeader", ns);

            String date = dates.getChild("CreationDate", ns).getValue();
            System.out.println(date);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            //OffsetDateTime offsetDateTime = OffsetDateTime.parse(date, formatter);
            OffsetDateTime offsetDateTime = parseDate(date);

            //OffsetDateTime offsetDateTime_lidar = OffsetDateTime.parse(LIDAR_ACQUISITION_DATE, formatter);
            OffsetDateTime offsetDateTime_lidar = parseDate(LIDAR_ACQUISITION_DATE);


            if (offsetDateTime.isBefore(offsetDateTime_lidar)) {
                System.out.println("LIDAR IS AFTER THE HARVESTING.");
                writeLineToLogfile("LIDAR IS AFTER THE HARVESTING ; " + this.xml_file.getName());
                continue;
            }else{
                System.out.println("LIDAR IS BEFORE THE HARVESTING.");
            }

            SpatialReference src = new SpatialReference();
            SpatialReference dst = new SpatialReference();

            src.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
            dst.ImportFromProj4("+proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs");

            CoordinateTransformation ct = new CoordinateTransformation(src, dst);

            CRSFactory crsFactory = new CRSFactory();

            RegistryManager registryManager = crsFactory.getRegistryManager();
            registryManager.addRegistry(new EPSGRegistry());
            CoordinateReferenceSystem to = null;
            CoordinateReferenceSystem from = null;
            Set<CoordinateOperation> operations;
            CoordinateOperation op = null;

            try {
                to = crsFactory.getCRS("EPSG:3067");
                from = crsFactory.getCRS("EPSG:4326");
                operations = CoordinateOperationFactory
                        .createCoordinateOperations((GeodeticCRS) from, (GeodeticCRS) to);

                Iterator iter = operations.iterator();
                op = (CoordinateOperation) iter.next();
            }catch (Exception e){
                e.printStackTrace();
            }

            if(false)
                try {
                    ofile.createNewFile();
                    //BufferedReader br = new BufferedReader(new FileReader(in));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));

                    ArrayList<Tree> trees = new ArrayList<Tree>();
                    printElement(root, bw);

                    bw.close();
                }catch (Exception e){
                    e.printStackTrace();
                }

            //System.exit(1);
            double[] transformed = null;

            //System.exit(1);
            ArrayList<Tree> trees = new ArrayList<Tree>();
            List<Element> stems = null;
            List<Element> productKeys = null;
            List<Element> species = null;
            List<Element> tmp = null;

            stemChecker sc = new stemChecker();

            String pine = "pine";


            ArrayList<double[]> unknown = new ArrayList<double[]>();
            ArrayList<double[]> knownTukki = new ArrayList<double[]>();
            ArrayList<double[]> knownKuitu = new ArrayList<double[]>();
            ArrayList<double[]> stemCoordinates = new ArrayList<double[]>();
            HashSet<Integer> stemsOutsideStands = new HashSet<>();
            HashMap<Integer, Tree> allTrees = new HashMap<Integer, Tree>();
            HashSet<Integer> uniqueStands = new HashSet<>();


            for(int i = 0; i < machines.size(); i++){

                species = machines.get(i).getChildren("SpeciesGroupDefinition", ns);

                for(int s = 0; s < species.size(); s++){
                    //System.out.println(species.get(s).getChild("SpeciesGroupKey", ns).getValue());
                    tmp = species.get(s).getChildren("StemTypeDefinition", ns);


                    for(int o = 0; o < tmp.size(); o++){

                        String name = (tmp.get(o).getChild("StemTypeName", ns).getValue());


                        if(name.toLowerCase().contains("mänty")){
                            sc.addPine(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                            sc.addPine(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }else if(name.toLowerCase().contains("koivu")){
                            sc.addBirch(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                            sc.addBirch(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }else if(name.toLowerCase().contains("kuusi")){
                            sc.addSpruce(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                            sc.addSpruce(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }else if(name.toLowerCase().contains("puulaji")){
                            sc.addOther(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                            sc.addOther(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }else{

                        }

                        if(name.toLowerCase().contains("kuitu")){
                            sc.addKuitu(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }
                        if(name.toLowerCase().contains("tukki")){
                            sc.addTukki(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }
                        if(name.toLowerCase().contains("runkolaji")){
                            sc.addMuuPuutavaraLaji(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }

                    }

                }

                productKeys = machines.get(i).getChildren("ProductDefinition", ns);
                //System.out.println(productKeys.size());

                HashMap<Integer, Integer> productKeyToStemTypeCode = new HashMap<Integer, Integer>();



                for(int s = 0; s < productKeys.size(); s++){

                    //System.out.println(productKeys.get(s).getChild("ProductKey", ns).getValue());
                    productKeyToStemTypeCode.put(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()), -1);
                    List<Element> lapset_1 = productKeys.get(s).getChildren();

                    for(int o = 0; o < lapset_1.size(); o++){

                        if(lapset_1.get(o).getName().equals("ClassifiedProductDefinition")){

                            List<Element> lapset = productKeys.get(s).getChild("ClassifiedProductDefinition", ns).getChildren();

                            for(int o_ = 0; o_ < lapset.size(); o_++){

                                if(lapset.get(o_).getName().equals("StemTypeCode")){
                                    productKeyToStemTypeCode.put(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()), Integer.parseInt(lapset.get(o_).getValue()));
                                }

                                if(lapset.get(o_).getName().equals("ProductGroupName")){
                                    String puutavaralaji = lapset.get(o_).getValue();
                                    //System.out.println(puutavaralaji);
                                    //System.out.println(productKeys.get(s).getChild("ClassifiedProductDefinition", ns).getChild("ProductGroupName", ns).getValue());

                                    if(puutavaralaji.toLowerCase().contains("kuitu")){
                                        sc.addKuitu(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                    }
                                    if(puutavaralaji.toLowerCase().contains("pikkutukki")){
                                        sc.addPikkutukki(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                    }else if(puutavaralaji.toLowerCase().contains("tukki")){
                                        sc.addTukki(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                    }
                                    if(puutavaralaji.toLowerCase().contains("energia")){
                                        sc.addEnergia(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                    }
                                }
                            }

                        }
                    }
                }

                stems = machines.get(i).getChildren("Stem", ns);

                System.out.println(stems.size());

                KdTree.XYZPoint point = new KdTree.XYZPoint(0, 0, 0);

                boolean firstInside = false;
                int countOutside = 0;
                double previousDistance = 0;

                ArrayList<Integer> currentOutside = new ArrayList<>();
                HashSet<Integer> toBeClassifiedOutsideStands = new HashSet<>();

                int currentStandId = -1;

                for(int s = 0; s < stems.size(); s++){

                    Tree tempTree = new Tree();
                    Element stem = stems.get(s);

                    tempTree.HPR_FILE_NAME = this.xml_file.getName();

                    List<Element> stem_coordinates = stem.getChildren("StemCoordinates", ns);

                    if(stem_coordinates.size() == 0){
                        //System.out.println("No coordinates for stem " + stem.getChild("StemNumber", ns).getValue());
                        continue;
                    }

                    //System.out.println(Arrays.toString(stem_coordinates.toArray()));
                    double latitude = Double.parseDouble(stem_coordinates.get(0).getChild("Latitude", ns).getValue());
                    double longitude = Double.parseDouble(stem_coordinates.get(0).getChild("Longitude", ns).getValue());
                    double altitude = Double.parseDouble(stem_coordinates.get(0).getChild("Altitude", ns).getValue());

                    //System.out.println("speciesgroupke: " + stem.getChild("SpeciesGroupKey", ns).getValue());
                    tempTree.setSpecies(sc.getSpecies(Integer.parseInt(stem.getChild("SpeciesGroupKey", ns).getValue())));
                    //System.out.println("Species: " + tempTree.getSpecies());
                    //System.out.println(latitude + " " + longitude);
                    try {
                        transformed = op.transform(new double[]{longitude, latitude});
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    //System.out.println(Arrays.toString(transformed));
                    tempTree.setX_coordinate_machine(transformed[0]);
                    tempTree.setY_coordinate_machine(transformed[1]);
                    tempTree.setPoint();

                    int insideStand = treeInPolygons(new double[]{transformed[0], transformed[1]}, standBounds);

                    tempTree.standId = insideStand;
                    tempTree.setGlobal_id(this.runningId++);

                    int stemNumber = Integer.parseInt(stem.getChild("StemNumber", ns).getValue());

                    tempTree.setId(stemNumber);


                    if(insideStand == -1){

                        point.setX(transformed[0]);
                        point.setY(transformed[1]);
                        point.setZ(0);
                        List<KdTree.XYZPoint> res = (List<KdTree.XYZPoint>)centroids.nearestNeighbourSearch2d(5, point);


                        int whichOne = -1;
                        double distance = Double.POSITIVE_INFINITY;

                        for(int i__ = 0; i__ < res.size(); i__++){

                            double distance_ = distanceToPolygon(new double[]{transformed[0], transformed[1]}, standBounds.get(res.get(i__).getIndex()));

                            if(distance_ < distance){
                                distance = distance_;
                                whichOne = res.get(i__).getIndex();
                            }
                        }

                        tempTree.standId = whichOne;

                        //System.out.println("distance: " + distance + " " + (res.get(0).getIndex()) + " " + (res.get(1).getIndex()));

                        tempTree.distanceToStandBorder = distance;
                        countOutside++;

                        previousDistance = distance;

                        currentOutside.add(tempTree.getId());

                        if(distance > maxDistanceFromStandBorder){

                            toBeClassifiedOutsideStands.addAll(currentOutside);


                            if(firstInside){
                                firstInside = false;
                            }

                        }

                    }else{

                        currentStandId = insideStand;
                        currentOutside.clear();
                        firstInside = true;
                        countOutside = 0;

                    }

                    if(!firstInside && insideStand == -1){
                        tempTree.trulyInStand = false;
                    }

                    if(tempTree.standId != -1){
                        uniqueStands.add(tempTree.standId);
                    }



                }

                for(int stand : uniqueStands){

                    if(!overlappingFilesMap.containsKey(stand)){
                        overlappingFilesMap.put(stand, new ArrayList<>());
                        overlappingFilesMap.get(stand).add(hprFiles.get(i_));
                    }
                    else{
                        overlappingFilesMap.get(stand).add(hprFiles.get(i_));
                    }
                }

                //for(int tree : toBeClassifiedOutsideStands){
                //    allTrees.get(tree).trulyInStand = false;
                //}

            }
            System.gc();
        }

        // print the map

        for(int stand : overlappingFilesMap.keySet()){

            String[] files = new String[overlappingFilesMap.get(stand).size()];
            for(int i = 0; i < overlappingFilesMap.get(stand).size(); i++){
                files[i] = overlappingFilesMap.get(stand).get(i).getName();
            }
            System.out.println("Stand " + stand + " overlaps with " + overlappingFilesMap.get(stand).size() + " files: " + Arrays.toString(files));
        }

        //System.exit(1);

        return overlappingFilesMap;

    }

    public static Geometry createPolygon(ArrayList<double[]> points) {
        if (points.size() < 3) {
            throw new IllegalArgumentException("Invalid number of points for a polygon");
        }

        Geometry polygon = new Geometry(ogr.wkbPolygon);
        Geometry ring = new Geometry(ogr.wkbLinearRing);

        for (int i = 0; i < points.size(); i++) {
            ring.AddPoint(points.get(i)[0], points.get(i)[1]);
        }

        ring.AddPoint(points.get(0)[0], points.get(0)[1]);

        polygon.AddGeometryDirectly(ring);

        return polygon;
    }

    public static Geometry createPolygon(double[][] points) {

        Geometry polygon = new Geometry(ogr.wkbPolygon);
        Geometry ring = new Geometry(ogr.wkbLinearRing);

        for (int i = 0; i < points.length; i++) {
            ring.AddPoint(points[i][0], points[i][1]);
        }

        ring.AddPoint(points[0][0], points[0][1]);

        polygon.AddGeometryDirectly(ring);

        return polygon;

    }


    public void parseBatches(){

        HashSet<String> alreadyProcessed = new HashSet<>();

        for(String s : this.processingBatches.keySet()){

            FileBatch batch = this.processingBatches.get(s);

            try {
                parse(batch, alreadyProcessed);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.gc();
            System.gc();
            System.gc();
            System.gc();
            System.gc();
        }


    }
    public void parse(FileBatch batch, HashSet<String> alreadyProcessed) throws IOException{

        curveFitting fitter = new curveFitting(aR);

        for(File f : batch.files){

            if(alreadyProcessed.contains(f.getName())){

                System.out.println("Already processed file: " + f.getName());
                return;
            }


        }


        ArrayList<Tree> trees = new ArrayList<Tree>();
        HashMap<Integer, Tree> allTrees = new HashMap<Integer, Tree>();
        HashSet<Integer> uniqueStands = new HashSet<>();

        int totalNumberOfStems = 0;
        ArrayList<double[]> unknown = new ArrayList<double[]>();
        ArrayList<double[]> knownTukki = new ArrayList<double[]>();
        ArrayList<double[]> knownKuitu = new ArrayList<double[]>();
        File ofile = new File("/home/koomikko/Documents/customer_work/metsahallitus/parsed.hpr");

        int stemNumberHere = 0;

        for(File f : batch.files) {

            alreadyProcessed.add(f.getName());
            SAXBuilder builder = new SAXBuilder();

            this.setXMLfile(f);

            System.out.println("Parsing XML file: " + this.xml_file.getAbsolutePath());

            Document xml = null;

            try {
                xml = builder.build(this.xml_file);
            } catch (JDOMException e) {
                //e.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }


            Element root = null;

            try {
                root = xml.getRootElement();
            } catch (Exception e) {
                System.out.println("Error parsing XML file: " + this.xml_file.getAbsolutePath());
                continue;
            }


            //System.exit(1);
            Namespace ns = root.getNamespace();

            System.out.println("Root element of XML document is : " + root.getName());
            System.out.println("Number of books in this XML : " + root.getChildren().size());

            List<Element> lista = root.getChildren();

            List<Element> machines = root.getChildren("Machine", ns);
            Element dates = root.getChild("HarvestedProductionHeader", ns);

            String date = dates.getChild("CreationDate", ns).getValue();
            System.out.println(date);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            //OffsetDateTime offsetDateTime = OffsetDateTime.parse(date, formatter);
            OffsetDateTime offsetDateTime = parseDate(date);

            //OffsetDateTime offsetDateTime_lidar = OffsetDateTime.parse(LIDAR_ACQUISITION_DATE, formatter);
            OffsetDateTime offsetDateTime_lidar = parseDate(LIDAR_ACQUISITION_DATE);


            if (offsetDateTime.isBefore(offsetDateTime_lidar)) {
                System.out.println("LIDAR IS AFTER THE HARVESTING.");
                writeLineToLogfile("LIDAR IS AFTER THE HARVESTING ; " + this.xml_file.getName());
                continue;
            } else {
                System.out.println("LIDAR IS BEFORE THE HARVESTING.");
            }

            SpatialReference src = new SpatialReference();
            SpatialReference dst = new SpatialReference();

            src.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
            dst.ImportFromProj4("+proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs");

            CoordinateTransformation ct = new CoordinateTransformation(src, dst);

            CRSFactory crsFactory = new CRSFactory();

            RegistryManager registryManager = crsFactory.getRegistryManager();
            registryManager.addRegistry(new EPSGRegistry());
            CoordinateReferenceSystem to = null;
            CoordinateReferenceSystem from = null;
            Set<CoordinateOperation> operations;
            CoordinateOperation op = null;

            try {
                to = crsFactory.getCRS("EPSG:3067");
                from = crsFactory.getCRS("EPSG:4326");
                operations = CoordinateOperationFactory
                        .createCoordinateOperations((GeodeticCRS) from, (GeodeticCRS) to);

                Iterator iter = operations.iterator();
                op = (CoordinateOperation) iter.next();
            } catch (Exception e) {
                e.printStackTrace();
            }

            double[] transformed = null;

            List<Element> stems = null;
            List<Element> productKeys = null;
            List<Element> species = null;
            List<Element> tmp = null;

            stemChecker sc = new stemChecker();

            String pine = "pine";



            ArrayList<double[]> stemCoordinates = new ArrayList<double[]>();
            HashSet<Integer> stemsOutsideStands = new HashSet<>();


            for (int i = 0; i < machines.size(); i++) {

                species = machines.get(i).getChildren("SpeciesGroupDefinition", ns);

                for (int s = 0; s < species.size(); s++) {
                    //System.out.println(species.get(s).getChild("SpeciesGroupKey", ns).getValue());
                    tmp = species.get(s).getChildren("StemTypeDefinition", ns);


                    for (int o = 0; o < tmp.size(); o++) {

                        String name = (tmp.get(o).getChild("StemTypeName", ns).getValue());


                        if (name.toLowerCase().contains("mänty")) {
                            sc.addPine(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                            sc.addPine(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        } else if (name.toLowerCase().contains("koivu")) {
                            sc.addBirch(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                            sc.addBirch(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        } else if (name.toLowerCase().contains("kuusi")) {
                            sc.addSpruce(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                            sc.addSpruce(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        } else if (name.toLowerCase().contains("puulaji")) {
                            sc.addOther(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                            sc.addOther(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        } else {

                        }

                        if (name.toLowerCase().contains("kuitu")) {
                            sc.addKuitu(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }
                        if (name.toLowerCase().contains("tukki")) {
                            sc.addTukki(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }
                        if (name.toLowerCase().contains("runkolaji")) {
                            sc.addMuuPuutavaraLaji(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                            //break;
                        }

                    }

                }

                productKeys = machines.get(i).getChildren("ProductDefinition", ns);
                //System.out.println(productKeys.size());

                HashMap<Integer, Integer> productKeyToStemTypeCode = new HashMap<Integer, Integer>();


                for (int s = 0; s < productKeys.size(); s++) {

                    //System.out.println(productKeys.get(s).getChild("ProductKey", ns).getValue());
                    productKeyToStemTypeCode.put(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()), -1);
                    List<Element> lapset_1 = productKeys.get(s).getChildren();

                    for (int o = 0; o < lapset_1.size(); o++) {

                        if (lapset_1.get(o).getName().equals("ClassifiedProductDefinition")) {

                            List<Element> lapset = productKeys.get(s).getChild("ClassifiedProductDefinition", ns).getChildren();

                            for (int o_ = 0; o_ < lapset.size(); o_++) {

                                if (lapset.get(o_).getName().equals("StemTypeCode")) {
                                    productKeyToStemTypeCode.put(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()), Integer.parseInt(lapset.get(o_).getValue()));
                                }

                                if (lapset.get(o_).getName().equals("ProductGroupName")) {
                                    String puutavaralaji = lapset.get(o_).getValue();
                                    //System.out.println(puutavaralaji);
                                    //System.out.println(productKeys.get(s).getChild("ClassifiedProductDefinition", ns).getChild("ProductGroupName", ns).getValue());

                                    if (puutavaralaji.toLowerCase().contains("kuitu")) {
                                        sc.addKuitu(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                    }
                                    if (puutavaralaji.toLowerCase().contains("pikkutukki")) {
                                        sc.addPikkutukki(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                    } else if (puutavaralaji.toLowerCase().contains("tukki")) {
                                        sc.addTukki(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                    }
                                    if (puutavaralaji.toLowerCase().contains("energia")) {
                                        sc.addEnergia(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                    }
                                }
                            }

                        }
                    }
                }

                stems = machines.get(i).getChildren("Stem", ns);

                System.out.println(stems.size());

                KdTree.XYZPoint point = new KdTree.XYZPoint(0, 0, 0);

                boolean firstInside = false;
                int countOutside = 0;
                double previousDistance = 0;

                ArrayList<Integer> currentOutside = new ArrayList<>();
                HashSet<Integer> toBeClassifiedOutsideStands = new HashSet<>();

                int currentStandId = -1;

                for (int s = 0; s < stems.size(); s++) {

                    Tree tempTree = new Tree();
                    Element stem = stems.get(s);

                    tempTree.HPR_FILE_NAME = this.xml_file.getName();

                    List<Element> stem_coordinates = stem.getChildren("StemCoordinates", ns);

                    if (stem_coordinates.size() == 0) {
                        //System.out.println("No coordinates for stem " + stem.getChild("StemNumber", ns).getValue());
                        continue;
                    }

                    //System.out.println(Arrays.toString(stem_coordinates.toArray()));
                    double latitude = Double.parseDouble(stem_coordinates.get(0).getChild("Latitude", ns).getValue());
                    double longitude = Double.parseDouble(stem_coordinates.get(0).getChild("Longitude", ns).getValue());
                    double altitude = Double.parseDouble(stem_coordinates.get(0).getChild("Altitude", ns).getValue());

                    //System.out.println("speciesgroupke: " + stem.getChild("SpeciesGroupKey", ns).getValue());
                    tempTree.setSpecies(sc.getSpecies(Integer.parseInt(stem.getChild("SpeciesGroupKey", ns).getValue())));
                    //System.out.println("Species: " + tempTree.getSpecies());
                    //System.out.println(latitude + " " + longitude);
                    try {
                        transformed = op.transform(new double[]{longitude, latitude});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //System.out.println(Arrays.toString(transformed));
                    tempTree.setX_coordinate_machine(transformed[0]);
                    tempTree.setY_coordinate_machine(transformed[1]);
                    tempTree.setPoint();

                    int insideStand = treeInPolygons(new double[]{transformed[0], transformed[1]}, standBounds);

                    tempTree.standId = insideStand;
                    tempTree.setGlobal_id(this.runningId++);

                    //int stemNumber = Integer.parseInt(stem.getChild("StemNumber", ns).getValue());
                    int stemNumber = stemNumberHere++;

                    tempTree.setId(stemNumber);

                    if (insideStand == -1) {

                        point.setX(transformed[0]);
                        point.setY(transformed[1]);
                        point.setZ(0);
                        List<KdTree.XYZPoint> res = (List<KdTree.XYZPoint>) centroids.nearestNeighbourSearch2d(5, point);


                        int whichOne = -1;
                        double distance = Double.POSITIVE_INFINITY;

                        for (int i_ = 0; i_ < res.size(); i_++) {

                            double distance_ = distanceToPolygon(new double[]{transformed[0], transformed[1]}, standBounds.get(res.get(i_).getIndex()));

                            if (distance_ < distance) {
                                distance = distance_;
                                whichOne = res.get(i_).getIndex();
                            }
                        }

                        tempTree.standId = whichOne;

                        //System.out.println("distance: " + distance + " " + (res.get(0).getIndex()) + " " + (res.get(1).getIndex()));

                        tempTree.distanceToStandBorder = distance;
                        countOutside++;

                        previousDistance = distance;

                        currentOutside.add(tempTree.getId());

                        if (distance > maxDistanceFromStandBorder) {

                            toBeClassifiedOutsideStands.addAll(currentOutside);


                            if (firstInside) {
                                firstInside = false;
                            }

                        }

                    } else {

                        currentStandId = insideStand;
                        currentOutside.clear();
                        firstInside = true;
                        countOutside = 0;

                    }

                    if (!firstInside && insideStand == -1) {
                        tempTree.trulyInStand = false;
                    }

                    if (tempTree.standId != -1) {
                        uniqueStands.add(tempTree.standId);
                    }

                    //System.out.println("stemnumber: " + stemNumber);
                    List<Element> stemInfo = null;
                    String name = "";
                    try {
                        stemInfo = stem.getChild("SingleTreeProcessedStem", ns).getChildren("Log", ns);
                        name = "SingleTreeProcessedStem";
                    } catch (Exception e) {
                        stemInfo = stem.getChild("SingleTreeFelledStem", ns).getChildren("Log", ns);
                        name = "SingleTreeFelledStem";
                    }
                    //List<Element> stemInfo__ = stem.getChild("SingleTreeProcessedStem", ns).getChildren();

                    double dbh = Double.parseDouble(stem.getChild(name, ns).getChild("DBH", ns).getValue());
                    tempTree.setDiameter((float) dbh);
                    //System.out.println("logs: " + stemInfo.size());

                    ArrayList<Double> stemCurveX = new ArrayList<Double>();
                    ArrayList<Double> stemCurveY = new ArrayList<Double>();

                    //stemCurveX.add(1.3);
                    //stemCurveY.add(dbh / 100.0);

                    double height = diameterToHeight(dbh, tempTree.getSpecies());
                    //System.out.println("height: " + height);
                    tempTree.setHeight((float) height);

                    double currentHeight = 0.5;

                    for (int i_ = 0; i_ < stemInfo.size(); i_++) {

                        double volume = Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue());

                        //System.out.println("volume: " + volume);
                        //System.out.println(stemInfo.get(i_).getChild("ProductKey", ns).getValue());
                        //System.out.println(stemInfo.get(i_));
                        //tempTree.setSpecies(sc.getSpecies(productKeyToStemTypeCode.get(Integer.parseInt(stemInfo.get(i_).getChild("ProductKey", ns).getValue()))));


                        int puutavaralaji = 0;


                        if (productKeyToStemTypeCode.get(Integer.parseInt(stemInfo.get(i_).getChild("ProductKey", ns).getValue())) != -1) {
                            puutavaralaji = sc.getPuutavaraLaji(productKeyToStemTypeCode.get(Integer.parseInt(stemInfo.get(i_).getChild("ProductKey", ns).getValue())));

                        } else
                            // THIS MEANS WE HAVE GARBO DATA
                            puutavaralaji = -1;
                        //int puutavaraLaji = sc.getPuutavaraLaji(productKeyToStemTypeCode.get(Integer.parseInt(stemInfo.get(i_).getChild("ProductKey", ns).getValue())));

                   /*
                    if(kuitu.contains(value)){
                        return 1;
                    }
                    if(tukki.contains(value)){
                        return 2;
                    }
                    if(energia.contains(value)){
                        return 3;
                    }
                    if(pikkutukki.contains(value)){
                        return 4;
                    }
                    */

                        double loglength = -1;
                        double logdiameterMid = -1;
                        double logdiameterEnd = -1;


                        if (puutavaralaji == 1) {
                            tempTree.volume_kuitu += volume;

                            Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);
                            int size = stemInfo_.getChildren("LogDiameter", ns).size();

                            if (size >= 3) {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                            } else {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            }

                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterEnd / 10.0);
                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterMid / 10.0);

                            knownKuitu.add(new double[]{Double.parseDouble(stemInfo.get(i_).getChildren("LogKey", ns).get(0).getValue()),
                                    stemInfo.size(),
                                    Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue()),
                                    loglength, logdiameterMid, logdiameterEnd
                            });


                        }
                        if (puutavaralaji == 2) {
                            tempTree.volume_tukki += volume;

                            Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                            int size = stemInfo_.getChildren("LogDiameter", ns).size();

                            if (size >= 3) {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                            } else {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            }
                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterEnd / 10.0);
                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterMid / 10.0);

                            knownTukki.add(new double[]{Double.parseDouble(stemInfo.get(i_).getChildren("LogKey", ns).get(0).getValue()),
                                    stemInfo.size(),
                                    Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue()),
                                    loglength, logdiameterMid, logdiameterEnd
                            });

                            //System.out.println(Arrays.toString(unknown.get(unknown.size()-1)));

                        }
                        if (puutavaralaji == 3) {
                            tempTree.volume_energia += volume;

                            Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                            int size = stemInfo_.getChildren("LogDiameter", ns).size();

                            if (size >= 3) {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                            } else {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            }
                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterEnd / 10.0);
                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterMid / 10.0);

                        }
                        if (puutavaralaji == 4) {
                            tempTree.volume_pikkutukki += volume;

                            Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                            int size = stemInfo_.getChildren("LogDiameter", ns).size();

                            if (size >= 3) {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                            } else {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            }
                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterEnd / 10.0);
                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterMid / 10.0);

                        }
                        if (puutavaralaji == -1) {

                            tempTree.volume_unknown += volume;

                            Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                            int size = stemInfo_.getChildren("LogDiameter", ns).size();

                            if (size >= 3) {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                            } else {
                                loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                                logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                                logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            }
                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterEnd / 10.0);
                            currentHeight += loglength / 2.0 / 100.0;
                            stemCurveX.add(currentHeight);
                            stemCurveY.add(logdiameterMid / 10.0);

                            unknown.add(new double[]{Double.parseDouble(stemInfo.get(i_).getChildren("LogKey", ns).get(0).getValue()),
                                    stemInfo.size(),
                                    Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue()),
                                    loglength, logdiameterMid, logdiameterEnd
                            });

                            //System.out.println(Arrays.toString(unknown.get(unknown.size()-1)));
                        }

                    }


                    tempTree.setStemCurveX(stemCurveX.stream().mapToDouble(Double::doubleValue).toArray());
                    tempTree.setStemCurveY(stemCurveY.stream().mapToDouble(Double::doubleValue).toArray());

                    //System.out.println(Arrays.toString(tempTree.getStemCurveX()));
                    //System.out.println(Arrays.toString(tempTree.getStemCurveY()));
                    //tempTree.estimateHeight(fitter);
                    //System.out.println(tempTree.getHeight());
                    //System.out.println("---------");
                    trees.add(tempTree);


                    allTrees.put(tempTree.getId(), tempTree);

                    processedStands.add(tempTree.standId);


                    if (true)
                        continue;
/*
                Element stemInfo = stem.getChild("Extension", ns).getChild("HPRCMResults", ns).getChild("StemInfo", ns);

                float treeHeight = Float.parseFloat(stemInfo.getChild("TreeHeight",ns).getValue());

                tempTree.setHeight(treeHeight);

                tempTree.setBoomPosition(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("BoomAngle", ns).getValue()));
                tempTree.setBoomExtension(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("BoomExtension", ns).getValue()));
                tempTree.setMachineBearing(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("MachineBearing", ns).getValue()));
                tempTree.setDiameter(Float.parseFloat(stem.getChild("SingleTreeProcessedStem", ns).getChild("DBH", ns).getValue()));

                System.out.println(tempTree);
                System.exit(1);

 */
                }

                for (int tree : toBeClassifiedOutsideStands) {
                    allTrees.get(tree).trulyInStand = false;
                }

                totalNumberOfStems += stems.size();

            }
        }


        if(trees.size() != totalNumberOfStems){

            System.out.println("Trees and stems are not the same size. Something went wrong.");

            failedFiles.add(this.xml_file.getAbsolutePath());
            failedFilesProps.add(new int[]{trees.size(), totalNumberOfStems});

            if(trees.size() == 0)
                return;

            /*
            try {
                ofile.createNewFile();
                //BufferedReader br = new BufferedReader(new FileReader(in));
                BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));

                trees = new ArrayList<Tree>();
                printElement(root, bw);

                bw.close();
            }catch (Exception e){
                e.printStackTrace();
            }

            throw new toolException("Trees and stems are not the same size. Something went wrong.");

            */
        }

        HashMap<Integer, ArrayList<tools.ConcaveHull.Point>> standTreeLocations = new HashMap<>();
        HashMap<Integer, ArrayList<Point>> standTreeLocations_convex = new HashMap<>();
        HashMap<Integer, ArrayList<double[]>> standTreeLocations_ = new HashMap<>();
        HashMap<Integer, Integer> standPolygonIds = new HashMap<>();


        for(int tree : allTrees.keySet()){

            if(!allTrees.get(tree).trulyInStand)
                continue;

            if(standTreeLocations.containsKey(allTrees.get(tree).standId)) {
                standTreeLocations.get(allTrees.get(tree).standId).add(new tools.ConcaveHull.Point(allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()));
                standTreeLocations_.get(allTrees.get(tree).standId).add(new double[]{allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()});
                standTreeLocations_convex.get(allTrees.get(tree).standId).add(new Point(allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()));
            }else{
                ArrayList<tools.ConcaveHull.Point> temp = new ArrayList<>();
                ArrayList<double[]> temp_ = new ArrayList<>();
                ArrayList<Point> temp_convex = new ArrayList<>();
                temp_.add(new double[]{allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()});
                temp.add(new tools.ConcaveHull.Point(allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()));
                temp_convex.add(new Point(allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()));
                standTreeLocations.put(allTrees.get(tree).standId, temp);
                standTreeLocations_.put(allTrees.get(tree).standId, temp_);
                standTreeLocations_convex.put(allTrees.get(tree).standId, temp_convex);

            }
        }

        HashSet<Integer> removeThese = new HashSet<>();
        HashSet<Integer> removeTheseSets = new HashSet<>();
        Set<Integer> removeThese_ = new TreeSet<>(Comparator.reverseOrder());
        Set<Integer> removeThese__ = new TreeSet<>(Comparator.reverseOrder());

        for(int i : standTreeLocations.keySet()){

            if(standTreeLocations.get(i).size() < 3){

                System.out.println("REMOVE: " + i + " ntrees: " + standTreeLocations.get(i).size());
                removeThese.add(i);
                continue;

            }

            Geometry g = createPolygon(standTreeLocations_.get(i));

            double[][] polygon = standBounds.get(i);

            Geometry g2 = createPolygon(polygon);

            System.out.println("Area: " + g.GetArea()/10000);
            double treesPerHectare = standTreeLocations.get(i).size() / (g.GetArea()/10000);
            double treesPerHectare2 = standTreeLocations.get(i).size() / (g2.GetArea()/10000);
            System.out.println("Trees per hectare: " + treesPerHectare);
            System.out.println("Trees per hectare2: " + treesPerHectare2);
            //if(standTreeLocations.get(i).size() < 10) {
            if(standTreeLocations.get(i).size() < 10){ // treesPerHectare2 < 100 ||
                System.out.println("REMOVE: " + i + " ntrees: " + standTreeLocations.get(i).size());
                removeThese.add(i);
            }
        }



        for(int i : removeThese){
            standTreeLocations_.remove(i);
            standTreeLocations.remove(i);
            standTreeLocations_convex.remove(i);
        }

        for(int i = 0; i < trees.size(); i++){

            if(removeThese.contains(trees.get(i).standId)){
                removeThese_.add(i);
                removeThese__.add(trees.get(i).getId());

            }
        }

        for(int i : removeThese_){
            trees.remove(i);
        }

        for(int i : removeThese__){
            allTrees.remove(i);
        }

        for(Tree tree : trees){
            //if(tree.standId == 123){
            //    System.out.println("FOUND " + removeThese.contains(tree.standId));
            //    System.exit(1);
            //}
        }

        HashMap<Integer, ArrayList<ConcaveHull.Point>> hulls = new HashMap<>();
        HashMap<Integer, ArrayList<Point>> hulls_convex = new HashMap<>();

        QuickHull quickHull = new QuickHull();
        HashSet<Integer> removeThese2 = new HashSet<>();

        System.out.println("Calculating hulls " + standTreeLocations.size());

        for(int i : standTreeLocations.keySet()){

            ArrayList<tools.ConcaveHull.Point> currentTrees = standTreeLocations.get(i);
            ArrayList<ConcaveHull.Point> hull = calculateConcaveHull(currentTrees, concave_hull_k);

            //ArrayList<Point> convexHullPoints = quickHull.quickHull(standTreeLocations_convex.get(i));

            //hulls_convex.put(i, convexHullPoints);

            if(hull.size() < 3){

                removeThese2.add(i);

            }else
                hulls.put(i, hull);
           // System.out.println("Hull size: " + hull.size() + " " + convexHullPoints.size());

        }

        Set<Integer> removeThese_2 = new TreeSet<>(Comparator.reverseOrder());
        Set<Integer> removeThese__2 = new TreeSet<>(Comparator.reverseOrder());

        for(int i : removeThese2){
            standTreeLocations_.remove(i);
            standTreeLocations.remove(i);
            standTreeLocations_convex.remove(i);
        }

        for(int i = 0; i < trees.size(); i++){

            if(removeThese2.contains(trees.get(i).standId)){

                removeThese_2.add(i);
                removeThese__2.add(trees.get(i).getId());

            }
        }

        for(int i : removeThese_2){
            trees.remove(i);
        }

        for(int i : removeThese__2){
            allTrees.remove(i);
        }

        //this.createPlots(allTrees, hulls);

        //System.exit(1);

        System.out.println(standTreeLocations.size());

        String pathSeparator = System.getProperty("file.separator");

        System.out.println("pathSeparator: " + pathSeparator);
        //System.exit(1);

        //System.out.println("xml: " + Arrays.toString(xml_file.getName().split(("\\."))));
        File output_directory = new File(aR.odir + pathSeparator + xml_file.getName().split("\\.")[0]);


        System.out.println("output_directory: " + output_directory.getAbsolutePath());
        output_directory.mkdirs();

        HashMap<Integer, List<ConcaveHull.Point>> bufferedHulls = new HashMap<>();



        Random rand = new Random(12345);



        HashMap<Integer, HashMap<Integer, List<ConcaveHull.Point>>> holePoints = this.createHoleDetectionGrid(allTrees, hulls, rand, 1);

        HashMap<Integer, HashMap<Integer, List<ConcaveHull.Point>>> holeConcaveHulls = new HashMap<>();



        for(int i : holePoints.keySet()){
            holeConcaveHulls.put(i, new HashMap<>());
            for(int j : holePoints.get(i).keySet()){
                //holeConcaveHulls.get(i).put(j, new ArrayList<>());
                System.out.println("number of points in hole: " + holePoints.get(i).get(j).size());
                for(int k = 0; k < holePoints.get(i).get(j).size(); k++){
                    //System.out.println("hole point: " + holePoints.get(i).get(j).get(k).getX() + " " + holePoints.get(i).get(j).get(k).getY());
                }

                List<ConcaveHull.Point> hull_ = calculateConcaveHull(holePoints.get(i).get(j), 10);
                //hull_ = LineModification.smoothPolygon(hull_);
                holeConcaveHulls.get(i).put(j, hull_);
                //ArrayList<ConcaveHull.Point> convexHullPoints = quickHull.quickHull(holePoints.get(i).get(j));
                //holeConcaveHulls.get(i).add(convexHullPoints);
                //print hull points

                //for(int k = 0; k < holeConcaveHulls.get(i).get(j).size(); k++){
                //    System.out.println("hole hull point: " + holeConcaveHulls.get(i).get(j).get(k).getX() + " " + holeConcaveHulls.get(i).get(j).get(k).getY());
                //}

            }

        }
        System.out.println("number of holes before pruning: " + holeConcaveHulls.size());

        Iterator<Integer> standIterator = holeConcaveHulls.keySet().iterator();
        while (standIterator.hasNext()) {
            int i = standIterator.next();
            System.out.println("number of holes in stand: " + i + " " + holeConcaveHulls.get(i).size());

            Iterator<Integer> holeIterator = holeConcaveHulls.get(i).keySet().iterator();
            while (holeIterator.hasNext()) {
                int j = holeIterator.next();
                System.out.println("number of points in hole: " + holeConcaveHulls.get(i).get(j).size());
                boolean isValid = LineModification.checkPolygonValidity(holeConcaveHulls.get(i).get(j));
                if (!isValid || holeConcaveHulls.get(i).get(j).size() < 3) {
                    System.out.println("HOLE IS NOT VALID");
                    // remove the hole
                    holeIterator.remove();
                }
            }

            // If all holes are removed, remove the stand
            if (holeConcaveHulls.get(i).isEmpty()) {
                standIterator.remove();
            }
        }

        System.out.println("number of holes after pruning: " + holeConcaveHulls.size());

        //System.exit(1);

        HashMap<Integer, Geometry> standGeometries = new HashMap<>();

        for(int i : standTreeLocations.keySet()){

            File tmpFile = new File(output_directory.getAbsolutePath() + pathSeparator + "stand_" + i + "_boundary.shp");
            System.out.println("HULL SIZE: " + hulls.get(i).size());
            //Geometry tmpGeom = null;
            ArrayList<ConcaveHull.Point> buf_ = new ArrayList<>();

            Geometry tmpGeom = exportShapefile_(tmpFile, hulls.get(i), i, holeConcaveHulls, buf_);

            if(buf_ != null) {
                bufferedHulls.put(i, buf_);
                standGeometries.put(i, tmpGeom);
                System.out.println("not null " + buf_.size());
            }else{
                System.out.println("NULL");
                holeConcaveHulls.remove(i);
            }
        }

        //System.exit(1);

        System.out.println("number of stands with bufferedHoles: " + holeConcaveHulls.size());

        for(int i : holeConcaveHulls.keySet()){
            System.out.println("stand " + i + " size: " + holeConcaveHulls.get(i).size());
            for(int j : holeConcaveHulls.get(i).keySet()){
                System.out.println("bufferedHull size: " + holeConcaveHulls.get(i).get(j).size());
                System.out.println(standGeometries.containsKey(i));
                System.out.println(standGeometries.get(i).GetArea());

            }
        }

        //System.exit(1);

        //aR.pfac.closeThread(thread_n);



        dynamicStatistics stats_x = new dynamicStatistics();
        dynamicStatistics stats_y = new dynamicStatistics();

        ArrayList<Tree> trees2 = new ArrayList<Tree>();

        ArrayList<int[]> trackTreeIds = new ArrayList<int[]>();

        double maxDistanceFromAchrorTree = 5.0;

        Tree anchorTree = new Tree();
        Tree previousAnchorTree = new Tree();

        int trees_size = trees2.size() - 1;

        double previousMeanX = 0;
        double previousMeanY = 0;

        double trackChangeDistance = maxDistanceFromAchrorTree * 8;

        int trackID = 0;
        int counter = 0;

        boolean smartStuf = false;

        double[] p1_ = new double[]{5,1};
        double[] p2_ = new double[]{5,5};
        double[] p_ = new double[]{3,3};

        System.out.println(canDrawPerpendicular(p_[0],p_[1], p1_, p2_));

        int countAnchors = 0;

        boolean firstBatchCorrected = false;

        //System.exit(1);
        for(int i = 0; i < trees.size(); i++){

            previousMeanY = stats_y.getMean();
            previousMeanX = stats_x.getMean();

            // We start a new averaging
            if(trees_size != trees2.size()){
                trees_size = trees2.size();

                stats_x.reset();
                stats_y.reset();
                stats_x.add(trees.get(i).getX_coordinate_machine());
                stats_y.add(trees.get(i).getY_coordinate_machine());

            }else {

                stats_x.add(trees.get(i).getX_coordinate_machine());
                stats_y.add(trees.get(i).getY_coordinate_machine());
            }

            double distance = euclideanDistance(stats_x.getMean(), stats_y.getMean(), anchorTree.getX_coordinate_machine(), anchorTree.getY_coordinate_machine());
            double distance2 = euclideanDistance(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), anchorTree.getX_coordinate_machine(), anchorTree.getY_coordinate_machine());

            if(distance > maxDistanceFromAchrorTree){

                Tree tmpTree = new Tree();

                boolean isBad = false;

                if(distance2 > trackChangeDistance) {

                    tmpTree.setX_coordinate_machine(previousMeanX);
                    tmpTree.setY_coordinate_machine(previousMeanY);

                }else{

                    if(smartStuf) {
                        if (trees2.size() > 2) {

                            double[] p1 = new double[]{trees2.get(trees2.size() - 2).getX_coordinate_machine(), trees2.get(trees2.size() - 2).getY_coordinate_machine()};
                            double[] p2 = new double[]{trees2.get(trees2.size() - 1).getX_coordinate_machine(), trees2.get(trees2.size() - 1).getY_coordinate_machine()};
                            double[] p = new double[]{stats_x.getMean(), stats_y.getMean()};

                            //while (isPointBetween(p1, p2, p) && stats_x.getNumValues() > 2) {
                                System.out.println("REMOVE");
                                stats_x.removeLast();
                                stats_y.removeLast();
                                p = new double[]{stats_x.getMean(), stats_y.getMean()};
                                p1 = new double[]{trees2.get(trees2.size() - 2).getX_coordinate_machine(), trees2.get(trees2.size() - 2).getY_coordinate_machine()};
                                p2 = new double[]{trees2.get(trees2.size() - 1).getX_coordinate_machine(), trees2.get(trees2.size() - 1).getY_coordinate_machine()};
                            //}

                            //if (isPointBetween(p1, p2, p)) {
                                isBad = true;
                            //}


                            tmpTree.setX_coordinate_machine(previousMeanX);
                            tmpTree.setY_coordinate_machine(previousMeanY);
                        } else {
                            tmpTree.setX_coordinate_machine(stats_x.getMean());
                            tmpTree.setY_coordinate_machine(stats_y.getMean());
                        }
                    }else{
                        tmpTree.setX_coordinate_machine(stats_x.getMean());
                        tmpTree.setY_coordinate_machine(stats_y.getMean());
                    }
                }

                //tmpTree.setX_coordinate_machine(previousMeanX);
                //tmpTree.setY_coordinate_machine(previousMeanY);

                if(!isBad) {
                    countAnchors++;
                    tmpTree.setId(trackID);
                    tmpTree.setKey(counter++);
                    trees2.add(tmpTree);

                    previousAnchorTree = anchorTree.clone();
                    anchorTree = trees.get(i);

                    if (distance2 > trackChangeDistance) {
                        trackID++;
                    }
                }
            }
            else {

            }


            if(countAnchors >= 2){

                double bearing = angleFromOnePointToAnother(previousAnchorTree.getX_coordinate_machine(), previousAnchorTree.getY_coordinate_machine(), anchorTree.getX_coordinate_machine(), anchorTree.getY_coordinate_machine());


                if(!firstBatchCorrected) {

                    firstBatchCorrected = true;

                    for (int i_ = 0; i_ <= i; i_++) {

                        trees.get(i_).setMachineBearing(bearing);

                    }

                }else{
                    trees.get(i).setMachineBearing(bearing);
                }
            }else{

            }
        }

        for(int i : standTreeLocations.keySet()){
            //System.out.println("standid: " + i + " " + standTreeLocations.get(i).size());
            //this.writeLineToLogfile("standid: " + i + " " + standTreeLocations.get(i).size());
        }

        //System.out.println(trees2.size() + " " + trees.size());

        for(int i = 0; i < trees.size(); i++){
            //System.out.println(trees.get(i).getMachineBearing());
        }


        double sum_a = 0;
        double sum_b = 0;

        for(int i = 0; i < trees.size(); i++){
            //System.out.println(trees.get(i));
            sum_a += trees.get(i).volume_kuitu;
        }

        for(int i : allTrees.keySet()){
            sum_b += allTrees.get(i).volume_kuitu;
        }
        System.out.println("a: " + sum_a + " b: " + sum_b);

        treeLocationEstimator estimator = new treeLocationEstimator(aR);


        estimator.setTrees(trees);
        estimator.setStandBoundaries(bufferedHulls);
        estimator.setStandPolygons(standPolygons);
        estimator.setHoles(holeConcaveHulls);

        if(aR.ref.size() > 0){
            estimator.setRasters(this.rasters);
        }

        if(aR.aux_file != null){

            estimator.setAuxiliaryDataFile(aR.aux_file);

            estimator.readRasters();

        }

        rasters.printCurrentSelectionFileNames();

        if(aR.noEstimation || (aR.estimationWithCHM && estimator.noAuxDataAvailable))
            estimator.noEstimation();
        else if(aR.simpleEstimation)
            estimator.simpleEstimation(10, 270, 2);
        else if(aR.simpleEstimationWithProb)
            estimator.simpleEstimationWithProbabilities(10, 270, 2, new double[]{0.1, 0.2, 0.5, 0.2}, new double[]{2.0, 6.0, 8.0, 10.0});
        else if(aR.estimationWithCHM)
            estimator.estimationWithAuxiliaryData(10, 270, 2,
                            new double[]{0.1, 0.2, 0.5, 0.2}, new double[]{2.0, 6.0, 8.0, 10.0}, 15.0);
        else if(aR.estimationSpecialThinning)
            estimator.simpleEstimationWithProbabilitiesOnlyBoomAngle(10, 270, 2, 25, 0.66);



        rasters.closeCurrentSelection();
        //

        //estimator.estimationWithAuxiliaryData(10, 180, 2,
        //        new double[]{0.1, 0.2, 0.5, 0.2}, new double[]{2.0, 6.0, 8.0, 10.0}, 15.0);

        System.out.println("STARTING OPTIMIZED ESTIMATION");
        //estimator.estimationWithAuxiliaryDataOptimized2();

        //System.exit(1);
        double[][] points = new double[trees.size()][2];



        for(int i = 0; i < trees.size(); i++){
            points[i][0] = trees.get(i).getX_coordinate_machine();
            points[i][1] = trees.get(i).getY_coordinate_machine();
        }


        double eps = 30.0;
        int minPts = 8;

        DBSCAN dbscan = new DBSCAN(eps, minPts);

        List<Set<Integer>> clusters = dbscan.performDBSCAN(points);

        //List<Set<Integer>> clusters = new ArrayList<>();
        System.out.println(clusters.size());

        for(int i = 0; i < trees.size(); i++){

            trees.get(i).setKey(-1);


        }
        System.out.println("Number of clusters found: " + clusters.size());

        for (int i = 0; i < clusters.size(); i++) {
            Set<Integer> cluster = clusters.get(i);
            System.out.println("Cluster " + i + ":");
            for (int point : cluster) {
                //System.out.println("tree: " + point + " " + trees.get(point).getX_coordinate_machine() + " " + trees.get(point).getY_coordinate_machine());
                trees.get(point).setKey(i);
            }
        }


        /*
        Point2D[] points = new Point2D[trees.size()];

        for(int i = 0; i < trees.size(); i++){
            points[i] = new Point2D.Double(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine());
        }


        List<RealVector> points2= new ArrayList<>();

        for(int i = 0; i < trees.size(); i++){
            points2.add(new ArrayRealVector(new double[] {trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine()}));
        }

        List<RealVector> points_f = filterPoints(points2);

        for(int i = 0; i < points2.size(); i++){
            trees.get(i).setX_coordinate_machine(points_f.get(i).getEntry(0));
            trees.get(i).setY_coordinate_machine(points_f.get(i).getEntry(1));
        }

*/
        System.out.println("starting to create plots");


        this.createPlots(allTrees, hulls, rand, holeConcaveHulls);

        System.out.println("created plots");

        System.out.println("starting to create grid");

        HashMap<Integer, ArrayList<ConcaveHull.Point>> original_stands = new HashMap<>();

        for(int i : standPolygons.keySet()){
            original_stands.put(i, standPolygons.get(i).toPoints());
        }

        //HashMap<Integer, int[][]> holeGrid = new HashMap<>();
        //System.exit(1);

        this.createGrid(allTrees, bufferedHulls, rand, 1, holeConcaveHulls, standGeometries);
        //this.createGrid(allTrees, original_stands, rand, 2);

        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "trees_filtered.txt");
        //ofile = new File("/home/koomikko/Documents/customer_work/metsahallitus/trees_filtered.txt");

        System.out.println("created plots");

        try {
            ofile.createNewFile();
            writeTrees(trees2, ofile);
        }catch (Exception e){
            e.printStackTrace();
        }

        //ofile = new File("/home/koomikko/Documents/customer_work/metsahallitus/trees.txt");
        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "trees.txt");

        try {
            ofile.createNewFile();
            writeTreesEstimatedLocations(trees, ofile);
        }catch (Exception e){
            e.printStackTrace();
        }

        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "unknown_volume.txt");

        try{
            ofile.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));
            bw.write("log_id nLogs logVolume logLength logDiameterMid logDiameterEnd" + "\n");
            for(int i = 0; i < unknown.size(); i++){

                bw.write(unknown.get(i)[0] + " " + unknown.get(i)[1] + " " + unknown.get(i)[2] + " " + unknown.get(i)[3] + " " + unknown.get(i)[4] + " " + unknown.get(i)[5] + "\n");

            }

            bw.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "kuitu.txt");

        try{
            ofile.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));
            bw.write("log_id nLogs logVolume logLength logDiameterMid logDiameterEnd" + "\n");
            for(int i = 0; i < knownKuitu.size(); i++){

                bw.write(knownKuitu.get(i)[0] + " " + knownKuitu.get(i)[1] + " " + knownKuitu.get(i)[2] + " " + knownKuitu.get(i)[3] + " " + knownKuitu.get(i)[4] + " " + knownKuitu.get(i)[5] + "\n");

            }

            bw.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "tukki.txt");

        try{
            ofile.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));
            bw.write("log_id nLogs logVolume logLength logDiameterMid logDiameterEnd" + "\n");
            for(int i = 0; i < knownTukki.size(); i++){

                bw.write(knownTukki.get(i)[0] + " " + knownTukki.get(i)[1] + " " + knownTukki.get(i)[2] + " " + knownTukki.get(i)[3] + " " + knownTukki.get(i)[4] + " " + knownTukki.get(i)[5] + "\n");

            }

            bw.close();

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public void parse_old() throws IOException{

        curveFitting fitter = new curveFitting(aR);

        SAXBuilder builder = new SAXBuilder();

        System.out.println("Parsing XML file: " + this.xml_file.getAbsolutePath());
        Document xml = null;
        try {
            xml = builder.build(this.xml_file);
        } catch (JDOMException e) {
            //e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


        Element root = null;

        try {
            root = xml.getRootElement();
        }catch (Exception e){
            System.out.println("Error parsing XML file: " + this.xml_file.getAbsolutePath());
            return;
        }

        File ofile =new File("/home/koomikko/Documents/customer_work/metsahallitus/parsed.hpr");

        //System.exit(1);
        Namespace ns = root.getNamespace();

        System.out.println("Root element of XML document is : " + root.getName());
        System.out.println("Number of books in this XML : " + root.getChildren().size());


        List<Element> lista =  root.getChildren();

        List<Element> machines = root.getChildren("Machine", ns);
        Element dates = root.getChild("HarvestedProductionHeader", ns);

        String date = dates.getChild("CreationDate", ns).getValue();
        System.out.println(date);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        //OffsetDateTime offsetDateTime = OffsetDateTime.parse(date, formatter);
        OffsetDateTime offsetDateTime = parseDate(date);

        //OffsetDateTime offsetDateTime_lidar = OffsetDateTime.parse(LIDAR_ACQUISITION_DATE, formatter);
        OffsetDateTime offsetDateTime_lidar = parseDate(LIDAR_ACQUISITION_DATE);


        if (offsetDateTime.isBefore(offsetDateTime_lidar)) {
            System.out.println("LIDAR IS AFTER THE HARVESTING.");
            writeLineToLogfile("LIDAR IS AFTER THE HARVESTING ; " + this.xml_file.getName());
            return;
        }else{
            System.out.println("LIDAR IS BEFORE THE HARVESTING.");
        }

        SpatialReference src = new SpatialReference();
        SpatialReference dst = new SpatialReference();

        src.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        dst.ImportFromProj4("+proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs");

        CoordinateTransformation ct = new CoordinateTransformation(src, dst);

        CRSFactory crsFactory = new CRSFactory();

        RegistryManager registryManager = crsFactory.getRegistryManager();
        registryManager.addRegistry(new EPSGRegistry());
        CoordinateReferenceSystem to = null;
        CoordinateReferenceSystem from = null;
        Set<CoordinateOperation> operations;
        CoordinateOperation op = null;

        try {
            to = crsFactory.getCRS("EPSG:3067");
            from = crsFactory.getCRS("EPSG:4326");
            operations = CoordinateOperationFactory
                    .createCoordinateOperations((GeodeticCRS) from, (GeodeticCRS) to);

            Iterator iter = operations.iterator();
            op = (CoordinateOperation) iter.next();
        }catch (Exception e){
            e.printStackTrace();
        }

        if(false)
            try {
                ofile.createNewFile();
                //BufferedReader br = new BufferedReader(new FileReader(in));
                BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));

                ArrayList<Tree> trees = new ArrayList<Tree>();
                printElement(root, bw);

                bw.close();
            }catch (Exception e){
                e.printStackTrace();
            }

        //System.exit(1);
        double[] transformed = null;

        //System.exit(1);
        ArrayList<Tree> trees = new ArrayList<Tree>();
        List<Element> stems = null;
        List<Element> productKeys = null;
        List<Element> species = null;
        List<Element> tmp = null;

        stemChecker sc = new stemChecker();

        String pine = "pine";


        ArrayList<double[]> unknown = new ArrayList<double[]>();
        ArrayList<double[]> knownTukki = new ArrayList<double[]>();
        ArrayList<double[]> knownKuitu = new ArrayList<double[]>();
        ArrayList<double[]> stemCoordinates = new ArrayList<double[]>();
        HashSet<Integer> stemsOutsideStands = new HashSet<>();
        HashMap<Integer, Tree> allTrees = new HashMap<Integer, Tree>();
        HashSet<Integer> uniqueStands = new HashSet<>();


        for(int i = 0; i < machines.size(); i++){

            species = machines.get(i).getChildren("SpeciesGroupDefinition", ns);

            for(int s = 0; s < species.size(); s++){
                //System.out.println(species.get(s).getChild("SpeciesGroupKey", ns).getValue());
                tmp = species.get(s).getChildren("StemTypeDefinition", ns);


                for(int o = 0; o < tmp.size(); o++){

                    String name = (tmp.get(o).getChild("StemTypeName", ns).getValue());


                    if(name.toLowerCase().contains("mänty")){
                        sc.addPine(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                        sc.addPine(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                        //break;
                    }else if(name.toLowerCase().contains("koivu")){
                        sc.addBirch(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                        sc.addBirch(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                        //break;
                    }else if(name.toLowerCase().contains("kuusi")){
                        sc.addSpruce(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                        sc.addSpruce(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                        //break;
                    }else if(name.toLowerCase().contains("puulaji")){
                        sc.addOther(Integer.parseInt(species.get(s).getChild("SpeciesGroupKey", ns).getValue()));
                        sc.addOther(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                        //break;
                    }else{

                    }

                    if(name.toLowerCase().contains("kuitu")){
                        sc.addKuitu(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                        //break;
                    }
                    if(name.toLowerCase().contains("tukki")){
                        sc.addTukki(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                        //break;
                    }
                    if(name.toLowerCase().contains("runkolaji")){
                        sc.addMuuPuutavaraLaji(Integer.parseInt(tmp.get(o).getChild("StemTypeCode", ns).getValue()));
                        //break;
                    }

                }

            }

            productKeys = machines.get(i).getChildren("ProductDefinition", ns);
            //System.out.println(productKeys.size());

            HashMap<Integer, Integer> productKeyToStemTypeCode = new HashMap<Integer, Integer>();



            for(int s = 0; s < productKeys.size(); s++){

                //System.out.println(productKeys.get(s).getChild("ProductKey", ns).getValue());
                productKeyToStemTypeCode.put(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()), -1);
                List<Element> lapset_1 = productKeys.get(s).getChildren();

                for(int o = 0; o < lapset_1.size(); o++){

                    if(lapset_1.get(o).getName().equals("ClassifiedProductDefinition")){

                        List<Element> lapset = productKeys.get(s).getChild("ClassifiedProductDefinition", ns).getChildren();

                        for(int o_ = 0; o_ < lapset.size(); o_++){

                            if(lapset.get(o_).getName().equals("StemTypeCode")){
                                productKeyToStemTypeCode.put(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()), Integer.parseInt(lapset.get(o_).getValue()));
                            }

                            if(lapset.get(o_).getName().equals("ProductGroupName")){
                                String puutavaralaji = lapset.get(o_).getValue();
                                //System.out.println(puutavaralaji);
                                //System.out.println(productKeys.get(s).getChild("ClassifiedProductDefinition", ns).getChild("ProductGroupName", ns).getValue());

                                if(puutavaralaji.toLowerCase().contains("kuitu")){
                                    sc.addKuitu(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                }
                                if(puutavaralaji.toLowerCase().contains("pikkutukki")){
                                    sc.addPikkutukki(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                }else if(puutavaralaji.toLowerCase().contains("tukki")){
                                    sc.addTukki(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                }
                                if(puutavaralaji.toLowerCase().contains("energia")){
                                    sc.addEnergia(Integer.parseInt(productKeys.get(s).getChild("ProductKey", ns).getValue()));
                                }
                            }
                        }

                    }
                }
            }

            stems = machines.get(i).getChildren("Stem", ns);

            System.out.println(stems.size());

            KdTree.XYZPoint point = new KdTree.XYZPoint(0, 0, 0);

            boolean firstInside = false;
            int countOutside = 0;
            double previousDistance = 0;

            ArrayList<Integer> currentOutside = new ArrayList<>();
            HashSet<Integer> toBeClassifiedOutsideStands = new HashSet<>();

            int currentStandId = -1;

            for(int s = 0; s < stems.size(); s++){

                Tree tempTree = new Tree();
                Element stem = stems.get(s);

                tempTree.HPR_FILE_NAME = this.xml_file.getName();

                List<Element> stem_coordinates = stem.getChildren("StemCoordinates", ns);

                if(stem_coordinates.size() == 0){
                    //System.out.println("No coordinates for stem " + stem.getChild("StemNumber", ns).getValue());
                    continue;
                }

                //System.out.println(Arrays.toString(stem_coordinates.toArray()));
                double latitude = Double.parseDouble(stem_coordinates.get(0).getChild("Latitude", ns).getValue());
                double longitude = Double.parseDouble(stem_coordinates.get(0).getChild("Longitude", ns).getValue());
                double altitude = Double.parseDouble(stem_coordinates.get(0).getChild("Altitude", ns).getValue());

                //System.out.println("speciesgroupke: " + stem.getChild("SpeciesGroupKey", ns).getValue());
                tempTree.setSpecies(sc.getSpecies(Integer.parseInt(stem.getChild("SpeciesGroupKey", ns).getValue())));
                //System.out.println("Species: " + tempTree.getSpecies());
                //System.out.println(latitude + " " + longitude);
                try {
                    transformed = op.transform(new double[]{longitude, latitude});
                }catch (Exception e){
                    e.printStackTrace();
                }

                //System.out.println(Arrays.toString(transformed));
                tempTree.setX_coordinate_machine(transformed[0]);
                tempTree.setY_coordinate_machine(transformed[1]);
                tempTree.setPoint();

                int insideStand = treeInPolygons(new double[]{transformed[0], transformed[1]}, standBounds);

                tempTree.standId = insideStand;
                tempTree.setGlobal_id(this.runningId++);

                int stemNumber = Integer.parseInt(stem.getChild("StemNumber", ns).getValue());

                tempTree.setId(stemNumber);


                if(insideStand == -1){

                    point.setX(transformed[0]);
                    point.setY(transformed[1]);
                    point.setZ(0);
                    List<KdTree.XYZPoint> res = (List<KdTree.XYZPoint>)centroids.nearestNeighbourSearch2d(5, point);


                    int whichOne = -1;
                    double distance = Double.POSITIVE_INFINITY;

                    for(int i_ = 0; i_ < res.size(); i_++){

                        double distance_ = distanceToPolygon(new double[]{transformed[0], transformed[1]}, standBounds.get(res.get(i_).getIndex()));

                        if(distance_ < distance){
                            distance = distance_;
                            whichOne = res.get(i_).getIndex();
                        }
                    }

                    tempTree.standId = whichOne;

                    //System.out.println("distance: " + distance + " " + (res.get(0).getIndex()) + " " + (res.get(1).getIndex()));

                    tempTree.distanceToStandBorder = distance;
                    countOutside++;

                    previousDistance = distance;

                    currentOutside.add(tempTree.getId());

                    if(distance > maxDistanceFromStandBorder){

                        toBeClassifiedOutsideStands.addAll(currentOutside);


                        if(firstInside){
                            firstInside = false;
                        }

                    }

                }else{

                    currentStandId = insideStand;
                    currentOutside.clear();
                    firstInside = true;
                    countOutside = 0;

                }

                if(!firstInside && insideStand == -1){
                    tempTree.trulyInStand = false;
                }

                if(tempTree.standId != -1){
                    uniqueStands.add(tempTree.standId);
                }

                //System.out.println("stemnumber: " + stemNumber);
                List<Element> stemInfo = null;
                String name = "";
                try{
                    stemInfo = stem.getChild("SingleTreeProcessedStem", ns).getChildren("Log", ns);
                    name = "SingleTreeProcessedStem";
                }catch (Exception e){
                    stemInfo = stem.getChild("SingleTreeFelledStem", ns).getChildren("Log", ns);
                    name = "SingleTreeFelledStem";
                }
                //List<Element> stemInfo__ = stem.getChild("SingleTreeProcessedStem", ns).getChildren();

                double dbh = Double.parseDouble(stem.getChild(name, ns).getChild("DBH", ns).getValue());
                tempTree.setDiameter((float)dbh);
                //System.out.println("logs: " + stemInfo.size());

                ArrayList<Double> stemCurveX = new ArrayList<Double>();
                ArrayList<Double> stemCurveY = new ArrayList<Double>();

                //stemCurveX.add(1.3);
                //stemCurveY.add(dbh / 100.0);

                double height = diameterToHeight(dbh, tempTree.getSpecies());
                //System.out.println("height: " + height);
                tempTree.setHeight((float)height);

                double currentHeight = 0.5;

                for(int i_ = 0; i_ < stemInfo.size(); i_++){

                    double volume = Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue());

                    //System.out.println("volume: " + volume);
                    //System.out.println(stemInfo.get(i_).getChild("ProductKey", ns).getValue());
                    //System.out.println(stemInfo.get(i_));
                    //tempTree.setSpecies(sc.getSpecies(productKeyToStemTypeCode.get(Integer.parseInt(stemInfo.get(i_).getChild("ProductKey", ns).getValue()))));


                    int puutavaralaji = 0;


                    if(productKeyToStemTypeCode.get(Integer.parseInt(stemInfo.get(i_).getChild("ProductKey", ns).getValue())) != -1){
                        puutavaralaji = sc.getPuutavaraLaji(productKeyToStemTypeCode.get(Integer.parseInt(stemInfo.get(i_).getChild("ProductKey", ns).getValue())));

                    }else
                        // THIS MEANS WE HAVE GARBO DATA
                        puutavaralaji = -1;
                    //int puutavaraLaji = sc.getPuutavaraLaji(productKeyToStemTypeCode.get(Integer.parseInt(stemInfo.get(i_).getChild("ProductKey", ns).getValue())));

                   /*
                    if(kuitu.contains(value)){
                        return 1;
                    }
                    if(tukki.contains(value)){
                        return 2;
                    }
                    if(energia.contains(value)){
                        return 3;
                    }
                    if(pikkutukki.contains(value)){
                        return 4;
                    }
                    */

                    double loglength = -1;
                    double logdiameterMid = -1;
                    double logdiameterEnd = -1;



                    if(puutavaralaji == 1){
                        tempTree.volume_kuitu += volume;

                        Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);
                        int size = stemInfo_.getChildren("LogDiameter", ns).size();

                        if(size >= 3) {
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                        }else{
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                        }

                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterEnd / 10.0);
                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterMid / 10.0);

                        knownKuitu.add(new double[]{Double.parseDouble(stemInfo.get(i_).getChildren("LogKey", ns).get(0).getValue()),
                                stemInfo.size(),
                                Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue()),
                                loglength,logdiameterMid, logdiameterEnd
                        });


                    }
                    if(puutavaralaji == 2){
                        tempTree.volume_tukki += volume;

                        Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                        int size = stemInfo_.getChildren("LogDiameter", ns).size();

                        if(size >= 3) {
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                        }else{
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                        }
                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterEnd / 10.0);
                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterMid / 10.0);

                        knownTukki.add(new double[]{Double.parseDouble(stemInfo.get(i_).getChildren("LogKey", ns).get(0).getValue()),
                                stemInfo.size(),
                                Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue()),
                                loglength,logdiameterMid, logdiameterEnd
                        });

                        //System.out.println(Arrays.toString(unknown.get(unknown.size()-1)));

                    }
                    if(puutavaralaji == 3){
                        tempTree.volume_energia += volume;

                        Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                        int size = stemInfo_.getChildren("LogDiameter", ns).size();

                        if(size >= 3) {
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                        }else{
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                        }
                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterEnd / 10.0);
                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterMid / 10.0);

                    }
                    if(puutavaralaji == 4){
                        tempTree.volume_pikkutukki += volume;

                        Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                        int size = stemInfo_.getChildren("LogDiameter", ns).size();

                        if(size >= 3) {
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                        }else{
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                        }
                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterEnd / 10.0);
                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterMid / 10.0);

                    }
                    if(puutavaralaji == -1){

                        tempTree.volume_unknown += volume;

                        Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                        int size = stemInfo_.getChildren("LogDiameter", ns).size();

                        if(size >= 3) {
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());
                        }else{
                            loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                            logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                            logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                        }
                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterEnd / 10.0);
                        currentHeight += loglength / 2.0 / 100.0;
                        stemCurveX.add(currentHeight);
                        stemCurveY.add(logdiameterMid / 10.0);

                        unknown.add(new double[]{Double.parseDouble(stemInfo.get(i_).getChildren("LogKey", ns).get(0).getValue()),
                                stemInfo.size(),
                                Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue()),
                                loglength,logdiameterMid, logdiameterEnd
                        });

                        //System.out.println(Arrays.toString(unknown.get(unknown.size()-1)));
                    }

                }


                tempTree.setStemCurveX(stemCurveX.stream().mapToDouble(Double::doubleValue).toArray());
                tempTree.setStemCurveY(stemCurveY.stream().mapToDouble(Double::doubleValue).toArray());

                //System.out.println(Arrays.toString(tempTree.getStemCurveX()));
                //System.out.println(Arrays.toString(tempTree.getStemCurveY()));
                //tempTree.estimateHeight(fitter);
                //System.out.println(tempTree.getHeight());
                //System.out.println("---------");
                trees.add(tempTree);


                allTrees.put(tempTree.getId(), tempTree);

                processedStands.add(tempTree.standId);


                if(true)
                    continue;
/*
                Element stemInfo = stem.getChild("Extension", ns).getChild("HPRCMResults", ns).getChild("StemInfo", ns);

                float treeHeight = Float.parseFloat(stemInfo.getChild("TreeHeight",ns).getValue());

                tempTree.setHeight(treeHeight);

                tempTree.setBoomPosition(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("BoomAngle", ns).getValue()));
                tempTree.setBoomExtension(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("BoomExtension", ns).getValue()));
                tempTree.setMachineBearing(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("MachineBearing", ns).getValue()));
                tempTree.setDiameter(Float.parseFloat(stem.getChild("SingleTreeProcessedStem", ns).getChild("DBH", ns).getValue()));

                System.out.println(tempTree);
                System.exit(1);

 */
            }

            for(int tree : toBeClassifiedOutsideStands){
                allTrees.get(tree).trulyInStand = false;
            }

        }


        if(trees.size() != stems.size()){

            System.out.println("Trees and stems are not the same size. Something went wrong.");

            failedFiles.add(this.xml_file.getAbsolutePath());
            failedFilesProps.add(new int[]{trees.size(), stems.size()});

            if(trees.size() == 0)
                return;

            /*
            try {
                ofile.createNewFile();
                //BufferedReader br = new BufferedReader(new FileReader(in));
                BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));

                trees = new ArrayList<Tree>();
                printElement(root, bw);

                bw.close();
            }catch (Exception e){
                e.printStackTrace();
            }

            throw new toolException("Trees and stems are not the same size. Something went wrong.");

            */
        }

        HashMap<Integer, ArrayList<tools.ConcaveHull.Point>> standTreeLocations = new HashMap<>();
        HashMap<Integer, ArrayList<Point>> standTreeLocations_convex = new HashMap<>();
        HashMap<Integer, ArrayList<double[]>> standTreeLocations_ = new HashMap<>();
        HashMap<Integer, Integer> standPolygonIds = new HashMap<>();


        for(int tree : allTrees.keySet()){

            if(!allTrees.get(tree).trulyInStand)
                continue;

            if(standTreeLocations.containsKey(allTrees.get(tree).standId)) {
                standTreeLocations.get(allTrees.get(tree).standId).add(new tools.ConcaveHull.Point(allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()));
                standTreeLocations_.get(allTrees.get(tree).standId).add(new double[]{allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()});
                standTreeLocations_convex.get(allTrees.get(tree).standId).add(new Point(allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()));
            }else{
                ArrayList<tools.ConcaveHull.Point> temp = new ArrayList<>();
                ArrayList<double[]> temp_ = new ArrayList<>();
                ArrayList<Point> temp_convex = new ArrayList<>();
                temp_.add(new double[]{allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()});
                temp.add(new tools.ConcaveHull.Point(allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()));
                temp_convex.add(new Point(allTrees.get(tree).getX_coordinate_machine(), allTrees.get(tree).getY_coordinate_machine()));
                standTreeLocations.put(allTrees.get(tree).standId, temp);
                standTreeLocations_.put(allTrees.get(tree).standId, temp_);
                standTreeLocations_convex.put(allTrees.get(tree).standId, temp_convex);

            }
        }

        HashSet<Integer> removeThese = new HashSet<>();
        HashSet<Integer> removeTheseSets = new HashSet<>();
        Set<Integer> removeThese_ = new TreeSet<>(Comparator.reverseOrder());
        Set<Integer> removeThese__ = new TreeSet<>(Comparator.reverseOrder());



        for(int i : standTreeLocations.keySet()){
/*
            if(standTreeLocations.get(i).size() < 3){
                System.out.println("REMOVE: " + i + " ntrees: " + standTreeLocations.get(i).size());
                removeThese.add(i);
                continue;
            }

 */
            Geometry g = createPolygon(standTreeLocations_.get(i));

            double[][] polygon = standBounds.get(i);

            Geometry g2 = createPolygon(polygon);

            System.out.println("Area: " + g.GetArea()/10000);
            double treesPerHectare = standTreeLocations.get(i).size() / (g.GetArea()/10000);
            double treesPerHectare2 = standTreeLocations.get(i).size() / (g2.GetArea()/10000);
            System.out.println("Trees per hectare: " + treesPerHectare);
            System.out.println("Trees per hectare2: " + treesPerHectare2);
            //if(standTreeLocations.get(i).size() < 10) {
            if(standTreeLocations.get(i).size() < 10){ // treesPerHectare2 < 100 ||
                System.out.println("REMOVE: " + i + " ntrees: " + standTreeLocations.get(i).size());
                removeThese.add(i);
            }
        }


        for(int i : removeThese){
            standTreeLocations_.remove(i);
            standTreeLocations.remove(i);
            standTreeLocations_convex.remove(i);
        }

        for(int i = 0; i < trees.size(); i++){

            if(removeThese.contains(trees.get(i).standId)){
                removeThese_.add(i);
                removeThese__.add(trees.get(i).getId());

            }
        }

        for(int i : removeThese_){
            trees.remove(i);
        }

        for(int i : removeThese__){
            allTrees.remove(i);
        }

        for(Tree tree : trees){
            //if(tree.standId == 123){
            //    System.out.println("FOUND " + removeThese.contains(tree.standId));
            //    System.exit(1);
            //}
        }

        HashMap<Integer, ArrayList<ConcaveHull.Point>> hulls = new HashMap<>();
        HashMap<Integer, ArrayList<Point>> hulls_convex = new HashMap<>();

        QuickHull quickHull = new QuickHull();
        HashSet<Integer> removeThese2 = new HashSet<>();

        System.out.println("Calculating hulls " + standTreeLocations.size());

        for(int i : standTreeLocations.keySet()){

            ArrayList<tools.ConcaveHull.Point> currentTrees = standTreeLocations.get(i);
            ArrayList<ConcaveHull.Point> hull = calculateConcaveHull(currentTrees, concave_hull_k);

            //ArrayList<Point> convexHullPoints = quickHull.quickHull(standTreeLocations_convex.get(i));

            //hulls_convex.put(i, convexHullPoints);

            if(hull.size() < 3){

                removeThese2.add(i);

            }else
                hulls.put(i, hull);
            //System.out.println("Hull size: " + hull.size() + " " + convexHullPoints.size());

        }

        Set<Integer> removeThese_2 = new TreeSet<>(Comparator.reverseOrder());
        Set<Integer> removeThese__2 = new TreeSet<>(Comparator.reverseOrder());

        for(int i : removeThese2){
            standTreeLocations_.remove(i);
            standTreeLocations.remove(i);
            standTreeLocations_convex.remove(i);
        }

        for(int i = 0; i < trees.size(); i++){

            if(removeThese2.contains(trees.get(i).standId)){

                removeThese_2.add(i);
                removeThese__2.add(trees.get(i).getId());

            }
        }

        for(int i : removeThese_2){
            trees.remove(i);
        }

        for(int i : removeThese__2){
            allTrees.remove(i);
        }


        //this.createPlots(allTrees, hulls);

        //System.exit(1);


        System.out.println(standTreeLocations.size());

        String pathSeparator = System.getProperty("file.separator");

        System.out.println("pathSeparator: " + pathSeparator);
        //System.exit(1);

        //System.out.println("xml: " + Arrays.toString(xml_file.getName().split(("\\."))));
        File output_directory = new File(aR.odir + pathSeparator + xml_file.getName().split("\\.")[0]);


        System.out.println("output_directory: " + output_directory.getAbsolutePath());
        output_directory.mkdirs();

        HashMap<Integer, List<ConcaveHull.Point>> bufferedHulls = new HashMap<>();



        for(int i : standTreeLocations.keySet()){

            File tmpFile = new File(output_directory.getAbsolutePath() + pathSeparator + "stand_" + i + "_boundary.shp");
            System.out.println("HULL SIZE: " + hulls.get(i).size());
            //ArrayList<ConcaveHull.Point> buf_ = exportShapefile_(tmpFile, hulls.get(i), i, new HashMap<Integer, List<List<ConcaveHull.Point>>>());

            //bufferedHulls.put(i, buf_);
        }


        dynamicStatistics stats_x = new dynamicStatistics();
        dynamicStatistics stats_y = new dynamicStatistics();

        ArrayList<Tree> trees2 = new ArrayList<Tree>();

        ArrayList<int[]> trackTreeIds = new ArrayList<int[]>();

        double maxDistanceFromAchrorTree = 5.0;

        Tree anchorTree = new Tree();
        Tree previousAnchorTree = new Tree();

        int trees_size = trees2.size() - 1;

        double previousMeanX = 0;
        double previousMeanY = 0;

        double trackChangeDistance = maxDistanceFromAchrorTree * 8;

        int trackID = 0;
        int counter = 0;

        boolean smartStuf = false;

        double[] p1_ = new double[]{5,1};
        double[] p2_ = new double[]{5,5};
        double[] p_ = new double[]{3,3};

        System.out.println(canDrawPerpendicular(p_[0],p_[1], p1_, p2_));

        int countAnchors = 0;

        boolean firstBatchCorrected = false;

        //System.exit(1);
        for(int i = 0; i < trees.size(); i++){

            previousMeanY = stats_y.getMean();
            previousMeanX = stats_x.getMean();

            // We start a new averaging
            if(trees_size != trees2.size()){
                trees_size = trees2.size();

                stats_x.reset();
                stats_y.reset();
                stats_x.add(trees.get(i).getX_coordinate_machine());
                stats_y.add(trees.get(i).getY_coordinate_machine());

            }else {

                stats_x.add(trees.get(i).getX_coordinate_machine());
                stats_y.add(trees.get(i).getY_coordinate_machine());
            }

            double distance = euclideanDistance(stats_x.getMean(), stats_y.getMean(), anchorTree.getX_coordinate_machine(), anchorTree.getY_coordinate_machine());
            double distance2 = euclideanDistance(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine(), anchorTree.getX_coordinate_machine(), anchorTree.getY_coordinate_machine());

            if(distance > maxDistanceFromAchrorTree){

                /*
                              if(distance2 > trackChangeDistance){
                    tmpTree.setX_coordinate_machine(previousMeanX);
                    tmpTree.setY_coordinate_machine(previousMeanY);

                    trackID++;
                }else{
                    tmpTree.setX_coordinate_machine(stats_x.getMean());
                    tmpTree.setY_coordinate_machine(stats_y.getMean());

                    tmpTree.setX_coordinate_machine(previousMeanX);
                    tmpTree.setY_coordinate_machine(previousMeanY);


                }

                 */
                Tree tmpTree = new Tree();

                boolean isBad = false;

                if(distance2 > trackChangeDistance) {

                    tmpTree.setX_coordinate_machine(previousMeanX);
                    tmpTree.setY_coordinate_machine(previousMeanY);

                }else{

                    if(smartStuf) {
                        if (trees2.size() > 2) {

                            double[] p1 = new double[]{trees2.get(trees2.size() - 2).getX_coordinate_machine(), trees2.get(trees2.size() - 2).getY_coordinate_machine()};
                            double[] p2 = new double[]{trees2.get(trees2.size() - 1).getX_coordinate_machine(), trees2.get(trees2.size() - 1).getY_coordinate_machine()};
                            double[] p = new double[]{stats_x.getMean(), stats_y.getMean()};

                            //while (isPointBetween(p1, p2, p) && stats_x.getNumValues() > 2) {
                            System.out.println("REMOVE");
                            stats_x.removeLast();
                            stats_y.removeLast();
                            p = new double[]{stats_x.getMean(), stats_y.getMean()};
                            p1 = new double[]{trees2.get(trees2.size() - 2).getX_coordinate_machine(), trees2.get(trees2.size() - 2).getY_coordinate_machine()};
                            p2 = new double[]{trees2.get(trees2.size() - 1).getX_coordinate_machine(), trees2.get(trees2.size() - 1).getY_coordinate_machine()};
                            //}

                            //if (isPointBetween(p1, p2, p)) {
                            isBad = true;
                            //}


                            tmpTree.setX_coordinate_machine(previousMeanX);
                            tmpTree.setY_coordinate_machine(previousMeanY);
                        } else {
                            tmpTree.setX_coordinate_machine(stats_x.getMean());
                            tmpTree.setY_coordinate_machine(stats_y.getMean());
                        }
                    }else{
                        tmpTree.setX_coordinate_machine(stats_x.getMean());
                        tmpTree.setY_coordinate_machine(stats_y.getMean());
                    }
                }

                //tmpTree.setX_coordinate_machine(previousMeanX);
                //tmpTree.setY_coordinate_machine(previousMeanY);

                if(!isBad) {
                    countAnchors++;
                    tmpTree.setId(trackID);
                    tmpTree.setKey(counter++);
                    trees2.add(tmpTree);

                    previousAnchorTree = anchorTree.clone();
                    anchorTree = trees.get(i);

                    if (distance2 > trackChangeDistance) {
                        trackID++;
                    }
                }
            }
            else {

            }


            if(countAnchors >= 2){

                double bearing = angleFromOnePointToAnother(previousAnchorTree.getX_coordinate_machine(), previousAnchorTree.getY_coordinate_machine(), anchorTree.getX_coordinate_machine(), anchorTree.getY_coordinate_machine());


                if(!firstBatchCorrected) {

                    firstBatchCorrected = true;

                    for (int i_ = 0; i_ <= i; i_++) {

                        trees.get(i_).setMachineBearing(bearing);

                    }

                }else{
                    trees.get(i).setMachineBearing(bearing);
                }
            }else{

            }
        }

        for(int i : standTreeLocations.keySet()){
            //System.out.println("standid: " + i + " " + standTreeLocations.get(i).size());
            //this.writeLineToLogfile("standid: " + i + " " + standTreeLocations.get(i).size());
        }

        //System.out.println(trees2.size() + " " + trees.size());

        for(int i = 0; i < trees.size(); i++){
            //System.out.println(trees.get(i).getMachineBearing());
        }

        treeLocationEstimator estimator = new treeLocationEstimator(aR);


        estimator.setTrees(trees);
        estimator.setStandBoundaries(bufferedHulls);
        estimator.setStandPolygons(standPolygons);


        if(aR.ref.size() > 0){
            estimator.setRasters(this.rasters);
        }

        if(aR.aux_file != null){

            estimator.setAuxiliaryDataFile(aR.aux_file);
            estimator.readRasters();

        }

        rasters.printCurrentSelectionFileNames();

        if(aR.noEstimation || (aR.estimationWithCHM && estimator.noAuxDataAvailable))
            estimator.noEstimation();
        else if(aR.simpleEstimation)
            estimator.simpleEstimation(10, 270, 2);
        else if(aR.simpleEstimationWithProb)
            estimator.simpleEstimationWithProbabilities(10, 270, 2, new double[]{0.1, 0.2, 0.5, 0.2}, new double[]{2.0, 6.0, 8.0, 10.0});
        else if(aR.estimationWithCHM)
            estimator.estimationWithAuxiliaryData(10, 270, 2,
                    new double[]{0.1, 0.2, 0.5, 0.2}, new double[]{2.0, 6.0, 8.0, 10.0}, 15.0);
        else if(aR.estimationSpecialThinning)
            estimator.simpleEstimationWithProbabilitiesOnlyBoomAngle(10, 270, 2, 25, 0.66);



        rasters.closeCurrentSelection();
        //

        //estimator.estimationWithAuxiliaryData(10, 180, 2,
        //        new double[]{0.1, 0.2, 0.5, 0.2}, new double[]{2.0, 6.0, 8.0, 10.0}, 15.0);

        System.out.println("STARTING OPTIMIZED ESTIMATION");
        //estimator.estimationWithAuxiliaryDataOptimized2();

        //System.exit(1);
        double[][] points = new double[trees.size()][2];



        for(int i = 0; i < trees.size(); i++){
            points[i][0] = trees.get(i).getX_coordinate_machine();
            points[i][1] = trees.get(i).getY_coordinate_machine();
        }


        double eps = 30.0;
        int minPts = 8;

        DBSCAN dbscan = new DBSCAN(eps, minPts);

        List<Set<Integer>> clusters = dbscan.performDBSCAN(points);

        //List<Set<Integer>> clusters = new ArrayList<>();
        System.out.println(clusters.size());

        for(int i = 0; i < trees.size(); i++){

            trees.get(i).setKey(-1);


        }
        System.out.println("Number of clusters found: " + clusters.size());

        for (int i = 0; i < clusters.size(); i++) {
            Set<Integer> cluster = clusters.get(i);
            System.out.println("Cluster " + i + ":");
            for (int point : cluster) {
                //System.out.println("tree: " + point + " " + trees.get(point).getX_coordinate_machine() + " " + trees.get(point).getY_coordinate_machine());
                trees.get(point).setKey(i);
            }
        }


        /*
        Point2D[] points = new Point2D[trees.size()];

        for(int i = 0; i < trees.size(); i++){
            points[i] = new Point2D.Double(trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine());
        }


        List<RealVector> points2= new ArrayList<>();

        for(int i = 0; i < trees.size(); i++){
            points2.add(new ArrayRealVector(new double[] {trees.get(i).getX_coordinate_machine(), trees.get(i).getY_coordinate_machine()}));
        }

        List<RealVector> points_f = filterPoints(points2);

        for(int i = 0; i < points2.size(); i++){
            trees.get(i).setX_coordinate_machine(points_f.get(i).getEntry(0));
            trees.get(i).setY_coordinate_machine(points_f.get(i).getEntry(1));
        }

*/

        Random rand = new Random(12345);

        System.out.println("starting to create plots");
        //this.createPlots(allTrees, hulls, rand);

        System.out.println("created plots");

        System.out.println("starting to create grid");

        HashMap<Integer, ArrayList<ConcaveHull.Point>> original_stands = new HashMap<>();

        for(int i : standPolygons.keySet()){
            original_stands.put(i, standPolygons.get(i).toPoints());
        }

        //this.createGrid(allTrees, bufferedHulls, rand, 1);
        //this.createGrid(allTrees, original_stands, rand, 2);


        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "trees_filtered.txt");
        //ofile = new File("/home/koomikko/Documents/customer_work/metsahallitus/trees_filtered.txt");

        System.out.println("created plots");

        try {
            ofile.createNewFile();
            writeTrees(trees2, ofile);
        }catch (Exception e){
            e.printStackTrace();
        }

        //ofile = new File("/home/koomikko/Documents/customer_work/metsahallitus/trees.txt");
        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "trees.txt");

        try {
            ofile.createNewFile();
            writeTreesEstimatedLocations(trees, ofile);
        }catch (Exception e){
            e.printStackTrace();
        }

        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "unknown_volume.txt");

        try{
            ofile.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));
            bw.write("log_id nLogs logVolume logLength logDiameterMid logDiameterEnd" + "\n");
            for(int i = 0; i < unknown.size(); i++){

                bw.write(unknown.get(i)[0] + " " + unknown.get(i)[1] + " " + unknown.get(i)[2] + " " + unknown.get(i)[3] + " " + unknown.get(i)[4] + " " + unknown.get(i)[5] + "\n");

            }

            bw.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "kuitu.txt");

        try{
            ofile.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));
            bw.write("log_id nLogs logVolume logLength logDiameterMid logDiameterEnd" + "\n");
            for(int i = 0; i < knownKuitu.size(); i++){

                bw.write(knownKuitu.get(i)[0] + " " + knownKuitu.get(i)[1] + " " + knownKuitu.get(i)[2] + " " + knownKuitu.get(i)[3] + " " + knownKuitu.get(i)[4] + " " + knownKuitu.get(i)[5] + "\n");

            }

            bw.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "tukki.txt");

        try{
            ofile.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));
            bw.write("log_id nLogs logVolume logLength logDiameterMid logDiameterEnd" + "\n");
            for(int i = 0; i < knownTukki.size(); i++){

                bw.write(knownTukki.get(i)[0] + " " + knownTukki.get(i)[1] + " " + knownTukki.get(i)[2] + " " + knownTukki.get(i)[3] + " " + knownTukki.get(i)[4] + " " + knownTukki.get(i)[5] + "\n");

            }

            bw.close();

        }catch (Exception e){
            e.printStackTrace();
        }


        //System.exit(1);


        if(false)
            for(int i = 0; i < lista.size(); i++){

                List<Element> lista_ = lista.get(i).getChildren();

                System.out.println(lista.get(i).getName() + " has " + lista_.size() + " books.");
                System.out.println(Arrays.toString(lista_.toArray()));
                System.out.println(lista_.get(lista_.size()-1).getChildren());
                List<Element> lista__ = lista.get(i).getChildren("Stem", ns);
                System.out.println("Number of stems: " + lista__.size());

            }

    }

    public void mergeTreeFiles() throws IOException{

        Path directory = Paths.get(aR.odir);

        List<Path> directories = new ArrayList<>();

        // Ensure the directory exists
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path dir : stream) {
                    if (Files.isDirectory(dir)) {
                        directories.add(dir);
                        System.out.println("Directory: " + dir.getFileName());
                    }
                }
            }
        } else {
            System.out.println("The specified path is not a directory or doesn't exist.");
        }

        File ofile = new File(aR.odir + aR.pathSep + "trees.txt");

        BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));

        for(int i = 0; i < directories.size(); i++){

            File file = new File(directories.get(i).toString() + aR.pathSep + "trees.txt");

            System.out.println("HAHA: " + file.getAbsolutePath());
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;
            int linecount = 0;

            while((line = br.readLine()) != null){

                if( linecount == 0){

                    if(i != 0) {
                        linecount++;
                        continue;

                    }
                    else {
                        linecount++;
                        bw.write(line + "\n");
                        continue;
                    }


                }

                bw.write(line + "\n");
                linecount++;

            }

            br.close();

        }

        bw.close();

    }

    public double angleFromOnePointToAnother(double x_start, double y_start, double x_end, double y_end) {
        double deltaX = x_end - x_start;
        double deltaY = y_end - y_start;

        // Calculate the angle in radians
        double angleRad = Math.atan2(deltaY, deltaX);

        // Convert the angle from radians to degrees
        double angleDeg = Math.toDegrees(angleRad);

        // Adjust the angle to be within the range of 0 to 360 degrees
        if (angleDeg < 0) {
            angleDeg += 360;
        }

        // Adjust the angle to match the convention where 0 angle points north
        angleDeg = (angleDeg + 360) % 360;

        return angleDeg;
    }

    public ArrayList<double[]> makeConcave(LASReader pointCloud) throws Exception{

        ArrayList<double[]> segments = new ArrayList<>();

        List<org.tinfour.common.IQuadEdge> concaveEdges = null;
        Point[] concaveHullInput = null;

        ArrayList<double[]> border = new ArrayList<>();
        org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();

        HashSet<Double> donet = new HashSet<Double>();

        List<org.tinfour.common.Vertex> closest = new ArrayList<>();

        LasPoint tempPoint = new LasPoint();

        //aR.p_update.updateProgressNBorder();

        if(!pointCloud.isIndexed()){
            //aR.p_update.threadFile[coreNumber-1] = "indexing";
            //aR.p_update.updateProgressNBorder();
            pointCloud.index(20);
        }else{
            pointCloud.getIndexMap();
        }

        //aR.p_update.threadFile[coreNumber-1] = "finding lowest Y";
        //aR.p_update.updateProgressNBorder();
        org.tinfour.common.Vertex tempV;

        int count = 0;

        double minY = Double.POSITIVE_INFINITY;

        int counter = 0;
        int minYindex = 0;

        int maxi = 0;

        pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

        //aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();
        //aR.p_update.threadProgress[coreNumber-1] = 0;

        /* First find the index of the lowest Y point
         */
        for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000){

            maxi = (int)Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            pointCloud.readRecord_noRAF(i, tempPoint, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                //if(!rule.ask(tempPoint, i+j, true)){
                //    continue;
                //}

                if(tempPoint.y < minY) {
                    minYindex = counter;
                    minY = tempPoint.y;
                }

                counter++;
                //aR.p_update.threadProgress[coreNumber-1] = counter;

                //if(counter % 10000 == 0){
                //    aR.p_update.updateProgressNoise();
                //}
            }
        }

        //aR.p_update.threadFile[coreNumber-1] = "finding border";

        //aR.p_update.updateProgressNBorder();

        LasPoint tempPoint1 = new LasPoint();
        LasPoint tempPoint2 = new LasPoint();
        LasPoint tempPoint3 = new LasPoint();

        /*
          tempPoint3 now holds the lowest Y point
         */
        pointCloud.readRecord(minYindex, tempPoint3);

        int startIndex = minYindex;

        double[] prevSegment = new double[]{0,0,0,0};

        tempPoint1.x = 0;
        tempPoint1.y = 0;

        HashSet<Long> doneIndexes = new HashSet<>();

        doneIndexes.add((long)startIndex);

        Path2D path = new Path2D.Double();
        //Path path2 = new Path();

        path.moveTo(tempPoint3.x, tempPoint3.y);

        double angle;
        //ArrayList<double[]> border = new ArrayList<>();

        border.add(new double[]{tempPoint3.x, tempPoint3.y});

        //tin.add(new org.tinfour.common.Vertex(tempPoint1.x, tempPoint1.y, 0));
        tin.add(new org.tinfour.common.Vertex(tempPoint3.x, tempPoint3.y, 0));

        int size = 1;

        double increase = 0.0;

        double concavity = aR.concavity;

        concavity = 125.0;
        while (pointCloud.queriedIndexes2.size() <= 1) {

            //this.pointCloud.query2(tempPoint3.x - concavity + increase, tempPoint3.x + concavity + increase, tempPoint3.y - concavity + increase, tempPoint3.y + concavity + increase);
            pointCloud.queryPoly2(tempPoint3.x - concavity + increase, tempPoint3.x + concavity + increase, tempPoint3.y - concavity + increase, tempPoint3.y + concavity + increase);
            increase += 5.0;


        }
        boolean running = true;

        int counti = 0;
        double dist = -1;

        int prevIndex = startIndex;

        double minidisti =-5.0;

        //aR.p_update.threadInt[coreNumber-1] = 0;

        //aR.p_update.threadEnd[coreNumber-1] = -1;
        //aR.p_update.threadProgress[coreNumber-1] = -1;

        //aR.p_update.updateProgressNBorder();

        int counteri = 0;

        /* Seems sketchy, but trust me, it will halt! */
        while(running) {

            minidisti =-5.0;

            double smallestAngle = Double.POSITIVE_INFINITY;
            long angleIndex = 0;

            while(!pointCloud.index_read_terminated){

                long p = pointCloud.fastReadFromQuery(tempPoint2);
                            /* Reading, so ask if this point is ok, or if
                            it should be modified.
                             */
                        //if(!rule.ask(tempPoint, i+p, true)){
                        //    continue;
                       // }

                        dist = euclideanDistance(tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y);

                        if (p != prevIndex && dist < concavity) { //  && !path.contains(tempPoint2.x, tempPoint2.y)

                            angle = angleBetween(tempPoint1.x, tempPoint1.y, tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y);

                            if (angle < 0.0)
                                angle = 360 + angle;

                            if (angle < smallestAngle && angle != 0.0) {

                                if (!intersection(tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y, segments) || (prevIndex != startIndex && p == startIndex)) {
                                    minidisti = dist;
                                    angleIndex = p;
                                    smallestAngle = angle;

                                    //aR.p_update.threadInt[coreNumber-1]++;

                                    //if(aR.p_update.threadInt[coreNumber-1] % 100 == 0)
                                    //    aR.p_update.updateProgressNBorder();
                                }
                            }
                        }
                        //count1++;


            }

            /* prevIndex always points to the previous vertex in the boundary. This guarantees that
              we don't trace back our steps in the bounday.
             */
            prevIndex = (int)angleIndex;

            prevSegment[0] = tempPoint1.x;
            prevSegment[1] = tempPoint1.y;
            prevSegment[2] = tempPoint3.x;
            prevSegment[3] = tempPoint3.y;

            segments.add(prevSegment.clone());

            angleIndex = Math.min(angleIndex, pointCloud.numberOfPointRecords-1);

            /* tempPoint now holds the next vertice of the boundary */
            pointCloud.readRecord((int)angleIndex, tempPoint);

            /* Assign temporary points as planned */
            tempPoint1.x = tempPoint3.x;
            tempPoint1.y = tempPoint3.y;
            tempPoint3.x = tempPoint.x;
            tempPoint3.y = tempPoint.y;

            /* Add the vertice to the boundary */
            border.add(new double[]{tempPoint.x, tempPoint.y});

            increase = 0.0;
            pointCloud.queriedIndexes2.clear();
            /* We search for points in the proximity of the new boundary vertex.
             */
            while (pointCloud.queriedIndexes2.size() <= 1) {
                //pointCloud.query2(tempPoint.x - concavity + increase, tempPoint.x + concavity + increase, tempPoint.y - concavity + increase, tempPoint.y + concavity + increase);
                pointCloud.queryPoly2(tempPoint.x - concavity + increase, tempPoint.x + concavity + increase, tempPoint.y - concavity + increase, tempPoint.y + concavity + increase);
                increase += 5.0;
            }

            /* Here is the termination condition! If the current vertex is the same as the starting vertex,
              we stop!
             */
            if(angleIndex == startIndex)
                running = false;

            counti++;
        }

        concaveEdges = tin.getPerimeter();

        System.out.println("final vertices: " + border.size());

        concaveHullInput = new Point[concaveEdges.size()];

        Point comparatorPoint = new Point(1,1);

        HashSet<org.tinfour.common.Vertex> vertexSetConcave = new HashSet<org.tinfour.common.Vertex>();

        ArrayList<org.tinfour.common.Vertex> vertexSetConcave2 = new ArrayList<org.tinfour.common.Vertex>();


        for(int i = 0; i < concaveEdges.size(); i++){

            org.tinfour.common.Vertex tempA = concaveEdges.get(i).getA();
            org.tinfour.common.Vertex tempB = concaveEdges.get(i).getB();

            if(!vertexSetConcave.contains(tempA)){
                vertexSetConcave.add(tempA);
                vertexSetConcave2.add(tempA);
            }

            if(!vertexSetConcave.contains(tempB)){
                vertexSetConcave.add(tempB);
                vertexSetConcave2.add(tempB);
            }

        }

        String pathSep = System.getProperty("file.separator");

        String oput = "";

        return border;

    }

    public boolean intersection(double x1, double y1, double x2, double y2, ArrayList<double[]> segments){

        for(int i = 0; i < segments.size(); i++)
            if(intersects(segments.get(i)[0], segments.get(i)[1], segments.get(i)[2], segments.get(i)[3],
                    x1, y1, x2, y2))
                return true;

        return false;
    }

    boolean intersects(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double bx = x2 - x1;
        double by = y2 - y1;
        double dx = x4 - x3;
        double dy = y4 - y3;
        double b_dot_d_perp = bx * dy - by * dx;
        if (b_dot_d_perp == 0) {
            return false;
        }
        double cx = x3 - x1;
        double cy = y3 - y1;
        double t = (cx * dy - cy * dx) / b_dot_d_perp;
        if (t < 0 || t > 1) {
            return false;
        }
        double u = (cx * by - cy * bx) / b_dot_d_perp;
        return !(u < 0) && !(u > 1);
    }


    public void exportShapefileConvex(File outputFileName, ArrayList<Point> border, int id){

        //ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        Geometry outShpGeom2 = new Geometry(ogr.wkbLinearRing);
        Geometry outShpGeom = new Geometry(ogr.wkbPolygon);

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        DataSource outShp = shpDriver.CreateDataSource(outputFileName.getAbsolutePath());

        Layer outShpLayer = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id",0);

        outShpLayer.CreateField(layerFieldDef);
        FeatureDefn outShpFeatDefn = outShpLayer.GetLayerDefn();
        Feature outShpFeat = new Feature(outShpFeatDefn);

        for(int i = 0; i < border.size(); i++) {

            //System.out.println("Wrote " + i);
            outShpGeom2.AddPoint_2D(border.get(i).getX(), border.get(i).getY());
            System.out.println(border.get(i).getX() + " " + border.get(i).getY());
        }

        outShpGeom.AddGeometry(outShpGeom2);

        outShpFeat.SetField("id",id);

        outShpFeat.SetGeometryDirectly(outShpGeom);
        outShpLayer.CreateFeature(outShpFeat);
        outShpLayer.SyncToDisk();
        System.out.println("features: " + outShpLayer.GetFeatureCount());



    }

    public void exportShapefileConcave(File outputFileName, ArrayList<double[]> border, int id){

        //ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        Geometry outShpGeom2 = new Geometry(ogr.wkbLinearRing);
        Geometry outShpGeom = new Geometry(ogr.wkbPolygon);

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        DataSource outShp = shpDriver.CreateDataSource(outputFileName.getAbsolutePath());

        Layer outShpLayer = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id",0);

        outShpLayer.CreateField(layerFieldDef);
        FeatureDefn outShpFeatDefn = outShpLayer.GetLayerDefn();
        Feature outShpFeat = new Feature(outShpFeatDefn);

        for(int i = 0; i < border.size(); i++) {

            //System.out.println("Wrote " + i);
            outShpGeom2.AddPoint_2D(border.get(i)[0], border.get(i)[1]);
            //System.out.println(border.get(i)[0] + " " + border.get(i)[1]);
        }

        outShpGeom.AddGeometry(outShpGeom2);

        outShpFeat.SetField("id",id);

        outShpFeat.SetGeometryDirectly(outShpGeom);
        outShpLayer.CreateFeature(outShpFeat);
        outShpLayer.SyncToDisk();
        System.out.println("features: " + outShpLayer.GetFeatureCount());



    }

    public void printFailedFiles(){
        System.out.println("Failed files: ");
        for(int i = 0; i < failedFiles.size(); i++){
            System.out.println(failedFiles.get(i) + " " + Arrays.toString(failedFilesProps.get(i)));
        }

        System.out.println("processed stands " + processedStands.size());

    }
    public static boolean canDrawPerpendicular(double x, double y, double[] lineStart, double[] lineEnd) {
        double x1 = lineStart[0];
        double y1 = lineStart[1];
        double x2 = lineEnd[0];
        double y2 = lineEnd[1];

        // Calculate the squared length of the line segment
        double lineLengthSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);

        // Check for degenerate line segment (i.e. zero length)
        if (lineLengthSquared == 0) {
            return x == x1 && y == y1;
        }

        // Calculate the dot product of the vector from the line start to the point with the vector from the line start to the line end
        double dotProduct = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / lineLengthSquared;

        // Calculate the coordinates of the projected point on the line
        double projectedX = x1 + dotProduct * (x2 - x1);
        double projectedY = y1 + dotProduct * (y2 - y1);

        // Check if the projected point lies on the line segment
        return projectedX >= Math.min(x1, x2) && projectedX <= Math.max(x1, x2)
                && projectedY >= Math.min(y1, y2) && projectedY <= Math.max(y1, y2);
    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }
    public void writeTrees(ArrayList<Tree> trees, File ofile) throws IOException {
        ofile.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));

        bw.write("fileName id key insideStand dToStand species dbh x y vtukki vkuitu vpikkutukki venergia vunknown" + "\n");
        for(int i = 0; i < trees.size(); i++){
            bw.write(trees.get(i).HPR_FILE_NAME + " " + trees.get(i).getId() + " " + trees.get(i).getKey() + " " + trees.get(i).trulyInStand + " " + trees.get(i).distanceToStandBorder + " " + + trees.get(i).species + " " + trees.get(i).diameter + " " +  trees.get(i).getX_coordinate_machine() + " " + trees.get(i).getY_coordinate_machine() +
                    " " + trees.get(i).volume_tukki + " " + trees.get(i).volume_kuitu + " " + trees.get(i).volume_pikkutukki + " " + trees.get(i).volume_energia +
                    " " + trees.get(i).volume_unknown  + "\n");
        }

        bw.close();
    }

    public void writeTreesEstimatedLocations(ArrayList<Tree> trees, File ofile) throws IOException {
        ofile.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(ofile));

        bw.write("fileName id key insideStand dToStand species dbh x y vtukki vkuitu vpikkutukki venergia vunknown" + "\n");
        for(int i = 0; i < trees.size(); i++){
            bw.write(trees.get(i).HPR_FILE_NAME + " " + trees.get(i).getId() + " " + trees.get(i).standId + " " + trees.get(i).trulyInStand + " " + trees.get(i).distanceToStandBorder + " " + + trees.get(i).species + " " + trees.get(i).diameter + " " +  trees.get(i).getX_coordinate_estimated() + " " + trees.get(i).getY_coordinate_estimated() +
                    " " + trees.get(i).volume_tukki + " " + trees.get(i).volume_kuitu + " " + trees.get(i).volume_pikkutukki + " " + trees.get(i).volume_energia +
                    " " + trees.get(i).volume_unknown  + "\n");
        }

        bw.close();
    }

    public static List<RealVector> filterPoints(List<RealVector> points) {
        List<RealVector> filteredPoints = new ArrayList<>();

        KalmanFilter filter = new KalmanFilter();

        for (RealVector point : points) {

            filter.predict();

            // Convert 2D point to measurement vector
            RealVector measurement = new ArrayRealVector(new double[] {point.getEntry(0), point.getEntry(1)});
            filter.update(measurement);

            // Extract position and velocity components of state estimate vector
            RealMatrix stateEstimateMatrix = filter.getState();
            RealVector stateEstimate = stateEstimateMatrix.getColumnVector(0);
            RealVector position = stateEstimate.getSubVector(0, 2);

            // Add the filtered point to the list of filtered points
            filteredPoints.add(position);
        }

        return filteredPoints;
    }

    public static boolean stringContainsSubstringIgnoreCase(String str, String substr) {
        return str.toLowerCase().contains(substr.toLowerCase());
    }

    private static void printElement(Element element, BufferedWriter bw) throws IOException {
        //System.out.println("Element: " + element.getName());
        bw.write("Element: " + element.getName() + "\n");
        // Print the attributes of the element, if any
        for (int i = 0; i < element.getAttributes().size(); i++) {
            bw.write("Attribute: " + element.getAttributes().get(i).getName() + " = " +
                    element.getAttributes().get(i).getValue() + "\n");
        }

        // Print the text content of the element, if any
        if (!element.getTextTrim().equals("")) {
            bw.write("Value: " + element.getTextTrim() + "\n");
        }

        // Recursively print the contents of all child elements
        for (int i = 0; i < element.getChildren().size(); i++) {
            printElement(element.getChildren().get(i), bw);
        }
    }

    public static double distanceToPolygon(double[] p, double[][] poly) {
        double minDist = Double.POSITIVE_INFINITY;
        int n = poly.length;

        for (int i = 0; i < n; i++) {
            double[] a = poly[i];
            double[] b = poly[(i + 1) % n];
            double dist = distanceToSegment(p, a, b);
            minDist = Math.min(minDist, dist);
        }

        return minDist;
    }

    public static double distanceToSegment(double[] p, double[] a, double[] b) {
        double[] v = new double[] { b[0] - a[0], b[1] - a[1] };
        double[] w = new double[] { p[0] - a[0], p[1] - a[1] };

        double c1 = dot(w, v);
        if (c1 <= 0) {
            return distance(p, a);
        }

        double c2 = dot(v, v);
        if (c2 <= c1) {
            return distance(p, b);
        }

        double b1 = c1 / c2;
        double[] pb = new double[] { a[0] + b1 * v[0], a[1] + b1 * v[1] };
        return distance(p, pb);
    }

    public static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1];
    }

    public static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        return Math.sqrt(dx * dx + dy * dy);
    }


}

class dynamicStatistics{

    private ArrayList<Double> values = new ArrayList<>();
    private double max_rolling_stats;
    private double min_rolling_stats;
    private double average_rolling_stats;
    private double pwrSumAvg_rolling_stats;
    private double stdDev_rolling_stats;
    private int count_rolling_stats;

    public dynamicStatistics(){

    }

    public void add(double val){

        values.add(val);
        if(val > max_rolling_stats)
            max_rolling_stats = val;

        if(val < min_rolling_stats)
            min_rolling_stats = val;

        count_rolling_stats++;
        average_rolling_stats += (val - average_rolling_stats) / count_rolling_stats;
        pwrSumAvg_rolling_stats += (val * val - pwrSumAvg_rolling_stats) / count_rolling_stats;
        stdDev_rolling_stats = Math.sqrt((pwrSumAvg_rolling_stats * count_rolling_stats - count_rolling_stats * average_rolling_stats * average_rolling_stats) / (count_rolling_stats - 1));
    }

    public int getNumValues(){
        return count_rolling_stats;
    }
    public void removeLast(){

        double val = values.get(values.size()-1);
        values.remove(values.size()-1);
        if(val == max_rolling_stats){
            max_rolling_stats = 0;
            for(int i = 0; i < values.size(); i++){
                if(values.get(i) > max_rolling_stats)
                    max_rolling_stats = values.get(i);
            }
        }
        if(val == min_rolling_stats){
            min_rolling_stats = 0;
            for(int i = 0; i < values.size(); i++){
                if(values.get(i) < min_rolling_stats)
                    min_rolling_stats = values.get(i);
            }
        }
        count_rolling_stats--;
        average_rolling_stats -= (val - average_rolling_stats) / count_rolling_stats;
        pwrSumAvg_rolling_stats -= (val * val - pwrSumAvg_rolling_stats) / count_rolling_stats;
        stdDev_rolling_stats = Math.sqrt((pwrSumAvg_rolling_stats * count_rolling_stats - count_rolling_stats * average_rolling_stats * average_rolling_stats) / (count_rolling_stats - 1));
    }





    public double getMean(){
        return average_rolling_stats;
    }

    public double getStdDev(){
        return stdDev_rolling_stats;
    }

    public void reset(){
        max_rolling_stats = 0;
        min_rolling_stats = 0;
        average_rolling_stats = 0;
        pwrSumAvg_rolling_stats = 0;
        stdDev_rolling_stats = 0;
        count_rolling_stats = 0;

        values.clear();

    }
}



class KalmanFilter {
    private RealMatrix A;  // state transition matrix
    private RealMatrix B;  // control input matrix
    private RealMatrix H;  // measurement matrix
    private RealMatrix Q;  // process noise covariance matrix
    private RealMatrix R;  // measurement noise covariance matrix
    private RealMatrix P;  // state estimate covariance matrix
    private RealMatrix x;  // state vector

    public KalmanFilter() {
        // Initialize matrices
        A = new Array2DRowRealMatrix(new double[][] {{1, 0, 1, 0}, {0, 1, 0, 1}, {0, 0, 1, 0}, {0, 0, 0, 1}});
        B = new Array2DRowRealMatrix(new double[][] {{0}, {0}, {0}, {0}});
        H = new Array2DRowRealMatrix(new double[][] {{1, 0, 0, 0}, {0, 1, 0, 0}});
        Q = new Array2DRowRealMatrix(new double[][] {{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}});
        R = new Array2DRowRealMatrix(new double[][] {{5, 0}, {0, 5}});
        P = new Array2DRowRealMatrix(new double[][] {{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}});
        x = new Array2DRowRealMatrix(new double[][] {{0}, {0}, {0}, {0}});
    }

    public void predict() {
        // Predict the state vector
        x = A.multiply(x).add(B);
        // Predict the state estimate covariance matrix
        P = A.multiply(P).multiply(A.transpose()).add(Q);
    }

    public void update(RealVector z) {
        RealMatrix zMatrix = new Array2DRowRealMatrix(new double[][]{{z.getEntry(0)}, {z.getEntry(1)}});
        RealMatrix S = H.multiply(P).multiply(H.transpose()).add(R);
        RealMatrix K = P.multiply(H.transpose()).multiply(new LUDecomposition(S).getSolver().getInverse());
        x = x.add(K.multiply(zMatrix.subtract(H.multiply(x))));
        P = P.subtract(K.multiply(H).multiply(P));
    }

    public RealMatrix getState() {
        return x;
    }
}


class stemChecker {

    HashSet<Integer> pines = new HashSet<>();
    HashSet<Integer> spruces = new HashSet<>();
    HashSet<Integer> birches = new HashSet<>();

    HashSet<Integer> other = new HashSet<>();

    HashSet<Integer> pineKuitu = new HashSet<>();
    HashSet<Integer> spruceKuitu = new HashSet<>();
    HashSet<Integer> birchKuitu = new HashSet<>();
    HashSet<Integer> otherKuitu = new HashSet<>();


    HashSet<Integer> pineTukki = new HashSet<>();
    HashSet<Integer> spruceTukki = new HashSet<>();
    HashSet<Integer> birchTukki = new HashSet<>();
    HashSet<Integer> otherTukki = new HashSet<>();

    HashSet<Integer> kuitu = new HashSet<>();
    HashSet<Integer> tukki = new HashSet<>();
    HashSet<Integer> energia = new HashSet<>();
    HashSet<Integer> pikkutukki = new HashSet<>();
    HashSet<Integer> muuPuutavaraLaji = new HashSet<>();

    public void addPine(int value){
        pines.add(value);
    }
    public void addSpruce(int value){
        spruces.add(value);
    }
    public void addBirch(int value){
        birches.add(value);
    }
    public void addOther(int value){
        other.add(value);
    }

    public void addKuitu(int value){
        kuitu.add(value);
    }
    public void addTukki(int value){
        tukki.add(value);
    }
    public void addEnergia(int value){
        energia.add(value);
    }
    public void addPikkutukki(int value){
        pikkutukki.add(value);
    }
    public void addMuuPuutavaraLaji(int value){
        muuPuutavaraLaji.add(value);
    }

    public int getSpecies(int value){

        if(pines.contains(value)){
            return 1;
        }
        if(spruces.contains(value)){
            return 2;
        }
        if(birches.contains(value)){
            return 3;
        }
        if(other.contains(value)){
            return 4;
        }

        System.out.println("Error: no species found for value: " + value);
        System.exit(1);
        return -1;
    }

    public int getPuutavaraLaji(int value){
        if(kuitu.contains(value)){
            return 1;
        }
        if(tukki.contains(value)){
            return 2;
        }
        if(energia.contains(value)){
            return 3;
        }
        if(pikkutukki.contains(value)){
            return 4;
        }

        System.out.println("Error: no puutavaralaji found for value: " + value);

        return -1;
    }


}


class Stand{

    KdTree forest = new KdTree();
    createCHM.chm canopy_height_model = null;



};

