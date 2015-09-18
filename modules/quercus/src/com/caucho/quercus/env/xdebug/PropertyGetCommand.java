package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class PropertyGetCommand extends XdebugCommand
{
  public final static String PARAM_PROPERTY_LONG_NAME = "-n";

  @Override
  protected XdebugResponse getInternalResponse(String commandName,
      Map<String, String> parameters, String transactionId,
      XdebugConnection conn) {
    String data = parameters.get(PARAM_PROPERTY_LONG_NAME);
    System.out.println("Requested eval expression: " + data);
    Object value = conn.eval(data);

    StringBuilder sb = new StringBuilder();
    sb.append("<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"property_get\" transaction_id=\""
            + transactionId + "\">");
    sb.append(createPropertyElement(value, conn, null, null, null, true));
    sb.append("</response>");
  return new XdebugResponse(null, sb.toString(), transactionId);
  }

}
