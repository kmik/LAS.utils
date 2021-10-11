package utils;

//import ucar.ma2.Array;

import java.util.*;

public class VoxelNeighborhood {

    //public float normal;
    public int id;
    double[][] data;
    int[] pointIndexes;
    public short count = 0;
    public short capacity = 0;

    public boolean stem = false;
    public boolean isSurface = false;

    public int x;
    public int y;
    public int z;

    public byte numberOfPointsInCenter = 0;

    float normal;
    List<KdTree.XYZPoint> nearest;

    double[] colMeans = new double[3];

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

        data = null;
    }

    public void addPoint(double x, double y, double z, int index){


        colMeans[0] += x;
        colMeans[1] += y;
        colMeans[2] += z;

        data[0][count] = x;
        data[1][count] = y;
        data[2][count] = z;

        pointIndexes[count] = index;

        count++;

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

    public void calculateNormal(){

        colMeans[0] /= capacity;
        colMeans[1] /= capacity;
        colMeans[2] /= capacity;

        Data dat = new Data(data, colMeans);

        dat.center();
        EigenSet eigen = dat.getCovarianceEigenSet();

        double angle = Math.toDegrees(Math.acos(eigen.vectors[2][2]/1.0));
        double[][] vals = {eigen.values};
        //double flatness = 1.0 - (vals[0][2] / (vals[0][0] + vals[0][1] + vals[0][2]));

        this.normal = (float)angle;

        if(!Double.isNaN(angle)) {
            //System.out.println(data[0][data[0].length-1]);
            //System.out.println("z angle: " + angle);
            if(Math.abs(angle - 90) <= 25.0){
                //System.out.println("flatness: " + flatness);
                this.stem = true;
            }
        }
    }

    public void setId(int id){
        this.id = id;
    }
}
