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

    private ListView listView;
    private VideoListAdapter adapter;

    private boolean isCreatedAfterRotate = false;

    public VideoListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // onSaveInstanceState called each time when device is rotated,
        // so savedInstanceState after rotate is not null
        isCreatedAfterRotate = (savedInstanceState != null);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isCreatedAfterRotate) {
            Log.i(TAG, "new start");
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

        return view;
    }

    private void restartLoader() {
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
                VideoTable.TOTAL_SIZE,
                VideoTable.LOADED_SIZE,
                VideoTable.WIDTH,
                VideoTable.HEIGHT,
                VideoTable.LOADING_STATUS
        });
        cursorLoader.setSelection(VideoTable.SHOULD_BE_SHOWN + " = ?");
        cursorLoader.setSelectionArgs(new String[] {"1"});
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() == 0) {
            return;
        }

        if (adapter == null) {
            adapter = new VideoListAdapter(getActivity(), data);
            listView.setAdapter(adapter);
        } else {
            adapter.swapCursor(data, listView);
        }
//        Log.i(TAG, "adapter size: " + adapter.getCount());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
