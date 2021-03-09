package utils;

import LASio.LasPoint;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import org.jblas.DoubleMatrix;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import java.awt.geom.Point2D;

public class residFunctionStemAlign implements LevenbergMarquardt.ResidualFunction  {

    boolean n_residuals_checked = false;
    int n_residuals = 1;
    int n_residuals_circleWise = 1;
    int n_residuals_trunk_centers = 1;

    double[] x_segments;
    TreeMap<Integer,double[]> x_segments_list;
    double[] y_x;
    TreeMap<Integer,double[]>  y_x_list;
    double[] y_y;
    TreeMap<Integer,double[]>  y_y_list;
    double[] y_z;
    TreeMap<Integer,double[]>  y_z_list;
    double[] y_scale;
    TreeMap<Integer,double[]>  y_scale_list;

    double[] y_x_rot;
    TreeMap<Integer,double[]>  y_x_rot_list;
    double[] y_y_rot;
    TreeMap<Integer,double[]>  y_y_rot_list;
    double[] y_z_rot;
    TreeMap<Integer,double[]>  y_z_rot_list;


    TreeMap<Integer,double[]>  y_penalty_list;

    public int numberOfCircles = 0;

    SplineInterpolator interpolatori = new SplineInterpolator();
    PolynomialSplineFunction po_x_rot;
    TreeMap<Integer,PolynomialSplineFunction> po_x_rot_list;
    PolynomialSplineFunction[] po_x_rot_sparseArray;
    PolynomialSplineFunction po_y_rot;
    TreeMap<Integer,PolynomialSplineFunction> po_y_rot_list;
    PolynomialSplineFunction[] po_y_rot_sparseArray;
    PolynomialSplineFunction po_z_rot;
    TreeMap<Integer,PolynomialSplineFunction> po_z_rot_list;
    PolynomialSplineFunction[] po_z_rot_sparseArray;
    PolynomialSplineFunction po_x;
    TreeMap<Integer,PolynomialSplineFunction> po_x_list;
    PolynomialSplineFunction[] po_x_sparseArray;
    PolynomialSplineFunction po_y;
    TreeMap<Integer,PolynomialSplineFunction> po_y_list;
    PolynomialSplineFunction[] po_y_sparseArray;
    PolynomialSplineFunction po_z;
    TreeMap<Integer,PolynomialSplineFunction> po_z_list;
    PolynomialSplineFunction[] po_z_sparseArray;

    TreeMap<Integer,PolynomialSplineFunction> po_penalty_list;
    PolynomialSplineFunction[] po_penalty_sparseArray;

    PolynomialSplineFunction po_scale;
    TreeMap<Integer,PolynomialSplineFunction> po_scale_list;
    PolynomialSplineFunction[] po_scale_sparseArray;

    DoubleMatrix tempMatrix = new DoubleMatrix(3,3);
    SimpleMatrix tempMatrix_fast = new SimpleMatrix(3,3);

    double[] tempTrans = new double[3];
    int numberOfSegments = 0;
    int numFunctions = 1;
    double diameter;
    double deltaT;

    int n_good_slices = 0;

    public double meanRadius = 0;

    int n_flightlines;

    double new_x;
    double new_y;
    double new_z;

    double[] locationOfAircraft;

    Map.Entry<Double, double[]> hehe;

    DoubleMatrix pointMatrix = new DoubleMatrix(1,3);
    SimpleMatrix pointMatrix_fast = new SimpleMatrix(1,3);
    DoubleMatrix rotatedPointMatrix = new DoubleMatrix(1,3);
    SimpleMatrix rotatedPointMatrix_fast = new SimpleMatrix(1,3);

    TreeMap<Short, HashMap<Integer, ArrayList<cloudPoint>>> trunkPoints;
    TreeMap<Double,double[]> trajectory = new TreeMap<>();


    ArrayList<ArrayList<cloudPoint>> slicesToBeOptimized;
    ArrayList<ArrayList<cloudPoint>> slicesToBeOptimized_for_residual;
    ArrayList<TreeMap<Integer, ArrayList<cloudPoint>>> slicesToBeOptimized_for_residual_map;
    //ArrayList<TreeMap<Integer, ArrayList<cloudPoint>>> slicesToBeOptimized_for_residual_map2;
    ArrayList<double[]> original_fit_for_slice;

    //ArrayList<ArrayList<cloudPoint>> trunkPoints_for_residual;
    ArrayList<TreeSet<cloudPoint>> trunkPoints_for_residual;
    double start_time, end_time;
    TreeMap<Integer, double[]> flightLines = new TreeMap<>();

    DMatrixRMaj final_param;

    public boolean initialized = false;
    ArrayList<Integer> flightLineIds;

    TreeMap<Integer, strip> stripInformation;

    SimpleRegression[] trunkRegressions;
    double[] trunkRegressionPenalty;

    ArrayList<Integer> slices_trunk_ids;
    ArrayList<Double> slices_z;

    public boolean writeToFile = false;
    File fout;
    FileOutputStream fos;

    BufferedWriter bw;

    ArrayList<Double> original_radius = new ArrayList<>();

    public double[] thisCosts = new double[2];

    int maxStripId = -1;

