package example.cmp.many2one;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;


/**
 * Remote interface for a student instance.
 */
public interface Student extends EJBLocalObject {
  /**
   * Returns the student's name.
   */
  String getName();
  /**
   * Returns the student's house
   */
  House getHouse();
  /**
   * Sets the student's house
   */
  void setHouse(House house);
}
