package tools;
import LASio.*;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.tinfour.utils.Polyside;
import utils.*;

import static org.tinfour.utils.Polyside.isPointInPolygon;
import static tools.cellStats.polygonArea;

public class lasGridStats {

    LASReader pointCloud;
    argumentReader aR;
    double orig_y, orig_x;
    double resolution = 0;
    double z_cutoff = 1.3;

    gridRAF bin_a;
    gridRAF bin_f;
    gridRAF bin_l;
    gridRAF bin_i;

    File outputMetricFile_a = null;
    File outputMetricFile_f = null;
    File outputMetricFile_l = null;
    File outputMetricFile_i = null;

    FileWriter writer_a = null;
    FileWriter writer_f = null;
    FileWriter writer_l = null;
    FileWriter writer_i = null;

    HashMap<Integer, Double> finalMerges_areas = new HashMap<>();

    ArrayList<String> colnames_metrics_a = new ArrayList<>();
    ArrayList<String> colnames_metrics_f = new ArrayList<>();
    ArrayList<String> colnames_metrics_l = new ArrayList<>();
    ArrayList<String> colnames_metrics_i = new ArrayList<>();
    HashMap<Integer, Double> neighborhood_areas = new HashMap<>();
    int grid_x_size = 0;
    int grid_y_size = 0;
    int coreNumber = 0;

    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();

    public lasGridStats(LASReader pointCloud, argumentReader aR, int coreNumber) throws Exception{


        this.coreNumber = coreNumber;

        this.resolution = aR.res;

        this.z_cutoff = aR.z_cutoff;

        this.pointCloud = pointCloud;

        if(!pointCloud.isIndexed){
            pointCloud.index((int)aR.res);
        }

        this.aR = aR;

        if(aR.orig_x == -1 || aR.orig_y == -1){
            this.orig_x = pointCloud.getMinX();
            this.orig_y = pointCloud.getMaxY();

        }else{
            this.orig_x = aR.orig_x;
            this.orig_y = aR.orig_y;

        }

        //this.resolution = aR.step;

        this.outputMetricFile_a = this.aR.createOutputFileWithExtension(pointCloud, "_gridStats_all_echoes.txt");
        this.outputMetricFile_l = this.aR.createOutputFileWithExtension(pointCloud, "_gridStats_last_and_only_echoes.txt");
        this.outputMetricFile_f = this.aR.createOutputFileWithExtension(pointCloud, "_gridStats_first_and_only_echoes.txt");
        this.outputMetricFile_i = this.aR.createOutputFileWithExtension(pointCloud, "_gridStats_intermediate_echoes.txt");
        bin_a = new gridRAF(this.aR.createOutputFileWithExtension(pointCloud, "_temp_a.bin"));
        bin_f = new gridRAF(this.aR.createOutputFileWithExtension(pointCloud, "_temp_f.bin"));
        bin_l = new gridRAF(this.aR.createOutputFileWithExtension(pointCloud, "_temp_l.bin"));
        bin_i = new gridRAF(this.aR.createOutputFileWithExtension(pointCloud, "_temp_i.bin"));

        this.start();

        aR.p_update.fileProgress++;

    }

    public void start() throws Exception{


        long tStart = System.currentTimeMillis();


        aR.output_statistics = true;

        this.grid_x_size = (int)Math.ceil((pointCloud.maxX - orig_x) / resolution);
        this.grid_y_size = (int)Math.ceil((orig_y - pointCloud.minY) / resolution);

        float[][] stats = new float[grid_x_size][grid_y_size];

        LasPoint tempPoint = new LasPoint();

        int pienin, suurin;

        ArrayList<ArrayList<Double>> gridPoints_z_a = new ArrayList<>();
        ArrayList<ArrayList<Integer>> gridPoints_i_a = new ArrayList<>();

        ArrayList<ArrayList<Double>> gridPoints_z_f = new ArrayList<>();
        ArrayList<ArrayList<Integer>> gridPoints_i_f = new ArrayList<>();

        ArrayList<ArrayList<Double>> gridPoints_z_l = new ArrayList<>();
        ArrayList<ArrayList<Integer>> gridPoints_i_l = new ArrayList<>();

        ArrayList<ArrayList<Double>> gridPoints_z_i = new ArrayList<>();
        ArrayList<ArrayList<Integer>> gridPoints_i_i = new ArrayList<>();

        ArrayList<ArrayList<int[]>> gridPoints_RGB_f = new ArrayList<>();

/*
        PriorityQueue<Double> que_gridPoints_z_a = new PriorityQueue<>();
        PriorityQueue<Integer> que_gridPoints_i_a = new PriorityQueue<>();

        PriorityQueue<Double> que_gridPoints_z_f = new PriorityQueue<>();
        PriorityQueue<Integer> que_gridPoints_i_f = new PriorityQueue<>();

        PriorityQueue<Double> que_gridPoints_z_l = new PriorityQueue<>();
        PriorityQueue<Integer> que_gridPoints_i_l = new PriorityQueue<>();

        PriorityQueue<Double> que_gridPoints_z_i = new PriorityQueue<>();
        PriorityQueue<Integer> que_gridPoints_i_i = new PriorityQueue<>();

*/
        ArrayList<Double> sum_z_a = new ArrayList<>();
        ArrayList<Double> sum_i_a = new ArrayList<>();
        ArrayList<Double> sum_z_f = new ArrayList<>();
        ArrayList<Double> sum_i_f = new ArrayList<>();
        ArrayList<Double> sum_z_l = new ArrayList<>();
        ArrayList<Double> sum_i_l = new ArrayList<>();
        ArrayList<Double> sum_z_i = new ArrayList<>();
        ArrayList<Double> sum_i_i = new ArrayList<>();



        ArrayList<ArrayList<Point>> points = new ArrayList<>();




        int raf_location_a = 0;
        int raf_location_f = 0;
        int raf_location_l = 0;
        int raf_location_i = 0;

        ArrayList<Integer> ids = new ArrayList<>();

        ArrayList<IncrementalTin> tins = new ArrayList<>();
        ArrayList<List<IQuadEdge>> tin_perimeters = new ArrayList<>();


        int doubles_per_cell = 0;
        int doubles_per_cell_rgb = 0;

        pointCloudMetrics pCM = new pointCloudMetrics(this.aR);

        ArrayList<String> colnames = new ArrayList<>();
        Vertex tempV;

        int gridCounter = 0;

        Set<Integer> foundStands = new HashSet<>();

        ArrayList<Double> metrics = new ArrayList<>();

        HashMap<Integer, Integer> order = new HashMap<>();

        ArrayList<Integer> do_overs = new ArrayList<>();

        ArrayList<ArrayList<ArrayList<Integer>>> gridLocationInRaf_a = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> gridLocationInRaf_f = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> gridLocationInRaf_l = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> gridLocationInRaf_i = new ArrayList<>();

        for(int x = 0; x < grid_x_size; x++){
            gridLocationInRaf_a.add(new ArrayList<>());
            gridLocationInRaf_f.add(new ArrayList<>());
            gridLocationInRaf_l.add(new ArrayList<>());
            gridLocationInRaf_i.add(new ArrayList<>());
            for(int y = 0; y < grid_y_size; y++) {

                gridLocationInRaf_a.get(x).add(new ArrayList<>());
                gridLocationInRaf_f.get(x).add(new ArrayList<>());
                gridLocationInRaf_l.get(x).add(new ArrayList<>());
                gridLocationInRaf_i.get(x).add(new ArrayList<>());
            }
        }

        boolean stands_delineated = true;
        /* Define the variables that we need */
        int polygon_id = -1;
        try {
            polygon_id = pointCloud.extraBytes_names.get("polygon_id");
        }catch (Exception e){

            System.out.println("The file is not stand delineated.");
            stands_delineated = false;
        }

        int[][] number_of_points_per_cell = new int[grid_x_size][grid_y_size];

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i++) {

            pointCloud.readRecord(i, tempPoint);
            int x = Math.min((int)((tempPoint.x - pointCloud.minX) / resolution), grid_x_size-1);
            int y = Math.min((int)((pointCloud.maxY - tempPoint.y) / resolution), grid_y_size-1);

            //long c = (long)x << 32 | y & 0xFFFFFFFFL;

            number_of_points_per_cell[x][y]++;

            //int aBack = (int)(c >> 32);
            //int bBack = (int)c;


        }

