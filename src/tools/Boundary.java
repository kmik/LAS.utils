package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.PointInclusionRule;
import err.toolException;
import javafx.scene.shape.Path;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;

import org.tinfour.common.IQuadEdge;
import org.tinfour.common.Vertex;
import org.tinfour.utils.Polyside;
import utils.argumentReader;
import utils.fileOperations;

import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.tinfour.utils.Polyside.isPointInPolygon;


/**
 * Calculates the polygon boundary of a LAS file.
 * Can be either convex or concave (not implemented yet).
 *
 * @author  Kukkonen Mikko
 * @version 0.1
 * @since 06.03.2018
 */

public class Boundary extends tool{

    fileOperations fo = new fileOperations();


    ArrayList<double[]> border = new ArrayList<>();

    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
    org.tinfour.standard.IncrementalTin perimTin = new org.tinfour.standard.IncrementalTin();
    HashSet<org.tinfour.common.Vertex> perimeterVertices = new HashSet<org.tinfour.common.Vertex>();
    List<org.tinfour.common.IQuadEdge> edges = null;

    TreeMap<Double, Vertex> concaveOrder = new TreeMap<Double, org.tinfour.common.Vertex>();

    List<org.tinfour.common.IQuadEdge> concaveEdges = null;

    Point[] hullInput = null;
    LASReader pointCloud = null;
    PointInclusionRule rule = new PointInclusionRule();

    double[] corners = new double[4];

    Point[] boundary;
    //ArrayList<ConcaveHull.Point> concave;

    boolean concave = true;

    Point[] concaveHullInput = null;
    String odir;
    String outputFileName;

    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;

    Point pivotPoint = null;

    HashSet<org.tinfour.common.Vertex> vertexSetConcave = new HashSet<org.tinfour.common.Vertex>();

    ArrayList<org.tinfour.common.Vertex> vertexSetConcave2 = new ArrayList<org.tinfour.common.Vertex>();

    TreeSet<Point> pointSetConcave = new TreeSet<Point>();

    ArrayList<Point> pointSetConcave2 = new ArrayList<Point>();

    argumentReader aR;

    HashSet<Integer> perim = new HashSet<>();

    ArrayList<double[]> segments = new ArrayList<>();

    int concavity = 75;

    int coreNumber = 0;

    public Boundary(){


    }

    /**
     * Constructor
     *
     * @param pointCloud2 		Input point cloud
     * @param odir2 				Output directory
     * @param outputFileName2 	Output point cloud name
     * @param concaveOrNot		Concace (true), convec (false)
     *
     */

