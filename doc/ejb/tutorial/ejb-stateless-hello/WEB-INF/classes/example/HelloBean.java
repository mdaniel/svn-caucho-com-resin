package example;

import java.rmi.*;
import javax.ejb.*;

/**
 * The trivial stateless session bean returns a greeting.
 */
public class HelloBean extends com.caucho.ejb.AbstractSessionBean {
  /**
   * The ejbCreate method initializes the stateless bean instance.
   * For example, the instance might look up any JNDI objects.
   * Because stateless bean instances are reused, like servlets,
   * the cached lookup can save time.
   *
   * <p>All stateless session beans need to implement ejbCreate.
   */
  public void ejbCreate()
  {
  }

  /**
   * This business method returns a trivial string.  To be accessible,
   * to the client, the method needs to be listed in the remote interface,
   * Hello.
   */
  public String hello()
  {
    return "Hello, world";
  }
}
