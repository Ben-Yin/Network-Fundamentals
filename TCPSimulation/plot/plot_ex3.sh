#!/bin/bash

# warning message when input params are erroneous
usage="usage: plot throughput/latency"

if [ $# -ne 1 ]
then
    echo "plot: num of params should be 1"
    echo $usage
    exit -1
fi

cd ../out/ex3/
INDICATOR="$1"

gnuplot -persist <<-EOFMarker
    set title "$INDICATOR for DropTail and RED"
    set xlabel "time"
    set ylabel "$INDICATOR"
    set output "../../img/ex3/$INDICATOR.eps"
    set xrange [0:31]
    set xtics 0,2,30
    set key box bottom right
    set terminal postscript eps color solid linewidth 2 "Helvetica" 20
    plot "$INDICATOR.out" using 1:2 w lp pt -1 lw 3 title "Reno-DropTail", "$INDICATOR.out" using 1:3 w lp pt -1 lw 3 title "SACK-DropTail", "$INDICATOR.out" using 1:4 w lp pt -1 lw 3 title "Reno-RED", "$INDICATOR.out" using 1:5 w lp pt -1 lw 3 title "SACK-RED"
EOFMarker

cd -