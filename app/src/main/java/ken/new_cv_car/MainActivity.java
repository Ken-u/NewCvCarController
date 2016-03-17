package ken.new_cv_car;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.Button;
import android.widget.Toast;

import com.android.rkpx2.Gpio;

public class MainActivity extends Activity implements OnTouchListener,CvCameraViewListener2 {


    private Gpio RKPX2_PIN0_PD4=null;
    private Gpio RKPX2_PIN0_PD5=null;

    private void putlow_L(){
        //init();
        this.RKPX2_PIN0_PD5.setPortValue(Gpio.TYPE_VALUE_LOW);
    }

    private void puthigh_L(){
        //init();
        this.RKPX2_PIN0_PD5.setPortValue(Gpio.TYPE_VALUE_HIGH);
    }

    private void putlow_R(){
        //init();
        this.RKPX2_PIN0_PD4.setPortValue(Gpio.TYPE_VALUE_LOW);
    }

    private void puthigh_R(){
        //init();
        this.RKPX2_PIN0_PD4.setPortValue(Gpio.TYPE_VALUE_HIGH);
    }

    private void go(){
        puthigh_L();
        puthigh_R();
    }
    private void left(){
        putlow_L();
        puthigh_R();
    }
    private void right(){
        puthigh_L();
        putlow_R();
    }
    private void stop(){
        putlow_L();
        putlow_R();
    }
    private void init(){
        RKPX2_PIN0_PD4=new Gpio(Gpio.RKPX2_PIN0_PD4);
        RKPX2_PIN0_PD5=new Gpio(Gpio.RKPX2_PIN0_PD5);
        //boolean isGpioValid=RKPX2_PIN0_PD4.gpio_request();
        this.RKPX2_PIN0_PD4.setDirectionValue(Gpio.TYPE_DIRECTION_OUT, Gpio.TYPE_VALUE_LOW);
        this.RKPX2_PIN0_PD5.setDirectionValue(Gpio.TYPE_DIRECTION_OUT, Gpio.TYPE_VALUE_LOW);
    }

    //BlueTooth
    // Debugging
    //private static final String TAG1 = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public boolean state=false;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;


    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    //private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    int directions=0;

    private static final String  TAG              = "OCVSample::Activity";

    String message_d;
    //String message_s="0";

    private boolean              mIsColorSelected = false;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private CameraBridgeViewBase mOpenCvCameraView;

    private int Center;
    //private boolean KeepRunning=true;
    private int LastX=0;
    private  double ObjectArea;

    private int Xpos;
    private int Ypos;

    Button connect;
    Button discover;

    Handler handler=new Handler(){
        public void handleMessage(Message msg){
            switch (directions) {
                case 1:go();
                    break;
                case 2:left();
                    break;
                case 3:right();
                    break;
                default:stop();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public class MyThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(50L);//
                    if((LastX<Center+25)&(LastX>Center-25)) {
                        directions=1;//OnItemCenter
                        //message_d="1";
                    }
                    if(LastX<Center-25) {
                        directions=2;//OnItemLeft
                        //message_d="2";
                    }
                    if(LastX>Center+25) {
                        directions=3;//OnItemRight
                        //message_d="3";
                    }
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);//
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connect=(Button)findViewById(R.id.connect);
        discover=(Button)findViewById(R.id.discover);

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        init();

        //if(state)
        //Car_Run();
        new Thread(new MyThread()).start();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public void onPause() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        //KeepRunning = false;
        super.onPause();
    }
    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        if(D) Log.e(TAG, "+ ON RESUME +");

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

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
        //init();
            //sendMessage0("0");
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        //mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        //mConversationView = (ListView) findViewById(R.id.in);
        //mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        //mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        //mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        //mSendButton = (Button) findViewById(R.id.button_send);
//        mSendButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                // Send a message using content of the edit text widget
//                TextView view = (TextView) findViewById(R.id.edit_text_out);
//                String message = view.getText().toString();
//                sendMessage(message);
//            }
//        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
//    private void sendMessage0(String message) {
//        // Check that we're actually connected before trying anything
//        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
//            //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Check that there's actually something to send
//        if (message.length() > 0) {
//            // Get the message bytes and tell the BluetoothChatService to write
//            byte[] send = message.getBytes();
//            mChatService.write(send);
//
//            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            /* mOutEditText.setText(mOutStringBuffer); */
//        }
//    }

    // The action listener for the EditText widget, to listen for the return key
//    private TextView.OnEditorActionListener mWriteListener =
//            new TextView.OnEditorActionListener() {
//                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
//                    // If the action is a key-up event on the return key, send the message
//                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
//                        String message = view.getText().toString();
//                        sendMessage(message);
//                    }
//                    if(D) Log.i(TAG, "END onEditorAction");
//                    return true;
//                }
//            };
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //mTitle.setText(R.string.title_connected_to);
                            //mTitle.append(mConnectedDeviceName);
                            //mConversationArrayAdapter.clear();
                            state=true;
                            Toast.makeText(MainActivity.this,"Device has connected!",Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            //mTitle.setText(R.string.title_not_connected);
                            state=false;
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    state=true;
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(device_list.EXTRA_DEVICE_ADDRESS);
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
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, device_list.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255.0D);
        mBlobColorHsv = new Scalar(255.0D);
        SPECTRUM_SIZE = new Size(200.0D, 64.0D);
        CONTOUR_COLOR = new Scalar(255.0D,0.0D,0.0D,255.0D);
        Center=(width/2);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Point[] arrayOfPoint;
        int[] arrayOfInt;
        int j;

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            if(contours.size()<=0){
                Xpos=0;
                Ypos=0;
            }
            else{
                MatOfPoint var11=new MatOfPoint((Mat)contours.get(0));
                int var12=(int)var11.total();
                Point[] var13=new Point[var12];
                if (var12>0){
                    int[] var19=new int[var12*2];
                    var11.get(0,0,var19);

                    for(int var21=0;var21<var12;++var21){
                        var13[var21]=new Point((double)var19[var21*2],(double)var19[1+var21*2]);
                    }
                    Xpos=(int)var13[0].x;
                    Ypos=(int)var13[0].y;
                    LastX=Xpos;
                }
            }
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
            ObjectArea=mDetector.getObjectArea();
            Mat var16=mRgba;
            String var17 = "X=" + this.Xpos + ", Y=" + this.Ypos + " ,A=" + this.ObjectArea;
            //change

            Point var18 = new Point((double)(80 + this.mSpectrum.cols()), 65.0D);
            Core.putText(var16, var17, var18, 1, 3.0D, new Scalar(0.0D, 0.0D, 255.0D, 255.0D), 5);
        } else {
            Mat var2 = this.mRgba;
            Point var3 = new Point(5.0D, 65.0D);
            Core.putText(var2, "Click object on screen to track", var3, 1, 3.0D, new Scalar(0.0D, 0.0D, 255.0D, 255.0D), 5);
        }

        String dir = "center" + ":"+directions;
        //sendMessage(message);
        Mat var4 = this.mRgba;
        Point var5 = new Point(5.0D, (double)(-25 + this.mRgba.rows()));
        Core.putText(var4, dir, var5, 1, 5.0D, new Scalar(0.0D, 0.0D, 255.0D, 255.0D), 5);
        Mat var6 = this.mRgba;
        Point var7 = new Point((double)this.Center, 0.0D);
        Point var8 = new Point((double)this.Center, (double)this.mRgba.rows());
        Core.line(var6, var7, var8, new Scalar(255.0D, 0.0D, 0.0D, 255.0D));

        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
