package com.horecka.acctcp;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;
import android.text.method.*;

public class MainActivity extends Activity {

    public int PORT = 12345; 
    private Button connectPhones;
    private String serverIpAddress = "192.168.1.24";
    private boolean connected = false;
    TextView text;
    EditText port;
    EditText ipAdr;
	TextView log;
    private float x,y,z;
    private SensorManager sensorManager;
    private Sensor sensor;
    boolean acc_disp = false;
    boolean isStreaming = false;
    PrintWriter out;
	Thread cThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        connectPhones = (Button)findViewById(R.id.send);
        connectPhones.setOnClickListener(connectListener);
        text=(TextView)findViewById(R.id.textin);
        port=(EditText)findViewById(R.id.port);
        ipAdr=(EditText)findViewById(R.id.ipadr);
		log=(TextView)findViewById(R.id.log);
		log.setMovementMethod(new ScrollingMovementMethod());
        text.setText("Press send to stream acceleration measurement");
		log.setText("Log\n");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        port.setText("12345");
        ipAdr.setText(serverIpAddress);
        acc_disp =false;
    }

    private Button.OnClickListener connectListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!connected) {
                if (!serverIpAddress.equals("")&&cThread==null) {
                    cThread = new Thread(new ClientThread());
                    cThread.start();
                }
            }
            else{
                connectPhones.setText("Start Streaming");
                connected=false;
                acc_disp=false;
				cThread=null;
            }
        }
    };

    public class ClientThread implements Runnable {
        Socket socket;
        public void run() {
            try {
                PORT = Integer.parseInt(port.getText().toString());
                serverIpAddress=ipAdr.getText().toString();
                InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
				runOnUiThread(new Runnable() {
						@Override
						public void run() {
							log.setText(log.getText()+"\n"+"Attempting Connection to "+serverIpAddress+" on port "+PORT+".");
							int scrollAmount = log.getLayout().getLineTop(log.getLineCount()) - log.getHeight();
							// if there is no need to scroll, scrollAmount will be <=0
							if (scrollAmount > 0)
								log.scrollTo(0, scrollAmount);
							else
								log.scrollTo(0, 0);
							connectPhones.setClickable(false);
							connectPhones.setAlpha(0.5f);
						}
					});
                socket = new Socket(serverAddr, PORT);
				if(socket.isConnected()){
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								log.setText(log.getText()+"\n"+"Connection Established.");
								connectPhones.setClickable(true);
								connectPhones.setAlpha(1f);
							}
						});
					acc_disp=true;
                	connected = true;
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								connectPhones.setText("Stop Streaming");
							}
						});
                	out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                	while (connected) {
						String vals = Float.toString(x) + "," + Float.toString(y) + "," + Float.toString(z);
                    	out.printf(vals + "\n", x);
                    	out.flush();
                    	Thread.sleep(2);
                	}
				}
				else{
					Log.d("Connection Error", "Could not establish connection.");
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								log.setText(log.getText()+"\n"+"Connection Failed: No Connection Established");
							}
						});
				}
            } 
            catch (Exception e) {
				Log.d("Connection Exception", e.getMessage());
				runOnUiThread(new Runnable() {
						@Override
						public void run() {
							log.setText(log.getText()+"\n"+"Connection Failed: Exception Attempting Connection");
						}
					});
            }
            finally{
                try{
                    acc_disp=false;
                    connected=false;
                    connectPhones.setText("Start Streaming");
                    socket.close();
					Log.d("Connection Closed", "Connection was closed as expected.");
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								log.setText(log.getText()+"\n"+"Connection Closed Gracefully.");
							}
						});
                }catch(Exception a){
					Log.d("Connection Error", "There was an error closing the connection.");
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								log.setText(log.getText()+"\n"+"Connection Failed: Error Closing Connection");
							}
						});
                }
            }
			cThread=null;
			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						connectPhones.setClickable(true);
						connectPhones.setAlpha(1f);
					}
			});
        }
    };

	private void init_perif(){    
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(accelerationListener, sensor,
									   SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        sensorManager.unregisterListener(accelerationListener);
        super.onStop();
    }

    private SensorEventListener accelerationListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }
        @Override
        public void onSensorChanged(SensorEvent event) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            refreshDisplay();
        }
    };

    private void refreshDisplay() {
        if(acc_disp == true){
            String output = String.format("X:%3.2f m/s^2  |  Y:%3.2f m/s^2  |   Z:%3.2f m/s^2", x, y, z);
            text.setText(output);
        }
    }
}
