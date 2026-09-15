cmake_minimum_required(VERSION 3.18)
project(blockdirs)

set(CMAKE_CXX_STANDARD 17)

# 通过 FetchContent 自动拉取 Dobby（工业级 ARM64 inline hook 库）
include(FetchContent)

FetchContent_Declare(
    dobby
    GIT_REPOSITORY https://github.com/jmpews/Dobby.git
    GIT_TAG master
    GIT_SHALLOW TRUE
)

# 关闭 Dobby 内部的测试/示例构建（正确选项名是单数）
set(DOBBY_BUILD_TEST OFF CACHE BOOL "" FORCE)
set(DOBBY_BUILD_EXAMPLE OFF CACHE BOOL "" FORCE)
# 关闭 SymbolResolver 子插件，避免额外依赖
set(Plugin.SymbolResolver OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(dobby)

add_library(blockdirs SHARED blockdirs.cpp)

# Dobby 的 target 名是 dobby_static（STATIC 库，OUTPUT_NAME 为 dobby）
target_link_libraries(blockdirs
    dobby_static
    log
    dl)