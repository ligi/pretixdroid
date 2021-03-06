package eu.pretix.pretixdroid.check;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.net.api.ApiException;
import eu.pretix.pretixdroid.net.api.PretixApi;

public class OnlineCheckProvider implements TicketCheckProvider {
    private Context ctx;
    private PretixApi api;
    private AppConfig config;

    public OnlineCheckProvider(Context ctx) {
        this.ctx = ctx;

        this.config = new AppConfig(ctx);
        this.api = PretixApi.fromConfig(config);
    }

    @Override
    public CheckResult check(String ticketid) {
        try {
            CheckResult res = new CheckResult(CheckResult.Type.ERROR);
            JSONObject response = api.redeem(ticketid);
            String status = response.getString("status");
            if ("ok".equals(status)) {
                res.setType(CheckResult.Type.VALID);
            } else {
                String reason = response.optString("reason");
                if ("already_redeemed".equals(reason)) {
                    res.setType(CheckResult.Type.USED);
                } else if ("unknown_ticket".equals(reason)) {
                    res.setType(CheckResult.Type.INVALID);
                } else if ("unpaid".equals(reason)) {
                    res.setType(CheckResult.Type.UNPAID);
                }
            }

            if (response.has("data")) {
                res.setTicket(response.getJSONObject("data").getString("item"));
                res.setVariation(response.getJSONObject("data").getString("variation"));
                res.setAttendee_name(response.getJSONObject("data").getString("attendee_name"));
                res.setOrderCode(response.getJSONObject("data").getString("order"));
            }
            return res;
        } catch (JSONException e) {
            return new CheckResult(CheckResult.Type.ERROR, "Invalid server response");
        } catch (ApiException e) {
            return new CheckResult(CheckResult.Type.ERROR, e.getMessage());
        }
    }
}
