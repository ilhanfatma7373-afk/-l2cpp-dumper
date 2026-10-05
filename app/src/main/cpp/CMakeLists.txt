cmake_minimum_required(VERSION 3.22.1)
project("anonymousdumper")

add_library(
    anonymousdumper
    SHARED
    dumper.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    anonymousdumper
    ${log-lib}
)
