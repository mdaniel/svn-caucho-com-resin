package example.cmp.id;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the Student bean.
 */
public interface Student extends EJBLocalObject {

  /**
   * Returns the student's name.
   */
  String getName();

  /**
   * Returns the student's quidditch scores.
   */
  Quidditch getQuidditch();
}
