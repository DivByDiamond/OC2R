import java.nio.*;
import java.nio.file.*;
import li.cil.sedna.Sedna;
import li.cil.sedna.api.device.*;
import li.cil.sedna.api.devicetree.*;
import li.cil.sedna.api.memory.MemoryMap;
import java.util.Optional;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.*;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import li.cil.sedna.memory.MemoryMaps;
import li.cil.sedna.riscv.R5Board;
/** Two real RV64 guests; two VirtIO NICs each; only NIC1 is bridged. */
public final class NetworkPair {
 static final class Provider implements DeviceTreeProvider {
  final String name,compatible;Provider(String n,String c){name=n;compatible=c;}
  public Optional<String> getName(Device d){return Optional.of(name);}
  public Optional<DeviceTree> createNode(DeviceTree r,MemoryMap m,Device d,String n){return m.getMemoryRange((MemoryMappedDevice)d).map(range->r.find("/soc").getChild(name,range.address()));}
  public void visit(DeviceTree node,MemoryMap map,Device d){node.addProp("compatible",compatible);if(d instanceof UART16550A)node.addProp("clock-frequency",3686400);}
 }
 static final class Guest {
  final String name; final R5Board board=new R5Board();final UART16550A uart=new UART16550A();
  final VirtIONetworkDevice[] net={new VirtIONetworkDevice(board.getMemoryMap()),new VirtIONetworkDevice(board.getMemoryMap())};
  final StringBuilder out=new StringBuilder(); int scan; long[] frames=new long[2];
  Guest(String name,String[] args)throws Exception {
   this.name=name;board.addDevice(0x80000000L,Memory.create(64*1024*1024));
   board.addDevice(0x10000000L,uart);uart.getInterrupt().set(4,board.getInterruptController());board.setStandardOutputDevice(uart);
   var block=new VirtIOBlockDevice(board.getMemoryMap(),ByteBufferBlockDevice.wrap(ByteBuffer.wrap(Files.readAllBytes(Path.of(args[2]))),false));
   board.addDevice(0x10001000L,block);block.getInterrupt().set(7,board.getInterruptController());
   for(int i=0;i<2;i++){board.addDevice(0x10002000L+i*0x1000,net[i]);net[i].getInterrupt().set(10+i,board.getInterruptController());}
   MemoryMaps.store(board.getMemoryMap(),0x80000000L,Files.newInputStream(Path.of(args[0])));
   MemoryMaps.store(board.getMemoryMap(),0x80200000L,Files.newInputStream(Path.of(args[1])));
   board.initialize();board.setRunning(true);
  }
  void step(){board.step(2000);int c;StringBuilder fresh=new StringBuilder();while((c=uart.read())>=0){out.append((char)c);fresh.append((char)c);}if(!fresh.isEmpty())System.out.print(fresh);if(out.indexOf("PANIC")>=0||out.indexOf("KERNEL page fault")>=0)throw new AssertionError(name+" fault");}
 }
 static Guest[] guests;static long bridged, routed;
 static void gateway(Guest g,byte[] f){
  if(f.length>=42 && f[12]==8 && f[13]==6 && f[21]==1 && f[38]==10 && f[39]==0 && f[40]==2 && f[41]==2){
   byte[] r=f.clone();byte[] mac={2,0,0,0,0,2};System.arraycopy(f,6,r,0,6);System.arraycopy(mac,0,r,6,6);r[21]=2;System.arraycopy(mac,0,r,22,6);System.arraycopy(f,38,r,28,4);System.arraycopy(f,22,r,32,6);System.arraycopy(f,28,r,38,4);g.net[0].writeEthernetFrame(r);
  }
  if(f.length>=42 && f[12]==8 && f[13]==0 && f[30]==1 && f[31]==1 && f[32]==1 && f[33]==1 && new String(f,java.nio.charset.StandardCharsets.ISO_8859_1).contains("OC2R-ROUTE"))routed++;
 }
 static void step(){for(var g:guests)g.step();for(int i=0;i<2;i++){var g=guests[i];byte[] frame;while((frame=g.net[0].readEthernetFrame())!=null){g.frames[0]++;System.err.println("INET "+g.name+" "+java.util.HexFormat.of().formatHex(frame));gateway(g,frame);}while((frame=g.net[1].readEthernetFrame())!=null){g.frames[1]++;System.err.println("FRAME "+g.name+" "+java.util.HexFormat.of().formatHex(frame));guests[1-i].net[1].writeEthernetFrame(frame);bridged++;}}}
 static void waitFor(Guest g,String marker)throws Exception {long end=System.nanoTime()+180_000_000_000L;while(System.nanoTime()<end){step();if(g.out.indexOf(marker,g.scan)>=0){g.scan=g.out.length();for(int settle=0;settle<200;settle++)step();return;}}throw new AssertionError(g.name+" timeout "+marker+"\n"+g.out.substring(Math.max(0,g.out.length()-2000)));}
 static void send(Guest g,String line)throws Exception {g.scan=g.out.length();for(byte b:(line+"\r").getBytes()){while(!g.uart.canPutByte())step();g.uart.putByte(b);step();}}
 static void cmd(Guest g,String command)throws Exception{send(g,command);waitFor(g,"osh$ ");}
 public static void main(String[]args)throws Exception{
  Sedna.initialize();DeviceTreeRegistry.putProvider(UART16550A.class,new Provider("uart","ns16550a"));DeviceTreeRegistry.putProvider(AbstractVirtIODevice.class,new Provider("virtio","virtio,mmio"));
  guests=new Guest[]{new Guest("A",args),new Guest("B",args)};
  for(var g:guests){waitFor(g,"login: ");send(g,"root");waitFor(g,"password: ");send(g,"");waitFor(g,"osh$ ");}
  for(int i=0;i<2;i++){var g=guests[i];cmd(g,"run /bin/netcfg list");cmd(g,"run /bin/netcfg set 0 10.0.2.15 255.255.255.0 10.0.2.2 1.1.1.1");cmd(g,"run /bin/netcfg set 1 192.168.50."+(i+1)+" 255.255.255.0 0.0.0.0 0.0.0.0");cmd(g,"run /bin/netcfg list");}
  send(guests[0],"run /bin/udpserve 12346");waitFor(guests[0],"UDP listening;");
  send(guests[1],"run /bin/udpdemo call 192.168.50.1 12346 OC2R-LAN-UDP");waitFor(guests[1],"UDP ROUNDTRIP: ONYX-UDP-ECHO");waitFor(guests[0],"UDP SERVER RECEIVED: OC2R-LAN-UDP");
  cmd(guests[0],"echo TCP_NEXT");cmd(guests[1],"echo TCP_NEXT");
  send(guests[0],"run /bin/tcpserve 12345");waitFor(guests[0],"TCP listening;");
  send(guests[1],"run /bin/tcpdemo 192.168.50.1 12345 OC2R-LAN-TCP");waitFor(guests[1],"TCP RECEIVED: ONYX-TCP-ECHO");waitFor(guests[0],"TCP SERVER RECEIVED: OC2R-LAN-TCP");
  waitFor(guests[1],"osh$ ");
  send(guests[0],"run /bin/tcpstream serve 12348");waitFor(guests[0],"TCP_STREAM_LISTENING");
  send(guests[1],"run /bin/tcpstream recv 192.168.50.1 12348");waitFor(guests[1],"TCP_STREAM_VERIFIED_8192");
  waitFor(guests[1],"osh$ ");
  send(guests[1],"run /bin/udpdemo send 1.1.1.1 12347 OC2R-ROUTE");
  waitFor(guests[1],"UDP sent 10 bytes");
  if(routed==0)throw new AssertionError("default route did not emit external packet on NIC0");
  for(var g:guests)Files.writeString(Path.of(System.getProperty("qa.output","/tmp")+"/network-pair-"+g.name+".log"),g.out);
  System.out.println("NETWORK_PAIR_PASS routed="+routed+" bridged="+bridged+" internetFrames="+guests[0].frames[0]+","+guests[1].frames[0]);
 }
}
