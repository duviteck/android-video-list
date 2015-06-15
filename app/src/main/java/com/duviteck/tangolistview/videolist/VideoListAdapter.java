package com.duviteck.tangolistview.videolist;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.duviteck.tangolistview.R;
import com.duviteck.tangolistview.VideoView;
import com.duviteck.tangolistview.network.DataLoaderService;
import com.duviteck.tangolistview.provider.SQLiteHelper.VideoTable;

/**
 * Created by duviteck on 15/06/15.
 */
public class VideoListAdapter extends CursorAdapter {
    private static final String TAG = "VideoListAdapter";

    private int titleIndex;
    private int urlIndex;
    private int totalSizeIndex;
    private int loadedSizeIndex;

    public VideoListAdapter(Context context, Cursor c) {
        super(context, c, false);
        initIndexes(c);
    }

    private void initIndexes(Cursor c) {
        titleIndex = c.getColumnIndexOrThrow(VideoTable.TITLE);
        urlIndex = c.getColumnIndexOrThrow(VideoTable.URL);
        totalSizeIndex = c.getColumnIndexOrThrow(VideoTable.TOTAL_SIZE);
        loadedSizeIndex = c.getColumnIndexOrThrow(VideoTable.LOADED_SIZE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.video_list_item, parent, false);
        inflate.setTag(createHolder(inflate));
        return inflate;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (isVideoContainer(cursor)) {
            bindVideoView(view, context, cursor);
        } else {
            bindLoadingView(view, context, cursor);
        }
    }

    public boolean isVideoContainer(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        return isVideoContainer(c);
    }

    public void playVideoIfLoaded(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder.videoView.getVisibility() == View.VISIBLE) {
            holder.videoView.start();
        }
    }

    private void bindLoadingView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.loadingContainer.setVisibility(View.VISIBLE);
        holder.videoContainer.setVisibility(View.GONE);

        holder.title.setText(cursor.getString(titleIndex));

        long totalSize = cursor.getLong(totalSizeIndex);
        long loadedSize = cursor.getLong(loadedSizeIndex);
        int progressPercent = (totalSize == 0) ? 0 : (int)(100 * loadedSize / totalSize);
        holder.progress.setText(context.getString(R.string.progress_text, progressPercent));
    }

    private void bindVideoView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.loadingContainer.setVisibility(View.GONE);
        holder.videoContainer.setVisibility(View.VISIBLE);

        String url = cursor.getString(urlIndex);
        holder.videoView.setVideoURI(DataLoaderService.getVideoUri(context, url));
//        holder.videoView.setZOrderOnTop(true);

//        if (holder.videoView.getWidth() > 0) {
//            Log.w("VideoListAdapter", "init height");
//            ViewGroup.LayoutParams lp = holder.videoView.getLayoutParams();
//            lp.height = holder.videoView.getWidth();
//            holder.videoView.setLayoutParams(lp);
//        }


        holder.videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "onCLick");
                ViewGroup.LayoutParams lp = holder.videoView.getLayoutParams();
                lp.height = holder.videoView.getWidth();
                holder.videoView.setLayoutParams(lp);

//                holder.videoView.setZOrderOnTop(true);
                holder.videoView.start();
            }
        });
    }

    private boolean isVideoContainer(Cursor cursor) {
        long totalSize = cursor.getLong(totalSizeIndex);
        long loadedSize = cursor.getLong(loadedSizeIndex);
        return (totalSize > 0) && (totalSize == loadedSize);
    }

    private ViewHolder createHolder(View view) {
        ViewHolder holder = new ViewHolder();

        holder.loadingContainer = view.findViewById(R.id.loading_container);
        holder.title = (TextView) view.findViewById(R.id.title);
        holder.progress = (TextView) view.findViewById(R.id.progress);

        holder.videoContainer = view.findViewById(R.id.video_container);
        holder.videoView = (VideoView) view.findViewById(R.id.video_view);
        holder.videoButton = view.findViewById(R.id.video_button);

        return holder;
    }


    private static class ViewHolder {
        View loadingContainer;
        TextView title;
        TextView progress;

        View videoContainer;
        VideoView videoView;
        View videoButton;
    }
}
