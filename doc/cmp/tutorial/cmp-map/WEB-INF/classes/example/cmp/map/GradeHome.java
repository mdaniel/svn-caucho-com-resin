package example.cmp.map;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the Grade bean.
 */
public interface GradeHome extends EJBLocalHome {
  /**
   * Returns the <code>Grade</code> entity that matches the key.
   */
  public Grade findByPrimaryKey(GradeKey gradeKey)
    throws FinderException;
}
