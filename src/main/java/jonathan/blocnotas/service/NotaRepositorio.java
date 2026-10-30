package main.java.jonathan.blocnotas.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import main.java.jonathan.blocnotas.model.Nota;

/**
 * Guarda cada nota como archivo en  ~/.blocnotas/
 * Formato: línea 1 = título, línea 2 = fecha ISO, resto = contenido.
 */
public class NotaRepositorio {

    private static final String EXT = ".nota";
    private final Path carpeta = Path.of(System.getProperty("user.home"), ".blocnotas");

    public NotaRepositorio() {
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Nota> listar() {
        try (Stream<Path> s = Files.list(carpeta)) {
            return s.filter(p -> p.getFileName().toString().endsWith(EXT))
                    .map(this::leer)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Nota::getModificada).reversed())
                    .toList();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void guardar(Nota n) {
        String titulo = n.getTitulo().replace("\r", " ").replace("\n", " ");
        String datos = titulo + "\n" + n.getModificada() + "\n" + n.getContenido();
        Path destino = ruta(n);
        Path tmp = carpeta.resolve(n.getId() + ".tmp");
        try {
            Files.writeString(tmp, datos, StandardCharsets.UTF_8);
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void eliminar(Nota n) {
        try {
            Files.deleteIfExists(ruta(n));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path ruta(Nota n) {
        return carpeta.resolve(n.getId() + EXT);
    }

    private Nota leer(Path p) {
        try {
            String t = Files.readString(p, StandardCharsets.UTF_8);
            int i1 = t.indexOf('\n');
            int i2 = t.indexOf('\n', i1 + 1);
            if (i1 < 0 || i2 < 0) return null;
            String id = p.getFileName().toString().replace(EXT, "");
            return new Nota(id, t.substring(0, i1),
                    t.substring(i2 + 1),
                    LocalDateTime.parse(t.substring(i1 + 1, i2)));
        } catch (Exception e) {
            return null;
        }
    }
}