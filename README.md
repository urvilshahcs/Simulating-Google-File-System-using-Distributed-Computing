# Google File System
Demo of file replication. Coursework CS 6378

## Introduction
1. There are n server nodes and m client nodes in the system, numbered from zero to n-1, zero to m-1. Each node executes on a different machine.
2. Establish reliable socket connections (TCP) between each pair of server-server nodes and client-server pair of nodes.
3. For each object, the class returns a server node to perform write or read.
4. Client Ci wants to update an object
 - Write is performed at three servers numbered: C(O), C(O)+1 modulo n, and C(O)+2 modulo n
 - Read is performed at any of the three servers numbered: C(O), C(O)+1 modulo n, and C(O)+2 modulo n
5. Conditions
 - Client should be able to randomly choose any of the three replicas of an object when it wishes to read the value of the object
 - Client can do update/insert only if two or more servers are available out of the chosen three.
 - Client should abort update/insert in case just one node is available.
 - In case of two or more clients trying to update same object. Updates must be performed in the same order in all servers. Set of nodes can only respond to same type and not to other.


## Usage
1) Place the executable jar Project3.jar and folder "resources/" in the same directory.
2) In all the nodes that function as servers, a folder with the same name as that of nodename of the server (say S1) has to be created in the folder in which the executable jar and "resources/" folder are placed. Empty objects O1.txt, O6.txt, O7.txt must be created in that folder before proceeding with the demo of this project. All these directory structures are also placed in the compressed folder.
3) resources/server.properties expects the following the entries.
	a) nodename (should be of the form Sx or Cx where x indicates the nodenum)
	b) function (This indicates whether the server is client or server)
	c) servers (comma separated nodenames of all the servers)
	d) master (This parameter identifies the name of the server that acts as controlling node of the whole execution)
	e) clients (comma separated nodenames of all the clients participating in the computation)
	f) objects (comma separated names of all the objects that are being stored at the servers)
	g) listeningport (port at which a server node is listening for incoming connections)
	h) S1=ipAddress:portNum (Example_Format: 10.176.66.51:10991) These addresses indicate the connecting nodes about the servers that are listening.
	i) S2=ipAddress:portNum (Example_Format: 127.0.0.1:11000)
	j) upto numberofservers that are mentioned in the servers property
	k) timeunit (this indicates the size of the timeunit. This affects the amount of time that the client stays in critical section and also the rate of generation of requests to access critical section)
	k) numofrequests (this indicates the number of requests that a client will generate)
	l) criticalsectiontimefactor (this indicates the number of timeunits that client spends in critical section. For project 3 this is no longer relevant as the critical section is modified to perform a 2-phase commit on a randomly chosen object from the stored objects)
	m) waittimefactorupperbound (this is upperbound of the wait time(in terms of timeunits) of a client before another request for critical section is issued)
	n) waittimefactorlowerbound (this is lowerbound of the wait time(in terms of timeunits) of a client before another request for critical section is issued)
	o) probabilityofyes (this is the probability with which a server will respond with an yes or no for requests to perform a write within the critical section of a client). If this is set to zero, no writes will be performed by the client because servers will never allow messages to be committed to files. This parameter can be set to be different for different servers.
3) A node can be started by the command "java -jar Project3.jar" and logs are printed in stdout console. These can be captured into files using redirection command '>'.
3) Start all the server nodes in any order before starting any of the client nodes. Make sure that the function property is set to 'server'
4) Start all the client nodes in any order. make sure that the function property is set to 'client'
5) As soon as the last client node gets connected to all servers, master server node sends begin message to all nodes and the clients begin the requests to access critical section
6) Once the client has finished executing the critical section for all the requests, it will send completion notification to master server and the whole system is brought down by master server.
7) Please find the source code in the directory "CS6378.001-P3-Avinash-Urvil"
8) All the statistics as per the requirement in project specs are displayed in the logs. 
9) Please find the logs of the 12 nodes (node1 to node7 are servers and the rest are clients) in the 'results' folder.
10) The order in which messages are written into a particular object in a server can be obtained from each server's log file. This can be achieved using cat and grep command with help from semantics of the log message (Example: cat node1.txt | grep "O1.txt"). For consistency check, these can be verified to be same on all the servers that write to that object file. Both commits and aborts are logged in the servers.
