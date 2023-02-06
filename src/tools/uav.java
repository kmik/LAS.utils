package tools;//package JavaMI;

import org.gdal.gdal.Driver;
import utils.*;

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.gdal.Dataset;
import org.gdal.gdalconst.*;
import org.gdal.gdal.Band;

import JavaMI.MutualInformation;

import LASio.*;


public class uav {

    public static ThreadProgressBar proge = new ThreadProgressBar("Aligning height models...");
    public static Output output = new Output();


    public static ArrayList<String> listFiles(String directory, String endsWithh){
        
        ArrayList<String> output = new ArrayList<String>(); 
        
        
              File[] files3 = new File(directory).listFiles();        //Haetaan tekstitiedostojen polut
                        int a3 = 0;
                        for(File file : files3){   //READ THE POINT CLOUD FILEPATHS
                            if(file.isFile()){
                            if(file.getName().endsWith((endsWithh))){

                            output.add(a3,file.getAbsolutePath());
                            a3++;
                            }
            }
        }
        
        
        
        return output;
    }



    static class multiThreadAlign implements Runnable{

        private final double[][] input;
        private final int min;
        private final int max;
        public final Chm chm1;
        public final Chm chm2;

        public multiThreadAlign (double[][] input2, int min2, int max2, Chm chm12, Chm chm22)
        {
            input = input2;
            min = min2;
            max = max2;
            chm1 = chm12;
            chm2 = chm22;
        }
         
        public void run() {
      
        //try {

            calcMinCost(input, min, max, chm1, chm2);

       // } catch (Exception e) {

           //System.out.println(e.getMessage());
        //System.out.println(minY + " " + maxY);
       //}
       //System.out.println("Thread done");
        }

    }


    public static float MI(double[] a, double[] b) {

        //double[] ad = intToDouble(a);
        //double[] bd = intToDouble(b);
        return (float)MutualInformation.calculateMutualInformation(a,b);

    }

    public static void calcMinCost(double[][] input, int min, int max, Chm chm1, Chm chm2){

        int count = 0;
        double similarity = 999999.999;


        for(int i = min; i < max; i++){

            similarity = -output.getInside().similarity(output.getOutside(), input[i][0], input[i][1]);

/*
            System.out.println(similarity + " " + input[i][0] + " " + input[i][1]);
            System.out.println();
            System.out.println();
*/

/*
            System.out.println("");
            System.out.println(similarity);
            System.out.println("");
*/
            output.setCost(similarity, input[i][0], input[i][1] );
            //while(!output.setCost(similarity, input[i][0], input[i][1] )){

                //similarity = -output.getInside().similarity(output.getOutside(), input[i][0], input[i][1]);

            //}

            if(i % 10 == 0)
                System.gc();

            //proge.updateCurrent(1);
            //proge.print();

        }

    }

