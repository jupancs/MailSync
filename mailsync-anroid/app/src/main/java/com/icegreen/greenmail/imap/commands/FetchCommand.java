/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.icegreen.greenmail.ExternalProxy;
import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.ndnproxy.NdnFolder;
import com.icegreen.greenmail.ndntranslator.TranslateWorker;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MessageFlags;
import com.icegreen.greenmail.store.SimpleMessageAttributes;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.util.GreenMailUtil;

import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.icegreen.greenmail.ndnproxy.Snapshot;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.protocol.INTERNALDATE;

import edu.ua.cs.nrl.mailsync.EmailRepository;


/**
 * Handles processing for the FETCH imap command.
 * <p/>
 * https://tools.ietf.org/html/rfc3501#section-6.4.5
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class FetchCommand extends SelectedStateCommand implements UidEnabledCommand {
  public static final String NAME = "FETCH";
  public static final String ARGS = "<message-set> <fetch-profile>";
  private static final Flags FLAGS_SEEN = new Flags(Flags.Flag.SEEN);
  private static final Pattern NUMBER_MATCHER = Pattern.compile("^\\d+$");
  private static EmailRepository emailRepository;
  private FetchCommandParser parser = new FetchCommandParser();
  private int cnt = 0;

  FetchCommand() {
    super(NAME, ARGS);
  }

  @Override
  protected void doProcess(ImapRequestLineReader request,
                           ImapResponse response,
                           ImapSession session)
      throws ProtocolException, FolderException, MessagingException {
    doProcess(request, response, session, false);
  }

  @Override
  public void doProcess(ImapRequestLineReader request,
                        ImapResponse response,
                        ImapSession session,
                        boolean useUids)
      throws ProtocolException, FolderException, MessagingException {
    IdRange[] idSet = parser.parseIdRange(request);
    FetchRequest fetch = parser.fetchRequest(request);
    parser.endLine(request);

    if (useUids) {
      fetch.uid = true;
    }

    System.out.println("I am in Fetch Command!");
    if (fetch.internalDate) {

      try {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imap.host", "imap.gmail.com");
        Session sessionIMAP = Session.getInstance(props, null);
        Store store = sessionIMAP.getStore("imaps");
        store.connect("imap.gmail.com", ExternalProxy.userEmail, ExternalProxy.userPassword);
        Folder emailFolder = store.getFolder("INBOX");
        emailFolder.open(Folder.READ_WRITE);
        IMAPFolder imapFolder = (IMAPFolder) emailFolder;
        emailRepository= new EmailRepository();
        NdnFolder.folder = imapFolder;

        for (IdRange idRange : idSet) {
          long uid = idRange.getLowVal();
          System.out.println(">>>> Fetching UID: " + uid);
          NdnFolder.syncNumber++;
          saveToNdnStorage(imapFolder, uid);
        }

        NdnFolder.messgeID.clear();
        NdnFolder.syncNumber = 0;

        store.close();

      } catch (NoSuchProviderException e) {
        e.printStackTrace();
      } catch (MessagingException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }


//    //local operations -- greenmail send it back
//    ImapSessionFolder mailbox = session.getSelected();
//    long[] uids = mailbox.getMessageUids();
//    for (long uid : uids) {
//      int msn = mailbox.getMsn(uid);
//
//      if ((useUids && includes(idSet, uid)) ||
//          (!useUids && includes(idSet, msn))) {
//        String msgData = getMessageData(useUids, fetch, mailbox, uid);
//        response.fetchResponse(msn, msgData);
//      }
//    }
//
//    // a wildcard search must include the last message if the folder is not empty,
//    // as per https://tools.ietf.org/html/rfc3501#section-6.4.8
//    long lastMessageUid = uids.length > 0 ? uids[uids.length - 1] : -1L;
//    if (mailbox.getMessageCount() > 0 && includes(idSet, Long.MAX_VALUE) &&
//        !includes(idSet, lastMessageUid)) {
//      String msgData = getMessageData(useUids, fetch, mailbox, lastMessageUid);
//      response.fetchResponse(mailbox.getMsn(lastMessageUid), msgData);
//    }
//
//    boolean omitExpunged = !useUids;
//    session.unsolicitedResponses(response, omitExpunged);
//    response.commandComplete(this);

//    long[] uids = NdnFolder.messageUids;
//    int size = NdnFolder.messageUids.length;
////    long[] uids = new long[10];
////    if (cnt + 10 <= size) {
////      uids = Arrays.copyOfRange(NdnFolder.messageUids, cnt, cnt + 10);
////      cnt += 10;
////    } else if (cnt < size && cnt + 10 > size) {
////      uids = Arrays.copyOfRange(NdnFolder.messageUids, cnt, size);
////      cnt += size - cnt;
////    }
//
//    for (long uid : uids) {
//      int msn = NdnFolder.getMsn(uid);
//      System.out.println("MSN: " + msn);
//      if ((useUids && includes(idSet, uid)) ||
//          (!useUids && includes(idSet, msn))) {
//        String msgData = getMessageDataNdn(useUids, fetch, NdnFolder.folder, uid);
//        response.fetchResponse(msn, msgData);
//      }
//    }
//
//    // a wildcard search must include the last message if the folder is not empty,
//    // as per https://tools.ietf.org/html/rfc3501#section-6.4.8
//    long lastMessageUid = uids.length > 0 ? uids[uids.length - 1] : -1L;
//    if (NdnFolder.getSnapshot().size > 0 && includes(idSet, Long.MAX_VALUE) &&
//        !includes(idSet, lastMessageUid)) {
//      String msgData = getMessageDataNdn(useUids, fetch, NdnFolder.folder, lastMessageUid);
//      response.fetchResponse(NdnFolder.getMsn(lastMessageUid), msgData);
//    }
//
//    boolean omitExpunged = !useUids;
//    session.unsolicitedResponses(response, omitExpunged);
//    response.commandComplete(this);

  }
  //Saves to NDN storage and increments storedMessages in emailRepository for later use
  private void saveToNdnStorage(IMAPFolder folder, long uid) {
    try {
      Message message = folder.getMessage(getMsn(uid, folder));
      MimeMessage mimeMessage = (MimeMessage) message;
      mimeMessage = new MimeMessage(mimeMessage);
      NdnFolder.messgeID.add(mimeMessage.getMessageID());
      TranslateWorker.start(mimeMessage, ExternalProxy.context);
      System.out.println("Saved Email with UID: " + uid);
      emailRepository.incrementStoredMessages();
    } catch (MessagingException e) {
      e.printStackTrace();
    } catch (FolderException e) {
      e.printStackTrace();
    } catch (CouchbaseLiteException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private int getMsn(long uid, IMAPFolder folder) throws MessagingException {
    long[] uids = getMessageUids(folder);
    for (int i = 0; i < uids.length; i++) {
      long messageUid = uids[i];
      if (uid == messageUid) {
        return i + 1;
      }
    }
    return -1;
  }

  private long[] getMessageUids(IMAPFolder folder) throws MessagingException {
    FetchProfile profile = new FetchProfile();
    profile.add(UIDFolder.FetchProfileItem.UID);
    Message[] messages = folder.getMessages();
    folder.fetch(messages, profile);

    long[] messageUids = new long[folder.getMessageCount()];

    int size = folder.getMessageCount();
    for (int i = 0; i < size; i++) {
      messageUids[i] = folder.getUID(messages[i]);
    }

    return messageUids;
  }

  public static void doNDNProcess(String idSetString,
                                   String fetchString,
                                   MailFolder mailbox,
                                   Snapshot snapshot)
      throws Exception {
    //NDN process
    //convert json to obj
    Gson gson = new Gson();
    System.out.println(idSetString);
    System.out.println(fetchString);
    IdRange[] idSet = gson.fromJson(idSetString, IdRange[].class);
    FetchRequest fetch = gson.fromJson(fetchString, FetchRequest.class);

    boolean useUids = false;

    long[] uids = mailbox.getMessageUids();
    for (long uid : uids) {
      try {
        int msn = mailbox.getMsn(uid);
        boolean isIncluded = false;
        if (useUids) {
          for (IdRange x : idSet) {
            if (x.includes(uid)) {
              isIncluded = true;
              break;
            }
          }
        } else if (!useUids) {
          for (IdRange x : idSet) {
            if (x.includes(msn)) {
              isIncluded = true;
              break;
            }
          }
        }

        if (isIncluded) {
          String msgData = outputMessageNDN(useUids, fetch, mailbox, uid);
          snapshot.fetchResponse.put(msn, msgData);
          System.out.println(String.valueOf(msn) + "\n " + msgData);
        }
      } catch (Exception e) {
        System.out.println(e);
      }

    }

    // a wildcard search must include the last message if the folder is not empty,
    // as per https://tools.ietf.org/html/rfc3501#section-6.4.8
    long lastMessageUid = uids.length > 0 ? uids[uids.length - 1] : -1L;
    boolean isIncluded = false;
    if (mailbox.getMessageCount() > 0) {
      for (IdRange x : idSet) {
        if (x.includes(Long.MAX_VALUE)) {
          if (!x.includes(lastMessageUid)) {
            isIncluded = true;
            break;
          }
        }
      }
    }

    if (isIncluded) {
      String msgData = outputMessageNDN(useUids, fetch, mailbox, lastMessageUid);
      snapshot.fetchResponseLastMsn = mailbox.getMsn(lastMessageUid);
      snapshot.fetchResponseLastMsnData = msgData;
    }
  }

  public static String outputMessageNDN(boolean useUids, FetchRequest fetch, MailFolder folder, long uid)
      throws FolderException, ProtocolException {
    StoredMessage message = folder.getMessage(uid);

    boolean ensureFlagsResponse = false;

    StringBuilder response = new StringBuilder();

    // FLAGS response
    if (fetch.flags || ensureFlagsResponse) {
      response.append(" FLAGS ");
      response.append(MessageFlags.format(message.getFlags()));
    }

    // INTERNALDATE response
    if (fetch.internalDate) {
      response.append(" INTERNALDATE \"");
      // TODO format properly
      response.append(message.getAttributes().getReceivedDateAsString());
      response.append('\"');
    }

    // RFC822.SIZE response
    if (fetch.size) {
      response.append(" RFC822.SIZE ");
      response.append(message.getAttributes().getSize());
    }

    // ENVELOPE response
    if (fetch.envelope) {
      response.append(" ENVELOPE ");
      response.append(message.getAttributes().getEnvelope());
    }

    // BODY response
    if (fetch.body) {
      response.append(" BODY ");
      response.append(message.getAttributes().getBodyStructure(false));
    }

    // BODYSTRUCTURE response
    if (fetch.bodyStructure) {
      response.append(" BODYSTRUCTURE ");
      response.append(message.getAttributes().getBodyStructure(true));
    }

    // UID response
    if (fetch.uid) {
      response.append(" UID ");
      response.append(message.getUid());
    }

    // BODY part responses.
    Collection<BodyFetchElement> elements = fetch.getBodyElements();
    for (BodyFetchElement fetchElement : elements) {
      response.append(SP);
      response.append(fetchElement.getResponseName());
      final Partial partial = fetchElement.getPartial();
      if (null == partial) {
        response.append(SP);
      }

      // Various mechanisms for returning message body.
      String sectionSpecifier = fetchElement.getParameters();

      MimeMessage mimeMessage = message.getMimeMessage();
      try {
        handleBodyFetchNDN(mimeMessage, sectionSpecifier, partial, response);
      } catch (Exception e) {
        throw new FolderException(e);
      }
    }

    if (response.length() > 0) {
      // Remove the leading " ".
      return response.substring(1);
    } else {
      return "";
    }
  }

  public static void handleBodyFetchNDN(MimeMessage mimeMessage,
                                        String sectionSpecifier,
                                        Partial partial,
                                        StringBuilder response)
      throws IOException, MessagingException {

    System.out.println("Fetching body part for section specifier " + sectionSpecifier +
        " and mime message (contentType=" + mimeMessage.getContentType());

    if (sectionSpecifier.length() == 0) {
      // TODO - need to use an InputStream from the response here.
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      mimeMessage.writeTo(bout);
      byte[] bytes = bout.toByteArray();
      bytes = doPartialNDN(partial, bytes, response);
      addLiteralNDN(bytes, response);
    } else if ("HEADER".equalsIgnoreCase(sectionSpecifier)) {
      Enumeration<?> inum = mimeMessage.getAllHeaderLines();
      addHeadersNDN(inum, response, partial);
    } else if (sectionSpecifier.startsWith("HEADER.FIELDS.NOT")) {
      String[] excludeNames = extractHeaderListNDN(sectionSpecifier, "HEADER.FIELDS.NOT".length());
      Enumeration<?> inum = mimeMessage.getNonMatchingHeaderLines(excludeNames);
      addHeadersNDN(inum, response, partial);
    } else if (sectionSpecifier.startsWith("HEADER.FIELDS ")) {
      String[] includeNames = extractHeaderListNDN(sectionSpecifier, "HEADER.FIELDS ".length());
      Enumeration<?> inum = mimeMessage.getMatchingHeaderLines(includeNames);
      addHeadersNDN(inum, response, partial);
    } else if (sectionSpecifier.endsWith("MIME")) {
      String[] strs = sectionSpecifier.trim().split("\\.");
      int partNumber = Integer.parseInt(strs[0]) - 1;
      MimeMultipart mp = (MimeMultipart) mimeMessage.getContent();
      byte[] bytes = GreenMailUtil.getHeaderAsBytes(mp.getBodyPart(partNumber));
      bytes = doPartialNDN(partial, bytes, response);
      addLiteralNDN(bytes, response);
    } else if ("TEXT".equalsIgnoreCase(sectionSpecifier)) {
      handleBodyFetchForTextNDN(mimeMessage, partial, response);
    } else {
      Object content = mimeMessage.getContent();
      if (content instanceof String) {
        handleBodyFetchForTextNDN(mimeMessage, partial, response);
      } else {
        MimeMultipart mp = (MimeMultipart) content;
        BodyPart part = null;

        // Find part by number spec, eg "1" or "2.1" or "4.3.1" ...
        String spec = sectionSpecifier;

        int dotIdx = spec.indexOf('.');
        String pre = dotIdx < 0 ? spec : spec.substring(0, dotIdx);
        while (null != pre && NUMBER_MATCHER.matcher(pre).matches()) {
          int partNumber = Integer.parseInt(pre) - 1;
          if (null == part) {
            part = mp.getBodyPart(partNumber);
          } else {
            // Content must be multipart
            part = ((Multipart) part.getContent()).getBodyPart(partNumber);
          }

          dotIdx = spec.indexOf('.');
          if (dotIdx > 0) { // Another sub part index?
            spec = spec.substring(dotIdx + 1);
            pre = spec.substring(0, dotIdx);
          } else {
            pre = null;
          }
        }

        if (null == part) {
          throw new IllegalStateException("Got null for " + sectionSpecifier);
        }

        // A bit optimistic to only cover theses cases ... TODO
        if ("message/rfc822".equalsIgnoreCase(part.getContentType())) {
          handleBodyFetchNDN((MimeMessage) part.getContent(), spec, partial, response);
        } else if ("TEXT".equalsIgnoreCase(spec)) {
          handleBodyFetchForTextNDN(mimeMessage, partial, response);
        } else {
          byte[] bytes = GreenMailUtil.getBodyAsBytes(part);
          bytes = doPartialNDN(partial, bytes, response);
          addLiteralNDN(bytes, response);
        }
      }
    }
  }

  public static void handleBodyFetchForTextNDN(MimeMessage mimeMessage, Partial partial, StringBuilder response) {
    // TODO - need to use an InputStream from the response here.
    // TODO - this is a hack. To get just the body content, I'm using a null
    // input stream to take the headers. Need to have a way of ignoring headers.

    byte[] bytes = GreenMailUtil.getBodyAsBytes(mimeMessage);
    bytes = doPartialNDN(partial, bytes, response);
    addLiteralNDN(bytes, response);
  }

  public static byte[] doPartialNDN(Partial partial, byte[] bytes, StringBuilder response) {
    if (null != partial) {
      int len = partial.computeLength(bytes.length);
      int start = partial.computeStart(bytes.length);
      byte[] newBytes = new byte[len];
      System.arraycopy(bytes, start, newBytes, 0, len);
      bytes = newBytes;
      response.append('<').append(partial.start).append('>');
    }
    return bytes;
  }

  public static void addLiteralNDN(byte[] bytes, StringBuilder response) {
    response.append('{');
    response.append(bytes.length);
    response.append('}');
    response.append("\r\n");

    for (byte b : bytes) {
      response.append((char) b);
    }
  }

  public static String[] extractHeaderListNDN(String headerList, int prefixLen) {
    // Remove the trailing and leading ')('
    String tmp = headerList.substring(prefixLen + 1, headerList.length() - 1);
    return splitNDN(tmp, " ");
  }

  public static String[] splitNDN(String value, String delimiter) {
    List<String> strings = new ArrayList<>();
    int startPos = 0;
    int delimPos;
    while ((delimPos = value.indexOf(delimiter, startPos)) != -1) {
      String sub = value.substring(startPos, delimPos);
      strings.add(sub);
      startPos = delimPos + 1;
    }
    String sub = value.substring(startPos);
    strings.add(sub);

    return strings.toArray(new String[strings.size()]);
  }

  public static void addHeadersNDN(Enumeration<?> inum, StringBuilder response, Partial partial) {
    StringBuilder buf = new StringBuilder();

    int count = 0;
    while (inum.hasMoreElements()) {
      String line = (String) inum.nextElement();
      count += line.length() + 2;
      buf.append(line).append("\r\n");
    }

    if (null != partial) {
      final String partialContent = buf.toString();
      int len = partial.computeLength(partialContent.length()); // TODO : Charset?
      int start = partial.computeStart(partialContent.length());

      response.append('<').append(partial.start).append('>');
      response.append(" {");
      response.append(len);
      response.append('}');
      response.append("\r\n");

      response.append(partialContent.substring(start, start + len));
    } else {
      response.append("{");
      response.append(count);
      response.append('}');
      response.append("\r\n");

      response.append(buf);
    }
  }

  private String getMessageData(boolean useUids, FetchRequest fetch, ImapSessionFolder mailbox, long uid) throws FolderException, ProtocolException {
    StoredMessage storedMessage = mailbox.getMessage(uid);
    return outputMessage(fetch, storedMessage, mailbox, useUids);
  }

  private String getMessageDataNdn(boolean useUids, FetchRequest fetch,
                                   IMAPFolder mailbox, long uid)
      throws FolderException, ProtocolException, MessagingException {
    MimeMessage mimeMessage = (MimeMessage) mailbox.getMessageByUID(uid);
    IMAPMessage imapMessage = (IMAPMessage) mailbox.getMessageByUID(uid);

//     mailbox.getMessageByUID(uid)
    System.out.println("Entering attribute *******");
    SimpleMessageAttributes attributes =
        new SimpleMessageAttributes(mimeMessage, mimeMessage.getReceivedDate());
    System.out.println("Exiting attribute >>>>>>>");
    return outputMessageNdn(fetch, mimeMessage, mailbox, useUids, uid, attributes);
  }

  private String outputMessageNdn(FetchRequest fetch, MimeMessage message, IMAPFolder folder,
                                  boolean useUids, long uid, SimpleMessageAttributes attributes)
      throws FolderException, ProtocolException, MessagingException {
    // Check if this fetch will cause the "SEEN" flag to be set on this message
    // If so, update the flags, and ensure that a flags response is included in the response.
    boolean ensureFlagsResponse = false;
    if (fetch.isSetSeen() && !message.isSet(Flags.Flag.SEEN)) {
      //folder.setFlags(FLAGS_SEEN, true, uid, folder, useUids);
      message.setFlags(FLAGS_SEEN, true);
      ensureFlagsResponse = true;
    }

    StringBuilder response = new StringBuilder();

    // FLAGS response
    if (fetch.flags || ensureFlagsResponse) {
      response.append(" FLAGS ");
      response.append(MessageFlags.format(message.getFlags()));
    }

    // INTERNALDATE response
    if (fetch.internalDate) {
      response.append(" INTERNALDATE \"");
      response.append(INTERNALDATE.format(message.getReceivedDate()));
      response.append('\"');
    }

    // RFC822.SIZE response
    if (fetch.size) {
      response.append(" RFC822.SIZE ");
      response.append(message.getSize());
    }

    // ENVELOPE response
    if (fetch.envelope) {
      response.append(" ENVELOPE ");
      response.append(attributes.getEnvelope());
    }

    // BODY response
    if (fetch.body) {
      response.append(" BODY ");
      response.append(attributes.getBodyStructure(false));
    }

    // BODYSTRUCTURE response
    if (fetch.bodyStructure) {
      response.append(" BODYSTRUCTURE ");
      response.append(attributes.getBodyStructure(true));
    }

    // UID response
    if (fetch.uid) {
      response.append(" UID ");
      response.append(uid);
    }

    // BODY part responses.
    Collection<BodyFetchElement> elements = fetch.getBodyElements();
    for (BodyFetchElement fetchElement : elements) {
      response.append(SP);
      response.append(fetchElement.getResponseName());
      final Partial partial = fetchElement.getPartial();
      if (null == partial) {
        response.append(SP);
      }

      // Various mechanisms for returning message body.
      String sectionSpecifier = fetchElement.getParameters();

      MimeMessage mimeMessage = message;
      try {
        handleBodyFetch(mimeMessage, sectionSpecifier, partial, response);
      } catch (Exception e) {
        throw new FolderException(e);
      }
    }

    if (response.length() > 0) {
      // Remove the leading " ".
      return response.substring(1);
    } else {
      return "";
    }
  }

  private String outputMessage(FetchRequest fetch, StoredMessage message,
                               ImapSessionFolder folder, boolean useUids)
      throws FolderException, ProtocolException {
    // Check if this fetch will cause the "SEEN" flag to be set on this message
    // If so, update the flags, and ensure that a flags response is included in the response.
    boolean ensureFlagsResponse = false;
    if (fetch.isSetSeen() && !message.isSet(Flags.Flag.SEEN)) {
      folder.setFlags(FLAGS_SEEN, true, message.getUid(), folder, useUids);
      message.setFlags(FLAGS_SEEN, true);
      ensureFlagsResponse = true;
    }

    StringBuilder response = new StringBuilder();

    // FLAGS response
    if (fetch.flags || ensureFlagsResponse) {
      response.append(" FLAGS ");
      response.append(MessageFlags.format(message.getFlags()));
    }

    // INTERNALDATE response
    if (fetch.internalDate) {
      response.append(" INTERNALDATE \"");
      response.append(message.getAttributes().getReceivedDateAsString());
      response.append('\"');
    }

    // RFC822.SIZE response
    if (fetch.size) {
      response.append(" RFC822.SIZE ");
      response.append(message.getAttributes().getSize());
    }

    // ENVELOPE response
    if (fetch.envelope) {
      response.append(" ENVELOPE ");
      response.append(message.getAttributes().getEnvelope());
    }

    // BODY response
    if (fetch.body) {
      response.append(" BODY ");
      response.append(message.getAttributes().getBodyStructure(false));
    }

    // BODYSTRUCTURE response
    if (fetch.bodyStructure) {
      response.append(" BODYSTRUCTURE ");
      response.append(message.getAttributes().getBodyStructure(true));
    }

    // UID response
    if (fetch.uid) {
      response.append(" UID ");
      response.append(message.getUid());
    }

    // BODY part responses.
    Collection<BodyFetchElement> elements = fetch.getBodyElements();
    for (BodyFetchElement fetchElement : elements) {
      response.append(SP);
      response.append(fetchElement.getResponseName());
      final Partial partial = fetchElement.getPartial();
      if (null == partial) {
        response.append(SP);
      }

      // Various mechanisms for returning message body.
      String sectionSpecifier = fetchElement.getParameters();

      MimeMessage mimeMessage = message.getMimeMessage();
      try {
        handleBodyFetch(mimeMessage, sectionSpecifier, partial, response);
      } catch (Exception e) {
        throw new FolderException(e);
      }
    }

    if (response.length() > 0) {
      // Remove the leading " ".
      return response.substring(1);
    } else {
      return "";
    }
  }


  private void handleBodyFetch(MimeMessage mimeMessage,
                               String sectionSpecifier,
                               Partial partial,
                               StringBuilder response) throws IOException, MessagingException {
    if (log.isDebugEnabled()) {
      log.debug("Fetching body part for section specifier " + sectionSpecifier +
          " and mime message (contentType=" + mimeMessage.getContentType());
    }

    if (sectionSpecifier.length() == 0) {
      // TODO - need to use an InputStream from the response here.
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      mimeMessage.writeTo(bout);
      byte[] bytes = bout.toByteArray();
      bytes = doPartial(partial, bytes, response);
      addLiteral(bytes, response);
    } else if ("HEADER".equalsIgnoreCase(sectionSpecifier)) {
      Enumeration<?> inum = mimeMessage.getAllHeaderLines();
      addHeadersNDN(inum, response, partial);
    } else if (sectionSpecifier.startsWith("HEADER.FIELDS.NOT")) {
      String[] excludeNames = extractHeaderListNDN(sectionSpecifier, "HEADER.FIELDS.NOT".length());
      Enumeration<?> inum = mimeMessage.getNonMatchingHeaderLines(excludeNames);
      addHeadersNDN(inum, response, partial);
    } else if (sectionSpecifier.startsWith("HEADER.FIELDS ")) {
      String[] includeNames = extractHeaderListNDN(sectionSpecifier, "HEADER.FIELDS ".length());
      Enumeration<?> inum = mimeMessage.getMatchingHeaderLines(includeNames);
      addHeadersNDN(inum, response, partial);
    } else if (sectionSpecifier.endsWith("MIME")) {
      String[] strs = sectionSpecifier.trim().split("\\.");
      int partNumber = Integer.parseInt(strs[0]) - 1;
      MimeMultipart mp = (MimeMultipart) mimeMessage.getContent();
      byte[] bytes = GreenMailUtil.getHeaderAsBytes(mp.getBodyPart(partNumber));
      bytes = doPartial(partial, bytes, response);
      addLiteral(bytes, response);
    } else if ("TEXT".equalsIgnoreCase(sectionSpecifier)) {
      handleBodyFetchForText(mimeMessage, partial, response);
    } else {
      Object content = mimeMessage.getContent();
      if (content instanceof String) {
        handleBodyFetchForText(mimeMessage, partial, response);
      } else {
        MimeMultipart mp = (MimeMultipart) content;
        BodyPart part = null;

        // Find part by number spec, eg "1" or "2.1" or "4.3.1" ...
        String spec = sectionSpecifier;

        int dotIdx = spec.indexOf('.');
        String pre = dotIdx < 0 ? spec : spec.substring(0, dotIdx);
        while (null != pre && NUMBER_MATCHER.matcher(pre).matches()) {
          int partNumber = Integer.parseInt(pre) - 1;
          if (null == part) {
            part = mp.getBodyPart(partNumber);
          } else {
            // Content must be multipart
            part = ((Multipart) part.getContent()).getBodyPart(partNumber);
          }

          dotIdx = spec.indexOf('.');
          if (dotIdx > 0) { // Another sub part index?
            spec = spec.substring(dotIdx + 1);
            pre = spec.substring(0, dotIdx);
          } else {
            pre = null;
          }
        }

        if (null == part) {
          throw new IllegalStateException("Got null for " + sectionSpecifier);
        }

        // A bit optimistic to only cover theses cases ... TODO
        if ("message/rfc822".equalsIgnoreCase(part.getContentType())) {
          handleBodyFetch((MimeMessage) part.getContent(), spec, partial, response);
        } else if ("TEXT".equalsIgnoreCase(spec)) {
          handleBodyFetchForText(mimeMessage, partial, response);
        } else {
          byte[] bytes = GreenMailUtil.getBodyAsBytes(part);
          bytes = doPartial(partial, bytes, response);
          addLiteral(bytes, response);
        }
      }
    }
  }

  private void handleBodyFetchForText(MimeMessage mimeMessage, Partial partial, StringBuilder response) {
    // TODO - need to use an InputStream from the response here.
    // TODO - this is a hack. To get just the body content, I'm using a null
    // input stream to take the headers. Need to have a way of ignoring headers.

    byte[] bytes = GreenMailUtil.getBodyAsBytes(mimeMessage);
    bytes = doPartial(partial, bytes, response);
    addLiteral(bytes, response);
  }

  private byte[] doPartial(Partial partial, byte[] bytes, StringBuilder response) {
    if (null != partial) {
      int len = partial.computeLength(bytes.length);
      int start = partial.computeStart(bytes.length);
      byte[] newBytes = new byte[len];
      System.arraycopy(bytes, start, newBytes, 0, len);
      bytes = newBytes;
      response.append('<').append(partial.start).append('>');
    }
    return bytes;
  }

  private void addLiteral(byte[] bytes, StringBuilder response) {
    response.append('{');
    response.append(bytes.length);
    response.append('}');
    response.append("\r\n");

    for (byte b : bytes) {
      response.append((char) b);
    }
  }

  // TODO should do this at parse time.
  private String[] extractHeaderList(String headerList, int prefixLen) {
    // Remove the trailing and leading ')('
    String tmp = headerList.substring(prefixLen + 1, headerList.length() - 1);
    return split(tmp, " ");
  }

  private String[] split(String value, String delimiter) {
    List<String> strings = new ArrayList<>();
    int startPos = 0;
    int delimPos;
    while ((delimPos = value.indexOf(delimiter, startPos)) != -1) {
      String sub = value.substring(startPos, delimPos);
      strings.add(sub);
      startPos = delimPos + 1;
    }
    String sub = value.substring(startPos);
    strings.add(sub);

    return strings.toArray(new String[strings.size()]);
  }

  private void addHeaders(Enumeration<?> inum, StringBuilder response, Partial partial) {
    StringBuilder buf = new StringBuilder();

    int count = 0;
    while (inum.hasMoreElements()) {
      String line = (String) inum.nextElement();
      count += line.length() + 2;
      buf.append(line).append("\r\n");
    }

    if (null != partial) {
      final String partialContent = buf.toString();
      int len = partial.computeLength(partialContent.length()); // TODO : Charset?
      int start = partial.computeStart(partialContent.length());

      response.append('<').append(partial.start).append('>');
      response.append(" {");
      response.append(len);
      response.append('}');
      response.append("\r\n");

      response.append(partialContent.substring(start, start + len));
    } else {
      response.append("{");
      response.append(count);
      response.append('}');
      response.append("\r\n");

      response.append(buf);
    }
  }

  private static class FetchCommandParser extends CommandParser {

    FetchRequest fetchRequest(ImapRequestLineReader request)
        throws ProtocolException {
      FetchRequest fetch = new FetchRequest();

      // Parenthesis optional if single 'atom'
      char next = nextNonSpaceChar(request);
      boolean parenthesis = '(' == next;
      if (parenthesis) {
        consumeChar(request, '(');

        next = nextNonSpaceChar(request);
        while (next != ')') {
          addNextElement(request, fetch);
          next = nextNonSpaceChar(request);
        }

        consumeChar(request, ')');
      } else {
        // Single item
        addNextElement(request, fetch);
      }

      return fetch;
    }

    private void addNextElement(ImapRequestLineReader command, FetchRequest fetch)
        throws ProtocolException {
      char next = nextCharInLine(command);
      StringBuilder element = new StringBuilder();
      while (next != ' ' && next != '[' && next != ')' && !isCrOrLf(next)) {
        element.append(next);
        command.consume();
        next = command.nextChar();
      }
      String name = element.toString();
      // Simple elements with no '[]' parameters.
      if (next == ' ' || next == ')' || isCrOrLf(next)) {
        if ("FAST".equalsIgnoreCase(name)) {
          fetch.flags = true;
          fetch.internalDate = true;
          fetch.size = true;
        } else if ("FULL".equalsIgnoreCase(name)) {
          fetch.flags = true;
          fetch.internalDate = true;
          fetch.size = true;
          fetch.envelope = true;
          fetch.body = true;
        } else if ("ALL".equalsIgnoreCase(name)) {
          fetch.flags = true;
          fetch.internalDate = true;
          fetch.size = true;
          fetch.envelope = true;
        } else if ("FLAGS".equalsIgnoreCase(name)) {
          fetch.flags = true;
        } else if ("RFC822.SIZE".equalsIgnoreCase(name)) {
          fetch.size = true;
        } else if ("ENVELOPE".equalsIgnoreCase(name)) {
          fetch.envelope = true;
        } else if ("INTERNALDATE".equalsIgnoreCase(name)) {
          fetch.internalDate = true;
        } else if ("BODY".equalsIgnoreCase(name)) {
          fetch.body = true;
        } else if ("BODYSTRUCTURE".equalsIgnoreCase(name)) {
          fetch.bodyStructure = true;
        } else if ("UID".equalsIgnoreCase(name)) {
          fetch.uid = true;
        } else if ("RFC822".equalsIgnoreCase(name)) {
          fetch.add(new BodyFetchElement("RFC822", ""), false);
        } else if ("RFC822.HEADER".equalsIgnoreCase(name)) {
          fetch.add(new BodyFetchElement("RFC822.HEADER", "HEADER"), true);
        } else if ("RFC822.TEXT".equalsIgnoreCase(name)) {
          fetch.add(new BodyFetchElement("RFC822.TEXT", "TEXT"), false);
        } else {
          throw new ProtocolException("Invalid fetch attribute: " + name);
        }
      } else {
        consumeChar(command, '[');

        StringBuilder sectionIdentifier = new StringBuilder();
        next = nextCharInLine(command);
        while (next != ']') {
          sectionIdentifier.append(next);
          command.consume();
          next = nextCharInLine(command);
        }
        consumeChar(command, ']');

        String parameter = sectionIdentifier.toString();

        Partial partial = null;
        next = command.nextChar(); // Can be end of line if single option
        if ('<' == next) { // Partial eg <2000> or <0.1000>
          partial = parsePartial(command);
        }

        if ("BODY".equalsIgnoreCase(name)) {
          fetch.add(new BodyFetchElement("BODY[" + parameter + ']', parameter, partial), false);
        } else if ("BODY.PEEK".equalsIgnoreCase(name)) {
          fetch.add(new BodyFetchElement("BODY[" + parameter + ']', parameter, partial), true);
        } else {
          throw new ProtocolException("Invalid fetch attribute: " + name + "[]");
        }
      }
    }

    private Partial parsePartial(ImapRequestLineReader command) throws ProtocolException {
      consumeChar(command, '<');
      int size = (int) consumeLong(command); // Assume <start>
      int start = 0;
      if (command.nextChar() == '.') {
        consumeChar(command, '.');
        start = size; // Assume <start.size> , so switch fields
        size = (int) consumeLong(command);
      }
      consumeChar(command, '>');
      return Partial.as(start, size);
    }

    private char nextCharInLine(ImapRequestLineReader request)
        throws ProtocolException {
      char next = request.nextChar();
      if (isCrOrLf(next)) {
        request.dumpLine();
        throw new ProtocolException("Unexpected end of line (CR or LF).");
      }
      return next;
    }

    private char nextNonSpaceChar(ImapRequestLineReader request)
        throws ProtocolException {
      char next = request.nextChar();
      while (next == ' ') {
        request.consume();
        next = request.nextChar();
      }
      return next;
    }

  }

  private static class FetchRequest {
    boolean flags;
    boolean uid;
    boolean internalDate;
    boolean size;
    boolean envelope;
    boolean body;
    boolean bodyStructure;

    private boolean setSeen = false;

    private Set<BodyFetchElement> bodyElements = new HashSet<>();

    public Collection<BodyFetchElement> getBodyElements() {
      return bodyElements;
    }

    public boolean isSetSeen() {
      return setSeen;
    }

    public void add(BodyFetchElement element, boolean peek) {
      if (!peek) {
        setSeen = true;
      }
      bodyElements.add(element);
    }
  }

  /**
   * See https://tools.ietf.org/html/rfc3501#page-55 : partial
   */
  private static class Partial {
    int start,
        size;

    int computeLength(final int contentSize) {
      if (size > 0) {
        return Math.min(size, contentSize - start); // Only up to max available bytes
      } else {
        // First len bytes
        return contentSize;
      }
    }

    int computeStart(final int contentSize) {
      return Math.min(start, contentSize);
    }

    public static Partial as(int start, int size) {
      Partial p = new Partial();
      p.start = start;
      p.size = size;
      return p;
    }
  }

  private static class BodyFetchElement {
    private String name;
    private String sectionIdentifier;
    private Partial partial;

    public BodyFetchElement(String name, String sectionIdentifier) {
      this(name, sectionIdentifier, null);
    }

    public BodyFetchElement(String name, String sectionIdentifier, Partial partial) {
      this.name = name;
      this.sectionIdentifier = sectionIdentifier;
      this.partial = partial;
    }

    public String getParameters() {
      return this.sectionIdentifier;
    }

    public String getResponseName() {
      return this.name;
    }

    public Partial getPartial() {
      return partial;
    }
  }

}

