package com.matthewn.subwich;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.matthewn.subwich.ui.ClickableAdapter;

import java.util.ArrayList;
import java.util.List;

public class VideoListAdapter extends ClickableAdapter<VideoListAdapter.Holder, VideoEntry> {
    public VideoListAdapter() {
        this(new ArrayList<VideoEntry>());
    }

    public VideoListAdapter(List<VideoEntry> entries) {
        super(entries);
        notifyDataSetChanged();
    }

    public static class Holder extends ClickableAdapter.ViewHolder {
        public final View view;
        public final ImageView imageView;
        public final TextView nameView;
        public final TextView detailsView;

        public Holder(View v) {
            super(v);
            view = v;
            imageView = (ImageView) v.findViewById(R.id.image);
            nameView = (TextView) v.findViewById(R.id.text);
            detailsView = (TextView) v.findViewById(R.id.details);
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.entry_layout, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        super.onBindViewHolder(holder, position);
        VideoEntry entry = mEntries.get(position);
        entry.loadImage(holder.imageView);
        holder.nameView.setText(Html.fromHtml(entry.getTitle()));
        holder.detailsView.setText(entry.getFormattedSubsInfo());
    }
}