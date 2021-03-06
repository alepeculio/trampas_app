package com.trampas.trampas;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.trampas.trampas.BD.BDCliente;
import com.trampas.trampas.BD.BDInterface;
import com.trampas.trampas.BD.Respuesta;
import com.trampas.trampas.BD.RespuestaColocaciones;
import com.trampas.trampas.Clases.Colocacion;
import com.trampas.trampas.Clases.Usuario;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MostrarTrampasColocadas extends Fragment {
    public static final int SOLICITUD_OBTENER_POSICION_ACTUAL = 111;
    @BindView(R.id.llGuardar)
    LinearLayout llGuardar;
    @BindView(R.id.btnGuardar)
    Button btnGuardar;
    @BindView(R.id.tvDesde)
    TextView tvDesde;
    @BindView(R.id.tvHasta)
    TextView tvHasta;
    @BindView(R.id.btnBuscar)
    Button btnBuscar;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.btnMasInformacion)
    LinearLayout btnMasInformacion;
    @BindView(R.id.btnExportar)
    LinearLayout btnExportar;
    @BindView(R.id.llProgressBarMapa)
    LinearLayout llProgressBarMapa;
    Calendar calendario = Calendar.getInstance();
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private Marker marcador;
    private List<Marker> marcadores;
    private List<Colocacion> colocaciones;
    private Usuario usuario;
    private String idColocacionCreada;
    private FusedLocationProviderClient mFusedLocationClient;
    private Context mContext;

    public MostrarTrampasColocadas() {
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setIdColocacionCreada(String idColocacionCreada) {
        this.idColocacionCreada = idColocacionCreada;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_mostrar_trampas_colocadas, container, false);
        ButterKnife.bind(this, v);

        if (usuario == null && mContext != null)
            usuario = ((MenuPrincipal) mContext).getUsuario();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

        final DatePickerDialog.OnDateSetListener fechaDesde = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendario.set(Calendar.YEAR, year);
                calendario.set(Calendar.MONTH, monthOfYear);
                calendario.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String myFormat = "dd/MM/yyyy";
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
                tvDesde.setText(sdf.format(calendario.getTime()));
            }


        };

        tvDesde.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog1 = new DatePickerDialog(mContext,
                        fechaDesde,
                        calendario.get(Calendar.YEAR),
                        calendario.get(Calendar.MONTH),
                        calendario.get(Calendar.DAY_OF_MONTH));

                dialog1.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            tvDesde.setText("");
                        }
                    }
                });

                dialog1.show();
            }
        });

        final DatePickerDialog.OnDateSetListener fechaHasta = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendario.set(Calendar.YEAR, year);
                calendario.set(Calendar.MONTH, monthOfYear);
                calendario.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String myFormat = "dd/MM/yyyy";
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
                tvHasta.setText(sdf.format(calendario.getTime()));
            }

        };

        tvHasta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog2 = new DatePickerDialog(mContext,
                        fechaHasta,
                        calendario.get(Calendar.YEAR),
                        calendario.get(Calendar.MONTH),
                        calendario.get(Calendar.DAY_OF_MONTH));

                dialog2.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            tvHasta.setText("");
                        }
                    }
                });

                dialog2.show();
            }
        });


        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtrarColocaciones();
            }
        });

        new LongOperationMap().execute();
        return v;
    }

    private void filtrarColocaciones() {
        if (colocaciones == null) {
            Toast.makeText(mContext, "No hay trampas colocadas", Toast.LENGTH_SHORT).show();
            return;
        }

        final String fechaDesde = tvDesde.getText().toString();
        String fechaHasta = tvHasta.getText().toString();
        Date inicio = null;
        Date fin = null;
        List<Colocacion> colocs = new ArrayList<>();

        String myFormat = "dd/MM/yyyy";
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
        try {
            inicio = sdf.parse(fechaDesde);
            fin = sdf.parse(fechaHasta);
        } catch (ParseException pe) {
            if (inicio == null) {
                Toast.makeText(mContext, R.string.seleccionar_fecha_inicio, Toast.LENGTH_SHORT).show();
                return;
            }
            if (fin == null) {
                fin = new Date();
                fechaHasta = sdf.format(fin);
                tvHasta.setText(fechaHasta);
            }


        }

        if (inicio.after(fin)) {
            Toast.makeText(mContext, "La fecha de comienzo debe ser anterior a la de finalización", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Colocacion c : colocaciones) {
            Date fColocacion = null;
            try {
                fColocacion = sdf.parse(convertFormat(c.getFechaInicio()));
            } catch (ParseException pe) {
                pe.printStackTrace();
            }

            if ((fColocacion.after(inicio) || fColocacion.equals(inicio)) && (fColocacion.before(fin) || fColocacion.equals(fin)))
                colocs.add(c);

        }

        final String fechaHastaFinal = fechaHasta;

        if (colocs.size() > 0 && usuario.getAdmin() == 1) {
            btnExportar.setVisibility(View.VISIBLE);
            btnExportar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, ExportarDatos.class);
                    intent.putExtra("usuario", usuario);
                    intent.putExtra("desde", fechaDesde);
                    intent.putExtra("hasta", fechaHastaFinal);
                    startActivity(intent);
                }
            });
        } else
            btnExportar.setVisibility(View.GONE);

        prepararMapa(colocs);
    }


    private void cargarColocaciones() {
        BDInterface bd = BDCliente.getClient().create(BDInterface.class);
        Call<RespuestaColocaciones> call = bd.obtenerColocaciones();
        call.enqueue(new Callback<RespuestaColocaciones>() {
            @Override
            public void onResponse(Call<RespuestaColocaciones> call, Response<RespuestaColocaciones> response) {
                llProgressBarMapa.setVisibility(View.GONE);
                if (response.body() != null) {
                    if (response.body().getCodigo().equals("1")) {
                        colocaciones = response.body().getColocaciones();
                        prepararMapa(null);
                    } else {
                        colocaciones = null;
                        Toast.makeText(mContext, response.body().getMensaje(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    colocaciones = null;
                    Toast.makeText(mContext, R.string.error_interno_servidor, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RespuestaColocaciones> call, Throwable t) {
                llProgressBarMapa.setVisibility(View.GONE);
                colocaciones = null;
                Toast.makeText(mContext, R.string.error_conexion_servidor, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void prepararMapa(List<Colocacion> colocs) {
        if (colocaciones != null) {
            if (marcadores == null)
                marcadores = new ArrayList<>();

            for (Marker m : marcadores)
                m.remove();

            marcadores.clear();
            btnMasInformacion.setVisibility(View.GONE);

            if (colocs == null) {
                colocs = colocaciones;
            } else if (colocs.size() == 0) {
                Toast.makeText(mContext, "Sin resultados.", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Colocacion c : colocs) {
                Marker m = mMap.addMarker(new MarkerOptions().position(new LatLng(c.getLat(), c.getLon())).title(c.getTrampa().getNombre()));
                if (c.getFechaFin() == null) {
                    m.setIcon(bitmapDescriptorFromVector(getActivity(), R.drawable.fondo_celeste_trampa));
                    if (c.getUsuario() == usuario.getId()) {
                        m.setDraggable(true);
                        //m.setIcon(getIcon(R.mipmap.ic_trampas_fondo_celeste_round));
                        m.setSnippet(mContext.getString(R.string.mensaje_editar_ubicacion));
                    } else {
                        //m.setIcon(getIcon(R.mipmap.ic_trampas_fondo_blanco_round));
                        m.setSnippet("Actualmente colocada");
                    }
                } else {
                    m.setSnippet("Ver gráfica");
                    m.setIcon(bitmapDescriptorFromVector(getActivity(), R.drawable.fondo_blanco_trampa));
                    //m.setIcon(getIcon(R.mipmap.ic_trampas_fondo_blanco_round));
                }
                m.setTag(c.getId());
                marcadores.add(m);

                if (idColocacionCreada != null) {
                    if (idColocacionCreada.equals(String.valueOf(c.getId()))) {
                        marcador = m;
                    }
                }
            }

            if (marcador != null) {
                marcador.showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLng(marcador.getPosition()));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
                marcador = null;
            }

            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {
                    llGuardar.setVisibility(View.VISIBLE);

                    if (marcador != null && !(marker.getTitle().equals(marcador.getTitle()))) {
                        mostrarMensaje();

                    }

                }

                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    if (marcador == null)
                        marcador = marker;
                }
            });

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(final Marker marker) {
                    if (usuario.getAdmin() == 1) {
                        ObjectAnimator animation = ObjectAnimator.ofFloat(btnMasInformacion, "translationX", -257f);
                        animation.setDuration(650);
                        animation.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {
                                btnMasInformacion.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {

                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {

                            }
                        });
                        animation.start();


                        btnMasInformacion.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                for (Colocacion c : colocaciones)
                                    if (c.getId() == (int) marker.getTag()) {
                                        Intent intent = new Intent(mContext, DatosTrampa.class);
                                        intent.putExtra("trampa", c.getTrampa());
                                        intent.putExtra("colocacion", c);
                                        mContext.startActivity(intent);
                                    }
                            }
                        });
                    }
                    return false;
                }
            });

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    ObjectAnimator animation = ObjectAnimator.ofFloat(btnMasInformacion, "translationX", 0f);
                    animation.setDuration(400);
                    animation.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            btnMasInformacion.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                    animation.start();
                }
            });

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    for (Colocacion c : colocaciones)
                        if (c.getId() == (int) marker.getTag() && c.getFechaFin() != null) {
                            Intent intent = new Intent(mContext, ColocacionGrafica.class);
                            intent.putExtra("colocacion", c);
                            intent.putExtra("usuario", usuario);
                            mContext.startActivity(intent);
                        }
                }
            });

            btnGuardar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btnGuardar.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    if (marcador != null) {
                        int id = 0;
                        for (Colocacion c : colocaciones) {
                            if (c.getTrampa().getNombre().equals(marcador.getTitle())) {
                                id = c.getId();
                            }
                        }

                        if (id != 0) {
                            double lat = marcador.getPosition().latitude;
                            double lon = marcador.getPosition().longitude;

                            BDInterface bd = BDCliente.getClient().create(BDInterface.class);
                            Call<Respuesta> call = bd.actualizarUbicacionColocacion(id, lat, lon);
                            call.enqueue(new Callback<Respuesta>() {
                                @Override
                                public void onResponse(Call<Respuesta> call, Response<Respuesta> response) {
                                    btnGuardar.setVisibility(View.VISIBLE);
                                    progressBar.setVisibility(View.GONE);
                                    if (response.body() != null) {
                                        if (response.body().getCodigo().equals("1")) {
                                            llGuardar.setVisibility(View.GONE);
                                            marcador = null;
                                            Snackbar.make(((MenuPrincipal) mContext).getWindow().getDecorView().getRootView(), response.body().getMensaje(), Snackbar.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(mContext, response.body().getMensaje(), Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(mContext, R.string.error_interno_servidor, Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Respuesta> call, Throwable t) {
                                    btnGuardar.setVisibility(View.VISIBLE);
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(mContext, R.string.error_conexion_servidor, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }

                }
            });
        }
    }

    private void centrarMapa(LatLng coordenadas) {
        if (coordenadas == null)
            coordenadas = new LatLng(-32.315827, -58.065113); //Coordenadas por defecto

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordenadas, 12.0f));
    }


    public void obtenerLocalizacion() {
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, SOLICITUD_OBTENER_POSICION_ACTUAL);
        } else {
            mFusedLocationClient.getLastLocation().addOnSuccessListener((MenuPrincipal) mContext, new OnSuccessListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        centrarMapa(new LatLng(location.getLatitude(), location.getLongitude()));
                    } else {
                        centrarMapa(null);
                        mMap.setMyLocationEnabled(false);
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        // Toast.makeText(getContext(), "No se pudo obtener su ubicación, compruebe la configuración de localización del dispositivo.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case SOLICITUD_OBTENER_POSICION_ACTUAL: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    obtenerLocalizacion();
                } else {
                    centrarMapa(null);
                }
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public void mostrarMensaje() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Cambios no guardados");
        builder.setMessage("Si continua perderá los cambios del último marcador");

        builder.setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                marcador = null;
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    //Transformar fechas.
    public String convertFormat(String inputDate) {
        if (inputDate == null)
            return "";

        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = simpleDateFormat.parse(inputDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (date == null)
            return "";

        @SuppressLint("SimpleDateFormat") SimpleDateFormat convetDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        return convetDateFormat.format(date);
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context,int fondo) {
        Drawable background = ContextCompat.getDrawable(context, fondo);
        background.setBounds(0, 0, background.getIntrinsicWidth(), background.getIntrinsicHeight());
        Drawable vectorDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_trampas_sin_fondo);
        vectorDrawable.setBounds(-35, -30, vectorDrawable.getIntrinsicWidth() - 80, vectorDrawable.getIntrinsicHeight() - 90);
        Bitmap bitmap = Bitmap.createBitmap(background.getIntrinsicWidth(), background.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        background.draw(canvas);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private class LongOperationMap extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            if (mapFragment == null) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                mapFragment = SupportMapFragment.newInstance();
                ft.replace(R.id.map, mapFragment).commit();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mMap = googleMap;
                    cargarColocaciones();
                    obtenerLocalizacion();
                }
            });
        }
    }
}


