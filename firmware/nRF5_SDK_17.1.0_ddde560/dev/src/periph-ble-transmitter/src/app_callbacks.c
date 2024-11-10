/**
 * function calls for event triggers
 */

#include "app_callbacks.h"
#include "app_debug.h"
#include "app_ble_nus.h"
#include "nrf_pwr_mgmt.h"
#include "app_deserializer.h"

// BLE events

// NUS connected -- do nothing here
CALLBACK_DEF_APP_SCHED(BLE_NUS_EVT_CONNECTED)       { debug_log("NUS connected"); }
// NUS notifications enabled
CALLBACK_DEF_APP_SCHED(BLE_NUS_EVT_COMM_STARTED)    { debug_log("NUS notifications enabled"); }
// NUS disconnected -- reset
CALLBACK_DEF_APP_SCHED(BLE_GAP_EVT_DISCONNECTED)    { debug_log("NUS disconnected. Resetting."); }

// Application events

typedef struct data_pack_s {
    uint32_t id;
    int32_t value;
    int32_t units;
} data_pack_t;
static uint32_t id = 0;

CALLBACK_DEF_APP_SCHED(DESERIALIZER_DATA_READY) {
    // debug_log("Deserializer data ready: %d %d", g_measure_value, g_units);
    // pack data
    data_pack_t data_pack = {
        .id = id++,
        .value = g_measure_value,
        .units = g_units,
    };
    ble_send((uint8_t *)&data_pack, sizeof(data_pack));
}


// // Accelerometer watermark interrupt raised
// CALLBACK_DEF_APP_SCHED(ACCELEROMETER_DATA_READY) {
//     // fetch accelerometer data -- 1. init spi, 2. fetch data, 3. sleep accel, 4. deinit spi
//     accelerometer_num_data = accelerometer_fetch_data(true, true, true);
//     accelerometer_copy_data(accelerometer_data_buf, accelerometer_num_data);
//     debug_log("ACCELEROMETER_DATA_READY: %d", accelerometer_num_data);

//     ble_send(accelerometer_data_buf, accelerometer_num_data);
// }

