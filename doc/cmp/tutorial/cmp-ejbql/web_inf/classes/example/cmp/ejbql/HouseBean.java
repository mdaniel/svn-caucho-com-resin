package example.cmp.ejbql;

import java.util.*;

/**
 * Implementation class for the House bean.
 *
 * Each instance of StudentBean maps to a table entry of "one2many_houses",
 * where student is defined.
 *
 * <p>HouseBean is abstract since it's taking advantage of container-managed
 * persistence.  Resin-CMP will create the implementation of the abstract
 * methods.
 *
 * <p>HouseBean also takes advantage of the AbstractEntityBean
 * implementation.  AbstractEntityBean is just a stub
 * EntityBean implementation with default methods to make life
 * a little more sane for simple beans.
 */
abstract public class HouseBean extends com.caucho.ejb.AbstractEntityBean {

  /**
   * returns the name of the house (CMP field). The name is the primary key as
   * defined in the deployment descriptor.
   */
  abstract public String getName();

  /**
   * returns a <code>Collection</code> of all Students living in this House
   * (CMR field).
   */
  abstract public Collection getStudentList();

}