/*
6.4.5.  FETCH Command

   Arguments:  message set
               message data item names

   Responses:  untagged responses: FETCH

   Result:     OK - fetch completed
               NO - fetch error: can't fetch that data
               BAD - command unknown or arguments invalid

      The FETCH command retrieves data associated with a message in the
      mailbox.  The data items to be fetched can be either a single atom
      or a parenthesized list.

      The currently defined data items that can be fetched are:

      ALL            Macro equivalent to: (FLAGS INTERNALDATE
                     RFC822.SIZE ENVELOPE)

      BODY           Non-extensible form of BODYSTRUCTURE.

      BODY[<section>]<<partial>>
                     The text of a particular body section.  The section
                     specification is a set of zero or more part
                     specifiers delimited by periods.  A part specifier
                     is either a part number or one of the following:
                     HEADER, HEADER.FIELDS, HEADER.FIELDS.NOT, MIME, and
                     TEXT.  An empty section specification refers to the
                     entire message, including the header.

                     Every message has at least one part number.
                     Non-[MIME-IMB] messages, and non-multipart
                     [MIME-IMB] messages with no encapsulated message,
                     only have a part 1.

                     Multipart messages are assigned consecutive part
                     numbers, as they occur in the message.  If a
                     particular part is of type message or multipart,
                     its parts MUST be indicated by a period followed by
                     the part number within that nested multipart part.

                     A part of type MESSAGE/RFC822 also has nested part
                     numbers, referring to parts of the MESSAGE part's
                     body.

                     The HEADER, HEADER.FIELDS, HEADER.FIELDS.NOT, and
                     TEXT part specifiers can be the sole part specifier
                     or can be prefixed by one or more numeric part
                     specifiers, provided that the numeric part
                     specifier refers to a part of type MESSAGE/RFC822.
                     The MIME part specifier MUST be prefixed by one or
                     more numeric part specifiers.

                     The HEADER, HEADER.FIELDS, and HEADER.FIELDS.NOT
                     part specifiers refer to the [RFC-822] header of
                     the message or of an encapsulated [MIME-IMT]
                     MESSAGE/RFC822 message.  HEADER.FIELDS and
                     HEADER.FIELDS.NOT are followed by a list of
                     field-name (as defined in [RFC-822]) names, and
                     return a subset of the header.  The subset returned
                     by HEADER.FIELDS contains only those header fields
                     with a field-name that matches one of the names in
                     the list; similarly, the subset returned by
                     HEADER.FIELDS.NOT contains only the header fields
                     with a non-matching field-name.  The field-matching
                     is case-insensitive but otherwise exact.  In all
                     cases, the delimiting blank line between the header
                     and the body is always included.

                     The MIME part specifier refers to the [MIME-IMB]
                     header for this part.

                     The TEXT part specifier refers to the text body of
                     the message, omitting the [RFC-822] header.


                       Here is an example of a complex message
                       with some of its part specifiers:

                        HEADER     ([RFC-822] header of the message)
                        TEXT       MULTIPART/MIXED
                        1          TEXT/PLAIN
                        2          APPLICATION/OCTET-STREAM
                        3          MESSAGE/RFC822
                        3.HEADER   ([RFC-822] header of the message)
                        3.TEXT     ([RFC-822] text body of the message)
                        3.1        TEXT/PLAIN
                        3.2        APPLICATION/OCTET-STREAM
                        4          MULTIPART/MIXED
                        4.1        IMAGE/GIF
                        4.1.MIME   ([MIME-IMB] header for the IMAGE/GIF)
                        4.2        MESSAGE/RFC822
                        4.2.HEADER ([RFC-822] header of the message)
                        4.2.TEXT   ([RFC-822] text body of the message)
                        4.2.1      TEXT/PLAIN
                        4.2.2      MULTIPART/ALTERNATIVE
                        4.2.2.1    TEXT/PLAIN
                        4.2.2.2    TEXT/RICHTEXT


                     It is possible to fetch a substring of the
                     designated text.  This is done by appending an open
                     angle bracket ("<"), the octet position of the
                     first desired octet, a period, the maximum number
                     of octets desired, and a close angle bracket (">")
                     to the part specifier.  If the starting octet is
                     beyond the end of the text, an empty string is
                     returned.

                     Any partial fetch that attempts to read beyond the
                     end of the text is truncated as appropriate.  A
                     partial fetch that starts at octet 0 is returned as
                     a partial fetch, even if this truncation happened.

                          Note: this means that BODY[]<0.2048> of a
                          1500-octet message will return BODY[]<0>
                          with a literal of size 1500, not BODY[].

                          Note: a substring fetch of a
                          HEADER.FIELDS or HEADER.FIELDS.NOT part
                          specifier is calculated after subsetting
                          the header.


                     The \Seen flag is implicitly set; if this causes
                     the flags to change they SHOULD be included as part
                     of the FETCH responses.

      BODY.PEEK[<section>]<<partial>>
                     An alternate form of BODY[<section>] that does not
                     implicitly set the \Seen flag.

      BODYSTRUCTURE  The [MIME-IMB] body structure of the message.  This
                     is computed by the server by parsing the [MIME-IMB]
                     header fields in the [RFC-822] header and
                     [MIME-IMB] headers.

      ENVELOPE       The envelope structure of the message.  This is
                     computed by the server by parsing the [RFC-822]
                     header into the component parts, defaulting various
                     fields as necessary.

      FAST           Macro equivalent to: (FLAGS INTERNALDATE
                     RFC822.SIZE)

      FLAGS          The flags that are set for this message.

      FULL           Macro equivalent to: (FLAGS INTERNALDATE
                     RFC822.SIZE ENVELOPE BODY)

      INTERNALDATE   The internal date of the message.

      RFC822         Functionally equivalent to BODY[], differing in the
                     syntax of the resulting untagged FETCH data (RFC822
                     is returned).

      RFC822.HEADER  Functionally equivalent to BODY.PEEK[HEADER],
                     differing in the syntax of the resulting untagged
                     FETCH data (RFC822.HEADER is returned).

      RFC822.SIZE    The [RFC-822] size of the message.

      RFC822.TEXT    Functionally equivalent to BODY[TEXT], differing in
                     the syntax of the resulting untagged FETCH data
                     (RFC822.TEXT is returned).

      UID            The unique identifier for the message.

   Example:    C: A654 FETCH 2:4 (FLAGS BODY[HEADER.FIELDS (DATE FROM)])
               S: * 2 FETCH ....
               S: * 3 FETCH ....
               S: * 4 FETCH ....
               S: A654 OK FETCH completed


7.4.2.  FETCH Response

   Contents:   message data

      The FETCH response returns data about a message to the client.
      The data are pairs of data item names and their values in
      parentheses.  This response occurs as the result of a FETCH or
      STORE command, as well as by unilateral server decision (e.g. flag
      updates).

      The current data items are:

      BODY           A form of BODYSTRUCTURE without extension data.

      BODY[<section>]<<origin_octet>>
                     A string expressing the body contents of the
                     specified section.  The string SHOULD be
                     interpreted by the client according to the content
                     transfer encoding, body type, and subtype.

                     If the origin octet is specified, this string is a
                     substring of the entire body contents, starting at
                     that origin octet.  This means that BODY[]<0> MAY
                     be truncated, but BODY[] is NEVER truncated.

                     8-bit textual data is permitted if a [CHARSET]
                     identifier is part of the body parameter
                     parenthesized list for this section.  Note that
                     headers (part specifiers HEADER or MIME, or the
                     header portion of a MESSAGE/RFC822 part), MUST be
                     7-bit; 8-bit characters are not permitted in
                     headers.  Note also that the blank line at the end
                     of the header is always included in header data.

                     Non-textual data such as binary data MUST be
                     transfer encoded into a textual form such as BASE64
                     prior to being sent to the client.  To derive the
                     original binary data, the client MUST decode the
                     transfer encoded string.

      BODYSTRUCTURE  A parenthesized list that describes the [MIME-IMB]
                     body structure of a message.  This is computed by
                     the server by parsing the [MIME-IMB] header fields,
                     defaulting various fields as necessary.

                     For example, a simple text message of 48 lines and
                     2279 octets can have a body structure of: ("TEXT"
                     "PLAIN" ("CHARSET" "US-ASCII") NIL NIL "7BIT" 2279
                     48)

                     Multiple parts are indicated by parenthesis
                     nesting.  Instead of a body type as the first
                     element of the parenthesized list there is a nested
                     body.  The second element of the parenthesized list
                     is the multipart subtype (mixed, digest, parallel,
                     alternative, etc.).

                     For example, a two part message consisting of a
                     text and a BASE645-encoded text attachment can have
                     a body structure of: (("TEXT" "PLAIN" ("CHARSET"
                     "US-ASCII") NIL NIL "7BIT" 1152 23)("TEXT" "PLAIN"
                     ("CHARSET" "US-ASCII" "NAME" "cc.diff")
                     "<960723163407.20117h@cac.washington.edu>"
                     "Compiler diff" "BASE64" 4554 73) "MIXED"))

                     Extension data follows the multipart subtype.
                     Extension data is never returned with the BODY
                     fetch, but can be returned with a BODYSTRUCTURE
                     fetch.  Extension data, if present, MUST be in the
                     defined order.

                     The extension data of a multipart body part are in
                     the following order:

                     body parameter parenthesized list
                        A parenthesized list of attribute/value pairs
                        [e.g. ("foo" "bar" "baz" "rag") where "bar" is
                        the value of "foo" and "rag" is the value of
                        "baz"] as defined in [MIME-IMB].

                     body disposition
                        A parenthesized list, consisting of a
                        disposition type string followed by a
                        parenthesized list of disposition
                        attribute/value pairs.  The disposition type and
                        attribute names will be defined in a future
                        standards-track revision to [DISPOSITION].

                     body language
                        A string or parenthesized list giving the body
                        language value as defined in [LANGUAGE-TAGS].

                     Any following extension data are not yet defined in
                     this version of the protocol.  Such extension data
                     can consist of zero or more NILs, strings, numbers,
                     or potentially nested parenthesized lists of such
                     data.  Client implementations that do a
                     BODYSTRUCTURE fetch MUST be prepared to accept such
                     extension data.  Server implementations MUST NOT
                     send such extension data until it has been defined
                     by a revision of this protocol.

                     The basic fields of a non-multipart body part are
                     in the following order:

                     body type
                        A string giving the content media type name as
                        defined in [MIME-IMB].

                     body subtype
                        A string giving the content subtype name as
                        defined in [MIME-IMB].

                     body parameter parenthesized list
                        A parenthesized list of attribute/value pairs
                        [e.g. ("foo" "bar" "baz" "rag") where "bar" is
                        the value of "foo" and "rag" is the value of
                        "baz"] as defined in [MIME-IMB].

                     body id
                        A string giving the content id as defined in
                        [MIME-IMB].

                     body description
                        A string giving the content description as
                        defined in [MIME-IMB].

                     body encoding
                        A string giving the content transfer encoding as
                        defined in [MIME-IMB].

                     body size
                        A number giving the size of the body in octets.
                        Note that this size is the size in its transfer
                        encoding and not the resulting size after any
                        decoding.

                     A body type of type MESSAGE and subtype RFC822
                     contains, immediately after the basic fields, the
                     envelope structure, body structure, and size in
                     text lines of the encapsulated message.

                     A body type of type TEXT contains, immediately
                     after the basic fields, the size of the body in
                     text lines.  Note that this size is the size in its
                     content transfer encoding and not the resulting
                     size after any decoding.

                     Extension data follows the basic fields and the
                     type-specific fields listed above.  Extension data
                     is never returned with the BODY fetch, but can be
                     returned with a BODYSTRUCTURE fetch.  Extension
                     data, if present, MUST be in the defined order.

                     The extension data of a non-multipart body part are
                     in the following order:

                     body MD5
                        A string giving the body MD5 value as defined in
                        [MD5].

                     body disposition
                        A parenthesized list with the same content and
                        function as the body disposition for a multipart
                        body part.

                     body language
                        A string or parenthesized list giving the body
                        language value as defined in [LANGUAGE-TAGS].

                     Any following extension data are not yet defined in
                     this version of the protocol, and would be as
                     described above under multipart extension data.

      ENVELOPE       A parenthesized list that describes the envelope
                     structure of a message.  This is computed by the
                     server by parsing the [RFC-822] header into the
                     component parts, defaulting various fields as
                     necessary.

                     The fields of the envelope structure are in the
                     following order: date, subject, from, sender,
                     reply-to, to, cc, bcc, in-reply-to, and message-id.
                     The date, subject, in-reply-to, and message-id
                     fields are strings.  The from, sender, reply-to,
                     to, cc, and bcc fields are parenthesized lists of
                     address structures.

                     An address structure is a parenthesized list that
                     describes an electronic mail address.  The fields
                     of an address structure are in the following order:
                     personal name, [SMTP] at-domain-list (source
                     route), mailbox name, and host name.

                     [RFC-822] group syntax is indicated by a special
                     form of address structure in which the host name
                     field is NIL.  If the mailbox name field is also
                     NIL, this is an end of group marker (semi-colon in
                     RFC 822 syntax).  If the mailbox name field is
                     non-NIL, this is a start of group marker, and the
                     mailbox name field holds the group name phrase.

                     Any field of an envelope or address structure that
                     is not applicable is presented as NIL.  Note that
                     the server MUST default the reply-to and sender
                     fields from the from field; a client is not
                     expected to know to do this.

      FLAGS          A parenthesized list of flags that are set for this
                     message.

      INTERNALDATE   A string representing the internal date of the
                     message.

      RFC822         Equivalent to BODY[].

      RFC822.HEADER  Equivalent to BODY.PEEK[HEADER].

      RFC822.SIZE    A number expressing the [RFC-822] size of the
                     message.

      RFC822.TEXT    Equivalent to BODY[TEXT].

      UID            A number expressing the unique identifier of the
                     message.


   Example:    S: * 23 FETCH (FLAGS (\Seen) RFC822.SIZE 44827)

*/
