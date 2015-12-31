package com.blogspot.atomskjournal.mobidevplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.support.v7.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * 31.12.2015, by Max Shkil (mshkill@gmail.com)
 * Test app for MobiDev (music player).
 *
 * In this app music is played via service, the UI is controlled through MainActivity,
 * has its own file browser.
 *
 * Methods getTrackList() and setController() are used for refreshing the track list
 * and interface respectively.
 *
 * Current version leaks ServiceConnection and IntentReceiver - sorry, didn't have time to fix,
 * will fix later. The rest works more or less stable (at least as tested on Genymotion devices).
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, View.OnTouchListener {


    public final static String BROADCAST_ACTION = "com.blogspot.atomskjournal.mobidevplayer";
    public static boolean fromBrowser = false;

    private static String trackUri = "";
    private static long currentTrackId = 0;

    private BroadcastReceiver broadcastReceiver;
    private ArrayList<Track> trackList;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("mm:ss");
    private ListView trackView;
    private MusicService musicService;
    private Intent playIntent;
    private TrackAdapter trackAdapter;
    private Thread thread;

    private int playPositionPercentage;
    private int sortNameFlag = 1;
    private int sortArtistFlag = 1;
    private int sortAlbumFlag = 1;
    private int sortDurationFlag = 1;

    private String sortOrder = "";
    private String queryText = "";

    private boolean trackPicked = false;
    private boolean flagSeekBarChangedByUser = false;
    private boolean appIsClosed = false;

    private ImageButton btnPlay;
    private ImageButton btnStop;
    private ImageButton btnNext;
    private ImageButton btnPrev;
    private SeekBar seekBar;
    private TextView trackTime;
    private TextView trackDuration;
    private TextView trackTitleInfo;
    private TextView trackArtistInfo;
    private TextView trackAlbumInfo;



    public static long getCurrentTrackId() {
        return currentTrackId;
    }

    public static void setCurrentTrackId(long currentTrackId) {
        MainActivity.currentTrackId = currentTrackId;
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Restore state if device rotated
        if (savedInstanceState != null) {
            sortOrder = savedInstanceState.getString("sortOrder");
            sortNameFlag = savedInstanceState.getInt("sortNameFlag");
            sortArtistFlag = savedInstanceState.getInt("sortArtistFlag");
            sortAlbumFlag = savedInstanceState.getInt("sortAlbumFlag");
            sortDurationFlag = savedInstanceState.getInt("sortDurationFlag");
            currentTrackId = savedInstanceState.getLong("currentTrackId");
            trackPicked = savedInstanceState.getBoolean("trackPicked");

        }


        setContentView(R.layout.activity_main);

        //initialize track list

        trackView = (ListView) findViewById(R.id.track_list);
        trackList = new ArrayList<Track>();
        trackAdapter = new TrackAdapter(this, trackList);
        trackView.setAdapter(trackAdapter);

        // set listener to changing tracks

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                trackPicked = true;
                getTrackList(queryText);
                setController();

            }
        };

        IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(broadcastReceiver, filter);

        // set thread for seekbar and changing track time and run it continuously

        thread = new Thread(new Runnable() {

            Handler handler = new Handler();

            @Override
            public void run() {

                while (!appIsClosed) {

                    if (musicService != null) {

                        while (musicService.isPlaying() && !flagSeekBarChangedByUser) {

                                playPositionPercentage = 100 - (100 * (musicService.getDuration()
                                        - musicService.getPosition()) / musicService.getDuration());


                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        seekBar.setProgress(playPositionPercentage);
                                        trackTime.setText(timeFormat.format(musicService
                                                .getPosition()));
                                        trackDuration.setText(timeFormat.format((musicService
                                                .getDuration())));

                                    }
                                };
                                handler.post(r);

                            try {

                                TimeUnit.MILLISECONDS.sleep(500);

                            } catch (InterruptedException e) {

                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });

        thread.start();

        // set listener to picking file from another application and playing it via mobidevplayer

        Intent intent = getIntent();

        if (intent.getType() != null) {

            if (intent.getType().indexOf("audio/") != -1) {

                trackUri = intent.getDataString().replaceAll("%20", " ").replace("file://", "");

                getTrackList("");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save all the needed data for restoring after rotation
        outState.putString("sortOrder", sortOrder);
        outState.putInt("sortNameFlag", sortNameFlag);
        outState.putInt("sortArtistFlag", sortArtistFlag);
        outState.putInt("sortAlbumFlag", sortAlbumFlag);
        outState.putInt("sortDurationFlag", sortDurationFlag);
        outState.putLong("currentTrackId", currentTrackId);
        outState.putBoolean("trackPicked", trackPicked);
    }


    @Override
    protected void onDestroy() {

        //if the app is being closed

        if (this.isFinishing()) {

            appIsClosed = true;
            musicService.stopPlayback();
            stopService(playIntent);
            unregisterReceiver(broadcastReceiver);
            unbindService(musicConnection);

            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            musicService = null;

        }



        super.onDestroy();

    }

    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setList(trackList);

            //refresh track list and controller
            getTrackList("");
            setController();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.
                findItem(R.id.action_search));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                queryText = newText;
                getTrackList(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_open_folder:
                Intent intent = new Intent(MainActivity.this, FolderBrowser.class);
                startActivity(intent);
                break;
            case R.id.action_show_all_files:
                trackUri = "";
                getTrackList("");
                break;

            // here while sorting we set flags in case if user wants to sort in ascending
            // or descending order
            case R.id.action_sort_name:
                sortOrder = "name";
                sortArtistFlag = 1;
                sortAlbumFlag = 1;
                sortDurationFlag = 1;

                if (sortNameFlag == 0) {
                    sortNameFlag = 1;
                } else {
                    sortNameFlag = 0;
                }
                getTrackList(queryText);
                break;
            case R.id.action_sort_artist:
                sortOrder = "artist";
                sortNameFlag = 1;
                sortAlbumFlag = 1;
                sortDurationFlag = 1;

                if (sortArtistFlag == 0) {
                    sortArtistFlag = 1;
                } else {
                    sortArtistFlag = 0;
                }
                getTrackList(queryText);
                break;
            case R.id.action_sort_album:
                sortOrder = "album";
                sortArtistFlag = 1;
                sortNameFlag = 1;
                sortDurationFlag = 1;

                if (sortAlbumFlag == 0) {
                    sortAlbumFlag = 1;
                } else {
                    sortAlbumFlag = 0;
                }
                getTrackList(queryText);
                break;
            case R.id.action_sort_duration:
                sortOrder = "duration";
                sortArtistFlag = 1;
                sortAlbumFlag = 1;
                sortNameFlag = 1;

                if (sortDurationFlag == 0) {
                    sortDurationFlag = 1;
                } else {
                    sortDurationFlag = 0;
                }
                getTrackList(queryText);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sortBy(String order) {

        //flags are used in case if user wants to sort in ascending
        // or descending order

        if (trackList.size() != 0) {

            switch (order) {
                case "name":
                    Collections.sort(trackList, new Comparator<Track>() {
                        public int compare(Track a, Track b) {
                            return a.getTitle().compareTo(b.getTitle());
                        }
                    });
                    if (sortNameFlag == 1) {
                        Collections.reverse(trackList);
                    }
                    break;
                case "artist":
                    Collections.sort(trackList, new Comparator<Track>() {
                        public int compare(Track a, Track b) {
                            return a.getArtist().compareTo(b.getArtist());
                        }
                    });
                    if (sortArtistFlag == 1) {
                        Collections.reverse(trackList);
                    }
                    break;
                case "album":
                    Collections.sort(trackList, new Comparator<Track>() {
                        public int compare(Track a, Track b) {
                            return a.getAlbum().compareTo(b.getAlbum());
                        }
                    });
                    if (sortAlbumFlag == 1) {
                        Collections.reverse(trackList);
                    }
                    break;
                case "duration":
                    Collections.sort(trackList, new Comparator<Track>() {
                        public int compare(Track a, Track b) {
                            return String.valueOf(a.getDuration()).compareTo(String.
                                    valueOf(b.getDuration()));
                        }
                    });
                    if (sortDurationFlag == 1) {
                        Collections.reverse(trackList);
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private void setController() {

        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnStop = (ImageButton) findViewById(R.id.btnStop);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnPrev = (ImageButton) findViewById(R.id.btnPrev);

        btnPlay.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnPrev.setOnClickListener(this);

        btnPlay.setOnTouchListener(this);
        btnStop.setOnTouchListener(this);
        btnNext.setOnTouchListener(this);
        btnPrev.setOnTouchListener(this);



        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);

        if (musicService.isPlaying()){
            btnPlay.setImageResource(R.drawable.ic_pause_button);
        }

        trackTitleInfo = (TextView) findViewById(R.id.track_title_info);
        trackArtistInfo = (TextView) findViewById(R.id.track_artist_info);
        trackAlbumInfo = (TextView) findViewById(R.id.track_album_info);

        trackTitleInfo.setText(musicService.getTitle());
        trackArtistInfo.setText(musicService.getArtist());
        trackAlbumInfo.setText(musicService.getAlbum());

        trackTime = (TextView) findViewById(R.id.track_time);
        trackTime.setText(timeFormat.format(musicService.getPosition()));

        trackDuration = (TextView) findViewById(R.id.track_duration);
        trackDuration.setText(timeFormat.format(musicService.getPosition()));
    }

    //Get list of tracks: find all available tracks and extract their parameters

    private void getTrackList(String filter) {

        trackList.clear();
        trackAdapter.notifyDataSetChanged();



        ContentResolver tracksResolver = getContentResolver();

        String selection = MediaStore.Audio.Media.DATA + " LIKE ? AND (TITLE LIKE " + "'%"
                + filter + "%'" + " OR "
                + "ARTIST LIKE " + "'%" + filter + "%'" + " OR "
                + "ALBUM LIKE " + "'%" + filter + "%') AND " + MediaStore.Audio.Media.IS_MUSIC
                + " != 0";

        String[] projection = { "*" };
        String[] selectionArgs = {"%" + trackUri + "%"};

        //search both external and internal storage and add to list

        Cursor externalTracksCursor = tracksResolver.query(android.provider.MediaStore.Audio.Media
                .EXTERNAL_CONTENT_URI, null, selection, selectionArgs, null);
        Cursor internalTracksCursor = tracksResolver.query(android.provider.MediaStore.Audio.Media
                .INTERNAL_CONTENT_URI, null, selection, selectionArgs, null);


        if (externalTracksCursor != null && externalTracksCursor.moveToFirst()) {
            int idColumn = externalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int titleColumn = externalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int artistColumn = externalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumn = externalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int durationColumn = externalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);

            do {
                long thisId = externalTracksCursor.getLong(idColumn);
                String thisTitle = externalTracksCursor.getString(titleColumn);
                String thisArtist = externalTracksCursor.getString(artistColumn);
                String thisAlbum = externalTracksCursor.getString(albumColumn);
                int thisDuration = externalTracksCursor.getInt(durationColumn);

                trackList.add(new Track(thisId, thisTitle, thisArtist, thisAlbum,
                        thisDuration, false));

            } while (externalTracksCursor.moveToNext());

            sortBy(sortOrder);

            trackAdapter.notifyDataSetChanged();
        }

        if (internalTracksCursor != null && internalTracksCursor.moveToFirst()) {
            int idColumnInternal = internalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int titleColumnInternal = internalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int artistColumnInternal = internalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumnInternal = internalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int durationColumnInternal = internalTracksCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);

            do {
                long thisIdInternal = internalTracksCursor.getLong(idColumnInternal);
                String thisTitleInternal = internalTracksCursor.getString(titleColumnInternal);
                String thisArtistInternal = internalTracksCursor.getString(artistColumnInternal);
                String thisAlbumInternal = internalTracksCursor.getString(albumColumnInternal);
                int thisDurationInternal = internalTracksCursor.getInt(durationColumnInternal);


                trackList.add(new Track(thisIdInternal, thisTitleInternal, thisArtistInternal,
                        thisAlbumInternal, thisDurationInternal, true));

            } while (internalTracksCursor.moveToNext());

            sortBy(sortOrder);

            trackAdapter.notifyDataSetChanged();
        }

        //set active track

        for (Track track : trackList) {
            if (track.getId() == currentTrackId) {

                musicService.setTrack(trackList.indexOf(track));
            }
        }

    }

    public void trackPicked(View view) {
        musicService.setTrack(Integer.parseInt(view.getTag().toString()));
        musicService.playTrack();
        changeIconToPause();
        trackPicked = true;
        setController();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //if we resume from file browser, stop the player and set the first track
        // from the list active (also useful for preventing nullpointers from changing the list)
        if (fromBrowser) {
            musicService.stopPlayback();
            trackPicked = false;
            musicService.setTrack(0);
            getTrackList("");
            if (trackList.size() != 0) {
                setCurrentTrackId(trackList.get(0).getId());
            }
            setController();
            fromBrowser = false;
        }
    }

    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPlay:

                //play button changes to play/pause based on state of player

                if (musicService != null) {
                    if (!trackPicked){
                        musicService.setTrack(0);
                        musicService.playTrack();
                        trackPicked = true;
                        changeIconToPause();
                        setController();
                    } else if (musicService.isPlaying()) {
                        changeIconToPlay();
                        musicService.pausePlayer();
                    } else {
                        changeIconToPause();
                        musicService.startPlayer();
                    }
                }
                break;

            //stop button resets progress for current track

            case R.id.btnStop:
                musicService.pausePlayer();
                playPositionPercentage = 0;
                musicService.seek(playPositionPercentage);
                seekBar.setProgress(0);
                changeIconToPlay();
                trackPicked = false;
                setController();
                trackPicked = true;
                break;

            case R.id.btnNext:
                if (trackList.size() == 0) break;
                trackPicked = true;
                musicService.playNext();
                changeIconToPause();
                setController();
                break;

            case R.id.btnPrev:
                if (trackList.size() == 0) break;
                trackPicked = true;
                musicService.playPrev();
                changeIconToPause();
                setController();
                break;

            default:
                break;
        }
    }

    private void changeIconToPause() {
        btnPlay.setImageResource(R.drawable.ic_pause_button);
    }

    private void changeIconToPlay() {
        btnPlay.setImageResource(R.drawable.ic_play_button);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        flagSeekBarChangedByUser = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        int playPositionMillisec = (seekBar.getProgress()*musicService.getDuration())/100;
        seekTo(playPositionMillisec);
        flagSeekBarChangedByUser = false;
    }

    public static void setTrackUri(String newUri) {
        trackUri = newUri;
    }

    //onTouch() is used for showing reactions of buttons when clicked

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        if(event.getAction() == MotionEvent.ACTION_DOWN) {


            v.setBackgroundResource(R.color.background_list);
        }
        if(event.getAction() == MotionEvent.ACTION_UP) {
            v.setBackgroundResource(R.color.background_panel);
        }
        return false;
    }
}
