package top.yixiangren.blockdirs;

/**
 * 模块激活状态的跨进程共享约定。
 *
 * 借鉴 YukiHookAPI 的 {@code isXposedModuleActive} 判定思路：
 * 激活信号不是"模块自己被 scope 命中"，而是"模块的 hook 代码被框架
 * 加载进 zygote 并执行"（即用户在 LSPosed 里勾选启用了本模块）。
 *
 * hook 侧（initZygote / handleLoadPackage，运行在 zygote/root 或目标进程）
 * 在框架加载本模块时，把激活标记写入模块自己的 files 目录；UI 侧
 * （MainActivity，普通 app 进程）读同一路径，根属同一 uid、免 root。
 *
 * 注意：不能用 XSharedPreferences 写（传统 de.robv API 的 edit() 是只读
 * 实现，会抛 UnsupportedOperationException），故改用文件标记。
 */
public final class ModuleStatus {

    /** 模块包名（与模块自身 data 目录路径一致） */
    public static final String PACKAGE_NAME = "top.yixiangren.blockdirs";

    /** 激活标记文件名（两端必须一致） */
    public static final String FLAG_FILE = "blockdirs_active";

    /** 模块 files 目录的绝对路径（hook 侧 root 环境可写） */
    public static final String FILES_DIR = "/data/data/" + PACKAGE_NAME + "/files";

    private ModuleStatus() {
    }
}