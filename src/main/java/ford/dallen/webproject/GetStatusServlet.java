package ford.dallen.webproject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

public class GetStatusServlet extends HttpServlet {

	private static Log logger = LogFactory.getLog(LoginServlet.class);

	private static AmazonDynamoDB dynamoDB;

	private static final String TABLE_NAME = "status";

	private static final String HTML_HEADER = "<html>\r\n"
			+ "<head>\r\n"
			+ "<title>Ford Thermo Server</title>    \r\n"
			+ "</head>\r\n";

	private static final String HTML_BODY_BEGIN = "<body>\r\n";

	private static final String HTML_LINKS_LINE = "<p><a href=\"setpoints\">Setpoints</a>\t<a href=\"status\">Status</a>";

	private static final String HTML_USERNAME_HEADER_BEGIN = "<h1>Thermostat status for ";
	private static final String HTML_USERNAME_HEADER_END = "</h1>\r\n";

	private static final String HTML_TEMPERATURE_BEGIN = "<p>Current temperature: ";
	private static final String HTML_TEMPERATURE_END = " &deg;F";

	private static final String HTML_HEATER_STATUS_BEGIN = "<p>Heater is ";

	private static final String HTML_UPDATE_TIME_BEGIN = "<p>Status was last updated on ";

	private static final String HTML_BODY_END = "</body>\r\n"; 

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
	public void doGet(HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
	{
		String username = null;
		if( ( username = ServletUtils.authAndGetUName(request, response) ) != null )
		{
			response.setContentType("text/html");

			ThermostatStatus status = queryStatus(username);

			response.getWriter().println(new StringBuilder()
					.append(HTML_HEADER)
					.append(HTML_BODY_BEGIN)
					.append(HTML_LINKS_LINE)
					.append(HTML_USERNAME_HEADER_BEGIN)
					.append(username)
					.append(HTML_USERNAME_HEADER_END)
					.append(HTML_TEMPERATURE_BEGIN)
					.append(Integer.toString(status.getTemperature()))
					.append(HTML_TEMPERATURE_END)
					.append(HTML_HEATER_STATUS_BEGIN)
					.append((status.isHeaterOn() ? "on" : "off"))
					.append(HTML_UPDATE_TIME_BEGIN)
					.append(status.getUpdateTime().format(ServletUtils.TIME_FORMATTER))
					.append(HTML_BODY_END)
					.toString());
			response.setStatus(HttpServletResponse.SC_OK);
		}
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
