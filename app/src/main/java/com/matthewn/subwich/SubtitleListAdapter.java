package com.matthewn.subwich;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.matthewn.subwich.ui.ClickableAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SubtitleListAdapter extends ClickableAdapter<SubtitleListAdapter.Holder, File> {
    public SubtitleListAdapter() {
        this(new ArrayList<File>());
    }

    public SubtitleListAdapter(List<File> entries) {
        super(entries);
        notifyDataSetChanged();
    }

    public static class Holder extends ClickableAdapter.ViewHolder {
        public final TextView view;

        public Holder(View v) {
            super(v);
            view = (TextView) v.findViewById(android.R.id.text1);
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.subtitle_list_item_layout, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        super.onBindViewHolder(holder, position);
        Resources res = holder.view.getResources();
        if (mEntries.size() == 1) {
            holder.view.setText(res.getText(R.string.label_single_episode));
        } else {
            String name = mEntries.get(position).getName();
            String title = res.getString(
                    R.string.label_episode, name.substring(0, name.indexOf(".")));
            holder.view.setText(title);
        }
    }
}