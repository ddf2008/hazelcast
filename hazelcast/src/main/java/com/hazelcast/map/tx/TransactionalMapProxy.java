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

package com.hazelcast.map.tx;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.map.MapService;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.transaction.impl.TransactionSupport;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mdogan 2/26/13
 */
public class TransactionalMapProxy<K, V> extends TransactionalMapProxySupport implements TransactionalMap<K, V> {

    private final Map<Object, TxnValueWrapper<V>> txMap = new HashMap<Object, TxnValueWrapper<V>>();

    public TransactionalMapProxy(String name, MapService mapService, NodeEngine nodeEngine, TransactionSupport transaction) {
        super(name, mapService, nodeEngine, transaction);
    }

    public boolean containsKey(Object key) {
        checkTransactionState();
        return txMap.containsKey(key) || containsKeyInternal(getService().toData(key));
    }

    public int size() {
        checkTransactionState();
        int currentSize = sizeInternal();
        for (TxnValueWrapper wrapper : txMap.values()) {
            if (wrapper.type == TxnValueWrapper.Type.NEW) {
                currentSize++;
            } else if (wrapper.type == TxnValueWrapper.Type.REMOVED) {
                currentSize--;
            }
        }
        return currentSize;
    }

    public boolean isEmpty() {
        checkTransactionState();
        return size() == 0;
    }

    public V get(Object key) {
        checkTransactionState();
        TxnValueWrapper currentValue = txMap.get(key);
        if (currentValue != null) {
            return checkIfRemoved(currentValue);
        }
        return (V) getService().toObject(getInternal(getService().toData(key)));
    }

    private V checkIfRemoved(TxnValueWrapper<V> wrapper) {
        checkTransactionState();
        return wrapper == null || wrapper.type == TxnValueWrapper.Type.REMOVED ? null : wrapper.value;
    }

    public V put(K key, V value) {
        checkTransactionState();
        final V valueBeforeTxn = (V) getService().toObject(putInternal(getService().toData(key), getService().toData(value)));
        TxnValueWrapper<V> currentValue = txMap.get(key);
        if (value != null) {
            TxnValueWrapper<V> wrapper = valueBeforeTxn == null ? new TxnValueWrapper<V>(value, TxnValueWrapper.Type.NEW) : new TxnValueWrapper<V>(value, TxnValueWrapper.Type.UPDATED);
            txMap.put(key, wrapper);
        }
        return currentValue == null ? valueBeforeTxn : checkIfRemoved(currentValue);
    }

    public void set(K key, V value) {
        checkTransactionState();
        final Data dataBeforeTxn = putInternal(getService().toData(key), getService().toData(value));
        if (value != null) {
            TxnValueWrapper<V> wrapper = dataBeforeTxn == null ? new TxnValueWrapper<V>(value, TxnValueWrapper.Type.NEW) : new TxnValueWrapper<V>(value, TxnValueWrapper.Type.UPDATED);
            txMap.put(key, wrapper);
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        checkTransactionState();
        TxnValueWrapper<V> wrapper = txMap.get(key);
        boolean haveTxnPast = wrapper != null;
        if (haveTxnPast) {
            if (wrapper.type != TxnValueWrapper.Type.REMOVED) {
                return wrapper.value;
            }
            putInternal(getService().toData(key), getService().toData(value));
            txMap.put(key, new TxnValueWrapper<V>(value, TxnValueWrapper.Type.NEW));
            return null;
        } else {
            Data oldValue = putIfAbsentInternal(getService().toData(key), getService().toData(value));
            if (oldValue == null) {
                txMap.put(key, new TxnValueWrapper<V>(value, TxnValueWrapper.Type.NEW));
            }
            return (V) getService().toObject(oldValue);
        }
    }

    @Override
    public V replace(K key, V value) {
        checkTransactionState();
        TxnValueWrapper<V> wrapper = txMap.get(key);
        boolean haveTxnPast = wrapper != null;

        if (haveTxnPast) {
            if (wrapper.type == TxnValueWrapper.Type.REMOVED) {
                return null;
            }
            putInternal(getService().toData(key), getService().toData(value));
            txMap.put(key, new TxnValueWrapper<V>(value, TxnValueWrapper.Type.UPDATED));
            return wrapper.value;
        } else {
            Data oldValue = replaceInternal(getService().toData(key), getService().toData(value));
            if (oldValue != null) {
                txMap.put(key, new TxnValueWrapper<V>(value, TxnValueWrapper.Type.UPDATED));
            }
            return (V) getService().toObject(oldValue);
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        checkTransactionState();
        TxnValueWrapper<V> wrapper = txMap.get(key);
        boolean haveTxnPast = wrapper != null;

        if (haveTxnPast) {
            if (!wrapper.value.equals(oldValue)) {
                return false;
            }
            putInternal(getService().toData(key), getService().toData(newValue));
            txMap.put(key, new TxnValueWrapper<V>(wrapper.value, TxnValueWrapper.Type.UPDATED));
            return true;
        } else {
            boolean success = replaceIfSameInternal(getService().toData(key), getService().toData(oldValue), getService().toData(newValue));
            if (success) {
                txMap.put(key, new TxnValueWrapper<V>(newValue, TxnValueWrapper.Type.UPDATED));
            }
            return success;
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        checkTransactionState();
        TxnValueWrapper wrapper = txMap.get(key);

        if (wrapper != null && !getService().compare(name, wrapper.value, value)) {
            return false;
        }
        boolean removed = removeIfSameInternal(getService().toData(key), value);
        if (removed) {
            txMap.put(key, new TxnValueWrapper(value, TxnValueWrapper.Type.REMOVED));
        }
        return removed;
    }

    @Override
    public V remove(Object key) {
        checkTransactionState();
        final V valueBeforeTxn = (V) getService().toObject(removeInternal(getService().toData(key)));
        TxnValueWrapper<V> wrapper = null;
        if(valueBeforeTxn != null || txMap.containsKey(key) ) {
            wrapper = txMap.put(key, new TxnValueWrapper<V>(valueBeforeTxn, TxnValueWrapper.Type.REMOVED));
        }
        return wrapper == null ? valueBeforeTxn : checkIfRemoved(wrapper);
    }

    @Override
    public void delete(Object key) {
        checkTransactionState();
        Data data = removeInternal(getService().toData(key));
        if(data != null || txMap.containsKey(key) ) {
            txMap.put(key, new TxnValueWrapper(getService().toObject(data), TxnValueWrapper.Type.REMOVED));
        }
    }

    public String getId() {
        return name;
    }
}
