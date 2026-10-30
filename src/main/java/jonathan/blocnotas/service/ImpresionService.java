package main.java.jonathan.blocnotas.service;

import javafx.geometry.VPos;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Window;

public final class ImpresionService {

    private ImpresionService() {}

    /** @return false si no hay impresora disponible o falló la impresión. */
    public static boolean imprimir(Window owner, String titulo, String contenido) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return false;
        if (!job.showPrintDialog(owner)) return true; // el usuario canceló

        PageLayout layout = job.getJobSettings().getPageLayout();
        double ancho = layout.getPrintableWidth();
        double alto = layout.getPrintableHeight();
        String texto = (titulo.isBlank() ? "" : titulo + "\n\n") + contenido;
        Font fuente = Font.font("System", 12);

        Text muestra = new Text("Ag");
        muestra.setFont(fuente);
        double altoLinea = muestra.getLayoutBounds().getHeight();
        double altoPagina = Math.floor(alto / altoLinea) * altoLinea;

        Text medida = crearTexto(texto, fuente, ancho);
        int paginas = Math.max(1, (int) Math.ceil(medida.getLayoutBounds().getHeight() / altoPagina));

        boolean ok = true;
        for (int i = 0; i < paginas && ok; i++) {
            Text t = crearTexto(texto, fuente, ancho);
            t.setTranslateY(-i * altoPagina);
            Pane pagina = new Pane(t);
            pagina.setPrefSize(ancho, altoPagina);
            pagina.setClip(new Rectangle(ancho, altoPagina));
            ok = job.printPage(pagina);
        }
        if (ok) job.endJob(); else job.cancelJob();
        return ok;
    }

    private static Text crearTexto(String s, Font f, double ancho) {
        Text t = new Text(s);
        t.setFont(f);
        t.setWrappingWidth(ancho);
        t.setTextOrigin(VPos.TOP);
        return t;
    }
}