#!/bin/bash

# warning message when input params are erroneous
usage="usage: plot throughput/drop-rate/latency"

if [ $# -ne 1 ]
then
    echo "plot: num of params should be 1"
    echo $usage
    exit -1
fi

cd ../out/ex1/
INPUT="$1-Tahoe-Reno-NewReno-Vegas"
INDICATOR="$1"

gnuplot -persist <<-EOFMarker
    set title "$INDICATOR for TCP variants"
    set xlabel "CBR"
    set ylabel "$INDICATOR"
    set output "../../img/ex1/$INDICATOR.png"
    set xrange [0:10]
    set xtics 0,1,10
    set key outside
    set key right
    set terminal png linewidth 2
    plot "$INPUT.out" using 1:2 w lp pt -1 lw 2 title "Tahoe", "$INPUT.out" using 1:3 w lp pt -1 lw 2 title "Reno", "$INPUT.out" using 1:4 w lp pt -1 lw 2 title "NewReno", "$INPUT.out" using 1:5 w lp pt -1 lw 2 title "Vegas"
EOFMarker

cd -
