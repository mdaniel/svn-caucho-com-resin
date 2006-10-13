package example;

import java.util.List;

public class MovieLister {
  private MovieFinder _finder;
  
  /**
   * Sets the finder.
   */
  public void setFinder(MovieFinder finder)
  {
    _finder = finder;
  }
  
  /**
   * Returns movies by a particular director.
   */
  public Movie []moviesDirectedBy(String director)
  {
    List movies = _finder.findAll();

    for (int i = movies.size() - 1; i >= 0; i--) {
      Movie movie = (Movie) movies.get(i);

      if (! director.equals(movie.getDirector()))
	movies.remove(i);
    }

    return (Movie []) movies.toArray(new Movie[movies.size()]);
  }
}
