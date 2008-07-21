package example.cmp.one2many;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the House bean.
 */
public interface HouseHome extends EJBLocalHome {

  /**
   * returns the <code>House</code> that has <code>name</code>
   * as its primary key.
   *
   * @param name name of the <code>House</code> we want to find.
   */
  House findByPrimaryKey(String name) throws FinderException;

  /**
   * returns a <code>Collection</code> of all Houses managed by the container.
   */
  Collection findAll() throws FinderException;
}
