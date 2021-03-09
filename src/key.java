import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;   
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class key{

	MessageDigest digest;
	byte[] encodedhash;

	String in;
	String out;

	String thisOut;

	public key(){


	}


	public void hashing(String input) throws NoSuchAlgorithmException{

		in = input.split("-")[0];
		out = input.split("-")[1];

		digest = MessageDigest.getInstance("SHA-256");
		encodedhash = digest.digest(in.getBytes(StandardCharsets.UTF_8));

		thisOut = bytesToHex(encodedhash);

	}



	public boolean isCorrect(){

		return out.equals(thisOut);

	}

	private static String bytesToHex(byte[] hash) {

	    StringBuffer hexString = new StringBuffer();

	    for (int i = 0; i < hash.length; i++) {

		    String hex = Integer.toHexString(0xff & hash[i]);

		    if(hex.length() == 1) 
		    	hexString.append('0');
		    hexString.append(hex);
	    }
	    return hexString.toString();
	}


}