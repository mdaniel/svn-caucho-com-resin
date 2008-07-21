package example.cmp.many2one;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;


/**
 * Remote interface for a house instance.
 */
public interface House extends EJBLocalObject {
  /**
   * Returns the house name.
   */
  String getName();
  /**
   * Returns the number of points for the house.
   */
  int getPoints();
  /**
   * Returns a collection of student beans.
   */
  Collection getStudentList();
  /**
   * Adds a student to the house.  If the student is already a member
   * of another house, he will be removed from that house automatically.
   */
  void addStudent(Student student);
  /**
   * Removes a student from the house.
   */
  void removeStudent(Student student);
}
