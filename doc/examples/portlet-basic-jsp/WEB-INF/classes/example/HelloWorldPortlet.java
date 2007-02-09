package example;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.*;

public class HelloWorldPortlet extends GenericPortlet {

  private String _view = "/hello.jsp";

  /**
   * Set the jsp for performing the render, the default is "/hello.jsp".
   */
  public void setView(String view)
  {
    _view = view;
  }

  public void init(PortletConfig portletConfig)
    throws PortletException
  {
    super.init(portletConfig);

    String p = getInitParameter("view");
    if (p != null)
      setView(p);
  }

  public void processAction(ActionRequest request, ActionResponse response) 
    throws PortletException, IOException
  {
    PortletMode mode = request.getPortletMode();

    if (mode.equals(PortletMode.EDIT)) { 

      // get the values submitted with the form

      String identity = request.getParameter("identity");
      String color = request.getParameter("color");

      // set the values of the render parameters

      response.setRenderParameter("identity", identity); 
      response.setRenderParameter("color", color); 

      // switch to View mode
      
      response.setPortletMode(PortletMode.VIEW);
    }
  }

  /**
   * Called just prior to a render, set's the following request attributes:
   *
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

    request.setAttribute("identity", identity);
    request.setAttribute("color", color);
  }

  /**
   * Dispatch to a jsp or servlet.
   */
  protected void dispatch( RenderRequest request, 
                           RenderResponse response, 
                           String path )
    throws PortletException, IOException
  {
    PortletContext ctx = getPortletContext();
    PortletRequestDispatcher dispatcher = ctx.getRequestDispatcher(path);
    dispatcher.include(request, response);
  }

  /**
   * Call prepareObjects() then dispatch to the view.
   */
  public void render(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    prepareObjects(request, response);
    dispatch(request, response, _view);
  }

  public void destroy()
  {
  }
}


