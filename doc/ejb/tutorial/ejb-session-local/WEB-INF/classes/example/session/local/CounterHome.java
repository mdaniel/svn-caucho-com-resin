package example.session.local;

import java.rmi.*;
import javax.ejb.*;

public interface CounterHome extends EJBHome {
  Counter create() throws CreateException, RemoteException;
}
