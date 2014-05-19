package com.example.android.SmartEPW;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.SmartEPW.util.FormatConvert;
import com.zerokol.views.JoystickView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;


public class ControlEPW_Fragment extends Fragment {
    // Debugging
    private static final String TAG = "JackABK Debug";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;


    /*SeekBar  and the relative TextView */
    private TextView seekBarValue;
    private SeekBar seekBar;

    /*Button for settings of speed and delay*/
    private Button mSpeedPlusBtn,mSpeedMinusBtn = null ;
    private Button mDurationPlusBtn,mDurationMinusBtn = null;

    /*test debug used*/
    private TextView mStateTextView;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    // Importing also other views
    private JoystickView mJoystick;

    /*show the joystickView used*/
    private TextView mAngleTextView;
    private TextView mPowerTextView;
    private TextView mDirectionTextView;

    private boolean isMoved = false;


    /*SeekBar  and the relative TextView */
    SeekBar mSpeedSeekBar , mDurationSeekBar= null;
    TextView mSpeedSeekBar_Progress , mDurationSeekBar_Progress= null;



    /*SoundPool*/
    private SoundPool soundPool = null;
    private static final float LEFT_VOLUME = 1.0F;
    private static final float RIGHT_VOLUME = 1.0F;
    private static final int STREAM_PRIORITY = 0;
    private static final int LOOP_MODE = 0;
    private static final float PLAYBACK_RATE = 1.0F;

    private static final int MAX_STREAMES = 4;
    private static final int QUALITY = 5;
    private static final int PRIORITY = 1;

    private MediaPlayer mMediaPlayer = null;


    /*Read webcam from url and display to ImageView.*/
    private ImageView displayWebcam;


    /*show the direction in imageView*/
    private ImageView dir_image;

    /*used to display the km/h of EPW speed*/
    private static TextView Kmh_display = null;

    /*URL of Server and HTTP get parameter.*/

    private final String SERVER_MAIN_URL = "http://140.116.164.46:8080/";
    private final String SERVER_TYPE_COMMAND="?action=command";
    private final String SERVER_TYPE_SNAPSHOT="?action=snapshot";
    private String server_parameter="";
    private String server_url = "";

    private boolean ready_to_send_command = false;

    /*command parameter type of _SmartEPW*/
    private static final int GROUP_EPW = 0;
    private static final int ID_EPW_MOTOR_DIRECTION = 100;
    private static final int ID_EPW_MOTOR_SPEED = 101;
    private static final int ID_EPW_LINEAR_ACTUATOR_A = 102;
    private static final int ID_EPW_LINEAR_ACTUATOR_B = 103;
    private static final int ID_PID_ALG_KP = 104;
    private static final int ID_PID_ALG_KI = 105;
    private static final int ID_PID_ALG_KD = 106;
    /*information parameter type of _SmartEPW*/
    private static final int ID_ULTRASONIC_0_ = 200;
    private static final int ID_ULTRASONIC_1 = 201;
    private static final int ID_ULTRASONIC_2 = 202;
    private static final int ID_ULTRASONIC_3 = 203;
    private static final int ID_ACTUATOR_A_LS = 204;
    private static final int ID_ACTUATOR_B_LS = 205;
    private static final int ID_MOTOR_LEFT_RPM = 206;
    private static final int ID_MOTOR_RIGHT_RPM = 207;

    /*direction control with duration time of EPW*/
    private int dir_ctrl_duration;//unit: ms

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //-------------------------------Bluetooth part----------------------------------------//

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(getActivity(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }
        //-------------------------------------------------------------------------------------//
        // Set up the custom title
        mTitle = (TextView) getActivity().findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) getActivity().findViewById(R.id.title_right_text);




