package de.codewave.mytunesrss.jmx;

/**
 * de.codewave.mytunesrss.jmx.ApplicationMBean
 */
public interface ApplicationConfigMBean {
    String getVersion();

    String quit();

    String getLicense();

    String getCodewaveLogLevel();

    void setCodewaveLogLevel(String level);

    String sendSupportRequest(String name, String email, String comment, boolean includeItunesXml);

    String getSystemInfo();

    String getApplicationState();
}