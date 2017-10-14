package com.psyclone.lyricbook.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.psyclone.lyricbook.R;
import com.psyclone.lyricbook.adapters.AlphabeticCursorAdapter;
import com.psyclone.lyricbook.services.BatchLyricsDownloadService;

public class SavedLyricsActivity
        extends AppCompatActivity
        implements AdapterView.OnItemClickListener
{
    private TextView tv_no_saved_lyrics;
    private ListView lv_lyrics;
    private AlphabeticCursorAdapter cursorAdapter;
    private ProgressDialog progressDialog;
    private ContentResolver contentResolver;
    private Uri uri;

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list_lyrics, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_lyrics);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //For the back button at the left of the action bar

        tv_no_saved_lyrics = (TextView) findViewById(R.id.tv_no_saved_lyrics);
        lv_lyrics = (ListView) findViewById(R.id.lv_lyrics);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setIndeterminate(true);

        contentResolver = getContentResolver();
        uri = Uri.parse("content://com.psyclone.lyricbook.provider");

        new BackgroundSavedLyricsFetcher().execute();
    }

    private class BackgroundSavedLyricsFetcher
            extends AsyncTask<Void, Void, Cursor>
    {
        @Override
        protected void onPreExecute()
        {
            progressDialog.show();
        }

        @Override
        protected Cursor doInBackground(Void... params)
        {
            String[] columns = {"_id", "track", "artist", "source"};
            return contentResolver.query(uri, columns, null, null, "track");
        }

        @Override
        protected void onPostExecute(Cursor cursor)
        {
            if(cursor.moveToFirst())
            {
                int layoutResId = R.layout.list_item_lyrics;
                String[] cursorColumns = {"track", "artist", "source"};
                int[] resId = {R.id.tv_list_track, R.id.tv_list_artist, R.id.tv_list_source};

                cursorAdapter = new AlphabeticCursorAdapter(SavedLyricsActivity.this, layoutResId, cursor, cursorColumns, resId, 0, 0);
                lv_lyrics.setAdapter(cursorAdapter);
                lv_lyrics.setOnItemClickListener(SavedLyricsActivity.this);
            }
            else
            //////////No saved lyrics found on the device
            {
                lv_lyrics.setVisibility(View.INVISIBLE);
                tv_no_saved_lyrics.setVisibility(View.VISIBLE);
            }

            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) //For back button at the left of the action bar
    {
        if (item.getItemId() == android.R.id.home )
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id)
    {
        final Cursor tempCursor = (Cursor) cursorAdapter.getItem(position);
        final String track = tempCursor.getString(tempCursor.getColumnIndex("track"));
        final String artist = tempCursor.getString(tempCursor.getColumnIndex("artist"));
        final String source = tempCursor.getString(tempCursor.getColumnIndex("source"));

        final String[] columns = {"lyrics"};
        final String selection = "artist = ? AND track = ? AND source = ?";
        final String[] selectionArgs = {artist, track, source};

        final Cursor cursor = contentResolver.query(uri, columns, selection, selectionArgs, null);
        cursor.moveToNext();

        final CharSequence[] options = {"View", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle("Choose an action")
                .setItems(options, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        if(which == 0)
                        {
                            //////////Fetch the corresponding lyrics and launch MainActivity with the track data
                            String savedLyrics = cursor.getString(cursor.getColumnIndex("lyrics"));
                            tempCursor.close();
                            cursor.close();

                            Intent intent = new Intent(SavedLyricsActivity.this, MainActivity.class);

                            Bundle bundle = new Bundle();
                            bundle.putString("savedArtist", artist);
                            bundle.putString("savedTrack", track);
                            bundle.putString("savedSource", source);
                            bundle.putString("savedLyrics", savedLyrics);

                            intent.putExtras(bundle);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); //Clears last instance of main activity from backstack and launches a new instance of MainActivity

                            startActivity(intent);
                        }
                        else
                        {
                            //////////Delete the corresponding saved lyrics from the database
                            contentResolver.delete(uri, selection, selectionArgs);

                            String[] columns = {"_id", "track", "artist", "source"};
                            cursorAdapter.swapCursor(contentResolver.query(uri, columns, null, null, "track"));
                            cursorAdapter.notifyDataSetChanged();
                            Toast.makeText(SavedLyricsActivity.this, "Successfully deleted", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    public void onDelete(MenuItem mi)
    {
        if(tv_no_saved_lyrics.getVisibility() == View.VISIBLE)
            Toast.makeText(SavedLyricsActivity.this, "No lyrics to delete", Toast.LENGTH_SHORT).show();
        else
        {
            new AlertDialog.Builder(this).
                    setMessage("Delete all saved lyrics?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            contentResolver.delete(uri, null, null);
                            Toast.makeText(SavedLyricsActivity.this, "Saved lyrics deleted", Toast.LENGTH_SHORT).show();
                            finish();
                            startActivity(getIntent());
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    public void checkConnection(MenuItem mi)
    {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork != null && activeNetwork.isConnectedOrConnecting())
            requestReadStoragePermission();
        else
            Toast.makeText(SavedLyricsActivity.this, "Device not connected to the internet. Try again when you have a stable connection", Toast.LENGTH_LONG).show();
    }

    private void requestReadStoragePermission()
    {
        //////////Request read permission at runtime for Android 6.0+
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        else
            batchLyricsDownload();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if(grantResults[0] == 0)
            batchLyricsDownload();
        else
            Toast.makeText(SavedLyricsActivity.this, "Lyrics cannot be downloaded without permission to read storage", Toast.LENGTH_LONG).show();
    }

    private void batchLyricsDownload()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        {
            builder.setTitle("Warning");
            builder.setMessage("LyricBook will now attempt to download lyrics for all tracks on your device. Make sure you have a stable connection for at least 5 minutes.");
            builder.setPositiveButton("Proceed", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    startService(new Intent(SavedLyricsActivity.this, BatchLyricsDownloadService.class));
                    finish();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    }
}