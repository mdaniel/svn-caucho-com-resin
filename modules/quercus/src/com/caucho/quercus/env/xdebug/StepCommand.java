package com.caucho.quercus.env.xdebug;

import java.util.Map;

import com.caucho.quercus.env.xdebug.XdebugConnection.State;

public class StepCommand extends XdebugCommand
{

  @Override
  protected XdebugResponse getInternalResponse(String commandName,
      Map<String, String> parameters, String transactionId,
      XdebugConnection conn) {
    if ("step_over".equals(commandName)) {
      conn.stepOver();
    } else if ("step_into".equals(commandName)) {
      conn.stepInto();
    } else if ("step_out".equals(commandName)) {
      conn.stepOut();
    }
    return new XdebugResponse(State.BREAK, null, transactionId);
  }

}
