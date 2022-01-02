package tools;

import LASio.*;
import org.opencv.core.Mat;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.Vertex;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.utils.Polyside;
import utils.fileOperations;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.tinfour.utils.Polyside.isPointInPolygon;

/**
 *	Relates all the input point clouds onto
 *	a common ground level. Implementation is based
 *	on algorithm proposed by Ali-Sisto et. al. 2018.
 *	This is a modified version that, instead of using
 *	raster interpolation, uses two Delaunay TIN networks
 *	created from both reference (ref) and point cloud (orig).
 *
 *	The correction is calculated with:
 *
 *		Z_corrected = Z_orig - (TIN_orig(x,y) - TIN_ref(x,y))
 *
 *  	,where x and y are the coordinates of a point in the original
 *		point cloud.
 */

public class LasRelate{


    fileOperations fo = new fileOperations();
    ArrayList<LASReader> pointCloudsToRelate = new ArrayList<LASReader>();

    ArrayList<LASReader> reference = new ArrayList<LASReader>();


    ArrayList<Path2D> pointCloudPolys = new ArrayList<Path2D>();
    ArrayList<Path2D> referencePolys = new ArrayList<Path2D>();

    ArrayList<org.tinfour.standard.IncrementalTin> referenceTinBank = new ArrayList<org.tinfour.standard.IncrementalTin>();
    ArrayList<org.tinfour.standard.IncrementalTin> pointCloudTinBank = new ArrayList<org.tinfour.standard.IncrementalTin>();

    ArrayList<int[]> pointCloudGroundIndexes = new ArrayList<int[]>();
    ArrayList<int[]> referenceCloudGroundIndexes = new ArrayList<int[]>();

    org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();
    org.tinfour.interpolation.TriangularFacetInterpolator polatorPointCloud;
    org.tinfour.interpolation.TriangularFacetInterpolator polatorReference;

    PointInclusionRule rule = new PointInclusionRule();

    LASraf raOutput;
    LASraf debug;

    HashSet<Integer> overlapping = new HashSet<Integer>();

    int axGrid = 15;

    float resolution = 2.0f;

    double meanCorrection = 0.0;

    public LasRelate(){

    }

    public LasRelate(ArrayList<LASReader> in, ArrayList<LASReader> referenceIn, int axGridi) throws IOException {

        //System.loadLibrary("opencv_java320");

        this.pointCloudsToRelate = in;
        this.reference = referenceIn;

        this.axGrid = axGridi;

        makePolys();
        findGround();

        translate();
    }

    public void makePolys(){


        for(int i = 0; i < pointCloudsToRelate.size(); i++){

            double minX = pointCloudsToRelate.get(i).getMinX();
            double maxX = pointCloudsToRelate.get(i).getMaxX();
            double minY = pointCloudsToRelate.get(i).getMinY();
            double maxY = pointCloudsToRelate.get(i).getMaxY();

            double[] topLeft = new double[]{minX, maxY};
            double[] topRight = new double[]{maxX, maxY};
            double[] bottomLeft = new double[]{minX, minY};
            double[] bottomRight = new double[]{maxX, minY};

            Path2D polygTemp = new Path2D.Double();

            polygTemp.moveTo(topLeft[0], topLeft[1]);
            polygTemp.lineTo(topRight[0], topRight[1]);
            polygTemp.lineTo(bottomLeft[0], bottomLeft[1]);
            polygTemp.lineTo(bottomRight[0], bottomRight[1]);

            polygTemp.closePath();

            pointCloudPolys.add(polygTemp);
        }

        for(int i = 0; i < reference.size(); i++){

            double minX = reference.get(i).getMinX();
            double maxX = reference.get(i).getMaxX();
            double minY = reference.get(i).getMinY();
            double maxY = reference.get(i).getMaxY();

            double[] topLeft = new double[]{minX, maxY};
            double[] topRight = new double[]{maxX, maxY};
            double[] bottomLeft = new double[]{minX, minY};
            double[] bottomRight = new double[]{maxX, minY};

            Path2D polygTemp = new Path2D.Double();

            polygTemp.moveTo(topLeft[0], topLeft[1]);
            polygTemp.lineTo(topRight[0], topRight[1]);
            polygTemp.lineTo(bottomLeft[0], bottomLeft[1]);
            polygTemp.lineTo(bottomRight[0], bottomRight[1]);

            polygTemp.closePath();

            referencePolys.add(polygTemp);
        }

        reduceToOverlapping();

        System.out.println("POLYS DONE! " + overlapping.size());

    }

