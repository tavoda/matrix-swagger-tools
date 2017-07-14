package org.matrix.swagger.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;

enum FileType {
	JSON, YAML
}

/**
 * Created by tavoda on 7/4/17.
 */
@Parameters(commandDescription = "Convert file to yaml")
public class ToYamlCommand extends ExecutableCommand {
	@Parameter(description = "<Input JSON or YAML file>", required = true)
	String inputFile;

	@Parameter(names = {"-o", "--output-file"}, description = "Output file, if not specified calculated automatically")
	String outputFile;

	@Override
	public void execute() throws IOException {
		parseAndWrite(inputFile, new YAMLFactory(), outputFile, ".yaml");
	}

	@Override
	public String toString() {
		return super.toString() + " :: inputFile=" + inputFile + (outputFile != null ? " :: outputFile=" + outputFile : "");
	}
}
