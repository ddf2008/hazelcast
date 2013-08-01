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

package com.hazelcast.concurrent.countdownlatch;

import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.Id;
import com.hazelcast.spi.AbstractDistributedObject;
import com.hazelcast.spi.Invocation;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.util.ExceptionUtil;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author mdogan 1/10/13
 */
public class CountDownLatchProxy extends AbstractDistributedObject<Id, CountDownLatchService> implements ICountDownLatch {

    private final Id id;
    private final int partitionId;

    public CountDownLatchProxy(Id id, NodeEngine nodeEngine) {
        super(nodeEngine, null);
        this.id = id;
        partitionId = nodeEngine.getPartitionService().getPartitionId(nodeEngine.toData(id));
    }

    public String getName() {
        return id.getName();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        final NodeEngine nodeEngine = getNodeEngine();
        Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(CountDownLatchService.SERVICE_NAME,
                new AwaitOperation(id.getName(), getTimeInMillis(timeout, unit)), partitionId).build();
        try {
            return (Boolean) nodeEngine.toObject(inv.invoke().get());
        } catch (ExecutionException e) {
            throw ExceptionUtil.rethrowAllowInterrupted(e);
        }
    }

    private long getTimeInMillis(final long time, final TimeUnit timeunit) {
        return timeunit != null ? timeunit.toMillis(time) : time;
    }

    public void countDown() {
        final NodeEngine nodeEngine = getNodeEngine();
        Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(CountDownLatchService.SERVICE_NAME,
                new CountDownOperation(id.getName()), partitionId).build();
        try {
            inv.invoke().get();
        } catch (Exception e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

    public int getCount() {
        final NodeEngine nodeEngine = getNodeEngine();
        Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(CountDownLatchService.SERVICE_NAME,
                new GetCountOperation(id.getName()), partitionId).build();
        try {
            return (Integer) nodeEngine.toObject(inv.invoke().get());
        } catch (Exception e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

    public boolean trySetCount(int count) {
        final NodeEngine nodeEngine = getNodeEngine();
        Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(CountDownLatchService.SERVICE_NAME,
                new SetCountOperation(id.getName(), count), partitionId).build();
        try {
            return (Boolean) nodeEngine.toObject(inv.invoke().get());
        } catch (Exception e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

    public String getServiceName() {
        return CountDownLatchService.SERVICE_NAME;
    }

    public Id getId() {
        return id;
    }
}
