package edu.utdallas.cs6378.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.client.ObjectSender;
import edu.utdallas.cs6378.models.RequestToWrite;
import edu.utdallas.cs6378.models.WriteCall;
import edu.utdallas.cs6378.models.WriteResponse;
import edu.utdallas.cs6378.utilities.Constants;

/**
 * This class has methods that provide exclusive access to an object stored at this server.
 * Only an instance of this class associated with the object can perform a write to that
 * object. Function of each of the methods in this class are explained at the function
 * implementation 
 * 
 */

public class WriteRequestProcessor extends Thread {

	private static Logger logger = Logger.getLogger(WriteRequestProcessor.class);
	private ConcurrentHashMap<String, ObjectSender> clientConnections;
	private String objectName;
	private ArrayBlockingQueue<WriteCall> queue = new ArrayBlockingQueue<WriteCall>(10);
	private Random randomGenerator = new Random();
	private String stagedStringToBeWritten;
	
	public WriteRequestProcessor(String objectName, ConcurrentHashMap<String, ObjectSender> clientConnections) {
		this.objectName = objectName;
		this.clientConnections = clientConnections;
		this.stagedStringToBeWritten = null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * This method is responsible for performing either a commit or abort based on the communication
	 * received from client through an instance of WriteCall class. 
	 * 
	 * Message to be written/committed is obtained when an instance of RequestToWrite is received from
	 * the client. That message is staged in the instance variable "stagedStringToBeWritten".
	 * 
	 * As soon as the action is performed, a "done" response is sent to the client which issued request
	 * to write to this object. Please note that ObjectSenders to clients are accessible to each and 
	 * every instance of this object from the instance variable clientConnections. 
	 */
	@Override
	public void run() {
		while(!isInterrupted()) {
			try {
				WriteCall writeCall = queue.take();
				if (Constants.COMMIT.equals(writeCall.getAction()) && stagedStringToBeWritten != null) {
					BufferedWriter bufferedWriter = null;
					try {
						bufferedWriter = new BufferedWriter(new FileWriter(
								Constants.MYNAME + File.separator + objectName,
								true));
						bufferedWriter.write(this.stagedStringToBeWritten);
						bufferedWriter.write("\n");
						bufferedWriter.close();
						logger.debug("Writing message \""+this.stagedStringToBeWritten+"\" to "+objectName);
					} catch(IOException ioe) {
						ioe.printStackTrace();
						try {
							bufferedWriter.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				} else {
					logger.debug("Aborting message \""+stagedStringToBeWritten+"\" to "+this.objectName);
				}
				this.stagedStringToBeWritten = null;
				this.clientConnections.get(writeCall.getFromClient()).submitObject(new WriteResponse(Constants.MYNAME, Constants.DONE));
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	/*
	 * WriteCall is a message that is received from the client that has information about the action to be taken
	 * with respect to staged data. These messages are put into a blocking queue which is accessed by the run method
	 * to process them.
	 */
	public void submitWriteCall(WriteCall writeCall) {
		try {
			queue.put(writeCall);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * RequestToWrite objects contain the message to be written to the object and have to be responded to with an yes
	 * or no message. Probability of yes is parameterized through properties. A random number generator is used to 
	 * generate a number uniformly from 0 to 99. Suppose the probability is 0.95. If the generated number is less than 95
	 * a yes message is sent otherwise a no message is sent. This corresponds to probability of 0.95 for an yes. 
	 * 
	 * The messages can be submitted to clients as this object has access to object senders to the clients. 
	 */
	public void processRequestToWrite(RequestToWrite requestToWrite) {
		int randomNum = randomGenerator.nextInt(100);
		this.stagedStringToBeWritten = requestToWrite.getStringToBeWritten();
		if(randomNum < 100 * Constants.PROBABILITYOFYES) {
			this.clientConnections.get(requestToWrite.getFromClient()).submitObject(new WriteResponse(Constants.MYNAME, Constants.YES));
		} else {
			this.clientConnections.get(requestToWrite.getFromClient()).submitObject(new WriteResponse(Constants.MYNAME, Constants.NO));
		}
	}
}
