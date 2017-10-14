package com.psyclone.lyricbook.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.Nullable;

public class LyricBookContentProvider
    extends ContentProvider
{
    private static final String DB_NAME = "LyricBookDB.sqlite";
    private static final int DB_VERSION = 1;

    private static final String TABLE_OFFLINE_LYRICS = "offlinelyrics";
    private static final String COL_OFFLINE_LYRICS_ID = "_id";
    private static final String COL_OFFLINE_LYRICS_ARTIST = "artist";
    private static final String COL_OFFLINE_LYRICS_TRACK = "track";
    private static final String COL_OFFLINE_LYRICS_LYRICS = "lyrics";
    private static final String COL_OFFLINE_LYRICS_SOURCE = "source";

    private static final String CREATE_TABLE_OFFLINE_LYRICS_QUERY = String.format
            ("CREATE TABLE %s(%s integer primary key, %s text not null, %s text not null, %s text not null, %s text not null);",
                    TABLE_OFFLINE_LYRICS, COL_OFFLINE_LYRICS_ID, COL_OFFLINE_LYRICS_ARTIST, COL_OFFLINE_LYRICS_TRACK, COL_OFFLINE_LYRICS_LYRICS, COL_OFFLINE_LYRICS_SOURCE);

    private SQLiteDatabase db;

    @Override
    public boolean onCreate()
    {
        LyricBookDBConnectionHelper helper = new LyricBookDBConnectionHelper(getContext());
        db = helper.getWritableDatabase();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        return db.query(TABLE_OFFLINE_LYRICS, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public String getType(Uri uri)
    {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        if(db.insert(TABLE_OFFLINE_LYRICS, null, values) == -1)
            throw new RuntimeException("Error while saving lyrics");
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        return db.delete(TABLE_OFFLINE_LYRICS, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        return 0;
    }

    class LyricBookDBConnectionHelper
            extends SQLiteOpenHelper
    {
        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(CREATE_TABLE_OFFLINE_LYRICS_QUERY);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
        }

        public LyricBookDBConnectionHelper(Context context)
        {
            super(context, DB_NAME, null, DB_VERSION);
        }
    }
}
