package com.example.blockdirs;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

/**
 * BlockDirs 模块的主界面（作者署名页）。
 *
 * 注意：Xposed 模块的入口是 MainHook（由 assets/xposed_init 指定），
 * 本 Activity 只是提供一个可视化的「关于」页面，方便用户在桌面/应用列表中找到并查看模块信息。
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 动态显示版本号
        TextView footer = findViewById(R.id.version_footer);
        if (footer != null) {
            String version = getVersionName();
            String text = getString(R.string.version_footer).replace("{version}", version);
            footer.setText(text);
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
}
