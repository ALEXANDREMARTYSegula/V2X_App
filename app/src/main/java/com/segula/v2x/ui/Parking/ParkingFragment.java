package com.segula.v2x.ui.Parking;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.mapbox.geojson.Point;
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
import com.segula.v2x.R;
import com.segula.v2x.databinding.FragmentParkingBinding;
import com.segula.v2x.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import timber.log.Timber;

public class ParkingFragment extends Fragment implements OnMapReadyCallback {

    //Map
    private MapView mapView;
    private MapboxMap map;

    private ParkingViewModel parkingViewModel;
    private FragmentParkingBinding binding;

    //Lat & Long
    private double latitudePositionCar = Utils.lastLocationLatitudeParking;
    private double longitudePositionCar =  Utils.lastLocationLongitudeParking;
    private Point positionCar = Point.fromLngLat(longitudePositionCar, latitudePositionCar);
    private LatLng posCar = new LatLng(latitudePositionCar, longitudePositionCar);

    //Markers
    List<MarkerOptions> markerTCUNOK = new ArrayList<>();
    Marker markerTCUConnected;
    private SymbolManager symbolManager;
    private Symbol symbol;
    private List<Symbol> symbols = new ArrayList<>();

    private int tcuConnected = 0, puissance = 0, ecu = 0, move = 0, rotate = 0;
    Button btnTCU, btnMoveCar;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        parkingViewModel =
                new ViewModelProvider(this).get(ParkingViewModel.class);

        Mapbox.getInstance(this.requireContext(), getString(R.string.access_token));
        binding = FragmentParkingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mapView = root.findViewById(R.id.mapview);

        mapView.getMapAsync(mapboxMap -> {
            this.map = mapboxMap;
            mapboxMap.setStyle(new Style.Builder().fromUrl(Style.TRAFFIC_DAY)
                    , style -> {
                this.map = mapboxMap;
                symbolManager = new SymbolManager(mapView, map, style);
                symbolManager.setIconAllowOverlap(true);  //your choice t/f
                symbolManager.setTextAllowOverlap(true);  //your choice t/f
                markerDisplay(1,1, posCar, puissance, ecu, 0);

                symbolManager.addClickListener(new OnSymbolClickListener() {
                    @Override
                    public void onAnnotationClick(Symbol symbol) {
                        Timber.d("Hello");
                        //Toast.makeText(requireView(),"clicked  " + symbol.getTextField().toLowerCase().toString(),Toast.LENGTH_SHORT).show();
                    }
                });

                map.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(posCar)
                                .zoom(20)
                                .build()
                ));
            });

            btnTCU = requireView().findViewById(R.id.btnConnectionTCU);
            btnTCU.setOnClickListener(v -> {
                tcuConnected = 1;
                markerDisplay(1,1,posCar, puissance, ecu, rotate);
            });

