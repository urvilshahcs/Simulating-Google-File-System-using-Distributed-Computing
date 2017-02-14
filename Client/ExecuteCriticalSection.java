package edu.utdallas.cs6378.client;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.models.Release;
import edu.utdallas.cs6378.models.Request;
import edu.utdallas.cs6378.models.RequestToWrite;
import edu.utdallas.cs6378.models.RequestState;
import edu.utdallas.cs6378.models.WriteCall;
import edu.utdallas.cs6378.models.WriteResponse;
import edu.utdallas.cs6378.utilities.Constants;


/**
 * This class is a thread sub-class that has methods to handle the quorum-granted
 * requests and execute the critical section for the corresponding request. It employs
 * producer consumer model by using ArrayBlockingQueue to accept grantedRequests and
 * process them.
 * 
 * @Method: run()
 * This method is executed when the thread is started and it terminates only when it is
 * interrupted by another thread. It reads the parameter about the number of requests
 * to be generated from the properties and generates the requests. A request is generated
 * and once quorum-grant is received, it executes critical section. After execution, it will
 * wait for 5-10 time units and generates another request and waits till another quorum-grant
 * is received. The wait is implemented by the use of take() method in arrayblockingqueue.
 * 
 * @Method: criticalSection()
 * This method is the critical section
 * 
 * @Method: submitGrantedRequests()
 * This method can be used to submit the quorum-granted requests for which critical section
 * can be executed.
 * 
 */

public class ExecuteCriticalSection extends Thread {
	
	private static Logger logger = Logger.getLogger(ExecuteCriticalSection.class);
	private ArrayBlockingQueue<Request> grantedRequests;
	private ClientSendWrapper clientSendWrapper;
	private int numOfRequestsGranted;
	private int numOfRequestsToBeIssued;
	private ArrayList<WriteResponse> serverResponses = new ArrayList<WriteResponse>();
	
	public ExecuteCriticalSection(ClientSendWrapper clientSendWrapper) {
		grantedRequests = new ArrayBlockingQueue<Request>(10);
		this.clientSendWrapper = clientSendWrapper;
		this.numOfRequestsGranted = 0;
		this.numOfRequestsToBeIssued = Constants.NUMOFREQUESTS;
	}
	
	@Override
	public void run() {
		Random randGenerator = new Random();
		
		
		int randomIndex = Constants.UPPERBOUND - Constants.LOWERBOUND;
		int[] values = new int[randomIndex];
		for(int i = 0; i < randomIndex; i++) {
			values[i] = i;
		}
		logger.debug("Started requesting critical section!");
		while(!isInterrupted()) {
			try {
				if(numOfRequestsToBeIssued > 0) {
					numOfRequestsToBeIssued--;
					
					Request newRequest = new Request(System.currentTimeMillis(), Constants.MYNAME, 
							Constants.NUMOFREQUESTS - numOfRequestsToBeIssued, Constants.OBJECTS[Math.abs(randGenerator.nextInt()%7)]);
					Constants.requestStates.put(newRequest.getRequestId(), 
							new RequestState(Constants.SERVERS.length, newRequest, clientSendWrapper));
					clientSendWrapper.submitObject(newRequest);
				}
				Request request = grantedRequests.take();
				criticalSection(request);
				this.numOfRequestsGranted++;
				clientSendWrapper.submitObject(new Release(request.getRequestId(), 
						Constants.requestStates.get(request.getRequestId()).releasesToServers(), request.getForObject()));
				if(this.numOfRequestsGranted == Constants.NUMOFREQUESTS) {
					clientSendWrapper.submitObject(Constants.COMPLETION);
				}
				int randomNumber = Math.abs((randGenerator.nextInt() % randomIndex));
				Thread.sleep((Constants.LOWERBOUND + values[randomNumber])*Constants.TIMEUNIT);
				
				int temp = values[--randomIndex];
				values[randomIndex] = values[randomNumber];
				values[randomNumber] = temp;
				
				if(randomIndex == 0) {
					randomIndex = Constants.UPPERBOUND - Constants.LOWERBOUND;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
		}
		comment("ExecuteCriticalSection is closed!");
	}
	
	/*
	 * This method has been modified to communicate with the servers in critical section. In this communication,
	 * it sends a RequestToWrite object to the corresponding servers that hold an object that has been randomly
	 * chosen and waits for the response from those servers. Once all the servers that hold the object respond
	 * with an yes, client sends commit message to all the servers which will execute the write action with the
	 * data received earlier. If all of the servers do respond with an "YES", client sends an abort message to all
	 * the servers and servers will drop the message that was not committed. Client exits the critical section once
	 * all servers sends a confirmation of commit or abort encoded as "done" message.
	 */
	private synchronized void criticalSection(Request request) {
		
		try {
			int objectNum = Integer.parseInt(request.getForObject().substring(1,2)) - 1;
			String message = "Message at timestamp: " + System.currentTimeMillis()
					+ " by client " + Constants.MYNAME;
			logger.debug("Sending request to write \""+message+"\" in " +request.getForObject());
			RequestToWrite reqForWrite = new RequestToWrite(request.getForObject(), Constants.MYNAME, message);
			reqForWrite.addServersBeingRequested("S"+(((objectNum)%7)+1), "S"+(((objectNum+1)%7)+1), "S"+(((objectNum+2)%7)+1));
			clientSendWrapper.submitObject(reqForWrite);
			this.wait();
			
			int numOfYESes = 0;
			StringBuffer serversRejected = new StringBuffer();
			for (WriteResponse aResponse: serverResponses) {
				if(Constants.YES.equals(aResponse.getResponse())) {
					numOfYESes++;
				} else {
					serversRejected.append("'").append(aResponse.getServer()).append("' ");
				}
			}
			
			WriteCall writeCall = null;
			if (numOfYESes == 3) {
				logger.debug("Received YES from all the servers! Sending commit message");
				writeCall = new WriteCall(Constants.COMMIT,
						Constants.MYNAME, request.getForObject());
			} else {
				logger.debug("Received NO to write from servers "+serversRejected+" Aborting write!");
				writeCall = new WriteCall(Constants.ABORT,
						Constants.MYNAME, request.getForObject());
			}
			writeCall.addServersBeingRequested("S"+(((objectNum)%7)+1), "S"+(((objectNum+1)%7)+1), "S"+(((objectNum+2)%7)+1));
			serverResponses.clear();
			clientSendWrapper.submitObject(writeCall);
			this.wait();
			serverResponses.clear();
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	private synchronized void resumeCriticalSection() {
		this.notify();
	}
	
	/*
	 * This method accepts the server responses after requesttowrite has been issued in the 
	 * critical section. Client waits until the response from all three servers is received and notifies
	 * the critical section thread that 3 server responses have arrived.
	 */
	public synchronized void submitServerResponse(WriteResponse response) {
		serverResponses.add(response);
		if(serverResponses.size() == 3) {
			resumeCriticalSection();
		}
	}
	
	public void submitGrantedRequests(Request request) {
		try {
			grantedRequests.put(request);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void comment(String message) {
		//logger.debug(message);
	}
	
}
