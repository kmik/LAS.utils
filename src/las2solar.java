import LASio.LASReader;
import org.gdal.ogr.ogr;
import tools.createCHM;
import tools.las2solar_photogrammetry;
import utils.argumentReader;
import java.io.File;
import java.io.IOException;
import static utils.miscProcessing.prepareData;

public class las2solar {
    public static void main(String[] args) throws IOException {
        ogr.RegisterAll();
        argumentReader aR = new argumentReader(args);
        prepareData(aR, "las2solar");
        // Each file uses aR.cores for its voxel calculations. Metadata is not shared concurrently.
        for (File input : aR.inputFiles) {
            try {
                LASReader cloud = new LASReader(input);
                createCHM creator = new createCHM();
                createCHM.chm chm = creator.new chm(cloud, "y", 1, aR, 1);
                if (aR.mode_3d) new las2solar_photogrammetry(chm.outputFileName, aR, cloud, false);
                else new las2solar_photogrammetry(chm.outputFileName, aR, cloud);
            } catch (Exception e) {
                throw new IOException("Solar calculation failed for " + input, e);
            }
        }
    }
}
