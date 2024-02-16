package utils;

import org.agrona.concurrent.SystemEpochClock;
import org.apache.commons.math3.util.FastMath;
import org.gdal.ogr.*;
import quickhull3d.Vector3d;
import tools.ConcaveHull;

import java.awt.*;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.util.*;
import java.util.List;

import static utils.solar3dManipulator.*;

public class zonalExtractor {


    zonalExtractorCounter zonalCounter = new zonalExtractorCounter(this);

    static final int precision = 1000; // gradations per degree, adjust to suit

    HashMap<Integer, zonalCell> cells = new HashMap<>();
    HashMap<Integer, zonalCell> cellsHole = new HashMap<>();

    List<double[]> pixels = new ArrayList<>();

    double resolution = 0.0;
    List<ConcaveHull.Point> polygon = new ArrayList<>();
    List<ConcaveHull.Point> polygon2 = new ArrayList<>();
    ArrayList<float[]> normalizedPolygon = new ArrayList<>();
    ArrayList<float[]> normalizedPolygon2 = new ArrayList<>();
    double originX = 0.0;
    double originY = 0.0;
    int xdim, ydim;

    boolean[][] cellMapBorder;
    boolean[][] cellMapBorderHole;
    boolean[][] cellMapInside;
    boolean[][] cellMapInsideHole;
    HashMap<Integer, int[]> cellMap = new HashMap<>();
    HashMap<Integer, int[]> cellMapHole = new HashMap<>();

    Geometry standGeometry = null;
    boolean mode_hole = false;

    public zonalExtractor(){

    }

    public void setResolution(double resolution){
        this.resolution = resolution;
    }

    public void setOriginX(double originX){
        this.originX = originX;
    }

    public void setOriginY(double originY){
        this.originY = originY;
    }

    public void normalizePolygon(){

        this.normalizedPolygon.clear();
        for(ConcaveHull.Point p : polygon){

            float[] normalizedPoint = new float[2];

            normalizedPoint[0] = (float) ((p.getX() - originX) / this.resolution);
            //normalizedPoint[1] = (float) ((originY - p.getY()) / this.resolution);
            normalizedPoint[1] = (float) ((p.getY() - (originY - (ydim * resolution))) / this.resolution);

            normalizedPolygon.add(normalizedPoint);
            //System.out.println(Arrays.toString(normalizedPoint));
        }

        cellMapBorder = new boolean[xdim][ydim];
        cellMapInside = new boolean[xdim][ydim];

    }

    public void normalizePolygonHole(){

        if(!mode_hole) {
            mode_hole = true;

        }

        cellMapBorderHole = new boolean[xdim][ydim];
        cellMapInsideHole = new boolean[xdim][ydim];
        this.cellsHole.clear();
        this.cellMapHole.clear();

        this.normalizedPolygon.clear();

        for(ConcaveHull.Point p : polygon){

            float[] normalizedPoint = new float[2];

            normalizedPoint[0] = (float) ((p.getX() - originX) / this.resolution);
            //normalizedPoint[1] = (float) ((originY - p.getY()) / this.resolution);
            normalizedPoint[1] = (float) ((p.getY() - (originY - (ydim * resolution))) / this.resolution);

            normalizedPolygon.add(normalizedPoint);
            //System.out.println(Arrays.toString(normalizedPoint));
        }

    }

    public void normalizePolygon2(){


        cellMapBorderHole = new boolean[xdim][ydim];
        cellMapInsideHole = new boolean[xdim][ydim];
        //this.cellsHole.clear();
        //this.cellMapHole.clear();

        this.normalizedPolygon2.clear();

        for(ConcaveHull.Point p : polygon2){

            float[] normalizedPoint = new float[2];

            normalizedPoint[0] = (float) ((p.getX() - originX) / this.resolution);
            //normalizedPoint[1] = (float) ((originY - p.getY()) / this.resolution);
            normalizedPoint[1] = (float) ((p.getY() - (originY - (ydim * resolution))) / this.resolution);

            normalizedPolygon2.add(normalizedPoint);
            //System.out.println(Arrays.toString(normalizedPoint));
        }

    }

    public void normalizeStandGeometry(Geometry standGeometry){

        int numberOfPolygons = this.standGeometry.GetGeometryCount();

        for(int i = 0; i < numberOfPolygons; i++){

            Geometry polygon = this.standGeometry.GetGeometryRef(i);

            int numberOfSubPolygons = polygon.GetGeometryCount();

            for(int j = 0; j < numberOfSubPolygons; j++){

                Geometry polygon2 = polygon.GetGeometryRef(j);

                int numberOfPoints = polygon2.GetPointCount();

                for(int k = 0; k < numberOfPoints; k++){

                    double[] point = polygon2.GetPoint(k);

                    float[] normalizedPoint = new float[2];

                    normalizedPoint[0] = (float) ((point[0] - originX) / this.resolution);
                    //normalizedPoint[1] = (float) ((originY - point[1]) / this.resolution);
                    normalizedPoint[1] = (float) ((point[1] - (originY - (ydim * resolution))) / this.resolution);

                    polygon2.SetPoint(k, normalizedPoint[0], normalizedPoint[1]);

                }

            }

        }


    }

    public void normalizeStandGeometry(Geometry standGeometry, int id){

        int numberOfPolygons = this.standGeometry.GetGeometryCount();

        if(id == 549) {
            System.out.println(numberOfPolygons);
            //System.exit(1);
        }

        for(int i = 0; i < numberOfPolygons; i++){

            Geometry polygon = this.standGeometry.GetGeometryRef(i);

            int numberOfSubPolygons = polygon.GetGeometryCount();

            if(id == 549) {
                System.out.println(numberOfSubPolygons);
                //System.exit(1);
            }

            if(numberOfSubPolygons != 0) {
                for (int j = 0; j < numberOfSubPolygons; j++) {

                    Geometry polygon2 = polygon.GetGeometryRef(j);

                    int numberOfPoints = polygon2.GetPointCount();

                    for (int k = 0; k < numberOfPoints; k++) {

                        double[] point = polygon2.GetPoint(k);

                        float[] normalizedPoint = new float[2];

                        normalizedPoint[0] = (float) ((point[0] - originX) / this.resolution);
                        //normalizedPoint[1] = (float) ((originY - point[1]) / this.resolution);
                        normalizedPoint[1] = (float) ((point[1] - (originY - (ydim * resolution))) / this.resolution);

                        polygon2.SetPoint(k, normalizedPoint[0], normalizedPoint[1]);

                    }

                }
            }else{

                    int numberOfPoints = polygon.GetPointCount();

                    for (int k = 0; k < numberOfPoints; k++) {

                        double[] point = polygon.GetPoint(k);

                        float[] normalizedPoint = new float[2];

                        normalizedPoint[0] = (float) ((point[0] - originX) / this.resolution);
                        //normalizedPoint[1] = (float) ((originY - point[1]) / this.resolution);
                        normalizedPoint[1] = (float) ((point[1] - (originY - (ydim * resolution))) / this.resolution);

                        polygon.SetPoint(k, normalizedPoint[0], normalizedPoint[1]);

                    }
            }

        }


    }
/*
    public double[] unNormalizePoint(float[] point){

        double[] unNormalizedPoint = new double[2];

        unNormalizedPoint[0] = (point[0] * this.resolution) + originX;
        unNormalizedPoint[1] = originY - (point[1] * this.resolution);

        return unNormalizedPoint;

    }

 */
    public double[] unNormalizePoint(float[] point){

        double[] unNormalizedPoint = new double[2];

        unNormalizedPoint[0] = (point[0] * this.resolution) + originX;
        unNormalizedPoint[1] = (point[1] * this.resolution) + (originY - (ydim * resolution));

        return unNormalizedPoint;

    }

    public class lineSegment{
        ArrayList<float[]> points = new ArrayList<>();
        ArrayList<Integer> cellIds = new ArrayList<>();
        public lineSegment(){

        }

        public void addPoint(float[] point){
            points.add(point);
        }

        public void addCellId(int cellId){
            cellIds.add(cellId);
        }

    }

