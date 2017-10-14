package com.psyclone.lyricbook.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;

public class AlphabeticCursorAdapter
        extends SimpleCursorAdapter
        implements SectionIndexer
{
    AlphabetIndexer alphabetIndexer;

    public AlphabeticCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags, int columnForIndexing)
    {
        super(context, layout, c, from, to, flags);
        alphabetIndexer = new AlphabetIndexer(c, c.getColumnIndex(from[columnForIndexing]), "ABCDFGHIJKLMNOPQRSTUVWXYZ");
        alphabetIndexer.setCursor(c);
    }

    @Override
    public Object[] getSections()
    {
        return alphabetIndexer.getSections();
    }

    @Override
    public int getPositionForSection(int sectionIndex)
    {
        return alphabetIndexer.getPositionForSection(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int position)
    {
        return alphabetIndexer.getSectionForPosition(position);
    }
}