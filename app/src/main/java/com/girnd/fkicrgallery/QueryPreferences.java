package com.girnd.fkicrgallery;

import android.content.Context;
import android.preference.PreferenceManager;

public class QueryPreferences {
    private static final String QUERY_PREF = "Query Preferences";
    public static String getQuery(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(QUERY_PREF, null);
    }

    public static void setQuery(Context context, String query){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(QUERY_PREF, query).apply();
    }
}
