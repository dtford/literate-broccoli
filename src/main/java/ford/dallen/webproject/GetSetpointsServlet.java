package ford.dallen.webproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReader;
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
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

public class GetSetpointsServlet extends HttpServlet {

	private static Log logger = LogFactory.getLog(GetSetpointsServlet.class);

	private class Setpoint implements Comparable<Setpoint>
	{
		private int temperature;

		// we only keep a beginTime, since the setting continues until the next setpoint's begin time.
		private LocalTime beginTime;

		public Setpoint(int temp, LocalTime begin)
		{
			temperature = temp;
			beginTime = begin;
		}

		public int getTemperature() {
			return temperature;
		}
		public void setTemperature(int temperature) {
			this.temperature = temperature;
		}
		public LocalTime getBeginTime() {
			return beginTime;
		}
		public void setBeginTime(LocalTime beginTime) {
			this.beginTime = beginTime;
		}

		@Override
		public int compareTo(Setpoint o) {
			return beginTime.compareTo(o.beginTime);
		}

	}

	private static AmazonDynamoDB dynamoDB;

	private static final String HTML_HEADER = "<html>\r\n"
			+ "<head>\r\n"
			+ "<title>Ford Thermo Server</title>    \r\n"
			+ "</head>\r\n";

	private static final String HTML_BODY_BEGIN = "<body>\r\n";

	private static final String HTML_LINKS_LINE = "<p><a href=\"setpoints\">Setpoints</a>\t<a href=\"status\">Status</a>";

	private static final String HTML_USERNAME_HEADER_BEGIN = "<h1>Setpoints for ";

	private static final String HTML_USERNAME_HEADER_END = "</h1>\r\n";

	private static final String HTML_ERROR_STRING = "<h2>Error: Invalid input, please re-enter.</h2>\r\n";

	private static final String HTML_TABLE_BEGIN = "<table>\r\n"
			+ "<tr>\r\n"
			+ "<th>Begin Time</th>\r\n"
			+ "<th>Temperature (F)</th>\r\n"
			+ "</tr>\r\n";

	private static final String HTML_TABLE_ROW_BEGIN = "<tr>\r\n";

	private static final String HTML_TABLE_CELL_BEGIN = "<td>";

	private static final String HTML_TABLE_CELL_END = "</td>\r\n";

	private static final String HTML_TABLE_ROW_END = "</tr>\r\n";

	private static final String HTML_TABLE_END = "</table>\r\n";

	private static final String HTML_FORM = "<form method=\"POST\" action=\"setpoints\">\r\n"
			+ "<label for=\"beginTime\">Begin Time:</label>\r\n"
			+ "<input type=\"time\" name=\"beginTime\" required>\r\n"
			+ "<label for=\"temperature\">Temperature:</label>\r\n"
			+ "<input type=\"number\" name=\"temperature\" required>\r\n"
			+ "<input type=\"submit\" value=\"Add Setpoint\">\r\n"
			+ "</form>\r\n";

	private static final String HTML_DELETE_BUTTON_SCRIPT = "<script>\r\n"
			+ "function deletePoint(point)\r\n"
			+ "{\r\n"
			+ "  bodyString = '{\"beginTime\":\"' + point + '\"}';\r\n"
			+ "  fetch('setpoints', {\r\n"
			+ "                 method:'DELETE',\r\n"
			+ "                 credentials: 'same-origin',\r\n"
			+ "                 headers: {\r\n"
			+ "	                            'Content-Type': 'application/json'\r\n"
			+ "                          },\r\n"
			+ "                 body: bodyString\r\n"
			+ "               }\r\n"
			+ "             );\r\n"
			+ "  location = location.href;\r\n"
			+ "}\r\n"
			+ "</script>\r\n";

	private static final String HTML_DELETE_BUTTON_BEGIN = "<button onclick=\"deletePoint('";

	private static final String HTML_DELETE_BUTTON_END = "')\">Delete</button>\r\n";

	private static final String HTML_BODY_END = "</body>\r\n"; 

