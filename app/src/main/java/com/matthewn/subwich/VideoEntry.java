package com.matthewn.subwich;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VideoEntry implements Parcelable {
    private static final Set<String> NoneCapitalizableWords = new HashSet<>(Arrays.asList(
            "no", "of", "a", "to"));

    private final String mName;
    private final String mTitle;
    private final String mPath;
    private final String mCoverPath;
    private final int[] mEpisodes;

    public static final FileFilter SubtitleFileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(".srt");
        }
    };

    public static final Creator<VideoEntry> CREATOR = new Creator<VideoEntry>() {
        @Override
        public VideoEntry createFromParcel(Parcel source) {
            return new VideoEntry(source);
        }

        @Override
        public VideoEntry[] newArray(int size) {
            return new VideoEntry[size];
        }
    };

    VideoEntry(Parcel p) {
        mName = p.readString();
        mTitle = p.readString();
        mPath = p.readString();
        mCoverPath = p.readString();
        mEpisodes = p.createIntArray();
    }

    public VideoEntry(String title, String path, String coverPath, int[] episodesList) {
        mName = title.toLowerCase().replaceAll("\\s+", "-");
        mTitle = presentable(title);
        mPath = path;
        mCoverPath = coverPath;
        mEpisodes = episodesList;
        Arrays.sort(mEpisodes);
    }

    public VideoEntry(String title, String path, String coverPath, List<Integer> episodesList) {
        this(title, path, coverPath, new int[episodesList.size()]);
        for (int i = 0; i < episodesList.size(); i++) {
            mEpisodes[i] = episodesList.get(i);
        }
        Arrays.sort(mEpisodes);
    }

    public void loadImage(ImageView imageView) {
        if (mCoverPath != null) {
            Picasso.with(imageView.getContext())
                    .load(new File(mCoverPath))
                    .centerCrop()
                    .fit()
                    .into(imageView);
        }
    }

    public String getFormattedSubsInfo() {
        return mEpisodes.length > 0
                ? (mEpisodes.length == 1 ? "Has Subtitles" : mEpisodes.length + " Subtitles")
                : "No Subtitles";
    }

    public String getName() {
        return mName;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getCoverPath() {
        return mCoverPath;
    }

    public int[] getEpisodeSubList() {
        return mEpisodes;
    }

    public int getNumSubs() {
        return mEpisodes.length;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mTitle);
        dest.writeString(mPath);
        dest.writeString(mCoverPath);
        dest.writeIntArray(mEpisodes);
    }

    private String presentable(String str) {
        StringBuilder ret = new StringBuilder();
        String[] words = str.split(" ");
        for(int i = 0; i < words.length; i++) {
            if (i > 0 && NoneCapitalizableWords.contains(words[i].toLowerCase())) {
                ret.append(words[i].toLowerCase());
            } else {
                ret.append(Character.toUpperCase(words[i].charAt(0)));
                ret.append(words[i].substring(1));
            }
            if(i < words.length - 1) {
                ret.append(' ');
            }
        }
        return ret.toString();
    }
}