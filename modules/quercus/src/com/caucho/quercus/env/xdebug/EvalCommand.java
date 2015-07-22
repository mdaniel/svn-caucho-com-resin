package com.caucho.quercus.env.xdebug;

import java.io.IOException;
import java.util.Map;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.program.QuercusProgram;

public class EvalCommand extends XdebugCommand
{
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		String data = getBase64DecodedData(parameters);
		System.out.println("Requested eval expression: " + data);
		Object value = eval(conn.getEnv(), data);

		String type = null;
		String size = null;
		String serializedValue = "";
		if (value instanceof Boolean) {
			type = "bool";
			serializedValue = ((Boolean) value) ? "1" : "0";
		} else if (value instanceof String) {
			type = "string";
			size = "" + ((String) value).length();
			serializedValue = javax.xml.bind.DatatypeConverter.printBase64Binary(((String) value).getBytes());
		} else {
			System.err.println("unknown type: " + value.getClass());
		}
		String response = "<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"eval\" transaction_id=\""
				+ transactionId + "\"><property address=\"140734717470128\" type=\"" + type + "\"" + (size == null ? "" : " size=\"" + size + "\" encoding=\"base64\"") + ">"
				+ "<![CDATA[" + serializedValue + "]]></property></response>";
		
	  return new XdebugResponse(null, response, transactionId);
  }

	private Object eval(Env env, String expr) {
    QuercusContext quercus = env.getQuercus();

    QuercusProgram program;
    try {
	    program = quercus.parseCode((StringValue) StringValue.create(expr));
	    Value value = program.createExprReturn().execute(env);

	    return value != null ? value.toJavaObject() : null;
    } catch (IOException e) {
	    e.printStackTrace(System.err);
	    return null;
    }
	}
}
