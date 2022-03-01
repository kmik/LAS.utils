package runners;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import err.toolException;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.gdal.Dataset;

import org.gdal.gdal.Band;

import LASio.*;

import tools.createCHM;
import utils.*;

import static org.gdal.gdalconst.gdalconstConstants.*;

class ai2las{
	public static listOfFiles tiedostoLista = new listOfFiles();
	public static ThreadProgressBar proge = new ThreadProgressBar();


	public static fileOperations fo = new fileOperations();

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
		if(proge >= 0.97*paatos && proge <= paatos)System.out.print(nimi + "   |####################|  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");


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

		argumentReader aR;

		public multiTXT2LAS (ArrayList<String> tiedostot2, String parse2, int numberOfCores2, int coreNumber2, String odir2, argumentReader aR){

			this.aR = aR;

			tiedostot = tiedostot2;
			parse = parse2;
			numberOfCores = numberOfCores2;
			coreNumber = coreNumber2;
			odir = odir2;
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

				}
				else{

				}

				ArrayList<File> from = new ArrayList<File>();
				ArrayList<LASraf> to = new ArrayList<LASraf>();
				ArrayList<String> outList = new ArrayList<String>();

				for(int i = 0; i < tiedostot.size(); i++){

					File tempFile = new File(tiedostot.get(i));

					File toFile = null;
/*
					if(odir.equals("asd"))

						toFile = fo.createNewFileWithNewExtension(tempFile, ".las");

					if(!odir.equals("asd"))

						toFile = fo.createNewFileWithNewExtension(tempFile, odir, ".las");
*/
					toFile = aR.createOutputFile(tempFile);
					File fromFile = new File(tiedostot.get(i));

					if(toFile.exists())
						toFile.delete();


					toFile.createNewFile();

					from.add(fromFile);

					to.add(new LASraf(toFile));
					outList.add(fo.createNewFileWithNewExtension(tiedostot.get(i), ".las").getAbsolutePath());

				}

				PointInclusionRule rule = new PointInclusionRule();
				tiedostoLista.add(outList);

