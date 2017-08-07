package com.matthewn.subwich;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.transition.Transition;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.matthewn.subwich.ui.EnhancedRecyclerView;
import com.matthewn.subwich.ui.RecyclerViewAdapterListener;
import com.matthewn.subwich.ui.SpacesItemDecoration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SubtitleListingActivity extends UsbDetectionActivity
        implements RecyclerViewAdapterListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "SubtitleListingActivity";
    static final String FILE_PATH_EXTRA = "SubtitleListingActivity.file.path.extra";
    private static final String SUBTITLE_FILE = "sub.srt";
    private static final String SUBTITLE_FOLDER = "sub";

    private EnhancedRecyclerView mRecyclerView;
    private SubtitleListAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private SharedPreferences mPrefs;
    private VideoEntry mEntry;
    private final List<File> mSubtitles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mRecyclerView = (EnhancedRecyclerView) findViewById(R.id.recycleview);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mLayoutManager = new GridLayoutManager(this, 1);
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(20));
        mRecyclerView.setLayoutManager(mLayoutManager);
        int verticalPadding = getResources()
                .getDimensionPixelOffset(R.dimen.activity_vertical_margin);
        int horizontalPadding = getResources()
                .getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
        mRecyclerView.setPadding(horizontalPadding, verticalPadding, horizontalPadding,
                verticalPadding);
        mAdapter = new SubtitleListAdapter();
        mAdapter.setAdapterListener(this);
        mRecyclerView.setAdapter(mAdapter);

        mEntry = getIntent().getParcelableExtra(FILE_PATH_EXTRA);
        if (mEntry == null) {
            Log.w(TAG, "Came to list subtitles but passed no data");
            finish();
        } else {
            getWindow().getSharedElementEnterTransition().addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    getWindow().getSharedElementEnterTransition().removeListener(this);
                    animateListing(true);
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

            mEntry.loadImage((ImageView) findViewById(R.id.image));
            setTitle(mEntry.getTitle());
            loadListing();
        }
    }

    @Override
    public void finishAfterTransition() {
        animateListing(false);
        super.finishAfterTransition();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.subtitle_listing_layout;
    }

    @Override
    protected int getMenuId() {
        return 0;
    }

    @Override
    public void onRefresh() {
        loadListing();
    }

    @Override
    public void onClick(View v, final int position) {
        updateUsbDevicesListing();

        final Uri selectedUri = getSelectedDevice();
        final Context ctx = this;
        if (selectedUri == null) {
            Toast.makeText(this, R.string.message_no_device_connected_to_write, Toast.LENGTH_SHORT)
                    .show();
        } else {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    int messageRes = 0;
                    boolean isError = true;

                    // Try to write file into /sub/sub.srt, create files if needed
                    DocumentFile file = DocumentFile.fromTreeUri(ctx, selectedUri);
                    if (!file.exists()) {
                        messageRes = R.string.message_device_missing;
                    } else {
                        // Create subtitle folder if not exists
                        DocumentFile subFolder = file.findFile(SUBTITLE_FOLDER);
                        if (subFolder == null) {
                            subFolder = file.createDirectory(SUBTITLE_FOLDER);
                        }
                        if (subFolder == null) {
                            messageRes = R.string.message_unable_create_folder_error;
                        } else {
                            OutputStream out = null;
                            InputStream in = null;
                            try {
                                // Create the subtitle file if not exists
                                DocumentFile srtFile = subFolder.findFile(SUBTITLE_FILE);
                                if (srtFile == null) {
                                    srtFile = subFolder.createFile(null, SUBTITLE_FILE);
                                }
                                if (srtFile == null) {
                                    messageRes = R.string.message_unable_create_output_error;
                                } else {
                                    // Copy data over to file
                                    out = getContentResolver().openOutputStream(srtFile.getUri());
                                    if (out != null) {
                                        in = new FileInputStream(mAdapter.getEntry(position));
                                        byte[] buffer = new byte[1024];
                                        int len;
                                        while ((len = in.read(buffer)) != -1) {
                                            out.write(buffer, 0, len);
                                        }
                                        messageRes = R.string.message_subtitle_success_copy;
                                        isError = false;
                                    } else {
                                        messageRes = R.string.message_output_stream_error;
                                    }
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                messageRes = R.string.message_missing_files;
                            } catch (IOException e) {
                                e.printStackTrace();
                                messageRes = R.string.message_copy_subtitle_error;
                            } finally {
                                if (out != null) {
                                    try {
                                        out.close();
                                    } catch (IOException ignored) {
                                    }
                                }
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException ignored) {
                                    }
                                }
                            }
                        }
                    }
                    final int finalMessageRes = messageRes;
                    final boolean finalIsError = isError;
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ctx, finalMessageRes, Toast.LENGTH_SHORT).show();
                            if (finalIsError) {
                                Log.e(TAG, getString(finalMessageRes));
                            }
                        }
                    });
                }
            });
        }
    }

    private void animateListing(boolean show) {
        mRecyclerView.animate()
                .alpha(show ? 1f : 0f)
                .setDuration(300)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    private void listingLoaded() {
        mSwipeRefreshLayout.setRefreshing(false);
        synchronized (mSubtitles) {
            mAdapter.addAll(mSubtitles);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void loadListing() {
        mAdapter.clear();
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                mEntry.reloadData();
                synchronized (mSubtitles) {
                    mSubtitles.clear();
                    String path = mEntry.getPath();
                    for (int i : mEntry.getEpisodeSubList()) {
                        mSubtitles.add(new File(path + "/" + i + ".srt"));
                    }
                }
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listingLoaded();
                    }
                });
            }
        });
    }
}
