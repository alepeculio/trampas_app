package com.trampas.trampas.BD;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BDInterface {
    @FormUrlEncoded
    @POST("login")
    Call<RespuestaLogin> login(
            @Field("correo") String correo,
            @Field("contrasenia") String contrasenia
    );

    @FormUrlEncoded
    @POST("colocarTrampa")
    Call<Respuesta> colocarTrampa(
            @Field("lat") Double lat,
            @Field("lon") Double lon,
            @Field("id_trampa") int idTrampa,
            @Field("id_usuario") int idUsuario
    );

    @GET("obtenerTrampas")
    Call<RespuestaTrampas> obtenerTrampas();

    @GET("obtenerTrampasNoColocadas")
    Call<RespuestaTrampas> obtenerTrampasNoColocadas();

    @GET("obtenerTrampasColocadas")
    Call<RespuestaTrampas> obtenerTrampasColocadas();

    @GET("obtenerColocacionesActivas")
    Call<RespuestaColocaciones> obtenerColocacionesActivas();

    @FormUrlEncoded
    @POST("obtenerColocacionesTrampa")
    Call<RespuestaColocaciones> obtenerColocacionesTrampa(
            @Field("id") int id
    );

    @FormUrlEncoded
    @POST("agregarTrampa")
    Call<Respuesta> agregarTrampa(
            @Field("nombre") String nombre,
            @Field("mac") String mac
    );

    @FormUrlEncoded
    @POST("eliminarTrampa")
    Call<Respuesta> eliminarTrampa(
            @Field("id") int id
    );

    @FormUrlEncoded
    @POST("extraerTrampa")
    Call<Respuesta> extraerTrampa(
            @Field("id_trampa") int idTrampa,
            @Field("tmin") float tMin,
            @Field("tmax") float tMax,
            @Field("hmin") float hMin,
            @Field("hmax") float hMax,
            @Field("hprom") float hProm,
            @Field("tprom") float tProm
    );

    @FormUrlEncoded
    @POST("agregarUsuario")
    Call<Respuesta> agregarUsuario(
            @Field("nombre") String nombre,
            @Field("apellido") String apellido,
            @Field("correo") String correo,
            @Field("contrasenia") String contrasenia,
            @Field("admin") Boolean admin
    );
}