	private static final String TABLE_NAME = "setpoints";

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
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{		
		try 
		{			
			String username = null;
			if( ( username = ServletUtils.authAndGetUName(request, response) ) != null )
			{	
				// check header for json/text
				String acceptHeader = request.getHeader("Accept");

				boolean respondWithJSON = acceptHeader.contains("application/json");

				Set<Setpoint> setpoints = queryForSetpoints(username);

				StringBuilder htmlBuilder = new StringBuilder();
				htmlBuilder.append(HTML_HEADER)
				.append(HTML_BODY_BEGIN)
				.append(HTML_LINKS_LINE)
				.append(HTML_USERNAME_HEADER_BEGIN)
				.append(username)
				.append(HTML_USERNAME_HEADER_END)
				.append(HTML_DELETE_BUTTON_SCRIPT)
				.append(HTML_TABLE_BEGIN);

				JsonObject wrapper = null;

				if(setpoints != null)
				{	
					// create JSON					
					JsonBuilderFactory factory = Json.createBuilderFactory(null);
					JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();

					for(Setpoint setpoint : setpoints)
					{
						arrayBuilder.add(factory.createObjectBuilder()
								.add("beginTime", setpoint.getBeginTime().toString())
								.add("temperature", Integer.toString(setpoint.getTemperature())));
						htmlBuilder.append(HTML_TABLE_ROW_BEGIN)
						.append(HTML_TABLE_CELL_BEGIN)
						.append(setpoint.getBeginTime().toString())
						.append(HTML_TABLE_CELL_END)
						.append(HTML_TABLE_CELL_BEGIN)
						.append(Integer.toString(setpoint.getTemperature()))
						.append(HTML_TABLE_CELL_END)
						.append(HTML_TABLE_CELL_BEGIN)
						.append(HTML_DELETE_BUTTON_BEGIN)
						.append(setpoint.getBeginTime().toString())
						.append(HTML_DELETE_BUTTON_END)
						.append(HTML_TABLE_CELL_END)
						.append(HTML_TABLE_ROW_END);
					}



					JsonArray array = arrayBuilder.build();

					wrapper = factory.createObjectBuilder().add("setpoints", array).build();
				}
				htmlBuilder.append(HTML_FORM)
				.append(HTML_TABLE_END)
				.append(HTML_BODY_END);

				if( respondWithJSON )
				{
					response.setContentType("application/json");
					if(wrapper != null);
					response.getWriter().println(wrapper.toString());
				}
				else 
				{
					response.setContentType("text/html");
					if(htmlBuilder != null);
					response.getWriter().println(htmlBuilder.toString());
				}
				response.setStatus(HttpServletResponse.SC_OK);
			}
			else
			{
				response.setContentType("text/html");
				response.getWriter().println("Not Authenticated");
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		}
		catch( Exception e)
		{
			logger.error(e.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}

	// allow Post also
	@Override
	public void doPost( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
	{
		String username = ServletUtils.authAndGetUName( request, response );

		if( username != null ) 
		{

			// parse setpoint from text
			LocalTime beginTime = null;
			int temperature = 0;

			String contentType = request.getHeader("Content-Type");

			if(contentType.equals("application/x-www-form-urlencoded"))
			{

				try
				{
					beginTime = LocalTime.parse( request.getParameter("beginTime") );
					temperature = Integer.parseInt(request.getParameter("temperature"));
				}
				catch( NumberFormatException e)
				{
					// fill out the HTML to send
					StringBuilder htmlBuilder = new StringBuilder()
							.append(HTML_HEADER)
							.append(HTML_BODY_BEGIN)
							.append(HTML_LINKS_LINE)
							.append(HTML_USERNAME_HEADER_BEGIN)
							.append(username)
							.append(HTML_USERNAME_HEADER_END)
							.append(HTML_ERROR_STRING)
							.append(HTML_FORM)
							.append(HTML_BODY_END);
					response.setContentType("text/html");
					response.getWriter().println(htmlBuilder.toString());
					response.setStatus(HttpServletResponse.SC_OK);
					return;
				}
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

					beginTime = LocalTime.parse( jsonObject.getString("beginTime") );
					temperature = Integer.parseInt( jsonObject.getString("password") );
				}
				catch( Exception e)
				{
					response.setContentType("text/html");
					response.getWriter().println(e.getMessage());
				}
			}

			addSetpoint( username, beginTime, temperature );

			doGet( request, response );
		}
	}		

	@Override
	public void doDelete( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
	{	
		String username = ServletUtils.authAndGetUName(request, response);

		if( username != null )
		{
			// process input as JSON
			// because I'll have the form send JSON too.

			LocalTime beginTime = null;

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

				beginTime = LocalTime.parse( jsonObject.getString("beginTime") );

			}
			catch( Exception e)
			{
				response.setContentType("text/html");
				response.getWriter().println(e.getMessage());
			}

			boolean removed = false;
			try {
				Set<Setpoint> setpoints = queryForSetpoints( username );

				for( Iterator<Setpoint> it = setpoints.iterator(); it.hasNext(); )
				{
					Setpoint setpoint = it.next();
					// find setpoint to delete
					if( setpoint.getBeginTime().equals(beginTime))
					{
						// delete point and break
						it.remove();
						removed = true;
						break;
					}
				}

				// upload to cloud
				if(removed) {
					Map<String, AttributeValue> item = dbItemFromSetpoints( username, setpoints );
					PutItemRequest putItemRequest = new PutItemRequest(TABLE_NAME, item);
					dynamoDB.putItem(putItemRequest);
				}

			} catch (Exception e) {

				e.printStackTrace();
			}

			// respond
			response.setContentType("text/html");


			if(removed) {
				response.getWriter().println("Success");
				response.setStatus(HttpServletResponse.SC_OK);
			}
			else
			{
				response.getWriter().println("Item not removed.");
				response.setStatus(HttpServletResponse.SC_OK);
			}
		}

	}

	private void addSetpoint(String username, LocalTime beginTime, int temperature ) 
	{
		// get the original map
		try
		{
			Set<Setpoint> setpoints = queryForSetpoints( username );

			if(setpoints == null) setpoints = new TreeSet<Setpoint>(Comparator.comparing(Setpoint::getBeginTime));

			Setpoint setpoint = new Setpoint( temperature, beginTime );

			if( !setpoints.add( setpoint ) )
			{
				setpoints.remove( setpoint );
				setpoints.add( setpoint );
			}

			Map<String, AttributeValue> item = dbItemFromSetpoints( username, setpoints );

			// Add an item
			PutItemRequest putItemRequest = new PutItemRequest(TABLE_NAME, item);
			dynamoDB.putItem(putItemRequest);

		}
		catch( Exception e )
		{
			logger.error(e.getMessage());
		}
	}

	private Map<String, AttributeValue> dbItemFromSetpoints( String username, Set<Setpoint> setpoints )
	{
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();

		item.put( "username", new AttributeValue( username ) );

		Map<String, AttributeValue> setpointsMap = new HashMap<String, AttributeValue>();
		for(Setpoint setpoint : setpoints )
		{			
			setpointsMap.put(setpoint.getBeginTime().toString(), new AttributeValue().withN( Integer.toString(setpoint.getTemperature())));
		}
		item.put( "setpoint", new AttributeValue().withM(setpointsMap));

		return item;
	}

	private Set<Setpoint> queryForSetpoints(String username) throws Exception
	{	
		HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
		Condition condition = new Condition()
				.withComparisonOperator(ComparisonOperator.EQ.toString())
				.withAttributeValueList(new AttributeValue().withS(username));
		scanFilter.put("username", condition);
		ScanRequest scanRequest = new ScanRequest(TABLE_NAME).withScanFilter(scanFilter);
		ScanResult scanResult = dynamoDB.scan(scanRequest);

		Set<Setpoint> setpoints = new TreeSet<Setpoint>(Comparator.comparing(Setpoint::getBeginTime));

		for(Map<String, AttributeValue> resultMap : scanResult.getItems())
		{        	
			AttributeValue singleMapAttribute = resultMap.get("setpoint");
			if( singleMapAttribute != null )
			{
				Map<String, AttributeValue> singleMap = resultMap.get("setpoint").getM();

				for(Map.Entry<String, AttributeValue> pair : singleMap.entrySet())
				{
					LocalTime beginTime = LocalTime.parse(pair.getKey());
					int temperature = Integer.parseInt(pair.getValue().getN());        	

					Setpoint setpoint = new Setpoint(temperature, beginTime);
					setpoints.add(setpoint);
				}
			}
		}


		if(setpoints.isEmpty())
		{
			return null;
		}

		return setpoints;
	}

}
