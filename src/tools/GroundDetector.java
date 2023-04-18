package tools;

import LASio.*;
//import jdk.jfr.events.ExceptionThrownEvent;
import err.toolException;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.gdal.ogr.Geometry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.tinfour.common.*;

import org.tinfour.common.Vertex;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.interpolation.VertexValuatorDefault;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.utils.Polyside;
import quickhull3d.Vector3d;
import utils.*;

import java.io.*;
import java.util.*;
import java.util.stream.StreamSupport;

import static org.gdal.ogr.ogrConstants.wkbLinearRing;
import static org.tinfour.utils.Polyside.isPointInPolygon;


/**
 *  Detects ground points from LAS file and classifies
 *  them with class 2 (ASPRS format). Classification
 *  is based on a modified Axelsson (2000) algorithm.
 *
 *  Also includes a method to normalize z values to TIN
 *  generated from ground points.
 *
 * @author  Kukkonen Mikko
 * @version 0.1
 * @since 06.03.2018
 */


public class GroundDetector{


    rolling_stats rolling_statistics = new rolling_stats();
    rolling_stats rolling_statistics_distance = new rolling_stats();

    boolean dynamic_angle_threshold = true;

    int count_rolling_stats = 0;
    double average_rolling_stats = 0;
    double pwrSumAvg_rolling_stats = 0;
    double stdDev_rolling_stats = 0; // Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));
    double max_rolling_stats = Double.NEGATIVE_INFINITY;
    double min_rolling_stats = Double.POSITIVE_INFINITY;




    int condition2 = 0;
    org.tinfour.common.Circumcircle CC = new Circumcircle();

    fileOperations fo = new fileOperations();
    IncrementalTin tin = new IncrementalTin();

    IncrementalTin tin2 = new IncrementalTin();


    double inside_x, inside_y, inside_dim, outside_x, outside_y, outside_dim;

    LASReader pointCloud = null;

    double expansion, expansion_orig;

    String oparse = "all";

    int method = 1;

    int kernel = 1;

    int axelssonGridSize = 20;

    double distanceThreshold = 0.75;
    double angleThreshold = 10.0;

    long numberOfGroundPoints = 0;
    long foundGroundPoints = 0;

    long seedPoints = 0;

    HashSet<Integer> doneIndexes = new HashSet<Integer>();
    boolean[] doneInd;
    boolean[] badInd;

    HashSet<Integer> groundPointIndexes = new HashSet<Integer>();

    ArrayList<LasPoint> surfaceNormalPoints = new ArrayList<LasPoint>();

    VertexValuatorDefault valuator = new VertexValuatorDefault();

    PointInclusionRule rule = new PointInclusionRule(true);

    double miniX;
    double maxiX;
    double miniY;
    double maxiY;

    File outWriteFile;

    String outputFileName = "";
    String odir;

    String pathSep = System.getProperty("file.separator");

    boolean print = false;

    boolean write = false;

    boolean fixedAngle = false;

    HashSet<Integer> seedPointIndexes = new HashSet<Integer>();
    HashMap<Integer, Vertex> seedPointVertices = new HashMap<Integer, org.tinfour.common.Vertex>();

    argumentReader aR;

    File groundPointFile;

    int progress_current = 0;
    int progress_end = 0;

    int pointsOutsideTin = 0;

    int coreNumber = 0;


    public GroundDetector(LASReader pointCloud2) throws IOException {

        this.pointCloud = pointCloud2;

        this.miniY = pointCloud.getMinY();
        this.maxiY = pointCloud.getMaxY();
        this.miniX = pointCloud.getMinX();
        this.maxiX = pointCloud.getMaxX();


        fixedAngle = true;
    }

    public GroundDetector() throws IOException{


    }


    public void wipe(){

        this.tin.clear();
        this.tin = null;
        this.tin2.clear();
        this.tin2 = null;
        aR.gc();
        //System.gc();

    }

    /**
     * Constructor
     *
     * This is an implementation of Axelsson (2000) incremental TIN
     * generation algorithm.
     *
     * @param pointCloud2 		Input point cloud
     * @param print2 			Print TIN or nor
     * @param odir2 				Output directory
     * @param rule2				Rule to either include or exclude a point
     *							Also includes a pointModify class that can
     *							modify the point attributes.
     * @param angleThreshold1 	Maximum angle between the point and a TIM point
     * @param distanceThreshold1 Maximum distance between the point and TIN facet.
     * @param axelssonGridSize1 	Side length of the search window for initial
     *							ground points.
     */

    public GroundDetector(LASReader pointCloud2, boolean print2, String outputFile, String odir2, PointInclusionRule rule2, double angleThreshold1, double distanceThreshold1
            , double axelssonGridSize1, argumentReader aR, int coreNumber) throws IOException{


        this.coreNumber = coreNumber;

        this.aR = aR;

        write = true;

        this.pointCloud = pointCloud2;
        this.print = print2;

        this.odir = odir2;

        doneInd = new boolean[(int)pointCloud.getNumberOfPointRecords()];
        badInd = new boolean[(int)pointCloud.getNumberOfPointRecords()];
        this.outWriteFile = aR.createOutputFile(pointCloud2);
        this.outputFileName = outWriteFile.getAbsolutePath();

        rule = rule2;

        if(axelssonGridSize1 != -999)
            axelssonGridSize =(int) axelssonGridSize1;

        if(aR.angle != -999) {
            this.angleThreshold = angleThreshold1;
            this.dynamic_angle_threshold = false;
            this.rolling_statistics.setManual_maximum(this.angleThreshold);
        }
        if(aR.dist != -999){
            this.distanceThreshold = this.aR.dist;

        }

        fixedAngle = false;

        this.axelssonGridSize = (int)aR.axgrid;

        this.aR.p_update.lasground_axelssonGridSize = this.axelssonGridSize;
        this.aR.p_update.lasground_angleThreshold = this.angleThreshold;
        this.aR.p_update.lasground_distanceThreshold = this.distanceThreshold;

        this.aR.p_update.threadFile[coreNumber-1] = this.pointCloud.getFile().getName();

        /* Guesstimate the number of ground points. Roughly 10% a good number? */
        tin.preAllocateEdges((int)(pointCloud.getNumberOfPointRecords() * 0.1));

    }


    /**
     * Set the output txt file column order
     *
     *
     */

    public void oparse(String in){

        this.oparse = in;

    }

    public void dispose() throws IOException{

        tin.clear();
        tin.dispose();

        tin = null;

        pointCloud.close();

        pointCloud = null;


        HashSet<Integer> doneIndexes = null;


        HashSet<Integer> groundPointIndexes = null;

        ArrayList<LasPoint> surfaceNormalPoints = null;

        org.tinfour.interpolation.VertexValuatorDefault valuator = null;

        PointInclusionRule rule = null;


    }

    /**
     * Set point cloud
     *
     * @param pointCloud2	New point cloud
     */

    public void setPointCloud(LASReader pointCloud2){

        this.pointCloud = pointCloud2;

        reset();

        this.miniY = pointCloud.getMinY();
        this.maxiY = pointCloud.getMaxY();
        this.miniX = pointCloud.getMinX();
        this.maxiX = pointCloud.getMaxX();

        fixedAngle = true;

    }


    public void setTin(org.tinfour.standard.IncrementalTin in){

        this.tin = in;

    }

    public void setAx(int in){

        this.axelssonGridSize = in;

    }


    /**
     * Reset the TIN and other related
     * variables.
     *
     */

    public void reset(){

        tin.clear();
        seedPoints = 0;
        doneIndexes = new HashSet<Integer>();
        surfaceNormalPoints.clear();
        valuator = new org.tinfour.interpolation.VertexValuatorDefault();

        this.distanceThreshold = this.distanceThreshold;
        this.angleThreshold = this.angleThreshold;

        this.numberOfGroundPoints = 0;
        this.foundGroundPoints = 0;

        this.seedPoints = 0;

        this.print = false;

        this.write = false;

        this.fixedAngle = false;

    }

    public double length(Vector3D a){

        return Math.sqrt(Math.pow(a.getX(), 2) + Math.pow(a.getY(), 2) + Math.pow(a.getZ(), 2));

    }

    public double AngleBetween(Vector3D a, Vector3D b) {
        return 2.0d * Math.atan(length((a.subtract(b)))/length((a.add(b))));

    }

    public static Vertex normal(Vertex a, Vertex b, Vertex c) {
        double ax = a.x - c.x;
        double ay = a.y - c.y;
        double az = a.getZ() - c.getZ();
        double bx = b.x - c.x;
        double by = b.y - c.y;
        double bz = b.getZ() - c.getZ();
        return new Vertex(ay * bz - az * by, az * bx - ax * bz, ax * by
                - ay * bx);
    }


    /**
     * Calculates the average slope of the TIN network.
     * This is used to update the angle threshold to accomodate
     * areas of varying topography.
     *
     * IMPORTANT! At the moment, only includes points found from
     * 			 detectSeedPoints(). Should be modified to include
     *			 all points current TIN.
     *
     * @return average surface normal from surfaceNormalPoints
     */