            btnMoveCar = requireView().findViewById(R.id.btnMoveCar);
            btnMoveCar.setOnClickListener(v -> {
                posCar = new LatLng(latitudePositionCar, longitudePositionCar);
                if(move == 0){
                    latitudePositionCar = 48.79631;
                    longitudePositionCar = 1.98491;
                    rotate = -75;
                }
                if(move == 1 ){
                    latitudePositionCar = 48.79626;
                    longitudePositionCar = 1.98483;
                    rotate = -100;
                }
                if(move == 2){
                    latitudePositionCar = 48.79620;
                    longitudePositionCar = 1.98470;
                    rotate = -125;
                }
                if(move == 3){
                    latitudePositionCar = 48.79617;
                    longitudePositionCar = 1.98461;
                    rotate = -15;
                }
                if(move == 4){
                    latitudePositionCar = 48.79614;
                    longitudePositionCar = 1.98446;
                }
                if(move == 5){
                    latitudePositionCar = 48.79611;
                    longitudePositionCar = 1.98426;
                }
                markerDisplay(1,1, posCar, puissance, ecu, rotate);
                move ++;
            });
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
    }

    public void markerDisplay(int typeMarker, int idMarker,LatLng positionMarker, int puissance, int ecu, float rotate) {
        String carInfo = "Date : " + android.text.format.DateFormat.format("dd/MM/yyyy", new java.util.Date()) + "\n" +
                "Heure : " + SimpleDateFormat.getTimeInstance().format(new java.util.Date()) + "\n" +
                "Position : " + positionMarker + "\n" +
                "Puissance : " + puissance + "\n" +
                "ECU : " + ecu;
        switch (tcuConnected) {
            case 0:
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.red_car);
                map.getStyle().addImage("my-marker",bm);
                //Icon red = IconFactory.getInstance(requireContext()).fromResource(R.drawable.red_car);

                symbol = symbolManager.create(new SymbolOptions()
                        .withLatLng(positionMarker)
                        .withIconImage("my-marker")
                        .withIconRotate(rotate)
                        //set the below attributes according to your requirements
//                        .withIconSize(1.5f)
//                        .withIconOffset(new Float[] {0f,-1.5f})
//                        .withZIndex(10)
//                        .withTextField("hello")
//                        .withTextHaloColor("rgba(255, 255, 255, 100)")
//                        .withTextHaloWidth(5.0f)
//                        .withTextAnchor("top")
//                        .withTextOffset(new Float[] {0f, 1.5f})
//                        .setDraggable(false)
                );

                map.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(positionMarker)
                                .zoom(20)
                                .build()
                ));
                break;

            case 1:
                switch (typeMarker) {
                    case 1:
                        Bitmap bluecar = BitmapFactory.decodeResource(getResources(), R.drawable.blue_car);
                        map.getStyle().addImage("bluecar",bluecar);
                        if(markerTCUConnected == null){
                            symbolManager.delete(symbol);
                            //symbolManager.delete(symbol);
                            map.removeAnnotations();
                            symbol = symbolManager.create(new SymbolOptions()
                                    .withLatLng(positionMarker)
                                    .withIconImage("bluecar")
                                    .withIconRotate(rotate)
                            );

                            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                    new CameraPosition.Builder()
                                            .target(positionMarker)
                                            .zoom(20)
                                            .build()
                            ));
                            positionCar = Point.fromLngLat(positionMarker.getLongitude(), positionMarker.getLatitude());
                        }
                        else{
                            symbol.setLatLng(positionMarker);
                            symbol.setIconRotate(rotate);
//                            markerTCUConnected.setPosition(positionMarker);
//                            markerTCUConnected.setSnippet(carInfo);

                            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                    new CameraPosition.Builder()
                                            .target(positionMarker)
                                            .zoom(20)
                                            .build()
                            ));
                            positionCar = Point.fromLngLat(positionMarker.getLongitude(), positionMarker.getLatitude());
                        }
                        break;
                }
            default:
                break;
        }
    }

    /*public void markerDisplay(int typeMarker, int idMarker,LatLng positionMarker, int puissance, int ecu) {
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
                                .zoom(20)
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

                            markerTCUConnected = map.addMarker(new MarkerOptions()
                                    .position(positionMarker)
                                    .title("ID : " + idMarker)
                                    .snippet(carInfo)
                                    .icon(blue));

                            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                    new CameraPosition.Builder()
                                            .target(positionMarker)
                                            .zoom(20)
                                            .build()
                            ));
                            positionCar = Point.fromLngLat(positionMarker.getLongitude(), positionMarker.getLatitude());
                        }
                        else{
                            markerTCUConnected.setPosition(positionMarker);
                            markerTCUConnected.setSnippet(carInfo);

                            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                    new CameraPosition.Builder()
                                            .target(positionMarker)
                                            .zoom(20)
                                            .build()
                            ));
                            positionCar = Point.fromLngLat(positionMarker.getLongitude(), positionMarker.getLatitude());
                        }
                        break;
                }
            default:
                break;
        }
    }*/
}