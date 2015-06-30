package com.duviteck.tangolistview.videolist;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.duviteck.tangolistview.R;
import com.duviteck.tangolistview.VideoView;
import com.duviteck.tangolistview.network.DataLoaderService;
import com.duviteck.tangolistview.network.DataLoaderService.LoadingStatus;
import com.duviteck.tangolistview.provider.SQLiteHelper.VideoTable;

import static com.duviteck.tangolistview.utils.Utils.calcProgress;

/**
 * Created by duviteck on 15/06/15.
 */
public class VideoListAdapter extends CursorAdapter {
    private static final String TAG = "VideoListAdapter";

    /**
     * Using a short delay on video playback helps to remove a "last frame" blink
     * on starting playing a new video.
     */
    private static final int VIDEO_PLAYBACK_DELAY = 300;

    private Context context;
    private int videoMaxWidth;

    private int titleIndex;
    private int urlIndex;
    private int thumbIndex;
    private int totalSizeIndex;
    private int loadedSizeIndex;
    private int widthIndex;
    private int heightIndex;
    private int loadingStatusIndex;

    private boolean ignoreNextNotifyDataSetChanged = false;
    private ViewHolder playingVideoViewHolder = null;

    private int pendingPlayVideoPos;
    private int pendingPlayVideoSeek;

    public VideoListAdapter(Context context, Cursor c) {
        super(context, c, false);
        this.context = context;
        this.videoMaxWidth = context.getResources().getDimensionPixelSize(R.dimen.video_max_width);
        initIndexes(c);
        clearPendingPlayVideo();
    }

    public void setPendingPlayVideo(int pos, int seek) {
        pendingPlayVideoPos = pos;
        pendingPlayVideoSeek = seek;
    }

    private void clearPendingPlayVideo() {
        pendingPlayVideoPos = -1;
        pendingPlayVideoSeek = -1;
    }

    private void initIndexes(Cursor c) {
        titleIndex = c.getColumnIndexOrThrow(VideoTable.TITLE);
        urlIndex = c.getColumnIndexOrThrow(VideoTable.URL);
        thumbIndex = c.getColumnIndexOrThrow(VideoTable.THUMB);
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
        final ViewHolder holder = (ViewHolder) view.getTag();
        if (playingVideoViewHolder == holder) {
            playingVideoViewHolder.videoView.stopPlayback();
        }

        if (isVideoContainer(cursor)) {
            bindVideoView(view, context, cursor);
        } else {
            bindLoadingView(view, context, cursor);
        }
    }

    private void bindLoadingView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.loadingContainer.setVisibility(View.VISIBLE);
        holder.videoContainerOuter.setVisibility(View.GONE);

        holder.title.setText(cursor.getString(titleIndex));

        int progress = calcProgress(cursor.getLong(loadedSizeIndex), cursor.getLong(totalSizeIndex));
        holder.progress.setText(context.getString(R.string.progress_text, progress));
        holder.progress2.setProgress(progress);

