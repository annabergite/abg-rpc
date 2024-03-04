package rpc.abg.config;

import java.util.List;

import com.typesafe.config.Config;

public class ConfigUtils {

	public static String getStringOrElse(Config config, String path, String defaultValue) {
		if (config.hasPath(path)) {
			return config.getString(path);
		} else {
			return defaultValue;
		}
	}

	public static List<String> getStringListOrElse(Config config, String path, List<String> defaultValue) {
		if (config.hasPath(path)) {
			return config.getStringList(path);
		} else {
			return defaultValue;
		}
	}

	public static int getIntOrElse(Config config, String path, int defaultValue) {
		if (config.hasPath(path)) {
			return config.getInt(path);
		} else {
			return defaultValue;
		}
	}

	public static boolean getBooleanOrElse(Config config, String path, boolean defaultValue) {
		if (config.hasPath(path)) {
			return config.getBoolean(path);
		} else {
			return defaultValue;
		}
	}

}
