package example;

import javax.jws.WebService;
import javax.jws.WebMethod;

@WebService
public class HelloService {
  @WebMethod
  public String hello()
  {
     return "hello, world!";
  }
}
