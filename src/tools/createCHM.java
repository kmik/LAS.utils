package tools;


import LASio.*;
import org.apache.commons.io.FileUtils;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import org.opencv.core.Mat;
import java.io.*;
import java.util.*;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import utils.*;
import org.tinfour.common.Vertex;

public class createCHM{

    public static fileOperations fo = new fileOperations();

    static class CheckerCellItem implements Comparator<cellItem>{

        public int compare(cellItem ob1, cellItem ob2){
            if (ob1.priority > ob2.priority)
                return 1;
            else
                return -1;
        }
    }

	static class Checker implements Comparator<Pixel>{

      	public int compare(Pixel ob1, Pixel ob2){
	          if (ob1.priority > ob2.priority) 
	          	return 1;
	          else
	          	return -1;
      	}
 	}

    static class CheckerWater implements Comparator<WaterBody>{

        public int compare(WaterBody ob1, WaterBody ob2){
              if (ob1.waterLevel > ob2.waterLevel) 
                return 1;
              else
                return -1;
        }
    }

 	public static void localSmoothing2(Mat input, int n){  // MEAN


        int height = input.height();
        int width = input.width();
        int counter = 0;
        int paatos = (height - n) * (width - n);


        for(int i = n; i < (height - n); i++){

            for(int j = n; j < (width - n); j++){

                int minX = j - n;
                int maxX = j + n;
                int minY = i - n;
                int maxY = i + n;

                if(minX < 0)
                    minX = 0;

                if(minY < 0)
                    minY = 0;

                if(maxX > (width - 1))
                    maxX = width - 1;

                if(maxY > (height - 1))
                    maxY = height - 1;

                Mat submat = input.submat(minY, maxY, minX, maxX);

                int sum = 0;

                int[] array = new int[(n * 2 + 1) * (n * 2 + 1)];

                int count = 0;
                int count2 = 0;

                for(int h = 0; h < submat.width(); h++)
                    for(int u = 0; u < submat.height(); u++){

                        if(h == submat.width() / 2 || u == submat.height() / 2){

                            if(u == h)
                                sum += (int)submat.get(u,h)[0] * 2;
                            else
                                sum += (int)submat.get(u,h)[0];
                            count2++;
                        }
                    
                    }
                double mean = (double)sum / (double)((n * 2 + 1) * (n * 2 + 1));

                input.put(i,j,(sum / count2));


                counter++;
            }

        } 

    }


    public static double[][] twoDimensionalArrayClone(double[][] a) {

        double[][] b = new double[a.length][];

        for (int i = 0; i < a.length; i++) {
          b[i] = a[i].clone();
        }

        return b;
    }

    public static double[][] arrayCopy(double[][] in){

        double[][] output = new double[in.length][in[0].length];

        for(int i = 0; i < in.length; i++)
            for(int j = 0; j < in[0].length; j++)
                output[i][j] = in[i][j];

        return output;


    }

    public static float[][] arrayCopy(float[][] in){

        float[][] output = new float[in.length][in[0].length];

        for(int i = 0; i < in.length; i++)
            for(int j = 0; j < in[0].length; j++)
                output[i][j] = in[i][j];

        return output;


    }


    public static float gradientAt(float[][] i, int x, int y){

            float Gx = (-i[x - 1][y + 1] +  2 * -i[x][y + 1] + -i[x + 1][y + 1]) - (-i[x - 1][y - 1] + 2 * -i[x][y - 1] + -i[x + 1][y - 1]);

            float Gy = (-i[x + 1][y - 1] +  2 * -i[x + 1][y] + -i[x + 1][y + 1]) - (-i[x - 1][y - 1] + 2 * -i[x - 1][y] + -i[x - 1][y + 1]);

            return (float)Math.sqrt(Math.pow(Gx,2) + Math.pow(Gy,2));// - image.get(x,y).z;

    }

