/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author jhonn
 */

import java.util.*;

public class Memoria {
    public int tamano;
    public int tamanoSO;
    private final List<String> celdas;
    private String mensajeEstado = "";
    public int proximaDireccionBCP = 0;
    private ArrayList<Particion> particiones = new ArrayList<>();
    private ArrayList<Integer> marcosLibres = new ArrayList<>();
    private String[] memoria;
    private int tamanoParticion = 10; // Ajustable
    public int tamanoMarco = 5; //Ajustable

    // 🆕 Tamaño de página (para memoria virtual)
    public int tamanoPagina = 8;

    public Memoria(int tamano, int tamanoSO) {
        this.tamano = tamano;
        this.tamanoSO = tamanoSO;
        this.celdas = new ArrayList<>();
        for (int i = 0; i < tamano; i++) celdas.add("");
        memoria = new String[tamano];
        inicializarMarcos();
        marcarSO();
    }

    public void redimensionar(int nuevoTamano, int nuevoSO) {
        if (nuevoSO >= nuevoTamano) {
            throw new RuntimeException("El tamaño del SO no puede ser mayor o igual al de la Memoria.");
        }
        this.tamano = nuevoTamano;
        this.tamanoSO = nuevoSO;
        celdas.clear();
        for (int i = 0; i < tamano; i++) celdas.add("");
        marcarSO();
    }

    public void limpiarUsuario() {
        for (int i = tamanoSO; i < tamano; i++) celdas.set(i, "");
    }

    public void marcarSO() {
        for (int i = 0; i < tamanoSO && i < tamano; i++) celdas.set(i, "<SO>");
    }

    public String obtenerCelda(int idx) {
        if (idx < 0 || idx >= tamano) return "0";
        String v = celdas.get(idx);
        if (v == null || v.trim().isEmpty()) return "0";
        if ("<SO>".equals(v)) return "0";
        return v;
    }
    
    public int guardarBCP(BCP bcp) {
        int tamanoBCP = 10; // cada BCP usa 10 celdas
        int finSO = tamanoSO; // límite del área del SO
        int siguiente = proximaDireccionBCP + tamanoBCP;

        // Validar que haya espacio
        if (siguiente > finSO) {
            System.out.println("❌ ERROR: No hay suficiente espacio en la memoria del SO para otro BCP.");
            return -1; // error
        }

        int inicio = proximaDireccionBCP;

        asignarCelda(inicio,     "PID=" + bcp.idProceso);
        asignarCelda(inicio + 1, "NOMBRE=" + bcp.nombre);
        asignarCelda(inicio + 2, "ESTADO=" + bcp.estado);
        asignarCelda(inicio + 3, "PC=" + bcp.PC);
        asignarCelda(inicio + 4, "BASE_COD=" + bcp.baseCodigo);
        asignarCelda(inicio + 5, "LIM_COD=" + bcp.limiteCodigo);
        asignarCelda(inicio + 6, "BASE_DAT=" + bcp.baseDatos);
        asignarCelda(inicio + 7, "LIM_DAT=" + bcp.limiteDatos);
        asignarCelda(inicio + 8, "PRIORIDAD=" + bcp.prioridad);
        asignarCelda(inicio + 9, "TIEMPO_CPU=" + bcp.tiempoCPU);

        bcp.dirEnMemoria = inicio;
        proximaDireccionBCP = siguiente;

        return inicio;
    }
    
    public void actualizarBCP(BCP bcp) {
        if (bcp.dirEnMemoria < 0) return; // no tiene dirección válida
        int base = bcp.dirEnMemoria;

        asignarCelda(base + 2, "ESTADO=" + bcp.estado);
        asignarCelda(base + 3, "PC=" + bcp.PC);
        asignarCelda(base + 4, "BASE_COD=" + bcp.baseCodigo);
        asignarCelda(base + 5, "LIM_COD=" + bcp.limiteCodigo);
        asignarCelda(base + 6, "BASE_DAT=" + bcp.baseDatos);
        asignarCelda(base + 7, "LIM_DAT=" + bcp.limiteDatos);
        asignarCelda(base + 8, "PRIORIDAD=" + bcp.prioridad);
        asignarCelda(base + 9, "TIEMPO_CPU=" + bcp.tiempoCPU);
    }

    public void asignarCelda(int idx, String valor) {
        if (idx < 0 || idx >= tamano) throw new RuntimeException("Dirección fuera de rango: " + idx);
        celdas.set(idx, valor);
    }

