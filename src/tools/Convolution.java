package tools;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import java.awt.*;
import java.util.ArrayList;

/**
 * Convolution is the code for applying the convolution operator.
 *
 * @author: Simon Horne
 */
public class Convolution extends Thread {


    public static float[] floatArray = new float[1];
    public static float[] floatArrayColumn;
    public static float[] floatArrayRow;
    public static float[] floatArrayRowFull;
    public static float[] floatArrayKS;
    public static float[] floatArrayLarge;

  /**
   * Default no-arg constructor.
   */
  public Convolution() {



  }

  public Convolution(int kernelSiz){

      floatArrayRow = new float[kernelSiz * kernelSiz];

  }

  /**
   * Takes an image (grey-levels) and a kernel and a position,
   * applies the convolution at that position and returns the
   * new pixel value.
   *
   * @param input The 2D double array representing the image.
   * @param x The x coordinate for the position of the convolution.
   * @param y The y coordinate for the position of the convolution.
   * @param k The 2D array representing the kernel.
   * @param kernelWidth The width of the kernel.
   * @param kernelHeight The height of the kernel.
   * @return The new pixel value after the convolution.
   */
  public static double singlePixelConvolution(double [][] input,
					      int x, int y,
					      double [][] k,
					      int kernelWidth,
					      int kernelHeight){
    double output = 0;

    for(int i=0;i<kernelWidth;++i){
      for(int j=0;j<kernelHeight;++j){
        //System.out.println(" i: " + i + " j: " + j + " x: " + x + " y: " + y + " width: " + kernelWidth + " col: " + input.length + " row: " + input[0].length);
	       output = output + (input[x+i][y+j] * k[i][j]);

      }
    }
    return output;
  }

    public static double singlePixelConvolution_nan_tolerant(double [][] input,
                                                int x, int y,
                                                double [][] k,
                                                int kernelWidth,
                                                int kernelHeight){
        double output = 0;
        double output_2 = 0;

        for(int i=0;i<kernelWidth;++i){
            for(int j=0;j<kernelHeight;++j){

                if(Double.isNaN(input[x+i][y+j])){

                    output = output + (0.0 * k[i][j]);
                    output_2 = output_2 + (0.0 * k[i][j]);

                }else{
                    output_2 = output_2 + (1.0 * k[i][j]);
                    //System.out.println(" i: " + i + " j: " + j + " x: " + x + " y: " + y + " width: " + kernelWidth + " col: " + input.length + " row: " + input[0].length);
                    output = output + (input[x+i][y+j] * k[i][j]);
                }

            }
        }

        //System.out.println("o: " + (output/output_2));
        return output / output_2;
    }

    public static double singlePixelConvolution_tif(Band input,
                                                int x, int y,
                                                double [][] k,
                                                int kernelWidth,
                                                int kernelHeight){
        double output = 0;

        input.ReadRaster(x, y, kernelWidth, kernelHeight, floatArrayKS);

        int count = 0;
        //for(int i=0;i<kernelHeight;++i){
          //  for(int j=0;j<kernelWidth;++j){

        int x_1;
        int y_1;

        for(int i = 0; i < floatArrayKS.length; i++) {
            y_1 = i / kernelHeight;
            x_1 = i - (y_1 * kernelWidth);
                    //System.out.println(" i: " + i + " j: " + j + " x: " + x + " y: " + y + " width: " + kernelWidth + " col: " + input.length + " row: " + input[0].length);
            //input.ReadRaster(x + x_1, y + y_1, 1, 1, floatArray);
            output = output + (floatArrayKS[i] * k[x_1][y_1]);

            //if (!Float.isNaN(floatArray[0]))
              //  System.out.println(floatArrayRow[count++] + " == " + floatArray[0]);
        }

          //  }
        //}
        return output;
    }

    public static double singlePixelConvolution_tif_array(float[] input,
                                                    int x,
                                                    double [][] k,
                                                    int kernelWidth,
                                                    int kernelHeight){
        double output = 0;
        double output_2 = 0;
        int count = 0;
        int index;
        float value;

        for(int y = 0; y < kernelHeight; y++){
            for(int x1 = 0; x1 < kernelWidth; x1++) {

                index = floatArrayRowFull.length * y + (x + x1);

                value = input[index];

                //if(Float.isNaN(value)){
                //    value = 0.0f;
                //}

                output = output + (value * k[x1][y]);

            }
        }

        //  }
        //}
        return output;
    }

