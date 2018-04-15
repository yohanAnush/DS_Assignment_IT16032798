import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import authenticate.Authenticator;

@SuppressWarnings("serial")
public class RmiServer extends UnicastRemoteObject implements FireAlarmDataService, Runnable {

	private int sensorCount = 0;	// we can get this simply by counting nodes in the XML file(given that we are reading the current.txt file).
	private static ArrayList<IListener> monitors = new ArrayList<>();
	public RmiServer() throws RemoteException {}
	
	public int getSensorCount() throws RemoteException {

		// when we call the following method with 2nd parameter being true(i.e: countSensors: true),
		// readXmlData method will assign the sensor count after parsing the file.
		readXmlData(new File("./current.txt"), true);
		
		return this.sensorCount;
	}
	
	public int getMonitorCount() throws RemoteException {
		synchronized (monitors) {
			return monitors.size();
		}
	}
	
	public void addMonitor(IListener monitor) throws RemoteException {
		synchronized (monitors) {
			monitors.add(monitor);
		}
	}
	
	public void removeMonitor(IListener monitor) throws RemoteException {
		synchronized (monitors) {
			monitors.remove(monitor);
		}
	}
	
	/*
	 * We send 3 types of data to the monitors( well actually two).
	 * 		Sensor readings
	 * 		Monitor count
	 * 		Sensor count
	 * 
	 * Therefore, depending on data type we need to call the relevant method of the monitor.
	 */
	public void notifyMonitors(String data, String dataType) throws RemoteException {
		
		for(IListener monitor: monitors) {
			switch (dataType) {
			case "data":	monitor.onData(data);
						    break;
						    
			case "monitor_count":	monitor.onMonitorChange(Integer.parseInt(data));
									break;
									
			case "sensor_count":	monitor.onSensorChnange(Integer.parseInt(data));
									break;
				
			}
		}
	}
	
	
	public String getAllReadings() throws RemoteException {
		String data = readXmlData(new File("./current.txt"), false);

		return data;
	}
	
	public String readXmlData(File dataFile, boolean countNumOfSensors) {
		String data = "";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document document;
			Element root;
			
			if (dataFile.exists()) {
				document = builder.parse(dataFile);
				document.getDocumentElement().normalize();
				root = document.getDocumentElement();
				
				if (root.getAttribute("new_data").equals("yes") || dataFile.getPath().equals("./current.txt")) {
					
					// this way no matter how much we read the current.txt file, its new_data attribute will stay "yes",
					// which will lead the xml writing method in socket server to keep appending new readings; which is,
					// what we expect since current.txt is supposed to keep every reading that came to the socket server.
					if (!dataFile.getPath().equals("./current.txt")) {
						root.setAttribute("new_data", "no");
					}
					
					NodeList nodes = document.getElementsByTagName("sensor");
					for (int i = 0; i < nodes.getLength(); i++) {
						// to access the inner elements of a node, we need to cast the node,
						// to Element first.
						Element sensor = (Element)nodes.item(i);
						
						// check if the element is really an element.
						if (sensor.getNodeType() == Node.ELEMENT_NODE) {
							data += sensor.getAttribute("id") + " : ";		//sensorId
							
							// print the other child elements.
							NodeList childNodes = sensor.getChildNodes();
							
							for (int j = 0; j < childNodes.getLength(); j++) {
								Element childElement = (Element)childNodes.item(j);
								data += "  " + childElement.getTextContent() + "   ";
							}
							//notifyMonitors("\n");
							data += "\n";
						}
					}
					
					// if we are reading the current.txt file(which has readings of each sensor),
					// we can get the sensor count by counting the number of child nodes in the file.
					if (countNumOfSensors) {
						this.sensorCount = nodes.getLength();
					}
					
					// update the xml file so that the Socket server knows we read the file.
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					StreamResult target = new StreamResult(dataFile);
					transformer.transform(new DOMSource(document), target);
				}
			}
				
				
		} catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
			data = null;
		}
		
		return data;
	}
	
	public void run() {

		while(true) {
			String data;
			try {
				// current readings.
				if ((data =readXmlData(new File("./data.txt"), false)) != null) {
					notifyMonitors(data, "data");
					
					// sensor and monitor counts.
					// we get the sensor count by reading current.txt where the reading method assigns the count.
					readXmlData(new File("./current.txt"), true);
					notifyMonitors(Integer.toString(this.sensorCount), "sensor_count");
					notifyMonitors(Integer.toString(getMonitorCount()), "monitor_count");
				}
			}
			catch (RemoteException e) {
				continue;
			}

			
		}
		
	}
	
	public static void main(String [] args) throws RemoteException, IOException {
		
		try {
			// get key for authenticating the mointors.
			System.out.println("Enter master authentication key(Use this key to authenticate each monitor).");
			System.out.print("Key:");
			
			Scanner scanner = new Scanner(System.in);
			String key = scanner.nextLine();
			scanner.close();
			
			// we'll store the key as a hash and use that to authenticate monitors.
			Authenticator authenticator = new Authenticator();
			authenticator.setRmiServerAuthentication(key);
			
			System.out.println("Authentication key set, use the same key when starting monitors.");
			System.out.println("Fire Alarm RMI server is up and running");
			
			// register ourself with the rmiregistry.
			RmiServer rmiServer = new RmiServer();
			String registration = "rmi://localhost/FireAlarmService";
			
			Naming.rebind(registration, rmiServer);
			
			Thread t = new Thread(rmiServer);
			t.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
 
}
