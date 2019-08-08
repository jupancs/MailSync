package edu.ua.cs.nrl.mailsync.database;

import android.content.Context;

import com.couchbase.lite.Blob;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;

import java.util.List;

public class CouchbaseLiteConnection implements NdnDBConnection {
  private DatabaseConfiguration config;
  private Database database;

  /**
   * Constructor
   */
  public CouchbaseLiteConnection(Context context) {
    // Get the database (and create it if it doesn’t exist).
    config = new DatabaseConfiguration(context);
  }

  @Override
  public void saveNDNData(String name, byte[] content, String databaseName)
      throws CouchbaseLiteException {
    try {
      database = new Database(databaseName, config);
    } catch (CouchbaseLiteException e) {
      e.printStackTrace();
    }

    Query query = QueryBuilder
        .select(SelectResult.property("name"))
        .from(DataSource.database(database))
        .where(Expression.property("name").equalTo(Expression.string(name)));

    ResultSet resultSet = query.execute();

    // Save it to database
    try {
      if (resultSet.allResults().size() == 0) {

        MutableDocument mutableDocument = new MutableDocument()
            .setString("name", name)
            .setBlob("content", new Blob("bytebuffer", content));

        database.save(mutableDocument);

      } else {
        System.out.println(">>> Duplicate name: "+ name);
      }
    } catch (CouchbaseLiteException e) {
      e.printStackTrace();
    }
  }

  @Override
  public DatabaseConfiguration getConfig() {
    System.out.println("Config" + config.toString());
    return config;
  }

  //Deletes ndn data
  //Has to be tested but is unneeded as of now
  @Override
  public void deleteNDNData(String name, String databaseName) throws CouchbaseLiteException {
    try {
      database = new Database(databaseName, config);
    } catch (CouchbaseLiteException e) {
      e.printStackTrace();
    }

    Query query = QueryBuilder
            .select(SelectResult.property("name"))
            .from(DataSource.database(database))
            .where(Expression.property("name").equalTo(Expression.string(name)));

    ResultSet resultSet = query.execute();
    List<Result> result = resultSet.allResults();
    String id = result.get(0).getString(0);
    Document document= database.getDocument(id);
    if(document!=null){
      database.delete(document);
    }


    // Save it to database
    try {
      if (resultSet.allResults().size() == 0) {

//        MutableDocument mutableDocument = new MutableDocument()
//                .setString("name", name)
//                .setBlob("content", new Blob("bytebuffer", content));
//
//        database.save(mutableDocument);
        System.out.println("No document to delete");

      } else {
//        System.out.println(">>> Duplicate name: "+ name);


      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
