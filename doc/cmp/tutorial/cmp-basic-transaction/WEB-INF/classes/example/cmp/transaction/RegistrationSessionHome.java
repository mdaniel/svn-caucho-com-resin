package example.cmp.transaction;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

/**
 * Home Interface for the RegistrationSession bean.
 */
public interface RegistrationSessionHome extends EJBLocalHome {

  /**
   * Returns a new RegistrationSessionBean.
   */
  RegistrationSession create()
    throws CreateException;

}
