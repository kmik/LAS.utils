package tools;

import LASio.*;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.ejml.data.DMatrixRMaj;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.opencv.core.Mat;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.Vertex;
import utils.*;
import java.io.*;
import java.util.*;
import static tools.GroundDetector.angleHypo;
import static tools.GroundDetector.euclideanDistance;

/**
 * Used for aligning flight lines ("swaths") of LiDAR data, or
 * basically any .las format point cloud data. The input requires
 * .las files of the flight lines, every flight line in its own
 * file, and a trajectory file of the aircraft.
 *
 * Both global (boresight and leverarm) and local (pitch, roll, yaw,
 * x, y, z) parameters are optimized using Levenberg Marquadt
 * optimization.
 *
 */
public class lasStrip {

    /**
     * Delta t means the time interval of the segments in flight lines
     * that are optimized separately. A polynomial is estimated to
     * interpolate a smooth correction for the parameters.
     */
    double deltaT = 5.0;

    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static ThreadProgressBar progeBar = new ThreadProgressBar();

    double angleThreshold = 6.66;
    LASReader file1;
    LASReader file2;


    org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

    TreeMap<Integer,double[]> trajectory = new TreeMap<>();

    List<org.tinfour.common.Vertex> vertices1;
    List<org.tinfour.common.Vertex> vertices2;

    Mat point;// = new Mat(1,3, CV_64F);
    DoubleMatrix pointMatrix = new DoubleMatrix(1,3);
    Mat rotatedpoint;// = new Mat(1,3, CV_64F);
    DoubleMatrix rotatedPointMatrix = new DoubleMatrix(1,3);

    Mat rotationMatrix1 = null;
    Mat rotationMatrix2 = null;
    Mat emptyMat = null;

    Mat tempRotation;
    DoubleMatrix tempRotationMatrix = new DoubleMatrix(3,3);
    Mat boreRotation;
    DoubleMatrix boreRotationMatrix = new DoubleMatrix(3,3);
    Mat leverTranslation;
    DoubleMatrix leverTranslationMatrix = new DoubleMatrix(1,3);

    org.tinfour.common.Vertex v_temp;
    //org.tinfour.interpolation.TriangularFacetInterpolator polator1;
    //org.tinfour.interpolation.TriangularFacetInterpolator polator2;

    float[] translation_1 = new float[]{0f,0f,0f};
    float[] translation_2 = new float[]{0f,0f,0f};

    double[] beta = null;
    double[] normal = null;

    HashSet<Integer> ignore1 = new HashSet<>();
    HashSet<Integer> ignore2 = new HashSet<>();

    KdTree tree1 = new KdTree();
    KdTree tree2 = new KdTree();

    Map.Entry<Integer, double[]> hehe = null;

    double new_x;
    double new_y;
    double new_z;

    Mat dummyMat;

    LasBlock blokki;

    double[] locationOfAircraft;
    double[] file1_pivotPoint;
    double[] file2_pivotPoint;

    public int tinProgress = 0;
    int pairProgress = 0;
    public int filesRead = 0;
    int filesWritten = 0;
    int pointProgress = 0;
    int pointsTotal = 0;

    boolean boreDone = false;
    boolean boreInProgress = false;
    boolean boreDisabled = false;

    double averageImprovement = 0.0;
    double averageDifference = 0.0;

    double averageImprovement_std = 0.0;
    double averageDifference_std = 0.0;

    int count_for_averageImrpovement = 0;

    ArrayList<DoubleMatrix> matrices_rot = new ArrayList<>();
    ArrayList<double[]> matrices_trans = new ArrayList<>();

    argumentReader aR;

    ArrayList<LASReader> fileList = new ArrayList<>();

    double[] polynomial_pitch;
    double[] polynomial_yaw;
    double[] polynomial_roll;
    double[] polynomial_x;
    double[] polynomial_y;
    double[] polynomial_z;

    PolynomialSplineFunction po_x_rot;
    PolynomialSplineFunction po_y_rot;
    PolynomialSplineFunction po_z_rot;
    PolynomialSplineFunction po_x;
    PolynomialSplineFunction po_y;
    PolynomialSplineFunction po_z;

    SplineInterpolator interpolatori = new SplineInterpolator();

    double[] x_segments;

    DoubleMatrix tempMatrix = new DoubleMatrix(3,3);
    double[] tempTrans = new double[3];

    public lasStrip(LASReader file1, LASReader file2){


    }

    public lasStrip(){


    }

    public lasStrip(argumentReader aR){

        this.aR = aR;

        this.deltaT = aR.delta;

        if(aR.skip_global)
            this.boreDisabled = true;
        //aR.createOutputFiles();

    }

