/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss;

import de.codewave.mytunesrss.datastore.MyTunesRssDataStore;
import de.codewave.mytunesrss.jmx.ErrorQueue;
import de.codewave.mytunesrss.jmx.MyTunesRssJmxUtils;
import de.codewave.mytunesrss.job.MyTunesRssJobUtils;
import de.codewave.mytunesrss.network.MulticastService;
import de.codewave.mytunesrss.quicktime.QuicktimePlayer;
import de.codewave.mytunesrss.server.WebServer;
import de.codewave.mytunesrss.settings.Settings;
import de.codewave.mytunesrss.statistics.StatisticsDatabaseWriter;
import de.codewave.mytunesrss.statistics.StatisticsEventManager;
import de.codewave.mytunesrss.task.DatabaseBuilderTask;
import de.codewave.mytunesrss.task.DeleteDatabaseFilesTask;
import de.codewave.mytunesrss.task.InitializeDatabaseTask;
import de.codewave.utils.ProgramUtils;
import de.codewave.utils.Version;
import de.codewave.utils.io.FileCache;
import de.codewave.utils.maven.MavenUtils;
import de.codewave.utils.swing.SwingUtils;
import de.codewave.utils.xml.JXPathUtils;
import org.apache.catalina.LifecycleException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.Timer;

/**
 * de.codewave.mytunesrss.MyTunesRss
 */
