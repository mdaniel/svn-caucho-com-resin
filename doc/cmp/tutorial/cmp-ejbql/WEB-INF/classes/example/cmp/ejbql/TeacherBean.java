package example.cmp.ejbql;

/**
 * Implementation class for the Teacher bean.
 */
abstract public class TeacherBean extends com.caucho.ejb.AbstractEntityBean {

  /**
   * returns the <code>Teacher</code>'s name
   */
  abstract public String getName();

  /**
   * Returns the Course that this Teacher is teaching (CMR field).
   */
  abstract public Course getCourse();

  //abstract public void setCourse();

}
