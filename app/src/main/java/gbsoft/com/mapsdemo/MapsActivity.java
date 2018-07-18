package gbsoft.com.mapsdemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private Spinner mSpinner;
    private ArrayAdapter<CharSequence> spinnerAdapter;
    private boolean isMapReady = false;
    private LatLng kalanki, busPark, ratnaPark;
    private int colorsArr[] = {Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.GRAY};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // initialization of spinner object
        mSpinner = findViewById(R.id.spnrTrvlMode);
        spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.travel_mode_arr, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(spinnerAdapter);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if(!isInternetConnectionAvailable())
            showConnDialog();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // LatLongs defined and camera was adjusted.
         kalanki = new LatLng(27.694235, 85.281476);
         busPark = new LatLng(27.734126, 85.308082);
         ratnaPark = new LatLng(27.706198, 85.315150);

        LatLngBounds bounds = new LatLngBounds(kalanki, ratnaPark);
        CameraUpdate camUpdate = CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 13.6f);
        mMap.moveCamera(camUpdate);
        mMap.setLatLngBoundsForCameraTarget(bounds);

        isMapReady = true;

    }

    // https://maps.googleapis.com/maps/api/directions/json?origin=Kalanki&destination=Gongabu+Bus+Park&key=YOUR_API_KEY
    public String makeURL (String mode, double srcLat, double srcLong, double destLat, double destLong ){
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(srcLat));
        urlString.append(",");
        urlString.append(Double.toString( srcLong));
        urlString.append("&destination=");// to
        urlString.append(Double.toString( destLat));
        urlString.append(",");
        urlString.append(Double.toString(destLong));
        urlString.append("&mode=");
        urlString.append(mode);
        urlString.append("&alternatives=true");
        urlString.append("&key=AIzaSyDIGCprNEBglHpTl1dOCYPC_3GRRoYByuk");
        return urlString.toString();
    }

    private void getDirection(String mode, LatLng from, LatLng to){
        //Getting the URL
        String url = makeURL(mode, from.latitude, from.longitude, to.latitude, to.longitude);
        Log.d("url", url);
        //Showing a dialog till we get the route
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage("Getting Route..Please wait...");
        dialog = builder.create();
        final AlertDialog dialog1 = dialog;
        dialog.show();

        //Creating a string request
        StringRequest stringRequest = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        Log.d("json_result", "response is fetched from google server "+ response);
                        dialog1.dismiss();
                        //Calling the method drawPath to draw the path
                        drawPath(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        dialog1.dismiss();
                    }
                });

        //Adding the request to request queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    //The parameter is the response from the server
    public void drawPath(String  result) {

        try {
            //Parsing json to get required polyline
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            int routeLen = routeArray.length();
            JSONObject routes[] = new JSONObject[routeLen], ovrViewPolLines[] = new JSONObject[routeLen];
            String encodedStr[] = new String[routeLen];
            String distance[] = new String[routeLen], time[] = new String[routeLen];
            Toast.makeText(this, routeLen+ " routes found and are highlighted with different colors", Toast.LENGTH_LONG).show();
            LatLng midLatLng;
            for(int i=0; i < routeLen; i++) {
                routes[i] = routeArray.getJSONObject(i);
                distance[i] = routes[i].getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getString("text");
                time[i] = routes[i].getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text");
                Log.d("attrib", time[i] + " " + distance[i]);
                ovrViewPolLines[i] = routes[i].getJSONObject("overview_polyline");
                encodedStr[i] = ovrViewPolLines[i].getString("points");
                List<LatLng> list = decodePoly(encodedStr[i]);
                Polyline line = mMap.addPolyline(new PolylineOptions()
                        .clickable(true)
                        .addAll(list)
                        .width(15)
                        .color(colorsArr[i])
                        .geodesic(true));
                midLatLng = line.getPoints().get(line.getPoints().size()/2);
                mMap.addMarker(new MarkerOptions().title(time[i]).snippet(distance[i]).position(midLatLng));
            }
        }
        catch (JSONException e) {

        }
    }

    // function to decode overview_polyline points(from stackoverflow :D)
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    public void onBtnCalcClick(View view){
        if(isMapReady == true) {
            mMap.clear();
            addMarkers();
            int pos = mSpinner.getSelectedItemPosition();
            String mode[] = getResources().getStringArray(R.array.travel_mode_arr);
            getDirection(mode[pos], kalanki, ratnaPark);
            Log.d("mode", mode[pos]);
        }
    }

    public boolean isInternetConnectionAvailable(){
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = manager.getActiveNetworkInfo();
        return activeNetInfo != null && activeNetInfo.isConnected();
    }

    private void showConnDialog(){
        AlertDialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Connection!");
        builder.setMessage("Please ensure your stable internet connection to get latest metrics on the map!");
        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    private void addMarkers(){
        mMap.addMarker(new MarkerOptions().position(kalanki).title("This is Kalanki"));
        mMap.addMarker(new MarkerOptions().position(busPark).title("This is Gongabu Bus Park"));
        mMap.addMarker(new MarkerOptions().position(ratnaPark).title("This is Ratna Park"));
    }
}
