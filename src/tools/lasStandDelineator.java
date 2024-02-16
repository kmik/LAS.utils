package tools;

import LASio.LASReader;
import LASio.LasPoint;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.utils.Polyside;
import utils.QuickHull;
import utils.argumentReader;
import utils.pointCloudMetrics;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.awt.*;

import static org.tinfour.utils.Polyside.isPointInPolygon;


public class lasStandDelineator {

    public AtomicInteger count = new AtomicInteger(0);
    argumentReader aR;
    LASReader pointCloud;

    int[][] n_points;

    //float[][][] grid;

    cellStats[][] grid;

    pointCloudMetrics pCM;

    public lasStandDelineator(LASReader pointCloud, argumentReader aR) throws Exception{

        this.aR = aR;
        this.pointCloud = pointCloud;

        this.pCM = new pointCloudMetrics(this.aR);

        createGrid();
    }

    public void createGrid() throws Exception{

        long tStart = System.currentTimeMillis();


        double minx = pointCloud.getMinX();
        double maxx = pointCloud.getMaxX();
        double miny = pointCloud.getMinY();
        double maxy = pointCloud.getMaxY();

        double cell_size = aR.res;

        int grid_x_size = (int)Math.ceil((pointCloud.maxX - minx) / cell_size);
        int grid_y_size = (int)Math.ceil((maxy - pointCloud.minY) / cell_size);

        n_points = new int[grid_x_size][grid_y_size];
        //grid = new float[grid_x_size][grid_y_size][5];
        grid = new cellStats[grid_x_size][grid_y_size];



        long n = pointCloud.getNumberOfPointRecords();
        LasPoint tempPoint = new LasPoint();

        FileWriter writer_a = null;
        FileWriter writer_f = null;
        FileWriter writer_l = null;
        FileWriter writer_i = null;

        File outputMetricFile_a = null;
        File outputMetricFile_f = null;
        File outputMetricFile_l = null;
        File outputMetricFile_i = null;
        outputMetricFile_a = this.aR.createOutputFileWithExtension(pointCloud, "_gridStats_all_echoes.txt");
        outputMetricFile_l = this.aR.createOutputFileWithExtension(pointCloud, "_gridStats_last_and_only_echoes.txt");
        outputMetricFile_f = this.aR.createOutputFileWithExtension(pointCloud, "_gridStats_first_and_only_echoes.txt");
        outputMetricFile_i = this.aR.createOutputFileWithExtension(pointCloud, "_gridStats_intermediate_echoes.txt");

        int thread_n = aR.pfac.addReadThread(pointCloud);

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                int x_coord = (int) Math.floor((tempPoint.x - minx) /  cell_size);   //X INDEX
                int y_coord = (int) Math.floor((maxy - tempPoint.y) / cell_size);

                n_points[x_coord][y_coord]++;

            }
        }

        writer_a = new FileWriter(outputMetricFile_a);
        writer_f = new FileWriter(outputMetricFile_f);
        writer_l = new FileWriter(outputMetricFile_l);
        writer_i = new FileWriter(outputMetricFile_i);

        boolean first = true;


        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                int x_coord = (int) Math.floor((tempPoint.x - minx) /  cell_size);   //X INDEX
                int y_coord = (int) Math.floor((maxy - tempPoint.y) / cell_size);

                if(grid[x_coord][y_coord] == null){
                    grid[x_coord][y_coord] = new cellStats(y_coord * grid_x_size + x_coord);

                }
                grid[x_coord][y_coord].addPoint(tempPoint);

                n_points[x_coord][y_coord]--;

                if(n_points[x_coord][y_coord] == 0){

                    grid[x_coord][y_coord].setMetricsComputator(pCM);

                    grid[x_coord][y_coord].computeMetrics();

                    if(first) {

                        writer_a.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");
                        writer_f.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");
                        writer_l.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");
                        writer_i.write("Grid_cell_id\tplot_id\tdiv\twhat\tarea\tx_coord\ty_coord\t");

                        for (int i_ = 0; i_ < grid[x_coord][y_coord].colnamesit.get(0).size(); i_++) {

                            writer_a.write(grid[x_coord][y_coord].colnamesit.get(0).get(i_) + "\t");
                            writer_l.write(grid[x_coord][y_coord].colnamesit.get(2).get(i_) + "\t");
                            writer_i.write(grid[x_coord][y_coord].colnamesit.get(3).get(i_) + "\t");

                        }

                        /* First and only contain RGB, so we need to do it separately */
                        for (int i_ = 0; i_ < grid[x_coord][y_coord].colnamesit.get(1).size(); i_++) {

                            writer_f.write(grid[x_coord][y_coord].colnamesit.get(1).get(i_) + "\t");

                        }

                        writer_a.write("\n");
                        writer_f.write("\n");
                        writer_l.write("\n");
                        writer_i.write("\n");

                        first = false;

                    }

                    double x_coord_ = minx + cell_size * x_coord;
                    double y_coord_ = maxy - cell_size * y_coord;

                    for(int p = 0; p < grid[x_coord][y_coord].ids.size(); p++) {

                        writer_a.write(grid[x_coord][y_coord].getId() + "\t" + grid[x_coord][y_coord].ids.get(p) + "\t" + "0" + "\t" + "0" + "\t" + grid[x_coord][y_coord].areas.get(p) + "\t" + x_coord_ + "\t" + y_coord_ + "\t");
                        writer_f.write(grid[x_coord][y_coord].getId() + "\t" + grid[x_coord][y_coord].ids.get(p) + "\t" + "0" + "\t" + "0" + "\t" + grid[x_coord][y_coord].areas.get(p) + "\t" + x_coord_ + "\t" + y_coord_ + "\t");
                        writer_l.write(grid[x_coord][y_coord].getId() + "\t" + grid[x_coord][y_coord].ids.get(p) + "\t" + "0" + "\t" + "0" + "\t" + grid[x_coord][y_coord].areas.get(p) + "\t" + x_coord_ + "\t" + y_coord_ + "\t");
                        writer_i.write(grid[x_coord][y_coord].getId() + "\t" + grid[x_coord][y_coord].ids.get(p) + "\t" + "0" + "\t" + "0" + "\t" + grid[x_coord][y_coord].areas.get(p) + "\t" + x_coord_ + "\t" + y_coord_ + "\t");

                        for (int i_ = 0; i_ < grid[x_coord][y_coord].output_a.get(p).size(); i_++) {
                            writer_a.write(grid[x_coord][y_coord].output_a.get(p).get(i_) + "\t");

                        }
                        writer_a.write("\n");
                        for (int i_ = 0; i_ <grid[x_coord][y_coord].output_f.get(p).size(); i_++) {
                            writer_f.write(grid[x_coord][y_coord].output_f.get(p).get(i_) + "\t");

                        }
                        writer_f.write("\n");
                        for (int i_ = 0; i_ < grid[x_coord][y_coord].output_l.get(p).size(); i_++) {
                            writer_l.write(grid[x_coord][y_coord].output_l.get(p).get(i_) + "\t");

                        }
                        writer_l.write("\n");
                        for (int i_ = 0; i_ < grid[x_coord][y_coord].output_i.get(p).size(); i_++) {
                            writer_i.write(grid[x_coord][y_coord].output_i.get(p).get(i_) + "\t");

                        }
                        writer_i.write("\n");

                    }

                    grid[x_coord][y_coord].dispose();
                    grid[x_coord][y_coord] = null;
                }

            }
        }


        writer_a.close();
        writer_f.close();
        writer_l.close();
        writer_i.close();



        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;

        long minutes = (tDelta / 1000)  / 60;
        int seconds = (int)((tDelta / 1000) % 60);

        System.out.println("TOOK: " + minutes + " min " + seconds + " sec");
    }



}

