package com.app.flexivendsymbol.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.app.flexivendsymbol.R;
import com.app.flexivendsymbol.helpers.Helper;
import com.app.flexivendsymbol.helpers.Recognizer;
import com.app.flexivendsymbol.helpers.UIUtils;
import com.app.flexivendsymbol.services.RecognizerService;
import com.er.ERusbsdk.UsbController;

import java.io.ByteArrayOutputStream;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final int REQUEST_PERMISSION_CAMERA = 0x01;

    private TextView tvResult;
    private Button btnConnect;

    private boolean cameraConfigured = false;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private ResponseReceiver responseReceiver;

    private UsbController usbController;
    private UsbDevice usbDevice;
    private Handler usbHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case UsbController.USB_CONNECTED:
                    btnConnect.setEnabled(false);
                    btnConnect.setText(R.string.UsbConnected);
                    startPreview();
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Map view elements to class members.
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        tvResult = (TextView) findViewById(R.id.tvResult);
        btnConnect = (Button) findViewById(R.id.btnConnect);

        // Wrap event handlers to view elements.
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usbController.close();
                usbDevice = usbController.getUsbDev();

                if (usbDevice != null) {
                    if (!(usbController.isHasPermission(usbDevice))) {
                        usbController.getPermission(usbDevice);
                    } else {
                        btnConnect.setEnabled(false);
                        btnConnect.setText(R.string.UsbConnected);
                        UIUtils.showToast(getApplicationContext(), R.string.msg_AccessSuccess);
                    }
                }
            }
        });

        if (!checkCameraHardware(this)) {
            Toast.makeText(this, "Camera is not available!", Toast.LENGTH_LONG).show();
            finish();
        } else if (!hasGrantedCameraPermission()) {
            requestCameraPermission();
        } else {
            surfaceHolder = surfaceView.getHolder();

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            surfaceHolder.addCallback(this);

            // deprecated setting, but required on Android versions prior to 3.0
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            // Setup usb controller.
            usbController = new UsbController(this, usbHandler);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        camera = Helper.getCameraInstance();
        startPreview();

        IntentFilter intentFilter = new IntentFilter(ResponseReceiver.ACTION_PROCESSED);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        responseReceiver = new ResponseReceiver();
        registerReceiver(responseReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(responseReceiver);
        releaseCamera();

        super.onPause();
    }

    private boolean hasGrantedCameraPermission() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA);
        if (shouldShowRationale) {
            tvResult.setText(R.string.Grant_necessary_permissions);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_PERMISSION_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                tvResult.setText(R.string.Permissions_not_granted);
            }
        }
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private void initPreview(int width, int height) {
        if (camera != null && surfaceHolder.getSurface() != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
            } catch (Throwable t) {
                UIUtils.showToast(this, t.getMessage());
            }

            camera.setPreviewCallback(this);

            if (!cameraConfigured) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = Helper.getBestPreviewSize(width, height, parameters);
                Camera.Size pictureSize = Helper.getSmallestPictureSize(parameters);

                Display display = getWindowManager().getDefaultDisplay();
                if (display.getRotation() == Surface.ROTATION_0) {
                    camera.setDisplayOrientation(90);
                } else if (display.getRotation() == Surface.ROTATION_270) {
                    camera.setDisplayOrientation(180);
                }

                if (size != null && pictureSize != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    parameters.setPictureSize(pictureSize.width, pictureSize.height);
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    camera.setParameters(parameters);

                    cameraConfigured = true;
                }
            }
        }
    }

    private void startPreview() {
        if (cameraConfigured && camera != null) {
            camera.startPreview();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        initPreview(width, height);
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera();
    }

    private Recognizer recognizer = new Recognizer();

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        int width = params.getPreviewSize().width;
        int height = params.getPreviewSize().height;
        YuvImage yuv = new YuvImage(data, params.getPreviewFormat(), width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);

        byte[] bytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        short[][] pixelLine = Helper.getPixelStripRotated(bitmap);
        long num = recognizer.Recognize(pixelLine, false);
        if (num != -1) {
            tvResult.setText(String.valueOf(num));
            checkAndSubmit();
        }
    }

    private void checkAndSubmit() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);

        String code = tvResult.getText().toString();
        int cupSize = Integer.parseInt(code.substring(code.length() - 1));
        if (cupSize < 1 || cupSize > 4) {
            Toast.makeText(this, "Invalid Number. Scan again!", Toast.LENGTH_SHORT).show();
            tvResult.setText(null);
            return;
        }

        // sendTextToPrinter(code);
        try {
            sendTextToPrinter(getString(R.string.PRINT_STRING));
        } catch (Exception e) {
            UIUtils.showToast(this, "Printer error!");
        }

        Intent intent = new Intent(this, ChooseCupActivity.class);
        intent.putExtra(ChooseCupActivity.KEY_SYMBOL_CODE, tvResult.getText().toString());
        startActivity(intent);
        finish();
    }

    //check access permission?
    private boolean checkUsbPermission() {
        if (usbDevice != null) {
            if (usbController.isHasPermission(usbDevice)) {
                return true;
            }
        }
        btnConnect.setEnabled(true);
        btnConnect.setText(R.string.Connect);
        UIUtils.showToast(getApplicationContext(), R.string.msg_AccessFail);
        return false;
    }

    private void sendTextToPrinter(String code) {
        byte printStatus = usbController.revByte(usbDevice);
        if (printStatus == 0x38) {
            UIUtils.showToast(getApplicationContext(), R.string.paper_stat);
            return;
        }
        if (checkUsbPermission()) {
            byte[] cmd_resume = new byte[4];
            cmd_resume[0] = 0x1B;
            cmd_resume[1] = 0x40; // reset command
            usbController.sendByte(cmd_resume, usbDevice);
            usbController.sendByte(new byte[]{0x1D, 0x21, 0x22}, usbDevice);
            usbController.sendMsg(code, "GBK", usbDevice);
        }
    }

    public class ResponseReceiver extends BroadcastReceiver {

        public static final String ACTION_PROCESSED = "ACTION_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra(RecognizerService.KEY_RESULT);
            tvResult.setText(result);
            checkAndSubmit();
        }
    }

}
