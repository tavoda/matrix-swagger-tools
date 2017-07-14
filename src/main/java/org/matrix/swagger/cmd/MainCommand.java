package org.matrix.swagger.cmd;

import com.beust.jcommander.Parameter;

/**
 * Created by tavoda on 7/4/17.
 */
public class MainCommand {
	@Parameter(names = {"-h", "--help"}, description = "Print help", help = true, hidden = true)
	public boolean help = false;

	@Parameter(names = {"-t", "--test"}, description = "Parsing parameters test - do NOT execute command", hidden = true)
	public boolean test = false;
}
