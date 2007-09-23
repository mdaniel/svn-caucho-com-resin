package example;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.servlets.comet.*;

public class TestCometServlet extends AbstractCometServlet
{
  private @Resource ScheduledExecutorService _timer;

  private TimerManager _timerManager;
  
  private ArrayList<CometItem> _itemList
    = new ArrayList<CometItem>();

  public void init()
  {
    _timerManager = new TimerManager(_timer);
  }
  
  @Override
  public boolean service(HttpServletRequest req,
                         HttpServletResponse res,
                         CometController controller)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();
    res.setHeader("Cache-Control", "no-cache, must-revalidate");
    res.setHeader("Expires", "Mon, 27 Jul 1997 05:00:00 GMT");

    res.setContentType("text/html");

    out.println("<html><body>");

    // Padding needed because Safari needs at least 1k data before
    // it will start progressive rendering.
    out.print("<!--");
    for (int i = 0; i < 1024; i++) {
      if (i % 64 == 0)
	out.println();
      
      out.print("*");
    }
    out.println();
    out.println("-->");

    // Add the comet item to the controller
    
    _timerManager.addCometItem(new CometItem(controller));

    return true;
  }
  
  @Override
  public boolean resume(HttpServletRequest req,
                         HttpServletResponse res,
                         CometController controller)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    out.println("<script type='text/javascript'>");
    out.println("window.parent.comet_update(" + req.getAttribute("comet.count") + ");");
    out.println("</script>");

    return true;
  }

  public void destroy()
  {
    _timerManager.close();
  }
}
