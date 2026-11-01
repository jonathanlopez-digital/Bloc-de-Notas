package main.java.jonathan.blocnotas.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import main.java.jonathan.blocnotas.model.Marca;
import main.java.jonathan.blocnotas.model.Nota;
import main.java.jonathan.blocnotas.service.NotaRepositorio;
import main.java.jonathan.blocnotas.util.Dialogos;
import main.java.jonathan.blocnotas.util.MarcaVista;
import main.java.jonathan.blocnotas.util.Navegador;
import main.java.jonathan.blocnotas.util.Tema;

public class HomeController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private FlowPane contenedorNotas;
    @FXML private ScrollPane scroll;
    @FXML private VBox vacio;
    @FXML private ToggleButton btnClaro, btnOscuro, btnBeige;

    private final NotaRepositorio repo = new NotaRepositorio();

    @FXML
    private void initialize() {
        marcarTema();
        cargarNotas();
    }

    // ---------- Notas ----------
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

        int adjuntos = n.contarImagenes() + n.getArchivos().size();
        Label fecha = new Label(FECHA.format(n.getModificada()) + (adjuntos > 0 ? "  •  " + adjuntos + " adj." : ""));
        fecha.getStyleClass().add("ficha-fecha");

        VBox ficha = new VBox(6, titulo);
        ficha.getStyleClass().add("ficha");
        if (!n.getMarca().esNinguna()) {
            Label chip = new Label(n.getMarca().getNombre(), MarcaVista.punto(n.getMarca()));
            chip.getStyleClass().add("chip-marca");
            ficha.getChildren().add(chip);
            ficha.setStyle(String.format("-fx-border-color: -c-borde -c-borde -c-borde %s; -fx-border-width: 1 1 1 5;",
                    n.getMarca().getColor()));
        }
        ficha.getChildren().addAll(vista, fecha);
        ficha.setPrefSize(220, 180);
        ficha.setMinSize(220, 180);
        ficha.setMaxSize(220, 180);

        ContextMenu menu = crearMenu(n);
        ficha.setOnContextMenuRequested(e -> {
            menu.show(ficha, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        ficha.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) Navegador.irAEditor(n);
        });
        return ficha;
    }

    private ContextMenu crearMenu(Nota n) {
        MenuItem abrir = new MenuItem("Abrir");
        abrir.setOnAction(e -> Navegador.irAEditor(n));

        Menu marcas = new Menu("Seleccionar marca");
        for (Marca m : Marca.values()) {
            MenuItem item = new MenuItem((n.getMarca() == m ? "✓ " : "") + m.getNombre());
            item.setGraphic(MarcaVista.punto(m));
            item.setOnAction(e -> cambiarMarca(n, m));
            marcas.getItems().add(item);
        }

        MenuItem eliminar = new MenuItem("Eliminar");
        eliminar.setOnAction(e -> confirmarEliminar(n));
        return new ContextMenu(abrir, marcas, new SeparatorMenuItem(), eliminar);
    }

    private void cambiarMarca(Nota n, Marca m) {
        try {
            n.setMarca(m);
            repo.guardar(n);
            cargarNotas();
        } catch (RuntimeException ex) {
            Dialogos.error("No se pudo guardar la marca.");
        }
    }

    private void confirmarEliminar(Nota n) {
        if (Dialogos.confirmar(n.getTituloMostrado(), "¿Eliminar esta nota y sus adjuntos?", "Eliminar")) {
            repo.eliminar(n);
            cargarNotas();
        }
    }

    @FXML
    private void onNuevaNota() {
        Navegador.irAEditor(new Nota());
    }

    // ---------- Tema ----------
    @FXML private void onTemaClaro() { cambiarTema(Tema.CLARO); }
    @FXML private void onTemaOscuro() { cambiarTema(Tema.OSCURO); }
    @FXML private void onTemaBeige() { cambiarTema(Tema.BEIGE); }

    private void cambiarTema(Tema t) {
        Tema.aplicar(Navegador.getStage().getScene(), t);
        marcarTema();
    }

    private void marcarTema() {
        btnClaro.setSelected(Tema.actual() == Tema.CLARO);
        btnOscuro.setSelected(Tema.actual() == Tema.OSCURO);
        btnBeige.setSelected(Tema.actual() == Tema.BEIGE);
    }
}