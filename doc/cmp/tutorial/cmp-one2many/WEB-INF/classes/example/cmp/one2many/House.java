package example.cmp.one2many;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the House bean.
 */
public interface House extends EJBLocalObject {

  /**
   * returns the name of the house (CMP field).
   */
  String getName();

  /**
   * returns a list of all students living in this house (CMR field).
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
