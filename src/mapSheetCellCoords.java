import LASio.LASReader;
import tools.process_las2las;
import utils.KarttaLehtiJako;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class mapSheetCellCoords {

    static double minX_finnishMap6k = 20000.0;
    static double maxY_finnishMap6k = 7818000.0;

    static double grid_x_size_MML = 45033;

    static double cellSizeVMI = 16.0;
    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        //fileDistributor fD = new fileDistributor(aR.inputFiles);

        String mapsheetId = aR.mapSheetId;

        KarttaLehtiJako klj = new KarttaLehtiJako();

        try {
            klj.readFromFile(null);
        }
        catch (Exception e){
            e.printStackTrace();
        }


        double[] mapSheetExtent = klj.getMapSheetExtentByName(mapsheetId);

        if(mapSheetExtent == null){
            System.out.println("Map sheet not found: " + mapsheetId);
            System.exit(1);
        }

        int x_iter = (int) ((mapSheetExtent[1] - mapSheetExtent[0]) / cellSizeVMI);
        int y_iter = (int) ((mapSheetExtent[3] - mapSheetExtent[2]) / cellSizeVMI);

        File outputFile = new File(aR.output);

        BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(outputFile));

        // Write header
        bw.write("id\tx\ty\n");

        for(int x = 0; x < x_iter; x++)
        {
            for(int y = 0; y < y_iter; y++)
            {
                double cellCenterX = mapSheetExtent[0] + (x * cellSizeVMI) + (cellSizeVMI / 2);
                double cellCenterY = mapSheetExtent[2] + (y * cellSizeVMI) + (cellSizeVMI / 2);

                int cellColumn = (int) ((cellCenterX - minX_finnishMap6k) / cellSizeVMI);
                int cellRow = (int) ((maxY_finnishMap6k - cellCenterY) / cellSizeVMI);

                long cellId = cellRow * (long) grid_x_size_MML + cellColumn;

                // decode cellId back to row and column
                int decodedColumn = (int) (cellId % grid_x_size_MML);
                int decodedRow = (int) (cellId / grid_x_size_MML);

                double decodedCenterXCoordinate = minX_finnishMap6k + (decodedColumn * cellSizeVMI) + (cellSizeVMI / 2);
                double decodedCenterYCoordinate = maxY_finnishMap6k - (decodedRow * cellSizeVMI) - (cellSizeVMI / 2);


                System.out.println("Cell row: " + cellRow + ", Cell column: " + cellColumn + ", Cell ID: " + cellId + ", Center X: " + cellCenterX + ", Center Y: " + cellCenterY);
                System.out.println("Decoded row: " + decodedRow + ", Decoded column: " + decodedColumn + ", Decoded ID: " + (decodedRow * (long) grid_x_size_MML + decodedColumn));
                System.out.println("Decoded Center X: " + decodedCenterXCoordinate + ", Decoded Center Y: " + decodedCenterYCoordinate);

                System.out.println("---------------------------------------");
                // Write to file
                bw.write(cellId + "\t" + cellCenterX + "\t" + cellCenterY + "\n");

            }
        }

        bw.close();
        //System.out.println(Arrays.toString(mapSheetExtent));

        aR.cleanup();


        // double grid_cell_id = (y + VMI_maxIndexY) * grid_x_size_MML + (x + VMI_minIndexX);

        // int x = Math.min((int) ((tempPoint.x - this.minX) / resolution), grid_x_size - 1);
        // int y = Math.min((int) ((this.maxY - tempPoint.y) / resolution), grid_y_size - 1);
    }





}
