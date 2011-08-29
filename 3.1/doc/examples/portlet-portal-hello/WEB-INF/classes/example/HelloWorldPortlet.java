package example;

import java.io.IOException;
import java.io.PrintWriter;

import java.text.MessageFormat;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;


public class HelloWorldPortlet implements Portlet {
  static protected final Logger log = 
    Logger.getLogger(HelloWorldPortlet.class.getName());

  final private PortletMode MODE_VIEW = PortletMode.VIEW;
  final private PortletMode MODE_EDIT = PortletMode.EDIT;
  final private PortletMode MODE_GOODBYE = new PortletMode("goodbye");

  private String[][] _locales = new String[][]  {
    { "default","Default for Browser" },
    { "en","English" },
    { "es","Español" },
    { "fr","Français" },
  };

  private PortletConfig _config;

  public HelloWorldPortlet()
  {
  }

  public void init(PortletConfig config)
    throws PortletException
  {
    _config = config;
  }

  /** 
   * This portlet allows a preference named 'locale' to override the locale
   * specified by the browser.
   */ 
  protected Locale getLocale(PortletRequest request)
  {
    PortletPreferences pref = request.getPreferences();
    String locale = pref.getValue("locale",null);

    return (locale != null) ? new Locale(locale) : request.getLocale();
  }

  protected ResourceBundle getBundle(PortletRequest request)
  {
    return _config.getResourceBundle(getLocale(request));
  }

  public void processAction(ActionRequest request, ActionResponse response) 
    throws PortletException, IOException
  {
    PortletMode mode = request.getPortletMode();

    if (mode.equals(MODE_VIEW))
      actionView(request,response);
    else if (mode.equals(MODE_EDIT))
      actionEdit(request,response);
  }

  public void render(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    PortletMode mode = request.getPortletMode();
    WindowState winstate = request.getWindowState();

    if (winstate.equals(WindowState.MINIMIZED))
      return;

    response.setContentType("text/html");
    response.setTitle(getBundle(request).getString("javax.portlet.title"));

    if (mode.equals(MODE_VIEW))
      doView(request,response);
    else if (mode.equals(MODE_EDIT))
      doEdit(request,response);
    else if (mode.equals(MODE_GOODBYE))
      doGoodbye(request,response);
    else
      throw new PortletException("unknown portlet mode: " + mode);
  }

  protected void doView(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    ResourceBundle bundle = getBundle(request);

    String salutation = request.getParameter("salutation");
    if (salutation == null)
      salutation = bundle.getString("world");

    PrintWriter out = new PrintWriter(response.getWriter());

    String hello = bundle.getString("hello");
    out.println("<p>" + hello + " " + salutation + "</p>");

    PortletURL goodbyeUrl = response.createRenderURL();
    goodbyeUrl.setPortletMode(MODE_GOODBYE);

    String youcansaygoodbye = MessageFormat.format(
        bundle.getString("youcansaygoodbye"),
        new Object[] { "<a href='" + goodbyeUrl + "'>", "</a>"});

    out.println("<p>" + youcansaygoodbye + "</p>");

    PortletURL actionUrl = response.createActionURL();
    String locale = request.getPreferences().getValue("locale","default");
    out.println("<form method='POST' action='" + actionUrl + "'>");
    out.println("<select name='locale'>");
    for (int i = 0; i < _locales.length; i++) {
      String value = _locales[i][0];
      String text = _locales[i][1];
      out.print("<option value='" + value + "'");
      if (value.equals(locale))
        out.print(" selected='SELECTED'");
      out.println(">" + text + "</option>");
    }
    out.println("</select>");

    String ok = bundle.getString("ok");
    out.println("<input type='submit' value='" + ok + "'/><br>");
    out.println("</form>");
  }

  public void actionView(ActionRequest request, ActionResponse response)
    throws PortletException, IOException
  {
    String locale = request.getParameter("locale");
    if (locale != null) {
      if (locale.equals("default"))
        locale = null;

      PortletPreferences pref = request.getPreferences();

      pref.setValue("locale",locale);
      pref.store();
    }
  }


  protected void doEdit(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    ResourceBundle bundle = getBundle(request);
    String salutation = request.getParameter("salutation");
    if (salutation == null)
      salutation = bundle.getString("world");

    PrintWriter out = new PrintWriter(response.getWriter());

    PortletURL submitUrl = response.createActionURL();
    String ok = bundle.getString("ok");
    String cancel = bundle.getString("cancel");
    out.println("<form method='POST' action='" + submitUrl + "'>");
    out.println("<p>");
    out.println("<input name='salutation' value='" + salutation + "'/><br>");
    out.println("</p>");
    out.println("<input type='submit' value='" + ok + "'/>");
    out.println("<input type='submit' name='CANCEL' value='" + cancel + "'/>");
    out.println("</form>");
  }

  public void actionEdit(ActionRequest request, ActionResponse response)
    throws PortletException, IOException
  {
    if (request.getParameter("CANCEL") != null) {
      response.setPortletMode(MODE_VIEW);
      return;
    }

    ResourceBundle bundle = getBundle(request);

    String salutation = request.getParameter("salutation");
    if (salutation == null)
      salutation = bundle.getString("world");
    if (salutation != null)
      response.setRenderParameter("salutation",salutation);

    // after a sucessful EDIT, switch to VIEW
    response.setPortletMode(MODE_VIEW);
  }


  protected void doGoodbye(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    ResourceBundle bundle = getBundle(request);

    String salutation = request.getParameter("salutation");
    if (salutation == null)
      salutation = bundle.getString("world");

    PrintWriter out = new PrintWriter(response.getWriter());
    String goodbye = bundle.getString("goodbye");
    out.println("<p>" + goodbye + " " + salutation + "</p>");
  }

  public void destroy()
  {
  }
}


