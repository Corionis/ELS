# Send-To Utility

## The Idea

Write an AutoIt program that could be used with most Windows
file explorers under the right-click "Send To ..." option.

When any directory or file is sent to the utility:
 * If a file the utility determines the directory of the file.
 * Pop-up a dialog for entering an optional description
 * Create a .volmunge file for the VolMunge utility in the directory:
   - Date & time of creation
   - Option description
 * Date/time and optional description are displayed & logged
  during the next VolMunge run.
 * Use the free version of Advanced Installer to create an actual
 Windows MSI installer for convenience. That installer can create the
 shortcut needed for the Send To ... menu.
   - The free version only allows an MSI.
   - Are there any other easy and free modern installer builders?
 