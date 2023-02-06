package utils;

public class forestTree {

    int treeSpecies;
    int treeID;
    int treeID_unique;
    int treePlotID;

    int plotID;
    int treeITCid = -1;
    double treeDBH;
    double treeHeight;
    double treeHeight_ITC = -1;
    double treeCrownArea;
    double treeVolume;

    double[] volume_total = new double[3];

    double[][] treeCrownBounds;
    public boolean hasCrown = false;
    double treeX;
    double treeX_ITC;
    double treeY;
    double tree_Y_ITC;



    public forestTree(){

    }

    public int getTreeSpecies() {
        return treeSpecies;
    }

    public void setTreeSpecies(int treeSpecies) {
        this.treeSpecies = treeSpecies;
    }

    public int getTreeID() {
        return treeID;
    }

    public void setTreeID(int treeID) {
        this.treeID = treeID;
    }

    public int getTreePlotID() {
        return treePlotID;
    }

    public void setTreePlotID(int treePlotID) {
        this.treePlotID = treePlotID;
    }

    public int getTreeITCid() {
        return treeITCid;
    }

    public void setTreeITCid(int treeITCid) {
        this.treeITCid = treeITCid;
    }

    public double getTreeDBH() {
        return treeDBH;
    }

    public void setTreeDBH(double treeDBH) {
        this.treeDBH = treeDBH;
    }

    public double getTreeHeight() {
        return treeHeight;
    }

    public void setTreeHeight(double treeHeight) {
        this.treeHeight = treeHeight;
    }

    public double getTreeCrownArea() {
        return treeCrownArea;
    }

    public void setTreeCrownArea(double treeCrownArea) {
        this.treeCrownArea = treeCrownArea;
    }

    public double getTreeVolume() {
        return treeVolume;
    }

    public void setTreeVolume(double treeVolume) {
        this.treeVolume = treeVolume;
    }

    public double getTreeX() {
        return treeX;
    }

    public void setTreeX(double treeX) {
        this.treeX = treeX;
    }

    public double getTreeY() {
        return treeY;
    }

    public void setTreeY(double treeY) {
        this.treeY = treeY;
    }

    public double getTreeX_ITC() {
        return treeX_ITC;
    }

    public void setTreeX_ITC(double treeX_ITC) {
        this.treeX_ITC = treeX_ITC;
    }

    public double getTree_Y_ITC() {
        return tree_Y_ITC;
    }

    public void setTree_Y_ITC(double tree_Y_ITC) {
        this.tree_Y_ITC = tree_Y_ITC;
    }

    public int getTreeID_unique() {
        return treeID_unique;
    }

    public void setTreeID_unique(int treeID_unique) {
        this.treeID_unique = treeID_unique;
    }

    public double[] getVolume_total() {
        return volume_total;
    }

    public void setVolume_total(double[] volume_total) {
        this.volume_total = volume_total;
    }

    public double[][] getTreeCrownBounds() {
        return treeCrownBounds;
    }

    public void setTreeCrownBounds(double[][] treeCrownBounds) {
        this.hasCrown = true;
        this.treeCrownBounds = treeCrownBounds;
    }

    public double getTreeHeight_ITC() {
        return treeHeight_ITC;
    }

    public void setTreeHeight_ITC(double treeHeight_ITC) {
        this.treeHeight_ITC = treeHeight_ITC;
    }

    public int getPlotID() {
        return plotID;
    }

    public void setPlotID(int plotID) {
        this.plotID = plotID;
    }
}
