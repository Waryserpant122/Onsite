BA_EVENT_PORT_BROADCAST_SES    0x208
#define HBA_EVENT_PORT_BROADCAST_D01_4  0x209
#define HBA_EVENT_PORT_BROADCAST_D04_7  0x20a
#define HBA_EVENT_PORT_BROADCAST_D16_7  0x20b
#define HBA_EVENT_PORT_BROADCAST_D29_7  0x20c
#define HBA_EVENT_PORT_ALL              0x2ff
   
   /* Port Statistics Events */
#define HBA_EVENT_PORT_STAT_THRESHOLD   0x301
#define HBA_EVENT_PORT_STAT_GROWTH      0x302

/* Phy Statistics Events */
#define HBA_EVENT_PHY_STAT_THRESHOLD    0x351
#define HBA_EVENT_PHY_STAT_GROWTH       0x352

   /* Target Level Events */
#define HBA_EVENT_TARGET_UNKNOWN        0x400
#define HBA_EVENT_TARGET_OFFLINE        0x401
#define HBA_EVENT_TARGET_ONLINE         0x402
#define HBA_EVENT_TARGET_REMOVED        0x403

   /* Fabric Link  Events */
#define HBA_EVENT_LINK_UNKNOWN          0x500
#define HBA_EVENT_LINK_INCIDENT         0x501
