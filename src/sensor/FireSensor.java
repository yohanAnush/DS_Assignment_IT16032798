package sensor;

import java.util.HashMap;
import java.util.Scanner;


public class FireSensor extends SocketSensor {
	
	// TODO mimic the procedure of the sensor getting data by using a file.
	public static void main(String[] main) throws InterruptedException {
		
		FireSensor sensor = new FireSensor();
		
		sensor.connectToServer("localhost", 9001);
		
		// we need to authenticate the sensor with the server.
		// the key that should be provided by the sensor has to be the same key that we used,
		// when the server was started.
		System.out.print("Authentication key:");
		Scanner scanner = new Scanner(System.in);
		String key = scanner.nextLine();
		scanner.close();
		
		// now we need to check if this key matches the hash of the server's key.
		String serverResponse;
		sensor.writeText(key);
		if ((serverResponse = sensor.readText()) != null) {
			System.out.println(serverResponse);
		}
			
		Randoms random = new Randoms();
		String sensorId = random.getRandomInt(1, 23) + "-" + random.getRandomInt(1, 13);		// we assume there are 23 floors and 13 sensors per floor.
		HashMap<String, String> data;
		
		
		while (sensor.isConnected()) {	
			// always reinitialize the hash map or same data will be sent to the server always.
			data = new HashMap<>();
		
			data.put("sensorId", sensorId);		
			data.put("temperature", Double.toString((random.getRandomDouble(20.0, 90.0))));
			data.put("battery", Integer.toString((random.getRandomInt(1, 100))));
			data.put("co2", "300");
			data.put("smoke", Integer.toString((random.getRandomInt(0, 10))));
			
			// we only send data to the server at 1 hour intervals.
			// 0 indicates the sensor never wrote to the server, so we have to do an initial write.
			// all the consecutive writes will happen when the last update exceeds one hour compared to current time.
			// write object method will take care of managing the last update time.
												// to make sure data is sent once only 1 hour (60 mins = 3,600,000 millis).
			if (sensor.getLastUpdate() == 0 || (System.currentTimeMillis() - sensor.getLastUpdate()) > 3600000) {
				sensor.writeObject(data);
			}
			
			// we only have to emulate taking readings once every 5 mins.
			// 5 mins = 300,000 milliseconds.
			new Thread();
			Thread.sleep(300000);
		}
	}
}
