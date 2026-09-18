package top.yixiangren.blockdirs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 检查 GitHub Releases 是否有新版本。
 *
 * 版本号约定：release 的 tag_name 形如 "92-git+03d914ec"，前缀数字即
 * versionCode（git rev-list --count），与本机 versionCode 直接比较大小。
 */
public final class UpdateChecker {

    /** GitHub Releases 最新版 API */
    private static final String LATEST_API =
            "https://api.github.com/repos/yixiangdebufengren/top.yixiangren.blockdirs/releases/latest";

    private static final String PREFS = "update_check";
    private static final String KEY_IGNORED = "ignored_version_code";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** 检测回调。 */
    public interface Callback {
        /**
         * @param info 有可用更新时非 null；无更新或失败时为 null
         * @param error 是否因网络/解析失败导致无法判断
         */
        void onResult(UpdateInfo info, boolean error);
    }

    public static final class UpdateInfo {
        public final int versionCode;
        public final String versionName;
        public final String releaseUrl;
        public final String downloadUrl;

        UpdateInfo(int versionCode, String versionName, String releaseUrl, String downloadUrl) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.releaseUrl = releaseUrl;
            this.downloadUrl = downloadUrl;
        }
    }

    private UpdateChecker() {
    }

    /** 异步检查更新，忽略已标记"忽略本次"的版本。 */
    public static void check(Context context, int currentVersionCode, Callback callback) {
        executor.execute(() -> {
            UpdateInfo info = fetchLatest();
            mainHandler.post(() -> {
                // 拉取失败（网络/解析异常），无法判断有无更新
                if (info == null) {
                    callback.onResult(null, true);
                    return;
                }
                // 无更新（远端 <= 本机），或已被忽略
                if (info.versionCode <= currentVersionCode || isIgnored(context, info.versionCode)) {
                    callback.onResult(null, false);
                    return;
                }
                callback.onResult(info, false);
            });
        });
    }

    /** 拉取 latest release，解析出 versionCode / versionName / url。失败返回 null。 */
    private static UpdateInfo fetchLatest() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(LATEST_API);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "BlockDirs");

            int code = conn.getResponseCode();
            if (code != 200) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONObject json = new JSONObject(sb.toString());
            String tag = json.optString("tag_name", "");
            String htmlUrl = json.optString("html_url", "");

            int versionCode = parseVersionCode(tag);
            if (versionCode <= 0) {
                return null;
            }

            String downloadUrl = "";
            org.json.JSONArray assets = json.optJSONArray("assets");
            if (assets != null && assets.length() > 0) {
                downloadUrl = assets.optJSONObject(0).optString("browser_download_url", "");
            }

            return new UpdateInfo(versionCode, tag, htmlUrl, downloadUrl);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** 从 "92-git+03d914ec" 解析出 92。 */
    private static int parseVersionCode(String tag) {
        if (tag == null || tag.isEmpty()) {
            return 0;
        }
        int i = 0;
        while (i < tag.length() && Character.isDigit(tag.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(tag.substring(0, i));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 是否已忽略该版本。 */
    private static boolean isIgnored(Context context, int versionCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_IGNORED, -1) == versionCode;
    }

    /** 标记"忽略本次"：记住该版本号，之后不再提醒。 */
    public static void ignoreVersion(Context context, int versionCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_IGNORED, versionCode).apply();
    }
}