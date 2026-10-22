/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.client.util;

public final class IterableUtils {
    @FunctionalInterface
    public interface IntAndObjectConsumer<T> {
        void accept(final int i, final T t);
    }

    public static <T> void forEachWithIndex(final Iterable<T> iterable, final IntAndObjectConsumer<T> consumer) {
        int i = 0;
        for (final T t : iterable) {
            consumer.accept(i++, t);
        }
    }
}
