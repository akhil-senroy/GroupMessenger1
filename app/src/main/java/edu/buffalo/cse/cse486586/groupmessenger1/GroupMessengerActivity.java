
package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
 public class GroupMessengerActivity extends Activity
 {
     static final String [] port_list = {"11108", "11112", "11116", "11120", "11124" };
     static final int SERVER_PORT = 10000;
     static final String TAG = GroupMessengerActivity.class.getSimpleName();


     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_group_messenger);

         TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
         String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
         final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

         try {
             ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
             new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
         } catch (IOException e) {
             Log.e(TAG, "Can't create a ServerSocket");
             return;
         }

         TextView tv = (TextView) findViewById(R.id.textView1);
         tv.setMovementMethod(new ScrollingMovementMethod());

         /*
          * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
          * OnPTestClickListener demonstrates how to access a ContentProvider.
          */

         findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(tv, getContentResolver()));
         final EditText editText = (EditText) findViewById(R.id.editText1);
         findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v)
             {
                 // taken from pa1 - SimpleMessenger
                 String msg = editText.getText().toString() + "\n";
                 editText.setText(""); // This is one way to reset the input box.

                     /*
                      * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                      * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                      * the difference, please take a look at
                      * http://developer.android.com/reference/android/os/AsyncTask.html
                      */
                 new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
             }
         });
     }

     @Override
     public boolean onCreateOptionsMenu(Menu menu)
     {
         // Inflate the menu; this adds items to the action bar if it is present.
         getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
         return true;
     }

     private class ServerTask extends AsyncTask<ServerSocket, String, Void>
     {
         private Uri mUri = null;
         int incr = 0;
         private Uri buildUri(String scheme, String authority)
         {
             Uri.Builder uriBuilder = new Uri.Builder();
             uriBuilder.authority(authority);
             uriBuilder.scheme(scheme);
             return uriBuilder.build();
         }

         @Override
         protected Void doInBackground(ServerSocket... sockets)
         {

             mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
             Socket socket = null;
             String str;
             ServerSocket serverSocket = sockets[0];

             do {
                 try {
                     try {
                         socket = serverSocket.accept(); //This establishes connection
                     }catch(Exception e){
                         Log.e(TAG," Failed ");
                     }
                     //to read primitive Java data types from an underlying input stream
                     DataInputStream dis = new DataInputStream(socket.getInputStream());
                     str = dis.readUTF();
                     ContentValues keyValueToInsert = new ContentValues();

                     // inserting <”key-to-insert”, “value-to-insert”>
                     keyValueToInsert.put("key", Integer.toString(incr++));
                     keyValueToInsert.put("value", str);

                     Uri newUri = getContentResolver().insert(
                             mUri,
                             keyValueToInsert
                     );
                     publishProgress(str);
                 } catch (Exception e) {
                     Log.e(TAG," Failed ");
                 }
             }while(!socket.isInputShutdown()) ; //while Input Stream is open

             return null;
         }


         protected void onProgressUpdate(String...strings)
         {
           /*
            * The following code displays what is received in doInBackground().
            */
             String strReceived = strings[0].trim();
             TextView remoteTextView = (TextView) findViewById(R.id.textView1);
             remoteTextView.append(strReceived + "\t\n");
             TextView localTextView = (TextView) findViewById(R.id.textView1);
             localTextView.append("\n");

             return;
         }
     }

     /***
      * ClientTask is an AsyncTask that should send a string over the network.
      * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
      * an enter key press event.
      *
      * @author stevko
      *
      */
     private class ClientTask extends AsyncTask<String, Void, Void>
     {
         private OutputStream out;


         @Override
         protected Void doInBackground(String... msgs)
         {

             for(int i=0; i<port_list.length;i++)
             {
                 try
                 {
                     String rem_PortNo = port_list[i];
                     Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                             Integer.parseInt(rem_PortNo));

                     String msgToSend = msgs[0];
                     //Ref : https://www.javatpoint.com/socket-programming
                     //primitives to an output source
                     DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                     //to write a string to the underlying output stream
                     dout.writeUTF(msgToSend);
                     dout.flush(); // the last data written gets out to the file.
                     socket.close();
                 }
                 catch (UnknownHostException e)
                 {
                     Log.e(TAG, "ClientTask UnknownHostException");
                 }
                 catch (IOException e)
                 {
                     Log.e(TAG, "ClientTask socket IOException");
                 }
             }

             return null;
         }
     }
 }
