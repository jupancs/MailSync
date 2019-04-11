package com.icegreen.greenmail.ndntranslator;

import android.content.Context;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.google.common.io.BaseEncoding;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.ndnproxy.NdnFolder;
import com.icegreen.greenmail.ndnproxy.Snapshot;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.MessageFlags;
import com.icegreen.greenmail.store.SimpleMessageAttributes;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.UserManager;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import edu.ua.cs.nrl.mailsync.database.NdnDBConnection;
import edu.ua.cs.nrl.mailsync.database.NdnDBConnectionFactory;
import edu.ua.cs.nrl.mailsync.fragments.MainServerFragment;

public class TranslateWorker {

  public static void start(MimeMessage mimeMessage, Context context, final FragmentActivity mainActivity) throws
      FolderException, IOException, CouchbaseLiteException, MessagingException {
    // Initialize IMAP-to-NDN translators
    NdnTranslator ndnTranslator = TranslatorFactory.getNdnTranslator("IMAP", context);

    NdnDBConnection ndnDBConnection = NdnDBConnectionFactory.getDBConnection(
        "couchbaseLite",
        context
    );

    /**
     * Deal with attributes
     *
     */
    String attributeName = ndnTranslator.generateAttributeName(
        "mailSync",
        ExternalProxy.userEmail,
        "inbox",
        "1",
        String.valueOf(mimeMessage.getMessageID())
    );
    Name attributeNdnName = new Name(attributeName);
    attributeName = attributeNdnName.toUri();

    byte[] attributeData = ndnTranslator.encodeAttribute(
        new SimpleMessageAttributes(mimeMessage, mimeMessage.getReceivedDate()), attributeNdnName
    );

    ndnTranslator.saveData(attributeName, attributeData, "Attribute");

    /**
     * Deal with MailFolder
     *
     */
//    Managers manager = ExternalProxy.gmail.getManagers();
//    ImapHostManager imapHostManager = manager.getImapHostManager();
//    UserManager userManager = manager.getUserManager();
//    MailFolder mailFolder = imapHostManager
//        .getInbox(userManager.getUserByEmail(ExternalProxy.userEmail));
//
//    Query query = QueryBuilder
//        .select(SelectResult.property("name"))
//        .from(DataSource.database(new Database("MessageID", ndnDBConnection.getConfig())));
//
//    ResultSet resultSet = query.execute();
//
//    Snapshot snapshot = new Snapshot(0);
//    snapshot.flags = MessageFlags.format(mailFolder.getPermanentFlags());
//    snapshot.exists = mailFolder.getMessageCount();
//    snapshot.recent = mailFolder.getRecentCount(false);
//    snapshot.uidvalidity = mailFolder.getUidValidity();
//    snapshot.uidnext = mailFolder.getUidNext();
//    snapshot.unseen = mailFolder.getFirstUnseen();
//    snapshot.complete = "READ-ONLY";
//    snapshot.size = resultSet.allResults().size();
//
//    for (int i = 0; i < mailFolder.getMessageCount(); i++) {
//      String messageIDTmp = mailFolder.getMessages().get(i).getMimeMessage().getMessageID();
//      snapshot.messageID.add(messageIDTmp);
//    }

    String mailFolderName = ndnTranslator.generateMailFolderName(
        "mailSync",
        ExternalProxy.userEmail,
        "inbox",
        "1",
        String.valueOf(mimeMessage.getMessageID())
    );
//    System.out.println(String.valueOf("MSN: " + NdnFolder.getMsn(NdnFolder.folder.getUID(mimeMessage))) + " Size: " + NdnFolder.folder.getMessageCount());
    Name mailFolderNdnName = new Name(mailFolderName);
    mailFolderName = mailFolderNdnName.toUri();

    byte[] mailFolderData =
        ndnTranslator.encodeMailFolder(NdnFolder.getSnapshot(), mailFolderNdnName);
//        ndnTranslator.encodeMailFolder(snapshot, mailFolderNdnName);

    System.out.println("MailFolder name: " + mailFolderName);
    ndnTranslator.saveData(mailFolderName, mailFolderData, "MailFolder");

    /**
     * Deal with MimeMessage
     *
     */
    String mimeMessageName = ndnTranslator.generateMimeMessageName(
        "mailSync",
        ExternalProxy.userEmail,
        "inbox",
        "1",
        String.valueOf(mimeMessage.getMessageID())
    );

    Name mimeMessageNdnName = new Name(mimeMessageName);
    mimeMessageName = mimeMessageNdnName.toUri();

//    byte[] mimeMessageData =
//        ndnTranslator.encodeMimeMessage(mimeMessage.getMimeMessage(), mimeMessageNdnName);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    byte[] byteArray;

    mimeMessage.writeTo(baos);

    oos.flush();
    byteArray = baos.toByteArray();
    String contentString = BaseEncoding.base64().encode(byteArray);

    // NDN packet upper bound is 8000
    int messageSize = mimeMessage.getSize();
    int numberOfChunks = messageSize / 4000 + 1;
    int chunkLength = (int) Math.ceil(contentString.length() / (double) numberOfChunks);
    String[] chunks = new String[numberOfChunks];
    for (int i = 0; i < numberOfChunks; i++) {
      if (i == numberOfChunks - 1) {
        chunks[i] = contentString.substring(i * chunkLength, contentString.length());
      } else {
        chunks[i] = contentString.substring(i * chunkLength, (i + 1) * chunkLength);
      }

      Name name = new Name(mimeMessageName + "/v" + i);
      Data data = new Data(name);
      data.setContent(new Blob(chunks[i]));

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

      ndnTranslator.saveData(mimeMessageName + "/v" + i, result, "MimeMessage");

    }
    Handler h = new Handler(mainActivity.getApplicationContext().getMainLooper());

    h.post(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(mainActivity, "Saved", Toast.LENGTH_SHORT).show();
      }
    });
  }
}