    //public residFunctionStemAlign(double deltaT, ArrayList<Integer> flightLineIds, ArrayList<Integer> n_segments_per_flightline, ArrayList<Integer> flightLineParamsStartFrom){
    public residFunctionStemAlign(double deltaT, TreeMap<Integer, strip> stripInformation){

        slices_z = new ArrayList<>();
        slices_trunk_ids = new ArrayList<>();
        this.stripInformation = stripInformation;

        this.n_flightlines = stripInformation.size();
        this.deltaT = deltaT;

        po_x_rot_list = new TreeMap<>();
        po_y_rot_list = new TreeMap<>();
        po_z_rot_list = new TreeMap<>();

        po_penalty_list = new TreeMap<>();

        y_x_rot_list = new TreeMap<>();
        y_y_rot_list = new TreeMap<>();
        y_z_rot_list = new TreeMap<>();

        y_penalty_list = new TreeMap<>();

        y_scale_list = new TreeMap<>();

        y_x_list = new TreeMap<>();
        y_y_list = new TreeMap<>();
        y_z_list = new TreeMap<>();

        po_x_list = new TreeMap<>();
        po_y_list = new TreeMap<>();
        po_z_list = new TreeMap<>();

        po_scale_list = new TreeMap<>();

        x_segments_list = new TreeMap<>();
        this.flightLineIds = new ArrayList<>();
        this.maxStripId = stripInformation.lastEntry().getKey();

        po_x_rot_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        po_y_rot_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        po_z_rot_sparseArray = new PolynomialSplineFunction[maxStripId+1];

        po_scale_sparseArray = new PolynomialSplineFunction[maxStripId+1];

        po_penalty_sparseArray = new PolynomialSplineFunction[maxStripId+1];

        po_x_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        po_y_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        po_z_sparseArray = new PolynomialSplineFunction[maxStripId+1];

        for(Map.Entry<Integer, strip> entry : stripInformation.entrySet()){

            this.flightLineIds.add(entry.getKey());
            po_x_rot_list.put(entry.getKey(), null);
            po_y_rot_list.put(entry.getKey(), null);
            po_z_rot_list.put(entry.getKey(), null);

            po_x_list.put(entry.getKey(), null);
            po_y_list.put(entry.getKey(), null);
            po_z_list.put(entry.getKey(), null);

            po_penalty_list.put(entry.getKey(), null);

            po_scale_list.put(entry.getKey(), null);

            x_segments_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);

            y_x_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);
            y_y_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);
            y_z_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);

            y_scale_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);
            y_penalty_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);

            y_x_rot_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);
            y_y_rot_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);
            y_z_rot_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);


        }

    }

    public void setNumFunctions(int in){

        this.numFunctions = in;

    }

    public void setTrunkPoints(TreeMap<Short, HashMap<Integer, ArrayList<cloudPoint>>> in) throws Exception{

        trunkPoints_for_residual = new ArrayList<>();

        this.trunkPoints = in;

        trunkRegressions = new SimpleRegression[(trunkPoints.lastKey())+1];
        trunkRegressionPenalty = new double[(trunkPoints.lastKey())+1];

        for(int i = 0; i < trunkRegressions.length; i++){

            trunkRegressions[i] = new SimpleRegression(true);

        }

        for (Map.Entry<Short, HashMap<Integer, ArrayList<cloudPoint>>> entry : trunkPoints.entrySet()) {

            trunkPoints_for_residual.add(new TreeSet<cloudPoint>());

            for (Map.Entry<Integer, ArrayList<cloudPoint>> entry2 : entry.getValue().entrySet()) {

                for(cloudPoint c : entry2.getValue()){
                    cloudPoint c_clone = new cloudPoint(c.x, c.y, c.z, c.dz, c.t, c.id, c.trunk_id);
                    trunkPoints_for_residual.get(trunkPoints_for_residual.size() - 1).add(c_clone);

                    if(!flightLines.containsKey(c.id))
                        flightLines.put(c.id, new double[6]);

                }
            }
        }
    }

    public double getSomething(){

        return diameter;

    }

    /**
     * Number of functions in output
     * @return function count
     */
    public int numFunctions(){

        return this.numFunctions;

    }

    public double[] calculateCost(DMatrixRMaj param, DMatrixRMaj residual, boolean writeToFile){

        this.writeToFile = writeToFile;

        if(this.writeToFile){

            fout = new File("po_s.txt");

            try {
                fout.createNewFile();
                fos = new FileOutputStream(fout);
            }catch (Exception e){
                e.printStackTrace();
            }

            bw = new BufferedWriter(new OutputStreamWriter(fos));
        }

        double resid = 0.0;
        compute(param, residual);
        resid = residual.get(0,0);
        this.writeToFile = false;
        return new double[]{residual.get(0,0), residual.get(0,0)};
    }



    public void createFlightLineSpecificCorrection(DMatrixRMaj param){

        int number = 0;

        for (Map.Entry<Integer, double[]> entry : flightLines.entrySet()) {

            //System.out.println(number * 6 + 0);
            entry.getValue()[0] = param.get(number * 6 + 0, 0);
            entry.getValue()[1] = param.get(number * 6 + 1, 0);
            entry.getValue()[2] = param.get(number * 6 + 2, 0);
            entry.getValue()[3] = param.get(number * 6 + 3, 0);
            entry.getValue()[4] = param.get(number * 6 + 4, 0);
            entry.getValue()[5] = param.get(number * 6 + 5, 0);
            number++;

        }
    }

    public void prep(DMatrixRMaj param){

        this.final_param = param;
        this.numberOfSegments = param.numRows / 6;
        createPolynomialSplines(param);

        double[] resid2;

        if(!initialized)
        for (Map.Entry<Short, HashMap<Integer, ArrayList<cloudPoint>>> entry : trunkPoints.entrySet()) {

            //counter2 = 0;
            trunkPoints_for_residual.add(new TreeSet<cloudPoint>());

            for (Map.Entry<Integer, ArrayList<cloudPoint>> entry2 : entry.getValue().entrySet()) {

                for (cloudPoint c : entry2.getValue()) {

                    pointMatrix.put(0, 0, c.x);
                    pointMatrix.put(0, 1, c.y);
                    pointMatrix.put(0, 2, c.z);

                    rotatePoint2(pointMatrix, c.t, c.id);

                    new_x = pointMatrix.get(0, 0) + tempTrans[0];
                    new_y = pointMatrix.get(0, 1) + tempTrans[1];
                    new_z = pointMatrix.get(0, 2) + tempTrans[2];

                    pointMatrix.put(0, 0, new_x);
                    pointMatrix.put(0, 1, new_y);
                    pointMatrix.put(0, 2, new_z);

                    cloudPoint c_t = new cloudPoint(new_x, new_y, new_z, c.dz, c.t, c.id, c.trunk_id);
                    //cloudPoint c_t2 = new cloudPoint(c.getX(), c.getY(), c.getZ(), c.getT());
                    //c_t.print();
                    //c_t2.print();
                    //System.out.println("------------");
                    trunkPoints_for_residual.get(trunkPoints_for_residual.size() - 1).add(c_t);
                    //counter2++;
                }
            }
            //counter++;
        }

        /** Calculate residual based on new point locations */
        try {
            resid2 = calculateResidual(null);
            //long estimatedTime = System.currentTimeMillis() - startTime;
            //System.out.println("Elapsed time: " + estimatedTime + "ms");
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("num_functions1: " + this.numFunctions());

        this.initialized = true;
    }

    public void count_n_points(){

    }


    public void rotateLasPoint(LasPoint p){

        pointMatrix.put(0, 0, p.x);
        pointMatrix.put(0, 1, p.y);
        pointMatrix.put(0, 2, p.z);

        rotatePoint2(pointMatrix, p.gpsTime, p.userData);

        new_x = pointMatrix.get(0, 0) + tempTrans[0];
        new_y = pointMatrix.get(0, 1) + tempTrans[1];
        new_z = pointMatrix.get(0, 2) + tempTrans[2];

        p.x = new_x;
        p.y = new_y;
        p.z = new_z;
    }

    public void compute(DMatrixRMaj param, DMatrixRMaj residual) {

        this.numberOfSegments = param.numRows / 6;

        SplineInterpolator interpolatori = new SplineInterpolator();

        PolynomialSplineFunction[] po_x_rot_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        PolynomialSplineFunction[] po_y_rot_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        PolynomialSplineFunction[] po_z_rot_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        PolynomialSplineFunction[] po_x_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        PolynomialSplineFunction[] po_y_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        PolynomialSplineFunction[] po_z_sparseArray = new PolynomialSplineFunction[maxStripId+1];

        long startTime = System.currentTimeMillis();

        createPolynomialSplines2(param, po_x_rot_sparseArray,
                                    po_y_rot_sparseArray,
                                    po_z_rot_sparseArray,
                                    po_x_sparseArray,
                                    po_y_sparseArray,
                                    po_z_sparseArray,
                                    interpolatori);


        long estimatedTime = System.currentTimeMillis() - startTime;

        ArrayList<ArrayList<double[]>>[] rotated_points = new ArrayList[this.n_good_slices];

        trunkPoints_for_residual.clear();

        int counter = 0;
        int counter2 = 0;
        cloudPoint c2;

        if(!initialized) {

            this.po_x_rot_sparseArray = po_x_rot_sparseArray;
                    this.po_y_rot_sparseArray = po_y_rot_sparseArray;
                    this.po_z_rot_sparseArray = po_z_rot_sparseArray;
                    this.po_x_sparseArray = po_x_sparseArray;
                    this.po_y_sparseArray = po_y_sparseArray;
                    this.po_z_sparseArray = po_z_sparseArray;

            /** Change the coordinates of the trunk points based on params */
            for (Map.Entry<Short, HashMap<Integer, ArrayList<cloudPoint>>> entry : trunkPoints.entrySet()) {

                counter2 = 0;
                trunkPoints_for_residual.add(new TreeSet<cloudPoint>());

                for (Map.Entry<Integer, ArrayList<cloudPoint>> entry2 : entry.getValue().entrySet()) {

                    for (cloudPoint c : entry2.getValue()) {

                        pointMatrix.put(0, 0, c.x);
                        pointMatrix.put(0, 1, c.y);
                        pointMatrix.put(0, 2, c.z);

                        rotatePoint2(pointMatrix, c.t, c.id);

                        new_x = pointMatrix.get(0, 0) + tempTrans[0];
                        new_y = pointMatrix.get(0, 1) + tempTrans[1];
                        new_z = pointMatrix.get(0, 2) + tempTrans[2];

                        pointMatrix.put(0, 0, new_x);
                        pointMatrix.put(0, 1, new_y);
                        pointMatrix.put(0, 2, new_z);

                        cloudPoint c_t = new cloudPoint(new_x, new_y, new_z, c.dz, c.t, c.id, c.trunk_id);

                        trunkPoints_for_residual.get(trunkPoints_for_residual.size() - 1).add(c_t);

                    }
                }

            }
        }else{
            ArrayList<cloudPoint> tempList;
            int tempKey;

            int counter1 = 0;

            for(int i = 0; i < slicesToBeOptimized_for_residual_map.size(); i++) {
                int debugConter = 0;

                if(slicesToBeOptimized_for_residual.get(i).size() == 0)
                    continue;

                rotated_points[counter1] = new ArrayList<>();

                for (Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map.get(i).entrySet()) {

                    tempList = entry.getValue();

                    rotated_points[counter1].add(new ArrayList<>());

                    for(int p = 0; p < tempList.size(); p++){

                        c2 = tempList.get(p);

                        double[] rotateddValue = new double[]{0, 0, 0};

                        rotatePoint3_cp(c2, c2.t, c2.id, po_x_rot_sparseArray,
                                po_y_rot_sparseArray,
                                po_z_rot_sparseArray,
                                po_x_sparseArray,
                                po_y_sparseArray,
                                po_z_sparseArray,
                                rotateddValue);

                        rotated_points[counter1].get(rotated_points[counter1].size()-1).add(rotateddValue);

                        debugConter++;


                    }

                }
                counter1++;
            }

            estimatedTime = System.currentTimeMillis() - startTime;

            //System.out.println("Rotating: " + estimatedTime + "ms");
        }

        double resid = 10.0;
        double[] resid2 = new double[]{0,0};

        //startTime = System.currentTimeMillis();



        /** Calculate residual based on new point locations */
        try {
            resid2 = calculateResidual(rotated_points);
            estimatedTime = System.currentTimeMillis() - startTime;
            //System.out.println("Time on residualCount: " + estimatedTime + "ms");
        }catch (Exception e){
            e.printStackTrace();
        }


        initialized = true;

        for(int i = 0; i < resid2.length-1; i++){
            residual.set(i, 0, resid2[i]);
        }

        estimatedTime = System.currentTimeMillis() - startTime;

    }

    double distance3d(double x1, double y1, double z1, double x2, double y2, double z2){

        return Math.sqrt( (x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) + (z1 - z2)*(z1 - z2) );

    }

    public int[] calculatePointsInQuadrants(ArrayList<Point2D.Double> slicePoints, double center_x, double center_y, double radius){

        int[] output = new int[]{0,0,0,0};

        for(int i = 0; i < slicePoints.size(); i++){

            if(slicePoints.get(i).x < center_x && slicePoints.get(i).y > center_y){
                output[0]++;
            }
            if(slicePoints.get(i).x > center_x && slicePoints.get(i).y > center_y){
                output[1]++;
            }
            if(slicePoints.get(i).x > center_x && slicePoints.get(i).y < center_y){
                output[2]++;
            }
            if(slicePoints.get(i).x < center_x && slicePoints.get(i).y < center_y){
                output[3]++;
            }
        }

        return output;

    }

    public double[] calculateResidual(ArrayList<ArrayList<double[]>>[] rotated_points) throws Exception{

        double highest_cost = Double.NEGATIVE_INFINITY;
        int highest_cost_index = 0;

        int iter = 250;

        int resid_counter = 0;
        int resid_counter_slicewise = 0;

        numberOfCircles = 0;

        SimpleRegression SR = new SimpleRegression(true);

        DecimalFormat format =
                new DecimalFormat("000.00000000",
                        new DecimalFormatSymbols(Locale.US));
        CircleFitter fitter = new CircleFitter();

        RANSAC_circleFitter fitter_ransac = new RANSAC_circleFitter();

        double[] output = new double[n_residuals_trunk_centers+1];

        //double output = 0;
        ArrayList<Point2D.Double> slicePoints = new ArrayList<>();

        double x_sum = 0, y_sum = 0;
        //double center_x, center_y;

        double minx = Double.POSITIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;

        double maxx = Double.NEGATIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        int counter = 0;
        //int counter2 = 0;
        double residualSum = 0;
        double residualSum2 = 0;
        double counter2 = 0;

        double maxXdim = Double.NEGATIVE_INFINITY;
        double maxYdim = Double.NEGATIVE_INFINITY;

        ArrayList<cloudPoint> tempList = new ArrayList<>();

        double radiusSum = 0.0d;
        double radiusCount = 0.0;

        int counterTrunk = 0;

        int good = 0, bad = 0;

        for(int i = 0; i < trunkRegressions.length; i++) {

            trunkRegressionPenalty[i] = 1.0d;
            trunkRegressions[i].clear();
        }

        double penalty = 0.0;

        double sum_sd_centers = 0;
        double sum_sd = 0;
        double sd_count = 0;

        HashSet<Integer> goodTrunks = new HashSet<>();

        if(!initialized) {

            original_fit_for_slice = new ArrayList<>();

            slicesToBeOptimized = new ArrayList<>();

            for (TreeSet<cloudPoint> s : trunkPoints_for_residual) {

                slicePoints.clear();

                double current_z = s.first().z;

                minx = Double.POSITIVE_INFINITY;
                miny = Double.POSITIVE_INFINITY;

                maxx = Double.NEGATIVE_INFINITY;
                maxy = Double.NEGATIVE_INFINITY;
                x_sum = 0;
                y_sum = 0;

                tempList.clear();
                counterTrunk++;

                for (cloudPoint c : s) {


                    slicePoints.add(new Point2D.Double(c.x, c.y));

                    tempList.add(c);

                    x_sum += c.x;
                    y_sum += c.y;

                    if (c.x > maxx)
                        maxx = c.x;
                    if (c.y > maxy)
                        maxy = c.y;

                    if (c.x < minx)
                        minx = c.x;
                    if (c.y < miny)
                        miny = c.y;

                    if (c.z >= current_z + 0.5) {

                        current_z = c.z;


                        if (slicePoints.size() < 10) {

                            x_sum = 0;
                            y_sum = 0;
                            slicePoints.clear();
                            tempList.clear();
                            minx = Double.POSITIVE_INFINITY;
                            miny = Double.POSITIVE_INFINITY;

                            maxx = Double.NEGATIVE_INFINITY;
                            maxy = Double.NEGATIVE_INFINITY;

                            continue;

                        }

                        Point2D.Double[] points =
                                (Point2D.Double[]) slicePoints.toArray(new Point2D.Double[slicePoints.size()]);

                        try {

                            fitter_ransac.initialize_arraylist(slicePoints, 10, 6, 0.02, 200);

                        } catch (Exception e) {
                            //e.printStackTrace();
                            x_sum = 0;
                            y_sum = 0;
                            slicePoints.clear();
                            tempList.clear();
                            minx = Double.POSITIVE_INFINITY;
                            miny = Double.POSITIVE_INFINITY;

                            maxx = Double.NEGATIVE_INFINITY;
                            maxy = Double.NEGATIVE_INFINITY;

                            continue;
                        }



                        int[] uads = calculatePointsInQuadrants(slicePoints, fitter_ransac.center_x, fitter_ransac.center_y, fitter_ransac.radius);

                        boolean baddy = false;
                        int sum = 0;

                        for(int o : uads){
                            sum += o;
                        }
                        for(int o : uads){
                            if((double)o / (double)sum < 0.05){
                                baddy = true;
                                break;
                            }
                        }

                        if(baddy){
                            x_sum = 0;
                            y_sum = 0;
                            slicePoints.clear();
                            tempList.clear();
                            minx = Double.POSITIVE_INFINITY;
                            miny = Double.POSITIVE_INFINITY;

                            maxx = Double.NEGATIVE_INFINITY;
                            maxy = Double.NEGATIVE_INFINITY;
                            continue;
                        }
                        x_sum = 0;
                        y_sum = 0;

                        minx = Double.POSITIVE_INFINITY;
                        miny = Double.POSITIVE_INFINITY;

                        maxx = Double.NEGATIVE_INFINITY;
                        maxy = Double.NEGATIVE_INFINITY;

                        if (fitter_ransac.radius < 0.5) {

                            slicesToBeOptimized.add(new ArrayList<>(tempList));
                            slices_trunk_ids.add(c.trunk_id);
                            trunkRegressions[c.trunk_id].addData(c.dz, fitter_ransac.radius);

                            goodTrunks.add(counterTrunk);

                            slices_z.add(c.dz);
                            original_fit_for_slice.add(new double[]{fitter_ransac.center_x, fitter_ransac.center_y, fitter_ransac.radius, fitter_ransac.cost});

                            double[] min_max = minMax(points);

                            if((min_max[1] - min_max[0]) > maxXdim)
                                maxXdim = (min_max[1] - min_max[0]);
                            if((min_max[3] - min_max[2]) > maxYdim)
                                maxYdim = (min_max[3] - min_max[2]);


                        }else{
                        }

                        slicePoints.clear();
                        tempList.clear();
                    }

                }
                tempList.clear();
            }

            slicesToBeOptimized_for_residual = new ArrayList<>();
            slicesToBeOptimized_for_residual_map = new ArrayList<>();
            //slicesToBeOptimized_for_residual_map2 = new ArrayList<>();
            slices_trunk_ids = new ArrayList<>();

            for(int i = 0; i < slicesToBeOptimized.size(); i++){

                slicesToBeOptimized_for_residual.add(new ArrayList<>());
                slicesToBeOptimized_for_residual_map.add(new TreeMap<>());
                //slicesToBeOptimized_for_residual_map2.add(new TreeMap<>());

                slices_trunk_ids.add(slicesToBeOptimized.get(i).get(0).trunk_id);

                for(int j = 0; j < slicesToBeOptimized.get(i).size(); j++){

                    slicesToBeOptimized_for_residual.get(i).add(slicesToBeOptimized.get(i).get(j).copy());

                    if(!slicesToBeOptimized_for_residual_map.get(slicesToBeOptimized_for_residual_map.size()-1).containsKey(slicesToBeOptimized.get(i).get(j).id)){
                        slicesToBeOptimized_for_residual_map.get(slicesToBeOptimized_for_residual_map.size()-1).put(slicesToBeOptimized.get(i).get(j).id, new ArrayList<>());
                        slicesToBeOptimized_for_residual_map.get(slicesToBeOptimized_for_residual_map.size()-1).get(slicesToBeOptimized.get(i).get(j).id).add(slicesToBeOptimized.get(i).get(j).copy());
                    }else{
                        slicesToBeOptimized_for_residual_map.get(slicesToBeOptimized_for_residual_map.size()-1).get(slicesToBeOptimized.get(i).get(j).id).add(slicesToBeOptimized.get(i).get(j).copy());

                    }

                }
            }

            ArrayList<Double> radii = new ArrayList<>();
            ArrayList<Integer> radii_FlightLines = new ArrayList<>();
            ArrayList<double[]> centers = new ArrayList<>();

            double distance = 0.0;
            double sd = 0.0, sd_center = 0.0;

            for(int i = 0; i < slicesToBeOptimized_for_residual.size(); i++){

                cloudPoint[] points =
                        (cloudPoint[]) slicesToBeOptimized_for_residual.get(i).toArray(new cloudPoint[slicesToBeOptimized_for_residual.get(i).size()]);

                // System.out.println("map size: " + slicesToBeOptimized_for_residual_map.get(i).size());
                //              System.out.println(slicesToBeOptimized_for_residual.get(i).size());

                //for(int j = 0; j < slicesToBeOptimized_for_residual_map.get(i).size(); j++){

                ArrayList<cloudPoint> newList = new ArrayList<>();
                radii.clear();
                radii_FlightLines.clear();
                centers.clear();
                int counter_debug = 0;
                ArrayList<Integer> hahhus = new ArrayList<>();
                ArrayList<cloudPoint> debugPoints = new ArrayList<>();

                for(Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map.get(i).entrySet()){
                    //System.out.println("HERE!!" + entry.getValue().size());
                    counter_debug++;
                    if(entry.getValue().size() > 5) {
                        cloudPoint[] points2 =
                                (cloudPoint[]) entry.getValue().toArray(new cloudPoint[entry.getValue().size()]);

                        try {


                            int d = (int)Math.max(1, (entry.getValue().size()) * 0.5);

                            fitter_ransac.initialize3(entry.getValue(), 3, d, 0.015, 10000, 1);

                            /** HERE WE REPLACE THE FLIGHT LINE SLICE WITH ONLY THE RANSAC INLIER POINTS! */
                            entry.setValue((ArrayList<cloudPoint>)fitter_ransac.ransac_inlier_points_c.clone());
                            //ArrayList<cloudPoint> newList = new ArrayList<>();

                            slicesToBeOptimized_for_residual_map.get(i).put(entry.getKey(), (ArrayList<cloudPoint>)fitter_ransac.ransac_inlier_points_c.clone());

                            if(fitter_ransac.ransac_inlier_points_c.size() > 0) {
                                radii.add(fitter_ransac.radius_ransac);
                                radii_FlightLines.add(entry.getValue().get(0).id);
                                centers.add(new double[]{fitter_ransac.center_x, fitter_ransac.center_y});
                                hahhus.add(counter_debug);
                                newList.addAll(entry.getValue());


                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }else{
                        entry.getValue().clear();
                        slicesToBeOptimized_for_residual_map.get(i).get(entry.getKey()).clear();
                    }
                }

                boolean not_good = false;


                if(radii.size() > 1) {

                    for(int f = 0; f < radii.size(); f++){
                        stripInformation.get(radii_FlightLines.get(f)).observed_slices++;
                    }
                     sd = sd2(radii);
/*

                       */

                    if(sd <= 0.05) {
/*
                        System.out.println(Arrays.toString(radii.toArray()));
                        System.out.println(sd);
                        System.out.println("-----------------------");
*/
                        sd_center = sd_centers(centers);
                        sd_count++;
                        resid_counter_slicewise++;
                        if (false)
                            if (i == 425) {
                                System.out.println("i: " + i + " " + Arrays.toString(hahhus.toArray()) + " " + sd_center);

                                for (int p = 0; p < debugPoints.size(); p++) {
                                    debugPoints.get(p).print();
                                }

                            }
                        //
                        //System.out.println("sd_center: " + sd_center);

                        sum_sd += sd;
                        sum_sd_centers += sd_center;
                    }
                    else{
                        slicesToBeOptimized_for_residual.get(i).clear();
                        for(Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map.get(i).entrySet()){
                            entry.getValue().clear();
                        }
                        for(Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map.get(i).entrySet()){
                            entry.getValue().clear();
                        }

                        continue;
                    }

                }else{
                    slicesToBeOptimized_for_residual.get(i).clear();
                    for(Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map.get(i).entrySet()){
                        entry.getValue().clear();
                    }
                    for(Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map.get(i).entrySet()){
                        entry.getValue().clear();
                    }

                    continue;
                }

                n_residuals_trunk_centers++;

                for(int p = 0; p < slicesToBeOptimized_for_residual.get(i).size(); p++){

                    for(int p2 = 0; p2 < radii.size(); p2++){

                        //output[resid_counter++] = Math.abs(euclideanDistance(slicesToBeOptimized_for_residual.get(i).get(p).x, slicesToBeOptimized_for_residual.get(i).get(p).y,
                          //      centers.get(p2)[0], centers.get(p2)[1]) - radii.get(p2));

                        residualSum2 += Math.abs(euclideanDistance(slicesToBeOptimized_for_residual.get(i).get(p).x, slicesToBeOptimized_for_residual.get(i).get(p).y,
                                      centers.get(p2)[0], centers.get(p2)[1]) - radii.get(p2));
                        counter2++;
                    }

                }

                    //fitter_ransac.initialize(points, 10, 6, 0.02, 200);
                    //System.out.println("non ransac: " + fitter_ransac.radius);
                    slicesToBeOptimized_for_residual.set(i, (ArrayList<cloudPoint>) newList.clone());
                    int d = (int)Math.max(5, (slicesToBeOptimized_for_residual.get(i).size() - 4) * 0.8);

                    //
                    fitter_ransac.initialize_arraylist_c(slicesToBeOptimized_for_residual.get(i), 4, d, 0.02, iter);
                    //System.out.println("ransac: " + fitter_ransac.radius);
                    //System.out.println("----------------");
                    if (Math.abs(fitter_ransac.cost) < 0.5 && Math.abs(fitter_ransac.radius) < 0.5 && radii.size() > 1) {
                        this.n_good_slices++;
                        //residualSum += fitter.J;
                        //residualSum += fitter.J;
                        trunkRegressions[slices_trunk_ids.get(i)].addData(slices_z.get(i), fitter_ransac.radius);
                        trunkRegressionPenalty[slices_trunk_ids.get(i)] = 1.0;
                        residualSum += fitter_ransac.cost;
                        radiusSum += fitter_ransac.radius;
                        radiusCount++;
                        //System.out.println(radiusSum);
                        counter++;
                        numberOfCircles++;

                        original_radius.add(fitter_ransac.radius);

                        if(n_residuals_checked)
                            for(int p = 0; p < fitter_ransac.costList.size(); p++) {
                                System.out.println(resid_counter + " " + n_residuals + " " + p + " / " + fitter_ransac.costList.size());
                                output[resid_counter++] = fitter_ransac.costList.get(p);

                            }
                        else
                            n_residuals += fitter_ransac.costList.size();

                        n_residuals_circleWise += fitter_ransac.costList.size() * radii.size();
                        //System.out.println(n_residuals);
/*
                        sd = sd(radii);
                        sd_center = sd_centers(centers);
                        sd_count++;

                        sum_sd += sd;
                        sum_sd_centers += sd_center;
                        */
                        good++;

                        if(fitter_ransac.cost > highest_cost){
                            highest_cost = fitter_ransac.cost;
                            highest_cost_index = i;
                        }

                        if(writeToFile){
                            String outString = fitter_ransac.center_x + "\t" + fitter_ransac.center_y + "\t" + fitter_ransac.radius;
                            bw.write(outString);
                            bw.newLine();
                            d = (int)Math.max(5, (slicesToBeOptimized_for_residual.get(i).size() - 4) * 0.4);
                            fitter_ransac.initialize3(slicesToBeOptimized_for_residual.get(i), 3, d, 0.02, iter, 1);
                            outString = fitter_ransac.center_x + "\t" + fitter_ransac.center_y + "\t" + fitter_ransac.radius_ransac;
                            bw.write(outString);
                            bw.newLine();

                            for(int j = 0; j < slicesToBeOptimized_for_residual.get(i).size(); j++){
                                String outString2 = slicesToBeOptimized_for_residual.get(i).get(j).x + "\t" + slicesToBeOptimized_for_residual.get(i).get(j).y;
                                bw.write(outString2);
                                bw.newLine();
                            }

                        }

                    }else{
                        bad++;
                        slicesToBeOptimized_for_residual.get(i).clear();
                    }

               // }

            }

            //slicesToBeOptimized_for_residual = (ArrayList<ArrayList<Point2D.Double>>)slicesToBeOptimized.clone();

        }else{
            ArrayList<Double> radii = new ArrayList<>();
            ArrayList<double[]> centers = new ArrayList<>();

            double sd = 0.0, sd_center = 0.0;

            ArrayList<double[]> fullSlicePoints = new ArrayList<>();

            for(int i = 0; i < rotated_points.length; i++){

                radii.clear();
                centers.clear();
                fullSlicePoints.clear();

                /** HERE WE HAVE ONE FLIGHT LINE POINTS FROM ONE SLICE */
                for(int p = 0; p < rotated_points[i].size(); p++){

                    if(rotated_points[i].get(p).size() == 0)
                        continue;

                    fitter_ransac.initialize_arraylist_custom(rotated_points[i].get(p), 3, 2, 0.01, iter);

                    radii.add(fitter_ransac.radius);
                    centers.add(new double[]{fitter_ransac.center_x, fitter_ransac.center_y});

                    //System.out.println(fitter_ransac.radius + " " + radii.size());
                    fullSlicePoints.addAll(rotated_points[i].get(p));
                }

                if(radii.size() > 1) {

                    sd = sd(radii);
                    sd_center = sd_centers(centers);

                    sd_count++;

                    sum_sd += sd;
                    sum_sd_centers += sd_center;

                }

                fitter_ransac.initialize_arraylist_custom(fullSlicePoints, 4, 2, 0.02, iter);
                residualSum += fitter_ransac.cost;
                radiusSum += fitter_ransac.radius;
                radiusCount++;
                output[resid_counter++] = sd_center;

            }
/*
            double sum_sd_centers_new = sum_sd_centers / (double)sd_count;
            double radius_new = radiusSum / (double)radiusCount;

            radiusSum = 0;
            radiusCount = 0;

            sum_sd_centers = 0;

            sum_sd = 0;
            sd_count = 0;
*/
            if(false)
            for(int i = 0; i < slicesToBeOptimized_for_residual.size(); i++){

                if(slicesToBeOptimized_for_residual.get(i).size() == 0)
                    continue;

                radii.clear();
                centers.clear();

                boolean debug = false;

                int counter_debug = 0;
                ArrayList<Integer> hahhus = new ArrayList<>();
                ArrayList<cloudPoint> debugPoints = new ArrayList<>();
                for(Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map.get(i).entrySet()){
                    //System.out.println("HERE!!" + entry.getValue().size());
                    counter_debug++;
                    if(entry.getValue().size() > 0) {
                        cloudPoint[] points2 =
                                (cloudPoint[]) entry.getValue().toArray(new cloudPoint[entry.getValue().size()]);

                        try {

                            int d = (int)Math.max(1, (entry.getValue().size() - 3) * 0.6);

                            fitter_ransac.initialize_arraylist_c(entry.getValue(), 3, d, 0.01, iter);

                            radii.add(fitter_ransac.radius);
                            centers.add(new double[]{fitter_ransac.center_x, fitter_ransac.center_y});
                            hahhus.add(counter_debug);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
                if(radii.size() > 1) {


                    sd = sd(radii);
                    sd_center = sd_centers(centers);

                    sd_count++;

                    sum_sd += sd;
                    sum_sd_centers += sd_center;

                }

                    //fitter_ransac.initialize(points, 10, 6, 0.02, 200);

                output[resid_counter++] = sd_center;

                    int d = (int)Math.max(5, (slicesToBeOptimized_for_residual.get(i).size() - 4) * 0.8);

                    //fitter_ransac.initialize2(slicesToBeOptimized_for_residual.get(i), 4, d, 0.02, iter, 1);
                    fitter_ransac.initialize_arraylist_c(slicesToBeOptimized_for_residual.get(i), 4, d, 0.02, iter);

                //if (Math.abs(fitter_ransac.cost) < 0.5 && Math.abs(fitter_ransac.radius) < 0.5) {

                        trunkRegressions[slices_trunk_ids.get(i)].addData(slices_z.get(i), fitter_ransac.radius);
                        trunkRegressionPenalty[slices_trunk_ids.get(i)] = 1.0;
                        residualSum += fitter_ransac.cost;
                        radiusSum += fitter_ransac.radius;
                        radiusCount++;
                        counter++;
                        numberOfCircles++;

                    if(fitter_ransac.cost > highest_cost){
                        highest_cost = fitter_ransac.cost;
                        highest_cost_index = i;
                    }

                        good++;




            }


        }
        this.meanRadius = radiusSum / radiusCount;

        if(true && !n_residuals_checked) {
            output[0] = residualSum / (double)good;

            thisCosts[0] = output[0];
            thisCosts[1] = sum_sd_centers / sd_count;
            this.setNumFunctions(n_residuals);
            this.setNumFunctions(n_residuals_circleWise);
            this.setNumFunctions(n_residuals_trunk_centers);
        }else{

        }

        if(writeToFile){
            bw.close();
        }

        thisCosts[0] = residualSum / (double)radiusCount;

        thisCosts[1] = sum_sd_centers / sd_count;

        this.n_residuals_checked = true;

        return output;
    }

    public static double mean (ArrayList<Double> table)
    {
        int total = 0;

        for ( int i= 0;i < table.size(); i++)
        {
            double currentNum = table.get(i);
            total+= currentNum;
        }
        return total/table.size();
    }

    public static double sd2(ArrayList<Double> sd) {

        double sum = 0;
        double newSum = 0;

        for (int i = 0; i < sd.size(); i++) {
            sum = sum + sd.get(i);
        }
        double mean = (sum) / (double)(sd.size());

        for (int j = 0; j < sd.size(); j++) {
            // put the calculation right in there
            newSum = newSum + ((sd.get(j) - mean) * (sd.get(j) - mean));
        }
        double squaredDiffMean = (newSum) / (double)(sd.size());
        double standardDev = (Math.sqrt(squaredDiffMean));

        return standardDev;
    }

    public static double sd (ArrayList<Double> table)
    {

        double mean = mean(table);
        double sum = 0;

        for (double d : table) {

            sum += ((d-mean)*(d-mean));
        }

        return Math.sqrt( sum / ( table.size() - 1 ) ); // sample

    }

    public double sd_centers(ArrayList<double[]> points){

        double sd = 0.0d;
        double mean_x = 0, mean_y = 0;
        for(int i = 0; i < points.size(); i++){

            mean_x += points.get(i)[0];
            mean_y += points.get(i)[1];

        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        for(int i = 0; i < points.size(); i++){

            sd += euclideanDistance(mean_x, mean_y, points.get(i)[0], points.get(i)[1]);

        }

        sd /= (double)points.size();
        return sd;

    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }

    public double[] minMax(Point2D.Double[] points){

        double[] output = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, };

        for(Point2D.Double p : points){

            if(p.x < output[0])
                output[0] = p.x;
            if(p.x > output[1])
                output[1] = p.x;
            if(p.y < output[2])
                output[2] = p.y;
            if(p.y > output[3])
                output[3] = p.y;

        }

        return output;

    }

    public double[] minMax(ArrayList<cloudPoint> points){

        double[] output = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, };

        for(cloudPoint p : points){

            if(p.x < output[0])
                output[0] = p.x;
            if(p.x > output[1])
                output[1] = p.x;
            if(p.y < output[2])
                output[2] = p.y;
            if(p.y > output[3])
                output[3] = p.y;

        }

        return output;

    }

    public void setPointCloud(){



    }

    public void setStartTime(double time){

        this.start_time = time;
    }
    public void setEndTime(double time){
        this.end_time = time;
    }

    public void createPolynomialSplines(DMatrixRMaj param){

        double[] previousValues = new double[6];


        for(int ii = 0; ii < flightLineIds.size(); ii++){

            int i = flightLineIds.get(ii);
            strip strippi = stripInformation.get(i);

            int counti = 1;

            int segs = strippi.n_segments;
            int startFrom = strippi.flightLineParamsStartFrom * 6;
            double start_t = strippi.start_t;
            double end_t = strippi.end_t;

            for(int j = 0; j < segs; j++){

                x_segments_list.get(i)[counti] = start_t + j * deltaT;
                counti++;

            }

            x_segments_list.get(i)[0] = x_segments_list.get(i)[1] - 2;
            x_segments_list.get(i)[x_segments_list.get(i).length - 2] = end_t;
            x_segments_list.get(i)[x_segments_list.get(i).length - 1] = x_segments_list.get(i)[x_segments_list.get(i).length - 2] + 2;

            double[] x_seg = x_segments_list.get(i);
            
            counti = 1;

            previousValues[0] = param.get(startFrom + 0, 0);
            previousValues[1] = param.get(startFrom + 1, 0);
            previousValues[2] = param.get(startFrom + 2, 0);
            previousValues[3] = param.get(startFrom + 3, 0);
            previousValues[4] = param.get(startFrom + 4, 0);
            previousValues[5] = param.get(startFrom + 5, 0);

            y_x_rot_list.get(i)[0] = param.get(startFrom + 0, 0);
            y_y_rot_list.get(i)[0] = param.get(startFrom + 1, 0);
            y_z_rot_list.get(i)[0] = param.get(startFrom + 2, 0);


            y_x_list.get(i)[0] = param.get(startFrom + 3, 0);
            y_y_list.get(i)[0] = param.get(startFrom + 4, 0);
            y_z_list.get(i)[0] = param.get(startFrom + 5, 0);

            y_x_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 6, 0);
            y_y_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 5, 0);
            y_z_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 4, 0);

            y_x_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 3, 0);
            y_y_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 2, 0);
            y_z_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 1, 0);

            y_x_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 6, 0);
            y_y_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 5, 0);
            y_z_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 4, 0);

            y_x_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 3, 0);
            y_y_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 2, 0);
            y_z_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 1, 0);

            y_penalty_list.get(i)[0] = 1.0;

            y_penalty_list.get(i)[y_x_list.get(i).length - 2] = 1.0;
            y_penalty_list.get(i)[y_x_list.get(i).length - 1] = 1.0;

            for (int h = startFrom; h < startFrom + segs*6; h += 6) {

                y_x_rot_list.get(i)[counti] = param.get(h + 0, 0);
                y_y_rot_list.get(i)[counti] = param.get(h + 1, 0);
                y_z_rot_list.get(i)[counti] = param.get(h + 2, 0);

                y_x_list.get(i)[counti] = param.get(h + 3, 0);
                y_y_list.get(i)[counti] = param.get(h + 4, 0);
                y_z_list.get(i)[counti] = param.get(h + 5, 0);

                counti++;
            }

            po_x_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_x_rot_list.get(i));
            po_y_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_y_rot_list.get(i));
            po_z_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_z_rot_list.get(i));

            po_x_sparseArray[i] = interpolatori.interpolate(x_seg, y_x_list.get(i));
            po_y_sparseArray[i] = interpolatori.interpolate(x_seg, y_y_list.get(i));
            po_z_sparseArray[i] = interpolatori.interpolate(x_seg, y_z_list.get(i));

            /*
            po_x_rot_list.put(i, interpolatori.interpolate(x_seg, y_x_rot_list.get(i)));
            po_y_rot_list.put(i, interpolatori.interpolate(x_seg, y_y_rot_list.get(i)));
            po_z_rot_list.put(i, interpolatori.interpolate(x_seg, y_z_rot_list.get(i)));
            po_x_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
            po_y_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
            po_z_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
*/
        }

    }

    public void createPolynomialSplines3(DMatrixRMaj param){

        double[] previousValues = new double[6];


        for(int ii = 0; ii < flightLineIds.size(); ii++){

            int i = flightLineIds.get(ii);
            strip strippi = stripInformation.get(i);

            int counti = 1;

            int segs = strippi.n_segments;
            int startFrom = strippi.flightLineParamsStartFrom * 6;
            double start_t = strippi.start_t;
            double end_t = strippi.end_t;

            for(int j = 0; j < segs; j++){

                x_segments_list.get(i)[counti] = start_t + j * deltaT;
                counti++;

            }

            x_segments_list.get(i)[0] = x_segments_list.get(i)[1] - 2;
            x_segments_list.get(i)[x_segments_list.get(i).length - 2] = end_t;
            x_segments_list.get(i)[x_segments_list.get(i).length - 1] = x_segments_list.get(i)[x_segments_list.get(i).length - 2] + 2;

            double[] x_seg = x_segments_list.get(i);

            counti = 1;

            previousValues[0] = param.get(startFrom + 0, 0);
            previousValues[1] = param.get(startFrom + 1, 0);
            previousValues[2] = param.get(startFrom + 2, 0);
            previousValues[3] = param.get(startFrom + 3, 0);
            previousValues[4] = param.get(startFrom + 4, 0);
            previousValues[5] = param.get(startFrom + 5, 0);

            y_x_rot_list.get(i)[0] = param.get(startFrom + 0, 0);
            y_y_rot_list.get(i)[0] = param.get(startFrom + 1, 0);
            y_z_rot_list.get(i)[0] = param.get(startFrom + 2, 0);


            y_x_list.get(i)[0] = param.get(startFrom + 3, 0);
            y_y_list.get(i)[0] = param.get(startFrom + 4, 0);
            y_z_list.get(i)[0] = param.get(startFrom + 5, 0);

            y_x_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 6, 0);
            y_y_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 5, 0);
            y_z_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 4, 0);

            y_x_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 3, 0);
            y_y_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 2, 0);
            y_z_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 1, 0);

            y_x_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 6, 0);
            y_y_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 5, 0);
            y_z_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 4, 0);

            y_x_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 3, 0);
            y_y_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 2, 0);
            y_z_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 1, 0);

            y_penalty_list.get(i)[0] = 1.0;

            y_penalty_list.get(i)[y_x_list.get(i).length - 2] = 1.0;
            y_penalty_list.get(i)[y_x_list.get(i).length - 1] = 1.0;

            for (int h = startFrom; h < startFrom + segs*6; h += 6) {

                y_x_rot_list.get(i)[counti] = param.get(h + 0, 0);
                y_y_rot_list.get(i)[counti] = param.get(h + 1, 0);
                y_z_rot_list.get(i)[counti] = param.get(h + 2, 0);

                y_x_list.get(i)[counti] = param.get(h + 3, 0);
                y_y_list.get(i)[counti] = param.get(h + 4, 0);
                y_z_list.get(i)[counti] = param.get(h + 5, 0);

                counti++;
            }

            po_x_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_x_rot_list.get(i));
            po_y_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_y_rot_list.get(i));
            po_z_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_z_rot_list.get(i));

            po_x_sparseArray[i] = interpolatori.interpolate(x_seg, y_x_list.get(i));
            po_y_sparseArray[i] = interpolatori.interpolate(x_seg, y_y_list.get(i));
            po_z_sparseArray[i] = interpolatori.interpolate(x_seg, y_z_list.get(i));

            po_x_rot_list.put(i, interpolatori.interpolate(x_seg, y_x_rot_list.get(i)));
            po_y_rot_list.put(i, interpolatori.interpolate(x_seg, y_y_rot_list.get(i)));
            po_z_rot_list.put(i, interpolatori.interpolate(x_seg, y_z_rot_list.get(i)));
            po_x_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
            po_y_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
            po_z_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));

        }


        if(false) {
            x_segments = new double[numberOfSegments + 3];

            y_x = new double[numberOfSegments + 3];
            y_y = new double[numberOfSegments + 3];
            y_z = new double[numberOfSegments + 3];

            y_x_rot = new double[numberOfSegments + 3];
            y_y_rot = new double[numberOfSegments + 3];
            y_z_rot = new double[numberOfSegments + 3];


            int counti = 1;

            for (int e = 0; e <= numberOfSegments; e++) {
                x_segments[counti] = start_time + e * deltaT;
                counti++;
            }

            x_segments[0] = x_segments[1] - 2;
            x_segments[x_segments.length - 2] = end_time;
            x_segments[x_segments.length - 1] = x_segments[x_segments.length - 2] + 2;

/*
        if(!blokki.aligned[blokki.currentFiles[0]]) {
            for (int e = 0; e <= numberOfSegments; e++) {
                x_segments[counti] = blokki.pointCloudTimeExtent.get(blokki.currentFiles[0])[0] + e * deltaT;
                counti++;
            }
            x_segments[0] = x_segments[1]-2;
            x_segments[x_segments.length-2] = blokki.pointCloudTimeExtent.get(blokki.currentFiles[0])[1];
            x_segments[x_segments.length-1] = x_segments[x_segments.length-2]+2;
        }
        else if(!blokki.aligned[blokki.currentFiles[1]]) {
            //System.out.println("HERE!");
            for (int e = 0; e <= numberOfSegments; e++) {
                x_segments[counti] = blokki.pointCloudTimeExtent.get(blokki.currentFiles[1])[0] + e * deltaT;
                counti++;
            }
            x_segments[0] = x_segments[1]-2;
            x_segments[x_segments.length-2] = blokki.pointCloudTimeExtent.get(blokki.currentFiles[1])[1];
            x_segments[x_segments.length-1] = x_segments[x_segments.length-2]+2;
        }
*/
            counti = 1;

            y_x_rot[0] = param.get(0 + 0, 0);
            y_y_rot[0] = param.get(0 + 1, 0);
            y_z_rot[0] = param.get(0 + 2, 0);

            y_x[0] = param.get(0 + 3, 0);
            y_y[0] = param.get(0 + 4, 0);
            y_z[0] = param.get(0 + 5, 0);

            y_x_rot[y_x.length - 2] = param.get(param.numRows - 6, 0);
            y_y_rot[y_x.length - 2] = param.get(param.numRows - 5, 0);
            y_z_rot[y_x.length - 2] = param.get(param.numRows - 4, 0);

            y_x[y_x.length - 2] = param.get(param.numRows - 3, 0);
            y_y[y_x.length - 2] = param.get(param.numRows - 2, 0);
            y_z[y_x.length - 2] = param.get(param.numRows - 1, 0);

            y_x_rot[y_x.length - 1] = param.get(param.numRows - 6, 0);
            y_y_rot[y_x.length - 1] = param.get(param.numRows - 5, 0);
            y_z_rot[y_x.length - 1] = param.get(param.numRows - 4, 0);

            y_x[y_x.length - 1] = param.get(param.numRows - 3, 0);
            y_y[y_x.length - 1] = param.get(param.numRows - 2, 0);
            y_z[y_x.length - 1] = param.get(param.numRows - 1, 0);

            for (int h = 0; h < param.numRows; h += 6) {

                y_x_rot[counti] = param.get(h + 0, 0);
                y_y_rot[counti] = param.get(h + 1, 0);
                y_z_rot[counti] = param.get(h + 2, 0);

                y_x[counti] = param.get(h + 3, 0);
                y_y[counti] = param.get(h + 4, 0);
                y_z[counti] = param.get(h + 5, 0);

                counti++;
            }

            //System.out.println(deltaT);
            //System.out.println(Arrays.toString(x_segments));
            po_x_rot = interpolatori.interpolate(x_segments, y_x_rot);
            po_y_rot = interpolatori.interpolate(x_segments, y_z_rot);
            po_z_rot = interpolatori.interpolate(x_segments, y_y_rot);
            po_x = interpolatori.interpolate(x_segments, y_x);
            po_y = interpolatori.interpolate(x_segments, y_y);
            po_z = interpolatori.interpolate(x_segments, y_z);
        }

    }


    public synchronized void createPolynomialSplines2(DMatrixRMaj param, PolynomialSplineFunction[] po_x_rot_sparseArray,
                                                            PolynomialSplineFunction[] po_y_rot_sparseArray,
                                         PolynomialSplineFunction[] po_z_rot_sparseArray,
                                         PolynomialSplineFunction[] po_x_sparseArray,
                                         PolynomialSplineFunction[] po_y_sparseArray,
                                         PolynomialSplineFunction[] po_z_sparseArray,
                                                      SplineInterpolator interpolatori
                                         ){

        double[] previousValues = new double[6];


        for(int ii = 0; ii < flightLineIds.size(); ii++){

            int i = flightLineIds.get(ii);
            strip strippi = stripInformation.get(i);

            int counti = 1;

            int segs = strippi.n_segments;
            int startFrom = strippi.flightLineParamsStartFrom * 6;
            double start_t = strippi.start_t;
            double end_t = strippi.end_t;

            for(int j = 0; j < segs; j++){

                x_segments_list.get(i)[counti] = start_t + j * deltaT;
                counti++;

            }

            x_segments_list.get(i)[0] = x_segments_list.get(i)[1] - 2;
            x_segments_list.get(i)[x_segments_list.get(i).length - 2] = end_t;
            x_segments_list.get(i)[x_segments_list.get(i).length - 1] = x_segments_list.get(i)[x_segments_list.get(i).length - 2] + 2;

            double[] x_seg = x_segments_list.get(i);

            counti = 1;

            previousValues[0] = param.get(startFrom + 0, 0);
            previousValues[1] = param.get(startFrom + 1, 0);
            previousValues[2] = param.get(startFrom + 2, 0);
            previousValues[3] = param.get(startFrom + 3, 0);
            previousValues[4] = param.get(startFrom + 4, 0);
            previousValues[5] = param.get(startFrom + 5, 0);

            y_x_rot_list.get(i)[0] = param.get(startFrom + 0, 0);
            y_y_rot_list.get(i)[0] = param.get(startFrom + 1, 0);
            y_z_rot_list.get(i)[0] = param.get(startFrom + 2, 0);


            y_x_list.get(i)[0] = param.get(startFrom + 3, 0);
            y_y_list.get(i)[0] = param.get(startFrom + 4, 0);
            y_z_list.get(i)[0] = param.get(startFrom + 5, 0);

            y_x_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 6, 0);
            y_y_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 5, 0);
            y_z_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 4, 0);

            y_x_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 3, 0);
            y_y_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 2, 0);
            y_z_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*6 - 1, 0);

            y_x_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 6, 0);
            y_y_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 5, 0);
            y_z_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 4, 0);

            y_x_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 3, 0);
            y_y_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 2, 0);
            y_z_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*6 - 1, 0);

            y_penalty_list.get(i)[0] = 1.0;

            y_penalty_list.get(i)[y_x_list.get(i).length - 2] = 1.0;
            y_penalty_list.get(i)[y_x_list.get(i).length - 1] = 1.0;

            for (int h = startFrom; h < startFrom + segs*6; h += 6) {

                y_x_rot_list.get(i)[counti] = param.get(h + 0, 0);
                y_y_rot_list.get(i)[counti] = param.get(h + 1, 0);
                y_z_rot_list.get(i)[counti] = param.get(h + 2, 0);

                y_x_list.get(i)[counti] = param.get(h + 3, 0);
                y_y_list.get(i)[counti] = param.get(h + 4, 0);
                y_z_list.get(i)[counti] = param.get(h + 5, 0);

                counti++;
            }

            po_x_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_x_rot_list.get(i));
            po_y_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_y_rot_list.get(i));
            po_z_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_z_rot_list.get(i));

            po_x_sparseArray[i] = interpolatori.interpolate(x_seg, y_x_list.get(i));
            po_y_sparseArray[i] = interpolatori.interpolate(x_seg, y_y_list.get(i));
            po_z_sparseArray[i] = interpolatori.interpolate(x_seg, y_z_list.get(i));

            /*
            po_x_rot_list.put(i, interpolatori.interpolate(x_seg, y_x_rot_list.get(i)));
            po_y_rot_list.put(i, interpolatori.interpolate(x_seg, y_y_rot_list.get(i)));
            po_z_rot_list.put(i, interpolatori.interpolate(x_seg, y_z_rot_list.get(i)));
            po_x_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
            po_y_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
            po_z_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
*/
        }

    }

    public void rotatePoint2(DoubleMatrix point, double time, int id){



        /*
        makeRotationMatrix(tempMatrix,
                interpolate(polynomial_pitch, time),
                interpolate(polynomial_roll, time),
                interpolate(polynomial_yaw, time));

        rotatePoint(point, tempMatrix, (int)(time*1000), 0);

        tempTrans[0] = interpolate(polynomial_x, time);
        tempTrans[1] = interpolate(polynomial_y, time);
        tempTrans[2] = interpolate(polynomial_z, time);

         */
        //System.out.println(time);

        //double scale = po_scale_sparseArray[id].value(time);
        /*
        makeRotationMatrix3(tempMatrix,
                po_x_rot_sparseArray[id].value(time),
                po_y_rot_sparseArray[id].value(time),
                po_z_rot_sparseArray[id].value(time));
*/
        //System.out.println(scale);

        double yaw = Math.toRadians(po_z_rot_sparseArray[id].value(time));
        double pitch = Math.toRadians(po_y_rot_sparseArray[id].value(time));
        double roll = Math.toRadians(po_x_rot_sparseArray[id].value(time));
        tempTrans[0] = po_x_sparseArray[id].value(time) ;
        tempTrans[1] = po_y_sparseArray[id].value(time) ;
        tempTrans[2] = po_z_sparseArray[id].value(time) ;

        //if(true)
        //return;



        if(trajectory.size() > 0) {
            hehe = trajectory.ceilingEntry(time);

            locationOfAircraft = hehe.getValue();
        }

        //locationOfAircraft = hehe.getValue();

        double new_x = locationOfAircraft[0] - point.get(0, 0);
        double new_y = locationOfAircraft[1] - point.get(0, 1);
        double new_z = locationOfAircraft[2] - point.get(0, 2);

        //rotatedPointMatrix = point.mmul(rotationMatrix);

        //Core.gemm(point, rotationMatrix, 1, dummyMat, 0, rotatedpoint);

        double temp_x = new_x;
        double temp_y = new_y;
        double temp_z = new_z;



        //rotatePoint(point, tempMatrix, (time), 1.0);
        /*
        if(yaw != 0 || pitch != 0 || roll != 0){
            System.out.println(point);
        }

         */
        //System.out.println(point);

        double cosa = Math.cos(yaw);
        double sina = Math.sin(yaw);

        double cosb = Math.cos(pitch);
        double sinb = Math.sin(pitch);

        double cosc = Math.cos(roll);
        double sinc = Math.sin(roll);

        double Axx = cosa*cosb;
        double Axy = cosa*sinb*sinc - sina*cosc;
        double Axz = cosa*sinb*cosc + sina*sinc;

        double Ayx = sina*cosb;
        double Ayy = sina*sinb*sinc + cosa*cosc;
        double Ayz = sina*sinb*cosc - cosa*sinc;

        double Azx = -sinb;
        double Azy = cosb*sinc;
        double Azz = cosb*cosc;

        double px = temp_x;
        double py = temp_y;
        double pz = temp_z;

        temp_x = Axx*px + Axy*py + Axz*pz;
        temp_y = Ayx*px + Ayy*py + Ayz*pz;
        temp_z = Azx*px + Azy*py + Azz*pz;

        //point.put(0, 0, locationOfAircraft[0] - rotatedPointMatrix.get(0, 0));
        //point.put(0, 1, locationOfAircraft[1] - rotatedPointMatrix.get(0, 1));
        //point.put(0, 2, locationOfAircraft[2] - rotatedPointMatrix.get(0, 2));

        point.put(0,0,locationOfAircraft[0] - temp_x);
        point.put(0,1,locationOfAircraft[1] - temp_y);
        point.put(0,2,locationOfAircraft[2] - temp_z);
/*
        if(yaw != 0 || pitch != 0 || roll != 0){
            System.out.println(point);
            System.out.println("----------------------");
        }

 */
/*
        if(yaw != 0 || pitch != 0 || roll != 0){
            System.out.println(point);
        }

*/


    }

    public void rotatePoint2_cp(cloudPoint point, double time, int id){



        /*
        makeRotationMatrix(tempMatrix,
                interpolate(polynomial_pitch, time),
                interpolate(polynomial_roll, time),
                interpolate(polynomial_yaw, time));

        rotatePoint(point, tempMatrix, (int)(time*1000), 0);

        tempTrans[0] = interpolate(polynomial_x, time);
        tempTrans[1] = interpolate(polynomial_y, time);
        tempTrans[2] = interpolate(polynomial_z, time);

         */
        //System.out.println(time);

        //double scale = po_scale_sparseArray[id].value(time);
        /*
        makeRotationMatrix3(tempMatrix,
                po_x_rot_sparseArray[id].value(time),
                po_y_rot_sparseArray[id].value(time),
                po_z_rot_sparseArray[id].value(time));
*/
        //System.out.println(scale);

        double yaw = Math.toRadians(po_z_rot_sparseArray[id].value(time));
        double pitch = Math.toRadians(po_y_rot_sparseArray[id].value(time));
        double roll = Math.toRadians(po_x_rot_sparseArray[id].value(time));
        tempTrans[0] = po_x_sparseArray[id].value(time) ;
        tempTrans[1] = po_y_sparseArray[id].value(time) ;
        tempTrans[2] = po_z_sparseArray[id].value(time) ;

        //if(true)
        //return;

        // IF the point has previously been rotated using these angles, dont rotate again */
        if( point.prev_yaw == yaw  &&
        point.prev_pitch == pitch &&
        point.prev_roll == roll &&
        point.prev_x == tempTrans[0] &&
        point.prev_y == tempTrans[1] &&
        point.prev_z == tempTrans[2] ){

            return;

        }

        point.prev_yaw = yaw;
        point.prev_pitch = pitch;
        point.prev_roll = roll;
        point.prev_x = tempTrans[0];
        point.prev_y = tempTrans[1];
        point.prev_z = tempTrans[2];

        double[] locationOfAircraft = new double[3];

        if(trajectory.size() > 0) {
            hehe = trajectory.ceilingEntry(time);

            locationOfAircraft = hehe.getValue();
        }

        //locationOfAircraft = hehe.getValue();

        double new_x = locationOfAircraft[0] - point.x;
        double new_y = locationOfAircraft[1] - point.y;
        double new_z = locationOfAircraft[2] - point.z;

        //rotatedPointMatrix = point.mmul(rotationMatrix);

        //Core.gemm(point, rotationMatrix, 1, dummyMat, 0, rotatedpoint);

        double temp_x = new_x;
        double temp_y = new_y;
        double temp_z = new_z;



        //rotatePoint(point, tempMatrix, (time), 1.0);
        /*
        if(yaw != 0 || pitch != 0 || roll != 0){
            System.out.println(point);
        }

         */
        //System.out.println(point);

        double cosa = Math.cos(yaw);
        double sina = Math.sin(yaw);

        double cosb = Math.cos(pitch);
        double sinb = Math.sin(pitch);

        double cosc = Math.cos(roll);
        double sinc = Math.sin(roll);

        double Axx = cosa*cosb;
        double Axy = cosa*sinb*sinc - sina*cosc;
        double Axz = cosa*sinb*cosc + sina*sinc;

        double Ayx = sina*cosb;
        double Ayy = sina*sinb*sinc + cosa*cosc;
        double Ayz = sina*sinb*cosc - cosa*sinc;

        double Azx = -sinb;
        double Azy = cosb*sinc;
        double Azz = cosb*cosc;

        double px = temp_x;
        double py = temp_y;
        double pz = temp_z;

        temp_x = Axx*px + Axy*py + Axz*pz;
        temp_y = Ayx*px + Ayy*py + Ayz*pz;
        temp_z = Azx*px + Azy*py + Azz*pz;

        //point.put(0, 0, locationOfAircraft[0] - rotatedPointMatrix.get(0, 0));
        //point.put(0, 1, locationOfAircraft[1] - rotatedPointMatrix.get(0, 1));
        //point.put(0, 2, locationOfAircraft[2] - rotatedPointMatrix.get(0, 2));

        point.x_rot = locationOfAircraft[0] - temp_x;
        point.y_rot = locationOfAircraft[1] - temp_y;
        point.z_rot = locationOfAircraft[2] - temp_z;
/*
        if(yaw != 0 || pitch != 0 || roll != 0){
            System.out.println(point);
            System.out.println("----------------------");
        }

 */
        /*
        if(yaw != 0 || pitch != 0 || roll != 0){
            point.print();
            System.out.println("---------------");
        }
*/
        point.x_rot += tempTrans[0];
        point.y_rot += tempTrans[1];
        point.z_rot += tempTrans[2];

    }

    public void rotatePoint3_cp(cloudPoint point, double time, int id, PolynomialSplineFunction[] po_x_rot_sparseArray,
                                PolynomialSplineFunction[] po_y_rot_sparseArray,
                                PolynomialSplineFunction[] po_z_rot_sparseArray,
                                PolynomialSplineFunction[] po_x_sparseArray,
                                PolynomialSplineFunction[] po_y_sparseArray,
                                PolynomialSplineFunction[] po_z_sparseArray,
                                double[] rotatedValue){



        /*
        makeRotationMatrix(tempMatrix,
                interpolate(polynomial_pitch, time),
                interpolate(polynomial_roll, time),
                interpolate(polynomial_yaw, time));

        rotatePoint(point, tempMatrix, (int)(time*1000), 0);

        tempTrans[0] = interpolate(polynomial_x, time);
        tempTrans[1] = interpolate(polynomial_y, time);
        tempTrans[2] = interpolate(polynomial_z, time);

         */
        //System.out.println(time);

        //double scale = po_scale_sparseArray[id].value(time);
        /*
        makeRotationMatrix3(tempMatrix,
                po_x_rot_sparseArray[id].value(time),
                po_y_rot_sparseArray[id].value(time),
                po_z_rot_sparseArray[id].value(time));
*/
        //System.out.println(scale);

        double[] tempTrans = new double[3];

        double yaw = Math.toRadians(po_z_rot_sparseArray[id].value(time));
        double pitch = Math.toRadians(po_y_rot_sparseArray[id].value(time));
        double roll = Math.toRadians(po_x_rot_sparseArray[id].value(time));
        tempTrans[0] = po_x_sparseArray[id].value(time) ;
        tempTrans[1] = po_y_sparseArray[id].value(time) ;
        tempTrans[2] = po_z_sparseArray[id].value(time) ;

        //if(true)
        //return;

        // IF the point has previously been rotated using these angles, dont rotate again */
        if(false)
        if( point.prev_yaw == yaw  &&
                point.prev_pitch == pitch &&
                point.prev_roll == roll &&
                point.prev_x == tempTrans[0] &&
                point.prev_y == tempTrans[1] &&
                point.prev_z == tempTrans[2] ){

            return;

        }

        point.prev_yaw = yaw;
        point.prev_pitch = pitch;
        point.prev_roll = roll;
        point.prev_x = tempTrans[0];
        point.prev_y = tempTrans[1];
        point.prev_z = tempTrans[2];

        double[] locationOfAircraft = new double[3];
        Map.Entry<Double, double[]> hehe;

        if(trajectory.size() > 0) {
            hehe = trajectory.ceilingEntry(time);

            locationOfAircraft = hehe.getValue();
        }

        //locationOfAircraft = hehe.getValue();

        double new_x = locationOfAircraft[0] - point.x;
        double new_y = locationOfAircraft[1] - point.y;
        double new_z = locationOfAircraft[2] - point.z;

        //rotatedPointMatrix = point.mmul(rotationMatrix);

        //Core.gemm(point, rotationMatrix, 1, dummyMat, 0, rotatedpoint);

        double temp_x = new_x;
        double temp_y = new_y;
        double temp_z = new_z;

        //rotatePoint(point, tempMatrix, (time), 1.0);
        /*
        if(yaw != 0 || pitch != 0 || roll != 0){
            System.out.println(point);
        }

         */
        //System.out.println(point);

        double cosa = Math.cos(yaw);
        double sina = Math.sin(yaw);

        double cosb = Math.cos(pitch);
        double sinb = Math.sin(pitch);

        double cosc = Math.cos(roll);
        double sinc = Math.sin(roll);

        double Axx = cosa*cosb;
        double Axy = cosa*sinb*sinc - sina*cosc;
        double Axz = cosa*sinb*cosc + sina*sinc;

        double Ayx = sina*cosb;
        double Ayy = sina*sinb*sinc + cosa*cosc;
        double Ayz = sina*sinb*cosc - cosa*sinc;

        double Azx = -sinb;
        double Azy = cosb*sinc;
        double Azz = cosb*cosc;

        double px = temp_x;
        double py = temp_y;
        double pz = temp_z;

        temp_x = Axx*px + Axy*py + Axz*pz;
        temp_y = Ayx*px + Ayy*py + Ayz*pz;
        temp_z = Azx*px + Azy*py + Azz*pz;

        //point.put(0, 0, locationOfAircraft[0] - rotatedPointMatrix.get(0, 0));
        //point.put(0, 1, locationOfAircraft[1] - rotatedPointMatrix.get(0, 1));
        //point.put(0, 2, locationOfAircraft[2] - rotatedPointMatrix.get(0, 2));

        point.x_rot = locationOfAircraft[0] - temp_x;
        point.y_rot = locationOfAircraft[1] - temp_y;
        point.z_rot = locationOfAircraft[2] - temp_z;

        rotatedValue[0] = locationOfAircraft[0] - temp_x + tempTrans[0];
        rotatedValue[1] = locationOfAircraft[1] - temp_y + tempTrans[1];
        rotatedValue[2] = locationOfAircraft[2] - temp_z + tempTrans[2];
/*
        if(yaw != 0 || pitch != 0 || roll != 0){
            System.out.println(point);
            System.out.println("----------------------");
        }

 */
        /*
        if(yaw != 0 || pitch != 0 || roll != 0){
            point.print();
            System.out.println("---------------");
        }
*/
        point.x_rot += tempTrans[0];
        point.y_rot += tempTrans[1];
        point.z_rot += tempTrans[2];

    }
    public void rotatePoint2_fast(SimpleMatrix point, double time, int id){

        makeRotationMatrix3_fast(tempMatrix_fast,
                po_x_rot_sparseArray[id].value(time),
                po_y_rot_sparseArray[id].value(time),
                po_z_rot_sparseArray[id].value(time));

        //System.out.println(scale);
        rotatePoint_fast(point, tempMatrix_fast, (time), 1.0);

        tempTrans[0] = po_x_sparseArray[id].value(time) ;
        tempTrans[1] = po_y_sparseArray[id].value(time) ;
        tempTrans[2] = po_z_sparseArray[id].value(time) ;

    }

    public void makeRotationMatrix(DoubleMatrix output, double x, double y, double z){

        x = Math.toRadians(x);
        y = Math.toRadians(y);
        z = Math.toRadians(z);

        // FIRST ROW DONE!
        output.put(0,0, (Math.cos(y) * Math.cos(z)));
        output.put(0,1, (Math.cos(x) * Math.sin(z)) + Math.sin(x) * Math.sin(y) * Math.cos(z));
        output.put(0,2, (Math.sin(x) * Math.sin(z)) - Math.cos(x) * Math.sin(y) * Math.cos(z));

        // SECOND ROW DONE!
        output.put(1,0, (-Math.cos(y) * Math.sin(z)));
        output.put(1,1, (Math.cos(x) * Math.cos(z)) - Math.sin(x) * Math.sin(y) * Math.sin(z));
        output.put(1,2, (Math.sin(x) * Math.cos(z)) + Math.cos(x) * Math.sin(y) * Math.sin(z));

        // THIRD ROW
        output.put(2,0, Math.sin(y));
        output.put(2,1, -Math.sin(x) * Math.cos(y));
        output.put(2,2, Math.cos(x) * Math.cos(y));

    }

    public void makeRotationMatrix3(DoubleMatrix output, double x, double y, double z){

        x = Math.toRadians(x);
        y = Math.toRadians(y);
        z = Math.toRadians(z);

        // FIRST ROW DONE!
        output.put(0,0, (Math.cos(y) * Math.cos(z)));
        output.put(0,1, (Math.cos(x) * Math.sin(z)) + Math.sin(x) * Math.sin(y) * Math.cos(z));
        output.put(0,2, (Math.sin(x) * Math.sin(z)) - Math.cos(x) * Math.sin(y) * Math.cos(z));

        // SECOND ROW DONE!
        output.put(1,0, (-Math.cos(y) * Math.sin(z)));
        output.put(1,1, (Math.cos(x) * Math.cos(z)) - Math.sin(x) * Math.sin(y) * Math.sin(z));
        output.put(1,2, (Math.sin(x) * Math.cos(z)) + Math.cos(x) * Math.sin(y) * Math.sin(z));

        // THIRD ROW
        output.put(2,0, Math.sin(y));
        output.put(2,1, -Math.sin(x) * Math.cos(y));
        output.put(2,2, Math.cos(x) * Math.cos(y));

    }

    public void makeRotationMatrix3_fast(SimpleMatrix output, double x, double y, double z){

        x = Math.toRadians(x);
        y = Math.toRadians(y);
        z = Math.toRadians(z);

        // FIRST ROW DONE!
        output.set(0,0, (Math.cos(y) * Math.cos(z)));
        output.set(0,1, (Math.cos(x) * Math.sin(z)) + Math.sin(x) * Math.sin(y) * Math.cos(z));
        output.set(0,2, (Math.sin(x) * Math.sin(z)) - Math.cos(x) * Math.sin(y) * Math.cos(z));

        // SECOND ROW DONE!
        output.set(1,0, (-Math.cos(y) * Math.sin(z)));
        output.set(1,1, (Math.cos(x) * Math.cos(z)) - Math.sin(x) * Math.sin(y) * Math.sin(z));
        output.set(1,2, (Math.sin(x) * Math.cos(z)) + Math.cos(x) * Math.sin(y) * Math.sin(z));

        // THIRD ROW
        output.set(2,0, Math.sin(y));
        output.set(2,1, -Math.sin(x) * Math.cos(y));
        output.set(2,2, Math.cos(x) * Math.cos(y));

    }

    public void rotatePoint(DoubleMatrix point, DoubleMatrix rotationMatrix, double time, double scale){

        //Find the closest timestamp in the trajectory file
        if(trajectory.size() > 0) {
            hehe = trajectory.ceilingEntry(time);

            locationOfAircraft = hehe.getValue();
        }

        //locationOfAircraft = hehe.getValue();

        new_x = locationOfAircraft[0] - point.get(0, 0);
        new_y = locationOfAircraft[1] - point.get(0, 1);
        new_z = locationOfAircraft[2] - point.get(0, 2);

        point.put(0, 0, new_x);
        point.put(0, 1, new_y);
        point.put(0, 2, new_z);


        rotatedPointMatrix = point.mmul(rotationMatrix);

        //Core.gemm(point, rotationMatrix, 1, dummyMat, 0, rotatedpoint);

        point.put(0, 0, locationOfAircraft[0] - rotatedPointMatrix.get(0, 0));
        point.put(0, 1, locationOfAircraft[1] - rotatedPointMatrix.get(0, 1));
        point.put(0, 2, locationOfAircraft[2] - rotatedPointMatrix.get(0, 2));

    }

    public void rotatePoint_fast(SimpleMatrix point, SimpleMatrix rotationMatrix, double time, double scale){

        //Find the closest timestamp in the trajectory file
        if(trajectory.size() > 0) {
            hehe = trajectory.ceilingEntry(time);

            locationOfAircraft = hehe.getValue();
        }

        //locationOfAircraft = hehe.getValue();

        new_x = locationOfAircraft[0] - point.get(0, 0);
        new_y = locationOfAircraft[1] - point.get(0, 1);
        new_z = locationOfAircraft[2] - point.get(0, 2);

        point.set(0, 0, new_x);
        point.set(0, 1, new_y);
        point.set(0, 2, new_z);


        rotatedPointMatrix_fast = point.mult(rotationMatrix);

        //Core.gemm(point, rotationMatrix, 1, dummyMat, 0, rotatedpoint);

        point.set(0, 0, locationOfAircraft[0] - rotatedPointMatrix.get(0, 0));
        point.set(0, 1, locationOfAircraft[1] - rotatedPointMatrix.get(0, 1));
        point.set(0, 2, locationOfAircraft[2] - rotatedPointMatrix.get(0, 2));

    }


    public void setTrajectory(TreeMap<Double,double[]> trajectory){

        this.trajectory = trajectory;
    }
}