public class MyTunesRss {
    public static final String APPLICATION_IDENTIFIER = "MyTunesRSS3";
    public static final Map<String, String[]> COMMAND_LINE_ARGS = new HashMap<String, String[]>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MyTunesRss.class);
    public static final String MYTUNESRSSCOM_URL = "http://mytunesrss.com";
    public static final String MYTUNESRSSCOM_TOOLS_URL = MYTUNESRSSCOM_URL + "/tools";
    public static String VERSION;
    public static URL UPDATE_URL;
    public static MyTunesRssDataStore STORE = new MyTunesRssDataStore();
    public static MyTunesRssConfig CONFIG = new MyTunesRssConfig();
    public static ResourceBundle BUNDLE = PropertyResourceBundle.getBundle("de.codewave.mytunesrss.MyTunesRss");
    public static ResourceBundle JMX_BUNDLE = PropertyResourceBundle.getBundle("de.codewave.mytunesrss.jmx.MyTunesRssJmx");
    public static WebServer WEBSERVER = new WebServer();
    public static Timer SERVER_RUNNING_TIMER = new Timer("MyTunesRSSServerRunningTimer");
    public static MyTunesRssSystray SYSTRAY;
    public static MessageDigest SHA1_DIGEST;
    public static MessageDigest MD5_DIGEST;
    public static JFrame ROOT_FRAME;
    public static ImageIcon PLEASE_WAIT_ICON;
    public static MyTunesRssRegistration REGISTRATION = new MyTunesRssRegistration();
    public static int OPTION_PANE_MAX_MESSAGE_LENGTH = 100;
    public static boolean HEADLESS = GraphicsEnvironment.isHeadless();
    private static Settings SETTINGS;
    public static final String THREAD_PREFIX = "MyTunesRSS: ";
    public static ErrorQueue ERROR_QUEUE = new ErrorQueue();
    public static boolean QUIT_REQUEST;
    public static FileCache STREAMING_CACHE;
    public static FileCache ARCHIVE_CACHE;
    public static Scheduler QUARTZ_SCHEDULER;
    public static MailSender MAILER = new MailSender();
    public static AdminNotifier ADMIN_NOTIFY = new AdminNotifier();
    public static String JMX_HOST;
    public static int JMX_PORT = -1;
    public static QuicktimePlayer QUICKTIME_PLAYER;
    public static LuceneTrackService LUCENE_TRACK_SERVICE = new LuceneTrackService();
    public static String[] ORIGINAL_CMD_ARGS;

    private static void init() {
        try {
            System.setProperty("MyTunesRSS.logDir", MyTunesRssUtils.getCacheDataPath());
        } catch (IOException e) {
            System.setProperty("MyTunesRSS.logDir", ".");
        }
        try {
            for (Iterator<File> iter =
                    (Iterator<File>) FileUtils.iterateFiles(new File(MyTunesRssUtils.getCacheDataPath()),
                            new String[]{"log"},
                            false); iter.hasNext();) {
                iter.next().delete();
            }
        } catch (Exception e) {
            // ignore exceptions when deleting log files
        }
        DOMConfigurator.configure(MyTunesRss.class.getResource("/mytunesrss-log4j.xml"));
        WEBSERVER = new WebServer();
        ERROR_QUEUE = new ErrorQueue();
        MAILER = new MailSender();
        ADMIN_NOTIFY = new AdminNotifier();
        try {
            File file = new File(MyTunesRssUtils.getPreferencesDataPath() + "/system.properties");
            if (file.isFile()) {
                Properties properties = new Properties();
                properties.load(new FileInputStream(file));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Setting system properties from \"" + file.getAbsolutePath() + "\".");
                }
                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    System.setProperty(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Could not load user system properties: " + e.getMessage());
            }
        }
        try {
            UPDATE_URL = new URL("http://www.codewave.de/download/versions/mytunesrss.xml");
        } catch (MalformedURLException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Could not create update external.", e);
            }
        }
        try {
            SHA1_DIGEST = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Could not create SHA-1 digest.", e);
            }
        }
        try {
            MD5_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Could not create MD5 digest.", e);
            }
        }
    }

    public static void main(final String[] args)
            throws LifecycleException, IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException,
            IOException, SQLException, SchedulerException {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                MyTunesRssUtils.onShutdown();
            }
        }));
        ORIGINAL_CMD_ARGS = args;
        Map<String, String[]> arguments = ProgramUtils.getCommandLineArguments(args);
        if (arguments != null) {
            COMMAND_LINE_ARGS.putAll(arguments);
        }
        if (System.getProperty("de.codewave.mytunesrss.shutdown") == null) {
            init();
        }
        LOGGER.info("Command line: " + StringUtils.join(args, " "));
        VERSION = MavenUtils.getVersion("de.codewave.mytunesrss", "runtime");
        if (StringUtils.isEmpty(VERSION)) {
            VERSION = System.getProperty("MyTunesRSS.version", "0.0.0");
        }
        CONFIG = new MyTunesRssConfig();
        MyTunesRss.CONFIG.load();
        File license = null;
        if (arguments.containsKey("license")) {
            license = new File(arguments.get("license")[0]);
            if (!license.isFile()) {
                LOGGER.error("License file \"" + license.getAbsolutePath() + "\" specified on command line does not exist.");
                license = null;
            } else {
                LOGGER.info("Using license file \"" + license.getAbsolutePath() + "\" specified on command line.");
            }
        }
        REGISTRATION = new MyTunesRssRegistration();
        REGISTRATION.init(license, true);
        if (REGISTRATION.getSettings() != null) {
            LOGGER.info("Loading configuration from license.");
            MyTunesRssConfig configFromFile = MyTunesRss.CONFIG;
            MyTunesRss.CONFIG = new MyTunesRssConfig();
            MyTunesRss.CONFIG.loadFromContext(REGISTRATION.getSettings());
            if (configFromFile.getPathInfoKey() != null) {
                MyTunesRss.CONFIG.setPathInfoKey(configFromFile.getPathInfoKey());
            }
        }
        if (System.getProperty("de.codewave.mytunesrss.shutdown") != null) {
            if (MyTunesRssUtils.isOtherInstanceRunning(1000)) {
                MyTunesRssUtils.shutdownRemoteProcess("http://localhost:" + CONFIG.getJmxPort());
                // return code 1 if other instance was not shut down or 0 otherwise
                System.exit(MyTunesRssUtils.isOtherInstanceRunning(10000) ? 1 : 0);
            }
            // return code 0 if other instance was not running on this machine
            System.exit(0);
        }
        HEADLESS = COMMAND_LINE_ARGS.containsKey("headless") || CONFIG.isDisableGui();
        MyTunesRssUtils.setCodewaveLogLevel(MyTunesRss.CONFIG.getCodewaveLogLevel());
        initializeQuicktimePlayer();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Operating system: " + SystemUtils.OS_NAME + ", " + SystemUtils.OS_VERSION + ", " + SystemUtils.OS_ARCH);
            LOGGER.info("Java: " + SystemUtils.JAVA_VERSION + "(" + SystemUtils.JAVA_HOME + ")");
            LOGGER.info("Maximum heap size: " + MyTunesRssUtils.getMemorySizeForDisplay(Runtime.getRuntime().maxMemory()));
            LOGGER.info("Application version: " + VERSION);
            LOGGER.info("Cache data path: " + MyTunesRssUtils.getCacheDataPath());
            LOGGER.info("Preferences data path: " + MyTunesRssUtils.getPreferencesDataPath());
            LOGGER.info("--------------------------------------------------------------------------------");
            for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
                LOGGER.info(entry.getKey() + "=" + entry.getValue());
            }
            LOGGER.info("--------------------------------------------------------------------------------");
        }
        if (!HEADLESS) {
            Thread.setDefaultUncaughtExceptionHandler(new MyTunesRssUncaughtHandler(ROOT_FRAME, false));
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            ROOT_FRAME = new JFrame(BUNDLE.getString("settings.title") + " v" + VERSION);
            ROOT_FRAME.setIconImage(ImageIO.read(MyTunesRss.class.getResource("WindowIcon.png")));
            PLEASE_WAIT_ICON = new ImageIcon(MyTunesRss.class.getResource("PleaseWait.gif"));
        }
        if (System.getProperty("de.codewave.mytunesrss") == null) {
            String type = "generic";
            if (SystemUtils.IS_OS_WINDOWS) {
                type = "windows";
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                type = "osx";
            }
            MyTunesRssUtils.showErrorMessage(BUNDLE.getString("error.missingSystemProperty." + type));
        }
        if (MyTunesRssUtils.isOtherInstanceRunning(3000)) {
            MyTunesRssUtils.showErrorMessage(BUNDLE.getString("error.otherInstanceRunning"));
            MyTunesRssUtils.shutdownGracefully();
        }
        if (new Version(CONFIG.getVersion()).compareTo(new Version(VERSION)) > 0) {
            MyTunesRssUtils.showErrorMessage(MessageFormat.format(BUNDLE.getString("error.configVersionMismatch"), VERSION, CONFIG.getVersion()));
            MyTunesRssUtils.shutdownGracefully();
        }
        if (REGISTRATION.isExpiredPreReleaseVersion()) {
            MyTunesRssUtils.showErrorMessage(BUNDLE.getString("error.preReleaseVersionExpired"));
            MyTunesRssUtils.shutdownGracefully();
        } else if (REGISTRATION.isExpired() && HEADLESS) {
            MyTunesRssUtils.showErrorMessage(BUNDLE.getString("error.registrationExpiredHeadless"));
            MyTunesRssUtils.shutdownGracefully();
        } else if (REGISTRATION.isExpirationDate() && !REGISTRATION.isExpired()) {
            MyTunesRssUtils.showInfoMessage(MyTunesRssUtils.getBundleString("info.expirationInfo",
                    REGISTRATION.getExpiration(MyTunesRssUtils.getBundleString(
                            "common.dateFormat"))));
        }
        QUARTZ_SCHEDULER = new StdSchedulerFactory().getScheduler();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting quartz scheduler.");
        }
        QUARTZ_SCHEDULER.start();
        STREAMING_CACHE = FileCache.createCache(APPLICATION_IDENTIFIER + "_Streaming", 10000, CONFIG.getStreamingCacheMaxFiles());
        File streamingCacheFile = new File(MyTunesRssUtils.getCacheDataPath() + "/transcoder/cache.xml");
        if (streamingCacheFile.isFile()) {
            try {
                STREAMING_CACHE.setContent(JXPathUtils.getContext(streamingCacheFile.toURL()));
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Could not read streaming cache file. Starting with empty cache.", e);
                }
                STREAMING_CACHE.clearCache();
            }
        }
        ARCHIVE_CACHE = FileCache.createCache(APPLICATION_IDENTIFIER + "_Archives", 10000, 50); // TODO max size config?
        MyTunesRssJmxUtils.startJmxServer();
        StatisticsEventManager.getInstance().addListener(new StatisticsDatabaseWriter());
        if (HEADLESS) {
            executeHeadlessMode();
        } else {
            Thread.setDefaultUncaughtExceptionHandler(new MyTunesRssUncaughtHandler(ROOT_FRAME, false));
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        executeGuiMode();
                    } catch (Exception e) {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error(null, e);
                        }
                    }
                }
            });
        }
    }

    private static void initializeQuicktimePlayer() {
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                // try to find class
                Class.forName("quicktime.util.QTBuild");
                QUICKTIME_PLAYER = new QuicktimePlayer();
                LOGGER.info("Quicktime player created successfully.");
            } catch (ClassNotFoundException e) {
                LOGGER.info("No quicktime environment found, quicktime player disabled.");
            }
        }
    }

    private static String getFirstTrimmedArgument(Map<String, String[]> args, String name) {
        String[] values = args.get(name);
        if (values != null && values.length > 0) {
            return StringUtils.trimToNull(values[0]);
        }
        return null;
    }

    private static ClassLoader createExtraClassloader(File libDir) {
        try {
            Collection<File> files = libDir.isDirectory() ? (Collection<File>) FileUtils.listFiles(libDir, new String[]{"jar", "zip"}, false) : null;
            if (files != null && !files.isEmpty()) {
                Collection<URL> urls = new ArrayList<URL>();
                for (File file : files) {
                    urls.add(file.toURL());
                }
                return new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassLoader.getSystemClassLoader());
            }
        } catch (IOException e) {
            LOGGER.error("Could not create extra classloader.", e);
        }
        return null;
    }

    private static void registerDatabaseDriver()
            throws IOException, SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        File libDir = new File(MyTunesRssUtils.getPreferencesDataPath() + "/lib");
        ClassLoader classLoader = createExtraClassloader(libDir);
        String driverClassName = MyTunesRss.CONFIG.getDatabaseDriver();
        LOGGER.info("Using database driver class \"" + driverClassName + "\".");
        if (classLoader != null) {
            try {
                final Class<Driver> driverClass = (Class<Driver>) Class.forName(driverClassName, true, classLoader);
                DriverManager.registerDriver(new Driver() {
                    private Driver myDriver = driverClass.newInstance();

                    public Connection connect(String string, Properties properties) throws SQLException {
                        return myDriver.connect(string, properties);
                    }

                    public boolean acceptsURL(String string) throws SQLException {
                        return myDriver.acceptsURL(string);
                    }

                    public DriverPropertyInfo[] getPropertyInfo(String string, Properties properties) throws SQLException {
                        return myDriver.getPropertyInfo(string, properties);
                    }

                    public int getMajorVersion() {
                        return myDriver.getMajorVersion();
                    }

                    public int getMinorVersion() {
                        return myDriver.getMinorVersion();
                    }

                    public boolean jdbcCompliant() {
                        return myDriver.jdbcCompliant();
                    }
                });
            } catch (ClassNotFoundException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Database driver class not found.", e);
                }
                MyTunesRssUtils.showErrorMessage(MyTunesRssUtils.getBundleString("error.databaseDriverNotFound",
                        driverClassName,
                        libDir.getAbsolutePath()));
                MyTunesRssUtils.shutdownGracefully();
            } catch (SQLException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(null, e);
                }

            }
        } else {
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Database driver class not found.", e);
                }
                MyTunesRssUtils.showErrorMessage(MyTunesRssUtils.getBundleString("error.databaseDriverNotFound",
                        driverClassName,
                        libDir.getAbsolutePath()));
                MyTunesRssUtils.shutdownGracefully();
            }
        }
    }

    private static void executeGuiMode()
            throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException, IOException,
            InterruptedException, AWTException, SQLException {
        showNewVersionInfo();
        SETTINGS = new Settings();
        //DATABASE_FORM = SETTINGS.getDatabaseForm();
        MyTunesRssMainWindowListener mainWindowListener = new MyTunesRssMainWindowListener(SETTINGS);
        executeApple(SETTINGS);
        SYSTRAY = new MyTunesRssSystray(SETTINGS);
        SYSTRAY.setServerStopped();
        MyTunesRssEventManager.getInstance().addListener(SYSTRAY);
        ROOT_FRAME.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        ROOT_FRAME.addWindowListener(mainWindowListener);
        ROOT_FRAME.getContentPane().add(SETTINGS.getRootPanel());
        ROOT_FRAME.setResizable(false);
        //SETTINGS.setGuiMode(GuiMode.ServerIdle);
        SwingUtils.removeEmptyTooltips(ROOT_FRAME.getRootPane());
        int x = CONFIG.getWindowX();
        int y = CONFIG.getWindowY();
        if (CONFIG.isCheckUpdateOnStart()) {
            UpdateUtils.checkForUpdate(true);
        }
        while (true) {
            registerDatabaseDriver();
            String retry = MyTunesRssUtils.getBundleString("question.databaseInitError.retry");
            String shutdown = MyTunesRssUtils.getBundleString("question.databaseInitError.shutdown");
            String[] options = new String[]{retry, shutdown};
            InitializeDatabaseTask task = new InitializeDatabaseTask();
            MyTunesRssUtils.executeTask(null, BUNDLE.getString("pleaseWait.initializingDatabase"), null, false, task);
            if (task.getException() != null) {
                String answer = (String) MyTunesRssUtils.showQuestionMessage(MyTunesRssUtils.getBundleString("question.databaseInitError"), options);
                if (answer == retry) {
                    MyTunesRss.CONFIG.setDefaultDatabaseSettings();
                    new DeleteDatabaseFilesTask().execute();
                    continue; // retry
                } else {
                    MyTunesRssUtils.shutdownGracefully();
                }
            }
            if (task.getDatabaseVersion().compareTo(new Version(MyTunesRss.VERSION)) > 0) {
                String answer = (String)MyTunesRssUtils.showQuestionMessage(MyTunesRssUtils.getBundleString("question.databaseVersionMismatch", MyTunesRss.VERSION, task.getDatabaseVersion()), options);
                if (answer == retry) {
                    MyTunesRss.CONFIG.setDefaultDatabaseSettings();
                    new DeleteDatabaseFilesTask().execute();
                    continue; // retry
                } else {
                    MyTunesRssUtils.shutdownGracefully();
                }
            }
            break; // ok, continue with main flow
        }
        MyTunesRssJobUtils.scheduleStatisticEventsJob();
        MyTunesRssJobUtils.scheduleDatabaseJob();
        SETTINGS.init();
        ROOT_FRAME.setVisible(true);
        ROOT_FRAME.pack();
        if (x == Integer.MAX_VALUE && y == Integer.MAX_VALUE) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Initial start with no saved window postion, centering on screen.");
            }
            ROOT_FRAME.setLocationRelativeTo(null);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Setting window to (" + x + ", " + y + ")");
            }
            ROOT_FRAME.setLocation(x, y);
            ensureCompletelyOnScreen(ROOT_FRAME);
        }
        if (REGISTRATION.isExpired()) {
            MyTunesRssUtils.showErrorMessage(BUNDLE.getString("error.registrationExpired"));
            SETTINGS.forceRegistration();
            MyTunesRssUtils.shutdownGracefully();
        }
        if (CONFIG.isAutoStartServer()) {
            SETTINGS.doStartServer();
            if (SystemUtils.IS_OS_MAC_OSX) {
                ROOT_FRAME.setVisible(false);
            } else {
                ROOT_FRAME.setExtendedState(ROOT_FRAME.getExtendedState() | JFrame.ICONIFIED);
            }
            if (SYSTRAY.isAvailable()) {
                SYSTRAY.setMinimized();
            }
        }
    }

    private static void ensureCompletelyOnScreen(final JFrame frame) {
        SwingUtils.invokeAndWait(new Runnable() {
            public void run() {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                Point location = ROOT_FRAME.getLocation();
                Dimension size = ROOT_FRAME.getSize();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "Frame is (" + location.x + ", " + location.y + ") - (" + (location.x + size.width) + ", " + (location.y + size.height) +
                                    ")");
                }
                if (location.x >= screenSize.width || location.y >= screenSize.height || location.x + size.width <= 0 ||
                        location.y + size.height <= 0) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Frame is completely off-screen, centering it on screen.");
                    }
                    frame.setLocationRelativeTo(null);
                } else {
                    if (location.x < 0) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Frame is left off-screen.");
                        }
                        location.x = 0;
                    } else if (location.x + size.width > screenSize.width) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Frame is right off-screen.");
                        }
                        location.x = screenSize.width - size.width;
                    }
                    if (location.y < 0) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Frame is top off-screen.");
                        }
                        location.y = 0;
                    } else if (location.y + size.height > screenSize.height) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Frame is bottom off-screen.");
                        }
                        location.y = screenSize.height - size.height;
                    }
                    frame.setLocation(location);
                }
            }
        });
    }

    private static void showNewVersionInfo() {
        if (!VERSION.equals(CONFIG.getLastNewVersionInfo())) {
            try {
                String message = MyTunesRssUtils.getBundleString("info.newVersion", MyTunesRss.VERSION);
                if (StringUtils.isNotEmpty(message)) {
                    MyTunesRssUtils.showInfoMessage(ROOT_FRAME, message);
                }
            } catch (MissingResourceException e) {
                // intentionally left blank
            }
            CONFIG.setLastNewVersionInfo(VERSION);
        }
    }

    private static void executeHeadlessMode() throws IOException, SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Headless mode");
        }
        while (true) {
            registerDatabaseDriver();
            InitializeDatabaseTask task = new InitializeDatabaseTask();
            MyTunesRssUtils.executeTask(null, BUNDLE.getString("pleaseWait.initializingDatabase"), null, false, task);
            if (task.getException() != null) {
                MyTunesRssUtils.showErrorMessage(MyTunesRssUtils.getBundleString("error.databaseInitError"));
                if (isAutoResetDatabaseOnError()) {
                    LOGGER.info("Recreating default database.");
                    CONFIG.setDefaultDatabaseSettings();
                    try {
                        new DeleteDatabaseFilesTask().execute();
                        LOGGER.info("Starting retry.");
                        continue; // retry
                    } catch (IOException e) {
                        LOGGER.error("Could not delete database files.");
                        CONFIG.setDeleteDatabaseOnExit(true);
                    }
                }
                MyTunesRssUtils.shutdownGracefully();
            }
            if (task.getDatabaseVersion().compareTo(new Version(MyTunesRss.VERSION)) > 0) {
                MyTunesRssUtils.showErrorMessage(MessageFormat.format(MyTunesRssUtils.getBundleString("error.databaseVersionMismatch"), MyTunesRss.VERSION, task.getDatabaseVersion().toString()));
                if (isAutoResetDatabaseOnError()) {
                    LOGGER.info("Recreating default database.");
                    CONFIG.setDefaultDatabaseSettings();
                    try {
                        STORE.destroy();
                        new DeleteDatabaseFilesTask().execute();
                        LOGGER.info("Starting retry.");
                        continue; // retry
                    } catch (IOException e) {
                        LOGGER.error("Could not delete database files.");
                        CONFIG.setDeleteDatabaseOnExit(true);
                    }
                }
                MyTunesRssUtils.shutdownGracefully();
            }
            break; // ok, continue with main flow
        }
        MyTunesRssJobUtils.scheduleStatisticEventsJob();
        MyTunesRssJobUtils.scheduleDatabaseJob();
        if (CONFIG.isAutoStartServer()) {
            startWebserver();
        }
        while (!QUIT_REQUEST) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.debug("Main thread was interrupted in headless mode.", e);
                QUIT_REQUEST = true;
            }
        }
        LOGGER.debug("Quit request was TRUE.");
        MyTunesRssUtils.shutdownGracefully();
    }

    private static boolean isAutoResetDatabaseOnError() {
        return COMMAND_LINE_ARGS.containsKey("autoResetDatabaseOnError");
    }

    public static void startWebserver() {
        if (MyTunesRss.CONFIG.isUpdateDatabaseOnServerStart()) {
            MyTunesRssUtils.executeDatabaseUpdate();
        }
        MyTunesRssUtils.executeTask(null, BUNDLE.getString("pleaseWait.serverstarting"), null, false, new MyTunesRssTask() {
            public void execute() throws Exception {
                WEBSERVER.start();
            }
        });
        if (WEBSERVER.isRunning()) {
            if (MyTunesRss.CONFIG.isMyTunesRssComActive()) {
                MyTunesRssComUpdateTask myTunesRssComUpdater = new MyTunesRssComUpdateTask(SERVER_RUNNING_TIMER,
                        300000,
                        CONFIG.getMyTunesRssComUser(),
                        CONFIG.getMyTunesRssComPasswordHash());
                SERVER_RUNNING_TIMER.schedule(myTunesRssComUpdater, 0);
            }
            if (MyTunesRss.CONFIG.isAvailableOnLocalNet()) {
                MulticastService.startListener();
            }
            MyTunesRssEventManager.getInstance().fireEvent(MyTunesRssEvent.create(MyTunesRssEvent.EventType.SERVER_STARTED));
        }
    }

    public static void stopWebserver() {
        MyTunesRssUtils.executeTask(null, MyTunesRssUtils.getBundleString("pleaseWait.serverstopping"), null, false, new MyTunesRssTask() {
            public void execute() throws Exception {
                MyTunesRss.WEBSERVER.stop();
            }
        });
        if (!MyTunesRss.WEBSERVER.isRunning()) {
            MyTunesRss.SERVER_RUNNING_TIMER.cancel();
            MyTunesRss.SERVER_RUNNING_TIMER = new Timer("MyTunesRSSServerRunningTimer");
            MulticastService.stopListener();
            MyTunesRssEventManager.getInstance().fireEvent(MyTunesRssEvent.create(MyTunesRssEvent.EventType.SERVER_STOPPED));
        }
    }

    private static void executeApple(Settings settings) {
        LOGGER.debug("Trying to execute apple specific code.");
        if (SystemUtils.IS_OS_MAC_OSX) {
            try {
                LOGGER.debug("Executing apple specific code.");
                Class appleExtensionsClass = Class.forName("de.codewave.apple.AppleExtensions");
                Method activateMethod = appleExtensionsClass.getMethod("activate", EventListener.class);
                activateMethod.invoke(null, new AppleExtensionsEventListener(settings));
            } catch (Exception e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Could not activate apple extensions.", e);
                }
            }
        }
    }

    private static class MyTunesRssMainWindowListener extends WindowAdapter {
        enum QuitConfirmOption {
            Yes(), No(), NoButMinimize();

            @Override
            public String toString() {
                return MyTunesRssUtils.getBundleString("confirmation.quitMyTunesRss.option" + name());
            }
        }

        private Settings mySettingsForm;

        public MyTunesRssMainWindowListener(Settings settingsForm) {
            mySettingsForm = settingsForm;
        }

        @Override
        public void windowClosing(WindowEvent e) {
            if (SystemUtils.IS_OS_MAC_OSX) {
                LOGGER.debug("Window is being closed on Mac OS X, so the window is hidden now.");
                ROOT_FRAME.setVisible(false);
            } else {
                LOGGER.debug("Window is being closed, so the application is shut down now.");
                if (CONFIG.isQuitConfirmation()) {
                    LOGGER.debug("Shutdown confirmation enabled.");
                    int result = JOptionPane.showOptionDialog(ROOT_FRAME,
                            MyTunesRssUtils.getBundleString("confirmation.quitMyTunesRss"),
                            MyTunesRssUtils.getBundleString("pleaseWait.defaultTitle"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new QuitConfirmOption[]{QuitConfirmOption.NoButMinimize, QuitConfirmOption.No,
                                    QuitConfirmOption.Yes},
                            QuitConfirmOption.No);
                    if (result == 1) {
                        LOGGER.debug("Shutdown cancelled by user.");
                        return;
                    } else if (result == 0) {
                        LOGGER.debug("Shutdown cancelled by user and window will be iconified.");
                        ROOT_FRAME.setExtendedState(ROOT_FRAME.getExtendedState() | JFrame.ICONIFIED);
                        SYSTRAY.setMinimized();
                        return;
                    }
                }
                LOGGER.debug("Shutdown will be executed.");
                mySettingsForm.doQuitApplication();
            }
        }

        @Override
        public void windowIconified(WindowEvent e) {
            if (SYSTRAY.isAvailable() && CONFIG.isMinimizeToSystray()) {
                LOGGER.debug("Window has been iconified (state is " + ROOT_FRAME.getExtendedState() + ") and systray is available, so we hide the window now!");
                ROOT_FRAME.setVisible(false);
                SYSTRAY.setMinimized();
            }
        }
    }

    public static class AppleExtensionsEventListener implements EventListener {
        private Settings mySettings;

        public AppleExtensionsEventListener(Settings settings) {
            mySettings = settings;
        }

        public void handleQuit() {
            LOGGER.debug("Apple extension: handleQuit.");
            mySettings.doQuitApplication();
        }

        public void handleReOpenApplication() {
            LOGGER.debug("Apple extension: handleReOpenApplication.");
            if (!ROOT_FRAME.isVisible()) {
                LOGGER.debug("Root frame not visible, setting to visible.");
                ROOT_FRAME.setVisible(true);
            } else {
                LOGGER.debug("Root frame already visible, nothing to do here.");
            }
        }
    }
}
