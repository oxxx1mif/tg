package org.telegram.messenger.amnezia;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AwgCacheManager {
    private static final String PREFS_NAME = "amnezia_wg_prefs";
    private static final String KEY_ACTIVE_CONFIG = "active_config_id";
    private static final String PREFIX_CONFIG_TEXT = "cfg_text_";
    private static final String PREFIX_CONFIG_NAME = "cfg_name_";

    private final SharedPreferences prefs;

    public AwgCacheManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Сохраняет или обновляет конфигурацию в кэше
     */
    public void saveConfig(AwgConfig config) {
        if (config == null || config.getId() == null) return;

        prefs.edit()
                .putString(PREFIX_CONFIG_NAME + config.getId(), config.getName())
                .putString(PREFIX_CONFIG_TEXT + config.getId(), config.getConfigText())
                .apply();
    }

    /**
     * Возвращает ID текущего выбранного активного конфига
     */
    public String getActiveConfigId() {
        return prefs.getString(KEY_ACTIVE_CONFIG, "");
    }

    /**
     * Устанавливает активный конфиг
     */
    public void setActiveConfigId(String configId) {
        prefs.edit().putString(KEY_ACTIVE_CONFIG, configId).apply();
    }

    /**
     * Возвращает список всех импортированных конфигураций
     */
    public List<AwgConfig> getAllConfigs() {
        List<AwgConfig> configs = new ArrayList<>();
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(PREFIX_CONFIG_TEXT)) {
                String id = key.substring(PREFIX_CONFIG_TEXT.length());
                String configText = entry.getValue().toString();
                String name = prefs.getString(PREFIX_CONFIG_NAME + id, "Amnezia Config");

                configs.add(new AwgConfig(id, name, configText));
            }
        }
        return configs;
    }

    /**
     * Удаляет конфигурацию по ID
     */
    public void deleteConfig(String configId) {
        if (configId == null) return;

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREFIX_CONFIG_TEXT + configId);
        editor.remove(PREFIX_CONFIG_NAME + configId);

        if (configId.equals(getActiveConfigId())) {
            editor.remove(KEY_ACTIVE_CONFIG);
        }
        editor.apply();
    }
}