package tools;

import err.toolException;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import utils.KdTree;
import utils.Polygon;
import utils.argumentReader;

import java.io.*;
import java.util.*;

public class heatMap {


    double res_x = 0.46875;
    double res_y = 0.46875;
    double res_z = 0.46875;

    ArrayList<float[]> labelTree = new ArrayList<float[]>();

    public double[][] treeBank;
    ArrayList<double[][]> polygons = new ArrayList<>();

    ArrayList<Polygon> polys = new ArrayList<Polygon>();

    ArrayList<Double> plotIds;
    argumentReader aR;
    public KdTree kd_tree2 = new KdTree();

    ArrayList<String> plotIdsString = new ArrayList<String>();


    public heatMap() {
        // Constructor logic if needed
    }

    public heatMap(argumentReader aR) {
        this.aR = aR;
    }


    public void readFieldPlots(File plots) throws IOException {

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        if(!plots.exists())
            return;

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
            //layeri.GetGeomType();
            String id = "";

            if(tempF.GetFieldCount() > 0)
                id = tempF.GetFieldAsString(this.aR.field);
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
            System.out.println(Arrays.toString(tokens));

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

        for(int i = 0; i < plotIds.size(); i++){

            Polygon tmpPoly = new Polygon();
            tmpPoly.addOuterRing(polygons.get(i));
            tmpPoly.setId((int)plotIds1.get(i).doubleValue());
            tmpPoly.findCentroid();
            tmpPoly.findBoundingBox();
            polys.add(tmpPoly);

        }



    }


    public void readMeasuredTrees(File measuredTreesFile) throws IOException{

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

                    kd_tree2.add(tempTreePoint);

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


        this.setTreeBank(trees.clone());

    }

