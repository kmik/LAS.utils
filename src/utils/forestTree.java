package utils;

import java.util.Arrays;

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

    double[] geometricCenter = new double[2];
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
        getCentroid(this.treeCrownBounds);
        System.out.println("Geometric center: " + Arrays.toString(this.geometricCenter));
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

    public static void print2DArray(double[][] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                System.out.print(arr[i][j] + " ");
            }
            System.out.println();
        }
    }

    public boolean containsCoordinate(double x, double y){

        double[] point = new double[2];
        point[0] = x;
        point[1] = y;

        //System.out.println(Arrays.toString(point));
        return pointInPolygon(point, treeCrownBounds);

    }

    public void setPlotID(int plotID) {
        this.plotID = plotID;
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

    public void getCentroid(double[][] polygon) {
        double centerX = 0;
        double centerY = 0;
        double signedArea = 0;
        double x0 = 0;
        double y0 = 0;
        double x1 = 0;
        double y1 = 0;
        double a = 0;

        for (int i = 0; i < polygon.length - 1; i++) {
            x0 = polygon[i][0];
            y0 = polygon[i][1];
            x1 = polygon[i + 1][0];
            y1 = polygon[i + 1][1];
            a = x0 * y1 - x1 * y0;
            signedArea += a;
            centerX += (x0 + x1) * a;
            centerY += (y0 + y1) * a;
        }

        x0 = polygon[polygon.length - 1][0];
        y0 = polygon[polygon.length - 1][1];
        x1 = polygon[0][0];
        y1 = polygon[0][1];
        a = x0 * y1 - x1 * y0;
        signedArea += a;
        centerX += (x0 + x1) * a;
        centerY += (y0 + y1) * a;

        signedArea *= 0.5;
        centerX /= (6 * signedArea);
        centerY /= (6 * signedArea);

        this.geometricCenter[0] = centerX;
        this.geometricCenter[1] = centerY;
    }
}
