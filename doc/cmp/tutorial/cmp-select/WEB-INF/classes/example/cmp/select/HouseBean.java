package example.cmp.select;

import javax.ejb.*;
import java.util.*;

/**
 * Implementation class for the House bean.
 *
 * <code><pre>
 * CREATE TABLE select_house (
 *   name VARCHAR(250) NOT NULL,
 *
 *   PRIMARY KEY(name)
 * );
 * </pre></code>
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
   * Returns the name of the house. The name is the primary key as
   * defined in the deployment descriptor.
   */
  abstract public String getName();

  /**
   * returns a <code>Collection</code> of all Students living in this House
   * (CMR field).
   */
  public abstract Collection getStudentList();

  /**
   * Returns a <code>Collection</code> of <code>String</code>s of all
   * Student's names who are boys.  Since the ejbSelect method can't be
   * exposed in the local interface, we need to add a business method
   * to return the names.
   *
   * <p>As the example shows, ejbSelect methods can return collections
   * and values of any type storable in the database.  In contrast, find
   * methods must always return the local interface of entity beans.
   *
   * <p>The ORDER BY clause is a Resin-CMP extension to the EJB-QL spec.
   * A later version of the EJB spec will almost certainly contain
   * similar functionality.
   *
   * <code><pre>
   * SELECT student.name
   * FROM select_house house, IN(house.studentList) student
   * WHERE student.gender='Boy' AND house=?1
   * ORDER BY student.name
   * </pre></code>
   */
  public abstract Collection ejbSelectAllBoys(House house)
    throws FinderException;

  /**
   * The business method to find the boys in this house.  Because ejbSelect
   * methods can only be called by the bean implementation, we need a
   * business method to return the names.
   */
  public List getAllBoyNamesSorted()
  {
    ArrayList list = null;
    
    try {
      House house = (House) getEntityContext().getEJBLocalObject();
      
      list = (ArrayList) ejbSelectAllBoys(house);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return list;
  }

}
