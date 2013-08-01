/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.concurrent.lock.proxy;

import com.hazelcast.concurrent.lock.InternalLockNamespace;
import com.hazelcast.concurrent.lock.LockService;
import com.hazelcast.concurrent.lock.LockServiceImpl;
import com.hazelcast.core.ICondition;
import com.hazelcast.core.ILock;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.AbstractDistributedObject;
import com.hazelcast.spi.NodeEngine;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author mdogan 2/12/13
 */
public class LockProxy extends AbstractDistributedObject<Object, LockServiceImpl> implements ILock {

    final Object key;
    final Data keyData;
    final InternalLockNamespace namespace = new InternalLockNamespace();
    private final LockProxySupport lockSupport;

    public LockProxy(NodeEngine nodeEngine, LockServiceImpl lockService, Data keyData) {
        super(nodeEngine, lockService);
        this.keyData = keyData;
        this.key = nodeEngine.toObject(keyData);
        lockSupport = new LockProxySupport(namespace);
    }

    public boolean isLocked() {
        return lockSupport.isLocked(getNodeEngine(), keyData);
    }

    public boolean isLockedByCurrentThread() {
        return lockSupport.isLockedByCurrentThread(getNodeEngine(), keyData);
    }

    public int getLockCount() {
        return lockSupport.getLockCount(getNodeEngine(), keyData);
    }

    public long getRemainingLeaseTime() {
        return lockSupport.getRemainingLeaseTime(getNodeEngine(), keyData);
    }

    public void lock() {
        lockSupport.lock(getNodeEngine(), keyData);
    }

    public void lock(long ttl, TimeUnit timeUnit) {
        lockSupport.lock(getNodeEngine(), keyData, timeUnit.toMillis(ttl));
    }

    public void lockInterruptibly() throws InterruptedException {
        lock();
    }

    public boolean tryLock() {
        return lockSupport.tryLock(getNodeEngine(), keyData);
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return lockSupport.tryLock(getNodeEngine(), keyData, time, unit);
    }

    public void unlock() {
        lockSupport.unlock(getNodeEngine(), keyData);
    }

    public void forceUnlock() {
        lockSupport.forceUnlock(getNodeEngine(), keyData);
    }

    public Condition newCondition() {
        throw new UnsupportedOperationException("Use ICondition.newCondition(String name) instead!");
    }

    public ICondition newCondition(String name) {
        return new ConditionImpl(this, name);
    }

    public Object getId() {
        return keyData;
    }

    public String getName() {
        return String.valueOf(key);
    }

    public String getServiceName() {
        return LockService.SERVICE_NAME;
    }

    public Object getKey() {
        return key;
    }
}
