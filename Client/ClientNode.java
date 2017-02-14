package edu.utdallas.cs6378.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.models.Identity;
import edu.utdallas.cs6378.models.Request;
import edu.utdallas.cs6378.utilities.Constants;
import edu.utdallas.cs6378.utilities.NodeProperties;

/**
 * This class has methods that determine the life cycle of a client node at a high
 * level. It has a constructor to initialize a client node with a name and number
 * of servers it needs to connect to. 
 * 
 * @Method: connectToServers()
 * This method connects to all the servers in the system based on properties file.
 * This method also starts all the threads that receive and send objects to those
 * servers. Whenever a node connects to another node, it sends an identity object
 * that has information about the connecting node.
 * 
 * @Method: startRequests()
 * This method makes sure that the client looks for "Begin" notification from 
 * master server that coordinates the distributed process. Once begin is received,
 * request generation is enabled by starting the thread that is responsible for
 * request generation and critical section execution.
 * 
 * @Method: lookForShutdownNotification()
 * This method looks for shutdown message from "Master" server. Once the shutdown
 * message is received, this method releases the control.
 * 
 * @Method: shutdown()
 * This method will look for all the live threads and interrupts them indicating
 * them to terminate. Once the threads are terminated, statistics are printed by
 * this method.
 * 
 */

public class ClientNode {
	
	private static Logger logger = Logger.getLogger(ClientNode.class);
			
	private Identity myId;
	
	private Socket[] connectionsToServers;
	private ConcurrentHashMap<String, ObjectSender> objectSendersToServers;
	private ConcurrentHashMap<String, GrantHandler> objectReceiversFromServers;
	private ExecuteCriticalSection executeCS;
	private ClientSendWrapper clientSendWrapper;
	
	public ClientNode(String clientName, int numOfServers) {
		myId = new Identity(clientName, Constants.CLIENT);
		connectionsToServers = new Socket[numOfServers+1];
		objectSendersToServers = new ConcurrentHashMap<String, ObjectSender>();
		objectReceiversFromServers = new ConcurrentHashMap<String, GrantHandler>();
		clientSendWrapper = new ClientSendWrapper(objectSendersToServers);
		executeCS = new ExecuteCriticalSection(clientSendWrapper);
	}
	
	public void connectToServers() {
		String[] serverNames = Constants.SERVERS;
		clientSendWrapper.start();
		for(int i = 0; i < serverNames.length; i++) {
			String ipAddress = NodeProperties.getProperty(serverNames[i]);
			String[] ipAndPort = ipAddress.split(Pattern.quote(":"));
			Socket connection = null;
			try {
				InetAddress inetAddress = InetAddress.getByName(ipAndPort[0]);
				connection = new Socket(inetAddress, Integer.parseInt(ipAndPort[1]));
				
				ObjectSender objSender = new ObjectSender(connection, 100);
				objSender.start();
				objSender.submitObject(myId);
				objectSendersToServers.put(serverNames[i], objSender);
				
				connectionsToServers[Integer.parseInt(serverNames[i].substring(1))] = connection;
				GrantHandler grantReceiver = new GrantHandler(connection, executeCS);
				grantReceiver.start();
				objectReceiversFromServers.put(serverNames[i], grantReceiver);
				
				logger.debug("Established connection to server "+serverNames[i]);
			} catch (Exception e) {
				if(e instanceof ConnectException) {
					try {
						if(connection != null) {
							connection.close();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					continue;
				} else {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void startRequests() {
		GrantHandler grantHandler = objectReceiversFromServers.get(Constants.MASTER);
		while(true) {
			if(grantHandler.getBeginReceived()) {
				executeCS.start();
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean lookForShutdownNotification() {
		GrantHandler grantHandler = objectReceiversFromServers.get(Constants.MASTER);
		while(true) {
			if(grantHandler.getShutdownReceived()) {
				logger.debug("Shutdown message received from master");
				return true;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void shutdown() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
		executeCS.interrupt();
		clientSendWrapper.interrupt();
		int numOfIdentitiesSent = 0;
		for(ObjectSender objSender:objectSendersToServers.values()) {
			objSender.interrupt();
			numOfIdentitiesSent = numOfIdentitiesSent + objSender.getNumOfIdentities();
		}
		
		logger.debug("**************** TIMEUNIT: "+String.format("%4d", Constants.TIMEUNIT)+" ms **********************************");
		logger.debug("**************** REQUESTS: "+String.format("%4d", Constants.NUMOFREQUESTS)+" from this client ***************");
		logger.debug("**************** MASTERSERVER: "+Constants.MASTER+" ***************************");
		int grantsForCriticalSection = 0;
		for(int i = 1; i <= Constants.NUMOFREQUESTS; i++) {
			Request request = Constants.requestStates.get(i).getRequest();
			grantsForCriticalSection = grantsForCriticalSection + request.getNumOfGrantsToEnterCriticalSection();
			logger.debug("Request: "+String.format("%2d", request.getRequestId())+", Received "+ request.getNumOfGrantsToEnterCriticalSection()
					+" server grants from: "+String.format("%-30s",request.getServersThatGranted())+"and latency: "+String.format("%5d", request.getLatency())+" ms");
			if(i%10 == 0) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		int totalMessagesReceived = 0;
		for(String server: Constants.SERVERS) {
			GrantHandler grantHandler = objectReceiversFromServers.get(server);
			totalMessagesReceived = totalMessagesReceived + grantHandler.getGrantsReceived() + grantHandler.getAuxiliaryMessages()
					+ grantHandler.getNumOfWriteResponses();
			logger.debug("from '"+server+"'- Total grants received: "+grantHandler.getGrantsReceived()
					+" total auxiliary messages received: "+grantHandler.getAuxiliaryMessages()
					+" and total write responses received: "+grantHandler.getNumOfWriteResponses());
		}
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("Total requests sent to all servers:          "+String.format("%4d", clientSendWrapper.getNumberOfRequests()));
		logger.debug("Total releases sent to all servers:          "+String.format("%4d", clientSendWrapper.getNumberOfReleases()));
		logger.debug("Total auxiliary messages sent to all servers:"+String.format("%4d", clientSendWrapper.getAuxiliaryMessages()));
		logger.debug("Total Requests to write:                     "+String.format("%4d", clientSendWrapper.getNumOfRequestsToWrite()));
		logger.debug("Total write calls:                           "+String.format("%4d", clientSendWrapper.getNumOfWriteCalls()));
		logger.debug("Total identity messages sent to all servers: "+String.format("%4d", numOfIdentitiesSent));
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("Total messages sent from this node:          "+String.format("%4d", clientSendWrapper.getNumberOfRequests()
																						+clientSendWrapper.getAuxiliaryMessages()
																						+clientSendWrapper.getNumberOfReleases()
																						+clientSendWrapper.getNumOfRequestsToWrite()
																						+clientSendWrapper.getNumOfWriteCalls()
																						+numOfIdentitiesSent));
		logger.debug("Total grants that enabled CS access:         "+String.format("%4d", grantsForCriticalSection));
		logger.debug("Total messages received at this node:        "+String.format("%4d", totalMessagesReceived));
		
		logger.debug("Client shutdown is complete!");
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public Identity getId() {
		return myId;
	}
}
