package com.caucho.quercus.env.xdebug;

import java.util.Map;

import com.caucho.quercus.env.Value;

public class PropertyGetCommand extends XdebugCommand
{
  public final static String PARAM_PROPERTY_LONG_NAME = "-n";
  private Value lastReturnedValue;
  private String lastRequestedValue;

  @Override
  protected XdebugResponse getInternalResponse(String commandName,
      Map<String, String> parameters, String transactionId,
      XdebugConnection conn) {
    String data = parameters.get(PARAM_PROPERTY_LONG_NAME);
//    System.out.println("Requested eval expression: " + data);
    
    Value value = null;
    if (lastRequestedValue != null && data.startsWith(lastRequestedValue) && lastReturnedValue != null && !lastReturnedValue.isNull()) {
      String suffix = data.substring(lastRequestedValue.length());
      if (suffix.startsWith("->")) {
        String propertyName = suffix.substring(2);
        value = getField(conn, lastReturnedValue, conn.getEnv().createString(propertyName));
      } else if (suffix.startsWith("['") || suffix.startsWith("[\"")) {
        String indexName = suffix.substring(2, suffix.length() - 2);
        value = lastReturnedValue.get(conn.getEnv().createString(indexName));
      } else if (suffix.startsWith("[")) {
        long index = Long.parseLong(suffix.substring(1, suffix.length() - 1));
        value = lastReturnedValue.get(index);
      }
    }
    if (value == null || value.isNull()) {
      value = conn.eval(data);
    }
    lastReturnedValue = value;
    lastRequestedValue = data;

    StringBuilder sb = new StringBuilder();
    sb.append("<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"property_get\" transaction_id=\""
            + transactionId + "\">");
    sb.append(createPropertyElement(value, conn, null, null, null, true));
    sb.append("</response>");
  return new XdebugResponse(null, sb.toString(), transactionId);
  }

}
