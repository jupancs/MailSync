package com.icegreen.greenmail.ndntranslator;

import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.ndnproxy.Snapshot;
import com.icegreen.greenmail.store.*;
import com.icegreen.greenmail.user.UserManager;
import net.named_data.jndn.Name;

import javax.mail.MessagingException;
import java.io.IOException;

public class TranslateWorker {
  public static void start(StoredMessage storedMessage)
      throws FolderException, IOException, MessagingException {
    // Initialize IMAP-to-NDN translators
    NdnTranslator attributeTranslator = TranslatorFactory.getNdnTranslator("IMAP", "Attribute");
    NdnTranslator mimeMessageTranslator = TranslatorFactory.getNdnTranslator("IMAP", "MimeMessage");
    NdnTranslator mailFolderTranslator = TranslatorFactory.getNdnTranslator("IMAP", "MailFolder");

    /**
     * Deal with attributes
     */
    String attributeName = attributeTranslator.generateAttributeName(
        "mailSync",
        "emailsynctest1@gmail.com",
        "inbox",
        "1",
        String.valueOf(storedMessage.getMimeMessage().getMessageID())
    );
    Name attributeNdnName = new Name(attributeName);
    attributeName = attributeNdnName.toUri();

    String attributeData = attributeTranslator
        .encodeAttribute((SimpleMessageAttributes) storedMessage.getAttributes());

    attributeTranslator.saveData(attributeName, attributeData, "Attribute");

    /**
     * Deal with MailFolder
     */
    Managers manager = ExternalProxy.gmail.getManagers();
    ImapHostManager imapHostManager = manager.getImapHostManager();
    UserManager userManager = manager.getUserManager();
    MailFolder mailFolder = imapHostManager
            .getInbox(userManager.getUserByEmail("emailsynctest1@gmail.com"));

    Snapshot snapshot = new Snapshot(0);
    snapshot.flags = MessageFlags.format(mailFolder.getPermanentFlags());
    snapshot.exists = mailFolder.getMessageCount();
    snapshot.recent = mailFolder.getRecentCount(false);
    snapshot.uidvalidity = mailFolder.getUidValidity();
    snapshot.uidnext = mailFolder.getUidNext();
    snapshot.unseen = mailFolder.getFirstUnseen();
    snapshot.complete = "READ-ONLY";
    snapshot.size = mailFolder.getMessageCount();
    for (int i = 0; i < mailFolder.getMessageCount(); i++) {
      String messageIDTmp = mailFolder.getMessages().get(i).getMimeMessage().getMessageID();
      snapshot.messageID.add(messageIDTmp);
    }

    String mailFolderName = mailFolderTranslator.generateMailFolderName(
        "mailSync",
        "emailsynctest1@gmail.com",
        "inbox",
        "1",
        String.valueOf(storedMessage.getUid())
    );
    String mailFolderData =
        mailFolderTranslator.encodeMailFolder(snapshot);

    mailFolderTranslator.saveData(mailFolderName, mailFolderData, "MailFolder");

    /**
     * Deal with MimeMessage
     */
    String mimeMessageName = mimeMessageTranslator.generateMimeMessageName(
        "mailSync",
        "emailsynctest1@gmail.com",
        "inbox",
        "1",
        String.valueOf(storedMessage.getMimeMessage().getMessageID())
    );
    Name mimeMessageNdnName = new Name(mimeMessageName);
    mimeMessageName = mimeMessageNdnName.toUri();

    String mimeMessageData =
        mimeMessageTranslator.encodeMimeMessage(storedMessage.getMimeMessage());

    int messageSize = storedMessage.getAttributes().getSize();
    // NDN packet upper bound is 8000
    int numberOfChunks = messageSize / 4000 + 1;
    int chunkLength = (int) Math.ceil(mimeMessageData.length() / (double) numberOfChunks);
    String[] chunks = new String[numberOfChunks];
    for (int i = 0; i < numberOfChunks; i++) {
      if (i == numberOfChunks - 1) {
        chunks[i] = mimeMessageData.substring(i * chunkLength, mimeMessageData.length());
      } else {
        chunks[i] = mimeMessageData.substring(i * chunkLength, (i + 1) * chunkLength);
      }
      mimeMessageTranslator.saveData(mimeMessageName + "/v" + i, chunks[i], "MimeMessage");
    }
  }
}
