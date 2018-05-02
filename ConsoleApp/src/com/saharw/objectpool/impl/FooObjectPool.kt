package com.saharw.objectpool.impl

import com.saharw.objectpool.core.ObjectPool

class FooObjectPool(expirationTime : Long, lockedTable : MutableMap<Foo, Long>,
                    unlockedTable : MutableMap<Foo, Long>) : ObjectPool<Foo>(expirationTime, lockedTable, unlockedTable) {
    override fun create(): Foo {
        return Foo(0)
    }

    override fun validate(instance: Foo): Boolean {
        return instance != null
    }

    override fun expire(instance: Foo) {
        instance.`val` = 0
    }
}