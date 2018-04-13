package socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


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
	File dataFile = new File("./data.xml");
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
			writer = new PrintWriter(dataFile);
			reader = new BufferedReader(new FileReader(dataFile));
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
			
			// for RMI server's usage.
			writeSensorCountToFile(new File("./stats.txt"));
		}
		
	}
	
	/*
	 * Writes the current number of sensors to a text file,
	 * NO XML here, just plain text.
	 * 
	 * Must be called when a sensor is connected as well as when a sensor disconnects.
	 */
	public void writeSensorCountToFile(File dataFile) {
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(dataFile));
			writer.write(sensorAndData.size());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * This method is kinda tricky,even though we iterate through the whole HashMap that contains,
	 * data from all the sensors, we skip the sensors whose data has previously being written and there,
	 * has not been any update since the last write.
	 * 
	 * But, we need to able to provide the data of all the sensors if the RMI server requires so. 
	 */
	public void writeSensorDataToXml(File dataFile, boolean coverAllSensors) throws ParserConfigurationException {
		// we use data.txt file to write/append new data,
		// and current.txt file to write all the data of all the sensors.
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document;
		Element root;
	
		
		if (dataFile.exists()) {
			// RMI server will set new_data attribute of the root element to "no" after reading.
			// and to tell the RMI server that it should read data, Socket server(here) will set it to "yes".
			try {
				document = builder.parse(dataFile);
				root = document.getDocumentElement();
				
				// if we are to cover all the sensors, we might as well overwrite the existing file anyways.
				if (root.getAttribute("new_data").equals("no") || coverAllSensors) {
					// we can safely overwrite the file.
					document = builder.newDocument();
					root = document.createElement("sensors");
					root.setAttribute("new_data", "yes");
				}
			} catch (SAXException | IOException e) {
				e.printStackTrace();
				// if parsing fails, make a new file.
				document = builder.newDocument();
				root = document.createElement("sensors");
				root.setAttribute("new_data", "yes");
			}
		}
		else {
			// since the file isn't there already, we can create a new file and add new data.
			document = builder.newDocument();
			root = document.createElement("sensors");
			root.setAttribute("new_data", "yes");
		}
		
		root.setAttribute("count", Integer.toString(sensorAndData.size()));
		document.appendChild(root);
		
		// since we are writing new data anyway..
		
		// adding elements.
		// sensors.
		for(FireSensorData fsd: sensorAndData.values()) {
			// see if the data has already been written.
			// TODO check if we are supposed to cover all the sensors, in which case we don't check if the data,
			//		is already written or not.
			if (coverAllSensors || !fsd.alreadyWrittenToFile()) {
				// sensor is a child of sensors.
				Element sensor = document.createElement("sensor");
				root.appendChild(sensor);
				
				// set sensor id.
				sensor.setAttribute("id", fsd.getSensorId());	// <sensor id="23-13">
				
				// set other 4 params.
				// they are children of the sensor.
				HashMap<String, String> dataHashMap = fsd.getHashMap();	// this way we can iterate instead of coding for each paramter.
				for (String param: dataHashMap.keySet()) {
					Element paramElement = document.createElement(param);
					paramElement.appendChild(document.createTextNode(dataHashMap.get(param)));		// <temperature>43.4</temperature>
					sensor.appendChild(paramElement);		// because temperature is a child of sensor.
				}
				
				// errors.
				// All the errors tags(i.e: 4, one for each param) must be there,
				// even if there is no error for that param.
				// Otherwise down the line if we get a new error, XML Parser wont let us,
				// add the error due to HIERARCHY_REQUEST_ERR.
				Element errors = document.createElement("errors");
				sensor.appendChild(errors);
				
				// error of each parameter of fire sensor is a child of errors.
				fsd.validateAllParameters();
				for (String error: fsd.getErrorsList()) {
					Element errorElement = document.createElement("error");
					errorElement.appendChild(document.createTextNode(error));
					errors.appendChild(errorElement);
				}
				
				// if we are not covering all the sensors we need to mark the data as already written.
				if (!coverAllSensors) {
					fsd.setAlreadyWrittenToFile(true); 	// to avoid duplication of data.
				}
			}
		}
		
		// write the data to .xml file.
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult target = new StreamResult(dataFile);
			
			transformer.transform(source, target);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
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
				FireSensorData fsd = new FireSensorData();
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
					
					if (/*(fsd = (FireSensorData)readSocketData()) != null*/  (sensorDataAsHashMap = (HashMap<String, String>) readSocketData()) != null) {
						//fsd = new FireSensorData().getFireSensorDataFromHashMap(sensorDataAsHashMap);
						fsd = fsd.getFireSensorDataFromHashMap(sensorDataAsHashMap);
						sensorId = fsd.getSensorId();
						
						fsd.printData();	
						insertDataToServerHashMap(sensorId, fsd);
						writeSensorDataToXml(new File("./data.txt"), false);		// for rmi server to read latest data.
						writeSensorDataToXml(new File("./current.txt"), true); 	// if the rmi server wants data of all the connected sensors.
						
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
					
					writeSensorCountToFile(new File("./stats.txt"));
				}
				
				// close the connection.
				closeSocket();
			}
		}
	
}
