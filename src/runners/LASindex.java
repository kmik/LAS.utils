package runners;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import LASio.*;
import tools.*;
class LASindex {


	static class multiPlot implements Runnable{

	   private final int j;     
	   private final int k;
	   private final ArrayList<String> tiedostot;
	   private final int spacing;

        public multiPlot (int j1, int k2, ArrayList<String> tiedostot2, int spacing2)
    	{

    		j = j1;
    		k = k2;
    		tiedostot = tiedostot2;
    		spacing = spacing2;

    	}
         
	   public void run() {
	      
	        try {

	              runMultiThreadIndexing(j, k, tiedostot, spacing);

	       } catch (Exception e) {

	           System.out.println(e);

	       }

	   }

	}

	public static void runMultiThreadIndexing(int threadNumber, int numberOfCores, ArrayList<String> tiedostot, int spacing) throws IOException{

		int pienin;
      	int suurin;  
      	int jako = 1;
      	
  		if(tiedostot.size() > numberOfCores)
        	jako = Math.round(tiedostot.size() / numberOfCores);
        
       


        if(threadNumber != numberOfCores){

          	pienin = (threadNumber - 1) * jako;
          	suurin = threadNumber * jako;  

        }
  				
        else{
  			pienin = (threadNumber - 1) * jako; 
          	suurin = tiedostot.size();
  		}
  		//System.out.println("pienin " + pienin);
  		//System.out.println("suurin " + suurin);



  		for(int i = pienin; i < suurin; i++){

  			File file1 = new File(tiedostot.get(i));
			LASReader asd = new LASReader(file1);
			//System.out.println(asd.getFile());
				
			//tinfour.las.LasPoint pointti = new tinfour.las.LasPoint();
			//long counter = 0;
			System.out.println("Indexing: " + asd.getFile().getName());

            try {
                asd.index(25);
            }catch (Exception e){
                e.printStackTrace();
            }
			    //index(asd, spacing, i);

  		}

	}

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


	/**
	 *	Indexes a .las file to enable faster spatial queries.
	 *	Creates a .lasx file with the same filename as the point
	 *	cloud.
	 *
	 *	@param in 				Input point cloud file
	 *	@param spacing 			The square size of the index grid.
	 *							Optimal size depends on the point
	 *							density of the data.
	 *	@param ii 				WHAT IS THIS?
	 */

	public static void index(LASReader in, int spacing , int ii) throws IOException{

		long minX = (long)in.getMinX();
		long maxX = (long)Math.ceil(in.getMaxX());
		long minY = (long)in.getMinY();
		long maxY = (long)Math.ceil(in.getMaxY());

		double xSpacing = (maxX - minX) / (double)spacing;
		double ySpacing = (maxY - minY) / (double)spacing;

		String outputFileName = in.getFile().getParent() + File.separator + in.getFile().getName().split(".las")[0] + ".lasx";

		Cantor homma = new Cantor();

		HashMap<Long, ArrayList<Long>> save = new HashMap<Long, ArrayList<Long>>();
		
		long tempx = minX;
		long tempy = maxY;

		int countx = 0;
		int county = 0;

		long[] asdi = new long[2];
		asdi[0] = -99L;
		asdi[1] = -99L;

		int maxCountX = 0;
		int maxCountY = 0;
		while(tempx <= maxX){
			while(tempy >= minY){

				long[] temp = new long[2];
				temp[0] = countx;
				temp[1] = county;

				if(countx > maxCountX)
					maxCountX = countx;
				if(county > maxCountY)
					maxCountY = county;

				save.put(homma.pair(temp[0], temp[1]), new ArrayList<Long>());
				tempy -= spacing;
				county++;
			}
			tempx += spacing;
			tempy = maxY;
			countx++;
			county = 0;
		}

		LasPoint tempPoint = new LasPoint();

		
		long n = in.getNumberOfPointRecords();

		//System.out.println(in);
		//System.out.println("GOT HERE " + n);

		//QuadTree qt = new QuadTree(in.getMinX(), in.getMinY(), in.getMaxX(), in.getMaxY());

		for(long i = 0; i < n; i++){

			long[] temppi = new long[2];
			//System.out.println(i + " " + n);
			in.readRecord(i, tempPoint);

			//qt.set(tempPoint.x, tempPoint.y, (int)i);

			temppi[0] = (long)Math.floor((tempPoint.x - (double)minX) / (double)spacing);   //X INDEX
			temppi[1] = (long)Math.floor(((double)maxY - tempPoint.y) / (double)spacing);
			//System.out.println(Arrays.toString(temppi) + " maxX: " + maxCountX + " maxY: " + maxCountY);
			save.get(homma.pair(temppi[0], temppi[1])).add(i);
			//System.out.println(i);
		}

		int count = 0;



		for(Long key : save.keySet()){

			ArrayList<Long> tempList = new ArrayList<Long>();
			//System.out.println("Orig min max: " + save.get(key).get(0) + "|" + save.get(key).get(save.get(key).size() - 1));
			if(save.get(key).size() > 0){

				tempList.add(save.get(key).get(0));

				for(int i = 1; i < save.get(key).size(); i++){

					if(i != (save.get(key).size() - 1)){
						if( (save.get(key).get(i) - save.get(key).get(i - 1)) > 1000){

							//if(count > 0)
							tempList.add(save.get(key).get(i - 1));
							//else
							//	System.out.println("HOXHOX");

							tempList.add(save.get(key).get(i));
							count = 0;
						}
						else{
							count++;
						}
					}else{

						tempList.add(save.get(key).get(i));

					}
				}
			}
			save.put(key, (ArrayList<Long>)tempList.clone());
		}
		
		ArrayList<Long> temppiLista = new ArrayList<Long>();
		temppiLista.add(minX);
		temppiLista.add(maxX);
		temppiLista.add(minY);
		temppiLista.add(maxY);
		temppiLista.add((long)spacing);
		System.out.println(homma.pair(asdi[0], asdi[1]));
		save.put(homma.pair(asdi[0], asdi[1]), temppiLista);

		try {
	        FileOutputStream fileOut = new FileOutputStream(outputFileName);
	        ObjectOutputStream out = new ObjectOutputStream(fileOut);
	        out.writeObject(save);
	        out.close();
	        fileOut.close();
      	}catch(IOException i) {
        i.printStackTrace();
      	}

      	HashMap<Integer, ArrayList<Long>> e = null;
      try {
         FileInputStream fileIn = new FileInputStream(outputFileName);
         ObjectInputStream in2 = new ObjectInputStream(fileIn);
         e = (HashMap<Integer, ArrayList<Long>>) in2.readObject();
         in2.close();
         fileIn.close();
      	}catch(IOException i) {
         i.printStackTrace();
         return;
      		}catch(ClassNotFoundException c) {
         c.printStackTrace();
         return;
      }
      //System.out.println(e);

	}

