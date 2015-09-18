package com.caucho.quercus.env.xdebug;

import java.util.Map;
import java.util.Map.Entry;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvVar;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

public class ContextGetCommand extends XdebugCommand
{
  public final static String PARAM_STACK_DEPTH = "-d";
  public final static String PARAM_CONTEXT_ID = "-c";

  @Override
  protected XdebugResponse getInternalResponse(String commandName,
      Map<String, String> parameters, String transactionId,
      XdebugConnection conn) {
    Env env = conn.getEnv();
    int stackDepth = Integer.parseInt(parameters.get(PARAM_STACK_DEPTH));
    String contextId = parameters.get(PARAM_CONTEXT_ID);
    StringBuilder response = new StringBuilder(
        "<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"context_get\" transaction_id=\"");
    response.append(transactionId);
    response.append("\" context=\"").append(contextId).append("\">");

    if (CONTEXT_ID_LOCALS.equals(contextId)) {
      Map<StringValue, EnvVar> varEnv = conn.getVarEnvAtStackDepth(stackDepth);
      for (Entry<StringValue, EnvVar> entry : varEnv.entrySet()) {
        String name = "$" + entry.getKey().toString();
        response.append(createPropertyElement(entry.getValue().get(), conn, name, name, null, true));
      }
      Value thisValue = env.peekCallThis(stackDepth);
      if (thisValue != null) {
        if (thisValue instanceof QuercusClass) {
          response.append(createPropertyElement(((QuercusClass)thisValue).getClassName(), conn, "::", "::", null, true));
        } else {
          response.append(createPropertyElement(thisValue, conn, "$this", "$this", null, true));
        }
      }
    } else if (CONTEXT_ID_GLOBALS.equals(contextId)) {
      ArrayValue arrayValue = new ArrayValueImpl();
      for (Entry<StringValue, EnvVar> entry : env.getGlobalEnv().entrySet()) {
        String name = entry.getKey().toString();
        Value value = entry.getValue().get();
        if (name.startsWith("_")) {
          // add variables like $_GET parallel to $GLOBALS
          name = "$" + name;
          response.append(createPropertyElement(value, conn, name, name, null, true));
        }
        arrayValue.put(entry.getKey(), value);
      }
      response.append(createPropertyElement(arrayValue, conn, "$GLOBALS", "$GLOBALS", null, true));
    }
    response.append("</response>");
    return new XdebugResponse(null, response.toString(), transactionId);
  }
}
