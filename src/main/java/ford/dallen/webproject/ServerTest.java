/**
 * 
 */
package ford.dallen.webproject;


import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Dallen
 *
 */
public class ServerTest extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2487041700575948185L;
	private static final int PAGE_SIZE = 3000;
	private static final String INDEX_HTML = loadIndex();
	
	private static String loadIndex()
	{
		/*
		 * try ( BufferedReader reader = new BufferedReader( new InputStreamReader(
		 * ServerTest.class.getResourceAsStream("indextest.html")))) { final
		 * StringBuilder page = new StringBuilder(PAGE_SIZE); String line = null;
		 * 
		 * while (( line = reader.readLine()) != null ) { page.append( line ); }
		 * 
		 * return page.toString(); } catch( final Exception exception ) { return "CWD: "
		 * + Paths.get("").toAbsolutePath().toString() + "\n" + getStackTrace( exception
		 * ); }
		 */
		final StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);        
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Sample Application Servlet Page</title>");
        writer.println("</head>");
        writer.println("<body bgcolor=white>");

        writer.println("<table border=\"0\" cellpadding=\"10\">");
        writer.println("<tr>");
        writer.println("<td>");
        writer.println("<img src=\"images/Pivotal_Logo.png\">");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("<h1>Sample Application Servlet</h1>");
        writer.println("</td>");
        writer.println("</tr>");
        writer.println("</table>");

        writer.println("This is the output of a servlet that is part of");
        writer.println("the Hello, World application.");

        writer.println("</body>");
        writer.println("</html>");
        
        return stringWriter.toString();
	}
	
	private static String getStackTrace( final Throwable throwable )
	{
		final StringWriter stringWriter = new StringWriter();
		final PrintWriter printWriter = new PrintWriter( stringWriter, true );
		throwable.printStackTrace(printWriter);
		
		return stringWriter.getBuffer().toString();
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response )
			throws IOException, ServletException
	{
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		
		response.getWriter().println(INDEX_HTML);
	}
	
	/*
	 * @Override public void handle(String target, Request baseRequest,
	 * HttpServletRequest request, HttpServletResponse response) throws IOException,
	 * ServletException {
	 * 
	 * baseRequest.setHandled(true);
	 * 
	 * String pathInfo = request.getPathInfo(); if(
	 * pathInfo.equalsIgnoreCase("/crontask")) { //handleCronTask(request, response
	 * ); } else { handleHttpRequest( request, response ); }
	 * 
	 * }
	 * 
	 * private void handleHttpRequest( HttpServletRequest request,
	 * HttpServletResponse response ) throws IOException {
	 * 
	 * }
	 */
}
