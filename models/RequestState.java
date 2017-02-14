package edu.utdallas.cs6378.models;

import org.apache.log4j.Logger;

import edu.utdallas.cs6378.client.ClientSendWrapper;

/**
 * This class is a RequestState model that is used to maintain the status of a request.
 * 
 * @Method: updateState()
 * This method returns true if the quorum condition has just been fulfilled otherwise false
 * in all other cases. It will use checkState() method to confirm if the quorum condition
 * has been satisfied. Once a request is granted, subsequent grants are immediately released 
 * to those servers.
 * 
 * @Method: checkState()
 * This method checks if a quorum of nodes has granted the request recursively.
 * 
 * @Method: releaseToServers()
 * This method returns all those servers that granted request before it was considered to be
 * granted by a quorum of nodes.
 * 
 */
public class RequestState {
	
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(RequestState.class);
	private boolean requestGranted;
	private final boolean[] serverGrantStatus;
	private final String[] orderOfGrants;
	private int index;
	private final int numOfServers;
	private final Request request;
	private final ClientSendWrapper clientSendWrapper;
	
	public RequestState(int numOfServers, Request request, ClientSendWrapper clientSendWrapper) {
		requestGranted = false;
		this.numOfServers = numOfServers + 1;
		serverGrantStatus = new boolean[this.numOfServers];
		index = 1;
		orderOfGrants = new String[this.numOfServers];
		this.request = request;
		this.clientSendWrapper = clientSendWrapper;
	}
	
	public synchronized boolean updateState(int serverNumber) {
		if(!requestGranted) {
			serverGrantStatus[serverNumber] = true;
			orderOfGrants[index] = "'S"+serverNumber+"' ";
			index++;
			if(checkState(1)) {
				requestGranted = true;
				StringBuffer sbuffer = new StringBuffer();
				for(int i = 1; i < index; i++) {
					sbuffer.append(orderOfGrants[i]);
				}
				request.setReqGrantedTimeStamp(System.currentTimeMillis());
				request.setServersThatGranted(sbuffer.toString());
				request.setNumOfGrantsToEnterCriticalSection(index-1);
				//logger.debug("Grants came from "+sbuffer.toString()+"for request "+request.getRequestId());
				return true;
			} else {
				return false;
			}
		} else {
			boolean[] copyGrantStatus = new boolean[this.numOfServers];
			copyGrantStatus[serverNumber] = true;
			clientSendWrapper.submitObject(new Release(request.getRequestId(), copyGrantStatus, request.getForObject()));
			return false;
		}
	}
	
	private boolean checkState(int parent) {
		if(2*parent >= serverGrantStatus.length - 1) {
			return serverGrantStatus[parent];
		} else {
			if(serverGrantStatus[parent]) {
				return (checkState(2*parent) || checkState(2*parent + 1));
			} else {
				return (checkState(2*parent) && checkState(2*parent + 1));
			}
		}
	}
	
	public synchronized boolean[] releasesToServers() {
		boolean[] copyGrantStatus = new boolean[this.numOfServers];
		for (int j = 0; j < this.numOfServers; j++) {
			copyGrantStatus[j] = serverGrantStatus[j];
		}
		return copyGrantStatus;
	}
	
	public Request getRequest() {
		return this.request;
	}
	/*
	private void submitReleases(int serverNum) {
		boolean[] copyGrantStatus = new boolean[this.numOfServers];
		copyGrantStatus[serverNum] = true;
		sendRelease.submitRelease(new Release(requestId, copyGrantStatus));
	}
	*/
}
