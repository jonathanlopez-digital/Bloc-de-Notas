package main.java.jonathan.blocnotas.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.VPos;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;
import main.java.jonathan.blocnotas.model.Bloque;
import main.java.jonathan.blocnotas.model.Nota;

public final class ImpresionService {

    private static final double PT_POR_PX = 0.75;
    private static final String ALINEACION = "alineacion";

    private ImpresionService() {}

    // ---------- Impresión normal (texto + imágenes) ----------
    /** @return false si no hay impresora disponible o falló la impresión. */
    public static boolean imprimir(Window owner, Nota nota, NotaRepositorio repo) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return false;
        if (!job.showPrintDialog(owner)) return true;

        PageLayout layout = job.getJobSettings().getPageLayout();
        double ancho = layout.getPrintableWidth(), alto = layout.getPrintableHeight();
        Font normal = Font.font("System", 12);

        List<Node> nodos = new ArrayList<>();
        if (!nota.getTitulo().isBlank()) {
            nodos.add(texto(nota.getTitulo(), Font.font("System", FontWeight.BOLD, 18), ancho, TextAlignment.LEFT));
            nodos.add(texto(" ", normal, ancho, TextAlignment.LEFT));
        }
        for (Bloque b : nota.getBloques()) {
            switch (b.getTipo()) {
                case TEXTO -> {
                    for (String p : b.getTexto().replace("\r", "").replace("\t", "    ").split("\n", -1)) {
                        nodos.add(texto(p, normal, ancho, TextAlignment.LEFT));
                    }
                }
                case IMAGEN -> nodos.add(imagen(nota, b, repo, normal, ancho, alto));
                case ARCHIVO -> nodos.add(texto("[Archivo adjunto: " + b.getNombreOriginal() + "]", normal, ancho, TextAlignment.LEFT));
            }
        }
        return imprimirNodos(job, nodos, ancho, alto, altoLinea(normal));
    }

    // ---------- Modo factura (solo texto, monoespaciado) ----------
    public static boolean imprimirFactura(Window owner, Nota nota, double tamFuente) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return false;
        if (!job.showPrintDialog(owner)) return true;

        try {
            Printer impresora = job.getPrinter();
            PageLayout base = job.getJobSettings().getPageLayout();
            job.getJobSettings().setPageLayout(impresora.createPageLayout(
                    base.getPaper(), base.getPageOrientation(), Printer.MarginType.HARDWARE_MINIMUM));
        } catch (RuntimeException ignorado) {
        }

        PageLayout layout = job.getJobSettings().getPageLayout();
        double ancho = layout.getPrintableWidth(), alto = layout.getPrintableHeight();
        Font mono = Font.font("Consolas", tamFuente);
        Font titulo = Font.font("Consolas", FontWeight.BOLD, tamFuente + 2);

        Text medida = new Text("-");
        medida.setFont(mono);
        int columnas = Math.max(8, (int) (ancho / medida.getLayoutBounds().getWidth()));
        String separador = "-".repeat(columnas);

        List<Node> nodos = new ArrayList<>();
        if (!nota.getTitulo().isBlank()) nodos.add(texto(limpiar(nota.getTitulo()), titulo, ancho, TextAlignment.CENTER));
        nodos.add(texto(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()), mono, ancho, TextAlignment.CENTER));
        nodos.add(texto(separador, mono, ancho, TextAlignment.LEFT));
        for (String linea : nota.getContenido().replace("\r", "").split("\n", -1)) {
            nodos.add(texto(limpiar(linea), mono, ancho, TextAlignment.LEFT));
        }
        nodos.add(texto(separador, mono, ancho, TextAlignment.LEFT));
        nodos.add(texto(" ", mono, ancho, TextAlignment.LEFT));
        nodos.add(texto(" ", mono, ancho, TextAlignment.LEFT));

        return imprimirNodos(job, nodos, ancho, alto, altoLinea(mono));
    }

    private static String limpiar(String s) {
        return s.replace("\t", "    ").replaceAll("\\p{Cntrl}", "");
    }

    // ---------- Paginación ----------
    private static boolean imprimirNodos(PrinterJob job, List<Node> nodos, double ancho, double alto, double altoLinea) {
        List<Pane> paginas = new ArrayList<>();
        Pane pag = nuevaPagina(ancho, alto);
        paginas.add(pag);
        double y = 0;

        for (Node n : nodos) {
            double h = n.getLayoutBounds().getHeight();

            if (n instanceof Text t && h > alto) {
                double paso = Math.max(altoLinea, Math.floor(alto / altoLinea) * altoLinea);
                if (y > 0) { pag = nuevaPagina(ancho, alto); paginas.add(pag); }
                int partes = (int) Math.ceil(h / paso);
                for (int i = 0; i < partes; i++) {
                    if (i > 0) { pag = nuevaPagina(ancho, alto); paginas.add(pag); }
                    Text c = copiar(t);
                    c.setClip(new Rectangle(0, i * paso, ancho, paso));
                    c.setLayoutY(-i * paso);
                    pag.getChildren().add(c);
                }
                y = h - (partes - 1) * paso;
                continue;
            }

            if (y > 0 && y + h > alto) {
                pag = nuevaPagina(ancho, alto);
                paginas.add(pag);
                y = 0;
            }
            if (n instanceof ImageView && n.getProperties().get(ALINEACION) instanceof Bloque.Alineacion al) {
                double w = n.getLayoutBounds().getWidth();
                n.setLayoutX(switch (al) {
                    case IZQUIERDA -> 0;
                    case CENTRO -> (ancho - w) / 2;
                    case DERECHA -> ancho - w;
                });
                h += 6;
            }
            n.setLayoutY(y);
            pag.getChildren().add(n);
            y += h;
        }

        if (paginas.size() > 1 && paginas.get(paginas.size() - 1).getChildren().isEmpty()) {
            paginas.remove(paginas.size() - 1);
        }

        boolean ok = true;
        for (Pane p : paginas) {
            if (!ok) break;
            ok = job.printPage(p);
        }
        if (ok) job.endJob(); else job.cancelJob();
        return ok;
    }

    // ---------- Fábricas de nodos ----------
    private static Pane nuevaPagina(double ancho, double alto) {
        Pane p = new Pane();
        p.setPrefSize(ancho, alto);
        p.setClip(new Rectangle(ancho, alto));
        return p;
    }

    private static Text texto(String s, Font f, double ancho, TextAlignment al) {
        Text t = new Text(s.isEmpty() ? " " : s);
        t.setFont(f);
        t.setWrappingWidth(ancho);
        t.setTextAlignment(al);
        t.setTextOrigin(VPos.TOP);
        return t;
    }

    private static Text copiar(Text o) {
        Text t = new Text(o.getText());
        t.setFont(o.getFont());
        t.setWrappingWidth(o.getWrappingWidth());
        t.setTextAlignment(o.getTextAlignment());
        t.setTextOrigin(VPos.TOP);
        return t;
    }

    private static double altoLinea(Font f) {
        Text t = new Text("Ag");
        t.setFont(f);
        return t.getLayoutBounds().getHeight();
    }

    private static Node imagen(Nota nota, Bloque b, NotaRepositorio repo, Font f, double ancho, double alto) {
        try {
            Image img = new Image(repo.rutaAdjunto(nota, b).toUri().toString(), 2000, 0, true, true);
            if (img.isError() || img.getWidth() <= 0) throw new IllegalStateException();
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iv.setFitWidth(Math.min(ancho, b.getAncho() * PT_POR_PX));
            iv.setFitHeight(Math.max(10, alto - 4));
            iv.getProperties().put(ALINEACION, b.getAlineacion());
            return iv;
        } catch (RuntimeException e) {
            return texto("[Imagen no disponible: " + b.getNombreOriginal() + "]", f, ancho, TextAlignment.LEFT);
        }
    }
}