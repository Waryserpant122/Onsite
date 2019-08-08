. One or more of the following limits can be in effect: 
Limit Working Set - Causes all processes associated with the job to use the same minimum and maximum working set sizes.
Limit Process Time - Establishes a user-mode execution time limit for each currently active process and for all future processes associated with the job.
Limit Job Time - Establishes a user-mode execution time limit for the job. This flag cannot be used with Limit Preserve Job Time. 
Active Process Limit - Establishes a maximum number of simultaneously active processes associated with the job.
Limit Affinity - Causes all processes associated with the job to use the same processor affinity.
Limit Priority Class - Causes all processes associated with the job to use the same priority class. 
Limit Preserve Job Time - Preserves any job time limits you previously set. As long as this flag is set, you can establish a per-job time limit once, then alter other limits in subsequent calls. This flag cannot be used with Limit Job Time. 
Limit Scheduling Class - Causes all processes in the job to use the same scheduling class. 
Limit Process Memory - Causes all processes associated with the job to limit their committed memory. When a process attempts to commit memory that would exceed the per-process limit, it fails. If the job object is associated with a completion port, a JOB_OBJECT_MSG_PROCESS_MEMORY_LIMIT message is sent to the completion port. 
Limit Job Memory - Causes all processes associated with the job to limit the job-wide sum of their committed memory. When a process attempts to commit memory that would exceed the job-wide limit, it fails. If the job object is associated with a completion port, a JOB_OBJECT_MSG_JOB_MEMORY_LIMIT message is sent to the completion port. 
Limit Die On Unhandled Exception - Forces a call to the SetErrorMode function with the SEM_NOGPFAULTERRORBOX flag for each process associated with the job.
Limit Breakaway OK - If any process associated with the job creates a child process using the CREATE_BREAKAWAY_FROM_JOB flag while this limit is in effect, the child process is not associated with the job.
Silent Breakaway OK - Allows any process associated with the job to create child processes that are not associated with the job. 