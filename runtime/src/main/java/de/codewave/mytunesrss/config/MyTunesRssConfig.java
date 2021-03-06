/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.config;

import de.codewave.mytunesrss.ImageImportType;
import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.config.transcoder.TranscoderConfig;
import de.codewave.mytunesrss.datastore.itunes.ItunesPlaylistType;
import de.codewave.utils.MiscUtils;
import de.codewave.utils.Version;
import de.codewave.utils.io.IOUtils;
import de.codewave.utils.xml.DOMUtils;
import de.codewave.utils.xml.JXPathUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.hsqldb.jdbc.JDBCDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * de.codewave.mytunesrss.config.MyTunesRssConfig
 */
public class MyTunesRssConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyTunesRssConfig.class);
    public static final String DEFAULT_INTERNAL_MYSQL_CONNECTION_OPTIONS = "server.max_allowed_packet=16M&server.innodb_log_file_size=64M&server.character-set-server=utf8&server.innodb_flush_log_at_trx_commit=2&server.innodb_buffer_pool_size=67108864&server.innodb_file_per_table=1";

    private String myHost;
    private int myPort;
    private String myServerName = "MyTunesRSS";
    private boolean myAvailableOnLocalNet = true;
    private List<DatasourceConfig> myDatasources = new ArrayList<>();
    private boolean myCheckUpdateOnStart = true;
    private String myVersion;
    private Collection<User> myUsers = new HashSet<>();
    private String myProxyHost = "";
    private int myProxyPort = -1;
    private boolean myLocalTempArchive;
    private SecretKey myPathInfoKey;
    private String myWebWelcomeMessage = "";
    private String myWebLoginMessage = "";
    private long myTranscodingCacheMaxGiB = 1;
    private long myTempMaxGiB = 1;
    private long myHttpLiveStreamCacheMaxGiB = 5;
    private Level myCodewaveLogLevel;
    private String myLastNewVersionInfo;
    private boolean myDeleteDatabaseOnExit;
    private String myUpdateIgnoreVersion;
    private List<String> myDatabaseUpdateTriggers = new ArrayList<>();
    private List<String> myDatabaseBackupTriggers = new ArrayList<>();
    private DatabaseType myDatabaseType;
    private String myDatabaseConnection;
    private String myDatabaseUser;
    private String myDatabasePassword;
    private String myDatabaseDriver;
    private String myDatabaseConnectionOptions;
    private String myWebappContext;
    private String myTomcatMaxThreads;
    private String mySslKeystoreFile;
    private String mySslKeystorePass;
    private String mySslHost;
    private int mySslPort;
    private String mySslKeystoreKeyAlias;
    private String myMailHost;
    private int myMailPort;
    private SmtpProtocol mySmtpProtocol;
    private String myMailLogin;
    private String myMailPassword;
    private String myMailSender;
    private String myAdminEmail;
    private boolean myNotifyOnPasswordChange;
    private boolean myNotifyOnEmailChange;
    private boolean myNotifyOnQuotaExceeded;
    private boolean myNotifyOnLoginFailure;
    private boolean myNotifyOnWebUpload;
    private boolean myNotifyOnTranscodingFailure;
    private boolean myNotifyOnInternalError;
    private boolean myNotifyOnDatabaseUpdate;
    private boolean myNotifyOnMissingFile;
    private boolean myNotifyOnOutdatedItunesXml;
    private boolean myNotifyOnSkippedDatabaseUpdate;
    private int myStatisticKeepTime = 60;
    private Collection<TranscoderConfig> myTranscoderConfigs = new ArrayList<>();
    private List<ExternalSiteDefinition> myExternalSites = new ArrayList<>();
    private String myAutoLogin;
    private boolean myDisableBrowser;
    private boolean myServerBrowserActive;
    private boolean myOpenIdActive;
    private boolean myDisableWebLogin;
    private LdapConfig myLdapConfig;
    private byte[] myAdminPasswordHash;
    private String myAdminHost;
    private int myAdminPort;
    private Set<FlashPlayerConfig> myFlashPlayers = new HashSet<>();
    private boolean myInitialWizard;
    private boolean myUpnpAdmin;
    private boolean myUpnpUserHttp;
    private boolean myUpnpUserHttps;
    private String mySelfRegisterTemplateUser;
    private boolean mySelfRegAdminEmail;
    private String myDefaultUserInterfaceTheme;
    private String myFacebookApiKey = "102138059883364";
    private int myNumberKeepDatabaseBackups;
    private boolean myBackupDatabaseAfterInit;
    private boolean myHeadless;
    private boolean myVlcEnabled;
    private File myVlcExecutable;
    private File myGmExecutable;
    private boolean myGmEnabled;
    private long myImageExpirationMillis;
    private long myRestApiJsExpirationMillis;
    private int myJpegQuality;
    private int myOnDemandThumbnailGenerationThreads;
    private int myOnDemandThumbnailGenerationTimeoutSeconds;
    private int myUserAccessLogRetainDays;
    private int myAdminAccessLogRetainDays;
    private boolean myUserAccessLogExtended;
    private boolean myAdminAccessLogExtended;
    private String myAccessLogTz;
    private Map<String, String> myGenreMappings = new HashMap<>();
    private boolean myUpnpMediaServerActive;
    private String myUpnpMediaServerName;
    private int myUpnpMediaServerLockTimeoutSeconds = 10;
    private Map<String, String> myCustomWebHeaders = new HashMap<>();

    /**
     * Get a shallow copy of the list of data sources. The list is a copy of the original list containing references to
     * the original data source configs. Modifications to the returned data source configs are stored on the next {@link #save()} call.
     *
     * @return A shallow copy of the list of data source configs.
     */
    public synchronized List<DatasourceConfig> getDatasources() {
        return new ArrayList<>(myDatasources);
    }

    /**
     * Returns the data source configuration for the specified source id. The reference to the original config is returned.
     * Modifications to the returned data source config are stored on the next {@link #save()} call.
     *
     * @param sourceId A data source id.
     *
     * @return The data source config for the specified source id.
     */
    public synchronized DatasourceConfig getDatasource(String sourceId) {
        for (DatasourceConfig config : getDatasources()) {
            if (config.getId().equals(sourceId)) {
                return config;
            }
        }
        return null;
    }

    public synchronized void setDatasources(List<DatasourceConfig> datasources) {
        myDatasources = new ArrayList<>(datasources);
        Collections.sort(myDatasources);
    }

    public synchronized String getHost() {
        return myHost;
    }

    public synchronized void setHost(String host) {
        myHost = StringUtils.trimToNull(host);
    }

    public synchronized int getPort() {
        return myPort;
    }

    public synchronized void setPort(int port) {
        myPort = port;
    }

    public synchronized String getServerName() {
        return myServerName;
    }

    public synchronized void setServerName(String serverName) {
        if (StringUtils.isNotEmpty(serverName)) {
            myServerName = serverName;
        }
    }

    public synchronized boolean isAvailableOnLocalNet() {
        return myAvailableOnLocalNet;
    }

    public synchronized void setAvailableOnLocalNet(boolean availableOnLocalNet) {
        myAvailableOnLocalNet = availableOnLocalNet;
    }

    public synchronized boolean isCheckUpdateOnStart() {
        return myCheckUpdateOnStart;
    }

    public synchronized void setCheckUpdateOnStart(boolean checkUpdateOnStart) {
        myCheckUpdateOnStart = checkUpdateOnStart;
    }

    public synchronized String getVersion() {
        return myVersion;
    }

    public synchronized void setVersion(String version) {
        myVersion = version;
    }

    public synchronized boolean isLocalTempArchive() {
        return myLocalTempArchive;
    }

    public synchronized void setLocalTempArchive(boolean localTempArchive) {
        myLocalTempArchive = localTempArchive;
    }

    public synchronized SecretKey getPathInfoKey() {
        return myPathInfoKey;
    }

    public synchronized void setPathInfoKey(SecretKey pathInfoKey) {
        myPathInfoKey = pathInfoKey;
    }

    public synchronized long getTranscodingCacheMaxGiB() {
        return myTranscodingCacheMaxGiB;
    }

    public synchronized void setTranscodingCacheMaxGiB(long transcodingCacheMaxGiB) {
        myTranscodingCacheMaxGiB = transcodingCacheMaxGiB;
    }

    public synchronized long getTempMaxGiB() {
        return myTempMaxGiB;
    }

    public synchronized void setTempMaxGiB(long tempMaxGiB) {
        myTempMaxGiB = tempMaxGiB;
    }

    public synchronized long getHttpLiveStreamCacheMaxGiB() {
        return myHttpLiveStreamCacheMaxGiB;
    }

    public synchronized void setHttpLiveStreamCacheMaxGiB(long httpLiveStreamCacheMaxGiB) {
        myHttpLiveStreamCacheMaxGiB = httpLiveStreamCacheMaxGiB;
    }

    public synchronized Level getCodewaveLogLevel() {
        return myCodewaveLogLevel;
    }

    public synchronized void setCodewaveLogLevel(Level codewaveLogLevel) {
        myCodewaveLogLevel = codewaveLogLevel;
    }

    public synchronized Collection<User> getUsers() {
        Collection<User> users = new HashSet<>();
        for (User user : myUsers) {
            users.add(user);
        }
        return users;
    }

    public synchronized User getUser(String name) {
        for (User user : getUsers()) {
            if (user.getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    public synchronized void removeUser(User user) {
        myUsers.remove(user);
        for (User eachUser : myUsers) {
            if (eachUser.getParent() == user) {
                eachUser.setParent(null);
            }
        }
    }

    public synchronized boolean addUser(User user) {
        if (myUsers.contains(user)) {
            return false;
        }
        myUsers.add(user);
        return true;
    }

    public synchronized String getProxyHost() {
        return myProxyHost;
    }

    public synchronized void setProxyHost(String proxyHost) {
        myProxyHost = proxyHost;
    }

    public synchronized int getProxyPort() {
        return myProxyPort;
    }

    public synchronized void setProxyPort(int proxyPort) {
        myProxyPort = proxyPort;
    }

    public synchronized boolean isProxyServer() {
        return StringUtils.isNotBlank(myProxyHost) && myProxyPort > 0 && myProxyPort < 65536;
    }

    public synchronized String getWebWelcomeMessage() {
        return myWebWelcomeMessage;
    }

    public synchronized void setWebWelcomeMessage(String webWelcomeMessage) {
        myWebWelcomeMessage = webWelcomeMessage;
    }

    public synchronized String getWebLoginMessage() {
        return myWebLoginMessage;
    }

    public synchronized void setWebLoginMessage(String webLoginMessage) {
        myWebLoginMessage = webLoginMessage;
    }

    public synchronized void setLastNewVersionInfo(String lastNewVersionInfo) {
        myLastNewVersionInfo = lastNewVersionInfo;
    }

    public synchronized boolean isDeleteDatabaseOnExit() {
        return myDeleteDatabaseOnExit;
    }

    public synchronized void setDeleteDatabaseOnExit(boolean deleteDatabaseOnExit) {
        myDeleteDatabaseOnExit = deleteDatabaseOnExit;
    }

    public synchronized void setUpdateIgnoreVersion(String updateIgnoreVersion) {
        myUpdateIgnoreVersion = updateIgnoreVersion;
    }

    public synchronized List<String> getDatabaseUpdateTriggers() {
        return myDatabaseUpdateTriggers;
    }

    public synchronized void setDatabaseUpdateTriggers(List<String> databaseCronTriggers) {
        myDatabaseUpdateTriggers = databaseCronTriggers;
    }

    public synchronized List<String> getDatabaseBackupTriggers() {
        return myDatabaseBackupTriggers;
    }

    public synchronized void setDatabaseBackupTriggers(List<String> databaseBackupTriggers) {
        myDatabaseBackupTriggers = databaseBackupTriggers;
    }

    public synchronized String getDatabaseConnection() {
        return myDatabaseConnection;
    }

    public synchronized void setDatabaseConnection(String databaseConnection) {
        myDatabaseConnection = databaseConnection;
    }

    public synchronized String getDatabasePassword() {
        return myDatabasePassword;
    }

    public synchronized void setDatabasePassword(String databasePassword) {
        myDatabasePassword = databasePassword;
    }

    public synchronized DatabaseType getDatabaseType() {
        return myDatabaseType;
    }

    public synchronized void setDatabaseType(DatabaseType databaseType) {
        myDatabaseType = databaseType;
    }

    public synchronized String getDatabaseUser() {
        return myDatabaseUser;
    }

    public synchronized void setDatabaseUser(String databaseUser) {
        myDatabaseUser = databaseUser;
    }

    public synchronized String getDatabaseDriver() {
        return myDatabaseDriver;
    }

    public synchronized void setDatabaseDriver(String databaseDriver) {
        myDatabaseDriver = databaseDriver;
    }

    public synchronized String getDatabaseConnectionOptions() {
        return myDatabaseConnectionOptions;
    }

    public synchronized void setDatabaseConnectionOptions(String databaseConnectionOptions) {
        myDatabaseConnectionOptions = databaseConnectionOptions;
    }

    public synchronized String getWebappContext() {
        return myWebappContext;
    }

    public synchronized void setWebappContext(String webappContext) {
        myWebappContext = webappContext;
    }

    public synchronized String getTomcatMaxThreads() {
        return myTomcatMaxThreads;
    }

    public synchronized void setTomcatMaxThreads(String tomcatMaxThreads) {
        myTomcatMaxThreads = tomcatMaxThreads;
    }

    public synchronized String getSslKeystoreFile() {
        return mySslKeystoreFile;
    }

    public synchronized void setSslKeystoreFile(String sslKeystoreFile) {
        mySslKeystoreFile = sslKeystoreFile;
    }

    public synchronized String getSslKeystoreKeyAlias() {
        return mySslKeystoreKeyAlias;
    }

    public synchronized void setSslKeystoreKeyAlias(String sslKeystoreKeyAlias) {
        mySslKeystoreKeyAlias = sslKeystoreKeyAlias;
    }

    public synchronized String getSslKeystorePass() {
        return mySslKeystorePass;
    }

    public synchronized void setSslKeystorePass(String sslKeystorePass) {
        mySslKeystorePass = sslKeystorePass;
    }

    public synchronized String getSslHost() {
        return mySslHost;
    }

    public synchronized void setSslHost(String sslHost) {
        mySslHost = StringUtils.trimToNull(sslHost);
    }

    public synchronized int getSslPort() {
        return mySslPort;
    }

    public synchronized void setSslPort(int sslPort) {
        mySslPort = sslPort;
    }

    public synchronized String getMailHost() {
        return myMailHost;
    }

    public synchronized void setMailHost(String mailHost) {
        myMailHost = mailHost;
    }

    public synchronized String getMailLogin() {
        return myMailLogin;
    }

    public synchronized void setMailLogin(String mailLogin) {
        myMailLogin = mailLogin;
    }

    public synchronized String getMailPassword() {
        return myMailPassword;
    }

    public synchronized void setMailPassword(String mailPassword) {
        myMailPassword = mailPassword;
    }

    public synchronized int getMailPort() {
        return myMailPort;
    }

    public synchronized void setMailPort(int mailPort) {
        myMailPort = mailPort;
    }

    public synchronized SmtpProtocol getSmtpProtocol() {
        return mySmtpProtocol;
    }

    public synchronized void setSmtpProtocol(SmtpProtocol smtpProtocol) {
        mySmtpProtocol = smtpProtocol;
    }

    public synchronized String getMailSender() {
        return myMailSender;
    }

    public synchronized void setMailSender(String mailSender) {
        myMailSender = mailSender;
    }

    public synchronized String getAdminEmail() {
        return myAdminEmail;
    }

    public synchronized void setAdminEmail(String adminEmail) {
        myAdminEmail = adminEmail;
    }

    public synchronized boolean isNotifyOnPasswordChange() {
        return myNotifyOnPasswordChange;
    }

    public synchronized void setNotifyOnPasswordChange(boolean notifyOnPasswordChange) {
        myNotifyOnPasswordChange = notifyOnPasswordChange;
    }

    public synchronized boolean isNotifyOnEmailChange() {
        return myNotifyOnEmailChange;
    }

    public synchronized void setNotifyOnEmailChange(boolean notifyOnEmailChange) {
        myNotifyOnEmailChange = notifyOnEmailChange;
    }

    public synchronized boolean isNotifyOnQuotaExceeded() {
        return myNotifyOnQuotaExceeded;
    }

    public synchronized void setNotifyOnQuotaExceeded(boolean notifyOnQuotaExceeded) {
        myNotifyOnQuotaExceeded = notifyOnQuotaExceeded;
    }

    public synchronized boolean isNotifyOnLoginFailure() {
        return myNotifyOnLoginFailure;
    }

    public synchronized void setNotifyOnLoginFailure(boolean notifyOnLoginFailure) {
        myNotifyOnLoginFailure = notifyOnLoginFailure;
    }

    public synchronized boolean isNotifyOnWebUpload() {
        return myNotifyOnWebUpload;
    }

    public synchronized void setNotifyOnWebUpload(boolean notifyOnWebUpload) {
        myNotifyOnWebUpload = notifyOnWebUpload;
    }

    public synchronized boolean isNotifyOnTranscodingFailure() {
        return myNotifyOnTranscodingFailure;
    }

    public synchronized void setNotifyOnTranscodingFailure(boolean notifyOnTranscodingFailure) {
        myNotifyOnTranscodingFailure = notifyOnTranscodingFailure;
    }

    public synchronized boolean isNotifyOnInternalError() {
        return myNotifyOnInternalError;
    }

    public synchronized void setNotifyOnInternalError(boolean notifyOnInternalError) {
        myNotifyOnInternalError = notifyOnInternalError;
    }

    public synchronized boolean isNotifyOnDatabaseUpdate() {
        return myNotifyOnDatabaseUpdate;
    }

    public synchronized void setNotifyOnDatabaseUpdate(boolean notifyOnDatabaseUpdate) {
        myNotifyOnDatabaseUpdate = notifyOnDatabaseUpdate;
    }

    public synchronized boolean isNotifyOnMissingFile() {
        return myNotifyOnMissingFile;
    }

    public synchronized void setNotifyOnMissingFile(boolean notifyOnMissingFile) {
        myNotifyOnMissingFile = notifyOnMissingFile;
    }

    public synchronized boolean isNotifyOnOutdatedItunesXml() {
        return myNotifyOnOutdatedItunesXml;
    }

    public synchronized void setNotifyOnOutdatedItunesXml(boolean notifyOnOutdatedItunesXml) {
        myNotifyOnOutdatedItunesXml = notifyOnOutdatedItunesXml;
    }

    public synchronized boolean isNotifyOnSkippedDatabaseUpdate() {
        return myNotifyOnSkippedDatabaseUpdate;
    }

    public synchronized void setNotifyOnSkippedDatabaseUpdate(boolean notifyOnSkippedDatabaseUpdate) {
        myNotifyOnSkippedDatabaseUpdate = notifyOnSkippedDatabaseUpdate;
    }

    public synchronized int getStatisticKeepTime() {
        return myStatisticKeepTime;
    }

    public synchronized void setStatisticKeepTime(int statisticKeepTime) {
        myStatisticKeepTime = statisticKeepTime;
    }

    public synchronized Collection<TranscoderConfig> getTranscoderConfigs() {
        return myTranscoderConfigs != null ? new ArrayList<>(myTranscoderConfigs) : new ArrayList<TranscoderConfig>();
    }

    public Collection<TranscoderConfig> getEffectiveTranscoderConfigs() {
        Set<TranscoderConfig> mergedConfigs = new HashSet<>();
        mergedConfigs.addAll(getTranscoderConfigs());
        mergedConfigs.addAll(TranscoderConfig.getDefaultTranscoders());
        return mergedConfigs;
    }

    public synchronized void setTranscoderConfigs(Collection<TranscoderConfig> configs) {
        myTranscoderConfigs = configs != null ? new ArrayList<>(configs) : new ArrayList<TranscoderConfig>();
    }

    public synchronized List<ExternalSiteDefinition> getExternalSites() {
        return new ArrayList<>(myExternalSites);
    }

    public synchronized List<ExternalSiteDefinition> getExternalSites(String type) {
        List<ExternalSiteDefinition> result = new ArrayList<>();
        for (ExternalSiteDefinition def : myExternalSites) {
            if (StringUtils.equals(type, def.getType())) {
                result.add(def);
            }
        }
        return result;
    }

    public synchronized void addExternalSite(ExternalSiteDefinition definition) {
        myExternalSites.add(definition);
    }

    public synchronized void removeExternalSite(ExternalSiteDefinition definition) {
        for (Iterator<ExternalSiteDefinition> iter = myExternalSites.iterator(); iter.hasNext();) {
            if (definition.equals(iter.next())) {
                iter.remove();
                break;
            }
        }
    }

    public synchronized String getAutoLogin() {
        return myAutoLogin;
    }

    public synchronized void setAutoLogin(String autoLogin) {
        myAutoLogin = autoLogin;
    }

    public synchronized boolean isDisableBrowser() {
        return myDisableBrowser;
    }

    public synchronized void setDisableBrowser(boolean disableBrowser) {
        myDisableBrowser = disableBrowser;
    }

    public synchronized boolean isServerBrowserActive() {
        return myServerBrowserActive;
    }

    public synchronized void setServerBrowserActive(boolean serverBrowserActive) {
        myServerBrowserActive = serverBrowserActive;
    }

    public synchronized boolean isOpenIdActive() {
        return myOpenIdActive;
    }

    public synchronized void setOpenIdActive(boolean openIdActive) {
        myOpenIdActive = openIdActive;
    }

    public synchronized boolean isDisableWebLogin() {
        return myDisableWebLogin;
    }

    public synchronized void setDisableWebLogin(boolean disableWebLogin) {
        myDisableWebLogin = disableWebLogin;
    }

    public synchronized LdapConfig getLdapConfig() {
        return myLdapConfig;
    }

    public synchronized boolean isAdminPassword() {
        return !Arrays.equals(MyTunesRss.CONFIG.getAdminPasswordHash(), MyTunesRss.SHA1_DIGEST.get().digest(new byte[0]));
    }

    public synchronized byte[] getAdminPasswordHash() {
        return myAdminPasswordHash != null ? myAdminPasswordHash.clone() : MyTunesRss.SHA1_DIGEST.get().digest(MiscUtils.getUtf8Bytes(""));
    }

    public synchronized void setAdminPasswordHash(byte[] adminPasswordHash) {
        myAdminPasswordHash = adminPasswordHash != null ? adminPasswordHash.clone() : null;
    }

    public synchronized String getAdminHost() {
        return myAdminHost;
    }

    public synchronized void setAdminHost(String adminHost) {
        myAdminHost = StringUtils.trimToNull(adminHost);
    }

    public synchronized int getAdminPort() {
        return myAdminPort;
    }

    public synchronized void setAdminPort(int adminPort) {
        myAdminPort = adminPort;
    }

    public synchronized Set<FlashPlayerConfig> getFlashPlayers() {
        return new HashSet<>(myFlashPlayers);
    }

    public synchronized boolean isFlashPlayer() {
        return myFlashPlayers != null && !myFlashPlayers.isEmpty();
    }

    public synchronized FlashPlayerConfig getFlashPlayer(String id) {
        for (FlashPlayerConfig flashPlayer : myFlashPlayers) {
            if (flashPlayer.getId().equals(id)) {
                return flashPlayer;
            }
        }
        return null;
    }

    public synchronized void addFlashPlayer(FlashPlayerConfig flashPlayer) {
        myFlashPlayers.add(flashPlayer);
    }

    public synchronized FlashPlayerConfig removeFlashPlayer(String id) {
        FlashPlayerConfig config = getFlashPlayer(id);
        if (config != null) {
            myFlashPlayers.remove(new FlashPlayerConfig(id, null, null, 0, 0, TimeUnit.SECONDS, 0));
        }
        return config;
    }

    public synchronized boolean isInitialWizard() {
        return myInitialWizard;
    }

    public synchronized void setInitialWizard(boolean initialWizard) {
        myInitialWizard = initialWizard;
    }

    public synchronized boolean isUpnpAdmin() {
        return myUpnpAdmin;
    }

    public synchronized void setUpnpAdmin(boolean upnpAdmin) {
        myUpnpAdmin = upnpAdmin;
    }

    public synchronized boolean isUpnpUserHttp() {
        return myUpnpUserHttp;
    }

    public synchronized void setUpnpUserHttp(boolean upnpUserHttp) {
        myUpnpUserHttp = upnpUserHttp;
    }

    public synchronized boolean isUpnpUserHttps() {
        return myUpnpUserHttps;
    }

    public synchronized void setUpnpUserHttps(boolean upnpUserHttps) {
        myUpnpUserHttps = upnpUserHttps;
    }

    public synchronized String getSelfRegisterTemplateUser() {
        return mySelfRegisterTemplateUser;
    }

    public synchronized void setSelfRegisterTemplateUser(String selfRegisterTemplateUser) {
        mySelfRegisterTemplateUser = selfRegisterTemplateUser;
    }

    public synchronized boolean isSelfRegAdminEmail() {
        return mySelfRegAdminEmail;
    }

    public synchronized void setSelfRegAdminEmail(boolean selfRegAdminEmail) {
        mySelfRegAdminEmail = selfRegAdminEmail;
    }

    public synchronized String getDefaultUserInterfaceTheme() {
        return myDefaultUserInterfaceTheme;
    }

    public synchronized void setDefaultUserInterfaceTheme(String defaultUserInterfaceTheme) {
        myDefaultUserInterfaceTheme = defaultUserInterfaceTheme;
    }

    public synchronized String getFacebookApiKey() {
        return myFacebookApiKey;
    }

    public synchronized void setFacebookApiKey(String facebookApiKey) {
        myFacebookApiKey = facebookApiKey;
    }

    public synchronized int getNumberKeepDatabaseBackups() {
        return myNumberKeepDatabaseBackups;
    }

    public synchronized void setNumberKeepDatabaseBackups(int numberKeepDatabaseBackups) {
        myNumberKeepDatabaseBackups = numberKeepDatabaseBackups;
    }

    public synchronized boolean isBackupDatabaseAfterInit() {
        return myBackupDatabaseAfterInit;
    }

    public synchronized void setBackupDatabaseAfterInit(boolean backupDatabaseAfterInit) {
        myBackupDatabaseAfterInit = backupDatabaseAfterInit;
    }

    public synchronized boolean isHeadless() {
        return myHeadless;
    }

    public synchronized void setHeadless(boolean headless) {
        myHeadless = headless;
    }

    public synchronized File getVlcExecutable() {
        return myVlcExecutable;
    }

    public synchronized void setVlcExecutable(File vlcExecutable) {
        myVlcExecutable = vlcExecutable;
    }

    public synchronized File getGmExecutable() {
        return myGmExecutable;
    }

    public synchronized void setGmExecutable(File gmExecutable) {
        myGmExecutable = gmExecutable;
    }

    public synchronized boolean isGmEnabled() {
        return myGmEnabled;
    }

    public synchronized void setGmEnabled(boolean gmEnabled) {
        myGmEnabled = gmEnabled;
    }

    public synchronized boolean isVlcEnabled() {
        return myVlcEnabled;
    }

    public synchronized void setVlcEnabled(boolean vlcEnabled) {
        myVlcEnabled = vlcEnabled;
    }

    public synchronized long getImageExpirationMillis() {
        return myImageExpirationMillis;
    }

    public synchronized void setImageExpirationMillis(long imageExpirationMillis) {
        myImageExpirationMillis = imageExpirationMillis;
    }

    public synchronized long getRestApiJsExpirationMillis() {
        return myRestApiJsExpirationMillis;
    }

    public synchronized void setRestApiJsExpirationMillis(long restApiJsExpirationMillis) {
        myRestApiJsExpirationMillis = restApiJsExpirationMillis;
    }

    public synchronized int getJpegQuality() {
        return myJpegQuality;
    }

    public synchronized void setJpegQuality(int jpegQuality) {
        myJpegQuality = jpegQuality;
    }

    public synchronized int getOnDemandThumbnailGenerationThreads() {
        return myOnDemandThumbnailGenerationThreads;
    }

    public synchronized void setOnDemandThumbnailGenerationThreads(int onDemandThumbnailGenerationThreads) {
        myOnDemandThumbnailGenerationThreads = onDemandThumbnailGenerationThreads;
        MyTunesRss.EXECUTOR_SERVICE.setOnDemandThumbnailGeneratorThreads();
    }

    public synchronized int getOnDemandThumbnailGenerationTimeoutSeconds() {
        return myOnDemandThumbnailGenerationTimeoutSeconds;
    }

    public synchronized void setOnDemandThumbnailGenerationTimeoutSeconds(int onDemandThumbnailGenerationTimeoutSeconds) {
        myOnDemandThumbnailGenerationTimeoutSeconds = onDemandThumbnailGenerationTimeoutSeconds;
    }

    public synchronized String getAccessLogTz() {
        return myAccessLogTz;
    }

    public synchronized void setAccessLogTz(String accessLogTz) {
        myAccessLogTz = accessLogTz;
    }

    public synchronized Map<String, String> getGenreMappings() {
        return new HashMap<>(myGenreMappings);
    }

    public synchronized String getGenreMapping(String fromGenre) {
        return myGenreMappings.get(fromGenre);
    }

    public synchronized void clearGenreMappings() {
        myGenreMappings.clear();
    }

    public synchronized void addGenreMapping(String fromGenre, String toGenre) {
        myGenreMappings.put(fromGenre, toGenre);
    }

    public Map<String, String> getCustomWebHeaders() {
        return myCustomWebHeaders;
    }

    public void setCustomWebHeaders(Map<String, String> customWebHeaders) {
        myCustomWebHeaders = customWebHeaders;
    }

    public synchronized boolean isAdminAccessLogExtended() {
        return myAdminAccessLogExtended;
    }

    public synchronized void setAdminAccessLogExtended(boolean adminAccessLogExtended) {
        myAdminAccessLogExtended = adminAccessLogExtended;
    }

    public synchronized boolean isUserAccessLogExtended() {
        return myUserAccessLogExtended;
    }

    public synchronized void setUserAccessLogExtended(boolean userAccessLogExtended) {
        myUserAccessLogExtended = userAccessLogExtended;
    }

    public synchronized int getAdminAccessLogRetainDays() {
        return myAdminAccessLogRetainDays;
    }

    public synchronized void setAdminAccessLogRetainDays(int adminAccessLogRetainDays) {
        myAdminAccessLogRetainDays = adminAccessLogRetainDays;
    }

    public synchronized int getUserAccessLogRetainDays() {
        return myUserAccessLogRetainDays;
    }

    public synchronized void setUserAccessLogRetainDays(int userAccessLogRetainDays) {
        myUserAccessLogRetainDays = userAccessLogRetainDays;
    }

    public boolean isUpnpMediaServerActive() {
        return myUpnpMediaServerActive;
    }

    public void setUpnpMediaServerActive(boolean upnpMediaServerActive) {
        myUpnpMediaServerActive = upnpMediaServerActive;
    }

    public String getUpnpMediaServerName() {
        return myUpnpMediaServerName;
    }

    public void setUpnpMediaServerName(String upnpMediaServerName) {
        myUpnpMediaServerName = upnpMediaServerName;
    }

    public int getUpnpMediaServerLockTimeoutSeconds() {
        return myUpnpMediaServerLockTimeoutSeconds;
    }

    public void setUpnpMediaServerLockTimeoutSeconds(int upnpMediaServerLockTimeoutSeconds) {
        myUpnpMediaServerLockTimeoutSeconds = upnpMediaServerLockTimeoutSeconds;
    }

    public synchronized void load() {
        try {
            File file = getSettingsFile();
            LOGGER.info("Loading configuration from \"" + file.getAbsolutePath() + "\".");
            if (!file.isFile()) {
                FileUtils.writeStringToFile(file,
                        "<settings></settings>");
            }
            JXPathContext settings = JXPathUtils.getContext(JXPathUtils.getContext(file.toURI().toURL()), "settings");
            setVersion(StringUtils.defaultIfEmpty(JXPathUtils.getStringValue(settings, "version", "0"), "0"));
            Version currentAppVersion = new Version(MyTunesRss.VERSION);
            Version currentConfigVersion = new Version(getVersion());
            Version minimumChecksumVersion = new Version("3.6.2");
            loadFromContext(settings);
            if (currentConfigVersion.compareTo(currentAppVersion) < 0) {
                migrate(currentConfigVersion);
            }
        } catch (IOException e) {
            LOGGER.error("Could not read configuration file.", e);
        }
    }

    public synchronized void loadFromContext(JXPathContext settings) {
        setVersion(MyTunesRss.VERSION);
        load(settings);

    }

    private void load(JXPathContext settings) {
        setAdminPasswordHash(JXPathUtils.getByteArray(settings, "adminPassword", getAdminPasswordHash()));
        setAdminHost(JXPathUtils.getStringValue(settings, "adminHost", getAdminHost()));
        setAdminPort(JXPathUtils.getIntValue(settings, "adminPort", getAdminPort()));
        setHost(JXPathUtils.getStringValue(settings, "serverHost", getHost()));
        setPort(JXPathUtils.getIntValue(settings, "serverPort", getPort()));
        setServerName(JXPathUtils.getStringValue(settings, "serverName", getServerName()));
        setAvailableOnLocalNet(JXPathUtils.getBooleanValue(settings, "availableOnLocalNet", isAvailableOnLocalNet()));
        setCheckUpdateOnStart(JXPathUtils.getBooleanValue(settings, "checkUpdateOnStart", isCheckUpdateOnStart()));
        readDataSources(settings);
        setLocalTempArchive(JXPathUtils.getBooleanValue(settings, "localTempArchive", isLocalTempArchive()));
        setProxyHost(JXPathUtils.getStringValue(settings, "proxyHost", getProxyHost()));
        setProxyPort(JXPathUtils.getIntValue(settings, "proxyPort", getProxyPort()));
        setWebWelcomeMessage(JXPathUtils.getStringValue(settings, "webWelcomeMessage", getWebWelcomeMessage()));
        setWebLoginMessage(JXPathUtils.getStringValue(settings, "webLoginMessage", getWebLoginMessage()));
        readPathInfoEncryptionKey(settings);
        setTranscodingCacheMaxGiB(JXPathUtils.getLongValue(settings, "transcodingCacheMaxGiB", getTranscodingCacheMaxGiB()));
        setTempMaxGiB(JXPathUtils.getLongValue(settings, "tempMaxGiB", getTempMaxGiB()));
        setHttpLiveStreamCacheMaxGiB(JXPathUtils.getLongValue(settings, "httpLiveStreamCacheMaxGiB", getHttpLiveStreamCacheMaxGiB()));
        setCodewaveLogLevel(Level.toLevel(JXPathUtils.getStringValue(settings, "codewaveLogLevel", Level.INFO.toString()).toUpperCase()));
        setLastNewVersionInfo(JXPathUtils.getStringValue(settings, "lastNewVersionInfo", "0"));
        setUpdateIgnoreVersion(JXPathUtils.getStringValue(settings, "updateIgnoreVersion", MyTunesRss.VERSION));
        Iterator<JXPathContext> updateTriggerIterator = JXPathUtils.getContextIterator(settings, "crontriggers/database");
        myDatabaseUpdateTriggers = new ArrayList<>();
        while (updateTriggerIterator.hasNext()) {
            myDatabaseUpdateTriggers.add(JXPathUtils.getStringValue(updateTriggerIterator.next(), ".", ""));
        }
        Iterator<JXPathContext> backupTriggerIterator = JXPathUtils.getContextIterator(settings, "crontriggers/database-backup");
        myDatabaseBackupTriggers = new ArrayList<>();
        while (backupTriggerIterator.hasNext()) {
            myDatabaseBackupTriggers.add(JXPathUtils.getStringValue(backupTriggerIterator.next(), ".", ""));
        }
        loadDatabaseSettings(settings);
        setTomcatMaxThreads(JXPathUtils.getStringValue(settings, "tomcat/max-threads", "200"));
        String context = StringUtils.trimToNull(StringUtils.strip(JXPathUtils.getStringValue(settings, "tomcat/webapp-context", ""), "/"));
        setWebappContext(context != null ? "/" + context : "");
        setSslKeystoreFile(JXPathUtils.getStringValue(settings, "ssl/keystore/file", null));
        setSslKeystoreKeyAlias(JXPathUtils.getStringValue(settings, "ssl/keystore/keyalias", null));
        setSslKeystorePass(JXPathUtils.getStringValue(settings, "ssl/keystore/pass", null));
        setSslHost(JXPathUtils.getStringValue(settings, "ssl/host", getSslHost()));
        setSslPort(JXPathUtils.getIntValue(settings, "ssl/port", 0));
        setMailHost(JXPathUtils.getStringValue(settings, "mail-host", null));
        setMailPort(JXPathUtils.getIntValue(settings, "mail-port", -1));
        setSmtpProtocol(SmtpProtocol.valueOf(JXPathUtils.getStringValue(settings, "smtp-protocol", SmtpProtocol.PLAINTEXT.name())));
        setMailLogin(JXPathUtils.getStringValue(settings, "mail-login", null));
        setMailPassword(JXPathUtils.getStringValue(settings, "mail-password", null));
        setMailSender(JXPathUtils.getStringValue(settings, "mail-sender", null));
        setAdminEmail(JXPathUtils.getStringValue(settings, "admin-email", null));
        setNotifyOnDatabaseUpdate(JXPathUtils.getBooleanValue(settings, "admin-notify/database-update", false));
        setNotifyOnEmailChange(JXPathUtils.getBooleanValue(settings, "admin-notify/email-change", false));
        setNotifyOnInternalError(JXPathUtils.getBooleanValue(settings, "admin-notify/internal-error", false));
        setNotifyOnLoginFailure(JXPathUtils.getBooleanValue(settings, "admin-notify/login-failure", false));
        setNotifyOnPasswordChange(JXPathUtils.getBooleanValue(settings, "admin-notify/password-change", false));
        setNotifyOnQuotaExceeded(JXPathUtils.getBooleanValue(settings, "admin-notify/quota-exceeded", false));
        setNotifyOnTranscodingFailure(JXPathUtils.getBooleanValue(settings, "admin-notify/transcoding-failure", false));
        setNotifyOnWebUpload(JXPathUtils.getBooleanValue(settings, "admin-notify/web-upload", false));
        setNotifyOnMissingFile(JXPathUtils.getBooleanValue(settings, "admin-notify/missing-file", false));
        setNotifyOnOutdatedItunesXml(JXPathUtils.getBooleanValue(settings, "admin-notify/outdated-itunesxml", false));
        setNotifyOnSkippedDatabaseUpdate(JXPathUtils.getBooleanValue(settings, "admin-notify/skipped-db-update", false));
        try {
            setStatisticKeepTime(JXPathUtils.getIntValue(settings, "statistics-keep-time", getStatisticKeepTime()));
        } catch (Exception ignored) {
            LOGGER.warn("Could not read/parse statistics keep time, keeping default.");
            // intentionally left blank; keep default
        }
        Iterator<JXPathContext> transcoderConfigIterator = JXPathUtils.getContextIterator(settings, "transcoders/config");
        myTranscoderConfigs = new ArrayList<>();
        while (transcoderConfigIterator.hasNext()) {
            JXPathContext transcoderConfigContext = transcoderConfigIterator.next();
            myTranscoderConfigs.add(new TranscoderConfig(transcoderConfigContext));
        }
        Iterator<JXPathContext> externalSitesIterator = JXPathUtils.getContextIterator(settings, "external-sites/site");
        myExternalSites = new ArrayList<>();
        while (externalSitesIterator.hasNext()) {
            JXPathContext externalSiteContext = externalSitesIterator.next();
            String name = JXPathUtils.getStringValue(externalSiteContext, "name", null);
            String url = JXPathUtils.getStringValue(externalSiteContext, "url", null);
            String type = JXPathUtils.getStringValue(externalSiteContext, "type", null);
            myExternalSites.add(new ExternalSiteDefinition(type, name, url));
        }
        setServerBrowserActive(JXPathUtils.getBooleanValue(settings, "serverBrowserActive", true));
        setOpenIdActive(JXPathUtils.getBooleanValue(settings, "openIdActive", true));
        setAutoLogin(JXPathUtils.getStringValue(settings, "autoLogin", null));
        setDisableBrowser(JXPathUtils.getBooleanValue(settings, "disableBrowser", false));
        setDisableWebLogin(JXPathUtils.getBooleanValue(settings, "disableWebLogin", false));
        myLdapConfig = new LdapConfig(settings);
        Iterator<JXPathContext> users = JXPathUtils.getContextIterator(settings, "users/user");
        while (users != null && users.hasNext()) {
            JXPathContext userContext = users.next();
            User user = new User(JXPathUtils.getStringValue(userContext, "name", null));
            user.loadFromPreferences(userContext);
            addUser(user);
        }
        markGroupUsers();
        myFlashPlayers.clear();
        Iterator<JXPathContext> flashPlayerIterator = JXPathUtils.getContextIterator(settings, "flash-players/player");
        while (flashPlayerIterator.hasNext()) {
            JXPathContext flashPlayerContext = flashPlayerIterator.next();
            FlashPlayerConfig flashPlayerConfig = new FlashPlayerConfig(
                    JXPathUtils.getStringValue(flashPlayerContext, "id", UUID.randomUUID().toString()),
                    JXPathUtils.getStringValue(flashPlayerContext, "name", "Unknown Flash Player"),
                    PlaylistFileType.valueOf(JXPathUtils.getStringValue(flashPlayerContext, "filetype", PlaylistFileType.Xspf.name())),
                    JXPathUtils.getIntValue(flashPlayerContext, "width", 600),
                    JXPathUtils.getIntValue(flashPlayerContext, "height", 276),
                    TimeUnit.valueOf(JXPathUtils.getStringValue(flashPlayerContext, "timeunit", TimeUnit.SECONDS.name())),
                    JXPathUtils.getIntValue(flashPlayerContext, "imageSize", 256)
            );
            addFlashPlayer(flashPlayerConfig);
        }
        setInitialWizard(JXPathUtils.getBooleanValue(settings, "initialWizard", true));
        setUpnpAdmin(JXPathUtils.getBooleanValue(settings, "upnp-admin", false));
        setUpnpUserHttp(JXPathUtils.getBooleanValue(settings, "upnp-user-http", true));
        setUpnpUserHttps(JXPathUtils.getBooleanValue(settings, "upnp-user-https", true));
        setSelfRegisterTemplateUser(JXPathUtils.getStringValue(settings, "selfreg-template-user", null));
        setSelfRegAdminEmail(JXPathUtils.getBooleanValue(settings, "selfreg-admin-email", true));
        setDefaultUserInterfaceTheme(JXPathUtils.getStringValue(settings, "default-ui-theme", null));
        //setFacebookApiKey(JXPathUtils.getStringValue(settings, "facebook-api-key", null));
        setNumberKeepDatabaseBackups(JXPathUtils.getIntValue(settings, "database-backup-max", 5));
        setBackupDatabaseAfterInit(JXPathUtils.getBooleanValue(settings, "database-backup-after-init", true));
        setHeadless(JXPathUtils.getBooleanValue(settings, "headless", false));
        String vlc = JXPathUtils.getStringValue(settings, "vlc", MyTunesRssUtils.findVlcExecutable());
        setVlcExecutable(vlc != null ? new File(vlc) : null);
        setVlcEnabled(JXPathUtils.getBooleanValue(settings, "vlc-enabled", true));
        String gm = JXPathUtils.getStringValue(settings, "gm", MyTunesRssUtils.findGraphicsMagickExecutable());
        setGmExecutable(gm != null ? new File(gm) : null);
        setGmEnabled(JXPathUtils.getBooleanValue(settings, "gm-enabled", true));
        setImageExpirationMillis(JXPathUtils.getLongValue(settings, "image-expiration-millis", 1000 * 3600 * 48)); // default to 48 hours
        setRestApiJsExpirationMillis(JXPathUtils.getLongValue(settings, "restapijs-expiration-millis", 1000 * 3600 * 1)); // default to 1 hour
        setJpegQuality(JXPathUtils.getIntValue(settings, "jpeg-quality", 80));
        setOnDemandThumbnailGenerationThreads(JXPathUtils.getIntValue(settings, "on-demand-thumbnail-threads", 5));
        setOnDemandThumbnailGenerationTimeoutSeconds(JXPathUtils.getIntValue(settings, "on-demand-thumbnail-timoeut-seconds", 60));
        setAccessLogTz(JXPathUtils.getStringValue(settings, "accesslog-tz", "GMT"));
        setUserAccessLogRetainDays(JXPathUtils.getIntValue(settings, "accesslog-user-retain", 5));
        setAdminAccessLogRetainDays(JXPathUtils.getIntValue(settings, "accesslog-admin-retain", 5));
        setUserAccessLogExtended(JXPathUtils.getBooleanValue(settings, "accesslog-user-ext", true));
        setAdminAccessLogExtended(JXPathUtils.getBooleanValue(settings, "accesslog-admin-ext", true));
        clearGenreMappings();
        Iterator<JXPathContext> genreMappingsIterator = JXPathUtils.getContextIterator(settings, "genre-mappings/mapping");
        while (genreMappingsIterator.hasNext()) {
            JXPathContext genreMappingContext = genreMappingsIterator.next();
            addGenreMapping(JXPathUtils.getStringValue(genreMappingContext, "from", ""), JXPathUtils.getStringValue(genreMappingContext, "to", ""));
        }
        myGenreMappings.remove(""); // if the default was used for any 'from' key
        myCustomWebHeaders.clear();
        Iterator<JXPathContext> customWebHeadersIterator = JXPathUtils.getContextIterator(settings, "custom-web-headers/header");
        while (customWebHeadersIterator.hasNext()) {
            JXPathContext customWebHeadersContext = customWebHeadersIterator.next();
            myCustomWebHeaders.put(JXPathUtils.getStringValue(customWebHeadersContext, "name", ""), JXPathUtils.getStringValue(customWebHeadersContext, "value", ""));
        }
        myCustomWebHeaders.remove(""); // if the default was used for any 'name' key
        setUpnpMediaServerActive(JXPathUtils.getBooleanValue(settings, "upnp-server-active", true));
        setUpnpMediaServerName(JXPathUtils.getStringValue(settings, "upnp-server-name", null));
        setUpnpMediaServerLockTimeoutSeconds(JXPathUtils.getIntValue(settings, "upnp-server-timeout", 10));
    }

    /**
     * Mark all groups users as groups
     */
    private void markGroupUsers() {
        // mark them
        for (User userToCheck : myUsers) {
            for (User user : myUsers) {
                if (userToCheck.equals(user.getParent())) {
                    userToCheck.setGroup(true);
                    break;
                }
            }
        }
        // and flatten them
        for (User user : myUsers) {
            if (user.isGroup()) {
                user.setParent(null);
            }
        }
    }

    private void readDataSources(JXPathContext settings) {
        List<DatasourceConfig> dataSources = new ArrayList<>();
        Iterator<JXPathContext> contextIterator = JXPathUtils.getContextIterator(settings, "datasources/datasource");
        while (contextIterator.hasNext()) {
            JXPathContext datasourceContext = contextIterator.next();
            if (JXPathUtils.getStringValue(datasourceContext, "type", null) == null) {
                // read pre-4.0.0-EAP-6 data source definitions
                String definition = JXPathUtils.getStringValue(datasourceContext, ".", null);
                if (definition != null) {
                    dataSources.add(DatasourceConfig.create(UUID.randomUUID().toString(), null, definition));
                }
            } else {
                DatasourceType type = DatasourceType.valueOf(JXPathUtils.getStringValue(datasourceContext, "type", DatasourceType.Itunes.name()));
                String id = JXPathUtils.getStringValue(datasourceContext, "id", UUID.randomUUID().toString());
                String definition = JXPathUtils.getStringValue(datasourceContext, "definition", "");
                String name = JXPathUtils.getStringValue(datasourceContext, "name", null);
                switch (type) {
                    case Watchfolder:
                        WatchfolderDatasourceConfig watchfolderDatasourceConfig = new WatchfolderDatasourceConfig(id, name, definition);
                        watchfolderDatasourceConfig.setMinFileSize(JXPathUtils.getLongValue(datasourceContext, "minFileSize", 0));
                        watchfolderDatasourceConfig.setMaxFileSize(JXPathUtils.getLongValue(datasourceContext, "maxFileSize", 0));
                        watchfolderDatasourceConfig.setIncludePattern(JXPathUtils.getStringValue(datasourceContext, "include", null));
                        watchfolderDatasourceConfig.setExcludePattern(JXPathUtils.getStringValue(datasourceContext, "exclude", null));
                        watchfolderDatasourceConfig.setTitleFallback(JXPathUtils.getStringValue(datasourceContext, "titleFallback", WatchfolderDatasourceConfig.DEFAULT_TITLE_FALLBACK));
                        watchfolderDatasourceConfig.setAlbumFallback(JXPathUtils.getStringValue(datasourceContext, "albumFallback", WatchfolderDatasourceConfig.DEFAULT_ALBUM_FALLBACK));
                        watchfolderDatasourceConfig.setArtistFallback(JXPathUtils.getStringValue(datasourceContext, "artistFallback", WatchfolderDatasourceConfig.DEFAULT_ARTIST_FALLBACK));
                        watchfolderDatasourceConfig.setSeriesFallback(JXPathUtils.getStringValue(datasourceContext, "seriesFallback", WatchfolderDatasourceConfig.DEFAULT_SERIES_FALLBACK));
                        watchfolderDatasourceConfig.setSeasonFallback(JXPathUtils.getStringValue(datasourceContext, "seasonFallback", WatchfolderDatasourceConfig.DEFAULT_SEASON_FALLBACK));
                        watchfolderDatasourceConfig.setEpisodeFallback(JXPathUtils.getStringValue(datasourceContext, "episodeFallback", WatchfolderDatasourceConfig.DEFAULT_EPISODE_FALLBACK));
                        watchfolderDatasourceConfig.setVideoType(VideoType.valueOf(JXPathUtils.getStringValue(datasourceContext, "videoType", VideoType.Movie.name())));
                        watchfolderDatasourceConfig.setPhotoAlbumPattern(JXPathUtils.getStringValue(datasourceContext, "photoAlbumPattern", WatchfolderDatasourceConfig.DEFAULT_PHOTO_ALBUM_PATTERN));
                        watchfolderDatasourceConfig.setIgnoreFileMeta(JXPathUtils.getBooleanValue(datasourceContext, "ignoreFileMeta", false));
                        watchfolderDatasourceConfig.setArtistDropWords(JXPathUtils.getStringValue(datasourceContext, "artistDropwords", ""));
                        watchfolderDatasourceConfig.setId3v2TrackComment(JXPathUtils.getStringValue(datasourceContext, "id3v2-track-comment", ""));
                        watchfolderDatasourceConfig.setDisabledMp4Codecs(JXPathUtils.getStringValue(datasourceContext, "disabled-mp4-codecs", ""));
                        watchfolderDatasourceConfig.setTrackImagePatterns(readTrackImagePatterns(datasourceContext));
                        watchfolderDatasourceConfig.setTrackImageImportType(ImageImportType.valueOf(JXPathUtils.getStringValue(datasourceContext, "track-image-import", ImageImportType.Auto.name())));
                        watchfolderDatasourceConfig.setPhotoThumbnailImportType(ImageImportType.valueOf(JXPathUtils.getStringValue(datasourceContext, "photo-thumbnail-import", ImageImportType.OnDemand.name())));
                        watchfolderDatasourceConfig.setLastUpdate(JXPathUtils.getLongValue(datasourceContext, "last-update", 0));
                        watchfolderDatasourceConfig.setUpload(JXPathUtils.getBooleanValue(datasourceContext, "upload", false));
                        watchfolderDatasourceConfig.setUseSingleImageInFolder(JXPathUtils.getBooleanValue(datasourceContext, "use-single-image", false));
                        watchfolderDatasourceConfig.setImportPlaylists(JXPathUtils.getBooleanValue(datasourceContext, "import-playlists", true));
                        dataSources.add(watchfolderDatasourceConfig);
                        break;
                    case Itunes:
                        ItunesDatasourceConfig itunesDatasourceConfig = new ItunesDatasourceConfig(id, name, definition);
                        Iterator<JXPathContext> pathReplacementsIterator = JXPathUtils.getContextIterator(datasourceContext, "path-replacements/replacement");
                        itunesDatasourceConfig.clearPathReplacements();
                        while (pathReplacementsIterator.hasNext()) {
                            JXPathContext pathReplacementContext = pathReplacementsIterator.next();
                            String search = JXPathUtils.getStringValue(pathReplacementContext, "search", null);
                            String replacement = JXPathUtils.getStringValue(pathReplacementContext, "replacement", null);
                            itunesDatasourceConfig.addPathReplacement(new ReplacementRule(search, replacement));
                        }
                        Iterator<JXPathContext> ignorePlaylistsIterator = JXPathUtils.getContextIterator(datasourceContext, "ignore-playlists/type");
                        itunesDatasourceConfig.clearIgnorePlaylists();
                        while (ignorePlaylistsIterator.hasNext()) {
                            JXPathContext ignorePlaylistsContext = ignorePlaylistsIterator.next();
                            try {
                                itunesDatasourceConfig.addIgnorePlaylist(ItunesPlaylistType.valueOf(JXPathUtils.getStringValue(ignorePlaylistsContext, ".", ItunesPlaylistType.Master.name())));
                            } catch (IllegalArgumentException ignored) {
                                // ignore illegal config entry
                            }
                        }
                        itunesDatasourceConfig.setArtistDropWords(JXPathUtils.getStringValue(datasourceContext, "artistDropwords", ""));
                        itunesDatasourceConfig.setDisabledMp4Codecs(JXPathUtils.getStringValue(datasourceContext, "disabled-mp4-codecs", ""));
                        itunesDatasourceConfig.setTrackImagePatterns(readTrackImagePatterns(datasourceContext));
                        itunesDatasourceConfig.setTrackImageImportType(ImageImportType.valueOf(JXPathUtils.getStringValue(datasourceContext, "track-image-import", ImageImportType.Auto.name())));
                        itunesDatasourceConfig.setLastUpdate(JXPathUtils.getLongValue(datasourceContext, "last-update", 0));
                        itunesDatasourceConfig.setUpload(JXPathUtils.getBooleanValue(datasourceContext, "upload", false));
                        itunesDatasourceConfig.setUseSingleImageInFolder(JXPathUtils.getBooleanValue(datasourceContext, "use-single-image", false));
                        dataSources.add(itunesDatasourceConfig);
                        break;
                    case Iphoto:
                        IphotoDatasourceConfig iphotoDatasourceConfig = new IphotoDatasourceConfig(id, name, definition);
                        pathReplacementsIterator = JXPathUtils.getContextIterator(datasourceContext, "path-replacements/replacement");
                        iphotoDatasourceConfig.clearPathReplacements();
                        while (pathReplacementsIterator.hasNext()) {
                            JXPathContext pathReplacementContext = pathReplacementsIterator.next();
                            String search = JXPathUtils.getStringValue(pathReplacementContext, "search", null);
                            String replacement = JXPathUtils.getStringValue(pathReplacementContext, "replacement", null);
                            iphotoDatasourceConfig.addPathReplacement(new ReplacementRule(search, replacement));
                        }
                        iphotoDatasourceConfig.setImportRolls(JXPathUtils.getBooleanValue(datasourceContext, "importRolls", true));
                        iphotoDatasourceConfig.setImportAlbums(JXPathUtils.getBooleanValue(datasourceContext, "importAlbums", true));
                        iphotoDatasourceConfig.setPhotoThumbnailImportType(ImageImportType.valueOf(JXPathUtils.getStringValue(datasourceContext, "photo-thumbnail-import", ImageImportType.OnDemand.name())));
                        iphotoDatasourceConfig.setLastUpdate(JXPathUtils.getLongValue(datasourceContext, "last-update", 0));
                        dataSources.add(iphotoDatasourceConfig);
                        break;
                    case Aperture:
                        ApertureDatasourceConfig apertureDatasourceConfig = new ApertureDatasourceConfig(id, name, definition);
                        pathReplacementsIterator = JXPathUtils.getContextIterator(datasourceContext, "path-replacements/replacement");
                        apertureDatasourceConfig.clearPathReplacements();
                        while (pathReplacementsIterator.hasNext()) {
                            JXPathContext pathReplacementContext = pathReplacementsIterator.next();
                            String search = JXPathUtils.getStringValue(pathReplacementContext, "search", null);
                            String replacement = JXPathUtils.getStringValue(pathReplacementContext, "replacement", null);
                            apertureDatasourceConfig.addPathReplacement(new ReplacementRule(search, replacement));
                        }
                        apertureDatasourceConfig.setPhotoThumbnailImportType(ImageImportType.valueOf(JXPathUtils.getStringValue(datasourceContext, "photo-thumbnail-import", ImageImportType.OnDemand.name())));
                        apertureDatasourceConfig.setLastUpdate(JXPathUtils.getLongValue(datasourceContext, "last-update", 0));
                        dataSources.add(apertureDatasourceConfig);
                        break;
                    default:
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Illegal data source of type \"" + JXPathUtils.getStringValue(datasourceContext, "type", DatasourceType.Itunes.name()) + "\" ignored.");
                        }
                }
            }
        }
        setDatasources(dataSources);
    }

    private List<String> readTrackImagePatterns(JXPathContext settings) {
        List<String> patterns = new ArrayList<>();
        Iterator<JXPathContext> trackImageMappingIterator = JXPathUtils.getContextIterator(settings, "track-image-patterns/pattern");
        while (trackImageMappingIterator.hasNext()) {
            JXPathContext mappingContext = trackImageMappingIterator.next();
            patterns.add(JXPathUtils.getStringValue(mappingContext, ".", null));
        }
        return patterns;
    }

    private void loadDatabaseSettings(JXPathContext settings) {
        DatabaseType databaseType = DatabaseType.valueOf(JXPathUtils.getStringValue(settings, "database/type", DatabaseType.h2.name()));
        setDatabaseType(databaseType);
        setDefaultDatabaseSettings();
        if (databaseType != DatabaseType.h2 && databaseType != DatabaseType.h2single && databaseType != DatabaseType.h2custom && databaseType != DatabaseType.hsqldb) {
            setDatabaseDriver(JXPathUtils.getStringValue(settings, "database/driver", getDatabaseDriver()));
        }
        if (databaseType != DatabaseType.h2 && databaseType != DatabaseType.h2single && databaseType != DatabaseType.hsqldb) {
            setDatabaseConnectionOptions(JXPathUtils.getStringValue(settings, "database/conn-options", getDatabaseConnectionOptions()));
        }
        if (databaseType != DatabaseType.h2 && databaseType != DatabaseType.h2single && databaseType != DatabaseType.mysqlinternal && databaseType != DatabaseType.hsqldb) {
            setDatabaseConnection(JXPathUtils.getStringValue(settings, "database/connection", getDatabaseConnection()));
            setDatabaseUser(JXPathUtils.getStringValue(settings, "database/user", getDatabaseUser()));
            setDatabasePassword(JXPathUtils.getStringValue(settings, "database/password", getDatabasePassword()));
        }
    }

    public synchronized void setDefaultDatabaseSettings() {
        if (getDatabaseType() == DatabaseType.h2) {
            setDatabaseDriver("org.h2.Driver");
            setDatabaseConnection("jdbc:h2:file:" + MyTunesRss.CACHE_DATA_PATH + "/" + "h2/MyTunesRSS;MAX_LOG_SIZE=64;MULTI_THREADED=1");
            setDatabaseUser("sa");
            setDatabasePassword("");
        } else if (getDatabaseType() == DatabaseType.h2single) {
            setDatabaseDriver("org.h2.Driver");
            setDatabaseConnection("jdbc:h2:file:" + MyTunesRss.CACHE_DATA_PATH + "/" + "h2/MyTunesRSS;MAX_LOG_SIZE=64");
            setDatabaseUser("sa");
            setDatabasePassword("");
        } else if (getDatabaseType() == DatabaseType.hsqldb) {
            setDatabaseDriver(JDBCDriver.class.getName());
            setDatabaseConnection("jdbc:hsqldb:file:" + MyTunesRss.CACHE_DATA_PATH + "/" + "hsqldb/MyTunesRSS");
            setDatabaseUser("sa");
            setDatabasePassword("");
        } else if (getDatabaseType() == DatabaseType.mysqlinternal) {
            setDatabaseDriver("com.mysql.jdbc.Driver");
            setDatabaseConnection("jdbc:mysql:mxj://localhost/mytunesrss?createDatabaseIfNotExist=true&server.initialize-user=true&useCursorFetch=true&server.basedir=" + MyTunesRss.INTERNAL_MYSQL_SERVER_PATH);
            setDatabaseConnectionOptions(DEFAULT_INTERNAL_MYSQL_CONNECTION_OPTIONS);
            setDatabaseUser("mytunesrss");
            setDatabasePassword("mytunesrss");
        }
    }

    private static File getSettingsFile() {
        String filename = "settings.xml";
        return new File(MyTunesRss.PREFERENCES_DATA_PATH + "/" + filename);
    }

    private void readPathInfoEncryptionKey(JXPathContext settings) {
        byte[] keyBytes = JXPathUtils.getByteArray(settings, "pathInfoKey", null);
        if (keyBytes != null && keyBytes.length > 0) {
            myPathInfoKey = new SecretKeySpec(keyBytes, "DES");
        }
        if (myPathInfoKey == null) {
            LOGGER.info("No path info encryption key found, generating a new one.");
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance("DES");
                keyGenerator.init(56);
                myPathInfoKey = keyGenerator.generateKey();
            } catch (Exception e) {
                LOGGER.error("Could not generate path info encryption key.", e);
            }
        }
    }

    public synchronized void save() {
        try {
            LOGGER.info("Saving configuration to \"" + getSettingsFile().getAbsolutePath() + "\".");
            Document settings = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = settings.createElement("settings");
            settings.appendChild(root);
            root.appendChild(DOMUtils.createByteArrayElement(settings, "adminPassword", getAdminPasswordHash()));
            root.appendChild(DOMUtils.createTextElement(settings, "adminHost", myAdminHost));
            root.appendChild(DOMUtils.createIntElement(settings, "adminPort", myAdminPort));
            root.appendChild(DOMUtils.createTextElement(settings, "version", myVersion));
            if (StringUtils.isNotBlank(myHost)) {
                root.appendChild(DOMUtils.createTextElement(settings, "serverHost", myHost));
            }
            root.appendChild(DOMUtils.createIntElement(settings, "serverPort", myPort));
            root.appendChild(DOMUtils.createTextElement(settings, "serverName", myServerName));
            root.appendChild(DOMUtils.createBooleanElement(settings, "availableOnLocalNet", myAvailableOnLocalNet));
            root.appendChild(DOMUtils.createBooleanElement(settings, "checkUpdateOnStart", myCheckUpdateOnStart));
            writeDataSources(settings, root);
            root.appendChild(DOMUtils.createBooleanElement(settings, "localTempArchive", myLocalTempArchive));
            Element users = settings.createElement("users");
            root.appendChild(users);
            for (User user : myUsers) {
                Element userElement = settings.createElement("user");
                users.appendChild(userElement);
                user.saveToPreferences(settings, userElement);
            }
            root.appendChild(DOMUtils.createTextElement(settings, "proxyHost", myProxyHost));
            root.appendChild(DOMUtils.createIntElement(settings, "proxyPort", myProxyPort));
            root.appendChild(DOMUtils.createTextElement(settings, "webWelcomeMessage", myWebWelcomeMessage));
            root.appendChild(DOMUtils.createTextElement(settings, "webLoginMessage", myWebLoginMessage));
            if (myPathInfoKey != null) {
                root.appendChild(DOMUtils.createByteArrayElement(settings, "pathInfoKey", myPathInfoKey.getEncoded()));
            }
            root.appendChild(DOMUtils.createLongElement(settings, "transcodingCacheMaxGiB", myTranscodingCacheMaxGiB));
            root.appendChild(DOMUtils.createLongElement(settings, "tempMaxGiB", myTempMaxGiB));
            root.appendChild(DOMUtils.createLongElement(settings, "httpLiveStreamCacheMaxGiB", myHttpLiveStreamCacheMaxGiB));
            root.appendChild(DOMUtils.createTextElement(settings, "codewaveLogLevel", myCodewaveLogLevel.toString().toUpperCase()));
            root.appendChild(DOMUtils.createTextElement(settings, "lastNewVersionInfo", myLastNewVersionInfo));
            root.appendChild(DOMUtils.createTextElement(settings, "updateIgnoreVersion", myUpdateIgnoreVersion));
            if ((myDatabaseUpdateTriggers != null && myDatabaseUpdateTriggers.size() > 0) || (myDatabaseBackupTriggers != null && myDatabaseBackupTriggers.size() > 0)) {
                Element cronTriggers = settings.createElement("crontriggers");
                root.appendChild(cronTriggers);
                if (myDatabaseUpdateTriggers != null && myDatabaseUpdateTriggers.size() > 0) {
                    for (String trigger : myDatabaseUpdateTriggers) {
                        cronTriggers.appendChild(DOMUtils.createTextElement(settings, "database", trigger));
                    }
                }
                if (myDatabaseBackupTriggers != null && myDatabaseBackupTriggers.size() > 0) {
                    for (String trigger : myDatabaseBackupTriggers) {
                        cronTriggers.appendChild(DOMUtils.createTextElement(settings, "database-backup", trigger));
                    }
                }
            }
            // for default h2 database we should not save anything to the config
            if (getDatabaseType() != DatabaseType.h2) {
                Element database = settings.createElement("database");
                root.appendChild(database);
                database.appendChild(DOMUtils.createTextElement(settings, "type", getDatabaseType().name()));
                if (getDatabaseType() != DatabaseType.hsqldb && getDatabaseType() != DatabaseType.h2single) {
                    // for hsqldb we only save the type
                    database.appendChild(DOMUtils.createTextElement(settings, "conn-options", getDatabaseConnectionOptions()));
                    // for internal mysql database we should not save additional info in the config
                    if (getDatabaseType() != DatabaseType.mysqlinternal) {
                        database.appendChild(DOMUtils.createTextElement(settings, "driver", getDatabaseDriver()));
                        database.appendChild(DOMUtils.createTextElement(settings, "connection", getDatabaseConnection()));
                        database.appendChild(DOMUtils.createTextElement(settings, "user", getDatabaseUser()));
                        database.appendChild(DOMUtils.createTextElement(settings, "password", getDatabasePassword()));
                    }
                }
            }
            Element tomcat = settings.createElement("tomcat");
            root.appendChild(tomcat);
            tomcat.appendChild(DOMUtils.createTextElement(settings, "max-threads", getTomcatMaxThreads()));
            tomcat.appendChild(DOMUtils.createTextElement(settings, "webapp-context", getWebappContext()));
            Element ssl = settings.createElement("ssl");
            root.appendChild(ssl);
            if (StringUtils.isNotBlank(getSslHost())) {
                ssl.appendChild(DOMUtils.createTextElement(settings, "host", getSslHost()));
            }
            ssl.appendChild(DOMUtils.createIntElement(settings, "port", getSslPort()));
            Element keystore = settings.createElement("keystore");
            ssl.appendChild(keystore);
            keystore.appendChild(DOMUtils.createTextElement(settings, "file", getSslKeystoreFile()));
            keystore.appendChild(DOMUtils.createTextElement(settings, "pass", getSslKeystorePass()));
            keystore.appendChild(DOMUtils.createTextElement(settings, "keyalias", getSslKeystoreKeyAlias()));
            root.appendChild(DOMUtils.createTextElement(settings, "mail-host", getMailHost()));
            root.appendChild(DOMUtils.createIntElement(settings, "mail-port", getMailPort()));
            root.appendChild(DOMUtils.createTextElement(settings, "smtp-protocol", getSmtpProtocol().name()));
            root.appendChild(DOMUtils.createTextElement(settings, "mail-login", getMailLogin()));
            root.appendChild(DOMUtils.createTextElement(settings, "mail-password", getMailPassword()));
            root.appendChild(DOMUtils.createTextElement(settings, "mail-sender", getMailSender()));
            root.appendChild(DOMUtils.createTextElement(settings, "admin-email", getAdminEmail()));
            Element notify = settings.createElement("admin-notify");
            root.appendChild(notify);
            notify.appendChild(DOMUtils.createBooleanElement(settings, "database-update", isNotifyOnDatabaseUpdate()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "email-change", isNotifyOnEmailChange()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "internal-error", isNotifyOnInternalError()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "login-failure", isNotifyOnLoginFailure()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "password-change", isNotifyOnPasswordChange()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "quota-exceeded", isNotifyOnQuotaExceeded()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "transcoding-failure", isNotifyOnTranscodingFailure()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "web-upload", isNotifyOnWebUpload()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "missing-file", isNotifyOnMissingFile()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "outdated-itunesxml", isNotifyOnOutdatedItunesXml()));
            notify.appendChild(DOMUtils.createBooleanElement(settings, "skipped-db-update", isNotifyOnSkippedDatabaseUpdate()));
            root.appendChild(DOMUtils.createIntElement(settings, "statistics-keep-time", getStatisticKeepTime()));
            Element transcoderConfigs = settings.createElement("transcoders");
            root.appendChild(transcoderConfigs);
            for (TranscoderConfig transcoderConfig : getTranscoderConfigs()) {
                Element config = settings.createElement("config");
                transcoderConfigs.appendChild(config);
                transcoderConfig.writeTo(settings, config);
            }
            if (myExternalSites != null && !myExternalSites.isEmpty()) {
                Element externalSites = settings.createElement("external-sites");
                root.appendChild(externalSites);
                for (ExternalSiteDefinition def : myExternalSites) {
                    Element xmlSite = settings.createElement("site");
                    externalSites.appendChild(xmlSite);
                    xmlSite.appendChild(DOMUtils.createTextElement(settings, "type", def.getType()));
                    xmlSite.appendChild(DOMUtils.createTextElement(settings, "name", def.getName()));
                    xmlSite.appendChild(DOMUtils.createTextElement(settings, "url", def.getUrl()));
                }
            }
            root.appendChild(DOMUtils.createBooleanElement(settings, "serverBrowserActive", isServerBrowserActive()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "openIdActive", isOpenIdActive()));
            root.appendChild(myLdapConfig.createSettingsElement(settings));
            if (!getFlashPlayers().isEmpty()) {
                Element flashPlayers = settings.createElement("flash-players");
                root.appendChild(flashPlayers);
                for (FlashPlayerConfig flashPlayerConfig : getFlashPlayers()) {
                    Element player = settings.createElement("player");
                    flashPlayers.appendChild(player);
                    player.appendChild(DOMUtils.createTextElement(settings, "id", flashPlayerConfig.getId()));
                    player.appendChild(DOMUtils.createTextElement(settings, "name", flashPlayerConfig.getName()));
                    player.appendChild(DOMUtils.createTextElement(settings, "filetype", flashPlayerConfig.getPlaylistFileType().name()));
                    player.appendChild(DOMUtils.createTextElement(settings, "timeunit", flashPlayerConfig.getTimeUnit().name()));
                    player.appendChild(DOMUtils.createIntElement(settings, "width", flashPlayerConfig.getWidth()));
                    player.appendChild(DOMUtils.createIntElement(settings, "height", flashPlayerConfig.getHeight()));
                    player.appendChild(DOMUtils.createIntElement(settings, "imageSize", flashPlayerConfig.getImageSize()));
                }
            }
            root.appendChild(DOMUtils.createBooleanElement(settings, "initialWizard", isInitialWizard()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "upnp-admin", isUpnpAdmin()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "upnp-user-http", isUpnpUserHttp()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "upnp-user-https", isUpnpUserHttps()));
            root.appendChild(DOMUtils.createTextElement(settings, "selfreg-template-user", getSelfRegisterTemplateUser()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "selfreg-admin-email", isSelfRegAdminEmail()));
            root.appendChild(DOMUtils.createTextElement(settings, "default-ui-theme", getDefaultUserInterfaceTheme()));
            //root.appendChild(DOMUtils.createTextElement(settings, "facebook-api-key", getFacebookApiKey()));
            root.appendChild(DOMUtils.createIntElement(settings, "database-backup-max", getNumberKeepDatabaseBackups()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "database-backup-after-init", isBackupDatabaseAfterInit()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "headless", isHeadless()));
            if (getVlcExecutable() != null) {
                root.appendChild(DOMUtils.createTextElement(settings, "vlc", getVlcExecutable().getAbsolutePath()));
            }
            if (getGmExecutable() != null) {
                root.appendChild(DOMUtils.createTextElement(settings, "gm", getGmExecutable().getAbsolutePath()));
            }
            root.appendChild(DOMUtils.createBooleanElement(settings, "vlc-enabled", isVlcEnabled()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "gm-enabled", isGmEnabled()));
            root.appendChild(DOMUtils.createLongElement(settings, "image-expiration-millis", getImageExpirationMillis()));
            root.appendChild(DOMUtils.createLongElement(settings, "restapijs-expiration-millis", getRestApiJsExpirationMillis()));
            root.appendChild(DOMUtils.createIntElement(settings, "jpeg-quality", getJpegQuality()));
            root.appendChild(DOMUtils.createIntElement(settings, "on-demand-thumbnail-threads", getOnDemandThumbnailGenerationThreads()));
            root.appendChild(DOMUtils.createIntElement(settings, "on-demand-thumbnail-timoeut-seconds", getOnDemandThumbnailGenerationTimeoutSeconds()));
            root.appendChild(DOMUtils.createTextElement(settings, "accesslog-tz", getAccessLogTz()));
            root.appendChild(DOMUtils.createIntElement(settings, "accesslog-user-retain", getUserAccessLogRetainDays()));
            root.appendChild(DOMUtils.createIntElement(settings, "accesslog-admin-retain", getAdminAccessLogRetainDays()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "accesslog-user-ext", isUserAccessLogExtended()));
            root.appendChild(DOMUtils.createBooleanElement(settings, "accesslog-admin-ext", isAdminAccessLogExtended()));
            if (!getGenreMappings().isEmpty()) {
                Element genreMappingsElement = settings.createElement("genre-mappings");
                root.appendChild(genreMappingsElement);
                for (Map.Entry<String, String> genreMapping : getGenreMappings().entrySet()) {
                    Element genreMappingElement = settings.createElement("mapping");
                    genreMappingsElement.appendChild(genreMappingElement);
                    genreMappingElement.appendChild(DOMUtils.createTextElement(settings, "from", genreMapping.getKey()));
                    genreMappingElement.appendChild(DOMUtils.createTextElement(settings, "to", genreMapping.getValue()));
                }
            }
            if (!getCustomWebHeaders().isEmpty()) {
                Element customWebHeadersElement = settings.createElement("custom-web-headers");
                root.appendChild(customWebHeadersElement);
                for (Map.Entry<String, String> customWebHeader : getCustomWebHeaders().entrySet()) {
                    Element customWebHeaderElement = settings.createElement("header");
                    customWebHeadersElement.appendChild(customWebHeaderElement);
                    customWebHeaderElement.appendChild(DOMUtils.createTextElement(settings, "name", customWebHeader.getKey()));
                    customWebHeaderElement.appendChild(DOMUtils.createTextElement(settings, "value", customWebHeader.getValue()));
                }
            }
            root.appendChild(DOMUtils.createBooleanElement(settings, "upnp-server-active", isUpnpMediaServerActive()));
            if (StringUtils.isNotBlank(getUpnpMediaServerName())) {
                root.appendChild(DOMUtils.createTextElement(settings, "upnp-server-name", getUpnpMediaServerName()));
            }
            root.appendChild(DOMUtils.createIntElement(settings, "upnp-server-timeout", getUpnpMediaServerLockTimeoutSeconds()));
            FileOutputStream outputStream = null;
            try {
                File settingsFile = getSettingsFile();
                if (!settingsFile.renameTo(new File(settingsFile.getParentFile(), settingsFile.getName() + ".bak"))) {
                    LOGGER.debug("Could not add backup extension to file \"" + settingsFile.getAbsolutePath() + "\".");
                }
                outputStream = new FileOutputStream(settingsFile);
                DOMUtils.prettyPrint(settings, outputStream);
            } finally {
                IOUtils.close(outputStream);
            }
        } catch (IOException | ParserConfigurationException | RuntimeException e) {
            LOGGER.error("Could not write settings file.", e);
        }
    }

    private void writeDataSources(Document settings, Element root) {
        Element dataSources = settings.createElement("datasources");
        root.appendChild(dataSources);
        for (DatasourceConfig myDatasource : myDatasources) {
            Element dataSource = settings.createElement("datasource");
            dataSources.appendChild(dataSource);
            dataSource.appendChild(DOMUtils.createTextElement(settings, "type", myDatasource.getType().name()));
            dataSource.appendChild(DOMUtils.createTextElement(settings, "name", myDatasource.getName()));
            dataSource.appendChild(DOMUtils.createTextElement(settings, "definition", myDatasource.getDefinition()));
            dataSource.appendChild(DOMUtils.createTextElement(settings, "id", myDatasource.getId()));
            switch (myDatasource.getType()) {
                case Watchfolder:
                    WatchfolderDatasourceConfig watchfolderDatasourceConfig = (WatchfolderDatasourceConfig) myDatasource;
                    dataSource.appendChild(DOMUtils.createLongElement(settings, "minFileSize", watchfolderDatasourceConfig.getMinFileSize()));
                    dataSource.appendChild(DOMUtils.createLongElement(settings, "maxFileSize", watchfolderDatasourceConfig.getMaxFileSize()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "include", watchfolderDatasourceConfig.getIncludePattern()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "exclude", watchfolderDatasourceConfig.getExcludePattern()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "artistFallback", watchfolderDatasourceConfig.getArtistFallback()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "titleFallback", watchfolderDatasourceConfig.getTitleFallback()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "albumFallback", watchfolderDatasourceConfig.getAlbumFallback()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "seriesFallback", watchfolderDatasourceConfig.getSeriesFallback()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "seasonFallback", watchfolderDatasourceConfig.getSeasonFallback()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "episodeFallback", watchfolderDatasourceConfig.getEpisodeFallback()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "videoType", watchfolderDatasourceConfig.getVideoType().name()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "photoAlbumPattern", watchfolderDatasourceConfig.getPhotoAlbumPattern()));
                    dataSource.appendChild(DOMUtils.createBooleanElement(settings, "ignoreFileMeta", watchfolderDatasourceConfig.isIgnoreFileMeta()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "artistDropwords", watchfolderDatasourceConfig.getArtistDropWords()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "id3v2-track-comment", watchfolderDatasourceConfig.getId3v2TrackComment()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "disabled-mp4-codecs", watchfolderDatasourceConfig.getDisabledMp4Codecs()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "track-image-import", watchfolderDatasourceConfig.getTrackImageImportType().name()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "photo-thumbnail-import", watchfolderDatasourceConfig.getPhotoThumbnailImportType().name()));
                    dataSource.appendChild(DOMUtils.createBooleanElement(settings, "use-single-image", watchfolderDatasourceConfig.isUseSingleImageInFolder()));
                    dataSource.appendChild(DOMUtils.createBooleanElement(settings, "import-playlists", watchfolderDatasourceConfig.isImportPlaylists()));
                    writeTrackImagePatterns(settings, dataSource, watchfolderDatasourceConfig);
                    dataSource.appendChild(DOMUtils.createBooleanElement(settings, "upload", myDatasource.isUpload()));
                    break;
                case Itunes:
                    ItunesDatasourceConfig itunesDatasourceConfig = (ItunesDatasourceConfig) myDatasource;
                    if (itunesDatasourceConfig.getPathReplacements() != null && !itunesDatasourceConfig.getPathReplacements().isEmpty()) {
                        Element pathReplacementsElement = settings.createElement("path-replacements");
                        dataSource.appendChild(pathReplacementsElement);
                        for (ReplacementRule pathReplacement : itunesDatasourceConfig.getPathReplacements()) {
                            Element pathReplacementElement = settings.createElement("replacement");
                            pathReplacementsElement.appendChild(pathReplacementElement);
                            pathReplacementElement.appendChild(DOMUtils.createTextElement(settings, "search", pathReplacement.getSearchPattern()));
                            pathReplacementElement.appendChild(DOMUtils.createTextElement(settings, "replacement", pathReplacement.getReplacement()));
                        }
                    }
                    if (itunesDatasourceConfig.getIgnorePlaylists() != null && !itunesDatasourceConfig.getIgnorePlaylists().isEmpty()) {
                        Element ignorePlaylistsElement = settings.createElement("ignore-playlists");
                        dataSource.appendChild(ignorePlaylistsElement);
                        for (ItunesPlaylistType type : itunesDatasourceConfig.getIgnorePlaylists()) {
                            ignorePlaylistsElement.appendChild(DOMUtils.createTextElement(settings, "type", type.name()));
                        }
                    }
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "artistDropwords", itunesDatasourceConfig.getArtistDropWords()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "disabled-mp4-codecs", itunesDatasourceConfig.getDisabledMp4Codecs()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "track-image-import", itunesDatasourceConfig.getTrackImageImportType().name()));
                    dataSource.appendChild(DOMUtils.createBooleanElement(settings, "use-single-image", itunesDatasourceConfig.isUseSingleImageInFolder()));
                    writeTrackImagePatterns(settings, dataSource, itunesDatasourceConfig);
                    dataSource.appendChild(DOMUtils.createBooleanElement(settings, "upload", myDatasource.isUpload()));
                    break;
                case Iphoto:
                    IphotoDatasourceConfig iphotoDatasourceConfig = (IphotoDatasourceConfig) myDatasource;
                    if (iphotoDatasourceConfig.getPathReplacements() != null && !iphotoDatasourceConfig.getPathReplacements().isEmpty()) {
                        Element pathReplacementsElement = settings.createElement("path-replacements");
                        dataSource.appendChild(pathReplacementsElement);
                        for (ReplacementRule pathReplacement : iphotoDatasourceConfig.getPathReplacements()) {
                            Element pathReplacementElement = settings.createElement("replacement");
                            pathReplacementsElement.appendChild(pathReplacementElement);
                            pathReplacementElement.appendChild(DOMUtils.createTextElement(settings, "search", pathReplacement.getSearchPattern()));
                            pathReplacementElement.appendChild(DOMUtils.createTextElement(settings, "replacement", pathReplacement.getReplacement()));
                        }
                    }
                    dataSource.appendChild(DOMUtils.createBooleanElement(settings, "importRolls", iphotoDatasourceConfig.isImportRolls()));
                    dataSource.appendChild(DOMUtils.createBooleanElement(settings, "importAlbums", iphotoDatasourceConfig.isImportAlbums()));
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "photo-thumbnail-import", iphotoDatasourceConfig.getPhotoThumbnailImportType().name()));
                    break;
                case Aperture:
                    ApertureDatasourceConfig apertureDatasourceConfig = (ApertureDatasourceConfig) myDatasource;
                    if (apertureDatasourceConfig.getPathReplacements() != null && !apertureDatasourceConfig.getPathReplacements().isEmpty()) {
                        Element pathReplacementsElement = settings.createElement("path-replacements");
                        dataSource.appendChild(pathReplacementsElement);
                        for (ReplacementRule pathReplacement : apertureDatasourceConfig.getPathReplacements()) {
                            Element pathReplacementElement = settings.createElement("replacement");
                            pathReplacementsElement.appendChild(pathReplacementElement);
                            pathReplacementElement.appendChild(DOMUtils.createTextElement(settings, "search", pathReplacement.getSearchPattern()));
                            pathReplacementElement.appendChild(DOMUtils.createTextElement(settings, "replacement", pathReplacement.getReplacement()));
                        }
                    }
                    dataSource.appendChild(DOMUtils.createTextElement(settings, "photo-thumbnail-import", apertureDatasourceConfig.getPhotoThumbnailImportType().name()));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown datasource type!");
            }
        }
    }

    private void writeTrackImagePatterns(Document settings, Element dataSource, CommonTrackDatasourceConfig datasourceConfig) {
        if (!datasourceConfig.getTrackImagePatterns().isEmpty()) {
            Element trackImageMappingsElement = settings.createElement("track-image-patterns");
            dataSource.appendChild(trackImageMappingsElement);
            for (String pattern : datasourceConfig.getTrackImagePatterns()) {
                trackImageMappingsElement.appendChild(DOMUtils.createTextElement(settings, "pattern", pattern));
            }
        }
    }

    private void migrate(Version current) {
        if (current.compareTo(new Version("4.1.0")) < 0) {
            setVersion("4.1.0");
            for (DatasourceConfig dc : getDatasources()) {
                if (dc.getType() == DatasourceType.Watchfolder) {
                    WatchfolderDatasourceConfig wdc = (WatchfolderDatasourceConfig) dc;
                    wdc.setAlbumFallback(StringUtils.trimToEmpty(migrateFallback(wdc.getAlbumFallback())));
                    wdc.setArtistFallback(StringUtils.trimToEmpty(migrateFallback(wdc.getArtistFallback())));
                }
            }
        }
        if (current.compareTo(new Version("4.3.0")) < 0) {
            setVersion("4.3.0");
            myFlashPlayers = FlashPlayerConfig.getDefaults();
        }
        if (current.compareTo(new Version("4.8.0")) < 0) {
            setVersion("4.8.0");
            myTranscoderConfigs = TranscoderConfig.getDefaultTranscoders();
        }
        if (current.compareTo(new Version("6.3.2")) < 0) {
            setVersion("6.3.2");
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            MyTunesRss.REBUILD_LUCENE_INDEX_ON_STARTUP = true;
        }
    }

    private String migrateFallback(String fallback) {
        for (String type : new String[] {"dir", "file"}) {
            for (String token : MyTunesRssUtils.substringsBetween(fallback, "[" + type + ":", "]")) {
                fallback = fallback.replace("[" + type + ":" + token + "]","[[[dir:" + token + "]]]");
            }
        }
        return fallback;
    }

    public synchronized boolean isDefaultDatabase() {
        return getDatabaseType() == null || getDatabaseType() == DatabaseType.h2;
    }

    public synchronized boolean isRemoteControl() {
        //return MyTunesRss.CONFIG.isVlcEnabled() && MyTunesRssUtils.canExecute(getVlcExecutable());
        return true;
    }

    public synchronized boolean isValidMailConfig() {
        return StringUtils.isNotEmpty(getMailHost()) && StringUtils.isNotEmpty(getMailSender());
    }

    public synchronized boolean isShowInitialWizard() {
        return isInitialWizard() && getUsers().isEmpty() && getDatasources().isEmpty();
    }

    public synchronized boolean isValidVlcConfig() {
        return isVlcEnabled() && MyTunesRssUtils.canExecute(getVlcExecutable());
    }

    public synchronized void replaceDatasourceConfig(DatasourceConfig config) {
        for (Iterator<DatasourceConfig> iterConfigs = myDatasources.iterator(); iterConfigs.hasNext(); ) {
            if (iterConfigs.next().getId().equals(config.getId())) {
                iterConfigs.remove();
                myDatasources.add(config);
                break;
            }
        }
    }

    public synchronized List<DatasourceConfig> getUploadableDatasources() {
        List<DatasourceConfig> uploadableDatasources = new ArrayList<>();
        for (DatasourceConfig datasource : getDatasources()) {
            if (datasource.isUploadable() && datasource.isUpload()) {
                uploadableDatasources.add(DatasourceConfig.copy(datasource));
            }
        }
        return uploadableDatasources;
    }

    public synchronized boolean isUploadableDatasource() {
        for (DatasourceConfig datasource : getDatasources()) {
            if (datasource.isUploadable() && datasource.isUpload()) {
                return true;
            }
        }
        return false;
    }

}
