package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.ejml.data.DMatrixRMaj;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import utils.*;

import java.io.*;
import java.util.*;

class MedianOfIntegerStream {


    public Queue<Double> minHeap, maxHeap;
    public boolean hasData = false;

    MedianOfIntegerStream() {
        minHeap = new PriorityQueue<>();
        maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
    }

    void add(double num) {

        hasData = true;

        if (!minHeap.isEmpty() && num < minHeap.peek()) {
            maxHeap.offer(num);
            if (maxHeap.size() > minHeap.size() + 1) {
                minHeap.offer(maxHeap.poll());
            }
        } else {
            minHeap.offer(num);
            if (minHeap.size() > maxHeap.size() + 1) {
                maxHeap.offer(minHeap.poll());
            }
        }
    }

    double getMedian() {
        double median;
        if (minHeap.size() < maxHeap.size()) {
            median = maxHeap.peek();
        } else if (minHeap.size() > maxHeap.size()) {
            median = minHeap.peek();
        } else {
            median = (minHeap.peek() + maxHeap.peek()) / 2;
        }
        return median;
    }
}

public class trunkDBH {

    ArrayList<double[][]> polygons;
    ArrayList<Double> plotIds;
    ArrayList<String> plotIdsString = new ArrayList<String>();


    TreeMap <Double, Double> GRUBB = new TreeMap<>();

    HashMap<Integer, int[]> trunkMatches;

    /*
    TreeMap<Double, Double> GRUBB = Stream.of(new double[][] {
            {3,	1.1543},	 	{15,	2.5483},	 	{80,	3.3061},
            {4,	1.4812},	 	{16,	2.5857},	 	{90,	3.3477},
            {5,	1.7150},	 	{17,	2.6200},	 	{100,	3.3841},
            {6,	1.8871},	 	{18,	2.6516},	 	{120,	3.4451},
            {7,	2.0200},	 	{19,	2.6809},	 	{140,	3.4951},
            {8,	2.1266},	 	{20,	2.7082},	 	{160,	3.5373},
            {9,	2.2150},	 	{25,	2.8217},	 	{180,	3.5736},
            {10,	2.2900},	 	{30,	2.9085},	 	{200,	3.6055},
            {11,	2.3547},	 	{40,	3.0361},	 	{300,	3.7236},
            {12,	2.4116},	 	{50,	3.1282},	 	{400,	3.8032},
            {13,	2.4620},	 	{60,	3.1997},	 	{500,	3.8631},
            {14,	2.5073},	 	{70,	3.2576},	 	{600,	3.9109}
    }).collect(Collectors.to(data -> data[0], data -> data[1]));
    */
    LASReader pointCloud;
    argumentReader aR;

    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
    org.tinfour.interpolation.TriangularFacetInterpolator polator;// = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);
    org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

    int[] n_trunks;

    public trunkDBH(LASReader temp, argumentReader aR, HashMap<Integer, int[]> trunkMatches, int[] n_trunks) throws Exception{

        this.pointCloud = temp;
        this.aR = aR;

        this.n_trunks = n_trunks;

        //thinPointCloud(pointCloud, 0.05, 2);
        GRUBB.put(3.0, 1.1543);
        this.trunkMatches = trunkMatches;

    }


    public void thinPointCloud(LASReader pointCloud, double step, int few) throws IOException{

        aR.step = step;
        //System.out.println(this.y_interval);
        aR.few = few;
        aR.thin3d = true;
        aR.cores = 1;
        aR.p_update = new progressUpdater(aR);

        Thinner thin = new Thinner(pointCloud, aR.step, aR, 1);

        this.pointCloud = new LASReader(thin.outputFile);
    }

