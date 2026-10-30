package main.java.jonathan.blocnotas.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exporta según la extensión: txt (predeterminado), pdf, html, md, rtf. Cualquier otra se guarda como texto plano. */
public final class ExportService {

    private ExportService() {}

    public static void exportar(String titulo, String contenido, Path destino) throws IOException {
        String nombre = destino.getFileName().toString().toLowerCase();
        String ext = nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.') + 1) : "txt";

        switch (ext) {
            case "pdf" -> Files.write(destino, PdfWriter.generar(titulo, contenido));
            case "html", "htm" -> Files.writeString(destino, aHtml(titulo, contenido), StandardCharsets.UTF_8);
            case "md" -> Files.writeString(destino,
                    (titulo.isBlank() ? "" : "# " + titulo + "\n\n") + contenido, StandardCharsets.UTF_8);
            case "rtf" -> Files.writeString(destino, aRtf(titulo, contenido), StandardCharsets.US_ASCII);
            default -> Files.writeString(destino, contenido, StandardCharsets.UTF_8);
        }
    }

    private static String aHtml(String titulo, String contenido) {
        String t = escaparHtml(titulo.isBlank() ? "Nota" : titulo);
        return "<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n<meta charset=\"UTF-8\">\n<title>" + t
                + "</title>\n</head>\n<body style=\"font-family:Segoe UI,Arial,sans-serif;max-width:800px;margin:40px auto;\">\n"
                + "<h1>" + t + "</h1>\n<pre style=\"white-space:pre-wrap;font-family:inherit;font-size:16px;\">"
                + escaparHtml(contenido) + "</pre>\n</body>\n</html>\n";
    }

    private static String escaparHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String aRtf(String titulo, String contenido) {
        StringBuilder sb = new StringBuilder("{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Calibri;}}\\f0\\fs24 ");
        if (!titulo.isBlank()) sb.append("{\\b\\fs32 ").append(escaparRtf(titulo)).append("}\\par\\par ");
        sb.append(escaparRtf(contenido)).append("}");
        return sb.toString();
    }

    private static String escaparRtf(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.replace("\r", "").toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '{' -> sb.append("\\{");
                case '}' -> sb.append("\\}");
                case '\n' -> sb.append("\\par\n");
                case '\t' -> sb.append("\\tab ");
                default -> {
                    if (c > 127) sb.append("\\u").append((int) (short) c).append('?');
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}