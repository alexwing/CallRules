package com.icaria.block39;

import android.net.Uri;
import android.telecom.Call;
import android.telecom.CallScreeningService;

/**
 * Rechaza las llamadas entrantes cuyo número empieza por +39 (o 0039).
 * El resto de llamadas se dejan pasar sin tocar.
 */
public class Block39Service extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details details) {
        boolean block = false;

        Uri handle = details.getHandle();
        if (handle != null && "tel".equals(handle.getScheme())) {
            String number = handle.getSchemeSpecificPart().replaceAll("[\\s\\-()]", "");
            if (number.startsWith("+39") || number.startsWith("0039")) {
                block = true;
            }
        }

        CallScreeningService.CallResponse.Builder response =
                new CallScreeningService.CallResponse.Builder();
        if (block) {
            response.setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .setSkipCallLog(false);
        }
        respondToCall(details, response.build());
    }
}
