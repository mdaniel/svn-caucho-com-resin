package test;

import java.io.*;

import javax.servlet.http.*;
import javax.servlet.*;

import java.util.*;

public class BeanServlet extends HttpServlet {
  private ArrayList _beans = new ArrayList();

  public void addBean(Bean bean)
  {
    _beans.add(bean);
  }

  public void doGet (HttpServletRequest req,
                     HttpServletResponse res)
    throws ServletException, IOException
  {
    res.setContentType("text/html");

    PrintWriter out = res.getWriter();

    for (int i = 0; i < _beans.size(); i++) {
      Bean bean = (Bean) _beans.get(i);

      out.println(bean.getValue() + "<br>");
    }

    out.close();
  }
}
