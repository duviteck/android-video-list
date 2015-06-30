package com.duviteck.tangolistview.videolist;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.duviteck.tangolistview.R;
import com.duviteck.tangolistview.network.DataLoaderService;
import com.duviteck.tangolistview.provider.SQLiteHelper.VideoTable;
import com.duviteck.tangolistview.provider.VideoContentProvider;

import static com.duviteck.tangolistview.provider.DatabaseUtils.clearShowFlag;

public class VideoListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "VideoListFragment";

    private static final String KEY_FIRST_VISIBLE_POS = "firstVisiblePos";
    private static final String KEY_OFFSET = "offset";
    private static final String KEY_PLAYING_VIDEO_SEEK = "playingVideoSeek";

    private ListView listView;
    private View progressView;
    private VideoListAdapter adapter;

    private boolean isCreatedAfterRotate = false;

    private int firstVisiblePos;
    private int offset;
    private int playingVideoSeek;

    public VideoListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // onSaveInstanceState called each time when device is rotated,
        // so savedInstanceState after rotate is not null
        isCreatedAfterRotate = (savedInstanceState != null);

        if (savedInstanceState != null) {
            firstVisiblePos = savedInstanceState.getInt(KEY_FIRST_VISIBLE_POS, -1);
            offset = savedInstanceState.getInt(KEY_OFFSET, -1);
            playingVideoSeek = savedInstanceState.getInt(KEY_PLAYING_VIDEO_SEEK, -1);
        } else {
            resetStoredAdapterPosition();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isCreatedAfterRotate) {
            Log.i(TAG, "new start");
            resetState();
            clearShowFlag(getActivity());
            requestLoadVideoList();
        } else {
            Log.i(TAG, "start after rotate");
        }
        isCreatedAfterRotate = false;

        restartLoader();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_list, container, false);

        listView = (ListView) view.findViewById(R.id.list_view);
        progressView = view.findViewById(R.id.loading);

        return view;
    }

    @Override
    public void onDestroyView() {
        calcAdapterPositionForRestore();
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        calcAdapterPositionForRestore();
        outState.putInt(KEY_FIRST_VISIBLE_POS, firstVisiblePos);
        outState.putInt(KEY_OFFSET, offset);
        outState.putInt(KEY_PLAYING_VIDEO_SEEK, playingVideoSeek);
    }

    private void calcAdapterPositionForRestore() {
        if (listView != null && adapter != null && adapter.getCount() > 0) {
            int playingVideoPos = adapter.getPlayingVideoPos(listView);
            if (playingVideoPos >= 0) {
                // (after rotate) in case of playing video we prefer to show playing video
                firstVisiblePos = playingVideoPos;
                offset = 0;
            } else {
                firstVisiblePos = listView.getFirstVisiblePosition();
                offset = listView.getChildAt(0).getTop();
            }
            playingVideoSeek = adapter.getPlayingVideoSeek();
        }
    }

    private void resetStoredAdapterPosition() {
        firstVisiblePos = -1;
        offset = -1;
        playingVideoSeek = -1;
    }

    private void resetState() {
        listView.setAdapter(null);
        adapter = null;
    }

    private void restartLoader() {
        listView.setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);

        getLoaderManager().restartLoader(0, null, this);
    }


    private void requestLoadVideoList() {
        Intent intent = new Intent(getActivity(), DataLoaderService.class);
        intent.setAction(DataLoaderService.LOAD_VIDEO_LIST_ACTION);
        getActivity().startService(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(getActivity());
        cursorLoader.setUri(VideoContentProvider.VIDEO_LIST_URI);
        cursorLoader.setProjection(new String[]{
                VideoTable.URL,
                VideoTable.TITLE,
                VideoTable.THUMB,
                VideoTable.TOTAL_SIZE,
                VideoTable.LOADED_SIZE,
                VideoTable.WIDTH,
                VideoTable.HEIGHT,
                VideoTable.LOADING_STATUS
        });
        cursorLoader.setSelection(VideoTable.SHOULD_BE_SHOWN + " = ?");
        cursorLoader.setSelectionArgs(new String[]{"1"});
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() == 0) {
            return;
        }

        listView.setVisibility(View.VISIBLE);
        progressView.setVisibility(View.GONE);

        if (adapter == null) {
            adapter = new VideoListAdapter(getActivity(), data);
            listView.setAdapter(adapter);

            if (firstVisiblePos >= 0) {
                listView.setSelectionFromTop(firstVisiblePos, offset);
                if (playingVideoSeek >= 0) {
                    adapter.setPendingPlayVideo(firstVisiblePos, playingVideoSeek);
                }
                resetStoredAdapterPosition();
            }
        } else {
            adapter.swapCursor(data, listView);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