    public double calcSurfaceNormal(boolean remove){


        //double[] data = new double[3][capacity];
        double[][] data = new double[3][3];
        double[] colmeans = new double[3];


        TriangularFacetInterpolator polator2 = new TriangularFacetInterpolator(tin);
        polator2.resetForChangeToTin();

        //SimpleTriangleIterator triangleIterator = new SimpleTriangleIterator(tin);

        for(SimpleTriangle st : tin.triangles()){

            Vertex normalVertex = normal(st.getVertexA(), st.getVertexB(), st.getVertexC());

            double norm_angle = 90.0d - Math.abs(Math.toDegrees(FastMath.atan(Math.abs(normalVertex.getZ()) / Math.sqrt(normalVertex.x * normalVertex.x + normalVertex.y * normalVertex.y))));

            //System.out.println("angle: " + norm_angle);

            rolling_statistics.add(norm_angle);

            if(true)
                continue;

            System.out.println(normalVertex);

            data[0][0] = st.getVertexA().x; colmeans[0] += st.getVertexA().x;
            data[1][0] = st.getVertexA().y; colmeans[1] += st.getVertexA().y;
            data[2][0] = st.getVertexA().getZ(); colmeans[2] += st.getVertexA().getZ();

            data[0][1] = st.getVertexB().x; colmeans[0] += st.getVertexB().x;
            data[1][1] = st.getVertexB().y; colmeans[1] += st.getVertexB().y;
            data[2][1] = st.getVertexB().getZ(); colmeans[2] += st.getVertexB().getZ();

            data[0][2] = st.getVertexC().x; colmeans[0] += st.getVertexC().x;
            data[1][2] = st.getVertexC().y; colmeans[1] += st.getVertexC().y;
            data[2][2] = st.getVertexC().getZ(); colmeans[2] += st.getVertexC().getZ();

            colmeans[0] /= 3.0d; colmeans[1] /= 3.0d; colmeans[2] /= 3.0d;

            Data dat = new Data(data, colmeans);

            dat.center();
            EigenSet eigen = dat.getCovarianceEigenSet();

            double angle = Math.toDegrees(Math.acos(eigen.vectors[2][2]/1.0));

            //System.out.println(angle);

            colmeans[0] = 0; colmeans[1] = 0; colmeans[2] = 0;

        }


        double[] predictionInterval = null;
        double[] beta = null;

        double sum = 0.0;

        long count2 = 0L;

        double maxAngle = 0.0;

        double angle = 0.0;

        int spacing = 2;

        int xRes = (int)Math.ceil( (this.maxiX - this.miniX) / (double)spacing);
        int yRes = (int)Math.ceil( (this.maxiX - this.miniX) / (double)spacing);

        double x = 0.0;
        double y = 0.0;

        List<Vertex> closest;

        double[][] vs = new double[3][3];

        double[] left = new double[3];
        double[] right = new double[3];

        double interpolatedValue = 0;
        double maxValue = Double.MIN_VALUE;

        for(int i = 0; i < xRes; i++){
            for(int j = 0; j < yRes; j++){

                x = this.miniX + spacing * i;
                y = this.maxiY - spacing * j;

                if(isPointInPolygon(tin.getPerimeter(), x, y) == Polyside.Result.Inside){

                    interpolatedValue = polator2.interpolate(x, y, valuator);
                    closest = tin.getNeighborhoodPointsCollector().collectNeighboringVertices(x, y, 0, 0);

                    if(interpolatedValue > maxValue)
                        maxValue = interpolatedValue;

                    double[] normal = polator2.getSurfaceNormal();



                    if(normal.length > 0) {

                        double norm_angle = 90.0d - Math.abs(Math.toDegrees(FastMath.atan(normal[2] / Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1]))));

                        //System.out.println(norm_angle);

                        this.rolling_statistics.add(norm_angle);

                    }

                    if(true)
                        continue;

                    if(closest.size() > 0){

                        org.tinfour.common.Vertex key = null;

                        for(int v = 0; v < 3; v++){

                            key = closest.get(v);
                            vs[v][0] = key.getX();
                            vs[v][1] = key.getY();
                            vs[v][2] = key.getZ();

                        }

                        left[0] = vs[1][0] - vs[0][0];
                        left[1] = vs[1][1] - vs[0][1];
                        left[2] = vs[1][2] - vs[0][2];

                        right[0] = vs[2][0] - vs[0][0];
                        right[1] = vs[2][1] - vs[0][1];
                        right[2] = vs[2][2] - vs[0][2];

                    }


                    if(beta.length > 3){

                        double zX = beta[1];
                        double zY = beta[2];

                        double grade = Math.sqrt(zX * zX + zY * zY);

                        angle = Math.toDegrees(Math.atan(grade));



                        if(normal.length > 0){

                            double kateetti1 = euclideanDistance(0.0, 0.0, normal[0], normal[1]);

                            double kateetti2 = normal[2];

                            //angleHypo(double hypotenuse, double adjacentSideLength)

                            angle = angleHypo(1.0, kateetti2);

                            //System.out.println("snormal: " + angle);

                            if(!Double.isNaN(angle) && angle < 30){

                                count2++;
                                sum += angle;

                                if(angle > maxAngle)
                                    maxAngle = angle;

                            }
                        }
                        //System.out.println(angle);
                    }

                }
            }



        }

        maxValue = Double.MIN_VALUE;
        int count_outlier = 0;

        //rolling_stats_reset();
        double[] z_values = new double[]{0,0,0};
        int n_removed = 0;

        if(remove)
        for(int i = 0; i < xRes; i++){
            for(int j = 0; j < yRes; j++){

                x = this.miniX + spacing * i;
                y = this.maxiY - spacing * j;

                if(isPointInPolygon(tin.getPerimeter(), x, y) == Polyside.Result.Inside){

                    interpolatedValue = polator2.interpolate(x, y, valuator);
                    closest = tin.getNeighborhoodPointsCollector().collectNeighboringVertices(x, y, 0, 0);

                    double[] normal = polator2.getSurfaceNormal();

                    if(normal.length > 0) {
                        double norm_angle = 90.0d - Math.abs(Math.toDegrees(FastMath.atan(normal[2] / Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1]))));

                        if(this.rolling_statistics.reject_as_outlier_topSide(norm_angle, 2.5)){

                            int remove_index = -1;
                            double remove_max_value = 0;
                            double z_sum = 0;
                            for(int v = 0; v < 3; v++){

                                z_values[v] = closest.get(v).getZ();
                                z_sum += z_values[v];

                            }

                            n_removed++;

                            double mean_z = z_sum / 3.0;

                            if(Math.abs(z_values[0] - mean_z) > Math.abs(z_values[1] - mean_z) && Math.abs(z_values[0] - mean_z) > Math.abs(z_values[2] - mean_z)){

                                tin.remove(closest.get(0));
                                polator2.resetForChangeToTin();

                            }else if(Math.abs(z_values[1] - mean_z) > Math.abs(z_values[0] - mean_z) && Math.abs(z_values[1] - mean_z) > Math.abs(z_values[2] - mean_z)){
                                tin.remove(closest.get(1));
                                polator2.resetForChangeToTin();
                            }else if(Math.abs(z_values[2] - mean_z) > Math.abs(z_values[0] - mean_z) && Math.abs(z_values[2] - mean_z) > Math.abs(z_values[1] - mean_z)){
                                tin.remove(closest.get(2));
                                polator2.resetForChangeToTin();
                            }

                        }
                    }
                }
            }
        }

        if(remove) {
            this.rolling_statistics.reset();

            for (int i = 0; i < xRes; i++) {
                for (int j = 0; j < yRes; j++) {

                    x = this.miniX + spacing * i;
                    y = this.maxiY - spacing * j;

                    if (isPointInPolygon(tin.getPerimeter(), x, y) == Polyside.Result.Inside) {
                        interpolatedValue = polator2.interpolate(x, y, valuator);
                        closest = tin.getNeighborhoodPointsCollector().collectNeighboringVertices(x, y, 0, 0);

                        if (interpolatedValue > maxValue)
                            maxValue = interpolatedValue;

                        double[] normal = polator2.getSurfaceNormal();

                        if (normal.length > 0) {
                            double norm_angle = 90.0d - Math.abs(Math.toDegrees(FastMath.atan(normal[2] / Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1]))));

                            this.rolling_statistics.add(norm_angle);

                        }
                    }
                }


            }
        }

        return Math.min(sum / (double)count2, 15.0);

    }

    public void rolling_stats_add(double val){

        if(val > max_rolling_stats)
            max_rolling_stats = val;

        if(val < min_rolling_stats)
            min_rolling_stats = val;

        count_rolling_stats++;
        average_rolling_stats += (val - average_rolling_stats) / count_rolling_stats;
        pwrSumAvg_rolling_stats += (val * val - pwrSumAvg_rolling_stats) / count_rolling_stats;
        stdDev_rolling_stats = Math.sqrt((pwrSumAvg_rolling_stats * count_rolling_stats - count_rolling_stats * average_rolling_stats * average_rolling_stats) / (count_rolling_stats - 1));
    }

    public double get_rolling_mean(){
        return this.average_rolling_stats;
    }

    public double get_rolling_std(){
        return this.stdDev_rolling_stats;
    }

    public void rolling_stats_reset(){

        this.count_rolling_stats = 0;
        this.average_rolling_stats = 0;
        this.pwrSumAvg_rolling_stats = 0;
        this.stdDev_rolling_stats = 0;
        this.max_rolling_stats = Double.NEGATIVE_INFINITY;
        this.min_rolling_stats = Double.POSITIVE_INFINITY;
    }

    public boolean reject_as_outlier(double val, double threshold){

        if(val < this.average_rolling_stats)
            return false;

        if( Math.abs(val - average_rolling_stats) > (this.stdDev_rolling_stats * threshold) )
            return true;

        return false;

    }

    public static double[] cross(double[] p1, double[] p2) {

        double[] result = new double[p1.length];

        result[0] = p1[1] * p2[2] - p2[1] * p1[2];
        result[1] = p1[2] * p2[0] - p2[2] * p1[0];
        result[2] = p1[0] * p2[1] - p2[0] * p1[1];

        return result;
    }

    public static double dotProduct(double[] a, double[] b) {

        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;

    }

    //(Ax(By -Cy) + Bx(Cy -Ay) + Cx(Ay - By))/2
    public static double triangleArea(double[] a, double[] b, double[] c){

        return (a[0] * (b[1] - c[1]) + b[0] * (c[1] - a[1]) + c[0] * (a[1] - b[1])) / 2.0;

    }

    public static double[] normalize(double[] in){

        double x = in[0];
        double y = in[1];
        double z = in[2];

        double length = Math.sqrt(x*x+y*y+z*z);

        x = x/length;
        y = y/length;
        z = z/length;

        return new double[]{x, y, z};
    }


    /**
     * Classifies points as ground. Requires seed points to have
     * been calculated with detectSeedPoints(). Based on the algorithm Axelsson (2000).
     *
     * Control parameters:
     *
     */

    public int[] detect() throws IOException {

        aR.p_update.updateProgressGroundDetector();
        this.fixedAngle = true;
        TIntArrayList indexes = new TIntArrayList();
        File outputFile = outWriteFile;// new File(outputFileName);

        if (write) {
            if (outputFile.exists())
                outputFile.delete();

            outputFile.createNewFile();

        }

        if (seedPoints == 0)
            throw new toolException("No seed points. What happened?");

        long tStart = System.currentTimeMillis();
        LasPoint tempPoint = new LasPoint();
        ArrayList<Double> angles = new ArrayList<>();
        ArrayList<Double> distances = new ArrayList<>();
        double interpolatedZ;
        double[] normal = null;
        double distanceSigned = 0.0;
        int thread_n = aR.pfac.addReadThread(pointCloud);
        int counter2 = 0;
        TriangularFacetInterpolator polator = new TriangularFacetInterpolator(tin);
        INeighborhoodPointsCollector closest_points = tin.getNeighborhoodPointsCollector();
        int counter_this_iteration = 0;
        ArrayList<Vertex> add_these_to_tin = new ArrayList<>();
        rolling_stats vertex_distance_to_nearest = new rolling_stats();
        TreeMap<Integer, Double> zets = new TreeMap<>();
        IIncrementalTinNavigator navi = tin.getNavigator();
        Vertex[] closest = new Vertex[3];
        SimpleTriangle triang = null;

        for(int loo = 0; loo < aR.num_iter; loo++) {

            add_these_to_tin.clear();
            counter_this_iteration = 0;
            distances.clear();
            angles.clear();

            aR.p_update.updateProgressGroundDetector();
            int maxi = 0;
            polator.resetForChangeToTin();
            pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());
            counter2 = 0;

            //for (int p = 0; p < pointCloud.getNumberOfPointRecords(); p += 200000) {
            for (long p = 0; p < pointCloud.getNumberOfPointRecords(); p++) {


/*

                maxi = (int) Math.min(200000, Math.abs(pointCloud.getNumberOfPointRecords() - (p)));

                try {
                aR.pfac.prepareBuffer(thread_n, p, 200000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (int j = 0; j < maxi; j++) {

                    pointCloud.readFromBuffer(tempPoint);
*/
                pointCloud.readRecord(p, tempPoint);
                    if (!rule.ask(tempPoint, p, true) || doneInd[(int)p]) { // badInd[p + j] || // || tempPoint.numberOfReturns != tempPoint.returnNumber
                        continue;
                    }

                        double distance2 = Double.POSITIVE_INFINITY;
                        double distance = Double.POSITIVE_INFINITY;

                        triang = navi.getContainingTriangle(tempPoint.x, tempPoint.y);

                            if (triang == null) {
                                badInd[(int)p] = true;
                                continue;
                            }

                        closest[0] = triang.getVertexA();
                        closest[1] = triang.getVertexB();
                        closest[2] = triang.getVertexC();

                        interpolatedZ = polator.interpolate(tempPoint.x, tempPoint.y, valuator);

                        if (false)
                            if (tempPoint.x > 607000 && tempPoint.x < 607200 && tempPoint.y > 6943000 && tempPoint.y < 6943200) {
                                System.out.println("GOT HERE!");
                            }

                        if (Double.isNaN(interpolatedZ))
                            continue;

                        distanceSigned = (interpolatedZ - tempPoint.z);

                        normal = polator.getSurfaceNormal();

                        if (normal.length == 3) {
                            distanceSigned = (normal[0] * tempPoint.x + normal[1] * tempPoint.y + normal[2] * tempPoint.z -
                                    (normal[0] * tempPoint.x + normal[1] * tempPoint.y + normal[2] * (tempPoint.z - distanceSigned))) /
                                    Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
                        } else
                            continue;

                        distance = FastMath.abs(distanceSigned);

                        if(distance > distanceThreshold && !aR.axelsson_mirror){
                            continue;
                        }

                        if(distance > 10 && !aR.axelsson_mirror){
                            badInd[(int)p] = true;
                            continue;
                        }

                        double miniDist = Double.POSITIVE_INFINITY;

                                double maxAngle = Double.NEGATIVE_INFINITY;
                                //zets.clear();
                                int counter_angle = 0;
                                int which_is_closest = -1;
                                int counter_v = 0;
                                boolean reject = false;

                                for (Vertex key_ : closest) {

                                    distance2 = euclideanDistance(key_.x, key_.y, tempPoint.x, tempPoint.y);
                                    double distance3d = euclideanDistance_3d(tempPoint.x, tempPoint.y, tempPoint.z,
                                            key_.x, key_.y, key_.getZ());
                                    double angle = FastMath.abs(angleHypo_sine(distance3d, distance));
                                    counter_angle++;

                                    if(this.rolling_statistics.reject_as_outlier_topSide(angle, aR.std_threshold) || Double.isNaN(angle)){
                                        reject = true;
                                        break;
                                    }

                                    if (angle > maxAngle)
                                        maxAngle = angle;

                                    if (distance2 < miniDist) {
                                        miniDist = distance2;
                                        which_is_closest = counter_v;
                                    }

                                    counter_v++;

                                }


                                if (!reject && !Double.isNaN(maxAngle) && distance < distanceThreshold) { // !this.rolling_statistics.reject_as_outlier_topSide(maxAngle, aR.std_threshold)

                                    counter_this_iteration++;

                                    foundGroundPoints++;

                                    if (miniDist > aR.min_edge_length) {

                                        this.rolling_statistics.add(maxAngle);

                                        org.tinfour.common.Vertex tempVertex = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);
                                        tempVertex.setIndex(((int)p));

                                        tin.add(tempVertex);
                                        polator.resetForChangeToTin();
                                        navi.resetForChangeToTin();
                                        doneInd[(int)p] = true;

                                    }//else{
                                    //        doneInd[p + j] = true;
                                    //}

                                } else if(aR.axelsson_mirror) {

                                    reject = false;
                                    /* TEST THE MIRROR POINT ??? */
                                    double angl = FastMath.atan2((closest[which_is_closest].y - tempPoint.y), closest[which_is_closest].x - tempPoint.x);

                                    double x_coord = tempPoint.x + FastMath.cos(angl) * miniDist*2.0;
                                    double y_coord = tempPoint.y + FastMath.sin(angl) * miniDist*2.0;

                                    tempPoint.x = x_coord;
                                    tempPoint.y = y_coord;

                                    triang = navi.getContainingTriangle(tempPoint.x, tempPoint.y);

                                    if (triang == null) {
                                        badInd[(int)p] = true;
                                        continue;
                                    }

                                    closest[0] = triang.getVertexA();
                                    closest[1] = triang.getVertexB();
                                    closest[2] = triang.getVertexC();

                                    interpolatedZ = polator.interpolate(tempPoint.x, tempPoint.y, valuator);

                                    if (Double.isNaN(interpolatedZ))
                                        continue;

                                    distanceSigned = (interpolatedZ - tempPoint.z);

                                    normal = polator.getSurfaceNormal();

                                    if (normal.length == 3) {
                                        distanceSigned = (normal[0] * tempPoint.x + normal[1] * tempPoint.y + normal[2] * tempPoint.z -
                                                (normal[0] * tempPoint.x + normal[1] * tempPoint.y + normal[2] * (tempPoint.z - distanceSigned))) /
                                                Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);

                                    } else
                                        continue;

                                    distance = FastMath.abs(distanceSigned);

                                    if(distance > distanceThreshold ){
                                        continue;
                                    }

                                    if ( false )
                                        if (distance > 5) {
                                            badInd[(int)p] = true;

                                            continue;
                                        }

                                    miniDist = Double.POSITIVE_INFINITY;

                                    maxAngle = Double.NEGATIVE_INFINITY;
                                    counter_angle = 0;
                                    which_is_closest = -1;
                                    counter_v = 0;

                                    for (Vertex key_ : closest) {

                                        distance2 = euclideanDistance(key_.x, key_.y, tempPoint.x, tempPoint.y);
                                        double distance3d = euclideanDistance_3d(tempPoint.x, tempPoint.y, tempPoint.z,
                                                key_.x, key_.y, key_.getZ());

                                        double angle = FastMath.abs(angleHypo_sine(distance3d, distance));

                                        counter_angle++;

                                        if(this.rolling_statistics.reject_as_outlier_topSide(angle, aR.std_threshold) || Double.isNaN(angle)){
                                            reject = true;
                                            break;
                                        }

                                        if (angle > maxAngle)
                                            maxAngle = angle;


                                        if (distance2 < miniDist) {
                                            miniDist = distance2;
                                            which_is_closest = counter_v;
                                        }

                                        counter_v++;

                                    }

                                    if (!reject&& !Double.isNaN(maxAngle) && distance < distanceThreshold) {

                                        counter_this_iteration++;

                                        foundGroundPoints++;

                                        if (miniDist > aR.min_edge_length) {

                                            this.rolling_statistics.add(maxAngle);

                                            org.tinfour.common.Vertex tempVertex = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);
                                            tempVertex.setIndex(((int)p));

                                            tin.add(tempVertex);
                                            polator.resetForChangeToTin();
                                            navi.resetForChangeToTin();

                                            doneInd[(int)p] = true;
                                        }
                                        //else{
                                        //    doneInd[p + j] = true;
                                        //}
                                    }
                                }



                    if (counter2++ % 100000 == 0) {
                        update_progress((int) loo);
                    }
                //}
            } //DONEINDEXES

            vertex_distance_to_nearest.reset();

            if(true){

                removeOutlierPoints(polator, vertex_distance_to_nearest, navi);


            }

            if((double)counter_this_iteration / (double)foundGroundPoints * 100.0 < 1.0){
                break;
            }


        }


        //System.gc();


        int maxi = 0;

        polator.resetForChangeToTin();

        pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

        if(aR.dense)
        for (int p = 0; p < pointCloud.getNumberOfPointRecords(); p += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - (p)));

            try {
                pointCloud.readRecord_noRAF(p, tempPoint, maxi);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(!rule.ask(tempPoint, p+j, true)){
                    continue;
                }

                if (!doneInd[p+j]) {

                    double distance2 = Double.POSITIVE_INFINITY;
                    double distance = Double.POSITIVE_INFINITY;

                    interpolatedZ = polator.interpolate(tempPoint.x, tempPoint.y, valuator);
                    distanceSigned = (interpolatedZ - tempPoint.z);

                    distance = Math.abs(distanceSigned);

                    if(distance <= 0.05) {

                        //System.out.println("HERE!!");
                        doneInd[p + j] = true;

                        //tin.add(new Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                        //polator.resetForChangeToTin();
                    }

                }

            }
        }

        aR.p_update.lasground_doneIndexes = (int)foundGroundPoints;
        aR.p_update.updateProgressGroundDetector();

        ArrayList<org.tinfour.common.Vertex> vertexit = (ArrayList<org.tinfour.common.Vertex>)tin.getVertices();
        pointWriterMultiThread pw = new pointWriterMultiThread(outputFile, pointCloud, "las2las", aR);
        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        try{

            if(write){

                for (long p = 0; p < pointCloud.getNumberOfPointRecords(); p += 20000) {
                //for (long p = 0; p < pointCloud.getNumberOfPointRecords(); p++) {

                    maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - (p)));

                    try {
                        aR.pfac.prepareBuffer(thread_n, p, 20000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    for (long j = 0; j < maxi; j++) {

                        pointCloud.readFromBuffer(tempPoint);

                    //pointCloud.readRecord(p, tempPoint);
                        if(!rule.ask(tempPoint, p+j, true)){
                            continue;
                        }
                        /*      If the las point is already classified as ground, we clear the classification
                                because we do not trust the prior classification.
                         */
                        if(tempPoint.classification == 2)
                            tempPoint.classification = 0;

                        if (doneInd[(int)p+(int)j])
                            tempPoint.classification = 2;

                        if (aR.o_dz) {

                            interpolatedZ = polator.interpolate(tempPoint.x, tempPoint.y, valuator);
                            tempPoint.z -= interpolatedZ;

                        }

                        aR.pfac.writePoint(tempPoint, p+j, thread_n);

                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace(System.out);

        }

        aR.pfac.closeThread(thread_n);

        int[] result = new int[indexes.size()];

        indexes.clear();

        aR.p_update.lasground_fileProgress++;

        aR.p_update.updateProgressGroundDetector();

        if(aR.harmonized){

            File tin_out = fo.createNewFileWithNewExtension(pointCloud.getFile().getAbsolutePath(), "_ground.tin");

            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tin_out)));

            for( Vertex v : tin.getVertices()){

                out.writeDouble(v.getX());
                out.writeDouble(v.getY());
                out.writeFloat((float)v.getZ());

            }

            out.flush();
            out.close();
        }
        return result;

    }

    private void update_progress(int loo) {

        if (this.dynamic_angle_threshold)
            this.angleThreshold = this.rolling_statistics.average_rolling_stats + this.rolling_statistics.stdDev_rolling_stats * aR.std_threshold;

        else
            this.angleThreshold = this.rolling_statistics.manual_maximum;

        this.aR.p_update.lasground_vertices = this.tin.getVertices().size();
        this.aR.p_update.lasground_doneIndexes = (int) foundGroundPoints;

        this.aR.p_update.threadDouble[coreNumber - 1] = this.angleThreshold;

        this.aR.p_update.threadProgress[coreNumber - 1] = (int) foundGroundPoints;
        this.aR.p_update.threadInt[coreNumber - 1] = loo + 1;

        aR.p_update.updateProgressGroundDetector();
    }

    private void removeOutlierPoints(TriangularFacetInterpolator polator, rolling_stats vertex_distance_to_nearest, IIncrementalTinNavigator navi) {

        StreamSupport.stream(tin.triangles().spliterator(), false).forEach((t) -> {

            vertex_distance_to_nearest.add(Math.abs(t.getVertexA().getZ() - t.getVertexB().getZ()));
            vertex_distance_to_nearest.add(Math.abs(t.getVertexA().getZ() - t.getVertexC().getZ()));
            vertex_distance_to_nearest.add(Math.abs(t.getVertexB().getZ() - t.getVertexC().getZ()));


        });

        ArrayList<Vertex> remove_these = new ArrayList<>();

        double threshold = 1.5;
        for(SimpleTriangle t : tin.triangles()){

            double dist_a_b = Math.abs(t.getVertexA().getZ() - t.getVertexB().getZ());
            double dist_a_c = Math.abs(t.getVertexA().getZ() - t.getVertexC().getZ());

            double dist_b_a = Math.abs(t.getVertexB().getZ() - t.getVertexA().getZ());
            double dist_b_c = Math.abs(t.getVertexB().getZ() - t.getVertexC().getZ());

            double dist_c_a = Math.abs(t.getVertexC().getZ() - t.getVertexA().getZ());
            double dist_c_b = Math.abs(t.getVertexC().getZ() - t.getVertexB().getZ());

            if(vertex_distance_to_nearest.reject_as_outlier(dist_a_b, threshold) && vertex_distance_to_nearest.reject_as_outlier(dist_a_c, threshold)){
                remove_these.add(t.getVertexA());
                badInd[t.getVertexA().getIndex()] = true;
                doneInd[t.getVertexA().getIndex()] = false;

            }else if(vertex_distance_to_nearest.reject_as_outlier(dist_b_a, threshold) && vertex_distance_to_nearest.reject_as_outlier(dist_b_c, threshold)){
                remove_these.add(t.getVertexB());
                badInd[t.getVertexB().getIndex()] = true;
                doneInd[t.getVertexB().getIndex()] = false;
            }else if(vertex_distance_to_nearest.reject_as_outlier(dist_c_a, threshold) && vertex_distance_to_nearest.reject_as_outlier(dist_c_b, threshold)){
                remove_these.add(t.getVertexC());
                badInd[t.getVertexC().getIndex()] = true;
                doneInd[t.getVertexC().getIndex()] = false;
            }

        }

        for(Vertex v : remove_these){
            tin.remove(v);
        }

        vertex_distance_to_nearest.reset();

        polator.resetForChangeToTin();
        navi.resetForChangeToTin();
    }

    float squareRoot(float n)
    {

        /*We are using n itself as
        initial approximation This
        can definitely be improved */
        float x = n;
        float y = 1;

        // e decides the accuracy level
        double e = 0.001;
        while (x - y > e) {
            x = (x + y) / 2;
            y = n / x;
        }
        return x;
    }

    double squareRoot(double n)
    {

        /*We are using n itself as
        initial approximation This
        can definitely be improved */
        double x = n;
        double y = 1;

        // e decides the accuracy level
        double e = 0.001;
        while (x - y > e) {
            x = (x + y) / 2;
            y = n / x;
        }
        return x;
    }

    public double interpolate(double v1_x, double v1_y, double v1_z, double v2_x, double v2_y, double v2_z, double v3_x, double v3_y, double v3_z, double x, double y, double z){

        double d1 = Math.sqrt((v1_x - x)*(v1_x - x) + (v1_y - y)*(v1_y - y) + (v1_z - z)*(v1_z - z));
        double d2 = Math.sqrt((v2_x - x)*(v2_x - x) + (v2_y - y)*(v2_y - y) + (v2_z - z)*(v2_z - z));
        double d3 = Math.sqrt((v3_x - x)*(v3_x - x) + (v3_y - y)*(v3_y - y) + (v3_z - z)*(v3_z - z));

        double w1 = 1.0/d1;
        double w2 = 1.0/d2;
        double w3 = 1.0/d3;

        return (w1 * v1_z + w2 * v2_z + w3 * v3_z) / (w1 + w2 + w3);
    }

    public static void removeSpikes(org.tinfour.standard.IncrementalTin in, double thresHold){

        List<org.tinfour.common.IQuadEdge> edges = new ArrayList<org.tinfour.common.IQuadEdge>();
        edges = in.getEdges();

        double spike = thresHold;

        org.tinfour.common.Vertex tempVertexA = null;
        org.tinfour.common.Vertex tempVertexB = null;

        org.tinfour.common.IQuadEdge tempEdge = null;

        /* Idea:


         */

        for(int i = 0; i < edges.size(); i++){

            tempEdge = edges.get(i);

            tempVertexA = tempEdge.getA();
            tempVertexB = tempEdge.getB();

            if(tempVertexA != null && tempVertexB != null){
                //System.out.println(tempVertexB.getStatus() + " " + tempVertexB.getStatus());

                if(tempVertexA.getZ() - tempVertexB.getZ() > spike){

                    tempVertexA.setIndex(tempVertexA.getIndex() + 1);
                    tempVertexB.setIndex(tempVertexB.getIndex() - 1);

                }

                if(tempVertexA.getZ() - tempVertexB.getZ() < (-spike)){

                    tempVertexB.setIndex(tempVertexB.getIndex() + 1);
                    tempVertexA.setIndex(tempVertexA.getIndex() - 1);

                }

                tempVertexA.setStatus(tempVertexA.getStatus() + 1);
                tempVertexB.setStatus(tempVertexB.getStatus() + 1);
            }
        }


        List<org.tinfour.common.Vertex> vL = new ArrayList<org.tinfour.common.Vertex>();
        vL = in.getVertices();

        /* Reset the TIN */
        for(int i = 0; i < vL.size(); i++){

            if(vL.get(i).getIndex() == (vL.get(i).getStatus()) || -vL.get(i).getIndex() == (vL.get(i).getStatus()) ||
                    vL.get(i).getIndex() == (vL.get(i).getStatus() - 1) || -vL.get(i).getIndex() == (vL.get(i).getStatus() + 1)){

                in.remove(vL.get(i));

            }

            else{

                vL.get(i).setIndex(0);
                vL.get(i).setStatus(0);

            }
        }

    }


    /**
     * Calculates the tangent angle
     *
     * @param oppositeSideLength
     * @param adjacentSideLength
     * @return the angle based on trigonometric function
     */

    public double angle(double oppositeSideLength, double adjacentSideLength) {

        return Math.toDegrees(Math.atan(oppositeSideLength/ adjacentSideLength));

    }

    /**
     * Calculates the cosine angle
     *
     * @param hypotenuse
     * @param adjacentSideLength
     * @return the angle based on trigonometric function
     */

    public double angleHypo(double hypotenuse, double adjacentSideLength) {

        return Math.toDegrees(Math.acos(adjacentSideLength / hypotenuse));

    }

    /**
     * Calculates the cosine angle
     *
     * @param hypotenuse
     * @return the angle based on trigonometric function
     */

    public double angleHypo_sine(double hypotenuse, double opposite) {
        return FastMath.toDegrees(Math.asin(opposite / hypotenuse));

    }

    /**
     * Calculates the euclidean distance between two points
     *
     * @param x1 	X coordinate of point1
     * @param y1 	Y coordinate of point1
     * @param x2 	X coordinate of point2
     * @param y2 	Y coordinate of point2
     */

    public double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt(  (x1 - x2) * ( x1 - x2) + ( y1 - y2) * ( y1 - y2) );

    }

    public double euclideanDistance_3d(double x1, double y1, double z1, double x2, double y2, double z2){


        return squareRoot( ( x1 - x2) * ( x1 - x2) + ( y1 - y2) * ( y1 - y2) + ( z1 - z2) * ( z1 - z2) );

    }

    /**
     * Calculates the initial ground points used for
     * initial TIN creation before detect() method.
     */

    public int[] detectSeedPoints() throws IOException{

        aR.p_update.updateProgressGroundDetector();

        TIntArrayList indexes = new TIntArrayList();

        LasPoint tempPoint = new LasPoint();

        double tHold = 5.0;

        double stdResolution = 0.5;


        double minX = pointCloud.getMinX();
        double maxX = pointCloud.getMaxX();
        double minY = pointCloud.getMinY();
        double maxY = pointCloud.getMaxY();

        int numberOfPixelsX = 0;
        int numberOfPixelsY = 0;

        int numberOfPixelsXstd = 0;
        int numberOfPixelsYstd = 0;

        int origAxGrid = axelssonGridSize;

        if(false)
        while(numberOfPixelsX < 5 && numberOfPixelsY < 5){

            numberOfPixelsX = (int)Math.ceil((maxX - minX) / (double)axelssonGridSize);
            numberOfPixelsY = (int)Math.ceil((maxY - minY) / (double)axelssonGridSize);
            axelssonGridSize -= 2;

        }

        axelssonGridSize += 2;

        this.miniY = minY;
        this.maxiY = maxY;
        this.miniX = minX;
        this.maxiX = maxX;

        numberOfPixelsX = (int)Math.ceil((maxX - minX) / (double)axelssonGridSize);
        numberOfPixelsY = (int)Math.ceil((maxY - minY) / (double)axelssonGridSize);

        double[][][] statisticsBig = new double[numberOfPixelsX+1][numberOfPixelsY+1][12];

        double tempx = minX;
        double tempy = maxY;
        //Cantor homma = new Cantor();
        long countx = 0;
        long county = 0;

        while(countx < numberOfPixelsX){

            while(county < numberOfPixelsY){

                statisticsBig[(int)countx][(int)county][3] = Float.NEGATIVE_INFINITY;
                statisticsBig[(int)countx][(int)county][2] = Float.POSITIVE_INFINITY;
                statisticsBig[(int)countx][(int)county][7] = 0;
                long[] temp = new long[2];
                temp[0] = countx;
                temp[1] = county;
                double[] temppiP = new double[5];
                tempy -= axelssonGridSize;
                county++;
                //count2++;
            }
            tempx += axelssonGridSize;
            tempy = maxY;
            countx++;
            county = 0;

        }

        long n = pointCloud.getNumberOfPointRecords();

        int pulseDensitySpacing = 20;

        long[] temppi = new long[2];
        double[] temppiP = new double[5];


        int maxi = 0;

        int counter = 0;

        pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

        if(false)
        for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i += 200000){

            maxi = (int)Math.min(200000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, maxi);
            }catch(Exception e){
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!rule.ask(tempPoint, i+j, true)){
                    continue;
                }

                    temppi[0] = (long) Math.floor((tempPoint.x - minX) / (double) pulseDensitySpacing);   //X INDEX
                    temppi[1] = (long) Math.floor((maxY - tempPoint.y) / (double) pulseDensitySpacing);

                    if(temppi[0] < 0)
                        temppi[0] = 0;
                    if(temppi[0] >= numberOfPixelsX)
                        temppi[0] = numberOfPixelsX - 1;

                    if(temppi[1] < 0)
                        temppi[1] = 0;
                    if(temppi[1] >= numberOfPixelsY)
                        temppi[1] = numberOfPixelsY - 1;

            }

        }

        numberOfPixelsXstd = (int)Math.ceil((maxX - minX) / stdResolution);
        numberOfPixelsYstd = (int)Math.ceil((maxY - minY) / stdResolution);

        /* This will include statistics from stdResolution * stdResolution sized
         area as follows:
         [0] = number of observations
         [1] = sum of z values
         [2      = min z
         [3] = max Z
         [4] = M
         [5] = S
         [6] = standard deviation Z
         [7] = min Index
         [8] = max Index
         */

        float[][][] statistics = new float[numberOfPixelsXstd+1][numberOfPixelsYstd+1][9];


        for(int i = 0; i < numberOfPixelsXstd; i++){
            for(int j = 0; j < numberOfPixelsYstd; j++){

                statistics[i][j][3] = Float.NEGATIVE_INFINITY;
                statistics[i][j][2] = Float.POSITIVE_INFINITY;

            }
        }


        long count = 0;

        float oldM = 0.0f;

        float M = 0.0f;

        double oldMbig = 0.0;
        double Mbig = 0.0;

        int smallX = 0;
        int smallY = 0;

        int bigX = 0;
        int bigY = 0;


        maxi = 0;

        counter = 0;

        LasPoint tempPoint2 = new LasPoint();

        double minz = Double.POSITIVE_INFINITY;


        //for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 200000){
        for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i++){

            //maxi = (int)Math.min(200000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            //try {
            //    pointCloud.readRecord_noRAF(i, tempPoint, maxi);
            //}catch(Exception e){
            //    e.printStackTrace();
            //}

            //for (long j = 0; j < maxi; j++) {

                //pointCloud.readFromBuffer(tempPoint);
            pointCloud.readRecord(i, tempPoint);
                if(rule.ask(tempPoint, i, true)){

                    temppi[0] = (long)Math.floor((tempPoint.x - minX) / (double)axelssonGridSize);   //X INDEX
                    temppi[1] = (long)Math.floor((maxY - tempPoint.y) / (double)axelssonGridSize);

                    if(temppi[0] < 0)
                        temppi[0] = 0;
                    if(temppi[0] >= numberOfPixelsX)
                        temppi[0] = numberOfPixelsX - 1;

                    if(temppi[1] < 0)
                        temppi[1] = 0;
                    if(temppi[1] >= numberOfPixelsY)
                        temppi[1] = numberOfPixelsY - 1;

                    /* This will include statistics from stdResolution * stdResolution sized
                     area as follows:
                     [0] = number of observations
                     [1] = sum of z values
                     [2] = min z
                     [3] = max Z
                     [4] = M
                     [5] = S
                     [6] = standard deviation Z
                     [7] = lowest z index
                     */

                    smallX = (int)((tempPoint.x - minX) / stdResolution);
                    smallY = (int)((maxY - tempPoint.y) / stdResolution);

                    statistics[smallX][smallY][0]++;

                    statisticsBig[(int)temppi[0]][(int)temppi[1]][0]++;

                    statistics[smallX][smallY][1] += tempPoint.z;
                    statisticsBig[(int)temppi[0]][(int)temppi[1]][1] += tempPoint.z;

                    if(tempPoint.z > statistics[smallX][smallY][3]){
                        statistics[smallX][smallY][3] = (float)tempPoint.z;
                        statistics[smallX][smallY][8] = i;
                    }

                    if(tempPoint.z < statistics[smallX][smallY][2]){
                        statistics[smallX][smallY][2] = (float)tempPoint.z;
                        statistics[smallX][smallY][7] = i;
                    }

                    if(tempPoint.z > statisticsBig[(int)temppi[0]][(int)temppi[1]][3]){
                        statisticsBig[(int)temppi[0]][(int)temppi[1]][3] = (float)tempPoint.z;
                        statisticsBig[(int)temppi[0]][(int)temppi[1]][8] = i;
                    }

                    if(tempPoint.z < statisticsBig[(int)temppi[0]][(int)temppi[1]][2]){
                        statisticsBig[(int)temppi[0]][(int)temppi[1]][2] = (float)tempPoint.z;
                        statisticsBig[(int)temppi[0]][(int)temppi[1]][7] = (i);
                        statisticsBig[(int)temppi[0]][(int)temppi[1]][9] = tempPoint.x;
                        statisticsBig[(int)temppi[0]][(int)temppi[1]][10] = tempPoint.y;
                        statisticsBig[(int)temppi[0]][(int)temppi[1]][11] = tempPoint.z;
                        //System.out.println(i+j);
                    }

                    oldM = statistics[smallX][smallY][4];

                    oldMbig = statisticsBig[(int)temppi[0]][(int)temppi[1]][4];

                    M = statistics[smallX][smallY][4];

                    Mbig = statisticsBig[(int)temppi[0]][(int)temppi[1]][4];

                    // M := M + (x-M)/k

                    statistics[smallX][smallY][4] = M + ((float)tempPoint.z - M) / statistics[smallX][smallY][0];
                    statisticsBig[(int)temppi[0]][(int)temppi[1]][4] = Mbig + ((float)tempPoint.z - Mbig) / statisticsBig[(int)temppi[0]][(int)temppi[1]][0];

                    // S := S + (x-M)*(x-oldM)

                    statistics[smallX][smallY][5] = statistics[smallX][smallY][5] + ((float)tempPoint.z - statistics[smallX][smallY][4]) * ((float)tempPoint.z - oldM);
                    statisticsBig[(int)temppi[0]][(int)temppi[1]][5] = statisticsBig[(int)temppi[0]][(int)temppi[1]][5] + ((float)tempPoint.z - statisticsBig[(int)temppi[0]][(int)temppi[1]][4]) * ((float)tempPoint.z - oldMbig);
                    // S/(N-1)

                    statistics[smallX][smallY][6] = (float)Math.sqrt(statistics[smallX][smallY][5] / (statistics[smallX][smallY][0] - 1.0f));
                    statisticsBig[(int)temppi[0]][(int)temppi[1]][6] = (float)Math.sqrt(statisticsBig[(int)temppi[0]][(int)temppi[1]][5] / (statisticsBig[(int)temppi[0]][(int)temppi[1]][0] - 1.0f));

                    temppiP[0] = tempPoint.x;
                    temppiP[1] = tempPoint.y;
                    temppiP[2] = tempPoint.z;
                    temppiP[3] = tempPoint.classification;
                    temppiP[4] = i;

                }

            //}


        }

        double averagePointDensity = 0.0;
        int averagePointDensityCount = 0;

        for(int i = 0; i < numberOfPixelsXstd; i++){
            for(int j = 0; j < numberOfPixelsYstd; j++){

                if(statistics[i][j][0] > 0){
                    averagePointDensityCount++;
                    averagePointDensity += statistics[i][j][0]; // * (1.0 / stdResolution);
                }
            }
        }

        averagePointDensity = averagePointDensity / averagePointDensityCount;
        rolling_stats_reset();

        for(int x = 0; x < numberOfPixelsXstd; x++){
            for(int y = 0; y < numberOfPixelsYstd; y++) {

                rolling_stats_add(statistics[x][y][0]);

            }
        }

        double factor = stdResolution*stdResolution / (axelssonGridSize*axelssonGridSize);

        float threshold = 0.25f;
        float threshold_std = 0.10f;
        float meani = 0.0f;
        float std = 0.0f;

        double[] point = new double[3];

        if(!aR.photogrammetry)
        if(tin.getVertices().size() < 6)
            for(int x = 0; x < numberOfPixelsX - 0; x++)
                for(int y = 0; y < numberOfPixelsY - 0; y++){

                    /* This will include statistics from stdResolution * stdResolution sized
         area as follows:
         [0] = number of observations
         [1] = sum of z values
         [2      = min z
         [3] = max Z
         [4] = M
         [5] = S
         [6] = standard deviation Z
         [7] = min Index
         [8] = max Index
         */

                    inspectSeedPoint(indexes, statisticsBig, point, x, y);
                }


        this.rolling_statistics.reset();
        double z_threshold = -1;

        double neigh_max_mean_difference = 1.0;


        if(aR.photogrammetry)
        while(tin.getVertices().size() < 6) {

            for (int i = 0; i < numberOfPixelsYstd; i++) {
                for (int j = 0; j < numberOfPixelsXstd; j++) {

                    double mean = statistics[j][i][1] / statistics[j][i][0];

                    int number2 = 0;

                    /* Two sanity checks to remove outliers:

                      (1) Must have neighboring cell with a ground point.
                      (2) The neighboring cell must not be too different with regard to z
                     */

                    int minus_j = Math.max(0,j-1);
                    int plus_j = Math.min(numberOfPixelsXstd-1,j+1);

                    int minus_i = Math.max(0,i-1);
                    int plus_i = Math.min(numberOfPixelsYstd-1,i+1);

                    boolean ignoreMinus_j = minus_j == j;
                    boolean ignorePlus_j = plus_j == j;
                    boolean ignoreMinus_i = minus_i == i;
                    boolean ignorePlus_i = plus_i == i;

                    number2 = sanityCheck(statistics, threshold_std, neigh_max_mean_difference, i, j, mean, number2, minus_j, plus_j, minus_i, plus_i, ignoreMinus_j, ignorePlus_j, ignoreMinus_i, ignorePlus_i);

                    /*

                    number2 += statistics[j-1][i][6] < threshold_std && statistics[j-1][i][0] > 5
                            &&  Math.abs(mean - (statistics[j-1][i][1] / statistics[j-1][i][0])) < neigh_max_mean_difference
                            && statistics[j-1][i][3] - statistics[j-1][i][2] < threshold_std*2 ? 1 : 0;
                    number2 += statistics[j-1][i-1][6] < threshold_std && statistics[j-1][i-1][0] > 5
                            &&  Math.abs(mean - (statistics[j-1][i-1][1] / statistics[j-1][i-1][0])) < neigh_max_mean_difference
                            && statistics[j-1][i-1][3] - statistics[j-1][i-1][2] < threshold_std*2 ? 1 : 0;
                    number2 += statistics[j][i-1][6] < threshold_std && statistics[j][i-1][0] > 5
                            &&  Math.abs(mean - (statistics[j][i-1][1] / statistics[j][i-1][0])) < neigh_max_mean_difference
                            && statistics[j][i-1][3] - statistics[j][i-1][2] < threshold_std*2 ? 1 : 0;
                    number2 += statistics[j+1][i-1][6] < threshold_std && statistics[j+1][i-1][0] > 5
                            &&  Math.abs(mean - (statistics[j+1][i-1][1] / statistics[j+1][i-1][0])) < neigh_max_mean_difference
                            && statistics[j+1][i-1][3] - statistics[j+1][i-1][2] < threshold_std*2 ? 1 : 0;
                    number2 += statistics[j+1][i][6] < threshold_std && statistics[j+1][i][0] > 5
                            &&  Math.abs(mean - (statistics[j+1][i][1] / statistics[j+1][i][0])) < neigh_max_mean_difference
                            && statistics[j+1][i][3] - statistics[j+1][i][2] < threshold_std*2 ? 1 : 0;
                    number2 += statistics[j+1][i+1][6] < threshold_std && statistics[j+1][i+1][0] > 5
                            &&  Math.abs(mean - (statistics[j+1][i+1][1] / statistics[j+1][i+1][0])) < neigh_max_mean_difference
                            && statistics[j+1][i+1][3] - statistics[j+1][i+1][2] < threshold_std*2 ? 1 : 0;
                    number2 += statistics[j][i+1][6] < threshold_std && statistics[j][i+1][0] > 5
                            &&  Math.abs(mean - (statistics[j][i+1][1] / statistics[j][i+1][0])) < neigh_max_mean_difference
                            && statistics[j][i+1][3] - statistics[j][i+1][2] < threshold_std*2 ? 1 : 0;
                    number2 += statistics[j-1][i+1][6] < threshold_std && statistics[j-1][i+1][0] > 5
                            &&  Math.abs(mean - (statistics[j-1][i+1][1] / statistics[j-1][i+1][0])) < neigh_max_mean_difference
                            && statistics[j-1][i+1][3] - statistics[j-1][i+1][2] < threshold_std*2? 1 : 0;

                     */

                    if (statistics[j][i][0] > 5 && number2 >= 1) {

                        if (statistics[j][i][6] < threshold_std && (long) statistics[j][i][7] < pointCloud.getNumberOfPointRecords()
                                && statistics[j][i][3] - statistics[j][i][2] < threshold_std*2) {
                            /* This is the lowest point in the smaller rectangle **/

                            pointCloud.readRecord((long) statistics[j][i][7], tempPoint);

                            if((float)tempPoint.z != statistics[j][i][2])
                                continue;

                            bigX = (int) Math.floor((tempPoint.x - minX) / (double) axelssonGridSize);   //X INDEX
                            bigY = (int) Math.floor((maxY - tempPoint.y) / (double) axelssonGridSize);

                            if(bigX < 0)
                                bigX = 0;
                            if(bigX >= numberOfPixelsX)
                                bigX = numberOfPixelsX - 1;

                            if(bigY < 0)
                                bigY = 0;
                            if(bigY >= numberOfPixelsY)
                                bigY = numberOfPixelsY - 1;

                            /* This will include statistics from stdResolution * stdResolution sized
                             area as follows:
                             [0] = number of observations
                             [1] = sum of z values
                             [2] = min z
                             [3] = max Z
                             [4] = M
                             [5] = S
                             [6] = standard deviation Z
                             [7] = lowest z index
                             */

                            z_threshold = statisticsBig[bigX][bigY][2] + 2.0;

                            /* Let's see if the point is at the lower end in the larger rectangle */
                            if (tempPoint.z < z_threshold && !seedPointIndexes.contains((int) statistics[j][i][7])) {

                                doneInd[(int) statistics[j][i][7]] = true;
                                indexes.add((int) statistics[j][i][7]);
                                seedPoints++;
                                seedPointIndexes.add((int) statistics[j][i][7]);
                                doneInd[(int) statistics[j][i][7]] = true;
                                org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);
                                tempV.setIndex((int) statistics[j][i][7]);
                                tin.add(tempV);
                                seedPointVertices.put((int) statistics[j][i][7], tempV);

                            }
                        }
                    }
                }
            }
            threshold += 0.1f;
            threshold_std += 0.1f;
        }

        calcSurfaceNormal(false);

        //Geometry outRing = new Geometry(wkbLinearRing);

        List<IQuadEdge> perim = new ArrayList<>();
        perim = tin.getPerimeter();

        Coordinate[] coordinates = new Coordinate[]{new Coordinate(0, 0),
                new Coordinate(10, 10), new Coordinate(20, 20)};

        Coordinate[] coords = new Coordinate[perim.size() + 1];

        //Polygon poly = new Polygon();
        KdTree tree = new KdTree();



        for(int i = 0 ; i < perim.size(); i++){
            coords[i] = new Coordinate(perim.get(i).getA().x, perim.get(i).getA().y, perim.get(i).getA().getZ());
            tree.add(new KdTree.XYZPoint(perim.get(i).getA().x, perim.get(i).getA().y, perim.get(i).getA().getZ()));

        }

        coords[coords.length-1] = new Coordinate(perim.get(perim.size()-1).getB().x, perim.get(perim.size()-1).getB().y, perim.get(perim.size()-1).getB().getZ());

        org.locationtech.jts.geom.Geometry g2 = new GeometryFactory().createLineString(coords);
        org.locationtech.jts.geom.Geometry g1 = new GeometryFactory().createPolygon(coords);

        org.locationtech.jts.geom.Geometry g_buf = g1.buffer(50);

        KdTree.XYZPoint searchPoint = new KdTree.XYZPoint(0,0,0);
        List<KdTree.XYZPoint> nearest;
        for(Coordinate c : g_buf.getCoordinates()){

            searchPoint.setX(c.x);
            searchPoint.setY(c.y);
            searchPoint.setZ(c.z);
            nearest = (List<KdTree.XYZPoint>)tree.nearestNeighbourSearch(1, searchPoint);
            tin.add(new Vertex(c.x, c.y, nearest.get(0).getZ()));

        }

        int[] result = new int[indexes.size()];
        for(int i = 0; i < result.length; i++){
            result[i] = indexes.getQuick(i);
        }
        indexes.clear();

        this.axelssonGridSize = origAxGrid;

        aR.p_update.lasground_seedPoints = (int)this.seedPoints;

        aR.p_update.updateProgressGroundDetector();

        aR.p_update.threadEnd[coreNumber-1] = (int)seedPoints;




        return result;

    }

    private int sanityCheck(float[][][] statistics, float threshold_std, double neigh_max_mean_difference, int i, int j, double mean, int number2, int minus_j, int plus_j, int minus_i, int plus_i, boolean ignoreMinus_j, boolean ignorePlus_j, boolean ignoreMinus_i, boolean ignorePlus_i) {
        if(!ignoreMinus_j)
            number2 += statistics[minus_j][i][6] < threshold_std && statistics[minus_j][i][0] > 5
                    &&  Math.abs(mean - (statistics[minus_j][i][1] / statistics[minus_j][i][0])) < neigh_max_mean_difference
                    && statistics[minus_j][i][3] - statistics[minus_j][i][2] < threshold_std *2 ? 1 : 0;

        if(!ignoreMinus_j || !ignoreMinus_i)
            number2 += statistics[minus_j][minus_i][6] < threshold_std && statistics[minus_j][minus_i][0] > 5
                    &&  Math.abs(mean - (statistics[minus_j][minus_i][1] / statistics[minus_j][minus_i][0])) < neigh_max_mean_difference
                    && statistics[minus_j][minus_i][3] - statistics[minus_j][minus_i][2] < threshold_std *2 ? 1 : 0;

        if(!ignoreMinus_i)
            number2 += statistics[j][minus_i][6] < threshold_std && statistics[j][minus_i][0] > 5
                    &&  Math.abs(mean - (statistics[j][minus_i][1] / statistics[j][minus_i][0])) < neigh_max_mean_difference
                    && statistics[j][minus_i][3] - statistics[j][minus_i][2] < threshold_std *2 ? 1 : 0;

        if(!ignoreMinus_i || !ignorePlus_j)
            number2 += statistics[plus_j][minus_i][6] < threshold_std && statistics[plus_j][minus_i][0] > 5
                    &&  Math.abs(mean - (statistics[plus_j][minus_i][1] / statistics[plus_j][minus_i][0])) < neigh_max_mean_difference
                    && statistics[plus_j][minus_i][3] - statistics[plus_j][minus_i][2] < threshold_std *2 ? 1 : 0;

        if(!ignorePlus_j)
            number2 += statistics[plus_j][i][6] < threshold_std && statistics[plus_j][i][0] > 5
                    &&  Math.abs(mean - (statistics[plus_j][i][1] / statistics[plus_j][i][0])) < neigh_max_mean_difference
                    && statistics[plus_j][i][3] - statistics[plus_j][i][2] < threshold_std *2 ? 1 : 0;

        if(!ignorePlus_j || !ignorePlus_i)
            number2 += statistics[plus_j][plus_i][6] < threshold_std && statistics[plus_j][plus_i][0] > 5
                    &&  Math.abs(mean - (statistics[plus_j][plus_i][1] / statistics[plus_j][plus_i][0])) < neigh_max_mean_difference
                    && statistics[plus_j][plus_i][3] - statistics[plus_j][plus_i][2] < threshold_std *2 ? 1 : 0;

        if(!ignorePlus_i)
            number2 += statistics[j][plus_i][6] < threshold_std && statistics[j][plus_i][0] > 5
                    &&  Math.abs(mean - (statistics[j][plus_i][1] / statistics[j][plus_i][0])) < neigh_max_mean_difference
                    && statistics[j][plus_i][3] - statistics[j][plus_i][2] < threshold_std *2 ? 1 : 0;

        if(!ignorePlus_i || !ignoreMinus_j)
            number2 += statistics[minus_j][plus_i][6] < threshold_std && statistics[minus_j][plus_i][0] > 5
                    &&  Math.abs(mean - (statistics[minus_j][plus_i][1] / statistics[minus_j][plus_i][0])) < neigh_max_mean_difference
                    && statistics[minus_j][plus_i][3] - statistics[minus_j][plus_i][2] < threshold_std *2? 1 : 0;
        return number2;
    }

    private void inspectSeedPoint(TIntArrayList indexes, double[][][] statisticsBig, double[] point, int x, int y) {
        if(statisticsBig[x][y][0] > 10 ) {

            point[0] = statisticsBig[x][y][9];
            point[1] = statisticsBig[x][y][10];
            point[2] = statisticsBig[x][y][11];

            indexes.add((int) statisticsBig[x][y][7]);
            seedPoints++;

            seedPointIndexes.add((int) statisticsBig[x][y][7]);
            doneInd[(int) statisticsBig[x][y][7]] = true;


            //org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);
            Vertex tempV = new Vertex(point[0], point[1], point[2]);
            tempV.setIndex((int) statisticsBig[x][y][7]);
            tin.add(tempV);
            seedPointVertices.put((int) statisticsBig[x][y][7], tempV);


        }
    }


    public boolean isLastOfManyOrOnly(LasPoint point){

        if(true)
            return true;

        if(point.numberOfReturns == 1 || point.numberOfReturns == 0)
            return true;

        return point.returnNumber == point.numberOfReturns;
    }

    /**
     * This ain't done yet!!
     *
     * @param outputFile2
     * @param rule
     * @param otype
     */
    public void normalizeZ_mem_eff(String outputFile2, PointInclusionRule rule, String otype){

        if(aR.ground_class == -1)
            this.aR.p_update.lasheight_groundClass = 2;
        else
            this.aR.p_update.lasheight_groundClass = aR.ground_class;

        this.progress_end = (int)pointCloud.getNumberOfPointRecords();

        aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber-1] = 0;
        aR.p_update.threadInt[coreNumber-1] = 0;

        aR.p_update.updateProgressNormalize();
        //this.updateProgress_normalize();

        double minX = pointCloud.getMinX();
        double maxX = pointCloud.getMaxX();
        double minY = pointCloud.getMinY();
        double maxY = pointCloud.getMaxY();

        double orig_x = minX;
        double orig_y = maxY;
        double resolution = aR.step;

        int n_x = 0;
        int n_y = 0;

        n_x = (int)Math.ceil((maxX - minX) / aR.step);
        n_y = (int)Math.ceil((maxY - minY) / aR.step);


        if(!pointCloud.isIndexed){
            try {
                pointCloud.index(20);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        LasPoint tempPoint = new LasPoint();

        tin.clear();

        System.gc();
        System.gc();
        System.gc();

        /* Iterate through the rectangles.
        * For each rectangle, we create a new TIN
        *  */
        for(int x = 0; x < n_x; x++) {
            for (int y = 0; y < n_y; y++) {

                try {
                    pointCloud.queryPoly2(orig_x + resolution * x, orig_x + resolution * x + resolution, orig_y - resolution * y - resolution, orig_y - resolution * y);
                }catch (Exception e){
                    e.printStackTrace();
                }

                this.inside_dim = resolution;
                this.inside_x = orig_x + resolution * x;
                this.inside_y = orig_y - resolution * y;

                this.outside_y = this.inside_y;
                this.outside_x = this.inside_x;

                int pienin, suurin;

                if (pointCloud.queriedIndexes2.size() > 0) {

                    for (int u = 0; u < pointCloud.queriedIndexes2.size(); u++) {

                        long n1 = pointCloud.queriedIndexes2.get(u)[1] - pointCloud.queriedIndexes2.get(u)[0];
                        long n2 = pointCloud.queriedIndexes2.get(u)[1];

                        int parts = (int) Math.ceil((double) n1 / 20000.0);
                        int jako = (int) Math.ceil((double) n1 / (double) parts);

                        int ero;

                        for (int c = 1; c <= parts; c++) {

                            if (c != parts) {
                                pienin =    (c - 1) * jako;
                                suurin =    c * jako;
                            } else {
                                pienin =    (c - 1) * jako;
                                suurin =    (int) n1;
                            }

                            pienin = pienin + pointCloud.queriedIndexes2.get(u)[0];
                            suurin = suurin + pointCloud.queriedIndexes2.get(u)[0];

                            ero = suurin - pienin + 1;

                            try {
                                pointCloud.readRecord_noRAF(pienin, tempPoint, ero);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            for (int p = pienin; p <= suurin; p++) {

                                pointCloud.readFromBuffer(tempPoint);

                                                /* Reading, so ask if this point is ok, or if
                                it should be modified.
                                 */
                                if(!rule.ask(tempPoint, p, true)){
                                    continue;
                                }

                                if(tempPoint.x < inside_x + aR.step && tempPoint.x >= inside_x &&
                                        tempPoint.y >= inside_y - aR.step && tempPoint.y < inside_y)
                                if(tempPoint.classification == 2)
                                    tin.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));

                            }
                        }
                    }

                    /* Expanding means that we have guaranteed that no additional point may alter
                    the TIN in the area of the rectangle.
                     */
                    expandTin();

                    tin.clear();

                    if(false)
                        /* Second iteration of the points to normalize and output */
                    for (int u = 0; u < pointCloud.queriedIndexes2.size(); u++) {

                        long n1 = pointCloud.queriedIndexes2.get(u)[1] - pointCloud.queriedIndexes2.get(u)[0];
                        long n2 = pointCloud.queriedIndexes2.get(u)[1];

                        int parts = (int) Math.ceil((double) n1 / 20000.0);
                        int jako = (int) Math.ceil((double) n1 / (double) parts);

                        int ero;

                        for (int c = 1; c <= parts; c++) {

                            if (c != parts) {
                                pienin = (c - 1) * jako;
                                suurin = c * jako;
                            } else {
                                pienin = (c - 1) * jako;
                                suurin = (int) n1;
                            }

                            pienin = pienin + pointCloud.queriedIndexes2.get(u)[0];
                            suurin = suurin + pointCloud.queriedIndexes2.get(u)[0];

                            ero = suurin - pienin + 1;

                            try {
                                pointCloud.readRecord_noRAF(pienin, tempPoint, ero);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            for (int p = pienin; p <= suurin; p++) {

                                pointCloud.readFromBuffer(tempPoint);

                                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                                if(!rule.ask(tempPoint, p, true)){
                                    continue;
                                }

                                /* We must find new ground points to the tin, but with what
                                  criterion?
                                 */
                                //if(!tin.isPointInsideTin(tempPoint.x, tempPoint.y)){

                                //}
                            }
                        }
                    }
                }
            }
        }
    }

    public void expandTin(){

        double expansion_increment = 5.0;
        double current_expansion = 5.0;

        this.expansion = current_expansion;
        this.expansion_orig = expansion_increment;

        double outside_x = inside_x, outside_y = inside_y;
        double prev_x = inside_x, prev_y = inside_y;

        GeometricOperations geoOp;

        geoOp = new GeometricOperations(tin.getThresholds());

        tin2.clear();

        /* We need to guarantee that adding more points WILL NOT
            influence the TIN inside the current square.

        Termination condition :


         */

        boolean loop = true;

        boolean[] bad = new boolean[4];
        boolean[] doneSide = new boolean[4];
        boolean[] outsideboundary = new boolean[4];

        System.out.println("ORIG: " + tin.getVertices().size());

        /* FIRST WE FIND GOOD TRIANGLES WITHIN THE SQUARE, OTHER
          TRIANGLES ARE OUTPUT TO TIN2 */
        if (tin.isBootstrapped()) {

            int maxIndex = tin.getMaximumEdgeAllocationIndex();
            int maxMapIndex = maxIndex + 2;

            BitSet bitset = new BitSet(maxMapIndex);

            for(SimpleTriangle triangle : tin.triangles()){


                Circumcircle cc = triangle.getCircumcircle();

                double dist_x = Math.min(cc.getX() - inside_x, inside_x + aR.step - cc.getX());
                double dist_y = Math.min(cc.getY() - (inside_y-aR.step), inside_y - cc.getY());

                boolean inside__x = cc.getX() < (inside_x + aR.step) && cc.getX() > inside_x;
                boolean inside__y = cc.getY() < (inside_y) && cc.getY() > (inside_y - aR.step);

                /* This means the circle is completely within the square, and can be output to the FINAL tin */
                if(dist_x < cc.getRadius() && dist_y < cc.getRadius() && inside__x && inside__y){



                }

            }

            Iterator<IQuadEdge> iEdge = tin.getEdgeIterator();

            while (iEdge.hasNext()) {
                IQuadEdge e = iEdge.next();

                /* If one vertex is null, we ignore this edge */
                if (e.getA() == null || e.getB() == null) {
                    setMarkBit(bitset, e);
                    setMarkBit(bitset, e.getDual());
                    continue;
                }

                /* Why do both e and e.getDual()? Don't remember... */
                this.countTriangleEdge(bitset, e);
                this.countTriangleEdge(bitset, e.getDual());
            }

        }

        System.gc();
        System.gc();
        System.gc();
        System.gc();

        isRectangleWithinTin(tin2.getPerimeter());

        System.out.println("###########################");
        prev_y = outside_y;
        prev_x = outside_x;

        while(loop) {

            outside_x -= expansion_increment;
            outside_y += expansion_increment;

            this.outside_x = outside_x;
            this.outside_y = outside_y;

            outsideboundary[0] = addBufferPointsToTin(1, outside_x, outside_y, current_expansion, expansion_increment);
            outsideboundary[1] = addBufferPointsToTin(2, outside_x, outside_y, current_expansion, expansion_increment);
            outsideboundary[2] = addBufferPointsToTin(3, outside_x, outside_y, current_expansion, expansion_increment);
            outsideboundary[3] = addBufferPointsToTin(4, outside_x, outside_y, current_expansion, expansion_increment);
/*
            System.out.println(expansion_increment + " " + current_expansion);
            System.out.println(Arrays.toString(outsideboundary));
*/

            condition2 = 0;

            if (tin.isBootstrapped()) {

                int maxIndex = tin.getMaximumEdgeAllocationIndex();
                int maxMapIndex = maxIndex + 2;

                BitSet bitset = new BitSet(maxMapIndex);

                Iterator<IQuadEdge> iEdge = tin.getEdgeIterator();

                while (iEdge.hasNext()) {
                    IQuadEdge e = iEdge.next();

                    if (e.getA() == null || e.getB() == null) {
                        setMarkBit(bitset, e);
                        setMarkBit(bitset, e.getDual());
                        continue;
                    }

                    this.countTriangleEdge(bitset, e);
                    this.countTriangleEdge(bitset, e.getDual());
                }
            }

            System.gc();
            System.gc();
            System.gc();
            System.gc();

            System.out.println(expansion + " " + condition2);

            if(current_expansion > expansion_increment)
                loop = false;

            current_expansion += expansion_increment;

            this.expansion = current_expansion;

            prev_y = outside_y;
            prev_x = outside_x;

        }
    }

    public boolean addBufferPointsToTin(int side, double outside_x, double outside_y, double expansion, double expansion_orig){

        LasPoint tempPoint = new LasPoint();

        double minx = 0, maxx = 0, miny = 0, maxy = 0;

        /* TOP */
        if(side == 1){
            minx = outside_x;
            maxx = outside_x + aR.step + 2.0 * expansion;

            miny = outside_y - expansion_orig;
            maxy =  outside_y;
        }

        /* RIGHT */
        if(side == 2){
            minx = outside_x + aR.step + 2.0 * expansion - expansion_orig;
            maxx = outside_x + aR.step + 2.0 * expansion;

            miny = outside_y - expansion * 2.0 - aR.step + expansion_orig ;
            maxy =  outside_y - expansion_orig;

        }

        /* BOTTOM */
        if(side == 3){
            minx = outside_x;
            maxx = outside_x + aR.step + 2.0 * expansion;

            maxy = outside_y - expansion * 2.0 - aR.step + expansion_orig ;
            miny = outside_y - expansion * 2.0 - aR.step;

        }

        /* LEFT */
        if(side == 4){
            minx = outside_x;
            maxx = outside_x + expansion_orig;

            miny = outside_y - expansion * 2.0 - aR.step + expansion_orig ;
            maxy = outside_y - expansion_orig;
        }

        int pointsRead = 0;
        if(true){

            try {
                pointCloud.queryPoly2(minx,maxx, miny,  maxy);

            }catch (Exception e){
                e.printStackTrace();
            }

            int pienin, suurin;

            if (pointCloud.queriedIndexes2.size() > 0) {

                for (int u = 0; u < pointCloud.queriedIndexes2.size(); u++) {

                    long n1 = pointCloud.queriedIndexes2.get(u)[1] - pointCloud.queriedIndexes2.get(u)[0];
                    long n2 = pointCloud.queriedIndexes2.get(u)[1];

                    int parts = (int) Math.ceil((double) n1 / 20000.0);
                    int jako = (int) Math.ceil((double) n1 / (double) parts);

                    int ero;

                    for (int c = 1; c <= parts; c++) {

                        if (c != parts) {
                            pienin = (c - 1) * jako;
                            suurin = c * jako;
                        } else {
                            pienin = (c - 1) * jako;
                            suurin = (int) n1;
                        }

                        pienin = pienin + pointCloud.queriedIndexes2.get(u)[0];
                        suurin = suurin + pointCloud.queriedIndexes2.get(u)[0];

                        ero = suurin - pienin + 1;

                        try {
                            pointCloud.readRecord_noRAF(pienin, tempPoint, ero);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        for (int p = pienin; p <= suurin; p++) {

                            pointCloud.readFromBuffer(tempPoint);
/* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                            if(!rule.ask(tempPoint, p, true)){
                                continue;
                            }


                            if(tempPoint.x >= minx && tempPoint.x < maxx && tempPoint.y >= miny && tempPoint.y < maxy){
                                pointsRead++;

                                if(tempPoint.classification == 2){
                                    if(tempPoint.classification == 2)
                                        tin.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                                }
                            }
                        }
                    }
                }
            }
            else{

            }
        }

        return pointsRead > 0;

    }

    public boolean isRectangleWithinTin(List<IQuadEdge> edges){

        for(IQuadEdge e : edges){
            if(pointInsideRectangle(e.getA().x, e.getA().y, inside_x, inside_y, aR.step)){
                return false;
            }
            if(pointInsideRectangle(e.getB().x, e.getB().y, inside_x, inside_y, aR.step)){
                return false;
            }
        }

        return true;
    }

    public boolean pointInsideRectangle(double p_x, double p_y, double r_x, double r_y, double r_s){

        return p_x >= r_x && p_x < r_x + r_s && p_y >= r_y - r_s && p_y < r_y;

    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean getMarkBit(BitSet bitset, final IQuadEdge edge) {
        int index = edge.getIndex();
        return bitset.get(index);
    }

    private void countTriangleEdge(BitSet bitset, IQuadEdge e) {

        if (!getMarkBit(bitset, e)) {
            setMarkBit(bitset, e);
            IQuadEdge f = e.getForward();
            // ghost triangle, not tabulated
            if (f.getB() != null) {
                IQuadEdge r = e.getReverse();
                // check to see that both neighbors are not marked.
                if (!getMarkBit(bitset, f) && !getMarkBit(bitset, r)) {

                    setMarkBit(bitset, f);
                    setMarkBit(bitset, r);

                    /* Find the circumcircle of this triangle */
                    CC.compute(e.getA(), f.getA(), r.getA());


                    boolean isInside = CC.getX() < inside_x + aR.step && CC.getX() >= inside_x &&
                            CC.getY() >= inside_y - aR.step && CC.getY() < inside_y;

                    /* IF the circle is inside the current square */
                    if(isInside) {

                        /* Distance to the smaller square (i.e. the original one) */
                        double dist_x = Math.min(CC.getX() - inside_x, inside_x + aR.step - CC.getX());
                        double dist_y = Math.min(CC.getY() - (inside_y-aR.step), inside_y - CC.getY());

                        double disti = Math.min(dist_x, dist_y);


                        double dist_x_outer = Math.min(CC.getX() - outside_x, outside_x + aR.step - CC.getX());
                        double dist_y_outer = Math.min(CC.getY() - (outside_y-aR.step), outside_y - CC.getY());

                        /* Distance to the buffered square */
                        double disti_outer = Math.min(dist_x_outer, dist_y_outer);

                        /* These are the ones we want to ELIMINATE, but how to do it efficiently? */

                        /* The termination condition is that NO CIRCLE HAS A CIRCUMCIRCLE BOTH INSIDE THE SMALLER RECTANGLE AND
                        OUTSIDE THE LARGER RECTANGLE!!

                                    CONFUSING AS FUCK, I KNOW!!!
                         */
                        /* WE DON'T DO THIS; RIGHT? */
                        if(disti_outer < CC.getRadius() && false) {
                            //System.out.println("Inside, but circle touches outside!! " + disti + " " + CC.getRadius());
                            tin2.add(e.getA());
                            tin2.add(f.getA());
                            tin2.add(r.getA());

                            condition2++;

                        }else{

                        }
                    }
                    else{

                        double disti = 0.0;
                        int whichOne = 0;

                        if(CC.getX() >= inside_x && CC.getX() < inside_x + aR.step &&
                                CC.getY() > inside_y) {
                            disti = CC.getY() - inside_y;
                            whichOne = 1;
                        }
                        else if(CC.getX() >= inside_x && CC.getX() < inside_x + aR.step &&
                                CC.getY() <= inside_y-aR.step) {
                                disti = inside_y - aR.step - CC.getY();
                            whichOne = 2;
                            }
                        else if(CC.getY() >= inside_y - aR.step && CC.getY() < inside_y &&
                                CC.getX() < inside_x) {
                                    disti = inside_x - CC.getX();
                            whichOne =3;
                                }
                        else if(CC.getY() >= inside_y - aR.step && CC.getY() < inside_y &&
                                CC.getX() >= inside_x+aR.step) {
                                        disti = CC.getX() - (inside_x + aR.step);
                            whichOne = 4;
                                    }
                        else if(CC.getX() >= inside_x + aR.step && CC.getY() >= inside_y) {
                                            disti = Math.sqrt((CC.getX() - (inside_x + aR.step)) * (CC.getX() - (inside_x + aR.step)) +
                                                    ((CC.getY() - inside_y) * (CC.getY() - inside_y)));
                            whichOne = 5;
                                        }
                        else if(CC.getX() >= inside_x + aR.step && CC.getY() < inside_y-aR.step) {
                                                disti = Math.sqrt((CC.getX() - (inside_x + aR.step)) * (CC.getX() - (inside_x + aR.step)) +
                                                        ((CC.getY() - (inside_y - aR.step)) * (CC.getY() - (inside_y - aR.step))));
                            whichOne = 6;
                                            }
                        else if(CC.getX() < inside_x && CC.getY() < inside_y-aR.step) {
                                                    disti = Math.sqrt((CC.getX() - (inside_x)) * (CC.getX() - (inside_x)) +
                                                            ((CC.getY() - (inside_y - aR.step)) * (CC.getY() - (inside_y - aR.step))));
                            whichOne = 7;
                                                }
                        else if(CC.getX() < inside_x && CC.getY() >= inside_y) {
                                                        disti = Math.sqrt((CC.getX() - (inside_x)) * (CC.getX() - (inside_x)) +
                                                                ((CC.getY() - (inside_y)) * (CC.getY() - (inside_y))));
                            whichOne = 8;
                                                    }

                        if(disti < CC.getRadius()){

                            /* These MIGHT also be bad, just to be sure we eliminate these also */
                            if(CC.getRadius() > (this.expansion - disti)){

                                condition2++;

                            }

                        }

                        tin2.add(e.getA());
                        tin2.add(f.getA());
                        tin2.add(r.getA());

                    }
                }
            }
        }
    }

    private void setMarkBit(BitSet bitset, final IQuadEdge edge) {
        int index = edge.getIndex() ;
        bitset.set(index);
    }


    /**
     * Subtracts the TIN elevation from points in
     * point cloud.
     *
     * Returns if TIN is degenerate or non-existent.
     *
     * @param outputFile2 	Name of the output las file
     * @param rule 			Point includion rule
     * @param otype 			Type of output file, las or txt (deprecated)
     */

    public void normalizeZ(String outputFile2, PointInclusionRule rule, String otype) throws Exception{

        if(aR.ground_class == -1)
            this.aR.p_update.lasheight_groundClass = 2;
        else
            this.aR.p_update.lasheight_groundClass = aR.ground_class;

        this.progress_end = (int)pointCloud.getNumberOfPointRecords();

        aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber-1] = 0;
        aR.p_update.threadInt[coreNumber-1] = 0;

        aR.p_update.updateProgressNormalize();
        //this.updateProgress_normalize();

        if(!tin.isBootstrapped()){
            createTin(2);

        }

        if(!tin.isBootstrapped()){
            return;
        }


        org.tinfour.interpolation.TriangularFacetInterpolator polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);

        LasPoint tempPoint = new LasPoint();
        long n = pointCloud.getNumberOfPointRecords();

        int maxi;

        pointWriterMultiThread pw = new pointWriterMultiThread(this.outWriteFile, pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        double distance = 0.0;

        int thread_n = aR.pfac.addReadThread(pointCloud);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        List<IQuadEdge> perimeter = tin.getPerimeter();

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                //pointCloud.readRecord_noRAF(i, tempPoint, 10000);
                aR.pfac.prepareBuffer(thread_n, i, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!rule.ask(tempPoint, i+j, true)){
                    continue;
                }

                this.progress_current++;
                aR.p_update.threadProgress[coreNumber-1] = this.progress_current;

                //if(tin.isPointInsideTin(tempPoint.x, tempPoint.y)) {
               // if(isPointInPolygon(perimeter, tempPoint.x, tempPoint.y) == Polyside.Result.Inside) {
                    distance = (tempPoint.z - polator.interpolate(tempPoint.x, tempPoint.y, valuator));
                    //distance = 0.0;

                    if(!Double.isNaN(distance)) {
                        tempPoint.z = distance;
                        //buf.writePoint(tempPoint, aR.inclusionRule, i);
                        aR.pfac.writePoint(tempPoint, i + j, thread_n);
                    }else{
                        this.pointsOutsideTin++;
                        aR.p_update.threadInt[coreNumber-1] = this.pointsOutsideTin;
                    }

                    if (progress_current % 10000 == 0) {
                        aR.p_update.updateProgressNormalize();
                        //this.updateProgress_normalize();
                    }
                //}else {
               //     this.pointsOutsideTin++;
               //     aR.p_update.threadInt[coreNumber-1] = this.pointsOutsideTin;
               // }

            }
        }

        aR.pfac.closeThread(thread_n);

        aR.p_update.lasground_fileProgress++;
        aR.p_update.updateProgressNormalize();

    }

    /**
     * Subtracts the TIN elevation from points in
     * point cloud. Can be used to subtract from TIN
     * generated from another file.
     *
     * Returns if TIN is degenerate or non-existent.
     *
     * @param groundClassification 	class of ground
     * @param outputFile2 			Name of the output las file
     * @param rule 					Point includion rule
     * @param otype 					Type of output file, las or txt (deprecated)
     * @param groundPoints 			External file to be used for TIN
     */

    public void normalizeZ(int groundClassification, String outputFile2, PointInclusionRule rule, String otype, String groundPoints) throws Exception{

        if(aR.ground_class == -1)
            this.aR.p_update.lasheight_groundClass = 2;
        else
            this.aR.p_update.lasheight_groundClass = aR.ground_class;

        this.progress_end = (int)pointCloud.getNumberOfPointRecords();

        aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber-1] = 0;
        aR.p_update.threadInt[coreNumber-1] = 0;
        aR.p_update.updateProgressNormalize();

        double interpolatedvalue = 0;

        if(groundPoints.equals("-999")){

            LasPoint tempPoint = new LasPoint();

            if(!tin.isBootstrapped())
                createTin(groundClassification);

            if(!tin.isBootstrapped()){

                throw new toolException("TIN is not bootstrapped. There are not enough ground points with class " + aR.ground_class + "" +
                        "to construct a TIN.");

            }

            org.tinfour.interpolation.TriangularFacetInterpolator polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);

            long n = pointCloud.getNumberOfPointRecords();

            int maxi;

            double distance = 0.0;

            int thread_n = aR.pfac.addReadThread(pointCloud);

            pointWriterMultiThread pw = new pointWriterMultiThread(this.outWriteFile, pointCloud, "las2las", aR);
            LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
            aR.pfac.addWriteThread(thread_n, pw, buf);

            List<IQuadEdge> perimeter = tin.getPerimeter();

            int counter_debug = 0;

            for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

                maxi = (int) Math.min(10000, pointCloud.getNumberOfPointRecords() - i);

                aR.pfac.prepareBuffer(thread_n, i, 10000);

                for (int j = 0; j < maxi; j++) {

                    pointCloud.readFromBuffer(tempPoint);

                    /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                    if(!rule.ask(tempPoint, i+j, true)){
                        continue;
                    }

                    this.progress_current++;

                    aR.p_update.threadProgress[coreNumber-1] = this.progress_current;

                    //if(isPointInPolygon(perimeter, tempPoint.x, tempPoint.y) == Polyside.Result.Inside) {

                        counter_debug++;

                        distance = (tempPoint.z - polator.interpolate(tempPoint.x, tempPoint.y, valuator));
                        //distance = 0.0;

                        if(!Double.isNaN(distance)) {
                            tempPoint.z = distance;
                            //buf.writePoint(tempPoint, aR.inclusionRule, i);
                            aR.pfac.writePoint(tempPoint, i + j, thread_n);
                        }else{
                            this.pointsOutsideTin++;
                            aR.p_update.threadInt[coreNumber-1] = this.pointsOutsideTin;
                        }

                        if (progress_current % 10000 == 0) {
                            aR.p_update.updateProgressNormalize();
                            //this.updateProgress_normalize();
                        }
                    //}else {
                    //    this.pointsOutsideTin++;
                    //    aR.p_update.threadInt[coreNumber - 1] = this.pointsOutsideTin;
                    //}

                }
            }

            aR.pfac.closeThread(thread_n);

        }

        else{

            groundPointFile = new File(groundPoints);

            createTinFromFile(groundPointFile, 2);

            if(!tin.isBootstrapped())
                createTin(groundClassification);

            if(!tin.isBootstrapped()){
                return;
            }

            org.tinfour.interpolation.TriangularFacetInterpolator polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);

            File outputFile = this.outWriteFile;// new File(outputFileName);

            if(outputFile.exists())
                outputFile.delete();

            outputFile.createNewFile();

            LasPoint tempPoint = new LasPoint();

            long n = pointCloud.getNumberOfPointRecords();

            int maxi;

            double distance = 0.0;
            pointWriterMultiThread pw = new pointWriterMultiThread(this.outWriteFile, pointCloud, "las2las", aR);

            LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

            List<IQuadEdge> perimeter = tin.getPerimeter();
            for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

                maxi = (int) Math.min(10000, pointCloud.getNumberOfPointRecords() - i);

                try {
                    pointCloud.readRecord_noRAF(i, tempPoint, 10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (int j = 0; j < maxi; j++) {

                    pointCloud.readFromBuffer(tempPoint);

                    /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                    if(!rule.ask(tempPoint, i+j, true)){
                        continue;
                    }

                    this.progress_current++;
                    aR.p_update.threadProgress[coreNumber-1] = this.progress_current;

                    if(isPointInPolygon(perimeter, tempPoint.x, tempPoint.y) == Polyside.Result.Inside) {
                        interpolatedvalue = polator.interpolate(tempPoint.x, tempPoint.y, valuator);

                        distance = (tempPoint.z - interpolatedvalue);

                        tempPoint.z = distance;

                        buf.writePoint(tempPoint, aR.inclusionRule, i);

                        if (progress_current % 10000 == 0) {
                            aR.p_update.updateProgressNormalize();
                        }
                    }else {
                        this.pointsOutsideTin++;
                        aR.p_update.threadInt[coreNumber - 1] = this.pointsOutsideTin;
                    }
                }
            }

            buf.close();
            buf.pwrite.close(aR);
        }
        aR.p_update.lasground_fileProgress++;
        aR.p_update.updateProgressNormalize();
    }


    /**
     * Creates a deluney TIN from point cloud.
     *
     * @param groundClassification 	Ground class
     */

    public void createTin(int groundClassification) throws Exception{

        LasPoint tempPoint = new LasPoint();

        int thread_n = aR.pfac.addReadThread(pointCloud);

        double decimate_res = aR.decimate_tin;

        boolean decimate = decimate_res > 0;
        List<Vertex> closest;
        Vertex closest_vertex;

        boolean tin_is_bootstrapped = false;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!rule.ask(tempPoint, i+j, true)){
                    continue;
                }

                if(tempPoint.classification == groundClassification) {

                    if(decimate && tin_is_bootstrapped){

                        closest = tin.getNeighborhoodPointsCollector().collectNeighboringVertices(tempPoint.x, tempPoint.y, 0, 0);
                        closest_vertex = closest.get(0);

                        if(euclideanDistance(tempPoint.x, tempPoint.y, closest_vertex.x, closest_vertex.y) > decimate_res){
                            tin.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));

                        }

                    }else{
                        tin.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                        if(!tin_is_bootstrapped){
                            tin_is_bootstrapped = tin.isBootstrapped();
                        }
                    }
                }

            }
        }
    }

    /**
     * Creates a deluney TIN from external point cloud.
     *
     * @param in 					External point cloud
     * @param groundClassification 	Ground class
     */

    public void createTinFromFile(File in, int groundClassification) throws IOException{

        LasPoint tempPoint = new LasPoint();

        LASReader temp = new LASReader(in);

        long n = temp.getNumberOfPointRecords();



        for(int i = 0; i < n; i++){

            temp.readRecord(i, tempPoint);

            if(tempPoint.classification == groundClassification)
                tin.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));

        }

    }

}
