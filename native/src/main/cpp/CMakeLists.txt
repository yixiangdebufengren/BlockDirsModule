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
    SOURCE_SUBDIR shadowhook/src/main/cpp
)

FetchContent_MakeAvailable(shadowhook)

# shadowhook 的 CMakeLists 用了 -Weverything -Werror，在 NDK 27 的 clang 下会触发
# -Wunsafe-buffer-usage / -Wdeclaration-after-statement 等大量告警进而编译失败。
# 这里追加 -Wno-error 关闭「告警即错误」。
target_compile_options(shadowhook PRIVATE -Wno-error)

# shadowhook 的 shadowhook.map.txt 版本脚本声明了 arm32 专用的
# vfpv3d16/vfpv3d32 符号，在 arm64 上不存在，导致 ld.lld 报
# "symbol not defined"。追加 --undefined-version 让 lld 把这些
# 未定义符号当作警告而非错误。
target_link_options(shadowhook PRIVATE -Wl,--undefined-version)

add_library(blockdirs SHARED blockdirs.cpp)

# shadowhook 的头文件用 PRIVATE include dir 不会传播给 blockdirs，
# 这里显式把 include 目录加给 blockdirs。
target_include_directories(blockdirs PRIVATE
    ${shadowhook_SOURCE_DIR}/shadowhook/src/main/cpp/include)

target_link_libraries(blockdirs
    shadowhook
    log)