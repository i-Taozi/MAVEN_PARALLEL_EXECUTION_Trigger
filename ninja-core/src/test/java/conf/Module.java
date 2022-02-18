/**
 * Copyright (C) the original author or authors.
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

package conf;

import com.google.inject.AbstractModule;

import ninja.utils.NinjaProperties;

// Just a dummy for testing.
// Allows to check that custom Ninja module in user's conf directory
// works properly.
public class Module extends AbstractModule {

    NinjaProperties ninjaProperties;

    public Module(NinjaProperties ninjaProperties) {

        if (ninjaProperties == null) {
            throw new IllegalArgumentException("Received null as an instance of NinjaProperties");
        }

        this.ninjaProperties = ninjaProperties;
    }

    @Override
    protected void configure() {

        bind(DummyInterfaceForTesting.class).to(DummyClassForTesting.class);

    }

    public static interface DummyInterfaceForTesting {
    }

    public static class DummyClassForTesting implements DummyInterfaceForTesting {
    }

}
