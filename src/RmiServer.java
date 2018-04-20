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

	private static ArrayList<FireAlarmMonitor> monitors = new ArrayList<>();	// TODO any operation on this must be always synchronized.
	private FileIO fileManager = new FileIO();
	private File monitorCountFile = new File("./m_count.txt");
	private File sensorCountFile = new File("./s_count.txt");
	
	
	// only reason we declare this constructor is because the remote object should be able to,
	// throw a RemoteException upon creation if need be.
	public RmiServer() throws RemoteException {}
	
	/*
	 * Socket server writes its sensor count to a file so the RMI server can read it.
	 * 
	 * (non-Javadoc)
	 * @see FireAlarmDataService#getSensorCount()
	 */
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
	
	/*
	 * Since we have the monitors list, we can directly get the count from it.
	 * 
	 * (non-Javadoc)
	 * @see FireAlarmDataService#getMonitorCount()
	 */
	public int getMonitorCount() throws RemoteException {
		
		// we have to synchronize the monitors list so that a monitor is not added/removed,
		// while we are getting the count.
		synchronized (monitors) {
			return monitors.size();
		}
		
	}
	
	/*
	 * Apart from adding a monitor to the server's monitor list, we need to notify,
	 * all the other connected monitors about the new monitor count.
	 * 
	 * Additionally we ask for a key to authenticate the monitor.
	 * This key should be as same as the one we gave when the RMI server was started.
	 * 
	 * (non-Javadoc)
	 * @see FireAlarmDataService#addMonitor(IListener)
	 */
	public void addMonitor(FireAlarmMonitor monitor, String key) throws RemoteException {
		
		Authenticator authenticator = new Authenticator();
		
		// we add the monitor only if the key is correct.
		if (authenticator.authenticateMonitor(key)) {
			synchronized (monitors) {
				monitors.add(monitor);
				
				// since a monitor is added, we need to let all connected monitors(included this one that we just added) know.
				updateMonitorCount();
				notifyMonitors(Integer.toString(getMonitorCount()), "monitor_count");
			}
		}
		else {
			// tell the monitor the key is wrong.
			// and since we haven't added it to the list, it won't receive anymore data from the server.
			monitor.onData("Invalid key; make sure you provide the same key as the one given for the RMI server.");
		}
		
		
	}
	
	/*
	 * Apart from remove the monitor from the server's monitor list, we need to notify,
	 * all the other connected monitors about the new monitor count.
	 * 
	 * (non-Javadoc)
	 * @see FireAlarmDataService#removeMonitor(IListener)
	 */
	public void removeMonitor(FireAlarmMonitor monitor) throws RemoteException {
		synchronized (monitors) {
			monitors.remove(monitor);
	
			// since a monitor is removed, we need to let other monitors know.
			updateMonitorCount();
			notifyMonitors(Integer.toString(getMonitorCount()), "monitor_count");
		}
	}
	
	public void login(String key) throws RemoteException {
		
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
		
		// if we don't synchronize the monitors array list, we will get a concurrent modification exception,
		// when this method is going through the monitors(iterating) while a new monitor joins.
		synchronized (monitors) {
			for(FireAlarmMonitor monitor: monitors) {
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
