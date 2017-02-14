package edu.utdallas.cs6378.client;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.models.Grant;
import edu.utdallas.cs6378.models.RequestState;
import edu.utdallas.cs6378.models.WriteResponse;
import edu.utdallas.cs6378.utilities.Constants;
/**
 * This class functions as a object receiver for client nodes and as the
 * client receives primarily grants, it is named as grantHandler. There is
 * one GrantHandler thread running for each of the servers to receive messages
 * from that server. 
 * 
 * @Method: run()
 * Client receives only either grants or string messages. String messages are
 * either begin or shutdown only. Based on these messages the boolean parameters 
 * are set and these can be used by other threads to check the current status.
 * When a grant a received, process grant method is used.
 * 
 * @Method: processGrant()
 * In processGrant, a request state is picked and checked whether it has reached
 * a quorum-grant status or is in critical section or has already finished execution
 * of critical section. updateState method will return true if it is reached a quorum
 * grant status with the current grant and it all other scenarios it will return
 * false. When it returns true, the corresponding request is submitted to ExecuteCS 
 * thread for executing critical section.
 * 
 * @Method: getBeginReceived()
 * returns true for thread that is receiving messages from master and master has sent
 * begin message.
 * 
 * @Method: getShutdownReceived()
 * returns true for thread that is receiving messages from master and master has sent
 * shutdown message.
 * 
 */
public class GrantHandler extends Thread {

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(GrantHandler.class);
	private Socket connection;
	private ExecuteCriticalSection executeCS;
	private boolean beginReceived;
	private boolean shutdownReceived;
	private int grantsReceived;
	private int auxiliaryMessages;
	private int numOfWriteResponses;
	
	public GrantHandler(Socket connection, ExecuteCriticalSection executeCS) {
		this.connection = connection;
		this.executeCS = executeCS;
		this.beginReceived = false;
		this.grantsReceived = 0;
		this.numOfWriteResponses = 0;
	}
	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * This method has been modified from project 2 to handle receipt of Server Responses to
	 * write requests.
	 */
	@Override
	public void run() {
		try {
			comment("grant Handler has been started!");
			InputStream inStream = connection.getInputStream();
			ObjectInputStream objInStream = new ObjectInputStream(inStream);
			while(!isInterrupted()) {
				Object obj = objInStream.readObject();
				if(obj instanceof Grant) {
					this.grantsReceived++;
					Grant grantObj = (Grant) obj;
					comment("Received a grant from "+grantObj.getFromServer()+ " for request "+grantObj.getRequest().getRequestId());
					processGrant(grantObj);
				} else if (obj instanceof WriteResponse) {
					WriteResponse response = (WriteResponse) obj;
					executeCS.submitServerResponse(response);
					this.numOfWriteResponses++;
				} else if(obj instanceof String) {
					this.auxiliaryMessages++;
					if(Constants.BEGIN.equals(obj.toString())) {
						this.beginReceived = true;
					} else if(Constants.SHUTDOWN.equals(obj.toString())) {
						this.shutdownReceived = true;
					}
				}
			}
		} catch(Exception e) {
			//e.printStackTrace();
		}
		comment("GrantHandler is closed!");
	}
	
	private void processGrant(Grant grant) {
		RequestState rState = Constants.requestStates.get(grant.getRequest().getRequestId());
		if(rState != null && rState.updateState(Integer.parseInt(grant.getFromServer().substring(1)))) {
			comment("Request "+grant.getRequest().getRequestId()+ " has been granted!");
			executeCS.submitGrantedRequests(grant.getRequest());
		}
	}
	
	public boolean getBeginReceived() {
		return beginReceived;
	}
	
	public boolean getShutdownReceived() {
		return shutdownReceived;
	}
	
	public int getGrantsReceived() {
		return grantsReceived;
	}

	public void setGrantsReceived(int grantsReceived) {
		this.grantsReceived = grantsReceived;
	}

	public int getAuxiliaryMessages() {
		return auxiliaryMessages;
	}

	public void setAuxiliaryMessages(int auxiliaryMessages) {
		this.auxiliaryMessages = auxiliaryMessages;
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
