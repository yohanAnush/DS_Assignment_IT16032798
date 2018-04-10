package socket;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/*
 * This class will act as a helper class to the server to manage sensor data.
 * A sensor will send 4 parameters as follows;
 *  		1) Temperature(celcius)
 *  		2) Battery level (percentage)
 *  		3) Smoke level (scale of 1 - 10)
 *  		4) CO2 level (parts per million)
 *  
 *  The above parameters should be stored, and validated for their correctness and checked for,
 *  dangerous levels/values and notify the server right away as well.
 */
public class FireSensorData {
	
	private String sensorId;
	private double temperature;
	private int batteryPercentage;
	private int smokeLevel;
	private double co2Level;
	
	// for error handling.
	private String tempErr = "";
	private String batteryErr = "";
	private String smokeErr = "";
	private String co2Err = "";
	
	// for data passing via a file.
	private boolean alreadyWrittenToFile = false;	// set this to true when the data is written to file.
	
	
	// Getters.
	public String getSensorId() {
		return sensorId;
	}
	
	public double getTemperature() {
		return temperature;
	}
	
	public int getBatteryPercentage() {
		return batteryPercentage;
	}
	
	public int getSmokeLevel() {
		return smokeLevel;
	}
	
	public double getCo2Level() {
		return co2Level;
	}
	
	public boolean alreadyWrittenToFile() {
		return this.alreadyWrittenToFile;
	}
	
	public String getTempErr() {
		return tempErr;
	}

	public String getBatteryErr() {
		return batteryErr;
	}

	public String getSmokeErr() {
		return smokeErr;
	}

	public String getCo2Err() {
		return co2Err;
	}

	
	
	/* 
	 * Setters.
	 * A separate method should be used to determine such value is deemed dangerous as specified, or invalid.
	 */
	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}
	
	public void setTemperature(double temperature) {
		this.temperature = temperature;	
	}
	
	public void setBatteryPercentage(int batteryPercentage) {
		this.batteryPercentage = batteryPercentage;
	}
	
	public void setSmokeLevel(int smokeLevel) {
		this.smokeLevel = smokeLevel;
	}
	
	public void setCo2Level(double co2Level) {
		this.co2Level = co2Level;
	}
	
	public void setAlreadyWrittenToFile(boolean alreadyWrittenToFile) {
		this.alreadyWrittenToFile = alreadyWrittenToFile;
	}
	
	public void setTempErr(String tempErr) {
		this.tempErr = tempErr;
	}

	public void setBatteryErr(String batteryErr) {
		this.batteryErr = batteryErr;
	}

	public void setSmokeErr(String smokeErr) {
		this.smokeErr = smokeErr;
	}

	public void setCo2Err(String co2Err) {
		this.co2Err = co2Err;
	}

	// Data is sent from the fire sensor as a hashmap, encoded as follows.
	// 		Ex: "temp", "45.0"
	public FireSensorData getFireSensorDataFromHashMap(HashMap<String, String> stringData) {
		setSensorId(stringData.get("sensorId"));
		setTemperature(Double.parseDouble(stringData.get("temperature")));
		setBatteryPercentage(Integer.parseInt(stringData.get("battery")));
		setSmokeLevel(Integer.parseInt(stringData.get("smoke")));
		setCo2Level(Double.parseDouble(stringData.get("co2")));
		
		return this;
	}
	
	
	/* 
	 * Validators.
	 * The 4 parameters will be checked for their validity and whether they indicate any sort of danger.
	 * Always check for invalidity first and then for dangerous values.
	 * 
	 * If there's an error, we set the error message to the variable relevant to holding that param's error, and
	 * no error means the variable getting an empty string.
	 * We do so to allow the server to check if there's an error(since validation methods returns a boolean), 
	 * and if there's an error, server can get the relevant error and send to the monitors.
	 */
	
	// Minimum possible temperature is -273.15 degrees celcius.
	// Anything above 50 degrees is considered dangerous.
	public boolean isTemperatureInLevel() {
		boolean validity = true;
		
		// always set the error to none at the beginning.
		setTempErr("");
		
		if (this.temperature < -273.15) {
			validity = false;
			setTempErr(this.sensorId + " : Sensor is malfunctioning; A temperature of " + this.temperature + " celcius is below absolute zero.");
		}
		else if (this.temperature > 50.0) {
			validity = false;
			setTempErr(this.sensorId + " : Temperature is reaching a dangerous level at " + this.temperature + " celcius.");
		}
		
		return validity;
	}
	
	// Battery level over 100% indicates a malfunction in the battery.
	// Anything from 30% to 0% indicates low battery level.
	public boolean isBatteryInLevel() {
		boolean validity = true;
		
		// always set the error to none at the beginning.
		setBatteryErr("");
		if (this.batteryPercentage > 100 || this.batteryPercentage < 0) {
			validity = false;
			setBatteryErr(this.sensorId + " : Battery malfunction.");
		}
		
		else if (this.batteryPercentage <= 30) {
			validity = false;
			setBatteryErr(this.sensorId + " : Battery low!");
		}

		return validity;
	}
	
	// Smoke level above 7 is dangerous.
	// From 1 to 6 is considered okay.
	public boolean isSmokeInLevel() {
		boolean validity = true;
		
		// always set the error to none at the beginning.
		setSmokeErr("");
		
		if (this.smokeLevel < 1 || this.smokeLevel > 10) {
			setSmokeErr(this.sensorId + " : Smoke sensor malfunction.");
			validity = false;
		}
		
		else if (this.smokeLevel > 7) {
			setSmokeErr(this.sensorId + " : Smoke level is at a dangerous level of " + this.smokeLevel);
			validity = false;
		}
		
		return validity;
	}
	
	// A CO2 level of above or belowe 300.0 is considered dangerous.
	// At 300.0, CO2 level is considered okay.
	public boolean isCo2InLevel() {
		boolean validity = true; 
		
		// always set the error to none at the beginning.
		setCo2Err("");
		
		if (this.co2Level != 300.0) {
			setCo2Err(this.sensorId + " : CO2 level is at a dangerous level of " + this.co2Level);
			validity = false;
		}
		
		return validity;
	}
	
	
	// For testing whether data is passed from the client to the server properly.
	// TODO Remove this method after all implementations are done.
	public void printData() {
		System.out.println(this.sensorId);
		System.out.println("Temps   : " + this.temperature);
		System.out.println("Battery : " + this.batteryPercentage);
		System.out.println("Smoke   : " + this.smokeLevel);
		System.out.println("CO2     : " + this.co2Level);
	}
}
