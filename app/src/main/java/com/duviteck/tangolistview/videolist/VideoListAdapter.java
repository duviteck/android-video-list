package com.duviteck.tangolistview.videolist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.duviteck.tangolistview.R;
import com.duviteck.tangolistview.VideoView;
import com.duviteck.tangolistview.network.DataLoaderService;
import com.duviteck.tangolistview.network.DataLoaderService.LoadingStatus;
import com.duviteck.tangolistview.provider.SQLiteHelper.VideoTable;

import static com.duviteck.tangolistview.network.DataLoaderService.getVideoFirstFramePath;
import static com.duviteck.tangolistview.utils.Utils.calcProgress;

/**
 * Created by duviteck on 15/06/15.
 */
public class VideoListAdapter extends CursorAdapter {
    private static final String TAG = "VideoListAdapter";

    private Context context;

    private int titleIndex;
    private int urlIndex;
    private int totalSizeIndex;
    private int loadedSizeIndex;
    private int widthIndex;
    private int heightIndex;
    private int loadingStatusIndex;

    private boolean ignoreNextNotifyDataSetChanged = false;

    public VideoListAdapter(Context context, Cursor c) {
        super(context, c, false);
        this.context = context;
        initIndexes(c);
    }

    private void initIndexes(Cursor c) {
        titleIndex = c.getColumnIndexOrThrow(VideoTable.TITLE);
        urlIndex = c.getColumnIndexOrThrow(VideoTable.URL);
        totalSizeIndex = c.getColumnIndexOrThrow(VideoTable.TOTAL_SIZE);
        loadedSizeIndex = c.getColumnIndexOrThrow(VideoTable.LOADED_SIZE);
        widthIndex = c.getColumnIndexOrThrow(VideoTable.WIDTH);
        heightIndex = c.getColumnIndexOrThrow(VideoTable.HEIGHT);
        loadingStatusIndex = c.getColumnIndexOrThrow(VideoTable.LOADING_STATUS);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.video_list_item, parent, false);
        inflate.setTag(createHolder(inflate));
        return inflate;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Log.i(TAG, "bindView [position]:" + cursor.getPosition());

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

        int progress = calcProgress(cursor.getLong(loadedSizeIndex), cursor.getLong(totalSizeIndex));
        holder.progress.setText(context.getString(R.string.progress_text, progress));
    }

    private void bindVideoView(final View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.loadingContainer.setVisibility(View.GONE);
        holder.videoContainer.setVisibility(View.VISIBLE);

        String url = cursor.getString(urlIndex);
        final Uri videoUri = DataLoaderService.getVideoUri(context, url);

        final int videoWidth = cursor.getInt(widthIndex);
        final int videoHeight = cursor.getInt(heightIndex);
        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                int width = view.getWidth() != 0 ? view.getWidth() : lp.width;
                int height = videoHeight * width / videoWidth;

                lp.width = width;
                lp.height = height;
                view.setLayoutParams(lp);

                lp = holder.videoView.getLayoutParams();
                lp.width = width;
                lp.height = height;
                holder.videoView.setLayoutParams(lp);

                lp = holder.videoButton.getLayoutParams();
                lp.width = width;
                lp.height = height;
                holder.videoButton.setLayoutParams(lp);

                view.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });

        holder.videoButton.setVisibility(View.VISIBLE);
        holder.videoButton.setImageBitmap(BitmapFactory.decodeFile(getVideoFirstFramePath(context, url)));

        holder.videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "onCLick");
                holder.videoButton.setVisibility(View.GONE);
                holder.videoView.setVideoURI(videoUri);
                holder.videoView.start();
            }
        });
    }

    private boolean isVideoContainer(Cursor cursor) {
        return LoadingStatus.fromValue(cursor.getInt(loadingStatusIndex)) == LoadingStatus.LOADED;
    }

    private ViewHolder createHolder(View view) {
        ViewHolder holder = new ViewHolder();

        holder.loadingContainer = view.findViewById(R.id.loading_container);
        holder.title = (TextView) view.findViewById(R.id.title);
        holder.progress = (TextView) view.findViewById(R.id.progress);

        holder.videoContainer = view.findViewById(R.id.video_container);
        holder.videoView = (VideoView) view.findViewById(R.id.video_view);
        holder.videoButton = (ImageView) view.findViewById(R.id.video_button);

        return holder;
    }

    public void swapCursor(Cursor cursor, ListView listView) {
        /**
         * if no one of currently visible items is changed state, we can ignore next
         * notifyDataSetChanged() and just update loading progress (if needed)
         *
         * it helps to play video without jerks during swapCursor
         */
        int firstVisiblePos = listView.getFirstVisiblePosition();
        int lastVisiblePos = listView.getLastVisiblePosition();

        boolean needNotify = false;
        for (int i = firstVisiblePos; i <= lastVisiblePos; i++) {
            needNotify |= isLoadingStateChanged(i, getCursor(), cursor);
        }

        if (!needNotify) {
            ignoreNextNotifyDataSetChanged = true;

            // since notifyDataSetChanged() won't called, we need to update progress view itself
            for (int i = firstVisiblePos; i <= lastVisiblePos; i++) {
                View child = listView.getChildAt(i - firstVisiblePos);
                updateProgressIfNeeded(child, i, cursor);
            }
        }

        super.swapCursor(cursor);
    }

    private boolean isLoadingStateChanged(int pos, Cursor oldCursor, Cursor newCursor) {
        oldCursor.moveToPosition(pos);
        newCursor.moveToPosition(pos);

        // changing state from NOT_LOADING to LOADING is not interesting here,
        // since both states has same visual type (aka LoadingView)
        return !isVideoContainer(oldCursor) && isVideoContainer(newCursor);
    }

    private void updateProgressIfNeeded(View child, int pos, Cursor cursor) {
        cursor.moveToPosition(pos);

        if (isVideoContainer(cursor)) {
            return;
        }

        ViewHolder holder = (ViewHolder) child.getTag();
        int progress = calcProgress(cursor.getLong(loadedSizeIndex), cursor.getLong(totalSizeIndex));
        holder.progress.setText(context.getString(R.string.progress_text, progress));
    }

    @Override
    public void notifyDataSetChanged() {
        if (ignoreNextNotifyDataSetChanged) {
            Log.i(TAG, "notifyDataSetChanged ignored");
            ignoreNextNotifyDataSetChanged = false;
        } else {
            super.notifyDataSetChanged();
        }
    }

    private static class ViewHolder {
        View loadingContainer;
        TextView title;
        TextView progress;

        View videoContainer;
        VideoView videoView;
        ImageView videoButton;
    }
}
