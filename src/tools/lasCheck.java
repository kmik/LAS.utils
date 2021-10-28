package tools;

import LASio.LASReader;
import LASio.LasPoint;
import utils.argumentReader;

import java.util.ArrayList;
import java.util.Calendar;

import static com.github.mreutegg.laszip4j.laszip.MyDefs.U32_MAX;

public class lasCheck {

    argumentReader aR;
    LASReader pointCloud;

    ArrayList<String> fails = new ArrayList<String>();
    long numberOfPoints = 0L;

    long[] numberOfPointsByReturn = new long[15];
    double[] gpsRange = new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};

    double[] minMax_x = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
    double[] minMax_y = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
    double[] minMax_z = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};

    public lasCheck(LASReader pointCloud, argumentReader aR) throws Exception {

        this.pointCloud = pointCloud;
        this.aR = aR;

        System.out.println("START READING:");
        this.readPoints();
        System.out.println("END READING:");

    }

    public void readPoints() throws Exception{

        int maxi = 0;

        LasPoint tempPoint = new LasPoint();

        //pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());
        int thread_n = aR.pfac.addReadThread(pointCloud);

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {


            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            //try {
                //pointCloud.readRecord_noRAF(i, tempPoint, maxi);

            aR.pfac.prepareBuffer(thread_n, i, 10000);
            //} catch (Exception e) {
                //e.printStackTrace();//pointCloud.braf.buffer.position(0);
           // }

            for (int j = 0; j < maxi; j++) {
                //Sstem.out.println(j);
                pointCloud.readFromBuffer(tempPoint);


                this.numberOfPoints++;

                numberOfPointsByReturn[tempPoint.returnNumber]++;

                if(tempPoint.gpsTime < gpsRange[0])
                    gpsRange[0] = tempPoint.gpsTime;
                if(tempPoint.gpsTime > gpsRange[1])
                    gpsRange[1] = tempPoint.gpsTime;

                if(tempPoint.x < minMax_x[0])
                    minMax_x[0] = tempPoint.x;
                if(tempPoint.x > minMax_x[1])
                    minMax_x[1] = tempPoint.x;

                if(tempPoint.y < minMax_y[0])
                    minMax_y[0] = tempPoint.y;
                if(tempPoint.y > minMax_y[1])
                    minMax_y[1] = tempPoint.y;

                if(tempPoint.z < minMax_z[0])
                    minMax_z[0] = tempPoint.z;
                if(tempPoint.z > minMax_z[1])
                    minMax_z[1] = tempPoint.z;

            }
        }
    }


    // TODO: Check offset x y z ; ASCII checks

    public boolean check(){

        //System.out.println(4 & 2);
        //System.exit(1);

        int global_encoding = pointCloud.globalEncoding;
        if(!pointCloud.fileSignature.equals("LASF")){
            fails.add("File signature should be LASF and not " + pointCloud.fileSignature);
        }

        if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor <= 1)) {
            if (pointCloud.globalEncoding > 0) {
                fails.add(String.format("global encoding: should be 0 for LAS version %d.%d but is %d", pointCloud.versionMajor, pointCloud.versionMinor, pointCloud.globalEncoding));
            }
        }
        else if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor <= 2)) {
            if (pointCloud.globalEncoding > 1) {
                fails.add(String.format("global encoding: should not be greater than 1 for LAS version %d.%d but is %d", pointCloud.versionMajor, pointCloud.versionMinor, pointCloud.globalEncoding));
            }
        }
        else if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor <= 3)) {
            if (pointCloud.globalEncoding > 15) {
                fails.add(String.format("global encoding: should not be greater than 1 for LAS version %d.%d but is %d", pointCloud.versionMajor, pointCloud.versionMinor, pointCloud.globalEncoding));
            }
        }
        else if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor <= 4)) {
            if (pointCloud.globalEncoding > 31) {
                fails.add(String.format("global encoding: should not be greater than 1 for LAS version %d.%d but is %d", pointCloud.versionMajor, pointCloud.versionMinor, pointCloud.globalEncoding));
            }
        }

        //if ((pointCloud.globalEncoding & 16) == 1) {
        if ((pointCloud.globalEncoding & 16) == 1) {
            if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor <= 3)) {
                fails.add(String.format("global encoding: set bit 4 not defined for LAS version %d.%d", pointCloud.versionMajor, pointCloud.versionMinor));
            }
        }
        else {
            if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor >= 4) && (pointCloud.pointDataRecordFormat >= 6)) {
                fails.add(String.format("global encoding: bit 4 must be set (OGC WKT must be used) for point data format %d", pointCloud.pointDataRecordFormat));
            }
        }

        if ((pointCloud.globalEncoding & 8) == 1) {
            if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor <= 2)) {
                fails.add(String.format("global encoding: set bit 3 not defined for LAS version %d.%d", pointCloud.versionMajor, pointCloud.versionMinor));
            }
        }

        if ((pointCloud.globalEncoding & 4) == 1) {
            if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor <= 2)) {
                fails.add(String.format("set bit 2 not defined for LAS version %d.%d", pointCloud.versionMajor, pointCloud.versionMinor));
            }
            if ((pointCloud.pointDataRecordFormat != 4) && (pointCloud.pointDataRecordFormat != 5) && (pointCloud.pointDataRecordFormat != 9) && (pointCloud.pointDataRecordFormat != 10)) {
                fails.add(String.format("set bit 2 not defined for point data format %d", pointCloud.pointDataRecordFormat));
            }
            if ((pointCloud.globalEncoding & 2) == 1) {
                fails.add(String.format("global encoding: although bit 1 and bit 2 are mutually exclusive they are both set"));
            }
        }
        else if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor >= 3))
        {
            if ((pointCloud.pointDataRecordFormat == 4) || (pointCloud.pointDataRecordFormat == 5) || (pointCloud.pointDataRecordFormat == 9) || (pointCloud.pointDataRecordFormat == 10)) {
                if ((pointCloud.globalEncoding & 2) == 0 && (pointCloud.globalEncoding & 4) == 0) {
                    fails.add(String.format("global encoding: neither bit 1 nor bit 2 are set for point data format %d", pointCloud.pointDataRecordFormat));
                }
            }
        }


        if ((pointCloud.globalEncoding & 2) == 1) {
            if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor <= 2)){
                fails.add(String.format("global encoding: set bit 1 not defined for LAS version %d.%d", pointCloud.versionMajor, pointCloud.versionMinor));
            }
            if ((pointCloud.pointDataRecordFormat != 4) && (pointCloud.pointDataRecordFormat != 5) && (pointCloud.pointDataRecordFormat != 9) && (pointCloud.pointDataRecordFormat != 10)) {
                fails.add(String.format("global encoding: set bit 1 not defined for point data format %d", pointCloud.pointDataRecordFormat));
            }
        }

        if ((pointCloud.globalEncoding & 1) == 1) {
            if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor <= 1)) {
                fails.add(String.format("global encoding: set bit 0 not defined for LAS version %d.%d", pointCloud.versionMajor, pointCloud.versionMinor));
            }

            if (pointCloud.pointDataRecordFormat == 0) {
                fails.add(String.format("global encoding: set bit 0 not defined for point data format 0", pointCloud.versionMajor, pointCloud.versionMinor));
            }
        }
        else {
            if (pointCloud.pointDataRecordFormat > 0) {

                if ((this.gpsRange[0] < 0.0) || (this.gpsRange[0] > 604800.0)) {

                    fails.add(String.format("global encoding: unset bit 0 suggests GPS week time but GPS time ranges from %d to %d", gpsRange[0], gpsRange[1]));

                }

            }
        }

        // check version major

        if (pointCloud.versionMajor != 1) {
            fails.add(String.format("version major should be 1 and not %d", pointCloud.versionMajor));
        }

        // check version minor

        if ((pointCloud.versionMinor != 0) && (pointCloud.versionMinor != 1) && (pointCloud.versionMinor != 2) && (pointCloud.versionMinor != 3) && (pointCloud.versionMinor != 4)) {
            fails.add(String.format("version minor should be between 0 and 4 and not %d", pointCloud.versionMinor));
        }

        int i = 0, j = 0;
        /*
        for (i = 0; i < 32; i++) {
            if (pointCloud.systemIdentifier.charAt(i) == '\0') {
                break;
            }
        }

        if (i == 32) {
            fails.add(String.format("system identifier: string should be terminated by a '\\0' character"));
        }
        else if (i == 0) {
            fails.add(String.format("system identifier: empty string. first character is '\\0'"));
        }
        for (j = i; j < 32; j++) {
            if (pointCloud.systemIdentifier.charAt(j) != '\0') {
                break;
            }
        }
        if (j != 32) {
            fails.add(String.format("system identifier: remaining characters should all be '\\0'"));
        }


        // check generating software

        for (i = 0; i < 32; i++) {
            if (pointCloud.generatingSoftware.charAt(i) == '\0') {
                break;
            }
        }
        if (i == 32) {
            fails.add(String.format("generating software: string should be terminated by a '\\0' character"));
        }
        else if (i == 0) {
            fails.add(String.format("generating software: empty string. first character is '\\0'"));
        }
        for (j = i; j < 32; j++) {
            if (pointCloud.generatingSoftware.charAt(j) != '\0') {
                break;
            }
        }
        if (j != 32) {
            fails.add(String.format("generating software: remaining characters should all be '\\0'"));
        }
        */

        // check file creation date

        if (pointCloud.fileCreationYear == 0) {
            if (pointCloud.fileCreationDayOfYear == 0) {
                fails.add(String.format("file creation day: not set"));
            }
            else if (pointCloud.fileCreationDayOfYear > 365) {
                fails.add(String.format("should be between 1 and 365 and not %d", pointCloud.fileCreationDayOfYear));
            }
            fails.add(String.format("file creation year: not set"));
        }
        else {
            // get today's date

            Calendar calendar = Calendar.getInstance();

            int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

            // does the year fall into the expected range

            if ((pointCloud.fileCreationYear < 1990) || (pointCloud.fileCreationYear > calendar.get(Calendar.YEAR))) {
                fails.add(String.format("file creation year: should be between 1990 and %d and not %d", calendar.get(Calendar.YEAR), pointCloud.fileCreationYear));
            }

            // does the day fall into the expected range

            int max_day_of_year = 365;

            if (pointCloud.fileCreationYear == calendar.get(Calendar.YEAR)) {
                // for the current year we need to limit the range (plus 1 because in GPS time January 1 is day 1)

                max_day_of_year = dayOfYear + 1;
            }
            else if ((((pointCloud.fileCreationYear)%4) == 0)) {
                // in a leap year we need to add one day

                max_day_of_year++;
            }

            if (pointCloud.fileCreationDayOfYear > max_day_of_year) {
                fails.add(String.format("file creation day: should be between 1 and %d and not %d", pointCloud.fileCreationDayOfYear));
            }
        }



        // check header size

        int min_header_size = 227;

        if (pointCloud.versionMajor == 1) {
            if (pointCloud.versionMinor >= 3) {
                min_header_size += 8;
            }
            if (pointCloud.versionMinor >= 4) {
                min_header_size += 40;
            }
        }

        if (pointCloud.headerSize < min_header_size) {
            fails.add(String.format("header size: should be at least %d and not %d", pointCloud.headerSize));
        }

        // check offset to point data

        int min_offset_to_point_data = pointCloud.headerSize;

        // add size of all VLRs

        for (i = 0; i < pointCloud.numberVariableLengthRecords; i++) {
            min_offset_to_point_data += 54; // VLR header size
            min_offset_to_point_data += pointCloud.getVariableLengthRecordList().get(i).getRecordLength(); // VLR payload size
        }

        if (pointCloud.offsetToPointData < min_offset_to_point_data) {
            fails.add(String.format("offset to point data: should be at least %u and not %u", min_offset_to_point_data, pointCloud.offsetToPointData));
        }

        // check point data format

        int max_point_data_format = 1;

        if (pointCloud.versionMajor == 1) {
            if (pointCloud.versionMinor == 2) {
                max_point_data_format = 3;
            }
            else if (pointCloud.versionMinor == 3) {
                max_point_data_format = 5;
            }
            else if (pointCloud.versionMinor == 4) {
                max_point_data_format = 10;
            }
        }

        if (pointCloud.pointDataRecordFormat > max_point_data_format) {
            fails.add(String.format("point data format: should be between 0 and %d and not %d", max_point_data_format, pointCloud.pointDataRecordFormat));
        }

        // check point data record length

        int min_point_data_record_length = 20;

        switch (pointCloud.pointDataRecordFormat)
        {
            case 1:
                min_point_data_record_length = 28;
                break;
            case 2:
                min_point_data_record_length = 26;
                break;
            case 3:
                min_point_data_record_length = 34;
                break;
            case 4:
                min_point_data_record_length = 57;
                break;
            case 5:
                min_point_data_record_length = 63;
                break;
            case 6:
                min_point_data_record_length = 30;
                break;
            case 7:
                min_point_data_record_length = 36;
                break;
            case 8:
                min_point_data_record_length = 38;
                break;
            case 9:
                min_point_data_record_length = 59;
                break;
            case 10:
                min_point_data_record_length = 67;
                break;
        }

        if (pointCloud.pointDataRecordLength < min_point_data_record_length) {
            fails.add(String.format("point data record length: should be at least %d and not %d", min_point_data_record_length, pointCloud.pointDataRecordLength));
        }

        // check that there is at least one point

        if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor < 4)) {
            if ( pointCloud.legacyNumberOfPointRecords == 0) {
                fails.add(String.format("zero points in file: files must contain at least one point record to be considered valid"));
            }
        }
        else {
            if (pointCloud.numberOfPointRecords == 0) {
                fails.add(String.format("zero points in file: files must contain at least one point record to be considered valid"));
            }
        }

        // check that number of points by return is less than or equal to number of point records

        long total = 0;

        if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor < 4)) {
            for (i = 0; i < 5; i++) {
                if (pointCloud.legacyNumberOfPointsByReturn[i] > pointCloud.legacyNumberOfPointRecords) {
                    fails.add(String.format("legacy number of points by return[%d] should not be larger than number of point records", i));
                }
                total += pointCloud.legacyNumberOfPointsByReturn[i];
            }
            if (total > pointCloud.legacyNumberOfPointRecords) {
                fails.add(String.format("legacy number of points by return [] array: sum should not be larger than number of point records"));
            }
        }
        else {
            for (i = 0; i < 15; i++) {
                if (pointCloud.numberOfPointsByReturn[i] > pointCloud.numberOfPointRecords) {
                    fails.add(String.format("legacy number of points by return[%d] should not be larger than number of point records", i));

                }
                total += pointCloud.numberOfPointsByReturn[i];
            }
            if (total > pointCloud.numberOfPointRecords) {
                fails.add(String.format("sum should not be larger than number of point records"));
            }
        }

        // check integraty between legacy number of point records and number of point records for LAS 1.4 and higher

        if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor >= 4)) {
            if (pointCloud.pointDataRecordFormat <= 5) {
                if (pointCloud.legacyNumberOfPointRecords == 0) {
                    if ((pointCloud.numberOfPointRecords > 0) && (pointCloud.numberOfPointRecords <= U32_MAX)) {
                        fails.add(String.format("legacy number of point records: unnecessary lack of forward compatibility. should be %u as this LAS 1.%d file contains less than %u points of type %d", (int)pointCloud.numberOfPointRecords, pointCloud.versionMinor, U32_MAX, pointCloud.pointDataRecordFormat));
                    }
                }
                else {
                    if (pointCloud.numberOfPointRecords != pointCloud.legacyNumberOfPointRecords) {
                        if (pointCloud.numberOfPointRecords <= U32_MAX) {
                            fails.add(String.format("legacy number of point records: should be %u to be consistent with number of point records instead of %u", pointCloud.numberOfPointRecords, pointCloud.legacyNumberOfPointRecords));

                        }
                        else {
                            fails.add(String.format("legacy number of point records: should be 0 for LAS 1.%d files that contain over %u points of type %d", pointCloud.versionMinor, U32_MAX, pointCloud.pointDataRecordFormat));
                        }

                    }
                }
            }
            else
            {
                if (pointCloud.legacyNumberOfPointRecords != 0)
                {
                    fails.add(String.format("legacy number of point records: should be 0 for LAS 1.%d files that contain point type %d", pointCloud.versionMinor, pointCloud.pointDataRecordFormat));
                }
            }
        }

        // check integraty between legacy number of points by return and number of points by return for LAS 1.4 and higher

        if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor >= 4))
        {
            for (i = 0; i < 5; i++)
            {
                if (pointCloud.pointDataRecordFormat <= 5)
                {
                    if (pointCloud.legacyNumberOfPointsByReturn[i] == 0)
                    {
                        if ((pointCloud.legacyNumberOfPointsByReturn[i] > 0) && (pointCloud.numberOfPointRecords <= U32_MAX))
                        {
                            fails.add(String.format("legacy number of points by return[%d]: unnecessary lack of forward compatibility. should be %u as this LAS 1.%d file contains less than %u points of type %d", i, pointCloud.legacyNumberOfPointsByReturn[i], pointCloud.versionMinor, pointCloud.pointDataRecordFormat));
                        }
                    }
                    else
                    {
                        if (pointCloud.numberOfPointsByReturn[i] != pointCloud.legacyNumberOfPointsByReturn[i])
                        {
                            if (pointCloud.numberOfPointRecords <= U32_MAX)
                            {
                                fails.add(String.format("legacy number of points by return[%d]: should be %u to be consistent with number of points by return instead of %u", i, pointCloud.numberOfPointsByReturn[i], pointCloud.legacyNumberOfPointsByReturn[i]));
                            }
                            else
                            {
                                fails.add(String.format("legacy number of points by return[%d]: should be 0 for LAS 1.%d files that contain over %u points of type %d", i, pointCloud.versionMinor, U32_MAX, pointCloud.pointDataRecordFormat));
                            }
                        }
                    }
                }
                else
                {
                    if (pointCloud.legacyNumberOfPointsByReturn[i] != 0)
                    {
                        //sprintf(problem, "legacy number of points by return[%d]", i);
                        fails.add(String.format("legacy number of points by return[%d]: should be 0 for LAS 1.%d files that contain point type %d", i, pointCloud.versionMinor, pointCloud.pointDataRecordFormat));

                        //sprintf(note, "should be 0 for LAS 1.%d files that contain point type %d", lasheader->version_minor, lasheader->point_data_format);
                        //lasheader->add_fail(problem, note);
                    }
                }
            }
        }


        // check number of point records in header against the counted inventory


        if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor >= 4)) {
            if (pointCloud.numberOfPointRecords != this.numberOfPoints) {
                fails.add(String.format("number of point records: there are only %d point records and not %d", this.numberOfPoints, pointCloud.numberOfPointRecords));
            }
        }
        else {
            if (pointCloud.legacyNumberOfPointRecords != this.numberOfPoints) {
                fails.add(String.format("number of point records: there are only %d point records and not %d", this.numberOfPoints, pointCloud.legacyNumberOfPointRecords));
            }
        }


        // check number of points by return in header against the counted inventory


        if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor >= 4)) {
            for (i = 0; i < 15; i++) {
                if (pointCloud.numberOfPointsByReturn[i] != this.numberOfPointsByReturn[i+1]) {
                    fails.add(String.format("number of points by return[%d]: the number of %d%s returns is %lld and not %lld", i, this.numberOfPointsByReturn[i+1], pointCloud.numberOfPointsByReturn[i]));
                }
            }
        }
        else {
            for (i = 0; i < 5; i++) {
                if (pointCloud.legacyNumberOfPointsByReturn[i] != this.numberOfPointsByReturn[i+1]) {
                    fails.add(String.format("number of points by return[%d]: the number of %d%s returns is %lld and not %lld", i, this.numberOfPointsByReturn[i+1], pointCloud.legacyNumberOfPointsByReturn[i]));
                }
            }
        }
        // check scale factor x y z

        if (pointCloud.xScaleFactor <= 0.0)
        {
            fails.add(String.format("x scale factor: %g is equal to or smaller than zero", pointCloud.xScaleFactor));
        }

        if (pointCloud.yScaleFactor <= 0.0)
        {
            fails.add(String.format("y scale factor: %g is equal to or smaller than zero", pointCloud.yScaleFactor));
        }

        if (pointCloud.zScaleFactor <= 0.0)
        {
            fails.add(String.format("z scale factor: %g is equal to or smaller than zero", pointCloud.zScaleFactor));
        }

        if(pointCloud.xScaleFactor * 10 == 1 || pointCloud.xScaleFactor * 10 == 5 || pointCloud.xScaleFactor * 10 == 2.5 ||
            pointCloud.xScaleFactor * 100 == 1 || pointCloud.xScaleFactor * 100 == 5 || pointCloud.xScaleFactor * 100 == 2.5 ||
            pointCloud.xScaleFactor * 1000 == 1 || pointCloud.xScaleFactor * 1000 == 5 || pointCloud.xScaleFactor * 1000 == 2.5 ||
            pointCloud.xScaleFactor * 10000 == 1 || pointCloud.xScaleFactor * 10000 == 5 || pointCloud.xScaleFactor * 10000 == 2.5 ||
            pointCloud.xScaleFactor * 100000 == 1 || pointCloud.xScaleFactor * 100000 == 5 || pointCloud.xScaleFactor * 100000 == 2.5 ||
            pointCloud.xScaleFactor * 1000000 == 1 || pointCloud.xScaleFactor * 1000000 == 5 || pointCloud.xScaleFactor * 1000000 == 2.5 ||
            pointCloud.xScaleFactor * 10000000 == 1 || pointCloud.xScaleFactor * 10000000 == 5 || pointCloud.xScaleFactor * 10000000 == 2.5 ||
            pointCloud.xScaleFactor * 100000000 == 1 || pointCloud.xScaleFactor * 100000000 == 5 || pointCloud.xScaleFactor * 100000000 == 2.5){

        }else{
            fails.add(String.format("x scale factor: should be factor ten of 0.1 or 0.5 or 0.25 and not %f", pointCloud.xScaleFactor));
        }
        if(pointCloud.yScaleFactor * 10 == 1 || pointCloud.yScaleFactor * 10 == 5 || pointCloud.yScaleFactor * 10 == 2.5 ||
                pointCloud.yScaleFactor * 100 == 1 || pointCloud.yScaleFactor * 100 == 5 || pointCloud.yScaleFactor * 100 == 2.5 ||
                pointCloud.yScaleFactor * 1000 == 1 || pointCloud.yScaleFactor * 1000 == 5 || pointCloud.yScaleFactor * 1000 == 2.5 ||
                pointCloud.yScaleFactor * 10000 == 1 || pointCloud.yScaleFactor * 10000 == 5 || pointCloud.yScaleFactor * 10000 == 2.5 ||
                pointCloud.yScaleFactor * 100000 == 1 || pointCloud.yScaleFactor * 100000 == 5 || pointCloud.yScaleFactor * 100000 == 2.5 ||
                pointCloud.yScaleFactor * 1000000 == 1 || pointCloud.yScaleFactor * 1000000 == 5 || pointCloud.yScaleFactor * 1000000 == 2.5 ||
                pointCloud.yScaleFactor * 10000000 == 1 || pointCloud.yScaleFactor * 10000000 == 5 || pointCloud.yScaleFactor * 10000000 == 2.5 ||
                pointCloud.yScaleFactor * 100000000 == 1 || pointCloud.yScaleFactor * 100000000 == 5 || pointCloud.yScaleFactor * 100000000 == 2.5){

        }else{
            fails.add(String.format("y scale factor: should be factor ten of 0.1 or 0.5 or 0.25 and not %f", pointCloud.yScaleFactor));
        }

        if(pointCloud.zScaleFactor * 10 == 1 || pointCloud.zScaleFactor * 10 == 5 || pointCloud.zScaleFactor * 10 == 2.5 ||
                pointCloud.zScaleFactor * 100 == 1 || pointCloud.zScaleFactor * 100 == 5 || pointCloud.zScaleFactor * 100 == 2.5 ||
                pointCloud.zScaleFactor * 1000 == 1 || pointCloud.zScaleFactor * 1000 == 5 || pointCloud.zScaleFactor * 1000 == 2.5 ||
                pointCloud.zScaleFactor * 10000 == 1 || pointCloud.zScaleFactor * 10000 == 5 || pointCloud.zScaleFactor * 10000 == 2.5 ||
                pointCloud.zScaleFactor * 100000 == 1 || pointCloud.zScaleFactor * 100000 == 5 || pointCloud.zScaleFactor * 100000 == 2.5 ||
                pointCloud.zScaleFactor * 1000000 == 1 || pointCloud.zScaleFactor * 1000000 == 5 || pointCloud.zScaleFactor * 1000000 == 2.5 ||
                pointCloud.zScaleFactor * 10000000 == 1 || pointCloud.zScaleFactor * 10000000 == 5 || pointCloud.zScaleFactor * 10000000 == 2.5 ||
                pointCloud.zScaleFactor * 100000000 == 1 || pointCloud.zScaleFactor * 100000000 == 5 || pointCloud.zScaleFactor * 100000000 == 2.5){

        }else{
            fails.add(String.format("z scale factor: should be factor ten of 0.1 or 0.5 or 0.25 and not %f", pointCloud.zScaleFactor));
        }



        // check z

        if(pointCloud.maxZ != this.minMax_z[1]){
            fails.add(String.format("max z: should be %f and not %f", this.minMax_z[1], pointCloud.maxZ));
        }
        if(pointCloud.minZ != this.minMax_z[0]){
            fails.add(String.format("min z: should be %f and not %f", this.minMax_z[0], pointCloud.minZ));
        }

        if(pointCloud.maxX != this.minMax_x[1]){
            fails.add(String.format("max x: should be %f and not %f", this.minMax_x[1], pointCloud.maxX));
        }
        if(pointCloud.minX != this.minMax_x[0]){
            fails.add(String.format("min x: should be %f and not %f", this.minMax_x[0], pointCloud.maxX));
        }

        if(pointCloud.maxY != this.minMax_y[1]){
            fails.add(String.format("max y: should be %f and not %f", this.minMax_y[1], pointCloud.maxY));
        }
        if(pointCloud.minY != this.minMax_y[0]){
            fails.add(String.format("min y: should be %f and not %f", this.minMax_y[0], pointCloud.maxY));
        }

        // check global encoding

        if ((pointCloud.versionMajor == 1) && (pointCloud.versionMinor >= 3)) {
            if (((pointCloud.globalEncoding & 2) == 0) && (pointCloud.startOfWaveformDataPacketRec != 0)) {
                fails.add(String.format("start of waveform data packet record: should be 0 and not %u because global encoding bit 1 is not set", pointCloud.startOfWaveformDataPacketRec));
            }
            else if (((pointCloud.globalEncoding & 2) == 2) && (pointCloud.startOfWaveformDataPacketRec == 0)) {
                fails.add(String.format("start of waveform data packet record: should not be 0 because global encoding bit 1 is set"));
            }
        }


        // check for resolution fluff in the coordinates



        //System.out.println(fails.size());
        System.out.println("Report for file: " + pointCloud.getFile().getAbsolutePath());
        if(fails.size() == 0)
            System.out.println("All checks OK!!");

        for(int f = 0; f < fails.size(); f++)
            System.out.println(fails.get(f));

        return true;
    }

}
