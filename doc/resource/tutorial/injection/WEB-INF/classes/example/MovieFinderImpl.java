package example;

import java.util.List;
import java.util.ArrayList;

public class MovieFinderImpl implements MovieFinder {
  private ArrayList _list = new ArrayList();

  /**
   * Adds a movie to the list.
   */
  public void addMovie(Movie movie)
  {
    _list.add(movie);
  }
  
  /**
   * Returns all the movies.
   */
  public List findAll()
  {
    return new ArrayList(_list);
  }
}
