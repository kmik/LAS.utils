package LASio;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jblas.DoubleMatrix;
import org.tinfour.common.IQuadEdge;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.utils.Polyside;
import tools.lasStrip;
import utils.KdTree;
import utils.ThreadProgressBar;
import utils.fileOperations;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.tinfour.utils.Polyside.isPointInPolygon;

/* A container class for a collection of overlapping
.las files.
 */
public class LasBlock {

    fileOperations fo = new fileOperations();
    public static ThreadProgressBar progeBar = new ThreadProgressBar();

    double angleThreshold = 3.3;
    double resolution = 1.0;

    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    public double totalPoints = 0;

    String odir;

    int filesRead = 0;

    org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

    public List<LASReader> pointClouds = new ArrayList<>();
    public List<HashSet<Integer>> includedPointsFile1 = new ArrayList<>();
    public List<HashSet<Integer>> includedPointsFile2 = new ArrayList<>();

    public List<float[][]> images = new ArrayList<>();
    public List<float[]> imageExtents = new ArrayList<>();
    public List<double[]> pointCloudTimeExtent = new ArrayList<>();

    public List<LASraf> outputPointClouds = new ArrayList<>();

    public TreeMap<Double, int[]> pairs = new TreeMap<>();

    boolean[][] thinner;


    public boolean[] aligned;

    public HashSet<Integer> filesDone = new HashSet<>();

    public ArrayList<int[]> order = new ArrayList<>();

    public ArrayList<Double> differenceBefore_1_to_2 = new ArrayList<>();
    public ArrayList<Double> differenceBefore_2_to_1 = new ArrayList<>();

    public ArrayList<Double> stdBefore_1_to_2 = new ArrayList<>();
    public ArrayList<Double> stdBefore_2_to_1 = new ArrayList<>();

    lasStrip strippi;

    public int[] currentFiles;


    public LasBlock(){

    }


    public LasBlock(String outputDirectory, double resolution){

        this.resolution = resolution;
        this.odir = outputDirectory;
    }

    public void setStrip(lasStrip in){
        this.strippi = in;
    }

    /**
     * Adds a .las file to this block. Also updates the extent of the
     * block.
     * @param pointCloud
     * @throws IOException
     */
    public void addLasFile(LASReader pointCloud) throws IOException {

        pointClouds.add(pointCloud);
        images.add(new float[1][1]);
        imageExtents.add(new float[1]);

        if(pointCloud.minX < this.minX)
            this.minX = pointCloud.minX;
        if(pointCloud.minY < this.minY)
            this.minY = pointCloud.minY;

        if(pointCloud.maxX > this.maxX)
            this.maxX = pointCloud.maxX;
        if(pointCloud.maxY > this.maxY)
            this.maxY = pointCloud.maxY;

    }

