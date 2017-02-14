package edu.utdallas.cs6378.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.client.ObjectSender;
import edu.utdallas.cs6378.models.Grant;
import edu.utdallas.cs6378.models.Request;
import edu.utdallas.cs6378.utilities.Constants;
/**
 * This class RequestProcessor accepts requests and processes them one by one.
 * This is a thread sub-class that is blocked when a grant is sent to a client.
 * It is unblocked only when a release is received in ObjectReceiver which calls
 * the method resumeProcessing()
 * 
 * @Method: run()
 * this is a synchronized method that is blocked once a grant is sent. All the 
 * requests are polled from ConcurrentSkipListSet which maintains the requests
 * in order as the Request objects are comparable.
 * 
 * @Method: submitRequest()
 * This is a method that accepts requests to be processed.
 * 
 * @Method: resumeProcessing()
 * This method unblocks the run() method that was previously blocked when grant
 * was sent. This method should be used only when a release is received.
 * 
 */

public class RequestProcessor extends Thread {
	
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(RequestProcessor.class);
	private ConcurrentSkipListSet<Request> requests;
	private ConcurrentHashMap<String, ObjectSender> clientConnections;
	
	public RequestProcessor(ConcurrentHashMap<String, ObjectSender> clientConnections) {
		requests = new ConcurrentSkipListSet<Request>();
		this.clientConnections = clientConnections;
	}
	
	@Override
	public synchronized void run() {
		while(!isInterrupted()) {
			try {
				Request request = requests.pollFirst();
				if (request != null) {
					comment("Sending grant for request "+request.getRequestId()+" to client "+request.getClient());
					clientConnections.get(request.getClient()).submitObject(new Grant(Constants.MYNAME, request));
					this.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		comment("RequestProcessor has terminated!");
	}
	
	public void submitRequest(Request request) {
		try {
			comment("Received request "+request.getRequestId()+" from "+request.getClient());
			requests.add(request);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void resumeProcessing() {
		this.notify();
	}
	
	private void comment(String message) {
		//logger.debug(message);
	}
}
