package example;

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
   * Returns the name of the instructor for this house.
   *
   * Amber will automatically cache the value so most
   * calls to getInstructor can avoid database calls.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the name of the instructor for this house.
   */
  public void setName(String name)
  {
    _name = name;
  }
}

    
