package org.telegram.messenger.amnezia;

public class AwgConfig {
    private String id;
    private String name;
    private String configText;

    public AwgConfig(String id, String name, String configText) {
        this.id = id;
        this.name = name;
        this.configText = configText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfigText() {
        return configText;
    }

    public void setConfigText(String configText) {
        this.configText = configText;
    }
}