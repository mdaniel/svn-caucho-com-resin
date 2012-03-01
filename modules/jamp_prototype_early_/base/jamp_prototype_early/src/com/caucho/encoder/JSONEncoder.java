package com.caucho.encoder;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Converts a Java object into a JSON string. */
public class JSONEncoder implements Encoder {
	private String encodeString(String str) {
		StringBuilder builder = new StringBuilder();
		builder.append("\"");
		char[] charArray = str.toCharArray();
		for (int index=0; index < charArray.length; index++) {
			char c = charArray[index];
			switch (c) {
			case '\n':
				builder.append('\\').append('n');
			    break;
			case '\"':
				builder.append('\\').append('\"');
			    break;

			default:
				builder.append(c);
			}
		}
		builder.append("\"");
		return builder.toString();
	}
	
	public Object encodeObject(Object obj) throws Exception {
		if (obj==null) {
			return "null";
		}
		if (obj instanceof Number || obj instanceof Boolean) {
			return obj.toString();
		} else if (obj instanceof String) {
			return encodeString((String)obj);
		} else if (obj instanceof Collection) {
			Collection<?> collection = (Collection<?>) obj;
			Object[] array = collection.toArray(new Object[collection.size()]);

			return encodeArray(array);
			
		} else if (obj instanceof Array){
			return encodeArray((Object[]) obj);			
		}
		
		else {
			StringBuilder builder = new StringBuilder();
			builder.append("{"); 
			builder.append("\"java_type\":\"");
			builder.append(obj.getClass().getName());
			builder.append('"');

			Method [] methods = obj.getClass().getMethods();
			
			
			List<Method> methodList = new ArrayList<Method>(methods.length);
			
			for (int index = 0; index<methods.length; index++){
				Method method = methods[index];
				String name = method.getName();
				
				if (method.getParameterTypes().length>0 ||
						method.getReturnType() == Void.class ||
						!(name.startsWith("get") || name.startsWith("is")) ||
						name.equals("getClass")	
				){
					continue;
				}
				methodList.add(method);

			}
			
			if (methodList.size()>0) {
				builder.append(',');
			}

			for (int index=0; index < methodList.size(); index++) {
				Method method = methodList.get(index);
				String name = method.getName();
				if(name.charAt(0)=='g') {
					name = name.substring(3);
				} else {
					name = name.substring(2);					
				}
				name = "" + Character.toLowerCase(name.charAt(0)) + name.substring(1);
				builder.append('\"'); 
				builder.append(name);
				builder.append("\":");
				Object object = method.invoke(obj);
				builder.append(encodeObject(object));
			
				if (index+1!=methodList.size()) {
					builder.append(',');
				}
			}

			builder.append("}");
			return builder.toString();
		}
	}

	private Object encodeArray(Object[] array) throws Exception {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (int index = 0; index < array.length; index++) {
			builder.append(encodeObject(array[index]));
			if (index != array.length-1) {
				builder.append(',');
			}
		}
		builder.append("]");
		return builder.toString();
	}

}
