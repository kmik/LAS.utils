package tools;

import java.util.ArrayList;

public class treeStem {


    int[] layersArray;
    int[][] midpointsArray;


    ArrayList<Integer> layers = new ArrayList<>();

    int id;

    public int[] baseLocation = new int[]{-1,-1};

    ArrayList<int[]> midpoints = new ArrayList<int[]>();
    ArrayList<ArrayList<int[]>> allPoints = new ArrayList<ArrayList<int[]>>();

    ArrayList<Double> distanceToPreviousLayer = new ArrayList<>();

    public treeStem(int id, int x, int y, int layer, ArrayList<int[]> allPoints){

        this.layers.add(layer);
        this.allPoints.add((ArrayList<int[]>)allPoints.clone());
        this.id = id;
        this.midpoints.add(new int[]{x,y});

        this.baseLocation[0] = x;
        this.baseLocation[1] = y;

    }

    public void addLayer(int layer, int[] midpoint, ArrayList<int[]> allPoints){

        if(layers.size() > 0){
            distanceToPreviousLayer.add(euclideanDistance(midpoint[0], midpoint[1], midpoints.get(midpoints.size()-1)[0], midpoints.get(midpoints.size()-1)[1]));
        }
        this.allPoints.add((ArrayList<int[]>)allPoints.clone());
        layers.add(layer);
        midpoints.add(midpoint);

    }

    public static double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }

    public double meanDistance(){

        double sum = 0.0;

        for(int i = 0; i < distanceToPreviousLayer.size(); i++)
            sum += distanceToPreviousLayer.get(i);

        return sum / (double)distanceToPreviousLayer.size();

    }

}
