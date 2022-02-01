package com.segula.v2x.ui.V2X;

import static android.content.ContentValues.TAG;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfMeta;
import com.mapbox.turf.TurfTransformation;
import com.segula.v2x.R;
import com.segula.v2x.databinding.FragmentHomeBinding;
import com.segula.v2x.utils.GlobalConstants;
import com.segula.v2x.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class V2XFragment extends Fragment implements OnMapReadyCallback {

    private V2XViewModel v2XViewModel;
    private FragmentHomeBinding binding;

    private FillLayer fillLayer;
    private int fillLayerCounter = 0;

    Button btnStellantis, btnOther, btnTCU, btnMoveCar, btnCrash, btnMoveAutre;
    private int marqueur = 0;

    public final ArrayList<Integer> idStellantis = new ArrayList<>();
    public final ArrayList<Integer> idOther = new ArrayList<>();
    public final ArrayList<Integer> idMarkerOtherList = new ArrayList<>();
    public final ArrayList<Integer> idMarkerStellantisList = new ArrayList<>();

    private int tcuConnected = 0, distanceCrash = 0, distanceInfo = 0, typeToast = 0, idMarkerOther = 9, puissance = 0, ecu = 0;
    private static int stellantisId = 0;
    private static int otherId = 0;
    private boolean click=false;

    //Map
    private MapView mapView;
    private MapboxMap map;

    //Lat & Long
    private double latitudePositionCar = Utils.lastLocationLatitude;
    private double longitudePositionCar =  Utils.lastLocationLongitude;
    private Point positionCar = Point.fromLngLat(longitudePositionCar, latitudePositionCar);
    private LatLng posCar = new LatLng(latitudePositionCar, longitudePositionCar);

    //Turf
    private static final String TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID = "TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID";
    private static final String TURF_CALCULATION_FILL_LAYER_ID = "TURF_CALCULATION_FILL_LAYER_ID";
    private static final String CIRCLE_CENTER_SOURCE_ID = "CIRCLE_CENTER_SOURCE_ID";
    private static final String CIRCLE_CENTER_ICON_ID = "CIRCLE_CENTER_ICON_ID";
    private static final String CIRCLE_CENTER_LAYER_ID = "CIRCLE_CENTER_LAYER_ID";

    //Markers
    List<MarkerOptions> markerTCUNOK = new ArrayList<>();
    Marker markerTCUConnected;
    Marker markersStellantis;
    Marker markersOther;

    private SymbolManager symbolManager;
    private List<Symbol> symbols = new ArrayList<>();

    //Car Info
    ConstraintLayout carInfo;
    LayoutInflater inflater2;
    View layout2;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        v2XViewModel =
                new ViewModelProvider(this).get(V2XViewModel.class);

        Mapbox.getInstance(this.requireContext(), getString(R.string.access_token));
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        latitudePositionCar = Utils.lastLocationLatitude;
        longitudePositionCar = Utils.lastLocationLongitude;

        mapView = root.findViewById(R.id.mapview) ;
        //mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapboxMap -> {
            this.map = mapboxMap;
            mapboxMap.setStyle(new Style.Builder().fromUrl(Style.TRAFFIC_DAY)//mapbox://styles/fcbasm/ckwkrwjdo4gvb14p3j33g34pd
                        .withSource(new GeoJsonSource(CIRCLE_CENTER_SOURCE_ID,
                                Feature.fromGeometry(positionCar)))
                        .withSource(new GeoJsonSource(TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID))
                        .withLayer(new SymbolLayer(CIRCLE_CENTER_LAYER_ID,
                                CIRCLE_CENTER_SOURCE_ID).withProperties(
                                iconImage(CIRCLE_CENTER_ICON_ID),
                                iconIgnorePlacement(true),
                                iconAllowOverlap(true),
                                iconOffset(new Float[] {0f, -4f})
                        )
                        ), style -> {
                        this.map = mapboxMap;
                        symbolManager = new SymbolManager(mapView, map, style);
                        markerDisplay(1,1, posCar, puissance, ecu);

                        map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(posCar)
                                        .zoom(13.6)
                                        .build()
                        ));

                        initPolygonCircleFillLayer();
                        drawPolygonCircle(positionCar);
                    });



            btnStellantis = requireView().findViewById(R.id.btnStellantis);
            btnStellantis.setOnClickListener(v -> {
                stellantisId ++;
                LatLng carStellantis = new LatLng(48.80043011689067,1.978732392194232);
                LatLng carStellantis2 = new LatLng(48.798734,2.000806);
                markerDisplay(3,5, carStellantis, puissance, ecu);

            });

            btnOther = requireView().findViewById(R.id.btnOther);
            btnOther.setOnClickListener(v -> {

                LatLng carOther = new LatLng(48.796768,1.978605);
                LatLng carOther2 = new LatLng(48.798772218369194,1.9941396447244915);
                if(idMarkerOther == 9) markerDisplay(2,idMarkerOther, carOther, puissance, ecu);

                if(idMarkerOther == 10)  markerDisplay(2,idMarkerOther, carOther2, puissance, ecu);

                idMarkerOther++;
                //OtherCar(carOther, carOther2, Integer.toString(R.string.idcar2));
            });

            btnTCU = requireView().findViewById(R.id.btnConnectionTCU);
            btnTCU.setOnClickListener(v -> {
                tcuConnected = 1;
                markerDisplay(1,1,posCar, puissance, ecu);
            });

            btnMoveCar = requireView().findViewById(R.id.btnMoveCar);
            btnMoveCar.setOnClickListener(v -> {
                posCar = new LatLng(latitudePositionCar, longitudePositionCar);
                longitudePositionCar = longitudePositionCar + 0.0001;
                latitudePositionCar = latitudePositionCar + 0.0001;
                markerDisplay(1,1, posCar, puissance, ecu);
            });

            btnCrash = requireView().findViewById(R.id.btnCrash);
            btnCrash.setOnClickListener(v ->
            {
                if(typeToast >= 2) typeToast = 0;
                typeToast ++;
                showTypeToast(typeToast, getString(R.string.idcar1));
            });

            btnMoveAutre = requireView().findViewById(R.id.btnMoveAutre);
            btnMoveAutre.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    posCar = new LatLng(49.798772218369194,1.9941396447244915);
                    markerDisplay(3,5, posCar, puissance, ecu);
                }
            });
        });

        binding.fbLoc.setOnClickListener(v -> {
            setCameraPosition(positionCar);
            initPolygonCircleFillLayer();
            drawPolygonCircle(positionCar);
        });

        //carInfo = (ConstraintLayout) requireView().findViewById(R.layout.toast_car);

        return root;
    }

