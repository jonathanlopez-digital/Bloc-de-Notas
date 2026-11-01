package main.java.jonathan.blocnotas.model;

public class Bloque {

    public enum Tipo { TEXTO, IMAGEN, ARCHIVO }
    public enum Alineacion { IZQUIERDA, CENTRO, DERECHA }

    private final Tipo tipo;
    private String texto = "";
    private String archivo = "";
    private String nombreOriginal = "";
    private double ancho = 400;
    private Alineacion alineacion = Alineacion.IZQUIERDA;

    private Bloque(Tipo tipo) { this.tipo = tipo; }

    // ---------- Fábricas ----------
    public static Bloque texto(String t) {
        Bloque b = new Bloque(Tipo.TEXTO);
        b.setTexto(t);
        return b;
    }

    public static Bloque imagen(String archivo, String nombre, double ancho) {
        Bloque b = new Bloque(Tipo.IMAGEN);
        b.archivo = archivo;
        b.nombreOriginal = nombre;
        b.setAncho(ancho);
        return b;
    }

    public static Bloque archivo(String archivo, String nombre) {
        Bloque b = new Bloque(Tipo.ARCHIVO);
        b.archivo = archivo;
        b.nombreOriginal = nombre;
        return b;
    }

    // ---------- Acceso ----------
    public Tipo getTipo() { return tipo; }
    public boolean esTexto() { return tipo == Tipo.TEXTO; }
    public String getTexto() { return texto; }
    public void setTexto(String t) { this.texto = t == null ? "" : t; }
    public String getArchivo() { return archivo; }
    public String getNombreOriginal() { return nombreOriginal; }
    public double getAncho() { return ancho; }
    public void setAncho(double a) { this.ancho = Math.max(40, a); }
    public Alineacion getAlineacion() { return alineacion; }
    public void setAlineacion(Alineacion a) { this.alineacion = a; }
}