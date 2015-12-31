package com.blogspot.atomskjournal.mobidevplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class TrackAdapter extends BaseAdapter {

    private ArrayList<Track> tracks;
    private LayoutInflater tracksInflater;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("mm:ss");

    public TrackAdapter(Context context, ArrayList<Track> tracks) {
        this.tracks = tracks;
        tracksInflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return tracks.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout trackLayout = (RelativeLayout) tracksInflater.inflate
                (R.layout.track, parent, false);


        TextView titleView = (TextView)trackLayout.findViewById(R.id.track_title);
        TextView artistView = (TextView)trackLayout.findViewById(R.id.track_artist);
        TextView albumView = (TextView)trackLayout.findViewById(R.id.track_album);
        TextView durationView = (TextView)trackLayout.findViewById(R.id.track_duration);
        ImageView trackStateIcon = (ImageView) trackLayout.findViewById(R.id.track_state_icon);


        Track currentTrack = tracks.get(position);
        titleView.setText(currentTrack.getTitle());
        artistView.setText(currentTrack.getArtist());
        albumView.setText(currentTrack.getAlbum());
        durationView.setText(timeFormat.format(currentTrack.getDuration()));
        trackStateIcon.setImageResource(R.drawable.ic_track_idle);

        //here we highlight the active track

        if(currentTrack.getId() == MainActivity.getCurrentTrackId()){
            trackStateIcon.setImageResource(R.drawable.ic_play_button);
            trackLayout.setBackgroundResource(R.color.background_panel);
            durationView.setBackgroundResource(R.color.background_panel);
        }

        trackLayout.setTag(position);

        return trackLayout;
    }

}