        int counter = 0;
        ArrayList<LasPoint>[][] grid_of_points = (ArrayList<LasPoint>[][]) new ArrayList[grid_x_size][grid_y_size] ;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i++) {

            pointCloud.readRecord(i, tempPoint);

            int x = Math.min((int)((tempPoint.x - pointCloud.minX) / resolution), grid_x_size-1);
            int y = Math.min((int)((pointCloud.maxY - tempPoint.y) / resolution), grid_y_size-1);

            //long c = (long)x << 32 | y & 0xFFFFFFFFL;

            number_of_points_per_cell[x][y]--;

            if(grid_of_points[x][y] == null){
                grid_of_points[x][y] = new ArrayList<>();
                grid_of_points[x][y].add(new LasPoint(tempPoint));
            }else
                grid_of_points[x][y].add(new LasPoint(tempPoint));

            if(number_of_points_per_cell[x][y] == 0){
                System.out.printf("%.3fGiB", Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0 * 1024.0));

                System.out.println("process: " + counter++);
                grid_of_points[x][y].clear();
                grid_of_points[x][y] = null;

                if(counter % 1000 == 0){
                    System.gc();
                }

            }

        }



        int[][] cell_only_id = new int[grid_x_size][grid_y_size];

        for(int x = 0; x < grid_x_size; x++){
            for(int y = 0; y < grid_y_size; y++) {


                //System.out.println( (grid_y_size*x+y) + " / " + (grid_y_size*grid_x_size));
                cell_only_id[x][y] = -1;

                gridPoints_z_a.clear();
                gridPoints_i_a.clear();

                gridPoints_z_f.clear();
                gridPoints_i_f.clear();

                gridPoints_RGB_f.clear();

                points.clear();

                gridPoints_z_l.clear();
                gridPoints_i_l.clear();

                gridPoints_z_i.clear();
                gridPoints_i_i.clear();

                ids.clear();

                sum_z_a.clear(); sum_z_f.clear(); sum_z_l.clear(); sum_z_i.clear();
                sum_i_a.clear(); sum_i_f.clear(); sum_i_l.clear(); sum_i_i.clear();

                pointCloud.queryPoly2(orig_x + resolution * x, orig_x + resolution * x + resolution, orig_y - resolution * y - resolution, orig_y - resolution * y);

                gridLocationInRaf_a.get(x).get(y).clear();
                gridLocationInRaf_f.get(x).get(y).clear();
                gridLocationInRaf_l.get(x).get(y).clear();
                gridLocationInRaf_i.get(x).get(y).clear();

                foundStands.clear();

                if (pointCloud.queriedIndexes2.size() > 0) {

                    /*
                    for (int u = 0; u < pointCloud.queriedIndexes2.size(); u++) {

                        //System.out.println(Arrays.toString(pointCloud.queriedIndexes2.get(u)));
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

                            pointCloud.readRecord_noRAF(pienin, tempPoint, ero);

                            for (int p = pienin; p <= suurin; p++) {
*/
                    int p = 0;

                    long startTime = System.currentTimeMillis();

                    while(!pointCloud.index_read_terminated){

                        p = pointCloud.fastReadFromQuery(tempPoint);
                                //pointCloud.readFromBuffer(tempPoint);
/*
                                if (tempPoint.z <= z_cutoff)
                                    continue;
*/
                        //if(false)
                                if (tempPoint.x >= (orig_x + resolution * x) && tempPoint.x < (orig_x + resolution * x + resolution) &&
                                        tempPoint.y <= (orig_y - resolution * y) && tempPoint.y > (orig_y - resolution * y - resolution)) {



                                    //if(!foundStands.contains(tempPoint.pointSourceId)) {
                                    int stand_id = 1;
                                    if(stands_delineated){
                                        stand_id = tempPoint.getExtraByteInt(polygon_id);
                                    }else{
                                    }
                                    //if(!foundStands.contains(tempPoint.pointSourceId)) {
                                    if(!foundStands.contains(stand_id)) {

                                        //tins.add(new IncrementalTin());
                                       // tin_perimeters.add(null);

                                        ids.add(stand_id);

                                        points.add(new ArrayList<>());

                                        gridPoints_z_a.add(new ArrayList<>());
                                        gridPoints_i_a.add(new ArrayList<>());

                                        gridPoints_z_f.add(new ArrayList<>());
                                        gridPoints_RGB_f.add(new ArrayList<>());

                                        gridPoints_i_f.add(new ArrayList<>());

                                        gridPoints_z_l.add(new ArrayList<>());
                                        gridPoints_i_l.add(new ArrayList<>());

                                        gridPoints_z_i.add(new ArrayList<>());
                                        gridPoints_i_i.add(new ArrayList<>());

                                        sum_z_a.add(0.0);
                                        sum_i_a.add(0.0);

                                        sum_z_f.add(0.0);
                                        sum_i_f.add(0.0);

                                        sum_z_l.add(0.0);
                                        sum_i_l.add(0.0);

                                        sum_z_i.add(0.0);
                                        sum_i_i.add(0.0);

                                        order.put(stand_id, gridPoints_z_a.size()-1);
                                        foundStands.add(stand_id);

                                    }

                                    /* In order to save memory we only add point to tin if it is outside of it. After all,
                                      we only need the bounday of the tin.
                                     */
                                    //if(!tins.get(order.get(tempPoint.pointSourceId)).isPointInsideTin(tempPoint.x, tempPoint.y)){
                                    /*
                                    if(tins.get(order.get(tempPoint.pointSourceId)).isBootstrapped()) {

                                        if(tin_perimeters.get(order.get(tempPoint.pointSourceId)) == null){
                                            tin_perimeters.set(order.get(tempPoint.pointSourceId), tins.get(order.get(tempPoint.pointSourceId)).getPerimeter());
                                        }

                                        if (isPointInPolygon(tin_perimeters.get(order.get(tempPoint.pointSourceId)), tempPoint.x, tempPoint.y) == Polyside.Result.Outside) {
                                            //System.out.println(tin_perimeters.get(order.get(tempPoint.pointSourceId)).size());
                                            tempV = new Vertex(tempPoint.x, tempPoint.y, 0.0);
                                            //perim.add(i);
                                            tempV.setIndex(p);
                                            tins.get(order.get(tempPoint.pointSourceId)).add(tempV);
                                            tin_perimeters.set(order.get(tempPoint.pointSourceId), tins.get(order.get(tempPoint.pointSourceId)).getPerimeter());

                                        }
                                    }else{
                                        tempV = new Vertex(tempPoint.x, tempPoint.y, 0.0);
                                        //perim.add(i);
                                        tempV.setIndex(p);
                                        tins.get(order.get(tempPoint.pointSourceId)).add(tempV);

                                    }

                                     */
/*
                                    que_gridPoints_z_a.add(tempPoint.z);
                                    que_gridPoints_i_a.add(tempPoint.intensity);
*/
                                    gridPoints_z_a.get(order.get(stand_id)).add(tempPoint.z);
                                    gridPoints_i_a.get(order.get(stand_id)).add(tempPoint.intensity);

                                    sum_z_a.set(order.get(stand_id), sum_z_a.get(order.get(stand_id)) + tempPoint.z);
                                    sum_i_a.set(order.get(stand_id), sum_i_a.get(order.get(stand_id)) + tempPoint.intensity);

                                    points.get(order.get(stand_id)).add(new Point(tempPoint.x, tempPoint.y));

                                    //System.out.println("intensity" + tempPoint.intensity);

                                    if (tempPoint.returnNumber == 1) {
                                        /*
                                        sum_z_f += tempPoint.z;
                                        sum_i_f += tempPoint.intensity;

                                        gridPoints_z_f.add(tempPoint.z);
                                        gridPoints_i_f.add(tempPoint.intensity);

                                         */
                                        gridPoints_z_f.get(order.get(stand_id)).add(tempPoint.z);

                                        gridPoints_RGB_f.get(order.get(stand_id)).add(new int[]{tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});

                                        gridPoints_i_f.get(order.get(stand_id)).add(tempPoint.intensity);

                                        sum_z_f.set(order.get(stand_id), sum_z_f.get(order.get(stand_id)) + tempPoint.z);
                                        sum_i_f.set(order.get(stand_id), sum_i_f.get(order.get(stand_id)) + tempPoint.intensity);

                                    }
                                    if (tempPoint.returnNumber == tempPoint.numberOfReturns) {
/*
                                        sum_z_l += tempPoint.z;
                                        sum_i_l += tempPoint.intensity;

                                        gridPoints_z_l.add(tempPoint.z);
                                        gridPoints_i_l.add(tempPoint.intensity);

 */
                                        gridPoints_z_l.get(order.get(stand_id)).add(tempPoint.z);
                                        gridPoints_i_l.get(order.get(stand_id)).add(tempPoint.intensity);

                                        sum_z_l.set(order.get(stand_id), sum_z_l.get(order.get(stand_id)) + tempPoint.z);
                                        sum_i_l.set(order.get(stand_id), sum_i_l.get(order.get(stand_id)) + tempPoint.intensity);

                                    }
                                    if (tempPoint.returnNumber > 1 && tempPoint.returnNumber != tempPoint.numberOfReturns) {
/*
                                        sum_z_i += tempPoint.z;
                                        sum_i_i += tempPoint.intensity;
                                        gridPoints_z_i.add(tempPoint.z);
                                        gridPoints_i_i.add(tempPoint.intensity);
 */
                                        gridPoints_z_i.get(order.get(stand_id)).add(tempPoint.z);
                                        gridPoints_i_i.get(order.get(stand_id)).add(tempPoint.intensity);

                                        sum_z_i.set(order.get(stand_id), sum_z_i.get(order.get(stand_id)) + tempPoint.z);
                                        sum_i_i.set(order.get(stand_id), sum_i_i.get(order.get(stand_id)) + tempPoint.intensity);

                                    }
                                }
                            }
                      // }
                    //}
                    long stopTime = System.currentTimeMillis();
                    System.out.println(stopTime - startTime);
                }else{
                    //System.out.println("WHAT THE FUCK!!");
                }

                /* No points in this rectangle */
                if(ids.size() == 0)
                    continue;

                ArrayList<Double> areas = new ArrayList<>();

                int divided = foundStands.size() > 1 ? 1 : 0;

                doubles_per_cell = 0;

                if(divided == 1 ){

                    do_overs.add( x * grid_y_size + y );

                }else{

                    cell_only_id[x][y] = ids.get(0);

                }

                double grid_cell_id = y * grid_x_size + x;
/*
                for(int ii = 0; ii < gridPoints_z_a.size(); ii++) {

                    areas.add(tins.get(ii).countTriangles().getAreaSum());
                    tins.get(ii).clear();
                    tins.get(ii).dispose();
                    tins.set(ii, new IncrementalTin());
                    tin_perimeters.set(ii, null);

                }
                System.gc();

 */

                for(int ii = 0; ii < gridPoints_z_a.size(); ii++) {
                    QuickHull qh = new QuickHull();
                    ArrayList<Point> p = null;

                    if (points.get(ii).size() > 3) {
                        p = qh.quickHull(points.get(ii));
                        areas.add(polygonArea(p));
                    } else {
                        areas.add(-99.9);
                    }
                }



                double x_coord = orig_x + resolution * x;
                double y_coord = orig_y - resolution * y;

                boolean addedBecauseTooSmall = false;

                /* Iterate over all the plot ids within this grid cell */
                for(int ii = 0; ii < gridPoints_z_a.size(); ii++) {

                    /* If this plot id within this grid cell has more than 10 points */
                    if(gridPoints_z_a.get(ii).size() > aR.min_points) {

                        metrics = pCM.calc(gridPoints_z_a.get(ii), gridPoints_i_a.get(ii), sum_z_a.get(ii), sum_i_a.get(ii), "_a", colnames);

                        if(colnames_metrics_a.size() == 0)
                            colnames_metrics_a = (ArrayList<String>)colnames.clone();

                        gridLocationInRaf_a.get(x).get(y).add(raf_location_a);

                        bin_a.writeDouble(grid_cell_id);
                        bin_a.writeDouble(ids.get(ii));
                        bin_a.writeDouble(divided);
                        bin_a.writeDouble(0.0);
                        bin_a.writeDouble(areas.get(ii));
                        bin_a.writeDouble(x_coord);
                        bin_a.writeDouble(y_coord);

                        if(divided == 0 && areas.get(ii) <= (resolution * resolution * 0.66) && !addedBecauseTooSmall){
                            addedBecauseTooSmall = true;
                            do_overs.add( x * grid_y_size + y );
                        }

                        for (Double metric : metrics) {
                            bin_a.writeDouble(metric);
                        }
                        raf_location_a++;

                    }
                }

                if(doubles_per_cell == 0)
                    doubles_per_cell = 7 + metrics.size();

                long start_time = System.currentTimeMillis();

                for(int ii = 0; ii < gridPoints_z_f.size(); ii++) {

                    if(gridPoints_z_f.get(ii).size() > aR.min_points) {

                        metrics = pCM.calc_with_RGB(gridPoints_z_f.get(ii), gridPoints_i_f.get(ii), sum_z_f.get(ii), sum_i_f.get(ii), "_f", colnames, gridPoints_RGB_f.get(ii));

                        if(colnames_metrics_f.size() == 0)
                            colnames_metrics_f = (ArrayList<String>)colnames.clone();

                        bin_f.writeDouble(grid_cell_id);
                        bin_f.writeDouble(ids.get(ii));
                        bin_f.writeDouble(divided);
                        bin_f.writeDouble(0.0);
                        bin_f.writeDouble(areas.get(ii));
                        bin_f.writeDouble(x_coord);
                        bin_f.writeDouble(y_coord);

                        for (Double metric : metrics) {
                            bin_f.writeDouble(metric);
                        }

                        gridLocationInRaf_f.get(x).get(y).add(raf_location_f);
                        raf_location_f++;

                        if(doubles_per_cell_rgb == 0)
                            doubles_per_cell_rgb = 7 + metrics.size();

                    }
                }



                for(int ii = 0; ii < gridPoints_z_l.size(); ii++) {

                    if(gridPoints_z_l.get(ii).size() > aR.min_points) {

                        metrics = pCM.calc(gridPoints_z_l.get(ii), gridPoints_i_l.get(ii), sum_z_l.get(ii), sum_i_l.get(ii), "_l", colnames);

                        if(colnames_metrics_l.size() == 0)
                            colnames_metrics_l = (ArrayList<String>)colnames.clone();

                        bin_l.writeDouble(grid_cell_id);
                        bin_l.writeDouble(ids.get(ii));
                        bin_l.writeDouble(divided);
                        bin_l.writeDouble(0.0);
                        bin_l.writeDouble(areas.get(ii));
                        bin_l.writeDouble(x_coord);
                        bin_l.writeDouble(y_coord);

                        for (Double metric : metrics) {
                            bin_l.writeDouble(metric);
                        }

                        gridLocationInRaf_l.get(x).get(y).add(raf_location_l);
                        raf_location_l++;

                    }
                }

                for(int ii = 0; ii < gridPoints_z_i.size(); ii++) {

                    if(gridPoints_z_i.get(ii).size() > aR.min_points) {



                        metrics = pCM.calc(gridPoints_z_i.get(ii), gridPoints_i_i.get(ii), sum_z_i.get(ii), sum_i_i.get(ii), "_i", colnames);


                        if(colnames_metrics_i.size() == 0)
                            colnames_metrics_i = (ArrayList<String>)colnames.clone();

                        bin_i.writeDouble(grid_cell_id);
                        bin_i.writeDouble(ids.get(ii));
                        bin_i.writeDouble(divided);
                        bin_i.writeDouble(0.0);
                        bin_i.writeDouble(areas.get(ii));
                        bin_i.writeDouble(x_coord);
                        bin_i.writeDouble(y_coord);

                        for (Double metric : metrics) {
                            bin_i.writeDouble(metric);
                        }

                        gridLocationInRaf_i.get(x).get(y).add(raf_location_i);
                        raf_location_i++;

                    }
                }

                long endTime = System.currentTimeMillis();

                //System.out.println("METRICS TOOK: " + (endTime-start_time));

                if(foundStands.size() > 1) {
                    for(int i = 0; i < areas.size(); i++){

                        System.out.println("Area: " + areas.get(i) + " " + i);

                    }

                }

                foundStands.clear();
                //tin.clear();


                gridCounter++;

                if(gridCounter % 15 == 0){
                    System.gc();
                    System.gc();
                    System.gc();
                }
            }
        }
        bin_a.writeBuffer2();
        bin_a.refresh();
        bin_f.writeBuffer2();
        bin_f.refresh();
        bin_l.writeBuffer2();
        bin_l.refresh();
        bin_i.writeBuffer2();
        bin_i.refresh();


        HashMap<Integer, HashSet<Integer>> neighborhoods = new HashMap<>();
        neighborhood_areas = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> gridsMerged_one_way = new HashMap<>();

        HashSet<Integer> grid_ids_merged = new HashSet<>();



        double optimalSize = resolution * resolution;

        if(false)
        for(int i : do_overs){

            System.out.println("-----------------------------");
            System.out.println("-----------------------------");
            System.out.println("-----------------------------");

            System.out.println("DO OVER: " + i);

            int x = i / grid_y_size ;
            int y = (i - x * grid_y_size) ;

            int siz = gridLocationInRaf_a.get(x).get(y).size();

            int plot_id;
            int plot_id2;
            double grid_id2 = -1;
            double optimal_pair_grid_id = -1;
            double area;
            int divided = 0;
            int modded = 0;

            Set<Integer> currentNeghborhood = new HashSet<>();

            for(int j = 0; j < siz; j++){

                int pos = gridLocationInRaf_a.get(x).get(y).get(j);

                System.out.println("pos: " + pos);

                if(neighborhoods.containsKey(pos)) {
                    System.out.println("Pos: " + pos + " containedd");
                    continue;
                }

                bin_a.seek(pos * (doubles_per_cell * 8));

                double grid_id = bin_a.readDouble();

                System.out.println("Grid id: " + grid_id);
                plot_id = (int)bin_a.readDouble();

                divided = (int)bin_a.readDouble();
                modded = (int)bin_a.readDouble();
                area = bin_a.readDouble();

                currentNeghborhood.clear();

                /* This means that this split cell is already used in a neighborhood */
                if(neighborhoods.containsKey(pos)){

                    area = neighborhood_areas.get(pos);
                    currentNeghborhood = neighborhoods.get(pos);

                }


                System.out.println(grid_x_size + " " + grid_y_size);
                System.out.println(area + " " + x + " " + y + " " + i + " " + siz);

                int siz2 = 0;

                System.out.println("Finding optimal neighbor for " + plot_id + " " + pos);

                double areaOrig = area;
                double bestSize = Math.abs(area - optimalSize);
                double optimalAreaAddition = 0;
                int pair = -1;

                int[] x__ = new int[4];
                int[] y__ = new int[4];

                x__[0] = x; y__[0] = y + 1;
                x__[1] = x + 1; y__[1] = y;
                x__[2] = x; y__[2] = y - 1;
                x__[3] = x - 1; y__[3] = y;

                for(int f = 0; f < 4; f++){

                    int x_ = x__[f];
                    int y_ = y__[f];

                    if(x_ < 0 || x_ >= grid_x_size || y_ >= grid_y_size || y_ < 0)
                        continue;

                    siz2 = gridLocationInRaf_a.get(x_).get(y_).size();
                    //System.out.println("x_: " + x_ + " y_: " + y_ + " siz2: " + siz2);

                    for(int h = 0; h < siz2; h++){

                        int pos2 = gridLocationInRaf_a.get(x_).get(y_).get(h);

                        if(currentNeghborhood.contains(pos2))
                            continue;

                        bin_a.seek(pos2 * (doubles_per_cell * 8));
                        grid_id2 = bin_a.readDouble();
                        plot_id2 = (int)bin_a.readDouble();

                        if(plot_id2 != plot_id)
                            continue;

                        divided = (int)bin_a.readDouble();
                        modded = (int)bin_a.readDouble();

                        area = bin_a.readDouble();

                        if(neighborhood_areas.containsKey(pos2))
                            area = neighborhood_areas.get(pos2);
/*
                        if(gridsMerged_all.containsKey(pos2)){

                            area = gridsMerged_all.get(pos2).get(0);

                        }
*/
                        if(Math.abs(area + areaOrig - optimalSize) < bestSize){

                            bestSize = Math.abs(area + areaOrig - optimalSize);
                            optimalAreaAddition = area;
                            pair = pos2;
                            optimal_pair_grid_id = grid_id2;

                        }

                    }
                }

                if(pair != -1){

                    /* IF NEITHER ARE IN NEIGHBORHOODS! */
                    if(!neighborhoods.containsKey(pair) && !neighborhoods.containsKey(pos)) {

                        neighborhoods.put(pair, new HashSet<>());
                        neighborhoods.get(pair).add(pos);
                        neighborhoods.get(pair).add(pair);

                        neighborhoods.put(pos, new HashSet<>());
                        neighborhoods.get(pos).add(pair);
                        neighborhoods.get(pos).add(pos);

                        neighborhood_areas.put(pair,  optimalAreaAddition + areaOrig);
                        neighborhood_areas.put(pos,  optimalAreaAddition + areaOrig);

                        grid_ids_merged.add((int)grid_id);
                        grid_ids_merged.add((int)optimal_pair_grid_id);

                        System.out.println(" DEBUG: " + grid_id + " " + optimal_pair_grid_id);

                        System.out.println(optimalAreaAddition + " " + areaOrig);

                    }
                    /* IF BOTH ARE IN NEIGHBORHOODS! */
                    else if(neighborhoods.containsKey(pair) && neighborhoods.containsKey(pos)){

                        ArrayList<Integer> modThese_pair = new ArrayList<>(neighborhoods.get(pair));
                        ArrayList<Integer> modThese_pos = new ArrayList<>(neighborhoods.get(pos));


                        for(int i_ : modThese_pair) {

                            neighborhoods.get(i_).addAll(modThese_pos);
                            neighborhood_areas.put(i_,  optimalAreaAddition + areaOrig);
                        }
                        for(int i_ : modThese_pos) {

                            neighborhoods.get(i_).addAll(modThese_pair);
                            neighborhood_areas.put(i_,  optimalAreaAddition + areaOrig);

                        }
                    }else if(!neighborhoods.containsKey(pair) && neighborhoods.containsKey(pos)){

                        grid_ids_merged.add((int)optimal_pair_grid_id);

                        ArrayList<Integer> modThese = new ArrayList<>(neighborhoods.get(pos));
                        neighborhoods.put(pair, new HashSet<>());


                        for(int i_ : modThese) {

                            neighborhoods.get(i_).add(pair);
                            neighborhood_areas.put(i_,  optimalAreaAddition + areaOrig);

                        }

                        neighborhoods.get(pair).addAll(modThese);
                        neighborhood_areas.put(pair,  optimalAreaAddition + areaOrig);

                    }
                    else if(neighborhoods.containsKey(pair) && !neighborhoods.containsKey(pos)){

                        grid_ids_merged.add((int)grid_id);

                        ArrayList<Integer> modThese = new ArrayList<>(neighborhoods.get(pair));
                        neighborhoods.put(pos, new HashSet<>());

                        for(int i_ : modThese) {

                            neighborhoods.get(i_).add(pos);
                            neighborhood_areas.put(i_,  optimalAreaAddition + areaOrig);

                        }

                        neighborhoods.get(pos).addAll(modThese);
                        neighborhood_areas.put(pos,  optimalAreaAddition + areaOrig);

                    }
                    System.out.println("bestSizeIndex: " + pair + " bestSize: " + bestSize + " neigh size: " + neighborhood_areas.get(pos) + " n_neigh: " + neighborhoods.get(pos).size());
                }else{
                    System.out.println("FAILED TO FIND NEIGHBOR FOR : " + pos);
                    System.out.println(grid_ids_merged.size());


                }
            }
/*
            for(int i_ : grid_ids_merged){
                System.out.println(i_);
            }
            System.exit(1);
            */

        }



        ArrayList<HashSet<Integer>> finalMerges = new ArrayList<>();
        finalMerges_areas = new HashMap<Integer, Double>();

        HashSet<Integer> all = new HashSet<>();
        HashSet<Integer> all2 = new HashSet<>();



        for(int i_ : neighborhoods.keySet()){

            if(finalMerges.size() == 0){

                finalMerges.add(neighborhoods.get(i_));
                all.addAll(neighborhoods.get(i_));
                finalMerges_areas.put(i_, neighborhood_areas.get(i_));
                continue;

            }

            if(!all.contains(i_)){

                finalMerges.add(neighborhoods.get(i_));
                finalMerges_areas.put(i_, neighborhood_areas.get(i_));
                all.addAll(neighborhoods.get(i_));

            }

        }

        for(int i_ = 0; i_ < finalMerges.size(); i_++){

            System.out.println(Arrays.toString(finalMerges.get(i_).toArray()) + " " + finalMerges_areas.get(i_));

        }
