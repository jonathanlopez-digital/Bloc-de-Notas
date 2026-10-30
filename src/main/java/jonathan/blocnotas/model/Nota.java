package main.java.jonathan.blocnotas.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Nota {

    private final String id;
    private String titulo;
    private String contenido;
    private LocalDateTime modificada;

    public Nota() {
        this(UUID.randomUUID().toString(), "", "", LocalDateTime.now());
    }

    public Nota(String id, String titulo, String contenido, LocalDateTime modificada) {
        this.id = id;
        this.titulo = titulo;
        this.contenido = contenido;
        this.modificada = modificada;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo == null ? "" : titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido == null ? "" : contenido; }
    public LocalDateTime getModificada() { return modificada; }
    public void setModificada(LocalDateTime modificada) { this.modificada = modificada; }

    public boolean estaVacia() {
        return titulo.isBlank() && contenido.isBlank();
    }

    /** Título para mostrar en la ficha. */
    public String getTituloMostrado() {
        if (!titulo.isBlank()) return titulo;
        String linea = contenido.strip().lines().findFirst().orElse("");
        if (linea.isBlank()) return "Sin título";
        return linea.length() > 40 ? linea.substring(0, 40) + "…" : linea;
    }
}