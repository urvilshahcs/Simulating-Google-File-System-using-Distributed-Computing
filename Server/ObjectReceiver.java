package edu.utdallas.cs6378.server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.models.Identity;
import edu.utdallas.cs6378.models.Release;
import edu.utdallas.cs6378.models.Request;
import edu.utdallas.cs6378.models.RequestToWrite;
import edu.utdallas.cs6378.models.WriteCall;
import edu.utdallas.cs6378.utilities.Constants;

/**
 * This thread functions as a receiver thread for server nodes and each connection
 * has a corresponding receiver thread associated with it. 
 * 
 * @Method: run()
 * A server node can receive either Request, Release, Identity or String messages.
 * When a request is received, it is submitted to RequestProcessor for the requests
 * to be processed. If it is a release message, the blocked reqProcessor thread is 
 * resumed to process the subsequent requests in the request Processor queue. 
 * The string messages that are received are either completion notification or shutdown messages.
 * 
 * @Method: getReceivedObj()
 * Whenever a connection is established, an identity object is expected as the first object and
 * it is ensured from using the blocking method getReceivedObj() which looks for identity
 * objects in the ArrayBlockingQueue receivedObjs.
 * 
 * This class also keeps track of all the messages received over this connection
 * and distinguishes them based on type of message.
 * 
 */
public class ObjectReceiver extends Thread {

	/*
	 * Since each server can simultaneously provide grants for different objects at the same
	 * time, We have to maintain different request processors corresponding to each of those
	 * 7 objects. Also, writes can be performed concurrently on all the objects that this server
	 * holds and Hence we maintain three WriteRequestProcessors corresponding to each of the
	 * objects held physically at this server (in our case it is 3). 
	 */
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(ObjectReceiver.class);
	private Socket connection;
	private String fromNode;
	//Modification for project 3
	private ConcurrentHashMap<String, RequestProcessor> reqProcessors;
	private ConcurrentHashMap<String, WriteRequestProcessor> writeReqProcessors;
	private boolean beginReceived;
	private boolean shutdownReceived;
	private ArrayBlockingQueue<Object> receivedObjs = new ArrayBlockingQueue<Object>(10);
	private int numberOfRequests;
	private int numberOfReleases;
	private int numberOfAuxiliaryMessages;
	private int numberOfIdentities;
	private int numberOfReqToWrites;
	private int numberOfWriteCalls;
	
	public ObjectReceiver(Socket connection,
			ConcurrentHashMap<String, RequestProcessor> reqProcessors,
			ConcurrentHashMap<String, WriteRequestProcessor> writeReqProcessors) {
		this.connection = connection;
		this.reqProcessors = reqProcessors;
		this.writeReqProcessors = writeReqProcessors;
		this.beginReceived = false;
		this.numberOfAuxiliaryMessages = 0;
		this.numberOfReleases = 0;
		this.numberOfRequests = 0;
		this.numberOfIdentities = 0;
		this.numberOfReqToWrites = 0;
		this.numberOfWriteCalls = 0;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * This method has been modified from project 2 to accept the RequestToWrite objects and WriteCall objects
	 * from clients. 
	 * When a requesttowrite object is received for a particular object, corresponding WriteRequestProcessor's 
	 * method processRequestToWrite is invoked which immediately sends an YES or NO to the client from which
	 * this requesttowrite is received.
	 * When a write call is received for a particular object to be written, the data would have already been staged
	 * in a field of its WriteRequestProcessor object. Once a WriteCall is submitted to WriteRequestProcessor, it
	 * will be processed by either committing or aborting as per the contents of WriteCall.
	 */
	@Override
	public void run() {
		try {
			InputStream inStream = connection.getInputStream();
			ObjectInputStream objInStream = new ObjectInputStream(inStream);
			while(!isInterrupted()) {
				Object obj = objInStream.readObject();
				if(obj instanceof Request) {
					this.numberOfRequests++;
					comment("Received a request from "+((Request)obj).getClient());
					//Modification for project 3
					reqProcessors.get(((Request)obj).getForObject()).submitRequest((Request) obj);
				} else if (obj instanceof Release) {
					this.numberOfReleases++;
					comment("Received a release for "+((Release)obj).getRequestId());
					//Modification for project 3
					reqProcessors.get(((Release)obj).getRequestedObject()).resumeProcessing();
				} else if (obj instanceof RequestToWrite) {
					RequestToWrite reqToWrite = (RequestToWrite) obj;
					writeReqProcessors.get(reqToWrite.getForObject()).processRequestToWrite(reqToWrite);
					this.numberOfReqToWrites++;
				} else if (obj instanceof WriteCall) {
					WriteCall writeCall = (WriteCall) obj;
					writeReqProcessors.get(writeCall.getForObject()).submitWriteCall(writeCall);
					this.numberOfWriteCalls++;
				} else if (obj instanceof String) {
					this.numberOfAuxiliaryMessages++;
					if(Constants.COMPLETION.equals(obj.toString())) {
						Constants.COMPLETION_NOTIFICATIONS[Integer.parseInt(fromNode.substring(1))-1] = true;
					} else if (Constants.SHUTDOWN.equals(obj.toString())) {
						this.shutdownReceived = true;
					} else if (Constants.BEGIN.equals(obj.toString())) {
						this.beginReceived = true;
					}
				} else if (obj instanceof Identity) {
					this.numberOfIdentities++;
					receivedObjs.put(obj);
				}
			}
		} catch(Exception e) {
			//e.printStackTrace();
		}
		comment("ObjectReceiver has terminated!");
	}
	
	public Object getReceivedObj() {
		try {
			return receivedObjs.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void setFromNode(String fromNode) {
		this.fromNode = fromNode;
	}
	
	public boolean getBeginReceived() {
		return beginReceived;
	}
	
	public boolean getShutdownReceived() {
		return shutdownReceived;
	}
	
	public int getNumberOfRequests() {
		return numberOfRequests;
	}

	public void setNumberOfRequests(int numberOfRequests) {
		this.numberOfRequests = numberOfRequests;
	}

	public int getNumberOfReleases() {
		return numberOfReleases;
	}

	public void setNumberOfReleases(int numberOfReleases) {
		this.numberOfReleases = numberOfReleases;
	}

	public int getNumberOfAuxiliaryMessages() {
		return numberOfAuxiliaryMessages;
	}

	public void setNumberOfAuxiliaryMessages(int numberOfAuxiliaryMessages) {
		this.numberOfAuxiliaryMessages = numberOfAuxiliaryMessages;
	}
	
	public int getNumberOfIdentities() {
		return this.numberOfIdentities;
	}

	public int getNumberOfReqToWrites() {
		return numberOfReqToWrites;
	}

	public void setNumberOfReqToWrites(int numberOfReqToWrites) {
		this.numberOfReqToWrites = numberOfReqToWrites;
	}

	public int getNumberOfWriteCalls() {
		return numberOfWriteCalls;
	}

	public void setNumberOfWriteCalls(int numberOfWriteCalls) {
		this.numberOfWriteCalls = numberOfWriteCalls;
	}

	private void comment(String message) {
		//logger.debug(message);
	}
	
}
