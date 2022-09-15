package com.example.zenbocontrolserver;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.asus.robotframework.API.MotionControl;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.RobotAPI;

public class MainActivity extends RobotActivity {

    DataOutputStream out;
    TextView idReveal;
    TextView tvMessage;
    ArrayList<Socket> client = new ArrayList<Socket>();

    public MainActivity() {
        super(robotCallback, robotListenCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        idReveal = (TextView) findViewById(R.id.idReveal);
        tvMessage = (TextView) findViewById(R.id.tvMessage);
        serverThread server = new serverThread("server");
        Thread m1=new Thread(server);
        m1.start();
    }

    public static RobotCallback robotCallback = new RobotCallback() {
        @Override
        public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
            super.onResult(cmd, serial, err_code, result);

        }

        @Override
        public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
            super.onStateChange(cmd, serial, err_code, state);
        }

        @Override
        public void initComplete() {
            super.initComplete();

        }
    };

    public static RobotCallback.Listen robotListenCallback = new RobotCallback.Listen() {
        @Override
        public void onFinishRegister() {

        }

        @Override
        public void onVoiceDetect(JSONObject jsonObject) {

        }

        @Override
        public void onSpeakComplete(String s, String s1) {

        }

        @Override
        public void onEventUserUtterance(JSONObject jsonObject) {

        }

        @Override
        public void onResult(JSONObject jsonObject) {

        }

        @Override
        public void onRetry(JSONObject jsonObject) {

        }
    };

    class serverThread implements Runnable{
        private String name;
        public serverThread(String str)
        {
            name=str;
        }
        @Override
        public void run(){
            try{
                ServerSocket serverSocket = new ServerSocket(7100);
                String localIP = getLocalIpAddress();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        idReveal.setText("server IP is "+localIP);
                    }
                });
                try{
                    while(true) {
                        Socket socket = serverSocket.accept();
                        client.add(socket);
                        Handler server2 = new Handler(socket);
                        Thread m2 = new Thread(server2);
                        m2.start();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Handler implements Runnable{
        private Socket socket;
        public Handler(Socket socket){
            this.socket = socket;
        }
        public void run(){
            try {
                //取得網路輸入串流
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //取得網路輸出串流
                out = new DataOutputStream(socket.getOutputStream());
                String tmp;
                while ((tmp = reader.readLine()) != null) {
                    JSONObject jsonObj = new JSONObject(tmp);
                    String finalTmp = tmp;
                    runOnUiThread(new Runnable() {
                        JSONObject _jsonObj;
                        @Override
                        public void run() {
                            try{
                                String type = jsonObj.getString("type");
                                String content = jsonObj.getString("content");
                                tvMessage.setText(finalTmp);
                                if(type.equals("speak")){
                                    robotAPI.robot.speak(content);
                                }
                                if(type.equals("move")){
                                    if(content.equals("forward")){
                                        robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.FORWARD);
                                    }
                                    else if(content.equals("left")){
                                        robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.TURN_LEFT);
                                    }
                                    else if(content.equals("right")){
                                        robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.TURN_RIGHT);
                                    }
                                    else if(content.equals("backward")){
                                        robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.BACKWARD);
                                    }
                                    else if(content.equals("stop")){
                                        robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.STOP);
                                    }
                                }
                            }catch(JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        public Runnable init(JSONObject jsonObj) {
                            _jsonObj = jsonObj;
                            return this;
                        }
                    }.init(jsonObj));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class SendData implements Runnable{
        private String message;
        SendData(String message) {
            this.message = message;
        }
        @Override
        public  void run() {
            Socket[] cs = new Socket[client.size()];
            client.toArray(cs);
            for(Socket socket: cs) {
                try {
                    out = new DataOutputStream(socket.getOutputStream());
                    Map<String, Object> map = new HashMap();
                    map.put("message", message);
                    map.put("name", "server");
                    try {
                        JSONObject json = new JSONObject(map);
                        byte[] jsonByte = (json.toString() + "\n").getBytes();
                        out.write(jsonByte);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getLocalIpAddress(){
        try{
            for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface intf = en.nextElement();
                for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();){
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()){
                        return inetAddress.getHostAddress();
                    }
                }
            }
        }catch (SocketException ex){
            Log.e("WifiPreference IpAddress", ex.toString());
        }
        return null;
    }
}