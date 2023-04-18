package tools;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.ogr;
import org.opencv.core.Mat;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

class gdalE {

    public static void hei(String filename, Mat in){

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        Dataset dataset = null;
        Driver driver = null;
        Band band = null;


        int METHOD_DBB = 1;
        int METHOD_JAVA_ARRAYS = 2;
        int method = 2;


        int xsize = in.width();
        int ysize = in.height();
        int nbIters = 1;
        driver = gdal.GetDriverByName("GTiff");

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * xsize);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        int[] intArray = new int[xsize];
        float[] floatArray = new float[xsize];
        //System.out.println(filename);
        dataset = driver.Create(filename, xsize, ysize, 1, gdalconst.GDT_Float32);
        band = dataset.GetRasterBand(1);

        for (int iter = 0; iter < nbIters; iter++)
        {
            if (method == METHOD_DBB)
            {
                for (int i = 0; i < ysize; i++)
                {
                    for (int j = 0; j < xsize; j++)
                    {
                        floatBuffer.put(j, (float) (i + j));
                    }
                    band.WriteRaster_Direct(0, i, xsize, 1, gdalconst.GDT_Float32, byteBuffer);
                }
            }
            else
            {
                for (int i = 0; i < ysize; i++)
                {
                    for (int j = 0; j < xsize; j++)
                    {
                        floatArray[j] = (float) in.get(i, j)[0];
                    }
                    band.WriteRaster(0, i, xsize, 1, floatArray);
                }
            }
        }

