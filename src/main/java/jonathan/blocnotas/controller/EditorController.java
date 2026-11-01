package main.java.jonathan.blocnotas.controller;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.util.Duration;
import main.java.jonathan.blocnotas.model.Bloque;
import main.java.jonathan.blocnotas.model.Marca;
import main.java.jonathan.blocnotas.model.Nota;
import main.java.jonathan.blocnotas.service.ExportService;
import main.java.jonathan.blocnotas.service.ImpresionService;
import main.java.jonathan.blocnotas.service.NotaRepositorio;
import main.java.jonathan.blocnotas.util.Dialogos;
import main.java.jonathan.blocnotas.util.MarcaVista;
import main.java.jonathan.blocnotas.util.Navegador;

public class EditorController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String FUENTE = "Consolas";
    private static final double MIN_IMAGEN = 80;
    private static final long MAX_ADJUNTO = 100L * 1024 * 1024;
    private static final Set<String> EXT_IMAGEN = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");
    private static final Set<String> EXT_PELIGROSAS = Set.of("exe", "bat", "cmd", "com", "msi", "ps1", "vbs", "js", "jse", "jar", "scr", "lnk", "reg", "hta");

    @FXML private TextField txtTitulo;
    @FXML private ScrollPane scroll;
    @FXML private VBox lienzo;
    @FXML private Label lblGuardado, lblConteo, lblMarca;
    @FXML private Menu mnuMarca;

    private final NotaRepositorio repo = new NotaRepositorio();
    private final PauseTransition autoguardado = new PauseTransition(Duration.seconds(1));
    private final PauseTransition seguimiento = new PauseTransition(Duration.millis(30));
    private final Map<Bloque, TextArea> areas = new IdentityHashMap<>();
    private final Map<String, Image> imagenes = new HashMap<>();
    private Nota nota;
    private TextArea ultimaArea;
    private boolean cargando;
    private double tamanoFuente = 14;

    // ================= Inicio =================
    @FXML
    private void initialize() {
        autoguardado.setOnFinished(e -> guardar());
        seguimiento.setOnFinished(e -> {
            if (ultimaArea != null) asegurarVisible(ultimaArea);
        });
        txtTitulo.textProperty().addListener((o, a, b) -> {
            if (!cargando) autoguardado.playFromStart();
        });
        txtTitulo.setOnAction(e -> {
            TextArea primera = areas.get(nota.getBloques().get(0));
            if (primera != null) primera.requestFocus();
        });
        lienzo.minHeightProperty().bind(scroll.heightProperty().subtract(2));
        lienzo.widthProperty().addListener((o, a, b) -> areas.values().forEach(this::ajustarAltura));
        lienzo.setOnMouseClicked(e -> {
            if (e.getTarget() == lienzo) enfocarUltimo();
        });
        construirMenuMarca();
    }

    public void setNota(Nota n) {
        this.nota = n;
        cargando = true;
        txtTitulo.setText(n.getTitulo());
        cargando = false;
        render();
        actualizarMarca();
        actualizarConteo();
        Platform.runLater(() -> {
            if (n.estaVacia()) txtTitulo.requestFocus();
        });
    }

    // ================= Dibujo de bloques =================
    private void render() {
        nota.normalizar();
        areas.clear();
        ultimaArea = null;
        lienzo.getChildren().clear();
        for (Bloque b : nota.getBloques()) {
            switch (b.getTipo()) {
                case TEXTO -> lienzo.getChildren().add(crearTexto(b));
                case IMAGEN -> lienzo.getChildren().add(crearImagen(b));
                case ARCHIVO -> lienzo.getChildren().add(crearArchivo(b));
            }
        }
    }

    private void rerender() {
        double v = scroll.getVvalue();
        render();
        Platform.runLater(() -> scroll.setVvalue(v));
    }

    private void enfocarUltimo() {
        List<Bloque> l = nota.getBloques();
        TextArea ta = areas.get(l.get(l.size() - 1));
        if (ta != null) {
            ta.requestFocus();
            ta.positionCaret(ta.getLength());
        }
    }

    // ----- Texto -----
    private TextArea crearTexto(Bloque b) {
        TextArea ta = new TextArea(b.getTexto());
        ta.setWrapText(true);
        ta.getStyleClass().add("bloque-texto");
        ta.setStyle(estiloFuente());
        ta.setUserData(b);
        ta.setMinHeight(Region.USE_PREF_SIZE);
        ta.setMaxHeight(Region.USE_PREF_SIZE);
        ajustarAltura(ta);

        ta.textProperty().addListener((o, a, t) -> {
            b.setTexto(t);
            ajustarAltura(ta);
            cambio();
        });
        ta.widthProperty().addListener((o, a, w) -> ajustarAltura(ta));
        ta.caretPositionProperty().addListener((o, a, c) -> seguimiento.playFromStart());
        ta.focusedProperty().addListener((o, a, f) -> {
            if (f) ultimaArea = ta;
        });
        areas.put(b, ta);
        return ta;
    }

    private void ajustarAltura(TextArea ta) {
        String t = ta.getText();
        if (t.isEmpty() || t.endsWith("\n")) t += " ";
        Text medida = new Text(t);
        medida.setFont(Font.font(FUENTE, tamanoFuente));
        double ancho = ta.getWidth() > 0 ? ta.getWidth() : Math.max(300, lienzo.getWidth() - 40);
        medida.setWrappingWidth(Math.max(50, ancho - 16));
        ta.setPrefHeight(Math.ceil(medida.getLayoutBounds().getHeight()) + 12);
    }

    private void asegurarVisible(TextArea ta) {
        if (!ta.isFocused()) return;
        String antes = ta.getText().substring(0, Math.min(ta.getCaretPosition(), ta.getLength()));
        if (antes.isEmpty() || antes.endsWith("\n")) antes += " ";
        Font f = Font.font(FUENTE, tamanoFuente);
        Text t = new Text(antes);
        t.setFont(f);
        t.setWrappingWidth(Math.max(50, ta.getWidth() - 16));
        Text l = new Text("Ag");
        l.setFont(f);

        double abajo = ta.getBoundsInParent().getMinY() + 2 + t.getLayoutBounds().getHeight();
        double arriba = abajo - l.getLayoutBounds().getHeight();
        double visible = scroll.getViewportBounds().getHeight();
        double total = lienzo.getHeight();
        if (total <= visible) return;

        double top = scroll.getVvalue() * (total - visible);
        double nuevo = top;
        if (abajo > top + visible - 24) nuevo = abajo - visible + 24;
        else if (arriba < top + 8) nuevo = arriba - 8;
        scroll.setVvalue(Math.max(0, Math.min(1, nuevo / (total - visible))));
    }

    private String estiloFuente() {
        return "-fx-font-family: '" + FUENTE + "'; -fx-font-size: " + tamanoFuente + "px;";
    }

    // ----- Imagen -----
    private Node crearImagen(Bloque b) {
        Image img = cargarImagen(b);
        if (img == null) return crearArchivo(b);

        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setFitWidth(Math.min(b.getAncho(), anchoDisponible()));

        final boolean der = b.getAlineacion() == Bloque.Alineacion.DERECHA;
        final boolean centro = b.getAlineacion() == Bloque.Alineacion.CENTRO;

        Region tirador = new Region();
        tirador.getStyleClass().add("tirador");
        tirador.setPrefSize(12, 12);
        tirador.setMaxSize(12, 12);
        tirador.setCursor(der ? Cursor.SW_RESIZE : Cursor.SE_RESIZE);

        StackPane marco = new StackPane(iv, tirador);
        marco.getStyleClass().add("marco-imagen");
        marco.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(tirador, der ? Pos.BOTTOM_LEFT : Pos.BOTTOM_RIGHT);

        double[] ini = new double[2];
        tirador.setOnMousePressed(e -> {
            ini[0] = e.getScreenX();
            ini[1] = iv.getFitWidth();
            e.consume();
        });
        tirador.setOnMouseDragged(e -> {
            double factor = der ? -1 : (centro ? 2 : 1);
            double nuevo = Math.max(MIN_IMAGEN, Math.min(anchoDisponible(), ini[1] + (e.getScreenX() - ini[0]) * factor));
            b.setAncho(nuevo);
            iv.setFitWidth(nuevo);
            cambio();
            e.consume();
        });

        HBox barra = new HBox(2,
                mini("Izq", b.getAlineacion() == Bloque.Alineacion.IZQUIERDA, () -> alinear(b, Bloque.Alineacion.IZQUIERDA)),
                mini("Centro", centro, () -> alinear(b, Bloque.Alineacion.CENTRO)),
                mini("Der", der, () -> alinear(b, Bloque.Alineacion.DERECHA)),
                mini("100%", false, () -> {
                    b.setAncho(anchoDisponible());
                    rerender();
                    cambio();
                }),
                mini("▲", false, () -> mover(b, -1)),
                mini("▼", false, () -> mover(b, 1)),
                mini("✕", false, () -> quitar(b)));
        barra.getStyleClass().add("barra-bloque");

        Pos pos = posicion(b.getAlineacion());
        VBox contenido = new VBox(3, barra, marco);
        contenido.setAlignment(pos);
        contenido.setMaxWidth(Region.USE_PREF_SIZE);
        barra.opacityProperty().bind(Bindings.when(contenido.hoverProperty()).then(1.0).otherwise(0.3));

        HBox fila = new HBox(contenido);
        fila.setAlignment(pos);
        fila.setPadding(new Insets(4, 0, 4, 0));
        return fila;
    }

    private Image cargarImagen(Bloque b) {
        Image img = imagenes.get(b.getArchivo());
        if (img != null) return img;
        try {
            Image nueva = new Image(repo.rutaAdjunto(nota, b).toUri().toString(), 1600, 0, true, true);
            if (nueva.isError() || nueva.getWidth() <= 0) return null;
            imagenes.put(b.getArchivo(), nueva);
            return nueva;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Pos posicion(Bloque.Alineacion a) {
        return switch (a) {
            case IZQUIERDA -> Pos.TOP_LEFT;
            case CENTRO -> Pos.TOP_CENTER;
            case DERECHA -> Pos.TOP_RIGHT;
        };
    }

    private double anchoDisponible() {
        double w = lienzo.getWidth();
        return w > 0 ? Math.max(MIN_IMAGEN, w - 48) : 600;
    }

    // ----- Archivo -----
    private Node crearArchivo(Bloque b) {
        String ext = extension(b.getNombreOriginal()).toUpperCase();
        if (ext.length() > 4) ext = ext.substring(0, 4);
        Label insignia = new Label(ext.isEmpty() ? "FILE" : ext);
        insignia.getStyleClass().add("insignia-archivo");

        Label nombre = new Label(b.getNombreOriginal());
        nombre.getStyleClass().add("archivo-nombre");
        nombre.setMaxWidth(300);
        Label info = new Label(tamanoArchivo(b));
        info.getStyleClass().add("archivo-info");
        VBox textos = new VBox(2, nombre, info);
        HBox.setHgrow(textos, Priority.ALWAYS);

        HBox tarjeta = new HBox(10, insignia, textos,
                mini("Abrir", false, () -> abrirArchivo(b)),
                mini("Guardar copia", false, () -> guardarCopia(b)),
                mini("▲", false, () -> mover(b, -1)),
                mini("▼", false, () -> mover(b, 1)),
                mini("✕", false, () -> quitar(b)));
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.getStyleClass().add("tarjeta-archivo");
        tarjeta.setMaxWidth(560);

        VBox fila = new VBox(tarjeta);
        fila.setPadding(new Insets(4, 0, 4, 0));
        return fila;
    }

    private String tamanoArchivo(Bloque b) {
        try {
            long n = Files.size(repo.rutaAdjunto(nota, b));
            if (n < 1024) return n + " B";
            if (n < 1048576) return String.format("%.0f KB", n / 1024.0);
            return String.format("%.1f MB", n / 1048576.0);
        } catch (Exception e) {
            return "Archivo no disponible";
        }
    }

    private Button mini(String texto, boolean activo, Runnable accion) {
        Button bt = new Button(texto);
        bt.getStyleClass().add("boton-mini");
        if (activo) bt.getStyleClass().add("boton-mini-activo");
        bt.setFocusTraversable(false);
        bt.setOnAction(e -> accion.run());
        return bt;
    }

    // ================= Operaciones sobre bloques =================
    private void alinear(Bloque b, Bloque.Alineacion a) {
        b.setAlineacion(a);
        rerender();
        cambio();
    }

    private void mover(Bloque b, int delta) {
        nota.mover(b, delta);
        rerender();
        cambio();
    }

    private void quitar(Bloque b) {
        if (!Dialogos.confirmar("Quitar adjunto", "¿Quitar «" + b.getNombreOriginal() + "» de la nota?", "Quitar")) return;
        nota.getBloques().remove(b);
        rerender();
        cambio();
    }

    private void abrirArchivo(Bloque b) {
        Path p;
        try {
            p = repo.rutaAdjunto(nota, b);
        } catch (RuntimeException e) {
            Dialogos.error("El adjunto no es válido.");
            return;
        }
        if (!Files.exists(p)) {
            Dialogos.error("El archivo adjunto ya no existe en el disco.");
            return;
        }
        if (EXT_PELIGROSAS.contains(extension(b.getNombreOriginal()))) {
            Dialogos.error("Por seguridad no se abren archivos ejecutables desde la nota.\nUsa «Guardar copia» si quieres sacarlo.");
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            Dialogos.error("Este sistema no permite abrir archivos desde la app.");
            return;
        }
        Thread t = new Thread(() -> {
            try {
                Desktop.getDesktop().open(p.toFile());
            } catch (Exception e) {
                Platform.runLater(() -> Dialogos.error("No se pudo abrir el archivo:\n" + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void guardarCopia(Bloque b) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar copia del adjunto");
        fc.setInitialFileName(b.getNombreOriginal());
        File f = fc.showSaveDialog(ventana());
        if (f == null) return;
        try {
            Files.copy(repo.rutaAdjunto(nota, b), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
            lblGuardado.setText("Copia guardada: " + f.getName());
        } catch (Exception e) {
            Dialogos.error("No se pudo guardar la copia:\n" + e.getMessage());
        }
    }

    // ================= Adjuntar =================
    @FXML
    private void onAdjuntar() {
        if (nota == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Adjuntar imágenes o archivos");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Todos los archivos", "*.*"),
                new ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
                new ExtensionFilter("Documentos", "*.pdf", "*.doc", "*.docx", "*.xls", "*.xlsx", "*.ppt", "*.pptx", "*.txt", "*.odt"),
                new ExtensionFilter("Comprimidos", "*.zip", "*.rar", "*.7z"));
        List<File> archivos = fc.showOpenMultipleDialog(ventana());
        if (archivos == null || archivos.isEmpty()) return;

        List<Bloque> nuevos = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        for (File f : archivos) {
            try {
                if (f.length() > MAX_ADJUNTO) {
                    avisos.add(f.getName() + ": supera el límite de 100 MB.");
                    continue;
                }
                String guardado = repo.copiarAdjunto(nota, f.toPath());
                Bloque bloque = Bloque.archivo(guardado, f.getName());
                String ext = extension(f.getName());
                if (EXT_IMAGEN.contains(ext)) {
                    Image img = cargarImagen(bloque);
                    if (img != null) {
                        bloque = Bloque.imagen(guardado, f.getName(), Math.min(img.getWidth(), 480));
                    } else if (ext.equals("webp")) {
                        avisos.add(f.getName() + ": WebP no se puede mostrar dentro de la nota, se adjuntó como archivo.");
                    } else {
                        avisos.add(f.getName() + ": no se pudo leer como imagen, se adjuntó como archivo.");
                    }
                }
                nuevos.add(bloque);
            } catch (Exception e) {
                avisos.add(f.getName() + ": " + e.getMessage());
            }
        }
        if (!nuevos.isEmpty()) insertar(nuevos);
        if (!avisos.isEmpty()) Dialogos.info(String.join("\n", avisos));
    }

    private void insertar(List<Bloque> nuevos) {
        List<Bloque> lista = nota.getBloques();
        Bloque destino;
        int caret;
        if (ultimaArea != null && ultimaArea.getUserData() instanceof Bloque b && lista.contains(b)) {
            destino = b;
            caret = Math.min(ultimaArea.getCaretPosition(), b.getTexto().length());
        } else {
            destino = lista.get(lista.size() - 1);
            caret = destino.getTexto().length();
        }
        int idx = lista.indexOf(destino);
        String t = destino.getTexto();
        Bloque antes = Bloque.texto(t.substring(0, caret));
        Bloque despues = Bloque.texto(t.substring(caret));

        lista.remove(idx);
        lista.add(idx, antes);
        lista.addAll(idx + 1, nuevos);
        lista.add(idx + 1 + nuevos.size(), despues);

        rerender();
        cambio();
        TextArea ta = areas.get(despues);
        if (ta != null) {
            Platform.runLater(() -> {
                ta.requestFocus();
                ta.positionCaret(0);
                asegurarVisible(ta);
            });
        }
    }

    private static String extension(String nombre) {
        int i = nombre.lastIndexOf('.');
        return i >= 0 ? nombre.substring(i + 1).toLowerCase() : "";
    }

    // ================= Guardado automático =================
    private void cambio() {
        autoguardado.playFromStart();
        actualizarConteo();
    }

    private void sincronizar() {
        nota.setTitulo(txtTitulo.getText());
    }

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
        String t = nota.getContenido();
        int palabras = t.isBlank() ? 0 : t.strip().split("\\s+").length;
        lblConteo.setText("Palabras: " + palabras + "   Caracteres: " + t.length());
    }

    // ================= Marca =================
    private void construirMenuMarca() {
        for (Marca m : Marca.values()) {
            MenuItem item = new MenuItem(m.getNombre());
            item.setGraphic(MarcaVista.punto(m));
            item.setOnAction(e -> {
                nota.setMarca(m);
                actualizarMarca();
                cambio();
            });
            mnuMarca.getItems().add(item);
        }
    }

    private void actualizarMarca() {
        Marca m = nota.getMarca();
        lblMarca.setText(m.esNinguna() ? "" : m.getNombre());
        lblMarca.setGraphic(m.esNinguna() ? null : MarcaVista.punto(m));
    }

    // ================= Archivo =================
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
                new ExtensionFilter("Nota con adjuntos (*.zip)", "*.zip"),
                new ExtensionFilter("Markdown (*.md)", "*.md"),
                new ExtensionFilter("HTML (*.html)", "*.html"),
                new ExtensionFilter("Rich Text (*.rtf)", "*.rtf"),
                new ExtensionFilter("Otra extensión (*.*)", "*.*"));
        fc.setSelectedExtensionFilter(txt);
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
        String ext = ExportService.extension(destino);
        if (!confirmarPerdidas(ext)) return;
        try {
            ExportService.exportar(nota, destino, repo);
            lblGuardado.setText("Exportado: " + destino.getFileName());
        } catch (Exception e) {
            Dialogos.error("No se pudo exportar el archivo:\n" + e.getMessage());
        }
    }

    private boolean confirmarPerdidas(String ext) {
        int imgs = nota.contarImagenes();
        List<Bloque> archivos = nota.getArchivos();
        boolean pierdeImg = imgs > 0 && !ExportService.admiteImagenes(ext);
        boolean pierdeArch = !archivos.isEmpty() && !ExportService.admiteArchivos(ext);
        if (!pierdeImg && !pierdeArch) return true;

        StringBuilder sb = new StringBuilder("El formato ." + ext.toUpperCase() + " no puede incluir:\n");
        if (pierdeImg) sb.append("\n• Imágenes (").append(imgs).append(")");
        if (pierdeArch) {
            sb.append("\n• Archivos adjuntos: ");
            sb.append(String.join(", ", archivos.stream().limit(5).map(Bloque::getNombreOriginal).toList()));
            if (archivos.size() > 5) sb.append("… (+").append(archivos.size() - 5).append(")");
        }
        sb.append("\n\nSe quedan en tu nota, pero no aparecerán en el archivo exportado.");
        sb.append("\nPara conservarlo todo, exporta como ZIP.");
        return Dialogos.confirmar("Algunos elementos no se exportarán", sb.toString(), "Exportar de todos modos");
    }

    @FXML
    private void onImprimir() {
        sincronizar();
        if (!ImpresionService.imprimir(ventana(), nota, repo)) {
            Dialogos.error("No se encontró ninguna impresora disponible o la impresión falló.");
        }
    }

    @FXML
    private void onImprimirFactura() {
        sincronizar();
        int imgs = nota.contarImagenes();
        int archivos = nota.getArchivos().size();
        String encabezado = "Modo factura: solo texto en letra monoespaciada."
                + (imgs + archivos > 0 ? "\nSe omitirán " + imgs + " imagen(es) y " + archivos + " archivo(s)." : "")
                + "\nEn la siguiente ventana elige tu impresora de facturas.";
        List<String> opciones = List.of("Ticket 80 mm", "Ticket 58 mm", "Hoja carta / A4");
        Optional<String> r = Dialogos.elegir("Imprimir como factura", encabezado, "Formato:", opciones);
        if (r.isEmpty()) return;
        double fuente = switch (opciones.indexOf(r.get())) {
            case 0 -> 9;
            case 1 -> 8;
            default -> 11;
        };
        if (!ImpresionService.imprimirFactura(ventana(), nota, fuente)) {
            Dialogos.error("No se encontró ninguna impresora disponible o la impresión falló.");
        }
    }

    @FXML
    private void onEliminar() {
        if (Dialogos.confirmar("Eliminar nota", "¿Eliminar esta nota y todos sus adjuntos?", "Eliminar")) {
            autoguardado.stop();
            repo.eliminar(nota);
            nota = null;
            Navegador.irAInicio();
        }
    }

    @FXML
    private void onSalir() {
        autoguardado.stop();
        guardar();
        Platform.exit();
    }

    // ================= Edición =================
    private void conArea(Consumer<TextArea> accion) {
        if (ultimaArea != null) {
            ultimaArea.requestFocus();
            accion.accept(ultimaArea);
        }
    }

    @FXML private void onDeshacer() { conArea(TextArea::undo); }
    @FXML private void onRehacer() { conArea(TextArea::redo); }
    @FXML private void onCortar() { conArea(TextArea::cut); }
    @FXML private void onCopiar() { conArea(TextArea::copy); }
    @FXML private void onPegar() { conArea(TextArea::paste); }
    @FXML private void onSeleccionarTodo() { conArea(TextArea::selectAll); }

    // ================= Formato =================
    @FXML private void onAumentarFuente() { tamanoFuente = Math.min(48, tamanoFuente + 2); aplicarFuente(); }
    @FXML private void onReducirFuente() { tamanoFuente = Math.max(8, tamanoFuente - 2); aplicarFuente(); }
    @FXML private void onFuentePredeterminada() { tamanoFuente = 14; aplicarFuente(); }

    private void aplicarFuente() {
        for (TextArea ta : areas.values()) {
            ta.setStyle(estiloFuente());
            ajustarAltura(ta);
        }
    }

    // ================= Utilidades =================
    private Window ventana() {
        return lienzo.getScene().getWindow();
    }

    private String nombreSeguro() {
        String t = txtTitulo.getText().isBlank() ? "Nota" : txtTitulo.getText().strip();
        return t.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}