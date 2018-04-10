package sensor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

public class FireSensor {
	private static ObjectOutputStream sensorDataOutput;
	private static ObjectInputStream serverDataInput;
	private static PrintWriter sensorTextOutput;
	
	
	// TODO mimic the procedure of the sensor getting data by using a file.
	public static void main(String[] main) {
		String server = "localhost";
		Socket socket = null;
		try {
			socket = new Socket(server, 9001);
			sensorDataOutput = new ObjectOutputStream(socket.getOutputStream());
			serverDataInput = new ObjectInputStream(socket.getInputStream());
			sensorTextOutput = new PrintWriter(socket.getOutputStream(), true);
			
			// send to the server
			// TODO Send to the server according to the specifications.
			HashMap<String, String> sensorData;
			int count = 0; // for testing.
			while (true) {
				if (count > 1) {break;}	// for testing.
				
				// add the parameters and their readings to the hashmap first.
				sensorData = new HashMap<>();

				sensorData.put("sensorId", "01-1" + Integer.toString(count));
				sensorData.put("temperature", "89.0");
				sensorData.put("battery", "100");
				sensorData.put("smoke", "3");
				sensorData.put("co2", "300.0");

				// let the server know data is ready to be read through its ObjectInputStream;
				//sensorTextOutput.println("23-41");
				// send the data to the server
				sensorDataOutput.writeObject(sensorData);
			
				count++;
				
				Thread.sleep(5000);
			}
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
