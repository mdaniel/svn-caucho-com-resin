package example;

import java.io.Serializable;
import java.util.Enumeration;

import javax.ejb.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.caucho.ejb.AbstractEntityBean;

/**
 * Implementation class for the Course bean.
 *
 * <p>Its methods will be called only by the
 * EJB container, and not ever by any client programs that we write.
 * Instead, we call methods in the Remote Interface which will prompt the
 * container to access methods in this class on our behalf. The container
 * will also call the various housekeeping methods described below when it
 * sees fit.
 *
 * <p> This CMP bean uses the following schema:
 *
 * <code><pre>
 *   DROP TABLE find_courses;
 *   CREATE TABLE find_courses (
 *     course_id VARCHAR(250) NOT NULL,
 *     instructor VARCHAR(250),
 *
 *     PRIMARY KEY(course_id)
 *   );
 *
 *   INSERT INTO find_xcourses VALUES('Potions', 'Severus Snape');
 *   INSERT INTO find_xcourses VALUES('Transfiguration', 'Minerva McGonagall');
 *   INSERT INTO find_xcourses VALUES('Defense Against the Dark Arts', 'Remus Lupin');
 * </pre></code>
 * The implementation class for the Course bean. Its methods will be
 * called only by the EJB container, and not by the client programs.
 * The client calls methods in the local interface (Course) which will
 * use the Resin-CMP-generated stub to access methods in this class
 * on our behalf.
 *
 * @ejb.bean name="course" view-type="local" type="CMP"
 *           reentrant="False" schema="courses" primkey-field="id"
 * @ejb.pk class="java.lang.String"
 * @ejb.home generate="local" local-class="example.CourseHome"
 * @ejb.interface generate="local" local-class="example.Course"
 *
 * @ejb.finder signature="java.util.Collection findAll()"
 *             query="SELECT o FROM courses o"
 *
 * @ejb.finder signature="Course findByInstructor(String o)"
 *             query="SELECT o FROM courses o WHERE o.instructor=?1"
 *
 * @ejb.persistence table-name="find_xcourses"
 */
public abstract class CourseBean extends AbstractEntityBean {

  /**
   * Returns the id (and name) of this course.
   *
   * <p>Each cmp-field described in the deployment descriptor needs to
   * be matched in the implementation class by abstract setXXX and
   * getXXX methods. The container will take care of implementing them.
   *
   * <p>Unless you make these methods available in the Local Interface,
   * you will never be able to access them from an EJB client such as
   * a servlet.
   *
   * <p>Resin-CMP will implement the getCourseId method.
   *
   * @ejb.interface-method
   * @ejb.persistence column_name="course_id"
   * @ejb.pk-field
   *
   * @return the course id
   */
  public abstract String getId();

  /**
   * Sets the id (and name) of this course.  Because the
   * course id is the bean's primary key, clients may not call it.
   * setId may only be called in the ejbCreate method.
   *
   * <p>Resin-EE will implement the setCourseId methods.
   *
   * @param courseId the new course id
   *
   * @exception EJBException if the database call or the transaction fails.
   */
  public abstract void setId(String courseId);

  /**
   * returns the name of the instructor who is teaching this
   * course.
   *
   * <p>Resin-EE will implement the getInstructor method.
   *
   * @ejb.interface-method
   * @ejb.persistence column-name="instructor"
   *
   * @return the name of the course's instructor.
   *
   * @exception EJBException if the database call or the transaction fails.
   */
  public abstract String getInstructor();

  /**
   * Sets the name of the instructor who is teaching this course.
   *
   * <p>Resin-EE will implement the setInstructor method.
   *
   * @param instructor the name of the new course instructor.
   *
   * @exception EJBException if the database call or the transaction fails.
   */
  public abstract void setInstructor(String instructor);
}
