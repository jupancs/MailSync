package com.icegreen.greenmail.database;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * APIs for operating NDN database
 */
public class CouchbaseConnection implements NdnDBConnection {
  private Cluster ndnCluster;

  public CouchbaseConnection(String serverAddress, String user, String password) {
    // Connects to local Couchbase server
    ndnCluster = CouchbaseCluster.create(serverAddress);
    ndnCluster.authenticate(user, password);
  }

  @Override
  public Cluster getNdnCluster() {
    return ndnCluster;
  }

  @Override
  public void saveNDNData(String name, String content, String bucketName) {
    // Create a JSON Document
    JsonObject data = JsonObject.create()
        .put("content", content);

    Bucket ndnBucket = ndnCluster.openBucket(bucketName);

    // Store the Document
    ndnBucket.upsert(JsonDocument.create(name, data));

    // Load the Document and print it
    // Prints Content and Metadata of the stored Document
    System.out.println(ndnBucket.get(name));
  }
}
