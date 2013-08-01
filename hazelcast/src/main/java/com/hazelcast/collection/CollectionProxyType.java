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

/**
 * @author ali 1/2/13
 */
public enum CollectionProxyType {

    MULTI_MAP((byte) 0), LIST((byte) 1), SET((byte) 2), QUEUE((byte) 3);

    private final byte type;

    private CollectionProxyType(final byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

    public static CollectionProxyType getByType(final byte proxyType) {
        for (CollectionProxyType collectionProxyType : values()) {
            if (collectionProxyType.type == proxyType) {
                return collectionProxyType;
            }
        }
        return null;
    }
}
