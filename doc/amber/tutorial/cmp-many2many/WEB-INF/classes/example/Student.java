package example;

import java.util.Collection;

import javax.ejb.Entity;
import javax.ejb.Table;
import javax.ejb.Id;
import javax.ejb.Basic;
import javax.ejb.Column;
import javax.ejb.JoinColumn;
import javax.ejb.ManyToOne;
import static javax.ejb.GeneratorType.AUTO;

/**
 * Implementation class for the Student bean.
 *
 * <code><pre>
 * CREATE TABLE ejb3_many2many_student (
 *   student_id INTEGER PRIMARY KEY auto_increment,
 *   name VARCHAR(250),
 * );
 * </pre></code>
 */
@Entity
@Table(name="ejb3_many2many_student")
public class Student {
  private long _id;
  private String _name;
  private Collection<Course> _courses;

  public Student()
  {
  }

  public Student(String name)
  {
    setName(name);
  }

  /**
   * Gets the id.
   */
  @Id(generate=AUTO)
  @Column(name="student_id")
  public long getId()
  {
    return _id;
  }

  /**
   * Sets the id.
   */
  public void setId(long id)
  {
    _id = id;
  }
  
  /**
   * Returns the name of the student.
   */
  @Basic
  @Column(unique=true, nullable=false)
  public String getName()
  {
    return _name;
  }
  
  /**
   * Sets the name of the student.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Returns the courses.
   */
  @javax.ejb.ManyToMany(targetEntity="Course")
  @javax.ejb.AssociationTable(
    table=@javax.ejb.Table(name="ejb3_many2many_map"),
    joinColumns=@javax.ejb.JoinColumn(name="student_id"),
    inverseJoinColumns=@javax.ejb.JoinColumn(name="course_id")
  )
  public Collection getCourses()
  {
    return _courses;
  }

  /**
   * Sets the courses.
   */
  public void setCourses(Collection courses)
  {
    _courses = courses;
  }
}