    /**
     * Prepares the output .las files (writes dummy header information).
     * Also initializes the "aligned" tags.
     * @throws IOException
     */
    public void prepare() throws IOException{

        aligned = new boolean[pointClouds.size()];

        for(int i = 0; i < pointClouds.size(); i++){

            File oFile = fo.createNewFileWithNewExtension(pointClouds.get(i).getFile(), odir, "_SA.las");

            if(oFile.exists())
                oFile.delete();

            oFile.createNewFile();

            LASraf tempWriter = new LASraf(oFile);
            LASwrite.writeHeader(tempWriter, "lasStrip", pointClouds.get(i).versionMajor,
                    pointClouds.get(i).versionMinor, pointClouds.get(i).pointDataRecordFormat, pointClouds.get(i).pointDataRecordLength,
                    pointClouds.get(i).headerSize, pointClouds.get(i).offsetToPointData, pointClouds.get(i).numberVariableLengthRecords,
                    pointClouds.get(i).fileSourceID, pointClouds.get(i).globalEncoding,
                    pointClouds.get(i).xScaleFactor, pointClouds.get(i).yScaleFactor, pointClouds.get(i).zScaleFactor,
                    pointClouds.get(i).xOffset, pointClouds.get(i).yOffset, pointClouds.get(i).zOffset);
            outputPointClouds.add(tempWriter);

            pointCloudTimeExtent.add(new double[2]);

            filesRead++;
            strippi.filesRead++;
            strippi.updateProgress();
        }

    }
/*
    public void makeImages() throws IOException{

        boolean reject = false;

        progeBar.setName("Creating tins...");
        progeBar.setEnd(this.order.size());
        int n1;
        int n2;

        LASReader file1;
        LASReader file2;

        double[] normal1;
        double[] normal2;
        LasPoint tempPoint = new LasPoint();

        for(int p = 0; p < this.order.size(); p++){

            tempTin.clear();
            tempTin2.clear();
            tempTin3.clear();

            file1 = this.pointClouds.get(this.order.get(p)[0]);
            file2 = this.pointClouds.get(this.order.get(p)[1]);

            n1 = (int)file1.getNumberOfPointRecords();
            n2 = (int)file2.getNumberOfPointRecords();

            double minTime = Double.POSITIVE_INFINITY;
            double maxTime = Double.NEGATIVE_INFINITY;

            thinner = new boolean[(int)Math.ceil(((this.maxX - this.minX) / resolution))][(int)Math.ceil(((this.maxY - this.minY) / resolution))];

            for(int i = 0; i < n1; i++) {

                file1.readRecord(i, tempPoint);

                if(tempPoint.gpsTime > maxTime)
                    maxTime = tempPoint.gpsTime;
                if(tempPoint.gpsTime < minTime)
                    minTime = tempPoint.gpsTime;

                reject = thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)];

                //System.out.println(tempPoint.gpsTime);
                if(!reject && tempPoint.classification == 2) {

                    thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)] = true;

                    if (tempPoint.classification == 2 && Math.abs(tempPoint.scanAngleRank) < 90 && Math.random() > 0.0) {
                        //tree1.add(new KdTree.XYZPoint(tempPoint.x, tempPoint.y, tempPoint.z));
                        org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);
                        tempV.setIndex((int) i);
                        //tempV.setColorIndex(count++);
                        tempTin.add(tempV);

                        //tree1.add(new KdTree.XYZPoint(tempV.x, tempV.y, tempV.getZ(), tempV.getIndex()));
                    }
                }
            }

            pointCloudTimeExtent.get(this.order.get(p)[0])[0] = minTime;
            pointCloudTimeExtent.get(this.order.get(p)[0])[1] = maxTime;

            polator1 = new org.tinfour.interpolation.TriangularFacetInterpolator(tempTin);




            minTime = Double.POSITIVE_INFINITY;
            maxTime = Double.NEGATIVE_INFINITY;

            //thinner = new boolean[(int)Math.ceil(((Math.max(file1.getMaxX(), file2.getMaxX()) - Math.min(file1.getMinX(), file2.getMinX())) / resolution))]
              //      [(int)Math.ceil(((Math.max(file1.getMaxY(), file2.getMaxY()) - Math.min(file1.getMinY(), file2.getMinY())) / resolution))];

            //System.out.println("*****");
            //System.out.println("n2: " + n2 + " p: " + p);

            for(int i = 0; i < n2; i++) {

                file2.readRecord(i, tempPoint);

                if(tempPoint.gpsTime > maxTime)
                    maxTime = tempPoint.gpsTime;
                if(tempPoint.gpsTime < minTime)
                    minTime = tempPoint.gpsTime;

                //reject = thinner[(int)((tempPoint.x - file2.getMinX())/resolution)][(int)((file2.getMaxY() - tempPoint.y)/resolution)];

                //if(!reject) {

                   // thinner[(int)((tempPoint.x - file2.getMinX())/resolution)][(int)((file2.getMaxY() - tempPoint.y)/resolution)] = true;

                    if (tempPoint.classification == 2 && Math.abs(tempPoint.scanAngleRank) < 90 && Math.random() > 0.0) {
                        //tree2.add(new KdTree.XYZPoint(tempPoint.x, tempPoint.y, tempPoint.z));
                        org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);
                        tempV.setIndex(i);
                        //if(i == 1320490 && p == 4)
                          //  System.out.println("WHAT! " + i + " " + n2);
                        //tempV.setColorIndex(count++);
                        tempTin3.add(tempV);
                        //tree2.add(new KdTree.XYZPoint(tempV.x, tempV.y, tempV.getZ(), tempV.getIndex()));
                    }
                //}
            }

            polator3 = new org.tinfour.interpolation.TriangularFacetInterpolator(tempTin3);

            //System.out.println("n2: " + n2 + " p: " + p);

            List<org.tinfour.common.Vertex> closest = null;

            double angle = 0.0;

            boolean found = false;
            boolean terminate = false;

            int countBelowResolution = 0;

            for(org.tinfour.common.Vertex v : tempTin.getVertices()){

                found = false;
                terminate = false;

                while(!found && !terminate) {

                    countBelowResolution = 0;

                    polator1.interpolate(v.x, v.y, valuator);
                    //polator1.interpolate(v_closest.x, v_closest.y, valuator);
                    //beta = polator1.getCoefficients();
                    normal1 = polator1.getSurfaceNormal();
                    //System.out.println(Arrays.toString(normal));
                    Vector3D normalVector1 = new Vector3D(normal1[0], normal1[1], normal1[2]);

                    closest = tempTin3.getNeighborhoodPointsCollector().collectNeighboringVertices(v.x, v.y, 0, 0);

                    if (closest.size() > 0) {

                        for (int n = 0; n < closest.size(); n++) {

                            org.tinfour.common.Vertex v2 = new org.tinfour.common.Vertex(closest.get(n).x, closest.get(n).y, closest.get(n).getZ());

                            polator3.interpolate(v2.x, v2.y, valuator);

                            normal2 = polator3.getSurfaceNormal();
                            //System.out.println(Arrays.toString(normal));
                            Vector3D normalVector2 = new Vector3D(normal2[0], normal2[1], normal2[2]);

                            angle = Math.toDegrees(AngleBetween(normalVector1, normalVector2));

                            //System.out.println(angle);

                            if(v2.getDistance(v) < resolution)
                                countBelowResolution++;

                            if (v2.getDistance(v) < resolution && angle < angleThreshold) {

                                //System.out.println("dist: " + v2.getDistance(v) + " angle " + angle);
                                found = true;

                                v2.setIndex(closest.get(0).getIndex());
                                tempTin2.add(v2);

                                break;
                            }
                        }

                        if (!found) {

                            if(countBelowResolution == 0) {
                                tempTin.remove(v);
                                polator1.resetForChangeToTin();
                                terminate = true;
                            }else{
                                for (int n = 0; n < closest.size(); n++) {
                                    tempTin3.remove(closest.get(n));
                                }
                                polator3.resetForChangeToTin();
                            }

                        }

                    }
                }
                closest.clear();
            }

            //System.out.println(tempTin2.getVertices().size());

            //tempTin3.clear();

            pointCloudTimeExtent.get(this.order.get(p)[1])[0] = minTime;
            pointCloudTimeExtent.get(this.order.get(p)[1])[1] = maxTime;

            polator2 = new org.tinfour.interpolation.TriangularFacetInterpolator(tempTin2);

            //pruneTins();

            polator1.resetForChangeToTin();
            polator2.resetForChangeToTin();

            //HashSet<Integer> file1Indexes = new HashSet<>();
            //HashSet<Integer> file2Indexes = new HashSet<>();

            double minX1 = Double.POSITIVE_INFINITY;
            double maxX1 = Double.NEGATIVE_INFINITY;
            double minY1 = Double.POSITIVE_INFINITY;
            double maxY1 = Double.NEGATIVE_INFINITY;

            double minX2 = Double.POSITIVE_INFINITY;
            double maxX2 = Double.NEGATIVE_INFINITY;
            double minY2 = Double.POSITIVE_INFINITY;
            double maxY2 = Double.NEGATIVE_INFINITY;

            //System.out.println(tempTin2.getVertices().size());

            for(org.tinfour.common.Vertex v: tempTin.getVertices()) {

                includedPointsFile1.get(p).add(v.getIndex());
                file1.readRecord(v.getIndex(), tempPoint);
                v.setIndex((int)(tempPoint.gpsTime * 1000));

                if(v.x < minX1){
                    minX1 = v.x;
                }
                if(v.x > maxX1){
                    maxX1 = v.x;
                }
                if(v.y < minY1){
                    minY1 = v.y;
                }
                if(v.y > maxY1){
                    maxY1 = v.y;
                }

            }

            for(org.tinfour.common.Vertex v: tempTin2.getVertices()) {

                includedPointsFile2.get(p).add(v.getIndex());

                file2.readRecord(v.getIndex(), tempPoint);
                v.setIndex((int)(tempPoint.gpsTime * 1000));

                if(v.x < minX2){
                    minX2 = v.x;
                }
                if(v.x > maxX2){
                    maxX2 = v.x;
                }
                if(v.y < minY2){
                    minY2 = v.y;
                }
                if(v.y > maxY2){
                    maxY2 = v.y;
                }

            }

            //System.out.println(includedPointsFile1.get(p).size() + " / " + includedPointsFile2.get(p).size());

            this.totalPoints += (includedPointsFile1.get(p).size() + includedPointsFile2.get(p).size())/2;

            //System.out.println("size"  + includedPointsFile2.get(p).s ize());
            //System.out.println("-------");
            //progeBar.updateCurrent(1);
            //progeBar.print();
            strippi.tinProgress++;
            strippi.updateProgress();
            //images.set(this.order.get(p)[0], makeImage(tempTin, polator1, minX1, maxX1, minY1, maxY1, this.order.get(p)[0]));
            //images.set(this.order.get(p)[1], makeImage(tempTin2, polator2, minX2, maxX2, minY2, maxY2, this.order.get(p)[1]));

        }
    }
*/
/*
    public void makeImages2() throws IOException{

        boolean reject = false;

        progeBar.setName("Creating tins...");
        progeBar.setEnd(this.order.size());
        int n1;
        int n2;

        LASReader file1;
        LASReader file2;

        double[] normal1;
        double[] normal2;
        LasPoint tempPoint = new LasPoint();



        for(int p = 0; p < this.order.size(); p++){

            tempTin.clear();
            tempTin2.clear();
            tempTin3.clear();

            float[][] lowest = new float[(int)Math.ceil(((this.maxX - this.minX) / resolution))][(int)Math.ceil(((this.maxY - this.minY) / resolution))];
            int[][] lowest_index = new int[(int)Math.ceil(((this.maxX - this.minX) / resolution))][(int)Math.ceil(((this.maxY - this.minY) / resolution))];
            thinner = new boolean[(int)Math.ceil(((this.maxX - this.minX) / resolution))][(int)Math.ceil(((this.maxY - this.minY) / resolution))];

            for(int x = 0; x < thinner.length; x++) {
                for (int y = 0; y < thinner[0].length; y++) {
                    thinner[x][y] = false;
                    lowest[x][y] = Float.POSITIVE_INFINITY;
                    lowest_index[x][y] = -1;

                }
            }

            file1 = this.pointClouds.get(this.order.get(p)[0]);
            file2 = this.pointClouds.get(this.order.get(p)[1]);

            n1 = (int)file1.getNumberOfPointRecords();
            n2 = (int)file2.getNumberOfPointRecords();

            double minTime = Double.POSITIVE_INFINITY;
            double maxTime = Double.NEGATIVE_INFINITY;

            HashMap<Integer, Integer> ground1 = new HashMap();
            HashMap<Integer, Integer> ground2 = new HashMap();

            HashSet<Integer> testi = new HashSet<>();

            thinner = new boolean[(int)Math.ceil(((this.maxX - this.minX) / resolution))][(int)Math.ceil(((this.maxY - this.minY) / resolution))];

            for(int i = 0; i < n1; i++) {

                file1.readRecord(i, tempPoint);

                if(tempPoint.gpsTime > maxTime)
                    maxTime = tempPoint.gpsTime;
                if(tempPoint.gpsTime < minTime)
                    minTime = tempPoint.gpsTime;

                if(tempPoint.classification == 2)
                    thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)] = true;

                if(thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)] && tempPoint.z < lowest[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)]) {

                    //thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)] = true;
                    lowest[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)] = (float)tempPoint.z;
                    lowest_index[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)] = i;

                }
            }

            for(int x = 0; x < thinner.length; x++) {
                for(int y = 0; y < thinner[0].length; y++) {

                    if (lowest_index[x][y] != -1) {

                        ground1.put(thinner.length * y + x, lowest_index[x][y]);

                        //System.out.println(lowest_index[x][y]);
                    }
                }
            }

            for(int x = 0; x < thinner.length; x++) {
                for (int y = 0; y < thinner[0].length; y++) {
                    thinner[x][y] = false;
                    lowest[x][y] = Float.POSITIVE_INFINITY;
                    lowest_index[x][y] = -1;

                }
            }

            pointCloudTimeExtent.get(this.order.get(p)[0])[0] = minTime;
            pointCloudTimeExtent.get(this.order.get(p)[0])[1] = maxTime;

            minTime = Double.POSITIVE_INFINITY;
            maxTime = Double.NEGATIVE_INFINITY;

            for(int i = 0; i < n2; i++) {

                file2.readRecord(i, tempPoint);

                if(tempPoint.gpsTime > maxTime)
                    maxTime = tempPoint.gpsTime;
                if(tempPoint.gpsTime < minTime)
                    minTime = tempPoint.gpsTime;

                if(tempPoint.classification == 2)
                    thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)] = true;

                if(thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)] && tempPoint.z < lowest[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)]) {

                    //thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)] = true;
                    lowest[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)] = (float)tempPoint.z;

                    lowest_index[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)] = i;

                }
            }

            for(int x = 0; x < thinner.length; x++) {
                for (int y = 0; y < thinner[0].length; y++) {

                    if (lowest_index[x][y] != -1) {

                        if(ground1.containsKey(thinner.length * y + x)) {

                            includedPointsFile1.get(p).add(ground1.get(thinner.length * y + x));

                            if(!testi.contains(thinner.length * y + x))
                                testi.add(thinner.length * y + x);
                            else
                                System.out.println(thinner.length * y + x);

                            includedPointsFile2.get(p).add(lowest_index[x][y]);
                            //System.out.println(includedPointsFile1.get(p).size() + " == " + includedPointsFile2.get(p).size());
                        }
                    }
                }
            }

            //System.out.println(tempTin2.getVertices().size());

            //tempTin3.clear();

            pointCloudTimeExtent.get(this.order.get(p)[1])[0] = minTime;
            pointCloudTimeExtent.get(this.order.get(p)[1])[1] = maxTime;

            //polator2 = new org.tinfour.interpolation.TriangularFacetInterpolator(tempTin2);

            //pruneTins();

            //polator1.resetForChangeToTin();
            //polator2.resetForChangeToTin();

            //HashSet<Integer> file1Indexes = new HashSet<>();
            //HashSet<Integer> file2Indexes = new HashSet<>();
/*
            double minX1 = Double.POSITIVE_INFINITY;
            double maxX1 = Double.NEGATIVE_INFINITY;
            double minY1 = Double.POSITIVE_INFINITY;
            double maxY1 = Double.NEGATIVE_INFINITY;

            double minX2 = Double.POSITIVE_INFINITY;
            double maxX2 = Double.NEGATIVE_INFINITY;
            double minY2 = Double.POSITIVE_INFINITY;
            double maxY2 = Double.NEGATIVE_INFINITY;

            //System.out.println(tempTin2.getVertices().size());

            for(org.tinfour.common.Vertex v: tempTin.getVertices()) {

                includedPointsFile1.get(p).add(v.getIndex());
                file1.readRecord(v.getIndex(), tempPoint);
                v.setIndex((int)(tempPoint.gpsTime * 1000));

                if(v.x < minX1){
                    minX1 = v.x;
                }
                if(v.x > maxX1){
                    maxX1 = v.x;
                }
                if(v.y < minY1){
                    minY1 = v.y;
                }
                if(v.y > maxY1){
                    maxY1 = v.y;
                }

            }

            for(org.tinfour.common.Vertex v: tempTin2.getVertices()) {

                includedPointsFile2.get(p).add(v.getIndex());

                file2.readRecord(v.getIndex(), tempPoint);
                v.setIndex((int)(tempPoint.gpsTime * 1000));

                if(v.x < minX2){
                    minX2 = v.x;
                }
                if(v.x > maxX2){
                    maxX2 = v.x;
                }
                if(v.y < minY2){
                    minY2 = v.y;
                }
                if(v.y > maxY2){
                    maxY2 = v.y;
                }

            }

            this.totalPoints += (includedPointsFile1.get(p).size() + includedPointsFile2.get(p).size())/2;
            //System.out.println(includedPointsFile1.get(p).size() + " / " + includedPointsFile2.get(p).size());

            //System.out.println("size"  + includedPointsFile2.get(p).s ize());
            //System.out.println("-------");
            //progeBar.updateCurrent(1);
            //progeBar.print();
            strippi.tinProgress++;
            strippi.updateProgress();
            //images.set(this.order.get(p)[0], makeImage(tempTin, polator1, minX1, maxX1, minY1, maxY1, this.order.get(p)[0]));
            //images.set(this.order.get(p)[1], makeImage(tempTin2, polator2, minX2, maxX2, minY2, maxY2, this.order.get(p)[1]));

        }
    }
*/
    public void makeImages3() throws IOException{

        boolean reject = false;

        progeBar.setName("Creating tins...");
        progeBar.setEnd(this.order.size());
        int n1;
        int n2;

        LASReader file1 = null;
        LASReader file2 = null;

        double[] normal1;
        double[] normal2;
        LasPoint tempPoint = new LasPoint();

        ArrayList<Integer>[][] indexes;
        ArrayList<Integer>[][] indexes2;

        KdTree.XYZPoint searchPoint = new KdTree.XYZPoint(0,0,0);

        org.tinfour.common.Vertex temp_v = new org.tinfour.common.Vertex(0,0,0);


        thinner = new boolean[(int)Math.ceil(((this.maxX - this.minX) / resolution))][(int)Math.ceil(((this.maxY - this.minY) / resolution))];

        KdTree.XYZPoint po;

        ArrayList<KdTree.XYZPoint> nearestPoints = new ArrayList<>();

        DoubleMatrix normal_doublematrix1 = new DoubleMatrix(3,1);
        DoubleMatrix normal_doublematrix2 = new DoubleMatrix(3,1);

        double angle = 0.0;

        Iterator it;

        double[] normal = null;

        for(int p = 0; p < this.order.size(); p++){

            indexes = new ArrayList[(int)Math.ceil(((this.maxX - this.minX) / resolution))][(int)Math.ceil(((this.maxY - this.minY) / resolution))];
            indexes2 = new ArrayList[(int)Math.ceil(((this.maxX - this.minX) / resolution))][(int)Math.ceil(((this.maxY - this.minY) / resolution))];

            KdTree tree1 = new KdTree();

            file1 = this.pointClouds.get(this.order.get(p)[0]);
            file2 = this.pointClouds.get(this.order.get(p)[1]);

            IncrementalTin tempTin_1 = new IncrementalTin();
            IncrementalTin tempTin_2 = new IncrementalTin();

            HashMap<Integer, org.tinfour.common.Vertex> vertices_1 = new HashMap<>();
            HashMap<Integer, org.tinfour.common.Vertex> vertices_2 = new HashMap<>();

            for(int x = 0; x < thinner.length; x++) {
                for (int y = 0; y < thinner[0].length; y++) {

                    thinner[x][y] = false;

                }
            }


            n1 = (int)file1.getNumberOfPointRecords();
            n2 = (int)file2.getNumberOfPointRecords();

            double minTime = Double.POSITIVE_INFINITY;
            double maxTime = Double.NEGATIVE_INFINITY;

            for(int i = 0; i < n1; i++) {

                file1.readRecord(i, tempPoint);

                if(tempPoint.gpsTime > maxTime)
                    maxTime = tempPoint.gpsTime;
                if(tempPoint.gpsTime < minTime)
                    minTime = tempPoint.gpsTime;

                if(tempPoint.classification == 2)
                    thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)] = true;

                if(tempPoint.classification == 2 && thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)]) {

                    if(indexes[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)] == null){
                        indexes[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)] = new ArrayList<>(1000);
                    }

                    indexes[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)].add(i);

                }
            }

            pointCloudTimeExtent.get(this.order.get(p)[0])[0] = minTime;
            pointCloudTimeExtent.get(this.order.get(p)[0])[1] = maxTime;

            minTime = Double.POSITIVE_INFINITY;
            maxTime = Double.NEGATIVE_INFINITY;

            for(int i = 0; i < n2; i++) {

                file2.readRecord(i, tempPoint);

                if(tempPoint.gpsTime > maxTime)
                    maxTime = tempPoint.gpsTime;
                if(tempPoint.gpsTime < minTime)
                    minTime = tempPoint.gpsTime;

                if(tempPoint.classification == 2)
                  thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)] = true;

                if(tempPoint.classification == 2 && thinner[(int)((tempPoint.x - this.minX)/resolution)][(int)((this.maxY - tempPoint.y)/resolution)]) {

                    if(indexes2[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)] == null){
                        indexes2[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)] = new ArrayList<>(1000);
                    }

                    indexes2[(int) ((tempPoint.x - this.minX) / resolution)][(int) ((this.maxY - tempPoint.y) / resolution)].add(i);

                }
            }


            pointCloudTimeExtent.get(this.order.get(p)[1])[0] = minTime;
            pointCloudTimeExtent.get(this.order.get(p)[1])[1] = maxTime;

            nearestPoints.clear();


            for(int x = 0; x < indexes.length; x++) {
                for(int y = 0; y < indexes[0].length; y++) {



                    if (indexes[x][y] != null && indexes2[x][y] != null &&
                            indexes[x][y].size() >= 4 && indexes2[x][y].size() >=4) {

                        for(int i = 0; i < indexes2[x][y].size(); i++){

                            file2.readRecord(indexes2[x][y].get(i).intValue(), tempPoint);

                            KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(tempPoint.x, tempPoint.y, tempPoint.z);
                            tempTreePoint.setIndex(indexes2[x][y].get(i).intValue());

                            org.tinfour.common.Vertex tempVertex = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);

                            tempVertex.setIndex(indexes2[x][y].get(i).intValue());
                            vertices_2.put(indexes2[x][y].get(i).intValue(), tempVertex);

                            tree1.add(tempTreePoint);
                            tempTin_2.add(tempVertex);

                        }

                        for(int i = 0; i < indexes[x][y].size(); i++){

                            file1.readRecord(indexes[x][y].get(i).intValue(), tempPoint);

                            org.tinfour.common.Vertex tempVertex = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);
                            tempVertex.setIndex(indexes[x][y].get(i).intValue());
                            tempTin_1.add(tempVertex);

                        }

                        if(!tempTin_1.isBootstrapped() || !tempTin_2.isBootstrapped() || false) {
                            continue;
                        }



                        markPerimeter(tempTin_1, -99);
                        markPerimeter(tempTin_2, -99);

                        org.tinfour.interpolation.TriangularFacetInterpolator polator_1 = new org.tinfour.interpolation.TriangularFacetInterpolator(tempTin_1);
                        org.tinfour.interpolation.TriangularFacetInterpolator polator_2 = new org.tinfour.interpolation.TriangularFacetInterpolator(tempTin_2);

                        double tempDistance = 0.0;
                        double tempDistance2 = 0.0;

                        for(org.tinfour.common.Vertex v : tempTin_1.getVertices()){

                            if(v.getIndex() != -99) {

                                searchPoint.setX(v.x);
                                searchPoint.setY(v.y);
                                searchPoint.setZ(v.getZ());
                                nearestPoints = (ArrayList<KdTree.XYZPoint>) tree1.nearestNeighbourSearch(1, searchPoint);

                                temp_v = vertices_2.get(nearestPoints.get(0).getIndex());

                                if (temp_v.getIndex() != -99) {

                                    polator_2.interpolate(temp_v.x, temp_v.y, valuator);
                                    normal2 = polator_2.getSurfaceNormal();

                                    normal_doublematrix2.put(0,0, normal2[0]);
                                    normal_doublematrix2.put(1,0, normal2[1]);
                                    normal_doublematrix2.put(2,0, normal2[2]);

                                    temp_v = vertices_2.get(nearestPoints.get(0).getIndex());

                                    tempDistance = polator_1.interpolate(v.x, v.y, valuator);

                                    normal1 = polator_1.getSurfaceNormal();

                                    normal_doublematrix1.put(0,0, normal1[0]);
                                    normal_doublematrix1.put(1,0, normal1[1]);
                                    normal_doublematrix1.put(2,0, normal1[2]);

                                    angle = Math.toDegrees(AngleBetween(normal_doublematrix1, normal_doublematrix2));

                                    if (angle < this.angleThreshold) {

                                        includedPointsFile1.get(p).add(v.getIndex());
                                        includedPointsFile2.get(p).add(temp_v.getIndex());
                                        temp_v.setIndex(-99);

                                    }
                                }
                                nearestPoints.set(0,null);
                                nearestPoints.clear();
                            }
                        }


                    }

                    it = tree1.reverse_iterator();

                    while(it.hasNext()){
                        po = (KdTree.XYZPoint)it.next();
                        tree1.remove(po);
                        po = null;
                    }

                    it = tree1.iterator();

                    while(it.hasNext()){
                        po = (KdTree.XYZPoint)it.next();
                        tree1.remove(po);
                        po = null;
                    }


                    vertices_1.clear();
                    vertices_2.clear();
                    tempTin_1.clear();
                    tempTin_2.clear();

                }
            }

            vertices_1.clear();
            vertices_2.clear();
            tempTin_1.clear();
            tempTin_2.clear();

            for(int x = 0; x < thinner.length; x++) {
                for(int y = 0; y < thinner[0].length; y++) {

                    if(indexes[x][y] != null){

                        for(int i = 0; i < indexes[x][y].size(); i++){
                            indexes[x][y].remove(i);
                        }
                        indexes[x][y].clear();
                        indexes[x][y] = null;
                    }

                    if(indexes2[x][y] != null){
                        for(int i = 0; i < indexes2[x][y].size(); i++){
                            indexes2[x][y].remove(i);
                        }
                        indexes2[x][y].clear();
                        indexes2[x][y] = null;
                    }
                }
            }

            for(org.tinfour.common.Vertex v : tempTin_1.getVertices())
                v = null;

            for(org.tinfour.common.Vertex v : tempTin_2.getVertices())
                v = null;

            for(org.tinfour.common.IQuadEdge e : tempTin_1.getEdges())
                e = null;

            for(org.tinfour.common.IQuadEdge e : tempTin_2.getEdges())
                e = null;

            tempTin_1.clear();
            tempTin_2.clear();


            for(int i = 0; i < n1; i++) {
                if(includedPointsFile1.get(p).contains(i)) {
                    file1.readRecord(i, tempPoint);

                    temp_v = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);

                    tempTin_1.add(temp_v);
                }
            }
            for(int i = 0; i < n2; i++) {
                if(includedPointsFile2.get(p).contains(i)) {
                    file2.readRecord(i, tempPoint);

                    temp_v = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);

                    tempTin_2.add(temp_v);
                }
            }

            org.tinfour.interpolation.TriangularFacetInterpolator polator_1 = new org.tinfour.interpolation.TriangularFacetInterpolator(tempTin_1);
            org.tinfour.interpolation.TriangularFacetInterpolator polator_2 = new org.tinfour.interpolation.TriangularFacetInterpolator(tempTin_2);

            double distance = 0.0;

            double disti = 0.0;

            int count = 0;
            double difBefore = 0.0;

            double stdDev = 0.0;
            double average = 0.0;
            double pwrSumAvg = 0.0;

            double distiSigned = 0.0;

            double tempDistance2 = 0.0;

            List<IQuadEdge> perimeter = tempTin_1.getPerimeter();

            for (org.tinfour.common.Vertex v : tempTin_2.getVertices()) {

                //if (tempTin_1.isPointInsideTin(v.x, v.y)) {
                if (isPointInPolygon(perimeter, v.x, v.y) == Polyside.Result.Inside) {

                    double interpolatedValue = polator_1.interpolate(v.x, v.y, valuator);
                    distiSigned = v.getZ() - interpolatedValue;

                    normal = polator_1.getSurfaceNormal();

                    distiSigned = (normal[0] * tempPoint.x + normal[1] * tempPoint.y + normal[2] * tempPoint.z -
                            (normal[0] * tempPoint.x + normal[1] * tempPoint.y + normal[2] * (tempPoint.z - distiSigned))) /
                            Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);

                    disti = Math.abs(distiSigned);

                    if (!Double.isNaN(disti)) {
                        count++;
                        distance += disti;

                        average += (distiSigned - average) / count;
                        pwrSumAvg += (distiSigned * distiSigned - pwrSumAvg) / count;
                        stdDev = Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));
                    }
                }
            }

            stdBefore_2_to_1.add(stdDev);
            differenceBefore_2_to_1.add((distance / (double)count));

            distance = 0.0;
            disti = 0.0;
            count = 0;

            stdDev = 0.0;
            average = 0.0;
            pwrSumAvg = 0.0;

            distiSigned = 0.0;


            for (org.tinfour.common.Vertex v : tempTin_1.getVertices()) {

                double interpolatedValue = polator_2.interpolate(v.x, v.y, valuator);

                if(!Double.isNaN(interpolatedValue)) {

                    distiSigned = v.getZ() - interpolatedValue;

                    normal = polator_1.getSurfaceNormal();

                    distiSigned = (normal[0] * tempPoint.x + normal[1] * tempPoint.y + normal[2] * tempPoint.z -
                            (normal[0] * tempPoint.x + normal[1] * tempPoint.y + normal[2] * (tempPoint.z - distiSigned))) /
                            Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);

                    disti = Math.abs(distiSigned);

                    if(!Double.isNaN(disti)) {
                        count++;
                        distance += disti;

                        average += (distiSigned - average) / count;
                        pwrSumAvg += (distiSigned * distiSigned - pwrSumAvg) / count;
                        stdDev = Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));
                    }
                }

            }

            stdBefore_1_to_2.add(stdDev);
            differenceBefore_1_to_2.add((distance / (double)count));

            this.totalPoints += (includedPointsFile1.get(p).size() + includedPointsFile2.get(p).size())/2;

            for(org.tinfour.common.Vertex v : tempTin_1.getVertices())
                v = null;

            for(org.tinfour.common.Vertex v : tempTin_2.getVertices())
                v = null;

            for(org.tinfour.common.IQuadEdge e : tempTin_1.getEdges())
                e = null;

            for(org.tinfour.common.IQuadEdge e : tempTin_2.getEdges())
                e = null;

            polator_1 = null;
            polator_2 = null;

            tempTin_1.clear();
            tempTin_2.clear();

            tempTin_1.dispose();
            tempTin_2.dispose();

            strippi.tinProgress++;
            strippi.updateProgress();

            System.gc();
        }

        indexes = new ArrayList[1][1];
        indexes2 = new ArrayList[1][1];

        System.gc();

    }

    public void markPerimeter(org.tinfour.standard.IncrementalTin tin, int mark){

        for(IQuadEdge e : tin.getPerimeter()){
            e.getA().setIndex(mark);
            e.getB().setIndex(mark);
        }
    }

    public float[][] makeImage(org.tinfour.standard.IncrementalTin tinIn,org.tinfour.interpolation.TriangularFacetInterpolator polator, double minX, double maxX, double minY, double maxY, int pointCloudIndex){

        int xDim = (int)Math.ceil((maxX - minX) / this.resolution);
        int yDim = (int)Math.ceil((maxY - minY) / this.resolution);

        double xCoord;
        double yCoord;

        float value;

        float[][] output = new float[xDim][yDim];

        for(int x = 0; x < xDim; x++){
            for(int y = 0; y < yDim; y++) {

                xCoord = minX + this.resolution * (double)x;
                yCoord = minY + this.resolution * (double)y;

                value = (float)polator.interpolate(xCoord, yCoord, valuator);

                //System.out.println(value);

                output[x][y] = value;
            }
        }

        return output;
    }
