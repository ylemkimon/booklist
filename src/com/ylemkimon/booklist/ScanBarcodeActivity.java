/*
 * Some portion of this is from
 * BatchModeScanSample - SampleScanAndConfirmBarcodeActivity.java
 * Copyright 2014 Scandit AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ylemkimon.booklist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.scandit.barcodepicker.BarcodePicker;
import com.scandit.barcodepicker.OnScanListener;
import com.scandit.barcodepicker.ScanOverlay;
import com.scandit.barcodepicker.ScanSession;
import com.scandit.barcodepicker.ScanSettings;
import com.scandit.barcodepicker.ScanditLicense;
import com.scandit.base.system.SbSystemUtils;
import com.scandit.recognition.Barcode;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.List;

public class ScanBarcodeActivity extends Activity implements OnScanListener {

    private BarcodePicker mBarcodePicker;
    private UIHandler mHandler = null;
    private Button buttonScan = null;
    private View barcodeView = null;
    private TextView barcodeText = null;
    private Runnable mRunnable = null;

    public BookDbHelper mDbHelper;

    private static final String sScanditSdkAppKey = "cPP5xkyoEeSFGXgm0POs4UPAV2CF/TjTN79BbvV0WFc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.barcode);
        mHandler = new UIHandler(this);
        mDbHelper = new BookDbHelper(this);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeAndStartBarcodeScanning();
    }

    @Override
    protected void onPause() {
		super.onPause();
        mBarcodePicker.stopScanning();
    }

    @Override
    protected void onResume() {
		super.onResume();
    	mBarcodePicker.startScanning();
        mBarcodePicker.pauseScanning();
    }

    @SuppressWarnings("deprecation")
    private void initializeAndStartBarcodeScanning() {
        ScanditLicense.setAppKey(sScanditSdkAppKey);
        getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN,
                LayoutParams.FLAG_FULLSCREEN);

        if (SbSystemUtils.getDeviceDefaultOrientation(this) == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        barcodeView = findViewById(R.id.barcode_detected);
        barcodeText = (TextView) findViewById(R.id.barcode_text);
        mRunnable = new Runnable() {
            @Override
            public void run() {
                mBarcodePicker.removeView(barcodeView);
            }
        };

        ScanSettings settings = ScanSettings.create();
        settings.setSymbologyEnabled(Barcode.SYMBOLOGY_EAN13, true);
        settings.setScanningHotSpot(0.5f, 0.5f);
		settings.setCodeCachingDuration(-1);
		settings.setCodeDuplicateFilter(500);
        settings.setRestrictedAreaScanningEnabled(true);
        settings.setScanningHotSpotHeight(0.05f);
        mBarcodePicker = new BarcodePicker(this, settings);

        ScanOverlay overlay = mBarcodePicker.getOverlayView();
        overlay.drawViewfinder(true);
        overlay.setViewfinderDimension(0.7f, 0.3f, 0.4f, 0.3f);
        overlay.setBeepEnabled(true);
        overlay.setVibrateEnabled(false);

        mBarcodePicker.setOnScanListener(this);
        setContentView(mBarcodePicker);
        mBarcodePicker.getOverlayView().setTorchEnabled(false);
        mBarcodePicker.getOverlayView().setGuiStyle(ScanOverlay.GUI_STYLE_LASER);

        buttonScan = new Button(this);
        buttonScan.setTextColor(Color.WHITE);
        buttonScan.setTextColor(Color.WHITE);
        buttonScan.setTextSize(20);
        buttonScan.setGravity(Gravity.CENTER);
        buttonScan.setText("바코드 스캔");
        buttonScan.setBackgroundColor(0xFF39C1CC);
        addScanButton();
        buttonScan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resumeScanning();
                }
            });
    }

    private void addScanButton() {
        RelativeLayout layout = mBarcodePicker.getOverlayView();
        RelativeLayout.LayoutParams rParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 160);
        rParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        rParams.bottomMargin = 50;
        rParams.leftMargin = 50;
        rParams.rightMargin = 50;
        layout.addView(buttonScan, rParams);
    }

	@SuppressWarnings("deprecation")
    @Override
	public void didScan(ScanSession session) {
		List<Barcode> newlyDecoded = session.getNewlyRecognizedCodes();
        String cleanData = "";
        String data = newlyDecoded.get(0).getData();
        for (int i = 0; i < data.length(); ++i) {
            char c = data.charAt(i);
            cleanData += Character.isISOControl(c) ? '#' : c;
        }
        if (cleanData.length() > 30) {
            cleanData = cleanData.substring(0, 25)+"[...]";
        }

        InputStream inputStream;
        String result = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse httpResponse = httpclient.execute(new HttpGet("https://apis.daum.net/search/book?apikey=8faeaed3b789b3a7f6468527810892cb&result=1&output=json&searchType=isbn&q=" + cleanData));
            inputStream = httpResponse.getEntity().getContent();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null)
                result += line;
            inputStream.close();

            JSONObject item = new JSONObject(result).getJSONObject("channel").getJSONArray("item").getJSONObject(0);
            String title = item.getString("title");
            String author = item.getString("author");
            String pub_nm = item.getString("pub_nm");
            String category = item.getString("category");

            ContentValues values = new ContentValues();
            values.put(Book.BookEntry.COLUMN_NAME_TITLE, title);
            values.put(Book.BookEntry.COLUMN_NAME_AUTHOR, author);
            values.put(Book.BookEntry.COLUMN_NAME_PUB, pub_nm);
            values.put(Book.BookEntry.COLUMN_NAME_CATEGORY, category);

            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            db.insert(Book.BookEntry.TABLE_NAME, null, values);
            db.close();

            result = title + "(" + author + ", " + pub_nm + ") - " + category;
        } catch (Exception e) {
            result = "ERROR";
        }

        Message msg = mHandler.obtainMessage(UIHandler.SHOW_BARCODES, result);
        mHandler.removeCallbacks(mRunnable);
        mHandler.sendMessage(msg);
        mHandler.postDelayed(mRunnable, 3 * 1000);

        session.pauseScanning();
        session.clear();
	}

    @Override
    public void onBackPressed() {
        mBarcodePicker.stopScanning();
        finish();
    }

    private void resumeScanning() {
        mBarcodePicker.resumeScanning();
        mBarcodePicker.getOverlayView().removeView(buttonScan);
    }

    static private class UIHandler extends Handler {
        public static final int SHOW_BARCODES = 0;
        private WeakReference<ScanBarcodeActivity> mActivity;

        UIHandler(ScanBarcodeActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_BARCODES:
                    showSplash((String) msg.obj);
                    break;
            }

        }

        private void showSplash(String msg) {
            final ScanBarcodeActivity activity = mActivity.get();
            RelativeLayout layout = activity.mBarcodePicker;
            layout.removeView(activity.barcodeView);
            RelativeLayout.LayoutParams rParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 200);
            rParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            layout.addView(activity.barcodeView, rParams);

            if (msg.equals("ERROR")) {
                activity.barcodeText.setText("책을 찾지 못하였습니다. 수동으로 등록하여 주십시오.");

                LinearLayout.LayoutParams wmhw = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                LinearLayout ll = new LinearLayout(activity);
                ll.setOrientation(LinearLayout.VERTICAL);
                ll.setLayoutParams(wmhw);
                final EditText param1 = new EditText(activity);
                param1.setMaxLines(1);
                param1.setHint("도서명");
                param1.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                param1.setLayoutParams(wmhw);
                ll.addView(param1);
                final EditText param2 = new EditText(activity);
                param2.setMaxLines(1);
                param2.setHint("저자명");
                param2.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                param2.setLayoutParams(wmhw);
                ll.addView(param2);
                final EditText param3 = new EditText(activity);
                param3.setMaxLines(1);
                param3.setHint("출판사명");
                param3.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                param3.setLayoutParams(wmhw);
                ll.addView(param3);
                AlertDialog.Builder alert = new AlertDialog.Builder(activity)
                        .setView(ll)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String title = param1.getText().toString();
                                String author = param2.getText().toString();
                                String pub_nm = param3.getText().toString();
                                String category = "";

                                ContentValues values = new ContentValues();
                                values.put(Book.BookEntry.COLUMN_NAME_TITLE, title);
                                values.put(Book.BookEntry.COLUMN_NAME_AUTHOR, author);
                                values.put(Book.BookEntry.COLUMN_NAME_PUB, pub_nm);
                                values.put(Book.BookEntry.COLUMN_NAME_CATEGORY, category);

                                SQLiteDatabase db = activity.mDbHelper.getWritableDatabase();
                                db.insert(Book.BookEntry.TABLE_NAME, null, values);
                                db.close();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null);
                AlertDialog alertdialog = alert.create();
                alertdialog.setTitle("책을 찾지 못하였습니다. 수동으로 등록하여 주십시오.");
                alertdialog.show();
            } else
                activity.barcodeText.setText(msg);

            activity.addScanButton();
        }
    }
}
