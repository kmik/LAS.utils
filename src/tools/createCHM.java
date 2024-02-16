package tools;


import LASio.*;
import err.toolException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import org.opencv.core.Mat;
import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

import org.tinfour.common.IIncrementalTinNavigator;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.standard.IncrementalTin;
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


    public static float[][] arrayCopy(float[][] in){

        float[][] output = new float[in.length][in[0].length];

        for(int i = 0; i < in.length; i++)
            for(int j = 0; j < in[0].length; j++)
                output[i][j] = in[i][j];

        return output;


    }

    public static double[][] arrayCopy(double[][] in){

        double[][] output = new double[in.length][in[0].length];

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
    public double[][] removeOutliers(double[][] input,  int kernel, double theta, int rows, int cols){

        return GaussianSmooth.smooth(input, rows, cols, kernel, theta);

    }


    public static double[][] removeOutliers(double[][] input, int blur, int kernel, double theta){

        Statistics stat = new Statistics();

        int x = 0;
        int y = 0;

        int n = 1;

        float[][] output = new float[input.length - n * 2][input[0].length - n * 2];

        double[][] temppi = arrayCopy(input);

        int height = input[0].length;
        int width = input.length;
        int counter = 0;
        int paatos = (height - n) * (width - n);

        int count3 = 0;

        float[] tempF;

        ArrayList<int[]> leftOvers = new ArrayList<int[]>();

        TreeSet<Double> zets = new TreeSet<Double>();

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

        double[][] original = null;

        n = blur;
        n = 1;

        int count = 0;

        double p80 = 0f;

        for(double i : zets){

            if(count++ >= zets.size() * 0.80){
                p80 = i;
                break;
            }

        }

        if(theta > 0)
        if(n > 0){


            double sigma = 0.8;

            int rows = input.length;
            int cols = input[0].length;

            double[][] temppi2 = new double[rows][cols];

            original = new double[rows][cols];
            
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


            smoothed = GaussianSmooth.smooth(temppi2, rows, cols, kernel, theta);  // THIS IS GOOD!!!! :)

            for (int i = 0; i < rows; i++){
                for(int j = 0; j < cols; j++){
                     input[i][j] = (float)smoothed[i][j];
                }
            }

        }

        n = 1;

        return input;

    }

    public static Dataset copyRaster(Dataset from, Dataset to, String toName){

        Vector<String> optionsVector = new Vector<>();
        optionsVector.add("-of");
        optionsVector.add("GTiff");

        org.gdal.gdal.TranslateOptions optit = new org.gdal.gdal.TranslateOptions(optionsVector);

        return gdaltranslate(toName, from, optit); //gdal.Translate(name, inputdata, optit);

    }

    public static void copyRasterContents(Band from, Band to){


        int x = from.getXSize();
        int y = from.getYSize();

        float[] read = new float[x*y];

        from.ReadRaster(0, 0, x, y, read);

        to.WriteRaster(0, 0, x, y, read);

    }

    public static void copyRasterContents(double[][] from, Band to){


        int x = to.getXSize();
        int y = to.getYSize();

        float[] read = new float[x*y];

        int counter = 0;

        for(int y_ = 0; y_ < from[0].length; y_++){
            for(int x_ = 0; x_ < from.length; x_++){


                read[counter++] = (float)from[x_][y_];

            }
        }

        to.WriteRaster(0, 0, x, y, read);

    }

    public static void copyRasterContents(Band from, float[][] to){

        int x = from.getXSize();
        int y =from.getYSize();

        float[] read = new float[x];

        for(int y_ = 0; y_ < y; y_++){

            from.ReadRaster(0,y_, x, 1, read);

            for(int x_ = 0; x_ < x; x_++){
                to[x_][y_] = read[x_];
            }
        }
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

        /** Replace with 2d arrays for
         *
         * better performance
         */

        int[][] raster_id_array;
        short[][] raster_flag_array;
        float[][] raster_priority_array;
        float[][] raster_z_array;

        float[] floatArray_1 = new float[9];
        float[] floatArray_2 = new float[9];
        float[] floatArray_3 = new float[9];

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


            raster_id_array = new int[xDim][yDim];
            raster_flag_array = new short[xDim][yDim];
            raster_priority_array = new float[xDim][yDim];
            raster_z_array = new float[xDim][yDim];


            copyRasterContents(raster_z_b, raster_z_array);


            for(int x = 0 ; x < xDim; x++)
                for(int y = 0 ; y < yDim; y++)
                    raster_id_array[x][y] = -999;

            float[] floatArray = new float[1];

            long id = 0L;

 		}

 		public void releaseMemory(){

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
 		    //raster_flag_b.WriteRaster(x, y, 1, 1, floatArray);

             raster_flag_array[x][y] = -99;
        }

        public boolean isTreeTop(int x, int y){

 		    //raster_flag_b.ReadRaster(x, y, 1, 1, floatArray);
             return raster_flag_array[x][y] == -99;
            //return floatArray[0] == -99;
        }

 		public void attach(int x, int y, int id ){

 		    //System.out.println("ATTACHED: " + id);
            floatArray[0] = id;
            //raster_id_b.WriteRaster(x, y, 1, 1, floatArray);
            raster_id_array[x][y] = id;
        }

        public void detach(int x, int y){

            floatArray[0] = -999f;
            //raster_id_b.WriteRaster(x, y, 1, 1, floatArray);
            raster_id_array[x][y] = -999;
        }

        public void queue(int x, int y){
            floatArray[0] = 1.0f;
            //raster_flag_b.WriteRaster(x, y, 1, 1, floatArray);
            raster_flag_array[x][y] = 1;
        }

        public void dequeue(int x, int y) {
            floatArray[0] = 0.0f;
            //raster_flag_b.WriteRaster(x, y, 1, 1, floatArray);
            raster_flag_array[x][y] = 0;
        }

        public void priority(int x, int y, float priority){
            floatArray[0] = priority;
            //raster_priority_b.WriteRaster(x, y, 1, 1, floatArray);
            raster_priority_array[x][y] = priority;
        }

        public void populateFloatArrays(int x_, int y_){

             int counter = 0;

             int x__ = x_;
             int y__ = y_;

                for(int y = y_ - 1 ; y <= y_ + 1; y++) {
                    for(int x = x_ - 1 ; x <= x_ + 1; x++){

                        x__ = Math.max(0, x);
                        x__ = Math.min(xDim - 1, x__);
                        y__ = Math.max(0, y);
                        y__ = Math.min(yDim - 1, y__);

                        //System.out.println(counter + " " + x__ + " " + y__ + " xDim: " + xDim + " yDim: " + yDim + " x_: " + x_ + " y_: " + y_);

                        if(counter >= 9){
                         //   throw new toolException("counter >= 9");
                        }
                        floatArray_1[counter] = raster_id_array[x__][y__];
                        floatArray_2[counter] = raster_flag_array[x__][y__];
                        floatArray_3[counter] = raster_z_array[x__][y__];
                        counter++;

                }
             }


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

			this.midX = (maxX - minX) / 2.0;
			this.midY = (maxY - minY) / 2.0;

			cellCount = cellCount + 8;

			total = jono.size();


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

        //double roundness = 500;
        double roundness = 5;

        double minx = Double.MAX_VALUE, maxx = Double.MIN_VALUE;
        double miny = Double.MAX_VALUE, maxy = Double.MIN_VALUE;

        double minx_no_buf = Double.MAX_VALUE, maxx_no_buf = Double.MIN_VALUE;
        double miny_no_buf = Double.MAX_VALUE, maxy_no_buf = Double.MIN_VALUE;


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
		Band waterRaster_band_mask = null;


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

        ArrayList<Integer> key_to_treetop = new ArrayList<>(0);
        HashMap<Integer, Integer> key_to_treetop_map = new HashMap<>(1000);
        ArrayList<Integer> key_to_treetop_2 = new ArrayList<>(0);

        int coreNumber = 0;

        float[] floatArray = new float[1];

        argumentReader aR;

        ArrayList<double[][]> polyBank = new ArrayList<>();

        HashMap<Integer, Float> waterbodyAreas = new HashMap<>();

		public WaterShed(){


		}

        public void releaseMemory(){

                this.altaat = null;
                this.jono = null;
                this.jono2 = null;
                this.jono2_tif = null;
                this.key_to_treetop = null;
                this.key_to_treetop_map = null;
                this.key_to_treetop_2 = null;
                this.polyBank = null;
                this.treeTops = null;
                this.treeTopLocations = null;
                this.waterbodyAreas = null;
                this.waterRaster = null;
                this.waterRaster_band = null;
                this.waterRaster_band_mask = null;
                this.waterRaster_ds = null;
                this.raster = null;
                this.raster_band = null;
                this.raster_ds = null;
                this.image = null;
                this.imageOrig = null;
                this.floatArray = null;
                this.floatArray3x3 = null;
                this.floatArray3x3_2 = null;
                this.floatArray3x3_3 = null;
                this.aR = null;
                this.canopy = null;
                this.pixelBank = null;
        }

		public WaterShed(HashSet<double[]> in, double speed2, Dataset inMat, chm inChm, argumentReader aR, int coreNumber) throws Exception{

            this.minx = inChm.pointCloud.getMinX();
            this.maxx = inChm.pointCloud.getMaxX();
            this.miny = inChm.pointCloud.getMinY();
            this.maxy = inChm.pointCloud.getMaxY();

            this.minx_no_buf = inChm.minX_no_buf;
            this.miny_no_buf = inChm.minY_no_buf;
            this.maxx_no_buf = inChm.maxX_no_buf;
            this.maxy_no_buf = inChm.maxY_no_buf;

		    this.polyBank = aR.polyBank;
		    this.treeTops = in;

		    this.coreNumber = coreNumber;
		    this.aR = aR;
			canopy = inChm;

            // convert roundness from pixels to meters
            roundness = roundness / canopy.resolution_m;

			this.raster_ds = inMat;
			this.raster_band = this.raster_ds.GetRasterBand(1);


			this.xDim = raster_band.getXSize();
			this.yDim = raster_band.getYSize();

            jono2_tif = new PriorityQueue<>(yDim*xDim, new CheckerCellItem());

            waterRaster_ds = copyRaster(raster_ds, waterRaster_ds, "tempWater" + coreNumber + ".tif");

            waterRaster_ds = gdalE.hei("tempWater" + coreNumber + ".tif", raster_ds.getRasterYSize(), raster_ds.getRasterXSize(), Float.NaN, 2);// driver.Create("filtered.tif", input.getRasterXSize(), input.getRasterYSize(), 1, gdalconst.GDT_Float32);

            waterRaster_band = waterRaster_ds.GetRasterBand(1);
            waterRaster_band_mask = waterRaster_ds.GetRasterBand(2);

            copyRasterContents(raster_band, waterRaster_band);

            waterRaster = new float[xDim][yDim];

			this.speed = speed2;

			image = new Raster(this.raster_ds, coreNumber, aR.step);

			totPixels = inMat.GetRasterXSize() * inMat.getRasterYSize();

            int aidee = 0;

            aR.p_update.threadFile[0] = "waterShed";
            aR.p_update.threadProgress[coreNumber-1] = -1;
            aR.p_update.threadEnd[coreNumber-1] = -1;
            aR.p_update.updateProgressITD();

			for(double[] key : in){

                //aidee = (int)key[3];

                //System.out.println("key[3] " + key[3] + " " + aidee);
                floatArray[0] = 0.0f;
                this.key_to_treetop.add((int)key[3]);
                //this.key_to_treetop_map.put(aidee, (int)key[3]);
                waterRaster[(int)key[0]][(int)key[1]] = 0.0f;
				altaat.add(new WaterBody(aidee, key[0], key[1], key[2], image ));
				waterbodyAreas.put(aidee, 0.0f);
                filledPixels++;
                image.attach( (int)key[0], (int)key[1], aidee);

                treeTopLocations.add(new int[]{(int)key[0], (int)key[1]});
                aidee++;
                image.priority((int)key[0], (int)key[1], (float)-key[2]);
                jono2_tif.offer(new cellItem((int)key[0], (int)key[1], (float)-key[2]));
                image.queue((int)key[0], (int)key[1]);

			}



            aR.p_update.updateProgressITD();
			start2();
            aR.p_update.updateProgressITD();


			export();

		}

		public WaterShed(double speed2){

			this.speed = speed;

		}


        public double euclideanDistance(int x1, int y1, int x2, int y2){

            return squareRoot( (x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) );

        }

        double squareRoot(double n)
        {

        /*We are using n itself as
        initial approximation This
        can definitely be improved */
            double x = n;
            double y = 1;

            // e decides the accuracy level
            double e = 0.001;
            while (x - y > e) {
                x = (x + y) / 2;
                y = n / x;
            }
            return x;
        }

        public void start2(){

            /* TODO: Order altaat in the order of water level! */

            while( jono2_tif.size() > 0){

                cellItem ci = jono2_tif.poll();
                /* We label the pixel with the SEED closest to it (euc distance) */
                this.updateNeighbourhood_array(ci.x, ci.y, false);
            }

        }

        public long labelOrNot_tif(float[] in, int x, int y){

            HashMap<Integer, Integer> t = new HashMap();

            int nono = 5;

            for(int i = 1; i < in.length; i++){

                if(i % 2 != 0)
                if((int)in[i] != -999) {

                    if (!t.containsKey((int) in[i])) {
                        t.put((int) in[i], 1);
                    } else {
                        t.put((int) in[i], t.get((int) in[i]) + 1);

                    }
                }
            }

            int maxInt = -1;
            int maxIndex = -1;
            int output = 0;

            double minDistance = Double.POSITIVE_INFINITY;
            double distance = 0.0;
            double minDistanceTreeHeight = 0.0;


            if((int)in[4] == -999){

                if(t.size() > 0){
                    for(int key : t.keySet()){

                        distance = euclideanDistance(treeTopLocations.get(key)[0], treeTopLocations.get(key)[1],
                                x, y);
/*
                        if(t.get(key) > maxInt){
                            maxInt = t.get(key);
                            maxIndex = key;
                        }
*/
                        if( distance < minDistance){

                            minDistance = distance;
                            output = key;
                            minDistanceTreeHeight = altaat.get(key).zMiddle;

                        }
                    }

                    //output = maxIndex

                    //image.raster_z_b.ReadRaster(x, y, 1, 1, floatArray);
                    floatArray[0] = image.raster_z_array[x][y];

                    double kernel_size_meters = 1.1 + 0.005 * (altaat.get(output).zMiddle*altaat.get(output).zMiddle);

                    double disti = kernel_size_meters / aR.step;

                    if((floatArray[0] >= altaat.get(output).zMiddle * 0.0 || floatArray[0] > 5.0) && distance < roundness){

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

                            jono2_tif.offer(new cellItem(xIndex, yIndex, -floatArray3x3_3[i]));
                            image.queue(xIndex, yIndex);

                        }

                    }
                }
        }


        public void updateNeighbourhood_array(int x, int y, boolean giveLabel){

            long[] ids = new long[8];
            double zThreshold = 5.0;
            image.populateFloatArrays(x, y);

            int xIndex = 0;
            int yIndex = 0;

            if(labelOrNot_tif(image.floatArray_1, x, y) != -9876 || image.floatArray_1[4] != -999) {

                if (x >= 0 && x < image.xDim && y >= 0 && y < image.yDim) {

                    for (int i = 1; i < image.floatArray_1.length; i++) {
                        if (i % 2 != 0)
                            if (image.floatArray_2[i] == 0 && image.floatArray_1[i] == -999 && image.floatArray_3[i] > zThreshold) {


                                yIndex = i / 3;

                                xIndex = i - (yIndex * 3);

                                yIndex = y - 1 + yIndex;
                                xIndex = x - 1 + xIndex;

                                jono2_tif.offer(new cellItem(xIndex, yIndex, -image.floatArray_3[i]));
                                image.queue(xIndex, yIndex);

                            }
                    }
                }
            }

        }

        public double gradientAt(int x, int y){

            
            double Gx = (-image.get(x - 1, y + 1).z +  2 * -image.get(x, y + 1).z + -image.get(x + 1, y + 1).z) - (-image.get(x - 1, y - 1).z + 2 * -image.get(x, y - 1).z + -image.get(x + 1, y - 1).z);

            double Gy = (-image.get(x + 1, y - 1).z +  2 * -image.get(x + 1, y).z + -image.get(x + 1, y + 1).z) - (-image.get(x - 1, y - 1).z + 2 * -image.get(x - 1, y).z + -image.get(x - 1, y + 1).z);
        
            return Math.sqrt(Math.pow(Gx,2) + Math.pow(Gy,2));// - image.get(x,y).z;
            

        }


        public boolean isIsolated(int x, int y){

            image.populateFloatArrays(x, y);

            floatArray3x3 = image.floatArray_1;
            floatArray3x3_2 = image.floatArray_3;

            float v = floatArray3x3[4];// waterRaster[x][y];


            int minX = x - 1;
            int maxX = x + 1;
            int minY = y - 1;
            int maxY = y + 1;

            HashMap<Integer, double[]> pankki = new HashMap<Integer, double[]>();

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

            image.populateFloatArrays(x, y);

            floatArray3x3 = image.floatArray_1;
            floatArray3x3_2 = image.floatArray_3;


            float v = floatArray3x3[4];// waterRaster[x][y];

            int count = 0;

            TreeMap<Float, Integer> map = new TreeMap<Float, Integer>();

            int minX = x - 1;
            int maxX = x + 1;
            int minY = y - 1;
            int maxY = y + 1;

            if(minX < 0)
                minX = 0;
            if(maxX >= xDim)
                maxX = xDim - 1;
            if(minY < 0)
                minY = 0;
            if(maxY >= yDim)
                maxY = yDim - 1;

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

            }

            replaceValue = minIndexi;

               return true;

           }

           return false;

        }

        public void refine(){

            int[][] temp = image.raster_id_array.clone();
            //float replaceValue = 0.0f;

            for(int i = 1; i < image.xDim - 1; i++){
                for(int j = 1; j < image.yDim - 1; j++){

                    if(isIsolated(i,j)){
                        floatArray[0] = replaceValue;
                        temp[i][j] = (int)floatArray[0];
                    }

                    if(temp[i][j] < 0)
                        continue;

                    if(isHanging(i,j)){

                        floatArray[0] = replaceValue;
                        temp[i][j] = (int)floatArray[0];

                    }

                }
            }

            image.raster_id_array = temp.clone();
            temp = null;


        }
			
		public void export() throws Exception{

            aR.p_update.threadFile[coreNumber-1] = "waterShed - export";
            aR.p_update.threadProgress[coreNumber-1] = -1;
            aR.p_update.threadEnd[coreNumber-1] = -1;
            aR.p_update.updateProgressITD();

            double[] gt = raster_ds.GetGeoTransform();

            refine();
            refine();
            //waterRaster_band.SetNoDataValue(-999);
            //waterRaster_band_mask.SetNoDataValue(-999);

            float[] floatArrayRow = new float[image.xDim];
            float[] floatArrayRow_mask = new float[image.xDim];
            double[] point_ = new double[3];
            HashSet<Integer> treesOutsideTile = new HashSet<>();
            double x_, y_, z_;

            if(aR.remove_buffer_2)
            if(minx_no_buf > minx || miny_no_buf > miny || maxx_no_buf < maxx || maxy_no_buf < maxy){

                for(int i = 0; i < altaat.size(); i++){


                    x_ = altaat.get(i).xMiddle;
                    y_ = altaat.get(i).yMiddle;
                    z_ = altaat.get(i).zMiddle;

                    point_[0] = gt[0] + x_ * gt[1] + y_ * gt[2];
                    point_[1] = gt[3] + x_ * gt[4] + y_ * gt[5];
                    point_[2] = z_;

                    //System.out.println(Arrays.toString(point_));
                    if(point_[0] < minx_no_buf || point_[0] > maxx_no_buf || point_[1] < miny_no_buf || point_[1] > maxy_no_buf) {
                        //System.out.println(Arrays.toString(point_));
                        treesOutsideTile.add(altaat.get(i).id);
                    }

                }

            }

            HashMap<Integer, Integer> this_id_to_unique_id = new HashMap<>();

            boolean[][] mask = new boolean[image.xDim][image.yDim];
            //boolean[][] mask = new boolean[image.xDim][image.yDim];

            //for(int i = 0; i < altaat.size(); i++){
            //    this_id_to_unique_id.put(key_to_treetop.get(altaat.get(i).id), aR.get_thread_safe_id());
            //}

				for(int y = 0; y < image.yDim; y++){
                    for(int x = 0; x < image.xDim; x++){

                    if(image.raster_id_array[x][y] < 0 ) {
                        floatArrayRow[x] = image.raster_id_array[x][y];
                        floatArrayRow_mask[x] = 0;
                        mask[x][y] = false;
                    }else {
                        //System.out.println(image.raster_id_array[x][y]);
                        //floatArrayRow[x] = this_id_to_unique_id.get( key_to_treetop.get(image.raster_id_array[x][y]));
                        //floatArrayRow[x] = this_id_to_unique_id.get( key_to_treetop.get(image.raster_id_array[x][y]));
                        floatArrayRow[x] = key_to_treetop.get(image.raster_id_array[x][y]);
                        //floatArrayRow[x] = this_id_to_unique_id.get( key_to_treetop_map.get(image.raster_id_array[x][y]));
                        floatArrayRow_mask[x] = 1;
                        mask[x][y] = true;
                    }

                    if(treesOutsideTile.size() > 0){
                        if(treesOutsideTile.contains(image.raster_id_array[x][y] )){
                            //System.out.println("HERE");

                            floatArrayRow_mask[x] = 0;
                            mask[x][y] = false;

                        }


                    }

				}
                    waterRaster_band.WriteRaster(0, y, image.xDim, 1, floatArrayRow);
                    waterRaster_band_mask.WriteRaster(0, y, image.xDim, 1, floatArrayRow_mask);
			}


            String tempName = "tempWater" + coreNumber + ".tif";
            String tempName2 = "tempWater2" + coreNumber + ".tif";

            String epsg_code = "EPSG:" + aR.EPSG;


            Vector<String> optionsVector = new Vector<>();
            optionsVector.add("-of");
            optionsVector.add("GTiff");
            optionsVector.add("-a_srs");
            optionsVector.add(epsg_code);
            optionsVector.add("-a_ullr");
            optionsVector.add(Double.toString(canopy.minX));
            optionsVector.add(Double.toString(canopy.maxY));
            optionsVector.add(Double.toString((canopy.minX + canopy.resolution * canopy.numberOfPixelsX)));
            optionsVector.add(Double.toString((canopy.maxY - canopy.resolution * canopy.numberOfPixelsY)));

            org.gdal.gdal.TranslateOptions optit = new org.gdal.gdal.TranslateOptions(optionsVector);
            //Dataset inputdata = gdal.Open(tempName1, gdalconstConstants.GA_ReadOnly);

            Dataset outti = gdaltranslate(tempName2, waterRaster_ds, optit); //gdal.Translate("waterShed.tif", inputdata, optit);

            outti.FlushCache();

            ogr.RegisterAll(); //Registering all the formats..
            gdal.AllRegister();

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
            Band rasterBand_mask = hDataset.GetRasterBand(2);

            Driver shpDriver;
            shpDriver = ogr.GetDriverByName(driverName);

            DataSource outShp;
            outShp = shpDriver.CreateDataSource(out_file);

            Layer outShpLayer = outShp.CreateLayer("destination");

            FieldDefn layerFieldDef = new FieldDefn("DN",4);
            outShpLayer.CreateField(layerFieldDef);

            gdal.Polygonize(rasterBand, rasterBand_mask, outShpLayer, 0);

            String pointCloudName = canopy.pointCloud.getFile().getAbsolutePath();

            File outFile = fo.createNewFileWithNewExtension(canopy.pointCloud.getFile(), "_ITD.las");

            if(!aR.odir.equals("asd"))
                outFile = fo.transferDirectories(outFile, aR.odir);

            outShp.delete();

            File deleteFile1 = new File(tempName);
            File deleteFile12 = new File(tempName+".aux.xml");
            File deleteFile2 = new File(tempName2);
            File deleteFile22 = new File(tempName2+".aux.xml");

            deleteFile1.delete();
            deleteFile12.delete();
            deleteFile2.delete();
            deleteFile22.delete();




            if(!aR.skeleton_output) {

                if (outFile.exists())
                    outFile.delete();

                outFile.createNewFile();

                LASReader p_cloud = new LASReader(new File(pointCloudName));

                int thread_n = aR.pfac.addReadThread(p_cloud);

                aR.add_extra_bytes(6, "ITC_id", "ID for an ITC segment");

                pointWriterMultiThread pw = new pointWriterMultiThread(outFile, p_cloud, "lasITC", aR);

                LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

                aR.pfac.addWriteThread(thread_n, pw, buf);

                LasPoint tempPoint = new LasPoint();



                long pointCount = 0;

                int x = 0;
                int y = 0;


                for (double[] key : treeTops) {

                    image.treeTop((int) key[0], (int) key[1]);

                }

                boolean good = false;

                boolean poly = this.polyBank.size() > 0;

                double[] haku = new double[2];

                HashSet<Integer> rejected = new HashSet<>();

                if (poly) {
                    for (int i = 0; i < altaat.size(); i++) {

                        haku[0] = canopy.minX + altaat.get(i).xMiddle * canopy.resolution + (canopy.resolution / 2.0);
                        haku[1] = canopy.maxY - altaat.get(i).yMiddle * canopy.resolution - (canopy.resolution / 2.0);

                        if (insidePolygons(haku) && waterbodyAreas.get(altaat.get(i).id) > 1.0f) {
                            rejected.add(altaat.get(i).id);
                        }


                    }
                }

                boolean remove_buffer = aR.remove_buffer_2;

                /* This tool is a special case for "remove_buffer"
                *
                *   If we want to remove buffer, we actually want to keep
                *   the buffer points of ITC segments that have a treetop
                *   not in the buffer zone. So let's override the aR.
                *   remove buffer argument.
                *
                *  */
                //if(remove_buffer)
                //    aR.remove_buffer = false;

                //System.out.println("rejected size: " + rejected.size());

                //for(long i = 0; i < p_cloud.getNumberOfPointRecords(); i++){

                for (long i = 0; i < p_cloud.getNumberOfPointRecords(); i += 20000) {

                    int maxi = (int) Math.min(20000, Math.abs(p_cloud.getNumberOfPointRecords() - i));

                    aR.pfac.prepareBuffer(thread_n, i, maxi);

                    for (int j = 0; j < maxi; j++) {

                        p_cloud.readFromBuffer(tempPoint);
                        //p_cloud.readRecord(i, tempPoint);

                        if (!aR.inclusionRule.ask(tempPoint, i+j, true)) {
                            continue;
                        }
/*
            for(long i = 0; i < n; i++){

                reader.readRecord(i, tempPoint);

 */
                        x = Math.min((int) ((tempPoint.x - canopy.minX) / canopy.resolution), canopy.numberOfPixelsX - 1);   //X INDEX
                        y = Math.min((int) ((canopy.maxY - tempPoint.y) / canopy.resolution), canopy.numberOfPixelsY - 1);

                        floatArray[0] = image.raster_id_array[x][y];

                        if (rejected.contains((int) floatArray[0]) || !poly) {

                            if (floatArray[0] >= 0) {

                                if (image.isTreeTop(x, y)) {
                                    tempPoint.classification = 15;
                                }

                                /** This WILL overflow at larger areas */
                                //tempPoint.pointSourceId = (short) (floatArray[0] + 1);

                                /** This will definitely not overflow at larger areas */
                                //tempPoint.gpsTime = (double) (floatArray[0] + 1);

                                //tempPoint.setExtraByteINT((int)(floatArray[0] + 1), aR.create_extra_byte_vlr_n_bytes.get(0), 0);
                                //tempPoint.setExtraByteINT( key_to_treetop.get((int)(floatArray[0])), aR.create_extra_byte_vlr_n_bytes.get(0), 0);
                                tempPoint.setExtraByteINT(key_to_treetop.get((int) (floatArray[0])), aR.create_extra_byte_vlr_n_bytes.get(0), 0);
                                //tempPoint.setExtraByteINT( key_to_treetop_map.get((int)(floatArray[0])), aR.create_extra_byte_vlr_n_bytes.get(0), 0);

                            } else {
                                tempPoint.setExtraByteINT(0, aR.create_extra_byte_vlr_n_bytes.get(0), 0);

                                //tempPoint.pointSourceId = 0;
                                //tempPoint.gpsTime = 0;

                            }
                        } else {
                            tempPoint.setExtraByteINT(0, aR.create_extra_byte_vlr_n_bytes.get(0), 0);

                        }

                        try {

                            /* Output only the points inside non-synthetic points that belong to a ITC segment */
                            if (aR.output_only_itc_segments && remove_buffer) {

                                if (mask[x][y] && !treesOutsideTile.contains((int) floatArray[0]))
                                    aR.pfac.writePoint(tempPoint, i+j, thread_n);

                            } else if (aR.output_only_itc_segments && !remove_buffer) {

                                if (mask[x][y])
                                    aR.pfac.writePoint(tempPoint, i+j, thread_n);

                            } else if (!aR.output_only_itc_segments && remove_buffer) {

                                if (!treesOutsideTile.contains((int) floatArray[0]))
                                    aR.pfac.writePoint(tempPoint, i+j, thread_n);

                            } else {
                                aR.pfac.writePoint(tempPoint, i+j, thread_n);
                            }

                            //if(!treesOutsideTile.contains((int)floatArray[0]))
                            //

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }

                aR.pfac.closeThread(thread_n);
                p_cloud = null;



            }

            image.releaseMemory();
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


	public class chm{

        public Dataset cehoam;

        public ArrayList<Dataset> cehoam_pit_free = new ArrayList<>();
        public ArrayList<Band> cehoam_pit_free_band = new ArrayList<>();

        public Band band = null;

        public Dataset filtered = null;
        public Band band_filterd = null;

        boolean writePointCloud = false;

		Cantor homma = new Cantor();
		LASReader pointCloud = null;

		//Mat output = new Mat();

        public float[][] output2;
        public double[][] chm_array;

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
		double minX_no_buf = Double.MAX_VALUE;
		double maxX = 0.0;
		double maxX_no_buf = Double.MIN_VALUE;
		double minY = 0.0;
		double minY_no_buf = Double.MAX_VALUE;
		double maxY = 0.0;
		double maxY_no_buf = Double.MIN_VALUE;



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

        public String outputFileName;

        argumentReader aR;

        int coreNumber = 0;

        float[] floatArray = new float[1];

        float[] treeKernel;

        boolean dz_on_the_fly = false;

        org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
        org.tinfour.standard.IncrementalTin pit_free_tin = new org.tinfour.standard.IncrementalTin();

        org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();
        TriangularFacetInterpolator polator  = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);
        TriangularFacetInterpolator polator_pit_free  = new org.tinfour.interpolation.TriangularFacetInterpolator(pit_free_tin);

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

            if(!method.equals("null"))
                interpolation = true;

            aR.p_update.las2dsm_resolution = (int)this.resolution;
            aR.p_update.las2dsm_gaussianKernel = aR.kernel;
            aR.p_update.las2dsm_gaussianTheta = aR.theta;

            aR.p_update.updateProgressDSM();
            //interpolation = false;

            establish();
			//establish_pit_free();


            //kernel_size = 2 * ceil(2 * sigma) + 1
            if(aR.theta != 0.0)
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


            double freezeDistance = 1.5;
            double triangleBuffer = 0.5;

			minX = Math.floor(pointCloud.getMinX());
			maxX = Math.ceil(pointCloud.getMaxX());
			minY = Math.floor(pointCloud.getMinY());
			maxY = Math.ceil(pointCloud.getMaxY());


			numberOfPixelsX = (int)Math.ceil((maxX - minX) / resolution);
			numberOfPixelsY = (int)Math.ceil((maxY - minY) / resolution);

			float[][] groundLevel = new float[1][1];
			short[][] groundLevel_count = new short[1][1];

			if(aR.lasrelate) {
                groundLevel = new float[numberOfPixelsX][numberOfPixelsY];
                groundLevel_count = new short[numberOfPixelsX][numberOfPixelsY];
            }


            org.gdal.gdal.Driver driver = null;
            driver = gdal.GetDriverByName("GTiff");
            driver.Register();

            try {
                cehoam = driver.Create(outputFileName, numberOfPixelsX, numberOfPixelsY, 1, gdalconst.GDT_Float32);

            }catch (Exception e){
                System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
                return;
            }
			//cehoam = gdalE.hei(this.outputFileName, numberOfPixelsY, numberOfPixelsX, Float.NaN);
            band = cehoam.GetRasterBand(1);

            band.SetNoDataValue(Float.NaN);

            double[] geoTransform = new double[]{minX, aR.step, 0.0, maxY, 0.0, -aR.step};

            SpatialReference sr = new SpatialReference();

            sr.ImportFromEPSG(3067);

            cehoam.SetProjection(sr.ExportToWkt());
            cehoam.SetGeoTransform(geoTransform);

            this.chm_array = new double[numberOfPixelsX][numberOfPixelsY];


            for(int x = 0; x < numberOfPixelsX; x++)
                for(int y = 0; y < numberOfPixelsY; y++)
                    chm_array[x][y] = Double.NaN;


            this.resolution_m = (maxX - minX) / numberOfPixelsX;

			double tempx = minX;
			double tempy = maxY;

            long n = pointCloud.getNumberOfPointRecords();
            LasPoint tempPoint = new LasPoint();

            TreeMap<Double, ArrayList<Integer>> order_small_large = new TreeMap<>();

            if(aR.pitFree) {
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
                        if (!aR.inclusionRule.ask(tempPoint, i + j, true)) {
                            continue;
                        }

                        if(!order_small_large.containsKey(-tempPoint.z)) {
                            order_small_large.put(-tempPoint.z, new ArrayList<Integer>());
                            order_small_large.get(-tempPoint.z).add(i+j);
                        }else{
                            order_small_large.get(-tempPoint.z).add(i+j);

                        }

                    }
                }

                //for(Double d : order_small_large.keySet()){

                //    System.out.println(d);

                //}
            }

            //System.out.println("SORTED!");

            if(dz_on_the_fly) {

                for (long i = 0; i < n; i += 10000) {

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

            long tStart = System.currentTimeMillis();

            MaxSizeHashMap<Integer, Float> map = new MaxSizeHashMap<>(100000);
            IIncrementalTinNavigator navi = pit_free_tin.getNavigator();
            SimpleTriangle triang = null;
            Vertex[] closest = new Vertex[3];
            IQuadEdge[] closest_edges = new IQuadEdge[3];



            for(long i = 0; i < n; i += 20000) {

                int maxi = (int) Math.min(20000, Math.abs(n - i));

                try {
                    pointCloud.readRecord_noRAF(i, tempPoint, 20000);
                }catch (Exception e){
                    e.printStackTrace();
                }

                for (int j = 0; j < maxi; j++) {

                    pointCloud.readFromBuffer(tempPoint);

                    /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                    if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                        continue;
                    }

                    if(!tempPoint.synthetic) {
                        if (tempPoint.x < this.minX_no_buf)
                            this.minX_no_buf = tempPoint.x;
                        if (tempPoint.x > this.maxX_no_buf)
                            this.maxX_no_buf = tempPoint.x;
                        if (tempPoint.y < this.minY_no_buf)
                            this.minY_no_buf = tempPoint.y;
                        if (tempPoint.y > this.maxY_no_buf)
                            this.maxY_no_buf = tempPoint.y;
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

                    aR.p_update.threadProgress[coreNumber-1]++;

                    if(aR.p_update.threadProgress[coreNumber-1] % 100000 == 0) {

                        aR.p_update.updateProgressDSM();

                        if(!aR.p_update.las2dsm_print)
                            aR.p_update.updateProgressITD();
                    }

                    count++;


                    temppi[0] = Math.min((int)((tempPoint.x - minX) / resolution), numberOfPixelsX-1);   //X INDEX
                    temppi[1] = Math.min((int)((maxY - tempPoint.y) / resolution), numberOfPixelsY-1);
/*
                    System.out.println(tempPoint + " \n" + maxX + " " + minY + " " + Arrays.toString(temppi) + " " + ((tempPoint.y - minY)/resolution));

                    System.out.println(minY + " " + maxY + " " + (maxY - minY));

                    System.out.println("------------------");

 */
                    if(Double.isNaN(chm_array[(int)temppi[0]][(int)temppi[1]] )){
                        //floatArray[0] = (float)tempPoint.z;
                        //band.WriteRaster((int)temppi[0], (int)temppi[1], 1, 1, floatArray);

                        chm_array[(int) temppi[0]][(int) temppi[1]] = tempPoint.z;
                    }


                    if(tempPoint.z > chm_array[(int)temppi[0]][(int)temppi[1]]) {
                        //floatArray[0] = (float)tempPoint.z;
                        //band.WriteRaster((int)temppi[0], (int)temppi[1], 1, 1, floatArray);

                        chm_array[(int) temppi[0]][(int) temppi[1]] = tempPoint.z;
                    }

                    //if(true)
                    //    continue;

                    int key = (int) (temppi[1] * numberOfPixelsX + temppi[0]);

                    //if(!map.containsKey(key)) {
                        //band.ReadRaster((int) temppi[0], (int) temppi[1], 1, 1, floatArray);
                    //    map.put(key, floatArray[0]);
                    //}
                    //else
                      //  floatArray[0] = map.get(key);

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

                    if(false)
                    if(tempPoint.z > floatArray[0] || Float.isNaN(floatArray[0])){
                        floatArray[0] = (float)tempPoint.z;

                       // if(map.containsKey(key))
                        //    map.put(key, floatArray[0]);

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


                }

			}

            int counter = 0;
            int counter2 = 0;

            pit_free_tin.preAllocateEdges(1000000);


            if(aR.pitFree){

                for(Double d : order_small_large.keySet()) {


                    ArrayList<Integer> indices = order_small_large.get(d);

                    //System.out.println("indices size: " + indices.size());

                    for (int i = 0; i < indices.size(); i++) {

                        int index = indices.get(i);

                        pointCloud.readRecord((long) index, tempPoint);

                        if (counter++ <= 5) {

                            pit_free_tin.add(new Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                            polator_pit_free.resetForChangeToTin();
                            navi.resetForChangeToTin();
                            continue;

                        }

                        triang = navi.getContainingTriangle(tempPoint.x, tempPoint.y);

                        //List<Vertex> neighs = pit_free_tin.getNeighborhoodPointsCollector().collectNeighboringVertices(tempPoint.x, tempPoint.y, 1, 1);

                        //double dist_first = euclideanDistance(tempPoint.x, tempPoint.y, neighs.get(0).x, neighs.get(0).y);

                        //if(dist_first > 0.5){
                        //    pit_free_tin.add(new Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                        //    continue;
                        //}


                        //

                        boolean test = navi.isPointInsideTin(tempPoint.x, tempPoint.y);

                        //System.out.println("boolean: " + test);
                        if (!test) {
                           // System.out.println("null");
                            //System.exit(1);
                            pit_free_tin.add(new Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                            polator_pit_free.resetForChangeToTin();
                            navi.resetForChangeToTin();
                            continue;
                        }

                        closest[0] = triang.getVertexA();
                        closest[1] = triang.getVertexB();
                        closest[2] = triang.getVertexC();

                        closest_edges[0] = triang.getEdgeA();
                        closest_edges[1] = triang.getEdgeB();
                        closest_edges[2] = triang.getEdgeC();

                        if(closest_edges[0].getLength() < freezeDistance && closest_edges[1].getLength() < freezeDistance && closest_edges[2].getLength() < freezeDistance){

                            double interpolatedZ = polator_pit_free.interpolate(tempPoint.x, tempPoint.y, valuator);

                            if(interpolatedZ > triangleBuffer){

                                continue;

                            }else{

                                pit_free_tin.add(new Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                                polator_pit_free.resetForChangeToTin();
                                navi.resetForChangeToTin();
                                continue;
                            }

                        }
                        //System.out.println(pit_free_tin.getVertices().size());

                        // System.out.println(Arrays.toString(Arrays.stream(closest).toArray()));

                        double minDistance = Double.MAX_VALUE;
/*
                        for (Vertex V : closest) {

                            if (V == null)
                                continue;


                            double distance = euclideanDistance(V.x, V.y, tempPoint.x, tempPoint.y);

                            if (distance < minDistance)
                                minDistance = distance;

                        }

                        if (minDistance < 0.75) {
                            //System.out.println("TOO CLOSE");
                            continue;
                        }
*/

                        //navi.resetForChangeToTin();
                       // double interpolatedZ = polator_pit_free.interpolate(tempPoint.x, tempPoint.y, valuator);
                        //System.out.println(Math.abs(interpolatedZ - tempPoint.z));
                        //if (Math.abs(interpolatedZ - tempPoint.z) > 0.5) {
                            //System.out.println("TOO FAR AWAY");
                        //    continue;
                        //}

                        pit_free_tin.add(new Vertex(tempPoint.x, tempPoint.y, tempPoint.z));
                        polator_pit_free.resetForChangeToTin();
                        navi.resetForChangeToTin();
                        //counter++;
                        //System.out.println("tempz " + tempPoint.z);
                        //polator_pit_free.resetForChangeToTin();
                        //navi.resetForChangeToTin();

                    }
                    counter2++;

                    //System.out.println(counter2 + " " + order_small_large.size());
                    //System.out.println(d);
                    //if(counter++ % 100 == 0)
                    //    System.out.println(d + " " + pit_free_tin.getVertices().size());
                }

            }
            //System.exit(1);



            if(aR.pitFree)
                for(int x = 0; x < numberOfPixelsX; x++){
                    for(int y = 0; y < numberOfPixelsY; y++) {

                        //temppi[0] = Math.min((int)((tempPoint.x - minX) / resolution), numberOfPixelsX-1);   //X INDEX
                        //                    temppi[1] = Math.min((int)((maxY - tempPoint.y) / resolution), numberOfPixelsY-1);
                        double coord_x = x * resolution + minX + resolution / 2;
                        double coord_y = maxY - y * resolution - resolution / 2;

                        double value = polator_pit_free.interpolate(coord_x, coord_y, valuator);

                        chm_array[x][y] = value;

                        //System.out.println(chm_array[x][y] + " ?? " + value);
                    }

                }

            //System.exit(1);

            //System.exit(1);
            long tEnd = System.currentTimeMillis();
            long tDelta = tEnd - tStart;

            long minutes = (tDelta / 1000)  / 60;
            int seconds = (int)((tDelta / 1000) % 60);

            //System.out.println("TOOK: " + minutes + " min " + seconds + " sec");


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

            int num_iter = 2;

            HashMap<Integer, Float> chauvenets = new HashMap<>();

            chauvenets.put(3, 1.383f);
            chauvenets.put(4, 1.534f);
            chauvenets.put(5, 1.645f);
            chauvenets.put(6, 1.732f);
            chauvenets.put(7, 1.803f);
            chauvenets.put(8, 1.863f);
            chauvenets.put(9, 1.915f);

            float maxDifference = 2.0f;
            if(aR.pitFree && false)
            for(int iter = 0; iter < num_iter; iter++){

                ArrayList<int[]> indexes_to_set_nan = new ArrayList<>();

                int height = band.getYSize();
                int width = band.getXSize();

                int n_ = 1;

                //ArrayList<Float> ts_f_surroundings = new ArrayList<>();

                //int x_, y_;

               // int count_x = 0, count_y = 0;

                //for (int y = n_; y < (height - n_); y++) {

                /** TODO: LOADS OF THINGS TO OPTIMIZE HERE!!!! THIS IS JUST STUPID IMPLEMENTATION. */
                IntStream.range(n_, (height - n_)).parallel().forEach(y -> {

                    float value = 0;

                    for (int x = n_; x < (width - n_); x++) {

                        int minX = x - n_;
                        int maxX = x + n_;
                        int minY = y - n_;
                        int maxY = y + n_;

                        ArrayList<Float> list = new ArrayList<Float>();

                        int count_x = 0;
                        int count_y = 0;

                        int[] kernel_indexes = new int[9];
                        float[] ke = new float[9];
                        ArrayList<Float> numarray = new ArrayList<>();
                        ArrayList<Float> numarray_surroundings = new ArrayList<>();

                        runningMedian median_all = new runningMedian();
                        runningMedian median_surroundings_ = new runningMedian();
                        runningMedian median_MAD = new runningMedian();


                        for (int h = minX; h <= maxX; h++) {
                            //count_x++;
                            for (int u = minY; u <= maxY; u++) {

                                count_y++;

                                int x_ = h;
                                int y_ = u;

                                if (x_ < 0)
                                    x_ = 0;
                                if (y_ < 0)
                                    y_ = 0;
                                if (x_ > (width - 1))
                                    x_ = width - 1;
                                if (y_ > (height - 1))
                                    y_ = height - 1;

                                value = (float)chm_array[x_][y_];
                                //band.ReadRaster(x_, y_, 1, 1, floatArray);

                                //System.out.println("count_y: " + count_y);
                                ke[count_y - 1] = value;

                                if (!Float.isNaN(value)) { // (x != j || y != i) &&

                                    kernel_indexes[numarray.size()] = count_y;

                                    if(count_y != 5){
                                        numarray_surroundings.add(value);
                                        //median_surroundings_.add(floatArray[0]);
                                    }

                                    //median_all.add(floatArray[0]);
                                    numarray.add(value);

                                }
                            }
                        }

                        if(Float.isNaN(ke[4]) || numarray_surroundings.size() < 3) {
                            continue;
                        }

                       // Arrays.sort(ke);

                        Collections.sort(numarray);

                        double median;
                       /*
                        if (ke.length % 2 == 0)
                            median = ((double)ke[ke.length/2] + (double)ke[ke.length/2 - 1])/2;
                        else
                            median = (double) ke[ke.length/2];
*/
                        if (numarray.size() % 2 == 0)
                            median = ((double)numarray.get(numarray.size()/2) + (double)numarray.get(numarray.size()/2 - 1))/2;
                        else
                            median = (double) numarray.get(numarray.size()/2);


                        Collections.sort(numarray_surroundings);

                        double median_surroundings;

                        if (numarray_surroundings.size() % 2 == 0)
                            median_surroundings = ((double)numarray_surroundings.get(numarray_surroundings.size()/2) + (double)numarray_surroundings.get(numarray_surroundings.size()/2 - 1))/2;
                        else
                            median_surroundings = (double) numarray_surroundings.get(numarray_surroundings.size()/2);



                        //System.out.println(median + " == " + median_all.median());
                        //System.out.println(median_surroundings + " == " + median_surroundings_.median());

                        //float[] ke2 = new float[ke.length];

                        //median = median_all.median();
                        //median_surroundings = median_surroundings_.median();

                        ArrayList<Float> numarray2 = new ArrayList<>();

                        for(int i = 0; i < numarray.size(); i++){

                            numarray2.add((float)Math.abs(numarray.get(i) - median));

                            //median_MAD.add((float)Math.abs(numarray.get(i) - median));
                        }


                        //Arrays.sort(ke2);
                        Collections.sort(numarray2);

                        double MAD;
                        /*
                        if (ke2.length % 2 == 0)
                            MAD = ((double)ke2[ke2.length/2] + (double)ke2[ke2.length/2 - 1])/2;
                        else
                            MAD = (double) ke2[ke2.length/2];
*/
                        if (numarray2.size() % 2 == 0)
                            MAD = ((double)numarray2.get(numarray2.size()/2) + (double)numarray2.get(numarray2.size()/2 - 1))/2;
                        else
                            MAD = (double) numarray2.get(numarray2.size()/2);


                        //MAD = median_MAD.median();

                        int n_outliers = 0;
                        boolean center_outlier = false;
                        int counter3 = 0;

                        float sum_f = 0.0f;
                        int counter_f = 0;


                        if(median == 0.0f || MAD == 0.0f)
                            continue;

                        for(float f : ke){

                            counter3++;

                            if(!Float.isNaN(f)){

                                if(counter3 != 5) {
                                    sum_f += f;
                                    counter_f++;
                                }


                                /* the following idea by Iglewicz and Hoaglin (1993): Use modified 𝑍-scores 𝑀 such that:*/
                                float stuff = (float)Math.abs(.6745f * (f - median) / MAD);

                                if( stuff > aR.filter_intensity){  // aR.filter_intensity defaults to 1.75

                                    n_outliers++;

                                    if(counter3 == 5){
                                        center_outlier = true;
                                    }

                                }
                            }

                        }

                        float mean_surroundings = sum_f / (float)counter_f;

                        /*

                        for(int i = 0; i < ke.length; i++){

                            if(i == 4)
                                continue;

                            if(Math.abs(ke[4] - ke[i]) < maxDifference)
                                ke[i] = Float.NaN;

                        }


                         */

                        /*
                        float sum = 0;
                        float sq_sum = 0;
                        int valid_cells = 0;

                        for(float f : ke){

                            if(!Float.isNaN(f)){

                                sum += f;
                                sq_sum += f * f;
                                valid_cells++;

                            }

                        }

                        float mean = sum / (float)valid_cells;
                        float variance = sq_sum / (float)valid_cells - mean * mean;
                        float sd = (float)Math.sqrt(variance);

                        int n_outliers = 0;
                        boolean center_outlier = false;
                        int counter3 = 0;

                        if(valid_cells < 3)
                            continue;

                        for(float f : ke){

                            counter3++;

                            if(!Float.isNaN(f)){

                                if((mean - f) > (chauvenets.get(valid_cells) * sd)){  // aR.filter_intensity defaults to 1.75

                                    n_outliers++;

                                    if(counter3 == 5){
                                        center_outlier = true;
                                    }

                                }
                            }

                        }
*/
                        if(n_outliers <= 4 && center_outlier && median > 2){

                            //indexes_to_set_nan.add(new int[]{x,y});
                            addToArrayList(indexes_to_set_nan, new int[]{x,y});

                        }else if(n_outliers > 4 && center_outlier){

                            /* This probably means that the surrounding stuff is VERY flat*/
                            if((median_surroundings - ke[4]) > 5.0)
                                //indexes_to_set_nan.add(new int[]{x,y});
                                addToArrayList(indexes_to_set_nan, new int[]{x,y});

                        }

                    /*

                    boolean[] grubb = grubbsTest(ts_f);
                    int index_offset = 0;

                    boolean center_outlier = false;

                    while(grubb[0] == true){

                        int index = kernel_indexes[index_offset];

                        if(index == 5)
                            center_outlier = true;

                        index_offset++;
                        ts_f.pollFirst();

                        grubb = grubbsTest(ts_f);

                    }

                    //System.out.println(index_offset + " " + center_outlier);

                    if(index_offset <= 2 && center_outlier){
                        floatArray[0] = Float.NaN;

                        indexes_to_set_nan.add(new int[]{x,y});
                        System.out.println(Arrays.toString(ke));

                    }

                     */
                    }
                });

                floatArray[0] = Float.NaN;

                for(int[] i : indexes_to_set_nan){
                    chm_array[i[0]][i[1]] = Double.NaN;
                    //band.WriteRaster(i[0], i[1], 1, 1, floatArray);
                }
                //scehoam.FlushCache();
               // band.FlushCache();

                //System.exit(1);
            }



		}

        public boolean[] grubbsTest(TreeSet<Float> values){

            if(values.size() == 0)
                return new boolean[]{false, false};

            boolean[] output = new boolean[2];

            float sum = 0;
            float sq_sum = 0;

            for(float f : values){

                sum += f;
                sq_sum += f * f;

            }

            float mean = sum / (float)values.size();
            float variance = sq_sum / (float)values.size() - mean * mean;
            float sd = (float)Math.sqrt(variance);

            float critical_value = 0;

            switch (values.size()){

                case 3:
                    critical_value = 1.15f;
                    break;

                case 4:
                    critical_value = 1.49f;
                    break;

                case 5:
                    critical_value = 1.75f;
                    break;

                case 6:
                    critical_value = 1.94f;
                    break;

                case 7:
                    critical_value = 2.1f;
                    break;

                case 8:
                    critical_value = 2.22f;
                    break;

                case 9:
                    critical_value = 2.32f;
                    break;

            }

            float t_min = (mean - values.first()) / sd;

            if(t_min > critical_value)
                output[0] = true;

            float t_max = (values.last() - mean) / sd;

            if(t_max > critical_value)
                output[1] = true;
            

            return output;

        }

        public synchronized void addToArrayList(ArrayList<int[]> indexes_to_set_nan, int[] add){

            indexes_to_set_nan.add(add);

        }

        public void establish_pit_free() throws IOException{

            double rasterization_threshold = 0.5;

            rasterization_threshold = 1.1;

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

            long n = pointCloud.getNumberOfPointRecords();
            LasPoint tempPoint = new LasPoint();


            if(dz_on_the_fly) {

                for (long i = 0; i < n; i += 10000) {

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

            polator.resetForChangeToTin();






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

            int[] thresholds = new int[]{2,5,10,15,20};
            ArrayList<float[]> floatArrays = new ArrayList<>();

            ArrayList<IncrementalTin> tins = new ArrayList<>();

            ArrayList<TriangularFacetInterpolator> polators  = new ArrayList<>();


            for(int i = 0; i < thresholds.length; i++){

                String of = "_pitFree_" + thresholds[i] + ".tif";
                String o_tmp = fo.createNewFileWithNewExtension(pointCloud.getFile(), of).getAbsolutePath();
                cehoam_pit_free.add(gdalE.hei(o_tmp, numberOfPixelsY, numberOfPixelsX, Float.NaN));
                cehoam_pit_free_band.add(cehoam_pit_free.get(cehoam_pit_free.size()-1).GetRasterBand(1));
                floatArrays.add(new float[]{0.0f});

                tins.add(new IncrementalTin());
                polators.add(new TriangularFacetInterpolator(tins.get(tins.size()-1)));

            }

            for(long i = 0; i < n; i += 10000) {

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
                    temppi[0] = Math.max((long)Math.floor((tempPoint.x - minX) / resolution - 1), 0);   //X INDEX
                    temppi[1] = Math.max((long)Math.floor((maxY - tempPoint.y) / resolution - 1), 0);

                    if(temppi[0] >= numberOfPixelsX)
                        temppi[0] = numberOfPixelsX - 1;
                    if(temppi[1] >= numberOfPixelsY)
                        temppi[1] = numberOfPixelsY - 1;

                    band.ReadRaster((int)temppi[0], (int)temppi[1], 1, 1, floatArray);

                    int whichRaster = -1;

                    for(int i_ = thresholds.length-1; i_ >= 0; i_--){

                        if(tempPoint.z > thresholds[i_]){

                            cehoam_pit_free_band.get(i_).ReadRaster((int)temppi[0], (int)temppi[1], 1, 1, floatArrays.get(i_));

                            if(tempPoint.z > floatArrays.get(i_)[0] || Float.isNaN(floatArrays.get(i_)[0])){

                                floatArrays.get(i_)[0] = (float)tempPoint.z;

                                cehoam_pit_free_band.get(i_).WriteRaster((int)temppi[0], (int)temppi[1], 1, 1, floatArrays.get(i_));

                            }
                        }
                    }

                    if(aR.lasrelate){
                        if(tempPoint.classification == 2){

                            groundLevel_count[(int)temppi[0]][(int)temppi[1]]++;

                            float newAverage = groundLevel[(int)temppi[0]][(int)temppi[1]] * ((float)groundLevel_count[(int)temppi[0]][(int)temppi[1]]-1.0f) / (float)groundLevel_count[(int)temppi[0]][(int)temppi[1]]
                                    + (float)tempPoint.z / (float)groundLevel_count[(int)temppi[0]][(int)temppi[1]];

                            groundLevel[(int)temppi[0]][(int)temppi[1]] = newAverage;

                        }
                    }

                    if(tempPoint.z > floatArray[0] || Float.isNaN(floatArray[0])){
                        floatArray[0] = (float)tempPoint.z;
                        band.WriteRaster((int)temppi[0], (int)temppi[1], 1, 1, floatArray);

                    }

                    aR.p_update.threadProgress[coreNumber-1]++;

                    if(aR.p_update.threadProgress[coreNumber-1] % 100000 == 0) {

                        aR.p_update.updateProgressDSM();

                        if(!aR.p_update.las2dsm_print)
                            aR.p_update.updateProgressITD();
                    }

                    count++;

                }
                //}
            }

            float[] data = new float[numberOfPixelsX];

            for(int y = 0; y < numberOfPixelsY; y++) {
                for(int i = 0; i < thresholds.length; i++){
                    cehoam_pit_free_band.get(i).ReadRaster(0, y, numberOfPixelsX, 1, data);
                    for(int x = 0; x < data.length; x++){
                        if(data[x] > 0){
                            tins.get(i).add(new Vertex(x,y,data[x]));
                        }
                    }
                }
            }

            for(int i = 0; i < thresholds.length; i++){
                polators.get(i).resetForChangeToTin();
            }

            RealVector p1 = new ArrayRealVector(3);
            RealVector p2 = new ArrayRealVector(3);
            RealVector p3 = new ArrayRealVector(3);
            RealVector p4 = new ArrayRealVector(3);

            for(int y = 0; y < numberOfPixelsY; y++) {

                band.ReadRaster(0, y, numberOfPixelsX, 1, data);
                boolean changed = false;

                for (int x = 0; x < numberOfPixelsX; x++) {

                    for(int i = 0; i < tins.size(); i++) {

                        if (tins.get(i).isBootstrapped()) {

                            IQuadEdge e = tins.get(i).getNeighborEdgeLocator().getNeigborEdge(x, y); // getNeighborhoodPointsCollector().collectNeighboringVertices(tempPoint.x, tempPoint.y, 0, 0);

                            Vertex a = e.getA();
                            Vertex b = e.getB();

                            //System.out.println("HA " + e.getLength() + " " + a.getDistance(b));

                            if(e.getLength() < rasterization_threshold){

                                double value = polators.get(i).interpolate(x, y, valuator);

                                //System.out.println(value + " " + data[x]);
                                if((float)value > data[x]){
                                    data[x] = (float)value;
                                    changed = true;
                                }
                            }
                        }
                    }
                }

                if(changed){
                    band.WriteRaster(0, y, numberOfPixelsX, 1, data);
                }

            }

            if(false)
            for(int i = 0; i < tins.size(); i++){

                if(tins.get(i).isBootstrapped()){

                    for(SimpleTriangle st : tins.get(i).triangles()){

                        if(st.getEdgeA().getLength() > rasterization_threshold &&
                                st.getEdgeB().getLength() > rasterization_threshold &&
                                st.getEdgeC().getLength() > rasterization_threshold){


                            System.out.println("BADD");

                        }

                    }

                }

            }




            //System.out.println("SUCCESS!!");
            //System.exit(1);
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

            //band.ReadRaster(x, y, 1, 1, floatArray);


            //float zMiddle = floatArray[0];
            float zMiddle = (float)chm_array[x][y];

            //System.out.println(zMiddle  + " " + floatArray[0]);

            if(zMiddle < 5.0f || Double.isNaN(zMiddle))
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


            //zMiddle = floatArray[0];

            if(zMiddle <= 0)
                return false;

            if(Float.isNaN(zMiddle))
                return false;


            ArrayList<Float> bank = new ArrayList<>();

            ArrayList<int[]> valuesAboveLocations = new ArrayList<int[]>();

            //band_filterd.ReadRaster(x - kernelSize, y - kernelSize, kernelSize * 2 + 1, kernelSize * 2 + 1, treeKernel);


            int minX = x - kernelSize;
            int maxX = x + kernelSize;
            int minY = y - kernelSize;
            int maxY = y + kernelSize;

            //Mat submat = input.submat(minY, maxY + 1, minX, maxX + 1);

            int sum = 0;

            //double[] array = new double[(n * 2 + 1) * (n * 2 + 1)];
            ArrayList<Float> list = new ArrayList<Float>();
            int count = 0;
            int count2 = 0;

            //tempF = new float[2];

            for (int h = minX; h <= maxX; h++) {
                for (int u = minY; u <= maxY; u++) {

                    int x_ = h;
                    int y_ = u;

                    if (x_ < 0)
                        x_ = 0;
                    if (y_ < 0)
                        y_ = 0;
                    if (x_ > (band.getXSize() - 1))
                        x_ = band.getXSize()  - 1;
                    if (y_ > (band.getYSize()  - 1))
                        y_ = band.getYSize()  - 1;

                    //band.ReadRaster(x_, y_, 1, 1, floatArray);
                    floatArray[0] = (float)chm_array[x_][y_];

                    if(count++ != (int)((kernelSize * 2 + 1) * (kernelSize * 2 + 1) / 2) && !Float.isNaN(floatArray[0])){

                        int[] temp = new int[2];

                        temp[0] = x_;
                        temp[1] = y_;

                        //if(treeTopBank.contains(Cantor.pair(i, j))){
                        if(treeTopBank.contains((long)(x_ * this.maxY + y_))){
                            return false;
                        }


                        if(floatArray[0] > zMiddle)
                            return false;

                    }

                }
            }
                    /*
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

                        //if(treeTopBank.contains(Cantor.pair(i, j))){
                        if(treeTopBank.contains((long)(i * maxY + j))){

                            return false;
                        }


                        if(treeKernel[index] > zMiddle)
                            return false;
                    }

                    y_count++;
                }
                y_count = 0;
                x_count++;
            }

                     */
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
    		FieldDefn layerFieldDef_2 = new FieldDefn("id",2);
    		outShpLayer.CreateField(layerFieldDef);
    		outShpLayer.CreateField(layerFieldDef_2);
    		FeatureDefn outShpFeatDefn=outShpLayer.GetLayerDefn();
			Feature outShpFeat = new Feature(outShpFeatDefn);      


      		for(double[] temp : treeTops ){

				Geometry outShpGeom = new Geometry(1);
		        outShpGeom.SetPoint(0, minX + ( (temp[0] + 0.5) * resolution) , maxY - ( (temp[1] + 0.5) * resolution));
				outShpFeat.SetField("z", temp[2]);
				outShpFeat.SetField("id", temp[3]);

				outShpFeat.SetGeometryDirectly(outShpGeom);
				outShpLayer.CreateFeature(outShpFeat);
    		}

              outShp.FlushCache();
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

        public void releaseMemory(){

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

            int id = 0;


            for(int i = startX; i <= endX; i++){
				for(int j = startY; j <= endY; j++){

					//if(isTreeTop(output, i, j, kernelSize)){



                    //if(isTreeTop(output2, i, j, kernelSize)){
                    if(isTreeTop(band, i, j)){

                        //band.ReadRaster(i, j, 1, 1, floatArray);

                        floatArray[0] = (float)chm_array[i][j];

						double[] temp = new double[4];
						temp[0] = i;
						temp[1] = j;
						//temp[2] = output.get(j, i)[0];
                        //temp[2] = output2[i][j];
						temp[2] = floatArray[0];

                        //temp[3] = id++;
                        temp[3] = aR.getGlobalId();
                        long[] temp2 = new long[2];
						temp2[0] = i;
						temp2[1] = j;
						//temp[2] = output.get(j, i)[0];
						String tempString = temp[0] + " " + temp[1];

						//treeTopBank.add(Cantor.pair(i, j));
						treeTopBank.add((long)(i * this.maxY + j));
						treeTops.add(temp);

						aR.p_update.threadProgress[coreNumber-1]++;

						if(aR.p_update.threadProgress[coreNumber-1] % 1000 == 0)
						    aR.p_update.updateProgressITD();

						//output.put(j, i, -9999);

					}

				}

                //System.out.println("ROW:  " + i + " " + endX);
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

            if(layers > 1){

                //tempLista.add(output2R);
                //tempLista.add(output2G);
                //tempLista.add(output2B);

            }

            interpolation = true;
			//localSmoothing(output, 1);

            if(aR.lasrelate)
                aR.interpolate = false;

            //filtered = gdalE.hei("tempFilter_" + this.coreNumber + ".tif", cehoam.getRasterYSize(), cehoam.getRasterXSize(), Float.NaN);// driver.Create("filtered.tif", input.getRasterXSize(), input.getRasterYSize(), 1, gdalconst.GDT_Float32);


            if(interpolation && aR.theta > 0){

                //filtered = removeOutliers_tif(cehoam, 0, aR.kernel, aR.theta, this.coreNumber, outputFileName, aR.interpolate);
                chm_array = removeOutliers(chm_array, 1, aR.kernel, aR.theta);
                //chm_array = removeOutliers(chm_array, aR.kernel, aR.theta, this.numberOfPixelsX, this.numberOfPixelsY);
                //band_filterd = filtered.GetRasterBand(1);
                copyRasterContents(chm_array, band);
                //band_filterd = filtered.GetRasterBand(1);

                //filtered.FlushCache();
                //band_filterd.FlushCache();
                //System.exit(1);
                //cehoam = removeOutliers_tif(cehoam, 0, aR.kernel, aR.theta, this.coreNumber, outputFileName, aR.interpolate);




            }else{
                chm_array = removeOutliers(chm_array, 1, aR.kernel, aR.theta);
                copyRasterContents(chm_array, band);
                filtered = cehoam;
                band_filterd = cehoam.GetRasterBand(1);
            }

            cehoam.FlushCache();
            band.FlushCache();

            if(true)
                return;

            /*
            double[] geoTransform = new double[]{pointCloud.minX, aR.step, 0.0, pointCloud.maxY, 0.0, -aR.step};

            SpatialReference sr = new SpatialReference();

            //cehoam = filtered;
            sr.ImportFromEPSG(aR.EPSG);

            //filtered.SetProjection(sr.ExportToWkt());
            cehoam.SetProjection(sr.ExportToWkt());
            //filtered.SetGeoTransform(geoTransform);
            cehoam.SetGeoTransform(geoTransform);

            //filtered.FlushCache();
            cehoam.FlushCache();
            band.FlushCache();

            if(aR.p_update.las2dsm_print) {
                aR.p_update.fileProgress++;
                aR.p_update.updateProgressDSM();
            }

            if(!aR.p_update.las2dsm_print)
                aR.p_update.updateProgressITD();

            if(true)
                return;
*/

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

            //band_filterd.SetNoDataValue(Double.NaN);

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

            String epsg_code = "EPSG:" + aR.EPSG;

            Vector<String> optionsVector = new Vector<>();
            optionsVector.add("-of");
            optionsVector.add("GTiff");
            optionsVector.add("-a_srs");
            optionsVector.add(epsg_code);
            optionsVector.add("-a_ullr");
            optionsVector.add(Double.toString(this.minX));
            optionsVector.add(Double.toString(this.maxY));
            optionsVector.add(Double.toString((this.minX + resolution * numberOfPixelsX)));
            optionsVector.add(Double.toString((this.maxY - resolution * numberOfPixelsY)));

            org.gdal.gdal.TranslateOptions optit = new org.gdal.gdal.TranslateOptions(optionsVector);
            //Dataset inputdata = gdal.Open(tempName, gdalconstConstants.GA_ReadOnly);
           // cehoam = gdalE.hei(this.outputFileName, numberOfPixelsY, numberOfPixelsX, Float.NaN);


            //Dataset outti = gdaltranslate(outputFileName, filtered, optit); //gdal.Translate(name, inputdata, optit);
            Dataset outti = gdaltranslate(outputFileName, filtered, optit); //gdal.Translate(name, inputdata, optit);

            outti.FlushCache();

            File deleteFile = new File(tempName);
            File deleteFile2 = new File("tempFilter_" + coreNumber + ".tif.aux.xml");

            //filtered.delete();
            deleteFile.delete();
            deleteFile2.delete();

            //temp.delete();



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


            //System.exit(1);
		}

	}

	public static synchronized Dataset gdaltranslate(String name, Dataset inputdata, org.gdal.gdal.TranslateOptions optit){
        return gdal.Translate(name, inputdata, optit);

    }

	public static void main(String[] args) throws IOException {

		//System.loadLibrary("opencv_java320");
		
		File file1 = new File("dz.las");
		LASReader asd = new LASReader(file1);
		//chm testi = new chm(asd, "asd.tif");

		//testi.detectTreeTops(4);
		//System.out.println(testi.treeTops.size());

		//WaterShed fill = new WaterShed(testi.treeTops, 0.2, testi.output2, testi);

	}

    public static class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {

        private final int maxSize;

        public MaxSizeHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

}

