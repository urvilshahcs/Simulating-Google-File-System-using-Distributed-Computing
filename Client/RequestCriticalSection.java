package edu.utdallas.cs6378.client;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.models.Request;
import edu.utdallas.cs6378.models.RequestState;
import edu.utdallas.cs6378.utilities.Constants;

/**
 * This class can be used to generate requests independently irrespective of
 * the status of previously sent requests.
 * 
 * This class is not being used in the current implementation as the requirements
 * specify that a new request has to be generated after the first request is granted.
 * 
 */

public class RequestCriticalSection implements Runnable {

	private static Logger logger = Logger.getLogger(RequestCriticalSection.class);
	private ConcurrentHashMap<String, ObjectSender> serverConnections;
	private ClientSendWrapper sendRelease;
	
	public RequestCriticalSection(ConcurrentHashMap<String, ObjectSender> serverConnections, ClientSendWrapper sendRelease) {
		this.serverConnections = serverConnections;
		this.sendRelease = sendRelease;
	}
	
	
	@Override
	public void run() {
		int numOfRequests = Constants.NUMOFREQUESTS;
		try {
			Random randGenerator = new Random();
			int[] values = {1, 2, 3, 4, 5};
			int randomIndex = 5;
			logger.debug("Started requesting critical section!");
			for(int i = 0; i < numOfRequests; i++) {
				Request newRequest = new Request(System.currentTimeMillis(), Constants.MYNAME, i+1, Constants.OBJECTS[Math.abs(randGenerator.nextInt()%7)]);
				Constants.requestStates.put(newRequest.getRequestId(), new RequestState(serverConnections.size(), newRequest, sendRelease));
				for(String key: serverConnections.keySet()) {
					serverConnections.get(key).submitObject(newRequest);
				}
				
				int randomNumber = Math.abs((randGenerator.nextInt() % randomIndex));
				Thread.sleep((5 + values[randomNumber])*Constants.TIMEUNIT);
				
				int temp = values[--randomIndex];
				values[randomIndex] = values[randomNumber];
				values[randomNumber] = temp;
				
				if(randomIndex == 0) {
					randomIndex = 5;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			logger.debug("Request generated caused an exception: "+e.getMessage());
		}
		comment("RequestCriticalSection has terminated!");
	}
	
	private void comment(String message) {
		//logger.debug(message);
	}

}
