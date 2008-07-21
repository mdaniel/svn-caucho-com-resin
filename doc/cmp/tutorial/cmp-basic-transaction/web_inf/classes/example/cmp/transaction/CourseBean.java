package example.cmp.transaction;

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
 */
public abstract class CourseBean extends AbstractEntityBean {


  /**
   * Returns the ID of this course (CMP field). This is also the primary key.
   */
  public abstract int getId();

  /**
   * Returns the name of this Course (CMP field).
   */
  public abstract String getName();

  /**
   * SSets the name of this Course (CMP field).
   */
  public abstract void setName(String val);

  /**
   * Sets the maximum amount of students allowed to be enrolled in this course
   * (CMP field).
   */
  public abstract void setMaxStudentAmount(int amount);

  /**
   * returns the maximum amount of students allowed to be enrolled in this
   * course (CMP field).
   */
  public abstract int getMaxStudentAmount();

  /**
   * Returns a <code>Collection</code> of all Student who are currently
   * enrolled in this Course (CMR field).
   */
  abstract public Collection getStudentList();

  /**
   * Returns true if the course is full and no more students can enroll in it.
   */
  public boolean isFull()
  {
    int studentCount = getStudentList().size();
    if (studentCount == getMaxStudentAmount())
      return true;
    return false;
  }
}
