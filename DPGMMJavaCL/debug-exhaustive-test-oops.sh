
    pre= #optirun
    for dep in dependency dependency-RC3 ; do
        for java in java /usr/lib/jvm/java-6-openjdk-amd64/bin/java ; do
            echo "JAVA: $java"
            echo
            $java -version
            echo
            for opt in {,-Dbridj.protected=true}" "{,-XX:+UseCompressedOops,-XX:-UseCompressedOops} ; do
                echo "RUNNING: $pre $java $opt -cp target/DPGMMJavaCL-1.0-SNAPSHOT.jar:target/$dep/* com.heeere.dpgmm.javacl.TestGC"
                $pre $java $opt -cp target/DPGMMJavaCL-1.0-SNAPSHOT.jar:target/$dep/* com.heeere.dpgmm.javacl.TestGC
                echo;echo;echo
            done
        done
    done


  # 500  with_amdapp 
  # 501  . exhaustive-test.sh 2>&1 | tee exhaustive-log
  # 502  mkdir withamdapp
  # 503  mv hs_err_pid25* exhaustive-log withamdapp/
  # 504  zip -r withamdapp.zip withamdapp/

  # without
  # 502  rm -rf withoptirun/
  # 503  mkdir withoptirun
  # 504  mv hs_err_pid24* exhaustive-log withoptirun/
  # 505  zip -r withoptirun.zip withoptirun/

  # 508  scp with*zip dl@heeere.com:/var/www/dl
