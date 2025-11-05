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
import java.util.Random;

public class VentanaSimulador extends JFrame {
    
    private static class Core {
        public final CPU cpu = new CPU();
        public int idCore; 
        public Proceso procesoActual = null;
        public Instruccion instruccionActual = null;
        public int ciclosPendientes = 0;
        public int contadorQuantum = 0; // para RR por núcleo
        // constructor vacío
    }
    
    DefaultTableModel modeloPlanificacion = new DefaultTableModel();
    int numCores = 16;
    int tiempoMax = 80;
    

    // Configuración multiprocesador
    private int numProcesadores = 4;      // cambia entre 2..4 según UI o configuración
    private final int coresPorProcesador = 4;
    private List<Core> cores = new ArrayList<>(); // tamaño = numProcesadores * coresPorProcesador

    private final Memoria memoria = new Memoria(512, 64);
    private final CPU cpu = new CPU();
    public static BCP bcp = new BCP();
    
    private JTable tablaPaginacion;
    private DefaultTableModel modeloPaginacion;
    
    private int proximaDireccionLibre = memoria.tamanoSO;  
    private int contProgramas = 0;
    private final Queue<Proceso> colaEspera = new LinkedList<>();
    private final List<File> archivosSeleccionados = new ArrayList<>();
    private int tamanoPagina = 8;  // Cada página tiene 8 celdas físicas
    private final List<Integer> marcosLibres = new ArrayList<>(); // Lista de marcos disponibles
    public static ArrayList<BCP> listaBCP = new ArrayList<>();
    
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
                "Memoria Virtual con Dinámica",
                "Memoria Física con tamaño Fijo",
                "Memoria Física con tamaño Paginacion"
        });
    
    private String modoMemoria = "NINGUNO";  // valor por defecto
    private List<int[]> huecosLibres = new ArrayList<>();
    //private final java.util.List<Integer> marcosLibres = new ArrayList<>();
    private final java.util.Map<Integer, Integer> tablaPaginas = new HashMap<>(); // numPagina a marco
    private int tamañoPagina = 16; // Tamaño de página (en celdas)
    private final java.util.Map<Integer, String> swapSpace = new HashMap<>(); // Simulación de swap
    //private String modoMemoria = "Seleccionar Modo de Memoria"; // Valor por defecto
    
    private final PriorityQueue<Proceso> colaListosSJF =
        new PriorityQueue<>(Comparator.comparingInt(p -> p.bcp.rafaga));

    
    private Temporizador temporizador;

    
    private final java.util.List<Estadistica> estadisticas = new ArrayList<>();
    private Estadistica estadisticaActual = null;
    
    private final JComboBox<String> cbPlanificacion =
        new JComboBox<>(new String[]{
                "FCFS",
                "SJF",
                "SRT",
                "RR",
                "HRRN"
        });


    //private final JLabel lblModoPlanificacion = new JLabel("Planificación: FCFS");
    
    private String modoPlanificacion = "FCFS";
    private int tiempoGlobal = 0;
    private final List<Proceso> listaProcesos = new ArrayList<>();
    private final PriorityQueue<Proceso> colaListosSRT =
            new PriorityQueue<>(Comparator.comparingInt(p -> p.bcp.rafagaRestante));
    
    private final Queue<Proceso> colaListosRR = new LinkedList<>();
    private int quantum = 3; // Round Robin de 3 en este caso


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

    public VentanaSimulador() {
        super("MiniPC - Tarea 1");
        inicializarProcesadores(2);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 650));
        setLocationRelativeTo(null);
        
        modeloPlanificacion.addColumn("Núcleo");
        modeloPlanificacion.addColumn("Proceso");

        for (int t = 1; t <= tiempoMax; t++) {
            modeloPlanificacion.addColumn(String.valueOf(t));
        }

        // Crear filas: Núcleo + Proceso
        for (int i = 0; i < numCores; i++) {
            modeloPlanificacion.addRow(new Object[]{"Núcleo " + (i + 1), ""});
        }

        JTable tablaPlanificacion = new JTable(modeloPlanificacion);
        tablaPlanificacion.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaPlanificacion.setRowHeight(25);
        tablaPlanificacion.setFont(new Font("Consolas", Font.PLAIN, 12));


        JScrollPane scroll = new JScrollPane(tablaPlanificacion);
        scroll.setPreferredSize(new Dimension(1000, 150));
        
        
        
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
                
                case "Memoria Física con tamaño Fijo" -> {
                    modoMemoria = "FIJO";
                    huecosLibres.clear();
                    huecosLibres.add(new int[]{memoria.tamanoSO, memoria.tamano - 1});
                    lblEstado.setText("Modo de memoria cambiado a: Fijo");
                }
                
                case "Memoria Física con tamaño Paginacion" -> {
                    modoMemoria = "PAGINACION";
                    huecosLibres.clear();
                    huecosLibres.add(new int[]{memoria.tamanoSO, memoria.tamano - 1});
                    lblEstado.setText("Modo de memoria cambiado a: Paginacion");
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
                            """
                            Modo activado: Memoria Virtual con Din\u00e1mica
                            Tama\u00f1o de p\u00e1gina: """ + tamañoPagina + " celdas\n" +
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
            } else if (seleccion.contains("SJF")) {
                modoPlanificacion = "SJF";
            } else if (seleccion.contains("HRRN")) {
                modoPlanificacion = "HRRN";
            } else if (seleccion.contains("RR")) {
                modoPlanificacion = "RR";
            } else {
                modoPlanificacion = "FCFS";
            }

            lblEstado.setText("Planificador seleccionado: " + modoPlanificacion);

            // Si el usuario selecciona RR, puedes preguntar por el quantum
            if (modoPlanificacion.equalsIgnoreCase("RR")) {
                String input = JOptionPane.showInputDialog(this,
                        "Ingrese el valor de Quantum (en ciclos):", quantum);
                try {
                    if (input != null && !input.isEmpty()) {
                        quantum = Integer.parseInt(input);
                        if (quantum <= 0) quantum = 4;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Valor inválido. Se usará el quantum por defecto (4).");
                    quantum = 4;
                }
            }
        });
        
        
        
        JTabbedPane pestañasDerecha = new JTabbedPane();
        pestañasDerecha.addTab("CPU", construirPanelCPU());
        pestañasDerecha.addTab("BCP", construirPanelBCP());
        pestañasDerecha.addTab("Paginación", construirPanelPaginacion());
        pestañasDerecha.addTab("Tabla", new JScrollPane(tablaPlanificacion));

        JScrollPane scrollInstr = new JScrollPane(tablaInstrucciones);
        scrollInstr.setBorder(new TitledBorder("Instrucciones"));

        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollInstr, new JScrollPane(tablaMemoria));
        centro.setResizeWeight(0.4);

        JSplitPane divisionPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centro, pestañasDerecha);
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
        
        //panelDerecho.add(scroll, BorderLayout.SOUTH);

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
    
    private JPanel construirPanelPaginacion() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Tabla de Paginación"));

        inicializarTablaPaginacion();
        JScrollPane scroll = new JScrollPane(tablaPaginacion);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }
    

    private void inicializarTablaPaginacion() {
        // Crear el modelo con las columnas
        modeloPaginacion = new DefaultTableModel(
            new Object[]{"Proceso", "Página Lógica", "Marco Físico", "Presente"}, 0
        );

        // Crear la tabla y asignar el modelo
        tablaPaginacion = new JTable(modeloPaginacion);

        // Ajustar algunos detalles visuales opcionales
        tablaPaginacion.setFillsViewportHeight(true);
        tablaPaginacion.setRowHeight(24);
        tablaPaginacion.setEnabled(false); // solo visualización (puedes quitarlo si deseas editar)
    }



    private void limpiarTodo() {
        cpu.reiniciar();

        if (bcp != null) {
            bcp.cambiarEstado(EstadoProceso.TERMINADO);
        }
        
        memoria.limpiarSO();
        listaBCP.clear();

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
        modeloPaginacion = new DefaultTableModel(
            new Object[]{"Proceso", "Página Lógica", "Marco Físico", "Presente"}, 0
        );
        
        modeloPlanificacion.setRowCount(0);
        for (int i = 0; i < numCores; i++) {
            modeloPlanificacion.addRow(new Object[]{"Núcleo " + (i + 1), ""});
        }
        
        tiempoGlobal = 0;

        limpiarTodo();
        int cargados = 0, enEspera = 0;
        contadorProcesos = 0;
        memoria.proximaDireccionBCP = 0;
        proximaDireccionLibre = memoria.tamanoSO;
        
        memoria.inicializarParticiones();
        memoria.inicializarMarcos();

        for (File archivo : archivosSeleccionados) {
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
                Programa cargado = Cargador.parsear(lineas);

                BCP nuevoBCP = new BCP();
                nuevoBCP.idProceso = contadorProcesos++;
                nuevoBCP.nombre = archivo.getName();
                nuevoBCP.cambiarEstado(EstadoProceso.NUEVO);

                Proceso nuevo = new Proceso(nuevoBCP.idProceso, cargado, nuevoBCP, archivo);
                
                int dirBCP = memoria.guardarBCP(nuevoBCP);
                if (dirBCP == -1) {
                    // No hay espacio en la zona del SO para un nuevo BCP
                    JOptionPane.showMessageDialog(this,
                            "No hay espacio en la memoria del SO para más BCPs. Cancele o libere procesos.",
                            "SO lleno", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                
                nuevoBCP.setDirEnMemoria(dirBCP);
                Random rand = new Random();
                nuevoBCP.tiempoArribo = rand.nextInt(10);
                listaBCP.add(nuevo.bcp);
                
                cargarEnMemoria(nuevo);
                cargados++;

            } catch (ExcepcionAsm | HeadlessException | IOException ex) {
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
                memoria.actualizarBCP(p.bcp);

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
                memoria.actualizarBCP(p.bcp);

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
                memoria.actualizarBCP(p.bcp);

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
            
            // ======================================================
            // Memoria Fija
            // ======================================================
            
            case "FIJO" -> {
                
                
                Particion part = memoria.asignarParticion(p.bcp, p.programa.longitud());
                
                
                p.bcp.baseDatos = p.bcp.limiteCodigo + 1;
                if ( p.bcp.baseDatos >= memoria.tamano) {
                     // No hay espacio contiguo en memoria
                     JOptionPane.showMessageDialog(this, "No hay espacio en memoria para cargar el proceso en memoria.", "Memoria llena", JOptionPane.ERROR_MESSAGE);
                     // poner en espera
                     p.bcp.cambiarEstado(EstadoProceso.ESPERA);
                     memoria.actualizarBCP(p.bcp); // persistir cambio
                     colaEspera.add(p);
                     return;
                 }

                 // Escribir programa en memoria (usuario)
                for (int i = 0; i < cargado.longitud(); i++) {
                     memoria.asignarCelda(p.bcp.baseCodigo + i, cargado.lineaOriginal(i));
                 }

                 // completar BCP con direcciones reales
                  // si Programa define tamaño de datos
                p.bcp.cambiarEstado(EstadoProceso.NUEVO);
                 // actualizar el BCP en la zona del SO
                 memoria.actualizarBCP(p.bcp);
                 
                 // Ráfaga y tiempos
                p.bcp.rafaga = cargado.longitud();
                p.bcp.rafagaRestante = p.bcp.rafaga;
                p.bcp.tiempoLlegada = listaProcesos.isEmpty() ? 0 : tiempoGlobal + listaProcesos.size();

                if (!listaProcesos.contains(p)) listaProcesos.add(p);

                 // Encadenar en la lista enlazada (al final)
                 if (cabeza == null) cabeza = p;
                    else cola.siguiente = p;
                    cola = p;

                 proximaDireccionLibre = p.bcp.baseDatos; // avanzar el puntero
                 contProgramas++;
                 memoria.actualizarBCP(p.bcp);

                 modeloMemoria.fireTableDataChanged();
    
            }
        // ======================================================
        // Paginacion
        // ======================================================

        case "PAGINACION" -> {
            
             
             boolean cargado2 = memoria.asignarPaginas(p.bcp, p.programa.longitud());
             int baseFisicaPrimera = memoria.traducirDireccion(bcp, 0); // primera celda lógica
             int limiteFisicoUltima = memoria.traducirDireccion(bcp, p.programa.longitud() - 1);
             bcp.baseCodigo = baseFisicaPrimera;
             bcp.limiteCodigo = limiteFisicoUltima;

                if (!cargado2) {
                    JOptionPane.showMessageDialog(this, "Memoria llena (no hay marcos suficientes).", "Error", JOptionPane.ERROR_MESSAGE);
                    p.bcp.cambiarEstado(EstadoProceso.ESPERA);
                    memoria.actualizarBCP(p.bcp);
                    colaEspera.add(p);
                    return;
                }

                // En paginación el código lógico va de 0 a n-1
                p.bcp.baseCodigo = 0;
                p.bcp.limiteCodigo = cargado.longitud() - 1;
                
                System.out.println("📘 Tabla de páginas del proceso " + p.bcp.idProceso + ":");
                for (Pagina b : p.bcp.tablaPaginas) {
                    System.out.println("  Página " + b.numeroPagina + " → Marco " + b.marcoAsignado);
                }

            p.bcp.baseDatos = p.bcp.limiteCodigo + 1;
            if ( p.bcp.baseDatos >= memoria.tamano) {
                 // No hay espacio contiguo en memoria
                 JOptionPane.showMessageDialog(this, "No hay espacio en memoria para cargar el proceso en memoria.", "Memoria llena", JOptionPane.ERROR_MESSAGE);
                 // poner en espera
                 p.bcp.cambiarEstado(EstadoProceso.ESPERA);
                 memoria.actualizarBCP(p.bcp); // persistir cambio
                 colaEspera.add(p);
                 return;
             }

          
            for (int i = 0; i < cargado.longitud(); i++) {
                 memoria.asignarCelda(p.bcp.baseCodigo + i, cargado.lineaOriginal(i));
             }

             
            p.bcp.cambiarEstado(EstadoProceso.NUEVO);
                       memoria.actualizarBCP(p.bcp);

          
           p.bcp.rafaga = cargado.longitud();
           p.bcp.rafagaRestante = p.bcp.rafaga;
           p.bcp.tiempoLlegada = listaProcesos.isEmpty() ? 0 : tiempoGlobal + listaProcesos.size();

           if (!listaProcesos.contains(p)) listaProcesos.add(p);


             if (cabeza == null) {
                 cabeza = p;
                 cola = p;
             } else {
                 cola.siguiente = p;
                 cola = p;
             }

             proximaDireccionLibre = p.bcp.baseDatos; // avanzar el puntero
             contProgramas++;

             modeloMemoria.fireTableDataChanged();

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

        memoria.inicializarMarcos();
        memoria.inicializarParticiones();

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
                nuevoBCP.nombre = archivos[i].getName();
                nuevoBCP.cambiarEstado(EstadoProceso.NUEVO);

                Proceso nuevo = new Proceso(nuevoBCP.idProceso, cargado, nuevoBCP, archivos[i]);
                
                int dirBCP = memoria.guardarBCP(nuevoBCP);
                if (dirBCP == -1) {
                    // No hay espacio en la zona del SO para un nuevo BCP
                    JOptionPane.showMessageDialog(this,
                            "No hay espacio en la memoria del SO para más BCPs. Cancele o libere procesos.",
                            "SO lleno", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                
                nuevoBCP.setDirEnMemoria(dirBCP);
                Random rand = new Random();
                nuevoBCP.tiempoArribo = rand.nextInt(10);
                listaBCP.add(nuevo.bcp);
                
                cargarEnMemoria(nuevo);
                cargados++;
                

            } catch (ExcepcionAsm | HeadlessException | IOException ex) {
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
        tiempoGlobal++; // reloj global

        // 1) Mover procesos NUEVOS a las colas de listos cuando llegue su tiempo de arribo
        for (Proceso p : listaProcesos) {
            if (p.bcp.estado == EstadoProceso.NUEVO) {
                if (p.bcp.tiempoArribo <= tiempoGlobal) {
                    p.bcp.cambiarEstado(EstadoProceso.LISTO);
                    memoria.actualizarBCP(p.bcp);

                    switch (modoPlanificacion.toUpperCase()) {
                        case "SRT" -> colaListosSRT.offer(p);
                        case "SJF" -> colaListosSJF.offer(p);
                        case "RR"  -> colaListosRR.offer(p);
                        default    -> { /* FCFS, HRRN, etc. manejan listaProcesos directamente */ }
                    }

                    System.out.println("⏰ " + p.bcp.nombre + " ha llegado (t=" + tiempoGlobal + ")");
                } else {
                    // Solo informar si sigue NUEVO y aún no llegó
                    System.out.println("⏳ " + p.bcp.nombre + " aún no ha llegado (arribo " + p.bcp.tiempoArribo + ")");
                }
            }
        }

        // 2) Despachar: asignar procesos en cola a cores libres
        for (Core core : cores) {
            if (core.procesoActual == null) { // core libre
                Proceso siguiente = null;
                switch (modoPlanificacion.toUpperCase()) {
                    case "SRT" -> siguiente = colaListosSRT.poll();
                    case "SJF" -> siguiente = colaListosSJF.poll();
                    case "RR"  -> siguiente = colaListosRR.poll();
                    case "HRRN" -> {
                        // Buscar mejor HRRN entre LISTOS y reservarlo inmediatamente
                        double mejor = -1;
                        Proceso c = null;
                        for (Proceso p : listaProcesos) {
                            if (p.bcp.estado == EstadoProceso.LISTO) {
                                int espera = tiempoGlobal - p.bcp.tiempoArribo; // usar tiempoArribo
                                double ratio = ((double) (espera + p.bcp.rafaga)) / p.bcp.rafaga;
                                if (ratio > mejor) { mejor = ratio; c = p; }
                            }
                        }
                        if (c != null) {
                            // reservarlo inmediatamente para que otro core no lo tome
                            c.bcp.cambiarEstado(EstadoProceso.EJECUTANDO); // reserva temporal
                            memoria.actualizarBCP(c.bcp);
                            siguiente = c;
                        }
                    }
                    case "FCFS" -> {
                        // Buscar el proceso LISTO con menor tiempo de arribo (el que llegó primero)
                        Proceso primero = null;
                        int menorArribo = Integer.MAX_VALUE;

                        for (Proceso p : listaProcesos) {
                            if (p.bcp.estado == EstadoProceso.LISTO && p.bcp.tiempoArribo < menorArribo) {
                                menorArribo = p.bcp.tiempoArribo;
                                primero = p;
                            }
                        }

                        if (primero != null) {
                            primero.bcp.cambiarEstado(EstadoProceso.EJECUTANDO);
                            memoria.actualizarBCP(primero.bcp);
                            siguiente = primero;
                        }
                    }

        default -> {
            // Otros modos no definidos (solo seguridad)
        }
                }

                if (siguiente != null) {
                    // Si vino de una cola, ya fue removido (poll). Si vino de listaProcesos (HRRN/FCFS),
                    // su estado ya fue marcado arriba (HRRN) o lo marcamos ahora (FCFS).
                    core.procesoActual = siguiente;

                    // Restaurar contexto si usás cambio de contexto real:
                    // if (siguiente.bcp.dirContextoValido) { restaurar registros a core.cpu desde bcp }
                    // else core.cpu.reiniciar();

                    core.cpu.reiniciar();
                    core.instruccionActual = null;
                    core.ciclosPendientes = 0;
                    if (modoPlanificacion.equalsIgnoreCase("RR")) core.contadorQuantum = quantum;

                    // marcar BCP (si no fue marcado antes en HRRN)
                    if (siguiente.bcp.estado != EstadoProceso.EJECUTANDO) {
                        siguiente.bcp.cambiarEstado(EstadoProceso.EJECUTANDO);
                        memoria.actualizarBCP(siguiente.bcp);
                    } else {
                        // ya fue marcado (HRRN), actualizar BCP por si cambio de campos
                        memoria.actualizarBCP(siguiente.bcp);
                    }

                    // Si era la cabeza (FCFS), removerla de la lista enlazada
                    if (siguiente == cabeza) {
                        cabeza = cabeza.siguiente;
                        if (cabeza == null) cola = null;
                    }
                }
            }
        }

        // 3) SRT preempción global: si hay candidato con menos ráfaga que algún core en ejecución
        if (modoPlanificacion.equalsIgnoreCase("SRT")) {
            Proceso candidato = colaListosSRT.peek();
            if (candidato != null) {
                Core coreAReemplazar = null;
                int mayorRestante = -1;
                for (Core core : cores) {
                    if (core.procesoActual != null) {
                        int rem = core.procesoActual.bcp.rafagaRestante;
                        if (rem > mayorRestante) { mayorRestante = rem; coreAReemplazar = core; }
                    }
                }
                if (coreAReemplazar != null && mayorRestante > candidato.bcp.rafagaRestante) {
                    // remover candidato de la cola para asignarlo directamente
                    colaListosSRT.remove(candidato);

                    // devolver proceso del core a la cola
                    Proceso viejo = coreAReemplazar.procesoActual;
                    viejo.bcp.cambiarEstado(EstadoProceso.LISTO);
                    memoria.actualizarBCP(viejo.bcp);
                    colaListosSRT.offer(viejo);

                    // asignar candidato al core
                    coreAReemplazar.procesoActual = candidato;
                    coreAReemplazar.cpu.reiniciar();
                    coreAReemplazar.instruccionActual = null;
                    coreAReemplazar.ciclosPendientes = 0;
                    candidato.bcp.cambiarEstado(EstadoProceso.EJECUTANDO);
                    memoria.actualizarBCP(candidato.bcp);

                    lblEstado.setText("Preempción SRT: intercambiado procesos entre cores");
                }
            }
        }

        // 4) Ejecutar un paso en cada core ocupado
        for (Core core : cores) {
            if (core.procesoActual == null) continue;

            Proceso proc = core.procesoActual;

            // finalizar si PC fuera del programa
            if (core.cpu.PC >= proc.programa.longitud()) {
                proc.bcp.cambiarEstado(EstadoProceso.TERMINADO);
                if(modoMemoria.toUpperCase().equals("PAGINACION")) memoria.liberarPaginas(proc.bcp);
                memoria.actualizarBCP(proc.bcp);
                proc.bcp.tiempoFinalizacion = tiempoGlobal;
                proc.bcp.tiempoRetorno = proc.bcp.tiempoFinalizacion - proc.bcp.tiempoArribo; // usar arribo
                proc.bcp.tiempoEspera = proc.bcp.tiempoRetorno - proc.bcp.rafaga;
                registrarEstadistica();
                lblEstado.setText("Proceso finalizado (Core): " + proc.archivo.getName());
                core.procesoActual = null;
                core.instruccionActual = null;
                continue;
            }

            if (core.instruccionActual == null) {
                core.instruccionActual = proc.programa.obtener(core.cpu.PC);
                core.ciclosPendientes = DURACIONES.getOrDefault(core.instruccionActual.opcode, 1);
            }

            core.ciclosPendientes--;

            if (core.ciclosPendientes <= 0) {
                ejecutarInstruccion(core.cpu, core.instruccionActual);

                // actualizar tabla Gantt: usa fila = core.idCore-1 si tu modelo está así
                actualizarTabla(core.idCore - 1, tiempoGlobal, proc.bcp.nombre);

                // actualizar BCP: tiempos, PC, rafaga restante...
                proc.bcp.incrementarTiempoCPU();
                memoria.actualizarBCP(proc.bcp);
                proc.bcp.avanzarPC();
                memoria.actualizarBCP(proc.bcp);

                core.cpu.PC++;
                proc.bcp.rafagaRestante--;
                core.instruccionActual = null;
            }

            // RR: manejar quantum por núcleo
            if (modoPlanificacion.equalsIgnoreCase("RR")) {
                core.contadorQuantum--;
                if (core.contadorQuantum <= 0 && core.procesoActual != null) {
                    core.procesoActual.bcp.cambiarEstado(EstadoProceso.LISTO);
                    memoria.actualizarBCP(core.procesoActual.bcp);
                    colaListosRR.offer(core.procesoActual);
                    // guardar contexto en BCP si querés simulación completa:
                    // guardarContextoEnBCP(core.procesoActual.bcp, core.cpu);
                    core.procesoActual = null;
                    core.instruccionActual = null;
                }
            }

            // chequeo de terminación por rafaga o estado del CPU
            if (core.procesoActual != null &&
                (core.procesoActual.bcp.rafagaRestante <= 0 || core.cpu.estado == CPU.Estado.TERMINADO ||
                 core.cpu.PC >= core.procesoActual.programa.longitud())) {

                Proceso terminado = core.procesoActual;
                terminado.bcp.cambiarEstado(EstadoProceso.TERMINADO);
                if(modoMemoria.toUpperCase().equals("PAGINACION")) memoria.liberarPaginas(proc.bcp);
                memoria.actualizarBCP(terminado.bcp);
                terminado.bcp.tiempoFinalizacion = tiempoGlobal;
                terminado.bcp.tiempoRetorno = terminado.bcp.tiempoFinalizacion - terminado.bcp.tiempoArribo;
                terminado.bcp.tiempoEspera = terminado.bcp.tiempoRetorno - terminado.bcp.rafaga;
                registrarEstadistica();
                lblEstado.setText("Proceso finalizado (Core): " + terminado.archivo.getName());
                core.procesoActual = null;
                core.instruccionActual = null;
            }
        }

        // 5) Si hay procesos en cola de espera, tratar de cargar en memoria
        if (!colaEspera.isEmpty()) {
            Proceso next = colaEspera.poll();
            cargarEnMemoria(next);
        }

        // 6) Actualizar vistas
        actualizarVistas();
        modeloMemoria.fireTableDataChanged();
        actualizarTablaPaginacion();
    }


    private void ejecutarInstruccion(CPU cpu,Instruccion inst) {
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
                                int tamanoPagina2 = memoria.tamanoPagina;
                                int pagina = desplazamiento / tamanoPagina2;
                                int offset = desplazamiento % tamanoPagina2;

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
                            int tamanoPagina2 = memoria.tamanoPagina;
                            int pagina = desplazamiento / tamanoPagina2;
                            int offset = desplazamiento % tamanoPagina2;

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
                    } catch (NumberFormatException e) {
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
                            int tamanoPagina2 = memoria.tamanoPagina;
                            int pagina = desplazamiento / tamanoPagina2;
                            int offset = desplazamiento % tamanoPagina2;

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
        List<BCP> todos = obtenerTodosLosBCPs(); // función que recorre la lista de procesos y retorna todos los BCP

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
    
    /**
    * Se usa por los algoritmos SJF, SRT y HRRN.
    * Permite al usuario asignar o ajustar los tiempos de llegada de los procesos cargados.
    */
    private void asignarTiemposDeLlegada() {
        if (listaProcesos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay procesos cargados en memoria.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Crear tabla editable los datos requeridos
        String[] columnas = {"PID", "Nombre del proceso", "Tiempo de llegada"};
        Object[][] datos = new Object[listaProcesos.size()][3];

        // Cargar los valores actuales o aumentarlos de forma automática
        int tiempoIncremental = 0;
        for (int i = 0; i < listaProcesos.size(); i++) {
            Proceso p = listaProcesos.get(i);
            datos[i][0] = p.pid;
            datos[i][1] = p.archivo.getName();
            // Si aún no tiene tiempo asignado, sugerir uno incremental
            datos[i][2] = (p.bcp.tiempoLlegada == 0) ? tiempoIncremental : p.bcp.tiempoLlegada;
            tiempoIncremental += 2; // Por defecto, los procesos caen en cada 2 unidades
        }

        DefaultTableModel modelo = new DefaultTableModel(datos, columnas) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2; // solo editable la columna de tiempo de llegada
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(24);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(550, 250));

        // Mostrar cuadro de diálogo
        int res = JOptionPane.showConfirmDialog(this, scroll,
                "Asignar tiempos de llegada a los procesos",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (res == JOptionPane.OK_OPTION) {
            // Validar y aplicar los tiempos
            for (int i = 0; i < listaProcesos.size(); i++) {
                try {
                    int tLlegada = Integer.parseInt(modelo.getValueAt(i, 2).toString());
                    if (tLlegada < 0) throw new NumberFormatException();

                    Proceso p = listaProcesos.get(i);
                    p.bcp.tiempoLlegada = tLlegada;

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Tiempo de llegada inválido en la fila " + (i + 1),
                            "Error de entrada",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Mostrar resumen visual
            StringBuilder resumen = new StringBuilder("Tiempos de llegada asignados:\n");
            for (Proceso p : listaProcesos) {
                resumen.append("• PID ")
                        .append(p.pid)
                        .append(" (")
                        .append(p.archivo.getName())
                        .append(") → Llegada: ")
                        .append(p.bcp.tiempoLlegada)
                        .append("\n");
            }

            JOptionPane.showMessageDialog(this, resumen.toString(),
                    "Tiempos asignados correctamente", JOptionPane.INFORMATION_MESSAGE);

            lblEstado.setText("Tiempos de llegada actualizados para planificación.");
        }
    }
    
    private void inicializarProcesadores(int numProc) {
        if (numProc < 2) numProc = 2;
        if (numProc > 4) numProc = 4;
        this.numProcesadores = numProc;
        cores.clear();
        int total = numProcesadores * coresPorProcesador;
        for (int i = 0; i < total; i++) {
             Core core = new Core();
             core.idCore = i+1;
            cores.add(core);
        }
        // Actualiza UI si quieres mostrar cantidad de cores
    }
    
    public void actualizarTabla(int nucleo, int tiempo, String nombreProceso) {
        
        modeloPlanificacion.setValueAt(nombreProceso , nucleo, tiempo + 1); 
        // +1 porque la col 0 es "Núcleo" y la 1 es "Proceso"
    }
    
    private void actualizarTablaPaginacion() {
        // Limpia los datos previos
        modeloPaginacion.setRowCount(0);

        // Recorre todos los procesos que están en memoria o listos
        for (Proceso p : listaProcesos) {
            if (p.bcp.tablaPaginas != null && !p.bcp.tablaPaginas.isEmpty()) {
                for (Pagina pag : p.bcp.tablaPaginas) {
                    Object[] fila = new Object[]{
                        "P" + p.bcp.idProceso,
                        pag.numeroPagina,
                        pag.marcoFisico >= 0 ? pag.marcoFisico : "—",
                        pag.cargada ? "✅" : "❌"
                    };
                    modeloPaginacion.addRow(fila);
                }
            }
        }

        // Refresca la tabla visualmente
        tablaPaginacion.revalidate();
        tablaPaginacion.repaint();
    }
    /*
    private void actualizarMapaMemoria() {
        int marcos = memoria.getCantidadMarcos();
        tablaPaginacion.removeAll();

        // Dibujar los marcos de memoria
        for (int i = 0; i < marcos; i++) {
            JLabel celda = new JLabel();
            celda.setOpaque(true);
            celda.setPreferredSize(new Dimension(25, 25));

            Pagina pag = memoria.getPaginaPorMarco(i);
            if (pag != null) {
                celda.setBackground(Color.getHSBColor((pag.procesoId * 0.15f) % 1f, 0.6f, 0.9f));
                celda.setToolTipText("Marco " + i + " → P" + pag.procesoId + " [Pág " + pag.numeroPagina + "]");
            } else {
                celda.setBackground(Color.LIGHT_GRAY);
            }

            panelMapa.add(celda);
        }

        // Actualizar el panel gráfico
        panelMapa.revalidate();
        panelMapa.repaint();

        // 🔁 También actualiza la tabla de paginación para mantenerla sincronizada
        actualizarTablaPaginacion();
    }
*/

}