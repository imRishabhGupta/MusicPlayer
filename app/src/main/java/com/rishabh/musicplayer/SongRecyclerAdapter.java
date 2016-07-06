package com.rishabh.musicplayer;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by user on 28-06-2016.
 */
public class SongRecyclerAdapter extends RecyclerView.Adapter {
    private Context context;
    private ArrayList<Song> songs;
    private LayoutInflater songInf;
    private LruCache<String, Bitmap> mLruCache;
    private  Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    public SongRecyclerAdapter(Context c, ArrayList<Song> songArrayList){
        context=c;
        songs=songArrayList;
        songInf= LayoutInflater.from(c);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 2;
        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = songInf.inflate(R.layout.list_item, parent, false);
        ViewHolder holder=new ViewHolder(v);
        return holder;

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder1, int position) {
        Song currSong = songs.get(position);
        ViewHolder holder=(ViewHolder)holder1;
        holder.songView.setText(currSong.getTitle());
        holder.artistView.setText(currSong.getArtist());
        if(currSong.getDuration()!="--:--") {
            long milliseconds = Long.parseLong(currSong.getDuration());
            int seconds = (int) (milliseconds / 1000) % 60;
            int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
            if(seconds%10==seconds%100)
                holder.durationView.setText(minutes + ":0" + seconds);
            else
                holder.durationView.setText(minutes + ":" + seconds);
        }
        else holder.durationView.setText("--:--");
        System.out.println("c here "+currSong.getId());
 //       Bitmap thumbnailImage = getBitmapFromMemCache(Long.toString(currSong.getId()));

   //     if (thumbnailImage == null){
       //     BitmapWorkerTask task = new BitmapWorkerTask(holder.imageView);
     //       task.execute(new Long(currSong.getId()));
        //}
        //holder.imageView.setImageBitmap(thumbnailImage);

        try {
            Uri track= ContentUris.withAppendedId(musicUri,currSong.getId());
            MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
            metaRetriver.setDataSource(context,track);
            byte[] art = metaRetriver.getEmbeddedPicture();
            Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            //Picasso.with(context).load(bitmap).into(holder.imageView);
            holder.imageView.setImageBitmap(bitmap);
        }catch (Exception e) {

            holder.imageView.setImageResource(R.mipmap.ic_launcher);
        }
        holder.linearLayout.setTag(position);
    }


    public void addBitmapToMemoryCache(Long key1, Bitmap bitmap) {
        String key=key1.toString();
        System.out.println("come here "+key);
       // if (getBitmapFromMemCache(key) == null) {
            mLruCache.put(key, bitmap);
        //}
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mLruCache.get(key);
    }

    private Bitmap getScaledImage (long id){
        Bitmap bitmap = null;

        try {
            Uri track= ContentUris.withAppendedId(musicUri,id);
            MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
            metaRetriver.setDataSource(context,track);
            byte[] art = metaRetriver.getEmbeddedPicture();
            //   Picasso.with(context).load(new File(track.getPath())).into(holder.imageView);
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);

        }catch (Exception e) {

        }

        return bitmap;
    }

    class BitmapWorkerTask extends AsyncTask<Long, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Long... params) {
            final Bitmap bitmap = getScaledImage(params[0]);
            addBitmapToMemoryCache(params[0], bitmap);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = (ImageView)imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView songView,artistView,durationView;
        public ImageView imageView;
        public LinearLayout linearLayout;
        public ViewHolder(View view){
            super(view);
            songView = (TextView)view.findViewById(R.id.song_title);
            artistView = (TextView)view.findViewById(R.id.song_artist);
            durationView = (TextView)view.findViewById(R.id.song_duration);
            imageView = (ImageView)view.findViewById(R.id.song_image);
            linearLayout=(LinearLayout)view;
        }
    }
}
