package example.entity.home;

import javax.ejb.*;

/**
 * Implementation of the HomeBean.
 */
public class HomeBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns hello, world.
   */
  public String ejbHomeHello()
  {
    return "hello, world";
  }
  
  /**
   * Adds two numbers.
   */
  public int ejbHomeAdd(int a, int b)
  {
    return a + b;
  }
  
  /**
   * The primary key is just a dummy.
   */
  public String ejbFindByPrimaryKey(String key)
    throws FinderException
  {
    throw new FinderException("not supported");
  }
}
