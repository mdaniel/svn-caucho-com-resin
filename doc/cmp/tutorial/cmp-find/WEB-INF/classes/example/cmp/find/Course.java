package example.cmp.find;

import javax.ejb.*;

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
   * returns the id (and name) of this course (CMP field).
   * This is also the primary key as defined in the deployment descriptor.
   */
  public String getCourseId();

  /**
   * returns the name of the instructor who is teaching this course (CMP field).
   */
  public String getInstructor();
}