    public static void progebar(int paatos, int proge, String nimi) {
        System.out.print("\033[2K"); // Erase line content
        if(proge < 0.05*paatos)System.out.print(nimi + "   |                    |\r");
        if(proge >= 0.05*paatos && proge < 0.10*paatos)System.out.print(nimi + "   |#                   |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.10*paatos && proge < 0.15*paatos)System.out.print(nimi + "   |##                  |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.15*paatos && proge < 0.20*paatos)System.out.print(nimi + "   |###                 |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.20*paatos && proge < 0.25*paatos)System.out.print(nimi + "   |####                |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.25*paatos && proge < 0.30*paatos)System.out.print(nimi + "   |#####               |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.30*paatos && proge < 0.35*paatos)System.out.print(nimi + "   |######              |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.35*paatos && proge < 0.40*paatos)System.out.print(nimi + "   |#######             |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.40*paatos && proge < 0.45*paatos)System.out.print(nimi + "   |########            |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.45*paatos && proge < 0.50*paatos)System.out.print(nimi + "   |#########           |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.50*paatos && proge < 0.55*paatos)System.out.print(nimi + "   |##########          |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.55*paatos && proge < 0.60*paatos)System.out.print(nimi + "   |###########         |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.60*paatos && proge < 0.65*paatos)System.out.print(nimi + "   |############        |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.65*paatos && proge < 0.70*paatos)System.out.print(nimi + "   |#############       |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.70*paatos && proge < 0.75*paatos)System.out.print(nimi + "   |##############      |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.75*paatos && proge < 0.80*paatos)System.out.print(nimi + "   |###############     |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.80*paatos && proge < 0.85*paatos)System.out.print(nimi + "   |################    |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.85*paatos && proge < 0.90*paatos)System.out.print(nimi + "   |#################   |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.90*paatos && proge < 0.95*paatos)System.out.print(nimi + "   |##################  |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.95*paatos && proge < 0.97*paatos)System.out.print(nimi + "   |################### |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.97*paatos && proge <= 1*paatos)System.out.print(nimi + "   |####################|  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        
        
    }

    public static class ThreadProgressBar{

        int current = 0;
        int end = 0;
        String name = "give me name!";
        int numberOfThreads = 0;

        public ThreadProgressBar(){

        }

        public ThreadProgressBar(String name2){

            name = name2;

        }

        public void setEnd(int newEnd){
            end = newEnd;
        }

        public synchronized void updateCurrent(int input){

            current += input;

        }

        public synchronized void reset(){

            current = 0;
            numberOfThreads = 0;
            end = 0;

        }

        public void setName(String nimi){

            name = nimi;

        }

        public void addThread(){

            numberOfThreads++;

        }

        public synchronized void print(){

            progebar(end, current, " " + name);
            //System.out.println(end + " " + current);
        }

    }

    public static class Output{

        double cost = Double.POSITIVE_INFINITY;
        double xOffset = Double.POSITIVE_INFINITY;
        double yOffset = Double.POSITIVE_INFINITY;

        Chm chm1 = null;
        Chm chm2 = null;

        public boolean special = false;

        public boolean special_condition = false;

        Chm inside = null;
        Chm outside = null;

        double offset = 0.0;

        String costFunction = "Correlation";

        public Output(){

        }

        public void setCostFunction(int in){

            if(in == 1)
                costFunction = "Correlation";

            if(in == 2)
                costFunction = "SSD";

            if(in == 3)
                costFunction = "MI";
        }

        public String getCostFunction(){

            return costFunction;

        }

        public boolean setCost(double in, double x, double y){

            //System.out.println(in);
            //System.out.println();
            if(in <= this.cost){

                if(in != this.cost){
                    setCost(in);
                    setX(-x);
                    setY(y);
                    return true;
                }
                else {

                    
                    System.out.println("!!!!!SAMEVALUE!!!!!!!");
                    System.out.println("in: " + in + " current: " + this.cost);
                    System.out.println("Diff x: " + (this.xOffset - x) + "\nDiff y: " + (this.yOffset - y));
                    System.out.println();
                    
                    if(Math.abs(x) <= Math.abs(this.getX()) && Math.abs(y) <= Math.abs(this.getY())){

                        setCost(in);
                        setX(-x);
                        setY(y);

                    }
                    return false;
                }          
            }
            return true;
        }

        public synchronized void setX(double in){

            xOffset = in;

        }

        public synchronized void setY(double in){

            yOffset = in;

        }

        public synchronized void setCost(double in){

            cost = in;

        }

        public void setOffset(double in){

            offset = in;

        }

        public void setChms(Chm chm12, Chm chm22){

            chm1 = chm12;
            chm2 = chm22;

        }

        public double getCost(){

            return cost;

        }

        public double getX(){

            return xOffset;

        }

        public double getY(){

            return yOffset;

        }

        public void setup(){

            if(chm1.isInside(chm2)){
                setInside(chm1);
                setOutside(chm2);
            }

            else if(chm2.isInside(chm1)){
                setInside(chm2);
                setOutside(chm1);
            }

            else {
                System.out.println("Neither raster is inside the other, shifting the smaller one");

                special = true;

                double chm1_y = chm1.getY()[0] - chm1.getY()[1];
                double chm1_x = chm1.getX()[1] - chm1.getX()[0];

                double chm2_y = chm2.getY()[0] - chm2.getY()[1];
                double chm2_x = chm2.getX()[1] - chm2.getX()[0];

                System.out.println( (chm1_x * chm1_y) + " " + (chm2_x * chm2_y) ) ;
/*
                if( (chm1_x * chm1_y) < (chm2_x * chm2_y)){
                    setInside(chm1);
                    setOutside(chm2);
                    special_condition = true;
                }else{
                    setInside(chm2);
                    setOutside(chm1);



                }
*/
                setInside(chm1);
                setOutside(chm2);
                special_condition = true;

            }

            System.out.println("spec.cond: " + special_condition);

        }

        public void setInside(Chm in){

            inside = in;

        }


        public void setOutside(Chm in){

            outside = in;
            
        }

        public Chm getInside(){

            return this.inside;

        }

        public Chm getOutside(){

            return this.outside;

        }

        public String raport(){
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            //System.out.println(); //2016/11/16 12:08:43
            return "MKAlign report " + dateFormat.format(date)
                    + "\n" + "Inside: " + output.getInside().getName() + "\nOutside: " + output.getOutside().getName()
                    + "\nCost function: " + output.getCostFunction()
                    + "\n* Minimum cost: " + this.getCost() + "\n* X correction: " + this.getX()
                    + "\n* Y correction: " + this.getY(); 

        }

        public void raportToFile(){

            

            
        }

    }

    public static double Correlation(double[] xs, double[] ys) {
        //TODO: check here that arrays are not null, of the same length etc

        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double sxy = 0.0;

        int n = xs.length;

        int sum1 = 0;
        int sum2 = 0;

        for(int i = 0; i < xs.length; i++){

            //if()
            if(!Double.isNaN(xs[i]) && !Double.isNaN(ys[i])){
                sum1 += xs[i];
                sum2 += ys[i];
            }
            //System.out.println(xs[i]);
        }
        /*
        System.out.println("");
        System.out.println(sum2 );
        System.out.println("");
        */
        float mean1 = (float)sum1 / (float)xs.length;
        float mean2 = (float)sum2 / (float)ys.length;

        for(int i = 0; i < n; ++i) {

            double x = xs[i] - mean1;
            double y = ys[i] - mean2;

            sx += x;
            sy += y;
            sxx += x * x;
            syy += y * y;
            sxy += x * y;
        }

        // covariation
        double cov = sxy / n - sx * sy / n / n;
        // standard error of x
        double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
        // standard error of y
        double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);

        // correlation is just a normalized covariation

        //System.out.println("cov: " + cov + "\nSigmax: " + sum1 + "\nSigmay: " + xs.length);

        return cov / sigmax / sigmay;
    }

