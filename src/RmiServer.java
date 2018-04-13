import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.jar.Attributes.Name;

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

public class RmiServer extends UnicastRemoteObject implements IRmi, Runnable {

	private static ArrayList<IListener> monitors = new ArrayList<>();
	
	public RmiServer() throws RemoteException {}
	
	public int getSensorCount() throws RemoteException {
		int count = -1;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader("./stats.txt"));
			String line = reader.readLine();
			
			if (line != null) {
				count = Integer.parseInt(line);
			}
			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return count;
	}
	
	public int getMonitorCount() throws RemoteException {
		return monitors.size();
	}
	
	public void addMonitor(IListener monitor) throws RemoteException {
		monitors.add(monitor);
	}
	
	public void removeMonitor(IListener monitor) throws RemoteException {
		monitors.remove(monitor);
	}
	
	public void notifyMonitors(String data) throws RemoteException {
		for(IListener monitor: monitors) {
			monitor.onData(data);
		}
	}
	
	public void readXmlData(File dataFile) {
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
				
				if (root.getAttribute("new_data").equals("yes")) {
					root.setAttribute("new_data", "no");
					
					NodeList nodes = document.getElementsByTagName("sensor");

					for (int i = 0; i < nodes.getLength(); i++) {
						// to access the inner elements of a node, we need to cast the node,
						// to Element first.
						Element sensor = (Element)nodes.item(i);
						
						// check if the element is really an element.
						if (sensor.getNodeType() == Node.ELEMENT_NODE) {
							notifyMonitors(sensor.getAttribute("id") + " : ");		//sensorId
							
							// print the other child elements.
							NodeList childNodes = sensor.getChildNodes();
							
							for (int j = 0; j < childNodes.getLength(); j++) {
								Element childElement = (Element)childNodes.item(j);
								notifyMonitors(childElement.getTextContent() + "   ");
							}
							notifyMonitors("\n");
						}
					}
					
					// update the xml file so that the Socket server knows we read the file.
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					StreamResult target = new StreamResult(dataFile);
					transformer.transform(new DOMSource(document), target);
				}
			}
				
				
		} catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		System.out.println("started");
		try {
			Thread.sleep(600);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Reseumed");
		// keep reading the data file. Once read, write "READ" to the file,
		// so the Socket server can realize we have read the file.
		BufferedReader reader;
		PrintWriter writer;
		String line;
		
		while(true) {
			readXmlData(new File("./data.txt"));
		}
		
	}
	
	public static void main(String [] args) throws RemoteException {
		
		try {
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
