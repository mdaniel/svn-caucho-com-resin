package example;

import java.util.Collection;

import javax.ejb.Entity;
import javax.ejb.Table;
import javax.ejb.Id;
import javax.ejb.Basic;
import javax.ejb.Column;
import javax.ejb.DiscriminatorColumn;
import javax.ejb.Inheritance;
import static javax.ejb.AccessType.*;
import static javax.ejb.GeneratorType.*;

/**
 * Implementation class for the Student bean.
 *
 * <code><pre>
 * CREATE TABLE ejb3_inherit_student (
 *   id INTEGER PRIMARY KEY auto_increment,
 *   type VARCHAR(10),
 *   name VARCHAR(250),
 * );
 * </pre></code>
 */
@Entity(access=FIELD)
@Table(name="ejb3_inherit_student")
@Inheritance(discriminatorValue="student")
@DiscriminatorColumn(name="type")  
public class Student {
  @Id(generate=AUTO)
  @Column(name="id")
  private long _id;
  
  @Basic
  @Column(unique=true, nullable=false)
  private String _name;

  public Student()
  {
  }

  public Student(String name)
  {
    _name = name;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  public String toString()
  {
    return "Student[" + _name + "]";
  }
}
