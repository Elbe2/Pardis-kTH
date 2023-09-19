# Compile this plot using
# 'gnuplot amdahl.p'

# Output size
set terminal png size 800,500

# Output filename
set output 'amdahl.png'

# Graphics title
set title "My Amdahl's law for X sort"

# Set x and y label
set xlabel 'threads'
set ylabel 'speedup'

# Plot the data
# using X:Y means plot using column X and column Y
# Here column 1 is number of threads
# Column 2, 3, 4, 6 are the speedup
plot "amdahl.dat" using 1:2 with lines title 'p = 0.2', \
     "amdahl.dat" using 1:3 with lines title 'p = 0.4', \
     "amdahl.dat" using 1:4 with lines title 'p = 0.6', \
     "amdahl.dat" using 1:5 with lines title 'p = 0.8'