    public static double Correlation2(double[] xs, double[] ys) {
        //TODO: check here that arrays are not null, of the same length etc


        double[] xs2 = xs.clone();
        double[] ys2 = ys.clone();

        Arrays.sort(xs2);
        Arrays.sort(ys2);

        //System.out.println(Arrays.toString(xs));

        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double sxy = 0.0;

        

        double sum1 = 0;
        double sum2 = 0;

        int n = xs.length;
        double percentage = 0.5;


        double cutoff1 = xs2[(int)(n * percentage)];
        double cutoff2 = ys2[(int)(n * percentage)];

        double[] tempx = new double[(int)(n * percentage)];
        double[] tempy = new double[(int)(n * percentage)];

        int count1 = 0;
        int count2 = 0;

        for(int i = 0; i < n; i++){

            if(xs[i] > cutoff1 && count1 < (int)(n * percentage)){
                tempx[count1] = xs[i];
                count1++;
            }

            if(ys[i] > cutoff2 && count2 < (int)(n * percentage)){
                tempy[count2] = ys[i];
                count2++;
            }

        }

        xs = tempx.clone();
        ys = tempy.clone();

        n = xs.length;

        for(int i = 0; i < xs.length; i++){
            sum1 += xs[i];
            sum2 += ys[i];
        }

        double mean1 = sum1 / (double)xs.length;
        double mean2 = sum2 / (double)ys.length;

        for(int i = 0; i < n; ++i) {

            double x = xs[i] - mean1;
            double y = ys[i] - mean2;


            sx += x;
            sy += y;
            sxx += x * x;
            syy += y * y;
            sxy += x * y;
           

        }

        // covariation
        double cov = sxy / (n * (1.0 - percentage)) - sx * sy / (n * (1.0 - percentage)) / (n * (1.0 - percentage));
        // standard error of x
        double sigmax = Math.sqrt(sxx / (n * (1.0 - percentage)) -  sx * sx / (n * (1.0 - percentage)) / (n * (1.0 - percentage)));
        // standard error of y
        double sigmay = Math.sqrt(syy / (n * (1.0 - percentage)) -  sy * sy / (n * (1.0 - percentage)) / (n * (1.0 - percentage)));

        // correlation is just a normalized covariation
        return cov / sigmax / sigmay;
    }

