package utils;

import LASio.LasPoint;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DMatrixRMaj;
import org.jblas.DoubleMatrix;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class functionCircleFit implements FunctionNtoM {

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

    PolynomialSplineFunction po_scale;
    TreeMap<Integer,PolynomialSplineFunction> po_scale_list;
    PolynomialSplineFunction[] po_scale_sparseArray;

    DoubleMatrix tempMatrix = new DoubleMatrix(3,3);
    double[] tempTrans = new double[3];
    int numberOfSegments = 0;
    int numFunctions = 1;
    double diameter;
    double deltaT;

    public double meanRadius = 0;

    int n_flightlines;

    double new_x;
    double new_y;
    double new_z;

    double[] locationOfAircraft;

    Map.Entry<Double, double[]> hehe;

    DoubleMatrix pointMatrix = new DoubleMatrix(1,3);
    //Mat rotatedpoint = new Mat(1,3, CV_64F);
    DoubleMatrix rotatedPointMatrix = new DoubleMatrix(1,3);

    TreeMap<Short, HashMap<Integer, ArrayList<cloudPoint>>> trunkPoints;
    TreeMap<Short, HashMap<Integer, ArrayList<cloudPoint>>> trunkPoints_align;
    TreeMap<Double,double[]> trajectory = new TreeMap<>();


    ArrayList<ArrayList<cloudPoint>> slicesToBeOptimized;
    ArrayList<ArrayList<cloudPoint>> slicesToBeOptimized_for_residual;
    ArrayList<TreeMap<Integer, ArrayList<cloudPoint>>> slicesToBeOptimized_for_residual_map;
    ArrayList<TreeMap<Integer, ArrayList<cloudPoint>>> slicesToBeOptimized_for_residual_map2;
    ArrayList<double[]> original_fit_for_slice;

    //ArrayList<ArrayList<cloudPoint>> trunkPoints_for_residual;
    ArrayList<TreeSet<cloudPoint>> trunkPoints_for_residual;
    double start_time, end_time;
    TreeMap<Integer, double[]> flightLines = new TreeMap<>();

    DMatrixRMaj final_param;

    public boolean initialized = false;
    ArrayList<Integer> n_segments_per_flightline;
    ArrayList<Integer> flightLineParamsStartFrom;
    ArrayList<Integer> flightLineIds;

    TreeMap<Integer, strip> stripInformation;

    SimpleRegression[] trunkRegressions;
    double[] trunkRegressionPenalty;

    ArrayList<Integer> slices_trunk_ids;
    ArrayList<Double> slices_z;

    public boolean writeToFile = false;
    String outFileName;
    File fout;
    FileOutputStream fos;

    BufferedWriter bw;

    public double[] thisCosts = new double[2];
    int n_params = 0;
    double[] start_end_radius = new double[]{-1,-1};

    public functionCircleFit(double deltaT, TreeMap<Integer, strip> stripInformation) {
        slices_z = new ArrayList<>();
        slices_trunk_ids = new ArrayList<>();
        this.stripInformation = stripInformation;
        // this.flightLineIds = flightLineIds;
        //this.flightLineParamsStartFrom = flightLineParamsStartFrom;
        this.n_flightlines = stripInformation.size();
        this.deltaT = deltaT;
        //this.n_segments_per_flightline = n_segments_per_flightline;

        po_x_rot_list = new TreeMap<>();
        po_y_rot_list = new TreeMap<>();
        po_z_rot_list = new TreeMap<>();

        y_x_rot_list = new TreeMap<>();
        y_y_rot_list = new TreeMap<>();
        y_z_rot_list = new TreeMap<>();

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
        //for(int i = 0; i < flightLineIds.size(); i++){

        int maxStripId = stripInformation.lastEntry().getKey();

        po_x_rot_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        po_y_rot_sparseArray = new PolynomialSplineFunction[maxStripId+1];
        po_z_rot_sparseArray = new PolynomialSplineFunction[maxStripId+1];

        po_scale_sparseArray = new PolynomialSplineFunction[maxStripId+1];

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

            po_scale_list.put(entry.getKey(), null);

            x_segments_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);

            y_x_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);
            y_y_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);
            y_z_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);

            y_scale_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);

            y_x_rot_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);
            y_y_rot_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);
            y_z_rot_list.put(entry.getKey(), new double[entry.getValue().n_segments + 3]);


        }
    }


    /**
     * Number of parameters used to define the line.
     */
    @Override
    public int getNumOfInputsN() {
        return this.n_params;
    }

    /**
     * Number of output error functions.  Two for each point.
     */
    @Override
    public int getNumOfOutputsM() {
        return 1;
    }

    public void setNumFunctions(int in){

        this.n_params = in;

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
            //System.out.println("Trunk id: " + entry.getKey() + " n_flightLines: " + entry.getValue().size());

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

    public double[] calculateCost(double[] param, double[] residual, boolean writeToFile){

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
        process(param, residual);
        resid = residual[0];
        this.writeToFile = false;
        return new double[]{residual[0]};
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
/*
    public void prep(DMatrixRMaj param){

        this.final_param = param;
        createPolynomialSplines(param);

    }
*/
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

    @Override
    public void process(double[] input, double[] output) {

        //System.out.println(param);
        this.numberOfSegments = input.length / 6;
/*
        for(int i = 0; i < param.numRows; i++){
            if(param.get(i,0) != 0.0)
                System.out.println(i + " " + param.get(i,0));
        }
*/
        createPolynomialSplines(input);

        //createFlightLineSpecificCorrection(param);

        //System.out.println("DONE!");
        //System.exit(1);
        trunkPoints_for_residual.clear();

        int counter = 0;
        int counter2 = 0;
        cloudPoint c2;

        if(!initialized) {
            /* Change the coordinates of the trunk points based on params */
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
        }else{

            double[] orig = new double[3];
            double[] rotated = new double[3];

            double distance = 0.0;
            if(false)
                for(int i = 0; i < slicesToBeOptimized.size(); i++){

                    for(int j = 0; j < slicesToBeOptimized.get(i).size(); j++){

                        c2 = slicesToBeOptimized.get(i).get(j);
                        orig[0] = c2.x;
                        orig[1] = c2.y;
                        orig[2] = c2.z;


                        pointMatrix.put(0, 0, c2.x);
                        pointMatrix.put(0, 1, c2.y);
                        pointMatrix.put(0, 2, c2.z);

                        rotatePoint2(pointMatrix, c2.t, c2.id);

                        new_x = pointMatrix.get(0, 0) + tempTrans[0];
                        new_y = pointMatrix.get(0, 1) + tempTrans[1];
                        new_z = pointMatrix.get(0, 2) + tempTrans[2];

                        rotated[0] = new_x;
                        rotated[1] = new_y;
                        rotated[2] = new_z;

                        distance = distance3d(orig[0], orig[1], orig[2], rotated[0], rotated[1], rotated[2]);
/*
                    if(distance > 0.0) {

                        System.out.println("Distanec: " + distance);
                        //System.out.println(po_x_rot.value(c2.t) + " " + po_y_rot.value(c2.t) + " " + po_x_rot.value(c2.t));
                    }
*/
                        //System.out.println(i + " " + j);
                        //System.out.println("FINAL SHIT!!!: " + slicesToBeOptimized_for_residual.get(i).get(j).x);
                        slicesToBeOptimized_for_residual.get(i).get(j).x_rot = new_x;
                        slicesToBeOptimized_for_residual.get(i).get(j).y_rot = new_y;
                    }
                }



            for(int i = 0; i < slicesToBeOptimized_for_residual_map.size(); i++) {


                int debugConter = 0;
                for (Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map.get(i).entrySet()) {

                    for(int p = 0; p < entry.getValue().size(); p++){

                        c2 = entry.getValue().get(p);

                        double orig_x = c2.getX();
                        double orig_y = c2.getY();
                        double orig_z = c2.getZ();

                        orig[0] = c2.x;
                        orig[1] = c2.y;
                        orig[2] = c2.z;
                        //System.out.println(i + " " + debugConter);

                        pointMatrix.put(0, 0, orig[0]);
                        pointMatrix.put(0, 1, orig[1]);
                        pointMatrix.put(0, 2, orig[2]);

                        if(i == 506 && debugConter == 14){
                            //System.out.println("MITA VITTUA11111: ");
                            //c2.print();
                            //System.out.println("DEBUB PRINT!");
                            //c2.print();
                            //System.out.println(pointMatrix);
                            //System.out.println("------------------");
                        }

                        rotatePoint2(pointMatrix, c2.t, c2.id);

                        if(i == 506 && debugConter == 14){
                            //System.out.println("MITA VITTUA22222: " + orig_x + " " + orig_y + " " + orig_z);
                            //System.out.println("DEBUB PRINT!");
                            //c2.print();
                            //System.out.println(pointMatrix);
                            //System.out.println("------------------");
                        }
                        //System.out.println(Arrays.toString(tempTrans));
                        //pointMatrix.put(0, 0, (pointMatrix.get(0,0) + tempTrans[0]));
                        //pointMatrix.put(0, 1, (pointMatrix.get(0,1) + tempTrans[1]));
                        //pointMatrix.put(0, 2, (pointMatrix.get(0,2) + tempTrans[2]));
                        new_x = (pointMatrix.get(0, 0) + tempTrans[0]);
                        new_y = (pointMatrix.get(0, 1) + tempTrans[1]);
                        new_z = (pointMatrix.get(0, 2) + tempTrans[2]);

                        rotated[0] = new_x;
                        rotated[1] = new_y;
                        rotated[2] = new_z;

                        //System.out.println(i + " " + j);
                        slicesToBeOptimized_for_residual_map2.get(i).get(entry.getKey()).get(p).x_rot = new_x;
                        slicesToBeOptimized_for_residual_map2.get(i).get(entry.getKey()).get(p).y_rot = new_y;
                        slicesToBeOptimized_for_residual_map2.get(i).get(entry.getKey()).get(p).z_rot = new_z;

                        distance = distance3d(orig[0], orig[1], orig[2], slicesToBeOptimized_for_residual_map2.get(i).get(entry.getKey()).get(p).x_rot, slicesToBeOptimized_for_residual_map2.get(i).get(entry.getKey()).get(p).y_rot, slicesToBeOptimized_for_residual_map2.get(i).get(entry.getKey()).get(p).z_rot);
                        //System.out.println("distance: " + distance);
                        if(distance > 0.0) {

                            //System.out.println("Distanec: " + distance);
                            //System.out.println(po_x_rot.value(c2.t) + " " + po_y_rot.value(c2.t) + " " + po_x_rot.value(c2.t));
                        }

                        if(i == 506 && debugConter == 14){
                            //System.out.println("MITA VITTUA33333: " + orig_x + " " + orig_y + " " + orig_z);
                            //System.out.println(Arrays.toString(rotated));

                            /*
                            System.out.println(Arrays.toString(orig));
                            System.out.println("MITA VITTUA22222: " + orig_x + " " + orig_y + " " + orig_z);
                            slicesToBeOptimized_for_residual_map2.get(i).get(entry.getKey()).get(p).print();
                            //System.out.println(distance);
                            //System.out.println(Arrays.toString(orig) + " ___ " + Arrays.toString(rotated));
                            System.out.println((pointMatrix.get(0, 0) + tempTrans[0]));
                            System.out.println((pointMatrix.get(0, 1) + tempTrans[1]));
                            System.out.println((pointMatrix.get(0, 2) + tempTrans[2]));
                            System.out.println("------------------");

                             */
                            //System.out.println("------------------");
                        }
                        debugConter++;


                    }

                }
            }
        }

        double resid = 10.0;
        double[] resid2 = new double[]{0,0};

        long startTime = System.currentTimeMillis();



        /* Calculate residual based on new point locations */
        try {
            resid2 = calculateResidual();
            long estimatedTime = System.currentTimeMillis() - startTime;
            //System.out.println("Elapsed time: " + estimatedTime + "ms");
        }catch (Exception e){
            e.printStackTrace();
        }


        initialized = true;

        output[0] = resid2[0];
        //residual.set(1, 0, resid2[1]);
        //System.out.println(resid);
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

    public double[] calculateResidual() throws Exception{

        int iter = 250;

        numberOfCircles = 0;

        SimpleRegression SR = new SimpleRegression(true);


        DecimalFormat format =
                new DecimalFormat("000.00000000",
                        new DecimalFormatSymbols(Locale.US));
        CircleFitter fitter = new CircleFitter();

        RANSAC_circleFitter fitter_ransac = new RANSAC_circleFitter();

        double[] output = new double[]{0,0};
        //double output = 0;
        ArrayList<Point2D.Double> slicePoints = new ArrayList<>();

        double x_sum = 0, y_sum = 0;
        //double center_x, center_y;

        double minx = Double.POSITIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;

        double maxx = Double.NEGATIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        int counter = 0;
        int counter2 = 0;
        double residualSum = 0;

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
            //slicesToBeOptimized.add(new ArrayList<>());



            for (TreeSet<cloudPoint> s : trunkPoints_for_residual) {

                slicePoints.clear();

                double current_z = s.first().z;

                minx = Double.POSITIVE_INFINITY;
                miny = Double.POSITIVE_INFINITY;

                maxx = Double.NEGATIVE_INFINITY;
                maxy = Double.NEGATIVE_INFINITY;
                x_sum = 0;
                y_sum = 0;
                //
                tempList.clear();
                counterTrunk++;
                //System.out.println(counterTrunk + " " + trunkPoints_for_residual.size());
                for (cloudPoint c : s) {


                    slicePoints.add(new Point2D.Double(c.x, c.y));

                    tempList.add(c);

                    //slicesToBeOptimized.get(slicesToBeOptimized.size() - 1).add(new cloudPoint(c.x, c.y, c.z, c.t, c.id));

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

                    if (c.z >= current_z + 0.33) {

                        current_z = c.z;


                        if (slicePoints.size() < 10) {

                            //System.out.println(counterTrunk);
                            x_sum = 0;
                            y_sum = 0;
                            slicePoints.clear();
                            tempList.clear();
                            minx = Double.POSITIVE_INFINITY;
                            miny = Double.POSITIVE_INFINITY;

                            maxx = Double.NEGATIVE_INFINITY;
                            maxy = Double.NEGATIVE_INFINITY;
                            //slicesToBeOptimized.get(slicesToBeOptimized.size()-1).clear();
                            continue;

                        }


                        x_sum /= slicePoints.size();
                        y_sum /= slicePoints.size();

                        Point2D.Double[] points =
                                slicePoints.toArray(new Point2D.Double[slicePoints.size()]);

                        //fitter.initialize2(points, x_sum, y_sum, 0.15);

/*
                    System.out.println("n_points: " + slicePoints.size());
                    System.out.println("x_dim " + (maxx-minx) + " y_dim " + (maxy - miny));
                    System.out.println("initial circle: "
                            + format.format(fitter.getCenter().x)
                            + " "     + format.format(fitter.getCenter().y)
                            + " "     + format.format(fitter.getRadius()));

 */
                        // minimize the residuals

                        try {
                            //fitter.initialize(points);
                            fitter_ransac.initialize_arraylist(slicePoints, 10, 6, 0.02, 200);
                            //fitter_ransac.optimize();

                            //fitter.initialize2(points, x_sum, y_sum, 0.15);
                            //iter = fitter.minimize(1000, 0.1, 1.0e-12);



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
                            //slicesToBeOptimized.get(slicesToBeOptimized.size()-1).clear();
                            //System.out.println("Failed to converge!");
                            //System.exit(1);
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
                            //slicesToBeOptimized.get(slicesToBeOptimized.size()-1).clear();
                            //System.out.println("Failed to converge!");
                            //System.exit(1);
                            continue;
                        }

                        System.out.println(Arrays.toString(uads));
                        System.out.println(fitter_ransac.radius);
                        //System.out.println("ransac: " + fitter_ransac.radius + "\nnormal: " + fitter.rHat);
                    /*
                    System.out.println("converged after " + iter + " iterations");
                    System.out.println("final circle: "
                            + format.format(fitter.getCenter().x)
                            + " "     + format.format(fitter.getCenter().y)
                            + " "     + format.format(fitter.getRadius()));

                    System.out.println("Residual: " + fitter.J);


                     */
                        x_sum = 0;
                        y_sum = 0;

                        //slicePoints.clear();
/*
                    System.out.println("#######################");
                    System.out.println("#######################");
                    System.out.println("#######################");


 */
                        //System.out.println(fitter.rHat + " " + fitter_ransac.radius);
                        //System.out.println(fitter.J + " " + fitter_ransac.cost);
                        //System.out.println(fitter_ransac.center_x + " " + fitter_ransac.center_y);
                        //System.exit(1);

                        minx = Double.POSITIVE_INFINITY;
                        miny = Double.POSITIVE_INFINITY;

                        maxx = Double.NEGATIVE_INFINITY;
                        maxy = Double.NEGATIVE_INFINITY;

                        //if (Math.abs(fitter.J) < 0.5 && fitter.rHat < 0.5) {
                        //if(true){
                        if (Math.abs(fitter_ransac.cost) < 0.10 && fitter_ransac.radius < 0.5) {

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
                            //slicesToBeOptimized.get(slicesToBeOptimized.size()-1).clear();
                        }

                        slicePoints.clear();
                        tempList.clear();
                    }

                    //System.out.println(c.z);

                }
                tempList.clear();
            /*
            System.out.println("-------------------");
            System.out.println("-------------------");
            System.out.println("-------------------");
            System.out.println("-------------------");
            System.out.println("-------------------");
            System.out.println("-------------------");
            System.out.println("-------------------");
            System.out.println("-------------------");

             */
            }

            //System.out.println("maxx: " + maxXdim);
            //ntln("maxy: " + maxYdim);
            //System.exit(1);
            //slicesToBeOptimized.remove(slicesToBeOptimized.size()-1);
            //System.out.println(numberOfCircles + " == " + slicesToBeOptimized.size());
            //System.exit(1);

            slicesToBeOptimized_for_residual = new ArrayList<>();
            slicesToBeOptimized_for_residual_map = new ArrayList<>();
            slicesToBeOptimized_for_residual_map2 = new ArrayList<>();
            slices_trunk_ids = new ArrayList<>();

            for(int i = 0; i < slicesToBeOptimized.size(); i++){

                slicesToBeOptimized_for_residual.add(new ArrayList<>());
                slicesToBeOptimized_for_residual_map.add(new TreeMap<>());
                slicesToBeOptimized_for_residual_map2.add(new TreeMap<>());

                slices_trunk_ids.add(slicesToBeOptimized.get(i).get(0).trunk_id);

                for(int j = 0; j < slicesToBeOptimized.get(i).size(); j++){

                    slicesToBeOptimized_for_residual.get(i).add(slicesToBeOptimized.get(i).get(j).copy());

                    if(!slicesToBeOptimized_for_residual_map.get(slicesToBeOptimized_for_residual_map.size()-1).containsKey(slicesToBeOptimized.get(i).get(j).id)){
                        slicesToBeOptimized_for_residual_map.get(slicesToBeOptimized_for_residual_map.size()-1).put(slicesToBeOptimized.get(i).get(j).id, new ArrayList<>());
                        slicesToBeOptimized_for_residual_map.get(slicesToBeOptimized_for_residual_map.size()-1).get(slicesToBeOptimized.get(i).get(j).id).add(slicesToBeOptimized.get(i).get(j).copy());
                    }else{
                        slicesToBeOptimized_for_residual_map.get(slicesToBeOptimized_for_residual_map.size()-1).get(slicesToBeOptimized.get(i).get(j).id).add(slicesToBeOptimized.get(i).get(j).copy());

                    }

                    if(!slicesToBeOptimized_for_residual_map2.get(slicesToBeOptimized_for_residual_map2.size()-1).containsKey(slicesToBeOptimized.get(i).get(j).id)){
                        slicesToBeOptimized_for_residual_map2.get(slicesToBeOptimized_for_residual_map2.size()-1).put(slicesToBeOptimized.get(i).get(j).id, new ArrayList<>());
                        //slicesToBeOptimized_for_residual_map2.get(slicesToBeOptimized_for_residual_map2.size()-1).get(slicesToBeOptimized.get(i).get(j).id).add(new Point2D.Double(slicesToBeOptimized.get(i).get(j).x, slicesToBeOptimized.get(i).get(j).y));
                        slicesToBeOptimized_for_residual_map2.get(slicesToBeOptimized_for_residual_map2.size()-1).get(slicesToBeOptimized.get(i).get(j).id).add(slicesToBeOptimized.get(i).get(j).copy());


                    }else{
                        //slicesToBeOptimized_for_residual_map2.get(slicesToBeOptimized_for_residual_map2.size()-1).get(slicesToBeOptimized.get(i).get(j).id).add(new Point2D.Double(slicesToBeOptimized.get(i).get(j).x, slicesToBeOptimized.get(i).get(j).y));
                        slicesToBeOptimized_for_residual_map2.get(slicesToBeOptimized_for_residual_map2.size()-1).get(slicesToBeOptimized.get(i).get(j).id).add(slicesToBeOptimized.get(i).get(j).copy());

                    }

                }
            }

            ArrayList<Double> radii = new ArrayList<>();
            ArrayList<double[]> centers = new ArrayList<>();



            double distance = 0.0;
            double sd = 0.0, sd_center = 0.0;

            for(int i = 0; i < slicesToBeOptimized_for_residual.size(); i++){

                cloudPoint[] points =
                        slicesToBeOptimized_for_residual.get(i).toArray(new cloudPoint[slicesToBeOptimized_for_residual.get(i).size()]);

                // System.out.println("map size: " + slicesToBeOptimized_for_residual_map.get(i).size());
                //              System.out.println(slicesToBeOptimized_for_residual.get(i).size());

                //for(int j = 0; j < slicesToBeOptimized_for_residual_map.get(i).size(); j++){

                ArrayList<cloudPoint> newList = new ArrayList<>();
                radii.clear();
                centers.clear();
                int counter_debug = 0;
                ArrayList<Integer> hahhus = new ArrayList<>();
                ArrayList<cloudPoint> debugPoints = new ArrayList<>();

                for(Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map2.get(i).entrySet()){
                    //System.out.println("HERE!!" + entry.getValue().size());
                    counter_debug++;
                    if(entry.getValue().size() > 10) {
                        cloudPoint[] points2 =
                                entry.getValue().toArray(new cloudPoint[entry.getValue().size()]);

                        try {

                            // int n = (int)Math.min

                            int d = (int)Math.max(3, (entry.getValue().size() - 3) * 0.5);

                            fitter_ransac.initialize3(entry.getValue(), 3, d, 0.02, 1000, 1);

/*
                            System.out.println("ransac: " + fitter_ransac.radius_ransac);

                            fitter_ransac.initialize_arraylist(entry.getValue(), 6, d, 0.01, iter);
                            System.out.println("non ransac: " + fitter_ransac.radius);

 */
                            /* HERE WE REPLACE THE FLIGHT LINE SLICE WITH ONLY THE RANSAC INLIER POINTS! */
                            entry.setValue((ArrayList<cloudPoint>)fitter_ransac.ransac_inlier_points_c.clone());
                            //ArrayList<cloudPoint> newList = new ArrayList<>();



                            slicesToBeOptimized_for_residual_map.get(i).put(entry.getKey(), (ArrayList<cloudPoint>)fitter_ransac.ransac_inlier_points_c.clone());
                            //System.out.println(i + " " + entry.getKey());

                            if(false)
                                if(i == 425){
                                    debugPoints.addAll(entry.getValue());
                                }

                            //System.out.println("n: 4 d: " + d + " points: " + entry.getValue().size());
                            //System.out.println("ransac: " + fitter_ransac.radius);
                            //System.out.println("-----------------");
                            radii.add(fitter_ransac.radius_ransac);
                            centers.add(new double[]{fitter_ransac.center_x, fitter_ransac.center_y});
                            hahhus.add(counter_debug);

                            newList.addAll(entry.getValue());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }else{
                        entry.getValue().clear();
                        slicesToBeOptimized_for_residual_map.get(i).get(entry.getKey()).clear();
                    }
                }
                if(radii.size() > 1) {

                    sd = sd(radii);
                    sd_center = sd_centers(centers);
                    sd_count++;
                    if(false)
                        if(i == 425){
                            System.out.println("i: " + i + " " + Arrays.toString(hahhus.toArray()) + " " + sd_center);

                            for(int p = 0; p < debugPoints.size(); p++){
                                debugPoints.get(p).print();
                            }

                        }
                    //
                    //System.out.println("sd_center: " + sd_center);

                    sum_sd += sd;
                    sum_sd_centers += sd_center;

                }

                //fitter_ransac.initialize(points, 10, 6, 0.02, 200);
                //System.out.println("non ransac: " + fitter_ransac.radius);
                slicesToBeOptimized_for_residual.set(i, (ArrayList<cloudPoint>) newList.clone());
                int d = (int)Math.max(5, (slicesToBeOptimized_for_residual.get(i).size() - 4) * 0.8);

                //
                fitter_ransac.initialize_arraylist_c(slicesToBeOptimized_for_residual.get(i), 4, d, 0.02, iter);
                //System.out.println("ransac: " + fitter_ransac.radius);
                //System.out.println("----------------");
                if (Math.abs(fitter_ransac.cost) < 0.5 && Math.abs(fitter_ransac.radius) < 0.5) {
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
/*
                        sd = sd(radii);
                        sd_center = sd_centers(centers);
                        sd_count++;

                        sum_sd += sd;
                        sum_sd_centers += sd_center;
                        */
                    good++;

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
                }

                // }

            }

            //slicesToBeOptimized_for_residual = (ArrayList<ArrayList<Point2D.Double>>)slicesToBeOptimized.clone();

        }else{
            double[] orig = new double[3];
            double[] rotated = new double[3];

            ArrayList<Double> radii = new ArrayList<>();
            ArrayList<double[]> centers = new ArrayList<>();

            double distance = 0.0;
            double sd = 0.0, sd_center = 0.0;
            //System.out.println("Here");
            //System.out.println(slicesToBeOptimized_for_residual.size());

            for(int i = 0; i < slicesToBeOptimized_for_residual.size(); i++){
                //System.out.println(i);
//                System.out.println(slicesToBeOptimized_for_residual.get(i).size());

                // Point2D.Double[] points =
                // (Point2D.Double[]) slicesToBeOptimized_for_residual.get(i).toArray(new Point2D.Double[slicesToBeOptimized_for_residual.get(i).size()]);

                // System.out.println("map size: " + slicesToBeOptimized_for_residual_map.get(i).size());
                //              System.out.println(slicesToBeOptimized_for_residual.get(i).size());

                //for(int j = 0; j < slicesToBeOptimized_for_residual_map.get(i).size(); j++){

                radii.clear();
                centers.clear();

                boolean debug = false;

                int counter_debug = 0;
                ArrayList<Integer> hahhus = new ArrayList<>();
                ArrayList<cloudPoint> debugPoints = new ArrayList<>();
                for(Map.Entry<Integer, ArrayList<cloudPoint>> entry : slicesToBeOptimized_for_residual_map2.get(i).entrySet()){
                    //System.out.println("HERE!!" + entry.getValue().size());
                    counter_debug++;
                    if(entry.getValue().size() > 0) {
                        cloudPoint[] points2 =
                                entry.getValue().toArray(new cloudPoint[entry.getValue().size()]);

                        try {

                            int d = (int)Math.max(5, (entry.getValue().size() - 6) * 0.8);

                            //fitter_ransac.initialize(points2, 10, 6, 0.01, 200);

                            //System.out.println("non ransac: " + fitter_ransac.radius);


                            //fitter_ransac.initialize2(entry.getValue(), 6, d, 0.01, iter, 1);
                            fitter_ransac.initialize_arraylist_c(entry.getValue(), 6, d, 0.01, iter);

                            if(i == 425){
                                debug = true;
                                debugPoints.addAll(entry.getValue());
                                //System.out.println("DEBUF222: " + fitter_ransac.radius + " " + fitter_ransac.center_x + " " + fitter_ransac.center_y);

                            }


                            //System.out.println(" radius: " + fitter_ransac.radius);
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


                    if(false)
                        if(i == 425 && sd_center >= 0.2){
                            System.out.println("i: " + i + " " + Arrays.toString(hahhus.toArray()) + " " + sd_center);

                            for(int p = 0; p < debugPoints.size(); p++){
                                debugPoints.get(p).print();
                            }

                            System.exit(1);

                        }

                    sum_sd += sd;
                    sum_sd_centers += sd_center;

                }

                //fitter_ransac.initialize(points, 10, 6, 0.02, 200);

                int d = (int)Math.max(5, (slicesToBeOptimized_for_residual.get(i).size() - 4) * 0.8);

                //fitter_ransac.initialize2(slicesToBeOptimized_for_residual.get(i), 4, d, 0.02, iter, 1);
                fitter_ransac.initialize_arraylist_c(slicesToBeOptimized_for_residual.get(i), 4, d, 0.02, iter);

                if (Math.abs(fitter_ransac.cost) < 0.5 && Math.abs(fitter_ransac.radius) < 0.5) {
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
/*
                        sd = sd(radii);
                        sd_center = sd_centers(centers);
                        sd_count++;

                        sum_sd += sd;
                        sum_sd_centers += sd_center;
*/
                    //System.out.println("sd: " + sd);

                    good++;
                }else{
                    bad++;
                }

                //}



                if(true)
                    continue;
                //System.out.println("sd: " + sd);
                //System.out.println("sd_centers: " + sd_center);
                //System.out.println("------------");


                try {
                    //fitter.initialize2(points, original_fit_for_slice.get(i)[0], original_fit_for_slice.get(i)[1], original_fit_for_slice.get(i)[2]);
                    //fitter.initialize(points);

                    //.initialize(points, 10, 6, 0.02, 200);

                    //System.out.println(fitter_ransac.radius + " ?==? " + original_fit_for_slice.get(i)[2]);
                    //iter = fitter.minimize(100, 0.1, 1.0e-12);

                    //fitter_ransac.optimize();

                } catch (Exception e) {
                    e.printStackTrace();
/*
                    fitter.center.x = original_fit_for_slice.get(i)[0];
                    fitter.center.y = original_fit_for_slice.get(i)[1];
                    fitter.rHat = original_fit_for_slice.get(i)[2];
                    fitter.computeCost();
*/
                    //if(fitter.J > 24911) {
                    //  System.out.println(e);
                    //System.out.println(original_fit_for_slice.get(i)[0] + " " + original_fit_for_slice.get(i)[1] + " " + original_fit_for_slice.get(i)[2] + " " + original_fit_for_slice.get(i)[3]);


                    //System.out.println(fitter.J);
                    //System.out.println(original_fit_for_slice.size() + " ? == ? " + slicesToBeOptimized.size() + " ?==? " + slicesToBeOptimized_for_residual.size());

                    //double[] min_max = minMax(points);

                    //System.out.println((min_max[1] - min_max[0]) + " " + (min_max[3] - min_max[2]) );
                    //min_max = minMax(slicesToBeOptimized.get(i));
                    //System.out.println((min_max[1] - min_max[0]) + " " + (min_max[3] - min_max[2]) );
                    //System.out.println(original_fit_for_slice.get(i)[2]);


                    if (false)
                        for (int j = 0; j < slicesToBeOptimized.get(i).size(); j++) {

                            orig[0] = slicesToBeOptimized.get(i).get(j).x;
                            orig[1] = slicesToBeOptimized.get(i).get(j).y;
                            orig[2] = slicesToBeOptimized.get(i).get(j).z;

                            rotated[0] = slicesToBeOptimized_for_residual.get(i).get(j).x;
                            rotated[1] = slicesToBeOptimized_for_residual.get(i).get(j).y;
                            rotated[2] = slicesToBeOptimized.get(i).get(j).z;

                            distance = distance3d(orig[0], orig[1], orig[2], rotated[0], rotated[1], rotated[2]);

                            //System.out.println(orig[0] + " " + orig[1]);

                            System.out.println("ddisti: " + distance);

                        }

                    //System.out.println("------------------------------");
                    //}

                    radiusSum += fitter.rHat;
                    radiusCount++;

                    residualSum += fitter.J;
                    //System.out.println("Using orig dim: " + fitter.J);
                    //System.out.println(Arrays.toString(original_fit_for_slice.get(i)));
                    counter++;
                    numberOfCircles++;
                    continue;

                }

                //System.out.println("ransac: " + fitter_ransac.radius + "\nnormal: " + fitter.rHat);
/*
                System.out.println("Got through! ");
                System.out.println("------------------------");
                System.out.println("------------------------");
                System.out.println("------------------------");

 */
                //         System.out.println(iter);

                //if (Math.abs(fitter.J) < 0.5) {
                // System.out.println(Math.abs(fitter_ransac.radius));
                if (Math.abs(fitter_ransac.cost_all) < 0.5 && Math.abs(fitter_ransac.radius_ransac) < 0.5) {
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

                }else{
/*
                    trunkRegressionPenalty[slices_trunk_ids.get(i)] = 3.0;
                    residualSum += fitter_ransac.cost * 3;
                    radiusSum += fitter_ransac.radius;
                    radiusCount++;
                    //System.out.println(radiusSum);
                    counter++;
                    numberOfCircles++;
*/
                }



            }

            //System.out.println("----------------");
        }

        double errorSum = 0.0;
        //if(initialized)
        for(int i = 0; i < trunkRegressions.length; i++){
            if(trunkRegressions[i].getN() > 0) {
                if(!Double.isNaN(trunkRegressions[i].getMeanSquareError())) {

                    errorSum += trunkRegressions[i].getMeanSquareError(); // * trunkRegressionPenalty[i];
                    counter2++;

                }
            }
        }

        this.meanRadius = radiusSum / radiusCount;
        //System.out.println("Circles: " + numberOfCircles);
        //System.out.println("Mean radisu: " + this.meanRadius);

        if(true) {
            output[0] = residualSum / (double)good;
            //output[0] = sum_sd / sd_count;
            output[1] = sum_sd / sd_count;
            //output[1] = residualSum / (double)good;

            thisCosts[0] = output[0];
            thisCosts[1] = output[1];
        }
        else {
            output[0] = sum_sd / sd_count;
            output[1] = sum_sd_centers / sd_count;
        }

        if(writeToFile){
            bw.close();
        }
        //System.out.println("Good trunks: " + goodTrunks.size());
        //System.out.println(good + " " + bad + " " + (good+bad));
        //System.out.println(Arrays.toString(output));

        //output = residualSum / (double)counter;
        return output;
    }
    /*
        public double circleOverlap(double x1, double y1, double r1, double x2, double y2, double r2){

            double overlap = 0.0;
            double small_x, small_y, large_x, large_y;
            double large = Math.max(r1, r2), small;

            if(large == r2){
                small = r1;
                small_x = x1;
                small_y = y1;
                large_x = x2;
                large_y = y2;
            }else{

                small = r2;
                small_x = x2;
                small_y = y2;
                large_x = x1;
                large_y = y1;

            }

            double distance = euclideanDistance(x1, y1, x2, y2);

            if (distance >= (r1 + r2)) {
                return 0; // 0% coverage
            }else if (large >= distance + small) {
                return (PI * (small*small)) / (PI * (large*large)) ; // 100% Coverage
            }

            double x1_1 = (distance*distance- small*small + large*large )/(2.0*distance);
            // Distance of cross-point from origin of right circle
            // (symmetry means we can shortcut with absolute value)
            double x2_1 = Math.abs(distance - x1_1);

            double y = Math.sqrt(large*large - (x1_1*x1_1));

            // Area of left lens
            double a1 = ($r1*$r1) * acos($x1 / $r1) - $x1 * $y;
            // Area of right lens
            $a2 = ($r2*$r2) * acos($x2 / $r2) - $x2 * $y;

            // Special case, if the cross-point is further away from the origin of
            // the first circle than the second circle is, then a2 needs to be the
            // large side of the circle, not the small one:
            if ($x1 > $d)
                $a2 = (pi() * $r2*$r2) - $a2;

            $overlap_area = $a1 + $a2;

            // The total area of the union is:
            // area of circle 1 + area of circle 2 - overlap_area
            // (as the overlap area only needs to be counted once)
            $total_area = pi() * ($r1*$r1 + $r2*$r2) - $overlap_area;
            return $overlap_area / $total_area;


            return overlap;

        }
    */
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

    public static double sd (ArrayList<Double> table)
    {
        // Step 1:
        double mean = mean(table);
        double temp = 0;

        for (int i = 0; i < table.size(); i++)
        {
            double val = table.get(i);

            // Step 2:
            double squrDiffToMean = Math.pow(val - mean, 2);

            // Step 3:
            temp += squrDiffToMean;
        }

        // Step 4:
        double meanOfDiffs = temp / (double) (table.size());

        // Step 5:
        return Math.sqrt(meanOfDiffs);
    }

    public double sd_centers(ArrayList<double[]> points){

        double sd = 0.0d;
        double mean_x = 0, mean_y = 0;
        for(int i = 0; i < points.size(); i++){

            mean_x += points.get(i)[0];
            mean_y += points.get(i)[1];

        }

        mean_x /= points.size();
        mean_y /= points.size();

        for(int i = 0; i < points.size(); i++){

            sd += euclideanDistance(mean_x, mean_y, points.get(i)[0], points.get(i)[1]);

        }

        sd /= points.size();
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

    public void createPolynomialSplines(double[] param){

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

            y_x_rot_list.get(i)[0] = param[startFrom + 0];
            y_y_rot_list.get(i)[0] = param[startFrom + 1];
            y_z_rot_list.get(i)[0] = param[startFrom + 2];

            y_x_list.get(i)[0] = param[startFrom + 3];
            y_y_list.get(i)[0] = param[startFrom + 4];
            y_z_list.get(i)[0] = param[startFrom + 5];

            y_x_rot_list.get(i)[y_x_list.get(i).length - 2] = param[startFrom + segs*6 - 6];
            y_y_rot_list.get(i)[y_x_list.get(i).length - 2] = param[startFrom + segs*6 - 5];
            y_z_rot_list.get(i)[y_x_list.get(i).length - 2] = param[startFrom + segs*6 - 4];

            y_x_list.get(i)[y_x_list.get(i).length - 2] = param[startFrom + segs*6 - 3];
            y_y_list.get(i)[y_x_list.get(i).length - 2] = param[startFrom + segs*6 - 2];
            y_z_list.get(i)[y_x_list.get(i).length - 2] = param[startFrom + segs*6 - 1];

            y_x_rot_list.get(i)[y_x_list.get(i).length - 1] = param[startFrom + segs*6 - 6];
            y_y_rot_list.get(i)[y_x_list.get(i).length - 1] = param[startFrom + segs*6 - 5];
            y_z_rot_list.get(i)[y_x_list.get(i).length - 1] = param[startFrom + segs*6 - 4];

            y_x_list.get(i)[y_x_list.get(i).length - 1] = param[startFrom + segs*6 - 3];
            y_y_list.get(i)[y_x_list.get(i).length - 1] = param[startFrom + segs*6 - 2];
            y_z_list.get(i)[y_x_list.get(i).length - 1] = param[startFrom + segs*6 - 1];

            for (int h = startFrom; h < startFrom + segs*6; h += 6) {

                y_x_rot_list.get(i)[counti] = param[h + 0];
                y_y_rot_list.get(i)[counti] = param[h + 1];
                y_z_rot_list.get(i)[counti] = param[h + 2];

                y_x_list.get(i)[counti] = param[h + 3];
                y_y_list.get(i)[counti] = param[h + 4];
                y_z_list.get(i)[counti] = param[h + 5];

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
    }

    public void createPolynomialSplines2(DMatrixRMaj param){

        for(int ii = 0; ii < flightLineIds.size(); ii++){


            int i = flightLineIds.get(ii);
            strip strippi = stripInformation.get(i);

            int counti = 1;

            int segs = strippi.n_segments;
            int startFrom = strippi.flightLineParamsStartFrom * 7;
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

            y_x_rot_list.get(i)[0] = param.get(startFrom + 0, 0);
            y_y_rot_list.get(i)[0] = param.get(startFrom + 1, 0);
            y_z_rot_list.get(i)[0] = param.get(startFrom + 2, 0);

            y_x_list.get(i)[0] = param.get(startFrom + 3, 0);
            y_y_list.get(i)[0] = param.get(startFrom + 4, 0);
            y_z_list.get(i)[0] = param.get(startFrom + 5, 0);
            y_scale_list.get(i)[0] = param.get(startFrom + 6, 0);

            y_x_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*7 - 7, 0);
            y_y_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*7 - 6, 0);
            y_z_rot_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*7 - 5, 0);

            y_x_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*7 - 4, 0);
            y_y_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*7 - 3, 0);
            y_z_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*7 - 2, 0);
            y_scale_list.get(i)[y_x_list.get(i).length - 2] = param.get(startFrom + segs*7 - 1, 0);

            y_x_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*7 - 7, 0);
            y_y_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*7 - 6, 0);
            y_z_rot_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*7 - 5, 0);

            y_x_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*7 - 4, 0);
            y_y_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*7 - 3, 0);
            y_z_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*7 - 2, 0);
            y_scale_list.get(i)[y_x_list.get(i).length - 1] = param.get(startFrom + segs*7 - 1, 0);


            for (int h = startFrom; h < startFrom + segs*7; h += 7) {

                y_x_rot_list.get(i)[counti] = param.get(h + 0, 0);
                y_y_rot_list.get(i)[counti] = param.get(h + 1, 0);
                y_z_rot_list.get(i)[counti] = param.get(h + 2, 0);

                y_x_list.get(i)[counti] = param.get(h + 3, 0);
                y_y_list.get(i)[counti] = param.get(h + 4, 0);
                y_z_list.get(i)[counti] = param.get(h + 5, 0);
                y_scale_list.get(i)[counti] = param.get(h + 6, 0);
                //System.out.println("Should be one: " + (param.get(h + 6, 0)) );

                counti++;

            }

            if(false)
                if(ii == 1){
                    System.out.println(startFrom);
                    System.out.println(segs);
                    System.out.println(startFrom + segs*7 - 7);
                    System.exit(1);
                }


            po_x_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_x_rot_list.get(i));
            po_y_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_y_rot_list.get(i));
            po_z_rot_sparseArray[i] = interpolatori.interpolate(x_seg, y_z_rot_list.get(i));

            po_x_sparseArray[i] = interpolatori.interpolate(x_seg, y_x_list.get(i));
            po_y_sparseArray[i] = interpolatori.interpolate(x_seg, y_y_list.get(i));
            po_z_sparseArray[i] = interpolatori.interpolate(x_seg, y_z_list.get(i));

            po_scale_sparseArray[i] = interpolatori.interpolate(x_seg, y_scale_list.get(i));



            po_x_rot_list.put(i, interpolatori.interpolate(x_seg, y_x_rot_list.get(i)));
            po_y_rot_list.put(i, interpolatori.interpolate(x_seg, y_y_rot_list.get(i)));
            po_z_rot_list.put(i, interpolatori.interpolate(x_seg, y_z_rot_list.get(i)));
            po_x_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
            po_y_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));
            po_z_list.put(i, interpolatori.interpolate(x_seg, y_x_list.get(i)));

        }


        //System.out.println("SUCCESS!!!");
        //System.exit(1);

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
        makeRotationMatrix3(tempMatrix,
                po_x_rot_sparseArray[id].value(time),
                po_y_rot_sparseArray[id].value(time),
                po_z_rot_sparseArray[id].value(time));

        //System.out.println(scale);
        rotatePoint(point, tempMatrix, (time), 1.0);

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

    public void setTrajectory(TreeMap<Double,double[]> trajectory){

        this.trajectory = trajectory;
    }
}
