package top.yixiangren.blockdirs;

/**
 * 模块激活状态的跨进程共享约定。
 *
 * 借鉴 YukiHookAPI 的 {@code isXposedModuleActive} 判定思路：
 * 激活信号不是"模块自己被 scope 命中"，而是"模块的 hook 代码被框架
 * 加载进 zygote 并执行"（即用户在 LSPosed 里勾选启用了本模块）。
 *
 * hook 侧（initZygote，运行在 zygote/root 环境）在框架加载本模块时，
 * 把激活标记写入模块自己的 shared_prefs 文件；UI 侧（MainActivity，
 * 普通 app 进程）用 XSharedPreferences 读取该标记，跨进程共享、免 root。
 */
public final class ModuleStatus {

    /** shared_prefs 文件名（两端必须一致） */
    public static final String PREFS_NAME = "blockdirs_status";

    /** 激活状态 key */
    public static final String KEY_ACTIVE = "module_active";

    private ModuleStatus() {
    }
}
