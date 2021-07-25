package ford.dallen.webproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoginServlet extends HttpServlet {

	private static Log logger = LogFactory.getLog(LoginServlet.class);

	private static AmazonDynamoDB dynamoDB;

	private static final String SUCCESS_HTML = "<head>\r\n"
			+ "<meta http-equiv='refresh' content='0; URL=status'>\r\n"
			+ "</head>\r\n";

	private static final String TABLE_NAME = "logins";

	private class Credentials
	{
		private String username;
		private String password;

		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}

	}

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

	private Credentials processJSONCredentials(HttpServletRequest request)
	{	
		Credentials creds = new Credentials();
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

			creds.setUsername( jsonObject.getString("username") );
			creds.setPassword( jsonObject.getString("password") );
		}
		catch(Exception e)
		{		
			return null;
		}

		//final error check
		if( ( creds.getUsername() == null ) || ( creds.getPassword() == null ) )
		{
			return null;
		}

		return creds;
	}

	private Credentials processFormCredentials(HttpServletRequest request)
	{
		Credentials creds = new Credentials();
		try
		{
			creds.setUsername( request.getParameter("username") );
			creds.setPassword( request.getParameter("password") );
		}
		catch(Exception e)
		{
			return null;
		}

		if( ( creds.getUsername() == null ) || ( creds.getPassword() == null ) )
		{
			return null;
		}

		return creds;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response )
			throws ServletException, IOException
	{
		response.setContentType("text/html");
		PrintWriter printWriter = response.getWriter();


		try 
		{	
			String contentType = request.getHeader("Content-Type");

			Credentials creds = null;
			if(contentType.equals("application/x-www-form-urlencoded"))
			{
				creds = processFormCredentials(request);
			}
			else if(contentType.equals("application/json"))
			{
				creds = processJSONCredentials(request);
			}

			if(creds == null)
			{
				printWriter.println("Bad Request");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			HttpSession session = request.getSession();
			session.setAttribute("username", creds.getUsername());
			session.setAttribute("authenticated", false);

			if( checkCredentials(creds) )
			{
				printWriter.println(SUCCESS_HTML);
				session.setAttribute("authenticated", true);
				response.setStatus(HttpServletResponse.SC_OK);
			}
			else
			{
				printWriter.println("Access Denied!");
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);

			}
		}
		catch( Exception e )
		{
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

	}

	private boolean checkCredentials(final Credentials creds )
	{
		try
		{	
			HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
			Condition condition = new Condition()
					.withComparisonOperator(ComparisonOperator.EQ.toString())
					.withAttributeValueList(new AttributeValue().withS(creds.getUsername()));
			scanFilter.put("username", condition);
			ScanRequest scanRequest = new ScanRequest(TABLE_NAME).withScanFilter(scanFilter);
			ScanResult scanResult = dynamoDB.scan(scanRequest);

			for(Map<String, AttributeValue> resultMap : scanResult.getItems())
			{
				if(resultMap.get("password").getS().equals(creds.getPassword()))
					return true;
			}

		}
		catch( Exception e )
		{
			logger.error(e);
		}
		return false;

	}
}
