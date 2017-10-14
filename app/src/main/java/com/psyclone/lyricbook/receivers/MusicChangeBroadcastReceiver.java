package com.psyclone.lyricbook.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class MusicChangeBroadcastReceiver
    extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle extras = intent.getExtras();
        String artist = extras.getString("artist");
        String track = extras.getString("track");

        if(artist != null && !artist.equals("") && track != null && !track.equals(""))
        {
            SharedPreferences current_music = context.getSharedPreferences("current_music", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = current_music.edit();
            editor.putString("artist", artist);
            editor.putString("track", track);
            editor.commit();
        }
    }
}