package edu.ua.cs.nrl.mailsync.database;

import android.content.Context;

public class NdnDBConnectionFactory {
  private static String DEFAULT_DB =  "couchbaseLite";

  // Create a NdnDBConnection based on given db type
  public static NdnDBConnection getDBConnection(String db, Context context) {
    switch (db) {
      case "couchbaseLite":
        return new CouchbaseLiteConnection(context);
      case "couchbase":
        System.out.println("CouchbaseLite to be implemented ...");
      default:
        throw new IllegalArgumentException("Invalid db: " + db);
    }
  }

  public static NdnDBConnection getDBConnection(Context context, String databaseName) {
    return getDBConnection(DEFAULT_DB, context);
  }
}