    public static float[][] removeOutliers(float[][] input, int blur, int kernel, double theta){

        Statistics stat = new Statistics();

        int x = 0;
        int y = 0;

        int n = 1;

        float[][] output = new float[input.length - n * 2][input[0].length - n * 2];

        float[][] temppi = arrayCopy(input);

        int height = input[0].length;
        int width = input.length;
        int counter = 0;
        int paatos = (height - n) * (width - n);

        int count3 = 0;

        float[] tempF;

        ArrayList<int[]> leftOvers = new ArrayList<int[]>();

        TreeSet<Float> zets = new TreeSet<Float>();

        for(int i = n; i < (height - n); i++){

            for(int j = n; j < (width - n); j++){

                if(input[j][i] > 2.0)
                    zets.add(input[j][i]);

                int minX = j - n;
                int maxX = j + n;
                int minY = i - n;
                int maxY = i + n;



                ArrayList<Float> list = new ArrayList<Float>();



                tempF = new float[2];

                for(int h = minX; h <= maxX; h++){
                    for(int u = minY; u <= maxY; u++){

                        x = h;
                        y = u;

                        if(x < 0)
                            x = 0;
                        if(y < 0)
                            y = 0;
                        if(x > (width - 1))
                            x = width - 1;
                        if(y > (height - 1))
                            y = height - 1;

                        if((x != j || y != i) && !Double.isNaN(temppi[x][y])){

                            tempF[0] += temppi[x][y];
                            tempF[1]++;
                            //list.add(temppi[h][u]);

                        }

                    }


                    float median = Float.NaN;

                    if(tempF[1] > 0)
                        median = tempF[0] / tempF[1];

                    if(Double.isNaN(temppi[j][i]))
                        input[j][i] = median;

                    if(Double.isNaN(input[j][i])){

                        int[] leftOver = new int[2];

                        leftOver[0] = j;
                        leftOver[1] = i;

                        leftOvers.add(leftOver);

                    }

                    counter++;
                }
            }
        }

        int leftOverCount = 0;

        ArrayList<int[]> leftOvers2;

        while(leftOvers.size() > 0){

            temppi = arrayCopy(input);

            leftOvers2 = (ArrayList<int[]>)leftOvers.clone();
            leftOvers.clear();

            leftOverCount++;

            for(int i = 0; i < leftOvers2.size(); i++){

                int minX = leftOvers2.get(i)[0] - n;
                int maxX = leftOvers2.get(i)[0] + n;
                int minY = leftOvers2.get(i)[1] - n;
                int maxY = leftOvers2.get(i)[1] + n;


                ArrayList<Float> list = new ArrayList<Float>();

                tempF = new float[2];

                for(int h = minX; h <= maxX; h++){
                    for(int u = minY; u <= maxY; u++){

                        x = h;
                        y = u;

                        if(x < 0)
                            x = 0;
                        if(y < 0)
                            y = 0;
                        if(x > (width - 1))
                            x = width - 1;
                        if(y > (height - 1))
                            y = height - 1;

                        if((x != leftOvers2.get(i)[0] || y != leftOvers2.get(i)[1]) && !Double.isNaN(temppi[x][y])){

                            tempF[0] += temppi[x][y];
                            tempF[1]++;


                        }

                    }
                
                }


                float median = Float.NaN;

                if(tempF[1] > 0)
                    median = tempF[0] / tempF[1];

                if(Double.isNaN(temppi[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]]))
                    input[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] = median;


                else if(median > (temppi[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] + 2.0) || (median + 2.0) < temppi[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] ){
                    input[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] = median;
                }
                //input[leftOvers.get(i)[0]][leftOvers.get(i)[1]] = median;

                if(Double.isNaN(input[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]])){
                    
                    int[] leftOver = new int[2];

                    leftOver[0] = leftOvers2.get(i)[0];
                    leftOver[1] = leftOvers2.get(i)[1];

                    leftOvers.add(leftOver);

                }
            }
        }

        count3 = 0;

        float[][] original = null;

        n = blur;
        n = 1;

        int count = 0;

        float p80 = 0f;

        for(float i : zets){

            if(count++ >= zets.size() * 0.80){
                p80 = i;
                break;
            }

        }

        if(n > 0){


            double sigma = 0.8;

            int rows = input.length;
            int cols = input[0].length;

            double[][] temppi2 = new double[rows][cols];

            original = new float[rows][cols];
            
            for (int i = 0; i < rows; i++){
                for(int j = 0; j < cols; j++){
                     temppi2[i][j] = input[i][j];
                     original[i][j] = input[i][j];
                }
            }


            int[] kernelSizes = new int[]{9,9};
            double[] sigmas = new double[]{1.1,2.5};
            double[] tHolds = new double[]{15};

            

            double[][] smoothed = null;



            //smoothed = GaussianSmooth.smooth2(temppi2, cols, rows, kernelSizes, sigmas, tHolds);
            
            //if(p80 > 20)
              //  smoothed = GaussianSmooth.smooth(temppi2, rows, cols, 3, 1.0);  // THIS IS GOOD!!!! :)
            //else
              //  smoothed = GaussianSmooth.smooth(temppi2, rows, cols, 3, 0.5);

            smoothed = GaussianSmooth.smooth(temppi2, rows, cols, kernel, theta);  // THIS IS GOOD!!!! :)

            //smoothed = GaussianSmooth.smooth(smoothed, cols, rows, 9, 2.2);

            for (int i = 0; i < rows; i++){
                for(int j = 0; j < cols; j++){
                     input[i][j] = (float)smoothed[i][j];
                }
            }

            /*
            Size ksize = new Size(3, 3);

            Mat output2 = new Mat();//HxW 4x2


            Imgproc.GaussianBlur(tempMat, output2, ksize, sigma);
            */
            /*
            for(int i = 0; i < tempMat.height(); i++){
                for(int j = 0; j < tempMat.width(); j++){

                    input[j][i] = (float)tempMat.get(j,i)[0];

                }
            }
            */
            /*
            for(int i = 0; i < (height - 0); i++){

                for(int j = 0; j < (width - 0); j++){

                   
                    int minX = j - n;
                    int maxX = j + n;
                    int minY = i - n;
                    int maxY = i + n;


                    int sum = 0;

                    //double[] array = new double[(n * 2 + 1) * (n * 2 + 1)];

                    ArrayList<Float> list = new ArrayList<Float>();
                    int count = 0;
                    int count2 = 0;


                    tempF = new float[2];

                    

                    for(int h = minX; h <= maxX; h++){
                        for(int u = minY; u <= maxY; u++){

                            x = h;
                            y = u;

                            if(x < 0)
                                x = 0;
                            if(y < 0)
                                y = 0;
                            if(x > (width - 1))
                                x = width - 1;
                            if(y > (height - 1))
                                y = height - 1;

                            if((x != j || y != i) && !Double.isNaN(temppi[x][y])){

                                tempF[0] += temppi[x][y];
                                tempF[1]++;


                            }


                        }

                    float median = Float.NaN;

                    if(tempF[1] > 0)
                        median = tempF[0] / tempF[1];

                    input[j][i] = median;
                    

                    counter++;
                }
                //progebar(paatos, counter, nimi);
                } 

            

            }
            */
        }

        //System.out.println("");

        int rows = input.length;
        int cols = input[0].length;
        /*
        float[][] temppi2 = new float[rows][cols];
            
            for (int i = 0; i < rows; i++){
                for(int j = 0; j < cols; j++){
                     temppi2[i][j] = input[i][j];
                }
            }
        */
        n = 1;
        /*
        for(int i = 1; i < (height - 2); i++){

            for(int j = 1; j < (width - 2); j++){

                input[j][i] = (float)gradientAt(temppi2, j, i);

            }
        }
        */
        return original;

    }

    public static Dataset copyRaster(Dataset from, Dataset to, String toName){
/*
        int ysize = from.getRasterYSize();
        int xsize = from.getRasterXSize();

        float[] floatArray = new float[ysize];

        Band fromBand = from.GetRasterBand(1);
        Band toBand = to.GetRasterBand(1);

        for(int x = 0; x < xsize; x++) {
            //for (int y = 0; y < fromBand.getYSize(); y++) {

            //}

            fromBand.ReadRaster(x, 0, 1, ysize, floatArray);
            toBand.WriteRaster(x, 0, 1, ysize, floatArray);

            if(x % 100 == 0)
                System.out.println(x + " | " + xsize);
        }

*/
        Vector<String> optionsVector = new Vector<>();
        optionsVector.add("-of");
        optionsVector.add("GTiff");

        org.gdal.gdal.TranslateOptions optit = new org.gdal.gdal.TranslateOptions(optionsVector);
        //Dataset inputdata = gdal.Open(tempName, gdalconstConstants.GA_ReadOnly);

        return gdaltranslate(toName, from, optit); //gdal.Translate(name, inputdata, optit);

    }

    public static void copyRasterContents(Band from, Band to){


        int x = from.getXSize();
        int y = from.getYSize();

        float[] read = new float[x*y];

        from.ReadRaster(0, 0, x, y, read);

        to.WriteRaster(0, 0, x, y, read);

    }

    public static Dataset removeOutliers_tif(Dataset input, int blur, int kernel, double theta, int id, String filename, boolean nan){

        float[] floatArray = new float[1];
        float[] floatArray2 = new float[1];

        Statistics stat = new Statistics();

        int x = 0;
        int y = 0;

        int n = 1;

        //float[][] output = new float[input.length - n * 2][input[0].length - n * 2];

        org.gdal.gdal.Driver driver = gdal.GetDriverByName("GTiff");

        File copyFile = new File(filename);

        File newFile = new File("tempFilter_" + id + ".tif");

        try {
            FileUtils.copyFile(copyFile, newFile);
        }catch (IOException e){
            e.printStackTrace();
        }


        Dataset temppi = gdalE.hei("tempFilter_" + id + ".tif", input.getRasterYSize(), input.getRasterXSize(), Float.NaN);// driver.Create("filtered.tif", input.getRasterXSize(), input.getRasterYSize(), 1, gdalconst.GDT_Float32);

        copyRaster(input, temppi, "tempFilter_" + id + ".tif");

        temppi = gdalE.hei("tempFilter_" + id + ".tif", input.getRasterYSize(), input.getRasterXSize(), Float.NaN);// driver.Create("filtered.tif", input.getRasterXSize(), input.getRasterYSize(), 1, gdalconst.GDT_Float32);

        //System.exit(0);
        //Dataset temppi = gdal.Open(newFile.getAbsolutePath());

        //if(true)
          //  return;
        Band temppi_band = temppi.GetRasterBand(1);
        Band input_band = input.GetRasterBand(1);

        copyRasterContents(input_band, temppi_band);
        //float[][] temppi = arrayCopy(input);

        int height = temppi.getRasterYSize();
        int width = temppi.getRasterXSize();
        int counter = 0;
        int paatos = (height - n) * (width - n);

        int count3 = 0;

        float[] tempF;

        ArrayList<int[]> leftOvers = new ArrayList<int[]>();

        //TreeSet<Float> zets = new TreeSet<Float>();

        //System.out.println("height " + height);

        if(nan)
        for(int i = n; i < (height - n); i++){

            for(int j = n; j < (width - n); j++){

                //if(input[j][i] > 2.0)
                  //  zets.add(input[j][i]);

                if(count3++ % 10000 == 0){

                    //System.out.print("\033[2K"); // Erase line content
                    //System.out.print(count3 + "|" + (height * width) + " " + " NaNs found: " + leftOvers.size() + "\r");
                }

                int minX = j - n;
                int maxX = j + n;
                int minY = i - n;
                int maxY = i + n;

                /*
                if(minX < 0)
                    minX = 0;

                if(minY < 0)
                    minY = 0;

                if(maxX > (width - 1))
                    maxX = width - 1;

                if(maxY > (height - 1))
                    maxY = height - 1;
                */
                //Mat submat = input.submat(minY, maxY + 1, minX, maxX + 1);

                int sum = 0;

                //double[] array = new double[(n * 2 + 1) * (n * 2 + 1)];
                ArrayList<Float> list = new ArrayList<Float>();
                int count = 0;
                int count2 = 0;


                tempF = new float[2];

                for(int h = minX; h <= maxX; h++){
                    for(int u = minY; u <= maxY; u++){

                        x = h;
                        y = u;

                        if(x < 0)
                            x = 0;
                        if(y < 0)
                            y = 0;
                        if(x > (width - 1))
                            x = width - 1;
                        if(y > (height - 1))
                            y = height - 1;

                        input_band.ReadRaster(x, y, 1, 1, floatArray);

                        //if(!Float.isNaN(floatArray2[0]))
                        //System.out.println(floatArray2[0]);



                        if(!Float.isNaN(floatArray[0])){ // (x != j || y != i) &&

                            tempF[0] += floatArray[0];
                            tempF[1]++;
                            //list.add(temppi[h][u]);

                        }

                    }

                    float median = Float.NaN;

                    if(tempF[1] > 0)
                        median = tempF[0] / tempF[1];


                    //temppi_band.ReadRaster(j, i, 1, 1, floatArray);
                    input_band.ReadRaster(j, i, 1, 1, floatArray);
                    Double[] nodata = new Double[1];
                    input_band.GetNoDataValue(nodata);





                    //System.out.println(floatArray[0] + " " + floatArray2[0]);

                    if(Float.isNaN(floatArray[0])){ //  || floatArray[0] < median / 2.0f
                        floatArray[0] = median;
                        input_band.WriteRaster(j, i, 1, 1, floatArray);
                        //input[j][i] = median;
                    }



                    //else if((temppi[j][i]) > (median * 1.2) || temppi[j][i] < (median * 0.8)){
                    //  input[j][i] = median;
                    //}

                    input_band.ReadRaster(j, i, 1, 1, floatArray);
                    //input[j][i] = median;

                    if(Float.isNaN(floatArray[0])){

                        int[] leftOver = new int[2];

                        leftOver[0] = j;
                        leftOver[1] = i;

                        leftOvers.add(leftOver);

                    }
                    counter++;
                }
                //progebar(paatos, counter, nimi);
            }



        }

        //System.out.println("leftOvers.size(): " + leftOvers.size());
        int leftOverCount = 0;


        ArrayList<int[]> leftOvers2;

        if(nan)
        while(leftOvers.size() > 0){

            //temppi = arrayCopy(input);
            //copyRaster(input, temppi);
            //temppi = gdal.Open(newFile.getAbsolutePath());
            //temppi_band = temppi.GetRasterBand(1);

            Collections.shuffle(leftOvers);

            leftOvers2 = (ArrayList<int[]>)leftOvers.clone();
            leftOvers.clear();

            leftOverCount++;

            for(int i = 0; i < leftOvers2.size(); i++){

                int minX = leftOvers2.get(i)[0] - n;
                int maxX = leftOvers2.get(i)[0] + n;
                int minY = leftOvers2.get(i)[1] - n;
                int maxY = leftOvers2.get(i)[1] + n;

                /*
                if(minX < 0)
                    minX = 0;

                if(minY < 0)
                    minY = 0;

                if(maxX > (width - 1))
                    maxX = width - 1;

                if(maxY > (height - 1))
                    maxY = height - 1;
                */

                ArrayList<Float> list = new ArrayList<Float>();

                tempF = new float[2];

                for(int h = minX; h <= maxX; h++){
                    for(int u = minY; u <= maxY; u++){

                        x = h;
                        y = u;

                        if(x < 0)
                            x = 0;
                        if(y < 0)
                            y = 0;
                        if(x > (width - 1))
                            x = width - 1;
                        if(y > (height - 1))
                            y = height - 1;

                        input_band.ReadRaster(x, y, 1, 1, floatArray);

                        if(!Float.isNaN(floatArray[0])){ // (x != leftOvers2.get(i)[0] || y != leftOvers2.get(i)[1]) &&

                            tempF[0] += floatArray[0];
                            tempF[1]++;
                            //list.add(temppi[h][u]);

                        }


                        //else if(Double.isNaN(input[h][u]))
                        //array[count] = temppi[h][u];
                        //count++;
                    }

                }

                //stat.setDataF(list);

                float median = Float.NaN;

                if(tempF[1] > 0)
                    median = tempF[0] / tempF[1];

                //if(list.size() > 0)
                //  median = (float)stat.getMeanFromList();

                int x1 = leftOvers2.get(i)[0];
                int y1 = leftOvers2.get(i)[1];

                input_band.ReadRaster(x1, y1, 1, 1, floatArray);

                if(Float.isNaN(floatArray[0])) { //  || floatArray[0] < median / 2.0f

                    //if(!Float.isNaN(median))
                    //System.out.println("here! 1 " + median);
                    floatArray[0] = median;
                    input_band.WriteRaster(x1, y1, 1, 1, floatArray);
                    //input[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] = median;
                }


                else if(median > (floatArray[0] + 2.0) || (median + 2.0) < floatArray[0] ){
                    floatArray[0] = median;
                    input_band.WriteRaster(x1, y1, 1, 1, floatArray);
                    //input[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] = median;
                }
                //input[leftOvers.get(i)[0]][leftOvers.get(i)[1]] = median;

                x1 = leftOvers2.get(i)[0];
                y1 = leftOvers2.get(i)[1];

                input_band.ReadRaster(x1, y1, 1, 1, floatArray);

                if(Float.isNaN(floatArray[0])){

                    //System.out.println("here! 2");

                    int[] leftOver = new int[2];

                    leftOver[0] = leftOvers2.get(i)[0];
                    leftOver[1] = leftOvers2.get(i)[1];

                    leftOvers.add(leftOver);

                }

            }


            System.out.print("\033[2K"); // Erase line content
            System.out.print("Iteration: " + leftOverCount + " NaNs left: " + leftOvers.size() + "\r");

            if(leftOverCount > 100)
                leftOvers.clear();

        }

        count3 = 0;

        //System.loadLibrary("opencv_java320");

        float[][] original = null;

        n = blur;
        n = 1;

        int count = 0;

        float p80 = 0f;
/*
        for(float i : zets){

            if(count++ >= zets.size() * 0.80){
                p80 = i;
                break;
            }

        }
*/
        //System.out.println("p80: " + p80);
        if(n > 0){


            double sigma = 0.8;

            int rows = temppi_band.getYSize();
            int cols = temppi_band.getXSize();

            //Mat tempMat = new Mat(rows, cols, CvType.CV_32FC1);//HxW 4x2

            //double[][] temppi2 = new double[rows][cols];

            //original = new float[rows][cols];
/*
            for (int i = 0; i < rows; i++){
                for(int j = 0; j < cols; j++){

                    temppi2[i][j] = (double)input[i][j];
                    //original[i][j] = input[i][j];
                }
            }
*/

            //int[] kernelSizes = new int[]{9,9};
            //double[] sigmas = new double[]{1.1,2.5};
            //double[] tHolds = new double[]{15};



            //double[][] smoothed = null;

            double[][] matti = new double[input.getRasterXSize()][input.getRasterYSize()];
            double[][] matti_smooth = new double[input.getRasterXSize()][input.getRasterYSize()];

            for(int x_ = 0; x_ < input.getRasterXSize(); x_++){
                for(int y_ = 0; y_ < input.getRasterYSize(); y_++){

                    input_band.ReadRaster(x_, y_, 1, 1, floatArray);

                    matti[x_][y_] = floatArray[0];

                }
            }




            matti_smooth = GaussianSmooth.smooth(matti, cols, rows, kernel, theta);

            for(int x_ = 0; x_ < input.getRasterXSize(); x_++){
                for(int y_ = 0; y_ < input.getRasterYSize(); y_++){


                    floatArray[0] = (float)matti_smooth[x_][y_] ;

                    temppi_band.WriteRaster(x_, y_, 1, 1, floatArray);


                }
            }

            //if(p80 > 20)
            //  smoothed = GaussianSmooth.smooth(temppi2, rows, cols, 3, 1.0);  // THIS IS GOOD!!!! :)
            //else
            //  smoothed = GaussianSmooth.smooth(temppi2, rows, cols, 3, 0.5);

            //smoothed = GaussianSmooth.smooth(input, rows, cols, kernel, theta);  // THIS IS GOOD!!!! :)

            //GaussianSmooth.smooth_tif(input, temppi, cols, rows, kernel, theta);  // THIS IS GOOD!!!! :)

            //copyRaster(temppi, input);

            //newFile.delete();
            //smoothed = GaussianSmooth.smooth(smoothed, cols, rows, 9, 2.2);
/*
            for (int i = 0; i < rows; i++){
                for(int j = 0; j < cols; j++){
                    input[i][j] = (float)smoothed[i][j];
                }
            }
*/
            /*
            Size ksize = new Size(3, 3);

            Mat output2 = new Mat();//HxW 4x2


            Imgproc.GaussianBlur(tempMat, output2, ksize, sigma);
            */
            /*
            for(int i = 0; i < tempMat.height(); i++){
                for(int j = 0; j < tempMat.width(); j++){

                    input[j][i] = (float)tempMat.get(j,i)[0];

                }
            }
            */
            /*
            for(int i = 0; i < (height - 0); i++){

                for(int j = 0; j < (width - 0); j++){


                    int minX = j - n;
                    int maxX = j + n;
                    int minY = i - n;
                    int maxY = i + n;


                    int sum = 0;

                    //double[] array = new double[(n * 2 + 1) * (n * 2 + 1)];

                    ArrayList<Float> list = new ArrayList<Float>();
                    int count = 0;
                    int count2 = 0;


                    tempF = new float[2];



                    for(int h = minX; h <= maxX; h++){
                        for(int u = minY; u <= maxY; u++){

                            x = h;
                            y = u;

                            if(x < 0)
                                x = 0;
                            if(y < 0)
                                y = 0;
                            if(x > (width - 1))
                                x = width - 1;
                            if(y > (height - 1))
                                y = height - 1;

                            if((x != j || y != i) && !Double.isNaN(temppi[x][y])){

                                tempF[0] += temppi[x][y];
                                tempF[1]++;


                            }


                        }

                    float median = Float.NaN;

                    if(tempF[1] > 0)
                        median = tempF[0] / tempF[1];

                    input[j][i] = median;


                    counter++;
                }
                //progebar(paatos, counter, nimi);
                }



            }
            */
        }

        //System.out.println("");

        //int rows = input.length;
        //int cols = input[0].length;
        /*
        float[][] temppi2 = new float[rows][cols];

            for (int i = 0; i < rows; i++){
                for(int j = 0; j < cols; j++){
                     temppi2[i][j] = input[i][j];
                }
            }
        */
        n = 1;
        /*
        for(int i = 1; i < (height - 2); i++){

            for(int j = 1; j < (width - 2); j++){

                input[j][i] = (float)gradientAt(temppi2, j, i);

            }
        }
        */
        //return original;

        return temppi;

    }


    public static ArrayList<float[][]> removeOutliersRGB(ArrayList<float[][]> input){

        Statistics stat = new Statistics();

        int n = 2;

        float[][] output = new float[input.get(0).length - n * 2][input.get(0)[0].length - n * 2];

        ArrayList<float[][]> outputList = new ArrayList<float[][]>();

        float[][] temppi;

        int height = input.get(0)[0].length;
        int width = input.get(0).length;
        int counter = 0;
        int paatos = (height - n) * (width - n);

        int count3 = 0;

        float[] tempF;

        ArrayList<int[]> leftOvers = new ArrayList<int[]>();

        for(int k = 0; k < input.size(); k++){

            temppi = arrayCopy(input.get(k));

            for(int i = n; i < (height - n); i++){

                for(int j = n; j < (width - n); j++){

                    if(count3++ % 10000 == 0){

                        //System.out.print("\033[2K"); // Erase line content
                        //System.out.print(count3 + "|" + (height * width) + " " + " NaNs found: " + leftOvers.size() + "\r");
                    }

                    int minX = j - n;
                    int maxX = j + n;
                    int minY = i - n;
                    int maxY = i + n;

                    if(minX < 0)
                        minX = 0;

                    if(minY < 0)
                        minY = 0;

                    if(maxX > (width - 1))
                        maxX = width - 1;

                    if(maxY > (height - 1))
                        maxY = height - 1;

                    //Mat submat = input.submat(minY, maxY + 1, minX, maxX + 1);

                    int sum = 0;

                    ArrayList<Float> list = new ArrayList<Float>();
                    int count = 0;
                    int count2 = 0;


                    tempF = new float[2];

                    for(int h = minX; h <= maxX; h++){
                        for(int u = minY; u <= maxY; u++){

                            if((h != j || u != i) && !Double.isNaN(temppi[h][u])){

                                tempF[0] += temppi[h][u];
                                tempF[1]++;
                                //list.add(temppi[h][u]);

                            }

                        }

                    float median = Float.NaN;

                    if(tempF[1] > 0)
                        median = tempF[0] / tempF[1];

                    if(Double.isNaN(temppi[j][i]))
                        input.get(k)[j][i] = median;

                    else if(median > (temppi[j][i] + 2.0) || (median + 2.0) < temppi[j][i] ){
                        input.get(k)[j][i] = median;
                    }

                    //input[j][i] = median;

                    if(Double.isNaN(input.get(k)[j][i])){
                        
                        int[] leftOver = new int[3];

                        leftOver[0] = j;
                        leftOver[1] = i;
                        leftOver[2] = k;

                        leftOvers.add(leftOver);

                    }

                    //if(median )
                    //}

                    //input.put(i,j,array[indeksi[0]]);
                    //input.put(i,j,submat.get(indeksi[0], indeksi[1])[0]);
                    

                    counter++;
                }
                //progebar(paatos, counter, nimi);
                } 

            

            }
        }

        //System.out.println("");
        int leftOverCount = 0;

        ArrayList<int[]> leftOvers2;

        while(leftOvers.size() > 0){

            leftOvers2 = (ArrayList<int[]>)leftOvers.clone();
            leftOvers.clear();

            leftOverCount++;

            for(int i = 0; i < leftOvers2.size(); i++){

                /* THIS TAKES A LONG TIME */
                temppi = arrayCopy(input.get(leftOvers2.get(i)[2]));

                int minX = leftOvers2.get(i)[0] - n;
                int maxX = leftOvers2.get(i)[0] + n;
                int minY = leftOvers2.get(i)[1] - n;
                int maxY = leftOvers2.get(i)[1] + n;

                if(minX < 0)
                    minX = 0;

                if(minY < 0)
                    minY = 0;

                if(maxX > (width - 1))
                    maxX = width - 1;

                if(maxY > (height - 1))
                    maxY = height - 1;


                ArrayList<Float> list = new ArrayList<Float>();

                tempF = new float[2];

                for(int h = minX; h <= maxX; h++){
                    for(int u = minY; u <= maxY; u++){

                        if((h != leftOvers2.get(i)[0] || u != leftOvers2.get(i)[1]) && !Double.isNaN(temppi[h][u])){

                            tempF[0] += temppi[h][u];
                            tempF[1]++;
                            //list.add(temppi[h][u]);

                        }
                            

                        //else if(Double.isNaN(input[h][u]))
                            //array[count] = temppi[h][u];  
                            //count++;
                    }
                
                }

                //stat.setDataF(list);

                float median = Float.NaN;

                if(tempF[1] > 0)
                    median = tempF[0] / tempF[1];

                //if(list.size() > 0)
                  //  median = (float)stat.getMeanFromList();


                if(Double.isNaN(temppi[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]]))
                    input.get(leftOvers2.get(i)[2])[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] = median;

                else if(median > (temppi[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] + 2.0) || (median + 2.0) < temppi[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] ){
                    input.get(leftOvers2.get(i)[2])[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]] = median;
                }
                //input[leftOvers.get(i)[0]][leftOvers.get(i)[1]] = median;

                if(Double.isNaN(input.get(leftOvers2.get(i)[2])[leftOvers2.get(i)[0]][leftOvers2.get(i)[1]])){
                    
                    int[] leftOver = new int[3];

                    leftOver[0] = leftOvers2.get(i)[0];
                    leftOver[1] = leftOvers2.get(i)[1];
                    leftOver[2] = leftOvers2.get(i)[2];

                    leftOvers.add(leftOver);

                }

            }

            //System.out.print("\033[2K"); // Erase line content
            //System.out.print("Iteration: " + leftOverCount + " NaNs left: " + leftOvers.size() + "\r");

        }

        count3 = 0;

        n = 1;
        
        /*
        for(int i = n; i < (height - n); i++){

            for(int j = n; j < (width - n); j++){



                if(count3++ % 10000 == 0){

                    System.out.print("\033[2K"); // Erase line content
                    System.out.print(count3 + "|" + (height * width) + " " + " NaNs found: " + leftOvers.size() + "\r");
                }

                int minX = j - n;
                int maxX = j + n;
                int minY = i - n;
                int maxY = i + n;

                if(minX < 0)
                    minX = 0;

                if(minY < 0)
                    minY = 0;

                if(maxX > (width - 1))
                    maxX = width - 1;

                if(maxY > (height - 1))
                    maxY = height - 1;

                //Mat submat = input.submat(minY, maxY + 1, minX, maxX + 1);

                int sum = 0;

                //double[] array = new double[(n * 2 + 1) * (n * 2 + 1)];
                ArrayList<Float> list = new ArrayList<Float>();
                int count = 0;
                int count2 = 0;


                tempF = new float[2];

                for(int h = minX; h <= maxX; h++){
                    for(int u = minY; u <= maxY; u++){

                        if((h != j || u != i) && !Double.isNaN(temppi[h][u])){

                            tempF[0] += temppi[h][u];
                            tempF[1]++;


                        }


                    }

                float median = Float.NaN;

                if(tempF[1] > 0)
                    median = tempF[0] / tempF[1];

                input[j][i] = median;
                

                counter++;
            }
            //progebar(paatos, counter, nimi);
            } 

        

        }
        */

        //System.out.println("");

        n = 2;

        for(int k = 0; k < input.size(); k++){

            outputList.add(new float[input.get(0).length - n * 2][input.get(0)[0].length - n * 2]);

        }

        for(int k = 0; k < input.size(); k++){

            for(int i = 0; i < (height - n * 2); i++){

                for(int j = 0; j < (width - n * 2); j++){

                    outputList.get(k)[j][i] = input.get(k)[j + n][i + n]; // / (input.get(0)[j + n][i + n] + input.get(1)[j + n][i + n] + input.get(2)[j + n][i + n]) * 100.0f;

                }
            }
        }
        /*
        for(int i = 0; i < (height - n * 2); i++){

            for(int j = 0; j < (width - n * 2); j++){

                output[j][i] = input[j + n][i + n];

            }
        }
        */
        return outputList;

    }

 	public static class Raster{

        float[] floatArray = new float[1];

 		int xDim;
 		int yDim;

 		Pixel[][] layout;

        /** We need 3 tif images:
         * (1) Water pool ID (int)
         * (2) Flag for que (boolean)
         * (3) Priority (float)
         * (4) z
         */

        Dataset raster_id;
        Dataset raster_flag;
        Dataset raster_priority;
        Dataset raster_z;

        Band raster_id_b;
        Band raster_flag_b;
        Band raster_priority_b;
        Band raster_z_b;

        int coreNumber;

        double resolution;

 		public Raster(){


 		}

 		public Raster(Dataset in, int coreNumber, double resolution){

 		    this.resolution = resolution;
 		    this.coreNumber = coreNumber;
 		    this.raster_z = in;
 		    this.raster_z_b = raster_z.GetRasterBand(1);

 			this.yDim = raster_z_b.getYSize(); // in[0].length;
 			this.xDim = raster_z_b.getXSize(); //.length;

            float[] floatArray = new float[1];

            long id = 0L;
/*
 			this.layout = new Pixel[yDim][xDim];

 			for(int i = 0; i < xDim; i++){
 				for(int j = 0; j < yDim; j++) {
 				    //in.ReadRaster(i, j, 1, 1, floatArray);
                    //layout[j][i] = new Pixel(i, j, floatArray[0], 0.0f);
                }
 			}
*/
            raster_id = gdalE.hei("tempRasterId"+coreNumber+".tif", this.yDim, this.xDim, -999f);
 			raster_id_b = raster_id.GetRasterBand(1);

            raster_flag = gdalE.hei("tempRasterFlag"+coreNumber+".tif", this.yDim, this.xDim, 0.0f);
            raster_flag_b = raster_flag.GetRasterBand(1);

            raster_priority = gdalE.hei("tempRasterPriority"+coreNumber+".tif", this.yDim, this.xDim, 0.0f);
            raster_priority_b = raster_priority.GetRasterBand(1);

 		}

 		public void deleteTempFiles(){

 		    File tempFile = new File("tempRasterId"+coreNumber+".tif");
 		    tempFile.delete();

            tempFile = new File("tempRasterFlag"+coreNumber+".tif");
            tempFile.delete();

            tempFile = new File("tempRasterPriority"+coreNumber+".tif");
            tempFile.delete();


 /*
            raster_id_b.FlushCache();
            raster_id.delete();

            raster_flag.FlushCache();
            raster_flag.delete();

            raster_flag.FlushCache();
            raster_priority.delete();
*/
        }

 		public Pixel get(int x, int y){

            if(x < 0)
                x = 0;
            if(x >= xDim)
                x = xDim - 1;
            if(y < 0)
                y = 0;
            if(y >= yDim)
                y = yDim - 1;

 			return layout[y][x];

 		}

 		public void treeTop(int x, int y){

 		    floatArray[0] = -99;
 		    raster_flag_b.WriteRaster(x, y, 1, 1, floatArray);

        }

        public boolean isTreeTop(int x, int y){

 		    raster_flag_b.ReadRaster(x, y, 1, 1, floatArray);
 		    return floatArray[0] == -99;
        }

 		public void attach(int x, int y, int id ){

 		    //System.out.println("ATTACHED: " + id);
            floatArray[0] = id;
            raster_id_b.WriteRaster(x, y, 1, 1, floatArray);
        }

        public void detach(int x, int y){

            floatArray[0] = -999f;
            raster_id_b.WriteRaster(x, y, 1, 1, floatArray);
        }

        public void queue(int x, int y){
            floatArray[0] = 1.0f;
            raster_flag_b.WriteRaster(x, y, 1, 1, floatArray);
        }

        public void dequeue(int x, int y){
            floatArray[0] = 0.0f;
            raster_flag_b.WriteRaster(x, y, 1, 1, floatArray);
        }

        public void priority(int x, int y, float priority){
            floatArray[0] = priority;
            raster_priority_b.WriteRaster(x, y, 1, 1, floatArray);
        }


 	}

 	public static class cellItem{

        public int x;
        public int y;
        float priority;

        public cellItem(int x, int y, float priority){

            this.x = x;
            this.y = y;
            this.priority = priority;
        }


    }

 	public static class Pixel{

 		int x;
 		int y;
 		float z;
 		float priority;
 		int id = -999;

        //long uniqueId;

        boolean inQue = false;

        boolean treeTop = false;

        HashSet<Long> reject = new HashSet<Long>();

 		public Pixel(){


 		}

 		public Pixel(int x2, int y2, float z2, float priority2){

 			this.x = x2;
 			this.y = y2;
 			this.z = z2;

            //this.uniqueId = id;

 			this.priority = priority2;
 		}

 		public void attach(int waterBodyId){

 			this.id = waterBodyId;

 		}

        public void detach(){

            this.id = -999;

        }

        public void treeTop(){

            this.treeTop = true;

        }

        public void queue(){

            this.inQue = true;

        }

        public void dequeue(){

            this.inQue = false;

        }

 		public void setPriority(float newPriority){

 			this.priority = newPriority;

 		}

 		public String toString() {
        	return "x: " + this.x + " y: " + this.y + " " + this.z + " priority: " + this.priority ;
    	}

        public void reject(long in){

            reject.add(in);

        }

 	}

	public static class WaterBody{

		long cellCount;
		boolean first = true;
		int id;
		double xMiddle;
		double yMiddle;
		double zMiddle;
        double zMiddleOrig;

		PriorityQueue<Pixel> jono = new PriorityQueue<Pixel>(5, new Checker());
		PriorityQueue<cellItem> jono_tif = new PriorityQueue<>(5, new CheckerCellItem());

        public double area = 0.0;

		double minX;
		double maxX;
		double minY;
		double maxY;

		double xMiddle2;
		double yMiddle2;
		double zMiddle2;


		double leftX;
		double rightX;
		double upY;
		double downY;

		double[] koillis = new double[2];
		double[] kaakkois = new double[2];
		double[] lounais = new double[2];
		double[] luoteis = new double[2];

		int missed = 0;
		int total = 0;

		double midX;
		double midY;

		Raster image;

		Statistics stat = new Statistics();

		int increment = 1;

		boolean done = false;

		double minDistance;

        double waterLevel = 0.0;

        double priority = waterLevel;

        int missedIterations = 0;

        double prevIterWaterLevel = 0.0;

        int higherThanWaterLevel = 0;

        int iterationCount = 0;

        int countLastIteration = 0;
        double sumZLastIteration = 0.0;

        double meanZlast = 0.0;

        int iterationNumber = 0;

        ArrayList<double[]> previous = new ArrayList<double[]>();

        int numberOfIterations = 0;

        int maxTries = 2;
        int tryNumber = 0;

        int numberOfPixels = 0;

        double sumZ = 0.0;



		public WaterBody(){


		}

        public void add(Pixel in){

            this.jono.offer(in);

        }

        public void add(cellItem in){

            this.jono_tif.offer(in);

        }

        public double area(){

            return jono.size() * (this.image.resolution * this.image.resolution);

        }

		public WaterBody(int id2, double x, double y, double z, Raster img){
			this.id = id2;

			this.xMiddle = x;
			this.yMiddle = y;
			this.zMiddle = z;
            this.zMiddleOrig = z;

			this.image = img;

            this.waterLevel = -z;
            this.prevIterWaterLevel = -z;
            this.meanZlast = z;
            this.priority = waterLevel;
            this.countLastIteration = 0;

			this.minX = x;
			this.maxX = x;
			this.minY = y;
			this.maxY = y;

			this.minDistance = Double.POSITIVE_INFINITY;

			this.koillis[0] = x;
			this.koillis[1] = y;
			this.kaakkois[0] = x;
			this.kaakkois[1] = y;
			this.lounais[0] = x;
			this.lounais[1] = y;
			this.luoteis[0] = x;
			this.luoteis[1] = y;

			
			//System.out.println(x + " " + y);

			this.midX = (maxX - minX) / 2.0;
			this.midY = (maxY - minY) / 2.0;

			//drawCircle((int)x, (int)y, increment);

			//updateNeighbourhood((int)x, (int)y, 20.0);
			/*
			jono.offer(img.get((int)x - 1 ,(int)y - 1));
			img.get((int)x - 1 ,(int)y - 1).attach(this.id);

			jono.offer(img.get((int)x ,(int)y - 1));
			img.get((int)x ,(int)y - 1).attach(this.id);

			jono.offer(img.get((int)x + 1 ,(int)y - 1));
			img.get((int)x + 1 ,(int)y - 1).attach(this.id);

			jono.offer(img.get((int)x - 1 ,(int)y));
			img.get((int)x - 1 ,(int)y).attach(this.id);
			//jono.offer(img.get(x ,y));
			jono.offer(img.get((int)x + 1 ,(int)y));
			img.get((int)x + 1 ,(int)y).attach(this.id);

			jono.offer(img.get((int)x - 1 ,(int)y + 1));
			img.get((int)x - 1 ,(int)y + 1).attach(this.id);

			jono.offer(img.get((int)x ,(int)y + 1));
			img.get((int)x ,(int)y + 1).attach(this.id);

			jono.offer(img.get((int)x + 1 ,(int)y + 1));
			img.get((int)x + 1 ,(int)y + 1).attach(this.id);
			*/
			cellCount = cellCount + 8;

			total = jono.size();

			//System.out.println(jono.size());

			//System.out.println(jono);


		}

	

        public double gradientAt(int x, int y){

            double Gx = (image.get(x - 1, y + 1).z +  2 * image.get(x, y + 1).z + image.get(x + 1, y + 1).z) - (image.get(x - 1, y - 1).z + 2 * image.get(x, y - 1).z + image.get(x + 1, y - 1).z);

            double Gy = (image.get(x + 1, y - 1).z +  2 * image.get(x + 1, y).z + image.get(x + 1, y + 1).z) - (image.get(x - 1, y - 1).z + 2 * image.get(x - 1, y).z + image.get(x - 1, y + 1).z);
        
            return Math.sqrt(Math.pow(Gx,2) + Math.pow(Gy,2));
        }

		public boolean isIsolated(Pixel in){

			int x = in.x;
			int y = in.y;

			int count = 0;

			//System.out.println(x + " " + y);


			if(x > 0 && x < image.xDim && y > 0 && y < image.yDim){
				for(int i = (x - 1); i <= (x + 1); i++){
					for(int j = (y - 1); j <= (y + 1); j++){
						//String temp = i + " " + j;
						//System.out.println(temp);
						if(((i != x) || (j != y))){
							if(image.get(i, j).id != -999)
								count++;
						}
					}
				}
			}
            return count > 4;

		}

		public boolean isCircle(int x, int y){

			double minX2 = this.minX;
			double maxX2 = this.maxX;
			double minY2 = this.minY;
			double maxY2 = this.maxY;
	
			double[] koillisT = this.koillis.clone();
			double[] kaakkoisT = this.kaakkois.clone();
			double[] lounaisT = this.lounais.clone();
			double[] luoteisT = this.luoteis.clone();

			if(x < this.minX)
				minX2--;

			if(x > this.maxX)
				maxX2++;

			if(y < this.minY)
				minY2--;

			if(y > this.maxY)
				maxY2++;

			if(y >= this.maxY && x >= this.maxX){
				koillisT[0] = x; 
				koillisT[1] = y; 
			}

			if(y <= this.minY && x >= this.maxX){
				kaakkoisT[1] = y; 
			}

			if(y <= this.minY && x <= minX){
				lounaisT[0] = x; 
				lounaisT[1] = y; 
			}

			if(y >= this.maxY && x <= this.minX){
				luoteisT[0] = x; 
				luoteisT[1] = y; 
			}
				

			double midX2 = (maxX2 + minX2) / 2.0;
			double midY2 = (maxY2 + minY2) / 2.0;

			//midX2 = this.xMiddle;
			//midY2 = this.yMiddle;

			double meanDifference = ((midX2 - minX2) + (maxX2 - midX2) + (midY2 - minY2) + (maxY2 - midY2)) / 4.0;
			
			double horis = (midX2 - minX2);
			double verti = (midY2 - minY2);


			double[] d = new double[8];

			d[0] = euclideanDistance(midX2, midY2, koillis[0], koillis[1]);
			d[1] = euclideanDistance(midX2, midY2, kaakkois[0], kaakkois[1]);
			d[2] = euclideanDistance(midX2, midY2, lounais[0], lounais[1]);
			d[3] = euclideanDistance(midX2, midY2, luoteis[0], luoteis[1]);

			d[4] = (midX2 - minX2);
			d[5] = (maxX2 - midX2);
			d[6] = (midY2 - minY2);
			d[7] = (maxY2 - midY2);

			stat.setData(d);
			//System.out.println(Arrays.toString(d));
			//System.out.println(stat.getVariance());

			

			/*
			System.out.println("----------------");
			System.out.println(meanDifference);
			System.out.println("koillis : " + Arrays.toString(koillis));
			System.out.println("kaakkois : " + Arrays.toString(kaakkois));
			System.out.println("lounais : " + Arrays.toString(lounais));
			System.out.println("luoteis : " + Arrays.toString(luoteis));
			*/
			/*
			if(this.id == 162983){
				System.out.println(Arrays.toString(d));
				System.out.println(stat.getVariance());
			}
			*/
			//if(this.cellCount <= 5)
				//return true;

            // (Math.abs((midX2 - minX2) - (midY2 - minY2)) <= 1.0 &&
            return stat.getVariance() < 1.0;

        }

		public double euclideanDistance(double x1, double y1, double x2, double y2){


			return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

		}

		public boolean circle(double minx, double maxx, double miny, double maxy){

			double midX2 = (maxx - minx) / 2.0;
			double midY2 = (maxy - miny) / 2.0;

			double mean = (Math.abs(minx - midX2) + Math.abs(maxx - midX2) + Math.abs(miny - midY2) + Math.abs(maxy - midY2)) / 4.0;

			//if( Math.abs(minx - midX2) )

			return false;
		}

		public void newBorder(){
			//System.out.println("GOT HERE!");
			this.first = true;
			missed = 0;

			increment++;

			drawCircle((int)this.xMiddle, (int)yMiddle, increment);
			/*
			int minX = (int)xMiddle - increment;
			int maxX = (int)xMiddle + increment;

			int minY = (int)yMiddle - increment;
			int maxY = (int)yMiddle + increment;

			if(minX < 0)
				minX = 0;
			if(maxX >= image.xDim)
				maxX = image.xDim - 1;

			if(minY < 0)
				minY = 0;
			if(maxY >= image.yDim)
				maxY = image.yDim - 1;

			for(int i = minX; i <= maxX; i++){
				jono.offer(image.get(i,minY));
				image.get(i,minY).setPriority( (image.get((int)xMiddle,(int)yMiddle).z - image.get(i,minY).z) );
				jono.offer(image.get(i, maxY));
				image.get(i,maxY).setPriority( (image.get((int)xMiddle,(int)yMiddle).z - image.get(i,maxY).z) );
			}

			for(int i = minY; i <= maxY; i++){
				jono.offer(image.get(minX, i));
				image.get(minX, i).setPriority( (image.get((int)xMiddle,(int)yMiddle).z - image.get(minX, i).z) );
				jono.offer(image.get(maxX, i));
				image.get(maxX, i).setPriority( (image.get((int)xMiddle,(int)yMiddle).z - image.get(maxX, i).z) );
			}
			//System.out.println(jono);
			//System.out.println("jono xise: " + jono.size());
			*/
			total = jono.size() + 1;
			//if(this.id == 62285)
			//	System.out.println(total);
			
		}

		public void newCircle(){

			increment++;
			//updateCircle((int)this.xMiddle, (int)yMiddle, increment);

		}

		public void updateCircle(int x, int y, int rad){

			int minX = (int)xMiddle - increment;
			int maxX = (int)xMiddle + increment;

			int minY = (int)yMiddle - increment;
			int maxY = (int)yMiddle + increment;

			if(minX < 0)
				minX = 0;
			if(maxX >= image.xDim)
				maxX = image.xDim - 1;

			if(minY < 0)
				minY = 0;
			if(maxY >= image.yDim)
				maxY = image.yDim - 1;

			for(int i = minX; i <= maxX; i++){
				for(int j = 0; j <= minY; j++){

					if(pointInCircle(i,j,(int)this.xMiddle, (int)this.yMiddle, increment)){
						if(image.get(i,j).id == -999)
							jono.offer(image.get(i,j));
					}

				}
			}

		}

		public boolean pointInCircle(double[] point, double[] plotCenter,double radi){

            return Math.sqrt(Math.pow(Math.abs(point[1] - plotCenter[1]), 2.0) + Math.pow(Math.abs(point[0] - plotCenter[0]), 2.0)) <= radi;
         
 		}

 		public boolean pointInCircle(int pointX, int pointY, int plotCenterX, int plotCenterY, int radi){

            return Math.sqrt(Math.pow(Math.abs(pointY - plotCenterY), 2.0) + Math.pow(Math.abs(pointX - plotCenterX), 2.0)) <= radi;
         
 		}

		private void drawCircle(int centerX, int centerY, int radius) {

			int d = 1 - radius;
			int x = 0;
			int y = radius;
			//Color circleColor = Color.white;
	 		
			if(centerX - radius < 0)
				return;
			if(centerX + radius >= image.xDim)
				return;

			if(centerY - radius < 0)
				return;
			if(centerY + radius >= image.yDim)
				return;


			do {
				if(image.get(centerX + x, centerY + y).id == -999){
                    image.get(centerX + x, centerY + y).setPriority((float) (-this.waterLevel - image.get(centerX + x, centerY + y).z) );
					jono.offer(image.get(centerX + x, centerY + y));
					if(image.get(centerX + x - 1, centerY + y - 1).id == -999){
                        image.get(centerX + x - 1, centerY + y - 1).setPriority( (float)(-this.waterLevel - image.get(centerX + x - 1, centerY + y - 1).z) );
						jono.offer(image.get(centerX + x - 1, centerY + y - 1));
					}
				}

				if(image.get(centerX + x, centerY - y).id == -999){
                    image.get(centerX + x, centerY - y).setPriority( (float)(-this.waterLevel - image.get(centerX + x, centerY - y).z) );
					jono.offer(image.get(centerX + x, centerY - y));
					if(image.get(centerX + x - 1, centerY - y + 1).id == -999){
                        image.get(centerX + x - 1, centerY - y + 1).setPriority( (float)(-this.waterLevel - image.get(centerX + x - 1, centerY - y + 1).z) );
						jono.offer(image.get(centerX + x - 1, centerY - y + 1));
					}
				}

				if(image.get(centerX - x, centerY + y).id == -999){
                    image.get(centerX - x, centerY + y).setPriority( (float)(-this.waterLevel - image.get(centerX - x, centerY + y).z) );
					jono.offer(image.get(centerX - x, centerY + y));
					if(image.get(centerX - x + 1, centerY + y - 1).id == -999){
                        image.get(centerX - x + 1, centerY + y - 1).setPriority( (float)(-this.waterLevel - image.get(centerX - x + 1, centerY + y - 1).z) );
						jono.offer(image.get(centerX - x + 1, centerY + y - 1));
					}
				}

				if(image.get(centerX - x, centerY - y).id == -999){
                    image.get(centerX - x, centerY - y).setPriority( (float)(-this.waterLevel - image.get(centerX - x, centerY - y).z) );
					jono.offer(image.get(centerX - x, centerY - y));
					if(image.get(centerX - x + 1, centerY - y + 1).id == -999){
                        image.get(centerX - x + 1, centerY - y + 1).setPriority( (float)(-this.waterLevel - image.get(centerX - x + 1, centerY - y + 1).z) );
						jono.offer(image.get(centerX - x + 1, centerY - y + 1));
					}
				}

				if(image.get(centerX + y, centerY + x).id == -999){
                    image.get(centerX + y, centerY + x).setPriority( (float)(-this.waterLevel - image.get(centerX + y, centerY + x).z) );
					jono.offer(image.get(centerX + y, centerY + x));
					if(image.get(centerX + y - 1, centerY + x - 1).id == -999){
                        image.get(centerX + y - 1, centerY + x - 1).setPriority( (float)(-this.waterLevel - image.get(centerX + y - 1, centerY + x - 1).z) );
						jono.offer(image.get(centerX + y - 1, centerY + x - 1));
					}
				}

				if(image.get(centerX + y, centerY - x).id == -999){
                    image.get(centerX + y, centerY - x).setPriority( (float)(-this.waterLevel - image.get(centerX + y, centerY - x).z) );
					jono.offer(image.get(centerX + y, centerY - x));
					if(image.get(centerX + y - 1, centerY - x + 1).id == -999){
                        image.get(centerX + y - 1, centerY - x + 1).setPriority( (float)(-this.waterLevel - image.get(centerX + y - 1, centerY - x + 1).z) );
						jono.offer(image.get(centerX + y - 1, centerY - x + 1));
					}
				}

				if(image.get(centerX - y, centerY + x).id == -999){
                    image.get(centerX - y, centerY + x).setPriority( (float)(-this.waterLevel - image.get(centerX - y, centerY + x).z) );
					jono.offer(image.get(centerX - y, centerY + x));
					if(image.get(centerX - y + 1, centerY + x - 1).id == -999){
                        image.get(centerX - y + 1, centerY + x - 1).setPriority( (float)(-this.waterLevel - image.get(centerX - y + 1, centerY + x - 1).z) );
						jono.offer(image.get(centerX - y + 1, centerY + x - 1));
					}
				}

				if(image.get(centerX - y, centerY - x).id == -999){
                    image.get(centerX - y, centerY - x).setPriority((float) (-this.waterLevel - image.get(centerX - y, centerY - x).z) );
					jono.offer(image.get(centerX - y, centerY - x));
					if(image.get(centerX - y + 1, centerY - x + 1).id == -999){
                        image.get(centerX - y + 1, centerY - x + 1).setPriority( (float)(-this.waterLevel - image.get(centerX - y + 1, centerY - x + 1).z) );
						jono.offer(image.get(centerX - y + 1, centerY - x + 1));
					}
				}


				if (d < 0) {
					d += 2 * x + 1;
				} else {
					d += 2 * (x - y) + 1;
					y--;
				}
				x++;
			} while (x <= y);
 
		}

        public boolean smallerThanSurrounding(Pixel p, Raster i, long id){

            Pixel px = null;

            int x = p.x;
            int y = p.y;

            px = i.get(x - 1, y - 1);

            if(px.id == id && px.z < p.z){
                return false;
            }    
            px = i.get(x - 0, y - 1);

            if(px.id == id && px.z < p.z){
                return false;
            }
            px = i.get(x + 1, y - 1);

            if(px.id == id && px.z < p.z){
                return false;
            }
            px = i.get(x + 1, y - 0);

            if(px.id == id && px.z < p.z){
                return false;
            }
            px = i.get(x + 1, y + 1);

            if(px.id == id && px.z < p.z){
                return false;
            }
            px = i.get(x + 0, y + 1);

            if(px.id == id && px.z < p.z){
                return false;
            }
            px = i.get(x - 1, y + 1);

            if(px.id == id && px.z < p.z){
                return false;
            }
            px = i.get(x - 1, y + 0);

            return px.id != id || !(px.z < p.z);

        }

        public void revertPrevious(double thresHold){

            for(int i = 0; i < previous.size(); i++){

                //if(image.get((int)previous.get(i)[0], (int)previous.get(i)[1]).id == 1762)
                   //System.out.println("!!!!");

                if(previous.get(i)[2] > thresHold){

                    image.get((int)previous.get(i)[0], (int)previous.get(i)[1]).detach();

                }

            }

            previous.clear();

        }

        public void updateMidPoint(){

            double maximum = Double.NEGATIVE_INFINITY;

            for(int i = 0; i < previous.size(); i++){

                if(image.get((int)previous.get(i)[0], (int)previous.get(i)[1]).z > maximum){

                    maximum = image.get((int)previous.get(i)[0], (int)previous.get(i)[1]).z;

                    this.xMiddle = image.get((int)previous.get(i)[0], (int)previous.get(i)[1]).x;
                    this.yMiddle = image.get((int)previous.get(i)[0], (int)previous.get(i)[1]).y;
                    //this.zMiddle = image.get((int)previous.get(i)[0], (int)previous.get(i)[1]).z;
                }

            }
            
            increment = 1;

        }

		public boolean iteration(){

            if(missedIterations > 3){
                //System.out.println("TERMINATE! " + this.waterLevel + " " + this.zMiddleOrig);
                this.done = true;
                return false;
            }



			if(jono.size() == 0){

                numberOfIterations++;

                if(this.higherThanWaterLevel > countLastIteration * 0.75 && numberOfIterations > 2){

                    revertPrevious(meanZlast);
                    
                    if(tryNumber < maxTries){
                        tryNumber++;
                        updateMidPoint();

                    }
                    else{
                        this.done = true;
                        return false; 
                    }
                }

                previous.clear();

                if(countLastIteration > 0){

                    meanZlast = sumZLastIteration / (double)countLastIteration;

                    //this.waterLevel = -meanZlast;
                
                }

                missedIterations++;

                prevIterWaterLevel = this.waterLevel;
                sumZLastIteration = 0.0;

                countLastIteration = 0;

                higherThanWaterLevel = 0;
                iterationCount = 0;

				newBorder();

			}
			
            

			Pixel temp = null;

			temp = jono.poll();

            iterationCount++;

            if(temp != null) 
            if(!temp.reject.contains(this.id)){

                //if(temp.id == 39)
                    //System.out.println(-temp.z + " water: " + waterLevel + " asd: " + (-temp.z >= this.waterLevel + 1.5));

                //if(-temp.z >= (this.waterLevel + 1.5)){
                    //System.out.println(temp.reject.size());
                  //  temp.reject(this.id);
                //}
                //System.out.println(temp.z + " " + this.waterLevel);

                if((temp.id == -999) && temp.z < this.zMiddleOrig)
                if(numberOfIterations < 2 || connectedALL(temp.x, temp.y, temp.z)){    

        			if(temp != null && temp.z > 2 && temp.x > 0 && temp.x < (this.image.xDim - 1) &&
        				temp.y > 0 && temp.y < (this.image.yDim - 1)){ //  || temp.id == this.id

        				///if(true){
        				if(pointInCircle(temp.x, temp.y, (int)xMiddle, (int)yMiddle,
        						(int)(this.minDistance * 4.5))){

        					this.first = false;
        					
        					if(temp.x < this.minX)
        						this.minX = temp.x;

        					if(temp.x > this.maxX)
        						this.maxX = temp.x;

        					if(temp.y < this.minY)
        						this.minY = temp.y;

        					if(temp.y > this.maxY)
        						this.maxY = temp.y;
        					

        					
        					if(temp.y >= this.maxY && temp.x >= this.maxX){
        						this.koillis[0] = temp.x; 
        						this.koillis[1] = temp.y; 
        					}

        					if(temp.y <= this.minY && temp.x >= this.maxX){
        						this.kaakkois[0] = temp.x; 
        						this.kaakkois[1] = temp.y; 
        					}

        					if(temp.y <= this.minY && temp.x <= this.minX){
        						this.lounais[0] = temp.x; 
        						this.lounais[1] = temp.y; 
        					}

        					if(temp.y >= this.maxY && temp.x <= this.minX){
        						this.luoteis[0] = temp.x; 
        						this.luoteis[1] = temp.y; 
        					}

        					cellCount++;

        					temp.attach(this.id);

                            countLastIteration++;

                            //System.out.println(-this.meanZlast + " " + -temp.z);

                            previous.add(new double[]{temp.x, temp.y, temp.z});

        					image.get(temp.x, temp.y).attach(this.id);
                            
                            sumZLastIteration += temp.z;

                            if(-this.meanZlast + 0.0 > -temp.z)
                                higherThanWaterLevel++;

                            
                            if(this.waterLevel < -temp.z)
                                this.waterLevel = -temp.z;
                            
                            missedIterations = 0;

        					return true;
        				}
                        else{

                           // System.out.println("NOT IN CIRCLE");

                        }
        				
        			}
                }
                else{

                    //System.out.println("rejected because: " + );

                }
            }

			if(temp != null)
				if(euclideanDistance(this.xMiddle, this.yMiddle, temp.x, temp.y) < this.minDistance && 
				euclideanDistance(this.xMiddle, this.yMiddle, temp.x, temp.y) > 2)
					this.minDistance = euclideanDistance(this.xMiddle, this.yMiddle, temp.x, temp.y);

			missed++;
				
			return false;

		}

        public boolean iteration2(){

            if(jono.size() == 0){

                this.done = true;
                return false;

            }

            Pixel temp = null;

            temp = jono.poll();

            iterationCount++;

            if(euclideanDistance(temp.x, temp.y, this.xMiddle, this.yMiddle) <= (int)(3.0 / 0.25) && connectedALL(temp.x, temp.y, temp.z))
            if(temp != null && (temp.id == -999) && temp.z < this.zMiddle)
                if(temp != null && temp.z > 2 && temp.x > 0 && temp.x < (this.image.xDim - 1) &&
                    temp.y > 0 && temp.y < (this.image.yDim - 1)){ //  || temp.id == this.id

                    
                    //if(pointInCircle((int)temp.x, (int)temp.y, (int)xMiddle, (int)yMiddle, 
                           // (int)(this.minDistance * 2.0))){

                        this.first = false;
                        
                        if(temp.x < this.minX)
                            this.minX = temp.x;

                        if(temp.x > this.maxX)
                            this.maxX = temp.x;

                        if(temp.y < this.minY)
                            this.minY = temp.y;

                        if(temp.y > this.maxY)
                            this.maxY = temp.y;
                        

                        
                        if(temp.y >= this.maxY && temp.x >= this.maxX){
                            this.koillis[0] = temp.x; 
                            this.koillis[1] = temp.y; 
                        }

                        if(temp.y <= this.minY && temp.x >= this.maxX){
                            this.kaakkois[0] = temp.x; 
                            this.kaakkois[1] = temp.y; 
                        }

                        if(temp.y <= this.minY && temp.x <= this.minX){
                            this.lounais[0] = temp.x; 
                            this.lounais[1] = temp.y; 
                        }

                        if(temp.y >= this.maxY && temp.x <= this.minX){
                            this.luoteis[0] = temp.x; 
                            this.luoteis[1] = temp.y; 
                        }

                        cellCount++;

                        temp.attach(this.id);

                        countLastIteration++;

                        //System.out.println(-this.meanZlast + " " + -temp.z);

                        previous.add(new double[]{temp.x, temp.y, temp.z});

                        image.get(temp.x, temp.y).attach(this.id);
                        
                        sumZLastIteration += temp.z;

                        //if(-this.meanZlast + 0.0 > -temp.z)
                          //  higherThanWaterLevel++;

                        
                        if(this.waterLevel < -temp.z)
                            this.waterLevel = -temp.z;
                        
                        missedIterations = 0;

                        //this.updateNeighbourhood((int)temp.x, (int)temp.y, 5.5);

                        this.numberOfPixels++;

                        this.area += (this.image.resolution * this.image.resolution);
                        System.out.println(area);
                        this.sumZ += temp.z;

                        //this.waterLevel = -(double)(this.numberOfPixels / this.sumZ);

                        return true;
                    //}
                    
                }

            missed++;
                
            return false;

        }
		/*
		public boolean isPart(double x, double y){

			return (this.image.get(x - 1, y).id == )

		}
		*/

        public boolean connectedALL(double x, double y, double z){
            //System.out.println("GOT HERE");
            double allowedDrop = 1.5;

            boolean output1 = ( image.get((int)x - 1, (int)y).id == this.id || image.get((int)x + 1, (int)y).id == this.id || 
                image.get((int)x, (int)y - 1).id == this.id || image.get((int)x, (int)y + 1).id == this.id);

            boolean output2 = ( image.get((int)x - 1, (int)y).id == this.id || image.get((int)x + 1, (int)y).id == this.id || 
                image.get((int)x, (int)y - 1).id == this.id || image.get((int)x, (int)y + 1).id == this.id);

            boolean drop = ( image.get((int)x, (int)y).z > (this.zMiddle - 15.5 ));

            boolean asdi1 =  (image.get((int)x - 1, (int)y).id == this.id && image.get((int)x - 1, (int)y).z < z);
            boolean asdi2 =  (image.get((int)x + 1, (int)y).id == this.id && image.get((int)x + 1, (int)y).z < z);
            boolean asdi3 =  (image.get((int)x, (int)y - 1).id == this.id && image.get((int)x, (int)y - 1).z < z);
            boolean asdi4 =  (image.get((int)x, (int)y + 1).id == this.id && image.get((int)x, (int)y + 1).z < z);

            boolean asdi5 =  (image.get((int)x - 1, (int)y).id == this.id && image.get((int)x - 1, (int)y).z < z);
            boolean asdi6 =  (image.get((int)x + 1, (int)y).id == this.id && image.get((int)x + 1, (int)y).z < z);
            boolean asdi7 =  (image.get((int)x, (int)y - 1).id == this.id && image.get((int)x, (int)y - 1).z < z);
            boolean asdi8 =  (image.get((int)x, (int)y + 1).id == this.id && image.get((int)x, (int)y + 1).z < z);

            double mean =   ((image.get((int)x - 1, (int)y).id == this.id) ? 1 : 0) * image.get((int)x - 1, (int)y).z +
                            ((image.get((int)x + 1, (int)y).id == this.id) ? 1 : 0) * image.get((int)x + 1, (int)y).z +
                            ((image.get((int)x, (int)y - 1).id == this.id) ? 1 : 0) * image.get((int)x, (int)y - 1).z +
                            ((image.get((int)x, (int)y + 1).id == this.id) ? 1 : 0) * image.get((int)x, (int)y + 1).z +

                            ((image.get((int)x - 1, (int)y).id == this.id) ? 1 : 0) * image.get((int)x - 1, (int)y).z +
                            ((image.get((int)x + 1, (int)y).id == this.id) ? 1 : 0) * image.get((int)x + 1, (int)y).z +
                            ((image.get((int)x, (int)y - 1).id == this.id) ? 1 : 0) * image.get((int)x, (int)y - 1).z +
                            ((image.get((int)x, (int)y + 1).id == this.id) ? 1 : 0) * image.get((int)x, (int)y + 1).z;

            double counti = ((image.get((int)x - 1, (int)y).id == this.id) ? 1 : 0) +
                            ((image.get((int)x + 1, (int)y).id == this.id) ? 1 : 0) +
                            ((image.get((int)x, (int)y - 1).id == this.id) ? 1 : 0) +
                            ((image.get((int)x, (int)y + 1).id == this.id) ? 1 : 0) +

                            ((image.get((int)x - 1, (int)y).id == this.id) ? 1 : 0) +
                            ((image.get((int)x + 1, (int)y).id == this.id) ? 1 : 0) +
                            ((image.get((int)x, (int)y - 1).id == this.id) ? 1 : 0) +
                            ((image.get((int)x, (int)y + 1).id == this.id) ? 1 : 0);

            mean = mean / counti;

            int count = (asdi1 ? 1 : 0) + (asdi2 ? 1 : 0) + (asdi3 ? 1 : 0) + (asdi4 ? 1 : 0) + (asdi5 ? 1 : 0) + (asdi6 ? 1 : 0) + (asdi7 ? 1 : 0) + (asdi8 ? 1 : 0);

            //System.out.println(this.zMiddle);
            return (output1 && output2 && drop);// && z < (mean + 1.5) && z > (mean - 1.5));

        }

		public boolean connected(double x, double y, double z){
			//System.out.println("GOT HERE");
			double allowedDrop = 1.5;

			boolean output = ( image.get((int)x - 1, (int)y).id == this.id || image.get((int)x + 1, (int)y).id == this.id || 
				image.get((int)x, (int)y - 1).id == this.id || image.get((int)x, (int)y + 1).id == this.id);

			boolean drop = ( image.get((int)x, (int)y).z > (this.zMiddle - 15.5 ));

            boolean asdi1 =  (image.get((int)x - 1, (int)y).id == this.id && image.get((int)x - 1, (int)y).z < z);
            boolean asdi2 =  (image.get((int)x + 1, (int)y).id == this.id && image.get((int)x + 1, (int)y).z < z);
            boolean asdi3 =  (image.get((int)x, (int)y - 1).id == this.id && image.get((int)x, (int)y - 1).z < z);
            boolean asdi4 =  (image.get((int)x, (int)y + 1).id == this.id && image.get((int)x, (int)y + 1).z < z);

            int count = (asdi1 ? 1 : 0) + (asdi2 ? 1 : 0) + (asdi3 ? 1 : 0) + (asdi4 ? 1 : 0);

			//System.out.println(this.zMiddle);
			return (output && drop && count <= 1);

		}

		public boolean DIAGconnected(double x, double y, double z){
			//System.out.println("GOT HERE");
			double allowedDrop = 1.5;

			boolean output = ( image.get((int)x - 1, (int)y - 1).id == this.id || image.get((int)x + 1, (int)y - 1).id == this.id || 
				image.get((int)x + 1, (int)y + 1).id == this.id || image.get((int)x - 1, (int)y + 1).id == this.id);

			boolean drop = ( image.get((int)x, (int)y).z > (this.zMiddle - 15.5 ));

            boolean asdi1 =  (image.get((int)x - 1, (int)y - 1).id == this.id && image.get((int)x - 1, (int)y - 1).z < z);
            boolean asdi2 =  (image.get((int)x + 1, (int)y - 1).id == this.id && image.get((int)x + 1, (int)y - 1).z < z);
            boolean asdi3 =  (image.get((int)x + 1, (int)y + 1).id == this.id && image.get((int)x + 1, (int)y + 1).z < z);
            boolean asdi4 =  (image.get((int)x - 1, (int)y + 1).id == this.id && image.get((int)x - 1, (int)y + 1).z < z);

            int count = (asdi1 ? 1 : 0) + (asdi2 ? 1 : 0) + (asdi3 ? 1 : 0) + (asdi4 ? 1 : 0);
                            			//System.out.println(this.zMiddle);
			return (output && drop && count <= 2);

		}

        public boolean smaller(double x, double y){

            return true;

        }

		public void printQ(){

			for(Pixel i : jono){

				System.out.println(i);

			}

		}
	}


	public static class WaterShed{


        float[] floatArray3x3 = new float[9];
        float[] floatArray3x3_2 = new float[9];
        float[] floatArray3x3_3 = new float[9];

		long filledPixels = 0;
		long totPixels = 0;

        float replaceValue = 0.0f;

		chm canopy;

		double speed;

		//HashMap<Long,HashSet<double[]>> data = new HashMap<>();

		public HashSet<double[]> treeTops = new HashSet<double[]>();

		float[][] raster = null;

		Dataset raster_ds = null;
		Band raster_band = null;

		Dataset waterRaster_ds = null;
		Band waterRaster_band = null;


		float[][] waterRaster = null;

		double waterLevel = 0.0;

		double minLevel = 2.0;

		int xDim;
		int yDim;

		Raster image;
        Raster imageOrig;

		ArrayList<WaterBody> altaat = new ArrayList<WaterBody>();

        PriorityQueue<WaterBody> jono = new PriorityQueue<WaterBody>(5, new CheckerWater());

        PriorityQueue<Pixel> jono2 = new PriorityQueue<Pixel>(5, new Checker());
        PriorityQueue<cellItem> jono2_tif = new PriorityQueue<>(5, new CheckerCellItem());

        HashSet<Long> pixelBank = new HashSet<Long>();
        ArrayList<int[]> treeTopLocations = new ArrayList<int[]>();

        int coreNumber = 0;

        float[] floatArray = new float[1];

        argumentReader aR;

        ArrayList<double[][]> polyBank = new ArrayList<>();

        HashMap<Integer, Float> waterbodyAreas = new HashMap<>();

		public WaterShed(){


		}

		public WaterShed(HashSet<double[]> in, double speed2, Dataset inMat, chm inChm, argumentReader aR, int coreNumber) throws IOException{

		    this.polyBank = aR.polyBank;
		    this.treeTops = in;

		    this.coreNumber = coreNumber;
		    this.aR = aR;
			canopy = inChm;
/*
			this.raster = inMat.clone();
			this.waterRaster = inMat.clone();
*/
			this.raster_ds = inMat;
			this.raster_band = this.raster_ds.GetRasterBand(1);


			this.xDim = raster_band.getXSize();
			this.yDim = raster_band.getYSize();

            waterRaster_ds = copyRaster(raster_ds, waterRaster_ds, "tempWater" + coreNumber + ".tif");

			waterRaster_band = waterRaster_ds.GetRasterBand(1);
			this.speed = speed2;

			image = new Raster(this.raster_ds, coreNumber, aR.step);
            //imageOrig = new Raster(canopy.original);

			totPixels = inMat.GetRasterXSize() * inMat.getRasterYSize();

			//Cantor homma = new Cantor();

			int tempId;

            int aidee = 0;

            aR.p_update.threadFile[0] = "waterShed";
            aR.p_update.threadProgress[coreNumber-1] = -1;
            aR.p_update.threadEnd[coreNumber-1] = -1;
            aR.p_update.updateProgressITD();
            

			for(double[] key : in){

				//tempId = homma.pair((long)key[0], (long)key[1]);

				//data.put((long)aidee, new HashSet<double[]>());
				//data.get((long)aidee).add(key);

				//waterRaster.put((int)key[1], (int)key[0], (long)key[0], (long)key[1]);

                floatArray[0] = 0.0f;

                waterRaster_band.WriteRaster((int)key[0], (int)key[1], 1, 1, floatArray);

                //waterRaster[(int)key[0]][(int)key[1]] = 0.0f;

				altaat.add(new WaterBody(aidee, key[0], key[1], key[2], image ));

				waterbodyAreas.put(aidee, 0.0f);

                filledPixels++;

                image.attach( (int)key[0], (int)key[1], aidee);

				//image.get( (int)key[0], (int)key[1]).attach(aidee);

                //image.treeTop((int)key[0], (int)key[1]);
                //image.get( (int)key[0], (int)key[1]).treeTop();

                treeTopLocations.add(new int[]{(int)key[0], (int)key[1]});
                //if(aidee == 4)
                  //  System.out.println(key[0] + " " + key[1] + " " + key[2]);
                aidee++;

                //pixelBank.add(image.get( (int)key[0], (int)key[1]).id);

               // image.get( (int)key[0], (int)key[1]).setPriority(gradientAt((int)key[0], (int)key[1]));
                image.priority((int)key[0], (int)key[1], (float)-key[2]);
                //image.get( (int)key[0], (int)key[1]).setPriority((float)-key[2]);

                //image.get( (int)key[0], (int)key[1]).lock();

                //jono2.offer(image.get((int)key[0], (int)key[1]));
                jono2_tif.offer(new cellItem((int)key[0], (int)key[1], (float)-key[2]));

                image.queue((int)key[0], (int)key[1]);
                //image.get((int)key[0], (int)key[1]).queue();

                //this.updateNeighbourhood((int)key[0], (int)key[1], 10000, false);

			}

            for(int i = 0; i < altaat.size(); i++)
                jono.offer(altaat.get(i));

            aR.p_update.updateProgressITD();
			start2();
            aR.p_update.updateProgressITD();


			export();

		}

		public WaterShed(double speed2){

			this.speed = speed;

		}
			



		public void start(){

            /* TODO: Order altaat in the order of water level! */

			//System.out.println(totPixels + " " + filledPixels);
			double prevProgress = -99;

			//while( ((double)filledPixels / (double)totPixels) > prevProgress){
            while( jono.size() > 0 ){


				prevProgress = ((double)filledPixels / (double)totPixels);
				//System.out.println(((double)filledPixels / (double)totPixels));
				//for(int i = 0; i < altaat.size(); i++){

                WaterBody tempBody = jono.poll();

                //System.out.println(tempBody.id);
                if(!tempBody.done)
                    if(tempBody.iteration2())
                        filledPixels++;

                if(!tempBody.done)
                    jono.offer(tempBody);
                /*
				if(altaat.get(i).done == false)
					if(altaat.get(i).iteration())
						filledPixels++;
                */
				//}	

			}

		}

        public double euclideanDistance(int x1, int y1, int x2, int y2){

            return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

        }

        public void start2(){

            /* TODO: Order altaat in the order of water level! */

            //System.out.println(totPixels + " " + filledPixels);
            double prevProgress = -99;

            int count = 0; 

            //while( ((double)filledPixels / (double)totPixels) > prevProgress){
            while( jono2_tif.size() > 0){

                //System.out.println(jono2_tif.size());
                //Pixel tempPixel = jono2.poll();

                cellItem ci = jono2_tif.poll();
                /* We label the pixel with the SEED closest to it (euc distance) */

                //this.updateNeighbourhood((int)tempPixel.x, (int)tempPixel.y, 10000, false);
                this.updateNeighbourhood_tif(ci.x, ci.y, 10000, false);
            }

        }

        public long labelOrNot(int x, int y){

            long label = 0L;

            HashMap<Integer, Integer> t = new HashMap<Integer, Integer>();

            if(image.get(x - 1, y).id != -999){

                if(!t.containsKey(image.get(x - 1, y).id))
                    t.put(image.get(x - 1, y).id, 1);
                else
                    t.put(image.get(x - 1, y).id, t.get(image.get(x - 1, y).id) + 1);

            }

            if(image.get(x + 1, y).id != -999){

                if(!t.containsKey(image.get(x + 1, y).id))
                    t.put(image.get(x + 1, y).id, 1);
                else
                    t.put(image.get(x + 1, y).id, t.get(image.get(x + 1, y).id) + 1);

            }
            if(image.get(x, y - 1).id != -999){

                if(!t.containsKey(image.get(x, y - 1).id))
                    t.put(image.get(x, y - 1).id, 1);
                else
                    t.put(image.get(x, y - 1).id, t.get(image.get(x, y - 1).id) + 1);

            }
            if(image.get(x, y + 1).id != -999){

                if(!t.containsKey(image.get(x, y + 1).id))
                    t.put(image.get(x, y + 1).id, 1);
                else
                    t.put(image.get(x, y + 1).id, t.get(image.get(x, y + 1).id) + 1);

            }
            if(image.get(x - 1, y - 1).id != -999){

                if(!t.containsKey(image.get(x - 1, y - 1).id))
                    t.put(image.get(x - 1, y - 1).id, 1);
                else
                    t.put(image.get(x - 1, y - 1).id, t.get(image.get(x - 1, y - 1).id) + 1);

            }
            if(image.get(x + 1, y - 1).id != -999){

                if(!t.containsKey(image.get(x + 1, y - 1).id))
                    t.put(image.get(x + 1, y - 1).id, 1);
                else
                    t.put(image.get(x + 1, y - 1).id, t.get(image.get(x + 1, y - 1).id) + 1);

            }
            if(image.get(x - 1, y + 1).id != -999){

                if(!t.containsKey(image.get(x - 1, y + 1).id))
                    t.put(image.get(x - 1, y + 1).id, 1);
                else
                    t.put(image.get(x - 1, y + 1).id, t.get(image.get(x - 1, y + 1).id) + 1);

            }
            if(image.get(x + 1, y + 1).id != -999){

                if(!t.containsKey(image.get(x + 1, y + 1).id))
                    t.put(image.get(x + 1, y + 1).id, 1);
                else
                    t.put(image.get(x + 1, y + 1).id, t.get(image.get(x + 1, y + 1).id) + 1);

            }
            //System.out.println(idsT.size());

            int maxInt = -1;
            int output = 0;

            double minDistance = Double.POSITIVE_INFINITY;
            double distance = 0.0;



            if(image.get(x, y).id == -999)

                if(t.size() > 0){

                    for(int key : t.keySet()){

                        //if(key == 4 && t.size() > 1)
                          //  System.out.println(distance);

                        distance = euclideanDistance(treeTopLocations.get(key)[0], treeTopLocations.get(key)[1],
                                    x, y);
                        //System.out.println(distance + " " + t.size());
                        /*
                        if(t.get(key) > maxInt){

                            maxInt = t.get(key);
                            output = key;

                        }
                        */
                        if( distance < minDistance){

                            minDistance = distance;
                            output = key;

                        }
                    }   

                    if(image.get(x, y).z >= altaat.get(output).zMiddle * 0.25 && distance < 12){
                        image.get(x, y).attach(output);
                        altaat.get(output).add(image.get(x, y));

                        System.out.println("Area: " + altaat.get(output).area());
                        image.get(x, y).dequeue();
                    }

                    return output;

                }


            return -9876L;

        }

        public long labelOrNot_tif(float[] in, int x, int y){

            HashMap<Integer, Integer> t = new HashMap<Integer, Integer>();

            int nono = 5;

            for(int i = 0; i < in.length; i++){

                if((int)in[i] != -999) {

                    if (!t.containsKey((int) in[i])) {
                        t.put((int) in[i], 1);
                    } else {
                        t.put((int) in[i], t.get((int) in[i]) + 1);

                    }
                }
            }
/*
            if(image.get(x - 1, y).id != -999){

                if(!t.containsKey(image.get(x - 1, y).id))
                    t.put(image.get(x - 1, y).id, 1);
                else
                    t.put(image.get(x - 1, y).id, t.get(image.get(x - 1, y).id) + 1);

            }

            if(image.get(x + 1, y).id != -999){

                if(!t.containsKey(image.get(x + 1, y).id))
                    t.put(image.get(x + 1, y).id, 1);
                else
                    t.put(image.get(x + 1, y).id, t.get(image.get(x + 1, y).id) + 1);

            }
            if(image.get(x, y - 1).id != -999){

                if(!t.containsKey(image.get(x, y - 1).id))
                    t.put(image.get(x, y - 1).id, 1);
                else
                    t.put(image.get(x, y - 1).id, t.get(image.get(x, y - 1).id) + 1);

            }
            if(image.get(x, y + 1).id != -999){

                if(!t.containsKey(image.get(x, y + 1).id))
                    t.put(image.get(x, y + 1).id, 1);
                else
                    t.put(image.get(x, y + 1).id, t.get(image.get(x, y + 1).id) + 1);

            }
            if(image.get(x - 1, y - 1).id != -999){

                if(!t.containsKey(image.get(x - 1, y - 1).id))
                    t.put(image.get(x - 1, y - 1).id, 1);
                else
                    t.put(image.get(x - 1, y - 1).id, t.get(image.get(x - 1, y - 1).id) + 1);

            }
            if(image.get(x + 1, y - 1).id != -999){

                if(!t.containsKey(image.get(x + 1, y - 1).id))
                    t.put(image.get(x + 1, y - 1).id, 1);
                else
                    t.put(image.get(x + 1, y - 1).id, t.get(image.get(x + 1, y - 1).id) + 1);

            }
            if(image.get(x - 1, y + 1).id != -999){

                if(!t.containsKey(image.get(x - 1, y + 1).id))
                    t.put(image.get(x - 1, y + 1).id, 1);
                else
                    t.put(image.get(x - 1, y + 1).id, t.get(image.get(x - 1, y + 1).id) + 1);

            }
            if(image.get(x + 1, y + 1).id != -999){

                if(!t.containsKey(image.get(x + 1, y + 1).id))
                    t.put(image.get(x + 1, y + 1).id, 1);
                else
                    t.put(image.get(x + 1, y + 1).id, t.get(image.get(x + 1, y + 1).id) + 1);

            }

 */
            //System.out.println(idsT.size());

            int maxInt = -1;
            int output = 0;

            double minDistance = Double.POSITIVE_INFINITY;
            double distance = 0.0;


            if((int)in[4] == -999){

                if(t.size() > 0){
                    for(int key : t.keySet()){

                        distance = euclideanDistance(treeTopLocations.get(key)[0], treeTopLocations.get(key)[1],
                                x, y);

                        if( distance < minDistance){

                            minDistance = distance;
                            output = key;

                        }
                    }

                    image.raster_z_b.ReadRaster(x, y, 1, 1, floatArray);

                    if((floatArray[0] >= altaat.get(output).zMiddle * 0.2 || floatArray[0] > 2.0) && distance < 12){

                        //System.out.println("ATTACHED!");
                        image.attach(x, y, output);
                        //image.get(x, y).attach(output);
                        altaat.get(output).add(new cellItem(x, y, 0.0f));

                        //System.out.println("Area: " + altaat.get((int)output).area());
                        image.dequeue(x, y);
                        waterbodyAreas.put(output, waterbodyAreas.get(output) + (float)(this.image.resolution * this.image.resolution));
                        //image.get(x, y).dequeue();
                    }

                    return output;
                }
            }
/*
            if(image.get(x, y).id == -999)

                if(t.size() > 0){

                    for(int key : t.keySet()){

                        //if(key == 4 && t.size() > 1)
                        //  System.out.println(distance);

                        distance = euclideanDistance(treeTopLocations.get((int)key)[0], treeTopLocations.get((int)key)[1],
                                x, y);
                        //System.out.println(distance + " " + t.size());
                        /*
                        if(t.get(key) > maxInt){

                            maxInt = t.get(key);
                            output = key;

                        }

                        if( distance < minDistance){

                            minDistance = distance;
                            output = key;

                        }
                    }

                    if(image.get(x, y).z >= altaat.get((int)output).zMiddle * 0.25 && distance < 12){
                        image.get(x, y).attach(output);
                        altaat.get((int)output).add(image.get(x, y));

                        //System.out.println("Area: " + altaat.get((int)output).area());
                        image.get(x, y).dequeue();
                    }

                    return output;

                }
            */

            return -9876L;

        }

        public boolean insidePolygons(double[] haku){

		    if(polyBank.size() == 0)
		        return true;

		    for(int i = 0; i < polyBank.size(); i++){
		        if(pointInPolygon(haku, polyBank.get(i)))
		            return true;
            }

		    return false;
        }

        public static boolean pointInPolygon(double[] point, double[][] poly) {


            int numPolyCorners = poly.length;
            int j = numPolyCorners - 1;
            boolean isInside = false;

            for (int i = 0; i < numPolyCorners; i++) {
                if (poly[i][1] < point[1] && poly[j][1] >= point[1] || poly[j][1] < point[1] && poly[i][1] >= point[1]) {
                    if (poly[i][0] + (point[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) * (poly[j][0] - poly[i][0]) < point[0]) {
                        isInside = !isInside;
                    }
                }
                j = i;
            }
            return isInside;
        }


        public void updateNeighbourhood(int x, int y, double bufferi, boolean giveLabel){

            double buffer = bufferi;

            long[] ids = new long[8];

            //double z_treetop = altaat.get((int)image.get(x,y).id).zMiddle;



            double zThreshold = 2.0;

            //zThreshold = z_treetop * 0.25;

            //if(giveLabel)

            //HashSet<Long> idsT = new HashSet<Long>();

            if(labelOrNot(x, y) != -9876 || image.get(x,y).id != -999)

            if(x >= 0 && x < image.xDim && y >= 0 && y < image.yDim){

                //if(!giveLabel)
                //System.out.println(image.get(x - 1, y).inQue);

                if(!image.get(x - 1, y).inQue && image.get(x - 1, y).id == -999 && image.get(x - 1, y).z > zThreshold){

                    //if((image.get(x,y).z - image.get(x - 1, y).z) >= (0 - buffer)){/

                        //image.get(x - 1, y).setPriority( gradientAt(x - 1, y ));
                        image.get(x - 1, y).setPriority( -image.get(x - 1, y ).z);


                        if(giveLabel){
                            image.get(x - 1, y).attach(image.get(x,y).id);
                            image.get(x - 1, y).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x - 1, y));
                            image.get(x - 1, y).queue();

                        }

                        //System.out.println(image.get(x - 1, y).id);

                        ids[0] = image.get(x - 1, y).id;

                   // }
                }

                if(!image.get(x + 1, y).inQue && image.get(x + 1, y).id == -999 && image.get(x + 1, y).z > zThreshold){

                    if((image.get(x,y).z - image.get(x + 1, y).z) >= (0 - buffer)){

                        //image.get(x + 1, y).setPriority( gradientAt(x + 1, y) );
                        image.get(x + 1, y).setPriority( -image.get(x + 1, y).z );

                        if(giveLabel){
                            image.get(x + 1, y).attach(image.get(x,y).id);
                            image.get(x + 1, y).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x + 1, y));
                            image.get(x + 1, y).queue();

                        }

                        ids[1] = image.get(x + 1, y).id;
                    }
                }
                

                if(!image.get(x, y - 1).inQue && image.get(x, y - 1).id == -999 && image.get(x, y - 1).z > zThreshold){

                    //if((image.get(x,y).z - image.get(x, y - 1).z) >= (0 - buffer)){

                        //image.get(x, y - 1).setPriority( gradientAt(x, y - 1) );
                        image.get(x, y - 1).setPriority( -image.get(x, y - 1).z );

                        if(giveLabel){
                            image.get(x, y - 1).attach(image.get(x,y).id);
                            image.get(x, y - 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x, y - 1));
                            image.get(x, y - 1).queue();

                        }


                        ids[2] = image.get(x, y - 1).id;
                    //}
                }
                

                if(!image.get(x, y + 1).inQue && image.get(x, y + 1).id == -999 && image.get(x, y + 1).z > zThreshold){

                    //if((image.get(x,y).z - image.get(x, y + 1).z) >= (0 - buffer)){

                        //image.get(x, y + 1).setPriority( gradientAt(x, y + 1) );
                        image.get(x, y + 1).setPriority( -image.get(x, y + 1).z );
 
                        if(giveLabel){
                            image.get(x, y + 1).attach(image.get(x,y).id);
                            image.get(x, y + 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x, y + 1));
                            image.get(x, y + 1).queue();
                        }

                        ids[3] = image.get(x, y + 1).id;
                   // }
                }
                

                
                if(!image.get(x - 1, y - 1).inQue && image.get(x - 1, y - 1).id == -999 && image.get(x - 1, y - 1).z > zThreshold){


                    //if((image.get(x,y).z - image.get(x - 1, y - 1).z) >= (0 - buffer)){

                        //image.get(x - 1, y - 1).setPriority( gradientAt(x - 1, y - 1) );
                        image.get(x - 1, y - 1).setPriority( -image.get(x - 1, y - 1).z );

                        if(giveLabel){
                            image.get(x - 1, y - 1).attach(image.get(x,y).id);
                            image.get(x - 1, y - 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x - 1, y - 1));
                            image.get(x - 1, y - 1).queue();
                        }

                        ids[4] = image.get(x - 1, y - 1).id;
                    //}
                }
                

                if(!image.get(x + 1, y - 1).inQue && image.get(x + 1, y - 1).id == -999 && image.get(x + 1, y - 1).z > zThreshold){

                   // if((image.get(x,y).z - image.get(x + 1, y - 1).z) >= (0 - buffer)){

                        //image.get(x + 1, y - 1).setPriority( gradientAt(x + 1, y - 1) );
                        image.get(x + 1, y - 1).setPriority( -image.get(x + 1, y - 1).z );

                        if(giveLabel){
                            image.get(x + 1, y - 1).attach(image.get(x,y).id);
                            image.get(x + 1, y - 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x + 1, y - 1));
                            image.get(x + 1, y - 1).queue();
                        }

                        ids[5] = image.get(x + 1, y - 1).id;
                    //}
                }
                

                if(!image.get(x - 1, y + 1).inQue && image.get(x - 1, y + 1).id == -999 && image.get(x - 1, y + 1).z > zThreshold){

                    //if((image.get(x,y).z - image.get(x - 1, y - 1).z) >= (0 - buffer)){

                        //image.get(x - 1, y + 1).setPriority( gradientAt(x - 1, y + 1) );
                        image.get(x - 1, y + 1).setPriority( -image.get(x - 1, y + 1).z );

                        if(giveLabel){
                            image.get(x - 1, y + 1).attach(image.get(x,y).id);
                            image.get(x - 1, y + 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x - 1, y + 1));
                            image.get(x - 1, y + 1).queue();
                        }

                        ids[6] = image.get(x - 1, y + 1).id;
                    //}
                }
                

                if(!image.get(x + 1, y + 1).inQue && image.get(x + 1, y + 1).id == -999 && image.get(x + 1, y + 1).z > zThreshold){

                    //if((image.get(x,y).z - image.get(x + 1, y + 1).z) >= (0 - buffer)){

                        //System.out.println("GAVE lABEL: ");
                        //image.get(x + 1, y + 1).setPriority( gradientAt(x + 1, y + 1) );
                        image.get(x + 1, y + 1).setPriority( -image.get(x + 1, y + 1).z );

                        if(giveLabel){
                            image.get(x + 1, y + 1).attach(image.get(x,y).id);
                            image.get(x + 1, y + 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x + 1, y + 1));
                            image.get(x + 1, y + 1).queue();
                        }

                        ids[7] = image.get(x + 1, y + 1).id;
                    //}
                }

            }


            long prev = ids[0];

            long current = ids[0];

            boolean replace = true;

        }

        public void updateNeighbourhood_tif(int x, int y, double bufferi, boolean giveLabel){

            double buffer = bufferi;

            long[] ids = new long[8];


            double zThreshold = 2.0;


            image.raster_id_b.ReadRaster(x - 1, y - 1, 3, 3, floatArray3x3);
            image.raster_flag_b.ReadRaster(x - 1, y - 1, 3, 3, floatArray3x3_2);
            image.raster_z_b.ReadRaster(x - 1, y - 1, 3, 3, floatArray3x3_3);

            int xIndex = 0;
            int yIndex = 0;


            if(labelOrNot_tif(floatArray3x3, x, y) != -9876 || floatArray3x3[4] != -999)

                if(x >= 0 && x < image.xDim && y >= 0 && y < image.yDim){

                    for(int i = 0; i < floatArray3x3.length; i++){
                        if(floatArray3x3_2[i] == 0 && floatArray3x3[i] == -999 && floatArray3x3_3[i] > zThreshold){

                            yIndex = i / 3;

                            xIndex = i - (yIndex * 3);

                            yIndex = y - 1 + yIndex;
                            xIndex = x - 1 + xIndex;

                            if(giveLabel){
                                image.attach(xIndex, yIndex, (int)floatArray3x3[4]);
                                image.dequeue(xIndex, yIndex);
                            }
                            else{
                                /* ALWAYS GO HERE!! */
                                jono2_tif.offer(new cellItem(xIndex, yIndex, -floatArray3x3_3[i]));
                                //jono2.offer(image.get(x - 1, y));
                                image.queue(xIndex, yIndex);
                                //image.get(x - 1, y).queue();

                            }

                        }
                    }
                    //System.out.println("----------------");
/*
                    if(!image.get(x - 1, y).inQue && image.get(x - 1, y).id == -999 && image.get(x - 1, y).z > zThreshold){

                        //if((image.get(x,y).z - image.get(x - 1, y).z) >= (0 - buffer)){/

                        //image.get(x - 1, y).setPriority( gradientAt(x - 1, y ));
                        image.get(x - 1, y).setPriority( -image.get(x - 1, y ).z);


                        if(giveLabel){
                            image.get(x - 1, y).attach(image.get(x,y).id);
                            image.get(x - 1, y).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x - 1, y));
                            image.get(x - 1, y).queue();

                        }

                        //System.out.println(image.get(x - 1, y).id);

                        ids[0] = image.get(x - 1, y).id;

                        // }
                    }

                    if(!image.get(x + 1, y).inQue && image.get(x + 1, y).id == -999 && image.get(x + 1, y).z > zThreshold){

                        if((image.get(x,y).z - image.get(x + 1, y).z) >= (0 - buffer)){

                            //image.get(x + 1, y).setPriority( gradientAt(x + 1, y) );
                            image.get(x + 1, y).setPriority( -image.get(x + 1, y).z );

                            if(giveLabel){
                                image.get(x + 1, y).attach(image.get(x,y).id);
                                image.get(x + 1, y).dequeue();
                            }
                            else{
                                jono2.offer(image.get(x + 1, y));
                                image.get(x + 1, y).queue();

                            }

                            ids[1] = image.get(x + 1, y).id;
                        }
                    }


                    if(!image.get(x, y - 1).inQue && image.get(x, y - 1).id == -999 && image.get(x, y - 1).z > zThreshold){

                        //if((image.get(x,y).z - image.get(x, y - 1).z) >= (0 - buffer)){

                        //image.get(x, y - 1).setPriority( gradientAt(x, y - 1) );
                        image.get(x, y - 1).setPriority( -image.get(x, y - 1).z );

                        if(giveLabel){
                            image.get(x, y - 1).attach(image.get(x,y).id);
                            image.get(x, y - 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x, y - 1));
                            image.get(x, y - 1).queue();

                        }


                        ids[2] = image.get(x, y - 1).id;
                        //}
                    }


                    if(!image.get(x, y + 1).inQue && image.get(x, y + 1).id == -999 && image.get(x, y + 1).z > zThreshold){

                        //if((image.get(x,y).z - image.get(x, y + 1).z) >= (0 - buffer)){

                        //image.get(x, y + 1).setPriority( gradientAt(x, y + 1) );
                        image.get(x, y + 1).setPriority( -image.get(x, y + 1).z );

                        if(giveLabel){
                            image.get(x, y + 1).attach(image.get(x,y).id);
                            image.get(x, y + 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x, y + 1));
                            image.get(x, y + 1).queue();
                        }

                        ids[3] = image.get(x, y + 1).id;
                        // }
                    }



                    if(!image.get(x - 1, y - 1).inQue && image.get(x - 1, y - 1).id == -999 && image.get(x - 1, y - 1).z > zThreshold){


                        //if((image.get(x,y).z - image.get(x - 1, y - 1).z) >= (0 - buffer)){

                        //image.get(x - 1, y - 1).setPriority( gradientAt(x - 1, y - 1) );
                        image.get(x - 1, y - 1).setPriority( -image.get(x - 1, y - 1).z );

                        if(giveLabel){
                            image.get(x - 1, y - 1).attach(image.get(x,y).id);
                            image.get(x - 1, y - 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x - 1, y - 1));
                            image.get(x - 1, y - 1).queue();
                        }

                        ids[4] = image.get(x - 1, y - 1).id;
                        //}
                    }


                    if(!image.get(x + 1, y - 1).inQue && image.get(x + 1, y - 1).id == -999 && image.get(x + 1, y - 1).z > zThreshold){

                        // if((image.get(x,y).z - image.get(x + 1, y - 1).z) >= (0 - buffer)){

                        //image.get(x + 1, y - 1).setPriority( gradientAt(x + 1, y - 1) );
                        image.get(x + 1, y - 1).setPriority( -image.get(x + 1, y - 1).z );

                        if(giveLabel){
                            image.get(x + 1, y - 1).attach(image.get(x,y).id);
                            image.get(x + 1, y - 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x + 1, y - 1));
                            image.get(x + 1, y - 1).queue();
                        }

                        ids[5] = image.get(x + 1, y - 1).id;
                        //}
                    }


                    if(!image.get(x - 1, y + 1).inQue && image.get(x - 1, y + 1).id == -999 && image.get(x - 1, y + 1).z > zThreshold){

                        //if((image.get(x,y).z - image.get(x - 1, y - 1).z) >= (0 - buffer)){

                        //image.get(x - 1, y + 1).setPriority( gradientAt(x - 1, y + 1) );
                        image.get(x - 1, y + 1).setPriority( -image.get(x - 1, y + 1).z );

                        if(giveLabel){
                            image.get(x - 1, y + 1).attach(image.get(x,y).id);
                            image.get(x - 1, y + 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x - 1, y + 1));
                            image.get(x - 1, y + 1).queue();
                        }

                        ids[6] = image.get(x - 1, y + 1).id;
                        //}
                    }


                    if(!image.get(x + 1, y + 1).inQue && image.get(x + 1, y + 1).id == -999 && image.get(x + 1, y + 1).z > zThreshold){

                        //if((image.get(x,y).z - image.get(x + 1, y + 1).z) >= (0 - buffer)){

                        //System.out.println("GAVE lABEL: ");
                        //image.get(x + 1, y + 1).setPriority( gradientAt(x + 1, y + 1) );
                        image.get(x + 1, y + 1).setPriority( -image.get(x + 1, y + 1).z );

                        if(giveLabel){
                            image.get(x + 1, y + 1).attach(image.get(x,y).id);
                            image.get(x + 1, y + 1).dequeue();
                        }
                        else{
                            jono2.offer(image.get(x + 1, y + 1));
                            image.get(x + 1, y + 1).queue();
                        }

                        ids[7] = image.get(x + 1, y + 1).id;
                        //}
                    }
*/

                }

            long prev = ids[0];

            long current = ids[0];

            boolean replace = true;

        }


        public double gradientAt(int x, int y){

            
            double Gx = (-image.get(x - 1, y + 1).z +  2 * -image.get(x, y + 1).z + -image.get(x + 1, y + 1).z) - (-image.get(x - 1, y - 1).z + 2 * -image.get(x, y - 1).z + -image.get(x + 1, y - 1).z);

            double Gy = (-image.get(x + 1, y - 1).z +  2 * -image.get(x + 1, y).z + -image.get(x + 1, y + 1).z) - (-image.get(x - 1, y - 1).z + 2 * -image.get(x - 1, y).z + -image.get(x - 1, y + 1).z);
        
            return Math.sqrt(Math.pow(Gx,2) + Math.pow(Gy,2));// - image.get(x,y).z;
            

        }


        public boolean isIsolated(int x, int y){

            waterRaster_band.ReadRaster(x - 1, y - 1, 3, 3, floatArray3x3);
            image.raster_z_b.ReadRaster(x - 1, y - 1, 3, 3, floatArray3x3_2);

            float v = floatArray3x3[4];// waterRaster[x][y];


            int minX = x - 1;
            int maxX = x + 1;
            int minY = y - 1;
            int maxY = y + 1;

            HashMap<Integer, double[]> pankki = new HashMap<Integer, double[]>();

            //int xDim = waterRaster.length; // i

            //int yDim = waterRaster[0].length; // j

            if(minX < 0)
                minX = 0;
            if(maxX >= this.xDim)
                maxX = this.xDim - 1;
            if(minY < 0)
                minY = 0;
            if(maxY >= this.yDim)
                maxY = this.yDim - 1;

            int yIndex;
            int xIndex;

            ///for(int i = minX; i <= maxX; i++ ){
               //for(int j = minY; j <= maxY; j++ ){

            for(int i = 0; i < floatArray3x3.length; i++){

                if(i != 4){

                    yIndex = i / 3;

                    xIndex = i - (yIndex * 3);

                    yIndex = y - 1 + yIndex;
                    xIndex = x - 1 + xIndex;

                    if(!pankki.containsKey((int)floatArray3x3[i]))
                        pankki.put((int)floatArray3x3[i], new double[]{floatArray3x3_2[i], 1});
                    else{
                        pankki.get((int)floatArray3x3[i])[0] += floatArray3x3_2[i];
                        pankki.get((int)floatArray3x3[i])[1] += 1.0;
                    }

                    replaceValue = floatArray3x3[i];

                    if(floatArray3x3[i] == v){
                        return false;
                    }
                }
            }
            /*
                   waterRaster_band.ReadRaster(i, j, 1, 1, floatArray);

                    if(i != x || j != y){

                        if(!pankki.containsKey((int)floatArray[0]))
                            pankki.put((int)floatArray[0], new double[]{image.get(i,j).z, 1});
                        else{
                            pankki.get((int)floatArray[0])[0] += image.get(i,j).z;
                            pankki.get((int)floatArray[0])[1] += 1.0;
                        }

                        replaceValue = floatArray[0];

                        if(floatArray[0] == v){
                            return false;
                        }
                    }
                    /*
                    if(i != x || j != y){

                        if(!pankki.containsKey(waterRaster[i][j]))
                            pankki.put((int)waterRaster[i][j], new double[]{image.get(i,j).z, 1});
                        else{
                            pankki.get((int)waterRaster[i][j])[0] += image.get(i,j).z;
                            pankki.get((int)waterRaster[i][j])[1] += 1.0;
                        }

                        replaceValue = waterRaster[i][j];

                        if(waterRaster[i][j] == v){
                            return false;
                        }
                    }
                     */

               //}
            //}

            
            float thisValue = floatArray3x3_2[4];

            float minDifference = Float.POSITIVE_INFINITY;

            int minIndexi = -1;

            for(int i : pankki.keySet()){

                if(i != -999 & i != -1)
                if(thisValue - altaat.get(i).zMiddle < minDifference && (i != -999)){

                    minIndexi = i;
                    minDifference = thisValue - (float)altaat.get(i).zMiddle;

                }

            }

            replaceValue = minIndexi;
            
            return true;

        }

        public boolean isHanging(int x, int y){

            waterRaster_band.ReadRaster(x - 1, y - 1, 3, 3, floatArray3x3);
            image.raster_z_b.ReadRaster(x - 1, y - 1, 3, 3, floatArray3x3_2);

            float v = floatArray3x3[4];// waterRaster[x][y];

            int count = 0;

            TreeMap<Float, Integer> map = new TreeMap<Float, Integer>();


            waterRaster_band.ReadRaster(x, y, 1, 1, floatArray);

            //float v = floatArray[0]; // waterRaster[x][y];

            int minX = x - 1;
            int maxX = x + 1;
            int minY = y - 1;
            int maxY = y + 1;

            //int xDim = waterRaster.length; // i

            //int yDim = waterRaster[0].length; // j

            if(minX < 0)
                minX = 0;
            if(maxX >= xDim)
                maxX = xDim - 1;
            if(minY < 0)
                minY = 0;
            if(maxY >= yDim)
                maxY = yDim - 1;

            //System.out.println(maxX - minX);


            HashMap<Integer, double[]> pankki = new HashMap<Integer, double[]>();

            for(int i = 0; i < floatArray3x3.length; i++){

                if(i != 4){
                    if(!pankki.containsKey((int)floatArray3x3[i]))
                        pankki.put((int)floatArray3x3[i], new double[]{floatArray3x3_2[i], 1});
                    else{
                        pankki.get((int)floatArray3x3[i])[0] += floatArray3x3_2[i];
                        pankki.get((int)floatArray3x3[i])[1] += 1.0;
                    }

                    replaceValue = floatArray3x3[i];

                    if(!map.containsKey(replaceValue))
                        map.put(replaceValue, 1);
                    else
                        map.put(replaceValue, map.get(replaceValue) + 1);

                    if(floatArray3x3[i] == v){

                        count++;

                    }


                }
            }
/*
            for(int i = minX; i <= maxX; i++ ){

               for(int j = minY; j <= maxY; j++ ){

                   waterRaster_band.ReadRaster(i, j, 1, 1, floatArray);

                    if(i != x || j != y){

                        if(!pankki.containsKey((int)floatArray[0]))
                            pankki.put((int)floatArray[0], new double[]{image.get(i,j).z, 1});
                        else{
                            pankki.get((int)floatArray[0])[0] += image.get(i,j).z;
                            pankki.get((int)floatArray[0])[1] += 1.0;
                        }



                        replaceValue = floatArray[0];

                        if(!map.containsKey(replaceValue))
                            map.put(replaceValue, 1);
                        else
                            map.put(replaceValue, map.get(replaceValue) + 1);

                        if(floatArray[0] == v){
                            
                            count++;

                        }
                    }
                    /*
                    if(i != x || j != y){

                        if(!pankki.containsKey(waterRaster[i][j]))
                            pankki.put((int)waterRaster[i][j], new double[]{image.get(i,j).z, 1});
                        else{
                            pankki.get((int)waterRaster[i][j])[0] += image.get(i,j).z;
                            pankki.get((int)waterRaster[i][j])[1] += 1.0;
                        }

                        replaceValue = waterRaster[i][j];

                        if(!map.containsKey(replaceValue))
                            map.put(replaceValue, 1);
                        else
                            map.put(replaceValue, map.get(replaceValue) + 1);

                        if(waterRaster[i][j] == v){

                            count++;

                        }
                    }


               }
            }
                 */
           

           if(count == 1) {

            
            float thisValue = floatArray3x3[4];

            float minDifference = Float.POSITIVE_INFINITY;

            int minIndexi = -1;

            for(int i : pankki.keySet()){

                if(i != -999 && i != v && i != -1)
                if(thisValue - altaat.get(i).zMiddle < minDifference && (i != -999)){

                    minIndexi = i;
                    minDifference = thisValue - (float)altaat.get(i).zMiddle;

                }

                /*
                if(thisValue - pankki.get(i)[0] / pankki.get(i)[1] < minDifference && (i != -999)){

                    minIndexi = i;
                    minDifference = thisValue - pankki.get(i)[0] / pankki.get(i)[1];

                }
                */
            }

            replaceValue = minIndexi;

            /*
               Float max = 0.0f;
               int maxV = 0;

               for(Float key : map.keySet()){

                    if(map.get(key) > maxV){

                        max = key;
                        maxV = map.get(key);

                    }
               } 

               replaceValue = max;
            */
               return true;

           }

           return false;

        }

        public void refine(){

            //float replaceValue = 0.0f;

            for(int i = 1; i < image.xDim - 1; i++){
                for(int j = 1; j < image.yDim - 1; j++){

                    //waterRaster.put(j, i, image.get(i, j).id);
                    if(isIsolated(i,j)){

                        floatArray[0] = replaceValue;
                        waterRaster_band.WriteRaster(i, j, 1, 1, floatArray);
                        //waterRaster[i][j] = replaceValue;
                    }
                    if(isHanging(i,j)){
                        //System.out.println(replaceValue);
                        floatArray[0] = replaceValue;
                        waterRaster_band.WriteRaster(i, j, 1, 1, floatArray);
                        //waterRaster[i][j] = replaceValue;
                    }

                }
            }

        }
			
		public void export() throws IOException{

            aR.p_update.threadFile[0] = "waterShed - export";
            aR.p_update.threadProgress[coreNumber-1] = -1;
            aR.p_update.threadEnd[coreNumber-1] = -1;
            aR.p_update.updateProgressITD();



            for(int i = 0; i < image.xDim; i++){
				for(int j = 0; j < image.yDim; j++){

					//waterRaster.put(j, i, image.get(i, j).id);

                    //waterRaster[i][j] = image.get(i, j).id;

                    image.raster_id_b.ReadRaster(i, j, 1, 1, floatArray);
                    //System.out.println(image.raster_id_b.getXSize() + " ; " + image.raster_id_b.getYSize() + " == " + i + " ; " + j);

                    //floatArray[0] = image.get(i, j).id;
                    waterRaster_band.WriteRaster(i, j, 1, 1, floatArray);

				}
			}

            refine();


			//Imgcodecs.imwrite("watershed.tif", waterRaster);
/*
            String tempName1 = "temp1_" + coreNumber + ".tif";
            String tempName2 = "temp2_" + coreNumber + ".tif";

			gdalE out = new gdalE();
			File tied = new File(tempName1);

			if(tied.exists())
				tied.delete();

			out.hei(tempName1, waterRaster);

            Dataset outputti ;
*/
			//String a = "gdal_translate -of GTiff -a_srs EPSG:3067 -a_ullr " + canopy.minX + " " + canopy.maxY + " " + (canopy.minX + canopy.resolution * canopy.numberOfPixelsX) +
			//" " + (canopy.maxY - canopy.resolution * canopy.numberOfPixelsY) + " water.tif water2.tif";
			//System.out.println(a);
        	//String b = "gdal_polygonize.py water2.tif -f \"ESRI Shapefile\" crowns.shp";

            String tempName = "tempWater" + coreNumber + ".tif";
            String tempName2 = "tempWater2" + coreNumber + ".tif";

            Vector<String> optionsVector = new Vector<>();
            optionsVector.add("-of");
            optionsVector.add("GTiff");
            optionsVector.add("-a_srs");
            optionsVector.add("EPSG:3067");
            optionsVector.add("-a_ullr");
            optionsVector.add(Double.toString(canopy.minX));
            optionsVector.add(Double.toString(canopy.maxY));
            optionsVector.add(Double.toString((canopy.minX + canopy.resolution * canopy.numberOfPixelsX)));
            optionsVector.add(Double.toString((canopy.maxY - canopy.resolution * canopy.numberOfPixelsY)));

            org.gdal.gdal.TranslateOptions optit = new org.gdal.gdal.TranslateOptions(optionsVector);
            //Dataset inputdata = gdal.Open(tempName1, gdalconstConstants.GA_ReadOnly);

            Dataset outti = gdaltranslate(tempName2, waterRaster_ds, optit); //gdal.Translate("waterShed.tif", inputdata, optit);

            outti.FlushCache();
            //outti.CommitTransaction();
            //File tFile1 = new File("water2.tif");

            /*
			final String command = a;
			Process p = null;
			    try {
			        p = Runtime.getRuntime().exec(command);
			    } catch (final IOException e) {
			        e.printStackTrace();
			    }

			    //Wait to get exit value
			    try {
			        p.waitFor();
			        final int exitValue = p.waitFor();
			        if (exitValue == 0)
			            System.out.println("Successfully executed the command: " + command);
			        else {
			            System.out.println("Failed to execute the following command: " + command + " due to the following error(s):");
			            try (final BufferedReader b = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
			                String line;
			                if ((line = b.readLine()) != null)
			                    System.out.println(line);
			            } catch (final IOException e) {
			                e.printStackTrace();
			            }                
			        }
			    } catch (InterruptedException e) {
			        e.printStackTrace();
			    }

            */
			//gdalExport2("water.tif", waterRaster);

            ogr.RegisterAll(); //Registering all the formats..
            gdal.AllRegister();

            /*
            try {
                Thread.sleep(5000);
            }catch(Exception e){
                e.printStackTrace();
            }
            */

            //tied.delete();


            String in_file = "tempWater2" + coreNumber + ".tif";

            String out_file = "waterShed.shp";

            out_file = fo.createNewFileWithNewExtension(canopy.pointCloud.getFile(), "_TreeSegmentation.shp").getAbsolutePath();

            if(!aR.odir.equals("asd"))
                out_file = fo.transferDirectories(new File(out_file), aR.odir).getAbsolutePath();

            String driverName = "ESRI Shapefile";
      
            String[]  split2 = out_file.split("/.");
            String out_name = split2[0];
                
            Dataset hDataset = gdal.Open(in_file, gdalconstConstants.GA_ReadOnly);
            Band rasterBand = hDataset.GetRasterBand(1);
            
            Driver shpDriver;
            shpDriver = ogr.GetDriverByName(driverName);

            DataSource outShp;
            outShp = shpDriver.CreateDataSource(out_file);

            Layer outShpLayer = outShp.CreateLayer("destination");

            FieldDefn layerFieldDef = new FieldDefn("DN",4);
            outShpLayer.CreateField(layerFieldDef);
            //FeatureDefn outShpFeatDefn = outShpLayer.GetLayerDefn();
            //Feature outShpFeat = new Feature(outShpFeatDefn);      

            gdal.Polygonize(rasterBand, null, outShpLayer, 0);

            String pointCloudName = canopy.pointCloud.getFile().getAbsolutePath();

            //String outPointCloudName = pointCloudName.substring(pointCloudName.lastIndexOf(".")-1) + "_ITD.las";

            File outFile = fo.createNewFileWithNewExtension(canopy.pointCloud.getFile(), "_ITD.las");

            if(!aR.odir.equals("asd"))
                outFile = fo.transferDirectories(outFile, aR.odir);

            //File tFile = new File("waterShed.shp");

            //System.out.println(isCompletelyWritten(tFile));

            //while(!isCompletelyWritten(tFile)){


            //}

           // new File(tempName2).delete();
           // new File(tempName2 + ".aux.xml").delete();


            outShp.delete();

            File deleteFile1 = new File(tempName);
            File deleteFile12 = new File(tempName+".aux.xml");
            File deleteFile2 = new File(tempName2);
            File deleteFile22 = new File(tempName2+".aux.xml");

            deleteFile1.delete();
            deleteFile12.delete();
            deleteFile2.delete();
            deleteFile22.delete();

            if(outFile.exists())
                outFile.delete();

            outFile.createNewFile();

            LASraf raOutput = new LASraf(outFile);

            LASReader reader = new LASReader(new File(pointCloudName));
            PointInclusionRule rule = new PointInclusionRule();

            LASwrite.writeHeader(raOutput, "lasITD", reader.versionMajor, reader.versionMinor,
                    reader.pointDataRecordFormat, reader.pointDataRecordLength,
                    reader.headerSize, reader.offsetToPointData, reader.numberVariableLengthRecords,
                    reader.fileSourceID, reader.globalEncoding,
                    reader.xScaleFactor, reader.yScaleFactor, reader.zScaleFactor,
                    reader.xOffset, reader.yOffset, reader.zOffset);


            long n = reader.getNumberOfPointRecords();

            LasPoint tempPoint = new LasPoint();

            long pointCount = 0;

            int x = 0;
            int y = 0;


            for(double[] key : treeTops) {

                image.treeTop((int)key[0], (int)key[1]);

            }

            boolean good = false;

            boolean poly = this.polyBank.size() > 0;

            double[] haku = new double[2];

            HashSet<Integer> rejected = new HashSet<>();

            if(poly) {
                for (int i = 0; i < altaat.size(); i++) {

                    haku[0] = canopy.minX + altaat.get(i).xMiddle * canopy.resolution + (canopy.resolution /2.0);
                    haku[1] = canopy.maxY - altaat.get(i).yMiddle * canopy.resolution - (canopy.resolution /2.0);

                    if (insidePolygons(haku) && waterbodyAreas.get(altaat.get(i).id) > 1.0f) {
                        rejected.add(altaat.get(i).id);
                    }


                }
            }

            //System.out.println("rejected size: " + rejected.size());

            for(long i = 0; i < n; i++){

                reader.readRecord(i, tempPoint);

                x = (int)Math.floor((tempPoint.x - canopy.minX) / canopy.resolution);   //X INDEX
                y = (int)Math.floor((canopy.maxY - tempPoint.y) / canopy.resolution);

                image.raster_id_b.ReadRaster(x, y, 1, 1, floatArray);

                //System.out.println("HERE!! " + rejected.size() + " " + polyBank.size());
/*
                if(poly){


                    haku[0] = canopy.minX + x * (double)canopy.resolution + ((double)canopy.resolution/2.0);
                    haku[1] = canopy.maxY - y * (double)canopy.resolution - ((double)canopy.resolution/2.0);

                    good = insidePolygons(haku);

                }else{
                    good = true;
                }
*/

                if(rejected.contains((int)floatArray[0]) || !poly) {

                    if (floatArray[0] >= 0) {

                        if (image.isTreeTop(x, y)) {
                            tempPoint.synthetic = true;
                        }

                        tempPoint.pointSourceId = (short) (floatArray[0] + 1);

                        if (raOutput.writePoint(tempPoint, rule, reader.xScaleFactor, reader.yScaleFactor, reader.zScaleFactor,
                                reader.xOffset, reader.yOffset, reader.zOffset, reader.pointDataRecordFormat, 0))
                            pointCount++;

                    } else {
                        tempPoint.pointSourceId = 0;

                        if (raOutput.writePoint(tempPoint, rule, reader.xScaleFactor, reader.yScaleFactor, reader.zScaleFactor,
                                reader.xOffset, reader.yOffset, reader.zOffset, reader.pointDataRecordFormat, 0))
                            pointCount++;
                    }
                }
            }

            raOutput.writeBuffer2();
            raOutput.updateHeader2();
            raOutput.close();

            image.deleteTempFiles();

            aR.p_update.fileProgress++;




            //System.out.println(outPointCloudName);


		}

        private boolean isCompletelyWritten(File file) {
            RandomAccessFile stream = null;
            try {
                stream = new RandomAccessFile(file, "rw");
                return true;
            } catch (Exception e) {
                //log.info("Skipping file " + file.getName() + " for this iteration due it's not completely written");
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        //log.error("Exception during closing file " + file.getName());
                    }
                }
            }
            return false;
        }


	}


	public static class chm{

        public Dataset cehoam;
        public Band band = null;

        public Dataset filtered = null;
        public Band band_filterd = null;

        boolean writePointCloud = false;

		Cantor homma = new Cantor();
		LASReader pointCloud = null;

		//Mat output = new Mat();

        public float[][] output2;
        float[][] output2R;
        float[][] output2G;
        float[][] output2B;


		double resolution = 1.0;

		//HashMap<Long, ArrayList<Double>> data = new HashMap<Long, ArrayList<Double>>();
        HashMap<Long, ArrayList<Double>> data2 = new HashMap<Long, ArrayList<Double>>();

        HashMap<Long, ArrayList<LasPoint>> data = new HashMap<Long, ArrayList<LasPoint>>();

        //LasPoint tempPoint = new LasPoint();

        HashMap<Long, ArrayList<Integer>> dataIndexes = new HashMap<Long, ArrayList<Integer>>();

        float[][] bank;

        float[][] bankR;
        float[][] bankG;
        float[][] bankB;

		double minX = 0.0;
		double maxX = 0.0;
		double minY = 0.0;
		double maxY = 0.0;

		HashSet<Long> treeTopBank = new HashSet<Long>();

		int numberOfPixelsX = 0;
		int numberOfPixelsY = 0;

        boolean interpolation = false;

		public HashSet<double[]> treeTops = new HashSet<double[]>();

        HashSet<Integer> indexes = new HashSet<Integer>();

        LASraf raTemp = null;

        int layers = 1;

        double resolution_m = 0.0;

        float[][] original = null;

        String outputFileName;

        argumentReader aR;

        int coreNumber = 0;

        float[] floatArray = new float[1];

        float[] treeKernel;

        boolean dz_on_the_fly = false;

        org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();

        org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();
        TriangularFacetInterpolator polator  = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);



        public chm(){

		}

		public chm(LASReader in, String outputFileName) throws IOException{

			pointCloud = in;
			establish();
			make();
			export(outputFileName);

		}

        public chm(double in){

            //System.out.println("WHAT THE HECK");

        }

		public chm(LASReader in, String method, int layers2, argumentReader aR, int coreNumber) throws IOException{

		    this.coreNumber = coreNumber;

		    treeKernel = new float[(int)(aR.dist * 2 + 1) * (int)(aR.dist * 2 + 1)];

		    this.aR = aR;

            this.layers = layers2;
			pointCloud = in;
			resolution = aR.step;

			double maxi_z = in.maxZ;
			double mini_z = in.minZ;

			if(aR.dz_on_the_fly){

			    dz_on_the_fly = true;

            }

			this.outputFileName = fo.createNewFileWithNewExtension(in.getFile(), ".tif").getAbsolutePath();

            if(!aR.odir.equals("asd"))
                this.outputFileName = fo.transferDirectories(new File(outputFileName), aR.odir).getAbsolutePath();

            //String asdi = in.getFile().getAbsolutePath().split(".las")[0] + "_ch1.las";

            //File outputPointCloud = new File(asdi);

            //if(outputPointCloud.exists())
               // outputPointCloud.delete();

            //raTemp = new LASraf(outputPointCloud);
            //LASwrite.writeHeader(raTemp, "las2dsm");

            if(!method.equals("null"))
                interpolation = true;

            aR.p_update.las2dsm_resolution = (int)this.resolution;
            aR.p_update.las2dsm_gaussianKernel = aR.kernel;
            aR.p_update.las2dsm_gaussianTheta = aR.theta;

            aR.p_update.updateProgressDSM();
            //interpolation = false;

			establish();


            aR.kernel = (int)( 2.0 * Math.ceil( 3.0 * aR.theta) + 1.0);


            //make();

            //polator = new tinfour.interpolation.TriangularFacetInterpolator(tin);

            //System.out.println("OUTPUT: " + outputFileName);

			export(outputFileName);
		}

        public void get(double x, double y){



        }

        public void deleteOutputImage(){

        }

		public void establish() throws IOException{

			minX = pointCloud.getMinX();
			maxX = pointCloud.getMaxX();
			minY = pointCloud.getMinY();
			maxY = pointCloud.getMaxY();

			numberOfPixelsX = (int)Math.ceil((maxX - minX) / resolution);
			numberOfPixelsY = (int)Math.ceil((maxY - minY) / resolution);

			float[][] groundLevel = new float[1][1];
			short[][] groundLevel_count = new short[1][1];

			if(aR.lasrelate) {
                groundLevel = new float[numberOfPixelsX][numberOfPixelsY];
                groundLevel_count = new short[numberOfPixelsX][numberOfPixelsY];
            }

			cehoam = gdalE.hei(this.outputFileName, numberOfPixelsY, numberOfPixelsX, Float.NaN);
            band = cehoam.GetRasterBand(1);

            this.resolution_m = (maxX - minX) / numberOfPixelsX;

			//output = new Mat(numberOfPixelsY, numberOfPixelsX, CvType.CV_64FC1);

            //output2 = new float[numberOfPixelsX][numberOfPixelsY];

            //bank = new float[numberOfPixelsX * numberOfPixelsY][3];

/*
            if(layers > 1){

                bankR = new float[numberOfPixelsX * numberOfPixelsY][3];
                bankG = new float[numberOfPixelsX * numberOfPixelsY][3];
                bankB = new float[numberOfPixelsX * numberOfPixelsY][3];

                output2R = new float[numberOfPixelsX][numberOfPixelsY];
                output2G = new float[numberOfPixelsX][numberOfPixelsY];
                output2B = new float[numberOfPixelsX][numberOfPixelsY];
            }
*/
			double tempx = minX;
			double tempy = maxY;

			long countx = 0;
			long county = 0;
			int count2 = 0;

            long n = pointCloud.getNumberOfPointRecords();
            LasPoint tempPoint = new LasPoint();
            if(dz_on_the_fly) {

                for (int i = 0; i < n; i += 10000) {

                    int maxi = (int) Math.min(10000, Math.abs(n - i));

                    try {
                        pointCloud.readRecord_noRAF(i, tempPoint, 10000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //pointCloud.braf.buffer.position(0);

                    for (int j = 0; j < maxi; j++) {
                        //Sstem.out.println(j);
                        //count++;
                        pointCloud.readFromBuffer(tempPoint);

                        /* Reading, so ask if this point is ok, or if
                        it should be modified.
                         */
                        if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                            continue;
                        }

                        if(tempPoint.classification == 2){

                            tin.add(new Vertex(tempPoint.x, tempPoint.y, tempPoint.z));

                        }
                    }

                }

            }


			//System.out.println("Data size: " + data.size());
			//System.out.println(count2);



            aR.p_update.threadEnd[coreNumber-1] = (int)n;
            aR.p_update.threadProgress[coreNumber-1]++;
            aR.p_update.threadFile[coreNumber-1] = "CHM - first pass";
            aR.p_update.updateProgressDSM();


            if(!aR.p_update.las2dsm_print)
                aR.p_update.updateProgressITD();

			long count = 0;
			long[] temppi = new long[2];

            int counti = 0;


            int counter = 0;


            for(int i = 0; i < n; i += 10000) {

                int maxi = (int) Math.min(10000, Math.abs(n - i));

                try {
                    pointCloud.readRecord_noRAF(i, tempPoint, 10000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                //pointCloud.braf.buffer.position(0);

                for (int j = 0; j < maxi; j++) {
                    //Sstem.out.println(j);
                    //count++;
                    pointCloud.readFromBuffer(tempPoint);

                    /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                    if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                        continue;
                    }

                    if(dz_on_the_fly){
                        double interpolatedZ = polator.interpolate(tempPoint.x, tempPoint.y, valuator);

                        tempPoint.z -= interpolatedZ;

                    }
/*
			for(long i = 0; i < n; i++){

				count++;

				
                if(i % 10000 == 0){

                    //System.out.print("\033[2K"); // Erase line content
                    //System.out.print(i + "\r");

                }
*/
				//pointCloud.readRecord(i, tempPoint);
				//System.out.println(tempPoint.z);
                    temppi[0] = Math.max((long)Math.floor((tempPoint.x - minX) / resolution), 0);   //X INDEX
                    temppi[1] = Math.max((long)Math.floor((maxY - tempPoint.y) / resolution), 0);

                    band.ReadRaster((int)temppi[0], (int)temppi[1], 1, 1, floatArray);

                    //imgRaf.read((int)temppi[0], (int)temppi[1]);

                    if(temppi[0] >= numberOfPixelsX)
                        temppi[0] = numberOfPixelsX - 1;
                    if(temppi[1] >= numberOfPixelsY)
                        temppi[1] = numberOfPixelsY - 1;


                    if(aR.lasrelate){
                        if(tempPoint.classification == 2){

                            groundLevel_count[(int)temppi[0]][(int)temppi[1]]++;

                            float newAverage = groundLevel[(int)temppi[0]][(int)temppi[1]] * ((float)groundLevel_count[(int)temppi[0]][(int)temppi[1]]-1.0f) / (float)groundLevel_count[(int)temppi[0]][(int)temppi[1]]
                                    + (float)tempPoint.z / (float)groundLevel_count[(int)temppi[0]][(int)temppi[1]];

                            groundLevel[(int)temppi[0]][(int)temppi[1]] = newAverage;

                        }
                    }


                    //bank[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][0] += (float)tempPoint.z;
                    //bank[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][1]++;

                    /* If this z is greater than the max observed in this cell */

                    //if(tempPoint.z > bank[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][2])
                        //bank[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][2] = (float)tempPoint.z;

                    if(tempPoint.z > floatArray[0] || Float.isNaN(floatArray[0])){
                        floatArray[0] = (float)tempPoint.z;
                        band.WriteRaster((int)temppi[0], (int)temppi[1], 1, 1, floatArray);

                        //imgRaf.writePixel((int)temppi[0], (int)temppi[1], floatArray[0]);
                        //System.out.println("Wrote: " + floatArray[0] + " " + temppi[0]);
                    }

                    //if(temppi[0] < 0 || temppi[1] < 0)
                      //  System.out.println("Wrote: " + floatArray[0] + " " + temppi[0]);

/*
                    if(layers > 1 && (tempPoint.numberOfReturns == 1 || tempPoint.numberOfReturns == 0) && tempPoint.z > 5.0){

                        counti++;

                        if((float)tempPoint.R > 0){
                            bankR[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][0] += (float)tempPoint.R;
                            bankR[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][1]++;

                            /** If this z is greater than the max observed in this cell
                            if(tempPoint.R > bankR[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][2])
                                bankR[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][2] = (float)tempPoint.R;
                        }
                        if((float)tempPoint.G > 0){
                            bankG[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][0] += (float)tempPoint.G;
                            bankG[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][1]++;

                            /** If this z is greater than the max observed in this cell
                            if(tempPoint.G > bankG[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][2])
                                bankG[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][2] = (float)tempPoint.G;
                        }
                        if((float)tempPoint.B > 0){
                            bankB[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][0] += (float)tempPoint.B;
                            bankB[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][1]++;

                            /** If this z is greater than the max observed in this cell
                            if(tempPoint.B > bankB[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][2])
                                bankB[(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]][2] = (float)tempPoint.B;
                        }
                    }
*/
                    aR.p_update.threadProgress[coreNumber-1]++;

                    if(aR.p_update.threadProgress[coreNumber-1] % 100000 == 0) {

                        aR.p_update.updateProgressDSM();

                        if(!aR.p_update.las2dsm_print)
                            aR.p_update.updateProgressITD();
                    }

                    count++;

                    //if(count % 100000 == 0){
                      //  System.out.println(count + " | " + n);
                    //}
                }
                //}
			}
            //System.out.println("counti: " + counti);

            if(aR.lasrelate){


                float rasterValue = 0;
                float groundValue = 0;



                for(int x = 0; x < numberOfPixelsX; x++){
                    for(int y = 0; y < numberOfPixelsY; y++){

                        if(groundLevel[x][y] != 0.0f){
                            tin.add(new Vertex(x, y, groundLevel[x][y]));
                        }

                    }
                }
                polator.resetForChangeToTin();

                if(false)

                for(int x = 0; x < numberOfPixelsX; x++){
                    for(int y = 0; y < numberOfPixelsY; y++){

                        band.ReadRaster(x, y, 1, 1, floatArray);

                        if(Float.isNaN(floatArray[0]) || floatArray[0] == 0)
                            continue;

                        double interpolatedZ = polator.interpolate(x + resolution / 2.0, y + resolution / 2.0, valuator);

                        //System.out.println("interpolatedZ "  + interpolatedZ);

                        if(Double.isNaN(interpolatedZ)){
                            floatArray[0] = Float.NaN;
                            band.WriteRaster(x, y, 1, 1, floatArray);
                        }else{

                            double delta_z = tempPoint.z - interpolatedZ;

                            floatArray[0] = (float)delta_z;
                            band.WriteRaster(x, y, 1, 1, floatArray);



                        }
                    }
                }


            }

		}

		public void make() throws IOException{

			//Cantor homma = new Cantor();

            int pointCount = 0;

            //PointInclusionRule rule = new PointInclusionRule();

            if(writePointCloud)
                 LASwrite.writeHeader(raTemp, "las2dsmMOD", pointCloud.versionMajor,
                         pointCloud.versionMinor, pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength,
                         pointCloud.headerSize, pointCloud.offsetToPointData, pointCloud.numberVariableLengthRecords,
                         pointCloud.fileSourceID, pointCloud.globalEncoding,
                         pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                         pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset);

             //System.out.println("gote here");

			for(int i = 0; i < output2[0].length; i++){
				for(int j = 0; j < output2.length; j++){


                    pointCount++;

                    if(pointCount % 10000 == 0){
                        //System.out.print("\033[2K"); // Erase line content
                        //System.out.print(pointCount + "\r");

                    }
                    /*
                    double sum = 0.0;
                    double sum2 = 0.0;

					long[] temp = new long[2];
					temp[0] = j;
					temp[1] = i;

					ArrayList<LasPoint> tempList = data.get(homma.pair(temp[0], temp[1]));
                    ArrayList<Double> tempList2 = data2.get(homma.pair(temp[0], temp[1]));

					double max = 0.0;
					double min = Double.POSITIVE_INFINITY;

                    double max2 = 0.0;
                    double min2 = Double.POSITIVE_INFINITY;

                    
					for(int h = 0; h < tempList.size(); h++){

                        pointCount++;
                        sum += tempList.get(h).z;

						if(tempList.get(h).z < min)
							min = tempList.get(h).z;
						if(tempList.get(h).z > max)
							max = tempList.get(h).z;

					}
                    
                    
                    /*
                    for(int h = 0; h < tempList2.size(); h++){

                        sum2 += tempList2.get(h);

                        if(tempList2.get(h) < min2)
                            min2 = tempList2.get(h);
                        if(tempList2.get(h) > max2)
                            max2 = tempList2.get(h);

                    }
                    */
					//System.out.println(max);'

                    /*
					if(tempList.size() > 0)
						output.put(i, j, max);
					else
						output.put(i, j, 9999);
                    
                    
                    double mean;
                    double mean2;

                    //System.out.println(max + "  " + max2);
                    
                    
                    if(tempList.size() != 0)
                        mean = sum / (double)tempList.size();
                    else
                        mean = 0.0;
                        */
                    /*
                    if(tempList2.size() != 0)
                        mean2 = sum2 / (double)tempList2.size();
                    else
                        mean2 = 0.0;
                        */
                    /*
                    if(mean == 0.0 || mean2 == 0.0)
                        output.put(i,j, 0.0);
                    else
                           
                    for(int h = 0; h < tempList.size(); h++){

                        LasPoint write = tempList.get(h);
                        write.intensity = (int)mean;
                        LASwrite.writePoint(raTemp, write, rule, 0.01, 0.01, 0.01, 0, 0, 0, 1, h);

                    }

                    */  
                    //System.out.println(tempList.size());
                    //output.put(i,j, (mean) );

                    //if(bank[j + (numberOfPixelsX) * i][0] != 0 && bank[j + (numberOfPixelsX) * i][0] != 0){
                        //output.put(i,j, bank[j + (numberOfPixelsX) * i][0] / bank[j + (numberOfPixelsX) * i][1]);
                    

                    //output2[j][i] = bank[j + (numberOfPixelsX) * i][0] / bank[j + (numberOfPixelsX) * i][1];

                    if(bank[j + (numberOfPixelsX) * i][1] > 0)
                        output2[j][i] = bank[j + (numberOfPixelsX) * i][2];
                    else
                        output2[j][i] = Float.NaN;
/*
                    if(layers > 1){

                        if(bankR[j + (numberOfPixelsX) * i][1] > 0)
                            output2R[j][i] = bankR[j + (numberOfPixelsX) * i][2];
                        else
                            output2R[j][i] = Float.NaN;

                        if(bankG[j + (numberOfPixelsX) * i][1] > 0)
                            output2G[j][i] = bankG[j + (numberOfPixelsX) * i][2];
                        else
                            output2G[j][i] = Float.NaN;

                        if(bankB[j + (numberOfPixelsX) * i][1] > 0)
                            output2B[j][i] = bankB[j + (numberOfPixelsX) * i][2];
                        else
                            output2B[j][i] = Float.NaN;

                    }
*/

                   // band.ReadRaster(i, j, 1, 1, floatArray);

                    //if(floatArray[0] != Float.NEGATIVE_INFINITY){

                    //}

                    //if(!Double.isNaN(output2[j][i]))
                      //  tin.add(new tinfour.common.Vertex(j, i, output2[j][i]));    
                    
                    //tin.add(new tinfour.common.Vertex(j, i, output2[j][i]));   
                   // }
                   // else{
                        //output.put(i,j, 9999);
                        //output2[j][i] = 9999;
                   // }
                    //(int)temppi[0] + (numberOfPixelsX) * (int)temppi[1]
                    //System.out.println("Mean: " + mean);
                    
                        //output.put(i,j,mean);
                    //output.put(i,j, mean);
                    //output.put(i,j, (max / (max + max2)));
					//System.out.println(output.get(i,j)[0]);
				}
			}

            //raTemp.writeBuffer2();
            //LASwrite.updateHeader2(raTemp, pointCount);

		}

        private ArrayList<float[]> drawCircle(int centerX, int centerY, int radius, float[][] image) {

            PriorityQueue<Float> jono = new PriorityQueue<Float>();

            ArrayList<float[]> mappi = new ArrayList<float[]>();

            int radius2 = radius;

            for(int i = 1; i <= radius2; i++){

                radius = i;

                int d = 1 - radius;
                int x = 0;
                int y = radius;
                //Color circleColor = Color.white;
                
                if(centerX - radius < 0)
                    return mappi;
                if(centerX + radius >= numberOfPixelsX)
                    return mappi;

                if(centerY - radius < 0)
                    return mappi;
                if(centerY + radius >= numberOfPixelsY)
                    return mappi;

                do {
                    
                        if(x != centerX || y != centerY){

                            //jono.offer(image[centerX + x][centerY + y]);
                            mappi.add(new float[]{image[centerX + x][centerY + y], centerX + x, centerY + y});

                            //jono.offer(image[centerX + x - 1][centerY + y - 1]);
                            mappi.add(new float[]{image[centerX + x - 1][centerY + y - 1], centerX + x - 1, centerY + y - 1});
                            
                            //jono.offer(image[centerX + x][centerY - y]);
                            mappi.add(new float[]{image[centerX + x][centerY - y], centerX + x, centerY - y});
                            //jono.offer(image[centerX + x - 1][centerY - y + 1]);
                            mappi.add(new float[]{image[centerX + x][centerY + y], centerX + x, centerY + y});

                            //jono.offer(image[centerX - x][centerY + y]);
                            mappi.add(new float[]{image[centerX - x][centerY + y], centerX - x, centerY + y});
                            //jono.offer(image[centerX - x + 1][centerY + y - 1]);
                            mappi.add(new float[]{image[centerX - x + 1][centerY + y - 1], centerX - x + 1, centerY + y - 1});

                            //jono.offer(image[centerX - x][centerY - y]);
                            mappi.add(new float[]{image[centerX - x][centerY - y], centerX - x, centerY - y});
                            //jono.offer(image[centerX - x + 1][centerY - y + 1]);
                            mappi.add(new float[]{image[centerX - x + 1][centerY - y + 1], centerX - x + 1, centerY - y + 1});

                            //jono.offer(image[centerX + y][centerY + x]);
                            mappi.add(new float[]{image[centerX + y][centerY + x], centerX + y, centerY + x});
                            //jono.offer(image[centerX + y - 1][centerY + x - 1]);
                            mappi.add(new float[]{image[centerX + y - 1][centerY + x - 1], centerX + y - 1, centerY + x - 1});

                            //jono.offer(image[centerX + y][centerY - x]);
                            mappi.add(new float[]{image[centerX + y][centerY - x], centerX + y, centerY - x});
                            //jono.offer(image[centerX + y - 1][centerY - x + 1]);
                            mappi.add(new float[]{image[centerX + y - 1][centerY - x + 1], centerX + y - 1, centerY - x + 1});

                            //jono.offer(image[centerX - y][centerY + x]);
                            mappi.add(new float[]{image[centerX - y][centerY + x], centerX - y, centerY + x});
                            //jono.offer(image[centerX - y + 1][centerY + x - 1]);
                            mappi.add(new float[]{image[centerX - y + 1][centerY + x - 1], centerX - y + 1, centerY + x - 1});

                            //jono.offer(image[centerX - y][centerY - x]);
                            mappi.add(new float[]{image[centerX - y][centerY - x], centerX - y, centerY - x});
                            //jono.offer(image[centerX - y + 1][centerY - x + 1]);
                            mappi.add(new float[]{image[centerX - y + 1][centerY - x + 1], centerX - y + 1, centerY - x + 1});

                        }

                    if (d < 0) {
                        d += 2 * x + 1;
                    } else {
                        d += 2 * (x - y) + 1;
                        y--;
                    }
                    x++;
                } while (x <= y);
            }

            return mappi;

        }



		public boolean isTreeTop(Mat input, int x, int y, int kernelSize){

			double zMiddle = input.get(y, x)[0];

			if(zMiddle < 5.0)
				return false;
			//System.out.println(zMiddle);
			int count = 0;
			ArrayList<Double> bank = new ArrayList<Double>();
			for(int i = (x - kernelSize); i <= (x + kernelSize); i++){
				for(int j = (y - kernelSize); j <= (y + kernelSize); j++){
					//String temp = i + " " + j;
					//System.out.println(temp);
					if(((i != x) || (j != y))){
						int[] temp = new int[2];
						temp[0] = i;
						temp[1] = j;

						if(treeTopBank.contains(Cantor.pair(i, j))){
							//System.out.println("GOT HERE");
							return false;
						}

						if(input.get(j, i)[0] > zMiddle)
							return false;

                        if(input.get(j,i)[0] < zMiddle * 0.75)
                            return false;

						bank.add(input.get(j, i)[0]);

					}

				}
			}

			return true;
		}	

        public double euclideanDistance(double x1, double y1, double x2, double y2){


            return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

        }

        public boolean isTreeTop(float[][] input, int x, int y, int kernelSize){

            double zMiddle = original[x][y];

            if(zMiddle < 5.0)
                return false;

            zMiddle = input[x][y];

            if(Double.isNaN(zMiddle))
                return false;

            ArrayList<float[]> circle = drawCircle(x, y, kernelSize, input);

            int count = 0;
            ArrayList<Double> bank = new ArrayList<Double>();

            double value = 0.0;

            //System.out.println(circle.size());

            int valuesBelow = 0;

            int valuesAbove = 0;

            ArrayList<int[]> valuesAboveLocations = new ArrayList<int[]>();


            double minDistance = 0.66 * (double)kernelSize;
            /*
            if(circle.size() > 0){

                for(float[] i : circle){

                    value = (double)i[0];
                    //System.out.println(value);

                    if(treeTopBank.contains(homma.pair((long)i[1], (long)i[2]))){
                        //System.out.println("GOT HERE");
                        return false;
                    }

                    if(value > zMiddle){

                        /*
                        if(euclideanDistance(x, y, (int)i[1], (int)i[2]) > minDistance){
                            valuesAbove++;
                            valuesAboveLocations.add(new int[]{(int)i[1], (int)i[2]});

                            if(valuesAbove > 0.3 * (double)circle.size())
                                return false;
                        }
                        else
                            return false;
                        

                        return false;
                    }
                        

                    if(value < 3.0){
                        
                        //valuesBelow++;

                        //if(valuesBelow > (double)circle.size() * 0.2)
                          //  return false;
                    }

                }
                */
                
                for(int i = (x - kernelSize); i <= (x + kernelSize); i++){
                    for(int j = (y - kernelSize); j <= (y + kernelSize); j++){
                        //String temp = i + " " + j;
                        //System.out.println(temp);
                        if(((i != x) || (j != y))){
                            int[] temp = new int[2];
                            temp[0] = i;
                            temp[1] = j;

                            if(treeTopBank.contains(Cantor.pair(i, j))){
                                //System.out.println("GOT HERE");
                                return false;
                            }

                            //System.out.println(x +" " + kernelSize);

                            if((double)input[i][j] > zMiddle)
                                return false;

                            //if((double)input[i][j] <= 2)
                              //  return false;

                            bank.add((double)input[i][j]);

                        }

                    }
                }
                
            //}
            //else{
              //  return false;
            //}

            if(valuesAboveLocations.size() > 0){

                for(int[] i : valuesAboveLocations){

                    
                    
                }

            }

            return true;
        }

        /**
         * The pixel is a tree top if it satisfies the condition:
         *  pixel_z < all_pixel_z_in_kernel.
         * @param input
         * @param x
         * @param y
         * @return
         */
        public boolean isTreeTop(Band input, int x, int y){

            band.ReadRaster(x, y, 1, 1, floatArray);

            float zMiddle = floatArray[0];

            if(zMiddle < 4.0f || Double.isNaN(zMiddle))
                return false;
            double kernel_size_meters = 1.1 + 0.002 * (zMiddle*zMiddle);
            int kernelSize = (int)Math.round(kernel_size_meters / this.resolution);

            if(kernelSize % 2 == 0){

                double one = (kernelSize + 1) * this.resolution - kernel_size_meters;
                double two = kernel_size_meters - (kernelSize - 1) * this.resolution;

                //System.out.println(one + " " + two);

                if(one <= two){

                    kernelSize -= 1;

                }else{

                    kernelSize += 1;
                }
            }

            kernelSize /= 2;

            if(kernelSize < 1)
                kernelSize = 1;
            //System.out.println("------------------------");


            //System.out.println(zMiddle + " < 2.0f? " +  (zMiddle < 2.0f));
            band_filterd.ReadRaster(x, y, 1, 1, floatArray);

            zMiddle = floatArray[0];

            if(zMiddle <= 0)
                return false;

            if(Float.isNaN(zMiddle))
                return false;

            //ArrayList<float[]> circle = drawCircle(x, y, kernelSize, input);

            int count = 0;
            ArrayList<Float> bank = new ArrayList<>();

            double value = 0.0;

            //System.out.println(circle.size());

            int valuesBelow = 0;

            int valuesAbove = 0;

            ArrayList<int[]> valuesAboveLocations = new ArrayList<int[]>();

            band_filterd.ReadRaster(x - kernelSize, y - kernelSize, kernelSize * 2 + 1, kernelSize * 2 + 1, treeKernel);


            int x_count = 0;
            int y_count = 0;

            int index = 0;

            for(int i = (x - kernelSize); i <= (x + kernelSize); i++) {
                for (int j = (y - kernelSize); j <= (y + kernelSize); j++) {

                    index = (kernelSize * 2 + 1) * y_count + (x_count);

                    if(((i != x) || (j != y)) && !Float.isNaN(treeKernel[index])){

                        int[] temp = new int[2];
                        temp[0] = i;
                        temp[1] = j;

                        if(treeTopBank.contains(Cantor.pair(i, j))){
                            //System.out.println("GOT HERE");
                            return false;
                        }

                        //System.out.println(index + " " + y_count + " " + x_count + " " + treeKernel.length);

                        if(treeKernel[index] > zMiddle)
                            return false;
                    }

                    y_count++;
                }
                y_count = 0;
                x_count++;
            }
/*
            for(int i = (x - kernelSize); i <= (x + kernelSize); i++){
                for(int j = (y - kernelSize); j <= (y + kernelSize); j++){
                    //String temp = i + " " + j;
                    //System.out.println(temp);
                    if(((i != x) || (j != y))){
                        int[] temp = new int[2];
                        temp[0] = i;
                        temp[1] = j;

                        if(treeTopBank.contains(homma.pair((long)i, (long)j))){
                            //System.out.println("GOT HERE");
                            return false;
                        }

                        //System.out.println(x +" " + kernelSize);

                        if((double)input[i][j] > (double)zMiddle)
                            return false;

                        //if((double)input[i][j] <= 2)
                        //  return false;

                        //bank.add((double)input[i][j]);

                    }

                }
            }

 */

            //}
            //else{
            //  return false;
            //}

            if(valuesAboveLocations.size() > 0){

                for(int[] i : valuesAboveLocations){



                }

            }
/*
            System.out.println(Arrays.toString(treeKernel));
            System.out.println(zMiddle);
            System.out.println("------------");

 */
            //System.out.println("FOUND ONE! " + zMiddle);

            return true;
        }

        public void writeTrees(){

			ogr.RegisterAll(); //Registering all the formats..
      		gdal.AllRegister();

            String out_file = fo.createNewFileWithNewExtension(this.pointCloud.getFile(), "_treeTops.shp").getAbsolutePath();// this.pointCloud.getFile().getAbsolutePath().substring(this.pointCloud.getFile().getAbsolutePath().lastIndexOf(".")-1) + "_treeTops.shp";

            if(!aR.odir.equals("asd"))
                out_file = fo.transferDirectories(new File(out_file), aR.odir).getAbsolutePath();

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


      		for(double[] temp : treeTops ){

				Geometry outShpGeom = new Geometry(1);
		        outShpGeom.SetPoint(0, minX + ( (temp[0] + 0.5) * resolution) , maxY - ( (temp[1] + 0.5) * resolution));
				outShpFeat.SetField("z", temp[2]);
				outShpFeat.SetGeometryDirectly(outShpGeom);
				outShpLayer.CreateFeature(outShpFeat);
    		}
		}

        public void deleteOutliers(){

            int height = output2[0].length;
            int width = output2.length;

            for(int i = 0; i < height; i++){

                for(int j = 0; j < width; j++){

                    if(Double.isNaN(output2[j][i]))
                        output2[j][i] = (float)polator.interpolate(j, i, valuator);
                }
            }

        }

		public void detectTreeTops(int kernelSize){

            //kernelSize = 1;

			int startX = kernelSize;
			int startY = kernelSize;
			int endX = numberOfPixelsX - 1 - kernelSize;
			int endY = numberOfPixelsY - 1 - kernelSize;


			aR.p_update.threadProgress[coreNumber-1] = 0;
            aR.p_update.threadEnd[coreNumber-1] = (endX-startX)*(endY-startY);

           // System.out.println("kernel: " + kernelSize);

            for(int i = startX; i <= endX; i++){
				for(int j = startY; j <= endY; j++){

					//if(isTreeTop(output, i, j, kernelSize)){



                    //if(isTreeTop(output2, i, j, kernelSize)){
                    if(isTreeTop(band_filterd, i, j)){

                        band_filterd.ReadRaster(i, j, 1, 1, floatArray);
						double[] temp = new double[3];
						temp[0] = i;
						temp[1] = j;
						//temp[2] = output.get(j, i)[0];
                        //temp[2] = output2[i][j];
						temp[2] = floatArray[0];
                        long[] temp2 = new long[2];
						temp2[0] = i;
						temp2[1] = j;
						//temp[2] = output.get(j, i)[0];
						String tempString = temp[0] + " " + temp[1];

						treeTopBank.add(Cantor.pair(i, j));
						treeTops.add(temp);

						aR.p_update.threadProgress[coreNumber-1]++;

						if(aR.p_update.threadProgress[coreNumber-1] % 1000 == 0)
						    aR.p_update.updateProgressITD();

						//output.put(j, i, -9999);

					}

				}
			}
			//System.out.println(treeTopBank.size());
			writeTrees();
			//gdalExport();
			//export("asd.tif");
		}

		public void gdalExport(String outName){
			
			gdalE out = new gdalE();

			//out.hei(outName, output);
            gdalE.hei(outName, output2);
		    
		}

        public void gdalExport(String outName, int layers, ArrayList<float[][]> in){
            
            gdalE out = new gdalE();

            //out.hei(outName, output);
            gdalE.hei(outName, in, layers);
            
        }

		public void gdalExport2(String outName, Mat in){
			
			gdalE out = new gdalE();

			gdalE.hei(outName, in);
		    
		}



		public void export(String name){

            aR.p_update.threadFile[coreNumber-1] = "CHM - exporting!";
            aR.p_update.threadProgress[coreNumber-1] = -1;
            aR.p_update.threadEnd[coreNumber-1] = -1;

            aR.p_update.updateProgressDSM();

            if(!aR.p_update.las2dsm_print)
                aR.p_update.updateProgressITD();

			/*
			Imgcodecs.imwrite("test.tif", output);
			try        
			{
    		Thread.sleep(1000);
			} 
			catch(InterruptedException ex) 
			{Thread.currentThread().interrupt();
			}
			//GTiff
			*/

            //ArrayList<float[][]> tempLista = new ArrayList<float[][]>();

            if(layers > 1){

                //tempLista.add(output2R);
                //tempLista.add(output2G);
                //tempLista.add(output2B);

            }

            interpolation = true;
			//localSmoothing(output, 1);

            if(aR.lasrelate)
                aR.interpolate = false;



            if(interpolation){

                //System.out.println("");
    			//localSmoothing(output, 2, true);


                //original = removeOutliers(output2, 0, aR.kernel, aR.theta);


                filtered = removeOutliers_tif(cehoam, 0, aR.kernel, aR.theta, this.coreNumber, outputFileName, aR.interpolate);

                band_filterd = filtered.GetRasterBand(1);
                //if(layers > 1)
                  //  tempLista = removeOutliersRGB(tempLista);

                //deleteOutliers();
                //localSmoothing(output2, 2, false);


                //removeOutliers(output);


                //Mat gaussian(Mat input, int kernelSize, double sigma){
                //output = gaussian(output, 3, 0.8);


            }else{
                filtered = cehoam;
                band_filterd = cehoam.GetRasterBand(1);
            }


            //if(aR.lasrelate){
            if(false){

                for(int x = 0; x < numberOfPixelsX; x++){
                    for(int y = 0; y < numberOfPixelsY; y++){

                        band_filterd.ReadRaster(x, y, 1, 1, floatArray);

                        if(Float.isNaN(floatArray[0]) || floatArray[0] == 0)
                            continue;

                        double interpolatedZ = polator.interpolate(x + resolution / 2.0, y + resolution / 2.0, valuator);

                        //System.out.println("interpolatedZ "  + interpolatedZ);

                        if(Double.isNaN(interpolatedZ)){
                            floatArray[0] = Float.NaN;
                            band_filterd.WriteRaster(x, y, 1, 1, floatArray);
                        }else{


                            double delta_z = floatArray[0] - interpolatedZ;

                            if(delta_z > 5.0){
                                floatArray[0] = (float)delta_z;
                                band_filterd.WriteRaster(x, y, 1, 1, floatArray);
                            }else{
                                floatArray[0] = Float.NaN;
                                band_filterd.WriteRaster(x, y, 1, 1, floatArray);
                            }
                        }
                    }
                }

            }

            band_filterd.SetNoDataValue(Double.NaN);
            //String tempName = "temp" + coreNumber + ".tif";

            String tempName = "tempFilter_" + coreNumber + ".tif";
            String tempName2 = "tempFilter2_" + coreNumber + ".tif";
            //if(layers > 1){

              //  gdalExport(tempName,layers, tempLista);

            //}
            //else
            //gdalExport(tempName);
/*
            File temp = new File(tempName);

			File tied = new File(name);

			if(tied.exists())
				tied.delete();
*/
            SpatialReference dst = new SpatialReference("");

            //String a = "";

            /*
            if(System.getProperty("os.name").equals("Linux"))
			     a = "/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/proj-4.8.0/gdal-2.2.3/apps/gdal_translate -of GTiff -a_srs EPSG:3067 -a_ullr " + this.minX + " " + this.maxY + " " + (this.minX + resolution * numberOfPixelsX) +
			" " + (this.maxY - resolution * numberOfPixelsY) + " temp.tif " + name;

            else
                a = "E:\\proj-4.8.0\\gdal-2.2.3_win\\apps\\gdal_translate --config GDAL_DATA \"E:/proj-4.8.0/gdal-2.2.3_win/data\" -of GTiff -a_srs EPSG:3067 -a_ullr " + this.minX + " " + this.maxY + " " + (this.minX + resolution * numberOfPixelsX) +
            " " + (this.maxY - resolution * numberOfPixelsY) + " temp.tif " + name;
			//System.out.println(a);
        	//String b = "gdalwarp -t_srs EPSG:3067 test2.tif final.tif";

            */

            Vector<String> optionsVector = new Vector<>();
            optionsVector.add("-of");
            optionsVector.add("GTiff");
            optionsVector.add("-a_srs");
            optionsVector.add("EPSG:3067");
            optionsVector.add("-a_ullr");
            optionsVector.add(Double.toString(this.minX));
            optionsVector.add(Double.toString(this.maxY));
            optionsVector.add(Double.toString((this.minX + resolution * numberOfPixelsX)));
            optionsVector.add(Double.toString((this.maxY - resolution * numberOfPixelsY)));

            org.gdal.gdal.TranslateOptions optit = new org.gdal.gdal.TranslateOptions(optionsVector);
            //Dataset inputdata = gdal.Open(tempName, gdalconstConstants.GA_ReadOnly);

            Dataset outti = gdaltranslate(outputFileName, filtered, optit); //gdal.Translate(name, inputdata, optit);

            outti.FlushCache();

            File deleteFile = new File(tempName);
            File deleteFile2 = new File("tempFilter_" + coreNumber + ".tif.aux.xml");

            //filtered.delete();
            deleteFile.delete();
            deleteFile2.delete();

            //temp.delete();

            if(aR.p_update.las2dsm_print) {
                aR.p_update.fileProgress++;
                aR.p_update.updateProgressDSM();
            }

            if(!aR.p_update.las2dsm_print)
                aR.p_update.updateProgressITD();

            /*

			final String command = a;

			Process p = null;
			    try {
			        p = Runtime.getRuntime().exec(command);
			    } catch (final IOException e) {
			        e.printStackTrace();
			    }

			    //Wait to get exit value
			    try {
			        p.waitFor();
			        final int exitValue = p.waitFor();
			        if (exitValue == 0){
			            //System.out.println("Successfully executed the command: " + command);
                    }
			        else {
			            System.out.println("Failed to execute the following command: " + command + " due to the following error(s):");
			            try (final BufferedReader b = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
			                String line;
			                if ((line = b.readLine()) != null)
			                    System.out.println(line);
			            } catch (final IOException e) {
			                e.printStackTrace();
			            }                
			        }
			    } catch (InterruptedException e) {
			        e.printStackTrace();
			    }

            temp.delete();

             */

            //outti.FlushCache();
		}

	}

	public static synchronized Dataset gdaltranslate(String name, Dataset inputdata, org.gdal.gdal.TranslateOptions optit){
        return gdal.Translate(name, inputdata, optit);
    }

	public static void main(String[] args) throws IOException {

		//System.loadLibrary("opencv_java320");
		
		File file1 = new File("dz.las");
		LASReader asd = new LASReader(file1);
		chm testi = new chm(asd, "asd.tif");

		testi.detectTreeTops(4);
		//System.out.println(testi.treeTops.size());

		//WaterShed fill = new WaterShed(testi.treeTops, 0.2, testi.output2, testi);

	}

}