    public static double singlePixelConvolution_tif_array_nan_tolerant(float[] input,
                                                          int x,
                                                          double [][] k,
                                                          int kernelWidth,
                                                          int kernelHeight){
        double output = 0;
        double output_2 = 0;
        int count = 0;
        int index;
        float value;

        for(int y = 0; y < kernelHeight; y++){
            for(int x1 = 0; x1 < kernelWidth; x1++) {

                index = floatArrayRowFull.length * y + (x + x1);

                value = input[index];

                if(Float.isNaN(value)){
                    output = output + 0.0 * k[x1][y];
                    output_2 = output_2 + 0.0 * k[x1][y];
                }else{
                    output_2 = output_2 + 1.0 * k[x1][y];
                    output = output + (value * k[x1][y]);
                }
            }
        }

        //  }
        //}
        return output / output_2;
    }
  /**
   * Takes an image (grey-levels) and a kernel and a position,
   * applies the convolution at that position and returns the
   * new pixel value.
   *
   * @param input The 2D double array representing the image.
   * @param x The x coordinate for the position of the convolution.
   * @param y The y coordinate for the position of the convolution.
   * @param k The 2D array representing the kernel.
   * @param kernelWidth The width of the kernel.
   * @param kernelHeight The height of the kernel.
   * @return The new pixel value after the convolution.
   */
  public static double singlePixelConvolution2(double [][] input,
                int x, int y,
                ArrayList<double[][]> k,
                int[] kernelWidth,
                int[] kernelHeight,
                double[] tHolds){

    double output = 0;

    int chosenKernel = -1;

    for(int i = 0; i < tHolds.length; i++){

      if(input[x][y] < tHolds[i]){
        chosenKernel = i;
        break;
      }

    }

    if(chosenKernel == -1)
      chosenKernel = k.size() - 1;

    for(int i = 0; i < kernelWidth[chosenKernel]; ++i){
      for(int j = 0; j < kernelHeight[chosenKernel]; ++j){

         output = output + (input[x+i][y+j] * k.get(chosenKernel)[i][j]);

      }
    }
    return output;
  }

  public static int applyConvolution(int [][] input,
				     int x, int y,
				     double [][] k,
				     int kernelWidth,
				     int kernelHeight){
    int output = 0;
    for(int i=0;i<kernelWidth;++i){
      for(int j=0;j<kernelHeight;++j){
	output = output + (int) Math.round(input[x+i][y+j] * k[i][j]);
      }
    }
    return output;
  }


    public static double [][] convolution2DPadded(double [][] input,
                                                  int width, int height,
                                                  double [][] kernel,
                                                  int kernelWidth,
                                                  int kernelHeight){

        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;

        int top = kernelHeight/2;
        int left = kernelWidth/2;

        double[][] small  = new double [smallWidth][smallHeight];

        small = convolution2D(input,width,height,kernel,kernelWidth,kernelHeight);

        double[][] large  = new double [width][height];

        for(int j = 0; j < height; ++j){
            for(int i = 0; i < width; ++i){
                large[i][j] = 0;
            }
        }
        for(int j = 0; j < smallHeight; ++j){
            for(int i = 0; i < smallWidth; ++i){
                //if (i+left==32 && j+top==100) System.out.println("Convolve2DP: "+small[i][j]);
                large[i+left][j+top] = small[i][j];
            }
        }

        return large;
    }


    /**
     * Takes a 2D array of grey-levels and a kernel and applies the convolution
     * over the area of the image specified by width and height.
     *
     * @param input the 2D double array representing the image
     * @param width the width of the image
     * @param height the height of the image
     * @param kernel the 2D array representing the kernel
     * @param kernelWidth the width of the kernel
     * @param kernelHeight the height of the kernel
     * @return the 2D array representing the new image
     */
    public static double [][] convolution2D(double [][] input,
                                            int width, int height,
                                            double [][] kernel,
                                            int kernelWidth,
                                            int kernelHeight){

        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;

        double [][] output = new double [smallWidth][smallHeight];

        for(int i=0;i<smallWidth;++i){
            for(int j=0;j<smallHeight;++j){
                output[i][j]=0;
            }
        }
        for(int i=0;i<smallWidth;++i){
            for(int j=0;j<smallHeight;++j){

                output[i][j] = singlePixelConvolution(input,i,j,kernel,
                        kernelWidth,kernelHeight);
                if(Double.isNaN(output[i][j])){
                    output[i][j] = singlePixelConvolution_nan_tolerant(input,i,j,kernel,
                            kernelWidth,kernelHeight);

                }
//if (i==32- kernelWidth + 1 && j==100- kernelHeight + 1) System.out.println("Convolve2D: "+output[i][j]);
            }
        }
        return output;
    }


