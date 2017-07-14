package org.matrix.swagger;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.matrix.swagger.cmd.ExecutableCommand;
import org.matrix.swagger.cmd.MainCommand;
import org.matrix.swagger.cmd.ParseAndPrintCommand;
import org.matrix.swagger.cmd.SwaggerMergeCommand;
import org.matrix.swagger.cmd.SwaggerSmartMergeCommand;
import org.matrix.swagger.cmd.ToJsonCommand;
import org.matrix.swagger.cmd.ToYamlCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool for converting and building swagger OUTPUT
 */
public class Tool {
	private static final Logger log = LoggerFactory.getLogger(Tool.class);

	public static void main(String... args) {
		MainCommand mainCommand = new MainCommand();
		ToYamlCommand toYamlCommand = new ToYamlCommand();
		ToJsonCommand toJsonCommand = new ToJsonCommand();
		SwaggerMergeCommand swaggerMergeCommand = new SwaggerMergeCommand();
		SwaggerSmartMergeCommand swaggerSmartMergeCommand = new SwaggerSmartMergeCommand();
		ParseAndPrintCommand parseAndPrintCommand = new ParseAndPrintCommand();
		JCommander jc = JCommander.newBuilder()
				.addObject(mainCommand)
				.addCommand("toYaml", toYamlCommand)
				.addCommand("toJson", toJsonCommand)
				.addCommand("merge", swaggerMergeCommand)
				.addCommand("smartMerge", swaggerSmartMergeCommand)
				.addCommand("parse", parseAndPrintCommand)
				.allowAbbreviatedOptions(true)
				.allowParameterOverwriting(true)
				.build();

		jc.setCaseSensitiveOptions(false);
		jc.setProgramName("Tool");
		try {
			jc.parse(args);
			if (mainCommand.help || jc.getParsedCommand() == null) {
				jc.usage();
			} else {
				JCommander parsedCommand = jc.getCommands().get(jc.getParsedCommand());
				ExecutableCommand execCommand = (ExecutableCommand) parsedCommand .getObjects().get(0);
				if (execCommand.help) {
					parsedCommand .usage();
				} else if (mainCommand.test == false) {
					if (execCommand.debug) {
						ch.qos.logback.classic.Logger sk = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
						sk.setLevel(Level.DEBUG);
						log.debug("Debug ON");
					}
					execCommand.execute();
				} else {
					log.debug("Parsed command: {}", execCommand);
				}
			}
		} catch (ParameterException pe) {
			System.err.println("Error parsing arguments: " + pe.getMessage());
			pe.usage();
		} catch (Throwable th) {
			th.printStackTrace(System.err);
		}
	}
}
