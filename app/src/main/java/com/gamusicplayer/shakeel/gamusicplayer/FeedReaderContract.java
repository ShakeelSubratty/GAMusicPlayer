package com.gamusicplayer.shakeel.gamusicplayer;

import android.provider.BaseColumns;



public final class FeedReaderContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public FeedReaderContract() {}

    /* Inner class that defines the table contents */
    public static abstract class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "GAMusicPlayer";
        public static final String COLUMN_NAME_ENTRY_ID = "entryID";
        public static final String COLUMN_NAME_SONG_NAME = "songName";
        public static final String COLUMN_NAME_SONG_ID = "songID";
        public static final String COLUMN_NAME_SPOTIFY_ID = "spotifyID";
        public static final String COLUMN_NAME_FITNESS = "fitness";
        public static final String COLUMN_NAME_NULLABLE = "";
    }
}