package tools;

import LASio.LASReader;
import LASio.LASraf;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import err.toolException;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import org.tinfour.utils.Polyside;
import utils.argumentReader;
import utils.pointWriterMultiThread;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.tinfour.utils.Polyside.isPointInPolygon;

public class lasStat {

    org.tinfour.standard.IncrementalTin perimTin = new org.tinfour.standard.IncrementalTin();
    List<org.tinfour.common.IQuadEdge> concaveEdges = null;
    HashSet<Integer> perim = new HashSet<>();

    LASReader pointCloud;

    argumentReader aR;

    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();

    double pulse_density = 0.0;
    double point_density = 0.0;

    public lasStat(LASReader in, argumentReader aR) throws Exception {

        this.pointCloud = in;
        this.aR = aR;

        double[] densities = pointDensity_m2();
        this.pulse_density = densities[0];
        this.point_density = densities[1];

        System.out.println("average pulse density m2: " + pulse_density);
        System.out.println("average point density m2: " + point_density);
    }


    public double[] pointDensity_m2() throws Exception {


        HashSet<Double> donet = new HashSet<Double>();

        List<Vertex> closest = new ArrayList<>();

        double minx = pointCloud.getMinX();
        double maxx = pointCloud.getMaxX();
        double miny = pointCloud.getMinY();
        double maxy = pointCloud.getMaxY();


        double cell_size = 5.0;

        int grid_x_size = (int)Math.ceil((pointCloud.maxX - minx) / cell_size);
        int grid_y_size = (int)Math.ceil((maxy - pointCloud.minY) / cell_size);

        int[][] stats_pulse = new int[grid_x_size][grid_y_size];
        int[][] stats_points = new int[grid_x_size][grid_y_size];


        org.tinfour.interpolation.TriangularFacetInterpolator polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);
        org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

        long n = pointCloud.getNumberOfPointRecords();
        LasPoint tempPoint = new LasPoint();

        if(false)
            if(!pointCloud.isIndexed){
                pointCloud.index(10);
            }

        org.tinfour.common.Vertex tempV;

        List<IQuadEdge> currentBorder = null;

        int thread_n = aR.pfac.addReadThread(pointCloud);

        int number_of_last_returns = 0;
        int number_of_all_returns = 0;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if (!aR.inclusionRule.ask(tempPoint, i + j, true)) {
                    continue;
                }

                int x_coord = (int) Math.floor((tempPoint.x - minx) /  cell_size);   //X INDEX
                int y_coord = (int) Math.floor((maxy - tempPoint.y) / cell_size);


                if (tempPoint.returnNumber == tempPoint.numberOfReturns) {
                    number_of_last_returns++;
                    stats_pulse[x_coord][y_coord]++;
                }

                    number_of_all_returns++;
                    stats_points[x_coord][y_coord]++;

            }
        }


        double approx_area_pulse = 0.0;
        double approx_area_points = 0.0;
        int counter_pulse = 0;
        int counter_points = 0;

        for(int x = 0; x < grid_x_size; x++) {
            for (int y = 0; y < grid_y_size; y++) {

                if(stats_pulse[x][y] > 0){
                    approx_area_pulse += stats_pulse[x][y] / (cell_size*cell_size);
                    counter_pulse++;
                }
                if(stats_points[x][y] > 0){
                    approx_area_points += stats_points[x][y] / (cell_size*cell_size);
                    counter_points++;
                }

            }
        }

        approx_area_pulse /= (double)counter_pulse;
        approx_area_points /= (double)counter_points;
        /*
        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

                if(tempPoint.returnNumber == tempPoint.numberOfReturns){
                    number_of_last_returns++;
                }else{
                    continue;
                }

                if (tin.isBootstrapped()) {

                    if (currentBorder == null) {
                        currentBorder = tin.getPerimeter();
                    }

                    if (isPointInPolygon(currentBorder, tempPoint.x, tempPoint.y) == Polyside.Result.Outside) {

                        tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, 0.0);
                        //perim.add(i);
                        tempV.setIndex(i);
                        tin.add(tempV);
                        currentBorder = tin.getPerimeter();


                    }
                } else {
                    tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, 0.0);
                    //perim.add(i);
                    tempV.setIndex(i);
                    tin.add(tempV);
                }

                if (i % 10000 == 0) {
                    calcPerim();
                }
            }
        }


         */

/*
        for(SimpleTriangle triangle : tin.triangles()){

            approx_area += triangle.getArea();

        }
*/


        //return (double)number_of_last_returns / approx_area;
        return new double[]{approx_area_pulse, approx_area_points};
    }

    public double getPulsePerUnit(){

        return this.pulse_density;

    }

    public double getPointsPerUnit(){

        return this.point_density;

    }

    public void calcPerim(){

        concaveEdges = tin.getPerimeter();

        this.perim.clear();

        for(int i = 0; i < concaveEdges.size(); i++){

            this.perim.add(concaveEdges.get(i).getA().getIndex());
            this.perim.add(concaveEdges.get(i).getB().getIndex());
        }

    }

}