    public void createHeatMap(double searchRadius, double nearestNeighborDistance, int numNeighbors, int maxZ) {

        // This method will create the heatmap based on the treeBank and polygons
        // The implementation details will depend on the specific requirements of the heatmap generation
        // For example, it could involve iterating through the treeBank, checking which trees fall within each polygon,
        // and then aggregating the data to create a heatmap representation.

        // Placeholder for heatmap creation logic

        KdTree.XYZPoint point = new KdTree.XYZPoint(0,0,0);

        File outputFile = new File(aR.output);

        if(outputFile.exists())
            outputFile.delete();

        BufferedWriter bw = null;

        try {
            bw = new BufferedWriter(new FileWriter(outputFile));
        }catch(Exception e){
            e.printStackTrace();
        }

        for(int i = 0; i < polys.size(); i++){

            double centerX = polys.get(i).getCentroid()[0];
            double centerY = polys.get(i).getCentroid()[1];

            int x_dim = (int)Math.ceil((polys.get(i).getMaxX() - polys.get(i).getMinX()) / res_x);
            int y_dim = (int)Math.ceil((polys.get(i).getMaxY() - polys.get(i).getMinY()) / res_y);
            int z_dim = (int)Math.ceil(maxZ / res_z);

            System.out.println("Polygon " + i + ": Center = (" + centerX + ", " + centerY + "), Dimensions = (" + x_dim + ", " + y_dim + ", " + z_dim + ")");

            point.setX(centerX);
            point.setY(centerY);
            point.setZ(0); // Assuming Z is not used for heatmap generation


            List<KdTree.XYZPoint> nearestTreesAll = (List<KdTree.XYZPoint>)kd_tree2.nearestNeighbourSearch(100, point);
            KdTree tree2 = new KdTree();

            int numtrees = 0;

            for(KdTree.XYZPoint tree : nearestTreesAll){
                /*
                System.out.println("Checking tree: " + tree.getX() + ", " + tree.getY() + ", " + tree.getZ());
                System.out.println("polygon minX: " + polys.get(i).getMinX() + ", maxX: " + polys.get(i).getMaxX());
                System.out.println("polygon minY: " + polys.get(i).getMinY() + ", maxY: " + polys.get(i).getMaxY());
                System.out.println("Polygon centroid: " + polys.get(i).getCentroid()[0] + ", " + polys.get(i).getCentroid()[1]);
                System.out.println("polygon id: " + polys.get(i).getId());

                System.out.println("distance to center: " + point.euclideanDistance(tree));
                System.out.println("Tree coordinates: " + tree.getX() + ", " + tree.getY() + ", " + tree.getZ());

                 */

                if(polys.get(i).pointInPolygon(new double[]{tree.getX(), tree.getY()})){
                    //System.out.println("Added tree: " + tree.getX() + ", " + tree.getY() + ", " + tree.getZ());
                    tree2.add(tree);
                    numtrees++;
                }
            }

            double[][][] grid = new double[x_dim][y_dim][z_dim];

            for(int z=0 ; z < z_dim ; z++){


                for(int x=0 ; x < x_dim ; x++) {

                    for (int y = 0; y < y_dim; y++) {

                        double coordinateX = polys.get(i).getMinX() + x * res_x + res_x / 2;
                        double coordinateY = polys.get(i).getMinY() + y * res_y + res_y / 2;
                        double coordinateZ = z * res_z + res_z / 2;

                        point.setX(coordinateX);
                        point.setY(coordinateY);
                        point.setZ(coordinateZ);

                        ArrayList<KdTree.XYZPoint> nearestTrees = (ArrayList<KdTree.XYZPoint>)tree2.nearestNeighbourSearch(numNeighbors, point);

                        double[] distances = new double[nearestTrees.size()];
                        double averageDistance = 0.0;

                        double heatMapValue = 0.0;
                        double counter = 0.0;

                        for(int i_ = 0; i_ < nearestTrees.size(); i_++){

                            if(polys.get(i).pointInPolygon(nearestTrees.get(i_).getX(), nearestTrees.get(i_).getY()) == false) {
                                continue; // Skip trees that are not within the polygon
                            }

                            //if(nearestTrees.get(i_).getZ() < (coordinateZ + 0.25 ) )
                            //    continue;

                            boolean withinCone = isPointInCone(nearestTrees.get(i_).getX(), nearestTrees.get(i_).getY(), nearestTrees.get(i_).getZ(),
                                    point.getX(), point.getY(), point.getZ(), 4.0, 3.5);

                            if(!withinCone)
                                continue;

                            double distance = point.euclideanDistance(nearestTrees.get(i_));
                            double smoothDistance = smoothScaleLinear(distance, 1, 5);

                            averageDistance += smoothDistance;
                            //System.out.println("Distance to tree " + i_ + ": " + distance + " Smooth Distance: " + smoothDistance);
                            counter++;
                        }

                        if(counter == 0.0) {
                            // If no trees were found in the search radius, set heatMapValue to 0
                            heatMapValue = 0.0;
                        } else {
                            heatMapValue = averageDistance / counter;
                        }

                        if(heatMapValue > 1.0)
                            throw new toolException("Heatmap value exceeds 1.0, which is unexpected. Check the distance calculations. Found " + heatMapValue + " counter " + counter);


                        grid[x][y][z] = heatMapValue;

                    }
                }
            }
            tree2 = null;

            if(i % 10 == 0)
                System.gc();
            // Write the grid to the output file
            try {
                bw.write(String.valueOf(polys.get(i).getId()));

                for (int z = 0; z < z_dim; z++) {
                    for (int x = 0; x < x_dim; x++) {
                        for (int y = 0; y < y_dim; y++) {

                            bw.write(aR.sep + grid[x][y][z]);
                        }
                    }
                }
                bw.newLine();

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Polygon " + i + " processed. Center = (" + centerX + ", " + centerY + "), Dimensions = (" + x_dim + ", " + y_dim + ", " + z_dim + ")");

        }

        // Close the BufferedWriter
        if (bw != null)
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static double smoothScaleCosine(double x, double min, double max) {
        if (x < min) {
            return 1.0;
        } else if (x >= max) {
            return 0.0;
        } else {
            double t = (x - min) / (max - min); // Normalize to [0,1]
            return min * (1 + Math.cos(Math.PI * t)); // Cosine interpolation
        }
    }

    public static double smoothScaleLinear(double x, double min, double max) {
        if (x < min) {
            return 1.0;
        } else if (x >= max) {
            return 0.0;
        } else {
            double t = (x - min) / (max - min); // Normalize to [0,1]
            return 1.0 - t; // Linear decrease
        }
    }

    public static boolean isPointInCone(double ax, double ay, double az,
                                        double px, double py, double pz,
                                        double height, double radius) {

        double dz = az - pz;
        if (dz < 0 || dz > height) {
            // Point is either above the anchor or below the cone's bottom
            return false;
        }

        double dx = px - ax;
        double dy = py - ay;

        double distanceSquared = dx * dx + dy * dy;
        double maxRadiusAtZ = (radius / height) * dz;
        return distanceSquared <= maxRadiusAtZ * maxRadiusAtZ;
    }

    /** Order: 	[0] = x coordinate
     [1] = y coordinate
     [2] = height
     [3] = diameter
     [4] = species
     [5] = used (1) or not (0)
     */
    public void setTreeBank(double[][] in){

        //this.groundMeasuredOK = true;

        this.treeBank = in.clone();

        //System.out.println("tree length " + treeBank.length);

        for(int i = 0; i < treeBank.length; i++){

            labelTree.add(new float[]{Float.POSITIVE_INFINITY,0,0,0});

        }

        //

    }


}
