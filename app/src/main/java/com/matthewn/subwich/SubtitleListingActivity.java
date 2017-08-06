package com.matthewn.subwich;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.transition.Transition;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.matthewn.subwich.ui.EnhancedRecyclerView;
import com.matthewn.subwich.ui.RecyclerViewAdapterListener;
import com.matthewn.subwich.ui.SpacesItemDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SubtitleListingActivity extends UsbDetectionActivity
        implements RecyclerViewAdapterListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "SubtitleListingActivity";
    static final String FILE_PATH_EXTRA = "SubtitleListingActivity.file.path.extra";

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
    public void onClick(View v, int position) {

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
