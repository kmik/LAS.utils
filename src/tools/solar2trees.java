package tools;

import LASio.LASReader;
import LASio.LasPoint;
import org.tinfour.common.Vertex;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.interpolation.VertexValuatorDefault;
import org.tinfour.standard.IncrementalTin;
import utils.KdTree;
import utils.argumentReader;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static tools.ITDstatistics.euclideanDistance;

public class solar2trees {

    IncrementalTin tin = new IncrementalTin();

    argumentReader aR;
    int coreNumber = 0;
    double[][] trees;
    double[][] trees_2017;

    boolean[] doneTrees;

    double resolution = 0.5;
    double kernel_in_meters = 4;

    float[][][] averageIrradiance, averageClosure;
    byte[][][] averageScanAngle;
    int[][][] count;
    File outputFile;
    FileOutputStream fos;
    BufferedWriter bw;
    int x_dim, y_dim, z_dim;

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

        //System.out.println(kernel_div_2);
        double minx = pointCloud.getMinX();
        double maxx = pointCloud.getMaxX();

        double miny = pointCloud.getMinY();
        double maxy = pointCloud.getMaxY();

        double minz = pointCloud.getMinZ();
        double maxz = pointCloud.getMaxZ();

        prep(pointCloud);

        tin.clear();
        tin = new IncrementalTin();
        createTin(2, pointCloud);

        TriangularFacetInterpolator polator = new TriangularFacetInterpolator(this.tin);
        VertexValuatorDefault valuator = new VertexValuatorDefault();


