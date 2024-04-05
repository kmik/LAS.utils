package tools;

import LASio.LASReader;
import LASio.LasPoint;
import err.toolException;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.ogr;
import org.gdal.osr.SpatialReference;
import org.tinfour.common.IIncrementalTinNavigator;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import org.tinfour.interpolation.IVertexValuator;
import org.tinfour.interpolation.NaturalNeighborInterpolator;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.interpolation.VertexValuatorDefault;
import org.tinfour.standard.IncrementalTin;
import utils.argumentReader;
import utils.fileOperations;

import java.util.List;

public class ground2raster {

    public fileOperations fo = new fileOperations();


    argumentReader aR;
    LASReader pointCloud;
    IncrementalTin tin = new IncrementalTin();

    public ground2raster(LASReader pointCloud, argumentReader aR, int coreNumber) throws Exception{

        IIncrementalTinNavigator navi = tin.getNavigator();
        SimpleTriangle triang = null;


        LasPoint tempPoint = new LasPoint();

        double true_min_x = Double.POSITIVE_INFINITY, true_min_y = Double.POSITIVE_INFINITY;
        double true_max_x = Double.NEGATIVE_INFINITY, true_max_y = Double.NEGATIVE_INFINITY;

        int thread_n = aR.pfac.addReadThread(pointCloud);

        for (int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(aR.min_edge_length > 0 && tin.isBootstrapped()) {

                    List<Vertex> closest = tin.getNeighborhoodPointsCollector().collectNeighboringVertices(tempPoint.x, tempPoint.y, 0, 0);

                    if (closest.size() > 0) {

                        double distToClosest = Double.POSITIVE_INFINITY;
                        double dist = Math.sqrt(Math.pow(closest.get(0).getX() - tempPoint.x, 2) + Math.pow(closest.get(0).getY() - tempPoint.y, 2));

                        if(dist < aR.min_edge_length)
                            continue;
                    }

                    if (tempPoint.classification == 2) {
                        tin.add(new Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                    }
                }
                if(tempPoint.classification == 2){

                    tin.add(new Vertex(tempPoint.x, tempPoint.y, tempPoint.z));

                }

                if (!aR.inclusionRule.ask(tempPoint, i + j, true)) {
                    continue;
                }

                if(tempPoint.x < true_min_x)
                    true_min_x = tempPoint.x;
                if(tempPoint.x > true_max_x)
                    true_max_x = tempPoint.x;
                if(tempPoint.y < true_min_y)
                    true_min_y = tempPoint.y;
                if(tempPoint.y > true_max_y)
                    true_max_y = tempPoint.y;


            }
        }

        double minx = pointCloud.getMinX();
        double maxx = pointCloud.getMaxX();
        double miny = pointCloud.getMinY();
        double maxy = pointCloud.getMaxY();

        double cell_size = aR.step;

        int grid_x_size = (int)Math.ceil((true_max_x - true_min_x) / cell_size);
        int grid_y_size = (int)Math.ceil((true_max_y - true_min_y) / cell_size);

        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();

        String outputFileName = aR.createOutputFileWithExtension(pointCloud.getFile(), "_terrain.tif").getAbsolutePath();


        //Dataset dataset = driver.Create("tmp.tif", grid_x_size, grid_x_size, 1, gdalconst.GDT_Float32);
        Dataset dataset_output = null;
        Band band = null;

        try {
            dataset_output = driver.Create(outputFileName, grid_x_size, grid_y_size, 1, gdalconst.GDT_Float32);
            band =  dataset_output.GetRasterBand(1);
        }catch (Exception e){
            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
            return;
        }
        //Band band = dataset.GetRasterBand(1);


        VertexValuatorDefault valuator = new VertexValuatorDefault();

        TriangularFacetInterpolator polator = new TriangularFacetInterpolator(tin);

        band.SetNoDataValue(Float.NaN);

        double[] geoTransform = new double[]{true_min_x, aR.step, 0.0, true_max_y, 0.0, -aR.step};

        SpatialReference sr = new SpatialReference();

        sr.ImportFromEPSG(aR.EPSG);

        dataset_output.SetProjection(sr.ExportToWkt());
        dataset_output.SetGeoTransform(geoTransform);

        float[] outValue = new float[grid_x_size];

        for(int y = 0; y < grid_y_size; y++) {

            for(int x = 0; x < grid_x_size; x++){

                double interpolatedValue = polator.interpolate(true_min_x + x * aR.step + aR.step/2.0, true_max_y - y * aR.step - aR.step/2.0, valuator);



                if(Double.isNaN(interpolatedValue)){

                    // Search for the closest 3 vertices and calculate the weighted average based on euclidean distance
                    List<Vertex> closest = tin.getNeighborhoodPointsCollector().collectNeighboringVertices(true_min_x + x * aR.step + aR.step/2.0, true_max_y - y * aR.step - aR.step/2.0, 0, 0);



                    if (closest.size() > 0) {
                        double sum = 0;
                        double sumWeights = 0;

                        int count = 0;

                        for (Vertex v : closest) {
                            double dist = Math.sqrt(Math.pow(v.getX() - (true_min_x + x * aR.step + aR.step/2.0), 2) + Math.pow(v.getY() - (true_max_y - y * aR.step - aR.step/2.0), 2));

                            // Inverse distance weighting: accumulate values inversely proportional to distance
                            if (dist != 0) { // Avoid division by zero
                                sum += v.getZ() / dist; // Divide by distance instead of multiplying
                                sumWeights += 1 / dist; // Accumulate inverse distance
                            }

                            if(count++ > 2)
                                break;
                        }
                        interpolatedValue = sum / sumWeights; // Divide by sum of inverse distances
                    }

                }
                outValue[x] = (float)interpolatedValue;

            }
            band.WriteRaster(0, y, grid_x_size, 1, outValue);

        }

        band.FlushCache();
        //band2.FlushCache();

        dataset_output.delete();

    }
}