        final String thumb = cursor.getString(thumbIndex);
        Glide.with(context)
                .load(thumb)
                .placeholder(R.drawable.video_placeholder)
                .into(holder.thumb);
    }

    private void bindVideoView(final View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.loadingContainer.setVisibility(View.GONE);
        holder.videoContainerOuter.setVisibility(View.VISIBLE);

        final String url = cursor.getString(urlIndex);
        final String thumb = cursor.getString(thumbIndex);

        final int videoWidth = cursor.getInt(widthIndex);
        final int videoHeight = cursor.getInt(heightIndex);
        configureVideoContainerSizes(holder, videoWidth, videoHeight);

        holder.videoButton.setVisibility(View.VISIBLE);
        Glide.with(context)
                .load(thumb)
                .placeholder(R.drawable.video_placeholder)
                .dontTransform()
                .into(holder.videoButton);

        holder.videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onCLick");
                playVideo(holder, url, 0);
            }
        });

        if (pendingPlayVideoPos == cursor.getPosition()) {
            playVideo(holder, url, pendingPlayVideoSeek);
            clearPendingPlayVideo();
        }
    }

    private void configureVideoContainerSizes(final ViewHolder holder,
                                              final int videoWidth, final int videoHeight) {
        holder.videoContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                // calc target size
                int currentWidth = holder.videoContainer.getWidth() != 0
                        ? holder.videoContainer.getWidth()
                        : holder.videoContainer.getLayoutParams().width;
                int targetWidth = Math.min(videoMaxWidth, currentWidth);
                int targetHeight = videoHeight * targetWidth / videoWidth;

                ViewGroup.LayoutParams lp = holder.videoContainer.getLayoutParams();
                lp.width = targetWidth;
                lp.height = targetHeight;
                holder.videoContainer.setLayoutParams(lp);

                holder.videoContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
    }

    private void playVideo(final ViewHolder holder, String url, final int seekTo) {
        final Uri videoUri = DataLoaderService.getVideoUri(context, url);
        holder.videoView.setVideoURI(videoUri);

        holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "onPrepared");
                holder.videoButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        holder.videoButton.setVisibility(View.GONE);
                    }
                }, VIDEO_PLAYBACK_DELAY);
                holder.videoView.setVisibility(View.VISIBLE);
                holder.videoView.seekTo(Math.max(0, seekTo - VIDEO_PLAYBACK_DELAY));
                holder.videoView.start();

                if (playingVideoViewHolder != null) {
                    playingVideoViewHolder.videoView.stopPlayback();
                }
                playingVideoViewHolder = holder;
            }
        });

        holder.videoView.setMediaControllListener(new VideoView.MediaControllListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onPause() {
                onComplete();
            }

            @Override
            public void onStop() {
                onComplete();
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete");
                holder.videoButton.setVisibility(View.VISIBLE);
                holder.videoView.setVisibility(View.GONE);

                if (playingVideoViewHolder == holder) {
                    playingVideoViewHolder = null;
                }
            }
        });
    }

    private boolean isVideoContainer(Cursor cursor) {
        return LoadingStatus.fromValue(cursor.getInt(loadingStatusIndex)) == LoadingStatus.LOADED;
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

    public int getPlayingVideoPos(ListView listView) {
        if (playingVideoViewHolder == null) {
            return -1;
        }

        int firstVisiblePos = listView.getFirstVisiblePosition();
        for (int i = firstVisiblePos; i <= listView.getLastVisiblePosition(); i++) {
            View v = listView.getChildAt(i - firstVisiblePos);
            if (v.getTag() == playingVideoViewHolder) {
                return i;
            }
        }

        return -1;
    }

    public int getPlayingVideoSeek() {
        if (playingVideoViewHolder == null) {
            return -1;
        } else {
            return playingVideoViewHolder.videoView.getCurrentPosition();
        }
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
        holder.progress2.setProgress(progress);
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

    private ViewHolder createHolder(View view) {
        ViewHolder holder = new ViewHolder();

        holder.loadingContainer = view.findViewById(R.id.loading_container);
        holder.title = (TextView) view.findViewById(R.id.title);
        holder.progress = (TextView) view.findViewById(R.id.progress);
        holder.progress2 = (ProgressBar) view.findViewById(R.id.progress2);
        holder.thumb = (ImageView) view.findViewById(R.id.thumb);

        holder.videoContainerOuter = view.findViewById(R.id.video_container_outer);
        holder.videoContainer = view.findViewById(R.id.video_container);
        holder.videoView = (VideoView) view.findViewById(R.id.video_view);
        holder.videoButton = (ImageView) view.findViewById(R.id.video_button);

        return holder;
    }


    private static class ViewHolder {
        View loadingContainer;
        TextView title;
        TextView progress;
        ProgressBar progress2;
        ImageView thumb;

        View videoContainerOuter;
        View videoContainer;
        VideoView videoView;
        ImageView videoButton;
    }
}
