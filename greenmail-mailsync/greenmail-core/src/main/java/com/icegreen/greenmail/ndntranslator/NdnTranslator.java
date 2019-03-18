package com.icegreen.greenmail.ndntranslator;

import com.icegreen.greenmail.ndnproxy.Snapshot;
import com.icegreen.greenmail.store.SimpleMessageAttributes;

import javax.mail.internet.MimeMessage;
import java.io.IOException;

public interface NdnTranslator {
  /**
   * Convert IMAP attributes to byte string
   *
   * @param attributes
   */
  String encodeAttribute(SimpleMessageAttributes attributes) throws IOException;

  /**
   * Convert IMAP MimeMessage to byte string
   *
   * @param mimeMessage
   * @return
   */
  String encodeMimeMessage(MimeMessage mimeMessage);

  /**
   * Convert MailFolder to byte string
   *
   * @param snapshot
   * @return
   */
  String encodeMailFolder(Snapshot snapshot);

  /**
   * Generate name for "attribute" data unit
   *
   * @param appName
   * @param userName
   * @param mailboxName
   * @param mailboxVersion
   * @param messageUID
   */
  String generateAttributeName(String appName, String userName, String mailboxName,
                               String mailboxVersion, String messageUID);

  /**
   * Generate name for "MimeMessage" data unit
   *
   * @param appName
   * @param userName
   * @param mailboxName
   * @param mailboxVersion
   * @param messageUID
   * @return
   */
  String generateMimeMessageName(String appName, String userName, String mailboxName,
                                 String mailboxVersion, String messageUID);

  /**
   * Generate name for "MailFolder" data unit
   *
   * @param appName
   * @param userName
   * @param mailboxName
   * @param mailboxVersion
   * @param messageUID
   * @return
   */
  String generateMailFolderName(String appName, String userName, String mailboxName,
                                String mailboxVersion, String messageUID);

  /**
   * Put NDN data units into database
   *
   * @param name
   * @param content
   */
  void saveData(String name, String content, String bucketName);
}
