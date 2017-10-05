package moe.democyann.pixivformuzeiplus;


import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import moe.democyann.pixivformuzeiplus.dbUtil.DbUtil;
import moe.democyann.pixivformuzeiplus.util.ConfigManger;
import moe.democyann.pixivformuzeiplus.util.PixivLike;
import moe.democyann.pixivformuzeiplus.util.PixivTop50;
import moe.democyann.pixivformuzeiplus.util.PixivUser;


/**
 * Created by demo on 4/3/17.
 * Pixiv Source for Muzei
 * Auth:Democyann
 * email:support@democyann.moe
 * github:@democyann
 * blog:https://democyann.moe
 */

public class PixivSource extends RemoteMuzeiArtSource {
    private static final String TAG = "PixivSource";
    private static final String SOURCE_NAME = "PixivSource";
    private static final int MINUTE = 60 * 1000;

    private static boolean loadflag = false;
    private static String error = "";


    private static PixivTop50 pixivtop;    //Top50类
    private static PixivUser pixivUser;    //Pixiv用户推荐类
    private static PixivLike pixivLike;    //Pixiv收藏夹

    private static ConfigManger conf;      //设置管理器
    private DbUtil db;                     //数据库辅助类


    private int cont = 0;
    private int reason = 0;
    /**
     * 上次更新时间
     */
    private long lasttime = 0;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            loadflag = true;

            Artwork a = null;
            while (true) {

                error = "";

                int method = conf.getMethod();
                //每日TOP50模式
                if (method == 0) {
                    try {
                        a = pixivtop.getArtwork();
                    } catch (Exception e) {
                        error = pixivtop.getError();
                        e.printStackTrace();
                    }
                } else if (method == 1) {
                    //用户推荐模式
                    try {
                        a = pixivUser.getArtwork();
                    } catch (Exception e) {
                        Log.i(TAG, "run: ERROR get User ArtWork");
                        error = pixivUser.getError();
                        Log.i(TAG, "run: ERROR !" + error);
                        e.printStackTrace();
                        try {
                            a = pixivtop.getArtwork();
                        } catch (Exception er) {
                            er.printStackTrace();
                            error += "," + pixivtop.getError();
                        }
                    }

                } else {
                    //收藏夹模式
                    try {
                        a = pixivLike.getArtwork();
                    } catch (Exception e) {
                        Log.i(TAG, "run: ERROR get Like ArtWork");
                        error = pixivLike.getError();
                        Log.i(TAG, "run: ERROR !" + error);
                        e.printStackTrace();
                        try {
                            a = pixivtop.getArtwork();
                        } catch (Exception er) {
                            er.printStackTrace();
                            error += "," + pixivtop.getError();
                        }
                    }
                }
                int i = 0;
                if (a != null) {
                    try {
                        i = db.insertImg(a.toJson().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Log.i(TAG, "run: ERROR" + error);
                if (!"".equals(error)) {
                    if (error.equals("1100")) error = getString(R.string.u_err);
                    if (error.equals("1005")) error = getString(R.string.login_failed);
                    if (error.equals("2001")) error = getString(R.string.permission);
                    cont++;
                    Artwork t = PixivSource.this.getCurrentArtwork();
                    Artwork p = new Artwork.Builder()
                            .title(t.getTitle())
                            .byline(t.getByline() + "\nERROR:" + error)
                            .imageUri(t.getImageUri())
                            .viewIntent(t.getViewIntent())
                            .token(t.getToken())
                            .build();
                    publishArtwork(p);
                    break;
                } else {
                    cont = 0;
                }
                if (i > 5) break;
            }
            loadflag = false;
        }
    };


    public PixivSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
        conf = new ConfigManger(this);
        pixivtop = new PixivTop50(this, getDir());
        pixivUser = new PixivUser(this, getDir());
        pixivLike = new PixivLike(this, getDir());
        db = new DbUtil(this);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        this.reason = reason;
        Log.i(TAG, "onTryUpdate: ===== info:" + reason);

        if (System.currentTimeMillis() - lasttime < 1000) {
            scheduleUpdate();
            return;
        }

        if (reason == UPDATE_REASON_SCHEDULED && conf.getChangeInterval() == 0) {
            scheduleUpdate(0);
            Log.i(TAG, "onTryUpdate: STOP Update");
            return;
        }

        lasttime = System.currentTimeMillis();

        if (!isEnabledWifi() && conf.isOnlyUpdateOnWifi()) {
            scheduleUpdate();
            return;
        }
        if (cont >= 6) {
            scheduleUpdate(0);
        }

        String[] arr = {"KP", "PRK", "408", "KR", "KOR", "410", "ko", "kor"};
        String ct = Locale.getDefault().getCountry();
        String lg = Locale.getDefault().getLanguage();
        for (String te : arr) {
            if (ct.equals(te) || lg.equals(te))
                return;
        }

        Artwork last = getCurrentArtwork();
        Artwork artwork = last;

        String json = db.getImg();
        JSONObject o = null;
        try {
            o = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (o != null) {
            try {
                artwork = Artwork.fromJson(o);
            } catch (JSONException e) {
                e.printStackTrace();

            }
        }

        //进程未启动时则获取新图片
        if (!loadflag) {
            Thread t = new Thread(runnable);
            Log.i(TAG, "onTryUpdate: Thread Start");
            Log.i(TAG, "onTryUpdate: ==========METHOD======" + conf.getMethod());
            t.start();
        }

        //未找到文件则2秒后重新获取下一张图片
        if (cont < 6) {
            if (artwork != null) {
                File test = new File(getDir(), artwork.getToken());
                if (!test.exists()) {
                    Log.i(TAG, "onTryUpdate: No Find File");
                    scheduleUpdate(System.currentTimeMillis() + 2 * 1000);
                    cont++;
                    return;
                }
            } else {
                scheduleUpdate(System.currentTimeMillis() + 5 * 1000);
                cont++;
                return;
            }
        } else {
            scheduleUpdate(0);
            return;
        }

        Log.i(TAG, "onTryUpdate: AWRK URI:" + artwork.getImageUri().toString());
        //分享文件前进行授权
        grantUriPermission("net.nurik.roman.muzei", artwork.getImageUri(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        //推送图片
        publishArtwork(artwork);

        //清理无用缓存
//        if (!artwork.equals(last)) {
//            try {
//                File f = new File(getDir(), last.getToken());
//                f.delete();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }

        scheduleUpdate();
    }

    private File getDir() {

        String savePath = "/pixiv/";
                // 创建外置缓存文件夹
                File pPath = new File(Environment.getExternalStorageDirectory() + savePath);
                if (pPath.exists()) {
                    if (pPath.isFile()) {
                        pPath.delete();
                        pPath.mkdir();
                    }
                } else {
                    pPath.mkdir();
                }
                return pPath;
//        Application app = getApplication();
//        if (app.getExternalCacheDir() == null) {
//            return app.getCacheDir();
//        } else {
//            return app.getExternalCacheDir();
//        }
    }


    /***
     * 获取是否开启了 Wifi 网络
     * @return
     */
    private boolean isEnabledWifi() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi.isConnected();
    }


    /***
     * 设置默认更新时间
     */

    private void scheduleUpdate() {
        int changeInterval = conf.getChangeInterval();
        if (changeInterval > 0) {
            scheduleUpdate(System.currentTimeMillis() + changeInterval * MINUTE);
        }
    }
}
