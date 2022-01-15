package utils;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static runners.RunLASutils.fo;

public class gz_tools {

    argumentReader aR;

    public gz_tools(argumentReader aR){

        this.aR = aR;

    }

    public void process(File inputFile) throws Exception{

        File outputFile = aR.createOutputFileWithExtension(inputFile, "_R_G_B_NIR.txt.gz");

        System.out.println("INPUT FILE: " + inputFile.getAbsolutePath());

        InputStream fileStream = new FileInputStream(inputFile);
        OutputStream fileStream_out = new FileOutputStream(outputFile);

        InputStream gzipStream = new GZIPInputStream(fileStream, 5000000);
        Writer writer = new OutputStreamWriter(new GZIPOutputStream(fileStream_out), "US-ASCII");

        Reader decoder = new InputStreamReader(gzipStream, "US-ASCII");

        BufferedReader br = new BufferedReader(decoder);

        GZIPOutputStream gos = new GZIPOutputStream(
                new FileOutputStream(outputFile));

        String content;

        aR.sep = " ";

        String[] tokens = br.readLine().split(" ");

        double x = Double.parseDouble(tokens[0]);
        double y = Double.parseDouble(tokens[1]);
        double dz = Double.parseDouble(tokens[3]);

        double prev_x = x, prev_y = y, prev_dz = dz;

        int R = Integer.parseInt(tokens[5]), G = Integer.parseInt(tokens[6]),
        B = Integer.parseInt(tokens[7]), NIR = Integer.parseInt(tokens[8]);
        int counter = 1;
        double sum_R = R, sum_G = G, sum_B = B, sum_NIR = NIR;
        StringBuilder str
                = new StringBuilder();

        while ((content = br.readLine()) != null) {

            tokens = content.split(aR.sep);

            /* 511000.01 6871988.36 131.01 -0.03 0 688 1273 1428 3875 374 */
            /* x , y , z , dz , echo_class , R , G , B , NiR , id */

            x = Double.parseDouble(tokens[0]);
            y = Double.parseDouble(tokens[1]);
            dz = Double.parseDouble(tokens[3]);

            R = Integer.parseInt(tokens[5]);
            G = Integer.parseInt(tokens[6]);
            B = Integer.parseInt(tokens[7]);
            NIR = Integer.parseInt(tokens[8]);


            if( x != prev_x || y != prev_y || dz != prev_dz){

                sum_R /= (double)counter;
                sum_G /= (double)counter;
                sum_B /= (double)counter;
                sum_NIR /= (double)counter;
                String out = prev_x + " " + prev_y + " " + prev_dz + " " + Math.round(sum_R*100.0)/100.0 + " " + Math.round(sum_G*100.0)/100.0 + " " + Math.round(sum_B*100.0)/100.0 + " " + Math.round(sum_NIR*100.0)/100.0;

                str.append(out + "\n");

                sum_R = 0; sum_G = 0; sum_B = 0; sum_NIR = 0; counter = 0;

            }

            sum_R += R; sum_G += G; sum_B += B; sum_NIR += NIR;
            prev_x = x; prev_y = y; prev_dz = dz;
            counter++;

        }

        writer.write(str.toString());

        writer.flush();
        writer.close();

    }
}