    public String obtenerRaw(int idx) { return celdas.get(idx); }

    public void asignarMensaje(String msg) { this.mensajeEstado = msg; }
    public String obtenerMensaje() { return mensajeEstado; }

    // Utilidad para memoria virtual dinámica: total de páginas disponibles
    public int getTotalPaginas() {
        return (int) Math.ceil((tamano - tamanoSO) / (double) tamanoPagina);
    }

    // Devuelve las direcciones base de cada página (útil para crear tabla de páginas)
    public List<Integer> getBasesDePaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = tamanoSO; i < tamano; i += tamanoPagina) {
            paginas.add(i);
        }
        return paginas;
    }
    
    //==================================== Particion fija ========================
    
    public void inicializarParticiones() {
        particiones.clear();
        for (int i = tamanoSO; i < tamano; i += tamanoParticion) {
            int limite = Math.min(i + tamanoParticion - 1, tamano - 1);
            particiones.add(new Particion(i, limite));
            
        }
    }

    public Particion asignarParticion(BCP bcp, int tamanoProceso) {
        for (Particion p : particiones) {
            if (!p.ocupada && (p.limite - p.base + 1) >= tamanoProceso) {
                p.ocupada = true;
                p.proceso = bcp;
                bcp.baseCodigo = p.base;
                bcp.limiteCodigo = p.limite;
                System.out.println(p.base + " ============================= "+ p.limite);
                return p;
            }
        }
        System.out.println("❌ Memoria llena, no se puede cargar el proceso " + bcp.idProceso);
        return null; // No hay espacio disponible
    }

    // 🔹 Liberar una partición
    public void liberarParticion(BCP bcp) {
        for (Particion p : particiones) {
            if (p.proceso == bcp) {
                p.ocupada = false;
                p.proceso = null;
                bcp.baseCodigo = bcp.limiteCodigo = -1;
                break;
            }
        }
    }

    // 🔹 Mostrar estado de particiones (para depuración)
    public void mostrarParticiones() {
        System.out.println("=== Estado de Particiones ===");
        for (Particion p : particiones) {
            System.out.println(p);
        }
        System.out.println("=============================");
    }

    // Getter opcional si quieres mostrarlas en interfaz
    public ArrayList<Particion> getParticiones() {
        return particiones;
    }
    
    //============================= PAGINACION  ===================================================
    
     public void inicializarMarcos() {
        int baseUsuario = tamanoSO;
        for (int i = baseUsuario; i < tamano; i += tamanoMarco) {
            marcosLibres.add(i); // agregamos la dirección base de cada marco
        }
    }

    public boolean asignarPaginas(BCP bcp, int tamProceso) {
        int paginasNecesarias = (int) Math.ceil((double) tamProceso / tamanoMarco);
        if (paginasNecesarias > marcosLibres.size()) {
            System.out.println("❌ No hay marcos suficientes para el proceso " + bcp.idProceso);
            return false;
        }

        bcp.crearTablaPaginas(tamProceso, tamanoMarco);

        for (int i = 0; i < bcp.tablaPaginas.size(); i++) {
            int baseMarco = marcosLibres.remove(0); // sacamos un marco libre
            bcp.tablaPaginas.get(i).marcoAsignado = baseMarco;
            bcp.tablaPaginas.get(i).cargada = true;
            
        }
        
        return true;
    }

    public void liberarPaginas(BCP bcp) {
        for (Pagina p : bcp.tablaPaginas) {
            if (p.cargada) {
                marcosLibres.add(p.marcoAsignado);
                p.cargada = false;
            }
        }
    }
    
    public int traducirDireccion(BCP bcp, int direccionLogica) {
        int tamanoPagina = this.tamanoPagina; // depende de tu implementación

        int nroPagina = direccionLogica / tamanoPagina;
        int desplazamiento = direccionLogica % tamanoPagina;

        if (nroPagina >= bcp.tablaPaginas.size()) {
            System.out.println("❌ Dirección lógica fuera del rango del proceso");
            return -1;
        }

        Pagina pag = bcp.tablaPaginas.get(nroPagina);
        if (!pag.cargada) {
            System.out.println("⚠️ Page Fault: página no está en memoria");
            return -1;
        }

        // Calcula dirección física
        int direccionFisica = (pag.marcoAsignado * tamanoPagina) + desplazamiento;
        return direccionFisica;
    }

}
