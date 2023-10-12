package utils;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import java.lang.reflect.Array;
import java.util.Arrays;

public class gdalRaster {

    String filename = null;

    Dataset raster;
    double[] rasterExtent;

    double[] geoTransform = new double[6];

    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    double resolution;
    public gdalRaster() {

    }

    public gdalRaster(String filename) {
        this.filename = filename;
        this.readRaster(filename);
    }

    public void readRaster(String filename) {
        this.raster = (gdal.Open(filename, gdalconst.GA_ReadOnly));
    }

    public double[] rasterExtent() {

        double[] rasterExtent = new double[6];
        this.raster.GetGeoTransform(rasterExtent);


        this.raster.GetGeoTransform(geoTransform);

        double[] rasterWidthHeight_ = new double[2];
        rasterWidthHeight_[0] = raster.GetRasterXSize();
        rasterWidthHeight_[1] = raster.GetRasterYSize();

        this.resolution = rasterExtent[1];




        this.rasterExtent = new double[]{rasterExtent[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0],  rasterExtent[3]};

        return this.rasterExtent;

    }

    public boolean isOverlapping(double minx, double maxx, double miny, double maxy){

        if (minx > this.rasterExtent[2] || maxx < this.rasterExtent[0] || miny > this.rasterExtent[3] || maxy < this.rasterExtent[1]){
            return false;
        }
        else{
            return true;
        }

    }

    public void close(){
        this.raster.delete();
    }

    public void flush(){
        this.raster.FlushCache();
    }

    public void open(){
        this.open(this.filename);
    }
    public void open(String filename){
        this.raster = (gdal.Open(filename, gdalconst.GA_Update));
    }

    public double getMinX(){
        return this.rasterExtent[0];
    }

    public double getMaxX(){
        return this.rasterExtent[2];
    }

    public double getMinY(){
        return this.rasterExtent[1];
    }

    public double getMaxY(){
        return this.rasterExtent[3];
    }

    public double getResolution(){
        return this.resolution;
    }

    public void rasterToArray(float[][] array, double arrayMinx, double arrayMaxx, double arrayMiny, double arrayMaxy){

        Band tifBand = this.raster.GetRasterBand(1);
        int number_of_pix_x = this.raster.getRasterXSize();
        int number_of_pix_y = this.raster.getRasterYSize();

        float[] floatArray = new float[number_of_pix_x];

        System.out.println("Reading raster line by line");

        double diffx = this.getMinX() - arrayMinx;
        double diffy = this.getMaxY() - arrayMaxy;

        int offsetx = (int)(diffx / this.getResolution());
        int offsety = (int)(diffy / this.getResolution());


        long startTime = System.currentTimeMillis();

        for(int y = 0; y < number_of_pix_y; y++) {

            tifBand.ReadRaster(0, y, number_of_pix_x, 1, floatArray);

            for (int x = 0; x < number_of_pix_x; x++) {

                //System.out.println("line " + y);
                float value = floatArray[x];



                array[x + offsetx][y - offsety] = value;

            }
        }


    }
}
