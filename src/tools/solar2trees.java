package tools;

import LASio.LASReader;
import LASio.LasPoint;
import utils.KdTree;
import utils.argumentReader;

import java.io.*;
import java.util.Arrays;

public class solar2trees {

    argumentReader aR;
    int coreNumber = 0;
    double[][] trees;
    double[][] trees_2017;

    boolean[] doneTrees;

    double resolution = 0.5;
    double kernel_in_meters = 4;

    float[][][] averageIrradiance, averageClosure;
    int[][][] count;
    File outputFile;
    FileOutputStream fos;
    BufferedWriter bw;

    public solar2trees(argumentReader aR, int coreNumber) throws Exception {

        outputFile = new File("solar2trees.txt");
        fos = new FileOutputStream(outputFile);
        bw = new BufferedWriter(new OutputStreamWriter(fos));

        bw.write("plot_id\ttree_id\taverage_irradiance\taverage_closure\tdiameter_2020\theight_2020\tspecies\tvolume\tdiameter_2017\theight_2017\tvolume_2017");
        bw.newLine();

        this.aR = aR;

    }

    public void closeOutputStream() throws Exception{
        this.bw.close();
    }

    public void processPointCloud(LASReader pointCloud) throws Exception{

        int kernel_div_2 = (int)Math.ceil (( kernel_in_meters / 2.0 - this.resolution / 2.0 ) / this.resolution);

        double minx = pointCloud.getMinX();
        double maxx = pointCloud.getMaxX();

        double miny = pointCloud.getMinY();
        double maxy = pointCloud.getMaxY();

        double minz = pointCloud.getMinY();
        double maxz = pointCloud.getMaxY();

        prep(pointCloud);

        for(int i = 0; i < trees.length; i++){

            if(!doneTrees[i]){

                double x = trees[i][0];
                double y = trees[i][1];
                double z = trees[i][2];

                if(isTreeInsidePointCloud(x, y, pointCloud)){

                    int x_coord = (int)((x - minx) / this.resolution);
                    int y_coord = (int)((maxy - y) / this.resolution);
                    int z_coord = (int)(z / resolution);

                    double average_irradiance = 0;
                    double average_closure = 0;
                    int counter = 0;

                    for(int x_ = (x_coord - kernel_div_2); x_ <= (x_coord + kernel_div_2); x_++){
                        for(int y_ = (y_coord - kernel_div_2); y_ <= (y_coord + kernel_div_2); y_++) {
                            for(int z_ = 1; z_ <= z_coord; z_++) {

                                if(averageIrradiance[x_][y_][z_] > 0){
                                    counter++;
                                    average_irradiance += (averageIrradiance[x_][y_][z_] / count[x_][y_][z_]);
                                    average_closure += (averageClosure[x_][y_][z_] / count[x_][y_][z_]);
                                }
                            }
                        }
                    }

                    //bw.write("\tplot_id\ttree_id\taverage_irradiance\taverage_closure\tdiameter\theight\tspecies\tvolume");

                    String out = trees[i][7] + "\t" + trees[i][8] + "\t" + (average_irradiance / (double)counter) + "\t" + (average_closure / (double)counter) +
                            "\t" + trees[i][3] + "\t" + trees[i][2] + "\t" + trees[i][4] + "\t" + trees[i][6] + "\t" + trees_2017[i][3] + "\t" + trees_2017[i][2] + "\t" + trees_2017[i][6];
                    //System.out.println("HERE!! " + (average_irradiance / (double)counter));
                    //String outputString = trees[i][]
                    System.out.println(out);
                    bw.write(out);
                    bw.newLine();
                    //bw.write();
                }
            }
        }
        //bw.close();
    }

    public boolean isTreeInsidePointCloud(double x, double y, LASReader pointCloud){

        if(x > pointCloud.getMinX() && x < pointCloud.getMaxX() &&
                y < pointCloud.getMaxY() && y > pointCloud.getMinY())
            return true;

        return false;
    }

