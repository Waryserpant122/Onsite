s class exposes the security capabilities if the adapter 
// supports IPSEC.
//
// This class uses PDO instance names with a single instance.
//
typedef enum {
    ISCSI_ENCRYPT_NONE = 0,
    ISCSI_ENCRYPT_3DES_HMAC_SHA1 = 1,
    ISCSI_ENCRYPT_AES_CTR = 2              
} ISCSI_ENCRYPTION_TYPES, *PISCSI_ENCRYPTION_TYPES;

