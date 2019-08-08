s are given the system default error mode of the calling process instead of inheriting the error mode of the parent process.  This flag is useful for multi-threaded shell applications that run with hard errors disabled.
Create_New_Console - The new process has a new console, instead of inheriting the parent's console. This flag cannot be used with the Detached_Process flag.
Create_New_Process_Group - The new process is the root process of a new process group. The process group includes all processes that are descendants of this root process. The process identifier of the new process group is the same as the process identifier (returned in the ProcessID property of the Win32_Process class).  Process groups are used by the GenerateConsoleCtrlEvent function to enable sending a CTRL+C or CTRL+BREAK signal to a group of console processes.
Create_Suspended - The primary thread of the new process is created in a suspended state and does not run until the ResumeThread function is called.
Create_Unicode_Environment - The environment settings listed in the EnvironmentVariables property use Unicode characters. If clear, the environment block uses ANSI characters.
Debug_Process - If this flag is set, the calling process is treated as a debugger, and the new process is a process being debugged.  The system notifies the debugger of all debug events that occur in the process being debugged.  On Windows 95 and Windows 98 systems, this flag is not valid if the new process is a 16-bit application.
Debug_Only_This_Process - If not set and the calling process is being debugged, the new process becomes another process being debugged by the process of the calling debugger. If the calling process is not a process being debugged, no debugging related actions occur.
Detached_Process - For console processes, the new process does not have access to the console of the parent process.  This flag cannot be used if the Create_New_Console flag is set.