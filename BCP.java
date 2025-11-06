/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

import java.util.ArrayList;

/**
 *
 * @author jhonn
 */

public class BCP {
    public int idProceso = -1;
    public String nombre = "SIN_NOMBRE";
    public EstadoProceso estado = EstadoProceso.NUEVO;
    public ArrayList<Pagina> tablaPaginas = new ArrayList<>();
    
    //Info del BCP
    public int baseCodigo = -1;
    public int limiteCodigo = -1;
    public int baseDatos = -1;
    public int limiteDatos = -1;
    public int PC = 0;                
    public int prioridad = 1;         
    public int tiempoCPU = 0; 
    public String ultimoResultado = "-";
    public int dirEnMemoria = -1;
    public int tiempoArribo;

    // Campos para planificación SRT
    public int tiempoLlegada = 0;
    public int rafaga = 0;
    public int rafagaRestante = 0;
    public int tiempoFinalizacion = 0;
    public int tiempoRetorno = 0;
    public int tiempoEspera = 0;
    

    public void reiniciar() {
        idProceso = -1;
        nombre = "SIN_NOMBRE";
        estado = EstadoProceso.NUEVO;
        baseCodigo = limiteCodigo = baseDatos = limiteDatos = -1;
        PC = 0;
        prioridad = 1;
        tiempoCPU = 0;
        ultimoResultado = "-";
        dirEnMemoria = -1;
        tiempoLlegada = rafaga = rafagaRestante = 0;
        tiempoFinalizacion = tiempoRetorno = tiempoEspera = 0;
    }
    
    public void setDirEnMemoria(int dirEnMemoria) {
        this.dirEnMemoria = dirEnMemoria;
    }

    public void cambiarEstado(EstadoProceso nuevo) {
        this.estado = nuevo;
    }
    
    public void crearTablaPaginas(int tamProceso, int tamPagina) {
        int numPaginas = (int) Math.ceil((double) tamProceso / tamPagina);
        tablaPaginas.clear();
        for (int i = 0; i < numPaginas; i++) {
            tablaPaginas.add(new Pagina(i));
        }
    }
    
    public void avanzarPC() {
        this.PC++;
    }

    /**
     * Incrementa el tiempo de CPU consumido por el proceso.
     */
    public void incrementarTiempoCPU() {
        this.tiempoCPU++;
    }

    @Override
    public String toString() {
        return "BCP{" +
                "PID=" + idProceso +
                ", Nombre='" + nombre + '\'' +
                ", Estado=" + estado +
                ", PC=" + PC +
                ", Prioridad=" + prioridad +
                ", TiempoCPU=" + tiempoCPU +
                ", BaseC=" + baseCodigo +
                ", LimC=" + limiteCodigo +
                ", BaseD=" + baseDatos +
                ", LimD=" + limiteDatos +
                ", DirBCP=" + dirEnMemoria +
                '}';
    }
}
