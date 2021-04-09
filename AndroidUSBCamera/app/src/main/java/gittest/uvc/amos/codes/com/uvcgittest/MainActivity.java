package gittest.uvc.amos.codes.com.uvcgittest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.amos.codes.uvc.FileUtils;
import com.amos.codes.uvc.UVCCameraHelper;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent, CameraViewInterface.Callback{
    private Button btnPhoto,btnStartRec,btnStopRec,btnRotate,btnStart,btnStop,btnRelease,btnReStart,btnClearDisplay;
    private  int int_rotation=0;
    private RelativeLayout relativeVideo;

    private UVCCameraTextureView uvcCameraTextureView;
    private UVCCameraHelper mCameraHelper;
    private boolean isRequest;
    private boolean isPreview;


    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {
        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }
        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                showShortMsg(device.getDeviceName() + " is out");
            }
        }
        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params");
                isPreview = false;
            } else {
            }
        }
        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("disconnecting");
        }
    };
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };
    private static final int REQUEST_CODE = 1;
    private List<String> mMissPermissions = new ArrayList<>();

    private boolean isVersionM() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private void checkAndRequestPermissions() {
        mMissPermissions.clear();
        for (String permission : REQUIRED_PERMISSION_LIST) {
            int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                mMissPermissions.add(permission);
            }
        }
        // check permissions has granted
        if (mMissPermissions.isEmpty()) {
            //startMainActivity();
        } else {
            ActivityCompat.requestPermissions(this,
                    mMissPermissions.toArray(new String[mMissPermissions.size()]),
                    REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mMissPermissions.remove(permissions[i]);
                }
            }
        }
        // Get permissions success or not
        if (mMissPermissions.isEmpty()) {
        } else {
        }
    }
    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); //无标题
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN); //全屏
        hideBottomUIMenu();  //隐藏虚拟按键
        initButtons();  //初始化按钮事件
        if(isVersionM())
        {
            checkAndRequestPermissions();
        }
        initTextureViewSurface();  //初始化播放器控件


    }
    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {

            Window _window = getWindow();
            WindowManager.LayoutParams params = _window.getAttributes();
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE;
            _window.setAttributes(params);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("取消操作");
        }
    }

    public boolean isCameraOpened() {
        return mCameraHelper.isCameraOpened();
    }

    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {
    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
    }





    /**
     * 初始化播放器的Surface
     * 一个UVC专用的
     */
    public void initTextureViewSurface() {
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int Screen_W = outMetrics.widthPixels;
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.MATCH_PARENT);
        relativeVideo = (RelativeLayout) findViewById(R.id.surface_layout);
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        float topY = Screen_W * 1.25f;
        int ph = (int) topY;
        params.height = 768;
        params.setMargins(0, 0, 0, 0);
        params.width = 1024;
        relativeVideo.setLayoutParams(params);




        uvcCameraTextureView = new UVCCameraTextureView(this);
        uvcCameraTextureView.setLayoutParams(params);
        relativeVideo.addView(uvcCameraTextureView);
        uvcCameraTextureView.setCallback(this);
        if(mCameraHelper==null) {
            mCameraHelper = UVCCameraHelper.getInstance();
            mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
            mCameraHelper.initUSBMonitor(this, uvcCameraTextureView, listener);
            mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
                @Override
                public void onPreviewResult(byte[] nv21Yuv) {
                }
            });

        }
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }

    }

    /**
     * 按钮事件
     */
    private void initButtons()
    {
        btnClearDisplay=findViewById(R.id.clearDisplay);
        btnClearDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraHelper.stopPreview();
                relativeVideo.removeAllViews();
            }
        });
        btnRelease=findViewById(R.id.release);
        btnRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                relativeVideo.removeAllViews();
                mCameraHelper.release();
            }
        });

        btnReStart=findViewById(R.id.restart);
        btnReStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initTextureViewSurface();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //code by amos
                        mCameraHelper.startPreview(uvcCameraTextureView);
                        isPreview = true;
                    }
                }, 5000);//1秒后执行Runnable中的run方法

            }
        });


        btnPhoto=(Button)findViewById(R.id.btn_TakePhoto);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return;
                }
                String picPath = "/sdcard/iScopePro/" +"image/"+ System.currentTimeMillis()
                        + UVCCameraHelper.SUFFIX_JPEG;
                Log.e("AmosDemo","save picPath picPath：" + picPath);
                mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
                    @Override
                    public void onCaptureResult(String path) {
                        Log.e("AmosDemo","save path：" + path);
                    }
                });
            }
        });


        btnStartRec=(Button)findViewById(R.id.btn_Rec_Start);
        btnStartRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return;
                }
                if (!mCameraHelper.isPushing()) {
                    String videoPath = "/sdcard/iScopePro/" + "image/" + System.currentTimeMillis();
                    FileUtils.createfile(FileUtils.ROOT_PATH + "test666.h264");
                    RecordParams params = new RecordParams();
                    params.setRecordPath(videoPath);
                    params.setRecordDuration(0);                        // 设置为0，不分割保存
                    params.setVoiceClose(true);
                    mCameraHelper.startPusher(params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                        @Override
                        public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                            if (type == 1) {
                                FileUtils.putFileStream(data, offset, length);
                            }
                            if (type == 0) {

                            }
                        }

                        @Override
                        public void onRecordResult(String videoPath) {
                            Log.e("AmosDemo", "videoPath = " + videoPath);
                        }
                    });
                    showShortMsg("start record...");
                }
            }
        });


        btnStopRec=(Button)findViewById(R.id.btn_Rec_Stop);
        btnStopRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtils.releaseFile();
                mCameraHelper.stopPusher();
                showShortMsg("stop record...");
            }
        });


        btnRotate=(Button)findViewById(R.id.btn_Rotate);
        btnRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int_rotation+=90;
                if(int_rotation>270)
                {
                    int_rotation=0;
                }
                uvcCameraTextureView.setRotation(int_rotation);
            }
        });

        btnStart=(Button)findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraHelper.startPreview(uvcCameraTextureView);
                isPreview = true;
            }
        });

        btnStop=(Button)findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraHelper.stopPreview();
                isPreview = false;
            }
        });
    }

}