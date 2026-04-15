import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class VentanaSistema extends JFrame {

    // ── Tema ──────────────────────────────────────────────────────────────
    static final Color BG_MAIN      = new Color(245, 247, 250);
    static final Color BG_CARD      = Color.WHITE;
    static final Color BG_CHART     = new Color(250, 250, 252);
    static final Color BORDER_COLOR = new Color(220, 222, 228);
    static final Color TEXT_DARK    = new Color(33,  37,  41);
    static final Color TEXT_GRAY    = new Color(108, 117, 125);
    static final Color COLOR_DELETE = new Color(211,  47,  47);
    static final Color[] BAR_COLORS = {
        new Color(66, 133, 244), new Color(52, 168, 83),
        new Color(251, 188, 4),  new Color(234, 67, 53),
        new Color(103, 58, 183)
    };

    // ── Datos ─────────────────────────────────────────────────────────────
    private Map<String, Vendedor> mapaVendedores      = new HashMap<>();
    private Map<String, Producto> mapaProductos       = new HashMap<>();
    private List<Vendedor>        vendedoresOrdenados = new ArrayList<>();
    private List<Producto>        productosOrdenados  = new ArrayList<>();

    // ── KPI ───────────────────────────────────────────────────────────────
    private JLabel lblNumVendedores, lblNumProductos, lblTotalVentas;

    // ── Gráfico (alterna vendedores/productos según tab activa) ───────────
    private GraficoBarrasPanel graficoPanel;
    private JLabel             lblTituloGrafico;
    private JPanel             cardCenter;
    private CardLayout         cardLayout;

    // ── Tablas ────────────────────────────────────────────────────────────
    private JTable            tablaVendedores, tablaProductos;
    private DefaultTableModel modeloVendedores, modeloProductos;
    private BarraRenderer     rendererVendedores, rendererProductos;

    // ── Comparación (vendedores Y productos) ──────────────────────────────
    private JComboBox<String> comboV1, comboV2;
    private JComboBox<String> comboProd1, comboProd2;
    private JComboBox<String> comboFiltroVendedorProd; // filtro vendedor en comparación productos
    private JLabel lblV1Nombre, lblV1Doc, lblV1Ventas, lblV1Rank;
    private JLabel lblV2Nombre, lblV2Doc, lblV2Ventas, lblV2Rank;
    private JLabel lblP1Nombre, lblP1Precio, lblP1Unidades, lblP1Total;
    private JLabel lblP2Nombre, lblP2Precio, lblP2Unidades, lblP2Total;
    private JTabbedPane tabsComparacion;

    // ── Controles ─────────────────────────────────────────────────────────
    private JButton btnGenerar, btnProcesar, btnEliminar;
    private JLabel  lblEstado;

    // ─────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new VentanaSistema().setVisible(true);
        });
    }

    public VentanaSistema() {
        super("Sistema de Gestión de Ventas CFP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 820);
        setMinimumSize(new Dimension(960, 640));
        construirUI();
        setLocationRelativeTo(null);
        loadResultados();
    }

    // ─────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────

    private void construirUI() {
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout());
        add(crearPanelKPI(),    BorderLayout.NORTH);
        add(crearPanelCentro(), BorderLayout.CENTER);
        add(crearPanelSur(),    BorderLayout.SOUTH);
    }

    // ── KPI ───────────────────────────────────────────────────────────────

    private JPanel crearPanelKPI() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_MAIN);
        wrap.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));

        JLabel titulo = new JLabel("Dashboard · Rendimiento de Ventas");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        titulo.setForeground(TEXT_DARK);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        wrap.add(titulo, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
        cards.setBackground(BG_MAIN);
        lblNumVendedores = new JLabel("–");
        lblNumProductos  = new JLabel("–");
        lblTotalVentas   = new JLabel("–");
        cards.add(crearKpiCard("Vendedores",   lblNumVendedores, BAR_COLORS[0]));
        cards.add(crearKpiCard("Productos",    lblNumProductos,  BAR_COLORS[1]));
        cards.add(crearKpiCard("Total Ventas", lblTotalVentas,   BAR_COLORS[3]));
        wrap.add(cards, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel crearKpiCard(String titulo, JLabel valor, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        JLabel t = new JLabel(titulo.toUpperCase());
        t.setFont(new Font("SansSerif", Font.PLAIN, 11));
        t.setForeground(TEXT_GRAY); t.setAlignmentX(LEFT_ALIGNMENT);
        valor.setFont(new Font("SansSerif", Font.BOLD, 28));
        valor.setForeground(accent); valor.setAlignmentX(LEFT_ALIGNMENT);
        JPanel bar = new JPanel(); bar.setBackground(accent);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        bar.setAlignmentX(LEFT_ALIGNMENT);
        card.add(t); card.add(Box.createVerticalStrut(6));
        card.add(valor); card.add(Box.createVerticalStrut(10)); card.add(bar);
        return card;
    }

    // ── Centro ────────────────────────────────────────────────────────────

    private JPanel crearPanelCentro() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(BG_MAIN);
        p.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        // Izquierda: gráfico
        JPanel izq = new JPanel(new BorderLayout(0, 4));
        izq.setBackground(BG_MAIN);
        izq.setPreferredSize(new Dimension(380, 0));

        lblTituloGrafico = new JLabel("Ventas por Vendedor");
        lblTituloGrafico.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTituloGrafico.setForeground(TEXT_DARK);
        lblTituloGrafico.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        graficoPanel = new GraficoBarrasPanel();
        cardLayout   = new CardLayout();
        cardCenter   = new JPanel(cardLayout);
        cardCenter.setBackground(BG_CHART);

        JLabel placeholder = new JLabel(
            "<html><center>Sin datos<br><small>Usa '↺ Generar nuevos datos'</small></center>",
            SwingConstants.CENTER);
        placeholder.setForeground(TEXT_GRAY);
        placeholder.setFont(new Font("SansSerif", Font.ITALIC, 14));

        cardCenter.add(graficoPanel, "chart");
        cardCenter.add(placeholder,  "placeholder");
        cardCenter.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        izq.add(lblTituloGrafico, BorderLayout.NORTH);
        izq.add(cardCenter,       BorderLayout.CENTER);

        // Derecha: tabla + comparación
        JTabbedPane tablaTabs = crearTabbedPane();
        JPanel der = new JPanel(new BorderLayout(0, 8));
        der.setBackground(BG_MAIN);
        der.add(tablaTabs,             BorderLayout.CENTER);
        der.add(crearPanelComparacion(), BorderLayout.SOUTH);

        p.add(izq, BorderLayout.WEST);
        p.add(der, BorderLayout.CENTER);
        return p;
    }

    // ── Tabbed pane de tablas ─────────────────────────────────────────────

    private JTabbedPane crearTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tabs.addTab("Vendedores", crearTabVendedores());
        tabs.addTab("Productos",  crearTabProductos());

        // Cambiar gráfico al cambiar de tab
        tabs.addChangeListener(e -> {
            boolean esVendedores = tabs.getSelectedIndex() == 0;
            if (esVendedores) {
                lblTituloGrafico.setText("Ventas por Vendedor");
                graficoPanel.setModo(GraficoBarrasPanel.MODO_VENDEDORES, vendedoresOrdenados, productosOrdenados);
            } else {
                lblTituloGrafico.setText("Unidades por Producto");
                graficoPanel.setModo(GraficoBarrasPanel.MODO_PRODUCTOS, vendedoresOrdenados, productosOrdenados);
            }
            graficoPanel.repaint();
        });
        return tabs;
    }

    private JScrollPane crearTabVendedores() {
        // Columnas: #, Nombre, Total ($), Unidades, Rendimiento%
        modeloVendedores = new DefaultTableModel(
                new String[]{"#", "Nombre", "Total ($)", "Unidades", "Rendimiento"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 4 ? Double.class : Object.class;
            }
        };
        tablaVendedores = new JTable(modeloVendedores);
        tablaVendedores.setRowHeight(28);
        tablaVendedores.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaVendedores.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaVendedores.setSelectionBackground(new Color(227, 238, 255));

        rendererVendedores = new BarraRenderer(BAR_COLORS[0]);
        tablaVendedores.getColumnModel().getColumn(0).setPreferredWidth(30);
        tablaVendedores.getColumnModel().getColumn(2).setPreferredWidth(110);
        tablaVendedores.getColumnModel().getColumn(3).setPreferredWidth(80);
        tablaVendedores.getColumnModel().getColumn(4).setCellRenderer(rendererVendedores);
        tablaVendedores.getColumnModel().getColumn(4).setPreferredWidth(160);
        return new JScrollPane(tablaVendedores);
    }

    private JScrollPane crearTabProductos() {
        // Columnas: ID, Nombre, Precio ($), Unidades, Total ($), Popularidad%
        modeloProductos = new DefaultTableModel(
                new String[]{"ID", "Nombre", "Precio ($)", "Unidades", "Total ($)", "Popularidad"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 5 ? Double.class : Object.class;
            }
        };
        tablaProductos = new JTable(modeloProductos);
        tablaProductos.setRowHeight(28);
        tablaProductos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaProductos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaProductos.setSelectionBackground(new Color(227, 255, 234));

        rendererProductos = new BarraRenderer(BAR_COLORS[1]);
        tablaProductos.getColumnModel().getColumn(5).setCellRenderer(rendererProductos);
        tablaProductos.getColumnModel().getColumn(5).setPreferredWidth(150);
        tablaProductos.getColumnModel().getColumn(4).setPreferredWidth(110);
        return new JScrollPane(tablaProductos);
    }

    // ── Panel de comparación (vendedores + productos en tabs) ─────────────

    private JPanel crearPanelComparacion() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_MAIN);
        wrap.setPreferredSize(new Dimension(0, 170));

        tabsComparacion = new JTabbedPane();
        tabsComparacion.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tabsComparacion.addTab("Comparar Vendedores", crearSubPanelVendedores());
        tabsComparacion.addTab("Comparar Productos",  crearSubPanelProductos());
        wrap.add(tabsComparacion, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel crearSubPanelVendedores() {
        comboV1 = new JComboBox<>();
        comboV2 = new JComboBox<>();
        ActionListener al = e -> actualizarComparacionVendedores();
        comboV1.addActionListener(al);
        comboV2.addActionListener(al);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setBackground(BG_MAIN);
        top.add(new JLabel("Vendedor 1:")); top.add(comboV1);
        top.add(Box.createHorizontalStrut(20));
        top.add(new JLabel("Vendedor 2:")); top.add(comboV2);

        lblV1Nombre = new JLabel(" "); lblV1Doc  = new JLabel(" ");
        lblV1Ventas = new JLabel(" "); lblV1Rank = new JLabel(" ");
        lblV2Nombre = new JLabel(" "); lblV2Doc  = new JLabel(" ");
        lblV2Ventas = new JLabel(" "); lblV2Rank = new JLabel(" ");

        JPanel cards = new JPanel(new GridLayout(1, 2, 10, 0));
        cards.setBackground(BG_MAIN);
        cards.add(crearTarjetaComp(lblV1Nombre, lblV1Doc, lblV1Ventas, lblV1Rank, BAR_COLORS[0]));
        cards.add(crearTarjetaComp(lblV2Nombre, lblV2Doc, lblV2Ventas, lblV2Rank, BAR_COLORS[2]));

        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setBackground(BG_MAIN);
        panel.add(top,   BorderLayout.NORTH);
        panel.add(cards, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearSubPanelProductos() {
        comboProd1 = new JComboBox<>();
        comboProd2 = new JComboBox<>();
        comboFiltroVendedorProd = new JComboBox<>();

        ActionListener al = e -> actualizarComparacionProductos();
        comboProd1.addActionListener(al);
        comboProd2.addActionListener(al);
        comboFiltroVendedorProd.addActionListener(al);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setBackground(BG_MAIN);
        top.add(new JLabel("Producto 1:")); top.add(comboProd1);
        top.add(Box.createHorizontalStrut(12));
        top.add(new JLabel("Producto 2:")); top.add(comboProd2);
        top.add(Box.createHorizontalStrut(20));
        JLabel lf = new JLabel("Filtrar vendedor:");
        lf.setForeground(TEXT_GRAY);
        top.add(lf); top.add(comboFiltroVendedorProd);

        lblP1Nombre   = new JLabel(" "); lblP1Precio   = new JLabel(" ");
        lblP1Unidades = new JLabel(" "); lblP1Total    = new JLabel(" ");
        lblP2Nombre   = new JLabel(" "); lblP2Precio   = new JLabel(" ");
        lblP2Unidades = new JLabel(" "); lblP2Total    = new JLabel(" ");

        JPanel cards = new JPanel(new GridLayout(1, 2, 10, 0));
        cards.setBackground(BG_MAIN);
        cards.add(crearTarjetaComp(lblP1Nombre, lblP1Precio, lblP1Unidades, lblP1Total, BAR_COLORS[1]));
        cards.add(crearTarjetaComp(lblP2Nombre, lblP2Precio, lblP2Unidades, lblP2Total, BAR_COLORS[4]));

        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setBackground(BG_MAIN);
        panel.add(top,   BorderLayout.NORTH);
        panel.add(cards, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearTarjetaComp(JLabel l1, JLabel l2, JLabel l3, JLabel l4, Color accent) {
        JPanel card = new JPanel(new GridLayout(4, 1, 2, 2));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(accent, 2, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        l1.setFont(new Font("SansSerif", Font.BOLD, 13)); l1.setForeground(accent);
        l2.setFont(new Font("SansSerif", Font.PLAIN, 11)); l2.setForeground(TEXT_GRAY);
        l3.setFont(new Font("SansSerif", Font.BOLD, 12)); l3.setForeground(TEXT_DARK);
        l4.setFont(new Font("SansSerif", Font.PLAIN, 11)); l4.setForeground(TEXT_GRAY);
        card.add(l1); card.add(l2); card.add(l3); card.add(l4);
        return card;
    }

    // ── Panel sur ─────────────────────────────────────────────────────────

    private JPanel crearPanelSur() {
        JPanel sur = new JPanel(new BorderLayout(0, 4));
        sur.setBackground(BG_MAIN);
        sur.setBorder(BorderFactory.createEmptyBorder(4, 14, 10, 14));

        btnGenerar  = new JButton("↺  Generar nuevos datos");
        btnProcesar = new JButton("▶  Procesar Datos");
        btnEliminar = new JButton("✕  Eliminar Archivos");
        btnGenerar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnProcesar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnEliminar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnEliminar.setForeground(COLOR_DELETE);
        btnGenerar.addActionListener(e  -> accionGenerarDatos());
        btnProcesar.addActionListener(e -> accionProcesarDatos());
        btnEliminar.addActionListener(e -> accionEliminarArchivos());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setBackground(BG_MAIN);
        botones.add(btnGenerar); botones.add(btnProcesar); botones.add(btnEliminar);

        lblEstado = new JLabel("Listo");
        lblEstado.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblEstado.setForeground(TEXT_GRAY);
        lblEstado.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG_MAIN);
        bottom.add(lblEstado, BorderLayout.EAST);

        sur.add(botones, BorderLayout.NORTH);
        sur.add(bottom,  BorderLayout.SOUTH);
        return sur;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Carga de datos
    // ─────────────────────────────────────────────────────────────────────

    public void loadResultados() {
        setButtonsEnabled(false);
        lblEstado.setText("Cargando datos...");

        String fecha   = GenerateInfoFiles.FECHA;
        String carpeta = GenerateInfoFiles.CARPETA;
        String sep     = GenerateInfoFiles.SEP;

        Path pProds  = Paths.get(carpeta + "productos_"  + fecha + ".csv");
        Path pVends  = Paths.get(carpeta + "vendedores_" + fecha + ".csv");

        if (!Files.exists(pProds) || !Files.exists(pVends)) {
            cardLayout.show(cardCenter, "placeholder");
            lblEstado.setText("Sin archivos — genera datos primero.");
            setButtonsEnabled(true);
            return;
        }

        new SwingWorker<Void, Void>() {
            Map<String, Producto> tmpP = new HashMap<>();
            Map<String, Vendedor> tmpV = new HashMap<>();

            @Override protected Void doInBackground() throws Exception {
                tmpP = Files.lines(pProds).skip(1)
                        .map(l -> { String[] d = l.split(sep);
                            return new Producto(d[0], d[1], Double.parseDouble(d[2])); })
                        .collect(Collectors.toMap(Producto::getId, x -> x));

                tmpV = Files.lines(pVends).skip(1)
                        .map(l -> { String[] d = l.split(sep);
                            return new Vendedor(d[0], d[1], d[2], d[3]); })
                        .collect(Collectors.toMap(Vendedor::getNumDoc, x -> x));

                Files.walk(Paths.get(carpeta))
                        .filter(p -> p.getFileName().toString().startsWith("vendedor_")
                                  && p.toString().endsWith(".csv"))
                        .forEach(p -> Main.procesarArchivoVenta(p, tmpP, tmpV, sep));
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    mapaProductos  = tmpP;
                    mapaVendedores = tmpV;
                    refreshUI();
                    lblEstado.setText("Datos cargados.");
                } catch (InterruptedException | ExecutionException ex) {
                    lblEstado.setText("Error: " + ex.getCause().getMessage());
                    ex.printStackTrace();
                }
                setButtonsEnabled(true);
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Refresco UI
    // ─────────────────────────────────────────────────────────────────────

    public void refreshUI() {
        vendedoresOrdenados = mapaVendedores.values().stream()
                .sorted(Comparator.comparingDouble(v -> -v.getVentasTotales()))
                .collect(Collectors.toList());
        productosOrdenados = mapaProductos.values().stream()
                .sorted(Comparator.comparingInt(p -> -p.getCantidadVendida()))
                .collect(Collectors.toList());

        // KPIs
        double totalVentas = vendedoresOrdenados.stream()
                .mapToDouble(Vendedor::getVentasTotales).sum();
        lblNumVendedores.setText(String.valueOf(mapaVendedores.size()));
        lblNumProductos .setText(String.valueOf(mapaProductos.size()));
        lblTotalVentas  .setText(String.format("$%,.0f", totalVentas));

        // Gráfico (modo vendedores por defecto)
        graficoPanel.setModo(GraficoBarrasPanel.MODO_VENDEDORES, vendedoresOrdenados, productosOrdenados);
        graficoPanel.repaint();
        cardLayout.show(cardCenter, vendedoresOrdenados.isEmpty() ? "placeholder" : "chart");

        // ── Tabla vendedores ──
        // Para % de rendimiento: proporción del total acumulado
        double totalV = vendedoresOrdenados.stream().mapToDouble(Vendedor::getVentasTotales).sum();
        if (totalV == 0) totalV = 1;

        // Calcular total de unidades por vendedor leyendo sus archivos
        Map<String, Integer> unidadesPorVendedor = calcularUnidadesPorVendedor();

        rendererVendedores.setMaxValue(100.0); // siempre sobre 100 porque es porcentaje
        modeloVendedores.setRowCount(0);
        for (int i = 0; i < vendedoresOrdenados.size(); i++) {
            Vendedor v   = vendedoresOrdenados.get(i);
            int unidades = unidadesPorVendedor.getOrDefault(v.getNumDoc(), 0);
            double pct   = v.getVentasTotales() / totalV * 100.0;
            modeloVendedores.addRow(new Object[]{
                i + 1,
                v.getNombres() + " " + v.getApellidos(),
                String.format("%,.2f", v.getVentasTotales()),
                unidades,
                pct   // porcentaje del total → barra siempre relativa al 100%
            });
        }

        // ── Tabla productos ──
        int totalUnidades = productosOrdenados.stream().mapToInt(Producto::getCantidadVendida).sum();
        if (totalUnidades == 0) totalUnidades = 1;

        rendererProductos.setMaxValue(100.0);
        modeloProductos.setRowCount(0);
        for (Producto p : productosOrdenados) {
            double totalProd = p.getCantidadVendida() * p.getPrecio();
            double pct       = (double) p.getCantidadVendida() / totalUnidades * 100.0;
            modeloProductos.addRow(new Object[]{
                p.getId(),
                p.getNombre(),
                String.format("%,.2f", p.getPrecio()),
                p.getCantidadVendida(),
                String.format("%,.2f", totalProd),
                pct   // porcentaje del total unidades → suma 100%
            });
        }

        // ── Combos comparación vendedores ──
        String s1 = (String) comboV1.getSelectedItem();
        String s2 = (String) comboV2.getSelectedItem();
        comboV1.removeAllItems(); comboV2.removeAllItems();
        for (Vendedor v : vendedoresOrdenados) {
            String n = v.getNombres() + " " + v.getApellidos();
            comboV1.addItem(n); comboV2.addItem(n);
        }
        if (s1 != null) comboV1.setSelectedItem(s1);
        if (s2 != null) comboV2.setSelectedItem(s2);

        // ── Combos comparación productos ──
        String sp1 = (String) comboProd1.getSelectedItem();
        String sp2 = (String) comboProd2.getSelectedItem();
        comboProd1.removeAllItems(); comboProd2.removeAllItems();
        for (Producto p : productosOrdenados) {
            comboProd1.addItem(p.getNombre());
            comboProd2.addItem(p.getNombre());
        }
        if (sp1 != null) comboProd1.setSelectedItem(sp1);
        if (sp2 != null) comboProd2.setSelectedItem(sp2);

        // Filtro vendedor en comparación productos
        String sfv = (String) comboFiltroVendedorProd.getSelectedItem();
        comboFiltroVendedorProd.removeAllItems();
        comboFiltroVendedorProd.addItem("(Todos los vendedores)");
        for (Vendedor v : vendedoresOrdenados)
            comboFiltroVendedorProd.addItem(v.getNombres() + " " + v.getApellidos());
        if (sfv != null) comboFiltroVendedorProd.setSelectedItem(sfv);

        actualizarComparacionVendedores();
        actualizarComparacionProductos();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Comparación vendedores
    // ─────────────────────────────────────────────────────────────────────

    private void actualizarComparacionVendedores() {
        actualizarTarjetaVendedor((String) comboV1.getSelectedItem(),
                lblV1Nombre, lblV1Doc, lblV1Ventas, lblV1Rank);
        actualizarTarjetaVendedor((String) comboV2.getSelectedItem(),
                lblV2Nombre, lblV2Doc, lblV2Ventas, lblV2Rank);
    }

    private void actualizarTarjetaVendedor(String nombre, JLabel lN, JLabel lD, JLabel lV, JLabel lR) {
        if (nombre == null || vendedoresOrdenados.isEmpty()) {
            lN.setText("–"); lD.setText("Seleccione un vendedor"); lV.setText(""); lR.setText(""); return;
        }
        Vendedor found = null; int rank = 0;
        for (int i = 0; i < vendedoresOrdenados.size(); i++) {
            Vendedor v = vendedoresOrdenados.get(i);
            if ((v.getNombres() + " " + v.getApellidos()).equals(nombre)) {
                found = v; rank = i + 1; break;
            }
        }
        if (found == null) { lN.setText("–"); lD.setText("No encontrado"); lV.setText(""); lR.setText(""); return; }

        double totalGlobal = vendedoresOrdenados.stream().mapToDouble(Vendedor::getVentasTotales).sum();
        double pct = totalGlobal > 0 ? found.getVentasTotales() / totalGlobal * 100 : 0;

        lN.setText(found.getNombres() + " " + found.getApellidos());
        lD.setText(found.getTipoDoc() + " · " + found.getNumDoc());
        lV.setText(String.format("Ventas: $%,.2f  (%.1f%% del total)", found.getVentasTotales(), pct));
        lR.setText("Posición #" + rank + " de " + vendedoresOrdenados.size());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Comparación productos
    // ─────────────────────────────────────────────────────────────────────

    private void actualizarComparacionProductos() {
        String filtroVendedor = (String) comboFiltroVendedorProd.getSelectedItem();
        boolean hayFiltro = filtroVendedor != null && !filtroVendedor.equals("(Todos los vendedores)");

        actualizarTarjetaProducto((String) comboProd1.getSelectedItem(),
                lblP1Nombre, lblP1Precio, lblP1Unidades, lblP1Total, filtroVendedor, hayFiltro);
        actualizarTarjetaProducto((String) comboProd2.getSelectedItem(),
                lblP2Nombre, lblP2Precio, lblP2Unidades, lblP2Total, filtroVendedor, hayFiltro);
    }

    private void actualizarTarjetaProducto(String nombre,
            JLabel lN, JLabel lPrecio, JLabel lUnidades, JLabel lTotal,
            String filtroVendedor, boolean hayFiltro) {

        if (nombre == null || productosOrdenados.isEmpty()) {
            lN.setText("–"); lPrecio.setText("Seleccione un producto"); lUnidades.setText(""); lTotal.setText(""); return;
        }
        Producto found = null;
        for (Producto p : productosOrdenados) {
            if (p.getNombre().equals(nombre)) { found = p; break; }
        }
        if (found == null) { lN.setText("–"); lPrecio.setText("No encontrado"); lUnidades.setText(""); lTotal.setText(""); return; }

        int    unidades = found.getCantidadVendida();
        double total    = found.getPrecio() * unidades;

        // Si hay filtro de vendedor, recalcular solo sus ventas de este producto
        if (hayFiltro) {
            // Buscar numDoc del vendedor seleccionado
            String vendedorId = null;
            for (Vendedor v : vendedoresOrdenados) {
                if ((v.getNombres() + " " + v.getApellidos()).equals(filtroVendedor)) {
                    vendedorId = v.getNumDoc(); break;
                }
            }
            if (vendedorId != null) {
                Path archivoV = Paths.get(GenerateInfoFiles.CARPETA + "vendedor_" + vendedorId + ".csv");
                try {
                    int uni = 0;
                    List<String> lineas = Files.readAllLines(archivoV);
                    for (int i = 2; i < lineas.size(); i++) {
                        String[] d = lineas.get(i).split(GenerateInfoFiles.SEP);
                        if (d.length >= 3 && d[1].trim().equals(nombre)) {
                            uni += Integer.parseInt(d[2].trim());
                        }
                    }
                    unidades = uni;
                    total    = found.getPrecio() * unidades;
                } catch (Exception ignored) {}
            }
        }

        String contexto = hayFiltro ? "  [" + filtroVendedor + "]" : "  [todos los vendedores]";
        lN      .setText(found.getNombre());
        lPrecio .setText(String.format("Precio unitario: $%,.2f", found.getPrecio()));
        lUnidades.setText(String.format("Unidades vendidas: %d%s", unidades, contexto));
        lTotal  .setText(String.format("Total recaudado: $%,.2f", total));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Calcular unidades totales por vendedor (leyendo sus archivos)
    // ─────────────────────────────────────────────────────────────────────

    private Map<String, Integer> calcularUnidadesPorVendedor() {
        Map<String, Integer> map = new HashMap<>();
        String carpeta = GenerateInfoFiles.CARPETA;
        String sep     = GenerateInfoFiles.SEP;
        for (Vendedor v : vendedoresOrdenados) {
            Path archivo = Paths.get(carpeta + "vendedor_" + v.getNumDoc() + ".csv");
            int total = 0;
            try {
                List<String> lineas = Files.readAllLines(archivo);
                for (int i = 2; i < lineas.size(); i++) {
                    String[] d = lineas.get(i).split(sep);
                    if (d.length >= 3) total += Integer.parseInt(d[2].trim());
                }
            } catch (Exception ignored) {}
            map.put(v.getNumDoc(), total);
        }
        return map;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Acciones
    // ─────────────────────────────────────────────────────────────────────

    private void accionGenerarDatos() {
        setButtonsEnabled(false);
        lblEstado.setText("Generando datos...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                Files.createDirectories(Paths.get(GenerateInfoFiles.CARPETA));
                GenerateInfoFiles.createProductsFile(4);
                GenerateInfoFiles.createSalesManInfoFile(5);
                return null;
            }
            @Override protected void done() {
                try { get(); lblEstado.setText("Datos generados. Recargando..."); loadResultados(); }
                catch (InterruptedException | ExecutionException ex) {
                    lblEstado.setText("Error: " + ex.getCause().getMessage()); setButtonsEnabled(true); }
            }
        }.execute();
    }

    private void accionProcesarDatos() {
        if (mapaVendedores.isEmpty() || mapaProductos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Sin datos. Genera datos primero.",
                    "Sin datos", JOptionPane.WARNING_MESSAGE); return;
        }
        setButtonsEnabled(false);
        lblEstado.setText("Generando reportes...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { Main.main(null); return null; }
            @Override protected void done() {
                try { get();
                    JOptionPane.showMessageDialog(VentanaSistema.this,
                        "Reportes guardados en Resultados/", "Listo", JOptionPane.INFORMATION_MESSAGE);
                    lblEstado.setText("Reportes en Resultados/");
                } catch (InterruptedException | ExecutionException ex) {
                    lblEstado.setText("Error: " + ex.getCause().getMessage()); }
                setButtonsEnabled(true);
            }
        }.execute();
    }

    private void accionEliminarArchivos() {
        int r1 = JOptionPane.showConfirmDialog(this,
            "Se eliminarán TODOS los archivos CSV de Resultados/. ¿Continuar?",
            "Confirmación 1 de 2", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r1 != JOptionPane.YES_OPTION) return;
        int r2 = JOptionPane.showConfirmDialog(this,
            "CONFIRMACIÓN FINAL — Esta acción no se puede deshacer.\n¿Está seguro?",
            "Confirmación 2 de 2", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (r2 != JOptionPane.YES_OPTION) return;

        setButtonsEnabled(false);
        lblEstado.setText("Eliminando...");
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                int[] c = {0};
                Path carpeta = Paths.get(GenerateInfoFiles.CARPETA);
                if (!Files.exists(carpeta)) return 0;
                Files.walk(carpeta).filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".csv"))
                        .forEach(p -> { try { Files.delete(p); c[0]++; } catch (IOException ignored) {} });
                return c[0];
            }
            @Override protected void done() {
                try { int n = get();
                    lblEstado.setText(n + " archivo(s) eliminado(s).");
                    mapaVendedores = new HashMap<>(); mapaProductos = new HashMap<>();
                    vendedoresOrdenados = new ArrayList<>(); productosOrdenados = new ArrayList<>();
                    refreshUI(); cardLayout.show(cardCenter, "placeholder");
                } catch (InterruptedException | ExecutionException ex) {
                    lblEstado.setText("Error: " + ex.getCause().getMessage()); }
                setButtonsEnabled(true);
            }
        }.execute();
    }

    private void setButtonsEnabled(boolean e) {
        btnGenerar.setEnabled(e); btnProcesar.setEnabled(e); btnEliminar.setEnabled(e);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Clase interna: Gráfico de barras (vendedores O productos)
    // ═════════════════════════════════════════════════════════════════════

    static class GraficoBarrasPanel extends JPanel {
        static final int MODO_VENDEDORES = 0;
        static final int MODO_PRODUCTOS  = 1;

        private int            modo   = MODO_VENDEDORES;
        private List<Vendedor> vends  = new ArrayList<>();
        private List<Producto> prods  = new ArrayList<>();

        public void setModo(int modo, List<Vendedor> vends, List<Producto> prods) {
            this.modo  = modo;
            this.vends = vends;
            this.prods = prods;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(BG_CHART);
            g2.fillRect(0, 0, w, h);

            List<String> etiquetas = new ArrayList<>();
            List<Double> valores   = new ArrayList<>();

            if (modo == MODO_VENDEDORES) {
                double total = vends.stream().mapToDouble(Vendedor::getVentasTotales).sum();
                if (total == 0) total = 1;
                for (Vendedor v : vends) {
                    etiquetas.add(v.getNombres());
                    valores.add(v.getVentasTotales() / total * 100.0); // % del total
                }
            } else {
                int total = prods.stream().mapToInt(Producto::getCantidadVendida).sum();
                if (total == 0) total = 1;
                for (Producto p : prods) {
                    etiquetas.add(p.getNombre());
                    valores.add((double) p.getCantidadVendida() / total * 100.0); // % del total
                }
            }

            if (valores.isEmpty()) {
                g2.setColor(TEXT_GRAY);
                g2.setFont(new Font("SansSerif", Font.ITALIC, 13));
                String msg = "Sin datos";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                return;
            }

            int padding  = 30;
            int labelH   = 40;
            int chartH   = h - padding - labelH - 20;
            int chartW   = w - 2 * padding;
            int n        = valores.size();
            int barGap   = 12;
            int barWidth = Math.max(20, (chartW - (n + 1) * barGap) / n);

            // Líneas de guía a 25%, 50%, 75%, 100%
            for (int pct = 25; pct <= 100; pct += 25) {
                int y = padding + chartH - (int) (chartH * pct / 100.0);
                g2.setColor(new Color(230, 230, 235));
                g2.drawLine(padding, y, w - padding, y);
                g2.setColor(TEXT_GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g2.drawString(pct + "%", 2, y + 4);
            }

            for (int i = 0; i < n; i++) {
                double val   = valores.get(i);
                int    barH  = (int) (chartH * val / 100.0);
                int    x     = padding + barGap + i * (barWidth + barGap);
                int    y     = padding + chartH - barH;
                Color  color = BAR_COLORS[i % BAR_COLORS.length];

                g2.setColor(color);
                g2.fillRoundRect(x, y, barWidth, barH, 6, 6);

                // Valor encima de la barra
                g2.setColor(TEXT_DARK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                String label = String.format("%.1f%%", val);
                FontMetrics fm = g2.getFontMetrics();
                int lx = x + (barWidth - fm.stringWidth(label)) / 2;
                if (lx < 0) lx = x;
                g2.drawString(label, lx, Math.max(y - 4, 14));

                // Nombre debajo del eje
                g2.setColor(TEXT_GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                String nom = etiquetas.get(i);
                FontMetrics fm2 = g2.getFontMetrics();
                int nx = x + (barWidth - fm2.stringWidth(nom)) / 2;
                if (nx < 0) nx = x;
                g2.drawString(nom, nx, padding + chartH + 14);
            }

            g2.setColor(BORDER_COLOR);
            g2.drawLine(padding, padding + chartH, w - padding, padding + chartH);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Clase interna: renderer de barra porcentual en tablas
    // ═════════════════════════════════════════════════════════════════════

    static class BarraRenderer extends JPanel implements TableCellRenderer {
        private double maxValue = 100.0; // siempre 100 porque valores son porcentajes
        private double value    = 0.0;
        private final Color barColor;

        BarraRenderer(Color barColor) { this.barColor = barColor; setOpaque(true); }

        public void setMaxValue(double v) { this.maxValue = v <= 0 ? 100 : v; }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object val,
                boolean selected, boolean focused, int row, int col) {
            this.value = val instanceof Number ? ((Number) val).doubleValue() : 0.0;
            setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int bw = (int) (w * value / maxValue);
            int pad = 3;
            g2.setColor(new Color(230, 232, 236));
            g2.fillRoundRect(pad, pad, w - 2 * pad, h - 2 * pad, 4, 4);
            if (bw > 0) {
                g2.setColor(barColor);
                g2.fillRoundRect(pad, pad, Math.max(4, bw - 2 * pad), h - 2 * pad, 4, 4);
            }
            String label = String.format("%.1f%%", value);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(TEXT_DARK);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, (w - fm.stringWidth(label)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
        }
    }
}