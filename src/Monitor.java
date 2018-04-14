import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import authenticate.Authenticator;

@SuppressWarnings("serial")
public class Monitor extends UnicastRemoteObject implements IListener {

	// GUI properties.
	JFrame frame = new JFrame();
	JLabel statsLbl = new JLabel();
	JTextArea dataTxtArea = new JTextArea(8, 40);
	JButton refreshBtn = new JButton("Refresh");

	// Monitor properties.
	int currentMonitorCount = 0;
	int currentSensorCount = 0;
	
	static FireAlarmDataService server;
	
	public Monitor() throws RemoteException {
		
		// initialize the GUI.
		dataTxtArea.setEditable(false);
		frame.getContentPane().add(statsLbl, BorderLayout.NORTH);
		frame.getContentPane().add(refreshBtn, BorderLayout.SOUTH);
		frame.getContentPane().add(dataTxtArea, BorderLayout.WEST);
		frame.pack();
	}
	
	
	//Remote methods exposed to the server.
	 
	/* 
	 * (non-Javadoc)
	 * @see IListener#onData(java.lang.String)
	 */
	public void onData(String sensorData) throws RemoteException {
		System.out.print(sensorData);
		dataTxtArea.append(sensorData);
	}
	
	/*
	 * Basically methods when a sensor is connected/disconnected to/from the server.
	 * Always update the class's variable first.
	 * 
	 * (non-Javadoc)
	 * @see IListener#onSensorChnange(int)
	 */
	public void onSensorChnange(int newSensorCount) throws RemoteException {
		this.currentSensorCount = newSensorCount;
		statsLbl.setText("#Sensors: " + this.currentSensorCount + "     " + "#Monitors: " + this.currentMonitorCount);
	}
	
	/*
	 * Basically methods when a monitor is connected/disconnected to/from the server.
	 * Always update the class's variable first.
	 * 
	 * (non-Javadoc)
	 * @see IListener#onMonitorChange(int)
	 */
	public void onMonitorChange(int newMonitorCount) throws RemoteException {
		this.currentMonitorCount = newMonitorCount;
		statsLbl.setText("#Sensors: " + this.currentSensorCount + "     " + "#Monitors: " + this.currentMonitorCount);
	}
	
	
	/* * * Execution.
	 * 
	 */
	public static void main(String [] args) {

		try {
			// authenticate with the RMI server.
			// to do that, we'll use a dialogue box and ask for the key.
			JFrame dialogueBoxFrame = new JFrame("Enter Authentication Key");
			String key = JOptionPane.showInputDialog(dialogueBoxFrame, "Key:");
			Authenticator authenticator = new Authenticator();
			System.err.println(key);
			if (!authenticator.authenticateMonitor(key)) {
				System.err.println(key + "  closing");
				return;
			}
			else {
				dialogueBoxFrame.dispose();
			}
			// if the monitor can not be authenticated, we'll terminate it.
			
			
			// find the FireAlarmService.
			String remoteServiceAddress = "//localhost/FireAlarmService";
			Remote remoteService = Naming.lookup(remoteServiceAddress);
			server = (FireAlarmDataService)remoteService;
			
			// we can now use addClient method in server to add ourself to the server.
			// gui is also deployed.
			Monitor monitor = new Monitor();
			server.addMonitor(monitor);
			
			// update the ui with details taken from the server.
			monitor.currentMonitorCount = server.getMonitorCount();
			monitor.currentSensorCount = server.getSensorCount();
			monitor.statsLbl.setText("#Sensors: " + monitor.currentSensorCount + "     " + "#Monitors: " + monitor.currentMonitorCount);
			
			monitor.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			monitor.frame.setVisible(true);
			
			// we need to remove the monitor from the server when the monitor exits,
			// otherwise the server will throw an exception trying to call a monitor that is not running.
			monitor.frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					try {
						server.removeMonitor(monitor);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					
				}
			});	
			
			monitor.refreshBtn.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("clicked!!");
					try {
						System.err.println(server.getAllReadings());
						monitor.dataTxtArea.append(server.getAllReadings());
					}
					catch (RemoteException e1) {
						e1.printStackTrace();
					}
					
				}
			});
		} 
		catch (MalformedURLException | RemoteException | NotBoundException e) {
			e.printStackTrace();
		}
	}
}
