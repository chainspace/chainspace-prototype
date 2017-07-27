#!/bin/bash
screen -X -S s0n0 kill
screen -X -S s0n1 kill
screen -X -S s0n2 kill
screen -X -S s0n3 kill
screen -X -S s1n0 kill
screen -X -S s1n1 kill
screen -X -S s1n2 kill
screen -X -S s1n3 kill

rm -rf TreeMapServer-*
