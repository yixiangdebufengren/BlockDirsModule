package top.yixiangren.blockdirs;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * 阻止 MediaProvider 在 OTG/SD 卡等外置存储卷的根目录下创建任何目录。
 *
 * 实测结论（ColorOS 16 / Android 16）：
 *  目录创建者是 com.android.providers.media.module（uid 10176），
 *  创建发生在 native 层（libfuse 的 fuse_fs_mkdir -> libc 的 mkdir/mkdirat），
 *  Java 层 File.mkdir 不会被调用。
 *
 * 方案：Hook 目标进程内 libc 的 mkdir/mkdirat，
 * 只要路径是「外置卷根目录下的第一层目录」，一律返回成功但不创建。
 * 不依赖任何硬编码目录名，无遗漏。
 */
public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static volatile boolean nativeLoaded = false;

    @Override
    public void initZygote(StartupParam startupParam) {
        // 不在 zygote 阶段加载 native 库，避免每个 fork 出来的进程都带库、
        // 每条日志都刷屏。改为在目标进程（MediaProvider）里按需加载。
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        boolean isMediaProvider =
                "com.android.providers.media.module".equals(lpparam.packageName)
                        || "com.android.providers.media".equals(lpparam.packageName);

        if (!isMediaProvider) {
            return;
        }

        if (!nativeLoaded) {
            try {
                System.loadLibrary("blockdirs");
                nativeLoaded = true;
            } catch (Throwable t) {
                XposedBridge.log("[BlockDirs] load native lib in " + lpparam.packageName + " failed: " + t);
                return;
            }
        }

        XposedBridge.log("[BlockDirs] hooking " + lpparam.packageName);
        installNativeHook();

        // 写激活标记：供模块 UI（独立进程）读取，判断 hook 是否真正生效。
        // 用 XSharedPreferences + world-readable 方式，传统 API 免 root。
        try {
            XSharedPreferences prefs = new XSharedPreferences("top.yixiangren.blockdirs", "blockdirs_status");
            prefs.makeWorldReadable();
            prefs.reload();
            prefs.edit().putLong("last_hook_time", System.currentTimeMillis()).commit();
        } catch (Throwable t) {
            XposedBridge.log("[BlockDirs] write status failed: " + t);
        }
    }

    private static native void installNativeHook();
}