package com.duviteck.tangolistview.videolist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.duviteck.tangolistview.entity.VideoEntity;

import java.util.List;

/**
 * Created by duviteck on 15/06/15.
 */
public class VideoListAdapterTemp extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;
    private List<VideoEntity> videos;

    public VideoListAdapterTemp(Context context, List<VideoEntity> videos) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.videos = videos;
    }

    @Override
    public int getCount() {
        return videos.size();
    }

    @Override
    public VideoEntity getItem(int position) {
        return videos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position); // TODO
    }
}