//    private void moveFastCar(){
//
//    }

//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        binding = null;
//    }
//
//    @Override
//    public void onMapReady(@NonNull MapboxMap mapboxMap) {
//        map= mapboxMap;
//    }

    private void showTypeToast(int typeToast, String id){
        LayoutInflater inflater1 = requireActivity().getLayoutInflater();
        View layout = inflater1.inflate(R.layout.toast, requireView().findViewById(R.id.toast_container));
        TextView text = layout.findViewById(R.id.tvDanger);
        LayoutInflater inflater2 = requireActivity().getLayoutInflater();
        View layout2 = inflater2.inflate(R.layout.toast_car, requireView().findViewById(R.id.toast_container));
        TextView numID = layout2.findViewById(R.id.tvNumID);

        switch (typeToast) {
            case 1:
                layout.setBackground(getResources().getDrawable(R.drawable.layout_border_danger));
                text.setText(getString(R.string.crashat) + " " + distanceCrash + " m");

                Toast toast = new Toast(this.getContext());
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);
                toast.show();
                break;
            case 2:
                layout.setBackground(getResources().getDrawable(R.drawable.layout_border_info));

                text.setText(getString(R.string.infoat) + " " + distanceInfo + " m");
                text.setTextColor(getResources().getColor(R.color.black));
                Toast toast2 = new Toast(this.getContext());
                toast2.setGravity(Gravity.TOP, 0, 0);
                toast2.setDuration(Toast.LENGTH_LONG);
                toast2.setView(layout);
                toast2.show();
                break;
            case 3:
                layout2.setBackground(getResources().getDrawable(R.drawable.layout_info_car));
                numID.setText(id);

                text.setText(getString(R.string.infoat) + " " + distanceInfo + " m");
                text.setTextColor(getResources().getColor(R.color.black));
                Toast toast3 = new Toast(this.getContext());
                toast3.setGravity(Gravity.RIGHT, 0, 0);
                toast3.setView(layout2);
                toast3.show();
                break;
            case 4:
                layout2.setBackground(getResources().getDrawable(R.drawable.layout_error_car));
                numID.setText(id);

                text.setText(getString(R.string.infoat) + " " + distanceInfo + " m");
                text.setTextColor(getResources().getColor(R.color.black));
                Toast toast4 = new Toast(this.getContext());
                toast4.setGravity(Gravity.RIGHT, 0, 0);
                toast4.setView(layout2);
                toast4.show();
                break;
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void setCameraPosition(Point location){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.latitude(), location.longitude()), 13.0));
    }

    private void drawPolygonCircle(Point circleCenter) {
        map.getStyle(style -> {
// Use Turf to calculate the Polygon's coordinates
            Polygon polygonArea = getTurfPolygon(circleCenter);
            GeoJsonSource polygonCircleSource = style.getSourceAs(TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID);
            if (polygonCircleSource != null) {
                polygonCircleSource.setGeoJson(Polygon.fromOuterInner(
                        LineString.fromLngLats(TurfMeta.coordAll(polygonArea, false))));
            }
        });
    }

    private Polygon getTurfPolygon(@NonNull Point centerPoint) {
        return TurfTransformation.circle(centerPoint, 1, 180, "kilometers");
    }

    private void initPolygonCircleFillLayer() {
        map.getStyle(style -> {
            if(fillLayerCounter == 0){
                fillLayer = new FillLayer(TURF_CALCULATION_FILL_LAYER_ID,
                        TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID);
                fillLayer.setProperties(
                        fillColor(Color.parseColor("#1E8CAB")),
                        fillOpacity(.3f));
                style.addLayerBelow(fillLayer, CIRCLE_CENTER_LAYER_ID);
                fillLayerCounter = 1;
            }
            if(fillLayer != null){
                style.removeLayer(fillLayer);
                fillLayer = new FillLayer(TURF_CALCULATION_FILL_LAYER_ID,
                        TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID);
                fillLayer.setProperties(
                        fillColor(Color.parseColor("#1E8CAB")),
                        fillOpacity(.3f));
                style.addLayerBelow(fillLayer, CIRCLE_CENTER_LAYER_ID);
                fillLayerCounter = 1;
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void updateNewLocation(){
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(GlobalConstants.SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putLong(GlobalConstants.lastLocationLatitude, Double.doubleToRawLongBits(latitudePositionCar));
        editor.putLong(GlobalConstants.lastLocationLongitude, Double.doubleToRawLongBits(longitudePositionCar));
    }


    public void recupDonnee(){


    }

    public void markerDisplay(int typeMarker, int idMarker,LatLng positionMarker, int puissance, int ecu) {
        inflater2 = requireActivity().getLayoutInflater();
        layout2 = inflater2.inflate(R.layout.toast_car, requireView().findViewById(R.id.toast_container));
        TextView numID = layout2.findViewById(R.id.tvNumID);
        String carInfo = "Date : " + android.text.format.DateFormat.format("dd/MM/yyyy", new java.util.Date()) + "\n" +
                "Heure : " + SimpleDateFormat.getTimeInstance().format(new java.util.Date()) + "\n" +
                "Position : " + positionMarker + "\n" +
                "Puissance : " + puissance + "\n" +
                "ECU : " + ecu;
        switch (tcuConnected) {
            case 0:
                Icon red = IconFactory.getInstance(requireContext()).fromResource(R.drawable.red_car);

                markerTCUNOK.add(new MarkerOptions()
                        .position(positionMarker)
                        .title("ID : " + idMarker)
                        .snippet(carInfo)
                        .icon(red)
                );
                map.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(positionMarker)
                                .zoom(13.6)
                                .build()
                ));
                map.addMarkers(markerTCUNOK);
                break;

            case 1:
                switch (typeMarker) {
                    case 1:
                        if(markerTCUConnected == null){
                            map.removeAnnotations();
                            Icon blue = IconFactory.getInstance(requireContext()).fromResource(R.drawable.blue_car);
                            //Bitmap blueCar = BitmapFactory.decodeResource(getResources(), R.drawable.blue_car);
                            //map.getStyle().addImage("blueCar", blueCar);


                            markerTCUConnected = map.addMarker(new MarkerOptions()
                                    .position(positionMarker)
                                    .title("ID : " + idMarker)
                                    .snippet(carInfo)
                                    .icon(blue));

//                            markerTCUConnectedSymbol = symbolManager.create(new SymbolOptions()
//                                          .withLatLng(positionMarker)
//                                          .withIconImage("blueCar")
////                                          .withTextField(carInfoOK)
//                                          .withIconRotate(45F));

//                            symbolManager.addClickListener(symbol -> {
//
//                            });

                            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                    new CameraPosition.Builder()
                                            .target(positionMarker)
                                            .zoom(13.6)
                                            .build()
                            ));
                            positionCar = Point.fromLngLat(positionMarker.getLongitude(), positionMarker.getLatitude());
                            drawPolygonCircle(positionCar);
                        }
                        else{
                            //markerTCUConnectedSymbol.setLatLng(positionMarker);

                            markerTCUConnected.setPosition(positionMarker);
                            markerTCUConnected.setSnippet(carInfo);

                            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                    new CameraPosition.Builder()
                                            .target(positionMarker)
                                            .zoom(13.6)
                                            .build()
                            ));
                            positionCar = Point.fromLngLat(positionMarker.getLongitude(), positionMarker.getLatitude());
                            drawPolygonCircle(positionCar);
                        }

                        break;
                    case 2:
                        if(idMarkerOtherList.contains(idMarker)){
                            markersOther.setPosition(positionMarker);
                            markersOther.setSnippet(carInfo);
                        }
                        else{
                            Icon yellow = IconFactory.getInstance(requireContext()).fromResource(R.drawable.yellow_car);
                            markersOther = map.addMarker(new MarkerOptions()
                                    .position(positionMarker)
                                    .title("ID : " + idMarker)
                                    .snippet(carInfo)
                                    .icon(yellow));

                            idMarkerOtherList.add(idMarker);
                            map.getMarkers();
                        }

                        break;
                    case 3:
                        if(idMarkerStellantisList.contains(idMarker)){
                            markersStellantis.setPosition(positionMarker);
                            markersStellantis.setSnippet(carInfo);
                        }
                        else{
                            Icon green = IconFactory.getInstance(requireContext()).fromResource(R.drawable.green_car);

                            markersStellantis = map.addMarker(new MarkerOptions()
                                    .position(positionMarker)
                                    .title("ID : " + idMarker)
                                    .snippet(carInfo)
                                    .icon(green));

                            idMarkerStellantisList.add(idMarker);
                            map.getMarkers();
                        }

                        break;
                }
            default:
                break;
        }
    }
}