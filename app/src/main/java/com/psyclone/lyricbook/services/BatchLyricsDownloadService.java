package com.psyclone.lyricbook.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;

import com.psyclone.lyricbook.R;
import com.psyclone.lyricbook.activities.SavedLyricsActivity;
import com.psyclone.lyricbook.lyricsources.LyricWiki;

public class BatchLyricsDownloadService
        extends Service
{
    public BatchLyricsDownloadService() {}

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId)
    {
        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                int currentTrack = 0;
                int lyricsSuccessfullyDownloaded = 0;

                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("LyricBook")
                        .setSmallIcon(R.drawable.ic_notif_batch_download)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                Notification notification;
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                //////////PendingIntent used to launch SavedLyricsActivity when the notification is tapped
                Intent launchListActivity = new Intent(getApplicationContext(), SavedLyricsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, launchListActivity, 0);

                ContentResolver contentResolver = getContentResolver();
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String[] projection = {"artist", "title"};
                String selection = "artist IS NOT NULL AND artist <> ?";
                String[] selectionArgs = {"<unknown>"};
                Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, null);

                int totalTracks = cursor.getCount();

                if (totalTracks > 0)
                {
                    uri = Uri.parse("content://com.psyclone.lyricbook.provider");
                    contentResolver.delete(uri, null, null); //First clear the previously saved lyrics from the database

                    int artistColIndex = cursor.getColumnIndex("artist");
                    int trackColIndex = cursor.getColumnIndex("title");

                    builder.setContentText("Downloading lyrics...")
                            .setProgress(totalTracks, 0, false)
                            .setContentInfo("0/" + totalTracks);

                    notification = builder.build();
                    notification.flags = Notification.FLAG_ONGOING_EVENT; //Notification cannot be swiped away

                    manager.notify(1, notification);

                    startForeground(1, notification); //Service keeps downloading even if app is cleared from recents

                    while(cursor.moveToNext())
                    {
                        String artist = cursor.getString(artistColIndex);
                        String track = cursor.getString(trackColIndex);

                        String lyrics = LyricWiki.fetchLyrics(artist, track);

                        if(!lyrics.startsWith("ERROR:"))
                        {
                            //////////Lyrics downloaded successfully for this track
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("artist", artist);
                            contentValues.put("track", track);
                            contentValues.put("lyrics", lyrics);
                            contentValues.put("source", "LyricWiki");

                            contentResolver.insert(uri, contentValues);

                            lyricsSuccessfullyDownloaded++;
                        }

                        //////////Update progress in the notification
                        builder
                                .setProgress(totalTracks, ++currentTrack, false)
                                .setContentInfo(currentTrack + "/" + totalTracks);
                        notification = builder.build();
                        notification.flags = Notification.FLAG_ONGOING_EVENT;
                        manager.notify(1, notification);
                    }

                    cursor.close();
                    stopForeground(true); //Download completed. Service can now be killed by system if required.

                    String contentText;
                    if(lyricsSuccessfullyDownloaded == 0)
                        contentText = "Couldn't download lyrics for any track";
                    else if(lyricsSuccessfullyDownloaded == 1)
                        contentText = "Downloaded lyrics for 1 track";
                    else
                        contentText = "Downloaded lyrics for " + lyricsSuccessfullyDownloaded + " tracks";

                    builder.setContentInfo(null)
                            .setProgress(0, 0, false)
                            .setContentText(contentText)
                            .setContentIntent(pendingIntent);
                    notification = builder.build();
                    notification.flags = Notification.FLAG_AUTO_CANCEL; //Notification disappears after it has been used to launch SavedLyricsActivity

                    manager.notify(1, notification);
                }
                else
                {
                    builder.setContentText("You do not have any tracks in your library!")
                            .setContentIntent(pendingIntent);
                    notification = builder.build();
                    notification.flags = Notification.FLAG_AUTO_CANCEL;

                    manager.notify(1, notification);
                }

                stopSelf();
            }
        });

        t.start();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}