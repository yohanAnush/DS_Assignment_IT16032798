package socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


/*
 *  this will handle two responsibilities.
 *  		1) communicate between the fire sensors and gain information.
 *  		2) inform the monitors about the current state of information gained in 1).
 *  
 *  A socket is used to communicate between the fire sensors.
 *  RMI is used to let the monitors know about the current state of information.
 *  
 */
public class SocketServer implements ISocket, Runnable {

	// server config.
	private static final int PORT_TO_LISTEN = 9001;
	
	/*
	 *  Recording data given by each sensor.
	 *  
	 *  A sensor will send 4 parameters as follows;
	 *  		1) Temperature(celcius)
	 *  		2) Battery level (percentage)
	 *  		3) Smoke level (scale of 1 - 10)
	 *  		4) CO2 level (parts per million)
	 *  
	 *  Use a helper class to validate those parameters and check for dangerous values/levels.
	 */
	private static HashMap<String, FireSensorData> sensorAndData = new HashMap<>();

	// Socket Connection properties.
	private Socket socket;
	private BufferedReader sensorTextInput;	// gives sort of a heads-up before sending the actual data via object output stream.
	private ObjectInputStream sensorDataInput;	// this will delivery a hash map where a key can be 1 of the 4 parameters.
											// and the value relevent to the parameter is the object assigned to the key.
											// both the key and the object/value are Strings (Parse as needed).
	
	// while we are not sending any data to the client,
	// we need this object initialized before the input stream,
	// in order for everything to work.
	// TODO initialize the ObjectOutputStream object before ObjectInpuStream.
	@SuppressWarnings("unused")
	private ObjectOutputStream serverDataOutput;

	// File I/O properties.
	File dataFile = new File("./data.txt");
	BufferedReader reader;
	PrintWriter writer;
	
