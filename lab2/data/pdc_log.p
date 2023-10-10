# Compile this plot using
# 'gnuplot pdc_log.p'

# Output size
set terminal png size 800,500

# Output filename
set output 'pdc_log.png'

# Graphics title
set title "X sort performance on PDC Dardel (log scale)"

# Set x and y label
set xlabel 'threads'
set ylabel 'ms'
set logscale x
set logscale y
set yrange[100:1100]

# Plot the data
# using X:Y means plot using column X and column Y
# Here column 1 is number of threads
# Column 2, 3, 4, 6 are the speedup
plot "pdc.dat" using 1:2 with lines title 'Sequential', \
     "pdc.dat" using 1:3 with lines title 'Thread', \
     "pdc.dat" using 1:4 with lines title 'ExecutorService', \
     "pdc.dat" using 1:5 with lines title 'ExecutorServicePhaser', \
     "pdc.dat" using 1:6 with lines title 'ForkJoinPool', \
     "pdc.dat" using 1:7 with lines title 'ParallelStream'