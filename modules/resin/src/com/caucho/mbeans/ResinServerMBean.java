/**
 * Created by IntelliJ IDEA.
 * User: sam
 * Date: May 16, 2006
 * Time: 12:01:52 PM
 * To change this template use File | Settings | File Templates.
 */

package com.caucho.mbeans;

import com.caucho.jmx.MBean;
import com.caucho.jmx.MBeanAttribute;
import com.caucho.jmx.MBeanOperation;
import com.caucho.jmx.MBeanAttributeCategory;

import javax.management.ObjectName;
import java.util.Date;

/**
 * Management interface for the server.
 * There is one ResinServer global for the entire JVM.
 */
@MBean(description="A single ResinServer for each JVM provides a global environment for Resin")
public interface ResinServerMBean {
  /**
   * Returns the {@link ObjectName} of the mbean.
   */
  @MBeanAttribute(description="The JMX ObjectName for the MBean",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public ObjectName getObjectName();

  /**
   * Returns the ip address of the machine that is running this ResinServer.
   */
  @MBeanAttribute(description="The ip address of the machine that is running"
                              + " this instance of Resin",
                  category = MBeanAttributeCategory.CONFIGURATION)
  public String getLocalHost();

  /**
   * Returns the server id, the value of "-server id"
   */
  @MBeanAttribute(description="The server id used when starting this instance"
                              + " of Resin, the value of `-server'",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public String getServerId();

  /**
   * The Resin home directory used when starting this instance of Resin.
   * This is the location of the Resin program files.
   */
  @MBeanAttribute(description="The Resin home directory used when starting"
                              + " this instance of Resin. This is the location"
                              + " of the Resin program files",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public String getResinHome();

  /**
   * The server root directory used when starting this instance of Resin.
   * This is the root directory of the web server files.
   */
  @MBeanAttribute(description="The server root directory used when starting"
                              + " this instance of Resin. This is the root"
                              + " directory of the web server files",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public String getServerRoot();

  /**
   * Returns the config file, the value of "-conf foo.conf"
   */
  @MBeanAttribute(description="The configuration file used when starting this"
                              + " instance of Resin, the value of `-conf'",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public String getConfigFile();

  /**
   * Returns true if detailed statistics are being kept.
   */
  @MBeanAttribute(description="Detailed statistics causes various parts of"
                              + " Resin to keep more detailed statistics at the possible expense of some performance",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public boolean isDetailedStatistics();

  @MBeanAttribute(description="",
                  category =MBeanAttributeCategory.CONFIGURATION)
  public ObjectName getThreadPoolObjectName();

  @MBeanAttribute(description="",
                  category =MBeanAttributeCategory.STATISTIC)
  public ObjectName[] getServerObjectNames();

  /**
   * The current lifecycle state.
   */
  @MBeanAttribute(description="The current lifecycle state",
                  category =MBeanAttributeCategory.STATISTIC)
  public String getState();

  /**
   * Returns the initial start time.
   */
  @MBeanAttribute(description="The time that this instance was first started",
                  category =MBeanAttributeCategory.STATISTIC)
  public Date getInitialStartTime();

  /**
   * Returns the last start time.
   */
  @MBeanAttribute(description="The time that this instance was last started or restarted",
                  category =MBeanAttributeCategory.STATISTIC)
  public Date getStartTime();

  /**
   * Returns the current total amount of memory available for the JVM, in bytes.
   */
  @MBeanAttribute(description="The current total amount of memory available for"
                              + " the JVM, in bytes",
                  category =MBeanAttributeCategory.STATISTIC)
  public long getTotalMemory();

  /**
   * Returns the current free amount of memory available for the JVM, in bytes.
   */
  @MBeanAttribute(description="The current free amount of memory available for"
                              + " the JVM, in bytes",
                  category =MBeanAttributeCategory.STATISTIC)
  public long getFreeMemory();

  /**
   * Restart this Resin server.
   */
  @MBeanOperation(description="Exit this instance cleanly and allow the"
                              + " wrapper script to start a new JVM")
  public void restart();

}
