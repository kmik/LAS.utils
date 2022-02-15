package utils;

import fastParser.Parser;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
        //File outputFile = aR.createOutputFileWithExtension(inputFile, "_R_G_B_NIR.txt");

        System.out.println("INPUT FILE: " + inputFile.getAbsolutePath());

        InputStream fileStream = new FileInputStream(inputFile);
        //OutputStream fileStream_out = new FileOutputStream(outputFile);

        //InputStream gzipStream = new GZIPInputStream(fileStream, 2^24);
        InputStreamReader gzipStream = new InputStreamReader(new GZIPInputStream(new BufferedInputStream(fileStream)));

        //Writer writer = new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(fileStream_out, 2^24), 2^24), "US-ASCII");

        //Reader decoder = new InputStreamReader(gzipStream, "US-ASCII");
        //Reader decoder = new InputStreamReader(fileStream, "US-ASCII");

        //BufferedReader br = new BufferedReader(decoder);
        BufferedReader br = new BufferedReader(gzipStream);

        GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(outputFile), 2^23);
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(gos, "US-ASCII"), 2^23);
        //OutputStream gos = new FileOutputStream(outputFile);

        String content;

        aR.sep = " ";

        char delim = ' ';

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

            Parser parser = new Parser(content);

            x = parser.eatDouble();
            parser.eatWhitespace();
            y = parser.eatDouble();
            parser.eatWhitespace();
            dz = parser.eatDouble();
            parser.eatWhitespace();
            dz = parser.eatDouble();
            parser.eatWhitespace();

            parser.eatInt();
            parser.eatWhitespace();

            R = parser.eatInt();
            parser.eatWhitespace();

            G = parser.eatInt();
            parser.eatWhitespace();

            B = parser.eatInt();
            parser.eatWhitespace();

            NIR = parser.eatInt();
            parser.eatWhitespace();

            //tokens = content.split(aR.sep);
            //tokens = split(content, delim);

            /* 511000.01 6871988.36 131.01 -0.03 0 688 1273 1428 3875 374 */
            /* x , y , z , dz , echo_class , R , G , B , NiR , id */
/*
            x = Double.parseDouble(tokens[0]);
            y = Double.parseDouble(tokens[1]);
            dz = Double.parseDouble(tokens[3]);

            R = Integer.parseInt(tokens[5]);
            G = Integer.parseInt(tokens[6]);
            B = Integer.parseInt(tokens[7]);
            NIR = Integer.parseInt(tokens[8]);

*/
            if( x != prev_x || y != prev_y || dz != prev_dz){

                sum_R /= (double)counter;
                sum_G /= (double)counter;
                sum_B /= (double)counter;
                sum_NIR /= (double)counter;
                String out = prev_x + " " + prev_y + " " + prev_dz + " " + Math.round(sum_R*100.0)/100.0 + " " + Math.round(sum_G*100.0)/100.0 + " " + Math.round(sum_B*100.0)/100.0 + " " + Math.round(sum_NIR*100.0)/100.0;
/*
                //str.append(out + "\n");
                writer.append(String.valueOf(prev_x));writer.append(" ");
                writer.append(String.valueOf(prev_y));writer.append(" ");
                writer.append(String.valueOf(prev_dz));writer.append(" ");
                writer.append(String.valueOf(Math.round(sum_R*100.0)/100.0));writer.append(" ");
                writer.append(String.valueOf(Math.round(sum_G*100.0)/100.0));writer.append(" ");
                writer.append(String.valueOf(Math.round(sum_B*100.0)/100.0));writer.append(" ");
                writer.append(String.valueOf(Math.round(sum_NIR*100.0)/100.0));writer.append(" ");
                writer.append("\n");


*/
                //writer.append(str.toString());
                //writer.w
                //str = new StringBuilder(8);
                str.append(out);
                str.append("\n");

                sum_R = 0; sum_G = 0; sum_B = 0; sum_NIR = 0; counter = 0;

            }

            sum_R += R; sum_G += G; sum_B += B; sum_NIR += NIR;
            prev_x = x; prev_y = y; prev_dz = dz;
            counter++;

        }

        if(sum_R != 0 && sum_G != 0 &&  sum_B != 0 &&  sum_NIR != 0){

            sum_R /= (double)counter;
            sum_G /= (double)counter;
            sum_B /= (double)counter;
            sum_NIR /= (double)counter;
            String out = prev_x + " " + prev_y + " " + prev_dz + " " + Math.round(sum_R*100.0)/100.0 + " " + Math.round(sum_G*100.0)/100.0 + " " + Math.round(sum_B*100.0)/100.0 + " " + Math.round(sum_NIR*100.0)/100.0;
/*
            writer.append(String.valueOf(prev_x));writer.append(" ");
            writer.append(String.valueOf(prev_y));writer.append(" ");
            writer.append(String.valueOf(prev_dz));writer.append(" ");
            writer.append(String.valueOf(Math.round(sum_R*100.0)/100.0));writer.append(" ");
            writer.append(String.valueOf(Math.round(sum_G*100.0)/100.0));writer.append(" ");
            writer.append(String.valueOf(Math.round(sum_B*100.0)/100.0));writer.append(" ");
            writer.append(String.valueOf(Math.round(sum_NIR*100.0)/100.0));writer.append(" ");
            writer.append("\n");
*/
            //str = new StringBuilder(8);

            str.append(out);
            str.append("\n");

            sum_R = 0; sum_G = 0; sum_B = 0; sum_NIR = 0; counter = 0;

        }

        writer.write(str.toString());

        writer.flush();
        writer.close();

    }

    public static String[] split(final String line, final char delimiter)
    {
        CharSequence[] temp = new CharSequence[(line.length() / 2) + 1];
        int wordCount = 0;
        int i = 0;
        int j = line.indexOf(delimiter, 0); // first substring

        while (j >= 0)
        {
            temp[wordCount++] = line.substring(i, j);
            i = j + 1;
            j = line.indexOf(delimiter, i); // rest of substrings
        }

        temp[wordCount++] = line.substring(i); // last substring

        String[] result = new String[wordCount];
        System.arraycopy(temp, 0, result, 0, wordCount);

        return result;
    }
}
