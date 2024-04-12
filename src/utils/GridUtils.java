package utils;

import err.toolException;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.osr.SpatialReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class GridUtils {


    public File rasterFile = null;
    HashMap<Integer, ArrayList<Integer>> gridCellValues = new HashMap<>();
    argumentReader aR;
    short[][] grid;

    public double resolution;

    // Origin is the top left corner of the grid (top left corner of the bounding box, i.e., the top left corner of the top left cell).
    public double originX, originY;

    public ArrayList<double[]> seedPoints = new ArrayList<>();

    public HashMap<Integer, ArrayList<int[]>> featuresInMultipleCells = new HashMap<>();

    public GridUtils(argumentReader aR){
        this.aR = aR;
    }

    public void setResolution(double resolution){

        this.resolution = resolution;

    }

    public short[][] getGrid(){

        return this.grid;

    }

    public void setGrid(short[][] grid){

        this.grid = grid;

    }

    public void setGridByCloning(short[][] grid){

        this.grid = clone2DArray(grid);

    }

    public void setFeaturesInMultipleCells(HashMap<Integer, ArrayList<int[]>> featuresInMultipleCells){

        this.featuresInMultipleCells = featuresInMultipleCells;

    }

    public boolean featureInMultipleCells(int featureId){

        if(this.featuresInMultipleCells.containsKey(featureId)){
            return true;
        }

        return false;

    }

    public void setOrigin(double originX, double originY){

        this.originX = originX;
        this.originY = originY;

    }

    public void addValueToGridCell(double x, double y, int value){

        int gridX = (int)Math.floor((x - this.originX) / this.resolution);
        int gridY = (int)Math.floor((this.originY - y) / this.resolution);

        int gridCell = gridX + gridY * this.grid.length;

        if(!this.gridCellValues.containsKey(gridCell)){
            this.gridCellValues.put(gridCell, new ArrayList<>());
        }

        this.gridCellValues.get(gridCell).add(value);
    }

    public void addValueToGridCell(int x, int y, int value){

        int gridX = x;
        int gridY = y;

        int gridCell = gridX + gridY * this.grid.length;

        if(!this.gridCellValues.containsKey(gridCell)){
            this.gridCellValues.put(gridCell, new ArrayList<>());
        }

        this.gridCellValues.get(gridCell).add(value);
    }

    public ArrayList<Integer> getCellValues(int x, int y){

        int gridCell = x + y * this.grid.length;

        if(!this.gridCellValues.containsKey(gridCell)){
            return new ArrayList<>();
        }

        return this.gridCellValues.get(gridCell);

    }

    public boolean cellHasValue(int x, int y){

        int gridCell = x + y * this.grid.length;

        if(!this.gridCellValues.containsKey(gridCell)){
            return false;
        }

        return true;

    }

    public ArrayList<int[]> getCellsThatOverlapWithExtent(double[] bbox){

        ArrayList<int[]> cells = new ArrayList<>();

        int x1 = (int)Math.floor((bbox[0] - this.originX) / this.resolution);
        int y1 = (int)Math.floor((this.originY - bbox[3]) / this.resolution);

        int x2 = (int)Math.floor((bbox[2] - this.originX) / this.resolution);
        int y2 = (int)Math.floor((this.originY - bbox[1]) / this.resolution);

        for(int x = x1; x <= x2; x++){
            for(int y = y1; y <= y2; y++){
                int[] cell = {x, y};
                cells.add(cell);
            }
        }

        return cells;
    }


    public ArrayList<int[]> coordinatesToGridCoordinates(ArrayList<double[]> points){

        ArrayList<int[]> gridCoordinates = new ArrayList<>();

        for(int i = 0; i < points.size(); i++){

            double[] point = points.get(i);

            double x = point[0];
            double y = point[1];

            int gridX = (int)Math.floor((x - this.originX) / this.resolution);
            int gridY = (int)Math.floor((this.originY - y) / this.resolution);

            //System.out.println(this.originX + " " + this.originY + " " + x + " " + y + " " + gridX + " " + gridY + " " + this.resolution);
            int[] gridPoint = {gridX, gridY};

            gridCoordinates.add(gridPoint);

        }

        return gridCoordinates;

    }


    public void writeRasterOutput(String filename, short[][] grid) {

        double[] geoTransform = new double[]{this.originX, this.resolution, 0.0, this.originY, 0.0, -this.resolution};


        Dataset dataset_output = null;
        Band band = null;

        org.gdal.gdal.Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();

        rasterFile = aR._createOutputFile_("segments.tif");

        try {
            dataset_output = driver.Create(rasterFile.getAbsolutePath(), grid.length , grid[0].length, 1, gdalconst.GDT_Int32);
            band =  dataset_output.GetRasterBand(1);
        }catch (Exception e){
            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
            return;
        }

        dataset_output.SetGeoTransform(geoTransform);
        SpatialReference sr = new SpatialReference();

        sr.ImportFromEPSG(aR.EPSG);

        dataset_output.SetProjection(sr.ExportToWkt());

        int[] outValue = new int[grid.length];

        for(int y = 0; y < grid[0].length; y++) {

            for (int x = 0; x < grid.length; x++) {
                outValue[x] = (int)grid[x][y];
            }

            band.WriteRaster(0, y, grid.length, 1, outValue);

        }

        band.FlushCache();
        dataset_output.FlushCache();
        //band2.FlushCache();
        dataset_output.delete();

        //System.out.println("GOT HER! " + this.originX + " " + this.originY);
        // Write the grid to a raster file
    }

    public void polygonize(String pathToTif){

        Dataset hDataset = gdal.Open(pathToTif, gdalconstConstants.GA_ReadOnly);
        Band rasterBand = hDataset.GetRasterBand(1);
        //Band rasterBand_mask = hDataset.GetRasterBand(2);


        String driverName = "ESRI Shapefile";

        org.gdal.ogr.Driver shpDriver;
        shpDriver = ogr.GetDriverByName(driverName);

        // Export the grid to a raster file
        DataSource outShp;

        File ofile = aR._createOutputFile_("segments.shp");

        outShp = shpDriver.CreateDataSource(ofile.getAbsolutePath());

        Layer outShpLayer = outShp.CreateLayer("ahhaa");

        FieldDefn layerFieldDef = new FieldDefn("DN",4);
        outShpLayer.CreateField(layerFieldDef);

        gdal.Polygonize(rasterBand, rasterBand, outShpLayer, 0);

        outShp.FlushCache();
        outShp.delete();
    }

    public short[][] interpolate(String method, short naValue){
        // Interpolate the grid

        // Create a new grid
        short[][] newGrid = new short[this.grid.length][this.grid[0].length];

        int countNa = 0;

        for(int x = 0; x < this.grid.length; x++){
            for(int y = 0; y < this.grid[0].length; y++) {
                if(this.grid[x][y] == naValue){
                    countNa++;
                }
                newGrid[x][y] = this.grid[x][y];
            }
        }

        int iterations = 0;

        ArrayList<Integer> xcoordinatesToAlter = new ArrayList<>();
        ArrayList<Integer> ycoordinatesToAlter = new ArrayList<>();
        ArrayList<Short> values = new ArrayList<>();

        while(countNa > 0){

            xcoordinatesToAlter.clear();
            ycoordinatesToAlter.clear();
            values.clear();

            for(int x = 0; x < this.grid.length; x++){
                for(int y = 0; y < this.grid[0].length; y++) {
                    if(newGrid[x][y] == naValue){
                        // Interpolate the value
                        short median = neighborhoodMedian(x, y, 1, naValue, newGrid);

                        if(median != naValue) {

                            xcoordinatesToAlter.add(x);
                            ycoordinatesToAlter.add(y);
                            values.add(median);
                            countNa--;

                        }
                        //System.out.println("HERE");
                    }else{
                        // Otherwise, just copy the value
                        //newGrid[x][y] = this.grid[x][y];
                    }
                }
            }
            for(int i = 0; i < xcoordinatesToAlter.size(); i++){
                newGrid[xcoordinatesToAlter.get(i)][ycoordinatesToAlter.get(i)] = values.get(i);
            }

           // System.out.println("Iterations: " + iterations++ + " " + countNa);
        }

        int countNanew = 0;
        // Check that there indeed are no na values
        for(int x = 0; x < this.grid.length; x++){
            for(int y = 0; y < this.grid[0].length; y++) {
                if(newGrid[x][y] == naValue){
                    countNanew++;
                }
            }
        }

        if(countNanew > 0){
            throw new toolException("Interpolation failed! There are still " + countNanew + " NA values in the grid!");
        }
        return newGrid;
    }

    public short neighborhoodMedian(int x, int y, int radius, short naValue, short[][] grid){
        // Calculate the median of the neighborhood

        // Create a list of values
        ArrayList<Short> values = new ArrayList<>();

        int minCountForCalculation = 2;
        int countNotNa = 0;

        for(int i = -radius; i <= radius; i++){
            for(int j = -radius; j <= radius; j++){

                int x1 = x + i;
                int y1 = y + j;

                if(x1 < 0 || x1 >= grid.length || y1 < 0 || y1 >= grid[0].length){
                    continue;
                }

                if(grid[x1][y1] == naValue){
                    continue;
                }


                values.add(grid[x1][y1]);
                countNotNa++;

            }
        }

        if(values.size() == 0)
            return naValue;
        if(countNotNa < minCountForCalculation)
            return naValue;

        return median(values);
    }

    public short median(ArrayList<Short> in){

        // Sort the values
        in.sort(null);

        // Return the median
        return in.get(in.size() / 2);
    }

    public static short[][] clone2DArray(short[][] original) {
        int rows = original.length;
        short[][] clone = new short[rows][];

        for (int i = 0; i < rows; i++) {
            int columns = original[i].length;
            clone[i] = new short[columns];
            System.arraycopy(original[i], 0, clone[i], 0, columns);
        }

        return clone;
    }

    public double getResolution(){
        return this.resolution;
    }

}
