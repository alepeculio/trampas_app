package com.trampas.trampas;


import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trampas.trampas.Adaptadores.AdaptadorListaUsuarios;
import com.trampas.trampas.BD.BDCliente;
import com.trampas.trampas.BD.BDInterface;
import com.trampas.trampas.BD.RespuestaUsuarios;
import com.trampas.trampas.Clases.Usuario;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AdministrarUsuarios extends Fragment {
    @BindView(R.id.rvUsuarios)
    RecyclerView mRecyclerView;
    @BindView(R.id.swipeRefresh)
    SwipeRefreshLayout swipeRefresh;
    @BindView(R.id.noHayUsuarios)
    LinearLayout noHayUsuarios;
    @BindView(R.id.tvNoHayUsuarios)
    TextView tvNoHayUsuarios;
    @BindView(R.id.cbSolicitudAdmin)
    CheckBox cbSolicitudAdmin;
    Context mContext;
    private List<Usuario> usuarios;
    private Usuario usuario;
    private AdaptadorListaUsuarios adaptadorListaUsuarios;
    private SearchView searchView = null;
    private SearchView.OnQueryTextListener queryTextListener;
    private String ultimaBusqueda = null;

    public AdministrarUsuarios() {
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
        filtrarUsuarios();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_administrar_usuarios, container, false);
        ButterKnife.bind(this, v);

        //Si el usuario logueado está nulo obtenerlo del menu principal.
        if (usuario == null && mContext != null)
            usuario = ((MenuPrincipal) mContext).getUsuario();

        setAdaptadorListaUsuarios();

        //Setear evento para recargar lista de usuarios al hacer el swipe.
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                cargarUsuarios();
            }
        });

        //Setear evento para filtrar usuarios que solicitaron ser admin.
        cbSolicitudAdmin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filtrarUsuarios();
            }
        });

        cargarUsuarios();
        return v;
    }

    //Crear y setear el adaptador a la lista de usuarios.
    public void setAdaptadorListaUsuarios() {
        if (adaptadorListaUsuarios == null) {
            adaptadorListaUsuarios = new AdaptadorListaUsuarios(new ArrayList<Usuario>(), mContext, this);
            mRecyclerView.setAdapter(adaptadorListaUsuarios);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            mRecyclerView.setHasFixedSize(true);
        }
    }

    //Crear la barra de busqueda.
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) mContext.getSystemService(((MenuPrincipal) mContext).SEARCH_SERVICE);

        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(((MenuPrincipal) mContext).getComponentName()));

            queryTextListener = new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    ultimaBusqueda = newText.toLowerCase().trim();
                    filtrarUsuarios();
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    ultimaBusqueda = query.toLowerCase().trim();
                    filtrarUsuarios();
                    return true;
                }
            };
            searchView.setOnQueryTextListener(queryTextListener);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    //cargar usuarios desde la bd
    private void cargarUsuarios() {
        swipeRefresh.setRefreshing(true);
        BDInterface bd = BDCliente.getClient().create(BDInterface.class);
        Call<RespuestaUsuarios> call = bd.obtenerUsuarios();
        call.enqueue(new Callback<RespuestaUsuarios>() {
            @Override
            public void onResponse(Call<RespuestaUsuarios> call, Response<RespuestaUsuarios> response) {
                if (response.body() != null) {
                    if (response.body().getCodigo().equals("1")) {
                        usuarios = response.body().getUsuarios();
                        removerLogueado();
                        filtrarUsuarios();
                    } else {
                        usuarios = null;
                        filtrarUsuarios();
                        Toast.makeText(mContext, response.body().getMensaje(), Toast.LENGTH_SHORT).show();

                    }
                } else {
                    usuarios = null;
                    filtrarUsuarios();
                    Toast.makeText(mContext, R.string.error_interno_servidor, Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call<RespuestaUsuarios> call, Throwable t) {
                usuarios = null;
                filtrarUsuarios();
                Toast.makeText(mContext, R.string.error_conexion_servidor, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Quitar usuario logueado de la lista.
    private void removerLogueado() {
        for (int i = 0; i < usuarios.size(); i++)
            if (usuarios.get(i).getId() == usuario.getId())
                usuarios.remove(i);
    }

    //Filtrar usuarios por busqueda o los que solicitaron ser admin.
    private void filtrarUsuarios() {
        List<Usuario> usuariosFinal = new ArrayList<>();

        if (usuarios != null) {
            Boolean busquedaVacia = false;
            if (ultimaBusqueda != null) {
                usuariosFinal.clear();
                for (Usuario u : usuarios) {
                    if (u.getNombre().toLowerCase().contains(ultimaBusqueda) || String.valueOf(u.getId()).toLowerCase().contains(ultimaBusqueda) || u.getCorreo().toLowerCase().contains(ultimaBusqueda)) {
                        if (cbSolicitudAdmin.isChecked()) {
                            if (u.getAdmin() == 2)
                                usuariosFinal.add(u);
                        } else
                            usuariosFinal.add(u);
                    }
                }
                if (usuariosFinal.size() == 0 && !ultimaBusqueda.equals("")) {
                    tvNoHayUsuarios.setText("No hay resultados para \"" + ultimaBusqueda + "\"");
                    busquedaVacia = true;
                }

            } else {
                if (cbSolicitudAdmin.isChecked()) {
                    for (Usuario u1 : usuarios)
                        if (u1.getAdmin() == 2)
                            usuariosFinal.add(u1);
                } else
                    usuariosFinal = usuarios;
            }

            if (usuariosFinal.size() == 0 && !busquedaVacia) {
                if (cbSolicitudAdmin.isChecked())
                    tvNoHayUsuarios.setText(R.string.no_hay_usuarios_solicitantes);
                else
                    tvNoHayUsuarios.setText(R.string.no_hay_usuarios);
            }
        } else {
            tvNoHayUsuarios.setText(R.string.no_hay_usuarios);
        }

        if (usuariosFinal.size() == 0) {
            noHayUsuarios.setVisibility(View.VISIBLE);
        } else {
            noHayUsuarios.setVisibility(View.GONE);
        }
        adaptadorListaUsuarios.actualizarUsuarios(usuariosFinal);
        swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }
}
