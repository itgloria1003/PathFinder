package hk.cuhk.gloriatse.pathfinder;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import hk.cuhk.gloriatse.pathfinder.model.DirectionResponse;

/**
 * Created by anupamchugh on 27/11/15.
 */

public class DirectionsJSONParser {


    public static DirectionResponse parse(String jObject){
        Gson gson = new Gson();
        DirectionResponse directionResponse = gson.fromJson(jObject, DirectionResponse.class);
        System.out.println("directionResponse:" + directionResponse.getStatus());
        return directionResponse;
    }

}