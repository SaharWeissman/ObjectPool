package com.saharw.objectpool.impl

import com.saharw.annotationprocessor.pooled.Pooled

@Pooled(1000 * 10, 100 , 100)
class Goo(var `val`: Float, var name: String?) {


    companion object {
        fun create() : Goo {
            return Goo(0f, "")
        }

        fun validate(instance: Goo) : Boolean {
            return instance.name != null
        }

        fun expire(instance: Goo) {
            instance.`val` = 0f
            instance.name = ""
        }
    }
}