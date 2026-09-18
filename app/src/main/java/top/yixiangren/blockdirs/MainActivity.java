package top.yixiangren.blockdirs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.color.DynamicColors;
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
        // Material You 动态取色：跟随系统壁纸取色，新设备生效，旧设备自动降级
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        setContentView(R.layout.activity_main);

        bindVersion();
        bindLinks();
        bindHideIconSwitch();
        bindActivationStatus();
    }

    /**
     * 绑定「模块激活状态」卡片。
     * 检测 LSPosed 框架是否已注入 zygote（传统模式免 root）：
     * 若系统 classloader 能加载 XposedBridge，说明框架已激活，
     * 模块 UI 既已能启动，即为已激活状态。
     */
    private void bindActivationStatus() {
        LinearLayout card = findViewById(R.id.card_status);
        TextView title = findViewById(R.id.status_title);
        TextView desc = findViewById(R.id.status_desc);
        ImageView icon = findViewById(R.id.status_icon);
        if (card == null || title == null || desc == null || icon == null) return;

        boolean active = isModuleActive();
        if (active) {
            card.setBackgroundResource(R.drawable.card_status_green);
            icon.setImageResource(R.drawable.ic_status_ok);
            title.setText(R.string.module_status_title_active);
            desc.setText(R.string.module_status_desc_active);
        } else {
            card.setBackgroundResource(R.drawable.card_status_red);
            icon.setImageResource(R.drawable.ic_status_warn);
            title.setText(R.string.module_status_title_inactive);
            desc.setText(R.string.module_status_desc_inactive);
        }
    }

    /**
     * 判断模块是否激活（免 root，借鉴 WeKit HookStatus.isLegacyXposed）。
     * 传统模式下 LSPosed 把 XposedBridge 注入 zygote，每个 APP 进程
     * 的 system classloader 都能加载到该类，因此模块 UI 进程检测到
     * 该类存在，即代表框架已激活。
     */
    private boolean isModuleActive() {
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            cl.loadClass("de.robv.android.xposed.XposedBridge");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
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