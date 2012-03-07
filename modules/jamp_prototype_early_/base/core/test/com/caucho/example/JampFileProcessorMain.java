package com.caucho.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.caucho.amp.AmpFactory;
import com.caucho.amp.SkeletonServiceInvoker;
import com.caucho.test.EmployeeService;

public class JampFileProcessorMain {
	
    static SkeletonServiceInvoker serviceInvoker = AmpFactory.factory().createJampServerSkeleton(EmployeeService.class);


	private static String readPayload(File file) throws Exception {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = null;
		
		try {
		reader = new BufferedReader(new FileReader(file));
		String line = null;
		
		while ((line=reader.readLine())!=null) {
			builder.append(line);
		}
		}finally{
			if (reader!=null)reader.close();
		}
		
		return builder.toString();
	}
	public static void main (String [] args) throws Exception {
		
		File dir = new File("/Users/rick/test/file_invoker");
		
		File[] listFiles = dir.listFiles();
		for (File file : listFiles) {
			System.out.println(file);
			if (!file.toString().endsWith(".jamp")){
				continue;
			}
			String payload = readPayload(file);
		    serviceInvoker.invokeMessage(payload);

		}
	}
	
	
}
