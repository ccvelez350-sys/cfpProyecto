import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;

public class Main {

    public static void main(String[] args) {
        try {
            System.out.println("Iniciando procesamiento...");
            String fecha   = GenerateInfoFiles.FECHA;
            String carpeta = GenerateInfoFiles.CARPETA;
            String sep     = GenerateInfoFiles.SEP;

            Files.createDirectories(Paths.get(carpeta));

            Map<String, Producto> mapaProductos = cargarDatos(
                    carpeta + "productos_" + fecha + ".csv",
                    l -> { String[] d = l.split(sep); return new Producto(d[0], d[1], Double.parseDouble(d[2])); },
                    Producto::getId);

            Map<String, Vendedor> mapaVendedores = cargarDatos(
                    carpeta + "vendedores_" + fecha + ".csv",
                    l -> { String[] d = l.split(sep); return new Vendedor(d[0], d[1], d[2], d[3]); },
                    Vendedor::getNumDoc);

            Files.walk(Paths.get(carpeta))
                    .filter(p -> p.getFileName().toString().startsWith("vendedor_") && p.toString().endsWith(".csv"))
                    .forEach(p -> procesarArchivoVenta(p, mapaProductos, mapaVendedores, sep));

            generarReportes(mapaVendedores, mapaProductos, fecha, carpeta, sep);
            System.out.println("Reportes generados en " + carpeta);

        } catch (NoSuchFileException e) {
            System.err.println("ERROR: Archivos CSV no encontrados. Ejecute la opción 1 primero.");
        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static <T> Map<String, T> cargarDatos(String archivo,
            java.util.function.Function<String, T> ctor,
            java.util.function.Function<T, String> key) throws IOException {
        return Files.lines(Paths.get(archivo)).skip(1)
                .map(ctor).collect(Collectors.toMap(key, x -> x));
    }

    static void procesarArchivoVenta(Path archivo, Map<String, Producto> prods,
                                      Map<String, Vendedor> vends, String sep) {
        try {
            List<String> lineas = Files.readAllLines(archivo);
            if (lineas.size() < 2) return;
            String idVendedor = lineas.get(1).split(sep)[1].trim();
            Vendedor vendedor = vends.get(idVendedor);
            if (vendedor == null) return;

            for (int i = 2; i < lineas.size(); i++) {
                String[] d = lineas.get(i).split(sep);
                if (d.length < 3) continue;
                // columnas: idProducto, nombreProducto, cantidad
                Producto prod = prods.get(d[0].trim());
                int cantidad  = Integer.parseInt(d[2].trim());
                if (prod != null) {
                    vendedor.setVentasTotales(vendedor.getVentasTotales() + prod.getPrecio() * cantidad);
                    prod.setCantidadVendida(prod.getCantidadVendida() + cantidad);
                }
            }
        } catch (Exception e) {
            System.err.println("ADVERTENCIA: " + archivo.getFileName() + " → " + e.getMessage());
        }
    }

    private static void generarReportes(Map<String, Vendedor> mv, Map<String, Producto> mp,
                                         String fecha, String carpeta, String sep) throws IOException {
        List<Vendedor> vOrden = mv.values().stream()
                .sorted(Comparator.comparingDouble(v -> -v.getVentasTotales()))
                .collect(Collectors.toList());
        List<Producto> pOrden = mp.values().stream()
                .sorted(Comparator.comparingInt(p -> -p.getCantidadVendida()))
                .collect(Collectors.toList());

        Vendedor estrella = vOrden.isEmpty() ? null : vOrden.get(0);

        try (PrintWriter w = new PrintWriter(carpeta + "reporte_vendedores_" + fecha + ".csv")) {
            w.println("Fecha del reporte" + sep + LocalDate.now());
            w.println("Vendedor" + sep + "Total Ventas ($)" + sep + "Unidades Vendidas");
            for (Vendedor v : vOrden) {
                int totalUnidades = mp.values().stream()
                        .mapToInt(p -> 0).sum(); // placeholder — ventas por vendedor no se acumulan por unidades aquí
                w.printf("%s %s%s%.2f%n", v.getNombres(), v.getApellidos(), sep, v.getVentasTotales());
            }
            if (estrella != null) {
                w.println();
                w.println("VENDEDOR ESTRELLA");
                w.printf("%s %s%s%.2f%n", estrella.getNombres(), estrella.getApellidos(), sep, estrella.getVentasTotales());
            }
        }

        try (PrintWriter w = new PrintWriter(carpeta + "reporte_productos_" + fecha + ".csv")) {
            w.println("Fecha del reporte" + sep + LocalDate.now());
            w.println("Producto" + sep + "Unidades Vendidas" + sep + "Precio Unitario ($)" + sep + "Total Recaudado ($)");
            for (Producto p : pOrden)
                w.printf("%s%s%d%s%.2f%s%.2f%n",
                        p.getNombre(), sep, p.getCantidadVendida(), sep,
                        p.getPrecio(), sep, p.getCantidadVendida() * p.getPrecio());
        }
    }
}