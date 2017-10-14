package com.psyclone.lyricbook.activities;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.psyclone.lyricbook.R;
import com.psyclone.lyricbook.lyricsources.AZLyrics;
import com.psyclone.lyricbook.lyricsources.LyricWiki;

public class MainActivity
        extends AppCompatActivity
{
    private TextView tv_current_artist, tv_current_track, tv_hidden_source, tv_lyrics;
    private NestedScrollView sv_lyrics_scroller;
    private FloatingActionButton fab;
    private Snackbar snackbar;

    private SharedPreferences settings;
    private String source, artist, track;
    private boolean saveLyrics;

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_current_artist = (TextView) findViewById(R.id.tv_current_artist);
        tv_current_track = (TextView) findViewById(R.id.tv_current_track);
        tv_hidden_source = (TextView) findViewById(R.id.tv_hidden_source);
        tv_lyrics = (TextView) findViewById(R.id.tv_lyrics);
        sv_lyrics_scroller = (NestedScrollView) findViewById(R.id.sv_lyrics_scroller);
        fab = (FloatingActionButton) findViewById(R.id.fab_refresh);

        CoordinatorLayout cl_snackbar = (CoordinatorLayout) findViewById(R.id.cl_snackbar);

        //////////Hide FAB when lyrics are scrolled up and show when scrolled down
        sv_lyrics_scroller.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY > oldScrollY && fab.isShown())
                    fab.hide();
                if(scrollY < oldScrollY && !fab.isShown())
                    fab.show();
            }
        });

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        //////////Force screen on if user has enabled it
        if(settings.getBoolean("pref_force_screen_on", false))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //////////To be shown when lyrics fetch fails
        snackbar = Snackbar
                .make(cl_snackbar, "Try searching on Google?", Snackbar.LENGTH_INDEFINITE)
                .setAction("Yes", snackbarClickListener);

        Bundle bundle = getIntent().getExtras();

        if(bundle != null && bundle.containsKey("savedArtist")) //Activity is launched from SavedLyricsActivity
        {
            tv_current_artist.setText(bundle.getString("savedArtist"));
            tv_current_track.setText(bundle.getString("savedTrack"));
            tv_hidden_source.setText(bundle.getString("savedSource"));
            tv_lyrics.setText(bundle.getString("savedLyrics"));
        }
        else //Activity is launched directly
            readSharedPreferences(false);
    }

    private void readSharedPreferences(boolean isCalledByUser)
    {
        SharedPreferences current_music = getSharedPreferences("current_music", MODE_PRIVATE);
        artist = current_music.getString("artist", "testArtist");
        track = current_music.getString("track", "testTrack");
        source = settings.getString("pref_choose_source", "Automatic");
        saveLyrics = settings.getBoolean("pref_auto_save", true);

        if(artist.equals("testArtist") && track.equals("testTrack")) //Launched for the first time
        {
            tv_current_artist.setText(R.string.welcome);
            tv_current_track.setText(R.string.instructions);
            tv_lyrics.setText("");
        }

        else
        {
            if(track.startsWith(artist + " - "))
                track = track.replace(artist + " - ", ""); //Removes artist name from track for some SoundCloud tracks

            if(isCalledByUser
                    && tv_current_artist.getText().toString().equals(artist)
                    && tv_current_track.getText().toString().equals(track)
                    && (source.equals("Automatic") || tv_hidden_source.getText().toString().equals(source))
                    && !tv_lyrics.getText().toString().equals("Uh-oh! Looks like you aren't connected to the internet.")
                    )
                Toast.makeText(MainActivity.this, "Refresh not required", Toast.LENGTH_SHORT).show();

            else
            {
                tv_current_artist.setText(artist);
                tv_current_track.setText(track);
                tv_lyrics.setText(R.string.wait);

                new BackgroundLyricsFetcher().execute();
            }
        }
    }

    private class BackgroundLyricsFetcher
            extends AsyncTask<Void, Void, String[]>
    {
        private boolean isFetchedOffline;
        private ContentResolver contentResolver;
        private Uri uri;

        @Override
        protected void onPreExecute()
        {
            isFetchedOffline = false;
        }

        @Override
        protected String[] doInBackground(Void... params)
        {
            //////////1. Search for lyrics offline first
            contentResolver = getContentResolver();
            uri = Uri.parse("content://com.psyclone.lyricbook.provider");
            String[] columns = {"lyrics", "source"};
            String selection = "artist = ? AND track = ?";
            String[] selectionArgs = {artist, track};

            Cursor cursor = contentResolver.query(uri, columns, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst())
            {
                String sourceOfStoredLyrics = cursor.getString(cursor.getColumnIndex("source"));
                String lyricsFetchedOffline = cursor.getString(cursor.getColumnIndex("lyrics"));
                cursor.close();

                if(source.equals("Automatic") || sourceOfStoredLyrics.equals(source))
                //Lyrics found offline. No need to attempt online fetch
                {
                    isFetchedOffline = true;
                    return new String[]{lyricsFetchedOffline, sourceOfStoredLyrics};
                }

                ////////Delete previous entry from different source
                contentResolver.delete(uri, selection, selectionArgs);
            }

            String lyrics;

            if(source.equals("Automatic") || source.equals("AZLyrics"))
            {
                //////////First try AZLyrics, then LyricWiki
                lyrics = AZLyrics.fetchLyrics(artist, track);
                if(!lyrics.equals("") && !lyrics.startsWith("ERROR"))
                    return new String[] {lyrics, "AZLyrics"};

                lyrics = LyricWiki.fetchLyrics(artist, track);
                    return new String[] {lyrics, "LyricWiki"};
            }
            else
            {
                //////////First try LyricWiki, then AZLyrics
                lyrics = LyricWiki.fetchLyrics(artist, track);
                if(!lyrics.equals("") && !lyrics.startsWith("ERROR"))
                    return new String[] {lyrics, "LyricWiki"};

                lyrics = AZLyrics.fetchLyrics(artist, track);
                return new String[] {lyrics, "AZLyrics"};
            }
        }

        @Override
        protected void onPostExecute(String[] lyricsWithSource)
        {
            String lyricsReturned = lyricsWithSource[0];
            String sourceReturned = lyricsWithSource[1];

            tv_hidden_source.setText(sourceReturned);

            if(!lyricsReturned.startsWith("ERROR"))
            {
                tv_lyrics.setText(lyricsReturned);

                if (saveLyrics && !isFetchedOffline)
                {
                    //////////Save lyrics to device if the user wishes
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("artist", artist);
                    contentValues.put("track", track);
                    contentValues.put("lyrics", lyricsReturned);
                    contentValues.put("source", sourceReturned);

                    contentResolver.insert(uri, contentValues);
                }
            }
            else
            {
                //////////Check for Internet connectivity
                ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if(activeNetwork != null && activeNetwork.isConnectedOrConnecting())
                {
                    //////////Connected to Internet but lyrics search failed. Give the user the option to search the lyrics on Google
                    tv_lyrics.setText(R.string.fetchFail);
                    snackbar.show();
                }
                else
                    //////////Not connected to Internet
                    tv_lyrics.setText(R.string.noInternet);
            }
        }
    }

    public void onRefresh(View view)
    {
        if(snackbar.isShown())
            snackbar.dismiss();
        readSharedPreferences(true);
    }

    public void onList(MenuItem mi)
    {
        startActivity(new Intent(this, SavedLyricsActivity.class));
    }

    public void onSettings(MenuItem mi)
    {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    View.OnClickListener snackbarClickListener = new View.OnClickListener() {
        public void onClick(View v)
        {
            String searchString = String.format("http://www.google.com/search?q=%s+%s+lyrics", LyricWiki.encodeInUTF8(artist), LyricWiki.encodeInUTF8(track));
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(searchString)));
        }
    };
}
