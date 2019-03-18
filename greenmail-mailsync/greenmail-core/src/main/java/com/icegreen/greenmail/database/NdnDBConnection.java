package com.icegreen.greenmail.database;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;

public interface NdnDBConnection {
  /**
   * Save name-data into database
   *
   * @param name
   * @param content
   */
  public void saveNDNData(String name, String content, String bucketName);

  /**
   * Get NDN bucket of a database
   *
   * @return
   */
  public Cluster getNdnCluster();
}
