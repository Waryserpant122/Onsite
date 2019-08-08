********************************************
//
//  hbapiwmi.h
// 
//  Module: WDM classes to expose HBA api data from drivers
//
//  Purpose: Contains WDM classes that specify the HBA data to be exposed 
//           via the HBA api set.
//
//  NOTE: This file contains information that is based upon:
//        SM-HBA Version 1.0 and FC-HBA 2.18 specification.
//
//        Please specify which WMI interfaces the provider will implement by
//        defining MS_SM_HBA_API or MSFC_HBA_API before including this file.
//        That is:
//
//        #define MS_SM_HBA_API
//        #include <hbapiwmi.h>
//
//        - or -
//
//        #define MSFC_HBA_API
//        #include <hbapiwmi.h>
//
//
//  Copyright (c) 2001 Microsoft Corporation
//
//***************************************************************************

