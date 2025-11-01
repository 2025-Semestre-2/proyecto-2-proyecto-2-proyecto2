/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author jhonn
 */

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Comparator;

public class VentanaSimulador extends JFrame {

    private final Memoria memoria = new Memoria(512, 64);
    private final CPU cpu = new CPU();
    public static BCP bcp = new BCP();
    
    private File ultimoArchivoCargado = null;
    
    private int proximaDireccionLibre = memoria.tamanoSO;  
    private int contProgramas = 0;
    private final Queue<Proceso> colaEspera = new LinkedList<>();
    private final List<File> archivosSeleccionados = new ArrayList<>();
    private int tamanoPagina = 8;  // Cada página tiene 8 celdas físicas
    private final List<Integer> marcosLibres = new ArrayList<>(); // Lista de marcos disponibles
    
    private Proceso cabeza = null;        // primer proceso cargado
    private Proceso cola = null;          // último proceso (para ir encadenando)
    private Proceso procesoActual = null; 
    private int contadorProcesos = 1;     // ID's de los procesos

    private final ModeloTablaMemoria modeloMemoria = new ModeloTablaMemoria(memoria, () -> obtenerPCAbsoluto());
    private final JTable tablaMemoria = new JTable(modeloMemoria);

    private final DefaultTableModel modeloInstrucciones = new DefaultTableModel(new Object[]{"Instrucción", "Binario"}, 0);
    private final JTable tablaInstrucciones = new JTable(modeloInstrucciones);
    
    //private final List<int[]> huecosLibres = new ArrayList<>();

    private final JLabel lblEstado = new JLabel("Sin programa");
    private final JLabel lblPC = new JLabel("0");
    private final JLabel lblAC = new JLabel("0");
    private final JLabel lblAX = new JLabel("0");
    private final JLabel lblBX = new JLabel("0");
    private final JLabel lblCX = new JLabel("0");
    private final JLabel lblDX = new JLabel("0");
    private final JLabel lblZF = new JLabel("false");
    private JLabel lblUltimoResultado = new JLabel("-");

    private final JLabel lblIdProceso = new JLabel("-");
    private final JLabel lblEstadoBCP = new JLabel("-");
    private final JLabel lblBaseCodigo = new JLabel("-");
    private final JLabel lblLimiteCodigo = new JLabel("-");
    private final JLabel lblBaseDatos = new JLabel("-");
    private final JLabel lblIR = new JLabel("-");

    private final JSpinner spTamMemoria = new JSpinner(new SpinnerNumberModel(512, 16, 4096, 1));
    private final JSpinner spTamSO = new JSpinner(new SpinnerNumberModel(64, 1, 2048, 1));

    private final JButton btnAsignarMemoria = new JButton("Asignar Memoria");
    private final JButton btnCargar = new JButton("Cargar .asm");
    JButton btnLlegadas = new JButton("Asignar tiempos");
    private final JButton btnRecargar = new JButton("Recargar");
    private final JButton btnPaso = new JButton("Paso a paso");
    private final JButton btnEjecutar = new JButton("Ejecutar");
    private final JButton btnDetener = new JButton("Detener");
    private final JButton btnLimpiar = new JButton("Limpiar");
    private final JButton btnEstadisticas = new JButton("Estadísticas");
    JButton btnEstados = new JButton("Ver estados");
    
    // Selector de modo de memoria
    private final JComboBox<String> cbTipoMemoria =
        new JComboBox<>(new String[]{
                "Seleccione el Tipo de Memoria",
                "Memoria Física con Segmentación",
                "Memoria Física con Dinámica",
                "Memoria Virtual con Dinámica"
        });
    
    private String modoMemoria = "NINGUNO";  // valor por defecto
    private List<int[]> huecosLibres = new ArrayList<>();
    //private final java.util.List<Integer> marcosLibres = new ArrayList<>();
    private final java.util.Map<Integer, Integer> tablaPaginas = new HashMap<>(); // numPagina a marco
    private int tamañoPagina = 16; // Tamaño de página (en celdas)
    private final java.util.Map<Integer, String> swapSpace = new HashMap<>(); // Simulación de swap
    //private String modoMemoria = "Seleccionar Modo de Memoria"; // Valor por defecto
    
    
    private Temporizador temporizador;

    private Instruccion instruccionActual = null;
    private int ciclosPendientes = 0;
    
    private final java.util.List<Estadistica> estadisticas = new ArrayList<>();
    private Estadistica estadisticaActual = null;
    
    private final JComboBox<String> cbPlanificacion = new JComboBox<>(new String[] {"FCFS", "SRT"});
    //private final JLabel lblModoPlanificacion = new JLabel("Planificación: FCFS");
    
    private String modoPlanificacion = "FCFS";
    private int tiempoGlobal = 0;
    private final List<Proceso> listaProcesos = new ArrayList<>();
    private final PriorityQueue<Proceso> colaListosSRT =
            new PriorityQueue<>(Comparator.comparingInt(p -> p.bcp.rafagaRestante));

    private static final Map<String,Integer> DURACIONES = new HashMap<>();
    static {
        DURACIONES.put("LOAD", 1);
        DURACIONES.put("STORE", 1);
        DURACIONES.put("MOV", 1);
        DURACIONES.put("ADD", 1);
        DURACIONES.put("SUB", 1);
        DURACIONES.put("INC", 1);
        DURACIONES.put("DEC", 1);
        DURACIONES.put("SWAP", 1);
        DURACIONES.put("INT", 1);
        DURACIONES.put("JMP", 1);
        DURACIONES.put("CMP", 1);
        DURACIONES.put("JE", 1);
        DURACIONES.put("JNE", 1);
        DURACIONES.put("PARAM", 1);
        DURACIONES.put("PUSH", 1);
        DURACIONES.put("POP", 1);
    }
    
    // Clase interna para representar un segmento en memoria
    private static class Segmento {
        String tipo;
        int base;
        int limite;

        Segmento(String tipo, int base, int limite) {
            this.tipo = tipo;
            this.base = base;
            this.limite = limite;
        }
    }