    public static void convolution2DPadded_tif(Dataset input, Dataset output,
                                                  int width, int height,
                                                  double [][] kernel,
                                                  int kernelWidth,
                                                  int kernelHeight){


        //floatArrayColumn = new float[height];
        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;


        floatArrayColumn = new float[smallHeight];
        //floatArrayColumn = new float[smallHeight-kernelHeight];
        //floatArrayRow = new float[smallWidth-kernelWidth];
        floatArrayRow = new float[smallWidth];
        floatArrayRowFull = new float[input.getRasterXSize()];

        floatArrayKS = new float[kernelWidth * kernelHeight];

        floatArrayLarge = new float[floatArrayRowFull.length * kernelHeight];

        int top = kernelHeight/2;
        int left = kernelWidth/2;

        Band inBand = input.GetRasterBand(1);
        Band outBand = output.GetRasterBand(1);

        int startY = kernelHeight/2;
        int startX = kernelWidth/2;

        double convolutionValue = 0d;

        int counter = 0;

        for(int x = 0; x < floatArrayRowFull.length; x++)
            floatArrayRowFull[x] = 0;

        for(int y = 0; y < height; y++) {

            outBand.WriteRaster(0,y,width,1, floatArrayRowFull);

        }

            //for(int y = 0; y < smallHeight-kernelHeight; y++){
        for(int y = 0; y < smallHeight; y++){

            inBand.ReadRaster(0, y, floatArrayRowFull.length, kernelHeight, floatArrayLarge);

            //for(int x = 0; x < floatArrayRow.length; x++) {
            for(int x = 0; x < floatArrayRow.length; x++) {

                    convolutionValue = singlePixelConvolution_tif_array(floatArrayLarge, x, kernel,
                            kernelWidth, kernelHeight);

                if(Double.isNaN(convolutionValue)){

                    convolutionValue = singlePixelConvolution_tif_array_nan_tolerant(floatArrayLarge, x, kernel,
                            kernelWidth, kernelHeight);
                }

                floatArray[0] = (float) convolutionValue;

                counter++;

                floatArrayRow[x] = floatArray[0];

            }
            outBand.WriteRaster(left, y + top, floatArrayRow.length, 1, floatArrayRow);
        }

    }
/*
    public static void convolution2D_tif(Band input, Band output,
                                            int width, int height,
                                            double [][] kernel,
                                            int kernelWidth,
                                            int kernelHeight){

        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;

        int startY = kernelHeight/2;
        int startX = kernelWidth/2;

        double [][] output = new double [smallWidth][smallHeight];

        for(int i=0;i<smallWidth;++i){
            for(int j=0;j<smallHeight;++j){
                output[i][j]=0;
            }
        }
        for(int i=0;i<smallWidth;++i){
            for(int j=0;j<smallHeight;++j){
                output[i][j] = singlePixelConvolution_tif(input,i,j,kernel,
                        kernelWidth,kernelHeight);
//if (i==32- kernelWidth + 1 && j==100- kernelHeight + 1) System.out.println("Convolve2D: "+output[i][j]);
            }
        }
        return output;
    }
*/
  /**
   * Takes a 2D array of grey-levels and a kernel and applies the convolution
   * over the area of the image specified by width and height.
   *
   * @param input the 2D double array representing the image
   * @param width the width of the image
   * @param height the height of the image
   * @param kernel the 2D array representing the kernel
   * @param kernelWidth the width of the kernel
   * @param kernelHeight the height of the kernel
   * @return the 2D array representing the new image
   */
  public static double [][] convolution2D2(double [][] input,
                int width, int height,
                ArrayList<double[][]> kernel,
                int[] kernelWidth,
                int[] kernelHeight,
                double[] tHolds){

    int smallWidth = width - kernelWidth[kernelWidth.length - 1] + 1;
    int smallHeight = height - kernelHeight[kernelWidth.length - 1] + 1;

    double [][] output = new double [smallWidth][smallHeight];
    
    for(int i = 0; i < smallWidth;++i){
      for(int j = 0;j < smallHeight;++j){
         output[i][j] = 0;
      }
    }

    for(int i = 0; i < smallWidth; ++i){
      for(int j = 0;j < smallHeight; ++j){

         output[i][j] = singlePixelConvolution2(input,i,j,kernel,
          kernelWidth,kernelHeight, tHolds);
          //if (i==32- kernelWidth + 1 && j==100- kernelHeight + 1) System.out.println("Convolve2D: "+output[i][j]);
      }
    }
    return output;
  }

