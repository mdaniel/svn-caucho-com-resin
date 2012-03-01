package com.caucho.encoder;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts an input JSON String into Java objects. */
public class JSONDecoder implements Decoder{
	
	String str; 
	char[] cs; 
	int index; 
	int line;
	int lastLineStart;
	char c;
	final boolean debug=false;
	
	Map<String, Object> currentMap;
	List<Object> currentList;
	
	JSONStringDecoder jsStringDecoder = new JSONStringDecoder();

	
	
	@Override
	public Object decodeObject(Object obj) throws Exception {
		str = (String) obj;
		cs = str.toCharArray();
		return decodeValue("root");
	}
	
	public void debug() {
		if (debug) {
			PrintStream out = System.out;
			out.println("Current line #" + line);
			
			out.println(currentMap);
			out.println(currentList);
			int lineCount = index-lastLineStart;
			out.println("line contents\n" + str.substring(lastLineStart,index));

			StringBuilder builder = new StringBuilder();
			for (int i=0; i<lineCount; i++) {
				if (lineCount-1==i) {
					builder.append('^');
				}else {
					builder.append('.');				
				}
			}
			System.out.println(builder.toString());
		}
	}
	
	public void skipWhiteSpace() throws Exception {
		for (; index < cs.length; index++) {
			c = cs[index];
			if(c=='\n') {
				line++;
				lastLineStart=index;
				continue;
			}
			else if(c=='\r') {
				line++;
				if (index + 1 < cs.length) {
					index++;
					if (c!='\n') {
						index--;
					}
					lastLineStart=index;
				}
				continue;
			}else if (Character.isWhitespace(c)) {
				continue;
			}
			else {
				break;
			}
		}
	}
	
	
	public Object decodeJsonObject() throws Exception {
		if (debug) System.out.println("decodeJsonObject enter");
		if (c=='{' && index < cs.length) index++;
		HashMap<String, Object> map = new HashMap<String, Object>();
		this.currentMap = map;
		for (; index < cs.length; index++) {

			skipWhiteSpace();
			if (index >= cs.length-1) break;
			c = cs[index];
			
			if (c=='"') {
				String key = decodeKeyName();
				if (debug) System.out.printf("key=%s\n", key);

				index++;
				if (index >= cs.length-1) break;
				skipWhiteSpace();
				if (c!=':') {
					debug();
					throw new IllegalStateException("Expecting to find ':', but found '"+ c +"' instead on line = " + line + "  index = " + index);
				}
				index++;
				if (index >= cs.length-1) break;
				skipWhiteSpace();				
				Object value = decodeValue(key);
				if (debug) System.out.printf("key=%s, value=%s\n", key, value);
				skipWhiteSpace();				
				map.put(key,value);
				if (!(c=='}' || c==',')){
					debug();
					throw new IllegalStateException("Unable to get key value pair c='"+ c + "' from Object line = " + line + "  index = " + index);				
				}
			} 
			if (c=='}'){
				if (index < cs.length)index++;
				break;
			} else if (c==','){
				continue;
			}
			else {
				debug();
				throw new IllegalStateException("Unable to get key value pair c='"+ c + "' from Object line = " + line + "  index = " + index);				
			}
		}
		return map;
	}

	private Object decodeValue(String key) throws Exception {
		Object value = null;
		for (; index < cs.length; index++) {
			c = cs[index];
			if (c=='"') {
				value = decodeString();
				break;
			} else if (c=='t' || c=='f') {
				value = decodeBoolean();
				break;
			} else if (c=='[') {
				value = decodeJsonArray();
				break;
			} else if (c=='{') {
				value = decodeJsonObject();
				break;
			} else if (c=='-' || Character.isDigit(c)){
				value = decodeNumber();
				break;
			}
		}
		//System.out.println(key + " = " + value);
		skipWhiteSpace();
		if (!(c==',' || c=='}' || c==']' || c=='"' || Character.isWhitespace(c))) {
			debug();
			throw new IllegalStateException("Can't parse value of object entry expecting " +
					"',' or '}' or ']' but got '" + c + "' line = " + line + "  index = " + index);
		} 
		return value;
	}


	private Object decodeNumber() {
		int startIndex = index;
		boolean doubleFloat = false;
		for (; index < cs.length; index++) {
			c = cs[index];
			if (Character.isWhitespace(c) || c==',' || c=='}' || c==']') {
				break;
			}
			if (Character.isDigit(c) || c=='.' || c=='e' || c=='E' 
				|| c == '+' || c == '-') {
				if (c=='.' || c=='e' || c=='E') {
					doubleFloat = true;
				}
				continue;
			} else {
				debug();
				throw new IllegalStateException("Can't parse number line = " + line + "  index = " + index + " bad char = " + c);
			}
		}
		String svalue = str.substring(startIndex, index);
		Object value = null;
		try {
			if (doubleFloat) {
				value = Double.parseDouble(svalue);
			} else {
				value = Integer.parseInt(svalue);
			}
		} catch (Exception ex) {
			debug();
			throw new IllegalStateException("Can't parse number bad number string line = " + line + "  index = " + index + " not a valid number = " + svalue);
			
		}
		
		return value;
		
	}

	private Object decodeBoolean() {
		int startIndex = index;
		for (; index < cs.length; index++) {
			c = cs[index];
			if (Character.isWhitespace(c) || c==',' || c=='}') {
				break;
			}
		}
		return Boolean.parseBoolean(str.substring(startIndex, index));
	}

	private Object decodeString() throws Exception {
		
		index++;
		int startIndex = index;
		for (; index < cs.length; index++) {
			c = cs[index];
			if (c=='"') {
				break;
			}
		}
		
		Object value =  encodeString(str.substring(startIndex, index));
		if (index < cs.length) {
			index++;
		}
		return value;
	}

	private Object encodeString(String string) throws Exception {
		return jsStringDecoder.decodeObject(string);
	}

	private String decodeKeyName() {
		index++;
		
		int startIndex = index;
		for (; index < cs.length; index++) {
			c = cs[index];
			if (c=='"') {
				break;
			}			
		}
		return str.substring(startIndex, index);
	}


	public Object decodeJsonArray() throws Exception {
		if (c=='[' && index < cs.length) index++;
		skipWhiteSpace();
		ArrayList<Object> list = new ArrayList<Object>();
		currentList = list;
		
		int arrayIndex = 0;

		for (; index < cs.length; index++) {
			skipWhiteSpace();
			if (index >= cs.length-1) break;
			c = cs[index];

			list.add( decodeValue(""+arrayIndex) );
			arrayIndex++;
			skipWhiteSpace();
			if ( !(c==',' || c==']')) {
				debug();
				throw new IllegalStateException("Expecting to find ',' or ']', but found '"+ c +"' instead on line = " + line + "  index = " + index);
			} 
			
			if (c==']') {
				if (index < cs.length)index++;
				break;
			}
		}
		return list;
	}
}