    public void traversePolygon(){

        int startCellId = 0;
        float[] p1_ = normalizedPolygon.get(0);

        int x = (int)((p1_[0]) / this.resolution);
        int y = (int)((p1_[1]) / this.resolution);

        int id = x + y * this.xdim;
        startCellId = id;

        //System.out.println(x + " " + y + " " + id);
        //System.out.println(Arrays.toString(p1_));

        zonalCounter.setStartCellId(startCellId);
        //zonalCell tmpCell = new zonalCell(x, y);
        //tmpCell.addLineSegment();
        //tmpCell.addPoint(p1_);

        //cells.put(startCellId, tmpCell);

        //System.out.println("START ID: " + startCellId);

        //System.out.println( polygon.get(0).getX() + " " + polygon.get(0).getY());
        //System.out.println( normalizedPolygon.get(0)[0] + " " + normalizedPolygon.get(0)[1]);
        //System.exit(1);

        ArrayList<lineSegment> lineSegments = new ArrayList<>();

        Geometry poly = createPolygon(normalizedPolygon);

        if(!poly.IsValid())
            return;

        double polyArea = poly.GetArea();

        //System.out.println(polyArea);

        HashMap<Integer, zonalCell> borderCells = new HashMap<>();

        for(int i = 0; i < normalizedPolygon.size()-1; i++){

            float[] p1 = normalizedPolygon.get(i);
            float[] p2 = normalizedPolygon.get((i + 1));

            List<double[]> intersectedPoints = new ArrayList<>();
            List<int[]> gridCellCoordinates = new ArrayList<>();

            //getIntersectedPoints(p1[0], p1[1], p2[0], p2[1], (int)this.resolution, intersectedPoints, gridCellCoordinates);
            //gridCellCoordinates = getOverlappingCells(p1[0], p1[1], p2[0], p2[1], (int)this.resolution);

            int flag = rayTrace(p1[0], p1[1], 0,  p2[0], p2[1], 10);

            //if(flag == 2){

                zonalCell tmpCell = new zonalCell((int)p1[0], (int)p1[1]);
                cellMapBorder[(int)p1[0]][((int)p1[1])] = true;
                int idHere = xy_to_id2((int)p1[0], (int)p1[1]);
                borderCells.put(idHere, tmpCell);
                this.cellMap.put(idHere, new int[]{(int)p1[0], (int)p1[1]});
                tmpCell.border = true;

           // }
            if(false)
            for(int i_ = 0; i_ < gridCellCoordinates.size(); i_++){
                System.out.println(Arrays.toString(gridCellCoordinates.get(i_)));
                int _id_ = gridCellCoordinates.get(i_)[0] + (gridCellCoordinates.get(i_)[1]) * this.xdim;

                if(!borderCells.containsKey(_id_)){
                    zonalCell tmpCell_ = new zonalCell(gridCellCoordinates.get(i_)[0], (gridCellCoordinates.get(i_)[1]));

                    cellMapBorder[gridCellCoordinates.get(i_)[0]][(gridCellCoordinates.get(i_)[1])] = true;

                    borderCells.put(_id_, tmpCell_);

                }

            }

        }

        for(int i : cellMap.keySet()){

            zonalCell tmpCell = new zonalCell(cellMap.get(i)[0], cellMap.get(i)[1]);

            borderCells.put(i, tmpCell);
            tmpCell.border = true;
            //if(cellMap.get(i).length > 2)
            //    System.out.println("direction: " + cellMap.get(i)[2]   );

            //System.out.println("cell " + cellMap.get(i)[0] + ", " + (cellMap.get(i)[1]) );
            cellMapBorder[cellMap.get(i)[0]][cellMap.get(i)[1]] = true;
        }



        //System.out.println("border cells: " + borderCells.size());

        double intersectedArea = 0;

        for(int i : borderCells.keySet()){

            this.cells.put(borderCells.get(i).x + borderCells.get(i).y * this.xdim, borderCells.get(i));

            //System.out.println("border cell " + borderCells.get(i).x + ", " + (borderCells.get(i).y));

            // cell extent
            ArrayList<float[]> cellExtent = new ArrayList<>();
            cellExtent.add(new float[]{(float)(borderCells.get(i).x), (float)((borderCells.get(i).y + 1))});
            cellExtent.add(new float[]{(float)((borderCells.get(i).x + 1)), (float)((borderCells.get(i).y + 1))});
            cellExtent.add(new float[]{(float)((borderCells.get(i).x + 1)), (float)(((borderCells.get(i).y)))});
            cellExtent.add(new float[]{(float)(borderCells.get(i).x ), (float)(((borderCells.get(i).y)))});
            cellExtent.add(new float[]{(float)(borderCells.get(i).x), (float)((borderCells.get(i).y + 1))});

            borderCells.get(i).setCellExtent(cellExtent);

            Geometry cellGeom = createPolygon(cellExtent);

            double area = cellGeom.GetArea();

            //System.out.println("area: " + cellGeom.GetArea());
            //System.out.println("polygon area: " + poly.GetArea());
           // System.out.println(poly.IsValid());
            double intersect = poly.Intersection(cellGeom).GetArea();

            if(this.standGeometry != null)
                intersect = this.standGeometry.Intersection(cellGeom).GetArea();

            intersectedArea += intersect;
            borderCells.get(i).intersectArea = intersect;
            cellMapBorder[borderCells.get(i).x][borderCells.get(i).y] = true;
            //System.out.println("intersection: " + intersect);
            //System.out.println("xdim: " + this.xdim + ", ydim: " + this.ydim);
            //print cellextent
            //for(float[] p : cellExtent){
            //    System.out.println(Arrays.toString(p));
            //}

//
            //System.out.println("---------------------------");

        }


        //System.exit(1);

        this.flood(polyArea, normalizedPolygon, intersectedArea, borderCells);

        System.out.println(xdim * ydim);
        //System.exit(1);
        if(true)
            return;



        File debugOutput = new File("/home/koomikko/Documents/customer_work/metsahallitus/HPR_data_final/debug/debug_output.txt");

        if(debugOutput.exists()){
            debugOutput.delete();
        }

        BufferedWriter writer = null;

        try{
            debugOutput.createNewFile();
            writer = new BufferedWriter(new java.io.FileWriter(debugOutput));

            writer.write("x\ty\tid\n");
        }catch(Exception e){
            e.printStackTrace();
        }

        int counter = 0;

        for(int i : cells.keySet()){

            System.out.println("Cell " + i + " has " + cells.get(i).lineSegments.size() + " line segments");

            zonalCell cell = cells.get(i);

            for(ArrayList<float[]> lineSegment : cell.lineSegments){
                System.out.println("line segment has " + lineSegment.size() + " points");

                for(float[] point : lineSegment){
                    System.out.println(Arrays.toString(point));
                    try {
                        //writer.write((point[0] + originX) + "\t" + (originY- point[1]) + "\t" + counter++ + "\n");
                        writer.write((point[0] + originX) + "\t" + (originY- point[1]) + "\t" + i + "\n");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                double cellMinX = cell.x * this.resolution;
                double cellMaxX = (cell.x) * this.resolution + resolution;

                double cellMinY = cell.y * this.resolution - resolution;
                double cellMaxY = (cell.y) * this.resolution;
                System.out.println("cellMinX: " + cellMinX + " cellMaxX: " + cellMaxX + " cellMinY: " + cellMinY + " cellMaxY: " + cellMaxY);
                System.out.println("cell.x: " + cell.x + " cell.y: " + cell.y);
                System.out.println("area: " + calculateAreaToRightOfPolygon(cellMinX, cellMaxX, cellMinY, cellMaxY, lineSegment));
            }
            System.out.println("---------------------");
        }

        try {
            writer.flush();

            writer.close();
        }catch (Exception e){
        }
        System.exit(1);

    }

    public void traverseHole(){

        int startCellId = 0;

        if(normalizedPolygon.size() < 3){
            return;
        }
        float[] p1_ = normalizedPolygon.get(0);

        int x = (int)((p1_[0]) / this.resolution);
        int y = (int)((p1_[1]) / this.resolution);

        int id = x + y * this.xdim;
        startCellId = id;

        zonalCounter.setStartCellId(startCellId);

        ArrayList<lineSegment> lineSegments = new ArrayList<>();

        for(int i = 0; i < normalizedPolygon.size(); i++){
            System.out.println(Arrays.toString(normalizedPolygon.get(i)));
        }

        System.out.println("creating polygon");
        Geometry poly = createPolygon(normalizedPolygon);
        Geometry poly2 = createPolygon(normalizedPolygon2);
        System.out.println("polygon created");
        double polyArea = poly.GetArea();

        System.out.println(polyArea);

        HashMap<Integer, zonalCell> borderCells = new HashMap<>();

        for(int i = 0; i < normalizedPolygon.size()-1; i++){

            float[] p1 = normalizedPolygon.get(i);
            float[] p2 = normalizedPolygon.get((i + 1));

            int flag = rayTrace(p1[0], p1[1], 0,  p2[0], p2[1], 10);

            zonalCell tmpCell = new zonalCell((int)p1[0], (int)p1[1]);
            cellMapBorderHole[(int)p1[0]][((int)p1[1])] = true;
            tmpCell.border = true;
            int idHere = xy_to_id2((int)p1[0], (int)p1[1]);
            borderCells.put(idHere, tmpCell);
            this.cellMapHole.put(idHere, new int[]{(int)p1[0], (int)p1[1]});


        }

        for(int i : cellMapHole.keySet()){

            zonalCell tmpCell = new zonalCell(cellMapHole.get(i)[0], cellMapHole.get(i)[1]);
            //tmpCell.intersectArea = 0.0;

            borderCells.put(i, tmpCell);
            tmpCell.border = true;
            this.cellsHole.put(i, tmpCell);
            cellMapBorderHole[cellMapHole.get(i)[0]][cellMapHole.get(i)[1]] = true;

        }

        System.out.println("border cells HOLE!!: " + borderCells.size() + " " + cellMapHole.size());


        System.out.println(poly.GetArea());

        double[][] points = poly.GetGeometryRef(0).GetPoints();

        // print points
        //for(int i = 0; i < points.length; i++){
        //    System.out.println(Arrays.toString(points[i]));
        //}
        //System.exit(1);
        //System.out.println(poly.IsValid());

        double intersectedArea = 0;



        for(int i : borderCells.keySet()){

            //System.out.println(this.cells.containsKey(borderCells.get(i).x + borderCells.get(i).y * this.xdim));
            //System.out.println(this.cells.containsKey(i));

            //borderCells.get(i).intersectArea = 0;


            //System.out.println((borderCells.get(i).x + borderCells.get(i).y * this.xdim) + " " + i);
            //this.cells.put(borderCells.get(i).x + borderCells.get(i).y * this.xdim, borderCells.get(i));

            ArrayList<float[]> cellExtent = new ArrayList<>();
            cellExtent.add(new float[]{(float)(borderCells.get(i).x), (float)((borderCells.get(i).y + 1))});
            cellExtent.add(new float[]{(float)((borderCells.get(i).x + 1)), (float)((borderCells.get(i).y + 1))});
            cellExtent.add(new float[]{(float)((borderCells.get(i).x + 1)), (float)(((borderCells.get(i).y)))});
            cellExtent.add(new float[]{(float)(borderCells.get(i).x ), (float)(((borderCells.get(i).y)))});
            cellExtent.add(new float[]{(float)(borderCells.get(i).x), (float)((borderCells.get(i).y + 1))});

            borderCells.get(i).setCellExtent(cellExtent);

            Geometry cellGeom = createPolygon(cellExtent);
/*
            System.out.println("extent: ");
            for(float[] p : cellExtent){
                System.out.println(Arrays.toString(p));
            }

            // print the polygon vertices
            for(int j = 0; j < poly.GetGeometryRef(0).GetPoints().length; j++){

                System.out.println(Arrays.toString(poly.GetGeometryRef(0).GetPoints()[j]));

            }




 */
            double area = cellGeom.GetArea();

            //System.out.println("area: " + cellGeom.GetArea());
            //System.out.println("polygon area: " + poly.GetArea());

            double intersect = poly.Intersection(cellGeom).GetArea();

            // this means that the cell is already partially inside another hole
            if(borderCells.get(i).touchesHole){

                double intersectWithStandBoundary = standGeometry.Intersection(cellGeom).GetArea();
                borderCells.get(i).intersectAreas.add(intersectWithStandBoundary);
                //System.out.println(intersectWithStandBoundary);
                //System.exit(1);
            }


            if(cellMapBorder[borderCells.get(i).x][borderCells.get(i).y]){
                //System.out.println(cells.get(i).intersectArea);
                double intersectWithStandBoundary = standGeometry.Intersection(cellGeom).GetArea();
                System.out.println(intersectWithStandBoundary + " " + standGeometry.GetGeometryCount());
                borderCells.get(i).intersectAreas.add(intersectWithStandBoundary);

                //if(intersectWithStandBoundary != 0.0)
                //    System.exit(1);
                //System.exit(1);
            }

            borderCells.get(i).touchesHole = true;


            intersect = 1.0 - intersect;

            intersectedArea += intersect;

            borderCells.get(i).intersectArea = intersect;

            //borderCells.get(i).intersectArea = intersect;
            cellMapBorderHole[borderCells.get(i).x][borderCells.get(i).y] = true;
            cellsHole.put(i, borderCells.get(i));
            cells.put(i, borderCells.get(i));
            //System.out.println("intersection: " + intersect + " area: " + area + " polyarea: " + polyArea + " intersected area: " + intersectedArea);
            //System.out.println("xdim: " + this.xdim + ", ydim: " + this.ydim);
            //print cellextent
            //for(float[] p : cellExtent){
            //    System.out.println(Arrays.toString(p));
            //}

//
            //System.out.println("---------------------------");

        }


        //System.exit(1);
        this.floodHole(polyArea, normalizedPolygon, intersectedArea, borderCells);

        System.out.println(xdim * ydim);
        //System.exit(1);
        if(true)
            return;



        File debugOutput = new File("/home/koomikko/Documents/customer_work/metsahallitus/HPR_data_final/debug/debug_output.txt");

        if(debugOutput.exists()){
            debugOutput.delete();
        }

        BufferedWriter writer = null;

        try{
            debugOutput.createNewFile();
            writer = new BufferedWriter(new java.io.FileWriter(debugOutput));

            writer.write("x\ty\tid\n");
        }catch(Exception e){
            e.printStackTrace();
        }

        int counter = 0;

        for(int i : cells.keySet()){

            System.out.println("Cell " + i + " has " + cells.get(i).lineSegments.size() + " line segments");

            zonalCell cell = cells.get(i);

            for(ArrayList<float[]> lineSegment : cell.lineSegments){
                System.out.println("line segment has " + lineSegment.size() + " points");

                for(float[] point : lineSegment){
                    System.out.println(Arrays.toString(point));
                    try {
                        //writer.write((point[0] + originX) + "\t" + (originY- point[1]) + "\t" + counter++ + "\n");
                        writer.write((point[0] + originX) + "\t" + (originY- point[1]) + "\t" + i + "\n");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                double cellMinX = cell.x * this.resolution;
                double cellMaxX = (cell.x) * this.resolution + resolution;

                double cellMinY = cell.y * this.resolution - resolution;
                double cellMaxY = (cell.y) * this.resolution;
                System.out.println("cellMinX: " + cellMinX + " cellMaxX: " + cellMaxX + " cellMinY: " + cellMinY + " cellMaxY: " + cellMaxY);
                System.out.println("cell.x: " + cell.x + " cell.y: " + cell.y);
                System.out.println("area: " + calculateAreaToRightOfPolygon(cellMinX, cellMaxX, cellMinY, cellMaxY, lineSegment));
            }
            System.out.println("---------------------");
        }

        try {
            writer.flush();

            writer.close();
        }catch (Exception e){
        }
        System.exit(1);

    }

    public void writeGeometryToFile(String output, Geometry out){

        // Specify output shapefile path
        String outputShapefilePath = output;

        // Create a new shapefile
        Driver driver = ogr.GetDriverByName("ESRI Shapefile");
        DataSource dataSource = driver.CreateDataSource(outputShapefilePath);

        // Create a new layer in the shapefile
        String layerName = "polygon_layer";
        FeatureDefn featureDefn = new FeatureDefn(layerName);
        dataSource.CreateLayer(layerName, null, ogr.wkbPolygon);

        // Create a new feature with a polygon geometry
        Feature feature = new Feature(featureDefn);
        Geometry polygonGeometry = out; // Replace with your polygon geometry
        feature.SetGeometry(polygonGeometry);

        // Get the layer by name and add the feature if the layer exists
        Layer layer = dataSource.GetLayerByName(layerName);
        if (layer != null) {
            layer.CreateFeature(feature);
        } else {
            System.err.println("Layer not found: " + layerName);
        }

        // Cleanup
        feature.delete();
        dataSource.delete();
    }

    public void flood(double polygonArea, ArrayList<float[]> polygon, double intersectionArea, HashMap<Integer, zonalCell> borderCells){

        double totalArea = intersectionArea;

        HashMap<Integer, zonalCell> insideCells = new HashMap<>();

        while(true){

            //System.out.println(borderCells.size());

            boolean anyFound = false;

            for(int i : borderCells.keySet()){

                zonalCell cell = borderCells.get(i);

                ArrayList<float[]> cellextent = (ArrayList<float[]>)(cell.cellExtent.clone());

                boolean foundone = false;
                int whichOne = 0;


                if(cell.x + 1 < this.xdim && cell.y < this.ydim){
                    if(cellMapBorder[cell.x+1][cell.y] == false && cellMapInside[cell.x+1][cell.y] == false) {
                        if (cellInPolygon(polygon, cellextent, (float) 1, (float) 0)) {
                            foundone = true;
                            anyFound = true;
                            whichOne = 1;
                        }
                    }
                }

                if(cell.x - 1 >= 0 && cell.y < this.ydim){
                    if(cellMapBorder[cell.x-1][cell.y] == false && cellMapInside[cell.x-1][cell.y] == false) {
                        if (cellInPolygon(polygon, cellextent, (float) -1, (float) 0)) {
                            foundone = true;
                            anyFound = true;
                            whichOne = 2;
                        }
                    }
                }

                if(cell.x >= 0 && cell.y - 1 >= 0){
                    if(cellMapBorder[cell.x][cell.y-1] == false && cellMapInside[cell.x][cell.y-1] == false) {
                        if (cellInPolygon(polygon, cellextent, (float) 0, (float) -1)) {
                            foundone = true;
                            anyFound = true;
                            whichOne = 3;
                        }
                    }
                }

                if(cell.x < this.xdim && cell.y + 1 < this.ydim){
                    if(cellMapBorder[cell.x][cell.y+1] == false && cellMapInside[cell.x][cell.y+1] == false) {
                        if (cellInPolygon(polygon, cellextent, (float) 0, (float) +1)) {
                            foundone = true;
                            anyFound = true;
                            whichOne = 4;
                        }
                    }
                }


                double addedArea = 0;
                //if(false)
                if(foundone){

                    switch (whichOne){
                        case 1:
                            addedArea = cellsInside(cell.x+1, cell.y, (int)this.resolution, borderCells, insideCells);
                            break;
                        case 2:
                            addedArea = cellsInside(cell.x-1, cell.y, (int)this.resolution, borderCells, insideCells);

                            break;
                        case 3:
                            addedArea = cellsInside(cell.x, cell.y-1, (int)this.resolution, borderCells, insideCells);
                            break;
                        case 4:
                            addedArea = cellsInside(cell.x, cell.y+1, (int)this.resolution, borderCells, insideCells);
                            break;
                    }

                    System.out.println("found one!");
                }

                totalArea += addedArea;
                //System.out.println("Added area (OUTSIDE): " + addedArea);
                //System.out.println("Found a neighbor inside the polygon: " + foundone + " " + whichOne + " " + borderCells.size() + " " + insideCells.size() + " " + totalArea + " / " + polygonArea);
                //System.out.println("Cells.size: " + this.cells.size());

            }
            if(!anyFound){
                System.out.println("DID NOT FIND ANY!");
                break;
            }
        }
    }

    public void floodHole(double polygonArea, ArrayList<float[]> polygon, double intersectionArea, HashMap<Integer, zonalCell> borderCells){

        double totalArea = intersectionArea;

        HashMap<Integer, zonalCell> insideCells = new HashMap<>();

        while(true){

            boolean anyFound = false;

            for(int i : borderCells.keySet()){

                zonalCell cell = borderCells.get(i);

                ArrayList<float[]> cellextent = (ArrayList<float[]>)(cell.cellExtent.clone());

                boolean foundone = false;
                int whichOne = 0;

                if(cell.x + 1 < this.xdim && cell.y < this.ydim){
                    if(cellMapBorderHole[cell.x+1][cell.y] == false && cellMapInsideHole[cell.x+1][cell.y] == false) {
                        if (cellInPolygon(polygon, cellextent, (float) 1, (float) 0)) {
                            foundone = true;
                            anyFound = true;
                            whichOne = 1;
                        }
                    }
                }

                if(cell.x - 1 >= 0 && cell.y < this.ydim){
                    if(cellMapBorderHole[cell.x-1][cell.y] == false && cellMapInsideHole[cell.x-1][cell.y] == false) {
                        if (cellInPolygon(polygon, cellextent, (float) -1, (float) 0)) {
                            foundone = true;
                            anyFound = true;
                            whichOne = 2;
                        }
                    }
                }

                if(cell.x >= 0 && cell.y - 1 >= 0){
                    if(cellMapBorderHole[cell.x][cell.y-1] == false && cellMapInsideHole[cell.x][cell.y-1] == false) {
                        if (cellInPolygon(polygon, cellextent, (float) 0, (float) -1)) {
                            foundone = true;
                            anyFound = true;
                            whichOne = 3;
                        }
                    }
                }

                if(cell.x < this.xdim && cell.y + 1 < this.ydim){
                    if(cellMapBorderHole[cell.x][cell.y+1] == false && cellMapInsideHole[cell.x][cell.y+1] == false) {
                        if (cellInPolygon(polygon, cellextent, (float) 0, (float) +1)) {
                            foundone = true;
                            anyFound = true;
                            whichOne = 4;
                        }
                    }
                }


                double addedArea = 0;
                //if(false)
                if(foundone){

                    switch (whichOne){
                        case 1:
                            addedArea = cellsInsideHole(cell.x+1, cell.y, (int)this.resolution, borderCells, insideCells);
                            break;
                        case 2:
                            addedArea = cellsInsideHole(cell.x-1, cell.y, (int)this.resolution, borderCells, insideCells);

                            break;
                        case 3:
                            addedArea = cellsInsideHole(cell.x, cell.y-1, (int)this.resolution, borderCells, insideCells);
                            break;
                        case 4:
                            addedArea = cellsInsideHole(cell.x, cell.y+1, (int)this.resolution, borderCells, insideCells);
                            break;
                    }

                    System.out.println("found one!");
                }

                totalArea += addedArea;
                //System.out.println("Added area (OUTSIDE): " + addedArea);
                //System.out.println("Found a neighbor inside the polygon: " + foundone + " " + whichOne + " " + borderCells.size() + " " + insideCells.size() + " " + totalArea + " / " + polygonArea);
                //System.out.println("Cells.size: " + this.cells.size());

            }
            if(!anyFound){
                System.out.println("DID NOT FIND ANY!");
                break;
            }
        }
    }
    public int flood(int[][] array, int x, int y, int id) {

        int numberOfConnectedCells = 0;

        if (x < 0 || x >= this.xdim || y < 0 || y >= this.ydim || array[x][y] <= 0) {
            return 0;
        }

        array[x][y] = -id;  // Mark the current cell as visited

        numberOfConnectedCells++;

        // Recursively check neighboring cells
        numberOfConnectedCells += flood(array, x + 1, y, id);
        numberOfConnectedCells += flood(array, x - 1, y, id);
        numberOfConnectedCells += flood(array, x, y - 1, id);
        numberOfConnectedCells += flood(array, x, y + 1, id);

        return numberOfConnectedCells;
    }


    public zonalCell getCell(int x, int y){
        y = this.ydim - y - 1;

        if(this.cells.containsKey(x + y * this.xdim))
            return this.cells.get(x + y * this.xdim);
        else
            return null;
    }

    public double cellsInside(int x, int y, int cellSize, HashMap<Integer, zonalCell> borderCells, HashMap<Integer, zonalCell> insideCells){

        double area = this.resolution * this.resolution;
        // Iterate the neighbors of x and y
        for(int x_ = x - 1; x_ <= x + 1; x_++){
            for(int y_ = y - 1; y_ <= y + 1; y_++){

                if(x_ < 0 || x_ >= this.xdim || y_ < 0 || y_ >= this.ydim){
                    continue;
                }
                // If the neighbor is not the cell itself
                if(x_ != x || y_ != y){

                    // If the neighbor is not already inside the polygon
                    if(!cellMapBorder[x_][y_] && !cellMapInside[x_][y_]){

                        zonalCell tmp = new zonalCell(x_, y_);
                        tmp.intersectArea = 1;
                        this.cells.put(x_ + y_ * this.xdim, tmp);

                        insideCells.put(x_ + y_ * this.xdim, tmp);
                        cellMapInside[x_][y_] = true;
                        area += this.resolution * this.resolution;
                        cellsInside(x_, y_, cellSize, borderCells, insideCells);

                    }
                }
            }
        }

        if(!insideCells.containsKey(x + y * this.xdim)){
            zonalCell tmp = new zonalCell(x, y);
            tmp.intersectArea = 1;
            this.cells.put(x + y * this.xdim, tmp);

            insideCells.put(x + y * this.xdim, tmp);
            cellMapInside[x][y] = true;
        }

        //System.out.println("Added area: " + area);
        return area;
    }

    public double cellsInsideHole(int x, int y, int cellSize, HashMap<Integer, zonalCell> borderCells, HashMap<Integer, zonalCell> insideCells){

        double area = this.resolution * this.resolution;
        // Iterate the neighbors of x and y
        for(int x_ = x - 1; x_ <= x + 1; x_++){
            for(int y_ = y - 1; y_ <= y + 1; y_++){

                if(x_ < 0 || x_ >= this.xdim || y_ < 0 || y_ >= this.ydim){
                    continue;
                }
                // If the neighbor is not the cell itself
                if(x_ != x || y_ != y){

                    // If the neighbor is not already inside the polygon
                    if(!cellMapBorderHole[x_][y_] && !cellMapInsideHole[x_][y_]){

                        zonalCell tmp = new zonalCell(x_, y_);
                        tmp.intersectArea = 0;
                        this.cells.put(x_ + y_ * this.xdim, tmp);

                        insideCells.put(x_ + y_ * this.xdim, tmp);
                        cellMapInsideHole[x_][y_] = true;
                        area += this.resolution * this.resolution;
                        cellsInsideHole(x_, y_, cellSize, borderCells, insideCells);

                    }
                }
            }
        }

        if(!insideCells.containsKey(x + y * this.xdim)){
            zonalCell tmp = new zonalCell(x, y);
            tmp.intersectArea = 0;
            this.cells.put(x + y * this.xdim, tmp);

            insideCells.put(x + y * this.xdim, tmp);
            cellMapInsideHole[x][y] = true;
        }

        //System.out.println("Added area: " + area);
        return area;
    }

    public boolean cellInPolygon(ArrayList<float[]> polygon, ArrayList<float[]> cellExtent, float offsetX, float offsetY){

        for(float[] point : cellExtent){
            if(!pointInPolygon(polygon, point[0] + offsetX, point[1] + offsetY))
                return false;
        }
        return true;

    }

    public boolean anyOfTheCellInPolygon(ArrayList<float[]> polygon, ArrayList<float[]> cellExtent, float offsetX, float offsetY){

        for(float[] point : cellExtent){
            if(pointInPolygon(polygon, point[0] + offsetX, point[1] + offsetY))
                return true;
            else return false;
        }
        return false;

    }


    public boolean pointInPolygon(ArrayList<float[]> polygon, float x, float y){

        int i, j;
        boolean c = false;
        for (i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            if (((polygon.get(i)[1] > y) != (polygon.get(j)[1] > y)) &&
                    (x < (polygon.get(j)[0] - polygon.get(i)[0]) * (y - polygon.get(i)[1]) / (polygon.get(j)[1] - polygon.get(i)[1]) + polygon.get(i)[0])) {
                c = !c;
            }
        }
        return c;

    }



    public static Geometry createPolygon(ArrayList<float[]> points) {
        if (points.size() < 3) {
            throw new IllegalArgumentException("Invalid number of points for a polygon");
        }

        Geometry polygon = new Geometry(ogr.wkbPolygon);
        Geometry ring = new Geometry(ogr.wkbLinearRing);

        for (int i = 0; i < points.size(); i++) {
            ring.AddPoint(points.get(i)[0], points.get(i)[1]);
        }

        polygon.AddGeometryDirectly(ring);

        return polygon;
    }


    public static Geometry createPolygon2(ArrayList<float[]> points) {
        if (points.size() < 3) {
            throw new IllegalArgumentException("Invalid number of points for a polygon");
        }

        Geometry polygon = new Geometry(ogr.wkbPolygon);
        Geometry ring = new Geometry(ogr.wkbLinearRing);

        for (int i = 0; i < points.size(); i++) {
            ring.AddPoint(points.get(i)[0], points.get(i)[1]);
        }

        polygon.AddGeometryDirectly(ring);


        return polygon;
    }

    static class Point {
        double x, y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static List<Point> findIntersection(double minx, double maxx, double miny, double maxy, double[] a, double[] b) {
        Point pointA = new Point(a[0], a[1]);
        Point pointB = new Point(b[0], b[1]);

        List<Point> intersectionPoints = new ArrayList<>();

        // Check intersection with left edge
        addIntersection(intersectionPoints, pointA, pointB, minx, miny, minx, maxy);

        // Check intersection with right edge
        addIntersection(intersectionPoints, pointA, pointB, maxx, miny, maxx, maxy);

        // Check intersection with top edge
        addIntersection(intersectionPoints, pointA, pointB, minx, maxy, maxx, maxy);

        // Check intersection with bottom edge
        addIntersection(intersectionPoints, pointA, pointB, minx, miny, maxx, miny);

        return intersectionPoints;
    }

    private static void addIntersection(List<Point> intersections, Point a, Point b, double x1, double y1, double x2, double y2) {
        Point intersection = calculateIntersection(a, b, x1, y1, x2, y2);
        if (intersection != null) {
            intersections.add(intersection);
        }
    }

    private static Point calculateIntersection(Point a, Point b, double x1, double y1, double x2, double y2) {
        double m = (b.y - a.y) / (b.x - a.x);

        if (b.x - a.x == 0) {  // Vertical line
            double x = a.x;
            double y = m * (x - a.x) + a.y;
            if ((y >= Math.min(a.y, b.y) && y <= Math.max(a.y, b.y)) && (x >= Math.min(x1, x2) && x <= Math.max(x1, x2))) {
                return new Point(x, y);
            }
        } else {
            double c = a.y - m * a.x;

            double x = (y1 - c) / m;
            if (x >= Math.min(a.x, b.x) && x <= Math.max(a.x, b.x) && x >= Math.min(x1, x2) && x <= Math.max(x1, x2)) {
                return new Point(x, y1);
            }
        }

        return null;
    }
    private static double calculateRectangleArea(double y1, double y2, double x1, double x2) {
        // Use the formula for the area of a rectangle
        return Math.abs(y2 - y1) * Math.abs(x2 - x1);
    }

    /*
    public static void getIntersectedPoints(double x1, double y1, double x2, double y2, double gridSize,
                                            List<double[]> intersectedPoints, List<int[]> gridCellCoordinates) {

        int gridX1 = (int) (x1 / gridSize);
        int gridY1 = (int) (y1 / gridSize);
        int gridX2 = (int) (x2 / gridSize);
        int gridY2 = (int) (y2 / gridSize);

        int dx = Math.abs(gridX2 - gridX1);
        int dy = Math.abs(gridY2 - gridY1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        double exactX = x1;
        double exactY = y1;

        boolean hasIntersection = false;

        while (true) {
            intersectedPoints.add(new double[]{exactX, exactY});
            gridCellCoordinates.add(new int[]{gridX1, gridY1});

            // Check for the end of the line
            if (gridX1 == gridX2 && gridY1 == gridY2) {
                hasIntersection = true;
                break;
            }

            int err2 = 2 * err;

            if (err2 > -dy) {
                err -= dy;
                exactX += sx * gridSize;
                gridX1 += sx;

                // Check if the line crosses the border to the next grid cell
                if (sx == 1 && exactX > x2 || sx == -1 && exactX < x2) {
                    intersectedPoints.add(new double[]{exactX, exactY});
                    gridCellCoordinates.add(new int[]{gridX1, gridY1});
                }
            }

            if (err2 < dx) {
                err += dx;
                exactY += sy * gridSize;
                gridY1 += sy;

                // Check if the line crosses the border to the next grid cell
                if (sy == 1 && exactY > y2 || sy == -1 && exactY < y2) {
                    intersectedPoints.add(new double[]{exactX, exactY});
                    gridCellCoordinates.add(new int[]{gridX1, gridY1});
                }
            }
        }

        if (!hasIntersection) {
            intersectedPoints.clear(); // No intersection, clear the list
            gridCellCoordinates.clear();
        }
        if(gridCellCoordinates.size() > 1){
            //remove the last point
            //intersectedPoints.remove(intersectedPoints.size()-1);
            //gridCellCoordinates.remove(gridCellCoordinates.size()-1);
        }
    }
     */


    public void getIntersectedPoints_debug(double x1, double y1, double x2, double y2, double gridSize,
                                            List<double[]> intersectedPoints, List<int[]> gridCellCoordinates) {

        boolean debug = false;
        if((x1 + this.originX) > 621268.5 && (x1 + this.originX) < 621270) { // < 621270 && y1 > 7061215.5 && y1 < 7061216.5){
            System.out.println("DEBUG!!");
            debug = true;
            //System.exit(1);
        }

        int gridX1 = (int) (x1 / gridSize);
        int gridY1 = (int) (y1 / gridSize);
        int gridX2 = (int) (x2 / gridSize);
        int gridY2 = (int) (y2 / gridSize);

        int dx = Math.abs(gridX2 - gridX1);
        int dy = Math.abs(gridY2 - gridY1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        double exactX = x1;
        double exactY = y1;

        boolean hasIntersection = false;

        while (true) {
            intersectedPoints.add(new double[]{exactX, exactY});
            gridCellCoordinates.add(new int[]{gridX1, gridY1});

            // Check for the end of the line
            if (gridX1 == gridX2 && gridY1 == gridY2) {
                hasIntersection = true;
                break;
            }

            int err2 = 2 * err;

            if (err2 > -dy) {
                err -= dy;
                exactX += sx * gridSize;
                gridX1 += sx;

                // Check if the line crosses the border to the next grid cell in the x-direction
                if (sx == 1 || sx == -1 ) {
                    intersectedPoints.add(new double[]{exactX, exactY});
                    gridCellCoordinates.add(new int[]{gridX1, gridY1});
                }
            }

            if (err2 < dx) {
                err += dx;
                exactY += sy * gridSize;
                gridY1 += sy;

                // Check if the line crosses the border to the next grid cell in the y-direction
                if (sy == 1 || sy == -1 ) {
                    intersectedPoints.add(new double[]{exactX, exactY});
                    gridCellCoordinates.add(new int[]{gridX1, gridY1});
                }
            }
        }

        if(debug){

            System.out.println("sx, sy " + sx + ", " + sy);
            System.out.println("dx, dy " + dx + ", " + dy);
            for(int i = 0; i < intersectedPoints.size(); i++){
                System.out.println("intersectedPoints: " + gridCellCoordinates.get(i)[0] + ", " + gridCellCoordinates.get(i)[1]);
            }
            System.exit(1);
        }

        if (!hasIntersection) {
            //intersectedPoints.clear(); // No intersection, clear the list
            //gridCellCoordinates.clear();
        }
        if (gridCellCoordinates.size() > 1) {
            // Remove the last point
            intersectedPoints.remove(intersectedPoints.size() - 1);
            gridCellCoordinates.remove(gridCellCoordinates.size() - 1);
        }
    }

    public static List<int[]> getOverlappingCellsBreihman(double x1, double y1, double x2, double y2, double gridSize) {
        List<int[]> gridCellCoordinates = new ArrayList<>();

        int gridX1 = (int) (x1 / gridSize);
        int gridY1 = (int) (y1 / gridSize);
        int gridX2 = (int) (x2 / gridSize);
        int gridY2 = (int) (y2 / gridSize);

        int dx = Math.abs(gridX2 - gridX1);
        int dy = Math.abs(gridY2 - gridY1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        int err = dx - dy;

        while (true) {
            gridCellCoordinates.add(new int[]{gridX1, gridY1});

            // Check for the end of the line
            if (gridX1 == gridX2 && gridY1 == gridY2) {
                break;
            }

            int err2 = 2 * err;

            if (err2 > -dy) {
                err -= dy;
                gridX1 += sx;
            }

            if (err2 < dx) {
                err += dx;
                gridY1 += sy;
            }
        }

        return gridCellCoordinates;
    }

    public static List<int[]> getOverlappingCells(double x1, double y1, double x2, double y2, double gridSize) {
        List<int[]> gridCellCoordinates = new ArrayList<>();

        int startX = (int) (Math.min(x1, x2) / gridSize);
        int startY = (int) (Math.min(y1, y2) / gridSize);
        int endX = (int) (Math.max(x1, x2) / gridSize);
        int endY = (int) (Math.max(y1, y2) / gridSize);

        int stepX = (x1 <= x2) ? 1 : -1;
        int stepY = (y1 <= y2) ? 1 : -1;

        for (int x = startX; x != endX + stepX; x += stepX) {
            for (int y = startY; y != endY + stepY; y += stepY) {
                if (lineIntersectsCell(x1, y1, x2, y2, gridSize, x, y)) {
                    gridCellCoordinates.add(new int[]{x, y});
                }
            }
        }

        return gridCellCoordinates;
    }

    private static boolean lineIntersectsCell(double x1, double y1, double x2, double y2, double gridSize, int cellX, int cellY) {
        double cellMinX = cellX * gridSize;
        double cellMinY = cellY * gridSize;
        double cellMaxX = (cellX + 1) * gridSize;
        double cellMaxY = (cellY + 1) * gridSize;

        return (x1 < cellMaxX && x2 > cellMinX && y1 < cellMaxY && y2 > cellMinY);
    }
    public static void getIntersectedPoints(double x1, double y1, double x2, double y2, double gridSize,
                                            List<double[]> intersectedPoints, List<int[]> gridCellCoordinates) {

        int gridX1 = (int) (x1 / gridSize);
        int gridY1 = (int) (y1 / gridSize);
        int gridX2 = (int) (x2 / gridSize);
        int gridY2 = (int) (y2 / gridSize);

        double dx = Math.abs(x2 - x1);
        double dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        double err = dx - dy;

        double exactX = x1;
        double exactY = y1;

        boolean hasIntersection = false;

        while (true) {
            intersectedPoints.add(new double[]{exactX, exactY});
            gridCellCoordinates.add(new int[]{gridX1, gridY1});

            // Check for the end of the line
            if (gridX1 == gridX2 && gridY1 == gridY2) {
                hasIntersection = true;
                break;
            }

            double err2 = 2 * err;

            if (err2 > -dy) {
                err -= dy;
                exactX += sx * gridSize;
                gridX1 += sx;

                // Check if the line crosses the border to the next grid cell in the x-direction
                if (sx == 1 || sx == -1 ) {
                    intersectedPoints.add(new double[]{exactX, exactY});
                    gridCellCoordinates.add(new int[]{gridX1, gridY1});
                }
            }

            if (err2 < dx) {
                err += dx;
                exactY += sy * gridSize;
                gridY1 += sy;

                // Check if the line crosses the border to the next grid cell in the y-direction
                if (sy == 1 || sy == -1 ) {
                    intersectedPoints.add(new double[]{exactX, exactY});
                    gridCellCoordinates.add(new int[]{gridX1, gridY1});
                }
            }
        }

        if (!hasIntersection) {
            intersectedPoints.clear(); // No intersection, clear the list
            gridCellCoordinates.clear();
        }
        if (gridCellCoordinates.size() > 1) {
            // Remove the last point
            intersectedPoints.remove(intersectedPoints.size() - 1);
            gridCellCoordinates.remove(gridCellCoordinates.size() - 1);
        }
    }
    public static List<int[]> getIntersectedCells(double x1, double y1, double x2, double y2, double gridSize) {
        List<int[]> intersectedCells = new ArrayList<>();

        int gridX1 = (int) (x1 / gridSize);
        int gridY1 = (int) (y1 / gridSize);
        int gridX2 = (int) (x2 / gridSize);
        int gridY2 = (int) (y2 / gridSize);

        int dx = Math.abs(gridX2 - gridX1);
        int dy = Math.abs(gridY2 - gridY1);
        int sx = gridX1 < gridX2 ? 1 : -1;
        int sy = gridY1 < gridY2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            intersectedCells.add(new int[]{gridX1, gridY1});

            if (gridX1 == gridX2 && gridY1 == gridY2) {
                break;
            }

            int err2 = 2 * err;

            if (err2 > -dy) {
                err -= dy;
                gridX1 += sx;
            }

            if (err2 < dx) {
                err += dx;
                gridY1 += sy;
            }
        }

        return intersectedCells;
    }

    public void ask(int x, int y, int directionAngle){

        //y = this.ydim - y - 1;
        int cellId = xy_to_id2(x, y);

        if(!mode_hole) {
            if (!cellMap.containsKey(cellId)) {
                cellMap.put(cellId, new int[]{x, y, directionAngle});
            }
        }else{
             if(!cellMapHole.containsKey(cellId)) {
                 cellMapHole.put(cellId, new int[]{x, y, directionAngle});
             }
        }
        /*
        if(zonalCounter.changed(cellId)){

            System.out.println("CELL ID: " + cellId);

            if(cells.containsKey(cellId)) {
                zonalCell cell = cells.get(cellId);
                cell.addLineSegment();
                cell.addPoint(new float[]{x, y});

            }else{

                int x_ = (int)(x / this.resolution);
                int y_ = (int)(y / this.resolution);
                zonalCell cell = new zonalCell(x_, y_);
                cell.addLineSegment();
                cell.addPoint(new float[]{x, y});
                cells.put(cellId, cell);

            }

            cells.get(zonalCounter.previousId).addPoint(new float[]{x, y});

        }else{
            System.out.println("NO CHANGE!");
        }

         */

    }

    public static double calculateAreaToRightOfPolygon(double minX, double maxX, double minY, double maxY,
                                                       ArrayList<float[]> polygon) {
        double areaToRight = 0;

        // Iterate through each edge of the polygon
        for (int i = 0; i < polygon.size(); i++) {
            float[] p1 = polygon.get(i);
            float[] p2 = polygon.get((i + 1) % polygon.size());

            // Check if the edge crosses the rectangle
            if (edgeCrossesRectangle(p1, p2, minX, maxX, minY, maxY)) {
                // Calculate intersection points
                double[] intersection1 = calculateIntersection(p1, p2, minX, minY, maxX, minY);
                double[] intersection2 = calculateIntersection(p1, p2, maxX, minY, maxX, maxY);

                // Calculate area of the sub-rectangle to the right of the polygonal line
                areaToRight += calculateRectangleArea(intersection1[0], intersection2[0], minY, maxY);
            }
        }

        return areaToRight;
    }

    // Helper method to check if an edge of the polygon crosses the rectangle
    private static boolean edgeCrossesRectangle(float[] p1, float[] p2, double minX, double maxX,
                                                double minY, double maxY) {
        return (p1[0] < maxX && p2[0] > minX) &&
                (Math.min(p1[1], p2[1]) < maxY && Math.max(p1[1], p2[1]) > minY);
    }

    // Helper method to calculate the intersection point of a line segment with a rectangle edge
    private static double[] calculateIntersection(float[] p1, float[] p2, double x, double y, double xMax, double yMax) {
        double m = (p2[1] - p1[1]) / (p2[0] - p1[0]);
        double intersectionX = x;
        double intersectionY = p1[1] + m * (x - p1[0]);

        if (intersectionY < y) {
            intersectionY = y;
            intersectionX = p1[0] + (y - p1[1]) / m;
        } else if (intersectionY > yMax) {
            intersectionY = yMax;
            intersectionX = p1[0] + (yMax - p1[1]) / m;
        }

        return new double[]{intersectionX, intersectionY};
    }

    // Helper method to calculate the area of a rectangle



    public void query_xy(int x, int y){

        int cellid = xy_to_id(x, y);

        if(cells.containsKey(cellid)) {

            zonalCell cell = cells.get(cellid);

            System.out.println("cell " + cellid + " has " + cell.lineSegments.size() + " line segments");

            for (ArrayList<float[]> lineSegment : cell.lineSegments) {

                System.out.println("line segment has " + lineSegment.size() + " points");

                for (float[] point : lineSegment) {

                    System.out.println(Arrays.toString(point));
                }
            }
        }

    }

    public int xy_to_id(int x, int y){
        x = (int)(x / this.resolution);
        y = (int)(y / this.resolution);
        return x + y * this.xdim;
    }

    public int xy_to_id2(int x, int y){
        return x + y * this.xdim;
    }

    public void setXdim(int xdim){
        this.xdim = xdim;
    }

    public void setYdim(int ydim){
        this.ydim = ydim;
    }

    public void setPolygon(List<ConcaveHull.Point> polygon){
        this.polygon = polygon;
    }
    public void setPolygon2(List<ConcaveHull.Point> polygon){
        this.polygon2 = polygon;
    }

    public void setStandGeometry(Geometry standGeometry){
        this.standGeometry = standGeometry;
    }



    public void setPolygon(double[][] polygon){

        this.polygon = new ArrayList<>();

        for(int i = 0; i < polygon.length; i++){
            this.polygon.add(new ConcaveHull.Point(polygon[i][0], polygon[i][1]));
        }

    }

    public float euclideanDistance(float x1, float y1, float x2, float y2){


        return (float)Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) );

    }

    public float euclideanDistance3d(float x1, float y1, float z1, float x2, float y2, float z2){


        return (float)Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) + (z1 - z2)*(z1 - z2) );

    }



    private static float sinLookup(int a) {
        return a>=0 ? sin[a%(modulus)] : -sin[-a%(modulus)];
    }

    public double bearing(double a1, double a2, double b1, double b2) {

        // if (a1 = b1 and a2 = b2) throw an error
        double theta = FastMath.atan2(b1 - a1, a2 - b2);

        if (theta < 0.0)
            theta += TWOPI;

        return RAD2DEG * theta;

    }

    public int rayTrace(float from_x, float from_y, float from_z, float to_x, float to_y, float to_z){

        float resolution = 1f;

        double[] unNormalized = unNormalizePoint( new float[]{from_x, from_y});

        boolean debug = false;



        if(debug){

            from_x = 15;
            from_y = 15;
            from_z = 5;

            to_x = 30;
            to_y = 30;
            to_z = 10;
        }


        int penetration = 0;

        int minPenetration = 2;


        Vector3d line_quick = new Vector3d(0,0,0);
        Vector3d point_quick = new Vector3d(0,0,0);

        float center_of_pixel_x = (float)from_x;
        float center_of_pixel_y = (float)from_y;
        float center_of_pixel_z = (float)from_z;

        int current_x = (int)from_x;
        int current_y = (int)from_y;
        int current_z = (int)from_z;

        float outside_x = to_x, outside_y = to_y, outside_z = to_z;


        float xy_dist = euclideanDistance(from_x, from_y, to_x, to_y);
        float z_dist = Math.abs(from_z - to_z);
        float xyz_dist =  euclideanDistance3d(from_x, from_y, from_z, to_x, to_y, to_z);

        float zenith_angle = 90.0f - (float)Math.toDegrees(FastMath.atan2(z_dist, xyz_dist));

        float cosAngle2 = cos((FastMath.abs((float)zenith_angle)));
        float sinAngle2 = sin((FastMath.abs((float)zenith_angle)));

        float cosAngle, sinAngle;

        int step_x = -2, step_y = -2;
        float n_points_in_voxel = 0;

        float direction_angle = (float)bearing(center_of_pixel_x, center_of_pixel_y, to_x, to_y);
        int step_z = 1;

        if(unNormalized[0] > 621876 && unNormalized[0] < 621877 && unNormalized[1] > 7060631 && unNormalized[1] < 7060632){

            System.out.println("from " + from_x + " " + from_y + " " + from_z + " to " + to_x + " " + to_y + " " + to_z);

            double[] unNormalized2 = unNormalizePoint( new float[]{to_x, to_y});


            System.out.println("unNormalized " + unNormalized[0] + " " + unNormalized[1]);
            System.out.println("unNormalized2 " + unNormalized2[0] + " " + unNormalized2[1]);
            System.out.println(direction_angle);
            //System.exit(1);
            debug = true;
        }


        if(debug){

            System.out.println("dir: " + direction_angle);
            System.out.println("zen: " + zenith_angle);
            //System.exit(1);

        }

        //System.out.println("direction_angle: " + direction_angle);

        if(direction_angle < 90.0f){

            step_x = 1;
            step_y = -1;

            cosAngle = cos((90.0f - direction_angle));
            sinAngle = sin((90.0f - direction_angle));

            //outside_y = -center_of_pixel_y + sinAngle * 10000;
            //outside_x = center_of_pixel_x + cosAngle * 10000;

            line_quick.set(outside_x-center_of_pixel_x, outside_y-(center_of_pixel_y), outside_z-center_of_pixel_z);

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            float[] y_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(center_of_pixel_y), center_of_pixel_x + 10000, (int)(center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), (float) center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, from_x - 10000,  (int)(center_of_pixel_z+1), from_x + 10000,  (int)(center_of_pixel_z+1));
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, from_x - 10000,  (int)(center_of_pixel_z), from_x + 10000,  (int)(center_of_pixel_z));

            //System.out.println(Arrays.toString(y_intersect));
            //System.out.println(Arrays.toString(x_intersect));
            //System.out.println(Arrays.toString(z_intersect));

            float t_max_x, t_max_y, t_max_z;

            if(y_intersect == null){
                t_max_y = Float.POSITIVE_INFINITY;
            }else{
                t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            }

            if(x_intersect == null){
                t_max_x = Float.POSITIVE_INFINITY;
            }else{
                t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;

            }

            if(z_intersect == null){
                t_max_z = Float.POSITIVE_INFINITY;
            }else{
                //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
                t_max_z =  FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;
            }

            boolean breakki = false;
            int counter = 0;
            /* RAY TRACE ITERATIONS */


            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {

                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;


                } else {

                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;


                }
                counter++;

                if(current_x > (int)to_x)
                    current_x = (int)to_x;

                if(current_y < (int)to_y)
                    current_y = (int)to_y;

                if(current_x < 0 || current_x >= this.xdim || current_y < 0 || current_y >= this.ydim)
                    return 0;

                this.ask(current_x, current_y, (int)direction_angle);

                if(current_x >= (int)to_x && current_y <= (int)to_y) {
                    return 1;
                }


            }

        }

