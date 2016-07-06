package com.rishabh.musicplayer;

/**
 * Created by user on 09-06-2016.
 */
public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private String duration;
    private String genre;

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDuration() {
        return duration;
    }

    public String getGenre() {
        return genre;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public long getId() {
        return id;
    }

    public String getTitle(){return title;}

    public String getArtist(){return artist;}

}
