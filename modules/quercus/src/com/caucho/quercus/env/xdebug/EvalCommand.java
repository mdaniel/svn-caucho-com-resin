package com.caucho.quercus.env.xdebug;

import java.util.Map;

import com.caucho.quercus.env.Value;

public class EvalCommand extends XdebugCommand
{
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		String data = getBase64DecodedData(parameters);
//		System.out.println("Requested eval expression: " + data);
		Value value = conn.eval(data);

		StringBuilder sb = new StringBuilder();
		sb.append("<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"eval\" transaction_id=\""
				+ transactionId + "\">");
		sb.append(createPropertyElement(value, conn, null, null, null, true));
		sb.append("</response>");
	  return new XdebugResponse(null, sb.toString(), transactionId);
  }
}
