package com.icegreen.greenmail.ndnproxy;

import java.io.Serializable;
import java.util.*;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

public class Snapshot implements Serializable {
  public String capability;
  public int exists;
  public int recent;
  public String flags;
  public long uidvalidity;
  public long uidnext;
  public int unseen;
  public String complete;
  public Map<Integer, String> fetchResponse;
  public int fetchResponseLastMsn;
  public String fetchResponseLastMsnData;
  public int size = 0;
  public long[] messageUids;
  public long msn;
  public List<String> messageID;
  public HashMap<Long, MimeMessage> map;
  public HashMap<Long, Flags> flagMap;
  public int syncAmount;
  public int initSize;
  public int syncCheckpoint;

  public Snapshot(int a) {
    this.fetchResponse = new HashMap<>();
    this.messageID = new ArrayList<>();
    map = new HashMap<>();
    flagMap = new HashMap<>();
  }
}