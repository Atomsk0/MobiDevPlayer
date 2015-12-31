package com.blogspot.atomskjournal.mobidevplayer;



/**
 * Model for a single track in the list
 */
public class Track {

    private long id;
    private String title;
    private String artist;
    private String album;
    private int duration;
    private boolean isInternal = false;


    public Track(long trackId, String trackTitle, String trackArtist, String trackAlbum,
                 int trackDuration, boolean isInternal) {
        id = trackId;
        title = trackTitle;
        artist = trackArtist;
        album = trackAlbum;
        duration = trackDuration;
        this.isInternal = isInternal;

    }


    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isInternal() {
        return isInternal;
    }
}
