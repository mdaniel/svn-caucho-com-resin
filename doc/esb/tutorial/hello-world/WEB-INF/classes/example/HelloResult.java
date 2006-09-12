package example;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="hello")
public class HelloResult implements Serializable {

  @XmlElement
  public String value = "hello, world";

  public String toString()
  {
    return value;
  }
}
