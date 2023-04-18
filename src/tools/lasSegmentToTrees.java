package tools;

import LASio.LASReader;
import LASio.LASraf;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import err.toolException;
import utils.KdTree;
import utils.argumentReader;
import utils.forestTree;
import utils.pointWriterMultiThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class lasSegmentToTrees {

    KdTree forest = new KdTree();

    HashMap<Integer, forestTree> trees = new HashMap<>();
    argumentReader aR;
    public lasSegmentToTrees(argumentReader aR) {

        this.aR = aR;

        //this.aR.add_extra_bytes(6, "tree_id", "Closest tree ID");

        try{
            this.readMeasuredTrees(this.aR.measured_trees);
        }catch (IOException e){
            System.out.println("Error reading measured trees file");
        }
    }

    public void setFieldMeasuredTrees(){

    }

    public void readMeasuredTrees(File measuredTreesFile) throws IOException {

        BufferedReader sc = new BufferedReader( new FileReader(measuredTreesFile));
        sc.readLine();
        String line1;

        if(!measuredTreesFile.exists())
            throw new toolException("Measured trees file does not exist: " + measuredTreesFile.getAbsolutePath());

        while((line1 = sc.readLine())!= null) {

            String[] line  = line1.split(",");

            double tree_x = Double.parseDouble(line[3]);
            double tree_y = Double.parseDouble(line[4]);


            forestTree tmpTree = new forestTree();

            tmpTree.setTreeLineFromFieldData(line1);
            tmpTree.setTreeLineFromFieldData_delimited((String[])line.clone());
            tmpTree.setTreeID(Integer.parseInt(line[1]));
            tmpTree.setTreeID_unique(Integer.parseInt(line[0]));

            trees.put(tmpTree.getTreeID_unique(), tmpTree);
            //tmpTree.setTreeCrownBounds(treeBounds.get(tmpTree.getTreeID_unique()));

            //print2DArray(treeBounds.get(tmpTree.getTreeID_unique()));

            tmpTree.setTreeX(Double.parseDouble(line[3]));
            tmpTree.setTreeY(Double.parseDouble(line[4]));
            tmpTree.setTreeHeight(Double.parseDouble(line[11]));

            int species = Integer.parseInt(line[7]);

            species = Math.min(species, 3) - 1;

            tmpTree.setTreeSpecies(species);
            tmpTree.setTreePlotID(Integer.parseInt(line[1]));

            tmpTree.setTreeDBH(Double.parseDouble(line[9]));

            tmpTree.setTreeVolume(Double.parseDouble(line[42]));

            KdTree.XYZPoint tmpPoint = new KdTree.XYZPoint(tree_x, tree_y, tmpTree.getTreeHeight());
            tmpPoint.setIndex(tmpTree.getTreeID_unique());

            forest.add(tmpPoint);

        }

    }

    public void processPointCloud(LASReader in, argumentReader aR) throws IOException{


        File outFile = aR.createOutputFile(in);
        int thread_n = aR.pfac.addReadThread(in);

        aR.add_extra_bytes(6, "tree_id", "tree id");

        pointWriterMultiThread pw = new pointWriterMultiThread(outFile, in, "las2trees", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        LasPoint tempPoint = new LasPoint();

        for(long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

            int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

            try{
                aR.pfac.prepareBuffer(thread_n, i, maxi);
            }catch (Exception e){
                System.out.println("Error preparing buffer");
            }
            for (int j = 0; j < maxi; j++) {

                in.readFromBuffer(tempPoint);

                if (!aR.inclusionRule.ask(tempPoint, i+j, true)) {
                    continue;
                }

                int closestIndex = findClosestTree(tempPoint.x, tempPoint.y, tempPoint.z);

                forestTree closestTree = trees.get(closestIndex);

                double distanceThreshold = closestTree.getTreeHeight() * 0.15;
                distanceThreshold = Math.max(distanceThreshold, 1.5);
                //System.out.println(closestTree.getTreeID_unique());

                if(euclideanDistance2d(tempPoint.x, tempPoint.y, closestTree.getTreeX(), closestTree.getTreeY()) < distanceThreshold && tempPoint.z <= closestTree.getTreeHeight()){
                    tempPoint.setExtraByteINT(closestTree.getTreeID_unique(), aR.create_extra_byte_vlr_n_bytes.get(0), 0);
                }else{
                    tempPoint.setExtraByteINT(0, aR.create_extra_byte_vlr_n_bytes.get(0), 0);
                }


                    //
                //System.out.println(tempPoint);
                try {

                    aR.pfac.writePoint(tempPoint, i+j, thread_n);

                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }


        aR.pfac.closeThread(thread_n);

        System.out.println("Done with file: " + in.getFile().getName());

    }

    public double euclideanDistance(double x1, double y1, double z1, double x2, double y2, double z2){

        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2));
    }

    public double euclideanDistance2d(double x1, double y1, double x2, double y2){

        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
    public int findClosestTree(double x, double y, double z){

        List<KdTree.XYZPoint> nearest = (List<KdTree.XYZPoint>)forest.nearestNeighbourSearch(1, new KdTree.XYZPoint(x, y, z));

        return nearest.get(0).getIndex();
    }
}

class tree{

    double x, y, z;
    int treeUniqueId, plotId;

    public tree(){

    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public int getTreeUniqueId() {
        return treeUniqueId;
    }

    public void setTreeUniqueId(int treeUniqueId) {
        this.treeUniqueId = treeUniqueId;
    }

    public int getPlotId() {
        return plotId;
    }

    public void setPlotId(int plotId) {
        this.plotId = plotId;
    }
}
