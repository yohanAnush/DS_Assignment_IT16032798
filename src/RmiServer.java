import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.jar.Attributes.Name;

public class RmiServer extends UnicastRemoteObject implements IRmi, Runnable {

	private static ArrayList<IListener> monitors = new ArrayList<>();
	
	public RmiServer() throws RemoteException {}
	
	public ArrayList<String> getConnectedSensors() throws RemoteException {
		
		return new ArrayList<>();
	}
	
	public void addMonitor(IListener monitor) throws RemoteException {
		monitors.add(monitor);
	}
	
	public void removeMonitor(IListener monitor) throws RemoteException {
		monitors.remove(monitor);
	}
	
	public void notifyMonitors(String data) throws RemoteException {
		
		for(IListener monitor: monitors) {
			monitor.onData(data);
		}
	}
	
	public void run() {
		System.out.println("started");
		try {
			Thread.sleep(600);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Reseumed");
		// keep reading the data file. Once read, write "READ" to the file,
		// so the Socket server can realize we have read the file.
		BufferedReader reader;
		PrintWriter writer;
		try {
			reader = new BufferedReader(new FileReader("./data.txt"));
			String line;
			while (true) {
				line = reader.readLine();
				
				// if the current line is empty, go back the beginning of the file.
				if (line != null && line.equals("END")) {
					writer = new PrintWriter("./data.txt");
					writer.write("READ");
					writer.close();
					
					// re open the file to go the beginning.
					reader = new BufferedReader(new FileReader("./data.txt"));
				}
				else if (line != null && !line.startsWith("READ")) {
					// means the line now has the time stamp.
					System.out.println(line);
					notifyMonitors(line);
				}
				
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String [] args) throws RemoteException {
		
		try {
			RmiServer rmiServer = new RmiServer();
			String registration = "rmi://localhost/FireAlarmService";
			
			Naming.rebind(registration, rmiServer);
			
			Thread t = new Thread(rmiServer);
			t.start();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
 
}
