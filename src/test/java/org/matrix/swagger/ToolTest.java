package org.matrix.swagger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test for Tool.
 */
public class ToolTest {
	@Test
	public void testCmdParsing() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024 * 100); // 100kiB
		ByteArrayOutputStream errorStream = new ByteArrayOutputStream(1024 * 100); // 100kiB
		System.setOut(new PrintStream(outputStream));
		System.setErr(new PrintStream(errorStream));
		String output;

		Tool.main("-t", "parseJson");
		output = outputStream.toString();
		Assert.assertNotEquals(0, errorStream.size());
		Assert.assertTrue(output.startsWith("Usage:"));

		outputStream.reset();
		errorStream.reset();
		Tool.main("-t", "toYaml", "-v", "in.file");
		output = outputStream.toString();
		Assert.assertEquals(0, errorStream.size());
		Assert.assertNotEquals(-1, output.indexOf("inputFile=in.file"));
		Assert.assertNotEquals(-1, output.indexOf("debug=true"));
		Assert.assertEquals(-1, output.indexOf("outputFile"));

		outputStream.reset();
		errorStream.reset();
		Tool.main("-t", "toJson", "-o", "out.json", "in.file");
		output = outputStream.toString();
		Assert.assertEquals(0, errorStream.size());
		Assert.assertNotEquals(-1, output.indexOf("inputFile=in.file"));
		Assert.assertNotEquals(-1, output.indexOf("debug=false"));
		Assert.assertNotEquals(-1, output.indexOf("outputFile=out.json"));

		outputStream.reset();
		errorStream.reset();
		Tool.main("-t", "toJson", "--outputFile", "out.JSON");
		output = outputStream.toString();
		Assert.assertNotEquals(0, errorStream.size());
		Assert.assertTrue(output.startsWith("Usage:"));

		// Abbreviations and case sensitivity
		outputStream.reset();
		errorStream.reset();
		Tool.main("-t", "toJson", "--vE", "--OUt", "out.JSON", "kuku.in");
		output = outputStream.toString();
		Assert.assertNotEquals(-1, output.indexOf("inputFile=kuku.in"));
		Assert.assertNotEquals(-1, output.indexOf("debug=true"));
		Assert.assertNotEquals(-1, output.indexOf("outputFile=out.JSON"));
	}

	private String FILE_YAML =
			"invoice: 34843\n" +
			"date   : !!timestamp 2001-01-23\n" +
			"bill-to: &id001\n" +
			"    given  : Chris\n" +
			"    family : Dumars\n" +
			"    address:\n" +
			"        lines: |\n" +
			"            458 Walkman Dr.\n" +
			"            Suite #292\n" +
			"        city    : Royal Oak\n" +
			"        state   : MI\n" +
			"        postal  : 48046\n" +
			"ship-to: *id001\n" +
			"product:\n" +
			"    - sku         : BL394D\n" +
			"      quantity    : 4\n" +
			"      description : Basketball\n" +
			"      price       : 450.00\n" +
			"    - sku         : BL4438H\n" +
			"      quantity    : 1\n" +
			"      description : Super Hoop\n" +
			"      price       : 2392.00\n" +
			"tax  : 251.42\n" +
			"total: 4443.52\n" +
			"comments: >\n" +
			"    Late afternoon is best.\n" +
			"    Backup contact is Nancy\n" +
			"    Billsmer @ 338-4338.";

	private String FILE_JSON = "{\n" +
			"    \"glossary\": {\n" +
			"        \"title\": \"example glossary\",\n" +
			"\t\t\"GlossDiv\": {\n" +
			"            \"title\": \"S\",\n" +
			"\t\t\t\"GlossList\": {\n" +
			"                \"GlossEntry\": {\n" +
			"                    \"ID\": 13.456,\n" +
			"\t\t\t\t\t\"SortAs\": \"SGML\",\n" +
			"\t\t\t\t\t\"GlossTerm\": \"Standard Generalized Markup Language\",\n" +
			"\t\t\t\t\t\"Acronym\": [\"SGML\", \"Kuku\"],\n" +
			"\t\t\t\t\t\"Abbrev\": 8879,\n" +
			"\t\t\t\t\t\"GlossDef\": {\n" +
			"                        \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n" +
			"\t\t\t\t\t\t\"GlossSeeAlso\": [\"GML\", \"XML\"]\n" +
			"                    },\n" +
			"\t\t\t\t\t\"GlossSee\": \"markup\"\n" +
			"                }\n" +
			"            }\n" +
			"        }\n" +
			"    }\n" +
			"}";

	@Test
	public void testConversation() throws IOException {
		String tmpDir = System.getProperty("java.io.tmpdir");

		{
			String inputFile1 = tmpDir + "/input1.yaml";
			String outputFileYaml1 = tmpDir + "/output1.yaml";
			String outputFileJson1 = tmpDir + "/output1.json";
			new File(inputFile1).delete();
			new File(outputFileJson1).delete();
			new File(outputFileYaml1).delete();
			try (FileOutputStream out = new FileOutputStream(inputFile1)) {
				out.write(FILE_YAML.getBytes());
			} catch (Throwable e) {
				e.printStackTrace();
			}

			Tool.main("toYaml", "-o", outputFileYaml1, inputFile1);
			Tool.main("toJson", "-o", outputFileJson1, inputFile1);
			testOutput1(new YAMLFactory(), outputFileYaml1);
			testOutput1(new JsonFactory(), outputFileJson1);
		}

		{
			String inputFile2 = tmpDir + "/input2.json";
			String outputFileYaml2 = tmpDir + "/output2.yaml";
			String outputFileJson2 = tmpDir + "/output2.json";
			new File(inputFile2).delete();
			new File(outputFileJson2).delete();
			new File(outputFileYaml2).delete();
			try (FileOutputStream out = new FileOutputStream(inputFile2)) {
				out.write(FILE_JSON.getBytes());
			} catch (Throwable e) {
				e.printStackTrace();
			}

			Tool.main("toYaml", "-o", outputFileYaml2, inputFile2);
			Tool.main("toJson", "-o", outputFileJson2, inputFile2);
			testOutput2(new YAMLFactory(), outputFileYaml2);
			testOutput2(new JsonFactory(), outputFileJson2);
		}
	}

	private void testOutput1(JsonFactory factory, String file) throws IOException {
		Map<String, Object> result = new ObjectMapper(factory).readValue(new File(file), new TypeReference<Map<String, Object>>() {});
		Assert.assertTrue(result.containsKey("invoice"));
		Assert.assertTrue(result.containsKey("date"));
		Assert.assertTrue(result.containsKey("ship-to"));
		Assert.assertTrue(result.get("bill-to") instanceof Map);
		Assert.assertTrue(result.get("tax") instanceof Number);
		Assert.assertTrue(result.get("total") instanceof Number);
	}

	private void testOutput2(JsonFactory factory, String file) throws IOException {
		Map<String, Object> result = new ObjectMapper(factory).readValue(new File(file), new TypeReference<Map<String, Object>>() {});
		Assert.assertTrue(result.containsKey("glossary"));
		Map entry = (Map) ((Map) ((Map) ((Map) result.get("glossary")).get("GlossDiv")).get("GlossList")).get("GlossEntry");
		Assert.assertEquals("SGML", entry.get("SortAs"));
		Assert.assertTrue(entry.get("ID") instanceof Number);
		Assert.assertTrue(entry.get("Abbrev") instanceof Number);
		Assert.assertTrue(entry.get("Acronym") instanceof List);
		Assert.assertEquals(2, ((List) entry.get("Acronym")).size());
	}

}
