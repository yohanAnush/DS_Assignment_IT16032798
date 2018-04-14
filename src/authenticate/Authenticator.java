package authenticate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/*
 * This class will server as a helper class to authenticate sensors, monitors with their,
 * respective servers.
 * 
 * Since we are using text files to store the keys set by the servers, we have to store the hash code,
 * of the keys instead of the keys themselves.
 */
public class Authenticator {
	
	private File socketAuthenFile = new File("./sockAuthen.txt");
	private File rmiAuthenFile = new File("./rmiAuthen.txt");

	public void setSocketServerAuthentication(String key) throws IOException {
		FileWriter writer = new FileWriter(socketAuthenFile);
		writer.write(Integer.toString(key.hashCode()));		// always convert to a string or otherwise some invalid character will be written.
		writer.close();
	}
	
	public void setRmiServerAuthentication(String key) throws IOException {
		FileWriter writer = new FileWriter(rmiAuthenFile);
		writer.write(Integer.toString(key.hashCode()));		// always convert to a string or otherwise some invalid character will be written.
		writer.close();
	}
	
	public boolean authenticateSensor(String input) {
		boolean result = false;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(socketAuthenFile));
			String line = reader.readLine();
			reader.close();
			
			if (line != null) {
				result = Integer.parseInt(line) == input.hashCode();
			}
		}
		catch (IOException e) {
			System.err.println("Socket authentication key is not set!");
		}
		
		return result;
	}
	
	public boolean authenticateMonitor(String input) {
		boolean result = false;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(rmiAuthenFile));
			String line = reader.readLine();
			reader.close();
			
			if (line != null) {
				result = Integer.parseInt(line) == input.hashCode();
			}
		}
		catch (IOException e) {
			System.err.println("RMI authentication key is not set!");
		}
		
		return result;
	}

}
