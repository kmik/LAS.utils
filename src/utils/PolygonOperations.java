package utils;

import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
public class PolygonOperations {


    public PolygonOperations(){

    }


    public static Geometry clipGeometry(Geometry geometryA, Geometry geometryB) {
        ogr.RegisterAll();

        // Create output data source
        Driver driver = ogr.GetDriverByName("ESRI Shapefile");
        DataSource outputDataSource = driver.CreateDataSource("");

        // Create the output layer
        FeatureDefn outputLayerDefn = new FeatureDefn("");
        org.gdal.ogr.Layer outputLayer = outputDataSource.CreateLayer("", geometryA.GetSpatialReference(), geometryA.GetGeometryType());

        // Get the difference between the two geometries
        Geometry differenceGeometry = geometryA.Difference(geometryB);

        // Check if the resulting geometry is not null and not empty
        if (differenceGeometry != null && !differenceGeometry.IsEmpty()) {
            // Create a new feature with the difference geometry
            Feature outputFeature = new Feature(outputLayerDefn);
            outputFeature.SetGeometry(differenceGeometry);

            // Add the output feature to the output layer
            outputLayer.CreateFeature(outputFeature);

            // Cleanup
            outputFeature.delete();
            outputDataSource.delete();
            System.out.println("RETURNING DIFFERENCE GEOMETRY");

            return differenceGeometry;
        } else {
            // Return an empty geometry or handle the case as needed
            System.out.println("RETURNING ORIGINAL GEOMETRY");
            return geometryA;
        }
    }



}
