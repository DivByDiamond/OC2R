package li.cil.oc2.common.inet.protocol;

public record ICMPReply(byte type, byte code, int srcIpAddress, int dstIpAddress, byte[] payload) {}
