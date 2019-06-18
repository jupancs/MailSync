package com.icegreen.greenmail.ndnproxy;

import com.google.common.primitives.Longs;
import com.icegreen.greenmail.store.MessageFlags;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
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

import edu.ua.cs.nrl.mailsync.EmailRepository;

public class NdnFolder {

    public static IMAPFolder folder;
    public static long[] messageUids;
    public static List<String> messgeID = new ArrayList<>();
    public static Snapshot snapshot;
    public static int syncNumber = 0;
    public static List<Long> messageUidList = new ArrayList<>();
    public static int sizeDiff = 0;
    public static int lastSize;
    public static HashMap<Long, Flags> flagsMap = new HashMap<>();

    public static int updateSize() {
        int size = 0;
        try {
            size = folder.getMessageCount() - lastSize;
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return size;
    }

    public static Snapshot getSnapshot() throws MessagingException {
        snapshot = new Snapshot(0);
        snapshot.map = new HashMap<>();
        snapshot.flags = MessageFlags.format(folder.getPermanentFlags());
        System.out.println("$$$$$$$$$$$$$$$$ check 1 $$$$$$$$$$$$$$$$");
        //The number of messages in the laptop mailbox is the number of messages in the mobile mailbox - the number of new messages that need to be synced
        snapshot.exists = folder.getMessageCount()- EmailRepository.getIncompleteUids().size()+1;
        System.out.println("$$$$$$$$$$$$$$$$ check 2 $$$$$$$$$$$$$$$$");
        snapshot.recent = getRecentCount(false);
        System.out.println("$$$$$$$$$$$$$$$$ check 3 $$$$$$$$$$$$$$$$");
        snapshot.uidvalidity = folder.getUIDValidity();
        System.out.println("$$$$$$$$$$$$$$$$ check 4 $$$$$$$$$$$$$$$$");
        snapshot.uidnext = EmailRepository.nextUid;
        System.out.println("$$$$$$$$$$$$$$$$ check 5 $$$$$$$$$$$$$$$$");
        snapshot.unseen = getFirstUnseen();
        System.out.println("$$$$$$$$$$$$$$$$ check 6 $$$$$$$$$$$$$$$$");
        snapshot.complete = "READ-ONLY";
        //The size is equal to the above comment
        snapshot.size = folder.getMessageCount()- EmailRepository.getIncompleteUids().size()+1;
        snapshot.flagMap = flagsMap;
        System.out.println("$$$$$$$$$$$$$$$$ check 7 $$$$$$$$$$$$$$$$");
        snapshot.messageUids = Longs.toArray(messageUidList);
        System.out.println("messageUids size: : : : : " + messageUidList.size());
//    snapshot.messageUids = getMessageUids();
        System.out.println("$$$$$$$$$$$$$$$$ check 8 $$$$$$$$$$$$$$$$");
        snapshot.messageID = messgeID;
        snapshot.syncAmount = syncNumber;
        //Initial size is the size of laptop mailbox without the new message being synced
        snapshot.initSize=folder.getMessageCount()-EmailRepository.maxEmailsStored;
        return snapshot;
    }

    public static int getMsn(long uid) throws MessagingException {
        System.out.println("############# check 1 ############# check 1");
        long[] uids = getMessageUids();
        System.out.println("############# check 2 ############# check 2");
        for (int i = 0; i < uids.length; i++) {
            long messageUid = uids[i];
            if (uid == messageUid) {
                return i + 1;
            }
        }
        return -1;
    }

    //Used for printing the messageIds in the messageID list
    public static void printMsgIds() {
        System.out.println("MessageID List contains");
        for (String a : messgeID) {
            System.out.print(a + " ");
        }
    }

    public static long[] getMessageUids() throws MessagingException {
        FetchProfile profile = new FetchProfile();
        profile.add(UIDFolder.FetchProfileItem.UID);
        Message[] messages = folder.getMessages();
        System.out.println("<><><><><><><><> Message size is: " + messages.length);
        folder.fetch(messages, profile);

        messageUids = new long[folder.getMessageCount()];
        System.out.println("############# check 4 ############# check 4");
        int size = messages.length;
//    sizeDiff = folder.getMessageCount() - getLastSize();
//    for (int i = 0; i < size; i++) {
//      messageUids[i] = folder.getUID(messages[i]);
//      snapshot.flagMap.put(messageUids[i], messages[i].getFlags());
//    }
//    for (int i = lastSize; i < lastSize + updateSize(); i++) {
        for (int i = lastSize; i < lastSize + updateSize(); i++) {
            System.out.println("i ======== " + i);
            messageUidList.add(folder.getUID(messages[i]));
//      messageUids[i] = folder.getUID(messages[i]);
//      snapshot.flagMap.put(messageUidList.get(i), messages[i].getFlags());
            flagsMap.put(messageUidList.get(i), messages[i].getFlags());
        }
        lastSize = folder.getMessageCount();
        System.out.println("Last size is: >>>>>>>> " + lastSize);
        System.out.println("############# check 3 ############# check 3");
        return Longs.toArray(messageUidList);
//    return messageUids;
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

        for (int i = 0; i < folder.getMessageCount(); i++) {
            Message message = messages[i];
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