    public void createTin(int groundClassification) throws IOException {

        LasPoint tempPoint = new LasPoint();

        long n = this.pointCloud.getNumberOfPointRecords();

        for(int i = 0; i < n; i++){

            this.pointCloud.readRecord(i, tempPoint);

            if(tempPoint.classification == groundClassification)
                tin.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));


        }

        polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);

    }


    public void process() throws Exception{

        readFieldPlots(new File("GridPlots_2020.shp"));

        KdTree kd_tree_trunks = new KdTree();
        double resolution = 0.5;

        int xDim1 = (int)Math.ceil((this.pointCloud.getMaxX() - this.pointCloud.getMinX()) / resolution) + 1;
        int yDim1 = (int)Math.ceil((this.pointCloud.getMaxY() - this.pointCloud.getMinY()) / resolution) + 1;

        float[][] raster = new float[xDim1][yDim1];
        ITDstatistics stats = new ITDstatistics();
        File fil = aR.measured_trees ;//    new File("/home/koomikko/Documents/codes/silvai/example_data/stems/tree_table_grid_2020.csv");
        File outputFile = new File("trunk_dbh_out.txt");
        stats.readMeasuredTrees_2d(fil);

        KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(1,2,3);

        double sliceSize = 0.1;

        RANSAC_circleFitter RcF = new RANSAC_circleFitter();

        createTin(2);

        int thread_n = aR.pfac.addReadThread(this.pointCloud);

        LasPoint tempPoint = new LasPoint();

        HashMap<Short, Integer> n_points_per_trunk = new HashMap<>();

        HashMap<Short, ArrayList<double[]>> trunk_points = new HashMap<>();
        HashMap<Short, TreeMap<Double, double[]>> trunk_points2 = new HashMap<>();
        HashMap<Integer, Double> trunk_diameters = new HashMap<>();
        HashMap<Integer, Double> trunk_plot_ids = new HashMap<>();
        HashMap<Integer, Double> trunk_diameter_scan_angle_ranks = new HashMap<>();
        //HashMap<Short, Integer> trunk_plot_id = new HashMap<>();


        for(int i = 0; i < this.pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(this.pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                this.pointCloud.readFromBuffer(tempPoint);

                int xCoord = (int) Math.floor((tempPoint.x - pointCloud.getMinX()) / resolution);
                int yCoord = (int) Math.floor((pointCloud.getMaxY() - tempPoint.y) / resolution);

                tempPoint.z -= polator.interpolate(tempPoint.x, tempPoint.y, valuator); //= tempPoint.z - raster_dz[xCoord][yCoord];

                if(tempPoint.z > raster[xCoord][yCoord])
                    raster[xCoord][yCoord] = (float)tempPoint.z;

                if(tempPoint.classification == 4){
                    if(n_points_per_trunk.containsKey(tempPoint.pointSourceId)){
                        n_points_per_trunk.put(tempPoint.pointSourceId, n_points_per_trunk.get(tempPoint.pointSourceId)+1);
                    }else{
                        n_points_per_trunk.put(tempPoint.pointSourceId, 1);
                    }
                }
            }
        }

        boolean terminate = false;

        System.out.println("Number of trunks: " + n_points_per_trunk.size());

        String out = "trunkDBH_out.txt";

        File file = new File(out);

        if(file.exists()){
            file.delete();
            file.createNewFile();
        }else{
            file.createNewFile();
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(out, true));

        BufferedWriter bw2 = new BufferedWriter(new FileWriter(outputFile, true));

        HashMap<Short,ArrayList<ArrayList<double[]>>> perTrunkPoints = new HashMap<>();

        HashMap<Short, Double> stemId_diameter = new HashMap<>();

        HashMap<Integer, Integer> localIdToGlobalId = new HashMap<>();

        File o_file = aR.createOutputFileWithExtension(pointCloud, "_trunkDiam.las");

        pointWriterMultiThread pw = new pointWriterMultiThread(o_file, pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(pointCloud.pointDataRecordLength, 1, pw);
        aR.pfac.addWriteThread(thread_n, pw, buf);

        for(int i = 0; i < this.pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(this.pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                this.pointCloud.readFromBuffer(tempPoint);

                tempPoint.z -= polator.interpolate(tempPoint.x, tempPoint.y, valuator); //= tempPoint.z - raster_dz[xCoord][yCoord];


                if(tempPoint.classification == 4){

                    n_points_per_trunk.put(tempPoint.pointSourceId, n_points_per_trunk.get(tempPoint.pointSourceId)-1);

                    if(trunk_points2.containsKey(tempPoint.pointSourceId)){
                        trunk_points2.get(tempPoint.pointSourceId).put(tempPoint.z, new double[]{tempPoint.x, tempPoint.y, tempPoint.z, Math.abs(tempPoint.scanAngleRank), tempPoint.userData});
                        //System.out.println("scan angle rank: " + tempPoint.scanAngleRank);
                    }else{
                        trunk_points2.put(tempPoint.pointSourceId, new TreeMap<>());
                        trunk_points2.get(tempPoint.pointSourceId).put(tempPoint.z, new double[]{tempPoint.x, tempPoint.y, tempPoint.z, Math.abs(tempPoint.scanAngleRank), tempPoint.userData});
                    }


                    if(n_points_per_trunk.get(tempPoint.pointSourceId) == 0){

                        ArrayList<ArrayList<double[]>> perTrunkPoints2 = new ArrayList<>();
                        ArrayList<List<int[]>> perTrunkPoints_combinations = new ArrayList<>();
                        perTrunkPoints2.add(new ArrayList<>());
                        //perTrunkPoints_combinations.add(new ArrayList<>());

                        perTrunkPoints.put(tempPoint.pointSourceId, new ArrayList<>());
                        perTrunkPoints.get(tempPoint.pointSourceId).add(new ArrayList<>());

                        bw.write("NEW_TRUNK\n");
                        bw.write(tempPoint.pointSourceId + "\n");

                        ArrayList<double[]> dataForRansac = new ArrayList<>();
                        ArrayList<double[]> dataForRansac2 = new ArrayList<>();
                        ArrayList<Double> dataForRansac_center_x = new ArrayList<>();
                        ArrayList<Double> dataForRansac_center_y = new ArrayList<>();

                        ArrayList<Double> dataForRansac_confidence = new ArrayList<>();

                        //System.out.println("ALL POINT READ FOR TRUNK: " + tempPoint.pointSourceId);

                        /* Now we have all point for trunk "tempPoint.pointSourceId" recorded */
                        ArrayList<double[]> slicePoints = new ArrayList<>();
                        ArrayList<double[]> slicePoints_temp = new ArrayList<>();
                        ArrayList<double[]> slicePoints_temp_for_ransac = new ArrayList<>();
                        ArrayList<double[]> slicePoints_prev_1 = new ArrayList<>();
                        ArrayList<double[]> slicePoints_prev_2 = new ArrayList<>();

                        ArrayList<Double> sliceConfidence = new ArrayList<>();

                        boolean sliceStarted = false;
                        double sliceStart = Double.POSITIVE_INFINITY;
                        double mean_x = 0;
                        double mean_y = 0;
                        double counter = 0;

                        ArrayList<ArrayList<double[]>> ransacInlierSlicePoints = new ArrayList<>();

                        for(double z : trunk_points2.get(tempPoint.pointSourceId).keySet()){

                            mean_x += trunk_points2.get(tempPoint.pointSourceId).get(z)[0];
                            mean_y += trunk_points2.get(tempPoint.pointSourceId).get(z)[1];
                            counter++;

                        }

                        mean_x /= counter;
                        mean_y /= counter;

                        int xCoord1 = (int) Math.floor((mean_x - pointCloud.getMinX()) / resolution);
                        int yCoord1 = (int) Math.floor((pointCloud.getMaxY() - mean_y) / resolution);

                        double zet_constraint = raster[xCoord1][yCoord1];

                        double maxRadius = zet_constraint * 1.1 / 100.0 / 2.0;
                        //System.out.println("b_height = " + maxRadius);
                        //d13 ~ b1 * (h-0)^b2
                        maxRadius = 0.5610092  * Math.pow((zet_constraint - 0.0), 1.241524) / 100.0 / 2.0 * 1.1;
                        //System.out.println("nlme: " + maxRadius);
                        //System.out.println("------------------------");

                        double minRadius = 0.5610092  * Math.pow((zet_constraint - 0.0), 1.241524) / 100.0 / 2.0 * 0.7;
                        //minRadius = maxRadius * 0.5;
                        //minRadius = 0.025;
                        RcF.setMaxRadius(maxRadius);
                        RcF.setMinRadius(minRadius);

                        mean_x = 0;
                        mean_y = 0;
                        counter = 0;

                        bw.write(trunk_points2.get(tempPoint.pointSourceId).size() + "\n");
                        ArrayList<double[]> slicePointsFinal = new ArrayList<>();

                        for(double z : trunk_points2.get(tempPoint.pointSourceId).keySet()){

                            if(z > zet_constraint * (4.0 / 4.0))
                                continue;

                            //System.out.println(z + " " + Arrays.toString(trunk_points2.get(tempPoint.pointSourceId).get(z)));
                            //System.out.println(z + " " + sliceStart + " " +  slicePoints.size());
                            bw.write(trunk_points2.get(tempPoint.pointSourceId).get(z)[0] + " " +
                                    trunk_points2.get(tempPoint.pointSourceId).get(z)[1] + " " +
                                    trunk_points2.get(tempPoint.pointSourceId).get(z)[2] + "\n");

                            mean_x += trunk_points2.get(tempPoint.pointSourceId).get(z)[0];
                            mean_y += trunk_points2.get(tempPoint.pointSourceId).get(z)[1];
                            counter++;

                            if(z - sliceStart >= sliceSize) {

                                //List<int[]> comb = generate(perTrunkPoints2.get(perTrunkPoints2.size()-1).size(), 4);
                                //perTrunkPoints_combinations.add(comb);
                                //System.out.println("Combinations: " + comb.size());

                                //perTrunkPoints.get(tempPoint.pointSourceId).add(new ArrayList<>());
                                perTrunkPoints2.add(new ArrayList<>());
                                slicePoints_temp.addAll(slicePoints);
                                slicePoints_temp.addAll(slicePoints_prev_1);
                                slicePoints_temp.addAll(slicePoints_prev_2);
                                slicePoints_temp_for_ransac = (ArrayList<double[]>)slicePoints_temp.clone();
                                HashMap<Short, ArrayList<double[]>> slice_flightlines = new HashMap<>();
                                ArrayList<KdTree> trees = new ArrayList<>();

                                ArrayList<double[]> all_points = new ArrayList<>();

                                //System.out.println(slicePoints_temp.size());
                                if(slicePoints_temp.size() > 10) {

                                    for (int i_ = 0; i_ < slicePoints_temp.size(); i_++) {

                                        if (slice_flightlines.containsKey((short) slicePoints_temp.get(i_)[4])) {
                                            slice_flightlines.get((short) slicePoints_temp.get(i_)[4]).add(new double[]{slicePoints_temp.get(i_)[0], slicePoints_temp.get(i_)[1]});
                                        } else {
                                            slice_flightlines.put((short) slicePoints_temp.get(i_)[4], new ArrayList<>());
                                            slice_flightlines.get((short) slicePoints_temp.get(i_)[4]).add(new double[]{slicePoints_temp.get(i_)[0], slicePoints_temp.get(i_)[1]});

                                        }
                                    }

                                    //System.out.println("Slice contains " + slice_flightlines.size() + " flightlines");
                                    ArrayList<double[]> flightLineCircleCenters = new ArrayList<>();
                                    ArrayList<Short> ids = new ArrayList<>();
                                    HashSet<Short> removeTheseFlightLines = new HashSet<>();
                                    ArrayList<Double> flightLineConfidence = new ArrayList<Double>();

                                    int goodFlightLines = 0;
                                    double meanError = 0;
                                    double meanError_counter = 0;

                                    for (short id : slice_flightlines.keySet()) {

                                        ids.add(id);

                                        if (slice_flightlines.get(id).size() <= 5) {
                                            removeTheseFlightLines.add(id);
                                            flightLineCircleCenters.add(new double[]{-1, -1});
                                            flightLineConfidence.add(Double.POSITIVE_INFINITY);

                                            continue;
                                        }

                                        int d = (int) Math.max(1, (slice_flightlines.get(id).size()) * 0.33 );
                                        double min_inlier = 0.66;
                                        int iter = (int) (Math.log(1.0 - 0.99) / (Math.log(1.0 - (min_inlier * min_inlier * min_inlier))));
                                        //System.out.println(iter);

                                        RcF.initialize5(slice_flightlines.get(id), 3, d, 0.03, 1000, 1, aR.set_seed);

                                        //System.out.println("Flightline: " + id + " radius(" + RcF.ransacFailed + "): " + RcF.radius_ransac);

                                        //slice_flightlines.get(id).clear();

                                        all_points.addAll(slice_flightlines.get(id));

                                        if(true)
                                        if (!RcF.ransacFailed && RcF.radius_ransac <= maxRadius) {
                                            //System.out.println("COST: " + RcF.cost_all);
                                            meanError += RcF.cost_all;
                                            meanError_counter++;

                                            goodFlightLines++;
                                            flightLineCircleCenters.add(new double[]{RcF.center_x, RcF.center_y});
                                            flightLineConfidence.add(RcF.cost_all);
                                            //slicePointsFinal.addAll(RcF.ransac_inlier_points_a);
                                            all_points.addAll(RcF.ransac_inlier_points_a);
                                            slice_flightlines.get(id).addAll(RcF.ransac_inlier_points_a);
                                        } else {
                                            //removeTheseFlightLines.add(id);
                                            flightLineCircleCenters.add(new double[]{-1, -1});
                                            flightLineConfidence.add(Double.POSITIVE_INFINITY);
                                        }

                                    }


                                    slicePoints_temp = new ArrayList(all_points);
                                    slicePoints_temp.clear();

                                    ArrayList<double[]> all_points_backup = new ArrayList<>();

                                    for(int i_ = 0; i_ < all_points.size(); i_++){

                                        slicePoints_temp.add(new double[]{all_points.get(i_)[0], all_points.get(i_)[1]});
                                       all_points_backup.add(new double[]{all_points.get(i_)[0], all_points.get(i_)[1]});

                                    }

                                    double radius = -1;

                                    if(slicePoints_temp.size() > 10){
                                    //if (goodFlightLines > 0 && all_points.size() > 10) {

                                        if(false) {
                                            double orig_center_x = 0;
                                            double orig_center_y = 0;

                                            double orig_center_counter = 0;

                                            all_points.clear();
                                            /* JIGGLE THE FLIGHT LINE POINT CLOUDS */
                                            for (short id : slice_flightlines.keySet()) {


                                                if (slice_flightlines.get(id).size() <= 5) {
                                                    continue;
                                                }

                                                RcF.initialize_arraylist_a(slice_flightlines.get(id), 0, 0, 0, 0);
                                                //System.out.println("rad: " + RcF.radius);

                                                for (int p = 0; p < slice_flightlines.get(id).size(); p++) {
                                                    //System.out.println(p);
                                                    //System.out.println(slice_flightlines.get(id).get(p)[0] + " " + RcF.center_x);
                                                    slice_flightlines.get(id).get(p)[0] -= RcF.center_x;
                                                    //System.out.println(slice_flightlines.get(id).get(p)[0] + " " + RcF.center_x);
                                                    slice_flightlines.get(id).get(p)[1] -= RcF.center_y;

                                                    orig_center_counter++;
                                                    orig_center_x += RcF.center_x;
                                                    orig_center_y += RcF.center_y;
                                                    //System.out.println("---------------");
                                                }

                                                all_points.addAll(slice_flightlines.get(id));

                                            }

                                            orig_center_x /= orig_center_counter;
                                            orig_center_y /= orig_center_counter;


                                            RcF.initialize_arraylist_a(all_points, 0, 0, 0, 0);

                                            //System.out.println("RADIUS: " + RcF.radius + " " + RcF.center_x + " " + RcF.center_y);

                                            LevenbergMarquardt lm = new LevenbergMarquardt(1);

                                            ResidFunctionCircleFit_fixed_center res = new ResidFunctionCircleFit_fixed_center();
                                            res.setPoints(all_points);

                                            DMatrixRMaj param = new DMatrixRMaj(1, 1);
                                            param.set(0, 0, RcF.radius);

                                            lm.optimize(res, param);

                                            //System.out.println("radius2: " + param.get(0, 0));
                                            radius = RcF.radius;

                                            for (int ite = 0; ite < 5; ite++) {

                                                all_points.clear();

                                                for (short id : slice_flightlines.keySet()) {


                                                    if (slice_flightlines.get(id).size() <= 5) {
                                                        continue;
                                                    }

                                                    LevenbergMarquardt lm2 = new LevenbergMarquardt(1);
                                                    ResidFunctionCircleFit_fixed_radius res2 = new ResidFunctionCircleFit_fixed_radius();
                                                    res2.setPoints(slice_flightlines.get(id));
                                                    res2.setRadius(radius);

                                                    DMatrixRMaj param2 = new DMatrixRMaj(2, 1);
                                                    param2.set(0, 0, 0);
                                                    param2.set(1, 0, 0);

                                                    lm.optimize(res2, param2);

                                                    //System.out.println("rad: " + RcF.radius);

                                                    for (int p = 0; p < slice_flightlines.get(id).size(); p++) {
                                                        //System.out.println(p);
                                                        //System.out.println(slice_flightlines.get(id).get(p)[0] + " " + RcF.center_x);
                                                        slice_flightlines.get(id).get(p)[0] -= param2.get(0, 0);
                                                        //System.out.println(slice_flightlines.get(id).get(p)[0] + " " + RcF.center_x);
                                                        slice_flightlines.get(id).get(p)[1] -= param2.get(1, 0);

                                                        //System.out.println("---------------");
                                                    }

                                                    all_points.addAll(slice_flightlines.get(id));

                                                }

                                                res.setPoints(all_points);
                                                DMatrixRMaj param3 = new DMatrixRMaj(1, 1);
                                                param.set(0, 0, radius);

                                                lm.optimize(res, param3);

                                                radius = param3.get(0, 0);
                                                //System.out.println("rad: " + radius);
                                            }
                                        }

                                        //double radius;
                                        /* FIDDLING WITH THIS PERCENTAGE INCREASES THE NUMBER OF DETECTED STEMS */
                                        int d = (int) Math.max(1, ((slicePoints_temp.size() -3 )    *  0.4));
                                        //if(all_points.size() == slicePoints_temp.size()){

                                            //System.out.println(all_points.size() + " " + slicePoints_temp.size());
                                        meanError /= meanError_counter;


                                        //System.out.println("mean erro: " + meanError);
                                        RcF.initialize5(slicePoints_temp, 3, d, 0.04, 1000, 1, aR.set_seed);
                                        //RcF.initialize5(all_points, 3, d, 0.03, 1000, 1, aR.set_seed);

                                        //System.out.println(RcF.ransacFailed + " " + RcF.radius_ransac);

                                        radius = RcF.radius_ransac;
                                        //RcF.initialize5(slicePoints_temp, 3, d, 0.01, 1000, 1, aR.set_seed);
                                            //RcF.initialize4(slicePoints_temp, 3, d, 0.015, 1000, 1, aR.set_seed);
                                            //radius = RcF.radius_ransac;
                                            //RcF.initialize_arraylist_a(all_points, 3, d, 0.010, 1000);
                                            //radius = RcF.radius;


                                        //}else {
                                        //    radius = Double.POSITIVE_INFINITY;
                                        //}
                                        //System.out.println("radius final: " + radius);
                                        //System.out.println("center: " + RcF.center_x + " " + RcF.center_y);
                                        //RcF.initialize_arraylist_a(all_points, 0,0,0,0);
                                        //System.out. println("Hyperaccurate: " + RcF.radius);
                                        //if (slicePoints.size() > 0 && slicePoints_prev_1.size() > 0 && slicePoints_prev_2.size() > 0 && slicePoints_temp.size() > 5 && slicePointsFinal.size() > 10) {
                                        //if (radius > 0.0 && radius <= 0.25 && !RcF.ransacFailed) {
                                        if (radius > minRadius && radius <= maxRadius && !RcF.ransacFailed) {

                                            //int d = (int) Math.max(1, (all_points.size()) * 0.5);
                                            //RcF.initialize5(slicePoints_temp, 3, d, 0.03, 1000, 1, aR.set_seed);

                                            //if(!RcF.ransacFailed)
                                                //radius = RcF.radius;
                                            if(!RcF.ransacFailed){

                                                sliceConfidence.add(RcF.cost_all);
                                                //RcF.initialize4(slicePoints_temp, 3, d, 0.025, 1000, 1, aR.set_seed);
                                                //RcF.initialize_arraylist_a(slicePoints_temp, 0,0,0,0);
                                                dataForRansac.add(new double[]{z - sliceSize * 1.5, radius});
                                                dataForRansac2.add(new double[]{RcF.center_x, RcF.center_y, z - sliceSize * 1.5, radius});
                                            }

                                            //System.out.println("CENTER: " + orig_center_x + " " + orig_center_y);
                                            //RcF.initialize4(all_points, 3, d, 0.035, 1000, 1, aR.set_seed);
                                            //System.out.println("Radius ransac: " + RcF.radius_ransac + " radius_hyyppa: " + radius);
                                            if (false)
                                                if (!RcF.ransacFailed) {

                                                    dataForRansac.add(new double[]{z - sliceSize * 1.5, RcF.radius_ransac});
                                                    dataForRansac2.add(new double[]{RcF.center_x, RcF.center_y, z - sliceSize * 1.5, RcF.radius_ransac});
                                                    dataForRansac_confidence.add(RcF.cost_all);
                                                    ransacInlierSlicePoints.add(new ArrayList<>());
                                                    ransacInlierSlicePoints.get(ransacInlierSlicePoints.size() - 1).addAll(slicePoints_temp);

                                                    //System.out.println("Ransac radius: " + RcF.radius_ransac + " " + RcF.cost_all);
                                                }
                                            //slicePoints.clear();
                                        }


                                    } else if(false) {
                                        //if (slicePoints.size() > 0 && slicePoints_prev_1.size() > 0 && slicePoints_prev_2.size() > 0 && slicePoints_temp.size() > 5 && slicePointsFinal.size() > 10) {
                                        int d = (int) Math.max(1, (slicePoints_temp.size()) * 0.5);
                                        RcF.initialize4(slicePoints_temp_for_ransac, 3, d, 0.02, 1000, 1, aR.set_seed);


                                        if (true)
                                            if (!RcF.ransacFailed) {
                                                //System.out.println("Hyyppa FAILED!!!: ");
                                                //System.out.println("Radius ransac: " + RcF.radius_ransac);
                                                dataForRansac.add(new double[]{z - sliceSize * 1.5, RcF.radius_ransac});
                                                dataForRansac2.add(new double[]{RcF.center_x, RcF.center_y, z - sliceSize * 1.5, RcF.radius_ransac});
                                                dataForRansac_confidence.add(RcF.cost_all);
                                                ransacInlierSlicePoints.add(new ArrayList<>());
                                                ransacInlierSlicePoints.get(ransacInlierSlicePoints.size() - 1).addAll(slicePoints_temp_for_ransac);

                                                //System.out.println("Ransac radius: " + RcF.radius_ransac + " " + RcF.cost_all);
                                            }
                                        //}
                                    }
                                }
                                slicePointsFinal.clear();
                                slicePoints_temp.clear();
                                slicePoints_temp_for_ransac.clear();
                                sliceStarted = false;
                            }

                            if(!sliceStarted) {

                                slicePoints_prev_2.clear();
                                slicePoints_prev_2.addAll(slicePoints_prev_1);
                                slicePoints_prev_1.clear();
                                slicePoints_prev_1.addAll(slicePoints);
                                slicePoints.clear();
                                sliceStart = z;
                                sliceStarted = true;

                            }

                            //perTrunkPoints.get(tempPoint.pointSourceId).get(perTrunkPoints.get(tempPoint.pointSourceId).size() - 1).add(trunk_points2.get(tempPoint.pointSourceId).get(z));
                            perTrunkPoints2.get(perTrunkPoints2.size()-1).add(trunk_points2.get(tempPoint.pointSourceId).get(z));
                            slicePoints.add(trunk_points2.get(tempPoint.pointSourceId).get(z));


                        }


                        if(false) {


                            System.out.println(perTrunkPoints2.size() + " == " + perTrunkPoints_combinations.size());

                            ResidFunctionTrunkDBH resid_dbh = new ResidFunctionTrunkDBH();
                            DMatrixRMaj param = null;
                            DMatrixRMaj residuals = null;

                            param = new DMatrixRMaj(2 * perTrunkPoints2.size(), 1);

                            for (int r = 0; r < param.numRows; r += 2) {

                                param.set(r, 0, 0.5);
                                param.set(r + 1, 0, 0.025);

                            }

                            resid_dbh.setup(perTrunkPoints2, param);

                            LevenbergMarquardt lm = new LevenbergMarquardt(1);

                            if (resid_dbh.canBeOptimized) {

                                lm.optimize(resid_dbh, param);
                                //System.exit(1);
                                resid_dbh.getDbh(perTrunkPoints2, param);
                            }

                        }

                        if(false){
                            boolean[] outliers = outliers(dataForRansac, 6);

                            ArrayList<double[]> inlies1 = new ArrayList<>();
                            ArrayList<double[]> inlies2 = new ArrayList<>();

                            for(int i_ = 0; i_ < dataForRansac2.size(); i_++){

                                if(!outliers[i_]){
                                    inlies1.add(dataForRansac.get(i_));
                                    inlies2.add(dataForRansac2.get(i_));
                                }

                            }

                            dataForRansac = (ArrayList<double[]>)inlies1.clone();
                            dataForRansac2 = (ArrayList<double[]>)inlies2.clone();

                        }

                        if(dataForRansac2.size() > 4) {

                            bw.write("GOOD\n");
                            mean_x /= counter;
                            mean_y /= counter;

                            int xCoord = (int) Math.floor((mean_x - pointCloud.getMinX()) / resolution);
                            int yCoord = (int) Math.floor((pointCloud.getMaxY() - mean_y) / resolution);

                            double zet = raster[xCoord][yCoord];

                            ArrayList<double[]> dataForRansacInverse = new ArrayList<>();
                            ArrayList<Double> sliceConfidenceInverse = new ArrayList<>();

                            for(int i_ = dataForRansac.size()-1; i_ >= 0; i_--){

                                dataForRansacInverse.add(new double[]{zet - dataForRansac.get(i_)[0],dataForRansac.get(i_)[1]});
                                sliceConfidenceInverse.add(sliceConfidence.get(i_));
                            }

                            //System.out.println("zet " + zet);
                            RansacLinearRegression RLR = new RansacLinearRegression(dataForRansacInverse, 0.33, 0.33, 0.045  , aR.set_seed);

                            /* LISÄÄ PAINOT TUONNE REGRESSIOON!!! */

                            RLR.set_n(2);

                            RLR.intercept(true);

                            RLR.constrain(zet-1.3, maxRadius, minRadius);

                            //RLR.addFixedPoint(new double[]{zet, 0.0});

                            SimpleRegression ransacRegression = RLR.optimize3();

                            System.out.println("ransac_failed: " + RLR.ransacFailed);
                            int polySize = 0;

                            for(int i_ = 0; i_ < RLR.inlierIndexes.length; i_++){
                                if(RLR.inlierIndexes[dataForRansac2.size() - 1 - i_])
                                //if(!outliers[i_])
                                    polySize++;
                            }

                            double[] x_ = new double[polySize];
                            double[] y_ = new double[polySize];

                            mean_x = 0.0;
                            mean_y = 0.0;
                            counter = 0.0;
                            bw.write(dataForRansac2.size() + "\n");

                            SplineInterpolator interpolatori = new SplineInterpolator();
                            //LoessInterpolator interpolatori = new LoessInterpolator();

                            int counti = 0;

                            for(int i_ = 0; i_ < dataForRansac2.size(); i_++){
                                //System.out.println(dataForRansac2.get(i_).length);

                                if(RLR.inlierIndexes[dataForRansac2.size() - 1 - i_]){
                                //if(!outliers[i_]){
                                    x_[counti] = dataForRansac2.get(i_)[2];
                                    y_[counti] = dataForRansac2.get(i_)[3];
                                    counti++;
                                }

                                bw.write(dataForRansac2.get(i_)[0] + " " + dataForRansac2.get(i_)[1] + " " + dataForRansac2.get(i_)[2] + " " +
                                        dataForRansac2.get(i_)[3] * 100.0 + " " + RLR.inlierIndexes[dataForRansac2.size() - 1 - i_] + "\n");

                                if(RLR.inlierIndexes[dataForRansac2.size() - 1 - i_] && counter < 3){
                                //if(!outliers[i_] && counter < 3){
                                //if(counter < 3){
                                    mean_x += dataForRansac2.get(i_)[0];
                                    mean_y += dataForRansac2.get(i_)[1];
                                    counter++;

                                }
                            }

                            double predicted_dbh = -1.0;

                            if(x_.length > 3 && true) {
                                PolynomialSplineFunction PSF = interpolatori.interpolate(x_, y_);

                                if (PSF.isValidPoint(zet -1.3)) {
                                    predicted_dbh = PSF.value(zet - 1.3);

                                    if(predicted_dbh >= 0.3)
                                        predicted_dbh = ransacRegression.predict(zet - 1.3);
                                } else {
                                    predicted_dbh = ransacRegression.predict(zet -1.3);
                                }
                            }else{
                                predicted_dbh = ransacRegression.predict(zet -1.3);
                            }
                            System.out.println("Spline(?): " + predicted_dbh);
                            mean_x /= counter;
                            mean_y /= counter;

                            bw.write(mean_x + " " + mean_y + " " + zet + "\n");

                            System.out.println("Regression(?): " + predicted_dbh);
                            System.out.println("lim: " + maxRadius);
                            //System.out.println("Ransac Predicted 1.3m: " + predicted_dbh + " " + dataForRansac.size());

                            bw.write(predicted_dbh + "\n");
                            //System.out.println(dataForRansac2.size() + " == " + ransacInlierSlicePoints.size());
                            double[] plot_id = new double[]{0};

                            boolean inside = pointInPolygons(new double[]{mean_x, mean_y}, plot_id);
                            double[] scan_angle = new double[1];
                            double newDbh_maybe = dbh_straight(dataForRansac2, RLR.inlierIndexes, ransacInlierSlicePoints, scan_angle);

                            if(Double.isNaN(predicted_dbh) || predicted_dbh <= 0.0)
                                predicted_dbh = newDbh_maybe;

                            if(!Double.isNaN(predicted_dbh) && inside && predicted_dbh > minRadius && predicted_dbh < maxRadius){


                                KdTree.XYZPoint tempTreePoint2 = new KdTree.XYZPoint(mean_x,mean_y,0.0);
                                tempTreePoint2.setIndex(n_trunks[0]);

                                kd_tree_trunks.add(tempTreePoint2);

                                //stats.kd_tree2.nearestNeighbourSearch(1,tempTreePoint);
                                //newDbh_maybe = -1.0;

                                //System.out.println("scan angle: " + scan_angle[0]);

                                /* IF there are inlier diameter estimates from BELOW and ABOVE 1.3m,
                                   we use the direct estimates from the point cloud.
                                   Otherwise, we use the predicted diameter from regression line.
                                 */
                                if(newDbh_maybe != -1.0 && false) {

/*
                                    trunk_diameters.put(tempPoint.pointSourceId, (newDbh_maybe + predicted_dbh) / 2.0);
                                    trunk_diameter_scan_angle_ranks.put(tempPoint.pointSourceId, scan_angle[0]);
                                    trunk_plot_ids.put(tempPoint.pointSourceId, plot_id[0]);
*/
                                }
                                else {

                                    stemId_diameter.put(tempPoint.pointSourceId, predicted_dbh);
                                    trunk_diameters.put(n_trunks[0], predicted_dbh);
                                    trunk_diameter_scan_angle_ranks.put(n_trunks[0], Double.NaN);
                                    trunk_plot_ids.put(n_trunks[0], plot_id[0]);
                                    localIdToGlobalId.put((int)tempPoint.pointSourceId, this.n_trunks[0]);
                                }

                                n_trunks[0]++;
                                //if(predicted_dbh > 0.15)
                                System.out.println(newDbh_maybe + " ?==? " + predicted_dbh);

                                if(false)
                                if(trunk_diameters.size() == 100) {
                                    terminate = true;
                                    break;
                                }
                            }
                        }else{
                            bw.write("BAD\n");
                        }

                    }
                }
            }
        if(terminate)
            break;
        }

        Iterator itr = kd_tree_trunks.iterator();

        List<KdTree.XYZPoint> nearest;
        List<KdTree.XYZPoint> nearest2;
        double mean_measured = 0.0;
        double mean_predicted = 0.0;
        double mean_predicted_all = 0.0;
        double mean_predicted_all_counter = 0.0;
        double counter = 0;

        ArrayList<Double> predicted = new ArrayList<>();
        ArrayList<Double> observed = new ArrayList<>();

        HashMap<Short, Short> trunkMatchId = new HashMap<>();

        HashSet<Integer> matchedTrunks = new HashSet<>();


        while(itr.hasNext()){

            KdTree.XYZPoint p = (KdTree.XYZPoint)itr.next();

            double plot_id = trunk_plot_ids.get(p.getIndex());
            double diam = trunk_diameters.get(p.getIndex());
            double scanAngle = trunk_diameter_scan_angle_ranks.get(p.getIndex());
            nearest = (List<KdTree.XYZPoint>)stats.kd_tree2.nearestNeighbourSearch(1, p);
            //nearest2 = (List<KdTree.XYZPoint>)kd_tree_trunks.nearestNeighbourSearch(1, nearest.get(0));

            //int indexi = findMatchIndex(kd_tree_trunks, stats.kd_tree2, p, diam * 100.0 * 2.0, stats.treeBank);

            int indexi = nearest.get(0).getIndex();

            double distance = p.euclideanDistance(nearest.get(0));

            //if(p.getIndex() == nearest2.get(0).getIndex()){
            if(indexi >= 0 && distance < 2.0 ){


                //int index = nearest.get(0).getIndex();
                int index = indexi;

                trunkMatches.put(p.getIndex(), new int[]{(int)stats.treeBank[index][7], (int)stats.treeBank[index][8], (int)(diam * 100.0 * 2.0 * 1000)});

                System.out.println("FOUND A GOOD MATCH WITH FIELD DATA!! " + diam + " " + stats.treeBank[index][2]);
                mean_measured += stats.treeBank[index][2];
                mean_predicted += (diam * 100.0 * 2.0);
                counter++;
                bw2.write(p.getIndex() + "\t" + (diam * 100.0 * 2.0) + "\t" + stats.treeBank[index][3] + "\t" + stats.treeBank[index][7] + "\t" + stats.treeBank[index][8] + "\t" + scanAngle + "\n");
                predicted.add((diam * 100.0 * 2.0));
                observed.add(stats.treeBank[index][3]);

                trunkMatchId.put((short)p.getIndex(),(short)stats.treeBank[index][8]);

            }else{
                System.out.println("NO MATCH IN FIELD DATA!");
                bw2.write(p.getIndex() + "\t" + (diam * 100.0 * 2.0) + "\t" + (-1) + "\t" + plot_id + "\t" + (-1) + "\t" + scanAngle + "\n");
            }

            mean_predicted_all += diam * 100.0 * 2.0;
            mean_predicted_all_counter++;

        }

        bw.close();
        bw2.close();

        mean_measured /= counter;
        mean_predicted /= counter;
        mean_predicted_all /= mean_predicted_all_counter;

        System.out.println("mean_measured: " + mean_measured + "\nMean_predicted: " + mean_predicted + "\nMean_predicter_all: " + mean_predicted_all);
        System.out.println("Linked trees: " + counter);
        System.out.println("RMSE(%): " + relRMSE(predicted, observed));

        for(int i = 0; i < this.pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(this.pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                this.pointCloud.readFromBuffer(tempPoint);
                tempPoint.gpsTime = 0.0d;
                tempPoint.intensity = 0;

                if(tempPoint.classification == 4){

                    if(stemId_diameter.containsKey(tempPoint.pointSourceId)){

                        int globalId = localIdToGlobalId.get((int)tempPoint.pointSourceId);

                        if(trunkMatches.containsKey(globalId)) {
                            tempPoint.gpsTime = stemId_diameter.get(tempPoint.pointSourceId) * 2.0;
                            tempPoint.intensity = globalId;
                            tempPoint.pointSourceId = 0;
                        }else{
                            tempPoint.pointSourceId = 0;
                            tempPoint.intensity = 0;
                            tempPoint.gpsTime = 0;
                            tempPoint.classification = 0;
                        }

                    }else{
                        tempPoint.pointSourceId = 0;
                        tempPoint.intensity = 0;
                        tempPoint.gpsTime = 0;
                        tempPoint.classification = 0;
                    }
                }else{
                    tempPoint.pointSourceId = 0;
                    tempPoint.intensity = 0;
                    tempPoint.gpsTime = 0;
                    if(tempPoint.classification != 2)
                        tempPoint.classification = 0;
                }

                try {
                    aR.pfac.writePoint(tempPoint, i + j, thread_n);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        aR.pfac.closeThread(thread_n);
    }

    public double relRMSE(ArrayList<Double> pred, ArrayList<Double> obs){

        double mean = 0;

        for(int i = 0; i < obs.size(); i++){
            mean += obs.get(i);
        }
        mean /= (double)obs.size();

        double mean2 = 0;

        for(int i = 0; i < obs.size(); i++){

            mean2 += ((obs.get(i) - pred.get(i)) * (obs.get(i) - pred.get(i)));

        }

        mean2 /= (double)obs.size();

        mean2 = 100 * Math.sqrt(mean2);

        mean2 /= (double)mean;

        return mean2;
    }

    public int findMatchIndex(KdTree tree_estimated, KdTree tree_measured, KdTree.XYZPoint p, double estimatedDiameter, double[][] treeBank){

        List<KdTree.XYZPoint> nearest_measured;
        List<KdTree.XYZPoint> nearest_estimated;

        /* We take 5 neighbors to account for incorrect match in the closest one */
        nearest_measured = (List<KdTree.XYZPoint>)tree_measured.nearestNeighbourSearch(5, p);

        //ArrayList<Double> dbh_differences = new ArrayList<>(5);
        double min_dbh_difference = Double.POSITIVE_INFINITY;
        int min_dbh_difference_index = -1;

        for(int i = 0; i < nearest_measured.size(); i++){

            nearest_estimated = (List<KdTree.XYZPoint>)tree_estimated.nearestNeighbourSearch(1, nearest_measured.get(i));
            //System.out.println(nearest_estimated.get(0).getIndex() + " " + p.getIndex() + " " + nearest_measured.get(i).getIndex());
            double distance = p.euclideanDistance(nearest_measured.get(i));

            if(p.getIndex() == nearest_estimated.get(0).getIndex() && distance < 5.0){

                double dbh_dif = Math.abs(treeBank[nearest_measured.get(i).getIndex()][2] - estimatedDiameter);

                //System.out.println("dbh_dif: " + dbh_dif);

                if(dbh_dif < min_dbh_difference){
                    min_dbh_difference = dbh_dif;
                    min_dbh_difference_index = nearest_measured.get(i).getIndex();
                }
            }
        }

        return min_dbh_difference_index;
    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }

    public double dbh_straight(ArrayList<double[]> ransacPoints, boolean[] inliers, ArrayList<ArrayList<double[]>> ransacSlicePoints, double[] scanAngle){

        double output = -1.0;

        double prev = 0.0;
        boolean prevInlier = false;
        double current = 0.0;

        double diamSum = 0.0;
        double counter = 0.0;
        Statistics stats = new Statistics();

        ArrayList<Double> diams = new ArrayList<>();

        SimpleRegression reg = new SimpleRegression(true);
        MedianOfIntegerStream moi = new MedianOfIntegerStream();

        for(int i = 0; i < ransacPoints.size(); i++){

            current = ransacPoints.get(i)[2];
/*
            if(current >= 1.0 && current <= 2.0){

                reg.addData(current, ransacPoints.get(i)[3]);
                counter++;
            }
*/

            //current = ransacPoints.get(i)[2];
/*
            if(current > 1.3){

                if(inliers[i] && prevInlier){

                    if(prev == 0.0) {
                        return -1.0;
                    }

                    double meanScanAngle = 0;
                    double scanAngleCounter = 0;

                    for(int x = 0; x < ransacSlicePoints.get(i).size(); x++){
                        meanScanAngle += ransacSlicePoints.get(i).get(x)[3];
                        scanAngleCounter++;
                    }
                    for(int x = 0; x < ransacSlicePoints.get(i-1).size(); x++){
                        meanScanAngle += ransacSlicePoints.get(i-1).get(x)[3];
                        scanAngleCounter++;
                    }
                    scanAngle[0] = (meanScanAngle / scanAngleCounter);
                    return (ransacPoints.get(i)[3] + ransacPoints.get(i-1)[3]) / 2.0;
                }

            }
*/
            if(current >= 1.1 && current <= 1.5 && inliers[i]){
                moi.add(ransacPoints.get(i)[3]);
                diamSum += ransacPoints.get(i)[3];
                counter++;
            }

            prev = current;
            prevInlier = inliers[i];

            if(current >= 1.5) {
                //return diamSum / counter;
                if(moi.hasData)
                    return moi.getMedian();
                    //return diamSum / counter;
                break;
            }

        }
/*
        if(counter == 0.0)
            return -1.0;
        else{
            return reg.predict(1.3);
        }
*/
        return -1;
    }

    public List<int[]> generate(int n, int r) {
        List<int[]> combinations = new ArrayList<>();
        helper(combinations, new int[r], 0, n-1, 0);
        return combinations;
    }

    private void helper(List<int[]> combinations, int[] data, int start, int end, int index) {
        if (index == data.length) {
            int[] combination = data.clone();
            combinations.add(combination);
        } else if (start <= end) {
            data[index] = start;
            helper(combinations, data, start + 1, end, index + 1);
            helper(combinations, data, start + 1, end, index);
        }
    }

    public boolean[] outliers(ArrayList<double[]> in, int k){

        boolean[] output = new boolean[in.size()];

        k = Math.min(k, in.size());

        int[] k_closest = new int[k];

        int current_k = 0;

        double[] x_s = new double[in.size()];

        for(int i = 0; i < in.size(); i++){
            x_s[i] = in.get(i)[0];
        }
        Statistics stat = new Statistics();

        double[] closestValues = new double[k];

        for(int i = 0; i < in.size(); i++){

            //System.out.println(Arrays.toString(x_s) + " " + in.get(i)[0] + " " + k);
            int[] closest = printKclosest(x_s, in.get(i)[0], k, x_s.length);



            for(int i_ = 0; i_ < closest.length; i_++){

                closestValues[i_] = in.get(closest[i_])[1];

            }

            stat.setData(closestValues);

            double mean = stat.getMean();
            double std = stat.getStdDev();

            double threshold = 2.0;

            if(Math.abs(in.get(i)[1] - mean) / std >= threshold)
                output[i] = true;
        }
        //System.out.println(Arrays.toString(output));
        //System.exit(1);
        return output;
    }

    int findCrossOver(double[] arr, int low, int high, double x)
    {
        // Base cases
        if (arr[high] <= x) // x is greater than all
            return high;
        if (arr[low] > x)  // x is smaller than all
            return low;

        // Find the middle point
        int mid = (low + high)/2;  /* low + (high - low)/2 */

        /* If x is same as middle element, then return mid */
        if (arr[mid] <= x && arr[mid+1] > x)
            return mid;

        /* If x is greater than arr[mid], then either arr[mid + 1]
          is ceiling of x or ceiling lies in arr[mid+1...high] */
        if(arr[mid] < x)
            return findCrossOver(arr, mid+1, high, x);

        return findCrossOver(arr, low, mid - 1, x);
    }

    // This function prints k closest elements to x in arr[].
    // n is the number of elements in arr[]
    int[] printKclosest(double[] arr, double x, int k, int n)
    {
        int[] output = new int[k];
        // Find the crossover point
        int l = findCrossOver(arr, 0, n-1, x);
        int r = l+1;   // Right index to search
        int count = 0; // To keep track of count of elements
        // already printed

        // If x is present in arr[], then reduce left index
        // Assumption: all elements in arr[] are distinct
        if (arr[l] == x) l--;

        // Compare elements on left and right of crossover
        // point to find the k closest elements
        int counter = 0;

        while (l >= 0 && r < n && count < k)
        {
            if (x - arr[l] < arr[r] - x) {
                output[counter++] = l--;
                //System.out.print(arr[l--] + " ");
            }
            else {
                output[counter++] = r++;
                //System.out.print(arr[r++] + " ");
            }
            count++;
        }

        // If there are no more elements on right side, then
        // print left elements
        while (count < k && l >= 0)
        {
            output[counter++] = l--;
            //System.out.print(arr[l--]+" ");
            count++;
        }


        // If there are no more elements on left side, then
        // print right elements
        while (count < k && r < n)
        {
            output[counter++] = r++;
            //System.out.print(arr[r++]+" ");
            count++;
        }
        return output;

    }

    public void readFieldPlots(File plots) throws IOException{

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        DataSource ds = ogr.Open(plots.getAbsolutePath());
        System.out.println("Layer count: " + ds.GetLayerCount());
        Layer layeri = ds.GetLayer(0);
        System.out.println("Feature count: " + layeri.GetFeatureCount());



        File fout = new File("tempWKT.csv");

        if(fout.exists())
            fout.delete();

        fout.createNewFile();

        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        bw.write("WKT,plot_id");
        bw.newLine();

        //System.out.println("Feature count: " + layeri.GetFeatureCount());

        for(long i = 0; i < layeri.GetFeatureCount(); i++ ){

            Feature tempF = layeri.GetFeature(i);
            Geometry tempG = tempF.GetGeometryRef();
            String id = "";

            if(tempF.GetFieldCount() > 0)
                id = tempF.GetFieldAsString(0);
            else
                id = String.valueOf(i);

            //System.out.println(tempG.ExportToWkt());

            String out = "\"" + tempG.ExportToWkt() + "\"," + id;

            //System.out.println();
            bw.write(out);
            bw.newLine();


        }

        bw.close();


        ArrayList<double[][]> polyBank1 = new ArrayList<double[][]>();
        ArrayList<Double> plotID1 = new ArrayList<Double>();
        //String tiedosto_coord = "plotsTEST30.csv";
        //String tiedosto_coord = "plotsTEST15.csv";
        String tiedosto_coord = "tempWKT.csv";
        String line1 = "";

        File tiedostoCoord = new File(tiedosto_coord);
        tiedostoCoord.setReadable(true);

        BufferedReader sc = new BufferedReader( new FileReader(tiedostoCoord));
        sc.readLine();
        while((line1 = sc.readLine())!= null){

            //System.out.println(line1);

            String[] tokens =  line1.split(",");

            if(Double.parseDouble(tokens[tokens.length - 1]) != -999){

                plotID1.add(Double.parseDouble(tokens[tokens.length - 1]));

                double[][] tempPoly = new double[tokens.length - 1][2];
                int n = (tokens.length) - 2;
                int counteri = 0;

                tempPoly[counteri][0] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[1]);

                counteri++;

                boolean breikki = false;

                for(int i = 1; i < n; i++){
                    //System.out.println(tokens[i]);

                    if(tokens[i].split(" ")[0].contains(")") || tokens[i].split(" ")[1].contains(")")){
                        plotID1.remove(plotID1.size() - 1);
                        breikki = true;
                        break;
                    }

                    tempPoly[counteri][0] = Double.parseDouble(tokens[i].split(" ")[0]);
                    tempPoly[counteri][1] = Double.parseDouble(tokens[i].split(" ")[1]);
                    //System.out.println(Arrays.toString(tempPoly[counteri]));
                    counteri++;
                }


                tempPoly[counteri][0] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[1]);

                if(!breikki)
                    polyBank1.add(tempPoly);
            }

        }

        this.setPolygon(polyBank1, plotID1);


    }

    public void setPolygon(ArrayList<double[][]> in, ArrayList<Double> plotIds1){

        this.polygons = in;
        this.plotIds = plotIds1;

        for(int i = 0; i < plotIds.size(); i++){

            plotIdsString.add(String.valueOf(plotIds.get(i)));

        }

    }

    public boolean pointInPolygons(double[] point, double[] plotId) {

        double[][] poly;

        for(int f = 0; f < polygons.size(); f++){

            poly = polygons.get(f);

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

            if(isInside){
                plotId[0] = this.plotIds.get(f);
                return true;
            }
        }
        return false;
    }

}
