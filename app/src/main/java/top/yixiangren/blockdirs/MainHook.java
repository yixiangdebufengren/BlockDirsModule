package top.yixiangren.blockdirs;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.io.File;
import java.io.FileOutputStream;

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

        // 激活检测：本方法被回调，即代表模块已被框架（LSPosed）加载进 zygote，
        // 等价于"用户在 LSPosed 里勾选启用了本模块"。把该信号写入模块自己的
        // files 目录下的标记文件，供 UI 进程读取（免 root、不依赖 scope 命中）。
        writeActiveFlag();
    }

    /**
     * 在模块自己的 files 目录下写入「已激活」标记文件。
     *
     * 为什么不用 XSharedPreferences：传统 de.robv API 的 XSharedPreferences
     * 只支持读（edit() 返回只读实现，调用会抛 UnsupportedOperationException）。
     * hook 进程与 UI 进程同属一个 uid（u0_a583），共享 /data/data/<pkg>/files 目录，
     * 因此 hook 侧（root 环境）直接写文件，UI 侧读同一路径即可。
     */
    private void writeActiveFlag() {
        try {
            File dir = new File("/data/data/top.yixiangren.blockdirs/files");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File flag = new File(dir, ModuleStatus.FLAG_FILE);
            FileOutputStream fos = new FileOutputStream(flag);
            fos.write("active".getBytes("UTF-8"));
            fos.flush();
            fos.close();
        } catch (Throwable t) {
            XposedBridge.log("[BlockDirs] write active flag failed: " + t);
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        // 每次目标进程启动时也刷新激活标记，保证"重启应用即生效"（无需重启手机）。
        writeActiveFlag();

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
    }

    private static native void installNativeHook();
}