/*
 * Copyright 2017 The Android Things Samples Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.imageclassifier;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;

public class ImageClassifierActivity extends Activity {
    private static final String TAG = "ImageClassifierActivity";
    private static final String PIN_BUTTON = "GPIO_174";

    private ButtonInputDriver mButtonDriver;
    private boolean mProcessing;

    private String[] labels;
    private TensorFlowInferenceInterface inferenceInterface;
    private TextView messageView;
    private ImageView imageView;
    private ProgressBar progressView;
    // ADD CAMERA SUPPORT

    /**
     * Initialize the classifier that will be used to process images.
     */
    private void initClassifier() {
        this.inferenceInterface = new TensorFlowInferenceInterface(
                getAssets(), Helper.MODEL_FILE);
        this.labels = Helper.readLabels(this);
    }

    /**
     * Clean up the resources used by the classifier.
     */
    private void destroyClassifier() {
        inferenceInterface.close();
    }

    /**
     * Process an image and identify what is in it. When done, the method
     * {@link #onPhotoRecognitionReady(String[])} must be called with the results of
     * the image recognition process.
     *
     * @param image Bitmap containing the image to be classified. The image can be
     *              of any size, but preprocessing might occur to resize it to the
     *              format expected by the classification process, which can be time
     *              and power consuming.
     */
    private void doRecognize(Bitmap image) {

        imageView.setImageBitmap(image);
        Log.d(TAG, "Starting recognition");
        float[] pixels = Helper.getPixels(image);

        // Feed the pixels of the image into the
        // TensorFlow Neural Network
        inferenceInterface.feed(Helper.INPUT_NAME, pixels,
                Helper.NETWORK_STRUCTURE);

        // Run the TensorFlow Neural Network with the provided input
        inferenceInterface.run(Helper.OUTPUT_NAMES);

        // Extract the output from the neural network back
        // into an array of confidence per category
        float[] outputs = new float[Helper.NUM_CLASSES];
        inferenceInterface.fetch(Helper.OUTPUT_NAME, outputs);
        Log.d(TAG, "Recognition finished");

        // Send to the callback the results with the highest
        // confidence and their labels
        onPhotoRecognitionReady(Helper.getBestResults(outputs, labels));

    }

    /**
     * Initialize the camera that will be used to capture images.
     */
    private void initCamera() {
        // ADD CAMERA SUPPORT
    }

    /**
     * Clean up resources used by the camera.
     */
    private void closeCamera() {
        // ADD CAMERA SUPPORT
    }

    /**
     * Load the image that will be used in the classification process.
     * When done, the method {@link #onPhotoReady(Bitmap)} must be called with the image.
     */
    private void loadPhoto() {
        // ADD CAMERA SUPPORT
        Bitmap bitmap = getStaticBitmap();
        onPhotoReady(bitmap);
    }


    // --------------------------------------------------------------------------------------
    // NOTE: The normal codelab flow won't require you to change anything below this line,
    // although you are encouraged to read and understand it.

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initLayout();
        initCamera();
        initClassifier();
        initButton();
        Log.d(TAG, "READY");
    }

    private void initLayout() {
        setContentView(R.layout.main_activity);
        messageView = findViewById(R.id.message);
        imageView = findViewById(R.id.image);
        progressView = (ProgressBar) findViewById(R.id.progress);
    }

    /**
     * Register a GPIO button that, when clicked, will generate the {@link KeyEvent#KEYCODE_ENTER}
     * key, to be handled by {@link #onKeyUp(int, KeyEvent)} just like any regular keyboard
     * event.
     * <p>
     * If there's no button connected to the board, the doRecognize can still be triggered by
     * sending key events using a USB keyboard or `adb shell input keyevent 66`.
     */
    private void initButton() {
        try {
            mButtonDriver = new ButtonInputDriver(PIN_BUTTON,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER // the keycode to send
            );
            mButtonDriver.register();
        } catch (IOException e) {
            Log.w(TAG, "Cannot find button. Ignoring push button. Use a keyboard instead.", e);
        }
    }

    private Bitmap getStaticBitmap() {
        Log.d(TAG, "Using sample photo in res/drawable/sampledog_224x224.png");
        return BitmapFactory.decodeResource(this.getResources(), R.drawable.sampledog_224x224);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mProcessing) {
                displayMessage("Still processing, please wait");
                return true;
            }
            showProgress();
            displayMessage("Running photo recognition");
            mProcessing = true;
            loadPhoto();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void showProgress() {
        imageView.setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
    }

    private void onPhotoReady(Bitmap bitmap) {
        Log.d(TAG, "Photo ready");
        doRecognize(bitmap);
    }

    private void onPhotoRecognitionReady(String[] results) {
        String message = String.format("RESULT: %1$s", Helper.formatResults(results));
        displayMessage(message);
        mProcessing = false;
        hideProgress();
    }

    private void displayMessage(String message) {
        messageView.setText(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            destroyClassifier();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            closeCamera();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            if (mButtonDriver != null) mButtonDriver.close();
        } catch (Throwable t) {
            // close quietly
        }
    }
}