/*
    public void pruneTins(){

        List<org.tinfour.common.Vertex> closest;
        List<org.tinfour.common.Vertex> closest2;

        org.tinfour.common.Vertex v_closest = null;
        org.tinfour.common.Vertex v_closest2;
        double[][] vs = new double[3][3];

        double[] left = new double[3];
        double[] right = new double[3];

        double angle1 = -1.0;
        double angle2 = -1.0;
        double angle3 = -1.0;

        double[] a;
        double[] b;
        double[] c;

        double meanDistance = 0.0;

        double distance1 = 0.0;
        double distance2 = 0.0;
        double counti = 0;

        double[] normal;

        //System.out.println("Orig counts: " + tempTin.getVertices().size() + " + " + tempTin2.getVertices().size());

        int changed = 101;

        ArrayList<Double> median1 = new ArrayList<>();
        ArrayList<Double> median2 = new ArrayList<>();

        while(changed >= 25){

            median1.clear();
            median2.clear();


            changed = 0;
            polator1.resetForChangeToTin();
            //this.vertices1.clear();
            //this.vertices1 = tin1.getVertices();

            for(org.tinfour.common.Vertex v : tempTin.getVertices()){

                angle1 = -1.0;
                angle2 = -1.0;
                angle3 = -1.0;

                polator1.interpolate(v.x, v.y, valuator);
                normal = polator1.getSurfaceNormal();
                Vector3D normal3 = new Vector3D(normal[0],normal[1],normal[2]);

                if(tempTin2.isPointInsideTin(v.x, v.y)){

                    distance1 = Math.abs(v.getZ() - polator2.interpolate(v.x, v.y, valuator));
                    //beta = polator2.getCoefficients();
                    normal = polator2.getSurfaceNormal();
                    //System.out.println(Arrays.toString(normal));
                    //Vector3D normal1 = new Vector3D(normal[0],normal[1],normal[2]);

                    closest = tempTin2.getNeighborhoodPointsCollector().collectNeighboringVertices(v.x, v.y, 0, 0);

                    if(closest.size() > 0){

                        v_closest = closest.get(0);

                        //polator2.interpolate(v_closest.x, v_closest.y, valuator);

                        if(tempTin.isPointInsideTin(v_closest.x, v_closest.y)){

                            polator2.interpolate(v_closest.x, v_closest.y, valuator);
                            //polator1.interpolate(v_closest.x, v_closest.y, valuator);
                            //beta = polator1.getCoefficients();
                            normal = polator2.getSurfaceNormal();
                            //System.out.println(Arrays.toString(normal));
                            Vector3D normal2 = new Vector3D(normal[0],normal[1],normal[2]);

                            //System.out.println("angle: " + Math.toDegrees(AngleBetween(normal1, normal2)));
                            angle1 = Math.toDegrees(AngleBetween(normal3, normal2));

                            closest2 = tempTin.getNeighborhoodPointsCollector().collectNeighboringVertices(v_closest.x, v_closest.y, 0, 0);

                            if(closest.size() > 0){

                                v_closest2 = closest2.get(0);

                                if(tempTin2.isPointInsideTin(v_closest2.x, v_closest2.y)){

                                    distance2 = Math.abs(v_closest2.getZ() - polator2.interpolate(v_closest2.x, v_closest2.y, valuator));
                                    //beta = polator2.getCoefficients();
                                    polator1.interpolate(v_closest2.x, v_closest2.y, valuator);

                                    normal = polator1.getSurfaceNormal();

                                    Vector3D normal1 = new Vector3D(normal[0],normal[1],normal[2]);
                                    //angle2 = calcNormal(closest2);
                                    angle2 = Math.toDegrees(AngleBetween(normal3, normal1));
                                    angle3 = Math.toDegrees(AngleBetween(normal2, normal1));
                                }
                            }
                        }else{
                            tempTin2.remove(v_closest);
                            //v_closest.setSynthetic(true);
                            v_closest = null;
                            polator2.resetForChangeToTin();
                            continue;
                        }


                        if(angle1 != -1.0 && angle2 != -1.0){

                            if(Math.abs(angle1 - angle2) > this.angleThreshold ||
                                    Math.abs(angle2 - angle3) > this.angleThreshold ||
                                    Math.abs(angle1 - angle3) > this.angleThreshold){
                                changed++;
                                tempTin.remove(v);
                                //tin2.remove(v_closest);
                                v = null;
                                //polator2.resetForChangeToTin();
                                polator1.resetForChangeToTin();

                            }else {

                                median1.add((distance1+distance2)/2.0d);

                                //System.out.println(angle1 + " " + angle2);
                                //v_closest.setSynthetic(true);
                                //v.setSynthetic(true);
                            }


                        }


                    }else{
                        tempTin2.remove(v_closest);
                        v_closest = null;
                        polator2.resetForChangeToTin();
                        continue;
                    }


                }
                else{
                    tempTin.remove(v);
                    v = null;
                    polator1.resetForChangeToTin();
                }

            }

            polator2.resetForChangeToTin();

            //this.vertices2.clear();
            //this.vertices2 = tin2.getVertices();


            for(org.tinfour.common.Vertex v : tempTin2.getVertices()){

                angle1 = -1.0;
                angle2 = -1.0;
                angle3 = -1.0;

                polator2.interpolate(v.x, v.y, valuator);
                normal = polator2.getSurfaceNormal();
                Vector3D normal3 = new Vector3D(normal[0],normal[1],normal[2]);

                if(tempTin.isPointInsideTin(v.x, v.y)){

                    distance1 = Math.abs(v.getZ() - polator1.interpolate(v.x, v.y, valuator));
                    //beta = polator1.getCoefficients();
                    normal = polator1.getSurfaceNormal();
                    //Vector3D normal1 = new Vector3D(normal[0],normal[1],normal[2]);
                    closest = tempTin.getNeighborhoodPointsCollector().collectNeighboringVertices(v.x, v.y, 0, 0);

                    if(closest.size() > 0){

                        v_closest = closest.get(0);

                        if(tempTin2.isPointInsideTin(v_closest.x, v_closest.y)){

                            polator1.interpolate(v_closest.x, v_closest.y, valuator);
                            //beta = polator2.getCoefficients();
                            normal = polator2.getSurfaceNormal();

                            Vector3D normal2 = new Vector3D(normal[0],normal[1],normal[2]);

                            //System.out.println("angle: " + Math.toDegrees(AngleBetween(normal1, normal2)));
                            angle1 = Math.toDegrees(AngleBetween(normal3, normal2));

                            //angle1 = calcNormal(closest);

                            closest2 = tempTin2.getNeighborhoodPointsCollector().collectNeighboringVertices(v_closest.x, v_closest.y, 0, 0);

                            if(closest2.size() > 0){

                                v_closest2 = closest2.get(0);

                                if(tempTin.isPointInsideTin(v_closest2.x, v_closest2.y)){

                                    distance2 = Math.abs(v_closest2.getZ() - polator1.interpolate(v_closest2.x, v_closest2.y, valuator));
                                    polator2.interpolate(v_closest2.x, v_closest2.y, valuator);
                                    //beta = polator1.getCoefficients();
                                    normal = polator2.getSurfaceNormal();

                                    Vector3D normal1 = new Vector3D(normal[0],normal[1],normal[2]);
                                    //angle2 = calcNormal(closest2);
                                    angle2 = Math.toDegrees(AngleBetween(normal3, normal1));
                                    angle3 = Math.toDegrees(AngleBetween(normal2, normal1));
                                    //angle2 = calcNormal(closest2);

                                }
                            }
                        }else{
                            tempTin.remove(v_closest);
                            //v_closest.setSynthetic(true);
                            v_closest = null;
                            polator1.resetForChangeToTin();
                            continue;
                        }


                        if(angle1 != -1.0 && angle2 != -1.0){

                            if(Math.abs(angle1 - angle2) > this.angleThreshold ||
                                    Math.abs(angle2 - angle3) > this.angleThreshold ||
                                    Math.abs(angle1 - angle3) > this.angleThreshold){
                                changed++;
                                tempTin2.remove(v);
                                //tin2.remove(v_closest);
                                v = null;
                                //polator2.resetForChangeToTin();
                                polator2.resetForChangeToTin();

                            }else {
                                median2.add((distance1+distance2)/2.0d);
                            }


                        }


                    }else{
                        tempTin.remove(v_closest);
                        v_closest = null;
                        polator1.resetForChangeToTin();
                        continue;
                    }


                }
                else{
                    tempTin2.remove(v);
                    v = null;
                    polator2.resetForChangeToTin();
                }

            }

            //System.out.print("\33[2K");
            //System.out.print("Vertices removed: " + changed + " Tin1 size: " + tempTin.getVertices().size() + " Tin2 size: " + tempTin2.getVertices().size() + "\r");


            System.gc();
        }
        /*
        Collections.sort(median1);
        Collections.sort(median2);

        double mediaani1 = median1.get(median1.size()/2);
        double mediaani2 = median2.get(median2.size()/2);

        HashSet<Integer> remove1 = new HashSet<>();
        HashSet<Integer> remove2 = new HashSet<>();

        int count1 = -1;
        int count2 = -1;

        for(org.tinfour.common.Vertex v : tempTin2.getVertices()){
            count2++;

            if(tempTin.isPointInsideTin(v.x, v.y)){

                distance2 = Math.abs(v.getZ() - polator1.interpolate(v.x, v.y, valuator));

                if(distance2 >= (2.0 * mediaani2))
                    remove2.add(count2);

            }else{
                remove2.add(count2);
            }

        }

        for(org.tinfour.common.Vertex v : tempTin.getVertices()){
            count1++;

            if(tempTin2.isPointInsideTin(v.x, v.y)){

                distance1 = Math.abs(v.getZ() - polator2.interpolate(v.x, v.y, valuator));

                if(distance1 >= (2.0 * mediaani1))
                    remove1.add(count1);

            }else{
                remove1.add(count1);
            }

        }
        count1 = 0;
        count2 = 0;

        for(org.tinfour.common.Vertex v : tempTin2.getVertices()){

            if(remove2.contains(count2))
                tempTin2.remove(v);

            count2++;
        }
        for(org.tinfour.common.Vertex v : tempTin.getVertices()){

            if(remove1.contains(count1))
                tempTin.remove(v);

            count1++;
        }



        //System.out.println("Tin1 size: " + tempTin.getVertices().size());
        //System.out.println("Tin2 size: " + tempTin2.getVertices().size());

    }
*/
    public double AngleBetween(Vector3D a, Vector3D b) {
        return 2.0d * Math.atan(length((a.subtract(b)))/length((a.add(b))));

    }

    public double AngleBetween(DoubleMatrix a, DoubleMatrix b) {
        return 2.0d * Math.atan(length((a.sub(b)))/length((a.add(b))));

    }

    public double length(Vector3D a){

        return Math.sqrt(Math.pow(a.getX(), 2) + Math.pow(a.getY(), 2) + Math.pow(a.getZ(), 2));

    }

    public double length(DoubleMatrix a){

        return Math.sqrt(Math.pow(a.get(0,0), 2) + Math.pow(a.get(1,0), 2) + Math.pow(a.get(2,0), 2));

    }

    /**
     * Iterates all the .las files in pairs, computes the overlap percentage
     * and adds the > 0.0 overlap pairs to TreeMap<Double, int[]> pairs,
     * where the key is the negative overlap percentage. So when iterating
     * the treemap, the pairs with the highest overlap are seen first.
     */
    public void makePairs(){

        for(int i = 0; i < pointClouds.size()-1; i++){
            for(int j = i+1; j < pointClouds.size(); j++){

                overlap(pointClouds.get(i), pointClouds.get(j), i, j);

            }
        }
    }


    /**
     *
     */
    public void makeAlignOrder(){

        boolean done = false;

        while(!done){

            for(Double d : pairs.keySet()){

                //Neither of the las files are in the que, and this is the beginning
                if(!filesDone.contains(pairs.get(d)[0]) && !filesDone.contains(pairs.get(d)[1]) && pairs.get(d)[2] == 0 && order.size() == 0){
                    order.add(new int[]{pairs.get(d)[0], pairs.get(d)[1]});
                    pairs.get(d)[2] = 1;
                    filesDone.add(pairs.get(d)[0]);
                    filesDone.add(pairs.get(d)[1]);
                    includedPointsFile1.add(new HashSet<>());
                    includedPointsFile2.add(new HashSet<>());
                    break;
                }
                else if(filesDone.contains(pairs.get(d)[0]) &&  !filesDone.contains(pairs.get(d)[1]) && pairs.get(d)[2] == 0){
                    order.add(new int[]{pairs.get(d)[0], pairs.get(d)[1]});
                    pairs.get(d)[2] = 1;
                    filesDone.add(pairs.get(d)[1]);
                    includedPointsFile1.add(new HashSet<>());
                    includedPointsFile2.add(new HashSet<>());
                    break;
                }
                else if(!filesDone.contains(pairs.get(d)[0]) &&  filesDone.contains(pairs.get(d)[1]) && pairs.get(d)[2] == 0){
                    order.add(new int[]{pairs.get(d)[0], pairs.get(d)[1]});
                    pairs.get(d)[2] = 1;
                    filesDone.add(pairs.get(d)[0]);
                    includedPointsFile1.add(new HashSet<>());
                    includedPointsFile2.add(new HashSet<>());
                    break;

                }
            }

            if(filesDone.size() == pointClouds.size())
                done = true;
        }

    }

    /**
     * Return the overlap fraction of the extents of two input .las files.
     * Also, if the overlap fraction is greater than 0.0, the pair is added
     * to TreeMap<Double, int[]> pairs.
     * @param file1
     * @param file2
     * @param index1
     * @param index2
     * @return
     */
    public double overlap(LASReader file1, LASReader file2, int index1, int index2){

        double[] extentFile1 = new double[]{file1.getMinX(), file1.getMinY(), file1.getMaxX(), file1.getMaxY()};
        double[] extentFile2 = new double[]{file2.getMinX(), file2.getMinY(), file2.getMaxX(), file2.getMaxY()};

        double x_overlap = Math.max(0.0, Math.min(extentFile1[2], extentFile2[2]) - Math.max(extentFile1[0], extentFile2[0]));
        double y_overlap = Math.max(0.0, Math.min(extentFile1[3], extentFile2[3]) - Math.max(extentFile1[1], extentFile2[1]));

        double area = x_overlap * y_overlap;

        double output = (area / ( (extentFile1[2] - extentFile1[0]) * (extentFile1[3] - extentFile1[1]) ));

        /*If the .las file extents overlap */
        if(output > 0.0) {

            pairs.put(-output, new int[]{index1, index2, 0});

        }

        return output;

    }

    public void setOdir(){

    }
}
