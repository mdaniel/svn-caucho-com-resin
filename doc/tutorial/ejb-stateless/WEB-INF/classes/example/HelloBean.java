package example;

import javax.annotation.Resource;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.SUPPORTS;

/**
 * Implementation of the Hello bean.
 */
@Stateless
public class HelloBean implements Hello {
  private String _greeting = "Default Hello";

  /**
   * Injector to set the greeting.
   */
  @Resource
  public void setGreeting(String greeting)
  {
    _greeting = greeting;
  }
  
  /**
   * Returns a hello, world string.
   */
  @TransactionAttribute(SUPPORTS)
  public String hello()
  {
    return _greeting;
  }
}
