/**
 * Copyright 2019 Pramati Prism, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hyscale.commons.framework.events.model;

import java.io.Serializable;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

/**
 * Class to define Generic events
 * All events which uses Generic type should extend this class
 * Ensures Generic events are resolved at runtime
 *
 */
public abstract class HyscaleGenericEvent<T extends Serializable> extends HyscaleEvent
        implements ResolvableTypeProvider {

    private T message;

    public HyscaleGenericEvent(T message) {
        super(message);
        this.message = message;
    }

    public HyscaleGenericEvent(Object source, T message) {
        super(source);
        this.message = message;
    }

    public T getMessage() {
        return message;
    }

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(message));
    }

}
