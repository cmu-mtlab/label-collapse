#!/bin/env bash

# USAGE:
#
# (1) Run the Java label collapser to exhaustion on an input list of labels
#     and counts.  Save the result and set it as $LC_ALL below.  Save the
#     input file itself and set it as $NT_PAIR_COUNTS below.
# (2) Determine the number of iterations you would like to try and set it as
#     $NUM_IT below.
# (3) Run this script!  Label maps go to lc-it$NUM_IT.{src,tgt}.

# Constants:
LC_ALL=lc-all.out
NUM_IT=2067
NT_PAIR_COUNTS=nt-pair-counts-all-fix.txt

## Given the number of iterations, produce the LC tables:
srcStart=$(expr $(grep -n '===' $LC_ALL | grep -A 3 "ITERATION $NUM_IT " | cut -d ':' -f 1 | head -n 2 | tail -n 1) + 1)
srcEnd=$(expr $(grep -n '===' $LC_ALL | grep -A 3 "ITERATION $NUM_IT " | cut -d ':' -f 1 | head -n 3 | tail -n 1) - 2)
tgtStart=$(expr $(grep -n '===' $LC_ALL | grep -A 3 "ITERATION $NUM_IT " | cut -d ':' -f 1 | head -n 3 | tail -n 1) + 1)
tgtEnd=$(expr $(grep -n '===' $LC_ALL | grep -A 3 "ITERATION $NUM_IT " | cut -d ':' -f 1 | head -n 4 | tail -n 1) - 1)

head -n $srcEnd $LC_ALL | tail -n +$srcStart > x
head -n $tgtEnd $LC_ALL | tail -n +$tgtStart > y
perl name-label-clusters.pl x $NT_PAIR_COUNTS src > lc-it$NUM_IT.src
perl name-label-clusters.pl y $NT_PAIR_COUNTS tgt > lc-it$NUM_IT.tgt