    public VentanaSimulador() {
        super("MiniPC - Tarea 1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 650));
        setLocationRelativeTo(null);
        
        // Inicializar lista de espacios libres para memoria dinámica
        huecosLibres.clear();
        huecosLibres.add(new int[]{memoria.tamanoSO, memoria.tamano - 1});


        tablaMemoria.setFillsViewportHeight(true);
        tablaMemoria.setDefaultRenderer(Object.class, new RenderizadorMemoria(memoria, () -> obtenerPCAbsoluto()));

        JPanel barraSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        //barraSuperior.add(new JLabel("Tipo de Memoria:"));
        barraSuperior.add(cbTipoMemoria);
        //barraSuperior.add(new JLabel("Planificación:"));
        barraSuperior.add(cbPlanificacion);
        //barraSuperior.add(lblModoPlanificacion);
        barraSuperior.add(btnLlegadas);
        btnLlegadas.addActionListener(e -> asignarTiemposDeLlegada());
        barraSuperior.add(new JLabel("Memoria:"));
        barraSuperior.add(spTamMemoria);
        barraSuperior.add(new JLabel("SO:"));
        barraSuperior.add(spTamSO);
        barraSuperior.add(btnAsignarMemoria);
        barraSuperior.add(btnCargar);
        barraSuperior.add(btnRecargar);
        barraSuperior.add(btnPaso);
        barraSuperior.add(btnEjecutar);
        barraSuperior.add(btnDetener);
        barraSuperior.add(btnLimpiar);
        barraSuperior.add(btnEstadisticas);
        barraSuperior.add(btnEstados);
        
        /*barraSuperior.add(new JLabel("Modo:"));
        barraSuperior.add(cmbModoMemoria);

        cmbModoMemoria.addActionListener(e -> {
            modoMemoria = (String) cmbModoMemoria.getSelectedItem();
            lblEstado.setText("Modo seleccionado: " + modoMemoria);
            onCambioModoMemoria(modoMemoria);
        });*/
        
        cbTipoMemoria.addActionListener(e -> {
            String seleccion = (String) cbTipoMemoria.getSelectedItem();
            if (seleccion == null) return;

            switch (seleccion) {
                case "Memoria Física con Segmentación" -> {
                    modoMemoria = "SEGMENTACION";
                    lblEstado.setText("Modo de memoria cambiado a: Segmentación");
                }

                case "Memoria Física con Dinámica" -> {
                    modoMemoria = "DINAMICA";
                    huecosLibres.clear();
                    huecosLibres.add(new int[]{memoria.tamanoSO, memoria.tamano - 1});
                    lblEstado.setText("Modo de memoria cambiado a: Dinámica");
                }

                case "Memoria Virtual con Dinámica" -> {
                    modoMemoria = "VIRTUAL";
                    marcosLibres.clear();
                    tablaPaginas.clear();
                    swapSpace.clear();

                    // Inicializamos marcos libres según el tamaño de página
                    for (int i = memoria.tamanoSO; i < memoria.tamano; i += tamañoPagina) {
                        marcosLibres.add(i);
                    }

                    lblEstado.setText("Modo de memoria cambiado a: Virtual con Dinámica (páginas de "
                            + tamañoPagina + " celdas, marcos libres: " + marcosLibres.size() + ")");
                    JOptionPane.showMessageDialog(this,
                            "Modo activado: Memoria Virtual con Dinámica\n" +
                                    "Tamaño de página: " + tamañoPagina + " celdas\n" +
                                    "Marcos libres iniciales: " + marcosLibres.size(),
                            "Memoria Virtual", JOptionPane.INFORMATION_MESSAGE);
                }

                default -> {
                    modoMemoria = "NINGUNO";
                    lblEstado.setText("No se ha seleccionado ningún modo de memoria.");
                }
            }
        });
        
        cbPlanificacion.addActionListener(e -> {
            String seleccion = (String) cbPlanificacion.getSelectedItem();
            if (seleccion.contains("SRT")) {
                modoPlanificacion = "SRT";
            } else {
                modoPlanificacion = "FCFS";
            }
            lblEstado.setText("Planificador seleccionado: " + modoPlanificacion);

        });
        
        JPanel panelCPU = construirPanelCPU();
        JPanel panelBCP = construirPanelBCP();

        JPanel panelDerecho = new JPanel(new GridLayout(2, 1));
        panelDerecho.add(panelCPU);
        panelDerecho.add(panelBCP);

        JScrollPane scrollInstr = new JScrollPane(tablaInstrucciones);
        scrollInstr.setBorder(new TitledBorder("Instrucciones"));

        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollInstr, new JScrollPane(tablaMemoria));
        centro.setResizeWeight(0.4);

        JSplitPane divisionPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centro, panelDerecho);
        divisionPrincipal.setResizeWeight(0.7);

        setLayout(new BorderLayout());
        add(barraSuperior, BorderLayout.NORTH);
        add(divisionPrincipal, BorderLayout.CENTER);
        add(lblEstado, BorderLayout.SOUTH);

