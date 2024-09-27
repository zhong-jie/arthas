package com.taobao.arthas.core.command.model;

import java.util.Map;

/**
 * @author zhongjie
 * @since 2024-09-27
 */
public class SpringPropertiesVO {

    private Object applicationContext;
    private Map<String, String> properties;

    public Object getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(Object applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
