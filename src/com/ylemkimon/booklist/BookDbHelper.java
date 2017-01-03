package com.ylemkimon.booklist;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BookDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Book.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Book.BookEntry.TABLE_NAME + " (" +
                    Book.BookEntry._ID + " INTEGER PRIMARY KEY," +
                    Book.BookEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    Book.BookEntry.COLUMN_NAME_AUTHOR + TEXT_TYPE + COMMA_SEP +
                    Book.BookEntry.COLUMN_NAME_PUB + TEXT_TYPE + COMMA_SEP +
                    Book.BookEntry.COLUMN_NAME_CATEGORY + TEXT_TYPE + ")";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Book.BookEntry.TABLE_NAME;

    public BookDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
