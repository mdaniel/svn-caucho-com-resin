package example;

import java.io.PrintWriter;
import java.io.IOException;

import javax.portlet.GenericPortlet;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.PortletException;

public class HelloWorldPortlet extends GenericPortlet {
  /**
   * The portlet's main view prints "Hello, World"
   */
  public void doView(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    PrintWriter out = response.getWriter();

    out.println("Hello, World");
  }
}


