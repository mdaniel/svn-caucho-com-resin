package example;

import javax.persistence.*;

/**
 * Local interface for a course taught at Hogwarts, providing
 * methods to view and change it.
 *
 * <code><pre>
 * CREATE TABLE amber_resin_xa_courses (
 *   id INTEGER
 *   course VARCHAR(250),
 *   teacher VARCHAR(250),
 * 
 *   PRIMARY KEY(course_id)
 * );
 * </pre></code>
 */
@Entity
@Table(name="amber_resin_xa_courses")
public class Course {
  @Id
  @Column(name="id")
  @GeneratedValue
  private int _id;
  
  @Basic
  @Column(name="course")
  private String _course;
  
  @Basic
  @Column(name="teacher")
  private String _teacher;

  /**
   * Returns the ID of the course.
   */
  public int getId()
  {
    return _id;
  }
  
  public void setId(int id)
  {
    _id = id;
  }

  /**
   * Returns the course name.
   */
  public String getCourse()
  {
    return _course;
  }

  /**
   * Sets the course name.
   */
  public void setCourse(String course)
  {
    _course = course;
  }

  /**
   * Returns the teacher name.
   */
  public String getTeacher()
  {
    return _teacher;
  }

  /**
   * Sets the teacher name.
   */
  public void setTeacher(String teacher)
  {
    _teacher = teacher;
  }
}