/*
        System.out.println();
        for(int i : grid_ids_merged){
            System.out.print(i + "\t");
        }
        System.exit(1);
*/

        //FileWriter writer = null;
        try {
            writer_a = new FileWriter(this.outputMetricFile_a);
            writer_f = new FileWriter(this.outputMetricFile_f);
            writer_l = new FileWriter(this.outputMetricFile_l);
            writer_i = new FileWriter(this.outputMetricFile_i);

        }catch (Exception e){
            e.printStackTrace();
        }
        /*
                                   bin_i.writeDouble(grid_cell_id);
                                   bin_i.writeDouble(ids.get(ii));
                                   bin_i.writeDouble(divided);
                                   bin_i.writeDouble(0.0);
                                   bin_i.writeDouble(areas.get(ii));
         */
        writer_a.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");
        writer_f.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");
        writer_l.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");
        writer_i.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");

        for(int i = 0; i < colnames_metrics_a.size(); i++){

            writer_a.write(colnames_metrics_a.get(i) + "\t");

            writer_l.write(colnames_metrics_l.get(i) + "\t");
            writer_i.write(colnames_metrics_i.get(i) + "\t");

        }

        /* First and only contain RGB, so we need to do it separately */
        for(int i = 0; i < colnames_metrics_f.size(); i++) {
            writer_f.write(colnames_metrics_f.get(i) + "\t");

        }


        writer_a.write("\n");
        writer_f.write("\n");
        writer_l.write("\n");
        writer_i.write("\n");

        /* This is for if we want to do the merging thingy */
        if(false)
        for(int x = 0; x < grid_x_size; x++) {
            for (int y = 0; y < grid_y_size; y++) {

                /* this means no points in that grid cell */
                if(gridLocationInRaf_a.get(x).get(y).size() == 0)
                    continue;

                int siz_a = gridLocationInRaf_a.get(x).get(y).size();
                int siz_f = gridLocationInRaf_f.get(x).get(y).size();
                int siz_l = gridLocationInRaf_l.get(x).get(y).size();
                int siz_i = gridLocationInRaf_i.get(x).get(y).size();

                /* This means that we want to merge */
                if (grid_ids_merged.contains(x * grid_y_size + y)) {

                    for (int i = 0; i < siz_a; i++) {

                        if (!all.contains(gridLocationInRaf_a.get(x).get(y).get(i)))
                            continue;

                        if (!all2.contains(gridLocationInRaf_a.get(x).get(y).get(i))) {
                            System.out.println("THIS: " + gridLocationInRaf_a.get(x).get(y).get(i));
                            System.out.println("Merging " + gridLocationInRaf_a.get(x).get(y).get(i) + " with " +
                                    Arrays.toString(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)).toArray()));
                            all2.addAll(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)));

                            double[] newMetrics = redoMetrics(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)), doubles_per_cell, doubles_per_cell_rgb);

                        } else {

                        }
                    }
                    continue;
                } else {

                    double value = 0.0;
                    ArrayList<Double> emptyArrayList = new ArrayList<>();

                    bin_a.readLine(doubles_per_cell * 8, gridLocationInRaf_a.get(x).get(y).get(0) * (doubles_per_cell * 8));
                    for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                        value = bin_a.buffer.getDouble();
                        //System.out.print(value + " ");
                        writer_a.write(value + "\t");

                        if(i_ < 7){
                            emptyArrayList.add(value);
                        }else{
                            emptyArrayList.add(Double.NaN);
                        }
                    }
                    writer_a.write("\n");


                    if(siz_f > 0) {
                        bin_f.readLine(doubles_per_cell_rgb * 8, gridLocationInRaf_f.get(x).get(y).get(0) * (doubles_per_cell_rgb * 8));
                        for (int i_ = 0; i_ < doubles_per_cell_rgb; i_++) {
                            value = bin_f.buffer.getDouble();
                            //System.out.print(value + " ");
                            writer_f.write(value + "\t");
                        }
                        writer_f.write("\n");
                    }else{
                        for (int i_ = 0; i_ < doubles_per_cell_rgb; i_++) {

                            writer_f.write(emptyArrayList.get(i_) + "\t");
                        }
                        writer_f.write("\n");
                    }

                    if(siz_l > 0) {
                        bin_l.readLine(doubles_per_cell * 8, gridLocationInRaf_l.get(x).get(y).get(0) * (doubles_per_cell * 8));
                        for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                            value = bin_l.buffer.getDouble();
                            //System.out.print(value + " ");
                            writer_l.write(value + "\t");
                        }

                        writer_l.write("\n");
                    }else{
                        for (int i_ = 0; i_ < doubles_per_cell; i_++) {

                            writer_l.write(emptyArrayList.get(i_) + "\t");
                        }
                        writer_l.write("\n");
                    }
                    if(siz_i > 0) {
                        bin_i.readLine(doubles_per_cell * 8, gridLocationInRaf_i.get(x).get(y).get(0) * (doubles_per_cell * 8));
                        for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                            value = bin_i.buffer.getDouble();
                            //System.out.print(value + " ");
                            writer_i.write(value + "\t");
                        }

                        writer_i.write("\n");
                    }else{
                        for (int i_ = 0; i_ < doubles_per_cell; i_++) {

                            writer_i.write(emptyArrayList.get(i_) + "\t");
                        }
                        writer_i.write("\n");
                    }
                }
            }
        }

        for(int x = 0; x < grid_x_size; x++) {
            for (int y = 0; y < grid_y_size; y++) {

                /* this means no points in that grid cell */
                if(gridLocationInRaf_a.get(x).get(y).size() == 0)
                    continue;

                int siz_a = gridLocationInRaf_a.get(x).get(y).size();
                int siz_f = gridLocationInRaf_f.get(x).get(y).size();
                int siz_l = gridLocationInRaf_l.get(x).get(y).size();
                int siz_i = gridLocationInRaf_i.get(x).get(y).size();

                /* This means that we want to merge  NOT HERE*/
                if (grid_ids_merged.contains(x * grid_y_size + y) && false) {

                    for (int i = 0; i < siz_a; i++) {

                        if (!all.contains(gridLocationInRaf_a.get(x).get(y).get(i)))
                            continue;

                        if (!all2.contains(gridLocationInRaf_a.get(x).get(y).get(i))) {
                            System.out.println("THIS: " + gridLocationInRaf_a.get(x).get(y).get(i));
                            System.out.println("Merging " + gridLocationInRaf_a.get(x).get(y).get(i) + " with " +
                                    Arrays.toString(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)).toArray()));
                            all2.addAll(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)));

                            double[] newMetrics = redoMetrics(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)), doubles_per_cell, doubles_per_cell_rgb);

                        } else {

                        }
                    }
                    continue;
                } else {

                    double value = 0.0;
                    ArrayList<Double> emptyArrayList = new ArrayList<>();

                    for(int p = 0; p < gridLocationInRaf_a.get(x).get(y).size(); p++) {



                        bin_a.readLine(doubles_per_cell * 8, gridLocationInRaf_a.get(x).get(y).get(p) * (doubles_per_cell * 8));
                        for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                            value = bin_a.buffer.getDouble();
                            //System.out.print(value + " ");
                            writer_a.write(value + "\t");

                            if (i_ < 7) {
                                emptyArrayList.add(value);
                            } else {
                                emptyArrayList.add(Double.NaN);
                            }
                        }
                        writer_a.write("\n");
                    }

                    for(int p = 0; p < gridLocationInRaf_f.get(x).get(y).size(); p++) {


                        if (siz_f > 0) {
                            bin_f.readLine(doubles_per_cell_rgb * 8, gridLocationInRaf_f.get(x).get(y).get(p) * (doubles_per_cell_rgb * 8));
                            for (int i_ = 0; i_ < doubles_per_cell_rgb; i_++) {
                                value = bin_f.buffer.getDouble();
                                //System.out.print(value + " ");
                                writer_f.write(value + "\t");
                            }
                            writer_f.write("\n");
                        } else {
                            for (int i_ = 0; i_ < doubles_per_cell_rgb; i_++) {

                                writer_f.write(emptyArrayList.get(i_) + "\t");
                            }
                            writer_f.write("\n");
                        }
                    }

                    for(int p = 0; p < gridLocationInRaf_l.get(x).get(y).size(); p++) {
                        if (siz_l > 0) {
                            bin_l.readLine(doubles_per_cell * 8, gridLocationInRaf_l.get(x).get(y).get(p) * (doubles_per_cell * 8));
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                                value = bin_l.buffer.getDouble();
                                //System.out.print(value + " ");
                                writer_l.write(value + "\t");
                            }

                            writer_l.write("\n");
                        } else {
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {

                                writer_l.write(emptyArrayList.get(i_) + "\t");
                            }
                            writer_l.write("\n");
                        }
                    }
                    for(int p = 0; p < gridLocationInRaf_i.get(x).get(y).size(); p++) {
                        if (siz_i > 0) {
                            bin_i.readLine(doubles_per_cell * 8, gridLocationInRaf_i.get(x).get(y).get(p) * (doubles_per_cell * 8));
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                                value = bin_i.buffer.getDouble();
                                //System.out.print(value + " ");
                                writer_i.write(value + "\t");
                            }

                            writer_i.write("\n");
                        } else {
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {

                                writer_i.write(emptyArrayList.get(i_) + "\t");
                            }
                            writer_i.write("\n");
                        }
                    }

                }
            }
        }


        writer_a.close();
        writer_f.close();
        writer_l.close();
        writer_i.close();

        bin_a.file.delete();
        bin_f.file.delete();
        bin_l.file.delete();
        bin_i.file.delete();

        bin_a.close();
        bin_f.close();
        bin_l.close();
        bin_i.close();

        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;

        long minutes = (tDelta / 1000)  / 60;
        int seconds = (int)((tDelta / 1000) % 60);

        //System.out.println("TOOK: " + minutes + " min " + seconds + " sec");
    }

    public void start_2() throws Exception{


        long tStart = System.currentTimeMillis();


        aR.output_statistics = true;

        this.grid_x_size = (int)Math.ceil((pointCloud.maxX - orig_x) / resolution);
        this.grid_y_size = (int)Math.ceil((orig_y - pointCloud.minY) / resolution);

        aR.p_update.threadEnd[coreNumber-1] = grid_x_size*grid_y_size;
        float[][] stats = new float[grid_x_size][grid_y_size];

        LasPoint tempPoint = new LasPoint();

        int pienin, suurin;

        ArrayList<ArrayList<Double>> gridPoints_z_a = new ArrayList<>();
        ArrayList<ArrayList<Integer>> gridPoints_i_a = new ArrayList<>();

        ArrayList<ArrayList<Double>> gridPoints_z_f = new ArrayList<>();
        ArrayList<ArrayList<Integer>> gridPoints_i_f = new ArrayList<>();

        ArrayList<ArrayList<Double>> gridPoints_z_l = new ArrayList<>();
        ArrayList<ArrayList<Integer>> gridPoints_i_l = new ArrayList<>();

        ArrayList<ArrayList<Double>> gridPoints_z_i = new ArrayList<>();
        ArrayList<ArrayList<Integer>> gridPoints_i_i = new ArrayList<>();

        ArrayList<ArrayList<int[]>> gridPoints_RGB_f = new ArrayList<>();

        ArrayList<Double> sum_z_a = new ArrayList<>();
        ArrayList<Double> sum_i_a = new ArrayList<>();
        ArrayList<Double> sum_z_f = new ArrayList<>();
        ArrayList<Double> sum_i_f = new ArrayList<>();
        ArrayList<Double> sum_z_l = new ArrayList<>();
        ArrayList<Double> sum_i_l = new ArrayList<>();
        ArrayList<Double> sum_z_i = new ArrayList<>();
        ArrayList<Double> sum_i_i = new ArrayList<>();

        ArrayList<ArrayList<Point>> points = new ArrayList<>();

        int raf_location_a = 0;
        int raf_location_f = 0;
        int raf_location_l = 0;
        int raf_location_i = 0;

        ArrayList<Integer> ids = new ArrayList<>();

        ArrayList<IncrementalTin> tins = new ArrayList<>();
        ArrayList<List<IQuadEdge>> tin_perimeters = new ArrayList<>();


        int doubles_per_cell = 0;
        int doubles_per_cell_rgb = 0;

        pointCloudMetrics pCM = new pointCloudMetrics(this.aR);

        ArrayList<String> colnames = new ArrayList<>();
        Vertex tempV;

        int gridCounter = 0;

        Set<Integer> foundStands = new HashSet<>();

        ArrayList<Double> metrics = new ArrayList<>();

        HashMap<Integer, Integer> order = new HashMap<>();

        ArrayList<Integer> do_overs = new ArrayList<>();

        ArrayList<ArrayList<ArrayList<Integer>>> gridLocationInRaf_a = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> gridLocationInRaf_f = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> gridLocationInRaf_l = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> gridLocationInRaf_i = new ArrayList<>();

        for(int x = 0; x < grid_x_size; x++){
            gridLocationInRaf_a.add(new ArrayList<>());
            gridLocationInRaf_f.add(new ArrayList<>());
            gridLocationInRaf_l.add(new ArrayList<>());
            gridLocationInRaf_i.add(new ArrayList<>());
            for(int y = 0; y < grid_y_size; y++) {

                gridLocationInRaf_a.get(x).add(new ArrayList<>());
                gridLocationInRaf_f.get(x).add(new ArrayList<>());
                gridLocationInRaf_l.get(x).add(new ArrayList<>());
                gridLocationInRaf_i.get(x).add(new ArrayList<>());
            }
        }

        boolean stands_delineated = true;
        /* Define the variables that we need */
        int polygon_id = -1;
        try {
            polygon_id = pointCloud.extraBytes_names.get("polygon_id");
        }catch (Exception e){

            System.out.println("The file is not stand delineated.");
            stands_delineated = false;
        }

        int[][] number_of_points_per_cell = new int[grid_x_size][grid_y_size];

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i++) {

            pointCloud.readRecord(i, tempPoint);
            int x = Math.min((int)((tempPoint.x - pointCloud.minX) / resolution), grid_x_size-1);
            int y = Math.min((int)((pointCloud.maxY - tempPoint.y) / resolution), grid_y_size-1);

            //long c = (long)x << 32 | y & 0xFFFFFFFFL;

            number_of_points_per_cell[x][y]++;

            //int aBack = (int)(c >> 32);
            //int bBack = (int)c;


        }

        int counter = 0;
        ArrayList<double[]>[][] grid_of_points = (ArrayList<double[]>[][]) new ArrayList[grid_x_size][grid_y_size] ;

        if(false)
        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i++) {

            pointCloud.readRecord(i, tempPoint);

            int x = Math.min((int)((tempPoint.x - pointCloud.minX) / resolution), grid_x_size-1);
            int y = Math.min((int)((pointCloud.maxY - tempPoint.y) / resolution), grid_y_size-1);

            //long c = (long)x << 32 | y & 0xFFFFFFFFL;

            number_of_points_per_cell[x][y]--;

            if(grid_of_points[x][y] == null){
                grid_of_points[x][y] = new ArrayList<>();
                grid_of_points[x][y].add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.intensity, tempPoint.numberOfReturns, tempPoint.returnNumber});
            }else
                grid_of_points[x][y].add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.intensity, tempPoint.numberOfReturns, tempPoint.returnNumber});

            if(number_of_points_per_cell[x][y] == 0){
                System.out.printf("%.3fGiB", Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0 * 1024.0));

                System.out.println("process: " + counter++);
                grid_of_points[x][y].clear();
                grid_of_points[x][y] = null;

                if(counter % 1000 == 0){
                    System.gc();
                }

            }

        }

        int[][] cell_only_id = new int[grid_x_size][grid_y_size];

        aR.p_update.threadFile[0] = "lasGridStats";
        aR.p_update.threadProgress[coreNumber-1] = 0;
        aR.p_update.threadEnd[coreNumber-1] = grid_y_size*grid_x_size;
        aR.p_update.updateProgressITD();

        int stand_id = 1;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i++) {

            pointCloud.readRecord(i, tempPoint);

            if(stands_delineated){
                stand_id = tempPoint.getExtraByteInt(polygon_id);
            }else{
            }

            int x = Math.min((int)((tempPoint.x - pointCloud.minX) / resolution), grid_x_size-1);
            int y = Math.min((int)((pointCloud.maxY - tempPoint.y) / resolution), grid_y_size-1);

            //long c = (long)x << 32 | y & 0xFFFFFFFFL;

            number_of_points_per_cell[x][y]--;

            if(grid_of_points[x][y] == null){
                grid_of_points[x][y] = new ArrayList<>();
                grid_of_points[x][y].add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.intensity, tempPoint.numberOfReturns, tempPoint.returnNumber, stand_id});
            }else
                grid_of_points[x][y].add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.intensity, tempPoint.numberOfReturns, tempPoint.returnNumber, stand_id});

            if(number_of_points_per_cell[x][y] == 0){

                counter++;
                cell_only_id[x][y] = -1;

                gridPoints_z_a.clear();
                gridPoints_i_a.clear();

                gridPoints_z_f.clear();
                gridPoints_i_f.clear();

                gridPoints_RGB_f.clear();

                points.clear();

                gridPoints_z_l.clear();
                gridPoints_i_l.clear();

                gridPoints_z_i.clear();
                gridPoints_i_i.clear();

                ids.clear();

                sum_z_a.clear(); sum_z_f.clear(); sum_z_l.clear(); sum_z_i.clear();
                sum_i_a.clear(); sum_i_f.clear(); sum_i_l.clear(); sum_i_i.clear();

                //pointCloud.queryPoly2(orig_x + resolution * x, orig_x + resolution * x + resolution, orig_y - resolution * y - resolution, orig_y - resolution * y);

                gridLocationInRaf_a.get(x).get(y).clear();
                gridLocationInRaf_f.get(x).get(y).clear();
                gridLocationInRaf_l.get(x).get(y).clear();
                gridLocationInRaf_i.get(x).get(y).clear();

                foundStands.clear();

                if (true) {

                    /*
                    for (int u = 0; u < pointCloud.queriedIndexes2.size(); u++) {

                        //System.out.println(Arrays.toString(pointCloud.queriedIndexes2.get(u)));
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

                            pointCloud.readRecord_noRAF(pienin, tempPoint, ero);

                            for (int p = pienin; p <= suurin; p++) {
*/
                    int p = 0;

                    long startTime = System.currentTimeMillis();

                    for(int p_ = 0; p_ < grid_of_points[x][y].size(); p_++){

                        tempPoint.x = grid_of_points[x][y].get(p_)[0];
                        tempPoint.y = grid_of_points[x][y].get(p_)[1];
                        tempPoint.z = grid_of_points[x][y].get(p_)[2];
                        tempPoint.intensity = (int)grid_of_points[x][y].get(p_)[3];
                        tempPoint.numberOfReturns = (int)grid_of_points[x][y].get(p_)[4];
                        tempPoint.returnNumber = (int)grid_of_points[x][y].get(p_)[5];
                        stand_id = (int)grid_of_points[x][y].get(p_)[6];
                        //pointCloud.readFromBuffer(tempPoint);
/*
                                if (tempPoint.z <= z_cutoff)
                                    continue;
*/
                        //if(false)
                        if (tempPoint.x >= (orig_x + resolution * x) && tempPoint.x < (orig_x + resolution * x + resolution) &&
                                tempPoint.y <= (orig_y - resolution * y) && tempPoint.y > (orig_y - resolution * y - resolution)) {

                            //if(!foundStands.contains(tempPoint.pointSourceId)) {
                            //stand_id = 1;
                            //if(stands_delineated){
                            //    stand_id = tempPoint.getExtraByteInt(polygon_id);
                            //}else{
                           // }
                            //if(!foundStands.contains(tempPoint.pointSourceId)) {
                            if(!foundStands.contains(stand_id)) {

                                //tins.add(new IncrementalTin());
                                // tin_perimeters.add(null);

                                ids.add(stand_id);

                                points.add(new ArrayList<>());

                                gridPoints_z_a.add(new ArrayList<>());
                                gridPoints_i_a.add(new ArrayList<>());

                                gridPoints_z_f.add(new ArrayList<>());
                                gridPoints_RGB_f.add(new ArrayList<>());

                                gridPoints_i_f.add(new ArrayList<>());

                                gridPoints_z_l.add(new ArrayList<>());
                                gridPoints_i_l.add(new ArrayList<>());

                                gridPoints_z_i.add(new ArrayList<>());
                                gridPoints_i_i.add(new ArrayList<>());

                                sum_z_a.add(0.0);
                                sum_i_a.add(0.0);

                                sum_z_f.add(0.0);
                                sum_i_f.add(0.0);

                                sum_z_l.add(0.0);
                                sum_i_l.add(0.0);

                                sum_z_i.add(0.0);
                                sum_i_i.add(0.0);

                                order.put(stand_id, gridPoints_z_a.size()-1);
                                foundStands.add(stand_id);

                            }

                                    /* In order to save memory we only add point to tin if it is outside of it. After all,
                                      we only need the bounday of the tin.
                                     */
                            //if(!tins.get(order.get(tempPoint.pointSourceId)).isPointInsideTin(tempPoint.x, tempPoint.y)){
                                    /*
                                    if(tins.get(order.get(tempPoint.pointSourceId)).isBootstrapped()) {

                                        if(tin_perimeters.get(order.get(tempPoint.pointSourceId)) == null){
                                            tin_perimeters.set(order.get(tempPoint.pointSourceId), tins.get(order.get(tempPoint.pointSourceId)).getPerimeter());
                                        }

                                        if (isPointInPolygon(tin_perimeters.get(order.get(tempPoint.pointSourceId)), tempPoint.x, tempPoint.y) == Polyside.Result.Outside) {
                                            //System.out.println(tin_perimeters.get(order.get(tempPoint.pointSourceId)).size());
                                            tempV = new Vertex(tempPoint.x, tempPoint.y, 0.0);
                                            //perim.add(i);
                                            tempV.setIndex(p);
                                            tins.get(order.get(tempPoint.pointSourceId)).add(tempV);
                                            tin_perimeters.set(order.get(tempPoint.pointSourceId), tins.get(order.get(tempPoint.pointSourceId)).getPerimeter());

                                        }
                                    }else{
                                        tempV = new Vertex(tempPoint.x, tempPoint.y, 0.0);
                                        //perim.add(i);
                                        tempV.setIndex(p);
                                        tins.get(order.get(tempPoint.pointSourceId)).add(tempV);

                                    }

                                     */
/*
                                    que_gridPoints_z_a.add(tempPoint.z);
                                    que_gridPoints_i_a.add(tempPoint.intensity);
*/
                            gridPoints_z_a.get(order.get(stand_id)).add(tempPoint.z);
                            gridPoints_i_a.get(order.get(stand_id)).add(tempPoint.intensity);

                            sum_z_a.set(order.get(stand_id), sum_z_a.get(order.get(stand_id)) + tempPoint.z);
                            sum_i_a.set(order.get(stand_id), sum_i_a.get(order.get(stand_id)) + tempPoint.intensity);

                            points.get(order.get(stand_id)).add(new Point(tempPoint.x, tempPoint.y));

                            //System.out.println("intensity" + tempPoint.intensity);

                            if (tempPoint.returnNumber == 1) {
                                        /*
                                        sum_z_f += tempPoint.z;
                                        sum_i_f += tempPoint.intensity;

                                        gridPoints_z_f.add(tempPoint.z);
                                        gridPoints_i_f.add(tempPoint.intensity);

                                         */
                                gridPoints_z_f.get(order.get(stand_id)).add(tempPoint.z);

                                gridPoints_RGB_f.get(order.get(stand_id)).add(new int[]{tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});

                                gridPoints_i_f.get(order.get(stand_id)).add(tempPoint.intensity);

                                sum_z_f.set(order.get(stand_id), sum_z_f.get(order.get(stand_id)) + tempPoint.z);
                                sum_i_f.set(order.get(stand_id), sum_i_f.get(order.get(stand_id)) + tempPoint.intensity);

                            }
                            if (tempPoint.returnNumber == tempPoint.numberOfReturns) {
/*
                                        sum_z_l += tempPoint.z;
                                        sum_i_l += tempPoint.intensity;

                                        gridPoints_z_l.add(tempPoint.z);
                                        gridPoints_i_l.add(tempPoint.intensity);

 */
                                gridPoints_z_l.get(order.get(stand_id)).add(tempPoint.z);
                                gridPoints_i_l.get(order.get(stand_id)).add(tempPoint.intensity);

                                sum_z_l.set(order.get(stand_id), sum_z_l.get(order.get(stand_id)) + tempPoint.z);
                                sum_i_l.set(order.get(stand_id), sum_i_l.get(order.get(stand_id)) + tempPoint.intensity);

                            }
                            if (tempPoint.returnNumber > 1 && tempPoint.returnNumber != tempPoint.numberOfReturns) {
/*
                                        sum_z_i += tempPoint.z;
                                        sum_i_i += tempPoint.intensity;
                                        gridPoints_z_i.add(tempPoint.z);
                                        gridPoints_i_i.add(tempPoint.intensity);
 */
                                gridPoints_z_i.get(order.get(stand_id)).add(tempPoint.z);
                                gridPoints_i_i.get(order.get(stand_id)).add(tempPoint.intensity);

                                sum_z_i.set(order.get(stand_id), sum_z_i.get(order.get(stand_id)) + tempPoint.z);
                                sum_i_i.set(order.get(stand_id), sum_i_i.get(order.get(stand_id)) + tempPoint.intensity);

                            }
                        }
                        grid_of_points[x][y].set(p_, null);

                    }
                    // }
                    //}
                    long stopTime = System.currentTimeMillis();
                    //System.out.println(stopTime - startTime);
                }else{
                    //System.out.println("WHAT THE FUCK!!");
                }

                grid_of_points[x][y].clear();
                grid_of_points[x][y] = null;

                aR.p_update.threadProgress[0]++;

                /* No points in this rectangle */
                if(ids.size() == 0)
                    continue;

                ArrayList<Double> areas = new ArrayList<>();

                int divided = foundStands.size() > 1 ? 1 : 0;

                doubles_per_cell = 0;

                if(divided == 1 ){

                    do_overs.add( x * grid_y_size + y );

                }else{

                    cell_only_id[x][y] = ids.get(0);

                }

                double grid_cell_id = y * grid_x_size + x;
/*
                for(int ii = 0; ii < gridPoints_z_a.size(); ii++) {

                    areas.add(tins.get(ii).countTriangles().getAreaSum());
                    tins.get(ii).clear();
                    tins.get(ii).dispose();
                    tins.set(ii, new IncrementalTin());
                    tin_perimeters.set(ii, null);

                }
                System.gc();

 */

                for(int ii = 0; ii < gridPoints_z_a.size(); ii++) {
                    QuickHull qh = new QuickHull();
                    ArrayList<Point> p = null;

                    if (points.get(ii).size() > 3) {
                        p = qh.quickHull(points.get(ii));
                        areas.add(polygonArea(p));
                    } else {
                        areas.add(-99.9);
                    }
                }



                double x_coord = orig_x + resolution * x;
                double y_coord = orig_y - resolution * y;

                boolean addedBecauseTooSmall = false;

                /* Iterate over all the plot ids within this grid cell */
                for(int ii = 0; ii < gridPoints_z_a.size(); ii++) {

                    /* If this plot id within this grid cell has more than 10 points */
                    if(gridPoints_z_a.get(ii).size() > aR.min_points) {

                        metrics = pCM.calc(gridPoints_z_a.get(ii), gridPoints_i_a.get(ii), sum_z_a.get(ii), sum_i_a.get(ii), "_a", colnames);

                        if(colnames_metrics_a.size() == 0)
                            colnames_metrics_a = (ArrayList<String>)colnames.clone();

                        gridLocationInRaf_a.get(x).get(y).add(raf_location_a);

                        bin_a.writeDouble(grid_cell_id);
                        bin_a.writeDouble(ids.get(ii));
                        bin_a.writeDouble(divided);
                        bin_a.writeDouble(0.0);
                        bin_a.writeDouble(areas.get(ii));
                        bin_a.writeDouble(x_coord);
                        bin_a.writeDouble(y_coord);

                        if(divided == 0 && areas.get(ii) <= (resolution * resolution * 0.66) && !addedBecauseTooSmall){
                            addedBecauseTooSmall = true;
                            do_overs.add( x * grid_y_size + y );
                        }

                        for (Double metric : metrics) {
                            bin_a.writeDouble(metric);
                        }
                        raf_location_a++;

                    }
                }

                if(doubles_per_cell == 0)
                    doubles_per_cell = 7 + metrics.size();

                long start_time = System.currentTimeMillis();

                for(int ii = 0; ii < gridPoints_z_f.size(); ii++) {

                    if(gridPoints_z_f.get(ii).size() > aR.min_points) {

                        metrics = pCM.calc_with_RGB(gridPoints_z_f.get(ii), gridPoints_i_f.get(ii), sum_z_f.get(ii), sum_i_f.get(ii), "_f", colnames, gridPoints_RGB_f.get(ii));

                        if(colnames_metrics_f.size() == 0)
                            colnames_metrics_f = (ArrayList<String>)colnames.clone();

                        bin_f.writeDouble(grid_cell_id);
                        bin_f.writeDouble(ids.get(ii));
                        bin_f.writeDouble(divided);
                        bin_f.writeDouble(0.0);
                        bin_f.writeDouble(areas.get(ii));
                        bin_f.writeDouble(x_coord);
                        bin_f.writeDouble(y_coord);

                        for (Double metric : metrics) {
                            bin_f.writeDouble(metric);
                        }

                        gridLocationInRaf_f.get(x).get(y).add(raf_location_f);
                        raf_location_f++;

                        if(doubles_per_cell_rgb == 0)
                            doubles_per_cell_rgb = 7 + metrics.size();

                    }
                }



                for(int ii = 0; ii < gridPoints_z_l.size(); ii++) {

                    if(gridPoints_z_l.get(ii).size() > aR.min_points) {

                        metrics = pCM.calc(gridPoints_z_l.get(ii), gridPoints_i_l.get(ii), sum_z_l.get(ii), sum_i_l.get(ii), "_l", colnames);

                        if(colnames_metrics_l.size() == 0)
                            colnames_metrics_l = (ArrayList<String>)colnames.clone();

                        bin_l.writeDouble(grid_cell_id);
                        bin_l.writeDouble(ids.get(ii));
                        bin_l.writeDouble(divided);
                        bin_l.writeDouble(0.0);
                        bin_l.writeDouble(areas.get(ii));
                        bin_l.writeDouble(x_coord);
                        bin_l.writeDouble(y_coord);

                        for (Double metric : metrics) {
                            bin_l.writeDouble(metric);
                        }

                        gridLocationInRaf_l.get(x).get(y).add(raf_location_l);
                        raf_location_l++;

                    }
                }

                for(int ii = 0; ii < gridPoints_z_i.size(); ii++) {

                    if(gridPoints_z_i.get(ii).size() > aR.min_points) {



                        metrics = pCM.calc(gridPoints_z_i.get(ii), gridPoints_i_i.get(ii), sum_z_i.get(ii), sum_i_i.get(ii), "_i", colnames);


                        if(colnames_metrics_i.size() == 0)
                            colnames_metrics_i = (ArrayList<String>)colnames.clone();

                        bin_i.writeDouble(grid_cell_id);
                        bin_i.writeDouble(ids.get(ii));
                        bin_i.writeDouble(divided);
                        bin_i.writeDouble(0.0);
                        bin_i.writeDouble(areas.get(ii));
                        bin_i.writeDouble(x_coord);
                        bin_i.writeDouble(y_coord);

                        for (Double metric : metrics) {
                            bin_i.writeDouble(metric);
                        }

                        gridLocationInRaf_i.get(x).get(y).add(raf_location_i);
                        raf_location_i++;

                    }
                }

                foundStands.clear();

                gridCounter++;

                aR.p_update.threadProgress[coreNumber-1]++;


                if(counter % 100 == 0){
                    aR.p_update.updateProgressGridStats();
                }
                if(counter % 1000 == 0)
                    System.gc();

            }
        }
        bin_a.writeBuffer2();
        bin_a.refresh();
        bin_f.writeBuffer2();
        bin_f.refresh();
        bin_l.writeBuffer2();
        bin_l.refresh();
        bin_i.writeBuffer2();
        bin_i.refresh();


        HashMap<Integer, HashSet<Integer>> neighborhoods = new HashMap<>();
        neighborhood_areas = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> gridsMerged_one_way = new HashMap<>();

        HashSet<Integer> grid_ids_merged = new HashSet<>();



        double optimalSize = resolution * resolution;

        if(false)
            for(int i : do_overs){

                System.out.println("-----------------------------");
                System.out.println("-----------------------------");
                System.out.println("-----------------------------");

                System.out.println("DO OVER: " + i);

                int x = i / grid_y_size ;
                int y = (i - x * grid_y_size) ;

                int siz = gridLocationInRaf_a.get(x).get(y).size();

                int plot_id;
                int plot_id2;
                double grid_id2 = -1;
                double optimal_pair_grid_id = -1;
                double area;
                int divided = 0;
                int modded = 0;

                Set<Integer> currentNeghborhood = new HashSet<>();

                for(int j = 0; j < siz; j++){

                    int pos = gridLocationInRaf_a.get(x).get(y).get(j);

                    System.out.println("pos: " + pos);

                    if(neighborhoods.containsKey(pos)) {
                        System.out.println("Pos: " + pos + " containedd");
                        continue;
                    }

                    bin_a.seek(pos * (doubles_per_cell * 8));

                    double grid_id = bin_a.readDouble();

                    System.out.println("Grid id: " + grid_id);
                    plot_id = (int)bin_a.readDouble();

                    divided = (int)bin_a.readDouble();
                    modded = (int)bin_a.readDouble();
                    area = bin_a.readDouble();

                    currentNeghborhood.clear();

                    /* This means that this split cell is already used in a neighborhood */
                    if(neighborhoods.containsKey(pos)){

                        area = neighborhood_areas.get(pos);
                        currentNeghborhood = neighborhoods.get(pos);

                    }


                    System.out.println(grid_x_size + " " + grid_y_size);
                    System.out.println(area + " " + x + " " + y + " " + i + " " + siz);

                    int siz2 = 0;

                    System.out.println("Finding optimal neighbor for " + plot_id + " " + pos);

                    double areaOrig = area;
                    double bestSize = Math.abs(area - optimalSize);
                    double optimalAreaAddition = 0;
                    int pair = -1;

                    int[] x__ = new int[4];
                    int[] y__ = new int[4];

                    x__[0] = x; y__[0] = y + 1;
                    x__[1] = x + 1; y__[1] = y;
                    x__[2] = x; y__[2] = y - 1;
                    x__[3] = x - 1; y__[3] = y;

                    for(int f = 0; f < 4; f++){

                        int x_ = x__[f];
                        int y_ = y__[f];

                        if(x_ < 0 || x_ >= grid_x_size || y_ >= grid_y_size || y_ < 0)
                            continue;

                        siz2 = gridLocationInRaf_a.get(x_).get(y_).size();
                        //System.out.println("x_: " + x_ + " y_: " + y_ + " siz2: " + siz2);

                        for(int h = 0; h < siz2; h++){

                            int pos2 = gridLocationInRaf_a.get(x_).get(y_).get(h);

                            if(currentNeghborhood.contains(pos2))
                                continue;

                            bin_a.seek(pos2 * (doubles_per_cell * 8));
                            grid_id2 = bin_a.readDouble();
                            plot_id2 = (int)bin_a.readDouble();

                            if(plot_id2 != plot_id)
                                continue;

                            divided = (int)bin_a.readDouble();
                            modded = (int)bin_a.readDouble();

                            area = bin_a.readDouble();

                            if(neighborhood_areas.containsKey(pos2))
                                area = neighborhood_areas.get(pos2);
/*
                        if(gridsMerged_all.containsKey(pos2)){

                            area = gridsMerged_all.get(pos2).get(0);

                        }
*/
                            if(Math.abs(area + areaOrig - optimalSize) < bestSize){

                                bestSize = Math.abs(area + areaOrig - optimalSize);
                                optimalAreaAddition = area;
                                pair = pos2;
                                optimal_pair_grid_id = grid_id2;

                            }

                        }
                    }

                    if(pair != -1){

                        /* IF NEITHER ARE IN NEIGHBORHOODS! */
                        if(!neighborhoods.containsKey(pair) && !neighborhoods.containsKey(pos)) {

                            neighborhoods.put(pair, new HashSet<>());
                            neighborhoods.get(pair).add(pos);
                            neighborhoods.get(pair).add(pair);

                            neighborhoods.put(pos, new HashSet<>());
                            neighborhoods.get(pos).add(pair);
                            neighborhoods.get(pos).add(pos);

                            neighborhood_areas.put(pair,  optimalAreaAddition + areaOrig);
                            neighborhood_areas.put(pos,  optimalAreaAddition + areaOrig);

                            grid_ids_merged.add((int)grid_id);
                            grid_ids_merged.add((int)optimal_pair_grid_id);

                            System.out.println(" DEBUG: " + grid_id + " " + optimal_pair_grid_id);

                            System.out.println(optimalAreaAddition + " " + areaOrig);

                        }
                        /* IF BOTH ARE IN NEIGHBORHOODS! */
                        else if(neighborhoods.containsKey(pair) && neighborhoods.containsKey(pos)){

                            ArrayList<Integer> modThese_pair = new ArrayList<>(neighborhoods.get(pair));
                            ArrayList<Integer> modThese_pos = new ArrayList<>(neighborhoods.get(pos));


                            for(int i_ : modThese_pair) {

                                neighborhoods.get(i_).addAll(modThese_pos);
                                neighborhood_areas.put(i_,  optimalAreaAddition + areaOrig);
                            }
                            for(int i_ : modThese_pos) {

                                neighborhoods.get(i_).addAll(modThese_pair);
                                neighborhood_areas.put(i_,  optimalAreaAddition + areaOrig);

                            }
                        }else if(!neighborhoods.containsKey(pair) && neighborhoods.containsKey(pos)){

                            grid_ids_merged.add((int)optimal_pair_grid_id);

                            ArrayList<Integer> modThese = new ArrayList<>(neighborhoods.get(pos));
                            neighborhoods.put(pair, new HashSet<>());


                            for(int i_ : modThese) {

                                neighborhoods.get(i_).add(pair);
                                neighborhood_areas.put(i_,  optimalAreaAddition + areaOrig);

                            }

                            neighborhoods.get(pair).addAll(modThese);
                            neighborhood_areas.put(pair,  optimalAreaAddition + areaOrig);

                        }
                        else if(neighborhoods.containsKey(pair) && !neighborhoods.containsKey(pos)){

                            grid_ids_merged.add((int)grid_id);

                            ArrayList<Integer> modThese = new ArrayList<>(neighborhoods.get(pair));
                            neighborhoods.put(pos, new HashSet<>());

                            for(int i_ : modThese) {

                                neighborhoods.get(i_).add(pos);
                                neighborhood_areas.put(i_,  optimalAreaAddition + areaOrig);

                            }

                            neighborhoods.get(pos).addAll(modThese);
                            neighborhood_areas.put(pos,  optimalAreaAddition + areaOrig);

                        }
                        System.out.println("bestSizeIndex: " + pair + " bestSize: " + bestSize + " neigh size: " + neighborhood_areas.get(pos) + " n_neigh: " + neighborhoods.get(pos).size());
                    }else{
                        System.out.println("FAILED TO FIND NEIGHBOR FOR : " + pos);
                        System.out.println(grid_ids_merged.size());


                    }
                }
/*
            for(int i_ : grid_ids_merged){
                System.out.println(i_);
            }
            System.exit(1);
            */

            }



        ArrayList<HashSet<Integer>> finalMerges = new ArrayList<>();
        finalMerges_areas = new HashMap<Integer, Double>();

        HashSet<Integer> all = new HashSet<>();
        HashSet<Integer> all2 = new HashSet<>();



        for(int i_ : neighborhoods.keySet()){

            if(finalMerges.size() == 0){

                finalMerges.add(neighborhoods.get(i_));
                all.addAll(neighborhoods.get(i_));
                finalMerges_areas.put(i_, neighborhood_areas.get(i_));
                continue;

            }

            if(!all.contains(i_)){

                finalMerges.add(neighborhoods.get(i_));
                finalMerges_areas.put(i_, neighborhood_areas.get(i_));
                all.addAll(neighborhoods.get(i_));

            }

        }

        for(int i_ = 0; i_ < finalMerges.size(); i_++){

            System.out.println(Arrays.toString(finalMerges.get(i_).toArray()) + " " + finalMerges_areas.get(i_));

        }
