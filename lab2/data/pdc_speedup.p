# Compile this plot using
# 'gnuplot pdc_speedup.p'

# Output size
set terminal png size 800,500

# Output filename
set output 'pdc_speedup.png'

# Graphics title
set title "X sort speedup on PDC Dardel"

# Set x and y label
set xlabel 'threads'
set ylabel 'ms'

plot "pdc_speedup.dat" using 1:2 with lines title 'Thread', \
     "pdc_speedup.dat" using 1:3 with lines title 'ExecutorService', \
     "pdc_speedup.dat" using 1:4 with lines title 'ExecutorServicePhaser', \
     "pdc_speedup.dat" using 1:5 with lines title 'ForkJoinPool', \
     "pdc_speedup.dat" using 1:6 with lines title 'ParallelStream'
