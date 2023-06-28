package utils;

import java.util.HashMap;

public class SurveyBlock {

    public int id;
    public String name;
    public String description;

    public double min_x = Double.POSITIVE_INFINITY;
    public double min_y = Double.POSITIVE_INFINITY;
    public double max_x = Double.NEGATIVE_INFINITY;
    public double max_y = Double.NEGATIVE_INFINITY;

    public HashMap<Integer, SurveyBlock> neighbors = new HashMap<Integer, SurveyBlock>();

    public SurveyBlock(){

    }

    public void addNeighbor(SurveyBlock neighbor){
        neighbors.put(neighbor.id, neighbor);
    }

}
