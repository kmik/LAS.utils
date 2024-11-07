import LASio.*;
import err.lasFormatException;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static LASio.LASwrite.String2LASpoint__;
import static LASio.LASwrite.setUnsetBit;
import LASio.LASraf;
import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;
import org.smurn.jply.Element;
import org.smurn.jply.ElementReader;
import org.smurn.jply.ElementType;
import org.smurn.jply.PlyReader;
import org.smurn.jply.PlyReaderFile;

public class ply2las {

    public static Byte myByte = new Byte("00000000");

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if(aR.cores > 1){
            threadTool(aR, fD);
        }else{

            //process_las2las tooli = new process_las2las(1);


            for (int i = 0; i < inputFiles.size(); i++) {


                File fromFile = new File(inputFiles.get(i).getAbsolutePath());
                System.out.println(fromFile.getParent());

                File toFile = null;

                toFile = aR.createOutputFileWithExtension(fromFile, ".las");

                LASraf asd2 = new LASraf(toFile);

                ply_2_las(fromFile, asd2, "xyz", "ply2las", " ", new PointInclusionRule(), false, aR);

                //asd2.writeBuffer2();
                asd2.close();

                /*
                while (reader != null) {

                    ElementType type = reader.getElementType();

                    // In PLY files vertices always have a type named "vertex".
                    if (type.getName().equals("vertex")) {
                        printVertices(reader);
                    }

                    temp.x = reader.readElement().getDouble("x");
                    temp.y = reader.readElement().getDouble("y");
                    temp.z = reader.readElement().getDouble("z");


                    // Close the reader for the current type before getting the next one.
                    reader.close();

                    reader = ply.nextElementReader();
                }


                try {

                    //tooli.convert(temp, aR);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                }
                 */
            }
        }

