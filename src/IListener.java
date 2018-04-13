import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IListener extends Remote {
	
	public void onData(String sensorData) throws RemoteException;
	public void onSensorChnange(int newSensorCount) throws RemoteException;
	public void onMonitorChange(int newMonitorCount) throws RemoteException;
}
