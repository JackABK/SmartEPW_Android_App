package com.example.android.SmartEPW;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.SmartEPW.R;
import com.example.android.SmartEPW.util.ChartInitialization;
import com.example.android.SmartEPW.util.FormatConvert;

import com.zerokol.views.JoystickView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;



public class Debug_Fragment extends Fragment {
    // Debugging
    private static final String TAG = "JackABK Debug";
    private static final boolean D = true;
    private static final boolean ENABLE_LINE = false;

    GraphicalView mGraphicView;
    XYMultipleSeriesDataset mDataset;
    XYMultipleSeriesRenderer mRenderer;
    private XYSeries mCurrentSeries;
    private XYSeriesRenderer mCurrentRenderer;
    public static final int  SERIES_CH1_INDEX=0;
    public static final int  SERIES_CH2_INDEX=1;
    public static final int  SERIES_CH3_INDEX=2;
    public static final int  SERIES_CH4_INDEX=3;

    public static final int MAX_X_AXIS = 100;
    public static final int MAX_Y_AXIS = 300;
    protected Update mUpdateTask;


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


    /* Website object.*/
    WebView browser =null;

    private ImageButton mWebRefresh ;
    private ImageButton mWebStop;

    /*Layout Views*/
    private TextView mTitle;
    private ListView mConversationView;
    private JoystickView mDebugJoystickView;
    private TextView mDebugAngleTextView;
    private TextView mDebugPowerTextView;
    private TextView mDebugDirectionTextView;


    /*SeekBar  and the relative TextView */
    SeekBar mPwmSeekBar , mKpSeekBar , mKiSeekBar , mKdSeekBar = null;
    TextView mPwmSeekBar_Progress , mKpSeekBar_Progress , mKiSeekBar_Progress , mKdSeekBar_Progress = null;

    /*graphic view apply button*/
    private Button mCaptureBmpFromChartBtn = null;
    private Bitmap bitmap= null;

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

    private ChartInitialization mChartInitialization = null;

    /*variable from the other device. */
    private int rpm_left_value=0, rpm_right_value=0;
    private int ultrasonic_left=0, ultrasonic_right=0;

    private boolean isMoved = false;


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
        //-------------------------------End of Bluetooth part---------------------------------//