   /**
   * Takes a 2D array of grey-levels and a kernel, applies the convolution
   * over the area of the image specified by width and height and returns
   * a part of the final image.
   * @param input the 2D double array representing the image
   * @param width the width of the image
   * @param height the height of the image
   * @param kernel the 2D array representing the kernel
   * @param kernelWidth the width of the kernel
   * @param kernelHeight the height of the kernel
   * @return the 2D array representing the new image
   */


  /**
   * Takes a 2D array of grey-levels and a kernel, applies the convolution
   * over the area of the image specified by width and height and returns
   * a part of the final image.
   * @param input the 2D double array representing the image
   * @param width the width of the image
   * @param height the height of the image
   * @param kernel the 2D array representing the kernel
   * @param kernelWidth the width of the kernel
   * @param kernelHeight the height of the kernel
   * @return the 2D array representing the new image
   */
  public static double [][] convolution2DPadded2(double [][] input,
            int width, int height,
            ArrayList<double[][]> kernel,
            int[] kernelWidth,
            int[] kernelHeight,
            double[] tHolds){

    int smallWidth = width - kernelWidth[kernelWidth.length - 1] + 1;
    int smallHeight = height - kernelHeight[kernelWidth.length - 1] + 1;

    int top = kernelHeight[kernelWidth.length - 1]/2;
    int left = kernelWidth[kernelWidth.length - 1]/2;

    double[][] small  = new double [smallWidth][smallHeight];

    small = convolution2D2(input,width,height,kernel,kernelWidth,kernelHeight, tHolds);

    double[][] large  = new double [width][height];

    for(int j = 0; j < height; ++j){
      for(int i = 0; i < width; ++i){
         large[i][j] = 0;
      }
    }
    for(int j = 0; j < smallHeight; ++j){
      for(int i = 0; i < smallWidth; ++i){
      //if (i+left==32 && j+top==100) System.out.println("Convolve2DP: "+small[i][j]);
        large[i+left][j+top] = small[i][j];
      }
    }
    
    return large;
  }

   /**
   * Takes a 2D array of grey-levels and a kernel and applies the convolution
   * over the area of the image specified by width and height.
   *
   * @param input the 2D double array representing the image
   * @param width the width of the image
   * @param height the height of the image
   * @param kernel the 2D array representing the kernel
   * @param kernelWidth the width of the kernel
   * @param kernelHeight the height of the kernel
   * @return the 1D array representing the new image
   */
  public static double [] convolutionDouble(double [][] input,
					    int width, int height,
					    double [][] kernel,
					    int kernelWidth, int kernelHeight){
    int smallWidth = width - kernelWidth + 1;
    int smallHeight = height - kernelHeight + 1;
    double [][] small = new double [smallWidth][smallHeight];
    small = convolution2D(input,width,height,kernel,kernelWidth,kernelHeight);
    double [] result = new double  [smallWidth * smallHeight];
    for(int j=0;j<smallHeight;++j){
      for(int i=0;i<smallWidth;++i){
	result[j*smallWidth +i]=small[i][j];
      }
    }
    return result;
  }

 /**
   * Takes a 2D array of grey-levels and a kernel and applies the convolution
   * over the area of the image specified by width and height.
   *
   * @param input the 2D double array representing the image
   * @param width the width of the image
   * @param height the height of the image
   * @param kernel the 2D array representing the kernel
   * @param kernelWidth the width of the kernel
   * @param kernelHeight the height of the kernel
   * @return the 1D array representing the new image
   */
  public static double [] convolutionDoublePadded(double [][] input,
						  int width, int height,
						  double [][] kernel,
						  int kernelWidth,
						  int kernelHeight){
    double [][] result2D = new double [width][height];
    result2D = convolution2DPadded(input,width,height,
				   kernel,kernelWidth,kernelHeight);
    double [] result = new double  [width * height];
    for(int j=0;j<height;++j){
      for(int i=0;i<width;++i){
	result[j*width +i]=result2D[i][j];
//if (i==32 && j==100) System.out.println("ConvolveDP: "+result[j*width +i]+" "+result2D[i][j]);
      }
    }
    return result;
  }