    public void reduceToOverlapping(){

        for(int i = 0; i < pointCloudPolys.size(); i++){

            for(int j = 0; j < referencePolys.size(); j++){

                if(testIntersection(pointCloudPolys.get(i), referencePolys.get(j)))
                    overlapping.add(j);

            }

        }

    }

    public void findGround() throws IOException{

        LasPoint tempPoint = new LasPoint();
        GroundDetector det = new GroundDetector();

        det.setAx(this.axGrid);

        try{

            for(int i = 0; i < pointCloudsToRelate.size(); i++){

                //System.out.println("GOT HERE!" + " " + pointCloudsToRelate.size());
                det.setPointCloud(pointCloudsToRelate.get(i));

                LASReader tempReader = pointCloudsToRelate.get(i);

                int[] seeds = det.detectSeedPoints();

                int[] grounds = det.detect();

                int[] outti = new int[seeds.length + grounds.length];

                System.arraycopy(seeds, 0, outti, 0, seeds.length);
                System.arraycopy(grounds, 0, outti, seeds.length, grounds.length);

                pointCloudGroundIndexes.add(outti);

                grounds = null;
                seeds = null;
                //outti = null;
                //det.reset();
                //det.dispose();
                //det = null;

                System.gc();
            }

            if(overlapping.size() > 0){

                for(int i : overlapping){

                    LASReader tempReader = reference.get(i);

                    det.setPointCloud(reference.get(i));

                    int[] seeds = det.detectSeedPoints();

                    int[] grounds = det.detect();

                    int[] outti = new int[seeds.length + grounds.length];

                    System.arraycopy(seeds, 0, outti, 0, seeds.length);
                    System.arraycopy(grounds, 0, outti, seeds.length, grounds.length);

                    referenceCloudGroundIndexes.add(outti);

                    grounds = null;
                    seeds = null;
                    //outti = null;
                    //det.dispose();
                    //det = null;
                    //det.reset();

                    System.gc();

                }
            }
            else
                System.out.println("Datasets do not overlap!");
        }catch(IOException e) {
            e.printStackTrace();
        }

    }

    public static boolean testIntersection(Shape shapeA, Shape shapeB) {

        Area areaA = new Area(shapeA);
        areaA.intersect(new Area(shapeB));
        return !areaA.isEmpty();

    }

