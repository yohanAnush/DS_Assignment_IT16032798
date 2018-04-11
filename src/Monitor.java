import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import socket.FireSensorData;

public class Monitor extends UnicastRemoteObject implements IListener {

	public Monitor() throws RemoteException {}
	
	public void onData(String sensorData) throws RemoteException {
		System.out.println(sensorData);
	}
	
	public void onError(String error) throws RemoteException {
		System.err.println(error);
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
