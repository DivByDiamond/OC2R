import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import li.cil.sedna.Sedna;
import li.cil.sedna.api.device.*;
import li.cil.sedna.api.devicetree.*;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.*;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import li.cil.sedna.memory.MemoryMaps;
import li.cil.sedna.riscv.R5Board;

/** Real RV64 guest and VirtIO transport fixture. The host advertises synthetic
 * probe devices; this verifies byte transport and parsing, not Minecraft robot actions. */
public final class RpcTransportProbe {
    final R5Board board = new R5Board();
    final UART16550A uart = new UART16550A();
    final VirtIOKeyboardDevice keyboard = new VirtIOKeyboardDevice(board.getMemoryMap());
    final VirtIOConsoleDevice rpc = new VirtIOConsoleDevice(board.getMemoryMap());
    final StringBuilder request = new StringBuilder();
    final java.util.ArrayDeque<Byte> replies = new java.util.ArrayDeque<>();
    int requests;
    final StringBuilder output = new StringBuilder();
    final Display first = new Display(Integer.getInteger("displayWidth",640),Integer.getInteger("displayHeight",400)), second = new Display(640,400), third = new Display(640,480);
    final ByteBufferBlockDevice disk;
    final VirtIOKeyboardDevice secondKeyboard;
    int scan; int stepSize = Integer.getInteger("stepSize", 1000); long cycles;
    boolean interactive;
    RpcTransportProbe(String[] args)throws Exception {
        Sedna.initialize();
        DeviceTreeRegistry.putProvider(UART16550A.class,new Provider("uart","ns16550a"));
        DeviceTreeRegistry.putProvider(AbstractVirtIODevice.class,new Provider("virtio","virtio,mmio"));
        DeviceTreeRegistry.putProvider(Display.class,new Provider("framebuffer","simple-framebuffer"));
        int ram = args.length>3?Integer.parseInt(args[3]):64;
        board.addDevice(0x80000000L,Memory.create(ram*1024*1024));
        board.addDevice(0x10000000L,uart);uart.getInterrupt().set(4,board.getInterruptController());board.setStandardOutputDevice(uart);
        disk=ByteBufferBlockDevice.wrap(ByteBuffer.wrap(Files.readAllBytes(Path.of(args[2]))),false);
        VirtIOBlockDevice block = new VirtIOBlockDevice(board.getMemoryMap(),disk);
        board.addDevice(0x10001000L,block);block.getInterrupt().set(7,board.getInterruptController());
        board.addDevice(0x3020d400L,keyboard);keyboard.getInterrupt().set(8,board.getInterruptController());
        secondKeyboard = new VirtIOKeyboardDevice(board.getMemoryMap());
        board.addDevice(0x3028a600L,secondKeyboard);secondKeyboard.getInterrupt().set(9,board.getInterruptController());
        // Deliberately packed and reordered: fb0 begins after a sub-page gap.
        if(!Boolean.getBoolean("noFb")){board.addDevice(0x31000000L,first);board.addDevice(0x32000000L,second);board.addDevice(0x33000000L,third);}
        MemoryMaps.store(board.getMemoryMap(),0x80000000L,Files.newInputStream(Path.of(args[0])));
        MemoryMaps.store(board.getMemoryMap(),0x80200000L,Files.newInputStream(Path.of(args[1])));
        board.addDevice(0x10003000L,rpc); rpc.getInterrupt().set(10,board.getInterruptController());
        board.initialize();board.setRunning(true);
    }
    void step() {
        board.step(stepSize); cycles += stepSize;
        int incoming;
        while((incoming=rpc.read())>=0) {
            if(incoming==0) { if(!request.isEmpty()){ respond(request.toString());request.setLength(0); } }
            else request.append((char)incoming);
        }
        for(int sent=0;sent<4096 && !replies.isEmpty() && rpc.canPutByte();sent++)rpc.putByte(replies.removeFirst());
        rpc.flush();
        int c;while((c=uart.read())>=0){System.out.write(c);output.append((char)c);}System.out.flush();
        String tail=output.substring(Math.max(0,output.length()-500));
        if(tail.contains("KERNEL page fault")||tail.contains("PANIC"))throw new AssertionError("guest fault");
    }
    void waitFor(String needle)throws Exception {
        long until=System.nanoTime()+120_000_000_000L;
        while(System.nanoTime()<until){step();if(output.indexOf(needle,scan)>=0){scan=output.length();for(int settle=0;settle<200;settle++)step();return;}}
        throw new AssertionError("timeout waiting for "+needle);
    }
    void line(String value)throws Exception {
        scan=output.length();
        byte[] bytes=(value+"\r").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for(byte b:bytes){while(!uart.canPutByte())step();uart.putByte(b);step();}
        if(interactive) {
            long until=System.nanoTime()+120_000_000_000L;
            while(output.indexOf("osh$ ",scan)<0) { if(System.nanoTime()>until)throw new AssertionError("shell command timeout: "+value);step(); }
        }
    }
    void marker(String value)throws Exception {
        long until=System.nanoTime()+120_000_000_000L;
        Pattern exact=Pattern.compile("(?m)^"+Pattern.quote(value)+"\\r?$");
        while(System.nanoTime()<until){step();if(exact.matcher(output.substring(scan)).find()){scan=output.length();for(int settle=0;settle<200;settle++)step();return;}}
        throw new AssertionError("timeout waiting for output line "+value);
    }
    void respond(String text) {
        requests++;
        var json=com.google.gson.JsonParser.parseString(text).getAsJsonObject();
        String reply;
        if(json.get("type").getAsString().equals("list")) {
            var devices=new com.google.gson.JsonArray();
            for(int i=0;i<80;i++) { var device=new com.google.gson.JsonObject();device.addProperty("deviceId",String.format("01234567-89ab-cdef-0123-%012d",i));
                var types=new com.google.gson.JsonArray();types.add(i==0?"probe":"device_"+i);device.add("typeNames",types);devices.add(device); }
            reply="{\"type\":\"list\",\"data\":"+devices+"}";
        } else reply="{\"type\":\"result\",\"data\":\"RPC_TRANSPORT_OK\"}";
        replies.add((byte)0);for(byte value:reply.getBytes(java.nio.charset.StandardCharsets.UTF_8))replies.add(value);replies.add((byte)0);
    }
    public static void main(String[] args)throws Exception {
        var t=new RpcTransportProbe(args);
        t.waitFor("login: ");t.line("root");t.waitFor("password: ");t.line("");t.waitFor("osh$ ");t.interactive=true;
        t.line("run /bin/oc2ctl list");
        if(!t.output.toString().contains("device_79"))throw new AssertionError("truncated >4KiB response");
        t.line("run /bin/oc2ctl call probe payload");
        if(!t.output.toString().contains("\"RPC_TRANSPORT_OK\""))throw new AssertionError("missing RPC result");
        if(t.requests!=3)throw new AssertionError("expected list, discovery list, invoke; got "+t.requests);
        System.err.println("SEDNA_RPC_TRANSPORT_PASS requests="+t.requests);
    }
    Display display(int index) {
        var matcher=Pattern.compile("simple-framebuffer at 0x([0-9a-fA-F]+)").matcher(output);
        for(int i=0;matcher.find();i++)if(i==index) {
            long address=Long.parseUnsignedLong(matcher.group(1),16);
            if(address==0x31000000L)return first;
            if(address==0x32000000L)return second;
            if(address==0x33000000L)return third;
        }
        throw new AssertionError("missing framebuffer index "+index);
    }
    void draw(int index,String color,int rgb)throws Exception {
        var d=display(index);d.expected=rgb;d.firstSeen=false;d.lastSeen=false;
        line("run /bin/fb-draw /dev/fb"+index+" "+color);
        marker("FB_DRAW_OK "+d.width+"x"+d.height+"x16");
        if(!d.firstSeen||!d.lastSeen)throw new AssertionError("incorrect physical first/last pixel fb"+index);
        // fb0's console can overwrite its pixels after the diagnostic exits.
        if(index!=0 && ((d.pixels.getShort(0)&65535)!=rgb || (d.pixels.getShort(d.getLength()-2)&65535)!=rgb))
            throw new AssertionError("surface corner colors differ fb"+index);
    }
    static final class Display implements MemoryMappedDevice {
        final int width,height;final ByteBuffer pixels;long writes,redWrites;int expected=-1;boolean firstSeen,lastSeen;
        Display(int w,int h){width=w;height=h;pixels=ByteBuffer.allocate(w*h*2).order(ByteOrder.LITTLE_ENDIAN);}
        public int getLength(){return pixels.capacity();}public int getSupportedSizes(){return 15;}
        public long load(int o,int s){return switch(s){case 0->pixels.get(o)&255L;case 1->pixels.getShort(o)&65535L;case 2->Integer.toUnsignedLong(pixels.getInt(o));default->pixels.getLong(o);};}
        public void store(int o,long v,int s){writes++;if((v&65535)==63488)redWrites++;
            if(s==1 && (v&65535)==expected) {if(o==0)firstSeen=true;if(o==getLength()-2)lastSeen=true;}
            switch(s){case 0->pixels.put(o,(byte)v);case 1->pixels.putShort(o,(short)v);case 2->pixels.putInt(o,(int)v);default->pixels.putLong(o,v);}}
        int nonzero(){int n=0;for(int i=0;i<pixels.capacity();i++)if(pixels.get(i)!=0)n++;return n;}
    }
    static final class Provider implements DeviceTreeProvider {
        final String name,compatible;Provider(String n,String c){name=n;compatible=c;}
        public Optional<String> getName(Device d){return Optional.of(name);}
        public Optional<DeviceTree> createNode(DeviceTree r,MemoryMap m,Device d,String n){return m.getMemoryRange((MemoryMappedDevice)d).map(range->r.find("/soc").getChild(name,range.address()));}
        public void visit(DeviceTree node,MemoryMap map,Device d){node.addProp("compatible",compatible);
            if(d instanceof UART16550A)node.addProp("clock-frequency",3686400);
            if(d instanceof Display f)node.addProp("width",f.width).addProp("height",f.height).addProp("stride",f.width*2).addProp("format","r5g6b5").addProp("status","okay");}
    }
}
