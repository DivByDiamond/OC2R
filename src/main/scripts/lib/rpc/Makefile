CC ?= riscv64-linux-gnu-gcc
CXX ?= riscv64-linux-gnu-g++
CFLAGS ?= -static -Os -Wall -Wextra
CXXFLAGS ?= -static -Os -Wall -Wextra -std=c++17

all: example_redstone.elf redstone_blink.elf note_block_player.elf example_raii.elf

rpc.o: rpc.c rpc.h
	$(CC) $(CFLAGS) -c -o $@ rpc.c

example_redstone.elf: example_redstone.c rpc.o rpc.h
	$(CC) $(CFLAGS) -o $@ example_redstone.c rpc.o

redstone_blink.elf: redstone_blink.c rpc.o rpc.h
	$(CC) $(CFLAGS) -o $@ redstone_blink.c rpc.o

note_block_player.elf: note_block_player.c rpc.o rpc.h
	$(CC) $(CFLAGS) -o $@ note_block_player.c rpc.o

example_raii.elf: example_raii.cpp rpc.o rpc_raii.hpp rpc.h
	$(CXX) $(CXXFLAGS) -o $@ example_raii.cpp rpc.o

clean:
	rm -f *.o *.elf

.PHONY: all clean
