package tools;

import LASio.*;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.ejml.data.DMatrixRMaj;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import utils.*;

import java.io.*;
import java.util.*;


/**
 *
 *
 *
 **/


public class stemDetector{

    double miniZ = 1.3;

    int noiseThreshold = 1;

    double minStemLength = 1.0;
    int minNumberOfPoints = 0;

    int allowedMisses = 2;

    LASReader pointCloud;
    LASReader pointCloud_thin;

    double y_interval = 0.5;
    double z_cutoff = 1.0;
    double z_cutoff_max = 50.0;
    short[][][] voxels;

    double cloudMaxX;
    double cloudMaxY;
    double cloudMinX;
    double cloudMinY;
    double cloudMaxZ;
    double cloudMinZ;

    int xDim;
    int yDim;
    int zDim;

    byte[][] layer;

    double maxStemDiameter = 0.5;

    int[][] stemIds;
    byte[][] progress;

    int coreNumber = 0;

    public float[] stemDiams;
    public float[] stemHeights;
    public boolean[] stemUnderstorey;

    public SimpleRegression regression = new SimpleRegression();

    HashMap<Integer, Integer> stemLocations = new HashMap<Integer, Integer>();

    ArrayList<treeStem> stems = new ArrayList<>();

    HashMap<Integer, surfacePatch> patches = new HashMap<>();
    ArrayList<surfacePatch> patchesList = new ArrayList<>();

    public LASReader pointCloudClassified;

    int min_n_voxels = 5;

    ArrayList<KdTree.XYZPoint> treePoints = new ArrayList<>();
    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();

    KdTree tree = new KdTree();

    int numberOfStems = 0;

    public argumentReader aR;

    org.tinfour.interpolation.TriangularFacetInterpolator polator;// = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);
    org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();


    File outputFile, outputFile2, outputFile3, outputFile4, outputFile5, outputFile6, outputFile7, outputFile_stemAlign;

    /**
     *
     * @param pointCloud
     * @param y_interval
     */
    public stemDetector(LASReader pointCloud, double y_interval, double z_cutoff, int coreNumber, argumentReader aR) throws IOException{


        /* PARAMS:

         */
        this.min_n_voxels = 10;


        this.aR = aR;
        this.y_interval = y_interval;

        aR.inclusionRule = new PointInclusionRule(false);
        thinPointCloud(pointCloud);

        this.coreNumber = coreNumber;
        //System.loadLibrary("opencv_java320");

        this.maxStemDiameter = Math.ceil(maxStemDiameter / y_interval) * 1.0;

        //System.out.println("Max stem diameter: " + this.maxStemDiameter);

        this.pointCloud = pointCloud;
        this.pointCloud_thin = new LASReader(this.pointCloud.getFile());
        this.cloudMaxX = pointCloud.getMaxX();
        this.cloudMaxY = pointCloud.getMaxY();
        this.cloudMinX = pointCloud.getMinX();
        this.cloudMinY = pointCloud.getMinY();
        this.cloudMinZ = pointCloud.getMinZ();
        this.cloudMaxZ = pointCloud.getMaxZ();

        this.z_cutoff_max = cloudMaxZ * (2.0/4.0);

        this.z_cutoff = z_cutoff;

        this.z_cutoff_max = 10.0;
        this.z_cutoff_max = 12.5;

        this.xDim = (int)Math.ceil((this.pointCloud.getMaxX() - this.pointCloud.getMinX()) / y_interval) + 1;
        this.yDim = (int)Math.ceil((this.pointCloud.getMaxY() - this.pointCloud.getMinY()) / y_interval) + 1;
        //this.zDim = (int)Math.ceil((this.pointCloud.getMaxZ() - this.pointCloud.getMinZ()) / y_interval) + 1;
        this.zDim = (int)Math.ceil((this.z_cutoff_max - this.pointCloud.getMinZ()) / y_interval) + 1;
        this.zDim = (int)Math.ceil((this.z_cutoff_max - 0) / y_interval) + 1;

        //voxels = new short[xDim][yDim][zDim];

        layer = new byte[xDim][yDim];

        progress = new byte[xDim][yDim];

        stemIds = new int[xDim][yDim];

        System.out.println("voxel memory requirement: " + (xDim * yDim * zDim * 1 / 2000000.0) + " MB");
        System.out.println("max diam: " + this.maxStemDiameter);

    }

    public void createPatches(VoxelNeighborhood[][][] in){

        System.out.println("xdim: " + this.xDim + "ydim: " + this.yDim + "zdim: " + this.zDim);

        //ArrayList<surfacePatch> patches = new ArrayList<>();
        int id = 1;

        for(int x = 1; x < this.xDim-1; x++)
            for(int y = 1; y < this.yDim-1; y++)
                for(int z = 1; z < this.zDim-1; z++) {
                    if(in[x][y][z] != null)
                        if (!in[x][y][z].isSurface) {

                            surfacePatch sp = new surfacePatch(-99);
                            searchNeighborhood(in, x, y, z, sp);

                            double ratio1 = (sp.maxX - sp.minX) / (sp.maxZ - sp.minZ);
                            double ratio2 = (sp.maxY - sp.minY) / (sp.maxZ - sp.minZ);

                            if(sp.voxels.size() > min_n_voxels && ratio1 < 0.30 && ratio2 < 0.30) {

                                sp.setId(id);
                                sp.label(id);
                                id++;
                                this.numberOfStems = sp.id;
                                //System.out.println(sp.id);
                                sp.calculateMidPoint();
                                patches.put(sp.id, sp);
                                patchesList.add(sp);

                                KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(sp.x, sp.y, 1);
                                tempTreePoint.setIndex(sp.id);
                                treePoints.add(tempTreePoint);
                                tree.add(tempTreePoint);

                            }else{
                                sp = null;
                            }
                        }
                }
    }

    public void searchNeighborhood(VoxelNeighborhood[][][] in, int x1, int y1, int z1, surfacePatch patch){

        if(in[x1][y1][z1].numberOfPointsInCenter < minNumberOfPoints)
            return;

        for (int x = -1; x < 2; x++)
            for (int y = -1; y < 2; y++)
                for (int z = -1; z < 2; z++){
                    if(x != 0 || y != 0 || z != 0)
                        if(x1 + x < this.xDim-1 && x1 + x > 0 &&
                                y1 + y < this.yDim-1 && y1 + y > 0 &&
                                z1 + z < this.zDim-1 && z1 + z > 0)
                            if(in[x1 + x][y1 + y][z1 + z] != null)
                                if(in[x1 + x][y1 + y][z1 + z].count > 1 && !in[x1 + x][y1 + y][z1 + z].isSurface && in[x1 + x][y1 + y][z1 + z].stem){

                                    //System.out.println("HERE!!");
                                    in[x1 + x][y1 + y][z1 + z].isSurface = true;
                                    in[x1 + x][y1 + y][z1 + z].id = patch.id;
                                    patch.addVoxel(in[x1 + x][y1 + y][z1 + z]);
                                    searchNeighborhood(in,x1 + x, y1 + y, z1 + z, patch);

                                }
                }
    }

    public void mergePatches(){

        List<KdTree.XYZPoint> nearest;


/*
        for(int i = 0 ; i < patchesList.size() - 1; i++){

            for(int j = i + 1; j < patchesList.size(); j++){

                double distance = euclideanDistance(patchesList.get(i).x, patchesList.get(i).y,
                        patchesList.get(j).x, patchesList.get(j).y);

                if(distance < 1.0){

                    patches.get(j).merge(patches.get(i));
                    mergeMap.put((short)patches.get(j).id, (short)patches.get(i).id);
                    //System.out.println("MERGED");
                }

            }

        }

 */


        for(int i = 0 ; i < treePoints.size(); i++){

            if(treePoints.get(i).getIndex() < 0)
                continue;

            nearest = (List<KdTree.XYZPoint>)tree.nearestNeighbourSearch(2, treePoints.get(i));

            if(nearest.get(nearest.size()-1).getIndex() < 0 || treePoints.get(i).getIndex() < 0)
                continue;

            double distance = euclideanDistance(treePoints.get(i).getX(), treePoints.get(i).getY(),
                    nearest.get(nearest.size()-1).getX(), nearest.get(nearest.size()-1).getY());

            int neighs = 1;

            while(distance < (0.5 / y_interval)){


                patches.get(treePoints.get(i).getIndex()).merge(patches.get(nearest.get(nearest.size()-1).getIndex()));

                nearest.get(nearest.size()-1).setIndex(-1);

                nearest = (List<KdTree.XYZPoint>)tree.nearestNeighbourSearch(2 + neighs++, treePoints.get(i));



                if(nearest.get(nearest.size()-1).getIndex() < 0)
                    break;

                distance = euclideanDistance(treePoints.get(i).getX(), treePoints.get(i).getY(),
                        nearest.get(nearest.size()-1).getX(), nearest.get(nearest.size()-1).getY());


            }
        }
    }

