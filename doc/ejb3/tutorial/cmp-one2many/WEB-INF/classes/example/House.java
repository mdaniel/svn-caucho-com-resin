package example;

import javax.ejb.Entity;
import javax.ejb.Table;
import javax.ejb.Id;
import javax.ejb.Column;
import javax.ejb.Basic;
import javax.ejb.GeneratorType;
import javax.ejb.OneToMany;
import javax.ejb.JoinColumn;
import javax.ejb.AccessType;

import java.util.Set;

/**
 * Implementation class for the House bean.
 *
 * <code><pre>
 * CREATE TABLE ejb3_one2many_house (
 *   house BIGINT PRIMARY KEY auto_increment,
 *   name VARCHAR(250) UNIQUE NOT NULL
 * );
 * </pre></code>
 */
@Entity(access=AccessType.FIELD)
@Table(name="ejb3_one2many_house")
public class House {
  @Id(generate=GeneratorType.AUTO)
  @Column(name="house_id")
  private long _id;
  
  @Basic
  @Column(name="name",unique=true)
  private String _name;
  
  @OneToMany
  @JoinColumn(name="house")
  private Set<Student> _students;

  public House()
  {
  }

  public House(String name)
  {
    _name = name;
  }
  
  /**
   * Returns the id of the house.
   */
  public long getId()
  {
    return _id;
  }
  
  /**
   * Sets the id of the house.
   */
  public void setId(long id)
  {
    _id = id;
  }
  
  /**
   * Returns the name of the house.
   */
  public String getName()
  {
    return _name;
  }
  
  /**
   * Sets the name of the house.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Get the Student's that belong to this house.
   */
  public Set<Student> getStudents()
  {
    return _students;
  }
}
