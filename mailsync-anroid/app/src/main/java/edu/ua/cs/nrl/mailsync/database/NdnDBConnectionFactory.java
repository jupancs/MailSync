package edu.ua.cs.nrl.mailsync.database;

import android.content.Context;

public class NdnDBConnectionFactory {

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
}
