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

import android.widget.Toast;


public class MainActivity extends Activity implements OnTouchListener,CvCameraViewListener2 {


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

    String directions;
    String directionsUD;

    String message_LR;
    String message_UD;

    private boolean              mIsColorSelected = false;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private CameraBridgeViewBase mOpenCvCameraView;

    private int CenterLR;
    private int CenterUD;
    private boolean KeepRunning=true;
    private int LastX=0;
    private int LastY=0;
    private  double ObjectArea;

    private int Xpos;
    private int Ypos;

    private static boolean ISFront=false;
    //private Button btncon;
   // private Button btndis;

    Handler handler=new Handler(){
        public void handleMessage(Message msg){
            sendMessage0(message_LR+message_UD);

            //sendMessage0(message_UD);
            super.handleMessage(msg);
        }
    };

    public class MyThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(35);
                    // Thread paused 35ms
                    if (ObjectArea<=4000){ /**Distance/Area <=4000**/
                        if((LastX<=CenterLR+35)&&(LastX>=CenterLR-35)) {
                            directions="Center";//OnItemCenter
                            message_LR="6";
                        }
                        else{
                            if(LastX<CenterLR-35) {
                                directions="Left";//OnItemLeft
                                message_LR="2";
                            }
                            if(LastX>CenterLR+35) {
                                directions="Right";//OnItemRight
                                message_LR="4";
                            }
                        }

                        if ((LastY<=CenterUD+35)&&(LastY>=CenterUD-35)){
                            directionsUD="Center";
                            message_UD="0";
                        }
                        else{
                            if (LastY<CenterUD-35){
                                directionsUD="Up";
                                message_UD="9";
                            }
                            if (LastY>CenterUD+35){
                                directionsUD="Down";
                                message_UD="6";
                            }
                        }

                    }
                    else if (ObjectArea>=8000) {
                        message_LR="9";
                        directions="Back";
                    }
                    else{
                        message_LR="0";
                        message_UD="0";
                        directions="Stop";
                        directionsUD="Stop";
                    }
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);// Send message
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
    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState!=null){
             ISFront=savedInstanceState.getBoolean("ISFront",ISFront);
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        if(ISFront){
            mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);

        }

        mOpenCvCameraView.setCvCameraViewListener(this);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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
            if (mChatService == null) setupChat();
        }
    }
    @Override
    public void onPause() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        KeepRunning = false;
        super.onPause();
    }
    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
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
        sendMessage0("0");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("ISFront", ISFront);
    }

    private void setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void ensureChange(){
        ISFront=!ISFront;
        return;
    }

    public void restart(){
        Intent intent = getIntent();

        overridePendingTransition(0, 0);

        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        finish();

        overridePendingTransition(0, 0);

        startActivity(intent);
    }

    private void sendMessage0(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            /* mOutEditText.setText(mOutStringBuffer); */
        }
    }
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            state=true;
                            Toast.makeText(MainActivity.this,"Device has connected!",Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            state=false;
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
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
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255.0D);
        mBlobColorHsv = new Scalar(255.0D);
        SPECTRUM_SIZE = new Size(200.0D, 64.0D);
        CONTOUR_COLOR = new Scalar(255.0D,0.0D,0.0D,255.0D);
        CenterUD=(height/2);
        CenterLR=(width/2);
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

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();
        new Thread(new MyThread()).start();
        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            if(contours.size()<=0){
                Xpos=0;
                Ypos=0;
            }
            else{
                MatOfPoint mat_contours=new MatOfPoint((Mat)contours.get(0));
                int temp=(int)mat_contours.total();
                System.out.println(temp);
                Point[] pos_temp=new Point[temp];
                if (temp>0){
                    int[] point_contours=new int[temp*2];
                    mat_contours.get(0, 0, point_contours);

                    for(int i=0;i<temp;++i){
                        pos_temp[i]=new Point((double)point_contours[i*2],(double)point_contours[1+i*2]);
                    }
                    Xpos=(int)pos_temp[0].x;
                    Ypos=(int)pos_temp[0].y;
                    LastX=Xpos;
                    LastY=Ypos;
                }
            }
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
            ObjectArea=mDetector.getObjectArea();
            Mat color0 =mRgba;
            String Coordinate = "X=" + this.Xpos + ", Y=" + this.Ypos + " ,Area=" + this.ObjectArea;
            //change

            Point text_PosXY0 = new Point((double)(80 + this.mSpectrum.cols()), 65.0D);
            Core.putText(color0, Coordinate,text_PosXY0, 1, 1.5D, new Scalar(0.0D, 0.0D, 255.0D, 255.0D), 2);
        } else {
            Mat color1 = this.mRgba;
            Point text_PosXY1 = new Point(5.0D, 65.0D);
            Core.putText(color1, "Click object on screen to select!", text_PosXY1, 1, 2.0D, new Scalar(0.0D, 0.0D, 255.0D, 255.0D), 2);
        }

        String dir = "Position" + ":"+directions+","+directionsUD;
        Mat Capturer0 = this.mRgba;
        Point text_Pos = new Point(5.0D, (double)(-25 + this.mRgba.rows()));
        Core.putText(Capturer0, dir, text_Pos, 2,1.5D, new Scalar(0.0D, 0.0D, 255.0D, 255.0D), 2);
        Mat Capturer1 = this.mRgba;
        Point pointCenter_Up = new Point((double)this.CenterLR, 0.0D);
        Point pointCenter_Left=new Point(0.0D,(double)this.CenterUD);
        Point pointCenter_Down = new Point((double)this.CenterLR, (double)this.mRgba.rows());
        Point pointCenter_Right=new Point((double)this.mRgba.cols(),(double)this.CenterUD);
        Core.line(Capturer1, pointCenter_Up, pointCenter_Down, new Scalar(255.0D, 0.0D, 0.0D, 255.0D));
        Core.line(Capturer1,pointCenter_Left,pointCenter_Right,new Scalar(255.0D,0.0D,255.0D,255.0D));
        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    //Menu
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
            case R.id.front:
                ensureChange();
                restart();
                return true;
            case R.id.about:
                Intent aboutIntent=new Intent(this,AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }
        return false;
    }

}
