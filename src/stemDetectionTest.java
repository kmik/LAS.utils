import java.io.File;
import java.io.IOException;
import LASio.*;
import tools.*;
import utils.argumentReader;

public class stemDetectionTest {



    public static void main(String[] args) throws Exception {

        //File lasFile = new File("/media/koomikko/LaCie/ITD_understorey/cloudData/las/delineated/kernel_40/3_ITD.las");

        File lasFile = new File(args[0]);
        LASReader las = new LASReader(lasFile);

        argumentReader aR = new argumentReader();

        /** THIS WAS USE IN KUNNONEN ET AL. 2020) */
        //stemDetector sd = new stemDetector(las, 0.10, 0.2, 1, aR);

        stemDetector sd = new stemDetector(las, 0.10, 0.2, 1, aR);

        sd.setUpOutputFiles(las);

        sd.detect(false);

    }
}
