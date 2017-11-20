
package hk.cuhk.gloriatse.pathfinder.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Step {

    @SerializedName("distance")
    @Expose
    private Distance_ distance;
    @SerializedName("duration")
    @Expose
    private Duration_ duration;
    @SerializedName("end_location")
    @Expose
    private EndLocation_ endLocation;
    @SerializedName("html_instructions")
    @Expose
    private String htmlInstructions;
    @SerializedName("polyline")
    @Expose
    private Polyline polyline;
    @SerializedName("start_location")
    @Expose
    private StartLocation_ startLocation;
    @SerializedName("travel_mode")
    @Expose
    private String travelMode;

    public Distance_ getDistance() {
        return distance;
    }

    public void setDistance(Distance_ distance) {
        this.distance = distance;
    }

    public Duration_ getDuration() {
        return duration;
    }

    public void setDuration(Duration_ duration) {
        this.duration = duration;
    }

    public EndLocation_ getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(EndLocation_ endLocation) {
        this.endLocation = endLocation;
    }

    public String getHtmlInstructions() {
        return htmlInstructions;
    }

    public void setHtmlInstructions(String htmlInstructions) {
        this.htmlInstructions = htmlInstructions;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }

    public StartLocation_ getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(StartLocation_ startLocation) {
        this.startLocation = startLocation;
    }

    public String getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(String travelMode) {
        this.travelMode = travelMode;
    }

}
