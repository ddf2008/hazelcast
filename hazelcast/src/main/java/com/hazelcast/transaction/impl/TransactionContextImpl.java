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

package com.hazelcast.transaction.impl;

import com.hazelcast.collection.CollectionProxyId;
import com.hazelcast.collection.CollectionService;
import com.hazelcast.core.*;
import com.hazelcast.map.MapService;
import com.hazelcast.queue.QueueService;
import com.hazelcast.spi.TransactionalService;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.hazelcast.transaction.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mdogan 2/26/13
 */
final class TransactionContextImpl implements TransactionContext {

    private final NodeEngineImpl nodeEngine;
    private final TransactionImpl transaction;
    private final Map<TransactionalObjectKey, TransactionalObject> txnObjectMap = new HashMap<TransactionalObjectKey, TransactionalObject>(2);

    TransactionContextImpl(TransactionManagerServiceImpl transactionManagerService, NodeEngineImpl nodeEngine,
                           TransactionOptions options, String ownerUuid) {
        this.nodeEngine = nodeEngine;
        this.transaction = new TransactionImpl(transactionManagerService, nodeEngine, options, ownerUuid);
    }

    public String getTxnId() {
        return transaction.getTxnId();
    }

    public void beginTransaction() {
        transaction.begin();
    }

    public void commitTransaction() throws TransactionException {
        if (transaction.getTransactionType().equals(TransactionOptions.TransactionType.TWO_PHASE)) {
            transaction.prepare();
        }
        transaction.commit();
    }

    public void rollbackTransaction() {
        transaction.rollback();
    }

    @SuppressWarnings("unchecked")
    public <K, V> TransactionalMap<K, V> getMap(String name) {
        if (name == null) {
            throw new NullPointerException("Retrieving a map instance with a null name is not allowed!");
        }
        return (TransactionalMap<K, V>) getTransactionalObject(MapService.SERVICE_NAME, name);
    }

    public <E> TransactionalQueue<E> getQueue(String name) {
        if (name == null) {
            throw new NullPointerException("Retrieving a queue instance with a null name is not allowed!");
        }
        return getQueue(new Id(name));
    }

    @SuppressWarnings("unchecked")
    public <E> TransactionalQueue<E> getQueue(Id id) {
        if (id == null) {
            throw new NullPointerException("Retrieving a queue instance with a null id is not allowed!");
        }
        return (TransactionalQueue<E>) getTransactionalObject(QueueService.SERVICE_NAME, id);
    }

    @SuppressWarnings("unchecked")
    public <K, V> TransactionalMultiMap<K, V> getMultiMap(String name) {
        if (name == null) {
            throw new NullPointerException("Retrieving a multi-map instance with a null name is not allowed!");
        }
        return (TransactionalMultiMap<K, V>) getTransactionalObject(CollectionService.SERVICE_NAME, CollectionProxyId.newMultiMapProxyId(name));
    }

    public <E> TransactionalList<E> getList(String name) {
        if (name == null) {
            throw new NullPointerException("Retrieving a list instance with a null name is not allowed!");
        }
        return getList(new Id(name));
    }

    @SuppressWarnings("unchecked")
    public <E> TransactionalList<E> getList(Id id) {
        if (id == null) {
            throw new NullPointerException("Retrieving a list instance with a null id is not allowed!");
        }
        return (TransactionalList<E>) getTransactionalObject(CollectionService.SERVICE_NAME, CollectionProxyId.newListProxyId(id.getName(), id.getPartitionKey()));
    }

    public <E> TransactionalSet<E> getSet(String name) {
        if (name == null) {
            throw new NullPointerException("Retrieving a set instance with a null name is not allowed!");
        }
        return getSet(new Id(name));
    }

    @SuppressWarnings("unchecked")
    public <E> TransactionalSet<E> getSet(Id id) {
        if (id == null) {
            throw new NullPointerException("Retrieving a set instance with a null id is not allowed!");
        }
        return (TransactionalSet<E>) getTransactionalObject(CollectionService.SERVICE_NAME, CollectionProxyId.newSetProxyId(id.getName(), id.getPartitionKey()));
    }

    @SuppressWarnings("unchecked")
    public TransactionalObject getTransactionalObject(String serviceName, Object id) {
        if (serviceName == null) {
            throw new NullPointerException("Retrieving a transactional object with a null service name is not allowed!");
        }
        if (id == null) {
            throw new NullPointerException("Retrieving a transactional object with a null id is not allowed!");
        }
        if (transaction.getState() != Transaction.State.ACTIVE) {
            throw new TransactionNotActiveException("No transaction is found while accessing " +
                    "transactional object -> " + serviceName + "[" + id + "]!");
        }
        TransactionalObjectKey key = new TransactionalObjectKey(serviceName, id);
        TransactionalObject obj = txnObjectMap.get(key);
        if (obj == null) {
            final Object service = nodeEngine.getService(serviceName);
            if (service instanceof TransactionalService) {
                obj = ((TransactionalService) service).createTransactionalObject(id, transaction);
                txnObjectMap.put(key, obj);
            } else {
                throw new IllegalArgumentException("Service[" + serviceName + "] is not transactional!");
            }
        }
        return obj;

    }

    Transaction getTransaction() {
        return transaction;
    }

    private class TransactionalObjectKey {

        private final String serviceName;
        private final Object id;

        TransactionalObjectKey(String serviceName, Object id) {
            this.serviceName = serviceName;
            this.id = id;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TransactionalObjectKey)) return false;

            TransactionalObjectKey that = (TransactionalObjectKey) o;

            if (!id.equals(that.id)) return false;
            if (!serviceName.equals(that.serviceName)) return false;

            return true;
        }

        public int hashCode() {
            int result = serviceName.hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }
    }
}
