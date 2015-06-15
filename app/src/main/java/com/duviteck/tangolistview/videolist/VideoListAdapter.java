package com.duviteck.tangolistview.videolist;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.duviteck.tangolistview.R;
import com.duviteck.tangolistview.provider.SQLiteHelper.VideoTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duviteck on 15/06/15.
 */
public class VideoListAdapter extends CursorAdapter {

    private int titleIndex;
    private int urlIndex;
    private int totalSizeIndex;
    private int loadedSizeIndex;

    public VideoListAdapter(Context context, Cursor c) {
        super(context, c, false);
        initIndexes(c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.video_list_item, parent, false);
        inflate.setTag(createHolder(inflate));
        return inflate;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.title.setText(cursor.getString(titleIndex));
        holder.url.setText(cursor.getString(urlIndex));

        long totalSize = cursor.getLong(totalSizeIndex);
        long loadedSize = cursor.getLong(loadedSizeIndex);
        holder.totalSize.setText(cursor.getString(totalSizeIndex));
        holder.loadedSize.setText(cursor.getString(loadedSizeIndex));

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (totalSize == loadedSize) {
            lp.height = 500 + (int)(totalSize % 300);
        } else {
            lp.height = 360;
        }
        view.setLayoutParams(lp);
    }

    public List<String> getVideoUrls() {
        Cursor c = getCursor();
        List<String> urls = new ArrayList<>(c.getCount());

        c.moveToPosition(-1);
        while (c.moveToNext()) {
            urls.add(c.getString(urlIndex));
        }
        return urls;
    }

    private void initIndexes(Cursor c) {
        titleIndex = c.getColumnIndexOrThrow(VideoTable.TITLE);
        urlIndex = c.getColumnIndexOrThrow(VideoTable.URL);
        totalSizeIndex = c.getColumnIndexOrThrow(VideoTable.TOTAL_SIZE);
        loadedSizeIndex = c.getColumnIndexOrThrow(VideoTable.LOADED_SIZE);
    }

    private ViewHolder createHolder(View view) {
        ViewHolder holder = new ViewHolder();
        holder.title = (TextView) view.findViewById(R.id.title);
        holder.url = (TextView) view.findViewById(R.id.url);
        holder.totalSize = (TextView) view.findViewById(R.id.total_size);
        holder.loadedSize = (TextView) view.findViewById(R.id.loaded_size);
        return holder;
    }


    private static class ViewHolder {
        TextView title;
        TextView url;
        TextView totalSize;
        TextView loadedSize;
    }
}
