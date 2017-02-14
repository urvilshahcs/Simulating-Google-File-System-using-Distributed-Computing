package edu.utdallas.cs6378.server;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.client.ObjectSender;
import edu.utdallas.cs6378.models.Identity;
import edu.utdallas.cs6378.utilities.Constants;
import edu.utdallas.cs6378.utilities.NodeProperties;
/**
 * This class has methods that determine the life cycle of a server node at a high
 * level. It has a constructor to initialize a server node with a name and server
 * port number
 * 
 * @Method: connectToOtherServers()
 * This method establishes connections to all other servers and starts all the 
 * ObjectReceiver and ObjectSender threads that correspond to those connections.
 * 
 * @Method: bootup()
 * This method starts accepting connections from other servers at the port number
 * mentioned in properties file. Once all the required nodes as per the properties
 * file connect to this server, it stops listening to connections. It also sends 
 * begin message once all the connections are established.
 * 
 * @Method: lookForCompletion()
 * This method is only used by master node. This is a blocking method that keeps 
 * looking for completion notifications from all the clients.
 * 
 * @Method: sendShutDownNotifications()
 * This method sends shutdown notifications to all the servers and clients that 
 * are connected. This is to be used by only the master node.
 * 
 * @Method: lookForShutdownNotification()
 * This method is used by server nodes that are different from master node and
 * this is a blocking call. This method looks for shutdown notification until it
 * is received
 * 
 * @Method: shutdown()
 * This method bring all the threads down by interrupting them and closing down
 * connections. Finally, this method prints all the necessary statistics.
 * 
 */

public class ServerNode {

	private static Logger logger = Logger.getLogger(ServerNode.class);

	private ServerSocket gatewayToLocalhost;
	
	private Identity myId;
	
	private ConcurrentHashMap<String, Socket> clientConnections;
	private ConcurrentHashMap<String, ObjectSender> objectSendersToClients;
	private ConcurrentHashMap<String, ObjectReceiver> objectReceiversFromClients;
	
	private ConcurrentHashMap<String, Socket> serverConnections;
	private ConcurrentHashMap<String, ObjectSender> objectSendersToServers;
	private ConcurrentHashMap<String, ObjectReceiver> objectReceiversFromServers;
	
	private ConcurrentHashMap<String, RequestProcessor> reqProcessors;
	private ConcurrentHashMap<String, WriteRequestProcessor> writeReqProcessors;
	private int serverPort;
	
	private boolean listening;

	public ServerNode(int serverPort, String serverName) {
		this.myId = new Identity(serverName, Constants.SERVER);
		this.serverPort = serverPort;
		this.clientConnections = new ConcurrentHashMap<String, Socket>();
		this.serverConnections = new ConcurrentHashMap<String, Socket>();
		this.objectReceiversFromClients = new ConcurrentHashMap<String, ObjectReceiver>();
		this.objectReceiversFromServers = new ConcurrentHashMap<String, ObjectReceiver>();
		this.objectSendersToClients = new ConcurrentHashMap<String, ObjectSender>();
		this.objectSendersToServers = new ConcurrentHashMap<String, ObjectSender>();
		this.reqProcessors = new ConcurrentHashMap<String, RequestProcessor>();
		this.writeReqProcessors = new ConcurrentHashMap<String, WriteRequestProcessor>();
	}

