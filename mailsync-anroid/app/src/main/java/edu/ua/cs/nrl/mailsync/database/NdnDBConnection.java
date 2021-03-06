package edu.ua.cs.nrl.mailsync.database;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;

import java.util.List;

public interface NdnDBConnection {

  /**
   * Save name-data into database on Android
   *
   * @param name
   * @param content
   * @param databaseName
   */
  void saveNDNData(String name, byte[] content, String databaseName)
      throws CouchbaseLiteException;

  /**
   * Get the database configuration
   *
   * @return
   */
  DatabaseConfiguration getConfig();
}
