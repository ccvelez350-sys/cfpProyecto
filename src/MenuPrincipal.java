import java.util.Scanner;
import java.nio.file.*;
import java.io.IOException;
import javax.swing.SwingUtilities;

public class MenuPrincipal {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int opcion = -1;

        do {
            System.out.println("\n==============================");
            System.out.println("      MENÚ PRINCIPAL");
            System.out.println("==============================");
            System.out.println("1. Generar archivos de información");
            System.out.println("2. Procesar información y generar reportes");
            System.out.println("3. Eliminar todos los archivos CSV");
            System.out.println("4. Abrir ventana gráfica (Dashboard)");
            System.out.println("0. Salir");
            System.out.print("Seleccione una opción: ");

            try {
                opcion = Integer.parseInt(scanner.nextLine());

                switch (opcion) {
                    case 1:
                        System.out.println("\nGenerando archivos...");
                        GenerateInfoFiles.main(null);
                        break;

                    case 2:
                        System.out.println("\nProcesando información...");
                        Main.main(null);
                        break;

                    case 3:
                        System.out.println("\nEliminando todos los archivos CSV de la carpeta Resultados...");
                        eliminarTodosCSV();
                        break;

                    case 4:
                        System.out.println("\nAbriendo ventana gráfica...");
                        SwingUtilities.invokeLater(() -> {
                            try {
                                javax.swing.UIManager.setLookAndFeel(
                                    javax.swing.UIManager.getSystemLookAndFeelClassName());
                            } catch (Exception ignored) {}
                            new VentanaSistema().setVisible(true);
                        });
                        break;

                    case 0:
                        System.out.println("\nSaliendo del programa...");
                        break;

                    default:
                        System.out.println("\nOpción inválida. Intente nuevamente.");
                }

            } catch (Exception e) {
                System.out.println("Error: Ingrese una opción válida.");
            }

        } while (opcion != 0);

        scanner.close();
    }

    private static void eliminarTodosCSV() {
        Path carpetaResultados = Paths.get(GenerateInfoFiles.CARPETA);

        if (!Files.exists(carpetaResultados)) {
            System.out.println("La carpeta Resultados/ está vacía o no existe.");
            return;
        }

        try {
            Files.walk(carpetaResultados)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".csv"))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("Archivo eliminado: " + path.getFileName());
                    } catch (IOException e) {
                        System.err.println("No se pudo eliminar: " + path.getFileName());
                    }
                });

        } catch (IOException e) {
            System.err.println("Error al eliminar archivos CSV.");
        }
    }
}