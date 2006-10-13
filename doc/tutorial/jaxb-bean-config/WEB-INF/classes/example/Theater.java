package example;

import java.util.*;

import javax.xml.bind.annotation.*;

@XmlRootElement
public class Theater {
  @XmlElement(name="name")
  private String _name;

  @XmlElement(name="movie")
  private ArrayList<Movie> _movieList = new ArrayList<Movie>();

  public String getName()
  {
    return _name;
  }

  public ArrayList<Movie> getMovieList()
  {
    return _movieList;
  }
}
