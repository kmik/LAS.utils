package runners;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Pilkonta {

	public static void Pilko(String inputfile, String outputfilepath){
	
		int solulkm = 0;
		int soluid = 0;
		int idcount = 1;
		int id1 = 0;
		int id2 = 0;
		int laskuri = 1;
		double xmin = 1000000;
		double ymax = 0;
		double xmax = 0;
		double ymin = 1000000;
		String[] tokens;
		try (BufferedReader br = new BufferedReader(new FileReader(inputfile))) {
		    String line;
		    
	    	File fout = new File(outputfilepath + "output_1.txt");
	    	FileOutputStream fos = new FileOutputStream(fout);
	    	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		    
		    while ((line = br.readLine()) != null) {    
		    	    	
		    	
		    	//if(fout.length() > koko){
		    		
		    		if(idcount == 1){
			    	tokens = line.split(" "); 
			    	id1 =Integer.parseInt(tokens[0]);
			    	id2 =Integer.parseInt(tokens[0]);
		    		}
		    		
		    		else{
				    tokens = line.split(" "); 
				    id2 =Integer.parseInt(tokens[0]);
		    		}		    	    			    
				idcount = idcount + 1;
		    	//}
		    	
		    	
		    	if(id1 != id2){
		    		
		    		laskuri++;
		    		bw.close();
		    		
		    	    File oldfile =new File(outputfilepath + "output_" + (laskuri - 1)  + ".txt");
		            File newfile =new File(outputfilepath + "cells_" + Math.round(xmin) + "_" + Math.round(ymax) + "_" +  Math.round(xmax) + "_" + Math.round(ymin) +".txt");
		            oldfile.renameTo(newfile);
		    		
		    	    File fout1 = new File(outputfilepath + "output_" + laskuri + ".txt");
		    	    FileOutputStream fos1 = new FileOutputStream(fout1);
		    	    BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(fos1));
		    	
		    	    fout=fout1;
		    	    fos=fos1;
		    	    bw=bw1;
		    	
		    	    id1 = 0;
		    	    id2 = 0;
		    	    idcount = 1;
		    		xmin = 1000000;
		    		ymax = 0;
		    		solulkm = 0;
		    		soluid = 0;
		    		xmax = 0;
		    		ymin = 1000000;
		    	   
				   line = tokens[tokens.length - 1];
				   
				   for(int i = 1; i < (tokens.length - 1); i++)
					   line += (" " + tokens[i]);
				   
				   
		    	    //line = tokens[10] + " " +tokens[1] + " " +tokens[2] + " " +tokens[3] + " " +tokens[4] + " " +tokens[5] + " " +tokens[6] + " " +	tokens[7] + " " +tokens[8] + " " +tokens[9];
		    	    xmin = Double.parseDouble(tokens[1]);
		    	    ymax = Double.parseDouble(tokens[2]);
		    	    xmax = Double.parseDouble(tokens[1]);
		    	    ymin = Double.parseDouble(tokens[2]);
		    	    bw.write(line);
			    	bw.newLine();
		    	}
		    
				line = tokens[tokens.length - 1];
			
				for(int i = 1; i < (tokens.length - 1); i++)
					   line += (" " + tokens[i]);
			
		    	//line = tokens[10] + " " +tokens[1] + " " +tokens[2] + " " +tokens[3] + " " +tokens[4] + " " +tokens[5] + " " +tokens[6] + " " +	tokens[7] + " " +tokens[8] + " " +tokens[9];
			    
		    	if(Double.parseDouble(tokens[1]) < xmin){
		    		xmin = Double.parseDouble(tokens[1]);
		    	}
		    	
		    	if(Double.parseDouble(tokens[2]) > ymax){
		    		ymax = Double.parseDouble(tokens[2]);
		    	}
		    	
		    	if(Double.parseDouble(tokens[1]) > xmax){
		    		xmax = Double.parseDouble(tokens[1]);
		    	}
		    	
		    	if(Double.parseDouble(tokens[2]) < ymin){
		    		ymin = Double.parseDouble(tokens[2]);
		    	}
		    	
		    	if(Integer.parseInt(tokens[tokens.length - 1]) != soluid){
		    		solulkm++;
		    	}
		    	
		    	bw.write(line);
			    bw.newLine();

		    	id1=id2;

		    } 

		   bw.close();
		   br.close();
		   
		   File oldfile =new File(outputfilepath + "output_" + laskuri  + ".txt");
		   File newfile =new File(outputfilepath + "cells_" + Math.round(xmin) + "_" + Math.round(ymax) + "_" +  Math.round(xmax) + "_" + Math.round(ymin) +".txt");
           oldfile.renameTo(newfile);
		   
		} catch( IOException ioException ) {
            ioException.printStackTrace();
        }
}
}