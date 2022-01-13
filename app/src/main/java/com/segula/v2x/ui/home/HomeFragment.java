package com.segula.v2x.ui.home;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
import java.util.Objects;
import timber.log.Timber;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;

    private SymbolManager symbolManager;
    private Symbol originIcon;
    private List<Symbol> symbols = new ArrayList<>();
    List<SymbolOptions> options = new ArrayList<>();
    private FillLayer fillLayer;
    private int fillLayerCounter = 0;

    Button btnStellantis, btnOther, btnTCU, btnMoveCar, btnMenu, btnCrash, btnWaiting;
    private int marqueur = 0;

    private static int stellantisId = 0;
    private static int otherId = 0;

    public final ArrayList<Integer> idStellantis = new ArrayList<>();
    public final ArrayList<Integer> idOther = new ArrayList<>();
    public final ArrayList<Integer> idMarkerOtherList = new ArrayList<>();
    public final ArrayList<Integer> idMarkerStellantisList = new ArrayList<>();

    private int tcuConnected = 0;
    private int distanceCrash = 0;
    private int distanceInfo = 0;
    private int typeToast = 0;
    int idMarkerOther = 9;
    private String date = "13/01/2021";
    private String heure = "17:00";
    private int puissance = 0, ecu = 0;

    public Context context = getContext();

    //private TCP tcpServer;

    private MapView mapView;
    private MapboxMap map;

    private double latitudePositionCar = Utils.lastLocationLatitude;
    private double longitudePositionCar =  Utils.lastLocationLongitude;
    private final Point positionCar = Point.fromLngLat(longitudePositionCar, latitudePositionCar);
    private LatLng posCar = new LatLng(latitudePositionCar, longitudePositionCar);

    private static final String TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID = "TURF_CALCULATION_FILL_LAYER_GEOJSON_SOURCE_ID";
    private static final String TURF_CALCULATION_FILL_LAYER_ID = "TURF_CALCULATION_FILL_LAYER_ID";
    private static final String CIRCLE_CENTER_SOURCE_ID = "CIRCLE_CENTER_SOURCE_ID";
    private static final String CIRCLE_CENTER_ICON_ID = "CIRCLE_CENTER_ICON_ID";
    private static final String CIRCLE_CENTER_LAYER_ID = "CIRCLE_CENTER_LAYER_ID";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

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
                        markerDisplay(1,1, date, heure, posCar, puissance, ecu);
//                        chooseMapStyle(tcuConnected, posCar, Integer.toString(R.string.idcar1));
                        symbolManager.addClickListener(new OnSymbolClickListener() {
                            @Override
                            public void onAnnotationClick(Symbol symbol) {
                                try {
                                    marqueur = Integer.parseInt(originIcon.getIconImage());
                                } catch(NumberFormatException nfe) {
                                    System.out.println("Could not parse " + nfe);
                                }
                                if(marqueur == R.string.idcar1){
                                    typeToast = 3;
                                    Timber.d("onAnnotationClick: type %s", originIcon.getIconImage());
                                    Timber.d("onAnnotationClick: type int %s", R.string.idcar1);
                                    showTypeToast(typeToast, getString(R.string.idcar1));
                                }
                                if(marqueur == R.string.idcar2){
                                    typeToast = 4;
                                    Timber.d("onAnnotationClick: type %s", originIcon.getIconImage());
                                    Timber.d("onAnnotationClick: type int %s", R.string.idcar2);
                                    showTypeToast(typeToast, getString(R.string.idcar2));
                                }
                            }
                        });

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
                markerDisplay(3,5, date, heure, carStellantis, puissance, ecu);
