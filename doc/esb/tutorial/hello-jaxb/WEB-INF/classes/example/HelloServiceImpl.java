package example;

import javax.xml.bind.annotation.*;

@XmlRootElement
public class HelloServiceImpl implements HelloService {
  @XmlElement(name="hello")
  private String _hello;

  /**
   * Returns "hello, world".
   */
  public String hello()
  {
    return _hello;
  }
}
