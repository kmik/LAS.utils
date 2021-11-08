package utils;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;

public class rasterManipulator {

    Band raster;
    float[] raster_write = new float[1];
    public rasterManipulator(Band raster){

        this.raster = raster;

    }

    public synchronized void setValue(int x, int y, float value){

        raster_write[0] = value;
        raster.WriteRaster(x, y, 1, 1, raster_write);

    }

}
