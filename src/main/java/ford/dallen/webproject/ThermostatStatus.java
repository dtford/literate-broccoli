package ford.dallen.webproject;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class ThermostatStatus {

	private int temperature;
	private boolean heaterOn;
	private LocalDateTime updateTime;

	public ThermostatStatus(int temp, boolean on, LocalDateTime time)
	{
		temperature = temp;
		heaterOn = on;
		updateTime = time;
	}

	public int getTemperature() {
		return temperature;
	}
	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}
	public boolean isHeaterOn() {
		return heaterOn;
	}
	public void setHeaterOn(boolean heaterOn) {
		this.heaterOn = heaterOn;
	}

	public LocalDateTime getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}

	public Map<String, AttributeValue> makeDBItem(String username)
	{
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		item.put("username", new AttributeValue().withS(username) );
		item.put("temperature", new AttributeValue().withN(Integer.toString(temperature)));
		item.put("heaterOn", new AttributeValue().withBOOL(heaterOn));
		item.put("updateTime", new AttributeValue().withS(updateTime.format(ServletUtils.TIME_FORMATTER)));

		return item;
	}

}
