package edu.utdallas.cs6378.models;

import java.io.Serializable;

/**
 * This class is a Grant model whose instance is returned as a grant for 
 * a given request object and granting server.
 * 
 */

public class Grant implements Serializable {

	private static final long serialVersionUID = -4709803816656372367L;
	
	private String fromServer;
	private Request forRequest;
	
	public Grant(String fromServer, Request forRequest) {
		this.fromServer = fromServer;
		this.forRequest = forRequest;
	}
	
	public String getFromServer() {
		return fromServer;
	}
	
	public Request getRequest() {
		return forRequest;
	}

}
