package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    static final String authority = "edu.buffalo.cse.cse486586.groupmessenger2.provider";
    static final Uri uri = Uri.parse("content://"+authority);

    static DataEntry dbhelper;

    public class DataEntry extends SQLiteOpenHelper {
        public static final String DATABASE_NAME = "GroupMessenger";
        public static final String TableName = "Messages";
        public static final String Column_name1 = "key";
        public static final String Column_name2 = "value";

        private static final String Create_Table = "CREATE TABLE "+DataEntry.TableName+"("+DataEntry.Column_name1+" TEXT PRIMARY KEY NOT NULL,"
                +DataEntry.Column_name2+" TEXT);";
        private static final String checkStatement = "DROP TABLE IF EXISTS "+DataEntry.TableName;
        public DataEntry(Context context)
        {
            super(context,DATABASE_NAME,null,1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(Create_Table);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        try {
            SQLiteDatabase db = dbhelper.getWritableDatabase();
            long temp = db.insertWithOnConflict(DataEntry.TableName,null,values,SQLiteDatabase.CONFLICT_REPLACE);
            System.out.println(temp);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        dbhelper = new DataEntry(getContext());
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.v("query", selection);
        SQLiteDatabase dbreader = dbhelper.getWritableDatabase();
        String[] args = {selection};
        Cursor cursor = dbreader.query(DataEntry.TableName,new String[]{"key","value"},DataEntry.Column_name1+"=?",args,null,null,null);
        cursor.moveToFirst();
        System.out.println(cursor.getColumnIndex(DataEntry.Column_name2));
        Log.v("query", selection);
        return cursor;
    }
}
