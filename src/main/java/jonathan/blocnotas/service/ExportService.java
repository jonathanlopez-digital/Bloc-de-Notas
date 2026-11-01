package main.java.jonathan.blocnotas.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import main.java.jonathan.blocnotas.model.Bloque;
import main.java.jonathan.blocnotas.model.Nota;

/** Formatos: txt (predeterminado), pdf, html, md, rtf, zip. Cualquier otra extensión se guarda como texto plano. */
public final class ExportService {

    private ExportService() {}

    // ---------- Compatibilidad por formato ----------
    public static boolean admiteImagenes(String ext) {
        return Set.of("pdf", "html", "htm", "zip").contains(ext);
    }

    public static boolean admiteArchivos(String ext) {
        return ext.equals("zip");
    }

    public static String extension(Path destino) {
        String nombre = destino.getFileName().toString().toLowerCase();
        return nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.') + 1) : "txt";
    }

    // ---------- Exportar ----------
    public static void exportar(Nota nota, Path destino, NotaRepositorio repo) throws IOException {
        String ext = extension(destino);
        String titulo = nota.getTitulo();
        String texto = nota.getContenido();

        switch (ext) {
            case "pdf" -> Files.write(destino, PdfWriter.generar(nota, repo));
            case "html", "htm" -> Files.writeString(destino, aHtml(nota, repo), StandardCharsets.UTF_8);
            case "md" -> Files.writeString(destino, (titulo.isBlank() ? "" : "# " + titulo + "\n\n") + texto, StandardCharsets.UTF_8);
            case "rtf" -> Files.writeString(destino, aRtf(titulo, texto), StandardCharsets.US_ASCII);
            case "zip" -> aZip(nota, destino, repo);
            default -> Files.writeString(destino, texto, StandardCharsets.UTF_8);
        }
    }

    // ---------- ZIP ----------
    private static void aZip(Nota nota, Path destino, NotaRepositorio repo) throws IOException {
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(destino), StandardCharsets.UTF_8)) {
            String cabecera = nota.getTitulo().isBlank() ? "" : nota.getTitulo() + "\n\n";
            z.putNextEntry(new ZipEntry("nota.txt"));
            z.write((cabecera + nota.getContenido()).getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
            int i = 1;
            for (Bloque b : nota.getBloques()) {
                if (b.esTexto()) continue;
                String nombre = b.getNombreOriginal().replaceAll("[\\\\/:*?\"<>|]", "_");
                z.putNextEntry(new ZipEntry("adjuntos/" + (i++) + "_" + nombre));
                Files.copy(repo.rutaAdjunto(nota, b), z);
                z.closeEntry();
            }
        }
    }

    // ---------- HTML ----------
    private static String aHtml(Nota nota, NotaRepositorio repo) throws IOException {
        String t = escaparHtml(nota.getTitulo().isBlank() ? "Nota" : nota.getTitulo());
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n<meta charset=\"UTF-8\">\n<title>").append(t)
          .append("</title>\n</head>\n<body style=\"font-family:Segoe UI,Arial,sans-serif;max-width:800px;margin:40px auto;\">\n")
          .append("<h1>").append(t).append("</h1>\n");

        for (Bloque b : nota.getBloques()) {
            switch (b.getTipo()) {
                case TEXTO -> {
                    if (!b.getTexto().isBlank()) {
                        sb.append("<pre style=\"white-space:pre-wrap;font-family:inherit;font-size:16px;\">")
                          .append(escaparHtml(b.getTexto())).append("</pre>\n");
                    }
                }
                case IMAGEN -> {
                    String ext = b.getNombreOriginal().toLowerCase();
                    ext = ext.contains(".") ? ext.substring(ext.lastIndexOf('.') + 1) : "png";
                    String mime = ext.equals("jpg") ? "image/jpeg" : "image/" + ext;
                    String alineacion = switch (b.getAlineacion()) {
                        case IZQUIERDA -> "left";
                        case CENTRO -> "center";
                        case DERECHA -> "right";
                    };
                    String datos = Base64.getEncoder().encodeToString(Files.readAllBytes(repo.rutaAdjunto(nota, b)));
                    sb.append("<div style=\"text-align:").append(alineacion).append("\"><img alt=\"\" style=\"width:")
                      .append((int) b.getAncho()).append("px;max-width:100%;height:auto\" src=\"data:").append(mime)
                      .append(";base64,").append(datos).append("\"></div>\n");
                }
                case ARCHIVO -> sb.append("<p><em>[Archivo adjunto no incluido: ")
                                  .append(escaparHtml(b.getNombreOriginal())).append("]</em></p>\n");
            }
        }
        return sb.append("</body>\n</html>\n").toString();
    }

    private static String escaparHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ---------- RTF ----------
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