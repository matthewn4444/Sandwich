package com.matthewn.subwich;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;

public abstract class BaseActivity extends AppCompatActivity {
    protected static final int PERMISSIONS_REQUEST_READ_STORAGE = 1;

    protected final Handler mMainHandler = new Handler(Looper.getMainLooper());

    protected static void requestStoragePermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_READ_STORAGE);
    }

    private static boolean mStartedForResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        // Setup toolbar
        if (getToolbarId() != 0) {
            final Toolbar toolbar = getToolbarView();
            if (toolbar != null) {
                setSupportActionBar(toolbar);
            }
        }

        // Check permissions
        if (!hasStoragePermissions()) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.message_request_storage_permission_explanation)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(BaseActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        PERMISSIONS_REQUEST_READ_STORAGE);
                            }
                        })
                        .show();
            } else {
                requestStoragePermissions(this);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getMenuId() == 0) {
            return false;
        }
        getMenuInflater().inflate(getMenuId(), menu);
        return true;
    }

    protected void selectFolder(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        mStartedForResult = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStartedForResult = false;
    }

    protected boolean hasStartedForResult() {
        return mStartedForResult;
    }

    protected boolean hasStoragePermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    protected Toolbar getToolbarView() {
        return (Toolbar) findViewById(getToolbarId());
    }

    protected abstract int getToolbarId();
    protected abstract int getLayoutId();
    protected abstract int getMenuId();

    protected static void log(Object... txt) {
        String returnStr = "";
        int i = 1;
        int size = txt.length;
        if (size != 0) {
            returnStr = txt[0] == null ? "null" : txt[0].toString();
            for (; i < size; i++) {
                returnStr += ", "
                        + (txt[i] == null ? "null" : txt[i].toString());
            }
        }
        Log.i("lunch", returnStr);
    }
}