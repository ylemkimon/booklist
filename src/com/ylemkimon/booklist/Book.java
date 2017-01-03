package com.ylemkimon.booklist;

import android.provider.BaseColumns;

public final class Book {
    public Book() {}

    public static abstract class BookEntry implements BaseColumns {
        public static final String TABLE_NAME = "book";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_AUTHOR = "author";
        public static final String COLUMN_NAME_PUB = "pub";
        public static final String COLUMN_NAME_CATEGORY = "category";
    }
}
