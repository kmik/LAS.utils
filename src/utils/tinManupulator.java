package utils;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.interpolation.VertexValuatorDefault;
import org.tinfour.utils.Polyside;
import tools.GaussianSmooth;

import java.io.File;

import static org.tinfour.utils.Polyside.isPointInPolygon;

public class tinManupulator {

    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
    VertexValuatorDefault valuator = new VertexValuatorDefault();

    argumentReader aR;

    TriangularFacetInterpolator polator = new TriangularFacetInterpolator(tin);
    public double minx = 0, miny = 0, minz = 0, maxx = 0, maxy = 0, maxz = 0;
    public tinManupulator(argumentReader aR){

        this.aR = aR;
    }

    public synchronized void addPointToTin(double x, double y, double z){

        tin.add(new org.tinfour.common.Vertex(x, y, z));

        //System.out.println(tin.getVertices().size());
    }

    public void prepareTin(){
        polator.resetForChangeToTin();
    }

    public void writeTinToFile(File outputFile, double resolution){

        polator.resetForChangeToTin();

        int numberOfPixelsX = (int) Math.ceil((maxx - minx) / resolution);
        int numberOfPixelsY = (int) Math.ceil((maxy - miny) / resolution);

        System.out.println(numberOfPixelsX + " " + numberOfPixelsY);

        System.out.println(minx + " " + miny + " " + maxx + " " + maxy);
        double[] geoTransform = new double[]{minx, resolution, 0.0, maxy, 0.0, -resolution};

        SpatialReference sr = new SpatialReference();

        sr.ImportFromEPSG(aR.EPSG);



        gdal.AllRegister();

        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();
        String outputFileName = null;
        try {
            outputFileName = aR.createOutputFileWithExtension(outputFile, "_correctionRaster.tif").getAbsolutePath();
        }catch (Exception e){
            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
            return;
        }

        Dataset dataset_output = null;
        Band band = null;

        try {
            dataset_output = driver.Create(outputFileName, numberOfPixelsX, numberOfPixelsY, 1, gdalconst.GDT_Float32);
            band =  dataset_output.GetRasterBand(1);
        }catch (Exception e){
            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
            return;
        }

        dataset_output.SetProjection(sr.ExportToWkt());
        dataset_output.SetGeoTransform(geoTransform);

        double[][] smoothed = new double[numberOfPixelsX][numberOfPixelsY];


        for (int j = 0; j < numberOfPixelsX; j++) {
            for (int k = 0; k < numberOfPixelsY; k++) {

                float interpolatedValue = (float)polator.interpolate(minx + j * resolution + resolution / 2.0, maxy - k * resolution - resolution / 2.0, valuator);

                float[] outValue = new float[]{interpolatedValue};

                smoothed[j][k] = interpolatedValue;

                //band.WriteRaster(j, k, 1, 1, outValue);
            }
        }

        if(aR.theta != 0.0)
            aR.kernel = (int)( 2.0 * Math.ceil( 3.0 * aR.theta) + 1.0);

        //System.out.println("Kernel: " + aR.kernel + " theta: " + aR.theta);

        smoothed = GaussianSmooth.smooth(smoothed, numberOfPixelsX, numberOfPixelsY, aR.kernel, aR.theta);  // THIS IS GOOD!!!! :)

        double sum = 0.0;
        double count = 0;

        for (int j = 0; j < numberOfPixelsX; j++) {
            for (int k = 0; k < numberOfPixelsY; k++) {

                float[] outValue = new float[]{(float)smoothed[j][k]};

                band.WriteRaster(j, k, 1, 1, outValue);

                if(!Float.isNaN(outValue[0])){
                    sum += outValue[0];
                    count++;
                }

            }
        }

        double averageCorrection = sum / count;

        //System.out.println("AVERAGE: " + sum / count);
        band.FlushCache();
        dataset_output.delete();


    }

    public synchronized double interpolate(double x, double y){

        return polator.interpolate(x, y, valuator);

    }

    public synchronized boolean isPointInTin(double x, double y){

        return isPointInPolygon(tin.getPerimeter(), x, y) == Polyside.Result.Inside;

    }
}
