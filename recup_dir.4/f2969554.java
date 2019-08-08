ation. 
The following values for CIM status code are defined: 
1 - CIM_ERR_FAILED. A general error occurred that is not covered by a more specific error code. 
2 - CIM_ERR_ACCESS_DENIED. Access to a CIM resource was not available to the client. 
3 - CIM_ERR_INVALID_NAMESPACE. The target namespace does not exist. 
4 - CIM_ERR_INVALID_PARAMETER. One or more parameter values passed to the method were invalid. 
5 - CIM_ERR_INVALID_CLASS. The specified Class does not exist. 
6 - CIM_ERR_NOT_FOUND. The requested object could not be found. 
7 - CIM_ERR_NOT_SUPPORTED. The requested operation is not supported. 
8 - CIM_ERR_CLASS_HAS_CHILDREN. Operation cannot be carried out on this class since it has instances. 
9 - CIM_ERR_CLASS_HAS_INSTANCES. Operation cannot be carried out on this class since it has instances. 
10 - CIM_ERR_INVALID_SUPERCLASS. Operation cannot be carried out since the specified superclass does not exist. 
11 - CIM_ERR_ALREADY_EXISTS. Operation cannot be carried out because an object already exists. 
12 - CIM_ERR_NO_SUCH_PROPERTY. The specified Property does not exist. 
13 - CIM_ERR_TYPE_MISMATCH. The value supplied is incompatible with the type. 
14 - CIM_ERR_QUERY_LANGUAGE_NOT_SUPPORTED. The query language is not recognized or supported. 
15 - CIM_ERR_INVALID_QUERY. The query is not valid for the specified query language. 
16 - CIM_ERR_METHOD_NOT_AVAILABLE. The extrinsic Method could not be executed. 
17 - CIM_ERR_METHOD_NOT_FOUND. The specified extrinsic Method does not exist. 
18 - CIM_ERR_UNEXPECTED_RESPONSE. The returned response to the asynchronous operation was not expected. 
19 - CIM_ERR_INVALID_RESPONSE_DESTINATION. The specified destination for the asynchronous response is not valid. 
20 - CIM_ERR_NAMESPACE_NOT_EMPTY. The specified Namespace is not empty.
21 - CIM_ERR_INVALID_ENUMERATION_CONTEXT. The enumeration context supplied is not valid.
22 - CIM_ERR_INVALID_OPERATION_TIMEOUT. The specified Namespace is not empty.
23 - CIM_ERR_PULL_HAS_BEEN_ABANDONED. The specified Namespace is not empty.
24 - CIM_ERR_PULL_CANNOT_BE_ABANDONED. The attempt to abandon a pull operation has failed.
25 - CIM_ERR_FILTERED_ENUMERATION_NOT_SUPPORTED. Filtered Enumeratrions are not supported.
26 - CIM_ERR_CONTINUATION_ON_ERROR_NOT_SUPPORTED. Continue on error is not supported.
27 - CIM_ERR_SERVER_LIMITS_EXCEEDED. The WBEM Server limits have been exceeded (e.g. memory, connections, ...).
28 - CIM_ERR_SERVER_IS_SHUTTING_DOWN. The WBEM Server is shutting down.
29 - CIM_ERR_QUERY_FEATURE_NOT_SUPPORTED. The specified Query Feature is not supported.