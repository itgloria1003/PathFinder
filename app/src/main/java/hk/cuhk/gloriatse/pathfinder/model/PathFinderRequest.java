package hk.cuhk.gloriatse.pathfinder.model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by gloriatse on 19/11/2017.
 */

public class PathFinderRequest {
    public LatLng getOrig() {
        return orig;
    }

    public void setOrig(LatLng orig) {
        this.orig = orig;
    }

    private LatLng orig;
    private String origLocation;

    public String getOrigLocation() {
        return origLocation;
    }

    public void setOrigLocation(String origLocation) {
        this.origLocation = origLocation;
    }

    public String getDestLocation() {
        return destLocation;
    }

    public void setDestLocation(String destLocation) {
        this.destLocation = destLocation;
    }

    private String destLocation;


}
