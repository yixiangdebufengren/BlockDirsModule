cmake_minimum_required(VERSION 4.0.2)
project(blockdirs)

set(CMAKE_CXX_STANDARD 17)

include(FetchContent)

# shadowhook：字节跳动开源 Android inline hook 库
# 库的 CMakeLists 位于仓库的 shadowhook/src/main/cpp 子目录，根目录没有，
# 所以必须用 SOURCE_SUBDIR 指向它。
FetchContent_Declare(
    shadowhook
    GIT_REPOSITORY https://github.com/bytedance/android-inline-hook.git
    GIT_TAG main
    GIT_SHALLOW TRUE
)

# 手动 Populate（而非 MakeAvailable），这样可以在 shadowhook 的
# file(GLOB *.c) 执行之前先禁用 sh_jni.c。
# CMake 4.0.2 中 FetchContent_Populate 已被弃用（策略 CMP0169），
# 这里显式允许直接调用它。
if(POLICY CMP0169)
    cmake_policy(SET CMP0169 OLD)
endif()
FetchContent_Populate(shadowhook)

# 关键修复：libshadowhook.so 导出 JNI_OnLoad，内部会 FindClass("com/bytedance/shadowhook/ShadowHook")，
# 我们 APK 中没有（zygote classloader 也找不到）该类时 FindClass 返回 null，JNI_OnLoad 返回 JNI_ERR，
# 导致 System.loadLibrary("blockdirs") 级联调用依赖库 JNI_OnLoad 时抛 UnsatisfiedLinkError。
#
# 我们根本不需要 shadowhook 的 Java 绑定，只需 native inline hook 能力。
# 所以这里在 add_subdirectory 之前禁用 sh_jni.c（重命名成非 .c 后缀），
# 让 shadowhook 的 file(GLOB *.c) 不编译它，从而彻底移除 JNI_OnLoad。
set(_SH_JNI "${shadowhook_SOURCE_DIR}/shadowhook/src/main/cpp/sh_jni.c")
if(EXISTS "${_SH_JNI}")
    file(RENAME "${_SH_JNI}" "${_SH_JNI}.disabled")
    message(STATUS "[blockdirs] disabled shadowhook sh_jni.c to remove JNI_OnLoad")
endif()

# 手动 add_subdirectory shadowhook 的源码目录
add_subdirectory(${shadowhook_SOURCE_DIR}/shadowhook/src/main/cpp shadowhook-build)

# shadowhook 的 CMakeLists 用了 -Weverything -Werror，在 NDK 27 的 clang 下会触发
# -Wunsafe-buffer-usage / -Wdeclaration-after-statement 等大量告警进而编译失败。
# 这里追加 -Wno-error 关闭「告警即错误」。
target_compile_options(shadowhook PRIVATE -Wno-error)

# shadowhook 的 shadowhook.map.txt 版本脚本声明了 arm32 专用的
# vfpv3d16/vfpv3d32 符号（以及被禁用的 JNI_OnLoad），在 arm64 上不存在
# 则导致 ld.lld 报 "symbol not defined"。追加 --undefined-version 让 lld
# 把这些未定义符号当作警告而非错误。
target_link_options(shadowhook PRIVATE -Wl,--undefined-version)

add_library(blockdirs SHARED blockdirs.cpp)

# shadowhook 的头文件用 PRIVATE include dir 不会传播给 blockdirs，
# 这里显式把 include 目录加给 blockdirs。
target_include_directories(blockdirs PRIVATE
    ${shadowhook_SOURCE_DIR}/shadowhook/src/main/cpp/include)

target_link_libraries(blockdirs
    shadowhook
    log)