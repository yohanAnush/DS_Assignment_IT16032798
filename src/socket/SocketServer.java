package socket;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

import authenticate.Authenticator;
import file.FileIO;


/*
 *  this will handle two responsibilities.
 *  		1) communicate between the fire sensors and gain information.
 *  		2) inform the monitors about the current state of information gained in 1).
 *  
 *  A socket is used to communicate between the fire sensors.
 *  RMI is used to let the monitors know about the current state of information.
 *  
 */
public class SocketServer implements Runnable {

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
	@SuppressWarnings("unused")
	private BufferedReader sensorTextInput;
	private PrintWriter serverTextOutput;
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
	FileIO fileManager = new FileIO();
	File latestDataFile = new File("./data.txt");
	File allCurrentReadingsFile = new File("./current.txt");
	File sensorCountFile = new File("./s_count.txt");
	
	
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
			this.serverTextOutput = new PrintWriter(this.socket.getOutputStream(), true);
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	// must be initialized before input stream.
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
		
		// get key for authenticating the sensors.
		System.out.println("Enter master authentication key(Use this key to authenticate each sensor).");
		System.out.print("Key:");
		
		Scanner scanner = new Scanner(System.in);
		String key = scanner.nextLine();
		scanner.close();
		
		// we'll store the key as a hash and use that to authenticate sensors.
		Authenticator authenticator = new Authenticator();
		authenticator.setSocketServerAuthentication(key);
		
		System.out.println("Authentication key set, use the same key when starting sensors.");
		System.out.println("Fire Alarm Socket Server is up and running");
		
		// initiate socket operations.
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
			
			// finally we update the sensor count.
			updateSensorCount();
		}
	}

	/*
	 * Every time a sensor is connected/disconnected we need to update the sensor count,
	 * and write to a file so the RMI server can use that data.
	 */
	public void updateSensorCount() {

		// we must always overwrite the file so there's only one count.
		synchronized (sensorAndData) {
			fileManager.writeToFile(Integer.toString(sensorAndData.keySet().size()), sensorCountFile, false);
			System.out.println(sensorAndData.size());
		}
	}
	
	/*
	 * Thread for each sensor.
	 * This way server can deal with all the sensors without mixing up or blocking communication.
	 * Each thread should be given the socket of each sensor.	
	 */
	
	/* * * Each ServerInstance is simple an unique instance of FireAlarmServer with a couple of data handling parameters. * * */
	private String sensorId;
	private long lastUpdate;	// using Time() we can get the difference easily.
	
	
	public SocketServer(Socket sensorSocket) {
		this.socket = sensorSocket;
	}
	
	public SocketServer() {}
		
		
		
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
			FireSensorData fsd = null;
			String sensorId = "Unassigned Sensor Id";
			lastUpdate = System.currentTimeMillis();
			
			// authenticate the server.
			// the first and only text the sensor will send is the password.
			String password = sensorTextInput.readLine();
			Authenticator authenticator = new Authenticator();
			
			if (password != null && !authenticator.authenticateSensor(password)) {
				serverTextOutput.println("Authentication failed, disconnecting....");
				closeSocket();
			}
			else {
				serverTextOutput.println("Authenticated successfully.");
			}
			
			while (socket != null) {
				// TODO Always get the text input and data input of the sensor into a,
				// 		local variable to avoid null pointers.
					
				// Monitors should be notified if the sensor's last update exceeds one hour.
				// 1 hour = 3.6e+6 millis = 3,600,000 millis. 
				if ((System.currentTimeMillis() - lastUpdate) > 3600000 && fsd != null) {

					fsd.setUnreportedErr("* * * " + sensorId + " has not reported in 1 hour. * * * ");
						
					fsd.setAlreadyWrittenToFile(false);		// otherwise writting method will ignore the sensor.
					fileManager.writeSensorDataToXml(sensorAndData, false, new File("./data.txt"));		// for rmi server to read latest data.
					fileManager.writeSensorDataToXml(sensorAndData, true, new File("./current.txt")); 	// in case the rmi server wants data of all the connected sensors.
						
					// Don't remove the following code as it will result in a non-stop loop until data arrives.
					// Sending the warning once and then waiting another 1 hour will suffice.
					lastUpdate = System.currentTimeMillis();
					fsd.setAlreadyWrittenToFile(true);		// since writting is now over, we has to indicate that the data of this sensor is written,				
																// otherwise the writting method in the below if condition will continenously write this sensor's data.
				}
					
				if ( (sensorDataAsHashMap = (HashMap<String, String>) readSocketData()) != null) {
					fsd = new FireSensorData(sensorDataAsHashMap);
					sensorId = fsd.getSensorId();
						
					fsd.printData();	
					insertDataToServerHashMap(sensorId, fsd);
					fileManager.writeSensorDataToXml(sensorAndData, false, new File("./data.txt"));		// for rmi server to read latest data.
					fileManager.writeSensorDataToXml(sensorAndData, true, new File("./current.txt")); 	// if the rmi server wants data of all the connected sensors.
						
					// coming upto this points indicates that the sensor sent data,
					// hence we can set the last update to the current time.
					lastUpdate = System.currentTimeMillis();
				}	
			}
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}	
		finally {
			// sensor disconnecting from the server.
			// therefore remove the sensor and its data.
			synchronized (sensorAndData) {
				sensorAndData.remove(sensorId);	
				
				// we have to update the sensor count since we have removed a sensor.
				updateSensorCount();
				// close the connection.
				closeSocket();
			}	
		}
	}
}
