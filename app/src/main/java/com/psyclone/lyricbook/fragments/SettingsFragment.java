package com.psyclone.lyricbook.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.psyclone.lyricbook.R;

public class SettingsFragment
        extends PreferenceFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}