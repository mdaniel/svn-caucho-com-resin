package example.cmp.ejbql;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the House bean.
 */
public interface House extends EJBLocalObject {

  /**
   * returns the name of the house (CMP field).
   */
  public String getName();

  /**
   * returns a <code>Collection</code> of all Students living in this House
   * (CMR field).
   */
  public Collection getStudentList();

}
