#!/bin/bash

rm -f prf*.tsv
i=0
while read q ; do
	i=$((i + 1));
	prfout=prf-$i.tsv;
	curl "http://localhost:25808/prf?query=$q&ranker=favorite&numdocs=20&numterms=5" > $prfout;
	echo $q:$prfout >> prf.tsv
done < queries.tsv
java -cp src edu.nyu.cs.cs2580.Bhattacharyya prf.tsv qsim.tsv