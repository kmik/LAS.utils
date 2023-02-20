import LASio.LASReader;
import LASio.LasPoint;
import com.clust4j.algo.DBSCAN;
import com.clust4j.algo.DBSCANParameters;
import com.clust4j.algo.MeanShift;
import com.clust4j.algo.MeanShiftParameters;
import com.clust4j.data.DataSet;
import com.clust4j.data.ExampleDataSets;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.gdal.gdal.gdal;
import org.gdal.ogr.ogr;
import tools.neuralNetworkHyperparameterOptimization;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import utils.miscProcessing;
import tools.plotAugmentator;
import static runners.RunLASutils.*;
import static utils.miscProcessing.prepareData;

public class plotAugment {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();


            plotAugmentator testi = new plotAugmentator(aR);
            testi.readShapeFiles(aR.poly, aR.poly_2);

            if(aR.poly_3.length > 0)
                testi.readSmallerPolygon(aR.poly_3);

            if(aR.target != null)
                testi.readTargets(aR.target);

            testi.readMeasuredTrees(aR.measured_trees);
            testi.readMatchedTrees(aR.ITC_metrics_file);
            testi.readITCPolygons(aR.poly_2);
            testi.preparePlots();
            testi.simulatePlots();

    }
}