	// Socket Connection implementations.
	/*
	 * (non-Javadoc)
	 * @see fireAlarmServer.ISocketConnection#initSocketConnection(java.net.Socket)
	 */
	public void initSocketConnection(Socket serverSocket) {
		this.socket = serverSocket;
		try {
			this.serverDataOutput = new ObjectOutputStream(this.socket.getOutputStream());
			this.sensorTextInput =  new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.sensorDataInput = new ObjectInputStream(this.socket.getInputStream());
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	// must be initialized before input stream.
		
		// initiate file stream.
		try {
			reader = new BufferedReader(new FileReader(dataFile));
			writer = new PrintWriter(dataFile);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Reads the ObjectOutputStream of the connected client socket and returns if any data is read.
	 * TODO Above stream will always be read to check for data, thus EOFException will be thrown most of the time,
	 * 		(since data will be available every couple of minutes). Therefore we have to ignore the above exception.
	 * 
	 * (non-Javadoc)
	 * @see fireAlarmServer.ISocketConnection#readSocketData()
	 */
	public Object readSocketData() {
		Object data = null;
		try {
			data = this.sensorDataInput.readObject();
		} 
		catch (IOException  ioe) {
			// do not do a stack trace since most of the time the exception will be,
			// EOFException, since data will be available in fixed intervals of times.
		}
		catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		
		return data;
	}

	/*
	 * Returns the server socket that is passed to the ServerInstance at the time of the creation of a,
	 * ServerInstance object. We need this server socket to initialize other parameters such as i/o streams,
	 * of the socket.
	 * 
	 * (non-Javadoc)
	 * @see fireAlarmServer.ISocketConnection#getServerSocket()
	 */
	public Socket getServerSocket() {
		return this.socket;
	}
	
	/*
	 * When a client exits, call this method and close its socket.
	 * 
	 * (non-Javadoc)
	 * @see fireAlarmServer.ISocketConnection#closeSocket()
	 */
	public void closeSocket() {
		try {
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
 	
	/*
	 * Execution:-
	 * Main method will listen to the port specified in PORT_TO_LISTEN and will
	 * assign a new thread to each unique sensor.
	 * Implementation of the thread aspects are below the main method.
	 */
	
	public static void main(String[] args) throws IOException {
		System.out.println("Fire Alarm Sensor is up and running");
		
		ServerSocket portListner = new ServerSocket(PORT_TO_LISTEN);
		
		try {
			// accept as requests come.
			while (true) {
				SocketServer server = new SocketServer(portListner.accept());
				Thread t = new Thread(server);
				t.start();
			}
		}
		finally {
			// server is shutting down.
			portListner.close();
		}
	}
	
	/*
	 * Add FireSensor's data to the hashmap that we maintain.
	 * Hashmap is keyed by the sensor's id and the data is paired with that key.
	 * sensor id is of type String and data is of type FireSensorData.
	 * 
	 * TODO Always synchronize and avoid duplicates.
	 */
	public void insertDataToServerHashMap(String sensorId, FireSensorData fireSensorData) {
		synchronized (sensorAndData) {
			// HashMap will automatically replace the value if the sensorId already exists.
			// set writtenToFile in fireSensorData to false so the writting method may recognize,
			// new data and write to the file.
			fireSensorData.setAlreadyWrittenToFile(false);
			sensorAndData.put(sensorId, fireSensorData);
		}
		
	}
	
	/*
	 * For RMI Server to be able to access data, we need to write to a file.
	 * Once the data is written, set its alreadyWrittenToFile to true to avoid same data being duplicated.
	 * 
	 * Always erase the current content and write new content.
	 * Add the timestamp to the file so the RMI server can identify if the file is new or not.
	 *  
	 * Append new data if the RMI server hasn't read the previous data.
	 */
	public void writeToStorage() throws IOException {
		// initialize file.
		reader = new BufferedReader(new FileReader(dataFile));	// to access the first line of the file always.
		String firstLine = reader.readLine();
		System.out.println(firstLine);
		
		// check if it's already there.
		if (!dataFile.exists()|| firstLine == null || firstLine.startsWith("READ")){
			writer.write(Long.toString(System.currentTimeMillis()) + "\n");	// so the reader can identify whether the file is new or not.
		}
	
		// writting data.
		for (FireSensorData sensorData: sensorAndData.values()) {
			if (!sensorData.alreadyWrittenToFile()) {
				String dataToWrite = sensorData.getSensorId() + " : " + 
						   "T: " + sensorData.getTemperature() + "  " +
						   "B: " + sensorData.getBatteryPercentage() + "  " +
						   "CO2: " + sensorData.getCo2Level() + "  " +
						   "S: " + sensorData.getSmokeLevel();
		
				System.err.println(dataToWrite);
				writer.append(dataToWrite + "\n");	
				
				sensorData.setAlreadyWrittenToFile(true);
			}
		}
		writer.append("END" + "\n");
		writer.close();
	}
	
	
	/*
	 * Thread for each sensor.
	 * This way server can deal with all the sensors without mixing up or blocking communication.
	 * Each thread should be given the socket of each sensor.	
	 */
	
	/* * * Each ServerInstance is simple an unique instance of FireAlarmServer with a couple of data handling parameters. * * */
	private String sensorId;
	private FireSensorData fireSensorData;
	private long lastUpdate;	// using Time() we can get the difference easily.
	
	
	public SocketServer(Socket sensorSocket) throws RemoteException {
		this.socket = sensorSocket;
	}
	
	public SocketServer() {
	}
		
		
		
		/*
		 * run method of the Thread class.
		 * Listens to the sensor and accepts the hashmap sent.
		 * Parse the content of the hashmap as needed by the FireSensorHelper class,
		 * TODO And determine if the monitors should be notified or not.
		 * TODO Send alert to the sensors to turn the alarm on.
		 * 
		 * Monitors should be notified if the sensor does not report back after an hour.
		 */
		@SuppressWarnings("unchecked")
		public void run() {
			try {
				initSocketConnection(socket);
				
				HashMap<String, String> sensorDataAsHashMap;
				FireSensorData fsd;
				String sensorId = "Unassigned Sensor Id";
				lastUpdate = System.currentTimeMillis();
				
				while (true) {
					// TODO Always get the text input and data input of the sensor into a,
					// 		local variable to avoid null pointers.
					
					// Monitors should be notified if the sensor's last update exceeds one hour.
					// 1 hour = 3.6e+6 millis. 
					if ((System.currentTimeMillis() - lastUpdate) > 	600) {
						//notifyMonitors(sensorId + " has not reported in 1 hour.");
						
						// Don't remove the following code as it will result in a non-stop loop until data arrives.
						// Sending the warning once and then waiting another 1 hour will suffice.
						lastUpdate = System.currentTimeMillis();
					}
					
					if ( (sensorDataAsHashMap = (HashMap<String, String>) readSocketData()) != null) {
						fsd = new FireSensorData().getFireSensorDataFromHashMap(sensorDataAsHashMap);
						sensorId = fsd.getSensorId();
						fsd.printData();
							
						insertDataToServerHashMap(sensorId, fsd);
						
						writeToStorage();
						// we need to notify the listeners about the new data.
						// always get the data from the hashmap instead of transmitting the local variable.
						//notifyMonitors(sensorAndData.get(sensorId));
						
						// check for errors and send error messages.
						if (!fsd.isTemperatureInLevel()) {
							//notifyMonitors(fsd.getTempErr());
						}
						if (!fsd.isBatteryInLevel()) {
							//notifyMonitors(fsd.getBatteryErr());
						}
						if (!fsd.isSmokeInLevel()) {
							//notifyMonitors(fsd.getSmokeErr());
						}
						if (!fsd.isCo2InLevel()) {
							//notifyMonitors(fsd.getCo2Err());
						}
						
						// coming upto this points indicates that the sensor sent data,
						// hence we can set the last update to the current time.
						lastUpdate = System.currentTimeMillis();
					}
					
					
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			finally {
				// sensor disconnecting from the server.
				// therefore remove the sensor and its data.
				if (sensorId != null) {
					sensorAndData.remove(sensorId);
				}
				
				// close the connection.
				closeSocket();
			}
		}
	
}
