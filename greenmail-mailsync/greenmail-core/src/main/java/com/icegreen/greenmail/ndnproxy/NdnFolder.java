package com.icegreen.greenmail.ndnproxy;

import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.store.MessageFlags;
import com.icegreen.greenmail.store.SimpleMessageAttributes;
import com.icegreen.greenmail.store.StoredMessage;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;

public class NdnFolder {

  public static long[] messageUids;
  public static Snapshot snapshot;
  public static HashMap<Long, MimeMessage> uidToMime = new HashMap<>();
  public static HashMap<Long, SimpleMessageAttributes> uidToAttr= new HashMap<>();

//  public static Snapshot getSnapshot() throws MessagingException {
//    snapshot = new Snapshot(0);
//    snapshot.flags = MessageFlags.format(folder.getPermanentFlags());
//    snapshot.exists = folder.getMessageCount();
//    snapshot.recent = getRecentCount(false);
//    snapshot.uidvalidity = folder.getUIDValidity();
//    snapshot.uidnext = folder.getUIDNext();
//    snapshot.unseen = getFirstUnseen();
//    snapshot.complete = "READ-ONLY";
//    snapshot.size = folder.getMessageCount();
//    snapshot.messageUids = messageUids;
//
//    return snapshot;
//  }

  public static int getMsn(long uid) throws MessagingException {
    //messageUids = getMessageUids();
    int end = NDNMailSyncConsumerProducer.mailbox.size - ExternalProxy.mailboxSize;
//    long[] uids = Arrays.copyOfRange(NDNMailSyncConsumerProducer.mailbox.messageUids, 0, end);
    long[] uids = NDNMailSyncConsumerProducer.mailbox.messageUids;
    for (int i = 0; i < uids.length; i++) {
      long messageUid = uids[i];
      if (uid == messageUid) {
        return i + 1;
      }
    }
    return -1;
  }

//  public static long[] getMessageUids() throws MessagingException {
//    FetchProfile profile = new FetchProfile();
//    profile.add(UIDFolder.FetchProfileItem.UID);
//    Message[] messages = folder.getMessages();
//    folder.fetch(messages, profile);
//
//    messageUids = new long[folder.getMessageCount()];
//
//    int size = folder.getMessageCount();
//    for (int i = size - 1; i >= 0; i--) {
//      messageUids[size - 1 - i] = folder.getUID(messages[i]);
//    }
//
//    return messageUids;
//  }

//  private static int getFirstUnseen() throws MessagingException {
//    Flags seen = new Flags(Flags.Flag.SEEN);
//    FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
//    Message[] messages = folder.search(unseenFlagTerm);
//    if (messages.length == 0) {
//      System.out.println("No messages found");
//      return 0;
//    }
//    return getMsn(folder.getUID(messages[0]));
//  }

//  private static int getRecentCount(boolean reset) throws MessagingException {
//    int count = 0;
//    Message[] messages = folder.getMessages();
//    for (int i = 0; i < folder.getMessageCount(); i++) {
//      MimeMessage message = (MimeMessage) messages[i];
//      count++;
//      if (reset) {
//        message.setFlag(Flags.Flag.RECENT, false);
//      }
//    }
//    return count;
//  }

//  public static long[] search(SearchTerm searchTerm) throws MessagingException {
//    List<Message> matchedMessages = new ArrayList<>();
//    Message[] messages = folder.getMessages();
//
//    for (int i = 0; i< folder.getMessageCount(); i++) {
//      Message message = messages[i];
//      // Update message sequence number for potential sequence set search
//      // https://tools.ietf.org/html/rfc3501#page-10
//      if (searchTerm.match((MimeMessage) message)) {
//        matchedMessages.add(message);
//      }
//    }
//
//    long[] matchedUids = new long[matchedMessages.size()];
//    for (int i = 0; i < matchedUids.length; i++) {
//      Message message = matchedMessages.get(i);
//      long uid = folder.getUID(message);
//      matchedUids[i] = uid;
//    }
//    return matchedUids;
//  }

}