class cellStats{

    ArrayList<IncrementalTin> tins = new ArrayList<>();
    ArrayList<Short> ids = new ArrayList<>();
    ArrayList<List<IQuadEdge>> tin_perimeters = new ArrayList<>();
    Vertex tempV;

    Set<Short> foundStands = new HashSet<>();


    ArrayList<ArrayList<Double>> gridPoints_z_a = new ArrayList<>();
    ArrayList<ArrayList<Integer>> gridPoints_i_a = new ArrayList<>();

    ArrayList<ArrayList<Double>> gridPoints_z_f = new ArrayList<>();
    ArrayList<ArrayList<Integer>> gridPoints_i_f = new ArrayList<>();

    ArrayList<ArrayList<Double>> gridPoints_z_l = new ArrayList<>();
    ArrayList<ArrayList<Integer>> gridPoints_i_l = new ArrayList<>();

    ArrayList<ArrayList<Double>> gridPoints_z_i = new ArrayList<>();
    ArrayList<ArrayList<Integer>> gridPoints_i_i = new ArrayList<>();

    ArrayList<ArrayList<int[]>> gridPoints_RGB_f = new ArrayList<>();

    ArrayList<ArrayList<ConcaveHull.Point>> points = new ArrayList<>();

    HashMap<Short, Integer> order = new HashMap<>();


