package example;

/**
 * Implementation class for the Student bean.
 *
 * This bean use the following schema:
 *
 * <code><pre>
 * CREATE TABLE amber_many2one_students (
 *   id INTEGER NOT NULL,
 *
 *   name VARCHAR(250),
 *
 *   house INTEGER,
 *
 *   PRIMARY KEY(id)
 * );
 * </pre></code>
 */
public class Student {
  private int _id;

  private String _name;
  
  private House _house;
  
  /**
   * Returns the id of the student, the primary key.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Sets the primary key of the student, only called from a create method.
   *
   * @param id the studentId of the new student
   */
  public void setId(int id)
  {
    _id = id;
  }

  /**
   * Returns the name of the instructor for this student.
   *
   * Amber will automatically cache the value so most
   * calls to getInstructor can avoid database calls.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the name of the instructor for this student.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Returns the student's house.
   *
   * Amber will cache the value so most calls to getHouse can
   * avoid database calls.
   */
  public House getHouse()
  {
    return _house;
  }

  /**
   * Sets the student's house.
   */
  public void setHouse(House house)
  {
    _house = house;
  }
}

    
