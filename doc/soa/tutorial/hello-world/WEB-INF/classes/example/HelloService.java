package example;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface HelloService {
  /**
   * Returns "hello, world".
   */
  @WebMethod
  public HelloResult hello();
}
