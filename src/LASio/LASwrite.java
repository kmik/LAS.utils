package LASio;

import LASio.LASReader;
import LASio.LASraf;
import LASio.LasPoint;
import LASio.PointInclusionRule;
import org.apache.commons.lang.StringUtils;
import utils.argumentReader;

import java.io.*;
import java.util.*;

//import Tinfour.*;

public class LASwrite {

	public static Byte myByte = new Byte("00000000");
	public static Byte myByte2 = new Byte("0000000000000000");
	public static byte myBitti = myByte.byteValue();
	public static byte myBitti2 = myByte.byteValue();

	public static ThreadProgressBar proge = new ThreadProgressBar();

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

	/**
	*	Update header with priori knowledge about the point data. Could be acquired while reading the point data.
	*
	*	@param to 				Point cloud, which header is to be checked
	*	@param nPerReturn 		Points by number of returns
	*	@param minX
	*	@param maxX
	*	@param minY	
	*	@param maxY
	* 	@param maxZ
	*	@param minZ
	*
	* 	@author  Kukkonen Mikko
	* 	@version 0.1
	* 	@since 09.03.2018  
	*/

	public static void updateHeader(long pointCount, long[] nPerReturn, double minX, double maxX, double minY, double maxY
		, double maxZ, double minZ, LASraf to) throws IOException{

		to.writePointCount(pointCount);
		to.writeMinMax(minX, maxX, minY, maxY, maxZ, minZ);
		to.writePByReturn(nPerReturn);

	}

	/**
	*	Converts a LAS point into a line of string
	*	
	*	@param point 				LAS point which is to be transformed into string
	*	@param oparse 				Guide how to parse the output line
	*
	*	@return 					String of text representing the LAS point
	*
	* 	@author  Kukkonen Mikko
	* 	@version 0.1
	* 	@since 09.03.2018  
	*/

	public static String LASpoint2String(LasPoint point, String oparse){

		String output = "";

		String sep = "\t";

		char[] charArray = oparse.toCharArray();

		boolean charFound = false;

		for(int i = 0; i < charArray.length; i++){

			if(charArray[i] == 'x'){
				output += (Double.toString(point.x)); 
				charFound = true;
			}
			if(charArray[i] == 'y'){
				output += (Double.toString(point.y)); 
				charFound = true;
			}
			if(charArray[i] == 'z'){
				output += (Double.toString(point.z)); 
				charFound = true;
			}
			if(charArray[i] == 'i'){
				output += (Integer.toString(point.intensity)); 
				charFound = true;
			}
			if(charArray[i] == 'c'){
				output += (Integer.toString(point.classification)); 
				charFound = true;
			}
			if(charArray[i] == 't'){
				output += (Double.toString(point.gpsTime)); 
				charFound = true;
			}
			if(charArray[i] == 'n'){
				output += (Integer.toString(point.numberOfReturns));  
				charFound = true;
			}
			if(charArray[i] == 'r'){
				output += (Integer.toString(point.returnNumber)); 
				charFound = true;
			}
			if(charArray[i] == 'p'){
				output += (Integer.toString(point.pointSourceId)); 
				charFound = true;
			}
			if(charArray[i] == 'u'){
				output += (Integer.toString(point.userData)); 
				charFound = true;
			}
			if(charArray[i] == 'R'){
				output += (Integer.toString(point.R)); 
				charFound = true;
			}
			if(charArray[i] == 'G'){
				output += (Integer.toString(point.G)); 
				charFound = true;
			}
			if(charArray[i] == 'B'){
				output += (Integer.toString(point.B)); 
				charFound = true;
			}
			if(charArray[i] == 's'){
				output += (0); 
				charFound = true;
			}

			if(i != (charArray.length - 1)& charFound)
				output += sep;

			charFound = false;

		}

		charArray = null;

		return output;

	}

	/**
	*	Converts a line of string into a LAS point
	*	
	*	@param point 				LAS point to which the line is written
	*	@param in 					Line representing a LAS point
	*	@param parse 				Guide how to parse the line
	*	@param sep 					Field separator for the text line
	*
	* 	@author  Kukkonen Mikko
	* 	@version 0.1
	* 	@since 09.03.2018  
	*/