	public static void indexAll(String dir){

		String outputFileName = dir + "all.lasxALL";
		File indfile = new File(outputFileName);
		

		if(indfile.exists()){
			indfile.delete();
		}

		ArrayList<String> fileList = listFiles(dir, ".lasx");

		try{
			FileWriter fw = new FileWriter(outputFileName, true);
    		BufferedWriter bw = new BufferedWriter(fw);
    		PrintWriter outt = new PrintWriter(bw);            
			for(int i = 0; i < fileList.size(); i++){

				File tiedosto = new File(fileList.get(i));
				HashMap<Integer, ArrayList<Long>> temp = null;
				//System.out.println(tiedosto);
	      		try {
	      			
	         		FileInputStream fileIn = new FileInputStream(fileList.get(i));
	         		ObjectInputStream in2 = new ObjectInputStream(fileIn);
	         		temp = (HashMap<Integer, ArrayList<Long>>) in2.readObject();
	         		in2.close();
	         		fileIn.close();
	      		}catch(IOException e) {
	         		e.printStackTrace();
	         		return;
	      		}catch(ClassNotFoundException c) {
	         		c.printStackTrace();
	         		return;
	      		}
	      		//System.out.println(temp.get(4097));
	      		outt.println(tiedosto.getAbsolutePath().split(".lasx")[0] + ".las" + " " + temp.get(4097).get(0) + " " + temp.get(4097).get(1) + " " + temp.get(4097).get(2) + " " + temp.get(4097).get(3));
			}
			outt.close();
		}catch(Exception e){

		}

	}

	public static double[] indexAll2(String dir) throws IOException{
/*
		String pathSep = System.getProperty("file.separator");
		String outputFileName = dir + pathSep + "all.lasxALL";
		File indfile = new File(outputFileName);
		
		double[] output = new double[2];

		output[0] = Double.POSITIVE_INFINITY;
		output[1] = Double.NEGATIVE_INFINITY;

		if(indfile.exists()){
			//System.out.println("DELETE");
			indfile.delete();
		}

		ArrayList<String> fileList = listFiles(dir, ".las");
		//System.out.println(dir);
		//System.out.println(fileList.size());
		try{
			FileWriter fw = new FileWriter(outputFileName, true);
    		BufferedWriter bw = new BufferedWriter(fw);
    		PrintWriter outt = new PrintWriter(bw);            
			for(int i = 0; i < fileList.size(); i++){

				File tiedosto = new File(fileList.get(i));
				//HashMap<Integer, ArrayList<Long>> temp = null;
				//System.out.println(tiedosto);
				tinfour.las.LasFileReader in = new tinfour.las.LasFileReader(tiedosto);
	      		long minX = (long)in.getMinX();
				long maxX = (long)Math.ceil(in.getMaxX());
				long minY = (long)in.getMinY();
				long maxY = (long)Math.ceil(in.getMaxY());

				if(minX < output[0])
					output[0] = in.getMinX();;
				if(maxY > output[1])
					output[1] = in.getMaxY();

	      		//System.out.println(temp.get(4097));
	      		outt.println(tiedosto.getAbsolutePath() + " " + minX + " " + maxX + " " + minY + " " + maxY);
			}
			outt.close();
		}catch(Exception e){

		}

 */
		//return output;
		return new double[]{0,0};
	}

