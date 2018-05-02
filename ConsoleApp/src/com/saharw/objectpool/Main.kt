package com.saharw.objectpool

import com.saharw.objectpool.impl.FooObjectPool

fun main(args: Array<String>){
    var fooObjectPool = FooObjectPool(1000 * 30, HashMap(1), HashMap(1))
    var fooInstance = fooObjectPool.checkOut()
    fooInstance.`val` = 10
    println("fooInstance.val = ${fooInstance.`val`}")
    fooObjectPool.checkIn(fooInstance)
    var newFooInstance = fooObjectPool.checkOut()
    println("fooInstance.val = ${newFooInstance.`val`}")
}