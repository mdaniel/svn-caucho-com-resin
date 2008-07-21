package example.cmp.map;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * The composite grade key.  The fields names match the primary key
 * of the Grade object.
 */
public class GradeKey {
  public Student student;
  public Course course;
  
  /**
   * Null constructor for the GradeKey.
   */
  public GradeKey()
  {
  }
  
  /**
   * Create a new GradeKey.
   */
  public GradeKey(Student student, Course course)
  {
    this.student = student;
    this.course = course;
  }

  /**
   * Overrides equals to return true when the key matches.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof GradeKey))
      return false;

    GradeKey key = (GradeKey) obj;

    return student.equals(key.student) && course.equals(key.course);
  }

  /**
   * Returns the key's hash code.
   */
  public int hashCode()
  {
    return 65521 * student.hashCode() + course.hashCode();
  }
}