    public void removeShort(){


        for(int i : patches.keySet()){

            if(patches.get(i).maxZ - patches.get(i).minZ < (1.0 / y_interval)){
                patches.get(i).renderUseles();
            }
/*
            else if(patches.get(i).maxX - patches.get(i).minX > (1.0 / y_interval)){
                patches.get(i).renderUseles();
            }
            else if(patches.get(i).maxY - patches.get(i).minY > (1.0 / y_interval)){
                patches.get(i).renderUseles();
            }

 */
        }
    }

    public void thinPointCloud(LASReader pointCloud) throws IOException{

        aR.step = this.y_interval;
        System.out.println(this.y_interval);
        aR.few = 10;
        aR.thin3d = true;
        aR.cores = 1;
        aR.p_update = new progressUpdater(aR);

        //Thinner thin = new Thinner(pointCloud, aR.step, aR, 1);


        //this.pointCloud_thin = new LASReader(thin.outputFile);
        //this.pointCloud_thin = new LASReader(this.pointCloud.getFile());
    }

    /**
     * Creates a deluney TIN from point cloud.
     *
     * @param groundClassification 	Ground class
     */

    public void createTin(int groundClassification) throws IOException{

        LasPoint tempPoint = new LasPoint();

        long n = pointCloud_thin.getNumberOfPointRecords();

        for(int i = 0; i < n; i++){

            pointCloud_thin.readRecord(i, tempPoint);

            if(tempPoint.classification == groundClassification)
                tin.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));


        }

        polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);

        int maxi;
        double minz = Double.POSITIVE_INFINITY;

        for(int i = 0; i < n; i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(n - i));

            try {
                pointCloud_thin.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {
                //Sstem.out.println(j);
                //long[] temppi = new long[2];

                pointCloud_thin.readFromBuffer(tempPoint);
                tempPoint.z -= polator.interpolate(tempPoint.x, tempPoint.y, valuator);

                if(tempPoint.z < minz){
                    minz = tempPoint.z;
                }

            }
        }

        n = pointCloud.getNumberOfPointRecords();
        for(int i = 0; i < n; i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(n - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int j = 0; j < maxi; j++) {
                //Sstem.out.println(j);
                //long[] temppi = new long[2];

                pointCloud.readFromBuffer(tempPoint);
                tempPoint.z -= polator.interpolate(tempPoint.x, tempPoint.y, valuator);

                if(tempPoint.z < minz){
                    minz = tempPoint.z;
                }

            }
        }

        this.cloudMinZ = minz;
        this.miniZ = minz;

        this.zDim = (int)Math.ceil((this.z_cutoff_max - this.cloudMinZ) / y_interval) + 1;
        this.zDim = (int)Math.ceil((this.z_cutoff_max - this.cloudMinZ) / y_interval) + 1;
        //System.out.println("zdim " + this.zDim);
    }

    public double groundLevel(LasPoint p){

        return polator.interpolate(p.x, p.y, valuator);

    }

    public void detect(boolean dz) throws Exception {

        if(!dz){
            //System.out.println("creating tin");
            createTin(2);
        }

        double bucketSize = 0.1;

        //double resolution = 0.25;
        double resolution = 1.0;

        int xDim1 = (int)Math.ceil((this.pointCloud.getMaxX() - this.pointCloud.getMinX()) / resolution) + 1;
        int yDim1 = (int)Math.ceil((this.pointCloud.getMaxY() - this.pointCloud.getMinY()) / resolution) + 1;
        //int zDim1 = (int)Math.ceil((this.pointCloud.getMaxZ() - this.pointCloud.getMinZ()) / resolution) + 1;

        float[][] raster = new float[xDim1][yDim1];
        float[][] raster_dz = new float[xDim1][yDim1];


        long n = pointCloud_thin.getNumberOfPointRecords();
        LasPoint tempPoint = new LasPoint();

        int maxITDid = 0;
        System.out.println(zDim);
        voxels = new short[xDim][yDim][zDim];
        VoxelNeighborhood[][][] surface = new VoxelNeighborhood[this.xDim][this.yDim][this.zDim];
/*
        for(int x = 0; x < this.xDim; x++)
            for(int y = 0; y < this.yDim; y++)
                for(int z = 0; z < this.zDim; z++) {
                    surface[x][y][z] = new VoxelNeighborhood();
                }

*/
        int maxi;

        for(int i = 0; i < n; i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(n - i));

            try {
                pointCloud_thin.readRecord_noRAF(i, tempPoint, 10000);
            }catch (Exception e){
                e.printStackTrace();
            }
            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {
                //Sstem.out.println(j);
                //long[] temppi = new long[2];

                pointCloud_thin.readFromBuffer(tempPoint);

                int xCoord = (int) Math.floor((tempPoint.x - pointCloud.getMinX()) / resolution);
                int yCoord = (int) Math.floor((pointCloud.getMaxY() - tempPoint.y) / resolution);

                if(!dz){

                    raster_dz[xCoord][yCoord] = (float)groundLevel(tempPoint);
                    tempPoint.z -= polator.interpolate(tempPoint.x, tempPoint.y, valuator); //= tempPoint.z - raster_dz[xCoord][yCoord];

                }

                if(tempPoint.z > raster[xCoord][yCoord])
                    raster[xCoord][yCoord] = (float)tempPoint.z;



                if (tempPoint.pointSourceId > maxITDid)
                    maxITDid = tempPoint.pointSourceId;

                /* && tempPoint.pointSourceId > 0 means that we work only with ITD segments */
                if (tempPoint.z >= z_cutoff && tempPoint.z < z_cutoff_max){// && tempPoint.pointSourceId > 0) {

                    int xLocation = (int) Math.floor((tempPoint.x - cloudMinX) / y_interval);
                    int yLocation = (int) Math.floor((cloudMaxY - tempPoint.y) / y_interval);
                    int zLocation = (int) Math.floor((tempPoint.z - cloudMinZ) / y_interval);


/*
                    if (xLocation < this.xDim - 1 && xLocation > 0 &&
                            yLocation < this.yDim - 1 && yLocation > 0 &&
                            zLocation < this.zDim - 1 && zLocation > 0) {

                        for (int x = -1; x < 2; x++)
                            for (int y = -1; y < 2; y++)
                                for (int z = -1; z < 2; z++)
                                    surface[xLocation + x][yLocation + y][zLocation + z].addObservable();
                    }
*/
                    if (voxels[xLocation][yLocation][zLocation] < Byte.MAX_VALUE)
                        voxels[xLocation][yLocation][zLocation]++;

                }
            }
        }
        System.out.println("DONE 1");
        for(int i = 0; i < n; i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(n - i));

            try {
                pointCloud_thin.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {
                //Sstem.out.println(j);
                //long[] temppi = new long[2];

                pointCloud_thin.readFromBuffer(tempPoint);

                if (tempPoint.pointSourceId > maxITDid)
                    maxITDid = tempPoint.pointSourceId;

                int xCoord = (int) Math.floor((tempPoint.x - pointCloud.getMinX()) / resolution);
                int yCoord = (int) Math.floor((pointCloud.getMaxY() - tempPoint.y) / resolution);

                if(!dz){

                    //raster_dz[xCoord][yCoord] = (float)groundLevel(tempPoint);
                    tempPoint.z -= polator.interpolate(tempPoint.x, tempPoint.y, valuator); //= tempPoint.z - raster_dz[xCoord][yCoord];

                }

                /* && tempPoint.pointSourceId > 0 means that we work only with ITD segments */
                if (tempPoint.z >= z_cutoff && tempPoint.z < z_cutoff_max){// tempPoint.pointSourceId > 0) {

                    int xLocation = (int) Math.floor((tempPoint.x - cloudMinX) / y_interval);
                    int yLocation = (int) Math.floor((cloudMaxY - tempPoint.y) / y_interval);
                    int zLocation = (int) Math.floor((tempPoint.z - cloudMinZ) / y_interval);

                    if (xLocation < this.xDim - 1 && xLocation > 0 &&
                            yLocation < this.yDim - 1 && yLocation > 0 &&
                            zLocation < this.zDim - 1 && zLocation > 0) {

                        if(surface[xLocation][yLocation][zLocation] != null)
                        if(surface[xLocation][yLocation][zLocation].numberOfPointsInCenter < Byte.MAX_VALUE){
                            surface[xLocation][yLocation][zLocation].numberOfPointsInCenter++;
                        }

                        for (int x = -1; x < 2; x++)
                            for (int y = -1; y < 2; y++)
                                for (int z = -1; z < 2; z++) {
                                    if (voxels[xLocation + x][yLocation + y][zLocation + z] > 0 && surface[xLocation + x][yLocation + y][zLocation + z] == null) {
                                        surface[xLocation + x][yLocation + y][zLocation + z] = new VoxelNeighborhood(xLocation + x,yLocation + y, zLocation + z);
                                        surface[xLocation + x][yLocation + y][zLocation + z].addObservable();
                                    } else if (voxels[xLocation + x][yLocation + y][zLocation + z] > 0 && surface[xLocation + x][yLocation + y][zLocation + z] != null) {
                                        //System.out.println("HERE!");
                                        surface[xLocation + x][yLocation + y][zLocation + z].addObservable();
                                    }
                                }
                    }
                    /*
                    if(voxels[xLocation][yLocation][zLocation] > 2 && surface[xLocation][yLocation][zLocation] == null){
                        surface[xLocation][yLocation][zLocation] = new VoxelNeighborhood();
                        surface[xLocation][yLocation][zLocation].addObservable();
                    }
                    else if(voxels[xLocation][yLocation][zLocation] > 2 && surface[xLocation][yLocation][zLocation] != null)
                        surface[xLocation][yLocation][zLocation].addObservable();

                     */
                }
            }
        }
        System.out.println("DONE 2");
        int counter123 = 0;
        int maxCount = 0;
        for(int x = 0; x < this.xDim; x++)
            for(int y = 0; y < this.yDim; y++)
                for(int z = 0; z < this.zDim; z++)
                    if(surface[x][y][z] != null) {


                        surface[x][y][z].prepare();
                        if(surface[x][y][z].capacity > maxCount)
                            maxCount = surface[x][y][z].capacity;

                        //if(counter123 % 1000 == 0)
                          //  System.out.println(counter123 + " / " + (this.xDim * this.yDim * this.zDim) + " " + surface[x][y][z].capacity + " " + maxCount);

                        counter123++;
                    }


        for(int i = 0; i < n; i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(n - i));

            try {
                pointCloud_thin.readRecord_noRAF(i, tempPoint, 10000);
            }catch (Exception e){
                e.printStackTrace();
            }
            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {
                //Sstem.out.println(j);
                //long[] temppi = new long[2];

                pointCloud_thin.readFromBuffer(tempPoint);
                //pointCloud.readRecord(i, tempPoint);

                if(!dz){

                    //raster_dz[xCoord][yCoord] = (float)groundLevel(tempPoint);
                    tempPoint.z -= polator.interpolate(tempPoint.x, tempPoint.y, valuator); //= tempPoint.z - raster_dz[xCoord][yCoord];

                }

                /* && tempPoint.pointSourceId > 0 means that we work only with ITD segments */
                if (tempPoint.z >= z_cutoff && tempPoint.z < z_cutoff_max){// tempPoint.pointSourceId > 0) {

                    int xLocation = (int) Math.floor((tempPoint.x - cloudMinX) / y_interval);
                    int yLocation = (int) Math.floor((cloudMaxY - tempPoint.y) / y_interval);
                    int zLocation = (int) Math.floor((tempPoint.z - cloudMinZ) / y_interval);

                    if (xLocation < this.xDim - 1 && xLocation > 0 &&
                            yLocation < this.yDim - 1 && yLocation > 0 &&
                            zLocation < this.zDim - 1 && zLocation > 0) {

                        for (int x = -1; x < 2; x++)
                            for (int y = -1; y < 2; y++)
                                for (int z = -1; z < 2; z++)
                                    if(surface[xLocation + x][yLocation + y][zLocation + z] != null)
                                        surface[xLocation + x][yLocation + y][zLocation + z].addPoint(tempPoint.x, tempPoint.y, tempPoint.z, i+j);
                    }
                }
            }
        }

        int end = xDim * yDim * zDim;
        int counter22 = 0;

        HashSet<Integer> goodIndices = new HashSet<>();

        if(false)
        for(int x = 0; x < this.xDim; x++)
            for(int y = 0; y < this.yDim; y++)
                for(int z = 0; z < this.zDim; z++) {
                    if(surface[x][y][z] != null) {

                        surface[x][y][z].calculateNormal();
/*
                        if(surface[x][y][z].stem) {
                            surface[x][y][z].filter();

                            goodIndices.addAll(surface[x][y][z].goodIndexes);
                            //if (++counter22 % 1000 == 0)
                              //  System.out.println(counter22 + " / " + (xDim * yDim * zDim) + " g: " + goodIndices.size());
                        }
*/
                        surface[x][y][z].release();
                    }
                }


        System.out.println("CREATING PATCHES:!");
        createPatches(surface);
        mergePatches();

        System.gc();
        System.gc();
        System.gc();
        System.gc();

        if(outputFile.exists())
            outputFile.delete();

        outputFile.createNewFile();

        PointInclusionRule rule = new PointInclusionRule();

        LASraf asd2 = new LASraf(outputFile);

        LASwrite.writeHeader(asd2, "lasStem", pointCloud.versionMajor, pointCloud.versionMinor,
                pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength,
                pointCloud.headerSize, pointCloud.offsetToPointData, pointCloud.numberVariableLengthRecords,
                pointCloud.fileSourceID, pointCloud.globalEncoding,
                pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset);
        int pointCount = 0;
