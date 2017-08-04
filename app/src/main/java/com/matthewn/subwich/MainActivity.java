package com.matthewn.subwich;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.matthewn.subwich.ui.EnhancedRecyclerView;
import com.matthewn.subwich.ui.RecyclerViewAdapterListener;
import com.matthewn.subwich.ui.SpacesItemDecoration;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements RecyclerViewAdapterListener,
        SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_SUBTITLES_FOLDER = 1;

    private EnhancedRecyclerView mRecyclerView;
    private VideoListAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private View mEmptyView;
    private Button mWritePermButton;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Dialog mSubtitleSelectMessageDialog;

    private SharedPreferences mPrefs;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private final FileFilter mFolderFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mRecyclerView = (EnhancedRecyclerView) findViewById(R.id.recycleview);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mEmptyView = findViewById(R.id.emptytext);
        mWritePermButton = (Button) findViewById(R.id.requestwritepermissionsbutton);
        mLayoutManager = new GridLayoutManager(this, 1);
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(20));
        mRecyclerView.setLayoutManager(mLayoutManager);
        int verticalPadding = getResources()
                .getDimensionPixelOffset(R.dimen.activity_vertical_margin);
        int horizontalPadding = getResources()
                .getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
        mRecyclerView.setPadding(horizontalPadding, verticalPadding, horizontalPadding,
                verticalPadding);
        mAdapter = new VideoListAdapter();
        mAdapter.setAdapterListener(this);
        mRecyclerView.setAdapter(mAdapter);

        if (hasStoragePermissions()) {
            loadFiles();
        } else {
            mSwipeRefreshLayout.setEnabled(false);
            mWritePermButton.setVisibility(View.VISIBLE);
            mWritePermButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestStoragePermissions(MainActivity.this);
                }
            });
        }
    }

    @Override
    protected int getToolbarId() {
        return 0;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected int getMenuId() {
        return R.menu.main_menu;
    }

    @Override
    public void onClick(View v, int position) {
//        Intent in = new Intent(this, DetailsActivity.class);
//        in.putExtra(VideoEntry.EXTRA, mAdapter.getEntry(position));
//        startActivity(in);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case BaseActivity.PERMISSIONS_REQUEST_READ_STORAGE:
                if (hasStoragePermissions()) {
                    mWritePermButton.setVisibility(View.GONE);
                    loadFiles();
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change_folder:
                selectSubtitlesFolder();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public final void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_CODE_SUBTITLES_FOLDER) {
            Uri treeUri;
            if (resultCode == Activity.RESULT_OK) {
                // Save the selected folder as the subtitles folder
                treeUri = resultData.getData();
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri,
                        DocumentsContract.getTreeDocumentId(treeUri));
                String path = UriUtils.getPath(this, docUri);
                mPrefs.edit().putString(getString(R.string.settings_subtitles_folder), path).apply();

                // Load after selecting a new folder
                loadFiles();
            }
        }
    }

    @Override
    public void onRefresh() {
        if (hasStoragePermissions()) {
            loadFiles();
        }
    }

    private void selectSubtitlesFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_SUBTITLES_FOLDER);
    }

    private void loadFinished() {
        mSwipeRefreshLayout.setEnabled(true);
        mAdapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);
        mEmptyView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void showSelectSubtitlesFolderDialog() {
        if (mSubtitleSelectMessageDialog == null) {
            mSubtitleSelectMessageDialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.message_no_subtitles_folder_selected)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectSubtitlesFolder();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
        mSubtitleSelectMessageDialog.show();
    }

    private void loadFiles() {
        final String subsPath = mPrefs.getString(
                getString(R.string.settings_subtitles_folder), null);
        if (subsPath == null) {
            setTitle(R.string.label_no_subtitles_folder_selected);
            showSelectSubtitlesFolderDialog();
            loadFinished();
            return;
        }
        final File root = new File(subsPath);
        setTitle(root.getName() + "/");

        // Run routine to scan for files under root folder
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                mAdapter.clear();


                // Check for valid root folder
                if (!root.exists() || !root.isDirectory() || !root.canRead()) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    R.string.message_subtitles_folder_not_valid,
                                    Toast.LENGTH_SHORT).show();
                            selectSubtitlesFolder();
                        }
                    });
                } else {
                    // Scan for the videos and its information
                    File[] folders = root.listFiles(mFolderFilter);
                    if (folders != null) {
                        for (File folder : folders) {
                            String title = folder.getName();
                            String path = folder.getAbsolutePath();

                            // Search for cover image
                            String imagePath = null;
                            File imageFile = new File(path + "/cover.png");
                            if (imageFile.exists()
                                    || (imageFile = new File(path + "/cover.jpg")).exists()
                                    || (imageFile = new File(path + "/image.png")).exists()
                                    || (imageFile = new File(path + "/image.jpg")).exists()) {
                                imagePath = imageFile.getAbsolutePath();
                            }

                            // Search number of subtitles
                            List<Integer> episodeNumbers = new ArrayList<>();
                            File[] subs = folder.listFiles(VideoEntry.SubtitleFileFilter);
                            if (subs != null) {
                                for (File subtitleFile : subs) {
                                    String fileName = subtitleFile.getName();
                                    String name = fileName.substring(0, fileName.lastIndexOf("."));
                                    try {
                                        episodeNumbers.add(Integer.parseInt(name, 10));
                                    } catch (NumberFormatException e) {
                                        Log.w(TAG,
                                                "Cannot parse subtitle name because wrong format:" +
                                                        " [" + fileName + "]");
                                    }
                                }
                            }
                            if (!episodeNumbers.isEmpty()) {
                                mAdapter.add(new VideoEntry(title, path, imagePath, episodeNumbers));
                            } else {
                                Log.w(TAG, "Ignore folder '" + path
                                        + "' because no subtitles were found");
                            }
                        }
                    }
                }
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadFinished();
                    }
                });
            }
        });
    }
}
