/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author Keingell
 */
public class Pagina {
    public int numeroPagina;
    public int marcoAsignado = -1;
    public int marcoFisico = 0;
    public boolean cargada = false;

    public Pagina(int numeroPagina) {
        this.numeroPagina = numeroPagina;
    }

    @Override
    public String toString() {
        return "Pag " + numeroPagina + " -> Marco " + marcoAsignado;
    }
}