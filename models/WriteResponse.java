package edu.utdallas.cs6378.models;

import java.io.Serializable;
/*
 * This class functions as the model that is utilized to communicate the server response
 * with respect to its willingness to commit or not commit. This is also used to respond
 * with a "done" message that indicates that the requested action has been completed.
 * 
 * The field response can take either "yes", "no" or "done" messages.
 */
public class WriteResponse implements Serializable {

	private static final long serialVersionUID = -6439619480502114762L;
	private String server;
	private String response;
	public WriteResponse(String server, String response) {
		this.server = server;
		this.response = response;
	}
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	public String getResponse() {
		return response;
	}
	public void setResponse(String response) {
		this.response = response;
	}
}
