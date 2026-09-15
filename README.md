# BlockDirs — 阻止外置存储自动创建目录

一个 LSPosed 模块，阻止手机在 OTG U 盘 / SD 卡等**外置存储卷的根目录下创建任何目录**（包括 `Music`、`Pictures`、`Movies`、`Android`、`DCIM` 等，以及任何未来的目录）。

## 背景与原理

实测（ColorOS 16 / Android 16）确认：

- 外置卷的标准媒体目录由 `com.android.providers.media.module`（MediaProvider，uid 10176）创建
- 创建发生在 **native 层**（libfuse 的 `fuse_fs_mkdir` → libc 的 `mkdir`/`mkdirat`），**完全绕过 Java 的 `File.mkdir`**

因此本模块 Hook 目标进程内 libc 的 `mkdir` / `mkdirat`：

- 只要路径是「外置卷根目录下的第一层目录」（如 `/storage/XXXX-XXXX/<任意名字>`），一律返回成功但不真正创建
- 不依赖任何硬编码目录名，**无遗漏**
- 排除 `/storage/emulated`（内部共享存储），不影响手机自带存储

## 构建（GitHub Actions）

push 到 `main`/`master` 分支或手动触发 workflow，自动构建 APK，产物在 Artifacts 中下载。

本地构建：

```bash
# 确保有 Android SDK + NDK + CMake
./gradlew assembleDebug
```

## 安装与启用

1. 安装生成的 APK
2. 在 LSPosed 中启用模块
3. 作用域勾选：**系统框架** + **媒体存储（com.android.providers.media.module）**
4. 重启手机

## 副作用说明

外置卷**根目录下**的任何第一层目录（含你手动建的）都会被拦截。如果你需要在 U 盘建目录，请建在已有的子目录内，或多级路径，不受影响。

## 目录结构

```
app/          # Xposed 模块（Java 入口）
native/       # native hook（C++，自包含 ARM64 inline hook）
```