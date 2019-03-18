package com.icegreen.greenmail;

import java.net.*;
import java.io.*;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class Relayer {

  private int serverPort;

  private ServerSocket serverSocket;

  private InputStream inFromGoogle;
  private OutputStream outToGoogle;

  private InputStream inFromClient;
  private OutputStream outToClient;

  private  InputStream fakeInFromGoogle;

  private boolean handshake = false;

  private ByteArrayOutputStream baos = new ByteArrayOutputStream();

  private Thread clientInputThread;
  private Thread googleInputThread;
  private Thread fakeGoogleInput;

  public Relayer(int serverPort) {
    this.serverPort = serverPort;
  }

  public void run() {
    initServerSocket();
    start();
  }

  /**
   * Start threads to handle traffic
   */
  private void start() {
    while (!ExternalProxy.relayerStop) {
      initStreams(handshake);
      handshake = true;

      clientInputThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            handleClientInput(inFromClient, outToGoogle);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });

      googleInputThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            handlerGoogleInput(inFromGoogle, outToClient, baos);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });

      fakeGoogleInput = new Thread(new Runnable() {
        @Override
        public void run() {
          fakeInFromGoogle = new ByteArrayInputStream(baos.toByteArray());
        }
      });

      clientInputThread.start();
      googleInputThread.start();
      fakeGoogleInput.start();

    }
    try {
      serverSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Initialize server socket for relayer.
   */
  private void initServerSocket() {
    try {
      serverSocket = new ServerSocket(serverPort);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Initialize input & output streams
   *
   * @param handshake already finished the handshake with Google?
   */
  private void initStreams(boolean handshake) {
    try {
      Socket clientSocket = serverSocket.accept();
      SSLSocket googleSslSocket =
          (SSLSocket) SSLSocketFactory.getDefault().createSocket("imap.gmail.com", 993);

      if (!handshake) {
        googleSslSocket.startHandshake();
      }

      inFromClient = clientSocket.getInputStream();
      outToGoogle = googleSslSocket.getOutputStream();

      inFromGoogle = googleSslSocket.getInputStream();
      outToClient = clientSocket.getOutputStream();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Convert Google's input to the output that will be sent to the client.
   *
   * @param in InputStream from Google server
   * @param out OutputStream to the client
   * @param baos Global baos for cloning InputStream from Google server
   * @throws IOException
   */
  private void handlerGoogleInput(InputStream in, OutputStream out, ByteArrayOutputStream baos)
      throws IOException {
    System.out.println("************* Google*************");

    byte[] buffer = new byte[1024];
    byte[] fakeBuffer;
    int len;
    while ((len = in.read(buffer, 0, buffer.length)) > -1) {
      fakeBuffer = Arrays.copyOf(buffer, buffer.length);
      out.write(buffer, 0, len);
      baos.write(fakeBuffer, 0, len);
      System.out.println("Google >>> " + new String(buffer));
    }
    System.out.println("*********************************");
  }

  /**
   * Convert Client's input to the output that will be sent to the Google server
   *
   * @param in InputStream from the client
   * @param out OutputStream to the Google server
   * @throws IOException
   */
  private void handleClientInput(InputStream in, OutputStream out)
      throws IOException {
    System.out.println("------------- Client -------------");

    byte[] buffer = new byte[1024];
    int len;
    while ((len = in.read(buffer, 0, buffer.length)) > -1) {
      out.write(buffer, 0, len);
      System.out.println("Client >>> " + new String(buffer));
    }

    System.out.println("-----------------------------------");
  }

  /**
   * Get serverSocket of relayer
   *
   * @return serverSocket
   */
  public ServerSocket getServerSocket() {
    return serverSocket;
  }

}
