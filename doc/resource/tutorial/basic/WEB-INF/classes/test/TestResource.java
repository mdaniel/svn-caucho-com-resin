package test;

import java.util.logging.Logger;

import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.BootstrapContext;

import com.caucho.jca.AbstractResourceAdapter;

/**
 * Implements a resource which is a plain-old bean.
 *
 * The resource is configured in the resin.conf (or web.xml) using
 * bean-style configuration and saved in JNDI.
 *
 * <pre>
 * &lt;resource name="test/basic"
 *           type="test.TestResource">
 *  &lt;init>
 *    &lt;value>sample configuration&lt;/value>
 *  &lt;/init>
 * &lt;/resource>
 * </pre>
 *
 * <p>Applications will use JNDI to retrieve the resource:</p>
 *
 * <code><pre>
 * Context ic = new InitialContext();
 * TestResource resource = (TestResource) ic.lookup("java:comp/env/test/basic");
 * </pre></code>
 */
public class TestResource {
  private static final Logger log =
    Logger.getLogger(TestResource.class.getName());

  /**
   * Sample initialization param set by the <resource>
   */
  private String _value = "default";

  /**
   * Sets the value.
   */
  public void setValue(String value)
  {
    _value = value;
  }

  /**
   * init() is called at the end of the configuration.
   */
  public void init()
  {
    log.config("TestResource[" + _value + "] init");
  }

  /**
   * Returns a printable version of the resource.
   */
  public String toString()
  {
    return "TestResource[" + _value + "]";
  }
}
