package example;

import java.util.*;

import javax.xml.bind.annotation.*;

@XmlRootElement
public class Movie {
  @XmlElement(name="title")
  private String _title;
    
  @XmlElement(name="star")
  private ArrayList<String> _starList = new ArrayList<String>();

  public String getTitle()
  {
    return _title;
  }

  public ArrayList<String> getStarList()
  {
    return _starList;
  }
}
