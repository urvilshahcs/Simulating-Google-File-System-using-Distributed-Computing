package edu.utdallas.cs6378.models;

import java.io.Serializable;

public class RequestToWrite implements Serializable {
	
	private static final long serialVersionUID = -1174628394476508392L;
	private String forObject;
	private String fromClient;
	private String[] serversBeingRequested;
	private String stringToBeWritten;
	
	public RequestToWrite(String forObject, String fromClient, String stringToBeWritten) 
	{
		this.forObject = forObject;
		this.fromClient = fromClient;
		this.serversBeingRequested = new String[3];
		this.stringToBeWritten = stringToBeWritten;
	}
	
	public void addServersBeingRequested(String server1, String server2, String server3) 
	{
		this.serversBeingRequested[0] = server1;
		this.serversBeingRequested[1] = server2;
		this.serversBeingRequested[2] = server3;
	}

	public String getForObject() {
		return forObject;
	}

	public void setForObject(String forObject) {
		this.forObject = forObject;
	}

	public String getFromClient() {
		return fromClient;
	}

	public void setFromClient(String fromClient) {
		this.fromClient = fromClient;
	}

	public String[] getServersBeingRequested() {
		return serversBeingRequested;
	}

	public String getStringToBeWritten() {
		return stringToBeWritten;
	}

	public void setStringToBeWritten(String stringToBeWritten) {
		this.stringToBeWritten = stringToBeWritten;
	}

}
