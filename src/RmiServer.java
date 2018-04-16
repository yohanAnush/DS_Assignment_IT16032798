import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;

import authenticate.Authenticator;
import file.FileIO;

@SuppressWarnings("serial")
public class RmiServer extends UnicastRemoteObject implements FireAlarmDataService, Runnable {

	private int sensorCount = 0;	// we can get this simply by counting nodes in the XML file(given that we are reading the current.txt file).
	private static ArrayList<IListener> monitors = new ArrayList<>();
	private FileIO fileManager = new FileIO();
	private File monitorCountFile = new File("./m_count.txt");
	private File sensorCountFile = new File("./s_count.txt");
	
	
	public RmiServer() throws RemoteException {}
	
	public int getSensorCount() throws RemoteException {

		int count;
		
		try {
			count = Integer.parseInt(fileManager.readFile(sensorCountFile));
		}
		catch (NumberFormatException e) {
			count = -1;
		}
		
		return count;
		
	}
	
	public int getMonitorCount() throws RemoteException {
		int count;
		
		try {
			count = Integer.parseInt(fileManager.readFile(monitorCountFile));
		}
		catch (NumberFormatException e) {
			count = -1;
		}
		
		return count;
		
	}
	
	/*
	 * Apart from adding a monitor to the server's monitor list, we need to notify,
	 * all the other connected monitors about the new monitor count.
	 * 
	 * (non-Javadoc)
	 * @see FireAlarmDataService#addMonitor(IListener)
	 */
	public void addMonitor(IListener monitor) throws RemoteException {
		synchronized (monitors) {
			monitors.add(monitor);
			
			updateMonitorCount();
			notifyMonitors(Integer.toString(getMonitorCount()), "monitor_count");
		}
	}
	
	/*
	 * Apart from remove the monitor from the server's monitor list, we need to notify,
	 * all the other connected monitors about the new monitor count.
	 * 
	 * (non-Javadoc)
	 * @see FireAlarmDataService#removeMonitor(IListener)
	 */
	public void removeMonitor(IListener monitor) throws RemoteException {
		synchronized (monitors) {
			monitors.remove(monitor);
	
			updateMonitorCount();
			notifyMonitors(Integer.toString(getMonitorCount()), "monitor_count");
		}
	}
	
	/*
	 * We send 3 types of data to the monitors( well actually two).
	 * 		Sensor readings
	 * 		Monitor count
	 * 		Sensor count
	 * 
	 * Therefore, depending on data type we need to call the relevant method of the monitor.
	 */
	public void notifyMonitors(String data, String dataType) throws RemoteException {
		
		for(IListener monitor: monitors) {
			switch (dataType) {
			case "data":	monitor.onData(data);
						    break;
						    
			case "monitor_count":	monitor.onMonitorChange(Integer.parseInt(data));
									break;
									
			case "sensor_count":	monitor.onSensorChnange(Integer.parseInt(data));
									break;
				
			}
		}
	}
	
	
	public String getAllReadings() throws RemoteException {
		String data = fileManager.readXmlData(new File("./current.txt"));

		return data;
	}
	
	public void updateMonitorCount() {
		synchronized (monitors) {
			fileManager.writeToFile(Integer.toString(monitors.size()), monitorCountFile, false);
		}
	}
	
	
	public void run() {

		while(true) {
			String data;
			try {
				// current readings.
				if ((data = fileManager.readXmlData(new File("./data.txt"))) != null) {
					notifyMonitors(data, "data");
				}
				this.notifyMonitors(Integer.toString(getSensorCount()), "sensor_count");
			}
		
			catch (RemoteException e) {
				continue;
			}
		}
		
	}
	
	public static void main(String [] args) throws RemoteException, IOException {
		
		try {
			// get key for authenticating the mointors.
			System.out.println("Enter master authentication key(Use this key to authenticate each monitor).");
			System.out.print("Key:");
			
			Scanner scanner = new Scanner(System.in);
			String key = scanner.nextLine();
			scanner.close();
			
			// we'll store the key as a hash and use that to authenticate monitors.
			Authenticator authenticator = new Authenticator();
			authenticator.setRmiServerAuthentication(key);
			
			System.out.println("Authentication key set, use the same key when starting monitors.");
			System.out.println("Fire Alarm RMI server is up and running");
			
			// register ourself with the rmiregistry.
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
