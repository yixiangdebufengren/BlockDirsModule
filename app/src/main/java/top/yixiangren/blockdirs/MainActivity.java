package top.yixiangren.blockdirs;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

/**
 * BlockDirs 模块主页（视觉复刻 WeKit 风格）。
 *
 * 浅绿主题 + Material 卡片布局 + 居中标题栏。
 * 顶部卡片点击跳转到博客说明页。
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindVersion();
        bindLinks();
    }

    private void bindVersion() {
        TextView titleVersion = findViewById(R.id.title_version);
        TextView footer = findViewById(R.id.version_footer);
        String version = getVersionName();
        if (titleVersion != null) {
            titleVersion.setText(getString(R.string.module_app_version_summary, version, getVersionCode()));
        }
        if (footer != null) {
            footer.setText(getString(R.string.version_footer).replace("{version}", version));
        }
    }

    private void bindLinks() {
        int[] cards = {
                R.id.card_guide,
                R.id.card_github,
                R.id.card_blog
        };
        String[] urls = {
                "https://yixiangren.top/463187456",
                "https://github.com/yixiangdebufengren/top.yixiangren.blockdirs",
                "https://yixiangren.top"
        };
        for (int i = 0; i < cards.length; i++) {
            final String url = urls[i];
            findViewById(cards[i]).setOnClickListener(v -> openUrl(url));
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    private String getVersionName() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName != null ? pi.versionName : "1.0";
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    private int getVersionCode() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 1;
        }
    }
}