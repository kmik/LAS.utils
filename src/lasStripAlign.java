import LASio.LASReader;
import tools.ToShp;
import tools.lasStrip;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class lasStripAlign {


    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "lasStripAlign");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        lasStrip tooli = new lasStrip(aR);
        tooli.align();


    }

}
