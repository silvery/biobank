#!/bin/bash

#set -o verbose

ARGS=4
E_BADARGS=85

if [ $# -ne $ARGS ]  # Correct number of arguments passed to script?
then
  echo "Usage: `basename $0` HOST USER PWD OUTFILE"
  exit $E_BADARGS
fi

MYSQL=/usr/bin/mysql
SED=/bin/sed
HOST=$1
USER=$2
PWD=$3
OUTPUT=$4
DBNAME=bbpdb
HEADER=""

echo "" > $OUTPUT

function mysqlCsv {
    #echo "parms: $*"
    $MYSQL $HEADER -h$HOST -u$USER -p$PWD -D$DBNAME -B -e "$*" | $SED 's/\t/\",\"/g;s/^/\"/;s/$/\"/;s/\n//g;s/\"NULL\"/null/g' >> $OUTPUT
    HEADER="-N"
}

INV_IDS=(
NUAW474817
NUBB645000
NUBB770184
NUBB770306
NUBB770342
NUBB770582
NUBB770795
NUBB770829
NUBQ793463
NUBQ793560
NUBQ793694
NUBQ905794
NUBR024643
NUBR063688
NUBR293702
NUBR314342
NUBR314634
NUBR343122
NUBR343159
NUBR352032
NUBR491845
NUBR492367
NUBR492446
NUBS513870
NUBS914316
NUBS985785
NUBS985882
NUBS985907
NUBS986137
NUBS986155
NUBT019982
NUBT227402
NUBT242207
NUBT242623
NUBT306949
NUBT311385
NUBT322479
NUBT323636
NUBT323742
NUBT323858
NUBT324121
NUBT324343
NUBT354113
NUBT354326
NUBT354566
NUBT354636
NUBT354733
NUBT355024
NUBT355909
NUBT355936
NUBT378575
NUBT379121
NUBT379228
NUBT383647
NUBT386334
NUBT388776
NUBT389076
NUBT389252
NUBT389605
NUBT429949
NUBT430079
NUBT430662
NUBT477636
NUBT477733
NUBT479193
NUBT479616
NUBT479829
NUBT479838
NUBT479926
NUBT480311
NUBT486041
NUBT486263
NUBT533242
NUBT534612
NUBT542383
NUBT542408
NUBT542569
NUBT542897
NUBT542903
NUBT543197
NUBT543203
NUBT544530
NUBT544804
NUBT546121
NUBT546264
NUBT546440
NUBT546459
NUBT546468
NUBT546574
NUBT546635
NUBT560024
NUBT601154
NUBT601385
NUBT601516
NUBT601631
NUBT615218
NUBT615430
NUBT615500
NUCU139936
NUCU140905
NUCU140969
NUCU141922
NUCU143197
NUCU143568
NUCU143577
NUCU147759
NUCU148466
NUCU148554
NUCU181151
NUCU181197
NUCU181221
NUCU183177
NUCU199691
NUCU199752
NUCU200155
NUCU200164
NUCU200191
NUCU203134
NUCU204470
NUCU204595
NUCU204610
NUCU204629
NUCU205080
NUCU214060
NUCU215926
NUCU216341
NUCU216697
NUCU216749
NUCU216846
NUCU216855
NUCU217182
NUCU217544
NUCU223785
NUCU223882
NUCU224818
NUCU245325
NUCU245422
NUCU245556
NUCU245927
NUCU246139
NUCU246272
NUCU246494
NUCU246555
NUCU249330
NUCU249844
NUCU249914
NUCU250220
NUCU250363
NUCU250406
NUCU251928
NUCU252149
NUCU255331
NUCU262881
)

for inv_id in "${INV_IDS[@]}"
do
    mysqlCsv "select study_name_short,dec_chr_nr,
patient_visit.bb2_pv_id,
patient_visit.date_received,patient_visit.date_taken,
sample_name_short, freezer_link.link_date,
freezer_link.inventory_id
from freezer_link
join sample_list on sample_list.sample_nr=freezer_link.sample_nr
join patient_visit on patient_visit.visit_nr=freezer_link.visit_nr
join patient on patient.patient_nr=patient_visit.patient_nr
join study_list on study_list.study_nr=patient_visit.study_nr
left join freezer on freezer.inventory_id=freezer_link.inventory_id
where freezer_link.inventory_id='$inv_id'"
done
