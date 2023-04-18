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
import tools.ConcaveHull;
import tools.ConvexHull_arraylist;
import tools.Point;
import tools.QuickHull;
import tools.createCHM;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import static tools.Boundary.angleBetween;
import static tools.ConcaveHull.calculateConcaveHull;

public class Stanford2010 {


    int concave_hull_k = 55;
    ArrayList<String> failedFiles = new ArrayList<>();
    ArrayList<int[]> failedFilesProps = new ArrayList<>();
    File xml_file;
    argumentReader aR;

    public double maxDistanceFromStandBorder = 25;
    HashMap<Integer, double[][]> standBounds = new HashMap<>();
    HashMap<Integer, double[]> standCentroids = new HashMap<>();

    KdTree centroids = new KdTree();

    Layer allStandsShapeFileLayer;

    public Stanford2010(argumentReader aR) throws toolException {
        this.aR = aR;
        this.declareMergedShapefile();
    }



    public void setXMLfile(File file){

        this.xml_file = file;

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

    public void finalizeMergedShapefile(){

        allStandsShapeFileLayer.SyncToDisk();

    }

    public void exportShapefile(File outputFileName, ArrayList<tools.ConcaveHull.Point> border, int id){

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
        Geometry bufferedShapefile = outShpGeom.Buffer(5);

        outShpFeat.SetField("id",id);

        outShpFeat.SetGeometryDirectly(bufferedShapefile);
        outShpLayer.CreateFeature(outShpFeat);
        outShpLayer.SyncToDisk();
        System.out.println("features: " + outShpLayer.GetFeatureCount());

        allStandsShapeFileLayer.CreateFeature(outShpFeat);

    }


    public void readShapeFiles(String shapeFile) throws IOException {

        DataSource ds = ogr.Open( shapeFile );
        //DataSource ds2 = ogr.Open( shapeFile2 );

        if( ds == null ) {
            System.out.println( "Opening stand shapefile failed." );
            System.exit( 1 );
        }

        Layer shapeFileLayer = ds.GetLayer(0);

        int shapeId = 0;

        for(long i = 0; i < shapeFileLayer.GetFeatureCount(); i++ ) {

            if( i % 3 == 0 || true) {

                Feature tempF = shapeFileLayer.GetFeature(i);
                Geometry tempG = tempF.GetGeometryRef();
                //System.out.println(tempG.GetGeometryName());
// check if geometry is a MultiPolygon
                if (tempG.GetGeometryName().equals("MULTIPOLYGON")) {
                    //System.out.println("here1 " + tempF.GetFieldAsInteger(0));
                    int numGeom = tempG.GetGeometryCount();

                    for (int j = 0; j < numGeom; j++) {

                        shapeId++;
                        Geometry tempG2 = tempG.GetGeometryRef(j).GetGeometryRef(0);
                        //System.out.println("here2 " + tempF.GetFieldAsInteger(0));

                        //System.out.println(tempG2.GetGeometryName());

                        if (tempG2.GetGeometryName().equals("LINEARRING")) {

                            if (tempG2 == null || tempG2.GetPoints() == null) {
                                continue;
                            }


                            double[] centroid = tempG.GetGeometryRef(j).Centroid().GetPoint(0);
                            //System.out.println("here3 " + tempF.GetFieldAsInteger(0));
                            standBounds.put(shapeId, clone2DArray(tempG2.GetPoints()));
                            //System.out.println("here4 " + tempF.GetFieldAsInteger(0));
                            standCentroids.put(shapeId, new double[]{centroid[0], centroid[1]});
                            //System.out.println("here5 " + tempF.GetFieldAsInteger(0));
                            centroids.add(new KdTree.XYZPoint(centroid[0], centroid[1], 0, shapeId));
                            System.out.println("centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);
                            //System.out.println("here6 " + tempF.GetFieldAsInteger(0));
                        }
                    }
                } else if (tempG.GetGeometryName().equals("POLYGON")) {
                    Geometry tempG2 = tempG.GetGeometryRef(0);


                    //System.out.println(tempG2.GetPointCount() + " " + tempG2.GetGeometryName().equals("LINEARRING"));


                    if (tempG2.GetGeometryName().equals("LINEARRING")) {

                        shapeId++;
                        if (tempG2 == null || tempG2.GetPoints() == null) {
                            continue;
                        }
                        double[] centroid = tempG.Centroid().GetPoint(0);
                        standBounds.put(shapeId, clone2DArray(tempG2.GetPoints()));
                        standCentroids.put(shapeId, new double[]{centroid[0], centroid[1]});
                        centroids.add(new KdTree.XYZPoint(centroid[0], centroid[1], 0, shapeId));
                        //System.out.println("centroid: " + centroid[0] + " " + centroid[1] + " " + shapeId);
                    }
                } else {
                    // handle other types of geometries if needed
                }

                //if(tempF.GetFieldAsInteger(0) == 199){
                //    System.exit(1);
                //}

            }
        }

        System.out.println(standBounds.size() + " stand bounds read.");
        System.out.println(standCentroids.size() + " stand centroids read.");

        //ystem.exit(1);
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

    public static void print2DArray(double[][] arr) {

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                System.out.print(arr[i][j] + " ");
            }
            System.out.println();
        }
    }

    public int treeInPolygons(double[] tree, HashMap<Integer, double[][]> polygons) {

        for (Map.Entry<Integer, double[][]> entry : polygons.entrySet()) {

            if(pointInPolygon(entry.getValue(), tree)) {
                return entry.getKey();
            }

        }

        return -1;
    }

    public void parse() throws IOException{

        SAXBuilder builder = new SAXBuilder();

        System.out.println("Parsing XML file: " + this.xml_file.getAbsolutePath());
        Document xml = null;
        try {
            xml = builder.build(this.xml_file);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }



        Element root = xml.getRootElement();


        File ofile =new File("/home/koomikko/Documents/customer_work/metsahallitus/parsed.hpr");

        //System.exit(1);
        Namespace ns = root.getNamespace();

        System.out.println("Root element of XML document is : " + root.getName());
        System.out.println("Number of books in this XML : " + root.getChildren().size());


        List<Element> lista =  root.getChildren();

        List<Element> machines = root.getChildren("Machine", ns);


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

                //System.out.println("insideStand: " + insideStand);

                List<Element> stemInfo = stem.getChild("SingleTreeProcessedStem", ns).getChildren("Log", ns);
                double dbh = Double.parseDouble(stem.getChild("SingleTreeProcessedStem", ns).getChild("DBH", ns).getValue());
                tempTree.setDiameter((float)dbh);
                //System.out.println("logs: " + stemInfo.size());

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

                   if(puutavaralaji == 1){
                       tempTree.volume_kuitu += volume;

                       Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                       double loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                       double logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                       double logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());

                       knownKuitu.add(new double[]{Double.parseDouble(stemInfo.get(i_).getChildren("LogKey", ns).get(0).getValue()),
                               stemInfo.size(),
                               Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue()),
                               loglength,logdiameterMid, logdiameterEnd
                       });


                   }
                   if(puutavaralaji == 2){
                       tempTree.volume_tukki += volume;

                       Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                       double loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                       double logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                       double logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());

