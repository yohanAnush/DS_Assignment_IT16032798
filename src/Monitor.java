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
}