        for(int i = 0; i < trees.length; i++){

            if(!doneTrees[i]){

                double x = trees[i][0];
                double y = trees[i][1];
                double z = trees[i][2];

                if(isTreeInsidePointCloud(x, y, pointCloud)){

                    int x_coord = (int)((x - minx) / this.resolution);
                    int y_coord = (int)((maxy - y) / this.resolution);

                    double tin_value = polator.interpolate(x, y, valuator);

                    int z_coord = (int)((z + tin_value - minz) / this.resolution);

                    //System.out.println(z_coord + " " + this.z_dim + " " + z);
                    double average_irradiance = 0;
                    double average_closure = 0;
                    double average_scan_angle = 0;
                    int counter = 0;

                    for(int x_ = (x_coord - kernel_div_2); x_ <= (x_coord + kernel_div_2); x_++){
                        for(int y_ = (y_coord - kernel_div_2); y_ <= (y_coord + kernel_div_2); y_++) {
                            for(int z_ = (int)(z_coord / 2.0); z_ <= z_coord; z_++) {

                                if(z_coord < this.z_dim && y_coord < this.y_dim && x_coord < this.x_dim
                                && z_coord > 0 && y_coord > 0 && x_coord > 0)
                                    if(count[x_][y_][z_] > 0){
                                        counter++;
                                        average_irradiance += (averageIrradiance[x_][y_][z_] / count[x_][y_][z_]);
                                        average_closure += (averageClosure[x_][y_][z_] / count[x_][y_][z_]);
                                        average_scan_angle += ((int)averageScanAngle[x_][y_][z_] / count[x_][y_][z_]);
                                    }
                            }
                        }
                    }

                    //System.out.println(counter);
                    //bw.write("\tplot_id\ttree_id\taverage_irradiance\taverage_closure\tdiameter\theight\tspecies\tvolume");

                    String out = trees[i][7] + "\t" + trees[i][8] + "\t" + (average_irradiance / (double)counter) + "\t" + (average_closure / (double)counter) +
                            "\t" + trees[i][3] + "\t" + trees[i][2] + "\t" + trees[i][4] + "\t" + trees[i][6] + "\t" + trees_2017[i][3] + "\t" + trees_2017[i][2] + "\t" + trees_2017[i][6];
                    //System.out.println("HERE!! " + (average_irradiance / (double)counter));
                    //String outputString = trees[i][]
                    //System.out.println(out);
                    bw.write(out);
                    bw.newLine();
                    //bw.write();
                }else{

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


        this.x_dim = (int)Math.ceil((maxx - minx) / this.resolution);
        this.y_dim = (int)Math.ceil((maxy - miny) / this.resolution);
        this.z_dim = (int)Math.ceil((maxz - minz) / this.resolution);

        this.averageClosure = new float[x_dim][y_dim][z_dim];
        this.averageIrradiance = new float[x_dim][y_dim][z_dim];
        this.averageScanAngle = new byte[x_dim][y_dim][z_dim];

        this.count = new int[x_dim][y_dim][z_dim];

        int thread_n = aR.pfac.addReadThread(pointCloud);


        for (int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000) {

            int maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(tempPoint.returnNumber == 1){
                    int x_coord = (int)((tempPoint.x - minx) / this.resolution);
                    int y_coord = (int)((maxy - tempPoint.y) / this.resolution);
                    int z_coord = (int)((tempPoint.z - minz) / this.resolution);

                    //System.out.println(x_coord + " " + y_coord + " " + z_coord);
                    count[x_coord][y_coord][z_coord]++;
                    averageClosure[x_coord][y_coord][z_coord] += tempPoint.pointSourceId;
                    averageIrradiance[x_coord][y_coord][z_coord] += tempPoint.intensity;
                    averageScanAngle[x_coord][y_coord][z_coord] += Math.abs(tempPoint.scanAngleRank);

                    //System.out.println(tempPoint.intensity);
                }
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


    public void readMeasuredTrees_(File measuredTreesFile) throws IOException{

        int treeCount = 0;

        int lineCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {
            String line;
            while ((line = br.readLine()) != null) {

                if(lineCount != 0)
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0)
                    treeCount++;

                lineCount++;

            }
        }

        double[][] trees = new double[treeCount][10];

        treeCount = 0;
        lineCount = 0;

        HashMap<String, Integer> column_name_to_index = new HashMap<>();


        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.replaceAll("\"", "");



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

                    treeCount++;

                }else{

                    String[] header = line.split(",");

                    HashMap<String, HashSet<String>> correctNames = new HashMap<>();

                    correctNames.put("diameter", new HashSet<>(Arrays.asList("diameter", "Diameter", "d", "D", "tree_diameter")));
                    correctNames.put("height", new HashSet<>(Arrays.asList("height", "Height", "h", "H", "tree_height")));
                    correctNames.put("species", new HashSet<>(Arrays.asList("species", "Species", "s", "S", "tree_species")));
                    correctNames.put("x", new HashSet<>(Arrays.asList("tree_x", "x", "X", "x_coordinate")));
                    correctNames.put("y", new HashSet<>(Arrays.asList("tree_y", "y", "Y", "y_coordinate")));
                    correctNames.put("volume", new HashSet<>(Arrays.asList("volume", "Volume", "v", "V", "tree_volume", "vtot")));
                    correctNames.put("plot_id", new HashSet<>(Arrays.asList( "plot_id", "Plot_id", "plot", "Plot", "p", "P")));
                    correctNames.put("tree_id", new HashSet<>(Arrays.asList("tree_id", "Tree_id", "tree", "Tree", "t", "T")));
                    correctNames.put("tree_plot_id", new HashSet<>(Arrays.asList("tree_plot_id", "Tree_plot_id")));



                    for(int i = 0; i < header.length; i++){

                        if(correctNames.containsKey("diameter"))
                            if(correctNames.get("diameter").contains(header[i])){
                                column_name_to_index.put("diameter", i);
                                correctNames.remove("diameter");
                            }

                        if(correctNames.containsKey("height"))

                            if(correctNames.get("height").contains(header[i])){
                                column_name_to_index.put("height", i);
                                correctNames.remove("height");
                            }

                        if(correctNames.containsKey("species"))

                            if(correctNames.get("species").contains(header[i])){
                                column_name_to_index.put("species", i);
                                correctNames.remove("species");
                            }

                        if(correctNames.containsKey("x"))
                            if(correctNames.get("x").contains(header[i])){
                                column_name_to_index.put("x", i);
                                correctNames.remove("x");
                            }

                        if(correctNames.containsKey("y"))

                            if(correctNames.get("y").contains(header[i])){
                                column_name_to_index.put("y", i);
                                correctNames.remove("y");
                            }

                        if(correctNames.containsKey("volume"))
                            if(correctNames.get("volume").contains(header[i])){
                                column_name_to_index.put("volume", i);
                                correctNames.remove("volume");
                            }

                        if(correctNames.containsKey("plot_id"))

                            if(correctNames.get("plot_id").contains(header[i])){
                                column_name_to_index.put("plot_id", i);
                                correctNames.remove("plot_id");
                            }

                        if(correctNames.containsKey("tree_id"))
                            if(correctNames.get("tree_id").contains(header[i])){
                                column_name_to_index.put("tree_id", i);
                                correctNames.remove("tree_id");
                            }

                        if(correctNames.containsKey("tree_plot_id"))
                            if(correctNames.get("tree_plot_id").contains(header[i])){
                                column_name_to_index.put("tree_plot_id", i);
                                correctNames.remove("tree_plot_id");
                            }



                    }

                    if(correctNames.size() != 0){
                        System.out.println("Could not find the following columns in the measured trees file: ");
                        for(String key : correctNames.keySet()){
                            System.out.println(key);
                            System.out.println("Consider using the following names: ");
                            for(String name : correctNames.get(key)){
                                System.out.println(name);
                            }
                            System.out.println("----------------");
                        }

                    }
                }

                lineCount++;
            }

        }

        this.trees = trees.clone();
        this.doneTrees = new boolean[trees.length];

    }


