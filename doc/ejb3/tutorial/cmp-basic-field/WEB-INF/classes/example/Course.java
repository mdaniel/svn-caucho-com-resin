package example;

import javax.ejb.Entity;
import javax.ejb.Table;
import javax.ejb.Id;
import javax.ejb.Column;
import javax.ejb.Basic;
import static javax.ejb.GeneratorType.AUTO;
import static javax.ejb.AccessType.FIELD;

/**
 * Local interface for a course taught at Hogwarts, providing
 * methods to view and change it.
 *
 * <code><pre>
 * CREATE TABLE ejb3_field_courses (
 *   id INTEGER
 *   course VARCHAR(250),
 *   teacher VARCHAR(250),
 * 
 *   PRIMARY KEY(course_id)
 * );
 * </pre></code>
 */
@Entity(access=FIELD)
@Table(name="ejb3_field_courses")
public class Course {
  @Id(generate=AUTO)
  @Column(name="id")
  private int _id;
  
  @Basic
  @Column(name="course")
  private String _course;
  
  @Basic
  @Column(name="teacher")
  private String _teacher;

  /**
   * Entities need a zero-arg constructor.
   */
  public Course()
  {
  }

  /**
   * Constructor for the init servlet.
   */
  public Course(String course, String teacher)
  {
    _course = course;
    _teacher = teacher;
  }
  
  /**
   * Returns the course name.
   */
  public String course()
  {
    return _course;
  }

  /**
   * Returns the teacher name.
   */
  public String teacher()
  {
    return _teacher;
  }
}
