package main.java.jonathan.blocnotas.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Nota {

    private final String id;
    private String titulo;
    private final List<Bloque> bloques = new ArrayList<>();
    private Marca marca = Marca.NINGUNA;
    private LocalDateTime modificada;

    public Nota() {
        this(UUID.randomUUID().toString(), "", LocalDateTime.now());
    }

    public Nota(String id, String titulo, LocalDateTime modificada) {
        this.id = id;
        this.titulo = titulo == null ? "" : titulo;
        this.modificada = modificada;
        normalizar();
    }

    // ---------- Datos básicos ----------
    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String t) { this.titulo = t == null ? "" : t; }
    public LocalDateTime getModificada() { return modificada; }
    public void setModificada(LocalDateTime m) { this.modificada = m; }
    public Marca getMarca() { return marca; }
    public void setMarca(Marca m) { this.marca = m == null ? Marca.NINGUNA : m; }

    // ---------- Bloques ----------
    public List<Bloque> getBloques() { return bloques; }

    public void setBloques(List<Bloque> nuevos) {
        List<Bloque> copia = new ArrayList<>(nuevos);
        bloques.clear();
        bloques.addAll(copia);
        normalizar();
    }

    /** Estructura garantizada: texto, (adjunto, texto)*  -> siempre se puede escribir antes, entre y después. */
    public void normalizar() {
        List<Bloque> r = new ArrayList<>();
        for (Bloque b : bloques) {
            Bloque ult = r.isEmpty() ? null : r.get(r.size() - 1);
            if (b.esTexto()) {
                if (ult != null && ult.esTexto()) ult.setTexto(ult.getTexto() + "\n" + b.getTexto());
                else r.add(b);
            } else {
                if (ult == null || !ult.esTexto()) r.add(Bloque.texto(""));
                r.add(b);
            }
        }
        if (r.isEmpty() || !r.get(r.size() - 1).esTexto()) r.add(Bloque.texto(""));
        bloques.clear();
        bloques.addAll(r);
    }

    /** Mueve un adjunto un párrafo (o un adjunto) hacia arriba (-1) o abajo (+1). */
    public void mover(Bloque x, int delta) {
        List<Object> a = new ArrayList<>();
        for (Bloque b : bloques) {
            if (b.esTexto()) {
                if (!b.getTexto().isEmpty()) a.addAll(Arrays.asList(b.getTexto().split("\n", -1)));
            } else {
                a.add(b);
            }
        }
        int i = a.indexOf(x);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= a.size()) return;
        Collections.swap(a, i, j);

        List<Bloque> nuevos = new ArrayList<>();
        List<String> parrafos = new ArrayList<>();
        for (Object o : a) {
            if (o instanceof Bloque b) {
                if (!parrafos.isEmpty()) {
                    nuevos.add(Bloque.texto(String.join("\n", parrafos)));
                    parrafos.clear();
                }
                nuevos.add(b);
            } else {
                parrafos.add((String) o);
            }
        }
        if (!parrafos.isEmpty()) nuevos.add(Bloque.texto(String.join("\n", parrafos)));
        setBloques(nuevos);
    }

    // ---------- Consultas ----------
    public String getContenido() {
        StringBuilder sb = new StringBuilder();
        for (Bloque b : bloques) {
            if (!b.esTexto()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(b.getTexto());
        }
        return sb.toString();
    }

    public boolean estaVacia() {
        return titulo.isBlank() && bloques.stream().allMatch(b -> b.esTexto() && b.getTexto().isBlank());
    }

    public int contarImagenes() {
        return (int) bloques.stream().filter(b -> b.getTipo() == Bloque.Tipo.IMAGEN).count();
    }

    public List<Bloque> getArchivos() {
        return bloques.stream().filter(b -> b.getTipo() == Bloque.Tipo.ARCHIVO).toList();
    }

    public String getTituloMostrado() {
        if (!titulo.isBlank()) return titulo;
        String linea = getContenido().strip().lines().findFirst().orElse("");
        if (linea.isBlank()) return "Sin título";
        return linea.length() > 40 ? linea.substring(0, 40) + "…" : linea;
    }
}