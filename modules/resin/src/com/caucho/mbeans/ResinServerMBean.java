/**
 * Created by IntelliJ IDEA.
 * User: sam
 * Date: May 16, 2006
 * Time: 12:01:52 PM
 * To change this template use File | Settings | File Templates.
 */

package com.caucho.mbeans;

import javax.management.ObjectName;
import java.util.Date;

/**
 * Management interface for the server.
 * There is one ResinServer global for the entire JVM.
 */
public interface ResinServerMBean {
  /**
   * Returns the {@link ObjectName} of the mbean.
   */
  public ObjectName getObjectName();

  /**
   * Returns the ip address of the machine that is running this ResinServer.
   */
  public String getLocalHost();

  /**
   * Returns the server id, the value of "-server id"
   */
  public String getServerId();

  /**
   * The Resin home directory used when starting this instance of Resin.
   * This is the location of the Resin program files.
   */
  public String getResinHome();

  /**
   * The server root directory used when starting this instance of Resin.
   * This is the root directory of the web server files.
   */
  public String getServerRoot();

  /**
   * Returns the config file, the value of "-conf foo.conf"
   */
  public String getConfigFile();

  /**
   * The current lifecycle state.
   */
  public String getState();

  /**
   * Returns the initial start time.
   */
  public Date getInitialStartTime();

  /**
   * Returns the last start time.
   */
  public Date getStartTime();

  /**
   * Returns the current total amount of memory available for the JVM, in bytes.
   */
  public long getTotalMemory();

  /**
   * Returns the current free amount of memory available for the JVM, in bytes.
   */
  public long getFreeMemory();

  /**
   * Restart this Resin server.
   */
  public void restart();


  public ObjectName[] getServerObjectNames();


  public ObjectName getThreadPoolObjectName();
}
