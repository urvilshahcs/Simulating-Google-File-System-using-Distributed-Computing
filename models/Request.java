package edu.utdallas.cs6378.models;

import java.io.Serializable;

/**
 * This class is a Request model and its instance is used to perform 
 * a request operation to enter into critical section. The attributes 
 * are as follows:
 * 
 * reqGenTimeStamp = Timestamp at which request is generated
 * fromClient = The client name that is generating the request
 * requestId = Each request is associated with a number with in a given client
 * reqGrantedTimeStamp = Timestamp at which a quorum of server nodes have given grants to this request
 * latency = Time elapsed between the time of request generation and request granted. reqGrantedTimeStamp - reqGenTimeStamp
 * serversThatGranted = The quorum of servers that granted requests in the order specified in this string
 * numOfGrantsToEnterCriticalSection = number of servers that acted as quorum
 * 
 * The objects of this class are comparable based on timestamp and if 
 * timestamp is equal, they are compared based on name of the client.
 * 
 */

public class Request implements Comparable<Request>, Serializable {
	
	private static final long serialVersionUID = 2617222019976458958L;
	private long reqGenTimeStamp;
	private String fromClient;
	private String forObject;
	private Integer requestId;
	private long reqGrantedTimeStamp;
	private long latency;
	private String serversThatGranted;
	private int numOfGrantsToEnterCriticalSection;
	
	public Request(long reqGenTimeStamp, String fromClient, int requestId, String forObject) {
		this.reqGenTimeStamp = reqGenTimeStamp;
		this.fromClient = fromClient;
		this.requestId = requestId;
		this.forObject = forObject;
	}
	
	public long getTimeStamp() {
		return reqGenTimeStamp;
	}
	
	public Integer getRequestId() {
		return requestId;
	}
	
	public String getClient() {
		return fromClient;
	}

	public long getReqGrantedTimeStamp() {
		return reqGrantedTimeStamp;
	}

	public void setReqGrantedTimeStamp(long reqGrantedTimeStamp) {
		this.reqGrantedTimeStamp = reqGrantedTimeStamp;
		this.latency = this.reqGrantedTimeStamp - this.reqGenTimeStamp;
	}

	public long getLatency() {
		return latency;
	}

	public void setLatency(long latency) {
		this.latency = latency;
	}

	public String getServersThatGranted() {
		return serversThatGranted;
	}

	public void setServersThatGranted(String serversThatGranted) {
		this.serversThatGranted = serversThatGranted;
	}

	public int getNumOfGrantsToEnterCriticalSection() {
		return numOfGrantsToEnterCriticalSection;
	}

	public void setNumOfGrantsToEnterCriticalSection(
			int numOfGrantsToEnterCriticalSection) {
		this.numOfGrantsToEnterCriticalSection = numOfGrantsToEnterCriticalSection;
	}

	public String getForObject() {
		return forObject;
	}

	public void setForObject(String forObject) {
		this.forObject = forObject;
	}

	@Override
	public int compareTo(Request o) {
		if(reqGenTimeStamp < o.reqGenTimeStamp) {
			return -1;
		} else if (reqGenTimeStamp == o.reqGenTimeStamp) {
			return fromClient.compareTo(o.fromClient);
		} else {
			return 1;
		}
	}
	
}
