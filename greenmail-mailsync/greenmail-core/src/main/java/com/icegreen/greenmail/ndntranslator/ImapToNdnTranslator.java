package com.icegreen.greenmail.ndntranslator;

import com.google.common.io.BaseEncoding;
import com.icegreen.greenmail.database.NdnDBConnection;
import com.icegreen.greenmail.database.NdnDBConnectionFactory;
import com.icegreen.greenmail.ndnproxy.Snapshot;
import com.icegreen.greenmail.store.SimpleMessageAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ImapToNdnTranslator implements NdnTranslator{

  private NdnDBConnection ndnDBConnection;

  public ImapToNdnTranslator(String bucketName) {
    // Initialize the database
    ndnDBConnection = NdnDBConnectionFactory.getDBConnection("couchbase");
  }

  @Override
  public String encodeAttribute(SimpleMessageAttributes attributes) {
    String encodedString = null;
    try {
      encodedString = encodeHelper(attributes);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MessagingException e) {
      e.printStackTrace();
    }
    return encodedString;
  }

  @Override
  public String encodeMimeMessage(MimeMessage mimeMessage) {
    String encodedString = null;
    try {
      encodedString = encodeHelper(mimeMessage);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MessagingException e) {
      e.printStackTrace();
    }
    return encodedString;
  }

  @Override
  public String encodeMailFolder(Snapshot snapshot) {
    String encodedString = null;
    try {
      encodedString = encodeHelper(snapshot);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MessagingException e) {
      e.printStackTrace();
    }
    return encodedString;
  }

  @Override
  public String generateAttributeName(String appName, String userName, String mailboxName,
                                      String mailboxVersion, String messageUID) {
    String name = "/" + appName + "/" + userName + "/" + mailboxName + "/" + mailboxVersion
        + "/attribute" + "/" + messageUID;
    return name;
  }

  @Override
  public String generateMimeMessageName(String appName, String userName, String mailboxName,
                                        String mailboxVersion, String messageUID) {
    String name = "/" + appName + "/" + userName + "/" + mailboxName + "/" + mailboxVersion
        + "/MimeMessage" + "/" + messageUID;
    return name;
  }

  @Override
  public String generateMailFolderName(String appName, String userName, String mailboxName,
                                       String mailboxVersion, String messageUID) {
    String name = "/" + appName + "/" + userName + "/" + mailboxName + "/" + mailboxVersion
        + "/MailFolder" + "/" + messageUID;
    return name;
  }

  @Override
  public void saveData(String name, String content, String bucketName) {
    ndnDBConnection.saveNDNData(name, content, bucketName);
  }

  // Encoding helper method
  private String encodeHelper(Object object) throws IOException, MessagingException {
    String result;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    byte[] byteArray;
    if (object instanceof SimpleMessageAttributes) {
      SimpleMessageAttributes attributes = (SimpleMessageAttributes) object;
      oos.writeObject(attributes);
    } else if (object instanceof MimeMessage) {
      MimeMessage mimeMessage = (MimeMessage) object;
      mimeMessage.writeTo(baos);
    } else if (object instanceof Snapshot) {
      Snapshot mailFolder = (Snapshot) object;
      oos.writeObject(mailFolder);
    } else {
      throw new IllegalArgumentException("Invalid object type!");
    }
    oos.flush();
    byteArray = baos.toByteArray();
    result = BaseEncoding.base64().encode(byteArray);
    return result;
  }
}