    public void prep(LASReader pointCloud) throws Exception{

        LasPoint tempPoint = new LasPoint();

        double minx = pointCloud.getMinX();
        double maxx = pointCloud.getMaxX();

        double miny = pointCloud.getMinY();
        double maxy = pointCloud.getMaxY();

        double minz = pointCloud.getMinZ();
        double maxz = pointCloud.getMaxZ();


        int x_dim = (int)Math.ceil((maxx - minx) / this.resolution);
        int y_dim = (int)Math.ceil((maxy - miny) / this.resolution);
        int z_dim = (int)Math.ceil((maxz - minz) / this.resolution);

        this.averageClosure = new float[x_dim][y_dim][z_dim];
        this.averageIrradiance = new float[x_dim][y_dim][z_dim];
        this.count = new int[x_dim][y_dim][z_dim];

        int thread_n = aR.pfac.addReadThread(pointCloud);


        for (int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                int x_coord = (int)((tempPoint.x - minx) / this.resolution);
                int y_coord = (int)((maxy - tempPoint.y) / this.resolution);
                int z_coord = (int)((tempPoint.z - minz) / this.resolution);

                //System.out.println(x_coord + " " + y_coord + " " + z_coord);
                count[x_coord][y_coord][z_coord]++;
                averageClosure[x_coord][y_coord][z_coord] += tempPoint.pointSourceId;
                averageIrradiance[x_coord][y_coord][z_coord] += tempPoint.intensity;
                //System.out.println(tempPoint.intensity);
            }
        }
    }

    public void readMeasuredTrees(File measuredTreesFile) throws IOException{

        int treeCount = 0;

        int lineCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {

            String line;

            while ((line = br.readLine()) != null) {

                if(lineCount != 0)
                    treeCount++;

                lineCount++;

            }
        }

        double[][] trees = new double[treeCount][10];

        treeCount = 0;
        lineCount = 0;



        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {

            String line;

            while ((line = br.readLine()) != null) {

                /** SKIP FIRST LINE */
                if(lineCount != 0){
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0){


                    /* Tree x*/
                    trees[treeCount][0] = Double.parseDouble(line.split(",")[3]);

                    /* Tree y*/
                    trees[treeCount][1] = Double.parseDouble(line.split(",")[4]);

                    /* Tree height*/
                    trees[treeCount][2] = Double.parseDouble(line.split(",")[11]);

                    /* Tree diameter*/
                    trees[treeCount][3] = Double.parseDouble(line.split(",")[9]);

                    /* Tree species*/
                    trees[treeCount][4] = Math.min(Double.parseDouble(line.split(",")[7]), 3.0);

                    /* Tree WHAT?*/
                    trees[treeCount][5] = 0.0;

                    /* Tree volume*/
                    trees[treeCount][6] = Double.parseDouble(line.split(",")[42]);

                    /* Tree plot-id*/
                    trees[treeCount][7] = Double.parseDouble(line.split(",")[2]);

                    /* Tree id*/
                    trees[treeCount][8] = Double.parseDouble(line.split(",")[1]);

                    KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(0,0,0);
                    tempTreePoint.setX(trees[treeCount][0]);
                    tempTreePoint.setY(trees[treeCount][1]);
                    tempTreePoint.setZ(trees[treeCount][2]);
                    tempTreePoint.setIndex(treeCount);

                    //kd_tree2.add(tempTreePoint);

                    treeCount++;

                }

                lineCount++;
            }
        }

        //this.setTreeBank(trees.clone());
        this.trees = trees.clone();
        this.doneTrees = new boolean[trees.length];

    }

    public void readMeasuredTrees_2(File measuredTreesFile) throws IOException{

        int treeCount = 0;

        int lineCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {

            String line;

            while ((line = br.readLine()) != null) {

                if(lineCount != 0)
                    treeCount++;

                lineCount++;

            }
        }

        double[][] trees = new double[treeCount][10];

        treeCount = 0;
        lineCount = 0;



        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {

            String line;

            while ((line = br.readLine()) != null) {

                /** SKIP FIRST LINE */
                if(lineCount != 0){
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0){


                    /* Tree x*/
                    trees[treeCount][0] = Double.parseDouble(line.split(",")[3]);

                    /* Tree y*/
                    trees[treeCount][1] = Double.parseDouble(line.split(",")[4]);

                    /* Tree height*/
                    trees[treeCount][2] = Double.parseDouble(line.split(",")[11]);

                    /* Tree diameter*/
                    trees[treeCount][3] = Double.parseDouble(line.split(",")[9]);

                    /* Tree species*/
                    trees[treeCount][4] = Math.min(Double.parseDouble(line.split(",")[7]), 3.0);

                    /* Tree WHAT?*/
                    trees[treeCount][5] = 0.0;

                    /* Tree volume*/
                    trees[treeCount][6] = Double.parseDouble(line.split(",")[42]);

                    /* Tree plot-id*/
                    trees[treeCount][7] = Double.parseDouble(line.split(",")[2]);

                    /* Tree id*/
                    trees[treeCount][8] = Double.parseDouble(line.split(",")[1]);

                    KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(0,0,0);
                    tempTreePoint.setX(trees[treeCount][0]);
                    tempTreePoint.setY(trees[treeCount][1]);
                    tempTreePoint.setZ(trees[treeCount][2]);
                    tempTreePoint.setIndex(treeCount);

                    //kd_tree2.add(tempTreePoint);

                    treeCount++;

                }

                lineCount++;
            }
        }

        //this.setTreeBank(trees.clone());
        this.trees_2017 = trees.clone();
        this.doneTrees = new boolean[trees.length];

    }


}
