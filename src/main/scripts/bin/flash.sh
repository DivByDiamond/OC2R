#!/bin/sh

if [ "$1" = "" ] || [ "$2" = "" ]; then
    printf "Usage:\nflash.sh <path to flash mem> <path to firmware> <OpenSBI (yes/no default: no)>\n"
    exit 1
fi

if [ "$3" = "" ] || [ "$3" = "no" ]; then
    echo "Flashing without OpenSBI"
    usesbi=no
else
    echo "Flashing with OpenSBI"
    usesbi=yes
fi

if [ ! -e "$1" ]; then
    printf "Error: %s does not exist; are you sure your computer is connected to the flash memory flasher?\n" "$1"
    exit 1
elif [ ! -b "$1" ]; then
    printf "Warning: %s does not appear to be a block device\n" "$1"
fi

printf "Proceeding will completely erase %s and flash your custom firmware\nto it. This process is irreversible.\n" "$1"

printf "Are you sure? [y/N] "
read -r reply
case "$reply" in
    [yY][eE][sS]|[yY])
        printf "Erasing current firmware..."
        dd if="$2" of="$1" status=none # Work around for empty flash chips
        dd if=/dev/zero of="$1" status=none 2>/dev/null
        printf " Done\n"
        if [ "$usesbi" = "yes" ]; then
            printf "Flashing OpenSBI..."
            dd if=/mnt/builtin/firmware_files/fw_jump.bin of="$1" status=none
            printf " Done\n"
            printf "Flashing custom firmware..."
            dd if="$2" of="$1" seek=$((2*1024*1024)) oflag=seek_bytes status=none
            printf " Done\n"
        else
            printf "Flashing custom firmware..."
            dd if="$2" of="$1" status=none
            printf " Done\n"
        fi
        ;;
    *)
esac
