CC ?= riscv64-linux-gnu-gcc
CFLAGS ?= -static -Os -Wall -Wextra

all: example_redstone.elf

rpc.o: rpc.c rpc.h
	$(CC) $(CFLAGS) -c -o $@ rpc.c

example_redstone.elf: example_redstone.c rpc.o rpc.h
	$(CC) $(CFLAGS) -o $@ example_redstone.c rpc.o

clean:
	rm -f *.o *.elf

.PHONY: all clean
