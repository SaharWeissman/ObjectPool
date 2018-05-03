package com.saharw.objectpool

import com.saharw.objectpool.impl.PooledFoo

fun main(args: Array<String>) {
    var pooledFoo = PooledFoo()
    var foo = pooledFoo.checkOut()
    println("foo: ${foo.`val`}")
}