package authenticate;

import java.io.File;
import java.io.IOException;

import file.FileIO;

/*
 * This class will server as a helper class to authenticate sensors, monitors with their,
 * respective servers.
 * 
 * Since we are using text files to store the keys set by the servers, we have to store the hash code,
 * of the keys instead of the keys themselves.
 */
public class Authenticator {
	
	// Authenticator properties.
	private FileIO fileManager = new FileIO();
	private File socketAuthenFile = new File("./sockAuthen.txt");
	private File rmiAuthenFile = new File("./rmiAuthen.txt");

	
	/*
	 * Implementation.
	 */
	public void setSocketServerAuthentication(String key) throws IOException {
		// we should always overwrite the file so that there will only be one master key stored in the file.
		fileManager.writeToFile(Integer.toString(key.hashCode()), socketAuthenFile, false);
	}
	
	public void setRmiServerAuthentication(String key) throws IOException {
		// we should always overwrite the file so that there will only be one master key stored in the file.
		fileManager.writeToFile(Integer.toString(key.hashCode()), rmiAuthenFile, false);
	}
	
	public boolean authenticateSensor(String input) {
		boolean result = false;
		
		// get the hash code of the master key.
		String masterKeyHash = fileManager.readFile(socketAuthenFile);
			
		if (masterKeyHash != null) {
			result = Integer.parseInt(masterKeyHash) == input.hashCode();
		}
		
		return result;
	}
	
	public boolean authenticateMonitor(String input) {
		boolean result = false;
		
		// get the hash code of the master key.
		String masterKeyHash = fileManager.readFile(rmiAuthenFile);
			
		if (masterKeyHash != null) {
			result = Integer.parseInt(masterKeyHash) == input.hashCode();
		}
		
		return result;
	}

}
