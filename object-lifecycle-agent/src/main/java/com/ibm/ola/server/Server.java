package com.ibm.ola.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

import com.ibm.ola.agent.Configuration;
import com.ibm.ola.agent.Logger;
import com.ibm.ola.datastore.AbstractDataStore;
import com.ibm.ola.datastore.Data;
import com.ibm.ola.datastore.Event;
import com.ibm.ola.vo.DataPeriodRequest;
import com.ibm.ola.vo.DataPeriodResponse;
import com.ibm.ola.vo.DataRequest;
import com.ibm.ola.vo.DataResponse;
import com.ibm.ola.vo.GarbageCollectionRequest;
import com.ibm.ola.vo.GarbageCollectionResponse;
import com.ibm.ola.vo.IdNameRequest;
import com.ibm.ola.vo.IdNameResponse;
import com.ibm.ola.vo.ObjectCounter;
import com.ibm.ola.vo.UnknownRequestResponse;

public class Server {
	
	AbstractDataStore dataStore;
	
	ServerSocket serverSocket;
	
	ServerSocketThread thread;
	
	boolean alive = true;
	
	Configuration configuration;
	
	Logger logger = new Logger();

	public Server(Configuration configuration, AbstractDataStore dataStore) throws IOException {
		this.configuration = configuration;
		int port = configuration.getIntValue("server-port");
		logger.info("Started server on port " + port);
		this.dataStore = dataStore;
		serverSocket = new ServerSocket(port);
		thread = new ServerSocketThread();
	}
	
	public void start() {
		thread.start();
	}
	
	
	class ServerSocketThread extends Thread {

		public void run() {
			while (alive) {
				logger.info("Server alive...");
				try {
					Socket socket = serverSocket.accept();
					SocketThread socketThread = new SocketThread(socket);
					socketThread.start();
				} 
				catch (IOException e) {
					logger.error("Error accepting sock.", e);
				} 
			}
		}
	}
	
	class SocketThread extends Thread {
		
		Socket socket;
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		
		public SocketThread(Socket socket) {
			this.socket = socket;
		}
		
		ObjectOutputStream getObjectOutputStream() throws IOException {
			if (oos == null) 
				oos = new ObjectOutputStream(socket.getOutputStream());
			return oos;
		}
		
		ObjectInputStream getObjectInputStream() throws IOException {
			if (ois == null) 
				ois = new ObjectInputStream(socket.getInputStream());
			return ois;
		}
		
		public void run() {
			while (alive) {
				try {
					handleSocketRequest(socket);
				} 
				catch (IOException e) {
					logger.trace("Unexpected error.", e);
					break;
				} 
				catch (ClassNotFoundException e) {
					throw new Error(e);
				}
			}
		}
		
		void handleSocketRequest(Socket socket) throws IOException, ClassNotFoundException {
			Serializable request = (Serializable) getObjectInputStream().readObject();
			Serializable response = handleRequest(request);
			getObjectOutputStream().writeObject(response);
		}

		Serializable handleRequest(Serializable request) {
			if (request instanceof DataPeriodRequest) {
				return new DataPeriodResponse(dataStore.getDataStart(), dataStore.getDataEnd());				
			}
			else if (request instanceof DataRequest) {
				DataRequest dataRequest = (DataRequest) request;
				Data data = dataStore.getData(dataRequest.start, dataRequest.end, dataRequest.interval);
				return new DataResponse(data.ids, data.objectCounters);				
			}
			else if (request instanceof IdNameRequest) {
				IdNameRequest idNameRequest = (IdNameRequest) request;
				String names[] = new String[idNameRequest.ids.length];
				for (int i=0; i<names.length; i++)
					names[i] = dataStore.getClassName(idNameRequest.ids[i]);
				return new IdNameResponse(names);				
			}
			else if (request instanceof GarbageCollectionRequest) {
			    System.gc();
			    return new GarbageCollectionResponse();
            }
			else return new UnknownRequestResponse(request);
		}
	}
}
