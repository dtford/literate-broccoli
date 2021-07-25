package ford.dallen.webproject;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
//import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

public class ServletUtils {

	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public static AmazonDynamoDB DBinit(AmazonDynamoDB dynamoDB) throws Exception {
		/*
		 * The ProfileCredentialsProvider will return your [default]
		 * credential profile by reading from the credentials file located at
		 * (C:\\Users\\Dallen\\.aws\\credentials).
		 */

		// Use InstanceProfileCredentialsProvider.getInstance() for AWS Deployment
		// Use new ProfileCredentialsProvider() ONLY for local hosting
		AWSCredentialsProvider credentialsProvider = InstanceProfileCredentialsProvider.getInstance();
		//AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
		try {
			credentialsProvider.getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
							"Please make sure that your credentials file is at the correct " +
							"location (C:\\Users\\Dallen\\.aws\\credentials), and is in valid format.",
							e);
		}
		dynamoDB = AmazonDynamoDBClientBuilder.standard()
				.withCredentials(credentialsProvider)
				.withRegion("us-east-2")
				.build();
		return dynamoDB;
	}

	public static String authAndGetUName( HttpServletRequest request, HttpServletResponse response ) throws IOException
	{
		HttpSession session = request.getSession(false);

		String username = null;

		if( session != null )
		{				
			username = (String)session.getAttribute("username");				
			boolean authenticated = (boolean)session.getAttribute("authenticated");

			if(!authenticated)
			{
				response.setContentType("text/html");
				response.getWriter().println("Not Authenticated");
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}
		}
		return username;
	}
}
