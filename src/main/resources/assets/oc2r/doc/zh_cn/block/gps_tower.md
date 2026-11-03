# GPS 塔
![GPS 塔](block:oc2r:gps_tower)

GPS 塔报告底座坐标和维度。需要坚固地基及五格垂直空间，通过底座连接电脑设备总线。设备名为 `gps_tower`。

`getPosition()` 默认返回世界 X、Y、Z 数组。`getDimension()` 返回维度 ID。`isAssembled()` 检查完整性。

开启通用配置 `gameplay.relative_coordinates` 后，塔返回相对主塔的偏移。第一座塔调用 `setAsReference()`，读取 `getNetworkId()`；其他塔调用 `joinNetwork(id)` 加入同一网络。

每次无线跳跃的默认距离为 45 格，仅计算 X/Z 水平圆形范围，不限制高度，可通过 `gameplay.gps_tower_range` 修改。`hasSignal()` 检查主塔是否可达。无信号时读取坐标会报错，不会退回世界坐标。

卸载区块保留网络节点，破坏塔会移除节点。每个网络只有一座主塔；先在旧主塔调用 `clearReference()` 才能重新选择原点。更换原点后需要更新保存的路径。

此设备不定位玩家，也不为机器人添加 GPS 接收器。

[完整说明](https://wiki.loki-code.dev/en/reference/gps-networks)。
