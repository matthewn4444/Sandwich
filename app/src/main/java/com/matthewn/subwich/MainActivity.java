package com.matthewn.subwich;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.transition.Transition;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.matthewn.subwich.ui.EnhancedRecyclerView;
import com.matthewn.subwich.ui.RecyclerViewAdapterListener;
import com.matthewn.subwich.ui.SpacesItemDecoration;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends UsbDetectionActivity implements RecyclerViewAdapterListener,
        SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_SUBTITLES_FOLDER = REQUEST_CODE_SETUP_USB  + 1;

    private EnhancedRecyclerView mRecyclerView;
    private VideoListAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private View mEmptyView;
    private Button mWritePermButton;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Dialog mSubtitleSelectMessageDialog;

    private SharedPreferences mPrefs;

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
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected int getMenuId() {
        return R.menu.main_menu;
    }

    @Override
    public void onClick(View v, int position) {
        Intent in = new Intent(this, SubtitleListingActivity.class);
        View sharedElement1 = v.findViewById(R.id.image);
        View sharedElement2 = findViewById(R.id.footer);
        View sharedElement3 = findViewById(R.id.transitionList);
        View navigationBar = findViewById(android.R.id.navigationBarBackground);
        Pair<View, String> p1 = Pair.create(sharedElement1, sharedElement1.getTransitionName());
        Pair<View, String> p2 = Pair.create(sharedElement2, sharedElement2.getTransitionName());
        Pair<View, String> p3 = Pair.create(sharedElement3, sharedElement3.getTransitionName());
        Pair<View, String> p4 = null;
        if (navigationBar != null) {
            p4 = Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME);
        }
        ActivityOptions options = ActivityOptions
                .makeSceneTransitionAnimation(this, p1, p2, p3, p4);
        in.putExtra(SubtitleListingActivity.FILE_PATH_EXTRA, mAdapter.getEntry(position));
        startActivity(in, options.toBundle());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case BaseActivity.PERMISSIONS_REQUEST_READ_STORAGE:
                if (hasStoragePermissions()) {
                    invalidateOptionsMenu();
                    mWritePermButton.setVisibility(View.GONE);
                    loadFiles();
                }
                break;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Set the disabled state when no storage permissions are valid
        MenuItem changeFolderItem = menu.findItem(R.id.action_change_folder);
        changeFolderItem.setEnabled(hasStoragePermissions());
        Drawable resIcon = changeFolderItem.getIcon();
        if (!hasStoragePermissions()) {
            resIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        }
        changeFolderItem.setEnabled(hasStoragePermissions());
        changeFolderItem.setIcon(resIcon);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change_folder:
                Toast.makeText(this, R.string.message_select_subtitles_folder, Toast.LENGTH_SHORT)
                        .show();
                selectSubtitlesFolder();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public final void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == REQUEST_CODE_SUBTITLES_FOLDER && resultCode == Activity.RESULT_OK) {
            // Save the selected folder as the subtitles folder
            Uri treeUri = resultData.getData();
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri));
            String path = UriUtils.getPath(this, docUri);
            mPrefs.edit().putString(getString(R.string.settings_subtitles_folder), path).apply();

            // Load after selecting a new folder
            loadFiles();
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);

        // If moved subtitle file to external device, it will scroll to the top and move the video
        // that was last moved to the top of the list and update the transition
        if (data != null
                && data.getBooleanExtra(SubtitleListingActivity.RETURN_HAS_WRITTEN_EXTRA, false)) {
            // Resort the entries
            for (int i = 0; i < mAdapter.getItemCount(); i++) {
                VideoEntry entry = mAdapter.getEntry(i);
                entry.setLastUsed(getTimestamp(entry));
            }
            mAdapter.sort();
            mAdapter.notifyDataSetChanged();

            // Scroll to the top to do this animation
            mRecyclerView.scrollToPosition(0);
            setExitSharedElementCallback(new SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> elements) {
                    super.onMapSharedElements(names, elements);
                    elements.put("bg", mRecyclerView.getChildAt(0).findViewById(R.id.image));
                }
            });
            getWindow().getSharedElementExitTransition().addListener(
                    new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    setExitSharedElementCallback((SharedElementCallback) null);
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                }

                @Override
                public void onTransitionPause(Transition transition) {
                }

                @Override
                public void onTransitionResume(Transition transition) {

                }
            });
        }
    }

    @Override
    public void onRefresh() {
        if (hasStoragePermissions()) {
            loadFiles();
        }
    }

    private void selectSubtitlesFolder() {
        selectFolder(REQUEST_CODE_SUBTITLES_FOLDER);
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
        mAdapter.clear();
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
                        List<VideoEntry> entries = new ArrayList<>();
                        for (File folder : folders) {
                            VideoEntry entry = new VideoEntry(folder);
                            entry.setLastUsed(getTimestamp(entry));
                            entry.reloadData();
                            if (entry.getNumSubs() > 0) {
                                entries.add(entry);
                            } else {
                                Log.w(TAG, "Ignore folder '" + folder.getAbsolutePath()
                                        + "' because no subtitles were found");
                            }
                        }
                        if (!entries.isEmpty()) {
                            removeUnusedTimestamps();
                            mAdapter.addAll(entries);
                            mAdapter.sort();
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
