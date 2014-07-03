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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
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

    /*Button for settings of distance and delay*/
    private Button mDistancePlusBtn, mDistanceMinusBtn = null;
    private Button mDurationPlusBtn, mDurationMinusBtn = null;

    /*Button for the actuaotr A and B.*/
    private Button mActuator_A_PlusBtn, mActuator_A_MinusBtn = null;
    private Button mActuator_B_PlusBtn, mActuator_B_MinusBtn = null;


    /*EditText for the PID par.*/
    private EditText mKp_EditText, mKi_EditText, mKd_EditText = null;
    private Button mPID_par_send=null;

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
    SeekBar mDistanceSeekBar, mDurationSeekBar = null;
    TextView mDistanceSeekBar_Progress, mDurationSeekBar_Progress = null;


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

    /*used to display the km/h of EPW Distance*/
    private static TextView Kmh_display = null;
    /*display the Actuator of limit switch states.*/
    private static RadioButton Actuator_A_LS_Upper = null, Actuator_A_LS_Lower = null, Actuator_B_LS_Upper = null, Actuator_B_LS_Lower = null;

    /*URL of Server and HTTP get parameter.*/

    private final String SERVER_MAIN_URL = "http://10.0.0.1:8080/";
    private final String SERVER_TYPE_COMMAND = "?action=command";
    private final String SERVER_TYPE_SNAPSHOT = "?action=snapshot";
    private final String SERVER_TYPE_GETJSON = "output.json";

    private String server_parameter = "";
    private String server_url = "";

    private boolean ready_to_send_command = false;

    /*command parameter type of _SmartEPW*/
    private static final int GROUP_EPW = 0;
    private static final int ID_EPW_MOTOR_DIRECTION = 100;
    private static final int ID_EPW_MOTOR_Distance = 101;
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
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*Media play */
        if (mMediaPlayer != null) mMediaPlayer.release();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //future maybe to use the optionItem menu.
        /*
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                //empty.
                return true;
        }*/
        return false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) Log.e(TAG, "+++ ON CREATE +++");
        setupController();
        setupJoystick();
        setupEPW_Info();
        setupWebcam();

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void setupWebcam() {
        /*=========read the webcam from the url display to ImageView*/
        displayWebcam = (ImageView) getActivity().findViewById(R.id.webcam_read);
        String getWebcam_URL = SERVER_MAIN_URL + SERVER_TYPE_SNAPSHOT;
        displayWebcam.setTag(getWebcam_URL);/*set the url into the imageView.*/
        new updateImagesTask().execute(displayWebcam); //start to loop update the Image from the server's webcam.
    }

    private void setupEPW_Info() {
        /*get JSON from the EPW server*/
        String getJSON_URL = SERVER_MAIN_URL + SERVER_TYPE_GETJSON;
        new updateSmartEPW_Info_Task().execute(getJSON_URL);
        Kmh_display = (TextView) getActivity().findViewById(R.id.kmh_display);
        Actuator_A_LS_Upper = (RadioButton) getActivity().findViewById(R.id.actuator_A_LS_Upper_radioButton);
        Actuator_A_LS_Lower = (RadioButton) getActivity().findViewById(R.id.actuator_A_LS_Lower_radioButton);
        Actuator_B_LS_Upper = (RadioButton) getActivity().findViewById(R.id.actuator_B_LS_Upper_radioButton);
        Actuator_B_LS_Lower = (RadioButton) getActivity().findViewById(R.id.actuator_B_LS_Lower_radioButton);
    }

    private void setupJoystick() {

        /*testing debug mode*/
        mStateTextView = (TextView) getActivity().findViewById(R.id.state_textView);

        mAngleTextView = (TextView) getActivity().findViewById(R.id.angleTextView);
        mPowerTextView = (TextView) getActivity().findViewById(R.id.powerTextView);
        mDirectionTextView = (TextView) getActivity().findViewById(R.id.directionTextView);

        /*it's used to showing the Joystick's direction by ImageView*/
        dir_image = (ImageView) getActivity().findViewById(R.id.direction_imageView);

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
                        sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 'f');
                        dir_image.setImageResource(R.drawable.v1_arrow_up);
                        break;
                    case JoystickView.FRONT_RIGHT:
                        mDirectionTextView.setText(R.string.front_right_lab);
                        break;
                    case JoystickView.RIGHT:
                        mDirectionTextView.setText(R.string.right_lab);
                        mStateTextView.setText("Right");
                        sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 'r');
                        dir_image.setImageResource(R.drawable.v1_arrow_right);
                        break;
                    case JoystickView.BACK_RIGHT:
                        mDirectionTextView.setText(R.string.back_right_lab);
                        break;
                    case JoystickView.BACK:
                        mDirectionTextView.setText(R.string.back_lab);
                        mStateTextView.setText("Backward");
                        sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 'b');
                        dir_image.setImageResource(R.drawable.v1_arrow_down);
                        break;
                    case JoystickView.BACK_LEFT:
                        mDirectionTextView.setText(R.string.back_left_lab);
                        break;
                    case JoystickView.LEFT:
                        mDirectionTextView.setText(R.string.left_lab);
                        mStateTextView.setText("Left");
                        sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 'l');
                        dir_image.setImageResource(R.drawable.v1_arrow_left);
                        break;
                    case JoystickView.FRONT_LEFT:
                        mDirectionTextView.setText(R.string.left_front_lab);
                        break;
                    default:/*center*/
                        mDirectionTextView.setText(R.string.center_lab);
                        mStateTextView.setText("Stop");
                        sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 's');
                        dir_image.setImageResource(R.drawable.stop_icon_v1);
                }
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);


    }


    private void setupController() {
        Log.d(TAG, "setup Controller");

        /*create new object of the send command method in AsyncTask.*/
        new Http_sendCommand_Task().execute(SERVER_MAIN_URL);

        /**
         * SeekBar
         * Ref to https://github.com/AndroSelva/Vertical-SeekBar-Android
         **/
        //distance of Seekbar
        mDistanceSeekBar = (SeekBar) getActivity().findViewById(R.id.ctrl_distance);
        mDistanceSeekBar_Progress = (TextView) getActivity().findViewById(R.id.ctrl_distance_value);
        mDistanceSeekBar.setMax(10);
        sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_Distance, mDistanceSeekBar.getProgress()); //send current distance value to smartEPW for start up.
        sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 's');//stop for start up.
        mDistanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                mDistanceSeekBar_Progress.setText(String.valueOf(progress));
                sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_Distance, progress);
            }
        });
        //Delay of Seekbar
        mDurationSeekBar = (SeekBar) getActivity().findViewById(R.id.ctrl_duration);
        mDurationSeekBar_Progress = (TextView) getActivity().findViewById(R.id.ctrl_duration_value);
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
                dir_ctrl_duration = progress * 100;
            }
        });


        /*distance control of button*/
        mDistancePlusBtn = (Button) getActivity().findViewById(R.id.ctrl_distance_plus);
        mDistancePlusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDistanceSeekBar.getProgress() >= mDistanceSeekBar.getMax()) {
                    return;
                } else {
                    mDistanceSeekBar.setProgress(mDistanceSeekBar.getProgress() + 1);
                }
            }
        });
        mDistanceMinusBtn = (Button) getActivity().findViewById(R.id.ctrl_distance_minus);
        mDistanceMinusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDistanceSeekBar.getProgress() <= 0) {
                    return;
                } else {
                    mDistanceSeekBar.setProgress(mDistanceSeekBar.getProgress() - 1);
                }
            }
        });
        /*Delay control of button*/
        mDurationPlusBtn = (Button) getActivity().findViewById(R.id.ctrl_duration_plus);
        mDurationPlusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDurationSeekBar.getProgress() >= mDurationSeekBar.getMax()) {
                    return;
                } else {
                    mDurationSeekBar.setProgress(mDurationSeekBar.getProgress() + 5);
                }
            }
        });
        mDurationMinusBtn = (Button) getActivity().findViewById(R.id.ctrl_duration_minus);
        mDurationMinusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDurationSeekBar.getProgress() <= 0) {
                    return;
                } else {
                    mDurationSeekBar.setProgress(mDurationSeekBar.getProgress() - 5);
                }
            }
        });


        mActuator_A_MinusBtn = (Button) getActivity().findViewById(R.id.actuA_ctrl_minus);
        mActuator_A_MinusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommandToEPW(GROUP_EPW, ID_EPW_LINEAR_ACTUATOR_A, 2);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendCommandToEPW(GROUP_EPW, ID_EPW_LINEAR_ACTUATOR_A, 0);
            }
        });
        mActuator_A_PlusBtn = (Button) getActivity().findViewById(R.id.actuA_ctrl_plus);
        mActuator_A_PlusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommandToEPW(GROUP_EPW, ID_EPW_LINEAR_ACTUATOR_A, 1);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendCommandToEPW(GROUP_EPW, ID_EPW_LINEAR_ACTUATOR_A, 0);
            }
        });
        mActuator_B_MinusBtn = (Button) getActivity().findViewById(R.id.actuB_ctrl_minus);
        mActuator_B_MinusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommandToEPW(GROUP_EPW, ID_EPW_LINEAR_ACTUATOR_B, 2);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendCommandToEPW(GROUP_EPW, ID_EPW_LINEAR_ACTUATOR_B, 0);
            }
        });
        mActuator_B_PlusBtn = (Button) getActivity().findViewById(R.id.actuB_ctrl_plus);
        mActuator_B_PlusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommandToEPW(GROUP_EPW, ID_EPW_LINEAR_ACTUATOR_B, 1);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendCommandToEPW(GROUP_EPW, ID_EPW_LINEAR_ACTUATOR_B, 0);
            }
        });

        /*for PID adjust*/
        mKp_EditText = (EditText) getActivity().findViewById(R.id.Kp_editText);
        mKi_EditText = (EditText) getActivity().findViewById(R.id.Ki_editText);
        mKd_EditText = (EditText) getActivity().findViewById(R.id.Kd_editText);

        mPID_par_send = (Button) getActivity().findViewById(R.id.PID_par_send);
        mPID_par_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommandToEPW(GROUP_EPW, ID_PID_ALG_KP, Integer.valueOf(String.valueOf(mKp_EditText.getText())));
                sendCommandToEPW(GROUP_EPW, ID_PID_ALG_KI,  Integer.valueOf(String.valueOf(mKi_EditText.getText())));
                sendCommandToEPW(GROUP_EPW, ID_PID_ALG_KD,  Integer.valueOf(String.valueOf(mKd_EditText.getText())));
            }
        });

    }


    /**
     * accept user input the keydown event
     * and calculate the joystick's circle direction
     */
    public void myOnKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_I:/*up*/
                mJoystick.setPostion((mJoystick.getWidth()) / 2, 0);
                mJoystick.invalidate(); /*invalidate is implement to move the joystick's circle*/
                sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 'f');
                dir_image.setImageResource(R.drawable.v1_arrow_up);
                isMoved = true;
                mAngleTextView.setText(" " + String.valueOf(0) + "°");
                mPowerTextView.setText(" " + String.valueOf(100) + "%");
                mDirectionTextView.setText(R.string.front_lab);
                break;
            case KeyEvent.KEYCODE_M:/*down*/
                mJoystick.setPostion((mJoystick.getWidth()) / 2, mJoystick.getHeight());
                mJoystick.invalidate();
                sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 'b');
                dir_image.setImageResource(R.drawable.v1_arrow_down);
                isMoved = true;
                mAngleTextView.setText(" " + String.valueOf(180) + "°");
                mPowerTextView.setText(" " + String.valueOf(100) + "%");
                mDirectionTextView.setText(R.string.back_lab);
                break;
            case KeyEvent.KEYCODE_T:/*left*/
                mJoystick.setPostion(0, mJoystick.getHeight() / 2);
                mJoystick.invalidate();
                sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 'l');
                dir_image.setImageResource(R.drawable.v1_arrow_left);
                isMoved = true;
                mAngleTextView.setText(" " + String.valueOf(-90) + "°");
                mPowerTextView.setText(" " + String.valueOf(100) + "%");
                mDirectionTextView.setText(R.string.left_lab);
                break;
            case KeyEvent.KEYCODE_E:/*right && stop*/
                if (isMoved) {
                    /*stop to move*/
                    mJoystick.setPostion(mJoystick.getWidth() / 2, mJoystick.getHeight() / 2);
                    sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 's');
                    dir_image.setImageResource(R.drawable.stop_icon_v1);
                    isMoved = false;
                    mAngleTextView.setText(" " + String.valueOf(0) + "°");
                    mPowerTextView.setText(" " + String.valueOf(0) + "%");
                    mDirectionTextView.setText(R.string.center_lab);
                } else {/*right*/
                    mJoystick.setPostion(mJoystick.getWidth(), mJoystick.getHeight() / 2);
                    sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 'r');
                    dir_image.setImageResource(R.drawable.v1_arrow_right);
                    isMoved = true;
                    mAngleTextView.setText(" " + String.valueOf(90) + "°");
                    mPowerTextView.setText(" " + String.valueOf(100) + "%");
                    mDirectionTextView.setText(R.string.right_lab);
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
                sendCommandToEPW(GROUP_EPW, ID_EPW_MOTOR_DIRECTION, 's');
                dir_image.setImageResource(R.drawable.stop_icon_v1);
                isMoved = false;

                mAngleTextView.setText(" " + String.valueOf(0) + "°");
                mPowerTextView.setText(" " + String.valueOf(0) + "%");
                mDirectionTextView.setText(R.string.center_lab);
                mJoystick.invalidate();

            }
        }, dir_ctrl_duration);
    }

    /**
     * send command to EPW of stm32f4 MCU using by http get method.
     * for example http request such as:
     * http://10.0.0.1:8080/?action=command&dest=1&plugin=0&id=100&group=0&value=102
     *
     * @param group, id, value.
     */
    public void sendCommandToEPW(int group, int id, int value) {
        //note, dest and plugin are fixed.
        server_parameter = "&dest=1&plugin=0" + ("&group=" + String.valueOf(group)) + ("&id=" + String.valueOf(id)) + ("&value=" + String.valueOf(value));
        Log.d("JackABK", server_parameter);
        ready_to_send_command = true;
    }

    /**
     * play all of the sound.
     *
     * @param resId, for example: R.raw.test
     */
    public void playSound(int resId) {
        mMediaPlayer = MediaPlayer.create(getActivity(), resId);
        mMediaPlayer.start();
    }


    /**
     * using http get method to parse the url, note that should be running in background thread, cannot in the UI thread,
     * so that the function must be in AsyncTask.
     *
     * @param url
     */
    private String HTTP_GET_PARSER(String url) {
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
            if (inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";
            Toast.makeText(this.getActivity(), "Host not found!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
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
        Bitmap Bitmap_temp = null;
        int frame_count = 0; /*calculate the frame count.*/
        double current_time = 0;

        @Override
        protected Bitmap doInBackground(ImageView... imageViews) {
            this.imageView = imageViews[0];
            try {
                    /*loop read the webcam of image from the url.*/
                while (true) {
                    Bitmap_temp = getBitmapFromURL((String) imageView.getTag());
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
        private Bitmap getBitmapFromURL(String imageUrl) {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class updateSmartEPW_Info_Task extends AsyncTask<String, Integer, String> {
        JSONObject json = null;
        JSONArray controls = null; //the json group name.
        double kmh_temp;
        @Override
        protected String doInBackground(String... urls) {
            int i;
            while (true) {
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
                    for (i = 0; i < json.getJSONArray("controls").length(); i++) {
                        publishProgress(i); /*update the info of index.*/
                    }
                    Thread.sleep(50);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //return HTTP_GET_PARSER(urls[0]); //without return to PostExecute.
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            try {
                switch (Integer.valueOf(controls.getJSONObject(values[0]).getString("id"))) {
                    case ID_ACTUATOR_A_LS:
                        update_actuator_A_LS(controls.getJSONObject(values[0]).getString("value"));
                        break;
                    case ID_ACTUATOR_B_LS:
                        update_actuator_B_LS(controls.getJSONObject(values[0]).getString("value"));
                        break;
                    case ID_MOTOR_RIGHT_RPM:
                        update_kmh(controls.getJSONObject(values[0]).getString("value"));
                        break;
                    default:
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            ;
        }


        private void update_kmh(String kmh_value) {

            //0.11 = 60*22*2.54*3.14159 / 100 / 1000
            kmh_temp = Integer.valueOf(kmh_value) *0.11;

            Kmh_display.setText(String.valueOf( kmh_temp));
            Log.d(TAG, kmh_value);
        }

        private void update_actuator_A_LS(String actuator_A_LS_value) {
            switch (Integer.valueOf(actuator_A_LS_value)) {
                //normal state.
                case 0:
                    Actuator_A_LS_Upper.setChecked(false);
                    Actuator_A_LS_Lower.setChecked(false);
                    break;
                //upper limited.
                case 1:
                    Actuator_A_LS_Upper.setChecked(true);
                    Actuator_A_LS_Lower.setChecked(false);
                    break;
                //lower limited.
                case 2:
                    Actuator_A_LS_Upper.setChecked(false);
                    Actuator_A_LS_Lower.setChecked(true);
                    break;
                /*impossibility*/
                case 3:
                    break;
                default:
                    break;
            }
        }

        private void update_actuator_B_LS(String actuator_B_LS_value) {
            switch (Integer.valueOf(actuator_B_LS_value)) {
                //normal state.
                case 0:
                    Actuator_B_LS_Upper.setChecked(false);
                    Actuator_B_LS_Lower.setChecked(false);
                    break;
                //upper limited.
                case 1:
                    Actuator_B_LS_Upper.setChecked(true);
                    Actuator_B_LS_Lower.setChecked(false);
                    break;
                //lower limited.
                case 2:
                    Actuator_B_LS_Upper.setChecked(false);
                    Actuator_B_LS_Lower.setChecked(true);
                    break;
                /*impossibility*/
                case 3:
                    break;
                default:
                    break;
            }
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
                while (true) {
                    if (ready_to_send_command) {
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