    public void readMeasuredTrees_2_(File measuredTreesFile) throws IOException{

        int treeCount = 0;

        int lineCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {
            String line;
            while ((line = br.readLine()) != null) {

                if(lineCount != 0)
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0)
                    treeCount++;

                lineCount++;

            }
        }

        double[][] trees = new double[treeCount][10];

        treeCount = 0;
        lineCount = 0;

        HashMap<String, Integer> column_name_to_index = new HashMap<>();


        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.replaceAll("\"", "");



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

                    treeCount++;

                }else{

                    String[] header = line.split(",");

                    HashMap<String, HashSet<String>> correctNames = new HashMap<>();

                    correctNames.put("diameter", new HashSet<>(Arrays.asList("diameter", "Diameter", "d", "D", "tree_diameter")));
                    correctNames.put("height", new HashSet<>(Arrays.asList("height", "Height", "h", "H", "tree_height")));
                    correctNames.put("species", new HashSet<>(Arrays.asList("species", "Species", "s", "S", "tree_species")));
                    correctNames.put("x", new HashSet<>(Arrays.asList("tree_x", "x", "X", "x_coordinate")));
                    correctNames.put("y", new HashSet<>(Arrays.asList("tree_y", "y", "Y", "y_coordinate")));
                    correctNames.put("volume", new HashSet<>(Arrays.asList("volume", "Volume", "v", "V", "tree_volume", "vtot")));
                    correctNames.put("plot_id", new HashSet<>(Arrays.asList( "plot_id", "Plot_id", "plot", "Plot", "p", "P")));
                    correctNames.put("tree_id", new HashSet<>(Arrays.asList("tree_id", "Tree_id", "tree", "Tree", "t", "T")));
                    correctNames.put("tree_plot_id", new HashSet<>(Arrays.asList("tree_plot_id", "Tree_plot_id")));



                    for(int i = 0; i < header.length; i++){

                        if(correctNames.containsKey("diameter"))
                            if(correctNames.get("diameter").contains(header[i])){
                                column_name_to_index.put("diameter", i);
                                correctNames.remove("diameter");
                            }

                        if(correctNames.containsKey("height"))

                            if(correctNames.get("height").contains(header[i])){
                                column_name_to_index.put("height", i);
                                correctNames.remove("height");
                            }

                        if(correctNames.containsKey("species"))

                            if(correctNames.get("species").contains(header[i])){
                                column_name_to_index.put("species", i);
                                correctNames.remove("species");
                            }

                        if(correctNames.containsKey("x"))
                            if(correctNames.get("x").contains(header[i])){
                                column_name_to_index.put("x", i);
                                correctNames.remove("x");
                            }

                        if(correctNames.containsKey("y"))

                            if(correctNames.get("y").contains(header[i])){
                                column_name_to_index.put("y", i);
                                correctNames.remove("y");
                            }

                        if(correctNames.containsKey("volume"))
                            if(correctNames.get("volume").contains(header[i])){
                                column_name_to_index.put("volume", i);
                                correctNames.remove("volume");
                            }

                        if(correctNames.containsKey("plot_id"))

                            if(correctNames.get("plot_id").contains(header[i])){
                                column_name_to_index.put("plot_id", i);
                                correctNames.remove("plot_id");
                            }

                        if(correctNames.containsKey("tree_id"))
                            if(correctNames.get("tree_id").contains(header[i])){
                                column_name_to_index.put("tree_id", i);
                                correctNames.remove("tree_id");
                            }

                        if(correctNames.containsKey("tree_plot_id"))
                            if(correctNames.get("tree_plot_id").contains(header[i])){
                                column_name_to_index.put("tree_plot_id", i);
                                correctNames.remove("tree_plot_id");
                            }



                    }

                    if(correctNames.size() != 0){
                        System.out.println("Could not find the following columns in the measured trees file: ");
                        for(String key : correctNames.keySet()){
                            System.out.println(key);
                            System.out.println("Consider using the following names: ");
                            for(String name : correctNames.get(key)){
                                System.out.println(name);
                            }
                            System.out.println("----------------");
                        }

                    }
                }

                lineCount++;
            }

        }

        this.trees_2017 = trees.clone();
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


    public void createTin(int groundClassification, LASReader pointCloud) throws Exception{

        LasPoint tempPoint = new LasPoint();

        int thread_n = aR.pfac.addReadThread(pointCloud);

        double decimate_res = aR.decimate_tin;

        boolean decimate = decimate_res > 0;
        List<org.tinfour.common.Vertex> closest;
        Vertex closest_vertex;

        boolean tin_is_bootstrapped = false;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000) {

            int maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
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


}
