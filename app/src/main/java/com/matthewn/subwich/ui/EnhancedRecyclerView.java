package com.matthewn.subwich.ui;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;

public class EnhancedRecyclerView extends RecyclerView {
    public static final int DefaultLoadingThreshold = 3;

    private ContextMenuInfo menuInfo;
    private LoadMoreListener mLoadMoreListener;
    private int mLoadFromBottomThreshold;
    private boolean mDataLoading;

    public interface LoadMoreListener {
        public void loadMoreRequested();
    }

    public EnhancedRecyclerView(Context context) {
        this(context, null);
    }

    public EnhancedRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EnhancedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLoadFromBottomThreshold = DefaultLoadingThreshold;
        mDataLoading = false;
    }

    public void setLoadMoreListener(LoadMoreListener listener) {
        mLoadMoreListener = listener;
    }

    public void setLoadingThreshold(int n) {
        mLoadFromBottomThreshold = n;
    }

    public void notifyLoadFinished() {
        mDataLoading = false;
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return menuInfo;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getChildPosition(originalView);
        if (longPressPosition >= 0) {
            final long longpressid = getAdapter().getItemId(longPressPosition);
            menuInfo = new ContextMenuInfo(longPressPosition, longpressid);
            return super.showContextMenuForChild(originalView);
        }
        return false;
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        if(dy > 0 && !mDataLoading && mLoadMoreListener != null) {
            if (getLayoutManager() instanceof LinearLayoutManager) {
                LinearLayoutManager manager = (LinearLayoutManager) getLayoutManager();
                int numViewed = manager.getChildCount();
                int offset = manager.findFirstVisibleItemPosition();
                int totalItems = manager.getItemCount();
                if (offset >= totalItems - numViewed * mLoadFromBottomThreshold) {
                    mDataLoading = true;
                    mLoadMoreListener.loadMoreRequested();
                }
            }
        }
    }

    public static class ContextMenuInfo implements ContextMenu.ContextMenuInfo {
        public ContextMenuInfo(int position, long id) {
            this.position = position;
            this.id = id;
        }

        final public int position;
        final public long id;
    }
}
