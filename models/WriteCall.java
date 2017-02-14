package edu.utdallas.cs6378.models;

import java.io.Serializable;
/*
 * This class is the model that conveys the action to be performed
 * by a server with the staged data from the request to write message sent earlier.
 * The action could be either "commit" or "abort" based on the class field.
 * 
 * The field serversBeingRequested facilitates in identifying the destination
 * of the particular instance of the class. forObject refers to the object 
 * on the server that is being modified.
 */
public class WriteCall implements Serializable {
	
	private static final long serialVersionUID = 1983533900181540820L;
	private String action;
	private String fromClient;
	private String forObject;
	private String[] serversBeingRequested;
	
	
	public WriteCall(String action, String fromClient, String forObject) {
		this.action = action;
		this.fromClient = fromClient;
		this.forObject = forObject;
		this.serversBeingRequested = new String[3];
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
	
	public String getFromClient() {
		return fromClient;
	}

	public void setFromClient(String fromClient) {
		this.fromClient = fromClient;
	}

	public String getForObject() {
		return forObject;
	}

	public void setForObject(String forObject) {
		this.forObject = forObject;
	}

	public void addServersBeingRequested(String server1, String server2, String server3) 
	{
		this.serversBeingRequested[0] = server1;
		this.serversBeingRequested[1] = server2;
		this.serversBeingRequested[2] = server3;
	}
	
	public String[] getServersBeingRequested() {
		return serversBeingRequested;
	}
}
