package example.cmp.id;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the Quidditch bean.
 */
public interface Quidditch extends EJBLocalObject {

  /**
   * Returns the student.  This is the primary key.
   */
  Student getStudent();

  /**
   * Returns the player's position.
   */
  String getPosition();

  /**
   * Sets the player's position.
   */
  void setPosition(String position);

  /**
   * Returns the number of points the player has earned this season.
   */
  int getPoints();

  /**
   * Sets the number of points the player has earned this season.
   */
  void setPoints(int points);
}
