package utils;

import err.toolException;
import tools.plotAugmentator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class forestPlot {

    plotAugmentator pA;
    int id;
    double[][] plotBounds;
    int plotID;
    double[][] plotITCTreeBounds;

    double area;
    HashMap<Integer, forestTree> trees = new HashMap<Integer, forestTree>();

    HashSet<Integer> treeHasITC = new HashSet<Integer>();
    HashSet<Integer> ITCWithoutMatch = new HashSet<Integer>();

    HashSet<Integer> treeWithoutITC = new HashSet<Integer>();
    public forestPlot(){
    }

    public forestPlot( int id,  double[][] plotBounds, plotAugmentator pA){
        this.id = id;
        this.pA = pA;
        this.plotBounds = plotBounds;
    }

    public void addTree(forestTree tree){
        trees.put(tree.getTreeID(), tree);
        tree.setPlotID(this.id);

    }

    public void hasITCid(int treeID){
        treeHasITC.add(treeID);
        //System.out.println(pA.getTreeBounds().get(treeID)[0][0]);
    }

    // print 2d array

    public void preparePlot(){

        for(int treeID : trees.keySet()){
            if(!treeHasITC.contains(treeID)){
                treeWithoutITC.add(treeID);
            }
        }
    }

    public void ITCHasNoMatch(int ITCid){
        ITCWithoutMatch.add(ITCid);
        //System.out.println("plot " + this.id + " " + treeWithoutITC.size());
    }
    public forestTree getTree(int treeID){

        if(!trees.containsKey(treeID))
            throw new toolException("Tree " + treeID + " not found in plot " + this.id);

        return trees.get(treeID);
    }

    public boolean pointInPlot(double[] point ){


        if(pointInPolygon(point, this.plotBounds))
            return true;

        return false;
    }

    public void findOmittedTrees(){
        for(int treeID : trees.keySet()){
            if(!treeHasITC.contains(treeID)){


            }
        }
    }

    public double[][] getPlotBounds() {
        return plotBounds;
    }

    public void setPlotBounds(double[][] plotBounds) {
        this.plotBounds = plotBounds;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }


    public static boolean pointInPolygon(double[] point, double[][] poly) {


        int numPolyCorners = poly.length;
        int j = numPolyCorners - 1;
        boolean isInside = false;

        for (int i = 0; i < numPolyCorners; i++) {
            if (poly[i][1] < point[1] && poly[j][1] >= point[1] || poly[j][1] < point[1] && poly[i][1] >= point[1]) {
                if (poly[i][0] + (point[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) * (poly[j][0] - poly[i][0]) < point[0]) {
                    isInside = !isInside;
                }
            }
            j = i;
        }
        return isInside;
    }

}