//                if(stellantisId == 1) StellantisCar(carStellantis, stellantisId);
//                if(stellantisId == 2) StellantisCar(carStellantis2, stellantisId);

            });

            btnOther = requireView().findViewById(R.id.btnOther);
            btnOther.setOnClickListener(v -> {

                LatLng carOther = new LatLng(48.796768,1.978605);
                LatLng carOther2 = new LatLng(48.798772218369194,1.9941396447244915);
                markerDisplay(2,idMarkerOther,date, heure, carOther, puissance, ecu);
                idMarkerOther++;
                //OtherCar(carOther, carOther2, Integer.toString(R.string.idcar2));
            });

            btnTCU = requireView().findViewById(R.id.btnConnectionTCU);
            btnTCU.setOnClickListener(v -> {
                tcuConnected = 1;
                //btnWaiting.setVisibility(View.GONE);
                LatLng carStellantis2 = new LatLng(48.798734,2.000806);
                markerDisplay(1,1,date, heure, carStellantis2, puissance, ecu);
                //chooseMapStyle(tcuConnected,posCar, Integer.toString(R.string.idcar1));
            });

            btnMoveCar = requireView().findViewById(R.id.btnMoveCar);
            btnMoveCar.setOnClickListener(v -> {
                longitudePositionCar = 1.958275;
                latitudePositionCar = 48.799861;
                posCar = new LatLng(latitudePositionCar, longitudePositionCar);
                markerDisplay(2,9,date, heure, posCar, puissance, ecu);
                //chooseMapStyle(tcuConnected,posCar, Integer.toString(R.string.idcar1));
            });

            btnCrash = requireView().findViewById(R.id.btnCrash);
            btnCrash.setOnClickListener(v ->
            {
                if(typeToast >= 2) typeToast = 0;
                typeToast ++;
                showTypeToast(typeToast, getString(R.string.idcar1));
            });
        });

        binding.fbLoc.setOnClickListener(v -> {
            setCameraPosition(positionCar);
            initPolygonCircleFillLayer();
            drawPolygonCircle(positionCar);
        });

        return root;
    }

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

    private void StellantisCar(LatLng point, int stellantisId){
        if(!idStellantis.contains(stellantisId)){
            idStellantis.add(stellantisId);
            Bitmap stellantis = BitmapFactory.decodeResource(getResources(), R.drawable.green_car);
            Objects.requireNonNull(map.getStyle()).addImage("stellantis",stellantis);
            options.add(new SymbolOptions()
                    .withLatLng(point)
                    .withIconImage("stellantis")
                    .withIconSize(1f)
                    .withIconOffset(new Float[] {0f,-1.5f})
                    .withZIndex(10)
                    .withTextHaloColor("rgba(255, 255, 255, 100)")
                    .withTextHaloWidth(5.0f)
                    .withTextAnchor("top")
                    .withTextOffset(new Float[] {0f, 1.5f})
                    .setDraggable(false)
            );
            symbols = symbolManager.create(options);
        }
        if(idStellantis.contains(stellantisId)){
            options.add(new SymbolOptions()
                    .withLatLng(point)
                    .withIconImage("stellantis")
                    .withIconSize(1f)
                    .withIconOffset(new Float[] {0f,-1.5f})
                    .withZIndex(10)
                    .withTextHaloColor("rgba(255, 255, 255, 100)")
                    .withTextHaloWidth(5.0f)
                    .withTextAnchor("top")
                    .withTextOffset(new Float[] {0f, 1.5f})
                    .setDraggable(false)
            );
        }
    }

    private void OtherCar(LatLng point, LatLng point2, String idCar){
        Bitmap other1 = BitmapFactory.decodeResource(getResources(), R.drawable.yellow_car);
        Objects.requireNonNull(map.getStyle()).addImage(idCar, other1);
        switch (otherId){
            case 0:
                originIcon = symbolManager.create(new SymbolOptions()
                        .withLatLng(point)
                        .withIconImage(idCar)
                        .withIconSize(1f)
                        .withIconOffset(new Float[] {0f,-1.5f})
                        .withZIndex(10)
                        .withTextHaloColor("rgba(255, 255, 255, 100)")
                        .withTextHaloWidth(5.0f)
                        .withTextAnchor("top")
                        .withTextOffset(new Float[] {0f, 1.5f})
                        .setDraggable(false));
                break;
            case 1:
                originIcon = symbolManager.create(new SymbolOptions()
                        .withLatLng(point2)
                        .withIconImage(idCar)
                        .withIconSize(1f)
                        .withIconOffset(new Float[] {0f,-1.5f})
                        .withZIndex(10)
                        .withTextHaloColor("rgba(255, 255, 255, 100)")
                        .withTextHaloWidth(5.0f)
                        .withTextAnchor("top")
                        .withTextOffset(new Float[] {0f, 1.5f})
                        .setDraggable(false));
                break;
            case 2:
                otherId = 0;
                break;
        }
        otherId++;
    }

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

    private void MoveCar(LatLng posCar){
        /**
         * On recupere les données du tcu, on appelle updateNewLocation() pour mettre à jour la position de la voiture, et ensuite on bouge
         */
        //updateNewLocation();
        //chooseMapStyle(tcuConnected);
        Bitmap blueCar = BitmapFactory.decodeResource(getResources(), R.drawable.blue_car);
        Objects.requireNonNull(map.getStyle()).addImage("Car",blueCar);
        options.add(new SymbolOptions()
                .withLatLng(posCar)
                .withIconImage("Car")
                .withIconSize(1f)
                .withIconOffset(new Float[] {0f,-1.5f})
                .withZIndex(10)
                .withTextHaloColor("rgba(255, 255, 255, 100)")
                .withTextHaloWidth(5.0f)
                .withTextAnchor("top")
                .withTextOffset(new Float[] {0f, 1.5f})
                .setDraggable(false)
        );
        map.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(posCar)
                        .zoom(13.6)
                        .build()
        ));
        symbols = symbolManager.create(options);
        //initPolygonCircleFillLayer();
        //drawPolygonCircle(positionCar);
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

    private void chooseMapStyle(int tcuConnected, LatLng posCar, String idCar){
        switch (tcuConnected){
            case 0:
                Bitmap redCar = BitmapFactory.decodeResource(getResources(), R.drawable.red_car);
                Objects.requireNonNull(map.getStyle()).addImage(idCar,redCar);
                originIcon = symbolManager.create(new SymbolOptions()
                        .withLatLng(posCar)
                        .withIconImage(idCar)
                        .withIconSize(1f)
                        .withIconOffset(new Float[] {0f,-1.5f})
                        .withZIndex(10)
                        .withTextHaloColor("rgba(255, 255, 255, 100)")
                        .withTextHaloWidth(5.0f)
                        .withTextAnchor("top")
                        .withTextOffset(new Float[] {0f, 1.5f})
                        .setDraggable(false));
                map.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(posCar)
                                .zoom(13.6)
                                .build()
                ));
                //fillLayerCounter = 0;
                initPolygonCircleFillLayer();
                drawPolygonCircle(positionCar);
                break;
            case 1:
                if(originIcon == null){
                    Bitmap blueCar = BitmapFactory.decodeResource(getResources(), R.drawable.blue_car);
                    Objects.requireNonNull(map.getStyle()).addImage(idCar,blueCar);
                    options.add(new SymbolOptions()
                            .withLatLng(posCar)
                            .withIconImage(idCar)
                            .withIconSize(1f)
                            .withIconOffset(new Float[] {0f,-1.5f})
                            .withZIndex(10)
                            .withTextHaloColor("rgba(255, 255, 255, 100)")
                            .withTextHaloWidth(5.0f)
                            .withTextAnchor("top")
                            .withTextOffset(new Float[] {0f, 1.5f})
                            .setDraggable(false)
                    );
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(posCar)
                                    .zoom(13.6)
                                    .build()
                    ));
