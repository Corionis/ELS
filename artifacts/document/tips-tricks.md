ELS has many nuances built-in to its features. Here are some Tips 'n Tricks.
Suggestions for additions to this list are welcome.

## Terminology

 * Local :: When each set of publisher and subscriber storage devices are attached
   to the local system.
 * Remote :: Any separate ELS operation running anywhere, local or remote system.
   For example a Subscriber Listener or the Hint Status Server.


## Hints

Hints are a way to make manual changes to a collection (rename, delete, move)
and coordinate those changes with n-backups sometime later. It's a text file
with some tracking information and the command needed to perform the change.
Note that additions are automatically backed-up and do not require a hint.

 * Hints are processed through 4 stages that allow n-way processing
   among multiple back-ups and systems. Those status values are: For, Done,
   Seen and Deleted. When all back-ups have completed the appropriate steps
   through 3-4 backup runs the Hint files and any associated Hint Tracking
   are automatically deleted. That is, Hints are a self-maintaining mechanism.

 * Hint Tracking is needed when there are more than one backups. Tracking may
   be local for multiple locally-attached backups, or remote for one or
   more remote backups running ELS.

## Tools

 * Tools may be defined that are intended to be run by hand, or as part of an automated Job, or both.

 * Renamer Tools<br/>
   Important point: When using the Renamer Tool the "Old Name" & "New Name" list are _only examples_
   and are __not__ the files that will be used with the Run button. The Run button uses whatever is
   selected in the Browser. The list does not have to be refreshed before using the Run button.

   * Feature nuance: Running a Renamer tool manually uses the sorting of the selected
     files in the Navigator Browser. The default behavior is processing the files in
     alphabetical order, i.e. in a command line-executed Job.
     * Example: 
       * To order a set of files with season and episode numbering:
       * In the Browser use Touch on each episode in the order desired.
       * Sort the file list by ascending date.
       * Select the desired files.
       * Run your Renamer numbering tool or a Job on that list of files.