    public void translate(){

        LasPoint tempPoint = new LasPoint();

        try{

            for(int i = 0; i < pointCloudsToRelate.size(); i++){

                LASReader tempReader = pointCloudsToRelate.get(i);

                String outNameG = fo.createNewFileWithNewExtension(tempReader.getFile(), "_ground.las").getAbsolutePath();
                //tempReader.getFile().getAbsolutePath().replaceFirst("[.][^.]+$", "") + "_ground.las";

                File tempG = new File(outNameG);

                if(tempG.exists())
                    tempG.delete();

                tempG.createNewFile();

                debug = new LASraf(tempG);

                LASwrite.writeHeader(debug, "debug", tempReader.versionMajor, tempReader.versionMinor,
                        tempReader.pointDataRecordFormat, tempReader.pointDataRecordLength,
                        tempReader.headerSize, tempReader.offsetToPointData, tempReader.numberVariableLengthRecords,
                        tempReader.fileSourceID, tempReader.globalEncoding,
                        tempReader.xScaleFactor, tempReader.yScaleFactor, tempReader.zScaleFactor,
                        tempReader.xOffset, tempReader.yOffset, tempReader.zOffset);

                int count = 0;

                int pointCount = 0;

                double minX = tempReader.getMinX();
                double maxX = tempReader.getMaxX();
                double minY = tempReader.getMinY();
                double maxY = tempReader.getMaxY();

                org.tinfour.standard.IncrementalTin tempTin = new org.tinfour.standard.IncrementalTin();

                int[] indexes = pointCloudGroundIndexes.get(i);

                for(int j = 0; j < indexes.length; j++){

                    tempReader.readRecord(indexes[j], tempPoint);
                    tempTin.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                    debug.writePoint( tempPoint, rule, tempReader.xScaleFactor, tempReader.yScaleFactor, tempReader.zScaleFactor,
                            tempReader.xOffset, tempReader.yOffset, tempReader.zOffset, tempReader.pointDataRecordFormat, j);
                    pointCount++;

                }

                Path2D thisPoly = pointCloudPolys.get(i);

                //ArrayList<Integer> overlapping = new ArrayList<Integer>();

                org.tinfour.standard.IncrementalTin tempTinReference = new org.tinfour.standard.IncrementalTin();






                for(int j : overlapping){

                    //if(testIntersection(thisPoly, referencePolys.get(j))){

                    int[] indexes2 = referenceCloudGroundIndexes.get(count);

                    LASReader tempReader2 = reference.get(j);

                    for(int l = 0; l < indexes2.length; l++){

                        //System.out.println("!!!!!!");
                        tempReader2.readRecord(indexes2[l], tempPoint);
                        tempTinReference.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));

                    }

                    count++;
                    //}

                }

                debug.writeBuffer2();
                debug.updateHeader2();




                String outName = fo.createNewFileWithNewExtension(tempReader.getFile(), "_related.las").getAbsolutePath();
                // tempReader.getFile().getAbsolutePath().replaceFirst("[.][^.]+$", "") + "_related.las";
                File temp = new File(outName);


                if(temp.exists())
                    temp.delete();

                temp.createNewFile();


                raOutput = new LASraf(temp);



                LASwrite.writeHeader(raOutput, "lasrelate", tempReader.versionMajor, tempReader.versionMinor,
                        tempReader.pointDataRecordFormat, tempReader.pointDataRecordLength,
                        tempReader.headerSize, tempReader.offsetToPointData, tempReader.offsetToPointData,
                        tempReader.fileSourceID, tempReader.globalEncoding,
                        tempReader.xScaleFactor, tempReader.yScaleFactor, tempReader.zScaleFactor,
                        tempReader.xOffset, tempReader.yOffset, tempReader.zOffset);


                /* Here we should refine both TIN networks
                 Ideas: Remove isolated points which have a large
                 angle.
                 */


                double[] extent = getExtent(tempTin);
                double[] extentReference = getExtent(tempTinReference);

                System.out.println("Unify " + tempTin.getVertices().size() + " " + tempTinReference.getVertices().size());

                unify(tempTin, tempTinReference);

                GroundDetector.removeSpikes(tempTin, 0.5);
                GroundDetector.removeSpikes(tempTinReference, 0.5);

                System.out.println("Unify complete! " + tempTin.getVertices().size() + " " + tempTinReference.getVertices().size());




                int sizeX = 0;
                int sizeY = 0;

                sizeX = (int)Math.ceil((maxX - minX) / resolution);
                sizeY = (int)Math.ceil((maxY - minY) / resolution);




                //Mat correctionRaster = new Mat(sizeY, sizeX, CvType.CV_64FC1);

                //fillCorrectionRaster(correctionRaster, tempTin, tempTinReference, extent);

                org.tinfour.standard.IncrementalTin correctionTin = makeCorrectionTIN(tempTin, tempTinReference);
                GroundDetector.removeSpikes(correctionTin, 0.5);

                /* Let's make a correction raster from correctionTin and interpolate it */

                float[][] correctionRaster = new float[sizeX][sizeY];

                double locationX = 0.0;
                double locationY = 0.0;

                polatorPointCloud = new org.tinfour.interpolation.TriangularFacetInterpolator(tempTin);
                polatorReference = new org.tinfour.interpolation.TriangularFacetInterpolator(correctionTin);

                List<IQuadEdge> correctionTin_perimeter = correctionTin.getPerimeter();

                polatorPointCloud.resetForChangeToTin();
                polatorReference.resetForChangeToTin();
                double interpolatedvalue = 0.0;

                for(int l = 0; l < sizeX; l++){
                    for(int p = 0; p < sizeY; p++){

                        locationX = minX + resolution * l + resolution / 2.0f;
                        locationY = maxY - resolution * p - resolution / 2.0f;


                        //if(correctionTin.isPointInsideTin(locationX, locationY)){
                        if(isPointInPolygon(correctionTin_perimeter, locationX, locationY) == Polyside.Result.Inside){
                        //if(!Double.isNaN(interpolatedvalue)){
                            interpolatedvalue = polatorReference.interpolate(locationX, locationY, valuator);

                            float Z = (float)interpolatedvalue;
                            correctionRaster[l][p] = Z;

                        }
                        else{

                            correctionRaster[l][p] = Float.NaN;

                        }

                    }
                }

                correctionRaster = createCHM.removeOutliers(correctionRaster, 2, 3, 1.0);



                gdalE out = new gdalE();

                //out.hei(outName, output);
                gdalE.hei("outName.tif", correctionRaster);

                long n = tempReader.getNumberOfPointRecords();

                pointCount = 0;

                int rasterX = 0;
                int rasterY = 0;

                for(int j = 0; j < n; j++){

                    tempReader.readRecord(j, tempPoint);

                    /* Threw away a condition */

                    if(true){ // correctionTin.isPointInsideTin(tempPoint.x, tempPoint.y)

                        //	 double tinPointCloudZ = polatorPointCloud.interpolate(tempPoint.x, tempPoint.y, valuator);

                        //temppi[0] = (long)Math.floor((tempPoint.x - minX) / (double)axelssonGridSize);   //X INDEX
                        //temppi[1] = (long)Math.floor((maxY - tempPoint.y) / (double)axelssonGridSize);

                        rasterX = (int)Math.floor((tempPoint.x - minX) / (double)resolution);
                        rasterY = (int)Math.floor((maxY - tempPoint.y) / (double)resolution);

                        //if(rasterX > 0 && rasterX < sizeX - 2 && rasterY > 0 && rasterY < sizeY - 2){

                        //System.out.println(rasterX + " " + rasterY + " real: " + sizeX + " " + sizeY);

                        //float tinReferenceZ = polatorReference.interpolate(tempPoint.x, tempPoint.y, valuator);
                        float tinReferenceZ = correctionRaster[rasterX][rasterY];

                        //System.out.println(tinPointCloudZ);
                        //System.out.println(tinReferenceZ);

                        //System.out.println("old z: " + tempPoint.z);

                        //tempPoint.z = tempPoint.z - (tinPointCloudZ - tinReferenceZ);
                        tempPoint.z = tempPoint.z - tinReferenceZ;
                        //System.out.println("new z: " + tempPoint.z);
                        //System.out.println("--------------");

                        //tempPoint.z =
                        //System.out.println(tempPoint.x);
                        //writePoint(outputFilesMatrix[x][y], tempPoint, rule, 0.01, 0.01, 0.01, 0, 0, 0, 1, j)
                        raOutput.writePoint( tempPoint, rule, tempReader.xScaleFactor, tempReader.yScaleFactor, tempReader.zScaleFactor,
                                tempReader.xOffset, tempReader.yOffset, tempReader.zOffset, tempReader.pointDataRecordFormat, j);

                        pointCount++;
                    }
                    //else
                    //}
                    //	System.out.println("DROPPED");
                }

                raOutput.writeBuffer2();

                raOutput.updateHeader2();


            }
        }catch(IOException e){
            e.printStackTrace(System.out);
        }
    }

    public void refine(org.tinfour.standard.IncrementalTin in){

        double thresHoldAngle = 10.0;
        double thresHoldDistance = 5.0;

        java.util.List<Vertex> closest;

        java.util.List<IQuadEdge> edges = new ArrayList<org.tinfour.common.IQuadEdge>();
        edges = in.getEdges();

        org.tinfour.common.IQuadEdge tempEdge = null;

        org.tinfour.common.Vertex tempVertexA = null;
        org.tinfour.common.Vertex tempVertexB = null;

        double euc = 0.0;

        double vert = 0.0;

        double angle = 0.0;

        for(int i = 0; i < edges.size(); i++){

            tempEdge = edges.get(i);

            tempVertexA = tempEdge.getA();
            tempVertexB = tempEdge.getB();

            vert = Math.abs(tempVertexA.getZ() - tempVertexB.getZ());

            euc = euclideanDistance(tempVertexA.x, tempVertexA.y, tempVertexB.x, tempVertexB.y);

            angle = angle(vert, euc);

            if(angle > thresHoldAngle && euc > thresHoldDistance){

                closest = in.getNeighborhoodPointsCollector().collectNeighboringVertices(tempVertexA.x, tempVertexA.y, 0, 0);

                for(int j = 0; j < closest.size(); j++){

                    if(tempVertexA.getDistance(closest.get(j)) > thresHoldDistance ){}

                }

            }

        }


    }

    public double angle(double oppositeSideLength, double adjacentSideLength) {

        return Math.toDegrees(Math.atan(oppositeSideLength/ adjacentSideLength));

    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt(  (x1 - x2) * ( x1 - x2) + ( y1 - y2) * ( y1 - y2) );

    }

    public void fillCorrectionRaster(Mat raster, org.tinfour.standard.IncrementalTin in1, org.tinfour.standard.IncrementalTin in2, double[] extent){

        org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

        org.tinfour.interpolation.TriangularFacetInterpolator polator = new org.tinfour.interpolation.TriangularFacetInterpolator(in2);

        java.util.List<Vertex> vL = new ArrayList<org.tinfour.common.Vertex>();
        vL = in1.getVertices();
        org.tinfour.common.Vertex tempVertexA = null;

        double correctionValue = 0.0;

        int x = 0;
        int y = 0;

        for(int i = 0; i < vL.size(); i++){

            tempVertexA = vL.get(i);

            correctionValue = polator.interpolate(tempVertexA.x, tempVertexA.y, valuator);

            x = (int)((tempVertexA.x - extent[0]) / resolution);
            y = (int)((extent[3] - tempVertexA.y) / resolution);

            raster.put(y,x,correctionValue);

        }

    }

    public org.tinfour.standard.IncrementalTin makeCorrectionTIN(org.tinfour.standard.IncrementalTin in1, org.tinfour.standard.IncrementalTin in2){

        org.tinfour.standard.IncrementalTin output = new org.tinfour.standard.IncrementalTin();

        java.util.List<Vertex> vL = new ArrayList<org.tinfour.common.Vertex>();

        org.tinfour.common.Vertex tempVertex = null;

        vL = in1.getVertices();

        org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

        org.tinfour.interpolation.TriangularFacetInterpolator polator = new org.tinfour.interpolation.TriangularFacetInterpolator(in2);

        double correctionValue = 0.0;
        double interpolatedvalue = 0;

        List<IQuadEdge> in2_perimeter = in2.getPerimeter();


        for(int i = 0; i < vL.size(); i++){

            tempVertex = vL.get(i);

            //if(in2.isPointInsideTin(tempVertex.x, tempVertex.y)){
            if(isPointInPolygon(in2_perimeter, tempVertex.x, tempVertex.y) == Polyside.Result.Inside){
            //if(!Double.isNaN(interpolatedvalue)){
                interpolatedvalue = polator.interpolate(tempVertex.x, tempVertex.y, valuator);

                correctionValue = interpolatedvalue;

                correctionValue = tempVertex.getZ() - correctionValue;

                //System.out.println("Correction: " + correctionValue);

                output.add(new org.tinfour.common.Vertex(tempVertex.x, tempVertex.y, correctionValue));

            }

        }

        return output;

    }

    /**
     * Keeps only the vertices from two TINs which are
     * spatially close to one another (less than @param dist)
     *
     * @param in1	Input TIN
     * @param in2 	Input TIN
     *
     *
     */

    public void unify(org.tinfour.standard.IncrementalTin in1, org.tinfour.standard.IncrementalTin in2){

        //List<org.tinfour.common.Vertex> vL;
        TriangularFacetInterpolator temp = new TriangularFacetInterpolator(in2);
        double distanceThreshold = 0.2;

        java.util.List<Vertex> closest;

        org.tinfour.common.Vertex tempVertexA = null;
        org.tinfour.common.Vertex tempVertexB = null;

        java.util.List<Vertex> vL = new ArrayList<org.tinfour.common.Vertex>();
        java.util.List<Vertex> vL2 = new ArrayList<org.tinfour.common.Vertex>();

        vL = in1.getVertices();

        double distance = 0.0;

        boolean keepA = false;

        List<IQuadEdge> in2_perimeter = in2.getPerimeter();

        //boolean keepB = false;
        double interpolatedvalue = 0.0;
        for(int i = 0; i < vL.size(); i++){

            /* Always false in the beginning */
            keepA = false;

            tempVertexA = vL.get(i);


            //if(in2.isPointInsideTin(tempVertexA.x, tempVertexA.y)){
            if(isPointInPolygon(in2_perimeter, tempVertexA.x, tempVertexA.y) == Polyside.Result.Inside){
            //if(!Double.isNaN(interpolatedvalue)){
                interpolatedvalue = temp.interpolate(tempVertexA.x, tempVertexA.y, valuator);
                closest = in2.getNeighborhoodPointsCollector().collectNeighboringVertices(tempVertexA.x, tempVertexA.y, 0, 0);

                for(int j = 0; j < closest.size(); j++){

                    tempVertexB = closest.get(j);
                    distance = euclideanDistance(tempVertexA.x, tempVertexA.y, tempVertexB.x, tempVertexB.y);

                    if(distance < distanceThreshold && keepA == false){

                        /* We keep the vertex because it has a close neighbour
                         in the other TIN */
                        keepA = true;


                    }

                    else if(keepA == true){

                        //in2.remove(tempVertexB);

                    }

                }

                if(keepA == false){

                    in1.remove(tempVertexA);

                }
            }

        }

        vL2 = in2.getVertices();

        System.out.println(vL2.size());

    }

    public double angleHypo(double hypotenuse, double adjacentSideLength) {

        return Math.toDegrees(Math.acos(adjacentSideLength / hypotenuse));

    }


    public double[] getExtent(org.tinfour.standard.IncrementalTin in1){

        double[] output = new double[4];

        /* Min X */
        output[0] = Double.POSITIVE_INFINITY;
        /* Max X */
        output[1] = Double.NEGATIVE_INFINITY;
        /* Min Y */
        output[2] = Double.POSITIVE_INFINITY;
        /* Max Y */
        output[3] = Double.NEGATIVE_INFINITY;

        List<Vertex> vL = new ArrayList<org.tinfour.common.Vertex>();

        vL = in1.getVertices();

        for(int i = 0; i < vL.size(); i++){

            if(vL.get(i).x < output[0])
                output[0] = vL.get(i).x;

            if(vL.get(i).x > output[1])
                output[1] = vL.get(i).x;

            if(vL.get(i).y < output[2])
                output[2] = vL.get(i).y;

            if(vL.get(i).y > output[3])
                output[3] = vL.get(i).y;


        }

        return output;

    }

}