package li.cil.oc2.common.item.tool;

import li.cil.oc2.common.item.ModItem;

/**
 * Fabric stand-in for the NeoForge {@code ManualItem}, which is built on the Markdown Manual mod
 * library (NeoForge only, so the in-game manual is not available on Fabric yet). The item is still
 * registered under the same id, so worlds and recipes stay compatible between the loaders.
 */
public final class ManualItem extends ModItem {
}
