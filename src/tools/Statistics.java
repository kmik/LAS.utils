package tools;

import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Statistics
{
    double[] data;
    float[] dataF;
    double size;
    ArrayList<Double> dataList;   
    ArrayList<Float> dataListF;  

    public Statistics(){


    }

    public Statistics(double[] data) 
    {
        this.data = data;
        size = data.length;
    }   

    public void setData(double[] data2){

        this.data = data2.clone();
        size = data2.length;
    }

    public void setData(ArrayList<Double> data2){

        this.dataList = data2;
        size = data2.size();
    }

    public void setDataF(ArrayList<Float> data2){

        this.dataListF = data2;
        size = data2.size();
    }


    double getMean()
    {
        double sum = 0.0;
        for(double a : data)
            sum += a;

        return sum / size;

    }

    double getMeanFromList()
    {
        double sum = 0.0;

        for(int i = 0; i < dataList.size(); i++)
            sum += dataList.get(i);

        return sum/size;

    }

    double getVariance()
    {
        
        double mean = getMean();
        double temp = 0;
        for(double a :data)
            temp += (a-mean)*(a-mean);
        return temp/(size-1);

    }

    double getVarianceFromList(double mean)
    {

        //double mean = getMean();
        double temp = 0;
        for(int i = 0; i < dataList.size(); i++)
            temp += (dataList.get(i)-mean)*(dataList.get(i)-mean);
        return temp/(size-1);

    }

    double getStdDev()
    {
        return Math.sqrt(getVariance());
    }
    double getStdDevFromList(double mean)
    {
        return Math.sqrt(getVarianceFromList(mean));
    }

    public double median() 
    {
       Arrays.sort(data);

       if (data.length % 2 == 0) 
       {
          return (data[(data.length / 2) - 1] + data[data.length / 2]) / 2.0;
       } 
       return data[data.length / 2];
    }

    public double medianFromListF() 
    {
       Collections.sort(dataList);

       if (dataList.size() % 2 == 0) 
       {
          return (dataList.get((dataList.size() / 2) - 1) + dataList.get(dataList.size() / 2)) / 2.0;
       } 
       return dataList.get(dataList.size() / 2);
    }

    // Function to calculate skewness.
    public double skewness(ArrayList<Double> arr, double mean, double std) {
        // Find skewness using
        // above formula
        double n = arr.size();
        double sum = 0;

        double accum3 = 0.0;
        for (double d : arr)
            accum3 += ((d - mean) * (d - mean) * (d - mean));

        accum3 /= ((std*std) * std);
        /*
        return sum / (n * (std *
                std *
                std *
                std));

         */

        return (n / ((n - 1) * (n - 2))) * accum3;
    }

    // Function to calculate kurtosis.
    public double kurtosis(ArrayList<Double> arr, double mean, double std) {
        // Find skewness using
        // above formula

        double accum3 = 0.0;
        for (double d : arr) {
            accum3 += ((d - mean) * (d - mean) * (d - mean) * (d - mean));
        }
        accum3 /= (std * std * std * std); // FastMath.pow(stdDev, 4.0d);
        double n = arr.size();

        double coefficientOne = (n * (n + 1)) / ((n - 1) * (n - 2) * (n - 3));
        double termTwo = (3 * ((n - 1) * (n - 1))) / ((n - 2) * (n - 3));
        return (coefficientOne * accum3) - termTwo;
    }


    public double skewness(double[] arr, double mean, double std) {
        // Find skewness using
        // above formula
        double n = arr.length;
        double sum = 0;

        double accum3 = 0.0;
        for (double d : arr)
            accum3 += ((d - mean) * (d - mean) * (d - mean));

        accum3 /= ((std*std) * std);
        /*
        return sum / (n * (std *
                std *
                std *
                std));

         */

        return (n / ((n - 1) * (n - 2))) * accum3;
    }

    // Function to calculate kurtosis.
    public double kurtosis(double[] arr, double mean, double std) {
        // Find skewness using
        // above formula

        double accum3 = 0.0;
        for (double d : arr) {
            accum3 += ((d - mean) * (d - mean) * (d - mean) * (d - mean));
        }
        accum3 /= (std * std * std * std); // FastMath.pow(stdDev, 4.0d);
        double n = arr.length;

        double coefficientOne = (n * (n + 1)) / ((n - 1) * (n - 2) * (n - 3));
        double termTwo = (3 * ((n - 1) * (n - 1))) / ((n - 2) * (n - 3));
        return (coefficientOne * accum3) - termTwo;
    }
}