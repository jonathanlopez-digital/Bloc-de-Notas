package main.java.jonathan.blocnotas.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;

public enum Tema {
    CLARO("claro"), OSCURO("oscuro"), BEIGE("beige");

    private static final String BASE = "/main/resources/css/estilos.css";
    private static final Path CONFIG = Path.of(System.getProperty("user.home"), ".blocnotas", "config.properties");
    private static Tema actual = CLARO;

    private final String id;

    Tema(String id) { this.id = id; }

    public static Tema actual() { return actual; }

    // ---------- Preferencia guardada ----------
    public static Tema cargar() {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG)) {
            p.load(in);
        } catch (IOException e) {
            return actual;
        }
        String v = p.getProperty("tema", "claro");
        for (Tema t : values()) if (t.id.equals(v)) actual = t;
        return actual;
    }

    private static void guardar() {
        Properties p = new Properties();
        p.setProperty("tema", actual.id);
        try {
            Files.createDirectories(CONFIG.getParent());
            try (OutputStream o = Files.newOutputStream(CONFIG)) {
                p.store(o, "Bloc de notas");
            }
        } catch (IOException ignorado) {
        }
    }

    // ---------- Aplicar ----------
    public static void aplicar(Scene scene, Tema tema) {
        actual = tema;
        scene.getStylesheets().setAll(recurso(tema.archivo()), recurso(BASE));
        guardar();
    }

    public static void estilizar(Dialog<?> dialogo) {
        dialogo.getDialogPane().getStylesheets().setAll(recurso(actual.archivo()), recurso(BASE));
    }

    private String archivo() { return "/main/resources/css/tema-" + id + ".css"; }

    private static String recurso(String ruta) {
        return Objects.requireNonNull(Tema.class.getResource(ruta), "Falta el recurso " + ruta).toExternalForm();
    }
}