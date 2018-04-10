import java.rmi.Remote;
import java.rmi.RemoteException;

import sensor.FireSensor;
import socket.FireSensorData;

public interface IListener extends Remote {
	
	public void onData(String sensorData) throws RemoteException;
	public void onError(String error) throws RemoteException;
}
