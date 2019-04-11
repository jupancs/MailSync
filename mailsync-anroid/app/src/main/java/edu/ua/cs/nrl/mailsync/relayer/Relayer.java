package edu.ua.cs.nrl.mailsync.relayer;

import android.content.Context;
import android.os.AsyncTask;

import java.net.*;
import java.io.*;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import edu.ua.cs.nrl.mailsync.fragments.MainServerFragment;

public class Relayer extends AsyncTask<String, Void, String> {

  private Context context;
  private int serverPort;

  private ServerSocket serverSocket;

  private InputStream inFromGoogle;
  private OutputStream outToGoogle;

  private InputStream inFromClient;
  private OutputStream outToClient;

  private OutputStream outToGreen;

  private boolean handshake = false;

  private Thread clientInputThread;
  private Thread googleInputThread;

  public Relayer(int serverPort, Context context) {

    this.serverPort = serverPort;
    this.context = context;
  }

  @Override
  protected String doInBackground(String[] s) {
    initServerSocket();
    start();
    return null;
  }

  /**
   * Start threads to handle traffic
   */
  private void start() {
    while (!MainServerFragment.stop) {
      initStreams(handshake);
      handshake = true;

      clientInputThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            handleClientInput(inFromClient, outToGoogle, outToGreen);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });

      googleInputThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            handlerGoogleInput(inFromGoogle, outToClient);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });

      clientInputThread.start();
      googleInputThread.start();

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

//      SSLSocket greenSslSocket =
//          (SSLSocket) SSLSocketFactory.getDefault().createSocket("127.0.0.1", 3144);

      Socket socket = new Socket("127.0.0.1", 3144);

      if (!handshake) {
        googleSslSocket.startHandshake();
      }

      inFromClient = clientSocket.getInputStream();
      outToGoogle = googleSslSocket.getOutputStream();

      inFromGoogle = googleSslSocket.getInputStream();
      outToClient = clientSocket.getOutputStream();

      outToGreen = socket.getOutputStream();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Convert Google's input to the output that will be sent to the client.
   *
   * @param in InputStream from Google server
   * @param out OutputStream to the client
   * @throws IOException
   */
  private void handlerGoogleInput(InputStream in, OutputStream out)
      throws IOException {
    System.out.println("************* Google*************");

    byte[] buffer = new byte[1024];
    int len;
    while (!MainServerFragment.stop && (len = in.read(buffer, 0, buffer.length)) > -1) {
      out.write(buffer, 0, len);
//      System.out.println("Google >>> " + new String(buffer));
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
  private void handleClientInput(InputStream in, OutputStream out, OutputStream outToGreen)
      throws IOException {
    System.out.println("------------- Client -------------");

    byte[] buffer = new byte[1024];
    byte[] fakeBuffer;
    int len;
    while (!MainServerFragment.stop && (len = in.read(buffer, 0, buffer.length)) > -1) {
      fakeBuffer = Arrays.copyOf(buffer, buffer.length);
      out.write(buffer, 0, len);
      outToGreen.write(fakeBuffer, 0, len);
//      System.out.println("Client >>> " + new String(buffer));
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

  public void closeThread() {

  }
}