    ArrayList<Double> sum_z_a = new ArrayList<>();
    ArrayList<Double> sum_i_a = new ArrayList<>();
    ArrayList<Double> sum_z_f = new ArrayList<>();
    ArrayList<Double> sum_i_f = new ArrayList<>();
    ArrayList<Double> sum_z_l = new ArrayList<>();
    ArrayList<Double> sum_i_l = new ArrayList<>();
    ArrayList<Double> sum_z_i = new ArrayList<>();
    ArrayList<Double> sum_i_i = new ArrayList<>();

    public ArrayList<Double> areas = new ArrayList<>();


    public ArrayList<ArrayList<String>> colnamesit = new ArrayList<>();

    ArrayList<Double> metrics = new ArrayList<>();

    public int id;

    public ArrayList<ArrayList<Double>> output_a = new ArrayList<>();
    public ArrayList<ArrayList<Double>> output_f = new ArrayList<>();
    public ArrayList<ArrayList<Double>> output_l = new ArrayList<>();
    public ArrayList<ArrayList<Double>> output_i = new ArrayList<>();

    pointCloudMetrics pCM;


    public cellStats(int id){

        this.id = id;

    }

    public int getId(){
        return this.id;
    }

    public void setMetricsComputator(pointCloudMetrics pCM){
        this.pCM = pCM;
    }

