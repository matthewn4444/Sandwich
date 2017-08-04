package com.matthewn.subwich.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Collection;
import java.util.List;

public abstract class ClickableAdapter<VH extends RecyclerView.ViewHolder, Type> extends RecyclerView.Adapter<VH>
        implements RecyclerViewAdapterListener {
    protected List<Type> mEntries;
    protected RecyclerViewAdapterListener mAdapterListener;

    public ClickableAdapter(List<Type> entries) {
        mEntries = entries;
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private RecyclerViewAdapterListener mListener;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
        }

        public void setOnClickListener(RecyclerViewAdapterListener listener) {
            mListener = listener;
        }

        @Override
        public void onClick(View v) {
            mListener.onClick(v, getAdapterPosition());
        }
    }

    public void add(Type entry) {
        mEntries.add(entry);
    }

    public void addAll(Collection<Type> entries) {
        mEntries.addAll(entries);
    }

    public void clear() {
        mEntries.clear();
    }

    public void setAdapterListener(RecyclerViewAdapterListener listener) {
        mAdapterListener = listener;
    }

    public Type getEntry(int i) {
        return mEntries.get(i);
    }

    @Override
    public void onClick(View v, int position) {
        if (mAdapterListener != null) {
            mAdapterListener.onClick(v, position);
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        ((ViewHolder) holder).setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return mEntries.size();
    }
}