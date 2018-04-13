import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface IRmi extends Remote {
	
	public int getSensorCount() throws RemoteException;
	public int getMonitorCount() throws RemoteException;
	public void addMonitor(IListener monitor) throws RemoteException;
	public void removeMonitor(IListener monitor) throws RemoteException;
}
