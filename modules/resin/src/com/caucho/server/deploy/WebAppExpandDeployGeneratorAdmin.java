package com.caucho.server.deploy;

import com.caucho.server.webapp.WebAppExpandDeployGenerator;

public class WebAppExpandDeployGeneratorAdmin
  extends ExpandDeployGeneratorAdmin<WebAppExpandDeployGenerator>
{
  public WebAppExpandDeployGeneratorAdmin(WebAppExpandDeployGenerator webAppDeployGenerator)
  {
    super(webAppDeployGenerator);
  }
}
