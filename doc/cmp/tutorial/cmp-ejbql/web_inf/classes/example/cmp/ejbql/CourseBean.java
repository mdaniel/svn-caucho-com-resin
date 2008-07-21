package example.cmp.ejbql;

import java.io.Serializable;
import java.util.Enumeration;

import javax.ejb.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.caucho.ejb.AbstractEntityBean;

import java.util.Collection;

/**
 * Implementation class for the Course bean.
 * This is our implementation class. Its methods will be called only by the
 * EJB container, and not ever by any client programs that we write.
 * Instead, we call methods in the Remote Interface which will prompt the
 * container to access methods in this class on our behalf. The container
 * will also call the various housekeeping methods described below when it
 * sees fit.
 */
public abstract class CourseBean extends AbstractEntityBean {
  /**
   * CMP accessor and mutator methods are left for Resin-CMP to implement.
   * Each cmp-field described in the deployment descriptor needs to be matched
   * in the implementation class by abstract setXXX and getXXX methods. The
   * container will take care of implementing them.
   * Note that unless you make these methods available in the Local Interface,
   * you will never be able to access them from an EJB client such as a servlet.
   */
  public abstract String getName();

  /**
   * CMP accessor and mutator methods are left for Resin-CMP to implement.
   */
  public abstract void setName(String val);

  /**
   * Returns a <code>Collection</code> of all Student who are currently
   * enrolled in this Course.
   */
  abstract public Collection getStudentList();

  /**
   * returns the Teacher who is teaching this Course.
   */
  abstract public Teacher getTeacher();
}
