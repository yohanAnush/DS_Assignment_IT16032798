package file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import socket.FireSensorData;

public class FileIO {

	private FileWriter writer;
	private BufferedReader reader;
	
	/* 
	 * Standard file manipulation.
	 */
	public String readFile(File file) {
		String fileContent = "";
		
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				fileContent += line;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return fileContent;
	}
	
	public void writeToFile(String data, File file, boolean append) {
		try {
			writer = new FileWriter(file);
			
			// we need to append data to the current file if need be.
			if (append) {
				writer.append(data);
			}
			else {
				// overwrite.
				writer.write(data);
			}
			
			writer.close();
		}
		catch (IOException e) {
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
	public void writeSensorDataToXml(HashMap<String, FireSensorData> sensorAndData, boolean coverFullHashMap, File dataFile) throws ParserConfigurationException  {
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
				if (root.getAttribute("new_data").equals("no")) {
					// we can safely overwrite the file.
					document = builder.newDocument();
					root = document.createElement("sensors");
					root.setAttribute("new_data", "yes");
					document.appendChild(root);
				}
			} catch (SAXException | IOException e) {
				e.printStackTrace();
				// if parsing fails, make a new file.
				document = builder.newDocument();
				root = document.createElement("sensors");
				root.setAttribute("new_data", "yes");
				document.appendChild(root);
			}
		}
		else {
			// since the file isn't there already, we can create a new file and add new data.
			document = builder.newDocument();
			root = document.createElement("sensors");
			root.setAttribute("new_data", "yes");
			document.appendChild(root);
		}
		
		
		// since we are writing new data anyway..
		
		// adding elements.
		// sensors.
		for(FireSensorData fsd: sensorAndData.values()) {
			// see if the data has already been written.
			// TODO check if we are supposed to cover all the sensors, in which case we don't check if the data,
			//		is already written or not.
			if (coverFullHashMap || !fsd.alreadyWrittenToFile()) {
				// sensor is a child of sensors.
				Element sensor = document.createElement("sensor");
				root.appendChild(sensor);
				
				// set sensor id.
				sensor.setAttribute("id", fsd.getSensorId());	// <sensor id="23-13">
				
				// set other 4 params.
				// they are children of the sensor.
				HashMap<String, String> dataHashMap = fsd.getParamHashMap();	// this way we can iterate instead of coding for each paramter.
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
				for (String error: fsd.getSensorErrors()) {
					Element errorElement = document.createElement("error");
					errorElement.appendChild(document.createTextNode(error));
					errors.appendChild(errorElement);
				}
				
				// usually we indicate that this specific data has already been written to a file,
				// so that it won't be written again unless it has been updated.
				// but covering the full hashmap is not our standard way and most of the time, we use,
				// a separate file to write when we cover the full hashmap so no need to alter the alreadyWritten status.
				if (!coverFullHashMap) {
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
	
	public String readXmlData(File dataFile) {
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
					
					
					// update the xml file so that the Socket server knows we read the file.
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					StreamResult target = new StreamResult(dataFile);
					transformer.transform(new DOMSource(document), target);
				}
			}
				
				
		} 
		catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
			//
		}
		
		return data;
	}
}
