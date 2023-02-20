package utils;

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
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import tools.createCHM;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Stanford2010 {

    File xml_file;



    public Stanford2010(){

    }

    public void setXMLfile(File file){

        this.xml_file = file;

    }

    public void parse(){

        SAXBuilder builder = new SAXBuilder();

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
                //readElements(root, trees);

                bw.close();
            }catch (Exception e){
                e.printStackTrace();
            }

        double[] transformed = null;

        ArrayList<Tree> trees = new ArrayList<Tree>();
        List<Element> stems = null;

        for(int i = 0; i < machines.size(); i++){

            stems = machines.get(i).getChildren("Stem", ns);

            System.out.println(stems.size());

            for(int s = 0; s < stems.size(); s++){

                Tree tempTree = new Tree();

                Element stem = stems.get(s);

                List<Element> stem_coordinates = stem.getChildren("StemCoordinates", ns);

                System.out.println(Arrays.toString(stem_coordinates.toArray()));
                double latitude = Double.parseDouble(stem_coordinates.get(0).getChild("Latitude", ns).getValue());
                double longitude = Double.parseDouble(stem_coordinates.get(0).getChild("Longitude", ns).getValue());
                double altitude = Double.parseDouble(stem_coordinates.get(0).getChild("Altitude", ns).getValue());

                System.out.println(latitude + " " + longitude);
                try {
                    transformed = op.transform(new double[]{longitude, latitude});
                }catch (Exception e){
                    e.printStackTrace();
                }

                System.out.println(Arrays.toString(transformed));

                tempTree.setX_coordinate_machine(transformed[0]);
                tempTree.setY_coordinate_machine(transformed[1]);

                int stemNumber = Integer.parseInt(stem.getChild("StemNumber", ns).getValue());
                tempTree.setId(stemNumber);
                trees.add(tempTree);

                if(true)
                    continue;

                Element stemInfo = stem.getChild("Extension", ns).getChild("HPRCMResults", ns).getChild("StemInfo", ns);

                float treeHeight = Float.parseFloat(stemInfo.getChild("TreeHeight",ns).getValue());

                tempTree.setHeight(treeHeight);

                tempTree.setBoomPosition(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("BoomAngle", ns).getValue()));
                tempTree.setBoomExtension(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("BoomExtension", ns).getValue()));
                tempTree.setMachineBearing(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("MachineBearing", ns).getValue()));
                tempTree.setDiameter(Float.parseFloat(stem.getChild("SingleTreeProcessedStem", ns).getChild("DBH", ns).getValue()));

                System.out.println(tempTree);
                System.exit(1);
            }
        }

        if(trees.size() != stems.size()){
            throw new toolException("Trees and stems are not the same size. Something went wrong.");
        }

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
        ofile = new File("/home/koomikko/Documents/customer_work/metsahallitus/trees_filtered.txt");


        try {
            ofile.createNewFile();
            writeTrees(trees2, ofile);
        }catch (Exception e){
            e.printStackTrace();
        }




        System.exit(1);



        for(int i = 0; i < lista.size(); i++){

            List<Element> lista_ = lista.get(i).getChildren();

            System.out.println(lista.get(i).getName() + " has " + lista_.size() + " books.");
            System.out.println(Arrays.toString(lista_.toArray()));
            System.out.println(lista_.get(lista_.size()-1).getChildren());
            List<Element> lista__ = lista.get(i).getChildren("Stem", ns);
            System.out.println("Number of stems: " + lista__.size());

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

        for(int i = 0; i < trees.size(); i++){
            bw.write(trees.get(i).getX_coordinate_machine() + " " + trees.get(i).getY_coordinate_machine() + " " + trees.get(i).getId() + " " + trees.get(i).getKey() + "\n");
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


class Tree {

    public float height, diameter;
    public double x_coordinate_machine, y_coordinate_machine, z_coordinate_machine;
    public double x_coordinate_estimated, y_coordinate_estimated, z_coordinate_estimated;
    public int species, key, id;

    //
    public double boomAngle, boomPosition, boomExtension, machineBearing;
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

