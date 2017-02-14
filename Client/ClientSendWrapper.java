package edu.utdallas.cs6378.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import edu.utdallas.cs6378.models.Release;
import edu.utdallas.cs6378.models.Request;
import edu.utdallas.cs6378.models.RequestToWrite;
import edu.utdallas.cs6378.models.WriteCall;
import edu.utdallas.cs6378.utilities.Constants;

/**
 * This class functions as a Wrapper to send objects to servers through 
 * ObjectSender threads. 
 * This follows a producer-consumer model of processing send requests using
 * an ArrayBlockingQueue. This class also keeps track of all of the 
 * messages being sent to the servers and distinguishes them based on
 * their instance type. 
 * 
 * @Method: submitObject()
 * this method accepts objects that need to be sent.
 * 
 * @Method: run()
 * This is a thread sub-class that executes run method when started
 * and it continues until it is interrupted by another thread.
 * In this method, requests are sent to all the servers and release
 * objects are sent only to those servers that are present inside
 * the release object. This is done because once a quorum grants a
 * request and the client enters critical section, the grants that 
 * are subsequently received from other servers are going to be 
 * immediately released. However, the grants from the quorum will 
 * be released only after the execution of critical section. Auxiliary
 * messages are sent only to master server based on the properties
 * file.
 * 
 */

public class ClientSendWrapper extends Thread {
	
	//private static Logger logger = Logger.getLogger(ClientSendWrapper.class);
	private ArrayBlockingQueue<Object> objects;
	private ConcurrentHashMap<String, ObjectSender> serverConnections;
	private int numberOfReleases;
	private int numberOfRequests;
	private int auxiliaryMessages;
	private int numOfRequestsToWrite;
	private int numOfWriteCalls;
	
	public ClientSendWrapper(ConcurrentHashMap<String, ObjectSender> serverConnections) {
		objects = new ArrayBlockingQueue<Object>(100);
		this.serverConnections = serverConnections;
		this.numberOfReleases = 0;
		this.numberOfRequests = 0;
		this.auxiliaryMessages = 0;
		this.numOfRequestsToWrite = 0;
		this.numOfWriteCalls = 0;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * This method has been modified to accept RequestToWrite objects and WriteCall objects that
	 * facilitate the 2-phase commit protocol
	 */
	@Override
	public void run() {
		while(!isInterrupted()) {
			try {
				Object object = objects.take();
				if(object instanceof Release) {
					Release release = (Release) object;
					for(int j = 1; j < release.getReleasesToServers().length; j++) {
						if(release.getReleasesToServers()[j]) {
							serverConnections.get("S"+j).submitObject(release);
							numberOfReleases++;
						}
					}
				} else if (object instanceof Request) {
					for(String key: serverConnections.keySet()) {
						serverConnections.get(key).submitObject(object);
						numberOfRequests++;
					}
				} else if (object instanceof RequestToWrite) {
					RequestToWrite reqForCommit = (RequestToWrite) object;
					for (String server:reqForCommit.getServersBeingRequested()) {
						serverConnections.get(server).submitObject(object);
						this.numOfRequestsToWrite++;
					}
				} else if (object instanceof WriteCall) {
					WriteCall writeCall = (WriteCall) object;
					for (String server:writeCall.getServersBeingRequested()) {
						serverConnections.get(server).submitObject(object);
						this.numOfWriteCalls++;
					}
				} else if (object instanceof String) {
					this.auxiliaryMessages++;
					serverConnections.get(Constants.MASTER).submitObject(object);
				}
				
			} catch (InterruptedException e) {
				comment("SendRelease thread has been interrupted and interrupt status is "+isInterrupted());
				Thread.currentThread().interrupt();
			}
		}
		comment("SendRelease has terminated!");
	}
	
	public void submitObject(Object object) {
		try {
			objects.put(object);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public int getNumberOfReleases() {
		return numberOfReleases;
	}

	public void setNumberOfReleases(int numberOfReleases) {
		this.numberOfReleases = numberOfReleases;
	}

	public int getNumberOfRequests() {
		return numberOfRequests;
	}

	public void setNumberOfRequests(int numberOfRequests) {
		this.numberOfRequests = numberOfRequests;
	}

	public int getAuxiliaryMessages() {
		return auxiliaryMessages;
	}

	public void setAuxiliaryMessages(int auxiliaryMessages) {
		this.auxiliaryMessages = auxiliaryMessages;
	}

	public int getNumOfRequestsToWrite() {
		return numOfRequestsToWrite;
	}

	public void setNumOfRequestsToWrite(int numOfRequestsToWrite) {
		this.numOfRequestsToWrite = numOfRequestsToWrite;
	}

	public int getNumOfWriteCalls() {
		return numOfWriteCalls;
	}

	public void setNumOfWriteCalls(int numOfWriteCalls) {
		this.numOfWriteCalls = numOfWriteCalls;
	}

	private void comment(String message) {
		//logger.debug(message);
	}
}