                       knownTukki.add(new double[]{Double.parseDouble(stemInfo.get(i_).getChildren("LogKey", ns).get(0).getValue()),
                               stemInfo.size(),
                               Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue()),
                               loglength,logdiameterMid, logdiameterEnd
                       });

                       //System.out.println(Arrays.toString(unknown.get(unknown.size()-1)));

                   }
                   if(puutavaralaji == 3){
                       tempTree.volume_energia += volume;
                   }
                   if(puutavaralaji == 4){
                       tempTree.volume_pikkutukki += volume;
                   }
                   if(puutavaralaji == -1){

                       tempTree.volume_unknown += volume;

                       Element stemInfo_ = stemInfo.get(i_).getChild("LogMeasurement", ns);

                       double loglength = Double.parseDouble(stemInfo_.getChild("LogLength", ns).getValue());
                       double logdiameterMid = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(0).getValue());
                       double logdiameterEnd = Double.parseDouble(stemInfo_.getChildren("LogDiameter", ns).get(3).getValue());

                       unknown.add(new double[]{Double.parseDouble(stemInfo.get(i_).getChildren("LogKey", ns).get(0).getValue()),
                               stemInfo.size(),
                               Double.parseDouble(stemInfo.get(i_).getChildren("LogVolume", ns).get(0).getValue()),
                               loglength,logdiameterMid, logdiameterEnd
                                });

                        //System.out.println(Arrays.toString(unknown.get(unknown.size()-1)));
                   }

                }

                trees.add(tempTree);


                allTrees.put(tempTree.getId(), tempTree);

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

        HashMap<Integer, ArrayList<ConcaveHull.Point>> hulls = new HashMap<>();
        HashMap<Integer, ArrayList<Point>> hulls_convex = new HashMap<>();

        QuickHull quickHull = new QuickHull();

        for(int i : standTreeLocations.keySet()){

            ArrayList<tools.ConcaveHull.Point> currentTrees = standTreeLocations.get(i);
            ArrayList<ConcaveHull.Point> hull = calculateConcaveHull(currentTrees, concave_hull_k);
            //ConvexHull_arraylist convexHull = new ConvexHull_arraylist(standTreeLocations_convex.get(i));
            //convexHull.computeConvexHull();
            //ArrayList<Point> convexHullPoints = convexHull.getConvexHull();
            ArrayList<Point> convexHullPoints = quickHull.quickHull(standTreeLocations_convex.get(i));
            hulls_convex.put(i, convexHullPoints);
            hulls.put(i, hull);
            System.out.println("Hull size: " + hull.size());

        }


        System.out.println(standTreeLocations.size());

        String pathSeparator = System.getProperty("file.separator");

        System.out.println("pathSeparator: " + pathSeparator);
        //System.exit(1);

        //System.out.println("xml: " + Arrays.toString(xml_file.getName().split(("\\."))));
        File output_directory = new File(aR.odir + pathSeparator + xml_file.getName().split("\\.")[0]);


        System.out.println("output_directory: " + output_directory.getAbsolutePath());
        output_directory.mkdirs();



        for(int i : standTreeLocations.keySet()){

            /*
            LASReader templateLas = new LASReader(new File("/home/koomikko/Documents/customer_work/metsahallitus/out.las"));

            File tmpFile2 = new File(output_directory.getAbsolutePath() + pathSeparator + "tmp.las");
            File tmpFile22 = new File(output_directory.getAbsolutePath() + pathSeparator + "tmp.lasx");

            if(tmpFile2.exists()){
                tmpFile2.delete();
            }
            if(tmpFile22.exists()){
                tmpFile22.delete();
            }

            pointWriterMultiThread pw = new pointWriterMultiThread(tmpFile2, templateLas, "haha", aR);

            LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
            //int thread_n = aR.pfac.addReadThread(in);
            int thread_n = aR.pfac.addReadThread(templateLas);
            aR.pfac.addWriteThread(thread_n, pw, buf);

            LasPoint tempPoint = new LasPoint();
            templateLas.readRecord(1, tempPoint);

            for(int i_ = 0; i_ < standTreeLocations.get(i).size(); i_++){

                tempPoint.x = standTreeLocations.get(i).get(i_).getX();
                tempPoint.y = standTreeLocations.get(i).get(i_).getY();

                try {

                    aR.pfac.writePoint(tempPoint, i + i_, thread_n);

                } catch (Exception e) {
                    e.printStackTrace();

                }
            }

            aR.pfac.closeThread(thread_n);
            ArrayList<double[]> concave = null;

            try{
                //concave = makeConcave(new LASReader(tmpFile2));
            }catch(Exception e){
                e.printStackTrace();
            }
            System.out.println("Stand " + i + " size: " + standTreeLocations.get(i).size());
*/
            File tmpFile = new File(output_directory.getAbsolutePath() + pathSeparator + "stand_" + i + "_boundary.shp");
            exportShapefile(tmpFile, hulls.get(i), i);
            //exportShapefileConcave(tmpFile, concave, i);
            //exportShapefileConvex(tmpFile, hulls_convex.get(i), i);
        }


        //System.exit(1);

        //aR.pfac.closeThread(thread_n);



        //System.exit(1);


        dynamicStatistics stats_x = new dynamicStatistics();
        dynamicStatistics stats_y = new dynamicStatistics();

        ArrayList<Tree> trees2 = new ArrayList<Tree>();

        ArrayList<int[]> trackTreeIds = new ArrayList<int[]>();

        double maxDistanceFromAchrorTree = 5.0;

        Tree anchorTree = new Tree();

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
                    tmpTree.setId(trackID);
                    tmpTree.setKey(counter++);
                    trees2.add(tmpTree);
                    anchorTree = trees.get(i);

                    if (distance2 > trackChangeDistance) {
                        trackID++;
                    }
                }
            }
        }

        System.out.println(trees2.size() + " " + trees.size());

        double[][] points = new double[trees.size()][2];

        for(int i = 0; i < trees.size(); i++){
            points[i][0] = trees.get(i).getX_coordinate_machine();
            points[i][1] = trees.get(i).getY_coordinate_machine();
        }


        double eps = 30.0;
        int minPts = 8;

        DBSCAN dbscan = new DBSCAN(eps, minPts);

        List<Set<Integer>> clusters = dbscan.performDBSCAN(points);

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


        ofile = new File(output_directory.getAbsolutePath() + pathSeparator + "trees_filtered.txt");
        //ofile = new File("/home/koomikko/Documents/customer_work/metsahallitus/trees_filtered.txt");


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
            writeTrees(trees, ofile);
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

                int p = pointCloud.fastReadFromQuery(tempPoint2);
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

