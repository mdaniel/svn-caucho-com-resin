package example.cmp.transaction;

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
   * Returns the ID of this course (CMP field). This is also the primary key.
   */
  public int getId();

  /**
   * Get the ID of the course (CMP field).
   */
  public String getName();

  /**
   * Returns a <code>Collection</code> of all Students who are currently
   * enrolled in this Course (CMR field).
   */
  public Collection getStudentList();

  /**
   * Returns true if the course is full and no more students can enroll in it.
   */
  public boolean isFull();

}
