/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.util;

import li.cil.oc2.manual.api.ManualModel;

/**
 * Result type for {@link MarkdownManualRegistryEntry#matches(ManualModel)} to determine if an entry is valid for a
 * {@link ManualModel}.
 */
public enum MatchResult {
    PASS,
    MATCH,
    MISMATCH
}
