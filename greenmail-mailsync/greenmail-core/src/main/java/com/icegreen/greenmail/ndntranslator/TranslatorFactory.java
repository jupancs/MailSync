package com.icegreen.greenmail.ndntranslator;

public class TranslatorFactory {

  private static String DEFAULT_SERVER = "IMAP";

  // Create a NdnTranslator based on given server type
  public static NdnTranslator getNdnTranslator(String server, String bucketName) {
    switch (server) {
      case "IMAP":
        return new ImapToNdnTranslator(bucketName);
      case "POP3":
        System.out.println("POP3 service to be implemented ...");
      default:
          throw new IllegalArgumentException("Invalid server type: " + server);
    }
  }

  public static NdnTranslator getNdnTranslator(String bucketName) {
    return getNdnTranslator(DEFAULT_SERVER, bucketName);
  }
}
