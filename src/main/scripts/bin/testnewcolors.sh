#!/bin/bash

for bg in {40..47}; do
  [[ $bg -le 42 ]] && fg=97 || fg=30
  printf "\e[2;%s;%sm %3s \e[0m" "$fg" "$bg" "$bg"
done
echo

for bg in {40..47}; do
  [[ $bg -le 42 ]] && fg=97 || fg=30
  printf "\e[%s;%sm %3s \e[0m" "$fg" "$bg" "$bg"
done
echo

for bg in {100..107}; do
  [[ $bg -le 102 ]] && fg=97 || fg=30
  printf "\e[%s;%sm %3s \e[0m" "$fg" "$bg" "$bg"
done
echo
