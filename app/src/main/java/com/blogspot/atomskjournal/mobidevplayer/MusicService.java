package com.blogspot.atomskjournal.mobidevplayer;


import android.app.Notification;
import android.support.v7.app.NotificationCompat;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;


public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final int NOTIFY_ID = 1;

    private final IBinder musicBind = new MusicBinder();

    private MediaPlayer player;
    private ArrayList<Track> tracks;
    private int trackPosition;
    private String trackTitle = "";
    private String trackArtist = "";
    private String trackAlbum = "";




    @Override
    public void onCreate() {
        super.onCreate();
        trackPosition = 0;
        player = new MediaPlayer();
        initMusicPlayer();
    }

    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(ArrayList<Track> tracks1) {
        tracks = tracks1;
    }

    public class MusicBinder extends Binder {

        MusicService getService() {
            return MusicService.this;
        }

    }

    public void playTrack() {

        Intent intent = new Intent(MainActivity.BROADCAST_ACTION);

        sendBroadcast(intent);

        player.reset();
        if (tracks.size() ==0) return;
        Track playTrack = tracks.get(trackPosition);

        trackTitle = playTrack.getTitle();
        trackArtist = playTrack.getArtist();
        trackAlbum = playTrack.getAlbum();

        MainActivity.setCurrentTrackId(playTrack.getId());

        long currentTrack = playTrack.getId();
        Uri trackUri;
        if (playTrack.isInternal()) {
            trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                    currentTrack);
        } else {
            trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currentTrack);
        }


        try {
            player.setDataSource(getApplicationContext(), trackUri);

        } catch (IOException e) {
            Toast.makeText(this, "Error setting data source", Toast.LENGTH_LONG).show();

        }
        try {
            player.prepareAsync();
        } catch (IllegalStateException e) {
            playNext();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.reset();
        return false;
    }



    @Override
    public void onCompletion(MediaPlayer mp) {

        if (player.getCurrentPosition() > 0) {
            mp.reset();
            playNext();
        }

    }

    @Override
    public void onDestroy() {

        stopForeground(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        mp.reset();
        return false;
    }



    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_play_button)
                .setTicker(trackTitle).setOngoing(true).setContentTitle("Playing")
                .setContentText(trackTitle);
        Notification notification = builder.build();
        startForeground(NOTIFY_ID, notification);






    }

    public void setTrack (int trackIndex) {

        trackPosition = trackIndex;
    }

    public int getPosition() {
       return player.getCurrentPosition();
    }

    public int getDuration() {
        return player.getDuration();
    }

    public String getTitle() {
        return trackTitle;
    }

    public String getArtist() {
        return trackArtist;
    }
    public String getAlbum() {
        return trackAlbum;
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void stopPlayback() {
        player.reset();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int position) {
        player.seekTo(position);

    }

    public void startPlayer() {
        player.start();
    }

    public void playPrev() {

        trackPosition --;
        if (trackPosition < 0) {
            trackPosition = tracks.size() - 1;

        }

        playTrack();

    }
    public void playNext() {


        trackPosition ++;
        if (trackPosition >= tracks.size()) {
            trackPosition = 0;

        }
        playTrack();
    }

}