//                    originIcon = symbolManager.create(new SymbolOptions()
//                            .withLatLng(posCar)
//                            .withIconImage("Car")
//                            .withIconSize(1f)
//                            .withIconOffset(new Float[] {0f,-1.5f})
//                            .withZIndex(10)
//                            .withTextHaloColor("rgba(255, 255, 255, 100)")
//                            .withTextHaloWidth(5.0f)
//                            .withTextAnchor("top")
//                            .withTextOffset(new Float[] {0f, 1.5f})
//                            .setDraggable(false));
//                    symbols = symbolManager.create(options);
                    //fillLayerCounter = 0;
                    //initPolygonCircleFillLayer();
                    drawPolygonCircle(positionCar);
                }
                else{
                    Bitmap blueCar = BitmapFactory.decodeResource(getResources(), R.drawable.blue_car);
                    Objects.requireNonNull(map.getStyle()).addImage(idCar,blueCar);
                    originIcon.setLatLng(posCar);
                    originIcon.setIconImage(idCar);
                    symbolManager.update(originIcon);
                    //fillLayerCounter = 0;
                    //initPolygonCircleFillLayer();
                    drawPolygonCircle(positionCar);
                }
                break;
        }
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

    public void markerDisplay(int typeMarker, int idMarker, String date, String heure, LatLng positionMarker, int puissance, int ecu) {
        String isMarkerString = Integer.toString(idMarker);
        switch (tcuConnected) {
            case 0:
                List<MarkerOptions> markerTCUNOK = new ArrayList<>();
                Icon red = IconFactory.getInstance(requireContext()).fromResource(R.drawable.red_car);
                String carInfoNOK = "Date : " + date + ", \n" +
                        "Heure : " + heure + ", \n" +
                        "Position : " + isMarkerString + ", \n" +
                        "Puissance : " + puissance + ", \n" +
                        "ECU : " + ecu;

                markerTCUNOK.add(new MarkerOptions()
                        .position(positionMarker)
                        .title("ID : " + idMarker)
                        .snippet(carInfoNOK)
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
//                        Bitmap blueCar = BitmapFactory.decodeResource(getResources(), R.drawable.blue_car);
//                        Objects.requireNonNull(map.getStyle()).addImage(isMarkerString, blueCar);
//                        map.animateCamera(CameraUpdateFactory.newCameraPosition(
//                                new CameraPosition.Builder()
//                                        .target(positionMarker)
//                                        .zoom(13.6)
//                                        .build()
//                        ));
//                        originIcon.setLatLng(positionMarker);
//                        originIcon.setIconImage(isMarkerString);
//                        symbolManager.update(originIcon);
//                        drawPolygonCircle(positionCar);
//                        symbols.add(originIcon);
//                        Timber.d("markerDisplay: %s", symbols);

                        List<MarkerOptions> markerTCUConnected = new ArrayList<>();
                        Icon blue = IconFactory.getInstance(requireContext()).fromResource(R.drawable.blue_car);
                        String carInfoOK = "Date : " + date + ", \n" +
                                "Heure : " + heure + ", \n" +
                                "Position : " + isMarkerString + ", \n" +
                                "Puissance : " + puissance + ", \n" +
                                "ECU : " + ecu;

                        markerTCUConnected.add(new MarkerOptions()
                                    .position(positionMarker)
                                    .title("ID : " + idMarker)
                                    .snippet(carInfoOK)
                                    .icon(blue)
                        );
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(positionMarker)
                                        .zoom(13.6)
                                        .build()
                        ));
                        map.addMarkers(markerTCUConnected);
                        break;
                    case 2:
                        List<MarkerOptions> markers = new ArrayList<>();
                        Icon yellow = IconFactory.getInstance(requireContext()).fromResource(R.drawable.yellow_car);
                        String carInfoOther = "Date : " + date + ", \n" +
                                "Heure : " + heure + ", \n" +
                                "Position : " + isMarkerString + ", \n" +
                                "Puissance : " + puissance + ", \n" +
                                "ECU : " + ecu;

                                markers.add(new MarkerOptions()
                                    .position(positionMarker)
                                    .title("ID : " + idMarker)
                                    .snippet(carInfoOther)
                                    .icon(yellow)
                            );

//                        if (idMarker == 9) {
//                            markers.add(new MarkerOptions()
//                                    .position(positionMarker)
//                                    .title("ID : " + idMarker)
//                                    .snippet(all)
//                                    .icon(yellow)
//                            );
//                        }
//                        if (idMarker == 10) {
//                            markers.add(new MarkerOptions()
//                                    .position(posCar)
//                                    .title("ID : " + idMarker)
//                                    .snippet("Date : " + date)
//                                    .snippet("Heure : " + heure)
//                                    .icon(yellow)
//                            );
//                        }
                        map.addMarkers(markers);
                        Timber.d("IDMarkerOther : %s", idMarkerOtherList);
                }
                break;
            case 3:
                Bitmap stellantisCar = BitmapFactory.decodeResource(getResources(), R.drawable.green_car);
                Objects.requireNonNull(map.getStyle()).addImage(isMarkerString, stellantisCar);
                if (idMarkerStellantisList.contains(idMarker)) {
                    originIcon.setLatLng(positionMarker);
                    originIcon.setIconImage(isMarkerString);
                    symbolManager.update(originIcon);
                } else {
                    options.add(new SymbolOptions()
                            .withLatLng(positionMarker)
                            .withIconImage(isMarkerString)
                            .withIconSize(1f)
                            .withIconOffset(new Float[]{0f, -1.5f})
                            .withZIndex(10)
                            .withTextHaloColor("rgba(255, 255, 255, 100)")
                            .withTextHaloWidth(5.0f)
                            .withTextAnchor("top")
                            .withTextOffset(new Float[]{0f, 1.5f})
                            .setDraggable(false)
                    );
                    idMarkerStellantisList.add(idMarker);
                }
                Timber.d("IDMarkerStellantis : %s", idMarkerStellantisList);
                break;

            default:
                break;
        }
    }
}