package example.cmp.transaction;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBHome;
import javax.ejb.FinderException;

import java.util.Collection;

/**
 * Home interface for the Course bean.
 * The Home Interface enables you to create new entities and to obtain
 * references to existing ones.
 *
 * <p>The idea is that you use the Home Interface to obtain references
 * to whatever entities you're interested in. Each entity that you
 * get from the Home Interface (using its create or finder methods)
 * is then accessible through its Local Interface.
 *
 */
public interface CourseHome extends EJBLocalHome {
  /**
   * Returns the Course identified by the given primary key.
   */
  Course findByPrimaryKey(int primaryKey)
    throws FinderException;

  /**
   * Returns a <code>Collection</code> of all Courses taught.
   */
  Collection findAll()
    throws FinderException;

}