    public static double SSD(double[] in1, double[] in2, boolean normalized){

        double output = 0.0f;
        double diff = 0.0f;

        double sum1 = 0;
        double sum2 = 0;
        double mean1 = 0;
        double mean2 = 0;

        if(normalized){

            for(int i = 0; i < in1.length; i++){
                sum1 += in1[i];
                sum2 += in2[i];
            }
        
        mean1 = sum1 / (double)in1.length;
        mean2 = sum2 / (double)in1.length;


        for(int i = 1; i < in1.length; i++){
            diff = Math.abs( (in1[i] - mean1) - (in2[i] - mean2));
            output += diff * diff;
        }
        return output;
        }
        else{
            for(int i = 1; i < in1.length; i++){
                diff = Math.abs(in1[i] - in2[i]);
                output += diff * diff;
            }
        return output;

        }
        
    }

    /*
    public static GridCoverage2D grid;
    public static Raster gridData;

    public static void initTif() throws Exception {

        File tiffFile = new File("chm/chm_tin_hhs_d20.tif");
        GeoTiffReader reader = new GeoTiffReader(tiffFile);
 
        grid = reader.read(null);
        gridData = grid.getRenderedImage().getData();
    }
    
    */

    public static class Chm{

        String fileName = null;

        double xOrigin = 0;
        double yOrigin = 0;

        double xRight = 0;
        double yBottom = 0;

        int xSize = 0;
        int ySize = 0;

        double pixelWidth = 0;
        double pixelHeight = 0;

        double [] transform = null;
        float[][] data = null;

        Dataset dataSet = null;
        Band band = null;

        double width = 0;
        double height = 0;

        double[] dat = new double[1];

        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;

        ArrayList<Double> array1_list = new ArrayList<>();
        ArrayList<Double> array2_list = new ArrayList<>();

        public Chm(){


        }

        public Chm(String fileName2, createCHM.chm chm_in){


            org.gdal.gdal.Driver driver = null;
            driver = gdal.GetDriverByName("GTiff");
            driver.Register();


            fileName = fileName2;

            dataSet = gdal.Open(fileName, gdalconst.GA_ReadOnly);
            band = dataSet.GetRasterBand(1);

            xSize = dataSet.getRasterXSize();
            ySize = dataSet.getRasterYSize();

            transform = dataSet.GetGeoTransform();

            xOrigin = transform[0];
            yOrigin = transform[3];

            pixelWidth = transform[1];
            pixelHeight = -transform[5];

            System.out.println(Arrays.toString(transform));


            width = pixelWidth * xSize;
            height = pixelHeight * ySize;

            xRight = xOrigin + width;
            yBottom = yOrigin - height;

            //System.out.println("width: " + width + " height: "  + height);

            data = new float[ySize][xSize];

            for(int y = 0; y < ySize; y++) {

                chm_in.band.ReadRaster(0, y, xSize, 1, data[y]);
                //System.out.println(Arrays.toString(data[y]));

            }

            //System.out.println(dataSet.GetDriver());
            //System.exit(1);


            minMaxValues();

            //data = null;
            System.gc();

        }

