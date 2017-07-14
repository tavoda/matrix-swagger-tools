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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by tavoda on 7/6/17.
 */
@Parameters(commandDescription = "Merge swagger definitions from multiple files to one")
public class SwaggerSmartMergeCommand extends ExecutableCommand {
	private ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	private ObjectMapper jsonMapper = new ObjectMapper(new JsonFactory());

	private static final Logger log = LoggerFactory.getLogger(SwaggerSmartMergeCommand.class);

	@Parameter(description = "<Swagger API definition files>", required = true)
	List<String> inputFiles;

	@Parameter(names = {"-o", "--output-file"}, description = "Output file, if not specified calculated automatically")
	String outputFile = "/tmp/output.json";

	@Override
	public void execute() throws IOException {
		List<String> realFiles = new ArrayList<>();
		for (String inputFile : inputFiles) {
			File realFile = new File(inputFile);
			if (!realFile.exists()) {
				throw new InvalidParameterException("File " + realFile.getCanonicalPath() + " doesn't exist");
			} else if (!realFile.isFile()) {
				throw new InvalidParameterException("The path " + realFile.getCanonicalPath() + " is NOT file");
			} else if (!realFile.canRead()) {
				throw new InvalidParameterException("File " + realFile.getCanonicalPath() + " is NOT readable");
			} else {
				realFiles.add(realFile.getCanonicalPath());
			}
		}

		Map<String, Object> result = new HashMap<>();
		processedFiles.addAll(realFiles);
		merge(realFiles, result);
		processRefs(result);
		jsonMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputFile), result);
		log.info("Output written to {}", outputFile);
	}

	List<String> processedFiles = new ArrayList<>();

	private void merge(List<String> fileNames, Map<String, Object> result) throws IOException {
		TypeReference<Map<String, Object>> mapReference = new TypeReference<Map<String, Object>>() {};
		List<String> newFiles = new ArrayList<>();
		for (String fileName : fileNames) {
			File file = new File(fileName);
			Map<String, Object> parsedFile = null;
			try {
				if (file.isDirectory()) {
					log.error("Skipping folder {}", file.getName());
				} else if (file.getName().endsWith(".json")) {
					parsedFile = jsonMapper.readValue(file, mapReference);
				} else {
					parsedFile = yamlMapper.readValue(file, mapReference);
				}
			} catch (IOException ioe) {
				log.warn("Can not parse file {} - skipping ({})", file.getCanonicalPath(), ioe.getMessage());
			}
			if (parsedFile != null) {
				log.info("Processing file {}", file.getPath());
				List<String> refs = getRefs(parsedFile);
				for (String ref : refs) {
					if (!ref.startsWith("#")) {
						String newFile = file.getAbsoluteFile().getParent() + "/" + ref;
						newFile = (new File(newFile)).getCanonicalPath();
						if (!processedFiles.stream().filter(Predicate.isEqual(newFile)).findFirst().isPresent()) {
							newFiles.add(newFile);
							processedFiles.add(newFile);
						}
					}
				}
				if (parsedFile.containsKey("swagger")) {
					deepMapCopy("> ", parsedFile, result);
				} else {
					// Prepare definitionName
					String definitionName = file.getName();
					definitionName = definitionName.endsWith(".yaml") ? definitionName.substring(0, definitionName.length() - 5) : definitionName;
					// Prepare parsedFile
					int rootIndex = file.getCanonicalPath().indexOf("/matrix-doc/");
					rootIndex = rootIndex == -1 ? 0 : rootIndex + 1;
					parsedFile.put("x-origin", file.getCanonicalPath().substring(rootIndex));

					Map<String, Object> definitions = getOrCreate(result, "definitions");
					if (definitions.containsKey(definitionName)) {
						log.warn("Definition with name '{}' already exist", definitionName);
						Map<String, Object> subDefinition = (Map<String, Object>) definitions.get(definitionName);
						deepMapCopy("> ", parsedFile, subDefinition);
					} else {
						definitions.put(definitionName, parsedFile);
					}
				}
			}
		}
		if (newFiles.size() > 0) {
			merge(newFiles, result);
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

	private void deepMapCopy(String prefix, Map<String, Object> from, Map<String, Object> to) {
		for (Map.Entry<String, Object> entry : from.entrySet()) {
			if (!to.containsKey(entry.getKey())) {
				log.info("{}Puting new value in key '{}'", prefix, entry.getKey());
				to.put(entry.getKey(), entry.getValue());
			} else if (entry.getValue() instanceof Collection) {
//				log.error("Recursive LIST COPY {}", entry.getKey());
			} else if (entry.getValue() instanceof Map && to.get(entry.getKey()) instanceof Map) {
				log.info("{}Diving to key '{}'", prefix, entry.getKey());
				deepMapCopy("  " + prefix, (Map<String, Object>) entry.getValue(), (Map<String, Object>) to.get(entry.getKey()));
			} else if (entry.getValue() instanceof Map) {
				log.warn("{}Different datatype in key '{}' src value '{}' dst value '{}'", prefix, entry.getKey(), entry.getValue(), to.get(entry.getKey()));
			} else if (entry.getValue() instanceof String) {
				if (!entry.getValue().equals(to.get(entry.getKey()))) {
					log.info("{}Overwriting key '{}' with value '{}' with '{}'", prefix, entry.getKey(), to.get(entry.getKey()), entry.getValue());
				}
			} else if (entry.getValue() instanceof Number) {
				if (!entry.getValue().equals(to.get(entry.getKey()))) {
					log.info("{}Overwriting key '{}' with value '{}' with '{}'", prefix, entry.getKey(), to.get(entry.getKey()), entry.getValue());
				}
			} else {
				log.error("{}Unknown value in key '{}' type '{}'", prefix, entry.getKey(), entry.getValue().getClass().getName());
			}
		}
	}

	private List<String> getRefs(Map<String, Object> result) {
		List<String> refs = new ArrayList<>();
		getRefsRecursive(result, refs);
		return refs;
	}

	private List<String> getRefsRecursive(Map<String, Object> result, final List<String> refs) {
		for (Map.Entry<String, Object> entry : result.entrySet()) {
			if (entry.getKey().equals("$ref")) {
				refs.add((String) entry.getValue());
			} else if (entry.getValue() instanceof Map) {
				getRefsRecursive((Map<String, Object>) entry.getValue(), refs);
			} else if (entry.getValue() instanceof Collection) {
				Collection c = (Collection) entry.getValue();
				c.forEach(it -> {
					if (it instanceof Map) {
						getRefsRecursive((Map<String, Object>) it, refs);
					}
				});
			}
		}
		return refs;
	}

	private void processRefs(Map<String, Object> result) {
		for (Map.Entry<String, Object> entry : result.entrySet()) {
			if (entry.getKey().equals("$ref")) {
				String ref = (String) entry.getValue();
				int slashIndex = ref.lastIndexOf('/');
				String newRef = slashIndex == -1 ? ref : ref.substring(slashIndex + 1);
				newRef = "#/definitions/" + (newRef.endsWith(".yaml") ? newRef.substring(0, newRef.length() - 5) : newRef);
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
		return super.toString() + " :: inputFiles=" + inputFiles + (outputFile != null ? " :: outputFile=" + outputFile : "");
	}
}