  /**
   * Converts a greylevel array into a pixel array.
   * @return the 1D array of RGB pixels.
   */
  public static int [] doublesToValidPixels (double [] greys){
    int [] result = new int [greys.length];
    int grey;
    for(int i=0;i<greys.length;++i){
      if(greys[i]>255){
	grey = 255;
      }else if(greys[i]<0){
	grey = 0;
      }else{
	grey = (int) Math.round(greys[i]);
      }
      result[i] = (new Color(grey,grey,grey)).getRGB();
    }
    return result;
  }

  /**
   * Applies the convolution2D algorithm to the input array as many as
   * iterations.
   * @param input the 2D double array representing the image
   * @param width the width of the image
   * @param height the height of the image
   * @param kernel the 2D array representing the kernel
   * @param kernelWidth the width of the kernel
   * @param kernelHeight the height of the kernel
   * @param iterations the number of iterations to apply the convolution
   * @return the 2D array representing the new image
   */
  public double [][] convolutionType1(double [][] input,
				      int width, int height,
				      double [][] kernel,
				      int kernelWidth, int kernelHeight,
				      int iterations){
    double [][] newInput = input.clone();
    double [][] output = input.clone();
    for(int i=0;i<iterations;++i){
      int smallWidth = width-kernelWidth+1;
      int smallHeight = height-kernelHeight+1;
      output = new double [smallWidth][smallHeight];
      output = convolution2D(newInput,width,height,
			     kernel,kernelWidth,kernelHeight);
      width = smallWidth;
      height = smallHeight;
      newInput = output.clone();
    }
    return output;
  }
   /**
   * Applies the convolution2DPadded  algorithm to the input array as many as
   * iterations.
   * @param input the 2D double array representing the image
   * @param width the width of the image
   * @param height the height of the image
   * @param kernel the 2D array representing the kernel
   * @param kernelWidth the width of the kernel
   * @param kernelHeight the height of the kernel
   * @param iterations the number of iterations to apply the convolution
   * @return the 2D array representing the new image
   */
  public double [][] convolutionType2(double [][] input,
				      int width, int height,
				      double [][] kernel,
				      int kernelWidth, int kernelHeight,
				      int iterations){
    double [][] newInput = input.clone();
    double [][] output = input.clone();

    for(int i=0;i<iterations;++i){
      output = new double [width][height];
//System.out.println("Iter: "+i+" conIN(50,50): "+newInput[50][50]);
      output = convolution2DPadded(newInput,width,height,
			     kernel,kernelWidth,kernelHeight);
//System.out.println("conOUT(50,50): "+output[50][50]);
      newInput = output.clone();
    }
    return output;
  }
   /**
   * Applies the convolution2DPadded  algorithm and an offset and scale factors
   * @param input the 1D int array representing the image
   * @param width the width of the image
   * @param height the height of the image
   * @param kernel the 2D array representing the kernel
   * @param kernelWidth the width of the kernel
   * @param kernelHeight the height of the kernel
   * @param scale the scale factor to apply
   * @param offset the offset factor to apply
   * @return the 1D array representing the new image
   */
  public static int [] convolution_image(int [] input ,int width, int height,
					 double [][] kernel,
					 int kernelWidth,int kernelHeight,
					 double scale, double offset){
    double [][] input2D = new double [width][height];
    double [] output = new double [width*height];
    for(int j=0;j<height;++j){
      for(int i=0;i<width;++i){
	input2D[i][j] = (new Color(input[j*width+i])).getRed();
      }
    }
    output = convolutionDoublePadded(input2D,width,height,
				       kernel,kernelWidth,kernelHeight);
    int [] outputInts = new int [width*height];

    for(int i=0;i<outputInts.length;++i){
      outputInts[i] = (int) Math.round(output[i] * scale + offset);
      if(outputInts[i]>255) outputInts[i] = 255;
      if(outputInts[i]<0) outputInts[i] = 0;
      int g = outputInts[i];
      outputInts[i] = (new Color(g,g,g)).getRGB();
    }
    return outputInts;
  }
}
