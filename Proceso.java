/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author Keingell
 */
import java.io.File;
import java.util.HashMap;
import java.util.Map;

class Proceso {
    int pid;
    Programa programa;
    BCP bcp;
    File archivo;
    Proceso siguiente; // apunta al siguiente proceso en la lista

    // Tabla de páginas para Memoria Virtual con Dinámica
    public Map<Integer, Integer> tablaPaginas = new HashMap<>();

    public Proceso(int pid, Programa programa, BCP bcp, File archivo) {
        this.pid = pid;
        this.programa = programa;
        this.bcp = bcp;
        this.archivo = archivo;
        this.siguiente = null;
    }
}
