package moe.democyann.pixivformuzeiplus.util;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by demo on 3/31/17.
 */

public class Pixiv {
    private static Cookie cookie;
    private static String token;
    private static String userid;
    private String restring;

    private final String INDEX_URL = "https://www.pixiv.net";
    private final String POST_KEY_URL = "https://accounts.pixiv.net/login?lang=zh&source=pc&view_type=page&ref=wwwtop_accounts_index";
    private final String LOGIN_URL = "https://accounts.pixiv.net/api/login?lang=zh";
    private final String RECOMM_URL = "https://www.pixiv.net/rpc/recommender.php?type=illust&sample_illusts=auto&num_recommendations=500&page=discovery&tt=";
    private final String RECOMM_URL_ANDROID = "https://app-api.pixiv.net/v1/illust/recommended?filter=for_android";
    private final String ILLUST_URL = "https://www.pixiv.net/rpc/illust_list.php?verbosity=&exclude_muted_illusts=1&illust_ids=";

    private final String DETA_URL = "https://app-api.pixiv.net/v1/illust/detail?illust_id=";

    private final String RALL_URL = "https://www.pixiv.net/ranking.php?mode=daily&content=illust&p=1&format=json";

    private final String BOOK_URL = "https://app-api.pixiv.net/v1/user/bookmarks/illust?restrict=public&user_id=";
    private final String BOOK_URL_WWW = "https://www.pixiv.net/bookmark.php?id=%s&p=%s";

    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/42.0.2311.152 Safari/537.36";
    private static final String USER_AGENT_MOBILE = "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Mobile Safari/537.36";

    private Map<String, String> basepre;
    private final String TAG = "PixivUtil";
    private Pattern pattern;
    private Matcher matcher;
    private String PostKey = "";
    private String error = "0";

