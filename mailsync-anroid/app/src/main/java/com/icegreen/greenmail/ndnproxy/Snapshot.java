package com.icegreen.greenmail.ndnproxy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

public class Snapshot implements Serializable {
    public String capability;
    public int exists;
    public int recent;
    public String flags;
    public long uidvalidity;
    /**
     * UID of the next newest email
     */
    public long uidnext;
    public int unseen;
    public String complete;
    public Map<Integer, String> fetchResponse;
    public int fetchResponseLastMsn;
    public String fetchResponseLastMsnData;
    public int size = 0;
    /**
     * Maintains a list of UIDs of all the emails in the mailbox
     */
    public long[] messageUids;
    public long msn;
    /**
     * Maintains a list of IDs for each email that was stored on
     * the mobile side
     */
    public List<String> messageID;
    public HashMap<Long, MimeMessage> map;
    public HashMap<Long, Flags> flagMap;
    /**
     * Keeps track of the number of emails that can be synced
     */
    public int syncAmount;
    /**
     * The initial size of the mailbox before all the syncing of emails
     * Used to find the correct messageUID for each email on the laptop side
     */
    public int initSize;
    /**
     * Tracks the position of where the last sync ended
     * Used to find the correct messageID for each email on the laptop side
     */
    public int syncCheckpoint;

    public Snapshot(int a) {
        this.fetchResponse = new HashMap<>();
        this.messageID = new ArrayList<>();
        map = new HashMap<>();
        flagMap = new HashMap<>();
    }
}