package tools;

import LASio.LASReader;
import LASio.LasPoint;
import org.tinfour.common.IQuadEdge;
import utils.argumentReader;
import utils.pointCloudMetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class lasStandDelineator {

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

        double minx = pointCloud.getMinX();
        double maxx = pointCloud.getMaxX();
        double miny = pointCloud.getMinY();
        double maxy = pointCloud.getMaxY();

        double cell_size = 1.0;

        int grid_x_size = (int)Math.ceil((pointCloud.maxX - minx) / cell_size);
        int grid_y_size = (int)Math.ceil((maxy - pointCloud.minY) / cell_size);

        n_points = new int[grid_x_size][grid_y_size];
        //grid = new float[grid_x_size][grid_y_size][5];
        grid = new cellStats[grid_x_size][grid_y_size];



        long n = pointCloud.getNumberOfPointRecords();
        LasPoint tempPoint = new LasPoint();


        int thread_n = aR.pfac.addReadThread(pointCloud);

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                int x_coord = (int) Math.floor((tempPoint.x - minx) /  cell_size);   //X INDEX
                int y_coord = (int) Math.floor((maxy - tempPoint.y) / cell_size);

                n_points[x_coord][y_coord]++;

            }
        }


        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                int x_coord = (int) Math.floor((tempPoint.x - minx) /  cell_size);   //X INDEX
                int y_coord = (int) Math.floor((maxy - tempPoint.y) / cell_size);

                if(grid[x_coord][y_coord] == null){
                    grid[x_coord][y_coord] = new cellStats();
                }
                grid[x_coord][y_coord].addPoint(tempPoint);

                n_points[x_coord][y_coord]--;

                if(n_points[x_coord][y_coord] == 0){
                    grid[x_coord][y_coord].setMetricsComputator(pCM);
                    grid[x_coord][y_coord].computeMetrics();
                    grid[x_coord][y_coord].dispose();
                    grid[x_coord][y_coord] = null;
                }

            }
        }



    }


}

class cellStats{

    ArrayList<Double> gridPoints_z_a = new ArrayList<>();
    ArrayList<Integer> gridPoints_i_a = new ArrayList<>();

    ArrayList<Double> gridPoints_z_f = new ArrayList<>();
    ArrayList<Integer> gridPoints_i_f = new ArrayList<>();

    ArrayList<Double> gridPoints_z_l = new ArrayList<>();
    ArrayList<Integer> gridPoints_i_l = new ArrayList<>();

    ArrayList<Double> gridPoints_z_i = new ArrayList<>();
    ArrayList<Integer> gridPoints_i_i = new ArrayList<>();

    ArrayList<int[]> gridPoints_RGB_f = new ArrayList<>();

    double sum_z_a = 0.0;
    double sum_i_a = 0.0;
    double sum_z_f = 0.0;
    double sum_i_f = 0.0;
    double sum_z_l = 0.0;
    double sum_i_l = 0.0;
    double sum_z_i = 0.0;
    double sum_i_i = 0.0;

    ArrayList<Double> metrics = new ArrayList<>();


    pointCloudMetrics pCM;


    public cellStats(){


    }

    public void setMetricsComputator(pointCloudMetrics pCM){
        this.pCM = pCM;
    }

    public void addPoint(LasPoint tempPoint){


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

    }

    public void computeMetrics(){
        ArrayList<String> colnames = new ArrayList<>();

        System.out.println(sum_z_a + " " + sum_z_i);
        metrics = pCM.calc(gridPoints_z_a, gridPoints_i_a, sum_z_a, sum_i_a, "_a", colnames);

        System.out.println(Arrays.toString(metrics.toArray()));
        //System.exit(1);
    }

    public void dispose(){

        gridPoints_i_a.clear();
        gridPoints_i_f.clear();
        gridPoints_i_i.clear();

        gridPoints_z_a.clear();
        gridPoints_z_f.clear();
        gridPoints_z_i.clear();


    }

}
