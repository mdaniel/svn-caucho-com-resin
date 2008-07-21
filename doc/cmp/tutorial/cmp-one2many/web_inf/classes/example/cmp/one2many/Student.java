package example.cmp.one2many;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the Student bean.
 */
public interface Student extends EJBLocalObject {

  /**
   * Returns the student's name (CMP field).
   */
  String getName();

  /**
   * Returns the student's house (CMR field).
   */
  House getHouse();

  /**
   * Sets the student's house (CMR field).
   *
   * @param <code>House</code> that will be the student's new House
   */
  void setHouse(House house);
}
