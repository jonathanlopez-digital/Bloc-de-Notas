package main.java.jonathan.blocnotas.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Generador mínimo de PDF (A4, Helvetica, con saltos de página). */
public final class PdfWriter {

    private static final Charset WIN = Charset.forName("windows-1252");
    private static final int MARGEN = 56, ALTO = 842, ANCHO = 595;
    private static final int FUENTE = 11, INTERLINEADO = 14, MAX_CARACTERES = 80;

    private PdfWriter() {}

    public static byte[] generar(String titulo, String contenido) {
        String tit = (titulo == null || titulo.isBlank()) ? "Nota" : titulo.strip();
        if (tit.length() > 60) tit = tit.substring(0, 60) + "...";

        List<String> lineas = new ArrayList<>();
        for (String p : contenido.replace("\r", "").replace("\t", "    ").split("\n", -1)) {
            lineas.addAll(ajustar(p));
        }

        List<List<String>> paginas = new ArrayList<>();
        int i = 0;
        boolean primera = true;
        do {
            int yInicio = primera ? ALTO - MARGEN - 28 : ALTO - MARGEN;
            int capacidad = (yInicio - MARGEN) / INTERLINEADO + 1;
            int fin = Math.min(lineas.size(), i + capacidad);
            paginas.add(new ArrayList<>(lineas.subList(i, fin)));
            i = fin;
            primera = false;
        } while (i < lineas.size());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        int n = paginas.size();

        escribir(out, "%PDF-1.4\n");
        objeto(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < n; p++) kids.append(5 + 2 * p).append(" 0 R ");
        objeto(out, offsets, 2, "<< /Type /Pages /Kids [" + kids + "] /Count " + n + " >>");
        objeto(out, offsets, 3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objeto(out, offsets, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");

        for (int p = 0; p < n; p++) {
            objeto(out, offsets, 5 + 2 * p,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + ANCHO + " " + ALTO + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (6 + 2 * p) + " 0 R >>");

            ByteArrayOutputStream s = new ByteArrayOutputStream();
            int y = ALTO - MARGEN;
            if (p == 0) {
                escribir(s, "BT /F2 16 Tf " + MARGEN + " " + (ALTO - MARGEN - 12) + " Td (");
                s.writeBytes(escapar(tit).getBytes(WIN));
                escribir(s, ") Tj ET\n");
                y = ALTO - MARGEN - 28;
            }
            escribir(s, "BT /F1 " + FUENTE + " Tf " + INTERLINEADO + " TL " + MARGEN + " " + y + " Td\n");
            for (String l : paginas.get(p)) {
                escribir(s, "(");
                s.writeBytes(escapar(l).getBytes(WIN));
                escribir(s, ") Tj T*\n");
            }
            escribir(s, "ET");
            byte[] flujo = s.toByteArray();

            offsets.add(out.size());
            escribir(out, (6 + 2 * p) + " 0 obj\n<< /Length " + flujo.length + " >>\nstream\n");
            out.writeBytes(flujo);
            escribir(out, "\nendstream\nendobj\n");
        }

        int xref = out.size();
        escribir(out, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) escribir(out, String.format("%010d 00000 n \n", off));
        escribir(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF");
        return out.toByteArray();
    }

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