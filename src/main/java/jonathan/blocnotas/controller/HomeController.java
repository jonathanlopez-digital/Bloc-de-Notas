package main.java.jonathan.blocnotas.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import main.java.jonathan.blocnotas.model.Nota;
import main.java.jonathan.blocnotas.service.NotaRepositorio;
import main.java.jonathan.blocnotas.util.Navegador;

public class HomeController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private FlowPane contenedorNotas;
    @FXML private ScrollPane scroll;
    @FXML private VBox vacio;

    private final NotaRepositorio repo = new NotaRepositorio();

    @FXML
    private void initialize() {
        cargarNotas();
    }

    private void cargarNotas() {
        contenedorNotas.getChildren().clear();
        List<Nota> notas = repo.listar();
        boolean hay = !notas.isEmpty();

        vacio.setVisible(!hay);
        scroll.setVisible(hay);
        for (Nota n : notas) contenedorNotas.getChildren().add(crearFicha(n));
    }

    private VBox crearFicha(Nota n) {
        Label titulo = new Label(n.getTituloMostrado());
        titulo.getStyleClass().add("ficha-titulo");
        titulo.setWrapText(true);

        String resumen = n.getContenido().strip();
        if (resumen.length() > 140) resumen = resumen.substring(0, 140) + "…";
        Label vista = new Label(resumen);
        vista.getStyleClass().add("ficha-texto");
        vista.setWrapText(true);
        VBox.setVgrow(vista, Priority.ALWAYS);

        Label fecha = new Label(FECHA.format(n.getModificada()));
        fecha.getStyleClass().add("ficha-fecha");

        VBox ficha = new VBox(8, titulo, vista, fecha);
        ficha.getStyleClass().add("ficha");
        ficha.setPrefSize(210, 170);
        ficha.setMinSize(210, 170);
        ficha.setMaxSize(210, 170);

        MenuItem abrir = new MenuItem("Abrir");
        abrir.setOnAction(e -> Navegador.irAEditor(n));
        MenuItem eliminar = new MenuItem("Eliminar");
        eliminar.setOnAction(e -> confirmarEliminar(n));
        ficha.setOnContextMenuRequested(e ->
                new ContextMenu(abrir, eliminar).show(ficha, e.getScreenX(), e.getScreenY()));

        ficha.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) Navegador.irAEditor(n);
        });
        return ficha;
    }

    private void confirmarEliminar(Nota n) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar esta nota?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(n.getTituloMostrado());
        a.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            repo.eliminar(n);
            cargarNotas();
        });
    }

    @FXML
    private void onNuevaNota() {
        Navegador.irAEditor(new Nota());
    }
}