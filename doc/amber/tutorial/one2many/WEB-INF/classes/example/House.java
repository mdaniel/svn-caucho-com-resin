package example;

import java.util.Collection;

/**
 * Implementation class for the House bean.
 *
 * This bean use the following schema:
 *
 * <code><pre>
 * CREATE TABLE amber_many2one_houses (
 *   id INTEGER NOT NULL,
 *
 *   name VARCHAR(250),
 *
 *   PRIMARY KEY(id)
 * );
 * </pre></code>
 */
public class House {
  private int _id;

  private String _name;
  private Collection _students;
  
  /**
   * Returns the id of the house, the primary key.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Sets the primary key of the house, only called from a create method.
   *
   * @param id the houseId of the new house
   */
  public void setId(int id)
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
   * Returns house students
   */
  public Collection getStudents()
  {
    return _students;
  }

  /**
   * Sets the students of the house.
   */
  public void setStudents(Collection students)
  {
    _students = students;
  }
}

    
