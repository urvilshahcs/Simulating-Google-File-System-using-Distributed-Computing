package edu.utdallas.cs6378.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
/**
 * This class is an independent class that has a main method which clears objects
 * to facilitate new demo. This is not part of the project specification
 * 
 */
public class CleanObjects {

	public static void main(String[] args) {
		for (int i = 0; i < 7; i++) {
			String server = "dc0"+(i + 1)+File.separator+"S" + (i + 1);
			if(args.length != 0) {
				server = "S" + (i + 1);
			}
			for (int j = 0; j < 3; j++) {
				String object = "O"+(((i + 7 - j) % 7) + 1)+".txt";
				BufferedWriter bufferedWriter = null;
				try {
					bufferedWriter = new BufferedWriter(new FileWriter(
							server + File.separator + object,
							false));
					bufferedWriter.write("");
					bufferedWriter.close();
				} catch(IOException ioe) {
					ioe.printStackTrace();
					try {
						bufferedWriter.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}

}
