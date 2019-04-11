package com.icegreen.greenmail.ndntranslator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.common.io.BaseEncoding;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.ndnproxy.NdnFolder;
import com.icegreen.greenmail.ndnproxy.Snapshot;
import com.icegreen.greenmail.store.SimpleMessageAttributes;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import edu.ua.cs.nrl.mailsync.database.NdnDBConnection;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnectionFactory;
import edu.ua.cs.nrl.mailsync.fragments.BaseFragment;

public class ImapToNdnTranslator extends BaseFragment implements NdnTranslator {

  private NdnDBConnection ndnDBConnection;

  public ImapToNdnTranslator() {}

  @SuppressLint("ValidFragment")
  public ImapToNdnTranslator(Context context) {
    ndnDBConnection = NdnDBConnectionFactory.getDBConnection("couchbaseLite", context);
  }

  @Override
  public byte[] encodeAttribute(SimpleMessageAttributes attributes, Name name) {
    byte[] encodedString = null;
    try {
      encodedString = encodeHelper(attributes, name);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MessagingException e) {
      e.printStackTrace();
    }

    return encodedString;
  }

  @Override
  public byte[] encodeMimeMessage(MimeMessage mimeMessage, Name name) {
    byte[] encodedString = null;
    try {
      encodedString = encodeHelper(mimeMessage, name);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MessagingException e) {
      e.printStackTrace();
    }
    return encodedString;
  }

  @Override
  public byte[] encodeMailFolder(Snapshot snapshot, Name name) {
    byte[] encodedString = null;
    try {
      encodedString = encodeHelper(snapshot, name);
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
    StringBuilder nameBuilder = new StringBuilder();
    nameBuilder.append("/").append(appName).append("/").append(userName).append("/")
        .append(mailboxName).append("/").append(mailboxVersion).append("/attribute")
        .append("/").append(messageUID);
    return nameBuilder.toString();
  }

  @Override
  public String generateMimeMessageName(String appName, String userName, String mailboxName,
                                        String mailboxVersion, String messageUID) {
    StringBuilder nameBuilder = new StringBuilder();
    nameBuilder.append("/").append(appName).append("/").append(userName).append("/")
        .append(mailboxName).append("/").append(mailboxVersion).append("/MimeMessage")
        .append("/").append(messageUID);
    return nameBuilder.toString();
  }

  @Override
  public String generateMailFolderName(String appName, String userName, String mailboxName,
                                       String mailboxVersion, String messageUID) {
    StringBuilder nameBuilder = new StringBuilder();
    nameBuilder.append("/").append(appName).append("/").append(userName).append("/")
        .append(mailboxName).append("/").append(mailboxVersion).append("/MailFolder").append("/")
        .append(messageUID);
    return nameBuilder.toString();
  }


  @Override
  public void saveData(String name, byte[] content, String databaseName)
      throws CouchbaseLiteException {
    ndnDBConnection.saveNDNData(name, content, databaseName);


    final Context act = this.getActivity();

  }

  // Encoding helper method
  private byte[] encodeHelper(Object object, Name name) throws IOException, MessagingException {
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
      name = new Name("/mailSync/"+ ExternalProxy.userEmail + "/inbox/1/MailFolder/1");
      Snapshot mailFolder = (Snapshot) object;
      oos.writeObject(mailFolder);
    } else {
      throw new IllegalArgumentException("Invalid object type!");
    }
    oos.flush();
    byteArray = baos.toByteArray();
    String contentString = BaseEncoding.base64().encode(byteArray);

    Data data = new Data(name);
    data.setContent(new Blob(contentString));

    KeyChain keyChain = ExternalProxy.ndnMailSyncOneThread.keyChain_;
    Name certificateName = ExternalProxy.ndnMailSyncOneThread.certificateName_;

    try {
      keyChain.sign(data, certificateName);
    } catch (SecurityException e) {
      e.printStackTrace();
    }

    // Get SingedBuffer
    Blob encoding = data.wireEncode();
    byte[] result = encoding.getImmutableArray();

    return result;
  }
}
