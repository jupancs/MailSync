package com.icegreen.greenmail.database;

public class NdnDBConnectionFactory {

  private static String DEFAULT_DB =  "couchbase";

  // Create a NdnDBConnection based on given db type
  public static NdnDBConnection getDBConnection(String db) {
    switch (db) {
      case "couchbase":
        return new CouchbaseConnection("localhost", "jupan", "emailsync");
      case "couchbaseLite":
        System.out.println("CouchbaseLite to be implemented ...");
      default:
        throw new IllegalArgumentException("Invalid db: " + db);
    }
  }

  public static NdnDBConnection getDBConnection() {
    return getDBConnection(DEFAULT_DB);
  }
}
