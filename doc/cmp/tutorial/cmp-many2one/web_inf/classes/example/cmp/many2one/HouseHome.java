package example.cmp.many2one;

import java.rmi.*;
import javax.ejb.*;

/**
 * Remote interface for the house home.
 */
public interface HouseHome extends EJBLocalHome {
  /**
   * Returns the named house.
   */
  House findByPrimaryKey(String name) throws FinderException;
}
