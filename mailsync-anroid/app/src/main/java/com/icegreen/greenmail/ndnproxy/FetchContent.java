package com.icegreen.greenmail.ndnproxy;

import java.util.List;

import javax.mail.internet.MimeMessage;

/**
 * FetchContent: this is a class to deal
 *
 */
public class FetchContent {
  private List<MimeMessage> messageList;

  public FetchContent(List<MimeMessage> messageList) {
    this.messageList = messageList;
  }

  public List<MimeMessage> getMimeMessage() {
    return messageList;
  }


}
