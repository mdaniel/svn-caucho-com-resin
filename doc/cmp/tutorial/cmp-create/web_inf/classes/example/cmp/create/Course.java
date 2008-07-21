package example.cmp.create;

import javax.ejb.*;

/**
 * Local interface for the Course bean.
 *
 * The Remote Interface represents one entity; in this case a course.
 *
 * <p>The idea is that you use the Home Interface to obtain references
 * to whatever entities you're interested in. Each entity that you
 * get from the Home Interface (using its create or finder methods)
 * is then represented by its Remote Interface.
 *
 */
public interface Course extends EJBLocalObject {

  /**
   * returns the ID of the course. This is also the primary key as defined
   * in ejb-jar.xml.
   */
  public String getCourseId();

  /**
   * returns the name of the instructor who is teaching this course.
   */
  public String getInstructor();
}