    public void addPoint(LasPoint tempPoint){

        if(!foundStands.contains(tempPoint.pointSourceId)) {

            //tins.add(new IncrementalTin());
            //tin_perimeters.add(null);

            ids.add(tempPoint.pointSourceId);

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

            order.put(tempPoint.pointSourceId, gridPoints_z_a.size() - 1);
            foundStands.add(tempPoint.pointSourceId);
        }

        /*
        if(tins.get(order.get(tempPoint.pointSourceId)).isBootstrapped()) {

            if(tin_perimeters.get(order.get(tempPoint.pointSourceId)) == null){
                tin_perimeters.set(order.get(tempPoint.pointSourceId), tins.get(order.get(tempPoint.pointSourceId)).getPerimeter());
            }

            if (isPointInPolygon(tin_perimeters.get(order.get(tempPoint.pointSourceId)), tempPoint.x, tempPoint.y) == Polyside.Result.Outside) {
                //System.out.println(tin_perimeters.get(order.get(tempPoint.pointSourceId)).size());
                tempV = new Vertex(tempPoint.x, tempPoint.y, 0.0);
                //perim.add(i);
                //tempV.setIndex(p);
                tins.get(order.get(tempPoint.pointSourceId)).add(tempV);
                tin_perimeters.set(order.get(tempPoint.pointSourceId), tins.get(order.get(tempPoint.pointSourceId)).getPerimeter());

            }
        }else{
            tempV = new Vertex(tempPoint.x, tempPoint.y, 0.0);
            //perim.add(i);
            //tempV.setIndex(p);
            tins.get(order.get(tempPoint.pointSourceId)).add(tempV);

        }
*/


        points.get(order.get(tempPoint.pointSourceId)).add(new ConcaveHull.Point(tempPoint.x, tempPoint.y));

        gridPoints_z_a.get(order.get(tempPoint.pointSourceId)).add(tempPoint.z);
        gridPoints_i_a.get(order.get(tempPoint.pointSourceId)).add(tempPoint.intensity);

        sum_z_a.set(order.get(tempPoint.pointSourceId), sum_z_a.get(order.get(tempPoint.pointSourceId)) + tempPoint.z);
        sum_i_a.set(order.get(tempPoint.pointSourceId), sum_i_a.get(order.get(tempPoint.pointSourceId)) + tempPoint.intensity);

        //System.out.println("intensity" + tempPoint.intensity);

        if (tempPoint.returnNumber == 1) {
                                        /*
                                        sum_z_f += tempPoint.z;
                                        sum_i_f += tempPoint.intensity;

                                        gridPoints_z_f.add(tempPoint.z);
                                        gridPoints_i_f.add(tempPoint.intensity);

                                         */
            gridPoints_z_f.get(order.get(tempPoint.pointSourceId)).add(tempPoint.z);

            gridPoints_RGB_f.get(order.get(tempPoint.pointSourceId)).add(new int[]{tempPoint.R, tempPoint.G, tempPoint.B});

            gridPoints_i_f.get(order.get(tempPoint.pointSourceId)).add(tempPoint.intensity);

            sum_z_f.set(order.get(tempPoint.pointSourceId), sum_z_f.get(order.get(tempPoint.pointSourceId)) + tempPoint.z);
            sum_i_f.set(order.get(tempPoint.pointSourceId), sum_i_f.get(order.get(tempPoint.pointSourceId)) + tempPoint.intensity);

        }
        if (tempPoint.returnNumber == tempPoint.numberOfReturns) {
/*
                                        sum_z_l += tempPoint.z;
                                        sum_i_l += tempPoint.intensity;

                                        gridPoints_z_l.add(tempPoint.z);
                                        gridPoints_i_l.add(tempPoint.intensity);

 */
            gridPoints_z_l.get(order.get(tempPoint.pointSourceId)).add(tempPoint.z);
            gridPoints_i_l.get(order.get(tempPoint.pointSourceId)).add(tempPoint.intensity);

            sum_z_l.set(order.get(tempPoint.pointSourceId), sum_z_l.get(order.get(tempPoint.pointSourceId)) + tempPoint.z);
            sum_i_l.set(order.get(tempPoint.pointSourceId), sum_i_l.get(order.get(tempPoint.pointSourceId)) + tempPoint.intensity);

        }
        if (tempPoint.returnNumber > 1 && tempPoint.returnNumber != tempPoint.numberOfReturns) {
/*
                                        sum_z_i += tempPoint.z;
                                        sum_i_i += tempPoint.intensity;
                                        gridPoints_z_i.add(tempPoint.z);
                                        gridPoints_i_i.add(tempPoint.intensity);
 */
            gridPoints_z_i.get(order.get(tempPoint.pointSourceId)).add(tempPoint.z);
            gridPoints_i_i.get(order.get(tempPoint.pointSourceId)).add(tempPoint.intensity);

            sum_z_i.set(order.get(tempPoint.pointSourceId), sum_z_i.get(order.get(tempPoint.pointSourceId)) + tempPoint.z);
            sum_i_i.set(order.get(tempPoint.pointSourceId), sum_i_i.get(order.get(tempPoint.pointSourceId)) + tempPoint.intensity);

        }


/*
        gridPoints_z_a.add(tempPoint.z);
        gridPoints_i_a.add(tempPoint.intensity);

        sum_z_a += tempPoint.z;
        sum_i_a += tempPoint.intensity;

        if (tempPoint.returnNumber == 1) {

            gridPoints_z_f.add(tempPoint.z);
            gridPoints_i_f.add(tempPoint.intensity);

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
*/
    }

