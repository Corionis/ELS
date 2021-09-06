ELS is written in Java version 1.8 and supporting libraries. The resulting jar
is completely self-contained. ELS runs on any version 1.8 or newer of either Oracle
Java or the OpenJDK.

ELS was developed using [JetBrains Intellij IDEA](https://www.jetbrains.com/idea/). Eclipse
users can adapt the project easily.

In the interest of readability and extensibility some portions of ELS are written-out in "long hand" code.
Heavy abstration and terse "denso-code" have been avoided, while other areas are
more object-oriented.

The SFTP client is implemented using [Jsch : Java Secure Channel](http://www.jcraft.com/jsch/).

The SFTP server implementation utilizes [Apache MINA Project](https://mina.apache.org/sshd-project/)
technology.

The STTY implementation is entirely custom. The command set available is
easily extensible. Commands are used by both automated and interactive (-r M and -r T) sessions
in a request/response REST style. Interactive sessions display a built-in terminal developed
in Java Swing. The Look 'n Feel of the terminal depends on the operating system. The default LnF
supported by the installed Java VM is used by default. The LnF used may be changed, see the
Communications How-To for details.

All remote communication is encrypted. Remote being not on the local computer where two
systems are being used.

The mock directory contains pre-set publisher and subscriber collections and hint files
to support testing and provide a completely self-contained development and test environment.
The mock/scripts/linux/ directory has many scripts to perform application-level tests.

These scripts show many of the various ways ELS may be executed using different combinations
of options. See the **README** in that directory for more information and a description of 
the testing sequence.

For IntelliJ users several run/debug configurations have been added that match the scripts in
the mock/scripts/linux/ directory organized in the same way and use the same mock/ data.