	public static void String2LASpoint(LasPoint point, String in, String parse, String sep){

		char[] charArray = parse.toCharArray();

		String[] tokens = in.split(sep);

		//System.out.println("SHOULD GET HERE");
		for(int i = 0; i < charArray.length; i++){

			if(charArray[i] == 'x')
				point.x = Double.parseDouble(tokens[i]);

			if(charArray[i] == 'y')
				point.y = Double.parseDouble(tokens[i]);

			if(charArray[i] == 'z'){
				//System.out.println(tokens[i]);
				point.z = Double.parseDouble(tokens[i]);
			}

			if(charArray[i] == 'i')
				point.intensity = (int)Math.round(Double.parseDouble(tokens[i]));

			if(charArray[i] == 'c')
				point.classification = Integer.parseInt(tokens[i]);

			if(charArray[i] == 't')
				point.gpsTime = Double.parseDouble(tokens[i]);

			if(charArray[i] == 'n')
				point.numberOfReturns = Integer.parseInt(tokens[i]);

			if(charArray[i] == 'r')
				point.returnNumber = Integer.parseInt(tokens[i]);

			if(charArray[i] == 'p')
				point.pointSourceId = (short)Integer.parseInt(tokens[i]);

			if(charArray[i] == 'u')
				point.userData = Integer.parseInt(tokens[i]);

			if(charArray[i] == 'R')
				point.R = (short)Double.parseDouble(tokens[i]);

			if(charArray[i] == 'G')
				point.G = (short)Double.parseDouble(tokens[i]);

			if(charArray[i] == 'B')
				point.B = (short)Double.parseDouble(tokens[i]);
			if(charArray[i] == 'N')
				point.N = (short)Double.parseDouble(tokens[i]);

		}

		//System.out.println(point.scanAngleRank);

	}


	/**
	*	Converts a txt file into a LAS file.
	*	
	*	
	*
	* 	@author  Kukkonen Mikko
	* 	@version 0.1
	* 	@since 09.03.2018  
	*/

	public static void txt2las(File from, LASraf to, String parse, String softwareName, String sep, PointInclusionRule rule, boolean echoClass) throws IOException{

		//PointInclusionRule rule = new PointInclusionRule();
		//PointModifyRule prule = new PointModifyRule();
		//System.out.println(parse);
		//proge.setName(softwareName + " export");
		//proge.setEnd((int)from.getNumberOfPointRecords());

		//prule.setClassification(2);
		//rule.keepClassifcation(12);
		//rule.dropClassification(12);
		//rule.dropClassification(12);
		//rule.dropFirst();
		//rule.keepTriple();

		to.writeAscii(4, "LASF");
		
		//to.fileChannel.position(to.raFile.length());
		
	    to.writeUnsignedShort((short)2); // = braf.readUnsignedShort();
	    
	    //System.out.println(to.fileChannel.position());
	    to.writeUnsignedShort((short)0); // = braf.readUnsignedShort();

	    // GUID
	    
	    to.writeLong(0);
	    
	    //to.writeUnsignedShort((short)0);
	    
	    //to.writeUnsignedShort((short)0);
	    
	    to.writeAscii(8, "");
	   	//to.skipBytes(16); 
 		
	    //braf.skipBytes(16);
	    
	    to.writeUnsignedByte((byte)1);// = braf.readUnsignedByte();
	    to.writeUnsignedByte((byte)2);// = braf.readUnsignedByte();
	    
	    //System.out.println((byte)from.versionMajor);
	    //System.out.println((byte)from.versionMinor);
	    


	    to.writeAscii(32, "LASutils (c) by Mikko Kukkonen");// systemIdentifier = braf.readAscii(32);
	    to.writeAscii(32, (softwareName + " version 0.1"));// generatingSoftware = braf.readAscii(32);
	    
	    //System.out.println(from.generatingSoftware);
	    //Date now = new Date();     // Gets the current date and time
		int year = Calendar.getInstance().get(Calendar.YEAR); //now.getYear();
		Calendar calendar = Calendar.getInstance();
		int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);  
	    to.writeUnsignedShort((short)dayOfYear);// = braf.readUnsignedShort();
	    
	    to.writeUnsignedShort((short)(year));// = braf.readUnsignedShort();

	    //fileCreationDate = cal.getTime();

	    to.writeUnsignedShort((short)227);// = braf.readUnsignedShort();
	    //System.out.println((int)from.headerSize);
	    
	    //to.writeUnsignedInt((int)from.offsetToPointData);// = braf.readUnsignedInt();
	    to.writeUnsignedInt(227);

	    //to.writeUnsignedInt((int)from.numberVariableLengthRecords);// = braf.readUnsignedInt();
	    to.writeUnsignedInt(0);// = braf.readUnsignedInt();

	    //System.out.println((int)from.numberVariableLengthRecords);
	    to.writeUnsignedByte((byte)3);// = braf.readUnsignedByte();
	    to.writeUnsignedShort((short)34);// = braf.readUnsignedShort();
	    
	    to.writeUnsignedInt(0);// = braf.readUnsignedInt();
	    //System.out.println(from.legacyNumberOfPointRecords);
	    //legacyNumberOfPointsByReturn = new long[5];
	   	to.writeUnsignedInt(0);

