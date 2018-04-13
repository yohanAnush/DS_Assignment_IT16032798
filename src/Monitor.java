import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;


public class Monitor extends UnicastRemoteObject implements IListener {

	// UI Properties.
	JFrame frame = new JFrame();
	JTextArea dataTxtArea = new JTextArea(10, 40);
	JLabel statsLbl = new JLabel();
	
	
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
		} 
		catch (MalformedURLException | RemoteException | NotBoundException e) {
			e.printStackTrace();
		} 
	}
}
