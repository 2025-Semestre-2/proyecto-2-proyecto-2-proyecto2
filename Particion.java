/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author Keingell
 */
public class Particion {
    public int base;
    public int limite;
    public boolean ocupada;
    public BCP proceso; 

    public Particion(int base, int limite) {
        this.base = base;
        this.limite = limite;
        this.ocupada = false;
        this.proceso = null;
    }

    @Override
    public String toString() {
        String estado = ocupada ? "OCUPADA" : "LIBRE";
        return "[Base=" + base + ", Limite=" + limite + ", " + estado + "]";
    }
}
