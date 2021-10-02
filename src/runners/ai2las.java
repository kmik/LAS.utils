package runners;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.gdal.Dataset;

import org.gdal.gdal.Band;


//import Tinfour.*;
import LASio.*;
//import org.nd4j.linalg.primitives.Atomic;
import org.nd4j.shade.guava.util.concurrent.AtomicDoubleArray;
import utils.*;
class ai2las{
	public static listOfFiles tiedostoLista = new listOfFiles();
	public static ThreadProgressBar proge = new ThreadProgressBar();

	public int n_bands = -1;

	public static fileOperations fo = new fileOperations();
	//public static double[][] rotationMatrix = new double[3][3];

	public static class ThreadProgressBar{
		int current = 0;
		int end = 0;
		String name = "give me name!";
		int numberOfThreads = 0;

		public ThreadProgressBar(){

		}

		public synchronized void setEnd(int newEnd){
			end = newEnd;
		}

		public synchronized void updateCurrent(int input){

			current += input;

		}

		public synchronized void reset(){

			current = 0;
			numberOfThreads = 0;
			end = 0;
			name = "give me name!";

		}

		public void setName(String nimi){
			//System.out.println("Setting name to");
			name = nimi;

		}

		public void addThread(){

			numberOfThreads++;

		}

		public synchronized void print(){
			//System.out.println(end);
			progebar(end, current, " " + name);
			//System.out.println(end + " " + current);
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

	public static class listOfFiles{

		ArrayList<String> files = new ArrayList<String>();






		public listOfFiles(){


		}

		public synchronized void add(ArrayList<String> in){

			files.addAll(in);

		}

	}

	static class multiTXT2LAS implements Runnable{

		private final String parse;
		ArrayList<String> tiedostot;
		ArrayList<String> returnList = new ArrayList<String>();
		int numberOfCores;
		int coreNumber;
		String odir;

		public multiTXT2LAS (ArrayList<String> tiedostot2, String parse2, int numberOfCores2, int coreNumber2, String odir2){

			tiedostot = tiedostot2;
			parse = parse2;
			numberOfCores = numberOfCores2;
			coreNumber = coreNumber2;
			odir = odir2;
		}

		public ArrayList<String> getList(){

			return returnList;

		}

		public void run() {

			try {

				int pienin = 0;
				int suurin = 0;
				if(coreNumber != 0){

					int jako = (int)Math.ceil((double)tiedostot.size() / (double) numberOfCores);
					//System.out.println(plotID1.size() / (double)cores);
					if(coreNumber != numberOfCores){

						pienin = (coreNumber - 1) * jako;
						suurin = coreNumber * jako;
					}

					else{
						pienin = (coreNumber - 1) * jako;
						suurin = tiedostot.size();
					}

					tiedostot = new ArrayList<String>(tiedostot.subList(pienin, suurin));
					//System.out.println(tiedostot);
					//polyBank = new ArrayList<double[][]>(polyBank1.subList(pienin, suurin));

				}
				else{

					//tiedostot = new ArrayList<Double>(tiedostot);
					//polyBank = new ArrayList<double[][]>(polyBank1);
				}



				ArrayList<File> from = new ArrayList<File>();
				ArrayList<LASraf> to = new ArrayList<LASraf>();
				ArrayList<String> outList = new ArrayList<String>();

				for(int i = 0; i < tiedostot.size(); i++){

					File tempFile = new File(tiedostot.get(i));

					File toFile = null;

					if(odir.equals("asd"))

						toFile = fo.createNewFileWithNewExtension(tempFile, ".las");
					//  new File(tiedostot.get(i).replaceFirst("[.][^.]+$", "") + ".las");

					if(!odir.equals("asd"))

						toFile = fo.createNewFileWithNewExtension(tempFile, odir, ".las");
					// new File(odir + System.getProperty("file.separator") + tempFile.getName().replaceFirst("[.][^.]+$", "") + ".las");

					//System.out.println(toFile);
					File fromFile = new File(tiedostot.get(i));

					//System.out.println(odir + System.getProperty("file.separator") + tempFile.getName().replaceFirst("[.][^.]+$", "") + ".las");

					if(toFile.exists())
						toFile.delete();


					toFile.createNewFile();

					//System.out.println(toFile);
					from.add(fromFile);

					to.add(new LASraf(toFile));
					//System.exit(0);
					outList.add(fo.createNewFileWithNewExtension(tiedostot.get(i), ".las").getAbsolutePath());
					//outList.add(tiedostot.get(i).replaceFirst("[.][^.]+$", "") + ".las");

				}
				PointInclusionRule rule = new PointInclusionRule();
				tiedostoLista.add(outList);

				for(int i = 0; i < tiedostot.size(); i++){

					LASwrite.txt2las(from.get(i), to.get(i), parse, "txt2las", " ", rule, false);
					to.get(i).writeBuffer2();
					to.get(i).close();
					//System.out.println("GOT HERE");
					proge.updateCurrent(1);
					if(i % 10 == 0)
						proge.print();
				}
				//return 1;
			} catch (Exception e) {
				//System.out.println(e);
				System.out.println(e.getMessage());

			}

		}



	}





	public static void readImages(File eoInformation, ArrayList<Dataset> images, ArrayList<ArrayList<Dataset>> threadImages, ArrayList<Integer> imageID, ArrayList<double[]> eos, String sep, double[] interior, ArrayList<double[]> extents,
								  double minZ, double[][] rotationMatrix, ArrayList<String> imageNames, argumentReader aR){


		int cores = 5;

		File file = eoInformation;

		LasPoint tempPoint = new LasPoint();

		int count = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {

			String line;

			while ((line = br.readLine()) != null) {

				threadImages.add(new ArrayList<Dataset>());

				String[] tokens = line.split(sep);  //THIS should be of length 8: (1) Filepath, (2) id, (3) Camera X, (4) Camera Y,
				// (5) Camera Z, (6) Omega, (7) Phi, (8) Kappa
				//System.out.println("Address: " + tokens[0]);
				Dataset tempDataset = null;

				try {
					if (System.getProperty("os.name").equals("Linux"))
						tempDataset = gdal.Open("/" + tokens[0]);
					else
						tempDataset = gdal.Open(tokens[0]);
				}catch (Exception e){
					System.out.println("WARNING!! " + tokens[0] + " DOES NOT EXIST!!");
					continue;
				}


				try{
					double test = (double)tempDataset.GetRasterXSize();
				}catch (Exception e){
					System.out.println("WARNING!! " + tokens[0] + " DOES NOT EXIST!!");
					continue;
				}
				System.out.print(count++ + " Images read\r");
				double sensorSize = interior[1] * (double)tempDataset.GetRasterXSize();
				double focalLength_millimeters = interior[0] * 1000.0;



				//System.out.println(focalLength_millimeters);
				//System.out.println(tempDataset);
				images.add(tempDataset);
				imageNames.add("/" + tokens[0]);
				double[] eoTemp = new double[6];

				eoTemp[0] = Double.parseDouble(tokens[2]);
				eoTemp[1] = Double.parseDouble(tokens[3]);
				eoTemp[2] = Double.parseDouble(tokens[4]);
				eoTemp[3] = Double.parseDouble(tokens[5]);
				eoTemp[4] = Double.parseDouble(tokens[6]);
				eoTemp[5] = Double.parseDouble(tokens[7]);

				double flyingHeight = eoTemp[2] - minZ;

				if(aR.altitude != 0.0)
					flyingHeight = aR.altitude;

				double gsd = ((sensorSize * flyingHeight * 100.0) / (focalLength_millimeters * (double)tempDataset.GetRasterXSize())) / 100.0;

				System.out.println("img altitude: " + flyingHeight + " gsd: " + gsd);


				tempPoint.x = eoTemp[0];
				tempPoint.y = eoTemp[1];
				tempPoint.z = minZ; //DOES NOT

				eos.add(eoTemp);
				imageID.add(Integer.parseInt(tokens[1]));

				double[] extentsThis = new double[4];
				double x_s = tempDataset.getRasterXSize();
				double y_s = tempDataset.getRasterYSize();
				double[] temp = collinearStuff(tempPoint, interior, eoTemp, rotationMatrix, x_s, y_s);
		    	/*
		    	System.out.println(gsd);
		    	System.out.println(temp[0] + " " + (double)tempDataset.GetRasterXSize() / 2.0);
		    	System.out.println(temp[1] + " " + (double)tempDataset.GetRasterYSize() / 2.0);
		    	System.out.println("-----------------");
				*/
				Math.max((double)tempDataset.GetRasterXSize() / 2.0, temp[0]);


				extentsThis[0] = eoTemp[0] - ((double)tempDataset.GetRasterXSize() / 1.0 * gsd);
				extentsThis[1] = eoTemp[0] + ((double)tempDataset.GetRasterXSize() / 1.0 * gsd);

				extentsThis[2] = eoTemp[1] - ((double)tempDataset.GetRasterYSize() / 1.0 * gsd);
				extentsThis[3] = eoTemp[1] + ((double)tempDataset.GetRasterYSize() / 1.0 * gsd);


				extents.add(extentsThis.clone());

			}

		}
		catch (Exception e){
			e.printStackTrace(System.out); // handle exception
		}

		System.out.println("");

	}


	public static double[] readIo(File ioInformation, String sep){

		double[] output = new double[6];

		File file = ioInformation;

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {

			String line;

			while ((line = br.readLine()) != null) {

				String[] tokens = line.split(sep);

				output[0] = Double.parseDouble(tokens[0]); //Focal length
				output[1] = Double.parseDouble(tokens[1]); // pixel size
				output[2] = Double.parseDouble(tokens[2]);	//ppx
				output[3] = Double.parseDouble(tokens[3]);  //ppy
				//output[4] = Double.parseDouble(tokens[4]);
				//output[5] = Double.parseDouble(tokens[5]);

			}
		}
		catch (Exception e){
			System.err.println(e.getMessage()); // handle exception
		}

		return output;

	}

	public static double[] collinearStuff(LasPoint point, double[] io, double[] eo, double[][] rotationMatrix, double x_s, double y_s){

		double[] output = new double[2];

		double pointX = point.x;
		double pointY = point.y;
		double pointZ = point.z;

		double cameraX = eo[0];
		double cameraY = eo[1];
		double cameraZ = eo[2];

		double omega = eo[3];
		double phi = eo[4];
		double kappa = eo[5];

		double cc = io[0];
		double ps = io[1] / 1000.0; //12.0 / 1000000.0;

		double nc = x_s;
		double nr = y_s;

		double ppx = io[2];
		double ppy = io[3];

		//Mat rotation = new Mat(3,3, CvType.CV_64FC1);

		rotationMatrix[0][0] = Math.cos(phi) * Math.cos(kappa);

		rotationMatrix[0][1] =  Math.cos(omega) * Math.sin(kappa) + Math.sin(omega) * Math.sin(phi) * Math.cos(kappa);

		rotationMatrix[0][2] = Math.sin(omega) * Math.sin(kappa) - Math.cos(omega) * Math.sin(phi) * Math.cos(kappa);

		rotationMatrix[1][0] = -Math.cos(phi) * Math.sin(kappa);

		rotationMatrix[1][1] = Math.cos(omega) * Math.cos(kappa) - Math.sin(omega) * Math.sin(phi) * Math.sin(kappa);

		rotationMatrix[1][2] = Math.sin(omega) * Math.cos(kappa) + Math.cos(omega) * Math.sin(phi) * Math.sin(kappa);

		rotationMatrix[2][0] = Math.sin(phi);

		rotationMatrix[2][1] = -Math.sin(omega) * Math.cos(phi);

		rotationMatrix[2][2] = Math.cos(omega) * Math.cos(phi);

		double xx0;
		double yy0;

		xx0 = -cc * ( ( rotationMatrix[0][0] * (pointX - cameraX) + rotationMatrix[0][1] * (pointY - cameraY) + rotationMatrix[0][2] * (pointZ - cameraZ) ) /
				( rotationMatrix[2][0] * (pointX - cameraX) + rotationMatrix[2][1] * (pointY - cameraY) + rotationMatrix[2][2] * (pointZ - cameraZ) ) );

		yy0 = -cc * ( ( rotationMatrix[1][0] * (pointX - cameraX) + rotationMatrix[1][1] * (pointY - cameraY) + rotationMatrix[1][2] * (pointZ - cameraZ) ) /
				( rotationMatrix[2][0] * (pointX - cameraX) + rotationMatrix[2][1] * (pointY - cameraY) + rotationMatrix[2][2] * (pointZ - cameraZ) ) );


		double x = xx0 + ppx;
		double y = yy0 + ppy;

		double xpx = (nc / 2.0) + (x / ps);
		double ypx = (nr / 2.0) - (y / ps);

		output[0] = xpx;
		output[1] = ypx;

		return output;

	}

	public static double[] collinearStuff2(LasPoint point, double[] io, double[] eo, double[][] rotationMatrix, double x_s, double y_s){

		double[] output = new double[2];

		double pointX = point.x;
		double pointY = point.y;
		double pointZ = point.z;

		double cameraX = eo[0];
		double cameraY = eo[1];
		double cameraZ = eo[2];

		double omega = eo[3];
		double phi = eo[4];
		double kappa = eo[5];

		double cc = io[0];
		double ps = io[1] / 1000.0; //12.0 / 1000000.0;

		double nc = x_s;
		double nr = y_s;

		double ppx = io[2];
		double ppy = io[3];

		double xx0;
		double yy0;

		xx0 = -cc * ( ( rotationMatrix[0][0] * (pointX - cameraX) + rotationMatrix[0][1] * (pointY - cameraY) + rotationMatrix[0][2] * (pointZ - cameraZ) ) /
				( rotationMatrix[2][0] * (pointX - cameraX) + rotationMatrix[2][1] * (pointY - cameraY) + rotationMatrix[2][2] * (pointZ - cameraZ) ) );

		yy0 = -cc * ( ( rotationMatrix[1][0] * (pointX - cameraX) + rotationMatrix[1][1] * (pointY - cameraY) + rotationMatrix[1][2] * (pointZ - cameraZ) ) /
				( rotationMatrix[2][0] * (pointX - cameraX) + rotationMatrix[2][1] * (pointY - cameraY) + rotationMatrix[2][2] * (pointZ - cameraZ) ) );


		double x = xx0 + ppx;
		double y = yy0 + ppy;

		double xpx = (nc / 2.0) + (x / ps);
		double ypx = (nr / 2.0) - (y / ps);

		output[0] = xpx;
		output[1] = ypx;

		return output;

	}

	public static double[] collinearStuff3(double p_x, double p_y, double p_z, double[] io, double[] eo, double[][] rotationMatrix, double x_s, double y_s){

		double[] output = new double[2];

		double pointX = p_x;
		double pointY = p_y;
		double pointZ = p_z;

		double cameraX = eo[0];
		double cameraY = eo[1];
		double cameraZ = eo[2];

		double omega = eo[3];
		double phi = eo[4];
		double kappa = eo[5];

		double cc = io[0];
		double ps = io[1] / 1000.0; //12.0 / 1000000.0;

		double nc = x_s;
		double nr = y_s;

		double ppx = io[2];
		double ppy = io[3];

		//Mat rotation = new Mat(3,3, CvType.CV_64FC1);
/*
		rotationMatrix[0][0] = Math.cos(phi) * Math.cos(kappa);

		rotationMatrix[0][1] =  Math.cos(omega) * Math.sin(kappa) + Math.sin(omega) * Math.sin(phi) * Math.cos(kappa);

		rotationMatrix[0][2] = Math.sin(omega) * Math.sin(kappa) - Math.cos(omega) * Math.sin(phi) * Math.cos(kappa);

		rotationMatrix[1][0] = -Math.cos(phi) * Math.sin(kappa);

		rotationMatrix[1][1] = Math.cos(omega) * Math.cos(kappa) - Math.sin(omega) * Math.sin(phi) * Math.sin(kappa);

		rotationMatrix[1][2] = Math.sin(omega) * Math.cos(kappa) + Math.cos(omega) * Math.sin(phi) * Math.sin(kappa);

		rotationMatrix[2][0] = Math.sin(phi);

		rotationMatrix[2][1] = -Math.sin(omega) * Math.cos(phi);

		rotationMatrix[2][2] = Math.cos(omega) * Math.cos(phi);
*/
		double xx0;
		double yy0;

		xx0 = -cc * ( ( rotationMatrix[0][0] * (pointX - cameraX) + rotationMatrix[0][1] * (pointY - cameraY) + rotationMatrix[0][2] * (pointZ - cameraZ) ) /
				( rotationMatrix[2][0] * (pointX - cameraX) + rotationMatrix[2][1] * (pointY - cameraY) + rotationMatrix[2][2] * (pointZ - cameraZ) ) );

		yy0 = -cc * ( ( rotationMatrix[1][0] * (pointX - cameraX) + rotationMatrix[1][1] * (pointY - cameraY) + rotationMatrix[1][2] * (pointZ - cameraZ) ) /
				( rotationMatrix[2][0] * (pointX - cameraX) + rotationMatrix[2][1] * (pointY - cameraY) + rotationMatrix[2][2] * (pointZ - cameraZ) ) );


		double x = xx0 + ppx;
		double y = yy0 + ppy;

		double xpx = (nc / 2.0) + (x / ps);
		double ypx = (nr / 2.0) - (y / ps);

		output[0] = xpx;
		output[1] = ypx;

		return output;

	}


	public static double[][] makeRotationMatrix(double[] io, double[] eo){

		double[][] rotationMatrix = new double[3][3];

		double cameraX = eo[0];
		double cameraY = eo[1];
		double cameraZ = eo[2];

		double omega = eo[3];
		double phi = eo[4];
		double kappa = eo[5];

		double cc = io[0];
		double ps = io[1] / 1000.0; //12.0 / 1000000.0;

		double ppx = io[2];
		double ppy = io[3];

		//Mat rotation = new Mat(3,3, CvType.CV_64FC1);

		rotationMatrix[0][0] = Math.cos(phi) * Math.cos(kappa);

		rotationMatrix[0][1] =  Math.cos(omega) * Math.sin(kappa) + Math.sin(omega) * Math.sin(phi) * Math.cos(kappa);

		rotationMatrix[0][2] = Math.sin(omega) * Math.sin(kappa) - Math.cos(omega) * Math.sin(phi) * Math.cos(kappa);

		rotationMatrix[1][0] = -Math.cos(phi) * Math.sin(kappa);

		rotationMatrix[1][1] = Math.cos(omega) * Math.cos(kappa) - Math.sin(omega) * Math.sin(phi) * Math.sin(kappa);

		rotationMatrix[1][2] = Math.sin(omega) * Math.cos(kappa) + Math.cos(omega) * Math.sin(phi) * Math.sin(kappa);

		rotationMatrix[2][0] = Math.sin(phi);

		rotationMatrix[2][1] = -Math.sin(omega) * Math.cos(phi);

		rotationMatrix[2][2] = Math.cos(omega) * Math.cos(phi);

		return rotationMatrix;

	}

	public static class pointAI{

		int threadsDone;

		LasPoint point;

		ArrayList<Integer> imagesVisible = new ArrayList<Integer>();


		//ArrayList<ArrayList<Integer>> valuePerChannel = new ArrayList<ArrayList<Integer>>();
		//ArrayList<double[]> valuePerChannel = new ArrayList<double[]>();
		double[] channelMeans = new double[5];

		int iteration;

		boolean poison;

		int[] array1 = new int[1];

		//ArrayList<Integer> channelValues = new ArrayList<Integer>();

		//double[] channelValues = new double[];

		public pointAI(){

			poison = true;

		}

		public pointAI(int n_bands){

			poison = true;
			channelMeans = new double[n_bands];

		}


		public pointAI(LasPoint point1, int iteration2){

			//this.point = new LasPoint(point1);
			threadsDone = 0;
			this.iteration = iteration2;
			//dataStructure = new TreeMap<Integer, TreeMap<Integer, Integer>>();
			poison = false;
			//ogr.RegisterAll(); //Registering all the formats..
			//gdal.AllRegister();

		}

		public LasPoint getPoint(){

			return this.point;

		}

		public synchronized void addObservation(Dataset image, int imageId, double x, double y){

			imagesVisible.add(imageId);

			double channelSum = 0.0;

			for(int i = 1; i <= image.GetRasterCount(); i++){

				//System.out.println(i + " " + image.getRasterCount());
				Band temp = image.GetRasterBand(i);

				int a = temp.ReadRaster((int)x,
						(int)y,
						1,
						1,
						array1);


				//if(iteration % 10000 == 0)
					//temp.FlushCache();

				//channelValues.add(array1[0]);

				//channelValues[i] = array1[0];
				channelMeans[i - 1] += array1[0];
        		/*
				channelValues.add(1542451);

        		channelMeans[i - 1] += 154872;
        		*/

				//temp.FlushCache();
			}

			//array1;

			//valuePerChannel.add(channelValues);

			//if(iteration % 10000 == 0)
				//image.FlushCache();

			//this.threadsDone++;

		}

		public synchronized void threadDone(){

			threadsDone++;

		}

		public String toString() {
			return "";
			//return "Point - x: " + point.x + " y: " + point.y + " z: " + point.z + "\nImages visible: " + imagesVisible.size();

		}

		public void done(){

			for(int i = 0; i < channelMeans.length; i++)
				channelMeans[i] = channelMeans[i] / (double)imagesVisible.size();

			//this.point = null;
			//valuePerChannel = null;
			//imagesVisible = null;
			//for(int i = 0; i < channelMeans.length; i++)
			//	channelMeans[i] = 0;
			//imagesVisible.clear();
			//channelMeans = null;
			//System.out.println("size: " + imagesVisible.size());
		}

		public void prepare(){

			for(int i = 0; i < channelMeans.length; i++)
				channelMeans[i] = 0;

			imagesVisible.clear();
		}

	}

	/*
	public static class output{


		File outFile;

		BufferedWriter writer;

		public output(File in) throws IOException{

			outFile = in;

			writer = new BufferedWriter(new FileWriter(in), (int)Math.pow(2,18));

		}

		public synchronized void write(String in) throws IOException{

			writer.write(in);
			writer.newLine();

		}

		public void close() throws IOException{

			writer.close();

		}

	}
	*/

	public static double findMaxZ(ArrayList<String> pointClouds) throws IOException{

		double output = Double.POSITIVE_INFINITY;

		LasPoint tempPoint;

		for(int i = 0; i < pointClouds.size(); i++){

			File tempFile = new File(pointClouds.get(i));
			LASReader asd2 = new LASReader(tempFile);

			if(asd2.minZ < output)
				output = asd2.minZ;

			//System.out.println(output);

		}

		return output;

	}	


	static class multiImage implements Runnable{

		int threadNumber;
		int numberOfThreads;
		ArrayList<Dataset> datasets;

		ArrayList<Dataset> datasets_subset;


		double[] interior;
		ArrayList<double[]> exteriors;
		ArrayList<double[]> extents;
		ArrayList<Integer> imageIDs;
		int pienin;
		int suurin;

		BlockingQueue<pointAI> queue;

		boolean stop = true;

		boolean done = false;

		boolean out = false;

		boolean finished;

		ArrayList<Integer> shuffle;
		ArrayList<Integer> threadIn;

		double[][] rotationMatrix = new double[3][3];


		public multiImage (int threadNumber1, int numberOfThreads2, ArrayList<Dataset> datasets2,
						   double[] interior2, ArrayList<double[]> exteriors2, ArrayList<double[]> extents2,
						   ArrayList<Integer> imageIDs2, BlockingQueue<pointAI> queue1, ArrayList<Integer> shuffle2,
						   ArrayList<Integer> threadIn2){

			threadNumber = threadNumber1;
			numberOfThreads = numberOfThreads2;
			datasets = datasets2;
			exteriors = exteriors2;
			interior = interior2;

			extents = extents2;
			imageIDs = imageIDs2;
			queue = queue1;

			shuffle = shuffle2;
			threadIn = threadIn2;
		}


		@Override
		public void run() {
    		/*
        	this.suurin = -1;
        	this.pienin = -1;

            int jako = (int)Math.ceil((double)datasets.size() / (double) numberOfThreads);

            if(threadNumber != numberOfThreads){

              pienin = (threadNumber - 1) * jako;
              suurin = threadNumber * jako;  
            }
          
          	else{
	            pienin = (threadNumber - 1) * jako; 
	            suurin = (int)datasets.size();
          	}
   			*/
			try {
				while(true){

					pointAI asd = queue.take();

					if(asd.poison)
						break;
					//System.out.println(asd);
					this.ask(asd);
					asd.threadDone();
					asd = null;

				}
			} catch (InterruptedException e) {
				System.out.println("ERROR!!!!!!!!!!!!!!");
			}
			//datasets_subset = new ArrayList<Dataset>(datasets.subList(pienin, suurin));

		}

		public void beforeLoop(){

			this.finished = false;
			this.out = false;

		}

		public void afterLoop(){

			this.out = true;

		}

		public void stop(){

			stop = false;

		}

		public boolean finished(){

			return finished;

		}



		public void ask(pointAI tempP){

			this.done = false;

			for(int j = 0; j < threadIn.size(); j++){
				//shuffle.get(j);

				if(tempP.getPoint().x >= extents.get(threadIn.get(j))[0] && tempP.getPoint().x <= extents.get(threadIn.get(j))[1] &&
						tempP.getPoint().y >= extents.get(threadIn.get(j))[2] && tempP.getPoint().y <= extents.get(threadIn.get(j))[3]){

					double[] temp = collinearStuff(tempP.getPoint(), interior, exteriors.get(threadIn.get(j)), rotationMatrix, 0, 0);

					//
					if(temp[0] > 0 && temp[0] < datasets.get(threadIn.get(j)).GetRasterXSize()
							&& temp[1] > 0 && temp[1] < datasets.get(threadIn.get(j)).GetRasterYSize()){

						//System.out.println(temp[0] + " " + temp[1]);
						tempP.addObservation(datasets.get(threadIn.get(j)), imageIDs.get(threadIn.get(j)), temp[0], temp[1]);
						//outWrite += " " + imageIDs.get(j);
					}
				}
			}

			this.done = true;

		}

		public boolean getDone(){

			return this.done;

		}
	}

	public static double roundAvoid(double value, int places) {
		double scale = Math.pow(10, places);
		return Math.round(value * scale) / scale;
	}

	public static String timeRemaining(int n, int i, long elapsed){


		double perUnit = (double)elapsed / (double)i;

		String remaining = Double.toString( roundAvoid(perUnit * ((double)n - (double)i) / 1000000000.0,2)) + " s ";



		return remaining;


	}

	public static boolean notClose(double x1, double y1, ArrayList<double[]> reference, double threshold){

		double deltaX;
		double deltaY;

		for(int i = 0; i < reference.size(); i++){

			deltaX = y1 - reference.get(i)[1];
			deltaY = x1 - reference.get(i)[0];

			if(Math.sqrt(deltaX * deltaX + deltaY*deltaY) < threshold)
				return false;

		}

		return true;

	}

	public static void main(String[] args) throws IOException {




		argumentReader aR = new argumentReader(args);
		aR.setExecDir( System.getProperty("user.dir"));
		aR.parseArguents();

		double[][] rotationMatrix = new double[3][3];

		String pathSep = System.getProperty("file.separator");

		int nCores = 1;

		boolean lasFormat = false;
		boolean txtFormat = false;
		boolean wildCard = false;

		ArrayList<String> filesList = new ArrayList<String>();
		String input = "";
		String iparse = "xyz";
		String oparse = iparse;
		String odir = "";

		int warmUp = 0;

		File exteriorFile = null;
		File interiorFile = null;

		ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
		ArrayList<File> inputFiles = new ArrayList<>();

		//if(args.length > 0){
		input = aR.files[0];
			//input = args[0];
		System.out.println(input);

		//lasFormat = input.split(pathSep)[(input.split(pathSep)).length - 1].split("\\.")[1].equals("las");
		//txtFormat = input.split(pathSep)[(input.split(pathSep)).length - 1].split("\\.")[1].equals("txt");

		lasFormat = new File(aR.files[0]).getName().split("\\.")[1].equals("las");
		//lasFormat = aR.files[0].split("\\.")[1].equals("las");
		txtFormat = new File(aR.files[0]).getName().split("\\.")[1].equals("txt");

			//wildCard = input.split(pathSep)[(input.split(pathSep)).length - 1].split("\\.")[0].equals("*");
			wildCard = new File(aR.files[0]).getName().split("\\.")[1].equals("*");

		//}

		gdal.AllRegister();

		System.out.println("max cache: " + gdal.GetCacheMax());
		gdal.SetCacheMax(413375897 * 2);
		System.out.println("max cache: " + gdal.GetCacheMax());


		nCores = aR.cores;
		iparse = aR.iparse;
		oparse = aR.oparse;
		odir = aR.odir;

		exteriorFile = new File(aR.exterior);
		interiorFile = new File(aR.interior);


		warmUp = 0;

		if(lasFormat){

			if(wildCard){
				//tiedostot_indeksi = homma.listFiles(pathi, ".lasx");
				filesList = MKid4pointsLAS.listFiles(input.split("\\*")[0], ".las");
			}

			else{

				filesList.add(input);
				//System.out.println(tiedostot_sorted);
			}

			for(int i = 0; i < filesList.size(); i++){
				//System.out.println(filesList.get(i));
				//pointClouds.add(new LASReader(new File(filesList.get(i))));
				inputFiles.add(new File(filesList.get(i)));

			}
		}

		if(txtFormat && !aR.debug){
			proge.setName("Converting .txt to .las ...");
			ArrayList<String> tempList = new ArrayList<String>();

			if(wildCard){
				//tiedostot_indeksi = homma.listFiles(pathi, ".lasx");
				tempList = MKid4pointsLAS.listFiles(input.split("\\*")[0], ".txt");
			}

			else{

				tempList.add(input);
				//System.out.println(tiedostot_sorted);
			}
			//proge.setName("Converting .txt to .las ...");
			proge.setEnd(tempList.size());

			if(nCores > tempList.size())
				nCores = tempList.size();



			ArrayList<Thread> lista11 = new ArrayList<Thread>();
			for(int ii = 1; ii <= nCores; ii++){

				proge.addThread();
				Thread temp = new Thread(new multiTXT2LAS(tempList, iparse, nCores, ii, odir));
				lista11.add(temp);
				temp.start();


			}

			for(int i = 0; i < lista11.size(); i++){

				try{

					lista11.get(i).join();
				}catch(Exception e){}
			}

			filesList = tiedostoLista.files;

			for(int i = 0; i < filesList.size(); i++){
				//System.out.println(filesList.get(i));
				pointClouds.add(new LASReader(new File(filesList.get(i))));

			}
		}

		//File asd = new File("196.tif");
		nCores = 1;
		ogr.RegisterAll(); //Registering all the formats..
		gdal.AllRegister();
		gdal.UseExceptions();

		ArrayList<ArrayList<Dataset>> threadImages = new ArrayList<ArrayList<Dataset>>();

		ArrayList<double[]> exteriors = new ArrayList<double[]>();
		double[] interior2 = new double[4];


		interior2 = readIo(interiorFile, "\t");

		final double[] interior = interior2;

		ArrayList<Integer> imageIDs = new ArrayList<Integer>();

		ArrayList<String> tempList = filesList;// runners.MKid4pointsLAS.listFiles("/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/ai2las_test/noDupe/forTest/", ".las");

		double minZ = findMaxZ(tempList);


		ArrayList<pointAI> output = new ArrayList<pointAI>();

		ArrayList<double[]> extents = new ArrayList<double[]>();
		ArrayList<Dataset> datasets = new ArrayList<Dataset>();
		ArrayList<String> imageNames = new ArrayList<>();

		readImages(exteriorFile, datasets, threadImages, imageIDs, exteriors, "\t", interior, extents, minZ, rotationMatrix, imageNames, aR);

		int n_bands = datasets.get(0).GetRasterCount();

		ArrayList<Integer> shuffle = new ArrayList<Integer>();

		ArrayList<ArrayList<Integer>> threadIn = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> coreNumbers = new ArrayList<Integer>();

		for(int i = 0; i < nCores; i++){

			coreNumbers.add(i);
			ArrayList<Integer> temp = new ArrayList<Integer>();
			threadIn.add(temp);

		}

		int count1 = 0;

		for(int i = 0; i < datasets.size(); i++){

			count1++;

			if(count1 == nCores)
				count1 = 0;

			threadIn.get(count1).add(i);

			//shuffle.add(i);

		}





		if(odir.equals("asd"))
			odir = "";


		pointAI tempP = new pointAI(n_bands);

		//LinkedList<ArrayList<int[]>> cachedImages = new LinkedList<>();

		HashMap<Integer, Dataset> cachedImages = new HashMap<>();

		LinkedList<Integer> cachedImg = new LinkedList<>();

		double x_s = (double)datasets.get(0).getRasterXSize();
		double y_s = (double)datasets.get(0).getRasterYSize();

		int[] cach = new int[datasets.size()];

		ArrayList<double[][]> rotationMatrices = new ArrayList<>();

		int pix_threshold_x = (int) ((double)datasets.get(0).getRasterXSize() * aR.edges);
		int pix_threshold_y = (int) ((double)datasets.get(0).getRasterYSize() * aR.edges);

		for(int i = 0; i < datasets.size(); i++){

			//datasets.get(i).delete();
			rotationMatrices.add(makeRotationMatrix(interior, exteriors.get(i)));

		}


		if(!aR.debug) {
			for (int t = 0; t < tempList.size(); t++) {

				int outsidePoint = 0;

				File tempFile = new File(tempList.get(t));
				//proge.reset();
				//proge.setName(t + " | " + tempList.size());


				//File ofile = new File("testi.txt");
				String oname = odir + (tempFile.getName().split(".las")[0] + ".txt");

				File ofile2 = aR.createOutputFileWithExtension(tempFile, "_ai.txt");

				//File ofile2 = new File(oname);


				//if(ofile.exists())
				//ofile.delete();

				//ofile.createNewFile();

				if (ofile2.exists())
					ofile2.delete();

				ofile2.createNewFile();

				//output outti = new output(ofile);

				LasPoint tempPoint = new LasPoint();
				//int[] array1 = new int[1];

				LASReader asd2 = new LASReader(tempFile);

				File outputFile = null;
				pointWriterMultiThread pw = null;
				LasPointBufferCreator buf = null;

				if (aR.olas) {

					outputFile = aR.createOutputFile(asd2);
					pw = new pointWriterMultiThread(outputFile, asd2, "las2las", aR);

					buf = new LasPointBufferCreator(asd2.pointDataRecordLength, 1, pw);


				}

				tempFile = null;

				long n = asd2.getNumberOfPointRecords();

				int count = 0;

				long lStartTime = 0;
				long lStartTime_debug = 0;
				long lEndTime = 0;
				long lEndTime_debug1 = 0;
				long lEndTime_debug2 = 0;
				long lEndTime_debug3 = 0;
				long lEndTime_debug4 = 0;
				//proge.setEnd((int)n);


				ArrayList<double[]> valmiit = new ArrayList<double[]>();

				double[] thisLocation = new double[2];

				double[] temp;

				try {

					FileWriter fw = new FileWriter(ofile2);
					BufferedWriter bw2 = new BufferedWriter(fw, (int) Math.pow(2, 10));

					lStartTime = System.nanoTime();
					boolean pointFound = false;

					for (int p = 0; p < asd2.getNumberOfPointRecords(); p += 10000) {
						//for(int i = 0; i < n; i++){

						int maxi = (int) Math.min(10000, Math.abs(asd2.getNumberOfPointRecords() - (p)));

						try {
							asd2.readRecord_noRAF(p, tempPoint, maxi);
						} catch (Exception e) {
							e.printStackTrace();
						}

						for (int j = 0; j < maxi; j++) {

							//if((j+p) > 1600000)

							//System.out.println((j) + " " + maxi + " " + pointCloud.getNumberOfPointRecords());
							asd2.readFromBuffer(tempPoint);
							//count++;
							pointFound = false;

							if (j + p % 10000 == 0) {
								//System.gc();
								lEndTime = System.nanoTime();
								//System.out.print("\33[1A\33[2K");
								System.out.print("\033[2K"); // Erase line content
								System.out.print((j + p) + "|" + n + " Time remaining: " + timeRemaining((int) n, j + p, (lEndTime - lStartTime)) + " ms/point: " + roundAvoid(((double) lEndTime - (double) lStartTime) / 1000000.0 / (double) (j + p), 2) + " o: " + outsidePoint + " " + gdal.GetCacheUsed() + "\r");  //+ asd2.getFile().getName() + "\r");

							}

							tempP.prepare();
							//tempP = new pointAI(tempPoint, j+p);

							String outWrite = "";
							String outWrite2 = "";

							int numberOfimages = 0;

							int visited = 0;

							int recent = -1;
							//lStartTime_debug = System.currentTimeMillis();
							double p_x = tempPoint.x;
							double p_y = tempPoint.y;
							double p_z = tempPoint.z;
							/*
							AtomicDoubleArray channels = new AtomicDoubleArray(datasets.get(0).GetRasterCount());
							AtomicInteger n_img = new AtomicInteger(0);
							if(aR.cores > 1){

								IntStream s = IntStream.range(0, datasets.size());

								s.parallel().forEach(j_ -> {
									int[] array1 = new int[1];
									//System.out.println(j_);
									//thisLocation[0] = exteriors.get(j_)[2];
									//thisLocation[1] = exteriors.get(j_)[3];


									//System.out.println(notClose(thisLocation[0], thisLocation[1], valmiit, 15.0) + " " + valmiit.size());

									if (p_x >= extents.get(j_)[0] && p_x <= extents.get(j_)[1] && p_y >= extents.get(j_)[2] && p_y <= extents.get(j_)[3]) {
										//if(true){
										//visited++;

										//temp = collinearStuff(tempPoint, interior, exteriors.get(j_), rotationMatrix, x_s, y_s);
										double[] temp2 = collinearStuff3(p_x, p_y, p_z, interior, exteriors.get(j_), rotationMatrices.get(j_), x_s, y_s);

										if (temp2[0] > pix_threshold_x && temp2[0] < (x_s - pix_threshold_x)
												&& temp2[1] > pix_threshold_y && temp2[1] < (y_s - pix_threshold_y)) {

											System.out.println("HERE");
											//channels[0]++;
											for(int i = 1; i <= datasets.get(j_).GetRasterCount(); i++){

												//System.out.println(i + " " + image.getRasterCount());
												Band temp_b = datasets.get(j_).GetRasterBand(i);

												int a = temp_b.ReadRaster((int)temp2[0],
														(int)temp2[1],
														1,
														1,
														array1);

												channels.getAndAdd(i - 1, array1[0]);

											}
											n_img.incrementAndGet();
											//.addObservation(datasets.get(j_), imageIDs.get(j_), temp2[0], temp2[1]);

											//pointFound = true;
											//valmiit.add(new double[]{thisLocation[0], thisLocation[1]});
											//numberOfimages++;
											//recent = j_;
											//outWrite += " " + imageIDs.get(j);

										}
									}

								});

							}

							else
								*/
							for (int j_ = 0; j_ < datasets.size(); j_++) {

								//thisLocation[0] = exteriors.get(j_)[2];
								//thisLocation[1] = exteriors.get(j_)[3];


								//System.out.println(notClose(thisLocation[0], thisLocation[1], valmiit, 15.0) + " " + valmiit.size());

								if (tempPoint.x >= extents.get(j_)[0] && tempPoint.x <= extents.get(j_)[1] && tempPoint.y >= extents.get(j_)[2] && tempPoint.y <= extents.get(j_)[3]) {
								//if(true){
									visited++;

									//temp = collinearStuff(tempPoint, interior, exteriors.get(j_), rotationMatrix, x_s, y_s);
									temp = collinearStuff2(tempPoint, interior, exteriors.get(j_), rotationMatrices.get(j_), x_s, y_s);

									if (temp[0] > pix_threshold_x && temp[0] < (x_s - pix_threshold_x)
											&& temp[1] > pix_threshold_y && temp[1] < (y_s - pix_threshold_y)) {

										tempP.addObservation(datasets.get(j_), imageIDs.get(j_), temp[0], temp[1]);

										pointFound = true;
										//valmiit.add(new double[]{thisLocation[0], thisLocation[1]});
										numberOfimages++;
										recent = j_;
										//outWrite += " " + imageIDs.get(j);

									}
								}
							}

							//lEndTime_debug1= System.currentTimeMillis();

							//System.out.println("debug_time1: " + (lEndTime_debug1-lStartTime_debug));
							//System.out.println(numberOfimages + "|" + datasets.size());
							//System.out.println("");

							valmiit.clear();
							//for(int ii = 0; ii < 4; ii++)
							//	System.out.println(tempP.valuePerChannel.get(0).get(ii));

							//for(int j = 0; j < tempP.imagesVisible.size(); j++)
							//outWrite += " " + tempP.imagesVisible.get(j);

							//outWrite += ";";

							if (pointFound == false) {
								//System.out.println("POINT HAS NO RGB VALUE!!");
								//System.exit(0);
								/*
								System.out.println("no value found for: " + tempPoint.x + " " + tempPoint.y + " " + tempPoint.z + " " + visited);

								for (int j_ = 0; j_ < datasets.size(); j_++) {

									thisLocation[0] = exteriors.get(j_)[2];
									thisLocation[1] = exteriors.get(j_)[3];

									//System.out.println(notClose(thisLocation[0], thisLocation[1], valmiit, 15.0) + " " + valmiit.size());

									//if (tempPoint.x >= extents.get(j_)[0] && tempPoint.x <= extents.get(j_)[1] && tempPoint.y >= extents.get(j_)[2] && tempPoint.y <= extents.get(j_)[3]) {
										if(true){
										visited++;

										//temp = collinearStuff(tempPoint, interior, exteriors.get(j_), rotationMatrix, x_s, y_s);
										temp = collinearStuff2(tempPoint, interior, exteriors.get(j_), rotationMatrices.get(j_), x_s, y_s);

										if(j_ >= 1936)
											System.out.println(Arrays.toString(temp) + " " + pix_threshold_x + " " + x_s + " " + pix_threshold_y + " " + y_s + " " + datasets.get(j_).GetFileList().get(0));


											if (temp[0] > pix_threshold_x && temp[0] < (x_s - pix_threshold_x)
												&& temp[1] > pix_threshold_y && temp[1] < (y_s - pix_threshold_y)) {

											tempP.addObservation(datasets.get(j_), imageIDs.get(j_), temp[0], temp[1]);

											pointFound = true;
											//valmiit.add(new double[]{thisLocation[0], thisLocation[1]});
											numberOfimages++;
											recent = j_;
											//outWrite += " " + imageIDs.get(j);

										}
									}
								}
*/
								outsidePoint++;
								tempP.done();
								continue;
							}

							tempP.done();

							if (aR.olas) {

								//System.out.println("seq: " + Arrays.toString(aR.sequence));
								/*
								System.out.println(tempPoint.R);
								System.out.println(tempPoint.G);
								System.out.println(tempPoint.B);

								 */
								if (aR.sequence.length < 1) {


								} else if (aR.sequence.length < 2) {
									tempPoint.R = (int) tempP.channelMeans[aR.sequence[0]];
								} else if (aR.sequence.length < 3) {
									tempPoint.R = (int) tempP.channelMeans[aR.sequence[0]];
									tempPoint.G = (int) tempP.channelMeans[aR.sequence[1]];
								} else if (aR.sequence.length < 4) {
									tempPoint.R = (int) tempP.channelMeans[aR.sequence[0]];
									tempPoint.G = (int) tempP.channelMeans[aR.sequence[1]];
									tempPoint.B = (int) tempP.channelMeans[aR.sequence[2]];
								}


/*
								System.out.println("++++++++++++++++++++");
								System.out.println(tempPoint.R);
								System.out.println(tempPoint.G);
								System.out.println(tempPoint.B);
								*/
								buf.writePoint(tempPoint, aR.inclusionRule, (j + p));

							}

							if (!aR.olas) {

								outWrite2 += LASwrite.LASpoint2String(tempPoint, oparse); //tempP.point.x + " " + tempP.point.y + " " + tempP.point.z;

								for (int l = 0; l < n_bands; l++) {


									//double channelMean = 0.0;
						/*
						for(int j = 0; j < tempP.imagesVisible.size(); j++){

							outWrite += " " + tempP.valuePerChannel.get(j).get(l);
							channelMean += tempP.valuePerChannel.get(j).get(l);

						}
						*/
									//channelMean /= tempP.imagesVisible.size();
									outWrite2 += "\t" + tempP.channelMeans[l];

									//System.out.println("opas: " + outWrite2);
									//outWrite += ";";

								}

								//outti.write(outWrite2);

								//tempP = null;

								bw2.write(outWrite2);
								bw2.newLine();
							}
							outWrite2 = null;

							//bw.write(outWrite);
							//bw.newLine();

							//output.add(tempP);
							//tempPoint = null;
						}
					}

					//bw.close();
					bw2.close();

					//outti.close();
					ofile2 = null;

					tempPoint = null;
					oname = null;
					fw.close();

				} catch (Exception e) {

					System.out.println(count + "/" + n);

					e.printStackTrace(System.out);
				}

				lEndTime = System.nanoTime();

				long time = lEndTime - lStartTime;


				System.out.print(n + "|" + n + " Filename: " + asd2.getFile().getName() + "\r");
				asd2.close();
				asd2 = null;
				System.out.println("\nms/point: " + roundAvoid((double) time / 1000000.0 / (double) n, 2));
				//n = null;
				tempFile = null;

				if(aR.olas){
					buf.close();
					pw.close();
				}



			}
		}else{

			/** DEBUG CODE */

			System.out.println(aR.debug_file);
			System.out.println("DEBUGG!!!!!");

			BufferedReader sourceReader = new BufferedReader(new FileReader(aR.debug_file));
			File o_file = aR.createOutputFileWithExtension(aR.debug_file, "_debug.txt");

			if(o_file.exists()){
				o_file.delete();
				o_file.createNewFile();
			}else{
				o_file.createNewFile();
			}
			BufferedWriter outWriter = new BufferedWriter(new FileWriter(o_file));
			String sourceLine = null;
			int counter = 1;
			while ((sourceLine = sourceReader.readLine()) != null) {

				String[] split = sourceLine.split("\t");

				double p_x = Double.parseDouble(split[0]);
				double p_y = Double.parseDouble(split[1]);
				double p_z = Double.parseDouble(split[2]);

				System.out.println("p_x: " + p_x + " p_y: " + p_y + " p_z: " + p_z);

				tempP.prepare();

				double[] thisLocation = new double[2];

				double[] temp;


				for (int j_ = 0; j_ < datasets.size(); j_++) {

					thisLocation[0] = exteriors.get(j_)[2];
					thisLocation[1] = exteriors.get(j_)[3];

					if (p_x >= extents.get(j_)[0] && p_x <= extents.get(j_)[1] && p_y >= extents.get(j_)[2] && p_y <= extents.get(j_)[3]) {
						//if(true){
						//visited++;

						temp = collinearStuff3(p_x, p_y, p_z, interior, exteriors.get(j_), rotationMatrices.get(j_), x_s, y_s);

						if (temp[0] > pix_threshold_x && temp[0] < (x_s - pix_threshold_x)
								&& temp[1] > pix_threshold_y && temp[1] < (y_s - pix_threshold_y)) {



							Vector nam = datasets.get(j_).GetFileList();
							System.out.println("p found in: " + new File(nam.get(0).toString()).getName() + " " + Arrays.toString(temp) + " " + imageIDs.get(j_));
							tempP.addObservation(datasets.get(j_), imageIDs.get(j_), temp[0], temp[1]);

							outWriter.write(p_x + "\t" + p_y + "\t" + p_z + "\t" +
									new File(nam.get(0).toString()).getName() + "\t" +
									imageIDs.get(j_) + "\t" +
									temp[0] + "\t" +
									-temp[1] + "\t" +
									counter + "\t");
							outWriter.newLine();
						}
					}
				}
				counter++;
			}

			sourceReader.close();
			outWriter.close();
		}
		pointAI POISON_PILL = new pointAI();
		/*
		for(int u = 0;  u < lista11.size(); u++){

          	lista12.get(u).add(POISON_PILL);

          }
          */
		//System.out.println("Band1: " + array1[0]);

		
		/*
		try{
			FileOutputStream fos = new FileOutputStream(ofile);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

			for(int i = 0; i < output.size(); i++){

				String outWrite = "";

				pointAI tempP = output.get(i);

				outWrite += tempP.point.x + " " + tempP.point.y + " " + tempP.point.z;
				outWrite += ";";

				for(int j = 0; j < tempP.imagesVisible.size(); j++)
					outWrite += " " + tempP.imagesVisible.get(j);

				outWrite += ";";

				for(int l = 0; l < 4; l++){

					for(int j = 0; j < tempP.imagesVisible.size(); j++){

						outWrite += " " + tempP.valuePerChannel.get(j).get(l);

					}

					outWrite += ";";

				}
				

				bw.write(outWrite);
				bw.newLine();

			}

			bw.close();

		}catch(Exception e){System.out.println(e);}
		*/
	}

	public static void addImageToCache(LinkedList<ArrayList<int[]>> cache, Dataset image) {

		cache.add(new ArrayList<>(image.GetLayerCount()));

		for (int i = 1; i <= image.GetRasterCount(); i++) {

			int[] array = new int[image.getRasterXSize() * image.getRasterYSize()];
			//System.out.println(i + " " + image.getRasterCount());
			Band temp = image.GetRasterBand(i);

			int a = temp.ReadRaster((int) 0,
					(int) 0,
					image.GetRasterXSize(),
					image.GetRasterYSize(),
					array);

			cache.getLast().set(i-1, array);

		}
	}

}