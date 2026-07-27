package li.cil.oc2.common.inet;

record ICMPReply(byte type, byte code, int srcIpAddress, int dstIpAddress, byte[] payload) {}