        public void minMaxValues(){


            for(int i = 0; i < data.length; i++){
                for(int j = 0; j < data[0].length; j++){

                    if(data[i][j] > maxValue)
                        maxValue = data[i][j];
                    
                    if(data[i][j] < minValue)
                        minValue = data[i][j];

                }
            }

        }

        public float get(int x, int y){

            if(y < 0 || x < 0 || y >= ySize || x >= xSize)
                return Float.NaN;

            return data[y][x];

            //band.ReadRaster(x, y, 1, 1, dat);
            //return dat[0];

        }

        public double[] getX(){

            double[] output = new double[2];

            output[0] = xOrigin;
            output[1] = xRight;

            return output;

        }

        public double[] getY(){

            double[] output = new double[2];

            output[0] = yOrigin;
            output[1] = yBottom;

            return output;
        }

        public double getPixelWidth(){

            return pixelWidth;

        }

        public double getPixelHeight(){

            return pixelHeight;

        }

        public String getName(){

            return fileName;

        }


        public boolean isInside(Chm in){

            return in.getY()[0] > yOrigin &&
                    in.getY()[1] < yBottom &&
                    in.getX()[0] < xOrigin &&
                    in.getX()[1] > xRight;

        }

        /**
         *  
         *
         *
         */

        public double similarity(Chm in, double x_offset, double y_offset){

            //double[] array1 = new double[xSize * ySize];
            //double[] array2 = new double[xSize * ySize];
            //System.out.println(x_offset);
            int count = 0;

            double val1 = 0, val2 = 0;

            int nanCount = 0;
            array1_list.clear();
            array2_list.clear();

            for(int i = 0; i < ySize; i++){
                for(int j = 0; j < xSize; j++){
                    //System.out.println((int)((-(yOrigin - i * pixelHeight) + in.getY()[0]) / in.getPixelHeight()));

                    val1 = this.get(j,i);
                    val2 = in.get( (int)(((xOrigin + j * pixelWidth) - in.getX()[0] + x_offset) / in.getPixelWidth()),
                            (int)((-(yOrigin - i * pixelHeight) + in.getY()[0] + y_offset) / in.getPixelHeight()) );


                    //System.out.println(val1);
                    //System.out.println(val2);
                    //System.out.println("--------------");

                    if(Double.isNaN(val1) || Double.isNaN(val2) || val1 == 0 || val2 == 0)
                        continue;

                    array1_list.add(val1);
                    array2_list.add(val2);

                    /*
                    array1[count] = val1;

                    if(Double.isNaN(this.get(j,i))){
                        //System.out.println(nanCount++);
                    }
                    array2[count] = val2;

                    System.out.println(array1[count] + " & " + array2[count]);

                    count++;

                     */
                }
            }

            double[] array1 = new double[array1_list.size()];
            double[] array2 = new double[array2_list.size()];

            for(int i = 0; i < array1_list.size();i++){
                array1[i] = array1_list.get(i);
                array2[i] = array2_list.get(i);
            }
            //System.out.println(Correlation(array1, array2));
/*
            if(array1.length > 0) {
                System.out.println(Arrays.toString(array1));
                System.out.println(Arrays.toString(array1));
                System.out.println(Arrays.toString(array1));
                System.out.println(Arrays.toString(array1));
                System.out.println(Arrays.toString(array2));
                System.out.println(Arrays.toString(array2));
                System.out.println(Arrays.toString(array2));
                System.out.println(Arrays.toString(array2));
                System.exit(1);
            }
            */
            array1_list.clear();
            array2_list.clear();

            double output1 = -1;

            if(output.getCostFunction().equals("MI"))
                output1 = MI(array1,array2);
            if(output.getCostFunction().equals("Correlation"))
                output1 = Math.abs(uav.Correlation(array1, array2));
            if(output.getCostFunction().equals("SSD"))
                output1 =  -SSD(array1, array2, true);

            array1 = null;
            array2 = null;

            return output1;

        }   

    }

