package com.ylemkimon.booklist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MainActivity extends Activity {
    private boolean mPaused = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.setTheme(android.R.style.Theme_DeviceDefault);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        Button activityAimButton = (Button) findViewById(R.id.aim_scan_button);
        activityAimButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPaused) {
                    return;
                }
                startActivity(new Intent(MainActivity.this,
                        ScanBarcodeActivity.class));
            }
        });

        Button excelButton = (Button) findViewById(R.id.excel_button);
        excelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPaused) {
                    return;
                }
                new ExportToCsvTask().execute();
            }
        });

        Button resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPaused) {
                    return;
                }
                BookDbHelper helper = new BookDbHelper(MainActivity.this);
                SQLiteDatabase db = helper.getWritableDatabase();
                helper.onUpgrade(db, BookDbHelper.DATABASE_VERSION, BookDbHelper.DATABASE_VERSION);
                db.close();
            }
        });
    }

    @Override
    protected void onPause() {
        mPaused = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        mPaused = false;
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private class ExportToCsvTask extends AsyncTask<Void, Void, String> {
        private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("내보내는 중...");
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCancelable(false);
            dialog.show();
        }

        protected String doInBackground(Void... args) {
            File file = new File(Environment.getExternalStorageDirectory(), "book.csv");
            try {
                file.createNewFile();
                OutputStream os = new FileOutputStream(file);
                os.write(239);
                os.write(187);
                os.write(191);
                CSVWriter csvWrite = new CSVWriter(new OutputStreamWriter(os, "UTF-8"));
                SQLiteDatabase db = new BookDbHelper(MainActivity.this).getReadableDatabase();
                Cursor curCSV = db.rawQuery("select * from " + Book.BookEntry.TABLE_NAME, null);
                dialog.setMax(curCSV.getCount());
                String header[] = { "도서명", "저자명", "출판사명", "분류" };
                csvWrite.writeNext(header);
                while (curCSV.moveToNext()) {
                    dialog.incrementProgressBy(1);
                    String data[] = { curCSV.getString(1), curCSV.getString(2), curCSV.getString(3), curCSV.getString(4) };
                    csvWrite.writeNext(data);
                }
                curCSV.close();
                csvWrite.close();
                os.close();
                return file.getAbsolutePath();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String message) {
            dialog.dismiss();
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(message)), "text/csv"), message));
        }
    }
}
