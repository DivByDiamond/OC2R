package li.cil.oc2.platform;

import li.cil.oc2.client.hooks.ClientProxyImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * The Fabric client proxy. Fabric models read what they need from the block view while rendering, so
 * unlike on NeoForge there is no render model data to compute; the defaults of the proxy apply.
 */
@Environment(EnvType.CLIENT)
public final class FabricClientProxy extends ClientProxyImpl {}
