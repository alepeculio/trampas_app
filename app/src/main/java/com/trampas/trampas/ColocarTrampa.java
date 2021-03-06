package com.trampas.trampas;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.trampas.trampas.Adaptadores.AdaptadorListaTrampasColocar;
import com.trampas.trampas.Adaptadores.LocalizacionInterface;
import com.trampas.trampas.Adaptadores.MostrarTrampasColocadasInterface;
import com.trampas.trampas.BD.BDCliente;
import com.trampas.trampas.BD.BDInterface;
import com.trampas.trampas.BD.RespuestaTrampas;
import com.trampas.trampas.Clases.Trampa;
import com.trampas.trampas.Clases.Usuario;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ColocarTrampa extends Fragment implements LocalizacionInterface {
    public static final int SOLICITUD_LOCALIZACION = 1;
    Usuario usuario;
    List<Trampa> trampas;
    @BindView(R.id.listaTrampas)
    RecyclerView mRecyclerView;
    @BindView(R.id.swipeRefresh)
    SwipeRefreshLayout swipeRefresh;
    @BindView(R.id.noHayTrampas)
    LinearLayout noHayTrampas;
    @BindView(R.id.tvNoHayTrampas)
    TextView tvNoHayTrampas;
    @BindView(R.id.llProgressBar)
    LinearLayout llProgressBar;
    @BindView(R.id.tvProgressBar)
    TextView tvProgressBar;
    Location ubicacionActual = null;
    private MostrarTrampasColocadasInterface mpi;
    private AdaptadorListaTrampasColocar adaptadorListaTrampasColocar;
    private SearchView searchView = null;
    private SearchView.OnQueryTextListener queryTextListener;
    private String ultimaBusqueda = null;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Context mContext;

    public ColocarTrampa() {
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_colocar_trampa, container, false);
        ButterKnife.bind(this, view);

        if (usuario == null && mContext != null)
            usuario = ((MenuPrincipal) mContext).getUsuario();

        //Obtener provedor de localizacion de google.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        createLocationRequest();

        setAdaptadorListaTrampas();

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                cargarTrampas();
            }
        });
        cargarTrampas();
        return view;
    }

    private void createLocationRequest() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            SettingsClient client = LocationServices.getSettingsClient(mContext);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

            task.addOnSuccessListener((MenuPrincipal) mContext, new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    startLocationUpdates();
                }
            });

            task.addOnFailureListener((MenuPrincipal) mContext, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult((MenuPrincipal) mContext, SOLICITUD_LOCALIZACION);
                        } catch (IntentSender.SendIntentException sendEx) {
                            Log.e("task onFailureListener", sendEx.getMessage());
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SOLICITUD_LOCALIZACION) {
            if (resultCode == -1)
                startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        } else {
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null)
                        return;

                    for (Location location : locationResult.getLocations()) {
                        if (ubicacionActual == null) {
                            llProgressBar.setVisibility(View.GONE);
                        }
                        ubicacionActual = location;
                        Log.d("Localizacion: ", location.getLatitude() + " " + location.getLongitude());
                    }
                }
            };
            if (ubicacionActual == null)
                llProgressBar.setVisibility(View.VISIBLE);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    createLocationRequest();
                }
            }
        } else {
            Toast.makeText(mContext, "Su ubicación es necesaria para colocar la trampa.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopLocationUpdates() {
        if (mLocationCallback != null)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    public void setAdaptadorListaTrampas() {
        if (adaptadorListaTrampasColocar == null) {
            adaptadorListaTrampasColocar = new AdaptadorListaTrampasColocar(new ArrayList<Trampa>(), mContext, this);
            adaptadorListaTrampasColocar.setUsuario(usuario);
            mRecyclerView.setAdapter(adaptadorListaTrampasColocar);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            mRecyclerView.setHasFixedSize(true);
        }
    }

    private void filtrarTrampas() {
        List<Trampa> trampasFinal = new ArrayList<>();
        Boolean busquedaVacia = false;
        if (trampas != null) {
            if (ultimaBusqueda != null) {
                for (Trampa t : trampas) {
                    if (t.getNombre().toLowerCase().contains(ultimaBusqueda) || String.valueOf(t.getId()).toLowerCase().contains(ultimaBusqueda)) {
                        trampasFinal.add(t);
                    }
                }

                if (trampasFinal.size() == 0 && !ultimaBusqueda.equals("")) {
                    tvNoHayTrampas.setText("No hay resultados para \"" + ultimaBusqueda + "\"");
                    busquedaVacia = true;
                }

            } else {
                trampasFinal = trampas;
            }
        }

        if (trampasFinal.size() == 0) {
            if (!busquedaVacia)
                tvNoHayTrampas.setText(R.string.no_hay_trampas_para_colocar);

            noHayTrampas.setVisibility(View.VISIBLE);
        } else {
            noHayTrampas.setVisibility(View.GONE);
        }
        adaptadorListaTrampasColocar.actualizarTrampas(trampasFinal);
        swipeRefresh.setRefreshing(false);
    }

    private void cargarTrampas() {
        swipeRefresh.setRefreshing(true);
        BDInterface bd = BDCliente.getClient().create(BDInterface.class);
        Call<RespuestaTrampas> call = bd.obtenerTrampasNoColocadas();
        call.enqueue(new Callback<RespuestaTrampas>() {
            @Override
            public void onResponse(Call<RespuestaTrampas> call, Response<RespuestaTrampas> response) {
                if (response.body() != null) {
                    if (response.body().getCodigo().equals("1")) {
                        trampas = response.body().getTrampas();
                        filtrarTrampas();
                    } else {
                        trampas = null;
                        filtrarTrampas();
                        Toast.makeText(mContext, response.body().getMensaje(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    trampas = null;
                    filtrarTrampas();
                    Toast.makeText(mContext, R.string.error_interno_servidor, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RespuestaTrampas> call, Throwable t) {
                trampas = null;
                filtrarTrampas();
                Toast.makeText(mContext, R.string.error_conexion_servidor, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) mContext.getSystemService(mContext.SEARCH_SERVICE);

        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(((MenuPrincipal) mContext).getComponentName()));

            queryTextListener = new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    ultimaBusqueda = newText.toLowerCase().trim();
                    filtrarTrampas();
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    ultimaBusqueda = query.toLowerCase().trim();
                    filtrarTrampas();
                    return true;
                }
            };
            searchView.setOnQueryTextListener(queryTextListener);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof MostrarTrampasColocadasInterface) {
            mpi = (MostrarTrampasColocadasInterface) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement AgregarParadaInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stopLocationUpdates();
    }

    @Override
    public Location obtenerLocalizacion() {
        if (ubicacionActual == null) {
            if (llProgressBar.getVisibility() == View.VISIBLE) {
                Toast.makeText(mContext, "Estableciendo ubicación, aguarde porfavor.", Toast.LENGTH_SHORT).show();
            } else {
                createLocationRequest();
            }
        }
        return ubicacionActual;
    }









    /*Otra manera de obtener la lozalizacion (Sin api de google)*/
    /*
    LocationManager locationManager;
    LocationListener locationListenerCoarse;
    LocationListener locationListenerFine;
     */

    /*-----------------------------------------------------------------------------------------*/
    /*Definir los listener en el oncreateview del fragment*/
        /*
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationListenerFine = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                ubicacionActual = location;
                llProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                if (ubicacionActual == null && provider.equals("gps")) {
                    tvProgressBar.setText(R.string.obteniendo_ubicacion_gps);
                    llProgressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onProviderEnabled(String provider) {
                if (ubicacionActual == null && provider.equals("gps")) {
                    tvProgressBar.setText(R.string.obteniendo_ubicacion_gps);
                    llProgressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        locationListenerCoarse = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                ubicacionActual = location;
                llProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                if (ubicacionActual == null && provider.equals("network")) {
                    tvProgressBar.setText(R.string.obteniendo_ubicacion_red);
                    llProgressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onProviderEnabled(String provider) {
                if (ubicacionActual == null && provider.equals("network")) {
                    tvProgressBar.setText(R.string.obteniendo_ubicacion_red);
                    llProgressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        iniciarLocalizacion();*/

    /*------------------------------------------------------------------------------------------*/

    /* public void mensajeEncenderUbicacion() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("Ubicación apagada");
        alertDialog.setMessage("¿Desea ir al menú para encenderla?");
        alertDialog.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                getActivity().startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

        public void iniciarLocalizacion() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            Location g = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location n = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (g != null)
                ubicacionActual = g;
            else if (n != null)
                ubicacionActual = n;

            String fineProvider = locationManager.getBestProvider(createFineCriteria(), false);
            String coarseProvider = locationManager.getBestProvider(createCoarseCriteria(), true);

            if (fineProvider != null) {
                locationManager.requestLocationUpdates(fineProvider, 0, 0f, locationListenerFine);
                locationListenerFine.onStatusChanged(fineProvider, 1, null);
            }

            if (coarseProvider != null) {
                if (coarseProvider.equals("passive")) {
                    llProgressBar.setVisibility(View.GONE);
                    mensajeEncenderUbicacion();
                    locationManager.requestLocationUpdates("network", 2000, 20f, locationListenerCoarse);
                } else {
                    locationManager.requestLocationUpdates(coarseProvider, 2000, 20f, locationListenerCoarse);
                    locationListenerCoarse.onStatusChanged(coarseProvider, 1, null);
                }

            }

        }

    }

    public static Criteria createFineCriteria() {
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        c.setAltitudeRequired(false);
        c.setBearingRequired(false);
        c.setSpeedRequired(false);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_HIGH);
        return c;
    }

    public static Criteria createCoarseCriteria() {
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_COARSE);
        c.setAltitudeRequired(false);
        c.setBearingRequired(false);
        c.setSpeedRequired(false);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_HIGH);
        return c;

    }
*/
}