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

package com.hazelcast.queue.proxy;

import com.hazelcast.config.ItemListenerConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.Id;
import com.hazelcast.core.ItemListener;
import com.hazelcast.nio.ClassLoaderUtil;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.queue.*;
import com.hazelcast.spi.AbstractDistributedObject;
import com.hazelcast.spi.Invocation;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.impl.SerializableCollection;
import com.hazelcast.util.ExceptionUtil;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * User: ali
 * Date: 11/14/12
 * Time: 12:47 AM
 */
abstract class QueueProxySupport extends AbstractDistributedObject<Id, QueueService> {

    final Id id;
    final int partitionId;
    final QueueConfig config;

    QueueProxySupport(final Id id, final QueueService queueService, NodeEngine nodeEngine) {
        super(nodeEngine, queueService);
        this.id = id;
        this.partitionId = nodeEngine.getPartitionService().getPartitionId(id);
        this.config = nodeEngine.getConfig().getQueueConfig(id.getName());
        initializeListeners(nodeEngine);
    }

    private void initializeListeners(NodeEngine nodeEngine) {
        final List<ItemListenerConfig> itemListenerConfigs = config.getItemListenerConfigs();
        for (ItemListenerConfig itemListenerConfig : itemListenerConfigs) {
            ItemListener listener = itemListenerConfig.getImplementation();
            if (listener == null && itemListenerConfig.getClassName() != null) {
                try {
                    listener = ClassLoaderUtil.newInstance(nodeEngine.getConfigClassLoader(), itemListenerConfig.getClassName());
                } catch (Exception e) {
                    throw ExceptionUtil.rethrow(e);
                }
            }
            if (listener != null) {
                if (listener instanceof HazelcastInstanceAware) {
                    ((HazelcastInstanceAware) listener).setHazelcastInstance(nodeEngine.getHazelcastInstance());
                }
                addItemListener(listener, itemListenerConfig.isIncludeValue());
            }
        }
    }

    boolean offerInternal(Data data, long timeout) throws InterruptedException {
        throwExceptionIfNull(data);
        OfferOperation operation = new OfferOperation(id.getName(), timeout, data);
        final NodeEngine nodeEngine = getNodeEngine();
        try {
            Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(QueueService.SERVICE_NAME, operation, getPartitionId()).build();
            Future f = inv.invoke();
            return (Boolean) nodeEngine.toObject(f.get());
        } catch (Throwable throwable) {
            throw ExceptionUtil.rethrowAllowInterrupted(throwable);
        }
    }

    public int size() {
        SizeOperation operation = new SizeOperation(id.getName());
        return (Integer) invoke(operation);
    }

    public void clear() {
        ClearOperation operation = new ClearOperation(id.getName());
        invoke(operation);
    }

    Object peekInternal() {
        PeekOperation operation = new PeekOperation(id.getName());
        return invokeData(operation);
    }

    Object pollInternal(long timeout) throws InterruptedException {
        PollOperation operation = new PollOperation(id.getName(), timeout);
        final NodeEngine nodeEngine = getNodeEngine();
        try {
            Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(QueueService.SERVICE_NAME, operation, getPartitionId()).build();
            Future f = inv.invoke();
            return f.get();
        } catch (Throwable throwable) {
            throw ExceptionUtil.rethrowAllowInterrupted(throwable);
        }
    }

    boolean removeInternal(Data data) {
        throwExceptionIfNull(data);
        RemoveOperation operation = new RemoveOperation(id.getName(), data);
        return (Boolean) invoke(operation);
    }

    boolean containsInternal(Collection<Data> dataList) {
        ContainsOperation operation = new ContainsOperation(id.getName(), dataList);
        return (Boolean) invoke(operation);
    }

    List<Data> listInternal() {
        IteratorOperation operation = new IteratorOperation(id.getName());
        SerializableCollection collectionContainer = invoke(operation);
        return (List<Data>) collectionContainer.getCollection();
    }

    Collection<Data> drainInternal(int maxSize) {
        DrainOperation operation = new DrainOperation(id.getName(), maxSize);
        SerializableCollection collectionContainer = invoke(operation);
        return collectionContainer.getCollection();
    }

    boolean addAllInternal(Collection<Data> dataList) {
        AddAllOperation operation = new AddAllOperation(id.getName(), dataList);
        return (Boolean) invoke(operation);
    }

    boolean compareAndRemove(Collection<Data> dataList, boolean retain) {
        CompareAndRemoveOperation operation = new CompareAndRemoveOperation(id.getName(), dataList, retain);
        return (Boolean) invoke(operation);
    }

    private int getPartitionId() {
        return partitionId;
    }

    private void throwExceptionIfNull(Object o) {
        if (o == null) {
            throw new NullPointerException("Object is null");
        }
    }

    private <T> T invoke(QueueOperation operation) {
        final NodeEngine nodeEngine = getNodeEngine();
        try {
            Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(QueueService.SERVICE_NAME, operation, getPartitionId()).build();
            Future f = inv.invoke();
            return (T) nodeEngine.toObject(f.get());
        } catch (Throwable throwable) {
            throw ExceptionUtil.rethrow(throwable);
        }
    }

    private Object invokeData(QueueOperation operation) {
        final NodeEngine nodeEngine = getNodeEngine();
        try {
            Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(QueueService.SERVICE_NAME, operation, getPartitionId()).build();
            Future f = inv.invoke();
            return f.get();
        } catch (Throwable throwable) {
            throw ExceptionUtil.rethrow(throwable);
        }
    }

    public final String getServiceName() {
        return QueueService.SERVICE_NAME;
    }

    public final Id getId() {
        return id;
    }

    public final String getName() {
        return id.getName();
    }

    public String addItemListener(ItemListener listener, boolean includeValue) {
        return getService().addItemListener(id.getName(), listener, includeValue);
    }

    public boolean removeItemListener(String registrationId) {
        return getService().removeItemListener(id.getName(), registrationId);
    }
}
