/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import java.io.*;
import java.util.Properties;

import com.google.common.io.BaseEncoding;
import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.ndnproxy.NDNMailSyncConsumerProducer;
import com.icegreen.greenmail.ndnproxy.NdnFolder;
import com.icegreen.greenmail.ndnproxy.Snapshot;
import com.icegreen.greenmail.store.*;

import com.icegreen.greenmail.ExternalProxy;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;


/**
 * Handles processeing for the SELECT imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class SelectCommand extends AuthenticatedStateCommand {
  public static final String NAME = "SELECT";
  public static final String ARGS = "mailbox";
  public int lastSize = 0;

  private int cnt = 0;

  SelectCommand() {
    super(NAME, ARGS);
  }

  SelectCommand(String name) {
    super(name, null);
  }

  @Override
  protected void doProcess(ImapRequestLineReader request,
                           ImapResponse response,
                           ImapSession session)
      throws ProtocolException, FolderException, IOException, ClassNotFoundException, MessagingException {
    String mailboxName = parser.mailbox(request);
    parser.endLine(request);

    session.deselect();

    final boolean isExamine = this instanceof ExamineCommand;
    selectMailbox(mailboxName, session, isExamine);
    System.out.println(":::::::::: " + mailboxName);

    if (mailboxName.equals("INBOX")) {

      int selectedNetwork = ExternalProxy.getSelectedProxy();
      System.out.println(">>> Select <<<");

      if (selectedNetwork == 1) {
        // Using NDN service
        session.deselect();

//        final boolean isExamine = this instanceof ExamineCommand;
        selectMailbox(mailboxName, session, isExamine);

        // Express NDN Interest
        ExternalProxy.expressInterest("/mailSync/emailsynctest1@gmail.com/inbox/1/MailFolder/1");
        ExternalProxy.setNDNResult(false);

        while (!ExternalProxy.getNDNResult()) {
          synchronized (ExternalProxy.monitor) {
            try {
              ExternalProxy.monitor.wait();
            } catch (Exception e) {
              System.out.println(e);
            }
          }
        }
        System.out.println("the result is true");

        // Decode the serialized string to Snapshot
        String contentString = ExternalProxy.getContentString();
        String value = contentString;
        ObjectInputStream ois;

        byte[] decodeByteArray = BaseEncoding.base64().decode(value);
        ByteArrayInputStream bais = new ByteArrayInputStream(decodeByteArray);
        ois = new ObjectInputStream(bais);
        Snapshot mailFolder = (Snapshot) ois.readObject();

        // Update the CP mailfolder (Snapshot) information
        NDNMailSyncConsumerProducer.mailFolder = mailFolder;

        try {
          response.flagsStringResponse(mailFolder.flags);
          response.existsResponse(mailFolder.exists);
          response.recentResponse(mailFolder.recent);
          response.okResponse("UIDVALIDITY " + String.valueOf(mailFolder.uidvalidity), null);
          response.okResponse("UIDNEXT " + String.valueOf(mailFolder.uidnext), null);

          int firstUnseen = mailFolder.unseen;
          if (firstUnseen > 0) {
            response.okResponse("UNSEEN " + firstUnseen,
                "Message " + firstUnseen + " is the first unseen");
          } else {
            response.okResponse(null, "No messages unseen");
          }

          response.flagsStringResponse(mailFolder.flags);
          response.commandComplete(this, "READ-ONLY");

        } catch (Exception e) {
          e.printStackTrace();
        }

        // Send out Attribute Interest according to the mailbox details
        Snapshot snapshot = NDNMailSyncConsumerProducer.mailFolder;
        int size = snapshot.size;
        System.out.println(">>>>>> Size: " + size);
        if (size > lastSize) {
          for (int i = 0; i < size; i++) {
            String messageID = snapshot.messageID.get(i);
            ExternalProxy.expressInterest("/mailSync/emailsynctest1@gmail.com/inbox/1/attribute/"
                + messageID);

            ExternalProxy.setNDNResult(false);
            while (!ExternalProxy.getNDNResult()) {
              synchronized (ExternalProxy.monitor) {
                try {
                  ExternalProxy.monitor.wait();
                } catch (Exception e) {
                  System.out.println(e);
                }
              }
            }

            String contentStringAttribute = ExternalProxy.getContentString();
            String attributeValue = contentStringAttribute;
            ObjectInputStream attributeOIS;

            // Decode the serialized string to Attribute
            byte[] attributeDecodedByteArray = BaseEncoding.base64().decode(attributeValue);
            ByteArrayInputStream attributeBAIS = new ByteArrayInputStream(attributeDecodedByteArray);
            attributeOIS = new ObjectInputStream(attributeBAIS);
            SimpleMessageAttributes attribute = (SimpleMessageAttributes) attributeOIS.readObject();

            // Deal with MimeMessage Interest
            int numberOfMessage = attribute.getSize() / 4000 + 1;
            StringBuilder messageBuilder = new StringBuilder();
            String mimeMessageContentString = null;
            for (int j = 0; j < numberOfMessage; j++) {
              ExternalProxy.expressInterest("/mailSync/emailsynctest1@gmail.com/inbox/1/MimeMessage/"
                  + messageID + "/v" + j);
              ExternalProxy.setNDNResult(false);
              while (!ExternalProxy.getNDNResult()) {
                synchronized (ExternalProxy.monitor) {
                  try {
                    ExternalProxy.monitor.wait();
                  } catch (Exception e) {
                    System.out.println(e);
                  }
                }
              }
              mimeMessageContentString = ExternalProxy.getContentString();
              messageBuilder.append(mimeMessageContentString);
            }
            String valueMimeMessage = messageBuilder.toString();
            ObjectInputStream oisMimeMessage;

            byte[] decodeByteArrayMimeMessage = BaseEncoding.base64().decode(valueMimeMessage);
            ByteArrayInputStream baisMimeMessage =
                new ByteArrayInputStream(decodeByteArrayMimeMessage);

            try {
              ExternalProxy.storeInGreenStorageNDN(baisMimeMessage);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          lastSize = size;
        }

      } else if (selectedNetwork == 2) {
        //      // Using local server
        //      session.deselect();
        //
        //      final boolean isExamine = this instanceof ExamineCommand;
        //      selectMailbox(mailboxName, session, isExamine);
        //
        //      ImapSessionFolder mailbox = session.getSelected();
        //
        //      response.flagsResponse(mailbox.getPermanentFlags());
        //      response.existsResponse(mailbox.getMessageCount());
        //
        //      final boolean resetRecent = !isExamine;
        //      response.recentResponse(mailbox.getRecentCount(resetRecent));
        //      response.okResponse("UIDVALIDITY " + mailbox.getUidValidity(), null);
        //      response.okResponse("UIDNEXT " + mailbox.getUidNext(), null);
        //
        //      int firstUnseen = mailbox.getFirstUnseen();
        //      if (firstUnseen > 0) {
        //        response.okResponse("UNSEEN " + firstUnseen,
        //            "Message " + firstUnseen + " is the first unseen");
        //      } else {
        //        response.okResponse(null, "No messages unseen");
        //      }
        //
        //      response.permanentFlagsResponse(mailbox.getPermanentFlags());
        //
        //      if (mailbox.isReadonly()) {
        //        response.commandComplete(this, "READ-ONLY");
        //      } else {
        //        response.commandComplete(this, "READ-WRITE");
        //      }

        // Using local server

        try {
          Properties props = new Properties();
          props.setProperty("mail.store.protocol", "imaps");
          props.setProperty("mail.imap.host", "imap.gmail.com");
          Session sessionIMAP = Session.getInstance(props, null);
          Store store = sessionIMAP.getStore("imaps");
          store.connect("imap.gmail.com", ExternalProxy.userEmail, ExternalProxy.userPassword);
          Folder emailFolder = store.getFolder("INBOX");
          emailFolder.open(Folder.READ_WRITE);
          NdnFolder.folder = (IMAPFolder) emailFolder;

          if (NdnFolder.folder != null && NdnFolder.folder.exists()) {
            if (NdnFolder.folder.isOpen() && NdnFolder.folder.getMode() != Folder.READ_WRITE) {
              NdnFolder.folder.close(false);
              NdnFolder.folder.open(Folder.READ_WRITE);
            } else if (!NdnFolder.folder.isOpen()) {
              NdnFolder.folder.open(Folder.READ_WRITE);
            }
          } else {
            String errMsg = "Folder not found";
            System.out.println(errMsg);
          }

          System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^");
          System.out.println(" Ding Ding");
          Snapshot snapshot = NdnFolder.getSnapshot();
          System.out.println(" Da Da");
          System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^");

          response.flagsStringResponse(snapshot.flags);
          response.existsResponse(snapshot.size);

          final boolean resetRecent = !isExamine;
          response.recentResponse(snapshot.recent);
          response.okResponse("UIDVALIDITY " + snapshot.uidvalidity, null);
          response.okResponse("UIDNEXT " + snapshot.uidnext, null);

          int firstUnseen = snapshot.unseen;
          if (firstUnseen > 0) {
            response.okResponse("UNSEEN " + firstUnseen,
                "Message " + firstUnseen + " is the first unseen");
          } else {
            response.okResponse(null, "No messages unseen");
          }

          response.flagsStringResponse(snapshot.flags);
          response.commandComplete(this, "READ-WRITE");


          store.close();

        } catch (NoSuchProviderException e) {
          e.printStackTrace();
        } catch (MessagingException e) {
          e.printStackTrace();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }



//      // Save Mailbox snapshot into NdnStorage
//      NdnTranslator ndnTranslator =
//          TranslatorFactory.getNdnTranslator("IMAP", ExternalProxy.context);
//      NdnDBConnection ndnDBConnection = NdnDBConnectionFactory.getDBConnection(
//          "couchbaseLite",
//          ExternalProxy.context
//      );
//
//      Managers manager = ExternalProxy.gmail.getManagers();
//      ImapHostManager imapHostManager = manager.getImapHostManager();
//      UserManager userManager = manager.getUserManager();
//      MailFolder mailFolder = imapHostManager
//          .getInbox(userManager.getUserByEmail(ExternalProxy.userEmail));
//
//      Query query = null;
//      try {
//        query = QueryBuilder
//            .select(SelectResult.property("name"))
//            .from(DataSource.database(new Database("MessageID", ndnDBConnection.getConfig())));
//        ResultSet resultSet = query.execute();
//        Snapshot contentObj = new Snapshot(0);
//        contentObj.flags = MessageFlags.format(mailFolder.getPermanentFlags());
//        contentObj.exists = mailFolder.getMessageCount();
//        contentObj.recent = mailFolder.getRecentCount(false);
//        contentObj.uidvalidity = mailFolder.getUidValidity();
//        contentObj.uidnext = mailFolder.getUidNext();
//        contentObj.unseen = mailFolder.getFirstUnseen();
//        contentObj.complete = "READ-ONLY";
//        contentObj.size = resultSet.allResults().size();
//
//        for (int i = 0; i < mailFolder.getMessageCount(); i++) {
//          String messageIDTmp = mailFolder.getMessages().get(i).getMimeMessage().getMessageID();
//          contentObj.messageID.add(messageIDTmp);
//        }
//
//        String mailFolderName = ndnTranslator.generateMailFolderName(
//            "mailSync",
//            ExternalProxy.userEmail,
//            "inbox",
//            "1",
//            "/MailFolder/1"
//        );
//        Name mailFolderNdnName = new Name(mailFolderName);
//        mailFolderName = mailFolderNdnName.toUri();
//
//        byte[] mailFolderData =
//            ndnTranslator.encodeMailFolder(contentObj, mailFolderNdnName);
//
//        ndnTranslator.saveData(mailFolderName, mailFolderData, "MailFolder");
//      } catch (CouchbaseLiteException e) {
//        e.printStackTrace();
//      } catch (MessagingException e) {
//        e.printStackTrace();
//      }

    }
  }

  private boolean selectMailbox(String mailboxName, ImapSession session, boolean readOnly) throws FolderException {
    MailFolder folder = getMailbox(mailboxName, session, true);

    if (!folder.isSelectable()) {
      throw new FolderException("Nonselectable mailbox.");
    }

    session.setSelected(folder, readOnly);
    return readOnly;
  }
}

/*
6.3.1.  SELECT Command

   Arguments:  mailbox name

   Responses:  REQUIRED untagged responses: FLAGS, EXISTS, RECENT
               OPTIONAL OK untagged responses: UNSEEN, PERMANENTFLAGS

   Result:     OK - select completed, now in selected state
               NO - select failure, now in authenticated state: no
                    such mailbox, can't access mailbox
               BAD - command unknown or arguments invalid

   The SELECT command selects a mailbox so that messages in the
   mailbox can be accessed.  Before returning an OK to the client,
   the server MUST send the following untagged data to the client:

      FLAGS       Defined flags in the mailbox.  See the description
                  of the FLAGS response for more detail.

      <n> EXISTS  The number of messages in the mailbox.  See the
                  description of the EXISTS response for more detail.

      <n> RECENT  The number of messages with the \Recent flag set.
                  See the description of the RECENT response for more
                  detail.

      OK [UIDVALIDITY <n>]
                  The unique identifier validity value.  See the
                  description of the UID command for more detail.

   to define the initial state of the mailbox at the client.

   The server SHOULD also send an UNSEEN response code in an OK
   untagged response, indicating the message sequence number of the
   first unseen message in the mailbox.

   If the client can not change the permanent state of one or more of
   the flags listed in the FLAGS untagged response, the server SHOULD
   send a PERMANENTFLAGS response code in an OK untagged response,
   listing the flags that the client can change permanently.

   Only one mailbox can be selected at a time in a connection;
   simultaneous access to multiple mailboxes requires multiple
   connections.  The SELECT command automatically deselects any
   currently selected mailbox before attempting the new selection.
   Consequently, if a mailbox is selected and a SELECT command that
   fails is attempted, no mailbox is selected.




Crispin                     Standards Track                    [Page 23]

RFC 2060                       IMAP4rev1                   December 1996


   If the client is permitted to modify the mailbox, the server
   SHOULD prefix the text of the tagged OK response with the
         "[READ-WRITE]" response code.

      If the client is not permitted to modify the mailbox but is
      permitted read access, the mailbox is selected as read-only, and
      the server MUST prefix the text of the tagged OK response to
      SELECT with the "[READ-ONLY]" response code.  Read-only access
      through SELECT differs from the EXAMINE command in that certain
      read-only mailboxes MAY permit the change of permanent state on a
      per-user (as opposed to global) basis.  Netnews messages marked in
      a server-based .newsrc file are an example of such per-user
      permanent state that can be modified with read-only mailboxes.

   Example:    C: A142 SELECT INBOX
               S: * 172 EXISTS
               S: * 1 RECENT
               S: * OK [UNSEEN 12] Message 12 is first unseen
               S: * OK [UIDVALIDITY 3857529045] UIDs valid
               S: * FLAGS (\Answered \Flagged \Deleted \Seen \Draft)
               S: * OK [PERMANENTFLAGS (\Deleted \Seen \*)] Limited
               S: A142 OK [READ-WRITE] SELECT completed
*/
