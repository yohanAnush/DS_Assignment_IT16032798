import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FireAlarmDataService extends Remote {
	
	public int getSensorCount() throws RemoteException;
	public int getMonitorCount() throws RemoteException;
	public String getAllReadings() throws RemoteException;
	public void addMonitor(IListener monitor, String key) throws RemoteException;
	public void removeMonitor(IListener monitor) throws RemoteException;
}
