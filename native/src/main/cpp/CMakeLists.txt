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
# 这里追加 -Wno-error 关闭「告警即错误」，只保留真正致命的错误。
target_compile_options(shadowhook PRIVATE -Wno-error)

add_library(blockdirs SHARED blockdirs.cpp)

target_link_libraries(blockdirs
    shadowhook
    log)