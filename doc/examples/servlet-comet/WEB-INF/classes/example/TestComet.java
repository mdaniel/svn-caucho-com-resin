package example;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.servlets.comet.*;

public class TestComet extends AbstractCometServlet implements Runnable {
  @Resource ScheduledExecutorService _timer;
  
  private ArrayList<CometItem> _itemList
    = new ArrayList<CometItem>();

  public void init()
  {
    _timer.scheduleAtFixedRate(this, 0, 2, TimeUnit.SECONDS);
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

    out.println("<html xmlns='http://www.w3.org/1999/xhtml'>");
    out.println("<head>");
    out.println("  <title>Comet Backend</title>");
    out.println("  <meta http-equiv='ContentType' content='text/html' />");

    out.println("</head>");
    out.println("<body>");

    // Safari needs at least 1k data before it will start progressive
    // rendering.
    out.print("<!--");
    for (int i = 0; i < 1024; i++)
      out.print("*");
    out.println("-->");

    synchronized (_itemList) {
      _itemList.add(new CometItem(controller));
    }

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

    //out.println("<h3>HI</h3>");
    Integer count = (Integer) req.getAttribute("comet.count");

    if (count != null && count > 5)
      return false;
    else
      return true;
  }

  public void run()
  {
    synchronized (_itemList) {
      for (int i = _itemList.size() - 1; i >= 0; i--) {
        CometItem item = _itemList.get(i);

        if (! item.wake())
          _itemList.remove(i);
      }
    }
  }
}
