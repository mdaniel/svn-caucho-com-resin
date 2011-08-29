package example;

import java.io.*;

import javax.annotation.*;
import javax.servlet.*;
import javax.webbeans.In;

public class TestServlet extends GenericServlet {
  @In
  private Theater _theater;
  
  public void service(ServletRequest req,
		      ServletResponse res)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    out.println("<title>Resin/JAXB: " + _theater.getName() + "</title>");
    out.println("<h1>Resin/JAXB: " + _theater.getName() + "</h1>");

    for (Movie movie : _theater.getMovieList()) {
      out.println("<h3>Movie: " + movie.getTitle() + "</h3>");

      if (movie.getStarList().size() > 0) {
	out.println("Starring:");
	
	for (String star : movie.getStarList()) {
	  out.println("<li>" + star);
	}
      }
    }
  }
}