    public Boundary(LASReader pointCloud2, String odir2, String outputFileName2, boolean concaveOrNot, argumentReader aR, int coreNumber) throws IOException {


        this.coreNumber = coreNumber;

        this.aR = aR;


        this.concavity = (int)aR.concavity;

        this.pointCloud = pointCloud2;
        this.odir = aR.odir;
        this.outputFileName = aR.output;
        this.concave = concaveOrNot;

        aR.p_update.lasborder_mode = "concave";

        concave = aR.concave;

        if(concave){
            try {
                makeConcave3();
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            try {
                makeConvex();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    /**
     * Calculates the perimeter from TIN
     * Saves the TIN in
     */

    public void updatePerimeter(){

        perimTin.clear();
        perimeterVertices.clear();

        edges = tin.getPerimeter();
        concaveEdges = tin.getPerimeter();

        for (org.tinfour.common.IQuadEdge edge : edges) {

            perimeterVertices.add(edge.getA());
            perimeterVertices.add(edge.getB());

        }

        for(org.tinfour.common.Vertex v : perimeterVertices){

            if(v.y < minY){
                minY = v.y;
                pivotPoint = new Point(v.x, v.y + 100.0);
            } else if (v.y < minY) {
                if (v.x > maxX){
                    pivotPoint = new Point(v.x, v.y + 100.0);
                    maxX = v.x;
                }
            }

            perimTin.add(v);

        }
    }

    /**
     * Determines a minimum octagon area for the shapefile
     * in order to reduce the number of points before calculation.
     */

    public void prune() throws IOException{

        long n = pointCloud.getNumberOfPointRecords();

        LasPoint tempPoint = new LasPoint();

        double maxA = Double.NEGATIVE_INFINITY;
        double maxB = Double.NEGATIVE_INFINITY;
        double minC = Double.POSITIVE_INFINITY;
        double minD = Double.POSITIVE_INFINITY;

        double[] north_east = new double[2]; //b
        double[] south_east = new double[2]; //a
        double[] south_west = new double[2]; //d
        double[] north_west = new double[2]; //c


        for(int i = 0; i < n; i++){

            // NOT USED
            pointCloud.readRecord(i, tempPoint);

            if(tempPoint.x - tempPoint.y > maxA){
                maxA = tempPoint.x - tempPoint.y;
                south_east[0] = tempPoint.x;
                south_east[1] = tempPoint.y;
            }

            if(tempPoint.x + tempPoint.y > maxB){
                maxB = tempPoint.x + tempPoint.y;
                north_east[0] = tempPoint.x;
                north_east[1] = tempPoint.y;
            }

            if(tempPoint.x - tempPoint.y < minC){
                minC = tempPoint.x - tempPoint.y;
                north_west[0] = tempPoint.x;
                north_west[1] = tempPoint.y;
            }

            if(tempPoint.x + tempPoint.y < minD){
                minD = tempPoint.x + tempPoint.y;
                south_west[0] = tempPoint.x;
                south_west[1] = tempPoint.y;
            }

        }

        corners[0] = Math.max(north_west[0], south_west[0]);
        corners[1] = Math.min(north_east[0], south_east[0]);

        corners[2] = Math.max(south_east[1], south_west[1]);
        corners[3] = Math.min(north_east[1], north_west[1]);

    }

    /**
     * This is the easy one. Just add a point to the tin if it
     * lies outside it. The final boundary is then the boundary
     * of the tin.
     * @throws Exception
     */
    public void makeConvex() throws Exception{

        long n = pointCloud.getNumberOfPointRecords();
        LasPoint tempPoint = new LasPoint();

        if(false)
        if(!pointCloud.isIndexed){
            pointCloud.index(10);
        }

        org.tinfour.common.Vertex tempV;

        List<IQuadEdge> currentBorder = null;

        int thread_n = aR.pfac.addReadThread(pointCloud);

        for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000) {

            int maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

        //for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i++) {

            //pointCloud.readRecord(i, tempPoint);
                /* Reading, so ask if this point is ok, or if
                    it should be modified.
                     */
                if(!aR.inclusionRule.ask(tempPoint, i + j, true)){
                    continue;
                }
                if (tin.isBootstrapped()) {

                    if (currentBorder == null) {
                        currentBorder = tin.getPerimeter();
                    }

                    if (isPointInPolygon(currentBorder, tempPoint.x, tempPoint.y) == Polyside.Result.Outside) {

                        tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, 0.0);
                        tin.add(tempV);
                        currentBorder = tin.getPerimeter();


                    }
                } else {
                    tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, 0.0);
                    tin.add(tempV);
                }

                if (i + j % 10000 == 0) {
                    calcPerim();
                }
            }
            //System.out.println("ASD");
        }

        if(!tin.isBootstrapped()){
            throw new toolException("TIN is not bootstrapped, not enought points in the .las file?");
        }

        perimeterVertices.clear();
        perimTin.clear();
        concaveEdges = tin.getPerimeter();
        concaveHullInput = new Point[concaveEdges.size()];
        double[] start = new double[]{0,0};

        for(int i = 0; i < concaveEdges.size(); i++){


            org.tinfour.common.Vertex tempA = concaveEdges.get(i).getA();
            org.tinfour.common.Vertex tempB = concaveEdges.get(i).getB();

            if(i == 0){
                start[0] = tempA.x;
                start[1] = tempA.y;
            }

            border.add(new double[]{tempA.x, tempA.y});

            if(!vertexSetConcave.contains(tempA)){

                vertexSetConcave.add(tempA);
                vertexSetConcave2.add(tempA);


            }

            if(!vertexSetConcave.contains(tempB)){
                vertexSetConcave.add(tempB);
                vertexSetConcave2.add(tempB);
            }

        }



        border.add(new double[]{start[0], start[1]});

        String oput = "";

        if(aR.output.equals("asd"))
            oput = fo.createNewFileWithNewExtension(pointCloud.getFile(), ".shp").getAbsolutePath();
        // pointCloud.path.getParent() + pathSep + pointCloud.path.getName().split(".las")[0] + ".shp";

        if(!aR.odir.equals("asd")){

            oput = fo.transferDirectories(new File(oput), aR.odir).getAbsolutePath();

        }

        exportShapefile(oput);

    }