        /*enable optionsmenu*/
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.control_panel, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) {
                setupChat();
                setupController();
                setupJoystick();
                setupEPW_Info();
                setupWebcam();
            }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();

            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        /*Media play */
        if(mMediaPlayer != null) mMediaPlayer.release();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }

    private void ensureDiscoverable() {
        if (D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) Log.e(TAG, "+++ ON CREATE +++");
        setupChat();
        setupController();
        setupJoystick();
        setupEPW_Info();
        setupWebcam();

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                    setupController();
                    setupJoystick();
                    setupEPW_Info();
                    setupWebcam();

                } else {
                    // User did not enable Bluetooth or an error occured

                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        //sendMessage(message);
                    }
                    if (D) Log.i(TAG, "END onEditorAction");
                    return true;
                }
            };
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            mTitle.setText(R.string.title_connected_to);
                            mTitle.append(mConnectedDeviceName);
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;

                case MESSAGE_WRITE:
                    //I could not be to know I send what char.
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;

                case MESSAGE_READ:
                    //byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":" + msg.obj);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getActivity().getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getActivity().getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void setupWebcam(){
        /*=========read the webcam from the url display to ImageView*/
        displayWebcam = (ImageView) getActivity().findViewById(R.id.webcam_read);
        String URL = "http://140.116.164.46:8080/?action=snapshot";
        displayWebcam.setTag(URL);/*set the url into the imageView.*/
        new updateImagesTask().execute(displayWebcam); //start to loop update the Image from the server's webcam.
    }

    private void setupEPW_Info(){
        /*get JSON from the EPW server*/
        new updateSmartEPW_Info_Task().execute("http://140.116.164.46:8080/output.json");

        Kmh_display = (TextView) getActivity().findViewById(R.id.kmh_display);

    }



    private void setupJoystick(){
        /*testing debug mode*/
        mStateTextView = (TextView) getActivity().findViewById(R.id.state_textView);

        mAngleTextView = (TextView) getActivity().findViewById(R.id.angleTextView);
        mPowerTextView = (TextView) getActivity().findViewById(R.id.powerTextView);
        mDirectionTextView = (TextView) getActivity().findViewById(R.id.directionTextView);

        //Referencing also other views
        mJoystick = (JoystickView) getActivity().findViewById(R.id.joystickView);
        //Event listener that always returns the variation of the angle in degrees, motion power in percentage and direction of movement
        mJoystick.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                // TODO Auto-generated method stub
                mAngleTextView.setText(" " + String.valueOf(angle) + "°");
                mPowerTextView.setText(" " + String.valueOf(power) + "%");
                switch (direction) {
                    case JoystickView.FRONT:
                        mDirectionTextView.setText(R.string.front_lab);
                        mStateTextView.setText("Forward");
                        sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'f');
                        dir_image.setImageResource(R.drawable.v1_arrow_up);
                        break;
                    case JoystickView.FRONT_RIGHT:
                        mDirectionTextView.setText(R.string.front_right_lab);
                        break;
                    case JoystickView.RIGHT:
                        mDirectionTextView.setText(R.string.right_lab);
                        mStateTextView.setText("Right");
                        sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'r');
                        dir_image.setImageResource(R.drawable.v1_arrow_right);
                        break;
                    case JoystickView.BACK_RIGHT:
                        mDirectionTextView.setText(R.string.back_right_lab);
                        break;
                    case JoystickView.BACK:
                        mDirectionTextView.setText(R.string.back_lab);
                        mStateTextView.setText("Backward");
                        sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'b');
                        dir_image.setImageResource(R.drawable.v1_arrow_down);
                        break;
                    case JoystickView.BACK_LEFT:
                        mDirectionTextView.setText(R.string.back_left_lab);
                        break;
                    case JoystickView.LEFT:
                        mDirectionTextView.setText(R.string.left_lab);
                        mStateTextView.setText("Left");
                        sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'l');
                        dir_image.setImageResource(R.drawable.v1_arrow_left);
                        break;
                    case JoystickView.FRONT_LEFT:
                        mDirectionTextView.setText(R.string.left_front_lab);
                        break;
                    default:/*center*/
                        mDirectionTextView.setText(R.string.center_lab);
                        mStateTextView.setText("Stop");
                        dir_image.setImageResource(R.drawable.stop_icon_v1);
                        sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'s');
                }
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);

        /*it's used to showing the Joystick's direction by ImageView*/
        dir_image = (ImageView) getActivity().findViewById(R.id.direction_imageView);
    }


    private void setupController() {
        Log.d(TAG, "setup Controller");

        /*create new object of the send command method in AsyncTask.*/
        new Http_sendCommand_Task().execute(SERVER_MAIN_URL);


        /**
         * SeekBar
         * Ref to https://github.com/AndroSelva/Vertical-SeekBar-Android
         **/
        //Speed of Seekbar
        mSpeedSeekBar=(SeekBar)getActivity().findViewById(R.id.ctrl_speed);
        mSpeedSeekBar_Progress=(TextView)getActivity().findViewById(R.id.ctrl_speed_value);
        mSpeedSeekBar.setMax(10);
        mSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mSpeedSeekBar_Progress.setText(String.valueOf(progress));
                sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_SPEED,mSpeedSeekBar.getProgress()*12);
            }
        });
        //Delay of Seekbar
        mDurationSeekBar=(SeekBar)getActivity().findViewById(R.id.ctrl_duration);
        mDurationSeekBar_Progress=(TextView)getActivity().findViewById(R.id.ctrl_duration_value);
        mDurationSeekBar.setMax(50);
        mDurationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mDurationSeekBar_Progress.setText(String.valueOf(FormatConvert.IntToFloatByScale(progress, 0.1f)));
                /**
                 * dir_ctrl_duration is represent millisecond,
                 * so the dir_ctrl_duration will convert to msec.
                 *
                 **/
                dir_ctrl_duration = mDurationSeekBar.getProgress()*100;
            }
        });


        /*Speed control of button*/
        mSpeedPlusBtn = (Button)getActivity().findViewById(R.id.ctrl_speed_plus);
        mSpeedPlusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSpeedSeekBar.getProgress() >= mSpeedSeekBar.getMax()) {
                    return;
                }
                else {
                    mSpeedSeekBar.setProgress(mSpeedSeekBar.getProgress() + 1);
                    mSpeedSeekBar_Progress.setText(String.valueOf(mSpeedSeekBar.getProgress()));
                    /*motor speed value of EPW divided into ten parts, the stm32f4 allowing of acceptable pwm range is 0 to 120, corresponds 0 to 2.5 volts*/
                    sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_SPEED,mSpeedSeekBar.getProgress()*12);

                }
            }
        });
        mSpeedMinusBtn = (Button)getActivity().findViewById(R.id.ctrl_speed_minus);
        mSpeedMinusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSpeedSeekBar.getProgress() <= 0) {
                    return;
                }
                else {
                    mSpeedSeekBar.setProgress(mSpeedSeekBar.getProgress() - 1);
                    mSpeedSeekBar_Progress.setText(String.valueOf(mSpeedSeekBar.getProgress()));
                    /*motor speed value of EPW divided into ten parts, the stm32f4 allowing of acceptable pwm range is 0 to 120, corresponds 0 to 2.5 volts*/
                    sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_SPEED,mSpeedSeekBar.getProgress()*12);
                }
            }
        });
        /*Delay control of button*/
        mDurationPlusBtn = (Button)getActivity().findViewById(R.id.ctrl_duration_plus);
        mDurationPlusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDurationSeekBar.getProgress() >= mDurationSeekBar.getMax()) {
                    return;
                }
                else {
                    mDurationSeekBar.setProgress(mDurationSeekBar.getProgress() + 5);
                    mDurationSeekBar_Progress.setText(String.valueOf(FormatConvert.IntToFloatByScale(mDurationSeekBar.getProgress(), 0.1f)));
                    /**
                     * dir_ctrl_duration is represent millisecond,
                     * so the dir_ctrl_duration will convert to msec.
                     *
                     **/
                    dir_ctrl_duration = mDurationSeekBar.getProgress()*100;
                }
            }
        });
        mDurationMinusBtn = (Button)getActivity().findViewById(R.id.ctrl_duration_minus);
        mDurationMinusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDurationSeekBar.getProgress() <= 0) {
                    return;
                }
                else {
                    mDurationSeekBar.setProgress(mDurationSeekBar.getProgress() - 5);
                    mDurationSeekBar_Progress.setText(String.valueOf(FormatConvert.IntToFloatByScale(mDurationSeekBar.getProgress(), 0.1f)));
                    /**
                     * dir_ctrl_duration is represent millisecond,
                     * so the dir_ctrl_duration will convert to msec.
                     *
                     **/
                    dir_ctrl_duration = mDurationSeekBar.getProgress()*100;
                }
            }
        });
    }


    /**
     *  accept user input the keydown event
     *  and calculate the joystick's circle direction
     **/
    public void myOnKeyDown(int keyCode , KeyEvent event){
        switch (keyCode) {
            case KeyEvent.KEYCODE_I:/*up*/
                mJoystick.setPostion((mJoystick.getWidth())/ 2 , 0);
                mJoystick.invalidate(); /*invalidate is implement to move the joystick's circle*/
                sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'f');
                dir_image.setImageResource(R.drawable.v1_arrow_up);
                isMoved = true;
                break;
            case KeyEvent.KEYCODE_M:/*down*/
                mJoystick.setPostion((mJoystick.getWidth())/ 2 , mJoystick.getHeight());
                mJoystick.invalidate();
                sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'b');
                dir_image.setImageResource(R.drawable.v1_arrow_down);
                isMoved = true;
                break;
            case KeyEvent.KEYCODE_T:/*left*/
                mJoystick.setPostion(0 , mJoystick.getHeight()/2);
                mJoystick.invalidate();
                sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'l');
                dir_image.setImageResource(R.drawable.v1_arrow_left);
                isMoved = true;
                break;
            case KeyEvent.KEYCODE_E:/*right && stop*/
                if(isMoved){
                    /*stop to move*/
                    mJoystick.setPostion(mJoystick.getWidth()/2 , mJoystick.getHeight()/2);
                    sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'s');
                    dir_image.setImageResource(R.drawable.stop_icon_v1);
                    isMoved = false;
                }
                else{/*right*/
                    mJoystick.setPostion(mJoystick.getWidth() , mJoystick.getHeight()/2);
                    sendCommandToEPW(GROUP_EPW,ID_EPW_MOTOR_DIRECTION,'r');
                    dir_image.setImageResource(R.drawable.v1_arrow_right);
                    isMoved = true;
                }
                mJoystick.invalidate();
                break;
            default:
                break;
        }


        /*there should be add loop function to keeping send command for specified time period*/

         /*keep in the restrict time, force return the joystick to center of position after 1 sec.*/
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                /*stop to move*/
                mJoystick.setPostion(mJoystick.getWidth() / 2, mJoystick.getHeight() / 2);

                mAngleTextView.setText(" " + String.valueOf(0) + "°");
                mPowerTextView.setText(" " + String.valueOf(0) + "%");
                mDirectionTextView.setText(R.string.center_lab);

                mJoystick.invalidate();
                isMoved = false;
            }
        }, dir_ctrl_duration);
    }

    /**
     * send command to EPW of stm32f4 MCU using by http get method.
     * for example http request such as:
     * http://140.116.164.46:8080/?action=command&dest=1&plugin=0&id=100&group=0&value=102
     * @param group, id, value.
     */
    public void sendCommandToEPW(int group,int id,int value){
        //note, dest and plugin are fixed.
        server_parameter = "&dest=1&plugin=0" + ("&group="+String.valueOf(group)) + ("&id="+String.valueOf(id)) + ("&value="+String.valueOf(value));
        Log.d("JackABK",server_parameter);
        ready_to_send_command = true;
    }
    /**
     * play all of the sound.
     * @param resId, for example: R.raw.test
     */
    public void playSound(int resId){
        mMediaPlayer = MediaPlayer.create(getActivity() , resId);
        mMediaPlayer.start();
    }



    /**
     * using http get method to parse the url, note that should be running in background thread, cannot in the UI thread,
     * so that the function must be in AsyncTask.
     * @param url
     */
    private  String HTTP_GET_PARSER(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";
                Toast.makeText(this.getActivity(), "Host not found!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private  String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    /**
     * create AsyncTask thread to read picture from the url path.
     * more details please see http://stackoverflow.com/questions/3090650/android-loading-an-image-from-the-web-with-asynctask
     */
    public class updateImagesTask extends AsyncTask<ImageView, Bitmap, Bitmap> {

        ImageView imageView = null;
        Bitmap Bitmap_temp =null;
        int frame_count=0; /*calculate the frame count.*/
        double current_time=0;
        @Override
        protected Bitmap doInBackground(ImageView... imageViews) {
            this.imageView = imageViews[0];
                try {
                    /*loop read the webcam of image from the url.*/
                    while (true){
                        Bitmap_temp = getBitmapFromURL((String)imageView.getTag());
                        publishProgress(Bitmap_temp); /*update the ImageView.*/
                        frame_count++;
                        Thread.sleep(30); /*sampling period, unit : ms */
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return Bitmap_temp; /*Last Bitmap image result when stop to looping.*/
        }

        @Override
        protected void onProgressUpdate(Bitmap... frame) {
            super.onProgressUpdate(frame);
            imageView.setImageBitmap(frame[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
        }
        /*read the picture from url, the type is Bitmap*/
        private Bitmap getBitmapFromURL(String imageUrl)
        {
            try
            {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                return bitmap;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class updateSmartEPW_Info_Task extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            JSONObject json = null;
            JSONArray controls = null;
            int i;
            while(true) {
                try {
                    json = new JSONObject(HTTP_GET_PARSER(urls[0]));

                    /* showing to Logcat.
                    String str = "";
                    JSONArray controls = json.getJSONArray("controls");
                    str += "controls length = " + json.getJSONArray("controls").length();
                    str += "\n--------\n";
                    str += "names: " + controls.getJSONObject(0).names();
                    str += "\n--------\n";
                    str += "id: " + controls.getJSONObject(17).getString("id");scanning
                    Log.d(TAG, str);
                    */

                    controls = json.getJSONArray("controls");
                    /*scanning all information of json to find out the expected index*/
                    for(i=0;i<json.getJSONArray("controls").length();i++){
                        if(Integer.valueOf(controls.getJSONObject(i).getString("id"))== ID_MOTOR_RIGHT_RPM )
                            Kmh_display.setText(controls.getJSONObject(i).getString("value"));
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            //return HTTP_GET_PARSER(urls[0]); //without return to PostExecute.
        }
        @Override
        protected void onPostExecute(String result) {
           ;
        }
    }

    /**
     * A {@link this#sendCommandToEPW} that update the url parameter and response the "ready_to_send_command" of signal.
     * This Task will loop processing the URL to Request server using http get method.
     */
    private class Http_sendCommand_Task extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            server_url = urls[0];
            while(true) {
                if(ready_to_send_command){
                    HTTP_GET_PARSER(server_url + SERVER_TYPE_COMMAND + server_parameter);
                    ready_to_send_command = false;//back to non-ready of state
                }
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            ;//don't need to parse the result string.
        }
    }

}
