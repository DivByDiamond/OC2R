#!/bin/sh

prompt() {
  echo
  read -p "Press any key to continue... " -n1 -s
  clear
}

clear
echo "XTERM True Color (24-bit RGB) Test"
echo "Rendering 20x20 background color gradient"
prompt

# 20x20 character block gradient
# Horizontal: Red (0–255)
# Vertical:   Blue (0–255)
for y in $(seq 0 19); do
  for x in $(seq 0 19); do
    r=$(( x * 255 / 19 ))
    g=0
    b=$(( y * 255 / 19 ))
    printf "\033[48;2;%d;%d;%dm  \033[0m" "$r" "$g" "$b"
  done
  echo
done

prompt
clear
