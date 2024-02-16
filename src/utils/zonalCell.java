package utils;

import org.gdal.ogr.Geometry;

import java.util.ArrayList;

public class zonalCell {

    int x, y;
    double area;
    double intersectArea;

    public boolean touchesHole = false;
    public ArrayList<Double> intersectAreas = new ArrayList<>();
    double minx, miny, maxx, maxy;

    public int id;

    ArrayList<float[]> cellExtent = new ArrayList<>();

    boolean border = false;

    ArrayList<ArrayList<float[]>> lineSegments = new ArrayList<>();
    ArrayList<ArrayList<float[]>> insidePoints = new ArrayList<>();
    ArrayList<ArrayList<float[][]>> outsidePoints = new ArrayList<>();
    public zonalCell(){

    }

    public zonalCell(int x, int y){
        this.x = x;
        this.y = y;
    }

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public void setExtent(double origox, double origoy, double cellsize){
        minx = origox + x * cellsize;
        miny = origoy + y * cellsize;
        maxx = origox + (x + 1) * cellsize;
        maxy = origoy + (y + 1) * cellsize;
    }

    public void addLineSegment(){
        lineSegments.add(new ArrayList<>());
    }

    public void addPoint(float[] point){
        lineSegments.get(lineSegments.size() - 1).add(point);
    }

    public void setCellExtent(ArrayList<float[]> cellExtent){
        this.cellExtent = cellExtent;
        for(float[] point : cellExtent){
            //System.out.println(point[0] + " " + point[1]);
        }
        //System.exit(1);
    }

    public boolean cellInPolygon(ArrayList<float[]> polygon){

        for(float[] point : cellExtent){
            if(pointInPolygon(polygon, point[0], point[1]))
                return true;
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

}
