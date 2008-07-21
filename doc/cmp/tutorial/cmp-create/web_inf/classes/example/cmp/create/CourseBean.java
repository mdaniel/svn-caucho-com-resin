package example.cmp.create;

import javax.ejb.*;
import javax.naming.*;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Enumeration;
import com.caucho.ejb.AbstractEntityBean;

/**
 * Implementation class for the Course bean.
 *
 * The implementation class of the <code>Course</code> entity bean. Its methods
 * will be called only by the EJB container, and not ever by any client programs
 * that we write. Instead, we call methods in the Local Interface which will
 * prompt the container to access methods in this class on our behalf.
 *
 * <p><code>AbstractEntityBean</code> is a convenience superclass that provides
 * a set of methods required by the spec.
 *
 * <p>This CMP entity bean use the following schema:
 *
 * <code><pre>
 * DROP TABLE create_courses;
 * CREATE TABLE create_courses (
 *   course_id VARCHAR(250) NOT NULL,
 *   instructor VARCHAR(250),
 *
 *   PRIMARY KEY(course_id)
 * );
 *
 * INSERT INTO create_courses VALUES('Potions', 'Severus Snape');
 * INSERT INTO create_courses VALUES('Transfiguration', 'Minerva McGonagall');
 * INSERT INTO create_courses VALUES('Defense Against the Dark Arts', 'Remus Lupin');
 * </pre></code>
 */
public abstract class CourseBean extends AbstractEntityBean {

  /**
   * Creates a new Course entity.
   * <p><code>ejbCreate</code> methods implement the <code>create</code> methods
   * declared in the Home Interface. This is like a bean "constructor" where
   * entity properties are initialized.
   *
   * @param courseId the name of the course to be created
   * @param name of the instructor who will teach the new course
   */
  public String ejbCreate(String courseId, String instructor)
    throws CreateException
  {
    setCourseId(courseId);
    setInstructor(instructor);

    // With CMP, always return null
    return null;
  }

  /**
   * required by ejbCreate(String, String)
   * <p>The container will call <code>ejbPostCreate</code> after the corresponding
   * <code>ejbCreate</code> has completed and the entity has a new identity.
   * The method is not used in this example.
   */
  public void
  ejbPostCreate(String courseId, String instructor)
  {
  }

  /**
   * returns the id of this course, which is also the name of the course (CMP field).
   * <p>CMP accessor and mutator methods are left for Resin-CMP to implement.
   * Each cmp-field described in the deployment descriptor needs to be matched
   * in the implementation class by abstract setXXX and getXXX methods. The
   * container will take care of implementing them.
   * <p>Note that unless you make these methods available in the Local Interface,
   * you will never be able to access them from an EJB client such as a servlet.
   */
  public abstract String getCourseId();

  /**
   * sets the id of this course (CMP field).
   * <p>CMP accessor and mutator methods are left for Resin-CMP to implement.
   *
   * @param val new id
   */
  public abstract void setCourseId( String val );

  /**
   * returns the name of the instructor who is teaching this course (CMP field).
   * <p>CMP accessor and mutator methods are left for Resin-CMP to implement.
   */
  public abstract String getInstructor();

  /**
   * sets the name of the instructor whi is teaching this course (CMP field).
   * <p>CMP accessor and mutator methods are left for Resin-CMP to implement.
   *
   * @param val new instructor
   */
  public abstract void setInstructor( String val );
}
