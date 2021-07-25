package ford.dallen.webproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

public class SetHeaterServlet extends HttpServlet {	

	private static Log logger = LogFactory.getLog(LoginServlet.class);

	private static AmazonDynamoDB dynamoDB;

	private static final String TABLE_NAME = "status";

	@Override
	public void init()
	{
		try {
			dynamoDB = ServletUtils.DBinit(dynamoDB);

			TableUtils.waitUntilActive(dynamoDB, TABLE_NAME);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage());
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response ) throws IOException
	{
		String username = null;
		if( ( username = ServletUtils.authAndGetUName(request, response) ) != null )
		{
			response.setContentType("text/html");

			// get the current status so we can update it
			ThermostatStatus status = queryStatus(username);

			String contentType = request.getHeader("Content-Type");

			boolean heaterOn = false;
			if(contentType.equals("application/x-www-form-urlencoded"))
			{
				heaterOn = Boolean.parseBoolean(request.getParameter("heaterOn") );
			}
			else if(contentType.equals("application/json"))
			{
				StringBuffer jsonBuffer = new StringBuffer();
				String line = null;
				try 
				{
					BufferedReader reader = request.getReader();

					while( (line = reader.readLine()) != null )
					{					
						// cleanup start and end 's
						line = line.replaceAll("'", "");
						jsonBuffer.append(line);
					}

					JsonReader jsonReader = Json.createReader(new StringReader(jsonBuffer.toString()));
					JsonObject jsonObject = jsonReader.readObject();

					heaterOn = Boolean.parseBoolean( jsonObject.getString("heaterOn") );
				}
				catch( Exception e)
				{
					logger.error(e.getMessage());
				}
			}

			status.setHeaterOn(heaterOn);
			status.setUpdateTime(LocalDateTime.now());

			setStatus(username, status);

			response.getWriter().println("Success");

			response.setStatus(HttpServletResponse.SC_OK);

		}
	}

	private void setStatus(String username, ThermostatStatus status)
	{
		Map<String, AttributeValue> item = status.makeDBItem(username);

		PutItemRequest putItemRequest = new PutItemRequest(TABLE_NAME, item);
		dynamoDB.putItem(putItemRequest);

	}

	private ThermostatStatus queryStatus(String username)
	{
		HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
		Condition condition = new Condition()
				.withComparisonOperator(ComparisonOperator.EQ.toString())
				.withAttributeValueList(new AttributeValue().withS(username));
		scanFilter.put("username", condition);
		ScanRequest scanRequest = new ScanRequest(TABLE_NAME).withScanFilter(scanFilter);
		ScanResult scanResult = dynamoDB.scan(scanRequest);

		ThermostatStatus status = null;

		try {
			for(Map<String, AttributeValue> resultMap : scanResult.getItems())
			{
				int temperature = Integer.parseInt( resultMap.get("temperature").getN() );
				boolean heaterOn = resultMap.get("heaterOn").getBOOL();
				String timeString = resultMap.get("updateTime").getS();
				LocalDateTime ldt = LocalDateTime.parse(timeString, ServletUtils.TIME_FORMATTER);
				status = new ThermostatStatus(temperature, heaterOn, ldt);

			}
		}
		catch (Exception e)
		{
			logger.error(e.getMessage());
			status = null;
		}
		return status;
	}
}
