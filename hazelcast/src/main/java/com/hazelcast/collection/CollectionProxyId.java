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

package com.hazelcast.collection;

import com.hazelcast.collection.list.ObjectListProxy;
import com.hazelcast.collection.set.ObjectSetProxy;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;

/**
 * @author mdogan 1/14/13
 */
public class CollectionProxyId implements IdentifiedDataSerializable {

    private String name;
    private String keyName;
    private CollectionProxyType type;
    private Object partitionKey;

    public CollectionProxyId() {
    }

    public static CollectionProxyId newMultiMapProxyId(String name) {
        return new CollectionProxyId(name, null, CollectionProxyType.MULTI_MAP);
    }

    public static CollectionProxyId newListProxyId(String name, Object partitionKey) {
        return new CollectionProxyId(ObjectListProxy.COLLECTION_LIST_NAME, name, CollectionProxyType.LIST, partitionKey);
    }

    public static CollectionProxyId newSetProxyId(String name, Object partitionKey) {
        return new CollectionProxyId(ObjectSetProxy.COLLECTION_SET_NAME, name, CollectionProxyType.SET, partitionKey);
    }

    private CollectionProxyId(String name, String keyName, CollectionProxyType type) {
        this.name = name;
        this.keyName = keyName;
        this.type = type;
    }

    private CollectionProxyId(String name, String keyName, CollectionProxyType type, Object partitionKey) {
        this(name, keyName, type);
        this.partitionKey = partitionKey;
    }

    public int getFactoryId() {
        return CollectionDataSerializerHook.F_ID;
    }

    public int getId() {
        return CollectionDataSerializerHook.COLLECTION_PROXY_ID;
    }

    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(name);
        out.writeByte(type.getType());
        out.writeUTF(keyName);
        out.writeObject(partitionKey);
    }

    public void readData(ObjectDataInput in) throws IOException {
        name = in.readUTF();
        type = CollectionProxyType.getByType(in.readByte());
        keyName = in.readUTF();
        partitionKey = in.readObject();
    }

    public String getName() {
        return name;
    }

    public String getKeyName() {
        return keyName;
    }

    public CollectionProxyType getType() {
        return type;
    }

    public Object getPartitionKey() {
        return partitionKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectionProxyId that = (CollectionProxyId) o;

        if (keyName != null ? !keyName.equals(that.keyName) : that.keyName != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (partitionKey != null ? !partitionKey.equals(that.partitionKey) : that.partitionKey != null) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (keyName != null ? keyName.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (partitionKey != null ? partitionKey.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CollectionProxyId");
        sb.append("{name='").append(name).append('\'');
        sb.append(", keyName='").append(keyName).append('\'');
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