        // Set up the custom title
        mTitle = (TextView) getActivity().findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) getActivity().findViewById(R.id.title_right_text);


        /*enable optionsmenu*/
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.debug, container, false);
    }



    /*
     *  setup the all of the object class
     *  GraphicChart , JoystickView , SeekBar , WebView.
     */
    @Override
    public void onStart() {
        super.onStart();
        /*-------------------------------Bluetooth part----------------------------------------*/
        // If BT is not on, request that it be enabled.
        // setupBTChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {  /* Otherwise, setup the chat session */
            if (mChatService == null) {
                setupBTChat();
            }
        }
        /*-------------------------------End of the Bluetooth part----------------------------*/
        /*setup used of all object*/
        setupGraphicChart();
        setupJoystickView();
        setupSeekBar();
        setupWebView();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

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
        setupBTChat();

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
                    setupBTChat();

                } else {
                    // User did not enable Bluetooth or an error occured

                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    protected final Handler mHandler = new Handler() {
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
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //I could not be to know I send what char.
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;

                case MESSAGE_READ:
                    // construct a string from the valid bytes in the buffer
                    String readStr = (String) msg.obj;
                    byte[] readBuf = new byte[7];
                    try {
                        readBuf = readStr.getBytes("ISO-8859-1");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    /*identifier the command list from stm32f4*/
                    if (readBuf[0]=='c' && readBuf[1]=='m' && readBuf[2]=='d'){


                        rpm_left_value = ((int)readBuf[3])&0x00FF;
                        rpm_right_value = ((int)readBuf[4])&0x00FF;
                        ultrasonic_left = ((int)readBuf[5])&0x00FF;
                        ultrasonic_right = ((int)readBuf[6])&0x00FF;
                        Log.d(TAG , "rpm Left is "+ rpm_left_value  + "rpm_right is "+ rpm_right_value);
                        //Log.d(TAG , "cmd is " + readStr);
                    }
                    mConversationArrayAdapter.add(/*mConnectedDeviceName + ":" +*/readStr);
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

    private void setupBTChat() {
        Log.d(TAG, "setupBTChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);
        mConversationView = (ListView) getActivity().findViewById(R.id.shell_ListView);
        mConversationView.setAdapter(mConversationArrayAdapter);
        
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }



    private void setupGraphicChart() {
        Log.d(TAG, "setup GraphicChart");
        
        String[] series_channel_title=new String[]{
                "RPM Left",
                "RPM Right",
                "Ultrasonic Left (cm)",
                "Ultrasonic Right (cm)"
        };

        /*initialize the chart object.*/
        mChartInitialization =new ChartInitialization();

        //mDataset = buildDatset(titles, x, y); // 儲存座標值
        mDataset = mChartInitialization.buildDatset(series_channel_title);

        int[] colors = new int[] { Color.BLUE, Color.RED , Color.GREEN, Color.CYAN};// 折線的顏色
        /*Note, Dataset and renderer should be not null and should have the same number of series*/
        if(colors.length != series_channel_title.length){
            if (D) Log.e(TAG,"the number of series should be same by dataset and renderer");}

        PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE, PointStyle.DIAMOND,
                PointStyle.CIRCLE, PointStyle.DIAMOND };
        mRenderer= mChartInitialization.buildRenderer(colors, styles, true);

        mChartInitialization.setChartSettings(mRenderer, "Data Chart", "Time(sec)", "Output", 0, MAX_X_AXIS, 0, MAX_Y_AXIS, Color.DKGRAY, Color.DKGRAY);// 定義折線圖

        mGraphicView = ChartFactory.getLineChartView(this.getActivity(), mDataset, mRenderer);


        if (mGraphicView != null) {
            LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.chart);

            mGraphicView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SeriesSelection seriesSelection = mGraphicView.getCurrentSeriesAndPoint();
                    double[] xy = mGraphicView.toRealPoint(0);
                    if (seriesSelection == null) {
                        Toast.makeText(getActivity(), "No chart element was clicked", Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        Toast.makeText(
                                getActivity(),
                                "Chart element in series index " + seriesSelection.getSeriesIndex()
                                        + " data point index " + seriesSelection.getPointIndex() + " was clicked"
                                        + " closest point value X=" + seriesSelection.getXValue() + ", Y=" + seriesSelection.getValue()
                                        + " clicked point value X=" + (float) xy[0] + ", Y=" + (float) xy[1], Toast.LENGTH_SHORT).show();
                    }
                }
            });


            mGraphicView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    SeriesSelection seriesSelection = mGraphicView.getCurrentSeriesAndPoint();
                    if (seriesSelection == null) {
                        Toast.makeText(getActivity(), "No chart element was long pressed",
                                Toast.LENGTH_SHORT);
                        return false; // no chart element was long pressed, so let something
                        // else handle the event
                    } else {
                        Toast.makeText(getActivity(), "Chart element in series index "
                                + seriesSelection.getSeriesIndex() + " data point index "
                                + seriesSelection.getPointIndex() + " was long pressed", Toast.LENGTH_SHORT);
                        return true; // the element was long pressed - the event has been
                        // handled
                    }
                }
            });
            mGraphicView.addZoomListener(new ZoomListener() {
                public void zoomApplied(ZoomEvent e) {
                    String type = "out";
                    if (e.isZoomIn()) {
                        type = "in";
                    }
                    System.out.println("Zoom " + type + " rate " + e.getZoomRate());
                }

                public void zoomReset() {
                    System.out.println("Reset");
                }
            }, true, true);


            /*
            //note , I no adding the UI object to mCaptureBmpFromChartBtn.
            mCaptureBmpFromChartBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    try {
                        File file = new File(Environment.getExternalStorageDirectory(), "test" + "1" + ".png");
                        FileOutputStream output = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });*/




            layout.addView(mGraphicView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.FILL_PARENT));

        } else {
            mGraphicView.repaint();
        }


        /*start to update the graphic view by thread*/
        mUpdateTask = new Update();
        mUpdateTask.execute(getActivity());


    }

    private void setupJoystickView() {
        Log.d(TAG, "setup JoystickView");
        /*=====================JoystickView Setting================================*/
        mDebugAngleTextView = (TextView) getActivity().findViewById(R.id.debug_angle);
        mDebugPowerTextView = (TextView) getActivity().findViewById(R.id.debug_power);
        mDebugDirectionTextView = (TextView) getActivity().findViewById(R.id.debug_direction);
        //Referencing also other views
        mDebugJoystickView = (JoystickView) getActivity().findViewById(R.id.debug_joystickview);
        //Event listener that always returns the variation of the angle in degrees, motion power in percentage and direction of movement
        mDebugJoystickView.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                // TODO Auto-generated method stub
                mDebugAngleTextView.setText(" " + String.valueOf(angle) + "°");
                mDebugPowerTextView.setText(" " + String.valueOf(power) + "%");
                switch (direction) {
                    case JoystickView.FRONT:
                        mDebugDirectionTextView.setText(R.string.front_lab);
                        sendCommandListToEPW((byte)'f',
                                (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                                (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                        );
                        break;
                    case JoystickView.FRONT_RIGHT:
                        mDebugDirectionTextView.setText(R.string.front_right_lab);
                        break;
                    case JoystickView.RIGHT:
                        mDebugDirectionTextView.setText(R.string.right_lab);
                        sendCommandListToEPW((byte)'r',
                                (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                                (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                        );
                        break;
                    case JoystickView.BACK_RIGHT:
                        mDebugDirectionTextView.setText(R.string.back_right_lab);
                        break;
                    case JoystickView.BACK:
                        mDebugDirectionTextView.setText(R.string.back_lab);
                        sendCommandListToEPW((byte)'b',
                                (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                                (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                        );
                        break;
                    case JoystickView.BACK_LEFT:
                        mDebugDirectionTextView.setText(R.string.back_left_lab);
                        break;
                    case JoystickView.LEFT:
                        mDebugDirectionTextView.setText(R.string.left_lab);
                        sendCommandListToEPW((byte)'l',
                                (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                                (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                        );
                        break;
                    case JoystickView.FRONT_LEFT:
                        mDebugDirectionTextView.setText(R.string.left_front_lab);
                        break;
                    default:/*center*/
                        mDebugDirectionTextView.setText(R.string.center_lab);
                        sendCommandListToEPW((byte)'s',
                                (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                                (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                                (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                        );
                }
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);
          /*=====================End of Joystickview Setting================================*/
    }

    private void setupSeekBar() {
        Log.d(TAG, "setup SeekBar");
        
        /*=========================SeekBar==============================================*/
      /*==================Ref to https://github.com/AndroSelva/Vertical-SeekBar-Android======*/
        mPwmSeekBar=(SeekBar)getActivity().findViewById(R.id.pwm_seekBar);
        mPwmSeekBar_Progress=(TextView)getActivity().findViewById(R.id.pwm_progresstext);
        mPwmSeekBar.setMax(255);
        mPwmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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
                mPwmSeekBar_Progress.setText(String.valueOf(progress));

            }
        });


        /*Kp SeekBar settings*/
        mKpSeekBar=(SeekBar)getActivity().findViewById(R.id.Kp_seekBar);
        mKpSeekBar_Progress=(TextView)getActivity().findViewById(R.id.Kp_progresstext);
        mKpSeekBar.setMax(50);
        mKpSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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
                mKpSeekBar_Progress.setText(String.valueOf(FormatConvert.IntToFloatByScale(progress , 0.1f)));

            }
        });
        /*Ki SeekBar settings*/
        mKiSeekBar=(SeekBar)getActivity().findViewById(R.id.Ki_seekBar);
        mKiSeekBar_Progress=(TextView)getActivity().findViewById(R.id.Ki_progresstext);
        mKiSeekBar.setMax(50);
        mKiSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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
                mKiSeekBar_Progress.setText(String.valueOf(FormatConvert.IntToFloatByScale(progress , 0.1f)));

            }
        });

        mKdSeekBar=(SeekBar)getActivity().findViewById(R.id.Kd_seekBar);
        mKdSeekBar_Progress=(TextView)getActivity().findViewById(R.id.Kd_progresstext);
        mKdSeekBar.setMax(50);
        mKdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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
                mKdSeekBar_Progress.setText(String.valueOf(FormatConvert.IntToFloatByScale(progress , 0.1f)));

            }
        });


    }



    private void setupWebView() {
        Log.d(TAG, "setupWebView()");

        /*website from the webcam.*/
        browser=(WebView)getActivity().findViewById(R.id.WebCam);
        browser.getSettings().setSupportZoom(true);
        browser.getSettings().setBuiltInZoomControls(true);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.setWebChromeClient(new WebChromeClient());
        browser.setWebViewClient(new WebViewClient());
        /*fit the webview screen size*/
        browser.getSettings().setLoadWithOverviewMode(true);
        browser.getSettings().setUseWideViewPort(true);

        /*I'm using the javascript side to show the mjpg-stream data.*/
        browser.loadUrl("http://140.116.164.36:8080/javascript_simple.html");

        /*best on the refresh website of method.*/
        mWebRefresh = (ImageButton)getActivity().findViewById(R.id.refresh_bt);
        mWebRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                browser.loadUrl("http://140.116.164.36:8080/javascript_simple.html");
            }
        });


        /*stop loading web.*/
        mWebStop = (ImageButton)getActivity().findViewById(R.id.web_stop_btn);
        mWebStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                browser.stopLoading();
            }
        });

    }
    
    
  
    /**
     * send the command list to stm32f4 MCU,
     * @param control_cmd, pwm_value , Kp , Ki , Kd.
     */
    public void sendCommandListToEPW(byte control_cmd , byte pwm_value , byte Kp , byte Ki , byte Kd ){
        /*command ID */
        byte[] Identifier = new byte[] {'c','m','d'};
        byte[] cmd_byteArray = new byte[]{
                Identifier[0],
                Identifier[1],
                Identifier[2],
                control_cmd,
                pwm_value,
                Kp,
                Ki,
                Kd
        };

        Log.i("JackABK Debug" ,  "the cmd_byteArray is" + cmd_byteArray.length);
        Log.i("JackABK Debug" ,  "the control_cmd is" + control_cmd);
        Log.i("JackABK Debug" ,  "the pwm_value is" + pwm_value);
        Log.i("JackABK Debug" ,  "the Kp is" + Kp);
        Log.i("JackABK Debug" ,  "the Ki is" + Ki);
        Log.i("JackABK Debug" ,  "the Kd is" + Kd);
        mChatService.write(cmd_byteArray);
    }

    /**
     *  accept user input the keydown event
     *  and calculate the joystick's circle direction
     **/
    public void myOnKeyDown(int keyCode , KeyEvent event){
        switch (keyCode) {
            case KeyEvent.KEYCODE_I:
                mDebugJoystickView.setPostion((mDebugJoystickView.getWidth())/ 2 , 0);

                mDebugAngleTextView.setText(" " + String.valueOf(0) + "°");
                mDebugPowerTextView.setText(" " + String.valueOf(100) + "%");
                mDebugDirectionTextView.setText(R.string.front_lab);

                sendCommandListToEPW((byte)'f',
                        (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                        (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                        (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                        (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                );

                mDebugJoystickView.invalidate(); /*invalidate is implement to move the joystick's circle*/
                isMoved = true;
                break;
            case KeyEvent.KEYCODE_M:
                mDebugJoystickView.setPostion((mDebugJoystickView.getWidth())/ 2 , mDebugJoystickView.getHeight());


                mDebugAngleTextView.setText(" " + String.valueOf(180) + "°");
                mDebugPowerTextView.setText(" " + String.valueOf(100) + "%");
                mDebugDirectionTextView.setText(R.string.back_lab);

                sendCommandListToEPW((byte)'b',
                        (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                        (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                        (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                        (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                );


                mDebugJoystickView.invalidate();
                isMoved = true;
                break;
            case KeyEvent.KEYCODE_T:
                mDebugJoystickView.setPostion(0 , mDebugJoystickView.getHeight()/2);


                mDebugAngleTextView.setText(" " + String.valueOf(-90) + "°");
                mDebugPowerTextView.setText(" " + String.valueOf(100) + "%");
                mDebugDirectionTextView.setText(R.string.left_lab);

                sendCommandListToEPW((byte)'l',
                        (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                        (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                        (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                        (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                );
                mDebugJoystickView.invalidate();
                isMoved = true;
                break;
            case KeyEvent.KEYCODE_E:
                if(isMoved){
                    /*stop to move*/
                    mDebugJoystickView.setPostion(mDebugJoystickView.getWidth()/2 , mDebugJoystickView.getHeight()/2);

                    mDebugAngleTextView.setText(" " + String.valueOf(0) + "°");
                    mDebugPowerTextView.setText(" " + String.valueOf(0) + "%");
                    mDebugDirectionTextView.setText(R.string.center_lab);

                    sendCommandListToEPW((byte)'s',
                            (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                            (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                            (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                            (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                    );

                    isMoved = false;

                }
                else{
                    mDebugJoystickView.setPostion(mDebugJoystickView.getWidth() , mDebugJoystickView.getHeight()/2);

                    mDebugAngleTextView.setText(" " + String.valueOf(90) + "°");
                    mDebugPowerTextView.setText(" " + String.valueOf(100) + "%");
                    mDebugDirectionTextView.setText(R.string.right_lab);

                    sendCommandListToEPW((byte)'r',
                            (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                            (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                            (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                            (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                    );

                    isMoved = true;
                }
                mDebugJoystickView.invalidate();
                break;
            default:
                break;
        }


        /*keep in the restrict time, force return the joystick to center of position.*/
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                /*stop to move*/
                mDebugJoystickView.setPostion(mDebugJoystickView.getWidth()/2 , mDebugJoystickView.getHeight()/2);

                mDebugAngleTextView.setText(" " + String.valueOf(0) + "°");
                mDebugPowerTextView.setText(" " + String.valueOf(0) + "%");
                mDebugDirectionTextView.setText(R.string.center_lab);

                sendCommandListToEPW((byte)'s',
                        (byte)(Integer.parseInt(mPwmSeekBar_Progress.getText().toString()) & 0xFF),
                        (byte)((Float.parseFloat(mKpSeekBar_Progress.getText().toString())*10.0f)),
                        (byte)((Float.parseFloat(mKiSeekBar_Progress.getText().toString())*10.0f)),
                        (byte)((Float.parseFloat(mKdSeekBar_Progress.getText().toString())*10.0f))
                );

                mDebugJoystickView.invalidate();
                isMoved = false;
            }
        }, 1000);


    }


    protected class Update extends AsyncTask<Context, Integer, String> {
        @Override
        protected String doInBackground(Context... params) {
            int i=0;
            double current_time=0;

                try {
                    while (true){
                        if(current_time <=MAX_X_AXIS){
                            mDataset.getSeriesAt(SERIES_CH1_INDEX).add(current_time , (double)rpm_left_value);
                            mDataset.getSeriesAt(SERIES_CH2_INDEX).add(current_time , (double)rpm_right_value);
                            mDataset.getSeriesAt(SERIES_CH3_INDEX).add(current_time , (double)ultrasonic_left);
                            mDataset.getSeriesAt(SERIES_CH4_INDEX).add(current_time , (double)ultrasonic_right);
                            current_time+=0.1f; /*update the current time once sampling period, unit : sec */
                        }
                        else{
                            current_time = 0.0f;
                        /*clear all series of the point.*/
                            mDataset.getSeriesAt(SERIES_CH1_INDEX).clear();
                            mDataset.getSeriesAt(SERIES_CH2_INDEX).clear();
                            mDataset.getSeriesAt(SERIES_CH3_INDEX).clear();
                            mDataset.getSeriesAt(SERIES_CH4_INDEX).clear();
                        /*re-add ponint by first of the new graphic.*/
                            mDataset.getSeriesAt(SERIES_CH1_INDEX).add(current_time , (double)rpm_left_value);
                            mDataset.getSeriesAt(SERIES_CH2_INDEX).add(current_time , (double)rpm_right_value);
                            mDataset.getSeriesAt(SERIES_CH3_INDEX).add(current_time , (double)ultrasonic_left);
                            mDataset.getSeriesAt(SERIES_CH4_INDEX).add(current_time , (double)ultrasonic_right);
                        }
                        Thread.sleep(100); /*sampling period, unit : ms */
                        publishProgress(i);
                        i++;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

             return "COMPLETE!";
        }


        // -- gets called just before thread begins
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            if (mGraphicView != null) {
                mGraphicView.repaint();
            }
        }

        // -- called if the cancel button is pressed
        @Override
        protected void onCancelled() {
            super.onCancelled();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

}

