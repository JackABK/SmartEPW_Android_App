package com.example.android.SmartEPW;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
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

import com.example.android.SmartEPW.R;
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
                        sendMessage(message);
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

        /*=========read the webcam from the url display to ImageView*/
        displayWebcam = (ImageView) getActivity().findViewById(R.id.webcam_read);
        String URL = "http://140.116.164.36:8080/?action=snapshot";
        displayWebcam.setTag(URL);/*set the url into the imageView.*/
        new UpdateImagesTask().execute(displayWebcam); //start to loop update the ImageView from the url.



        /*image of the EPW direction */
        dir_image = (ImageView) getActivity().findViewById(R.id.direction_imageView);


        /*get JSON from the URL*/
        new HttpAsyncTask().execute("http://140.116.164.36:8080/output.json");


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
                        sendCommandListToEPW((byte)'f' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                        dir_image.setImageResource(R.drawable.v1_arrow_up);
                        break;
                    case JoystickView.FRONT_RIGHT:
                        mDirectionTextView.setText(R.string.front_right_lab);
                        break;
                    case JoystickView.RIGHT:
                        mDirectionTextView.setText(R.string.right_lab);
                        mStateTextView.setText("Right");
                        sendCommandListToEPW((byte)'r' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                        dir_image.setImageResource(R.drawable.v1_arrow_right);
                        break;
                    case JoystickView.BACK_RIGHT:
                        mDirectionTextView.setText(R.string.back_right_lab);
                        break;
                    case JoystickView.BACK:
                        mDirectionTextView.setText(R.string.back_lab);
                        mStateTextView.setText("Backward");
                        sendCommandListToEPW((byte)'b' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                        dir_image.setImageResource(R.drawable.v1_arrow_down);
                        break;
                    case JoystickView.BACK_LEFT:
                        mDirectionTextView.setText(R.string.back_left_lab);
                        break;
                    case JoystickView.LEFT:
                        mDirectionTextView.setText(R.string.left_lab);
                        mStateTextView.setText("Left");
                        sendCommandListToEPW((byte)'l' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                        dir_image.setImageResource(R.drawable.v1_arrow_left);
                        break;
                    case JoystickView.FRONT_LEFT:
                        mDirectionTextView.setText(R.string.left_front_lab);
                        break;
                    default:/*center*/
                        mDirectionTextView.setText(R.string.center_lab);
                        mStateTextView.setText("Stop");
                        sendCommandListToEPW((byte)'s' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                }
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);




      /*=====================SeekBar Connector=================================================*/
      /*==================Ref to http://androidbiancheng.blogspot.tw/2010/02/seekbar.html======*/
        seekBarValue = (TextView) getActivity().findViewById(R.id.seekbarvalue);
        seekBar = (SeekBar) getActivity().findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                seekBarValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });


        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void
    sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = new byte[0];
            try {
                send = message.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            mStateTextView.setText(String.valueOf(send.length));
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);

        }
    }

    /**
     *  accept user input the keydown event
     *  and calculate the joystick's circle direction
     **/
    public void myOnKeyDown(int keyCode , KeyEvent event){
        switch (keyCode) {
            case KeyEvent.KEYCODE_I:
                mJoystick.setPostion((mJoystick.getWidth())/ 2 , 0);
                mJoystick.invalidate(); /*invalidate is implement to move the joystick's circle*/
                sendCommandListToEPW((byte)'f' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                isMoved = true;
                break;
            case KeyEvent.KEYCODE_M:
                mJoystick.setPostion((mJoystick.getWidth())/ 2 , mJoystick.getHeight());
                mJoystick.invalidate();
                sendCommandListToEPW((byte)'b' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                isMoved = true;
                break;
            case KeyEvent.KEYCODE_T:
                mJoystick.setPostion(0 , mJoystick.getHeight()/2);
                mJoystick.invalidate();
                sendCommandListToEPW((byte)'l' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                isMoved = true;
                break;
            case KeyEvent.KEYCODE_E:
                if(isMoved){
                    /*stop to move*/
                    mJoystick.setPostion(mJoystick.getWidth()/2 , mJoystick.getHeight()/2);
                    sendCommandListToEPW((byte)'s' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                    isMoved = false;
                }
                else{
                    mJoystick.setPostion(mJoystick.getWidth() , mJoystick.getHeight()/2);
                    sendCommandListToEPW((byte)'r' ,(byte)(Integer.parseInt(seekBarValue.getText().toString()) & 0xFF) );
                    isMoved = true;
                }
                mJoystick.invalidate();
                break;
            default:
                break;
        }
    }

    /**
     * send the command list to stm32f4 MCU,
     * @param control_cmd, pwm_value.
     */
    public void sendCommandListToEPW(byte control_cmd , byte pwm_value ){
        byte[] command_array;
        command_array = new byte[]{control_cmd, pwm_value};
        mChatService.write(command_array);
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
     * create AsyncTask thread to read picture from the url path.
     * more details please see http://stackoverflow.com/questions/3090650/android-loading-an-image-from-the-web-with-asynctask
     */
    public class UpdateImagesTask extends AsyncTask<ImageView, Bitmap, Bitmap> {

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
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);

                String str = "";

                JSONArray controls = json.getJSONArray("controls");
                str += "controls length = "+json.getJSONArray("controls").length();
                str += "\n--------\n";
                str += "names: "+controls.getJSONObject(0).names();
                str += "\n--------\n";
                str += "id: "+controls.getJSONObject(17).getString("id");
                Log.d(TAG, str);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private  String GET(String url){
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
    }

}
