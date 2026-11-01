package main.java.jonathan.blocnotas.util;

import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;

public final class Dialogos {

    private Dialogos() {}

    public static void error(String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR, mensaje);
        preparar(a);
        a.showAndWait();
    }

    public static void info(String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, mensaje);
        a.setHeaderText(null);
        preparar(a);
        a.showAndWait();
    }

    public static boolean confirmar(String titulo, String mensaje, String textoSi) {
        ButtonType si = new ButtonType(textoSi, ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, si, no);
        a.setHeaderText(titulo);
        preparar(a);
        return a.showAndWait().filter(b -> b == si).isPresent();
    }

    public static Optional<String> elegir(String titulo, String encabezado, String etiqueta, List<String> opciones) {
        ChoiceDialog<String> d = new ChoiceDialog<>(opciones.get(0), opciones);
        d.setTitle(titulo);
        d.setHeaderText(encabezado);
        d.setContentText(etiqueta);
        preparar(d);
        return d.showAndWait();
    }

    private static void preparar(Dialog<?> d) {
        if (Navegador.getStage() != null) d.initOwner(Navegador.getStage());
        Tema.estilizar(d);
    }
}