class Tree {

    public tools.ConcaveHull.Point point = new tools.ConcaveHull.Point(0d,0d);
    public float height, diameter;
    public double x_coordinate_machine, y_coordinate_machine, z_coordinate_machine;
    public double x_coordinate_estimated, y_coordinate_estimated, z_coordinate_estimated;
    public int species, key, id;

    public double volume = 0, volume_kuitu = 0, volume_tukki = 0, volume_energia = 0, volume_pikkutukki = 0, volume_unknown = 0;

    public double distanceToStandBorder = -1;
    public int standId = -1;
    public boolean trulyInStand = true;
    //
    public double boomAngle, boomPosition, boomExtension, machineBearing;

    public String HPR_FILE_NAME = "";

    public Tree(){

    }

    public Tree(float height, float diameter, int species){

        this.height = height;
        this.species = species;
        this.species = species;

    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getDiameter() {
        return diameter;
    }

    public void setDiameter(float diameter) {
        this.diameter = diameter;
    }

    public double getX_coordinate_machine() {
        return x_coordinate_machine;
    }

    public void setX_coordinate_machine(double x_coordinate_machine) {
        this.x_coordinate_machine = x_coordinate_machine;
    }

    public double getY_coordinate_machine() {
        return y_coordinate_machine;
    }

    public void setY_coordinate_machine(double y_coordinate_machine) {
        this.y_coordinate_machine = y_coordinate_machine;
    }

    public void setPoint(){
        this.point = new tools.ConcaveHull.Point(x_coordinate_machine, y_coordinate_machine);
        //System.out.println("point: " + this.point.toString());
    }

    public tools.ConcaveHull.Point getPoint(){
        return this.point;
    }

    public double getZ_coordinate_machine() {
        return z_coordinate_machine;
    }

    public void setZ_coordinate_machine(double z_coordinate_machine) {
        this.z_coordinate_machine = z_coordinate_machine;
    }

    public int getSpecies() {
        return species;
    }

    public void setSpecies(int species) {
        this.species = species;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getX_coordinate_estimated() {
        return x_coordinate_estimated;
    }

    public void setX_coordinate_estimated(double x_coordinate_estimated) {
        this.x_coordinate_estimated = x_coordinate_estimated;
    }

    public double getY_coordinate_estimated() {
        return y_coordinate_estimated;
    }

    public void setY_coordinate_estimated(double y_coordinate_estimated) {
        this.y_coordinate_estimated = y_coordinate_estimated;
    }

    public double getZ_coordinate_estimated() {
        return z_coordinate_estimated;
    }

    public void setZ_coordinate_estimated(double z_coordinate_estimated) {
        this.z_coordinate_estimated = z_coordinate_estimated;
    }

    public double getBoomAngle() {
        return boomAngle;
    }

    public void setBoomAngle(double boomAngle) {
        this.boomAngle = boomAngle;
    }

    public double getBoomPosition() {
        return boomPosition;
    }

    public void setBoomPosition(double boomPosition) {
        this.boomPosition = boomPosition;
    }

    public double getBoomExtension() {
        return boomExtension;
    }

    public void setBoomExtension(double boomExtension) {
        this.boomExtension = boomExtension;
    }

    public double getMachineBearing() {
        return machineBearing;
    }

    public void setMachineBearing(double machineBearing) {
        this.machineBearing = machineBearing;
    }



    @Override
    public String toString() {
        return "Tree{" +
                "height=" + height +
                ", diameter=" + diameter +
                ", x_coordinate_machine=" + x_coordinate_machine +
                ", y_coordinate_machine=" + y_coordinate_machine +
                ", z_coordinate_machine=" + z_coordinate_machine +
                ", x_coordinate_estimated=" + x_coordinate_estimated +
                ", y_coordinate_estimated=" + y_coordinate_estimated +
                ", z_coordinate_estimated=" + z_coordinate_estimated +
                ", species=" + species +
                ", key=" + key +
                ", id=" + id +
                ", boomAngle=" + boomAngle +
                ", boomPosition=" + boomPosition +
                ", boomExtension=" + boomExtension +
                ", machineBearing=" + machineBearing +
                '}';
    }
};

class Stand{

    KdTree forest = new KdTree();
    createCHM.chm canopy_height_model = null;



};