        else if(direction_angle < 180.0f){

            // System.out.println("HERE!!");
            step_x = 1;
            step_y = 1;

            cosAngle = cos((180.0f - direction_angle));
            sinAngle = sin((180.0f - direction_angle));

            // outside_y = -center_of_pixel_y - cosAngle * 10000;
            //outside_x = center_of_pixel_x + sinAngle * 10000;

            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(center_of_pixel_y), outside_z-center_of_pixel_z);

            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            //from: 13.070367 5.5872746


            float[] y_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(center_of_pixel_y+1), center_of_pixel_x + 10000, (int)(center_of_pixel_y+1));


            float[] x_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD BE HERE!");
            }else
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));

            if(debug){

                System.out.println("y_intersect: " + Arrays.toString(y_intersect));
                System.out.println("x_intersect: " + Arrays.toString(x_intersect));
                System.out.println("z_intersect: " + Arrays.toString(z_intersect));
                //System.exit(1);
            }

            //if(y_intersect == null || x_intersect == null || z_intersect == null)
            //    return 2;
            float t_max_x, t_max_y, t_max_z;

            if(y_intersect == null){
                t_max_y = Float.POSITIVE_INFINITY;
            }else{
                t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            }

            if(x_intersect == null){
                t_max_x = Float.POSITIVE_INFINITY;
            }else{
                t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;

            }

            if(z_intersect == null){
                t_max_z = Float.POSITIVE_INFINITY;
            }else{
                //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
                t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;
            }

            //float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            //float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            //float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {

                    t_current = t_max_x;
                    t_max_x = t_max_x + t_d_x;
                    current_x += step_x;


                } else {

                    t_current = t_max_y;
                    t_max_y = t_max_y + t_d_y;
                    current_y += step_y;


                }


                if(current_x > (int)to_x)
                    current_x = (int)to_x;

                if(current_y > (int)to_y)
                    current_y = (int)to_y;

                if(current_x < 0 || current_x >= this.xdim || current_y < 0 || current_y >= this.ydim)
                    return 0;

                this.ask(current_x, current_y, (int)direction_angle);

                if(debug){
                    System.out.println("current_x: " + current_x + " current_y: " + current_y + " current_z: " + current_z);
                    System.out.println("from_x: " + from_x + " from_y: " + from_y + " from_z: " + from_z);
                    System.out.println("--------------------");
                }

                if(current_x >= (int)to_x && current_y >= (int)to_y) {
                    //if(debug)
                    //    System.exit(1);
                    return 1;
                }

            }



        }

        else if(direction_angle < 270.0f){

            step_x = -1;
            step_y = 1;

            //cosAngle = (float)Math.cos(Math.toRadians(270.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(270.0 - direction_angle));

            cosAngle = cos((270.0f - direction_angle));
            sinAngle = sin((270.0f - direction_angle));

            //outside_y = -center_of_pixel_y - sinAngle * 10000;
            //outside_x = center_of_pixel_x - cosAngle * 10000;

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(center_of_pixel_y), outside_z-center_of_pixel_z);


            float[] y_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, from_x - 10000, (int)(center_of_pixel_y+1), from_x + 10000, (int)(center_of_pixel_y+1));
            float[] x_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x), center_of_pixel_y + 10000, (int)(center_of_pixel_x), center_of_pixel_y - 10000);

            float[] z_intersect;

            if(true)
            if(debug) {
                lineIntersect_debug(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, from_x - 1000000, (int) (center_of_pixel_y + 1), from_x + 1000000, (int) (center_of_pixel_y + 1));
                lineIntersect_debug(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x), center_of_pixel_y + 10000, (int)(center_of_pixel_x), center_of_pixel_y - 10000);
                y_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, from_x - 1000000, (int)(center_of_pixel_y+1), from_x + 1000000, (int)(center_of_pixel_y+1));
                System.out.println(Arrays.toString(y_intersect));
                System.out.println(Arrays.toString(x_intersect));
                System.out.println(center_of_pixel_x + " " + center_of_pixel_y + " " + outside_x + " " + outside_y + " " + from_x + " " + from_y);
                //System.exit(1);
            }

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));


            float t_max_x, t_max_y, t_max_z;

            if(y_intersect == null){
                t_max_y = Float.POSITIVE_INFINITY;
            }else{
                t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            }

            if(x_intersect == null){
                t_max_x = Float.POSITIVE_INFINITY;
            }else{
                t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;

            }

            if(z_intersect == null){
                t_max_z = Float.POSITIVE_INFINITY;
            }else{
                //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
                t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;
            }

            if(debug){
                System.out.println("t_max_x: " + t_max_x);
                System.out.println("t_max_y: " + t_max_y);

            }

            if(debug){
                System.out.println("STARTING: " + current_x + " " + current_y);
            }

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {

                    t_current = t_max_x;
                    t_max_x = t_max_x + t_d_x;
                    current_x += step_x;

                    if(debug)
                        System.out.println("x");

                } else {

                    t_current = t_max_y;
                    t_max_y = t_max_y + t_d_y;
                    current_y += step_y;

                    if(debug)
                        System.out.println("y");

                }



                if(current_x < (int)to_x)
                    current_x = (int)to_x;

                if(current_y > (int)to_y)
                    current_y = (int)to_y;

                if(current_x < 0 || current_x >= this.xdim || current_y < 0 || current_y >= this.ydim)
                    return 0;

                this.ask(current_x, current_y, (int)direction_angle);

                if(debug){
                    System.out.println("current: " + current_x + " " + current_y);
                    System.out.println("origin: " + from_x + " " + from_y);

                }

                if(current_x <= (int)to_x && current_y >= (int)to_y) {
                    //if(debug)
                   //     System.exit(1);
                    return 1;
                }

            }



        }

        else if(direction_angle < 360.0f){

            step_x = -1;
            step_y = -1;

            //cosAngle = (float)Math.cos(Math.toRadians(360.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(360.0 - direction_angle));

            cosAngle = cos((360.0f - direction_angle));
            sinAngle = sin((360.0f - direction_angle));

            //outside_y = -center_of_pixel_y + cosAngle * 10000;
            //outside_x = center_of_pixel_x - sinAngle * 10000;


            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            // Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(center_of_pixel_y), outside_z-center_of_pixel_z);


            float[] y_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, from_x - 10000, (int)(center_of_pixel_y), from_x + 10000, (int)(center_of_pixel_y));
            //float[] y_intersect_z = lineIntersect(-center_of_pixel_y, center_of_pixel_z, outside_y, outside_z, z - 10000, (int)(-center_of_pixel_y), x + 10000, (int)(-center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x), center_of_pixel_y + 10000, (int)(center_of_pixel_x), center_of_pixel_y - 10000);


            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, from_x - 10000,  (int)(center_of_pixel_z+1), from_x + 10000, (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, from_x - 10000, (int)(center_of_pixel_z), from_x + 10000,  (int)(center_of_pixel_z));


            //System.out.println(Arrays.toString(y_intersect));
            //System.out.println(Arrays.toString(x_intersect));
            float t_max_x, t_max_y, t_max_z;

            if(y_intersect == null){
                t_max_y = Float.POSITIVE_INFINITY;
            }else{
                t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            }

            if(x_intersect == null){
                t_max_x = Float.POSITIVE_INFINITY;
            }else{
                t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;

            }

            if(z_intersect == null){
                t_max_z = Float.POSITIVE_INFINITY;
            }else{
                //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
                t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;
            }

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {

                    t_current = t_max_x;
                    t_max_x = t_max_x + t_d_x;
                    current_x += step_x;


                } else {

                    t_current = t_max_y;
                    t_max_y = t_max_y + t_d_y;
                    current_y += step_y;


                }


                if(current_x < (int)to_x)
                    current_x = (int)to_x;

                if(current_y < (int)to_y)
                    current_y = (int)to_y;

                if(current_x < 0 || current_x >= this.xdim || current_y < 0 || current_y >= this.ydim)
                    return 0;

                this.ask(current_x, current_y, (int)direction_angle);


                if(current_x <= (int)to_x && current_y <= (int)to_y) {
                    return 1;
                }


            }
        }

        return 3;

    }


}
