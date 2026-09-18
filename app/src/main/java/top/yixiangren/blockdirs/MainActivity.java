package top.yixiangren.blockdirs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import com.google.android.material.materialswitch.MaterialSwitch;

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
        bindHideIconSwitch();
    }

    /** 绑定「隐藏桌面图标」开关：切换 activity-alias 的 enabled 状态 */
    private void bindHideIconSwitch() {
        MaterialSwitch sw = findViewById(R.id.switch_hide_icon);
        if (sw == null) return;

        boolean hidden = isIconHidden();
        sw.setChecked(hidden);
        sw.setOnCheckedChangeListener((buttonView, isChecked) ->
                setIconHidden(isChecked));
    }

    private boolean isIconHidden() {
        ComponentName alias = new ComponentName(this, getPackageName() + ".MainActivityAlias");
        int state = getPackageManager().getComponentEnabledSetting(alias);
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
    }

    private void setIconHidden(boolean hidden) {
        ComponentName alias = new ComponentName(this, getPackageName() + ".MainActivityAlias");
        int newState = hidden
                ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        getPackageManager().setComponentEnabledSetting(
                alias, newState, PackageManager.DONT_KILL_APP);
    }

    private void bindVersion() {
        TextView footer = findViewById(R.id.version_footer);
        String version = getVersionName();
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