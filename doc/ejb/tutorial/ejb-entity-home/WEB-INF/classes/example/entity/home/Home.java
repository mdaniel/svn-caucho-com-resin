package example.entity.home;

import java.rmi.*;
import javax.ejb.*;

/**
 * Remote interface for the hello home.
 */
public interface Home extends EJBHome {
  /**
   * Dummy find by primary key.
   */
  HomeObj findByPrimaryKey(String name) throws FinderException, RemoteException;
  /**
   * Returns hello, world.
   */
  String hello() throws RemoteException;
  
  /**
   * Adds two numbers.
   */
  int add(int a, int b) throws RemoteException;
}
