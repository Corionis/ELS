
# VolMunger: Testing Notes

## Command lines

 * Subscriber Listener<br/> 
   -a 1234 -d debug -r S -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -t TestRun/targets-1.json -f TestRun/volmunger-subscriber.log
   
 * Publisher Dry-Run<br/>
   -D -d debug -r P -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -t TestRun/targets-1.json -m TestRun/mismatches.txt -n TestRun/whatsnew.txt -f TestRun/volmunger-publisher.log
   
 * Publisher Munge<br/>
   -d debug -r P -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -t TestRun/targets-1.json -m TestRun/mismatches.txt -n TestRun/whatsnew.txt -f TestRun/volmunger-publisher.log
   
  * Manual terminal to Subscriber<br/>
    -d debug -r M -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -T TestRun/targets-1.json -f TestRun/volmunger-publisher-manual.log
   