	public static double[] indexAll3(ArrayList<File> in2, String indfile2) throws IOException{

		File indfile = new File(indfile2);
		
		double[] output = new double[2];

		output[0] = Double.POSITIVE_INFINITY;
		output[1] = Double.NEGATIVE_INFINITY;
		//if(indfile.exists()){
			//System.out.println("DELETE");
			//indfile.delete();
		//}

		indfile.createNewFile();

		//ArrayList<String> fileList = (ArrayList<String>)in2.clone();
		LASReader in = null;
		double[] extent = new double[]{0.0,0.0};

		//System.out.println(fileList);
		try{
			//FileWriter fw = new FileWriter(indfile2, true);
    		//BufferedWriter bw = new BufferedWriter(fw);
    		//PrintWriter outt = new PrintWriter(bw);

			BufferedWriter out = new BufferedWriter(new FileWriter(indfile2), 32768);
			in = new LASReader(in2.get(0));
			for(int i = 0; i < in2.size(); i++){

				File tiedosto = in2.get(i);
				//HashMap<Integer, ArrayList<Long>> temp = null;
				//System.out.println(i);
				in = new LASReader(tiedosto, true);
				extent = in.readHeaderOnlyExtent();

				double minX = extent[0];
				double maxX = Math.ceil(extent[1]);
				double minY = extent[2];
				double maxY = Math.ceil(extent[3]);

				//System.out.println(minX + " " + maxX + " " + minY + " " + maxY);
/*

				in = new LASReader(tiedosto);

	      		minX = (long)in.getMinX();
				maxX = (long)Math.ceil(in.getMaxX());
				minY = (long)in.getMinY();
				maxY = (long)Math.ceil(in.getMaxY());
				System.out.println(minX + " " + maxX + " " + minY + " " + maxY);
				System.out.println("-----------------------");

 */
	      		//System.out.println(temp.get(4097));

	      		if(minX < output[0])
					output[0] = in.getMinX();;
				if(maxY > output[1])
					output[1] = in.getMaxY();

	      		out.write(tiedosto.getAbsolutePath() + " " + minX + " " + maxX + " " + minY + " " + maxY + "\n");

	      		in = null;

			}
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}

		//System.gc();
		//System.gc();
		//System.gc();

		return output;
	}

	public static void main(String[] args) throws IOException {

		String directory = "las/";

		int cellsize = 20;
		int nCores = 1;

		String input = "";

		if(args.length > 0)
			input = args[0];

		if(args.length > 1)
			nCores = Integer.parseInt(args[1]);


		String pathSep = System.getProperty("file.separator");
/*
		if(!System.getProperty("os.name").equals("Linux"))
          pathSep = "\\" + pathSep;
*/
		boolean lasFormat = input.split(pathSep)[(input.split(pathSep)).length - 1].split("\\.")[1].equals("las");
        boolean txtFormat = input.split(pathSep)[(input.split(pathSep)).length - 1].split("\\.")[1].equals("txt");
        boolean wildCard = input.split(pathSep)[(input.split(pathSep)).length - 1].split("\\.")[0].equals("*"); 

        ArrayList<String> filesList = new ArrayList<String>();
        ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();

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
          	pointClouds.add(new LASReader(new File(filesList.get(i))));

          }
        }

		long tStart = 0L;
		
		int n = Math.min(nCores, filesList.size());

		if(nCores > 0){
			//System.out.println(Math.min(nCores, lasFiles.size())); 	

            ArrayList<Thread> lista = new ArrayList<Thread>();
            tStart = System.currentTimeMillis();

            for(int ii = 1; ii <= n; ii++){
              Thread temp = new Thread(new multiPlot(ii, nCores, filesList, cellsize));
              
              lista.add(temp);
              temp.start();

          	}

            for(int i = 0; i < lista.size(); i++){

              try{

              lista.get(i).join();
              }catch(Exception e){}
            }
        }
        indexAll2(pointClouds.get(0).path.getParent());
	}

	

}