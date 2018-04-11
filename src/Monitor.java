import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

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
import org.w3c.dom.css.ElementCSSInlineStyle;
import org.xml.sax.SAXException;

import socket.FireSensorData;

public class Monitor extends UnicastRemoteObject implements IListener {

	public Monitor() throws RemoteException {}
	
	public void onData(String sensorData) throws RemoteException {
		System.out.print(sensorData);
	}
	
	public void onError(String error) throws RemoteException {
		System.err.print(error);
	}
	
	public static void main(String [] args) {

		try {
			String remoteServiceAddress = "//localhost/FireAlarmService";
			Remote remoteService = Naming.lookup(remoteServiceAddress);
			IRmi rmiServer = (IRmi)remoteService;
			
			// we can now use addClient method in server to add ourself to the server.
			rmiServer.addMonitor(new Monitor());
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			e.printStackTrace();
		} 
	}
}
