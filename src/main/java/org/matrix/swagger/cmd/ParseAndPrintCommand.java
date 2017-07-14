package org.matrix.swagger.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.swagger.parser.Swagger20Parser;
import io.swagger.parser.util.SwaggerDeserializationResult;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Created by tavoda on 7/11/17.
 */
@Parameters(commandDescription = "Parse Swagger definition file and print all values recursively")
public class ParseAndPrintCommand extends ExecutableCommand {
	@Parameter(description = "<Swagger definition file>", required = true)
	String inputFile;

	@Override
	public void execute() throws IOException {
		Swagger20Parser parser = new Swagger20Parser();
		SwaggerDeserializationResult result = parser.readWithInfo("file://" + inputFile, null);
		printRecursive("", result.getSwagger());
	}

	private void printRecursive(String prefix, Object swagger) {
		Method[] methods = swagger.getClass().getMethods();
		for (Method method : methods) {
			if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
				try {
					Object result = method.invoke(swagger);
					if (result == null) {
						System.out.println(prefix + method.getName() + "() = null");
					} else if (result instanceof String) {
						System.out.println(prefix + method.getName() + "() = " + ((String) result).replace("\n", " "));
					} else if (result.getClass().isPrimitive()) {
						System.out.println(prefix + method.getName() + "() = " + result);
					} else if (result instanceof Enum) {
						Enum e = (Enum) result;
						System.out.println(prefix + method.getName() + "() = " + e.name() + "[" + e.ordinal() + "]");
					} else if (result instanceof Map) {
						System.out.println(prefix + method.getName() + "(): MAP");
						Map map = (Map) result;
						for (Object o : map.entrySet()) {
							Map.Entry entry = (Map.Entry) o;
							System.out.println(prefix + "  # " + entry.getKey());
							printPrimitiveRecursive(prefix + "    -> ", entry.getValue());
						}
					} else if (result instanceof Collection) {
						System.out.println(prefix + method.getName() + "(): LIST");
						Collection collection = (Collection) result;
						int i = 0;
						for (Object o : collection) {
							printPrimitiveRecursive(prefix + "  - [" + i++ + "] = ", o);
						}
					} else if (method.getDeclaringClass().getName().startsWith("java")) {
						System.out.println(prefix + method.getName() + "() = " + result);
					} else {
						System.out.println(prefix + method.getName() + "():");
						printRecursive(prefix + "    ", result);
					}
				} catch (Throwable e) {
					System.out.println(prefix + method.getName() + "() = ERROR: " + e.getMessage());
				}
			}
		}
	}

	private void printPrimitiveRecursive(String prefix, Object value) {
		if (value == null) {
			System.out.println(prefix + "null");
		} else if (value instanceof String) {
			System.out.println(prefix + ((String) value).replace("\n", " "));
		} else if (value.getClass().isPrimitive()) {
			System.out.println(prefix + value);
		} else if (value instanceof Enum) {
			Enum e = (Enum) value;
			System.out.println(prefix + e.name() + "[" + e.ordinal() + "]");
		} else {
			printRecursive(prefix, value);
		}
	}

}
