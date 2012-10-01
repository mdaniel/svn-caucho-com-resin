package com.caucho.encoder;

public class JSONStringDecoder implements Decoder<String, String> {

	@Override
	public String decodeObject(String string) throws Exception {
		
		char[] cs = string.toCharArray();
		StringBuilder builder = new StringBuilder(cs.length);
		for (int index=0; index < cs.length; index++) {
			char c = cs[index];
			if (c=='\\'){
				if (index < cs.length) {
					index++;
					c = cs[index];
					if (c=='n'){
						builder.append("\n");
					}else if (c=='/') {
						builder.append("/");	
					}else if (c=='"') {
						builder.append("\"");	
					}else if (c=='f') {
						builder.append("\f");	
					}else if (c=='t') {
						builder.append("\t");	
					}else if (c=='\\') {
						builder.append("\\");	
					}else if (c=='b') {
						builder.append("\b");	
					}else if (c=='r') {
						builder.append("\r");	
					}else if (c=='u') {
						if (index+4 < cs.length) {
							String hex = string.substring(index+1, index+5);
							char unicode = (char) Integer.parseInt( hex, 16 );
							builder.append(unicode);
							index+=4;
						}
					}
				}
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
		
	}

}
