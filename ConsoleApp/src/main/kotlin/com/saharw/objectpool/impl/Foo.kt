package com.saharw.objectpool.impl

import com.saharw.annotationprocessor.pooled.Pooled

@Pooled(1000 * 30L)
class Foo(var `val`: Int) {
}