    public void updateProgress(){


        if(true)
            return;
        System.out.printf(((char) 0x1b) + "[16A\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - LasStrip, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%d / %d", "* LAS files read", filesRead, blokki.pointClouds.size() );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%d / %d", "* LAS pairs processed", tinProgress, blokki.order.size());
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%d / %d", "* LAS files written", filesWritten, blokki.pointClouds.size() );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f", "* Average improvement (cm) ", this.averageImprovement);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f", "* Average difference (cm) ", this.averageDifference);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f", "* Average improvement, std (cm) ", this.averageImprovement_std);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f", "* Average difference, std (cm) ", this.averageDifference_std);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f ; %.0f", "* Points (total ; avg per pair)", blokki.totalPoints, blokki.totalPoints / blokki.differenceBefore_2_to_1.size());
        if(boreDisabled)
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%s", "* Global optimization", "Disabled");
        else if(boreDone)
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%s", "* Global optimization", "done");
        else if(boreInProgress)
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%s", "* Global optimization", "In progress...");
        else
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%s", "* Global optimization", "Waiting");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-20s" + progeBar.getProgress(blokki.order.size(),tinProgress), "Tin creation");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-20s" + progeBar.getProgress(blokki.order.size(), pairProgress), "Local optimization");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-20s" + progeBar.getProgress(blokki.pointClouds.size(),this.filesWritten), "Cloud output");

    }

    /**
     * Reads an ASCII trajectory file. The column order should be:
     *
     *  Time[s] Northing[m] Easting[m] Height[m] Roll[deg] Pitch[deg] Yaw[deg]
     *
     *
     * @param fileName
     */
    public void readTrajectoryFile(String fileName){

        File trajFile = new File(fileName);

        String[] tokens = null;

        try (BufferedReader br = new BufferedReader(new FileReader(trajFile))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {


                tokens = line.split(" ");

                double[] temp = new double[]{Double.parseDouble( tokens[1]),Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3])
                        ,Double.parseDouble( tokens[4]),Double.parseDouble(tokens[5]), Double.parseDouble(tokens[6])};

                trajectory.put((int)(Double.parseDouble(tokens[0])*1000d), temp);

            }
        }catch (IOException e){
            e.printStackTrace();
        }


    }

    public ArrayList<LASReader> readLasFiles(File directory) throws IOException{

        ArrayList<LASReader> output = new ArrayList<>();

        File[] lista = directory.listFiles();

        for(File f : lista){

            if(f.getName().endsWith(".las"))
                output.add(new LASReader(f));
        }

        return output;
    }

    public void globalAlign(){

    }

    public void rotateBoresight(DoubleMatrix point, int time){

        hehe = trajectory.ceilingEntry(time);

        locationOfAircraft = hehe.getValue();

        makeRotationMatrix(tempRotationMatrix, locationOfAircraft[3], locationOfAircraft[4], locationOfAircraft[5]);

        new_x = locationOfAircraft[0] - point.get(0, 0);
        new_y = locationOfAircraft[1] - point.get(0, 1);
        new_z = locationOfAircraft[2] - point.get(0, 2);

        point.put(0, 0, new_x);
        point.put(0, 1, new_y);
        point.put(0, 2, new_z);

        rotatedPointMatrix = point.mmul(Solve.pinv(tempRotationMatrix));
        //Core.gemm(point, tempRotation.inv(), 1, dummyMat, 0, rotatedpoint);

        // Rotate the boresight
        rotatedPointMatrix = rotatedPointMatrix.mmul(boreRotationMatrix);
        //Core.gemm(rotatedpoint, boreRotation, 1, dummyMat, 0, rotatedpoint);

        rotatedPointMatrix.put(0,0, rotatedPointMatrix.get(0, 0) + leverTranslationMatrix.get(0,0));
        rotatedPointMatrix.put(0,1, rotatedPointMatrix.get(0, 1) + leverTranslationMatrix.get(0,1));
        rotatedPointMatrix.put(0,2, rotatedPointMatrix.get(0, 2) + leverTranslationMatrix.get(0,2));

        // Rotate the point back
        //makeRotationMatrix(tempRotation, locationOfAircraft[3], locationOfAircraft[4], locationOfAircraft[5]);
        rotatedPointMatrix = rotatedPointMatrix.mmul(tempRotationMatrix);
        //Core.gemm(rotatedpoint, tempRotation, 1, dummyMat, 0, rotatedpoint);

        point.put(0, 0, locationOfAircraft[0] - rotatedPointMatrix.get(0, 0));
        point.put(0, 1, locationOfAircraft[1] - rotatedPointMatrix.get(0, 1));
        point.put(0, 2, locationOfAircraft[2] - rotatedPointMatrix.get(0, 2));
    }

    public void align() throws IOException {

        LASraf asd1;

        int id = 1;
        int id2 = 255;

        for(int i = 0; i < aR.inputFiles.size(); i++){
            this.fileList.add(new LASReader(aR.inputFiles.get(i)));
        }

        File outputDirectory = new File("/media/koomikko/B8C80A93C80A4FD41/Linux_downloads/UAV_LIDAR/150/gc/output/");

        String outputDir = "/media/koomikko/B8C80A93C80A4FD41/Linux_downloads/UAV_LIDAR/150/gc/output/";


        if(aR.odir.equals("asd"))
            outputDir = "";
        else
            outputDir = aR.odir + System.getProperty("file.separator");

        blokki = new LasBlock(outputDir, aR.step);

        aR.blokki = blokki;

        aR.p_update.updateProgressLasStrip();

        for(int i = 0; i < fileList.size(); i++){

            blokki.addLasFile(fileList.get(i));
        }

        readTrajectoryFile(aR.trajectory);

        blokki.setStrip(this);

        blokki.prepare();

        blokki.makePairs();

        blokki.makeAlignOrder();

        blokki.makeImages3();

        aR.blokki = blokki;

        ResidFunction_leverArm_Boresight rs2 = new ResidFunction_leverArm_Boresight();

        rs2.setBlock(blokki);

        rs2.setup();

        rs2.setTrajectory(this.trajectory);

        DMatrixRMaj resid2 = new DMatrixRMaj(1, 1);
        DMatrixRMaj param2 = new DMatrixRMaj(6, 1);
        param2.set(0,0, 0);
        param2.set(1,0, 0);
        param2.set(2,0, 0);
        param2.set(3,0, 0);
        param2.set(4,0, 0);
        param2.set(5,0, 0);

        LevenbergMarquardt lm2 = new LevenbergMarquardt(aR.lambda);

        /*
        System.out.println("\n#########################################");
        System.out.println("## Boresight and leverarm optimization ##");
        System.out.println("#########################################");
        */

        aR.p_update.lasstrip_filesRead = this.filesRead;


        boreInProgress = true;
        aR.p_update.updateProgressLasStrip();

        if(!this.boreDisabled) {
            lm2.optimize(rs2, param2);
        }
        makeRotationMatrix(boreRotationMatrix, param2.get(0, 0), param2.get(1, 0), param2.get(2, 0));
        leverTranslationMatrix.put(0,0, param2.get(3,0));
        leverTranslationMatrix.put(0,1, param2.get(4,0));
        leverTranslationMatrix.put(0,2, param2.get(5,0));


        this.boreDone = true;

        rs2.release();

        lm2 = null;
        rs2 = null;

        System.gc();


        int[] currentFiles;

        progeBar.setName("Local optimization...");
        progeBar.setEnd(blokki.order.size());

        PointInclusionRule rule = new PointInclusionRule();

        for(int p = 0; p < blokki.order.size(); p++){

            org.tinfour.standard.IncrementalTin tin1 = new org.tinfour.standard.IncrementalTin();

            org.tinfour.standard.IncrementalTin tin2 = new org.tinfour.standard.IncrementalTin();

            currentFiles = blokki.order.get(p);

            System.gc();
            System.gc();
            System.gc();

            System.gc();
            System.gc();
            System.gc();

            blokki.currentFiles = currentFiles;

            if(!blokki.aligned[currentFiles[0]])
                file1 = blokki.pointClouds.get(currentFiles[0]);
            else
                file1 = new LASReader(blokki.outputPointClouds.get(currentFiles[0]).getFileReference());

            if(!blokki.aligned[currentFiles[1]])
                file2 = blokki.pointClouds.get(currentFiles[1]);
            else
                file2 = new LASReader(blokki.outputPointClouds.get(currentFiles[1]).getFileReference());

            long n1 = file1.getNumberOfPointRecords();
            long n2 = file2.getNumberOfPointRecords();

            tin1.clear();
            tin2.clear();

            LasPoint tempPoint = new LasPoint();

            int pointCount = 0;

            int count = 0;

            /* This part means that we start from the tin1 file of the first pair!!,
              if p == 0, they both are not aligned, so we make the first one aligned,
              i.e. reference.
             */
            if(p == 0)
                blokki.aligned[currentFiles[0]] = !blokki.aligned[currentFiles[0]];

            org.tinfour.interpolation.TriangularFacetInterpolator polator1 = null;
            org.tinfour.interpolation.TriangularFacetInterpolator polator2 = null;


            if(!blokki.aligned[currentFiles[0]]) {
                for (int i = 0; i < n2; i++) {

                    if (blokki.includedPointsFile2.get(p).contains(i)) {
                        file2.readRecord(i, tempPoint);

                        pointMatrix.put(0, 0, tempPoint.x);
                        pointMatrix.put(0, 1, tempPoint.y);
                        pointMatrix.put(0, 2, tempPoint.z);

                        if (!boreDisabled)
                            if (!blokki.aligned[currentFiles[1]])
                                rotateBoresight(pointMatrix, (int) (tempPoint.gpsTime * 1000));


                        org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(pointMatrix.get(0, 0), pointMatrix.get(0, 1), pointMatrix.get(0, 2));
                        tempV.setIndex((int) (tempPoint.gpsTime * 1000));
                        tin2.add(tempV);
                    }

                }

                List<IQuadEdge> tin2_perimeter = tin2.getPerimeter();

                polator2 = new org.tinfour.interpolation.TriangularFacetInterpolator(tin2);


                count = 0;

                for (int i = 0; i < n1; i++) {

                    if (blokki.includedPointsFile1.get(p).contains(i)) {
                        file1.readRecord(i, tempPoint);

                        pointMatrix.put(0, 0, tempPoint.x);
                        pointMatrix.put(0, 1, tempPoint.y);
                        pointMatrix.put(0, 2, tempPoint.z);

                        if (!boreDisabled)
                            if (!blokki.aligned[currentFiles[0]])
                                rotateBoresight(pointMatrix, (int) (tempPoint.gpsTime * 1000));


                        org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(pointMatrix.get(0, 0), pointMatrix.get(0, 1), pointMatrix.get(0, 2));
                        tempV.setIndex((int) (tempPoint.gpsTime * 1000));

                        if (pointDistanceToPerimeter(tin2_perimeter, tempV) > 3.5 && tin2.isPointInsideTin(tempV.x, tempV.y))
                            tin1.add(tempV);
                    }
                }


                polator1 = new org.tinfour.interpolation.TriangularFacetInterpolator(tin1);
            }else{

                count = 0;

                for (int i = 0; i < n1; i++) {

                    if (blokki.includedPointsFile1.get(p).contains(i)) {
                        file1.readRecord(i, tempPoint);

                        pointMatrix.put(0, 0, tempPoint.x);
                        pointMatrix.put(0, 1, tempPoint.y);
                        pointMatrix.put(0, 2, tempPoint.z);

                        if (!boreDisabled)
                            if (!blokki.aligned[currentFiles[0]])
                                rotateBoresight(pointMatrix, (int) (tempPoint.gpsTime * 1000));


                        org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(pointMatrix.get(0, 0), pointMatrix.get(0, 1), pointMatrix.get(0, 2));
                        tempV.setIndex((int) (tempPoint.gpsTime * 1000));

                            tin1.add(tempV);
                    }
                }


                List<IQuadEdge> tin1_perimeter = tin1.getPerimeter();

                polator1 = new org.tinfour.interpolation.TriangularFacetInterpolator(tin1);

                for (int i = 0; i < n2; i++) {

                    if (blokki.includedPointsFile2.get(p).contains(i)) {
                        file2.readRecord(i, tempPoint);

                        pointMatrix.put(0, 0, tempPoint.x);
                        pointMatrix.put(0, 1, tempPoint.y);
                        pointMatrix.put(0, 2, tempPoint.z);

                        if (!boreDisabled)
                            if (!blokki.aligned[currentFiles[1]])
                                rotateBoresight(pointMatrix, (int) (tempPoint.gpsTime * 1000));


                        org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(pointMatrix.get(0, 0), pointMatrix.get(0, 1), pointMatrix.get(0, 2));
                        tempV.setIndex((int) (tempPoint.gpsTime * 1000));

                        if (pointDistanceToPerimeter(tin1_perimeter, tempV) > 3.5 && tin1.isPointInsideTin(tempV.x, tempV.y))
                            tin2.add(tempV);
                    }

                }

                polator2 = new org.tinfour.interpolation.TriangularFacetInterpolator(tin2);




            }

            count = 0;

            double distance = 0.0;

            double disti = 0.0;

            double difBefore = 0.0;
            double difBefore_std = 0.0;


            int numberOfSegments = 0;

            if(!blokki.aligned[currentFiles[1]]) {

                numberOfSegments = (int)Math.ceil((blokki.pointCloudTimeExtent.get(currentFiles[1])[1] - blokki.pointCloudTimeExtent.get(currentFiles[1])[0]) / deltaT);


                difBefore = blokki.differenceBefore_2_to_1.get(p);
                difBefore_std = blokki.stdBefore_2_to_1.get(p);

            }
            else if(!blokki.aligned[currentFiles[0]]){

                numberOfSegments = (int)Math.ceil((blokki.pointCloudTimeExtent.get(currentFiles[0])[1] - blokki.pointCloudTimeExtent.get(currentFiles[0])[0]) / deltaT);
                difBefore = blokki.differenceBefore_1_to_2.get(p);
                difBefore_std = blokki.stdBefore_1_to_2.get(p);
            }

            ResidFunction rs = new ResidFunction(this.deltaT);

            file1_pivotPoint = new double[]{(file1.getMaxX() - file1.minX) / 2.0d + file1.getMinX(),
                    (file1.getMaxY() - file1.minY) / 2.0d + file1.getMinY(),
                    (file1.getMinZ() + 100.0d)};

            file2_pivotPoint = new double[]{(file2.getMaxX() - file2.minX) / 2.0d + file2.getMinX(),
                    (file2.getMaxY() - file2.minY) / 2.0d + file2.getMinY(),
                    (file2.getMinZ() + 100.0d)};

            rs.setPivotPoints(file1_pivotPoint, file2_pivotPoint);

            rs.setTins(tin1,!blokki.aligned[currentFiles[0]], blokki.pointCloudTimeExtent.get(currentFiles[0]), tin2, !blokki.aligned[currentFiles[1]], blokki.pointCloudTimeExtent.get(currentFiles[1]));

            rs.setTrajectory(trajectory);

            LevenbergMarquardt lm = new LevenbergMarquardt(aR.lambda);

            DMatrixRMaj param = null;

            if(!blokki.aligned[currentFiles[0]] && !blokki.aligned[currentFiles[1]]) {
                System.out.println("Should not be here, ever! Something went terribly terribly wrong!!");
            }else{
                param = new DMatrixRMaj(6*numberOfSegments, 1);
                for(int g = 0 ; g < param.numRows; g++)
                    param.set(g,0,0);
            }

            rs.setBlock(this.blokki);
            lm.optimize(rs, param);

            x_segments = new double[numberOfSegments + 3];

            double[] y_x = new double[numberOfSegments + 3];
            double[] y_y = new double[numberOfSegments + 3];
            double[] y_z = new double[numberOfSegments + 3];

            double[] y_x_rot = new double[numberOfSegments + 3];
            double[] y_y_rot = new double[numberOfSegments + 3];
            double[] y_z_rot = new double[numberOfSegments + 3];

            int counti = 1;
            if(!blokki.aligned[currentFiles[0]]) {
                for (int e = 0; e <= numberOfSegments; e++) {
                    x_segments[counti] = blokki.pointCloudTimeExtent.get(currentFiles[0])[0] + e * deltaT;
                    counti++;
                }
                x_segments[0] = x_segments[1]-2;
                x_segments[x_segments.length-2] = blokki.pointCloudTimeExtent.get(currentFiles[0])[1];
                x_segments[x_segments.length-1] = x_segments[x_segments.length-2]+2;
            }
            else if(!blokki.aligned[currentFiles[1]]) {
                //System.out.println("HERE!");
                for (int e = 0; e <= numberOfSegments; e++) {
                    x_segments[counti] = blokki.pointCloudTimeExtent.get(currentFiles[1])[0] + e * deltaT;
                    counti++;
                }
                x_segments[0] = x_segments[1]-2;
                x_segments[x_segments.length-2] = blokki.pointCloudTimeExtent.get(currentFiles[1])[1];
                x_segments[x_segments.length-1] = x_segments[x_segments.length-2]+2;
            }

            counti = 1;

            y_x_rot[0] = param.get(0 + 0,0);
            y_y_rot[0] = param.get(0 + 1,0);
            y_z_rot[0] = param.get(0 + 2,0);

            y_x[0] = param.get(0 + 3,0);
            y_y[0] = param.get(0 + 4,0);
            y_z[0] = param.get(0 + 5,0);

            y_x_rot[y_x.length-2] = param.get(param.numRows - 6,0);
            y_y_rot[y_x.length-2] = param.get(param.numRows - 5,0);
            y_z_rot[y_x.length-2] = param.get(param.numRows - 4,0);

            y_x[y_x.length-2] = param.get(param.numRows - 3,0);
            y_y[y_x.length-2] = param.get(param.numRows - 2,0);
            y_z[y_x.length-2] = param.get(param.numRows - 1,0);

            y_x_rot[y_x.length-1] = param.get(param.numRows - 6,0);
            y_y_rot[y_x.length-1] = param.get(param.numRows - 5,0);
            y_z_rot[y_x.length-1] = param.get(param.numRows - 4,0);

            y_x[y_x.length-1] = param.get(param.numRows - 3,0);
            y_y[y_x.length-1] = param.get(param.numRows - 2,0);
            y_z[y_x.length-1] = param.get(param.numRows - 1,0);

            for(int h = 0; h < param.numRows; h += 6){

                y_x_rot[counti] = param.get(h + 0,0);
                y_y_rot[counti] = param.get(h + 1,0);
                y_z_rot[counti] = param.get(h + 2,0);

                y_x[counti] = param.get(h + 3,0);
                y_y[counti] = param.get(h + 4,0);
                y_z[counti] = param.get(h + 5,0);

                counti++;
            }
            double distanceSum = 0.0;
            count = 0;
            disti = 0.0;

            int index = 0;

            po_x_rot = interpolatori.interpolate(x_segments, y_x_rot);
            po_y_rot = interpolatori.interpolate(x_segments, y_z_rot);
            po_z_rot = interpolatori.interpolate(x_segments, y_y_rot);
            po_x = interpolatori.interpolate(x_segments, y_x);
            po_y = interpolatori.interpolate(x_segments, y_y);
            po_z = interpolatori.interpolate(x_segments, y_z);

            double stdDev = 0.0;
            double average = 0.0;
            double pwrSumAvg = 0.0;

            double distiSigned = 0.0;

            double[] normal = null;
            double interpolatedvalue = 0;

            if(!blokki.aligned[currentFiles[0]]) {

                for (org.tinfour.common.Vertex v : tin1.getVertices()) {

                    pointMatrix.put(0, 0, v.x);
                    pointMatrix.put(0, 1, v.y);
                    pointMatrix.put(0, 2, v.getZ());

                    rotatePoint2(pointMatrix, (double)v.getIndex()/1000.0d);

                    new_x = pointMatrix.get(0, 0) + tempTrans[0];
                    new_y = pointMatrix.get(0, 1) + tempTrans[1];
                    new_z = pointMatrix.get(0, 2) + tempTrans[2];

                    pointMatrix.put(0,0, new_x);
                    pointMatrix.put(0,1, new_y);
                    pointMatrix.put(0,2, new_z);


                    if (tin2.isPointInsideTin(pointMatrix.get(0, 0), pointMatrix.get(0, 1))) {

                        interpolatedvalue = polator2.interpolate(pointMatrix.get(0, 0), pointMatrix.get(0, 1), valuator);

                        distiSigned = pointMatrix.get(0, 2) - interpolatedvalue;

                        normal = polator2.getSurfaceNormal();

                        distiSigned = (normal[0] * pointMatrix.get(0, 0) + normal[1] * pointMatrix.get(0, 1) + normal[2] * pointMatrix.get(0, 2) -
                                (normal[0] * pointMatrix.get(0, 0) + normal[1] * pointMatrix.get(0, 1) + normal[2] * (pointMatrix.get(0, 2) - distiSigned))) /
                                Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);

                        disti = Math.abs(distiSigned);

                        if (!Double.isNaN(disti)) {
                            count++;
                            distanceSum += disti;

                            average += (distiSigned - average) / count;
                            pwrSumAvg += (distiSigned * distiSigned - pwrSumAvg) / count;
                            stdDev = Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));
                        }
                    }

                }
            }

            else if(!blokki.aligned[currentFiles[1]]) {


                for (org.tinfour.common.Vertex v : tin2.getVertices()) {

                    pointMatrix.put(0, 0, v.x);
                    pointMatrix.put(0, 1, v.y);
                    pointMatrix.put(0, 2, v.getZ());

                    rotatePoint2(pointMatrix, (double)v.getIndex()/1000.0d);

                    new_x = pointMatrix.get(0, 0) + tempTrans[0];
                    new_y = pointMatrix.get(0, 1) + tempTrans[1];
                    new_z = pointMatrix.get(0, 2) + tempTrans[2];

                    pointMatrix.put(0,0, new_x);
                    pointMatrix.put(0,1, new_y);
                    pointMatrix.put(0,2, new_z);


                    if (tin1.isPointInsideTin(pointMatrix.get(0, 0), pointMatrix.get(0, 1))) {
                        interpolatedvalue = polator1.interpolate(pointMatrix.get(0, 0), pointMatrix.get(0, 1), valuator);

                        distiSigned = pointMatrix.get(0, 2) - interpolatedvalue;

                        normal = polator1.getSurfaceNormal();

                        distiSigned = (normal[0] * pointMatrix.get(0, 0) + normal[1] * pointMatrix.get(0, 1) + normal[2] * pointMatrix.get(0, 2) -
                                (normal[0] * pointMatrix.get(0, 0) + normal[1] * pointMatrix.get(0, 1) + normal[2] * (pointMatrix.get(0, 2) - distiSigned))) /
                                Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);

                        disti = Math.abs(distiSigned);

                        if (!Double.isNaN(disti)) {
                            count++;
                            distanceSum += disti;

                            average += (distiSigned - average) / count;
                            pwrSumAvg += (distiSigned * distiSigned - pwrSumAvg) / count;
                            stdDev = Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));
                        }
                    }

                }
            }

            count_for_averageImrpovement++;

            difBefore_std *= 100;
            stdDev *= 100;
            difBefore *= 100.0;

            difBefore = Math.abs(difBefore);
            double difAfter = Math.abs((distanceSum / (double)count) * 100.0);


            this.averageDifference += ((difAfter - this.averageDifference) / (double) count_for_averageImrpovement);
            this.averageDifference = round(this.averageDifference,4);

            aR.p_update.lasstrip_averageDifference = this.averageDifference;

            this.averageImprovement += ((difBefore - difAfter - this.averageImprovement) / (double) count_for_averageImrpovement);
            this.averageImprovement = round(this.averageImprovement,4);

            aR.p_update.lasstrip_averageImprovement = this.averageImprovement;

            this.averageDifference_std += ((stdDev - this.averageDifference_std) / (double) count_for_averageImrpovement);
            this.averageDifference_std = round(this.averageDifference_std,4);

            aR.p_update.lasstrip_averageDifference_std = this.averageDifference_std;

            this.averageImprovement_std += ((Math.abs(difBefore_std) - Math.abs(stdDev) - this.averageImprovement_std) / (double) count_for_averageImrpovement);
            this.averageImprovement_std = round(this.averageImprovement_std,4);

            aR.p_update.lasstrip_averageImprovement_std = this.averageImprovement_std;

            aR.p_update.updateProgressLasStrip();

            id += 25;
            id2 -= 25;

            if(p == 0)
                blokki.aligned[currentFiles[0]] = !blokki.aligned[currentFiles[0]];


            //System.out.println("\nTranslating and outputting points...");
            //System.out.println("---------------------------------------\n\n");

            if(p == 0){
                asd1 = blokki.outputPointClouds.get(currentFiles[0]);
                pointCount = 0;
                this.pointsTotal = (int)n1;

                for (int i = 0; i < n1; i++) {
                    file1.readRecord(i, tempPoint);
                    tempPoint.pointSourceId = (short)(id);
                    tempPoint.userData = id;

                    if (asd1.writePoint( tempPoint, rule, file1.xScaleFactor, file1.yScaleFactor, file1.zScaleFactor,
                            file1.xOffset, file1.yOffset, file1.zOffset, file1.pointDataRecordFormat, i))
                        pointCount++;

                }
                asd1.writeBuffer2();
                asd1.updateHeader2();

                blokki.aligned[currentFiles[0]] = true;
                asd1.close();

                this.pointProgress = 0;

                filesWritten++;
                aR.p_update.updateProgressLasStrip();

            }
            else if(!blokki.aligned[currentFiles[0]]) {

                asd1 = blokki.outputPointClouds.get(currentFiles[0]);

                pointCount = 0;
                this.pointsTotal = (int)n1;

                for (int i = 0; i < n1; i++) {

                    file1.readRecord(i, tempPoint);

                    pointMatrix.put(0, 0, tempPoint.x);
                    pointMatrix.put(0, 1, tempPoint.y);
                    pointMatrix.put(0, 2, tempPoint.z);

                    rotatePoint2(pointMatrix, tempPoint.gpsTime);

                    tempPoint.x = pointMatrix.get(0, 0) + tempTrans[0];
                    tempPoint.y = pointMatrix.get(0, 1) + tempTrans[1];
                    tempPoint.z = pointMatrix.get(0, 2) + tempTrans[2];

                    tempPoint.pointSourceId = (short)(id);
                    tempPoint.userData = id;

                    if (asd1.writePoint( tempPoint, rule, file1.xScaleFactor, file1.yScaleFactor, file1.zScaleFactor,
                            file1.xOffset, file1.yOffset, file1.zOffset, file1.pointDataRecordFormat, i))
                        pointCount++;

                }

                asd1.writeBuffer2();
                asd1.updateHeader2();

                blokki.aligned[currentFiles[0]] = true;
                asd1.close();
                asd1 = null;

                this.pointProgress = 0;

                filesWritten++;
                aR.p_update.updateProgressLasStrip();


            }

            if(!blokki.aligned[currentFiles[1]]) {

                asd1 = blokki.outputPointClouds.get(currentFiles[1]);
                pointCount = 0;
                this.pointsTotal = (int)n2;

                for (int i = 0; i < n2; i++) {

                    file2.readRecord(i, tempPoint);

                    pointMatrix.put(0, 0, tempPoint.x);
                    pointMatrix.put(0, 1, tempPoint.y);
                    pointMatrix.put(0, 2, tempPoint.z);

                    rotatePoint2(pointMatrix, tempPoint.gpsTime);

                    tempPoint.x = pointMatrix.get(0, 0) + tempTrans[0];
                    tempPoint.y = pointMatrix.get(0, 1) + tempTrans[1];
                    tempPoint.z = pointMatrix.get(0, 2) + tempTrans[2];

                    tempPoint.pointSourceId = (short)(id2);
                    tempPoint.userData = id2;

                    if (asd1.writePoint( tempPoint, rule, file2.xScaleFactor, file2.yScaleFactor, file2.zScaleFactor,
                            file2.xOffset, file2.yOffset, file2.zOffset, file2.pointDataRecordFormat, i))
                        pointCount++;

                }

                this.pointProgress = 0;

                asd1.writeBuffer2();
                asd1.updateHeader2();

                blokki.aligned[currentFiles[1]] = true;

                asd1.close();
                asd1 = null;

                filesWritten++;
                aR.p_update.updateProgressLasStrip();
            }

            for(org.tinfour.common.Vertex v : tin1.getVertices())
                v = null;

            for(org.tinfour.common.Vertex v : tin2.getVertices())
                v = null;

            for(org.tinfour.common.IQuadEdge e : tin1.getEdges())
                e = null;

            for(org.tinfour.common.IQuadEdge e : tin2.getEdges())
                e = null;

            rs.release();

            lm = null;

            rs = null;

            param = null;

            tin1.clear();
            tin2.clear();

            tin1.dispose();
            tin2.dispose();

            file1.close();
            file2.close();

            System.gc();
            System.gc();
            System.gc();



            pairProgress++;

            aR.p_update.lasstrip_filesWritten = this.filesWritten;
            aR.p_update.lasstrip_pairProgress = this.pairProgress;

            aR.p_update.updateProgressLasStrip();
        }

    }

    public double interpolate(double[] coeffs, double x){

        double output = 0.0;
        int powi = coeffs.length-1;

        for(int i = 0; i < coeffs.length-1; i++){
            output += coeffs[i] * Math.pow(x,powi);
            powi--;
        }

        output += coeffs[coeffs.length-1];
        return output;
    }

    public void makeRotationMatrixes(DMatrixRMaj in){

        this.matrices_rot.clear();
        this.matrices_trans.clear();

        for(int i = 0; i < in.numRows; i += 6){

            DoubleMatrix temp = new DoubleMatrix(3,3);
            makeRotationMatrix(temp, in.get(i,0), in.get(i+1,0), in.get(i+2,0));
            matrices_rot.add(temp);
            matrices_trans.add(new double[]{in.get(i+3), in.get(i+4), in.get(i+5)});

        }
    }

    public double pointDistanceToPerimeter(List<IQuadEdge> perimeter, Vertex point){

        double distance = Double.POSITIVE_INFINITY;

        double distance_ = 0;
        for(IQuadEdge e : perimeter){

            Vertex a = e.getA();

            distance_ = a.getDistance(point);

            if(distance_ < distance){

                distance = distance_;

            }
        }

        return distance;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
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

            for(org.tinfour.common.Vertex v : tin1.getVertices()){

                angle1 = -1.0;
                angle2 = -1.0;
                angle3 = -1.0;

                polator1.interpolate(v.x, v.y, valuator);
                normal = polator1.getSurfaceNormal();
                Vector3D normal3 = new Vector3D(normal[0],normal[1],normal[2]);

                if(tin2.isPointInsideTin(v.x, v.y)){

                    distance1 = Math.abs(v.getZ() - polator2.interpolate(v.x, v.y, valuator));
                    //beta = polator2.getCoefficients();
                    normal = polator2.getSurfaceNormal();
                    //System.out.println(Arrays.toString(normal));
                    //Vector3D normal1 = new Vector3D(normal[0],normal[1],normal[2]);

                    closest = tin2.getNeighborhoodPointsCollector().collectNeighboringVertices(v.x, v.y, 0, 0);

                    if(closest.size() > 0){

                        v_closest = closest.get(0);

                        //polator2.interpolate(v_closest.x, v_closest.y, valuator);

                        if(tin1.isPointInsideTin(v_closest.x, v_closest.y)){

                            polator2.interpolate(v_closest.x, v_closest.y, valuator);
                            //polator1.interpolate(v_closest.x, v_closest.y, valuator);
                            //beta = polator1.getCoefficients();
                            normal = polator2.getSurfaceNormal();
                            //System.out.println(Arrays.toString(normal));
                            Vector3D normal2 = new Vector3D(normal[0],normal[1],normal[2]);

                            //System.out.println("angle: " + Math.toDegrees(AngleBetween(normal1, normal2)));
                            angle1 = Math.toDegrees(AngleBetween(normal3, normal2));

                            closest2 = tin1.getNeighborhoodPointsCollector().collectNeighboringVertices(v_closest.x, v_closest.y, 0, 0);

                            if(closest.size() > 0){

                                v_closest2 = closest2.get(0);

                                if(tin2.isPointInsideTin(v_closest2.x, v_closest2.y)){

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
                            tin2.remove(v_closest);
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
                                tin1.remove(v);
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
                        tin2.remove(v_closest);
                        v_closest = null;
                        polator2.resetForChangeToTin();
                        continue;
                    }


                }
                else{
                    tin1.remove(v);
                    v = null;
                    polator1.resetForChangeToTin();
                }

            }

            polator2.resetForChangeToTin();

            //this.vertices2.clear();
            //this.vertices2 = tin2.getVertices();


            for(org.tinfour.common.Vertex v : tin2.getVertices()){

                angle1 = -1.0;
                angle2 = -1.0;
                angle3 = -1.0;

                polator2.interpolate(v.x, v.y, valuator);
                normal = polator2.getSurfaceNormal();
                Vector3D normal3 = new Vector3D(normal[0],normal[1],normal[2]);

                if(tin1.isPointInsideTin(v.x, v.y)){

                    distance1 = Math.abs(v.getZ() - polator1.interpolate(v.x, v.y, valuator));
                    //beta = polator1.getCoefficients();
                    normal = polator1.getSurfaceNormal();
                    //Vector3D normal1 = new Vector3D(normal[0],normal[1],normal[2]);
                    closest = tin1.getNeighborhoodPointsCollector().collectNeighboringVertices(v.x, v.y, 0, 0);

                    if(closest.size() > 0){

                        v_closest = closest.get(0);

                        if(tin2.isPointInsideTin(v_closest.x, v_closest.y)){

                            polator1.interpolate(v_closest.x, v_closest.y, valuator);
                            //beta = polator2.getCoefficients();
                            normal = polator2.getSurfaceNormal();

                            Vector3D normal2 = new Vector3D(normal[0],normal[1],normal[2]);

                            //System.out.println("angle: " + Math.toDegrees(AngleBetween(normal1, normal2)));
                            angle1 = Math.toDegrees(AngleBetween(normal3, normal2));

                            //angle1 = calcNormal(closest);

                            closest2 = tin2.getNeighborhoodPointsCollector().collectNeighboringVertices(v_closest.x, v_closest.y, 0, 0);

                            if(closest2.size() > 0){

                                v_closest2 = closest2.get(0);

                                if(tin1.isPointInsideTin(v_closest2.x, v_closest2.y)){

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
                            tin1.remove(v_closest);
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
                                tin2.remove(v);
                                //tin2.remove(v_closest);
                                v = null;
                                //polator2.resetForChangeToTin();
                                polator2.resetForChangeToTin();

                            }else {
                                median2.add((distance1+distance2)/2.0d);
                            }


                        }


                    }else{
                        tin1.remove(v_closest);
                        v_closest = null;
                        polator1.resetForChangeToTin();
                        continue;
                    }


                }
                else{
                    tin2.remove(v);
                    v = null;
                    polator2.resetForChangeToTin();
                }

            }

            //System.out.print("\33[2K");
            //System.out.print("Vertices removed: " + changed + " Tin1 size: " + tin1.getVertices().size() + " Tin2 size: " + tin2.getVertices().size() + "\r");


            System.gc();
        }

        Collections.sort(median1);
        Collections.sort(median2);

        double mediaani1 = median1.get(median1.size()/2);
        double mediaani2 = median2.get(median2.size()/2);

        HashSet<Integer> remove1 = new HashSet<>();
        HashSet<Integer> remove2 = new HashSet<>();

        int count1 = -1;
        int count2 = -1;

        for(org.tinfour.common.Vertex v : tin2.getVertices()){
            count2++;

            if(tin1.isPointInsideTin(v.x, v.y)){

                distance2 = Math.abs(v.getZ() - polator1.interpolate(v.x, v.y, valuator));

                if(distance2 >= (2.0 * mediaani2))
                    remove2.add(count2);

            }else{
                remove2.add(count2);
            }

        }

        for(org.tinfour.common.Vertex v : tin1.getVertices()){
            count1++;

            if(tin2.isPointInsideTin(v.x, v.y)){

                distance1 = Math.abs(v.getZ() - polator2.interpolate(v.x, v.y, valuator));

                if(distance1 >= (2.0 * mediaani1))
                    remove1.add(count1);

            }else{
                remove1.add(count1);
            }

        }
        count1 = 0;
        count2 = 0;

        for(org.tinfour.common.Vertex v : tin2.getVertices()){

            if(remove2.contains(count2))
                tin2.remove(v);

            count2++;
        }
        for(org.tinfour.common.Vertex v : tin1.getVertices()){

            if(remove1.contains(count1))
                tin1.remove(v);

            count1++;
        }

        //System.out.println("Tin1 size: " + tin1.getVertices().size());
        //System.out.println("Tin2 size: " + tin2.getVertices().size());

    }
*/
    public double AngleBetween(Vector3D a, Vector3D b) {
        return 2.0d * Math.atan(length((a.subtract(b)))/length((a.add(b))));

    }

    public double length(Vector3D a){

        return Math.sqrt(Math.pow(a.getX(), 2) + Math.pow(a.getY(), 2) + Math.pow(a.getZ(), 2));

    }

    public double calcNormal(List<org.tinfour.common.Vertex> closest){

        double angle = -1.0;

        double[][] vs = new double[3][3];

        double[] left = new double[3];
        double[] right = new double[3];

        double[] a;
        double[] b;
        double[] c;

        if(closest.size() > 0){

            org.tinfour.common.Vertex key = null;

            for(int v1 = 0; v1 < 3; v1++){

                key = closest.get(v1);
                vs[v1][0] = key.getX();
                vs[v1][1] = key.getY();
                vs[v1][2] = key.getZ();

            }

            a = new double[]{vs[0][0], vs[0][1]};
            b = new double[]{vs[1][0], vs[1][1]};
            c = new double[]{vs[2][0], vs[2][1]};

            left[0] = vs[1][0] - vs[0][0];
            left[1] = vs[1][1] - vs[0][1];
            left[2] = vs[1][2] - vs[0][2];

            right[0] = vs[2][0] - vs[0][0];
            right[1] = vs[2][1] - vs[0][1];
            right[2] = vs[2][2] - vs[0][2];

        }

        if(beta.length > 3){

            double zX = beta[1];
            double zY = beta[2];
            double zXX = 2 * beta[3];
            double zYY = 2 * beta[4];
            double zXY = beta[4];
            double azimuth = Math.atan2(zY, zX);

            double grade = Math.sqrt(zX * zX + zY * zY);

            angle = Math.toDegrees(Math.atan(grade));



            if(normal.length > 0){

                double kateetti1 = euclideanDistance(0.0, 0.0, normal[0], normal[1]);

                double kateetti2 = normal[2];

                angle = angleHypo(1.0, kateetti2);

                if(!Double.isNaN(angle) && angle < 30){

                }
            }

        }

        return angle;
    }

    /**
     * Calculates the tangent angle
     *
     * @param oppositeSideLength
     * @param adjacentSideLength
     * @return the angle based on trigonometric function
     */

    public static double angle(double oppositeSideLength, double adjacentSideLength) {

        return Math.toDegrees(Math.atan(oppositeSideLength/ adjacentSideLength));

    }

    public void rotatePoint2(DoubleMatrix point, double time){

        makeRotationMatrix(tempMatrix,
                po_x_rot.value(time),
                po_y_rot.value(time),
                po_z_rot.value(time));

        rotatePoint(point, tempMatrix, (int)(time*1000), 0);

        tempTrans[0] = po_x.value(time);
        tempTrans[1] = po_y.value(time);
        tempTrans[2] = po_z.value(time);

    }

    public void rotatePoint(DoubleMatrix point, DoubleMatrix rotationMatrix, int time, int flightLine){


        //Find the closest timestamp in the trajectory file
        if(trajectory.size() > 0) {
            hehe = trajectory.ceilingEntry(time);

            locationOfAircraft = hehe.getValue();
        }else if(flightLine == 1){
            locationOfAircraft = file1_pivotPoint;
        }else if(flightLine == 2){
            locationOfAircraft = file2_pivotPoint;
        }

        new_x = locationOfAircraft[0] - point.get(0, 0);
        new_y = locationOfAircraft[1] - point.get(0, 1);
        new_z = locationOfAircraft[2] - point.get(0, 2);

        point.put(0, 0, new_x);
        point.put(0, 1, new_y);
        point.put(0, 2, new_z);


        rotatedPointMatrix = point.mmul(rotationMatrix);

        point.put(0, 0, locationOfAircraft[0] - rotatedPointMatrix.get(0, 0));
        point.put(0, 1, locationOfAircraft[1] - rotatedPointMatrix.get(0, 1));
        point.put(0, 2, locationOfAircraft[2] - rotatedPointMatrix.get(0, 2));


    }

    public void translatePoint(DoubleMatrix point, double[] translation){

        new_x = point.get(0,0) + translation[0];
        new_y = point.get(0,1) + translation[1];
        new_z = point.get(0,2) + translation[2];

        point.put(0,0, new_x);
        point.put(0,1, new_y);
        point.put(0,2, new_z);
    }


    public void makeRotationMatrix(DoubleMatrix output, double x, double y, double z){

        x = Math.toRadians(x);
        y = Math.toRadians(y);
        z = Math.toRadians(z);

        // FIRST ROW DONE!
        output.put(0,0, (Math.cos(y) * Math.cos(z)));
        output.put(0,1, (Math.cos(x) * Math.sin(z)) + Math.sin(x) * Math.sin(y) * Math.cos(z));
        output.put(0,2, (Math.sin(x) * Math.sin(z)) - Math.cos(x) * Math.sin(y) * Math.cos(z));

        // SECOND ROW DONE!
        output.put(1,0, (-Math.cos(y) * Math.sin(z)));
        output.put(1,1, (Math.cos(x) * Math.cos(z)) - Math.sin(x) * Math.sin(y) * Math.sin(z));
        output.put(1,2, (Math.sin(x) * Math.cos(z)) + Math.cos(x) * Math.sin(y) * Math.sin(z));

        // THIRD ROW
        output.put(2,0, Math.sin(y));
        output.put(2,1, -Math.sin(x) * Math.cos(y));
        output.put(2,2, Math.cos(x) * Math.cos(y));
    }


}

