package moe.democyann.pixivformuzeiplus.dbUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by demo on 4/3/17.
 */

public class DbUtil {
    private Context context;
    private DbHelper dbHelper;
    private SQLiteDatabase db;

    public DbUtil(Context context){
        this.context=context;
        dbHelper=new DbHelper(this.context);
    }

    /***
     * 插入图片信息数据，并返回当前已有数量
     * @param str
     * @return 当前已有数量
     */
    public int  insertImg(String str){
        int count=0;
        ContentValues values=new ContentValues();
        values.put("Info",str);
        db=dbHelper.getWritableDatabase();
//        db.beginTransaction();
        db.insert(DbHelper.TABLE_NAME_IMAGE,null,values);
//        db.rawQuery("select count(Id) from "+DbHelper.TABLE_NAME_IMAGE, new String[0]).getInt(0);
        // todo 使用count替换查询所有数据
        Cursor cursor = db.query(DbHelper.TABLE_NAME_IMAGE,new String[]{"Info"},null,null,null,null,null);
        count=cursor.getCount();
        db.close();
        return count;
//        db.setTransactionSuccessful();
    }

    /***
     * 清空图片信息数据
     */
    public void cleanDb(){
        db=dbHelper.getWritableDatabase();
//        db.beginTransaction();
        db.delete(DbHelper.TABLE_NAME_IMAGE,null,null);
        db.close();
//        db.setTransactionSuccessful();

    }


    /***
     * 获取图片信息并删除图片信息
     * @return
     */
    public String getImg(){
        String con="";
        db=dbHelper.getWritableDatabase();
        Cursor cursor = db.query(DbHelper.TABLE_NAME_IMAGE,new String[]{"Id","Info"},null,null,null,null,"Id","1");
        if(cursor.getCount()>0) {
            if (cursor.moveToFirst()) {
                con = cursor.getString(1);
            }
//        db.beginTransaction();
            db.delete(DbHelper.TABLE_NAME_IMAGE, "Id=?", new String[]{String.valueOf(cursor.getInt(0))});
        }
        db.close();
//        db.setTransactionSuccessful();
        return con;
    }

    /***
     * 设置信息
     * @param key
     * @param value
     */
    public void  setInfo(String key,String value){
        ContentValues values=new ContentValues();
        values.put("Value",value);
        db=dbHelper.getWritableDatabase();
//        db.beginTransaction();
        db.update(DbHelper.TABLE_NAME_Info,values,"Key=?",new String[]{key});
        db.close();
//        db.setTransactionSuccessful();
    }

    /***
     * 获取信息
     * @param key
     * @return
     */
    public String getInfo(String key){
        String con="";
        db=dbHelper.getWritableDatabase();
        Cursor cursor = db.query(DbHelper.TABLE_NAME_Info,new String[]{"Value"},"Key=?",new String[]{key},null,null,null);
        if(cursor.moveToFirst()){
            con=cursor.getString(0);
        }
        db.close();
        return con;
    }


}
