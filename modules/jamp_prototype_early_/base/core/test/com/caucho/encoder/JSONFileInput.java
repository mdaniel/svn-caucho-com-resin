package com.caucho.encoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class JSONFileInput {

	@Test
	public void fileInputTest() throws Exception {
		File projectDir = new File(".");
		projectDir = projectDir.getCanonicalFile();
		File testDir = new File(projectDir, "test/text");

		System.out.println(testDir);

		File[] listFiles = testDir.listFiles();
		for (File file : listFiles) {
			if (file.toString().endsWith(".json")) {
				System.out.printf("next up %s \n", file.getName());


				BufferedReader reader = null;

				try {
					reader = new BufferedReader(new FileReader(file));

					Decoder decoder = new JSONDecoder();
					Object object = decoder.decodeObject(reader);
					verifyObject(object);
					System.out.printf("verified %s \n\n", file.getName());
				} finally {
					if (reader != null) {
						reader.close();
					}
				}

			}
		}

	}

	private void verifyObject(Object object) {
		if (object instanceof List) {
			verifyList((List) object);
		} else if (object instanceof Map) {
			verifyMap((Map) object);
		}

	}

	private void verifyList(List<Object> list) {
		for (Object object : list) {
			verifyObject(object);
		}

	}

	private void verifyMap(Map<String, Object> map) {
		Set<Entry<String, Object>> entrySet = map.entrySet();
		for (Entry<String, Object> entry : entrySet) {
			if (entry.getKey().startsWith("object_")) {
				verifyObject(entry.getValue());
			} else if (entry.getKey().startsWith("array_")) {
				verifyList((List<Object>) entry.getValue());
			} else {

				// System.out.println("KEY " + entry.getKey());
				String[] strings = entry.getKey().split("_");

				if (strings.length != 3)
					continue;

				String type = strings[0];
				String name = strings[1];
				String value = strings[2];
				// System.out.printf("type='%s',name='%s',value='%s'\n", type,
				// name, value);
				if (type.equals("string")) {
					String svalue = (String) entry.getValue();
					// System.err.printf("svalue = %s, value=%s \n", svalue,
					// value);
					assertTrue(svalue.toLowerCase().startsWith(
							value.toLowerCase()));
				} else if (type.equals("double")) {
					Double dvalue = (Double) entry.getValue();
					double d = dvalue.doubleValue();
					double d1 = Double.parseDouble(value);
					assertTrue(d1 == d);
				} else if (type.equals("integer")) {
					Integer ivalue = (Integer) entry.getValue();
					double i = ivalue.intValue();
					double i1 = Double.parseDouble(value);
					assertTrue(i == i1);
				} else if (type.equals("boolean")) {
					Boolean bvalue = (Boolean) entry.getValue();
					boolean b = bvalue.booleanValue();
					boolean b1 = Boolean.parseBoolean(value);
					assertTrue(b == b1);
				}
			}

		}
	}

}
