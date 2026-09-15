package com.example.blockdirs;

import android.app.Activity;
import android.os.Bundle;

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
    }
}
