/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author jhonn
 */

public class BCP {
    public int idProceso = -1;
    public EstadoProceso estado = EstadoProceso.NUEVO;
    public int baseCodigo = -1;
    public int limiteCodigo = -1;
    public int baseDatos = -1;
    public int limiteDatos = -1;
    public String ultimoResultado = "-";

    // Campos para planificación SRT
    public int tiempoLlegada = 0;
    public int rafaga = 0;
    public int rafagaRestante = 0;
    public int tiempoFinalizacion = 0;
    public int tiempoRetorno = 0;
    public int tiempoEspera = 0;

    public void reiniciar() {
        idProceso = -1;
        estado = EstadoProceso.NUEVO;
        baseCodigo = limiteCodigo = baseDatos = -1;
        ultimoResultado = "-";
        tiempoLlegada = rafaga = rafagaRestante = 0;
        tiempoFinalizacion = tiempoRetorno = tiempoEspera = 0;
    }

    public void cambiarEstado(EstadoProceso nuevo) {
        this.estado = nuevo;
    }
}