    public static void printGrid(double[][] in){ 

        for(int i = 0; i < in.length; i++){  
            for(int j = 0; j < in[0].length; j++){  
                    System.out.print(in[i][j] + "\t");
            }
            System.out.print("\n");
        }
    
    }

    public static void time(long current, long start){

            long difference = current - start; 
            double millis = difference/1000000;
            double seconds=(millis/1000)%60;
            double minutes=((millis-seconds)/1000)/60;
            System.out.println("\n");

            if((int)Math.floor(seconds) > 10)
                System.out.println("Time elapsed: " + (int)Math.floor(minutes) + " minutes " + (int)Math.floor(seconds) + " seconds ");
            else
                System.out.println("Time elapsed: " + (int)Math.floor(millis) + " ms");
    }
/*
    public static void DatasetToMat(Dataset data, Mat mat){

        int[] array1 = new int[1];

        //System.out.println(data);

        Band temp = data.GetRasterBand(1);

        for(int x = 0; x < mat.width(); x++){
            for(int y = 0; y < mat.height(); y++){

               int a = (int)temp.ReadRaster(x,
                      y,
                      1,
                      1,
                      array1); 
               System.out.println(a);

               mat.put(y, x, array1[0]);
            }


        }

        
    }

    public static void DatasetToMat(float[][] data, Mat mat){

        int[] array1 = new int[1];

        //System.out.println(data);

        //Band temp = data.GetRasterBand(1);

        for(int x = 0; x < data.length; x++){
            for(int y = 0; y < data[0].length; y++){

               int a = (int)data[x][y];

               //System.out.println(a);

               mat.put(y, x, a);
            }


        }

        
    }
*/
    public static void normalize(Chm in, double min, double max){



        for(int i = 0; i < in.data.length; i++){
            for(int j = 0; j < in.data[0].length; j++){

                in.data[i][j] = (float) ( (max - min) / (in.maxValue - in.minValue) * (in.data[i][j] - in.maxValue) + max);
                //  System.out.println(in.data[i][j]);

            }
        }

    }

