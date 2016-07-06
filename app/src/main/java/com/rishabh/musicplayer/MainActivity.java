package com.rishabh.musicplayer;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;

import com.rishabh.musicplayer.MusicService.MusicBinder;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MediaPlayerControl{

    private ArrayList<Song> songs;
    private SongRecyclerAdapter songRecyclerAdapter;
    private MusicService musicSrv;
    private Intent playIntent;
    private MusicController controller;
    private boolean paused=false, playbackPaused=false;
    private boolean musicBound=false;
    private String title,album,artist,duration,genre;
    private Song song;
    private RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setController();

        recyclerView=(RecyclerView)findViewById(R.id.songs_view);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        songs=new ArrayList<>();
        songRecyclerAdapter=new SongRecyclerAdapter(this,songs);
        recyclerView.setAdapter(songRecyclerAdapter);

        new LongOperation().execute("");
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else
            return 0;

    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        else
            return false;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else
            return 0;
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            musicSrv = binder.getService();
            musicSrv.setList(songs);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(!musicBound){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
            setController();
        }
    }

    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void setController(){
        controller=new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.rl));
        controller.setEnabled(true);
    }

    @Override
    protected void onStop() {
        paused=true;
        controller.hide();
        musicBound=false;
        System.out.println("stopped");
        if(!isPlaying())
            stopService(playIntent);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unbindService(musicConnection);
        musicSrv=null;
        System.out.println("destroyed");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class LongOperation extends AsyncTask<String, Song , String> {

        @Override
        protected String doInBackground(String... params) {

            ContentResolver musicResolver = getContentResolver();
            Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
            if(musicCursor!=null && musicCursor.moveToFirst()){
                int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
                int isMusicColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
                int displayNameColumn=musicCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);

                int dataColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                do {
                    long thisId = musicCursor.getLong(idColumn);
                    String isMusic=musicCursor.getString(isMusicColumn);
                    String data=musicCursor.getString(dataColumn);
                    String displayName=musicCursor.getString(displayNameColumn);
                    if(isMusic.equals("0"))
                        continue;
                    Uri track= ContentUris.withAppendedId(musicUri,thisId);
                    MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
                    metaRetriver.setDataSource(getApplicationContext(),track);

                    album=metaRetriver .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    title=metaRetriver .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    artist=metaRetriver .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    duration=metaRetriver .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    genre=metaRetriver .extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);

                    System.out.println(album);
                    System.out.println(title);
                    System.out.println(isMusic);
                    System.out.println(data);
                    System.out.println(displayName);
                    System.out.println(artist);
                    System.out.println(duration);
                    System.out.println(genre);

                    song =new Song();
                    song.setId(thisId);
                    if(title!=null)
                        song.setTitle(title);
                    else if(displayName!=null)
                        song.setTitle(displayName);

                    if(album!=null)
                        song.setAlbum(album);
                    else
                        song.setAlbum("Unknown");
                    if(duration!=null)
                        song.setDuration(duration);
                    else
                        song.setDuration("--:--");
                    if(genre!=null)
                        song.setGenre(genre);
                    else
                        song.setGenre("Unknown");
                    if(artist!=null)
                        song.setArtist(artist);
                    else
                        song.setArtist("Unknown artist");
                    publishProgress(song);
                }
                while (musicCursor.moveToNext());
            }
            musicCursor.close();
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
           //songs=songArrayList;
       //     Collections.sort(songs, new Comparator<Song>(){
         //       public int compare(Song a, Song b){
           //       return a.getTitle().compareTo(b.getTitle());
             //}
            //});
            songRecyclerAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Song... song) {
            songs.add(song[0]);
            System.out.println("yes");
            songRecyclerAdapter.notifyDataSetChanged();
        }
    }
}