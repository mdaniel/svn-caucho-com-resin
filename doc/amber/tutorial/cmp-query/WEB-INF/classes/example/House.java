package example;

import javax.ejb.Entity;
import javax.ejb.Table;
import javax.ejb.Id;
import javax.ejb.Column;
import javax.ejb.Basic;
import javax.ejb.OneToMany;
import static javax.ejb.GeneratorType.AUTO;

import java.util.Collection;

/**
 * Implementation class for the House bean.
 *
 * <code><pre>
 * CREATE TABLE ejb3_query_house (
 *   id BIGINT PRIMARY KEY auto_increment,
 *   name VARCHAR(250) UNIQUE NOT NULL
 * );
 * </pre></code>
 */
@Entity
@Table(name="ejb3_query_house")
public class House {
  private long _id;
  private String _name;

  private Collection _studentList;

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
  @Id(generate=AUTO)
  @Column(name="id")
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
  @Basic
  @Column(unique=true)
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
   * returns a <code>Collection</code> of all Students living in this House.
   */
  @OneToMany(targetEntity="Student")
  public Collection getStudentList()
  {
    return _studentList;
  }
}
