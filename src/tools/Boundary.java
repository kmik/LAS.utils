package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.PointInclusionRule;
import javafx.scene.shape.Path;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;

import org.tinfour.common.Vertex;
import utils.argumentReader;
import utils.fileOperations;

import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.util.*;


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
        //tinfour.semivirtual.SemiVirtualIncrementalTin tempTin = new tinfour.semivirtual.SemiVirtualIncrementalTin();

        for(int i = 0; i < edges.size(); i++){

            perimeterVertices.add(edges.get(i).getA());
            perimeterVertices.add(edges.get(i).getB());

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
            //	tin.add(v);
        }

        //tin = perimTin;

        //System.out.println("NEW PERIM: " + perimeterVertices.size());
    }

    /**
     * Determines a minimum octagon area for the shapefile
     * in order to reduce the number of points before calculation.
     */

    public void prune() throws IOException{

        long n = pointCloud.getNumberOfPointRecords();

        LasPoint tempPoint = new LasPoint();

			/*
			A = (Ax, Ay) which maximizes x-y
			B = (Bx, Xy) which maximizes x+y
			C = (Cx, Cy) which minimizes x-y
			D = (Dx, Dy) which minimizes x+y
			*/

        double maxA = Double.NEGATIVE_INFINITY;
        double maxB = Double.NEGATIVE_INFINITY;
        double minC = Double.POSITIVE_INFINITY;
        double minD = Double.POSITIVE_INFINITY;

        double[] koillinen = new double[2]; //b
        double[] kaakko = new double[2]; //a
        double[] lounas = new double[2]; //d
        double[] luode = new double[2]; //c


        for(int i = 0; i < n; i++){

            pointCloud.readRecord(i, tempPoint);

            if(tempPoint.x - tempPoint.y > maxA){
                maxA = tempPoint.x - tempPoint.y;
                kaakko[0] = tempPoint.x;
                kaakko[1] = tempPoint.y;
            }

            if(tempPoint.x + tempPoint.y > maxB){
                maxB = tempPoint.x + tempPoint.y;
                koillinen[0] = tempPoint.x;
                koillinen[1] = tempPoint.y;
            }

            if(tempPoint.x - tempPoint.y < minC){
                minC = tempPoint.x - tempPoint.y;
                luode[0] = tempPoint.x;
                luode[1] = tempPoint.y;
            }

            if(tempPoint.x + tempPoint.y < minD){
                minD = tempPoint.x + tempPoint.y;
                lounas[0] = tempPoint.x;
                lounas[1] = tempPoint.y;
            }

        }

        corners[0] = Math.max(luode[0], lounas[0]);
        corners[1] = Math.min(koillinen[0], kaakko[0]);

        corners[2] = Math.max(kaakko[1], lounas[1]);
        corners[3] = Math.min(koillinen[1], luode[1]);

    }

    /**
     * This is the easy one. Just add a point to the tin if it
     * lies outside it. The final boundary is then the boundary
     * of the tin.
     * @throws Exception
     */
    public void makeConvex() throws Exception{

        HashSet<Double> donet = new HashSet<Double>();

        //Arrays.sort(boundary);

        List<org.tinfour.common.Vertex> closest = new ArrayList<>();

        org.tinfour.interpolation.TriangularFacetInterpolator polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);
        org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

        long n = pointCloud.getNumberOfPointRecords();
        LasPoint tempPoint = new LasPoint();

        if(!pointCloud.isIndexed){
            pointCloud.index(10);
        }

        org.tinfour.common.Vertex tempV;

        int count = 0;

        for(int i = 0; i < n; i++){

            count = 0;

            pointCloud.readRecord(i, tempPoint);

            if(!tin.isPointInsideTin(tempPoint.x, tempPoint.y)){

                tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, 0.0);
                //perim.add(i);
                tempV.setIndex(i);
                tin.add(tempV);

            }

            if(i % 10000 == 0){
                calcPerim();
            }

        }

        perimeterVertices.clear();

        perimTin.clear();

        concaveEdges = tin.getPerimeter();

        concaveHullInput = new Point[concaveEdges.size()];

        Point comparatorPoint = new Point(1,1);

        double[] start = new double[]{0,0};

        for(int i = 0; i < concaveEdges.size(); i++){


            org.tinfour.common.Vertex tempA = concaveEdges.get(i).getA();
            org.tinfour.common.Vertex tempB = concaveEdges.get(i).getB();

            if(i == 0){
                start[0] = tempA.x;
                start[1] = tempA.y;
            }

            //System.out.println("ADD " + i);
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
            pointCloud.index(40);
        }else{
            pointCloud.getIndexMap();
        }

        aR.p_update.threadFile[coreNumber-1] = "finding lowest Y";
        aR.p_update.updateProgressNBorder();
        org.tinfour.common.Vertex tempV;

        int count = 0;

        double minY = Double.POSITIVE_INFINITY;

        int counter = 0;
        int minYindex = 0;

        //pointCloud.readRecord(0, tempPoint);

        int maxi = 0;

        counter = 0;

        pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

        aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber-1] = 0;

        /* First find the index of the lowest Y point
         */
        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000){

            maxi = (int)Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            count = 0;
            pointCloud.readRecord_noRAF(i, tempPoint, maxi);

            for (int j = 0; j < maxi; j++) {
                pointCloud.readFromBuffer(tempPoint);

                if(tempPoint.y < minY) {
                    minYindex = counter;
                    minY = tempPoint.y;
                }

                counter++;
                aR.p_update.threadProgress[coreNumber-1] = counter;

                if(counter % 10000 == 0){
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

        int startIndex = minYindex;

        double[] prevSegment = new double[]{0,0,0,0};

        tempPoint1.x = 0;
        tempPoint1.y = 0;

        HashSet<Long> doneIndexes = new HashSet<>();

        doneIndexes.add((long)startIndex);

        Path2D path = new Path2D.Double();
        Path path2 = new Path();

        path.moveTo(tempPoint3.x, tempPoint3.y);

        double angle;
        //ArrayList<double[]> border = new ArrayList<>();

        border.add(new double[]{tempPoint3.x, tempPoint3.y});

        //tin.add(new org.tinfour.common.Vertex(tempPoint1.x, tempPoint1.y, 0));
        tin.add(new org.tinfour.common.Vertex(tempPoint3.x, tempPoint3.y, 0));

        int size = 1;

        double increase = 0.0;


        while (this.pointCloud.queriedIndexes2.size() <= 1) {

            this.pointCloud.query2(tempPoint3.x - concavity + increase, tempPoint3.x + concavity + increase, tempPoint3.y - concavity + increase, tempPoint3.y + concavity + increase);
            increase += 5.0;


        }
        boolean running = true;

        int counti = 0;
        double dist = -1;

        int prevIndex = startIndex;

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


                        pointCloud.readRecord_noRAF(pienin, tempPoint, ero);

                        int count1 = 0;

                        for (long p = pienin; p <suurin; p++) {

                            //System.out.println("READ");
                            pointCloud.readFromBuffer(tempPoint2);

                            dist = euclideanDistance(tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y);

                            if (p != prevIndex && dist < concavity) { //  && !path.contains(tempPoint2.x, tempPoint2.y)

                                angle = angleBetween(tempPoint1.x, tempPoint1.y, tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y);

                                if (angle < 0.0)
                                    angle = 360 + angle;

                                if (angle < smallestAngle && angle != 0.0) {

                                    if (!intersection(tempPoint2.x, tempPoint2.y, tempPoint3.x, tempPoint3.y) || (prevIndex != startIndex && p == startIndex)) {
                                        minidisti = dist;
                                        angleIndex = p;
                                        smallestAngle = angle;

                                        aR.p_update.threadInt[coreNumber-1]++;

                                        if(aR.p_update.threadInt[coreNumber-1] % 100 == 0)
                                            aR.p_update.updateProgressNBorder();
                                    }
                                }
                            }
                            count1++;
                        }
                    }
            }

            /* prevIndex always points to the previous vertex in the boundary. This guarantees that
              we don't trace back our steps in the bounday.
             */
            prevIndex = (int)angleIndex;

            prevSegment[0] = tempPoint1.x;
            prevSegment[1] = tempPoint1.y;
            prevSegment[2] = tempPoint3.x;
            prevSegment[3] = tempPoint3.y;

            segments.add(prevSegment.clone());

            /* tempPoint now holds the next vertice of the boundary */
            pointCloud.readRecord((int)angleIndex, tempPoint);

            /* Assign temporary points as planned */
            tempPoint1.x = tempPoint3.x;
            tempPoint1.y = tempPoint3.y;
            tempPoint3.x = tempPoint.x;
            tempPoint3.y = tempPoint.y;

            /* Add the vertice to the boundary */
            border.add(new double[]{tempPoint.x, tempPoint.y});

            increase = 0.0;

            /* We search for points in the proximity of the new boundary vertex.
             */
            while (pointCloud.queriedIndexes2.size() <= 1) {
                pointCloud.query2(tempPoint.x - concavity + increase, tempPoint.x + concavity + increase, tempPoint.y - concavity + increase, tempPoint.y + concavity + increase);
                increase += 5.0;
            }

            /* Here is the termination condition! If the current vertex is the same as the starting vertex,
              we stop!
             */
            if(angleIndex == startIndex)
                running = false;

            counti++;
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

        //concave = ConcaveHull.calculateConcaveHull(concPoints, 50);
        //closest = tin.getNeighborhoodPointsCollector().collectNeighboringVertices(tempPoint.x, tempPoint.y, 0, 0);
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
