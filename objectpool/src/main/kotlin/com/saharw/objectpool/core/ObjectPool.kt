package com.saharw.objectpool.core

/**
 * Created by Sahar on 05/01/2018.
 */
abstract class ObjectPool<T>(val expirationTime : Long, lockedTableInitialSize : Int,
                             unlockedTableInitialSize : Int) {

    var lockedTable = HashMap<T, Long>(lockedTableInitialSize)
    var unlockedTable = HashMap<T, Long>(unlockedTableInitialSize)

    abstract fun create(): T
    abstract fun validate(instance : T) : Boolean
    abstract fun expire(instance: T)



    /**
     * this method handles checking out an object from the object pool,
     * and contain the internal logic of validating an object prior to returning it
     * and expiring an object if it is not valid or has expired
     */
    @Synchronized fun checkOut() : T {
        var currTimeMillis = System.currentTimeMillis()
        if(unlockedTable.isNotEmpty()){
            var entriesIterator = unlockedTable.entries.iterator()
            while(entriesIterator.hasNext()){
                var currEntry = entriesIterator.next()

                // check if currEntry's object has expired
                if(currTimeMillis - currEntry.value > expirationTime){

                    // if so - remove from poo;
                    entriesIterator.remove()
                    expire(currEntry.key)

                    // TODO: check if need to make currEntry key (object instance) null so it can be recycled via java's garbage collector
                }else { // object has not expired

                    // check if object is valid
                    if(validate(currEntry.key)){

                        // if so - remove from unlocked & insert into locked
                        entriesIterator.remove()
                        lockedTable[currEntry.key] = currTimeMillis
                        return currEntry.key
                    }else { // object has not passed validation - remove & expire
                        entriesIterator.remove()
                        expire(currEntry.key)
                    }
                }
            }
        }

        // if no objects are available - need to create new object
        var newInstance = create()
        lockedTable[newInstance] = currTimeMillis
        return newInstance
    }

    @Synchronized fun checkIn(instance: T) {
        lockedTable.remove(instance)
        unlockedTable[instance] = System.currentTimeMillis()
    }
}