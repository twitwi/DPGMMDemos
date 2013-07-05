
cd ../Utilities && mvn clean install && cd - && cd ../DPGMMJavaCL && mvn clean install dependency:copy-dependencies && cd -


sess=session-1

mkdir $sess

for dim in 2 10 100 ; do
for samples in 10000 100000 ; do

out=$sess/$(hostname)-$dim-$samples-java
echo "DOING: $out"
java -cp ../DPGMMJavaCL/target/DPGMMJavaCL-1.0-SNAPSHOT.jar:../DPGMMJavaCL/target/dependency/\* com.heeere.dpgmm.javacl.Benchmark1 10 100000 10 .01 5000 10000 --java > $out

for block in 1000 10000 ; do
out=$sess/$(hostname)-$dim-$samples-clgpu-$block
echo "DOING: $out"
java -cp ../DPGMMJavaCL/target/DPGMMJavaCL-1.0-SNAPSHOT.jar:../DPGMMJavaCL/target/dependency/\* com.heeere.dpgmm.javacl.Benchmark1 10 100000 10 .01 5000 $block > $out
done

for block in 1000 10000 ; do
out=$sess/$(hostname)-$dim-$samples-clcpu-$block
echo "DOING: $out"
(SETSHELL opencl && java -cp ../DPGMMJavaCL/target/DPGMMJavaCL-1.0-SNAPSHOT.jar:../DPGMMJavaCL/target/dependency/\* com.heeere.dpgmm.javacl.Benchmark1 10 100000 10 .01 5000 $block > $out)
done

done; done


plotAll() {
    ls -1 "$@" | awk '
BEGIN {printf "plot "}
NR != 1 {printf ","}
{printf "\"%s\" using 1:2 with lines", $$0}
END {print ";"; print "pause -1"}
' > ,,.gp
    gnuplot ,,.gp
}
plotAllCorr() {
    local q=SEP
    local awk="NR==2 {base=\$2} NR>=2 {print \$1 $q (\$2 - base)}"
    ls -1 "$@" | awk '
BEGIN {printf "plot "}
NR != 1 {printf ","}
{printf "\"< cat %s | awk '"'$awk'"'\" using 1:2 with lines ti \"ZZZ%s\"", $$0, $$0}
END {print ";"; print "pause -1"}
' | sed 's@SEP@\\" \\"@g' > ,,.gp
    gnuplot ,,.gp
}
plotAllCorr session-1/thinkstitch-*-100000-*









function quicksubgpu {
    which qsub >/dev/null || echo "You need to have qsub (SETSHELL grid ?)"
    mkdir -p '/idiap/temp/'$(whoami)'/jobs-out'
    quicksubdry "$@" | qsub -N J_${NAME} -l q_gpu -j y -o '/idiap/temp/'$(whoami)'/jobs-out/$JOB_NAME--$JOB_ID--$TASK_ID--$HOSTNAME'
    # ,*@dynamix*
    #  -q '*@dynamix*'
}





# ~/projects/DPGMMDemos/da cat tosub.sh
#/bin/bash

a=$(hostname)
cd ../DPGMMJavaCL
java -Xmx400m -cp target/DPGMMJavaCL-1.0-SNAPSHOT.jar:target/dependency/* com.heeere.dpgmm.javacl.TestGC 2>&1 | tee /idiap/temp/remonet/cl-$a


***
quicksubgpu ./tosub.sh


for i in /idiap/temp/remonet/cl-*; do echo $i; cat $i ; done




# ~/projects/DPGMMDemos/da cat tosub2.sh
#/bin/bash

a=$(hostname)

for dim in 2 10 100 ; do
for samples in 10000 100000 ; do

java="java -Xmx350m"
sess=session-1

out=$sess/$(hostname)-$dim-$samples-java
echo "DOING: $out"
$java -cp ../DPGMMJavaCL/target/DPGMMJavaCL-1.0-SNAPSHOT.jar:../DPGMMJavaCL/target/dependency/\* com.heeere.dpgmm.javacl.Benchmark1 10 100000 10 .01 5000 10000 --java > $out

for block in 1000 10000 ; do
out=$sess/$(hostname)-$dim-$samples-clgpu-$block
echo "DOING: $out"
$java -cp ../DPGMMJavaCL/target/DPGMMJavaCL-1.0-SNAPSHOT.jar:../DPGMMJavaCL/target/dependency/\* com.heeere.dpgmm.javacl.Benchmark1 10 100000 10 .01 5000 $block > $out
done

for block in 1000 10000 ; do
out=$sess/$(hostname)-$dim-$samples-clcpu-$block
echo "DOING: $out"
(SETSHELL opencl && $java -cp ../DPGMMJavaCL/target/DPGMMJavaCL-1.0-SNAPSHOT.jar:../DPGMMJavaCL/target/dependency/\* com.heeere.dpgmm.javacl.Benchmark1 10 100000 10 .01 5000 $block > $out)
done

done; done


***
quicksubgpu ./tosub2.sh
