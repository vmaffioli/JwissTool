package com.digitalfen.jwiss.tool.boot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.digitalfen.jwiss.devkit.handlers.JwissCache;
import com.digitalfen.jwiss.devkit.handlers.JwissLogger;
import com.digitalfen.jwiss.devkit.handlers.JwissUtils;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class JwissConfigLoader {

    /**
     * Read, load and cache all configurations. Method by method.
     */
    public void init() {
	loadGlobalConfig();
    }

    /**
     * Read, load and cache global configurations
     */
    private void loadGlobalConfig() {

	Map<String, String> definedCfg;
	try {
	    definedCfg = readValues("global");

	    for (Entry<String, String> option : definedCfg.entrySet()) {
		JwissCache.configurations.put(null,
			option.getKey(),
			option.getValue());
	    }

	    JwissLogger.printer.info("Loading Global Configurations");
	    JwissLogger.printer.debug("Global Configurations File successful loaded.");
	    JwissLogger.printer.info("Loading Global Configurations Done.");

	} catch (Exception e) {
	    JwissLogger.printer.error(e);

	}

    }

    private Map<String, String> readValues(String configName) throws Exception {
	boolean isValid = false;
	Map<String, String> definedCfg = readDefinedValues(configName);
	Map<String, String[]> defaultCfg = readDefaultValues("jwiss.properties");

	for (Entry<String, String> option : definedCfg.entrySet()) {
	    if (defaultCfg.containsKey(option.getKey())) {
		for (String defaultOption : defaultCfg.get(option.getKey())) {
		    if (option.getValue().equals(defaultOption))
			isValid = true;
		}
	    }
	}

	if (!isValid)
	    throw new Exception();

	return definedCfg;

    };

    private Map<String, String[]> readDefaultValues(String configName) {
	Map<String, String[]> propertiesMap = new HashMap<>();
	Map<String, List<String>> tempMap = new HashMap<>();
	Map<String, String> activeToOptionMap = new HashMap<>();

	try (InputStream inputStream = JwissUtils.class.getClassLoader()
		.getResourceAsStream(configName)) {
	    if (inputStream == null) {
		throw new IOException("File not found: " + configName);
	    }

	    Properties properties = new Properties();
	    properties.load(inputStream);

	    // Primeiro, mapear todos os valores .active para os respectivos prefixos
	    properties.stringPropertyNames().forEach(key -> {
		String[] parts = key.split("\\.");
		if (parts.length == 2 && parts[1].equals("active")) {
		    String prefix = parts[0];
		    String activeValue = properties.getProperty(key);
		    activeToOptionMap.put(prefix, activeValue);

		}
	    });

	    // Em seguida, armazenar todos os valores .option associados aos respectivos
	    // prefixos
	    properties.stringPropertyNames().forEach(key -> {
		String[] parts = key.split("\\.");
		if (parts.length == 3 && parts[1].equals("option")) {
		    String prefix = parts[0];
		    String activeValue = activeToOptionMap.get(prefix);
		    if (activeValue != null) {
			String optionKey = prefix + ".option." + activeValue;
			if (properties.getProperty(optionKey) != null) {
			    tempMap.putIfAbsent(prefix, new ArrayList<>());
			    tempMap.get(prefix).add(properties.getProperty(key));
			}
		    }
		}
	    });

	    tempMap.forEach((k, v) -> propertiesMap.put(k, v.toArray(new String[0])));

	} catch (IOException e) {
	    e.printStackTrace();
	}

	return propertiesMap;
    }

    private Map<String, String> readDefinedValues(String fileName) {
	String configDirPath = new File("").getAbsolutePath();

	String filePath = configDirPath + File.separator
		+ "config"
		+ File.separator
		+ fileName
		+ ".conf";

	Map<String, String> propertiesMap = new HashMap<>();

	try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
	    String line;
	    while ((line = br.readLine()) != null) {
		line = line.trim();
		if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
		    continue;
		}
		String[] parts = line.split("[=:]");
		if (parts.length >= 2) {
		    String key = parts[0].trim();
		    String value = parts[1].trim();
		    propertiesMap.put(key, value);
		}
	    }
	} catch (FileNotFoundException e) {
	    e.printStackTrace();

	} catch (IOException e) {
	    e.printStackTrace();

	}

	return propertiesMap;
    }

}