    public Pixiv() {
        this.cookie = new Cookie();
        basepre = new HashMap<String, String>();
        basepre.put("User-Agent", USER_AGENT);
        basepre.put("Accept-Encoding", "gzip,deflate,sdch");
        basepre.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        Log.i(TAG, "Pixiv: init");
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    /***
     * 获取错误代码
     * @return 错误代码
     */
    public String getError() {
        return error;
    }

    /***
     * 设置Cookie
     * @param cookie
     */
    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    /***
     * 获取字符串类型的Cookie
     * @return
     */
    public String getCookie() {
        return cookie.toString();
    }

    public void setToken(String token) {
        this.token = token;
    }

    /***
     * 获取POST ID 私有
     */
    private void getPostKey() {

        error = "0";

        HttpUtil postkeyurl = new HttpUtil(POST_KEY_URL, cookie);
        if (postkeyurl.checkURL()) {
            restring = postkeyurl.getData(basepre);
            if (restring.equals("ERROR")) {
                Log.e(TAG, "Post Key get Filed");
                error = "1001";
                return;
            }
            cookie = postkeyurl.getCookie();
            pattern = Pattern.compile("name=\"post_key\"\\svalue=\"([a-z0-9]{32})\"", Pattern.DOTALL);
            matcher = pattern.matcher(restring);
            if (matcher.find()) {
                PostKey = matcher.group(1);
            } else {
                Log.e(TAG, "Post Key Not Find");
                error = "1002";
                return;
            }
        } else {
            Log.e(TAG, "URL Error");
            error = "1003";
            return;
        }
    }

    /***
     * 登录 Pixiv 私有
     * @param pixiv_id 用户名/邮箱/ID
     * @param password 密码
     * @return 成功返回 OK 失败返回 ERROR
     */
    private boolean login(String pixiv_id, String password) {
        getPostKey();
        if (PostKey.equals("")) {
            return false;
        }
        HttpUtil login_url = new HttpUtil(LOGIN_URL, cookie);
        if (login_url.checkURL()) {
            basepre.put("Accept", "application/json, text/javascript, */*; q=0.01");
            restring = login_url.postData(basepre, "pixiv_id=" + pixiv_id + "&password=" + password + "&captcha=&g_recaptcha_response=&post_key="
                    + PostKey
                    + "&source=pc&ref=wwwtop_accounts_index&return_to=http://www.pixiv.net/");

            if (restring.equals("ERROR")) {
                Log.e(TAG, "Login Filed");
                error = "1004";
                return false;
            }
            cookie = login_url.getCookie();

            try {

                restring = restring.replaceFirst("null", "");
                Log.i(TAG, "======LOGIN restart:" + restring);
                JSONObject json = new JSONObject(restring);
                JSONObject obj = json.getJSONObject("body");
                if (obj.isNull("success")) {
                    Log.i(TAG, json.getString("message"));
                    error = "1005";
                    return false;
                } else {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                error = "1006";
                return false;
            }
        } else {
            Log.e(TAG, "URL Error");
            error = "1007";
            return false;
        }
    }


    /***
     * 登录并获取tooken
     * @param pixiv_id 用户名
     * @param password 密码
     * @param login 是否登录
     * @return
     */
    public String getToken(String pixiv_id, String password, boolean login) {
        boolean re;
        if (login) {
            re = login(pixiv_id, password);
        } else {
            re = true;
        }
        if (!re) {
            token = "";
            return "";
        } else {
            HttpUtil index = new HttpUtil(INDEX_URL, cookie);
            index.checkURL();
            basepre.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            restring = index.getData(basepre);
            if (restring.equals("ERROR")) {
                error = "1008";
                return "";
            }
            cookie = index.getCookie();
            pattern = Pattern.compile("pixiv.context.token\\s=\\s\"([a-z0-9]{32})\"", Pattern.DOTALL);
            matcher = pattern.matcher(restring);
            if (matcher.find()) {
                token = matcher.group(1);

            } else {
                Log.e(TAG, "Not Find Token");
                error = "1009";
                return "";
            }
            pattern = Pattern.compile("pixiv.user.id\\s=\\s\"(\\d+)\"", Pattern.DOTALL);
            matcher = pattern.matcher(restring);
            if (matcher.find()) {
                userid = matcher.group(1);
                Log.i(TAG, "USER_ID: " + userid);
            }

            return token;
        }
    }

    /***
     * 获取推荐列表
     * @return 推荐列表
     */
    public List getRcomm() {
        Log.d(TAG, "getRcomm");
        List list = new ArrayList();
        Log.i(TAG, "getRcomm: TOKEN:" + token);
        Log.i(TAG, "getRcomm: COOKIE:" + cookie);
        HttpUtil recomm = new HttpUtil(RECOMM_URL + token, cookie);
        recomm.checkURL();
        Map<String, String> recprer = basepre;
        recprer.put("Referer", "http://www.pixiv.net/recommended.php");
        recprer.put("Accept", "application/json, text/javascript, */*; q=0.01");
        restring = recomm.getData(recprer);
        if (restring.equals("ERROR")) {
            error = "1021";
            return null;
        }

        try {
            restring = restring.replaceFirst("null", "");
            Log.i(TAG, restring);
            JSONObject o = new JSONObject(restring);
            JSONArray arr = o.getJSONArray("recommendations");
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getInt(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString(), e);
            error = "1022";
            return null;
        }
        return list;
    }

    /***
     * 获取推荐列表(Android)
     * @return 推荐列表
     */
    public JSONArray getRcommAndroid() {
        Log.d(TAG, "getRcommAndroid");
        Log.i(TAG, "getRcomm: TOKEN:" + token);
        Log.i(TAG, "getRcomm: COOKIE:" + cookie);
        //// TODO: 2017/10/04 这个方法不可用，需要模拟Android的认证方式
        HttpUtil recomm = new HttpUtil(RECOMM_URL_ANDROID, cookie);
        recomm.checkURL();
        Map<String, String> recprer = basepre;
        recprer.put("Authorization", "Bearer ");
        //recprer.put("Accept", "application/json, text/javascript, */*; q=0.01");
        restring = recomm.getData(recprer);
        if (restring.equals("ERROR")) {
            error = "1021";
            return null;
        }

        try {
            Log.i(TAG, restring);
            JSONObject o = new JSONObject(restring);
            JSONArray arr = o.getJSONArray("illusts");
            return arr;
        } catch (JSONException e) {
            Log.e(TAG, e.toString(), e);
            error = "1022";
            return null;
        }
    }

    /***
     * 获取每日 TOP 50 列表
     * @return
     */
    public JSONArray getRalllist() {
        HttpUtil rall = new HttpUtil(RALL_URL, new Cookie());
        rall.checkURL();
        basepre.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        restring = rall.getData(basepre);
        if (restring.equals("ERROR")) {
            Log.e(TAG, "getRalllist: get RALL ERROR");
            error = "1023";
            return null;
        }
        JSONArray arr = null;
        restring = restring.replaceFirst("null", "");
//        Log.i(TAG, restring.substring(50000,restring.length()));
        try {
            JSONObject o = new JSONObject(restring);
            arr = o.getJSONArray("contents");
        } catch (JSONException e) {
            error = "1024";
            Log.e(TAG, e.toString(), e);
        }
        return arr;
    }


    public List getBooklist() {
        ArrayList list = new ArrayList();
        HttpUtil book;
        String tempurl = BOOK_URL + userid;

        for (int j = 0; j < 9; j++) {
            book = new HttpUtil(tempurl, cookie);
            book.checkURL();

            Map<String, String> recprer = basepre;
            recprer.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            restring = book.getData(recprer);
            if (restring.equals("ERROR")) {
                error = "1041";
                return null;
            }

            try {
                restring = restring.replaceFirst("null", "");
                Log.i(TAG, restring);
                JSONObject o = new JSONObject(restring);
                JSONArray ill = o.getJSONArray("illusts");
                for (int i = 0; i < ill.length(); i++) {
                    JSONObject t = ill.getJSONObject(i);
                    list.add(t.get("id"));
                }
                tempurl = o.getString("next_url");
                if (o.isNull("next_url")) {
                    break;
                }

            } catch (JSONException e) {
                Log.e(TAG, e.toString(), e);
                error = "1042";
                return null;
            }

        }

        return list;
    }

    /**
     * 使用网页解析收藏列表
     *
     * @return
     */
    public List getBooklistHtml(Boolean isAll, Integer maxPage) {
        ArrayList list = new ArrayList();
        HttpUtil book;
        Integer page = 1;
        while (isAll || page <= maxPage) {
            String tempurl = String.format(BOOK_URL_WWW, userid, page);
            book = new HttpUtil(tempurl, cookie);
            book.checkURL();

            Map<String, String> recprer = basepre;
            recprer.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            restring = book.getData(recprer);
            //Log.d(TAG, restring);
            // 解析网页
            Document document = Jsoup.parse(restring);
            Elements select = document.select(".image-item img[data-id]");
            Log.i(TAG, "HTML node count:" + select.size());
            if (select.size() == 0) {
                break;
            }
            for (org.jsoup.nodes.Element element : select) {
                String id = element.attr("data-id");
                list.add(Integer.parseInt(id));
            }
            page++;
        }
        return list;
    }

    public List getBooklistHtml(Boolean isAll) {
        return getBooklistHtml(isAll, 10);
    }

    public List getBooklistHtml(Integer maxPage) {
        return getBooklistHtml(false, maxPage);
    }

    public List getBooklistHtml() {
        return getBooklistHtml(false, 10);
    }

    /***
     * 获取作品信息方式1（R18作品会获取失败）
     * @param id 作品ID
     * @return
     */
    private JSONObject getIllInfo1(String id) {
        HttpUtil illust = new HttpUtil(DETA_URL + id, cookie);
        illust.checkURL();
        Map<String, String> recprer = basepre;
//        recprer.put("Referer", "http://www.pixiv.net/recommended.php");
        recprer.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
//        recprer.put("Authorization","Bearer "+token);
        restring = illust.getData(recprer);
        if (restring.equals("ERROR")) {
            error = "1031";
            return null;
        }
        try {
            restring = restring.replaceFirst("null", "");
            Log.i(TAG, restring);
            JSONObject o = new JSONObject(restring);
            return o;
        } catch (JSONException e) {
            Log.e(TAG, e.toString(), e);
            error = "1032";
            return null;
        }
    }

    /***
     * 获取作品信息方式2
     * @param id 作品ID
     * @return
     */
    private JSONObject getIllInfo2(String id) {
        Log.i(TAG, "getIllInfo: " + token);
        HttpUtil illust = new HttpUtil(ILLUST_URL + id + "&tt=" + token, cookie);
        illust.checkURL();
        Map<String, String> recprer = basepre;
        recprer.put("Referer", "http://www.pixiv.net/recommended.php");
        recprer.put("Accept", "application/json, text/javascript, */*; q=0.01");
        restring = illust.getData(recprer);
        if (restring.equals("ERROR")) {
            error = "1033";
            return null;
        }
        try {
            restring = restring.replaceFirst("null", "");
            Log.i(TAG, restring);
            JSONArray arr = new JSONArray(restring);
            return arr.getJSONObject(0);
        } catch (JSONException e) {
            Log.d(TAG, restring);
            Log.e(TAG, e.toString(), e);
            error = "1034";
            return null;
        }
    }


    private ImgInfo getInfoFromMobilePage(String id){
        HttpUtil http = new HttpUtil(String.format("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=%s",id), cookie);
        http.checkURL();
        Map<String, String> headers = basepre;
        headers.remove("User-Agent");
        headers.put("Referer", "https://www.pixiv.net/discovery");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        headers.put("User-Agent",USER_AGENT_MOBILE);

        String html = http.getData(headers);
        Document doc = Jsoup.parse(html);
        ImgInfo info = new ImgInfo();

        info.setImg_id(id);
        info.setImg_name(doc.select("span.title").text());
        info.setImg_url(doc.select(".img-box div.imgbox img").attr("src"));
        info.setTags(doc.select("div.works-info-tags .tag").eachText().toString());
        info.setR18(TagFliter.is_r18(info.getTags()));

        Pattern p = Pattern.compile("data-user-id=\"(\\d+)\"");
        Matcher  m = p.matcher(html);
        if(m.find()){
            info.setUser_id(m.group(1));
        }

        info.setUser_name(doc.select(".author .username").eq(0).text());
        info.setView(Integer.parseInt(doc.select(".activity-views strong").text()));
        info.setBookmarkCount(Integer.parseInt(doc.select(".activity-likes .bookmark-count").text()));
//        info.setPx();
        return info;
    }


    /***
     * 获取作品信息方式
     * @param id 作品ID
     * @return
     */
    public ImgInfo getIllInfo(String id) {
        ImgInfo o = getInfoFromMobilePage(id);
        return o;
    }


    /***
     * 下载图片
     * @param imgurl 图片地址
     * @param workid 作品ID
     * @param file   存储位置
     * @param x      true 进行地址转换，false 不转换
     * @return 图片文件 Uri
     */
    public Uri downloadImage(String imgurl, String workid, File file, boolean x) {
        String ref = "https://www.pixiv.net/member_illust.php?mode=big&illust_id=" + workid;

        Log.i(TAG, "downloadImage: " + imgurl);
        HttpUtil download = new HttpUtil(imgurl, null);
        download.checkURL();
        if (file.exists()) {
            Log.i(TAG, "target image has been downloaded before.");
        }else if(!download.downloadImg(ref, USER_AGENT_MOBILE, file)){
            return null;
        }
        return Uri.parse("file://" + file.getAbsolutePath());
    }

    /**
     * 获取原图URL
     * @param imgurl
     * @return
     */
    public String getOriginalUrl(String imgurl) {
        // 略缩：https://i.pximg.net/c/150x150_90/img-master/img/2016/11/05/21/07/57/59813504_p0_master1200.jpg
        // 高清：https://i.pximg.net/img-master/img/2016/11/05/21/07/57/59813504_p0_master1200.jpg
        // 原图：https://i.pximg.net/img-original/img/2016/11/05/21/07/57/59813504_p0.png
        // 或者：https://i.pximg.net/img-original/img/2010/08/30/00/32/39/12904418_p0.jpg
//        Log.d(TAG, imgurl);
        String big = Pattern.compile("/c/[0-9]+x[0-9]+/img-master").matcher(imgurl).replaceFirst("/img-original");
        big = Pattern.compile("/c/[0-9]+x[0-9]+_\\d+/img-master").matcher(big).replaceFirst("/img-original");
        big = Pattern.compile("\\_master[0-9]+\\.(jpg|png)", Pattern.CASE_INSENSITIVE).matcher(big).replaceFirst(".png");
//        Log.d(TAG, String.format("%s -> %s", imgurl, big));
        return big;
    }

    /**
     * 获取文件名
     *
     * @param imgurl
     * @return
     */
    public String getFilename(String imgurl) {
        String[] split = getOriginalUrl(imgurl).split("/");
        String fileName = split[split.length - 1];
        return fileName;
    }

}
