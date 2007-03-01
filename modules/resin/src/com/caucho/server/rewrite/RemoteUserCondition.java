package com.caucho.server.rewrite;

import com.caucho.util.L10N;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
* A rewrite condition that passes if the client has been authenticated
 * and the remote user has the specified name, as determined by
 * {@link javax.servlet.http.HttpServletRequest#getRemoteUser()}.
*/
public class RemoteUserCondition
  extends AbstractCondition
{
  private static final L10N L = new L10N(RemoteUserCondition.class);

  private final String _remoteUser;
  private boolean _sendVary = true;

  public RemoteUserCondition(String remoteUser)
  {
    _remoteUser = remoteUser;
  }

  public String getTagName()
  {
    return "remote-user";
  }

  /**
   * If true, send a "Vary: Authorization" in the response, default is true.
   */
  public void setSendVary(boolean sendVary)
  {
    _sendVary = sendVary;
  }

  public boolean isMatch(HttpServletRequest request,
                         HttpServletResponse response)
  {
    if (_sendVary)
      addVary(response, "Authorization");

    String remoteUser = request.getRemoteUser();

    return remoteUser != null && remoteUser.equals(_remoteUser);
  }
}
