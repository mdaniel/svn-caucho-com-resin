package com.caucho.amp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;

public class FileMessageSender implements AmpMessageSender {
	private File dir;
	private int counter;
	
	public FileMessageSender (final String aDir) {
		dir = new File(aDir);
		if (!dir.isDirectory()) {
			if (!dir.mkdirs()){
				throw new IllegalStateException("not able to create directory and it is not a directory " + dir);
			}
		}
		
		if (!dir.canWrite()) {
			throw new IllegalStateException("unable to write to dir " + dir);
		}
	}
	
	@Override
	public void sendMessage(String name, Object payload, String toInvoker, String fromInvoker) throws Exception  {
		counter++;
		File outputFile = new File(dir, name + "_" + System.currentTimeMillis() + "_" + counter + ".jamp");
		
		if (payload instanceof String) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(outputFile);
				writer.write((String)payload);
			} finally {
				if (writer!=null) writer.close();
			}
		} else if (payload instanceof byte[]) {
			FileOutputStream writer = null;
			try {
				writer = new FileOutputStream(outputFile);
				writer.write((byte[])payload);
			} finally {
				if (writer!=null) writer.close();
			}			
		} else {
			FileOutputStream writer = null;
			try {
				writer = new FileOutputStream(outputFile);
				ObjectOutputStream out = new ObjectOutputStream(writer);
				out.writeObject(payload);
			} finally {
				if (writer!=null) writer.close();
			}			
		}
	}

}
