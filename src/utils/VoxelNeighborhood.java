package utils;

//import ucar.ma2.Array;

import java.util.*;

import com.clust4j.algo.DBSCAN;
import com.clust4j.algo.DBSCANParameters;
import ij.util.ArrayUtil;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

public class VoxelNeighborhood {

    //public float normal;
    public int id;
    double[][] data;
    int[] pointIndexes;
    public short count = 0;
    public short capacity = 0;

    public Array2DRowRealMatrix mat;
    HashSet<xyz> buffer_neighbors;


    public boolean stem = false;
    public boolean isSurface = false;
    public boolean isSurface_buffer = false;
    public boolean ignore_this = false;

    public int n_solid_points = 0;
    public boolean isSolid = false;

    public int x;
    public int y;
    public int z;

    public byte numberOfPointsInCenter = 0;

    public int counter = 0;
    public float normal;
    public float flatness;
    List<KdTree.XYZPoint> nearest;

    double[] colMeans = new double[3];

    public boolean garbage;
    public HashSet<Integer> goodIndexes;

    public VoxelNeighborhood(int x, int y, int z){

        this.x = x;
        this.y = y;
        this.z = z;

    }

    public void prepare(){

        data = new double[3][capacity];
        pointIndexes = new int[capacity];

    }

    public void release(){

        this.mat = null;
        data = null;
    }

    public void count(){

        this.counter++;

        if(this.counter == this.capacity){

        }

    }

    public synchronized void addPoint(double x, double y, double z, int index){

        if(counter == 0){
            this.prepare();
        }

        counter++;

        colMeans[0] += x;
        colMeans[1] += y;
        colMeans[2] += z;

        data[0][count] = x;
        data[1][count] = y;
        data[2][count] = z;

        pointIndexes[count] = index;

        count++;

        if(counter == this.capacity){
            this.calculateNormal();
        }

    }

    public void addObservable(){
        capacity++;
    }

    public void filter(){

        goodIndexes = new HashSet<>();

        for(int i = 0; i < pointIndexes.length; i++){
            goodIndexes.add(pointIndexes[i]);
        }

        if(true)
            return;

        KdTree kd_tree = new KdTree();
        int n = data[0].length;

        if(n <= 7){
            return;
        }

        double[][] neighbor = new double[3][3];

        //KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(0, 0, 0);

        for(int i = 0; i < n; i++){

            KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(data[0][i], data[1][i], data[2][i]);
            //tempTreePoint.x = data[0][i];
            //tempTreePoint.y = data[1][i];
            //tempTreePoint.z = data[2][i];

            tempTreePoint.setIndex(i);
            kd_tree.add(tempTreePoint);

        }

        KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(0,0,0);
        //Iterator it = kd_tree.iterator();
        int counter = 0;

        HashSet<Integer> alreadyAccepted = new HashSet<>();

        double[] colMeans = new double[3];
        ArrayList<Integer> accept = new ArrayList<>(3);
        ArrayList<Integer> good = new ArrayList<>(3);
        accept.add(0);
        accept.add(0);
        accept.add(0);
        good.add(0);
        good.add(0);
        good.add(0);

        for(int j = 0; j < n; j++){

            if(alreadyAccepted.contains(j))
                continue;

            tempTreePoint.x = data[0][j];
            tempTreePoint.y = data[1][j];
            tempTreePoint.z = data[2][j];
            //KdTree.XYZPoint tem = (KdTree.XYZPoint)it.next();

            nearest = (List<KdTree.XYZPoint>)kd_tree.nearestNeighbourSearch(4, tempTreePoint);
            nearest.remove(0);
            //System.out.println(nearest.get(0).x + " == " + tempTreePoint.x );

            colMeans[0] = 0;
            colMeans[1] = 0;
            colMeans[2] = 0;

            if(nearest.size() != 3)
                continue;

            for(int i = 0; i < nearest.size(); i++){

                neighbor[0][i] = nearest.get(i).x;
                neighbor[1][i] = nearest.get(i).y;
                neighbor[2][i] = nearest.get(i).z;

                colMeans[0] += neighbor[0][i];
                colMeans[1] += neighbor[1][i];
                colMeans[2] += neighbor[2][i];

                accept.set(i, nearest.get(i).getIndex());
                good.set(i, pointIndexes[nearest.get(i).getIndex()]);
            }

            colMeans[0] /= nearest.size();
            colMeans[1] /= nearest.size();
            colMeans[2] /= nearest.size();

            Data tempData = new Data(neighbor, colMeans);

            tempData.center();
            EigenSet eigen = tempData.getCovarianceEigenSet();
            double angle = Math.toDegrees(Math.acos(eigen.vectors[2][2]/1.0));

            angle = Math.abs(angle - 90);
            //System.out.println(angle);
            if(angle <= 15){

                alreadyAccepted.addAll(accept);

                goodIndexes.addAll(good);
                counter++;
            }
        }


        //System.out.println("good points: " + counter + " / " + n);
    }

