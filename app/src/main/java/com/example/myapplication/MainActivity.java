package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgcodecs.Imgcodecs.imread;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity" ;
    ImageView imgView;
    Button loadBtn;
    Button processBtn;
    Bitmap bitmap;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    private BaseLoaderCallback _baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
            }
        }
    };

    //to check whether opencv is loaded successfully or not.
    static {
        if (OpenCVLoader.initDebug()){
            Log.d(TAG,"OpenCV loaded successfully");
        }else {
            Log.d(TAG,"OpenCV not loaded");

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);

        loadBtn = (Button) findViewById(R.id.loadImgBtn);
        processBtn = (Button) findViewById(R.id.processImgBtn);
        imgView = (ImageView) findViewById(R.id.img);

        loadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 1);
            }
        });

        processBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processImage();
            }
        });_baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                try {
                    // 선택한 이미지에서 비트맵 생성
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    bitmap = BitmapFactory.decodeStream(in);
                    in.close();
                    // 이미지 표시
                    imgView.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processImage(){

        //reading input image from internal storage.

        Mat img = imread(Environment.getExternalStorageDirectory().getAbsolutePath() +"/colorballs.png");
        /*
        Mat img = new Mat();
        Utils.bitmapToMat(bitmap, img);*/
        Mat oImg = detectColor(img);

        // converting image from Mat to bitmap to display in ImageView:
        Bitmap bm = Bitmap.createBitmap(oImg.cols(), oImg.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(oImg,bm);
        imgView.setImageBitmap(bm);
    }


    Mat detectColor(Mat srcImg) {
        Mat blurImg = new Mat();
        Mat hsvImage = new Mat();
        //Mat color_range_red = new Mat();
        //Mat color_range_green = new Mat();
        Mat color_range_A = new Mat();
        Mat color_range_B = new Mat();
        Mat color_range_C = new Mat();
        Mat color_range_D = new Mat();
        Mat color_range_E = new Mat();
        Mat color_range_F = new Mat();

        Mat color_range = new Mat();

        //bluring image to filter noises
        Imgproc.GaussianBlur(srcImg, blurImg, new Size(5,5),0);

        //converting blured image from BGR to HSV
        Imgproc.cvtColor(blurImg, hsvImage, Imgproc.COLOR_BGR2HSV);

        //filtering red and green pixels based on given opencv HSV color range
        //Core.inRange(hsvImage, new Scalar(0,50,50), new Scalar(5,255,255), color_range_red);
        //Core.inRange(hsvImage, new Scalar(40,50,50), new Scalar(50,255,255), color_range_green);
        Core.inRange(hsvImage, new Scalar(80,80,80), new Scalar(90,180,180), color_range_A);
        Core.inRange(hsvImage, new Scalar(70,80,80), new Scalar(80,180,180), color_range_B);
        Core.inRange(hsvImage, new Scalar(10,80,80), new Scalar(20,180,180), color_range_C);
        Core.inRange(hsvImage, new Scalar(10,80,180), new Scalar(20,180,230), color_range_D);
        Core.inRange(hsvImage, new Scalar(0,80,80), new Scalar(10,180,180), color_range_E);
        Core.inRange(hsvImage, new Scalar(0,80,180), new Scalar(10,180,230), color_range_F);

        //applying bitwise or to detect both red and green color.
        Core.bitwise_or(color_range_A,color_range_B,color_range);
        Core.bitwise_or(color_range, color_range_C, color_range);
        Core.bitwise_or(color_range, color_range_D, color_range);
        Core.bitwise_or(color_range, color_range_E, color_range);
        Core.bitwise_or(color_range, color_range_F, color_range);

        return color_range;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            //try to load again
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, _baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            _baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

}