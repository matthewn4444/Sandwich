package com.matthewn.subwich;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseActivity extends AppCompatActivity {
    protected static final String TAG = "BaseActivity";
    protected static final int PERMISSIONS_REQUEST_READ_STORAGE = 1;

    protected final Handler mMainHandler = new Handler(Looper.getMainLooper());

    protected static void requestStoragePermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_READ_STORAGE);
    }

    private static boolean mStartedForResult = false;

    private SharedPreferences mPrefs;
    private String mLastWrittenKey;
    private final Map<String, Long> mVideoLastWrittenCache = new HashMap<>();
    private final Set<String> mUpdatedNames = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        mLastWrittenKey = getString(R.string.settings_subtitles_last_written);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

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
        loadVideoTimestamps();
        setResult(Activity.RESULT_OK);
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

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        loadVideoTimestamps();
    }

    protected long getTimestamp(VideoEntry entry) {
        synchronized (mVideoLastWrittenCache) {
            Long l = mVideoLastWrittenCache.get(entry.getName());
            if (l == null) {
                return 0;
            }
            mUpdatedNames.add(entry.getName());
            return l;
        }
    }

    protected void updateVideoTimeStamp(VideoEntry entry) {
        if (entry != null) {
            entry.updateUsed();
        }
        Set<String> data = new HashSet<>();
        synchronized (mVideoLastWrittenCache) {
            if (entry != null) {
                mVideoLastWrittenCache.put(entry.getName(), entry.getLastUsed());
            }
            for (String key : mVideoLastWrittenCache.keySet()) {
                long timestamp = mVideoLastWrittenCache.get(key);
                if (timestamp > 0) {
                    data.add(key + ":" + timestamp);
                }
            }
        }
        mPrefs.edit().putStringSet(mLastWrittenKey, data).apply();
    }

    private void loadVideoTimestamps() {
        // Get the last written timestamps of each video
        synchronized (mVideoLastWrittenCache) {
            Set<String> data = mPrefs.getStringSet(mLastWrittenKey, null);
            if (data != null) {
                for (String pair : data) {
                    String[] d = pair.split(":");
                    if (d.length == 2) {
                        try {
                            mVideoLastWrittenCache.put(d[0], Long.parseLong(d[1]));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            Log.w(TAG, "Ignored parsing timestamp invalid data: " + pair);
                        }
                    } else{
                        Log.w(TAG, "Ignored getting time stamp for this corrupted entry: " + pair);
                    }
                }
            }
        }
    }

    protected void removeUnusedTimestamps() {
        if (!mUpdatedNames.isEmpty()) {
            // Remove all the non-used cache timestamps
            synchronized (mVideoLastWrittenCache) {
                List<String> toRemove = new ArrayList<>();
                for (String name: mVideoLastWrittenCache.keySet()) {
                    if (!mUpdatedNames.contains(name)) {
                        toRemove.add(name);
                    }
                }
                for (String name: toRemove) {
                    mVideoLastWrittenCache.remove(name);
                }
                if (!toRemove.isEmpty()) {
                    updateVideoTimeStamp(null);
                }
            }
            mUpdatedNames.clear();
        }
    }

    protected boolean hasStartedForResult() {
        return mStartedForResult;
    }

    protected boolean hasStoragePermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

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