/*
        System.out.println();
        for(int i : grid_ids_merged){
            System.out.print(i + "\t");
        }
        System.exit(1);
*/

        //FileWriter writer = null;
        try {
            writer_a = new FileWriter(this.outputMetricFile_a);
            writer_f = new FileWriter(this.outputMetricFile_f);
            writer_l = new FileWriter(this.outputMetricFile_l);
            writer_i = new FileWriter(this.outputMetricFile_i);

        }catch (Exception e){
            e.printStackTrace();
        }
        /*
                                   bin_i.writeDouble(grid_cell_id);
                                   bin_i.writeDouble(ids.get(ii));
                                   bin_i.writeDouble(divided);
                                   bin_i.writeDouble(0.0);
                                   bin_i.writeDouble(areas.get(ii));
         */
        writer_a.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");
        writer_f.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");
        writer_l.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");
        writer_i.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");

        for(int i = 0; i < colnames_metrics_a.size(); i++){
            writer_a.write(colnames_metrics_a.get(i) + "\t");
        }

        for(int i = 0; i < colnames_metrics_l.size(); i++) {
            writer_l.write(colnames_metrics_l.get(i) + "\t");

        }

        for(int i = 0; i < colnames_metrics_i.size(); i++) {
            writer_i.write(colnames_metrics_i.get(i) + "\t");
        }

            /* First and only contain RGB, so we need to do it separately */
        for(int i = 0; i < colnames_metrics_f.size(); i++) {
            writer_f.write(colnames_metrics_f.get(i) + "\t");
        }


        writer_a.write("\n");
        writer_f.write("\n");
        writer_l.write("\n");
        writer_i.write("\n");

        /* This is for if we want to do the merging thingy */
        if(false)
            for(int x = 0; x < grid_x_size; x++) {
                for (int y = 0; y < grid_y_size; y++) {

                    /* this means no points in that grid cell */
                    if(gridLocationInRaf_a.get(x).get(y).size() == 0)
                        continue;

                    int siz_a = gridLocationInRaf_a.get(x).get(y).size();
                    int siz_f = gridLocationInRaf_f.get(x).get(y).size();
                    int siz_l = gridLocationInRaf_l.get(x).get(y).size();
                    int siz_i = gridLocationInRaf_i.get(x).get(y).size();

                    /* This means that we want to merge */
                    if (grid_ids_merged.contains(x * grid_y_size + y)) {

                        for (int i = 0; i < siz_a; i++) {

                            if (!all.contains(gridLocationInRaf_a.get(x).get(y).get(i)))
                                continue;

                            if (!all2.contains(gridLocationInRaf_a.get(x).get(y).get(i))) {
                                System.out.println("THIS: " + gridLocationInRaf_a.get(x).get(y).get(i));
                                System.out.println("Merging " + gridLocationInRaf_a.get(x).get(y).get(i) + " with " +
                                        Arrays.toString(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)).toArray()));
                                all2.addAll(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)));

                                double[] newMetrics = redoMetrics(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)), doubles_per_cell, doubles_per_cell_rgb);

                            } else {

                            }
                        }
                        continue;
                    } else {

                        double value = 0.0;
                        ArrayList<Double> emptyArrayList = new ArrayList<>();

                        bin_a.readLine(doubles_per_cell * 8, gridLocationInRaf_a.get(x).get(y).get(0) * (doubles_per_cell * 8));
                        for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                            value = bin_a.buffer.getDouble();
                            //System.out.print(value + " ");
                            writer_a.write(value + "\t");

                            if(i_ < 7){
                                emptyArrayList.add(value);
                            }else{
                                emptyArrayList.add(Double.NaN);
                            }
                        }
                        writer_a.write("\n");


                        if(siz_f > 0) {
                            bin_f.readLine(doubles_per_cell_rgb * 8, gridLocationInRaf_f.get(x).get(y).get(0) * (doubles_per_cell_rgb * 8));
                            for (int i_ = 0; i_ < doubles_per_cell_rgb; i_++) {
                                value = bin_f.buffer.getDouble();
                                //System.out.print(value + " ");
                                writer_f.write(value + "\t");
                            }
                            writer_f.write("\n");
                        }else{
                            for (int i_ = 0; i_ < doubles_per_cell_rgb; i_++) {

                                writer_f.write(emptyArrayList.get(i_) + "\t");
                            }
                            writer_f.write("\n");
                        }

                        if(siz_l > 0) {
                            bin_l.readLine(doubles_per_cell * 8, gridLocationInRaf_l.get(x).get(y).get(0) * (doubles_per_cell * 8));
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                                value = bin_l.buffer.getDouble();
                                //System.out.print(value + " ");
                                writer_l.write(value + "\t");
                            }

                            writer_l.write("\n");
                        }else{
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {

                                writer_l.write(emptyArrayList.get(i_) + "\t");
                            }
                            writer_l.write("\n");
                        }
                        if(siz_i > 0) {
                            bin_i.readLine(doubles_per_cell * 8, gridLocationInRaf_i.get(x).get(y).get(0) * (doubles_per_cell * 8));
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                                value = bin_i.buffer.getDouble();
                                //System.out.print(value + " ");
                                writer_i.write(value + "\t");
                            }

                            writer_i.write("\n");
                        }else{
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {

                                writer_i.write(emptyArrayList.get(i_) + "\t");
                            }
                            writer_i.write("\n");
                        }
                    }
                }
            }

        for(int x = 0; x < grid_x_size; x++) {
            for (int y = 0; y < grid_y_size; y++) {

                /* this means no points in that grid cell */
                if(gridLocationInRaf_a.get(x).get(y).size() == 0)
                    continue;

                int siz_a = gridLocationInRaf_a.get(x).get(y).size();
                int siz_f = gridLocationInRaf_f.get(x).get(y).size();
                int siz_l = gridLocationInRaf_l.get(x).get(y).size();
                int siz_i = gridLocationInRaf_i.get(x).get(y).size();

                /* This means that we want to merge  NOT HERE*/
                if (grid_ids_merged.contains(x * grid_y_size + y) && false) {

                    for (int i = 0; i < siz_a; i++) {

                        if (!all.contains(gridLocationInRaf_a.get(x).get(y).get(i)))
                            continue;

                        if (!all2.contains(gridLocationInRaf_a.get(x).get(y).get(i))) {
                            System.out.println("THIS: " + gridLocationInRaf_a.get(x).get(y).get(i));
                            System.out.println("Merging " + gridLocationInRaf_a.get(x).get(y).get(i) + " with " +
                                    Arrays.toString(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)).toArray()));
                            all2.addAll(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)));

                            double[] newMetrics = redoMetrics(neighborhoods.get(gridLocationInRaf_a.get(x).get(y).get(i)), doubles_per_cell, doubles_per_cell_rgb);

                        } else {

                        }
                    }
                    continue;
                } else {

                    double value = 0.0;
                    ArrayList<Double> emptyArrayList = new ArrayList<>();

                    for(int p = 0; p < gridLocationInRaf_a.get(x).get(y).size(); p++) {



                        bin_a.readLine(doubles_per_cell * 8, gridLocationInRaf_a.get(x).get(y).get(p) * (doubles_per_cell * 8));
                        for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                            value = bin_a.buffer.getDouble();
                            //System.out.print(value + " ");
                            writer_a.write(value + "\t");

                            if (i_ < 7) {
                                emptyArrayList.add(value);
                            } else {
                                emptyArrayList.add(Double.NaN);
                            }
                        }
                        writer_a.write("\n");
                    }

                    for(int p = 0; p < gridLocationInRaf_f.get(x).get(y).size(); p++) {


                        if (siz_f > 0) {
                            bin_f.readLine(doubles_per_cell_rgb * 8, gridLocationInRaf_f.get(x).get(y).get(p) * (doubles_per_cell_rgb * 8));
                            for (int i_ = 0; i_ < doubles_per_cell_rgb; i_++) {
                                value = bin_f.buffer.getDouble();
                                //System.out.print(value + " ");
                                writer_f.write(value + "\t");
                            }
                            writer_f.write("\n");
                        } else {
                            for (int i_ = 0; i_ < doubles_per_cell_rgb; i_++) {

                                writer_f.write(emptyArrayList.get(i_) + "\t");
                            }
                            writer_f.write("\n");
                        }
                    }

                    for(int p = 0; p < gridLocationInRaf_l.get(x).get(y).size(); p++) {
                        if (siz_l > 0) {
                            bin_l.readLine(doubles_per_cell * 8, gridLocationInRaf_l.get(x).get(y).get(p) * (doubles_per_cell * 8));
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                                value = bin_l.buffer.getDouble();
                                //System.out.print(value + " ");
                                writer_l.write(value + "\t");
                            }

                            writer_l.write("\n");
                        } else {
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {

                                writer_l.write(emptyArrayList.get(i_) + "\t");
                            }
                            writer_l.write("\n");
                        }
                    }
                    for(int p = 0; p < gridLocationInRaf_i.get(x).get(y).size(); p++) {
                        if (siz_i > 0) {
                            bin_i.readLine(doubles_per_cell * 8, gridLocationInRaf_i.get(x).get(y).get(p) * (doubles_per_cell * 8));
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {
                                value = bin_i.buffer.getDouble();
                                //System.out.print(value + " ");
                                writer_i.write(value + "\t");
                            }

                            writer_i.write("\n");
                        } else {
                            for (int i_ = 0; i_ < doubles_per_cell; i_++) {

                                writer_i.write(emptyArrayList.get(i_) + "\t");
                            }
                            writer_i.write("\n");
                        }
                    }

                }
            }
        }


        writer_a.close();
        writer_f.close();
        writer_l.close();
        writer_i.close();

        bin_a.file.delete();
        bin_f.file.delete();
        bin_l.file.delete();
        bin_i.file.delete();

        bin_a.close();
        bin_f.close();
        bin_l.close();
        bin_i.close();

        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;

        long minutes = (tDelta / 1000)  / 60;
        int seconds = (int)((tDelta / 1000) % 60);

        System.out.println("TOOK: " + minutes + " min " + seconds + " sec");
    }


    public double[] redoMetrics(HashSet<Integer> pos, int doubles_per_cell, int doubles_per_cell_rgb) throws Exception{

        ArrayList<String> colnames = new ArrayList<>();
        pointCloudMetrics pCM = new pointCloudMetrics(this.aR);

        double minx = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;

        double miny = Double.POSITIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;

        ArrayList<double[]> previousLines = new ArrayList<>();

        for(int i = 0; i < pos.size(); i++)
            previousLines.add(new double[doubles_per_cell]);

        int counter = 0;

        double[][] squareDimensions = new double[pos.size()][4];

        short plot_id = 0;

        ArrayList<Double> gridPoints_z_a = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_a = new ArrayList<>();

        ArrayList<Double> gridPoints_z_f = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_f = new ArrayList<>();

        ArrayList<Double> gridPoints_z_l = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_l = new ArrayList<>();

        ArrayList<Double> gridPoints_z_i = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_i = new ArrayList<>();

        ArrayList<int[]> gridPoints_rgb = new ArrayList<>();

        double sum_z_a = 0.0;
        double sum_i_a = 0.0;
        double sum_z_f = 0.0;
        double sum_i_f = 0.0;
        double sum_z_l = 0.0;
        double sum_i_l = 0.0;
        double sum_z_i = 0.0;
        double sum_i_i = 0.0;

        double area = 0.0;

        for(int i : pos){

            bin_a.readLine(doubles_per_cell *  8, i * (doubles_per_cell * 8));
            double value = 0.0;

            System.out.println("cells to merge: " + pos.size());
            area = neighborhood_areas.get(i);
            System.out.println(area);

            for(int i_ = 0; i_ < doubles_per_cell; i_++) {

                value = bin_a.buffer.getDouble();
                previousLines.get(counter)[i_] = value;
                //System.out.print(value + " ");

            }

            int grid_id = (int)previousLines.get(counter)[0];

            int x = grid_id / grid_y_size ;
            int y = (grid_id - x * grid_y_size) ;
            plot_id = (short)previousLines.get(counter)[1];
            System.out.println(x + " " + y + " " + plot_id);

            double x_min = this.orig_x + resolution * x;
            double x_max = this.orig_x + resolution * x + resolution;
            double y_max = this.orig_y - resolution * y;
            double y_min = this.orig_y - resolution * y - resolution;

            squareDimensions[counter][0] = x_min;
            squareDimensions[counter][1] = x_max;
            squareDimensions[counter][2] = y_min;
            squareDimensions[counter][3] = y_max;

            if(x_min < minx)
                minx = x_min;
            if(x_max > maxx)
                maxx = x_max;

            if(y_min < miny)
                miny = y_min;
            if(y_max > maxy)
                maxy = y_max;

            counter++;

        }

        previousLines.get(0)[4] = area;

        int suurin, pienin;
        LasPoint tempPoint = new LasPoint();

        pointCloud.queryPoly2(minx, maxx, miny, maxy);

        boolean accept = false;


        if (pointCloud.queriedIndexes2.size() > 0) {

            for (int u = 0; u < pointCloud.queriedIndexes2.size(); u++) {

                //System.out.println(Arrays.toString(pointCloud.queriedIndexes2.get(u)));
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

                    pointCloud.readRecord_noRAF(pienin, tempPoint, ero);

                    for (int p = pienin; p <= suurin; p++) {

                        pointCloud.readFromBuffer(tempPoint);

                        /* Reading, so ask if this point is ok, or if
                        it should be modified.
                         */
                        if(!aR.inclusionRule.ask(tempPoint, p, true)){
                            continue;
                        }

/*
                        if (tempPoint.z <= this.z_cutoff)
                            continue;
*/
                        if(accept(tempPoint, squareDimensions))
                            if(tempPoint.pointSourceId == plot_id){


                                gridPoints_z_a.add(tempPoint.z);
                                gridPoints_i_a.add(tempPoint.intensity);

                                sum_z_a += tempPoint.z;
                                sum_i_a += tempPoint.intensity;


                                if (tempPoint.returnNumber == 1) {

                                    gridPoints_z_f.add(tempPoint.z);
                                    gridPoints_i_f.add(tempPoint.intensity);

                                    gridPoints_rgb.add(new int[]{tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});

                                    sum_z_f += tempPoint.z;
                                    sum_i_f += tempPoint.intensity;
                                }
                                if (tempPoint.returnNumber == tempPoint.numberOfReturns) {

                                    gridPoints_z_l.add(tempPoint.z);
                                    gridPoints_i_l.add(tempPoint.intensity);

                                    sum_z_l += tempPoint.z;
                                    sum_i_l += tempPoint.intensity;

                                }
                                if (tempPoint.returnNumber > 1 && tempPoint.returnNumber != tempPoint.numberOfReturns) {

                                    gridPoints_z_i.add(tempPoint.z);
                                    gridPoints_i_i.add(tempPoint.intensity);

                                    sum_z_i += tempPoint.z;
                                    sum_i_i += tempPoint.intensity;

                                }
                            }
                    }
                }
            }
        }

        ArrayList<String> colnames_f = new ArrayList<>();

        ArrayList<Double> metrics_a = pCM.calc(gridPoints_z_a, gridPoints_i_a, sum_z_a, sum_i_a, "_a", colnames);
        ArrayList<Double> metrics_f = pCM.calc_with_RGB(gridPoints_z_f, gridPoints_i_f, sum_z_f, sum_i_f, "_f", colnames_f, gridPoints_rgb);
        ArrayList<Double> metrics_l = pCM.calc(gridPoints_z_l, gridPoints_i_l, sum_z_l, sum_i_l, "_l", colnames);
        ArrayList<Double> metrics_i = pCM.calc(gridPoints_z_i, gridPoints_i_i, sum_z_i, sum_i_i, "_i", colnames);

        for(int i = 0; i < 7; i++){
            writer_a.write(previousLines.get(0)[i] + "\t");
            writer_f.write(previousLines.get(0)[i] + "\t");
            writer_l.write(previousLines.get(0)[i] + "\t");
            writer_i.write(previousLines.get(0)[i] + "\t");
        }


        for(int i = 0; i < colnames.size(); i++){

            if(gridPoints_z_a.size() > aR.min_points)
                writer_a.write(metrics_a.get(i) + "\t");
            else
                writer_a.write(Double.NaN + "\t");

            if(gridPoints_z_l.size() > aR.min_points)
                writer_l.write(metrics_l.get(i) + "\t");
            else
                writer_l.write(Double.NaN + "\t");

            if(gridPoints_z_i.size() > aR.min_points)
                writer_i.write(metrics_i.get(i) + "\t");
            else
                writer_i.write(Double.NaN + "\t");
        }

        for(int i = 0; i < colnames_f.size(); i++){


            if(gridPoints_z_f.size() > aR.min_points)
                writer_f.write(metrics_f.get(i) + "\t");
            else
                writer_f.write(Double.NaN + "\t");

        }

        writer_a.write("\n");
        writer_f.write("\n");
        writer_l.write("\n");
        writer_i.write("\n");

        return new double[]{0,0};

    }

    public boolean accept(LasPoint tempPoint, double[][] grids){

        for (double[] grid : grids) {

            if ((tempPoint.x >= grid[0] && tempPoint.x < grid[1] &&
                    tempPoint.y >= grid[2] && tempPoint.y < grid[3]))
                return true;

        }

        return false;

    }


    public static class computeMetrics extends Thread{



        public computeMetrics(){



        }

        @Override
        public void run() {



        }

    }


    public class threadInput{

        ArrayList<Double> z;
        ArrayList<Integer> intensity;
        double sum_z;
        double sum_i;
        String suffix;
        ArrayList<String> colnames;

        public threadInput(ArrayList<Double> z, ArrayList<Integer> intensity, double sum_z, double sum_i, String suffix, ArrayList<String> colnames){

            this.z = z;
            this.intensity = intensity;
            this.sum_i = sum_i;
            this.suffix = suffix;
            this.sum_z = sum_z;
            this.colnames = colnames;

        }
    }
}
