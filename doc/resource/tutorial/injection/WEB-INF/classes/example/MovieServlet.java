package example;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

public class MovieServlet extends HttpServlet {
  private MovieLister _movieLister;

  /**
   * Sets the movie lister.
   */
  public void setMovieLister(MovieLister lister)
  {
    _movieLister = lister;
  }
  
  /**
   * Returns movies by a particular director.
   */
  public void doGet(HttpServletRequest request,
		    HttpServletResponse response)
    throws IOException, ServletException
  {
    PrintWriter out = response.getWriter();

    response.setContentType("text/html");

    String director = request.getParameter("director");

    if (director == null) {
      out.println("No director specified");
      return;
    }

    out.println("<h1>Director: " + director + "</h1>");

    Movie []movies = _movieLister.moviesDirectedBy(director);

    for (int i = 0; i < movies.length; i++)
      out.println(movies[i].getTitle() + "<br>");
  }
}
