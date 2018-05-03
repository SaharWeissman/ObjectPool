package com.saharw.annotationprocessor.pooled

annotation class Pooled(val expirationTime: Long, val lockedInitialCap : Int = 25, val unlockedInitialCap : Int = 25)