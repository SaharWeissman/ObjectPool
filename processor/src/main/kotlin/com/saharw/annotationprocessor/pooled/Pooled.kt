package com.saharw.annotationprocessor.pooled

annotation class Pooled(val expirationTime: Long, val lockedInitialCap : Int = 10, val unlockedInitialCap : Int = 10)