        btnAsignarMemoria.addActionListener(e -> {
            int nuevoTam = (int) spTamMemoria.getValue();
            int nuevoSO = (int) spTamSO.getValue();

            if (nuevoSO >= nuevoTam) {
                JOptionPane.showMessageDialog(this,
                        "El tamaño del SO no puede ser mayor o igual al de la Memoria.",
                        "Error de configuración",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            memoria.redimensionar(nuevoTam, nuevoSO);
            modeloMemoria.fireTableDataChanged();
            lblEstado.setText("Memoria asignada: " + nuevoTam + " celdas (SO=" + nuevoSO + ")");

            // Inicializar los marcos libres para Memoria Virtual con Dinámica
            marcosLibres.clear();
            for (int i = memoria.tamanoSO; i < memoria.tamano; i += tamanoPagina) {
                marcosLibres.add(i);
            }

            // Inicializar los espacios libres para Memoria Física Dinámica
            huecosLibres.clear();
            huecosLibres.add(new int[]{memoria.tamanoSO, memoria.tamano - 1});
        });


        btnCargar.addActionListener(e -> cargarDesdeChooser());
        btnRecargar.addActionListener(e -> recargarUltimoArchivo());
        btnPaso.addActionListener(e -> {
            if (modoMemoria.equals("NINGUNO")) {
                JOptionPane.showMessageDialog(this,
                        "Debe seleccionar un tipo de memoria antes de ejecutar paso a paso.",
                        "Modo de memoria no seleccionado",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            ejecutarPaso();
        });

        btnEjecutar.addActionListener(e -> {
            if (modoMemoria.equals("NINGUNO")) {
                JOptionPane.showMessageDialog(this,
                        "Debe seleccionar un tipo de memoria antes de ejecutar el programa.",
                        "Modo de memoria no seleccionado",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            temporizador.iniciar();
        });

        btnDetener.addActionListener(e -> temporizador.detener());
        btnLimpiar.addActionListener(e -> limpiarTodo());
        btnEstados.addActionListener(e -> mostrarEstadosBCP());
        btnEstadisticas.addActionListener(e -> mostrarEstadisticas());
        
        temporizador = new Temporizador(1000, this::ejecutarPaso);
    }
    
    private JPanel construirPanelCPU() {
        JPanel p = new JPanel(new GridLayout(0, 2));
        p.setBorder(new TitledBorder("CPU"));

        p.add(new JLabel("PC:")); p.add(lblPC);
        p.add(new JLabel("AC:")); p.add(lblAC);
        p.add(new JLabel("AX:")); p.add(lblAX);
        p.add(new JLabel("BX:")); p.add(lblBX);
        p.add(new JLabel("CX:")); p.add(lblCX);
        p.add(new JLabel("DX:")); p.add(lblDX);
        p.add(new JLabel("ZF:")); p.add(lblZF);

        return p;
    }

    private JPanel construirPanelBCP() {
        JPanel p = new JPanel(new GridLayout(0, 2));
        p.setBorder(new TitledBorder("BCP actual CPU1"));

        p.add(new JLabel("ID Proceso:")); p.add(lblIdProceso);
        p.add(new JLabel("Estado:")); p.add(lblEstadoBCP);
        p.add(new JLabel("Base Código:")); p.add(lblBaseCodigo);
        p.add(new JLabel("Límite Código:")); p.add(lblLimiteCodigo);
        p.add(new JLabel("Base Datos:")); p.add(lblBaseDatos);
        p.add(new JLabel("IR:")); p.add(lblIR);
         p.add(new JLabel("Último Resultado:")); 
        lblUltimoResultado = new JLabel("-");
        p.add(lblUltimoResultado);
        return p;
    }

    private void limpiarTodo() {
        cpu.reiniciar();

        if (bcp != null) {
            bcp.cambiarEstado(EstadoProceso.TERMINADO);
        }

        memoria.limpiarUsuario();
        contProgramas = 0;
        proximaDireccionLibre = memoria.tamanoSO;

        modeloInstrucciones.setRowCount(0);
        modeloMemoria.fireTableDataChanged();
        actualizarVistas();
        lblEstado.setText("CPU lista para el siguiente proceso.");

        // Reinicializar los marcos libres para Memoria Virtual con Dinámica
        marcosLibres.clear();
        for (int i = memoria.tamanoSO; i < memoria.tamano; i += tamanoPagina) {
            marcosLibres.add(i);
        }

        // Reinicializar también los espacios libres para Memoria Física Dinámica
        huecosLibres.clear();
        huecosLibres.add(new int[]{memoria.tamanoSO, memoria.tamano - 1});
    }


    private void recargarUltimoArchivo() {
        if (archivosSeleccionados.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay archivos cargados para recargar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        limpiarTodo();
        int cargados = 0, enEspera = 0;

        for (File archivo : archivosSeleccionados) {
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
                Programa cargado = Cargador.parsear(lineas);

                BCP nuevoBCP = new BCP();
                nuevoBCP.idProceso = contadorProcesos++;
                nuevoBCP.cambiarEstado(EstadoProceso.NUEVO);

                Proceso nuevo = new Proceso(nuevoBCP.idProceso, cargado, nuevoBCP, archivo);

                if (contProgramas < 5) {
                    cargarEnMemoria(nuevo);
                    cargados++;
                } else {
                    nuevo.bcp.cambiarEstado(EstadoProceso.ESPERA);
                    colaEspera.add(nuevo);
                    enEspera++;
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error al recargar " + archivo.getName() + ":\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        JOptionPane.showMessageDialog(this,
                "Recarga completada.\nArchivos cargados: " + cargados + "\nArchivos en espera: " + enEspera,
                "Recarga exitosa", JOptionPane.INFORMATION_MESSAGE);

        modeloMemoria.fireTableDataChanged();
        lblEstado.setText("Archivos recargados correctamente.");
    }

    
    private void cargarEnMemoria(Proceso p) {
        Programa cargado = p.programa;

        switch (modoMemoria.toUpperCase()) {
            // ======================================================
            // Memoria física con segmentación
            // ======================================================
            case "SEGMENTACION" -> {
                int baseCodigo = proximaDireccionLibre;
                int limiteCodigo = baseCodigo + cargado.longitud() - 1;

                int tamañoSegmentoDatos = 8; // tamaño fijo para datos
                int baseDatos = limiteCodigo + 1;
                int limiteDatos = baseDatos + tamañoSegmentoDatos - 1;

                if (limiteDatos >= memoria.tamano) {
                    JOptionPane.showMessageDialog(this,
                            "No hay espacio suficiente en memoria para cargar el proceso.",
                            "Error de memoria", JOptionPane.ERROR_MESSAGE);
                    p.bcp.cambiarEstado(EstadoProceso.ESPERA);
                    colaEspera.add(p);
                    return;
                }

                // Cargar instrucciones
                for (int i = 0; i < cargado.longitud(); i++) {
                    memoria.asignarCelda(baseCodigo + i, cargado.lineaOriginal(i));
                }

                // Configurar BCP
                p.bcp.baseCodigo = baseCodigo;
                p.bcp.limiteCodigo = limiteCodigo;
                p.bcp.baseDatos = baseDatos;
                p.bcp.limiteDatos = limiteDatos;
                p.bcp.cambiarEstado(EstadoProceso.NUEVO);

                // Ráfaga = cantidad de instrucciones
                p.bcp.rafaga = cargado.longitud();
                p.bcp.rafagaRestante = p.bcp.rafaga;

                // Tiempo de llegada inicial
                p.bcp.tiempoLlegada = listaProcesos.isEmpty() ? 0 : tiempoGlobal + listaProcesos.size();

                // Registrar en lista global
                if (!listaProcesos.contains(p)) listaProcesos.add(p);

                // Enlazar a la lista de procesos
                if (cabeza == null) cabeza = p;
                else cola.siguiente = p;
                cola = p;

                proximaDireccionLibre = limiteDatos + 1;
                contProgramas++;
            }

            // ======================================================
            // Memoria física con dinámica (first-fit)
            // ======================================================
            case "DINAMICA" -> {
                int tamanoProceso = cargado.longitud() + 8; // código + datos
                int baseAsignada = -1;

                // Buscar espacio libre
                for (int i = 0; i < huecosLibres.size(); i++) {
                    int[] hueco = huecosLibres.get(i);
                    int inicio = hueco[0];
                    int fin = hueco[1];
                    int tamanoHueco = fin - inicio + 1;

                    if (tamanoHueco >= tamanoProceso) {
                        baseAsignada = inicio;
                        int nuevoInicio = inicio + tamanoProceso;
                        huecosLibres.remove(i);
                        if (nuevoInicio <= fin) huecosLibres.add(i, new int[]{nuevoInicio, fin});
                        break;
                    }
                }

                if (baseAsignada == -1) {
                    JOptionPane.showMessageDialog(this,
                            "No hay espacio disponible para este proceso (modo dinámico).",
                            "Memoria llena", JOptionPane.WARNING_MESSAGE);
                    p.bcp.cambiarEstado(EstadoProceso.ESPERA);
                    colaEspera.add(p);
                    return;
                }

                int baseCodigo = baseAsignada;
                int limiteCodigo = baseCodigo + cargado.longitud() - 1;
                int baseDatos = limiteCodigo + 1;
                int limiteDatos = baseDatos + 7;

                // Cargar instrucciones
                for (int i = 0; i < cargado.longitud(); i++) {
                    memoria.asignarCelda(baseCodigo + i, cargado.lineaOriginal(i));
                }

                // Configurar BCP
                p.bcp.baseCodigo = baseCodigo;
                p.bcp.limiteCodigo = limiteCodigo;
                p.bcp.baseDatos = baseDatos;
                p.bcp.limiteDatos = limiteDatos;
                p.bcp.cambiarEstado(EstadoProceso.NUEVO);

                // Ráfaga y tiempos
                p.bcp.rafaga = cargado.longitud();
                p.bcp.rafagaRestante = p.bcp.rafaga;
                p.bcp.tiempoLlegada = listaProcesos.isEmpty() ? 0 : tiempoGlobal + listaProcesos.size();

                if (!listaProcesos.contains(p)) listaProcesos.add(p);

                if (cabeza == null) cabeza = p;
                else cola.siguiente = p;
                cola = p;
                contProgramas++;
            }

            // ======================================================
            // Paginación
            // ======================================================
            case "VIRTUAL" -> {
                int numPaginas = (int) Math.ceil((double) cargado.longitud() / tamanoPagina);

                // Comprobamos si hay marcos suficientes
                if (numPaginas > marcosLibres.size()) {
                    JOptionPane.showMessageDialog(this,
                            "No hay marcos libres suficientes para asignar al proceso.",
                            "Memoria Virtual", JOptionPane.WARNING_MESSAGE);
                    p.bcp.cambiarEstado(EstadoProceso.ESPERA);
                    colaEspera.add(p);
                    return;
                }

                // Asignamos páginas a marcos físicos
                for (int i = 0; i < numPaginas; i++) {
                    int baseMarco = marcosLibres.remove(0);
                    p.tablaPaginas.put(i, baseMarco);

                    // Cargamos las instrucciones de esa página en el marco
                    for (int j = 0; j < tamanoPagina; j++) {
                        int index = i * tamanoPagina + j;
                        if (index < cargado.longitud()) {
                            memoria.asignarCelda(baseMarco + j, cargado.lineaOriginal(index));
                        } else {
                            memoria.asignarCelda(baseMarco + j, ""); // relleno vacío
                        }
                    }
                }

                p.bcp.baseCodigo = -1;
                p.bcp.limiteCodigo = -1;
                p.bcp.baseDatos = -1;
                p.bcp.limiteDatos = -1;
                p.bcp.cambiarEstado(EstadoProceso.NUEVO);

                // Ráfaga y llegada
                p.bcp.rafaga = cargado.longitud();
                p.bcp.rafagaRestante = p.bcp.rafaga;
                p.bcp.tiempoLlegada = listaProcesos.isEmpty() ? 0 : tiempoGlobal + listaProcesos.size();

                if (!listaProcesos.contains(p)) listaProcesos.add(p);

                if (cabeza == null) cabeza = p;
                else cola.siguiente = p;
                cola = p;

                contProgramas++;
            }

            default -> JOptionPane.showMessageDialog(this,
                    "Modo de memoria no reconocido: " + modoMemoria);
        }

        modeloMemoria.fireTableDataChanged();
    }


    private void cargarDesdeChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Selecciona carpeta con archivos ASM");

        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File carpeta = chooser.getSelectedFile();
        if (carpeta == null || !carpeta.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una carpeta válida.");
            return;
        }

        File[] archivos = carpeta.listFiles((dir, name) -> name.toLowerCase().endsWith(".asm"));
        if (archivos == null || archivos.length == 0) {
            JOptionPane.showMessageDialog(this, "No se encontraron archivos .asm en la carpeta seleccionada.");
            return;
        }

        String[] columnas = {"Seleccionar", "Nombre", "Tamaño (bytes)", "Última modificación"};
        Object[][] datos = new Object[archivos.length][4];
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (int i = 0; i < archivos.length; i++) {
            datos[i][0] = Boolean.TRUE; // por defecto todos seleccionados
            datos[i][1] = archivos[i].getName();
            datos[i][2] = archivos[i].length();
            datos[i][3] = sdf.format(archivos[i].lastModified());
        }

        DefaultTableModel modelo = new DefaultTableModel(datos, columnas) {
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0) ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int r, int c) {
                return c == 0;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(22);
        tabla.getColumnModel().getColumn(0).setMaxWidth(100);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(650, 300));

        int opcion = JOptionPane.showConfirmDialog(this, scroll,
                "Selecciona los archivos ASM a cargar",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (opcion != JOptionPane.OK_OPTION) return;

        archivosSeleccionados.clear();
        int cargados = 0, enEspera = 0;
        for (int i = 0; i < archivos.length; i++) {
            boolean seleccionado = (Boolean) modelo.getValueAt(i, 0);
            if (!seleccionado) continue;

            archivosSeleccionados.add(archivos[i]);

            try {
                List<String> lineas = Files.readAllLines(archivos[i].toPath(), StandardCharsets.UTF_8);
                Programa cargado = Cargador.parsear(lineas);

                BCP nuevoBCP = new BCP();
                nuevoBCP.idProceso = contadorProcesos++;
                nuevoBCP.cambiarEstado(EstadoProceso.NUEVO);

                Proceso nuevo = new Proceso(nuevoBCP.idProceso, cargado, nuevoBCP, archivos[i]);

                if (contProgramas < 5) {
                    cargarEnMemoria(nuevo);
                    cargados++;
                } else {
                    nuevo.bcp.cambiarEstado(EstadoProceso.ESPERA);
                    colaEspera.add(nuevo);
                    enEspera++;
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error al cargar " + archivos[i].getName() + ":\n" + ex.getMessage(),
                        "Error de carga", JOptionPane.ERROR_MESSAGE);
            }
        }

        JOptionPane.showMessageDialog(this,
                "Archivos cargados: " + cargados + "\nArchivos en espera: " + enEspera,
                "Resultado de carga", JOptionPane.INFORMATION_MESSAGE);

        modeloMemoria.fireTableDataChanged();
        lblEstado.setText("Archivos cargados correctamente desde: " + carpeta.getName());
    }

    private void ejecutarPaso() {
        tiempoGlobal++; // Reloj global incrementa en cada paso

        // ==========================================================
        // PLANIFICACIÓN SRT (Shortest Remaining Time First)
        // ==========================================================
        if (modoPlanificacion.equalsIgnoreCase("SRT")) {

            // Mover procesos nuevos a la cola SRT cuando llegan
            for (Proceso p : listaProcesos) {
                if (p.bcp.estado == EstadoProceso.NUEVO && p.bcp.tiempoLlegada <= tiempoGlobal) {
                    p.bcp.cambiarEstado(EstadoProceso.LISTO);
                    colaListosSRT.offer(p);
                }
            }

            // Si no hay proceso actual, elegir el primero con menor ráfaga restante
            if (procesoActual == null) {
                procesoActual = colaListosSRT.poll();
                if (procesoActual == null) {
                    lblEstado.setText("Esperando procesos listos (SRT)...");
                    return;
                }

                cpu.reiniciar();
                bcp = procesoActual.bcp;
                bcp.cambiarEstado(EstadoProceso.EJECUTANDO);
                lblEstado.setText("Ejecutando (SRT): " + procesoActual.archivo.getName());

                // Cargar instrucciones del proceso actual
                modeloInstrucciones.setRowCount(0);
                for (Instruccion inst : procesoActual.programa.instrucciones) {
                    modeloInstrucciones.addRow(new Object[]{
                            inst.toString(),
                            inst.aBinario()
                    });
                }
            }

            // Ejecutar una instrucción
            if (cpu.PC < procesoActual.programa.longitud()) {
                if (instruccionActual == null) {
                    instruccionActual = procesoActual.programa.obtener(cpu.PC);
                    ciclosPendientes = DURACIONES.getOrDefault(instruccionActual.opcode, 1);
                }

                ciclosPendientes--;

                if (ciclosPendientes <= 0) {
                    ejecutarInstruccion(instruccionActual);
                    cpu.PC++;
                    procesoActual.bcp.rafagaRestante--;
                    instruccionActual = null;
                }

                // Si la ráfaga restante llega a 0, entonces el proceso ha terminado
                if (procesoActual.bcp.rafagaRestante <= 0 ||
                    cpu.estado == CPU.Estado.TERMINADO ||
                    cpu.PC >= procesoActual.programa.longitud()) {

                    procesoActual.bcp.cambiarEstado(EstadoProceso.TERMINADO);
                    procesoActual.bcp.tiempoFinalizacion = tiempoGlobal;
                    procesoActual.bcp.tiempoRetorno =
                            procesoActual.bcp.tiempoFinalizacion - procesoActual.bcp.tiempoLlegada;
                    procesoActual.bcp.tiempoEspera =
                            procesoActual.bcp.tiempoRetorno - procesoActual.bcp.rafaga;

                    registrarEstadistica();
                    lblEstado.setText("Proceso finalizado (SRT): " + procesoActual.archivo.getName());
                    procesoActual = null;
                }
            }

            // Preempción: si hay otro proceso con menor ráfaga restante
            Proceso candidato = colaListosSRT.peek();
            if (candidato != null && procesoActual != null &&
                candidato.bcp.rafagaRestante < procesoActual.bcp.rafagaRestante) {

                // Preempción
                procesoActual.bcp.cambiarEstado(EstadoProceso.LISTO);
                colaListosSRT.offer(procesoActual);
                procesoActual = colaListosSRT.poll();

                cpu.reiniciar();
                bcp = procesoActual.bcp;
                bcp.cambiarEstado(EstadoProceso.EJECUTANDO);
                lblEstado.setText("Cambio de contexto (SRT): " + procesoActual.archivo.getName());
            }

            actualizarVistas();
            modeloMemoria.fireTableDataChanged();
            return;
        }

        // ==========================================================
        // PLANIFICACIÓN FCFS (Primer Proyecto)
        // ==========================================================
        if (procesoActual == null) {
            procesoActual = cabeza;
            if (procesoActual == null) {
                lblEstado.setText("No hay programas cargados.");
                return;
            }

            cpu.reiniciar();
            bcp = procesoActual.bcp;
            lblEstado.setText("Ejecutando: " + procesoActual.archivo.getName());
            estadisticaActual = new Estadistica(Integer.toString(bcp.idProceso));

            modeloInstrucciones.setRowCount(0);
            for (Instruccion inst : procesoActual.programa.instrucciones) {
                modeloInstrucciones.addRow(new Object[]{
                        inst.toString(),
                        inst.aBinario()
                });
            }
        }

        procesoActual.bcp.cambiarEstado(EstadoProceso.EJECUTANDO);

        // Si terminó este proceso, entonces pasa al siguiente
        if (cpu.estado == CPU.Estado.TERMINADO || cpu.PC >= procesoActual.programa.longitud()) {
            procesoActual.bcp.cambiarEstado(EstadoProceso.TERMINADO);
            temporizador.detener();
            registrarEstadistica();
            lblEstado.setText("Programa finalizado.");
            actualizarVistas();

            // Remover el proceso finalizado
            if (procesoActual == cabeza) {
                cabeza = cabeza.siguiente;
                if (cabeza == null) cola = null;
            } else {
                Proceso p = cabeza;
                while (p != null && p.siguiente != null) {
                    if (p.siguiente == procesoActual) {
                        p.siguiente = procesoActual.siguiente;
                        if (p.siguiente == null) cola = p;
                        break;
                    }
                    p = p.siguiente;
                }
            }

            contProgramas = Math.max(0, contProgramas - 1);

            if (!colaEspera.isEmpty()) {
                Proceso siguienteEnEspera = colaEspera.poll();
                cargarEnMemoria(siguienteEnEspera);
            }

            procesoActual = cabeza;
            if (procesoActual == null) {
                lblEstado.setText("Todos los programas han sido ejecutados.");
                modeloInstrucciones.setRowCount(0);
                actualizarVistas();
                return;
            } else {
                cpu.reiniciar();
                bcp = procesoActual.bcp;
                bcp.cambiarEstado(EstadoProceso.EJECUTANDO);
                lblEstado.setText("Ejecutando: " + procesoActual.archivo.getName());

                modeloInstrucciones.setRowCount(0);
                for (Instruccion inst : procesoActual.programa.instrucciones) {
                    modeloInstrucciones.addRow(new Object[]{
                            inst.toString(),
                            inst.aBinario()
                    });
                }
                estadisticaActual = new Estadistica(Integer.toString(bcp.idProceso));
                return;
            }
        }

        // Ejecución paso a paso
        if (instruccionActual == null) {
            if (cpu.PC >= procesoActual.programa.longitud()) {
                cpu.estado = CPU.Estado.TERMINADO;
                registrarEstadistica();
                lblEstado.setText("Fin del programa.");
                return;
            }
            instruccionActual = procesoActual.programa.obtener(cpu.PC);
            ciclosPendientes = DURACIONES.getOrDefault(instruccionActual.opcode, 1);
        }

        ciclosPendientes--;

        if (ciclosPendientes <= 0) {
            ejecutarInstruccion(instruccionActual);
            cpu.PC++;
            instruccionActual = null;
        }

        actualizarVistas();
        modeloMemoria.fireTableDataChanged();
    }




    private void ejecutarInstruccion(Instruccion inst) {
        String op = inst.opcode;
        List<String> args = inst.operandos;
        try {
            switch (op) {
                case "MOV" -> {
                    String destino = args.get(0);
                    String origen = args.get(1);

                    // MOV REG, REG o MOV REG, INMEDIATO
                    if (cpu.registros.containsKey(destino)) {
                        int valor = origen.matches("[-+]?[0-9]+")
                                ? Integer.parseInt(origen)
                                : cpu.obtenerRegistro(origen);
                        cpu.asignarRegistro(destino, valor);
                    }

                    // MOV [n], REG, escritura en memoria
                    else if (destino.startsWith("[")) {
                        int desplazamiento = Integer.parseInt(destino.replaceAll("[\\[\\]]", ""));
                        int direccion = -1;

                        switch (modoMemoria.toUpperCase()) {
                            // SEGMENTACIÓN
                            case "SEGMENTACION" -> {
                                direccion = procesoActual.bcp.baseDatos + desplazamiento;
                                if (direccion < procesoActual.bcp.baseDatos || direccion > procesoActual.bcp.limiteDatos)
                                    throw new RuntimeException("Violación de segmento en MOV");
                            }

                            // DINÁMICA
                            case "DINAMICA" -> {
                                direccion = procesoActual.bcp.baseCodigo + desplazamiento;
                                if (direccion < procesoActual.bcp.baseCodigo || direccion > procesoActual.bcp.limiteCodigo)
                                    throw new RuntimeException("Violación de bloque dinámico en MOV");
                            }

                            // VIRTUAL (paginación)
                            case "VIRTUAL" -> {
                                int tamanoPagina = memoria.tamanoPagina;
                                int pagina = desplazamiento / tamanoPagina;
                                int offset = desplazamiento % tamanoPagina;

                                Integer basePagina = procesoActual.tablaPaginas.get(pagina);
                                if (basePagina == null)
                                    throw new RuntimeException("Fallo de página en MOV (página no asignada)");

                                direccion = basePagina + offset;
                            }

                            default -> throw new RuntimeException("Modo de memoria no reconocido: " + modoMemoria);
                        }

                        String valor = String.valueOf(cpu.obtenerRegistro(origen));
                        memoria.asignarCelda(direccion, valor);
                        procesoActual.bcp.ultimoResultado = "Dir " + direccion + " = " + valor;
                    } else {
                        throw new RuntimeException("Sintaxis de MOV no válida: " + destino + ", " + origen);
                    }
                }


                case "LOAD" -> {
                    String reg = args.get(0);
                    int desplazamiento = cpu.obtenerRegistro(reg);
                    int direccion = -1;

                    switch (modoMemoria.toUpperCase()) {
                        case "SEGMENTACION" -> {
                            direccion = procesoActual.bcp.baseDatos + desplazamiento;
                            if (direccion < procesoActual.bcp.baseDatos || direccion > procesoActual.bcp.limiteDatos)
                                throw new RuntimeException("Violación de segmento de datos en LOAD");
                        }

                        case "DINAMICA" -> {
                            direccion = procesoActual.bcp.baseCodigo + desplazamiento;
                            if (direccion < procesoActual.bcp.baseCodigo || direccion > procesoActual.bcp.limiteCodigo)
                                throw new RuntimeException("Violación de bloque dinámico en LOAD");
                        }

                        case "VIRTUAL" -> {
                            int tamanoPagina = memoria.tamanoPagina;
                            int pagina = desplazamiento / tamanoPagina;
                            int offset = desplazamiento % tamanoPagina;

                            Integer basePagina = procesoActual.tablaPaginas.get(pagina);
                            if (basePagina == null)
                                throw new RuntimeException("Fallo de página en LOAD (página no asignada)");

                            direccion = basePagina + offset;
                        }

                        default -> throw new RuntimeException("Modo de memoria no reconocido: " + modoMemoria);
                    }

                    String valor = memoria.obtenerRaw(direccion);
                    try {
                        cpu.AC = Integer.parseInt(valor.trim());
                    } catch (Exception e) {
                        cpu.AC = 0;
                    }
                    cpu.ZF = (cpu.AC == 0);
                }


                case "STORE" -> {
                    int desplazamiento = cpu.obtenerRegistro(args.get(0));
                    int direccion = -1;

                    switch (modoMemoria.toUpperCase()) {
                        case "SEGMENTACION" -> {
                            direccion = procesoActual.bcp.baseDatos + desplazamiento;
                            if (direccion < procesoActual.bcp.baseDatos || direccion > procesoActual.bcp.limiteDatos)
                                throw new RuntimeException("Violación de segmento de datos en STORE");
                        }

                        case "DINAMICA" -> {
                            direccion = procesoActual.bcp.baseCodigo + desplazamiento;
                            if (direccion < procesoActual.bcp.baseCodigo || direccion > procesoActual.bcp.limiteCodigo)
                                throw new RuntimeException("Violación de bloque dinámico en STORE");
                        }

                        case "VIRTUAL" -> {
                            int tamanoPagina = memoria.tamanoPagina;
                            int pagina = desplazamiento / tamanoPagina;
                            int offset = desplazamiento % tamanoPagina;

                            Integer basePagina = procesoActual.tablaPaginas.get(pagina);
                            if (basePagina == null)
                                throw new RuntimeException("Fallo de página en STORE (página no asignada)");

                            direccion = basePagina + offset;
                        }

                        default -> throw new RuntimeException("Modo de memoria no reconocido: " + modoMemoria);
                    }

                    String valor = String.valueOf(cpu.AC);
                    memoria.asignarCelda(direccion, valor);
                    procesoActual.bcp.ultimoResultado = "STORE -> Dir " + direccion + " = " + valor;
                }




                case "ADD" -> {
                    cpu.AC += cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                }

                case "SUB" -> {
                    cpu.AC -= cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                }

                case "INC" -> {
                    if (args.isEmpty()) {
                        cpu.AC++;
                        cpu.ZF = (cpu.AC == 0);
                    } else {
                        String r = args.get(0);
                        int newVal = cpu.obtenerRegistro(r) + 1;
                        cpu.asignarRegistro(r, newVal);
                        cpu.ZF = (newVal == 0);
                    }
                }

                case "DEC" -> {
                    if (args.isEmpty()) {
                        cpu.AC--;
                        cpu.ZF = (cpu.AC == 0);
                    } else {
                        String r = args.get(0);
                        int newVal = cpu.obtenerRegistro(r) - 1;
                        cpu.asignarRegistro(r, newVal);
                        cpu.ZF = (newVal == 0);
                    }
                }

                case "SWAP" -> {
                    String r1 = args.get(0), r2 = args.get(1);
                    int v1 = cpu.obtenerRegistro(r1);
                    int v2 = cpu.obtenerRegistro(r2);
                    cpu.asignarRegistro(r1, v2);
                    cpu.asignarRegistro(r2, v1);
                }

                case "CMP" -> {
                    int a = cpu.obtenerRegistro(args.get(0));
                    int b = cpu.obtenerRegistro(args.get(1));
                    cpu.ZF = (a == b);
                    cpu.CF = (a < b);
                }

                case "JE" -> {
                    if (cpu.ZF) cpu.PC = resolverDestino(args.get(0));
                }

                case "JNE" -> {
                    if (!cpu.ZF) cpu.PC = resolverDestino(args.get(0));
                }

                case "JMP" -> cpu.PC = resolverDestino(args.get(0));

                case "PUSH" -> cpu.pila.push(cpu.obtenerRegistro(args.get(0)));

                case "POP" -> {
                    if (cpu.pila.isEmpty()) throw new RuntimeException("Pila vacía");
                    cpu.asignarRegistro(args.get(0), cpu.pila.pop());
                }

                case "PARAM" -> {
                    if (args.size() > 3) throw new RuntimeException("PARAM admite máximo 3 valores");
                    for (String sval : args) {
                        if (!sval.matches("[-+]?[0-9]+")) throw new RuntimeException("PARAM solo acepta números");
                        cpu.pila.push(Integer.valueOf(sval));
                    }
                }

                case "INT" -> manejarINT(args.get(0));

                case "HALT" -> cpu.estado = CPU.Estado.TERMINADO;

                case "NOP" -> {
                }

                default -> throw new RuntimeException("Instrucción no implementada: " + op);
            }
        } catch (RuntimeException e) {
            cpu.estado = CPU.Estado.ERROR;
            lblEstado.setText("Error en instrucción: " + inst.opcode + " -> " + e.getMessage());
            temporizador.detener();
        }
    }

    private int resolverDestino(String token) {
        token = token.trim();

        if (token.matches("[+-]\\d+")) {
            return cpu.PC + Integer.parseInt(token);
        }

        Integer pos = procesoActual.programa.etiquetas.get(token.toUpperCase());
        if (pos != null) return pos;

        if (token.matches("\\d+")) return Integer.parseInt(token);
        throw new RuntimeException("Destino inválido: " + token);
    }
    
    private void manejarINT(String code) {
        code = code.toUpperCase().replace("H", "");

        switch (code) {
            case "20" -> {
                cpu.estado = CPU.Estado.TERMINADO;
                temporizador.detener();
                lblEstado.setText("INT 20H -> Programa finalizado");
            }

            case "10" -> JOptionPane.showMessageDialog(this,
                        "INT 10H -> Valor en DX = " + cpu.obtenerRegistro("DX"),
                        "Salida de pantalla", JOptionPane.INFORMATION_MESSAGE);

            case "09" -> {
                while (true) {
                    JTextField txtInput = new JTextField();
                    txtInput.setDocument(new javax.swing.text.PlainDocument() {
                        @Override
                        public void insertString(int offs, String str, javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                            if (str == null) return;
                            if (getLength() + str.length() > 3) return;
                            if (!str.matches("\\d+")) return;
                            super.insertString(offs, str, a);
                        }
                    });

                    int ok = JOptionPane.showConfirmDialog(this, txtInput,
                            "INT 09H -> Ingrese número (0-255)", JOptionPane.OK_CANCEL_OPTION);

                    if (ok != JOptionPane.OK_OPTION) {
                        continue;
                    }

                    String valStr = txtInput.getText().trim();
                    if (valStr.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Debe ingresar un valor.", "Error", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }

                    try {
                        int val = Integer.parseInt(valStr);
                        if (val < 0 || val > 255) {
                            JOptionPane.showMessageDialog(this, "El valor debe estar entre 0 y 255.", "Error", JOptionPane.ERROR_MESSAGE);
                            continue;
                        }
                        cpu.asignarRegistro("DX", val);
                        lblEstado.setText("INT 09H -> DX = " + val);
                        break;
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Entrada inválida. Debe ser numérica.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }


            default -> throw new RuntimeException("INT no soportado: " + code);
        }
    }

    private int obtenerValorOperando(String token) {
        if (cpu.registros.containsKey(token)) return cpu.obtenerRegistro(token);
        if (token.equals("AC")) return cpu.AC;
        if (token.matches("[-+]?[0-9]+")) return Integer.parseInt(token);
        throw new RuntimeException("Operando inválido: " + token);
    }

    private void actualizarVistas() {
        lblPC.setText(String.valueOf(cpu.PC));
        lblAC.setText(String.valueOf(cpu.AC));
        lblAX.setText(String.valueOf(cpu.obtenerRegistro("AX")));
        lblBX.setText(String.valueOf(cpu.obtenerRegistro("BX")));
        lblCX.setText(String.valueOf(cpu.obtenerRegistro("CX")));
        lblDX.setText(String.valueOf(cpu.obtenerRegistro("DX")));
        lblZF.setText(String.valueOf(cpu.ZF));

        if (procesoActual != null) {
            BCP bcpLocal = procesoActual.bcp;
            lblIdProceso.setText(String.valueOf(bcpLocal.idProceso));
            lblEstadoBCP.setText(bcpLocal.estado.toString());
            lblBaseCodigo.setText(String.valueOf(bcpLocal.baseCodigo));
            lblLimiteCodigo.setText(String.valueOf(bcpLocal.limiteCodigo));
            lblBaseDatos.setText(String.valueOf(bcpLocal.baseDatos));
            lblUltimoResultado.setText(bcpLocal.ultimoResultado);

            Programa prog = procesoActual.programa;
            if (prog != null && cpu.PC < prog.longitud()) {
                lblIR.setText(prog.obtener(cpu.PC).aBinario());
            }
        }
    }
    
    private void registrarEstadistica() {
        if (estadisticaActual != null) {
            estadisticaActual.marcarFin();
            estadisticas.add(estadisticaActual);
            JOptionPane.showMessageDialog(this,
                    "Programa finalizado.\n" + estadisticaActual.toString(),
                    "Estadística", JOptionPane.INFORMATION_MESSAGE);
            estadisticaActual = null;
        }
    }

    private void mostrarEstadisticas() {
        if (estadisticas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay estadísticas registradas.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Estadistica est : estadisticas) {
            sb.append(est.toString()).append("\n");
        }
        JTextArea area = new JTextArea(sb.toString(), 10, 50);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Historial de Estadísticas", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private int obtenerPCAbsoluto() {
        if (procesoActual == null || procesoActual.programa == null) return -1;
        return procesoActual.bcp.baseCodigo + cpu.PC;
    }
    
    private List<BCP> obtenerTodosLosBCPs() {
        List<BCP> lista = new ArrayList<>();

        // Procesos en memoria
        Proceso cursor = cabeza;
        while (cursor != null) {
            lista.add(cursor.bcp);
            cursor = cursor.siguiente;
        }

        // Procesos en espera
        for (Proceso p : colaEspera) {
            lista.add(p.bcp);
        }

        return lista;
    }
    
    private void mostrarEstadosBCP() {
        // Crear tabla con los datos de todos los procesos
        String[] columnas = {"PID", "Estado", "Base Código", "Límite Código", "Base Datos"};
        List<BCP> todos = obtenerTodosLosBCPs(); // función que recorre tu lista de procesos y retorna todos los BCP

        Object[][] datos = new Object[todos.size()][columnas.length];
        for (int i = 0; i < todos.size(); i++) {
            BCP bcpLocal = todos.get(i);
            datos[i][0] = bcpLocal.idProceso;
            datos[i][1] = bcpLocal.estado;
            datos[i][2] = bcpLocal.baseCodigo;
            datos[i][3] = bcpLocal.limiteCodigo;
            datos[i][4] = bcpLocal.baseDatos;
        }

        JTable tabla = new JTable(datos, columnas);
        JScrollPane scroll = new JScrollPane(tabla);

        JDialog dialogo = new JDialog(this, "Estados de procesos", true);
        dialogo.add(scroll);
        dialogo.setSize(500, 300);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }
    
    public BCP getBcp() {
        return bcp;
    }
    
    private void onCambioModoMemoria(String modo) {
        switch (modo) {
            case "Memoria Física con Segmentación" ->
                System.out.println("Modo Segmentación activado.");
            case "Memoria Física Dinámica" ->
                System.out.println("Modo Dinámico activado.");
            case "Memoria Virtual Dinámica" ->
                System.out.println("Modo Virtual activado.");
        }
    }
    
    private void asignarTiemposDeLlegada() {
        if (listaProcesos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay procesos cargados.");
            return;
        }

        String[] columnas = {"PID", "Nombre", "Tiempo de llegada"};
        Object[][] datos = new Object[listaProcesos.size()][3];

        for (int i = 0; i < listaProcesos.size(); i++) {
            Proceso p = listaProcesos.get(i);
            datos[i][0] = p.pid;
            datos[i][1] = p.archivo.getName();
            datos[i][2] = p.bcp.tiempoLlegada;
        }

        DefaultTableModel modelo = new DefaultTableModel(datos, columnas);
        JTable tabla = new JTable(modelo);
        JScrollPane scroll = new JScrollPane(tabla);

        int res = JOptionPane.showConfirmDialog(this, scroll,
                "Editar tiempos de llegada", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            for (int i = 0; i < listaProcesos.size(); i++) {
                int tLlegada = Integer.parseInt(modelo.getValueAt(i, 2).toString());
                listaProcesos.get(i).bcp.tiempoLlegada = tLlegada;
            }
        }
    }


}