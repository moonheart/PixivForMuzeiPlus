package moe.democyann.pixivformuzeiplus.util;

import android.os.Environment;
import android.util.Log;

import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by demo on 3/31/17.
 */

public class HttpUtil {
    private HttpURLConnection conn;
    private URL url;
    private String uri;
    private InputStream inputStream;
    private OutputStream outputStream;
    private InputStreamReader inputStreamReader;
    private String restring;
    private char[] buffer;
    private Cookie cookie;

    private String TAG = "PixivHttpUtil";

    public HttpUtil(String uri, Cookie cookie) {
        this.uri = uri;
        this.cookie = cookie;
    }

    public String getData(Map<String, String> head) {
        int retry = 5;
        while (retry-- > 0) {
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Cookie", cookie.toString());
                for (String key : head.keySet()) {
                    conn.setRequestProperty(key, head.get(key));
                }
                conn.setDoInput(true);
                conn.connect();

                int status = conn.getResponseCode();

                if (status != 200) {
                    Log.i(TAG, url.toString());
                    Log.w(TAG, "Response code: " + status);
                    Log.i(TAG, "getData: " + conn.getHeaderField("location"));
                    throw new RemoteMuzeiArtSource.RetryException(new Exception("HTTP ERROR " + status));
                }
                Log.d(TAG, "Response code: " + status);

                List<String> set_cookie = conn.getHeaderFields().get("Set-Cookie");
                if (set_cookie != null) {
                    for (String str : set_cookie) {
                        String ci = str.substring(0, str.indexOf(";"));
                        cookie.add(ci);
                    }
                }
                String tempRes = "";
                try {
                    inputStream = conn.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    int read;
                    if ("gzip".equals(conn.getContentEncoding())) {
                        GZIPInputStream gzip = new GZIPInputStream(inputStream);
                        inputStreamReader = new InputStreamReader(gzip);
                    }
                    buffer = new char[1024];
                    while ((read = inputStreamReader.read(buffer)) != -1) {
                        tempRes += String.valueOf(buffer, 0, read);
                    }
                } finally {
                    try {
                        inputStream.close();
                    } catch (final IOException e) {
                        Log.e(TAG, e.toString(), e);
                        throw new RemoteMuzeiArtSource.RetryException(e);
                    }
                }
                return tempRes;
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
//                return "ERROR";
            }
        }
        return "ERROR";
    }

    public String postData(Map<String, String> head, String poststr) {
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", cookie.toString());
            for (String key : head.keySet()) {
                conn.setRequestProperty(key, head.get(key));
            }
            conn.setDoInput(true);
            conn.setDoOutput(true);
            try {
                outputStream = conn.getOutputStream();
                outputStream.write(poststr.getBytes());
            } finally {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, e.toString(), e);
                    return "ERROR";
                }
            }


            conn.connect();

            int status = conn.getResponseCode();

            if (status != 200) {
                Log.w(TAG, "Response code: " + status);
                throw new RemoteMuzeiArtSource.RetryException(new Exception("HTTP ERROR " + status));
            }
            Log.d(TAG, "Response code: " + status);

            List<String> set_cookie = conn.getHeaderFields().get("Set-Cookie");

            if (set_cookie != null) {
                for (String str : set_cookie) {
                    String ci = str.substring(0, str.indexOf(";"));
                    cookie.add(ci);
                }
            }
            Log.i(TAG, "postData: COOKIE:" + cookie.toString());

            try {
                inputStream = conn.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                int read;
                if ("gzip".equals(conn.getContentEncoding())) {
                    GZIPInputStream gzip = new GZIPInputStream(inputStream);
                    inputStreamReader = new InputStreamReader(gzip);
                }
                buffer = new char[1024];
                while ((read = inputStreamReader.read(buffer)) != -1) {
                    restring += String.valueOf(buffer, 0, read);
//                    buffer=new char[1024*10];
                }
            } finally {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    Log.e(TAG, e.toString(), e);
                    throw new RemoteMuzeiArtSource.RetryException(e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
            return "ERROR";
        }

        return restring;
    }


    public Cookie getCookie() {
        return cookie;
    }

    public boolean checkURL() {
        boolean flag = true;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString(), e);
            flag = false;
        }
        return flag;
    }


    public boolean downloadImg(String referer, String USER_AGENT, File file) {
        int retry = 5;
        while (retry-- > 0) {
            try {
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

                FileOutputStream fileStream = new FileOutputStream(file);

                String[] split = url.getFile().split("/");
                String fileName = split[split.length - 1];
                File newFile = new File(Environment.getExternalStorageDirectory() + savePath + fileName);
                if (newFile.exists()) {
                    inputStream = new FileInputStream(newFile);
                    Log.d(TAG, "target image has been downloaded before.");
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                    setDownloadImgConn(conn, USER_AGENT, referer);
                    int status = conn.getResponseCode();
                    if (status == 404) {
                        conn.disconnect();
                        url = new URL(url.toString().replace(".png", ".jpg"));
                        Log.d(TAG, "Replace PNG with JPG, " + url.toString());
                        conn = (HttpURLConnection) url.openConnection();
                        setDownloadImgConn(conn, USER_AGENT, referer);
                        status = conn.getResponseCode();
                    }

                    if (status != 200) {
                        Log.i(TAG, "downloadImg: ERROR,code" + status);
                        return false;
                    }
                    inputStream = conn.getInputStream();
                    if ("gzip".equals(conn.getContentEncoding())) {
                        GZIPInputStream gzip = new GZIPInputStream(inputStream);
                        inputStream = gzip;
                    }
                }
                try {
                    byte[] buff = new byte[1024 * 50];
                    int read;
                    while ((read = inputStream.read(buff)) > 0) {
                        fileStream.write(buff, 0, read);
                    }
                } finally {
                    fileStream.close();
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        Log.e(TAG, e.toString(), e);
                        return false;
                    }
                }
                Log.d(TAG, "downloadImg finished.");

                if (!newFile.exists()) {
                    FileOutputStream newFileStream = new FileOutputStream(newFile);
                    FileInputStream oldInputStream = new FileInputStream(file);
                    try {
                        byte[] buff = new byte[1024 * 50];
                        int read;
                        while ((read = oldInputStream.read(buff)) > 0) {
                            newFileStream.write(buff, 0, read);
                        }
                    } finally {
                        newFileStream.close();
                        oldInputStream.close();
                    }
                }
                return true;

            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
        return false;
    }

    private void setDownloadImgConn(HttpURLConnection conn, String USER_AGENT, String referer) throws IOException {
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Referer", referer);
        conn.setDoInput(true);
        conn.connect();
    }
}
