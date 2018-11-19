package ch.sbb.matsim.plans.abm;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

public class AbmTrip {

    private String oAct;
    private String dAct;
    private String mode;
    private int deptime;
    private int arrtime;

    private Id<ActivityFacility> origFacilityId;
    private Id<ActivityFacility> destFacilityId;



    public AbmTrip(Id<ActivityFacility> origFacilityId, Id<ActivityFacility> destFacilityId, String oAct, String dAct, String mode, int deptime, int arrtime) {
        this.oAct = oAct;
        this.dAct = dAct;
        this.mode = mode;
        this.deptime = deptime;
        this.arrtime = arrtime;

        this.origFacilityId = origFacilityId;
        this.destFacilityId = destFacilityId;


    }

    public Id<ActivityFacility> getOrigFacilityId() {
        return origFacilityId;
    }

    public Id<ActivityFacility> getDestFacilityId() {
        return destFacilityId;
    }

    public String getMode() {
        return this.mode;
    }

    public int getArrtime() {
        return arrtime;
    }


    public int getDepTime() {
        return this.deptime;
    }

    public String getDestAct() {
        return this.dAct;
    }

    public String getoAct() {
        return oAct;
    }

}
