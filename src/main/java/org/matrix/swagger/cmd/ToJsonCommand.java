package org.matrix.swagger.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.JsonFactory;

import java.io.IOException;

/**
 * Created by tavoda on 7/4/17.
 */
@Parameters(commandDescription = "Convert file to JSON")
public class ToJsonCommand extends ExecutableCommand {
	@Parameter(description = "<Input JSON or YAML file>", required = true)
	String inputFile;

	@Parameter(names = {"-o", "--output-file"}, description = "Output file, if not specified calculated automatically")
	String outputFile;

	@Override
	public void execute() throws IOException {
		parseAndWrite(inputFile, new JsonFactory(), outputFile, ".yaml");
	}

	@Override
	public String toString() {
		return super.toString() + " :: inputFile=" + inputFile + (outputFile != null ? " :: outputFile=" + outputFile : "");
	}
}
