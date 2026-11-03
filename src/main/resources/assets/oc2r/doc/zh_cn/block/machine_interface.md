# 机器接口
![机器接口](block:oc2r:machine_interface)

机器接口控制连接面前方的设备，通过设备总线连接电脑。设备名为 `machine_interface`。

界面有 18 个普通缓冲槽，编号 0–17。`getTargetBlock()`、`getTargetSlotCount()` 和 `getTargetItem(slot)` 检查目标。`pushItems(from, to, amount)` 向目标发送物品，`pullItems(from, to, amount)` 取回物品，返回实际数量。

内部槽 18 保留未能退回的物品，先用 `recoverItems()` 恢复，再继续传输。能量和流体分别使用 `pullEnergy`、`pushEnergy`、`pullFluid`、`pushFluid`。目标必须提供相应的侧面接口。

支持的 Create 机器提供 `getSettings()` 和 `setSetting(id, value)`。不支持的操作会报错。

[方法说明](https://wiki.loki-code.dev/en/block/machine_interface)。
