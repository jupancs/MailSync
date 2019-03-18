package com.icegreen.greenmail.ndnproxy;

import com.icegreen.greenmail.store.MessageFlags;
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

  public static IMAPFolder folder;
  public static long[] messageUids;
  public static List<String> messgeID = new ArrayList<>();
  public static Snapshot snapshot;
  public static int syncNumber = 0;

  public static Snapshot getSnapshot() throws MessagingException {
    snapshot = new Snapshot(0);
    snapshot.map = new HashMap<>();
    snapshot.flags = MessageFlags.format(folder.getPermanentFlags());
    snapshot.exists = folder.getMessageCount();
    snapshot.recent = getRecentCount(false);
    snapshot.uidvalidity = folder.getUIDValidity();
    snapshot.uidnext = folder.getUIDNext();
    snapshot.unseen = getFirstUnseen();
    snapshot.complete = "READ-ONLY";
    snapshot.size = folder.getMessageCount();
    snapshot.messageUids = getMessageUids();
    snapshot.messageID = messgeID;
    snapshot.syncAmount = syncNumber;
    return snapshot;
  }

  public static int getMsn(long uid) throws MessagingException {
    //messageUids = getMessageUids();
    long[] uids = getMessageUids();
    for (int i = 0; i < uids.length; i++) {
      long messageUid = uids[i];
      if (uid == messageUid) {
        return i + 1;
      }
    }
    return -1;
  }

  public static long[] getMessageUids() throws MessagingException {
    FetchProfile profile = new FetchProfile();
    profile.add(UIDFolder.FetchProfileItem.UID);
    Message[] messages = folder.getMessages();
    folder.fetch(messages, profile);

    messageUids = new long[folder.getMessageCount()];

    int size = messages.length;
    for (int i = 0; i < size; i++) {
//      System.out.println(i + " message number: " + messages[i].getMessageNumber());
      messageUids[i] = folder.getUID(messages[i]);
      snapshot.flagMap.put(messageUids[i], messages[i].getFlags());
    }

    return messageUids;
  }

  private static int getFirstUnseen() throws MessagingException {
    Flags seen = new Flags(Flags.Flag.SEEN);
    FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
    Message[] messages = folder.search(unseenFlagTerm);
    if (messages.length == 0) {
      System.out.println("No messages found");
      return 0;
    }
    return getMsn(folder.getUID(messages[0]));
//    for (int i = 0; i < folder.getMessageCount(); i++) {
//      long start = System.nanoTime();
//      System.out.println(i);
//
//      MimeMessage message = (MimeMessage) messages[i];
//      long end = System.nanoTime();
//
//
//      if (!message.isSet(Flags.Flag.SEEN)) {
//        return i + 1;
//      }
//      System.out.println(">>> Download time cost: " + (double) (end - start) / 1000000000.0);
//      System.out.println("======================================");
//    }
//    return -1;
  }

  private static int getRecentCount(boolean reset) throws MessagingException {
    int count = 0;
    Message[] messages = folder.getMessages();
    for (int i = 0; i < folder.getMessageCount(); i++) {
      MimeMessage message = (MimeMessage) messages[i];
      count++;
      if (reset) {
        message.setFlag(Flags.Flag.RECENT, false);
      }
    }
    return count;
  }

  public static long[] search(SearchTerm searchTerm) throws MessagingException {
    List<Message> matchedMessages = new ArrayList<>();
    Message[] messages = folder.getMessages();

    for (int i = 0; i< folder.getMessageCount(); i++) {
      Message message = messages[i];
      // Update message sequence number for potential sequence set search
      // https://tools.ietf.org/html/rfc3501#page-10
      if (searchTerm.match((MimeMessage) message)) {
        matchedMessages.add(message);
      }
    }

    long[] matchedUids = new long[matchedMessages.size()];
    for (int i = 0; i < matchedUids.length; i++) {
      Message message = matchedMessages.get(i);
      long uid = folder.getUID(message);
      matchedUids[i] = uid;
    }
    return matchedUids;
  }

}