/*
        for(int i = 0; i < n; i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(n - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {
                //Sstem.out.println(j);
                //long[] temppi = new long[2];


                pointCloud.readFromBuffer(tempPoint);

                //if(tempPoint.z >= 0.2 && tempPoint.z < z_cutoff_max) {

                    int xLocation = (int) Math.floor((tempPoint.x - cloudMinX) / y_interval);
                    int yLocation = (int) Math.floor((cloudMaxY - tempPoint.y) / y_interval);
                    int zLocation = (int) Math.floor((tempPoint.z - cloudMinZ) / y_interval);

                    if(surface[xLocation][yLocation][zLocation] != null) {

                        if (surface[xLocation][yLocation][zLocation].isSurface && surface[xLocation][yLocation][zLocation].id >= 0) {

                            tempPoint.pointSourceId = (short) surface[xLocation][yLocation][zLocation].id;
                            tempPoint.classification = 2;


                        } else {
                            tempPoint.pointSourceId = 0;
                            tempPoint.classification = 0;
                        }
                    }
                    else{
                        tempPoint.pointSourceId = 0;
                        tempPoint.classification = 0;
                    }
                if (asd2.writePoint(tempPoint, rule, pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                        pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, i))
                    pointCount++;
                //}
            }
        }
*/
        asd2.writeBuffer2();
        asd2.updateHeader2();

        //outputFile = new File("stems_" + coreNumber + ".las");

        if(outputFile.exists())
            outputFile.delete();

        outputFile.createNewFile();

        //PointInclusionRule rule = new PointInclusionRule();

        //asd2 = new LASraf(outputFile);

        int thread_n = aR.pfac.addReadThread(pointCloud);

        pointWriterMultiThread pw = new pointWriterMultiThread(outputFile, pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);
        /*
        LASwrite.writeHeader(asd2, "lasStem", pointCloud.versionMajor, pointCloud.versionMinor,
                pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength,
                pointCloud.headerSize, pointCloud.offsetToPointData, pointCloud.numberVariableLengthRecords,
                pointCloud.fileSourceID, pointCloud.globalEncoding,
                pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset);

        pointCount = 0;
         */

        HashMap<Short, Short> trees = new HashMap<>();

        HashMap<Short, HashSet<Short>> treesBank = new HashMap<>();

        int[] stemPointCounts = new int[this.numberOfStems + 1];
        this.stemUnderstorey = new boolean[this.numberOfStems + 1];

        boolean[] doubleStems = new boolean[this.numberOfStems];

        int[] stemITDid = new int[this.numberOfStems + 1];

        HashSet<Short> idsAlreadyDone = new HashSet<>();
        int unders = 0;
        n = pointCloud.getNumberOfPointRecords();

        /* HERE WE ARE WRITING STEMS TO A FILE */
        //for(int i = 0; i < n; i++){

            //pointCloud.readRecord(i, tempPoint);
        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000) {

            maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));


            //in.readRecord_noRAF(i, tempPoint, 10000);
            aR.pfac.prepareBuffer(thread_n, i, maxi);

            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);
                //if(goodIndices.contains(i))

                if (!dz) {

                    //raster_dz[xCoord][yCoord] = (float)groundLevel(tempPoint);
                    tempPoint.z -= polator.interpolate(tempPoint.x, tempPoint.y, valuator); //= tempPoint.z - raster_dz[xCoord][yCoord];

                }

                if (tempPoint.z >= z_cutoff && tempPoint.z < z_cutoff_max) {// && tempPoint.pointSourceId > 0) {

                    int xLocation = (int) Math.floor((tempPoint.x - cloudMinX) / y_interval);
                    int yLocation = (int) Math.floor((cloudMaxY - tempPoint.y) / y_interval);
                    int zLocation = (int) Math.floor((tempPoint.z - cloudMinZ) / y_interval);

                    //if(voxels[xLocation][yLocation][zLocation] < 0) {
                    if (surface[xLocation][yLocation][zLocation] != null) {

                        if (surface[xLocation][yLocation][zLocation].isSurface && surface[xLocation][yLocation][zLocation].id >= 0) {

                            /* IF THE ITD TREE HAS NOT YET BEEN OBSERVED IN THE POINTS */
                            if (!treesBank.containsKey(tempPoint.pointSourceId)) { //   && !idsAlreadyDone.contains((short)surface[xLocation][yLocation][zLocation].id )
                                //if(!trees.containsKey(tempPoint.pointSourceId)){

                                //idsAlreadyDone.add((short)surface[xLocation][yLocation][zLocation].id );

                                treesBank.put(tempPoint.pointSourceId, new HashSet<>());
                                treesBank.get(tempPoint.pointSourceId).add((short) surface[xLocation][yLocation][zLocation].id);
                                //trees.put(tempPoint.pointSourceId, (short)-voxels[xLocation][yLocation][zLocation]);
                                //doubleStems[tempPoint.pointSourceId] = true;
                                //doubleStemId[]
                                //tempPoint.pointSourceId = (short)-voxels[xLocation][yLocation][zLocation];

                            } //else if(!idsAlreadyDone.contains((short)surface[xLocation][yLocation][zLocation].id )) {

                            else if (!treesBank.get(tempPoint.pointSourceId).contains((short) surface[xLocation][yLocation][zLocation].id)) {
                                unders++;
                                treesBank.get(tempPoint.pointSourceId).add((short) surface[xLocation][yLocation][zLocation].id);
                                //System.out.println("Understorey tree! " + treesBank.get(tempPoint.pointSourceId).size());
                            }
                            //tempPoint.pointSourceId = trees.get(tempPoint.pointSourceId);
                            // }

                            //System.out.println(xLocation + " " + yLocation + " " + zLocation);
                            //System.out.println(surface[xLocation][yLocation][zLocation].id);
                            stemITDid[(short) surface[xLocation][yLocation][zLocation].id] = tempPoint.pointSourceId;

                            tempPoint.pointSourceId = (short) surface[xLocation][yLocation][zLocation].id;

                            //if (asd2.writePoint(tempPoint, rule, pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                             //       pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, i))
                             //   pointCount++;
                            aR.pfac.writePoint(tempPoint, i + j, thread_n);
                            stemPointCounts[tempPoint.pointSourceId]++;

                        }
                    }
                }
            }
        }

        aR.pfac.closeThread(thread_n);
        System.out.println("UNDERS: " + unders + " ITD trees: " + treesBank.size());

        HashSet<Short> badStems = new HashSet<>();

        for(int i = 0; i < stemPointCounts.length; i++)
            if(stemPointCounts[i] < 50){
                //System.out.println("BAD STEM!!!");
                badStems.add((short)i);
            }else{
                //System.out.println("GOOD STEM!");
            }


        pointCount = 0;

        asd2.writeBuffer2();
        asd2.updateHeader2();

        LASReader pointCloud2 = new LASReader(outputFile);

        n = pointCloud2.getNumberOfPointRecords();

        ArrayList<double[]>[] group = (ArrayList<double[]>[])new ArrayList[stemPointCounts.length];

        ArrayList<Integer>[] group_indexes = (ArrayList<Integer>[])new ArrayList[stemPointCounts.length];

        for(int i = 0; i < stemPointCounts.length; i++){
            group[i] = new ArrayList<double[]>();
            group_indexes[i] = new ArrayList<Integer>();
            group[i].add(new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY});
        }

        LevenbergMarquardt lm = new LevenbergMarquardt(1);

        lm.setDelta(0.01);
        ResidFunctionCylinderFit rs = new ResidFunctionCylinderFit();

        DMatrixRMaj resid = new DMatrixRMaj(1, 1);
        DMatrixRMaj param = new DMatrixRMaj(7, 1);

        int counter = 0;

        double meanDiameter = 0.0;
        double count = 0;

        if(outputFile2.exists())
            outputFile2.delete();

        outputFile2.createNewFile();

        LASraf asd3 = new LASraf(outputFile2);

        LASwrite.writeHeader(asd3, "lasStem", pointCloud2.versionMajor, pointCloud2.versionMinor,
                pointCloud2.pointDataRecordFormat, pointCloud2.pointDataRecordLength,
                pointCloud2.headerSize, pointCloud2.offsetToPointData, pointCloud.numberVariableLengthRecords,
                pointCloud.fileSourceID, pointCloud.globalEncoding,
                pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset);

        int n2;

        short tempUid;

        this.stemDiams = new float[this.numberOfStems + 1];
        this.stemHeights = new float[this.numberOfStems + 1];

        FileWriter fw = null;
        BufferedWriter bw = null;//
        PrintWriter out = null;

        FileWriter fw_ransac = null;
        BufferedWriter bw_ransac = null;//
        PrintWriter out_ransac = null;

        FileWriter fw_diam = null;
        BufferedWriter bw_diam = null;//
        PrintWriter out_diam = null;

        FileWriter fw_diam_ransac = null;
        BufferedWriter bw_diam_ransac = null;//
        PrintWriter out_diam_ransac = null;


        //File outFile = new File("cylinderTest.txt");

        if(outputFile4.exists())
            outputFile4.delete();

        outputFile4.createNewFile();

        if(outputFile5.exists())
            outputFile5.delete();

        outputFile5.createNewFile();

        if(outputFile6.exists())
            outputFile6.delete();

        outputFile6.createNewFile();

        if(outputFile7.exists())
            outputFile7.delete();

        outputFile7.createNewFile();

        if(outputFile_stemAlign.exists())
            outputFile_stemAlign.delete();

        outputFile_stemAlign.createNewFile();

        try{
            fw  = new FileWriter(outputFile4, true);
            bw  = new BufferedWriter(fw);
            out  = new PrintWriter(bw);

            fw_ransac  = new FileWriter(outputFile5, true);
            bw_ransac  = new BufferedWriter(fw_ransac);
            out_ransac  = new PrintWriter(bw_ransac);

            fw_diam  = new FileWriter(outputFile6, true);
            bw_diam  = new BufferedWriter(fw_diam);
            out_diam  = new PrintWriter(bw_diam);

            fw_diam_ransac  = new FileWriter(outputFile7, true);
            bw_diam_ransac  = new BufferedWriter(fw_diam_ransac);
            out_diam_ransac  = new PrintWriter(bw_diam_ransac);

        }catch (Exception e){
            e.printStackTrace();
        }

        for(int i = 0; i < n; i++) {


            pointCloud2.readRecord(i, tempPoint);

            stemPointCounts[tempPoint.pointSourceId]--;

            //System.out.println(stemPointCounts[tempPoint.pointSourceId]);
            group[tempPoint.pointSourceId].add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z});

            if(tempPoint.x < group[tempPoint.pointSourceId].get(0)[0]){
                group[tempPoint.pointSourceId].get(0)[0] = tempPoint.x;
            }
            if(tempPoint.x > group[tempPoint.pointSourceId].get(0)[1]){
                group[tempPoint.pointSourceId].get(0)[1] = tempPoint.x;
            }

            if(tempPoint.y < group[tempPoint.pointSourceId].get(0)[2]){
                group[tempPoint.pointSourceId].get(0)[2] = tempPoint.y;
            }
            if(tempPoint.y > group[tempPoint.pointSourceId].get(0)[3]){
                group[tempPoint.pointSourceId].get(0)[3] = tempPoint.y;
            }

            if(tempPoint.z < group[tempPoint.pointSourceId].get(0)[4]){
                group[tempPoint.pointSourceId].get(0)[4] = tempPoint.z;
            }
            if(tempPoint.z > group[tempPoint.pointSourceId].get(0)[5]){
                group[tempPoint.pointSourceId].get(0)[5] = tempPoint.z;
            }

            group_indexes[tempPoint.pointSourceId].add(i);


            if(stemPointCounts[tempPoint.pointSourceId] == 0 && !badStems.contains(tempPoint.pointSourceId)){

                double startx = (group[tempPoint.pointSourceId].get(0)[0] + (group[tempPoint.pointSourceId].get(0)[1] - group[tempPoint.pointSourceId].get(0)[0]) / 2.0);
                double starty = (group[tempPoint.pointSourceId].get(0)[2] + (group[tempPoint.pointSourceId].get(0)[3] - group[tempPoint.pointSourceId].get(0)[2]) / 2.0);
                double startz = (group[tempPoint.pointSourceId].get(0)[4]);


                param.set(0,0, (group[tempPoint.pointSourceId].get(0)[0] + (group[tempPoint.pointSourceId].get(0)[1] - group[tempPoint.pointSourceId].get(0)[0]) / 2.0));
                param.set(1,0, (group[tempPoint.pointSourceId].get(0)[2] + (group[tempPoint.pointSourceId].get(0)[3] - group[tempPoint.pointSourceId].get(0)[2]) / 2.0));
                param.set(2,0, (group[tempPoint.pointSourceId].get(0)[4]));

                param.set(3,0, (group[tempPoint.pointSourceId].get(0)[0] + (group[tempPoint.pointSourceId].get(0)[1] - group[tempPoint.pointSourceId].get(0)[0]) / 2.0));
                param.set(4,0, (group[tempPoint.pointSourceId].get(0)[2] + (group[tempPoint.pointSourceId].get(0)[3] - group[tempPoint.pointSourceId].get(0)[2]) / 2.0));
                param.set(5,0, (group[tempPoint.pointSourceId].get(0)[5]));
                double maxiz = (group[tempPoint.pointSourceId].get(0)[5]);

                //maxiz = (group[tempPoint.pointSourceId].get(0)[4]) + ((group[tempPoint.pointSourceId].get(0)[5]) - (group[tempPoint.pointSourceId].get(0)[4])) * 0.10;

                double range = group[tempPoint.pointSourceId].get(0)[5] - (group[tempPoint.pointSourceId].get(0)[4]);


                int numberOfBuckets = (int)(range / bucketSize) + 1;



                maxiz = Math.max(1.3, maxiz);
                param.set(5,0, maxiz );

                param.set(6,0, 0.1);
                //param.set(7,0, 0.20);

                rs.setCylinder(group[tempPoint.pointSourceId].get(0)[0], group[tempPoint.pointSourceId].get(0)[1],
                        group[tempPoint.pointSourceId].get(0)[2], group[tempPoint.pointSourceId].get(0)[3],
                        group[tempPoint.pointSourceId].get(0)[4], maxiz);

                ArrayList<double[]> pointsForCylinderFit = new ArrayList<double[]>();

                String outtistring2 = "-99.000\t-99.000\t-99.000";
                String outtistring22 = "-99.000\t-99.000\t-99.000\t-99.000";
                String outtistring222 = "-99.000\t-99.000\t-99.000\t-99.000\t-99.000\t-99.000\t-99.000\t-99.000";

                out.println(outtistring22);
                out_ransac.println(outtistring222);
                out_diam_ransac.println(outtistring2);
                out_diam.println(outtistring222);

                double max_x = Double.MIN_VALUE;
                double min_x = Double.MAX_VALUE;

                double max_y = Double.MIN_VALUE;
                double min_y = Double.MAX_VALUE;

                double max_z = Double.MIN_VALUE;
                double min_z = Double.MAX_VALUE;

                double xSum = 0.0;
                double ySum = 0.0;
                double zSum = 0.0;
                double counterHere = 0.0d;

                ArrayList<String> outputti = new ArrayList<>();
                ArrayList<String> outputti_cylinders = new ArrayList<>();

                ArrayList<double[]>[] pointsInBuckets = new ArrayList[numberOfBuckets];
                ArrayList<double[]> bucketMetrics = new ArrayList();

                for(int g = 0; g < pointsInBuckets.length; g++) {
                    pointsInBuckets[g] = new ArrayList<>();
                    bucketMetrics.add(new double[]{0,0,0,0});
                }

                for(int g = 1; g < group[tempPoint.pointSourceId].size(); g++){

                    int bucket = (int)Math.floor( (group[tempPoint.pointSourceId].get(g)[2] - startz) / bucketSize );

                    if(group[tempPoint.pointSourceId].get(g)[2] <= maxiz && group[tempPoint.pointSourceId].get(g)[2] >= (group[tempPoint.pointSourceId].get(0)[4])){

                        pointsInBuckets[bucket].add(group[tempPoint.pointSourceId].get(g));


                        String outtistring = group[tempPoint.pointSourceId].get(g)[0] + "\t" + group[tempPoint.pointSourceId].get(g)[1]
                                + "\t" + group[tempPoint.pointSourceId].get(g)[2] + "\t" + (bucket);

                        outputti.add(outtistring);

                        bucketMetrics.get(bucket)[0]++;
                        bucketMetrics.get(bucket)[1] += group[tempPoint.pointSourceId].get(g)[0];
                        bucketMetrics.get(bucket)[2] += group[tempPoint.pointSourceId].get(g)[1];
                        bucketMetrics.get(bucket)[3] += group[tempPoint.pointSourceId].get(g)[2];


                        if(bucket - 1 >= 0){
                            pointsInBuckets[bucket-1].add(group[tempPoint.pointSourceId].get(g));
                            bucketMetrics.get(bucket-1)[0]++;
                            bucketMetrics.get(bucket-1)[1] += group[tempPoint.pointSourceId].get(g)[0];
                            bucketMetrics.get(bucket-1)[2] += group[tempPoint.pointSourceId].get(g)[1];
                            bucketMetrics.get(bucket-1)[3] += group[tempPoint.pointSourceId].get(g)[2];

                        }

                        if(bucket + 1 < pointsInBuckets.length){
                            pointsInBuckets[bucket+1].add(group[tempPoint.pointSourceId].get(g));
                            bucketMetrics.get(bucket+1)[0]++;
                            bucketMetrics.get(bucket+1)[1] += group[tempPoint.pointSourceId].get(g)[0];
                            bucketMetrics.get(bucket+1)[2] += group[tempPoint.pointSourceId].get(g)[1];
                            bucketMetrics.get(bucket+1)[3] += group[tempPoint.pointSourceId].get(g)[2];
                        }


                        if(bucket - 2 >= 0){
                            pointsInBuckets[bucket-2].add(group[tempPoint.pointSourceId].get(g));
                            bucketMetrics.get(bucket-2)[0]++;
                            bucketMetrics.get(bucket-2)[1] += group[tempPoint.pointSourceId].get(g)[0];
                            bucketMetrics.get(bucket-2)[2] += group[tempPoint.pointSourceId].get(g)[1];
                            bucketMetrics.get(bucket-2)[3] += group[tempPoint.pointSourceId].get(g)[2];
                        }

                        if(bucket + 2 < pointsInBuckets.length){
                            pointsInBuckets[bucket+2].add(group[tempPoint.pointSourceId].get(g));
                            bucketMetrics.get(bucket+2)[0]++;
                            bucketMetrics.get(bucket+2)[1] += group[tempPoint.pointSourceId].get(g)[0];
                            bucketMetrics.get(bucket+2)[2] += group[tempPoint.pointSourceId].get(g)[1];
                            bucketMetrics.get(bucket+2)[3] += group[tempPoint.pointSourceId].get(g)[2];
                        }



                        if(group[tempPoint.pointSourceId].get(g)[0] > max_x)
                            max_x = group[tempPoint.pointSourceId].get(g)[0];
                        if(group[tempPoint.pointSourceId].get(g)[0] < min_x)
                            min_x = group[tempPoint.pointSourceId].get(g)[0];

                        if(group[tempPoint.pointSourceId].get(g)[1] > max_y)
                            max_y = group[tempPoint.pointSourceId].get(g)[1];
                        if(group[tempPoint.pointSourceId].get(g)[1] < min_y)
                            min_y = group[tempPoint.pointSourceId].get(g)[1];

                        if(group[tempPoint.pointSourceId].get(g)[2] > max_z)
                            max_z = group[tempPoint.pointSourceId].get(g)[2];
                        if(group[tempPoint.pointSourceId].get(g)[2] < min_z)
                            min_z = group[tempPoint.pointSourceId].get(g)[2];

                        xSum += group[tempPoint.pointSourceId].get(g)[0];
                        ySum += group[tempPoint.pointSourceId].get(g)[1];
                        zSum += group[tempPoint.pointSourceId].get(g)[2];

                        counterHere++;

                        pointsForCylinderFit.add(group[tempPoint.pointSourceId].get(g));
                    }

                }


                if(counterHere < 30){
                    badStems.add(tempPoint.pointSourceId);
                    continue;
                }

                double x_mean = xSum / counterHere;
                double y_mean = ySum / counterHere;
                double z_mean = zSum / counterHere;

                double xx = min_x + (max_x - min_x) / 2.0;
                double yy = min_y + (max_y - min_y) / 2.0;
                double zz = min_z + (max_z - min_z) / 2.0;

                x_mean = (x_mean + xx) / 2.0;
                y_mean = (y_mean + yy) / 2.0;

                param.set(0,0, x_mean);
                param.set(1,0, y_mean);
                param.set(2,0, min_z);

                param.set(3,0, x_mean);
                param.set(4,0, y_mean);
                param.set(5,0, maxiz);

                SplineInterpolator interpolatori = new SplineInterpolator();
                PolynomialSplineFunction stemFunction;
                double[] x_segments;

                int xCoord = (int) Math.floor((x_mean - pointCloud.getMinX()) / resolution);
                int yCoord = (int) Math.floor((pointCloud.getMaxY() - y_mean) / resolution);

                double zet = raster[xCoord][yCoord];

                SimpleRegression simpleRegression = new SimpleRegression(true);

                simpleRegression.addData(zet, 0);

                ArrayList<double[]> dataForRansac = new ArrayList<>();
                ArrayList<double[]> ransacCoordinates = new ArrayList<>();

                ArrayList<String> outputti_diam = new ArrayList<>();
                ArrayList<String> outputti_diam_ransac = new ArrayList<>();

                int bucketAverageSize = 0;

                for(int g = 0; g < pointsInBuckets.length; g++){

                    bucketAverageSize += pointsInBuckets[g].size();

                    x_mean = bucketMetrics.get(g)[1] / bucketMetrics.get(g)[0];
                    y_mean = bucketMetrics.get(g)[2] / bucketMetrics.get(g)[0];
                    z_mean = bucketMetrics.get(g)[3] / bucketMetrics.get(g)[0];

                    double startz_here = (g * bucketSize + startz) - 1.0;
                    double endz_here = (g * bucketSize + startz) + bucketSize + 1.0;

                    param.set(0,0, x_mean);
                    param.set(1,0, y_mean);
                    param.set(2,0, startz_here);

                    param.set(3,0, x_mean);
                    param.set(4,0, y_mean);
                    param.set(5,0, endz_here);

                    if(pointsInBuckets[g].size() > 5){

                        rs.setPoints(pointsInBuckets[g]);
                        rs.setNumFunctions(1);
                        lm.optimize(rs, param);

                        double[] lmParameters = new double[]{param.data[0], param.data[1], param.data[2], param.data[3],
                                param.data[4], param.data[5], param.data[6]};

                        double xLocat = Math.min(lmParameters[0], lmParameters[3]) + Math.abs(lmParameters[0] - lmParameters[3]) / 2.0;
                        double yLocat = Math.min(lmParameters[1], lmParameters[4]) + Math.abs(lmParameters[1] - lmParameters[4]) / 2.0;
                        double zLocat = Math.min(lmParameters[2], lmParameters[5]) + Math.abs(lmParameters[2] - lmParameters[5]) / 2.0;
/*
                        param.set(0,0, x_mean);
                        param.set(1,0, y_mean);
                        param.set(2,0, startz_here);

                        param.set(3,0, x_mean);
                        param.set(4,0, y_mean);
                        param.set(5,0, endz_here);

                        Ransac ran = new Ransac(pointsInBuckets[g], 0.2, 0.4  , rs, lm, param);

                        //
                        // System.out.println("RUNNING RANSAC: " );

                        double[] ransacParams = ran.optimize(7);

                        lmParameters = ransacParams;
*/
                        //System.out.println(param.data[6] + " ? == ? " + ransacParams[6]);

                        dataForRansac.add(new double[]{(g * bucketSize + startz), lmParameters[6]});
                        ransacCoordinates.add(new double[]{xLocat, yLocat, zLocat});
                        simpleRegression.addData((g * bucketSize + startz), lmParameters[6] );

                        String outtiString = "" + (g * bucketSize + startz) + "\t" + lmParameters[6] + "\t-99.000";
                        String outtiString2 = "" + lmParameters[0] + "\t" + lmParameters[1] + "\t" + lmParameters[2] + "\t" + lmParameters[3]
                                + "\t" + lmParameters[4] + "\t" + lmParameters[5] + "\t" + lmParameters[6];

                        outputti_diam.add(outtiString);
                        outputti_cylinders.add(outtiString2);
                        //System.out.println("height: " + (g * 0.5 + startz) + " diameter: " + lmParameters[6]);
                    }
                }
                //simpleRegression.

                System.out.println(dataForRansac.size());

                //System.out.println(bucketAverageSize / pointsInBuckets.length);
                System.out.println("Predicted 1.3m: " + simpleRegression.predict(1.3));




                RansacLinearRegression RLR = new RansacLinearRegression(dataForRansac, 0.2, 0.3, 0.05, aR.set_seed);

                RLR.addFixedPoint(new double[]{zet, 0.0});

                String regressionString = "0\t0\t0";
                String regressionString_ransac = "0\t0\t0";

                boolean[] inliers = new boolean[dataForRansac.size()];

                if(dataForRansac.size() > 6){

                    SimpleRegression ransacRegression = RLR.optimize();
                    System.out.println("Ransac Predicted 1.3m: " + ransacRegression.predict(1.3));
                    System.out.println("Error normal: " + simpleRegression.getMeanSquareError() + " error ransac: " + ransacRegression.getMeanSquareError());

                    if( (dataForRansac.size() > 6 && !(ransacRegression.predict(1.3) != ransacRegression.predict(1.3)))){ //|| (simpleRegression.getMeanSquareError() > ransacRegression.getMeanSquareError()))

                        if(ransacRegression.predict(1.3) < 0){
                            badStems.add(tempPoint.pointSourceId);
                            continue;

                        }
                        regressionString_ransac = "" + ransacRegression.getIntercept() + "\t" + ransacRegression.getSlope() + "\t" + ransacRegression.predict(1.3);
                        //regressionString = "" + simpleRegression.getIntercept() + "\t" + simpleRegression.getSlope() + "\t" + simpleRegression.predict(1.3);
                        meanDiameter += 2.0 * ransacRegression.predict(1.3);
                        stemDiams[tempPoint.pointSourceId] = (float)(2.0 * ransacRegression.predict(1.3));

                        inliers = RLR.inlierIndexes;

                    }else {
                        badStems.add(tempPoint.pointSourceId);
                        continue;
/*
                        if(simpleRegression.predict(1.3) < 0){
                            badStems.add(tempPoint.pointSourceId);
                            continue;

                        }

                        regressionString = "" + simpleRegression.getIntercept() + "\t" + simpleRegression.getSlope() + "\t" + simpleRegression.predict(1.3);
                        meanDiameter += 2.0 * simpleRegression.predict(1.3);
                        stemDiams[tempPoint.pointSourceId] = (float)(2.0 * simpleRegression.predict(1.3));

 */
                    }

                }else{

                    badStems.add(tempPoint.pointSourceId);
                    continue;
/*
                    if(simpleRegression.predict(1.3) < 0){
                        badStems.add(tempPoint.pointSourceId);
                        continue;

                    }

                    regressionString = "" + simpleRegression.getIntercept() + "\t" + simpleRegression.getSlope() + "\t" + simpleRegression.predict(1.3);
                    meanDiameter += 2.0 * simpleRegression.predict(1.3);
                    stemDiams[tempPoint.pointSourceId] = (float)(2.0 * simpleRegression.predict(1.3));

 */
                }


                //double dx = zet - mappi_allStemStats.get(i)[4] / 10.0;
                //double dy = mappi_allStemStats.get(i)[3] / 2.0;
                //double slope = dy / dx;

                //System.out.println(zet + " " + dx + " " + dy + " " + slope + " " + (mappi_smallerTreesStats.get(i)[4] / 10.0));
                //double slopeValue_1_3m = slope * (zet - 1.3);

                //double diameter_1_3m = slopeValue_1_3m * 2.0d;

/*
                rs.setPoints(pointsForCylinderFit);

                //System.out.println("line center (pre): ");
                //System.out.println(param.get(0, 0) + " " + param.get(1, 0));

                //rs.setNumFunctions(pointsForCylinderFit.size());
                rs.setNumFunctions(1);

                //System.out.println("Number of points in stem: " + counterHere);

                lm.optimize(rs, param);

                double[] lmParameters = new double[]{param.data[0], param.data[1], param.data[2], param.data[3],
                        param.data[4], param.data[5], param.data[6]};

                param.set(0,0, x_mean);
                param.set(1,0, y_mean);
                param.set(2,0, min_z);

                param.set(3,0, x_mean);
                param.set(4,0, y_mean);
                param.set(5,0, maxiz);
                param.set(6,0, 0.1);

 */
/*
                Ransac ran = new Ransac(pointsForCylinderFit, 0.5, 0.1  , rs, lm, param);

                System.out.println("RUNNING RANSAC: " );

                double[] ransacParams = ran.optimize(7);

                if(ransacParams[0] == 0.0){

                    badStems.add(tempPoint.pointSourceId);
                    continue;
                }
*/

                for(int f = 0; f < outputti_diam.size(); f++) {
                    //out_diam.println(outputti_diam.get(f));
                    out_diam_ransac.println(outputti_diam.get(f));
                }

                for(int f = 0; f < outputti.size(); f++) {
                    out.println(outputti.get(f));
                }

                for(int f = 0; f < outputti_cylinders.size(); f++) {
                    //out.println(outputti.get(f));

                    int inlier = inliers[f] ? 1 : 0;
                    String outstring = outputti_cylinders.get(f) + "\t" + inlier;
                    out_diam.println(outstring);
                }


                double height = maxiz - (maxiz - (group[tempPoint.pointSourceId].get(0)[4])) / 2.0;

                count++;

                group[tempPoint.pointSourceId].clear();

                n2 = group_indexes[tempPoint.pointSourceId].size();

                tempUid = tempPoint.pointSourceId;


                stemHeights[tempPoint.pointSourceId] = (float)ransacCoordinates.get(0)[2];

                //String outtistring3 = "" + xLocat + "\t" + yLocat + "\t" + (2*(float)param.get(6, 0));
                //String outtistring4 = "" + xx+ "\t" + yy + "\t" + (2*(float)param.get(6, 0));

                //String outtistring3 = "" + xLocat + "\t" + yLocat + "\t" + (2.0*lmParameters[6]);
                String outtistring3 = "" + ransacCoordinates.get(0)[0] + "\t" + ransacCoordinates.get(0)[1] + "\t" + stemDiams[tempPoint.pointSourceId];

                //String outtistring4 = "" + xLocat_ransac + "\t" + yLocat_ransac + "\t" + (2.0*ransacParams[6]);
                //String outtistring3 = "" + param.get(3,0) + "\t" + param.get(4,0) + "\t" + (2*(float)param.get(6, 0));

                //out.println(outtistring3);
                out_ransac.println(outtistring3);

                //out_diam.println(regressionString);
                out_diam_ransac.println(regressionString_ransac);

                for(int j = 0; j < n2; j++){

                    pointCloud2.readRecord(group_indexes[tempUid].get(j), tempPoint);

                    //tempPoint.intensity = 2*(int)(param.get(6, 0) * 1000);
                    tempPoint.intensity = (int)(stemDiams[tempPoint.pointSourceId] * 1000);

                    if(asd3.writePoint( tempPoint, rule, pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                            pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, group_indexes[tempUid].get(j)))
                        pointCount++;

                }
            }
        }

        out.close();
        out_diam.close();
        out_diam_ransac.close();
        //out_ransac.close();

        pointCount = 0;
        HashMap<Short, Short> underStoreyIds = new HashMap<>();

        HashSet<Short> upperStoreyIds = new HashSet<>();

        int counter1 = 0;

        for(short s : treesBank.keySet()){

            if(treesBank.get(s).size() > 1){

                Iterator it = treesBank.get(s).iterator();

                float largestDiameter = Float.NEGATIVE_INFINITY;
                int largestDiameterIndex = 0;

                while(it.hasNext()){

                    int index = (short)it.next();

                    //System.out.println("Diam: " + stemDiams[index] + " " + index);

                    if(stemDiams[index] > largestDiameter){
                        largestDiameterIndex = index;
                        largestDiameter = stemDiams[index];
                    }

                }

                it = treesBank.get(s).iterator();

                counter1 = 1;

                while(it.hasNext()){

                    int index = (short)it.next();

                    if(index != largestDiameterIndex){

                        stemUnderstorey[index] = true;
                        underStoreyIds.put((short)index, (short)counter1);
                       // System.out.println("counter: " + counter1);
                        counter1++;

                    }else {
                        //System.out.println("Added index: " + (short)index);
                        upperStoreyIds.add((short) index);
                    }
                }
                //System.out.println("-----");
            }

        }

        //System.out.println("Point count: " + pointCount);
        asd3.writeBuffer2();
        asd3.updateHeader2();

        //File outputFile3 = new File("underStoreyC4_" + coreNumber + ".las");

        if(outputFile3.exists())
            outputFile3.delete();

        outputFile3.createNewFile();

        LASraf asd4 = new LASraf(outputFile3);

        LASwrite.writeHeader(asd4, "lasStem", pointCloud.versionMajor, pointCloud.versionMinor,
                pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength,
                pointCloud.headerSize, pointCloud.offsetToPointData, pointCloud.numberVariableLengthRecords,
                pointCloud.fileSourceID, pointCloud.globalEncoding,
                pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset);

        /*
        LASraf stemAlignPcloud = new LASraf(outputFile_stemAlign);

        LASwrite.writeHeader(stemAlignPcloud, "lasStem-align in", pointCloud.versionMajor, pointCloud.versionMinor,
                pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength,
                pointCloud.headerSize, pointCloud.offsetToPointData, pointCloud.numberVariableLengthRecords,
                pointCloud.fileSourceID, pointCloud.globalEncoding,
                pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset);
*/
        int thread_n_2 = aR.pfac.addReadThread(pointCloud);

        pointWriterMultiThread pw1 = new pointWriterMultiThread(outputFile_stemAlign, pointCloud, "lasStemAlign", aR);

        LasPointBufferCreator buf1 = new LasPointBufferCreator(1, pw1);

        aR.pfac.addWriteThread(thread_n_2, pw1, buf1);


        System.out.println(pointCloud.getNumberOfPointRecords());

        n = pointCloud.getNumberOfPointRecords();

        LasPoint tempPoint2 = new LasPoint();

        float[] stemITDid_highestZ = new float[maxITDid+1];

        double delta_z = 0.0;


        for(int i = 0; i < n; i++) {

            pointCloud.readRecord(i, tempPoint);
            pointCloud.readRecord(i, tempPoint2);

            if(tempPoint.classification != 2) {

                if (!dz) {

                    //raster_dz[xCoord][yCoord] = (float)groundLevel(tempPoint);
                    delta_z = tempPoint.z - polator.interpolate(tempPoint.x, tempPoint.y, valuator); //= tempPoint.z - raster_dz[xCoord][yCoord];

                } else {
                    delta_z = tempPoint.z;
                }

                //tempPoint2.classification = 0;

                if (tempPoint.z > stemITDid_highestZ[tempPoint.pointSourceId]) {
                    stemITDid_highestZ[tempPoint.pointSourceId] = (float) delta_z;
                }
                //if(tempPoint.z >= 0.5 && tempPoint.z < z_cutoff_max) {

                int xLocation = (int) Math.floor((tempPoint.x - cloudMinX) / y_interval);
                int yLocation = (int) Math.floor((cloudMaxY - tempPoint.y) / y_interval);
                int zLocation = (int) Math.floor((delta_z - cloudMinZ) / y_interval);

                if (zLocation < this.zDim) {
                    //System.out.println(xLocation + " " + yLocation + " " + zLocation);
                    if (surface[xLocation][yLocation][zLocation] != null) {

                        if (surface[xLocation][yLocation][zLocation].isSurface && surface[xLocation][yLocation][zLocation].id >= 0 && !badStems.contains((short) surface[xLocation][yLocation][zLocation].id)) {

                            tempPoint.classification = 1;

                            tempPoint2.classification = 4;
                            tempPoint2.pointSourceId = (short) surface[xLocation][yLocation][zLocation].id;
                            byte heighti = (byte) (stemHeights[(short) surface[xLocation][yLocation][zLocation].id] * 10);

                            heighti = heighti < 0 ? (byte) 255 : heighti;

                            tempPoint.userData = heighti;
                            tempPoint.gpsTime = stemDiams[(short) surface[xLocation][yLocation][zLocation].id];
                            tempPoint.intensity = (short) surface[xLocation][yLocation][zLocation].id;

                            if (underStoreyIds.containsKey((short) surface[xLocation][yLocation][zLocation].id)) {

                                //System.out.println("HERE1");
                                tempPoint.classification = 5;
                                //tempPoint.intensity = underStoreyIds.get((short) surface[xLocation][yLocation][zLocation].id);

                                //tempPoint.classification = (short) 5;
                                //System.out.println("intensity: " + tempPoint.intensity);


                                //System.out.println("diam: " + tempPoint.gpsTime + " height: " + tempPoint.userData + " ?=? " + (stemHeights[(short) surface[xLocation][yLocation][zLocation].id] * 10));
                            } else {

                            }

                        } else {

                            //tempPoint.classification = 0;
                            tempPoint.intensity = 0;
                            tempPoint.gpsTime = 0;
                            tempPoint.intensity = 0;
                            tempPoint.gpsTime = 0;
                            tempPoint.userData = 0;
                            tempPoint.classification = 0;
                            tempPoint2.pointSourceId = 0;

                        }
                    } else {

                        tempPoint.intensity = 0;
                        tempPoint.gpsTime = 0;
                        tempPoint.userData = 0;
                        tempPoint.classification = 0;
                        tempPoint2.pointSourceId = 0;

                    }
                } else {
                    tempPoint.intensity = 0;
                    tempPoint.gpsTime = 0;
                    tempPoint.userData = 0;
                    tempPoint.classification = 0;
                    tempPoint2.pointSourceId = 0;
                }
            }

            tempPoint.z = delta_z;

            if(asd4.writePoint( tempPoint, rule, pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                    pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, i))
                pointCount++;
/*
            if(stemAlignPcloud.writePoint( tempPoint2, rule, pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                    pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, i))
                pointCount++;
*/
            aR.pfac.writePoint(tempPoint2, i, thread_n_2);


            //}
        }

        try {
            asd4.writeBuffer2();
            asd4.updateHeader2();
        }catch (Exception e){
            e.printStackTrace();
        }
        aR.pfac.closeThread(thread_n_2);


        //stemAlignPcloud.writeBuffer2();
        //stemAlignPcloud.updateHeader2();



        System.out.printf("\nMean diameter %.2f cm\n", (meanDiameter / count)*100);
        System.out.println("Understorey trees found: " + underStoreyIds.size());

        for(short s : treesBank.keySet()){

            if(treesBank.get(s).size() > 0){

                Iterator it = treesBank.get(s).iterator();

                while(it.hasNext()) {

                    int index = (short)it.next();

                    //System.out.println("Asking index: " + (short)index);

                    if(upperStoreyIds.contains((short)index)){
                        //System.out.println("Diameter: " + stemDiams[index] + " height: " + stemITDid_highestZ[s]);
                        regression.addData(stemDiams[index], stemITDid_highestZ[s]);

                    }
                }

            }
        }

        this.pointCloudClassified = new LASReader(outputFile3);
    }

    public void setUpOutputFiles(LASReader pointCloud) throws IOException{

        this.outputFile3 = aR.createOutputFileWithExtension(pointCloud, "_stemClassified.las");
        this.outputFile2 = aR.createOutputFileWithExtension(pointCloud, "_stemDiameters.las");
        this.outputFile = aR.createOutputFileWithExtension(pointCloud, "_stems.las");
        this.outputFile4 = aR.createOutputFileWithExtension(pointCloud, "_cylinderFitting.txt");
        this.outputFile5 = aR.createOutputFileWithExtension(pointCloud, "_cylinderFitting_ransac.txt");
        this.outputFile6 = aR.createOutputFileWithExtension(pointCloud, "_diameters_simple.txt");
        this.outputFile7 = aR.createOutputFileWithExtension(pointCloud, "_diametersRansac.txt");
        this.outputFile_stemAlign = aR.createOutputFileWithExtension(pointCloud, "_stemAlign.las");
    }

    public static double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }

    public int cantor(int a, int b){

        return (a + b) * (a + b + 1) / 2 + a;

    }

    public boolean checkIfIsolated(int x_in, int y_in, int layerNumber){

        int count = 0;

        for(int x = x_in - (int)maxStemDiameter; x <= x_in + maxStemDiameter; x++){

            if(layer[x][y_in-(int)maxStemDiameter] == 99)
                count++;

            if(layer[x][y_in+(int)maxStemDiameter] == 99)
                count++;

/*
            if(layer[x][y_in-(int)maxStemDiameter + 1] == 99)
                count++;

            if(layer[x][y_in+(int)maxStemDiameter - 1] == 99)
                count++;
*/

            if(count > noiseThreshold)
                return false;
        }

        for(int y = y_in - (int)maxStemDiameter; y <= y_in + maxStemDiameter; y++){

            if(layer[x_in - (int)maxStemDiameter][y] == 99)
                count++;

            if(layer[x_in + (int)maxStemDiameter][y] == 99)
                count++;

/*
            if(layer[x_in - (int)maxStemDiameter + 1][y] == 99)
                count++;

            if(layer[x_in + (int)maxStemDiameter - 1][y] == 99)
                count++;
*/

            if(count > noiseThreshold)
                return false;
        }

        //So we found an isolated observation, jolly good.

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        boolean isAlreadyAStem = false;

        int prevStemId = -1;

        ArrayList<Integer> prevIds = new ArrayList<>();
        ArrayList<int[]> allPoints = new ArrayList<>();
        int containedId = -1;

        for(int x = x_in - (int)maxStemDiameter; x <= x_in + maxStemDiameter; x++) {
            for (int y = y_in - (int) maxStemDiameter; y <= y_in + maxStemDiameter; y++) {

                if(stemLocations.containsKey(cantor(x,y))) {
                    isAlreadyAStem = true;
                    containedId = cantor(x,y);
                    prevStemId = stemLocations.get(containedId);
                    prevIds.add(prevStemId);
                }

                if(layer[x][y] != 0) {

                    if(x < minX)
                        minX = x;
                    if(x > maxX)
                        maxX = x;
                    if(y < minY)
                        minY = y;
                    if(y > maxY)
                        maxY = y;

                    progress[x][y] = 19;
                    allPoints.add(new int[]{x,y});
                }
            }
        }

        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;

        if(!isAlreadyAStem) {
            stems.add(new treeStem(stems.size() - 1, centerX, centerY, layerNumber, allPoints));
            stemLocations.put(cantor(centerX, centerY), stems.size() - 1);
        }
        else{

            //System.out.println("HERE!");
            //if(stems.get(prevStemId).layers.get(stems.get(prevStemId).layers.size()-1) != layerNumber)
				/*
				if(!stemLocations.containsKey(cantor(centerX, centerY))){
					stemLocations.put(cantor(centerX, centerY), prevStemId);
				}
				*/
            stemLocations.remove(containedId);
            stemLocations.put(cantor(centerX, centerY), prevStemId);
            stems.get(prevStemId).addLayer(layerNumber, new int[]{centerX, centerY}, allPoints);
        }

        return true;
    }

    public void processLayer(int layer){

        //this.layeri = imageRGBFromArray(layer);

        String fileName = "stem/testi_" + layer + ".png";
        //Imgcodecs.imwrite(fileName, this.layeri);

    }
/*
    public Mat imageFromArray(int layer){

        Mat matti = new Mat(yDim, xDim, CV_8U);

        for(int i = 0; i < xDim; i++)
            for(int j = 0; j < yDim; j++) {
                //stemIds[i][j] = -99;

                if(voxels[i][j][layer] != 0) {
                    this.layer[i][j] = 99;
                    matti.put(j, i, 255);
                }
                else {
                    this.layer[i][j] = 0;
                    matti.put(j, i, 0);
                }
                progress[i][j] = 0;
            }

        return matti;
    }

    public Mat imageRGBFromArray(int layer){

        Mat matti = new Mat(yDim, xDim, CV_32SC3);

        for(int i = 0; i < xDim; i++)
            for(int j = 0; j < yDim; j++) {
                //stemIds[i][j] = -99;

                if(voxels[i][j][layer] > minNumberOfPoints) {
                    this.layer[i][j] = 99;
                    matti.put(j, i, new int[]{255,255,255});
                }
                else {
                    this.layer[i][j] = 0;
                    matti.put(j, i, new int[]{0,0,0});
                }
                progress[i][j] = 0;
            }

        return matti;
    }
*/
}
