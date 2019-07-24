package com.icegreen.greenmail.ndnproxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.icegreen.greenmail.database.NdnDBConnection;
import com.icegreen.greenmail.database.NdnDBConnectionFactory;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.OnData;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;

import com.icegreen.greenmail.ExternalProxy;
import org.json.JSONObject;

import static com.couchbase.client.java.query.Select.select;

public class NDNMailSyncConsumerProducer implements OnData, OnTimeout,
    OnInterestCallback, OnRegisterFailed {

  private Boolean result;
  public static final String PREFIX_DISCOVERY = "/mailSync/DISCOVERY";
  public int callbackCount_ = 0;
  public String hostName;
  public String contentString = "";
  public KeyChain keyChain_;
  public Name certificateName_;
  public int responseCount_ = 0;

  public static Snapshot mailbox;

  public NDNMailSyncConsumerProducer(KeyChain keyChain, Name certificateName) {
    keyChain_ = keyChain;
    certificateName_ = certificateName;
  }

  public void
  onData(Interest interest, Data data) {
    ++callbackCount_;

    Name name = data.getName();
    String adu = name.get(4).toEscapedString();

    // System.out.println("Got data packet with name: " + name.toUri());
    // System.out.println("adu: " + adu);

    ByteBuffer content = data.getContent().buf();

//    System.out.println("Sig: ======= " + data.getSignature().getSignature().toHex());

    if (adu.equals("CAPABILITY")) {
      CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(content);
      contentString = charBuffer.toString();
    } else if (adu.equals("MailFolder")) {
      CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(content);
      contentString = charBuffer.toString();
    } else if (adu.equals("attribute")) {
      CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(content);
      contentString = charBuffer.toString();
    } else if (adu.equals("MimeMessage")) {
      CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(content);
      contentString = charBuffer.toString();
    } else if (adu.equals("probe")) {
      contentString = StandardCharsets.UTF_8.decode(content).toString();
//      System.out.println("content: : " + content.);
//      CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(content);
//      System.out.println("charbuffer: " + charBuffer.toString());
//      contentString = charBuffer.toString();
//      System.out.println("contentString: " + contentString);
    }

    ExternalProxy.setNDNResult(true);
  }
/**
 * OnTimeout checks if retransmission max is 0 if not then tries again to send interest packet
 */
  public void onTimeout(Interest interest) {
    ++callbackCount_;
    System.out.println("Time out for interest " + interest.getName().toUri());
    EmailFactory.retransmitInterest(interest.getName().toUri());
    if(ExternalProxy.retransmissionMax == 0){
      ExternalProxy.setNDNResult(true);
    }
   
  }

  public String getContentString() {
    return contentString;
  }

  public void
  onInterest(Name prefix, Interest interest, Face face,
             long interestFilterId, InterestFilter filter) {
    ++responseCount_;
    System.out.println(Integer.toString(responseCount_) + " Interests received");

    Name name = interest.getName();
    String user = name.get(1).toEscapedString();
    String adu = name.get(4).toEscapedString();

    System.out.println("name: " + name.toUri());
    System.out.println("user: " + user);
    System.out.println("adu: " + adu);

    String contentString = new String();

    // Setup the database
    NdnDBConnection ndnDBConnection = NdnDBConnectionFactory.getDBConnection("couchbase");

    if (adu.equals("CAPABILITY")) {
      contentString = "IMAP4rev1 LITERAL+ SORT UIDPLUS";
    } else if (adu.equals("MailFolder")) {
      try {
        // Create primary index
        ndnDBConnection.getNdnCluster().openBucket("MailFolder")
            .bucketManager().createN1qlPrimaryIndex(true, false);

        // Retrieve the content field from "Attribute" bucket
        N1qlQueryResult mailFolderResult =
            ndnDBConnection.getNdnCluster().openBucket("MailFolder").query(
                N1qlQuery.simple(select("content").from("MailFolder"))
            );

        // Only retrieve the first (latest) MailFolder data in the database
        for (N1qlQueryRow row : mailFolderResult) {
          // Get content value in database
          JSONObject obj = new JSONObject(row.toString());
          contentString = obj.getString("content");
        }
      } catch (Exception e) {
        System.out.println(e);
      }

    } else if (adu.equals("attribute")) {

      ndnDBConnection.getNdnCluster().openBucket("Attribute")
          .bucketManager().createN1qlPrimaryIndex(true, false);

      String queryAttribute = "SELECT content FROM `Attribute` WHERE meta().id = "
          + "\"" + name.toString() + "\"";

      N1qlQueryResult attributeResult = ndnDBConnection.getNdnCluster().openBucket("Attribute")
          .query(N1qlQuery.simple(queryAttribute));

      for (N1qlQueryRow row : attributeResult) {
        JSONObject obj = new JSONObject(row.toString());
        contentString = obj.getString("content");
        System.out.println("**** Attriubte ADU: " + contentString);
      }

    } else if (adu.equals("MimeMessage")) {
      ndnDBConnection.getNdnCluster().openBucket("MimeMessage")
          .bucketManager().createN1qlPrimaryIndex(true, false);

      String queryMimeMessage = "SELECT content FROM `MimeMessage` WHERE meta().id = "
          + "\"" + name.toString() + "\"";

      N1qlQueryResult mimeMessageResult = ndnDBConnection.getNdnCluster().openBucket("MimeMessage")
          .query(N1qlQuery.simple(queryMimeMessage));

      for (N1qlQueryRow row : mimeMessageResult) {
        JSONObject obj = new JSONObject(row.toString());
        contentString = obj.getString("content");

        System.out.println("**** MimeMessage ADU: " + contentString);
      }
    }

    System.out.println("Sent content string: " + contentString);

    // Make and sign a Data packet.
    Data data = new Data(interest.getName());

    data.setContent(new Blob(contentString));

    try {
      keyChain_.sign(data, certificateName_);
    } catch (SecurityException exception) {
      // Don't expect this to happen.
      throw new Error("SecurityException in sign: " + exception.getMessage());
    }

    try {
      face.putData(data);
    } catch (IOException ex) {
      System.out.println("Echo: IOException in sending data " + ex.getMessage());
    }
  }

  public void
  onRegisterFailed(Name prefix) {
    ++responseCount_;
    System.out.println("Register failed for prefix " + prefix.toUri());
  }
}
