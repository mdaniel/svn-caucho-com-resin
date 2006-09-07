package example;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="hello")
public class HelloResult {

  @XmlElement
  public String value = "hello, world";

  public String toString()
  {
    return value;
  }
}
