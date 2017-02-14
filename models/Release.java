package edu.utdallas.cs6378.models;

import java.io.Serializable;
/**
 * This class is a Release model whose instance is used to release a
 * hold on server because of a grant issued for a particular request.
 * 
 */

public class Release implements Serializable {

	private static final long serialVersionUID = -613024894306893803L;
	
	private Integer requestId;
	private String requestedObject;
	private boolean[] releasesToServers;
	
	public Release(Integer requestId, boolean[] releasesToServers, String requestedObject) {
		this.requestId = requestId;
		this.releasesToServers = releasesToServers;
		this.requestedObject = requestedObject;
	}
	
	public Integer getRequestId() {
		return this.requestId;
	}

	public boolean[] getReleasesToServers() {
		return releasesToServers;
	}

	public void setReleasesToServers(boolean[] releasesToServers) {
		this.releasesToServers = releasesToServers;
	}

	public String getRequestedObject() {
		return requestedObject;
	}

	public void setRequestedObject(String requestedObject) {
		this.requestedObject = requestedObject;
	}
}
