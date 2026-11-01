package main.java.jonathan.blocnotas.model;

public enum Marca {
    NINGUNA("Sin marca", null),
    ROJO("Importante", "#e5484d"),
    AMARILLO("Pendiente", "#e8a800"),
    VERDE("Completada", "#2fa84f"),
    AZUL("En proceso", "#2f7de1"),
    MORADO("Ideas", "#8e5ad7");

    private final String nombre;
    private final String color;

    Marca(String nombre, String color) {
        this.nombre = nombre;
        this.color = color;
    }

    public String getNombre() { return nombre; }
    public String getColor() { return color; }
    public boolean esNinguna() { return this == NINGUNA; }

    public static Marca desde(String valor) {
        try {
            return valueOf(valor);
        } catch (Exception e) {
            return NINGUNA;
        }
    }
}