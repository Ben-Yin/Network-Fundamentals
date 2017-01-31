#!/bin/bash

# warning message when input params are erroneous
usage="usage: plot feature variant1 variant2"

if [ $# -ne 3 ]
then
    echo "plot: num of params should be 3"
    echo $usage
    exit -1
fi

cd ../out/ex2/
INDICATOR="$1"
VARIANT1="$2"
VARIANT2="$3"

gnuplot -persist <<-EOFMarker
    set title "$INDICATOR-$VARIANT1-$VARIANT2"
    set xlabel "CBR"
    set ylabel "$INDICATOR"
    set output "../../img/ex2/$INDICATOR-$VARIANT1-$VARIANT2.png"
    set xrange [0:10]
    set xtics 0,1,10
    set key outside
    set key right
    set terminal png linewidth 2
    plot "$INDICATOR-$VARIANT1-$VARIANT2.out" using 1:2 w lp pt -1 lw 2 title "$VARIANT1", "$INDICATOR-$VARIANT1-$VARIANT2.out" using 1:3 w lp pt -1 lw 2 title "$VARIANT2"
EOFMarker

cd -
