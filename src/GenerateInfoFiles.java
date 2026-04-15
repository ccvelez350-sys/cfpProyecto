import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;
import java.util.Random;

public class GenerateInfoFiles {

    private static final String[] NOMBRES   = {"Carlos", "Ana", "Luis", "Maria", "Juan", "Sofia", "Diego", "Laura"};
    private static final String[] APELLIDOS = {"Gomez", "Perez", "Rodriguez", "Martinez", "Torres", "Vargas"};
    private static final String[] TIPOS_DOC = {"CC", "CE", "TI"};

    public static final String[] PRODUCTOS_NOMBRES = {"Laptop", "Mouse", "Teclado", "Monitor"};
    public static final double[] PRODUCTOS_PRECIOS = {2500000.50, 80000.00, 150000.99, 950000.00};

    public static final long   ID_BASE_VENDEDOR = 10001L;
    public static final String FECHA   = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    public static final String CARPETA = "Resultados/";
    public static final String SEP     = ",";

    private static final Random RAND = new Random();

    public static void main(String[] args) {
        try {
            System.out.println("Iniciando generación de archivos...");
            Files.createDirectories(Paths.get(CARPETA));
            createProductsFile(PRODUCTOS_NOMBRES.length);
            createSalesManInfoFile(5);
            System.out.println("Archivos generados en " + CARPETA);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    public static void createProductsFile(int count) throws Exception {
        try (PrintWriter w = new PrintWriter(CARPETA + "productos_" + FECHA + ".csv", "UTF-8")) {
            w.println("ID" + SEP + "Nombre" + SEP + "Precio");
            for (int i = 0; i < count; i++)
                w.println((i + 1) + SEP + PRODUCTOS_NOMBRES[i] + SEP + PRODUCTOS_PRECIOS[i]);
        }
    }

    public static void createSalesManInfoFile(int count) throws Exception {
        String[] noms = shuffle(NOMBRES.clone());
        String[] apes = shuffle(APELLIDOS.clone());

        try (PrintWriter w = new PrintWriter(CARPETA + "vendedores_" + FECHA + ".csv", "UTF-8")) {
            w.println("TipoDoc" + SEP + "NumDoc" + SEP + "Nombres" + SEP + "Apellidos");
            for (int i = 0; i < count; i++) {
                long   id   = ID_BASE_VENDEDOR + i;
                String tipo = TIPOS_DOC[RAND.nextInt(TIPOS_DOC.length)];
                String nom  = noms[i % noms.length];
                String ape  = apes[i % apes.length];
                w.println(tipo + SEP + id + SEP + nom + SEP + ape);
                createSalesMenFile(2 + RAND.nextInt(5), id);
            }
        }
    }

    public static void createSalesMenFile(int salesCount, long id) throws Exception {
        try (PrintWriter w = new PrintWriter(CARPETA + "vendedor_" + id + ".csv", "UTF-8")) {
            w.println("Fecha del reporte" + SEP + LocalDate.now());
            w.println("CC" + SEP + id);
            for (int i = 0; i < salesCount; i++) {
                int prodIdx  = RAND.nextInt(PRODUCTOS_NOMBRES.length);
                int cantidad = 1 + RAND.nextInt(10);
                // formato: idProducto,nombreProducto,cantidad
                w.println((prodIdx + 1) + SEP + PRODUCTOS_NOMBRES[prodIdx] + SEP + cantidad);
            }
        }
    }

    private static String[] shuffle(String[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = RAND.nextInt(i + 1);
            String t = arr[i]; arr[i] = arr[j]; arr[j] = t;
        }
        return arr;
    }
}