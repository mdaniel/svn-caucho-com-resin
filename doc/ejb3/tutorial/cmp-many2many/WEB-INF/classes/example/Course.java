package example;

import java.util.Collection;

import javax.ejb.Entity;
import javax.ejb.Table;
import javax.ejb.Id;
import javax.ejb.Basic;
import javax.ejb.Column;
import javax.ejb.JoinColumn;
import javax.ejb.ManyToMany;
import static javax.ejb.GeneratorType.AUTO;

/**
 * Implementation class for the Course bean.
 *
 * <code><pre>
 * CREATE TABLE ejb3_many2many_course (
 *   course_id INTEGER PRIMARY KEY auto_increment,
 *   name VARCHAR(250),
 * );
 * </pre></code>
 */
@Entity
@Table(name="ejb3_many2many_course")
public class Course {
  private long _id;
  private String _name;

  public Course()
  {
  }

  public Course(String name)
  {
    setName(name);
  }

  /**
   * Gets the id.
   */
  @Id(generate=AUTO)
  @Column(name="course_id")
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
   * Returns the name of the course.
   */
  @Basic
  @Column(unique=true, nullable=false)
  public String getName()
  {
    return _name;
  }
  
  /**
   * Sets the name of the course.
   */
  public void setName(String name)
  {
    _name = name;
  }
}
