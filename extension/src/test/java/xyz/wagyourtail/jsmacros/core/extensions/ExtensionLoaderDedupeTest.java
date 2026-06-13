// SPDX-License-Identifier: MIT
package xyz.wagyourtail.jsmacros.core.extensions;

import xyz.wagyourtail.jsmacros.core.Core;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ExtensionLoaderDedupeTest {

    private static Extension stub(String name) {
        return new Extension() {
            @Override
            public String getExtensionName() {
                return name;
            }

            @Override
            public void init(Core<?, ?> runner) {
            }
        };
    }

    @Test
    void dedupeByName_keepsFirstOccurrencePerName() {
        Extension first = stub("a");
        Extension dupe = stub("a");
        Extension other = stub("b");

        List<Extension> result = ExtensionLoader.dedupeByName(List.of(first, dupe, other));

        assertEquals(2, result.size());
        assertSame(first, result.stream()
                .filter(e -> e.getExtensionName().equals("a"))
                .findFirst()
                .orElseThrow());
    }
}
