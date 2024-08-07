package tools;

import org.gdal.gdal.Dataset;

import java.util.ArrayList;

/**
 * Contains the functionality to generate a gaussian filter kernel and apply
 * it to an image.
 *
 */
public class GaussianSmooth extends Thread {

  /**
   * Default no-args constructor.
   */
  public GaussianSmooth() {
  }

  /**
   * Calculates the discrete value at x,y of the
   * 2D gaussian distribution.
   *
   * @param theta     the theta value for the gaussian distribution
   * @param x         the point at which to calculate the discrete value
   * @param y         the point at which to calculate the discrete value
   * @return          the discrete gaussian value
   */
  public static double gaussianDiscrete2D(double theta, int x, int y){
    double g = 0;
    for(double ySubPixel = y - 0.5; ySubPixel < y + 0.55; ySubPixel += 0.1){
      for(double xSubPixel = x - 0.5; xSubPixel < x + 0.55; xSubPixel += 0.1){
	g = g + ((1/(2*Math.PI*theta*theta)) *
		 Math.pow(Math.E,-(xSubPixel*xSubPixel+ySubPixel*ySubPixel)/
			  (2*theta*theta)));
      }
    }
    g = g/121;
    //System.out.println(g);
    return g;
  }

  /**
   * Calculates several discrete values of the 2D gaussian distribution.
   *
   * @param theta     the theta value for the gaussian distribution
   * @param size      the number of discrete values to calculate (pixels)
   * @return          2Darray (size*size) containing the calculated
   * discrete values
   */
  public static double [][] gaussian2D(double theta, int size){

    double [][] kernel = new double [size][size];
    for(int j=0;j<size;++j){
      for(int i=0;i<size;++i){
	kernel[i][j]=gaussianDiscrete2D(theta,i-(size/2),j-(size/2));
      }
    }

    double sum = 0;
    for(int j=0;j<size;++j){
      for(int i=0;i<size;++i){
	sum = sum + kernel[i][j];

      }
    }

    return kernel;
  }

  public static void printRow(double[] row) {
        for (double i : row) {
            System.out.print(i);
            System.out.print("\t");
        }
        System.out.println();
  }

  public static void print(double[][] in){

    for(double[] row : in) {
        printRow(row);
    }

  }

  /**
   * Takes an image and a gaussian distribution, calculates an
   * appropriate kernel and applies a convolution to smooth the image.
   *
   * @param ks the required size of the kernel
   * @param theta the gaussian distribution
   * @return 2D array representing the smoothed image
   */
  public static double [][] smooth(double [][] input, int width, int height,
				   int ks, double theta){

    double[][] mirror = mirrorAll(input, ks);
    width += ks*2;
    height += ks*2;

    Convolution convolution = new Convolution();
    double [][] gaussianKernel = new double [ks][ks];
    double [][] output = new double [width][height];
    gaussianKernel = gaussian2D(theta,ks);

    //print(gaussianKernel);

    output = Convolution.convolution2DPadded(mirror,width,height,
					   gaussianKernel,ks,ks);


    return unmirrorAll(output, ks);

  }

  public static double[][] mirrorAll(double[][] array, int bufferPixels) {
    int width = array[0].length;
    int height = array.length;
    int newWidth = width + bufferPixels * 2;
    int newHeight = height + bufferPixels * 2;
    double[][] result = new double[newHeight][newWidth];

    // Copy the original array to the center of the new array
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        result[y + bufferPixels][x + bufferPixels] = array[y][x];
      }
    }

    // Mirror the top and bottom sides of the array
    for (int y = 0; y < bufferPixels; y++) {
      int topIndex = bufferPixels - y;
      int bottomIndex = newHeight - bufferPixels + y - 1;
      for (int x = 0; x < newWidth; x++) {
        result[topIndex][x] = result[bufferPixels + y][x];
        result[bottomIndex][x] = result[newHeight - bufferPixels - y - 1][x];
      }
    }

    // Mirror the left and right sides of the array
    for (int x = 0; x < bufferPixels; x++) {
      int leftIndex = bufferPixels - x;
      int rightIndex = newWidth - bufferPixels + x - 1;
      for (int y = 0; y < newHeight; y++) {
        result[y][leftIndex] = result[y][bufferPixels + x];
        result[y][rightIndex] = result[y][newWidth - bufferPixels - x - 1];
      }
    }

    return result;
  }

  public static double[][] unmirrorAll(double[][] arr, int buffer) {
    int height = arr.length - 2*buffer;
    int width = arr[0].length - 2*buffer;
    double[][] result = new double[height][width];
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        result[i][j] = arr[i+buffer][j+buffer];
      }
    }
    return result;
  }

  public static void smooth_tif(Dataset input, Dataset output, int width, int height,
                                       int ks, double theta){

    Convolution convolution = new Convolution(ks);
    double [][] gaussianKernel = new double [ks][ks];
    //double [][] output = new double [width][height];
    gaussianKernel = gaussian2D(theta,ks);

    Convolution.convolution2DPadded_tif(input, output, width,height,
            gaussianKernel,ks,ks);

  }

  /**
   * Takes an image and a gaussian distribution, calculates an
   * appropriate kernel and applies a convolution to smooth the image.
   *
   * @param ks the required size of the kernel
   * @param theta the gaussian distribution
   * @return 2D array representing the smoothed image
   */
  public static double [][] smooth2(double [][] input, int width, int height,
                                    int[] ks, double[] theta, double[] tHolds){
    
    Convolution convolution = new Convolution();

    ArrayList<double[][]> kernelList = new ArrayList<double[][]>();

    for(int i = 0; i < theta.length; i++){

      kernelList.add(gaussian2D(theta[i], ks[i]));
    }

    double [][] output = new double [width][height];


    output = Convolution.convolution2DPadded2(input,width,height,
             kernelList,ks,ks, tHolds);
    return output;
  }

  /**
   * Takes an input image and a gaussian distribution, calculates
   * an appropriate kernel and applies a convolution to gaussian
   * smooth the image.
   *
   * @param input the input image array
   * @param w the width of the image
   * @param h the height of the image
   * @param ks the size of the kernel to be generated
   * @param theta the gaussian distribution
   * @return smoothed image array
   */
  public static int [] smooth_image(int [] input, int w, int h,
				    int ks, double theta){
    double [][] input2D = new double [w][h];
    double [] output1D = new double [w*h];
    double [][] output2D = new double [w][h];
    int [] output = new int [w*h];
    //extract greys from input (1D array) and place in input2D
    for(int j=0;j<h;++j){
      for(int i=0;i<w;++i){
	input2D[i][j] = input[j*w+i]; //(new Color(input[j*w+i])).getRed();
      }
    }
    //now smooth this new 2D array
    output2D = smooth(input2D,w,h,ks,theta);

    for(int j=0;j<h;++j){
      for(int i=0;i<w;++i){
	output1D[j*w+i]=output2D[i][j];
      }
    }
    for(int i=0;i<output1D.length;++i){
      int grey = (int) Math.round(output1D[i]);
      if (grey > 255) { grey = 255;}
      if (grey < 0) { grey = 0;}
      output[i] = grey; //(new Color(grey,grey,grey)).getRGB();
    }

    return output;
  }

}