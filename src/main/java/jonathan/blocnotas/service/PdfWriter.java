package main.java.jonathan.blocnotas.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DeflaterOutputStream;
import javax.imageio.ImageIO;
import main.java.jonathan.blocnotas.model.Bloque;
import main.java.jonathan.blocnotas.model.Nota;

/** Generador de PDF (A4, Helvetica) con texto, imágenes y saltos de página automáticos. */
public final class PdfWriter {

    private static final Charset WIN = Charset.forName("windows-1252");
    private static final int ANCHO = 595, ALTO = 842, MARGEN = 56;
    private static final int AREA_ANCHO = ANCHO - 2 * MARGEN, AREA_ALTO = ALTO - 2 * MARGEN;
    private static final int FUENTE = 11, INTERLINEADO = 14, MAX_CARACTERES = 80, MAX_PIXELES = 1600;

    private PdfWriter() {}

    // ---------- Estructuras ----------
    private static final class Pagina {
        final ByteArrayOutputStream cont = new ByteArrayOutputStream();
        final Set<Integer> imgs = new TreeSet<>();
    }

    private static final class Imagen {
        byte[] datos;
        int ancho, alto;
    }

    // ---------- Maquetación ----------
    private static final class Maquetador {
        final List<Pagina> paginas = new ArrayList<>();
        final List<Imagen> imagenes = new ArrayList<>();
        Pagina actual;
        int y;

        Maquetador() { nuevaPagina(); }

        void nuevaPagina() {
            actual = new Pagina();
            paginas.add(actual);
            y = ALTO - MARGEN;
        }

        void parrafo(String texto, boolean negrita, int tam, int interlinea) {
            for (String l : ajustar(texto)) linea(l, negrita, tam, interlinea);
        }

        void linea(String texto, boolean negrita, int tam, int interlinea) {
            if (y - interlinea < MARGEN) nuevaPagina();
            y -= interlinea;
            escribir(actual.cont, "BT /" + (negrita ? "F2" : "F1") + " " + tam + " Tf " + MARGEN + " " + y + " Td (");
            actual.cont.writeBytes(escapar(texto).getBytes(WIN));
            escribir(actual.cont, ") Tj ET\n");
        }

        void imagen(Imagen img, double anchoPt, Bloque.Alineacion al) {
            double w = Math.min(AREA_ANCHO, anchoPt);
            double h = w * img.alto / img.ancho;
            if (h > AREA_ALTO) {
                h = AREA_ALTO;
                w = h * img.ancho / img.alto;
            }
            if (y - h < MARGEN) nuevaPagina();
            double x = MARGEN + switch (al) {
                case IZQUIERDA -> 0;
                case CENTRO -> (AREA_ANCHO - w) / 2;
                case DERECHA -> AREA_ANCHO - w;
            };
            y -= (int) Math.ceil(h);
            int indice = imagenes.size();
            imagenes.add(img);
            actual.imgs.add(indice);
            escribir(actual.cont, String.format(Locale.ROOT, "q %.2f 0 0 %.2f %.2f %d cm /Im%d Do Q\n", w, h, x, y, indice + 1));
            y -= 8;
        }
    }

    // ---------- Generar ----------
    public static byte[] generar(Nota nota, NotaRepositorio repo) {
        Maquetador m = new Maquetador();

        String titulo = nota.getTitulo().isBlank() ? "Nota" : nota.getTitulo().strip();
        if (titulo.length() > 60) titulo = titulo.substring(0, 60) + "...";
        m.linea(titulo, true, 16, 24);
        m.y -= 6;

        for (Bloque b : nota.getBloques()) {
            switch (b.getTipo()) {
                case TEXTO -> {
                    for (String p : b.getTexto().replace("\r", "").replace("\t", "    ").split("\n", -1)) {
                        m.parrafo(p, false, FUENTE, INTERLINEADO);
                    }
                }
                case IMAGEN -> {
                    Imagen img = null;
                    try {
                        img = cargar(repo.rutaAdjunto(nota, b));
                    } catch (Exception ignorado) {
                    }
                    if (img == null) m.parrafo("[Imagen no incluida: " + b.getNombreOriginal() + "]", false, FUENTE, INTERLINEADO);
                    else m.imagen(img, b.getAncho() * 0.75, b.getAlineacion());
                }
                case ARCHIVO -> m.parrafo("[Archivo adjunto no incluido: " + b.getNombreOriginal() + "]", false, FUENTE, INTERLINEADO);
            }
        }
        return escribirPdf(m);
    }

