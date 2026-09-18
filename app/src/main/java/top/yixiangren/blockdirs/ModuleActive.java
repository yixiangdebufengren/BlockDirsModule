package top.yixiangren.blockdirs;

import androidx.annotation.Keep;

/**
 * 模块激活检测的标记类。
 *
 * 默认 {@link #isActive()} 返回 false；模块在 hook 入口点（handleLoadPackage）
 * 检测到自身包名被加载时，会 hook 本方法使其返回 true。
 *
 * 这样「激活状态」完全由「hook 是否在当前进程生效」实时决定，不落任何持久化
 * 文件，关闭模块后立即恢复为 false，无残留。
 *
 * {@code @Keep} 防止 release 混淆（minifyEnabled）时类名/方法名被改，导致 hook 不到。
 */
@Keep
public final class ModuleActive {

    private ModuleActive() {
    }

    @Keep
    public static boolean isActive() {
        return false;
    }
}