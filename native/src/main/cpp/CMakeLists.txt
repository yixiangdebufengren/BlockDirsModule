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

# Dobby 需要 clang 编译，关闭它内部可能开启的测试/示例构建
set(DOBBY_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(DOBBY_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(dobby)

add_library(blockdirs SHARED blockdirs.cpp)

target_include_directories(blockdirs PRIVATE ${dobby_SOURCE_DIR}/include)

find_library(log-lib log)
target_link_libraries(blockdirs
    dobby
    ${log-lib}
    dl)