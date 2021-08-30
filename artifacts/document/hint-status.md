ELS Hints track the status of completion for each back-up inside the .els hint file.
When using a single media back-up hints work well. However when using multiple
back-ups coordination between them is needed. This allows any back-up to perform
operations with any other back-up or the media server and keep it all straight, and
avoid the "odd man out" problem.

ELS 3.1.0 and later has the ability to coordinate hint status using an optional
tracker. The tracker may be used locally or the Hint Status Server may be run to
provide the needed functionality when using the -r | --remote option.

ELS Hints are optional. Hint tracking is optional when using hints. But the ELS
Hint Status Server (HSS) is required when using hints and hint tracking with the
-r | --remote option.

