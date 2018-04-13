package com.example.luan.hidrscan.views;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.luan.hidrscan.BuildConfig;
import com.example.luan.hidrscan.R;
import com.example.luan.hidrscan.utils.ImageUtil;
import com.example.luan.hidrscan.utils.PermissionUtil;
import com.example.luan.hidrscan.utils.ImageProcess;
import com.example.luan.hidrscan.utils.TensorFlowDigitClassifier;

import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.TextView;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.example.luan.hidrscan.utils.ImageProcessUtil.BitmapToNormalizedFloatArray;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static final int REQUEST_TAKE_PHOTO = 2;
    private final ViewHolder mViewHolder = new ViewHolder();
    private final ImageProcess mImageProcess = new ImageProcess(this);

    private static final int INPUT_SIZE_Y = 68;
    private static final int INPUT_SIZE_X = 32;
    private static final String INPUT_NAME = "conv2d_4_input_1";
    private static final String OUTPUT_NAME = "output_node0";

    private static final String MODEL_FILE =
            "file:///android_asset/tf_model_3conv_300relu_trained268000sampledigits.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/labels.txt";
    int mCurrentDigitImgIdx = 0;

    private TensorFlowDigitClassifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.loadLibrary("opencv_java3");

        this.buildViewHolder();
        this.setListeners();

        initTensorFlowAndLoadModel();
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowDigitClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE_Y,
                            INPUT_SIZE_X,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    /**
     * Eventos
     */

    private void setListeners() {
        mViewHolder.mBtnNewScan.setOnClickListener(this);
        mViewHolder.mBtnLoadTest.setOnClickListener(this);
        mViewHolder.mBtnNextStep.setOnClickListener(this);
        mViewHolder.mBtnTestDigit.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_scan_new:
                if (!PermissionUtil.hasCameraPermission(this)) {
                    PermissionUtil.asksCameraPermission(this);
                } else {
                    dispatchTakePictureIntent();
                }
                break;

            case R.id.btn_load_test:
                startNewImageProcessingFromTestPhoto();
                break;

            case R.id.btn_next_step:
                this.processNextStepAndShowResult();
                break;

            case R.id.btn_test_digit:
                startNewClassificationFromTestDigit();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.without_permission_camera_explanation))
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
//            this.setImageProcessingFromPhotoTaken();
            this.startNewImageProcessingFromPhotoTaken();
        }
    }

    /**
     * Intents
     */

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Certifica que a Activity da camera existe e consegue responder
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Cria o arquivo onde a foto será salva
            File photoFile = null;
            try {
                photoFile = ImageUtil.createImageFile(this);
                // Save a file: path for use with ACTION_VIEW intents
                this.mViewHolder.mUriPhotoPath = Uri.fromFile(photoFile);
            } catch (IOException ex) {
            }

            // Continua somente se teve sucesso na criação do arquivo
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    /**
     * View Holder
     */

    private static class ViewHolder {
        ImageView mImageProcessing;
        TextView mTextClassificationResult;
        Button mBtnNewScan;
        Button mBtnLoadTest;
        Button mBtnNextStep;
        Button mBtnTestDigit;
        Uri mUriPhotoPath;
    }

    private void buildViewHolder() {
        mViewHolder.mImageProcessing = (ImageView) this.findViewById(R.id.image_processing);
        mViewHolder.mTextClassificationResult = (TextView) this.findViewById(R.id.text_classification_result);
        mViewHolder.mBtnNewScan = (Button) this.findViewById(R.id.btn_scan_new);
        mViewHolder.mBtnLoadTest = (Button) this.findViewById(R.id.btn_load_test);
        mViewHolder.mBtnNextStep = (Button) this.findViewById(R.id.btn_next_step);
        mViewHolder.mBtnTestDigit = (Button) this.findViewById(R.id.btn_test_digit);
    }

    /**
     * Modificações na interface
     */

    private void setImageProcessingFromPhotoTaken() {

        // Obtém as dimensões da View onde a imagem será colocada
        int targetW = this.mViewHolder.mImageProcessing.getWidth();
        int targetH = this.mViewHolder.mImageProcessing.getHeight();

        // Obtém as dimensões do bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(this.mViewHolder.mUriPhotoPath.getPath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determina o quanto dimensionar a imagem
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decodifica a imagem em um arquivo de imagem para preencher a View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(this.mViewHolder.mUriPhotoPath.getPath(), bmOptions);
        Bitmap bitmapRotated = ImageUtil.rotateImageIfRequired(bitmap, this.mViewHolder.mUriPhotoPath);

//        Mat src = new Mat();
//        Mat dst = new Mat();
//        Utils.bitmapToMat(bitmapRotated, src);
//        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2GRAY);
//        //Bitmap bitmapGray = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(dst, bitmapRotated);
//        //bitmapRotated.recycle();

        this.mViewHolder.mImageProcessing.setImageBitmap(bitmapRotated);
    }

    private void startNewImageProcessingFromPhotoTaken() {

//        // Obtém as dimensões da View onde a imagem será colocada
//        int targetW = this.mViewHolder.mImageProcessing.getWidth();
//        int targetH = this.mViewHolder.mImageProcessing.getHeight();
//
//        // Obtém as dimensões do bitmap
//        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//        bmOptions.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(this.mViewHolder.mUriPhotoPath.getPath(), bmOptions);
//        int photoW = bmOptions.outWidth;
//        int photoH = bmOptions.outHeight;
//
//        // Determina o quanto dimensionar a imagem
//        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
//
//        // Decodifica a imagem em um arquivo de imagem para preencher a View
//        bmOptions.inJustDecodeBounds = false;
//        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(this.mViewHolder.mUriPhotoPath.getPath());//, bmOptions);
        Bitmap bitmapRotated = ImageUtil.rotateImageIfRequired(bitmap, this.mViewHolder.mUriPhotoPath);

        mImageProcess.StartNewProcessing(bitmapRotated);
        this.showProcessingImage(mImageProcess.GetCurrImage());
    }

    private void startNewImageProcessingFromTestPhoto() {

        InputStream imageStream = this.getResources().openRawResource(R.raw.img_to_process_01);
        Bitmap bitmapRotated = BitmapFactory.decodeStream(imageStream);

        mImageProcess.StartNewProcessing(bitmapRotated);
        this.showProcessingImage(mImageProcess.GetCurrImage());
    }

    private void processNextStepAndShowResult() {

        if (mImageProcess.HasFinished() == false ) {
            mImageProcess.ProcessNextStep();
            this.showProcessingImage(mImageProcess.GetCurrImage());
        }

//        while (mImageProcess.HasFinished() == false ) {
//            mImageProcess.ProcessNextStep();
//            this.showProcessingImage(mImageProcess.GetCurrImage());
//        }
    }

    private void showProcessingImage(Bitmap image) {

//        // Obtém as dimensões da View onde a imagem será colocada
//        int targetW = this.mViewHolder.mImageProcessing.getWidth();
//        int targetH = this.mViewHolder.mImageProcessing.getHeight();
//
//        // Obtém as dimensões da imagem
//        int photoW = image.getWidth();
//        int photoH = image.getHeight();
//
//        // Determina o quanto dimensionar a imagem
//        float scaleFactor = Math.max((float)targetW / photoW, (float)targetH / photoH);
//
//        Bitmap scaledImage = ImageUtil.scaleImage(image, scaleFactor);
//        this.mViewHolder.mImageProcessing.setImageBitmap(scaledImage);

        this.mViewHolder.mImageProcessing.setImageBitmap(image);
    }

    private void startNewClassificationFromTestDigit() {

        int img = 0;
        if (mCurrentDigitImgIdx == 0) img = R.raw.test_digit_0;
        else if (mCurrentDigitImgIdx == 1) img = R.raw.test_digit_1;
        else if (mCurrentDigitImgIdx == 2) img = R.raw.test_digit_2;
        else if (mCurrentDigitImgIdx == 3) img = R.raw.test_digit_3;
        else if (mCurrentDigitImgIdx == 4) img = R.raw.test_digit_4;
        else if (mCurrentDigitImgIdx == 5) img = R.raw.test_digit_5;
        else if (mCurrentDigitImgIdx == 6) img = R.raw.test_digit_6;
        else if (mCurrentDigitImgIdx == 7) img = R.raw.test_digit_7;
        else if (mCurrentDigitImgIdx == 8) img = R.raw.test_digit_8;
        else if (mCurrentDigitImgIdx == 9) img = R.raw.test_digit_9;

        mCurrentDigitImgIdx = (mCurrentDigitImgIdx + 1) % 10;

        InputStream imageStream = this.getResources().openRawResource(img);
        Bitmap digitImage = BitmapFactory.decodeStream(imageStream);

        float arrImage[] = BitmapToNormalizedFloatArray(digitImage);

        String result = classifier.recognizeImage(arrImage);

        this.mViewHolder.mImageProcessing.setImageBitmap(digitImage);
        mViewHolder.mTextClassificationResult.setText(result);
    }
}
