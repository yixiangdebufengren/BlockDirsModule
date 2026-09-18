package top.yixiangren.blockdirs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import de.robv.android.xposed.XSharedPreferences;

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
        applyStatusBar();

        bindVersion();
        bindLinks();
        bindHideIconSwitch();
        bindActivationStatus();
    }

    /**
     * 让状态栏背景跟随动态取色的 colorSurface，并把状态栏图标/文字设为深色。
     * 避免 Material3 默认把状态栏染成偏深蓝/深色的 colorPrimary 造成的突兀感。
     */
    private void applyStatusBar() {
        Window window = getWindow();
        // 取动态取色后的 surface 色作为状态栏背景（与页面背景一致）
        // colorSurface 是 Material Components 定义的 attr，不是框架 android.R.attr
        int surface = resolveThemeColor(com.google.android.material.R.attr.colorSurface);
        window.setStatusBarColor(surface);
        // 浅色背景 -> 深色前景图标，深色背景 -> 浅色前景图标
        boolean isLight = ColorUtils.calculateLuminance(surface) > 0.5;
        int flags = window.getDecorView().getSystemUiVisibility();
        if (isLight) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private int resolveThemeColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (getTheme().resolveAttribute(attr, tv, true)) {
            return tv.data;
        }
        return Color.WHITE;
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
     * 判断模块是否激活（借鉴 YukiHookAPI 的 isXposedModuleActive 思路）。
     *
     * 激活信号 = 模块的 hook 代码被框架（LSPosed）加载进 zygote 并执行，
     * 即用户在 LSPosed 里勾选启用了本模块。hook 侧在 initZygote 里把该信号
     * 写入模块自己的 shared_prefs，这里用 XSharedPreferences 跨进程读取，
     * 免 root、不依赖模块自己被 scope 命中。
     */
    private boolean isModuleActive() {
        try {
            XSharedPreferences prefs = new XSharedPreferences(
                    getPackageName(), ModuleStatus.PREFS_NAME);
            prefs.reload();
            return prefs.getBoolean(ModuleStatus.KEY_ACTIVE, false);
        } catch (Throwable t) {
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