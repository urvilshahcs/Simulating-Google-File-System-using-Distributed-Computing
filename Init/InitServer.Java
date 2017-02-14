package edu.utdallas.cs6378.init;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.client.ClientNode;
import edu.utdallas.cs6378.server.ServerNode;
import edu.utdallas.cs6378.utilities.Constants;

/**
 * This is the main class that is used to start the node. If the properties
 * specify the function of the node as client, ClientNode class is used to start
 * the node otherwise ServerNode class is used to start the node. 
 * 
 * @Method: startServer()
 * 1) For server startup, it looks for other servers that are up and running and 
 * connects to those servers using @Method:connectToOtherServers(). Then it starts
 * listening for connection attempts from other nodes. It will terminate listening
 * once all the client and server nodes specified in the properties file are connected
 * to it. Whenever a new connection is made or accepted, sender and receiver threads 
 * are started. Please note that all the servers have to be up and running before starting 
 * a client because a client will attempt to connect to all servers. If it is a master
 * server, it will look for completion messages from client nodes and based on that, it
 * will send the shut down notifications to the other servers and clients. If it is 
 * any other server, it will start looking for shutdown notification from master as
 * soon as all the other nodes get connected to it. This is done using a blocking method
 * call and once shutdown is received, it proceeds to shutdown.
 * 2) For client startup, it connects to all servers and starts looking for begin message
 * inside the method @Method: startRequests() and once it is received, it will start
 * generating requests using an instance of ExecuteCriticalSection thread. Simultaneously, 
 * the main thread starts looking for shutdown notification from master through a blocking
 * method @Method: lookForShutdownNotification(). Once it is received it proceeds to shutdown
 * where all the required statistics are printed.
 * 
 */

public class InitServer {

	private static Logger logger = Logger.getLogger(InitServer.class);
	public void startServer() {
		logger.debug("Node '"+Constants.MYNAME+"' has started!");
		if(Constants.SERVER.equals(Constants.MYFUNCTION)) {
			ServerNode server = new ServerNode(Constants.LISTENINGPORT, Constants.MYNAME);
			server.connectToOtherServers();
			server.bootup();
			if(Constants.MYNAME.equals(Constants.MASTER)) {
				server.lookForCompletion();
				server.sendShutDownNotifications();
			} else {
				server.lookForShutdownNotification();
			}
			server.shutdown();
		} else {
			ClientNode client = new ClientNode(Constants.MYNAME, Constants.SERVERS.length);
			client.connectToServers();
			client.startRequests();
			client.lookForShutdownNotification();
			client.shutdown();
		}
	}
	
	public static void main(String[] args) {
		InitServer initializeServer = new InitServer();
		initializeServer.startServer();
	}
}