    // ---------- Imágenes ----------
    private static Imagen cargar(Path ruta) throws IOException {
        BufferedImage src = ImageIO.read(ruta.toFile());
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        double esc = Math.min(1.0, (double) MAX_PIXELES / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * esc));
        int nh = Math.max(1, (int) Math.round(h * esc));

        BufferedImage rgb = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();

        int[] px = rgb.getRGB(0, 0, nw, nh, null, 0, nw);
        byte[] raw = new byte[nw * nh * 3];
        int k = 0;
        for (int c : px) {
            raw[k++] = (byte) (c >> 16);
            raw[k++] = (byte) (c >> 8);
            raw[k++] = (byte) c;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DeflaterOutputStream d = new DeflaterOutputStream(bos)) {
            d.write(raw);
        }
        Imagen img = new Imagen();
        img.datos = bos.toByteArray();
        img.ancho = nw;
        img.alto = nh;
        return img;
    }

    // ---------- Escritura del archivo PDF ----------
    private static byte[] escribirPdf(Maquetador m) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        int nImg = m.imagenes.size(), nPag = m.paginas.size();
        int baseImg = 5, basePag = baseImg + nImg;

        escribir(out, "%PDF-1.4\n");
        objeto(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < nPag; p++) kids.append(basePag + 2 * p).append(" 0 R ");
        objeto(out, offsets, 2, "<< /Type /Pages /Kids [" + kids + "] /Count " + nPag + " >>");
        objeto(out, offsets, 3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objeto(out, offsets, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");

        for (int i = 0; i < nImg; i++) {
            Imagen img = m.imagenes.get(i);
            offsets.add(out.size());
            escribir(out, (baseImg + i) + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + img.ancho
                    + " /Height " + img.alto + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length "
                    + img.datos.length + " >>\nstream\n");
            out.writeBytes(img.datos);
            escribir(out, "\nendstream\nendobj\n");
        }

        for (int p = 0; p < nPag; p++) {
            Pagina pg = m.paginas.get(p);
            StringBuilder xo = new StringBuilder();
            for (int i : pg.imgs) xo.append("/Im").append(i + 1).append(' ').append(baseImg + i).append(" 0 R ");
            int idPagina = basePag + 2 * p, idContenido = idPagina + 1;
            objeto(out, offsets, idPagina, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + ANCHO + " " + ALTO + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> /XObject << " + xo + ">> >> /Contents " + idContenido + " 0 R >>");
            byte[] flujo = pg.cont.toByteArray();
            offsets.add(out.size());
            escribir(out, idContenido + " 0 obj\n<< /Length " + flujo.length + " >>\nstream\n");
            out.writeBytes(flujo);
            escribir(out, "\nendstream\nendobj\n");
        }

        int xref = out.size();
        escribir(out, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) escribir(out, String.format("%010d 00000 n \n", off));
        escribir(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF");
        return out.toByteArray();
    }

    // ---------- Utilidades ----------
    private static void objeto(ByteArrayOutputStream out, List<Integer> offsets, int id, String cuerpo) {
        offsets.add(out.size());
        escribir(out, id + " 0 obj\n" + cuerpo + "\nendobj\n");
    }

    private static void escribir(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static List<String> ajustar(String p) {
        List<String> r = new ArrayList<>();
        if (p.isEmpty()) { r.add(""); return r; }
        StringBuilder l = new StringBuilder();
        for (String w : p.split(" ")) {
            while (w.length() > MAX_CARACTERES) {
                if (l.length() > 0) { r.add(l.toString()); l.setLength(0); }
                r.add(w.substring(0, MAX_CARACTERES));
                w = w.substring(MAX_CARACTERES);
            }
            if (l.length() == 0) l.append(w);
            else if (l.length() + 1 + w.length() <= MAX_CARACTERES) l.append(' ').append(w);
            else { r.add(l.toString()); l.setLength(0); l.append(w); }
        }
        r.add(l.toString());
        return r;
    }
}