        dataset.delete();
    }

    public static void hei(String filename, double[][] in){

        ogr.RegisterAll();
        gdal.AllRegister();

        Dataset dataset = null;
        Driver driver = null;
        Band band = null;


        int METHOD_DBB = 1;
        int METHOD_JAVA_ARRAYS = 2;
        int method = 2;


        int xsize = in.length;
        int ysize = in[0].length;

        int nbIters = 1;
        driver = gdal.GetDriverByName("GTiff");

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * xsize);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        int[] intArray = new int[xsize];
        float[] floatArray = new float[xsize];
        dataset = driver.Create(filename, xsize, ysize, 1, gdalconst.GDT_Float32);
        band = dataset.GetRasterBand(1);

        for (int iter = 0; iter < nbIters; iter++){
            if (method == METHOD_DBB){
                for (int i = 0; i < ysize; i++){
                    for (int j = 0; j < xsize; j++){
                        floatBuffer.put(j, (float) (i + j));
                    }
                    band.WriteRaster_Direct(0, i, xsize, 1, gdalconst.GDT_Float32, byteBuffer);
                }
            }
            else{
                for (int i = 0; i < ysize; i++){
                    for (int j = 0; j < xsize; j++){
                        floatArray[j] = (float) in[j][i];// in.get(i, j)[0];
                        //System.out.println(in[j][i]);
                    }
                    band.WriteRaster(0, i, xsize, 1, floatArray);
                }
            }
        }

        dataset.delete();
    }

    public static void hei(String filename, float[][] in){

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        Dataset dataset = null;
        Driver driver = null;
        Band band = null;


        int METHOD_DBB = 1;
        int METHOD_JAVA_ARRAYS = 2;
        int method = 2;


        int xsize = in.length;
        int ysize = in[0].length;

        int nbIters = 1;
        driver = gdal.GetDriverByName("GTiff");

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * xsize);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        int[] intArray = new int[xsize];
        float[] floatArray = new float[xsize];
        //System.out.println(filename);
        dataset = driver.Create(filename, xsize, ysize, 1, gdalconst.GDT_Float32);
        band = dataset.GetRasterBand(1);

        band.SetNoDataValue(Float.NaN);

        for (int iter = 0; iter < nbIters; iter++){
            if (method == METHOD_DBB){
                for (int i = 0; i < ysize; i++){
                    for (int j = 0; j < xsize; j++){
                        floatBuffer.put(j, (float) (i + j));
                    }
                    band.WriteRaster_Direct(0, i, xsize, 1, gdalconst.GDT_Float32, byteBuffer);
                }
            }
            else{
                for (int i = 0; i < ysize; i++){
                    for (int j = 0; j < xsize; j++){
                        floatArray[j] = in[j][i];// in.get(i, j)[0];
                        //System.out.println(in[j][i]);
                    }
                    band.WriteRaster(0, i, xsize, 1, floatArray);
                }
            }
        }

        dataset.delete();
    }

    public static Dataset hei(String filename, int xDim, int yDim, float value){
/*
        File file = new File(filename);
        if(file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        }catch (Exception e ){
            e.printStackTrace();
        }

 */
        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        Dataset dataset = null;
        Driver driver = null;
        Band band = null;


        int METHOD_DBB = 1;
        int METHOD_JAVA_ARRAYS = 2;
        int method = 2;


        int xsize = yDim;
        int ysize = xDim;

        int nbIters = 1;

        driver = gdal.GetDriverByName("GTiff");

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * xsize);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();

        int[] intArray = new int[xsize];
        float[] floatArray = new float[xsize];

        //int a = gdalconst.GDT_Float32 == 1 ? 1 : 2;

        //System.out.println(filename);
        dataset = driver.Create(filename, xsize, ysize, 1, gdalconst.GDT_Float32);
        band = dataset.GetRasterBand(1);
        band.SetNoDataValue(Float.NaN);

        for (int iter = 0; iter < nbIters; iter++){
            if (method == METHOD_DBB){
                for (int i = 0; i < ysize; i++){
                    for (int j = 0; j < xsize; j++){
                        floatBuffer.put(j, (float) (i + j));
                    }
                    band.WriteRaster_Direct(0, i, xsize, 1, gdalconst.GDT_Float32, byteBuffer);
                }
            }
            else{
                for (int i = 0; i < ysize; i++){
                    for (int j = 0; j < xsize; j++){
                        floatArray[j] = value;// in.get(i, j)[0];
                        //System.out.println(in[j][i]);
                    }
                    band.WriteRaster(0, i, xsize, 1, floatArray);
                }
            }
        }
        //dataset.delete();
        return dataset;

    }

    public static Dataset hei(String filename, int xDim, int yDim, float value, int layers){

        //ogr.RegisterAll(); //Registering all the formats..
        //gdal.AllRegister();

        Dataset dataset = null;
        Driver driver = null;
        Band band = null;


        int METHOD_DBB = 1;
        int METHOD_JAVA_ARRAYS = 2;
        int method = 2;


        int xsize = yDim;
        int ysize = xDim;

        int nbIters = 1;

        driver = gdal.GetDriverByName("GTiff");

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * xsize);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();

        int[] intArray = new int[xsize];
        float[] floatArray = new float[xsize];

        //int a = gdalconst.GDT_Float32 == 1 ? 1 : 2;

        //System.out.println(filename);
        dataset = driver.Create(filename, xsize, ysize, layers, gdalconst.GDT_Float32);

        for(int i_ = 1; i_ <= layers; i_++) {
            band = dataset.GetRasterBand(i_);
            if(i_ == 1)
            band.SetNoDataValue(Float.NaN);

            for (int iter = 0; iter < nbIters; iter++) {
                if (method == METHOD_DBB) {
                    for (int i = 0; i < ysize; i++) {
                        for (int j = 0; j < xsize; j++) {
                            floatBuffer.put(j, (float) (i + j));
                        }
                        band.WriteRaster_Direct(0, i, xsize, 1, gdalconst.GDT_Float32, byteBuffer);
                    }
                } else {
                    for (int i = 0; i < ysize; i++) {
                        for (int j = 0; j < xsize; j++) {
                            floatArray[j] = value;// in.get(i, j)[0];
                            //System.out.println(in[j][i]);
                        }
                        band.WriteRaster(0, i, xsize, 1, floatArray);
                    }
                }
            }
        }
        //dataset.delete();
        return dataset;

    }

    public static void hei(String filename, ArrayList<float[][]> in, int layers){

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        Dataset dataset = null;
        Driver driver = null;
        Band band = null;


        int METHOD_DBB = 1;
        int METHOD_JAVA_ARRAYS = 2;
        int method = 2;


        int xsize = in.get(0).length;
        int ysize = in.get(0)[0].length;

        int nbIters = 1;
        driver = gdal.GetDriverByName("GTiff");

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * xsize);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        int[] intArray = new int[xsize];
        float[] floatArray = new float[xsize];
        //System.out.println(filename);
        dataset = driver.Create(filename, xsize, ysize, layers, gdalconst.GDT_Float32);

        for(int k = 1; k <= layers; k++ ){

            band = dataset.GetRasterBand(k);

            band.SetNoDataValue(Float.NaN);

            for (int iter = 0; iter < nbIters; iter++){
                if (method == METHOD_DBB){
                    for (int i = 0; i < ysize; i++){
                        for (int j = 0; j < xsize; j++){
                            floatBuffer.put(j, (float) (i + j));
                        }
                        band.WriteRaster_Direct(0, i, xsize, 1, gdalconst.GDT_Float32, byteBuffer);
                    }
                }
                else{
                    for (int i = 0; i < ysize; i++){
                        for (int j = 0; j < xsize; j++){
                            floatArray[j] = in.get(k - 1)[j][i];// in.get(i, j)[0];
                            //System.out.println(in[j][i]);
                        }
                        band.WriteRaster(0, i, xsize, 1, floatArray);
                    }
                }
            }
        }
        dataset.delete();
    }

}