        aR.cleanup();

    }

    public static void ply_2_las(File infile, LASraf to, String parse, String softwareName, String sep, PointInclusionRule rule, boolean echoClass, argumentReader aR) throws IOException{

		/* First just create a placeholder header. Minimum header requirements will be updated
			with the information inferred from the point records.
		 */
        PlyReader ply;



        LasPoint temp = new LasPoint();

        ply = new PlyReaderFile(infile);

        ElementReader reader = ply.nextElementReader();

        int add_bytes_as_vlr = 0;

        char[] charArray = parse.toCharArray();

        int numberOfExtraBytes = 0;



        for(char c : charArray){
            if(c - '0' == 1){
                aR.add_extra_bytes(9, aR.extraByteNames.get(0), "blank");
            }
            if(c - '0' == 2){
                aR.add_extra_bytes(6, aR.extraByteNames.get(1), "blank");
            }

            if(c - '0' == 3){
                aR.add_extra_bytes(6, aR.extraByteNames.get(2), "blank");
            }

            if(c - '0' == 4){
                aR.add_extra_bytes(6, aR.extraByteNames.get(3), "blank");
            }

            if(c - '0' == 5){
                aR.add_extra_bytes(6, aR.extraByteNames.get(4), "blank");
            }

            if(c - '0' == 6){
                aR.add_extra_bytes(6, aR.extraByteNames.get(5), "blank");
            }

            if(c - '0' == 7){
                aR.add_extra_bytes(6, aR.extraByteNames.get(6), "blank");
            }
        }



        byte myBitti = myByte.byteValue();

        int minimum_point_format = 2;
        to.pointDataRecordFormat = minimum_point_format;
        int minimum_version = 2;


        String line = "";
        LasPoint tempPoint = new LasPoint();

        int x_offset_update = 0, y_offset_update = 0, z_offset_update = 0;
        double x_scale_update = 0, y_scale_update = 0, z_scale_update = 0;
        int n_elements = 0;

        while (reader != null) {

            ElementType type = reader.getElementType();

            // In PLY files vertices always have a type named "vertex".
            if (type.getName().equals("vertex")) {
                //printVertices(reader);


                tempPoint.x = reader.readElement().getDouble("x");
                tempPoint.y = reader.readElement().getDouble("y");
                tempPoint.z = reader.readElement().getDouble("z");

                x_offset_update = (int) tempPoint.x;
                y_offset_update = (int) tempPoint.y;
                z_offset_update = (int) tempPoint.z;


                x_scale_update = 0.01;
                y_scale_update = 0.01;
                z_scale_update = 0.01;
            }

            break;

        }


        ply = new PlyReaderFile(infile);

        reader = ply.nextElementReader();


        to.writeAscii(4, "LASF");
        to.writeUnsignedShort((short)2); // = braf.readUnsignedShort();
        to.writeUnsignedShort((short)1); // = braf.readUnsignedShort();

        // GUID
        to.writeLong(0);
        to.writeAscii(8, "");
        to.writeUnsignedByte((byte)1);// = braf.readUnsignedByte();
        to.writeUnsignedByte((byte)minimum_version);// = braf.readUnsignedByte();

        if(aR.identifier != null)
            to.writeAscii(32, aR.identifier);// = braf.readAscii(16);
        else
            to.writeAscii(32, "LASutils (c) by Mikko Kukkonen");// systemIdentifier = braf.readAscii(32);

        to.writeAscii(32, (softwareName + " version 0.1"));// generatingSoftware = braf.readAscii(32);
        int year = Calendar.getInstance().get(Calendar.YEAR); //now.getYear();
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        to.writeUnsignedShort((short)dayOfYear);// = braf.readUnsignedShort();
        to.writeUnsignedShort((short)(year));// = braf.readUnsignedShort();

        /* Offset to point data */
        int add_bytes_as_vlr_ = 0;

        if(aR.create_extra_byte_vlr_n_bytes.size() > 0) {

            add_bytes_as_vlr_ = 192  * aR.create_extra_byte_vlr_n_bytes.size()  + 54;

        }

        int add_bytes = 0;

        if(aR.create_extra_byte_vlr_n_bytes.size() > 0) {
            for(int i = 0; i < aR.create_extra_byte_vlr_n_bytes.size(); i++)
                add_bytes += aR.create_extra_byte_vlr_n_bytes.get(i);
        }

        if(minimum_version == 2){
            to.writeUnsignedShort((short)227);// = braf.readUnsignedShort();
            to.writeUnsignedInt(227 + add_bytes_as_vlr_);
        }
        else if(minimum_version == 3){
            to.writeUnsignedShort((short)335);// = braf.readUnsignedShort();
            to.writeUnsignedInt(335 + add_bytes_as_vlr_);

        }else if(minimum_version == 4){
            to.writeUnsignedShort((short)375);// = braf.readUnsignedShort();
            to.writeUnsignedInt(375 + add_bytes_as_vlr_);
        }

        if(aR.create_extra_byte_vlr_n_bytes.size() == 0)
            to.writeUnsignedInt(0);// = braf.readUnsignedInt();
        else{
            to.writeUnsignedInt(1);// = braf.readUnsignedInt();
        }
        to.writeUnsignedByte((byte)minimum_point_format);// = braf.readUnsignedByte();

        short pointDataRecordLength = (short)getPointDataRecordLength(minimum_point_format);

        if(aR.create_extra_byte_vlr_n_bytes.size() > 0)
            pointDataRecordLength += add_bytes;

        to.writeUnsignedShort(pointDataRecordLength);// = braf.readUnsignedShort();
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
        //System.out.println(minimum_version);

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


        int n_added_bytes = 0;

        /* If the tool requires to save data for each point
         * and no extra byte VLR exists */
        if(aR.create_extra_byte_vlr.size() > 0){

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

        //while(to.allArray2Index < from.offsetToPointData + n_added_bytes){
        //	to.writeUnsignedByte((byte)0);
        //}

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

        int bufferSize = 1000;

        double[][] points = new double[bufferSize][n_elements];
        int counter = 0;

        ElementType type = null;
        //Element element = reader.readElement();
        float[] extraBytes = new float[aR.create_extra_byte_vlr.size()];
        try {
            //FileInputStream fis = new FileInputStream(from);
            //BufferedReader in = new BufferedReader(new InputStreamReader(fis));

            while (reader != null) {

                type = reader.getElementType();
                // In PLY files vertices always have a type named "vertex".
                //if (type.getName().equals("vertex")) {
                    //printVertices(reader);
                if (type.getName().equals("vertex")) {
                    Element element2 = reader.readElement();

                    while (element2 != null) {


                        // Use the the 'get' methods to access the properties.
                        // jPly automatically converts the various data types supported
                        // by PLY for you.

                        //System.out.print("x=");
                        tempPoint.x = (element2.getDouble("x"));
                        //System.out.print(" y=");
                        tempPoint.y = (element2.getDouble("y"));
                        //System.out.print(" z=");
                        tempPoint.z = (element2.getDouble("z"));
                        //System.out.println();

                        element2 = reader.readElement();


                        //tempPoint.x = reader.readElement().getDouble("x");
                        //tempPoint.y = reader.readElement().getDouble("y");
                        //tempPoint.z = reader.readElement().getDouble("z");

                        //x_offset_update = (int) tempPoint.x;
                       // y_offset_update = (int) tempPoint.y;
                        //z_offset_update = (int) tempPoint.z;

                        //System.out.println("X: " + tempPoint.x + " Y: " + tempPoint.y + " Z: " + tempPoint.z);

                        x_scale_update = 0.01;
                        y_scale_update = 0.01;
                        z_scale_update = 0.01;
                        //}else{

                        //reader.close();
                        //reader = ply.nextElementReader();
                        //System.out.println(type.getName());
                        //continue;

                        //}

                        if (!aR.inclusionRule.ask(tempPoint, 1, true)) {
                            //continue;
                        }

                        if (tempPoint.numberOfReturns > 5) {
                            version_minor_update = 4;
                        }

                        /* Infer scale and offset from the first point */
                        if (x_scale_update == 0) {

                            x_offset_update = (int) tempPoint.x;
                            y_offset_update = (int) tempPoint.y;
                            z_offset_update = (int) tempPoint.z;

                            x_scale_update = 0.01;
                            y_scale_update = 0.01;
                            z_scale_update = 0.01;

                        }

                        if (echoClass) {

                            //tempPoint.userData = tempPoint.numberOfReturns;
					/*
					#   0 = only

					#   1 = first of many
					#   2 = intermediate

   					#   3 = last of many
							*/
                            //System.out.println("HERE!!");

                            //System.out.println(tempPoint.numberOfReturns + " " + tempPoint.returnNumber);

                            if (tempPoint.numberOfReturns == 0) {
                                tempPoint.numberOfReturns = 1;
                                tempPoint.returnNumber = 1;
                            } else if (tempPoint.numberOfReturns == 1) {
                                tempPoint.numberOfReturns = 2;
                                tempPoint.returnNumber = 1;
                            } else if (tempPoint.numberOfReturns == 2) {
                                tempPoint.numberOfReturns = 3;
                                tempPoint.returnNumber = 2;
                            } else if (tempPoint.numberOfReturns == 3) {
                                tempPoint.numberOfReturns = 2;
                                tempPoint.returnNumber = 2;
                            } else {

                            }

                        }

                        //System.out.println(tempPoint.pointSourceId);
                        /* Should we catch these? Probably not */
                        if (tempPoint.returnNumber <= 0 || tempPoint.numberOfReturns <= 0) {

                            //System.exit(1);
                            //continue;
                            tempPoint.returnNumber = 1;
                            tempPoint.numberOfReturns = 1;


                        }

                        if (aR.inclusionRule.ask(tempPoint, count, false)) {

                            //System.out.println("HERE");
                            pointCount++;

                            int returnNumberi = tempPoint.returnNumber - 1;

                            if (returnNumberi >= 0 && returnNumberi < 5)
                                pointsByReturn[returnNumberi]++;
                            if (returnNumberi >= 0 && returnNumberi < 15)
                                pointsByReturn_1_4[returnNumberi]++;

                            double x = tempPoint.x;
                            double y = tempPoint.y;
                            double z = tempPoint.z;

                            if (z < minZ)
                                minZ = z;
                            if (x < minX)
                                minX = x;
                            if (y < minY)
                                minY = y;

                            if (z > maxZ)
                                maxZ = z;
                            if (x > maxX)
                                maxX = x;
                            if (y > maxY)
                                maxY = y;

                            int lx = (int) ((x - x_offset_update) / x_scale_update);
                            int ly = (int) ((y - y_offset_update) / y_scale_update);
                            int lz = (int) ((z - z_offset_update) / z_scale_update);

                            if (minimum_point_format <= 5) {


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


                            } else {

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

                                int numberOfReturns_ = mask >> 4;

                                int returnNumber_ = mask & 15;

                                if (tempPoint.numberOfReturns != numberOfReturns_ || returnNumber_ != tempPoint.returnNumber) {
                                    System.out.println("HATHAFHSAFD: !");
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

                            if (extraBytes.length > 0) {

                                for (float f : extraBytes) {
                                    //System.out.println(f);
                                    to.writeFloat(f);
                                }

                            }

                        }
                        count++;
                    }
                }
                System.out.println("HERE!");
                reader.close();
                reader = ply.nextElementReader();

            }
            //in.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        to.writeBuffer2();

        if(minimum_version == 4){
            to.updateHeader_1_4_2((int)pointCount, minX, maxX, minY, maxY
                    , maxZ, minZ, pointsByReturn, pointsByReturn_1_4,  aR, x_offset_update, y_offset_update, z_offset_update,
                    x_scale_update, y_scale_update, z_scale_update);
        }else
            LASwrite.updateHeader_txt2las(pointCount, pointsByReturn, minX, maxX, minY, maxY
                    , maxZ, minZ, to, x_offset_update, y_offset_update, z_offset_update,
                    x_scale_update, y_scale_update, z_scale_update);

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


    private static void printVertices(ElementReader reader) throws IOException {

        // The number of elements is known in advance. This is great for
        // allocating buffers and a like.
        // You can even get the element counts for each type before getting
        // the corresponding reader via the PlyReader.getElementCount(..)
        // method.
        System.out.println("There are " + reader.getCount() + " vertices:");

        // Read the elements. They all share the same type.
        Element element = reader.readElement();
        while (element != null) {

            // Use the the 'get' methods to access the properties.
            // jPly automatically converts the various data types supported
            // by PLY for you.

            System.out.print("x=");
            System.out.print(element.getDouble("x"));
            System.out.print(" y=");
            System.out.print(element.getDouble("y"));
            System.out.print(" z=");
            System.out.print(element.getDouble("z"));
            System.out.println();

            element = reader.readElement();
        }
    }

    private static void threadTool(argumentReader aR, fileDistributor fD) {

        proge.setEnd(aR.inputFiles.size());

        if (aR.cores > aR.inputFiles.size())
            aR.cores = aR.inputFiles.size();

        ArrayList<Thread> threadList = new ArrayList<Thread>();

        for (int ii = 1; ii <= aR.cores; ii++) {

            proge.addThread();
            Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
            threadList.add(temp);
            temp.start();

        }

        for (int i = 0; i < threadList.size(); i++) {

            try {
                threadList.get(i).join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Just a class to divide workers for multithreaded tools.
     */
    static class multiThreadTool implements Runnable {

        argumentReader aR;
        int nCores;
        int nCore;
        fileDistributor fD;

        public multiThreadTool(argumentReader aR, int nCores, int nCore, fileDistributor fD) {

            this.aR = aR;
            this.nCores = nCores;
            this.nCore = nCore;
            this.fD = fD;

        }

        public void run() {

            process_las2las tooli = new process_las2las(nCore);

            while (true) {
                if (fD.isEmpty())
                    break;
                File f = fD.getFile();
                if (f == null)
                    continue;
                LASReader temp = null;
                try {
                    temp = new LASReader(f);
                }catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    tooli.convert(temp, aR);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
