package main.java.jonathan.blocnotas.controller;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.util.Duration;
import main.java.jonathan.blocnotas.model.Nota;
import main.java.jonathan.blocnotas.service.ExportService;
import main.java.jonathan.blocnotas.service.ImpresionService;
import main.java.jonathan.blocnotas.service.NotaRepositorio;
import main.java.jonathan.blocnotas.util.Navegador;

public class EditorController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private TextField txtTitulo;
    @FXML private TextArea txtContenido;
    @FXML private Label lblGuardado;
    @FXML private Label lblConteo;
    @FXML private CheckMenuItem mnuAjuste;

    private final NotaRepositorio repo = new NotaRepositorio();
    private final PauseTransition autoguardado = new PauseTransition(Duration.seconds(1));
    private Nota nota;
    private boolean cargando;
    private double tamanoFuente = 14;

    @FXML
    private void initialize() {
        autoguardado.setOnFinished(e -> guardar());
        ChangeListener<String> cambio = (obs, a, b) -> {
            if (!cargando) {
                autoguardado.playFromStart(); // guarda 1 s después de dejar de escribir
                actualizarConteo();
            }
        };
        txtTitulo.textProperty().addListener(cambio);
        txtContenido.textProperty().addListener(cambio);
        txtContenido.wrapTextProperty().bind(mnuAjuste.selectedProperty());
        aplicarFuente();
    }

    public void setNota(Nota n) {
        this.nota = n;
        cargando = true;
        txtTitulo.setText(n.getTitulo());
        txtContenido.setText(n.getContenido());
        cargando = false;
        actualizarConteo();
        Platform.runLater(() -> (n.estaVacia() ? txtTitulo : txtContenido).requestFocus());
    }

    // ---------- Guardado automático ----------

    private void sincronizar() {
        nota.setTitulo(txtTitulo.getText());
        nota.setContenido(txtContenido.getText());
    }

    /** Guarda la nota; si está totalmente vacía no deja basura en disco. */
    public void guardar() {
        if (nota == null) return;
        try {
            sincronizar();
            if (nota.estaVacia()) {
                repo.eliminar(nota);
                return;
            }
            nota.setModificada(LocalDateTime.now());
            repo.guardar(nota);
            lblGuardado.setText("Guardado automáticamente a las " + HORA.format(nota.getModificada()));
        } catch (RuntimeException e) {
            lblGuardado.setText("⚠ No se pudo guardar");
        }
    }

    private void actualizarConteo() {
        String t = txtContenido.getText();
        int palabras = t.isBlank() ? 0 : t.strip().split("\\s+").length;
        lblConteo.setText("Palabras: " + palabras + "   Caracteres: " + t.length());
    }

    // ---------- Archivo ----------

    @FXML
    private void onVolver() {
        autoguardado.stop();
        guardar();
        nota = null;
        Navegador.irAInicio();
    }

    @FXML
    private void onNuevaNota() {
        autoguardado.stop();
        guardar();
        Navegador.irAEditor(new Nota());
    }

    @FXML
    private void onGuardarComo() {
        sincronizar();
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar como");
        ExtensionFilter txt = new ExtensionFilter("Texto (*.txt)", "*.txt");
        fc.getExtensionFilters().addAll(txt,
                new ExtensionFilter("PDF (*.pdf)", "*.pdf"),
                new ExtensionFilter("Markdown (*.md)", "*.md"),
                new ExtensionFilter("HTML (*.html)", "*.html"),
                new ExtensionFilter("Rich Text (*.rtf)", "*.rtf"),
                new ExtensionFilter("Otra extensión (*.*)", "*.*"));
        fc.setSelectedExtensionFilter(txt); // predeterminado: .txt
        fc.setInitialFileName(nombreSeguro());

        File f = fc.showSaveDialog(ventana());
        if (f == null) return;

        Path destino = f.toPath();
        if (!f.getName().contains(".")) {
            String e = fc.getSelectedExtensionFilter().getExtensions().get(0);
            destino = Path.of(f.getPath() + (e.equals("*.*") ? ".txt" : e.substring(1)));
        }
        exportar(destino);
    }

    @FXML
    private void onExportarPdf() {
        sincronizar();
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar a PDF");
        fc.getExtensionFilters().add(new ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        fc.setInitialFileName(nombreSeguro() + ".pdf");
        File f = fc.showSaveDialog(ventana());
        if (f == null) return;
        Path destino = f.getName().toLowerCase().endsWith(".pdf") ? f.toPath() : Path.of(f.getPath() + ".pdf");
        exportar(destino);
    }

    private void exportar(Path destino) {
        try {
            ExportService.exportar(nota.getTitulo(), nota.getContenido(), destino);
            lblGuardado.setText("Exportado: " + destino.getFileName());
        } catch (Exception e) {
            error("No se pudo exportar el archivo:\n" + e.getMessage());
        }
    }

    @FXML
    private void onImprimir() {
        sincronizar();
        if (!ImpresionService.imprimir(ventana(), nota.getTitulo(), nota.getContenido())) {
            error("No se encontró ninguna impresora disponible o la impresión falló.");
        }
    }

    @FXML
    private void onEliminar() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar esta nota?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            autoguardado.stop();
            repo.eliminar(nota);
            nota = null;
            Navegador.irAInicio();
        });
    }

    @FXML
    private void onSalir() {
        autoguardado.stop();
        guardar();
        Platform.exit();
    }

    // ---------- Edición ----------

    @FXML private void onDeshacer() { txtContenido.undo(); }
    @FXML private void onRehacer() { txtContenido.redo(); }
    @FXML private void onCortar() { txtContenido.cut(); }
    @FXML private void onCopiar() { txtContenido.copy(); }
    @FXML private void onPegar() { txtContenido.paste(); }
    @FXML private void onSeleccionarTodo() { txtContenido.selectAll(); }

    // ---------- Formato ----------

    @FXML private void onAumentarFuente() { tamanoFuente = Math.min(48, tamanoFuente + 2); aplicarFuente(); }
    @FXML private void onReducirFuente() { tamanoFuente = Math.max(8, tamanoFuente - 2); aplicarFuente(); }
    @FXML private void onFuentePredeterminada() { tamanoFuente = 14; aplicarFuente(); }

    private void aplicarFuente() {
        txtContenido.setStyle("-fx-font-size: " + tamanoFuente + "px;");
    }

    // ---------- Utilidades ----------

    private Window ventana() {
        return txtContenido.getScene().getWindow();
    }

    private String nombreSeguro() {
        String t = txtTitulo.getText().isBlank() ? "Nota" : txtTitulo.getText().strip();
        return t.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}