package main.java.jonathan.blocnotas.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import main.java.jonathan.blocnotas.model.Bloque;
import main.java.jonathan.blocnotas.model.Marca;
import main.java.jonathan.blocnotas.model.Nota;

public class NotaRepositorio {

    private static final String EXT = ".bn";
    private static final String EXT_ANTIGUA = ".nota";
    private static final int MAGIA = 0x424E4F54;
    private static final int VERSION = 1;

    private final Path carpeta = Path.of(System.getProperty("user.home"), ".blocnotas");
    private final Path adjuntos = carpeta.resolve("adjuntos");

    public NotaRepositorio() {
        try {
            Files.createDirectories(adjuntos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- Listar ----------
    public List<Nota> listar() {
        try (Stream<Path> s = Files.list(carpeta)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> {
                        String nombre = p.getFileName().toString();
                        if (nombre.endsWith(EXT)) return leer(p);
                        if (nombre.endsWith(EXT_ANTIGUA)) return leerAntigua(p);
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Nota::getModificada).reversed())
                    .toList();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    // ---------- Guardar ----------
    public void guardar(Nota n) {
        Path destino = carpeta.resolve(n.getId() + EXT);
        Path tmp = carpeta.resolve(n.getId() + ".tmp");
        try {
            try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                o.writeInt(MAGIA);
                o.writeInt(VERSION);
                escribir(o, n.getTitulo());
                escribir(o, n.getModificada().toString());
                escribir(o, n.getMarca().name());
                o.writeInt(n.getBloques().size());
                for (Bloque b : n.getBloques()) {
                    o.writeByte(b.getTipo().ordinal());
                    if (b.esTexto()) {
                        escribir(o, b.getTexto());
                    } else {
                        escribir(o, b.getArchivo());
                        escribir(o, b.getNombreOriginal());
                        o.writeDouble(b.getAncho());
                        o.writeByte(b.getAlineacion().ordinal());
                    }
                }
            }
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(carpeta.resolve(n.getId() + EXT_ANTIGUA));
            limpiarHuerfanos(n);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- Eliminar ----------
    public void eliminar(Nota n) {
        try {
            Files.deleteIfExists(carpeta.resolve(n.getId() + EXT));
            Files.deleteIfExists(carpeta.resolve(n.getId() + EXT_ANTIGUA));
            borrarRecursivo(adjuntos.resolve(n.getId()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- Adjuntos ----------
    public String copiarAdjunto(Nota n, Path origen) throws IOException {
        Path dir = adjuntos.resolve(n.getId());
        Files.createDirectories(dir);
        String nombre = origen.getFileName().toString();
        int i = nombre.lastIndexOf('.');
        String ext = i >= 0 ? nombre.substring(i + 1).toLowerCase().replaceAll("[^a-z0-9]", "") : "";
        if (ext.length() > 8) ext = ext.substring(0, 8);
        String guardado = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Files.copy(origen, dir.resolve(guardado));
        return guardado;
    }

    public Path rutaAdjunto(Nota n, Bloque b) {
        Path dir = adjuntos.resolve(n.getId()).normalize();
        Path p = dir.resolve(b.getArchivo()).normalize();
        if (!p.startsWith(dir)) throw new IllegalArgumentException("Ruta de adjunto no válida");
        return p;
    }

    private void limpiarHuerfanos(Nota n) {
        Path dir = adjuntos.resolve(n.getId());
        if (!Files.isDirectory(dir)) return;
        Set<String> usados = n.getBloques().stream()
                .filter(b -> !b.esTexto())
                .map(Bloque::getArchivo)
                .collect(Collectors.toSet());
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> !usados.contains(p.getFileName().toString())).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignorado) {
                }
            });
        } catch (IOException ignorado) {
        }
    }

    private void borrarRecursivo(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        }
    }

    // ---------- Lectura ----------
    private Nota leer(Path p) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(p)))) {
            if (in.readInt() != MAGIA || in.readInt() != VERSION) return null;
            String id = p.getFileName().toString().replace(EXT, "");
            String titulo = leerTexto(in);
            LocalDateTime fecha = LocalDateTime.parse(leerTexto(in));
            Marca marca = Marca.desde(leerTexto(in));
            int cantidad = in.readInt();
            List<Bloque> lista = new ArrayList<>();
            for (int i = 0; i < cantidad; i++) {
                Bloque.Tipo tipo = Bloque.Tipo.values()[in.readByte()];
                if (tipo == Bloque.Tipo.TEXTO) {
                    lista.add(Bloque.texto(leerTexto(in)));
                } else {
                    String archivo = leerTexto(in);
                    String nombre = leerTexto(in);
                    double ancho = in.readDouble();
                    Bloque.Alineacion al = Bloque.Alineacion.values()[in.readByte()];
                    Bloque b = tipo == Bloque.Tipo.IMAGEN ? Bloque.imagen(archivo, nombre, ancho) : Bloque.archivo(archivo, nombre);
                    b.setAlineacion(al);
                    lista.add(b);
                }
            }
            Nota nota = new Nota(id, titulo, fecha);
            nota.setMarca(marca);
            nota.setBloques(lista);
            return nota;
        } catch (Exception e) {
            return null;
        }
    }

    private Nota leerAntigua(Path p) {
        try {
            String t = Files.readString(p, StandardCharsets.UTF_8);
            int i1 = t.indexOf('\n');
            int i2 = t.indexOf('\n', i1 + 1);
            if (i1 < 0 || i2 < 0) return null;
            String id = p.getFileName().toString().replace(EXT_ANTIGUA, "");
            Nota nota = new Nota(id, t.substring(0, i1), LocalDateTime.parse(t.substring(i1 + 1, i2)));
            nota.setBloques(List.of(Bloque.texto(t.substring(i2 + 1))));
            return nota;
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- Utilidades binarias ----------
    private static void escribir(DataOutputStream o, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        o.writeInt(b.length);
        o.write(b);
    }

    private static String leerTexto(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > 64_000_000) throw new IOException("Dato corrupto");
        byte[] b = new byte[len];
        in.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}