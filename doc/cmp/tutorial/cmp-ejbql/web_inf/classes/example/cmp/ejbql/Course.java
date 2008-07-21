package example.cmp.ejbql;

import javax.ejb.*;
import java.util.Collection;

/**
 * Local interface for the Course bean.
 * The Local Interface represents one entity; in this case a <code>Course
 * </code>.
 *
 * <p>The idea is that you use the Home Interface to obtain references
 * to whatever entities you're interested in. Each entity that you
 * get from the Home Interface (using its create or finder methods)
 * is then represented by its Local Interface.
 *
 */
public interface Course extends EJBLocalObject {

  /**
   * Get the ID of the course (CMP field).
   * This is also the primary key as defined in the deployment descriptor.
   */
  public String getName();

  /**
   * Returns a <code>Collection</code> of all Students who are currently
   * enrolled in this Course (CMR field).
   */
  public Collection getStudentList();


  /**
   * returns the Teacher who is teaching this Course (CMR field).
   */
  public Teacher getTeacher();
}
