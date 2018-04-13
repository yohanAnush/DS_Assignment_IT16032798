package sensor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

public class Sensor {
	
	private Socket socket;
	private ObjectOutputStream sensorDataOutput;
	@SuppressWarnings("unused")	// ObjectInputStream has to be there even if it's not used.
	private ObjectInputStream serverDataInput;
	@SuppressWarnings("unused")
	private PrintWriter sensorTextOutput;
	private long lastUpdate = 0;
	
	public boolean isConnected() {
		return socket.isConnected();
	}
	
	public long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public boolean connectToServer(String host, int port) {
		boolean connected = false;
		
		// attempt connection.
		try {
			this.socket = new Socket(host, port);
			
			// initiate I/O streams.
			this.sensorDataOutput = new ObjectOutputStream(socket.getOutputStream());
			this.serverDataInput = new ObjectInputStream(socket.getInputStream());
			this.sensorTextOutput = new PrintWriter(socket.getOutputStream(), true);
			
			connected = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return connected;
	}
	
	public boolean disconnectFromServer() {
		boolean disconnected= false;
		
		if (socket != null) {
			try {
				socket.close();
				disconnected = true;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return disconnected;
	}
	
	public boolean writeObject(HashMap<String, String> data) {
		boolean wrote = false;
		try {
			sensorDataOutput.writeObject(data);
			setLastUpdate(System.currentTimeMillis());
			
			wrote = true;
		}
		catch (SocketException e) {
			disconnectFromServer();
			// this mostly means the server is no longer running and the pipe is broken as a result.
			// so there's no point of staying connected.
			System.err.println("Server down, disconnected and exiting..");
			System.exit(1);
		}
		catch (IOException e) {
			e.printStackTrace();
		} 
		
		return wrote;
	}
}
