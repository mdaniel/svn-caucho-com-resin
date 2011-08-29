package example;

import javax.annotation.Resource;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.SUPPORTS;

import javax.webbeans.Named;

/**
 * Implementation of the Hello bean.
 */
@Stateless
public class HelloBean implements Hello {
  @Named("greeting")
  private String _greeting;
  
  /**
   * Returns a hello, world string.
   */
  @TransactionAttribute(SUPPORTS)
  public String hello()
  {
    return _greeting;
  }
}
