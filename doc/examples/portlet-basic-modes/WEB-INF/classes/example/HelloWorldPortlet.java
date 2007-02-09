package example;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.*;

public class HelloWorldPortlet extends GenericPortlet {

  public void processAction(ActionRequest request, ActionResponse response) 
    throws PortletException, IOException
  {
    PortletMode mode = request.getPortletMode();

    if (mode.equals(PortletMode.EDIT)) { 

      // get the values submitted with the form

      String identity = request.getParameter("identity");
      String color = request.getParameter("color");

      // set the values of the render parameters

      response.setRenderParameter("identity",identity); 
      response.setRenderParameter("color",color); 

      // switch to View mode
      
      response.setPortletMode(PortletMode.VIEW);
    }
  }

  public void render(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    PortletMode mode = request.getPortletMode();

    // prepare objects in common with all modes and store them
    // as request attributes
    prepareObjects(request, response);

    if (mode.equals(PortletMode.EDIT)) {
      doEdit(request,response);
    }
    else {
      doView(request,response);
    }
  }

  /**
   * Set's the following request attributes:
   * <dl>
   * <dt>identity
   * <dd>the identity 
   * <dt>color
   * <dd>the color
   * </dl>
   */ 
  protected void prepareObjects(RenderRequest request, RenderResponse response)
  {
    String identity = request.getParameter("identity");
    if (identity == null)
      identity = "World";

    String color = request.getParameter("color");
    if (color == null)
      color = "silver";

    request.setAttribute("identity",identity);
    request.setAttribute("color",color);
  }

  protected void doView(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    String identity = (String) request.getAttribute("identity");
    String color = (String) request.getAttribute("color");

    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    out.println("Hello, " + identity + ".");
    out.println("Your favorite color is " + color);

    PortletURL editUrl = response.createRenderURL();
    editUrl.setPortletMode(PortletMode.EDIT);
    
    out.println("<p>");
    out.println("<a href='" + editUrl + "'>Edit</a>");
  }

  protected void doEdit(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    String identity = (String) request.getAttribute("identity");
    String color = (String) request.getAttribute("color");

    PortletURL submitUrl = response.createActionURL();

    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

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


