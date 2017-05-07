#! /bin/bash
( cat run.scd ; read; echo 'f.play'; sleep 3 ) | sclang -s
