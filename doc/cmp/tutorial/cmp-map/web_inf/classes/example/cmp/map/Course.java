package example.cmp.map;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the Course bean.
 */
public interface Course extends EJBLocalObject {

  /**
   * Returns the course's name.
   */
  String getName();
}
