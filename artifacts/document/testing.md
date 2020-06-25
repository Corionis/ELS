
# VolMunger: Testing Notes

## Command lines

 * Bad arguments test<br/> 
   -p

 * Full munge dry run<br/> 
   -c off -D -d info -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -T TestRun/targets-1.json -m TestRun/mismatches.txt -n TestRun/whatsnew.txt

 * Full munge<br/> 
   -c off -d debug -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -T TestRun/targets-1.json -m TestRun/mismatches.txt -n TestRun/whatsnew.txt

 * Publisher export<br/> 
   -p TestRun/publisher/publisher-libraries.json -i TestRun/publisher-export.json

 * Munge dry run -P import publisher<br/> 
   -D -P TestRun/publisher-export.json -s TestRun/subscriber-1/subscriber-1-libraries.json

 * Targets validate<br/> 
   -c off -D -d debug -T TestRun/targets-1.json
	
 * Remote subscriber -r S
   -a 1234 -d debug -r S -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -t TestRun/targets-1.json -f TestRun/volmunger-subscriber.log

 * Remote publisher -r P
   -d debug -r P -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -t TestRun/targets-1.json -m TestRun/mismatches.txt -n TestRun/whatsnew.txt -f TestRun/volmunger-publisher.log

 * Remote publisher manually -r M
   -d debug -r M -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -T TestRun/targets-1.json -f TestRun/volmunger-publisher-manual.log
