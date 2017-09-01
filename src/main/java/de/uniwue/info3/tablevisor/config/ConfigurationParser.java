package de.uniwue.info3.tablevisor.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationParser {
	public static Configuration parseYamlFile(Path p) throws IOException {
		Yaml yaml = new Yaml(new Constructor(Configuration.class));
		InputStream stream = Files.newInputStream(p);
		Configuration config = (Configuration) yaml.load(stream);
		stream.close();
		return config;
	}
}

