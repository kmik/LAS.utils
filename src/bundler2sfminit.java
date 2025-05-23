import LASio.LASReader;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;
import utils.parseBundler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static utils.miscProcessing.prepareData;

public class bundler2sfminit {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        parseBundler pB = new parseBundler(aR);
        pB.setBundlerFile(aR.inputFiles.get(0));
        pB.parseBundlerFile();

        aR.cleanup();

    }

}
