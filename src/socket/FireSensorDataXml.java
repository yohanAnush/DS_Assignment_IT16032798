package socket;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FireSensorDataXml {

	private String sensorId;
	private double temperature;
	private int batteryPercentage;
	private int smokeLevel;
	private double co2Level;
	
	
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
	
	@XmlElement
	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}
	
	@XmlElement
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}
	
	@XmlElement
	public void setBatteryPercentage(int batteryPercentage) {
		this.batteryPercentage = batteryPercentage;
	}
	
	@XmlElement
	public void setSmokeLevel(int smokeLevel) {
		this.smokeLevel = smokeLevel;
	}
	
	@XmlElement
	public void setCo2Level(double co2Level) {
		this.co2Level = co2Level;
	}
	
	
	public FireSensorDataXml getXmlObject(FireSensorData sensorData) {
		FireSensorDataXml xmlObj = new FireSensorDataXml();
		
		xmlObj.setSensorId(sensorData.getSensorId());
		xmlObj.setTemperature(sensorData.getTemperature());
		xmlObj.setBatteryPercentage(sensorData.getBatteryPercentage());
		xmlObj.setCo2Level(sensorData.getCo2Level());
		xmlObj.setSmokeLevel(sensorData.getSmokeLevel());
		
		return xmlObj;
	}
	
}