    public void computeMetrics(){


        ArrayList<String> colnames = new ArrayList<>();
/*
        for(int ii = 0; ii < gridPoints_z_a.size(); ii++) {

            areas.add(tins.get(ii).countTriangles().getAreaSum());
            tins.get(ii).clear();
            tins.get(ii).dispose();
            tins.set(ii, new IncrementalTin());
            tin_perimeters.set(ii, null);

        }
        */

        for(int ii = 0; ii < gridPoints_z_a.size(); ii++) {

            tools.QuickHull qh = new tools.QuickHull();
            ArrayList<ConcaveHull.Point> p = null;

            if(points.get(ii).size() > 3) {
                p = qh.quickHull(points.get(ii));
                areas.add(polygonArea(p));
            }else{
                areas.add(-99.9);
            }
        }

        //System.gc();

        colnamesit.clear();
       // System.out.println(sum_z_a + " " + sum_z_i);
        ArrayList<ArrayList<Double>> output = new ArrayList<>();

        /* Iterate over all the plot ids within this grid cell */
        for(int ii = 0; ii < gridPoints_z_a.size(); ii++) {

            metrics = pCM.calc(gridPoints_z_a.get(ii), gridPoints_i_a.get(ii), sum_z_a.get(ii), sum_i_a.get(ii), "_a", colnames);
            output_a.add(metrics);
            colnamesit.add((ArrayList<String>)colnames.clone());

        }

        for(int ii = 0; ii < gridPoints_z_f.size(); ii++) {

            metrics = pCM.calc_with_RGB(gridPoints_z_f.get(ii), gridPoints_i_f.get(ii), sum_z_f.get(ii), sum_i_f.get(ii), "_f", colnames, gridPoints_RGB_f.get(ii));
            output_f.add(metrics);
            colnamesit.add((ArrayList<String>)colnames.clone());

            //System.out.println(Arrays.toString(metrics.toArray()));

        }
        for(int ii = 0; ii < gridPoints_z_l.size(); ii++) {
            metrics = pCM.calc(gridPoints_z_l.get(ii), gridPoints_i_l.get(ii), sum_z_l.get(ii), sum_i_l.get(ii), "_l", colnames);
            output_l.add(metrics);
            colnamesit.add((ArrayList<String>)colnames.clone());



        }
        for(int ii = 0; ii < gridPoints_z_i.size(); ii++) {
            metrics = pCM.calc(gridPoints_z_i.get(ii), gridPoints_i_i.get(ii), sum_z_i.get(ii), sum_i_i.get(ii), "_i", colnames);
            output_i.add(metrics);
            colnamesit.add((ArrayList<String>)colnames.clone());

        }

    }

    public void dispose(){

        gridPoints_i_a.clear();
        gridPoints_i_f.clear();
        gridPoints_i_i.clear();

        gridPoints_z_a.clear();
        gridPoints_z_f.clear();
        gridPoints_z_i.clear();


    }

    // (X[i], Y[i]) are coordinates of i'th point.
    public static double polygonArea(ArrayList<ConcaveHull.Point> points)
    {
        // Initialize area
        double area = 0.0;

        // Calculate value of shoelace formula
        int j = points.size() - 1;
        for (int i = 0; i < points.size(); i++)
        {
            area += (points.get(j).x + points.get(i).x) * (points.get(j).y - points.get(i).y);

            // j is previous vertex to i
            j = i;
        }

        // Return absolute value
        return Math.abs(area / 2.0);
    }
}
