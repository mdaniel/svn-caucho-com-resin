package example;

/**
 * Implementation class for the Course bean.
 *
 * This bean use the following schema:
 *
 * <code><pre>
 * CREATE TABLE amber_find_courses (
 *   id INTEGER NOT NULL,
 *
 *   name VARCHAR(250),
 *   teacher VARCHAR(250),
 *
 *   PRIMARY KEY(id)
 * );
 * </pre></code>
 */
public class Course {
  private int _id;

  private String _name;
  private String _teacher;
  
  /**
   * Returns the id of the course, the primary key.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Sets the primary key of the course, only called from a create method.
   *
   * @param id the courseId of the new course
   */
  public void setId(int id)
  {
    _id = id;
  }

  /**
   * Returns the name of the instructor for this course.
   *
   * Amber will automatically cache the value so most
   * calls to getInstructor can avoid database calls.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the name of the instructor for this course.
   *
   * The value will be written to the database when the transaction completes.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Returns the name of the instructor for this course. 
   *
   * Resin-CMP will automatically cache the value so most
   * calls to getTeacher can avoid database calls.
   */
  public String getTeacher()
  {
    return _teacher;
  }

  /**
   * Sets the name of the instructor for this course.
   *
   * The value will be written to the database when the transaction completes.
   */
  public void setTeacher(String teacher)
  {
    _teacher = teacher;
  }
}

    