				for(int i = 0; i < tiedostot.size(); i++){

					LASwrite.txt2las(from.get(i), to.get(i), parse, "txt2las", " ", rule, false, aR);
					to.get(i).writeBuffer2();
					to.get(i).close();
					proge.updateCurrent(1);
					if(i % 10 == 0)
						proge.print();
				}
			} catch (Exception e) {

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
					double test = tempDataset.GetRasterXSize();
				}catch (Exception e){
					System.out.println("WARNING!! " + tokens[0] + " DOES NOT EXIST!!");
					continue;
				}
				System.out.print(count++ + " Images read\r");
				double sensorSize = interior[1] * (double)tempDataset.GetRasterXSize();
				double focalLength_millimeters = interior[0];

				images.add(tempDataset);
				imageNames.add("/" + tokens[0]);
				double[] eoTemp = new double[6];

				eoTemp[0] = Double.parseDouble(tokens[2]);
				eoTemp[1] = Double.parseDouble(tokens[3]);
				eoTemp[2] = Double.parseDouble(tokens[4]);

				if(!aR.input_in_radians) {
					eoTemp[3] = Math.toRadians(Double.parseDouble(tokens[5]));
					eoTemp[4] = Math.toRadians(Double.parseDouble(tokens[6]));
					eoTemp[5] = Math.toRadians(Double.parseDouble(tokens[7]));
				}
				else{

				}

				double flyingHeight = eoTemp[2] - minZ;

				if(aR.altitude != 0.0)
					flyingHeight = aR.altitude;

				double gsd = ((sensorSize / 1000.0 * flyingHeight) / (focalLength_millimeters / 1000.0 * (double)tempDataset.GetRasterXSize())) * 100.0;
				double gsd_m = gsd / 100.0;

				System.out.println("img altitude: " + flyingHeight + " gsd: " + gsd + " cm");
				System.out.println(sensorSize + " " + focalLength_millimeters + " " + (double)tempDataset.GetRasterXSize());

				tempPoint.x = eoTemp[0];
				tempPoint.y = eoTemp[1];
				tempPoint.z = minZ; //DOES NOT

				eos.add(eoTemp);
				imageID.add(Integer.parseInt(tokens[1]));

				double[] extentsThis = new double[4];
				double x_s = tempDataset.getRasterXSize();
				double y_s = tempDataset.getRasterYSize();
				double[] temp = collinearStuff(tempPoint, interior, eoTemp, rotationMatrix, x_s, y_s);

				Math.max((double)tempDataset.GetRasterXSize() / 2.0, temp[0]);


				extentsThis[0] = eoTemp[0] - ((double) tempDataset.GetRasterXSize() * gsd_m);
				extentsThis[1] = eoTemp[0] + ((double) tempDataset.GetRasterXSize() * gsd_m);

				extentsThis[2] = eoTemp[1] - ((double) tempDataset.GetRasterYSize() * gsd_m);
				extentsThis[3] = eoTemp[1] + ((double) tempDataset.GetRasterYSize() * gsd_m);

				extents.add(extentsThis.clone());

			}

		}
		catch (Exception e){
			e.printStackTrace(System.out); // handle exception
		}

		System.out.println();

	}


	public static double[] readIo(File ioInformation, String sep){

		double[] output = new double[6];

		File file = ioInformation;

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {

			String line;

			while ((line = br.readLine()) != null) {

				String[] tokens = line.split(sep);

				output[0] = Double.parseDouble(tokens[0]); //Focal length

				if(output[0] < 1.0){
					throw new toolException("Focal length less than a millimeter?");
				}

				output[1] = Double.parseDouble(tokens[1]); // pixel size
				output[2] = Double.parseDouble(tokens[2]);	//ppx
				output[3] = Double.parseDouble(tokens[3]);  //ppy

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

		/** CHANGED THIS ONE */
		double ps = io[1]; //12.0 / 1000000.0;

		double nc = x_s;
		double nr = y_s;

		double ppx = io[2];
		double ppy = io[3];

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

		double cc = io[0];

		double ps = io[1]; //12.0 / 1000000.0;

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

	public static double[] collinearStuff_array(double[] point, double[] io, double[] eo, double[][] rotationMatrix, double x_s, double y_s){

		double[] output = new double[2];

		double pointX = point[0];
		double pointY = point[1];
		double pointZ = point[2];

		double cameraX = eo[0];
		double cameraY = eo[1];
		double cameraZ = eo[2];

		double cc = io[0];

		double ps = io[1]; //12.0 / 1000000.0;

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


	public static double[][] makeRotationMatrix(double[] io, double[] eo){

		double[][] rotationMatrix = new double[3][3];

		double omega = eo[3];
		double phi = eo[4];
		double kappa = eo[5];

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

		double[] channelMeans = new double[5];

		int iteration;

		boolean poison;

		int[] array1 = new int[1];

		//ArrayList<Integer> channelValues = new ArrayList<Integer>();

		//double[] channelValues = new double[];
		int n_bands = 0;
		int[] band_list;
		ByteBuffer bb;

		int data_type;
		byte n_bytes;

		public pointAI(){

			poison = true;

		}

		public pointAI(int n_bands, int n_bytes, int data_type){

			poison = true;
			channelMeans = new double[n_bands];
			this.n_bands = n_bands;
			band_list = new int[this.n_bands];

			for(int i = 1; i <= n_bands; i++)
				band_list[i-1] = i;

			this.n_bytes = (byte)n_bytes;

			this.data_type = data_type;

		}


		public pointAI(LasPoint point1, int iteration2){

			threadsDone = 0;
			this.iteration = iteration2;

			poison = false;


		}

		public LasPoint getPoint(){

			return this.point;

		}

		public synchronized void addObservation(Dataset image, int imageId, double x, double y){

			imagesVisible.add(imageId);

			imagesVisible.add(imageId);

			bb = ByteBuffer.allocateDirect( n_bytes * n_bands );
			bb.order(ByteOrder.nativeOrder());

			//System.out.println(Arrays.toString(band_list));
			//System.exit(1);
			image.ReadRaster_Direct((int)x, (int)y, 1, 1, 1, 1, data_type, bb, band_list);
/*
			System.out.println("this: " + temp1.GetRasterDataType());
			System.out.println("byte: " + GDT_Byte);
			System.out.println("int_16: " + GDT_Int16);
			System.out.println("uint_16: " + GDT_UInt16);
			System.out.println("float_32: " + GDT_Float32);
			System.out.println("float_64: " + GDT_Float64);

			System.exit(1);

*/
			int[] val1 = new int[n_bands];

			for(int i = 0; i < image.GetRasterCount(); i++){

				int value = 0;
				//System.out.println(temp1.GetRasterDataType());

				switch(data_type){

					case 1:
						value = bb.get();
						break;

					case 2:
						//System.out.println("READING: " + Arrays.toString(band_list));
						value = getUnsignedShort(bb);
						//System.out.println(value);
						break;

					case 3:
						value = bb.getShort();
						break;

					case 6:
						value = (int)bb.getFloat();
						break;

					case 7:
						value = (int)bb.getDouble();
						break;

					default:
						break;

				}


				//int value = getUnsignedShort(bb);
				channelMeans[i] += value;
				//val1[i] = value;

			}

			//int[] val2 = new int[n_bands];

			for(int i = 1; i <= image.GetRasterCount(); i++){

				Band temp = image.GetRasterBand(i);

				int a = temp.ReadRaster((int)x,
						(int)y,
						1,
						1,
						array1);

				channelMeans[i - 1] += array1[0];
				//val2[i-1] = array1[0];

			}

			//System.out.println(Arrays.toString(val1));
			//System.out.println(Arrays.toString(val2));
			//System.out.println("-------------");

		}

		public synchronized float[] addObservation_return_array(Dataset image, int imageId, double x, double y){

			float[] output = new float[image.GetRasterCount()];
			Band temp1 = image.GetRasterBand(1);
			imagesVisible.add(imageId);
			ByteBuffer bb = ByteBuffer.allocateDirect(2*5);
			bb.order(ByteOrder.nativeOrder());


			int[] band_list = new int[]{1,2,3,4,5};
			image.ReadRaster_Direct((int)x, (int)y, 1, 1, 1, 1, 2, bb, band_list);


			for(int i = 0; i < image.GetRasterCount(); i++){

				int value = getUnsignedShort(bb);
				channelMeans[i] += value;
				output[i] = value;
			}


			if(false)
			for(int i = 1; i <= image.GetRasterCount(); i++){

				Band temp = image.GetRasterBand(i);

/*
				ByteBuffer bb = temp.ReadRaster_Direct((int)x,
						(int)y,
						1,
						1,
						temp.getDataType());

				int dataType = temp.getDataType();


				int value = getUnsignedShort(bb);
*/
				int a = temp.ReadRaster((int)x,
						(int)y,
						1,
						1,
						array1);

				//System.out.println(value + " ?? " + array1[0]);

				channelMeans[i - 1] += array1[0];

				//System.out.println(array1[0]);
				output[i-1] = array1[0];

			}
			//System.out.println("##################");
			return output;
		}

		public static int getUnsignedShort(ByteBuffer buffer) {
			int pos = buffer.position();
			int rtn = getUnsignedShort(buffer, pos);
			buffer.position(pos + 2);
			return rtn;
		}

		/**
		 * Read an unsigned short from a buffer
		 * @param buffer Buffer containing the short
		 * @param offset Offset at which to read the short
		 * @return The unsigned short as an int
		 */
		public static int getUnsignedShort(ByteBuffer buffer, int offset) {
			return asUnsignedShort(buffer.getShort(offset));
		}

		/**
		 * @return the short value converted to an unsigned int value
		 */
		public static int asUnsignedShort(short s) {
			return s & 0xFFFF;
		}

		public synchronized void addObservation_from_array(Dataset image, float[] data, int imageId){

			imagesVisible.add(imageId);

			for(int i = 1; i <= image.GetRasterCount(); i++){

				channelMeans[i - 1] += data[i - 1];


			}

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

		}

		public void prepare(){

			for(int i = 0; i < channelMeans.length; i++)
				channelMeans[i] = 0;

			imagesVisible.clear();
		}

	}


	public static double findMaxZ(ArrayList<String> pointClouds) throws IOException{

		double output = Double.POSITIVE_INFINITY;

		for(int i = 0; i < pointClouds.size(); i++){

			File tempFile = new File(pointClouds.get(i));
			LASReader asd2 = new LASReader(tempFile);

			if(asd2.minZ < output)
				output = asd2.minZ;

		}

		return output;

	}	


	static class multiImage implements Runnable{

		int threadNumber;
		int numberOfThreads;
		ArrayList<Dataset> datasets;


		double[] interior;
		ArrayList<double[]> exteriors;
		ArrayList<double[]> extents;
		ArrayList<Integer> imageIDs;

		BlockingQueue<pointAI> queue;

		boolean done = false;

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

		public void ask(pointAI tempP){

			this.done = false;

			for(int j = 0; j < threadIn.size(); j++){

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

	}

	public static double roundAvoid(double value, int places) {
		double scale = Math.pow(10, places);
		return Math.round(value * scale) / scale;
	}

	public static String timeRemaining(int n, int i, long elapsed){


		double perUnit = (double)elapsed / (double)i;

		String remaining = ( roundAvoid(perUnit * ((double)n - (double)i) / 1000000000.0,2)) + " s ";



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

		int nCores = 1;

		boolean lasFormat = false;
		boolean txtFormat = false;
		boolean wildCard = false;

		ArrayList<String> filesList = new ArrayList<String>();

		String input = "";
		String iparse = "xyz";
		String oparse = iparse;
		String odir = "";


		File exteriorFile = null;
		File interiorFile = null;

		ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
		ArrayList<File> inputFiles = new ArrayList<>();

		//if(args.length > 0){
		input = aR.files[0];
			//input = args[0];
		System.out.println(input);

		lasFormat = new File(aR.files[0]).getName().split("\\.")[1].equals("las");
		//lasFormat = aR.files[0].split("\\.")[1].equals("las");
		txtFormat = new File(aR.files[0]).getName().split("\\.")[1].equals("txt");

			//wildCard = input.split(pathSep)[(input.split(pathSep)).length - 1].split("\\.")[0].equals("*");
		wildCard = new File(aR.files[0]).getName().split("\\.")[1].equals("*");

		//}

		gdal.AllRegister();

		System.out.println("max cache: " + gdal.GetCacheMax());
		//gdal.SetCacheMax(413375897 * 4);
		gdal.SetCacheMax((int)(aR.gdal_cache_gb * 1073741824));

		System.out.println("max cache: " + gdal.GetCacheMax() + " " + aR.gdal_cache_gb);
		//System.exit(1);

		nCores = aR.cores;
		iparse = aR.iparse;
		oparse = aR.oparse;
		odir = aR.odir;

		exteriorFile = new File(aR.exterior);
		interiorFile = new File(aR.interior);


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
				Thread temp = new Thread(new multiTXT2LAS(tempList, iparse, nCores, ii, odir, aR));
				lista11.add(temp);
				temp.start();


			}

			for(int i = 0; i < lista11.size(); i++){

				try{

					lista11.get(i).join();
				}catch(Exception e) {
					e.printStackTrace();
				}
			}

			filesList = tiedostoLista.files;

			for(int i = 0; i < filesList.size(); i++){
				//System.out.println(filesList.get(i));
				pointClouds.add(new LASReader(new File(filesList.get(i))));

			}
		}

		nCores = 1;
		ogr.RegisterAll();
		gdal.AllRegister();
		gdal.UseExceptions();

		ArrayList<ArrayList<Dataset>> threadImages = new ArrayList<ArrayList<Dataset>>();

		ArrayList<double[]> exteriors = new ArrayList<double[]>();
		double[] interior2 = new double[4];


		interior2 = readIo(interiorFile, "\t");

		final double[] interior = interior2;

		ArrayList<Integer> imageIDs = new ArrayList<Integer>();

		ArrayList<String> tempList = filesList;

		double minZ = findMaxZ(tempList);

		ArrayList<double[]> extents = new ArrayList<double[]>();
		ArrayList<Dataset> datasets = new ArrayList<Dataset>();
		ArrayList<String> imageNames = new ArrayList<>();

		readImages(exteriorFile, datasets, threadImages, imageIDs, exteriors, "\t", interior, extents, minZ, rotationMatrix, imageNames, aR);

		int n_bands = datasets.get(0).GetRasterCount();

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


		int n_bytes = 0;

		switch(datasets.get(0).GetRasterBand(1).getDataType()){

			case 1:
				n_bytes = 1;
				break;

			case 2:
				//System.out.println("READING: " + Arrays.toString(band_list));
				n_bytes = 2;
				//System.out.println(value);
				break;

			case 3:
				n_bytes = 2;
				break;

			case 6:
				n_bytes = 4;
				break;

			case 7:
				n_bytes = 8;
				break;

			default:
				break;

		}

		int data_type = datasets.get(0).GetRasterBand(1).getDataType();


		double x_s = datasets.get(0).getRasterXSize();
		double y_s = datasets.get(0).getRasterYSize();

		ArrayList<double[][]> rotationMatrices = new ArrayList<>();

		int pix_threshold_x = (int) ((double)datasets.get(0).getRasterXSize() * aR.edges);
		int pix_threshold_y = (int) ((double)datasets.get(0).getRasterYSize() * aR.edges);

		ArrayList<createCHM.MaxSizeHashMap<Integer, float[]>> maps = new ArrayList<>();


		for(int i = 0; i < datasets.size(); i++){

			maps.add(new createCHM.MaxSizeHashMap(10000));

			rotationMatrices.add(makeRotationMatrix(interior, exteriors.get(i)));

		}

		if(!aR.debug) {

			//IntStream.range(0, tempList.size()).parallel().forEach(t -> {
			for (int t = 0; t < tempList.size(); t++) {

				process_file(aR, oparse, exteriors, interior, imageIDs, tempList, extents, datasets, n_bands, n_bytes, data_type, x_s, y_s, rotationMatrices, pix_threshold_x, pix_threshold_y, t);

			}
		}else{

			pointAI tempP = new pointAI(n_bands, n_bytes, data_type);

			/* DEBUG CODE */
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
	}

	private static void process_file(argumentReader aR, String oparse, ArrayList<double[]> exteriors, double[] interior, ArrayList<Integer> imageIDs, ArrayList<String> tempList, ArrayList<double[]> extents, ArrayList<Dataset> datasets, int n_bands, int n_bytes, int data_type, double x_s, double y_s, ArrayList<double[][]> rotationMatrices, int pix_threshold_x, int pix_threshold_y, int t) throws IOException {
		pointAI tempP = new pointAI(n_bands, n_bytes, data_type);


		int outsidePoint = 0;

		File tempFile = new File(tempList.get(t));

		//System.out.println(tempFile.getAbsolutePath());

		File ofile2 = aR.createOutputFileWithExtension(tempFile, "_ai.txt");

		if (ofile2.exists())
			ofile2.delete();

		ofile2.createNewFile();

		LasPoint tempPoint = new LasPoint();

		LASReader asd2 = new LASReader(tempFile);

		File outputFile = null;
		pointWriterMultiThread pw = null;
		LasPointBufferCreator buf = null;

		if (aR.olas) {

			outputFile = aR.createOutputFile(asd2);
			pw = new pointWriterMultiThread(outputFile, asd2, "ai2las", aR);

			buf = new LasPointBufferCreator(1, pw);

		}

		tempFile = null;

		long n = asd2.getNumberOfPointRecords();

		int count = 0;

		long lStartTime = 0;

		long lEndTime = 0;

		ArrayList<double[]> valmiit = new ArrayList<double[]>();

		double[] temp;

		int n_threads = 0;


		Thread[] threads = new Thread[n_threads];

		int n_img_per_thread = (int)Math.ceil((double) datasets.size() / (double)n_threads);

		ArrayList<PriorityBlockingQueue<double[]>> thread_point_que = new ArrayList<>();
/*
				for (int i = 0; i < n_threads; i++) {

					thread_point_que.add(new PriorityBlockingQueue<>());

					int mini = i * n_img_per_thread;
					int maxi1 = Math.min(datasets.size(), mini + n_img_per_thread);
					threads[i] = new ai2lasParallel(mini, maxi1, tempP, thread_point_que.get(thread_point_que.size()-1), datasets, interior, exteriors, rotationMatrices,
							x_s, y_s, pix_threshold_x, pix_threshold_y, imageIDs, extents);
					threads[i].start();
				}
*/
		int x_size = datasets.get(0).getRasterXSize();

		try {

			FileWriter fw = new FileWriter(ofile2);
			BufferedWriter bw2 = new BufferedWriter(fw, (int) Math.pow(2, 10));

			lStartTime = System.nanoTime();
			boolean pointFound = false;

			for (int p = 0; p < asd2.getNumberOfPointRecords(); p += 10000) {

				int maxi = (int) Math.min(10000, Math.abs(asd2.getNumberOfPointRecords() - (p)));

				try {
					asd2.readRecord_noRAF(p, tempPoint, maxi);
				} catch (Exception e) {
					e.printStackTrace();
				}

				for (int j = 0; j < maxi; j++) {

					asd2.readFromBuffer(tempPoint);

					if(!aR.inclusionRule.ask(tempPoint, p+j, true)){
						continue;
					}

					pointFound = false;

					if (j + p % 10000 == 0) {
						lEndTime = System.nanoTime();
						System.out.print("\033[2K"); // Erase line content
						System.out.print((j + p) + "|" + n + " Time remaining: " + timeRemaining((int) n, j + p, (lEndTime - lStartTime)) + " ms/point: " + roundAvoid(((double) lEndTime - (double) lStartTime) / 1000000.0 / (double) (j + p), 2) + " o: " + outsidePoint + " " + gdal.GetCacheUsed() + "\r");
					}

					tempP.prepare();

					String outWrite2 = "";

					//int n_threads = 1;
/*
					Thread[] threads = new Thread[n_threads];

					int n_img_per_thread = (int)Math.ceil((double)datasets.size() / (double)n_threads);

					if(false) {
						for (int i = 0; i < n_threads; i++) {

							int mini = i * n_img_per_thread;
							int maxi1 = Math.min(datasets.size(), mini + n_img_per_thread);
							threads[i] = new ai2lasParallel(mini, maxi1, tempP, tempPoint, datasets, interior, exteriors, rotationMatrices,
									x_s, y_s, pix_threshold_x, pix_threshold_y, imageIDs, extents);
							threads[i].start();
						}

						for (int i = 0; i < threads.length; i++) {

							try {
								threads[i].join();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

*/
					/* CAN THIS BE PARALLELIZED?*/
					//if(true)
					int found = 0;

					//long time_start = System.currentTimeMillis();

					for (int j_ = 0; j_ < datasets.size(); j_++) {

						//System.out.println(Arrays.toString(extents.get(j_)));
						if (tempPoint.x >= extents.get(j_)[0] && tempPoint.x <= extents.get(j_)[1] && tempPoint.y >= extents.get(j_)[2] && tempPoint.y <= extents.get(j_)[3]) {


							temp = collinearStuff2(tempPoint, interior, exteriors.get(j_), rotationMatrices.get(j_), x_s, y_s);

							if (temp[0] > pix_threshold_x && temp[0] < (x_s - pix_threshold_x)
									&& temp[1] > pix_threshold_y && temp[1] < (y_s - pix_threshold_y)) {

								found++;

								int key = (int)(temp[1] * x_size + temp[0]);

								//if(!maps.get(j_).containsKey(key)) {
									tempP.addObservation(datasets.get(j_), imageIDs.get(j_), temp[0], temp[1]);
									//maps.get(j_).put(key, tempP.addObservation_return_array(datasets.get(j_), imageIDs.get(j_), temp[0], temp[1]));

								//}else{
									//System.out.println("OBS FROM ARRAY");
									//tempP.addObservation_from_array(datasets.get(j_), maps.get(j_).get(key), imageIDs.get(j_));
								//}

								pointFound = true;

							}
						}
					}

					//long time_end = System.currentTimeMillis();

					//System.out.println("TOOK: " + (time_end-time_start) + " ms " + found	);

					valmiit.clear();

					if (!pointFound) {

						outsidePoint++;
						tempP.done();
						continue;
					}

					tempP.done();

					if (aR.olas) {

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

						buf.writePoint(tempPoint, aR.inclusionRule, (j + p));

					}

					if (!aR.olas) {

						outWrite2 += LASwrite.LASpoint2String(tempPoint, oparse);

						for (int l = 0; l < n_bands; l++) {

							outWrite2 += "\t" + tempP.channelMeans[l];

						}

						bw2.write(outWrite2);
						bw2.newLine();
					}
					outWrite2 = null;

				}
			}

			//bw.close();
			bw2.close();

			//outti.close();
			ofile2 = null;

			tempPoint = null;

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
			pw.close(aR);
		}
	}

	public static void addImageToCache(LinkedList<ArrayList<int[]>> cache, Dataset image) {

		cache.add(new ArrayList<>(image.GetLayerCount()));

		for (int i = 1; i <= image.GetRasterCount(); i++) {

			int[] array = new int[image.getRasterXSize() * image.getRasterYSize()];

			cache.getLast().set(i-1, array);

		}
	}
}


class ai2lasParallel extends Thread{

	int min, max;
	ai2las.pointAI tempP;
	PriorityBlockingQueue<double[]> pointQ;
	ArrayList<Dataset> datasets;
	double[] interior;
	ArrayList<double[]> exterior;
	ArrayList<double[]> extents;
	ArrayList<double[][]> rotationMatrices;
	double x_s, y_s;
	int pix_threshold_x, pix_threshold_y;
	ArrayList<Integer> imageIDs;

	public ai2lasParallel(int min, int max, ai2las.pointAI tempP, PriorityBlockingQueue<double[]> pointQ, ArrayList<Dataset> datasets, double[] interior, ArrayList<double[]> exterior, ArrayList<double[][]> rotationMatrices,
						  double x_s, double y_s, int pix_threshold_x, int pix_threshold_y, ArrayList<Integer> imageIDs, ArrayList<double[]> extents){

		this.min = min;
		this.max = max;
		this.tempP = tempP;
		this.pointQ = pointQ;
		this.datasets = datasets;
		this.interior = interior;
		this.exterior = exterior;
		this.rotationMatrices = rotationMatrices;
		this.x_s = x_s;
		this.y_s = y_s;
		this.pix_threshold_y = pix_threshold_y;
		this.pix_threshold_x = pix_threshold_x;
		this.imageIDs = imageIDs;
		this.extents = extents;

	}

	@Override
	public void run(){

		double[] point;
		double[] temp;

		while(true){

			point = pointQ.poll();

			for( int i = min; i < this.max; i++ ) {

				if (point[0] >= extents.get(i)[0] && point[0] <= extents.get(i)[1] && point[1] >= extents.get(i)[2] && point[1] <= extents.get(i)[3]) {

					temp = ai2las.collinearStuff_array(point, interior, exterior.get(i), rotationMatrices.get(i), x_s, y_s);

					if (temp[0] > pix_threshold_x && temp[0] < (x_s - pix_threshold_x)
							&& temp[1] > pix_threshold_y && temp[1] < (y_s - pix_threshold_y)) {

						tempP.addObservation(datasets.get(i), imageIDs.get(i), temp[0], temp[1]);

					}
				}
			}
		}

	}

}
