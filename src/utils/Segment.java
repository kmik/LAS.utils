package utils;

import org.tinfour.common.IIncrementalTinNavigator;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.utils.Polyside;
import tools.lasSort;

import java.util.*;

import static org.tinfour.utils.Polyside.isPointInPolygon;

public class Segment {


    int numberOfSamples = 0;
    boolean compact = true;

    double geometricCenterX = 0;
    double geometricCenterY = 0;

    double meanDistanceFromGeometricCenter = 0;
    public IncrementalTin tin = new IncrementalTin();
    IIncrementalTinNavigator navi = tin.getNavigator();
    SimpleTriangle triang = null;

    List<IQuadEdge> perimeter = new ArrayList<>();

    Queue<QueueItem> queue = new PriorityQueue<>();

    public TreeMap<Integer, Integer> affiliationPercentages = new TreeMap<>();

    int affiliation = 0;

    double area = 0;
    int counter = 0;

    public int id;
    public ArrayList<int[]> points = new ArrayList<>();

    double resolution = Double.NaN;
    int center_x, center_y;

    double[] boundingBox = new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};

    double probabilityToUpdateCenter = 0.05;

    double areaLimit = 0;

    ArrayList<Integer> features = new ArrayList<>();
    ArrayList<String> features_s = new ArrayList<>();

    rolling_stats rs_center_x = new rolling_stats();
    rolling_stats rs_center_y = new rolling_stats();
    public Segment() {

    }

    public double getArea(){

        if(this.resolution == Double.NaN)
            return this.area;
        else
            return this.area * Math.pow(this.resolution / 1000, 2);

    }

    public void setAreaLimit(double areaLimit){
        this.areaLimit = areaLimit;
    }

    public double getAreaLimit(){
        return this.areaLimit;
    }

    public Segment(int id) {
        this.id = id;
    }

    public void addFeature(int feature){
        features.add(feature);
    }

    public void addFeature(String feature){
        features_s.add(feature);
    }

    public void setAffiliationPercentages(TreeMap<Integer, Integer> affiliationPercentages){
        this.affiliationPercentages = affiliationPercentages;
    }
    public boolean addPoint(int[] point) {
        points.add(point);

        if(true)
        if(!tin.isBootstrapped()){
            tin.add(new Vertex(point[0], point[1], this.id));
            navi.resetForChangeToTin();
        }else{

            if(this.perimeter.size() <= 0)
                this.perimeter = tin.getPerimeter();
            //triang = navi.getContainingTriangle(point[0], point[1]);
            //navi.resetForChangeToTin();
            if(isPointInPolygon(perimeter, point[0], point[1]) == Polyside.Result.Outside){
            //if (triang == null) {
            //if (navi.isPointInsideTin((double)point[0], (double)point[1])) {

                Vertex v = new Vertex(point[0], point[1], this.id);
                //
                //System.out.println("Segment " + id + " TIN area: " + tinArea);
                tin.add(v);
                navi.resetForChangeToTin();

                this.perimeter = tin.getPerimeter();

                double perimeterLength = 0;
                double geometricCenterX = 0;
                double geometricCenterY = 0;



                for(IQuadEdge e : perimeter){

                    perimeterLength += e.getLength();
                    geometricCenterX += e.getA().getX();
                    geometricCenterY += e.getA().getY();

                    if(e.getA().getX() < boundingBox[0])
                        boundingBox[0] = e.getA().getX();
                    if(e.getA().getX() > boundingBox[2])
                        boundingBox[2] = e.getA().getX();
                    if(e.getA().getY() < boundingBox[1])
                        boundingBox[1] = e.getA().getY();
                    if(e.getA().getY() > boundingBox[3])
                        boundingBox[3] = e.getA().getY();


                }

                geometricCenterX /= perimeter.size();
                geometricCenterY /= perimeter.size();



                this.geometricCenterX = geometricCenterX;
                this.geometricCenterY = geometricCenterY;

                //double circle_radius = perimeterLength / (2.0 * Math.PI);
                //double circle_area = Math.PI * Math.pow(circle_radius, 2);
                double tinArea = tin.countTriangles().getAreaSum();

                double boundingBoxArea = (boundingBox[2] - boundingBox[0]) * (boundingBox[3] - boundingBox[1]);

                double compactness = tinArea / boundingBoxArea;

                this.area = tinArea;

                if(true) {
                    if(areaLimit > 0){

                        if(tinArea > areaLimit * 1.0){
                            tin.remove(v);
                            navi.resetForChangeToTin();
                            this.perimeter = tin.getPerimeter();
                            //System.out.println("Segment " + id + " compactness: " + compactness + " " + tinArea);
                            //System.exit(1);
                            return false;
                        }

                    }

                    if (compactness < 0.55 && tinArea > 13) {
                        tin.remove(v);
                        navi.resetForChangeToTin();
                        this.perimeter = tin.getPerimeter();
                        //System.out.println("Segment " + id + " compactness: " + compactness + " " + tinArea);
                        //System.exit(1);
                        return false;
                    }
                }
                //double compactness = tinArea / circle_area;

                //System.out.println("Segment " + id + " compactness: " + compactness);

                if(false) {
                    if(compactness <= 0.50){

                        this.meanDistanceFromGeometricCenter = 0;

                        for (IQuadEdge e : tin.getPerimeter()) {
                            meanDistanceFromGeometricCenter += Math.sqrt(Math.pow(e.getA().getX() - geometricCenterX, 2) + Math.pow(e.getA().getY() - geometricCenterY, 2));
                        }

                        meanDistanceFromGeometricCenter /= tin.getPerimeter().size();
                        this.geometricCenterX = geometricCenterX;
                        this.geometricCenterY = geometricCenterY;

                        this.compact = false;
                        //System.out.println("NOT COMPACT");
                        //tin.remove(v);
                        //navi.resetForChangeToTin();
                        return true;

                    }
                }
            }
            else{
                //System.out.println("WHAT");
            }
        }

        return true;

    }

    public void addToQueue(int[] point) {

        if(rs_center_x.count_rolling_stats == 0) {
            rs_center_x.add(point[0]);
            rs_center_y.add(point[1]);
        }

        if(Math.random() < probabilityToUpdateCenter) {
            // Set the center to be the 2/3 of the distance between current center and input point

            rs_center_x.add(point[0]);
            rs_center_y.add(point[1]);

            this.center_x = (int) Math.round(rs_center_x.average_rolling_stats);
            this.center_y = (int) Math.round(rs_center_y.average_rolling_stats);
            // this.center_x = (int) Math.round((this.center_x + point[0]) / 2);
            //this.center_y = (int) Math.round((this.center_y + point[1]) / 2);

            if(rs_center_x.count_rolling_stats > 5){
                rs_center_x.reset();
                rs_center_y.reset();
            }
            //this.updateDistancesInQueue();
        }

        if(false)
        if(counter++ >= 10000){

            //this.updateGeometricCenter();
            this.updateCenterCoordinatesToGeometricCenter();
            this.updateDistancesInQueue();
            //System.out.println("New center: " + this.center_x + " " + this.center_y);
            counter = 0;
        }

        double distance = Math.sqrt(Math.pow(point[0] - this.center_x, 2) + Math.pow(point[1] - this.center_y, 2));

        //queue.add(new QueueItem(point, this.queue.size()));
        queue.add(new QueueItem(point, distance));

    }

    public void updateDistancesInQueue() {
        Queue<QueueItem> newQueue = new PriorityQueue<>();
        while(queue.size() > 0) {
            QueueItem qI = queue.poll();
            qI.distance = Math.sqrt(Math.pow(qI.point[0] - this.center_x, 2) + Math.pow(qI.point[1] - this.center_y, 2));
            newQueue.add(qI);
        }
        queue = newQueue;
    }

    public void updateCenterCoordinatesToGeometricCenter() {

        //navi.resetForChangeToTin();

        double sum_x = 0;
        double sum_y = 0;
        double count = 0;

        if(false) {
            for (SimpleTriangle t : tin.triangles()) {

                Vertex a = t.getVertexA(), b = t.getVertexB(), c = t.getVertexC();

                double x = (a.getX() + b.getX() + c.getX()) / 3.0;
                double y = (a.getY() + b.getY() + c.getY()) / 3.0;

                sum_x += x;
                sum_y += y;
                count++;
            }

            this.center_x = (int) Math.round(sum_x / count);
            this.center_y = (int) Math.round(sum_y / count);
        }

        this.center_x = (int) Math.round(this.geometricCenterX);
        this.center_y = (int) Math.round(this.geometricCenterY);
    }

    public void setAffiliation(int affiliation) {
        this.affiliation = affiliation;
    }

    public int getAffiliation() {
        return this.affiliation;
    }

    public void updateGeometricCenter() {

        double perimeterLength = 0;
        double geometricCenterX = 0;
        double geometricCenterY = 0;

        for (IQuadEdge e : tin.getPerimeter()) {

            perimeterLength += e.getLength();
            geometricCenterX += e.getA().getX();
            geometricCenterY += e.getA().getY();

        }

        geometricCenterX /= tin.getPerimeter().size();
        geometricCenterY /= tin.getPerimeter().size();

        this.geometricCenterX = geometricCenterX;
        this.geometricCenterY = geometricCenterY;

    }

    public QueueItem getFromQueue() {
        return queue.poll();
    }

    public void debugQueue() {
        while(queue.size() > 0)
            System.out.println(queue.poll().distance);
    }

    public void printQueueSize() {
        System.out.println("Queue size: " + queue.size());
    }

    public void setCenter(int x, int y) {
        this.center_x = x;
        this.center_y = y;
    }


    public static class QueueItem implements Comparable<QueueItem> {
        public int[] point;
        public double distance;

        public QueueItem(int[] point, double distance) {
            this.point = point;
            this.distance = distance;
        }

        // Comparator
        @Override
        public int compareTo(QueueItem qI) {
            if(this.distance < qI.distance) {
                return -1;
            }else if(this.distance > qI.distance) {
                return 1;
            }else{
                return 0;
            }
        }

        public void dispose() {
            this.point = null;
        }
    }

    public void addTinVertices(IncrementalTin inputTin){
        for(Vertex v : this.tin.getVertices()){
            //System.out.println("ADDED");
            inputTin.add(v);
        }
    }

    public int numFeatrues(){
        return features.size();
    }

    public double getResolution(){
        return resolution;
    }

    public void setResolution(double resolution){
        this.resolution = resolution;
    }

    public void mergeSegmentToThisOne(Segment s){
        for(int[] p : s.points){
            this.points.add(p);

        }

        for(int f : s.features){
            this.features.add(f);
        }

        this.area += s.getArea();


    }

}
