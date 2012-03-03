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


    // url links to change the name

    PortletURL harryUrl = response.createRenderURL();
    harryUrl.setParameter("identity", "Harry");
    harryUrl.setParameter("color", color);

    PortletURL ronUrl = response.createRenderURL();
    ronUrl.setParameter("identity", "Ron");
    ronUrl.setParameter("color", color);

    out.println("<h3>Pick a name:</h3>");
    out.println("<ul>");
    out.println("<li><a href='" + harryUrl.toString() + "'>Harry</a>");
    out.println("<li><a href='" + ronUrl.toString() + "'>Ron</a>");
    out.println("</ul>");

    // url links to change the color

    PortletURL silverUrl = response.createRenderURL();
    silverUrl.setParameter("identity", identity);
    silverUrl.setParameter("color", "silver");

    PortletURL goldUrl = response.createRenderURL();
    goldUrl.setParameter("identity", identity);
    goldUrl.setParameter("color", "gold");

    out.println("<h3>Or pick a color:</h3>");
    out.println("<ul>");
    out.println("<li><a href='" + silverUrl.toString() + "'>silver</a>");
    out.println("<li><a href='" + goldUrl.toString() + "'>gold</a>");
    out.println("</ul>");
  }

  public void destroy()
  {
  }
}