    public void cluster(){


    }
    public void calculateNormal(){

        //System.out.println("calculating normal");


        mat = new Array2DRowRealMatrix(capacity, 3);

        this.n_solid_points = (int)(((double)n_solid_points / (double)capacity) * 100.0);


        // THIS IS PERCENTAGE 0 - 100
        if(n_solid_points > 50){

            this.isSolid = true;

        }


        for(int i = 0; i < capacity; i++){

            this.mat.setEntry(i, 0, data[0][i]);
            this.mat.setEntry(i, 1, data[1][i]);
            this.mat.setEntry(i, 2, data[2][i]);

        }
/*
        DBSCAN db = new DBSCANParameters(0.2).fitNewModel(mat);
        final int[] results = db.getLabels();

        System.out.println(Arrays.toString(results));
*/
        colMeans[0] /= capacity;
        colMeans[1] /= capacity;
        colMeans[2] /= capacity;

        Data dat = new Data(data, colMeans);

        dat.center();
        EigenSet eigen = dat.getCovarianceEigenSet();

        double delta_x = eigen.vectors[0][2];
        double delta_y = eigen.vectors[1][2];
        double delta_z = eigen.vectors[2][2];

        int[] smallest_to_largest_id = new int[3];

        this.garbage = smallestToLargest(eigen.values, smallest_to_largest_id);
/*
        if(smallest_to_largest_id[0] != 2){

            System.out.println(bad_data);
            System.out.println(Arrays.toString(smallest_to_largest_id));
            System.out.println(Arrays.toString(eigen.values));

        }
*/
        //System.out.println(Arrays.toString(eigen.values));

        double dist3d = euclideanDistance_3d(colMeans[0], colMeans[1], colMeans[2], colMeans[0]+delta_x, colMeans[1]+delta_y, colMeans[2]+delta_z);
        double dist2d = euclideanDistance(colMeans[0], colMeans[1], colMeans[0]+delta_x, colMeans[1]+delta_y);

        double angle1 = Math.toDegrees(Math.cos(dist2d / dist3d));
        //double angle = Math.toDegrees(Math.acos(eigen.vectors[2][2]/1.0));
        double angle = Math.toDegrees(Math.acos(eigen.vectors[smallest_to_largest_id[0]][2]/1.0));



        if(angle1 < 0) {
            System.out.println(Arrays.toString(eigen.vectors[0]));
            System.out.println(Arrays.toString(eigen.vectors[1]));
            System.out.println(Arrays.toString(eigen.vectors[2]));
            System.out.println(angle + " " + angle1);
            System.out.println("----------------");
        }

        double[][] vals = {eigen.values};

        boolean negatives = false;

        for(int i = 0; i < eigen.values.length; i++)
            if(eigen.values[i] < 0.0d){
                negatives = true;
                break;
            }
        this.flatness = (float)(1.0f - (Math.abs(vals[0][2]) / (Math.abs(vals[0][0]) + Math.abs(vals[0][1]) + Math.abs(vals[0][2]))));

        // This flatness measure is from here: https://www.mdpi.com/1999-4907/7/9/207
        //this.flatness = (float)(((eigen.values[1]) - (eigen.values[2])) / (eigen.values[0]));
        //this.flatness = (float)(((eigen.values[smallest_to_largest_id[1]]) - (eigen.values[smallest_to_largest_id[0]])) / (eigen.values[smallest_to_largest_id[2]]));
        //this.normal = (float)angle1;

        // This flatness measure is from here: https://robotik.informatik.uni-wuerzburg.de/telematics/download/IAS-tutorial-06.pdf
        this.flatness = (float)((eigen.values[smallest_to_largest_id[0]]) / (eigen.values[smallest_to_largest_id[1]]));
        //this.flatness = (float)(Math.abs(eigen.values[2]) / Math.abs(eigen.values[1]));
        //System.out.println(this.flatness);

        //if(this.flatness == 0)
        //    System.out.println("WHAT THE ACTUAL FUCK! " + Arrays.toString(eigen.values) + " " + this.capacity);

        if(Float.isNaN(this.flatness) || Float.isInfinite(this.flatness))
            this.flatness = 0;


        if(negatives)
            this.flatness = Float.NaN;

        /*
        if(this.flatness < 0){
            System.out.println(Arrays.toString(eigen.values));
        }

         */
        this.normal = (float)angle;

        if(angle == 0.0){
            //System.out.println(Arrays.toString(eigen.values));
        }
        //System.out.println( "flatness: " + this.flatness);

        if(!Double.isNaN(angle)) {
            //System.out.println(data[0][data[0].length-1]);
            //System.out.println("z angle: " + angle);
            //if(Math.abs(angle - 90) <= 25.0){
            //if(Math.abs(angle - 90) <= 33.0){
            //if(angle1 <= 35.0){
            if(angle > 70.0 && angle < 110){

                if(flatness < 0.5)
                //System.out.println("flatness: " + flatness);
                    this.stem = true;
            }
        }

        this.release();
    }

    public boolean smallestToLargest(double[] values, int[] order){

        boolean garbage = false;

        int[] indexes = {0,1,2};
        TreeMap<Double,Integer> map = new TreeMap<Double,Integer>();

        for( int i : indexes ) {

            if(Double.isNaN(values[i]))
                garbage = true;
            map.put(values[i], i);
            //System.out.println(i + " " + values[i]);
        }
        //System.out.println(Arrays.toString(map.values().toArray()) + " " + map.keySet().size());
        int count = 0;

        for(int i : map.values()){
            order[count++] = i;
        }

        return garbage;
    }

    public void setId(int id){
        this.id = id;
    }

    public void addBufferNeighbor(int x, int y, int z){

        if(buffer_neighbors == null){
            buffer_neighbors = new HashSet<>();
        }

        buffer_neighbors.add(new xyz(x, y, z));
    }

    class xyz{
        int x, y, z;
        xyz(int x, int y, int z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }


    public double euclideanDistance_3d(double x1, double y1, double z1, double x2, double y2, double z2){


        return Math.sqrt( ( x1 - x2) * ( x1 - x2) + ( y1 - y2) * ( y1 - y2) + ( z1 - z2) * ( z1 - z2) );

    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){
        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }

}
