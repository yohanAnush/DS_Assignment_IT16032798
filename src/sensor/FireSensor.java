package sensor;

import java.util.HashMap;

public class FireSensor extends Sensor {
	
	// TODO mimic the procedure of the sensor getting data by using a file.
	public static void main(String[] main) throws InterruptedException {
		FireSensor sensor = new FireSensor();
		Randoms random = new Randoms();
		String sensorId = random.getRandomInt(1, 23) + "-" + random.getRandomInt(1, 13);		// we assume there are 23 floors and 13 sensors per floor.
		HashMap<String, String> data;
		
		sensor.connectToServer("localhost", 9001);

		while (sensor.isConnected()) {	
			// always reinitialize the hash map or same data will be sent to the server always.
			data = new HashMap<>();
		
			data.put("sensorId", sensorId);		
			data.put("temperature", Double.toString((random.getRandomDouble(20.0, 90.0))));
			data.put("battery", Integer.toString((random.getRandomInt(1, 100))));
			data.put("co2", Double.toString((random.getRandomDouble(300, 301))));
			data.put("smoke", Integer.toString((random.getRandomInt(0, 10))));
			
			// we only send data to the server at 1 hour intervals.
			// 0 indicates the sensor never wrote to the server, so we have to do an intial write.
			// all the consecutive writes will happen when the last update exceeds one hour compared to current time.
			// write object method will take care of managing the last update time.
			if (sensor.getLastUpdate() == 0 || (System.currentTimeMillis() - sensor.getLastUpdate()) > 3600000) {
				sensor.writeObject(data);
			}
			
			new Thread();
			// to emulate readings being taken every 5 mins:-
			Thread.sleep(300000);
		}
	}
}
