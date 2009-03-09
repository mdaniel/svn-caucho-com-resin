package example;

import javax.annotation.Resource;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.SUPPORTS;

import com.caucho.config.Name;

/**
 * Implementation of the Hello bean.
 */
@Stateless
public class HelloBean implements Hello {
  @Name("greeting")
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
