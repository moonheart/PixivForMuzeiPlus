package moe.democyann.pixivformuzeiplus.dbUtil;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by demo on 4/3/17.
 */

public class DbHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "pixiv.db";
    public static final String TABLE_NAME_IMAGE = "image";
    public static final String TABLE_NAME_Info = "info";

    public DbHelper(Context context){
        super(context,DB_NAME,null,DB_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists " + TABLE_NAME_IMAGE + " (Id integer primary key AUTOINCREMENT, Info text)";
        db.execSQL(sql);
        sql="create table if not exists " + TABLE_NAME_Info + " ([Key] text primary key, Value text)";
        db.execSQL(sql);
        db.execSQL("insert into "+ TABLE_NAME_Info + " ([Key],Value) values('last','0')");
        db.execSQL("insert into "+ TABLE_NAME_Info + " ([Key],Value) values('rallList','')");
        db.execSQL("insert into "+ TABLE_NAME_Info + " ([Key],Value) values('commList','')");
        db.execSQL("insert into "+ TABLE_NAME_Info + " ([Key],Value) values('likeList','')");
        db.execSQL("insert into "+ TABLE_NAME_Info + " ([Key],Value) values('cookie','')");
        db.execSQL("insert into "+ TABLE_NAME_Info + " ([Key],Value) values('token','')");
        db.execSQL("insert into "+ TABLE_NAME_Info + " ([Key],Value) values('username','')");
        db.execSQL("insert into "+ TABLE_NAME_Info + " ([Key],Value) values('userid','')");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
