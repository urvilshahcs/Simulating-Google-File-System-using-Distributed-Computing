package edu.utdallas.cs6378.client;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import edu.utdallas.cs6378.models.Grant;
import edu.utdallas.cs6378.models.Identity;
import edu.utdallas.cs6378.models.Release;
import edu.utdallas.cs6378.models.Request;
import edu.utdallas.cs6378.models.RequestToWrite;
import edu.utdallas.cs6378.models.WriteResponse;

/**
 * This class functions as a object sender for client and server nodes and
 * there is an ObjectSender thread for each node that this node is 
 * connected to. 
 * 
 * @Method: run()
 * The types of messages that are sent are either 
 * 1) Requests, Releases, Identities and auxiliary "completion" messages by client nodes 
 * 2) Grants, Identities and auxiliary "begin" and "shutdown" messages by Server nodes
 * This class follows the producer-consumer pattern to accept the sending
 * jobs and subsequently send them in the order they are received.
 * 
 * @Method: submitObject()
 * This method is used to submit objects to this thread so that they will
 * be sent to the node on the other end.
 * 
 */

public class ObjectSender extends Thread {

	//private static Logger logger = Logger.getLogger(ObjectSender.class);
	
	private ArrayBlockingQueue<Object> objectsToBeSent;
	private Socket connection;
	private int numOfRequests;
	private int numOfReleases;
	private int numOfAuxiliaryMessages;
	private int numOfGrants;
	private int numOfIdentities;
	private int numOfReqForWrites;
	private int numOfWriteResponses;
	
	public ObjectSender(Socket connection, int maxObjects) {
		objectsToBeSent = new ArrayBlockingQueue<Object>(maxObjects);
		this.connection = connection;
		this.numOfAuxiliaryMessages = 0;
		this.numOfGrants = 0;
		this.numOfReleases = 0;
		this.numOfRequests = 0;
		this.numOfIdentities = 0;
		this.numOfReqForWrites = 0;
		this.numOfWriteResponses = 0;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * This method has been modified to count the number of request for writes(from clients) 
	 * and write responses(from servers), if any.
	 */
	@Override
	public void run() {
		try {
			OutputStream outStream = connection.getOutputStream();
			ObjectOutputStream objOutStream = new ObjectOutputStream(outStream);
			while(!isInterrupted()) {
				Object object;
				try {
					object = objectsToBeSent.take();
					if(object instanceof Request) {
						this.numOfRequests++;
						comment("Sent a request");
					} else if (object instanceof Release) {
						this.numOfReleases++;
						comment("Sent a release");
					} else if (object instanceof Grant) {
						this.numOfGrants++;
						comment("Sent a grant");
					} else if (object instanceof String) {
						this.numOfAuxiliaryMessages++;
					} else if (object instanceof Identity) {
						this.numOfIdentities++;
					} else if (object instanceof RequestToWrite) {
						this.numOfReqForWrites++;
					} else if (object instanceof WriteResponse) {
						this.numOfWriteResponses++;
					}
					
					objOutStream.writeObject(object);
				} catch (InterruptedException e) {
					comment("ObjectSender got interrupted and interrupt status is "+isInterrupted());
					Thread.currentThread().interrupt();
					//e.printStackTrace();
				}
			}
		} catch (IOException e) {
			//e.printStackTrace();
		}
		comment("ObjectSender has terminated!");
	}
	
	public int submitObject(Object object) {
		if(object != null) {
			try {
				objectsToBeSent.put(object);
			} catch (InterruptedException e) {
				return 0;
			}
			return 1;
		}
		return 0;
	}
	
	public int getNumOfRequests() {
		return numOfRequests;
	}

	public void setNumOfRequests(int numOfRequests) {
		this.numOfRequests = numOfRequests;
	}

	public int getNumOfReleases() {
		return numOfReleases;
	}

	public void setNumOfReleases(int numOfReleases) {
		this.numOfReleases = numOfReleases;
	}

	public int getNumOfAuxiliaryMessages() {
		return numOfAuxiliaryMessages;
	}

	public void setNumOfAuxiliaryMessages(int numOfAuxiliaryMessages) {
		this.numOfAuxiliaryMessages = numOfAuxiliaryMessages;
	}

	public int getNumOfGrants() {
		return numOfGrants;
	}

	public void setNumOfGrants(int numOfGrants) {
		this.numOfGrants = numOfGrants;
	}

	public int getNumOfIdentities() {
		return numOfIdentities;
	}

	public void setNumOfIdentities(int numOfIdentities) {
		this.numOfIdentities = numOfIdentities;
	}

	public int getNumOfReqForWrites() {
		return numOfReqForWrites;
	}

	public void setNumOfReqForWrites(int numOfReqForWrites) {
		this.numOfReqForWrites = numOfReqForWrites;
	}

	public int getNumOfWriteResponses() {
		return numOfWriteResponses;
	}

	public void setNumOfWriteResponses(int numOfWriteResponses) {
		this.numOfWriteResponses = numOfWriteResponses;
	}

	private void comment(String message) {
		//logger.debug(message);
	}
	
}
