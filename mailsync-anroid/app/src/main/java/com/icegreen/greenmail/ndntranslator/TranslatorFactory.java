package com.icegreen.greenmail.ndntranslator;

import android.content.Context;

public class TranslatorFactory {

  private static String DEFAULT_SERVER = "IMAP";

  // Create a NdnTranslator based on given server type
  public static NdnTranslator getNdnTranslator(String server, Context context) {
    switch (server) {
      case "IMAP":
        return new ImapToNdnTranslator(context);
      case "POP3":
        System.out.println("POP3 service to be implemented ...");
      default:
          throw new IllegalArgumentException("Invalid server type: " + server);
    }
  }

  public static NdnTranslator getNdnTranslator(Context context) {
    return getNdnTranslator(DEFAULT_SERVER, context);
  }
}
