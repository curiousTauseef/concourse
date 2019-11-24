/*
 * Copyright (c) 2013-2019 Cinchapi Inc.
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
package com.cinchapi.concourse.server.ops;

import com.cinchapi.concourse.server.ConcourseServer;
import com.cinchapi.concourse.server.storage.Gatherable;
import com.cinchapi.concourse.server.storage.Memory;
import com.cinchapi.concourse.server.storage.Store;

/**
 * A {@link Strategy} is a guided plan for interacting with a {@link Store} to
 * service a {@link ConcourseServer} {@link Request}.
 *
 * @author Jeff Nelson
 */
public class Strategy {

    /**
     * The {@link Request} being serviced.
     */
    private final Request request;

    /**
     * The {@link Store} that will handle the request.
     */
    private final Store store;

    /**
     * Construct a new instance.
     * 
     * @param request
     * @param store
     */
    public Strategy(Request request, Store store) {
        this.request = request;
        this.store = store;
    }

    /**
     * Return the {@link Source} that this {@link Strategy} recommends for
     * looking up the {@code key} in {@code record}.
     * 
     * @param key
     * @param record
     * @return the {@link Source} to use for looking up {@code key} in
     *         {@code record}
     */
    public Source source(String key, long record) {
        Memory memory = store.memory();
        Source source;
        if(memory.contains(key, record)) {
            source = Source.FIELD;
        }
        else if(request.operationKeys().isEmpty()
                && !request.operation().startsWith("find")) {
            // The entire record is involved in the operation, so force the full
            // PrimaryRecord to be loaded.
            source = Source.RECORD;
        }
        else if(request.operationKeys().isEmpty()
                && request.operationRecords().isEmpty()
                && request.operationKeys().size() >= request.operationRecords()
                        .size()) {
            // TODO: calibrate...may want the Field for this...
            source = Source.RECORD;
        }
        else if(!(store instanceof Gatherable)) {
            source = memory.contains(record) ? Source.RECORD : Source.FIELD;
        }
        // NOTE: the following conditions can only occur for a Gatherable store
        else if(memory.contains(key)) {
            source = Source.INDEX;
        }
        else if(request.conditionKeys().contains(key)) {
            source = Source.INDEX;
        }
        else if(request.operationKeys().size() < request.operationRecords()
                .size()) { // TODO: calibrate?
            source = Source.INDEX;
        }
        else if(memory.contains(record)) {
            source = Source.RECORD;
        }
        else {
            source = Source.FIELD;
        }
        return source;
    }

    /**
     * The various structures where a data lookup can be performed.
     *
     * @author Jeff Nelson
     */
    public enum Source {
        RECORD, FIELD, INDEX
    }

}
