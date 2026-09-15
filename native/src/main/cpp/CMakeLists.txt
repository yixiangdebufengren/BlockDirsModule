cmake_minimum_required(VERSION 3.18)
project(blockdirs)

set(CMAKE_CXX_STANDARD 17)

include(FetchContent)

FetchContent_Declare(
    dobby
    GIT_REPOSITORY https://github.com/jmpews/Dobby.git
    GIT_TAG master
    GIT_SHALLOW TRUE
)

set(DOBBY_BUILD_TEST OFF CACHE BOOL "" FORCE)
set(DOBBY_BUILD_EXAMPLE OFF CACHE BOOL "" FORCE)
set(Plugin.SymbolResolver OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(dobby)

# ---- Patch Dobby 的 Darwin 汇编语法为 Android/Linux GNU 语法 ----
# Dobby master 的 closure_bridge_arm64.asm 使用了 Apple 汇编器的
# @PAGE / @PAGEOFF 修饰符，Android NDK 的 clang(GNU as) 不认识，
# 会导致 "invalid symbol kind for ADRP relocation" 编译错误。
# 这里在 add_subdirectory 之后、真正编译之前，把源码文件原地改写。
set(_DOBBY_ASM "${dobby_SOURCE_DIR}/source/TrampolineBridge/ClosureTrampolineBridge/arm64/closure_bridge_arm64.asm")
if(EXISTS "${_DOBBY_ASM}")
    file(READ "${_DOBBY_ASM}" _asm_content)
    # 注意顺序：先替换 @PAGEOFF（含 @PAGE 前缀），避免 @PAGE 误匹配
    # add reg, reg, sym@PAGEOFF  ->  add reg, reg, :lo12:sym
    string(REPLACE "@PAGEOFF" ":lo12:" _asm_content "${_asm_content}")
    # adrp reg, sym@PAGE  ->  adrp reg, sym
    string(REPLACE "@PAGE" "" _asm_content "${_asm_content}")
    file(WRITE "${_DOBBY_ASM}" "${_asm_content}")
    message(STATUS "[blockdirs] patched Dobby closure_bridge_arm64.asm for Android GNU as")
endif()

add_library(blockdirs SHARED blockdirs.cpp)

target_link_libraries(blockdirs
    dobby_static
    log
    dl)