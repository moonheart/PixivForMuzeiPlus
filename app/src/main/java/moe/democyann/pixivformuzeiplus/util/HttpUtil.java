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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
//    OkHttpClient httpClient = new OkHttpClient();

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
            File tempFile = new File(file.getParent() + "/" + System.currentTimeMillis());
            try {

                OkHttpClient httpClient = new OkHttpClient();
                Response response = httpClient.newCall(buildRequest(url, referer)).execute();

                if(!response.isSuccessful()){
                    if(response.code()==404){
                        url = new URL(url.toString().replace(".png", ".jpg"));
                        response = httpClient.newCall(buildRequest(url, referer)).execute();
                    }
                    else if(response.code()==403){
                        Log.d(TAG, "403 Forbidden");
                        return false;
                    }
                }
                if(response.isSuccessful()){
                    FileOutputStream fileStream = new FileOutputStream(tempFile);
                    inputStream = response.body().byteStream();
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
                }

                tempFile.renameTo(file);
                Log.d(TAG, "downloadImg finished.");

                return true;

            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
                Log.w(TAG, "image download retrying.");
            }
            finally {
                if(tempFile.exists()) tempFile.delete();
            }
        }
        return false;
    }

    private Request buildRequest(URL url, String referer){
        return new Request.Builder()
                .url(url)
                .addHeader("Referer", referer)
                .get()
                .build();
    }

    private void setDownloadImgConn(HttpURLConnection conn, String USER_AGENT, String referer) throws IOException {
        conn.setReadTimeout(60000);
        conn.setConnectTimeout(60000);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
    }
}
