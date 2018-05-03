package com.saharw.objectpool.impl

import com.saharw.annotationprocessor.pooled.Pooled

@Pooled(1000 * 30L)
class Foo(var `val`: Int) {

    companion object {
        fun create() : Foo {
            return Foo(0)
        }

        fun validate(instance: Foo) : Boolean {
            return instance != null
        }

        fun expire(instance: Foo) {
            instance.`val` = 0
        }
    }
}