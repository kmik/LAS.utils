package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.PointInclusionRule;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import utils.argumentReader;
import utils.fileOperations;

import java.io.File;
import java.io.IOException;
/**
 *	Converts a LAS file into an ESRI format
 *	shp file.
 *
 *  @author  Kukkonen Mikko
 *  @version 0.1
 *  @since 10.03.2018
 */
public class ToShp{

    fileOperations fo = new fileOperations();
    File outputFile;
    PointInclusionRule rule = new PointInclusionRule();
    LASReader pointCloud;// = new LASReader();
    String odir;

    String otype = "shp";

    String outputfilename = "";

    String pathSep = System.getProperty("file.separator");

    argumentReader aR;

    public ToShp(){

    }

    /**
     *	Constructor
     *
     *	@param outFile 				Name of the output file
     *	@param cloud 				Input point cloud
     *	@param ruleIn				Point inclusion rule
     *	@param odirIn 				Output directory
     *	@param oparse				Indicates which LAS point attributes
     *								are included in the shapefile.
     */

    public ToShp(String outFile, LASReader cloud, PointInclusionRule ruleIn, String odirIn, String oparse, argumentReader aR) throws Exception {

        this.aR = aR;

        this.pointCloud = cloud;
        this.rule = ruleIn;

        this.odir = odirIn;

        outputfilename = outFile;

        if(outFile.equals("asd")){
            outputfilename = cloud.getFile().getName();
        }


        if(!odir.equals("asd")){
            outputfilename = odir + pathSep + outputfilename;
        }else{
            outputfilename = cloud.getFile().getParent() + pathSep + outputfilename;
        }

        outputfilename = fo.createNewFileWithNewExtension(outputfilename, "."+otype).getAbsolutePath();
        // outputfilename.replaceFirst("[.][^.]+$", "") + "." + otype;

        System.out.println(outputfilename);

        this.outputFile = new File(outputfilename);

        if(outputFile.exists()){
            //outputFile = cloud.getFile().getAbsolutePath().replaceFirst("[.][^.]+$", "") + "_1.las";
            outputfilename = fo.createNewFileWithNewExtension(outputFile, "_1." + otype).getAbsolutePath()  ;//

            // outputFile.getAbsolutePath().replaceFirst("[.][^.]+$", "") + "_1." + otype;
            outputFile = new File(outputfilename);
        }

        export();

    }

    /** Export LAS file as shp */

    public void export() throws Exception{

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        String out_file = outputfilename;
        String driverName = "ESRI Shapefile";

        String[]  split2 = out_file.split("/.");
        String out_name = split2[0];


        Driver shpDriver;
        shpDriver = ogr.GetDriverByName(driverName);
        DataSource outShp;
        outShp = shpDriver.CreateDataSource(out_file);
        Layer outShpLayer = outShp.CreateLayer(out_name, null, 1);
        FieldDefn layerFieldDef = new FieldDefn("z",2);
        outShpLayer.CreateField(layerFieldDef);
        FeatureDefn outShpFeatDefn=outShpLayer.GetLayerDefn();
        Feature outShpFeat = new Feature(outShpFeatDefn);

        long n = pointCloud.getNumberOfPointRecords();

        LasPoint tempPoint = new LasPoint();

        int thread_n = aR.pfac.addReadThread(pointCloud);

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                Geometry outShpGeom = new Geometry(1);
                outShpGeom.SetPoint(0, tempPoint.x, tempPoint.y);
                outShpFeat.SetField("z", tempPoint.z);
                outShpFeat.SetGeometryDirectly(outShpGeom);
                outShpLayer.CreateFeature(outShpFeat);


            }

        }

        outShpLayer.SyncToDisk();
    }

}