    /**
     * This is a difficult one to implement. Need to do some clever spatial indexing.
     * Depends on some parameters, such as
     * @throws Exception
     */
    public void makeConcave3() throws Exception{

        HashSet<Double> donet = new HashSet<Double>();

        List<org.tinfour.common.Vertex> closest = new ArrayList<>();

        LasPoint tempPoint = new LasPoint();

        aR.p_update.updateProgressNBorder();

        if(!pointCloud.isIndexed()){
            aR.p_update.threadFile[coreNumber-1] = "indexing";
            aR.p_update.updateProgressNBorder();
            pointCloud.index(this.concavity);
        }else{
            pointCloud.getIndexMap();
        }

        aR.p_update.threadFile[coreNumber-1] = "finding lowest Y";
        aR.p_update.updateProgressNBorder();
        org.tinfour.common.Vertex tempV;

        int count = 0;

        double minY = Double.POSITIVE_INFINITY;

        int counter = 0;
        long minYindex = 0;

        int maxi = 0;

        //pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

        aR.p_update.threadEnd[coreNumber-1] = (long)pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber-1] = 0;

        /* First find the index of the lowest Y point
         */
        for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000){

            maxi = (int)Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            pointCloud.readRecord_noRAF(i, tempPoint, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!rule.ask(tempPoint, i+j, true)){
                    continue;
                }

                if(tempPoint.y < minY) {
                    minYindex = counter;
                    minY = tempPoint.y;
                }

                counter++;
                aR.p_update.threadProgress[coreNumber-1] = counter;

                if(counter % 100000 == 0){
                    aR.p_update.updateProgressNoise();
                }
            }
        }

        aR.p_update.threadFile[coreNumber-1] = "finding border";

        aR.p_update.updateProgressNBorder();

        LasPoint tempPoint1 = new LasPoint();
        LasPoint tempPoint2 = new LasPoint();
        LasPoint tempPoint3 = new LasPoint();

        /*
          tempPoint3 now holds the lowest Y point
         */
        pointCloud.readRecord(minYindex, tempPoint3);

        long startIndex = minYindex;

        double[] prevSegment = new double[]{0,0,0,0};

        tempPoint1.x = tempPoint3.x - 100;
        tempPoint1.y = tempPoint3.y;

        HashSet<Long> doneIndexes = new HashSet<>();

        doneIndexes.add((long)startIndex);

        Path2D path = new Path2D.Double();
        //Path path2 = new Path();

        path.moveTo(tempPoint3.x, tempPoint3.y);

        double angle;
        //ArrayList<double[]> border = new ArrayList<>();

        border.add(new double[]{tempPoint3.x, tempPoint3.y});

        //tin.add(new org.tinfour.common.Vertex(tempPoint1.x, tempPoint1.y, 0));
        tin.add(new org.tinfour.common.Vertex(tempPoint3.x, tempPoint3.y, 0));

        int size = 1;

        double increase = 0.0;


        while (this.pointCloud.queriedIndexes2.size() <= 1) {

            //this.pointCloud.query2(tempPoint3.x - concavity + increase, tempPoint3.x + concavity + increase, tempPoint3.y - concavity + increase, tempPoint3.y + concavity + increase);
            pointCloud.queryPoly2(tempPoint3.x - concavity + increase, tempPoint3.x + concavity + increase, tempPoint3.y - concavity + increase, tempPoint3.y + concavity + increase);
            increase += 5.0;


        }
        boolean running = true;

        int counti = 0;
        double dist = -1;

        long prevIndex = startIndex;

        double minidisti =-5.0;

        aR.p_update.threadInt[coreNumber-1] = 0;

        aR.p_update.threadEnd[coreNumber-1] = -1;
        aR.p_update.threadProgress[coreNumber-1] = -1;

        aR.p_update.updateProgressNBorder();

        int counteri = 0;

        /* Seems sketchy, but trust me, it will halt! */
        while(running) {

            minidisti =-5.0;

            double smallestAngle = Double.POSITIVE_INFINITY;
            long angleIndex = 0;


            /*
            for (int u = 0; u < pointCloud.queriedIndexes2.size(); u++) {

                    long n1 = pointCloud.queriedIndexes2.get(u)[1] - pointCloud.queriedIndexes2.get(u)[0];
                    long n2 = pointCloud.queriedIndexes2.get(u)[1];

                    int parts = (int)Math.ceil((double)n1 / 20000.0);
                    int jako = (int)Math.ceil((double)n1 / (double) parts);

                    int pienin;
                    int suurin;
                    int ero;

                    for(int i = 1; i <= parts; i++) {

                        if(i != parts){

                            pienin = (i - 1) * jako;
                            suurin = i * jako;
                        }

                        else{
                            pienin = (i - 1) * jako;
                            suurin = (int)n1;
                        }

                        pienin = pienin + pointCloud.queriedIndexes2.get(u)[0];
                        suurin = suurin + pointCloud.queriedIndexes2.get(u)[0];
                        ero = suurin - pienin;




                        int count1 = 0;

                        for (int p = pienin; p <suurin; p++) {

                            //System.out.println("READ");
                            pointCloud.readFromBuffer(tempPoint2);
*/
            int counter1 = 0;
            //System.out.println("Start reading query " + pointCloud.queriedIndexes2.size());
            while(!pointCloud.index_read_terminated){


                long p = pointCloud.fastReadFromQuery(tempPoint2);
                counter1++;

                //System.out.println("p = " + p + " angleindex: " + angleIndex);
                            /* Reading, so ask if this point is ok, or if
                            it should be modified.
                             */
                            if(!rule.ask(tempPoint2, p, true)){
                                continue;
                            }

                            dist = euclideanDistance(tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y);

                            if (p != prevIndex && dist < concavity && dist > 0.1) { //  && !path.contains(tempPoint2.x, tempPoint2.y)

// tempPoint1 is the first point in line
                                // tempPoint2 is the current point
                                // tempPoint3 is the previous point
                                angle = angleBetween2(tempPoint1.x, tempPoint1.y, tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y);
                                double angle3 = calculateAngleDeviation(tempPoint1.x, tempPoint1.y, tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y);
                                double angle2 = angleBetween(tempPoint1.x, tempPoint1.y, tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y);
                                double angle4 = calculateAngle_(tempPoint1.x, tempPoint1.y, tempPoint3.x, tempPoint3.y, tempPoint2.x, tempPoint2.y);


                                double angle_test = findAngle(tempPoint1.x, tempPoint1.y, tempPoint3.x, tempPoint3.y, tempPoint2.x, tempPoint2.y);

                                //System.out.println("angle: " + angle + " angle2 " + angle2 + " angle3 " + angle3 + " angle4 " + angle4 + " angle_test " + angle_test);
                                //System.exit(1);

                                // angle_test distance from 180 degrees

                                angle = angle_test;
                                 angle = angle4;

                                if (angle < smallestAngle && angle != 0.0) {


                                    boolean iter = intersection(tempPoint3.x, tempPoint3.y, tempPoint2.x, tempPoint2.y);

                                    //System.out.println("GOT HERE! " + iter);
                                     //
                                    if (!iter &&  angle > 1.0 || (prevIndex != startIndex && p == startIndex)) {

                                        minidisti = dist;
                                        angleIndex = p;
                                        smallestAngle = angle;
                                        tempPoint.x = tempPoint2.x;
                                        tempPoint.y = tempPoint2.y;

                                        aR.p_update.threadInt[coreNumber-1]++;

                                        if(aR.p_update.threadInt[coreNumber-1] % 100 == 0)
                                            aR.p_update.updateProgressNBorder();
                                    }else{

                                    }
                                }
                            }
                            //count1++;


            }

            pointCloud.resetReadPoints();

            // print point 1 x and y
            System.out.println("Point 1: " + tempPoint1.x + " " + tempPoint1.y);
            // print point 2 x and y
            System.out.println("Point 3: " + tempPoint3.x + " " + tempPoint3.y);
            System.out.println("Point 2: " + tempPoint.x + " " + tempPoint.y);
            System.out.println("anleindex: " + angleIndex + " " + prevIndex);
            // print point 3 x and y
            System.out.println("----------------------");

            System.out.println("smallestangle: " + smallestAngle + " " + minidisti + " " + angleIndex + " " + counter1 + " " + border.size());

            /* prevIndex always points to the previous vertex in the boundary. This guarantees that
              we don't trace back our steps in the bounday.
             */
            prevIndex = (int)angleIndex;

            prevSegment[0] = tempPoint1.x;
            prevSegment[1] = tempPoint1.y;
            prevSegment[2] = tempPoint3.x;
            prevSegment[3] = tempPoint3.y;

            segments.add(prevSegment.clone());

            angleIndex = Math.min(angleIndex, pointCloud.numberOfPointRecords - 1);
            /* tempPoint now holds the next vertice of the boundary */
            //pointCloud.readRecord((int)angleIndex, tempPoint);

            /* Assign temporary points as planned */
            tempPoint1.x = tempPoint3.x;
            tempPoint1.y = tempPoint3.y;
            tempPoint3.x = tempPoint.x;
            tempPoint3.y = tempPoint.y;

            /* Add the vertice to the boundary */
            border.add(new double[]{tempPoint.x, tempPoint.y});

            increase = 0.0;
            pointCloud.queriedIndexes2.clear();
            /* We search for points in the proximity of the new boundary vertex.
             */
            while (pointCloud.queriedIndexes2.size() <= 1) {
                //pointCloud.query2(tempPoint.x - concavity + increase, tempPoint.x + concavity + increase, tempPoint.y - concavity + increase, tempPoint.y + concavity + increase);
                pointCloud.queryPoly2(tempPoint.x - concavity + increase, tempPoint.x + concavity + increase, tempPoint.y - concavity + increase, tempPoint.y + concavity + increase);
                increase += 5.0;

                System.out.println("HERE! " + increase);
            }

            /* Here is the termination condition! If the current vertex is the same as the starting vertex,
              we stop!
             */

            //System.out.println("angleIndex: " + angleIndex + " startIndex: " + startIndex);
            if(angleIndex == startIndex || smallestAngle == Double.POSITIVE_INFINITY)
                running = false;

            System.out.println(angleIndex + " " + startIndex + " " + running + " "  + pointCloud.numberOfPointRecords);

            counti++;

            counteri++;
        }

        concaveEdges = tin.getPerimeter();

        System.out.println("final vertices: " + border.size());

        concaveHullInput = new Point[concaveEdges.size()];

        Point comparatorPoint = new Point(1,1);


        for(int i = 0; i < concaveEdges.size(); i++){

            org.tinfour.common.Vertex tempA = concaveEdges.get(i).getA();
            org.tinfour.common.Vertex tempB = concaveEdges.get(i).getB();

            if(!vertexSetConcave.contains(tempA)){
                vertexSetConcave.add(tempA);
                vertexSetConcave2.add(tempA);
            }

            if(!vertexSetConcave.contains(tempB)){
                vertexSetConcave.add(tempB);
                vertexSetConcave2.add(tempB);
            }

        }

        String pathSep = System.getProperty("file.separator");

        String oput = "";

        if(aR.output.equals("asd"))
            oput = fo.createNewFileWithNewExtension(pointCloud.getFile(), ".shp").getAbsolutePath();
        // pointCloud.path.getParent() + pathSep + pointCloud.path.getName().split(".las")[0] + ".shp";

        if(!aR.odir.equals("asd")){

            oput = fo.transferDirectories(new File(oput), aR.odir).getAbsolutePath();

        }

        exportShapefile(oput);

    }

    public static double calculateAngle_(double x1, double y1, double x2, double y2, double x3, double y3) {
        double angle1 = Math.atan2(y1 - y2, x1 - x2);
        double angle2 = Math.atan2(y3 - y2, x3 - x2);
        double angle = Math.toDegrees(angle2 - angle1);

        // Ensure the angle is positive
        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    public static double calculateAngle__(double x1, double y1, double x2, double y2, double x3, double y3) {
        double angle1 = Math.atan2(y2 - y1, x2 - x1);
        double angle2 = Math.atan2(y3 - y2, x3 - x2);
        double angle = Math.toDegrees(angle2 - angle1);

        // Ensure the angle is positive
        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    public boolean intersection(double x1, double y1, double x2, double y2){

        for(int i = 0; i < segments.size(); i++)
            if(intersects(segments.get(i)[0], segments.get(i)[1], segments.get(i)[2], segments.get(i)[3],
                    x1, y1, x2, y2))
                return true;

        return false;
    }

    boolean intersects(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double bx = x2 - x1;
        double by = y2 - y1;
        double dx = x4 - x3;
        double dy = y4 - y3;
        double b_dot_d_perp = bx * dy - by * dx;
        if (b_dot_d_perp == 0) {
            return false;
        }
        double cx = x3 - x1;
        double cy = y3 - y1;
        double t = (cx * dy - cy * dx) / b_dot_d_perp;
        if (t < 0 || t > 1) {
            return false;
        }
        double u = (cx * by - cy * bx) / b_dot_d_perp;
        return !(u < 0) && !(u > 1);
    }

    /**
     * Calculate angle between two lines with two given points
     *
     * @return Angle between two lines in degrees
     */

    public static double angleBetween2Lines(double A1_x, double A1_y, double A2_x, double A2_y, double B1_x, double B1_y, double B2_x, double B2_y) {

        double angle1 =  Math.atan2(A2_y - A1_y, A1_x - A2_x);
        double angle2 =  Math.atan2(B2_y - B1_y, B1_x - B2_x);
        double calculatedAngle = Math.toDegrees(angle1 - angle2);
        if (calculatedAngle < 0) calculatedAngle += 360;

        return calculatedAngle;
    }

    public static double angleBetween(double x1, double y1, double x2, double y2, double x3, double y3) {

        double deltax1 = x1 - x3;
        double deltay1 = y1 - y3;

        double deltax2 = x2 - x3;
        double deltay2 = y2 - y3;

        double angle1 = Math.atan2(deltay1, deltax1);
        double angle2 = Math.atan2(deltay2, deltax2);

        return Math.toDegrees(angle2 - angle1);
    }

    public static double findAngle(double x1, double y1, double x2, double y2, double x3, double y3) {
        // Calculate the distances between the points
        double ab = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        double bc = Math.sqrt(Math.pow(x3 - x2, 2) + Math.pow(y3 - y2, 2));
        double ac = Math.sqrt(Math.pow(x3 - x1, 2) + Math.pow(y3 - y1, 2));

        // Use the law of cosines to find the angle at point B
        double cosB = (Math.pow(ab, 2) + Math.pow(bc, 2) - Math.pow(ac, 2)) / (2 * ab * bc);
        double angleB = Math.acos(cosB);

        // Convert the angle from radians to degrees and adjust to the range from 0 to 360 degrees
        double angleB_degrees = Math.toDegrees(angleB);
        if (y3 > y2) {
            angleB_degrees = 360 - angleB_degrees;
        }

        // Return the angle
        return angleB_degrees;
    }

    public double angleBetween2(double x1, double y1, double x2, double y2, double x3, double y3) {
        double ab = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        double bc = Math.sqrt(Math.pow(x3 - x2, 2) + Math.pow(y3 - y2, 2));
        double ac = Math.sqrt(Math.pow(x3 - x1, 2) + Math.pow(y3 - y1, 2));

        double angle = Math.toDegrees(Math.acos((bc * bc + ab * ab - ac * ac) / (2 * bc * ab)));
        angle = Math.toDegrees(Math.acos((ac * ac + ab * ab - bc * bc) / (2 * ac * ab)));
        return angle;
    }

    public static double calculateAngleDeviation(double x1, double y1, double x2, double y2, double x3, double y3) {
        // Calculate the equation of the line passing through (x1, y1) and (x3, y3)
        double slope = (y3 - y1) / (x3 - x1);
        double yIntercept = y1 - slope * x1;

        // Find the y-coordinate of the point on the line with the same x-coordinate as (x2, y2)
        double yOnLine = slope * x2 + yIntercept;

        // Calculate the angle between the line and the line segment connecting (x2, y2) and the point on the line
        double angleRadians = Math.atan2(yOnLine - y2, x3 - x1) - Math.atan2(y3 - y1, x2 - x1);
        double angleDegrees = Math.toDegrees(angleRadians);

        // Convert the angle to the range [-180, 180] degrees
        if (angleDegrees < -180) {
            angleDegrees += 360;
        } else if (angleDegrees > 180) {
            angleDegrees -= 360;
        }

        return Math.abs(angleDegrees);
    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }

    private double calculateAngle(double P1X, double P1Y, double P2X, double P2Y,
                                  double P3X, double P3Y){

        double numerator = P2Y*(P1X-P3X) + P1Y*(P3X-P2X) + P3Y*(P2X-P1X);
        double denominator = (P2X-P1X)*(P1X-P3X) + (P2Y-P1Y)*(P1Y-P3Y);
        double ratio = numerator/denominator;

        //System.out.println(ratio);

        double angleRad = Math.atan(ratio);
        double angleDeg = (angleRad*180.0)/Math.PI;

        if(angleDeg<0){
            angleDeg = 180+angleDeg;
        }

        return angleDeg;
    }

    public void calcPerim(){

        concaveEdges = tin.getPerimeter();

        this.perim.clear();

        for(int i = 0; i < concaveEdges.size(); i++){

            this.perim.add(concaveEdges.get(i).getA().getIndex());
            this.perim.add(concaveEdges.get(i).getB().getIndex());
        }

    }

    /** THIS IS SLOOOOOOOOOW */

    public void makeConcave2() throws IOException{

        ConcaveHull conkaavi = new ConcaveHull();

        String pathSep = System.getProperty("file.separator");

        ArrayList<ConcaveHull.Point> pisteet = new ArrayList<ConcaveHull.Point>();

        ArrayList<ConcaveHull.Point> hulli = new ArrayList<ConcaveHull.Point>();

        long n = pointCloud.getNumberOfPointRecords();
        LasPoint tempPoint = new LasPoint();

        for(int i = 0; i < n; i++){

            pointCloud.readRecord(i, tempPoint);

            pisteet.add(new ConcaveHull.Point(tempPoint.x, tempPoint.y));

        }

        hulli = ConcaveHull.calculateConcaveHull(pisteet, 100);

        String oput = "";

        if(aR.output.equals("asd"))
            oput = fo.createNewFileWithNewExtension(pointCloud.getFile(), ".shp").getAbsolutePath();
        // pointCloud.path.getParent() + pathSep + pointCloud.path.getName().split(".las")[0] + ".shp";

        if(!aR.odir.equals("asd")){

            oput = fo.transferDirectories(new File(oput), aR.odir).getAbsolutePath();

        }

        exportShapefile(oput);

    }

    public double pointToLineDistance(org.tinfour.common.Vertex A, org.tinfour.common.Vertex B, org.tinfour.common.Vertex P) {

        double normalLength = Math.sqrt((B.x-A.x)*(B.x-A.x)+(B.y-A.y)*(B.y-A.y));
        return Math.abs((P.x-A.x)*(B.y-A.y)-(P.y-A.y)*(B.x-A.x))/normalLength;
    }

    /**
     * Calculates a convex hull polygon to represent
     * the extent of LAS points.
     *
     * @param getConvex 		I don't know why?
     */

    public void make(boolean getConvex) throws IOException{

        long n = pointCloud.getNumberOfPointRecords();
        Point[] hullInputTemp = new Point[(int)n];

        LasPoint tempPoint = new LasPoint();
        int hullCount = 0;

        for(int i = 0; i < n; i++){

            pointCloud.readRecord(i, tempPoint);

            if(tempPoint.x >= corners[1] || tempPoint.x <= corners[0] || tempPoint.y <= corners[2] || tempPoint.y >= corners[3]){
                //concPoints.add(new ConcaveHull.Point(tempPoint.x, tempPoint.y));
                hullInputTemp[hullCount] = new Point(tempPoint.x, tempPoint.y);
                hullCount++;
            }


        }

        hullInput = Arrays.copyOfRange(hullInputTemp, 0, (hullCount - 1) );

        ConvexHull hulli = new ConvexHull(hullInput);

        hulli.computeConvexHull();

        boundary = hulli.getConvexHull();

        if(getConvex)
            return;


        String pathSep = System.getProperty("file.separator");

        if(odir.equals("asd")){
            //System.out.println("GOT HERE");
            if(outputFileName.equals("asd"))
                outputFileName = pointCloud.path.getParent() + pathSep + pointCloud.path.getName().split(".las")[0] + ".shp";
        }
        else
            outputFileName = odir + pathSep + pointCloud.path.getName().split(".las")[0] + ".shp";

        exportShapefile(outputFileName);

    }

    /**
     * Writes the shapefile
     *
     * @param outputFileName 		Output shapefile name
     */

    public void exportShapefile(String outputFileName){

        //ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        Geometry outShpGeom2 = new Geometry(ogr.wkbLinearRing);
        Geometry outShpGeom = new Geometry(ogr.wkbPolygon);

        String driverName = "ESRI Shapefile";

        Driver shpDriver = ogr.GetDriverByName(driverName);

        DataSource outShp = shpDriver.CreateDataSource(outputFileName);

        Layer outShpLayer = outShp.CreateLayer("out_name", null, 0);

        FieldDefn layerFieldDef = new FieldDefn("id",0);

        outShpLayer.CreateField(layerFieldDef);
        FeatureDefn outShpFeatDefn = outShpLayer.GetLayerDefn();
        Feature outShpFeat = new Feature(outShpFeatDefn);


        if(!concave) {

            for(int i = 0; i < border.size(); i++) {

                //System.out.println("Wrote " + i);
                outShpGeom2.AddPoint_2D(border.get(i)[0], border.get(i)[1]);
            }

            if(false)
            for (int i = 0; i < concaveHullInput.length; i++) {
                outShpGeom2.AddPoint_2D(boundary[i].x, boundary[i].y);

            }
        }
        if(concave) {
            for (int i = 0; i < border.size(); i++)
                outShpGeom2.AddPoint_2D(border.get(i)[0], border.get(i)[1]);
        }
        outShpGeom.AddGeometry(outShpGeom2);

        outShpFeat.SetField("id",0);

        outShpFeat.SetGeometryDirectly(outShpGeom);
        outShpLayer.CreateFeature(outShpFeat);
        outShpLayer.SyncToDisk();
        System.out.println("features: " + outShpLayer.GetFeatureCount());



    }

}