	/*
	 * This method has been modified for project 3 by adding a request processor corresponding to 
	 * each of the objects. Also, each of the object that is held at this server is associated
	 * with a writerequestprocessor which has exclusive access to perform writes on that object.
	 * 
	 * All of these newly added RequestProcessor and WriteRequestProcessor threads are interrupted
	 * in the shutdown method to facilitate graceful shutdown once the whole request generation is
	 * complete.
	 */
	public void bootup() {
		listening = true;
		try {
			
			for(int i = 0; i < Constants.OBJECTS.length; i++) {
				RequestProcessor reqProcessor = new RequestProcessor(objectSendersToClients);
				reqProcessors.put(Constants.OBJECTS[i], reqProcessor);
				reqProcessor.start();
			}
			
			int objectNum = Integer.parseInt(myId.getName().substring(1)) - 1;
			for(int i = 0; i < 3; i++) {
				String objectName = "O"+(((objectNum + 7 - i) % 7) + 1)+".txt";
				logger.debug(objectName);
				WriteRequestProcessor writeReqProcessor = new WriteRequestProcessor(objectName, objectSendersToClients);
				writeReqProcessor.start();
				this.writeReqProcessors.put(objectName, writeReqProcessor);
			}
			
			gatewayToLocalhost = new ServerSocket(serverPort);
			while(listening) {
				Socket connection = gatewayToLocalhost.accept();
				try {
					ObjectReceiver objReceiver = new ObjectReceiver(connection, reqProcessors, writeReqProcessors);
					objReceiver.start();
					Identity machineId = (Identity) objReceiver.getReceivedObj();
					if(machineId.getFunction().equals(Constants.CLIENT)) {
						clientConnections.put(machineId.getName(), connection);
						ObjectSender objSender = new ObjectSender(connection, 100);
						objSender.start();
						
						objectSendersToClients.put(machineId.getName(), objSender);
						objReceiver.setFromNode(machineId.getName());
						objectReceiversFromClients.put(machineId.getName(), objReceiver);
						logger.debug("Client "+machineId.getName()+" got connected!");
						if(clientConnections.size() == Constants.CLIENTS.length) {
							listening = false;
							if (Constants.MYNAME.equals(Constants.MASTER)) {
								for (ObjectSender value: objectSendersToClients.values()) {
									value.submitObject(Constants.BEGIN);
								}
								for (ObjectSender value: objectSendersToServers.values()) {
									value.submitObject(Constants.BEGIN);
								}
							}
						}
					} else {
						serverConnections.put(machineId.getName(), connection);
						ObjectSender objSender = new ObjectSender(connection, 100);
						objSender.start();
						
						objectSendersToServers.put(machineId.getName(), objSender);
						objectReceiversFromServers.put(machineId.getName(), objReceiver);
						logger.debug("Server "+machineId.getName()+" got connected!");
					}
				} catch (ClassCastException cce) {
					try {
						connection.close();
					} catch (IOException ioe) {
						logger.debug("Connections different from clients or servers will not be accepted! "
								+ "Any machine must send an identity when they get connected!");
						ioe.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void connectToOtherServers() {
		String servers = NodeProperties.getProperty("servers");
		if(servers == null || !servers.contains(myId.getName())) {
			logger.debug("Either or both of the properties \"servers\" or \"nodename\" is wrong! please rectify and restart this server");
		} else {
			String[] serverNames = servers.split(Pattern.quote(","));
			for(int i = 0; i < serverNames.length; i++) {
				if(!myId.getName().equals(serverNames[i])) {
					String ipAddress = NodeProperties.getProperty(serverNames[i]);
					String[] ipAndPort = ipAddress.split(Pattern.quote(":"));
					Socket connection = null;
					try {
						InetAddress inetAddress = InetAddress.getByName(ipAndPort[0]);
						connection = new Socket(inetAddress, Integer.parseInt(ipAndPort[1]));
						
						serverConnections.put(serverNames[i], connection);
						ObjectSender objSender = new ObjectSender(connection, 100);
						objSender.start();
						objSender.submitObject(myId);
						objectSendersToServers.put(serverNames[i], objSender);
						ObjectReceiver objReceiver = new ObjectReceiver(connection, reqProcessors, writeReqProcessors);
						objReceiver.start();
						objReceiver.setFromNode(serverNames[i]);
						objectReceiversFromServers.put(serverNames[i], objReceiver);
						
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
		}
	}
	
	public void shutdown() {
		try {
			
			if(Constants.MYNAME.equals(Constants.MASTER)) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			for(ObjectSender objSender: objectSendersToClients.values()) {
				objSender.interrupt();
			}
			for(ObjectSender objSender: objectSendersToServers.values()) {
				objSender.interrupt();
			}
			
			for(WriteRequestProcessor writeReqProcessor: writeReqProcessors.values()) {
				writeReqProcessor.interrupt();
			}
			
			for(Socket connection: clientConnections.values()) {
				try {
					connection.close();
				} catch(IOException ie) {
					ie.printStackTrace();
				}
			}
			for(Socket connection: serverConnections.values()) {
				try {
					if (!connection.isClosed()) {
						connection.close();
					}
				} catch(IOException ie) {
					ie.printStackTrace();
				}
			}
			
			for(String object: Constants.OBJECTS) {
				this.reqProcessors.get(object).interrupt();
			}
			
			this.gatewayToLocalhost.close();
			
			int totalMessagesSent = 0;
			int totalMessagesReceived = 0;
			for(String client: Constants.CLIENTS) {
				ObjectSender objSender = objectSendersToClients.get(client);
				totalMessagesSent = totalMessagesSent + objSender.getNumOfGrants() +
						objSender.getNumOfRequests() + objSender.getNumOfReleases() + 
						objSender.getNumOfAuxiliaryMessages() + objSender.getNumOfIdentities() +
						objSender.getNumOfReqForWrites() + objSender.getNumOfWriteResponses();
				logger.debug("Messages to client: '"+client+"', Grants:"+String.format("%3d", objSender.getNumOfGrants())
						+", Requests:"+String.format("%3d", objSender.getNumOfRequests())
						+", Releases:"+String.format("%3d", objSender.getNumOfReleases())
						+", WriteResponses:"+String.format("%3d", objSender.getNumOfWriteResponses())
						+", Auxiliary Messages:"+String.format("%3d", objSender.getNumOfAuxiliaryMessages())
						+", Identities:"+String.format("%3d", objSender.getNumOfIdentities())
						+", RequestsToWrite:"+String.format("%3d", objSender.getNumOfReqForWrites()));
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for(String server: Constants.SERVERS) {
				ObjectSender objSender = objectSendersToServers.get(server);
				if(objSender != null) {
					logger.debug("Messages to server: '"+server+"', Grants:"+String.format("%3d", objSender.getNumOfGrants())
							+", Requests:"+String.format("%3d", objSender.getNumOfRequests())
							+", Releases:"+String.format("%3d", objSender.getNumOfReleases())
							+", WriteResponses:"+String.format("%3d", objSender.getNumOfWriteResponses())
							+", Auxiliary Messages:"+String.format("%3d", objSender.getNumOfAuxiliaryMessages())
							+", Identities:"+String.format("%3d", objSender.getNumOfIdentities())
							+", RequestsToWrite:"+String.format("%3d", objSender.getNumOfReqForWrites()));
					totalMessagesSent = totalMessagesSent + objSender.getNumOfGrants() +
							objSender.getNumOfRequests() + objSender.getNumOfReleases() + 
							objSender.getNumOfAuxiliaryMessages() + objSender.getNumOfIdentities() +
							objSender.getNumOfReqForWrites() + objSender.getNumOfWriteResponses();
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for(String client: Constants.CLIENTS) {
				ObjectReceiver objReceiver = objectReceiversFromClients.get(client);
				totalMessagesReceived = totalMessagesReceived + objReceiver.getNumberOfRequests() + objReceiver.getNumberOfReleases()
						+ objReceiver.getNumberOfAuxiliaryMessages() + objReceiver.getNumberOfIdentities() + objReceiver.getNumberOfReqToWrites()
						+ objReceiver.getNumberOfWriteCalls();
				logger.debug("Messages from client: '"+client+"', Requests:"+String.format("%3d", objReceiver.getNumberOfRequests())
						+", Releases:"+String.format("%3d", objReceiver.getNumberOfReleases())
						+", RequestsToWrite:"+String.format("%3d", objReceiver.getNumberOfReqToWrites())
						+", WriteCalls:"+String.format("%3d", objReceiver.getNumberOfWriteCalls())
						+", Auxiliary Messages:"+String.format("%3d", objReceiver.getNumberOfAuxiliaryMessages())
						+", Identities:"+String.format("%3d", objReceiver.getNumberOfIdentities()));
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for(String server: Constants.SERVERS) {
				ObjectReceiver objReceiver = objectReceiversFromServers.get(server);
				if(objReceiver != null) {
					totalMessagesReceived = totalMessagesReceived + objReceiver.getNumberOfRequests() + objReceiver.getNumberOfReleases()
							+ objReceiver.getNumberOfAuxiliaryMessages() + objReceiver.getNumberOfIdentities() + objReceiver.getNumberOfReqToWrites()
							+ objReceiver.getNumberOfWriteCalls();
					logger.debug("Messages from server: '"+server+"', Requests:"+String.format("%3d", objReceiver.getNumberOfRequests())
							+", Releases:"+String.format("%3d", objReceiver.getNumberOfReleases())
							+", RequestsToWrite:"+String.format("%3d", objReceiver.getNumberOfReqToWrites())
							+", WriteCalls:"+String.format("%3d", objReceiver.getNumberOfWriteCalls())
							+", Auxiliary Messages:"+String.format("%3d", objReceiver.getNumberOfAuxiliaryMessages())
							+", Identities:"+String.format("%3d", objReceiver.getNumberOfIdentities()));
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			logger.debug("Total messages received on this node: "+String.format("%4d", totalMessagesReceived));
			logger.debug("Total messages sent on this node:     "+String.format("%4d", totalMessagesSent));
			logger.debug("Server shutdown is complete!");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			logger.debug("Error during server shutdown!");
			e.printStackTrace();
		}
	}
	
	public boolean lookForCompletion() {
		while(true) {
			boolean stop = true;
			for(int j=0; j<Constants.CLIENTS.length; j++) {
				stop = stop && Constants.COMPLETION_NOTIFICATIONS[j];
			}
			if(stop) {
				logger.debug("Completion notifications received from all clients!");
				return stop;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
		}
	}
	
	public boolean lookForShutdownNotification() {
		ObjectReceiver objReceiver = objectReceiversFromServers.get(Constants.MASTER);
		while(true) {
			if(objReceiver.getShutdownReceived()) {
				logger.debug("Shutdown message received from master");
				return true;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}
		}
	}
	
	public void sendShutDownNotifications() {
		for (ObjectSender objSender: objectSendersToClients.values()) {
			objSender.submitObject(Constants.SHUTDOWN);
		}
		for (ObjectSender objSender: objectSendersToServers.values()) {
			objSender.submitObject(Constants.SHUTDOWN);
		}
	}
	
}
