package org.matrix.swagger.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tavoda on 7/6/17.
 */
@Parameters(commandDescription = "Merge swagger definitions from multiple files to one")
public class SwaggerMergeCommand extends ExecutableCommand {
	private ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	private ObjectMapper jsonMapper = new ObjectMapper(new JsonFactory());

	private static final Logger log = LoggerFactory.getLogger(SwaggerMergeCommand.class);

	@Parameter(description = "<Swagger definition directory>", required = true)
	String inputFile;

	@Parameter(names = {"-o", "--output-file"}, description = "Output file, if not specified calculated automatically")
	String outputFile;

	@Override
	public void execute() throws IOException {
		File realFile = new File(inputFile);
		if (!realFile.exists()) {
			throw new InvalidParameterException("Directory " + realFile.getCanonicalPath() + " doesn't exist");
		} else if (!realFile.isDirectory()) {
			throw new InvalidParameterException("File " + realFile.getCanonicalPath() + " is NOT directory");
		} else if (!realFile.canRead()) {
			throw new InvalidParameterException("Directory " + realFile.getCanonicalPath() + " is NOT readable");
		} else {
			Map<String, Object> result = new HashMap<>();
			smartMerge(realFile, result);
			processRefs(result);
			jsonMapper.writerWithDefaultPrettyPrinter().writeValue(new File("/tmp/output.json"), result);
		}
	}

	private void smartMerge(File dir, Map<String, Object> result) throws IOException {
		File[] files = dir.listFiles();
		TypeReference<Map<String, Object>> mapReference = new TypeReference<Map<String, Object>>() {};
		for (File file : files) {
			Map<String, Object> parsedFile = null;
			try {
				if (file.isDirectory()) {
					if (file.getAbsolutePath().indexOf("example") == -1) {
						log.info("Diving to directory {}", file.getCanonicalPath());
						smartMerge(file, result);
						log.info("Up to directory {}", file.getCanonicalPath());
					}
				} else if (file.getName().endsWith(".json")) {
					parsedFile = jsonMapper.readValue(file, mapReference);
				} else {
					parsedFile = yamlMapper.readValue(file, mapReference);
//					log.warn("Skipping file {} is not YAML or JSON file", file.getName());
				}
			} catch (IOException ioe) {
				log.warn("Can not parse file {} - skipping", file.getCanonicalPath());
			}
			if (parsedFile != null) {
				log.info("Processing file {}", file.getName());
				if (file.getAbsolutePath().indexOf("definitions") != -1) {
					String definitionName = file.getName();
					definitionName = definitionName.endsWith(".yaml") ? definitionName.substring(0, definitionName.length() - 5) : definitionName;

					Map<String, Object> definitions = getOrCreate(result, "definitions");
					Map<String, Object> subDefinition = getOrCreate(definitions, definitionName);
					subDefinition.put("origin", file.getPath());
					deepMapCopy(parsedFile, subDefinition);
				} else {
					deepMapCopy(parsedFile, result);
				}
			}
		}
	}

	private Map<String, Object> getOrCreate(Map<String, Object> result, String property) {
		Map<String, Object> newProperty = (Map<String, Object>) result.get(property);
		if (newProperty == null) {
			newProperty = new HashMap<String, Object>();
			result.put(property, newProperty);
		}
		return newProperty;
	}

	private void deepMapCopy(Map<String, Object> from, Map<String, Object> to) {
		for (Map.Entry<String, Object> entry : from.entrySet()) {
			if (!to.containsKey(entry.getKey())) {
				to.put(entry.getKey(), entry.getValue());
			} else if (entry.getValue() instanceof Collection) {
//				log.error("Recursive LIST COPY {}", entry.getKey());
			} else if (entry.getValue() instanceof Map && to.get(entry.getKey()) instanceof Map) {
				log.info("Recursive key {}", entry.getKey());
				deepMapCopy((Map<String, Object>) entry.getValue(), (Map<String, Object>) to.get(entry.getKey()));
			} else if (entry.getValue() instanceof Map) {
				log.info("Different datatype in key {} src value {} dst value {}", entry.getKey(), entry.getValue(), to.get(entry.getKey()));
			} else if (entry.getValue() instanceof String) {
				if (!entry.getValue().equals(to.get(entry.getKey()))) {
					log.info("Overwriting key {} with value {} to {}", entry.getKey(), to.get(entry.getKey()), entry.getValue());
				}
			} else if (entry.getValue() instanceof Number) {
				if (!entry.getValue().equals(to.get(entry.getKey()))) {
					log.info("Overwriting key {} with value {} to {}", entry.getKey(), to.get(entry.getKey()), entry.getValue());
				}
			} else {
				log.error("Unknown value in key {} type {}", entry.getKey(), entry.getValue().getClass().getName());
			}
		}
	}

	private void processRefs(Map<String, Object> result) {
		for (Map.Entry<String, Object> entry : result.entrySet()) {
			if (entry.getKey().equals("$ref")) {
				String ref = (String) entry.getValue();
				int slashIndex = ref.lastIndexOf('/');
				String newRef = slashIndex == -1 ? ref : ref.substring(slashIndex + 1);
				newRef = "#definitions/" + (newRef.endsWith(".yaml") ? newRef.substring(0, newRef.length() - 5) : newRef);
				log.info("REF: {} -> {}", ref, newRef);
				entry.setValue(newRef);
			} else if (entry.getValue() instanceof Map) {
				processRefs((Map<String, Object>) entry.getValue());
			} else if (entry.getValue() instanceof Collection) {
				Collection c = (Collection) entry.getValue();
				c.forEach(it -> {
					if (it instanceof Map) {
						processRefs((Map<String, Object>) it);
					}
				});
			}
		}
	}

	@Override
	public String toString() {
		return super.toString() + " :: inputFile=" + inputFile + (outputFile != null ? " :: outputFile=" + outputFile : "");
	}
}
