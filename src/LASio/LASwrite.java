package LASio;


import err.argumentException;
import err.lasFormatException;
import utils.argumentReader;

import java.io.*;
import java.util.*;

public class LASwrite {

	public static Byte myByte = new Byte("00000000");
	public static Byte myByte2 = new Byte("0000000000000000");
	public static byte myBitti = myByte.byteValue();
	public static byte myBitti2 = myByte.byteValue();

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


	public static void updateHeader_txt2las(long pointCount, long[] nPerReturn, double minX, double maxX, double minY, double maxY
			, double maxZ, double minZ, LASraf to,
			int x_offset, int y_offset, int z_offset,
										double x_scale, double y_scale, double z_scale	) throws IOException{


		int lx = (int)((minX - x_offset) / x_scale);
		int ly = (int)((minY - y_offset) / y_scale);
		int lz = (int)((minZ - z_offset) / z_scale);

		double min_x = lx * x_scale + x_offset;
		double min_y = ly * y_scale + y_offset;
		double min_z = lz * z_scale + z_offset;


		lx = (int)((maxX - x_offset) / x_scale);
		ly = (int)((maxY - y_offset) / y_scale);
		lz = (int)((maxZ - z_offset) / z_scale);

		double max_x = lx * x_scale + x_offset;
		double max_y = ly * y_scale + y_offset;
		double max_z = lz * z_scale + z_offset;

		to.writePointCount(pointCount);
		to.writeMinMax(min_x, max_x, min_y, max_y, max_z, min_z);
		to.writePByReturn(nPerReturn);

		to.write_z_offset((double)z_offset);
		to.write_y_offset((double)y_offset);
		to.write_x_offset((double)x_offset);

		to.write_x_scale_factor(x_scale);
		to.write_y_scale_factor(y_scale);
		to.write_z_scale_factor(z_scale);

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

	public static int inferPointDataFormat(String parse){

		int format_out = -1;
		char[] charArray = parse.toCharArray();

		ArrayList<TreeSet<Integer>> minimumFormat = new ArrayList<>();

		for(int i = 0; i < charArray.length; i++) {

			switch (charArray[i]) {
				case 'x':
					/* Irrelevant */
					break;
				case 'y':
					/* Irrelevant */
					break;
				case 'z':
					/* Irrelevant */
					break;
				case 'i':
					/* Irrelevant */
					break;
				case 'c':
					/* Irrelevant */
					break;
				case 't':
					/* This is either 1, 3, 4, 5, 6, 7, 8, 9, 10 */
					minimumFormat.add(new TreeSet<>());
					minimumFormat.get(minimumFormat.size() - 1).add(1);
					minimumFormat.get(minimumFormat.size() - 1).add(3);
					minimumFormat.get(minimumFormat.size() - 1).add(4);
					minimumFormat.get(minimumFormat.size() - 1).add(5);
					minimumFormat.get(minimumFormat.size() - 1).add(6);
					minimumFormat.get(minimumFormat.size() - 1).add(7);
					minimumFormat.get(minimumFormat.size() - 1).add(8);
					minimumFormat.get(minimumFormat.size() - 1).add(9);
					minimumFormat.get(minimumFormat.size() - 1).add(10);
					break;
				case 'n':
					/* Irrelevant */
					break;
				case 'r':
					/* Irrelevant */
					break;
				case 'p':
					/* Irrelevant */
					break;
				case 's':
					/* Irrelevant */
					break;
				case 'u':
					/* Irrelevant */
					break;
				case 'R':
					/* This is either 2, 3, 5, 7, 8, 10 */
					minimumFormat.add(new TreeSet<>());
					minimumFormat.get(minimumFormat.size() - 1).add(2);
					minimumFormat.get(minimumFormat.size() - 1).add(3);
					minimumFormat.get(minimumFormat.size() - 1).add(5);
					minimumFormat.get(minimumFormat.size() - 1).add(7);
					minimumFormat.get(minimumFormat.size() - 1).add(8);
					minimumFormat.get(minimumFormat.size() - 1).add(10);

					break;
				case 'G':
					/* This is either 2, 3, 5, 7, 8, 10 */
					minimumFormat.add(new TreeSet<>());
					minimumFormat.get(minimumFormat.size() - 1).add(2);
					minimumFormat.get(minimumFormat.size() - 1).add(3);
					minimumFormat.get(minimumFormat.size() - 1).add(5);
					minimumFormat.get(minimumFormat.size() - 1).add(7);
					minimumFormat.get(minimumFormat.size() - 1).add(8);
					minimumFormat.get(minimumFormat.size() - 1).add(10);
					break;
				case 'B':
					/* This is either 2, 3, 5, 7, 8, 10 */
					minimumFormat.add(new TreeSet<>());
					minimumFormat.get(minimumFormat.size() - 1).add(2);
					minimumFormat.get(minimumFormat.size() - 1).add(3);
					minimumFormat.get(minimumFormat.size() - 1).add(5);
					minimumFormat.get(minimumFormat.size() - 1).add(7);
					minimumFormat.get(minimumFormat.size() - 1).add(8);
					minimumFormat.get(minimumFormat.size() - 1).add(10);
					break;
				case 'N':
					/* This is either 8, 10 */
					minimumFormat.add(new TreeSet<>());
					minimumFormat.get(minimumFormat.size() - 1).add(8);
					minimumFormat.get(minimumFormat.size() - 1).add(10);
					break;
				default:
					throw new argumentException("-iparse command " + charArray[i] + " not recognized");

			}

		}

		for (int i = 0; i <= 10; i++) {

			boolean all_true = true;

			for(int j = 0; j < minimumFormat.size(); j++){

				if(!minimumFormat.get(j).contains(i)){
					all_true = false;
				}
			}

			if(all_true) {
				format_out = i;
				break;
			}
		}

		if(format_out == -1){
			throw new argumentException("-iparse results in impossible pointFormat");
		}

		return format_out;
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

		for(int i = 0; i < charArray.length; i++){

			//System.out.println(tokens[i]);
			switch(charArray[i])
			{
				case 'x':
					point.x = Double.parseDouble(tokens[i]);
					break;
				case 'y':
					point.y = Double.parseDouble(tokens[i]);
					break;
				case 'z':
					point.z = Double.parseDouble(tokens[i]);
					break;
				case 'i':
					point.intensity = (int)Math.round(Double.parseDouble(tokens[i]));
					break;
				case 'c':
					point.classification = Integer.parseInt(tokens[i]);
					break;
				case 't':
					point.gpsTime = Double.parseDouble(tokens[i]);
					break;
				case 'n':
					point.numberOfReturns = Integer.parseInt(tokens[i]);
					break;
				case 'r':
					point.returnNumber = Integer.parseInt(tokens[i]);
					break;
				case 'p':
					point.pointSourceId = (short)Integer.parseInt(tokens[i]);
					break;
				case 'd':
					point.scanAngleRank = Integer.parseInt(tokens[i]);
					break;
				case 'u':
					point.userData = Integer.parseInt(tokens[i]);
					break;
				case 'R':
					point.R = (short)Double.parseDouble(tokens[i]);
					break;
				case 'G':
					point.G = (short)Double.parseDouble(tokens[i]);
					break;
				case 'B':
					point.B = (short)Double.parseDouble(tokens[i]);
					break;
				case 'N':
					point.N = (short)Double.parseDouble(tokens[i]);
					break;
				case 's':
					/* SKIP */
					break;
				default:
					throw new argumentException("-iparse command " + charArray[i] + " not recognized");
			}
		}
	}

	public static int getPointDataRecordLength(int pointDataRecordFormat){

		if(pointDataRecordFormat == 0){
			return 20;
		}else if(pointDataRecordFormat == 1){
			return 28;
		}else if(pointDataRecordFormat == 2){
			return 26;
		}else if(pointDataRecordFormat == 3){
			return 34;
		}else if(pointDataRecordFormat == 4){
			return 57;
		}else if(pointDataRecordFormat == 5){
			return 63;
		}else if(pointDataRecordFormat == 6){
			return 30;
		}else if(pointDataRecordFormat == 7){
			return 36;
		}else if(pointDataRecordFormat == 8){
			return 38;
		}else if(pointDataRecordFormat == 9){
			return 59;
		}else if(pointDataRecordFormat == 10){
			return 67;
		}

		if(true)
			throw new lasFormatException("WHAT HAPPENED!!");

		return -1;
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

	public static void txt2las(File from, LASraf to, String parse, String softwareName, String sep, PointInclusionRule rule, boolean echoClass, argumentReader aR) throws IOException{

		/* First just create a placeholder header. Minimum header requirements will be updated
			with the information inferred from the point records.
		 */

		byte myBitti = myByte.byteValue();

		int minimum_point_format = inferPointDataFormat(parse);
		to.pointDataRecordFormat = minimum_point_format;
		int minimum_version = -1;

		if(minimum_point_format <= 3){
			minimum_version = 2;
		}else if(minimum_point_format <= 5){
			minimum_version = 3;
		}else{
			minimum_version = 4;
		}

		String line = "";
		LasPoint tempPoint = new LasPoint();

		int x_offset_update = 0, y_offset_update = 0, z_offset_update = 0;
		double x_scale_update = 0, y_scale_update = 0, z_scale_update = 0;

		try {
			FileInputStream fis = new FileInputStream(from);
			BufferedReader in = new BufferedReader(new InputStreamReader(fis));

			while ((line = in.readLine()) != null) {

				String2LASpoint(tempPoint, line, parse, sep);

				x_offset_update = (int)tempPoint.x;
				y_offset_update = (int)tempPoint.y;
				z_offset_update = (int)tempPoint.z;


				x_scale_update = 0.01;
				y_scale_update = 0.01;
				z_scale_update = 0.01;

			}

		}catch (Exception e){
			e.printStackTrace();
		}

		to.writeAscii(4, "LASF");
	    to.writeUnsignedShort((short)2); // = braf.readUnsignedShort();
	    to.writeUnsignedShort((short)1); // = braf.readUnsignedShort();

	    // GUID
	    to.writeLong(0);
	    to.writeAscii(8, "");
	    to.writeUnsignedByte((byte)1);// = braf.readUnsignedByte();
	    to.writeUnsignedByte((byte)minimum_version);// = braf.readUnsignedByte();
	    to.writeAscii(32, "LASutils (c) by Mikko Kukkonen");// systemIdentifier = braf.readAscii(32);
	    to.writeAscii(32, (softwareName + " version 0.1"));// generatingSoftware = braf.readAscii(32);
		int year = Calendar.getInstance().get(Calendar.YEAR); //now.getYear();
		Calendar calendar = Calendar.getInstance();
		int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
	    to.writeUnsignedShort((short)dayOfYear);// = braf.readUnsignedShort();
	    to.writeUnsignedShort((short)(year));// = braf.readUnsignedShort();

		if(minimum_version == 2){
			to.writeUnsignedShort((short)227);// = braf.readUnsignedShort();
			to.writeUnsignedInt(227);
		}
		else if(minimum_version == 3){
			to.writeUnsignedShort((short)335);// = braf.readUnsignedShort();
			to.writeUnsignedInt(335);

		}else if(minimum_version == 4){
			to.writeUnsignedShort((short)375);// = braf.readUnsignedShort();
			to.writeUnsignedInt(375);
		}

	    to.writeUnsignedInt(0);// = braf.readUnsignedInt();
	    to.writeUnsignedByte((byte)minimum_point_format);// = braf.readUnsignedByte();
	    to.writeUnsignedShort((short)getPointDataRecordLength(minimum_point_format));// = braf.readUnsignedShort();
	    to.writeUnsignedInt(0);// = braf.readUnsignedInt();
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);
	   	to.writeUnsignedInt(0);

	    to.writeDouble(0.01);// = braf.readDouble();
	    to.writeDouble(0.01);// = braf.readDouble();
	    to.writeDouble(0.01);// = braf.readDouble();
	    
	    to.writeDouble(x_offset_update);// = braf.readDouble();
	    to.writeDouble(y_offset_update);// = braf.readDouble();
	    to.writeDouble(z_offset_update);// = braf.readDouble();
		//System.out.println("Z_OFFSET: " + z_offset_update);
		//System.exit(1);
		to.xOffset = x_offset_update; to.yOffset = y_offset_update; to.zOffset = z_offset_update;
		to.xScaleFactor = 0.01; to.yScaleFactor = 0.01; to.zScaleFactor = 0.01;
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
	    to.writeDouble(0);// = braf.readDouble();
		System.out.println(minimum_version);
		if(minimum_version == 3){
			to.writeDouble(0);
		}
		if(minimum_version == 4){
			to.writeDouble(0);
			to.writeDouble(0);
			to.writeUnsignedInt(0);
			to.writeDouble(0);
			to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);
			to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);
			to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);
			to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);
			to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);
			to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);to.writeUnsignedInt(0);
		}

	    to.writeBuffer2();
	    long n = 0;//from.getNumberOfPointRecords();

		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;

		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		double minZ = Double.POSITIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		long pointCount = 0;

		long[] pointsByReturn = new long[5];
		long[] pointsByReturn_1_4 = new long[15];

		//LasPoint tempPoint = new LasPoint();

		double offSet = 0.0;
		double scaleFactor = 0.01;

		int count = 0;


		/* UPDATE THESE HEADER INFORMATION */

		int version_minor_update = 2;
		int point_data_format_update = 0;


		try {
	        FileInputStream fis = new FileInputStream(from);
	        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

	        while((line = in.readLine())!= null){

	        	String2LASpoint(tempPoint, line, parse, sep);

				if(!aR.inclusionRule.ask(tempPoint, 1, true)){
					continue;
				}

				if(tempPoint.numberOfReturns > 5){
					version_minor_update = 4;
				}

				/* Infer scale and offset from the first point */
				if(x_scale_update == 0){

					x_offset_update = (int)tempPoint.x;
					y_offset_update = (int)tempPoint.y;
					z_offset_update = (int)tempPoint.z;

					x_scale_update = 0.01;
					y_scale_update = 0.01;
					z_scale_update = 0.01;

				}

				if(echoClass){

					//tempPoint.userData = tempPoint.numberOfReturns;
					/*
					#   0 = only

					#   1 = first of many
					#   2 = intermediate

   					#   3 = last of many
							*/
					//System.out.println("HERE!!");

					//System.out.println(tempPoint.numberOfReturns + " " + tempPoint.returnNumber);

					if(tempPoint.numberOfReturns == 0){
						tempPoint.numberOfReturns = 1;
						tempPoint.returnNumber = 1;
					}
					else if(tempPoint.numberOfReturns == 1){
						tempPoint.numberOfReturns = 2;
						tempPoint.returnNumber = 1;
					}
					else if(tempPoint.numberOfReturns == 2){
						tempPoint.numberOfReturns = 3;
						tempPoint.returnNumber = 2;
					}
					else if(tempPoint.numberOfReturns == 3){
						tempPoint.numberOfReturns = 2;
						tempPoint.returnNumber = 2;
					}
					else{

					}

				}


				if(tempPoint.returnNumber <= 0 || tempPoint.numberOfReturns <= 0){

					System.exit(1);
					continue;



				}

				if(rule.ask(tempPoint, count, false)){

					pointCount++;
					
					int returnNumberi = tempPoint.returnNumber - 1;

					if(returnNumberi >= 0 && returnNumberi < 5)
						pointsByReturn[returnNumberi]++;
					if(returnNumberi >= 0 && returnNumberi < 15)
						pointsByReturn_1_4[returnNumberi]++;

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

					int lx = (int)((x - x_offset_update) / x_scale_update);
		    		int ly = (int)((y - y_offset_update) / y_scale_update);
		    		int lz = (int)((z - z_offset_update) / z_scale_update);

					if(minimum_point_format <= 5) {


						to.writeInt(lx);
						to.writeInt(ly);
						to.writeInt(lz);

						to.writeUnsignedShort((short) tempPoint.intensity);

						myBitti = setUnsetBit(myBitti, 7, tempPoint.edgeOfFlightLine ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 6, tempPoint.scanDirectionFlag);
						myBitti = setUnsetBit(myBitti, 5, ((byte) tempPoint.numberOfReturns & (1 << (2))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 4, ((byte) tempPoint.numberOfReturns & (1 << (1))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 3, ((byte) tempPoint.numberOfReturns & (1 << (0))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 2, ((byte) tempPoint.returnNumber & (1 << (2))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 1, ((byte) tempPoint.returnNumber & (1 << (1))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 0, ((byte) tempPoint.returnNumber & (1 << (0))) > 0 ? 1 : 0);

						to.writeUnsignedByte(myBitti);

						myBitti = setUnsetBit(myBitti, 7, (tempPoint.withheld) ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 6, (tempPoint.keypoint) ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 5, (tempPoint.synthetic) ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 4, ((byte) tempPoint.classification & (1 << (4))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 3, ((byte) tempPoint.classification & (1 << (3))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 2, ((byte) tempPoint.classification & (1 << (2))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 1, ((byte) tempPoint.classification & (1 << (1))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 0, ((byte) tempPoint.classification & (1 << (0))) > 0 ? 1 : 0);

						to.writeUnsignedByte(myBitti);

						/* Write scan angle */
						to.writeUnsignedByte((byte) tempPoint.scanAngleRank);

						/* Write user data */
						to.writeUnsignedByte((byte) tempPoint.userData);

						/* Write point source ID */
						to.writeUnsignedShort(tempPoint.pointSourceId);

						/* RGB is included in both 2 and 3 record types in LAS 1.2 */

						if (minimum_point_format == 1 ||
								minimum_point_format == 3 ||
								minimum_point_format == 4 ||
								minimum_point_format == 5) {

							to.writeDouble(tempPoint.gpsTime);// = braf.readDouble();

						}

						if (minimum_point_format == 2 ||
								minimum_point_format == 3 ||
								minimum_point_format == 5) {

							to.writeUnsignedShort(tempPoint.R);
							to.writeUnsignedShort(tempPoint.G);
							to.writeUnsignedShort(tempPoint.B);

						}

						if (minimum_point_format == 4 ||
								minimum_point_format == 5) {

							to.writeUnsignedByte((byte) tempPoint.WavePacketDescriptorIndex);
							to.writeLong(tempPoint.ByteOffsetToWaveformData);
							to.writeUnsignedInt(tempPoint.WaveformPacketSizeInBytes);
							to.writeFloat(tempPoint.ReturnPointWaveformLocation);

							to.writeFloat(tempPoint.x_t);
							to.writeFloat(tempPoint.y_t);
							to.writeFloat(tempPoint.z_t);
						}
					}else {

						/* Write scaled and offset x, y and z */
						to.writeInt(lx);
						to.writeInt(ly);
						to.writeInt(lz);

						to.writeUnsignedShort((short) tempPoint.intensity);// braf.readUnsignedShort()

                /*
				Return Number 3 bits 					(bits 0, 1, 2, 3) 4 bits *
				Number of Returns (given pulse) 3 bits 	(bits 4, 5, 6, 7) 4 bits *
			 */

						myBitti = setUnsetBit(myBitti, 7, ((byte) tempPoint.numberOfReturns & (1 << (3))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 6, ((byte) tempPoint.numberOfReturns & (1 << (2))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 5, ((byte) tempPoint.numberOfReturns & (1 << (1))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 4, ((byte) tempPoint.numberOfReturns & (1 << (0))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 3, ((byte) tempPoint.returnNumber & (1 << (3))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 2, ((byte) tempPoint.returnNumber & (1 << (2))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 1, ((byte) tempPoint.returnNumber & (1 << (1))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 0, ((byte) tempPoint.returnNumber & (1 << (0))) > 0 ? 1 : 0);

						int mask = myBitti;

						int numberOfReturns_ = mask>>4;

						int returnNumber_ = mask&15;

						if(tempPoint.numberOfReturns != numberOfReturns_ || returnNumber_ != tempPoint.returnNumber){
							System.out.println("HATHAFHSAFD: !" );
							System.out.println(numberOfReturns_ + " " + tempPoint.numberOfReturns);
							System.out.println(returnNumber_ + " " + tempPoint.returnNumber);
						}


						/* Write byte */
						to.writeUnsignedByte(myBitti);

                /*

			Bits 	Explanation

			0 - 3 	Classification Flags: Classification flags are used to indicate special characteristics associated
                    with the point.
			4 - 5   Scanner Channel
            6       Scan Direction Flag
            7       Edge of Flight Line
			*/




						myBitti = setUnsetBit(myBitti, 7, (tempPoint.edgeOfFlightLine) ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 6, (tempPoint.scanDirectionFlag == 1) ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 5, ((byte) tempPoint.scannerCannel & (1 << (7))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 4, ((byte) tempPoint.scannerCannel & (1 << (6))) > 0 ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 3, (tempPoint.overlap) ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 2, (tempPoint.withheld) ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 1, (tempPoint.keypoint) ? 1 : 0);
						myBitti = setUnsetBit(myBitti, 0, (tempPoint.synthetic) ? 1 : 0);


						/* Write byte */
						to.writeUnsignedByte(myBitti);

						/* Write classification */
						to.writeUnsignedByte((byte) tempPoint.classification);

						/* Write user data */
						to.writeUnsignedByte((byte) tempPoint.userData);

						/* Write scan angle */
						to.writeUnsignedShort((byte) tempPoint.scanAngleRank);

						/* Write pointSourceId */
						to.writeUnsignedShort((byte) tempPoint.pointSourceId);

						/* Write gps time */
						to.writeDouble(tempPoint.gpsTime);


						if (minimum_point_format >= 7 && minimum_point_format != 9) {

							to.writeUnsignedShort(tempPoint.R);
							to.writeUnsignedShort(tempPoint.G);
							to.writeUnsignedShort(tempPoint.B);

							if (minimum_point_format >= 8) {
								to.writeUnsignedShort(tempPoint.N);
							}
						}
						if (minimum_point_format == 10 || minimum_point_format == 9) {

							to.writeUnsignedByte((byte) tempPoint.WavePacketDescriptorIndex);
							to.writeLong(tempPoint.ByteOffsetToWaveformData);
							to.writeUnsignedInt(tempPoint.WaveformPacketSizeInBytes);
							to.writeFloat(tempPoint.ReturnPointWaveformLocation);

							to.writeFloat(tempPoint.x_t);
							to.writeFloat(tempPoint.y_t);
							to.writeFloat(tempPoint.z_t);

						}
					}
				}
				count++;
	        }
	        in.close();
	    }catch(Exception e){
			e.printStackTrace();
		}
	    to.writeBuffer2();

		if(minimum_version == 4){
			to.updateHeader_1_4_2((int)pointCount, minX, maxX, minY, maxY
					, maxZ, minZ, pointsByReturn, pointsByReturn_1_4,  aR, x_offset_update, y_offset_update, z_offset_update,
					x_scale_update, y_scale_update, z_scale_update);
		}else
			updateHeader_txt2las(pointCount, pointsByReturn, minX, maxX, minY, maxY
				, maxZ, minZ, to, x_offset_update, y_offset_update, z_offset_update,
					x_scale_update, y_scale_update, z_scale_update);

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
		if(rule.ask(tempPoint, i, false)){

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


			myBitti = setUnsetBit(myBitti, 7, tempPoint.edgeOfFlightLine ? 1 : 0);
			myBitti = setUnsetBit(myBitti, 6, tempPoint.scanDirectionFlag);
			myBitti = setUnsetBit(myBitti, 5, (byte)tempPoint.numberOfReturns >> 2);
			myBitti = setUnsetBit(myBitti, 4, (byte)tempPoint.numberOfReturns >> 1);
			myBitti = setUnsetBit(myBitti, 3, (byte)tempPoint.numberOfReturns >> 0);
			myBitti = setUnsetBit(myBitti, 2, (byte)tempPoint.returnNumber >> 2);
			myBitti = setUnsetBit(myBitti, 1, (byte)tempPoint.returnNumber >> 1);
			myBitti = setUnsetBit(myBitti, 0, (byte)tempPoint.returnNumber >> 0);

			/* Write byte */
			to.writeUnsignedByte(myBitti);

			myBitti = setUnsetBit(myBitti, 7, (tempPoint.withheld) ? 1 : 0);
			myBitti = setUnsetBit(myBitti, 6, (tempPoint.keypoint) ? 1 : 0);
			myBitti = setUnsetBit(myBitti, 5, (tempPoint.synthetic) ? 1 : 0);
			myBitti = setUnsetBit(myBitti, 4, (byte)tempPoint.classification >> 4);
			myBitti = setUnsetBit(myBitti, 3, (byte)tempPoint.classification >> 3);
			myBitti = setUnsetBit(myBitti, 2, (byte)tempPoint.classification >> 2);
			myBitti = setUnsetBit(myBitti, 1, (byte)tempPoint.classification >> 1);
			myBitti = setUnsetBit(myBitti, 0, (byte)tempPoint.classification >> 0);

			/* Write the byte */
			to.writeUnsignedByte(myBitti);

			/* Write scan angle */
			to.writeUnsignedByte((byte)tempPoint.scanAngleRank);

			/* Write user data */
			to.writeUnsignedByte((byte)tempPoint.userData);

			/* Write point source ID */
			to.writeUnsignedShort(tempPoint.pointSourceId);

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

		int year = Calendar.getInstance().get(Calendar.YEAR); //now.getYear();
		Calendar calendar = Calendar.getInstance();
		int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

		/* File creation date */
	    to.writeUnsignedShort((short)dayOfYear);// = braf.readUnsignedShort();

		/* File creation year */
	    to.writeUnsignedShort((short)(year));// = braf.readUnsignedShort();

		/* Header size */
	    to.writeUnsignedShort((short)headerSize);// = braf.readUnsignedShort();

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

			for (int i = 0; i < 15; i++) {
				to.writeLong(0);
			}
		}
	    to.writeBuffer2();

	}


	/**
	 * Copies the header data from one las file (the input file) to another (the output file).
	 *
	 * @param to
	 * @param softwareName
	 * @param from
	 * @param aR
	 * @throws IOException
	 */
	public static synchronized void writeHeader(LASraf to, String softwareName, LASReader from, argumentReader aR) throws IOException{


		if(aR.change_point_type == -999)
			to.pointDataRecordFormat = from.pointDataRecordFormat;
		else
			to.pointDataRecordFormat = aR.change_point_type;

		to.xScaleFactor = from.xScaleFactor;
		to.yScaleFactor = from.yScaleFactor;
		to.zScaleFactor = from.zScaleFactor;

		to.xOffset = from.xOffset;
		to.yOffset = from.yOffset;
		to.zOffset = from.zOffset;

		to.writeAscii(4, "LASF");

		/* File source ID */
		if(from.fileSourceID == 0)
			to.writeUnsignedShort((short)42); // = braf.readUnsignedShort();
		else
			to.writeUnsignedShort((short)from.fileSourceID); // = braf.readUnsignedShort();

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
			myBitti2 = setUnsetBit(myBitti2, 0, from.globalEncoding==1 ? 1 : 0);
			to.writeUnsignedShort(myBitti2);


		}else
			to.writeUnsignedShort((short)from.globalEncoding); // = braf.readUnsignedShort();

		/* ID */
		to.writeLong(0);

		/* GUID */
		to.writeAscii(8, "");

		/* Version major */
		to.writeUnsignedByte((byte)from.versionMajor);// = braf.readUnsignedByte();

		/* Version minor */
		if(aR.change_version_minor == -999)
			to.writeUnsignedByte((byte)from.versionMinor);// = braf.readUnsignedByte();
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
		short header_size = (short)from.headerSize;
		short header_size_difference = 0;

		if(aR.change_version_minor == -999)
			header_size = (short)from.headerSize;

		else if(aR.change_version_minor == 2){
			header_size = 227;

		}
		else if(aR.change_version_minor == 3){
			header_size = (short)235;
		}
		else if(aR.change_version_minor == 4){
			header_size = (short)375;
		}

		header_size_difference = (short)(header_size - from.headerSize);

		to.writeUnsignedShort(header_size);

		/* Offset to point data */
		int add_bytes_as_vlr = 0;

		if(aR.create_extra_byte_vlr_n_bytes.size() > 0 && from.containsExtraBytes) {

			add_bytes_as_vlr = 192 * aR.create_extra_byte_vlr_n_bytes.size();

		}else if(aR.create_extra_byte_vlr_n_bytes.size() > 0 && !from.containsExtraBytes){
			add_bytes_as_vlr = 192  * aR.create_extra_byte_vlr_n_bytes.size()  + 54;
		}

		if(aR.change_version_minor == -999){
			to.writeUnsignedInt((short)from.offsetToPointData + add_bytes_as_vlr + header_size_difference);
		}
		else if(aR.change_version_minor == 2){
			to.writeUnsignedInt((short)from.offsetToPointData  + add_bytes_as_vlr + header_size_difference);
		}
		else if(aR.change_version_minor == 3){
			to.writeUnsignedInt((short)from.offsetToPointData  + add_bytes_as_vlr + header_size_difference);
		}
		else if(aR.change_version_minor == 4){
			to.writeUnsignedInt((short)from.offsetToPointData  + add_bytes_as_vlr + header_size_difference);
		}

		/* #Variable length records*/
		if(aR.create_extra_byte_vlr_n_bytes.size() > 0 && !from.containsExtraBytes) {
			//System.out.println("ERE!");
			to.writeUnsignedInt((int)from.numberVariableLengthRecords + 1);
		}
		else
			to.writeUnsignedInt((int)from.numberVariableLengthRecords);// = braf.readUnsignedInt();

		/* Point data format */
		if(aR.change_point_type == -999)
			to.writeUnsignedByte((byte)from.pointDataRecordFormat);// = braf.readUnsignedByte();
		else
			to.writeUnsignedByte((byte)aR.change_point_type);

		int add_bytes = 0;

		if(aR.create_extra_byte_vlr_n_bytes.size() > 0) {
			for(int i = 0; i < aR.create_extra_byte_vlr_n_bytes.size(); i++)
				add_bytes += aR.create_extra_byte_vlr_n_bytes.get(i);
		}

		/* Point data record length */
		if(aR.change_point_type == -999) {
			to.writeUnsignedShort((short) from.pointDataRecordLength + add_bytes);// = braf.readUnsignedShort();
			to.pointDataRecordLength = (short) from.pointDataRecordLength + add_bytes;
		}
		else{
			if(aR.change_point_type == 0){
				to.writeUnsignedShort(20 + add_bytes);
				to.pointDataRecordLength = 20 + add_bytes;

			}else if(aR.change_point_type== 1){
				to.writeUnsignedShort(20 + add_bytes);
				to.pointDataRecordLength = 20 + add_bytes;
			}else if(aR.change_point_type == 2){
				to.writeUnsignedShort(26 + add_bytes);
				to.pointDataRecordLength = 26 + add_bytes;
			}else if(aR.change_point_type == 3){
				to.writeUnsignedShort(34 + add_bytes);
				to.pointDataRecordLength = 34 + add_bytes;
			}else if(aR.change_point_type == 4){
				to.writeUnsignedShort(57 + add_bytes);
				to.pointDataRecordLength = 57 + add_bytes;
			}else if(aR.change_point_type == 5){
				to.writeUnsignedShort(63 + add_bytes);
				to.pointDataRecordLength = 63 + add_bytes;
			}else if(aR.change_point_type == 6){
				to.writeUnsignedShort(30 + add_bytes);
				to.pointDataRecordLength = 30 + add_bytes;
			}else if(aR.change_point_type == 7){
				to.writeUnsignedShort(36 + add_bytes);
				to.pointDataRecordLength = 36 + add_bytes;
			}else if(aR.change_point_type == 8){
				to.writeUnsignedShort(38 + add_bytes);
				to.pointDataRecordLength = 38 + add_bytes;
			}else if(aR.change_point_type == 9){
				to.writeUnsignedShort(59 + add_bytes);
				to.pointDataRecordLength = 59 + add_bytes;
			}else if(aR.change_point_type == 10){
				to.writeUnsignedShort(67 + add_bytes);
				to.pointDataRecordLength = 67 + add_bytes;
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
		to.writeDouble(from.xScaleFactor);// = braf.readDouble();

		/* Y scale factor */
		to.writeDouble((from.yScaleFactor));// = braf.readDouble();

		/* Z scale factor */
		to.writeDouble((from.zScaleFactor));// = braf.readDouble();

		/* X offset */
		to.writeDouble((from.xOffset));// = braf.readDouble();

		/* Y offset */
		to.writeDouble((from.yOffset));// = braf.readDouble();

		/* Z offset */
		to.writeDouble((from.zOffset));// = braf.readDouble();

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

		if(from.versionMinor == 3 || aR.change_version_minor == 3){
			to.writeLong(0);
		}

		if(from.versionMinor == 4 || aR.change_version_minor == 4){
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
		boolean added_to_existing = false;

		int n_added_bytes = 0;

		for(int i = 0; i < from.vlrList.size(); i++){

			/* UNUSED */
			to.writeUnsignedByte((byte)0);
			to.writeUnsignedByte((byte)0);


			to.writeAscii(16, from.vlrList.get(i).userId);
			/* Predefined id for extrabytes */
			to.writeUnsignedShort(from.vlrList.get(i).recordId);
			/* Record length */

			if(aR.create_extra_byte_vlr_n_bytes.size() > 0 && from.vlrList.get(i).userId.compareTo("LASF_Spec") == 0 && from.vlrList.get(i).recordId == 4){
				to.writeUnsignedShort(from.vlrList.get(i).recordLength + 192 * aR.create_extra_byte_vlr_n_bytes.size());
			}
			else
				to.writeUnsignedShort(from.vlrList.get(i).recordLength);

			to.writeAscii(32, from.vlrList.get(i).description);

			from.seekVlr(from.vlrList.get(i).offset);

			if(from.vlrList.get(i).userId.compareTo("LASF_Spec") == 0 && from.vlrList.get(i).recordId == 4){

				int numberOfExtraRecords = from.vlrList.get(i).recordLength / 192;

				for(int rec = 0; rec < numberOfExtraRecords; rec++) {

					from.seekVlr(from.vlrList.get(i).offset + rec * 192);

					from.braf.readUnsignedByte();
					from.braf.readUnsignedByte();

					/* DATA TYPE */
					int data_type = from.braf.readUnsignedByte();
					to.extra_bytes.add(data_type_to_n_bytes(data_type));

					/* OPTIONS */

					from.braf.readUnsignedByte();

					/* NAME */
					from.braf.readAscii(32);

					/* UNUSED */

					from.braf.readInt();

					/* no_data[3] */
					from.braf.readDouble();
					from.braf.readDouble();
					from.braf.readDouble();
					/* min[3]; */
					from.braf.readDouble();
					from.braf.readDouble();
					from.braf.readDouble();
					/* max[3]*/
					from.braf.readDouble();
					from.braf.readDouble();
					from.braf.readDouble();
					/* scale[3]; */
					from.braf.readDouble();
					from.braf.readDouble();
					from.braf.readDouble();
					/* offset[3];*/
					from.braf.readDouble();
					from.braf.readDouble();
					from.braf.readDouble();

					/* description[32]; */
					from.braf.readAscii(32);

					from.seekVlr(from.vlrList.get(i).offset + rec * 192);

					for (int i_ = 0; i_ < 192; i_++)
						to.writeUnsignedByte(from.readByte());

				}

				if(aR.create_extra_byte_vlr_n_bytes.size() > 0){

					int from_bytes = to.allArray2Index;

					for(int i_ = 0; i_ < aR.create_extra_byte_vlr_n_bytes.size(); i_++) {
						added_to_existing = true;

						/* UNUSED */
						to.writeUnsignedByte((byte) 0);
						to.writeUnsignedByte((byte) 0);

						/* DATA TYPE */
						to.writeUnsignedByte(aR.create_extra_byte_vlr.get(i_).byteValue());
						//to.extra_bytes.add(data_type_to_n_bytes((byte)aR.create_extra_byte_vlr));
						/* OPTIONS */

						to.writeUnsignedByte((byte) 0);

						/* NAME */
						to.writeAscii(32, aR.create_extra_byte_vlr_name.get(i_));

						/* UNUSED */
						to.writeInt(0);

						/* no_data[3] */
						to.writeDouble(0);
						to.writeDouble(0);
						to.writeDouble(0);
						/* min[3]; */
						to.writeDouble(0);
						to.writeDouble(0);
						to.writeDouble(0);
						/* max[3]*/
						to.writeDouble(0);
						to.writeDouble(0);
						to.writeDouble(0);
						/* scale[3]; */
						to.writeDouble(0);
						to.writeDouble(0);
						to.writeDouble(0);
						/* offset[3];*/
						to.writeDouble(0);
						to.writeDouble(0);
						to.writeDouble(0);

						/* description[32]; */
						to.writeAscii(32, aR.create_extra_byte_vlr_description.get(i_));
					}

					n_added_bytes += to.allArray2Index - from_bytes;
				}
			}
			else{
				for(int i_ = 0; i_ < from.vlrList.get(i).recordLength; i_++)
					to.writeUnsignedByte(from.readByte());
			}


		}

		/* If the tool requires to save data for each point
		* and no extra byte VLR exists */
		if(aR.create_extra_byte_vlr.size() > 0 && !added_to_existing){

			int from_bytes = to.allArray2Index;

			/* UNUSED */
			to.writeUnsignedByte((byte)0);
			to.writeUnsignedByte((byte)0);

			String userId = "LASF_Spec";
			to.writeAscii(16, userId);
			/* Predefined id for extrabytes */
			to.writeUnsignedShort(4);
			/* Record length */
			to.writeUnsignedShort(aR.create_extra_byte_vlr.size() * 192);
			String description = "LASutils_extrabytes";
			to.writeAscii(32, description);

			{
				for(int i_ = 0; i_ < aR.create_extra_byte_vlr.size(); i_++) {
					/* UNUSED */
					to.writeUnsignedByte((byte) 0);
					to.writeUnsignedByte((byte) 0);

					/* DATA TYPE */
					to.writeUnsignedByte(aR.create_extra_byte_vlr.get(i_).byteValue());

					/* OPTIONS */

					to.writeUnsignedByte((byte) 0);

					/* NAME */
					to.writeAscii(32, aR.create_extra_byte_vlr_name.get(i_));

					/* UNUSED */
					to.writeInt(0);

					/* no_data[3] */
					to.writeDouble(0);
					to.writeDouble(0);
					to.writeDouble(0);
					/* min[3]; */
					to.writeDouble(0);
					to.writeDouble(0);
					to.writeDouble(0);
					/* max[3]*/
					to.writeDouble(0);
					to.writeDouble(0);
					to.writeDouble(0);
					/* scale[3]; */
					to.writeDouble(0);
					to.writeDouble(0);
					to.writeDouble(0);
					/* offset[3];*/
					to.writeDouble(0);
					to.writeDouble(0);
					to.writeDouble(0);

					/* description[32]; */
					to.writeAscii(32, aR.create_extra_byte_vlr_description.get(i_));
				}
			}

			to.extra_bytes.add(add_bytes);

			n_added_bytes += to.allArray2Index - from_bytes;
		}

		while(to.allArray2Index < from.offsetToPointData + n_added_bytes){
			to.writeUnsignedByte((byte)0);
		}

		to.writeBuffer2();

		//to.setUpMappedByteBuffer();
	}

	public static int data_type_to_n_bytes(int data_type){

		int returni = -1;

		switch (data_type){

			case 0:
				returni = -1;
				break;

			case 1:
				returni = 1;
				break;

			case 2:
				returni = 1;
				break;

			case 3:
				returni = 2;
				break;

			case 4:
				returni = 2;
				break;

			case 5:
				returni = 4;
				break;

			case 6:
				returni = 4;
				break;

			case 7:
				returni = 8;
				break;

			case 8:
				returni = 8;
				break;

			case 9:
				returni = 4;
				break;

			case 10:
				returni =  8;
				break;

			case 11:
				returni = 2;
				break;

			case 12:
				returni =  2;
				break;

			case 13:
				returni = 4;
				break;

			case 14:
				returni = 4;
				break;

			case 15:
				returni = 8;
				break;

			case 16:
				returni = 8;
				break;

			case 17:
				returni = 16;
				break;

			case 18:
				returni = 8;
				break;

			case 19:
				returni = 16;
				break;

			case 20:
				returni = 8;
				break;

			case 21:
				returni = 3;
				break;

			case 22:
				returni = 3;
				break;
			case 23:
				returni = 6;
				break;
			case 24:
				returni = 6;
				break;
			case 25:
				returni = 12;
				break;
			case 26:
				returni = 12;
				break;
			case 27:
				returni = 24;
				break;

			case 28:
				returni =  24;
				break;
			case 29:
				returni =  12;
				break;
			case 30:
				returni = 24;
				break;

		}
		return returni;

	}

	/** Nothing to see here, move on */

	public static void main(String[] args) throws IOException {

	}

}