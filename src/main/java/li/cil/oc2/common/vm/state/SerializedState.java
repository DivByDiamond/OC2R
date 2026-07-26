/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.state;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.bus.adapter.RPCDeviceBusAdapter;
import li.cil.oc2.common.vm.BuiltinDevices;
import li.cil.oc2.common.vm.VMDeviceBusAdapter;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.riscv.R5Board;

@Serialized
public final class SerializedState {
    public R5Board board;
    public GlobalVMContext context;
    public BuiltinDevices builtinDevices;
    public RPCDeviceBusAdapter rpcAdapter;
    public transient VMDeviceBusAdapter vmAdapter;
}
