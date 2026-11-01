package main.java.jonathan.blocnotas.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import main.java.jonathan.blocnotas.controller.EditorController;
import main.java.jonathan.blocnotas.model.Nota;

/** Cambia entre la pantalla de inicio y el editor dentro de la misma ventana. */
public final class Navegador {

    private static final String RUTA_VISTAS = "/main/resources/view/";

    private static Stage stage;
    private static Object controladorActual;

    private Navegador() {}

    public static Stage getStage() { return stage; }

    public static void iniciar(Stage s) {
        stage = s;
        Scene scene = new Scene(new StackPane(), 960, 640);
        Tema.aplicar(scene, Tema.cargar());
        stage.setScene(scene);
    }

    public static void irAInicio() {
        cargar("HomeView.fxml");
    }

    public static void irAEditor(Nota nota) {
        EditorController c = cargar("EditorView.fxml");
        c.setNota(nota);
    }

    public static void guardarAlSalir() {
        if (controladorActual instanceof EditorController e) e.guardar();
    }

    private static <T> T cargar(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(recurso(RUTA_VISTAS + fxml));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            controladorActual = loader.getController();
            return loader.getController();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static URL recurso(String ruta) {
        URL url = Navegador.class.getResource(ruta);
        if (url == null) throw new IllegalStateException("No se encontró el recurso: " + ruta);
        return url;
    }
}