	   	//System.out.println(from.legacyNumberOfPointsByReturn[1]);
	   	
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);

		
	    
	    to.writeDouble(0.01);// = braf.readDouble();
	    
	    
	    to.writeDouble(0.01);// = braf.readDouble();
	    to.writeDouble(0.01);// = braf.readDouble();
	    
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();

	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();

	    //System.out.println(to.raFile.length());
	    /*
	    if(from.numberVariableLengthRecords > 0){

	    	System.out.println("MORE VARIABLES");
	    	
	    }
		*/
	    to.writeBuffer2();
	    long n = 0;//from.getNumberOfPointRecords();
		LasPoint tempPoint = new LasPoint();
		//to.writeBuffer();
		//to.writeShort((short)0);

		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;

		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		double minZ = Double.POSITIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		long pointCount = 0;

		long[] pointsByReturn = new long[5];
		String line = "";

		//LasPoint tempPoint = new LasPoint();

		double offSet = 0.0;
		double scaleFactor = 0.01;

		int count = 0;

		try {
	        FileInputStream fis = new FileInputStream(from);
	        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

	        while((line = in.readLine())!= null){

	        	//System.out.println("line: " + line);
	        	String2LASpoint(tempPoint, line, parse, sep);
	        	//System.out.println(tempPoint.z + " " + tempPoint.R + " " + tempPoint.N);

				if(echoClass){

					if(tempPoint.numberOfReturns == 0){
						tempPoint.numberOfReturns = 1;
						tempPoint.returnNumber = 1;
					}
					else if(tempPoint.numberOfReturns == 1){
						tempPoint.numberOfReturns = 2;
						tempPoint.returnNumber = 1;
					}
					else if(tempPoint.numberOfReturns == 2){
						tempPoint.numberOfReturns = 2;
						tempPoint.returnNumber = 2;
					}
					else if(tempPoint.numberOfReturns == 3){
						tempPoint.numberOfReturns = 3;
						tempPoint.returnNumber = 2;
					}
				}

	        	if(rule.ask(tempPoint, count, false)){

					pointCount++;
					
					int returnNumberi = tempPoint.returnNumber - 1;

					if(returnNumberi >= 0)
						pointsByReturn[returnNumberi]++;

				 	

					double x = tempPoint.x;
					double y = tempPoint.y;
					double z = tempPoint.z;

					if(z < minZ)
						minZ = z;
					if(x < minX)
						minX = x;
					if(y < minY)
						minY = y;

					if(z > maxZ)
						maxZ = z;
					if(x > maxX)
						maxX = x;
					if(y > maxY)
						maxY = y;
					//System.out.println((int)((x - from.xOffset) / from.xScaleFactor));
					int lx = (int)((x - offSet) / scaleFactor);
		    		int ly = (int)((y - offSet) / scaleFactor);
		    		int lz = (int)((z - offSet) / scaleFactor);
					
					to.writeInt(lx);
					to.writeInt(ly);

					to.writeInt(lz);
					
					//System.out.println(lx);

					to.writeUnsignedShort((short)tempPoint.intensity);// braf.readUnsignedShort()
					
					//String returnNumber = Integer.toBinaryString(tempPoint.returnNumber);
					String bitti = "";

					if(tempPoint.edgeOfFlightLine)
						bitti += "1";
					else
						bitti += "0";

					bitti += String.format("%01d", Integer.valueOf(Integer.toBinaryString(tempPoint.scanDirectionFlag)));
					bitti += String.format("%03d", Integer.valueOf(Integer.toBinaryString(tempPoint.numberOfReturns)));
					bitti += String.format("%03d", Integer.valueOf(Integer.toBinaryString(tempPoint.returnNumber)));

					//System.out.println(String.format("%03d", Integer.valueOf(Integer.toBinaryString(tempPoint.returnNumber))));

					//System.out.println(Integer.toBinaryString(10 | tempPoint.returnNumber));

					byte myByte = (byte)Integer.parseInt(bitti, 2);
					//System.out.println(myByte);
					to.writeUnsignedByte(myByte);

				 	bitti = "";
				 	if(tempPoint.withheld)
				 		bitti += 1;
				 	else
				 		bitti += 0;
				 	if(tempPoint.keypoint)
				 		bitti += 1;
				 	else
				 		bitti += 0;
				 	if(tempPoint.synthetic)
				 		bitti += 1;
				 	else
				 		bitti += 0;

				 	bitti += String.format("%04d", Integer.valueOf(Integer.toBinaryString(tempPoint.classification)));


				 	myByte = (byte)Integer.parseInt(bitti, 2);
					to.writeUnsignedByte(myByte);
					
					 // we currently skip
	    			//   scan angle rank  1 byte
	    				//   user data        1 byte
	    			//   point source ID  2 bytes
					to.writeUnsignedByte((byte)tempPoint.scanAngleRank);
					to.writeUnsignedByte((byte)tempPoint.userData);
					to.writeUnsignedShort(tempPoint.pointSourceId);


					//if (from.pointDataRecordFormat == 1 || from.pointDataRecordFormat == 3) {
				    to.writeDouble(tempPoint.gpsTime);// = braf.readDouble();
				      // Depending on the gpsTimeType element, the GPS time can be
				      // in one of two formats:
				      //    GPS Week Time  seconds since 12:00 a.m. Sunday
				      //    GPS Satellite Time   seconds since 12 a.m. Jan 6, 1980
				      //                         minus an offset 1.0e+9
				      //    The mapping to a Java time requires information about
				      //    the GPS time type
		    		//}
					//   scan angle rank  1 byte
		    		//   user data        1 byte
		    		//   point source ID  2 bytes

					// 
					//p.returnNumber = mask & 0x07;

					//System.out.println(tempPoint.returnNumber);
					//if(i%20 == 0){
						//System.out.println(i);
						//to.writeBuffer();
					//}

					to.writeUnsignedShort(tempPoint.R);
					to.writeUnsignedShort(tempPoint.G);
					to.writeUnsignedShort(tempPoint.B);
				}
				count++;
				//System.out.println(count);
	        }
	        in.close();
	    }catch(Exception e){System.out.println(e);}
	    to.writeBuffer2();
	    updateHeader(pointCount, pointsByReturn, minX, maxX, minY, maxY
			, maxZ, minZ, to);
	}



	public static int getBit(byte tavu, int k) {

		return (1 == ((tavu >> k) & 1))  ? 1 : 0 ;
    	//return (tavu >> k) & 1;

	}

	public static byte setBit(byte tavu, int k, int in){

		if(in == 1)
			return (byte) (tavu | (1 << k));
		else
			return (byte) (tavu & ~(1 << k));

	}

	public byte setBit(byte in, int bit){

		return in |= 1 << bit;

	}

	public byte unSetBit(byte in, int bit){

		return in &= ~(1 << bit);
	}

	public static byte setUnsetBit(byte in, int bit, int set){

		if(set==1)
			return in |= 1 << bit;
		else
			return in &= ~(1 << bit);
	}


	/**
	*	Writes a point into a LAS file.
	*	
	*	@param to 						LAS file to write into
	*	@param tempPoint				Point to write
	*	@param rule 					Filer or transformation applied to point
	*	@param xScaleFactor				...
	*	@param yScaleFactor				...
	*	@param zScaleFactor				...
	*	@param xOffset					...
	*	@param yOffset					...
	*	@param zOffset					...
	*	@param pointDataRecordFormat	THIS IS NOT USED YET!!!!
	*	@param i 						Point index
	*
	* 	@author  Kukkonen Mikko
	* 	@version 0.1
	* 	@since 09.03.2018  
	*/

	public static boolean writePoint(LASraf to, LasPoint tempPoint, PointInclusionRule rule,
												  double xScaleFactor, double yScaleFactor, double zScaleFactor,
												  double xOffset, double yOffset, double zOffset, int pointDataRecordFormat, int i) throws IOException{


		/* Written or not */
		boolean output = false;

		//Byte myByte = new Byte("00000000");
		byte myBitti = myByte.byteValue();

		/* Write if rule says so */
		if(rule.ask(tempPoint, i, true)){

			/* We got here, so output true */
			output = true;

			double x = tempPoint.x;
			double y = tempPoint.y;
			double z = tempPoint.z;

			/* Scale x and apply xOffset */
			int lx = (int)((x - xOffset) / xScaleFactor);
			/* Scale y and apply yOffset */
	   		int ly = (int)((y - yOffset) / yScaleFactor);
			/* Scale z and apply zOffset */
	   		int lz = (int)((z - zOffset) / zScaleFactor);

			/* Write scaled and offset x, y and z */
			to.writeInt(lx);
			to.writeInt(ly);
			to.writeInt(lz);

			/* Write intensity */
			to.writeUnsignedShort((short)tempPoint.intensity);// braf.readUnsignedShort()

			/*
				Return Number 3 bits 					(bits 0, 1, 2) 3 bits *
				Number of Returns (given pulse) 3 bits 	(bits 3, 4, 5) 3 bits *
				Scan Direction Flag 1 bit 				(bit 6) 1 bit *
				Edge of Flight Line 1 bit 				(bit 7)
			 */
/*
			myByte = setBit(myByte, 7, (tempPoint.edgeOfFlightLine) ? 1 : 0);
			myByte = setBit(myByte, 6, tempPoint.scanDirectionFlag);
			myByte = setBit(myByte, 5, getBit((byte)tempPoint.numberOfReturns, 2));
			myByte = setBit(myByte, 4, getBit((byte)tempPoint.numberOfReturns, 1));
			myByte = setBit(myByte, 3, getBit((byte)tempPoint.numberOfReturns, 0));
			myByte = setBit(myByte, 2, getBit((byte)tempPoint.returnNumber, 2));
			myByte = setBit(myByte, 1, getBit((byte)tempPoint.returnNumber, 1));
			myByte = setBit(myByte, 0, getBit((byte)tempPoint.returnNumber, 0));

 */


			myBitti = setUnsetBit(myBitti, 7, tempPoint.edgeOfFlightLine ? 1 : 0);
			myBitti = setUnsetBit(myBitti, 6, tempPoint.scanDirectionFlag);
			myBitti = setUnsetBit(myBitti, 5, (byte)tempPoint.numberOfReturns >> 2);
			myBitti = setUnsetBit(myBitti, 4, (byte)tempPoint.numberOfReturns >> 1);
			myBitti = setUnsetBit(myBitti, 3, (byte)tempPoint.numberOfReturns >> 0);
			myBitti = setUnsetBit(myBitti, 2, (byte)tempPoint.returnNumber >> 2);
			myBitti = setUnsetBit(myBitti, 1, (byte)tempPoint.returnNumber >> 1);
			myBitti = setUnsetBit(myBitti, 0, (byte)tempPoint.returnNumber >> 0);

			/*
			myByte = setBit(myByte, 7, (tempPoint.edgeOfFlightLine) ? 1 : 0);
			myByte = setBit(myByte, 6, tempPoint.scanDirectionFlag);
			myByte = setBit(myByte, 5, (byte)tempPoint.numberOfReturns >> 2);
			myByte = setBit(myByte, 4, (byte)tempPoint.numberOfReturns >> 1);
			myByte = setBit(myByte, 3, (byte)tempPoint.numberOfReturns >> 0);
			myByte = setBit(myByte, 2, (byte)tempPoint.returnNumber >> 2);
			myByte = setBit(myByte, 1, (byte)tempPoint.returnNumber >> 1);
			myByte = setBit(myByte, 0, (byte)tempPoint.returnNumber >> 0);
			*/
			/* Write byte */
			to.writeUnsignedByte(myBitti);

			/* Reset the byte */
			//myByte = 0;


			/*

			Bits 	Explanation

			0:4 	Classification Standard ASPRS classification as defined in the
					following classification table.
			5 		Synthetic If set then this point was created by a technique
					other than LIDAR collection such as digitized from
					a photogrammetric stereo model.
			6 		Key-point If set, this point is considered to be a model keypoint
					and thus generally should not be withheld in
					a thinning algorithm.
			7 		Withheld If set, this point should not be included in 

			*/
			/*
			myByte = setBit(myByte, 7, (tempPoint.withheld) ? 1 : 0);
			myByte = setBit(myByte, 6, (tempPoint.keypoint) ? 1 : 0);
			myByte = setBit(myByte, 5, (tempPoint.synthetic) ? 1 : 0);
			myByte = setBit(myByte, 4, getBit((byte)tempPoint.classification,4));
			myByte = setBit(myByte, 3, getBit((byte)tempPoint.classification,3));
			myByte = setBit(myByte, 2, getBit((byte)tempPoint.classification,2));
			myByte = setBit(myByte, 1, getBit((byte)tempPoint.classification,1));
			myByte = setBit(myByte, 0, getBit((byte)tempPoint.classification,0));

			 */

			myBitti = setUnsetBit(myBitti, 7, (tempPoint.withheld) ? 1 : 0);
			myBitti = setUnsetBit(myBitti, 6, (tempPoint.keypoint) ? 1 : 0);
			myBitti = setUnsetBit(myBitti, 5, (tempPoint.synthetic) ? 1 : 0);
			myBitti = setUnsetBit(myBitti, 4, (byte)tempPoint.classification >> 4);
			myBitti = setUnsetBit(myBitti, 3, (byte)tempPoint.classification >> 3);
			myBitti = setUnsetBit(myBitti, 2, (byte)tempPoint.classification >> 2);
			myBitti = setUnsetBit(myBitti, 1, (byte)tempPoint.classification >> 1);
			myBitti = setUnsetBit(myBitti, 0, (byte)tempPoint.classification >> 0);

			/*
            myByte = setBit(myByte, 7, (tempPoint.withheld) ? 1 : 0);
            myByte = setBit(myByte, 6, (tempPoint.keypoint) ? 1 : 0);
            myByte = setBit(myByte, 5, (tempPoint.synthetic) ? 1 : 0);
            myByte = setBit(myByte, 4, (byte)tempPoint.classification >> 4);
            myByte = setBit(myByte, 3, (byte)tempPoint.classification >> 3);
            myByte = setBit(myByte, 2, (byte)tempPoint.classification >> 2);
            myByte = setBit(myByte, 1, (byte)tempPoint.classification >> 1);
            myByte = setBit(myByte, 0, (byte)tempPoint.classification >> 0);
			*/

			/* Write the byte */
			to.writeUnsignedByte(myBitti);

			/* Write scan angle */
			to.writeUnsignedByte((byte)tempPoint.scanAngleRank);

			/* Write user data */
			to.writeUnsignedByte((byte)tempPoint.userData);

			/* Write point source ID */
			to.writeUnsignedShort(tempPoint.pointSourceId);


			/* Previous stuff is pretty much standard for any point data type.
				How to do the extra stuff that is point data type specific? */

			/* RGB is included in both 2 and 3 record types in LAS 1.2 */

			if (pointDataRecordFormat == 1 || pointDataRecordFormat == 3 || pointDataRecordFormat == 4 || pointDataRecordFormat == 5) {

				to.writeDouble(tempPoint.gpsTime);// = braf.readDouble();

			}

			if(pointDataRecordFormat == 2 || pointDataRecordFormat == 3 || pointDataRecordFormat == 5){

				to.writeUnsignedShort(tempPoint.R);
				to.writeUnsignedShort(tempPoint.G);
				to.writeUnsignedShort(tempPoint.B);

			}

			if(pointDataRecordFormat == 4 || pointDataRecordFormat == 5){

				to.writeUnsignedByte((byte)tempPoint.WavePacketDescriptorIndex);
				to.writeLong(tempPoint.ByteOffsetToWaveformData);
				to.writeUnsignedInt(tempPoint.WaveformPacketSizeInBytes);
				to.writeFloat(tempPoint.ReturnPointWaveformLocation);

				to.writeFloat(tempPoint.x_t);
				to.writeFloat(tempPoint.y_t);
				to.writeFloat(tempPoint.z_t);
			}

		}

		return output;
	}


	/**
	*  	Writes a "dummy" header for a LAS file. Currently only writes asprs LAS 1.2 format
	*	
	*	@Param to 				LAS file to write into
	*	@Param softwareName 	Creating software name
	*		
	*	TO BE ADDED!!!!
	*	@Param versionMinor		Version minor
	*	@Param versionMajor		Version major
	*	@Param pointDataFrormat Point data format
	* 	@author  Kukkonen Mikko
	* 	@version 0.1
	* 	@since 09.03.2018  
	*/

	public static synchronized void writeHeader(LASraf to, String softwareName, int major, int minor, int pointDataType, int pointDataRecordLength,
												int headerSize, long offSetToPointData, long numVariableLength, int fileSourceId,
												int globalEncoding, double xScale, double yScale, double zScale,
												double xOff, double yOff, double zOff) throws IOException{


		to.pointDataRecordFormat = pointDataType;
		to.xScaleFactor = xScale;
		to.yScaleFactor = yScale;
		to.zScaleFactor = zScale;

		to.xOffset = xOff;
		to.yOffset = yOff;
		to.zOffset = zOff;

		to.writeAscii(4, "LASF");

		/* File source ID */
		if(fileSourceId == 0)
	        to.writeUnsignedShort((short)42); // = braf.readUnsignedShort();
        else
            to.writeUnsignedShort((short)fileSourceId); // = braf.readUnsignedShort();

		/* Global encoding */
	    to.writeUnsignedShort((short)globalEncoding); // = braf.readUnsignedShort();

		/* ID */
	    to.writeLong(0);

		/* GUID */
	    to.writeAscii(8, "");

		/* Version major */
	    to.writeUnsignedByte((byte)major);// = braf.readUnsignedByte();

		/* Version minor */
	    to.writeUnsignedByte((byte)minor);// = braf.readUnsignedByte();

		/* System identified */
	    to.writeAscii(32, "LASutils (c) by Mikko Kukkonen");// systemIdentifier = braf.readAscii(32);

		/* Generating software */
	    to.writeAscii(32, (softwareName + " version 0.1"));// generatingSoftware = braf.readAscii(32);
	    
	    //System.out.println(from.generatingSoftware);
	    //Date now = new Date();     // Gets the current date and time
		int year = Calendar.getInstance().get(Calendar.YEAR); //now.getYear();
		Calendar calendar = Calendar.getInstance();
		int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

		/* File creation date */
	    to.writeUnsignedShort((short)dayOfYear);// = braf.readUnsignedShort();

		/* File creation year */
	    to.writeUnsignedShort((short)(year));// = braf.readUnsignedShort();

		/* Header size */
	    to.writeUnsignedShort((short)headerSize);// = braf.readUnsignedShort();

		//System.out.println((short)headerSize);
		/* Offset to point data */
	    to.writeUnsignedInt((short)headerSize);

		/* #Variable length records*/
	    to.writeUnsignedInt(0);// = braf.readUnsignedInt();

		/* Point data format */
	    to.writeUnsignedByte((byte)pointDataType);// = braf.readUnsignedByte();

		/* Point data record length */
	    to.writeUnsignedShort((short)pointDataRecordLength);// = braf.readUnsignedShort();

		/* Number of point records */
	    to.writeUnsignedInt(0);// = braf.readUnsignedInt();

		/* Number of points by return 0,1 ... 4, 5 */
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);

		/* X scale factor */
	    to.writeDouble(xScale);// = braf.readDouble();

		/* Y scale factor */
	    to.writeDouble(yScale);// = braf.readDouble();

		/* Z scale factor */
	    to.writeDouble(zScale);// = braf.readDouble();

		/* X offset */
	    to.writeDouble(xOff);// = braf.readDouble();

		/* Y offset */
	    to.writeDouble(yOff);// = braf.readDouble();

		/* Z offset */
	    to.writeDouble(zOff);// = braf.readDouble();

		/* Max X */
	    to.writeDouble(0);// = braf.readDouble();
		/* Min X */
	    to.writeDouble(0);// = braf.readDouble();

		/* Max Y */
	    to.writeDouble(0);// = braf.readDouble();
		/* Min Y */
	    to.writeDouble(0);// = braf.readDouble();

		/* Max Z */
	    to.writeDouble(0);// = braf.readDouble();
		/* Min Z */
	    to.writeDouble(0);// = braf.readDouble();

		if(minor == 3){
			to.writeLong(0);
		}

		if(minor == 4){
			to.writeLong(0);
			to.writeLong(0);
			to.writeUnsignedInt(0);
			to.writeLong(0);

			//numberOfPointsByReturn = new long[15];
			for (int i = 0; i < 15; i++) {
				to.writeLong(0);
			}
		}
	    to.writeBuffer2();

	}


	public static synchronized void writeHeader(LASraf to, String softwareName, LASReader p_c, argumentReader aR) throws IOException{


		if(aR.change_point_type == -999)
			to.pointDataRecordFormat = p_c.pointDataRecordFormat;
		else
			to.pointDataRecordFormat = aR.change_point_type;

		to.xScaleFactor = p_c.xScaleFactor;
		to.yScaleFactor = p_c.yScaleFactor;
		to.zScaleFactor = p_c.zScaleFactor;

		to.xOffset = p_c.xOffset;
		to.yOffset = p_c.yOffset;
		to.zOffset = p_c.zOffset;

		to.writeAscii(4, "LASF");

		/* File source ID */
		if(p_c.fileSourceID == 0)
			to.writeUnsignedShort((short)42); // = braf.readUnsignedShort();
		else
			to.writeUnsignedShort((short)p_c.fileSourceID); // = braf.readUnsignedShort();

		/* Global encoding */

		if(aR.change_point_type == 4 || aR.change_point_type == 5 || aR.change_point_type == 9 || aR.change_point_type == 10){

			myBitti2 = setUnsetBit(myBitti2, 15, 0);
			myBitti2 = setUnsetBit(myBitti2, 14, 0);
			myBitti2 = setUnsetBit(myBitti2, 13, 0);
			myBitti2 = setUnsetBit(myBitti2, 12, 0);
			myBitti2 = setUnsetBit(myBitti2, 11, 0);
			myBitti2 = setUnsetBit(myBitti2, 10, 0);
			myBitti2 = setUnsetBit(myBitti2, 9, 0);
			myBitti2 = setUnsetBit(myBitti2, 8, 0);
			myBitti2 = setUnsetBit(myBitti2, 7,0);
			myBitti2 = setUnsetBit(myBitti2, 6, 0);
			myBitti2 = setUnsetBit(myBitti2, 5, 0);
			myBitti2 = setUnsetBit(myBitti2, 4, 0);
			myBitti2 = setUnsetBit(myBitti2, 3, 0);

			/* Set if waveform data in extenral .wpd file. We set this by default
			in order to not trigger a check warning regarding the start of the
			waveform data.
			 */
			myBitti2 = setUnsetBit(myBitti2, 2, 1);

			/* Set if waveform data is in this file. This is deprecated.
			 */
			myBitti2 = setUnsetBit(myBitti2, 1, 0);
			/* This can be read from the input file */
			myBitti2 = setUnsetBit(myBitti2, 0, p_c.globalEncoding==1 ? 1 : 0);
			to.writeUnsignedShort(myBitti2);


		}else
			to.writeUnsignedShort((short)p_c.globalEncoding); // = braf.readUnsignedShort();

		/* ID */
		to.writeLong(0);

		/* GUID */
		to.writeAscii(8, "");

		/* Version major */
		to.writeUnsignedByte((byte)p_c.versionMajor);// = braf.readUnsignedByte();

		/* Version minor */
		if(aR.change_version_minor == -999)
			to.writeUnsignedByte((byte)p_c.versionMinor);// = braf.readUnsignedByte();
		else
			to.writeUnsignedByte((byte)aR.change_version_minor);

		/* System identified */
		to.writeAscii(32, "LASutils (c) by Mikko Kukkonen");// systemIdentifier = braf.readAscii(32);

		/* Generating software */
		to.writeAscii(32, (softwareName + " version 0.1"));// generatingSoftware = braf.readAscii(32);

		//Date now = new Date();     // Gets the current date and time
		int year = Calendar.getInstance().get(Calendar.YEAR); //now.getYear();

		Calendar calendar = Calendar.getInstance();
		int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

		/* File creation date */
		to.writeUnsignedShort((short)dayOfYear);// = braf.readUnsignedShort();

		/* File creation year */
		to.writeUnsignedShort((short)(year));// = braf.readUnsignedShort();

		/* Header size */
		if(aR.change_version_minor == -999)
			to.writeUnsignedShort((short)p_c.headerSize);// = braf.readUnsignedShort();
		else if(aR.change_version_minor == 2){
			to.writeUnsignedShort((short)227);
		}
		else if(aR.change_version_minor == 3){
			to.writeUnsignedShort((short)235);
		}
		else if(aR.change_version_minor == 4){
			to.writeUnsignedShort((short)375);
		}
		/* Offset to point data */
		//to.writeUnsignedInt((short)p_c.offsetToPointData);

		if(aR.change_version_minor == -999)
			to.writeUnsignedInt((short)p_c.offsetToPointData);
		else if(aR.change_version_minor == 2){
			to.writeUnsignedInt((short)227);
		}
		else if(aR.change_version_minor == 3){
			to.writeUnsignedInt((short)235);
		}
		else if(aR.change_version_minor == 4){
			to.writeUnsignedInt((short)375);
		}


		/* #Variable length records*/
		to.writeUnsignedInt(0);// = braf.readUnsignedInt();

		/* Point data format */
		if(aR.change_point_type == -999)
			to.writeUnsignedByte((byte)p_c.pointDataRecordFormat);// = braf.readUnsignedByte();
		else
			to.writeUnsignedByte((byte)aR.change_point_type);

		/* Point data record length */
		if(aR.change_point_type == -999)
			to.writeUnsignedShort((short)p_c.pointDataRecordLength);// = braf.readUnsignedShort();
		else{
			if(aR.change_point_type == 0){
				to.writeUnsignedShort(20);
			}else if(aR.change_point_type== 1){
				to.writeUnsignedShort(20);
			}else if(aR.change_point_type == 2){
				to.writeUnsignedShort(26);
			}else if(aR.change_point_type == 3){
				to.writeUnsignedShort(34);
			}else if(aR.change_point_type == 4){
				to.writeUnsignedShort(57);
			}else if(aR.change_point_type == 5){
				to.writeUnsignedShort(63);
			}else if(aR.change_point_type == 6){
				to.writeUnsignedShort(30);
			}else if(aR.change_point_type == 7){
				to.writeUnsignedShort(36);
			}else if(aR.change_point_type == 8){
				to.writeUnsignedShort(38);
			}else if(aR.change_point_type == 9){
				to.writeUnsignedShort(59);
			}else if(aR.change_point_type == 10){
				to.writeUnsignedShort(67);
			}
		}

		/* Number of point records */
		to.writeUnsignedInt(0);// = braf.readUnsignedInt();

		/* Number of points by return 0,1 ... 4, 5 */
		to.writeUnsignedInt(0);
		to.writeUnsignedInt(0);
		to.writeUnsignedInt(0);
		to.writeUnsignedInt(0);
		to.writeUnsignedInt(0);

		/* X scale factor */
		to.writeDouble(p_c.xScaleFactor);// = braf.readDouble();

		/* Y scale factor */
		to.writeDouble((p_c.yScaleFactor));// = braf.readDouble();

		/* Z scale factor */
		to.writeDouble((p_c.zScaleFactor));// = braf.readDouble();

		/* X offset */
		to.writeDouble((p_c.xOffset));// = braf.readDouble();

		/* Y offset */
		to.writeDouble((p_c.yOffset));// = braf.readDouble();

		/* Z offset */
		to.writeDouble((p_c.zOffset));// = braf.readDouble();

		/* Max X */
		to.writeDouble(0);// = braf.readDouble();
		/* Min X */
		to.writeDouble(0);// = braf.readDouble();

		/* Max Y */
		to.writeDouble(0);// = braf.readDouble();
		/* Min Y */
		to.writeDouble(0);// = braf.readDouble();

		/* Max Z */
		to.writeDouble(0);// = braf.readDouble();
		/* Min Z */
		to.writeDouble(0);// = braf.readDouble();

		if(p_c.versionMinor == 3 || aR.change_version_minor == 3){
			to.writeLong(0);
		}

		if(p_c.versionMinor == 4 || aR.change_version_minor == 4){
			to.writeLong(0);
			to.writeLong(0);
			to.writeUnsignedInt(0);
			to.writeLong(0);

			//numberOfPointsByReturn = new long[15];
			if(aR.change_version_minor == -999)
				for (int i = 0; i < 15; i++) {
					to.writeLong(0);
				}
			else{
				for (int i = 0; i < 15; i++) {
					to.writeLong(0);
				}
			}

		}
		to.writeBuffer2();

	}

	/** Nothing to see here, move on */

	public static void main(String[] args) throws IOException {

	}

}