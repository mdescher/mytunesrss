package de.codewave.mytunesrss.datastore;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssTestUtils;
import de.codewave.mytunesrss.config.DatabaseType;
import de.codewave.mytunesrss.config.MyTunesRssConfig;
import de.codewave.mytunesrss.datastore.statement.MigrationStatement;
import de.codewave.utils.sql.DataStoreSession;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class MigrationTest {

    @Test
    public void testStoreInitWithMigration() throws ClassNotFoundException, IOException, SQLException {
        File tempDir = MyTunesRssTestUtils.createTempDir();
        IOUtils.copy(getClass().getResourceAsStream("/testdatabase/MyTunesRSS.h2.db"), new FileOutputStream(new File(tempDir, "MyTunesRSS.h2.db")));
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        MyTunesRss.VERSION = "999.999.999";
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        MyTunesRss.CONFIG = new MyTunesRssConfig();
        MyTunesRss.CONFIG.setDatabaseType(DatabaseType.h2);
        MyTunesRss.CONFIG.setDatabaseConnection("jdbc:h2:file:" + tempDir.getAbsolutePath() + "/MyTunesRSS;DB_CLOSE_DELAY=-1");
        MyTunesRss.CONFIG.setDatabaseUser("sa");
        MyTunesRss.CONFIG.setDatabasePassword("");
        Class.forName("org.h2.Driver");
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        MyTunesRss.STORE = new MyTunesRssDataStore();
        MyTunesRss.STORE.init();
        DataStoreSession session = MyTunesRss.STORE.getTransaction();
        try {
            session.executeStatement(new MigrationStatement());
            session.commit();
        } finally {
            session.rollback();
        }
    }
}
