package example.cmp.many2one;

import java.rmi.*;
import javax.ejb.*;

/**
 * Remote interface for the student home.
 */
public interface StudentHome extends EJBLocalHome {
  /**
   * Returns the named student.
   */
  Student findByPrimaryKey(String name) throws FinderException;
}
