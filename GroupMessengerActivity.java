package edu.buffalo.cse.cse486586.groupmessenger2;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
class Sequencer{
    int mseq;
    String port;

    public Sequencer() {
    }
}
class Message
{
    String msg;
    int mid;
    String source;
    int seq;
    String dest;
    boolean status;

    public Message() {
    }

    public Message(String msg, int mid, String source, int seq, String dest, boolean status) {
        this.msg = msg;
        this.mid = mid;
        this.source = source;
        this.seq = seq;
        this.dest = dest;
        this.status = status;
    }
}
public class GroupMessengerActivity extends Activity {

    static final String[] remotePorts = {"11108", "11112", "11116", "11120", "11124"};

    private int counter1 = 0;
    private int counter2 = 0;
    private int seq =0;
    private String dead ="0";

    private PriorityQueue<Message> holdBackQueue= new PriorityQueue<Message>(10, new Comparator<Message>() {
        public int compare(Message message1, Message message2)
        {
            if (message1.seq<message2.seq)
                return -1;
            else if (message1.seq>message2.seq)
                return 1;
            else
            {
                if (Integer.parseInt(message1.source)<Integer.parseInt(message2.source))
                    return -1;
                else
                    return 1;
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {

            ServerSocket serverSocket = new ServerSocket(10000);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException e)
        {
            return;
        }


        final Button button = (Button) findViewById(R.id.button4);
        final EditText editText = (EditText) findViewById(R.id.editText1);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                /*
                 * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                 * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                 * an AsyncTask that sends the string to the remote AVD.
                 */
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                //TextView localTextView = (TextView) findViewById(R.id.textView1);
                //localTextView.append("\t" + msg); // This is one way to display a string.
                //TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                //remoteTextView.append("\n");

                /*
                 * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                 * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                 * the difference, please take a look at
                 * http://developer.android.com/reference/android/os/AsyncTask.html
                 */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);



            }
        });

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        public Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        @Override
        protected Void doInBackground(ServerSocket... sockets){

            ServerSocket serverSocket = sockets[0];

            while (true){
                try {

                    Socket server = serverSocket.accept();
                    server.setTcpNoDelay(true);
                    DataInputStream reader = new DataInputStream(server.getInputStream());
                    String msg = reader.readUTF();
                    String[] msgs = msg.split(":");
                    String type = msgs[0];

                    if(type.equalsIgnoreCase("multi")){
                        seq++;
                        DataOutputStream writer = new DataOutputStream(server.getOutputStream());
                        //String msgtosend = "ack" + ":" + msgs[2] + ":" + Integer.toString(seq) + ":" + msgs[4];
                        writer.writeUTF("ack" + ":" + msgs[2] + ":" + Integer.toString(seq) + ":" + msgs[4]);
                        writer.flush();
                        Message tempmesaage = new Message();
                        tempmesaage.msg = msgs[1];
                        tempmesaage.dest = msgs[4];
                        tempmesaage.source = msgs[3];
                        tempmesaage.mid = Integer.parseInt(msgs[2]);
                        tempmesaage.status = false;
                        holdBackQueue.add(tempmesaage);

                        dead=msgs[5];
                        if (Integer.parseInt(dead)!=0)
                            for (Message m1:holdBackQueue)
                                if(Integer.parseInt(m1.source)==Integer.parseInt(dead))
                                    holdBackQueue.remove(m1);
                    }

                    if (type.equalsIgnoreCase("reply")){
                        int seqprime = Integer.parseInt(msgs[3]);
                        if (seqprime > seq){
                            seq = seqprime;
                        }

                        for (Message m2:holdBackQueue)
                            if (m2.mid == Integer.parseInt(msgs[1]) && Integer.parseInt(m2.source) == Integer.parseInt(msgs[2])){
                                holdBackQueue.remove(m2);
                                holdBackQueue.add(new Message(m2.msg,m2.mid,m2.source,seqprime,msgs[4],true));
                            }

                        dead=msgs[6];
                        if (Integer.parseInt(dead)!=0)
                            for (Message m3:holdBackQueue)
                                if(Integer.parseInt(m3.source)==Integer.parseInt(dead))
                                    holdBackQueue.remove(m3);
                    }
                    while (holdBackQueue.peek()!=null){
                        if (holdBackQueue.peek().status){
                            Message temp = holdBackQueue.poll();
                            ContentValues cv = new ContentValues();
                            cv.put("key", counter1);
                            cv.put("value", temp.msg);
                            getContentResolver().insert(buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider"), cv);
                            publishProgress(String.valueOf(counter1),temp.msg);
                            counter1 ++;
                        }
                        else
                            break;

                    }
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }


        protected void onProgressUpdate(String... strings){
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[1].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(/*strings[0]+" "+*/strReceived + "\n");
            // TextView localTextView = (TextView) findViewById(R.id.textView1);
            //localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;


            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }


            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        ArrayList<Sequencer> sequencerArrayList = new ArrayList<Sequencer>();

        @Override
        protected Void doInBackground(String... msgs) {
            counter2=counter2+1;
            String msgseq = Integer.toString(counter2)+msgs[1];
            Sequencer sequencer = new Sequencer();

            for(String port:remotePorts){

                if (Integer.parseInt(port) != Integer.parseInt(dead)) {

                    Socket socket = null;
                    try {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port));
                        socket.setTcpNoDelay(true);
                        DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
                        String msgToSend =  "multi:" + msgs[0] + ":" + msgseq + ":" + msgs[1] + ":" + port+":"+dead;
                        writer.writeUTF(msgToSend);
                        writer.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    DataInputStream reader = null;
                    String ack = null;
                    try {
                        reader = new DataInputStream(socket.getInputStream());
                        if (reader != null) {
                            ack = reader.readUTF();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        dead = port;
                    }
                    try {
                        String[] strings = ack.split("\\:");
                        sequencer.mseq = Integer.parseInt(strings[2]);
                        sequencer.port = strings[3];
                        Thread.sleep(200);
                        socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sequencerArrayList.add(sequencer);
                    Collections.sort(sequencerArrayList, new Comparator<Sequencer>() {
                        @Override
                        public int compare(Sequencer lhs, Sequencer rhs) {
                            if (lhs.mseq<rhs.mseq)
                                return -1;
                            else if (lhs.mseq>rhs.mseq)
                                return 1;
                            else {
                                if (Integer.parseInt(lhs.port)<Integer.parseInt(rhs.port))
                                    return 1;
                                else
                                    return -1;
                            }
                        }
                    });
                }
            }

            sequencer = sequencerArrayList.get(0);

            for(String port:remotePorts){
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(port));
                    socket.setTcpNoDelay(true);
                    String msgToSend = "reply:" + msgseq + ":" + msgs[1] + ":" + sequencer.mseq + ":" +sequencer.port+":"+ port+":"+dead;
                    DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
                    writer.writeUTF(msgToSend);
                    writer.flush();
                    Thread.sleep(200);
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}