    public static void main(String[] args) throws Exception {

        File logFile = new File("lokkeri.txt");

        if(!logFile.exists())
            logFile.createNewFile();



        FileWriter fw = new FileWriter(logFile, true);


/*
        System.loadLibrary("opencv_java320");

        if(!System.getProperty("os.name").equals("Linux"))
            System.loadLibrary("gdal202");


        System.out.println("!_-------------------------_!");

 */


        //initTif();
        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();



        long startT = System.nanoTime();

        String modi = "/media/koomikko/juuka/test/mod/97.tif";
       // String lasFile1 = "";
        String refi = "/media/koomikko/juuka/test/ref/97.tif";
       // String lasFile2 = "";

        String fileName = "";
        String fileName2 = "";

        if(args.length > 0){
            //System.out.println("got here");
            fileName = args[0];  // THIS IS THE ONE BEING MODIFIED
            fileName2 = args[1];

        }

        if(args.length > 2){
            //System.out.println("got here");
            modi = args[2];  // THIS IS THE ONE BEING MODIFIED
            refi = args[3];

        }

        System.out.println(fileName);

        File f1 = new File(fileName);
        File f2 = new File(fileName2);

        LASReader pCloudMod = new LASReader(f1);
        LASReader pCloudRef = new LASReader(f2);

        Chm chm1 = null; //new chm("modTemp.tif");
        Chm chm2 = null; //new Chm("refTemp.tif");

        argumentReader asdi = new argumentReader();

        asdi.step = 0.75;
        asdi.cores = 1;
        asdi.p_update = new progressUpdater(asdi);

        asdi.interpolate = true;

        asdi.lasrelate = false;

        asdi.theta = 0.6;

        asdi.inclusionRule = new PointInclusionRule();
        asdi.modifyRule = new PointModifyRule();
        //asdi.kernel = 5;

        //asdi.parseArguents();

        asdi.pfac = new lasReadWriteFactory(asdi);

        fw.write(pCloudMod.getFile().getName() + "\t");

        if(args.length <= 2){

            createCHM testi_c = new createCHM();
            //createCHM testi_c_2 = new createCHM();

            createCHM.chm asd = testi_c.new chm(pCloudMod, "y", 1, asdi, 1);
            createCHM.chm asd2 = testi_c.new chm(pCloudRef, "y", 1, asdi, 1);

            /*
            chm1 = new Chm("modTemp.tif");
            chm2 = new Chm("refTemp.tif");

             */

            /*
            chm1 = new Chm("modTemp.tif");
            chm2 = new Chm("refTemp.tif");

             */

            chm1 = new Chm(asd.outputFileName, asd);
            chm2 = new Chm(asd2.outputFileName, asd2);


        }

        else{

            //chm1 = new Chm(modi);
            //chm2 = new Chm(refi);

        }


        System.gc();
        System.gc();
        System.gc();
        System.gc();
        /*
        Chm chm1 = new Chm(modi);
        Chm chm2 = new Chm(refi);
        */

        System.out.println(chm1.isInside(chm2));
        System.out.println(chm2.isInside(chm1));

        
        //normalize(chm1, 100.0, 10000.0);
        //normalize(chm2, 100.0, 10000.0);


        int toBeMoved = -1;

        if(chm1.isInside(chm2))
            toBeMoved = 1;
        else
            toBeMoved = 2;

        System.out.println("To be moved: " + toBeMoved + " (should be one)");

        int maximumDifference = 4;

        int n_ = (int)((double)maximumDifference/0.25);

        //double[] offsetStep = new double[(maximumDifference * 2) * 2 + 1];
        double[] offsetStep = new double[n_ * 2 + 1];

        int count = 0;

        for(double i = -maximumDifference; count < offsetStep.length; i += 0.25){
            System.out.println(i);
            offsetStep[count] = i;
            count++;
        }

        //System.exit(1);

        double[][] offsetPairs = new double[offsetStep.length * offsetStep.length][2]; 
        count = 0;
        for(int i = 0; i < offsetStep.length; i++)
            for(int j = 0; j < offsetStep.length; j++){
                offsetPairs[count][0] = offsetStep[i];
                offsetPairs[count][1] = offsetStep[j];
                count++;
            }

        double minCost = 9999.999;

        double xOffset = -99f;
        double yOffset = -99f;
        double similarity = 99;

        proge.setEnd(offsetStep.length * offsetStep.length);

        int nCores = 1;

        output.setChms(chm1, chm2);
        output.setup();
        output.setCostFunction(1);

        ArrayList<Thread> lista = new ArrayList<Thread>();
        
        for(int i = 1; i <= nCores; i++){

            proge.addThread();

            int min = (int)((double)offsetPairs.length * ( ((double)i - 1.0) / (double)nCores ));
            int max = (int)((double)offsetPairs.length * ( ((double)i) / (double)nCores ));


            Thread temp = new Thread(new multiThreadAlign(offsetPairs, min, max, chm1, chm2));
            lista.add(temp);
            temp.start();        
            //System.out.println("\nmin: " + min + " \nmax: " + max);
            //System.out.println("\nmin: " + min + " \nmax: " + max);
        }

        for(int i = 0; i < lista.size(); i++){

                try{

                lista.get(i).join();
                }catch(Exception e) {
                    e.printStackTrace();
                }
        }
        //System.out.println(offsetPairs.length);
        //System.out.println("\nMinimum cost: " + output.getCost() + "\nX offset: " + output.getX() + 
          //  "\nY offset: " + output.getY());

        
        if(chm1.isInside(chm2) || output.special_condition){

            output.xOffset =  output.xOffset * -1;
            output.yOffset =  output.yOffset * -1;

        }
        
        long endT = System.nanoTime();
        //System.out.println("");
        //System.out.println("");
        time(endT, startT);
        //chm2.similarity(chm1, 0, -2);

        System.out.println(output.raport());

        fw.write(output.getX() + "\t" + output.getY() + "\n");
        fw.close();
        String lasFile1 = "";
        System.out.println("HIDSGHUSHGIO: " + fileName.split("/dz/")[0] + File.separator + "dz/");
        
        
        lasFile1 = fileName;//listFiles(fileName.split("/dz/")[0] + File.separator + "dz/",".las").get(listFiles(fileName.split("/dz/")[0] + File.separator + "dz/",".las").size() - 1);

        fileOperations fo = new fileOperations();

        String lasOutName = fo.createNewFileWithNewExtension(fileName, "_aligned.las").getAbsolutePath();
        // fileName.replaceFirst("[.][^.]+$", "") + "_aligned.las";

        File outti = new File(lasOutName);

        if(outti.exists())
            outti.delete();

        String a = "wine /media/koomikko/B8C80A93C80A4FD41/LAStools/bin/las2las.exe -i " + lasFile1 + 
        " -translate_x " + output.getX() + " -translate_y " + output.getY() + " -o " + lasOutName;

        process_las2las tooli = new process_las2las(1);

        argumentReader aR = new argumentReader();

        aR.translate_x = output.getX();
        aR.translate_y = output.getY();
        aR.inclusionRule = new PointInclusionRule();
        aR.inclusionRule.translate_x(output.getX());
        aR.inclusionRule.translate_y(output.getY());
        aR.pfac = new lasReadWriteFactory(aR);
        tooli.convert(pCloudMod, aR);



        String b = "wine /media/koomikko/B8C80A93C80A4FD41/LAStools/bin/lasview.exe -i " + lasOutName;

        String c = "gdal_edit.py " + " -a_ullr " + (chm1.getX()[0] + output.getX()) + " " + (chm1.getY()[0] + output.getY()) + 
        " " + (chm1.getX()[1] + output.getX()) + " " + (chm1.getY()[1] + output.getY()) + " " + "modTemp.tif";

        Vector<String> optionsVector = new Vector<>();
        optionsVector.add("-a_ullr");
        optionsVector.add(Double.toString(chm1.getX()[0] + output.getX()));
        optionsVector.add(Double.toString(chm1.getY()[0] + output.getY()));
        optionsVector.add(Double.toString(chm1.getX()[1] + output.getX()));
        optionsVector.add(Double.toString(chm1.getY()[1] + output.getY()));

        org.gdal.gdal.TranslateOptions optit = new org.gdal.gdal.TranslateOptions(optionsVector);


        if(false)
        try{

            Runtime rt = Runtime.getRuntime();
            //Process proc = rt.exec(c);
            //System.out.println(c);
            //proc.waitFor();
            
            Process proc2 = rt.exec(a);
            System.out.println(a);
            proc2.waitFor();
            
            
            Process proc3 = rt.exec(c);
            System.out.println(b);
            proc3.waitFor();
            

        }catch( Exception ex ) {System.out.println(ex.getMessage());}
        
        
        //File logi = new File("log.log");


        BufferedWriter bw = new BufferedWriter(new FileWriter("log.log", true));

        bw.write("\n" + output.getX() + "\t" + output.getY());
        bw.close();

        File fileToDelete = new File("modTemp.tif");
        File fileToDelete2 = new File("refTemp.tif");

        //fileToDelete.delete();
        //fileToDelete2.delete();
    }

}