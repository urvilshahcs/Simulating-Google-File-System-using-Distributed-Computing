package edu.utdallas.cs6378.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
/**
 * This class loads the properties file. 
 * 
 * @Method: getProperty()
 * This method is used to access the property by passing the parameter as the 
 * property name
 * 
 */

public class NodeProperties {

	private static Logger logger = Logger.getLogger(NodeProperties.class);
	private static Properties nodeProperties = new Properties();
	private static String propertiesFile;

	static {
		propertiesFile = "resources"+File.separator+"server.properties";
		FileReader fReader = null;
		try {
			fReader = new FileReader(propertiesFile);
			nodeProperties.load(fReader);
			logger.debug("Properties are loaded into server's context");
		} catch (FileNotFoundException e) {
			logger.error("Properties file name \"" + propertiesFile
					+ "\"is not valid ");
		} catch (IOException ioe) {
			logger.error("IOException terminated loading properties"
					+ ioe.getMessage());
		} catch (Exception e) {
			StackTraceElement[] ste = e.getStackTrace();
			for (int i = 0; i < ste.length; i++) {
				logger.error(ste[i]);
			}
			e.printStackTrace();
		} finally {
			try {
				fReader.close();
			} catch (IOException e) {

			}
		}
	}
	
	public static String getProperty(String key) {
		return (String) nodeProperties.get(key);
	}
}
