package com.example.forum_9.smarthome;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView tempData, voltData, wattData, humData;
    SeekBar lightSeek, curtainSeek;
    Button openDoor, setLight, setCurtain;
    Handler inputBluetooth;
    final int handlerState = 0;
    private StringBuilder receivedData = new StringBuilder();
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private static final UUID BTUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String for MAC address
    private static String MAC;
    private SendThread sendThread;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LineChart lineChart = (LineChart) findViewById(R.id.chart);
        tempData = (TextView) findViewById(R.id.temp_data);
        voltData = (TextView) findViewById(R.id.volt_data);
        wattData = (TextView) findViewById(R.id.watt_data);
        humData = (TextView) findViewById(R.id.hum_data);
        lightSeek = (SeekBar) findViewById(R.id.light_seek);
        curtainSeek = (SeekBar) findViewById(R.id.curtain_seek);
        openDoor = (Button) findViewById(R.id.open_door);
        setLight = (Button) findViewById(R.id.set_light);
        setCurtain = (Button) findViewById(R.id.set_curtain);

        inputBluetooth = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                        //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    receivedData.append(readMessage);                                    //keep appending to string until ~
                    int endOfLineIndex = receivedData.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = receivedData.substring(0, endOfLineIndex);    // extract string

                        if (receivedData.charAt(0) == '#')                                //if it starts with # we know it is what we are looking for
                        {
                            String temperature = receivedData.substring(1, 5);             //get sensor value from string between indices 1-5
                            String humidity = receivedData.substring(6, 10);            //same again...
                            String voltage = receivedData.substring(11, 15);
                            String wattage = receivedData.substring(16, 20);

                            tempData.setText(temperature + " C");    //update the textviews with sensor values
                            humData.setText(humidity + " PH");
                            voltData.setText(voltage + " V");
                            wattData.setText(wattage + "V");
                        }
                        receivedData.delete(0, receivedData.length());                    //clear all string data

                    }
                }
            }
        };
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
        setLight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendThread.write(String.valueOf(lightSeek.getProgress())+"q");    // Send "0" via Bluetooth
                Toast.makeText(getBaseContext(),  String.valueOf(lightSeek.getProgress())+"q", Toast.LENGTH_SHORT).show();
            }
        });
        setCurtain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendThread.write(String.valueOf(curtainSeek.getProgress())+"w");    // Send "0" via Bluetooth
                Toast.makeText(getBaseContext(),  String.valueOf(curtainSeek.getProgress())+"w", Toast.LENGTH_SHORT).show();
            }
        });


        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(4f, 0));
        entries.add(new Entry(8f, 1));
        entries.add(new Entry(6f, 2));
        entries.add(new Entry(2f, 3));
        entries.add(new Entry(18f, 4));
        entries.add(new Entry(9f, 5));

        LineDataSet dataset = new LineDataSet(entries, "# of Calls");

        ArrayList<String> labels = new ArrayList<String>();
        labels.add("January");
        labels.add("February");
        labels.add("March");
        labels.add("April");
        labels.add("May");
        labels.add("June");

        LineData data = new LineData(labels, dataset);
        dataset.setColors(ColorTemplate.COLORFUL_COLORS); //
        dataset.setDrawCubic(true);
        dataset.setDrawFilled(true);
        dataset.setValueTextColor(Color.WHITE);
        dataset.setColor(Color.RED);
        dataset.setFillColor(Color.BLUE);
        dataset.setCircleColor(Color.YELLOW);
        lineChart.setDrawGridBackground(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setTextColor(Color.WHITE);

        lineChart.setData(data);
        lineChart.animateY(5000);

    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return device.createRfcommSocketToServiceRecord(BTUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();
        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        MAC = intent.getStringExtra(ConnectBluetooth.EXTRA_ADDRESS);
        BluetoothDevice device = btAdapter.getRemoteDevice(MAC);
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Toast.makeText(getBaseContext(), "Cannot close socket", Toast.LENGTH_LONG).show();
            }
        }
        sendThread = new SendThread(btSocket);
        sendThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        sendThread.write("x");

    }


    //for menu TODO
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                Toast.makeText(this, "Refresh  selected", Toast.LENGTH_SHORT)
                        .show();
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT)
                        .show();
                break;
            default:
                break;
        }

        return true;
    }

    //create new class for connect thread
    private class SendThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public SendThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    inputBluetooth.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
}
