package com.example

object DumperNative {
    init {
        System.loadLibrary("anonymousdumper")
    }

    external fun dumpIl2cpp(targetPackage: String): String
}
