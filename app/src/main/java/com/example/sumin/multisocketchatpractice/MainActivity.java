package com.example.sumin.multisocketchatpractice;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {

    // private static int port = 5001;
    // private static final String ipText = "192.168.0.7"; // IP지정으로 사용시에 쓸 코드
    String streammsg = "";
    TextView showText;
    Button connectBtn;
    Button Button_send;
    EditText ip_EditText;
    EditText port_EditText;
    EditText id_EditText;
    EditText editText_massage;
    Handler msghandler;

    SocketClient client;
    ReceiveThread receive;
    SendThread send;
    Socket socket;

    PipedInputStream sendstream = null;
    PipedOutputStream receivestream = null;

    LinkedList<SocketClient> threadList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        id_EditText = (EditText) findViewById(R.id.id_EditText);
        ip_EditText = (EditText) findViewById(R.id.ip_EditText);
        port_EditText = (EditText) findViewById(R.id.port_EditText);
        connectBtn = (Button) findViewById(R.id.connect_Button);
        showText = (TextView) findViewById(R.id.showText_TextView);
        editText_massage = (EditText) findViewById(R.id.editText_massage);
        Button_send = (Button) findViewById(R.id.Button_send);
        threadList = new LinkedList<MainActivity.SocketClient>();

        ip_EditText.setText("115.71.232.80");
        port_EditText.setText("5001");

        // ReceiveThread를통해서 받은 메세지를 Handler로 MainThread에서 처리(외부Thread에서는 UI변경이불가)
        msghandler = new Handler() {
            @Override
            public void handleMessage(Message hdmsg) {
                if (hdmsg.what == 1111) {
                    showText.append(hdmsg.obj.toString() + "\n");
                }
            }
        };

        // 연결버튼 클릭 이벤트
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //Client 연결부
                client = new SocketClient(ip_EditText.getText().toString(),
                        port_EditText.getText().toString());
                threadList.add(client);
                client.start();
            }
        });

        //전송 버튼 클릭 이벤트
        Button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                //SendThread 시작
                if (editText_massage.getText().toString() != null) {
                    send = new SendThread(socket);
                    send.start();

                    //시작후 edittext 초기화
                    editText_massage.setText("");
                }
            }
        });
    }


    class SocketClient extends Thread {
        boolean threadAlive;
        String ip;
        String port;
        String mac;

        //InputStream inputStream = null;
        OutputStream outputStream = null;
        BufferedReader br = null;

        private DataOutputStream output = null;

        public SocketClient(String ip, String port) {
            threadAlive = true;
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {

            try {
                // 연결후 바로 ReceiveThread 시작
                Log.d("연결", "SocketClient 쓰레드 시작. " + ip + ", 그리고 포트는 " +port);
                socket = new Socket(ip, Integer.parseInt(port));
                Log.d("연결", "SocketClient 쓰레드 시작. 어디서");
                //inputStream = socket.getInputStream();
                output = new DataOutputStream(socket.getOutputStream());
                Log.d("연결", "SocketClient 쓰레드 시작. 문제");
                receive = new ReceiveThread(socket);
                Log.d("연결", "SocketClient 쓰레드 시작. 일까");
                receive.start();
                Log.d("연결", "SocketClient 쓰레드 시작. 요");

                //mac주소를 받아오기위해 설정
                //★★★★★★★ API 24 이상부터는 getApplicationContext()를 통해서 getSystemService를 써야한다.
                WifiManager mng = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo info = mng.getConnectionInfo();
                Log.d("연결", "SocketClient 쓰레드 시작. 알아");

                //mac = info.getMacAddress();
                mac = id_EditText.getText().toString();
                Log.d("연결", "SocketClient 쓰레드 시작. 맞춰");
                if(mac != null)
                {
                    Log.d("연결", "SocketClient 쓰레드 시작. 봅시다");
                    Log.d("연결", "널 아닌데?");
                }
                else
                {
                    Log.d("연결", "널인데?");
                }
                Log.d("연결", "SocketClient 쓰레드 시작. - mac : " + mac);

                //mac 전송
                output.writeUTF(mac);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ReceiveThread extends Thread {
        private Socket socket = null;
        DataInputStream input;

        public ReceiveThread(Socket socket) {
            this.socket = socket;
            try{
                input = new DataInputStream(socket.getInputStream());
            }catch(Exception e){
            }
        }
        // 메세지 수신후 Handler로 전달
        public void run() {
            try {
                while (input != null) {

                    String msg = input.readUTF();
                    Log.d("연결", "ReceiveThread - 핸들러로 전달 if문 이전.");
                    if (msg != null) {
                        Log.d("연결", "ReceiveThread - 핸들러로 전달");

                        Message hdmsg = msghandler.obtainMessage();
                        hdmsg.what = 1111;
                        hdmsg.obj = msg;
                        msghandler.sendMessage(hdmsg);
                        Log.d("연결",hdmsg.obj.toString());
                    }
                }
                Log.d("리시버", "ReceiveThread - 종료");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    class SendThread extends Thread {
        private Socket socket;
        String sendmsg = editText_massage.getText().toString();
        DataOutputStream output;

        public SendThread(Socket socket) {
            this.socket = socket;
            try {
                output = new DataOutputStream(socket.getOutputStream());
            }
            catch (Exception e) {
            }
        }

        public void run() {

            try {

                // 메세지 전송부 (누군지 식별하기위한 방법으로 mac를 사용)
                Log.d("연결", "SendThread");
                String mac = null;
                WifiManager mng = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo info = mng.getConnectionInfo();
                mac = info.getMacAddress();

                if (output != null) {
                    if (sendmsg != null) {
                        output.writeUTF(mac + "  :  " +sendmsg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException npe) {
                npe.printStackTrace();

            }
        }
    }
}
