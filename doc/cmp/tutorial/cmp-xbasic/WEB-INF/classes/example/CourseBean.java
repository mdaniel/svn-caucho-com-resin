package example;

import javax.ejb.*;
import javax.naming.*;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Enumeration;
import com.caucho.ejb.AbstractEntityBean;

/**
 * Implementation class for the Course bean.
 *
 * <p>The implementation class of the <code>Course</code> entity bean.
 * Its methods will be called only by the EJB container, never directly
 * by clients.  Clients always call methods in the local interface, calling
 * methods in the Resin-CMP generated stub.  The stub methods will call
 * the CourseBean methods in the correct transaction context.
 *
 * <p><code>AbstractEntityBean</code> is a convenience superclass
 * that provides a set of methods required by the spec that we don't use in
 * this example.
 *
 * <p>This CMP entity bean use the following schema:
 *
 * <code><pre>
 * CREATE TABLE basic_courses (
 *   id VARCHAR(250) NOT NULL,
 *
 *   instructor VARCHAR(250),
 *   PRIMARY KEY(id)
 * );
 * </pre></code>
 *
 * <p>The column names are generated from the field names.  
 * getCourseId becomes course_id and getInstructor becomes instructor.
 * The table name is specified in the ejb deployment descriptor,
 * cmp-basic.ejb.
 *
 * @ejb.bean name="xbasic_course" view-type="local" type="CMP"
 *           schema="courses" primkey-field="id"
 * @ejb.persistence table-name="xbasic_courses"
 * @ejb.pk class="java.lang.String"
 * @ejb.home generate="local" local-class="example.CourseHome"
 * @ejb.interface generate="local" local-class="example.Course"
 */
public abstract class CourseBean extends AbstractEntityBean {
  /**
   * Returns the ID of the course, the primary key.
   *
   * <p>All container-managed fields are abstract since Resin-CMP will
   * generate the SQL and JDBC calls to manage them.
   *
   * <p>Each cmp-field described in the deployment descriptor needs to
   * be matched in the implementation class by abstract setXXX and
   * getXXX methods. The container will take care of implementing them.
   *
   * <p>Note that unless you make these methods available in the
   * local interface, you will never be able to access them
   * from an EJB client such as a servlet.
   *
   * @ejb.interface-method
   * @ejb.persistence column-name="id"
   * @ejb.pk-field
   */
  public abstract String getId();

  /**
   * Sets the primary key of the course, only called from a create method.
   * The primary key may only be set in an ejbCreate method.  Tables
   * that generate the primary key automatically will not define the
   * setXXX method, only the getXXX method.
   *
   * @param courseId the courseId of the new course
   *
   * @ejb.interface-method
   * @ejb.persistence column-name="id"
   */
  public abstract void setId(String courseId);

  /**
   * Returns the name of the instructor for this course.  Since
   * getInstructor is a container managed field (cmp-field),
   * it must be abstract for Resin-CMP to implement.
   *
   * <p>Resin-CMP will automatically cache the value so most
   * calls to getInstructor can avoid database calls.
   *
   * @ejb.interface-method
   * @ejb.persistence column-name="teacher"
   */
  public abstract String getInstructor();

  /**
   * Sets the name of the instructor for this course.  Since setInstructor
   * is a cmp-field, Resin-CMP will implement it.  The value will be
   * written to the database when the transaction completes.
   *
   * @ejb.interface-method
   * @ejb.persistence column-name="teacher"
   */
  public abstract void setInstructor(String instructor);

  /**
   * Swaps the instructors.  Resin-CMP encapsulates all business methods
   * a transaction to protect against concurrent access.  So client code
   * can call the business method without worrying about database
   * consistency.
   *
   * <p>Because concurrent modifications can conflict, it's always possible
   * for a business method to throw a transaction failure exception, a
   * runtime exception.  Depending on the business logic, clients can retry
   * the transaction or just return an error message.
   *
   * @param course the course with the instructor to swap.
   *
   * @exception EJBException if the transaction fails.
   *
   * @ejb.interface-method
   */
  public void swap(Course course)
  {
    String temp = getInstructor();
    setInstructor(course.getInstructor());
    course.setInstructor(temp);
  }
}

    
