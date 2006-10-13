package example;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.*;

public class HelloWorldPortlet implements Portlet {
  private PortletConfig _config;

  public HelloWorldPortlet()
  {
  }

  public void init(PortletConfig config)
    throws PortletException
  {
    _config = config;
  }

  public void processAction(ActionRequest request, ActionResponse response) 
    throws PortletException, IOException
  {
    // get the values submitted with the form

    String identity = request.getParameter("identity");
    String color = request.getParameter("color");

    // set the values of the render parameters

    response.setRenderParameter("identity", identity); 
    response.setRenderParameter("color", color); 
  }

  public void render(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    String identity = request.getParameter("identity");
    if (identity == null)
      identity = "World";

    String color = request.getParameter("color");
    if (color == null)
      color = "silver";

    response.setContentType("text/html");

    PrintWriter out = response.getWriter();

    out.println("Hello, " + identity + ".");
    out.println("Your favorite color is " + color);

    PortletURL submitUrl = response.createActionURL();

    out.println("<form method='POST' action='" + submitUrl + "'>");
    out.println("Name:");
    out.println("<input type='text' name='identity' value='" + identity + "'>");
    out.println("<br>");
    out.println("Color:");
    out.println("<input type='text' name='color' value='" + color + "'>");
    out.println("<br>");
    out.println("<input type='submit'");
    out.println("</form>");
  }

  public void destroy()
  {
  }
}


