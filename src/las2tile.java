import LASio.LASReader;
import tools.Tiler;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static utils.miscProcessing.prepareData;

public class las2tile {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2tile");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        try {
            Tiler tile = new Tiler(aR.inputFiles, aR);
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
        catch (Error e) {
            e.printStackTrace();
            System.exit(2);
        }


    }
}
