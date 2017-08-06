package com.matthewn.subwich;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VideoEntry implements Parcelable {
    private static final String TAG = "VideoEntry";
    private static final Set<String> NoneCapitalizableWords = new HashSet<>(Arrays.asList(
            "no", "of", "a", "to"));

    private final String mName;
    private final String mTitle;
    private final String mPath;
    private String mCoverPath;
    private int[] mEpisodes;
    private long mLastUsed;

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
        mLastUsed = p.readLong();
    }

    public VideoEntry(File file) {
        String title = file.getName();
        mName = title.toLowerCase().replaceAll("\\s+", "-");
        mTitle = presentable(title);
        mPath = file.getAbsolutePath();
        mCoverPath = null;
        mEpisodes = null;
        mLastUsed = 0;
    }

    public void loadImage(ImageView imageView) {
        if (mCoverPath != null) {
            Picasso.with(imageView.getContext())
                    .load(new File(mCoverPath))
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
        return mEpisodes != null ? mEpisodes.length : 0;
    }

    public String getPath() {
        return mPath;
    }

    public long getLastUsed() {
        return mLastUsed;
    }

    public void updateUsed() {
        mLastUsed = System.currentTimeMillis();
    }

    public void reloadData() {
        File folder = new File(mPath);

        // Search for cover image
        File imageFile = new File(mPath + "/cover.png");
        if (imageFile.exists()
                || (imageFile = new File(mPath + "/cover.jpg")).exists()
                || (imageFile = new File(mPath + "/image.png")).exists()
                || (imageFile = new File(mPath + "/image.jpg")).exists()) {
            mCoverPath = imageFile.getAbsolutePath();
        }

        // Search number of subtitles
        List<Integer> episodeNumbers = new ArrayList<>();
        File[] subs = folder.listFiles(SubtitleFileFilter);
        if (subs != null) {
            for (File subtitleFile : subs) {
                String fileName = subtitleFile.getName();
                String name = fileName.substring(0, fileName.lastIndexOf("."));
                try {
                    episodeNumbers.add(Integer.parseInt(name, 10));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Subtitle name has wrong format: [" + fileName + "]");
                }
            }
            if (!episodeNumbers.isEmpty()) {
                mEpisodes = new int[episodeNumbers.size()];
                for (int i = 0; i < episodeNumbers.size(); i++) {
                    mEpisodes[i] = episodeNumbers.get(i);
                }
            } else {
                mEpisodes = null;
            }
        }
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
        dest.writeLong(mLastUsed);
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