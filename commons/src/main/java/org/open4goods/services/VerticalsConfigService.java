package org.open4goods.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.Localisable;
import org.open4goods.model.constants.CacheConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.databind.ObjectReader;

/**
 * This service is in charge to provide the Verticals configurations.
 * Configurations are provided from the classpath AND from a git specific
 * project (fresh local clone on app startup)
 *
 * @author goulven
 *
 */
public class VerticalsConfigService {

	public static final String MAIN_VERTICAL_NAME = "all";

	public static final Logger logger = LoggerFactory.getLogger(VerticalsConfigService.class);

	private static final String DEFAULT_CONFIG_FILENAME = "_default.yml";
//	private static final String CLASSPATH_VERTICALS = "classpath:**verticals/*.yml";
//	private static final String CLASSPATH_VERTICAL_PREFIX = "classpath:verticals/";

	private SerialisationService serialisationService;

	private final Map<String, VerticalConfig> configs = new ConcurrentHashMap<>(100);
	
	private final Map<String,VerticalConfig> categoriesToVertical = new ConcurrentHashMap<>();

	private Map<String,VerticalConfig> verticalsByUrl = new HashMap<>();
//	private Map<String,String> verticalUrlByLanguage = new HashMap<>();



	private String verticalsFolder;

	public VerticalsConfigService(SerialisationService serialisationService, String verticalsFolder) {
		super();
		this.serialisationService = serialisationService;
		this.verticalsFolder = verticalsFolder;

		// initial configs loads
		loadConfigs();

	}

	/**
	 * Loads configs from classpath and from giit repositories. Thread safe
	 * operation, so can be used in refresh
	 */
	@Scheduled(fixedDelay = 1000 * 60 * 10, initialDelay = 1000 * 60 * 10)
	public synchronized void loadConfigs() {

		Map<String, VerticalConfig> configs2 = new ConcurrentHashMap<>(100);

		/////////////////////////////////////////
		// Load configurations from classpath
		/////////////////////////////////////////

		for (VerticalConfig uc : getFolderVerticalConfigs()) {
			logger.info("Adding config {} from classpath", uc.getId());
			configs2.put(uc.getId(), uc);
		}
		// Switching confs
		synchronized (configs) {
			configs.clear();
			configs.putAll(configs2);
		}

		// Associating categoriesToVertical
		synchronized (categoriesToVertical) {
			categoriesToVertical.clear();
			configs.values().forEach(c -> c.getMatchingCategories().forEach(cc -> categoriesToVertical.put(cc, c)));
		}

		// Mapping url to i18n
		getConfigsWithoutDefault().forEach(vc -> vc.getHomeUrl().forEach((key, value) -> {

			verticalsByUrl.put(value, vc);
//			verticalUrlByLanguage.put(key, value);
		}));

	}




	/**
	 *
	 * @return the available verticals configurations from classpath
	 */
	private List<VerticalConfig> getFolderVerticalConfigs() {
		List<VerticalConfig> ret = new ArrayList<>();

		File verticalFolder = new File(verticalsFolder);

		for (File filename : verticalFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"))) {
			if (filename.getName().equals(DEFAULT_CONFIG_FILENAME)) {
				continue;
			}
			try {
				ret.add(getConfig(new FileInputStream(filename), getDefaultConfig()));
			} catch (IOException e) {
				logger.error("Cannot retrieve vertical config", e);
			}
		}

		return ret;
	}

	/**
	 * Instanciate a config with a previously defaulted one
	 *
	 * @param inputStream
	 * @param existing
	 * @return
	 * @throws IOException
	 */
	public VerticalConfig getConfig(InputStream inputStream, VerticalConfig existing) throws IOException {

		ObjectReader objectReader = serialisationService.getYamlMapper().readerForUpdating(existing);
		VerticalConfig ret = objectReader.readValue(inputStream);
		inputStream.close();
		return ret;
	}

	/**	Add a config from api endpoint **/
	public void addTmpConfig(VerticalConfig vc) {

		try {
			logger.warn("Adding a non versionned vertical config file. Have to do a PR on open4goods-config to persist it");
			File dest = new File(verticalsFolder + File.separator + vc.getId() + ".yml");
			serialisationService.getYamlMapper().writeValue(dest, vc);

			// Reload configs
			loadConfigs();
		} catch (Exception e) {
			logger.error("Cannot persist vertical config", e);
		}
    }

	/**
	 * Instanciate a vertical config for a given category name
	 *
	 * @param inputStream
	 * @param existing
	 * @return
	 * @throws IOException
	 */
	public VerticalConfig getVerticalForCategories(Set<String> categories) {


		VerticalConfig vc = null;

		for (String category : categories) {
			vc = categoriesToVertical.get(category);

			// Discarding if unmatching category
			if (null != vc) {
				if (vc.getUnmatchingCategories().contains(category)) {
					vc = null;
				}
			}

			if (null != vc) {
				break;
			}
		}

		return vc;
	}



	/**
	 * Return the language for a vertical path, if any
	 * @param path
	 * @return
	 */
	public VerticalConfig getVerticalForPath(String path) {
		return verticalsByUrl.get(path);
	}

	/**
	 * Return the path for a vertical language, if any
	 * @param config 
	 * @param path
	 * @return
	 */
	public String getPathForVerticalLanguage(String language, VerticalConfig config) {
		Localisable pathes = configs.get(config.getId()).getHomeUrl();
		
		for (Entry<String, String> key : pathes.entrySet()) {
            if (key.getKey().equals(language)) {
                return key.getValue() ;
            }
        }
		
		return null;
	}

	/**
	 *
	 * @return
	 * @throws IOException
	 */
	@Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME)
	public VerticalConfig getDefaultConfig() throws IOException {
		FileInputStream f = new FileInputStream(verticalsFolder + File.separator + DEFAULT_CONFIG_FILENAME);
		VerticalConfig ret = serialisationService.fromYaml(f, VerticalConfig.class);
		f.close();
		return ret;
	}



	/**
	 * Return a config by it's Id
	 *
	 * @param verticalName
	 * @return
	 */
	public Optional<VerticalConfig> getConfigById(String verticalName) {
		return Optional.of(configs.get(verticalName));
	}


	/**
	 *
	 * @return all confifs, except the _default and the "all" one
	 */
	public Set<VerticalConfig>  getConfigsWithoutDefault() {
		return getConfigs().values().stream().filter(e -> (!e.getId().equals("all")) ).collect(Collectors.toSet());
	}
	/**
	 *
	 * @return all Vertical configs
	 */
	public Map<String, VerticalConfig> getConfigs() {
		return configs;
	}
}
