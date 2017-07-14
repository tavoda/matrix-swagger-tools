package org.matrix.swagger.cmd;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Map;

/**
 * Created by tavoda on 7/4/17.
 */
public abstract class ExecutableCommand {
	@Parameter(names = {"-v", "-d", "--verbose", "--debug"}, description = "Debug/verbose mode")
	public boolean debug = false;

	@Parameter(names = {"-h", "--help"}, description = "Print help", help = true)
	public boolean help = false;

	public abstract void execute() throws IOException;

	protected void parseAndWrite(String inputFile, JsonFactory outputFactory, String outputFile, String postfix) throws IOException {
		File realFile = new File(inputFile);
		if (!realFile.exists()) {
			throw new InvalidParameterException("File " + realFile.getCanonicalPath() + " doesn't exist");
		} else if (!realFile.canRead()) {
			throw new InvalidParameterException("File " + realFile.getCanonicalPath() + " is NOT readable");
		} else {
			JsonFactory jsonFactory = inputFile.toLowerCase().endsWith(".json") ? new JsonFactory() : new YAMLFactory();
			Map<String, Object> result = new ObjectMapper(jsonFactory).readValue(realFile, new TypeReference<Map<String, Object>>() {});

			if (outputFile == null) {
				int dotIndex = realFile.getName().lastIndexOf('.');
				outputFile = (dotIndex == -1 ? inputFile : realFile.getParent() + "/" + realFile.getName().substring(0, dotIndex)) + postfix;
			}
			new ObjectMapper(outputFactory).writerWithDefaultPrettyPrinter().writeValue(new File(outputFile), result);
		}
	}

	public String toString() {
		return this.getClass().getSimpleName() + " :: debug=" + debug + " :: help=" + help;
	}
}
