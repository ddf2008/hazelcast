package com.hazelcast.core;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;

public final class Id implements PartitionAware, DataSerializable {

    private String name;
    private Object partitionKey;

    public Id() {
        // we can eliminate empty ctor by making it package private if Id class is identified DS.
    }

    /**
     * Creates a new Id for a {@link DistributedObject}. It will use the name as partition-key.
     *
     * @param name the name of the DistributedObject.
     * @throws IllegalArgumentException if name is null.
     */
    public Id(String name) {
        this(name, name);
    }

    /**
     * Creates a new Id for a {@link DistributedObject}.
     *
     * @param name         the name of the DistributedObject
     * @param partitionKey the partition-key to specify the partition the DistributedObject lives on.
     * @throws IllegalArgumentException if name or partitionKey is null.
     */
    public Id(String name, Object partitionKey) {
        if (name == null) {
            throw new IllegalArgumentException("name can't be null");
        }

        if (partitionKey == null) {
            throw new IllegalArgumentException("partitionKey can't be null");
        }

        this.name = name;
        this.partitionKey = partitionKey == name ? null : partitionKey;
    }

    /**
     * Returns the name of the DistributedObject. Returned value will never be null.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the partition key to specify the partition the DistributedObject lives on. The returned value will never
     * be null.
     *
     * @return the partition key
     */
    @Override
    public Object getPartitionKey() {
        return partitionKey != null ? partitionKey : name;
    }

    @Override
    public boolean equals(Object thatObj) {
        if (this == thatObj) return true;
        if (thatObj == null || getClass() != thatObj.getClass()) return false;

        Id that = (Id) thatObj;

        if (!this.name.equals(that.name)) return false;

        if (!this.getPartitionKey().equals(that.getPartitionKey())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + getPartitionKey().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Id[" +
                "name='" + name + '\'' +
                ", partitionKey=" + getPartitionKey() +
                ']';
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(name);
        final boolean hasPK = partitionKey != null;
        out.writeBoolean(hasPK);
        if (hasPK) {
            out.writeObject(partitionKey);
        }
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        name = in.readUTF();
        if (in.readBoolean()) {
            partitionKey = in.readObject();
        }
    }
}
