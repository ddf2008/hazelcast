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

package com.hazelcast.partition;

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.PartitionAwareOperation;
import com.hazelcast.spi.exception.RetryableException;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @mdogan 4/11/13
 */
// runs locally
public class SyncReplicaVersion extends Operation implements PartitionAwareOperation {

    private int syncReplicaIndex;

    public SyncReplicaVersion() {
        this(1);
    }

    public SyncReplicaVersion(int syncReplicaIndex) {
        this.syncReplicaIndex = syncReplicaIndex;
    }

    public void beforeRun() throws Exception {
    }

    public void run() throws Exception {
        final PartitionServiceImpl partitionService = getService();
        final int partitionId = getPartitionId();
        final int replicaIndex = syncReplicaIndex;
        final PartitionInfo partition = partitionService.getPartitionInfo(partitionId);
        final Address target = partition.getReplicaAddress(replicaIndex);
        if (target != null) {
            final long[] currentVersions = partitionService.getPartitionReplicaVersions(partitionId);
            final NodeEngine nodeEngine = getNodeEngine();
            CheckReplicaVersion op = new CheckReplicaVersion(currentVersions[replicaIndex]);
            op.setPartitionId(partitionId).setReplicaIndex(replicaIndex).setServiceName(PartitionServiceImpl.SERVICE_NAME);
            nodeEngine.getOperationService().send(op, target);
        }
    }

    public void afterRun() throws Exception {
    }

    public boolean returnsResponse() {
        return false;
    }

    public Object getResponse() {
        return null;
    }

    public boolean validatesTarget() {
        return false;
    }

    public String getServiceName() {
        return PartitionServiceImpl.SERVICE_NAME;
    }

    public void logError(Throwable e) {
        final ILogger logger = getLogger();
        if (e instanceof RetryableException) {
            logger.log(Level.FINEST, e.getClass() + ": " + e.getMessage());
        } else {
            logger.log(Level.WARNING, e.toString());
        }
    }

    protected void writeInternal(ObjectDataOutput out) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected void readInternal(ObjectDataInput in) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SyncReplicaVersion{");
        sb.append("partitionId=").append(getPartitionId());
        sb.append(", replicaIndex=").append(syncReplicaIndex);
        sb.append('}');
        return sb.toString();
    }
}